# Task 1 Brief: Database Migration V9 & Model Updates

## Files
- Create: `src/main/resources/db/migration/V9__StockAdjustment_Cancel_Schema.sql`
- Modify: `src/main/java/com/shop/infra/db/SchemaInitializer.java`
- Modify: `src/main/java/com/shop/model/StockAdjustment.java`

## Requirements
1. Viết migration SQL `src/main/resources/db/migration/V9__StockAdjustment_Cancel_Schema.sql`:
```sql
-- V9: Bổ sung trạng thái và thời gian hủy phiếu kiểm kho
ALTER TABLE stock_adjustments ADD COLUMN status TEXT DEFAULT 'COMPLETED';
ALTER TABLE stock_adjustments ADD COLUMN canceled_at TEXT;
```

2. Cập nhật `src/main/java/com/shop/infra/db/SchemaInitializer.java`:
Thực thi migration V9 ngay sau V8:
```java
executeMigration(conn, stmt, appliedVersions, "V9", "/db/migration/V9__StockAdjustment_Cancel_Schema.sql");
```

3. Cập nhật Model `src/main/java/com/shop/model/StockAdjustment.java`:
Bổ sung 2 trường `status` và `canceledAt` cùng đầy đủ getter/setter (đảm bảo tuân thủ full-output-enforcement):
- `public String getStatus() { return status; }`
- `public void setStatus(String status) { this.status = status; }`
- `public String getCanceledAt() { return canceledAt; }`
- `public void setCanceledAt(String canceledAt) { this.canceledAt = canceledAt; }`

4. Biên dịch và kiểm tra: `mvn test-compile`
Lưu ý quy ước: Không commit code khi chưa có sự đồng ý của user.
