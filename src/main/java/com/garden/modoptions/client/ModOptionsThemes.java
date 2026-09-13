package com.garden.modoptions.client;

import com.garden.modoptions.EnumOption;
import com.garden.modoptions.ModOptions;
import com.garden.modoptions.ModOptionsStyle;
import com.garden.modoptions.OptionScope;

/** Registers this library's appearance controls in its own options catalog. */
final class ModOptionsThemes {
    private static EnumOption<Theme> theme;

    static void register() {
        ModOptions.group("modoptions", "appearance", "modoptions.group.modoptions.appearance")
                .description("modoptions.group.modoptions.appearance.description")
                .order(10);

        theme = new EnumOption<Theme>(
                "theme",
                "modoptions.option.theme",
                Theme.values(),
                0,
                OptionScope.CLIENT,
                new Runnable() {
                    @Override public void run() { theme.get().apply(); }
                });
        theme.description("modoptions.option.theme.description").order(10);
        ModOptions.register("modoptions", "appearance", theme);
    }

    private enum Theme {
        DARK("modoptions.theme.dark", 0xF0171C2C, 0xC90F1424, 0xFFF0A15A),
        STEEL("modoptions.theme.steel", 0xF01D222A, 0xC9141922, 0xFF9DAFC4),
        GOLD("modoptions.theme.gold", 0xF02A2118, 0xC91E1710, 0xFFFFC56B);

        private final String label;
        private final int panel;
        private final int sidebar;
        private final int accent;

        Theme(String label, int panel, int sidebar, int accent) {
            this.label = label;
            this.panel = panel;
            this.sidebar = sidebar;
            this.accent = accent;
        }

        void apply() {
            ModOptionsStyle.panelColor = panel;
            ModOptionsStyle.sidebarColor = sidebar;
            ModOptionsStyle.accentColor = accent;
        }

        @Override public String toString() {
            return ModOptionsText.translateOrLiteral(label);
        }
    }

    private ModOptionsThemes() {}
}
