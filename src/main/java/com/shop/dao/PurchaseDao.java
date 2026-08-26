package com.shop.dao;

import com.shop.util.DBConnection;
import com.shop.model.Purchase;
import com.shop.model.PurchaseItem;
import com.shop.model.SupplierPayment;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class PurchaseDao {

    public List<Purchase> getAllPurchases(String search, String status) {
        List<Purchase> list = new ArrayList<>();
        StringBuilder sql = new StringBuilder(
            "SELECT p.*, s.name as supplier_name " +
            "FROM purchases p " +
            "LEFT JOIN suppliers s ON p.supplier_id = s.id " +
            "WHERE 1=1"
        );

        if (search != null && !search.trim().isEmpty()) {
            sql.append(" AND (p.code LIKE ? OR s.name LIKE ?)");
        }
        if (status != null && !status.trim().isEmpty() && !status.equals("Tất cả")) {
            sql.append(" AND p.status = ?");
        }
        sql.append(" ORDER BY p.id DESC");

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql.toString())) {
             
            int paramIndex = 1;
            if (search != null && !search.trim().isEmpty()) {
                String searchPattern = "%" + search.trim() + "%";
                pstmt.setString(paramIndex++, searchPattern);
                pstmt.setString(paramIndex++, searchPattern);
            }
            if (status != null && !status.trim().isEmpty() && !status.equals("Tất cả")) {
                pstmt.setString(paramIndex++, status);
            }

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    Purchase p = new Purchase();
                    p.setId(rs.getInt("id"));
                    p.setCode(rs.getString("code"));
                    p.setSupplierId(rs.getObject("supplier_id") != null ? rs.getInt("supplier_id") : null);
                    p.setPurchaseDate(rs.getString("purchase_date"));
                    p.setTotalCost(rs.getLong("total_cost"));
                    p.setPaid(rs.getLong("paid"));
                    p.setDebt(rs.getLong("debt"));
                    p.setStatus(rs.getString("status"));
                    p.setNote(rs.getString("note"));
                    p.setCreatedAt(rs.getString("created_at"));
                    p.setSupplierName(rs.getString("supplier_name"));
                    list.add(p);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    public void createPurchaseTransaction(Purchase purchase, List<PurchaseItem> items) throws SQLException {
        Connection conn = DBConnection.getConnection();
        try {
            conn.setAutoCommit(false);
            
            // 1. Insert Purchase
            String insertPurchase = "INSERT INTO purchases (code, supplier_id, purchase_date, total_cost, paid, debt, status, note, created_at) " +
                                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
            int purchaseId = -1;
            try (PreparedStatement pstmt = conn.prepareStatement(insertPurchase, Statement.RETURN_GENERATED_KEYS)) {
                pstmt.setString(1, purchase.getCode());
                if (purchase.getSupplierId() != null) {
                    pstmt.setInt(2, purchase.getSupplierId());
                } else {
                    pstmt.setNull(2, java.sql.Types.INTEGER);
                }
                pstmt.setString(3, purchase.getPurchaseDate());
                pstmt.setLong(4, purchase.getTotalCost());
                pstmt.setLong(5, purchase.getPaid());
                pstmt.setLong(6, purchase.getDebt());
                pstmt.setString(7, purchase.getStatus());
                pstmt.setString(8, purchase.getNote());
                pstmt.setString(9, purchase.getCreatedAt());
                
                pstmt.executeUpdate();
                try (ResultSet rs = pstmt.getGeneratedKeys()) {
                    if (rs.next()) {
                        purchaseId = rs.getInt(1);
                        purchase.setId(purchaseId);
                    } else {
                        throw new SQLException("Creating purchase failed, no ID obtained.");
                    }
                }
            }

            // 2. Insert Items & Batches
            String insertBatch = "INSERT INTO inventory_batches (product_id, purchase_item_id, cost_price, qty_initial, qty_remaining, received_date, note, created_at) " +
                                 "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
            String insertItem = "INSERT INTO purchase_items (purchase_id, product_id, batch_id, qty, cost_price, amount) " +
                                "VALUES (?, ?, ?, ?, ?, ?)";
            String updateStock = "UPDATE products SET stock_quantity = stock_quantity + ? WHERE id = ?";
            String updateBatchRef = "UPDATE inventory_batches SET purchase_item_id = ? WHERE id = ?";

            for (PurchaseItem item : items) {
                // We have a circular dependency in DB: inventory_batches.purchase_item_id AND purchase_items.batch_id
                // We must insert batch first without purchase_item_id, then insert purchase_item, then update batch.
                
                int batchId = -1;
                try (PreparedStatement pb = conn.prepareStatement(insertBatch, Statement.RETURN_GENERATED_KEYS)) {
                    pb.setInt(1, item.getProductId());
                    pb.setNull(2, java.sql.Types.INTEGER); // will update later
                    pb.setLong(3, item.getCostPrice());
                    pb.setInt(4, item.getQty());
                    pb.setInt(5, item.getQty());
                    pb.setString(6, purchase.getPurchaseDate());
                    pb.setString(7, "Nhập hàng từ phiếu " + purchase.getCode());
                    pb.setString(8, purchase.getCreatedAt());
                    
                    pb.executeUpdate();
                    try (ResultSet rs = pb.getGeneratedKeys()) {
                        if (rs.next()) {
                            batchId = rs.getInt(1);
                            item.setBatchId(batchId);
                        }
                    }
                }
                
                int itemId = -1;
                try (PreparedStatement pi = conn.prepareStatement(insertItem, Statement.RETURN_GENERATED_KEYS)) {
                    pi.setInt(1, purchaseId);
                    pi.setInt(2, item.getProductId());
                    pi.setInt(3, batchId);
                    pi.setInt(4, item.getQty());
                    pi.setLong(5, item.getCostPrice());
                    pi.setLong(6, item.getAmount());
                    
                    pi.executeUpdate();
                    try (ResultSet rs = pi.getGeneratedKeys()) {
                        if (rs.next()) {
                            itemId = rs.getInt(1);
                            item.setId(itemId);
                        }
                    }
                }
                
                // Update batch with purchase_item_id
                try (PreparedStatement pu = conn.prepareStatement(updateBatchRef)) {
                    pu.setInt(1, itemId);
                    pu.setInt(2, batchId);
                    pu.executeUpdate();
                }
                
                // Update product stock
                try (PreparedStatement ps = conn.prepareStatement(updateStock)) {
                    ps.setInt(1, item.getQty());
                    ps.setInt(2, item.getProductId());
                    ps.executeUpdate();
                }
            }

            // 3. Insert Supplier Payment if paid > 0
            if (purchase.getPaid() > 0) {
                String insertPayment = "INSERT INTO supplier_payments (purchase_id, amount, payment_date, note) VALUES (?, ?, ?, ?)";
                try (PreparedStatement payStmt = conn.prepareStatement(insertPayment)) {
                    payStmt.setInt(1, purchaseId);
                    payStmt.setLong(2, purchase.getPaid());
                    payStmt.setString(3, purchase.getPurchaseDate());
                    payStmt.setString(4, "Thanh toán khi lập phiếu nhập " + purchase.getCode());
                    payStmt.executeUpdate();
                }
            }

            conn.commit();
        } catch (SQLException e) {
            conn.rollback();
            throw e;
        } finally {
            conn.setAutoCommit(true);
            if (conn != null) conn.close();
        }
    }
}
