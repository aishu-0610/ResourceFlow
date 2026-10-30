package com.resourceflow.service;

import com.resourceflow.database.DatabaseManager;
import com.resourceflow.engine.AllocationEngine;
import com.resourceflow.repository.*;

/**
 * Dependency injection container and service registry for the desktop application.
 */
public class ServiceRegistry {

    private static ServiceRegistry instance;

    private final DatabaseManager databaseManager;
    private final ResourceRepository resourceRepository;
    private final RequestRepository requestRepository;
    private final AllocationRunRepository allocationRunRepository;
    private final ScenarioRepository scenarioRepository;

    private final AllocationEngine allocationEngine;
    private final ResourceService resourceService;
    private final RequestService requestService;
    private final AllocationService allocationService;
    private final WhatIfService whatIfService;
    private final HistoryService historyService;

    private ServiceRegistry(DatabaseManager dbManager) {
        this.databaseManager = dbManager;
        this.databaseManager.initializeSchema();

        this.resourceRepository = new SqliteResourceRepository(this.databaseManager);
        this.requestRepository = new SqliteRequestRepository(this.databaseManager);
        this.allocationRunRepository = new SqliteAllocationRunRepository(this.databaseManager);
        this.scenarioRepository = new SqliteScenarioRepository(this.databaseManager);

        this.allocationEngine = new AllocationEngine();
        this.resourceService = new ResourceService(this.resourceRepository);
        this.requestService = new RequestService(this.requestRepository, this.resourceRepository);
        this.allocationService = new AllocationService(this.allocationEngine, this.resourceRepository, this.requestRepository, this.allocationRunRepository);
        this.whatIfService = new WhatIfService(this.allocationEngine, this.resourceRepository, this.requestRepository, this.allocationRunRepository, this.scenarioRepository);
        this.historyService = new HistoryService(this.allocationRunRepository, this.allocationEngine.getAdaptiveReallocator());
    }

    public static synchronized ServiceRegistry getInstance() {
        if (instance == null) {
            instance = new ServiceRegistry(new DatabaseManager());
        }
        return instance;
    }

    /**
     * Initializes the registry with a custom database manager (e.g. For in-memory integration testing).
     */
    public static synchronized ServiceRegistry initializeWithCustomDb(DatabaseManager customDbManager) {
        instance = new ServiceRegistry(customDbManager);
        return instance;
    }

    public DatabaseManager getDatabaseManager() { return databaseManager; }
    public ResourceRepository getResourceRepository() { return resourceRepository; }
    public RequestRepository getRequestRepository() { return requestRepository; }
    public AllocationRunRepository getAllocationRunRepository() { return allocationRunRepository; }
    public ScenarioRepository getScenarioRepository() { return scenarioRepository; }
    public AllocationEngine getAllocationEngine() { return allocationEngine; }
    public ResourceService getResourceService() { return resourceService; }
    public RequestService getRequestService() { return requestService; }
    public AllocationService getAllocationService() { return allocationService; }
    public WhatIfService getWhatIfService() { return whatIfService; }
    public HistoryService getHistoryService() { return historyService; }
}
