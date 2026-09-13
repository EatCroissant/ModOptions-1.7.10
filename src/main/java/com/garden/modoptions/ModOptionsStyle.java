package com.garden.modoptions;

/** Shared visual configuration for every screen using the Mod Options library. */
public final class ModOptionsStyle {
    public static int maxWindowWidth = 1040;
    public static int maxWindowHeight = 620;
    public static boolean compactLayout = true;
    public static boolean transparentPanels = true;
    public static boolean autoGroupUngrouped = true;

    // Semantic palette: surfaces are deliberately quiet; accent is reserved for state.
    public static int backgroundColor = 0xCC0D1020;
    public static int panelColor = 0xF0171C2C;
    public static int sidebarColor = 0xC90F1424;
    public static int surfaceColor = 0xC6252B3D;
    public static int surfaceHoverColor = 0xDD30384D;
    public static int surfaceSelectedColor = 0xEE263A49;
    public static int borderColor = 0xA94B5369;
    public static int dividerColor = 0x7A41485D;
    public static int accentColor = 0xFFF0A15A;
    public static int accentSoftColor = 0xA13A7888;
    public static int textColor = 0xFFF4F5F8;
    public static int mutedTextColor = 0xFF9DA6B8;
    public static int disabledTextColor = 0xFF6E7484;

    private ModOptionsStyle() {}
}
