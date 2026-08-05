# Spec: Khách hàng

> Module: phase-1-mvp/khach-hang · Phiên bản: 1.0 · Ngày: 2026-07-13
> Tham chiếu: [MVP-plan.md](../../MVP-plan.md) · [architecture.md](../../architecture.md) · [database-schema.md](../../database-schema.md)

---

## 1. Executive Summary

[To be written]

---

## 2. Requirements & Use Cases

### 2.1 Functional Requirements

| ID       | Yêu cầu                                                                              | Độ ưu tiên |
| -------- | ------------------------------------------------------------------------------------ | ---------- |
| FR-KH01  | Xem danh sách khách hàng (cuộn vô tận hoặc phân trang)                              | Bắt buộc   |
| FR-KH02  | Tìm kiếm real-time theo Tên / SĐT / Mã KH                                           | Bắt buộc   |
| FR-KH03  | Lọc danh sách theo trạng thái: Đang hoạt động / Đã ẩn                               | Bắt buộc   |
| FR-KH04  | Thêm khách hàng mới — tự động sinh Mã KH (format `KHxxx`)                           | Bắt buộc   |
| FR-KH05  | Sửa thông tin khách hàng (tất cả trường, trừ Mã KH)                                 | Bắt buộc   |
| FR-KH06  | Validate SĐT: bắt buộc, đúng 10 chữ số, không trùng với KH khác                    | Bắt buộc   |
| FR-KH07  | Validate Email: tùy chọn, kiểm tra format nếu nhập                                  | Bắt buộc   |
| FR-KH08  | Xóa mềm khách hàng (ẩn khỏi danh sách, giữ nguyên data trong DB)                   | Bắt buộc   |
| FR-KH09  | Chặn xóa nếu KH còn công nợ (`debt > 0`)                                            | Bắt buộc   |
| FR-KH10  | Khi KH bị xóa mềm có HĐ cũ đã PAID: giữ nguyên dữ liệu HĐ, hiển thị tên KH là `[KH đã ẩn]` tại các màn hình tra cứu | Bắt buộc   |
| FR-KH11  | Xem thông tin chi tiết 1 khách hàng                                                  | Bắt buộc   |
| FR-KH12  | Xem danh sách hóa đơn của KH (mã HĐ, ngày, tổng tiền, trạng thái)                  | Bắt buộc   |
| FR-KH13  | Xem tổng hợp KH: tổng số đơn, tổng doanh thu, tổng nợ còn lại                      | Bắt buộc   |
| FR-KH14  | Import danh sách KH từ file Excel / CSV                                              | Bắt buộc   |
| FR-KH15  | Export danh sách KH ra file Excel / CSV                                              | Bắt buộc   |

### 2.2 Non-functional Requirementsz

| ID        | Yêu cầu                                                                                    |
| --------- | ------------------------------------------------------------------------------------------ |
| NFR-KH01  | Tìm kiếm real-time phản hồi < 300ms (debounce 200ms, với vài nghìn KH)                   |
| NFR-KH02  | Toàn bộ thao tác DB (thêm/sửa/xóa/import) chạy trên background thread — không block UI   |
| NFR-KH03  | Import phải validate từng dòng và báo lỗi cụ thể (dòng bao nhiêu, lỗi gì) trước khi lưu |
| NFR-KH04  | Mã KH tự sinh đảm bảo không trùng lặp (dùng DB sequence / auto-increment + format)        |

### 2.3 Use Cases

#### UC-KH01 — Xem & Tìm kiếm danh sách khách hàng

- **Actor:** Chủ cửa hàng
- **Tiền điều kiện:** Đã đăng nhập.

**Luồng chính:**

```
1. Người dùng mở màn hình "Khách hàng" từ sidebar.
2. Hệ thống hiển thị danh sách KH đang hoạt động (is_active = 1), sắp xếp theo Mã KH tăng dần.
3. Người dùng gõ từ khóa vào ô tìm kiếm.
4. Hệ thống lọc real-time (debounce 200ms), khớp với Tên / SĐT / Mã KH.
5. Kết quả hiển thị ngay trên danh sách.
```

**Luồng ngoại lệ:**

| Bước | Điều kiện            | Xử lý                                             |
| ---- | -------------------- | ------------------------------------------------- |
| 4    | Không tìm thấy KH   | Hiển thị trạng thái rỗng: "Không tìm thấy khách hàng nào" |

---

#### UC-KH02 — Thêm khách hàng mới

- **Actor:** Chủ cửa hàng
- **Tiền điều kiện:** Đã đăng nhập.

**Luồng chính:**

```
1. Người dùng bấm nút [+ Thêm khách hàng].
2. Hệ thống mở form dialog "Thêm khách hàng".
3. Hệ thống tự động sinh Mã KH tiếp theo (ví dụ: KH001, KH002...).
4. Người dùng điền thông tin:
   - Họ tên (*): bắt buộc
   - Số điện thoại (*): bắt buộc, 10 chữ số
   - Email: tùy chọn
   - Ngày sinh: tùy chọn
   - Giới tính: tùy chọn (Nam / Nữ / Khác)
   - Địa chỉ: tùy chọn
   - Ghi chú: tùy chọn
5. Người dùng bấm [Lưu].
6. Hệ thống validate → lưu DB → thêm KH vào danh sách → hiện thông báo thành công.
```

**Luồng ngoại lệ:**

| Bước | Điều kiện                         | Xử lý                                                       |
| ---- | --------------------------------- | ----------------------------------------------------------- |
| 5    | Họ tên để trống                   | Báo lỗi inline: "Họ tên không được để trống"                |
| 5    | SĐT không đủ 10 chữ số           | Báo lỗi inline: "Số điện thoại phải có đúng 10 chữ số"     |
| 5    | SĐT đã tồn tại trong hệ thống    | Báo lỗi inline: "Số điện thoại đã được đăng ký"            |
| 5    | Email sai format (nếu có nhập)    | Báo lỗi inline: "Email không hợp lệ"                        |

---

#### UC-KH03 — Sửa thông tin khách hàng

- **Actor:** Chủ cửa hàng
- **Tiền điều kiện:** Khách hàng đang ở trạng thái hoạt động.

**Luồng chính:**

```
1. Người dùng chọn 1 KH trong danh sách → bấm [Sửa] (hoặc double-click).
2. Hệ thống mở form dialog "Sửa khách hàng" — điền sẵn dữ liệu hiện tại.
3. Mã KH hiển thị nhưng bị disabled (không cho sửa).
4. Người dùng chỉnh sửa các trường cần thiết.
5. Người dùng bấm [Lưu].
6. Hệ thống validate → cập nhật DB → refresh dòng KH trên danh sách.
```

**Luồng ngoại lệ:** Tương tự UC-KH02 (validate SĐT, Email).

---

#### UC-KH04 — Xóa mềm khách hàng

- **Actor:** Chủ cửa hàng
- **Tiền điều kiện:** KH không còn công nợ (`debt = 0`).

**Luồng chính:**

```
1. Người dùng chọn KH → bấm [Xóa].
2. Hệ thống kiểm tra: KH không còn công nợ (debt = 0).
3. Hệ thống hiện popup xác nhận:
   - Nếu KH chưa có HĐ: "Bạn có chắc muốn ẩn khách hàng [Tên KH]?"
   - Nếu KH có HĐ đã PAID: "Khách hàng [Tên KH] có [N] hóa đơn đã thanh toán.
     Sau khi ẩn, các hóa đơn cũ sẽ hiển thị tên '[KH đã ẩn]'. Tiếp tục?"
4. Người dùng bấm [Xác nhận].
5. Hệ thống set is_active = 0 → KH biến mất khỏi danh sách mặc định.
   Các hóa đơn cũ giữ nguyên customer_id, hiển thị tên '[KH đã ẩn]' tại
   màn hình danh sách HĐ và chi tiết HĐ.
```

**Luồng ngoại lệ:**

| Bước | Điều kiện              | Xử lý                                                             |
| ---- | ---------------------- | ----------------------------------------------------------------- |
| 2    | KH còn công nợ        | Hiện dialog lỗi: "Không thể xóa — khách hàng còn nợ [X đ]"       |
| 3    | Người dùng bấm [Hủy] | Đóng popup, giữ nguyên trạng thái KH                              |

---

#### UC-KH05 — Xem chi tiết khách hàng & lịch sử mua hàng

- **Actor:** Chủ cửa hàng

**Luồng chính:**

```
1. Người dùng chọn 1 KH → bấm [Xem chi tiết].
2. Hệ thống mở màn hình chi tiết KH, gồm 2 vùng:
   Vùng trên — Thông tin KH: Mã KH, Họ tên, SĐT, Email, Ngày sinh, Địa chỉ, Ghi chú.
   Vùng tổng hợp — 3 chỉ số: Tổng số đơn | Tổng doanh thu | Tổng còn nợ.
   Vùng dưới — Danh sách hóa đơn: Mã HĐ, Ngày, Tổng tiền, Đã trả, Còn nợ, Trạng thái.
3. Người dùng có thể sắp xếp danh sách HĐ theo ngày / tổng tiền.
```

---

#### UC-KH06 — Import danh sách khách hàng

- **Actor:** Chủ cửa hàng

**Luồng chính:**

```
1. Người dùng bấm [Import].
2. Hệ thống mở hộp thoại chọn file (*.xlsx, *.csv).
3. Người dùng chọn file.
4. Hệ thống đọc và validate toàn bộ dữ liệu:
   - Kiểm tra các cột bắt buộc (Họ tên, SĐT).
   - Kiểm tra format SĐT từng dòng (phải đúng 10 chữ số).
   - Kiểm tra SĐT trùng trong file (dòng sau trùng với dòng trước).
   - Kiểm tra SĐT trùng với KH đã có trong DB.
5. Hiện màn hình preview kết quả validate:
   - Tổng số dòng hợp lệ / tổng số dòng.
   - Danh sách dòng lỗi gồm: số dòng + lý do cụ thể
     (ví dụ: "Dòng 5: SĐT '090123456' đã tồn tại trong hệ thống").
6. Người dùng xem xét và bấm [Xác nhận import].
7. Hệ thống chỉ import các dòng hợp lệ → tự sinh Mã KH → lưu DB.
8. Hiện thông báo kết quả: "Đã import X khách hàng. Bỏ qua Y dòng lỗi."
```

**Luồng ngoại lệ:**

| Bước | Điều kiện                        | Xử lý                                                    |
| ---- | -------------------------------- | -------------------------------------------------------- |
| 3    | File sai định dạng / không đọc được | Hiện lỗi: "File không hợp lệ. Vui lòng chọn .xlsx hoặc .csv" |
| 4    | Toàn bộ dòng đều lỗi            | Không cho import, hiện thông báo danh sách lỗi           |

---

#### UC-KH07 — Export danh sách khách hàng

- **Actor:** Chủ cửa hàng

**Luồng chính:**

```
1. Người dùng bấm [Export].
2. Hệ thống mở hộp thoại chọn vị trí lưu file và tên file (mặc định: danh-sach-khach-hang.xlsx).
3. Hệ thống xuất đúng danh sách KH đang hiển thị theo filter/tìm kiếm hiện tại ra file Excel/CSV.
   (Ví dụ: đang lọc "Đã ẩn" → export ra danh sách KH đã ẩn)
4. Hiện thông báo: "Đã xuất file thành công tại [đường dẫn]."
```

---

## 3. UI/UX & Navigation

### 3.1 Vị trí trong Navigation Map

```
[Main Window] → Sidebar → 👤 Khách hàng
   └── Danh sách khách hàng              ← MÀN HÌNH CHÍNH
         ├── [+ Thêm] → Dialog Thêm KH
         ├── [Sửa]    → Dialog Sửa KH
         └── [Chi tiết] → Màn hình Chi tiết KH
```

### 3.2 Wireframe — Danh sách khách hàng

```
┌─────────────────────────────────────────────────────────────────────┐
│  KHÁCH HÀNG                                                          │
│  🔍[ tìm tên / SĐT / mã KH... ]  [Trạng thái ▼]  [+ Thêm] [Import] [Export] │
├──────────┬──────────────────┬─────────────┬──────────────┬──────────┬───────────────────┐
│ Mã KH    │ Họ tên           │ SĐT         │ Email        │ Địa chỉ  │ Thao tác          │
├──────────┼──────────────────┼─────────────┼──────────────┼──────────┼───────────────────┤
│ KH001    │ Nguyễn Văn A     │ 0901234567  │ a@mail.com   │ Q.1 HCM  │ [Chi tiết] [Sửa] [Xóa] │
│ KH002    │ Trần Thị B       │ 0907654321  │ —            │ Q.3 HCM  │ [Chi tiết] [Sửa] [Xóa] │
│ KH003    │ Lê Văn C (đã ẩn) │ 0912345678  │ —            │ —        │ [Chi tiết] [Khôi phục]  │
└──────────┴──────────────────┴─────────────┴──────────────┴──────────┴───────────────────┘
```

> **Lưu ý:**
> - Mặc định filter = "Đang hoạt động" — chỉ hiện KH `is_active = 1`.
> - Filter "Đã ẩn" → hiện KH `is_active = 0`, thao tác đổi thành `[Chi tiết]` `[Khôi phục]`.
> - Nút `[Xóa]` chỉ hiển thị với KH đang hoạt động.

### 3.3 Wireframe — Dialog Thêm / Sửa khách hàng

```
┌──────────────────────── Thêm khách hàng ────────────────────────────┐
│                                                                       │
│  Mã KH (tự động)   [ KH004  — readonly ]                             │
│  ─────────────────────────────────────────────────────────────────── │
│  Họ tên *          [ Nguyễn Văn D              ]                     │
│                                                                       │
│  ┌─────────────────────────────┐  ┌──────────────────────────────┐   │
│  │ Số điện thoại *             │  │ Email                        │   │
│  │ [ 0901234567             ]  │  │ [ example@mail.com        ]  │   │
│  │ ⚠ SĐT đã được đăng ký      │  │                              │   │
│  └─────────────────────────────┘  └──────────────────────────────┘   │
│                                                                       │
│  ┌─────────────────────────────┐  ┌──────────────────────────────┐   │
│  │ Ngày sinh                   │  │ Giới tính                    │   │
│  │ [ 01/01/1990  📅 ]          │  │ ( ) Nam  ( ) Nữ  ( ) Khác   │   │
│  └─────────────────────────────┘  └──────────────────────────────┘   │
│                                                                       │
│  Địa chỉ           [ 123 Đường ABC, Quận 1, TP.HCM           ]      │
│                                                                       │
│  Ghi chú           [ Khách VIP, ưu tiên giao hàng...          ]      │
│                                                                       │
│  ──────────────────────────────────────────────────────────────────  │
│                                         [ Hủy ]  [ 💾 Lưu ]         │
└───────────────────────────────────────────────────────────────────────┘
```

> **Lưu ý:**
> - Lỗi validation hiện **inline** ngay dưới trường bị lỗi (không dùng popup).
> - Khi **Sửa**: tiêu đề đổi thành "Sửa khách hàng", Mã KH luôn readonly.
> - Nút `[Lưu]` disabled khi form đang có lỗi validation.

### 3.4 Wireframe — Màn hình Chi tiết khách hàng

```
┌─────────────────────────────────────────────────────────────────────┐
│  ← Quay lại                    CHI TIẾT KHÁCH HÀNG       [Sửa thông tin] │
├─────────────────────────────────────────────────────────────────────┤
│  KH001 · Nguyễn Văn A                                               │
│  📞 0901234567   ✉ a@mail.com   📍 Quận 1, TP.HCM                  │
│  🎂 01/01/1990 (36 tuổi)  •  Nam  •  Ghi chú: Khách VIP            │
├────────────────────┬───────────────────────┬────────────────────────┤
│   Tổng số đơn      │   Tổng doanh thu       │   Còn nợ               │
│      12            │    4.500.000 đ          │    150.000 đ           │
├─────────────────────────────────────────────────────────────────────┤
│  LỊCH SỬ MUA HÀNG                                                    │
├────────────┬───────────────┬────────────┬──────────┬────────┬────────┤
│ Mã HĐ     │ Ngày          │ Tổng tiền  │ Đã trả   │ Còn nợ │ Trạng thái │
├────────────┼───────────────┼────────────┼──────────┼────────┼────────┤
│ HD000123  │ 08/07/2026    │ 500.000 đ  │ 350.000  │150.000 │🟠 Còn nợ │
│ HD000089  │ 01/06/2026    │ 200.000 đ  │ 200.000  │     0  │🟢 Đã trả │
│ ...       │ ...           │ ...        │ ...      │ ...    │ ...    │
└────────────┴───────────────┴────────────┴──────────┴────────┴────────┘
```

> **Lưu ý:**
> - Danh sách HĐ mặc định sắp xếp theo ngày mới nhất → cũ nhất.
> - Click vào header cột Ngày / Tổng tiền để đổi chiều sắp xếp.
> - Badge trạng thái: 🟢 PAID, 🟠 PARTIAL, 🔴 UNPAID.

### 3.5 Chi tiết các thành phần UI

| Thành phần              | Loại control                   | Hành vi                                                               |
| ----------------------- | ------------------------------ | --------------------------------------------------------------------- |
| Ô tìm kiếm              | `TextField`                    | Debounce 200ms. Khớp Tên / SĐT / Mã KH (case-insensitive).           |
| Dropdown Trạng thái     | `ComboBox`                     | 3 lựa chọn: Đang hoạt động / Đã ẩn / Tất cả. Mặc định: Đang hoạt động. |
| Bảng danh sách KH       | `TableView`                    | Cuộn vô tận. Click dòng để chọn.                                      |
| Nút `[+ Thêm]`          | `Button`                       | Mở dialog Thêm KH.                                                    |
| Nút `[Import]`          | `Button`                       | Mở FileChooser → xử lý file → mở màn hình preview validate.           |
| Nút `[Export]`          | `Button`                       | Mở FileChooser lưu file → export danh sách hiện tại.                  |
| Nút `[Sửa]`             | `Button` (mỗi dòng)            | Mở dialog Sửa KH, điền sẵn dữ liệu.                                  |
| Nút `[Xóa]`             | `Button` (mỗi dòng)            | Kiểm tra điều kiện → hiện popup xác nhận → soft-delete.               |
| Nút `[Chi tiết]`        | `Button` (mỗi dòng)            | Điều hướng sang màn hình Chi tiết KH.                                  |
| Nút `[Khôi phục]`       | `Button` (KH đã ẩn)            | Set `is_active = 1`, KH xuất hiện lại ở filter mặc định.             |
| Dialog form Thêm/Sửa   | `Dialog` + `GridPane`          | Modal. Validate inline. Nút Lưu disabled khi có lỗi.                  |
| Ô SĐT                   | `TextField` + `TextFormatter`  | Chỉ nhận chữ số, tối đa 10 ký tự.                                    |
| DatePicker Ngày sinh    | `DatePicker`                   | Format `dd/MM/yyyy`. Không cho chọn ngày tương lai.                   |
| RadioButton Giới tính   | `RadioButton` + `ToggleGroup`  | Nam / Nữ / Khác. Mặc định không chọn.                                |
| Stat cards Chi tiết KH  | `Label` (3 cái)                | Load trên background thread khi mở màn hình Chi tiết.                 |

---

## 4. Data Models & State

### 4.1 Bảng Database — `customers`

```sql
customers (
    id            INTEGER PRIMARY KEY AUTOINCREMENT,
    code          TEXT    NOT NULL UNIQUE,       -- "KH001", tự sinh sau khi insert
    name          TEXT    NOT NULL,              -- Họ tên
    phone         TEXT    NOT NULL UNIQUE,       -- SĐT (10 chữ số)
    email         TEXT,                          -- nullable
    date_of_birth TEXT,                          -- ISO 8601, nullable (vd: "1990-01-01")
    gender        TEXT,                          -- 'MALE' | 'FEMALE' | 'OTHER' | NULL
    address       TEXT,                          -- nullable
    note          TEXT,                          -- nullable
    is_active     INTEGER NOT NULL DEFAULT 1,    -- 1 = hoạt động, 0 = đã ẩn (soft delete)
    created_at    TEXT    NOT NULL               -- ISO 8601
)
```

**Index:**
```sql
CREATE UNIQUE INDEX idx_customers_phone ON customers(phone);
CREATE UNIQUE INDEX idx_customers_code  ON customers(code);
CREATE INDEX        idx_customers_name  ON customers(name);  -- hỗ trợ tìm kiếm
```

### 4.2 Logic sinh Mã KH

Mã KH được sinh **sau khi INSERT** (dùng `last_insert_rowid()`) để đảm bảo không trùng lặp ngay cả khi có rollback:

```
Bước 1: INSERT INTO customers (...) VALUES (...)       -- code để NULL tạm
Bước 2: id = last_insert_rowid()
Bước 3: code = "KH" + String.format("%03d", id)       -- KH001, KH002 ... KH999
         (nếu id > 999: code = "KH" + id)             -- KH1000, KH1001...
Bước 4: UPDATE customers SET code = ? WHERE id = ?
```

> Cả 4 bước nằm trong **1 TRANSACTION** — rollback thì code không bị chiếm.

### 4.3 State Changes

**Khi Thêm KH (UC-KH02):**
```
1. BEGIN TRANSACTION
2. INSERT → customers          (code = NULL tạm)
3. UPDATE → customers          SET code = 'KHxxx'
4. COMMIT
```

**Khi Sửa KH (UC-KH03):**
```
UPDATE → customers  SET name, phone, email, date_of_birth, gender, address, note
                    WHERE id = ?
```
> Không đổi `code`, `is_active`, `created_at`.

**Khi Xóa mềm (UC-KH04):**
```
UPDATE → customers  SET is_active = 0  WHERE id = ?
```

**Khi Khôi phục:**
```
UPDATE → customers  SET is_active = 1  WHERE id = ?
```

### 4.4 ViewModel State

```java
// CustomerListViewModel
ObservableList<Customer> allCustomers;       // toàn bộ KH đã load
FilteredList<Customer>   filteredCustomers;  // kết quả sau filter + search
StringProperty           searchKeyword;      // bind với ô tìm kiếm
ObjectProperty<CustomerStatus> filterStatus; // ACTIVE | INACTIVE | ALL

// CustomerFormViewModel (dùng chung cho Thêm và Sửa)
StringProperty  name;
StringProperty  phone;
StringProperty  email;
ObjectProperty<LocalDate> dateOfBirth;
ObjectProperty<Gender>    gender;
StringProperty  address;
StringProperty  note;

// Computed
BooleanProperty isValid;     // = name không rỗng AND phone hợp lệ AND email hợp lệ
BooleanProperty isEditMode;  // true = đang Sửa, false = đang Thêm mới
```

```java
// Customer (model)
class Customer {
    int     id;
    String  code;           // "KH001"
    String  name;
    String  phone;
    String  email;          // nullable
    LocalDate dateOfBirth;  // nullable
    Gender  gender;         // MALE | FEMALE | OTHER | null
    String  address;        // nullable
    String  note;           // nullable
    boolean isActive;
    String  createdAt;
}

enum Gender { MALE, FEMALE, OTHER }
enum CustomerStatus { ACTIVE, INACTIVE, ALL }
```

---

## 5. Integration & Architecture

### 5.1 Layer Flow (theo kiến trúc MVVM-lite)

```
┌─────────┐    ┌──────────────────────┐    ┌──────────────────────┐    ┌────────────────┐    ┌────────────┐
│  View   │───▶│ CustomerController   │───▶│ CustomerListViewModel │───▶│ CustomerService│───▶│ CustomerDao│
│ (FXML)  │    │ (bind + event)       │    │ CustomerFormViewModel │    │ (business)     │    │ (SQL thuần)│
└─────────┘    └──────────────────────┘    └──────────────────────┘    └────────────────┘    └────────────┘
                                                                               │
                                                               ┌───────────────┤
                                                               │               │
                                                      ┌────────────────┐  ┌──────────────┐
                                                      │ ImportService  │  │ ExportService│
                                                      │ (Excel/CSV)    │  │ (Excel/CSV)  │
                                                      └────────────────┘  └──────────────┘
```

### 5.2 Các class liên quan

| Layer      | Class                          | Trách nhiệm                                                           |
| ---------- | ------------------------------ | --------------------------------------------------------------------- |
| View       | `customer-list.fxml`           | Danh sách KH: toolbar + TableView                                     |
| View       | `customer-form-dialog.fxml`    | Dialog Thêm/Sửa KH (dùng chung)                                      |
| View       | `customer-detail.fxml`         | Chi tiết KH: thông tin + stat cards + lịch sử HĐ                    |
| View       | `import-preview.fxml`          | Màn hình preview kết quả validate file import                        |
| Controller | `CustomerListController`       | Bind danh sách, tìm kiếm, filter, mở dialog/chi tiết                 |
| Controller | `CustomerFormController`       | Bind form Thêm/Sửa, validate inline, gọi Service                     |
| Controller | `CustomerDetailController`     | Hiển thị thông tin chi tiết + load lịch sử HĐ                       |
| ViewModel  | `CustomerListViewModel`        | State danh sách + `FilteredList` + search/filter logic                |
| ViewModel  | `CustomerFormViewModel`        | State form + validation + `isValid` computed                          |
| Service    | `CustomerService`              | `add()`, `update()`, `softDelete()`, `restore()`, `checkDeletable()` |
| Service    | `ImportService`                | Đọc file Excel/CSV, validate từng dòng, trả về `ImportResult`        |
| Service    | `ExportService`                | Xuất `List<Customer>` ra file Excel/CSV                               |
| DAO        | `CustomerDao`                  | `insert()`, `update()`, `findAll()`, `findByFilter()`, `setActive()`  |

### 5.3 Threading Model

```
[JavaFX App Thread]
    │
    ├── Tìm kiếm / Filter: FilteredList.setPredicate() → trên UI thread (in-memory, nhanh)
    │
    ├── Load danh sách KH: CustomerService.findAll() → background Task
    │     └── Platform.runLater() → cập nhật ObservableList
    │
    ├── Thêm / Sửa / Xóa / Khôi phục: CustomerService → background Task
    │     └── Platform.runLater() → refresh danh sách + đóng dialog
    │
    ├── Import: ImportService.validate() + insert batch → background Task
    │     ├── Báo tiến độ (ProgressIndicator) khi đang đọc file lớn
    │     └── Platform.runLater() → hiện màn hình preview kết quả
    │
    └── Export: ExportService.export() → background Task
          └── Platform.runLater() → hiện dialog chọn nơi lưu file
```

### 5.4 Module phụ thuộc

- **Đầu ra cho module khác:**
  - `customers` → Module **Bán hàng** đọc để chọn khách khi tạo hóa đơn (`ComboBox`).
  - `customers` → Module **Dashboard** đọc để tổng hợp công nợ.
  - Khi KH bị soft-delete → Module Bán hàng hiển thị tên `[KH đã ẩn]` trên hóa đơn cũ.

- **Phụ thuộc thư viện:**
  - Import/Export Excel: **Apache POI** (`poi-ooxml`).
  - Import/Export CSV: **OpenCSV** hoặc xử lý thủ công.

---

## 6. Edge Cases & Risks

### 6.1 Edge Cases

| #   | Tình huống                                              | Xử lý                                                                          |
| --- | ------------------------------------------------------- | ------------------------------------------------------------------------------ |
| E1  | Thêm KH với SĐT đã tồn tại trong DB                   | Báo lỗi inline: "Số điện thoại đã được đăng ký"                                |
| E2  | Sửa KH đổi sang SĐT đang dùng bởi KH khác             | Báo lỗi inline: "Số điện thoại đã được đăng ký"                                |
| E3  | Xóa mềm KH còn công nợ                                 | Chặn, hiện dialog lỗi "Không thể xóa — khách hàng còn nợ [X đ]"               |
| E4  | Xóa mềm KH có HĐ đã PAID (không nợ)                  | Cho phép, hiện popup cảnh báo có N HĐ cũ sẽ hiển thị "[KH đã ẩn]"             |
| E5  | KH đã bị ẩn xuất hiện trong ComboBox chọn KH (POS)     | Không hiển thị KH `is_active = 0` trong ComboBox của màn hình Bán hàng          |
| E6  | Import file có dòng trùng SĐT với nhau trong chính file | Dòng sau bị đánh dấu lỗi "SĐT trùng với dòng X trong file", bỏ qua dòng đó   |
| E7  | Import file rỗng (0 dòng dữ liệu)                      | Hiện thông báo: "File không có dữ liệu để import"                               |
| E8  | Import file có cột thiếu (không có cột Họ tên hoặc SĐT)| Hiện lỗi ngay: "File không đúng định dạng — thiếu cột bắt buộc"                |
| E9  | Export khi danh sách rỗng (không có KH nào theo filter) | Hiện thông báo: "Không có dữ liệu để xuất"                                     |
| E10 | Tìm kiếm với từ khóa không có kết quả                  | Hiển thị trạng thái rỗng: "Không tìm thấy khách hàng nào"                      |
| E11 | Nhập Ngày sinh trong tương lai                          | DatePicker chặn, không cho chọn ngày > hôm nay                                 |
| E12 | Họ tên chỉ toàn khoảng trắng                           | Trim trước khi validate — coi là rỗng, báo lỗi "Họ tên không được để trống"    |
| E13 | Mã KH bị gap (vd: KH003 sau rollback không tồn tại)    | Chấp nhận gap — Mã KH chỉ cần unique, không cần liên tục                       |

### 6.2 Risks & Mitigations

| Rủi ro                                               | Mức độ     | Giảm thiểu                                                                |
| ---------------------------------------------------- | ---------- | ------------------------------------------------------------------------- |
| Import file lớn (nghìn dòng) gây đơ UI              | Trung bình | Chạy trên background Task, hiện ProgressIndicator                         |
| SĐT trùng do race condition (2 user cùng thêm)       | Thấp       | UNIQUE constraint trên DB — sẽ throw exception, bắt và báo lỗi            |
| Mất dữ liệu KH do xóa nhầm                          | Trung bình | Soft delete — data vẫn còn, có [Khôi phục]. Popup xác nhận trước khi xóa |
| HĐ cũ mất tên KH khi KH bị ẩn                       | Thấp       | Giữ `customer_id` trong `invoices`, hiển thị "[KH đã ẩn]" thay vì NULL   |
| File import CSV có encoding khác UTF-8               | Thấp       | Detect encoding hoặc ghi rõ trong hướng dẫn: "Lưu file CSV dạng UTF-8"   |

---

## 7. Decision Log

| #  | Quyết định                                                           | Lý do                                                              |
| -- | -------------------------------------------------------------------- | ------------------------------------------------------------------ |
| D1 | Soft delete thay vì hard delete                                      | Bảo toàn lịch sử HĐ, tránh mất dữ liệu do thao tác nhầm          |
| D2 | Chỉ chặn xóa KH còn công nợ — cho phép xóa KH có HĐ đã PAID        | Linh hoạt, danh sách KH không phình to; HĐ cũ vẫn còn đủ dữ liệu |
| D3 | Tên KH bị ẩn hiển thị "[KH đã ẩn]" trên HĐ cũ                      | Rõ ràng cho người dùng, không bị NULL/lỗi khi tra cứu HĐ          |
| D4 | SĐT bắt buộc, UNIQUE — không có KH không có SĐT                     | SĐT là định danh thực tế, tránh trùng KH                           |
| D5 | Mã KH tự sinh sau INSERT (dùng `last_insert_rowid()`)                | Tránh race condition khi sinh mã trước khi insert                  |
| D6 | Tìm kiếm thực hiện in-memory trên `FilteredList` (không query DB)    | Nhanh hơn, đủ tốt cho vài nghìn KH; giảm round-trip DB            |
| D7 | Import chỉ thêm mới — không cho update đè lên KH cũ khi trùng SĐT  | An toàn, tránh ghi đè dữ liệu ngoài ý muốn                        |
| D8 | Export theo đúng danh sách đang filter/tìm kiếm hiện tại            | Trực quan — người dùng thấy gì thì export ra đó                    |
