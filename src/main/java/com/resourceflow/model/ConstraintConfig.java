package com.resourceflow.model;

import java.io.Serializable;

/**
 * Configuration options and weights for hard and soft constraint validation.
 */
public class ConstraintConfig implements Serializable {

    /** Version stamp checked by Java Serialization when a saved .rflow file is read back. */
    private static final long serialVersionUID = 1L;

    private boolean enforceAllOrNothing;
    private boolean enforceMinimumAcceptable;
    private boolean respectInactiveStatus;
    private double utilisationWeight;
    private double fairnessWeight;
    private double fulfilmentWeight;

    public ConstraintConfig() {
        this.enforceAllOrNothing = true;
        this.enforceMinimumAcceptable = true;
        this.respectInactiveStatus = true;
        this.utilisationWeight = 0.35;
        this.fairnessWeight = 0.35;
        this.fulfilmentWeight = 0.30;
    }

    public static ConstraintConfig defaultConfig() {
        return new ConstraintConfig();
    }

    public boolean isEnforceAllOrNothing() {
        return enforceAllOrNothing;
    }

    public void setEnforceAllOrNothing(boolean enforceAllOrNothing) {
        this.enforceAllOrNothing = enforceAllOrNothing;
    }

    public boolean isEnforceMinimumAcceptable() {
        return enforceMinimumAcceptable;
    }

    public void setEnforceMinimumAcceptable(boolean enforceMinimumAcceptable) {
        this.enforceMinimumAcceptable = enforceMinimumAcceptable;
    }

    public boolean isRespectInactiveStatus() {
        return respectInactiveStatus;
    }

    public void setRespectInactiveStatus(boolean respectInactiveStatus) {
        this.respectInactiveStatus = respectInactiveStatus;
    }

    public double getUtilisationWeight() {
        return utilisationWeight;
    }

    public void setUtilisationWeight(double utilisationWeight) {
        this.utilisationWeight = utilisationWeight;
    }

    public double getFairnessWeight() {
        return fairnessWeight;
    }

    public void setFairnessWeight(double fairnessWeight) {
        this.fairnessWeight = fairnessWeight;
    }

    public double getFulfilmentWeight() {
        return fulfilmentWeight;
    }

    public void setFulfilmentWeight(double fulfilmentWeight) {
        this.fulfilmentWeight = fulfilmentWeight;
    }
}
