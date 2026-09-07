package com.shop.dao;

import com.shop.model.Product;
import com.shop.util.DBConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class ProductDao {
    private static final Logger log = LoggerFactory.getLogger(ProductDao.class);
    
    // Create new Product and return the generated ID
    // Note: the code "HH..." is generated after insertion
    public int insert(Connection conn, Product product) throws SQLException {
        String query = "INSERT INTO products (code, name, unit_id, category_id, sale_price, stock_qty, note, created_at, updated_at) " +
                       "VALUES (?, ?, ?, ?, ?, ?, ?, datetime('now', 'localtime'), datetime('now', 'localtime'))";
        
        try (PreparedStatement pstmt = conn.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setString(1, "TEMP"); // Temporary code, will be updated immediately
            pstmt.setString(2, product.getName());
            pstmt.setObject(3, product.getUnitId());
            pstmt.setObject(4, product.getCategoryId());
            pstmt.setLong(5, product.getSalePrice());
            pstmt.setDouble(6, product.getStockQty());
            pstmt.setString(7, product.getNote());
            
            pstmt.executeUpdate();
            
            try (ResultSet rs = pstmt.getGeneratedKeys()) {
                if (rs.next()) {
                    int id = rs.getInt(1);
                    // Generate code
                    String code = String.format("HH%06d", id);
                    if (id > 999999) code = "HH" + id;
                    
                    // Update code
                    updateCode(conn, id, code);
                    return id;
                }
            }
        } catch (SQLException e) {
            log.error("Lỗi khi thực thi câu lệnh SQL insert vào bảng products (name='{}', unitId={}): {}",
                    product.getName(), product.getUnitId(), e.getMessage(), e);
            throw e;
        }
        throw new SQLException("Insert failed, no ID obtained.");
    }
    
    public void updateCode(Connection conn, int id, String code) throws SQLException {
        String query = "UPDATE products SET code = ? WHERE id = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, code);
            pstmt.setInt(2, id);
            pstmt.executeUpdate();
        }
    }

    public void update(Connection conn, Product product) throws SQLException {
        String query = "UPDATE products SET name = ?, unit_id = ?, category_id = ?, sale_price = ?, note = ?, updated_at = datetime('now', 'localtime') WHERE id = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, product.getName());
            pstmt.setObject(2, product.getUnitId());
            pstmt.setObject(3, product.getCategoryId());
            pstmt.setLong(4, product.getSalePrice());
            pstmt.setString(5, product.getNote());
            pstmt.setInt(6, product.getId());
            pstmt.executeUpdate();
        }
    }
    
    public void setDeleted(Connection conn, int productId, boolean isDeleted) throws SQLException {
        String query = "UPDATE products SET deleted_at = " + (isDeleted ? "datetime('now', 'localtime')" : "NULL") + " WHERE id = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setInt(1, productId);
            pstmt.executeUpdate();
        }
    }
    
    public void updateStock(Connection conn, int productId, double actualQty) throws SQLException {
        String query = "UPDATE products SET stock_qty = ?, updated_at = datetime('now', 'localtime') WHERE id = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setDouble(1, actualQty);
            pstmt.setInt(2, productId);
            pstmt.executeUpdate();
        }
    }

    public void decreaseStock(Connection conn, int productId, double qty) throws SQLException {
        String query = "UPDATE products SET stock_qty = stock_qty - ? WHERE id = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setDouble(1, qty);
            pstmt.setInt(2, productId);
            pstmt.executeUpdate();
        }
    }

    public void increaseStock(Connection conn, int productId, double qty) throws SQLException {
        String query = "UPDATE products SET stock_qty = stock_qty + ? WHERE id = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setDouble(1, qty);
            pstmt.setInt(2, productId);
            pstmt.executeUpdate();
        }
    }

    public Product getById(Connection conn, int productId) throws SQLException {
        String query = "SELECT p.*, u.name as unit_name, c.name as category_name " +
                       "FROM products p " +
                       "LEFT JOIN units u ON p.unit_id = u.id " +
                       "LEFT JOIN categories c ON p.category_id = c.id " +
                       "WHERE p.id = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setInt(1, productId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToProduct(rs);
                }
            }
        }
        return null;
    }
    
    public List<Product> findAll() throws SQLException {
        List<Product> products = new ArrayList<>();
        String query = "SELECT p.*, u.name as unit_name, c.name as category_name " +
                       "FROM products p " +
                       "LEFT JOIN units u ON p.unit_id = u.id " +
                       "LEFT JOIN categories c ON p.category_id = c.id " +
                       "ORDER BY p.code ASC";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query);
             ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
                products.add(mapResultSetToProduct(rs));
            }
        }
        return products;
    }

    public List<Product> searchProducts(String keyword) throws SQLException {
        List<Product> products = new ArrayList<>();
        String query = "SELECT p.*, u.name as unit_name, c.name as category_name " +
                       "FROM products p " +
                       "LEFT JOIN units u ON p.unit_id = u.id " +
                       "LEFT JOIN categories c ON p.category_id = c.id " +
                       "WHERE (p.code LIKE ? OR p.name LIKE ?) AND p.deleted_at IS NULL " +
                       "ORDER BY p.code ASC";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            String searchPattern = "%" + keyword + "%";
            pstmt.setString(1, searchPattern);
            pstmt.setString(2, searchPattern);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    products.add(mapResultSetToProduct(rs));
                }
            }
        }
        return products;
    }

    private Product mapResultSetToProduct(ResultSet rs) throws SQLException {
        Product p = new Product();
        p.setId(rs.getInt("id"));
        p.setCode(rs.getString("code"));
        p.setName(rs.getString("name"));
        
        p.setUnitId(rs.getObject("unit_id") != null ? rs.getInt("unit_id") : null);
        p.setUnitName(rs.getString("unit_name")); // from JOIN
        
        p.setCategoryId(rs.getObject("category_id") != null ? rs.getInt("category_id") : null);
        p.setCategoryName(rs.getString("category_name")); // from JOIN

        p.setSalePrice(rs.getLong("sale_price"));
        p.setStockQty(rs.getDouble("stock_qty"));
        p.setNote(rs.getString("note"));
        p.setDeletedAt(rs.getString("deleted_at"));
        p.setCreatedAt(rs.getString("created_at"));
        p.setUpdatedAt(rs.getString("updated_at"));
        return p;
    }
}
