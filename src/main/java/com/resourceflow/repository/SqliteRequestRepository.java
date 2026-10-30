package com.resourceflow.repository;

import com.resourceflow.database.DatabaseManager;
import com.resourceflow.exception.DatabasePersistenceException;
import com.resourceflow.model.AllocationRequest;
import com.resourceflow.model.RequestStatus;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * SQLite JDBC implementation of RequestRepository.
 */
public class SqliteRequestRepository implements RequestRepository {

    private final DatabaseManager dbManager;

    public SqliteRequestRepository(DatabaseManager dbManager) {
        this.dbManager = dbManager;
    }

    @Override
    public void save(AllocationRequest request) {
        String sql = """
            INSERT INTO allocation_requests (request_id, requester_name, resource_id, requested_quantity, priority, min_acceptable_quantity, is_all_or_nothing, submission_date, deadline, status, notes)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?);
        """;
        try (Connection conn = dbManager.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, request.getRequestId());
            ps.setString(2, request.getRequesterName());
            ps.setString(3, request.getResourceId());
            ps.setDouble(4, request.getRequestedQuantity());
            ps.setInt(5, request.getPriority());
            ps.setDouble(6, request.getMinAcceptableQuantity());
            ps.setInt(7, request.isAllOrNothing() ? 1 : 0);
            ps.setString(8, (request.getSubmissionDate() != null ? request.getSubmissionDate() : LocalDateTime.now()).toString());
            ps.setString(9, request.getDeadline() != null ? request.getDeadline().toString() : null);
            ps.setString(10, request.getStatus().name());
            ps.setString(11, request.getNotes());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DatabasePersistenceException("Failed to save allocation request " + request.getRequestId() + ": " + e.getMessage(), e);
        }
    }

    @Override
    public void update(AllocationRequest request) {
        String sql = """
            UPDATE allocation_requests
            SET requester_name = ?, resource_id = ?, requested_quantity = ?, priority = ?, min_acceptable_quantity = ?, is_all_or_nothing = ?, deadline = ?, status = ?, notes = ?
            WHERE request_id = ?;
        """;
        try (Connection conn = dbManager.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, request.getRequesterName());
            ps.setString(2, request.getResourceId());
            ps.setDouble(3, request.getRequestedQuantity());
            ps.setInt(4, request.getPriority());
            ps.setDouble(5, request.getMinAcceptableQuantity());
            ps.setInt(6, request.isAllOrNothing() ? 1 : 0);
            ps.setString(7, request.getDeadline() != null ? request.getDeadline().toString() : null);
            ps.setString(8, request.getStatus().name());
            ps.setString(9, request.getNotes());
            ps.setString(10, request.getRequestId());
            int rows = ps.executeUpdate();
            if (rows == 0) {
                throw new DatabasePersistenceException("Request not found for update: " + request.getRequestId());
            }
        } catch (SQLException e) {
            throw new DatabasePersistenceException("Failed to update allocation request " + request.getRequestId() + ": " + e.getMessage(), e);
        }
    }

    @Override
    public void deleteById(String requestId) {
        String sql = "DELETE FROM allocation_requests WHERE request_id = ?;";
        try (Connection conn = dbManager.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, requestId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DatabasePersistenceException("Failed to delete request " + requestId + ": " + e.getMessage(), e);
        }
    }

    @Override
    public Optional<AllocationRequest> findById(String requestId) {
        String sql = "SELECT * FROM allocation_requests WHERE request_id = ?;";
        try (Connection conn = dbManager.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, requestId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToRequest(rs));
                }
            }
        } catch (SQLException e) {
            throw new DatabasePersistenceException("Failed to query request by ID " + requestId + ": " + e.getMessage(), e);
        }
        return Optional.empty();
    }

    @Override
    public List<AllocationRequest> findAll() {
        List<AllocationRequest> list = new ArrayList<>();
        String sql = "SELECT * FROM allocation_requests ORDER BY submission_date ASC;";
        try (Connection conn = dbManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                list.add(mapResultSetToRequest(rs));
            }
        } catch (SQLException e) {
            throw new DatabasePersistenceException("Failed to list allocation requests: " + e.getMessage(), e);
        }
        return list;
    }

    @Override
    public List<AllocationRequest> findByStatus(RequestStatus status) {
        List<AllocationRequest> list = new ArrayList<>();
        String sql = "SELECT * FROM allocation_requests WHERE status = ? ORDER BY submission_date ASC;";
        try (Connection conn = dbManager.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status.name());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSetToRequest(rs));
                }
            }
        } catch (SQLException e) {
            throw new DatabasePersistenceException("Failed to list requests by status: " + e.getMessage(), e);
        }
        return list;
    }

    @Override
    public List<AllocationRequest> findEligibleRequests() {
        List<AllocationRequest> list = new ArrayList<>();
        String sql = "SELECT * FROM allocation_requests WHERE status NOT IN ('CANCELLED', 'REJECTED') ORDER BY priority DESC, submission_date ASC;";
        try (Connection conn = dbManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                list.add(mapResultSetToRequest(rs));
            }
        } catch (SQLException e) {
            throw new DatabasePersistenceException("Failed to list eligible requests: " + e.getMessage(), e);
        }
        return list;
    }

    @Override
    public boolean existsById(String requestId) {
        String sql = "SELECT 1 FROM allocation_requests WHERE request_id = ? LIMIT 1;";
        try (Connection conn = dbManager.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, requestId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            throw new DatabasePersistenceException("Failed to check request existence: " + e.getMessage(), e);
        }
    }

    private AllocationRequest mapResultSetToRequest(ResultSet rs) throws SQLException {
        AllocationRequest r = new AllocationRequest();
        r.setRequestId(rs.getString("request_id"));
        r.setRequesterName(rs.getString("requester_name"));
        r.setResourceId(rs.getString("resource_id"));
        r.setRequestedQuantity(rs.getDouble("requested_quantity"));
        r.setPriority(rs.getInt("priority"));
        r.setMinAcceptableQuantity(rs.getDouble("min_acceptable_quantity"));
        r.setAllOrNothing(rs.getInt("is_all_or_nothing") == 1);

        String subDate = rs.getString("submission_date");
        if (subDate != null && !subDate.isEmpty()) {
            r.setSubmissionDate(LocalDateTime.parse(subDate));
        }

        String deadline = rs.getString("deadline");
        if (deadline != null && !deadline.isEmpty()) {
            r.setDeadline(LocalDateTime.parse(deadline));
        }

        String statusStr = rs.getString("status");
        if (statusStr != null) {
            r.setStatus(RequestStatus.valueOf(statusStr));
        }
        r.setNotes(rs.getString("notes"));
        return r;
    }
}
