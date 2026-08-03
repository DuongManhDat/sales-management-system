package com.shop.util;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SecurityUtilTest {

    @Test
    void testHashAndCheckPassword() {
        String plainText = "mySecurePassword123";
        String hashed = SecurityUtil.hashPassword(plainText);
        
        assertNotNull(hashed);
        assertTrue(hashed.startsWith("$2a$"));
        
        // Match
        assertTrue(SecurityUtil.checkPassword(plainText, hashed));
        
        // Mismatch
        assertFalse(SecurityUtil.checkPassword("wrongPassword", hashed));
        
        // Null inputs
        assertFalse(SecurityUtil.checkPassword(null, hashed));
        assertFalse(SecurityUtil.checkPassword(plainText, null));
    }
}
