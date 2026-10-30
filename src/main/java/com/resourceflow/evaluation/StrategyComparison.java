package com.resourceflow.evaluation;

import com.resourceflow.model.AllocationPlan;
import com.resourceflow.model.ConstraintConfig;
import com.resourceflow.model.StrategyType;

import java.util.*;

/**
 * Data structure holding side-by-side comparative metrics for multiple allocation plans.
 */
public class StrategyComparison {
    private final Map<StrategyType, AllocationPlan> planMap;
    private StrategyType bestUtilisationStrategy;
    private StrategyType bestFairnessStrategy;
    private StrategyType bestFulfilmentStrategy;

    public StrategyComparison() {
        this.planMap = new EnumMap<>(StrategyType.class);
    }

    public void addPlan(AllocationPlan plan) {
        if (plan != null && plan.getStrategyType() != null) {
            planMap.put(plan.getStrategyType(), plan);
            recalculateBests();
        }
    }

    private void recalculateBests() {
        if (planMap.isEmpty()) return;

        bestUtilisationStrategy = planMap.entrySet().stream()
                .max(Comparator.comparingDouble(e -> e.getValue().getMetrics().getAggregateUtilisation()))
                .map(Map.Entry::getKey)
                .orElse(null);

        bestFairnessStrategy = planMap.entrySet().stream()
                .max(Comparator.comparingDouble(e -> e.getValue().getMetrics().getJainsFairnessIndex()))
                .map(Map.Entry::getKey)
                .orElse(null);

        bestFulfilmentStrategy = planMap.entrySet().stream()
                .max(Comparator.comparingDouble(e -> e.getValue().getMetrics().getOverallFulfilment()))
                .map(Map.Entry::getKey)
                .orElse(null);
    }

    public Map<StrategyType, AllocationPlan> getPlanMap() {
        return Collections.unmodifiableMap(planMap);
    }

    public Optional<AllocationPlan> getPlan(StrategyType strategyType) {
        return Optional.ofNullable(planMap.get(strategyType));
    }

    public StrategyType getBestUtilisationStrategy() {
        return bestUtilisationStrategy;
    }

    public StrategyType getBestFairnessStrategy() {
        return bestFairnessStrategy;
    }

    public StrategyType getBestFulfilmentStrategy() {
        return bestFulfilmentStrategy;
    }

    /**
     * The strategy with the highest weighted score for the given objective weights.
     * Not cached: the user can move the weight sliders without re-running the strategies.
     */
    public StrategyType recommend(ConstraintConfig weights) {
        return planMap.entrySet().stream()
                .max(Comparator.comparingDouble(e -> e.getValue().getMetrics().weightedScore(weights)))
                .map(Map.Entry::getKey)
                .orElse(null);
    }
}
