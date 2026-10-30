package com.resourceflow.ui.views;

import com.resourceflow.io.ReportExporter;
import com.resourceflow.model.*;
import com.resourceflow.service.AllocationService;
import com.resourceflow.ui.util.DialogHelper;
import com.resourceflow.ui.util.TaskRunner;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;

import java.io.File;
import java.io.IOException;
import java.util.Locale;

/**
 * Allocation Workspace screen for configuring, executing, and reviewing allocation runs.
 */
public class AllocationWorkspaceView extends VBox {

    private final AllocationService allocationService;
    private final TableView<AllocatedItem> tableView;
    private final ObservableList<AllocatedItem> itemsList;

    private ComboBox<StrategyType> strategyCombo;
    private TextField scenarioNameField;
    private CheckBox allOrNothingCheck;
    private CheckBox minThresholdCheck;
    private CheckBox respectInactiveCheck;
    private Button executeBtn;
    private Button exportCsvBtn;
    private ProgressIndicator progressIndicator;
    private AllocationRun lastRun;

    private Label utilLabel;
    private Label fulfilLabel;
    private Label fairnessLabel;
    private Label statusSummaryLabel;
    private Label feasibilityBadge;

    public AllocationWorkspaceView(AllocationService allocationService) {
        this.allocationService = allocationService;
        this.itemsList = FXCollections.observableArrayList();
        this.tableView = new TableView<>(itemsList);

        setSpacing(20);
        getStyleClass().add("content-area");
        setPadding(new Insets(24));

        // Header
        VBox headerBox = new VBox(4);
        Label title = new Label("Allocation Execution Workspace");
        title.getStyleClass().add("view-header-title");
        Label subtitle = new Label("Select an allocation strategy, tune constraints, and generate a validated allocation plan.");
        subtitle.getStyleClass().add("view-header-subtitle");
        headerBox.getChildren().addAll(title, subtitle);

        // Control Panel
        VBox controlPanel = createControlPanel();

        // Results Summary Banner
        HBox resultsBanner = createResultsBanner();

        // Table
        setupTable();
        VBox.setVgrow(tableView, Priority.ALWAYS);

        getChildren().addAll(headerBox, controlPanel, resultsBanner, tableView);
    }

    private VBox createControlPanel() {
        VBox card = new VBox(16);
        card.getStyleClass().add("card");

        HBox row1 = new HBox(16);
        row1.setAlignment(Pos.CENTER_LEFT);

        strategyCombo = new ComboBox<>(FXCollections.observableArrayList(StrategyType.values()));
        strategyCombo.setValue(StrategyType.PRIORITY_BASED);
        strategyCombo.setPrefWidth(220);

        scenarioNameField = new TextField();
        scenarioNameField.setPromptText("Optional, e.g. Q4 Baseline Run");
        scenarioNameField.setTooltip(new Tooltip("Label for this run in History & Audit. Leave empty to use \"<Strategy> Execution\"."));
        scenarioNameField.setPrefWidth(260);

        executeBtn = new Button("⚡ Execute Allocation Plan");
        executeBtn.getStyleClass().add("btn-primary");
        executeBtn.setOnAction(e -> handleExecute());

        exportCsvBtn = new Button("⬇ Export CSV");
        exportCsvBtn.getStyleClass().add("btn-secondary");
        exportCsvBtn.setDisable(true); // enabled once a plan exists
        exportCsvBtn.setOnAction(e -> handleExportCsv());

        progressIndicator = new ProgressIndicator();
        progressIndicator.setPrefSize(24, 24);
        progressIndicator.setVisible(false);

        // Labels and buttons keep their full width; only the name field shrinks on narrow windows
        Label strategyLabel = new Label("Strategy:");
        Label nameLabel = new Label("Run name:");
        for (Region fixed : new Region[]{strategyLabel, nameLabel, executeBtn, exportCsvBtn}) {
            fixed.setMinWidth(Region.USE_PREF_SIZE);
        }
        strategyCombo.setMinWidth(180); // fits "Priority-Based"
        scenarioNameField.setMinWidth(120);
        HBox.setHgrow(scenarioNameField, Priority.SOMETIMES);

        row1.getChildren().addAll(
                strategyLabel, strategyCombo,
                nameLabel, scenarioNameField,
                executeBtn, exportCsvBtn, progressIndicator
        );

        HBox row2 = new HBox(24);
        row2.setAlignment(Pos.CENTER_LEFT);

        allOrNothingCheck = new CheckBox("Enforce All-or-Nothing (Atomic)");
        allOrNothingCheck.setSelected(true);

        minThresholdCheck = new CheckBox("Enforce Minimum Thresholds");
        minThresholdCheck.setSelected(true);

        respectInactiveCheck = new CheckBox("Respect Inactive Status");
        respectInactiveCheck.setSelected(true);

        row2.getChildren().addAll(allOrNothingCheck, minThresholdCheck, respectInactiveCheck);

        card.getChildren().addAll(row1, row2);
        return card;
    }

    private HBox createResultsBanner() {
        HBox banner = new HBox(32);
        banner.getStyleClass().add("card");
        banner.setAlignment(Pos.CENTER_LEFT);
        banner.setPadding(new Insets(14, 20, 14, 20));

        utilLabel = new Label("Utilisation: —");
        utilLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #f8fafc;");

        fulfilLabel = new Label("Fulfilment: —");
        fulfilLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #f8fafc;");

        fairnessLabel = new Label("Jain's Fairness: —");
        fairnessLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #f8fafc;");

        statusSummaryLabel = new Label("Satisfied: — | Partial: — | Denied: —");
        statusSummaryLabel.setStyle("-fx-text-fill: #cbd5e1;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        feasibilityBadge = new Label("STATUS: READY");
        feasibilityBadge.getStyleClass().add("badge-neutral");

        banner.getChildren().addAll(utilLabel, fulfilLabel, fairnessLabel, statusSummaryLabel, spacer, feasibilityBadge);
        return banner;
    }

    private void setupTable() {
        TableColumn<AllocatedItem, String> reqIdCol = new TableColumn<>("Request ID");
        reqIdCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getRequestId()));
        reqIdCol.setPrefWidth(110);

        TableColumn<AllocatedItem, String> nameCol = new TableColumn<>("Requester");
        nameCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getRequesterName()));
        nameCol.setPrefWidth(180);

        TableColumn<AllocatedItem, String> resCol = new TableColumn<>("Resource");
        resCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getResourceId()));
        resCol.setPrefWidth(120);

        TableColumn<AllocatedItem, String> demCol = new TableColumn<>("Demanded");
        demCol.setCellValueFactory(d -> new SimpleStringProperty(String.format(Locale.US, "%.1f", d.getValue().getRequestedQuantity())));
        demCol.setPrefWidth(90);

        TableColumn<AllocatedItem, String> allocCol = new TableColumn<>("Allocated");
        allocCol.setCellValueFactory(d -> new SimpleStringProperty(String.format(Locale.US, "%.1f", d.getValue().getAllocatedQuantity())));
        allocCol.setPrefWidth(90);

        TableColumn<AllocatedItem, String> ratioCol = new TableColumn<>("Ratio");
        ratioCol.setCellValueFactory(d -> new SimpleStringProperty(String.format(Locale.US, "%.1f%%", d.getValue().getFulfilmentRatio() * 100.0)));
        ratioCol.setPrefWidth(80);

        TableColumn<AllocatedItem, String> statusCol = new TableColumn<>("Outcome");
        statusCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getStatus().name()));
        statusCol.setPrefWidth(130);
        statusCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    Label badge = new Label(item);
                    switch (item) {
                        case "FULLY_SATISFIED" -> badge.getStyleClass().add("badge-success");
                        case "PARTIALLY_SATISFIED" -> badge.getStyleClass().add("badge-warning");
                        default -> badge.getStyleClass().add("badge-danger");
                    }
                    setGraphic(badge);
                    setText(null);
                }
            }
        });

        TableColumn<AllocatedItem, String> explCol = new TableColumn<>("Algorithmic Explanation / Rationale");
        explCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getExplanation()));
        explCol.setPrefWidth(380);

        tableView.getColumns().addAll(reqIdCol, nameCol, resCol, demCol, allocCol, ratioCol, statusCol, explCol);
        tableView.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
    }

    private void handleExecute() {
        StrategyType selectedStrategy = strategyCombo.getValue();
        String runName = scenarioNameField.getText().trim();
        if (runName.isEmpty()) {
            runName = selectedStrategy.getDisplayName() + " Execution";
        }

        ConstraintConfig config = new ConstraintConfig();
        config.setEnforceAllOrNothing(allOrNothingCheck.isSelected());
        config.setEnforceMinimumAcceptable(minThresholdCheck.isSelected());
        config.setRespectInactiveStatus(respectInactiveCheck.isSelected());

        executeBtn.setDisable(true);
        progressIndicator.setVisible(true);

        String finalRunName = runName;
        TaskRunner.executeAsync(() -> allocationService.runAllocation(selectedStrategy, config, finalRunName),
                run -> {
                    executeBtn.setDisable(false);
                    progressIndicator.setVisible(false);
                    lastRun = run;
                    exportCsvBtn.setDisable(false);

                    itemsList.clear();
                    itemsList.addAll(run.getAllocations());

                    EvaluationMetrics m = run.getMetrics();
                    utilLabel.setText(String.format(Locale.US, "Utilisation: %.1f%%", m.getAggregateUtilisation()));
                    fulfilLabel.setText(String.format(Locale.US, "Fulfilment: %.1f%%", m.getOverallFulfilment()));
                    fairnessLabel.setText(String.format(Locale.US, "Jain's Fairness: %.3f", m.getJainsFairnessIndex()));
                    statusSummaryLabel.setText(String.format(Locale.US, "Satisfied: %d | Partial: %d | Denied: %d",
                            m.getFullySatisfiedCount(), m.getPartiallySatisfiedCount(), m.getUnsatisfiedCount()));

                    feasibilityBadge.setText(m.isFeasible() ? "PLAN FEASIBLE" : "VIOLATIONS DETECTED");
                    feasibilityBadge.getStyleClass().clear();
                    feasibilityBadge.getStyleClass().add(m.isFeasible() ? "badge-success" : "badge-danger");

                    DialogHelper.showInfo("Allocation Complete", "Plan Generated & Persisted",
                            String.format(Locale.US, "Plan successfully executed using %s strategy.\nTotal Utilisation: %.1f%%\nJain's Fairness Index: %.3f",
                                    selectedStrategy.getDisplayName(), m.getAggregateUtilisation(), m.getJainsFairnessIndex()));
                },
                error -> {
                    executeBtn.setDisable(false);
                    progressIndicator.setVisible(false);
                    DialogHelper.showError("Allocation Failed", "Engine Execution Error", error.getMessage());
                });
    }

    private void handleExportCsv() {
        if (lastRun == null) return;

        FileChooser chooser = new FileChooser();
        chooser.setTitle("Export Allocation Report");
        chooser.setInitialFileName("allocation-report.csv");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV file", "*.csv"));
        File file = chooser.showSaveDialog(getScene().getWindow());
        if (file == null) return; // user cancelled

        try {
            ReportExporter.exportCsv(lastRun.getScenarioName(), lastRun.getAllocations(), lastRun.getMetrics(), file.toPath());
            DialogHelper.showInfo("Report Exported", null, "Saved to " + file.getAbsolutePath());
        } catch (IOException ex) {
            DialogHelper.showError("Export Failed", "Could not write the CSV file", ex.getMessage());
        }
    }
}
