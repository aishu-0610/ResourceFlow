package com.resourceflow.ui.views;

import com.resourceflow.model.AllocationRun;
import com.resourceflow.model.EvaluationMetrics;
import com.resourceflow.model.Resource;
import com.resourceflow.service.HistoryService;
import com.resourceflow.service.RequestService;
import com.resourceflow.service.ResourceService;
import com.resourceflow.ui.util.TaskRunner;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.chart.*;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.*;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Dashboard screen presenting real-time KPIs, resource utilisation charts,
 * and request fulfilment breakdowns.
 */
public class DashboardView extends ScrollPane {

    private final ResourceService resourceService;
    private final RequestService requestService;
    private final HistoryService historyService;

    private Label totalResourcesVal;
    private Label totalCapacityVal;
    private Label totalAllocatedVal;
    private Label utilisationVal;
    private Label totalRequestsVal;
    private Label satisfiedVal;
    private Label partialVal;
    private Label unsatisfiedVal;
    private Label overallFulfilmentVal;
    private Label jainsFairnessVal;

    private BarChart<String, Number> utilisationChart;
    private PieChart fulfilmentPieChart;
    private VBox latestRunCard;

    public DashboardView(ResourceService resourceService, RequestService requestService, HistoryService historyService) {
        this.resourceService = resourceService;
        this.requestService = requestService;
        this.historyService = historyService;

        setFitToWidth(true);
        setStyle("-fx-background-color: transparent; -fx-border-color: transparent;");

        VBox content = new VBox(24);
        content.getStyleClass().add("content-area");
        content.setPadding(new Insets(24));

        // Header
        VBox headerBox = new VBox(4);
        Label title = new Label("System Dashboard");
        title.getStyleClass().add("view-header-title");
        Label subtitle = new Label("Real-time capacity utilisation, request fulfilment, and fairness analytics");
        subtitle.getStyleClass().add("view-header-subtitle");
        headerBox.getChildren().addAll(title, subtitle);

        // KPI Cards Row 1: Capacity & Utilisation
        GridPane kpiGrid = createKpiGrid();

        // Charts Row
        HBox chartsRow = createChartsRow();

        // Latest Run Summary Card
        latestRunCard = createLatestRunCard();

        content.getChildren().addAll(headerBox, kpiGrid, chartsRow, latestRunCard);
        setContent(content);

        refreshData();
    }

    private GridPane createKpiGrid() {
        GridPane grid = new GridPane();
        grid.setHgap(16);
        grid.setVgap(16);

        totalResourcesVal = new Label("0");
        totalCapacityVal = new Label("0.0");
        totalAllocatedVal = new Label("0.0");
        utilisationVal = new Label("0.0%");
        totalRequestsVal = new Label("0");
        satisfiedVal = new Label("0");
        partialVal = new Label("0");
        unsatisfiedVal = new Label("0");
        overallFulfilmentVal = new Label("0.0%");
        jainsFairnessVal = new Label("1.000");

        grid.add(createMetricCard("ACTIVE RESOURCES", totalResourcesVal, "Configured inventory"), 0, 0);
        grid.add(createMetricCard("TOTAL CAPACITY", totalCapacityVal, "Active capacity across pools"), 1, 0);
        grid.add(createMetricCard("CAPACITY ALLOCATED", totalAllocatedVal, "Currently committed"), 2, 0);
        grid.add(createMetricCard("RESOURCE UTILISATION", utilisationVal, "Mean across active pools"), 3, 0);

        grid.add(createMetricCard("TOTAL REQUESTS", totalRequestsVal, "Submitted demands"), 0, 1);
        grid.add(createMetricCard("FULLY SATISFIED", satisfiedVal, "100% Demand fulfilled"), 1, 1);
        grid.add(createMetricCard("PARTIALLY SATISFIED", partialVal, "Partial capacity allocated"), 2, 1);
        grid.add(createMetricCard("JAIN'S FAIRNESS INDEX", jainsFairnessVal, "Max-min equity metric (0 - 1)"), 3, 1);

        for (int i = 0; i < 4; i++) {
            ColumnConstraints col = new ColumnConstraints();
            col.setPercentWidth(25);
            grid.getColumnConstraints().add(col);
        }

        return grid;
    }

    private VBox createMetricCard(String titleText, Label valueLabel, String subtitleText) {
        VBox card = new VBox(8);
        card.getStyleClass().add("card");
        card.setAlignment(Pos.CENTER_LEFT);

        Label title = new Label(titleText);
        title.getStyleClass().add("metric-title");

        valueLabel.getStyleClass().add("metric-value");

        Label subtitle = new Label(subtitleText);
        subtitle.getStyleClass().add("metric-subtitle");

        card.getChildren().addAll(title, valueLabel, subtitle);
        return card;
    }

    private HBox createChartsRow() {
        HBox row = new HBox(20);
        row.setAlignment(Pos.CENTER);

        // Bar Chart: Resource Utilisation
        CategoryAxis xAxis = new CategoryAxis();
        xAxis.setLabel("Resource Pool");
        NumberAxis yAxis = new NumberAxis(0, 100, 20);
        yAxis.setLabel("Utilisation (%)");

        utilisationChart = new BarChart<>(xAxis, yAxis);
        utilisationChart.setTitle("Resource Utilisation Rate");
        utilisationChart.setLegendVisible(false);
        utilisationChart.setAnimated(false);
        utilisationChart.setPrefHeight(320);

        VBox chartCard1 = new VBox(utilisationChart);
        chartCard1.getStyleClass().add("card");
        HBox.setHgrow(chartCard1, Priority.ALWAYS);

        // Pie Chart: Request Fulfilment Status
        fulfilmentPieChart = new PieChart();
        fulfilmentPieChart.setTitle("Request Fulfilment Breakdown");
        fulfilmentPieChart.setAnimated(false);
        fulfilmentPieChart.setPrefHeight(320);

        VBox chartCard2 = new VBox(fulfilmentPieChart);
        chartCard2.getStyleClass().add("card");
        HBox.setHgrow(chartCard2, Priority.ALWAYS);

        row.getChildren().addAll(chartCard1, chartCard2);
        return row;
    }

    private VBox createLatestRunCard() {
        VBox card = new VBox(12);
        card.getStyleClass().add("card");
        card.setAlignment(Pos.CENTER_LEFT);

        Label cardTitle = new Label("Latest Allocation Run Summary");
        cardTitle.getStyleClass().add("metric-title");

        Label infoLabel = new Label("No historical allocation run found. Run an allocation from the Allocation Workspace.");
        infoLabel.setStyle("-fx-text-fill: #94a3b8;");

        card.getChildren().addAll(cardTitle, infoLabel);
        return card;
    }

    public void refreshData() {
        TaskRunner.executeAsync(() -> {
            List<Resource> resources = resourceService.getAllResources();
            var requests = requestService.getAllRequests();
            Optional<AllocationRun> latestRun = historyService.getLatestRun();
            return new DashboardData(resources, requests, latestRun);
        }, data -> {
            // Update KPIs
            int activeCount = (int) data.resources.stream().filter(Resource::isActive).count();
            double totalCap = data.resources.stream().filter(Resource::isActive).mapToDouble(Resource::getTotalCapacity).sum();
            double totalAvail = data.resources.stream().filter(Resource::isActive).mapToDouble(Resource::getAvailableCapacity).sum();
            double allocated = Math.max(0.0, totalCap - totalAvail);
            double util = (totalCap > 0) ? (allocated / totalCap) * 100.0 : 0.0;

            totalResourcesVal.setText(String.valueOf(activeCount));
            totalCapacityVal.setText(String.format(Locale.US, "%.1f", totalCap));
            totalAllocatedVal.setText(String.format(Locale.US, "%.1f", allocated));
            utilisationVal.setText(String.format(Locale.US, "%.1f%%", util));

            totalRequestsVal.setText(String.valueOf(data.requests.size()));

            if (data.latestRun.isPresent()) {
                AllocationRun run = data.latestRun.get();
                EvaluationMetrics m = run.getMetrics();
                satisfiedVal.setText(String.valueOf(m.getFullySatisfiedCount()));
                partialVal.setText(String.valueOf(m.getPartiallySatisfiedCount()));
                unsatisfiedVal.setText(String.valueOf(m.getUnsatisfiedCount()));
                overallFulfilmentVal.setText(String.format(Locale.US, "%.1f%%", m.getOverallFulfilment()));
                jainsFairnessVal.setText(String.format(Locale.US, "%.3f", m.getJainsFairnessIndex()));

                // Update Pie Chart
                fulfilmentPieChart.getData().clear();
                if (m.getFullySatisfiedCount() > 0) {
                    fulfilmentPieChart.getData().add(new PieChart.Data("Fully Satisfied (" + m.getFullySatisfiedCount() + ")", m.getFullySatisfiedCount()));
                }
                if (m.getPartiallySatisfiedCount() > 0) {
                    fulfilmentPieChart.getData().add(new PieChart.Data("Partially Satisfied (" + m.getPartiallySatisfiedCount() + ")", m.getPartiallySatisfiedCount()));
                }
                if (m.getUnsatisfiedCount() > 0) {
                    fulfilmentPieChart.getData().add(new PieChart.Data("Unsatisfied (" + m.getUnsatisfiedCount() + ")", m.getUnsatisfiedCount()));
                }

                // Update Latest Run Info
                latestRunCard.getChildren().clear();
                Label cardTitle = new Label("Latest Allocation Run: " + (run.getScenarioName() != null ? run.getScenarioName() : run.getRunId()));
                cardTitle.getStyleClass().add("metric-title");

                Label details = new Label(String.format(Locale.US,
                        "Strategy: %s  |  Timestamp: %s  |  Aggregate Utilisation: %.1f%%  |  Fulfilment: %.1f%%  |  Jain's Fairness: %.3f",
                        run.getStrategyUsed().getDisplayName(),
                        run.getTimestamp().toString().replace("T", " ").substring(0, 19),
                        m.getAggregateUtilisation(),
                        m.getOverallFulfilment(),
                        m.getJainsFairnessIndex()));
                details.setStyle("-fx-text-fill: #cbd5e1; -fx-font-size: 13px;");

                latestRunCard.getChildren().addAll(cardTitle, details);
            } else {
                fulfilmentPieChart.getData().clear();
                fulfilmentPieChart.getData().add(new PieChart.Data("Pending Requests (" + data.requests.size() + ")", data.requests.size()));
            }

            // Update Bar Chart
            utilisationChart.getData().clear();
            XYChart.Series<String, Number> series = new XYChart.Series<>();
            series.setName("Utilisation %");
            for (Resource r : data.resources) {
                if (r.isActive()) {
                    double cap = r.getTotalCapacity();
                    double used = cap - r.getAvailableCapacity();
                    double pct = (cap > 0) ? (used / cap) * 100.0 : 0.0;
                    series.getData().add(new XYChart.Data<>(r.getName(), pct));
                }
            }
            utilisationChart.getData().add(series);

        }, null);
    }

    private static class DashboardData {
        final List<Resource> resources;
        final List<?> requests;
        final Optional<AllocationRun> latestRun;

        DashboardData(List<Resource> resources, List<?> requests, Optional<AllocationRun> latestRun) {
            this.resources = resources;
            this.requests = requests;
            this.latestRun = latestRun;
        }
    }
}
