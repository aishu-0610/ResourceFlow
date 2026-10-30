package com.resourceflow.engine;

import com.resourceflow.exception.ResourceFlowException;
import com.resourceflow.model.AllocationPlan;
import com.resourceflow.model.StrategyType;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * Runs several allocation strategies at the same time, one Thread per strategy.
 *
 * Syllabus concepts shown here:
 *  - Creating threads from a Runnable (StrategyWorker) and starting them with Thread.start()
 *  - Thread names and priorities
 *  - join(): the calling thread waits until every worker has finished
 *  - synchronized methods protecting a shared list written by several threads
 *  - Chained exceptions: a worker failure is re-thrown to the caller with the original cause attached
 */
public class ParallelStrategyRunner {

    private final Function<StrategyType, AllocationPlan> planner;
    private final AllocationListener listener;

    private final List<AllocationPlan> finishedPlans = new ArrayList<>();
    private Throwable firstFailure;

    /**
     * @param planner  computes one plan for a strategy type (the engine passes executeAllocation as a lambda)
     * @param listener callback notified as each strategy finishes
     */
    public ParallelStrategyRunner(Function<StrategyType, AllocationPlan> planner, AllocationListener listener) {
        this.planner = planner;
        this.listener = (listener != null) ? listener : AllocationListener.NONE;
    }

    /**
     * Starts one worker thread per strategy, waits for all of them and returns their plans
     * in the same order as {@code types}.
     */
    public List<AllocationPlan> runAll(List<StrategyType> types) {
        List<Thread> threads = new ArrayList<>();

        for (StrategyType type : types) {
            Thread worker = new Thread(new StrategyWorker(type), "Strategy-" + type.name());
            worker.setPriority(Thread.NORM_PRIORITY + 1); // slightly above normal: the user is waiting for this
            threads.add(worker);
            worker.start();
        }

        for (Thread worker : threads) {
            try {
                worker.join();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt(); // restore the interrupt flag for the caller
                throw new ResourceFlowException("Strategy comparison was interrupted", e);
            }
        }

        if (firstFailure != null) {
            throw new ResourceFlowException("A strategy failed during parallel comparison: " + firstFailure.getMessage(), firstFailure);
        }

        // Threads finish in any order; return plans in a stable order for the UI
        List<AllocationPlan> ordered = new ArrayList<>();
        for (StrategyType type : types) {
            for (AllocationPlan plan : finishedPlans) {
                if (plan.getStrategyType() == type) {
                    ordered.add(plan);
                }
            }
        }
        return ordered;
    }

    /** Only one thread at a time may add to the shared list. */
    private synchronized void recordSuccess(AllocationPlan plan) {
        finishedPlans.add(plan);
    }

    private synchronized void recordFailure(Throwable error) {
        if (firstFailure == null) {
            firstFailure = error;
        }
    }

    /**
     * The task each thread executes. An inner (non-static) class, so it can call the
     * runner's synchronized methods directly.
     */
    private class StrategyWorker implements Runnable {
        private final StrategyType type;

        StrategyWorker(StrategyType type) {
            this.type = type;
        }

        @Override
        public void run() {
            long start = System.nanoTime();
            try {
                AllocationPlan plan = planner.apply(type);
                recordSuccess(plan);
                long elapsedMillis = (System.nanoTime() - start) / 1_000_000;
                listener.onStrategyFinished(plan, elapsedMillis, Thread.currentThread().getName());
            } catch (RuntimeException e) {
                recordFailure(e);
            }
        }
    }
}
