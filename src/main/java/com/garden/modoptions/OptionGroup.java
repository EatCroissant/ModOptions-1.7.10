package com.garden.modoptions;

/** Metadata for a logical section of options inside one mod. */
public final class OptionGroup {
    private final String id;
    private final String label;
    private String description;
    private int order;
    private boolean collapsedByDefault;

    public OptionGroup(String id, String label) {
        if (id == null || id.length() == 0) throw new IllegalArgumentException("id");
        this.id = id;
        this.label = label == null || label.length() == 0 ? id : label;
    }

    public String getId() { return id; }
    public String getLabel() { return label; }
    public String getDescription() { return description; }
    public int getOrder() { return order; }
    public boolean isCollapsedByDefault() { return collapsedByDefault; }

    public OptionGroup description(String description) {
        this.description = description;
        return this;
    }

    public OptionGroup order(int order) {
        this.order = order;
        return this;
    }

    public OptionGroup collapsedByDefault(boolean collapsed) {
        this.collapsedByDefault = collapsed;
        return this;
    }
}
