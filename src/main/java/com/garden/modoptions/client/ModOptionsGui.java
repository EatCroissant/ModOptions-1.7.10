package com.garden.modoptions.client;

import com.garden.modoptions.ModOption;
import com.garden.modoptions.ModOptionsStyle;
import com.garden.modoptions.NumericOption;
import com.garden.modoptions.OptionGroup;
import com.garden.modoptions.OptionRegistry;
import com.garden.modoptions.OptionScope;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

/**
 * Responsive settings shell inspired by compact web preference panels:
 * persistent navigation, search, collapsible semantic sections and adaptive cards.
 */
public final class ModOptionsGui extends GuiScreen {

  private static final int HEADER_SCROLL_SPACE = 130;
  private static final int DONE = 29000;
  private static final int MOD_BASE = 1000;
  private static final int OPTION_BASE = 2000;
  private static final int GROUP_BASE = 30000;
  private static final int OPEN_NATIVE_CONFIG = 29100;
  private static final int THEME_SETTINGS = 29200;

  private static final int COLUMN_GAP = 5;
  private static final int MIN_CARD_WIDTH = 210;
  private static final int OPTION_SCROLLBAR_WIDTH = 5;
  private static final int NAV_SCROLLBAR_WIDTH = 4;
  private static final int TOP_MARGIN = 4;

  private final GuiScreen parent;
  private final Set<String> collapsedGroups = new HashSet<String>();
  private final Set<String> expandedDefaultGroups = new HashSet<String>();
  private final Map<Integer, String> groupButtonIds = new HashMap<Integer, String>();

  private String selectedMod;
  private List<String> modIds = new ArrayList<String>();
  private List<ForgeModCatalog.Entry> modEntries = new ArrayList<ForgeModCatalog.Entry>();
  private List<ModOption> selectedOptions = new ArrayList<ModOption>();
  private List<DisplayGroup> visibleGroups = new ArrayList<DisplayGroup>();
  private GuiTextField searchField;
  private int searchFieldX = Integer.MIN_VALUE;
  private int searchFieldY = Integer.MIN_VALUE;
  private int searchFieldWidth = -1;
  private int searchFieldHeight = -1;
  private String searchText = "";
  private int optionScroll;
  private int optionContentHeight;
  private int modScroll;
  private int filteredOptionCount;
  private boolean draggingOptionScrollbar;
  private boolean draggingModScrollbar;
  private int scrollbarDragOffset;

  public ModOptionsGui(GuiScreen parent) {
    this.parent = parent;
  }

  @Override
  public boolean doesGuiPauseGame() {
    return true;
  }

  @Override
  public void initGui() {
    refreshModCatalog();
    LayoutMetrics metrics = metrics();
    syncSearchField(metrics);
    rebuildButtons();
  }

  private void refreshModCatalog() {
    modEntries = ForgeModCatalog.entries();
    modIds = new ArrayList<String>();
    for (ForgeModCatalog.Entry entry : modEntries) modIds.add(entry.modId);

    ForgeModCatalog.Entry selected = entryFor(selectedMod);
    if (selected == null) {
      selected = firstConfigurableEntry();
      if (selected == null && !modEntries.isEmpty()) selected = modEntries.get(0);
      selectedMod = selected == null ? null : selected.modId;
    }
    loadSelectedOptions();
  }

  private ForgeModCatalog.Entry firstConfigurableEntry() {
    for (ForgeModCatalog.Entry entry : modEntries) {
      if (entry.isConfigurable()) return entry;
    }
    return null;
  }

  private ForgeModCatalog.Entry entryFor(String modId) {
    if (modId == null) return null;
    for (ForgeModCatalog.Entry entry : modEntries) {
      if (modId.equals(entry.modId)) return entry;
    }
    return null;
  }

  private void loadSelectedOptions() {
    Map<String, List<ModOption>> registered = OptionRegistry.mods();
    List<ModOption> options = selectedMod == null ? null : registered.get(selectedMod);
    selectedOptions = options == null ? new ArrayList<ModOption>() : new ArrayList<ModOption>(options);
  }

  private void syncSearchField(LayoutMetrics metrics) {
    int x = metrics.searchX + 5;
    int y = metrics.searchY + 1;
    int w = Math.max(20, metrics.searchWidth - 22);
    int h = Math.max(12, metrics.searchHeight - 2);
    if (
      searchField != null && x == searchFieldX && y == searchFieldY && w == searchFieldWidth && h == searchFieldHeight
    ) return;

    boolean focused = searchField != null && searchField.isFocused();
    String text = searchField == null ? searchText : searchField.getText();
    GuiTextField next = new GuiTextField(fontRendererObj, x, y, w, h);
    next.setMaxStringLength(64);
    next.setEnableBackgroundDrawing(false);
    next.setText(text == null ? "" : text);
    next.setFocused(focused);
    searchField = next;
    searchFieldX = x;
    searchFieldY = y;
    searchFieldWidth = w;
    searchFieldHeight = h;
  }

  private void rebuildButtons() {
    buttonList.clear();
    groupButtonIds.clear();
    LayoutMetrics metrics = metrics();
    syncSearchField(metrics);

    buildNavigation(metrics);
    visibleGroups = buildDisplayGroups(searchText);
    filteredOptionCount = countOptions(visibleGroups);

    int columns = optionColumns(metrics.contentWidth);
    optionContentHeight = HEADER_SCROLL_SPACE + measureContent(visibleGroups, columns, searchText.length() > 0);
    int maxScroll = globalContentMaxScroll(metrics);
    optionScroll = clamp(optionScroll, 0, maxScroll);

    buildOptionControls(metrics, visibleGroups, columns);
    ForgeModCatalog.Entry selectedEntry = entryFor(selectedMod);
    if (selectedEntry != null && !selectedEntry.libraryConfig) {
      boolean hasNativeConfig = selectedEntry.forgeConfig;
      TexturedButton configButton = new TexturedButton(
        OPEN_NATIVE_CONFIG,
        metrics.navX,
        metrics.doneY,
        metrics.navWidth,
        metrics.doneHeight,
        hasNativeConfig
          ? ModOptionsText.ui("modoptions.open_native_config", "Open Config")
          : ModOptionsText.ui("modoptions.no_config", "No config")
      );
      configButton.enabled = hasNativeConfig;
      buttonList.add(configButton);
    }
    buttonList.add(
      new TexturedButton(
        DONE,
        metrics.doneX,
        metrics.doneY,
        metrics.doneWidth,
        metrics.doneHeight,
        I18n.format("gui.done")
      )
    );
    buttonList.add(new TexturedButton(THEME_SETTINGS,
      metrics.searchX - 24, metrics.panelY + 7, 20, 20, "\u2699"));
  }

  private void buildNavigation(LayoutMetrics metrics) {
    int visibleRows = Math.max(1, metrics.navHeight / (navRowHeight() + navRowGap()));
    int maxScroll = Math.max(0, modIds.size() - visibleRows);
    modScroll = clamp(modScroll, 0, maxScroll);
    int end = Math.min(modIds.size(), modScroll + visibleRows);
    for (int i = modScroll; i < end; i++) {
      String modId = modIds.get(i);
      ForgeModCatalog.Entry entry = i < modEntries.size() ? modEntries.get(i) : null;
      int row = i - modScroll;
      TexturedButton button = new TexturedButton(
        MOD_BASE + i,
        metrics.navX,
        metrics.navY + row * (navRowHeight() + navRowGap()),
        metrics.navWidth,
        navRowHeight(),
        entry == null ? ModOptionsText.mod(modId) : entry.name
      );
            if (entry != null && entry.previewTexture != null) {
                ResourceLocation preview = ForgeModCatalog.loadPreview(mc, entry);
                if (preview != null) {
                    int[] size = ForgeModCatalog.previewSize(preview);
                    if (size != null) button.preview(preview, size[0], size[1]);
                    else button.preview(preview);
                }
      }
      button.leftAligned(true).selected(modId.equals(selectedMod));
      // Keep every installed mod selectable. Mods without registered
      // options still provide useful identity and may expose a preview.
      button.enabled = true;
      buttonList.add(button);
    }
  }

  private void buildOptionControls(LayoutMetrics metrics, List<DisplayGroup> groups, int columns) {
    int cardWidth = (metrics.contentWidth - (columns - 1) * COLUMN_GAP) / columns;
    int cursor = 0;
    int groupIndex = 0;
    boolean searching = searchText.length() > 0;
    boolean showHeaders = groups.size() > 1 || (groups.size() == 1 && !groups.get(0).implicitGeneral);

    for (DisplayGroup group : groups) {
      boolean collapsed = !searching && isGroupCollapsed(group);
      if (showHeaders) {
        int screenY = metrics.viewportY + cursor - optionScroll;
        if (intersects(screenY, groupHeaderHeight(), metrics.headerBottom, metrics.footerY)) {
          int id = GROUP_BASE + groupIndex;
          groupButtonIds.put(id, group.id);
          buttonList.add(
            new GroupHeaderButton(
              id,
              metrics.contentX,
              screenY,
              metrics.contentWidth,
              groupHeaderHeight(),
              group.label,
              collapsed,
              group.options.size()
            )
          );
        }
        cursor += groupHeaderHeight() + 3;
      }

      if (!collapsed) {
        for (int i = 0; i < group.options.size(); i++) {
          ModOption option = group.options.get(i);
          int column = i % columns;
          int row = i / columns;
          int x = metrics.contentX + column * (cardWidth + COLUMN_GAP);
          int virtualY = cursor + row * (cardHeight() + cardGap());
          int y = metrics.viewportY + virtualY - optionScroll;
          if (!intersects(y, cardHeight(), metrics.headerBottom, metrics.footerY)) continue;
          int sourceIndex = selectedOptions.indexOf(option);
          if (sourceIndex < 0) continue;
          GuiButton button =
            option instanceof NumericOption
              ? new NumericSliderButton(
                  OPTION_BASE + sourceIndex,
                  x,
                  y,
                  cardWidth,
                  cardHeight(),
                  option,
                  (NumericOption) option
                )
              : new OptionCycleButton(OPTION_BASE + sourceIndex, x, y, cardWidth, cardHeight(), option);
          buttonList.add(button);
        }
        int rows = (group.options.size() + columns - 1) / columns;
        cursor += rows * (cardHeight() + cardGap());
        if (rows > 0) cursor -= cardGap();
      }
      if (groupIndex < groups.size() - 1) cursor += groupGap();
      groupIndex++;
    }
  }

  @Override
  protected void actionPerformed(GuiButton button) {
    if (!button.enabled) return;
    if (button.id == DONE) {
      closeToParent();
      return;
    }
    if (button.id == THEME_SETTINGS) {
      mc.displayGuiScreen(new ThemeOptionsGui(this));
      return;
    }
    if (button.id == OPEN_NATIVE_CONFIG) {
      ForgeModCatalog.Entry entry = entryFor(selectedMod);
      try {
        GuiScreen screen = ForgeModCatalog.createForgeConfig(entry, this);
        if (screen != null) mc.displayGuiScreen(screen);
      } catch (Exception ignored) {
        // FML's own mod-list screen handles the same failure by keeping the parent open.
      }
      return;
    }
    if (button.id >= MOD_BASE && button.id < MOD_BASE + modIds.size()) {
      int index = button.id - MOD_BASE;
      if (index >= 0 && index < modIds.size()) {
        selectedMod = modIds.get(index);
        loadSelectedOptions();
        optionScroll = 0;
        searchText = "";
        if (searchField != null) searchField.setText("");
        rebuildButtons();
      }
      return;
    }
    if (button.id >= GROUP_BASE) {
      String groupId = groupButtonIds.get(button.id);
      if (groupId != null && searchText.length() == 0) {
        String key = collapseKey(groupId);
        DisplayGroup group = findVisibleGroup(groupId);
        boolean currentlyCollapsed = group != null && isGroupCollapsed(group);
        if (currentlyCollapsed) {
          collapsedGroups.remove(key);
          expandedDefaultGroups.add(key);
        } else {
          expandedDefaultGroups.remove(key);
          collapsedGroups.add(key);
        }
        rebuildButtons();
      }
      return;
    }
    if (button.id >= OPTION_BASE && button.id < OPTION_BASE + selectedOptions.size()) {
      if (button instanceof NumericSliderButton) return;
      ModOption option = selectedOptions.get(button.id - OPTION_BASE);
      option.cycle();
      rebuildButtons();
    }
  }

  @Override
  public void handleMouseInput() {
    super.handleMouseInput();
    int wheel = Mouse.getDWheel();
    if (wheel == 0) return;
    LayoutMetrics metrics = metrics();
    int mouseX = (Mouse.getEventX() * width) / mc.displayWidth;
    int mouseY = height - (Mouse.getEventY() * height) / mc.displayHeight - 1;

    if (
      mouseX >= metrics.sidebarX &&
      mouseX < metrics.sidebarX + metrics.sidebarWidth &&
      mouseY >= metrics.navY &&
      mouseY < metrics.navY + metrics.navHeight
    ) {
      int visibleRows = visibleNavRows(metrics);
      int maxScroll = Math.max(0, modIds.size() - visibleRows);
      modScroll = clamp(modScroll + (wheel < 0 ? 1 : -1), 0, maxScroll);
      rebuildButtons();
      return;
    }

    if (
      mouseX >= metrics.contentX &&
      mouseX < metrics.contentX + metrics.contentWidth &&
      mouseY >= metrics.headerBottom &&
      mouseY < metrics.footerY
    ) {
      int maxScroll = globalContentMaxScroll(metrics);
      int amount = ModOptionsStyle.compactLayout ? 18 : 24;
      if (Keyboard.isKeyDown(Keyboard.KEY_LSHIFT) || Keyboard.isKeyDown(Keyboard.KEY_RSHIFT)) {
        amount *= 2;
      }
      optionScroll = clamp(optionScroll + (wheel < 0 ? amount : -amount), 0, maxScroll);
      rebuildButtons();
    }
  }

  private int globalContentMaxScroll(LayoutMetrics metrics) {
    int visibleHeight = Math.max(24, metrics.footerY - metrics.headerBottom - 8);
    return Math.max(0, optionContentHeight - visibleHeight);
  }

  @Override
  protected void mouseClicked(int mouseX, int mouseY, int mouseButton) {
    LayoutMetrics metrics = metrics();

    if (
      mouseButton == 0 && pointIn(mouseX, mouseY, metrics.doneX, metrics.doneY, metrics.doneWidth, metrics.doneHeight)
    ) {
      closeToParent();
      return;
    }

    if (mouseButton == 0 && clearSearchHit(mouseX, mouseY, metrics)) {
      if (searchField != null) {
        searchField.setText("");
        searchField.setFocused(true);
      }
      searchText = "";
      optionScroll = 0;
      rebuildButtons();
      return;
    }

    if (mouseButton == 0 && beginScrollbarDrag(mouseX, mouseY, metrics)) return;

    ForgeModCatalog.Entry selectedEntry = entryFor(selectedMod);
    if (selectedEntry != null && selectedEntry.libraryConfig && searchField != null) {
      searchField.mouseClicked(mouseX, mouseY, mouseButton);
    }

    // GuiScreen does not know about our clipped scroll viewports. Temporarily
    // mask controls whose visible pixels do not contain this click so a
    // partially clipped row cannot be activated through the header/footer.
    List<GuiButton> masked = new ArrayList<GuiButton>();
    boolean optionPoint = pointIn(
      mouseX,
      mouseY,
      metrics.contentX,
      metrics.viewportY,
      metrics.contentWidth,
      metrics.viewportHeight
    );
    boolean navPoint = pointIn(mouseX, mouseY, metrics.navX, metrics.navY, metrics.navWidth, metrics.navHeight);
    for (Object object : buttonList) {
      if (!(object instanceof GuiButton)) continue;
      GuiButton button = (GuiButton) object;
      boolean optionControl =
        button instanceof NumericSliderButton ||
        button instanceof OptionCycleButton ||
        button instanceof GroupHeaderButton;
      boolean navControl = button.id >= MOD_BASE && button.id < MOD_BASE + modIds.size();
      if ((optionControl && !optionPoint) || (navControl && !navPoint)) {
        if (button.enabled) {
          button.enabled = false;
          masked.add(button);
        }
      }
    }
    super.mouseClicked(mouseX, mouseY, mouseButton);
    for (GuiButton button : masked) button.enabled = true;
  }

  @Override
  protected void mouseClickMove(int mouseX, int mouseY, int clickedMouseButton, long timeSinceLastClick) {
    if (clickedMouseButton == 0 && (draggingOptionScrollbar || draggingModScrollbar)) {
      updateScrollbarDrag(mouseY, metrics());
      return;
    }
    super.mouseClickMove(mouseX, mouseY, clickedMouseButton, timeSinceLastClick);
  }

  @Override
  protected void mouseMovedOrUp(int mouseX, int mouseY, int state) {
    super.mouseMovedOrUp(mouseX, mouseY, state);
    if (state == 0) {
      draggingOptionScrollbar = false;
      draggingModScrollbar = false;
    }
  }

  @Override
  protected void keyTyped(char typedChar, int keyCode) {
    if (keyCode == Keyboard.KEY_ESCAPE) {
      closeToParent();
      return;
    }
    ForgeModCatalog.Entry selectedEntry = entryFor(selectedMod);
    if (
      keyCode == Keyboard.KEY_F &&
      (Keyboard.isKeyDown(Keyboard.KEY_LCONTROL) || Keyboard.isKeyDown(Keyboard.KEY_RCONTROL))
    ) {
      if (selectedEntry != null && selectedEntry.libraryConfig && searchField != null) {
        searchField.setFocused(true);
      }
      return;
    }
    if (selectedEntry != null && selectedEntry.libraryConfig && searchField != null && searchField.isFocused()) {
      String before = searchField.getText();
      if (searchField.textboxKeyTyped(typedChar, keyCode)) {
        searchText = searchField.getText();
        if (!before.equals(searchText)) {
          optionScroll = 0;
          rebuildButtons();
        }
        return;
      }
    }
    super.keyTyped(typedChar, keyCode);
  }

  @Override
  public void updateScreen() {
    super.updateScreen();
    if (searchField != null) searchField.updateCursorCounter();
  }

  @Override
  public void drawScreen(int mouseX, int mouseY, float partialTicks) {
    drawDefaultBackground();
    LayoutMetrics metrics = metrics();
    syncSearchField(metrics);

    ModOptionsTextures.drawPanel(this, metrics.panelX, metrics.panelY, metrics.panelWidth, metrics.panelHeight);
    drawRect(
      metrics.sidebarX,
      metrics.panelY + 1,
      metrics.sidebarX + metrics.sidebarWidth,
      metrics.panelY + metrics.panelHeight - 1,
      ModOptionsStyle.sidebarColor
    );
    drawRect(
      metrics.contentDividerX,
      metrics.panelY + 1,
      metrics.contentDividerX + 1,
      metrics.panelY + metrics.panelHeight - 1,
      ModOptionsStyle.dividerColor
    );
    drawRect(
      metrics.panelX + 1,
      metrics.headerBottom,
      metrics.panelX + metrics.panelWidth - 1,
      metrics.headerBottom + 1,
      ModOptionsStyle.dividerColor
    );
    drawRect(
      metrics.panelX + 1,
      metrics.footerY,
      metrics.panelX + metrics.panelWidth - 1,
      metrics.footerY + 1,
      ModOptionsStyle.dividerColor
    );

    drawString(
      fontRendererObj,
      ModOptionsText.ui("modoptions.title", "Mod Options"),
      metrics.panelX + 12,
      metrics.panelY + 8,
      ModOptionsStyle.accentColor
    );
    drawString(
      fontRendererObj,
      ModOptionsText.ui("modoptions.mods", "MODS"),
      metrics.navX,
      metrics.headerBottom + 9,
      ModOptionsStyle.mutedTextColor
    );

    ForgeModCatalog.Entry selectedEntry = entryFor(selectedMod);
    if (selectedEntry != null) {
      int contentOffset = optionScroll;
      ResourceLocation preview = ForgeModCatalog.loadPreview(mc, selectedEntry);
      int iconAreaWidth = 8;
      if (preview != null) {
        int[] size = ForgeModCatalog.previewSize(preview);
        if (size != null) {
          float maxHeight = 64.0f;
          float scale = maxHeight / Math.max(1, size[1]);
          int renderedWidth = Math.max(1, Math.round(size[0] * scale));
          iconAreaWidth = Math.min(metrics.contentWidth / 2 - 8, renderedWidth + 8);
        }
      }
      int headerTextX = preview == null ? metrics.contentX + 8 : metrics.contentX + iconAreaWidth + 16;
      drawModPreview(selectedEntry, metrics, iconAreaWidth);
      int headerTextWidth = Math.max(40, metrics.panelX + metrics.panelWidth - headerTextX - 16);
      String modName = fontRendererObj.trimStringToWidth(selectedEntry.name, headerTextWidth);
      int titleY = metrics.headerBottom + 30 - contentOffset;
      if (titleY >= metrics.panelY && titleY < metrics.footerY - 8) {
        drawString(fontRendererObj, modName, headerTextX, titleY, ModOptionsStyle.textColor);
      }
      String meta = selectedEntry.description;
      if (selectedEntry.authors.length() > 0) {
        if (meta.length() > 0) meta += "  |  ";
        meta += selectedEntry.authors;
      }
      if (selectedEntry.version.length() > 0) meta += "  " + selectedEntry.version;
      if (meta.length() > 0) {
        int descriptionX = metrics.contentX + iconAreaWidth + 16;
        int rightWidth = metrics.panelX + metrics.panelWidth - descriptionX - 16;
        List<String> rightLines =
          rightWidth >= 40 ? fontRendererObj.listFormattedStringToWidth(meta, rightWidth) : new ArrayList<String>();
        if (rightLines.size() == 1) {
          int descriptionY = metrics.headerBottom + 58 - contentOffset;
          if (descriptionY >= metrics.panelY && descriptionY < metrics.footerY) {
            drawString(fontRendererObj, rightLines.get(0), descriptionX, descriptionY, ModOptionsStyle.mutedTextColor);
          }
        } else {
          List<String> metaLines = fontRendererObj.listFormattedStringToWidth(
            meta,
            Math.max(40, metrics.contentWidth - 16)
          );
          int lineY = metrics.headerBottom + 92 - contentOffset;
          for (int i = 0; i < metaLines.size() && i < 3; i++) {
            int currentY = lineY + i * 10;
            if (currentY >= metrics.panelY && currentY < metrics.footerY) {
              drawString(
                fontRendererObj,
                metaLines.get(i),
                metrics.contentX + 8,
                currentY,
                ModOptionsStyle.mutedTextColor
              );
            }
          }
        }
      }
    }

    if (selectedEntry != null && selectedEntry.libraryConfig) {
      drawSearchField(metrics);
      if (filteredOptionCount == 0) {
        String message = ModOptionsText.ui("modoptions.no_results", "No settings match your search");
        drawCenteredString(
          fontRendererObj,
          message,
          metrics.contentX + metrics.contentWidth / 2,
          metrics.viewportY + Math.min(45, metrics.viewportHeight / 2),
          ModOptionsStyle.mutedTextColor
        );
      }
    } else if (selectedEntry != null && selectedEntry.forgeConfig) {
      drawString(
        fontRendererObj,
        ModOptionsText.ui("modoptions.native_config_hint", "This mod uses its own configuration screen."),
        metrics.contentX,
        metrics.viewportY + 42,
        ModOptionsStyle.mutedTextColor
      );
    }

    // Unlike the old implementation, scrollable controls are truly clipped.
    // A row can enter/leave the viewport by pixels without drawing through
    // the toolbar or footer.
    drawClippedButtons(mouseX, mouseY, metrics);

    int optionMax = globalContentMaxScroll(metrics);
    int fullOptionTrackY = metrics.headerBottom + 8;
    int fullOptionTrackHeight = Math.max(24, metrics.footerY - fullOptionTrackY - 8);
    if (optionMax > 0) {
      boolean over = pointIn(
        mouseX,
        mouseY,
        optionScrollbarX(metrics) - 2,
        fullOptionTrackY,
        OPTION_SCROLLBAR_WIDTH + 4,
        fullOptionTrackHeight
      );
      ModOptionsTextures.drawScrollbar(
        this,
        optionScrollbarX(metrics),
        fullOptionTrackY,
        OPTION_SCROLLBAR_WIDTH,
        fullOptionTrackHeight,
        optionScroll,
        optionContentHeight,
        Math.max(24, metrics.footerY - metrics.headerBottom - 8),
        over,
        draggingOptionScrollbar
      );
    }

    int visibleNavRows = visibleNavRows(metrics);
    if (modIds.size() > visibleNavRows) {
      boolean over = pointIn(
        mouseX,
        mouseY,
        navScrollbarX(metrics) - 2,
        metrics.navY,
        NAV_SCROLLBAR_WIDTH + 4,
        metrics.navHeight
      );
      ModOptionsTextures.drawScrollbar(
        this,
        navScrollbarX(metrics),
        metrics.navY,
        NAV_SCROLLBAR_WIDTH,
        metrics.navHeight,
        modScroll,
        modIds.size(),
        visibleNavRows,
        over,
        draggingModScrollbar
      );
    }

    drawContextHelp(mouseX, mouseY, metrics);
  }

  private void drawModPreview(ForgeModCatalog.Entry entry, LayoutMetrics metrics, int iconAreaWidth) {
    if (entry.previewTexture == null || entry.previewTexture.length() == 0) return;
    try {
      ResourceLocation resource = ForgeModCatalog.loadPreview(mc, entry);
      if (resource == null) return;
      GL11.glColor4f(1.0f, 1.0f, 1.0f, 1.0f);
      mc.getTextureManager().bindTexture(resource);
      Tessellator tessellator = Tessellator.instance;
      int iconAreaLeft = metrics.contentX;
      iconAreaWidth = Math.max(8, iconAreaWidth);
      int[] sourceSize = ForgeModCatalog.previewSize(resource);
      int sourceWidth = sourceSize == null ? 1 : Math.max(1, sourceSize[0]);
      int sourceHeight = sourceSize == null ? 1 : Math.max(1, sourceSize[1]);
      float maxIconHeight = 64.0f;
      float scale = Math.min((float) Math.max(1, iconAreaWidth - 8) / sourceWidth, maxIconHeight / sourceHeight);
      int iconWidth = Math.max(1, Math.round(sourceWidth * scale));
      int iconHeight = Math.max(1, Math.round(sourceHeight * scale));
      int left = iconAreaLeft + Math.max(0, (iconAreaWidth - iconWidth) / 2);
      int top = metrics.headerBottom + 22 - optionScroll;
      if (top < metrics.panelY || top + iconHeight <= metrics.panelY || top >= metrics.footerY) return;
      tessellator.startDrawingQuads();
      tessellator.addVertexWithUV(left, top + iconHeight, zLevel, 0, 1);
      tessellator.addVertexWithUV(left + iconWidth, top + iconHeight, zLevel, 1, 1);
      tessellator.addVertexWithUV(left + iconWidth, top, zLevel, 1, 0);
      tessellator.addVertexWithUV(left, top, zLevel, 0, 0);
      tessellator.draw();
      GL11.glColor4f(1.0f, 1.0f, 1.0f, 1.0f);
    } catch (Throwable ignored) {
      // A missing optional logo must not prevent the settings screen opening.
    }
  }

  private void drawSearchField(LayoutMetrics metrics) {
    boolean focused = searchField != null && searchField.isFocused();
    int background = focused ? 0xC7242B3B : 0x9B202635;
    int border = focused ? ModOptionsStyle.accentSoftColor : 0x693E4658;
    drawRect(
      metrics.searchX,
      metrics.searchY,
      metrics.searchX + metrics.searchWidth,
      metrics.searchY + metrics.searchHeight,
      background
    );
    drawRect(metrics.searchX, metrics.searchY, metrics.searchX + metrics.searchWidth, metrics.searchY + 1, border);
    drawRect(
      metrics.searchX,
      metrics.searchY + metrics.searchHeight - 1,
      metrics.searchX + metrics.searchWidth,
      metrics.searchY + metrics.searchHeight,
      border
    );
    drawRect(metrics.searchX, metrics.searchY, metrics.searchX + 1, metrics.searchY + metrics.searchHeight, border);
    drawRect(
      metrics.searchX + metrics.searchWidth - 1,
      metrics.searchY,
      metrics.searchX + metrics.searchWidth,
      metrics.searchY + metrics.searchHeight,
      border
    );

    if (searchField != null) {
      searchField.drawTextBox();
      if (searchField.getText().length() == 0 && !focused) {
        drawString(
          fontRendererObj,
          ModOptionsText.ui("modoptions.search", "Search settings..."),
          metrics.searchX + 5,
          metrics.searchY + 5,
          ModOptionsStyle.mutedTextColor
        );
      } else if (searchField.getText().length() > 0) {
        int x = metrics.searchX + metrics.searchWidth - 12;
        int y = metrics.searchY + 5;
        drawString(fontRendererObj, "x", x, y, focused ? ModOptionsStyle.textColor : ModOptionsStyle.mutedTextColor);
      }
    }
  }

  private void drawClippedButtons(int mouseX, int mouseY, LayoutMetrics metrics) {
    enableScissor(metrics.navX, metrics.navY, metrics.navWidth, metrics.navHeight);
    for (Object object : buttonList) {
      if (!(object instanceof GuiButton)) continue;
      GuiButton button = (GuiButton) object;
      if (button.id >= MOD_BASE && button.id < MOD_BASE + modIds.size()) {
        button.drawButton(mc, mouseX, mouseY);
      }
    }
    drawModAvailabilityDivider(metrics);
    disableScissor();

    enableScissor(metrics.contentX, metrics.headerBottom, metrics.contentWidth, metrics.footerY - metrics.headerBottom);
    for (Object object : buttonList) {
      if (!(object instanceof GuiButton)) continue;
      GuiButton button = (GuiButton) object;
      if (
        button instanceof NumericSliderButton ||
        button instanceof OptionCycleButton ||
        button instanceof GroupHeaderButton
      ) {
        button.drawButton(mc, mouseX, mouseY);
      }
    }
    disableScissor();

    for (Object object : buttonList) {
      if (!(object instanceof GuiButton)) continue;
      GuiButton button = (GuiButton) object;
      boolean scrollable =
        button instanceof NumericSliderButton ||
        button instanceof OptionCycleButton ||
        button instanceof GroupHeaderButton ||
        (button.id >= MOD_BASE && button.id < MOD_BASE + modIds.size());
      if (!scrollable) button.drawButton(mc, mouseX, mouseY);
    }
  }

  private void drawModAvailabilityDivider(LayoutMetrics metrics) {
    int firstUnavailable = -1;
    for (int i = 0; i < modEntries.size(); i++) {
      if (!modEntries.get(i).isConfigurable()) {
        firstUnavailable = i;
        break;
      }
    }
    if (firstUnavailable <= 0) return;
    int row = firstUnavailable - modScroll;
    if (row < 0 || row > visibleNavRows(metrics)) return;
    int y = metrics.navY + row * (navRowHeight() + navRowGap()) - 2;
    if (y <= metrics.navY || y >= metrics.navY + metrics.navHeight) return;
    drawRect(metrics.navX, y, metrics.navX + metrics.navWidth, y + 1, ModOptionsStyle.dividerColor);
  }

  private void enableScissor(int x, int y, int width, int height) {
    int left = (x * mc.displayWidth) / Math.max(1, this.width);
    int right = ((x + width) * mc.displayWidth) / Math.max(1, this.width);
    int top = (y * mc.displayHeight) / Math.max(1, this.height);
    int bottom = ((y + height) * mc.displayHeight) / Math.max(1, this.height);
    GL11.glEnable(GL11.GL_SCISSOR_TEST);
    GL11.glScissor(left, mc.displayHeight - bottom, Math.max(0, right - left), Math.max(0, bottom - top));
  }

  private void disableScissor() {
    GL11.glDisable(GL11.GL_SCISSOR_TEST);
  }

  private boolean clearSearchHit(int mouseX, int mouseY, LayoutMetrics metrics) {
    ForgeModCatalog.Entry selectedEntry = entryFor(selectedMod);
    return (
      selectedEntry != null &&
      selectedEntry.libraryConfig &&
      searchField != null &&
      searchField.getText().length() > 0 &&
      pointIn(mouseX, mouseY, metrics.searchX + metrics.searchWidth - 17, metrics.searchY, 17, metrics.searchHeight)
    );
  }

  private boolean beginScrollbarDrag(int mouseX, int mouseY, LayoutMetrics metrics) {
    int optionMax = globalContentMaxScroll(metrics);
    if (
      optionMax > 0 &&
      pointIn(
        mouseX,
        mouseY,
        optionScrollbarX(metrics) - 3,
        metrics.viewportY,
        OPTION_SCROLLBAR_WIDTH + 6,
        metrics.viewportHeight
      )
    ) {
      int thumbHeight = ModOptionsTextures.scrollbarThumbHeight(
        metrics.viewportHeight,
        optionContentHeight,
        metrics.viewportHeight
      );
      int thumbY = ModOptionsTextures.scrollbarThumbY(
        metrics.viewportY,
        metrics.viewportHeight,
        thumbHeight,
        optionScroll,
        optionContentHeight,
        metrics.viewportHeight
      );
      if (mouseY >= thumbY && mouseY < thumbY + thumbHeight) {
        scrollbarDragOffset = mouseY - thumbY;
      } else {
        scrollbarDragOffset = thumbHeight / 2;
        optionScroll = optionScrollFromThumb(
          mouseY - scrollbarDragOffset,
          metrics.viewportY,
          metrics.viewportHeight,
          thumbHeight,
          optionMax
        );
        rebuildButtons();
      }
      draggingOptionScrollbar = true;
      draggingModScrollbar = false;
      return true;
    }

    int visibleRows = visibleNavRows(metrics);
    int navMax = Math.max(0, modIds.size() - visibleRows);
    if (
      navMax > 0 &&
      pointIn(mouseX, mouseY, navScrollbarX(metrics) - 3, metrics.navY, NAV_SCROLLBAR_WIDTH + 6, metrics.navHeight)
    ) {
      int thumbHeight = ModOptionsTextures.scrollbarThumbHeight(metrics.navHeight, modIds.size(), visibleRows);
      int thumbY = ModOptionsTextures.scrollbarThumbY(
        metrics.navY,
        metrics.navHeight,
        thumbHeight,
        modScroll,
        modIds.size(),
        visibleRows
      );
      if (mouseY >= thumbY && mouseY < thumbY + thumbHeight) {
        scrollbarDragOffset = mouseY - thumbY;
      } else {
        scrollbarDragOffset = thumbHeight / 2;
        modScroll = optionScrollFromThumb(
          mouseY - scrollbarDragOffset,
          metrics.navY,
          metrics.navHeight,
          thumbHeight,
          navMax
        );
        rebuildButtons();
      }
      draggingModScrollbar = true;
      draggingOptionScrollbar = false;
      return true;
    }
    return false;
  }

  private void updateScrollbarDrag(int mouseY, LayoutMetrics metrics) {
    if (draggingOptionScrollbar) {
      int max = globalContentMaxScroll(metrics);
      int thumbHeight = ModOptionsTextures.scrollbarThumbHeight(
        metrics.viewportHeight,
        optionContentHeight,
        metrics.viewportHeight
      );
      int next = optionScrollFromThumb(
        mouseY - scrollbarDragOffset,
        metrics.viewportY,
        metrics.viewportHeight,
        thumbHeight,
        max
      );
      if (next != optionScroll) {
        optionScroll = next;
        rebuildButtons();
      }
    } else if (draggingModScrollbar) {
      int visibleRows = visibleNavRows(metrics);
      int max = Math.max(0, modIds.size() - visibleRows);
      int thumbHeight = ModOptionsTextures.scrollbarThumbHeight(metrics.navHeight, modIds.size(), visibleRows);
      int next = optionScrollFromThumb(mouseY - scrollbarDragOffset, metrics.navY, metrics.navHeight, thumbHeight, max);
      if (next != modScroll) {
        modScroll = next;
        rebuildButtons();
      }
    }
  }

  private int optionScrollFromThumb(int thumbY, int trackY, int trackHeight, int thumbHeight, int maxScroll) {
    int travel = Math.max(1, trackHeight - thumbHeight);
    int relative = clamp(thumbY - trackY, 0, travel);
    return clamp((int) Math.round(maxScroll * (relative / (double) travel)), 0, maxScroll);
  }

  private int visibleNavRows(LayoutMetrics metrics) {
    return Math.max(1, metrics.navHeight / (navRowHeight() + navRowGap()));
  }

  private int optionScrollbarX(LayoutMetrics metrics) {
    return metrics.contentX + metrics.contentWidth + 6;
  }

  private int navScrollbarX(LayoutMetrics metrics) {
    return metrics.sidebarX + metrics.sidebarWidth - 7;
  }

  private static boolean pointIn(int mouseX, int mouseY, int x, int y, int width, int height) {
    return mouseX >= x && mouseY >= y && mouseX < x + width && mouseY < y + height;
  }

  private void drawContextHelp(int mouseX, int mouseY, LayoutMetrics metrics) {
    ModOption hovered = null;
    DisplayGroup hoveredGroup = null;
    for (Object object : buttonList) {
      if (!(object instanceof GuiButton)) continue;
      GuiButton button = (GuiButton) object;
      if (
        mouseX < button.xPosition ||
        mouseY < button.yPosition ||
        mouseX >= button.xPosition + button.width ||
        mouseY >= button.yPosition + button.height
      ) continue;
      boolean optionControl =
        button instanceof NumericSliderButton ||
        button instanceof OptionCycleButton ||
        button instanceof GroupHeaderButton;
      if (
        optionControl &&
        !pointIn(mouseX, mouseY, metrics.contentX, metrics.viewportY, metrics.contentWidth, metrics.viewportHeight)
      ) continue;
      if (button instanceof NumericSliderButton) {
        hovered = ((NumericSliderButton) button).getOption();
        break;
      }
      if (button instanceof OptionCycleButton) {
        hovered = ((OptionCycleButton) button).getOption();
        break;
      }
      if (button instanceof GroupHeaderButton) {
        String groupId = groupButtonIds.get(button.id);
        hoveredGroup = findVisibleGroup(groupId);
        break;
      }
    }
    if (hovered == null && hoveredGroup == null) return;

    if (hoveredGroup != null) {
      if (hoveredGroup.description == null || hoveredGroup.description.length() == 0) return;
      String clipped = fontRendererObj.trimStringToWidth(
        hoveredGroup.description,
        Math.max(40, metrics.doneX - metrics.contentX - 12)
      );
      drawString(fontRendererObj, clipped, metrics.contentX, metrics.footerY + 10, ModOptionsStyle.mutedTextColor);
      return;
    }

    String description = ModOptionsText.description(hovered);
    String scope = scopeName(hovered.getScope());
    String meta = scope + "  |  " + hovered.getKey();
    int maxWidth = Math.max(40, metrics.doneX - metrics.contentX - 12);
    if (description.length() > 0) {
      String clipped = fontRendererObj.trimStringToWidth(description, maxWidth);
      drawString(fontRendererObj, clipped, metrics.contentX, metrics.footerY + 10, ModOptionsStyle.textColor);
    } else {
      String clipped = fontRendererObj.trimStringToWidth(meta, maxWidth);
      drawString(fontRendererObj, clipped, metrics.contentX, metrics.footerY + 10, ModOptionsStyle.mutedTextColor);
    }
  }

  private String scopeName(OptionScope scope) {
    if (scope == OptionScope.SERVER) {
      return ModOptionsText.ui("modoptions.scope.server", "Server");
    }
    if (scope == OptionScope.COMMON) {
      return ModOptionsText.ui("modoptions.scope.common", "Common");
    }
    return ModOptionsText.ui("modoptions.scope.client", "Client");
  }

  private void closeToParent() {
        String parentName = parent == null ? "" : parent.getClass().getName();
        if (parent != null
                && !parentName.endsWith("GuiIngameModOptions")
                && !parentName.endsWith("GuiModList")) {
            mc.displayGuiScreen(parent);
        } else {
            mc.displayGuiScreen(null);
    }
  }

  private List<DisplayGroup> buildDisplayGroups(String query) {
    List<DisplayGroup> groups = explicitGroups();
    if (shouldAutoGroup(groups)) groups = autoGroups(selectedOptions);

    String needle = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
    if (needle.length() == 0) return groups;

    List<DisplayGroup> filtered = new ArrayList<DisplayGroup>();
    for (DisplayGroup group : groups) {
      List<ModOption> matches = new ArrayList<ModOption>();
      String groupText = group.label.toLowerCase(Locale.ROOT);
      boolean groupMatches = groupText.contains(needle);
      for (ModOption option : group.options) {
        if (groupMatches || matches(option, needle)) matches.add(option);
      }
      if (!matches.isEmpty()) {
        filtered.add(
          new DisplayGroup(
            group.id,
            group.label,
            group.description,
            matches,
            group.collapsedByDefault,
            group.implicitGeneral
          )
        );
      }
    }
    return filtered;
  }

  private List<DisplayGroup> explicitGroups() {
    LinkedHashMap<String, OptionGroup> metadata = new LinkedHashMap<String, OptionGroup>();
    if (selectedMod != null) {
      for (OptionGroup group : OptionRegistry.groups(selectedMod)) {
        metadata.put(group.getId(), group);
      }
    }
    LinkedHashMap<String, List<ModOption>> optionsByGroup = new LinkedHashMap<String, List<ModOption>>();
    for (OptionGroup group : metadata.values()) {
      optionsByGroup.put(group.getId(), new ArrayList<ModOption>());
    }
    for (ModOption option : selectedOptions) {
      String groupId = option.getGroupId();
      List<ModOption> options = optionsByGroup.get(groupId);
      if (options == null) {
        options = new ArrayList<ModOption>();
        optionsByGroup.put(groupId, options);
      }
      options.add(option);
    }

    List<DisplayGroup> result = new ArrayList<DisplayGroup>();
    for (Map.Entry<String, List<ModOption>> entry : optionsByGroup.entrySet()) {
      if (entry.getValue().isEmpty()) continue;
      OptionGroup group = metadata.get(entry.getKey());
      String label;
      String description = "";
      boolean collapsed = false;
      if (group != null) {
        label = ModOptionsText.group(selectedMod, group);
        description = ModOptionsText.groupDescription(group);
        collapsed = group.isCollapsedByDefault();
      } else {
        label = ModOptionsText.prettify(entry.getKey());
      }
      boolean implicitGeneral = "general".equals(entry.getKey()) && metadata.size() <= 1;
      result.add(new DisplayGroup(entry.getKey(), label, description, entry.getValue(), collapsed, implicitGeneral));
    }
    return result;
  }

  private boolean shouldAutoGroup(List<DisplayGroup> groups) {
    return (
      ModOptionsStyle.autoGroupUngrouped &&
      groups.size() == 1 &&
      groups.get(0).implicitGeneral &&
      groups.get(0).options.size() >= 6
    );
  }

  private List<DisplayGroup> autoGroups(List<ModOption> options) {
    Map<String, Integer> counts = new LinkedHashMap<String, Integer>();
    Map<ModOption, String> category = new LinkedHashMap<ModOption, String>();
    for (ModOption option : options) {
      String candidate = firstCategoryWord(ModOptionsText.option(option));
      category.put(option, candidate);
      if (candidate != null) {
        Integer count = counts.get(candidate);
        counts.put(candidate, count == null ? 1 : count + 1);
      }
    }

    LinkedHashMap<String, List<ModOption>> grouped = new LinkedHashMap<String, List<ModOption>>();
    for (ModOption option : options) {
      String candidate = category.get(option);
      String id =
        candidate != null && counts.get(candidate) != null && counts.get(candidate) >= 2
          ? "auto:" + candidate
          : "general";
      List<ModOption> list = grouped.get(id);
      if (list == null) {
        list = new ArrayList<ModOption>();
        grouped.put(id, list);
      }
      list.add(option);
    }

    List<DisplayGroup> result = new ArrayList<DisplayGroup>();
    for (Map.Entry<String, List<ModOption>> entry : grouped.entrySet()) {
      String label = "general".equals(entry.getKey())
        ? ModOptionsText.ui("modoptions.general", "General")
        : ModOptionsText.prettify(entry.getKey().substring("auto:".length()));
      result.add(new DisplayGroup(entry.getKey(), label, "", entry.getValue(), false, false));
    }
    return result.size() > 1 ? result : explicitGroups();
  }

  private String firstCategoryWord(String label) {
    if (label == null) return null;
    String text = label.trim();
    if (text.length() == 0) return null;
    int split = text.indexOf(' ');
    String raw = split < 0 ? text : text.substring(0, split);
    StringBuilder cleaned = new StringBuilder();
    for (int i = 0; i < raw.length(); i++) {
      char c = raw.charAt(i);
      if (Character.isLetterOrDigit(c)) cleaned.append(c);
    }
    String first = cleaned.toString().toLowerCase(Locale.ROOT);
    if (first.length() < 3) return null;
    if (
      "enable".equals(first) ||
      "enabled".equals(first) ||
      "disable".equals(first) ||
      "show".equals(first) ||
      "use".equals(first) ||
      "allow".equals(first)
    ) return null;
    return first;
  }

  private boolean matches(ModOption option, String needle) {
    return (
      ModOptionsText.option(option).toLowerCase(Locale.ROOT).contains(needle) ||
      option.getKey().toLowerCase(Locale.ROOT).contains(needle) ||
      option.displayValue().toLowerCase(Locale.ROOT).contains(needle) ||
      ModOptionsText.description(option).toLowerCase(Locale.ROOT).contains(needle)
    );
  }

  private DisplayGroup findVisibleGroup(String groupId) {
    for (DisplayGroup group : visibleGroups) {
      if (group.id.equals(groupId)) return group;
    }
    return null;
  }

  private boolean isGroupCollapsed(DisplayGroup group) {
    String key = collapseKey(group.id);
    if (collapsedGroups.contains(key)) return true;
    return group.collapsedByDefault && !expandedDefaultGroups.contains(key);
  }

  private String collapseKey(String groupId) {
    return (selectedMod == null ? "" : selectedMod) + "|" + groupId;
  }

  private int measureContent(List<DisplayGroup> groups, int columns, boolean searching) {
    int cursor = 0;
    boolean showHeaders = groups.size() > 1 || (groups.size() == 1 && !groups.get(0).implicitGeneral);
    for (int i = 0; i < groups.size(); i++) {
      DisplayGroup group = groups.get(i);
      if (showHeaders) cursor += groupHeaderHeight() + 3;
      boolean collapsed = !searching && isGroupCollapsed(group);
      if (!collapsed) {
        int rows = (group.options.size() + columns - 1) / columns;
        cursor += rows * (cardHeight() + cardGap());
        if (rows > 0) cursor -= cardGap();
      }
      if (i < groups.size() - 1) cursor += groupGap();
    }
    return cursor;
  }

  private int countOptions(List<DisplayGroup> groups) {
    int count = 0;
    for (DisplayGroup group : groups) count += group.options.size();
    return count;
  }

  private int optionColumns(int availableWidth) {
    int columns = Math.max(1, (availableWidth + COLUMN_GAP) / (MIN_CARD_WIDTH + COLUMN_GAP));
    return Math.min(3, columns);
  }

  private LayoutMetrics metrics() {
    int maxWidth = Math.max(1, width - 8);
    int panelWidth = Math.min(ModOptionsStyle.maxWindowWidth, maxWidth);
    int sidebarWidth = clamp(panelWidth / 5, Math.min(100, panelWidth / 3), 168);
    int panelX = (width - panelWidth) / 2;
    int dividerX = panelX + sidebarWidth;

    int contentUsableWidth = panelWidth - sidebarWidth - 1 - 24;
    int columns = optionColumns(contentUsableWidth);
    List<DisplayGroup> sizingGroups = buildDisplayGroups(searchText);
    int measured = measureContent(sizingGroups, columns, searchText.length() > 0);

    int headerHeight = 34;
    // Reserve a separate metadata line below the search field.
    int toolbarHeight = 130;
    int viewportBottomPadding = 6;
    int footerHeight = 26;
    int chromeHeight = headerHeight + toolbarHeight + viewportBottomPadding + footerHeight;
    int minimumNavRows = 4;
    int minimumNavigationHeight = minimumNavRows * (navRowHeight() + navRowGap());
    int minimumPanelHeight = chromeHeight + 18 + minimumNavigationHeight + 4;
    ForgeModCatalog.Entry selectedEntry = entryFor(selectedMod);
    int desiredViewport = Math.max(
      selectedEntry != null && selectedEntry.forgeConfig && !selectedEntry.libraryConfig ? 74 : 60,
      measured
    );
    int desiredHeight = chromeHeight + desiredViewport;
    int maxHeight = Math.min(ModOptionsStyle.maxWindowHeight, Math.max(160, height - 28));
    int panelHeight = clamp(desiredHeight, Math.max(176, minimumPanelHeight), Math.max(176, maxHeight));
    if (panelHeight > height - 16) panelHeight = Math.max(150, height - 16);

    // Settings windows are top-anchored: short configs grow downward instead of floating
    // in the vertical center of the screen. This keeps navigation and controls in a
    // predictable place when switching between mods with different option counts.
    int panelY = TOP_MARGIN;
    int headerBottom = panelY + headerHeight;
    int footerY = panelY + panelHeight - footerHeight;

    int sidebarX = panelX + 1;
    int navX = sidebarX + 6;
    int navY = headerBottom + 18;
    int navWidth = sidebarWidth - 14;
    int navHeight = Math.max(22, footerY - navY - 4);

    int contentX = dividerX + 12;
    int contentRightPadding = 8;
    int contentWidth = panelX + panelWidth - contentX - contentRightPadding;
    int searchWidth = Math.min(contentWidth, clamp(contentWidth / 3, 122, 190));
    int searchHeight = 18;
    int searchX = contentX + contentWidth - searchWidth;
    int searchY = panelY + 8;
    int viewportY = headerBottom + toolbarHeight;
    int viewportBottom = footerY - viewportBottomPadding;
    int viewportHeight = Math.max(24, viewportBottom - viewportY);

    int doneWidth = 70;
    int doneHeight = 18;
    int doneX = panelX + panelWidth - doneWidth - 9;
    int doneY = footerY + 4;

    return new LayoutMetrics(
      panelX,
      panelY,
      panelWidth,
      panelHeight,
      sidebarX,
      sidebarWidth,
      dividerX,
      headerBottom,
      footerY,
      navX,
      navY,
      navWidth,
      navHeight,
      contentX,
      contentWidth,
      viewportY,
      viewportBottom,
      viewportHeight,
      searchX,
      searchY,
      searchWidth,
      searchHeight,
      doneX,
      doneY,
      doneWidth,
      doneHeight
    );
  }

  private int navRowHeight() {
    return 26;
  }

  private int navRowGap() {
    return ModOptionsStyle.compactLayout ? 2 : 3;
  }

  private int groupHeaderHeight() {
    return ModOptionsStyle.compactLayout ? 18 : 21;
  }

  private int groupGap() {
    return ModOptionsStyle.compactLayout ? 8 : 10;
  }

  private int cardHeight() {
    return ModOptionsStyle.compactLayout ? 29 : 36;
  }

  private int cardGap() {
    return ModOptionsStyle.compactLayout ? 4 : 6;
  }

  private static boolean intersects(int y, int height, int top, int bottom) {
    return y + height > top && y < bottom;
  }

  private static int clamp(int value, int min, int max) {
    return Math.max(min, Math.min(max, value));
  }

  private static final class DisplayGroup {

    final String id;
    final String label;
    final String description;
    final List<ModOption> options;
    final boolean collapsedByDefault;
    final boolean implicitGeneral;

    DisplayGroup(
      String id,
      String label,
      String description,
      List<ModOption> options,
      boolean collapsedByDefault,
      boolean implicitGeneral
    ) {
      this.id = id;
      this.label = label;
      this.description = description;
      this.options = options;
      this.collapsedByDefault = collapsedByDefault;
      this.implicitGeneral = implicitGeneral;
    }
  }

  private static final class LayoutMetrics {

    final int panelX, panelY, panelWidth, panelHeight;
    final int sidebarX, sidebarWidth, contentDividerX, headerBottom, footerY;
    final int navX, navY, navWidth, navHeight;
    final int contentX, contentWidth, viewportY, viewportBottom, viewportHeight;
    final int searchX, searchY, searchWidth, searchHeight;
    final int doneX, doneY, doneWidth, doneHeight;

    LayoutMetrics(
      int panelX,
      int panelY,
      int panelWidth,
      int panelHeight,
      int sidebarX,
      int sidebarWidth,
      int contentDividerX,
      int headerBottom,
      int footerY,
      int navX,
      int navY,
      int navWidth,
      int navHeight,
      int contentX,
      int contentWidth,
      int viewportY,
      int viewportBottom,
      int viewportHeight,
      int searchX,
      int searchY,
      int searchWidth,
      int searchHeight,
      int doneX,
      int doneY,
      int doneWidth,
      int doneHeight
    ) {
      this.panelX = panelX;
      this.panelY = panelY;
      this.panelWidth = panelWidth;
      this.panelHeight = panelHeight;
      this.sidebarX = sidebarX;
      this.sidebarWidth = sidebarWidth;
      this.contentDividerX = contentDividerX;
      this.headerBottom = headerBottom;
      this.footerY = footerY;
      this.navX = navX;
      this.navY = navY;
      this.navWidth = navWidth;
      this.navHeight = navHeight;
      this.contentX = contentX;
      this.contentWidth = contentWidth;
      this.viewportY = viewportY;
      this.viewportBottom = viewportBottom;
      this.viewportHeight = viewportHeight;
      this.searchX = searchX;
      this.searchY = searchY;
      this.searchWidth = searchWidth;
      this.searchHeight = searchHeight;
      this.doneX = doneX;
      this.doneY = doneY;
      this.doneWidth = doneWidth;
      this.doneHeight = doneHeight;
    }
  }
}
