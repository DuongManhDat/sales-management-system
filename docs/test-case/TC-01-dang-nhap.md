# Test Cases: Đăng nhập (Authentication)

> Module: TC-01 · Feature: Đăng nhập & Thiết lập mật khẩu lần đầu  
> Spec tham chiếu: [spec.md](../specs/phase-1-mvp/dang-nhap/spec.md)  
> Ngày tạo: 2026-08-13

---

| Test ID | Module / Feature | Scenario Title | Pre-conditions | Test Steps | Expected Result | Priority | Type |
|---------|-----------------|----------------|----------------|------------|-----------------|----------|------|
| TC-01-001 | Đăng nhập | Đăng nhập thành công với mật khẩu đúng | App đã cài, bảng `app_user` có 1 bản ghi, mật khẩu đã thiết lập | 1. Mở app<br>2. Form đăng nhập hiện ra<br>3. Nhập đúng mật khẩu<br>4. Bấm [Đăng nhập] | Chuyển sang Main Window (màn hình chính với sidebar) | P0 | Functional |
| TC-01-002 | Đăng nhập | Đăng nhập thành công khi nhấn Enter | App đã cài, bảng `app_user` có 1 bản ghi | 1. Mở app<br>2. Nhập đúng mật khẩu<br>3. Nhấn phím Enter | Chuyển sang Main Window (tương đương bấm nút [Đăng nhập]) | P0 | Functional |
| TC-01-003 | Đăng nhập | Đăng nhập thất bại với mật khẩu sai | App đã cài, bảng `app_user` có 1 bản ghi | 1. Mở app<br>2. Nhập sai mật khẩu<br>3. Bấm [Đăng nhập] | Hiện popup "Sai mật khẩu. Vui lòng thử lại." → Ô mật khẩu bị xóa, focus lại ô | P0 | Negative |
| TC-01-004 | Đăng nhập | Nút Đăng nhập bị disabled khi ô mật khẩu trống | App đã cài, bảng `app_user` có 1 bản ghi | 1. Mở app<br>2. Để trống ô mật khẩu<br>3. Quan sát nút [Đăng nhập] | Nút [Đăng nhập] bị disabled (không click được) | P0 | Boundary |
| TC-01-005 | Đăng nhập | Toggle hiện/ẩn mật khẩu trên form đăng nhập | App đã cài, form đăng nhập đang hiển thị | 1. Nhập mật khẩu "abc123"<br>2. Bấm nút [👁️]<br>3. Quan sát ô mật khẩu | Mật khẩu hiển thị rõ "abc123" (chuyển từ PasswordField sang TextField) | P1 | Functional |
| TC-01-006 | Đăng nhập | Toggle ẩn lại mật khẩu sau khi đã hiện | Form đăng nhập đang hiện mật khẩu | 1. Bấm nút [🔒] (khi đang hiện MK)<br>2. Quan sát ô mật khẩu | Mật khẩu bị ẩn lại (dấu chấm •••••) | P1 | Functional |
| TC-01-007 | Đăng nhập | Hiển thị phiên bản app trên form đăng nhập | App đã cài | 1. Mở app<br>2. Quan sát góc dưới phải form đăng nhập | Label phiên bản hiển thị (vd: "v1.0.0") | P2 | Functional |
| TC-01-008 | Đăng nhập | Không cho phép nhập sai vô tận (không khóa) | App đã cài, bảng `app_user` có 1 bản ghi | 1. Nhập sai mật khẩu 10 lần liên tiếp<br>2. Bấm [Đăng nhập] mỗi lần | App vẫn cho phép nhập tiếp (không bị khóa sau N lần sai) | P2 | Security |
| TC-01-009 | Đăng nhập | Mật khẩu toàn khoảng trắng vẫn được xác thực | App đã cài, mật khẩu đã thiết lập = "   " (3 dấu cách) | 1. Nhập "   " vào ô mật khẩu<br>2. Bấm [Đăng nhập] | Hệ thống so khớp BCrypt đúng; nếu khớp → vào Main Window; nếu sai → popup lỗi | P2 | Edge Case |
| TC-01-010 | Đăng nhập | Bấm nút Đăng nhập nhiều lần liên tiếp (double-click) | App đã cài, mật khẩu đúng | 1. Nhập đúng mật khẩu<br>2. Double-click nhanh nút [Đăng nhập] | Chỉ xử lý 1 lần; nút bị disabled sau lần click đầu; vào Main Window đúng 1 lần | P1 | Edge Case |
| TC-01-011 | Đăng nhập | App kiểm tra `app_user` nhanh < 200ms | App đã cài, bảng `app_user` có 1 bản ghi | 1. Mở app<br>2. Đo thời gian từ lúc mở đến khi form đăng nhập hiển thị | Form hiển thị trong vòng 200ms | P1 | Boundary |
| TC-01-012 | Đăng nhập | BCrypt verify chạy trên background thread (không đơ UI) | App đã cài | 1. Nhập mật khẩu<br>2. Bấm [Đăng nhập]<br>3. Thử di chuyển cửa sổ trong lúc đang verify | UI không bị đơ/freeze trong khi đang verify BCrypt | P1 | Functional |
| TC-01-013 | Đăng nhập | Mật khẩu rất dài (>72 ký tự) | App đã cài, mật khẩu = chuỗi 100 ký tự | 1. Nhập 100 ký tự vào ô mật khẩu<br>2. Bấm [Đăng nhập] | BCrypt tự cắt ở 72 bytes; xác thực vẫn hoạt động bình thường (không crash) | P2 | Edge Case |
| TC-01-014 | Thiết lập MK lần đầu | App lần đầu mở — hiện form thiết lập MK | Bảng `app_user` trống (DB mới) | 1. Mở app lần đầu<br>2. Quan sát màn hình | Hiển thị form "Thiết lập mật khẩu lần đầu" (không phải form đăng nhập) | P0 | Functional |
| TC-01-015 | Thiết lập MK lần đầu | Thiết lập mật khẩu thành công | Bảng `app_user` trống | 1. Nhập "password123" vào ô Mật khẩu<br>2. Nhập "password123" vào ô Xác nhận<br>3. Bấm [Xác nhận] | Popup "Thiết lập mật khẩu thành công!" → bấm [OK] → chuyển về form Đăng nhập | P0 | Functional |
| TC-01-016 | Thiết lập MK lần đầu | Hai ô mật khẩu không khớp | Bảng `app_user` trống | 1. Nhập "pass1" vào ô Mật khẩu<br>2. Nhập "pass2" vào ô Xác nhận<br>3. Bấm [Xác nhận] | Popup "Mật khẩu xác nhận không khớp." → Xóa ô xác nhận, focus lại ô | P0 | Negative |
| TC-01-017 | Thiết lập MK lần đầu | Ô Mật khẩu để trống | Bảng `app_user` trống | 1. Để trống ô Mật khẩu<br>2. Nhập "abc" vào ô Xác nhận<br>3. Bấm [Xác nhận] | Popup "Vui lòng nhập mật khẩu." hoặc nút [Xác nhận] bị disabled | P0 | Negative |
| TC-01-018 | Thiết lập MK lần đầu | Ô Xác nhận mật khẩu để trống | Bảng `app_user` trống | 1. Nhập "abc123" vào ô Mật khẩu<br>2. Để trống ô Xác nhận<br>3. Bấm [Xác nhận] | Popup "Vui lòng xác nhận mật khẩu." hoặc nút [Xác nhận] bị disabled | P0 | Negative |
| TC-01-019 | Thiết lập MK lần đầu | Nút Xác nhận bị disabled khi 1 ô trống | Bảng `app_user` trống | 1. Nhập "pass" vào ô Mật khẩu<br>2. Để trống ô Xác nhận<br>3. Quan sát nút [Xác nhận] | Nút [Xác nhận] bị disabled | P0 | Boundary |
| TC-01-020 | Thiết lập MK lần đầu | Toggle hiện/ẩn mật khẩu trên form thiết lập | Bảng `app_user` trống | 1. Nhập "abc123" vào ô Mật khẩu<br>2. Bấm [👁️] cạnh ô Mật khẩu | Nội dung ô hiển thị rõ "abc123" | P1 | Functional |
| TC-01-021 | Thiết lập MK lần đầu | Toggle hiện/ẩn trên ô Xác nhận MK | Bảng `app_user` trống | 1. Nhập "abc123" vào ô Xác nhận<br>2. Bấm [👁️] cạnh ô Xác nhận | Nội dung ô xác nhận hiển thị rõ "abc123" | P1 | Functional |
| TC-01-022 | Thiết lập MK lần đầu | Sau thiết lập thành công → chuyển về form Đăng nhập | Bảng `app_user` trống | 1. Thiết lập MK thành công<br>2. Bấm [OK] trên popup thành công | Hiển thị form Đăng nhập (không tự động vào Main Window) | P0 | Functional |
| TC-01-023 | Thiết lập MK lần đầu | BCrypt hash chạy trên background thread | Bảng `app_user` trống | 1. Nhập 2 ô hợp lệ<br>2. Bấm [Xác nhận]<br>3. Thử di chuyển cửa sổ trong lúc hash | UI không bị đơ/freeze trong khi đang hash BCrypt | P1 | Functional |
| TC-01-024 | Thiết lập MK lần đầu | Lỗi DB khi INSERT app_user | Bảng `app_user` trống, DB bị lỗi write | 1. Nhập MK hợp lệ<br>2. Bấm [Xác nhận]<br>3. Simulate lỗi DB | Hiện popup lỗi hệ thống; form giữ nguyên để thử lại | P1 | Negative |
| TC-01-025 | Thiết lập MK lần đầu | Mật khẩu toàn khoảng trắng được chấp nhận | Bảng `app_user` trống | 1. Nhập "   " (3 dấu cách) vào cả 2 ô<br>2. Bấm [Xác nhận] | Hệ thống chấp nhận (không trim MK) → thiết lập thành công | P2 | Edge Case |
| TC-01-026 | Luồng điều hướng | App khởi động — DB bị xóa/corrupt → coi như lần đầu | DB bị xóa hoặc corrupt | 1. Xóa file DB<br>2. Mở app | SchemaInitializer tạo lại schema → Hiện form Thiết lập MK lần đầu | P1 | Edge Case |
| TC-01-027 | Luồng điều hướng | Bảng `app_user` có nhiều hơn 1 bản ghi | DB có 2 bản ghi trong `app_user` | 1. Mở app | Hệ thống lấy bản ghi đầu tiên (LIMIT 1), không báo lỗi | P2 | Edge Case |
