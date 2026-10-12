package com.shop.dao;

import com.shop.model.Payment;
import com.shop.util.DBConnection;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class PaymentDao {
    public void insert(Connection conn, Payment payment) throws SQLException {
        String query = "INSERT INTO payments (invoice_id, amount, payment_date, note, status, cancelled_at, cancel_reason, payment_type) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement pstmt = conn.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setInt(1, payment.getInvoiceId());
            pstmt.setLong(2, payment.getAmount());
            pstmt.setString(3, payment.getPaymentDate());
            pstmt.setString(4, payment.getNote());
            pstmt.setString(5, payment.getStatus() != null ? payment.getStatus() : "ACTIVE");
            pstmt.setString(6, payment.getCancelledAt());
            pstmt.setString(7, payment.getCancelReason());
            pstmt.setString(8, payment.getPaymentType() != null ? payment.getPaymentType() : "PAYMENT");
            pstmt.executeUpdate();
            try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    payment.setId(generatedKeys.getInt(1));
                }
            }
        }
    }

    public List<Payment> findByInvoiceId(int invoiceId) throws SQLException {
        List<Payment> list = new ArrayList<>();
        String query = "SELECT id, invoice_id, amount, payment_date, note, status, cancelled_at, cancel_reason, payment_type FROM payments WHERE invoice_id = ? ORDER BY id ASC";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setInt(1, invoiceId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    Payment p = new Payment();
                    p.setId(rs.getInt("id"));
                    p.setInvoiceId(rs.getInt("invoice_id"));
                    p.setAmount(rs.getLong("amount"));
                    p.setPaymentDate(rs.getString("payment_date"));
                    p.setNote(rs.getString("note"));
                    p.setStatus(rs.getString("status"));
                    p.setCancelledAt(rs.getString("cancelled_at"));
                    p.setCancelReason(rs.getString("cancel_reason"));
                    try {
                        p.setPaymentType(rs.getString("payment_type"));
                    } catch (SQLException ignored) {}
                    list.add(p);
                }
            }
        }
        return list;
    }

    public Payment findById(Connection conn, int id) throws SQLException {
        String query = "SELECT id, invoice_id, amount, payment_date, note, status, cancelled_at, cancel_reason, payment_type FROM payments WHERE id = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setInt(1, id);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    Payment p = new Payment();
                    p.setId(rs.getInt("id"));
                    p.setInvoiceId(rs.getInt("invoice_id"));
                    p.setAmount(rs.getLong("amount"));
                    p.setPaymentDate(rs.getString("payment_date"));
                    p.setNote(rs.getString("note"));
                    p.setStatus(rs.getString("status"));
                    p.setCancelledAt(rs.getString("cancelled_at"));
                    p.setCancelReason(rs.getString("cancel_reason"));
                    try {
                        p.setPaymentType(rs.getString("payment_type"));
                    } catch (SQLException ignored) {}
                    return p;
                }
            }
        }
        return null;
    }

    public void markCancelled(Connection conn, int id, String cancelledAt, String reason) throws SQLException {
        String query = "UPDATE payments SET status = 'CANCELLED', cancelled_at = ?, cancel_reason = ? WHERE id = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, cancelledAt);
            pstmt.setString(2, reason);
            pstmt.setInt(3, id);
            pstmt.executeUpdate();
        }
    }

    public void delete(Connection conn, int id) throws SQLException {
        String query = "DELETE FROM payments WHERE id = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setInt(1, id);
            pstmt.executeUpdate();
        }
    }
}

