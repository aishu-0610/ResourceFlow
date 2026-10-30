package com.resourceflow.exception;

/**
 * Base unchecked exception for all ResourceFlow domain and application errors.
 */
public class ResourceFlowException extends RuntimeException {
    public ResourceFlowException(String message) {
        super(message);
    }

    public ResourceFlowException(String message, Throwable cause) {
        super(message, cause);
    }
}
