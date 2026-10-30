package com.resourceflow.repository;

import com.resourceflow.database.DatabaseManager;
import com.resourceflow.exception.DatabasePersistenceException;
import com.resourceflow.model.*;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.*;

/**
 * SQLite JDBC implementation of AllocationRunRepository with atomic multi-table transactions.
 */
public class SqliteAllocationRunRepository implements AllocationRunRepository {

    private final DatabaseManager dbManager;

    public SqliteAllocationRunRepository(DatabaseManager dbManager) {
        this.dbManager = dbManager;
    }

    @Override
    public void saveRun(AllocationRun run) {
        String insertRunSql = """
            INSERT INTO allocation_runs (run_id, run_timestamp, strategy_used, scenario_name, is_baseline, summary_notes)
            VALUES (?, ?, ?, ?, ?, ?);
        """;

        String insertItemSql = """
            INSERT INTO allocations (allocation_id, run_id, request_id, requester_name, resource_id, requested_quantity, allocated_quantity, fulfilment_ratio, status, explanation)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?);
        """;

        String insertMetricsSql = """
            INSERT INTO allocation_metrics (run_id, aggregate_utilisation, overall_fulfilment, average_fulfilment, jains_fairness_index, fully_satisfied_count, partially_satisfied_count, unsatisfied_count, unallocated_capacity, constraint_violations)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?);
        """;

        Connection conn = null;
        try {
            conn = dbManager.getConnection();
            conn.setAutoCommit(false); // Begin transaction

            // 1. Insert allocation run header
            try (PreparedStatement psRun = conn.prepareStatement(insertRunSql)) {
                psRun.setString(1, run.getRunId());
                psRun.setString(2, (run.getTimestamp() != null ? run.getTimestamp() : LocalDateTime.now()).toString());
                psRun.setString(3, run.getStrategyUsed() != null ? run.getStrategyUsed().name() : StrategyType.PRIORITY_BASED.name());
                psRun.setString(4, run.getScenarioName());
                psRun.setInt(5, run.isBaseline() ? 1 : 0);
                psRun.setString(6, run.getSummaryNotes());
                psRun.executeUpdate();
            }

            // 2. Insert individual allocation items in batch
            if (run.getAllocations() != null && !run.getAllocations().isEmpty()) {
                try (PreparedStatement psItem = conn.prepareStatement(insertItemSql)) {
                    for (AllocatedItem item : run.getAllocations()) {
                        psItem.setString(1, item.getAllocationId() != null ? item.getAllocationId() : UUID.randomUUID().toString());
                        psItem.setString(2, run.getRunId());
                        psItem.setString(3, item.getRequestId());
                        psItem.setString(4, item.getRequesterName() != null ? item.getRequesterName() : "Unknown");
                        psItem.setString(5, item.getResourceId());
                        psItem.setDouble(6, item.getRequestedQuantity());
                        psItem.setDouble(7, item.getAllocatedQuantity());
                        psItem.setDouble(8, item.getFulfilmentRatio());
                        psItem.setString(9, item.getStatus() != null ? item.getStatus().name() : AllocationStatus.UNSATISFIED.name());
                        psItem.setString(10, item.getExplanation());
                        psItem.addBatch();
                    }
                    psItem.executeBatch();
                }
            }

            // 3. Insert evaluation metrics
            EvaluationMetrics metrics = run.getMetrics() != null ? run.getMetrics() : new EvaluationMetrics();
            try (PreparedStatement psMetrics = conn.prepareStatement(insertMetricsSql)) {
                psMetrics.setString(1, run.getRunId());
                psMetrics.setDouble(2, metrics.getAggregateUtilisation());
                psMetrics.setDouble(3, metrics.getOverallFulfilment());
                psMetrics.setDouble(4, metrics.getAverageFulfilment());
                psMetrics.setDouble(5, metrics.getJainsFairnessIndex());
                psMetrics.setInt(6, metrics.getFullySatisfiedCount());
                psMetrics.setInt(7, metrics.getPartiallySatisfiedCount());
                psMetrics.setInt(8, metrics.getUnsatisfiedCount());
                psMetrics.setDouble(9, metrics.getUnallocatedCapacity());
                psMetrics.setString(10, String.join("; ", metrics.getConstraintViolations()));
                psMetrics.executeUpdate();
            }

            conn.commit(); // Commit transaction
        } catch (SQLException e) {
            if (conn != null) {
                try {
                    conn.rollback(); // Roll back transaction on error
                } catch (SQLException rollbackEx) {
                    // Suppress or chain rollback error
                }
            }
            throw new DatabasePersistenceException("Failed to save allocation run transaction: " + e.getMessage(), e);
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                    conn.close();
                } catch (SQLException ignored) {}
            }
        }
    }

    @Override
    public Optional<AllocationRun> findById(String runId) {
        String sqlRun = "SELECT * FROM allocation_runs WHERE run_id = ?;";
        try (Connection conn = dbManager.getConnection(); PreparedStatement ps = conn.prepareStatement(sqlRun)) {
            ps.setString(1, runId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    AllocationRun run = mapResultSetToRun(rs);
                    run.setAllocations(loadAllocationsForRun(conn, runId));
                    run.setMetrics(loadMetricsForRun(conn, runId));
                    return Optional.of(run);
                }
            }
        } catch (SQLException e) {
            throw new DatabasePersistenceException("Failed to query run by ID: " + e.getMessage(), e);
        }
        return Optional.empty();
    }

    @Override
    public Optional<AllocationRun> findLatestRun() {
        String sql = "SELECT * FROM allocation_runs ORDER BY run_timestamp DESC LIMIT 1;";
        try (Connection conn = dbManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                AllocationRun run = mapResultSetToRun(rs);
                run.setAllocations(loadAllocationsForRun(conn, run.getRunId()));
                run.setMetrics(loadMetricsForRun(conn, run.getRunId()));
                return Optional.of(run);
            }
        } catch (SQLException e) {
            throw new DatabasePersistenceException("Failed to query latest run: " + e.getMessage(), e);
        }
        return Optional.empty();
    }

    @Override
    public List<AllocationRun> findAllRuns() {
        List<AllocationRun> list = new ArrayList<>();
        String sql = "SELECT * FROM allocation_runs ORDER BY run_timestamp DESC;";
        try (Connection conn = dbManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                AllocationRun run = mapResultSetToRun(rs);
                run.setAllocations(loadAllocationsForRun(conn, run.getRunId()));
                run.setMetrics(loadMetricsForRun(conn, run.getRunId()));
                list.add(run);
            }
        } catch (SQLException e) {
            throw new DatabasePersistenceException("Failed to list allocation runs: " + e.getMessage(), e);
        }
        return list;
    }

    @Override
    public void deleteRun(String runId) {
        String sql = "DELETE FROM allocation_runs WHERE run_id = ?;";
        try (Connection conn = dbManager.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, runId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DatabasePersistenceException("Failed to delete allocation run: " + e.getMessage(), e);
        }
    }

    @Override
    public int countRuns() {
        String sql = "SELECT COUNT(*) FROM allocation_runs;";
        try (Connection conn = dbManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            throw new DatabasePersistenceException("Failed to count allocation runs: " + e.getMessage(), e);
        }
        return 0;
    }

    private List<AllocatedItem> loadAllocationsForRun(Connection conn, String runId) throws SQLException {
        List<AllocatedItem> items = new ArrayList<>();
        String sql = "SELECT * FROM allocations WHERE run_id = ?;";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, runId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    AllocatedItem item = new AllocatedItem();
                    item.setAllocationId(rs.getString("allocation_id"));
                    item.setRunId(rs.getString("run_id"));
                    item.setRequestId(rs.getString("request_id"));
                    item.setRequesterName(rs.getString("requester_name"));
                    item.setResourceId(rs.getString("resource_id"));
                    item.setRequestedQuantity(rs.getDouble("requested_quantity"));
                    item.setAllocatedQuantity(rs.getDouble("allocated_quantity"));
                    item.setFulfilmentRatio(rs.getDouble("fulfilment_ratio"));
                    item.setStatus(AllocationStatus.valueOf(rs.getString("status")));
                    item.setExplanation(rs.getString("explanation"));
                    items.add(item);
                }
            }
        }
        return items;
    }

    private EvaluationMetrics loadMetricsForRun(Connection conn, String runId) throws SQLException {
        String sql = "SELECT * FROM allocation_metrics WHERE run_id = ?;";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, runId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    EvaluationMetrics m = new EvaluationMetrics();
                    m.setAggregateUtilisation(rs.getDouble("aggregate_utilisation"));
                    m.setOverallFulfilment(rs.getDouble("overall_fulfilment"));
                    m.setAverageFulfilment(rs.getDouble("average_fulfilment"));
                    m.setJainsFairnessIndex(rs.getDouble("jains_fairness_index"));
                    m.setFullySatisfiedCount(rs.getInt("fully_satisfied_count"));
                    m.setPartiallySatisfiedCount(rs.getInt("partially_satisfied_count"));
                    m.setUnsatisfiedCount(rs.getInt("unsatisfied_count"));
                    m.setUnallocatedCapacity(rs.getDouble("unallocated_capacity"));

                    String violationsStr = rs.getString("constraint_violations");
                    if (violationsStr != null && !violationsStr.trim().isEmpty()) {
                        for (String v : violationsStr.split(";")) {
                            m.addConstraintViolation(v.trim());
                        }
                    }
                    return m;
                }
            }
        }
        return new EvaluationMetrics();
    }

    private AllocationRun mapResultSetToRun(ResultSet rs) throws SQLException {
        AllocationRun run = new AllocationRun();
        run.setRunId(rs.getString("run_id"));
        String ts = rs.getString("run_timestamp");
        if (ts != null && !ts.isEmpty()) {
            run.setTimestamp(LocalDateTime.parse(ts));
        }
        String strat = rs.getString("strategy_used");
        if (strat != null) {
            run.setStrategyUsed(StrategyType.valueOf(strat));
        }
        run.setScenarioName(rs.getString("scenario_name"));
        run.setBaseline(rs.getInt("is_baseline") == 1);
        run.setSummaryNotes(rs.getString("summary_notes"));
        return run;
    }
}
