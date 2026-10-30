package com.resourceflow.model;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Domain entity representing a manageable, allocatable resource.
 */
public class Resource implements Serializable {

    /** Version stamp checked by Java Serialization when a saved .rflow file is read back. */
    private static final long serialVersionUID = 1L;

    private String resourceId;
    private String name;
    private String category;
    private double totalCapacity;
    private double availableCapacity;
    private String unit;
    private String description;
    private boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Resource() {
        this.active = true;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public Resource(String resourceId, String name, String category, double totalCapacity,
                    double availableCapacity, String unit, String description, boolean active) {
        setResourceId(resourceId);
        setName(name);
        setCategory(category);
        setTotalCapacity(totalCapacity);
        setAvailableCapacity(availableCapacity);
        setUnit(unit);
        setDescription(description);
        this.active = active;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public Resource copy() {
        Resource clone = new Resource(this.resourceId, this.name, this.category,
                this.totalCapacity, this.availableCapacity, this.unit, this.description, this.active);
        clone.setCreatedAt(this.createdAt);
        clone.setUpdatedAt(this.updatedAt);
        return clone;
    }

    public String getResourceId() {
        return resourceId;
    }

    public void setResourceId(String resourceId) {
        if (resourceId == null || resourceId.trim().isEmpty()) {
            throw new IllegalArgumentException("Resource ID cannot be null or blank");
        }
        this.resourceId = resourceId.trim();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Resource Name cannot be null or blank");
        }
        this.name = name.trim();
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = (category == null || category.trim().isEmpty()) ? "General" : category.trim();
    }

    public double getTotalCapacity() {
        return totalCapacity;
    }

    public void setTotalCapacity(double totalCapacity) {
        if (totalCapacity < 0) {
            throw new IllegalArgumentException("Total capacity cannot be negative");
        }
        this.totalCapacity = totalCapacity;
    }

    public double getAvailableCapacity() {
        return availableCapacity;
    }

    public void setAvailableCapacity(double availableCapacity) {
        if (availableCapacity < 0) {
            throw new IllegalArgumentException("Available capacity cannot be negative");
        }
        if (availableCapacity > this.totalCapacity) {
            throw new IllegalArgumentException("Available capacity (" + availableCapacity + 
                    ") cannot exceed total capacity (" + this.totalCapacity + ")");
        }
        this.availableCapacity = availableCapacity;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = (unit == null || unit.trim().isEmpty()) ? "Units" : unit.trim();
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description == null ? "" : description.trim();
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Resource resource = (Resource) o;
        return Objects.equals(resourceId, resource.resourceId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(resourceId);
    }

    @Override
    public String toString() {
        return name + " (" + resourceId + ") - " + availableCapacity + "/" + totalCapacity + " " + unit;
    }
}
