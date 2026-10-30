package com.resourceflow.evaluation;

import com.resourceflow.engine.AllocationEngine;
import com.resourceflow.model.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class WeightedScoreTest {

    private ConstraintConfig weights(double utilisation, double fulfilment, double fairness) {
        ConstraintConfig c = new ConstraintConfig();
        c.setUtilisationWeight(utilisation);
        c.setFulfilmentWeight(fulfilment);
        c.setFairnessWeight(fairness);
        return c;
    }

    @Test
    @DisplayName("Score is the weighted average of utilisation, fulfilment and fairness")
    void weightedAverage() {
        EvaluationMetrics m = new EvaluationMetrics();
        m.setAggregateUtilisation(80.0);   // 0.8
        m.setOverallFulfilment(60.0);      // 0.6
        m.setJainsFairnessIndex(0.9);

        assertEquals(0.8, m.weightedScore(weights(1, 0, 0)), 1e-9);
        assertEquals(0.9, m.weightedScore(weights(0, 0, 1)), 1e-9);
        assertEquals((0.8 + 0.6 + 0.9) / 3, m.weightedScore(weights(2, 2, 2)), 1e-9);
        assertEquals(0.0, m.weightedScore(weights(0, 0, 0)), 1e-9);
    }

    @Test
    @DisplayName("Changing the weights can change which strategy is recommended")
    void recommendationFollowsWeights() {
        Resource gpu = new Resource("GPU", "GPU", "Compute", 100.0, 100.0, "Hours", "", true);
        List<AllocationRequest> requests = List.of(
                new AllocationRequest("A", "A", "GPU", 90.0, 5, 0.0, false, LocalDateTime.now(), null, RequestStatus.PENDING, ""),
                new AllocationRequest("B", "B", "GPU", 30.0, 1, 0.0, false, LocalDateTime.now(), null, RequestStatus.PENDING, ""),
                new AllocationRequest("C", "C", "GPU", 30.0, 1, 0.0, false, LocalDateTime.now(), null, RequestStatus.PENDING, ""));

        StrategyComparison comparison = new AllocationEngine()
                .compareAllStrategies(new ResourceInventory(List.of(gpu)), requests, ConstraintConfig.defaultConfig());

        assertEquals(StrategyType.FAIR_SHARE, comparison.recommend(weights(0, 0, 1)), "fairness only");
        assertNotNull(comparison.recommend(ConstraintConfig.defaultConfig()));
    }

    @Test
    @DisplayName("Re-planning starts from total capacity, not from last run's leftovers")
    void replanningUsesTotalCapacity() {
        Resource used = new Resource("GPU", "GPU", "Compute", 100.0, 0.0, "Hours", "", true);
        ResourceInventory inventory = ResourceInventory.forReplanning(List.of(used));

        assertEquals(100.0, inventory.getResource("GPU").orElseThrow().getAvailableCapacity());
        assertEquals(0.0, used.getAvailableCapacity(), "the caller's object must not be modified");
    }
}
