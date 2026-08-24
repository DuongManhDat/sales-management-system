# Test Cases: Hệ thống (Cài đặt & Cross-cutting)

> Module: TC-08 · Feature: Hệ thống – Setup lần đầu, Cài đặt, Log lỗi, Lịch sử tồn kho  
> Spec tham chiếu: [spec.md](../specs/phase-1-mvp/he-thong/spec.md)  
> Ngày tạo: 2026-08-13

---

| Test ID | Module / Feature | Scenario Title | Pre-conditions | Test Steps | Expected Result | Priority | Type |
|---------|-----------------|----------------|----------------|------------|-----------------|----------|------|
| TC-08-001 | Setup lần đầu | Hiển thị màn hình Setup khi shop_name chưa có | settings.shop_name rỗng hoặc NULL | 1. Khởi động app lần đầu | Điều hướng tới màn hình Setup (không phải Login) | P0 | Functional |
| TC-08-002 | Setup lần đầu | Form Setup thu thập đúng thông tin | Màn hình Setup đang hiển thị | 1. Quan sát màn hình Setup | Có các trường: Tên cửa hàng (*), Địa chỉ (*), SĐT (*), Email, MST, Logo | P0 | Functional |
| TC-08-003 | Setup lần đầu | Lưu Setup thành công → chuyển về Login | Đã điền đủ các trường bắt buộc | 1. Điền Tên, Địa chỉ, SĐT<br>2. Bấm [Bắt đầu sử dụng] | Lưu vào settings DB → điều hướng tới form Login | P0 | Functional |
| TC-08-004 | Setup lần đầu | Chặn lưu khi thiếu trường bắt buộc | Đang ở màn hình Setup | 1. Để trống Tên cửa hàng<br>2. Bấm [Bắt đầu sử dụng] | Highlight đỏ + không cho lưu | P0 | Negative |
| TC-08-005 | Setup lần đầu | Chặn lưu khi thiếu Địa chỉ | Đang ở màn hình Setup | 1. Để trống Địa chỉ<br>2. Bấm [Bắt đầu sử dụng] | Highlight đỏ + không cho lưu | P0 | Negative |
| TC-08-006 | Setup lần đầu | Chặn lưu khi thiếu Số điện thoại | Đang ở màn hình Setup | 1. Để trống SĐT<br>2. Bấm [Bắt đầu sử dụng] | Highlight đỏ + không cho lưu | P0 | Negative |
| TC-08-007 | Setup lần đầu | Có thể bỏ trống Email (tùy chọn) | Đang ở màn hình Setup | 1. Điền đủ 3 trường bắt buộc<br>2. Để trống Email<br>3. Lưu | Lưu thành công | P1 | Functional |
| TC-08-008 | Setup lần đầu | Có thể bỏ trống MST và Logo (tùy chọn) | Đang ở màn hình Setup | 1. Điền đủ 3 trường bắt buộc<br>2. Để trống MST và không chọn logo<br>3. Lưu | Lưu thành công | P1 | Functional |
| TC-08-009 | Setup lần đầu | Upload Logo PNG/JPG và xem trước | Đang ở màn hình Setup | 1. Click [📁 Chọn ảnh...]<br>2. Chọn file PNG hợp lệ | Ảnh preview hiển thị ngay dưới nút chọn | P1 | Functional |
| TC-08-010 | Setup lần đầu | Cảnh báo khi Logo > 200KB | Đang ở màn hình Setup | 1. Chọn ảnh > 200KB | Hiển thị cảnh báo (nhưng vẫn cho lưu nếu người dùng xác nhận) | P2 | Edge Case |
| TC-08-011 | Setup lần đầu | App không hiện Setup lần 2 sau khi đã thiết lập | shop_name đã có trong settings | 1. Khởi động app lần 2 | Không hiển thị màn hình Setup; điều hướng thẳng vào Login | P0 | Functional |
| TC-08-012 | Cài đặt – Thông tin CH | Xem và sửa thông tin cửa hàng | shop_name đã được thiết lập | 1. Mở Cài đặt → tab Thông tin cửa hàng | Hiển thị đúng thông tin đã lưu; có thể sửa tất cả trường | P0 | Functional |
| TC-08-013 | Cài đặt – Thông tin CH | Nút [Lưu] disabled khi không có thay đổi | Đang ở tab Thông tin cửa hàng | 1. Không thay đổi gì | Nút [Lưu] bị disabled | P1 | Functional |
| TC-08-014 | Cài đặt – Thông tin CH | Nút [Lưu] enabled sau khi chỉnh sửa | Đang ở tab Thông tin cửa hàng | 1. Thay đổi Tên cửa hàng<br>2. Quan sát nút [Lưu] | Nút [Lưu] được enable | P1 | Functional |
| TC-08-015 | Cài đặt – Thông tin CH | Sau khi lưu — Title bar và Sidebar cập nhật | Đã sửa Tên cửa hàng thành "Cửa hàng XYZ" | 1. Lưu thay đổi tên cửa hàng | Title bar hiển thị "Cửa hàng XYZ — Quản lý Bán hàng"; Sidebar header cập nhật | P0 | Functional |
| TC-08-016 | Cài đặt – Thông tin CH | Chặn lưu khi shop_name trống | Đang ở tab Thông tin cửa hàng | 1. Xóa trống Tên cửa hàng<br>2. Bấm [Lưu] | Validation bắt buộc; không cho lưu nếu shop_name rỗng | P0 | Negative |
| TC-08-017 | Cài đặt – Đổi MK | Đổi mật khẩu thành công | Đã đăng nhập | 1. Cài đặt → Bấm [Đổi mật khẩu]<br>2. Nhập MK cũ đúng<br>3. Nhập MK mới và xác nhận (≥ 4 ký tự)<br>4. Bấm [Lưu] | Mật khẩu đổi thành công; lần sau đăng nhập phải dùng MK mới | P0 | Functional |
| TC-08-018 | Cài đặt – Đổi MK | Mật khẩu cũ sai bị từ chối | Đang trong dialog đổi MK | 1. Nhập MK cũ sai<br>2. Bấm [Lưu] | Báo lỗi: "Mật khẩu hiện tại không đúng" | P0 | Negative |
| TC-08-019 | Cài đặt – Đổi MK | MK mới < 4 ký tự bị từ chối | Đang trong dialog đổi MK | 1. Nhập MK mới = "abc" (3 ký tự)<br>2. Bấm [Lưu] | Báo lỗi: "Mật khẩu tối thiểu 4 ký tự" | P0 | Boundary |
| TC-08-020 | Cài đặt – Đổi MK | MK mới = 4 ký tự được chấp nhận | Đang trong dialog đổi MK | 1. Nhập MK mới = "abcd" (4 ký tự)<br>2. Xác nhận | Lưu thành công | P1 | Boundary |
| TC-08-021 | Cài đặt – Đổi MK | MK mới không khớp xác nhận | Đang trong dialog đổi MK | 1. Nhập MK mới = "pass1"<br>2. Xác nhận = "pass2"<br>3. Bấm [Lưu] | Báo lỗi "MK mới không khớp" | P0 | Negative |
| TC-08-022 | Cài đặt – Đổi MK | Dialog đổi MK có toggle hiện/ẩn mật khẩu | Dialog đổi MK đang mở | 1. Bấm icon [👁️] cạnh ô MK | Mật khẩu hiển thị rõ | P1 | Functional |
| TC-08-023 | Log lỗi – Xem | Xem tab Nhật ký lỗi | Có file app.log với vài dòng log | 1. Cài đặt → tab Nhật ký lỗi | Hiển thị danh sách dòng log theo thứ tự mới nhất trước; tối đa 500 dòng | P0 | Functional |
| TC-08-024 | Log lỗi – Xem | Màu hiển thị đúng theo Level | Log có dòng ERROR, WARN, INFO | 1. Xem tab Nhật ký lỗi | ERROR bôi đỏ; WARN bôi cam; INFO màu mặc định | P1 | Functional |
| TC-08-025 | Log lỗi – Refresh | Nút [🔄 Làm mới] tải lại log | Đang xem tab Nhật ký lỗi | 1. Bấm [Làm mới] | Danh sách log được tải lại; có thể thấy dòng log mới nhất | P1 | Functional |
| TC-08-026 | Log lỗi – Edge Case | File app.log không tồn tại | Chưa có lỗi nào | 1. Mở tab Nhật ký lỗi | Hiển thị "Chưa có nhật ký lỗi nào" — không báo lỗi | P1 | Edge Case |
| TC-08-027 | Log lỗi – Threading | Đọc log chạy trên background thread | Đang mở tab Nhật ký lỗi | 1. Mở tab Nhật ký lỗi<br>2. Thử tương tác UI trong lúc đọc | UI không bị freeze | P1 | Functional |
| TC-08-028 | Log lỗi – Rolling | Log file rolling theo ngày | Để qua ngày mới | 1. Kiểm tra folder logs sau khi qua ngày | File app.log chỉ chứa log ngày hôm nay; file ngày trước được đổi tên với ngày | P2 | Functional |
| TC-08-029 | Lịch sử kho – Xem | Xem tab Lịch sử tồn kho | Có bản ghi trong stock_movements | 1. Cài đặt → tab Lịch sử tồn kho | Bảng hiển thị: Thời gian, Sản phẩm, Loại, Thay đổi, Tồn sau | P0 | Functional |
| TC-08-030 | Lịch sử kho – Lọc ngày | Lọc theo khoảng ngày | Có stock_movements nhiều ngày | 1. Chọn Từ ngày và Đến ngày<br>2. Click [Lọc] | Chỉ hiển thị bản ghi trong khoảng ngày được chọn | P0 | Functional |
| TC-08-031 | Lịch sử kho – Lọc loại | Lọc theo Loại = NHAP | Có cả NHAP và BAN trong stock_movements | 1. Chọn Loại = "NHAP"<br>2. Click [Lọc] | Chỉ hiển thị bản ghi type = 'NHAP' | P0 | Functional |
| TC-08-032 | Lịch sử kho – Lọc loại | Lọc theo Loại = BAN | Có cả NHAP và BAN | 1. Chọn Loại = "BAN"<br>2. Click [Lọc] | Chỉ hiển thị bản ghi type = 'BAN' | P0 | Functional |
| TC-08-033 | Lịch sử kho – Lọc loại | Lọc "Tất cả" hiển thị đầy đủ | Đang lọc theo loại | 1. Chọn Loại = "Tất cả"<br>2. Click [Lọc] | Hiển thị tất cả bản ghi stock_movements | P1 | Functional |
| TC-08-034 | Lịch sử kho – Hiển thị | Dòng có stock_after ≤ 0 bôi vàng + icon ⚠️ | Có bản ghi stock_after = 0 hoặc âm | 1. Xem tab Lịch sử tồn kho | Dòng stock_after ≤ 0 được bôi vàng và hiện icon ⚠️ | P1 | Functional |
| TC-08-035 | Lịch sử kho – Edge Case | stock_movements rỗng → thông báo | Chưa có giao dịch nào | 1. Mở tab Lịch sử tồn kho | Hiển thị "Chưa có biến động tồn kho nào" | P1 | Edge Case |
| TC-08-036 | Lịch sử kho – Giới hạn | Giới hạn 1.000 dòng gần nhất | stock_movements có > 1.000 bản ghi | 1. Lọc và xem kết quả | Hiển thị 1.000 bản ghi gần nhất + thông báo "Hiển thị 1.000/X bản ghi gần nhất" | P2 | Boundary |
| TC-08-037 | Lịch sử kho – Threading | Lọc stock_movements chạy trên background | Đang lọc với nhiều dữ liệu | 1. Bấm [Lọc]<br>2. Thử tương tác UI | UI không bị freeze | P1 | Functional |
| TC-08-038 | stock_movements – Ghi log | Bán hàng → ghi stock_movements trong cùng transaction | Thực hiện giao dịch bán hàng | 1. Bán 3 lon Coca<br>2. Kiểm tra stock_movements | Có bản ghi type='BAN', qty_change=-3, ref_type='INVOICE', ref_id=invoice_id | P0 | Functional |
| TC-08-039 | stock_movements – Ghi log | Nhập hàng → ghi stock_movements trong cùng transaction | Thực hiện phiếu nhập hàng | 1. Nhập 20 kg gạo<br>2. Kiểm tra stock_movements | Có bản ghi type='NHAP', qty_change=+20, ref_type='PURCHASE', ref_id=purchase_id | P0 | Functional |
| TC-08-040 | stock_movements – Nhất quán | Transaction rollback → không có bản ghi stock_movements | Simulate lỗi transaction khi bán hàng | 1. Bán hàng<br>2. Simulate lỗi trước COMMIT | ROLLBACK; không có bản ghi mới trong stock_movements; stock_qty không thay đổi | P0 | Negative |
| TC-08-041 | Logback | Log lỗi tự ghi ra file app.log | Trigger một lỗi trong app (vd: load Dashboard lỗi) | 1. Gây lỗi có chủ ý<br>2. Mở file app.log | Dòng log xuất hiện với format: timestamp [thread] LEVEL class - message | P1 | Functional |
| TC-08-042 | Logback – Log retention | Log cũ hơn 30 ngày bị xóa tự động | Tạo file log giả với ngày > 30 ngày trước | 1. Kiểm tra sau khi rolling log | File log cũ hơn 30 ngày bị xóa; chỉ giữ 30 ngày gần nhất | P2 | Functional |
