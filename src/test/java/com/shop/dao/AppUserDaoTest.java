package com.shop.dao;

import com.shop.infra.db.SchemaInitializer;
import com.shop.model.AppUser;
import com.shop.util.DBConnection;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.Statement;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class AppUserDaoTest {
    private AppUserDao dao;

    @BeforeEach
    void setUp() throws Exception {
        System.setProperty("db.url", "jdbc:sqlite::memory:");
        SchemaInitializer.initialize();
        dao = new AppUserDao();
    }

    @AfterEach
    void tearDown() throws Exception {
        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute("DROP TABLE IF EXISTS app_user");
        }
        DBConnection.close();
    }

    @Test
    void testInsertAndFindFirst() {
        Optional<AppUser> empty = dao.findFirst();
        assertTrue(empty.isEmpty());

        AppUser user = new AppUser();
        user.setUsername("owner");
        user.setPasswordHash("hashed123");
        user.setCreatedAt("2026-08-03T10:00:00Z");

        dao.insert(user);

        Optional<AppUser> found = dao.findFirst();
        assertTrue(found.isPresent());
        assertEquals("owner", found.get().getUsername());
        assertEquals("hashed123", found.get().getPasswordHash());
    }
}
