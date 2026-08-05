# Spec: Bán hàng (POS — Point of Sale)

> Module: phase-1-mvp/ban-hang · Phiên bản: 1.2 · Ngày: 2026-07-08
> Tham chiếu: [MVP-plan.md](../../MVP-plan.md) · [architecture.md](../../architecture.md) · [database-schema.md](../../database-schema.md)

---

## 1. Executive Summary

Module **Bán hàng** là luồng nghiệp vụ **quan trọng nhất** của hệ thống — nơi tạo ra doanh thu. Chức năng cho phép chủ cửa hàng:

- Tìm kiếm và thêm sản phẩm vào giỏ hàng.
- Chỉnh số lượng, áp dụng giảm giá theo phần trăm (%).
- Chọn khách hàng (bắt buộc — mọi khách đều có thông tin trong hệ thống).
- Thanh toán **đầy đủ hoặc một phần** (ghi nợ), tự động trừ tồn kho, ghi log biến động kho.
- Sinh hóa đơn PDF A4 và **bật hộp thoại in của Windows** ngay sau khi thanh toán.
- **Thu nợ:** Ghi nhận khi khách quay lại trả tiền nợ cũ.

**Ràng buộc đã chốt:**
- Cho phép bán khi tồn kho = 0 hoặc âm (cảnh báo ngay khi thêm vào giỏ, nhưng không chặn).
- Giảm giá theo **phần trăm (%)** trên tổng hóa đơn.
- Tiền tệ: VND, xử lý bằng `long` (đơn vị đồng), **không dùng `double`**.
- **Ghi nợ** cho phép thanh toán một phần, phần còn lại ghi nợ cho khách.

---

## 2. Requirements & Use Cases

### 2.1 Functional Requirements

| ID     | Yêu cầu                                                      | Độ ưu tiên |
| ------ | ------------------------------------------------------------- | ---------- |
| FR-01  | Tìm kiếm sản phẩm theo mã SP hoặc tên SP                    | Bắt buộc   |
| FR-02  | Thêm SP vào giỏ bằng click chuột vào kết quả                 | Bắt buộc   |
| FR-03  | Chỉnh số lượng SP trong giỏ (gõ số hoặc +/−)                 | Bắt buộc   |
| FR-04  | Xóa từng dòng SP khỏi giỏ hàng                               | Bắt buộc   |
| FR-05  | Chọn khách hàng từ danh sách (bắt buộc chọn trước khi thanh toán) | Bắt buộc |
| FR-06  | Nhập giảm giá theo phần trăm (%) trên tổng đơn               | Bắt buộc   |
| FR-07  | Hiển thị tạm tính, tiền giảm, tổng cộng real-time            | Bắt buộc   |
| FR-08  | Nhập số tiền khách đưa, tính tiền thối hoặc còn nợ           | Bắt buộc   |
| FR-09  | Thanh toán: lưu hóa đơn, trừ tồn kho, ghi `stock_movements` | Bắt buộc   |
| FR-10  | Sinh PDF A4 và bật hộp thoại Print của Windows                | Bắt buộc   |
| FR-11  | Cảnh báo bán âm kho ngay khi thêm SP vào giỏ                 | Bắt buộc   |
| FR-12  | Hủy đơn hàng (xóa toàn bộ giỏ) với popup xác nhận           | Bắt buộc   |
| FR-13  | Xem danh sách hóa đơn đã tạo                                 | Bắt buộc   |
| FR-14  | **Ghi nợ:** Cho phép thanh toán một phần, ghi nợ phần còn lại | Bắt buộc |
| FR-15  | **Thu nợ:** Ghi nhận thanh toán nợ từ khách, cập nhật trạng thái hóa đơn | Bắt buộc |
| FR-16  | **Xem công nợ:** Hiển thị danh sách hóa đơn còn nợ theo khách hàng | Bắt buộc |

### 2.2 Non-functional Requirements

| ID      | Yêu cầu                                                                       |
| ------- | ------------------------------------------------------------------------------ |
| NFR-01  | Toàn bộ thao tác DB/PDF chạy trên background thread (không đơ UI)             |
| NFR-02  | Thanh toán phải atomic (1 transaction cho insert hóa đơn + trừ kho + ghi log) |
| NFR-03  | Tiền tệ xử lý bằng `long` (đồng), hiển thị format `1.000.000 đ`              |
| NFR-04  | Thời gian phản hồi tìm kiếm SP < 500ms (với vài nghìn SP)                     |

### 2.3 Use Cases

#### UC-04 — Bán hàng & Xuất hóa đơn (luồng lõi)

- **Actor:** Chủ cửa hàng
- **Tiền điều kiện:** Đã đăng nhập. Có ít nhất 1 sản phẩm trong hệ thống.

**Luồng chính:**

```
1.  Người dùng mở màn hình "Bán hàng" (POS) từ sidebar.
2.  Gõ mã SP hoặc tên SP vào ô tìm kiếm.
3.  Hệ thống hiển thị danh sách kết quả khớp (real-time khi gõ).
4.  Người dùng click chuột vào 1 sản phẩm trong kết quả.
5.  Hệ thống thêm 1 đơn vị SP đó vào giỏ hàng (bên phải).
    → Nếu SP đó đã có trong giỏ: tăng số lượng lên 1.
    → Nếu tồn kho SP ≤ 0: hiển thị cảnh báo inline
      "⚠️ [Tên SP] đã hết hàng (tồn: X). Vẫn cho phép bán."
      (cảnh báo không chặn thao tác, chỉ thông báo)
6.  Người dùng có thể chỉnh số lượng trong giỏ:
    - Bấm nút [+] / [−] hoặc
    - Gõ trực tiếp số lượng vào ô số.
7.  Người dùng chọn khách hàng từ dropdown (bắt buộc chọn trước khi thanh toán).
8.  Người dùng nhập giảm giá (%) — hệ thống tự tính tiền giảm.
9.  Hệ thống hiển thị real-time:
    - Tạm tính = Σ (đơn giá × số lượng)
    - Tiền giảm = Tạm tính × (% giảm giá / 100)
    - TỔNG CỘNG = Tạm tính − Tiền giảm
10. Người dùng nhập "Tiền khách đưa".
    → Hệ thống tính toán và hiển thị:
      - Nếu Tiền khách đưa ≥ Tổng cộng:
        Hiển thị "Tiền thối: X đ" (màu xanh)
      - Nếu Tiền khách đưa < Tổng cộng:
        Hiển thị "Còn nợ: X đ" (màu cam)
11. Người dùng bấm [Thanh toán & In PDF].
12. Hệ thống xử lý (trên background thread):
    a. BEGIN TRANSACTION
    b. Insert bản ghi `invoices` (kèm trạng thái PAID hoặc PARTIAL)
    c. Insert các bản ghi `invoice_items`
    d. Nếu thanh toán một phần → Insert bản ghi `payments` (ghi nhận lần trả đầu)
    e. Trừ `products.stock_qty` cho từng SP
    f. Insert bản ghi `stock_movements` (type=BAN) cho từng SP
    g. COMMIT (nếu lỗi → ROLLBACK toàn bộ)
13. Sinh file PDF hóa đơn A4 (có hiển thị trạng thái thanh toán / còn nợ).
14. Bật hộp thoại Print của Windows (người dùng chọn máy in → in).
15. Xóa giỏ hàng, sẵn sàng cho đơn tiếp theo.
```

**Luồng ngoại lệ:**

| Bước | Điều kiện                                        | Xử lý                                                        |
| ---- | ------------------------------------------------ | ------------------------------------------------------------- |
| 5    | Tồn kho SP ≤ 0                                  | Hiển thị cảnh báo inline, **vẫn cho thêm** vào giỏ           |
| 6    | Số lượng nhập ≤ 0                                | Không cho phép, giữ nguyên giá trị cũ                        |
| 8    | Giảm giá > 100% hoặc < 0%                       | Hiển thị lỗi validation, không cho nhập                       |
| 10   | Tiền khách đưa < Tổng cộng                      | ✅ Cho phép — ghi nợ phần còn lại cho khách hàng               |
| 11   | Giỏ hàng trống                                   | Disable nút "Thanh toán"                                      |
| 12   | Lỗi DB khi commit                               | ROLLBACK, hiển thị dialog lỗi, giữ nguyên giỏ để thử lại    |

#### UC-04b — Hủy đơn hàng

- **Luồng chính:**
  1. Người dùng bấm nút [Hủy đơn].
  2. Hệ thống hiện popup xác nhận: *"Bạn có chắc muốn hủy toàn bộ đơn hàng hiện tại?"*
  3. Người dùng bấm [Xác nhận] → Xóa sạch giỏ hàng, reset các trường giảm giá/tiền khách đưa.
  4. Người dùng bấm [Hủy] trên popup → Quay lại, giữ nguyên giỏ hàng.

#### UC-04c — Thu nợ

- **Actor:** Chủ cửa hàng
- **Tiền điều kiện:** Có ít nhất 1 hóa đơn ở trạng thái "Còn nợ" (PARTIAL).

**Luồng chính:**

```
1. Người dùng mở màn hình "Danh sách hóa đơn" hoặc "Công nợ khách hàng".
2. Lọc/tìm các hóa đơn có trạng thái "Còn nợ" (có thể lọc theo khách hàng).
3. Hệ thống hiển thị danh sách hóa đơn còn nợ, bao gồm:
   - Mã HĐ, ngày, tên khách, tổng tiền, đã trả, còn nợ.
4. Người dùng chọn 1 hóa đơn và bấm [Thu nợ].
5. Hệ thống hiện dialog "Thu nợ":
   - Hiển thị: Tổng HĐ, đã trả, còn nợ.
   - Ô nhập: "Số tiền thu" (mặc định = số tiền còn nợ).
6. Người dùng nhập số tiền thu (có thể trả một phần hoặc trả hết).
   → Validation: 0 < Số tiền thu ≤ Còn nợ.
7. Người dùng bấm [Xác nhận thu].
8. Hệ thống xử lý (trên background thread):
   a. BEGIN TRANSACTION
   b. Insert bản ghi `payments` (ghi nhận lần thanh toán)
   c. Cập nhật `invoices.paid` = tổng các lần thanh toán
   d. Nếu paid ≥ total → cập nhật `invoices.status` = 'PAID'
   e. COMMIT
9. Cập nhật lại danh sách hóa đơn trên màn hình.
```

**Luồng ngoại lệ:**

| Bước | Điều kiện                | Xử lý                                          |
| ---- | ------------------------ | ----------------------------------------------- |
| 6    | Số tiền thu ≤ 0          | Validation chặn, không cho nhập                 |
| 6    | Số tiền thu > Còn nợ     | Validation chặn, thông báo "Vượt quá số nợ"    |
| 8    | Lỗi DB khi commit       | ROLLBACK, hiển thị dialog lỗi                   |

---

## 3. UI/UX & Navigation

### 3.1 Vị trí trong Navigation Map

```
[Main Window] → Sidebar → 💵 Bán hàng
   ├── Tạo hóa đơn (POS)       ← MÀN HÌNH POS
   ├── Danh sách hóa đơn        ← Xem tất cả HĐ + Thu nợ
   └── Công nợ khách hàng       ← Tổng hợp nợ theo khách
```

### 3.2 Wireframe — Màn hình POS

```
┌───────────────────────────────────┬──────────────────────────┐
│  VÙNG TÌM KIẾM & KẾT QUẢ (trái)  │  VÙNG GIỎ HÀNG (phải)    │
│                                   │                          │
│ 🔍[ tìm mã/tên SP...          ]   │  Khách: [ Nguyễn A  ▼]  │
│                                   │                          │
│ ┌───────────────────────────────┐ │ ┌──────────────────────┐ │
│ │ SP001 · Coca 330ml   10.000đ │ │ │ Coca x2     20.000   │ │
│ │ SP002 · Gạo ST25     35.000đ │ │ │ Gạo  x1     35.000   │ │
│ │ SP003 · Mì Hảo Hảo    4.000đ │ │ │ Mì   x3     12.000   │ │
│ │         (click để thêm)       │ │ │   [−] [3] [+]    [✖] │ │
│ └───────────────────────────────┘ │ └──────────────────────┘ │
│                                   │                          │
│ ⚠️ "Mì Hảo Hảo" tồn = 0,          │ Tạm tính:      67.000 đ │
│    vẫn cho bán (âm kho)            │ Giảm giá:  [ 5 ] %      │
│                                   │ Tiền giảm:     −3.350 đ │
│                                   │ ───────────────────────  │
│                                   │ TỔNG:         63.650 đ  │
│                                   │ Khách đưa: [ 40.000  ]  │
│                                   │ 🟠 Còn nợ:    23.650 đ  │
│                                   │                          │
│                                   │ [  Hủy đơn  ] [Thanh toán & In PDF] │
└───────────────────────────────────┴──────────────────────────┘
```

> **Lưu ý hiển thị vùng thanh toán:**
> - Nếu `Tiền khách đưa ≥ Tổng` → Hiển thị `🟢 Tiền thối: X đ` (màu xanh)
> - Nếu `Tiền khách đưa < Tổng` → Hiển thị `🟠 Còn nợ: X đ` (màu cam)

### 3.3 Wireframe — Dialog Thu nợ

```
┌──────────────── Thu nợ ────────────────┐
│  Hóa đơn:    HD000123                 │
│  Khách hàng: Nguyễn Văn A              │
│  Ngày mua:   08/07/2026                │
│  ─────────────────────────────────────  │
│  Tổng HĐ:          63.650 đ            │
│  Đã trả:            40.000 đ            │
│  Còn nợ:            23.650 đ            │
│  ─────────────────────────────────────  │
│  Số tiền thu:  [ 23.650        ]        │
│                                         │
│              [ Hủy ]   [ Xác nhận thu ] │
└─────────────────────────────────────────┘
```

### 3.4 Wireframe — Màn hình Công nợ khách hàng

```
┌──────────────────────────────────────────────────────────────┐
│  Công nợ khách hàng      🔍[ tìm tên/SĐT... ]                │
├──────────────────────────────────────────────────────────────┤
│ Khách hàng     │ SĐT        │ Số HĐ nợ │ Tổng nợ    │ Thao tác │
│ Nguyễn Văn A   │ 0901234567 │    2     │ 150.000 đ  │ [Xem]    │
│ Trần Thị B     │ 0907654321 │    1     │  23.650 đ  │ [Xem]    │
├──────────────────────────────────────────────────────────────┤
│                                    TỔNG CÔNG NỢ: 173.650 đ    │
└──────────────────────────────────────────────────────────────┘

  [Xem] → mở danh sách hóa đơn còn nợ của khách đó → chọn HĐ → [Thu nợ]
```

### 3.5 Chi tiết các thành phần UI

| Thành phần               | Loại control                  | Hành vi                                                             |
| ------------------------ | ----------------------------- | ------------------------------------------------------------------- |
| Ô tìm kiếm              | `TextField`                   | Tìm real-time khi gõ (debounce ~300ms). Khớp mã hoặc tên.          |
| Danh sách kết quả tìm    | `ListView`                    | Hiển thị mã, tên, giá bán. Click → thêm vào giỏ.                  |
| Cảnh báo âm kho          | `Label` (style đỏ)            | Hiện inline phía dưới danh sách kết quả khi SP tồn ≤ 0.            |
| Giỏ hàng                 | `TableView`                   | Cột: Tên SP, SL (có nút +/−), Đơn giá, Thành tiền, Xóa.           |
| Nút [+] / [−]            | `Button`                      | Tăng/giảm SL từng 1. SL tối thiểu = 1 (bấm − khi SL=1 → không giảm). |
| Ô số lượng               | `TextField` + `TextFormatter` | Chỉ cho nhập số nguyên dương.                                       |
| Nút [✖] xóa dòng        | `Button`                      | Xóa SP khỏi giỏ (không cần confirm).                               |
| Dropdown Khách hàng      | `ComboBox`                    | Danh sách khách hàng. Bắt buộc chọn trước khi thanh toán.          |
| Ô giảm giá               | `TextField` + `TextFormatter` | Chỉ cho nhập số 0–100. Mặc định = 0.                               |
| Các label tạm tính/tổng   | `Label` (bind)                | Tự động cập nhật khi giỏ hàng hoặc % giảm giá thay đổi.            |
| Ô tiền khách đưa          | `TextField` + `TextFormatter` | Chỉ cho nhập số nguyên dương. Có thể = 0 (ghi nợ toàn bộ).        |
| Label tiền thối / còn nợ  | `Label` (bind + đổi màu)     | Xanh nếu thối, cam nếu nợ.                                        |
| Nút [Thanh toán & In PDF] | `Button`                      | Disabled khi: giỏ trống HOẶC chưa chọn khách hàng.                |
| Nút [Hủy đơn]            | `Button`                      | Hiện popup xác nhận trước khi xóa giỏ.                             |
| Nút [Thu nợ]              | `Button`                      | Hiện dialog thu nợ cho hóa đơn đang chọn (trong danh sách HĐ).    |

### 3.6 Wireframe — Hóa đơn PDF A4

```
┌──────────────── HÓA ĐƠN BÁN HÀNG ────────────────┐
│  CỬA HÀNG [Tên từ settings]      Số HĐ: HD000123  │
│  Địa chỉ / SĐT [từ settings]     Ngày: 08/07/2026  │
│  Khách hàng: Nguyễn Văn A        SĐT: 0901234567   │
├────────────────────────────────────────────────────┤
│ STT │ Tên hàng    │ ĐVT │ SL │ Đơn giá  │ T.Tiền  │
│  1  │ Coca 330ml  │ lon │ 2  │  10.000  │  20.000 │
│  2  │ Gạo ST25    │ kg  │ 1  │  35.000  │  35.000 │
│  3  │ Mì Hảo Hảo  │ gói │ 3  │   4.000  │  12.000 │
├────────────────────────────────────────────────────┤
│                        Tạm tính:         67.000 đ   │
│                        Giảm giá (5%):    −3.350 đ   │
│                        TỔNG CỘNG:        63.650 đ   │
│                        Đã thanh toán:    40.000 đ   │
│                        CÒN NỢ:           23.650 đ   │
│  (Bằng chữ: Sáu mươi ba nghìn sáu trăm năm mươi đồng) │
│                                                     │
│   Người mua hàng              Người bán hàng        │
└────────────────────────────────────────────────────┘
```

> Nếu thanh toán đủ thì **không hiển thị** dòng "Đã thanh toán" và "Còn nợ".

---

## 4. Data Models & State

### 4.1 Bảng Database liên quan

```sql
-- Hóa đơn
invoices (
    id            INTEGER PRIMARY KEY AUTOINCREMENT,
    code          TEXT    NOT NULL UNIQUE,       -- "HD000001", tự sinh
    customer_id   INTEGER NOT NULL REFERENCES customers(id),  -- bắt buộc chọn khách
    invoice_date  TEXT    NOT NULL,              -- ISO 8601
    subtotal      INTEGER NOT NULL,              -- tạm tính (đồng)
    discount_pct  REAL    NOT NULL DEFAULT 0,    -- % giảm giá (0–100)
    discount_amt  INTEGER NOT NULL DEFAULT 0,    -- tiền giảm (đồng)
    total         INTEGER NOT NULL,              -- tổng cộng = subtotal - discount_amt
    paid          INTEGER NOT NULL DEFAULT 0,    -- tổng tiền đã trả (đồng) — tổng hợp từ payments
    debt          INTEGER NOT NULL DEFAULT 0,    -- còn nợ (đồng) = total - paid
    status        TEXT    NOT NULL DEFAULT 'PAID' -- 'PAID' | 'PARTIAL' | 'UNPAID'
)

-- Chi tiết hóa đơn
invoice_items (
    id            INTEGER PRIMARY KEY AUTOINCREMENT,
    invoice_id    INTEGER NOT NULL REFERENCES invoices(id),
    product_id    INTEGER NOT NULL REFERENCES products(id),
    qty           INTEGER NOT NULL,              -- số lượng
    sale_price    INTEGER NOT NULL,              -- đơn giá tại thời điểm bán (đồng)
    amount        INTEGER NOT NULL               -- thành tiền = qty × sale_price
)

-- Lịch sử thanh toán (MỚI — hỗ trợ ghi nợ & thu nợ)
payments (
    id            INTEGER PRIMARY KEY AUTOINCREMENT,
    invoice_id    INTEGER NOT NULL REFERENCES invoices(id),
    amount        INTEGER NOT NULL,              -- số tiền thanh toán lần này (đồng)
    payment_date  TEXT    NOT NULL,              -- ISO 8601
    note          TEXT                            -- ghi chú (vd: "Thu nợ lần 2")
)

-- Log biến động kho
stock_movements (
    id            INTEGER PRIMARY KEY AUTOINCREMENT,
    product_id    INTEGER NOT NULL REFERENCES products(id),
    type          TEXT    NOT NULL,              -- 'BAN'
    qty_change    INTEGER NOT NULL,              -- số âm (vd: -3)
    stock_after   INTEGER NOT NULL,              -- tồn kho sau thay đổi
    ref_type      TEXT    NOT NULL,              -- 'INVOICE'
    ref_id        INTEGER NOT NULL,              -- invoices.id
    created_at    TEXT    NOT NULL,
    note          TEXT
)
```

### 4.2 Quy tắc trạng thái hóa đơn (status)

```
┌──────────┐    Thanh toán đủ      ┌──────────┐
│  (mới)   │ ──────────────────▶  │   PAID   │
└──────────┘                      └──────────┘
     │                                  ▲
     │ Thanh toán một phần              │ Thu nợ (paid ≥ total)
     ▼                                  │
┌──────────┐    Thu nợ (một phần)  ┌──────────┐
│  UNPAID  │ ◀────────────────── │ PARTIAL  │
└──────────┘ ──────────────────▶ └──────────┘
              Thu nợ (một phần)
```

| Trạng thái | Điều kiện                | Mô tả                       |
| ---------- | ------------------------ | ---------------------------- |
| `PAID`     | `paid >= total`          | Đã thanh toán đủ             |
| `PARTIAL`  | `0 < paid < total`      | Đã trả một phần, còn nợ     |
| `UNPAID`   | `paid = 0 AND total > 0` | Chưa trả đồng nào (ghi nợ toàn bộ) |

### 4.3 State Changes

**Khi Thanh toán (UC-04):**

```
1. INSERT → invoices            (status = PAID | PARTIAL | UNPAID tùy tiền khách đưa)
2. INSERT → invoice_items       (N bản ghi)
3. INSERT → payments            (1 bản ghi — lần thanh toán đầu tiên, nếu paid > 0)
4. UPDATE → products            SET stock_qty = stock_qty - qty
5. INSERT → stock_movements     (N bản ghi, type='BAN')
```

> **Tất cả 5 bước trên nằm trong 1 TRANSACTION.** Lỗi ở bất kỳ bước nào → ROLLBACK toàn bộ.

**Khi Thu nợ (UC-04c):**

```
1. INSERT → payments            (1 bản ghi — ghi nhận lần trả tiền)
2. UPDATE → invoices            SET paid = (SELECT SUM(amount) FROM payments WHERE invoice_id = ?),
                                    debt = total - paid,
                                    status = CASE WHEN paid >= total THEN 'PAID' ELSE 'PARTIAL' END
```

> Thu nợ cũng nằm trong **1 TRANSACTION**.

### 4.4 ViewModel State (PosViewModel)

```java
// State chính
ObservableList<CartItem> cartItems;        // giỏ hàng
ObjectProperty<Customer> selectedCustomer; // khách hàng đang chọn
DoubleProperty discountPercent;            // % giảm giá (0–100)
LongProperty paidAmount;                   // tiền khách đưa

// Computed (binding tự động)
LongProperty subtotal;        // = Σ cartItem.amount
LongProperty discountAmount;  // = subtotal * discountPercent / 100
LongProperty total;           // = subtotal - discountAmount
LongProperty debtAmount;      // = max(0, total - paidAmount)
LongProperty changeAmount;    // = max(0, paidAmount - total)
BooleanProperty isDebt;       // = paidAmount < total
BooleanProperty canCheckout;  // = cartItems.size > 0
                              //   AND selectedCustomer != null
```

```java
// CartItem
class CartItem {
    Product product;
    IntegerProperty quantity;       // mặc định = 1
    LongProperty amount;            // = product.salePrice * quantity
    boolean isNegativeStock;        // true nếu tồn kho ≤ 0
}
```

---

## 5. Integration & Architecture

### 5.1 Layer Flow (theo kiến trúc MVVM-lite)

```
┌─────────┐    ┌────────────────┐    ┌─────────────┐    ┌──────────────────┐    ┌──────────────┐
│  View   │───▶│ PosController  │───▶│PosViewModel │───▶│  SalesService    │───▶│    DAOs       │
│ (FXML)  │    │ (bind + event) │    │ (state+cmd) │    │ (tx + business)  │    │ (SQL thuần)   │
└─────────┘    └────────────────┘    └─────────────┘    └──────────────────┘    └──────────────┘
                                                                                  │
                                                              ┌─────────────────┐ │
                                                              │  PdfExporter    │ │
                                                              │  (JasperReports)│ │
                                                              └─────────────────┘
```

### 5.2 Các class liên quan

| Layer      | Class                | Trách nhiệm                                                    |
| ---------- | -------------------- | --------------------------------------------------------------- |
| View       | `pos-view.fxml`      | Layout màn hình POS (2 cột: tìm kiếm / giỏ hàng)              |
| View       | `invoice-list.fxml`  | Danh sách hóa đơn + nút thu nợ                                 |
| View       | `debt-overview.fxml` | Tổng hợp công nợ theo khách hàng                                |
| Controller | `PosController`      | Bind FXML controls ↔ ViewModel, xử lý sự kiện UI               |
| Controller | `InvoiceListController` | Hiển thị danh sách HĐ, mở dialog thu nợ                     |
| Controller | `DebtOverviewController` | Hiển thị công nợ, điều hướng đến HĐ cụ thể                |
| ViewModel  | `PosViewModel`       | State giỏ hàng, computed totals + debt, gọi `SalesService`     |
| Service    | `SalesService`       | `createInvoice()` — orchestrate DAOs trong 1 transaction        |
| Service    | `PaymentService`     | `collectPayment()` — thu nợ, cập nhật trạng thái HĐ **(MỚI)** |
| Service    | `ProductService`     | `searchProducts(keyword)` — tìm kiếm SP                        |
| DAO        | `InvoiceDao`         | `insert()`, `updatePaid()`, `findByStatus()`, `findByCustomer()` |
| DAO        | `InvoiceItemDao`     | `insertAll(items)` — lưu chi tiết hóa đơn                      |
| DAO        | `PaymentDao`         | `insert()`, `findByInvoiceId()` **(MỚI)**                      |
| DAO        | `ProductDao`         | `decreaseStock(productId, qty)` — trừ tồn kho                  |
| DAO        | `StockMovementDao`   | `insert(movement)` — ghi log biến động kho                      |
| Util       | `PdfExporter`        | Sinh PDF A4 từ template `.jrxml` + data                         |

### 5.3 Threading Model

```
[JavaFX App Thread]
    │
    ├── Tìm kiếm SP: ProductService.search() → background Task
    │     └── Platform.runLater() → cập nhật ListView
    │
    ├── Thanh toán: SalesService.createInvoice() → background Task
    │     ├── DB transaction (insert invoice + items + payment + update stock + log)
    │     ├── PdfExporter.generate()
    │     └── Platform.runLater() → bật PrintDialog + xóa giỏ
    │
    └── Thu nợ: PaymentService.collectPayment() → background Task
          ├── DB transaction (insert payment + update invoice status)
          └── Platform.runLater() → refresh danh sách HĐ
```

### 5.4 Module phụ thuộc

- **Đầu vào từ module khác:**
  - `products` (từ Quản lý hàng hóa) — tìm kiếm và lấy giá bán.
  - `customers` (từ Khách hàng) — chọn khách cho hóa đơn (bắt buộc).
  - `settings` — thông tin cửa hàng in lên PDF.

- **Đầu ra cho module khác:**
  - `invoices` + `invoice_items` → Dashboard đọc để tính doanh thu.
  - `payments` → Dashboard có thể đọc để tính dòng tiền thực thu.
  - `stock_movements` → truy vết biến động tồn kho.

---

## 6. Edge Cases & Risks

### 6.1 Edge Cases

| #   | Tình huống                                          | Xử lý                                                              |
| --- | --------------------------------------------------- | ------------------------------------------------------------------- |
| E1  | Bán SP có tồn kho = 0 hoặc âm                      | Cảnh báo inline ngay khi thêm vào giỏ. Vẫn cho bán.                |
| E2  | Giảm giá = 100%                                     | Cho phép (tổng = 0). Tiền khách đưa có thể = 0. Status = PAID.     |
| E3  | Giảm giá > 100% hoặc < 0%                          | Validation chặn, không cho nhập.                                    |
| E4  | Thêm cùng 1 SP nhiều lần                            | Không tạo dòng mới, chỉ tăng SL của dòng hiện có.                  |
| E5  | Giỏ hàng trống khi bấm Thanh toán                   | Nút Thanh toán bị disabled.                                        |
| E6  | Tiền khách đưa = 0, chọn khách cụ thể               | ✅ Cho phép — ghi nợ toàn bộ. Status = UNPAID.                     |
| E7  | Chưa chọn khách hàng khi bấm Thanh toán            | 🔴 Disable nút thanh toán cho đến khi chọn khách.                  |
| E8  | SP bị xóa/ẩn sau khi đã thêm vào giỏ               | Kiểm tra lại tại thời điểm thanh toán. Nếu không hợp lệ → báo lỗi. |
| E9  | Giá SP thay đổi sau khi đã thêm vào giỏ            | Giá trong giỏ là snapshot tại thời điểm thêm. Không tự cập nhật.   |
| E10 | Mã hóa đơn trùng lặp                               | Mã HD tự sinh (auto-increment format), DB có UNIQUE constraint.    |
| E11 | Transaction thất bại giữa chừng                     | ROLLBACK toàn bộ. Hiện dialog lỗi. Giữ nguyên giỏ hàng.           |
| E12 | Thu nợ vượt quá số tiền còn nợ                      | Validation chặn: Số tiền thu ≤ debt.                                |
| E13 | Thu nợ cho HĐ đã ở trạng thái PAID                  | Nút "Thu nợ" bị ẩn/disabled cho HĐ đã thanh toán đủ.              |
| E14 | Khách hàng bị xóa/ẩn nhưng vẫn còn HĐ nợ          | Không cho ẩn/xóa khách còn công nợ.                                 |

### 6.2 Risks & Mitigations

| Rủi ro                                         | Mức độ     | Giảm thiểu                                                     |
| ----------------------------------------------- | ---------- | --------------------------------------------------------------- |
| Tồn kho âm lan rộng không kiểm soát            | Trung bình | Ghi `stock_movements` mọi biến động + Kiểm kho ở GĐ2           |
| Sai số tiền tệ nếu dùng `double`               | Cao        | Bắt buộc dùng `long` (đồng). Làm tròn khi tính % giảm giá.    |
| Gọi DB trên UI thread gây đơ giao diện         | Cao        | Mọi nghiệp vụ chạy trong `javafx.concurrent.Task`               |
| PDF không in được (không có máy in)             | Thấp       | Hộp thoại Print cho phép hủy. PDF cũng được lưu file.           |
| Mất điện giữa lúc thanh toán                   | Thấp       | Transaction atomic — hoặc commit hết, hoặc rollback hết.        |
| Công nợ tích tụ, khó theo dõi                  | Trung bình | Màn hình "Công nợ khách hàng" tổng hợp nợ; cảnh báo nợ lớn.   |
| `paid` trong invoices lệch với tổng `payments` | Trung bình | Luôn tính `paid = SUM(payments.amount)` trong transaction.       |

---

## 7. Decision Log (Quyết định đã chốt trong spec này)

| #  | Quyết định                                         | Lý do                                                |
| -- | -------------------------------------------------- | ---------------------------------------------------- |
| D1 | Thêm SP bằng click chuột (không Enter)             | Rõ ràng, tránh nhầm lẫn khi tìm kiếm                |
| D2 | Không có nút "Thêm nhanh khách hàng" trên POS      | Thông tin khách sẽ được chuẩn bị trước                |
| D3 | Giảm giá theo % (không theo tiền mặt)              | Đơn giản, phù hợp thực tế                            |
| D4 | Cảnh báo âm kho hiện ngay khi thêm SP              | Người dùng biết sớm, chủ động quyết định              |
| D5 | Bật hộp thoại Print sau thanh toán                  | Nhanh, tiện cho việc in trực tiếp                     |
| D6 | SL mặc định = 1, chỉnh bằng +/− hoặc gõ số        | Thao tác nhanh, linh hoạt                             |
| D7 | Hủy đơn có popup xác nhận                          | Tránh mất dữ liệu giỏ hàng do bấm nhầm              |
| D8 | **Không có "Khách lẻ" — mọi khách đều có thông tin trong hệ thống** | Nghiệp vụ bên ngoài đảm bảo khách được nhập trước  |
| D9 | **Thanh toán một phần (trả bao nhiêu cũng được)**  | Linh hoạt, phù hợp thực tế cửa hàng nhỏ              |
| D10| **Thu nợ nằm trong MVP (không đợi GĐ2)**           | Đảm bảo vòng đời công nợ khép kín ngay từ đầu       |
