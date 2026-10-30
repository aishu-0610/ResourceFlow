package com.resourceflow.repository;

import com.resourceflow.database.DatabaseManager;
import com.resourceflow.exception.DatabasePersistenceException;
import com.resourceflow.model.Resource;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * SQLite JDBC implementation of ResourceRepository.
 */
public class SqliteResourceRepository implements ResourceRepository {

    private final DatabaseManager dbManager;

    public SqliteResourceRepository(DatabaseManager dbManager) {
        this.dbManager = dbManager;
    }

    @Override
    public void save(Resource resource) {
        String sql = """
            INSERT INTO resources (resource_id, name, category, total_capacity, available_capacity, unit, description, is_active, created_at, updated_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?);
        """;
        try (Connection conn = dbManager.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, resource.getResourceId());
            ps.setString(2, resource.getName());
            ps.setString(3, resource.getCategory());
            ps.setDouble(4, resource.getTotalCapacity());
            ps.setDouble(5, resource.getAvailableCapacity());
            ps.setString(6, resource.getUnit());
            ps.setString(7, resource.getDescription());
            ps.setInt(8, resource.isActive() ? 1 : 0);
            ps.setString(9, (resource.getCreatedAt() != null ? resource.getCreatedAt() : LocalDateTime.now()).toString());
            ps.setString(10, (resource.getUpdatedAt() != null ? resource.getUpdatedAt() : LocalDateTime.now()).toString());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DatabasePersistenceException("Failed to save resource " + resource.getResourceId() + ": " + e.getMessage(), e);
        }
    }

    @Override
    public void update(Resource resource) {
        String sql = """
            UPDATE resources
            SET name = ?, category = ?, total_capacity = ?, available_capacity = ?, unit = ?, description = ?, is_active = ?, updated_at = ?
            WHERE resource_id = ?;
        """;
        try (Connection conn = dbManager.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, resource.getName());
            ps.setString(2, resource.getCategory());
            ps.setDouble(3, resource.getTotalCapacity());
            ps.setDouble(4, resource.getAvailableCapacity());
            ps.setString(5, resource.getUnit());
            ps.setString(6, resource.getDescription());
            ps.setInt(7, resource.isActive() ? 1 : 0);
            ps.setString(8, LocalDateTime.now().toString());
            ps.setString(9, resource.getResourceId());
            int rows = ps.executeUpdate();
            if (rows == 0) {
                throw new DatabasePersistenceException("Resource not found for update: " + resource.getResourceId());
            }
        } catch (SQLException e) {
            throw new DatabasePersistenceException("Failed to update resource " + resource.getResourceId() + ": " + e.getMessage(), e);
        }
    }

    @Override
    public void deleteById(String resourceId) {
        String sql = "DELETE FROM resources WHERE resource_id = ?;";
        try (Connection conn = dbManager.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, resourceId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DatabasePersistenceException("Failed to delete resource " + resourceId + ": " + e.getMessage(), e);
        }
    }

    @Override
    public Optional<Resource> findById(String resourceId) {
        String sql = "SELECT * FROM resources WHERE resource_id = ?;";
        try (Connection conn = dbManager.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, resourceId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToResource(rs));
                }
            }
        } catch (SQLException e) {
            throw new DatabasePersistenceException("Failed to query resource by ID " + resourceId + ": " + e.getMessage(), e);
        }
        return Optional.empty();
    }

    @Override
    public List<Resource> findAll() {
        List<Resource> list = new ArrayList<>();
        String sql = "SELECT * FROM resources ORDER BY name ASC;";
        try (Connection conn = dbManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                list.add(mapResultSetToResource(rs));
            }
        } catch (SQLException e) {
            throw new DatabasePersistenceException("Failed to list resources: " + e.getMessage(), e);
        }
        return list;
    }

    @Override
    public List<Resource> findActive() {
        List<Resource> list = new ArrayList<>();
        String sql = "SELECT * FROM resources WHERE is_active = 1 ORDER BY name ASC;";
        try (Connection conn = dbManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                list.add(mapResultSetToResource(rs));
            }
        } catch (SQLException e) {
            throw new DatabasePersistenceException("Failed to list active resources: " + e.getMessage(), e);
        }
        return list;
    }

    @Override
    public boolean hasAllocations(String resourceId) {
        String sql = "SELECT 1 FROM allocations WHERE resource_id = ? LIMIT 1;";
        try (Connection conn = dbManager.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, resourceId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            throw new DatabasePersistenceException("Failed to check allocations for resource " + resourceId + ": " + e.getMessage(), e);
        }
    }

    @Override
    public boolean existsById(String resourceId) {
        String sql = "SELECT 1 FROM resources WHERE resource_id = ? LIMIT 1;";
        try (Connection conn = dbManager.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, resourceId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            throw new DatabasePersistenceException("Failed to check resource existence: " + e.getMessage(), e);
        }
    }

    private Resource mapResultSetToResource(ResultSet rs) throws SQLException {
        Resource r = new Resource();
        r.setResourceId(rs.getString("resource_id"));
        r.setName(rs.getString("name"));
        r.setCategory(rs.getString("category"));
        r.setTotalCapacity(rs.getDouble("total_capacity"));
        r.setAvailableCapacity(rs.getDouble("available_capacity"));
        r.setUnit(rs.getString("unit"));
        r.setDescription(rs.getString("description"));
        r.setActive(rs.getInt("is_active") == 1);

        String created = rs.getString("created_at");
        if (created != null && !created.isEmpty()) {
            r.setCreatedAt(LocalDateTime.parse(created));
        }
        String updated = rs.getString("updated_at");
        if (updated != null && !updated.isEmpty()) {
            r.setUpdatedAt(LocalDateTime.parse(updated));
        }
        return r;
    }
}
