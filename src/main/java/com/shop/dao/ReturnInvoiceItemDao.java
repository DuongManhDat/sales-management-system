package com.shop.dao;

import com.shop.model.ReturnInvoiceItem;
import com.shop.util.DBConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ReturnInvoiceItemDao {
    public void insert(Connection conn, ReturnInvoiceItem item) throws SQLException {
        String query = "INSERT INTO return_invoice_items (return_invoice_id, invoice_item_id, product_id, return_qty, refund_price, amount) " +
                       "VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setInt(1, item.getReturnInvoiceId());
            pstmt.setInt(2, item.getInvoiceItemId());
            pstmt.setInt(3, item.getProductId());
            pstmt.setInt(4, item.getReturnQty());
            pstmt.setLong(5, item.getRefundPrice());
            pstmt.setLong(6, item.getAmount());
            pstmt.executeUpdate();
        }
    }

    public List<ReturnInvoiceItem> findByReturnInvoiceId(int returnInvoiceId) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            return findByReturnInvoiceId(conn, returnInvoiceId);
        }
    }

    public List<ReturnInvoiceItem> findByReturnInvoiceId(Connection conn, int returnInvoiceId) throws SQLException {
        List<ReturnInvoiceItem> list = new ArrayList<>();
        String query = "SELECT rii.*, p.code AS product_code, p.name AS product_name, u.name AS unit_name " +
                       "FROM return_invoice_items rii " +
                       "LEFT JOIN products p ON rii.product_id = p.id " +
                       "LEFT JOIN units u ON p.unit_id = u.id " +
                       "WHERE rii.return_invoice_id = ? " +
                       "ORDER BY rii.id ASC";
        try (PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setInt(1, returnInvoiceId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSetToReturnInvoiceItem(rs));
                }
            }
        }
        return list;
    }

    private ReturnInvoiceItem mapResultSetToReturnInvoiceItem(ResultSet rs) throws SQLException {
        ReturnInvoiceItem item = new ReturnInvoiceItem();
        item.setId(rs.getInt("id"));
        item.setReturnInvoiceId(rs.getInt("return_invoice_id"));
        item.setInvoiceItemId(rs.getInt("invoice_item_id"));
        item.setProductId(rs.getInt("product_id"));
        item.setReturnQty(rs.getInt("return_qty"));
        item.setRefundPrice(rs.getLong("refund_price"));
        item.setAmount(rs.getLong("amount"));
        try {
            item.setProductCode(rs.getString("product_code"));
        } catch (SQLException ignored) {}
        try {
            item.setProductName(rs.getString("product_name"));
        } catch (SQLException ignored) {}
        try {
            item.setUnitName(rs.getString("unit_name"));
        } catch (SQLException ignored) {}
        return item;
    }
}
