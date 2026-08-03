package com.shop;

import com.shop.util.DBConnection;
import com.shop.util.GlobalExceptionHandler;
import com.shop.util.SceneManager;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class App extends Application {
    private static final Logger logger = LoggerFactory.getLogger(App.class);

    @Override
    public void start(Stage primaryStage) throws Exception {
        logger.info("Starting Sales Management System...");
        
        // Set the primary stage for SceneManager
        SceneManager.setPrimaryStage(primaryStage);
        
        // Load main window
        SceneManager.switchScene("/fxml/main.fxml", "Hệ thống quản lý bán hàng");
    }
    
    @Override
    public void stop() throws Exception {
        logger.info("Shutting down application...");
        DBConnection.close();
        super.stop();
    }

    public static void main(String[] args) {
        // Set default uncaught exception handler
        Thread.setDefaultUncaughtExceptionHandler(new GlobalExceptionHandler());
            
        launch(args);
    }
}
