# Spec: Mua hàng

> Module: phase-1-mvp/mua-hang · Phiên bản: 1.0 · Ngày: 2026-07-19
> Tham chiếu: [MVP-plan.md](../../MVP-plan.md) · [architecture.md](../../architecture.md) · [ban-hang/spec.md](../ban-hang/spec.md)

---

## 1. Executive Summary

Module **Mua hàng** quản lý toàn bộ quy trình nhập hàng vào kho, bao gồm quản lý nhà cung cấp (NCC) và theo dõi công nợ phải trả.

Đây là module **nền tảng cho tính chính xác lợi nhuận** — mỗi lần nhập hàng tạo ra một **Lô hàng (Batch)** với giá vốn riêng. Khi bán, hệ thống sử dụng thuật toán **FIFO** để xác định lô nào được xuất trước, đảm bảo tính chính xác 100% cho giá vốn hàng bán (COGS) và lợi nhuận, **không có bất kỳ sai số nào dù giá vốn biến động theo từng lần nhập**.

Chức năng cho phép chủ cửa hàng:

- **Quản lý phiếu nhập:** Tạo phiếu nhập với nhiều dòng sản phẩm, xem danh sách, xem chi tiết.
- **Theo dõi Lô hàng (Batch Tracking):** Mỗi dòng trong phiếu nhập → 1 lô hàng (`inventory_batches`) với giá vốn riêng; tồn kho lô giảm dần khi bán theo FIFO.
- **Quản lý NCC (optional):** CRUD nhà cung cấp; phiếu nhập có thể gắn hoặc không gắn NCC.
- **Công nợ phải trả:** Thanh toán từng phần hoặc toàn bộ cho NCC ngay khi tạo phiếu hoặc qua chức năng "Trả nợ NCC" sau này.

**Ràng buộc đã chốt:**
- `products.cost_price` bị **bỏ hoàn toàn** — giá vốn chỉ tồn tại trên từng lô (`inventory_batches.cost_price`).
- NCC là **optional** — phiếu nhập có thể không gắn nhà cung cấp.
- Thuật toán xuất kho: **FIFO mặc định** (lô nhập trước bán trước), người dùng có thể override tại POS.
- **Không có trả nhập hàng** trong MVP — thuộc Giai đoạn 2.

> **Lưu ý phạm vi:** MVP-plan.md ban đầu xếp "Quản lý NCC" vào Giai đoạn 2. Quyết định đưa vào MVP vì cần thiết để theo dõi công nợ phải trả ngay từ đầu.

---

## 2. Requirements & Use Cases

### 2.1 Functional Requirements

| ID       | Yêu cầu                                                                                    | Độ ưu tiên |
| -------- | ------------------------------------------------------------------------------------------ | ---------- |
| FR-MH01  | Xem danh sách phiếu nhập (phân trang / cuộn vô tận)                                       | Bắt buộc   |
| FR-MH02  | Tìm kiếm phiếu nhập theo Mã phiếu / Tên NCC                                               | Bắt buộc   |
| FR-MH03  | Lọc phiếu nhập theo trạng thái: Tất cả / Đã trả / Còn nợ                                  | Bắt buộc   |
| FR-MH04  | Tạo phiếu nhập hàng mới với nhiều dòng sản phẩm                                           | Bắt buộc   |
| FR-MH05  | Mỗi dòng SP trong phiếu nhập tự động tạo 1 Lô hàng với giá vốn riêng                     | Bắt buộc   |
| FR-MH06  | Sau khi lưu phiếu: tăng `products.stock_qty`, ghi `stock_movements` (type=NHAP)           | Bắt buộc   |
| FR-MH07  | Nhập "Tiền đã trả NCC" khi tạo phiếu (trả đủ / một phần / chưa trả)                      | Bắt buộc   |
| FR-MH08  | Xem chi tiết phiếu nhập: thông tin + danh sách dòng SP + lịch sử thanh toán NCC          | Bắt buộc   |
| FR-MH09  | Trả nợ NCC: ghi nhận thanh toán bổ sung cho phiếu nhập còn nợ                             | Bắt buộc   |
| FR-MH10  | Xem tổng hợp công nợ phải trả theo NCC                                                     | Bắt buộc   |
| FR-MH11  | Quản lý NCC: Thêm / Sửa / Xóa mềm / Khôi phục                                            | Bắt buộc   |
| FR-MH12  | Tìm kiếm NCC theo Tên / SĐT / Mã NCC                                                      | Bắt buộc   |
| FR-MH13  | FIFO tự động khi bán hàng; user có thể override chọn lô thủ công tại POS                  | Bắt buộc   |

### 2.2 Non-functional Requirements

| ID        | Yêu cầu                                                                                              |
| --------- | ---------------------------------------------------------------------------------------------------- |
| NFR-MH01  | Toàn bộ thao tác DB chạy trên background thread — không block UI                                    |
| NFR-MH02  | Tạo phiếu nhập phải atomic: tăng tồn kho + tạo lô + ghi log trong 1 transaction                    |
| NFR-MH03  | FIFO algorithm phân bổ đúng ngay cả khi tồn kho được nhập từ nhiều phiếu, nhiều ngày khác nhau     |
| NFR-MH04  | Mã phiếu nhập tự sinh (PN001, PN002...) đảm bảo không trùng (dùng `last_insert_rowid()`)           |
| NFR-MH05  | SUM(inventory_batches.qty_remaining) cho mỗi SP phải luôn = products.stock_qty (invariant kho)      |

### 2.3 Use Cases

#### UC-MH01 — Xem & Tìm kiếm danh sách phiếu nhập

- **Actor:** Chủ cửa hàng
- **Tiền điều kiện:** Đã đăng nhập.

**Luồng chính:**

```
1. Người dùng mở màn hình "Mua hàng" từ sidebar.
2. Hệ thống hiển thị danh sách phiếu nhập, sắp xếp theo ngày mới nhất.
3. Người dùng có thể:
   - Gõ từ khóa vào ô tìm kiếm (Mã phiếu / Tên NCC).
   - Chọn filter trạng thái (Tất cả / Đã trả / Còn nợ).
4. Danh sách cập nhật real-time (debounce 200ms).
```

**Luồng ngoại lệ:**

| Bước | Điều kiện          | Xử lý                                      |
| ---- | ------------------ | ------------------------------------------ |
| 4    | Không tìm thấy    | Hiển thị: "Không tìm thấy phiếu nhập nào" |

---

#### UC-MH02 — Tạo phiếu nhập hàng

- **Actor:** Chủ cửa hàng
- **Tiền điều kiện:** Đã đăng nhập. Có ít nhất 1 sản phẩm trong hệ thống.

**Luồng chính:**

```
1.  Người dùng bấm [+ Tạo phiếu nhập].
2.  Hệ thống mở màn hình "Tạo phiếu nhập mới" (full-page):
    - Mã phiếu: tự sinh (PN001...)
    - Ngày nhập: mặc định hôm nay
    - Nhà cung cấp: tùy chọn (ComboBox — có thể bỏ trống)
    - Ghi chú: tùy chọn
    - Danh sách dòng sản phẩm: ban đầu rỗng
3.  Người dùng bấm [+ Thêm dòng] để thêm sản phẩm:
    a. Chọn sản phẩm từ ComboBox (tìm theo tên/mã).
    b. Nhập số lượng (> 0).
    c. Nhập giá nhập — giá vốn của lô này (> 0).
    d. Thành tiền = SL × Giá nhập (tính tự động, readonly).
4.  Người dùng thêm nhiều dòng tùy ý (lặp bước 3).
5.  Hệ thống hiển thị TỔNG NHẬP = Σ Thành tiền (cập nhật real-time).
6.  Người dùng nhập "Tiền đã trả NCC":
    - Mặc định = TỔNG NHẬP (đã thanh toán đủ).
    - Có thể sửa xuống thấp hơn (trả một phần) hoặc về 0 (chưa trả).
    - Hệ thống hiển thị: Còn nợ NCC = TỔNG NHẬP − Tiền đã trả.
7.  Người dùng bấm [💾 Lưu phiếu nhập].
8.  Hệ thống xử lý (trên background thread):
    a. BEGIN TRANSACTION
    b. INSERT → purchases               (1 bản ghi, code = NULL tạm)
    c. UPDATE purchases SET code = 'PNxxx'
    d. FOR EACH dòng sản phẩm:
         INSERT → inventory_batches     (1 lô, qty_remaining = qty)
         INSERT → purchase_items        (liên kết phiếu ↔ lô)
         UPDATE products SET stock_qty += qty
         INSERT → stock_movements       (type=NHAP, qty_change=+qty, ref=purchase)
    e. IF tiền đã trả > 0:
         INSERT → supplier_payments     (1 bản ghi)
    f. COMMIT
9.  Hiện thông báo: "Đã lưu phiếu nhập [PN001] thành công."
10. Chuyển về danh sách phiếu nhập, highlight dòng vừa tạo.
```

**Luồng ngoại lệ:**

| Bước | Điều kiện                      | Xử lý                                                       |
| ---- | ------------------------------ | ----------------------------------------------------------- |
| 7    | Không có dòng nào trong phiếu | Chặn lưu: "Phiếu nhập phải có ít nhất 1 sản phẩm"          |
| 7    | Số lượng dòng nào đó ≤ 0      | Lỗi inline: "Số lượng phải lớn hơn 0"                       |
| 7    | Giá nhập dòng nào đó ≤ 0      | Lỗi inline: "Giá nhập phải lớn hơn 0"                       |
| 7    | Tiền đã trả > Tổng nhập        | Lỗi inline: "Tiền trả không được vượt quá tổng phiếu nhập" |
| 8    | Lỗi DB → ROLLBACK              | Hiển thị dialog lỗi, giữ nguyên form để thử lại            |

---

#### UC-MH03 — Xem chi tiết phiếu nhập

- **Actor:** Chủ cửa hàng

**Luồng chính:**

```
1. Người dùng chọn phiếu nhập → bấm [Xem chi tiết].
2. Hệ thống mở màn hình Chi tiết phiếu nhập:
   Vùng đầu:   Mã phiếu · Ngày nhập · NCC (nếu có) · Ghi chú · Trạng thái.
   Vùng giữa:  Danh sách dòng SP: Tên SP | SL | Giá nhập | Thành tiền | Mã lô.
   Vùng tổng:  3 chỉ số: Tổng nhập | Đã trả | Còn nợ.
   Vùng dưới:  Lịch sử thanh toán: Ngày | Số tiền | Ghi chú.
3. Nếu phiếu nhập còn nợ (status ≠ PAID): hiển thị nút [Trả nợ NCC].
```

---

#### UC-MH04 — Trả nợ NCC

- **Actor:** Chủ cửa hàng
- **Tiền điều kiện:** Phiếu nhập có `status = PARTIAL` hoặc `UNPAID`.

**Luồng chính:**

```
1. Người dùng bấm [Trả nợ NCC] từ màn hình Chi tiết phiếu nhập
   HOẶC từ màn hình "Công nợ phải trả".
2. Hệ thống hiện dialog "Trả nợ NCC":
   - Hiển thị: Tổng phiếu, Đã trả, Còn nợ.
   - Ô nhập: "Số tiền trả" (mặc định = số còn nợ).
   - Ô nhập: "Ghi chú" (tùy chọn).
3. Người dùng nhập số tiền trả (0 < Số tiền trả ≤ Còn nợ).
4. Người dùng bấm [Xác nhận].
5. Hệ thống xử lý (background thread):
   a. BEGIN TRANSACTION
   b. INSERT → supplier_payments
   c. UPDATE → purchases SET
        paid   = (SELECT SUM(amount) FROM supplier_payments WHERE purchase_id = ?),
        debt   = total_cost - paid,
        status = CASE WHEN paid >= total_cost THEN 'PAID' ELSE 'PARTIAL' END
   d. COMMIT
6. Đóng dialog, refresh màn hình Chi tiết phiếu nhập.
```

**Luồng ngoại lệ:**

| Bước | Điều kiện        | Xử lý                           |
| ---- | ---------------- | ------------------------------- |
| 3    | Số tiền ≤ 0      | Chặn, báo lỗi inline           |
| 3    | Số tiền > Còn nợ | Chặn: "Vượt quá số nợ hiện tại" |
| 5    | Lỗi DB           | ROLLBACK, hiển thị dialog lỗi  |

---

#### UC-MH05 — Quản lý nhà cung cấp (CRUD)

- **Actor:** Chủ cửa hàng

**Luồng chính (Thêm NCC):**

```
1. Người dùng vào Mua hàng → Nhà cung cấp → bấm [+ Thêm NCC].
2. Hệ thống mở dialog "Thêm nhà cung cấp":
   - Mã NCC: tự sinh (NCC001...), readonly.
   - Tên (*): bắt buộc.
   - SĐT: tùy chọn.
   - Địa chỉ: tùy chọn.
   - Ghi chú: tùy chọn.
3. Người dùng điền thông tin → bấm [Lưu].
4. Hệ thống validate → lưu DB → NCC xuất hiện trong danh sách.
```

**Xóa mềm NCC:**
- Kiểm tra: NCC không còn phiếu nhập chưa thanh toán (`debt > 0`).
- Nếu còn nợ: chặn, hiện thông báo lỗi.
- Nếu không nợ: cho phép xóa mềm (`is_active = 0`). Phiếu nhập cũ hiển thị NCC là `[NCC đã ẩn]`.

---

## 3. UI/UX & Navigation

### 3.1 Vị trí trong Navigation Map

```
[Main Window] → Sidebar → 🛒 Mua hàng
   ├── Danh sách phiếu nhập        ← MÀN HÌNH CHÍNH
   │     ├── [+ Tạo phiếu nhập]   → Màn hình Tạo phiếu nhập (full-page)
   │     └── [Xem chi tiết]       → Màn hình Chi tiết phiếu nhập
   ├── Công nợ phải trả            ← Tổng hợp nợ theo NCC
   └── Nhà cung cấp               ← Quản lý NCC (CRUD)
```

### 3.2 Wireframe — Danh sách phiếu nhập

```
┌───────────────────────────────────────────────────────────────────────┐
│  MUA HÀNG                                                               │
│  🔍[ tìm mã phiếu / NCC... ]  [Trạng thái ▼]   [+ Tạo phiếu nhập]    │
├──────────┬─────────────┬──────────────────┬───────────┬───────┬────────┤
│ Mã phiếu │ Ngày nhập   │ Nhà cung cấp     │ Tổng nhập │Còn nợ │Thao tác│
├──────────┼─────────────┼──────────────────┼───────────┼───────┼────────┤
│ PN001    │ 01/07/2026  │ Cty TNHH Bia ABC │ 2.200.000 │   0   │🟢 [Xem]│
│ PN002    │ 05/07/2026  │ (Không có)       │   700.000 │300.000│🟠 [Xem]│
│ PN003    │ 10/07/2026  │ NCC Gạo XYZ      │ 1.500.000 │1.500K │🔴 [Xem]│
└──────────┴─────────────┴──────────────────┴───────────┴───────┴────────┘
```

> **Chú giải trạng thái:**
> - 🟢 `PAID` — Đã thanh toán đủ.
> - 🟠 `PARTIAL` — Đã trả một phần, còn nợ.
> - 🔴 `UNPAID` — Chưa trả đồng nào.

### 3.3 Wireframe — Màn hình Tạo phiếu nhập

```
┌────────────────────────────────────────────────────────────────────────┐
│  ← Quay lại                   TẠO PHIẾU NHẬP MỚI                       │
│                                                                          │
│  Mã phiếu (tự động) [ PN004 — readonly ]  Ngày nhập: [ 19/07/2026 📅 ] │
│  Nhà cung cấp       [ (Chọn NCC...)   ▼ ] [+ Thêm NCC]  ← optional    │
│  Ghi chú            [ Nhập hàng tháng 7...             ]               │
│                                                                          │
│  DANH SÁCH HÀNG NHẬP                                                    │
├──────────────────────────┬──────────┬──────────────┬──────────────┬─────┤
│ Sản phẩm                 │ Số lượng │ Giá nhập (đ) │ Thành tiền   │     │
├──────────────────────────┼──────────┼──────────────┼──────────────┼─────┤
│ [ Coca 330ml         ▼ ] │ [ 100  ] │ [  7.000   ] │   700.000 đ  │ [✖] │
│ [ Gạo ST25           ▼ ] │ [  50  ] │ [ 30.000   ] │ 1.500.000 đ  │ [✖] │
├──────────────────────────┴──────────┴──────────────┴──────────────┴─────┤
│  [+ Thêm dòng sản phẩm]                                                  │
│                                                                          │
│                             TỔNG NHẬP:       2.200.000 đ                │
│                             Tiền đã trả NCC: [ 2.200.000   ]            │
│                             Còn nợ NCC:             0 đ                 │
│                                                                          │
│                             [ Hủy ]   [ 💾 Lưu phiếu nhập ]             │
└──────────────────────────────────────────────────────────────────────────┘
```

> **Lưu ý:**
> - Mỗi dòng SP khi lưu → tạo **1 Lô hàng riêng** trong `inventory_batches` với giá vốn của dòng đó.
> - "Tiền đã trả" mặc định = Tổng nhập. Có thể sửa thấp hơn hoặc về 0.
> - Nút `[Lưu]` disabled khi: không có dòng nào, hoặc bất kỳ dòng nào có lỗi validation.

### 3.4 Wireframe — Màn hình Chi tiết phiếu nhập

```
┌──────────────────────────────────────────────────────────────────────────┐
│  ← Quay lại         CHI TIẾT PHIẾU NHẬP — PN002          [Trả nợ NCC]   │
├──────────────────────────────────────────────────────────────────────────┤
│  Ngày nhập: 05/07/2026   NCC: (Không có)   Ghi chú: —                   │
│  Trạng thái: 🟠 Còn nợ một phần                                           │
├──────────────────────┬──────┬────────────┬────────────┬──────────────────┤
│ Sản phẩm             │  SL  │ Giá nhập   │ Thành tiền │ Mã lô            │
├──────────────────────┼──────┼────────────┼────────────┼──────────────────┤
│ Coca 330ml           │ 100  │   7.000 đ  │  700.000 đ │ BATCH-0042       │
├──────────────────────────────────────────────────────────────────────────┤
│  TỔNG NHẬP: 700.000 đ    ĐÃ TRẢ: 400.000 đ    CÒN NỢ: 300.000 đ        │
├──────────────────────────────────────────────────────────────────────────┤
│  LỊCH SỬ THANH TOÁN                                                       │
├──────────────────────────┬────────────────┬──────────────────────────────┤
│ Ngày                     │ Số tiền        │ Ghi chú                      │
├──────────────────────────┼────────────────┼──────────────────────────────┤
│ 05/07/2026               │    400.000 đ   │ Đặt cọc khi nhận hàng        │
└──────────────────────────┴────────────────┴──────────────────────────────┘
```

> Nút `[Trả nợ NCC]` chỉ hiển thị khi `status ≠ PAID`.

### 3.5 Wireframe — Dialog Trả nợ NCC

```
┌────────────────── Trả nợ NCC ───────────────────┐
│  Phiếu nhập:    PN002                             │
│  Nhà cung cấp:  (Không có)                        │
│  Ngày nhập:     05/07/2026                         │
│  ─────────────────────────────────────────────    │
│  Tổng phiếu:       700.000 đ                      │
│  Đã trả:           400.000 đ                      │
│  Còn nợ:           300.000 đ                      │
│  ─────────────────────────────────────────────    │
│  Số tiền trả: [ 300.000             ]              │
│  Ghi chú:     [ Trả nốt tiền hàng... ]             │
│                                                   │
│               [ Hủy ]   [ ✓ Xác nhận ]            │
└───────────────────────────────────────────────────┘
```

### 3.6 Wireframe — Công nợ phải trả (tổng hợp theo NCC)

```
┌──────────────────────────────────────────────────────────────┐
│  Công nợ phải trả         🔍[ tìm NCC... ]                    │
├──────────────────────────────────────────────────────────────┤
│ Nhà cung cấp     │ SĐT         │ Số phiếu nợ │ Tổng phải trả │
│ Cty TNHH Bia ABC │ 02812345678 │      1      │    500.000 đ  │
│ NCC Gạo XYZ      │ 0901111222  │      2      │  1.200.000 đ  │
│ (Không có NCC)   │ —           │      1      │    300.000 đ  │
├──────────────────────────────────────────────────────────────┤
│                              TỔNG PHẢI TRẢ:    2.000.000 đ   │
└──────────────────────────────────────────────────────────────┘
```

> Click vào 1 NCC → xem danh sách phiếu nhập còn nợ của NCC → chọn phiếu → [Trả nợ NCC].
> Dòng "(Không có NCC)" gom tất cả phiếu không gắn NCC có còn nợ.

### 3.7 Wireframe — Override FIFO tại POS (Popover)

Khi user click icon 🔧 trên một dòng SP trong giỏ hàng (chỉ hiện khi SP có > 1 lô tồn):

```
┌───────────────────────────────────────────────────┐
│  Coca 330ml — cần xuất: 3 lon                      │
│  ─────────────────────────────────────────────    │
│  Lô BATCH-0041 (01/06) giá vốn 6.800đ  còn: 2  [2]│
│  Lô BATCH-0042 (05/07) giá vốn 7.000đ  còn: 5  [1]│
│  Lô BATCH-0043 (15/07) giá vốn 7.200đ  còn:10  [0]│
│  ─────────────────────────────────────────────    │
│  Đã phân bổ: 3 / 3  ✓                             │
│               [ Hủy ]   [ Áp dụng ]               │
└───────────────────────────────────────────────────┘
```

> **Mặc định:** Số lượng điền sẵn theo FIFO (lô cũ nhất trước). User có thể chỉnh.
> **Ràng buộc:** Tổng phân bổ phải = SL dòng trong giỏ mới cho bấm [Áp dụng].

### 3.8 Wireframe — Danh sách nhà cung cấp

```
┌──────────────────────────────────────────────────────────────────┐
│  NHÀ CUNG CẤP                                                      │
│  🔍[ tìm tên / SĐT / mã... ]   [Trạng thái ▼]   [+ Thêm NCC]    │
├────────┬──────────────────────┬──────────────┬────────┬───────────┤
│ Mã NCC │ Tên                  │ SĐT          │Địa chỉ │ Thao tác  │
├────────┼──────────────────────┼──────────────┼────────┼───────────┤
│ NCC001 │ Cty TNHH Bia ABC     │ 02812345678  │Q.1 HCM │ [Sửa][Xóa]│
│ NCC002 │ NCC Gạo XYZ          │ 0901111222   │ —      │ [Sửa][Xóa]│
└────────┴──────────────────────┴──────────────┴────────┴───────────┘
```

### 3.9 Chi tiết các thành phần UI

| Thành phần                    | Loại control                  | Hành vi                                                                     |
| ----------------------------- | ----------------------------- | --------------------------------------------------------------------------- |
| Ô tìm kiếm phiếu nhập         | `TextField`                   | Debounce 200ms. Khớp Mã phiếu / Tên NCC.                                   |
| Dropdown Trạng thái phiếu     | `ComboBox`                    | Tất cả / Đã trả / Còn nợ. Mặc định: Tất cả.                                |
| Bảng danh sách phiếu          | `TableView`                   | Sắp xếp mặc định: ngày mới nhất. Click để chọn.                             |
| ComboBox Sản phẩm (dòng nhập) | `ComboBox` + search           | Tìm theo tên/mã SP. Không hiện SP đã bị ẩn (`is_active = 0`).              |
| Ô số lượng                    | `TextField` + `TextFormatter` | Chỉ nhận số nguyên > 0.                                                     |
| Ô giá nhập                    | `TextField` + `TextFormatter` | Chỉ nhận số nguyên > 0. Format VND khi blur.                                |
| Label Thành tiền              | `Label` (readonly, bind)      | Tự tính = SL × Giá nhập.                                                   |
| Ô Tiền đã trả                 | `TextField` + `TextFormatter` | Mặc định = Tổng nhập. Không vượt quá Tổng nhập.                            |
| Label Còn nợ NCC              | `Label` (bind, đổi màu)       | Cam nếu > 0, xanh nếu = 0.                                                 |
| DatePicker Ngày nhập          | `DatePicker`                  | Mặc định hôm nay. Cho phép chọn ngày trong quá khứ.                        |
| Nút [+ Thêm dòng]             | `Button`                      | Thêm dòng rỗng mới vào bảng nhập hàng.                                     |
| Nút [✖] xóa dòng             | `Button` (mỗi dòng)           | Xóa dòng khỏi bảng (không cần confirm).                                    |
| Nút [Lưu phiếu nhập]          | `Button`                      | Disabled khi: không có dòng, hoặc có lỗi validation.                       |
| Icon 🔧 override FIFO          | `Button` (nhỏ, mỗi dòng giỏ) | Mở popover chọn lô thủ công. Hiển thị khi SP có > 1 lô tồn tại.           |

---

## 4. Data Models & State

### 4.1 Database Schema

```sql
-- Nhà cung cấp (Suppliers)
suppliers (
    id          INTEGER PRIMARY KEY AUTOINCREMENT,
    code        TEXT    NOT NULL UNIQUE,       -- 'NCC001', tự sinh
    name        TEXT    NOT NULL,
    phone       TEXT,                          -- nullable
    address     TEXT,                          -- nullable
    note        TEXT,                          -- nullable
    is_active   INTEGER NOT NULL DEFAULT 1,    -- 1 = hoạt động, 0 = đã ẩn
    created_at  TEXT    NOT NULL               -- ISO 8601
)

-- Phiếu nhập hàng
purchases (
    id              INTEGER PRIMARY KEY AUTOINCREMENT,
    code            TEXT    NOT NULL UNIQUE,   -- 'PN001', tự sinh
    supplier_id     INTEGER REFERENCES suppliers(id),  -- nullable (NCC optional)
    purchase_date   TEXT    NOT NULL,          -- ISO 8601
    total_cost      INTEGER NOT NULL,          -- tổng tiền nhập (đồng)
    paid            INTEGER NOT NULL DEFAULT 0,         -- đã trả NCC (đồng)
    debt            INTEGER NOT NULL DEFAULT 0,         -- còn nợ = total_cost − paid
    status          TEXT    NOT NULL,          -- 'PAID' | 'PARTIAL' | 'UNPAID'
    note            TEXT,
    created_at      TEXT    NOT NULL
)

-- Lô hàng — BẢNG LÕI của hệ thống Specific Identification
inventory_batches (
    id               INTEGER PRIMARY KEY AUTOINCREMENT,
    product_id       INTEGER NOT NULL REFERENCES products(id),
    purchase_item_id INTEGER REFERENCES purchase_items(id),  -- nguồn gốc (nullable cho tương lai)
    cost_price       INTEGER NOT NULL,         -- giá vốn lô (đồng) — IMMUTABLE sau khi tạo
    qty_initial      INTEGER NOT NULL,         -- số lượng nhập ban đầu
    qty_remaining    INTEGER NOT NULL,         -- số lượng còn lại (giảm khi bán)
    received_date    TEXT    NOT NULL,         -- ngày nhập lô (= purchase_date)
    note             TEXT,
    created_at       TEXT    NOT NULL
)

-- Chi tiết phiếu nhập
purchase_items (
    id          INTEGER PRIMARY KEY AUTOINCREMENT,
    purchase_id INTEGER NOT NULL REFERENCES purchases(id),
    product_id  INTEGER NOT NULL REFERENCES products(id),
    batch_id    INTEGER NOT NULL REFERENCES inventory_batches(id), -- lô được tạo từ dòng này
    qty         INTEGER NOT NULL,             -- số lượng nhập
    cost_price  INTEGER NOT NULL,             -- giá vốn lô này (đồng)
    amount      INTEGER NOT NULL              -- = qty × cost_price
)

-- Lịch sử thanh toán NCC
supplier_payments (
    id           INTEGER PRIMARY KEY AUTOINCREMENT,
    purchase_id  INTEGER NOT NULL REFERENCES purchases(id),
    amount       INTEGER NOT NULL,            -- số tiền trả lần này (đồng)
    payment_date TEXT    NOT NULL,            -- ISO 8601
    note         TEXT
)

-- Phân bổ lô khi bán hàng (MỚI — liên kết hóa đơn ↔ lô cụ thể)
invoice_item_batches (
    id              INTEGER PRIMARY KEY AUTOINCREMENT,
    invoice_item_id INTEGER NOT NULL REFERENCES invoice_items(id),
    batch_id        INTEGER NOT NULL REFERENCES inventory_batches(id),
    qty             INTEGER NOT NULL,         -- số lượng lấy từ lô này
    cost_price      INTEGER NOT NULL          -- snapshot giá vốn lô tại thời điểm bán
)
```

**Index:**
```sql
CREATE INDEX idx_inventory_batches_product  ON inventory_batches(product_id, received_date);
-- Hỗ trợ FIFO: truy vấn lô theo sản phẩm, sắp theo ngày nhập tăng dần

CREATE INDEX idx_purchases_status           ON purchases(status);
CREATE INDEX idx_purchases_supplier         ON purchases(supplier_id);
CREATE INDEX idx_invoice_item_batches_item  ON invoice_item_batches(invoice_item_id);
```

### 4.2 Breaking Changes ảnh hưởng các module khác

> [!WARNING]
> Module Mua hàng yêu cầu **xóa cột `products.cost_price`** và **thêm bảng `invoice_item_batches`**. Đây là breaking change cần cập nhật đồng bộ với Quản lý hàng hóa và Bán hàng.

| Bảng/Module             | Thay đổi                                     | Ghi chú                                              |
| ----------------------- | -------------------------------------------- | ---------------------------------------------------- |
| `products`              | **Xóa cột `cost_price`**                     | Giá vốn chỉ tồn tại trên từng lô `inventory_batches`|
| `invoice_item_batches`  | **Bảng MỚI**                                 | Ghi nhận lô xuất khi bán → tính COGS chính xác      |
| `stock_movements`       | Thêm cột `batch_id` (nullable, FK → batches) | Truy vết biến động đến lô cụ thể                    |
| Spec Quản lý hàng hóa   | Cập nhật: bỏ trường "Giá vốn" ở form SP     | Giá vốn không còn gắn trực tiếp với sản phẩm        |
| Spec Bán hàng           | Cập nhật: thêm FIFO + override tại POS      | SalesService cần gọi InventoryService.allocateFIFO() |

### 4.3 Quy tắc trạng thái phiếu nhập

| Trạng thái | Điều kiện               | Mô tả                           |
| ---------- | ----------------------- | ------------------------------- |
| `PAID`     | `paid >= total_cost`    | Đã thanh toán đủ cho NCC        |
| `PARTIAL`  | `0 < paid < total_cost` | Đã trả một phần, còn nợ NCC    |
| `UNPAID`   | `paid = 0`              | Chưa trả đồng nào               |

### 4.4 Thuật toán FIFO — Xuất kho khi bán

`InventoryService.allocateFIFO(productId, qtyToSell, override?)` trả về danh sách phân bổ:

```
Đầu vào: product_id, qty_to_sell, override (nullable — danh sách lô user đã chọn)
Đầu ra:  List<{batch_id, qty, cost_price}>

Nếu override != null:
  → Dùng danh sách override (đã validate tổng = qty_to_sell) → Return ngay

Nếu override == null (FIFO tự động):
  1. SELECT * FROM inventory_batches
     WHERE product_id = ? AND qty_remaining > 0
     ORDER BY received_date ASC, id ASC    ← Lô cũ nhất trước

  2. qty_left = qty_to_sell
     allocations = []

  3. FOR EACH batch IN batches:
       take = MIN(batch.qty_remaining, qty_left)
       allocations.add({batch_id=batch.id, qty=take, cost_price=batch.cost_price})
       qty_left -= take
       IF qty_left == 0: BREAK

  4. IF qty_left > 0:
       -- Bán âm kho (đã cảnh báo tại POS khi thêm vào giỏ)
       -- Dùng lô gần nhất để ghi allocation âm
       last_batch = lô có received_date mới nhất của product
       allocations.add({batch_id=last_batch.id, qty=-qty_left, cost_price=last_batch.cost_price})

  Return allocations
```

> Kết quả phân bổ được dùng trong `SalesService.createInvoice()` để INSERT `invoice_item_batches` và UPDATE `inventory_batches.qty_remaining`.

### 4.5 Logic sinh Mã phiếu nhập & Mã NCC

**Mã phiếu nhập (PN):**
```
1. INSERT → purchases (code = NULL tạm)
2. id = last_insert_rowid()
3. code = "PN" + String.format("%03d", id)     -- PN001, PN002 ... PN999
   (nếu id > 999: code = "PN" + id)            -- PN1000, PN1001...
4. UPDATE purchases SET code = ? WHERE id = ?
-- Cả 4 bước trong 1 TRANSACTION
```

**Mã NCC:** Tương tự, format `NCC001`.
**Mã lô:** Không có code riêng — hiển thị dạng `BATCH-` + String.format(`"%04d"`, id).

### 4.6 State Changes

**Khi Tạo phiếu nhập (UC-MH02):**
```
BEGIN TRANSACTION
  INSERT → purchases                 (code = NULL tạm)
  UPDATE purchases SET code = 'PNxxx'
  FOR EACH dòng sản phẩm:
    INSERT → inventory_batches       (qty_remaining = qty, cost_price = giá nhập dòng)
    INSERT → purchase_items          (liên kết purchase ↔ batch)
    UPDATE products SET stock_qty += qty
    INSERT → stock_movements         (type='NHAP', qty_change=+qty, batch_id=batch.id,
                                      stock_after = stock_qty sau update)
  IF paid > 0:
    INSERT → supplier_payments
COMMIT
```

**Khi Trả nợ NCC (UC-MH04):**
```
BEGIN TRANSACTION
  INSERT → supplier_payments
  UPDATE purchases SET
    paid   = (SELECT SUM(amount) FROM supplier_payments WHERE purchase_id = ?),
    debt   = total_cost - paid,
    status = CASE WHEN paid >= total_cost THEN 'PAID' ELSE 'PARTIAL' END
COMMIT
```

**Khi Bán hàng (tích hợp vào SalesService.createInvoice()):**
```
BEGIN TRANSACTION
  ... (logic hiện có từ spec ban-hang)
  FOR EACH dòng trong invoice_items:
    allocations = InventoryService.allocateFIFO(product_id, qty, override?)
    FOR EACH {batch_id, qty, cost_price} IN allocations:
      INSERT → invoice_item_batches
      UPDATE inventory_batches SET qty_remaining -= qty WHERE id = batch_id
COMMIT
```

### 4.7 ViewModel State

```java
// PurchaseListViewModel
ObservableList<Purchase>       purchases;
FilteredList<Purchase>         filteredPurchases;
StringProperty                 searchKeyword;
ObjectProperty<PurchaseStatus> filterStatus;   // ALL | PAID | PARTIAL_OR_UNPAID

// PurchaseFormViewModel (màn hình tạo phiếu — phức tạp → tách ViewModel riêng)
ObjectProperty<Supplier>         selectedSupplier;  // nullable
ObjectProperty<LocalDate>        purchaseDate;
StringProperty                   note;
ObservableList<PurchaseLineItem> lines;
LongProperty   totalCost;     // computed = Σ line.amount
LongProperty   paidAmount;    // tiền đã trả NCC (user nhập)
LongProperty   debtAmount;    // computed = totalCost - paidAmount
BooleanProperty canSave;      // = lines.size > 0 AND no validation errors

// PurchaseLineItem (mỗi dòng trong form nhập hàng)
class PurchaseLineItem {
    ObjectProperty<Product> product;
    IntegerProperty qty;
    LongProperty    costPrice;
    LongProperty    amount;        // computed = qty * costPrice
    StringProperty  errorMessage;  // validation error (nếu có)
}

// Bổ sung vào CartItem (bên ban-hang)
class CartItem {
    Product product;
    IntegerProperty quantity;
    LongProperty    amount;
    boolean         isNegativeStock;
    List<BatchAllocation> batchOverride;  // null = dùng FIFO tự động
}

// BatchAllocation (phân bổ lô thủ công tại POS)
class BatchAllocation {
    int  batchId;
    int  qty;
    long costPrice;  // từ inventory_batches.cost_price
}

// Supplier (model)
class Supplier {
    int     id;
    String  code;      // "NCC001"
    String  name;
    String  phone;     // nullable
    String  address;   // nullable
    String  note;      // nullable
    boolean isActive;
    String  createdAt;
}

enum PurchaseStatus { PAID, PARTIAL, UNPAID }
```

---

## 5. Integration & Architecture

### 5.1 Layer Flow (theo kiến trúc MVVM-lite)

```
┌─────────┐   ┌──────────────────────────┐   ┌────────────────────────┐   ┌─────────────────────┐   ┌──────────┐
│  View   │──▶│ PurchaseListController   │──▶│ PurchaseListViewModel  │──▶│  PurchaseService    │──▶│   DAOs   │
│ (FXML)  │   │ PurchaseFormController   │   │ PurchaseFormViewModel  │   │  SupplierService    │   │          │
│         │   │ SupplierController       │   │ (tách ViewModel riêng) │   │                     │   │          │
└─────────┘   └──────────────────────────┘   └────────────────────────┘   └─────────────────────┘   └──────────┘
                                                                                       │
                                                        ┌──────────────────────────────┤
                                                        │                              │
                                            ┌───────────────────────┐   ┌─────────────────────┐
                                            │  InventoryService     │   │  PaymentService     │
                                            │  allocateFIFO(...)    │   │  collectSupplier-   │
                                            │  (SERVICE MỚI)        │   │  Payment() (MỞ RỘNG)│
                                            └───────────────────────┘   └─────────────────────┘
                                                        ▲
                                                        │ gọi từ SalesService khi checkout
                                            ┌───────────────────────┐
                                            │     SalesService      │
                                            │  createInvoice()      │
                                            └───────────────────────┘
```

### 5.2 Các class liên quan

| Layer      | Class                         | Trách nhiệm                                                                   |
| ---------- | ----------------------------- | ----------------------------------------------------------------------------- |
| View       | `purchase-list.fxml`          | Danh sách phiếu nhập + toolbar tìm kiếm/filter                                |
| View       | `purchase-form.fxml`          | Màn hình tạo phiếu nhập (full-page, không phải dialog)                        |
| View       | `purchase-detail.fxml`        | Chi tiết phiếu nhập + lịch sử thanh toán NCC                                 |
| View       | `debt-payable.fxml`           | Công nợ phải trả tổng hợp theo NCC                                            |
| View       | `supplier-list.fxml`          | Danh sách NCC + nút CRUD                                                      |
| View       | `supplier-form-dialog.fxml`   | Dialog Thêm/Sửa NCC                                                           |
| View       | `batch-override-popover.fxml` | Popover chọn lô thủ công tại POS                                             |
| Controller | `PurchaseListController`      | Bind danh sách, search/filter, điều hướng đến form/detail                     |
| Controller | `PurchaseFormController`      | Bind form tạo phiếu, quản lý dòng SP, validate, gọi PurchaseFormViewModel    |
| Controller | `PurchaseDetailController`    | Hiển thị chi tiết phiếu + lịch sử thanh toán; mở dialog Trả nợ               |
| Controller | `SupplierController`          | CRUD NCC; bind với SupplierService                                            |
| Controller | `BatchOverrideController`     | Quản lý popover phân bổ lô thủ công tại POS                                  |
| ViewModel  | `PurchaseFormViewModel`       | State phức tạp: lines, totals, validation, submit                             |
| Service    | `PurchaseService`             | `createPurchase()` — orchestrate toàn bộ DAOs trong 1 transaction            |
| Service    | `InventoryService`            | `allocateFIFO(productId, qty, override)` — thuật toán FIFO **(SERVICE MỚI)** |
| Service    | `SupplierService`             | `add()`, `update()`, `softDelete()`, `restore()`, `checkDeletable()`         |
| Service    | `PaymentService`              | Bổ sung `collectSupplierPayment()` — trả nợ NCC                              |
| DAO        | `PurchaseDao`                 | `insert()`, `updateCode()`, `findAll()`, `findByFilter()`, `updatePaid()`    |
| DAO        | `PurchaseItemDao`             | `insertAll(items)` — lưu chi tiết phiếu nhập                                |
| DAO        | `InventoryBatchDao`           | `insert()`, `findByProduct()`, `decreaseQty()`, `findBatchesForFIFO()`       |
| DAO        | `SupplierDao`                 | `insert()`, `update()`, `findAll()`, `setActive()`, `hasPendingDebt()`       |
| DAO        | `SupplierPaymentDao`          | `insert()`, `findByPurchaseId()`, `sumByPurchaseId()`                        |
| DAO        | `InvoiceItemBatchDao`         | `insertAll()`, `findByInvoiceItemId()` **(DAO MỚI)**                         |

### 5.3 Threading Model

```
[JavaFX App Thread]
    │
    ├── Tìm kiếm / Filter phiếu nhập: FilteredList in-memory → UI thread (nhanh)
    │
    ├── Tạo phiếu nhập: PurchaseService.createPurchase() → background Task
    │     ├── DB transaction: insert purchases + batches + items + stock + log
    │     └── Platform.runLater() → hiện thông báo + navigate về danh sách
    │
    ├── Trả nợ NCC: PaymentService.collectSupplierPayment() → background Task
    │     └── Platform.runLater() → refresh màn hình chi tiết phiếu
    │
    └── Phân bổ FIFO khi bán: InventoryService.allocateFIFO() → chạy trong
          background Task của SalesService.createInvoice() (đã có sẵn)
          → không cần Task riêng
```

### 5.4 Module phụ thuộc

- **Đầu vào từ module khác:**
  - `products` (từ Quản lý hàng hóa) — chọn SP khi tạo phiếu nhập; update `stock_qty`.

- **Đầu ra cho module khác:**
  - `inventory_batches` → **SalesService/InventoryService** đọc để phân bổ FIFO khi bán.
  - `invoice_item_batches` → **Dashboard (GĐ2)** tính COGS chính xác và lợi nhuận.
  - `stock_movements` (type=NHAP) → Lịch sử biến động tồn kho.
  - Khi NCC bị xóa mềm → Phiếu nhập cũ hiển thị `[NCC đã ẩn]` thay vì NULL.

---

## 6. Edge Cases & Risks

### 6.1 Edge Cases

| #   | Tình huống                                                        | Xử lý                                                                                      |
| --- | ----------------------------------------------------------------- | ------------------------------------------------------------------------------------------ |
| E1  | Nhập cùng 1 SP nhiều dòng trong 1 phiếu (giá khác nhau)          | Cho phép — mỗi dòng tạo 1 lô riêng; giá vốn khác nhau là hợp lệ                          |
| E2  | Nhập 1 SP với giá vốn khác hẳn lô trước                          | Cho phép — FIFO đảm bảo lô cũ bán trước, lô mới bán sau                                  |
| E3  | Bán vượt tổng qty_remaining của tất cả lô (bán âm kho)           | Vẫn cho bán (theo quyết định MVP); FIFO tạo allocation âm vào lô gần nhất; cảnh báo ở POS|
| E4  | Override FIFO: user phân bổ tổng ≠ SL giỏ hàng                   | Không cho bấm [Áp dụng] — validation ngay trong popover                                   |
| E5  | Override FIFO: user chọn qty từ lô nhiều hơn qty_remaining        | Không cho — ô nhập bị giới hạn max = qty_remaining của lô đó                              |
| E6  | Trả nợ NCC vượt quá số còn nợ                                     | Validation chặn ngay tại dialog                                                            |
| E7  | Xóa NCC còn phiếu nhập chưa thanh toán (debt > 0)                | Chặn: "Không thể ẩn NCC — còn công nợ chưa thanh toán"                                    |
| E8  | Xóa NCC có phiếu nhập đã thanh toán đủ                            | Cho phép xóa mềm; phiếu cũ hiển thị NCC là `[NCC đã ẩn]`                                  |
| E9  | Tạo phiếu nhập không chọn NCC                                      | Cho phép — `supplier_id = NULL`, hiển thị "(Không có NCC)"                                |
| E10 | SP bị ẩn sau khi đã có lô còn tồn                                 | Lô vẫn tồn; FIFO vẫn dùng lô đó khi bán; SP không xuất hiện ở form tạo phiếu mới        |
| E11 | Tiền đã trả = 0 khi tạo phiếu (ghi nợ toàn bộ)                   | Cho phép — `status = UNPAID`; phiếu xuất hiện ở màn hình Công nợ phải trả                 |
| E12 | qty_remaining của lô về 0 sau khi bán hết                         | Lô vẫn tồn DB (giữ lịch sử); FIFO bỏ qua lô đó khi `qty_remaining = 0`                   |
| E13 | SP nhập lần đầu, tồn kho đang = 0                                 | Tạo lô bình thường; `stock_qty` tăng từ 0 lên qty nhập                                    |
| E14 | Tạo phiếu nhập không có dòng sản phẩm nào                         | Chặn lưu — nút [Lưu] disabled khi lines rỗng                                              |

### 6.2 Risks & Mitigations

| Rủi ro                                                             | Mức độ         | Giảm thiểu                                                                         |
| ------------------------------------------------------------------ | -------------- | ----------------------------------------------------------------------------------- |
| FIFO bug gây sai COGS và lợi nhuận                                 | **Cao**        | Unit test `InventoryService.allocateFIFO()` với ≥ 10 kịch bản đa dạng             |
| `invoice_item_batches` không insert → mất traceability lợi nhuận  | **Cao**        | Toàn bộ trong 1 transaction — lỗi bất kỳ → rollback, hóa đơn không được tạo       |
| `qty_remaining` lệch `products.stock_qty` (invariant bị phá)      | **Cao**        | Transaction atomic; thêm assertion check trong dev/test                             |
| Override FIFO: user phân bổ sai → dữ liệu sai                     | **Trung bình** | Validation chặt trong popover (tổng = qty giỏ, qty từng lô ≤ qty_remaining)        |
| Phiếu nhập nhiều dòng SP → transaction dài                        | **Thấp**       | Background Task; SQLite với vài chục dòng vẫn nhanh                                |
| Giá vốn lô bị sửa sau khi đã bán (dữ liệu lịch sử sai)           | **Trung bình** | `inventory_batches.cost_price` là **IMMUTABLE** — không có chức năng sửa giá vốn lô|

---

## 7. Decision Log

| #   | Quyết định                                                                          | Lý do                                                                                     |
| --- | ----------------------------------------------------------------------------------- | ----------------------------------------------------------------------------------------- |
| D1  | Dùng Specific Identification (Batch Tracking) thay vì FIFO/LIFO/bình quân giá      | Chính xác 100% cho COGS và lợi nhuận, không sai số dù giá vốn biến động theo từng lô    |
| D2  | Bỏ `products.cost_price` — giá vốn chỉ trên lô (`inventory_batches.cost_price`)   | Tránh mâu thuẫn giữa giá vốn SP vs. giá vốn thực tế từng lô; dữ liệu nhất quán          |
| D3  | Thêm bảng `invoice_item_batches` liên kết hóa đơn ↔ lô                             | Cho phép tính COGS và lợi nhuận chính xác trong Dashboard/Báo cáo (GĐ2)                  |
| D4  | FIFO mặc định, cho phép override thủ công tại POS                                  | Tự động hóa quy trình thông thường; vẫn linh hoạt cho trường hợp đặc biệt                |
| D5  | NCC là optional trên phiếu nhập                                                     | Thực tế cửa hàng nhỏ thường mua từ chợ, không cần ghi NCC mỗi lần                       |
| D6  | Quản lý NCC trong MVP (không đợi GĐ2)                                              | Cần thiết để theo dõi công nợ phải trả ngay từ đầu                                      |
| D7  | Mỗi dòng SP trong phiếu nhập = 1 lô riêng                                          | Cho phép nhập cùng SP nhiều giá trong 1 phiếu; đơn giản hóa logic tạo lô                |
| D8  | Không xóa lô khi `qty_remaining = 0` (chỉ bỏ qua trong FIFO)                      | Giữ lịch sử để truy vết giá vốn hàng đã bán                                             |
| D9  | Tạo phiếu nhập là màn hình full-page (không phải dialog)                            | Danh sách SP nhập có thể dài, cần không gian rộng; tránh scroll trong dialog nhỏ         |
| D10 | Nhóm phiếu không có NCC thành "(Không có NCC)" trong màn hình Công nợ phải trả     | Vẫn theo dõi được tổng nợ chưa trả dù phiếu không gắn NCC cụ thể                       |
| D11 | `inventory_batches.cost_price` là IMMUTABLE sau khi tạo                             | Đảm bảo tính bất biến lịch sử — giá vốn đã bán không thể bị sửa hồi tố                 |
