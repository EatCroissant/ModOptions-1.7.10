package com.garden.modoptions;

public final class ModOptions {
    public static void register(String modId, ModOption option) {
        OptionRegistry.register(modId, option);
    }

    public static void register(String modId, String groupId, ModOption option) {
        OptionRegistry.register(modId, groupId, option);
    }

    public static OptionGroup group(String modId, String groupId, String label) {
        OptionGroup group = new OptionGroup(groupId, label);
        OptionRegistry.registerGroup(modId, group);
        return group;
    }

    public static void setWindowWidth(int width) {
        ModOptionsStyle.maxWindowWidth = Math.max(480, Math.min(1600, width));
    }

    public static void setWindowHeight(int height) {
        ModOptionsStyle.maxWindowHeight = Math.max(220, Math.min(1000, height));
    }

    public static void setCompactLayout(boolean compact) {
        ModOptionsStyle.compactLayout = compact;
    }

    public static void setAutoGrouping(boolean enabled) {
        ModOptionsStyle.autoGroupUngrouped = enabled;
    }

    public static void setTransparentPanels(boolean transparent) {
        ModOptionsStyle.transparentPanels = transparent;
    }

    private ModOptions() {}
}
