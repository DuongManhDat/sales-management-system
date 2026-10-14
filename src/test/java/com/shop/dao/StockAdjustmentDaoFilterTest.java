package com.shop.dao;

import com.shop.infra.db.SchemaInitializer;
import com.shop.model.StockAdjustment;
import com.shop.util.DBConnection;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class StockAdjustmentDaoFilterTest {

    private StockAdjustmentDao stockAdjustmentDao;

    @BeforeEach
    void setUp() throws SQLException {
        System.setProperty("db.url", "jdbc:sqlite::memory:");
        SchemaInitializer.initialize();

        stockAdjustmentDao = new StockAdjustmentDao();

        try (Connection conn = DBConnection.getConnection()) {
            try (PreparedStatement del1 = conn.prepareStatement("DELETE FROM stock_adjustment_items");
                 PreparedStatement del2 = conn.prepareStatement("DELETE FROM stock_adjustments")) {
                del1.executeUpdate();
                del2.executeUpdate();
            }

            String insertSql = "INSERT INTO stock_adjustments (code, adjustment_date, note, created_at) VALUES (?, ?, ?, ?)";
            try (PreparedStatement pstmt = conn.prepareStatement(insertSql)) {
                pstmt.setString(1, "DC000001");
                pstmt.setString(2, "2026-10-10");
                pstmt.setString(3, "Ghi chú 1");
                pstmt.setString(4, "2026-10-10 09:00:00");
                pstmt.executeUpdate();

                pstmt.setString(1, "DC000002");
                pstmt.setString(2, "2026-10-12");
                pstmt.setString(3, "Ghi chú 2");
                pstmt.setString(4, "2026-10-12 14:30:00");
                pstmt.executeUpdate();

                pstmt.setString(1, "DC000003");
                pstmt.setString(2, "2026-10-12");
                pstmt.setString(3, "Ghi chú 3");
                pstmt.setString(4, "2026-10-12 18:00:00");
                pstmt.executeUpdate();
            }
        }
    }

    @Test
    void testFilterByCode() throws SQLException {
        List<StockAdjustment> results = stockAdjustmentDao.filterAdjustments("000002", null);
        assertEquals(1, results.size());
        assertEquals("DC000002", results.get(0).getCode());
    }

    @Test
    void testFilterByDate() throws SQLException {
        List<StockAdjustment> results = stockAdjustmentDao.filterAdjustments(null, "2026-10-12");
        assertEquals(2, results.size());
    }

    @Test
    void testFilterByCodeAndDate() throws SQLException {
        List<StockAdjustment> results = stockAdjustmentDao.filterAdjustments("000003", "2026-10-12");
        assertEquals(1, results.size());
        assertEquals("DC000003", results.get(0).getCode());
    }

    @Test
    void testFilterAllWhenEmpty() throws SQLException {
        List<StockAdjustment> results = stockAdjustmentDao.filterAdjustments("", null);
        assertEquals(3, results.size());
    }
}
