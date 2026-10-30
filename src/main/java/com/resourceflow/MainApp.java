package com.resourceflow;

import com.resourceflow.service.ServiceRegistry;
import com.resourceflow.ui.MainLayout;
import com.resourceflow.ui.util.DialogHelper;
import com.resourceflow.util.AuditLog;
import com.resourceflow.util.DemoDataSeeder;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.net.URL;

/**
 * Main JavaFX application entry point for ResourceFlow.
 */
public class MainApp extends Application {

    @Override
    public void init() {
        // Started by Launcher normally; also start it when MainApp is launched directly (mvn javafx:run)
        if (AuditLog.currentFile() == null) {
            Launcher.startAuditLog();
        }
    }

    @Override
    public void stop() {
        AuditLog.record("Desktop window closed");
    }

    @Override
    public void start(Stage primaryStage) {
        // 0. Uncaught exceptions on the JavaFX thread: log them and show a dialog instead of failing silently
        Thread.currentThread().setUncaughtExceptionHandler((thread, error) -> {
            AuditLog.record("UNCAUGHT on %s: %s", thread.getName(), error);
            Platform.runLater(() -> DialogHelper.showError("Unexpected Error",
                    error.getClass().getSimpleName(), String.valueOf(error.getMessage())));
        });

        // 1. Initialize Application Service Registry & SQLite database
        ServiceRegistry registry = ServiceRegistry.getInstance();

        // 2. Automatically seed demo resources and requests on first launch if inventory is empty
        DemoDataSeeder.seedDemoDataIfEmpty(registry.getResourceService(), registry.getRequestService());

        // 3. Build Main UI Layout
        MainLayout rootLayout = new MainLayout(registry);
        Scene scene = new Scene(rootLayout, 1280, 800);

        // 4. Attach Modern Dark Stylesheet
        URL cssUrl = getClass().getResource("/css/styles.css");
        if (cssUrl != null) {
            scene.getStylesheets().add(cssUrl.toExternalForm());
        }

        // 5. Configure Desktop Stage
        primaryStage.setTitle("ResourceFlow — Adaptive Resource Allocation & Optimisation System");
        primaryStage.setMinWidth(1080);
        primaryStage.setMinHeight(680);
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
