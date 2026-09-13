package com.garden.modoptions;

public final class BooleanOption extends ModOption {
    private boolean value;
    private final Runnable onChanged;

    public BooleanOption(String key, String label, boolean value,
            OptionScope scope, Runnable onChanged) {
        super(key, label, scope);
        this.value = value;
        this.onChanged = onChanged;
    }

    public boolean get() { return value; }

    public void set(boolean value) {
        this.value = value;
        if (onChanged != null) onChanged.run();
    }

    @Override
    public String displayValue() {
        return value ? "ON" : "OFF";
    }

    @Override
    public void cycle() {
        set(!value);
    }
}
