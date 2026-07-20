package com.shop;

import com.shop.infra.db.SchemaInitializer;
import com.shop.infra.db.DBConnection;
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
        
        // Initialize database schema
        SchemaInitializer.initialize();
        
        // Load main window
        Parent root = FXMLLoader.load(getClass().getResource("/fxml/main.fxml"));
        primaryStage.setTitle("Hệ thống quản lý bán hàng");
        primaryStage.setScene(new Scene(root, 1024, 768));
        primaryStage.show();
    }
    
    @Override
    public void stop() throws Exception {
        logger.info("Shutting down application...");
        DBConnection.closeConnection();
        super.stop();
    }

    public static void main(String[] args) {
        // Set default uncaught exception handler
        Thread.setDefaultUncaughtExceptionHandler((t, e) -> 
            logger.error("Uncaught exception in thread " + t.getName(), e));
            
        launch(args);
    }
}
