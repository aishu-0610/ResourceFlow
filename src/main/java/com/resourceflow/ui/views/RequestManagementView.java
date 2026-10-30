package com.resourceflow.ui.views;

import com.resourceflow.model.AllocationRequest;
import com.resourceflow.model.RequestStatus;
import com.resourceflow.model.Resource;
import com.resourceflow.service.RequestService;
import com.resourceflow.service.ResourceService;
import com.resourceflow.ui.util.DialogHelper;
import com.resourceflow.ui.util.TaskRunner;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Request Management screen for submitting, managing, and tracking resource demands.
 */
public class RequestManagementView extends VBox {

    private final RequestService requestService;
    private final ResourceService resourceService;
    private final TableView<AllocationRequest> tableView;
    private final ObservableList<AllocationRequest> requestList;
    private TextField searchField;

    public RequestManagementView(RequestService requestService, ResourceService resourceService) {
        this.requestService = requestService;
        this.resourceService = resourceService;
        this.requestList = FXCollections.observableArrayList();
        this.tableView = new TableView<>(requestList);

        setSpacing(20);
        getStyleClass().add("content-area");
        setPadding(new Insets(24));

        // Header
        VBox headerBox = new VBox(4);
        Label title = new Label("Allocation Request Management");
        title.getStyleClass().add("view-header-title");
        Label subtitle = new Label("Submit, configure priorities, and inspect competing resource demands.");
        subtitle.getStyleClass().add("view-header-subtitle");
        headerBox.getChildren().addAll(title, subtitle);

        // Action Toolbar
        HBox toolbar = createToolbar();

        // Table
        setupTable();
        VBox.setVgrow(tableView, Priority.ALWAYS);

        getChildren().addAll(headerBox, toolbar, tableView);
        refreshData();
    }

    private HBox createToolbar() {
        HBox toolbar = new HBox(12);
        toolbar.setAlignment(Pos.CENTER_LEFT);

        searchField = new TextField();
        searchField.setPromptText("Filter requests by requester, resource, or ID...");
        searchField.setPrefWidth(320);
        searchField.textProperty().addListener((obs, oldVal, newVal) -> filterTable(newVal));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button newBtn = new Button("+ Submit Request");
        newBtn.getStyleClass().add("btn-primary");
        newBtn.setOnAction(e -> showRequestDialog(null));

        Button editBtn = new Button("Edit");
        editBtn.getStyleClass().add("btn-secondary");
        editBtn.setOnAction(e -> {
            AllocationRequest selected = tableView.getSelectionModel().getSelectedItem();
            if (selected != null) {
                showRequestDialog(selected);
            } else {
                DialogHelper.showWarning("Selection Required", null, "Please select a request to edit.");
            }
        });

        Button cancelBtn = new Button("Cancel Request");
        cancelBtn.getStyleClass().add("btn-secondary");
        cancelBtn.setOnAction(e -> {
            AllocationRequest selected = tableView.getSelectionModel().getSelectedItem();
            if (selected != null) {
                requestService.cancelRequest(selected.getRequestId());
                refreshData();
            }
        });

        Button deleteBtn = new Button("Delete");
        deleteBtn.getStyleClass().add("btn-danger");
        deleteBtn.setOnAction(e -> handleDelete());

        toolbar.getChildren().addAll(searchField, spacer, newBtn, editBtn, cancelBtn, deleteBtn);
        return toolbar;
    }

    private void setupTable() {
        TableColumn<AllocationRequest, String> idCol = new TableColumn<>("Request ID");
        idCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getRequestId()));
        idCol.setPrefWidth(120);

        TableColumn<AllocationRequest, String> reqCol = new TableColumn<>("Requester");
        reqCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getRequesterName()));
        reqCol.setPrefWidth(180);

        TableColumn<AllocationRequest, String> resCol = new TableColumn<>("Target Resource");
        resCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getResourceId()));
        resCol.setPrefWidth(130);

        TableColumn<AllocationRequest, String> qtyCol = new TableColumn<>("Demand");
        qtyCol.setCellValueFactory(d -> new SimpleStringProperty(String.format(Locale.US, "%.1f", d.getValue().getRequestedQuantity())));
        qtyCol.setPrefWidth(90);

        TableColumn<AllocationRequest, String> prioCol = new TableColumn<>("Priority");
        prioCol.setCellValueFactory(d -> new SimpleStringProperty("P" + d.getValue().getPriority()));
        prioCol.setPrefWidth(80);
        prioCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    Label badge = new Label(item);
                    if (item.contains("5") || item.contains("4")) {
                        badge.getStyleClass().add("badge-warning");
                    } else {
                        badge.getStyleClass().add("badge-neutral");
                    }
                    setGraphic(badge);
                    setText(null);
                }
            }
        });

        TableColumn<AllocationRequest, String> minCol = new TableColumn<>("Min Threshold");
        minCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getMinAcceptableQuantity() > 0 ?
                String.format(Locale.US, "%.1f", d.getValue().getMinAcceptableQuantity()) : "—"));
        minCol.setPrefWidth(100);

        TableColumn<AllocationRequest, String> atomicCol = new TableColumn<>("Atomic (All/None)");
        atomicCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().isAllOrNothing() ? "YES" : "NO"));
        atomicCol.setPrefWidth(110);

        TableColumn<AllocationRequest, String> statusCol = new TableColumn<>("Status");
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
                        case "ALLOCATED" -> badge.getStyleClass().add("badge-success");
                        case "PARTIALLY_ALLOCATED" -> badge.getStyleClass().add("badge-warning");
                        case "REJECTED", "CANCELLED" -> badge.getStyleClass().add("badge-danger");
                        default -> badge.getStyleClass().add("badge-neutral");
                    }
                    setGraphic(badge);
                    setText(null);
                }
            }
        });

        TableColumn<AllocationRequest, String> dateCol = new TableColumn<>("Submitted");
        dateCol.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().getSubmissionDate() != null ?
                        d.getValue().getSubmissionDate().toString().replace("T", " ").substring(0, 16) : ""));
        dateCol.setPrefWidth(140);

        tableView.getColumns().addAll(idCol, reqCol, resCol, qtyCol, prioCol, minCol, atomicCol, statusCol, dateCol);
        tableView.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
    }

    private void handleDelete() {
        AllocationRequest selected = tableView.getSelectionModel().getSelectedItem();
        if (selected == null) {
            DialogHelper.showWarning("Selection Required", null, "Please select a request to delete.");
            return;
        }

        boolean confirm = DialogHelper.showConfirmation("Delete Request",
                "Delete request " + selected.getRequestId() + "?",
                "This action cannot be undone.");

        if (confirm) {
            try {
                requestService.deleteRequest(selected.getRequestId());
                refreshData();
            } catch (Exception ex) {
                DialogHelper.showError("Error", "Failed to delete request", ex.getMessage());
            }
        }
    }

    private void showRequestDialog(AllocationRequest existing) {
        Dialog<AllocationRequest> dialog = new Dialog<>();
        dialog.setTitle(existing == null ? "Submit Allocation Request" : "Edit Request: " + existing.getRequestId());
        dialog.setHeaderText("Define requested resource, demanded quantity, and constraint thresholds.");

        ButtonType saveBtnType = new ButtonType("Submit", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveBtnType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(12);
        grid.setPadding(new Insets(20, 20, 10, 20));

        TextField idField = new TextField();
        idField.setPromptText("e.g., REQ-001");
        if (existing != null) {
            idField.setText(existing.getRequestId());
            idField.setDisable(true);
        }

        TextField requesterField = new TextField();
        requesterField.setPromptText("e.g., Dr. Alice (Vision Lab)");
        if (existing != null) requesterField.setText(existing.getRequesterName());

        ComboBox<String> resCombo = new ComboBox<>();
        List<Resource> resources = resourceService.getAllResources();
        for (Resource r : resources) {
            resCombo.getItems().add(r.getResourceId());
        }
        if (existing != null) {
            resCombo.setValue(existing.getResourceId());
        } else if (!resources.isEmpty()) {
            resCombo.setValue(resources.getFirst().getResourceId());
        }

        TextField qtyField = new TextField();
        qtyField.setPromptText("e.g., 40.0");
        if (existing != null) qtyField.setText(String.valueOf(existing.getRequestedQuantity()));

        ComboBox<Integer> prioCombo = new ComboBox<>(FXCollections.observableArrayList(5, 4, 3, 2, 1));
        prioCombo.setValue(existing != null ? existing.getPriority() : 3);

        TextField minField = new TextField();
        minField.setPromptText("Optional minimum threshold (e.g., 10.0)");
        if (existing != null) minField.setText(String.valueOf(existing.getMinAcceptableQuantity()));

        CheckBox atomicCheck = new CheckBox("All-or-Nothing (Atomic Allocation)");
        if (existing != null) atomicCheck.setSelected(existing.isAllOrNothing());

        TextArea notesArea = new TextArea();
        notesArea.setPromptText("Optional notes or project description");
        notesArea.setPrefRowCount(2);
        if (existing != null) notesArea.setText(existing.getNotes());

        grid.add(new Label("Request ID:"), 0, 0);
        grid.add(idField, 1, 0);
        grid.add(new Label("Requester Name:"), 0, 1);
        grid.add(requesterField, 1, 1);
        grid.add(new Label("Target Resource:"), 0, 2);
        grid.add(resCombo, 1, 2);
        grid.add(new Label("Requested Quantity:"), 0, 3);
        grid.add(qtyField, 1, 3);
        grid.add(new Label("Priority Level:"), 0, 4);
        grid.add(prioCombo, 1, 4);
        grid.add(new Label("Min Acceptable Quantity:"), 0, 5);
        grid.add(minField, 1, 5);
        grid.add(new Label("Atomicity:"), 0, 6);
        grid.add(atomicCheck, 1, 6);
        grid.add(new Label("Notes:"), 0, 7);
        grid.add(notesArea, 1, 7);

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == saveBtnType) {
                try {
                    String id = idField.getText().trim();
                    String reqName = requesterField.getText().trim();
                    String resId = resCombo.getValue();
                    double qty = Double.parseDouble(qtyField.getText().trim());
                    int prio = prioCombo.getValue();
                    double minQty = minField.getText().trim().isEmpty() ? 0.0 : Double.parseDouble(minField.getText().trim());
                    boolean atomic = atomicCheck.isSelected();
                    String notes = notesArea.getText().trim();

                    if (existing == null) {
                        return new AllocationRequest(id, reqName, resId, qty, prio, minQty, atomic,
                                LocalDateTime.now(), null, RequestStatus.PENDING, notes);
                    } else {
                        existing.setRequesterName(reqName);
                        existing.setResourceId(resId);
                        existing.setRequestedQuantity(qty);
                        existing.setPriority(prio);
                        existing.setMinAcceptableQuantity(minQty);
                        existing.setAllOrNothing(atomic);
                        existing.setNotes(notes);
                        return existing;
                    }
                } catch (NumberFormatException ex) {
                    DialogHelper.showError("Validation Error", "Invalid Number", "Quantities must be valid numbers.");
                    return null;
                }
            }
            return null;
        });

        Optional<AllocationRequest> result = dialog.showAndWait();
        result.ifPresent(req -> {
            try {
                if (existing == null) {
                    requestService.createRequest(req);
                } else {
                    requestService.updateRequest(req);
                }
                refreshData();
            } catch (Exception ex) {
                DialogHelper.showError("Error", "Failed to save request", ex.getMessage());
            }
        });
    }

    private void filterTable(String query) {
        if (query == null || query.trim().isEmpty()) {
            refreshData();
            return;
        }
        String lower = query.toLowerCase();
        TaskRunner.executeAsync(() -> requestService.getAllRequests().stream()
                .filter(r -> r.getRequesterName().toLowerCase().contains(lower) ||
                        r.getRequestId().toLowerCase().contains(lower) ||
                        r.getResourceId().toLowerCase().contains(lower))
                .toList(),
                filtered -> {
                    requestList.clear();
                    requestList.addAll(filtered);
                }, null);
    }

    public void refreshData() {
        TaskRunner.executeAsync(requestService::getAllRequests,
                requests -> {
                    requestList.clear();
                    requestList.addAll(requests);
                }, null);
    }
}
