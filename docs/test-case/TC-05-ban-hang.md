# Test Cases: Bán hàng (POS)

> Module: TC-05 · Feature: Bán hàng – Point of Sale  
> Spec tham chiếu: [spec.md](../specs/phase-1-mvp/ban-hang/spec.md)  
> Ngày tạo: 2026-08-13

---

| Test ID | Module / Feature | Scenario Title | Pre-conditions | Test Steps | Expected Result | Priority | Type |
|---------|-----------------|----------------|----------------|------------|-----------------|----------|------|
| TC-05-001 | POS – Tìm kiếm SP | Tìm kiếm SP theo tên thành công | Có SP "Coca Cola" trong hệ thống | 1. Mở màn hình Bán hàng<br>2. Gõ "Coca" vào ô tìm kiếm | Hiển thị SP "Coca Cola" trong kết quả real-time | P0 | Functional |
| TC-05-002 | POS – Tìm kiếm SP | Tìm kiếm SP theo Mã SP | Có SP với mã "SP001" | 1. Gõ "SP001" vào ô tìm kiếm | Hiển thị SP có mã SP001 trong kết quả | P0 | Functional |
| TC-05-003 | POS – Tìm kiếm SP | Tìm kiếm phản hồi < 500ms | DB có vài nghìn SP | 1. Gõ từ khóa | Kết quả hiện trong < 500ms | P1 | Boundary |
| TC-05-004 | POS – Giỏ hàng | Thêm SP vào giỏ bằng click | SP "Gạo ST25" đang hiển thị trong kết quả tìm kiếm | 1. Click vào "Gạo ST25" trong danh sách kết quả | SP xuất hiện trong giỏ hàng bên phải với số lượng = 1 | P0 | Functional |
| TC-05-005 | POS – Giỏ hàng | Thêm SP đã có trong giỏ → tăng số lượng | "Gạo ST25" đã ở trong giỏ với SL=1 | 1. Click lại "Gạo ST25" trong kết quả | Số lượng "Gạo ST25" trong giỏ tăng lên 2 | P0 | Functional |
| TC-05-006 | POS – Giỏ hàng | Cảnh báo bán âm kho khi thêm SP hết hàng | SP "Mì Hảo Hảo" có stock_qty = 0 | 1. Tìm "Mì Hảo Hảo"<br>2. Click thêm vào giỏ | Hiển thị cảnh báo inline "⚠️ Mì Hảo Hảo đã hết hàng (tồn: 0). Vẫn cho phép bán." SP vẫn được thêm vào giỏ | P0 | Edge Case |
| TC-05-007 | POS – Giỏ hàng | Cảnh báo bán âm kho — không chặn giao dịch | SP có stock_qty = -2 | 1. Thêm SP có tồn âm vào giỏ | Cảnh báo hiện nhưng SP vẫn được thêm vào giỏ; không block giao dịch | P0 | Edge Case |
| TC-05-008 | POS – Giỏ hàng | Chỉnh số lượng bằng nút [+] | SP trong giỏ với SL=2 | 1. Bấm nút [+] trên dòng SP | Số lượng tăng lên 3; thành tiền cập nhật real-time | P0 | Functional |
| TC-05-009 | POS – Giỏ hàng | Chỉnh số lượng bằng nút [−] | SP trong giỏ với SL=3 | 1. Bấm nút [−] trên dòng SP | Số lượng giảm xuống 2; thành tiền cập nhật real-time | P0 | Functional |
| TC-05-010 | POS – Giỏ hàng | Không cho số lượng ≤ 0 | SP trong giỏ với SL=1 | 1. Bấm nút [−] khi SL=1 | Số lượng không giảm xuống 0; giữ nguyên giá trị cũ hoặc tự xóa dòng | P0 | Boundary |
| TC-05-011 | POS – Giỏ hàng | Nhập số lượng trực tiếp | SP trong giỏ | 1. Click vào ô số lượng<br>2. Gõ số "5" | Số lượng = 5; thành tiền cập nhật real-time | P0 | Functional |
| TC-05-012 | POS – Giỏ hàng | Nhập số lượng ≤ 0 bị từ chối | SP trong giỏ | 1. Nhập "0" hoặc "-1" vào ô số lượng | Không cho phép; giữ nguyên giá trị cũ | P0 | Negative |
| TC-05-013 | POS – Giỏ hàng | Xóa dòng SP khỏi giỏ | Giỏ hàng có nhiều SP | 1. Click [✖] trên dòng SP cần xóa | SP bị xóa khỏi giỏ; tạm tính cập nhật | P0 | Functional |
| TC-05-014 | POS – Tính tiền | Tạm tính = Σ (đơn giá × số lượng) | Giỏ có 2 SP: A×2 (10k) và B×3 (5k) | 1. Quan sát Tạm tính | Tạm tính = 2×10.000 + 3×5.000 = 35.000 đ | P0 | Functional |
| TC-05-015 | POS – Giảm giá | Nhập giảm giá 5% | Tạm tính = 100.000 đ | 1. Nhập "5" vào ô Giảm giá (%)<br>2. Quan sát | Tiền giảm = 5.000 đ; TỔNG = 95.000 đ (cập nhật real-time) | P0 | Functional |
| TC-05-016 | POS – Giảm giá | Giảm giá 0% | Tạm tính = 100.000 đ | 1. Nhập "0" vào ô giảm giá | TỔNG = Tạm tính = 100.000 đ; Tiền giảm = 0 | P1 | Boundary |
| TC-05-017 | POS – Giảm giá | Giảm giá 100% | Tạm tính = 100.000 đ | 1. Nhập "100" vào ô giảm giá | TỔNG = 0 đ; Tiền giảm = 100.000 đ | P1 | Boundary |
| TC-05-018 | POS – Giảm giá | Không cho nhập giảm giá > 100% | Đang ở màn hình POS | 1. Nhập "101" vào ô giảm giá | Lỗi validation; không cho nhập giá trị > 100 | P0 | Negative |
| TC-05-019 | POS – Giảm giá | Không cho nhập giảm giá < 0% | Đang ở màn hình POS | 1. Nhập "-5" vào ô giảm giá | Lỗi validation; không cho nhập giá trị âm | P0 | Negative |
| TC-05-020 | POS – Khách hàng | Bắt buộc chọn KH trước khi thanh toán | Giỏ hàng có SP; chưa chọn KH | 1. Quan sát nút [Thanh toán & In PDF] khi chưa chọn KH | Nút [Thanh toán & In PDF] bị disabled khi chưa chọn KH | P0 | Functional |
| TC-05-021 | POS – Khách hàng | Chọn KH từ dropdown | Có KH trong danh sách | 1. Click ComboBox chọn KH<br>2. Chọn "Nguyễn Văn A" | KH "Nguyễn Văn A" được chọn | P0 | Functional |
| TC-05-022 | POS – Thanh toán | Tính tiền thối khi khách đưa >= Tổng | TỔNG = 63.650 đ | 1. Nhập Tiền khách đưa = 100.000<br>2. Quan sát | Hiển thị "Tiền thối: 36.350 đ" (màu xanh) | P0 | Functional |
| TC-05-023 | POS – Thanh toán | Tính tiền còn nợ khi khách đưa < Tổng | TỔNG = 63.650 đ | 1. Nhập Tiền khách đưa = 40.000<br>2. Quan sát | Hiển thị "Còn nợ: 23.650 đ" (màu cam) | P0 | Functional |
| TC-05-024 | POS – Thanh toán | Thanh toán đầy đủ (PAID) thành công | Giỏ hàng có SP; KH đã chọn; tiền đưa >= tổng | 1. Bấm [Thanh toán & In PDF] | INSERT invoice (status=PAID); trừ tồn kho; ghi stock_movements; sinh PDF; bật print dialog | P0 | Functional |
| TC-05-025 | POS – Thanh toán | Thanh toán một phần — ghi nợ (PARTIAL) | Giỏ hàng có SP; KH đã chọn; tiền đưa < tổng | 1. Nhập tiền đưa < tổng<br>2. Bấm [Thanh toán & In PDF] | INSERT invoice (status=PARTIAL); INSERT payment; trừ tồn kho; ghi nợ phần còn lại | P0 | Functional |
| TC-05-026 | POS – Thanh toán | Giao dịch atomic — tất cả trong 1 transaction | Đang thanh toán | 1. Simulate lỗi DB sau khi insert invoice nhưng trước khi trừ kho | ROLLBACK toàn bộ; hiện dialog lỗi; giỏ hàng giữ nguyên | P0 | Negative |
| TC-05-027 | POS – Thanh toán | Nút [Thanh toán] disabled khi giỏ hàng trống | Giỏ hàng trống | 1. Quan sát nút [Thanh toán & In PDF] | Nút bị disabled | P0 | Boundary |
| TC-05-028 | POS – PDF | Sinh PDF hóa đơn A4 sau khi thanh toán | Thanh toán thành công | 1. Bấm [Thanh toán & In PDF] | File PDF được sinh; hộp thoại Print của Windows bật lên | P0 | Functional |
| TC-05-029 | POS – PDF | PDF hiển thị đúng trạng thái thanh toán | Thanh toán PARTIAL | 1. Thanh toán một phần<br>2. Xem PDF | PDF hiển thị "Còn nợ: X đ" đúng | P1 | Functional |
| TC-05-030 | POS – Hủy đơn | Hủy đơn với popup xác nhận | Giỏ hàng có SP | 1. Bấm nút [Hủy đơn]<br>2. Popup xác nhận hiện<br>3. Bấm [Xác nhận] | Giỏ hàng bị xóa sạch; reset giảm giá và tiền đưa về 0 | P0 | Functional |
| TC-05-031 | POS – Hủy đơn | Hủy popup xác nhận → giữ nguyên giỏ | Popup xác nhận hủy đơn đang hiện | 1. Bấm [Hủy] trên popup | Popup đóng; giỏ hàng giữ nguyên | P0 | Functional |
| TC-05-032 | POS – Thu nợ | Thu nợ một phần thành công | Có HĐ status=PARTIAL | 1. Tìm HĐ còn nợ<br>2. Click [Thu nợ]<br>3. Nhập số tiền < số còn nợ<br>4. Xác nhận | INSERT payment; cập nhật invoice.paid; status giữ PARTIAL | P0 | Functional |
| TC-05-033 | POS – Thu nợ | Thu nợ toàn bộ → status chuyển PAID | HĐ còn nợ 50.000 đ | 1. Thu nợ đúng 50.000 đ | INSERT payment; invoice.paid = total; status = PAID | P0 | Functional |
| TC-05-034 | POS – Thu nợ | Không cho thu nợ ≤ 0 | Dialog thu nợ đang mở | 1. Nhập số tiền = 0<br>2. Bấm [Xác nhận] | Validation chặn; không cho nhập | P0 | Negative |
| TC-05-035 | POS – Thu nợ | Không cho thu nợ > số còn nợ | HĐ còn nợ 30.000 đ | 1. Nhập số tiền = 50.000<br>2. Bấm [Xác nhận] | Validation chặn; thông báo "Vượt quá số nợ" | P0 | Negative |
| TC-05-036 | POS – Thu nợ | Thu nợ atomic | Simulate lỗi DB | 1. Thực hiện thu nợ<br>2. Simulate lỗi DB | ROLLBACK; hiện dialog lỗi | P1 | Negative |
| TC-05-037 | POS – Danh sách HĐ | Xem danh sách hóa đơn | Có nhiều HĐ | 1. Mở tab "Danh sách hóa đơn" | Hiển thị danh sách HĐ với Mã HĐ, Ngày, KH, Tổng, Đã trả, Còn nợ, Trạng thái | P0 | Functional |
| TC-05-038 | POS – Công nợ | Lọc HĐ theo trạng thái "Còn nợ" | Có HĐ cả PAID và PARTIAL | 1. Lọc trạng thái "Còn nợ" | Chỉ hiện HĐ có status = PARTIAL hoặc UNPAID | P0 | Functional |
| TC-05-039 | POS – Tính tiền | Tiền tệ xử lý bằng long — không có sai số floating point | TỔNG = 1.000.001 đ; giảm giá 33% | 1. Tính 1.000.001 × 33% | Kết quả là số nguyên VND, không có phần thập phân; không dùng double | P0 | Functional |
| TC-05-040 | POS – Threading | Thanh toán chạy trên background thread | Đang thanh toán | 1. Bấm [Thanh toán & In PDF]<br>2. Thử tương tác UI | UI không bị freeze trong lúc xử lý giao dịch | P0 | Functional |
| TC-05-041 | POS – Sau giao dịch | Xóa giỏ hàng sau khi thanh toán thành công | Giao dịch vừa hoàn thành | 1. Quan sát sau khi thanh toán | Giỏ hàng bị xóa sạch; sẵn sàng cho đơn tiếp theo | P0 | Functional |
