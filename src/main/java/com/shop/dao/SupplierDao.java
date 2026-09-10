package com.shop.dao;

import com.shop.model.Supplier;
import com.shop.util.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class SupplierDao {
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public void insert(Supplier supplier) throws SQLException {
        String query = "INSERT INTO suppliers (code, name, phone, address, note, is_active, created_at) " +
                       "VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                if (supplier.getCode() == null || supplier.getCode().trim().isEmpty()) {
                    int nextId = 1;
                    try (PreparedStatement stmt = conn.prepareStatement("SELECT IFNULL(MAX(id), 0) + 1 FROM suppliers");
                         ResultSet rs = stmt.executeQuery()) {
                        if (rs.next()) {
                            nextId = rs.getInt(1);
                        }
                    }
                    supplier.setCode("NCC" + String.format("%03d", nextId));
                }
                
                if (supplier.getCreatedAt() == null) {
                    supplier.setCreatedAt(LocalDateTime.now().format(FORMATTER));
                }
                
                try (PreparedStatement pstmt = conn.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) {
                    pstmt.setString(1, supplier.getCode());
                    pstmt.setString(2, supplier.getName());
                    pstmt.setString(3, supplier.getPhone());
                    pstmt.setString(4, supplier.getAddress());
                    pstmt.setString(5, supplier.getNote());
                    pstmt.setInt(6, supplier.isActive() ? 1 : 0);
                    pstmt.setString(7, supplier.getCreatedAt());
                    
                    pstmt.executeUpdate();
                    
                    try (ResultSet rs = pstmt.getGeneratedKeys()) {
                        if (rs.next()) {
                            supplier.setId(rs.getInt(1));
                        }
                    }
                }
                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    public void update(Supplier supplier) throws SQLException {
        String query = "UPDATE suppliers SET name = ?, phone = ?, address = ?, note = ?, is_active = ? WHERE id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, supplier.getName());
            pstmt.setString(2, supplier.getPhone());
            pstmt.setString(3, supplier.getAddress());
            pstmt.setString(4, supplier.getNote());
            pstmt.setInt(5, supplier.isActive() ? 1 : 0);
            pstmt.setInt(6, supplier.getId());
            pstmt.executeUpdate();
        }
    }

    public void setActive(int id, boolean isActive) throws SQLException {
        String query = "UPDATE suppliers SET is_active = ? WHERE id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setInt(1, isActive ? 1 : 0);
            pstmt.setInt(2, id);
            pstmt.executeUpdate();
        }
    }

    public List<Supplier> findAllActive() throws SQLException {
        List<Supplier> suppliers = new ArrayList<>();
        String query = "SELECT * FROM suppliers WHERE is_active = 1 ORDER BY created_at DESC";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query);
             ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
                suppliers.add(mapRow(rs));
            }
        }
        return suppliers;
    }

    public boolean existsByCode(String code, int excludeId) throws SQLException {
        String query = "SELECT COUNT(*) FROM suppliers WHERE code = ? AND id != ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, code);
            pstmt.setInt(2, excludeId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        }
        return false;
    }

    public boolean existsByPhone(String phone, int excludeId) throws SQLException {
        String query = "SELECT COUNT(*) FROM suppliers WHERE phone = ? AND id != ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, phone);
            pstmt.setInt(2, excludeId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        }
        return false;
    }

    public Supplier findByCode(String code) throws SQLException {
        String query = "SELECT * FROM suppliers WHERE code = ? AND is_active = 1";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, code);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
            }
        }
        return null;
    }

    private Supplier mapRow(ResultSet rs) throws SQLException {
        Supplier s = new Supplier();
        s.setId(rs.getInt("id"));
        s.setCode(rs.getString("code"));
        s.setName(rs.getString("name"));
        s.setPhone(rs.getString("phone"));
        s.setAddress(rs.getString("address"));
        s.setNote(rs.getString("note"));
        s.setActive(rs.getInt("is_active") == 1);
        s.setCreatedAt(rs.getString("created_at"));
        return s;
    }
}
