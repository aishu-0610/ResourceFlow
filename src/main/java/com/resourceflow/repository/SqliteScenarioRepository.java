package com.resourceflow.repository;

import com.resourceflow.database.DatabaseManager;
import com.resourceflow.exception.DatabasePersistenceException;
import com.resourceflow.model.Scenario;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * SQLite JDBC implementation of ScenarioRepository.
 */
public class SqliteScenarioRepository implements ScenarioRepository {

    private final DatabaseManager dbManager;

    public SqliteScenarioRepository(DatabaseManager dbManager) {
        this.dbManager = dbManager;
    }

    @Override
    public void save(Scenario scenario) {
        String sql = """
            INSERT INTO scenarios (scenario_id, name, description, baseline_run_id, created_at)
            VALUES (?, ?, ?, ?, ?);
        """;
        try (Connection conn = dbManager.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, scenario.getScenarioId());
            ps.setString(2, scenario.getName());
            ps.setString(3, scenario.getDescription());
            ps.setString(4, scenario.getBaselineRunId());
            ps.setString(5, (scenario.getCreatedAt() != null ? scenario.getCreatedAt() : LocalDateTime.now()).toString());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DatabasePersistenceException("Failed to save scenario: " + e.getMessage(), e);
        }
    }

    @Override
    public void update(Scenario scenario) {
        String sql = """
            UPDATE scenarios
            SET name = ?, description = ?, baseline_run_id = ?
            WHERE scenario_id = ?;
        """;
        try (Connection conn = dbManager.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, scenario.getName());
            ps.setString(2, scenario.getDescription());
            ps.setString(3, scenario.getBaselineRunId());
            ps.setString(4, scenario.getScenarioId());
            int rows = ps.executeUpdate();
            if (rows == 0) {
                throw new DatabasePersistenceException("Scenario not found for update: " + scenario.getScenarioId());
            }
        } catch (SQLException e) {
            throw new DatabasePersistenceException("Failed to update scenario: " + e.getMessage(), e);
        }
    }

    @Override
    public void deleteById(String scenarioId) {
        String sql = "DELETE FROM scenarios WHERE scenario_id = ?;";
        try (Connection conn = dbManager.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, scenarioId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DatabasePersistenceException("Failed to delete scenario: " + e.getMessage(), e);
        }
    }

    @Override
    public Optional<Scenario> findById(String scenarioId) {
        String sql = "SELECT * FROM scenarios WHERE scenario_id = ?;";
        try (Connection conn = dbManager.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, scenarioId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToScenario(rs));
                }
            }
        } catch (SQLException e) {
            throw new DatabasePersistenceException("Failed to query scenario by ID: " + e.getMessage(), e);
        }
        return Optional.empty();
    }

    @Override
    public List<Scenario> findAll() {
        List<Scenario> list = new ArrayList<>();
        String sql = "SELECT * FROM scenarios ORDER BY created_at DESC;";
        try (Connection conn = dbManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                list.add(mapResultSetToScenario(rs));
            }
        } catch (SQLException e) {
            throw new DatabasePersistenceException("Failed to list scenarios: " + e.getMessage(), e);
        }
        return list;
    }

    private Scenario mapResultSetToScenario(ResultSet rs) throws SQLException {
        Scenario s = new Scenario();
        s.setScenarioId(rs.getString("scenario_id"));
        s.setName(rs.getString("name"));
        s.setDescription(rs.getString("description"));
        s.setBaselineRunId(rs.getString("baseline_run_id"));
        String created = rs.getString("created_at");
        if (created != null && !created.isEmpty()) {
            s.setCreatedAt(LocalDateTime.parse(created));
        }
        return s;
    }
}
