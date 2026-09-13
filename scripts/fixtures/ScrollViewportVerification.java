package com.garden.modoptions.client;

public final class ScrollViewportVerification {
    public static void main(String[] args) {
        ScrollViewport viewport = new ScrollViewport(20, 40, 200, 120, 7, 8);

        check(viewport.contains(20, 40), "top-left pixel must be interactive");
        check(viewport.contains(219, 159), "bottom-right pixel must be interactive");
        check(!viewport.contains(20, 39), "header must not be interactive");
        check(!viewport.contains(220, 159), "right edge must be clipped");
        check(!viewport.contains(219, 160), "footer must not be interactive");

        check(viewport.intersects(30, 11), "partially visible top row must exist");
        check(!viewport.intersects(30, 10), "fully hidden top row must be removed");
        check(viewport.intersects(159, 10), "partially visible bottom row must exist");
        check(!viewport.intersects(160, 10), "fully hidden bottom row must be removed");

        check(viewport.maxScroll(300) == 180, "last content pixel must be reachable");
        check(viewport.maxScroll(100) == 0, "short content must not scroll");
        check(viewport.trackY() == 47, "track top inset must be shared");
        check(viewport.trackHeight() == 105, "track height must include both insets");

        int titleOnly = ModHeaderLayout.contentHeight(false, 0);
        int compactHeader = ModHeaderLayout.contentHeight(false, 1);
        int previewHeader = ModHeaderLayout.contentHeight(true, 1);
        check(titleOnly == 38, "title-only header must stay compact");
        check(compactHeader == 52, "metadata must reserve only its text height");
        check(previewHeader == 86, "preview must reserve its 64-pixel footprint");
        check(compactHeader < previewHeader, "missing preview must release its space");

        System.out.println("Scroll viewport verification passed.");
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private ScrollViewportVerification() {}
}
