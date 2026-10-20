# Task 1 Report

## Status
DONE

## Implemented
- Tạo migration `src/main/resources/db/migration/V9__StockAdjustment_Cancel_Schema.sql` thêm 2 cột `status` (mặc định `'COMPLETED'`) và `canceled_at` vào bảng `stock_adjustments`.
- Đăng ký thực thi migration V9 trong `src/main/java/com/shop/infra/db/SchemaInitializer.java`.
- Bổ sung `status` và `canceledAt` kèm getter/setter trong `src/main/java/com/shop/model/StockAdjustment.java`.

## Verification
- Chạy `mvn test-compile`: BUILD SUCCESS, không có lỗi cú pháp hoặc xung đột.
