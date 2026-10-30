package com.resourceflow.evaluation;

import com.resourceflow.model.AllocatedItem;
import com.resourceflow.model.AllocationPlan;
import com.resourceflow.model.AllocationRequest;
import com.resourceflow.model.EvaluationMetrics;
import com.resourceflow.model.ResourceInventory;

import java.util.List;

/**
 * Interface for calculating deterministic mathematical metrics for allocation plans.
 */
public interface OptimisationEvaluator {

    /**
     * Calculates mathematical evaluation metrics for the given set of allocations.
     *
     * @param inventory        Active resource inventory
     * @param originalRequests Original input requests
     * @param allocations      Allocated result items
     * @return Evaluated metrics (utilisation, Jain's index, fulfilment, counts)
     */
    EvaluationMetrics evaluate(ResourceInventory inventory, List<AllocationRequest> originalRequests, List<AllocatedItem> allocations);

    /**
     * Compares multiple allocation plans generated under the same inputs.
     *
     * @param plans List of candidate plans
     * @return StrategyComparison with side-by-side metrics and highlights
     */
    StrategyComparison compare(List<AllocationPlan> plans);
}
