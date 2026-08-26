# Spec: Báo cáo nâng cao

## 1. Executive Summary
Module "Báo cáo nâng cao" cung cấp các công cụ phân tích số liệu kinh doanh, giúp chủ cửa hàng nắm bắt được tình hình doanh thu, lợi nhuận, xu hướng bán hàng và tình trạng tồn kho một cách trực quan, chính xác thông qua các bảng dữ liệu và biểu đồ.

## 2. Requirements & Use Cases
- **UC-01: Báo cáo Doanh thu & Lợi nhuận**
  - **Luồng chính**: Người dùng chọn khoảng thời gian (Từ ngày - Đến ngày). Hệ thống tính toán và hiển thị tổng doanh thu, tổng giá vốn, lợi nhuận gộp.
  - Hiển thị biểu đồ cột (Bar Chart) sự biến động doanh thu theo ngày.
- **UC-02: Báo cáo Bán hàng theo Sản phẩm**
  - **Luồng chính**: Thống kê số lượng bán, doanh thu, lợi nhuận của từng sản phẩm trong khoảng thời gian đã chọn.
  - Sắp xếp top sản phẩm bán chạy nhất.
  - Hiển thị biểu đồ tròn (Pie Chart) cơ cấu doanh thu.
- **UC-03: Báo cáo Xuất - Nhập - Tồn**
  - **Luồng chính**: Chọn thời gian. Hệ thống hiển thị tồn đầu kỳ, tổng lượng nhập, tổng lượng xuất, và tồn cuối kỳ của từng sản phẩm.

## 3. UI/UX & Navigation
- Thêm menu "Báo cáo" ở Navigation bar (Sidebar) dẫn đến màn hình Báo cáo (`report-view.fxml`).
- Sử dụng **TabPane** để gom nhóm các báo cáo: Doanh thu, Sản phẩm, Tồn kho.
- Tại mỗi Tab, cung cấp các bộ lọc (DatePicker: Từ ngày, Đến ngày) và nút "Xem báo cáo".
- Thiết kế kết hợp giữa Biểu đồ (Charts) ở phía trên và Bảng dữ liệu (TableView) chi tiết ở phía dưới. Đảm bảo giao diện hiện đại, phẳng, theo chuẩn `industrial-brutalist-ui`.

## 4. Data Models & State
- Database không cần bảng mới. Truy vấn dựa trên các bảng sẵn có: `Invoice`, `InvoiceItem`, `Product`, `StockMovement`, v.v.
- Các lớp Data Transfer Object (DTO) được tạo thêm để phục vụ View:
  - `RevenueReportRow` (ngày, doanh thu, giá vốn, lợi nhuận)
  - `ProductReportRow` (mã SP, tên SP, số lượng bán, doanh thu)
  - `InventoryReportRow` (mã SP, tên SP, tồn đầu, nhập, xuất, tồn cuối)

## 5. Integration & Architecture
- **DAO Layer**: `ReportDAO.java` chứa các câu lệnh SQL tổng hợp (GROUP BY, SUM, JOIN).
- **Service Layer**: `ReportService.java`.
- **Controller Layer**: `ReportController.java`.
- **View Layer**: `report-view.fxml`.

## 6. Edge Cases & Risks
- **Hiệu năng**: Các query tổng hợp lượng dữ liệu lớn có thể chậm. Cần tối ưu bằng index (nếu cần thiết) hoặc giới hạn khoảng thời gian truy vấn mặc định.
- **Trả hàng**: Cần đảm bảo báo cáo doanh thu đã được cấn trừ chính xác các phiếu trả hàng (`ReturnInvoice`).
