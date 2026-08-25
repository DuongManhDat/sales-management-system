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
            
            // Execute V3 schema
            InputStream isV3 = SchemaInitializer.class.getResourceAsStream("/db/migration/V3__Dashboard_Schema.sql");
            if (isV3 != null) {
                String sqlV3 = new BufferedReader(new InputStreamReader(isV3))
                        .lines()
                        .collect(Collectors.joining("\n"));
                String[] statementsV3 = sqlV3.split(";");
                for (String s : statementsV3) {
                    if (!s.trim().isEmpty()) {
                        try {
                            stmt.execute(s);
                        } catch (Exception ex) {
                            // Bỏ qua lỗi duplicate column name nếu đã chạy rồi
                            logger.warn("Migration V3 warning: " + ex.getMessage());
                        }
                    }
                }
                logger.info("V3 Dashboard Schema initialized successfully.");
            }
            // Execute V4 schema
            InputStream isV4 = SchemaInitializer.class.getResourceAsStream("/db/migration/V4__QuanLyHangHoa_Schema.sql");
            if (isV4 != null) {
                String sqlV4 = new BufferedReader(new InputStreamReader(isV4))
                        .lines()
                        .collect(Collectors.joining("\n"));
                String[] statementsV4 = sqlV4.split(";");
                for (String s : statementsV4) {
                    if (!s.trim().isEmpty()) {
                        try {
                            stmt.execute(s);
                        } catch (Exception ex) {
                            logger.warn("Migration V4 warning: " + ex.getMessage());
                        }
                    }
                }
                logger.info("V4 Quan Ly Hang Hoa Schema initialized successfully.");
            }
            
            // Execute V5 schema
            InputStream isV5 = SchemaInitializer.class.getResourceAsStream("/db/migration/V5__BanHang_Phase2_Schema.sql");
            if (isV5 != null) {
                String sqlV5 = new BufferedReader(new InputStreamReader(isV5))
                        .lines()
                        .collect(Collectors.joining("\n"));
                String[] statementsV5 = sqlV5.split(";");
                for (String s : statementsV5) {
                    if (!s.trim().isEmpty()) {
                        try {
                            stmt.execute(s);
                        } catch (Exception ex) {
                            logger.warn("Migration V5 warning: " + ex.getMessage());
                        }
                    }
                }
                logger.info("V5 Ban Hang Phase 2 Schema initialized successfully.");
            }
            
        } catch (Exception e) {
            logger.error("Failed to initialize database schema", e);
        }
    }
}
