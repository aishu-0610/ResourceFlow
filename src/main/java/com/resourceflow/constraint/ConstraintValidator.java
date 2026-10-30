package com.resourceflow.constraint;

import com.resourceflow.model.AllocatedItem;
import com.resourceflow.model.AllocationPlan;
import com.resourceflow.model.ConstraintConfig;
import com.resourceflow.model.ResourceInventory;

import java.util.List;

/**
 * Interface for verifying hard constraints and evaluating soft preferences.
 */
public interface ConstraintValidator {

    /**
     * Checks all non-negotiable hard constraints on the tentative allocations.
     * Hard constraints include:
     * - Total allocated quantity per resource must not exceed active available capacity
     * - Allocation cannot be negative
     * - Allocation cannot exceed requested quantity
     * - Inactive resources cannot receive new allocations
     * - Minimum acceptable allocation thresholds must be satisfied
     * - All-or-nothing atomicity requirements must be respected
     *
     * @param inventory Current resource inventory
     * @param items     Tentative allocation items
     * @param config    Constraint configuration flags
     * @return List of violation messages (empty list if plan is valid/feasible)
     */
    List<String> validateHardConstraints(ResourceInventory inventory, List<AllocatedItem> items, ConstraintConfig config);

    /**
     * Enforces hard constraints by throwing InfeasibleConstraintException if violations exist.
     */
    void assertFeasible(ResourceInventory inventory, List<AllocatedItem> items, ConstraintConfig config);
}
