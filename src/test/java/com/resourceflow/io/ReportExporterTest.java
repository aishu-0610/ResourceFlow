package com.resourceflow.io;

import com.resourceflow.model.AllocatedItem;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ReportExporterTest {

    @TempDir
    Path tempDir;

    @Test
    @DisplayName("CSV has a header row, one row per item, and quotes values containing commas")
    void writesCsv() throws IOException {
        List<AllocatedItem> items = List.of(
                new AllocatedItem(null, "REQ-1", "Smith, Alice", "GPU", 40.0, 20.0, "Partially satisfied"));
        Path file = tempDir.resolve("reports/plan.csv");

        ReportExporter.exportCsv("Test plan", items, null, file);

        List<String> lines = Files.readAllLines(file);
        assertEquals("# Test plan", lines.get(0));
        assertTrue(lines.get(1).startsWith("Request ID,Requester"));
        assertEquals("REQ-1,\"Smith, Alice\",GPU,40.00,20.00,50.0,Partially Satisfied,Partially satisfied", lines.get(2));
    }

    @Test
    void escapeDoublesQuotes() {
        assertEquals("\"say \"\"hi\"\"\"", ReportExporter.escape("say \"hi\""));
        assertEquals("plain", ReportExporter.escape("plain"));
        assertEquals("", ReportExporter.escape(null));
    }
}
