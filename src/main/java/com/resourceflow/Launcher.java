package com.resourceflow;

import com.resourceflow.console.ConsoleApp;
import com.resourceflow.database.DatabaseManager;
import com.resourceflow.util.AuditLog;

import java.nio.file.Path;

/**
 * Program entry point (the JVM starts it on the "main" thread).
 *
 *   java -jar resourceflow-1.0.0.jar             -> JavaFX desktop app
 *   java -jar resourceflow-1.0.0.jar --console   -> text menu in the terminal
 *
 * Kept separate from MainApp because a shaded JAR cannot start a class that extends
 * javafx.application.Application directly.
 */
public class Launcher {

    public static void main(String[] args) {
        startAuditLog();

        // Last-resort handler for exceptions nobody caught on any thread
        Thread.setDefaultUncaughtExceptionHandler((thread, error) -> {
            AuditLog.record("UNCAUGHT on %s: %s", thread.getName(), error);
            System.err.println("Unexpected error on thread " + thread.getName() + ": " + error);
        });

        boolean consoleMode = args.length > 0 && args[0].equals("--console");
        if (consoleMode) {
            ConsoleApp.main(args);
        } else {
            MainApp.main(args);
        }
        AuditLog.stop();
    }

    /** Starts the background audit writer; the log file lives next to the SQLite database. */
    static void startAuditLog() {
        Path dbFile = Path.of(DatabaseManager.resolveDefaultDbPath());
        AuditLog.start(dbFile.resolveSibling("audit.log"));
        AuditLog.record("ResourceFlow started (Java %s)", System.getProperty("java.version"));
    }
}
