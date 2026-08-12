package com.shop.dao;

import com.shop.model.Payment;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class PaymentDao {
    public void insert(Connection conn, Payment payment) throws SQLException {
        String query = "INSERT INTO payments (invoice_id, amount, payment_date, note) VALUES (?, ?, ?, ?)";
        try (PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setInt(1, payment.getInvoiceId());
            pstmt.setLong(2, payment.getAmount());
            pstmt.setString(3, payment.getPaymentDate());
            pstmt.setString(4, payment.getNote());
            pstmt.executeUpdate();
        }
    }
}
