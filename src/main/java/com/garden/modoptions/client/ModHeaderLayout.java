package com.garden.modoptions.client;

/** Vertical geometry for the scrollable mod identity block. */
final class ModHeaderLayout {
    static final int MAX_METADATA_LINES = 4;

    static int contentHeight(boolean hasPreview, int metadataLines) {
        int lines = Math.max(0, Math.min(MAX_METADATA_LINES, metadataLines));
        if (hasPreview) return Math.max(86, 42 + lines * 10);
        return lines == 0 ? 38 : 42 + lines * 10;
    }

    static int titleOffset(boolean hasPreview) {
        return hasPreview ? 14 : 12;
    }

    static int metadataOffset(boolean hasPreview) {
        return hasPreview ? 34 : 30;
    }

    private ModHeaderLayout() {}
}
