package com.shop.dao;

import com.shop.model.Unit;
import com.shop.util.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class UnitDao {

    public List<Unit> findAllActive() {
        List<Unit> list = new ArrayList<>();
        String sql = "SELECT id, name, status FROM units WHERE status = 1 ORDER BY name ASC";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
                Unit u = new Unit();
                u.setId(rs.getInt("id"));
                u.setName(rs.getString("name"));
                u.setStatus(rs.getInt("status"));
                list.add(u);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    public void add(String name) throws SQLException {
        String sql = "INSERT INTO units(name, status) VALUES (?, 1)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, name);
            pstmt.executeUpdate();
        }
    }

    public void update(int id, String name) throws SQLException {
        String sql = "UPDATE units SET name = ? WHERE id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, name);
            pstmt.setInt(2, id);
            pstmt.executeUpdate();
        }
    }

    public void softDelete(int id) throws SQLException {
        String sql = "UPDATE units SET status = 0 WHERE id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            pstmt.executeUpdate();
        }
    }

    public int countProductsUsingUnit(int unitId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM products WHERE unit_id = ? AND status = 1";
        int count = 0;
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, unitId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    count = rs.getInt(1);
                }
            }
        }
        return count;
    }
}
