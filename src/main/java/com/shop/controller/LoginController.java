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

public class LoginController {
    private static final Logger log = LoggerFactory.getLogger(LoginController.class);

    @FXML private PasswordField passwordField;
    @FXML private Label errorLabel;
    @FXML private Button loginButton;
    @FXML private ProgressIndicator progressIndicator;

    private AuthService authService;

    public void initialize() {
        authService = new AuthService();
    }

    @FXML
    protected void onLoginButtonClick() {
        String password = passwordField.getText();
        if (password == null || password.isEmpty()) {
            showError("Vui lòng nhập mật khẩu");
            return;
        }

        setLoading(true);

        Task<Boolean> loginTask = new Task<>() {
            @Override
            protected Boolean call() throws Exception {
                return authService.login(password);
            }
        };

        loginTask.setOnSucceeded(e -> {
            boolean success = loginTask.getValue();
            if (success) {
                log.info("Đăng nhập thành công");
                Platform.runLater(() -> {
                    try {
                        com.shop.util.SceneManager.switchScene("/fxml/main.fxml", "Hệ thống quản lý bán hàng");
                    } catch (Exception ex) {
                        showError("Lỗi chuyển màn hình: " + ex.getMessage());
                    }
                });
            } else {
                showError("Mật khẩu không đúng");
                setLoading(false);
            }
        });

        loginTask.setOnFailed(e -> {
            log.error("Lỗi đăng nhập", loginTask.getException());
            showError("Lỗi hệ thống");
            setLoading(false);
        });

        new Thread(loginTask).start();
    }

    private void showError(String message) {
        Platform.runLater(() -> {
            errorLabel.setText(message);
            errorLabel.setVisible(true);
        });
    }

    private void setLoading(boolean loading) {
        Platform.runLater(() -> {
            loginButton.setDisable(loading);
            passwordField.setDisable(loading);
            progressIndicator.setVisible(loading);
            if (loading) errorLabel.setVisible(false);
        });
    }
}
