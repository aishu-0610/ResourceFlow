package com.resourceflow.service;

import com.resourceflow.engine.AllocationEngine;
import com.resourceflow.model.*;
import com.resourceflow.repository.AllocationRunRepository;
import com.resourceflow.repository.RequestRepository;
import com.resourceflow.repository.ResourceRepository;
import com.resourceflow.util.AuditLog;

import java.util.*;

/**
 * High-level coordinator executing allocation runs, persisting history,
 * and performing adaptive recalculations when operational data changes.
 */
public class AllocationService {

    private final AllocationEngine engine;
    private final ResourceRepository resourceRepository;
    private final RequestRepository requestRepository;
    private final AllocationRunRepository runRepository;

    public AllocationService(AllocationEngine engine,
                             ResourceRepository resourceRepository,
                             RequestRepository requestRepository,
                             AllocationRunRepository runRepository) {
        this.engine = engine;
        this.resourceRepository = resourceRepository;
        this.requestRepository = requestRepository;
        this.runRepository = runRepository;
    }

    /**
     * Executes an allocation run across all active resources and eligible requests,
     * persists the historical run, and updates request statuses and remaining capacities.
     */
    public AllocationRun runAllocation(StrategyType strategyType, ConstraintConfig config, String scenarioName) {
        List<Resource> resources = resourceRepository.findAll();
        List<AllocationRequest> requests = requestRepository.findEligibleRequests();

        ResourceInventory inventory = ResourceInventory.forReplanning(resources);
        AllocationPlan plan = engine.executeAllocation(strategyType, inventory, requests, config);

        // Convert plan to historical run and persist
        AllocationRun run = AllocationRun.fromPlan(plan, scenarioName, true);
        runRepository.saveRun(run);

        // Update database operational state: request statuses and resource available capacities
        updateOperationalState(plan, resources);

        AuditLog.record("Allocation run '%s' (%s): utilisation %.1f%%, fulfilment %.1f%%, fairness %.3f",
                scenarioName, strategyType.getDisplayName(), plan.getMetrics().getAggregateUtilisation(),
                plan.getMetrics().getOverallFulfilment(), plan.getMetrics().getJainsFairnessIndex());
        return run;
    }

    /**
     * Reallocates adaptively against the latest baseline run and computes an impact diff.
     */
    public AllocationEngine.AdaptiveResult reallocateAdaptively(StrategyType strategyType, ConstraintConfig config) {
        Optional<AllocationRun> latestRunOpt = runRepository.findLatestRun();
        AllocationPlan previousPlan = null;

        if (latestRunOpt.isPresent()) {
            AllocationRun latest = latestRunOpt.get();
            previousPlan = new AllocationPlan(latest.getStrategyUsed(), latest.getStrategyUsed().getDisplayName(), latest.getAllocations());
            previousPlan.setPlanId(latest.getRunId());
            previousPlan.setTimestamp(latest.getTimestamp());
            previousPlan.setMetrics(latest.getMetrics());
        }

        List<Resource> resources = resourceRepository.findAll();
        List<AllocationRequest> requests = requestRepository.findEligibleRequests();
        ResourceInventory inventory = ResourceInventory.forReplanning(resources);

        AllocationEngine.AdaptiveResult result = engine.adaptiveReallocate(previousPlan, strategyType, inventory, requests, config);

        // Persist the new run
        AllocationRun newRun = AllocationRun.fromPlan(result.getNewPlan(), "Adaptive Reallocation", true);
        newRun.setSummaryNotes(result.getDiff().getSummaryExplanation());
        runRepository.saveRun(newRun);

        updateOperationalState(result.getNewPlan(), resources);

        AuditLog.record("Adaptive reallocation (%s): %s", strategyType.getDisplayName(),
                result.getDiff().getSummaryExplanation());
        return result;
    }

    private void updateOperationalState(AllocationPlan plan, List<Resource> resources) {
        Map<String, Double> allocatedByResource = new HashMap<>();

        for (AllocatedItem item : plan.getItems()) {
            allocatedByResource.merge(item.getResourceId(), item.getAllocatedQuantity(), Double::sum);

            // Update request status
            Optional<AllocationRequest> reqOpt = requestRepository.findById(item.getRequestId());
            if (reqOpt.isPresent()) {
                AllocationRequest req = reqOpt.get();
                if (item.getStatus() == AllocationStatus.FULLY_SATISFIED) {
                    req.setStatus(RequestStatus.ALLOCATED);
                } else if (item.getStatus() == AllocationStatus.PARTIALLY_SATISFIED) {
                    req.setStatus(RequestStatus.PARTIALLY_ALLOCATED);
                } else {
                    req.setStatus(RequestStatus.REJECTED);
                }
                requestRepository.update(req);
            }
        }

        // Update available capacity on resources
        for (Resource res : resources) {
            double totalAllocated = allocatedByResource.getOrDefault(res.getResourceId(), 0.0);
            double newAvail = Math.max(0.0, res.getTotalCapacity() - totalAllocated);
            res.setAvailableCapacity(newAvail);
            resourceRepository.update(res);
        }
    }

    public List<DecisionExplanation> explainPlan(AllocationPlan plan) {
        List<Resource> resources = resourceRepository.findAll();
        List<AllocationRequest> requests = requestRepository.findAll();
        return engine.explainPlan(plan, new ResourceInventory(resources), requests);
    }

    public AllocationEngine getEngine() {
        return engine;
    }
}
