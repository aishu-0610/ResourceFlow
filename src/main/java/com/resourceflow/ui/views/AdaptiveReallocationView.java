package com.resourceflow.ui.views;

import com.resourceflow.engine.AllocationEngine;
import com.resourceflow.model.ConstraintConfig;
import com.resourceflow.model.ReallocationDiff;
import com.resourceflow.model.StrategyType;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Adaptive Reallocation screen providing impact analysis and granular delta tracking
 * whenever capacities, demands, or constraints change.
 */
public class AdaptiveReallocationView extends VBox {

    private final AllocationService allocationService;
    private final TableView<ReallocationDiff.DiffItem> tableView;
    private final ObservableList<ReallocationDiff.DiffItem> diffList;

    private ComboBox<StrategyType> strategyCombo;
    private Button reallocateBtn;
    private ProgressIndicator progressIndicator;

    private Label reassignedLabel;
    private Label improvedCountLabel;
    private Label degradedCountLabel;
    private Label droppedCountLabel;
    private Label unchangedCountLabel;
    private Label summaryExplanationLabel;

    public AdaptiveReallocationView(AllocationService allocationService) {
        this.allocationService = allocationService;
        this.diffList = FXCollections.observableArrayList();
        this.tableView = new TableView<>(diffList);

        setSpacing(20);
        getStyleClass().add("content-area");
        setPadding(new Insets(24));

        // Header
        VBox headerBox = new VBox(4);
        Label title = new Label("Adaptive Reallocation & Impact Diff");
        title.getStyleClass().add("view-header-title");
        Label subtitle = new Label("Recalculate allocations dynamically when inputs change, maintaining historical integrity and itemized impact deltas.");
        subtitle.getStyleClass().add("view-header-subtitle");
        headerBox.getChildren().addAll(title, subtitle);

        // Toolbar
        HBox toolbar = createToolbar();

        // Metrics Summary Banner
        VBox summaryBanner = createSummaryBanner();

        // Table
        setupTable();
        VBox.setVgrow(tableView, Priority.ALWAYS);

        getChildren().addAll(headerBox, toolbar, summaryBanner, tableView);
    }

    private HBox createToolbar() {
        HBox toolbar = new HBox(16);
        toolbar.setAlignment(Pos.CENTER_LEFT);

        strategyCombo = new ComboBox<>(FXCollections.observableArrayList(StrategyType.values()));
        strategyCombo.setValue(StrategyType.PRIORITY_BASED);
        strategyCombo.setPrefWidth(220);

        reallocateBtn = new Button("🔄 Recalculate Adaptive Reallocation");
        reallocateBtn.getStyleClass().add("btn-primary");
        reallocateBtn.setOnAction(e -> handleReallocate());

        progressIndicator = new ProgressIndicator();
        progressIndicator.setPrefSize(24, 24);
        progressIndicator.setVisible(false);

        toolbar.getChildren().addAll(new Label("Strategy:"), strategyCombo, reallocateBtn, progressIndicator);
        return toolbar;
    }

    private VBox createSummaryBanner() {
        VBox card = new VBox(12);
        card.getStyleClass().add("card");

        HBox kpis = new HBox(32);
        kpis.setAlignment(Pos.CENTER_LEFT);

        reassignedLabel = new Label("Reassigned: 0.0 units");
        reassignedLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #f8fafc; -fx-font-size: 14px;");

        improvedCountLabel = new Label("Improved: 0");
        improvedCountLabel.getStyleClass().add("badge-success");

        degradedCountLabel = new Label("Degraded: 0");
        degradedCountLabel.getStyleClass().add("badge-warning");

        droppedCountLabel = new Label("Dropped to Zero: 0");
        droppedCountLabel.getStyleClass().add("badge-danger");

        unchangedCountLabel = new Label("Unchanged: 0");
        unchangedCountLabel.getStyleClass().add("badge-neutral");

        kpis.getChildren().addAll(reassignedLabel, improvedCountLabel, degradedCountLabel, droppedCountLabel, unchangedCountLabel);

        summaryExplanationLabel = new Label("Run an adaptive reallocation to inspect impact deltas against the prior baseline run.");
        summaryExplanationLabel.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 12px;");

        card.getChildren().addAll(kpis, summaryExplanationLabel);
        return card;
    }

    private void setupTable() {
        TableColumn<ReallocationDiff.DiffItem, String> reqIdCol = new TableColumn<>("Request ID");
        reqIdCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getRequestId()));
        reqIdCol.setPrefWidth(110);

        TableColumn<ReallocationDiff.DiffItem, String> reqCol = new TableColumn<>("Requester");
        reqCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getRequesterName()));
        reqCol.setPrefWidth(180);

        TableColumn<ReallocationDiff.DiffItem, String> resCol = new TableColumn<>("Resource");
        resCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getResourceId()));
        resCol.setPrefWidth(120);

        TableColumn<ReallocationDiff.DiffItem, String> demCol = new TableColumn<>("Demanded");
        demCol.setCellValueFactory(d -> new SimpleStringProperty(String.format(Locale.US, "%.1f", d.getValue().getRequestedQuantity())));
        demCol.setPrefWidth(90);

        TableColumn<ReallocationDiff.DiffItem, String> prevCol = new TableColumn<>("Previous");
        prevCol.setCellValueFactory(d -> new SimpleStringProperty(String.format(Locale.US, "%.1f", d.getValue().getPreviousAllocation())));
        prevCol.setPrefWidth(90);

        TableColumn<ReallocationDiff.DiffItem, String> newCol = new TableColumn<>("New");
        newCol.setCellValueFactory(d -> new SimpleStringProperty(String.format(Locale.US, "%.1f", d.getValue().getNewAllocation())));
        newCol.setPrefWidth(90);

        TableColumn<ReallocationDiff.DiffItem, String> deltaCol = new TableColumn<>("Impact Delta (Δ)");
        deltaCol.setCellValueFactory(d -> {
            double delta = d.getValue().getDelta();
            return new SimpleStringProperty(delta > 0 ? String.format(Locale.US, "+%.1f", delta) : String.format(Locale.US, "%.1f", delta));
        });
        deltaCol.setPrefWidth(120);
        deltaCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    Label badge = new Label(item);
                    if (item.startsWith("+")) {
                        badge.getStyleClass().add("badge-success");
                    } else if (item.startsWith("-")) {
                        badge.getStyleClass().add("badge-warning");
                    } else {
                        badge.getStyleClass().add("badge-neutral");
                    }
                    setGraphic(badge);
                    setText(null);
                }
            }
        });

        TableColumn<ReallocationDiff.DiffItem, String> reasonCol = new TableColumn<>("Reason for Change");
        reasonCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getReason()));
        reasonCol.setPrefWidth(350);

        tableView.getColumns().addAll(reqIdCol, reqCol, resCol, demCol, prevCol, newCol, deltaCol, reasonCol);
        tableView.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
    }

    private void handleReallocate() {
        StrategyType strategy = strategyCombo.getValue();
        reallocateBtn.setDisable(true);
        progressIndicator.setVisible(true);

        TaskRunner.executeAsync(() -> allocationService.reallocateAdaptively(strategy, ConstraintConfig.defaultConfig()),
                result -> {
                    reallocateBtn.setDisable(false);
                    progressIndicator.setVisible(false);

                    ReallocationDiff diff = result.getDiff();
                    List<ReallocationDiff.DiffItem> allItems = new ArrayList<>();
                    allItems.addAll(diff.getImprovedRequests());
                    allItems.addAll(diff.getDegradedRequests());
                    allItems.addAll(diff.getDroppedRequests());
                    allItems.addAll(diff.getUnchangedRequests());

                    diffList.clear();
                    diffList.addAll(allItems);

                    reassignedLabel.setText(String.format(Locale.US, "Reassigned: %.1f units", diff.getCapacityReassigned()));
                    improvedCountLabel.setText("Improved: " + diff.getImprovedRequests().size());
                    degradedCountLabel.setText("Degraded: " + diff.getDegradedRequests().size());
                    droppedCountLabel.setText("Dropped to Zero: " + diff.getDroppedRequests().size());
                    unchangedCountLabel.setText("Unchanged: " + diff.getUnchangedRequests().size());
                    summaryExplanationLabel.setText(diff.getSummaryExplanation());

                    DialogHelper.showInfo("Adaptive Reallocation Complete", "Plan Recalculated", diff.getSummaryExplanation());
                },
                error -> {
                    reallocateBtn.setDisable(false);
                    progressIndicator.setVisible(false);
                    DialogHelper.showError("Reallocation Failed", "Engine Error", error.getMessage());
                });
    }
}
