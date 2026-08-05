package com.shop;

import com.shop.infra.db.SchemaInitializer;
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
import atlantafx.base.theme.PrimerLight;

public class App extends Application {
    private static final Logger logger = LoggerFactory.getLogger(App.class);

    @Override
    public void start(Stage primaryStage) throws Exception {
        logger.info("Starting Sales Management System...");
        
        // Khởi tạo DB Schema
        SchemaInitializer.initialize();

        // Khởi tạo theme AtlantaFX (ví dụ: PrimerLight)
        Application.setUserAgentStylesheet(new PrimerLight().getUserAgentStylesheet());

        // Set the primary stage for SceneManager
        SceneManager.setPrimaryStage(primaryStage);
        
        com.shop.dao.AppUserDao dao = new com.shop.dao.AppUserDao();
        if (dao.findFirst().isPresent()) {
            SceneManager.switchScene("/fxml/login-view.fxml", "Đăng nhập hệ thống");
        } else {
            SceneManager.switchScene("/fxml/setup-view.fxml", "Thiết lập mật khẩu chủ cửa hàng");
        }
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
