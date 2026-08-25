package com.shop.dao;

import com.shop.model.InvoiceItem;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

public class InvoiceItemDao {
    public void insertAll(Connection conn, List<InvoiceItem> items) throws SQLException {
        String query = "INSERT INTO invoice_items (invoice_id, product_id, qty, sale_price, amount, cost_price) VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement pstmt = conn.prepareStatement(query)) {
            for (InvoiceItem item : items) {
                pstmt.setInt(1, item.getInvoiceId());
                pstmt.setInt(2, item.getProductId());
                pstmt.setInt(3, item.getQty());
                pstmt.setLong(4, item.getSalePrice());
                pstmt.setLong(5, item.getAmount());
                pstmt.setLong(6, item.getCostPrice());
                pstmt.addBatch();
            }
            pstmt.executeBatch();
        }
    }
    public List<InvoiceItem> findByInvoiceId(int invoiceId) throws SQLException {
        List<InvoiceItem> items = new java.util.ArrayList<>();
        String query = "SELECT * FROM invoice_items WHERE invoice_id = ?";
        try (Connection conn = com.shop.util.DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setInt(1, invoiceId);
            try (java.sql.ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    InvoiceItem item = new InvoiceItem();
                    item.setId(rs.getInt("id"));
                    item.setInvoiceId(rs.getInt("invoice_id"));
                    item.setProductId(rs.getInt("product_id"));
                    item.setQty(rs.getInt("qty"));
                    item.setSalePrice(rs.getLong("sale_price"));
                    item.setAmount(rs.getLong("amount"));
                    item.setCostPrice(rs.getLong("cost_price"));
                    items.add(item);
                }
            }
        }
        return items;
    }
}
