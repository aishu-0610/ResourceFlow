package com.resourceflow.exception;

/**
 * Thrown when an underlying SQLite JDBC operation or transaction fails.
 */
public class DatabasePersistenceException extends ResourceFlowException {
    public DatabasePersistenceException(String message) {
        super(message);
    }

    public DatabasePersistenceException(String message, Throwable cause) {
        super(message, cause);
    }
}
