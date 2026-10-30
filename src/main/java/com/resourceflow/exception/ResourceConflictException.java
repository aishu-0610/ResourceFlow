package com.resourceflow.exception;

/**
 * Thrown when an operation cannot be completed due to a state conflict,
 * such as attempting to delete a resource referenced by historical allocation runs.
 */
public class ResourceConflictException extends ResourceFlowException {
    public ResourceConflictException(String message) {
        super(message);
    }
}
