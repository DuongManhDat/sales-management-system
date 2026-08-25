package com.shop.dao;

import com.shop.model.LowStockRow;
import com.shop.model.TopProductRow;
import javafx.scene.chart.XYChart;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class DashboardDao {
    public long getTotalRevenue(Connection conn, String dateStr) throws SQLException {
        String query = "SELECT SUM(total) FROM invoices WHERE date(invoice_date) = ? AND status='PAID'";
        try (PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, dateStr);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getLong(1);
                }
            }
        }
        return 0;
    }

    public long getGrossProfit(Connection conn, String dateStr) throws SQLException {
        String query = "SELECT SUM((ii.sale_price - ii.cost_price) * ii.qty) " +
                       "FROM invoice_items ii JOIN invoices i ON i.id = ii.invoice_id " +
                       "WHERE date(i.invoice_date) = ? AND i.status='PAID'";
        try (PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, dateStr);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getLong(1);
                }
            }
        }
        return 0;
    }

    public int getTotalOrders(Connection conn, String dateStr) throws SQLException {
        String query = "SELECT COUNT(*) FROM invoices WHERE date(invoice_date) = ? AND status='PAID'";
        try (PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, dateStr);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }
        return 0;
    }

    public List<XYChart.Data<String, Number>> getRevenueByHour(Connection conn, String dateStr) throws SQLException {
        List<XYChart.Data<String, Number>> result = new ArrayList<>();
        for (int i = 0; i < 24; i++) {
            result.add(new XYChart.Data<>(String.format("%02d:00", i), 0L));
        }
        
        String query = "SELECT strftime('%H', invoice_date) as hour, SUM(total) as revenue " +
                       "FROM invoices WHERE date(invoice_date) = ? AND status='PAID' " +
                       "GROUP BY hour";
        try (PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, dateStr);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    String hourStr = rs.getString("hour");
                    if (hourStr != null) {
                        int hour = Integer.parseInt(hourStr);
                        result.get(hour).setYValue(rs.getLong("revenue"));
                    }
                }
            }
        }
        return result;
    }

    public List<XYChart.Data<String, Number>> getRevenueByDayOfWeek(Connection conn, String startDateStr, String endDateStr) throws SQLException {
        List<XYChart.Data<String, Number>> result = new ArrayList<>();
        String[] days = {"CN", "T2", "T3", "T4", "T5", "T6", "T7"};
        for (String day : days) {
            result.add(new XYChart.Data<>(day, 0L));
        }

        String query = "SELECT strftime('%w', invoice_date) as dow, SUM(total) as revenue " +
                       "FROM invoices WHERE date(invoice_date) BETWEEN ? AND ? AND status='PAID' " +
                       "GROUP BY dow";
        try (PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, startDateStr);
            pstmt.setString(2, endDateStr);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    String dowStr = rs.getString("dow");
                    if (dowStr != null) {
                        int dow = Integer.parseInt(dowStr); // 0=Sunday, 1=Monday...
                        result.get(dow).setYValue(rs.getLong("revenue"));
                    }
                }
            }
        }
        return result;
    }

    public List<TopProductRow> getTopProducts(Connection conn, String dateStr, int limit) throws SQLException {
        List<TopProductRow> result = new ArrayList<>();
        String query = "SELECT p.name, u.name as unit, SUM(ii.qty) as qty, SUM(ii.amount) as revenue " +
                       "FROM invoice_items ii " +
                       "JOIN invoices i ON i.id = ii.invoice_id " +
                       "JOIN products p ON p.id = ii.product_id " +
                       "LEFT JOIN units u ON u.id = p.unit_id " +
                       "WHERE date(i.invoice_date) = ? AND i.status='PAID' " +
                       "GROUP BY ii.product_id " +
                       "ORDER BY qty DESC LIMIT ?";
        try (PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, dateStr);
            pstmt.setInt(2, limit);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    result.add(new TopProductRow(
                        rs.getString("name"),
                        rs.getString("unit") != null ? rs.getString("unit") : "",
                        rs.getDouble("qty"),
                        rs.getLong("revenue")
                    ));
                }
            }
        }
        return result;
    }

    public List<LowStockRow> getLowStockProducts(Connection conn, int threshold) throws SQLException {
        List<LowStockRow> result = new ArrayList<>();
        String query = "SELECT p.code, p.name, u.name as unit, p.stock_qty " +
                       "FROM products p " +
                       "LEFT JOIN units u ON u.id = p.unit_id " +
                       "WHERE p.stock_qty <= ? AND p.status=1 " +
                       "ORDER BY p.stock_qty ASC";
        try (PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setInt(1, threshold);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    result.add(new LowStockRow(
                        rs.getString("code"),
                        rs.getString("name"),
                        rs.getString("unit") != null ? rs.getString("unit") : "",
                        rs.getDouble("stock_qty")
                    ));
                }
            }
        }
        return result;
    }
}
