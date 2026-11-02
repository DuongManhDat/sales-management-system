package com.shop.service;

import com.shop.dao.DashboardDao;
import com.shop.dao.InventoryBatchDao;
import com.shop.infra.db.SchemaInitializer;
import com.shop.model.InventoryBatch;
import com.shop.model.Invoice;
import com.shop.model.InvoiceItem;
import com.shop.util.DBConnection;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class FifoSalesCostTest {

    private SalesService salesService;
    private InventoryBatchDao batchDao;
    private DashboardDao dashboardDao;

    @BeforeEach
    void setUp() throws SQLException {
        System.setProperty("db.url", "jdbc:sqlite::memory:");
        SchemaInitializer.initialize();

        salesService = new SalesService();
        batchDao = new InventoryBatchDao();
        dashboardDao = new DashboardDao();

        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.executeUpdate("DELETE FROM invoice_item_batches");
            stmt.executeUpdate("DELETE FROM supplier_payments");
            stmt.executeUpdate("DELETE FROM return_invoice_items");
            stmt.executeUpdate("DELETE FROM return_invoices");
            stmt.executeUpdate("DELETE FROM payments");
            stmt.executeUpdate("DELETE FROM invoice_items");
            stmt.executeUpdate("DELETE FROM invoices");
            stmt.executeUpdate("DELETE FROM stock_adjustment_items");
            stmt.executeUpdate("DELETE FROM stock_adjustments");
            stmt.executeUpdate("DELETE FROM purchase_items");
            stmt.executeUpdate("DELETE FROM inventory_batches");
            stmt.executeUpdate("DELETE FROM purchases");
            stmt.executeUpdate("DELETE FROM stock_movements");
            stmt.executeUpdate("DELETE FROM price_history");
            stmt.executeUpdate("DELETE FROM products");
            stmt.executeUpdate("DELETE FROM suppliers");
            stmt.executeUpdate("DELETE FROM units");
            stmt.executeUpdate("DELETE FROM customers");

            stmt.executeUpdate("INSERT INTO units (id, name) VALUES (1, 'Hộp')");
            stmt.executeUpdate("INSERT INTO customers (id, code, name, phone, created_at) VALUES (1, 'KH01', 'Khách VIP', '0911223344', '2026-10-28 00:00:00')");

            // Tạo sản phẩm SP01 bán giá 100.000
            stmt.executeUpdate("INSERT INTO products (id, code, name, unit_id, sale_price, stock_qty, created_at, updated_at) " +
                    "VALUES (1, 'SP001', 'Thuốc Ho', 1, 100000, 20, '2026-10-28 00:00:00', '2026-10-28 00:00:00')");

            // Lô 1: nhập ngày 2026-10-01, giá vốn 40.000, tồn 5
            stmt.executeUpdate("INSERT INTO inventory_batches (id, product_id, cost_price, qty_initial, qty_remaining, received_date, created_at) " +
                    "VALUES (1, 1, 40000, 5, 5, '2026-10-01', '2026-10-01 10:00:00')");

            // Lô 2: nhập ngày 2026-10-10, giá vốn 60.000, tồn 15
            stmt.executeUpdate("INSERT INTO inventory_batches (id, product_id, cost_price, qty_initial, qty_remaining, received_date, created_at) " +
                    "VALUES (2, 1, 60000, 15, 15, '2026-10-10', '2026-10-10 10:00:00')");
        }
    }

    @Test
    void testCreateInvoiceCalculatesWeightedCostPriceAndDeductsBatches() throws SQLException {
        // Bán 8 hộp SP01:
        // Lấy 5 hộp từ Lô 1 (giá 40.000 = 200.000)
        // Lấy 3 hộp từ Lô 2 (giá 60.000 = 180.000)
        // Tổng giá vốn = 380.000. Giá vốn bình quân = 380.000 / 8 = 47.500 đ/hộp
        // Doanh thu = 8 * 100.000 = 800.000
        // Lợi nhuận gộp = 800.000 - 380.000 = 420.000
        Invoice invoice = new Invoice();
        invoice.setCode("HD-TEST-FIFO");
        invoice.setCustomerId(1);
        String todayStr = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE);
        invoice.setInvoiceDate(todayStr + " 10:00:00");
        invoice.setSubtotal(800000L);
        invoice.setTotal(800000L);
        invoice.setPaid(800000L);
        invoice.setDebt(0L);
        invoice.setStatus("PAID");

        List<InvoiceItem> items = new ArrayList<>();
        InvoiceItem item = new InvoiceItem();
        item.setProductId(1);
        item.setQty(8);
        item.setSalePrice(100000L);
        item.setAmount(800000L);
        items.add(item);

        salesService.createInvoice(invoice, items);

        // 1. Kiểm tra tồn lô kho
        try (Connection conn = DBConnection.getConnection()) {
            List<InventoryBatch> batches = batchDao.findAvailableBatchesByProductId(conn, 1);
            // Lô 1 đã hết (qty_remaining = 0), chỉ còn Lô 2
            assertEquals(1, batches.size());
            assertEquals(2, batches.get(0).getId());
            assertEquals(12, batches.get(0).getQtyRemaining()); // 15 - 3 = 12

            // 2. Kiểm tra cost_price của invoice_item
            assertEquals(47500L, items.get(0).getCostPrice());

            // 3. Kiểm tra lợi nhuận gộp trên Dashboard
            long grossProfit = dashboardDao.getGrossProfit(conn, todayStr);
            assertEquals(420000L, grossProfit);
        }
    }

    @Test
    void testCompleteOrderCalculatesCostPriceAndDeductsBatches() throws SQLException {
        // Tạo đơn đặt hàng trước
        Invoice order = new Invoice();
        order.setCode("DH-TEST-FIFO");
        order.setCustomerId(1);
        String todayStr = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE);
        order.setInvoiceDate(todayStr + " 09:00:00");
        order.setSubtotal(400000L);
        order.setTotal(400000L);

        List<InvoiceItem> items = new ArrayList<>();
        InvoiceItem item = new InvoiceItem();
        item.setProductId(1);
        item.setQty(4);
        item.setSalePrice(100000L);
        item.setAmount(400000L);
        items.add(item);

        salesService.createOrder(order, items, 100000L);

        // Lúc này Lô 1 vẫn nguyên (5 hộp) vì đơn PENDING chưa trừ kho
        try (Connection conn = DBConnection.getConnection()) {
            List<InventoryBatch> batchesBefore = batchDao.findAvailableBatchesByProductId(conn, 1);
            assertEquals(5, batchesBefore.get(0).getQtyRemaining());
        }

        // Hoàn thành đơn hàng
        salesService.completeOrder(order.getId());

        // Sau khi hoàn thành: Lô 1 giảm từ 5 xuống 1 (lấy 4 hộp giá vốn 40.000)
        try (Connection conn = DBConnection.getConnection()) {
            List<InventoryBatch> batchesAfter = batchDao.findAvailableBatchesByProductId(conn, 1);
            assertEquals(1, batchesAfter.get(0).getQtyRemaining());

            // Lợi nhuận gộp = 4 * 100.000 - 4 * 40.000 = 240.000
            long grossProfit = dashboardDao.getGrossProfit(conn, todayStr);
            assertEquals(240000L, grossProfit);
        }
    }
}
