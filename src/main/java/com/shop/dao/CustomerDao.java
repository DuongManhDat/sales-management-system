package com.shop.dao;

import com.shop.model.Customer;
import com.shop.model.Gender;
import com.shop.util.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class CustomerDao {
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public void insert(Customer customer) throws SQLException {
        String query = "INSERT INTO customers (code, name, phone, email, date_of_birth, gender, address, note, is_active, created_at) " +
                       "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                int nextId = 1;
                try (PreparedStatement stmt = conn.prepareStatement("SELECT IFNULL(MAX(id), 0) + 1 FROM customers");
                     ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        nextId = rs.getInt(1);
                    }
                }
                
                String code = "KH" + String.format("%05d", nextId);
                customer.setCode(code);
                customer.setCreatedAt(LocalDateTime.now().format(FORMATTER));
                
                try (PreparedStatement pstmt = conn.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) {
                    pstmt.setString(1, customer.getCode());
                    pstmt.setString(2, customer.getName());
                    pstmt.setString(3, customer.getPhone());
                    pstmt.setString(4, customer.getEmail());
                    pstmt.setString(5, customer.getDateOfBirth() != null ? customer.getDateOfBirth().toString() : null);
                    pstmt.setString(6, customer.getGender() != null ? customer.getGender().name() : null);
                    pstmt.setString(7, customer.getAddress());
                    pstmt.setString(8, customer.getNote());
                    pstmt.setInt(9, customer.isActive() ? 1 : 0);
                    pstmt.setString(10, customer.getCreatedAt());
                    
                    pstmt.executeUpdate();
                    
                    try (ResultSet rs = pstmt.getGeneratedKeys()) {
                        if (rs.next()) {
                            int actualId = rs.getInt(1);
                            customer.setId(actualId);
                            // Correct code if it mismatched
                            if (actualId != nextId) {
                                String actualCode = "KH" + String.format("%05d", actualId);
                                try (PreparedStatement updateStmt = conn.prepareStatement("UPDATE customers SET code = ? WHERE id = ?")) {
                                    updateStmt.setString(1, actualCode);
                                    updateStmt.setInt(2, actualId);
                                    updateStmt.executeUpdate();
                                }
                                customer.setCode(actualCode);
                            }
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

    public void update(Customer customer) throws SQLException {
        String query = "UPDATE customers SET name = ?, phone = ?, email = ?, date_of_birth = ?, gender = ?, address = ?, note = ? WHERE id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, customer.getName());
            pstmt.setString(2, customer.getPhone());
            pstmt.setString(3, customer.getEmail());
            pstmt.setString(4, customer.getDateOfBirth() != null ? customer.getDateOfBirth().toString() : null);
            pstmt.setString(5, customer.getGender() != null ? customer.getGender().name() : null);
            pstmt.setString(6, customer.getAddress());
            pstmt.setString(7, customer.getNote());
            pstmt.setInt(8, customer.getId());
            pstmt.executeUpdate();
        }
    }

    public void setActive(int id, boolean isActive) throws SQLException {
        String query = "UPDATE customers SET is_active = ? WHERE id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setInt(1, isActive ? 1 : 0);
            pstmt.setInt(2, id);
            pstmt.executeUpdate();
        }
    }

    public List<Customer> findAll() throws SQLException {
        List<Customer> customers = new ArrayList<>();
        String query = "SELECT * FROM customers ORDER BY created_at DESC";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query);
             ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
                customers.add(mapRow(rs));
            }
        }
        return customers;
    }
    
    public List<Customer> getAllActive() throws SQLException {
        List<Customer> customers = new ArrayList<>();
        String query = "SELECT * FROM customers WHERE is_active = 1 ORDER BY created_at DESC";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query);
             ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
                customers.add(mapRow(rs));
            }
        }
        return customers;
    }

    private Customer mapRow(ResultSet rs) throws SQLException {
        Customer c = new Customer();
        c.setId(rs.getInt("id"));
        c.setCode(rs.getString("code"));
        c.setName(rs.getString("name"));
        c.setPhone(rs.getString("phone"));
        c.setEmail(rs.getString("email"));
        
        String dobStr = rs.getString("date_of_birth");
        if (dobStr != null && !dobStr.isEmpty()) {
            c.setDateOfBirth(LocalDate.parse(dobStr));
        }
        
        String genderStr = rs.getString("gender");
        if (genderStr != null && !genderStr.isEmpty()) {
            c.setGender(Gender.valueOf(genderStr));
        }
        
        c.setAddress(rs.getString("address"));
        c.setNote(rs.getString("note"));
        c.setActive(rs.getInt("is_active") == 1);
        c.setCreatedAt(rs.getString("created_at"));
        
        return c;
    }
}
