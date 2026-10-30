package com.resourceflow.ui.views;

import com.resourceflow.model.AllocatedItem;
import com.resourceflow.model.AllocationRun;
import com.resourceflow.model.ReallocationDiff;
import com.resourceflow.service.HistoryService;
import com.resourceflow.ui.util.DialogHelper;
import com.resourceflow.ui.util.TaskRunner;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.util.List;
import java.util.Locale;

/**
 * Historical records viewer allowing users to audit, drill down into,
 * and compare historical allocation runs.
 */
public class AllocationHistoryView extends VBox {

    private final HistoryService historyService;
    private final TableView<AllocationRun> runsTable;
    private final ObservableList<AllocationRun> runsList;

    public AllocationHistoryView(HistoryService historyService) {
        this.historyService = historyService;
        this.runsList = FXCollections.observableArrayList();
        this.runsTable = new TableView<>(runsList);

        setSpacing(20);
        getStyleClass().add("content-area");
        setPadding(new Insets(24));

        // Header
        VBox headerBox = new VBox(4);
        Label title = new Label("Historical Allocation Audit & Comparison");
        title.getStyleClass().add("view-header-title");
        Label subtitle = new Label("Review immutable historical runs, evaluate past performance, and compare runs side-by-side.");
        subtitle.getStyleClass().add("view-header-subtitle");
        headerBox.getChildren().addAll(title, subtitle);

        // Toolbar
        HBox toolbar = createToolbar();

        // Table
        setupRunsTable();
        VBox.setVgrow(runsTable, Priority.ALWAYS);

        getChildren().addAll(headerBox, toolbar, runsTable);
        refreshData();
    }

    private HBox createToolbar() {
        HBox toolbar = new HBox(12);
        toolbar.setAlignment(Pos.CENTER_LEFT);

        Button inspectBtn = new Button("🔍 Inspect Run Allocations");
        inspectBtn.getStyleClass().add("btn-primary");
        inspectBtn.setOnAction(e -> handleInspectRun());

        Button compareBtn = new Button("⚖ Compare Selected Runs");
        compareBtn.getStyleClass().add("btn-secondary");
        compareBtn.setOnAction(e -> handleCompareRuns());

        Button deleteBtn = new Button("Delete Run");
        deleteBtn.getStyleClass().add("btn-danger");
        deleteBtn.setOnAction(e -> handleDeleteRun());

        toolbar.getChildren().addAll(inspectBtn, compareBtn, deleteBtn);
        return toolbar;
    }

    private void setupRunsTable() {
        runsTable.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

        TableColumn<AllocationRun, String> idCol = new TableColumn<>("Run ID");
        idCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getRunId()));
        idCol.setPrefWidth(120);

        TableColumn<AllocationRun, String> timeCol = new TableColumn<>("Timestamp");
        timeCol.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().getTimestamp() != null ? d.getValue().getTimestamp().toString().replace("T", " ").substring(0, 19) : ""));
        timeCol.setPrefWidth(160);

        TableColumn<AllocationRun, String> stratCol = new TableColumn<>("Strategy");
        stratCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getStrategyUsed().getDisplayName()));
        stratCol.setPrefWidth(180);

        TableColumn<AllocationRun, String> nameCol = new TableColumn<>("Scenario / Run Title");
        nameCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getScenarioName()));
        nameCol.setPrefWidth(200);

        TableColumn<AllocationRun, String> utilCol = new TableColumn<>("Utilisation (%)");
        utilCol.setCellValueFactory(d -> new SimpleStringProperty(String.format(Locale.US, "%.1f%%", d.getValue().getMetrics().getAggregateUtilisation())));
        utilCol.setPrefWidth(110);

        TableColumn<AllocationRun, String> fulfilCol = new TableColumn<>("Fulfilment (%)");
        fulfilCol.setCellValueFactory(d -> new SimpleStringProperty(String.format(Locale.US, "%.1f%%", d.getValue().getMetrics().getOverallFulfilment())));
        fulfilCol.setPrefWidth(110);

        TableColumn<AllocationRun, String> fairCol = new TableColumn<>("Jain's Fairness");
        fairCol.setCellValueFactory(d -> new SimpleStringProperty(String.format(Locale.US, "%.3f", d.getValue().getMetrics().getJainsFairnessIndex())));
        fairCol.setPrefWidth(110);

        TableColumn<AllocationRun, String> countCol = new TableColumn<>("Sat / Part / Denied");
        countCol.setCellValueFactory(d -> new SimpleStringProperty(String.format("%d / %d / %d",
                d.getValue().getMetrics().getFullySatisfiedCount(),
                d.getValue().getMetrics().getPartiallySatisfiedCount(),
                d.getValue().getMetrics().getUnsatisfiedCount())));
        countCol.setPrefWidth(140);

        runsTable.getColumns().addAll(idCol, timeCol, stratCol, nameCol, utilCol, fulfilCol, fairCol, countCol);
        runsTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
    }

    private void handleInspectRun() {
        AllocationRun selected = runsTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            DialogHelper.showWarning("Selection Required", null, "Please select an allocation run to inspect.");
            return;
        }

        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Inspect Run: " + selected.getRunId());
        dialog.setHeaderText("Allocations and metrics for " + selected.getScenarioName());
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);

        VBox content = new VBox(12);
        content.setPrefWidth(700);
        content.setPrefHeight(450);

        TableView<AllocatedItem> table = new TableView<>(FXCollections.observableArrayList(selected.getAllocations()));
        TableColumn<AllocatedItem, String> reqCol = new TableColumn<>("Requester");
        reqCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getRequesterName()));
        TableColumn<AllocatedItem, String> resCol = new TableColumn<>("Resource");
        resCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getResourceId()));
        TableColumn<AllocatedItem, String> demCol = new TableColumn<>("Demanded");
        demCol.setCellValueFactory(d -> new SimpleStringProperty(String.format(Locale.US, "%.1f", d.getValue().getRequestedQuantity())));
        TableColumn<AllocatedItem, String> allocCol = new TableColumn<>("Allocated");
        allocCol.setCellValueFactory(d -> new SimpleStringProperty(String.format(Locale.US, "%.1f", d.getValue().getAllocatedQuantity())));
        TableColumn<AllocatedItem, String> statusCol = new TableColumn<>("Status");
        statusCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getStatus().name()));
        TableColumn<AllocatedItem, String> expCol = new TableColumn<>("Explanation");
        expCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getExplanation()));

        table.getColumns().addAll(reqCol, resCol, demCol, allocCol, statusCol, expCol);
        VBox.setVgrow(table, Priority.ALWAYS);
        content.getChildren().add(table);

        dialog.getDialogPane().setContent(content);
        dialog.showAndWait();
    }

    private void handleCompareRuns() {
        var selected = runsTable.getSelectionModel().getSelectedItems();
        if (selected.size() != 2) {
            DialogHelper.showWarning("Comparison Requires 2 Runs", null,
                    "Please select exactly two runs (Ctrl+Click) from the table to compare them.");
            return;
        }

        AllocationRun run1 = selected.get(0);
        AllocationRun run2 = selected.get(1);

        TaskRunner.executeAsync(() -> historyService.compareRuns(run1.getRunId(), run2.getRunId()),
                diff -> {
                    DialogHelper.showInfo("Run Comparison Diff",
                            "Comparing " + run1.getScenarioName() + " vs " + run2.getScenarioName(),
                            String.format(Locale.US,
                                    "%s\n\n- Improved requests: %d\n- Degraded requests: %d\n- Dropped to 0: %d\n- Unchanged: %d\n- Total capacity reassigned: %.2f units",
                                    diff.getSummaryExplanation(),
                                    diff.getImprovedRequests().size(),
                                    diff.getDegradedRequests().size(),
                                    diff.getDroppedRequests().size(),
                                    diff.getUnchangedRequests().size(),
                                    diff.getCapacityReassigned()));
                },
                error -> DialogHelper.showError("Comparison Error", "Failed to compare runs", error.getMessage()));
    }

    private void handleDeleteRun() {
        AllocationRun selected = runsTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            DialogHelper.showWarning("Selection Required", null, "Please select an allocation run to delete.");
            return;
        }

        boolean confirm = DialogHelper.showConfirmation("Delete Historical Run",
                "Delete run " + selected.getRunId() + "?",
                "This will permanently delete this historical snapshot and its associated allocation records.");

        if (confirm) {
            historyService.deleteRun(selected.getRunId());
            refreshData();
        }
    }

    public void refreshData() {
        TaskRunner.executeAsync(historyService::getAllRuns,
                runs -> {
                    runsList.clear();
                    runsList.addAll(runs);
                }, null);
    }
}
