package com.garden.modoptions.client;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.client.Minecraft;
import net.minecraftforge.client.event.GuiScreenEvent;

@SideOnly(Side.CLIENT)
public final class ModOptionsClientEvents {
    @SubscribeEvent
    public void onModOptionsScreen(GuiScreenEvent.InitGuiEvent.Post event) {
        String screenName = event.gui.getClass().getName();
        if ("cpw.mods.fml.client.GuiIngameModOptions".equals(screenName)
                || "cpw.mods.fml.client.GuiModList".equals(screenName)) {
            Minecraft.getMinecraft().displayGuiScreen(new ModOptionsGui(event.gui));
        }
    }
}
