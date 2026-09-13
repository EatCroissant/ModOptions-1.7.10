package com.garden.modoptions.client;

/** Shared clipping and input bounds for a scrollable GUI region. */
final class ScrollViewport {
    private final int x;
    private final int y;
    private final int width;
    private final int height;
    private final int trackTopInset;
    private final int trackBottomInset;

    ScrollViewport(int x, int y, int width, int height) {
        this(x, y, width, height, 0, 0);
    }

    ScrollViewport(int x, int y, int width, int height,
            int trackTopInset, int trackBottomInset) {
        this.x = x;
        this.y = y;
        this.width = Math.max(0, width);
        this.height = Math.max(0, height);
        this.trackTopInset = Math.max(0, trackTopInset);
        this.trackBottomInset = Math.max(0, trackBottomInset);
    }

    int x() { return x; }
    int y() { return y; }
    int width() { return width; }
    int height() { return height; }
    int bottom() { return y + height; }

    boolean contains(int pointX, int pointY) {
        return pointX >= x && pointY >= y
                && pointX < x + width && pointY < y + height;
    }

    boolean intersects(int elementY, int elementHeight) {
        return elementY + elementHeight > y && elementY < y + height;
    }

    int maxScroll(int contentHeight) {
        return Math.max(0, contentHeight - height);
    }

    int trackY() {
        return y + Math.min(height, trackTopInset);
    }

    int trackHeight() {
        return Math.max(1, height - trackTopInset - trackBottomInset);
    }
}
