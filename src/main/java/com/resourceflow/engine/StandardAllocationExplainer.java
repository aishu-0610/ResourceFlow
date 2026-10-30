package com.resourceflow.engine;

import com.resourceflow.model.AllocatedItem;
import com.resourceflow.model.AllocationPlan;
import com.resourceflow.model.AllocationRequest;
import com.resourceflow.model.AllocationStatus;
import com.resourceflow.model.DecisionExplanation;
import com.resourceflow.model.Resource;
import com.resourceflow.model.ResourceInventory;

import java.util.*;

/**
 * Standard implementation of AllocationExplainer.
 * Generates transparent, human-readable causal reasoning for every request decision.
 */
public class StandardAllocationExplainer implements AllocationExplainer {

    private static final double EPSILON = 1e-6;

    @Override
    public List<DecisionExplanation> explain(AllocationPlan plan, ResourceInventory inventory, List<AllocationRequest> requests) {
        List<DecisionExplanation> explanations = new ArrayList<>();
        if (requests == null || plan == null) {
            return explanations;
        }

        Map<String, AllocationRequest> requestMap = new HashMap<>();
        for (AllocationRequest req : requests) {
            requestMap.put(req.getRequestId(), req);
        }

        for (AllocatedItem item : plan.getItems()) {
            String reqId = item.getRequestId();
            AllocationRequest req = requestMap.get(reqId);
            Optional<Resource> resOpt = inventory.getResource(item.getResourceId());

            double totalCap = resOpt.map(Resource::getTotalCapacity).orElse(0.0);
            double availCap = resOpt.map(Resource::getAvailableCapacity).orElse(0.0);
            int priority = (req != null) ? req.getPriority() : 3;

            List<String> constraints = new ArrayList<>();
            if (req != null) {
                if (req.isAllOrNothing()) {
                    constraints.add("All-or-Nothing (Atomic)");
                }
                if (req.getMinAcceptableQuantity() > 0) {
                    constraints.add(String.format(Locale.US, "Min Acceptable Threshold (%.2f)", req.getMinAcceptableQuantity()));
                }
                if (req.getDeadline() != null) {
                    constraints.add("Deadline: " + req.getDeadline().toString());
                }
            }

            if (resOpt.isPresent() && !resOpt.get().isActive()) {
                constraints.add("Resource Inactive");
            }

            String reason = item.getExplanation();
            if (reason == null || reason.trim().isEmpty()) {
                if (item.getStatus() == AllocationStatus.FULLY_SATISFIED) {
                    reason = "Fully satisfied: Requested quantity was within available resource capacity.";
                } else if (item.getStatus() == AllocationStatus.PARTIALLY_SATISFIED) {
                    reason = "Partially satisfied: Available capacity was shared or depleted.";
                } else {
                    reason = "Unsatisfied: Insufficient capacity or hard constraints prevented allocation.";
                }
            }

            DecisionExplanation exp = new DecisionExplanation(
                    reqId,
                    item.getRequesterName(),
                    item.getResourceId(),
                    item.getRequestedQuantity(),
                    item.getAllocatedQuantity(),
                    priority,
                    totalCap,
                    availCap,
                    plan.getStrategyType(),
                    item.getStatus(),
                    constraints,
                    reason
            );

            explanations.add(exp);
        }

        return explanations;
    }
}
