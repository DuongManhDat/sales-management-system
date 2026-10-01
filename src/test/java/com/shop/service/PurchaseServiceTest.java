package com.shop.service;

import com.shop.infra.db.SchemaInitializer;
import com.shop.model.Purchase;
import com.shop.model.PurchaseItem;
import com.shop.util.DBConnection;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PurchaseServiceTest {

    private PurchaseService purchaseService;
    private int testSupplierId;
    private int testProductId;

    @BeforeEach
    void setUp() throws SQLException {
        System.setProperty("db.url", "jdbc:sqlite::memory:");
        SchemaInitializer.initialize();
        purchaseService = new PurchaseService();

        try (Connection conn = DBConnection.getConnection()) {
            try (PreparedStatement u1 = conn.prepareStatement("UPDATE inventory_batches SET purchase_item_id = NULL");
                 PreparedStatement del0 = conn.prepareStatement("DELETE FROM invoice_item_batches");
                 PreparedStatement del1 = conn.prepareStatement("DELETE FROM supplier_payments");
                 PreparedStatement del2 = conn.prepareStatement("DELETE FROM return_invoice_items");
                 PreparedStatement del3 = conn.prepareStatement("DELETE FROM return_invoices");
                 PreparedStatement del4 = conn.prepareStatement("DELETE FROM payments");
                 PreparedStatement del5 = conn.prepareStatement("DELETE FROM invoice_items");
                 PreparedStatement del6 = conn.prepareStatement("DELETE FROM invoices");
                 PreparedStatement del7 = conn.prepareStatement("DELETE FROM stock_adjustment_items");
                 PreparedStatement del8 = conn.prepareStatement("DELETE FROM stock_adjustments");
                 PreparedStatement del9 = conn.prepareStatement("DELETE FROM purchase_items");
                 PreparedStatement del10 = conn.prepareStatement("DELETE FROM inventory_batches");
                 PreparedStatement del11 = conn.prepareStatement("DELETE FROM purchases");
                 PreparedStatement del12 = conn.prepareStatement("DELETE FROM stock_movements");
                 PreparedStatement del13 = conn.prepareStatement("DELETE FROM price_history");
                 PreparedStatement del14 = conn.prepareStatement("DELETE FROM products");
                 PreparedStatement del15 = conn.prepareStatement("DELETE FROM suppliers");
                 PreparedStatement del16 = conn.prepareStatement("DELETE FROM units")) {
                u1.executeUpdate();
                del0.executeUpdate();
                del1.executeUpdate();
                del2.executeUpdate();
                del3.executeUpdate();
                del4.executeUpdate();
                del5.executeUpdate();
                del6.executeUpdate();
                del7.executeUpdate();
                del8.executeUpdate();
                del9.executeUpdate();
                del10.executeUpdate();
                del11.executeUpdate();
                del12.executeUpdate();
                del13.executeUpdate();
                del14.executeUpdate();
                del15.executeUpdate();
                del16.executeUpdate();
            }

            // Tạo nhà cung cấp mẫu
            String insertSupplier = "INSERT INTO suppliers (code, name, phone, is_active, created_at) VALUES ('NCC_TEST', 'Nhà Cung Cấp Test', '0901234567', 1, datetime('now'))";
            try (PreparedStatement stmt = conn.prepareStatement(insertSupplier, PreparedStatement.RETURN_GENERATED_KEYS)) {
                stmt.executeUpdate();
                try (ResultSet rs = stmt.getGeneratedKeys()) {
                    if (rs.next()) {
                        testSupplierId = rs.getInt(1);
                    }
                }
            }

            // Tạo đơn vị tính mẫu riêng biệt cho test
            int unitId = 1;
            String insertUnit = "INSERT INTO units (name, status) VALUES ('Cái Test Nhập', 1)";
            try (PreparedStatement stmt = conn.prepareStatement(insertUnit, PreparedStatement.RETURN_GENERATED_KEYS)) {
                stmt.executeUpdate();
                try (ResultSet rs = stmt.getGeneratedKeys()) {
                    if (rs.next()) {
                        unitId = rs.getInt(1);
                    }
                }
            }

            // Tạo sản phẩm mẫu
            String insertProduct = "INSERT INTO products (code, name, unit_id, sale_price, stock_qty, created_at, updated_at) " +
                    "VALUES ('SP_TEST_PURCHASE', 'Sản Phẩm Test Nhập', ?, 50000, 10.0, datetime('now'), datetime('now'))";
            try (PreparedStatement stmt = conn.prepareStatement(insertProduct, PreparedStatement.RETURN_GENERATED_KEYS)) {
                stmt.setInt(1, unitId);
                stmt.executeUpdate();
                try (ResultSet rs = stmt.getGeneratedKeys()) {
                    if (rs.next()) {
                        testProductId = rs.getInt(1);
                    }
                }
            }
        }
    }

    @Test
    void testCreatePurchaseAndCheckStockAndDetail() throws SQLException {
        Purchase purchase = new Purchase();
        purchase.setCode("PN_TEST_001");
        purchase.setSupplierId(testSupplierId);
        purchase.setPurchaseDate("2026-09-10 10:00:00");
        purchase.setNote("Phiếu nhập hàng kiểm thử");
        purchase.setPaid(300000);

        List<PurchaseItem> items = new ArrayList<>();
        PurchaseItem item = new PurchaseItem();
        item.setProductId(testProductId);
        item.setQty(20);
        item.setCostPrice(25000);
        items.add(item);

        purchaseService.createPurchase(purchase, items);

        assertTrue(purchase.getId() > 0, "Phiếu nhập phải có ID sau khi lưu thành công");
        assertEquals(500000, purchase.getTotalCost(), "Tổng tiền nhập phải là 20 * 25.000 = 500.000");
        assertEquals(200000, purchase.getDebt(), "Còn nợ phải là 500.000 - 300.000 = 200.000");
        assertEquals("Còn nợ", purchase.getStatus());

        // Kiểm tra cập nhật tồn kho trong bảng products: 10 + 20 = 30
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement("SELECT stock_qty FROM products WHERE id = ?")) {
            stmt.setInt(1, testProductId);
            try (ResultSet rs = stmt.executeQuery()) {
                assertTrue(rs.next());
                assertEquals(30.0, rs.getDouble("stock_qty"), 0.001, "Tồn kho phải được tăng thêm 20 thành 30");
            }
        }

        // Kiểm tra ghi nhận trong bảng inventory_batches
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement("SELECT cost_price, qty_initial, qty_remaining FROM inventory_batches WHERE product_id = ?")) {
            stmt.setInt(1, testProductId);
            try (ResultSet rs = stmt.executeQuery()) {
                assertTrue(rs.next());
                assertEquals(25000, rs.getLong("cost_price"));
                assertEquals(20, rs.getInt("qty_initial"));
                assertEquals(20, rs.getInt("qty_remaining"));
            }
        }

        // Kiểm tra ghi nhận trong bảng stock_movements
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement("SELECT type, qty_change, stock_after, ref_type, ref_id FROM stock_movements WHERE product_id = ? AND ref_id = ?")) {
            stmt.setInt(1, testProductId);
            stmt.setInt(2, purchase.getId());
            try (ResultSet rs = stmt.executeQuery()) {
                assertTrue(rs.next(), "Phải có bản ghi biến động kho trong stock_movements");
                assertEquals("IN", rs.getString("type"));
                assertEquals(20.0, rs.getDouble("qty_change"), 0.001);
                assertEquals(30.0, rs.getDouble("stock_after"), 0.001);
                assertEquals("PURCHASE", rs.getString("ref_type"));
            }
        }

        // Kiểm tra truy vấn chi tiết các mặt hàng trong phiếu nhập
        List<PurchaseItem> savedItems = purchaseService.getItemsByPurchaseId(purchase.getId());
        assertNotNull(savedItems);
        assertEquals(1, savedItems.size());
        PurchaseItem savedItem = savedItems.get(0);
        assertEquals(testProductId, savedItem.getProductId());
        assertEquals("SP_TEST_PURCHASE", savedItem.getProductCode());
        assertEquals("Sản Phẩm Test Nhập", savedItem.getProductName());
        assertEquals("Cái Test Nhập", savedItem.getUnitName());
        assertEquals(20, savedItem.getQty());
        assertEquals(25000, savedItem.getCostPrice());
        assertEquals(500000, savedItem.getAmount());
    }

    @Test
    void testPayDebt_Success_PartialAndFull() throws SQLException {
        Purchase purchase = new Purchase();
        purchase.setCode("PN_PAY_001");
        purchase.setSupplierId(testSupplierId);
        purchase.setPurchaseDate("2026-09-10 10:00:00");
        purchase.setPaid(100000);

        List<PurchaseItem> items = new ArrayList<>();
        PurchaseItem item = new PurchaseItem();
        item.setProductId(testProductId);
        item.setQty(10);
        item.setCostPrice(20000); // Tổng 200.000, nợ 100.000
        items.add(item);

        purchaseService.createPurchase(purchase, items);
        assertEquals(100000, purchase.getDebt());
        assertEquals("Còn nợ", purchase.getStatus());

        // 1. Trả một phần 40.000
        Purchase updatedPartial = purchaseService.payDebt(purchase.getId(), 40000, "Trả nợ đợt 1");
        assertEquals(140000, updatedPartial.getPaid());
        assertEquals(60000, updatedPartial.getDebt());
        assertEquals("Còn nợ", updatedPartial.getStatus());

        // 2. Trả hết nợ còn lại 60.000
        Purchase updatedFull = purchaseService.payDebt(purchase.getId(), 60000, "Trả hết nợ còn lại");
        assertEquals(200000, updatedFull.getPaid());
        assertEquals(0, updatedFull.getDebt());
        assertEquals("Đã thanh toán", updatedFull.getStatus());

        // Kiểm tra bảng supplier_payments có ghi nhận các lần thanh toán
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement("SELECT COUNT(*) FROM supplier_payments WHERE purchase_id = ?")) {
            stmt.setInt(1, purchase.getId());
            try (ResultSet rs = stmt.executeQuery()) {
                assertTrue(rs.next());
                // 1 lần khi tạo phiếu (paid > 0) + 2 lần trả nợ = 3 lần
                assertEquals(3, rs.getInt(1));
            }
        }
    }

    @Test
    void testPayDebt_ValidationExceptions() throws SQLException {
        Purchase purchase = new Purchase();
        purchase.setCode("PN_PAY_002");
        purchase.setSupplierId(testSupplierId);
        purchase.setPurchaseDate("2026-09-10 10:00:00");
        purchase.setPaid(100000);

        List<PurchaseItem> items = new ArrayList<>();
        PurchaseItem item = new PurchaseItem();
        item.setProductId(testProductId);
        item.setQty(5);
        item.setCostPrice(20000); // Tổng 100.000, nợ 0
        items.add(item);

        purchaseService.createPurchase(purchase, items);
        assertEquals(0, purchase.getDebt());

        // Trả nợ khi không còn nợ
        assertThrows(IllegalArgumentException.class, () -> purchaseService.payDebt(purchase.getId(), 10000, null));

        // Trả số tiền <= 0
        assertThrows(IllegalArgumentException.class, () -> purchaseService.payDebt(purchase.getId(), 0, null));
        assertThrows(IllegalArgumentException.class, () -> purchaseService.payDebt(purchase.getId(), -5000, null));
    }
}
