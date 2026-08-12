package com.shop.dao;

import com.shop.model.InvoiceItem;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

public class InvoiceItemDao {
    public void insertAll(Connection conn, List<InvoiceItem> items) throws SQLException {
        String query = "INSERT INTO invoice_items (invoice_id, product_id, qty, sale_price, amount) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement pstmt = conn.prepareStatement(query)) {
            for (InvoiceItem item : items) {
                pstmt.setInt(1, item.getInvoiceId());
                pstmt.setInt(2, item.getProductId());
                pstmt.setInt(3, item.getQty());
                pstmt.setLong(4, item.getSalePrice());
                pstmt.setLong(5, item.getAmount());
                pstmt.addBatch();
            }
            pstmt.executeBatch();
        }
    }
}
