package com.resourceflow.model;

import java.util.*;

/**
 * Encapsulates computed mathematical evaluation metrics for an allocation plan.
 */
public class EvaluationMetrics {
    private double aggregateUtilisation; // Average / aggregate resource utilisation (%)
    private double overallFulfilment;    // Demand-weighted total fulfilment (%)
    private double averageFulfilment;    // Arithmetic mean of individual request fulfilment (%)
    private double jainsFairnessIndex;   // Jain's fairness index [0.0 to 1.0]
    private int fullySatisfiedCount;     // Requests with 100% allocation
    private int partiallySatisfiedCount; // Requests with 0% < allocation < 100%
    private int unsatisfiedCount;        // Requests with 0% allocation
    private double unallocatedCapacity;  // Total unallocated capacity across active resources
    private Map<String, Double> resourceUtilisationMap; // resourceId -> utilisation %
    private List<String> constraintViolations;

    public EvaluationMetrics() {
        this.resourceUtilisationMap = new LinkedHashMap<>();
        this.constraintViolations = new ArrayList<>();
    }

    public EvaluationMetrics(double aggregateUtilisation, double overallFulfilment,
                             double averageFulfilment, double jainsFairnessIndex,
                             int fullySatisfiedCount, int partiallySatisfiedCount,
                             int unsatisfiedCount, double unallocatedCapacity,
                             Map<String, Double> resourceUtilisationMap,
                             List<String> constraintViolations) {
        this.aggregateUtilisation = aggregateUtilisation;
        this.overallFulfilment = overallFulfilment;
        this.averageFulfilment = averageFulfilment;
        this.jainsFairnessIndex = jainsFairnessIndex;
        this.fullySatisfiedCount = fullySatisfiedCount;
        this.partiallySatisfiedCount = partiallySatisfiedCount;
        this.unsatisfiedCount = unsatisfiedCount;
        this.unallocatedCapacity = unallocatedCapacity;
        this.resourceUtilisationMap = (resourceUtilisationMap != null) ? new LinkedHashMap<>(resourceUtilisationMap) : new LinkedHashMap<>();
        this.constraintViolations = (constraintViolations != null) ? new ArrayList<>(constraintViolations) : new ArrayList<>();
    }

    public boolean isFeasible() {
        return constraintViolations.isEmpty();
    }

    public int getTotalRequests() {
        return fullySatisfiedCount + partiallySatisfiedCount + unsatisfiedCount;
    }

    public double getAggregateUtilisation() {
        return aggregateUtilisation;
    }

    public void setAggregateUtilisation(double aggregateUtilisation) {
        this.aggregateUtilisation = aggregateUtilisation;
    }

    public double getOverallFulfilment() {
        return overallFulfilment;
    }

    public void setOverallFulfilment(double overallFulfilment) {
        this.overallFulfilment = overallFulfilment;
    }

    public double getAverageFulfilment() {
        return averageFulfilment;
    }

    public void setAverageFulfilment(double averageFulfilment) {
        this.averageFulfilment = averageFulfilment;
    }

    public double getJainsFairnessIndex() {
        return jainsFairnessIndex;
    }

    public void setJainsFairnessIndex(double jainsFairnessIndex) {
        this.jainsFairnessIndex = jainsFairnessIndex;
    }

    public int getFullySatisfiedCount() {
        return fullySatisfiedCount;
    }

    public void setFullySatisfiedCount(int fullySatisfiedCount) {
        this.fullySatisfiedCount = fullySatisfiedCount;
    }

    public int getPartiallySatisfiedCount() {
        return partiallySatisfiedCount;
    }

    public void setPartiallySatisfiedCount(int partiallySatisfiedCount) {
        this.partiallySatisfiedCount = partiallySatisfiedCount;
    }

    public int getUnsatisfiedCount() {
        return unsatisfiedCount;
    }

    public void setUnsatisfiedCount(int unsatisfiedCount) {
        this.unsatisfiedCount = unsatisfiedCount;
    }

    public double getUnallocatedCapacity() {
        return unallocatedCapacity;
    }

    public void setUnallocatedCapacity(double unallocatedCapacity) {
        this.unallocatedCapacity = unallocatedCapacity;
    }

    public Map<String, Double> getResourceUtilisationMap() {
        return Collections.unmodifiableMap(resourceUtilisationMap);
    }

    public void setResourceUtilisationMap(Map<String, Double> resourceUtilisationMap) {
        this.resourceUtilisationMap = (resourceUtilisationMap != null) ? new LinkedHashMap<>(resourceUtilisationMap) : new LinkedHashMap<>();
    }

    public List<String> getConstraintViolations() {
        return Collections.unmodifiableList(constraintViolations);
    }

    public void setConstraintViolations(List<String> constraintViolations) {
        this.constraintViolations = (constraintViolations != null) ? new ArrayList<>(constraintViolations) : new ArrayList<>();
    }

    public void addConstraintViolation(String violation) {
        if (violation != null && !violation.trim().isEmpty()) {
            this.constraintViolations.add(violation.trim());
        }
    }

    /**
     * Combines the three objectives into one score in [0, 1] using the user's weights:
     * (wU * utilisation + wF * fulfilment + wJ * fairness) / (wU + wF + wJ).
     * Utilisation and fulfilment are stored as percentages, so they are divided by 100 first.
     */
    public double weightedScore(ConstraintConfig config) {
        double wU = Math.max(0.0, config.getUtilisationWeight());
        double wF = Math.max(0.0, config.getFulfilmentWeight());
        double wJ = Math.max(0.0, config.getFairnessWeight());
        double totalWeight = wU + wF + wJ;
        if (totalWeight == 0.0) {
            return 0.0;
        }
        double score = wU * (aggregateUtilisation / 100.0)
                + wF * (overallFulfilment / 100.0)
                + wJ * jainsFairnessIndex;
        return score / totalWeight;
    }

    @Override
    public String toString() {
        return String.format(Locale.US,
                "Metrics[Util: %.1f%%, Fulfilment: %.1f%%, Fairness: %.3f, Satisfied: %d, Partial: %d, Unsatisfied: %d, Feasible: %s]",
                aggregateUtilisation, overallFulfilment, jainsFairnessIndex,
                fullySatisfiedCount, partiallySatisfiedCount, unsatisfiedCount, isFeasible());
    }
}
