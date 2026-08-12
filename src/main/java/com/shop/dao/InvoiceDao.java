package com.shop.dao;

import com.shop.model.Invoice;
import com.shop.util.DBConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class InvoiceDao {
    public void insert(Connection conn, Invoice invoice) throws SQLException {
        String query = "INSERT INTO invoices (code, customer_id, invoice_date, subtotal, discount_pct, discount_amt, total, paid, debt, status) " +
                       "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement pstmt = conn.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setString(1, invoice.getCode());
            pstmt.setInt(2, invoice.getCustomerId());
            pstmt.setString(3, invoice.getInvoiceDate());
            pstmt.setLong(4, invoice.getSubtotal());
            pstmt.setDouble(5, invoice.getDiscountPct());
            pstmt.setLong(6, invoice.getDiscountAmt());
            pstmt.setLong(7, invoice.getTotal());
            pstmt.setLong(8, invoice.getPaid());
            pstmt.setLong(9, invoice.getDebt());
            pstmt.setString(10, invoice.getStatus());
            pstmt.executeUpdate();
            
            try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    invoice.setId(generatedKeys.getInt(1));
                }
            }
        }
    }

    public List<Invoice> findByStatus(String status) throws SQLException {
        List<Invoice> invoices = new ArrayList<>();
        String query = "SELECT * FROM invoices WHERE status = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, status);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    invoices.add(mapResultSetToInvoice(rs));
                }
            }
        }
        return invoices;
    }

    public List<Invoice> findAll() throws SQLException {
        List<Invoice> invoices = new ArrayList<>();
        String query = "SELECT * FROM invoices ORDER BY id DESC";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query);
             ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
                invoices.add(mapResultSetToInvoice(rs));
            }
        }
        return invoices;
    }

    public void updatePaymentStatus(Connection conn, int invoiceId, long newPaid, long newDebt, String newStatus) throws SQLException {
        String query = "UPDATE invoices SET paid = ?, debt = ?, status = ? WHERE id = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setLong(1, newPaid);
            pstmt.setLong(2, newDebt);
            pstmt.setString(3, newStatus);
            pstmt.setInt(4, invoiceId);
            pstmt.executeUpdate();
        }
    }

    private Invoice mapResultSetToInvoice(ResultSet rs) throws SQLException {
        Invoice invoice = new Invoice();
        invoice.setId(rs.getInt("id"));
        invoice.setCode(rs.getString("code"));
        invoice.setCustomerId(rs.getInt("customer_id"));
        invoice.setInvoiceDate(rs.getString("invoice_date"));
        invoice.setSubtotal(rs.getLong("subtotal"));
        invoice.setDiscountPct(rs.getDouble("discount_pct"));
        invoice.setDiscountAmt(rs.getLong("discount_amt"));
        invoice.setTotal(rs.getLong("total"));
        invoice.setPaid(rs.getLong("paid"));
        invoice.setDebt(rs.getLong("debt"));
        invoice.setStatus(rs.getString("status"));
        return invoice;
    }
}
