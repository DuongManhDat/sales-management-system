# Task 2 Brief: Data Access Layer (StockAdjustmentDao)

## Files
- Modify: `src/main/java/com/shop/dao/StockAdjustmentDao.java`

## Requirements
1. Cập nhật `insertAdjustment`:
Chèn thêm cột `status`:
```sql
INSERT INTO stock_adjustments (code, adjustment_date, note, status, created_at) VALUES (?, date('now', 'localtime'), ?, 'COMPLETED', datetime('now', 'localtime'))
```
2. Cập nhật `findAll()` và `filterAdjustments()`:
Map 2 trường `status` và `canceled_at`:
```java
adj.setStatus(rs.getString("status"));
adj.setCanceledAt(rs.getString("canceled_at"));
```
3. Bổ sung `findById(int id)`:
```java
public StockAdjustment findById(int id) throws SQLException
```
Tìm phiếu theo id và map đầy đủ các trường (id, code, adjustment_date, note, created_at, status, canceled_at).
4. Bổ sung `updateStatus(Connection conn, int id, String status, String canceledAt)`:
```java
public void updateStatus(Connection conn, int id, String status, String canceledAt) throws SQLException
```
Thực thi:
```sql
UPDATE stock_adjustments SET status = ?, canceled_at = ? WHERE id = ?
```
5. Đảm bảo tuân thủ nguyên tắc `full-output-enforcement`.
