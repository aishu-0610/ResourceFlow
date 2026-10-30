package com.resourceflow.model;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Domain entity representing an incoming demand request for a specific resource.
 */
public class AllocationRequest implements Serializable {

    /** Version stamp checked by Java Serialization when a saved .rflow file is read back. */
    private static final long serialVersionUID = 1L;

    private String requestId;
    private String requesterName;
    private String resourceId;
    private double requestedQuantity;
    private int priority; // 1 (lowest) to 5 (highest/critical)
    private double minAcceptableQuantity; // minimum quantity that requester will accept
    private boolean allOrNothing; // if true, must allocate requestedQuantity or 0
    private LocalDateTime submissionDate;
    private LocalDateTime deadline;
    private RequestStatus status;
    private String notes;

    public AllocationRequest() {
        this.priority = 3;
        this.minAcceptableQuantity = 0.0;
        this.allOrNothing = false;
        this.submissionDate = LocalDateTime.now();
        this.status = RequestStatus.PENDING;
        this.notes = "";
    }

    public AllocationRequest(String requestId, String requesterName, String resourceId,
                             double requestedQuantity, int priority, double minAcceptableQuantity,
                             boolean allOrNothing, LocalDateTime submissionDate,
                             LocalDateTime deadline, RequestStatus status, String notes) {
        setRequestId(requestId);
        setRequesterName(requesterName);
        setResourceId(resourceId);
        setRequestedQuantity(requestedQuantity);
        setPriority(priority);
        setMinAcceptableQuantity(minAcceptableQuantity);
        this.allOrNothing = allOrNothing;
        this.submissionDate = (submissionDate != null) ? submissionDate : LocalDateTime.now();
        this.deadline = deadline;
        this.status = (status != null) ? status : RequestStatus.PENDING;
        this.notes = (notes != null) ? notes.trim() : "";
    }

    public AllocationRequest copy() {
        return new AllocationRequest(this.requestId, this.requesterName, this.resourceId,
                this.requestedQuantity, this.priority, this.minAcceptableQuantity,
                this.allOrNothing, this.submissionDate, this.deadline, this.status, this.notes);
    }

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        if (requestId == null || requestId.trim().isEmpty()) {
            throw new IllegalArgumentException("Request ID cannot be null or empty");
        }
        this.requestId = requestId.trim();
    }

    public String getRequesterName() {
        return requesterName;
    }

    public void setRequesterName(String requesterName) {
        if (requesterName == null || requesterName.trim().isEmpty()) {
            throw new IllegalArgumentException("Requester Name cannot be null or empty");
        }
        this.requesterName = requesterName.trim();
    }

    public String getResourceId() {
        return resourceId;
    }

    public void setResourceId(String resourceId) {
        if (resourceId == null || resourceId.trim().isEmpty()) {
            throw new IllegalArgumentException("Resource ID cannot be null or empty");
        }
        this.resourceId = resourceId.trim();
    }

    public double getRequestedQuantity() {
        return requestedQuantity;
    }

    public void setRequestedQuantity(double requestedQuantity) {
        if (requestedQuantity <= 0) {
            throw new IllegalArgumentException("Requested quantity must be strictly greater than 0");
        }
        this.requestedQuantity = requestedQuantity;
    }

    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        if (priority < 1 || priority > 5) {
            throw new IllegalArgumentException("Priority must be between 1 (lowest) and 5 (highest)");
        }
        this.priority = priority;
    }

    public double getMinAcceptableQuantity() {
        return minAcceptableQuantity;
    }

    public void setMinAcceptableQuantity(double minAcceptableQuantity) {
        if (minAcceptableQuantity < 0) {
            throw new IllegalArgumentException("Minimum acceptable quantity cannot be negative");
        }
        if (this.requestedQuantity > 0 && minAcceptableQuantity > this.requestedQuantity) {
            throw new IllegalArgumentException("Minimum acceptable quantity cannot exceed requested quantity");
        }
        this.minAcceptableQuantity = minAcceptableQuantity;
    }

    public boolean isAllOrNothing() {
        return allOrNothing;
    }

    public void setAllOrNothing(boolean allOrNothing) {
        this.allOrNothing = allOrNothing;
    }

    public LocalDateTime getSubmissionDate() {
        return submissionDate;
    }

    public void setSubmissionDate(LocalDateTime submissionDate) {
        this.submissionDate = submissionDate;
    }

    public LocalDateTime getDeadline() {
        return deadline;
    }

    public void setDeadline(LocalDateTime deadline) {
        this.deadline = deadline;
    }

    public RequestStatus getStatus() {
        return status;
    }

    public void setStatus(RequestStatus status) {
        this.status = status;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes == null ? "" : notes.trim();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AllocationRequest that = (AllocationRequest) o;
        return Objects.equals(requestId, that.requestId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(requestId);
    }

    @Override
    public String toString() {
        return "Request[" + requestId + "] " + requesterName + " -> " + requestedQuantity + " of " + resourceId + " (P" + priority + ")";
    }
}
