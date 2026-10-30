package com.resourceflow.model;

import java.time.LocalDateTime;
import java.util.*;

/**
 * Historical record representing a completed allocation execution.
 */
public class AllocationRun {
    private String runId;
    private LocalDateTime timestamp;
    private StrategyType strategyUsed;
    private String scenarioName;
    private boolean baseline;
    private String summaryNotes;
    private List<AllocatedItem> allocations;
    private EvaluationMetrics metrics;

    public AllocationRun() {
        this.runId = UUID.randomUUID().toString();
        this.timestamp = LocalDateTime.now();
        this.baseline = true;
        this.allocations = new ArrayList<>();
        this.metrics = new EvaluationMetrics();
        this.summaryNotes = "";
    }

    public AllocationRun(String runId, LocalDateTime timestamp, StrategyType strategyUsed,
                         String scenarioName, boolean baseline, String summaryNotes,
                         List<AllocatedItem> allocations, EvaluationMetrics metrics) {
        this.runId = runId != null ? runId : UUID.randomUUID().toString();
        this.timestamp = timestamp != null ? timestamp : LocalDateTime.now();
        this.strategyUsed = strategyUsed;
        this.scenarioName = scenarioName;
        this.baseline = baseline;
        this.summaryNotes = summaryNotes != null ? summaryNotes : "";
        this.allocations = allocations != null ? new ArrayList<>(allocations) : new ArrayList<>();
        this.metrics = metrics != null ? metrics : new EvaluationMetrics();
    }

    public static AllocationRun fromPlan(AllocationPlan plan, String scenarioName, boolean isBaseline) {
        AllocationRun run = new AllocationRun();
        run.setRunId(plan.getPlanId());
        run.setTimestamp(plan.getTimestamp());
        run.setStrategyUsed(plan.getStrategyType());
        run.setScenarioName(scenarioName);
        run.setBaseline(isBaseline);
        run.setSummaryNotes(plan.getNotes());
        run.setAllocations(plan.getItems());
        run.setMetrics(plan.getMetrics());
        return run;
    }

    public String getRunId() {
        return runId;
    }

    public void setRunId(String runId) {
        this.runId = runId;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public StrategyType getStrategyUsed() {
        return strategyUsed;
    }

    public void setStrategyUsed(StrategyType strategyUsed) {
        this.strategyUsed = strategyUsed;
    }

    public String getScenarioName() {
        return scenarioName;
    }

    public void setScenarioName(String scenarioName) {
        this.scenarioName = scenarioName;
    }

    public boolean isBaseline() {
        return baseline;
    }

    public void setBaseline(boolean baseline) {
        this.baseline = baseline;
    }

    public String getSummaryNotes() {
        return summaryNotes;
    }

    public void setSummaryNotes(String summaryNotes) {
        this.summaryNotes = summaryNotes != null ? summaryNotes : "";
    }

    public List<AllocatedItem> getAllocations() {
        return allocations;
    }

    public void setAllocations(List<AllocatedItem> allocations) {
        this.allocations = allocations != null ? new ArrayList<>(allocations) : new ArrayList<>();
    }

    public EvaluationMetrics getMetrics() {
        return metrics;
    }

    public void setMetrics(EvaluationMetrics metrics) {
        this.metrics = metrics != null ? metrics : new EvaluationMetrics();
    }
}
