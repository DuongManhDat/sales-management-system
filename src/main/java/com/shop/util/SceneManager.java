package com.shop.util;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class SceneManager {
    private static final Logger log = LoggerFactory.getLogger(SceneManager.class);
    private static Stage primaryStage;

    public static void setPrimaryStage(Stage stage) {
        primaryStage = stage;
    }

    public static Stage getPrimaryStage() {
        return primaryStage;
    }

    public static void switchScene(String fxmlFile, String title) {
        try {
            FXMLLoader loader = new FXMLLoader(SceneManager.class.getResource(fxmlFile));
            Parent root = loader.load();
            Scene scene = new Scene(root);
            primaryStage.setTitle(title);
            primaryStage.setScene(scene);
            primaryStage.show();
            log.info("Chuyển màn hình tới: {}", fxmlFile);
        } catch (IOException e) {
            log.error("Không thể load giao diện: {}", fxmlFile, e);
            DialogHelper.showError("Lỗi giao diện", "Không thể tải giao diện: " + fxmlFile);
        }
    }
    
    // Hỗ trợ lấy FXMLLoader để truyền data cho Controller
    public static FXMLLoader getLoader(String fxmlFile) {
        return new FXMLLoader(SceneManager.class.getResource(fxmlFile));
    }
}
