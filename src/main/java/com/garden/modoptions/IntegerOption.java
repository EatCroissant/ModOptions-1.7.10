package com.garden.modoptions;

public final class IntegerOption extends ModOption implements NumericOption {
    private int value;
    private final int min;
    private final int max;
    private final int step;
    private final Runnable onChanged;

    public IntegerOption(String key, String label, int value, int min, int max,
            int step, OptionScope scope, Runnable onChanged) {
        super(key, label, scope);
        this.value = Math.max(min, Math.min(max, value));
        this.min = min;
        this.max = max;
        this.step = Math.max(1, step);
        this.onChanged = onChanged;
    }

    public int get() { return value; }

    @Override public double numericValue() { return value; }
    @Override public double minimum() { return min; }
    @Override public double maximum() { return max; }
    @Override
    public void setNumericValue(double value) {
        long steps = Math.round((value - min) / step);
        long snapped = (long) min + steps * (long) step;
        set((int) Math.max(min, Math.min((long) max, snapped)));
    }

    public void set(int value) {
        int next = Math.max(min, Math.min(max, value));
        if (this.value == next) return;
        this.value = next;
        OptionChangeCallback.run(getKey(), onChanged);
    }

    @Override
    public String displayValue() {
        return Integer.toString(value);
    }

    @Override
    public void cycle() {
        set(value + step > max ? min : value + step);
    }
}
