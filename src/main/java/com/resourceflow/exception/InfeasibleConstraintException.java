package com.resourceflow.exception;

/**
 * Thrown when an allocation plan violates a non-negotiable hard constraint.
 */
public class InfeasibleConstraintException extends ResourceFlowException {
    public InfeasibleConstraintException(String message) {
        super(message);
    }
}
