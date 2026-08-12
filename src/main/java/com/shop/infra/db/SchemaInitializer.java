package com.shop.infra.db;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.Statement;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.shop.util.DBConnection;

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
            
            // Execute V2 schema
            InputStream isV2 = SchemaInitializer.class.getResourceAsStream("/db/migration/V2__MuaHang_Schema.sql");
            if (isV2 != null) {
                String sqlV2 = new BufferedReader(new InputStreamReader(isV2))
                        .lines()
                        .collect(Collectors.joining("\n"));
                String[] statementsV2 = sqlV2.split(";");
                for (String s : statementsV2) {
                    if (!s.trim().isEmpty()) {
                        stmt.execute(s);
                    }
                }
                logger.info("V2 Mua Hang Schema initialized successfully.");
            }
            
        } catch (Exception e) {
            logger.error("Failed to initialize database schema", e);
        }
    }
}
