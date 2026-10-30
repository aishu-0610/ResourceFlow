package com.resourceflow.strategy;

import com.resourceflow.model.AllocatedItem;
import com.resourceflow.model.AllocationRequest;
import com.resourceflow.model.ConstraintConfig;
import com.resourceflow.model.Resource;
import com.resourceflow.model.ResourceInventory;
import com.resourceflow.model.StrategyType;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Second level of the strategy hierarchy (multilevel inheritance):
 *
 *   AllocationStrategy (interface)
 *     -> AbstractAllocationStrategy (abstract)
 *          -> ShareBasedAllocationStrategy (abstract, this class)
 *               -> FairShareAllocationStrategy
 *               -> ProportionalAllocationStrategy
 *
 * Fair-Share and Proportional differ only in HOW one resource's capacity is split.
 * Everything else lives here once: grouping requests by resource, skipping missing or
 * inactive resources, and the constraint-pruning loop that removes the worst violator
 * and re-splits until every hard constraint holds.
 */
public abstract class ShareBasedAllocationStrategy extends AbstractAllocationStrategy {

    protected ShareBasedAllocationStrategy(StrategyType type) {
        super(type);
    }

    /**
     * Splits {@code capacity} among {@code active} requests. Returns requestId -> allocated quantity.
     */
    protected abstract Map<String, Double> computeShares(double capacity, List<AllocationRequest> active);

    /** Explanation for a request that was only partly served. */
    protected abstract String describePartial(AllocationRequest req, double allocated);

    /** Short label used in explanations, e.g. "fair share". */
    protected abstract String shareLabel();

    @Override
    protected final List<AllocatedItem> computeItems(ResourceInventory inventory, List<AllocationRequest> requests,
                                                     ConstraintConfig config) {
        List<AllocatedItem> items = new ArrayList<>();

        Map<String, List<AllocationRequest>> requestsByResource = requests.stream()
                .collect(Collectors.groupingBy(AllocationRequest::getResourceId, LinkedHashMap::new, Collectors.toList()));

        for (Map.Entry<String, List<AllocationRequest>> entry : requestsByResource.entrySet()) {
            List<AllocationRequest> resourceRequests = entry.getValue();

            Optional<Resource> resOpt = inventory.getResource(entry.getKey());
            if (resOpt.isEmpty()) {
                resourceRequests.forEach(req -> items.add(unallocated(req, "Resource does not exist in inventory.")));
                continue;
            }

            Resource resource = resOpt.get();
            if (config.isRespectInactiveStatus() && !resource.isActive()) {
                resourceRequests.forEach(req -> items.add(
                        unallocated(req, "Resource '" + resource.getName() + "' is currently inactive.")));
                continue;
            }

            List<AllocationRequest> eligible = new ArrayList<>();
            for (AllocationRequest req : resourceRequests) {
                if (isIneligible(req)) {
                    items.add(unallocated(req, "Request is cancelled or ineligible for allocation."));
                } else {
                    eligible.add(req);
                }
            }

            double capacity = resource.getAvailableCapacity();
            Map<String, Double> allocations = allocateWithPruning(capacity, eligible, config);

            for (AllocationRequest req : eligible) {
                double allocated = allocations.getOrDefault(req.getRequestId(), 0.0);
                items.add(allocated(req, allocated, explain(req, allocated, capacity)));
            }
        }
        return items;
    }

    /**
     * Splits capacity, then repeatedly drops the request with the largest constraint deficit
     * (all-or-nothing or minimum threshold) and re-splits, until no constraint is violated.
     */
    private Map<String, Double> allocateWithPruning(double capacity, List<AllocationRequest> candidates,
                                                    ConstraintConfig config) {
        Set<String> excluded = new HashSet<>();

        while (true) {
            List<AllocationRequest> active = candidates.stream()
                    .filter(r -> !excluded.contains(r.getRequestId()))
                    .collect(Collectors.toList());

            if (active.isEmpty() || capacity < EPSILON) {
                return new HashMap<>();
            }

            Map<String, Double> tentative = computeShares(capacity, active);
            String worstViolator = findWorstViolator(active, tentative, config);

            if (worstViolator == null) {
                return tentative;
            }
            excluded.add(worstViolator);
        }
    }

    private String findWorstViolator(List<AllocationRequest> active, Map<String, Double> tentative,
                                     ConstraintConfig config) {
        String worst = null;
        double maxDeficit = -1.0;

        for (AllocationRequest req : active) {
            double alloc = tentative.getOrDefault(req.getRequestId(), 0.0);
            double deficit = 0.0;

            if (config.isEnforceAllOrNothing() && req.isAllOrNothing()
                    && alloc < req.getRequestedQuantity() - EPSILON && alloc > EPSILON) {
                deficit = req.getRequestedQuantity() - alloc;
            } else if (config.isEnforceMinimumAcceptable() && req.getMinAcceptableQuantity() > 0
                    && alloc < req.getMinAcceptableQuantity() - EPSILON && alloc > EPSILON) {
                deficit = req.getMinAcceptableQuantity() - alloc;
            }

            if (deficit > 0 && deficit > maxDeficit) {
                maxDeficit = deficit;
                worst = req.getRequestId();
            }
        }
        return worst;
    }

    private String explain(AllocationRequest req, double allocated, double capacity) {
        double demanded = req.getRequestedQuantity();
        if (Math.abs(allocated - demanded) < EPSILON) {
            return "Fully satisfied under " + getName() + ".";
        }
        if (allocated > EPSILON) {
            return describePartial(req, allocated);
        }
        if (capacity < EPSILON) {
            return "Unsatisfied: Zero capacity available on resource.";
        }
        if (req.isAllOrNothing()) {
            return "Unsatisfied: The " + shareLabel() + " could not meet the 100% all-or-nothing demand.";
        }
        if (req.getMinAcceptableQuantity() > 0) {
            return String.format(Locale.US,
                    "Unsatisfied: The %s was below the minimum acceptable threshold of %.2f.",
                    shareLabel(), req.getMinAcceptableQuantity());
        }
        return "Unsatisfied: Insufficient capacity to allocate a " + shareLabel() + ".";
    }
}
