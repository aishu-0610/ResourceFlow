package com.resourceflow.model;

/**
 * Supported resource allocation strategy types.
 */
public enum StrategyType {
    PRIORITY_BASED("Priority-Based", "Allocates resources strictly by priority ranking (1-5) and arrival order."),
    FAIR_SHARE("Fair-Share", "Distributes available capacity evenly using max-min fair share allocation."),
    PROPORTIONAL("Proportional", "Distributes capacity proportionally based on requested demand and weights.");

    private final String displayName;
    private final String description;

    StrategyType(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
