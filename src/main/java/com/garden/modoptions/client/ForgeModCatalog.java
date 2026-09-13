package com.garden.modoptions.client;

import com.garden.modoptions.OptionRegistry;
import cpw.mods.fml.client.FMLClientHandler;
import cpw.mods.fml.client.IModGuiFactory;
import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.ModContainer;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.util.ResourceLocation;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.io.File;
import java.io.FileInputStream;
import java.util.zip.ZipFile;
import java.util.zip.ZipEntry;
import java.util.HashMap;
import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

/**
 * Bridge between the compact ModOptions shell and FML's installed-mod catalog.
 * Configurable mods are sorted first; mods without any settings UI stay visible
 * afterwards as disabled navigation rows.
 */
public final class ForgeModCatalog {
    private static final Map<String, ResourceLocation> PREVIEWS =
            new HashMap<String, ResourceLocation>();
    private static final Map<ResourceLocation, int[]> PREVIEW_SIZES =
            new HashMap<ResourceLocation, int[]>();
    public static List<Entry> entries() {
        LinkedHashMap<String, Entry> byId = new LinkedHashMap<String, Entry>();

        ArrayList<ModContainer> containers = new ArrayList<ModContainer>();
        try {
            FMLClientHandler.instance().addSpecialModEntries(containers);
        } catch (Throwable ignored) {
            // Special entries (for example OptiFine) are optional.
        }
        try {
            containers.addAll(Loader.instance().getModList());
        } catch (Throwable ignored) {
            // Keep library-only registrations usable in stripped test environments.
        }

        for (ModContainer container : containers) {
            if (container == null) continue;
            String modId = safe(container.getModId());
            if (modId.length() == 0 || byId.containsKey(modId)) continue;
            boolean libraryConfig = OptionRegistry.hasOptions(modId);
            boolean forgeConfig = hasForgeConfig(container);
            byId.put(modId, metadataEntry(container, modId, libraryConfig, forgeConfig));
        }

        for (String modId : new ArrayList<String>(byId.keySet())) {
            if (modId.endsWith(".core")
                    && byId.containsKey(modId.substring(0, modId.length() - 5))) {
                byId.remove(modId);
            }
        }

        // A mod can register options before/without having a normal FML container.
        for (String modId : OptionRegistry.mods().keySet()) {
            if (byId.containsKey(modId)) continue;
            byId.put(modId, new Entry(modId, ModOptionsText.mod(modId),
                    null, true, false, null));
        }

        ArrayList<Entry> result = new ArrayList<Entry>(byId.values());
        Collections.sort(result, new Comparator<Entry>() {
            @Override
            public int compare(Entry a, Entry b) {
                if (a.isConfigurable() != b.isConfigurable()) {
                    return a.isConfigurable() ? -1 : 1;
                }
                int name = a.name.compareToIgnoreCase(b.name);
                if (name != 0) return name;
                return a.modId.compareToIgnoreCase(b.modId);
            }
        });
        return result;
    }

    private static boolean hasForgeConfig(ModContainer container) {
        try {
            IModGuiFactory factory = FMLClientHandler.instance().getGuiFactoryFor(container);
            return factory != null && factory.mainConfigGuiClass() != null;
        } catch (Throwable ignored) {
            return false;
        }
    }

    public static GuiScreen createForgeConfig(Entry entry, GuiScreen parent) throws Exception {
        if (entry == null || entry.container == null || !entry.forgeConfig) return null;
        IModGuiFactory factory = FMLClientHandler.instance().getGuiFactoryFor(entry.container);
        if (factory == null || factory.mainConfigGuiClass() == null) return null;
        Constructor<? extends GuiScreen> constructor =
                factory.mainConfigGuiClass().getConstructor(GuiScreen.class);
        return constructor.newInstance(parent);
    }

    /** Loads logos through Forge's resource pack, including special entries such as Forge. */
    public static ResourceLocation loadPreview(Minecraft minecraft, Entry entry) {
        if (entry == null || entry.previewTexture == null || entry.previewTexture.length() == 0
                || entry.container == null) return null;
        String key = entry.modId + ":" + entry.previewTexture;
        if (PREVIEWS.containsKey(key)) return PREVIEWS.get(key);
        ResourceLocation result = null;
        InputStream stream = null;
        try {
            BufferedImage packImage = resourcePackImage(entry);
            if (packImage != null) {
                result = registerPreview(minecraft, packImage);
                System.out.println("[ModOptions] icon loaded from pack for " + entry.modId
                        + " as " + result + " (" + packImage.getWidth() + "x"
                        + packImage.getHeight() + ")");
                PREVIEWS.put(key, result);
                return result;
            }
            String[] classpathPaths = new String[] {
                "/" + entry.previewTexture,
                "/assets/" + entry.modId + "/" + entry.previewTexture
            };
            for (String path : classpathPaths) {
                stream = ForgeModCatalog.class.getResourceAsStream(path);
                if (stream != null) break;
            }
            String[] paths = entry.modId.equals("particlerain")
                    ? new String[] {entry.previewTexture, "icon.png"}
                    : new String[] {entry.previewTexture};
            for (String path : paths) {
                if (stream != null) break;
                if (path == null || path.length() == 0) continue;
                stream = openSource(entry.container, entry.modId, path);
                if (stream != null) break;
            }
            if (stream == null) throw new java.io.FileNotFoundException(entry.previewTexture);
            BufferedImage image = ImageIO.read(stream);
            if (image != null) {
                result = registerPreview(minecraft, image);
                System.out.println("[ModOptions] icon loaded for " + entry.modId
                        + " as " + result + " (" + image.getWidth() + "x" + image.getHeight() + ")");
            }
        } catch (Throwable error) {
            System.out.println("[ModOptions] icon unavailable for " + entry.modId
                    + " (" + entry.previewTexture + "): "
                    + error.getClass().getSimpleName() + " " + error.getMessage());
        } finally {
            if (stream != null) try { stream.close(); } catch (Exception ignored) {}
        }
        PREVIEWS.put(key, result);
        return result;
    }

    public static int[] previewSize(ResourceLocation resource) {
        return PREVIEW_SIZES.get(resource);
    }

    private static BufferedImage resourcePackImage(Entry entry) {
        try {
            java.lang.reflect.Method method = FMLClientHandler.instance().getClass()
                    .getMethod("getResourcePackFor", String.class);
            method.setAccessible(true);
            Object pack = method.invoke(FMLClientHandler.instance(), entry.modId);
            if (pack == null) return null;
            java.lang.reflect.Method imageMethod = pack.getClass().getMethod("getPackImage");
            imageMethod.setAccessible(true);
            return (BufferedImage) imageMethod.invoke(pack);
        } catch (Throwable error) {
            System.out.println("[ModOptions] pack image unavailable for " + entry.modId
                    + ": " + error.getClass().getSimpleName() + " " + error.getMessage());
            return null;
        }
    }

    private static ResourceLocation registerPreview(Minecraft minecraft, BufferedImage image) {
        ResourceLocation result = new ResourceLocation("modoptions",
                "dynamic/mod_preview_" + PREVIEWS.size());
        minecraft.getTextureManager().loadTexture(result, new DynamicTexture(image));
        PREVIEW_SIZES.put(result, new int[] { image.getWidth(), image.getHeight() });
        return result;
    }

    private static InputStream openSource(ModContainer container, String modId, String path) {
        try {
            java.lang.reflect.Method method = container.getClass().getMethod("getSource");
            method.setAccessible(true);
            File source = (File) method.invoke(container);
            if (source == null) return null;
            if (source.isDirectory()) {
                File file = new File(source, "assets/" + modId + "/" + path);
                return file.isFile() ? new FileInputStream(file) : null;
            }
            ZipFile zip = new ZipFile(source);
            ZipEntry entry = zip.getEntry("assets/" + modId + "/" + path);
            if (entry == null) {
                zip.close();
                return null;
            }
            final ZipFile owned = zip;
            final InputStream input = owned.getInputStream(entry);
            return new java.io.FilterInputStream(input) {
                @Override public void close() throws java.io.IOException {
                    try { super.close(); } finally { owned.close(); }
                }
            };
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static String safeName(ModContainer container, String fallback) {
        try {
            String name = safe(container.getName());
            return name.length() == 0 ? fallback : name;
        } catch (Throwable ignored) {
            return fallback;
        }
    }

    private static Entry metadataEntry(ModContainer container, String modId,
            boolean libraryConfig, boolean forgeConfig) {
        String name = safeName(container, modId);
        String description = "";
        String authors = "";
        String version = "";
        try {
            if (container.getMetadata() != null) {
                if (container.getMetadata().name != null
                        && container.getMetadata().name.length() > 0) {
                    name = container.getMetadata().name;
                }
                description = safe(container.getMetadata().description);
                version = safe(container.getVersion());
                if (container.getMetadata().authorList != null) {
                    authors = join(container.getMetadata().authorList);
                }
            }
        } catch (Throwable ignored) {}
        return new Entry(modId, name, container, libraryConfig, forgeConfig,
                logo(container, modId), description, authors, version);
    }

    private static String join(List<String> values) {
        StringBuilder out = new StringBuilder();
        for (String value : values) {
            if (value == null || value.length() == 0) continue;
            if (out.length() > 0) out.append(", ");
            out.append(value);
        }
        return out.toString();
    }

    private static String logo(ModContainer container, String modId) {
        try {
            String logo = container.getMetadata().logoFile;
            if (logo == null || logo.length() == 0) return null;
            logo = logo.replace('\\', '/');
            while (logo.startsWith("/")) logo = logo.substring(1);
            String prefix = "assets/" + modId + "/";
            if (logo.startsWith(prefix)) logo = logo.substring(prefix.length());
            return logo;
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static String safe(String text) {
        return text == null ? "" : text;
    }

    public static final class Entry {
        public final String modId;
        public final String name;
        public final ModContainer container;
        public final boolean libraryConfig;
        public final boolean forgeConfig;
        public final String previewTexture;
        public final String description;
        public final String authors;
        public final String version;

        Entry(String modId, String name, ModContainer container,
                boolean libraryConfig, boolean forgeConfig, String previewTexture) {
            this.modId = modId;
            this.name = name;
            this.container = container;
            this.libraryConfig = libraryConfig;
            this.forgeConfig = forgeConfig;
            this.previewTexture = previewTexture;
            this.description = "";
            this.authors = "";
            this.version = "";
        }

        Entry(String modId, String name, ModContainer container,
                boolean libraryConfig, boolean forgeConfig, String previewTexture,
                String description, String authors, String version) {
            this.modId = modId;
            this.name = name;
            this.container = container;
            this.libraryConfig = libraryConfig;
            this.forgeConfig = forgeConfig;
            this.previewTexture = previewTexture;
            this.description = description;
            this.authors = authors;
            this.version = version;
        }

        public boolean isConfigurable() {
            return libraryConfig || forgeConfig;
        }
    }

    private ForgeModCatalog() {}
}
