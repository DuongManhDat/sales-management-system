package com.shop.dao;

import com.shop.model.StockMovement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class StockMovementDao {
    public void insert(Connection conn, StockMovement movement) throws SQLException {
        String query = "INSERT INTO stock_movements (product_id, type, qty_change, stock_after, ref_type, ref_id, created_at, note) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setInt(1, movement.getProductId());
            pstmt.setString(2, movement.getType());
            pstmt.setInt(3, movement.getQtyChange());
            pstmt.setInt(4, movement.getStockAfter());
            pstmt.setString(5, movement.getRefType());
            pstmt.setInt(6, movement.getRefId());
            pstmt.setString(7, movement.getCreatedAt());
            pstmt.setString(8, movement.getNote());
            pstmt.executeUpdate();
        }
    }
}
