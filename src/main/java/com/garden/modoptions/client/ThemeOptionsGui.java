package com.garden.modoptions.client;

import com.garden.modoptions.ModOptionsStyle;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;

public final class ThemeOptionsGui extends GuiScreen {
  private final GuiScreen parent;
  public ThemeOptionsGui(GuiScreen parent) { this.parent = parent; }

  @Override public void initGui() {
    int x = width / 2 - 80, y = height / 2 - 34;
    buttonList.add(new GuiButton(1, x, y, 160, 20, "Dark"));
    buttonList.add(new GuiButton(2, x, y + 24, 160, 20, "Steel"));
    buttonList.add(new GuiButton(3, x, y + 48, 160, 20, "Gold"));
    buttonList.add(new GuiButton(4, x, y + 78, 160, 20, "Done"));
  }

  @Override protected void actionPerformed(GuiButton button) {
    if (button.id == 4) { mc.displayGuiScreen(parent); return; }
    if (button.id == 1) apply(0xF0171C2C, 0xC90F1424, 0xFFF0A15A);
    if (button.id == 2) apply(0xF01D222A, 0xC9141922, 0xFF9DAFC4);
    if (button.id == 3) apply(0xF02A2118, 0xC91E1710, 0xFFFFC56B);
  }

  private static void apply(int panel, int sidebar, int accent) {
    ModOptionsStyle.panelColor = panel;
    ModOptionsStyle.sidebarColor = sidebar;
    ModOptionsStyle.accentColor = accent;
  }

  @Override protected void keyTyped(char typedChar, int keyCode) {
    if (keyCode == 1) { mc.displayGuiScreen(parent); return; }
    super.keyTyped(typedChar, keyCode);
  }

  @Override public void drawScreen(int mouseX, int mouseY, float partialTicks) {
    drawDefaultBackground();
    drawCenteredString(fontRendererObj, "Mod Options Theme", width / 2, height / 2 - 62, 0xFFFFFFFF);
    super.drawScreen(mouseX, mouseY, partialTicks);
  }
}
