package com.shop.dao;

import com.shop.model.StockAdjustment;
import com.shop.model.StockAdjustmentItem;
import com.shop.util.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class StockAdjustmentDao {

    public int insertAdjustment(Connection conn, StockAdjustment adj) throws SQLException {
        String query = "INSERT INTO stock_adjustments (code, adjustment_date, note, created_at) " +
                       "VALUES (?, date('now', 'localtime'), ?, datetime('now', 'localtime'))";
        
        try (PreparedStatement pstmt = conn.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setString(1, "TEMP"); 
            pstmt.setString(2, adj.getNote());
            pstmt.executeUpdate();
            
            try (ResultSet rs = pstmt.getGeneratedKeys()) {
                if (rs.next()) {
                    int id = rs.getInt(1);
                    String code = String.format("DC%06d", id);
                    if (id > 999999) code = "DC" + id;
                    
                    updateCode(conn, id, code);
                    return id;
                }
            }
        }
        throw new SQLException("Insert failed, no ID obtained.");
    }
    
    private void updateCode(Connection conn, int id, String code) throws SQLException {
        String query = "UPDATE stock_adjustments SET code = ? WHERE id = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, code);
            pstmt.setInt(2, id);
            pstmt.executeUpdate();
        }
    }

    public int insertItem(Connection conn, StockAdjustmentItem item) throws SQLException {
        String query = "INSERT INTO stock_adjustment_items (adjustment_id, product_id, current_qty, actual_qty, variance, cost_price, reason) " +
                       "VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement pstmt = conn.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setInt(1, item.getAdjustmentId());
            pstmt.setInt(2, item.getProductId());
            pstmt.setDouble(3, item.getCurrentQty());
            pstmt.setDouble(4, item.getActualQty());
            pstmt.setDouble(5, item.getVariance());
            if (item.getCostPrice() != null) {
                pstmt.setLong(6, item.getCostPrice());
            } else {
                pstmt.setNull(6, java.sql.Types.INTEGER);
            }
            pstmt.setString(7, item.getReason());
            pstmt.executeUpdate();
            
            try (ResultSet rs = pstmt.getGeneratedKeys()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }
        throw new SQLException("Insert item failed, no ID obtained.");
    }
    
    public List<StockAdjustment> findAll() throws SQLException {
        List<StockAdjustment> list = new ArrayList<>();
        String query = "SELECT * FROM stock_adjustments ORDER BY created_at DESC";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query);
             ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
                StockAdjustment adj = new StockAdjustment();
                adj.setId(rs.getInt("id"));
                adj.setCode(rs.getString("code"));
                adj.setAdjustmentDate(rs.getString("adjustment_date"));
                adj.setNote(rs.getString("note"));
                adj.setCreatedAt(rs.getString("created_at"));
                list.add(adj);
            }
        }
        return list;
    }
    
    // To count modified items for a history view if needed
    public int countItems(Connection conn, int adjustmentId) throws SQLException {
        String query = "SELECT COUNT(*) FROM stock_adjustment_items WHERE adjustment_id = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setInt(1, adjustmentId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        }
        return 0;
    }
}
