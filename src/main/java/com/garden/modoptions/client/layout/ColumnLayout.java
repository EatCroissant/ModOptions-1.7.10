package com.garden.modoptions.client.layout;

import net.minecraft.client.gui.GuiButton;

/** Small retained-mode column layout for the 1.7.10 GUI controls. */
public final class ColumnLayout {
    private final int x;
    private final int width;
    private final int top;
    private final int bottom;
    private final int rowHeight;
    private final int gap;
    private int cursor;

    public ColumnLayout(int x, int top, int width, int bottom,
            int rowHeight, int gap) {
        this.x = x;
        this.width = width;
        this.top = top;
        this.bottom = bottom;
        this.rowHeight = rowHeight;
        this.gap = gap;
        this.cursor = top;
    }

    public boolean place(GuiButton button) {
        int actualHeight = Math.max(rowHeight, button.height);
        if (cursor + actualHeight > bottom) return false;
        button.xPosition = x;
        button.yPosition = cursor;
        button.width = width;
        button.height = actualHeight;
        cursor += actualHeight + gap;
        return true;
    }

    public int getTop() { return top; }
    public int getBottom() { return bottom; }
}
