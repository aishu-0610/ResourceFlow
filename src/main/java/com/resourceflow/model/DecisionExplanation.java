package com.resourceflow.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Detailed transparent rationale for why a specific allocation decision was made for a request.
 */
public class DecisionExplanation {
    private String requestId;
    private String requesterName;
    private String resourceId;
    private double requestedQuantity;
    private double allocatedQuantity;
    private double unmetDemand;
    private int priority;
    private double resourceTotalCapacity;
    private double resourceAvailableCapacity;
    private StrategyType strategyUsed;
    private AllocationStatus status;
    private List<String> contributingConstraints;
    private String primaryReason;

    public DecisionExplanation() {
        this.contributingConstraints = new ArrayList<>();
    }

    public DecisionExplanation(String requestId, String requesterName, String resourceId,
                               double requestedQuantity, double allocatedQuantity, int priority,
                               double resourceTotalCapacity, double resourceAvailableCapacity,
                               StrategyType strategyUsed, AllocationStatus status,
                               List<String> contributingConstraints, String primaryReason) {
        this.requestId = requestId;
        this.requesterName = requesterName;
        this.resourceId = resourceId;
        this.requestedQuantity = requestedQuantity;
        this.allocatedQuantity = allocatedQuantity;
        this.unmetDemand = Math.max(0.0, requestedQuantity - allocatedQuantity);
        this.priority = priority;
        this.resourceTotalCapacity = resourceTotalCapacity;
        this.resourceAvailableCapacity = resourceAvailableCapacity;
        this.strategyUsed = strategyUsed;
        this.status = status;
        this.contributingConstraints = (contributingConstraints != null) ? new ArrayList<>(contributingConstraints) : new ArrayList<>();
        this.primaryReason = primaryReason != null ? primaryReason : "";
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
        this.unmetDemand = Math.max(0.0, this.requestedQuantity - this.allocatedQuantity);
    }

    public double getAllocatedQuantity() {
        return allocatedQuantity;
    }

    public void setAllocatedQuantity(double allocatedQuantity) {
        this.allocatedQuantity = allocatedQuantity;
        this.unmetDemand = Math.max(0.0, this.requestedQuantity - this.allocatedQuantity);
    }

    public double getUnmetDemand() {
        return unmetDemand;
    }

    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    public double getResourceTotalCapacity() {
        return resourceTotalCapacity;
    }

    public void setResourceTotalCapacity(double resourceTotalCapacity) {
        this.resourceTotalCapacity = resourceTotalCapacity;
    }

    public double getResourceAvailableCapacity() {
        return resourceAvailableCapacity;
    }

    public void setResourceAvailableCapacity(double resourceAvailableCapacity) {
        this.resourceAvailableCapacity = resourceAvailableCapacity;
    }

    public StrategyType getStrategyUsed() {
        return strategyUsed;
    }

    public void setStrategyUsed(StrategyType strategyUsed) {
        this.strategyUsed = strategyUsed;
    }

    public AllocationStatus getStatus() {
        return status;
    }

    public void setStatus(AllocationStatus status) {
        this.status = status;
    }

    public List<String> getContributingConstraints() {
        return Collections.unmodifiableList(contributingConstraints);
    }

    public void setContributingConstraints(List<String> contributingConstraints) {
        this.contributingConstraints = (contributingConstraints != null) ? new ArrayList<>(contributingConstraints) : new ArrayList<>();
    }

    public void addContributingConstraint(String constraint) {
        if (constraint != null && !constraint.trim().isEmpty()) {
            this.contributingConstraints.add(constraint.trim());
        }
    }

    public String getPrimaryReason() {
        return primaryReason;
    }

    public void setPrimaryReason(String primaryReason) {
        this.primaryReason = primaryReason != null ? primaryReason : "";
    }
}
