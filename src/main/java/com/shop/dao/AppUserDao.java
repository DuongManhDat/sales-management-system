package com.shop.dao;

import com.shop.model.AppUser;
import com.shop.util.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

public class AppUserDao {
    public Optional<AppUser> findFirst() {
        String sql = "SELECT * FROM app_user LIMIT 1";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            if (rs.next()) {
                AppUser user = new AppUser();
                user.setId(rs.getInt("id"));
                user.setUsername(rs.getString("username"));
                user.setPasswordHash(rs.getString("password_hash"));
                user.setCreatedAt(rs.getString("created_at"));
                return Optional.of(user);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi truy vấn app_user", e);
        }
        return Optional.empty();
    }

    public void insert(AppUser user) {
        String sql = "INSERT INTO app_user (username, password_hash, created_at) VALUES (?, ?, ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, user.getUsername());
            stmt.setString(2, user.getPasswordHash());
            stmt.setString(3, user.getCreatedAt());
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi tạo người dùng", e);
        }
    }
}
