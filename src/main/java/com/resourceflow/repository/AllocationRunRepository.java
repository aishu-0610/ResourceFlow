package com.resourceflow.repository;

import com.resourceflow.model.AllocationRun;

import java.util.List;
import java.util.Optional;

/**
 * Data access operations for historical Allocation Runs and their metric snapshots.
 */
public interface AllocationRunRepository {
    void saveRun(AllocationRun run);
    Optional<AllocationRun> findById(String runId);
    Optional<AllocationRun> findLatestRun();
    List<AllocationRun> findAllRuns();
    void deleteRun(String runId);
    int countRuns();
}
