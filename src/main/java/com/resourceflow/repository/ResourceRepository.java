package com.resourceflow.repository;

import com.resourceflow.model.Resource;

import java.util.List;
import java.util.Optional;

/**
 * Data access operations for Resources.
 */
public interface ResourceRepository {
    void save(Resource resource);
    void update(Resource resource);
    void deleteById(String resourceId);
    Optional<Resource> findById(String resourceId);
    List<Resource> findAll();
    List<Resource> findActive();
    boolean hasAllocations(String resourceId);
    boolean existsById(String resourceId);
}
