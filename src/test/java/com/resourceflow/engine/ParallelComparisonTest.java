package com.resourceflow.engine;

import com.resourceflow.evaluation.StrategyComparison;
import com.resourceflow.exception.ResourceFlowException;
import com.resourceflow.model.*;
import com.resourceflow.strategy.AllocationStrategy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class ParallelComparisonTest {

    private AllocationEngine engine;
    private ResourceInventory inventory;
    private List<AllocationRequest> requests;

    @BeforeEach
    void setUp() {
        engine = new AllocationEngine();
        inventory = new ResourceInventory(List.of(
                new Resource("GPU", "GPU", "Compute", 120.0, 120.0, "Hours", "", true),
                new Resource("CPU", "CPU", "Compute", 600.0, 600.0, "Cores", "", true)));
        requests = new ArrayList<>();
        for (int i = 1; i <= 30; i++) {
            String resource = (i % 2 == 0) ? "GPU" : "CPU";
            requests.add(new AllocationRequest("REQ-" + i, "Team " + i, resource, 10.0 + i * 3, 1 + i % 5,
                    0.0, false, LocalDateTime.now().minusMinutes(i), null, RequestStatus.PENDING, ""));
        }
    }

    @Test
    @Timeout(value = 10, unit = TimeUnit.SECONDS)
    @DisplayName("Parallel comparison gives exactly the same metrics as running strategies one by one")
    void parallelMatchesSequential() {
        StrategyComparison parallel = engine.compareAllStrategies(inventory, requests, ConstraintConfig.defaultConfig());

        for (StrategyType type : StrategyType.values()) {
            AllocationPlan sequential = engine.executeAllocation(type, inventory, requests);
            AllocationPlan threaded = parallel.getPlan(type).orElseThrow();
            assertEquals(sequential.getMetrics().getAggregateUtilisation(), threaded.getMetrics().getAggregateUtilisation(), 1e-9);
            assertEquals(sequential.getMetrics().getJainsFairnessIndex(), threaded.getMetrics().getJainsFairnessIndex(), 1e-9);
        }
    }

    @Test
    @Timeout(value = 10, unit = TimeUnit.SECONDS)
    @DisplayName("Callback is invoked once per strategy, each from its own worker thread")
    void callbackReportsEachWorkerThread() {
        // Set + synchronizedList because the callback is called from several threads at once
        Set<String> threadNames = Collections.synchronizedSet(new HashSet<>());
        List<StrategyType> finished = Collections.synchronizedList(new ArrayList<>());

        engine.compareAllStrategies(inventory, requests, ConstraintConfig.defaultConfig(),
                (plan, elapsedMillis, threadName) -> {
                    threadNames.add(threadName);
                    finished.add(plan.getStrategyType());
                    assertTrue(elapsedMillis >= 0);
                });

        assertEquals(3, finished.size());
        assertEquals(Set.of("Strategy-PRIORITY_BASED", "Strategy-FAIR_SHARE", "Strategy-PROPORTIONAL"), threadNames);
        assertFalse(threadNames.contains(Thread.currentThread().getName()), "work must not run on the caller's thread");
    }

    @Test
    @Timeout(value = 10, unit = TimeUnit.SECONDS)
    @DisplayName("A failing strategy is reported as a chained ResourceFlowException")
    void workerFailureIsChained() {
        // Anonymous class implementing the interface directly
        engine.registerStrategy(new AllocationStrategy() {
            @Override public StrategyType getType() { return StrategyType.FAIR_SHARE; }
            @Override public String getName() { return "Broken"; }
            @Override public String getDescription() { return "Always fails"; }
            @Override public AllocationPlan allocate(ResourceInventory inv, List<AllocationRequest> reqs, ConstraintConfig cfg) {
                throw new ArithmeticException("simulated bug");
            }
        });

        ResourceFlowException e = assertThrows(ResourceFlowException.class,
                () -> engine.compareAllStrategies(inventory, requests, ConstraintConfig.defaultConfig()));
        assertInstanceOf(ArithmeticException.class, e.getCause());
    }
}
