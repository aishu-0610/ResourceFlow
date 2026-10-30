package com.resourceflow.ui.views;

import com.resourceflow.exception.ScenarioFileException;
import com.resourceflow.io.ScenarioFileStore;
import com.resourceflow.model.*;
import com.resourceflow.service.WhatIfService;
import com.resourceflow.ui.util.DialogHelper;
import com.resourceflow.ui.util.TaskRunner;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;

import java.io.File;
import java.util.ArrayList;
import java.util.Locale;

/**
 * Dedicated What-If Scenario Analysis screen.
 * Allows interactive manipulation of resource capacities and demand priorities
 * in an isolated sandbox without affecting production baseline data.
 */
public class WhatIfAnalysisView extends ScrollPane {

    private final WhatIfService whatIfService;

    private Scenario activeScenario;
    private final ObservableList<Resource> sandboxResources;
    private final ObservableList<AllocationRequest> sandboxRequests;
    private final ObservableList<AllocatedItem> simulationResults;

    private TextField scenarioNameField;
    private ComboBox<StrategyType> strategyCombo;
    private Button simulateBtn;
    private Button saveScenarioBtn;
    private ProgressIndicator progressIndicator;

    private Label baselineUtilLabel;
    private Label scenarioUtilLabel;
    private Label baselineFulfilLabel;
    private Label scenarioFulfilLabel;
    private Label baselineFairnessLabel;
    private Label scenarioFairnessLabel;
    private Label diffSummaryLabel;

    public WhatIfAnalysisView(WhatIfService whatIfService) {
        this.whatIfService = whatIfService;
        this.sandboxResources = FXCollections.observableArrayList();
        this.sandboxRequests = FXCollections.observableArrayList();
        this.simulationResults = FXCollections.observableArrayList();

        setFitToWidth(true);
        setStyle("-fx-background-color: transparent; -fx-border-color: transparent;");

        VBox content = new VBox(20);
        content.getStyleClass().add("content-area");
        content.setPadding(new Insets(24));

        // Header
        VBox headerBox = new VBox(4);
        Label title = new Label("What-If Scenario Sandbox");
        title.getStyleClass().add("view-header-title");
        Label subtitle = new Label("Edit capacities, demands and priorities (double-click a cell), move capacity between resources, and simulate in isolation.");
        subtitle.getStyleClass().add("view-header-subtitle");
        headerBox.getChildren().addAll(title, subtitle);

        // Scenario Controls
        VBox controlsCard = createControlsCard();

        // Comparison KPI Card
        VBox comparisonCard = createComparisonCard();

        // Sandbox Resource & Request Tables
        HBox tablesRow = createTablesRow();

        // Simulation Results Table
        VBox resultsCard = createResultsTableCard();

        content.getChildren().addAll(headerBox, controlsCard, comparisonCard, tablesRow, resultsCard);
        setContent(content);

        initSandbox("Hypothetical Capacity Expansion", StrategyType.PRIORITY_BASED);
    }

    private VBox createControlsCard() {
        VBox card = new VBox(14);
        card.getStyleClass().add("card");

        HBox row = new HBox(16);
        row.setAlignment(Pos.CENTER_LEFT);

        scenarioNameField = new TextField("Hypothetical 20% GPU Expansion");
        scenarioNameField.setPrefWidth(280);

        strategyCombo = new ComboBox<>(FXCollections.observableArrayList(StrategyType.values()));
        strategyCombo.setValue(StrategyType.PRIORITY_BASED);
        strategyCombo.setPrefWidth(220);

        simulateBtn = new Button("⚡ Simulate Sandbox Scenario");
        simulateBtn.getStyleClass().add("btn-primary");
        simulateBtn.setOnAction(e -> runSimulation());

        saveScenarioBtn = new Button("💾 Save Scenario");
        saveScenarioBtn.getStyleClass().add("btn-secondary");
        saveScenarioBtn.setOnAction(e -> saveScenario());

        Button resetBtn = new Button("Reset from Live State");
        resetBtn.getStyleClass().add("btn-secondary");
        resetBtn.setOnAction(e -> initSandbox(scenarioNameField.getText(), strategyCombo.getValue()));

        progressIndicator = new ProgressIndicator();
        progressIndicator.setPrefSize(24, 24);
        progressIndicator.setVisible(false);

        row.getChildren().addAll(
                new Label("Scenario:"), scenarioNameField,
                new Label("Strategy:"), strategyCombo,
                simulateBtn, progressIndicator
        );

        // Second row: scenario files (Object Serialization)
        HBox fileRow = new HBox(12);
        fileRow.setAlignment(Pos.CENTER_LEFT);

        Button exportBtn = new Button("⬇ Export Scenario File");
        exportBtn.getStyleClass().add("btn-secondary");
        exportBtn.setOnAction(e -> exportScenarioFile());

        Button importBtn = new Button("⬆ Import Scenario File");
        importBtn.getStyleClass().add("btn-secondary");
        importBtn.setOnAction(e -> importScenarioFile());

        Label fileHint = new Label("Saves resources, requests, strategy and constraints to a " + ScenarioFileStore.FILE_EXTENSION + " file.");
        fileHint.getStyleClass().add("metric-subtitle");

        for (Button b : new Button[]{saveScenarioBtn, resetBtn, exportBtn, importBtn}) {
            b.setMinWidth(Region.USE_PREF_SIZE); // let only the hint text shrink when space is short
        }
        fileRow.getChildren().addAll(saveScenarioBtn, resetBtn, exportBtn, importBtn, fileHint);

        card.getChildren().addAll(row, fileRow);
        return card;
    }

    private VBox createComparisonCard() {
        VBox card = new VBox(12);
        card.getStyleClass().add("card");

        Label cardTitle = new Label("Baseline vs Hypothetical Scenario Metrics");
        cardTitle.getStyleClass().add("metric-title");

        GridPane grid = new GridPane();
        grid.setHgap(24);
        grid.setVgap(8);

        baselineUtilLabel = new Label("—");
        scenarioUtilLabel = new Label("—");
        baselineFulfilLabel = new Label("—");
        scenarioFulfilLabel = new Label("—");
        baselineFairnessLabel = new Label("—");
        scenarioFairnessLabel = new Label("—");

        grid.add(new Label("Metric"), 0, 0);
        grid.add(new Label("Baseline"), 1, 0);
        grid.add(new Label("Hypothetical Scenario"), 2, 0);

        grid.add(new Label("Resource Utilisation:"), 0, 1);
        grid.add(baselineUtilLabel, 1, 1);
        grid.add(scenarioUtilLabel, 2, 1);

        grid.add(new Label("Demand Fulfilment:"), 0, 2);
        grid.add(baselineFulfilLabel, 1, 2);
        grid.add(scenarioFulfilLabel, 2, 2);

        grid.add(new Label("Jain's Fairness Index:"), 0, 3);
        grid.add(baselineFairnessLabel, 1, 3);
        grid.add(scenarioFairnessLabel, 2, 3);

        diffSummaryLabel = new Label("Simulate the scenario to compute metrics.");
        diffSummaryLabel.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 12px;");

        card.getChildren().addAll(cardTitle, grid, diffSummaryLabel);
        return card;
    }

    private HBox createTablesRow() {
        HBox row = new HBox(16);

        // Resource Sandbox Table
        VBox resCard = new VBox(8);
        resCard.getStyleClass().add("card");
        HBox.setHgrow(resCard, Priority.ALWAYS);

        HBox resHeader = new HBox(8);
        resHeader.setAlignment(Pos.CENTER_LEFT);
        Label resTitle = new Label("Sandbox Resources (Editable)");
        resTitle.getStyleClass().add("metric-title");

        Button boostCapBtn = new Button("+10% All Capacity");
        boostCapBtn.getStyleClass().add("btn-secondary");
        boostCapBtn.setOnAction(e -> {
            for (Resource r : sandboxResources) {
                r.setTotalCapacity(r.getTotalCapacity() * 1.10);
                r.setAvailableCapacity(r.getTotalCapacity());
            }
            sandboxResources.setAll(new ArrayList<>(sandboxResources));
        });
        resHeader.getChildren().addAll(resTitle, boostCapBtn);

        TableView<Resource> resTable = new TableView<>(sandboxResources);
        resTable.setEditable(true);
        TableColumn<Resource, String> resNameCol = new TableColumn<>("Resource");
        resNameCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getName()));

        TableColumn<Resource, String> resCapCol = new TableColumn<>("Capacity ✎");
        resCapCol.setCellValueFactory(d -> new SimpleStringProperty(String.format(Locale.US, "%.1f", d.getValue().getTotalCapacity())));
        resCapCol.setCellFactory(TextFieldTableCell.forTableColumn());
        resCapCol.setOnEditCommit(e -> {
            Resource r = e.getRowValue();
            applyEdit(() -> {
                double capacity = Double.parseDouble(e.getNewValue().trim());
                r.setTotalCapacity(capacity);
                r.setAvailableCapacity(capacity);
            });
            resTable.refresh();
        });

        TableColumn<Resource, String> resUnitCol = new TableColumn<>("Unit");
        resUnitCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getUnit()));

        resTable.getColumns().addAll(resNameCol, resCapCol, resUnitCol);
        resTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        resTable.setPrefHeight(200);
        resCard.getChildren().addAll(resHeader, resTable, createTransferRow(resTable));

        // Request Sandbox Table
        VBox reqCard = new VBox(8);
        reqCard.getStyleClass().add("card");
        HBox.setHgrow(reqCard, Priority.ALWAYS);

        HBox reqHeader = new HBox(8);
        reqHeader.setAlignment(Pos.CENTER_LEFT);
        Label reqTitle = new Label("Sandbox Requests");
        reqTitle.getStyleClass().add("metric-title");

        Button boostPrioBtn = new Button("Bump Lower Priorities (+1)");
        boostPrioBtn.getStyleClass().add("btn-secondary");
        boostPrioBtn.setOnAction(e -> {
            for (AllocationRequest r : sandboxRequests) {
                if (r.getPriority() < 5) r.setPriority(r.getPriority() + 1);
            }
            sandboxRequests.setAll(new ArrayList<>(sandboxRequests));
        });
        reqHeader.getChildren().addAll(reqTitle, boostPrioBtn);

        TableView<AllocationRequest> reqTable = new TableView<>(sandboxRequests);
        reqTable.setEditable(true);
        TableColumn<AllocationRequest, String> reqNameCol = new TableColumn<>("Requester");
        reqNameCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getRequesterName()));

        TableColumn<AllocationRequest, String> reqDemandCol = new TableColumn<>("Demanded ✎");
        reqDemandCol.setCellValueFactory(d -> new SimpleStringProperty(String.format(Locale.US, "%.1f", d.getValue().getRequestedQuantity())));
        reqDemandCol.setCellFactory(TextFieldTableCell.forTableColumn());
        reqDemandCol.setOnEditCommit(e -> {
            AllocationRequest r = e.getRowValue();
            applyEdit(() -> r.setRequestedQuantity(Double.parseDouble(e.getNewValue().trim())));
            reqTable.refresh();
        });

        TableColumn<AllocationRequest, String> reqPrioCol = new TableColumn<>("Priority (1-5) ✎");
        reqPrioCol.setCellValueFactory(d -> new SimpleStringProperty(String.valueOf(d.getValue().getPriority())));
        reqPrioCol.setCellFactory(TextFieldTableCell.forTableColumn());
        reqPrioCol.setOnEditCommit(e -> {
            AllocationRequest r = e.getRowValue();
            applyEdit(() -> r.setPriority(Integer.parseInt(e.getNewValue().trim())));
            reqTable.refresh();
        });

        reqTable.getColumns().addAll(reqNameCol, reqDemandCol, reqPrioCol);
        reqTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        reqTable.setPrefHeight(200);
        reqCard.getChildren().addAll(reqHeader, reqTable);

        row.getChildren().addAll(resCard, reqCard);
        return row;
    }

    private VBox createResultsTableCard() {
        VBox card = new VBox(8);
        card.getStyleClass().add("card");

        Label cardTitle = new Label("Hypothetical Allocation Outcomes");
        cardTitle.getStyleClass().add("metric-title");

        TableView<AllocatedItem> table = new TableView<>(simulationResults);
        TableColumn<AllocatedItem, String> reqIdCol = new TableColumn<>("Request ID");
        reqIdCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getRequestId()));
        reqIdCol.setPrefWidth(120);

        TableColumn<AllocatedItem, String> reqCol = new TableColumn<>("Requester");
        reqCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getRequesterName()));
        reqCol.setPrefWidth(180);

        TableColumn<AllocatedItem, String> resCol = new TableColumn<>("Resource");
        resCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getResourceId()));
        resCol.setPrefWidth(120);

        TableColumn<AllocatedItem, String> demCol = new TableColumn<>("Demanded");
        demCol.setCellValueFactory(d -> new SimpleStringProperty(String.format(Locale.US, "%.1f", d.getValue().getRequestedQuantity())));
        demCol.setPrefWidth(90);

        TableColumn<AllocatedItem, String> allocCol = new TableColumn<>("Allocated");
        allocCol.setCellValueFactory(d -> new SimpleStringProperty(String.format(Locale.US, "%.1f", d.getValue().getAllocatedQuantity())));
        allocCol.setPrefWidth(90);

        TableColumn<AllocatedItem, String> statusCol = new TableColumn<>("Status");
        statusCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getStatus().name()));
        statusCol.setPrefWidth(140);

        TableColumn<AllocatedItem, String> expCol = new TableColumn<>("Explanation");
        expCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getExplanation()));
        expCol.setPrefWidth(350);

        table.getColumns().addAll(reqIdCol, reqCol, resCol, demCol, allocCol, statusCol, expCol);
        table.setPrefHeight(250);

        card.getChildren().addAll(cardTitle, table);
        return card;
    }

    /**
     * Applies a cell edit. Typing text that is not a number raises NumberFormatException;
     * a negative capacity or a priority outside 1-5 makes the model's setter throw
     * IllegalArgumentException. Catching IllegalArgumentException covers both, because
     * NumberFormatException is a subclass of it.
     */
    private void applyEdit(Runnable edit) {
        try {
            edit.run();
        } catch (NumberFormatException ex) {
            DialogHelper.showError("Invalid Number", "Please type a number", ex.getMessage());
        } catch (IllegalArgumentException ex) {
            DialogHelper.showError("Invalid Value", "Value rejected", ex.getMessage());
        }
    }

    /** "Move [amount] from [resource] to [resource]" controls under the resource table. */
    private HBox createTransferRow(TableView<Resource> resTable) {
        ComboBox<Resource> fromCombo = new ComboBox<>(sandboxResources);
        ComboBox<Resource> toCombo = new ComboBox<>(sandboxResources);
        javafx.util.StringConverter<Resource> byName = new javafx.util.StringConverter<>() {
            @Override
            public String toString(Resource r) {
                return (r == null) ? "" : r.getResourceId();
            }

            @Override
            public Resource fromString(String text) {
                return null; // combo boxes are not editable
            }
        };
        fromCombo.setConverter(byName);
        toCombo.setConverter(byName);
        fromCombo.setPromptText("From");
        toCombo.setPromptText("To");

        TextField amountField = new TextField();
        amountField.setPromptText("Amount");
        amountField.setPrefWidth(80);

        Button transferBtn = new Button("Move Capacity");
        transferBtn.getStyleClass().add("btn-secondary");
        transferBtn.setOnAction(e -> {
            try {
                double amount = Double.parseDouble(amountField.getText().trim());
                whatIfService.transferCapacity(fromCombo.getValue(), toCombo.getValue(), amount);
                resTable.refresh();
            } catch (NumberFormatException ex) {
                DialogHelper.showError("Invalid Amount", "Please type a number", "'" + amountField.getText() + "' is not a number.");
            } catch (IllegalArgumentException | com.resourceflow.exception.ResourceFlowException ex) {
                DialogHelper.showError("Transfer Rejected", "Capacity was not moved", ex.getMessage());
            }
        });

        HBox row = new HBox(8, new Label("Move"), amountField, fromCombo, new Label("→"), toCombo, transferBtn);
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

    private void exportScenarioFile() {
        if (activeScenario == null) return;
        syncScenarioFromControls();

        FileChooser chooser = scenarioFileChooser("Export Scenario");
        chooser.setInitialFileName(activeScenario.getName().replaceAll("[^A-Za-z0-9_-]+", "_") + ScenarioFileStore.FILE_EXTENSION);
        File file = chooser.showSaveDialog(getScene().getWindow());
        if (file == null) return; // user cancelled

        try {
            whatIfService.exportScenario(activeScenario, file.toPath());
            DialogHelper.showInfo("Scenario Exported", null, "Saved to " + file.getAbsolutePath());
        } catch (ScenarioFileException ex) {
            DialogHelper.showError("Export Failed", ex.getMessage(), describeCause(ex));
        }
    }

    private void importScenarioFile() {
        File file = scenarioFileChooser("Import Scenario").showOpenDialog(getScene().getWindow());
        if (file == null) return;

        try {
            Scenario loaded = whatIfService.importScenario(file.toPath());
            showScenario(loaded);
            scenarioNameField.setText(loaded.getName());
            strategyCombo.setValue(loaded.getStrategyType());
            DialogHelper.showInfo("Scenario Imported", null, "Loaded '" + loaded.getName() + "'. Press Simulate to run it.");
        } catch (ScenarioFileException ex) {
            DialogHelper.showError("Import Failed", ex.getMessage(), describeCause(ex));
        }
    }

    private FileChooser scenarioFileChooser(String title) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle(title);
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("ResourceFlow scenario", "*" + ScenarioFileStore.FILE_EXTENSION));
        return chooser;
    }

    /** Chained exceptions: show the original low-level cause the file store wrapped. */
    private static String describeCause(Exception ex) {
        Throwable cause = ex.getCause();
        return (cause == null) ? "" : "Cause: " + cause.getClass().getSimpleName() + " - " + cause.getMessage();
    }

    private void syncScenarioFromControls() {
        activeScenario.setName(scenarioNameField.getText().trim());
        activeScenario.setStrategyType(strategyCombo.getValue());
        activeScenario.setModifiedResources(new ArrayList<>(sandboxResources));
        activeScenario.setModifiedRequests(new ArrayList<>(sandboxRequests));
    }

    private void showScenario(Scenario scenario) {
        this.activeScenario = scenario;
        sandboxResources.setAll(scenario.getModifiedResources());
        sandboxRequests.setAll(scenario.getModifiedRequests());
        simulationResults.clear();
    }

    private void initSandbox(String name, StrategyType strategy) {
        TaskRunner.executeAsync(() -> whatIfService.createSandboxScenario(name, "Isolated simulation scenario", strategy),
                this::showScenario, null);
    }

    private void runSimulation() {
        if (activeScenario == null) return;
        syncScenarioFromControls();

        simulateBtn.setDisable(true);
        progressIndicator.setVisible(true);

        TaskRunner.executeAsync(() -> whatIfService.simulateScenario(activeScenario),
                result -> {
                    simulateBtn.setDisable(false);
                    progressIndicator.setVisible(false);

                    simulationResults.clear();
                    simulationResults.addAll(result.getHypotheticalPlan().getItems());

                    EvaluationMetrics hypM = result.getHypotheticalPlan().getMetrics();
                    scenarioUtilLabel.setText(String.format(Locale.US, "%.1f%%", hypM.getAggregateUtilisation()));
                    scenarioFulfilLabel.setText(String.format(Locale.US, "%.1f%%", hypM.getOverallFulfilment()));
                    scenarioFairnessLabel.setText(String.format(Locale.US, "%.3f", hypM.getJainsFairnessIndex()));

                    if (result.getBaselinePlan() != null) {
                        EvaluationMetrics baseM = result.getBaselinePlan().getMetrics();
                        baselineUtilLabel.setText(String.format(Locale.US, "%.1f%%", baseM.getAggregateUtilisation()));
                        baselineFulfilLabel.setText(String.format(Locale.US, "%.1f%%", baseM.getOverallFulfilment()));
                        baselineFairnessLabel.setText(String.format(Locale.US, "%.3f", baseM.getJainsFairnessIndex()));
                    }

                    if (result.getDiff() != null) {
                        diffSummaryLabel.setText(result.getDiff().getSummaryExplanation());
                    } else {
                        diffSummaryLabel.setText("Simulation finished. Feasible: " + result.getHypotheticalPlan().isFeasible());
                    }
                },
                error -> {
                    simulateBtn.setDisable(false);
                    progressIndicator.setVisible(false);
                    DialogHelper.showError("Simulation Error", "Sandbox Failure", error.getMessage());
                });
    }

    private void saveScenario() {
        if (activeScenario == null) return;
        activeScenario.setName(scenarioNameField.getText().trim());
        try {
            whatIfService.saveScenario(activeScenario);
            DialogHelper.showInfo("Scenario Saved", "Saved to Library",
                    "Scenario '" + activeScenario.getName() + "' saved successfully.");
        } catch (Exception ex) {
            DialogHelper.showError("Save Error", "Could not save scenario", ex.getMessage());
        }
    }
}
