package com.shop.service;

import com.shop.dao.ProductDao;
import com.shop.dao.StockAdjustmentDao;
import com.shop.dao.StockMovementDao;
import com.shop.model.StockAdjustment;
import com.shop.model.StockAdjustmentItem;
import com.shop.model.StockMovement;
import com.shop.util.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

public class AdjustmentService {
    private final StockAdjustmentDao adjustmentDao = new StockAdjustmentDao();
    private final ProductDao productDao = new ProductDao();
    private final StockMovementDao movementDao = new StockMovementDao();

    public void createAdjustment(StockAdjustment adj, List<StockAdjustmentItem> items) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                // 1. Insert StockAdjustment (header)
                int adjId = adjustmentDao.insertAdjustment(conn, adj);
                
                // 2. Process each item
                for (StockAdjustmentItem item : items) {
                    if (Math.abs(item.getVariance()) < 0.0001) continue; // Skip zero variance
                    
                    item.setAdjustmentId(adjId);
                    int itemId = adjustmentDao.insertItem(conn, item);
                    
                    // Update Product stock_qty
                    productDao.updateStock(conn, item.getProductId(), item.getActualQty());
                    
                    // Insert StockMovement
                    StockMovement movement = new StockMovement();
                    movement.setProductId(item.getProductId());
                    movement.setType("KIEMKHO");
                    movement.setQtyChange(item.getVariance());
                    movement.setStockAfter(item.getActualQty());
                    movement.setRefType("ADJUSTMENT");
                    movement.setRefId(adjId);
                    movement.setNote(item.getReason());
                    movementDao.insert(conn, movement); // Note: created_at handles in DAO (datetime('now'))
                    
                    // Insert InventoryBatch if variance > 0 (Dôi dư)
                    if (item.getVariance() > 0) {
                        String batchSql = "INSERT INTO inventory_batches " +
                                          "(product_id, stock_adjustment_item_id, cost_price, qty_initial, qty_remaining, received_date, source, created_at) " +
                                          "VALUES (?, ?, ?, ?, ?, date('now', 'localtime'), 'ADJUSTMENT', datetime('now', 'localtime'))";
                        try (PreparedStatement batchStmt = conn.prepareStatement(batchSql)) {
                            batchStmt.setInt(1, item.getProductId());
                            batchStmt.setInt(2, itemId);
                            batchStmt.setLong(3, item.getCostPrice() != null ? item.getCostPrice() : 0);
                            batchStmt.setDouble(4, item.getVariance());
                            batchStmt.setDouble(5, item.getVariance());
                            batchStmt.executeUpdate();
                        }
                    }
                }
                
                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        }
    }
    
    public List<StockAdjustment> getAllAdjustments() throws SQLException {
        return adjustmentDao.findAll();
    }
}
