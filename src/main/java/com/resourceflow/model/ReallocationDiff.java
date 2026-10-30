package com.resourceflow.model;

import java.util.*;

/**
 * Encapsulates differences between two allocation plans (e.g., before and after adaptive reallocation).
 */
public class ReallocationDiff {
    private String previousRunId;
    private String newRunId;
    private List<DiffItem> improvedRequests;
    private List<DiffItem> degradedRequests;
    private List<DiffItem> droppedRequests;
    private List<DiffItem> unchangedRequests;
    private double capacityReassigned;
    private String summaryExplanation;

    public static class DiffItem {
        private final String requestId;
        private final String requesterName;
        private final String resourceId;
        private final double previousAllocation;
        private final double newAllocation;
        private final double requestedQuantity;
        private final String reason;

        public DiffItem(String requestId, String requesterName, String resourceId,
                        double previousAllocation, double newAllocation, double requestedQuantity, String reason) {
            this.requestId = requestId;
            this.requesterName = requesterName;
            this.resourceId = resourceId;
            this.previousAllocation = previousAllocation;
            this.newAllocation = newAllocation;
            this.requestedQuantity = requestedQuantity;
            this.reason = reason;
        }

        public String getRequestId() { return requestId; }
        public String getRequesterName() { return requesterName; }
        public String getResourceId() { return resourceId; }
        public double getPreviousAllocation() { return previousAllocation; }
        public double getNewAllocation() { return newAllocation; }
        public double getRequestedQuantity() { return requestedQuantity; }
        public double getDelta() { return newAllocation - previousAllocation; }
        public String getReason() { return reason; }
    }

    public ReallocationDiff() {
        this.improvedRequests = new ArrayList<>();
        this.degradedRequests = new ArrayList<>();
        this.droppedRequests = new ArrayList<>();
        this.unchangedRequests = new ArrayList<>();
        this.summaryExplanation = "";
    }

    public String getPreviousRunId() {
        return previousRunId;
    }

    public void setPreviousRunId(String previousRunId) {
        this.previousRunId = previousRunId;
    }

    public String getNewRunId() {
        return newRunId;
    }

    public void setNewRunId(String newRunId) {
        this.newRunId = newRunId;
    }

    public List<DiffItem> getImprovedRequests() {
        return improvedRequests;
    }

    public void setImprovedRequests(List<DiffItem> improvedRequests) {
        this.improvedRequests = (improvedRequests != null) ? improvedRequests : new ArrayList<>();
    }

    public List<DiffItem> getDegradedRequests() {
        return degradedRequests;
    }

    public void setDegradedRequests(List<DiffItem> degradedRequests) {
        this.degradedRequests = (degradedRequests != null) ? degradedRequests : new ArrayList<>();
    }

    public List<DiffItem> getDroppedRequests() {
        return droppedRequests;
    }

    public void setDroppedRequests(List<DiffItem> droppedRequests) {
        this.droppedRequests = (droppedRequests != null) ? droppedRequests : new ArrayList<>();
    }

    public List<DiffItem> getUnchangedRequests() {
        return unchangedRequests;
    }

    public void setUnchangedRequests(List<DiffItem> unchangedRequests) {
        this.unchangedRequests = (unchangedRequests != null) ? unchangedRequests : new ArrayList<>();
    }

    public double getCapacityReassigned() {
        return capacityReassigned;
    }

    public void setCapacityReassigned(double capacityReassigned) {
        this.capacityReassigned = capacityReassigned;
    }

    public String getSummaryExplanation() {
        return summaryExplanation;
    }

    public void setSummaryExplanation(String summaryExplanation) {
        this.summaryExplanation = summaryExplanation != null ? summaryExplanation : "";
    }
}
