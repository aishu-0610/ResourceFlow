package com.resourceflow.evaluation;

import com.resourceflow.constraint.ConstraintValidator;
import com.resourceflow.constraint.StandardConstraintValidator;
import com.resourceflow.model.AllocatedItem;
import com.resourceflow.model.AllocationPlan;
import com.resourceflow.model.AllocationRequest;
import com.resourceflow.model.ConstraintConfig;
import com.resourceflow.model.EvaluationMetrics;
import com.resourceflow.model.Resource;
import com.resourceflow.model.ResourceInventory;

import java.util.*;

/**
 * Standard implementation of OptimisationEvaluator.
 * Computes exact mathematical metrics including Jain's fairness index, demand-weighted
 * fulfilment, aggregate utilisation, and multi-plan comparative analysis.
 */
public class StandardOptimisationEvaluator implements OptimisationEvaluator {

    private static final double EPSILON = 1e-6;
    private final ConstraintValidator constraintValidator;

    public StandardOptimisationEvaluator() {
        this.constraintValidator = new StandardConstraintValidator();
    }

    public StandardOptimisationEvaluator(ConstraintValidator constraintValidator) {
        this.constraintValidator = (constraintValidator != null) ? constraintValidator : new StandardConstraintValidator();
    }

    @Override
    public EvaluationMetrics evaluate(ResourceInventory inventory, List<AllocationRequest> originalRequests, List<AllocatedItem> allocations) {
        if (allocations == null) {
            allocations = Collections.emptyList();
        }

        // 1. Calculate per-resource allocations
        Map<String, Double> allocatedByResource = new LinkedHashMap<>();
        for (AllocatedItem item : allocations) {
            allocatedByResource.merge(item.getResourceId(), Math.max(0.0, item.getAllocatedQuantity()), Double::sum);
        }

        // 2. Resource Utilisation Map & Unallocated Capacity
        Map<String, Double> utilisationMap = new LinkedHashMap<>();
        double totalUnallocatedCapacity = 0.0;
        double sumUtilisation = 0.0;
        int activeResourceCount = 0;

        for (Resource res : inventory.getAllResources()) {
            if (res.isActive()) {
                activeResourceCount++;
                double capacity = res.getAvailableCapacity();
                double allocated = allocatedByResource.getOrDefault(res.getResourceId(), 0.0);
                double utilisation = (capacity > EPSILON) ? Math.min(100.0, (allocated / capacity) * 100.0) : 0.0;
                utilisationMap.put(res.getResourceId(), utilisation);

                sumUtilisation += utilisation;
                totalUnallocatedCapacity += Math.max(0.0, capacity - allocated);
            }
        }

        double aggregateUtilisation = (activeResourceCount > 0) ? (sumUtilisation / activeResourceCount) : 0.0;

        // 3. Request Fulfilment Metrics
        double totalRequested = 0.0;
        double totalAllocated = 0.0;
        double sumFulfilmentRatios = 0.0;
        double sumSquaredFulfilmentRatios = 0.0;
        int fullySatisfied = 0;
        int partiallySatisfied = 0;
        int unsatisfied = 0;
        int totalRequests = allocations.size();

        for (AllocatedItem item : allocations) {
            double req = item.getRequestedQuantity();
            double alloc = Math.max(0.0, item.getAllocatedQuantity());

            totalRequested += req;
            totalAllocated += alloc;

            double ratio = (req > EPSILON) ? Math.min(1.0, alloc / req) : 0.0;
            sumFulfilmentRatios += ratio;
            sumSquaredFulfilmentRatios += (ratio * ratio);

            if (Math.abs(alloc - req) < EPSILON || alloc >= req) {
                fullySatisfied++;
            } else if (alloc > EPSILON) {
                partiallySatisfied++;
            } else {
                unsatisfied++;
            }
        }

        // Demand-weighted fulfilment (%)
        double overallFulfilment = (totalRequested > EPSILON) ? Math.min(100.0, (totalAllocated / totalRequested) * 100.0) : 100.0;
        // Arithmetic mean fulfilment (%)
        double averageFulfilment = (totalRequests > 0) ? (sumFulfilmentRatios / totalRequests) * 100.0 : 100.0;

        // 4. Jain's Fairness Index: J = (sum(x_i))^2 / (n * sum(x_i^2))
        double jainsIndex;
        if (totalRequests == 0) {
            jainsIndex = 1.0; // Perfect fairness when no requests compete
        } else if (sumSquaredFulfilmentRatios < EPSILON) {
            jainsIndex = 0.0; // Zero allocation across all requests
        } else {
            jainsIndex = (sumFulfilmentRatios * sumFulfilmentRatios) / (totalRequests * sumSquaredFulfilmentRatios);
            jainsIndex = Math.max(0.0, Math.min(1.0, jainsIndex));
        }

        // 5. Hard constraint validation
        List<String> violations = constraintValidator.validateHardConstraints(inventory, allocations, ConstraintConfig.defaultConfig());

        return new EvaluationMetrics(
                aggregateUtilisation,
                overallFulfilment,
                averageFulfilment,
                jainsIndex,
                fullySatisfied,
                partiallySatisfied,
                unsatisfied,
                totalUnallocatedCapacity,
                utilisationMap,
                violations
        );
    }

    @Override
    public StrategyComparison compare(List<AllocationPlan> plans) {
        StrategyComparison comparison = new StrategyComparison();
        if (plans != null) {
            for (AllocationPlan plan : plans) {
                comparison.addPlan(plan);
            }
        }
        return comparison;
    }
}
