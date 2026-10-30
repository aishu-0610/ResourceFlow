package com.resourceflow.strategy;

import com.resourceflow.model.AllocationRequest;
import com.resourceflow.model.StrategyType;

import java.util.*;

/**
 * Proportional Allocation Strategy.
 * Distributes capacity in proportion to requested quantities and priority weights,
 * while iteratively pruning and redistributing when hard constraints are violated.
 */
@StrategyInfo(
        name = "Proportional Allocation",
        description = "Distributes available capacity in proportion to requested demand and priority weights. "
                + "Rebalances capacity iteratively to respect minimum thresholds and all-or-nothing constraints.",
        complexity = "O(n^2)")
public class ProportionalAllocationStrategy extends ShareBasedAllocationStrategy {

    public ProportionalAllocationStrategy() {
        super(StrategyType.PROPORTIONAL);
    }

    @Override
    protected String shareLabel() {
        return "proportional share";
    }

    @Override
    protected String describePartial(AllocationRequest req, double allocated) {
        return String.format(Locale.US,
                "Partially satisfied: Received %.2f proportional share based on demand (%.2f) and Priority %d.",
                allocated, req.getRequestedQuantity(), req.getPriority());
    }

    /**
     * If capacity covers total demand everyone is fully served; otherwise each request gets
     * capacity * (demand * priority) / sum(demand * priority), capped at its own demand.
     */
    @Override
    protected Map<String, Double> computeShares(double capacity, List<AllocationRequest> active) {
        Map<String, Double> shares = new HashMap<>();
        double totalDemand = active.stream().mapToDouble(AllocationRequest::getRequestedQuantity).sum();

        if (totalDemand <= capacity + EPSILON) {
            for (AllocationRequest r : active) {
                shares.put(r.getRequestId(), r.getRequestedQuantity());
            }
            return shares;
        }

        double sumWeights = active.stream()
                .mapToDouble(r -> r.getRequestedQuantity() * r.getPriority())
                .sum();
        if (sumWeights < EPSILON) {
            sumWeights = 1.0;
        }

        for (AllocationRequest r : active) {
            double weight = r.getRequestedQuantity() * r.getPriority();
            shares.put(r.getRequestId(), Math.min(r.getRequestedQuantity(), capacity * (weight / sumWeights)));
        }
        return shares;
    }
}
