package com.resourceflow.constraint;

import com.resourceflow.exception.InfeasibleConstraintException;
import com.resourceflow.model.AllocatedItem;
import com.resourceflow.model.ConstraintConfig;
import com.resourceflow.model.Resource;
import com.resourceflow.model.ResourceInventory;

import java.util.*;

/**
 * Standard implementation of ConstraintValidator enforcing all non-negotiable hard constraints.
 */
public class StandardConstraintValidator implements ConstraintValidator {

    private static final double EPSILON = 1e-6;

    @Override
    public List<String> validateHardConstraints(ResourceInventory inventory, List<AllocatedItem> items, ConstraintConfig config) {
        List<String> violations = new ArrayList<>();
        if (items == null || items.isEmpty()) {
            return violations;
        }

        ConstraintConfig activeConfig = (config != null) ? config : ConstraintConfig.defaultConfig();
        Map<String, Double> allocatedByResource = new HashMap<>();

        for (AllocatedItem item : items) {
            String resId = item.getResourceId();
            double allocated = item.getAllocatedQuantity();
            double requested = item.getRequestedQuantity();

            // 1. Non-negativity check
            if (allocated < -EPSILON) {
                violations.add(String.format("Request %s: Allocated quantity cannot be negative (got %.2f)",
                        item.getRequestId(), allocated));
            }

            // 2. Allocation cannot exceed eligible request quantity
            if (allocated > requested + EPSILON) {
                violations.add(String.format("Request %s: Allocated quantity (%.2f) exceeds requested quantity (%.2f)",
                        item.getRequestId(), allocated, requested));
            }

            // 3. Resource existence and active status
            Optional<Resource> resOpt = inventory.getResource(resId);
            if (resOpt.isEmpty()) {
                violations.add(String.format("Request %s references non-existent resource '%s'",
                        item.getRequestId(), resId));
            } else {
                Resource resource = resOpt.get();
                if (activeConfig.isRespectInactiveStatus() && !resource.isActive() && allocated > EPSILON) {
                    violations.add(String.format("Request %s: Cannot allocate %.2f units from inactive resource '%s'",
                            item.getRequestId(), allocated, resource.getName()));
                }
            }

            // Track cumulative allocation per resource
            allocatedByResource.merge(resId, Math.max(0.0, allocated), Double::sum);
        }

        // 4. Capacity bounds check per resource
        for (Map.Entry<String, Double> entry : allocatedByResource.entrySet()) {
            String resId = entry.getKey();
            double totalAllocated = entry.getValue();

            Optional<Resource> resOpt = inventory.getResource(resId);
            if (resOpt.isPresent()) {
                Resource resource = resOpt.get();
                double availableCapacity = resource.getAvailableCapacity();
                if (totalAllocated > availableCapacity + EPSILON) {
                    violations.add(String.format("Resource '%s' (%s): Total allocated quantity (%.2f) exceeds available capacity (%.2f)",
                            resource.getName(), resId, totalAllocated, availableCapacity));
                }
            }
        }

        return violations;
    }

    @Override
    public void assertFeasible(ResourceInventory inventory, List<AllocatedItem> items, ConstraintConfig config) {
        List<String> violations = validateHardConstraints(inventory, items, config);
        if (!violations.isEmpty()) {
            throw new InfeasibleConstraintException("Allocation plan violates hard constraints: " +
                    String.join("; ", violations));
        }
    }
}
