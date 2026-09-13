package com.garden.modoptions;

public abstract class ModOption {
    private final String key;
    private final String label;
    private final OptionScope scope;
    private String groupId = "general";
    private String description;
    private int order;

    protected ModOption(String key, String label, OptionScope scope) {
        this.key = key;
        this.label = label;
        this.scope = scope;
    }

    public String getKey() { return key; }
    public String getLabel() { return label; }
    public OptionScope getScope() { return scope; }
    public String getGroupId() { return groupId; }
    public String getDescription() { return description; }
    public int getOrder() { return order; }

    /** Assigns this option to a logical section. Kept fluent for registration code. */
    public ModOption group(String groupId) {
        this.groupId = groupId == null || groupId.length() == 0 ? "general" : groupId;
        return this;
    }

    /** Localization key or literal text shown as contextual help on hover. */
    public ModOption description(String description) {
        this.description = description;
        return this;
    }

    /** Lower values are rendered first inside the group. */
    public ModOption order(int order) {
        this.order = order;
        return this;
    }

    public abstract String displayValue();
    public abstract void cycle();
}
