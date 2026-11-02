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


    public com.shop.model.common.PageResult<Unit> findPaged(String keyword, int page, int pageSize) {
        StringBuilder sqlWhere = new StringBuilder(" WHERE status = 1");
        List<Object> params = new ArrayList<>();
        if (keyword != null && !keyword.trim().isEmpty()) {
            sqlWhere.append(" AND name LIKE ?");
            params.add("%" + keyword.trim() + "%");
        }

        String countSql = "SELECT COUNT(*) FROM units" + sqlWhere;
        int totalItems = 0;
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(countSql)) {
            for (int i = 0; i < params.size(); i++) {
                pstmt.setObject(i + 1, params.get(i));
            }
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    totalItems = rs.getInt(1);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        List<Unit> list = new ArrayList<>();
        int offset = (Math.max(1, page) - 1) * pageSize;
        String dataSql = "SELECT id, name, status FROM units" + sqlWhere + " ORDER BY name ASC LIMIT ? OFFSET ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(dataSql)) {
            int idx = 1;
            for (Object p : params) {
                pstmt.setObject(idx++, p);
            }
            pstmt.setInt(idx++, pageSize);
            pstmt.setInt(idx, offset);

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    Unit u = new Unit();
                    u.setId(rs.getInt("id"));
                    u.setName(rs.getString("name"));
                    u.setStatus(rs.getInt("status"));
                    list.add(u);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return new com.shop.model.common.PageResult<>(list, page, pageSize, totalItems);
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

    public Unit findByName(String name) {
        if (name == null || name.trim().isEmpty()) {
            return null;
        }
        String sql = "SELECT id, name, status FROM units WHERE LOWER(TRIM(name)) = LOWER(TRIM(?)) AND status = 1 LIMIT 1";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, name.trim());
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    Unit u = new Unit();
                    u.setId(rs.getInt("id"));
                    u.setName(rs.getString("name"));
                    u.setStatus(rs.getInt("status"));
                    return u;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }
}
