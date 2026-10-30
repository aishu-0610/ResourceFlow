package com.resourceflow.model;

/**
 * The fulfilment outcome status of an individual allocation item.
 */
public enum AllocationStatus {
    FULLY_SATISFIED("Fully Satisfied"),
    PARTIALLY_SATISFIED("Partially Satisfied"),
    UNSATISFIED("Unsatisfied");

    private final String label;

    AllocationStatus(String label) {
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
