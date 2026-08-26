# Spec: Hệ thống (Sao lưu, Khôi phục, Nhật ký)

## 1. Executive Summary
Module "Hệ thống" trong Giai đoạn 2 bổ sung các tính năng quan trọng để bảo vệ dữ liệu và tăng cường tính minh bạch, bao gồm: Sao lưu (Backup) cơ sở dữ liệu, Khôi phục (Restore) khi có sự cố, và Nhật ký hoạt động (Activity Log) để theo dõi các thao tác nhạy cảm của người dùng trên hệ thống.

## 2. Requirements & Use Cases
- **UC-01: Sao lưu dữ liệu (Backup)**
  - **Luồng chính**: Người dùng chọn thư mục đích để lưu file backup. Hệ thống copy file cơ sở dữ liệu `shop.db` hiện tại sang thư mục đó với tên dạng `shop_backup_YYYYMMDD_HHMMSS.db`.
  - **Luồng ngoại lệ**: Thư mục không có quyền ghi -> Hiển thị lỗi.

- **UC-02: Khôi phục dữ liệu (Restore)**
  - **Luồng chính**: Người dùng chọn file backup (`.db` hoặc `.bak`). Hệ thống đóng mọi kết nối DB, ghi đè file DB hiện tại bằng file backup. Sau khi hoàn thành, hệ thống thông báo yêu cầu người dùng khởi động lại ứng dụng.
  - **Luồng ngoại lệ**: File chọn không phải là SQLite hợp lệ -> Hiển thị lỗi, không ghi đè.

- **UC-03: Xem Nhật ký hoạt động (Activity Log)**
  - **Luồng chính**: Giao diện hiển thị bảng nhật ký (Ngày giờ, Hành động, Đối tượng, Chi tiết). Các thao tác như cập nhật giá, xoá sản phẩm, sửa hoá đơn sẽ tự động ghi 1 dòng log.

## 3. UI/UX & Navigation
- Bổ sung 2 tab vào màn hình "Thiết lập" (`settings-view.fxml`):
  1. **Tab Nhật ký (activity-log-tab.fxml)**: Chứa một TableView hiển thị log, sắp xếp mới nhất lên đầu.
  2. **Tab Sao lưu & Khôi phục (backup-restore-tab.fxml)**: Giao diện trực quan gồm 2 nút lớn "Sao lưu dữ liệu" và "Khôi phục dữ liệu".

## 4. Data Models & State
- Thêm bảng `activity_log` vào `schema.sql`:
  ```sql
  CREATE TABLE IF NOT EXISTS activity_log (
      id         INTEGER PRIMARY KEY AUTOINCREMENT,
      action     TEXT NOT NULL,
      entity     TEXT,
      entity_id  INTEGER,
      detail     TEXT,
      created_at TEXT NOT NULL
  ) STRICT;
  ```

## 5. Integration & Architecture
- **DBConnection**: Bổ sung phương thức `getDbPath()` để lấy đường dẫn file hiện tại phục vụ backup. Bổ sung phương thức để đóng kết nối và giải phóng file.
- **Service & DAO**: 
  - `ActivityLogDao`, `ActivityLogService` cho thao tác với log.
  - Sửa một số DAO hiện có (VD: ProductDao khi cập nhật/xoá) để tự động ghi log vào ActivityLogDao (tuỳ chọn hoặc làm thủ công trong Service).
  - `BackupRestoreService`: Xử lý I/O để copy file.
  
## 6. Edge Cases & Risks
- **Restore đang chạy mà bị ngắt**: Đề xuất copy file gốc ra tạm (vd: `shop.db.tmp`), nếu restore lỗi thì revert lại.
- **Lock DB**: Khi restore, phải chắc chắn toàn bộ HikariCP pool đã shutdown và không còn thread nào giữ lock file `shop.db`.
