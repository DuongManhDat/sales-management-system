package com.shop.util;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.sql.Connection;
import java.sql.SQLException;

public class DBConnection {
    private static final Logger log = LoggerFactory.getLogger(DBConnection.class);
    private static HikariDataSource dataSource;

    static {
        try {
            // Lấy thư mục %APPDATA%
            String appData = System.getenv("APPDATA");
            if (appData == null) {
                appData = System.getProperty("user.home"); // Fallback cho Linux/Mac nếu có
            }
            
            File dataDir = new File(appData, "ShopManager/data");
            if (!dataDir.exists()) {
                if (dataDir.mkdirs()) {
                    log.info("Đã tạo thư mục dữ liệu: {}", dataDir.getAbsolutePath());
                } else {
                    log.error("Không thể tạo thư mục dữ liệu: {}", dataDir.getAbsolutePath());
                }
            }

            String dbPath = dataDir.getAbsolutePath() + "/shop.db";
            String url = System.getProperty("db.url");
            if (url == null) {
                url = "jdbc:sqlite:" + dbPath;
            }

            HikariConfig config = new HikariConfig();
            config.setJdbcUrl(url);
            config.setMaximumPoolSize(2);
            config.setConnectionTestQuery("SELECT 1");
            
            // Tối ưu hóa SQLite cho môi trường multi-thread
            config.addDataSourceProperty("journal_mode", "WAL"); // Write-Ahead Logging
            config.addDataSourceProperty("foreign_keys", "ON"); // Bật ràng buộc khóa ngoại
            config.addDataSourceProperty("busy_timeout", "3000"); // Đợi tối đa 3 giây nếu DB bị lock

            dataSource = new HikariDataSource(config);
            log.info("Đã khởi tạo Database Connection Pool tới: {}", url);
        } catch (Exception e) {
            log.error("Lỗi khởi tạo DBConnection", e);
            throw new RuntimeException("Không thể khởi tạo Database Pool", e);
        }
    }

    private DBConnection() {
        // Private constructor
    }

    public static Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    public static void close() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
            log.info("Đã đóng Database Connection Pool.");
        }
    }
}
