package com.shop.dao;

import org.junit.jupiter.api.Test;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import static org.junit.jupiter.api.Assertions.*;

import com.shop.util.DBConnection;
import com.shop.infra.db.SchemaInitializer;

public class DatabaseMigrationTest {
    @Test
    public void testMuaHangTablesExist() throws Exception {
        SchemaInitializer.initialize();
        
        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement()) {
            
            ResultSet rs = stmt.executeQuery("SELECT name FROM sqlite_master WHERE type='table' AND name='inventory_batches'");
            assertTrue(rs.next(), "Table inventory_batches should exist");
            
            ResultSet rs2 = stmt.executeQuery("SELECT name FROM sqlite_master WHERE type='table' AND name='suppliers'");
            assertTrue(rs2.next(), "Table suppliers should exist");
        }
    }

    @Test
    public void testDataNotLostOnReinitialization() throws Exception {
        SchemaInitializer.initialize();

        // 1. Tạo 1 đơn vị tính và 1 sản phẩm
        int unitId;
        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute("INSERT INTO units (name) VALUES ('Cai_Test_Restart')");
            try (ResultSet rs = stmt.executeQuery("SELECT last_insert_rowid()")) {
                assertTrue(rs.next());
                unitId = rs.getInt(1);
            }

            stmt.execute("INSERT INTO products (code, name, unit_id, sale_price, stock_qty, created_at, updated_at) " +
                    "VALUES ('HH_RESTART_01', 'San pham kiem tra restart', " + unitId + ", 50000, 10, datetime('now'), datetime('now'))");
        }

        // 2. Khởi tạo lại SchemaInitializer (mô phỏng người dùng tắt đi bật lại phần mềm)
        SchemaInitializer.initialize();

        // 3. Kiểm tra sản phẩm và đơn vị tính có còn nguyên vẹn không
        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement()) {
            try (ResultSet rs = stmt.executeQuery("SELECT name, stock_qty FROM products WHERE code = 'HH_RESTART_01'")) {
                assertTrue(rs.next(), "Dữ liệu hàng hóa không được phép bị mất khi khởi động lại ứng dụng!");
                assertEquals("San pham kiem tra restart", rs.getString("name"));
                assertEquals(10.0, rs.getDouble("stock_qty"));
            }
        }
    }
}
