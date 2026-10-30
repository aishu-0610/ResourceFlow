package com.resourceflow.engine;

import com.resourceflow.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class AllocationEngineTest {

    private AllocationEngine engine;
    private Resource gpuCluster;
    private Resource cpuPool;

    @BeforeEach
    void setUp() {
        engine = new AllocationEngine();
        gpuCluster = new Resource("RES-GPU", "GPU Cluster", "Compute", 100.0, 100.0, "Hours", "Nvidia H100s", true);
        cpuPool = new Resource("RES-CPU", "CPU Pool", "Compute", 500.0, 500.0, "Cores", "AMD Epyc", true);
    }

    @Test
    @DisplayName("Single request with sufficient capacity receives full allocation")
    void testSingleRequestSufficientCapacity() {
        ResourceInventory inventory = new ResourceInventory(List.of(gpuCluster));
        AllocationRequest req = new AllocationRequest("REQ-1", "Alice", "RES-GPU", 40.0, 3, 0.0, false, LocalDateTime.now(), null, RequestStatus.PENDING, "");

        AllocationPlan plan = engine.executeAllocation(StrategyType.PRIORITY_BASED, inventory, List.of(req), ConstraintConfig.defaultConfig());

        assertTrue(plan.isFeasible());
        assertEquals(1, plan.getItems().size());
        AllocatedItem item = plan.getItems().getFirst();
        assertEquals(40.0, item.getAllocatedQuantity(), 1e-6);
        assertEquals(AllocationStatus.FULLY_SATISFIED, item.getStatus());
        assertEquals(40.0, plan.getMetrics().getAggregateUtilisation(), 1e-6);
        assertEquals(100.0, plan.getMetrics().getOverallFulfilment(), 1e-6);
        assertEquals(1.0, plan.getMetrics().getJainsFairnessIndex(), 1e-6);
    }

    @Test
    @DisplayName("Multiple requests competing with insufficient capacity under Priority allocation")
    void testMultipleRequestsPriorityAllocation() {
        ResourceInventory inventory = new ResourceInventory(List.of(gpuCluster)); // 100.0 total
        AllocationRequest reqHigh = new AllocationRequest("REQ-HIGH", "Bob", "RES-GPU", 70.0, 5, 0.0, false, LocalDateTime.now(), null, RequestStatus.PENDING, "");
        AllocationRequest reqLow = new AllocationRequest("REQ-LOW", "Charlie", "RES-GPU", 50.0, 2, 0.0, false, LocalDateTime.now(), null, RequestStatus.PENDING, "");

        AllocationPlan plan = engine.executeAllocation(StrategyType.PRIORITY_BASED, inventory, List.of(reqLow, reqHigh), ConstraintConfig.defaultConfig());

        assertTrue(plan.isFeasible());
        AllocatedItem highItem = plan.getItemForRequest("REQ-HIGH").orElseThrow();
        AllocatedItem lowItem = plan.getItemForRequest("REQ-LOW").orElseThrow();

        assertEquals(70.0, highItem.getAllocatedQuantity(), 1e-6);
        assertEquals(AllocationStatus.FULLY_SATISFIED, highItem.getStatus());

        assertEquals(30.0, lowItem.getAllocatedQuantity(), 1e-6);
        assertEquals(AllocationStatus.PARTIALLY_SATISFIED, lowItem.getStatus());

        assertEquals(100.0, plan.getMetrics().getAggregateUtilisation(), 1e-6);
        assertEquals(0.0, plan.getMetrics().getUnallocatedCapacity(), 1e-6);
    }

    @Test
    @DisplayName("Fair-Share Max-Min distributes capacity evenly across competing requests")
    void testFairShareAllocation() {
        ResourceInventory inventory = new ResourceInventory(List.of(gpuCluster)); // 100.0 capacity
        // 3 requests: R1 demands 20, R2 demands 60, R3 demands 60
        AllocationRequest r1 = new AllocationRequest("R1", "Alice", "RES-GPU", 20.0, 3, 0.0, false, LocalDateTime.now(), null, RequestStatus.PENDING, "");
        AllocationRequest r2 = new AllocationRequest("R2", "Bob", "RES-GPU", 60.0, 3, 0.0, false, LocalDateTime.now(), null, RequestStatus.PENDING, "");
        AllocationRequest r3 = new AllocationRequest("R3", "Charlie", "RES-GPU", 60.0, 3, 0.0, false, LocalDateTime.now(), null, RequestStatus.PENDING, "");

        AllocationPlan plan = engine.executeAllocation(StrategyType.FAIR_SHARE, inventory, List.of(r1, r2, r3), ConstraintConfig.defaultConfig());

        assertTrue(plan.isFeasible());
        // Under Max-Min: R1 receives its full demand 20. Remaining 80 is split equally between R2 and R3 (40 each).
        assertEquals(20.0, plan.getItemForRequest("R1").orElseThrow().getAllocatedQuantity(), 1e-6);
        assertEquals(40.0, plan.getItemForRequest("R2").orElseThrow().getAllocatedQuantity(), 1e-6);
        assertEquals(40.0, plan.getItemForRequest("R3").orElseThrow().getAllocatedQuantity(), 1e-6);

        assertEquals(1, plan.getMetrics().getFullySatisfiedCount());
        assertEquals(2, plan.getMetrics().getPartiallySatisfiedCount());
        assertEquals(0, plan.getMetrics().getUnsatisfiedCount());
        assertEquals(100.0, plan.getMetrics().getAggregateUtilisation(), 1e-6);
    }

    @Test
    @DisplayName("Proportional allocation distributes capacity by weighted demand")
    void testProportionalAllocation() {
        ResourceInventory inventory = new ResourceInventory(List.of(gpuCluster)); // 100.0 capacity
        // Two requests with identical priority (P=3), demanding 40 and 60 (total 100)
        AllocationRequest r1 = new AllocationRequest("R1", "Alice", "RES-GPU", 40.0, 3, 0.0, false, LocalDateTime.now(), null, RequestStatus.PENDING, "");
        AllocationRequest r2 = new AllocationRequest("R2", "Bob", "RES-GPU", 60.0, 3, 0.0, false, LocalDateTime.now(), null, RequestStatus.PENDING, "");

        AllocationPlan plan = engine.executeAllocation(StrategyType.PROPORTIONAL, inventory, List.of(r1, r2), ConstraintConfig.defaultConfig());

        assertTrue(plan.isFeasible());
        assertEquals(40.0, plan.getItemForRequest("R1").orElseThrow().getAllocatedQuantity(), 1e-6);
        assertEquals(60.0, plan.getItemForRequest("R2").orElseThrow().getAllocatedQuantity(), 1e-6);
        assertEquals(100.0, plan.getMetrics().getOverallFulfilment(), 1e-6);
    }

    @Test
    @DisplayName("Empty request list is handled safely without division by zero")
    void testEmptyRequestList() {
        ResourceInventory inventory = new ResourceInventory(List.of(gpuCluster, cpuPool));
        AllocationPlan plan = engine.executeAllocation(StrategyType.PRIORITY_BASED, inventory, Collections.emptyList(), ConstraintConfig.defaultConfig());

        assertTrue(plan.isFeasible());
        assertTrue(plan.getItems().isEmpty());
        assertEquals(0.0, plan.getMetrics().getAggregateUtilisation(), 1e-6);
        assertEquals(1.0, plan.getMetrics().getJainsFairnessIndex(), 1e-6);
        assertEquals(0, plan.getMetrics().getTotalRequests());
    }

    @Test
    @DisplayName("Hard Constraint: All-Or-Nothing atomicity rejects partial allocation")
    void testAllOrNothingConstraint() {
        gpuCluster.setAvailableCapacity(50.0);
        ResourceInventory inventory = new ResourceInventory(List.of(gpuCluster));

        // Demands 60 units with all-or-nothing flag
        AllocationRequest atomicReq = new AllocationRequest("REQ-ATOMIC", "Dave", "RES-GPU", 60.0, 5, 0.0, true, LocalDateTime.now(), null, RequestStatus.PENDING, "");

        AllocationPlan plan = engine.executeAllocation(StrategyType.PRIORITY_BASED, inventory, List.of(atomicReq), ConstraintConfig.defaultConfig());

        assertTrue(plan.isFeasible());
        AllocatedItem item = plan.getItems().getFirst();
        assertEquals(0.0, item.getAllocatedQuantity(), 1e-6);
        assertEquals(AllocationStatus.UNSATISFIED, item.getStatus());
        assertTrue(item.getExplanation().contains("All-or-nothing constraint unsatisfied"));
    }

    @Test
    @DisplayName("Hard Constraint: Minimum acceptable allocation rejects insufficient allocations")
    void testMinimumAcceptableThreshold() {
        gpuCluster.setAvailableCapacity(20.0);
        ResourceInventory inventory = new ResourceInventory(List.of(gpuCluster));

        // Demands 50 units, minimum acceptable is 25
        AllocationRequest req = new AllocationRequest("REQ-MIN", "Eve", "RES-GPU", 50.0, 4, 25.0, false, LocalDateTime.now(), null, RequestStatus.PENDING, "");

        AllocationPlan plan = engine.executeAllocation(StrategyType.PRIORITY_BASED, inventory, List.of(req), ConstraintConfig.defaultConfig());

        assertTrue(plan.isFeasible());
        AllocatedItem item = plan.getItems().getFirst();
        assertEquals(0.0, item.getAllocatedQuantity(), 1e-6);
        assertEquals(AllocationStatus.UNSATISFIED, item.getStatus());
        assertTrue(item.getExplanation().contains("Minimum acceptable threshold unsatisfied"));
    }

    @Test
    @DisplayName("Inactive resource produces 0 allocation and proper explanation")
    void testInactiveResourceHandling() {
        gpuCluster.setActive(false);
        ResourceInventory inventory = new ResourceInventory(List.of(gpuCluster));

        AllocationRequest req = new AllocationRequest("REQ-1", "Frank", "RES-GPU", 10.0, 3, 0.0, false, LocalDateTime.now(), null, RequestStatus.PENDING, "");

        AllocationPlan plan = engine.executeAllocation(StrategyType.PRIORITY_BASED, inventory, List.of(req), ConstraintConfig.defaultConfig());

        assertTrue(plan.isFeasible());
        AllocatedItem item = plan.getItems().getFirst();
        assertEquals(0.0, item.getAllocatedQuantity(), 1e-6);
        assertTrue(item.getExplanation().contains("currently inactive"));
    }

    @Test
    @DisplayName("Cancelled requests are excluded from allocation")
    void testCancelledRequestsExcluded() {
        ResourceInventory inventory = new ResourceInventory(List.of(gpuCluster));
        AllocationRequest cancelledReq = new AllocationRequest("REQ-CAN", "Grace", "RES-GPU", 30.0, 5, 0.0, false, LocalDateTime.now(), null, RequestStatus.CANCELLED, "");

        AllocationPlan plan = engine.executeAllocation(StrategyType.PRIORITY_BASED, inventory, List.of(cancelledReq), ConstraintConfig.defaultConfig());

        assertTrue(plan.isFeasible());
        AllocatedItem item = plan.getItems().getFirst();
        assertEquals(0.0, item.getAllocatedQuantity(), 1e-6);
        assertEquals(AllocationStatus.UNSATISFIED, item.getStatus());
        assertTrue(item.getExplanation().contains("cancelled"));
    }

    @Test
    @DisplayName("Adaptive reallocation computes correct diff when capacity decreases")
    void testAdaptiveReallocationCapacityDecrease() {
        ResourceInventory baselineInventory = new ResourceInventory(List.of(gpuCluster)); // 100.0
        AllocationRequest r1 = new AllocationRequest("R1", "Alice", "RES-GPU", 50.0, 5, 0.0, false, LocalDateTime.now(), null, RequestStatus.PENDING, "");
        AllocationRequest r2 = new AllocationRequest("R2", "Bob", "RES-GPU", 50.0, 3, 0.0, false, LocalDateTime.now(), null, RequestStatus.PENDING, "");

        AllocationPlan baselinePlan = engine.executeAllocation(StrategyType.PRIORITY_BASED, baselineInventory, List.of(r1, r2), ConstraintConfig.defaultConfig());
        assertEquals(50.0, baselinePlan.getItemForRequest("R1").orElseThrow().getAllocatedQuantity(), 1e-6);
        assertEquals(50.0, baselinePlan.getItemForRequest("R2").orElseThrow().getAllocatedQuantity(), 1e-6);

        // Capacity drops to 70.0
        Resource reducedGpu = gpuCluster.copy();
        reducedGpu.setAvailableCapacity(70.0);
        ResourceInventory newInventory = new ResourceInventory(List.of(reducedGpu));

        AllocationEngine.AdaptiveResult result = engine.adaptiveReallocate(baselinePlan, StrategyType.PRIORITY_BASED, newInventory, List.of(r1, r2), ConstraintConfig.defaultConfig());

        ReallocationDiff diff = result.getDiff();
        assertEquals(1, diff.getUnchangedRequests().size()); // R1 stays 50.0
        assertEquals(1, diff.getDegradedRequests().size()); // R2 drops from 50 to 20
        ReallocationDiff.DiffItem degraded = diff.getDegradedRequests().getFirst();
        assertEquals("R2", degraded.getRequestId());
        assertEquals(50.0, degraded.getPreviousAllocation(), 1e-6);
        assertEquals(20.0, degraded.getNewAllocation(), 1e-6);
    }

    @Test
    @DisplayName("Decision explanations provide causality for every request")
    void testDecisionExplanations() {
        ResourceInventory inventory = new ResourceInventory(List.of(gpuCluster));
        AllocationRequest r1 = new AllocationRequest("R1", "Alice", "RES-GPU", 60.0, 5, 0.0, false, LocalDateTime.now(), null, RequestStatus.PENDING, "");
        AllocationPlan plan = engine.executeAllocation(StrategyType.PRIORITY_BASED, inventory, List.of(r1), ConstraintConfig.defaultConfig());

        List<DecisionExplanation> explanations = engine.explainPlan(plan, inventory, List.of(r1));
        assertEquals(1, explanations.size());
        DecisionExplanation exp = explanations.getFirst();
        assertEquals("R1", exp.getRequestId());
        assertEquals(60.0, exp.getAllocatedQuantity(), 1e-6);
        assertEquals(AllocationStatus.FULLY_SATISFIED, exp.getStatus());
        assertFalse(exp.getPrimaryReason().isEmpty());
    }
}
