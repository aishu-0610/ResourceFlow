package com.resourceflow.strategy;

import com.resourceflow.model.AllocationPlan;
import com.resourceflow.model.AllocationRequest;
import com.resourceflow.model.ConstraintConfig;
import com.resourceflow.model.ResourceInventory;
import com.resourceflow.model.StrategyType;

import java.util.List;

/**
 * Common interface that every resource allocation algorithm must implement.
 *
 * The engine stores strategies only as AllocationStrategy references, so it can run
 * any implementation without knowing its concrete class (runtime polymorphism).
 */
public interface AllocationStrategy {

    /**
     * Tolerance used when comparing capacities. Variables declared in an interface are
     * implicitly public, static and final, so every implementing class shares this constant.
     */
    double EPSILON = 1e-6;

    /**
     * Unique enumeration identifying this strategy.
     */
    StrategyType getType();

    /**
     * Human-readable name for UI and reports.
     */
    String getName();

    /**
     * Algorithmic description of how allocations are calculated.
     */
    String getDescription();

    /**
     * Time complexity of the algorithm. A default method, so implementations may override it but do not have to.
     */
    default String getComplexity() {
        return "Not specified";
    }

    /**
     * Generates an AllocationPlan given resources, requests, and constraint configuration.
     * Must be deterministic for identical inputs.
     *
     * @param inventory Current resource capacity inventory
     * @param requests  List of competing demand requests
     * @param config    Active constraint configurations
     * @return Generated allocation plan
     */
    AllocationPlan allocate(ResourceInventory inventory, List<AllocationRequest> requests, ConstraintConfig config);
}
