package com.resourceflow.service;

import com.resourceflow.exception.ResourceConflictException;
import com.resourceflow.exception.ResourceNotFoundException;
import com.resourceflow.exception.ResourceValidationException;
import com.resourceflow.model.AllocationRequest;
import com.resourceflow.model.RequestStatus;
import com.resourceflow.repository.RequestRepository;
import com.resourceflow.repository.ResourceRepository;

import java.util.List;

/**
 * Service managing allocation requests with validation and status workflows.
 */
public class RequestService {

    private final RequestRepository requestRepository;
    private final ResourceRepository resourceRepository;

    public RequestService(RequestRepository requestRepository, ResourceRepository resourceRepository) {
        this.requestRepository = requestRepository;
        this.resourceRepository = resourceRepository;
    }

    public AllocationRequest createRequest(AllocationRequest request) {
        validateRequest(request);
        if (requestRepository.existsById(request.getRequestId())) {
            throw new ResourceConflictException("A request with ID '" + request.getRequestId() + "' already exists.");
        }
        if (!resourceRepository.existsById(request.getResourceId())) {
            throw new ResourceNotFoundException("Referenced resource '" + request.getResourceId() + "' does not exist.");
        }
        requestRepository.save(request);
        return request;
    }

    public AllocationRequest updateRequest(AllocationRequest request) {
        validateRequest(request);
        if (!requestRepository.existsById(request.getRequestId())) {
            throw new ResourceNotFoundException("Cannot update non-existent request: " + request.getRequestId());
        }
        if (!resourceRepository.existsById(request.getResourceId())) {
            throw new ResourceNotFoundException("Referenced resource '" + request.getResourceId() + "' does not exist.");
        }
        requestRepository.update(request);
        return request;
    }

    public void cancelRequest(String requestId) {
        AllocationRequest req = getRequestById(requestId);
        req.setStatus(RequestStatus.CANCELLED);
        requestRepository.update(req);
    }

    public void deleteRequest(String requestId) {
        if (!requestRepository.existsById(requestId)) {
            throw new ResourceNotFoundException("Request not found: " + requestId);
        }
        requestRepository.deleteById(requestId);
    }

    public AllocationRequest getRequestById(String requestId) {
        return requestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Request not found: " + requestId));
    }

    public List<AllocationRequest> getAllRequests() {
        return requestRepository.findAll();
    }

    public List<AllocationRequest> getEligibleRequests() {
        return requestRepository.findEligibleRequests();
    }

    public List<AllocationRequest> getRequestsByStatus(RequestStatus status) {
        return requestRepository.findByStatus(status);
    }

    private void validateRequest(AllocationRequest request) {
        if (request == null) {
            throw new ResourceValidationException("Request cannot be null");
        }
        if (request.getRequestId() == null || request.getRequestId().trim().isEmpty()) {
            throw new ResourceValidationException("Request ID is required");
        }
        if (request.getRequesterName() == null || request.getRequesterName().trim().isEmpty()) {
            throw new ResourceValidationException("Requester name is required");
        }
        if (request.getResourceId() == null || request.getResourceId().trim().isEmpty()) {
            throw new ResourceValidationException("Target resource ID is required");
        }
        if (request.getRequestedQuantity() <= 0) {
            throw new ResourceValidationException("Requested quantity must be strictly greater than 0");
        }
        if (request.getPriority() < 1 || request.getPriority() > 5) {
            throw new ResourceValidationException("Priority must be an integer between 1 and 5");
        }
        if (request.getMinAcceptableQuantity() < 0) {
            throw new ResourceValidationException("Minimum acceptable quantity cannot be negative");
        }
        if (request.getMinAcceptableQuantity() > request.getRequestedQuantity()) {
            throw new ResourceValidationException("Minimum acceptable quantity (" + request.getMinAcceptableQuantity() +
                    ") cannot exceed requested quantity (" + request.getRequestedQuantity() + ")");
        }
    }
}
