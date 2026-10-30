package com.resourceflow.ui.views;

import com.resourceflow.model.*;
import com.resourceflow.service.AllocationService;
import com.resourceflow.service.HistoryService;
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
import java.util.Optional;

/**
 * Decision Explanations screen providing transparent, causal drill-downs
 * for every individual allocation decision made by the system.
 */
public class DecisionExplanationsView extends VBox {

    private final AllocationService allocationService;
    private final HistoryService historyService;

    private final TableView<AllocatedItem> tableView;
    private final ObservableList<AllocatedItem> itemsList;

    private Label selectedReqLabel;
    private Label demandLabel;
    private Label allocatedLabel;
    private Label unmetLabel;
    private Label fulfilmentRatioLabel;
    private Label priorityLabel;
    private Label statusBadge;
    private FlowPane constraintsPane;
    private TextArea rationaleArea;

    public DecisionExplanationsView(AllocationService allocationService, HistoryService historyService) {
        this.allocationService = allocationService;
        this.historyService = historyService;
        this.itemsList = FXCollections.observableArrayList();
        this.tableView = new TableView<>(itemsList);

        setSpacing(20);
        getStyleClass().add("content-area");
        setPadding(new Insets(24));

        // Header
        VBox headerBox = new VBox(4);
        Label title = new Label("Algorithmic Decision Explanations");
        title.getStyleClass().add("view-header-title");
        Label subtitle = new Label("Inspect the mathematical and constraint rationale behind every individual request allocation.");
        subtitle.getStyleClass().add("view-header-subtitle");
        headerBox.getChildren().addAll(title, subtitle);

        // Split Layout: Left Table, Right Inspector
        HBox mainSplit = new HBox(20);
        VBox.setVgrow(mainSplit, Priority.ALWAYS);

        // Left Table
        VBox tableContainer = new VBox(8);
        HBox.setHgrow(tableContainer, Priority.ALWAYS);
        setupTable();
        VBox.setVgrow(tableView, Priority.ALWAYS);
        tableContainer.getChildren().add(tableView);

        // Right Inspector Card
        VBox inspectorCard = createInspectorCard();
        inspectorCard.setPrefWidth(420);
        inspectorCard.setMinWidth(360);

        mainSplit.getChildren().addAll(tableContainer, inspectorCard);

        getChildren().addAll(headerBox, mainSplit);

        refreshData();
    }

    private void setupTable() {
        TableColumn<AllocatedItem, String> idCol = new TableColumn<>("Request ID");
        idCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getRequestId()));
        idCol.setPrefWidth(110);

        TableColumn<AllocatedItem, String> reqCol = new TableColumn<>("Requester");
        reqCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getRequesterName()));
        reqCol.setPrefWidth(180);

        TableColumn<AllocatedItem, String> resCol = new TableColumn<>("Resource");
        resCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getResourceId()));
        resCol.setPrefWidth(110);

        TableColumn<AllocatedItem, String> statusCol = new TableColumn<>("Status");
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

        tableView.getColumns().addAll(idCol, reqCol, resCol, statusCol);
        tableView.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        tableView.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                displayItemExplanation(newVal);
            }
        });
    }

    private VBox createInspectorCard() {
        VBox card = new VBox(14);
        card.getStyleClass().add("card");

        Label cardTitle = new Label("Decision Inspector");
        cardTitle.getStyleClass().add("metric-title");

        selectedReqLabel = new Label("Select a request on the left");
        selectedReqLabel.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: #f8fafc;");

        statusBadge = new Label("—");
        statusBadge.getStyleClass().add("badge-neutral");

        HBox headerRow = new HBox(12, selectedReqLabel, statusBadge);
        headerRow.setAlignment(Pos.CENTER_LEFT);

        GridPane grid = new GridPane();
        grid.setHgap(16);
        grid.setVgap(8);

        demandLabel = new Label("—");
        allocatedLabel = new Label("—");
        unmetLabel = new Label("—");
        fulfilmentRatioLabel = new Label("—");
        priorityLabel = new Label("—");

        grid.add(new Label("Demanded:"), 0, 0);
        grid.add(demandLabel, 1, 0);

        grid.add(new Label("Allocated:"), 0, 1);
        grid.add(allocatedLabel, 1, 1);

        grid.add(new Label("Unmet Demand:"), 0, 2);
        grid.add(unmetLabel, 1, 2);

        grid.add(new Label("Fulfilment:"), 0, 3);
        grid.add(fulfilmentRatioLabel, 1, 3);

        grid.add(new Label("Priority:"), 0, 4);
        grid.add(priorityLabel, 1, 4);

        Label constraintsTitle = new Label("Contributing Constraints:");
        constraintsTitle.setStyle("-fx-text-fill: #94a3b8; -fx-font-weight: 600; -fx-font-size: 12px;");

        constraintsPane = new FlowPane(8, 8);

        Label rationaleTitle = new Label("Algorithmic Explanation:");
        rationaleTitle.setStyle("-fx-text-fill: #94a3b8; -fx-font-weight: 600; -fx-font-size: 12px;");

        rationaleArea = new TextArea();
        rationaleArea.setEditable(false);
        rationaleArea.setWrapText(true);
        rationaleArea.setPrefRowCount(6);

        card.getChildren().addAll(cardTitle, headerRow, new Separator(), grid, new Separator(),
                constraintsTitle, constraintsPane, rationaleTitle, rationaleArea);
        return card;
    }

    private void displayItemExplanation(AllocatedItem item) {
        selectedReqLabel.setText(item.getRequestId() + " (" + item.getRequesterName() + ")");

        statusBadge.setText(item.getStatus().name());
        statusBadge.getStyleClass().clear();
        switch (item.getStatus()) {
            case FULLY_SATISFIED -> statusBadge.getStyleClass().add("badge-success");
            case PARTIALLY_SATISFIED -> statusBadge.getStyleClass().add("badge-warning");
            default -> statusBadge.getStyleClass().add("badge-danger");
        }

        demandLabel.setText(String.format(Locale.US, "%.1f units", item.getRequestedQuantity()));
        allocatedLabel.setText(String.format(Locale.US, "%.1f units", item.getAllocatedQuantity()));
        unmetLabel.setText(String.format(Locale.US, "%.1f units", item.getUnmetQuantity()));
        fulfilmentRatioLabel.setText(String.format(Locale.US, "%.1f%%", item.getFulfilmentRatio() * 100.0));

        constraintsPane.getChildren().clear();
        Label resTag = new Label("Target: " + item.getResourceId());
        resTag.getStyleClass().add("badge-neutral");
        constraintsPane.getChildren().add(resTag);

        rationaleArea.setText(item.getExplanation());
    }

    public void refreshData() {
        TaskRunner.executeAsync(() -> {
            Optional<AllocationRun> latestRun = historyService.getLatestRun();
            return latestRun.map(AllocationRun::getAllocations).orElse(List.of());
        }, items -> {
            itemsList.clear();
            itemsList.addAll(items);
            if (!items.isEmpty()) {
                tableView.getSelectionModel().selectFirst();
            }
        }, null);
    }
}
