package com.resourceflow.io;

import com.resourceflow.exception.ScenarioFileException;
import com.resourceflow.model.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ScenarioFileStoreTest {

    @TempDir
    Path tempDir;

    private Scenario sampleScenario() {
        Resource gpu = new Resource("GPU", "GPU Cluster", "Compute", 150.0, 150.0, "Hours", "", true);
        AllocationRequest req = new AllocationRequest("REQ-1", "Alice", "GPU", 40.0, 4, 10.0, true,
                LocalDateTime.of(2026, 1, 15, 9, 30), null, RequestStatus.PENDING, "notes");
        ConstraintConfig config = new ConstraintConfig();
        config.setEnforceAllOrNothing(false);
        config.setFairnessWeight(0.8);
        return new Scenario("S-1", "Expansion", "20% more GPU", null, List.of(gpu), List.of(req),
                StrategyType.FAIR_SHARE, config);
    }

    @Test
    @DisplayName("Saving then loading gives back an identical scenario (object serialization)")
    void roundTrip() throws ScenarioFileException {
        Path file = tempDir.resolve("expansion.rflow");
        ScenarioFileStore.save(sampleScenario(), file);

        Scenario loaded = ScenarioFileStore.load(file);

        assertEquals("Expansion", loaded.getName());
        assertEquals(StrategyType.FAIR_SHARE, loaded.getStrategyType());
        assertEquals(150.0, loaded.getModifiedResources().getFirst().getTotalCapacity());
        AllocationRequest req = loaded.getModifiedRequests().getFirst();
        assertEquals(LocalDateTime.of(2026, 1, 15, 9, 30), req.getSubmissionDate());
        assertTrue(req.isAllOrNothing());
        assertFalse(loaded.getConfig().isEnforceAllOrNothing());
        assertEquals(0.8, loaded.getConfig().getFairnessWeight());
    }

    @Test
    @DisplayName("A file that is not a scenario is rejected with a chained cause")
    void corruptFileIsRejected() throws IOException {
        Path file = tempDir.resolve("notes.rflow");
        Files.writeString(file, "this is just text");

        ScenarioFileException e = assertThrows(ScenarioFileException.class, () -> ScenarioFileStore.load(file));
        assertNotNull(e.getCause(), "the low-level IOException should be attached as the cause");
    }

    @Test
    @DisplayName("A missing file gives a readable message")
    void missingFile() {
        ScenarioFileException e = assertThrows(ScenarioFileException.class,
                () -> ScenarioFileStore.load(tempDir.resolve("nothing-here.rflow")));
        assertTrue(e.getMessage().startsWith("File not found"));
    }
}
