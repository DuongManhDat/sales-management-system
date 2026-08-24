# Test Cases: Mua hàng (Nhập hàng)

> Module: TC-06 · Feature: Mua hàng – Nhập kho & Quản lý NCC  
> Spec tham chiếu: [spec.md](../specs/phase-1-mvp/mua-hang/spec.md)  
> Ngày tạo: 2026-08-13

---

| Test ID | Module / Feature | Scenario Title | Pre-conditions | Test Steps | Expected Result | Priority | Type |
|---------|-----------------|----------------|----------------|------------|-----------------|----------|------|
| TC-06-001 | Mua hàng – Danh sách | Hiển thị danh sách phiếu nhập mới nhất trước | Đã có nhiều phiếu nhập | 1. Mở màn hình "Mua hàng" từ sidebar | Danh sách phiếu nhập hiển thị, sắp xếp theo ngày mới nhất | P0 | Functional |
| TC-06-002 | Mua hàng – Tìm kiếm | Tìm kiếm phiếu nhập theo Mã phiếu | Có phiếu "PN001" | 1. Nhập "PN001" vào ô tìm kiếm | Chỉ hiện phiếu PN001 | P0 | Functional |
| TC-06-003 | Mua hàng – Tìm kiếm | Tìm kiếm theo Tên NCC | Có phiếu nhập từ NCC "Công ty A" | 1. Nhập "Công ty A" vào ô tìm kiếm | Chỉ hiện phiếu của NCC "Công ty A" | P0 | Functional |
| TC-06-004 | Mua hàng – Lọc | Lọc phiếu nhập theo trạng thái "Còn nợ" | Có phiếu cả PAID và UNPAID | 1. Chọn filter "Còn nợ" | Chỉ hiển thị phiếu có status PARTIAL hoặc UNPAID | P0 | Functional |
| TC-06-005 | Mua hàng – Lọc | Lọc phiếu nhập theo trạng thái "Đã trả" | Có phiếu đã PAID | 1. Chọn filter "Đã trả" | Chỉ hiển thị phiếu có status = PAID | P1 | Functional |
| TC-06-006 | Mua hàng – Tạo phiếu | Tạo phiếu nhập thành công với đầy đủ thông tin | Đã có ít nhất 1 SP và 1 NCC | 1. Bấm [+ Tạo phiếu nhập]<br>2. Chọn NCC<br>3. Thêm 1 dòng SP (SL=10, giá nhập=5.000)<br>4. Nhập tiền đã trả = Tổng<br>5. Bấm [💾 Lưu phiếu nhập] | Phiếu PN001 được tạo; tồn kho SP tăng 10; 1 lô mới trong inventory_batches; stock_movements ghi type=NHAP; status=PAID | P0 | Functional |
| TC-06-007 | Mua hàng – Tạo phiếu | Tạo phiếu nhập không gắn NCC (NCC optional) | Có ít nhất 1 SP | 1. Tạo phiếu nhập<br>2. Bỏ trống NCC<br>3. Thêm SP và lưu | Phiếu tạo thành công mà không có NCC | P0 | Functional |
| TC-06-008 | Mua hàng – Tạo phiếu | Mã phiếu tự động sinh (PNxxx) | Đang tạo phiếu nhập | 1. Mở form tạo phiếu nhập | Mã phiếu tự động hiển thị (PNxxx), không cho nhập thủ công | P0 | Functional |
| TC-06-009 | Mua hàng – Tạo phiếu | Ngày nhập mặc định là hôm nay | Đang tạo phiếu nhập | 1. Mở form tạo phiếu nhập | Ngày nhập mặc định = hôm nay | P1 | Functional |
| TC-06-010 | Mua hàng – Tạo phiếu | Thành tiền tự động tính = SL × Giá nhập | Đang thêm dòng SP: SL=5, giá nhập=10.000 | 1. Nhập SL=5, giá nhập=10.000<br>2. Quan sát Thành tiền | Thành tiền = 50.000 đ (tự động, readonly) | P0 | Functional |
| TC-06-011 | Mua hàng – Tạo phiếu | TỔNG NHẬP cập nhật real-time | Đang thêm nhiều dòng SP | 1. Thêm dòng SP thứ 2<br>2. Quan sát TỔNG NHẬP | TỔNG NHẬP = Σ Thành tiền, cập nhật ngay | P0 | Functional |
| TC-06-012 | Mua hàng – Tạo phiếu | Hiển thị Còn nợ NCC = Tổng − Đã trả | TỔNG = 100.000 đ; tiền đã trả = 60.000 đ | 1. Nhập tiền đã trả = 60.000 | Còn nợ NCC = 40.000 đ | P0 | Functional |
| TC-06-013 | Mua hàng – Tạo phiếu | Chặn lưu khi phiếu không có dòng nào | Đang ở form tạo phiếu nhập | 1. Không thêm dòng SP nào<br>2. Bấm [Lưu phiếu] | Chặn lưu: "Phiếu nhập phải có ít nhất 1 sản phẩm" | P0 | Negative |
| TC-06-014 | Mua hàng – Validation | Số lượng dòng ≤ 0 bị từ chối | Đang thêm dòng SP | 1. Nhập SL = 0<br>2. Bấm [Lưu] | Lỗi inline: "Số lượng phải lớn hơn 0" | P0 | Negative |
| TC-06-015 | Mua hàng – Validation | Số lượng âm bị từ chối | Đang thêm dòng SP | 1. Nhập SL = -5 | Lỗi inline: "Số lượng phải lớn hơn 0" | P0 | Boundary |
| TC-06-016 | Mua hàng – Validation | Giá nhập ≤ 0 bị từ chối | Đang thêm dòng SP | 1. Nhập giá nhập = 0 | Lỗi inline: "Giá nhập phải lớn hơn 0" | P0 | Negative |
| TC-06-017 | Mua hàng – Validation | Tiền đã trả > Tổng nhập bị từ chối | TỔNG = 100.000 đ | 1. Nhập tiền đã trả = 150.000 | Lỗi inline: "Tiền trả không được vượt quá tổng phiếu nhập" | P0 | Negative |
| TC-06-018 | Mua hàng – Atomic | Tạo phiếu atomic: lỗi DB → rollback | Simulate lỗi DB sau khi insert purchases | 1. Tạo phiếu nhập<br>2. Simulate lỗi DB | ROLLBACK toàn bộ; tồn kho không thay đổi; không có lô hàng mới | P0 | Negative |
| TC-06-019 | Mua hàng – Lô hàng | Mỗi dòng SP tạo 1 lô hàng với giá vốn riêng | Tạo phiếu nhập 2 dòng SP | 1. Tạo phiếu có dòng A(giá=5k) và B(giá=8k)<br>2. Lưu phiếu | 2 bản ghi mới trong `inventory_batches` với cost_price tương ứng | P0 | Functional |
| TC-06-020 | Mua hàng – Tồn kho | Tồn kho SP tăng đúng sau khi nhập hàng | SP "Gạo" có stock_qty = 20 | 1. Tạo phiếu nhập 50 kg Gạo<br>2. Lưu phiếu | products.stock_qty "Gạo" = 70 | P0 | Functional |
| TC-06-021 | Mua hàng – stock_movements | Ghi stock_movements type=NHAP sau khi nhập hàng | Tạo phiếu nhập thành công | 1. Tạo phiếu nhập<br>2. Kiểm tra bảng stock_movements | Có bản ghi type='NHAP', qty_change=+SL, ref_type='PURCHASE' | P0 | Functional |
| TC-06-022 | Mua hàng – Phần trả NCC | Tạo phiếu với tiền đã trả một phần → status=PARTIAL | TỔNG = 100.000 đ; trả 60.000 đ | 1. Tạo phiếu, nhập tiền đã trả = 60.000<br>2. Lưu | Phiếu có status=PARTIAL; debt=40.000 đ; 1 bản ghi trong supplier_payments | P0 | Functional |
| TC-06-023 | Mua hàng – Phần trả NCC | Tạo phiếu chưa trả → status=UNPAID | TỔNG = 100.000; tiền đã trả = 0 | 1. Tạo phiếu, nhập tiền đã trả = 0<br>2. Lưu | Phiếu có status=UNPAID; debt=100.000; không có bản ghi trong supplier_payments | P1 | Functional |
| TC-06-024 | Mua hàng – Trả nợ NCC | Trả nợ NCC một phần | Có phiếu status=PARTIAL, còn nợ 40.000 | 1. Mở chi tiết phiếu<br>2. Bấm [Trả nợ NCC]<br>3. Nhập 20.000<br>4. Xác nhận | INSERT supplier_payments; debt giảm 20.000; status vẫn PARTIAL | P0 | Functional |
| TC-06-025 | Mua hàng – Trả nợ NCC | Trả nợ NCC toàn bộ → status chuyển PAID | Còn nợ 40.000 đ | 1. Trả nợ đúng 40.000 đ | debt=0; status=PAID; không còn nút [Trả nợ NCC] | P0 | Functional |
| TC-06-026 | Mua hàng – Trả nợ NCC | Không cho trả nợ ≤ 0 | Dialog trả nợ đang mở | 1. Nhập số tiền = 0<br>2. Bấm [Xác nhận] | Validation chặn; không cho nhập | P0 | Negative |
| TC-06-027 | Mua hàng – Trả nợ NCC | Không cho trả nợ > số còn nợ | Còn nợ 40.000 | 1. Nhập số tiền = 50.000 | Chặn: "Vượt quá số nợ hiện tại" | P0 | Negative |
| TC-06-028 | Mua hàng – Chi tiết | Xem chi tiết phiếu nhập đầy đủ | Có phiếu nhập với thanh toán | 1. Click [Xem chi tiết] trên phiếu | Hiển thị: Mã phiếu, Ngày, NCC, dòng SP (tên, SL, giá nhập, thành tiền, mã lô), Tổng/Đã trả/Còn nợ, Lịch sử thanh toán | P0 | Functional |
| TC-06-029 | Mua hàng – Chi tiết | Nút [Trả nợ NCC] chỉ hiện khi còn nợ | Phiếu có status=PARTIAL | 1. Mở chi tiết phiếu còn nợ | Nút [Trả nợ NCC] hiển thị | P0 | Functional |
| TC-06-030 | Mua hàng – Chi tiết | Nút [Trả nợ NCC] ẩn khi đã thanh toán đủ | Phiếu có status=PAID | 1. Mở chi tiết phiếu đã PAID | Không có nút [Trả nợ NCC] | P1 | Functional |
| TC-06-031 | NCC – Thêm | Thêm NCC mới thành công | Đã đăng nhập | 1. Vào quản lý NCC<br>2. Bấm [+ Thêm NCC]<br>3. Điền tên, SĐT<br>4. Lưu | NCC mới xuất hiện trong danh sách; Mã NCC tự sinh | P0 | Functional |
| TC-06-032 | NCC – Thêm | Mã NCC tự sinh | Đang thêm NCC | 1. Mở form thêm NCC | Mã NCC tự động sinh; không cho nhập thủ công | P1 | Functional |
| TC-06-033 | NCC – Tìm kiếm | Tìm kiếm NCC theo Tên / SĐT / Mã NCC | Có nhiều NCC | 1. Nhập từ khóa vào ô tìm kiếm NCC | Danh sách NCC lọc theo từ khóa | P0 | Functional |
| TC-06-034 | NCC – Sửa | Sửa thông tin NCC | Đã có NCC trong danh sách | 1. Click [Sửa] trên NCC<br>2. Đổi thông tin<br>3. Lưu | Thông tin NCC được cập nhật | P0 | Functional |
| TC-06-035 | NCC – Xóa mềm | Xóa mềm NCC không có phiếu nhập | NCC chưa có phiếu nhập | 1. Click [Xóa] trên NCC | NCC bị ẩn khỏi danh sách | P0 | Functional |
| TC-06-036 | NCC – Khôi phục | Khôi phục NCC đã ẩn | Có NCC đã xóa mềm | 1. Lọc NCC đã ẩn<br>2. Click [Khôi phục] | NCC xuất hiện lại trong danh sách | P1 | Functional |
| TC-06-037 | Mua hàng – FIFO | SUM(inventory_batches.qty_remaining) = products.stock_qty | Có nhiều lô hàng | 1. Nhập hàng 2 phiếu khác ngày cho cùng 1 SP<br>2. Kiểm tra tồn kho | SUM(qty_remaining) trong inventory_batches = products.stock_qty (invariant kho) | P0 | Functional |
| TC-06-038 | Mua hàng – Performance | Tạo phiếu nhập phản hồi nhanh | Dataset bình thường | 1. Tạo phiếu với 10 dòng SP<br>2. Lưu | Toàn bộ transaction (insert + update stock + create batch + log) hoàn thành trong < 1s | P1 | Boundary |
| TC-06-039 | Mua hàng – Threading | Tạo phiếu chạy trên background thread | Đang tạo phiếu nhập | 1. Bấm [Lưu phiếu]<br>2. Thử tương tác UI | UI không bị freeze | P0 | Functional |
