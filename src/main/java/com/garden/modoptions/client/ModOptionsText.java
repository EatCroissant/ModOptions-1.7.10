package com.garden.modoptions.client;

import com.garden.modoptions.ModOption;
import com.garden.modoptions.OptionGroup;
import net.minecraft.client.resources.I18n;

public final class ModOptionsText {
    public static String translate(String key) { return I18n.format(key); }

    public static String ui(String key, String fallback) {
        String value = I18n.format(key);
        return value.equals(key) ? fallback : value;
    }

    public static String translateOrLiteral(String value) {
        if (value == null || value.length() == 0) return "";
        String translated = I18n.format(value);
        if (!translated.equals(value)) return translated;
        return value;
    }

    public static String option(ModOption option) { return translateOrLiteral(option.getLabel()); }

    public static String description(ModOption option) {
        return translateOrLiteral(option.getDescription());
    }

    public static String mod(String modId) {
        String key = "modoptions.mod." + modId;
        String value = translate(key);
        return value.equals(key) ? prettify(modId) : value;
    }

    public static String group(String modId, OptionGroup group) {
        String value = translateOrLiteral(group.getLabel());
        if (value.equals(group.getLabel()) && value.startsWith("modoptions.group.")) {
            return prettify(group.getId());
        }
        return value;
    }

    public static String groupDescription(OptionGroup group) {
        return translateOrLiteral(group.getDescription());
    }

    public static String prettify(String value) {
        if (value == null || value.length() == 0) return "";
        StringBuilder out = new StringBuilder();
        char previous = 0;
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c == '_' || c == '-' || c == '.') {
                if (out.length() > 0 && out.charAt(out.length() - 1) != ' ') out.append(' ');
                previous = c;
                continue;
            }
            if (Character.isUpperCase(c) && Character.isLowerCase(previous)) out.append(' ');
            out.append(c);
            previous = c;
        }
        String text = out.toString().trim();
        if (text.length() == 0) return value;
        return Character.toUpperCase(text.charAt(0)) + text.substring(1);
    }

    private ModOptionsText() {}
}
