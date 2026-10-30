package com.resourceflow.ui.views;

import com.resourceflow.engine.AllocationEngine;
import com.resourceflow.evaluation.StrategyComparison;
import com.resourceflow.model.*;
import com.resourceflow.service.AllocationService;
import com.resourceflow.service.RequestService;
import com.resourceflow.service.ResourceService;
import com.resourceflow.ui.util.DialogHelper;
import com.resourceflow.ui.util.TaskRunner;
import com.resourceflow.util.AuditLog;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.util.List;
import java.util.Locale;

/**
 * Strategy Comparison screen allowing side-by-side benchmarking of Priority, Fair-Share, and Proportional plans.
 */
public class StrategyComparisonView extends ScrollPane {

    private final AllocationService allocationService;
    private final ResourceService resourceService;
    private final RequestService requestService;

    private final TableView<AllocationPlan> tableView;
    private final ObservableList<AllocationPlan> planList;

    private BarChart<String, Number> comparisonChart;
    private Label bestFairnessLabel;
    private Label bestUtilisationLabel;
    private Label bestFulfilmentLabel;
    private Label recommendedLabel;
    private Button runCompareBtn;
    private ProgressIndicator progressIndicator;

    // Objective weights chosen with the sliders; changing them re-ranks without re-running
    private final ConstraintConfig weights = ConstraintConfig.defaultConfig();
    private StrategyComparison lastComparison;
    private final ListView<String> threadLog = new ListView<>();

    public StrategyComparisonView(AllocationService allocationService, ResourceService resourceService, RequestService requestService) {
        this.allocationService = allocationService;
        this.resourceService = resourceService;
        this.requestService = requestService;

        setFitToWidth(true);
        setStyle("-fx-background-color: transparent; -fx-border-color: transparent;");

        VBox content = new VBox(24);
        content.getStyleClass().add("content-area");
        content.setPadding(new Insets(24));

        // Header
        VBox headerBox = new VBox(4);
        Label title = new Label("Strategy Benchmark & Comparison");
        title.getStyleClass().add("view-header-title");
        Label subtitle = new Label("Simultaneously evaluate Priority-Based, Fair-Share, and Proportional strategies on current demands.");
        subtitle.getStyleClass().add("view-header-subtitle");
        headerBox.getChildren().addAll(title, subtitle);

        // Toolbar
        HBox toolbar = new HBox(16);
        toolbar.setAlignment(Pos.CENTER_LEFT);

        runCompareBtn = new Button("⚡ Run All Strategies Benchmark");
        runCompareBtn.getStyleClass().add("btn-primary");
        runCompareBtn.setOnAction(e -> executeComparison());

        progressIndicator = new ProgressIndicator();
        progressIndicator.setPrefSize(24, 24);
        progressIndicator.setVisible(false);

        toolbar.getChildren().addAll(runCompareBtn, progressIndicator);

        // Highlight Cards Row
        HBox highlightRow = createHighlightCards();

        // Table
        planList = FXCollections.observableArrayList();
        tableView = new TableView<>(planList);
        setupTable();
        tableView.setPrefHeight(180);

        // Side-by-side Chart
        VBox chartCard = createComparisonChart();

        // Weights + live thread log
        HBox weightsAndLog = new HBox(16, createWeightsCard(), createThreadLogCard());

        content.getChildren().addAll(headerBox, toolbar, highlightRow, weightsAndLog, tableView, chartCard);
        setContent(content);

        // Run initial comparison
        executeComparison();
    }

    private HBox createHighlightCards() {
        HBox row = new HBox(16);
        row.setAlignment(Pos.CENTER_LEFT);

        bestFairnessLabel = new Label("—");
        bestUtilisationLabel = new Label("—");
        bestFulfilmentLabel = new Label("—");

        VBox card1 = createSummaryCard("OPTIMAL FAIRNESS", bestFairnessLabel, "Maximizes Jain's equity index");
        VBox card2 = createSummaryCard("OPTIMAL UTILISATION", bestUtilisationLabel, "Minimizes idle resource capacity");
        VBox card3 = createSummaryCard("OPTIMAL FULFILMENT", bestFulfilmentLabel, "Maximizes total satisfied demand");
        recommendedLabel = new Label("—");
        VBox card4 = createSummaryCard("RECOMMENDED (WEIGHTED)", recommendedLabel, "Best score for your weights below");

        HBox.setHgrow(card1, Priority.ALWAYS);
        HBox.setHgrow(card2, Priority.ALWAYS);
        HBox.setHgrow(card3, Priority.ALWAYS);
        HBox.setHgrow(card4, Priority.ALWAYS);

        row.getChildren().addAll(card1, card2, card3, card4);
        return row;
    }

    /**
     * Three sliders for the objective weights. Each slider's valueProperty gets a listener
     * (event-driven programming): moving it updates the weight and re-ranks the strategies.
     */
    private VBox createWeightsCard() {
        VBox card = new VBox(10);
        card.getStyleClass().add("card");
        HBox.setHgrow(card, Priority.ALWAYS);

        Label title = new Label("OBJECTIVE WEIGHTS");
        title.getStyleClass().add("metric-title");

        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(8);
        addWeightSlider(grid, 0, "Utilisation", weights.getUtilisationWeight(), weights::setUtilisationWeight);
        addWeightSlider(grid, 1, "Fulfilment", weights.getFulfilmentWeight(), weights::setFulfilmentWeight);
        addWeightSlider(grid, 2, "Fairness", weights.getFairnessWeight(), weights::setFairnessWeight);

        Label formula = new Label("Score = (wU·utilisation + wF·fulfilment + wJ·fairness) / (wU + wF + wJ)");
        formula.getStyleClass().add("metric-subtitle");

        card.getChildren().addAll(title, grid, formula);
        return card;
    }

    private void addWeightSlider(GridPane grid, int row, String name, double initial,
                                 java.util.function.DoubleConsumer setter) {
        Slider slider = new Slider(0, 1, initial);
        slider.setPrefWidth(220);
        Label valueLabel = new Label(String.format(Locale.US, "%.2f", initial));

        slider.valueProperty().addListener((obs, oldValue, newValue) -> {
            setter.accept(newValue.doubleValue()); // Number -> double (unboxing)
            valueLabel.setText(String.format(Locale.US, "%.2f", newValue.doubleValue()));
            refreshRecommendation();
        });

        grid.add(new Label(name), 0, row);
        grid.add(slider, 1, row);
        grid.add(valueLabel, 2, row);
    }

    /** Shows the callback messages sent by the worker threads during a comparison. */
    private VBox createThreadLogCard() {
        VBox card = new VBox(10);
        card.getStyleClass().add("card");
        HBox.setHgrow(card, Priority.ALWAYS);

        Label title = new Label("PARALLEL EXECUTION LOG");
        title.getStyleClass().add("metric-title");

        threadLog.setPrefHeight(120);
        threadLog.setPlaceholder(new Label("Each strategy runs on its own thread; results appear here as they finish."));

        card.getChildren().addAll(title, threadLog);
        return card;
    }

    private void refreshRecommendation() {
        if (lastComparison == null) {
            return;
        }
        StrategyType best = lastComparison.recommend(weights);
        recommendedLabel.setText(best != null ? best.getDisplayName() : "—");
        tableView.refresh(); // re-evaluates the "Weighted Score" column
    }

    private VBox createSummaryCard(String title, Label valLabel, String sub) {
        VBox card = new VBox(6);
        card.getStyleClass().add("card");
        card.setAlignment(Pos.CENTER_LEFT);

        Label t = new Label(title);
        t.getStyleClass().add("metric-title");

        valLabel.getStyleClass().add("metric-value");

        Label s = new Label(sub);
        s.getStyleClass().add("metric-subtitle");

        card.getChildren().addAll(t, valLabel, s);
        return card;
    }

    private void setupTable() {
        TableColumn<AllocationPlan, String> nameCol = new TableColumn<>("Strategy");
        nameCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getStrategyName()));
        nameCol.setPrefWidth(220);

        TableColumn<AllocationPlan, String> utilCol = new TableColumn<>("Utilisation (%)");
        utilCol.setCellValueFactory(d -> new SimpleStringProperty(String.format(Locale.US, "%.1f%%", d.getValue().getMetrics().getAggregateUtilisation())));
        utilCol.setPrefWidth(120);

        TableColumn<AllocationPlan, String> fulfilCol = new TableColumn<>("Demand Fulfilment");
        fulfilCol.setCellValueFactory(d -> new SimpleStringProperty(String.format(Locale.US, "%.1f%%", d.getValue().getMetrics().getOverallFulfilment())));
        fulfilCol.setPrefWidth(140);

        TableColumn<AllocationPlan, String> fairCol = new TableColumn<>("Jain's Fairness");
        fairCol.setCellValueFactory(d -> new SimpleStringProperty(String.format(Locale.US, "%.3f", d.getValue().getMetrics().getJainsFairnessIndex())));
        fairCol.setPrefWidth(120);

        TableColumn<AllocationPlan, String> satCol = new TableColumn<>("Fully Satisfied");
        satCol.setCellValueFactory(d -> new SimpleStringProperty(String.valueOf(d.getValue().getMetrics().getFullySatisfiedCount())));
        satCol.setPrefWidth(110);

        TableColumn<AllocationPlan, String> partCol = new TableColumn<>("Partial");
        partCol.setCellValueFactory(d -> new SimpleStringProperty(String.valueOf(d.getValue().getMetrics().getPartiallySatisfiedCount())));
        partCol.setPrefWidth(90);

        TableColumn<AllocationPlan, String> unsatCol = new TableColumn<>("Unsatisfied");
        unsatCol.setCellValueFactory(d -> new SimpleStringProperty(String.valueOf(d.getValue().getMetrics().getUnsatisfiedCount())));
        unsatCol.setPrefWidth(90);

        TableColumn<AllocationPlan, String> unallocCol = new TableColumn<>("Unallocated Cap.");
        unallocCol.setCellValueFactory(d -> new SimpleStringProperty(String.format(Locale.US, "%.1f", d.getValue().getMetrics().getUnallocatedCapacity())));
        unallocCol.setPrefWidth(130);

        TableColumn<AllocationPlan, String> scoreCol = new TableColumn<>("Weighted Score");
        scoreCol.setCellValueFactory(d -> new SimpleStringProperty(String.format(Locale.US, "%.3f", d.getValue().getMetrics().weightedScore(weights))));
        scoreCol.setPrefWidth(120);

        // Read from the strategy's @StrategyInfo annotation
        TableColumn<AllocationPlan, String> complexityCol = new TableColumn<>("Complexity");
        complexityCol.setCellValueFactory(d -> new SimpleStringProperty(allocationService.getEngine()
                .getStrategy(d.getValue().getStrategyType())
                .map(strategy -> strategy.getComplexity())
                .orElse("—")));
        complexityCol.setPrefWidth(100);

        tableView.getColumns().addAll(nameCol, utilCol, fulfilCol, fairCol, scoreCol, satCol, partCol, unsatCol, unallocCol, complexityCol);
        tableView.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
    }

    private VBox createComparisonChart() {
        CategoryAxis xAxis = new CategoryAxis();
        xAxis.setLabel("Allocation Strategy");

        NumberAxis yAxis = new NumberAxis(0, 100, 20);
        yAxis.setLabel("Metric Score (%)");

        comparisonChart = new BarChart<>(xAxis, yAxis);
        comparisonChart.setTitle("Comparative Metric Performance Across Strategies");
        comparisonChart.setAnimated(false);
        comparisonChart.setPrefHeight(340);

        VBox card = new VBox(comparisonChart);
        card.getStyleClass().add("card");
        return card;
    }

    private void executeComparison() {
        runCompareBtn.setDisable(true);
        progressIndicator.setVisible(true);
        threadLog.getItems().clear();

        TaskRunner.executeAsync(() -> {
            List<Resource> resources = resourceService.getAllResources();
            List<AllocationRequest> requests = requestService.getEligibleRequests();
            ResourceInventory inv = ResourceInventory.forReplanning(resources);
            AllocationEngine engine = allocationService.getEngine();

            // Callback runs on each worker thread; JavaFX controls may only be changed on the
            // JavaFX Application Thread, so the UI update is handed over with Platform.runLater.
            return engine.compareAllStrategies(inv, requests, ConstraintConfig.defaultConfig(),
                    (plan, elapsedMillis, threadName) -> Platform.runLater(() -> threadLog.getItems().add(
                            String.format(Locale.US, "%s: %s done in %d ms", threadName,
                                    plan.getStrategyType().getDisplayName(), elapsedMillis))));
        }, comparison -> {
            runCompareBtn.setDisable(false);
            progressIndicator.setVisible(false);
            lastComparison = comparison;
            refreshRecommendation();
            AuditLog.record("Strategy comparison run; recommended %s", comparison.recommend(weights));

            planList.clear();
            planList.addAll(comparison.getPlanMap().values());

            if (comparison.getBestFairnessStrategy() != null) {
                bestFairnessLabel.setText(comparison.getBestFairnessStrategy().getDisplayName());
            }
            if (comparison.getBestUtilisationStrategy() != null) {
                bestUtilisationLabel.setText(comparison.getBestUtilisationStrategy().getDisplayName());
            }
            if (comparison.getBestFulfilmentStrategy() != null) {
                bestFulfilmentLabel.setText(comparison.getBestFulfilmentStrategy().getDisplayName());
            }

            // Update Chart
            comparisonChart.getData().clear();

            XYChart.Series<String, Number> utilSeries = new XYChart.Series<>();
            utilSeries.setName("Utilisation (%)");

            XYChart.Series<String, Number> fulfilSeries = new XYChart.Series<>();
            fulfilSeries.setName("Demand Fulfilment (%)");

            XYChart.Series<String, Number> fairnessSeries = new XYChart.Series<>();
            fairnessSeries.setName("Jain's Fairness (x100)");

            for (AllocationPlan plan : planList) {
                String name = plan.getStrategyType().getDisplayName();
                utilSeries.getData().add(new XYChart.Data<>(name, plan.getMetrics().getAggregateUtilisation()));
                fulfilSeries.getData().add(new XYChart.Data<>(name, plan.getMetrics().getOverallFulfilment()));
                fairnessSeries.getData().add(new XYChart.Data<>(name, plan.getMetrics().getJainsFairnessIndex() * 100.0));
            }

            comparisonChart.getData().addAll(utilSeries, fulfilSeries, fairnessSeries);
        }, error -> {
            runCompareBtn.setDisable(false);
            progressIndicator.setVisible(false);
            DialogHelper.showError("Benchmark Error", "Failed to compute strategy comparison", error.getMessage());
        });
    }
}
