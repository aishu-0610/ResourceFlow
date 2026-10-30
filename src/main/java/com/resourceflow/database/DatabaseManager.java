package com.resourceflow.database;

import com.resourceflow.exception.DatabasePersistenceException;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Manages SQLite database connectivity, local file path resolution, and schema initialization.
 */
public class DatabaseManager {

    private static final String APP_DIR_NAME = "ResourceFlow";
    private static final String DB_FILE_NAME = "resourceflow.db";
    private final String dbUrl;

    /**
     * Default constructor: uses local user application data directory.
     */
    public DatabaseManager() {
        this.dbUrl = "jdbc:sqlite:" + resolveDefaultDbPath();
    }

    /**
     * Parameterized constructor for custom database locations (such as test databases or in-memory).
     */
    public DatabaseManager(String dbPath) {
        if (dbPath == null || dbPath.trim().isEmpty() || dbPath.equals(":memory:")) {
            this.dbUrl = "jdbc:sqlite::memory:";
        } else {
            this.dbUrl = "jdbc:sqlite:" + dbPath;
        }
    }

    /**
     * Resolves the persistent application data directory on the host OS.
     */
    public static String resolveDefaultDbPath() {
        String baseDir;
        String os = System.getProperty("os.name", "").toLowerCase();

        if (os.contains("win")) {
            String appData = System.getenv("APPDATA");
            baseDir = (appData != null && !appData.isEmpty()) ? appData : System.getProperty("user.home");
        } else if (os.contains("mac")) {
            baseDir = System.getProperty("user.home") + "/Library/Application Support";
        } else {
            baseDir = System.getProperty("user.home") + "/.local/share";
        }

        File appFolder = new File(baseDir, APP_DIR_NAME);
        if (!appFolder.exists()) {
            boolean created = appFolder.mkdirs();
            if (!created && !appFolder.exists()) {
                // Fallback to user home directly
                appFolder = new File(System.getProperty("user.home"), "." + APP_DIR_NAME.toLowerCase());
                appFolder.mkdirs();
            }
        }

        return new File(appFolder, DB_FILE_NAME).getAbsolutePath();
    }

    /**
     * Opens a new database connection with Foreign Keys and WAL enabled.
     */
    public Connection getConnection() throws SQLException {
        Connection conn = DriverManager.getConnection(dbUrl);
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("PRAGMA foreign_keys = ON;");
            if (!dbUrl.contains(":memory:")) {
                stmt.execute("PRAGMA journal_mode = WAL;");
            }
        }
        return conn;
    }

    /**
     * Initializes the relational database schema if tables do not exist.
     */
    public void initializeSchema() {
        String schemaSql = """
            -- 1. Resources table
            CREATE TABLE IF NOT EXISTS resources (
                resource_id TEXT PRIMARY KEY,
                name TEXT NOT NULL,
                category TEXT NOT NULL,
                total_capacity REAL NOT NULL CHECK(total_capacity >= 0),
                available_capacity REAL NOT NULL CHECK(available_capacity >= 0),
                unit TEXT NOT NULL,
                description TEXT,
                is_active INTEGER NOT NULL DEFAULT 1 CHECK(is_active IN (0, 1)),
                created_at TEXT NOT NULL,
                updated_at TEXT NOT NULL
            );

            -- 2. Allocation Requests table
            CREATE TABLE IF NOT EXISTS allocation_requests (
                request_id TEXT PRIMARY KEY,
                requester_name TEXT NOT NULL,
                resource_id TEXT NOT NULL,
                requested_quantity REAL NOT NULL CHECK(requested_quantity > 0),
                priority INTEGER NOT NULL CHECK(priority BETWEEN 1 AND 5),
                min_acceptable_quantity REAL NOT NULL DEFAULT 0.0 CHECK(min_acceptable_quantity >= 0),
                is_all_or_nothing INTEGER NOT NULL DEFAULT 0 CHECK(is_all_or_nothing IN (0, 1)),
                submission_date TEXT NOT NULL,
                deadline TEXT,
                status TEXT NOT NULL DEFAULT 'PENDING',
                notes TEXT,
                FOREIGN KEY (resource_id) REFERENCES resources(resource_id) ON DELETE RESTRICT
            );

            -- 3. Allocation Runs table
            CREATE TABLE IF NOT EXISTS allocation_runs (
                run_id TEXT PRIMARY KEY,
                run_timestamp TEXT NOT NULL,
                strategy_used TEXT NOT NULL,
                scenario_name TEXT,
                is_baseline INTEGER NOT NULL DEFAULT 1,
                summary_notes TEXT
            );

            -- 4. Allocations table
            CREATE TABLE IF NOT EXISTS allocations (
                allocation_id TEXT PRIMARY KEY,
                run_id TEXT NOT NULL,
                request_id TEXT NOT NULL,
                requester_name TEXT NOT NULL,
                resource_id TEXT NOT NULL,
                requested_quantity REAL NOT NULL,
                allocated_quantity REAL NOT NULL CHECK(allocated_quantity >= 0),
                fulfilment_ratio REAL NOT NULL CHECK(fulfilment_ratio BETWEEN 0.0 AND 1.0),
                status TEXT NOT NULL,
                explanation TEXT,
                FOREIGN KEY (run_id) REFERENCES allocation_runs(run_id) ON DELETE CASCADE
            );

            -- 5. Allocation Metrics table
            CREATE TABLE IF NOT EXISTS allocation_metrics (
                run_id TEXT PRIMARY KEY,
                aggregate_utilisation REAL NOT NULL,
                overall_fulfilment REAL NOT NULL,
                average_fulfilment REAL NOT NULL,
                jains_fairness_index REAL NOT NULL,
                fully_satisfied_count INTEGER NOT NULL,
                partially_satisfied_count INTEGER NOT NULL,
                unsatisfied_count INTEGER NOT NULL,
                unallocated_capacity REAL NOT NULL,
                constraint_violations TEXT,
                FOREIGN KEY (run_id) REFERENCES allocation_runs(run_id) ON DELETE CASCADE
            );

            -- 6. What-If Scenarios table
            CREATE TABLE IF NOT EXISTS scenarios (
                scenario_id TEXT PRIMARY KEY,
                name TEXT NOT NULL UNIQUE,
                description TEXT,
                baseline_run_id TEXT,
                created_at TEXT NOT NULL,
                FOREIGN KEY (baseline_run_id) REFERENCES allocation_runs(run_id) ON DELETE SET NULL
            );
        """;

        try (Connection conn = getConnection(); Statement stmt = conn.createStatement()) {
            for (String statementSql : schemaSql.split(";")) {
                String trimmed = statementSql.trim();
                if (!trimmed.isEmpty()) {
                    stmt.execute(trimmed);
                }
            }
        } catch (SQLException e) {
            throw new DatabasePersistenceException("Failed to initialize database schema: " + e.getMessage(), e);
        }
    }

    public String getDbUrl() {
        return dbUrl;
    }
}
