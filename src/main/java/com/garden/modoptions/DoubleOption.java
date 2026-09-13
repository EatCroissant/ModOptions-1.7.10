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
        double next = Math.max(min, Math.min(max, value));
        if (Double.compare(this.value, next) == 0) return;
        this.value = next;
        OptionChangeCallback.run(getKey(), onChanged);
    }

    @Override public double numericValue() { return value; }
    @Override public double minimum() { return min; }
    @Override public double maximum() { return max; }
    @Override
    public void setNumericValue(double value) {
        long steps = Math.round((value - min) / step);
        set(min + steps * step);
    }

    @Override
    public String displayValue() {
        return String.format(Locale.ROOT, "%.2f", value);
    }

    @Override
    public void cycle() {
        set(value + step > max ? min : value + step);
    }
}
