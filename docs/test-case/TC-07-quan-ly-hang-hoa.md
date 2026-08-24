# Test Cases: Quản lý Hàng hóa

> Module: TC-07 · Feature: Quản lý Hàng hóa – CRUD, Kiểm kho, Import/Export  
> Spec tham chiếu: [spec.md](../specs/phase-1-mvp/quan-ly-hang-hoa/spec.md)  
> Ngày tạo: 2026-08-13

---

| Test ID | Module / Feature | Scenario Title | Pre-conditions | Test Steps | Expected Result | Priority | Type |
|---------|-----------------|----------------|----------------|------------|-----------------|----------|------|
| TC-07-001 | Hàng hóa – Danh sách | Hiển thị SP chưa bị xóa mềm | Có SP cả deleted_at IS NULL và deleted_at có giá trị | 1. Mở màn hình "Hàng hóa" | Chỉ hiển thị SP với `deleted_at IS NULL`; sắp xếp theo Mã hàng tăng dần | P0 | Functional |
| TC-07-002 | Hàng hóa – Tìm kiếm | Tìm kiếm real-time theo Tên SP | Có nhiều SP trong danh sách | 1. Gõ "Coca" vào ô tìm kiếm | Danh sách lọc real-time (debounce 200ms), chỉ hiện SP chứa "Coca" (case-insensitive) | P0 | Functional |
| TC-07-003 | Hàng hóa – Tìm kiếm | Tìm kiếm theo Mã hàng | Có SP với mã "HH000001" | 1. Gõ "HH000001" vào ô tìm kiếm | Chỉ hiện SP có mã HH000001 | P0 | Functional |
| TC-07-004 | Hàng hóa – Tìm kiếm | Tìm kiếm không có kết quả | Đã đăng nhập | 1. Gõ "xyzabc" vào ô tìm kiếm | Hiển thị "Không tìm thấy hàng hóa nào" | P1 | Negative |
| TC-07-005 | Hàng hóa – Lọc | Lọc theo Danh mục | Có SP thuộc "Nước uống" và SP khác | 1. Chọn danh mục "Nước uống" từ dropdown | Chỉ hiển thị SP thuộc nhóm "Nước uống" | P0 | Functional |
| TC-07-006 | Hàng hóa – Lọc | Lọc trạng thái "Đã ẩn" | Có SP đã bị xóa mềm | 1. Chọn trạng thái "Đã ẩn" | Hiển thị SP với deleted_at không NULL; thao tác đổi thành [Khôi phục] | P0 | Functional |
| TC-07-007 | Hàng hóa – Lọc | Lọc kết hợp Danh mục + Trạng thái | Có nhiều SP với danh mục và trạng thái khác nhau | 1. Chọn danh mục "Thực phẩm" + trạng thái "Đang bán" | Chỉ hiển thị SP thuộc "Thực phẩm" VÀ chưa xóa mềm | P1 | Functional |
| TC-07-008 | Hàng hóa – Thêm | Thêm SP mới thành công | Đã có ít nhất 1 đơn vị tính | 1. Bấm [+ Thêm hàng hóa]<br>2. Nhập Tên = "Coca Cola 330ml"<br>3. Chọn Đơn vị = "lon"<br>4. Nhập Giá bán = 10000<br>5. Bấm [Lưu] | SP mới xuất hiện trong danh sách; Mã hàng tự sinh (HH000001) | P0 | Functional |
| TC-07-009 | Hàng hóa – Thêm | Mã hàng tự sinh HH000001 | Thêm SP lần đầu | 1. Thêm SP mới | Mã hàng = "HH" + 6 chữ số (vd: HH000001); không cho nhập thủ công | P0 | Functional |
| TC-07-010 | Hàng hóa – Thêm | Tồn kho ban đầu = 0 sau khi thêm SP | Vừa thêm SP mới | 1. Xem chi tiết SP vừa thêm | stock_qty = 0 (phải dùng Mua hàng để nhập tồn) | P0 | Functional |
| TC-07-011 | Hàng hóa – Validation | Chặn thêm SP khi Tên trống | Đang trong form thêm SP | 1. Để trống Tên hàng hóa<br>2. Bấm [Lưu] | Inline error: "Tên hàng hóa không được để trống" | P0 | Negative |
| TC-07-012 | Hàng hóa – Validation | Tên chỉ toàn khoảng trắng bị từ chối | Đang trong form thêm SP | 1. Nhập "   " vào Tên<br>2. Bấm [Lưu] | Sau trim → rỗng → inline error | P1 | Edge Case |
| TC-07-013 | Hàng hóa – Validation | Chặn thêm SP khi chưa chọn Đơn vị | Đang trong form thêm SP | 1. Không chọn Đơn vị tính<br>2. Bấm [Lưu] | Inline error: "Vui lòng chọn đơn vị tính" | P0 | Negative |
| TC-07-014 | Hàng hóa – Validation | Chặn thêm SP khi Giá bán < 0 | Đang trong form thêm SP | 1. Nhập Giá bán = -5000<br>2. Bấm [Lưu] | Inline error: "Giá bán phải là số không âm" | P0 | Negative |
| TC-07-015 | Hàng hóa – Validation | Giá bán = 0 được chấp nhận | Đang trong form thêm SP | 1. Nhập Giá bán = 0<br>2. Bấm [Lưu] | Lưu thành công (giá bán ≥ 0 hợp lệ) | P1 | Boundary |
| TC-07-016 | Hàng hóa – Thêm | ComboBox đơn vị trống → hiện hướng dẫn | Chưa có đơn vị tính nào | 1. Thêm SP<br>2. Mở ComboBox đơn vị | Thông báo "Vui lòng thêm đơn vị tính trong Cài đặt" | P1 | Edge Case |
| TC-07-017 | Hàng hóa – Sửa | Sửa thông tin SP thành công | Có SP trong danh sách | 1. Click [Sửa] trên SP<br>2. Đổi Tên<br>3. Bấm [Lưu] | SP cập nhật trong danh sách; Mã hàng không đổi | P0 | Functional |
| TC-07-018 | Hàng hóa – Sửa | Mã hàng không thể sửa | Đang trong form sửa SP | 1. Quan sát field Mã hàng | Mã hàng hiển thị nhưng bị disabled/readonly | P0 | Functional |
| TC-07-019 | Hàng hóa – Sửa | Thay đổi Giá bán → ghi price_history | SP có giá bán hiện tại = 10.000 | 1. Sửa SP<br>2. Đổi giá bán từ 10.000 → 12.000<br>3. Lưu | 1 bản ghi mới trong price_history: (product_id, old_price=10000, new_price=12000, changed_at=NOW()) | P0 | Functional |
| TC-07-020 | Hàng hóa – Sửa | Giữ nguyên giá bán → không ghi price_history | SP có giá bán = 10.000 | 1. Sửa SP chỉ đổi Tên, giữ giá cũ<br>2. Lưu | Không có bản ghi mới trong price_history | P1 | Functional |
| TC-07-021 | Hàng hóa – Sửa | Không có trường Giá vốn trong form sửa | Đang trong form sửa SP | 1. Quan sát form sửa SP | Không có trường "Giá vốn" (giá vốn chỉ trên lô hàng) | P0 | Security |
| TC-07-022 | Hàng hóa – Xóa mềm | Xóa mềm SP có stock_qty = 0 thành công | SP "Hàng X" có stock_qty = 0 | 1. Click [Xóa] trên SP<br>2. Popup xác nhận<br>3. Bấm [Xác nhận] | SP bị ẩn; deleted_at = NOW(); biến mất khỏi danh sách và ComboBox POS | P0 | Functional |
| TC-07-023 | Hàng hóa – Xóa mềm | Chặn xóa mềm SP còn tồn kho | SP có stock_qty = 5 | 1. Click [Xóa] trên SP | Hiện dialog: "Không thể ẩn — hàng hóa còn [5] đơn vị trong kho" | P0 | Negative |
| TC-07-024 | Hàng hóa – Xóa mềm | SP đã xóa mềm không hiện trong ComboBox POS | SP có deleted_at != NULL | 1. Mở màn hình Bán hàng (POS)<br>2. Tìm SP đã xóa mềm | SP không xuất hiện trong kết quả tìm kiếm POS | P0 | Functional |
| TC-07-025 | Hàng hóa – Xóa mềm | Hủy xóa → giữ nguyên SP | Popup xác nhận xóa đang hiển thị | 1. Bấm [Hủy] trên popup | Popup đóng; SP vẫn trong danh sách | P1 | Functional |
| TC-07-026 | Hàng hóa – Khôi phục | Khôi phục SP đã ẩn | Có SP với deleted_at != NULL | 1. Lọc "Đã ẩn"<br>2. Click [Khôi phục] | deleted_at = NULL; SP xuất hiện lại ở filter mặc định và ComboBox POS | P0 | Functional |
| TC-07-027 | Hàng hóa – Chi tiết | Xem chi tiết SP gồm thông tin đầy đủ | Đã có SP trong danh sách | 1. Click [Chi tiết] trên SP | Hiển thị: Mã hàng, Tên, Đơn vị, Danh mục, Tồn kho, Giá bán hiện tại, Ghi chú | P0 | Functional |
| TC-07-028 | Hàng hóa – Chi tiết | Xem lịch sử giá bán của SP | SP đã thay đổi giá bán 2 lần | 1. Xem chi tiết SP<br>2. Xem tab/section lịch sử giá | Danh sách price_history theo thứ tự mới nhất; mỗi dòng: giá cũ, giá mới, thời điểm | P1 | Functional |
| TC-07-029 | Hàng hóa – Kiểm kho | Mở màn hình Phiếu điều chỉnh kho | Đã đăng nhập; có SP active | 1. Mở màn hình Kiểm kho | Bảng hiển thị tất cả SP active với 3 cột: Tồn phần mềm / Tồn thực tế (nhập) / Chênh lệch | P0 | Functional |
| TC-07-030 | Hàng hóa – Kiểm kho | Chênh lệch tự tính = Tồn thực - Tồn phần mềm | SP "Gạo" tồn PM = 50; nhập tồn thực = 45 | 1. Nhập tồn thực = 45 cho SP "Gạo"<br>2. Quan sát cột Chênh lệch | Chênh lệch = -5 (tự tính, locked/readonly) | P0 | Functional |
| TC-07-031 | Hàng hóa – Kiểm kho | Bắt buộc điền Lý do khi chênh lệch ≠ 0 | Đang kiểm kho, có SP chênh lệch | 1. Nhập tồn thực khác tồn PM<br>2. Bấm [Lưu phiếu] mà không điền Lý do | Chặn lưu; báo bắt buộc điền lý do cho từng dòng có chênh lệch | P0 | Negative |
| TC-07-032 | Hàng hóa – Kiểm kho | Variance > 0 → bắt buộc nhập Giá vốn | SP chênh lệch = +10 (thực > PM) | 1. Nhập tồn thực lớn hơn PM<br>2. Bấm [Lưu] | Bắt buộc nhập Giá vốn cho số hàng dôi dư | P0 | Negative |
| TC-07-033 | Hàng hóa – Kiểm kho | Lưu phiếu kiểm kho thành công | Đã điền đầy đủ lý do và giá vốn | 1. Điền đầy đủ<br>2. Bấm [Lưu phiếu] | stock_qty cập nhật; stock_movements type=KIEMKHO; inventory_batch mới nếu Variance>0 | P0 | Functional |
| TC-07-034 | Hàng hóa – Kiểm kho | SP không thay đổi → không cần lý do | Nhập tồn thực = tồn PM | 1. Không thay đổi tồn thực<br>2. Bấm [Lưu] | Lưu bình thường; không cần lý do (chênh lệch = 0) | P1 | Edge Case |
| TC-07-035 | Hàng hóa – Kiểm kho | Xem lịch sử phiếu điều chỉnh kho | Đã có phiếu điều chỉnh | 1. Mở tab Lịch sử phiếu điều chỉnh | Danh sách phiếu: ngày, mã phiếu (DC000001...), số dòng có thay đổi | P1 | Functional |
| TC-07-036 | Hàng hóa – Import | Import file Excel SP hợp lệ | Chuẩn bị file Excel đúng format | 1. Click [Import]<br>2. Chọn file Excel<br>3. Xem preview<br>4. Bấm [Xác nhận import] | SP mới được import thành công; Mã hàng tự sinh | P0 | Functional |
| TC-07-037 | Hàng hóa – Import | Trùng mã → báo lỗi, bỏ qua dòng đó | File import có dòng trùng mã HH000001 | 1. Import file<br>2. Xem preview | Dòng trùng mã báo lỗi; chỉ import dòng không trùng | P0 | Negative |
| TC-07-038 | Hàng hóa – Import | Validate và preview trước khi lưu | File import có 1 dòng lỗi | 1. Chọn file import<br>2. Xem màn hình preview | Hiển thị dòng hợp lệ và dòng lỗi rõ ràng trước khi import | P0 | Functional |
| TC-07-039 | Hàng hóa – Export | Export theo filter đang áp dụng | Đang lọc danh mục "Nước uống" | 1. Click [Export] | File xuất chứa đúng SP đang hiển thị theo filter | P0 | Functional |
| TC-07-040 | Hàng hóa – Export | Export khi danh sách rỗng | Tìm kiếm không có kết quả | 1. Tìm kiếm không có SP<br>2. Click [Export] | Thông báo "Không có dữ liệu để xuất" | P1 | Edge Case |
| TC-07-041 | Hàng hóa – Performance | Tìm kiếm real-time phản hồi < 300ms | DB có vài nghìn SP | 1. Gõ từ khóa | Kết quả hiện trong < 300ms | P1 | Boundary |
| TC-07-042 | Hàng hóa – stock_movements | Mọi thay đổi tồn kho đều ghi stock_movements | Bán hàng/Nhập hàng/Kiểm kho | 1. Thực hiện thao tác thay đổi kho<br>2. Kiểm tra bảng stock_movements | Luôn có bản ghi tương ứng trong stock_movements; không bao giờ UPDATE stock_qty trực tiếp mà không ghi log | P0 | Security |
