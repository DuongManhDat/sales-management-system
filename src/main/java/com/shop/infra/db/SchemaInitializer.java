package com.shop.infra.db;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.Statement;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SchemaInitializer {
    private static final Logger logger = LoggerFactory.getLogger(SchemaInitializer.class);

    public static void initialize() {
        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement()) {
             
            InputStream is = SchemaInitializer.class.getResourceAsStream("/db/schema.sql");
            if (is == null) {
                logger.error("schema.sql not found in resources/db/");
                return;
            }
            
            String sql = new BufferedReader(new InputStreamReader(is))
                    .lines()
                    .collect(Collectors.joining("\n"));
            
            String[] statements = sql.split(";");
            for (String s : statements) {
                if (!s.trim().isEmpty()) {
                    stmt.execute(s);
                }
            }
            logger.info("Database schema initialized successfully.");
            
        } catch (Exception e) {
            logger.error("Failed to initialize database schema", e);
        }
    }
}
