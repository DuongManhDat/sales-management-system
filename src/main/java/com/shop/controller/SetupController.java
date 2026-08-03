package com.shop.controller;

import com.shop.App;
import com.shop.service.AuthService;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.ProgressIndicator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SetupController {
    private static final Logger log = LoggerFactory.getLogger(SetupController.class);

    @FXML private PasswordField passwordField;
    @FXML private PasswordField confirmPasswordField;
    @FXML private Label errorLabel;
    @FXML private Button setupButton;
    @FXML private ProgressIndicator progressIndicator;

    private AuthService authService;

    public void initialize() {
        authService = new AuthService();
    }

    @FXML
    protected void onSetupButtonClick() {
        String pwd = passwordField.getText();
        String confirm = confirmPasswordField.getText();

        if (pwd == null || pwd.length() < 6) {
            showError("Mật khẩu phải dài ít nhất 6 ký tự");
            return;
        }

        if (!pwd.equals(confirm)) {
            showError("Mật khẩu xác nhận không khớp");
            return;
        }

        setLoading(true);

        Task<Void> setupTask = new Task<>() {
            @Override
            protected Void call() throws Exception {
                authService.setup(pwd);
                return null;
            }
        };

        setupTask.setOnSucceeded(e -> {
            log.info("Thiết lập mật khẩu thành công");
            Platform.runLater(() -> {
                try {
                    com.shop.util.SceneManager.switchScene("/fxml/login-view.fxml", "Đăng nhập hệ thống");
                } catch (Exception ex) {
                    showError("Lỗi: " + ex.getMessage());
                }
            });
        });

        setupTask.setOnFailed(e -> {
            log.error("Lỗi thiết lập mật khẩu", setupTask.getException());
            showError("Lỗi hệ thống");
            setLoading(false);
        });

        new Thread(setupTask).start();
    }

    private void showError(String message) {
        Platform.runLater(() -> {
            errorLabel.setText(message);
            errorLabel.setVisible(true);
        });
    }

    private void setLoading(boolean loading) {
        Platform.runLater(() -> {
            setupButton.setDisable(loading);
            passwordField.setDisable(loading);
            confirmPasswordField.setDisable(loading);
            progressIndicator.setVisible(loading);
            if (loading) errorLabel.setVisible(false);
        });
    }
}
