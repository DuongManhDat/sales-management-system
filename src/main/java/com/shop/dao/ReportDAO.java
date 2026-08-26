package com.shop.dao;

import com.shop.model.InventoryReportRow;
import com.shop.model.ProductReportRow;
import com.shop.model.RevenueReportRow;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class ReportDAO {

    public List<RevenueReportRow> getRevenueReport(Connection conn, String startDate, String endDate) throws SQLException {
        List<RevenueReportRow> result = new ArrayList<>();
        String sql = "SELECT report_date, SUM(total_revenue) as total_revenue, SUM(total_cost) as total_cost, SUM(total_profit) as total_profit " +
                     "FROM (" +
                     "  SELECT date(i.invoice_date) as report_date, " +
                     "  ii.amount as total_revenue, " +
                     "  (ii.qty * ii.cost_price) as total_cost, " +
                     "  (ii.amount - (ii.qty * ii.cost_price)) as total_profit " +
                     "  FROM invoice_items ii " +
                     "  JOIN invoices i ON i.id = ii.invoice_id " +
                     "  WHERE date(i.invoice_date) BETWEEN ? AND ? AND i.status = 'PAID' " +
                     "  UNION ALL " +
                     "  SELECT date(ri.return_date) as report_date, " +
                     "  -rii.amount as total_revenue, " +
                     "  -(rii.return_qty * ii.cost_price) as total_cost, " +
                     "  -(rii.amount - (rii.return_qty * ii.cost_price)) as total_profit " +
                     "  FROM return_invoice_items rii " +
                     "  JOIN return_invoices ri ON ri.id = rii.return_invoice_id " +
                     "  JOIN invoice_items ii ON ii.id = rii.invoice_item_id " +
                     "  WHERE date(ri.return_date) BETWEEN ? AND ? " +
                     ") " +
                     "GROUP BY report_date " +
                     "ORDER BY report_date ASC";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, startDate);
            stmt.setString(2, endDate);
            stmt.setString(3, startDate);
            stmt.setString(4, endDate);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    RevenueReportRow row = new RevenueReportRow();
                    row.setDate(LocalDate.parse(rs.getString("report_date")));
                    row.setRevenue(rs.getLong("total_revenue"));
                    row.setCost(rs.getLong("total_cost"));
                    row.setProfit(rs.getLong("total_profit"));
                    result.add(row);
                }
            }
        }
        return result;
    }

    public List<ProductReportRow> getProductReport(Connection conn, String startDate, String endDate) throws SQLException {
        List<ProductReportRow> result = new ArrayList<>();
        String sql = "SELECT p.id, p.name, " +
                     "SUM(ps.total_qty) as total_qty, " +
                     "SUM(ps.total_revenue) as total_revenue, " +
                     "SUM(ps.total_profit) as total_profit " +
                     "FROM (" +
                     "  SELECT ii.product_id, ii.qty as total_qty, ii.amount as total_revenue, " +
                     "  (ii.amount - (ii.qty * ii.cost_price)) as total_profit " +
                     "  FROM invoice_items ii " +
                     "  JOIN invoices i ON i.id = ii.invoice_id " +
                     "  WHERE date(i.invoice_date) BETWEEN ? AND ? AND i.status = 'PAID' " +
                     "  UNION ALL " +
                     "  SELECT rii.product_id, -rii.return_qty as total_qty, -rii.amount as total_revenue, " +
                     "  -(rii.amount - (rii.return_qty * ii.cost_price)) as total_profit " +
                     "  FROM return_invoice_items rii " +
                     "  JOIN return_invoices ri ON ri.id = rii.return_invoice_id " +
                     "  JOIN invoice_items ii ON ii.id = rii.invoice_item_id " +
                     "  WHERE date(ri.return_date) BETWEEN ? AND ? " +
                     ") ps " +
                     "JOIN products p ON p.id = ps.product_id " +
                     "GROUP BY p.id, p.name " +
                     "ORDER BY total_qty DESC";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, startDate);
            stmt.setString(2, endDate);
            stmt.setString(3, startDate);
            stmt.setString(4, endDate);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    ProductReportRow row = new ProductReportRow();
                    row.setProductId(rs.getInt("id"));
                    row.setProductName(rs.getString("name"));
                    row.setQtySold(rs.getInt("total_qty"));
                    row.setRevenue(rs.getLong("total_revenue"));
                    row.setProfit(rs.getLong("total_profit"));
                    result.add(row);
                }
            }
        }
        return result;
    }

    public List<InventoryReportRow> getInventoryReport(Connection conn, String startDate, String endDate) throws SQLException {
        List<InventoryReportRow> result = new ArrayList<>();
        String sql = "SELECT " +
                     "p.id, " +
                     "p.name, " +
                     "COALESCE(SUM(CASE WHEN date(sm.created_at) < ? AND sm.type = 'IN' THEN sm.qty_change ELSE 0 END) - " +
                     "SUM(CASE WHEN date(sm.created_at) < ? AND sm.type = 'OUT' THEN sm.qty_change ELSE 0 END), 0) as start_qty, " +
                     "COALESCE(SUM(CASE WHEN date(sm.created_at) BETWEEN ? AND ? AND sm.type = 'IN' THEN sm.qty_change ELSE 0 END), 0) as in_qty, " +
                     "COALESCE(SUM(CASE WHEN date(sm.created_at) BETWEEN ? AND ? AND sm.type = 'OUT' THEN sm.qty_change ELSE 0 END), 0) as out_qty " +
                     "FROM products p " +
                     "LEFT JOIN stock_movements sm ON p.id = sm.product_id " +
                     "WHERE p.deleted_at IS NULL " +
                     "GROUP BY p.id, p.name " +
                     "ORDER BY p.name ASC";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, startDate);
            stmt.setString(2, startDate);
            stmt.setString(3, startDate);
            stmt.setString(4, endDate);
            stmt.setString(5, startDate);
            stmt.setString(6, endDate);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    InventoryReportRow row = new InventoryReportRow();
                    row.setProductId(rs.getInt("id"));
                    row.setProductName(rs.getString("name"));
                    
                    int startQty = (int) rs.getDouble("start_qty");
                    int inQty = (int) rs.getDouble("in_qty");
                    int outQty = (int) rs.getDouble("out_qty");
                    
                    row.setStartQty(startQty);
                    row.setInQty(inQty);
                    row.setOutQty(outQty);
                    row.setEndQty(startQty + inQty - outQty);
                    
                    result.add(row);
                }
            }
        }
        return result;
    }
}
