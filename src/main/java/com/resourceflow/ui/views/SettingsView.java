package com.resourceflow.ui.views;

import com.resourceflow.database.DatabaseManager;
import com.resourceflow.service.RequestService;
import com.resourceflow.service.ResourceService;
import com.resourceflow.ui.util.DialogHelper;
import com.resourceflow.util.AuditLog;
import com.resourceflow.util.DemoDataSeeder;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.io.IOException;
import java.nio.file.Path;

/**
 * Settings and Configuration screen showing local storage paths,
 * diagnostic info, and sample data controls.
 */
public class SettingsView extends ScrollPane {

    private final DatabaseManager databaseManager;
    private final ResourceService resourceService;
    private final RequestService requestService;

    public SettingsView(DatabaseManager databaseManager, ResourceService resourceService, RequestService requestService) {
        this.databaseManager = databaseManager;
        this.resourceService = resourceService;
        this.requestService = requestService;

        setFitToWidth(true);
        setStyle("-fx-background-color: transparent; -fx-border-color: transparent;");

        VBox content = new VBox(24);
        content.getStyleClass().add("content-area");
        content.setPadding(new Insets(24));

        // Header
        VBox headerBox = new VBox(4);
        Label title = new Label("System Settings & Diagnostics");
        title.getStyleClass().add("view-header-title");
        Label subtitle = new Label("Local storage configuration, demonstration data controls, and system parameters.");
        subtitle.getStyleClass().add("view-header-subtitle");
        headerBox.getChildren().addAll(title, subtitle);

        // Database Info Card
        VBox dbCard = createDatabaseCard();

        // Sample Data Card
        VBox demoCard = createDemoDataCard();

        // Audit Log Card
        VBox auditCard = createAuditLogCard();

        // Technology & Architecture Info Card
        VBox techCard = createTechInfoCard();

        content.getChildren().addAll(headerBox, dbCard, demoCard, auditCard, techCard);
        setContent(content);
    }

    private VBox createDatabaseCard() {
        VBox card = new VBox(12);
        card.getStyleClass().add("card");

        Label title = new Label("SQLite Database Storage");
        title.getStyleClass().add("metric-title");

        GridPane grid = new GridPane();
        grid.setHgap(16);
        grid.setVgap(8);

        TextField pathField = new TextField(databaseManager.getDbUrl());
        pathField.setEditable(false);
        pathField.setPrefWidth(550);

        grid.add(new Label("Connection URL:"), 0, 0);
        grid.add(pathField, 1, 0);

        grid.add(new Label("Foreign Keys:"), 0, 1);
        grid.add(new Label("Enabled (PRAGMA foreign_keys = ON)"), 1, 1);

        grid.add(new Label("Journal Mode:"), 0, 2);
        grid.add(new Label("WAL (Write-Ahead Logging)"), 1, 2);

        card.getChildren().addAll(title, grid);
        return card;
    }

    private VBox createDemoDataCard() {
        VBox card = new VBox(12);
        card.getStyleClass().add("card");

        Label title = new Label("Demonstration & Seed Data");
        title.getStyleClass().add("metric-title");

        Label info = new Label("Populate realistic academic/research resources (GPU clusters, HPC cores, chemistry lab seats, research grants) and competing demand requests.");
        info.setStyle("-fx-text-fill: #94a3b8;");

        HBox actions = new HBox(12);
        actions.setAlignment(Pos.CENTER_LEFT);

        Button seedBtn = new Button("🌱 Seed Realistic Demo Data");
        seedBtn.getStyleClass().add("btn-primary");
        seedBtn.setOnAction(e -> {
            try {
                DemoDataSeeder.seedDemoDataIfEmpty(resourceService, requestService);
                DialogHelper.showInfo("Data Seeded", null, "Sample resources and competing requests successfully loaded into SQLite.");
            } catch (Exception ex) {
                DialogHelper.showError("Seed Error", null, ex.getMessage());
            }
        });

        actions.getChildren().add(seedBtn);
        card.getChildren().addAll(title, info, actions);
        return card;
    }

    /**
     * Shows the newest lines of the audit log, which a background thread writes to disk.
     */
    private VBox createAuditLogCard() {
        VBox card = new VBox(12);
        card.getStyleClass().add("card");

        Label title = new Label("Audit Log (background writer thread)");
        title.getStyleClass().add("metric-title");

        Path file = AuditLog.currentFile();
        Label pathLabel = new Label(file != null ? file.toString() : "Audit log is not running");
        pathLabel.setStyle("-fx-text-fill: #94a3b8;");

        TextArea logArea = new TextArea();
        logArea.setEditable(false);
        logArea.setPrefRowCount(10);
        logArea.setStyle("-fx-font-family: 'Menlo', 'Consolas', monospace; -fx-font-size: 11px;");

        Button refreshBtn = new Button("↻ Refresh");
        refreshBtn.getStyleClass().add("btn-secondary");
        refreshBtn.setOnAction(e -> loadAuditLines(logArea));

        loadAuditLines(logArea);
        card.getChildren().addAll(title, pathLabel, logArea, refreshBtn);
        return card;
    }

    private void loadAuditLines(TextArea logArea) {
        try {
            logArea.setText(String.join("\n", AuditLog.tail(200)));
            logArea.positionCaret(logArea.getLength()); // scroll to newest
        } catch (IOException ex) {
            logArea.setText("Could not read audit log: " + ex.getMessage());
        }
    }

    private VBox createTechInfoCard() {
        VBox card = new VBox(12);
        card.getStyleClass().add("card");

        Label title = new Label("Architecture & Runtime Information");
        title.getStyleClass().add("metric-title");

        GridPane grid = new GridPane();
        grid.setHgap(20);
        grid.setVgap(6);

        grid.add(new Label("Runtime Platform:"), 0, 0);
        grid.add(new Label("Core Java 21 / OpenJDK 25 (Desktop Only)"), 1, 0);

        grid.add(new Label("GUI Framework:"), 0, 1);
        grid.add(new Label("JavaFX 21 (Controls, Charts, Dark CSS Theme)"), 1, 1);

        grid.add(new Label("Persistence:"), 0, 2);
        grid.add(new Label("SQLite 3 with JDBC Driver (Local File-based)"), 1, 2);

        grid.add(new Label("Allocation Strategies:"), 0, 3);
        grid.add(new Label("Priority-Based, Fair-Share (Max-Min), Proportional"), 1, 3);

        grid.add(new Label("Fairness Metric:"), 0, 4);
        grid.add(new Label("Jain's Fairness Index: J = (Σ x_i)² / (n * Σ x_i²)"), 1, 4);

        grid.add(new Label("Console Mode:"), 0, 5);
        grid.add(new Label("java -jar resourceflow-1.0.0.jar --console"), 1, 5);

        card.getChildren().addAll(title, grid);
        return card;
    }
}
