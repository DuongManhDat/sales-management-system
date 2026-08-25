package com.shop.dao;

import com.shop.model.PriceHistory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class PriceHistoryDao {

    public void insert(Connection conn, PriceHistory history) throws SQLException {
        String query = "INSERT INTO price_history (product_id, old_price, new_price, changed_at) VALUES (?, ?, ?, datetime('now', 'localtime'))";
        try (PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setInt(1, history.getProductId());
            pstmt.setLong(2, history.getOldPrice());
            pstmt.setLong(3, history.getNewPrice());
            pstmt.executeUpdate();
        }
    }

    public List<PriceHistory> findByProduct(Connection conn, int productId) throws SQLException {
        List<PriceHistory> list = new ArrayList<>();
        String query = "SELECT * FROM price_history WHERE product_id = ? ORDER BY changed_at DESC";
        try (PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setInt(1, productId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    PriceHistory h = new PriceHistory();
                    h.setId(rs.getInt("id"));
                    h.setProductId(rs.getInt("product_id"));
                    h.setOldPrice(rs.getLong("old_price"));
                    h.setNewPrice(rs.getLong("new_price"));
                    h.setChangedAt(rs.getString("changed_at"));
                    list.add(h);
                }
            }
        }
        return list;
    }
}
