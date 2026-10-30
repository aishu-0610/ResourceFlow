package com.resourceflow.ui.views;

import com.resourceflow.exception.ResourceConflictException;
import com.resourceflow.model.Resource;
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
import java.util.Locale;
import java.util.Optional;

/**
 * Resource Management screen providing CRUD, capacity tracking, and status controls.
 */
public class ResourceManagementView extends VBox {

    private final ResourceService resourceService;
    private final TableView<Resource> tableView;
    private final ObservableList<Resource> resourceList;
    private TextField searchField;

    public ResourceManagementView(ResourceService resourceService) {
        this.resourceService = resourceService;
        this.resourceList = FXCollections.observableArrayList();
        this.tableView = new TableView<>(resourceList);

        setSpacing(20);
        getStyleClass().add("content-area");
        setPadding(new Insets(24));

        // Header
        VBox headerBox = new VBox(4);
        Label title = new Label("Resource Inventory Management");
        title.getStyleClass().add("view-header-title");
        Label subtitle = new Label("Configure available capacity, units of measurement, and operational status.");
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
        searchField.setPromptText("Filter resources by name, category, or ID...");
        searchField.setPrefWidth(320);
        searchField.textProperty().addListener((obs, oldVal, newVal) -> filterTable(newVal));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button newBtn = new Button("+ New Resource");
        newBtn.getStyleClass().add("btn-primary");
        newBtn.setOnAction(e -> showResourceDialog(null));

        Button editBtn = new Button("Edit");
        editBtn.getStyleClass().add("btn-secondary");
        editBtn.setOnAction(e -> {
            Resource selected = tableView.getSelectionModel().getSelectedItem();
            if (selected != null) {
                showResourceDialog(selected);
            } else {
                DialogHelper.showWarning("Selection Required", null, "Please select a resource to edit.");
            }
        });

        Button toggleBtn = new Button("Toggle Active");
        toggleBtn.getStyleClass().add("btn-secondary");
        toggleBtn.setOnAction(e -> {
            Resource selected = tableView.getSelectionModel().getSelectedItem();
            if (selected != null) {
                resourceService.toggleActiveStatus(selected.getResourceId());
                refreshData();
            }
        });

        Button deleteBtn = new Button("Delete");
        deleteBtn.getStyleClass().add("btn-danger");
        deleteBtn.setOnAction(e -> handleDelete());

        toolbar.getChildren().addAll(searchField, spacer, newBtn, editBtn, toggleBtn, deleteBtn);
        return toolbar;
    }

    private void setupTable() {
        TableColumn<Resource, String> idCol = new TableColumn<>("Resource ID");
        idCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getResourceId()));
        idCol.setPrefWidth(120);

        TableColumn<Resource, String> nameCol = new TableColumn<>("Name");
        nameCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getName()));
        nameCol.setPrefWidth(220);

        TableColumn<Resource, String> catCol = new TableColumn<>("Category");
        catCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getCategory()));
        catCol.setPrefWidth(120);

        TableColumn<Resource, String> totalCapCol = new TableColumn<>("Total Capacity");
        totalCapCol.setCellValueFactory(d -> new SimpleStringProperty(String.format(Locale.US, "%.1f %s", d.getValue().getTotalCapacity(), d.getValue().getUnit())));
        totalCapCol.setPrefWidth(140);

        TableColumn<Resource, String> availCapCol = new TableColumn<>("Available");
        availCapCol.setCellValueFactory(d -> new SimpleStringProperty(String.format(Locale.US, "%.1f %s", d.getValue().getAvailableCapacity(), d.getValue().getUnit())));
        availCapCol.setPrefWidth(140);

        TableColumn<Resource, String> statusCol = new TableColumn<>("Status");
        statusCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().isActive() ? "ACTIVE" : "INACTIVE"));
        statusCol.setPrefWidth(100);
        statusCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    Label badge = new Label(item);
                    badge.getStyleClass().add(item.equals("ACTIVE") ? "badge-success" : "badge-neutral");
                    setGraphic(badge);
                    setText(null);
                }
            }
        });

        TableColumn<Resource, String> descCol = new TableColumn<>("Description");
        descCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getDescription()));
        descCol.setPrefWidth(250);

        tableView.getColumns().addAll(idCol, nameCol, catCol, totalCapCol, availCapCol, statusCol, descCol);
        tableView.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
    }

    private void handleDelete() {
        Resource selected = tableView.getSelectionModel().getSelectedItem();
        if (selected == null) {
            DialogHelper.showWarning("Selection Required", null, "Please select a resource to delete.");
            return;
        }

        boolean confirm = DialogHelper.showConfirmation("Delete Resource",
                "Delete " + selected.getName() + " (" + selected.getResourceId() + ")?",
                "This action cannot be undone. If historical records depend on this resource, deletion will be blocked.");

        if (confirm) {
            try {
                resourceService.deleteResource(selected.getResourceId());
                refreshData();
            } catch (ResourceConflictException ex) {
                DialogHelper.showError("Deletion Blocked", "Historical Record Protection", ex.getMessage());
            } catch (Exception ex) {
                DialogHelper.showError("Error", "Failed to delete resource", ex.getMessage());
            }
        }
    }

    private void showResourceDialog(Resource existing) {
        Dialog<Resource> dialog = new Dialog<>();
        dialog.setTitle(existing == null ? "Create New Resource" : "Edit Resource: " + existing.getResourceId());
        dialog.setHeaderText(existing == null ? "Define resource specifications and capacity." : "Modify resource capacity and metadata.");

        ButtonType saveBtnType = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveBtnType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(12);
        grid.setPadding(new Insets(20, 20, 10, 20));

        TextField idField = new TextField();
        idField.setPromptText("e.g., GPU-A100");
        if (existing != null) {
            idField.setText(existing.getResourceId());
            idField.setDisable(true);
        }

        TextField nameField = new TextField();
        nameField.setPromptText("e.g., NVIDIA A100 GPU Cluster");
        if (existing != null) nameField.setText(existing.getName());

        TextField catField = new TextField();
        catField.setPromptText("e.g., Compute, Laboratory, Financial");
        if (existing != null) catField.setText(existing.getCategory());

        TextField totalCapField = new TextField();
        totalCapField.setPromptText("e.g., 100.0");
        if (existing != null) totalCapField.setText(String.valueOf(existing.getTotalCapacity()));

        TextField unitField = new TextField();
        unitField.setPromptText("e.g., Hours, Seats, Cores, USD");
        if (existing != null) unitField.setText(existing.getUnit());

        TextArea descArea = new TextArea();
        descArea.setPromptText("Optional description or specifications");
        descArea.setPrefRowCount(3);
        if (existing != null) descArea.setText(existing.getDescription());

        CheckBox activeCheck = new CheckBox("Active for Allocation");
        activeCheck.setSelected(existing == null || existing.isActive());

        grid.add(new Label("Resource ID:"), 0, 0);
        grid.add(idField, 1, 0);
        grid.add(new Label("Name:"), 0, 1);
        grid.add(nameField, 1, 1);
        grid.add(new Label("Category:"), 0, 2);
        grid.add(catField, 1, 2);
        grid.add(new Label("Total Capacity:"), 0, 3);
        grid.add(totalCapField, 1, 3);
        grid.add(new Label("Unit:"), 0, 4);
        grid.add(unitField, 1, 4);
        grid.add(new Label("Description:"), 0, 5);
        grid.add(descArea, 1, 5);
        grid.add(new Label("Status:"), 0, 6);
        grid.add(activeCheck, 1, 6);

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == saveBtnType) {
                try {
                    String id = idField.getText().trim();
                    String name = nameField.getText().trim();
                    String cat = catField.getText().trim();
                    double totalCap = Double.parseDouble(totalCapField.getText().trim());
                    String unit = unitField.getText().trim();
                    String desc = descArea.getText().trim();
                    boolean active = activeCheck.isSelected();

                    if (existing == null) {
                        return new Resource(id, name, cat, totalCap, totalCap, unit, desc, active);
                    } else {
                        existing.setName(name);
                        existing.setCategory(cat);
                        existing.setTotalCapacity(totalCap);
                        // Clamp available capacity if total capacity decreased
                        if (existing.getAvailableCapacity() > totalCap) {
                            existing.setAvailableCapacity(totalCap);
                        }
                        existing.setUnit(unit);
                        existing.setDescription(desc);
                        existing.setActive(active);
                        existing.setUpdatedAt(LocalDateTime.now());
                        return existing;
                    }
                } catch (NumberFormatException ex) {
                    DialogHelper.showError("Validation Error", "Invalid Capacity", "Total capacity must be a valid positive number.");
                    return null;
                }
            }
            return null;
        });

        Optional<Resource> result = dialog.showAndWait();
        result.ifPresent(res -> {
            try {
                if (existing == null) {
                    resourceService.createResource(res);
                } else {
                    resourceService.updateResource(res);
                }
                refreshData();
            } catch (Exception ex) {
                DialogHelper.showError("Error", "Failed to save resource", ex.getMessage());
            }
        });
    }

    private void filterTable(String query) {
        if (query == null || query.trim().isEmpty()) {
            refreshData();
            return;
        }
        String lower = query.toLowerCase();
        TaskRunner.executeAsync(() -> resourceService.getAllResources().stream()
                .filter(r -> r.getName().toLowerCase().contains(lower) ||
                        r.getResourceId().toLowerCase().contains(lower) ||
                        r.getCategory().toLowerCase().contains(lower))
                .toList(),
                filtered -> {
                    resourceList.clear();
                    resourceList.addAll(filtered);
                }, null);
    }

    public void refreshData() {
        TaskRunner.executeAsync(resourceService::getAllResources,
                resources -> {
                    resourceList.clear();
                    resourceList.addAll(resources);
                }, null);
    }
}
