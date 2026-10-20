# Task 3 Brief: Nghiệp Vụ Hủy Phiếu Kiểm Kho (AdjustmentService & Unit Tests)

## Files
- Modify: `src/main/java/com/shop/service/AdjustmentService.java`
- Create: `src/test/java/com/shop/service/AdjustmentServiceTest.java`

## Requirements
1. Trong `AdjustmentService.java`, thêm các phương thức:
   - `public StockAdjustment getAdjustmentById(int id) throws SQLException`
   - `public void cancelAdjustment(int adjustmentId) throws SQLException`
   Chi tiết nghiệp vụ `cancelAdjustment`:
   - Mở Transaction (`conn.setAutoCommit(false)`).
   - Kiểm tra phiếu kiểm kho: nếu `null` ném `IllegalArgumentException`.
   - Nếu `CANCELED` ném `IllegalStateException("Phiếu kiểm kho này đã bị hủy trước đó.")`.
   - Lấy thời gian hiện tại: `DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")`.
   - Gọi `adjustmentDao.updateStatus(conn, adjustmentId, "CANCELED", currentTime)`.
   - Lấy danh sách item qua `adjustmentDao.findItemsByAdjustmentId(adjustmentId)`.
   - Duyệt qua từng item có `Math.abs(item.getVariance()) >= 0.0001`:
     - Hoàn nguyên tồn kho: `productDao.decreaseStock(conn, item.getProductId(), item.getVariance())` (Dôi dư variance > 0 thì trừ tồn kho; Hao hụt variance < 0 thì bù lại tồn kho).
     - Ghi nhận `StockMovement`:
       - `type = "ADJUSTMENT_CANCEL"`
       - `qtyChange = -item.getVariance()`
       - `refType = "ADJUSTMENT_CANCEL"`
       - `refId = adjustmentId`
       - `createdAt = currentTime`
       - `note = "Hủy phiếu kiểm kho " + adj.getCode()`
     - Xóa các bản ghi `inventory_batches` phát sinh: `DELETE FROM inventory_batches WHERE stock_adjustment_item_id = ?`.
   - Commit transaction.
2. Viết test `AdjustmentServiceTest.java` theo TDD:
   - Setup memory database với `SchemaInitializer.initialize()`.
   - Test tạo phiếu kiểm kho và hủy phiếu kiểm kho.
   - Assert tồn kho sản phẩm được hoàn nguyên đúng số lượng.
   - Assert trạng thái phiếu là `CANCELED`, `canceled_at` không null.
   - Assert ném `IllegalStateException` khi cố tình hủy lần 2.
3. Chạy `mvn test -Dtest=AdjustmentServiceTest` pass 100%.
