package com.shop.dao;

import com.shop.model.Product;
import com.shop.util.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class ProductDao {
    public List<Product> searchProducts(String keyword) throws SQLException {
        List<Product> products = new ArrayList<>();
        String query = "SELECT * FROM products WHERE (code LIKE ? OR name LIKE ?) AND status = 1";
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
    
    public void decreaseStock(Connection conn, int productId, int qty) throws SQLException {
        String query = "UPDATE products SET stock_qty = stock_qty - ? WHERE id = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setInt(1, qty);
            pstmt.setInt(2, productId);
            pstmt.executeUpdate();
        }
    }

    public Product getById(Connection conn, int productId) throws SQLException {
        String query = "SELECT * FROM products WHERE id = ?";
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

    private Product mapResultSetToProduct(ResultSet rs) throws SQLException {
        Product p = new Product();
        p.setId(rs.getInt("id"));
        p.setCode(rs.getString("code"));
        p.setName(rs.getString("name"));
        p.setUnitId(rs.getInt("unit_id"));
        p.setCategoryId(rs.getInt("category_id"));
        p.setCostPrice(rs.getLong("cost_price"));
        p.setSalePrice(rs.getLong("sale_price"));
        p.setStockQty(rs.getInt("stock_qty"));
        p.setStatus(rs.getInt("status"));
        return p;
    }
}
