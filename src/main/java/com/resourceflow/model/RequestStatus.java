package com.resourceflow.model;

/**
 * Status of an allocation request.
 */
public enum RequestStatus {
    PENDING("Pending"),
    ALLOCATED("Allocated"),
    PARTIALLY_ALLOCATED("Partially Allocated"),
    REJECTED("Rejected"),
    CANCELLED("Cancelled");

    private final String label;

    RequestStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    @Override
    public String toString() {
        return label;
    }
}
