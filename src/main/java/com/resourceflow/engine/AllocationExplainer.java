package com.resourceflow.engine;

import com.resourceflow.model.AllocationPlan;
import com.resourceflow.model.AllocationRequest;
import com.resourceflow.model.DecisionExplanation;
import com.resourceflow.model.ResourceInventory;

import java.util.List;

/**
 * Interface for generating transparent, causal explanations of allocation decisions.
 */
public interface AllocationExplainer {

    /**
     * Explains the outcome for every request in an allocation plan.
     *
     * @param plan      The executed allocation plan
     * @param inventory The resource inventory during allocation
     * @param requests  The original demand requests
     * @return List of decision explanations per request
     */
    List<DecisionExplanation> explain(AllocationPlan plan, ResourceInventory inventory, List<AllocationRequest> requests);
}
