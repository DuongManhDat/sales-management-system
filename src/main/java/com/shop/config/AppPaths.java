package com.shop.config;

import java.io.File;

public class AppPaths {
    private static final String APP_NAME = "ShopManager";
    
    public static String getAppDataDir() {
        String appData = System.getenv("APPDATA");
        if (appData == null || appData.isEmpty()) {
            // Fallback for non-Windows systems
            appData = System.getProperty("user.home");
        }
        
        File dir = new File(appData, APP_NAME);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        return dir.getAbsolutePath();
    }
    
    public static String getDbFilePath() {
        return getAppDataDir() + File.separator + "shop.db";
    }
}
