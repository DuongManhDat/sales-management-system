# Test Cases: Dashboard

> Module: TC-03 · Feature: Dashboard doanh thu  
> Spec tham chiếu: [spec.md](../specs/phase-1-mvp/dashboard/spec.md)  
> Ngày tạo: 2026-08-13

---

| Test ID | Module / Feature | Scenario Title | Pre-conditions | Test Steps | Expected Result | Priority | Type |
|---------|-----------------|----------------|----------------|------------|-----------------|----------|------|
| TC-03-001 | Dashboard | Mở Dashboard sau khi đăng nhập thành công | Đã đăng nhập; có dữ liệu hóa đơn hôm nay | 1. Đăng nhập thành công | Dashboard tự động load dữ liệu ngày hôm nay; hiển thị 4 KPI cards, 2 biểu đồ, Top 5 SP, danh sách sắp hết | P0 | Functional |
| TC-03-002 | Dashboard – KPI | Hiển thị đúng Tổng doanh thu ngày hôm nay | Có các hóa đơn COMPLETED trong ngày hôm nay | 1. Mở Dashboard<br>2. Quan sát KPI card "Doanh thu" | Hiển thị tổng SUM(total) từ invoices có status='COMPLETED' ngày hôm nay, format "12.500.000 đ" | P0 | Functional |
| TC-03-003 | Dashboard – KPI | Hiển thị đúng Lợi nhuận gộp | Có hóa đơn với invoice_items.cost_price được lưu | 1. Mở Dashboard<br>2. Quan sát KPI "Lợi nhuận" | Hiển thị SUM((sale_price - cost_price) × qty) cho các HĐ COMPLETED hôm nay | P0 | Functional |
| TC-03-004 | Dashboard – KPI | Hiển thị đúng Số đơn hàng | Có 5 hóa đơn COMPLETED hôm nay | 1. Mở Dashboard<br>2. Quan sát KPI "Số đơn" | Hiển thị "5" (COUNT hóa đơn COMPLETED) | P0 | Functional |
| TC-03-005 | Dashboard – KPI | Hiển thị số SP sắp hết với ngưỡng mặc định | Có SP với stock_qty ≤ 10 và ACTIVE | 1. Mở Dashboard<br>2. Quan sát KPI card "Sắp hết" | Hiển thị số lượng SP có stock_qty ≤ ngưỡng mặc định (≤ 10) | P0 | Functional |
| TC-03-006 | Dashboard – KPI | KPI hiển thị 0 khi không có hóa đơn nào trong ngày | Không có hóa đơn nào trong ngày được chọn | 1. Chọn ngày chưa có hóa đơn nào<br>2. Quan sát KPI cards | Tất cả KPI card (Doanh thu, Lợi nhuận, Số đơn) hiển thị giá trị 0; **không báo lỗi** | P0 | Edge Case |
| TC-03-007 | Dashboard – DatePicker | DatePicker mặc định là ngày hôm nay | Đã đăng nhập | 1. Mở Dashboard | DatePicker hiển thị ngày hôm nay | P0 | Functional |
| TC-03-008 | Dashboard – DatePicker | Chọn ngày khác → dữ liệu reload | Dashboard đang hiển thị ngày hôm nay | 1. Click DatePicker<br>2. Chọn ngày hôm qua<br>3. Quan sát dữ liệu | Dashboard reload và hiển thị dữ liệu của ngày hôm qua | P0 | Functional |
| TC-03-009 | Dashboard – DatePicker | Không cho chọn ngày trong tương lai | Đang ở Dashboard | 1. Click DatePicker<br>2. Thử chọn ngày mai | DatePicker không cho chọn ngày sau hôm nay (ngày tương lai bị disabled) | P0 | Boundary |
| TC-03-010 | Dashboard – Refresh | Nút Làm mới tải lại dữ liệu | Dashboard đang hiển thị | 1. Bấm nút [🔄 Làm mới] | Dữ liệu được tải lại cho ngày đang chọn; thấy loading indicator trong lúc tải | P0 | Functional |
| TC-03-011 | Dashboard – Refresh | Nút Làm mới bị disabled khi đang loading | Đang trong quá trình load data | 1. Click [Làm mới]<br>2. Ngay sau đó click [Làm mới] lần nữa | Nút [Làm mới] bị disabled trong lúc loading; tránh race condition | P1 | Edge Case |
| TC-03-012 | Dashboard – Loading | Hiển thị loading indicator khi đang tải | Dashboard đang load data | 1. Bấm [Làm mới] hoặc đổi ngày | ProgressIndicator hiển thị overlay lên vùng KPI cards + biểu đồ trong khi chờ | P1 | Functional |
| TC-03-013 | Dashboard – Biểu đồ giờ | Biểu đồ Bar Chart theo giờ (0-23h) hiển thị đúng | Có hóa đơn trong nhiều giờ khác nhau trong ngày | 1. Mở Dashboard<br>2. Quan sát biểu đồ "Doanh thu theo GIỜ" | Bar Chart hiển thị 24 cột (0h-23h); chiều cao cột = doanh thu tương ứng; hover tooltip hiện giá trị | P0 | Functional |
| TC-03-014 | Dashboard – Biểu đồ giờ | Biểu đồ giờ trống khi không có hóa đơn | Ngày được chọn không có hóa đơn | 1. Chọn ngày không có giao dịch | Biểu đồ hiển thị empty state (tất cả cột = 0 hoặc thông báo trống); không báo lỗi | P1 | Edge Case |
| TC-03-015 | Dashboard – Biểu đồ thứ | Biểu đồ Bar Chart theo thứ (T2-CN) hiển thị đúng | Có hóa đơn trong 7 ngày gần nhất | 1. Mở Dashboard<br>2. Quan sát biểu đồ "Doanh thu theo THỨ" | Bar Chart hiển thị 7 cột (T2-CN), dựa trên 7 ngày tính từ ngày đang chọn về trước | P0 | Functional |
| TC-03-016 | Dashboard – Top 5 SP | Top 5 sản phẩm bán chạy hiển thị đúng | Có nhiều hóa đơn với các SP khác nhau | 1. Mở Dashboard | Hiển thị 5 SP bán nhiều nhất theo số lượng; gồm tên, đơn vị, số lượng, doanh thu | P0 | Functional |
| TC-03-017 | Dashboard – Top 5 SP | Top 5 rỗng khi không có hóa đơn | Không có hóa đơn trong ngày được chọn | 1. Chọn ngày không có giao dịch | Danh sách Top 5 rỗng; không báo lỗi | P1 | Edge Case |
| TC-03-018 | Dashboard – Sắp hết | Thay đổi ngưỡng sắp hết → reload danh sách | Đang xem danh sách sắp hết với ngưỡng ≤ 10 | 1. Đổi ngưỡng từ "≤ 10" sang "≤ 20" trong ComboBox | Danh sách sắp hết cập nhật ngay (không cần bấm Refresh) với SP có stock_qty ≤ 20 | P0 | Functional |
| TC-03-019 | Dashboard – Sắp hết | Ba ngưỡng sắp hết có thể chọn | Đang ở Dashboard | 1. Click ComboBox ngưỡng | Hiển thị 3 tùy chọn: ≤ 5, ≤ 10, ≤ 20 | P1 | Functional |
| TC-03-020 | Dashboard – Sắp hết | Sản phẩm có stock_qty ≤ 0 xuất hiện trong danh sách sắp hết | SP có stock_qty = -2 (âm kho) | 1. Chọn ngưỡng ≤ 5<br>2. Quan sát danh sách sắp hết | SP có stock_qty = -2 xuất hiện trong danh sách; có icon ⚠️ | P1 | Edge Case |
| TC-03-021 | Dashboard – Sắp hết | Không có SP sắp hết → hiện thông báo | Tất cả SP có stock_qty > ngưỡng | 1. Chọn ngưỡng ≤ 5 khi tất cả SP tồn kho tốt | Hiển thị "Không có sản phẩm nào sắp hết hàng" | P1 | Edge Case |
| TC-03-022 | Dashboard – Format tiền | Giá trị tiền định dạng vi-VN | Có doanh thu 12500000 đồng | 1. Quan sát giá trị tiền trên KPI card | Hiển thị "12.500.000 đ" (dấu chấm ngăn cách nghìn, đơn vị đ) | P0 | Functional |
| TC-03-023 | Dashboard – Error | DB đọc bị lỗi → hiện thông báo lỗi | DB bị lock hoặc corrupt | 1. Simulate lỗi DB<br>2. Mở hoặc refresh Dashboard | Hiển thị errorMessage; KPI cards hiện "—"; ghi log ERROR | P1 | Negative |
| TC-03-024 | Dashboard – Performance | Load dữ liệu ≤ 2 giây với ~10.000 hóa đơn | DB có ~10.000 hóa đơn | 1. Mở Dashboard<br>2. Đo thời gian load | Dữ liệu hiển thị trong ≤ 2 giây | P1 | Boundary |
| TC-03-025 | Dashboard – Threading | Truy vấn DB chạy trên background thread | Đang ở Dashboard | 1. Bấm [Làm mới]<br>2. Thử tương tác UI (click menu khác) trong lúc loading | UI không bị freeze; có thể tương tác với sidebar trong khi Dashboard đang load | P0 | Functional |
| TC-03-026 | Dashboard – cost_price | Lợi nhuận tính đúng khi cost_price = 0 (dữ liệu cũ) | Có invoice_items với cost_price = 0 (dữ liệu trước migration) | 1. Mở Dashboard<br>2. Quan sát Lợi nhuận | Lợi nhuận của HĐ đó = doanh thu (cost = 0); không báo lỗi | P2 | Edge Case |
| TC-03-027 | Dashboard – Load timeout | Timeout sau 30 giây nếu load quá chậm | DB query quá chậm | 1. Simulate query chậm > 30s | Sau 30 giây: hiện thông báo timeout/lỗi; nút Refresh được enable lại | P2 | Edge Case |
