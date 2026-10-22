package com.shop.dao;

import com.shop.infra.db.SchemaInitializer;
import com.shop.model.InvoiceItem;
import com.shop.util.DBConnection;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class InvoiceItemDaoTest {

    private InvoiceItemDao invoiceItemDao;

    @BeforeEach
    void setUp() throws SQLException {
        System.setProperty("db.url", "jdbc:sqlite::memory:");
        SchemaInitializer.initialize();

        invoiceItemDao = new InvoiceItemDao();

        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.executeUpdate("DELETE FROM invoice_items");
            stmt.executeUpdate("DELETE FROM invoices");
            stmt.executeUpdate("DELETE FROM customers");
            stmt.executeUpdate("DELETE FROM products");
            stmt.executeUpdate("DELETE FROM units");

            // Tạo unit
            stmt.executeUpdate("INSERT INTO units (id, name) VALUES (1, 'Hộp')");

            // Tạo product
            stmt.executeUpdate("INSERT INTO products (id, code, name, unit_id, sale_price, stock_qty, created_at, updated_at) " +
                    "VALUES (10, 'SP001', 'Thuốc cảm cúm', 1, 75000, 100, '2026-10-20 00:00:00', '2026-10-20 00:00:00')");

            // Tạo customer
            stmt.executeUpdate("INSERT INTO customers (id, code, name, phone, created_at) " +
                    "VALUES (1, 'KH001', 'Nguyễn Văn A', '0901234567', '2026-10-20 00:00:00')");

            // Tạo invoice
            stmt.executeUpdate("INSERT INTO invoices (id, code, invoice_date, customer_id, subtotal, discount_pct, discount_amt, total, paid, debt, status) " +
                    "VALUES (100, 'HD0001', '2026-10-20', 1, 150000, 0, 0, 150000, 150000, 0, 'COMPLETED')");
        }
    }

    @Test
    void testInsertAllAndFindByInvoiceIdWithProductInfo() throws SQLException {
        InvoiceItem item = new InvoiceItem();
        item.setInvoiceId(100);
        item.setProductId(10);
        item.setQty(2);
        item.setCostPrice(50000);
        item.setSalePrice(75000);
        item.setAmount(150000);

        try (Connection conn = DBConnection.getConnection()) {
            invoiceItemDao.insertAll(conn, Collections.singletonList(item));
        }

        List<InvoiceItem> items = invoiceItemDao.findByInvoiceId(100);
        assertEquals(1, items.size());

        InvoiceItem loaded = items.get(0);
        assertEquals(100, loaded.getInvoiceId());
        assertEquals(10, loaded.getProductId());
        assertEquals(2, loaded.getQty());
        assertEquals(75000, loaded.getSalePrice());
        assertEquals(150000, loaded.getAmount());
        assertEquals("SP001", loaded.getProductCode());
        assertEquals("Thuốc cảm cúm", loaded.getProductName());
        assertEquals("Hộp", loaded.getUnitName());
    }

    @org.junit.jupiter.api.AfterEach
    void tearDown() throws SQLException {
        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.executeUpdate("DELETE FROM invoice_items");
            stmt.executeUpdate("DELETE FROM invoices");
            stmt.executeUpdate("DELETE FROM customers");
            stmt.executeUpdate("DELETE FROM products");
            stmt.executeUpdate("DELETE FROM units");
        }
    }
}
