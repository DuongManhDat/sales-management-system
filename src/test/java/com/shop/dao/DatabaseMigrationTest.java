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
}
