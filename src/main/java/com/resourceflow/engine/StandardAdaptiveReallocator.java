package com.resourceflow.engine;

import com.resourceflow.model.AllocatedItem;
import com.resourceflow.model.AllocationPlan;
import com.resourceflow.model.ReallocationDiff;

import java.util.*;

/**
 * Standard implementation of AdaptiveReallocator.
 * Computes granular impact analysis between two allocation plans.
 */
public class StandardAdaptiveReallocator implements AdaptiveReallocator {

    private static final double EPSILON = 1e-6;

    @Override
    public ReallocationDiff computeDiff(AllocationPlan previousPlan, AllocationPlan newPlan) {
        ReallocationDiff diff = new ReallocationDiff();
        if (previousPlan != null) {
            diff.setPreviousRunId(previousPlan.getPlanId());
        }
        if (newPlan != null) {
            diff.setNewRunId(newPlan.getPlanId());
        }

        if (previousPlan == null || newPlan == null) {
            diff.setSummaryExplanation("Cannot compare null allocation plans.");
            return diff;
        }

        Map<String, AllocatedItem> prevMap = new HashMap<>();
        for (AllocatedItem item : previousPlan.getItems()) {
            prevMap.put(item.getRequestId(), item);
        }

        Map<String, AllocatedItem> newMap = new HashMap<>();
        for (AllocatedItem item : newPlan.getItems()) {
            newMap.put(item.getRequestId(), item);
        }

        Set<String> allRequestIds = new LinkedHashSet<>();
        allRequestIds.addAll(prevMap.keySet());
        allRequestIds.addAll(newMap.keySet());

        double totalReassigned = 0.0;
        int improvedCount = 0;
        int degradedCount = 0;
        int droppedCount = 0;

        for (String reqId : allRequestIds) {
            AllocatedItem prevItem = prevMap.get(reqId);
            AllocatedItem newItem = newMap.get(reqId);

            double prevAlloc = (prevItem != null) ? prevItem.getAllocatedQuantity() : 0.0;
            double newAlloc = (newItem != null) ? newItem.getAllocatedQuantity() : 0.0;
            double demanded = (newItem != null) ? newItem.getRequestedQuantity() :
                    (prevItem != null ? prevItem.getRequestedQuantity() : 0.0);

            String requester = (newItem != null) ? newItem.getRequesterName() :
                    (prevItem != null ? prevItem.getRequesterName() : "Unknown");
            String resourceId = (newItem != null) ? newItem.getResourceId() :
                    (prevItem != null ? prevItem.getResourceId() : "Unknown");

            double delta = newAlloc - prevAlloc;

            if (delta > EPSILON) {
                totalReassigned += delta;
                improvedCount++;
                String reason = String.format(Locale.US, "Allocation increased by +%.2f units (from %.2f to %.2f).",
                        delta, prevAlloc, newAlloc);
                diff.getImprovedRequests().add(new ReallocationDiff.DiffItem(
                        reqId, requester, resourceId, prevAlloc, newAlloc, demanded, reason));
            } else if (delta < -EPSILON) {
                if (newAlloc <= EPSILON) {
                    droppedCount++;
                    String reason = String.format(Locale.US, "Allocation dropped to 0 (was %.2f units).", prevAlloc);
                    diff.getDroppedRequests().add(new ReallocationDiff.DiffItem(
                        reqId, requester, resourceId, prevAlloc, newAlloc, demanded, reason));
                } else {
                    degradedCount++;
                    String reason = String.format(Locale.US, "Allocation decreased by %.2f units (from %.2f to %.2f).",
                            delta, prevAlloc, newAlloc);
                    diff.getDegradedRequests().add(new ReallocationDiff.DiffItem(
                        reqId, requester, resourceId, prevAlloc, newAlloc, demanded, reason));
                }
            } else {
                String reason = String.format(Locale.US, "Allocation unchanged at %.2f units.", newAlloc);
                diff.getUnchangedRequests().add(new ReallocationDiff.DiffItem(
                        reqId, requester, resourceId, prevAlloc, newAlloc, demanded, reason));
            }
        }

        diff.setCapacityReassigned(totalReassigned);
        diff.setSummaryExplanation(String.format(Locale.US,
                "Adaptive Reallocation Summary: %d improved, %d degraded, %d dropped. Total capacity reassigned: %.2f units.",
                improvedCount, degradedCount, droppedCount, totalReassigned));

        return diff;
    }
}
