package com.resourceflow.service;

import com.resourceflow.engine.AllocationEngine;
import com.resourceflow.exception.ScenarioFileException;
import com.resourceflow.io.ScenarioFileStore;
import com.resourceflow.model.*;
import com.resourceflow.repository.AllocationRunRepository;
import com.resourceflow.repository.RequestRepository;
import com.resourceflow.repository.ResourceRepository;
import com.resourceflow.repository.ScenarioRepository;
import com.resourceflow.util.AuditLog;

import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service providing isolated sandbox execution for What-If scenario analysis.
 * Guarantees that hypothetical simulations never overwrite live baseline data
 * unless explicitly committed by the user.
 */
public class WhatIfService {

    private final AllocationEngine engine;
    private final ResourceRepository resourceRepository;
    private final RequestRepository requestRepository;
    private final AllocationRunRepository runRepository;
    private final ScenarioRepository scenarioRepository;

    public WhatIfService(AllocationEngine engine,
                         ResourceRepository resourceRepository,
                         RequestRepository requestRepository,
                         AllocationRunRepository runRepository,
                         ScenarioRepository scenarioRepository) {
        this.engine = engine;
        this.resourceRepository = resourceRepository;
        this.requestRepository = requestRepository;
        this.runRepository = runRepository;
        this.scenarioRepository = scenarioRepository;
    }

    /**
     * Initializes a new sandbox scenario by cloning live resources and requests.
     */
    public Scenario createSandboxScenario(String name, String description, StrategyType strategyType) {
        // The sandbox starts from full capacity, like a fresh allocation run
        List<Resource> clonedResources = new ArrayList<>(
                ResourceInventory.forReplanning(resourceRepository.findAll()).getAllResources());

        List<AllocationRequest> clonedRequests = requestRepository.findAll().stream()
                .map(AllocationRequest::copy)
                .collect(Collectors.toList());

        Optional<AllocationRun> latestRun = runRepository.findLatestRun();
        String baselineId = latestRun.map(AllocationRun::getRunId).orElse(null);

        return new Scenario(
                UUID.randomUUID().toString(),
                name,
                description,
                baselineId,
                clonedResources,
                clonedRequests,
                strategyType != null ? strategyType : StrategyType.PRIORITY_BASED,
                ConstraintConfig.defaultConfig()
        );
    }

    /**
     * Simulates allocation in the sandbox without altering production tables.
     */
    public WhatIfSimulationResult simulateScenario(Scenario scenario) {
        ResourceInventory sandboxInventory = new ResourceInventory(scenario.getModifiedResources());
        List<AllocationRequest> eligibleRequests = scenario.getModifiedRequests().stream()
                .filter(r -> r.getStatus() != RequestStatus.CANCELLED && r.getStatus() != RequestStatus.REJECTED)
                .collect(Collectors.toList());

        AllocationPlan hypotheticalPlan = engine.executeAllocation(
                scenario.getStrategyType(),
                sandboxInventory,
                eligibleRequests,
                scenario.getConfig()
        );

        // Fetch baseline run if present for side-by-side comparison
        AllocationPlan baselinePlan = null;
        if (scenario.getBaselineRunId() != null) {
            Optional<AllocationRun> baselineOpt = runRepository.findById(scenario.getBaselineRunId());
            if (baselineOpt.isPresent()) {
                AllocationRun baseRun = baselineOpt.get();
                baselinePlan = new AllocationPlan(baseRun.getStrategyUsed(), baseRun.getStrategyUsed().getDisplayName(), baseRun.getAllocations());
                baselinePlan.setPlanId(baseRun.getRunId());
                baselinePlan.setMetrics(baseRun.getMetrics());
            }
        }

        ReallocationDiff deltaAgainstBaseline = null;
        if (baselinePlan != null) {
            deltaAgainstBaseline = engine.getAdaptiveReallocator().computeDiff(baselinePlan, hypotheticalPlan);
        }

        AuditLog.record("What-If '%s' simulated with %s: utilisation %.1f%%, fairness %.3f",
                scenario.getName(), scenario.getStrategyType().getDisplayName(),
                hypotheticalPlan.getMetrics().getAggregateUtilisation(),
                hypotheticalPlan.getMetrics().getJainsFairnessIndex());
        return new WhatIfSimulationResult(scenario, hypotheticalPlan, baselinePlan, deltaAgainstBaseline);
    }

    /**
     * Moves unused capacity between two sandbox resources (deadlock-safe, see CapacityTransfer).
     */
    public void transferCapacity(Resource from, Resource to, double amount) {
        CapacityTransfer.transfer(from, to, amount);
        AuditLog.record("What-If transfer: %.1f %s moved from %s to %s",
                amount, from.getUnit(), from.getResourceId(), to.getResourceId());
    }

    /**
     * Writes the full scenario (resources, requests, strategy, constraints) to a .rflow file.
     */
    public void exportScenario(Scenario scenario, Path file) throws ScenarioFileException {
        ScenarioFileStore.save(scenario, file);
        AuditLog.record("Scenario '%s' exported to %s", scenario.getName(), file.getFileName());
    }

    /**
     * Reads a scenario from a .rflow file so it can be simulated again.
     */
    public Scenario importScenario(Path file) throws ScenarioFileException {
        Scenario scenario = ScenarioFileStore.load(file);
        AuditLog.record("Scenario '%s' imported from %s", scenario.getName(), file.getFileName());
        return scenario;
    }

    /**
     * Saves the scenario metadata in the SQLite database.
     */
    public void saveScenario(Scenario scenario) {
        scenarioRepository.save(scenario);
    }

    public List<Scenario> getSavedScenarios() {
        return scenarioRepository.findAll();
    }

    public static class WhatIfSimulationResult {
        private final Scenario scenario;
        private final AllocationPlan hypotheticalPlan;
        private final AllocationPlan baselinePlan;
        private final ReallocationDiff diff;

        public WhatIfSimulationResult(Scenario scenario, AllocationPlan hypotheticalPlan,
                                      AllocationPlan baselinePlan, ReallocationDiff diff) {
            this.scenario = scenario;
            this.hypotheticalPlan = hypotheticalPlan;
            this.baselinePlan = baselinePlan;
            this.diff = diff;
        }

        public Scenario getScenario() { return scenario; }
        public AllocationPlan getHypotheticalPlan() { return hypotheticalPlan; }
        public AllocationPlan getBaselinePlan() { return baselinePlan; }
        public ReallocationDiff getDiff() { return diff; }
    }
}
