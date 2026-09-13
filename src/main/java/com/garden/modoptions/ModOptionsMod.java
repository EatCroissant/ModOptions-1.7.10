package com.garden.modoptions;

import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.SidedProxy;
import cpw.mods.fml.common.event.FMLInitializationEvent;

@Mod(modid = ModOptionsMod.MODID, name = "Mod Options", version = "0.0.4",
        acceptedMinecraftVersions = "[1.7.10]")
public final class ModOptionsMod {
    public static final String MODID = "modoptions";

    @SidedProxy(clientSide = "com.garden.modoptions.client.ModOptionsClientProxy",
            serverSide = "com.garden.modoptions.ModOptionsCommonProxy")
    public static ModOptionsCommonProxy proxy;

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        proxy.init();
    }
}
