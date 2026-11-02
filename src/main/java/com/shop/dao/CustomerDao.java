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
        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                insert(conn, customer);
                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    public void insert(Connection conn, Customer customer) throws SQLException {
        String query = "INSERT INTO customers (code, name, phone, email, date_of_birth, gender, address, note, is_active, created_at) " +
                       "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        int nextId = 1;
        try (PreparedStatement stmt = conn.prepareStatement("SELECT IFNULL(MAX(id), 0) + 1 FROM customers");
             ResultSet rs = stmt.executeQuery()) {
            if (rs.next()) {
                nextId = rs.getInt(1);
            }
        }

        if (customer.getCode() == null || customer.getCode().trim().isEmpty()) {
            String code = "KH" + String.format("%05d", nextId);
            customer.setCode(code);
        }
        if (customer.getCreatedAt() == null || customer.getCreatedAt().trim().isEmpty()) {
            customer.setCreatedAt(LocalDateTime.now().format(FORMATTER));
        }

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
                    // Nếu id sinh ra thực tế khác nextId ban đầu (do xóa bản ghi cũ trước đó) và mã tự sinh KHxxxxx bị lệch
                    if (customer.getCode().startsWith("KH") && customer.getCode().length() == 7) {
                        String actualCode = "KH" + String.format("%05d", actualId);
                        if (!actualCode.equals(customer.getCode())) {
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
        }
    }


    public void update(Customer customer) throws SQLException {
        String query = "UPDATE customers SET name = ?, phone = ?, email = ?, date_of_birth = ?, gender = ?, address = ?, note = ?, is_active = ? WHERE id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, customer.getName());
            pstmt.setString(2, customer.getPhone());
            pstmt.setString(3, customer.getEmail());
            pstmt.setString(4, customer.getDateOfBirth() != null ? customer.getDateOfBirth().toString() : null);
            pstmt.setString(5, customer.getGender() != null ? customer.getGender().name() : null);
            pstmt.setString(6, customer.getAddress());
            pstmt.setString(7, customer.getNote());
            pstmt.setInt(8, customer.isActive() ? 1 : 0);
            pstmt.setInt(9, customer.getId());
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
    
    public Customer findById(int id) throws SQLException {
        String query = "SELECT * FROM customers WHERE id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setInt(1, id);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
            }
        }
        return null;
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


    public com.shop.model.common.PageResult<Customer> findPaged(String keyword, String status, int page, int pageSize) throws SQLException {
        StringBuilder whereClause = new StringBuilder(" WHERE 1=1 ");
        List<Object> params = new ArrayList<>();

        if ("Hoạt động".equals(status)) {
            whereClause.append(" AND is_active = 1 ");
        } else if ("Ngưng hoạt động".equals(status)) {
            whereClause.append(" AND is_active = 0 ");
        }

        if (keyword != null && !keyword.trim().isEmpty()) {
            whereClause.append(" AND (code LIKE ? OR name LIKE ? OR phone LIKE ?) ");
            String kw = "%" + keyword.trim() + "%";
            params.add(kw);
            params.add(kw);
            params.add(kw);
        }

        String countSql = "SELECT COUNT(*) FROM customers " + whereClause;
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
        }

        List<Customer> items = new ArrayList<>();
        int offset = (Math.max(1, page) - 1) * pageSize;
        String dataSql = "SELECT * FROM customers " + whereClause + " ORDER BY created_at DESC LIMIT ? OFFSET ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(dataSql)) {
            int idx = 1;
            for (Object param : params) {
                pstmt.setObject(idx++, param);
            }
            pstmt.setInt(idx++, pageSize);
            pstmt.setInt(idx, offset);

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    items.add(mapRow(rs));
                }
            }
        }

        return new com.shop.model.common.PageResult<>(items, page, pageSize, totalItems);
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
