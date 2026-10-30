package com.resourceflow.model;

import java.util.*;

/**
 * Encapsulates the collection of resources participating in an allocation computation.
 */
public class ResourceInventory {
    private final Map<String, Resource> resourceMap;

    public ResourceInventory() {
        this.resourceMap = new LinkedHashMap<>();
    }

    public ResourceInventory(Collection<Resource> resources) {
        this.resourceMap = new LinkedHashMap<>();
        if (resources != null) {
            for (Resource res : resources) {
                if (res != null) {
                    this.resourceMap.put(res.getResourceId(), res.copy());
                }
            }
        }
    }

    /**
     * Inventory for planning a new allocation from scratch.
     *
     * In the database, availableCapacity holds what is left over after the LAST run. A new
     * run re-plans every eligible request, including ones already served, so it must start
     * from the full total capacity; otherwise capacity would be counted as used twice.
     */
    public static ResourceInventory forReplanning(Collection<Resource> resources) {
        ResourceInventory inventory = new ResourceInventory(resources);
        for (Resource res : inventory.resourceMap.values()) {
            res.setAvailableCapacity(res.getTotalCapacity());
        }
        return inventory;
    }

    public void addResource(Resource resource) {
        if (resource != null) {
            this.resourceMap.put(resource.getResourceId(), resource.copy());
        }
    }

    public Optional<Resource> getResource(String resourceId) {
        return Optional.ofNullable(resourceMap.get(resourceId));
    }

    public boolean containsResource(String resourceId) {
        return resourceMap.containsKey(resourceId);
    }

    public Collection<Resource> getAllResources() {
        return Collections.unmodifiableCollection(resourceMap.values());
    }

    public List<Resource> getActiveResources() {
        List<Resource> active = new ArrayList<>();
        for (Resource res : resourceMap.values()) {
            if (res.isActive()) {
                active.add(res);
            }
        }
        return Collections.unmodifiableList(active);
    }

    public double getTotalAvailableCapacity() {
        double sum = 0.0;
        for (Resource r : resourceMap.values()) {
            if (r.isActive()) {
                sum += r.getAvailableCapacity();
            }
        }
        return sum;
    }

    public double getTotalCapacity() {
        double sum = 0.0;
        for (Resource r : resourceMap.values()) {
            if (r.isActive()) {
                sum += r.getTotalCapacity();
            }
        }
        return sum;
    }

    public ResourceInventory createSnapshot() {
        return new ResourceInventory(this.resourceMap.values());
    }
}
