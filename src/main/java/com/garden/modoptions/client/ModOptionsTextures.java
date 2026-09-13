package com.garden.modoptions.client;

import com.garden.modoptions.ModOptionsStyle;
import net.minecraft.client.gui.Gui;

/**
 * Lightweight primitives used by the settings UI.
 *
 * The controls intentionally use flat semantic surfaces instead of stretching the
 * vanilla button texture. That keeps the UI readable at arbitrary GUI scales and
 * makes the library feel closer to a compact desktop/web settings panel.
 */
public final class ModOptionsTextures extends Gui {
    public static void drawPanel(Gui gui, int x, int y, int width, int height) {
        int fill = ModOptionsStyle.transparentPanels
                ? (ModOptionsStyle.panelColor & 0x00FFFFFF) | 0xD0000000
                : ModOptionsStyle.panelColor;
        drawRect(x, y, x + width, y + height, fill);
        drawRect(x, y, x + width, y + 1, ModOptionsStyle.borderColor);
        drawRect(x, y + height - 1, x + width, y + height, 0xA9080A12);
        drawRect(x, y, x + 1, y + height, ModOptionsStyle.borderColor);
        drawRect(x + width - 1, y, x + width, y + height, 0xA9080A12);
    }

    public static void drawSurface(int x, int y, int width, int height,
            boolean hovered, boolean selected, boolean enabled) {
        int color = !enabled ? 0x81191E2B
                : selected ? 0xD0253040
                : hovered ? 0xBE293143
                : 0x9E202636;
        drawRect(x, y, x + width, y + height, color);

        // A very light outline is enough to separate rows without turning every
        // setting into a giant Minecraft-style button.
        drawRect(x, y + height - 1, x + width, y + height,
                selected ? 0x924B6570 : 0x463A4152);
        if (selected) {
            drawRect(x, y, x + 2, y + height, ModOptionsStyle.accentColor);
        } else if (hovered && enabled) {
            drawRect(x, y, x + width, y + 1, 0x764E586E);
        }
    }

    public static void drawButtonBackground(Gui gui, int x, int y, int width, int height,
            boolean hovered, boolean pressed, boolean enabled) {
        drawSurface(x, y, width, height, hovered || pressed, false, enabled);
    }

    /**
     * Draws a compact overlay scrollbar. The track is deliberately quiet; only
     * the thumb becomes prominent on hover/drag, like a modern overlay scrollbar.
     */
    public static void drawScrollbar(Gui gui, int x, int y, int width, int height,
            int scroll, int total, int visible, boolean hovered, boolean dragging) {
        if (height <= 0 || total <= visible) return;

        int thumbHeight = scrollbarThumbHeight(height, total, visible);
        int thumbY = scrollbarThumbY(y, height, thumbHeight, scroll, total, visible);
        int center = x + width / 2;

        int trackColor = hovered || dragging ? 0x4F4A5366 : 0x253D4454;
        drawRect(center, y, center + 1, y + height, trackColor);

        int thumbWidth = dragging ? Math.max(4, width) : hovered ? Math.max(3, width - 1) : 2;
        int thumbX = center - thumbWidth / 2;
        int thumbColor = dragging ? ModOptionsStyle.accentColor
                : hovered ? 0xD09AA7B8 : 0x92909AAB;
        drawRect(thumbX, thumbY, thumbX + thumbWidth, thumbY + thumbHeight, thumbColor);
    }

    /** Backwards-compatible helper for callers that do not provide hover state. */
    public static void drawScrollbar(Gui gui, int x, int y, int width, int height,
            int scroll, int total, int visible) {
        drawScrollbar(gui, x, y, width, height, scroll, total, visible, false, false);
    }

    public static int scrollbarThumbHeight(int height, int total, int visible) {
        if (height <= 0 || total <= visible) return height;
        return Math.min(height, Math.max(18, height * visible / Math.max(1, total)));
    }

    public static int scrollbarThumbY(int y, int height, int thumbHeight,
            int scroll, int total, int visible) {
        int travel = Math.max(0, height - thumbHeight);
        int maxScroll = Math.max(1, total - visible);
        int clamped = Math.max(0, Math.min(scroll, maxScroll));
        return y + travel * clamped / maxScroll;
    }

    public static void drawDivider(int x1, int y, int x2) {
        drawRect(x1, y, x2, y + 1, ModOptionsStyle.dividerColor);
    }

    private ModOptionsTextures() {}
}
