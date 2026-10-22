package com.shop.dao;

import com.shop.infra.db.SchemaInitializer;
import com.shop.model.ReturnInvoice;
import com.shop.model.ReturnInvoiceItem;
import com.shop.util.DBConnection;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ReturnInvoiceItemDaoTest {

    private ReturnInvoiceItemDao returnInvoiceItemDao;
    private ReturnInvoiceDao returnInvoiceDao;

    @BeforeEach
    void setUp() throws SQLException {
        System.setProperty("db.url", "jdbc:sqlite::memory:");
        SchemaInitializer.initialize();

        returnInvoiceItemDao = new ReturnInvoiceItemDao();
        returnInvoiceDao = new ReturnInvoiceDao();

        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.executeUpdate("DELETE FROM return_invoice_items");
            stmt.executeUpdate("DELETE FROM return_invoices");
            stmt.executeUpdate("DELETE FROM invoice_items");
            stmt.executeUpdate("DELETE FROM invoices");
            stmt.executeUpdate("DELETE FROM customers");
            stmt.executeUpdate("DELETE FROM products");
            stmt.executeUpdate("DELETE FROM units");

            stmt.executeUpdate("INSERT INTO units (id, name) VALUES (1, 'Hộp')");
            stmt.executeUpdate("INSERT INTO products (id, code, name, unit_id, sale_price, stock_qty, created_at, updated_at) " +
                    "VALUES (10, 'SP001', 'Thuốc cảm', 1, 50000, 100, '2026-10-20 00:00:00', '2026-10-20 00:00:00')");
            stmt.executeUpdate("INSERT INTO customers (id, code, name, phone, created_at) " +
                    "VALUES (1, 'KH001', 'Nguyễn Văn A', '0901234567', '2026-10-20 00:00:00')");
            stmt.executeUpdate("INSERT INTO invoices (id, code, invoice_date, customer_id, subtotal, discount_pct, discount_amt, total, paid, debt, status) " +
                    "VALUES (100, 'HD0001', '2026-10-20', 1, 250000, 0, 0, 250000, 250000, 0, 'COMPLETED')");
            stmt.executeUpdate("INSERT INTO invoice_items (id, invoice_id, product_id, qty, sale_price, amount) " +
                    "VALUES (501, 100, 10, 5, 50000, 250000)");
        }
    }

    @Test
    void testGetReturnedQtyMapByInvoiceId_IgnoresCanceledReturns() throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            // Phiếu trả 1: COMPLETED, trả 2 hộp
            ReturnInvoice ri1 = new ReturnInvoice();
            ri1.setCode("TH0001");
            ri1.setInvoiceId(100);
            ri1.setCustomerId(1);
            ri1.setReturnDate("2026-10-21 10:00:00");
            ri1.setTotalRefund(100000);
            ri1.setReturnFee(0);
            ri1.setStatus("COMPLETED");
            returnInvoiceDao.insert(conn, ri1);

            ReturnInvoiceItem item1 = new ReturnInvoiceItem();
            item1.setReturnInvoiceId(ri1.getId());
            item1.setInvoiceItemId(501);
            item1.setProductId(10);
            item1.setReturnQty(2);
            item1.setRefundPrice(50000);
            item1.setAmount(100000);
            returnInvoiceItemDao.insert(conn, item1);

            // Phiếu trả 2: CANCELED, trả 1 hộp (đã hủy)
            ReturnInvoice ri2 = new ReturnInvoice();
            ri2.setCode("TH0002");
            ri2.setInvoiceId(100);
            ri2.setCustomerId(1);
            ri2.setReturnDate("2026-10-22 10:00:00");
            ri2.setTotalRefund(50000);
            ri2.setReturnFee(0);
            ri2.setStatus("CANCELED");
            returnInvoiceDao.insert(conn, ri2);

            ReturnInvoiceItem item2 = new ReturnInvoiceItem();
            item2.setReturnInvoiceId(ri2.getId());
            item2.setInvoiceItemId(501);
            item2.setProductId(10);
            item2.setReturnQty(1);
            item2.setRefundPrice(50000);
            item2.setAmount(50000);
            returnInvoiceItemDao.insert(conn, item2);
        }

        Map<Integer, Integer> map = returnInvoiceItemDao.getReturnedQtyMapByInvoiceId(100);
        assertNotNull(map);
        assertEquals(1, map.size());
        // Chỉ ghi nhận phiếu COMPLETED (2 cái), bỏ qua phiếu CANCELED
        assertEquals(2, map.get(501));
    }

    @AfterEach
    void tearDown() throws SQLException {
        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.executeUpdate("DELETE FROM return_invoice_items");
            stmt.executeUpdate("DELETE FROM return_invoices");
            stmt.executeUpdate("DELETE FROM invoice_items");
            stmt.executeUpdate("DELETE FROM invoices");
            stmt.executeUpdate("DELETE FROM customers");
            stmt.executeUpdate("DELETE FROM products");
            stmt.executeUpdate("DELETE FROM units");
        }
    }
}
