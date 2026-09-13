package com.garden.modoptions;

public final class EnumOption<T> extends ModOption {
    private final T[] values;
    private int index;
    private final Runnable onChanged;

    public EnumOption(String key, String label, T[] values, int initial,
            OptionScope scope, Runnable onChanged) {
        super(key, label, scope);
        if (values == null || values.length == 0) throw new IllegalArgumentException("values");
        this.values = values.clone();
        this.index = Math.max(0, Math.min(this.values.length - 1, initial));
        this.onChanged = onChanged;
    }

    public T get() { return values[index]; }

    @Override
    public String displayValue() { return String.valueOf(get()); }

    @Override
    public void cycle() {
        index = (index + 1) % values.length;
        if (onChanged != null) onChanged.run();
    }
}
