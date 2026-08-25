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
        List<ReturnInvoice> list = new ArrayList<>();
        String query = "SELECT * FROM return_invoices ORDER BY id DESC";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query);
             ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
                list.add(mapResultSetToReturnInvoice(rs));
            }
        }
        return list;
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
        return returnInvoice;
    }
}
