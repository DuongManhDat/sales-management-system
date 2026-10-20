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
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
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
                
                // 2. Process each item (Lưu tất cả sản phẩm kiểm kê vào biên bản kiểm kho)
                for (StockAdjustmentItem item : items) {
                    item.setAdjustmentId(adjId);
                    int itemId = adjustmentDao.insertItem(conn, item);

                    // Nếu không có chênh lệch tồn kho thì không cần điều chỉnh kho/tạo lô
                    if (Math.abs(item.getVariance()) < 0.0001) {
                        continue;
                    }
                    
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

    public void cancelAdjustment(int adjustmentId) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                StockAdjustment adj = adjustmentDao.findById(conn, adjustmentId);
                if (adj == null) {
                    throw new IllegalArgumentException("Không tìm thấy phiếu kiểm kho ID = " + adjustmentId);
                }
                if ("CANCELED".equalsIgnoreCase(adj.getStatus())) {
                    throw new IllegalStateException("Phiếu kiểm kho này đã bị hủy trước đó.");
                }

                String currentTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

                // 1. Đánh dấu trạng thái phiếu thành CANCELED
                adjustmentDao.updateStatus(conn, adjustmentId, "CANCELED", currentTime);

                // 2. Thu hồi tồn kho và xóa lô tồn kho phát sinh
                List<StockAdjustmentItem> items = adjustmentDao.findItemsByAdjustmentId(conn, adjustmentId);
                for (StockAdjustmentItem item : items) {
                    if (Math.abs(item.getVariance()) < 0.0001) continue;

                    // Hoàn nguyên tồn kho: variance > 0 (đã tăng) -> decrease; variance < 0 (đã giảm) -> bù lại
                    productDao.decreaseStock(conn, item.getProductId(), item.getVariance());

                    double stockAfter = productDao.getById(conn, item.getProductId()).getStockQty();

                    // Ghi nhận biến động kho
                    StockMovement movement = new StockMovement();
                    movement.setProductId(item.getProductId());
                    movement.setType("ADJUSTMENT_CANCEL");
                    movement.setQtyChange(-item.getVariance());
                    movement.setStockAfter(stockAfter);
                    movement.setRefType("ADJUSTMENT_CANCEL");
                    movement.setRefId(adjustmentId);
                    movement.setCreatedAt(currentTime);
                    movement.setNote("Hủy phiếu kiểm kho " + adj.getCode());
                    movementDao.insert(conn, movement);

                    // Xóa các lô tồn kho phát sinh từ phiếu dôi dư này
                    String delBatchSql = "DELETE FROM inventory_batches WHERE stock_adjustment_item_id = ?";
                    try (PreparedStatement pstmt = conn.prepareStatement(delBatchSql)) {
                        pstmt.setInt(1, item.getId());
                        pstmt.executeUpdate();
                    }
                }

                conn.commit();
            } catch (SQLException | RuntimeException e) {
                conn.rollback();
                throw e;
            }
        }
    }
    
    public List<StockAdjustment> getAllAdjustments() throws SQLException {
        return adjustmentDao.findAll();
    }

    public StockAdjustment getAdjustmentById(int id) throws SQLException {
        return adjustmentDao.findById(id);
    }

    public List<StockAdjustment> filterAdjustments(String code, String date) throws SQLException {
        return adjustmentDao.filterAdjustments(code, date);
    }
    
    public List<StockAdjustmentItem> getAdjustmentItems(int adjustmentId) throws SQLException {
        return adjustmentDao.findItemsByAdjustmentId(adjustmentId);
    }
}
