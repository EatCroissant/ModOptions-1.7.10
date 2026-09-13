package com.garden.modoptions;

import java.util.Locale;

public final class DoubleOption extends ModOption implements NumericOption {
    private double value;
    private final double min;
    private final double max;
    private final double step;
    private final Runnable onChanged;

    public DoubleOption(String key, String label, double value, double min, double max,
            double step, OptionScope scope, Runnable onChanged) {
        super(key, label, scope);
        this.min = min;
        this.max = max;
        this.step = Math.max(0.0001, step);
        this.onChanged = onChanged;
        this.value = Math.max(min, Math.min(max, value));
    }

    public double get() { return value; }

    public void set(double value) {
        this.value = Math.max(min, Math.min(max, value));
        if (onChanged != null) onChanged.run();
    }

    @Override public double numericValue() { return value; }
    @Override public double minimum() { return min; }
    @Override public double maximum() { return max; }
    @Override public void setNumericValue(double value) { set(value); }

    @Override
    public String displayValue() {
        return String.format(Locale.ROOT, "%.2f", value);
    }

    @Override
    public void cycle() {
        set(value + step > max ? min : value + step);
    }
}
