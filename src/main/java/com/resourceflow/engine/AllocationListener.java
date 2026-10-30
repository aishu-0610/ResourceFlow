package com.resourceflow.engine;

import com.resourceflow.model.AllocationPlan;

/**
 * Callback interface: the engine "calls back" whoever registered this listener each time
 * one strategy finishes during a parallel comparison.
 *
 * It has exactly one abstract method, so it is a functional interface and callers can
 * pass a lambda, e.g. {@code (plan, ms, thread) -> System.out.println(plan.getStrategyName())}.
 *
 * Note: the callback runs on the worker thread that computed the plan, not on the caller's thread.
 */
@FunctionalInterface
public interface AllocationListener {

    void onStrategyFinished(AllocationPlan plan, long elapsedMillis, String threadName);

    /** A listener that does nothing, used when the caller does not need progress updates. */
    AllocationListener NONE = (plan, elapsedMillis, threadName) -> { };
}
