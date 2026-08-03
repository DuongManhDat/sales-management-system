package com.shop.service;

import com.shop.dao.AppUserDao;
import com.shop.model.AppUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;

class AuthServiceTest {
    static class StubDao extends AppUserDao {
        AppUser stored = null;
        @Override
        public Optional<AppUser> findFirst() {
            return Optional.ofNullable(stored);
        }
        @Override
        public void insert(AppUser user) {
            this.stored = user;
        }
    }

    private AuthService service;
    private StubDao dao;

    @BeforeEach
    void setUp() {
        dao = new StubDao();
        service = new AuthService(dao);
    }

    @Test
    void testSetupAndLogin() {
        // Initially empty
        assertFalse(service.login("admin"));
        
        // Setup password
        service.setup("mypassword");
        assertNotNull(dao.stored);
        
        // Login fails with wrong password
        assertFalse(service.login("wrong"));
        
        // Login success
        assertTrue(service.login("mypassword"));
    }
}
