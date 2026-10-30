package com.resourceflow.repository;

import com.resourceflow.model.AllocationRequest;
import com.resourceflow.model.RequestStatus;

import java.util.List;
import java.util.Optional;

/**
 * Data access operations for Allocation Requests.
 */
public interface RequestRepository {
    void save(AllocationRequest request);
    void update(AllocationRequest request);
    void deleteById(String requestId);
    Optional<AllocationRequest> findById(String requestId);
    List<AllocationRequest> findAll();
    List<AllocationRequest> findByStatus(RequestStatus status);
    List<AllocationRequest> findEligibleRequests();
    boolean existsById(String requestId);
}
