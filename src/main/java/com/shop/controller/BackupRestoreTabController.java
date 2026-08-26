package com.shop.controller;

import com.shop.service.BackupRestoreService;
import com.shop.util.DialogHelper;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Window;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

public class BackupRestoreTabController {
    private static final Logger log = LoggerFactory.getLogger(BackupRestoreTabController.class);
    private BackupRestoreService backupRestoreService;

    @FXML
    public void initialize() {
        backupRestoreService = new BackupRestoreService();
    }

    @FXML
    public void handleBackup() {
        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle("Chọn thư mục để lưu bản sao lưu");
        
        Window window = javafx.stage.Window.getWindows().stream().filter(Window::isShowing).findFirst().orElse(null);
        File selectedDirectory = directoryChooser.showDialog(window);

        if (selectedDirectory != null) {
            try {
                File backupFile = backupRestoreService.backupDatabase(selectedDirectory);
                DialogHelper.showInfo("Thành công", "Đã sao lưu cơ sở dữ liệu thành công tới:\n" + backupFile.getAbsolutePath());
            } catch (Exception e) {
                log.error("Lỗi khi sao lưu dữ liệu", e);
                DialogHelper.showError("Lỗi sao lưu", "Đã xảy ra lỗi khi sao lưu dữ liệu: " + e.getMessage());
            }
        }
    }

    @FXML
    public void handleRestore() {
        DialogHelper.showConfirm("Xác nhận Khôi phục", 
            "Khôi phục dữ liệu sẽ GHI ĐÈ toàn bộ cơ sở dữ liệu hiện tại. Thao tác này KHÔNG THỂ HOÀN TÁC. Bạn có chắc chắn muốn tiếp tục?",
            () -> {
                FileChooser fileChooser = new FileChooser();
                fileChooser.setTitle("Chọn file sao lưu (Database)");
                fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("SQLite Database (*.db, *.bak)", "*.db", "*.bak"));
                
                Window window = javafx.stage.Window.getWindows().stream().filter(Window::isShowing).findFirst().orElse(null);
                File selectedFile = fileChooser.showOpenDialog(window);

                if (selectedFile != null) {
                    try {
                        backupRestoreService.restoreDatabase(selectedFile);
                        DialogHelper.showInfo("Khôi phục thành công", "Cơ sở dữ liệu đã được khôi phục. Ứng dụng sẽ tự động thoát. Vui lòng mở lại ứng dụng để sử dụng dữ liệu mới.");
                        Platform.exit();
                        System.exit(0);
                    } catch (Exception e) {
                        log.error("Lỗi khi khôi phục dữ liệu", e);
                        DialogHelper.showError("Lỗi khôi phục", "Đã xảy ra lỗi khi khôi phục dữ liệu: " + e.getMessage());
                    }
                }
            });
    }
}
