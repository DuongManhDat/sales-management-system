# Test Cases: Danh mục (Đơn vị tính & Nhóm hàng)

> Module: TC-02 · Feature: Quản lý Danh mục  
> Spec tham chiếu: [spec.md](../specs/phase-1-mvp/danh-muc/spec.md)  
> Ngày tạo: 2026-08-13

---

| Test ID | Module / Feature | Scenario Title | Pre-conditions | Test Steps | Expected Result | Priority | Type |
|---------|-----------------|----------------|----------------|------------|-----------------|----------|------|
| TC-02-001 | Đơn vị tính | Hiển thị danh sách đơn vị tính (chỉ ACTIVE) | Đã đăng nhập; có đơn vị tính cả ACTIVE và INACTIVE | 1. Vào Cài đặt → Tab "Đơn vị tính" | Chỉ hiển thị các đơn vị có `status = ACTIVE`; không hiện INACTIVE | P0 | Functional |
| TC-02-002 | Đơn vị tính | Thêm đơn vị tính mới thành công | Đã đăng nhập; tab Đơn vị tính | 1. Bấm [+ Thêm đơn vị]<br>2. Nhập tên "thùng"<br>3. Bấm [Lưu] | Đơn vị "thùng" xuất hiện trong danh sách; Label tổng tăng 1 | P0 | Functional |
| TC-02-003 | Đơn vị tính | Không thể thêm đơn vị với tên trùng (case-insensitive) | Đã có đơn vị "kg" trong DB | 1. Bấm [+ Thêm đơn vị]<br>2. Nhập "KG" hoặc "kg"<br>3. Bấm [Lưu] | Inline error: "Tên đơn vị đã tồn tại, vui lòng chọn tên khác." | P0 | Negative |
| TC-02-004 | Đơn vị tính | Không thể thêm đơn vị với tên trống | Đã đăng nhập | 1. Bấm [+ Thêm đơn vị]<br>2. Để trống ô tên<br>3. Bấm [Lưu] | Nút [Lưu] disabled hoặc inline error "Tên không được để trống" | P0 | Negative |
| TC-02-005 | Đơn vị tính | Tên chỉ toàn khoảng trắng bị coi là rỗng | Đã đăng nhập | 1. Bấm [+ Thêm đơn vị]<br>2. Nhập "   " (3 dấu cách)<br>3. Bấm [Lưu] | Sau trim → rỗng → báo lỗi "Tên không được để trống" | P1 | Edge Case |
| TC-02-006 | Đơn vị tính | Tên đơn vị quá dài (>100 ký tự) | Đã đăng nhập | 1. Bấm [+ Thêm đơn vị]<br>2. Nhập chuỗi 150 ký tự | TextField giới hạn tối đa 100 ký tự; không thể nhập quá | P2 | Boundary |
| TC-02-007 | Đơn vị tính | Tìm kiếm real-time theo tên đơn vị | Đã có nhiều đơn vị trong danh sách | 1. Gõ "kg" vào ô tìm kiếm | Danh sách lọc real-time, chỉ hiển thị đơn vị có "kg" trong tên (không cần nhấn Enter) | P0 | Functional |
| TC-02-008 | Đơn vị tính | Tìm kiếm không có kết quả | Đã đăng nhập | 1. Gõ "xyzabc" vào ô tìm kiếm | Danh sách trống; hiển thị thông báo hoặc bảng rỗng | P1 | Negative |
| TC-02-009 | Đơn vị tính | Sửa tên đơn vị tính thành công | Đã có đơn vị "cái" trong danh sách | 1. Bấm [✏️ Sửa] trên dòng "cái"<br>2. Dialog mở, điền sẵn "cái"<br>3. Đổi thành "chiếc"<br>4. Bấm [Lưu] | Đơn vị trong danh sách cập nhật thành "chiếc" | P0 | Functional |
| TC-02-010 | Đơn vị tính | Sửa đơn vị thành tên trùng với đơn vị khác | Có "cái" và "hộp" trong danh sách | 1. Bấm [✏️ Sửa] trên "cái"<br>2. Đổi thành "hộp"<br>3. Bấm [Lưu] | Inline error: "Tên đã tồn tại, vui lòng chọn tên khác." | P0 | Negative |
| TC-02-011 | Đơn vị tính | Sửa đơn vị giữ nguyên tên hiện tại | Đã có đơn vị "kg" | 1. Bấm [✏️ Sửa] trên "kg"<br>2. Để nguyên tên "kg"<br>3. Bấm [Lưu] | Lưu thành công (không báo lỗi trùng với chính nó) | P1 | Edge Case |
| TC-02-012 | Đơn vị tính | Xóa mềm đơn vị không được sản phẩm sử dụng | Có đơn vị "thùng" chưa có SP nào dùng | 1. Bấm [🗑️ Xóa] trên "thùng"<br>2. Popup xác nhận hiện ra<br>3. Bấm [Xóa] | "thùng" biến mất khỏi danh sách (status = INACTIVE) | P0 | Functional |
| TC-02-013 | Đơn vị tính | Chặn xóa đơn vị đang được sản phẩm sử dụng | Đơn vị "kg" có ít nhất 1 SP đang dùng | 1. Bấm [🗑️ Xóa] trên "kg" | Hiện cảnh báo: "Không thể xóa. Đơn vị đang được [N] sản phẩm sử dụng." | P0 | Negative |
| TC-02-014 | Đơn vị tính | Hủy xóa đơn vị | Đã có đơn vị "lít" không có SP | 1. Bấm [🗑️ Xóa] trên "lít"<br>2. Popup xác nhận hiện<br>3. Bấm [Hủy] | Popup đóng; "lít" vẫn còn trong danh sách | P1 | Functional |
| TC-02-015 | Đơn vị tính | Thêm đơn vị tên trùng với đơn vị INACTIVE | Có đơn vị "thùng" với status INACTIVE | 1. Bấm [+ Thêm đơn vị]<br>2. Nhập "thùng"<br>3. Bấm [Lưu] | Báo lỗi "Tên đã tồn tại" (kể cả INACTIVE); không thêm mới | P2 | Edge Case |
| TC-02-016 | Đơn vị tính | Bấm [Lưu] nhiều lần liên tiếp (double-click) | Dialog thêm đơn vị đang mở | 1. Nhập tên hợp lệ<br>2. Double-click [Lưu] | Nút bị disabled sau lần click đầu; chỉ INSERT 1 lần vào DB | P1 | Edge Case |
| TC-02-017 | Đơn vị tính | Phản hồi CRUD < 300ms | Dataset vài chục đơn vị | 1. Thêm đơn vị mới<br>2. Đo thời gian từ lúc bấm [Lưu] đến khi danh sách cập nhật | Danh sách cập nhật trong < 300ms | P1 | Boundary |
| TC-02-018 | Nhóm hàng | Hiển thị danh sách nhóm hàng (chỉ ACTIVE) | Đã đăng nhập; có nhóm hàng cả ACTIVE và INACTIVE | 1. Vào Cài đặt → Tab "Nhóm hàng" | Chỉ hiển thị nhóm hàng có `status = ACTIVE` | P0 | Functional |
| TC-02-019 | Nhóm hàng | Thêm nhóm hàng mới thành công | Đã đăng nhập; tab Nhóm hàng | 1. Bấm [+ Thêm nhóm hàng]<br>2. Nhập tên "Nước uống"<br>3. Bấm [Lưu] | Nhóm "Nước uống" xuất hiện trong danh sách | P0 | Functional |
| TC-02-020 | Nhóm hàng | Không thể thêm nhóm hàng tên trùng | Đã có nhóm "Thực phẩm" | 1. Thêm nhóm tên "THỰC PHẨM" | Inline error: "Tên đã tồn tại, vui lòng chọn tên khác." | P0 | Negative |
| TC-02-021 | Nhóm hàng | Chặn xóa nhóm hàng đang được sản phẩm sử dụng | Nhóm "Nước uống" có ít nhất 1 SP dùng | 1. Bấm [🗑️ Xóa] trên "Nước uống" | Hiện cảnh báo không cho xóa | P0 | Negative |
| TC-02-022 | Nhóm hàng | Xóa mềm nhóm hàng không có SP sử dụng | Nhóm "Nhóm tạm" chưa có SP nào dùng | 1. Bấm [🗑️ Xóa] trên "Nhóm tạm"<br>2. Xác nhận | "Nhóm tạm" biến mất khỏi danh sách | P0 | Functional |
| TC-02-023 | Nhóm hàng | Tìm kiếm real-time nhóm hàng | Đã có nhiều nhóm hàng | 1. Gõ "nước" vào ô tìm kiếm | Chỉ hiển thị nhóm chứa "nước" (case-insensitive) | P0 | Functional |
| TC-02-024 | Nhóm hàng | Label tổng cập nhật đúng sau khi thêm/xóa | Hiện có 3 nhóm hàng | 1. Thêm 1 nhóm mới → kiểm tra tổng<br>2. Xóa 1 nhóm → kiểm tra tổng | Label "Tổng: N nhóm hàng" cập nhật đúng (4 sau khi thêm, 3 sau khi xóa) | P1 | Functional |
| TC-02-025 | Danh mục chung | Tab mặc định khi vào Cài đặt là "Đơn vị tính" | Đã đăng nhập | 1. Từ sidebar click [⚙️ Cài đặt] | Tab "Đơn vị tính" được chọn mặc định | P1 | Functional |
| TC-02-026 | Danh mục chung | Chuyển tab giữa Đơn vị tính và Nhóm hàng | Đang ở tab Đơn vị tính | 1. Click tab "Nhóm hàng" | Nội dung chuyển sang danh sách nhóm hàng | P1 | Functional |
