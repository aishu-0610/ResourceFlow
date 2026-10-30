package com.resourceflow.ui.util;

import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.DialogPane;
import javafx.stage.Modality;

import java.util.Optional;

/**
 * Utility for presenting user-friendly alerts, confirmations, and error dialogs.
 */
public class DialogHelper {

    public static void showInfo(String title, String header, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        styleDialog(alert, title, header, content);
        alert.showAndWait();
    }

    public static void showWarning(String title, String header, String content) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        styleDialog(alert, title, header, content);
        alert.showAndWait();
    }

    public static void showError(String title, String header, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        styleDialog(alert, title, header, content);
        alert.showAndWait();
    }

    public static boolean showConfirmation(String title, String header, String content) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        styleDialog(alert, title, header, content);
        Optional<ButtonType> result = alert.showAndWait();
        return result.isPresent() && result.get() == ButtonType.OK;
    }

    private static void styleDialog(Alert alert, String title, String header, String content) {
        alert.initModality(Modality.APPLICATION_MODAL);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);

        DialogPane pane = alert.getDialogPane();
        pane.getStyleClass().add("card");
        try {
            String css = DialogHelper.class.getResource("/css/styles.css").toExternalForm();
            pane.getStylesheets().add(css);
        } catch (Exception ignored) {}
    }
}
