package com.resourceflow.strategy;

import com.resourceflow.model.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class StrategyHierarchyTest {

    private final List<AllocationStrategy> strategies = List.of(
            new PriorityAllocationStrategy(),
            new FairShareAllocationStrategy(),
            new ProportionalAllocationStrategy());

    @Test
    @DisplayName("Name, description and complexity come from the @StrategyInfo annotation")
    void metadataIsReadFromAnnotation() {
        for (AllocationStrategy strategy : strategies) {
            StrategyInfo info = strategy.getClass().getAnnotation(StrategyInfo.class);
            assertNotNull(info, strategy.getClass().getSimpleName() + " is missing @StrategyInfo");
            assertEquals(info.name(), strategy.getName());
            assertEquals(info.description(), strategy.getDescription());
            assertEquals(info.complexity(), strategy.getComplexity());
        }
    }

    @Test
    @DisplayName("Runtime polymorphism: one interface reference, three different behaviours")
    void dynamicDispatchPicksTheSubclassAlgorithm() {
        Resource gpu = new Resource("GPU", "GPU", "Compute", 100.0, 100.0, "Hours", "", true);
        List<AllocationRequest> requests = List.of(
                new AllocationRequest("BIG", "Big Lab", "GPU", 90.0, 5, 0.0, false, LocalDateTime.now(), null, RequestStatus.PENDING, ""),
                new AllocationRequest("SMALL", "Small Lab", "GPU", 30.0, 1, 0.0, false, LocalDateTime.now(), null, RequestStatus.PENDING, ""));

        double[] smallLabShare = new double[strategies.size()];
        for (int i = 0; i < strategies.size(); i++) {
            AllocationStrategy strategy = strategies.get(i); // static type: the interface
            AllocationPlan plan = strategy.allocate(new ResourceInventory(List.of(gpu)), requests, null);
            smallLabShare[i] = plan.getItemForRequest("SMALL").orElseThrow().getAllocatedQuantity();
            assertEquals(strategy.getType(), plan.getStrategyType());
        }

        assertEquals(10.0, smallLabShare[0], 1e-6, "Priority: big lab (P5) is served first");
        assertEquals(30.0, smallLabShare[1], 1e-6, "Fair share: small demand fits in the equal share");
        assertEquals(100.0 * 30.0 / (90.0 * 5 + 30.0), smallLabShare[2], 1e-6, "Proportional: weight = demand x priority");
    }

    @Test
    @DisplayName("Inheritance chain: Fair-Share and Proportional share the ShareBased parent")
    void multilevelInheritance() {
        assertInstanceOf(ShareBasedAllocationStrategy.class, strategies.get(1));
        assertInstanceOf(ShareBasedAllocationStrategy.class, strategies.get(2));
        assertFalse(strategies.get(0) instanceof ShareBasedAllocationStrategy);
        strategies.forEach(s -> assertInstanceOf(AbstractAllocationStrategy.class, s));
    }

    @Test
    @DisplayName("Interface constant is shared by all implementations")
    void interfaceVariableIsAConstant() {
        assertEquals(1e-6, AllocationStrategy.EPSILON);
    }

    /** A subclass that forgot the annotation (static nested class, no @StrategyInfo). */
    static class UnannotatedStrategy extends PriorityAllocationStrategy {
    }

    @Test
    @DisplayName("Annotation is inherited only when declared @Inherited, so a bare subclass is rejected")
    void missingAnnotationIsRejected() {
        IllegalStateException e = assertThrows(IllegalStateException.class, UnannotatedStrategy::new);
        assertTrue(e.getMessage().contains("@StrategyInfo"));
    }

    @Test
    @DisplayName("Empty request list returns an empty plan without calling the algorithm")
    void templateMethodHandlesEmptyInput() {
        for (AllocationStrategy strategy : strategies) {
            AllocationPlan plan = strategy.allocate(new ResourceInventory(), List.of(), null);
            assertTrue(plan.getItems().isEmpty());
        }
    }
}
