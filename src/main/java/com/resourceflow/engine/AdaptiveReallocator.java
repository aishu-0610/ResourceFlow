package com.resourceflow.engine;

import com.resourceflow.model.AllocationPlan;
import com.resourceflow.model.ReallocationDiff;

/**
 * Interface for computing deltas and reallocation impacts when parameters change.
 */
public interface AdaptiveReallocator {

    /**
     * Compares an existing allocation plan with a freshly recomputed plan
     * to identify improved, degraded, dropped, and unchanged requests.
     *
     * @param previousPlan The prior plan
     * @param newPlan      The newly generated plan
     * @return Detailed diff with impact breakdown and reasons
     */
    ReallocationDiff computeDiff(AllocationPlan previousPlan, AllocationPlan newPlan);
}
