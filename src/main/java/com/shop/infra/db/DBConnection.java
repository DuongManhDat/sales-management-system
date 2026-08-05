package com.shop.infra.db;

import com.shop.config.AppPaths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DBConnection {
    private static final Logger logger = LoggerFactory.getLogger(DBConnection.class);
    private static Connection connection = null;

    private DBConnection() { }

    public static synchronized Connection getConnection() throws SQLException {
        if (connection == null || connection.isClosed()) {
            try {
                // Ensure the JDBC driver is loaded
                Class.forName("org.sqlite.JDBC");
                
                String dbPath = AppPaths.getDbFilePath();
                String url = "jdbc:sqlite:" + dbPath;
                connection = DriverManager.getConnection(url);
                logger.info("Connected to database at {}", dbPath);
                
                // Enable foreign keys
                connection.createStatement().execute("PRAGMA foreign_keys = ON;");
            } catch (ClassNotFoundException e) {
                logger.error("SQLite JDBC driver not found", e);
                throw new SQLException("SQLite JDBC driver not found", e);
            }
        }
        return connection;
    }
    
    public static synchronized void closeConnection() {
        if (connection != null) {
            try {
                connection.close();
                logger.info("Database connection closed");
            } catch (SQLException e) {
                logger.error("Failed to close database connection", e);
            }
        }
    }
}
