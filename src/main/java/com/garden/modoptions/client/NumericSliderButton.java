package com.garden.modoptions.client;

import com.garden.modoptions.ModOption;
import com.garden.modoptions.ModOptionsStyle;
import com.garden.modoptions.NumericOption;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import org.lwjgl.input.Mouse;

/** Compact numeric row with a dedicated slider hit-area. */
public final class NumericSliderButton extends GuiButton {
    private static final int HORIZONTAL_PADDING = 9;
    private static final int SLIDER_HIT_HEIGHT = 13;

    private final ModOption option;
    private final NumericOption numeric;
    private boolean dragging;

    public NumericSliderButton(int id, int x, int y, int width, int height,
            ModOption option, NumericOption numeric) {
        super(id, x, y, width, height, "");
        this.option = option;
        this.numeric = numeric;
    }

    public ModOption getOption() { return option; }
    public boolean isDragging() { return dragging; }

    @Override
    public void drawButton(Minecraft mc, int mouseX, int mouseY) {
        if (!visible) return;

        // GuiButton normally invokes mouseDragged from drawButton. Because this
        // control owns its rendering we must preserve that behavior ourselves.
        if (dragging) {
            if (Mouse.isButtonDown(0)) updateValue(mouseX);
            else dragging = false;
        }

        boolean hovered = inside(mouseX, mouseY);
        boolean sliderHovered = insideSlider(mouseX, mouseY);
        ModOptionsTextures.drawSurface(xPosition, yPosition, width, height,
                hovered, dragging, enabled);

        String value = option.displayValue();
        int valueWidth = mc.fontRenderer.getStringWidth(value);
        String label = mc.fontRenderer.trimStringToWidth(ModOptionsText.option(option),
                Math.max(24, width - valueWidth - 30));
        int topY = yPosition + 5;
        drawString(mc.fontRenderer, label, xPosition + HORIZONTAL_PADDING, topY,
                enabled ? ModOptionsStyle.textColor : ModOptionsStyle.disabledTextColor);
        drawString(mc.fontRenderer, value,
                xPosition + width - valueWidth - HORIZONTAL_PADDING, topY,
                dragging ? ModOptionsStyle.accentColor
                        : enabled ? ModOptionsStyle.textColor : ModOptionsStyle.disabledTextColor);

        int left = sliderLeft();
        int right = sliderRight();
        int barY = sliderY();
        int trackColor = sliderHovered || dragging ? 0xB7434C60 : 0x8D343B4B;
        drawRect(left, barY, right, barY + 2, trackColor);

        int filled = left + (int) Math.round((right - left) * normalizedValue());
        if (filled > left) {
            drawRect(left, barY, filled, barY + 2,
                    dragging ? ModOptionsStyle.accentColor : ModOptionsStyle.accentSoftColor);
        }

        int thumbHalf = dragging ? 3 : 2;
        int thumbTop = barY - (dragging ? 4 : 3);
        int thumbBottom = barY + (dragging ? 6 : 5);
        drawRect(filled - thumbHalf, thumbTop, filled + thumbHalf + 1, thumbBottom,
                enabled ? (sliderHovered || dragging
                        ? ModOptionsStyle.accentColor : 0xFFD7DBE3)
                        : ModOptionsStyle.disabledTextColor);
    }

    private double normalizedValue() {
        double range = numeric.maximum() - numeric.minimum();
        if (range <= 0.0) return 0.0;
        double value = (numeric.numericValue() - numeric.minimum()) / range;
        return Math.max(0.0, Math.min(1.0, value));
    }

    @Override
    public boolean mousePressed(Minecraft mc, int mouseX, int mouseY) {
        if (!enabled || !visible || !insideSlider(mouseX, mouseY)) return false;
        dragging = true;
        updateValue(mouseX);
        return true;
    }

    @Override
    public void mouseDragged(Minecraft mc, int mouseX, int mouseY) {
        if (dragging) updateValue(mouseX);
    }

    @Override
    public void mouseReleased(int mouseX, int mouseY) {
        dragging = false;
    }

    private boolean inside(int mouseX, int mouseY) {
        return mouseX >= xPosition && mouseY >= yPosition
                && mouseX < xPosition + width && mouseY < yPosition + height;
    }

    private boolean insideSlider(int mouseX, int mouseY) {
        int top = yPosition + height - SLIDER_HIT_HEIGHT;
        return mouseX >= sliderLeft() - 3 && mouseX <= sliderRight() + 3
                && mouseY >= top && mouseY < yPosition + height;
    }

    private int sliderLeft() { return xPosition + HORIZONTAL_PADDING; }
    private int sliderRight() { return xPosition + width - HORIZONTAL_PADDING; }
    private int sliderY() { return yPosition + height - 7; }

    private void updateValue(int mouseX) {
        int left = sliderLeft();
        int right = sliderRight();
        double fraction = Math.max(0.0, Math.min(1.0,
                (mouseX - left) / (double) Math.max(1, right - left)));
        numeric.setNumericValue(numeric.minimum()
                + (numeric.maximum() - numeric.minimum()) * fraction);
    }
}
