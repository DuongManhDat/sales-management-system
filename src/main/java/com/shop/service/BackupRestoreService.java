package com.shop.service;

import com.shop.util.DBConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class BackupRestoreService {
    private static final Logger log = LoggerFactory.getLogger(BackupRestoreService.class);

    public File backupDatabase(File destinationDir) throws IOException {
        String dbPath = DBConnection.getDbPath();
        if (dbPath == null || dbPath.isEmpty()) {
            throw new IllegalStateException("Không tìm thấy đường dẫn Database hiện tại.");
        }

        File dbFile = new File(dbPath);
        if (!dbFile.exists()) {
            throw new IOException("File cơ sở dữ liệu không tồn tại: " + dbPath);
        }

        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String backupFileName = "shop_backup_" + timestamp + ".db";
        File backupFile = new File(destinationDir, backupFileName);

        Files.copy(dbFile.toPath(), backupFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
        log.info("Sao lưu dữ liệu thành công đến: {}", backupFile.getAbsolutePath());
        return backupFile;
    }

    public void restoreDatabase(File sourceFile) throws IOException {
        if (!sourceFile.exists() || !sourceFile.getName().endsWith(".db")) {
            throw new IllegalArgumentException("File khôi phục không hợp lệ.");
        }

        String dbPath = DBConnection.getDbPath();
        if (dbPath == null || dbPath.isEmpty()) {
            throw new IllegalStateException("Không tìm thấy đường dẫn Database hiện tại.");
        }

        File dbFile = new File(dbPath);

        // Đóng toàn bộ kết nối để giải phóng file lock
        DBConnection.close();

        // Ghi đè file
        Files.copy(sourceFile.toPath(), dbFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
        log.info("Khôi phục dữ liệu thành công từ: {}", sourceFile.getAbsolutePath());
    }
}
