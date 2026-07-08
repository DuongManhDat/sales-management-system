# Kế hoạch MVP — Hệ thống Quản lý Bán hàng (Desktop)

> Tài liệu thiết kế được tạo qua quy trình brainstorming. Phiên bản: 1.0 · Ngày: 2026-06-27
> Trạng thái: **Đã chốt hiểu biết (Understanding Lock) + thiết kế kỹ thuật. Sẵn sàng triển khai.**

---

## 1. Tóm tắt hiểu biết (Understanding Summary)

- **Cái gì:** Ứng dụng desktop **JavaFX (Java 21)** quản lý bán hàng cho cửa hàng, chạy **hoàn toàn offline**, đóng gói thành **file `.exe`** chạy trên máy Windows cá nhân.
- **Cho ai:** **Một người dùng duy nhất** — chủ cửa hàng (không multi-user, không phân quyền role).
- **Vì sao:** Thay thế quản lý thủ công/Excel bằng một công cụ tập trung quản lý hàng hóa, nhập hàng, bán hàng, khách hàng và doanh thu; **dữ liệu riêng tư nằm trên máy cá nhân**.
- **Cách làm:** Triển khai theo **giai đoạn** — MVP (Giai đoạn 1) là phần lõi, các tính năng nâng cao thuộc Giai đoạn 2.

### Ràng buộc chính
- Offline 100%, không cloud, không đồng bộ nhiều máy.
- Database cục bộ riêng tư (file trên máy người dùng).
- Đóng gói thành `.exe` / installer Windows.
- Có đăng nhập bằng mật khẩu.
- Xuất hóa đơn **PDF khổ A4**.
- Tìm/nhập sản phẩm **thủ công** (chưa dùng mã vạch).

### Non-goals (Không làm trong phạm vi này)
- ❌ Cloud / đồng bộ nhiều máy / web app.
- ❌ Multi-user, phân quyền theo vai trò.
- ❌ Mã vạch (barcode) / máy in nhiệt 80mm.
- ❌ Bán hàng online, tích hợp sàn TMĐT.
- ❌ Đa tiền tệ; VAT/thuế phức tạp; nhiều bảng giá theo nhóm khách.

---

## 2. Giả định (Assumptions)

| # | Giả định | Trạng thái |
|---|----------|-----------|
| 1 | Quy mô dữ liệu nhỏ: vài trăm–vài nghìn SP, vài chục nghìn đơn/năm | Đã chấp nhận |
| 2 | Tiền tệ: VND, định dạng `1.000.000 đ`, không đa tiền tệ | Đã chấp nhận |
| 3 | Database: **SQLite** (file `.db` cục bộ) | Đã chấp nhận |
| 4 | Đóng gói: **jpackage** (JDK 21) → `.exe`/installer | Đã chấp nhận |
| 5 | Mật khẩu được **hash** (BCrypt); DB chưa mã hóa toàn bộ ở MVP | Đã chấp nhận |
| 6 | Sao lưu MVP = sao chép tay file `.db`; backup/restore tự động ở GĐ2 | Đã chấp nhận |
| 7 | Hóa đơn MVP chưa tính VAT phức tạp (chỉ tổng tiền + giảm giá đơn giản) | Đã chấp nhận (mặc định) |
| 8 | Một giá bán / sản phẩm (không bảng giá theo nhóm khách) | Đã chấp nhận (mặc định) |
| 9 | Đơn vị tính: **danh mục đơn vị tập trung + mỗi SP chọn 1 đơn vị** (không quy đổi) | Đã chốt |
| 10 | Bán âm kho: **cảnh báo nhưng vẫn cho bán** | Đã chốt |

---

## 3. Decision Log (Nhật ký quyết định)

| Quyết định | Phương án đã chọn | Phương án khác đã cân nhắc | Lý do chọn |
|---|---|---|---|
| Phạm vi triển khai | **Phân giai đoạn** | Làm tất cả cùng lúc | Giảm rủi ro, có sản phẩm chạy được sớm |
| In hóa đơn | **PDF A4** | In nhiệt 80mm; cả hai | Dễ làm, linh hoạt, không phụ thuộc phần cứng |
| Đăng nhập | **Có mật khẩu** | Không đăng nhập | Bảo vệ dữ liệu doanh thu nhạy cảm |
| Mã vạch | **Không** (nhập tay) | Dùng máy quét | Đơn giản hóa MVP; có thể thêm sau |
| Đơn vị tính | **Danh mục đơn vị + 1 đv/SP** | Đa đơn vị quy đổi; nhập tự do | Linh hoạt + chuẩn hóa, không phức tạp hóa tồn kho |
| Bán âm kho | **Cảnh báo, vẫn cho bán** | Chặn bán khi hết hàng | Phù hợp thực tế cửa hàng nhỏ |
| Kiến trúc | **MVVM-lite** (FXML→Controller→Service→DAO) | MVC thuần | Tách lớp rõ, dễ test, dễ bảo trì |
| Database | **SQLite** | H2, Apache Derby | Nhúng, phổ biến, công cụ xem DB sẵn có |
| Data access | **JDBC thuần + DAO** | JPA/Hibernate; jOOQ | Nhẹ, khởi động nhanh, file đóng gói nhỏ, kiểm soát SQL |
| Build tool | **Maven** | Gradle | Phổ biến, tích hợp jpackage tốt |
| Đóng gói | **jpackage + jlink** | Launch4j; GraalVM native | Có sẵn JDK 21, tạo runtime gọn + .exe |
| Xuất PDF | **JasperReports** | OpenPDF/iText; PDFBox | Có template hóa đơn/báo cáo trực quan |
| Biểu đồ | **JavaFX Charts** built-in | Thư viện ngoài | Đủ dùng, không thêm phụ thuộc |
| Hash mật khẩu | **BCrypt** | SHA-256 + salt | Chuẩn an toàn, có salt sẵn |
| Ghi log lỗi | **SLF4J + Logback** | java.util.logging | Ghi log ra file, cấu hình linh hoạt |
| Ghi log nghiệp vụ | **Bảng DB** (stock_movements MVP, activity_log GĐ2) | Chỉ ghi file | Truy vấn/lọc được trong app |

---

## 4. Danh sách chức năng (Feature List)

### 🟢 Giai đoạn 1 — MVP (bản chạy được đầu tiên)

| Module | Chức năng | Mô tả ngắn |
|---|---|---|
| **Đăng nhập** | Khóa app bằng mật khẩu | Màn hình login, đổi mật khẩu trong cài đặt |
| **Danh mục** | Đơn vị tính | Thêm/sửa/xóa đơn vị (cái, hộp, kg, lít, thùng...) |
| | Nhóm/loại hàng | Phân loại sản phẩm (tùy chọn) |
| **Quản lý hàng hóa** | Danh sách hàng hóa | CRUD sản phẩm: mã, tên, đơn vị, nhóm, giá vốn, giá bán, tồn kho |
| | Thiết lập giá | Cập nhật giá bán / giá vốn |
| **Mua hàng** | Nhập hàng | Tạo phiếu nhập, tăng tồn kho, cập nhật giá vốn |
| **Bán hàng** | Tạo đơn / Hóa đơn | Chọn SP, số lượng, giảm giá, chọn khách, thanh toán, trừ tồn kho |
| | Xuất hóa đơn PDF | Sinh hóa đơn PDF A4, in hoặc lưu file |
| **Khách hàng** | Quản lý khách hàng | CRUD khách: tên, SĐT, địa chỉ; xem lịch sử mua |
| **Dashboard** | Thống kê doanh thu | Theo **ngày / giờ / thứ trong tuần**; tổng doanh thu, số đơn, top sản phẩm |
| **Hệ thống** | Ghi log lỗi (Logback) | Tự động ghi log lỗi/cảnh báo ra file để chẩn đoán |
| | Log biến động tồn kho | Mỗi lần nhập/bán/điều chỉnh ghi 1 dòng `stock_movements` |

### 🔵 Giai đoạn 2 — Mở rộng

| Module | Chức năng | Mô tả ngắn |
|---|---|---|
| **Hàng hóa** | Kiểm kho | Kiểm đếm thực tế, điều chỉnh chênh lệch tồn kho |
| **Mua hàng** | Trả nhập hàng | Trả hàng cho nhà cung cấp, giảm tồn kho |
| **Bán hàng** | Đặt hàng (order chờ) | Tạo đơn đặt trước, chuyển thành hóa đơn khi giao |
| | Trả hàng (bán) | Khách trả hàng, hoàn tiền, tăng lại tồn kho |
| **Báo cáo** | Báo cáo nâng cao | Lãi/lỗ, công nợ, xuất–nhập–tồn, xuất Excel/PDF |
| **Hệ thống** | Sao lưu / Khôi phục | Backup/restore file DB tự động theo lịch |
| | Nhật ký hoạt động (audit) | `activity_log`: lịch sử đổi giá, xóa SP, sửa dữ liệu... + màn hình xem log |
| **Danh mục** | Nhà cung cấp | Quản lý nhà cung cấp cho phiếu nhập |

> 💡 **Có thể bổ sung thêm:** chiết khấu theo %, phương thức thanh toán (tiền mặt/chuyển khoản), công nợ khách, in nhãn giá, chế độ tối (dark mode).

---

## 5. Sơ đồ màn hình (Text Wireframes)

### 5.1 Bản đồ điều hướng (Navigation Map)

```
[Login]
   │ (đăng nhập thành công)
   ▼
[Main Window] ── Sidebar điều hướng
   ├── Dashboard
   ├── Hàng hóa
   │     ├── Danh sách hàng hóa
   │     └── Thiết lập giá
   ├── Mua hàng
   │     └── Nhập hàng            (GĐ2: Trả nhập hàng)
   ├── Bán hàng
   │     ├── Tạo hóa đơn (POS)
   │     └── Danh sách hóa đơn    (GĐ2: Đặt hàng, Trả hàng)
   ├── Khách hàng
   ├── Báo cáo
   └── Cài đặt (đổi mật khẩu, danh mục đơn vị/nhóm)
```

### 5.2 Màn hình Đăng nhập

```
┌──────────────────────────────────────────────┐
│                                                │
│              🏪  QUẢN LÝ BÁN HÀNG              │
│                                                │
│        Mật khẩu:  [ •••••••••••• ]             │
│                                                │
│                 [  Đăng nhập  ]                │
│                                                │
│        (lần đầu: thiết lập mật khẩu)           │
└──────────────────────────────────────────────┘
```

### 5.3 Cửa sổ chính (Main Window — bố cục chung)

```
┌────────────┬──────────────────────────────────────────────┐
│            │  Tiêu đề trang                  👤 Chủ cửa hàng │
│  🏪 LOGO    ├──────────────────────────────────────────────┤
│            │                                                │
│ 📊 Dashboard│                                                │
│ 📦 Hàng hóa │            VÙNG NỘI DUNG CHÍNH                 │
│ 🛒 Mua hàng │         (đổi theo mục đang chọn)               │
│ 💵 Bán hàng │                                                │
│ 👥 Khách    │                                                │
│ 📈 Báo cáo  │                                                │
│ ⚙️ Cài đặt  │                                                │
│            │                                                │
└────────────┴──────────────────────────────────────────────┘
```

### 5.4 Dashboard

```
┌──────────────────────────────────────────────────────────┐
│  Dashboard                          Kỳ: [Hôm nay ▼]        │
├──────────────────────────────────────────────────────────┤
│  ┌─────────┐ ┌─────────┐ ┌─────────┐ ┌─────────┐          │
│  │Doanh thu│ │Số đơn   │ │Khách mới│ │SP sắp hết│         │
│  │ 12.5tr đ│ │   34    │ │    5    │ │    8     │          │
│  └─────────┘ └─────────┘ └─────────┘ └─────────┘          │
│                                                            │
│  Doanh thu theo GIỜ            Doanh thu theo THỨ          │
│  ┌──────────────────────┐     ┌──────────────────────┐    │
│  │      ▂▃▅▇█▇▅▃         │     │  T2 T3 T4 T5 T6 T7 CN│    │
│  │  (biểu đồ cột/đường)  │     │  ▅  ▆  ▃  ▇  █  ▇  ▂ │    │
│  └──────────────────────┘     └──────────────────────┘    │
│                                                            │
│  Top sản phẩm bán chạy                                     │
│  1. Coca 330ml ······· 120   2. Mì gói ······· 95         │
└──────────────────────────────────────────────────────────┘
```

### 5.5 Danh sách hàng hóa

```
┌──────────────────────────────────────────────────────────┐
│  Hàng hóa        🔍[ tìm tên/mã...      ]  [+ Thêm SP]     │
├──────────────────────────────────────────────────────────┤
│ Mã    │ Tên           │ ĐVT │ Giá bán  │ Tồn │ Thao tác   │
│ SP001 │ Coca 330ml    │ lon │ 10.000 đ │ 240 │ ✏️  🗑️      │
│ SP002 │ Gạo ST25      │ kg  │ 35.000 đ │  18 │ ✏️  🗑️      │
│ SP003 │ Mì gói Hảo Hảo│ gói │  4.000 đ │ ⚠️0 │ ✏️  🗑️      │
├──────────────────────────────────────────────────────────┤
│                                   ◄ 1 2 3 ►   Tổng: 152 SP │
└──────────────────────────────────────────────────────────┘
```

### 5.6 Form thêm/sửa hàng hóa (Dialog)

```
┌───────────── Thêm / Sửa sản phẩm ─────────────┐
│ Mã SP:    [ SP004            ]                 │
│ Tên:      [ Nước suối Lavie  ]                 │
│ Đơn vị:   [ chai        ▼ ]  Nhóm: [ Nước ▼ ] │
│ Giá vốn:  [ 3.000        ]                     │
│ Giá bán:  [ 5.000        ]                     │
│ Tồn kho:  [ 0            ]  (chỉ đọc khi sửa)  │
│                                                │
│                     [ Hủy ]   [ Lưu ]          │
└────────────────────────────────────────────────┘
```

### 5.7 Bán hàng / Tạo hóa đơn (POS)

```
┌───────────────────────────────────┬──────────────────────┐
│ 🔍[ tìm sản phẩm...            ]   │  HÓA ĐƠN              │
│ ┌───────────────────────────────┐ │  Khách: [ Khách lẻ ▼]│
│ │ Coca 330ml      10.000 đ      │ │ ─────────────────────│
│ │ Gạo ST25        35.000 đ      │ │ Coca x2     20.000   │
│ │ Mì Hảo Hảo       4.000 đ      │ │ Gạo  x1     35.000   │
│ │ ... (click để thêm)           │ │ Mì   x3     12.000   │
│ └───────────────────────────────┘ │ ─────────────────────│
│                                   │ Tạm tính:   67.000   │
│ ⚠️ "Mì Hảo Hảo" tồn = 0,          │ Giảm giá: [ 2.000  ] │
│    vẫn cho bán (âm kho)            │ TỔNG:       65.000 đ │
│                                   │ Khách đưa:[ 70.000 ] │
│                                   │ Tiền thối:   5.000 đ │
│                                   │ [Thanh toán & In PDF]│
└───────────────────────────────────┴──────────────────────┘
```

### 5.8 Nhập hàng (Phiếu nhập)

```
┌──────────────────────────────────────────────────────────┐
│  Nhập hàng — Phiếu nhập mới       Ngày: [27/06/2026]       │
│  Nhà cung cấp: [ (GĐ2) ___________ ]                       │
├──────────────────────────────────────────────────────────┤
│ Sản phẩm        │ SL  │ Giá nhập │ Thành tiền │  ✖        │
│ [Coca 330ml  ▼] │ 100 │  7.000   │  700.000   │  ✖        │
│ [Gạo ST25    ▼] │  50 │ 30.000   │1.500.000   │  ✖        │
│ [+ Thêm dòng]                                              │
├──────────────────────────────────────────────────────────┤
│                              TỔNG NHẬP:  2.200.000 đ       │
│                              [ Hủy ]   [ Lưu phiếu nhập ]  │
└──────────────────────────────────────────────────────────┘
```

### 5.9 Quản lý khách hàng

```
┌──────────────────────────────────────────────────────────┐
│  Khách hàng     🔍[ tìm tên/SĐT... ]      [+ Thêm khách]   │
├──────────────────────────────────────────────────────────┤
│ Tên          │ SĐT        │ Địa chỉ    │ Tổng mua │ Thao tác│
│ Nguyễn Văn A │ 0901234567 │ Q.1, HCM   │ 5.2tr đ  │ ✏️ 👁️   │
│ Trần Thị B   │ 0907654321 │ Q.3, HCM   │ 1.8tr đ  │ ✏️ 👁️   │
└──────────────────────────────────────────────────────────┘
   👁️ = xem lịch sử mua hàng của khách
```

### 5.10 Hóa đơn PDF A4 (bố cục in)

```
┌──────────────── HÓA ĐƠN BÁN HÀNG ────────────────┐
│  CỬA HÀNG ABC                  Số HĐ: HD000123    │
│  Địa chỉ / SĐT                 Ngày: 27/06/2026   │
│  Khách hàng: Nguyễn Văn A      SĐT: 0901234567    │
├───────────────────────────────────────────────────┤
│ STT │ Tên hàng    │ ĐVT │ SL │ Đơn giá │ T.Tiền   │
│  1  │ Coca 330ml  │ lon │ 2  │ 10.000  │ 20.000   │
│  2  │ Gạo ST25    │ kg  │ 1  │ 35.000  │ 35.000   │
├───────────────────────────────────────────────────┤
│                       Tạm tính:        55.000 đ    │
│                       Giảm giá:         2.000 đ    │
│                       TỔNG CỘNG:       53.000 đ    │
│  (Bằng chữ: Năm mươi ba nghìn đồng)               │
│                                                    │
│   Người mua hàng              Người bán hàng       │
└────────────────────────────────────────────────────┘
```

---

## 6. Use Cases (Các tình huống sử dụng)

### UC-01 — Đăng nhập
- **Actor:** Chủ cửa hàng
- **Tiền điều kiện:** App đã cài, đã thiết lập mật khẩu.
- **Luồng chính:** Mở app → nhập mật khẩu → hệ thống so khớp hash → vào màn hình chính.
- **Luồng phụ:** Lần đầu chưa có mật khẩu → yêu cầu thiết lập. Sai mật khẩu → báo lỗi, không vào.

### UC-02 — Thêm sản phẩm mới
- **Luồng chính:** Vào Hàng hóa → "Thêm SP" → nhập mã/tên/đơn vị/nhóm/giá vốn/giá bán → Lưu.
- **Ràng buộc:** Mã SP không trùng; giá ≥ 0; đơn vị chọn từ danh mục.
- **Hậu điều kiện:** SP xuất hiện trong danh sách, tồn kho khởi tạo = 0.

### UC-03 — Nhập hàng
- **Luồng chính:** Vào Mua hàng → tạo phiếu nhập → thêm dòng (SP, SL, giá nhập) → Lưu.
- **Hậu điều kiện:** **Tồn kho tăng** theo SL; giá vốn được cập nhật; lưu lịch sử phiếu nhập.

### UC-04 — Bán hàng & xuất hóa đơn (luồng quan trọng nhất)
- **Luồng chính:** Vào Bán hàng → tìm & thêm SP vào giỏ → chỉnh SL → chọn khách (hoặc khách lẻ) → nhập giảm giá → nhập tiền khách đưa → "Thanh toán & In PDF".
- **Hậu điều kiện:** Tạo hóa đơn lưu DB; **trừ tồn kho**; sinh **PDF A4**; doanh thu cập nhật Dashboard.
- **Luồng ngoại lệ:** SP tồn = 0 → **hiển thị cảnh báo nhưng vẫn cho bán** (âm kho). Tiền khách đưa < tổng → báo lỗi.

### UC-05 — Quản lý khách hàng
- **Luồng chính:** Thêm/sửa/xóa khách; xem lịch sử mua hàng theo khách.
- **Ràng buộc:** Không xóa khách đã gắn với hóa đơn (chỉ ẩn/đánh dấu ngừng).

### UC-06 — Xem Dashboard doanh thu
- **Luồng chính:** Mở Dashboard → chọn kỳ (hôm nay/tuần/tháng) → xem doanh thu theo **ngày/giờ/thứ**, số đơn, top SP, SP sắp hết.

### UC-07 — Quản lý danh mục đơn vị
- **Luồng chính:** Cài đặt → Đơn vị → thêm/sửa/xóa đơn vị dùng chung.
- **Ràng buộc:** Không xóa đơn vị đang được SP sử dụng.

### Use cases Giai đoạn 2 (tóm tắt)
- UC-08 Kiểm kho · UC-09 Trả nhập hàng · UC-10 Trả hàng (bán) · UC-11 Đặt hàng chờ · UC-12 Báo cáo nâng cao · UC-13 Sao lưu/khôi phục.

---

## 7. Mô hình dữ liệu sơ bộ (Data Model)

```
units (id, name, status)
categories (id, name)                         -- nhóm hàng (tùy chọn)
products (id, code, name, unit_id→units, category_id→categories,
          cost_price, sale_price, stock_qty, status)
customers (id, name, phone, address, status)
purchases (id, code, supplier, purchase_date, total, note)
purchase_items (id, purchase_id→purchases, product_id→products,
                qty, cost_price, amount)
invoices (id, code, customer_id→customers, invoice_date,
          subtotal, discount, total, paid, status)
invoice_items (id, invoice_id→invoices, product_id→products,
               qty, sale_price, amount)
app_user (id, password_hash)                  -- 1 người dùng
settings (key, value)                         -- thông tin cửa hàng, v.v.

-- Ghi log nghiệp vụ:
stock_movements (id, product_id→products, type, qty_change,    -- [MVP]
                 stock_after, ref_type, ref_id, created_at, note)
                 -- type: NHAP|BAN|TRA|KIEMKHO ; ref: phiếu nhập/hóa đơn liên quan
activity_log (id, action, entity, entity_id, detail, created_at)  -- [GĐ2]
                 -- vd: action=UPDATE_PRICE, entity=product, detail="5000→6000"
```

**Nguyên tắc tồn kho:** `products.stock_qty` được cập nhật khi **Nhập hàng** (+) và **Bán hàng** (−). **Mọi thay đổi tồn đều ghi 1 dòng vào `stock_movements`** (kèm tồn sau thay đổi + chứng từ liên quan) để truy vết — đặc biệt quan trọng khi cho phép bán âm kho.

---

## 8. Đề xuất công nghệ (Tech Stack)

| Hạng mục | Lựa chọn | Ghi chú |
|---|---|---|
| JDK | **Java 21 (LTS)** | Theo yêu cầu |
| UI | **JavaFX 21** + **FXML** | Tách view/logic; dùng Scene Builder để dựng FXML |
| Kiến trúc | **MVVM-lite**: FXML → Controller → Service → DAO | Tách lớp, dễ test |
| Database | **SQLite** | Driver: `org.xerial:sqlite-jdbc` |
| Data access | **JDBC thuần + DAO** | Tự viết SQL trong các lớp DAO; dùng `PreparedStatement` |
| Build | **Maven** | `javafx-maven-plugin` để chạy/đóng gói |
| Đóng gói .exe | **jpackage** + **jlink** | Tạo runtime gọn + installer/exe Windows (có thể cần WiX Toolset) |
| Xuất PDF | **JasperReports** | Hoặc OpenPDF nếu muốn nhẹ hơn; thiết kế template `.jrxml` |
| Biểu đồ | **JavaFX Charts** (BarChart, LineChart) | Built-in, không cần lib ngoài |
| Hash mật khẩu | **BCrypt** (`org.mindrot:jbcrypt`) | Lưu hash, không lưu thô |
| Ghi log | **SLF4J + Logback** (`ch.qos.logback`) | Ghi log lỗi/hoạt động ra file xoay vòng |
| Tiền tệ/ngày | `java.time` + `NumberFormat` (vi-VN) | Định dạng `1.000.000 đ`, `dd/MM/yyyy` |
| Test | **JUnit 5** | Ưu tiên test lớp Service/DAO |

**Cấu trúc thư mục đề xuất:**
```
src/main/java/com/shop/
  ├─ App.java                 (entry point)
  ├─ view/                    (FXML controllers)
  ├─ viewmodel/               (state cho UI - tùy chọn)
  ├─ service/                 (logic nghiệp vụ)
  ├─ dao/                     (truy cập SQLite)
  ├─ model/                   (POJO/entity)
  └─ util/                    (DBConnection, Formatter, PdfExporter)
src/main/resources/
  ├─ fxml/        css/        reports/ (.jrxml)
```

---

## 9. Kỹ thuật cần lưu ý cho JavaFX

1. **Tách giao diện bằng FXML + Scene Builder.** Mỗi màn hình một file `.fxml` + một Controller; dùng `@FXML` để bind.
2. **Không chặn UI thread.** Mọi truy vấn DB/xuất PDF nặng phải chạy trong `Task` / `Service` và cập nhật UI qua `Platform.runLater()`. Nếu chạy SQLite trên JavaFX Application Thread sẽ gây "đơ" giao diện.
3. **Quản lý vòng đời kết nối DB.** Mở 1 `Connection` dùng lại (SQLite đơn người dùng) hoặc connection-per-operation gọn; luôn dùng `try-with-resources` cho `PreparedStatement`/`ResultSet`.
4. **TableView + ObservableList + property binding.** Dùng `SimpleStringProperty`/`SimpleIntegerProperty` trong model để bảng tự cập nhật khi dữ liệu đổi.
5. **Định dạng & validate input.** Dùng `TextFormatter` cho ô số tiền/số lượng (chỉ cho nhập số); format hiển thị tiền VND tập trung ở `util/Formatter`.
6. **Dialog dùng lại.** Tạo helper mở dialog thêm/sửa, hộp thoại xác nhận xóa, và **alert cảnh báo bán âm kho**.
7. **CSS tách riêng.** Dùng file `.css` cho giao diện nhất quán (màu, font, trạng thái cảnh báo đỏ khi tồn = 0).
8. **i18n/locale.** Set `Locale("vi","VN")` cho định dạng số/tiền/ngày.
9. **Đường dẫn DB ổn định khi đóng gói.** Lưu file `.db` ở thư mục dữ liệu người dùng (vd `%APPDATA%/ShopManager/`), **không** lưu trong thư mục cài đặt (tránh mất quyền ghi). Tự tạo DB + chạy script khởi tạo schema nếu chưa tồn tại.
10. **Đóng gói:** cấu hình module-path JavaFX; dùng `jlink` tạo runtime tối giản rồi `jpackage` ra `.exe`/`.msi`. Kiểm thử bản đóng gói trên máy sạch (không cài sẵn Java).
11. **An toàn tài chính:** dùng `BigDecimal` (hoặc số nguyên "đồng") cho tiền tệ — **không dùng `double`** để tránh sai số.
12. **Ghi log:** khởi tạo SLF4J + Logback ngay từ đầu; bắt mọi exception ở lớp Service và `log.error(...)`; cài đặt `Thread.setDefaultUncaughtExceptionHandler` để không "nuốt" lỗi nền. File log nên **xoay vòng** (rolling theo ngày/kích thước) để không phình to.

### Chiến lược ghi log (chi tiết)

- **Log lỗi (Logback → file):** lưu ở `%APPDATA%/ShopManager/logs/app.log`, rolling theo ngày, giữ ~30 ngày. Mức: `INFO` cho sự kiện chính, `ERROR` kèm stack trace cho lỗi. Mục đích: chẩn đoán khi app lỗi trên máy người dùng (không có server).
- **Log biến động tồn kho (`stock_movements` — MVP):** mỗi nghiệp vụ làm đổi tồn (nhập/bán/trả/kiểm kho) ghi 1 dòng: loại, số lượng thay đổi, **tồn sau thay đổi**, chứng từ liên quan, thời điểm. Cho phép truy ngược "vì sao tồn âm/sai".
- **Nhật ký hoạt động (`activity_log` — GĐ2):** ghi thao tác nhạy cảm (đổi giá, xóa SP, sửa hóa đơn) kèm giá trị trước/sau, có màn hình tra cứu.

---

## 10. Lộ trình triển khai (gợi ý)

1. **Hạ tầng:** Khởi tạo Maven + JavaFX + SQLite; `DBConnection`; script tạo schema; màn hình chính + điều hướng.
2. **Đăng nhập + Danh mục đơn vị/nhóm.**
3. **Quản lý hàng hóa** (CRUD + thiết lập giá).
4. **Nhập hàng** (cập nhật tồn + giá vốn).
5. **Bán hàng + xuất hóa đơn PDF** (luồng lõi).
6. **Khách hàng.**
7. **Dashboard** (biểu đồ ngày/giờ/thứ).
8. **Đóng gói `.exe`** bằng jpackage + kiểm thử máy sạch.
9. → Bắt đầu **Giai đoạn 2**.

---

## 11. Rủi ro & Lưu ý

- ⚠️ **Đóng gói jpackage trên Windows** có thể cần WiX Toolset và cấu hình module JavaFX — nên làm sớm để phát hiện vấn đề.
- ⚠️ **Sai số tiền tệ** nếu dùng `double` → bắt buộc `BigDecimal`/số nguyên.
- ⚠️ **Mất dữ liệu**: chỉ có 1 file DB cục bộ → khuyến nghị người dùng sao lưu định kỳ; ưu tiên làm backup tự động ở GĐ2.
- ⚠️ **Đường dẫn ghi DB** khi cài ở `Program Files` dễ bị chặn quyền ghi → lưu ở `%APPDATA%`.
- ⚠️ **Bán âm kho** có thể khiến tồn kho âm → cần báo cáo cảnh báo & cho phép điều chỉnh (kiểm kho ở GĐ2).
```
