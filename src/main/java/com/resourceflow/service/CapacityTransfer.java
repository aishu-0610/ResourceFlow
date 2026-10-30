package com.resourceflow.service;

import com.resourceflow.exception.ResourceValidationException;
import com.resourceflow.model.Resource;

import java.util.Locale;

/**
 * Moves unused capacity from one resource to another (e.g. 20 GPU-hours from the H100
 * cluster to the A100 cluster) in a What-If sandbox.
 *
 * Deadlock and how it is avoided:
 * A transfer must lock BOTH resources so no other thread sees half a transfer. If thread 1
 * locks A then waits for B, while thread 2 locks B then waits for A, both wait forever:
 * a deadlock. The fix used here is lock ordering: always lock the resource with the
 * smaller ID first. Then both threads try to lock A first, one of them waits, and the
 * cycle can never form. CapacityTransferTest runs opposing transfers on two threads to prove it.
 */
public final class CapacityTransfer {

    private CapacityTransfer() {
    }

    public static void transfer(Resource from, Resource to, double amount) {
        if (from == null || to == null) {
            throw new IllegalArgumentException("Both resources are required");
        }
        if (from.getResourceId().equals(to.getResourceId())) {
            throw new ResourceValidationException("Choose two different resources");
        }
        if (amount <= 0) {
            throw new ResourceValidationException("Transfer amount must be greater than 0");
        }
        if (!from.getUnit().equalsIgnoreCase(to.getUnit())) {
            throw new ResourceValidationException(String.format(Locale.US,
                    "Cannot move %s into a resource measured in %s", from.getUnit(), to.getUnit()));
        }

        // Lock ordering: compare IDs so every thread locks the same resource first
        Resource first = (from.getResourceId().compareTo(to.getResourceId()) < 0) ? from : to;
        Resource second = (first == from) ? to : from;

        synchronized (first) {
            synchronized (second) {
                if (amount > from.getAvailableCapacity() + 1e-9) {
                    throw new ResourceValidationException(String.format(Locale.US,
                            "'%s' has only %.1f %s unused; cannot move %.1f",
                            from.getName(), from.getAvailableCapacity(), from.getUnit(), amount));
                }
                // Shrink the source: lower available first so it never exceeds total
                from.setAvailableCapacity(Math.max(0.0, from.getAvailableCapacity() - amount));
                from.setTotalCapacity(Math.max(0.0, from.getTotalCapacity() - amount));
                // Grow the target: raise total first for the same reason
                to.setTotalCapacity(to.getTotalCapacity() + amount);
                to.setAvailableCapacity(to.getAvailableCapacity() + amount);
            }
        }
    }
}
