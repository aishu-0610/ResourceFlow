package com.resourceflow.io;

import com.resourceflow.model.AllocatedItem;
import com.resourceflow.model.EvaluationMetrics;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;

/**
 * Writes an allocation result as a CSV file that opens in Excel.
 * Uses NIO (Path, Files.newBufferedWriter) wrapped in a PrintWriter (character streams).
 */
public final class ReportExporter {

    private static final String[] COLUMNS = {
            "Request ID", "Requester", "Resource", "Requested", "Allocated", "Fulfilment %", "Status", "Explanation"
    };

    private ReportExporter() {
    }

    /**
     * @throws IOException declared with "throws": the caller decides how to report a failed export
     */
    public static void exportCsv(String title, List<AllocatedItem> items, EvaluationMetrics metrics, Path target)
            throws IOException {
        Path parent = target.toAbsolutePath().getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }

        try (BufferedWriter buffered = Files.newBufferedWriter(target, StandardCharsets.UTF_8);
             PrintWriter out = new PrintWriter(buffered)) {

            out.println("# " + escape(title));
            if (metrics != null) {
                out.printf(Locale.US, "# Utilisation %.1f%%, Fulfilment %.1f%%, Jain's fairness %.3f%n",
                        metrics.getAggregateUtilisation(), metrics.getOverallFulfilment(), metrics.getJainsFairnessIndex());
            }
            out.println(String.join(",", COLUMNS));

            for (AllocatedItem item : items) {
                out.printf(Locale.US, "%s,%s,%s,%.2f,%.2f,%.1f,%s,%s%n",
                        escape(item.getRequestId()), escape(item.getRequesterName()), escape(item.getResourceId()),
                        item.getRequestedQuantity(), item.getAllocatedQuantity(), item.getFulfilmentRatio() * 100.0,
                        item.getStatus(), escape(item.getExplanation()));
            }
        }
    }

    /** CSV rule: wrap a value in quotes if it contains a comma, quote or newline; double any quotes. */
    static String escape(String value) {
        if (value == null) {
            return "";
        }
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}
