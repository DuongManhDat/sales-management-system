package com.shop.infra.db;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.shop.util.DBConnection;

public class SchemaInitializer {
    private static final Logger logger = LoggerFactory.getLogger(SchemaInitializer.class);

    public static void initialize() {
        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement()) {

            // 1. Tạo bảng quản lý lịch sử migration nếu chưa có
            stmt.execute("CREATE TABLE IF NOT EXISTS schema_migrations (" +
                    "version TEXT PRIMARY KEY, " +
                    "applied_at TEXT NOT NULL" +
                    ")");

            // 2. Lấy danh sách các migration đã áp dụng
            Set<String> appliedVersions = new HashSet<>();
            try (ResultSet rs = stmt.executeQuery("SELECT version FROM schema_migrations")) {
                while (rs.next()) {
                    appliedVersions.add(rs.getString("version"));
                }
            }

            // 3. Tương thích ngược: Nếu database đã có sẵn dữ liệu trước khi bổ sung bảng schema_migrations
            bootstrapExistingDatabase(conn, appliedVersions);

            // 4. Thực thi từng migration theo thứ tự, chỉ chạy nếu chưa được ghi nhận
            executeMigration(conn, stmt, appliedVersions, "V1", "/db/schema.sql");
            executeMigration(conn, stmt, appliedVersions, "V2", "/db/migration/V2__MuaHang_Schema.sql");
            executeMigration(conn, stmt, appliedVersions, "V3", "/db/migration/V3__Dashboard_Schema.sql");
            executeMigration(conn, stmt, appliedVersions, "V4", "/db/migration/V4__QuanLyHangHoa_Schema.sql");
            executeMigration(conn, stmt, appliedVersions, "V5", "/db/migration/V5__BanHang_Phase2_Schema.sql");

        } catch (Exception e) {
            logger.error("Failed to initialize database schema", e);
        }
    }

    private static void bootstrapExistingDatabase(Connection conn, Set<String> appliedVersions) {
        try (Statement stmt = conn.createStatement()) {
            boolean hasProducts = false;
            try (ResultSet rs = stmt.executeQuery("SELECT name FROM sqlite_master WHERE type='table' AND name='products'")) {
                if (rs.next()) {
                    hasProducts = true;
                }
            }

            if (hasProducts) {
                // Kiểm tra xem products đã được migrate sang cấu trúc V4 chưa (có cột deleted_at)
                boolean hasDeletedAt = false;
                try (ResultSet rs = stmt.executeQuery("PRAGMA table_info(products)")) {
                    while (rs.next()) {
                        if ("deleted_at".equalsIgnoreCase(rs.getString("name"))) {
                            hasDeletedAt = true;
                            break;
                        }
                    }
                }

                if (hasDeletedAt) {
                    // Cơ sở dữ liệu đã qua V1, V2, V3, V4 -> Đánh dấu đã áp dụng để không bao giờ chạy lại
                    markVersionApplied(conn, appliedVersions, "V1");
                    markVersionApplied(conn, appliedVersions, "V2");
                    markVersionApplied(conn, appliedVersions, "V3");
                    markVersionApplied(conn, appliedVersions, "V4");
                }

                // Kiểm tra bảng return_invoices của V5
                try (ResultSet rs = stmt.executeQuery("SELECT name FROM sqlite_master WHERE type='table' AND name='return_invoices'")) {
                    if (rs.next()) {
                        markVersionApplied(conn, appliedVersions, "V5");
                    }
                }
            }
        } catch (Exception e) {
            logger.warn("Lỗi khi kiểm tra dữ liệu hiện có trong bootstrap: {}", e.getMessage());
        }
    }

    private static void markVersionApplied(Connection conn, Set<String> appliedVersions, String version) {
        if (!appliedVersions.contains(version)) {
            try (PreparedStatement pstmt = conn.prepareStatement(
                    "INSERT OR IGNORE INTO schema_migrations (version, applied_at) VALUES (?, datetime('now', 'localtime'))")) {
                pstmt.setString(1, version);
                pstmt.executeUpdate();
                appliedVersions.add(version);
                logger.info("Đã ghi nhận migration: {}", version);
            } catch (Exception e) {
                logger.warn("Không thể ghi nhận version {}: {}", version, e.getMessage());
            }
        }
    }

    private static void executeMigration(Connection conn, Statement stmt, Set<String> appliedVersions, String version, String resourcePath) {
        if (appliedVersions.contains(version)) {
            logger.debug("Migration {} đã được áp dụng trước đó, bỏ qua.", version);
            return;
        }

        // Kiểm tra an toàn bổ sung cho V4: Nếu products đã có deleted_at thì tuyệt đối không chạy lại bước drop/rename
        if ("V4".equals(version)) {
            try (ResultSet rs = stmt.executeQuery("PRAGMA table_info(products)")) {
                while (rs.next()) {
                    if ("deleted_at".equalsIgnoreCase(rs.getString("name"))) {
                        logger.info("Bảng products đã có cấu trúc hiện đại (deleted_at), bỏ qua chạy lại V4.");
                        markVersionApplied(conn, appliedVersions, version);
                        return;
                    }
                }
            } catch (Exception e) {
                logger.warn("Lỗi kiểm tra cấu trúc bảng products trước khi chạy V4: {}", e.getMessage());
            }
        }

        InputStream is = SchemaInitializer.class.getResourceAsStream(resourcePath);
        if (is == null) {
            logger.warn("Không tìm thấy file migration: {}", resourcePath);
            return;
        }

        logger.info("Đang thực thi migration {}: {} ...", version, resourcePath);
        try {
            String sql = new BufferedReader(new InputStreamReader(is))
                    .lines()
                    .collect(Collectors.joining("\n"));

            String[] statements = sql.split(";");
            for (String s : statements) {
                String trimmed = s.trim();
                if (!trimmed.isEmpty()) {
                    try {
                        stmt.execute(trimmed);
                    } catch (Exception ex) {
                        logger.warn("Cảnh báo khi chạy lệnh trong migration {}: {}", version, ex.getMessage());
                    }
                }
            }

            markVersionApplied(conn, appliedVersions, version);
            logger.info("Hoàn tất migration {}.", version);
        } catch (Exception e) {
            logger.error("Lỗi khi thực thi migration {}: {}", version, e.getMessage(), e);
        }
    }
}
