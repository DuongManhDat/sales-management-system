package com.shop.dao;

import com.shop.infra.db.SchemaInitializer;
import com.shop.model.LowStockRow;
import com.shop.model.TopProductRow;
import com.shop.util.DBConnection;
import javafx.scene.chart.XYChart;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DashboardDaoTest {

    private DashboardDao dashboardDao;

    @BeforeEach
    void setUp() throws SQLException {
        System.setProperty("db.url", "jdbc:sqlite::memory:");
        SchemaInitializer.initialize();

        dashboardDao = new DashboardDao();

        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.executeUpdate("DELETE FROM invoice_items");
            stmt.executeUpdate("DELETE FROM invoices");
            stmt.executeUpdate("DELETE FROM customers");
            stmt.executeUpdate("DELETE FROM invoice_item_batches");
            stmt.executeUpdate("DELETE FROM supplier_payments");
            stmt.executeUpdate("DELETE FROM return_invoice_items");
            stmt.executeUpdate("DELETE FROM return_invoices");
            stmt.executeUpdate("DELETE FROM payments");
            stmt.executeUpdate("DELETE FROM stock_adjustment_items");
            stmt.executeUpdate("DELETE FROM stock_adjustments");
            stmt.executeUpdate("DELETE FROM purchase_items");
            stmt.executeUpdate("DELETE FROM inventory_batches");
            stmt.executeUpdate("DELETE FROM purchases");
            stmt.executeUpdate("DELETE FROM stock_movements");
            stmt.executeUpdate("DELETE FROM price_history");
            stmt.executeUpdate("DELETE FROM products");
            stmt.executeUpdate("DELETE FROM units");

            stmt.executeUpdate("INSERT INTO units (id, name) VALUES (1, 'Hộp')");

            stmt.executeUpdate("INSERT INTO products (id, code, name, unit_id, sale_price, stock_qty, created_at, updated_at) " +
                    "VALUES (10, 'SP001', 'Thuốc Paracetamol', 1, 100000, 3, '2026-10-28 00:00:00', '2026-10-28 00:00:00')");
            stmt.executeUpdate("INSERT INTO products (id, code, name, unit_id, sale_price, stock_qty, created_at, updated_at) " +
                    "VALUES (20, 'SP002', 'Khẩu trang 4D', 1, 50000, 25, '2026-10-28 00:00:00', '2026-10-28 00:00:00')");

            stmt.executeUpdate("INSERT INTO customers (id, code, name, phone, created_at) " +
                    "VALUES (1, 'KH001', 'Nguyễn Văn A', '0901234567', '2026-10-28 00:00:00')");

            // 1. Đơn PAID (trả đủ): tổng 100.000, vốn 60.000 -> lãi 40.000
            stmt.executeUpdate("INSERT INTO invoices (id, code, invoice_date, customer_id, subtotal, discount_pct, discount_amt, total, paid, debt, status) " +
                    "VALUES (101, 'HD001', '2026-10-28 08:30:00', 1, 100000, 0, 0, 100000, 100000, 0, 'PAID')");
            stmt.executeUpdate("INSERT INTO invoice_items (id, invoice_id, product_id, qty, sale_price, cost_price, amount) " +
                    "VALUES (1, 101, 10, 1, 100000, 60000, 100000)");

            // 2. Đơn PARTIAL (còn nợ): tổng 200.000, vốn 120.000 -> lãi 80.000
            stmt.executeUpdate("INSERT INTO invoices (id, code, invoice_date, customer_id, subtotal, discount_pct, discount_amt, total, paid, debt, status) " +
                    "VALUES (102, 'HD002', '2026-10-28 09:15:00', 1, 200000, 0, 0, 200000, 50000, 150000, 'PARTIAL')");
            stmt.executeUpdate("INSERT INTO invoice_items (id, invoice_id, product_id, qty, sale_price, cost_price, amount) " +
                    "VALUES (2, 102, 10, 2, 100000, 60000, 200000)");

            // 3. Đơn CANCELLED (đã hủy): tổng 150.000 -> KHÔNG được tính
            stmt.executeUpdate("INSERT INTO invoices (id, code, invoice_date, customer_id, subtotal, discount_pct, discount_amt, total, paid, debt, status) " +
                    "VALUES (103, 'HD003', '2026-10-28 10:00:00', 1, 150000, 0, 0, 150000, 0, 150000, 'CANCELLED')");
            stmt.executeUpdate("INSERT INTO invoice_items (id, invoice_id, product_id, qty, sale_price, cost_price, amount) " +
                    "VALUES (3, 103, 10, 1, 150000, 60000, 150000)");

            // 4. Đơn 'Đã hủy': tổng 50.000 -> KHÔNG được tính
            stmt.executeUpdate("INSERT INTO invoices (id, code, invoice_date, customer_id, subtotal, discount_pct, discount_amt, total, paid, debt, status) " +
                    "VALUES (104, 'HD004', '2026-10-28 11:00:00', 1, 50000, 0, 0, 50000, 0, 50000, 'Đã hủy')");
            stmt.executeUpdate("INSERT INTO invoice_items (id, invoice_id, product_id, qty, sale_price, cost_price, amount) " +
                    "VALUES (4, 104, 20, 1, 50000, 30000, 50000)");
        }
    }

    @Test
    void testGetTotalRevenueIncludesDebtExcludesCancelled() throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            long totalRevenue = dashboardDao.getTotalRevenue(conn, "2026-10-28");
            // HD001 (PAID: 100.000) + HD002 (PARTIAL: 200.000) = 300.000
            assertEquals(300000L, totalRevenue);
        }
    }

    @Test
    void testGetGrossProfitIncludesDebtExcludesCancelled() throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            long grossProfit = dashboardDao.getGrossProfit(conn, "2026-10-28");
            // HD001: 40.000 + HD002: 80.000 = 120.000
            assertEquals(120000L, grossProfit);
        }
    }

    @Test
    void testGetTotalOrdersIncludesDebtExcludesCancelled() throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            int totalOrders = dashboardDao.getTotalOrders(conn, "2026-10-28");
            // HD001 và HD002 -> 2 đơn
            assertEquals(2, totalOrders);
        }
    }

    @Test
    void testGetRevenueByHour() throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            List<XYChart.Data<String, Number>> hourly = dashboardDao.getRevenueByHour(conn, "2026-10-28");
            assertEquals(24, hourly.size());
            assertEquals(100000L, hourly.get(8).getYValue().longValue());
            assertEquals(200000L, hourly.get(9).getYValue().longValue());
            assertEquals(0L, hourly.get(10).getYValue().longValue()); // CANCELLED không tính
        }
    }

    @Test
    void testGetRevenueByDayOfWeek() throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            List<XYChart.Data<String, Number>> dow = dashboardDao.getRevenueByDayOfWeek(conn, "2026-10-22", "2026-10-28");
            assertEquals(7, dow.size());
            // 2026-10-28 là Thứ Tư (Wednesday - dow 3)
            assertEquals(300000L, dow.get(3).getYValue().longValue());
        }
    }

    @Test
    void testGetTopProducts() throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            List<TopProductRow> top = dashboardDao.getTopProducts(conn, "2026-10-28", 5);
            assertFalse(top.isEmpty());
            assertEquals("Thuốc Paracetamol", top.get(0).productName());
            assertEquals(3.0, top.get(0).qty());
            assertEquals(300000L, top.get(0).revenue());
        }
    }

    @Test
    void testGetLowStockProducts() throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            List<LowStockRow> lowStock = dashboardDao.getLowStockProducts(conn, 5);
            assertEquals(1, lowStock.size());
            assertEquals("SP001", lowStock.get(0).code());
        }
    }
}

