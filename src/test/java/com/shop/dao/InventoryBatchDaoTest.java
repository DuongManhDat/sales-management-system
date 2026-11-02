package com.shop.dao;

import com.shop.infra.db.SchemaInitializer;
import com.shop.model.InventoryBatch;
import com.shop.util.DBConnection;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class InventoryBatchDaoTest {

    private InventoryBatchDao batchDao;

    @BeforeEach
    void setUp() throws SQLException {
        System.setProperty("db.url", "jdbc:sqlite::memory:");
        SchemaInitializer.initialize();

        batchDao = new InventoryBatchDao();

        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.executeUpdate("UPDATE inventory_batches SET purchase_item_id = NULL");
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

            stmt.executeUpdate("INSERT INTO units (id, name) VALUES (1, 'Cái')");
            stmt.executeUpdate("INSERT INTO products (id, code, name, unit_id, sale_price, stock_qty, created_at, updated_at) " +
                    "VALUES (1, 'SP001', 'Sản phẩm Test', 1, 50000, 20, '2026-10-28 00:00:00', '2026-10-28 00:00:00')");

            // Lô 1: nhập ngày 2026-10-01, giá vốn 20.000, tồn 5
            stmt.executeUpdate("INSERT INTO inventory_batches (id, product_id, cost_price, qty_initial, qty_remaining, received_date, created_at) " +
                    "VALUES (10, 1, 20000, 10, 5, '2026-10-01', '2026-10-01 10:00:00')");

            // Lô 2: nhập ngày 2026-10-15, giá vốn 30.000, tồn 15
            stmt.executeUpdate("INSERT INTO inventory_batches (id, product_id, cost_price, qty_initial, qty_remaining, received_date, created_at) " +
                    "VALUES (11, 1, 30000, 15, 15, '2026-10-15', '2026-10-15 10:00:00')");

            // Lô 3: nhập ngày 2026-09-01 nhưng đã hết hàng (tồn 0) -> không được trả về
            stmt.executeUpdate("INSERT INTO inventory_batches (id, product_id, cost_price, qty_initial, qty_remaining, received_date, created_at) " +
                    "VALUES (12, 1, 15000, 10, 0, '2026-09-01', '2026-09-01 10:00:00')");
        }
    }

    @Test
    void testFindAvailableBatchesOrderedByReceivedDateAsc() throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            List<InventoryBatch> batches = batchDao.findAvailableBatchesByProductId(conn, 1);
            assertEquals(2, batches.size());
            assertEquals(10, batches.get(0).getId());
            assertEquals(20000L, batches.get(0).getCostPrice());
            assertEquals(5, batches.get(0).getQtyRemaining());

            assertEquals(11, batches.get(1).getId());
            assertEquals(30000L, batches.get(1).getCostPrice());
            assertEquals(15, batches.get(1).getQtyRemaining());
        }
    }

    @Test
    void testUpdateQtyRemaining() throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            batchDao.updateQtyRemaining(conn, 10, 2);
            List<InventoryBatch> batches = batchDao.findAvailableBatchesByProductId(conn, 1);
            assertEquals(2, batches.get(0).getQtyRemaining());
        }
    }

    @Test
    void testFindLatestBatchByProductId() throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            InventoryBatch latest = batchDao.findLatestBatchByProductId(conn, 1);
            assertNotNull(latest);
            assertEquals(11, latest.getId());
        }
    }
}
