package com.shop.dao;

import com.shop.model.ReturnInvoice;
import com.shop.util.DBConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ReturnInvoiceDao {
    public void insert(Connection conn, ReturnInvoice returnInvoice) throws SQLException {
        String query = "INSERT INTO return_invoices (code, invoice_id, customer_id, return_date, total_refund, return_fee, status) " +
                       "VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement pstmt = conn.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setString(1, returnInvoice.getCode());
            pstmt.setInt(2, returnInvoice.getInvoiceId());
            pstmt.setInt(3, returnInvoice.getCustomerId());
            pstmt.setString(4, returnInvoice.getReturnDate());
            pstmt.setLong(5, returnInvoice.getTotalRefund());
            pstmt.setLong(6, returnInvoice.getReturnFee());
            pstmt.setString(7, returnInvoice.getStatus());
            pstmt.executeUpdate();
            
            try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    returnInvoice.setId(generatedKeys.getInt(1));
                }
            }
        }
    }

    public List<ReturnInvoice> findAll() throws SQLException {
        return searchReturnInvoices(null, null, null, null, null);
    }

    public ReturnInvoice findById(int id) throws SQLException {
        String sql = "SELECT ri.*, i.code AS invoice_code, c.name AS customer_name " +
                     "FROM return_invoices ri " +
                     "LEFT JOIN invoices i ON ri.invoice_id = i.id " +
                     "LEFT JOIN customers c ON ri.customer_id = c.id " +
                     "WHERE ri.id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToReturnInvoice(rs);
                }
            }
        }
        return null;
    }

    public List<ReturnInvoice> searchReturnInvoices(String returnCode, String invoiceCode, String customer, String returnDate, String status) throws SQLException {
        List<ReturnInvoice> list = new ArrayList<>();
        StringBuilder sql = new StringBuilder(
            "SELECT ri.*, i.code AS invoice_code, c.name AS customer_name " +
            "FROM return_invoices ri " +
            "LEFT JOIN invoices i ON ri.invoice_id = i.id " +
            "LEFT JOIN customers c ON ri.customer_id = c.id " +
            "WHERE 1=1"
        );
        List<Object> params = new ArrayList<>();

        if (returnCode != null && !returnCode.trim().isEmpty()) {
            sql.append(" AND ri.code LIKE ?");
            params.add("%" + returnCode.trim() + "%");
        }

        if (invoiceCode != null && !invoiceCode.trim().isEmpty()) {
            sql.append(" AND i.code LIKE ?");
            params.add("%" + invoiceCode.trim() + "%");
        }

        if (customer != null && !customer.trim().isEmpty()) {
            sql.append(" AND (c.name LIKE ? OR c.phone LIKE ?)");
            params.add("%" + customer.trim() + "%");
            params.add("%" + customer.trim() + "%");
        }

        if (returnDate != null && !returnDate.trim().isEmpty()) {
            sql.append(" AND date(ri.return_date) = date(?)");
            params.add(returnDate.trim());
        }

        if (status != null && !status.trim().isEmpty() && !"Tất cả".equalsIgnoreCase(status) && !"ALL".equalsIgnoreCase(status)) {
            if ("COMPLETED".equalsIgnoreCase(status) || "Hoàn tất".equalsIgnoreCase(status)) {
                sql.append(" AND (ri.status = 'COMPLETED' OR ri.status = 'Hoàn tất')");
            } else if ("CANCELED".equalsIgnoreCase(status) || "CANCELLED".equalsIgnoreCase(status) || "Đã hủy".equalsIgnoreCase(status)) {
                sql.append(" AND (ri.status = 'CANCELED' OR ri.status = 'CANCELLED' OR ri.status = 'Đã hủy')");
            } else {
                sql.append(" AND ri.status = ?");
                params.add(status);
            }
        }

        sql.append(" ORDER BY ri.id DESC");

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                pstmt.setObject(i + 1, params.get(i));
            }
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSetToReturnInvoice(rs));
                }
            }
        }
        return list;
    }

    public void updateStatus(Connection conn, int returnInvoiceId, String status, String canceledAt) throws SQLException {
        String query = "UPDATE return_invoices SET status = ?, canceled_at = ? WHERE id = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, status);
            pstmt.setString(2, canceledAt);
            pstmt.setInt(3, returnInvoiceId);
            pstmt.executeUpdate();
        }
    }

    private ReturnInvoice mapResultSetToReturnInvoice(ResultSet rs) throws SQLException {
        ReturnInvoice returnInvoice = new ReturnInvoice();
        returnInvoice.setId(rs.getInt("id"));
        returnInvoice.setCode(rs.getString("code"));
        returnInvoice.setInvoiceId(rs.getInt("invoice_id"));
        returnInvoice.setCustomerId(rs.getInt("customer_id"));
        returnInvoice.setReturnDate(rs.getString("return_date"));
        returnInvoice.setTotalRefund(rs.getLong("total_refund"));
        returnInvoice.setReturnFee(rs.getLong("return_fee"));
        returnInvoice.setStatus(rs.getString("status"));
        try {
            returnInvoice.setCanceledAt(rs.getString("canceled_at"));
        } catch (SQLException ignored) {}
        try {
            returnInvoice.setInvoiceCode(rs.getString("invoice_code"));
        } catch (SQLException ignored) {}
        try {
            returnInvoice.setCustomerName(rs.getString("customer_name"));
        } catch (SQLException ignored) {}
        return returnInvoice;
    }
}
