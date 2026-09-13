package com.garden.modoptions.client;

import com.garden.modoptions.ModOptionsStyle;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.util.ResourceLocation;
import net.minecraft.client.renderer.Tessellator;
import org.lwjgl.opengl.GL11;

/** Generic compact button. Can also act as a selected/left-aligned navigation item. */
public class TexturedButton extends GuiButton {
    private boolean selected;
    private boolean leftAligned;
    private ResourceLocation preview;
    private int previewWidth = 1;
    private int previewHeight = 1;

    public TexturedButton(int id, int x, int y, int width, int height, String text) {
        super(id, x, y, width, height, text);
    }

    public TexturedButton selected(boolean selected) {
        this.selected = selected;
        return this;
    }

    public TexturedButton leftAligned(boolean leftAligned) {
        this.leftAligned = leftAligned;
        return this;
    }

    public TexturedButton preview(ResourceLocation preview) {
        this.preview = preview;
        return this;
    }

    public TexturedButton preview(ResourceLocation preview, int width, int height) {
        this.preview = preview;
        this.previewWidth = Math.max(1, width);
        this.previewHeight = Math.max(1, height);
        return this;
    }

    @Override
    public void drawButton(Minecraft mc, int mouseX, int mouseY) {
        if (!visible) return;
        boolean hovered = mouseX >= xPosition && mouseY >= yPosition
                && mouseX < xPosition + width && mouseY < yPosition + height;
        ModOptionsTextures.drawSurface(xPosition, yPosition,
                width, height, hovered, selected, enabled);
        // The icon occupies 24px; leave only a small gap before the label.
        int textOffset = preview == null ? 0 : 21;
        if (preview != null) {
            try {
                GL11.glColor4f(1.0f, 1.0f, 1.0f, 1.0f);
                mc.getTextureManager().bindTexture(preview);
                Tessellator tessellator = Tessellator.instance;
                float scale = Math.min(24.0f / previewWidth, 24.0f / previewHeight);
                int drawWidth = Math.max(1, Math.round(previewWidth * scale));
                int drawHeight = Math.max(1, Math.round(previewHeight * scale));
                int drawX = xPosition + 3 + (24 - drawWidth) / 2;
                int drawY = yPosition + 1 + (24 - drawHeight) / 2;
                tessellator.startDrawingQuads();
                tessellator.addVertexWithUV(drawX, drawY + drawHeight, zLevel, 0, 1);
                tessellator.addVertexWithUV(drawX + drawWidth, drawY + drawHeight, zLevel, 1, 1);
                tessellator.addVertexWithUV(drawX + drawWidth, drawY, zLevel, 1, 0);
                tessellator.addVertexWithUV(drawX, drawY, zLevel, 0, 0);
                tessellator.draw();
                GL11.glColor4f(1.0f, 1.0f, 1.0f, 1.0f);
            } catch (Throwable ignored) {
                preview = null;
                textOffset = 0;
            }
        }
        String text = mc.fontRenderer.trimStringToWidth(displayString, width - 16 - textOffset);
        int color = enabled ? (selected ? ModOptionsStyle.textColor : ModOptionsStyle.textColor)
                : ModOptionsStyle.disabledTextColor;
        int textY = yPosition + (height - 8) / 2;
        if (leftAligned) {
            drawString(mc.fontRenderer, text, xPosition + 9 + textOffset, textY, color);
        } else {
            drawCenteredString(mc.fontRenderer, text,
                    xPosition + width / 2, textY, color);
        }
    }
}
