package com.resourceflow.repository;

import com.resourceflow.database.DatabaseManager;
import com.resourceflow.exception.ResourceConflictException;
import com.resourceflow.model.*;
import com.resourceflow.service.AllocationService;
import com.resourceflow.service.RequestService;
import com.resourceflow.service.ResourceService;
import com.resourceflow.service.ServiceRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class PersistenceTest {

    private DatabaseManager dbManager;
    private ResourceRepository resourceRepo;
    private RequestRepository requestRepo;
    private AllocationRunRepository runRepo;
    private ResourceService resourceService;
    private RequestService requestService;
    private AllocationService allocationService;

    @BeforeEach
    void setUp() throws IOException {
        // Create an isolated temporary SQLite database file for each test
        File tempDb = Files.createTempFile("resourceflow-test-", ".db").toFile();
        tempDb.deleteOnExit();

        dbManager = new DatabaseManager(tempDb.getAbsolutePath());
        dbManager.initializeSchema();

        resourceRepo = new SqliteResourceRepository(dbManager);
        requestRepo = new SqliteRequestRepository(dbManager);
        runRepo = new SqliteAllocationRunRepository(dbManager);

        ServiceRegistry registry = ServiceRegistry.initializeWithCustomDb(dbManager);
        resourceService = registry.getResourceService();
        requestService = registry.getRequestService();
        allocationService = registry.getAllocationService();
    }

    @Test
    @DisplayName("Resource CRUD operations persist cleanly to SQLite")
    void testResourceCrud() {
        Resource res = new Resource("RES-001", "Lab Workstation", "Hardware", 20.0, 20.0, "Seats", "High-end CAD PCs", true);
        resourceService.createResource(res);

        Optional<Resource> loadedOpt = resourceRepo.findById("RES-001");
        assertTrue(loadedOpt.isPresent());
        Resource loaded = loadedOpt.get();
        assertEquals("Lab Workstation", loaded.getName());
        assertEquals(20.0, loaded.getTotalCapacity(), 1e-6);

        // Update
        loaded.setName("Renovated Lab Workstation");
        loaded.setTotalCapacity(25.0);
        loaded.setAvailableCapacity(25.0);
        resourceService.updateResource(loaded);

        Resource updated = resourceService.getResourceById("RES-001");
        assertEquals("Renovated Lab Workstation", updated.getName());
        assertEquals(25.0, updated.getTotalCapacity(), 1e-6);

        // Delete
        resourceService.deleteResource("RES-001");
        assertFalse(resourceRepo.existsById("RES-001"));
    }

    @Test
    @DisplayName("Request CRUD and validation against database")
    void testRequestCrud() {
        Resource res = new Resource("RES-GPU", "GPU Cluster", "Compute", 80.0, 80.0, "Hours", "", true);
        resourceService.createResource(res);

        AllocationRequest req = new AllocationRequest("REQ-101", "Dr. Vance", "RES-GPU", 30.0, 4, 10.0, false, LocalDateTime.now(), null, RequestStatus.PENDING, "Deep learning trial");
        requestService.createRequest(req);

        Optional<AllocationRequest> loaded = requestRepo.findById("REQ-101");
        assertTrue(loaded.isPresent());
        assertEquals("Dr. Vance", loaded.get().getRequesterName());
        assertEquals(30.0, loaded.get().getRequestedQuantity(), 1e-6);
        assertEquals(RequestStatus.PENDING, loaded.get().getStatus());

        // Cancel
        requestService.cancelRequest("REQ-101");
        assertEquals(RequestStatus.CANCELLED, requestService.getRequestById("REQ-101").getStatus());
    }

    @Test
    @DisplayName("Allocation run atomic transaction persists run, allocations, and metrics")
    void testAllocationRunPersistence() {
        Resource res = new Resource("RES-GPU", "GPU Cluster", "Compute", 100.0, 100.0, "Hours", "", true);
        resourceService.createResource(res);

        AllocationRequest req1 = new AllocationRequest("REQ-1", "Alice", "RES-GPU", 60.0, 5, 0.0, false, LocalDateTime.now(), null, RequestStatus.PENDING, "");
        AllocationRequest req2 = new AllocationRequest("REQ-2", "Bob", "RES-GPU", 60.0, 3, 0.0, false, LocalDateTime.now(), null, RequestStatus.PENDING, "");
        requestService.createRequest(req1);
        requestService.createRequest(req2);

        // Execute allocation run
        AllocationRun run = allocationService.runAllocation(StrategyType.PRIORITY_BASED, ConstraintConfig.defaultConfig(), "Test Run 1");

        assertNotNull(run.getRunId());
        Optional<AllocationRun> persistedOpt = runRepo.findById(run.getRunId());
        assertTrue(persistedOpt.isPresent());

        AllocationRun persisted = persistedOpt.get();
        assertEquals(2, persisted.getAllocations().size());
        assertNotNull(persisted.getMetrics());
        assertEquals(100.0, persisted.getMetrics().getAggregateUtilisation(), 1e-6);

        // Verify that database operational state was updated:
        // Alice (Priority 5) gets 60.0 (ALLOCATED)
        // Bob (Priority 3) gets 40.0 (PARTIALLY_ALLOCATED)
        AllocationRequest updatedAlice = requestService.getRequestById("REQ-1");
        AllocationRequest updatedBob = requestService.getRequestById("REQ-2");
        assertEquals(RequestStatus.ALLOCATED, updatedAlice.getStatus());
        assertEquals(RequestStatus.PARTIALLY_ALLOCATED, updatedBob.getStatus());

        Resource updatedGpu = resourceService.getResourceById("RES-GPU");
        assertEquals(0.0, updatedGpu.getAvailableCapacity(), 1e-6); // 100 - (60 + 40)
    }

    @Test
    @DisplayName("Historical integrity prevents deleting a resource referenced by historical allocations")
    void testHistoricalIntegrityOnDelete() {
        Resource res = new Resource("RES-GPU", "GPU Cluster", "Compute", 100.0, 100.0, "Hours", "", true);
        resourceService.createResource(res);

        AllocationRequest req = new AllocationRequest("REQ-1", "Alice", "RES-GPU", 50.0, 5, 0.0, false, LocalDateTime.now(), null, RequestStatus.PENDING, "");
        requestService.createRequest(req);

        allocationService.runAllocation(StrategyType.PRIORITY_BASED, ConstraintConfig.defaultConfig(), "Baseline");

        // Attempting to delete RES-GPU should throw ResourceConflictException
        assertThrows(ResourceConflictException.class, () -> resourceService.deleteResource("RES-GPU"));
    }
}
