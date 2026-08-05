package com.shop.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GlobalExceptionHandler implements Thread.UncaughtExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @Override
    public void uncaughtException(Thread t, Throwable e) {
        log.error("Lỗi không xác định trên thread: {}", t.getName(), e);
        DialogHelper.showError("Lỗi hệ thống", 
            "Đã xảy ra lỗi không xác định:\n" + e.getMessage() + "\nVui lòng xem file log để biết thêm chi tiết.");
    }
}
