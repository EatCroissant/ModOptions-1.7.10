package com.garden.modoptions.client;

import com.garden.modoptions.ModOptionsCommonProxy;
import net.minecraftforge.common.MinecraftForge;

public final class ModOptionsClientProxy extends ModOptionsCommonProxy {
    @Override
    public void init() {
        ModOptionsThemes.register();
        MinecraftForge.EVENT_BUS.register(new ModOptionsClientEvents());
    }
}
