package com.shop.util;

import org.mindrot.jbcrypt.BCrypt;

public class SecurityUtil {
    
    public static String hashPassword(String plainTextPassword) {
        if (plainTextPassword == null) return null;
        return BCrypt.hashpw(plainTextPassword, BCrypt.gensalt(12));
    }

    public static boolean checkPassword(String plainTextPassword, String hashedPassword) {
        if (plainTextPassword == null || hashedPassword == null) {
            return false;
        }
        if (!hashedPassword.startsWith("$2a$")) {
            return false;
        }
        try {
            return BCrypt.checkpw(plainTextPassword, hashedPassword);
        } catch (Exception e) {
            return false;
        }
    }
}
