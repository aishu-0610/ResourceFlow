package com.resourceflow.ui.util;

import javafx.application.Platform;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Utility executing long-running computational or database operations
 * on background worker threads while cleanly updating JavaFX components on the UI thread.
 */
public class TaskRunner {

    private static final ExecutorService EXECUTOR = Executors.newFixedThreadPool(4, r -> {
        Thread t = new Thread(r, "ResourceFlow-Worker");
        t.setDaemon(true);
        return t;
    });

    /**
     * Executes a supplier asynchronously and returns the result to the JavaFX Application Thread.
     *
     * @param task      Background work supplier
     * @param onSuccess Callback executed on JavaFX thread on success
     * @param onError   Callback executed on JavaFX thread on failure
     */
    public static <T> void executeAsync(Supplier<T> task, Consumer<T> onSuccess, Consumer<Throwable> onError) {
        CompletableFuture.supplyAsync(task, EXECUTOR)
                .thenAccept(result -> Platform.runLater(() -> {
                    if (onSuccess != null) {
                        onSuccess.accept(result);
                    }
                }))
                .exceptionally(ex -> {
                    Platform.runLater(() -> {
                        if (onError != null) {
                            onError.accept(ex.getCause() != null ? ex.getCause() : ex);
                        }
                    });
                    return null;
                });
    }

    /**
     * Executes a runnable task asynchronously.
     */
    public static void executeAsync(Runnable task, Runnable onSuccess, Consumer<Throwable> onError) {
        CompletableFuture.runAsync(task, EXECUTOR)
                .thenRun(() -> Platform.runLater(() -> {
                    if (onSuccess != null) {
                        onSuccess.run();
                    }
                }))
                .exceptionally(ex -> {
                    Platform.runLater(() -> {
                        if (onError != null) {
                            onError.accept(ex.getCause() != null ? ex.getCause() : ex);
                        }
                    });
                    return null;
                });
    }
}
