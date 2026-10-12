package com.shop.service;

import com.shop.dao.InvoiceDao;
import com.shop.dao.PaymentDao;
import com.shop.dao.ProductDao;
import com.shop.model.Invoice;
import com.shop.model.InvoiceItem;
import com.shop.model.Payment;
import com.shop.model.Product;
import com.shop.util.DBConnection;
import org.junit.jupiter.api.*;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class OrderServiceTest {

    private static SalesService salesService;
    private static ProductDao productDao;
    private static InvoiceDao invoiceDao;
    private static PaymentDao paymentDao;
    private static int defaultUnitId;

    @BeforeAll
    static void setupAll() throws Exception {
        System.setProperty("db.url", "jdbc:sqlite::memory:");
        DBConnection.close();
        com.shop.infra.db.SchemaInitializer.initialize();
        salesService = new SalesService();
        productDao = new ProductDao();
        invoiceDao = new InvoiceDao();
        paymentDao = new PaymentDao();

        try (Connection conn = DBConnection.getConnection();
             var stmt = conn.createStatement()) {
            stmt.execute("INSERT OR IGNORE INTO units (name) VALUES ('Cái')");
            try (var rs = stmt.executeQuery("SELECT id FROM units WHERE name = 'Cái'")) {
                if (rs.next()) defaultUnitId = rs.getInt(1);
            }
        }
    }

    @AfterAll
    static void tearDownAll() {
        DBConnection.close();
    }

    private int insertTestProduct(String name, double stockQty) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            Product p = new Product();
            p.setName(name);
            p.setUnitId(defaultUnitId);
            p.setSalePrice(100_000L);
            p.setStockQty(stockQty);
            return productDao.insert(conn, p);
        }
    }

    private int insertTestCustomer(String phone) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            try (var pstmt = conn.prepareStatement(
                    "INSERT OR IGNORE INTO customers (code, name, phone, created_at) VALUES (?,?,?,datetime('now'))",
                    java.sql.Statement.RETURN_GENERATED_KEYS)) {
                pstmt.setString(1, "KH_" + phone);
                pstmt.setString(2, "Khach " + phone);
                pstmt.setString(3, phone);
                pstmt.executeUpdate();
                try (var rs = pstmt.getGeneratedKeys()) {
                    if (rs.next()) return rs.getInt(1);
                }
            }
        }
        throw new SQLException("Failed to insert test customer");
    }

    @Test
    @org.junit.jupiter.api.Order(1)
    void createOrder_shouldIncreaseReservedQty() throws Exception {
        int productId = insertTestProduct("Áo Test", 10);
        int customerId = insertTestCustomer("0901000001");

        Invoice inv = new Invoice();
        inv.setCustomerId(customerId);
        inv.setTotal(300_000L);
        inv.setSubtotal(300_000L);
        inv.setDeliveryDate("2026-10-20");

        InvoiceItem item = new InvoiceItem();
        item.setProductId(productId);
        item.setQty(3);
        item.setSalePrice(100_000L);
        item.setAmount(300_000L);

        salesService.createOrder(inv, List.of(item), 0L);

        try (Connection conn = DBConnection.getConnection()) {
            double available = productDao.getAvailableQty(conn, productId);
            assertEquals(7.0, available, 0.001); // 10 - 3 = 7
        }
        assertEquals("PENDING", invoiceDao.findById(inv.getId()).getStatus());
    }

    @Test
    @org.junit.jupiter.api.Order(2)
    void createOrder_shouldFailWhenStockInsufficient() throws Exception {
        int productId = insertTestProduct("Quần Test", 2);
        int customerId = insertTestCustomer("0901000002");

        Invoice inv = new Invoice();
        inv.setCustomerId(customerId);
        inv.setTotal(500_000L);
        inv.setSubtotal(500_000L);
        inv.setDeliveryDate("2026-10-21");

        InvoiceItem item = new InvoiceItem();
        item.setProductId(productId);
        item.setQty(5); // vượt quá tồn kho = 2
        item.setSalePrice(100_000L);
        item.setAmount(500_000L);

        assertThrows(IllegalStateException.class, () ->
                salesService.createOrder(inv, List.of(item), 0L));
    }

    @Test
    @org.junit.jupiter.api.Order(3)
    void completeOrder_shouldDecreaseStockAndClearReserved() throws Exception {
        int productId = insertTestProduct("Túi Test", 5);
        int customerId = insertTestCustomer("0901000003");

        Invoice inv = new Invoice();
        inv.setCustomerId(customerId);
        inv.setTotal(200_000L);
        inv.setSubtotal(200_000L);
        inv.setDeliveryDate("2026-10-22");

        InvoiceItem item = new InvoiceItem();
        item.setProductId(productId);
        item.setQty(2);
        item.setSalePrice(100_000L);
        item.setAmount(200_000L);

        salesService.createOrder(inv, List.of(item), 0L);
        salesService.completeOrder(inv.getId());

        try (Connection conn = DBConnection.getConnection()) {
            Product p = productDao.getById(conn, productId);
            assertEquals(3.0, p.getStockQty(), 0.001); // 5 - 2 = 3
            assertEquals(0, p.getReservedQty());        // reserved giải phóng
        }
        assertEquals("COMPLETED", invoiceDao.findById(inv.getId()).getStatus());
    }

    @Test
    @org.junit.jupiter.api.Order(4)
    void cancelOrder_shouldReleaseReservedQty() throws Exception {
        int productId = insertTestProduct("Mũ Test", 8);
        int customerId = insertTestCustomer("0901000004");

        Invoice inv = new Invoice();
        inv.setCustomerId(customerId);
        inv.setTotal(300_000L);
        inv.setSubtotal(300_000L);
        inv.setDeliveryDate("2026-10-23");

        InvoiceItem item = new InvoiceItem();
        item.setProductId(productId);
        item.setQty(3);
        item.setSalePrice(100_000L);
        item.setAmount(300_000L);

        salesService.createOrder(inv, List.of(item), 0L);
        salesService.cancelOrder(inv.getId());

        try (Connection conn = DBConnection.getConnection()) {
            double available = productDao.getAvailableQty(conn, productId);
            assertEquals(8.0, available, 0.001); // reserved giải phóng → available = 8
        }
        assertEquals("CANCELLED", invoiceDao.findById(inv.getId()).getStatus());
    }

    @Test
    @org.junit.jupiter.api.Order(5)
    void createOrder_shouldRecordDepositPayment() throws Exception {
        int productId = insertTestProduct("Kính Test", 10);
        int customerId = insertTestCustomer("0901000005");

        Invoice inv = new Invoice();
        inv.setCustomerId(customerId);
        inv.setTotal(500_000L);
        inv.setSubtotal(500_000L);
        inv.setDeliveryDate("2026-10-25");

        InvoiceItem item = new InvoiceItem();
        item.setProductId(productId);
        item.setQty(2);
        item.setSalePrice(250_000L);
        item.setAmount(500_000L);

        salesService.createOrder(inv, List.of(item), 200_000L);

        Invoice saved = invoiceDao.findById(inv.getId());
        assertEquals(200_000L, saved.getPaid());
        assertEquals(300_000L, saved.getDebt());

        List<Payment> payments = paymentDao.findByInvoiceId(inv.getId());
        assertEquals(1, payments.size());
        assertEquals(200_000L, payments.get(0).getAmount());
        assertEquals("DEPOSIT", payments.get(0).getPaymentType());
    }
}
