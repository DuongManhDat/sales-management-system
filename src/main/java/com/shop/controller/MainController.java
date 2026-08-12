package com.shop.controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.util.Duration;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.animation.Interpolator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class MainController {
    private static final Logger log = LoggerFactory.getLogger(MainController.class);

    @FXML
    private BorderPane mainPane;
    
    @FXML
    private VBox sidebar;

    private final double SIDEBAR_WIDTH = 200.0;
    private boolean isSidebarVisible = true;

    @FXML
    private void toggleSidebar() {
        Timeline timeline = new Timeline();
        if (isSidebarVisible) {
            // Thu nhỏ sidebar
            sidebar.setMinWidth(0);
            KeyValue kv = new KeyValue(sidebar.prefWidthProperty(), 0, Interpolator.EASE_BOTH);
            KeyFrame kf = new KeyFrame(Duration.millis(250), kv);
            timeline.getKeyFrames().add(kf);
            timeline.setOnFinished(e -> {
                sidebar.setVisible(false);
                sidebar.setManaged(false);
            });
            isSidebarVisible = false;
        } else {
            // Mở rộng sidebar
            sidebar.setVisible(true);
            sidebar.setManaged(true);
            KeyValue kv = new KeyValue(sidebar.prefWidthProperty(), SIDEBAR_WIDTH, Interpolator.EASE_BOTH);
            KeyFrame kf = new KeyFrame(Duration.millis(250), kv);
            timeline.getKeyFrames().add(kf);
            isSidebarVisible = true;
        }
        timeline.play();
    }

    @FXML
    private void showDashboard() {
        showPlaceholder("Dashboard");
    }

    @FXML
    private void showProducts() {
        showPlaceholder("Hàng hóa");
    }

    @FXML
    private void showCustomers() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/customer-list.fxml"));
            Parent view = loader.load();
            mainPane.setCenter(view);
        } catch (IOException e) {
            log.error("Failed to load customer list view", e);
        }
    }

    @FXML
    private void showPurchases() {
        showPlaceholder("Mua hàng");
    }

    @FXML
    private void showSales() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/pos-view.fxml"));
            Parent view = loader.load();
            mainPane.setCenter(view);
        } catch (IOException e) {
            log.error("Failed to load pos view", e);
        }
    }

    @FXML
    private void showInvoices() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/invoice-list-view.fxml"));
            Parent view = loader.load();
            mainPane.setCenter(view);
        } catch (IOException e) {
            log.error("Failed to load invoice list view", e);
        }
    }

    @FXML
    private void showSettings() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/settings-view.fxml"));
            Parent view = loader.load();
            mainPane.setCenter(view);
        } catch (IOException e) {
            log.error("Failed to load settings view", e);
        }
    }

    private void showPlaceholder(String text) {
        Label label = new Label(text);
        label.setFont(new Font(24.0));
        mainPane.setCenter(label);
    }
}
