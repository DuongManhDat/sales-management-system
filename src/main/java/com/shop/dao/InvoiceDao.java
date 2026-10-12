package com.shop.dao;

import com.shop.model.Invoice;
import com.shop.util.DBConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class InvoiceDao {
    public void insert(Connection conn, Invoice invoice) throws SQLException {
        String query = "INSERT INTO invoices (code, customer_id, invoice_date, subtotal, discount_pct, discount_amt, total, paid, debt, status, delivery_date) " +
                       "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
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
            pstmt.setString(11, invoice.getDeliveryDate());
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
        String query = "SELECT i.*, c.name AS customer_name FROM invoices i " +
                       "LEFT JOIN customers c ON i.customer_id = c.id " +
                       "WHERE i.status = ? ORDER BY i.id DESC";
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

    public List<Invoice> searchInvoices(String keyword, String status, String dateFilter) throws SQLException {
        List<Invoice> invoices = new ArrayList<>();
        StringBuilder sql = new StringBuilder(
            "SELECT i.*, c.name AS customer_name FROM invoices i " +
            "LEFT JOIN customers c ON i.customer_id = c.id " +
            "WHERE 1=1"
        );
        List<Object> params = new ArrayList<>();

        if (keyword != null && !keyword.trim().isEmpty()) {
            String pattern = "%" + keyword.trim() + "%";
            sql.append(" AND (i.code LIKE ? OR c.name LIKE ?)");
            params.add(pattern);
            params.add(pattern);
        }

        if (status != null && !status.trim().isEmpty() && !"ALL".equalsIgnoreCase(status) && !"Tất cả".equalsIgnoreCase(status)) {
            if ("PAID".equalsIgnoreCase(status) || "Đã thanh toán".equalsIgnoreCase(status)) {
                sql.append(" AND (i.status = 'PAID' OR i.status = 'Đã thanh toán' OR (i.debt = 0 AND i.status != 'CANCELLED'))");
            } else if ("DEBT".equalsIgnoreCase(status) || "Còn nợ".equalsIgnoreCase(status)) {
                sql.append(" AND (i.status = 'PARTIAL' OR i.status = 'Còn nợ' OR (i.debt > 0 AND i.status != 'CANCELLED'))");
            } else {
                sql.append(" AND i.status = ?");
                params.add(status);
            }
        }

        if (dateFilter != null && !dateFilter.trim().isEmpty()) {
            sql.append(" AND date(i.invoice_date) = date(?)");
            params.add(dateFilter.trim());
        }

        sql.append(" ORDER BY i.id DESC");

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql.toString())) {
            for (int idx = 0; idx < params.size(); idx++) {
                pstmt.setObject(idx + 1, params.get(idx));
            }
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    invoices.add(mapResultSetToInvoice(rs));
                }
            }
        }
        return invoices;
    }

    public List<Invoice> searchOrders(String keyword, String status, String dateFilter) throws SQLException {
        List<Invoice> orders = new ArrayList<>();
        StringBuilder sql = new StringBuilder(
            "SELECT i.*, c.name AS customer_name FROM invoices i " +
            "LEFT JOIN customers c ON i.customer_id = c.id " +
            "WHERE 1=1"
        );
        List<Object> params = new ArrayList<>();

        if (keyword != null && !keyword.trim().isEmpty()) {
            String pattern = "%" + keyword.trim() + "%";
            sql.append(" AND (i.code LIKE ? OR c.name LIKE ?)");
            params.add(pattern);
            params.add(pattern);
        }

        if (status != null && !status.trim().isEmpty()) {
            sql.append(" AND i.status = ?");
            params.add(status);
        }

        if (dateFilter != null && !dateFilter.trim().isEmpty()) {
            sql.append(" AND date(i.invoice_date) = date(?)");
            params.add(dateFilter.trim());
        }

        sql.append(" ORDER BY i.id DESC");

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql.toString())) {
            for (int idx = 0; idx < params.size(); idx++) {
                pstmt.setObject(idx + 1, params.get(idx));
            }
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    orders.add(mapResultSetToInvoice(rs));
                }
            }
        }
        return orders;
    }

    public List<Invoice> findAll() throws SQLException {
        List<Invoice> invoices = new ArrayList<>();
        String query = "SELECT i.*, c.name AS customer_name FROM invoices i " +
                       "LEFT JOIN customers c ON i.customer_id = c.id " +
                       "ORDER BY i.id DESC";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query);
             ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
                invoices.add(mapResultSetToInvoice(rs));
            }
        }
        return invoices;
    }

    public List<Invoice> findByCustomerId(int customerId) throws SQLException {
        List<Invoice> invoices = new ArrayList<>();
        String query = "SELECT i.*, c.name AS customer_name FROM invoices i " +
                       "LEFT JOIN customers c ON i.customer_id = c.id " +
                       "WHERE i.customer_id = ? ORDER BY i.id DESC";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setInt(1, customerId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    invoices.add(mapResultSetToInvoice(rs));
                }
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

    public long getTotalDebtByCustomerId(int customerId) throws SQLException {
        String query = "SELECT SUM(debt) FROM invoices WHERE customer_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setInt(1, customerId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getLong(1);
                }
            }
        }
        return 0;
    }

    public Invoice findById(int id) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            return findById(conn, id);
        }
    }

    public Invoice findById(Connection conn, int id) throws SQLException {
        String query = "SELECT i.*, c.name AS customer_name FROM invoices i " +
                       "LEFT JOIN customers c ON i.customer_id = c.id " +
                       "WHERE i.id = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setInt(1, id);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToInvoice(rs);
                }
            }
        }
        return null;
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
        try {
            invoice.setDeliveryDate(rs.getString("delivery_date"));
        } catch (SQLException ignored) {
        }
        try {
            invoice.setCustomerName(rs.getString("customer_name"));
        } catch (SQLException ignored) {
            // Trường customer_name có thể không tồn tại nếu query đơn giản không join
        }
        return invoice;
    }
}
