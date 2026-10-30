package com.resourceflow.exception;

/**
 * Thrown when a scenario file cannot be written or read.
 *
 * Unlike ResourceFlowException (unchecked, extends RuntimeException), this is a CHECKED
 * exception: it extends Exception, so every method that can throw it must declare
 * "throws ScenarioFileException" and every caller must catch it or declare it too.
 * File problems are expected and recoverable (wrong file chosen, disk full), so the
 * compiler should force callers to handle them.
 */
public class ScenarioFileException extends Exception {

    public ScenarioFileException(String message) {
        super(message);
    }

    /** Chained-exception constructor: keeps the low-level cause (e.g. an IOException) for diagnosis. */
    public ScenarioFileException(String message, Throwable cause) {
        super(message, cause);
    }
}
