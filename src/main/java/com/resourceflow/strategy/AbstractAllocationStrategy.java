package com.resourceflow.strategy;

import com.resourceflow.model.AllocatedItem;
import com.resourceflow.model.AllocationPlan;
import com.resourceflow.model.AllocationRequest;
import com.resourceflow.model.ConstraintConfig;
import com.resourceflow.model.RequestStatus;
import com.resourceflow.model.ResourceInventory;
import com.resourceflow.model.StrategyType;

import java.util.ArrayList;
import java.util.List;

/**
 * Base class for every allocation algorithm (Template Method pattern).
 *
 * allocate() is final: it performs the steps every strategy needs (default config,
 * empty-input check, wrapping the result in a plan) and delegates the algorithm itself
 * to computeItems(), which each subclass overrides. Because the engine calls allocate()
 * through an AllocationStrategy reference, the JVM picks the right computeItems()
 * at runtime (dynamic method dispatch).
 *
 * Name, description and complexity are not hard-coded: they are read from the
 * subclass's @StrategyInfo annotation using reflection.
 */
public abstract class AbstractAllocationStrategy implements AllocationStrategy {

    private final StrategyType type;
    private final StrategyInfo info;

    protected AbstractAllocationStrategy(StrategyType type) {
        this.type = type;
        this.info = getClass().getAnnotation(StrategyInfo.class);
        if (this.info == null) {
            throw new IllegalStateException(getClass().getSimpleName() + " must be annotated with @StrategyInfo");
        }
    }

    @Override
    public final StrategyType getType() {
        return type;
    }

    @Override
    public String getName() {
        return info.name();
    }

    @Override
    public String getDescription() {
        return info.description();
    }

    @Override
    public String getComplexity() {
        return info.complexity();
    }

    @Override
    public final AllocationPlan allocate(ResourceInventory inventory, List<AllocationRequest> requests, ConstraintConfig config) {
        ConstraintConfig activeConfig = (config != null) ? config : ConstraintConfig.defaultConfig();
        List<AllocatedItem> items = (requests == null || requests.isEmpty())
                ? new ArrayList<>()
                : computeItems(inventory, requests, activeConfig);
        return new AllocationPlan(type, getName(), items);
    }

    /**
     * The algorithm-specific step. Called only with a non-empty request list and a non-null config.
     */
    protected abstract List<AllocatedItem> computeItems(ResourceInventory inventory,
                                                        List<AllocationRequest> requests,
                                                        ConstraintConfig config);

    // ---- Helpers shared by all subclasses (code reuse through inheritance) ----

    protected static boolean isIneligible(AllocationRequest req) {
        return req.getStatus() == RequestStatus.CANCELLED || req.getStatus() == RequestStatus.REJECTED;
    }

    /** Builds an item for a request that received nothing, with the reason shown to the user. */
    protected static AllocatedItem unallocated(AllocationRequest req, String reason) {
        return allocated(req, 0.0, reason);
    }

    /** Builds an item for a request that received {@code quantity} units. */
    protected static AllocatedItem allocated(AllocationRequest req, double quantity, String explanation) {
        return new AllocatedItem(null, req.getRequestId(), req.getRequesterName(), req.getResourceId(),
                req.getRequestedQuantity(), quantity, explanation);
    }
}
