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
    public List<InvoiceItem> findByInvoiceId(Connection conn, int invoiceId) throws SQLException {
        List<InvoiceItem> items = new java.util.ArrayList<>();
        String query = "SELECT ii.*, p.code AS product_code, p.name AS product_name, u.name AS unit_name " +
                       "FROM invoice_items ii " +
                       "LEFT JOIN products p ON ii.product_id = p.id " +
                       "LEFT JOIN units u ON p.unit_id = u.id " +
                       "WHERE ii.invoice_id = ? " +
                       "ORDER BY ii.id ASC";
        try (PreparedStatement pstmt = conn.prepareStatement(query)) {
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
                    try {
                        item.setProductCode(rs.getString("product_code"));
                    } catch (SQLException ignored) {}
                    try {
                        item.setProductName(rs.getString("product_name"));
                    } catch (SQLException ignored) {}
                    try {
                        item.setUnitName(rs.getString("unit_name"));
                    } catch (SQLException ignored) {}
                    items.add(item);
                }
            }
        }
        return items;
    }

    public List<InvoiceItem> findByInvoiceId(int invoiceId) throws SQLException {
        try (Connection conn = com.shop.util.DBConnection.getConnection()) {
            return findByInvoiceId(conn, invoiceId);
        }
    }
}
