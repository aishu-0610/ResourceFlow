package com.resourceflow.engine;

import com.resourceflow.constraint.ConstraintValidator;
import com.resourceflow.constraint.StandardConstraintValidator;
import com.resourceflow.evaluation.OptimisationEvaluator;
import com.resourceflow.evaluation.StandardOptimisationEvaluator;
import com.resourceflow.evaluation.StrategyComparison;
import com.resourceflow.model.*;
import com.resourceflow.strategy.AllocationStrategy;
import com.resourceflow.strategy.FairShareAllocationStrategy;
import com.resourceflow.strategy.PriorityAllocationStrategy;
import com.resourceflow.strategy.ProportionalAllocationStrategy;

import java.util.*;

/**
 * Core orchestration engine for generating, evaluating, and comparing allocation plans.
 */
public class AllocationEngine {

    private final Map<StrategyType, AllocationStrategy> registeredStrategies;
    private final ConstraintValidator constraintValidator;
    private final OptimisationEvaluator optimisationEvaluator;
    private final AllocationExplainer allocationExplainer;
    private final AdaptiveReallocator adaptiveReallocator;

    public AllocationEngine() {
        this(new StandardConstraintValidator(),
             new StandardOptimisationEvaluator(),
             new StandardAllocationExplainer(),
             new StandardAdaptiveReallocator());
    }

    public AllocationEngine(ConstraintValidator constraintValidator,
                            OptimisationEvaluator optimisationEvaluator,
                            AllocationExplainer allocationExplainer,
                            AdaptiveReallocator adaptiveReallocator) {
        this.constraintValidator = (constraintValidator != null) ? constraintValidator : new StandardConstraintValidator();
        this.optimisationEvaluator = (optimisationEvaluator != null) ? optimisationEvaluator : new StandardOptimisationEvaluator(this.constraintValidator);
        this.allocationExplainer = (allocationExplainer != null) ? allocationExplainer : new StandardAllocationExplainer();
        this.adaptiveReallocator = (adaptiveReallocator != null) ? adaptiveReallocator : new StandardAdaptiveReallocator();

        this.registeredStrategies = new EnumMap<>(StrategyType.class);
        registerDefaultStrategies();
    }

    private void registerDefaultStrategies() {
        registerStrategy(new PriorityAllocationStrategy());
        registerStrategy(new FairShareAllocationStrategy());
        registerStrategy(new ProportionalAllocationStrategy());
    }

    public void registerStrategy(AllocationStrategy strategy) {
        if (strategy != null && strategy.getType() != null) {
            registeredStrategies.put(strategy.getType(), strategy);
        }
    }

    public Optional<AllocationStrategy> getStrategy(StrategyType type) {
        return Optional.ofNullable(registeredStrategies.get(type));
    }

    public Collection<AllocationStrategy> getAvailableStrategies() {
        return Collections.unmodifiableCollection(registeredStrategies.values());
    }

    /**
     * Executes the requested allocation strategy, validates constraints, and attaches evaluation metrics.
     *
     * @param strategyType The strategy to use
     * @param inventory    Active resource inventory
     * @param requests     Competing demand requests
     * @param config       Constraint configuration
     * @return Fully evaluated and validated AllocationPlan
     */
    public AllocationPlan executeAllocation(StrategyType strategyType, ResourceInventory inventory,
                                           List<AllocationRequest> requests, ConstraintConfig config) {
        AllocationStrategy strategy = registeredStrategies.get(strategyType);
        if (strategy == null) {
            throw new IllegalArgumentException("Unsupported strategy type: " + strategyType);
        }

        ConstraintConfig activeConfig = (config != null) ? config : ConstraintConfig.defaultConfig();
        ResourceInventory inventorySnapshot = inventory.createSnapshot();

        // 1. Algorithmic allocation
        AllocationPlan plan = strategy.allocate(inventorySnapshot, requests, activeConfig);

        // 2. Validate hard constraints
        List<String> violations = constraintValidator.validateHardConstraints(inventorySnapshot, plan.getItems(), activeConfig);

        // 3. Compute evaluation metrics
        EvaluationMetrics metrics = optimisationEvaluator.evaluate(inventorySnapshot, requests, plan.getItems());
        metrics.setConstraintViolations(violations);
        plan.setMetrics(metrics);
        plan.setFeasible(violations.isEmpty());

        return plan;
    }

    /**
     * Overload of executeAllocation that uses the default constraint configuration.
     */
    public AllocationPlan executeAllocation(StrategyType strategyType, ResourceInventory inventory,
                                           List<AllocationRequest> requests) {
        return executeAllocation(strategyType, inventory, requests, ConstraintConfig.defaultConfig());
    }

    /**
     * Compares all registered allocation strategies under identical inputs.
     * Each strategy runs on its own thread; see ParallelStrategyRunner.
     */
    public StrategyComparison compareAllStrategies(ResourceInventory inventory, List<AllocationRequest> requests, ConstraintConfig config) {
        return compareAllStrategies(inventory, requests, config, AllocationListener.NONE);
    }

    /**
     * Overload that reports progress: {@code listener} is called back as each strategy finishes.
     */
    public StrategyComparison compareAllStrategies(ResourceInventory inventory, List<AllocationRequest> requests,
                                                   ConstraintConfig config, AllocationListener listener) {
        List<StrategyType> types = new ArrayList<>();
        for (StrategyType type : StrategyType.values()) {
            if (registeredStrategies.containsKey(type)) {
                types.add(type);
            }
        }

        ParallelStrategyRunner runner = new ParallelStrategyRunner(
                type -> executeAllocation(type, inventory, requests, config), listener);
        return optimisationEvaluator.compare(runner.runAll(types));
    }

    /**
     * Explains the decisions of an allocation plan.
     */
    public List<DecisionExplanation> explainPlan(AllocationPlan plan, ResourceInventory inventory, List<AllocationRequest> requests) {
        return allocationExplainer.explain(plan, inventory, requests);
    }

    /**
     * Reallocates adaptively and returns both the new plan and the impact diff.
     */
    public AdaptiveResult adaptiveReallocate(AllocationPlan previousPlan, StrategyType strategyType,
                                             ResourceInventory newInventory, List<AllocationRequest> newRequests,
                                             ConstraintConfig config) {
        AllocationPlan newPlan = executeAllocation(strategyType, newInventory, newRequests, config);
        ReallocationDiff diff = adaptiveReallocator.computeDiff(previousPlan, newPlan);
        return new AdaptiveResult(newPlan, diff);
    }

    public static class AdaptiveResult {
        private final AllocationPlan newPlan;
        private final ReallocationDiff diff;

        public AdaptiveResult(AllocationPlan newPlan, ReallocationDiff diff) {
            this.newPlan = newPlan;
            this.diff = diff;
        }

        public AllocationPlan getNewPlan() { return newPlan; }
        public ReallocationDiff getDiff() { return diff; }
    }

    public ConstraintValidator getConstraintValidator() {
        return constraintValidator;
    }

    public OptimisationEvaluator getOptimisationEvaluator() {
        return optimisationEvaluator;
    }

    public AllocationExplainer getAllocationExplainer() {
        return allocationExplainer;
    }

    public AdaptiveReallocator getAdaptiveReallocator() {
        return adaptiveReallocator;
    }
}
