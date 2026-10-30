package com.resourceflow.exception;

/**
 * Thrown when an input entity fails domain or business rule validations.
 */
public class ResourceValidationException extends ResourceFlowException {
    public ResourceValidationException(String message) {
        super(message);
    }
}
