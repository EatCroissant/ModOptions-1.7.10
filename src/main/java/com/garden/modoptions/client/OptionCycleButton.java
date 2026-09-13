package com.garden.modoptions.client;

import com.garden.modoptions.BooleanOption;
import com.garden.modoptions.ModOption;
import com.garden.modoptions.ModOptionsStyle;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;

/** Compact row for boolean and enum/cycle options. */
public final class OptionCycleButton extends GuiButton {
    private final ModOption option;

    public OptionCycleButton(int id, int x, int y, int width, int height, ModOption option) {
        super(id, x, y, width, height, "");
        this.option = option;
    }

    public ModOption getOption() { return option; }

    @Override
    public void drawButton(Minecraft mc, int mouseX, int mouseY) {
        if (!visible) return;
        boolean hovered = mouseX >= xPosition && mouseY >= yPosition
                && mouseX < xPosition + width && mouseY < yPosition + height;
        ModOptionsTextures.drawSurface(xPosition, yPosition, width, height,
                hovered, false, enabled);

        String label = ModOptionsText.option(option);
        int textY = yPosition + (height - 8) / 2;

        if (option instanceof BooleanOption) {
            drawBoolean(mc, hovered, label, textY);
        } else {
            drawCycle(mc, hovered, label, textY);
        }
    }

    private void drawBoolean(Minecraft mc, boolean hovered, String label, int textY) {
        boolean on = ((BooleanOption) option).get();
        String state = on ? "ON" : "OFF";
        int stateW = mc.fontRenderer.getStringWidth(state);
        int switchW = 24;
        int switchH = 10;
        int switchX = xPosition + width - switchW - 9;
        int switchY = yPosition + (height - switchH) / 2;
        int stateX = switchX - stateW - 7;
        int labelMax = Math.max(24, stateX - xPosition - 16);
        String clippedLabel = mc.fontRenderer.trimStringToWidth(label, labelMax);

        drawString(mc.fontRenderer, clippedLabel, xPosition + 9, textY,
                enabled ? ModOptionsStyle.textColor : ModOptionsStyle.disabledTextColor);
        drawString(mc.fontRenderer, state, stateX, textY,
                !enabled ? ModOptionsStyle.disabledTextColor
                        : on ? ModOptionsStyle.accentColor : ModOptionsStyle.mutedTextColor);

        int track = !enabled ? 0x7D303544
                : on ? (hovered ? 0xD25B7E86 : ModOptionsStyle.accentSoftColor)
                : hovered ? 0xC0444B5C : 0x9B393F4E;
        drawRect(switchX, switchY + 1, switchX + switchW, switchY + switchH - 1, track);
        drawRect(switchX + 1, switchY, switchX + switchW - 1, switchY + switchH, track);

        int knobW = 7;
        int knobX = on ? switchX + switchW - knobW - 2 : switchX + 2;
        int knobColor = !enabled ? ModOptionsStyle.disabledTextColor
                : on ? ModOptionsStyle.accentColor : 0xFFC0C6D0;
        drawRect(knobX, switchY + 2, knobX + knobW, switchY + switchH - 2, knobColor);
    }

    private void drawCycle(Minecraft mc, boolean hovered, String label, int textY) {
        String value = option.displayValue();
        String arrow = ">";
        int arrowW = mc.fontRenderer.getStringWidth(arrow);
        int valueMax = Math.max(32, Math.min(width / 2, width - 90));
        String clippedValue = mc.fontRenderer.trimStringToWidth(value, valueMax);
        int valueW = mc.fontRenderer.getStringWidth(clippedValue);
        int arrowX = xPosition + width - arrowW - 9;
        int valueX = arrowX - valueW - 6;
        int labelMax = Math.max(24, valueX - xPosition - 16);
        String clippedLabel = mc.fontRenderer.trimStringToWidth(label, labelMax);

        drawString(mc.fontRenderer, clippedLabel, xPosition + 9, textY,
                enabled ? ModOptionsStyle.textColor : ModOptionsStyle.disabledTextColor);
        drawString(mc.fontRenderer, clippedValue, valueX, textY,
                enabled ? (hovered ? ModOptionsStyle.textColor : ModOptionsStyle.mutedTextColor)
                        : ModOptionsStyle.disabledTextColor);
        drawString(mc.fontRenderer, arrow, arrowX, textY,
                enabled ? (hovered ? ModOptionsStyle.accentColor : ModOptionsStyle.mutedTextColor)
                        : ModOptionsStyle.disabledTextColor);
    }
}
