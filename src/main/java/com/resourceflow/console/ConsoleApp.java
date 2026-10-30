package com.resourceflow.console;

import com.resourceflow.engine.AllocationEngine;
import com.resourceflow.evaluation.StrategyComparison;
import com.resourceflow.io.ReportExporter;
import com.resourceflow.model.*;
import com.resourceflow.service.ServiceRegistry;
import com.resourceflow.util.AuditLog;
import com.resourceflow.util.DemoDataSeeder;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;

/**
 * Text-mode front end: the same services and engine as the JavaFX app, driven from the terminal.
 * Start it with:  java -jar resourceflow-1.0.0.jar --console
 *
 * Shows console I/O with the predefined streams: System.in is wrapped in an
 * InputStreamReader (bytes -> characters) and a BufferedReader (read whole lines);
 * output goes to System.out, errors to System.err.
 */
public class ConsoleApp {

    private static final String[] MENU = {
            "List resources",
            "List requests",
            "Compare all strategies (parallel threads)",
            "Run and save an allocation",
            "Export the last result to CSV",
    };

    private final ServiceRegistry registry;
    private final BufferedReader in;
    private AllocationPlan lastPlan;

    public ConsoleApp(ServiceRegistry registry) {
        this.registry = registry;
        this.in = new BufferedReader(new InputStreamReader(System.in));
    }

    public static void main(String[] args) {
        ServiceRegistry registry = ServiceRegistry.getInstance();
        DemoDataSeeder.seedDemoDataIfEmpty(registry.getResourceService(), registry.getRequestService());
        new ConsoleApp(registry).run();
    }

    public void run() {
        System.out.println("ResourceFlow console (running on thread '" + Thread.currentThread().getName() + "')");
        AuditLog.record("Console session started");

        boolean running = true;
        while (running) {
            printMenu();
            int choice = readInt("Choose an option: ", 0, MENU.length);
            try {
                switch (choice) {
                    case 1 -> listResources();
                    case 2 -> listRequests();
                    case 3 -> compareStrategies();
                    case 4 -> runAllocation();
                    case 5 -> exportLastPlan();
                    case 0 -> running = false;
                    default -> System.out.println("Unknown option");
                }
            } catch (RuntimeException e) {
                // Keep the console alive if one action fails
                System.err.println("Error: " + e.getMessage());
            }
        }
        System.out.println("Goodbye.");
        AuditLog.record("Console session ended");
    }

    private void printMenu() {
        System.out.println();
        for (int i = 0; i < MENU.length; i++) {
            System.out.printf("  %d. %s%n", i + 1, MENU[i]);
        }
        System.out.println("  0. Exit");
    }

    private void listResources() {
        System.out.printf("%-12s %-32s %12s %12s  %s%n", "ID", "Name", "Total", "Available", "Unit");
        for (Resource r : registry.getResourceService().getAllResources()) {
            System.out.printf(Locale.US, "%-12s %-32s %12.1f %12.1f  %s%n",
                    r.getResourceId(), r.getName(), r.getTotalCapacity(), r.getAvailableCapacity(), r.getUnit());
        }
    }

    private void listRequests() {
        System.out.printf("%-12s %-34s %-12s %10s %4s  %s%n", "ID", "Requester", "Resource", "Quantity", "Pri", "Status");
        for (AllocationRequest r : registry.getRequestService().getAllRequests()) {
            System.out.printf(Locale.US, "%-12s %-34s %-12s %10.1f %4d  %s%n",
                    r.getRequestId(), r.getRequesterName(), r.getResourceId(), r.getRequestedQuantity(),
                    r.getPriority(), r.getStatus());
        }
    }

    private void compareStrategies() {
        AllocationEngine engine = registry.getAllocationEngine();
        ResourceInventory inventory = ResourceInventory.forReplanning(registry.getResourceService().getAllResources());
        List<AllocationRequest> requests = registry.getRequestService().getEligibleRequests();

        System.out.println("Starting one thread per strategy...");
        // Callback as a lambda: printed by each worker thread the moment its strategy finishes
        StrategyComparison comparison = engine.compareAllStrategies(inventory, requests, ConstraintConfig.defaultConfig(),
                (plan, ms, thread) -> System.out.printf("  [%s] %s finished in %d ms%n", thread, plan.getStrategyName(), ms));

        System.out.printf("%n%-34s %12s %12s %10s %10s%n", "Strategy", "Utilisation", "Fulfilment", "Fairness", "Score");
        ConstraintConfig weights = ConstraintConfig.defaultConfig();
        for (AllocationPlan plan : comparison.getPlanMap().values()) {
            EvaluationMetrics m = plan.getMetrics();
            System.out.printf(Locale.US, "%-34s %11.1f%% %11.1f%% %10.3f %10.3f%n", plan.getStrategyName(),
                    m.getAggregateUtilisation(), m.getOverallFulfilment(), m.getJainsFairnessIndex(), m.weightedScore(weights));
        }
        StrategyType best = comparison.recommend(weights);
        System.out.println("Recommended (default weights): " + (best != null ? best.getDisplayName() : "none"));

        if (best != null) {
            lastPlan = comparison.getPlan(best).orElse(null);
        }
    }

    private void runAllocation() {
        StrategyType[] types = StrategyType.values();
        for (int i = 0; i < types.length; i++) {
            System.out.printf("  %d. %s%n", i + 1, types[i].getDisplayName());
        }
        StrategyType type = types[readInt("Strategy: ", 1, types.length) - 1];

        System.out.print("Run name (Enter for default): ");
        String name = readLine().trim();
        if (name.isEmpty()) {
            name = type.getDisplayName() + " (console)";
        }

        AllocationRun run = registry.getAllocationService().runAllocation(type, ConstraintConfig.defaultConfig(), name);
        lastPlan = new AllocationPlan(run.getStrategyUsed(), run.getScenarioName(), run.getAllocations());
        lastPlan.setMetrics(run.getMetrics());
        System.out.println("Saved run " + run.getRunId() + ": " + run.getMetrics());
    }

    private void exportLastPlan() {
        if (lastPlan == null) {
            System.out.println("Nothing to export yet: compare or run first.");
            return;
        }
        System.out.print("File name [allocation-report.csv]: ");
        String fileName = readLine().trim();
        Path target = Path.of(fileName.isEmpty() ? "allocation-report.csv" : fileName);
        try {
            ReportExporter.exportCsv(lastPlan.getStrategyName(), lastPlan.getItems(), lastPlan.getMetrics(), target);
            System.out.println("Written to " + target.toAbsolutePath());
        } catch (IOException e) {
            System.err.println("Export failed: " + e.getMessage());
        }
    }

    private String readLine() {
        try {
            String line = in.readLine();
            if (line == null) { // end of input (Ctrl+D or a closed pipe)
                System.out.println();
                AuditLog.record("Console session ended (input closed)");
                AuditLog.stop();
                System.exit(0);
            }
            return line;
        } catch (IOException e) {
            throw new IllegalStateException("Cannot read from console", e);
        }
    }

    /** Keeps asking until the user types a whole number between min and max. */
    private int readInt(String prompt, int min, int max) {
        while (true) {
            System.out.print(prompt);
            String text = readLine().trim();
            try {
                int value = Integer.parseInt(text);
                if (value >= min && value <= max) {
                    return value;
                }
                System.out.printf("Please enter a number from %d to %d.%n", min, max);
            } catch (NumberFormatException e) {
                System.out.println("'" + text + "' is not a number.");
            }
        }
    }
}
