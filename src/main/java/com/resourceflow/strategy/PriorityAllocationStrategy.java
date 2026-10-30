package com.resourceflow.strategy;

import com.resourceflow.model.AllocatedItem;
import com.resourceflow.model.AllocationRequest;
import com.resourceflow.model.ConstraintConfig;
import com.resourceflow.model.Resource;
import com.resourceflow.model.ResourceInventory;
import com.resourceflow.model.StrategyType;

import java.util.*;

/**
 * Priority-Based Allocation Strategy.
 * Allocates capacity strictly in order of priority level (5 down to 1),
 * with submission timestamp and demand as deterministic tie-breakers.
 */
@StrategyInfo(
        name = "Priority-Based Allocation",
        description = "Allocates resources strictly by priority ranking (5 to 1) with arrival time tie-breaking. "
                + "Respects capacity limits, minimum acceptable thresholds, and all-or-nothing constraints.",
        complexity = "O(n log n)")
public class PriorityAllocationStrategy extends AbstractAllocationStrategy {

    /** Priority (descending) -> SubmissionDate (ascending) -> Demand (ascending) -> ID. */
    private static final Comparator<AllocationRequest> SERVICE_ORDER = Comparator
            .comparingInt(AllocationRequest::getPriority).reversed()
            .thenComparing(AllocationRequest::getSubmissionDate, Comparator.nullsLast(Comparator.naturalOrder()))
            .thenComparingDouble(AllocationRequest::getRequestedQuantity)
            .thenComparing(AllocationRequest::getRequestId);

    public PriorityAllocationStrategy() {
        super(StrategyType.PRIORITY_BASED);
    }

    @Override
    protected List<AllocatedItem> computeItems(ResourceInventory inventory, List<AllocationRequest> requests,
                                               ConstraintConfig config) {
        List<AllocatedItem> items = new ArrayList<>();

        // Remaining capacity per resource (Map<String, Double>: double values are autoboxed to Double)
        Map<String, Double> remainingCapacity = new HashMap<>();
        for (Resource res : inventory.getAllResources()) {
            remainingCapacity.put(res.getResourceId(), res.isActive() ? res.getAvailableCapacity() : 0.0);
        }

        // A PriorityQueue always hands back the most urgent request first
        Queue<AllocationRequest> queue = new PriorityQueue<>(SERVICE_ORDER);
        queue.addAll(requests);

        while (!queue.isEmpty()) {
            AllocationRequest req = queue.poll();
            items.add(allocateOne(req, inventory, remainingCapacity, config));
        }
        return items;
    }

    private AllocatedItem allocateOne(AllocationRequest req, ResourceInventory inventory,
                                      Map<String, Double> remainingCapacity, ConstraintConfig config) {
        String resId = req.getResourceId();
        double demand = req.getRequestedQuantity();

        if (isIneligible(req)) {
            return unallocated(req, "Request is cancelled or ineligible for allocation.");
        }

        Optional<Resource> resOpt = inventory.getResource(resId);
        if (resOpt.isEmpty()) {
            return unallocated(req, "Resource does not exist in inventory.");
        }

        Resource resource = resOpt.get();
        if (config.isRespectInactiveStatus() && !resource.isActive()) {
            return unallocated(req, "Resource '" + resource.getName() + "' is currently inactive.");
        }

        double available = remainingCapacity.getOrDefault(resId, 0.0); // auto-unboxing Double -> double
        if (available < EPSILON) {
            return unallocated(req, "Resource capacity exhausted by higher-priority requests.");
        }

        double tentative = Math.min(demand, available);

        // Hard Constraint: All-Or-Nothing
        if (config.isEnforceAllOrNothing() && req.isAllOrNothing() && tentative < demand - EPSILON) {
            return unallocated(req, String.format(Locale.US,
                    "All-or-nothing constraint unsatisfied: demanded %.2f, but only %.2f was available.", demand, available));
        }

        // Hard Constraint: Minimum Acceptable Allocation
        if (config.isEnforceMinimumAcceptable() && req.getMinAcceptableQuantity() > 0
                && tentative < req.getMinAcceptableQuantity() - EPSILON) {
            return unallocated(req, String.format(Locale.US,
                    "Minimum acceptable threshold unsatisfied: minimum %.2f required, but only %.2f was available.",
                    req.getMinAcceptableQuantity(), available));
        }

        // Successful allocation
        remainingCapacity.put(resId, available - tentative);
        String explanation = (Math.abs(tentative - demand) < EPSILON)
                ? String.format(Locale.US, "Fully satisfied based on Priority %d ranking.", req.getPriority())
                : String.format(Locale.US,
                        "Partially satisfied: %.2f allocated due to remaining capacity limit of %.2f (Priority %d).",
                        tentative, available, req.getPriority());
        return allocated(req, tentative, explanation);
    }
}
