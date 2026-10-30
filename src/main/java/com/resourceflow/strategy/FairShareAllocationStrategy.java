package com.resourceflow.strategy;

import com.resourceflow.model.AllocationRequest;
import com.resourceflow.model.StrategyType;

import java.util.*;

/**
 * Fair-Share Allocation Strategy.
 * Implements progressive Max-Min Fair Share to distribute resources equitably,
 * preventing large requests from monopolizing capacity.
 */
@StrategyInfo(
        name = "Fair-Share (Max-Min) Allocation",
        description = "Distributes available capacity evenly using iterative max-min fairness. "
                + "Small requests are satisfied first, and residual capacity is shared equally among remaining requests.",
        complexity = "O(n^2)")
public class FairShareAllocationStrategy extends ShareBasedAllocationStrategy {

    public FairShareAllocationStrategy() {
        super(StrategyType.FAIR_SHARE);
    }

    @Override
    protected String shareLabel() {
        return "fair share";
    }

    @Override
    protected String describePartial(AllocationRequest req, double allocated) {
        return String.format(Locale.US,
                "Partially satisfied: Fair share of %.2f units received out of %.2f demanded.",
                allocated, req.getRequestedQuantity());
    }

    /**
     * Max-Min Fair Share "water-filling": offer everyone an equal share; anyone who needs less
     * takes only what they need, and the leftover is re-shared among the rest.
     */
    @Override
    protected Map<String, Double> computeShares(double totalCapacity, List<AllocationRequest> requests) {
        Map<String, Double> allocations = new HashMap<>();
        List<AllocationRequest> unsatisfied = new ArrayList<>(requests);
        double remainingCapacity = totalCapacity;

        while (!unsatisfied.isEmpty() && remainingCapacity > EPSILON) {
            double equalShare = remainingCapacity / unsatisfied.size();
            List<AllocationRequest> satisfiedInRound = new ArrayList<>();

            for (AllocationRequest req : unsatisfied) {
                if (req.getRequestedQuantity() <= equalShare + EPSILON) {
                    satisfiedInRound.add(req);
                }
            }

            if (satisfiedInRound.isEmpty()) {
                // Every remaining request demands more than equalShare; divide equally
                for (AllocationRequest req : unsatisfied) {
                    allocations.put(req.getRequestId(), equalShare);
                }
                remainingCapacity = 0.0;
            } else {
                for (AllocationRequest req : satisfiedInRound) {
                    allocations.put(req.getRequestId(), req.getRequestedQuantity());
                    remainingCapacity -= req.getRequestedQuantity();
                    unsatisfied.remove(req);
                }
            }
        }

        for (AllocationRequest req : unsatisfied) {
            allocations.putIfAbsent(req.getRequestId(), 0.0);
        }
        return allocations;
    }
}
