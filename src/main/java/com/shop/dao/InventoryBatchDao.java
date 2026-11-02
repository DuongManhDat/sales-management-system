package com.shop.dao;

import com.shop.model.InventoryBatch;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class InventoryBatchDao {

    public List<InventoryBatch> findAvailableBatchesByProductId(Connection conn, int productId) throws SQLException {
        List<InventoryBatch> batches = new ArrayList<>();
        String sql = "SELECT id, product_id, purchase_item_id, cost_price, qty_initial, qty_remaining, received_date, note, created_at " +
                     "FROM inventory_batches " +
                     "WHERE product_id = ? AND qty_remaining > 0 " +
                     "ORDER BY received_date ASC, id ASC";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, productId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    InventoryBatch batch = new InventoryBatch();
                    batch.setId(rs.getInt("id"));
                    batch.setProductId(rs.getInt("product_id"));
                    int pItemId = rs.getInt("purchase_item_id");
                    batch.setPurchaseItemId(rs.wasNull() ? null : pItemId);
                    batch.setCostPrice(rs.getLong("cost_price"));
                    batch.setQtyInitial(rs.getInt("qty_initial"));
                    batch.setQtyRemaining(rs.getInt("qty_remaining"));
                    batch.setReceivedDate(rs.getString("received_date"));
                    batch.setNote(rs.getString("note"));
                    batch.setCreatedAt(rs.getString("created_at"));
                    batches.add(batch);
                }
            }
        }
        return batches;
    }

    public void updateQtyRemaining(Connection conn, int batchId, int newQtyRemaining) throws SQLException {
        String sql = "UPDATE inventory_batches SET qty_remaining = ? WHERE id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, Math.max(0, newQtyRemaining));
            stmt.setInt(2, batchId);
            stmt.executeUpdate();
        }
    }

    public InventoryBatch findLatestBatchByProductId(Connection conn, int productId) throws SQLException {
        String sql = "SELECT id, product_id, purchase_item_id, cost_price, qty_initial, qty_remaining, received_date, note, created_at " +
                     "FROM inventory_batches " +
                     "WHERE product_id = ? " +
                     "ORDER BY received_date DESC, id DESC LIMIT 1";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, productId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    InventoryBatch batch = new InventoryBatch();
                    batch.setId(rs.getInt("id"));
                    batch.setProductId(rs.getInt("product_id"));
                    int pItemId = rs.getInt("purchase_item_id");
                    batch.setPurchaseItemId(rs.wasNull() ? null : pItemId);
                    batch.setCostPrice(rs.getLong("cost_price"));
                    batch.setQtyInitial(rs.getInt("qty_initial"));
                    batch.setQtyRemaining(rs.getInt("qty_remaining"));
                    batch.setReceivedDate(rs.getString("received_date"));
                    batch.setNote(rs.getString("note"));
                    batch.setCreatedAt(rs.getString("created_at"));
                    return batch;
                }
            }
        }
        return null;
    }
}
