package com.resourceflow.model;

import java.util.Objects;
import java.util.UUID;

/**
 * Result record for a single request within an allocation plan or run.
 */
public class AllocatedItem {
    private String allocationId;
    private String runId;
    private String requestId;
    private String requesterName;
    private String resourceId;
    private double requestedQuantity;
    private double allocatedQuantity;
    private double fulfilmentRatio; // allocated / requested [0.0 to 1.0]
    private AllocationStatus status;
    private String explanation;

    public AllocatedItem() {
        this.allocationId = UUID.randomUUID().toString();
        this.status = AllocationStatus.UNSATISFIED;
        this.explanation = "";
    }

    public AllocatedItem(String runId, String requestId, String requesterName, String resourceId,
                         double requestedQuantity, double allocatedQuantity, String explanation) {
        this.allocationId = UUID.randomUUID().toString();
        this.runId = runId;
        this.requestId = requestId;
        this.requesterName = requesterName;
        this.resourceId = resourceId;
        this.requestedQuantity = requestedQuantity;
        setAllocatedQuantity(allocatedQuantity);
        this.explanation = explanation != null ? explanation : "";
    }

    public String getAllocationId() {
        return allocationId;
    }

    public void setAllocationId(String allocationId) {
        this.allocationId = allocationId;
    }

    public String getRunId() {
        return runId;
    }

    public void setRunId(String runId) {
        this.runId = runId;
    }

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public String getRequesterName() {
        return requesterName;
    }

    public void setRequesterName(String requesterName) {
        this.requesterName = requesterName;
    }

    public String getResourceId() {
        return resourceId;
    }

    public void setResourceId(String resourceId) {
        this.resourceId = resourceId;
    }

    public double getRequestedQuantity() {
        return requestedQuantity;
    }

    public void setRequestedQuantity(double requestedQuantity) {
        this.requestedQuantity = requestedQuantity;
        recalculateStatus();
    }

    public double getAllocatedQuantity() {
        return allocatedQuantity;
    }

    public void setAllocatedQuantity(double allocatedQuantity) {
        if (allocatedQuantity < 0) {
            throw new IllegalArgumentException("Allocated quantity cannot be negative");
        }
        this.allocatedQuantity = allocatedQuantity;
        recalculateStatus();
    }

    private void recalculateStatus() {
        if (this.requestedQuantity <= 0) {
            this.fulfilmentRatio = 0.0;
            this.status = AllocationStatus.UNSATISFIED;
            return;
        }

        // Clamp to precision
        this.fulfilmentRatio = Math.min(1.0, Math.max(0.0, this.allocatedQuantity / this.requestedQuantity));
        // Use a small epsilon for floating point equality
        if (Math.abs(this.allocatedQuantity - this.requestedQuantity) < 1e-6 || this.allocatedQuantity >= this.requestedQuantity) {
            this.status = AllocationStatus.FULLY_SATISFIED;
        } else if (this.allocatedQuantity > 1e-6) {
            this.status = AllocationStatus.PARTIALLY_SATISFIED;
        } else {
            this.status = AllocationStatus.UNSATISFIED;
        }
    }

    public double getFulfilmentRatio() {
        return fulfilmentRatio;
    }

    public void setFulfilmentRatio(double fulfilmentRatio) {
        this.fulfilmentRatio = fulfilmentRatio;
    }

    public AllocationStatus getStatus() {
        return status;
    }

    public void setStatus(AllocationStatus status) {
        this.status = status;
    }

    public String getExplanation() {
        return explanation;
    }

    public void setExplanation(String explanation) {
        this.explanation = explanation != null ? explanation : "";
    }

    public double getUnmetQuantity() {
        return Math.max(0.0, requestedQuantity - allocatedQuantity);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AllocatedItem that = (AllocatedItem) o;
        return Objects.equals(allocationId, that.allocationId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(allocationId);
    }

    @Override
    public String toString() {
        return "AllocatedItem{" +
                "requestId='" + requestId + '\'' +
                ", resourceId='" + resourceId + '\'' +
                ", allocated=" + allocatedQuantity + "/" + requestedQuantity +
                ", status=" + status +
                '}';
    }
}
