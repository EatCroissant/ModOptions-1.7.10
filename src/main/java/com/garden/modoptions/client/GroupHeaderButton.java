package com.garden.modoptions.client;

import com.garden.modoptions.ModOptionsStyle;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;

/** Collapsible section heading. */
public final class GroupHeaderButton extends GuiButton {
    private final boolean collapsed;
    private final int optionCount;

    public GroupHeaderButton(int id, int x, int y, int width, int height,
            String text, boolean collapsed, int optionCount) {
        super(id, x, y, width, height, text);
        this.collapsed = collapsed;
        this.optionCount = optionCount;
    }

    @Override
    public void drawButton(Minecraft mc, int mouseX, int mouseY) {
        if (!visible) return;
        boolean hovered = mouseX >= xPosition && mouseY >= yPosition
                && mouseX < xPosition + width && mouseY < yPosition + height;
        if (hovered) drawRect(xPosition, yPosition, xPosition + width, yPosition + height,
                0x3AFFFFFF);
        int textY = yPosition + (height - 8) / 2;
        drawString(mc.fontRenderer, collapsed ? ">" : "v", xPosition + 2, textY,
                ModOptionsStyle.accentColor);
        drawString(mc.fontRenderer, displayString, xPosition + 13, textY,
                ModOptionsStyle.textColor);
        String count = Integer.toString(optionCount);
        drawString(mc.fontRenderer, count,
                xPosition + width - mc.fontRenderer.getStringWidth(count) - 2,
                textY, ModOptionsStyle.mutedTextColor);
        int lineStart = xPosition + 18 + mc.fontRenderer.getStringWidth(displayString);
        if (lineStart < xPosition + width - 28) {
            drawRect(lineStart + 7, yPosition + height / 2,
                    xPosition + width - 25, yPosition + height / 2 + 1,
                    ModOptionsStyle.dividerColor);
        }
    }
}
