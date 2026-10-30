package com.resourceflow.repository;

import com.resourceflow.model.Scenario;

import java.util.List;
import java.util.Optional;

/**
 * Data access operations for What-If scenarios.
 */
public interface ScenarioRepository {
    void save(Scenario scenario);
    void update(Scenario scenario);
    void deleteById(String scenarioId);
    Optional<Scenario> findById(String scenarioId);
    List<Scenario> findAll();
}
