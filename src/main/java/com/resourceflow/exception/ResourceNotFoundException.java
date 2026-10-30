package com.resourceflow.exception;

/**
 * Thrown when a requested resource or entity does not exist.
 */
public class ResourceNotFoundException extends ResourceFlowException {
    public ResourceNotFoundException(String message) {
        super(message);
    }
}
