package com.shop.service;

import com.shop.dao.ProductDao;
import com.shop.dao.StockAdjustmentDao;
import com.shop.infra.db.SchemaInitializer;
import com.shop.model.Product;
import com.shop.model.StockAdjustment;
import com.shop.model.StockAdjustmentItem;
import com.shop.model.StockMovement;
import com.shop.util.DBConnection;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class AdjustmentServiceTest {

    private AdjustmentService adjustmentService;
    private ProductDao productDao;
    private StockAdjustmentDao adjustmentDao;
    private int productId1;
    private int productId2;

    @BeforeEach
    void setUp() throws SQLException {
        System.setProperty("db.url", "jdbc:sqlite::memory:");
        SchemaInitializer.initialize();
        adjustmentService = new AdjustmentService();
        productDao = new ProductDao();
        adjustmentDao = new StockAdjustmentDao();

        try (Connection conn = DBConnection.getConnection()) {
            try (PreparedStatement del0 = conn.prepareStatement("DELETE FROM inventory_batches");
                 PreparedStatement del1 = conn.prepareStatement("DELETE FROM stock_movements");
                 PreparedStatement del2 = conn.prepareStatement("DELETE FROM stock_adjustment_items");
                 PreparedStatement del3 = conn.prepareStatement("DELETE FROM stock_adjustments");
                 PreparedStatement del4 = conn.prepareStatement("DELETE FROM products");
                 PreparedStatement del5 = conn.prepareStatement("DELETE FROM units")) {
                del0.executeUpdate();
                del1.executeUpdate();
                del2.executeUpdate();
                del3.executeUpdate();
                del4.executeUpdate();
                del5.executeUpdate();
            }

            int unitId;
            try (PreparedStatement pUnit = conn.prepareStatement("INSERT INTO units (name) VALUES ('Cái')", Statement.RETURN_GENERATED_KEYS)) {
                pUnit.executeUpdate();
                try (ResultSet rs = pUnit.getGeneratedKeys()) {
                    if (rs.next()) {
                        unitId = rs.getInt(1);
                    } else {
                        unitId = 1;
                    }
                }
            }

            Product p1 = new Product();
            p1.setCode("SP001");
            p1.setName("Sản phẩm 1");
            p1.setStockQty(10.0);
            p1.setSalePrice(50000L);
            p1.setUnitId(unitId);
            productId1 = productDao.insert(conn, p1);

            Product p2 = new Product();
            p2.setCode("SP002");
            p2.setName("Sản phẩm 2");
            p2.setStockQty(20.0);
            p2.setSalePrice(100000L);
            p2.setUnitId(unitId);
            productId2 = productDao.insert(conn, p2);
        }
    }

    @Test
    void testCancelAdjustment_RevertsStockAndMarksCanceled() throws SQLException {
        // 1. Tạo phiếu kiểm kho:
        // SP001: tồn hiện tại 10, kiểm tế 15 -> variance = +5 (dôi dư)
        // SP002: tồn hiện tại 20, kiểm tế 14 -> variance = -6 (hao hụt)
        StockAdjustment adj = new StockAdjustment();
        adj.setNote("Kiểm kho định kỳ");

        List<StockAdjustmentItem> items = new ArrayList<>();
        StockAdjustmentItem item1 = new StockAdjustmentItem();
        item1.setProductId(productId1);
        item1.setCurrentQty(10.0);
        item1.setActualQty(15.0);
        item1.setVariance(5.0);
        item1.setCostPrice(30000L);
        item1.setReason("Dôi dư thực tế");
        items.add(item1);

        StockAdjustmentItem item2 = new StockAdjustmentItem();
        item2.setProductId(productId2);
        item2.setCurrentQty(20.0);
        item2.setActualQty(14.0);
        item2.setVariance(-6.0);
        item2.setCostPrice(60000L);
        item2.setReason("Hư hỏng vứt bỏ");
        items.add(item2);

        adjustmentService.createAdjustment(adj, items);

        // Sau khi kiểm: tồn SP001 = 15, tồn SP002 = 14
        try (Connection conn = DBConnection.getConnection()) {
            assertEquals(15.0, productDao.getById(conn, productId1).getStockQty(), 0.001);
            assertEquals(14.0, productDao.getById(conn, productId2).getStockQty(), 0.001);
        }

        List<StockAdjustment> allAdjustments = adjustmentService.getAllAdjustments();
        assertFalse(allAdjustments.isEmpty());
        int adjId = allAdjustments.get(0).getId();
        assertEquals("COMPLETED", allAdjustments.get(0).getStatus());

        // 2. Thực hiện hủy phiếu kiểm kho
        adjustmentService.cancelAdjustment(adjId);

        // 3. Kiểm tra kết quả:
        // Tồn kho được hoàn nguyên về ban đầu: SP001 = 10, SP002 = 20
        try (Connection conn = DBConnection.getConnection()) {
            assertEquals(10.0, productDao.getById(conn, productId1).getStockQty(), 0.001);
            assertEquals(20.0, productDao.getById(conn, productId2).getStockQty(), 0.001);
        }

        StockAdjustment canceledAdj = adjustmentService.getAdjustmentById(adjId);
        assertNotNull(canceledAdj);
        assertEquals("CANCELED", canceledAdj.getStatus());
        assertNotNull(canceledAdj.getCanceledAt());

        // Không cho phép hủy lần 2
        assertThrows(IllegalStateException.class, () -> adjustmentService.cancelAdjustment(adjId));
    }
}
