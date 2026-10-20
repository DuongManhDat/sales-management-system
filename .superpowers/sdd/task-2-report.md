# Task 2 Report

## Status
DONE

## Implemented
- Cập nhật `StockAdjustmentDao.insertAdjustment` lưu `status = 'COMPLETED'`.
- Bổ sung `StockAdjustmentDao.updateStatus(Connection conn, int id, String status, String canceledAt)` cập nhật trạng thái và thời gian hủy phiếu.
- Bổ sung `StockAdjustmentDao.findById(int id)` truy vấn phiếu theo ID và map đầy đủ `status`, `canceled_at`.
- Cập nhật `StockAdjustmentDao.findAll()` và `filterAdjustments()` để map đầy đủ `status`, `canceled_at`.

## Verification
- Chạy `mvn test-compile`: BUILD SUCCESS.
