package com.resourceflow.service;

import com.resourceflow.exception.ResourceConflictException;
import com.resourceflow.exception.ResourceNotFoundException;
import com.resourceflow.exception.ResourceValidationException;
import com.resourceflow.model.Resource;
import com.resourceflow.repository.ResourceRepository;

import java.util.List;

/**
 * Service managing resources with validation and historical integrity enforcement.
 */
public class ResourceService {

    private final ResourceRepository resourceRepository;

    public ResourceService(ResourceRepository resourceRepository) {
        this.resourceRepository = resourceRepository;
    }

    public Resource createResource(Resource resource) {
        validateResource(resource);
        if (resourceRepository.existsById(resource.getResourceId())) {
            throw new ResourceConflictException("A resource with ID '" + resource.getResourceId() + "' already exists.");
        }
        resourceRepository.save(resource);
        return resource;
    }

    public Resource updateResource(Resource resource) {
        validateResource(resource);
        if (!resourceRepository.existsById(resource.getResourceId())) {
            throw new ResourceNotFoundException("Cannot update non-existent resource: " + resource.getResourceId());
        }
        resourceRepository.update(resource);
        return resource;
    }

    public void deleteResource(String resourceId) {
        if (!resourceRepository.existsById(resourceId)) {
            throw new ResourceNotFoundException("Resource '" + resourceId + "' does not exist.");
        }
        // Integrity check: prevent deletion if referenced by historical allocations
        if (resourceRepository.hasAllocations(resourceId)) {
            throw new ResourceConflictException("Cannot delete resource '" + resourceId +
                    "' because historical allocation records depend on it. Mark it inactive instead.");
        }
        resourceRepository.deleteById(resourceId);
    }

    public Resource getResourceById(String resourceId) {
        return resourceRepository.findById(resourceId)
                .orElseThrow(() -> new ResourceNotFoundException("Resource not found: " + resourceId));
    }

    public List<Resource> getAllResources() {
        return resourceRepository.findAll();
    }

    public List<Resource> getActiveResources() {
        return resourceRepository.findActive();
    }

    public void toggleActiveStatus(String resourceId) {
        Resource res = getResourceById(resourceId);
        res.setActive(!res.isActive());
        resourceRepository.update(res);
    }

    private void validateResource(Resource resource) {
        if (resource == null) {
            throw new ResourceValidationException("Resource cannot be null");
        }
        if (resource.getResourceId() == null || resource.getResourceId().trim().isEmpty()) {
            throw new ResourceValidationException("Resource ID is required");
        }
        if (resource.getName() == null || resource.getName().trim().isEmpty()) {
            throw new ResourceValidationException("Resource Name is required");
        }
        if (resource.getTotalCapacity() < 0) {
            throw new ResourceValidationException("Total capacity cannot be negative");
        }
        if (resource.getAvailableCapacity() < 0) {
            throw new ResourceValidationException("Available capacity cannot be negative");
        }
        if (resource.getAvailableCapacity() > resource.getTotalCapacity()) {
            throw new ResourceValidationException("Available capacity (" + resource.getAvailableCapacity() +
                    ") cannot exceed total capacity (" + resource.getTotalCapacity() + ")");
        }
    }
}
