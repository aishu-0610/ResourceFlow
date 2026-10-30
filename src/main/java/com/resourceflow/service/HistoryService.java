package com.resourceflow.service;

import com.resourceflow.engine.AdaptiveReallocator;
import com.resourceflow.model.AllocationPlan;
import com.resourceflow.model.AllocationRun;
import com.resourceflow.model.ReallocationDiff;
import com.resourceflow.repository.AllocationRunRepository;

import java.util.List;
import java.util.Optional;

/**
 * Service managing historical allocation runs and comparative post-mortems.
 */
public class HistoryService {

    private final AllocationRunRepository runRepository;
    private final AdaptiveReallocator adaptiveReallocator;

    public HistoryService(AllocationRunRepository runRepository, AdaptiveReallocator adaptiveReallocator) {
        this.runRepository = runRepository;
        this.adaptiveReallocator = adaptiveReallocator;
    }

    public List<AllocationRun> getAllRuns() {
        return runRepository.findAllRuns();
    }

    public Optional<AllocationRun> getRunById(String runId) {
        return runRepository.findById(runId);
    }

    public Optional<AllocationRun> getLatestRun() {
        return runRepository.findLatestRun();
    }

    public void deleteRun(String runId) {
        runRepository.deleteRun(runId);
    }

    /**
     * Compares two historical runs and computes an itemized delta.
     */
    public ReallocationDiff compareRuns(String runId1, String runId2) {
        AllocationRun run1 = runRepository.findById(runId1)
                .orElseThrow(() -> new IllegalArgumentException("Run not found: " + runId1));
        AllocationRun run2 = runRepository.findById(runId2)
                .orElseThrow(() -> new IllegalArgumentException("Run not found: " + runId2));

        AllocationPlan plan1 = new AllocationPlan(run1.getStrategyUsed(), run1.getStrategyUsed().getDisplayName(), run1.getAllocations());
        plan1.setPlanId(run1.getRunId());
        plan1.setTimestamp(run1.getTimestamp());
        plan1.setMetrics(run1.getMetrics());

        AllocationPlan plan2 = new AllocationPlan(run2.getStrategyUsed(), run2.getStrategyUsed().getDisplayName(), run2.getAllocations());
        plan2.setPlanId(run2.getRunId());
        plan2.setTimestamp(run2.getTimestamp());
        plan2.setMetrics(run2.getMetrics());

        return adaptiveReallocator.computeDiff(plan1, plan2);
    }
}
