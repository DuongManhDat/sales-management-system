package com.shop.util;

import java.util.prefs.Preferences;

public class AppConfig {
    
    private static final Preferences prefs = Preferences.userNodeForPackage(AppConfig.class);
    
    private static final String KEY_REMEMBER_ME = "REMEMBER_ME";
    private static final String KEY_LAST_USERNAME = "LAST_USERNAME";
    
    public static void setRememberMe(boolean remember) {
        prefs.putBoolean(KEY_REMEMBER_ME, remember);
    }
    
    public static boolean isRememberMe() {
        return prefs.getBoolean(KEY_REMEMBER_ME, false);
    }
    
    public static void setLastUsername(String username) {
        prefs.put(KEY_LAST_USERNAME, username);
    }
    
    public static String getLastUsername() {
        return prefs.get(KEY_LAST_USERNAME, "");
    }
    
}
