package com.resourceflow.ui;

import com.resourceflow.service.ServiceRegistry;
import com.resourceflow.ui.views.*;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.*;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Main application window layout containing persistent sidebar navigation
 * and dynamic view swapping container.
 */
public class MainLayout extends BorderPane {

    private final ServiceRegistry registry;
    private final StackPane contentContainer;
    private final Map<String, Button> navButtons;
    private final Map<String, Node> viewCache;
    private Button activeButton;

    public MainLayout(ServiceRegistry registry) {
        this.registry = registry;
        this.navButtons = new LinkedHashMap<>();
        this.viewCache = new LinkedHashMap<>();
        this.contentContainer = new StackPane();

        // 1. Sidebar
        VBox sidebar = createSidebar();
        setLeft(sidebar);

        // 2. Central Content Area
        setCenter(contentContainer);

        // 3. Default view: Dashboard
        navigateTo("Dashboard");
    }

    private VBox createSidebar() {
        VBox sidebar = new VBox(8);
        sidebar.getStyleClass().add("sidebar");
        sidebar.setPrefWidth(240);
        sidebar.setMinWidth(220);

        // Brand Header
        VBox brandBox = new VBox(2);
        brandBox.setPadding(new Insets(10, 8, 20, 8));

        Label brandTitle = new Label("ResourceFlow");
        brandTitle.getStyleClass().add("sidebar-brand-title");

        Label brandSubtitle = new Label("Adaptive Optimisation System");
        brandSubtitle.getStyleClass().add("sidebar-brand-subtitle");

        brandBox.getChildren().addAll(brandTitle, brandSubtitle);

        // Navigation Items
        VBox navItems = new VBox(4);
        navItems.getChildren().addAll(
                createNavButton("📊 Dashboard"),
                createNavButton("📦 Resources"),
                createNavButton("📝 Requests"),
                createNavButton("⚡ Allocation Workspace"),
                createNavButton("⚖ Strategy Comparison"),
                createNavButton("🔄 Adaptive Reallocation"),
                createNavButton("🧪 What-If Analysis"),
                createNavButton("💡 Decision Explanations"),
                createNavButton("📜 History & Audit"),
                createNavButton("⚙ Settings & Diagnostics")
        );

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        // Footer status indicator
        VBox footerBox = new VBox(4);
        footerBox.setPadding(new Insets(10, 8, 10, 8));
        Label statusDot = new Label("● SQLite Local Store: Active");
        statusDot.setStyle("-fx-font-size: 11px; -fx-text-fill: #10b981;");
        Label version = new Label("Version 1.0.0 (Java 21 Desktop)");
        version.setStyle("-fx-font-size: 10px; -fx-text-fill: #64748b;");
        footerBox.getChildren().addAll(statusDot, version);

        sidebar.getChildren().addAll(brandBox, navItems, spacer, footerBox);
        return sidebar;
    }

    private Button createNavButton(String labelWithIcon) {
        Button btn = new Button(labelWithIcon);
        btn.getStyleClass().add("nav-button");
        btn.setMaxWidth(Double.MAX_VALUE);

        // Strip emoji to get key
        String key = labelWithIcon.substring(labelWithIcon.indexOf(' ') + 1).trim();
        navButtons.put(key, btn);

        btn.setOnAction(e -> navigateTo(key));
        return btn;
    }

    public void navigateTo(String viewKey) {
        Button btn = navButtons.get(viewKey);
        if (btn == null && viewKey.contains(" ")) {
            btn = navButtons.get(viewKey.substring(viewKey.indexOf(' ') + 1).trim());
        }

        if (activeButton != null) {
            activeButton.getStyleClass().remove("nav-button-active");
        }
        if (btn != null) {
            btn.getStyleClass().add("nav-button-active");
            activeButton = btn;
        }

        Node view = getView(viewKey);
        contentContainer.getChildren().setAll(view);
    }

    private Node getView(String key) {
        // Re-instantiate or refresh views to guarantee real-time data synchronization
        return switch (key) {
            case "Dashboard" -> new DashboardView(registry.getResourceService(), registry.getRequestService(), registry.getHistoryService());
            case "Resources" -> new ResourceManagementView(registry.getResourceService());
            case "Requests" -> new RequestManagementView(registry.getRequestService(), registry.getResourceService());
            case "Allocation Workspace" -> new AllocationWorkspaceView(registry.getAllocationService());
            case "Strategy Comparison" -> new StrategyComparisonView(registry.getAllocationService(), registry.getResourceService(), registry.getRequestService());
            case "Adaptive Reallocation" -> new AdaptiveReallocationView(registry.getAllocationService());
            case "What-If Analysis" -> new WhatIfAnalysisView(registry.getWhatIfService());
            case "Decision Explanations" -> new DecisionExplanationsView(registry.getAllocationService(), registry.getHistoryService());
            case "History & Audit" -> new AllocationHistoryView(registry.getHistoryService());
            case "Settings & Diagnostics" -> new SettingsView(registry.getDatabaseManager(), registry.getResourceService(), registry.getRequestService());
            default -> new DashboardView(registry.getResourceService(), registry.getRequestService(), registry.getHistoryService());
        };
    }
}
