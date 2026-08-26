package com.shop.dao;

import com.shop.model.ActivityLog;
import com.shop.util.DBConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class ActivityLogDao {
    private static final Logger log = LoggerFactory.getLogger(ActivityLogDao.class);
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public void insert(Connection conn, ActivityLog logEntry) throws SQLException {
        String sql = "INSERT INTO activity_log (action, entity, entity_id, detail, created_at) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, logEntry.getAction());
            stmt.setString(2, logEntry.getEntity());
            if (logEntry.getEntityId() != null) {
                stmt.setInt(3, logEntry.getEntityId());
            } else {
                stmt.setNull(3, java.sql.Types.INTEGER);
            }
            stmt.setString(4, logEntry.getDetail());
            stmt.setString(5, logEntry.getCreatedAt() != null ? logEntry.getCreatedAt().format(FORMATTER) : LocalDateTime.now().format(FORMATTER));
            stmt.executeUpdate();
        }
    }

    public void insertLog(ActivityLog logEntry) {
        try (Connection conn = DBConnection.getConnection()) {
            insert(conn, logEntry);
        } catch (SQLException e) {
            log.error("Lỗi khi ghi nhật ký hoạt động", e);
        }
    }

    public List<ActivityLog> getAllLogs() {
        List<ActivityLog> logs = new ArrayList<>();
        String sql = "SELECT * FROM activity_log ORDER BY created_at DESC";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
             
            while (rs.next()) {
                ActivityLog activityLog = new ActivityLog();
                activityLog.setId(rs.getInt("id"));
                activityLog.setAction(rs.getString("action"));
                activityLog.setEntity(rs.getString("entity"));
                activityLog.setEntityId(rs.getInt("entity_id"));
                if (rs.wasNull()) {
                    activityLog.setEntityId(null);
                }
                activityLog.setDetail(rs.getString("detail"));
                activityLog.setCreatedAt(LocalDateTime.parse(rs.getString("created_at"), FORMATTER));
                logs.add(activityLog);
            }
        } catch (SQLException e) {
            log.error("Lỗi khi lấy danh sách nhật ký", e);
        }
        return logs;
    }
}
