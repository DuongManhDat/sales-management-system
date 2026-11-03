package com.shop.dao;

import com.shop.model.Product;
import com.shop.model.common.PageResult;
import com.shop.util.DBConnection;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.*;

public class ProductDaoPaginationTest {

    private final ProductDao productDao = new ProductDao();

    @BeforeEach
    public void setup() throws SQLException {
        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement()) {
            
            stmt.execute("INSERT OR IGNORE INTO units (id, name) VALUES (999, 'TestUnit')");
            stmt.execute("DELETE FROM products WHERE code LIKE 'TEST_PAGE_%'");
            for (int i = 1; i <= 25; i++) {
                stmt.execute(String.format(
                        "INSERT INTO products (code, name, unit_id, sale_price, stock_qty, created_at, updated_at) " +
                                "VALUES ('TEST_PAGE_%03d', 'Sản phẩm Test %d', 999, 10000, 10, datetime('now', 'localtime'), datetime('now', 'localtime'))",
                        i, i
                ));
            }
        }
    }

    @Test
    @DisplayName("Kiểm tra findPaged phân trang LIMIT OFFSET và COUNT chính xác")
    public void testFindPagedProducts() throws SQLException {
        PageResult<Product> page1 = productDao.findPaged("TEST_PAGE", "Tất cả", "Tất cả", 1, 15);
        assertEquals(1, page1.getPageIndex());
        assertEquals(15, page1.getPageSize());
        assertEquals(25, page1.getTotalItems());
        assertEquals(2, page1.getTotalPages());
        assertEquals(15, page1.getItems().size());

        PageResult<Product> page2 = productDao.findPaged("TEST_PAGE", "Tất cả", "Tất cả", 2, 15);
        assertEquals(2, page2.getPageIndex());
        assertEquals(10, page2.getItems().size());
    }
}
