package com.resourceflow.model;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Encapsulates a What-If analysis scenario containing isolated hypothetical inputs.
 */
public class Scenario implements Serializable {

    /** Version stamp checked by Java Serialization when a saved .rflow file is read back. */
    private static final long serialVersionUID = 1L;

    private String scenarioId;
    private String name;
    private String description;
    private String baselineRunId;
    private List<Resource> modifiedResources;
    private List<AllocationRequest> modifiedRequests;
    private StrategyType strategyType;
    private ConstraintConfig config;
    private LocalDateTime createdAt;

    public Scenario() {
        this.scenarioId = UUID.randomUUID().toString();
        this.modifiedResources = new ArrayList<>();
        this.modifiedRequests = new ArrayList<>();
        this.strategyType = StrategyType.PRIORITY_BASED;
        this.config = ConstraintConfig.defaultConfig();
        this.createdAt = LocalDateTime.now();
    }

    public Scenario(String scenarioId, String name, String description, String baselineRunId,
                    List<Resource> modifiedResources, List<AllocationRequest> modifiedRequests,
                    StrategyType strategyType, ConstraintConfig config) {
        this.scenarioId = scenarioId != null ? scenarioId : UUID.randomUUID().toString();
        this.name = name != null ? name.trim() : "Scenario";
        this.description = description != null ? description.trim() : "";
        this.baselineRunId = baselineRunId;
        this.modifiedResources = (modifiedResources != null) ? new ArrayList<>(modifiedResources) : new ArrayList<>();
        this.modifiedRequests = (modifiedRequests != null) ? new ArrayList<>(modifiedRequests) : new ArrayList<>();
        this.strategyType = (strategyType != null) ? strategyType : StrategyType.PRIORITY_BASED;
        this.config = (config != null) ? config : ConstraintConfig.defaultConfig();
        this.createdAt = LocalDateTime.now();
    }

    public String getScenarioId() {
        return scenarioId;
    }

    public void setScenarioId(String scenarioId) {
        this.scenarioId = scenarioId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getBaselineRunId() {
        return baselineRunId;
    }

    public void setBaselineRunId(String baselineRunId) {
        this.baselineRunId = baselineRunId;
    }

    public List<Resource> getModifiedResources() {
        return modifiedResources;
    }

    public void setModifiedResources(List<Resource> modifiedResources) {
        this.modifiedResources = (modifiedResources != null) ? new ArrayList<>(modifiedResources) : new ArrayList<>();
    }

    public List<AllocationRequest> getModifiedRequests() {
        return modifiedRequests;
    }

    public void setModifiedRequests(List<AllocationRequest> modifiedRequests) {
        this.modifiedRequests = (modifiedRequests != null) ? new ArrayList<>(modifiedRequests) : new ArrayList<>();
    }

    public StrategyType getStrategyType() {
        return strategyType;
    }

    public void setStrategyType(StrategyType strategyType) {
        this.strategyType = strategyType;
    }

    public ConstraintConfig getConfig() {
        return config;
    }

    public void setConfig(ConstraintConfig config) {
        this.config = config;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
