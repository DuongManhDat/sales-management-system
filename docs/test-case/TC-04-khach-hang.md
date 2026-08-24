# Test Cases: Khách hàng

> Module: TC-04 · Feature: Quản lý Khách hàng  
> Spec tham chiếu: [spec.md](../specs/phase-1-mvp/khach-hang/spec.md)  
> Ngày tạo: 2026-08-13

---

| Test ID | Module / Feature | Scenario Title | Pre-conditions | Test Steps | Expected Result | Priority | Type |
|---------|-----------------|----------------|----------------|------------|-----------------|----------|------|
| TC-04-001 | Khách hàng – Danh sách | Hiển thị danh sách KH đang hoạt động | Đã đăng nhập; có KH cả is_active=1 và is_active=0 | 1. Mở màn hình Khách hàng từ sidebar | Mặc định chỉ hiển thị KH với `is_active = 1`; sắp xếp theo Mã KH tăng dần | P0 | Functional |
| TC-04-002 | Khách hàng – Tìm kiếm | Tìm kiếm real-time theo Tên KH | Có nhiều KH trong danh sách | 1. Nhập "Nguyễn" vào ô tìm kiếm | Danh sách lọc real-time (debounce 200ms), chỉ hiện KH có "Nguyễn" trong tên | P0 | Functional |
| TC-04-003 | Khách hàng – Tìm kiếm | Tìm kiếm theo Số điện thoại | Đã đăng nhập | 1. Nhập "0901234" vào ô tìm kiếm | Chỉ hiển thị KH có SĐT chứa "0901234" | P0 | Functional |
| TC-04-004 | Khách hàng – Tìm kiếm | Tìm kiếm theo Mã KH | Đã đăng nhập | 1. Nhập "KH002" vào ô tìm kiếm | Chỉ hiển thị KH002 | P0 | Functional |
| TC-04-005 | Khách hàng – Tìm kiếm | Tìm kiếm không có kết quả | Đã đăng nhập | 1. Nhập "xyzabc" vào ô tìm kiếm | Hiển thị thông báo "Không tìm thấy khách hàng nào" | P1 | Negative |
| TC-04-006 | Khách hàng – Tìm kiếm | Tìm kiếm case-insensitive | Có KH tên "Nguyễn Văn A" | 1. Nhập "nguyễn văn a" (chữ thường) | Vẫn tìm thấy KH "Nguyễn Văn A" | P1 | Functional |
| TC-04-007 | Khách hàng – Lọc | Lọc theo trạng thái "Đã ẩn" | Có KH với is_active = 0 | 1. Chọn "Đã ẩn" từ dropdown Trạng thái | Chỉ hiển thị KH có is_active = 0; thao tác đổi thành [Chi tiết] [Khôi phục] | P0 | Functional |
| TC-04-008 | Khách hàng – Lọc | Lọc "Tất cả" hiển thị đủ KH | Có KH cả active và inactive | 1. Chọn "Tất cả" từ dropdown | Hiển thị tất cả KH không phân biệt is_active | P1 | Functional |
| TC-04-009 | Khách hàng – Thêm | Thêm KH mới thành công | Đã đăng nhập | 1. Bấm [+ Thêm khách hàng]<br>2. Nhập Họ tên = "Trần Thị B"<br>3. Nhập SĐT = "0901234567"<br>4. Bấm [Lưu] | KH mới xuất hiện trong danh sách; Mã KH tự sinh (KHxxx) | P0 | Functional |
| TC-04-010 | Khách hàng – Thêm | Mã KH tự động sinh sau INSERT | Đã đăng nhập | 1. Thêm KH mới | Mã KH = "KH" + số thứ tự tự động (vd: KH001); không thể nhập thủ công | P0 | Functional |
| TC-04-011 | Khách hàng – Thêm | Chặn thêm KH khi Họ tên trống | Đã đăng nhập | 1. Bấm [+ Thêm KH]<br>2. Để trống Họ tên<br>3. Nhập SĐT hợp lệ<br>4. Bấm [Lưu] | Inline error: "Họ tên không được để trống"; nút [Lưu] disabled | P0 | Negative |
| TC-04-012 | Khách hàng – Thêm | Họ tên toàn khoảng trắng bị coi là rỗng | Đã đăng nhập | 1. Nhập "   " vào Họ tên<br>2. Bấm [Lưu] | Sau trim → rỗng → inline error "Họ tên không được để trống" | P1 | Edge Case |
| TC-04-013 | Khách hàng – Validate SĐT | SĐT phải đúng 10 chữ số | Đã đăng nhập | 1. Thêm KH<br>2. Nhập SĐT = "090123456" (9 chữ số)<br>3. Bấm [Lưu] | Inline error: "Số điện thoại phải có đúng 10 chữ số" | P0 | Boundary |
| TC-04-014 | Khách hàng – Validate SĐT | SĐT 11 chữ số bị từ chối | Đã đăng nhập | 1. Nhập SĐT = "09012345678" (11 chữ số)<br>2. Bấm [Lưu] | Inline error: "Số điện thoại phải có đúng 10 chữ số" | P0 | Boundary |
| TC-04-015 | Khách hàng – Validate SĐT | SĐT đã tồn tại bị từ chối | Đã có KH với SĐT "0901234567" | 1. Thêm KH mới với SĐT "0901234567" | Inline error: "Số điện thoại đã được đăng ký" | P0 | Negative |
| TC-04-016 | Khách hàng – Validate Email | Email sai format bị từ chối | Đang trong form thêm KH | 1. Nhập Email = "abc@"<br>2. Bấm [Lưu] | Inline error: "Email không hợp lệ" | P0 | Negative |
| TC-04-017 | Khách hàng – Validate Email | Email bỏ trống được chấp nhận | Đang trong form thêm KH | 1. Để trống ô Email<br>2. Điền đủ họ tên + SĐT<br>3. Bấm [Lưu] | Lưu thành công (Email là tùy chọn) | P0 | Functional |
| TC-04-018 | Khách hàng – Validate Email | Email hợp lệ được chấp nhận | Đang trong form thêm KH | 1. Nhập Email = "test@gmail.com"<br>2. Bấm [Lưu] | Lưu thành công | P1 | Functional |
| TC-04-019 | Khách hàng – Thêm | Ngày sinh không được chọn ngày tương lai | Đang trong form thêm KH | 1. Click DatePicker Ngày sinh<br>2. Thử chọn ngày mai | DatePicker chặn, không cho chọn ngày tương lai | P1 | Boundary |
| TC-04-020 | Khách hàng – Thêm | Có thể thêm KH với chỉ Họ tên và SĐT (các trường còn lại tùy chọn) | Đã đăng nhập | 1. Nhập Họ tên và SĐT hợp lệ<br>2. Bỏ trống email, địa chỉ, ngày sinh, ghi chú<br>3. Bấm [Lưu] | Lưu thành công | P0 | Functional |
| TC-04-021 | Khách hàng – Sửa | Sửa thông tin KH thành công | Có KH trong danh sách | 1. Click [Sửa] trên dòng KH<br>2. Dialog mở với dữ liệu điền sẵn<br>3. Đổi Họ tên<br>4. Bấm [Lưu] | Thông tin KH trên danh sách cập nhật; Mã KH không thay đổi | P0 | Functional |
| TC-04-022 | Khách hàng – Sửa | Mã KH không thể chỉnh sửa khi sửa KH | Đang trong form sửa KH | 1. Quan sát field Mã KH trong dialog sửa | Mã KH hiển thị nhưng bị disabled/readonly | P0 | Functional |
| TC-04-023 | Khách hàng – Sửa | Sửa SĐT sang SĐT của KH khác bị từ chối | KH A có SĐT "0901"; KH B có SĐT "0902" | 1. Sửa KH A<br>2. Đổi SĐT thành "0902"<br>3. Bấm [Lưu] | Inline error: "Số điện thoại đã được đăng ký" | P0 | Negative |
| TC-04-024 | Khách hàng – Sửa | Sửa giữ nguyên SĐT của chính KH đó | Đang sửa KH có SĐT "0901234567" | 1. Không đổi SĐT<br>2. Đổi tên<br>3. Bấm [Lưu] | Lưu thành công (không báo lỗi trùng SĐT với chính nó) | P1 | Edge Case |
| TC-04-025 | Khách hàng – Xóa mềm | Xóa mềm KH không còn nợ và chưa có HĐ | KH không có công nợ và không có HĐ | 1. Click [Xóa] trên KH<br>2. Popup xác nhận: "Bạn có chắc muốn ẩn KH [Tên]?"<br>3. Bấm [Xác nhận] | KH biến mất khỏi danh sách; is_active = 0 | P0 | Functional |
| TC-04-026 | Khách hàng – Xóa mềm | Xóa mềm KH có HĐ đã PAID (không nợ) | KH có N HĐ đã PAID, debt = 0 | 1. Click [Xóa]<br>2. Popup cảnh báo có N HĐ cũ<br>3. Bấm [Xác nhận] | KH bị ẩn; HĐ cũ giữ nguyên customer_id; tên KH hiện "[KH đã ẩn]" trên HĐ | P0 | Functional |
| TC-04-027 | Khách hàng – Xóa mềm | Chặn xóa KH còn công nợ | KH có debt > 0 | 1. Click [Xóa] trên KH còn nợ | Hiện dialog lỗi: "Không thể xóa — khách hàng còn nợ [X đ]" | P0 | Negative |
| TC-04-028 | Khách hàng – Xóa mềm | Hủy xóa KH | Popup xác nhận xóa đang hiển thị | 1. Click [Hủy] trên popup xác nhận | Popup đóng; KH vẫn ở danh sách, is_active không thay đổi | P1 | Functional |
| TC-04-029 | Khách hàng – Khôi phục | Khôi phục KH đã ẩn | Có KH với is_active = 0 | 1. Lọc "Đã ẩn"<br>2. Click [Khôi phục] trên KH<br>3. Đổi filter về "Đang hoạt động" | KH xuất hiện lại ở danh sách mặc định | P0 | Functional |
| TC-04-030 | Khách hàng – Chi tiết | Xem chi tiết KH bao gồm 3 chỉ số tổng hợp | KH có nhiều HĐ | 1. Click [Chi tiết] trên KH | Hiển thị: thông tin KH + Tổng số đơn + Tổng doanh thu + Còn nợ | P0 | Functional |
| TC-04-031 | Khách hàng – Chi tiết | Danh sách HĐ trong chi tiết KH sắp xếp mới nhất trước | KH có nhiều HĐ | 1. Mở Chi tiết KH<br>2. Quan sát thứ tự HĐ | Danh sách HĐ sắp xếp ngày mới nhất → cũ nhất | P1 | Functional |
| TC-04-032 | Khách hàng – Chi tiết | Badge trạng thái HĐ đúng màu | KH có HĐ PAID, PARTIAL, UNPAID | 1. Xem Chi tiết KH | PAID = 🟢, PARTIAL = 🟠, UNPAID = 🔴 | P1 | Functional |
| TC-04-033 | Khách hàng – Import | Import file Excel hợp lệ | Đã chuẩn bị file Excel đúng format | 1. Click [Import]<br>2. Chọn file Excel hợp lệ<br>3. Xem preview validate<br>4. Bấm [Xác nhận import] | Import thành công; hiện "Đã import X khách hàng. Bỏ qua Y dòng lỗi." | P0 | Functional |
| TC-04-034 | Khách hàng – Import | Import file CSV hợp lệ | Đã chuẩn bị file CSV đúng format | 1. Click [Import]<br>2. Chọn file CSV<br>3. Xác nhận import | Import thành công | P0 | Functional |
| TC-04-035 | Khách hàng – Import | File import sai định dạng bị từ chối | Đang ở màn hình KH | 1. Click [Import]<br>2. Chọn file .txt | Hiện lỗi: "File không hợp lệ. Vui lòng chọn .xlsx hoặc .csv" | P0 | Negative |
| TC-04-036 | Khách hàng – Import | Import báo lỗi chi tiết từng dòng sai | File có dòng 5: SĐT sai format; dòng 8: SĐT trùng | 1. Import file<br>2. Xem màn hình preview | Preview hiện rõ: "Dòng 5: SĐT sai format", "Dòng 8: SĐT đã tồn tại"; chỉ import dòng hợp lệ | P0 | Functional |
| TC-04-037 | Khách hàng – Import | File import có SĐT trùng nhau trong nội bộ file | File có 2 dòng cùng SĐT "0901234567" | 1. Import file | Dòng sau đánh dấu lỗi "SĐT trùng với dòng X trong file"; chỉ dòng đầu được import | P1 | Edge Case |
| TC-04-038 | Khách hàng – Import | File import rỗng | File Excel không có dòng dữ liệu | 1. Import file rỗng | Hiện thông báo: "File không có dữ liệu để import" | P1 | Edge Case |
| TC-04-039 | Khách hàng – Import | File thiếu cột bắt buộc (Họ tên hoặc SĐT) | File không có cột "Họ tên" | 1. Import file thiếu cột | Hiện lỗi ngay: "File không đúng định dạng — thiếu cột bắt buộc" | P0 | Negative |
| TC-04-040 | Khách hàng – Import | Toàn bộ dòng đều lỗi → không cho import | File chỉ có dữ liệu lỗi | 1. Import file | Không import; hiện danh sách lỗi tất cả dòng | P1 | Edge Case |
| TC-04-041 | Khách hàng – Export | Export danh sách KH ra Excel | Đang hiển thị danh sách KH active | 1. Click [Export]<br>2. Chọn vị trí lưu<br>3. Xác nhận | File Excel được tạo với đúng danh sách KH đang hiển thị | P0 | Functional |
| TC-04-042 | Khách hàng – Export | Export theo filter hiện tại | Đang filter "Đã ẩn" | 1. Click [Export] khi đang lọc "Đã ẩn" | File export chứa danh sách KH đã ẩn (không phải toàn bộ KH) | P1 | Functional |
| TC-04-043 | Khách hàng – Export | Export khi danh sách rỗng | Tìm kiếm không có kết quả | 1. Nhập từ khóa không có kết quả<br>2. Click [Export] | Hiện thông báo: "Không có dữ liệu để xuất" | P1 | Edge Case |
| TC-04-044 | Khách hàng – ComboBox | KH đã ẩn không hiện trong ComboBox bán hàng | KH is_active=0 | 1. Mở màn hình Bán hàng (POS)<br>2. Mở ComboBox chọn KH | KH có is_active = 0 không xuất hiện trong ComboBox | P0 | Functional |
| TC-04-045 | Khách hàng – Performance | Tìm kiếm phản hồi < 300ms với vài nghìn KH | DB có 3000 KH | 1. Nhập từ khóa tìm kiếm<br>2. Đo thời gian phản hồi | Kết quả hiển thị trong < 300ms (có debounce 200ms) | P1 | Boundary |
