# Spec: Quản lý hàng hóa

> Module: phase-1-mvp/quan-ly-hang-hoa · Phiên bản: 1.0 · Ngày: 2026-07-19
> Tham chiếu: [MVP-plan.md](../../MVP-plan.md) · [architecture.md](../../architecture.md) · [database-schema.md](../../database-schema.md) · [mua-hang/spec.md](../mua-hang/spec.md)

---

## 1. Executive Summary

Module **Quản lý hàng hóa** quản lý toàn bộ danh mục sản phẩm và tình trạng tồn kho của cửa hàng. Đây là module **trung tâm** — mọi giao dịch nhập hàng và bán hàng đều xoay quanh dữ liệu sản phẩm.

Chức năng cho phép chủ cửa hàng:

- **CRUD hàng hóa:** Thêm / Sửa / Xóa mềm (`deleted_at`) / Khôi phục.
- **Quản lý giá bán:** Cập nhật giá bán với lịch sử đầy đủ (giá cũ → giá mới, thời điểm).
- **Tồn kho tự động:** Cập nhật real-time qua phiếu nhập hàng (Mua hàng) và hóa đơn bán hàng (Bán hàng).
- **Kiểm kho (Stock Adjustment):** Tạo phiếu điều chỉnh tồn kho (`DC000001`) với audit trail đầy đủ; bắt buộc ghi lý do.
- **Import/Export:** Nhập liệu hàng loạt từ Excel/CSV; xuất danh mục theo filter đang áp dụng.

**Ràng buộc đã chốt:**
- **Không hiển thị Giá vốn** trên màn hình sản phẩm — giá vốn chỉ tồn tại trên từng lô hàng (`inventory_batches`) theo mô hình FIFO (xem [mua-hang/spec.md](../mua-hang/spec.md)).
- Soft delete dùng **`deleted_at = NOW()`** (không dùng `status` flag).
- **Chặn xóa mềm** nếu SP còn tồn kho (`stock_qty > 0`).
- Mã hàng tự động sinh theo format **`HH000001`**, không cho phép chỉnh sửa.
- Mọi thay đổi tồn kho phải đi qua **`stock_movements`** — audit trail bắt buộc.

---

## 2. Requirements & Use Cases

### 2.1 Functional Requirements

| ID      | Yêu cầu                                                                                                                                | Độ ưu tiên |
| ------- | -------------------------------------------------------------------------------------------------------------------------------------- | ---------- |
| FR-HH01 | Xem danh sách hàng hóa — mặc định chỉ hiện SP chưa xóa mềm (`deleted_at IS NULL`)                                                    | Bắt buộc   |
| FR-HH02 | Tìm kiếm real-time theo Tên / Mã hàng (debounce 200ms, case-insensitive)                                                              | Bắt buộc   |
| FR-HH03 | Lọc danh sách theo Danh mục và Trạng thái (Đang bán / Đã ẩn / Tất cả)                                                                | Bắt buộc   |
| FR-HH04 | Thêm hàng hóa mới — tự động sinh Mã hàng (format `HH000001`)                                                                         | Bắt buộc   |
| FR-HH05 | Sửa thông tin SP (Tên, Đơn vị, Danh mục, Giá bán, Ghi chú) — Mã hàng readonly                                                       | Bắt buộc   |
| FR-HH06 | Xóa mềm SP: set `deleted_at = NOW()` — SP biến mất khỏi danh sách mặc định và ComboBox bán hàng                                      | Bắt buộc   |
| FR-HH07 | Khôi phục SP đã ẩn: set `deleted_at = NULL`                                                                                           | Bắt buộc   |
| FR-HH08 | Chặn xóa mềm nếu SP còn tồn kho (`stock_qty > 0`)                                                                                    | Bắt buộc   |
| FR-HH09 | Xem chi tiết SP: Tên, Mã, Đơn vị, Danh mục, Tồn kho, Giá bán hiện tại, Ghi chú                                                      | Bắt buộc   |
| FR-HH10 | Cập nhật Giá bán → tự động ghi 1 bản ghi vào `price_history` (giá cũ, giá mới, thời điểm)                                           | Bắt buộc   |
| FR-HH11 | Xem lịch sử giá bán của SP — read-only, trong màn hình Chi tiết                                                                      | Bắt buộc   |
| FR-HH12 | Mở màn hình Phiếu điều chỉnh kho — hiển thị bảng tất cả SP active với 3 cột: Tồn phần mềm / Tồn thực tế (nhập) / Chênh lệch (locked) | Bắt buộc   |
| FR-HH13 | Khi chênh lệch ≠ 0: bắt buộc điền Lý do cho từng dòng trước khi lưu phiếu                                                           | Bắt buộc   |
| FR-HH14 | Khi chênh lệch dương (Variance > 0): bắt buộc nhập Giá vốn của số hàng dôi dư                                                       | Bắt buộc   |
| FR-HH15 | Lưu phiếu → cập nhật `stock_qty` + ghi `stock_movements` (type=`KIEMKHO`) + tạo `inventory_batch` mới nếu Variance > 0               | Bắt buộc   |
| FR-HH16 | Xem lịch sử danh sách phiếu điều chỉnh kho (ngày, mã phiếu, số dòng có thay đổi)                                                    | Bắt buộc   |
| FR-HH17 | Import danh sách hàng hóa từ file Excel / CSV — validate + preview trước khi lưu; trùng mã → báo lỗi, bỏ qua dòng đó                | Bắt buộc   |
| FR-HH18 | Export danh sách hàng hóa ra file Excel / CSV theo filter đang áp dụng                                                               | Bắt buộc   |

### 2.2 Non-functional Requirements

| ID        | Yêu cầu                                                                                                            |
| --------- | ------------------------------------------------------------------------------------------------------------------ |
| NFR-HH01  | Tìm kiếm real-time phản hồi < 300ms (debounce 200ms, in-memory `FilteredList` với vài nghìn SP)                   |
| NFR-HH02  | Toàn bộ thao tác DB (thêm/sửa/xóa/import/lưu phiếu kiểm kho) chạy trên background thread — không block UI        |
| NFR-HH03  | Import phải validate từng dòng và báo lỗi cụ thể (dòng bao nhiêu, lỗi gì) trước khi lưu                          |
| NFR-HH04  | Mã hàng tự sinh đảm bảo không trùng lặp (dùng `last_insert_rowid()` + format)                                    |
| NFR-HH05  | Mọi thay đổi tồn kho phải đi qua `stock_movements` — không UPDATE `stock_qty` trực tiếp mà không ghi log         |

### 2.3 Use Cases

#### UC-HH01 — Xem & Tìm kiếm danh sách hàng hóa

- **Actor:** Chủ cửa hàng
- **Tiền điều kiện:** Đã đăng nhập.

**Luồng chính:**

```
1. Người dùng mở màn hình "Hàng hóa" từ sidebar.
2. Hệ thống hiển thị danh sách SP chưa xóa mềm (deleted_at IS NULL),
   sắp xếp theo Mã hàng tăng dần.
3. Người dùng gõ từ khóa vào ô tìm kiếm.
4. Hệ thống lọc real-time (debounce 200ms), khớp với Tên / Mã hàng.
5. Người dùng chọn Danh mục / Trạng thái từ dropdown để thu hẹp kết quả.
6. Kết quả cập nhật ngay trên danh sách.
```

**Luồng ngoại lệ:**

| Bước | Điều kiện           | Xử lý                                              |
| ---- | ------------------- | -------------------------------------------------- |
| 4–5  | Không tìm thấy SP   | Hiển thị trạng thái rỗng: "Không tìm thấy hàng hóa nào" |

---

#### UC-HH02 — Thêm hàng hóa mới

- **Actor:** Chủ cửa hàng
- **Tiền điều kiện:** Đã đăng nhập.

**Luồng chính:**

```
1. Người dùng bấm nút [+ Thêm hàng hóa].
2. Hệ thống mở form dialog "Thêm hàng hóa".
3. Người dùng điền thông tin:
   - Tên hàng hóa (*): bắt buộc
   - Đơn vị tính (*): bắt buộc (ComboBox từ bảng units)
   - Danh mục: tùy chọn (ComboBox từ bảng categories)
   - Giá bán (*): bắt buộc, ≥ 0 (đồng nguyên)
   - Ghi chú: tùy chọn
4. Người dùng bấm [Lưu].
5. Hệ thống validate → BEGIN TRANSACTION:
   - INSERT INTO products (code = NULL tạm, ...)
   - id = last_insert_rowid()
   - code = "HH" + String.format("%06d", id)
   - UPDATE products SET code = ?
   - COMMIT
6. SP mới xuất hiện trên danh sách, hiện thông báo thành công.
```

> **Lưu ý:** Tồn kho ban đầu = 0. Muốn nhập hàng đầu vào → dùng module **Mua hàng**.

**Luồng ngoại lệ:**

| Bước | Điều kiện                       | Xử lý                                                   |
| ---- | ------------------------------- | -------------------------------------------------------- |
| 4    | Tên để trống / chỉ khoảng trắng | Báo lỗi inline: "Tên hàng hóa không được để trống"      |
| 4    | Chưa chọn Đơn vị               | Báo lỗi inline: "Vui lòng chọn đơn vị tính"             |
| 4    | Giá bán < 0 hoặc không hợp lệ  | Báo lỗi inline: "Giá bán phải là số không âm"           |

---

#### UC-HH03 — Sửa thông tin / Cập nhật giá bán

- **Actor:** Chủ cửa hàng
- **Tiền điều kiện:** SP chưa bị xóa mềm (`deleted_at IS NULL`).

**Luồng chính:**

```
1. Người dùng chọn SP → bấm [Sửa] (hoặc double-click).
2. Hệ thống mở form dialog "Sửa hàng hóa" — điền sẵn dữ liệu hiện tại.
   Mã hàng hiển thị nhưng disabled (không cho sửa).
3. Người dùng chỉnh sửa các trường cần thiết.
4. Người dùng bấm [Lưu].
5. Hệ thống validate, sau đó:
   a. Nếu Giá bán thay đổi:
      INSERT INTO price_history (product_id, old_price, new_price, changed_at = NOW())
   b. UPDATE products SET name, unit_id, category_id, sale_price, note, updated_at = NOW()
6. Hệ thống refresh dòng SP trên danh sách.
```

> **Lưu ý:** Không có trường Giá vốn trong form sửa. Giá vốn chỉ thay đổi khi tạo phiếu nhập hàng mới.

**Luồng ngoại lệ:** Tương tự UC-HH02 (validate Tên, Đơn vị, Giá bán).

---

#### UC-HH04 — Xóa mềm / Khôi phục hàng hóa

- **Actor:** Chủ cửa hàng
- **Tiền điều kiện (Xóa):** SP có `stock_qty = 0`.

**Luồng chính — Xóa mềm:**

```
1. Người dùng chọn SP → bấm [Xóa].
2. Hệ thống kiểm tra stock_qty = 0.
3. Hệ thống hiện popup xác nhận:
   "Bạn có chắc muốn ẩn [Tên SP]?
   SP sẽ không xuất hiện trong danh sách bán hàng."
4. Người dùng bấm [Xác nhận].
5. UPDATE products SET deleted_at = NOW() WHERE id = ?
   SP biến mất khỏi danh sách mặc định và ComboBox bán hàng (POS).
```

**Luồng chính — Khôi phục:**

```
1. Người dùng lọc trạng thái "Đã ẩn" → chọn SP → bấm [Khôi phục].
2. UPDATE products SET deleted_at = NULL WHERE id = ?
3. SP xuất hiện lại ở filter mặc định.
```

**Luồng ngoại lệ:**

| Bước | Điều kiện                    | Xử lý                                                                                     |
| ---- | ---------------------------- | ----------------------------------------------------------------------------------------- |
| 2    | SP còn tồn kho (`stock_qty > 0`) | Chặn, hiện dialog: "Không thể ẩn — hàng hóa còn [X] đơn vị trong kho. Xuất kho hoặc điều chỉnh tồn về 0 trước." |
| 3    | Người dùng bấm [Hủy]        | Đóng popup, giữ nguyên trạng thái SP                                                     |

---

#### UC-HH05 — Xem chi tiết hàng hóa

- **Actor:** Chủ cửa hàng

**Luồng chính:**

```
1. Người dùng chọn SP → bấm [Chi tiết].
2. Hệ thống mở màn hình chi tiết gồm 2 vùng:
   Vùng trên — Thông tin SP:
     Mã hàng | Tên | Đơn vị | Danh mục | Tồn kho | Giá bán hiện tại | Ghi chú.
   Vùng dưới — Lịch sử giá bán:
     Bảng (Giá cũ → Giá mới | Thời điểm thay đổi), mới nhất trước.
     Nếu chưa có lịch sử → hiện thông báo "Chưa có lịch sử thay đổi giá".
```

---

#### UC-HH06 — Tạo phiếu điều chỉnh kho (Kiểm kho)

- **Actor:** Chủ cửa hàng
- **Tiền điều kiện:** Đã đăng nhập.

**Luồng chính:**

```
1. Người dùng bấm [Kiểm kho] từ màn hình Hàng hóa.
2. Hệ thống sinh Mã phiếu (ví dụ: DC000001) và mở màn hình
   "Phiếu điều chỉnh kho" với bảng tất cả SP active, mỗi dòng gồm:
   - Tên SP / Mã SP (readonly)
   - Tồn phần mềm (Current Stock): lấy từ products.stock_qty — locked
   - Tồn thực tế (Actual Stock): ô nhập liệu, mặc định = Current Stock
   - Chênh lệch (Variance = Actual − Current): hệ thống tự tính — locked
   - Lý do: ô nhập (bắt buộc nếu Variance ≠ 0)
   - Giá vốn dôi dư: ô nhập (bắt buộc nếu Variance > 0)
3. Người dùng điền cột "Tồn thực tế" cho các SP có thay đổi.
4. Hệ thống realtime tính Variance và highlight các dòng Variance ≠ 0.
5. Người dùng điền Lý do (+ Giá vốn nếu Variance > 0) cho từng dòng lệch.
6. Người dùng bấm [Lưu phiếu].
7. Hệ thống xử lý trong 1 TRANSACTION, cho từng dòng có Variance ≠ 0:
   a. UPDATE products SET stock_qty = actual_qty, updated_at = NOW()
   b. INSERT INTO stock_movements
      (product_id, type='KIEMKHO', qty_change=variance, stock_after=actual_qty,
       ref_type='ADJUSTMENT', ref_id=adjustment_id, note=reason)
   c. Nếu variance > 0:
      INSERT INTO inventory_batches
      (product_id, qty=variance, cost_price=<giá vốn người dùng nhập>, source='ADJUSTMENT')
8. Thông báo: "Đã lưu phiếu [DC000001]. Điều chỉnh [X] sản phẩm."
```

**Luồng ngoại lệ:**

| Bước | Điều kiện                                          | Xử lý                                                      |
| ---- | -------------------------------------------------- | ---------------------------------------------------------- |
| 6    | Dòng có Variance ≠ 0 nhưng Lý do để trống         | Báo lỗi inline, nút [Lưu phiếu] disabled                  |
| 6    | Dòng có Variance > 0 nhưng Giá vốn để trống       | Báo lỗi inline: "Vui lòng nhập giá vốn cho hàng dôi dư"   |
| 6    | Không có dòng nào Variance ≠ 0                    | Thông báo: "Không có hàng hóa nào thay đổi"               |
| 3    | Người dùng nhập Actual Stock < 0                  | Chặn: "Tồn thực tế không được âm"                         |

---

#### UC-HH07 — Import danh sách hàng hóa

- **Actor:** Chủ cửa hàng

**Luồng chính:**

```
1. Người dùng bấm [Import].
2. Hệ thống mở hộp thoại chọn file (*.xlsx, *.csv).
3. Người dùng chọn file.
4. Hệ thống đọc và validate toàn bộ dữ liệu:
   - Kiểm tra cột bắt buộc (Tên, Đơn vị).
   - Kiểm tra Đơn vị có tồn tại trong hệ thống không.
   - Kiểm tra Giá bán ≥ 0 (nếu có).
   - Kiểm tra Mã hàng trùng với SP đã có trong DB → đánh dấu lỗi, bỏ qua dòng đó.
   - Kiểm tra trùng Mã hàng trong chính file (dòng sau trùng với dòng trước).
5. Hiện màn hình preview kết quả validate:
   - Tổng dòng hợp lệ / tổng dòng.
   - Danh sách dòng lỗi (số dòng + lý do, ví dụ: "Dòng 4: Mã 'HH000003' đã tồn tại").
6. Người dùng bấm [Xác nhận import].
7. Hệ thống chỉ import dòng hợp lệ → tự sinh Mã hàng → tồn kho = 0 → lưu DB.
8. Thông báo: "Đã import X hàng hóa. Bỏ qua Y dòng lỗi."
```

**Luồng ngoại lệ:**

| Bước | Điều kiện                            | Xử lý                                                           |
| ---- | ------------------------------------ | --------------------------------------------------------------- |
| 3    | File sai định dạng / không đọc được  | Hiện lỗi: "File không hợp lệ. Vui lòng chọn .xlsx hoặc .csv"  |
| 4    | Toàn bộ dòng đều lỗi                | Không cho import, hiện danh sách lỗi đầy đủ                    |
| 4    | File rỗng (0 dòng dữ liệu)          | Hiện thông báo: "File không có dữ liệu để import"              |

---

#### UC-HH08 — Export danh sách hàng hóa

- **Actor:** Chủ cửa hàng

**Luồng chính:**

```
1. Người dùng bấm [Export].
2. Hệ thống mở hộp thoại chọn vị trí lưu file
   (mặc định: danh-sach-hang-hoa.xlsx).
3. Hệ thống xuất đúng danh sách SP đang hiển thị theo filter/tìm kiếm hiện tại.
4. Thông báo: "Đã xuất file thành công tại [đường dẫn]."
```

**Luồng ngoại lệ:**

| Bước | Điều kiện                       | Xử lý                                   |
| ---- | ------------------------------- | ---------------------------------------- |
| 1    | Danh sách hiện tại rỗng         | Thông báo: "Không có dữ liệu để xuất"  |

---

## 3. UI/UX & Navigation

### 3.1 Vị trí trong Navigation Map

```
[Main Window] → Sidebar → 📦 Hàng hóa
   └── Danh sách hàng hóa                  ← MÀN HÌNH CHÍNH
         ├── [+ Thêm]     → Dialog Thêm SP
         ├── [Sửa]        → Dialog Sửa SP
         ├── [Chi tiết]   → Màn hình Chi tiết SP
         └── [Kiểm kho]   → Màn hình Phiếu điều chỉnh kho
               └── [Lịch sử] → Danh sách phiếu đã lưu
```

### 3.2 Wireframe — Danh sách hàng hóa

```
┌───────────────────────────────────────────────────────────────────────────────────────────┐
│  HÀNG HÓA                                                                                  │
│  🔍[ tìm tên / mã hàng... ]  [Danh mục ▼]  [Trạng thái ▼]  [+ Thêm] [Kiểm kho] [Import] [Export] │
├──────────┬──────────────────────┬─────────┬────────────────┬──────────┬──────────┬──────────────────────────┤
│ Mã hàng  │ Tên                  │ Đơn vị  │ Danh mục       │ Tồn kho  │ Giá bán  │ Thao tác                 │
├──────────┼──────────────────────┼─────────┼────────────────┼──────────┼──────────┼──────────────────────────┤
│ HH000001 │ Nước khoáng Lavie    │ chai    │ Nước giải khát │ 120      │ 8.000 đ  │ [Chi tiết] [Sửa] [Xóa]  │
│ HH000002 │ Bánh mì sandwich     │ cái     │ Bánh           │ 0        │ 25.000 đ │ [Chi tiết] [Sửa] [Xóa]  │
│ HH000003 │ Bia Tiger (đã ẩn)    │ lon     │ Bia            │ —        │ 20.000 đ │ [Chi tiết] [Khôi phục]   │
└──────────┴──────────────────────┴─────────┴────────────────┴──────────┴──────────┴──────────────────────────┘
```

> **Lưu ý:**
> - Mặc định filter = "Đang bán" — chỉ hiện SP `deleted_at IS NULL`.
> - Filter "Đã ẩn" → hiện SP `deleted_at IS NOT NULL`, thao tác đổi thành `[Chi tiết]` `[Khôi phục]`.
> - Nút `[Xóa]` chỉ hiển thị với SP đang hoạt động.
> - Cột Tồn kho của SP đã ẩn hiển thị "—" (không có nghĩa khi đã ẩn).

### 3.3 Wireframe — Dialog Thêm / Sửa hàng hóa

```
┌──────────────────────── Thêm hàng hóa ─────────────────────────────┐
│                                                                      │
│  Mã hàng (tự động)  [ HH000004  — readonly ]                        │
│  ──────────────────────────────────────────────────────────────────  │
│  Tên hàng hóa *     [ Nước khoáng Lavie 500ml               ]      │
│                                                                      │
│  ┌──────────────────────────────┐  ┌───────────────────────────┐    │
│  │ Đơn vị tính *                │  │ Danh mục                  │    │
│  │ [ chai               ▼ ]     │  │ [ Nước giải khát    ▼ ]  │    │
│  └──────────────────────────────┘  └───────────────────────────┘    │
│                                                                      │
│  Giá bán *          [ 8.000                   ] đ                   │
│                                                                      │
│  Ghi chú            [ Hàng nhập thứ 3 hàng tuần              ]     │
│                                                                      │
│  ────────────────────────────────────────────────────────────────── │
│                                          [ Hủy ]  [ 💾 Lưu ]       │
└──────────────────────────────────────────────────────────────────────┘
```

> **Lưu ý:**
> - Lỗi validation hiện **inline** ngay dưới trường bị lỗi (không dùng popup).
> - Khi **Sửa**: tiêu đề đổi thành "Sửa hàng hóa", Mã hàng luôn readonly.
> - Nút `[Lưu]` disabled khi form đang có lỗi validation.
> - **Không có trường Giá vốn** — giá vốn chỉ thay đổi qua phiếu nhập hàng.

### 3.4 Wireframe — Màn hình Chi tiết hàng hóa

```
┌───────────────────────────────────────────────────────────────────┐
│  ← Quay lại          CHI TIẾT HÀNG HÓA          [Sửa thông tin]  │
├───────────────────────────────────────────────────────────────────┤
│  HH000001 · Nước khoáng Lavie 500ml                               │
│  📦 Đơn vị: chai  •  🏷 Danh mục: Nước giải khát  •  Tồn: 120   │
│  💰 Giá bán hiện tại: 8.000 đ                                     │
│  📝 Ghi chú: Hàng nhập thứ 3 hàng tuần                           │
├───────────────────────────────────────────────────────────────────┤
│  LỊCH SỬ GIÁ BÁN                                                   │
├──────────────────────┬────────────────────────┬───────────────────┤
│ Giá cũ               │ Giá mới                │ Thời điểm         │
├──────────────────────┼────────────────────────┼───────────────────┤
│ 7.500 đ              │ 8.000 đ                │ 15/07/2026 10:30  │
│ 7.000 đ              │ 7.500 đ                │ 01/06/2026 08:15  │
└──────────────────────┴────────────────────────┴───────────────────┘
  (Hiện "Chưa có lịch sử thay đổi giá" nếu chưa từng thay đổi)
```

### 3.5 Wireframe — Màn hình Phiếu điều chỉnh kho

```
┌──────────────────────────────────────────────────────────────────────────────────────────────┐
│  ← Quay lại            PHIẾU ĐIỀU CHỈNH KHO · DC000001              Ngày: 19/07/2026        │
├───────────────────────┬──────────────┬────────────────┬───────────┬─────────────────┬────────┤
│ Tên / Mã hàng         │ Tồn PM       │ Tồn thực tế    │ Chênh lệch│ Lý do           │ Giá vốn│
├───────────────────────┼──────────────┼────────────────┼───────────┼─────────────────┼────────┤
│ Nước Lavie/HH000001   │ 120 chai     │ [  120  ]      │  0        │                 │        │
│ Bánh mì/HH000002      │ 30 cái       │ [   28  ]      │ 🔴 -2    │ [ Bể vỡ      ] │        │
│ Hàng X/HH000003       │ 0 cái        │ [    5  ]      │ 🟢 +5    │ [ Nhập sót   ] │[25.000]│
├───────────────────────┴──────────────┴────────────────┴───────────┴─────────────────┴────────┤
│  🟡 2 sản phẩm có chênh lệch                                [Hủy]  [ 💾 Lưu phiếu ]          │
└──────────────────────────────────────────────────────────────────────────────────────────────┘
```

> **Lưu ý:**
> - Cột "Tồn thực tế" mặc định = Tồn phần mềm (không thay đổi = bỏ qua dòng đó).
> - Cột "Chênh lệch" tự tính realtime, locked: 🔴 âm / 🟢 dương / trắng = 0.
> - Dòng có Chênh lệch ≠ 0 được **highlight** màu nền nhạt.
> - Cột "Giá vốn" chỉ hiện input khi Chênh lệch > 0.
> - Nút `[Lưu phiếu]` disabled khi có dòng lệch chưa điền Lý do / Giá vốn.

### 3.6 Chi tiết các thành phần UI

| Thành phần              | Loại control                  | Hành vi                                                                  |
| ----------------------- | ----------------------------- | ------------------------------------------------------------------------ |
| Ô tìm kiếm              | `TextField`                   | Debounce 200ms. Khớp Tên / Mã hàng (case-insensitive).                  |
| Dropdown Danh mục       | `ComboBox`                    | Hiện tất cả danh mục active + "Tất cả". Mặc định: Tất cả.              |
| Dropdown Trạng thái     | `ComboBox`                    | Đang bán / Đã ẩn / Tất cả. Mặc định: Đang bán.                         |
| Bảng danh sách SP       | `TableView`                   | Cuộn vô tận. Click dòng để chọn.                                         |
| Nút `[+ Thêm]`          | `Button`                      | Mở dialog Thêm SP.                                                       |
| Nút `[Kiểm kho]`        | `Button`                      | Mở màn hình Phiếu điều chỉnh kho.                                       |
| Nút `[Import]`          | `Button`                      | Mở FileChooser → validate → mở màn hình preview.                        |
| Nút `[Export]`          | `Button`                      | Mở FileChooser lưu file → export danh sách hiện tại.                    |
| Nút `[Sửa]`             | `Button` (mỗi dòng)           | Mở dialog Sửa SP, điền sẵn dữ liệu.                                    |
| Nút `[Xóa]`             | `Button` (SP đang bán)        | Kiểm tra stock_qty → popup xác nhận → soft-delete.                      |
| Nút `[Khôi phục]`       | `Button` (SP đã ẩn)           | Set `deleted_at = NULL`.                                                  |
| Dialog form Thêm/Sửa   | `Dialog` + `GridPane`         | Modal. Validate inline. Nút Lưu disabled khi có lỗi.                    |
| Ô Giá bán               | `TextField` + `TextFormatter` | Chỉ nhận chữ số nguyên, không âm.                                       |
| Bảng kiểm kho           | `TableView` editable          | Cột Tồn thực tế: `TextField` chỉ nhận số ≥ 0; Lý do: `TextField`.     |
| Cột Chênh lệch          | `TableCell` (computed)        | Tự tính realtime; màu đỏ nếu < 0, xanh nếu > 0, trắng nếu = 0.        |


---

## 4. Data Models & State

### 4.1 Bảng Database — `products` (cập nhật so với schema v1.0)

```sql
CREATE TABLE IF NOT EXISTS products (
    id          INTEGER PRIMARY KEY,
    code        TEXT    NOT NULL UNIQUE,     -- "HH000001", tự sinh sau khi INSERT
    name        TEXT    NOT NULL,
    unit_id     INTEGER NOT NULL REFERENCES units(id)      ON DELETE RESTRICT,
    category_id INTEGER          REFERENCES categories(id) ON DELETE SET NULL,
    sale_price  INTEGER NOT NULL DEFAULT 0,  -- đồng (giá bán hiện tại, ≥ 0)
    stock_qty   REAL    NOT NULL DEFAULT 0,  -- tồn kho (cache; cập nhật qua transaction)
    note        TEXT,
    deleted_at  TEXT,                        -- NULL = đang bán | NOT NULL = đã ẩn
    created_at  TEXT    NOT NULL,
    updated_at  TEXT    NOT NULL,
    CHECK (sale_price >= 0)
) STRICT;
```

> ⚠️ **Thay đổi so với `database-schema.md` v1.0:**
> - **Bỏ** cột `cost_price` — giá vốn chỉ tồn tại trên `inventory_batches` (xem [mua-hang/spec.md](../mua-hang/spec.md)).
> - **Đổi** cột `status TEXT` → `deleted_at TEXT` (soft delete bằng timestamp, không phải flag).
> - **Thêm** cột `note TEXT`.
> Yêu cầu migration script `v1.x → v1.x+1` (xem §4.6).

**Index:**
```sql
CREATE INDEX IF NOT EXISTS idx_products_name     ON products(name);
CREATE INDEX IF NOT EXISTS idx_products_category ON products(category_id);
CREATE INDEX IF NOT EXISTS idx_products_deleted  ON products(deleted_at);  -- lọc active/deleted
```

---

### 4.2 Bảng Database — `price_history` (mới)

```sql
CREATE TABLE IF NOT EXISTS price_history (
    id          INTEGER PRIMARY KEY,
    product_id  INTEGER NOT NULL REFERENCES products(id) ON DELETE RESTRICT,
    old_price   INTEGER NOT NULL,   -- đồng (giá bán trước khi thay đổi)
    new_price   INTEGER NOT NULL,   -- đồng (giá bán sau khi thay đổi)
    changed_at  TEXT    NOT NULL    -- ISO local datetime
) STRICT;
```

**Index:**
```sql
CREATE INDEX IF NOT EXISTS idx_price_history_product ON price_history(product_id);
```

> Chỉ ghi 1 bản ghi khi `new_price ≠ old_price`. Trường hợp giá bán không đổi → không INSERT.

---

### 4.3 Bảng Database — `stock_adjustments` + `stock_adjustment_items` (mới)

```sql
CREATE TABLE IF NOT EXISTS stock_adjustments (
    id              INTEGER PRIMARY KEY,
    code            TEXT    NOT NULL UNIQUE,  -- "DC000001", tự sinh
    adjustment_date TEXT    NOT NULL,         -- ISO local date
    note            TEXT,                     -- ghi chú chung cho cả phiếu (tuỳ chọn)
    created_at      TEXT    NOT NULL
) STRICT;

CREATE TABLE IF NOT EXISTS stock_adjustment_items (
    id              INTEGER PRIMARY KEY,
    adjustment_id   INTEGER NOT NULL REFERENCES stock_adjustments(id) ON DELETE CASCADE,
    product_id      INTEGER NOT NULL REFERENCES products(id)          ON DELETE RESTRICT,
    current_qty     REAL    NOT NULL,    -- tồn phần mềm tại thời điểm tạo phiếu (snapshot)
    actual_qty      REAL    NOT NULL,    -- tồn thực tế người dùng đếm được
    variance        REAL    NOT NULL,    -- = actual_qty − current_qty (âm hoặc dương)
    cost_price      INTEGER,             -- giá vốn dôi dư (bắt buộc nếu variance > 0)
    reason          TEXT    NOT NULL,    -- lý do điều chỉnh (bắt buộc)
    CHECK (actual_qty >= 0)
) STRICT;
```

**Index:**
```sql
CREATE INDEX IF NOT EXISTS idx_adj_items_adj     ON stock_adjustment_items(adjustment_id);
CREATE INDEX IF NOT EXISTS idx_adj_items_product ON stock_adjustment_items(product_id);
```

---

### 4.4 Bảng `inventory_batches` — Tham chiếu từ mua-hang/spec.md

Bảng này được định nghĩa đầy đủ tại [mua-hang/spec.md §4.x](../mua-hang/spec.md). Module này chỉ **ghi thêm** lô mới khi kiểm kho lệch dương:

```sql
-- Trích dẫn (không redefine ở đây):
-- inventory_batches.source TEXT: 'PURCHASE' | 'ADJUSTMENT'
-- inventory_batches.ref_id: purchase_item_id hoặc stock_adjustment_item_id
```

---

### 4.5 Logic sinh Mã hàng (`HH000001`)

Mã hàng sinh **sau khi INSERT** để đảm bảo không trùng lặp dù có rollback:

```
Bước 1: INSERT INTO products (...) VALUES (...)       -- code = NULL tạm
Bước 2: id = last_insert_rowid()
Bước 3: code = "HH" + String.format("%06d", id)      -- HH000001 ... HH999999
         (nếu id > 999999: code = "HH" + id)          -- HH1000000...
Bước 4: UPDATE products SET code = ? WHERE id = ?
```

> Cả 4 bước trong **1 TRANSACTION** — rollback thì code không bị chiếm.

Mã phiếu điều chỉnh `DC000001` sinh tương tự từ `stock_adjustments.last_insert_rowid()`.

---

### 4.6 State Changes

**Thêm SP (UC-HH02):**
```
BEGIN TRANSACTION
  INSERT → products (code = NULL, name, unit_id, category_id, sale_price, note, created_at, updated_at)
  UPDATE → products SET code = 'HHxxxxxx' WHERE id = last_insert_rowid()
COMMIT
```

**Sửa SP — Giá bán thay đổi (UC-HH03):**
```
BEGIN TRANSACTION
  INSERT → price_history (product_id, old_price, new_price, changed_at = NOW())
  UPDATE → products SET name, unit_id, category_id, sale_price, note, updated_at = NOW()
COMMIT
```

**Sửa SP — Giá bán không thay đổi (UC-HH03):**
```
UPDATE → products SET name, unit_id, category_id, note, updated_at = NOW()
```

**Xóa mềm (UC-HH04):**
```
UPDATE → products SET deleted_at = NOW() WHERE id = ?
```

**Khôi phục (UC-HH04):**
```
UPDATE → products SET deleted_at = NULL WHERE id = ?
```

**Lưu phiếu điều chỉnh kho (UC-HH06):**
```
BEGIN TRANSACTION
  INSERT → stock_adjustments (code='DCxxxxxx', adjustment_date, note, created_at)
  adj_id = last_insert_rowid()

  FOR EACH dòng có variance ≠ 0:
    INSERT → stock_adjustment_items
             (adj_id, product_id, current_qty, actual_qty, variance, reason, cost_price)
    item_id = last_insert_rowid()

    UPDATE → products
             SET stock_qty = actual_qty, updated_at = NOW()

    INSERT → stock_movements
             (product_id, type='KIEMKHO', qty_change=variance, stock_after=actual_qty,
              ref_type='ADJUSTMENT', ref_id=adj_id, note=reason, created_at=NOW())

    IF variance > 0:
      INSERT → inventory_batches
               (product_id, qty=variance, cost_price=cost_price,
                source='ADJUSTMENT', ref_id=item_id, created_at=NOW())
COMMIT
```

---

### 4.7 ViewModel State

```java
// ProductListViewModel
ObservableList<Product>       allProducts;       // toàn bộ SP đã load
FilteredList<Product>         filteredProducts;  // sau filter + search
StringProperty                searchKeyword;     // bind ô tìm kiếm
ObjectProperty<Category>      filterCategory;   // null = Tất cả
ObjectProperty<ProductStatus> filterStatus;     // ACTIVE | DELETED | ALL

// ProductFormViewModel (dùng chung Thêm và Sửa)
StringProperty           name;
ObjectProperty<Unit>     unit;
ObjectProperty<Category> category;
LongProperty             salePrice;
StringProperty           note;
BooleanProperty          isValid;     // = name khác rỗng AND unit != null AND salePrice >= 0
BooleanProperty          isEditMode;  // true = Sửa, false = Thêm mới

// StockAdjustmentViewModel
ObservableList<AdjustmentRow> rows;     // 1 row per active product
BooleanProperty               canSave;  // có ít nhất 1 dòng variance ≠ 0
                                        //   VÀ tất cả dòng lệch đã hợp lệ
```

```java
// Product (model)
class Product {
    int       id;
    String    code;        // "HH000001"
    String    name;
    int       unitId;
    String    unitName;    // denorm để hiển thị
    Integer   categoryId;  // nullable
    String    categoryName;
    long      salePrice;   // đồng
    double    stockQty;
    String    note;
    String    deletedAt;   // null = đang bán
    String    createdAt;
    String    updatedAt;
}

// AdjustmentRow (VM inner class)
class AdjustmentRow {
    Product            product;
    double             currentQty;   // snapshot lúc mở phiếu
    DoubleProperty     actualQty;    // người dùng nhập, ≥ 0
    ReadOnlyDoubleProperty variance; // = actualQty - currentQty
    StringProperty     reason;       // bắt buộc nếu variance ≠ 0
    LongProperty       costPrice;    // bắt buộc nếu variance > 0
    BooleanProperty    isModified;   // = Math.abs(variance) > 0.0001
    BooleanProperty    isValid;      // = !isModified
                                    //   OR (reason.notBlank
                                    //       AND (variance <= 0 OR costPrice >= 0))
}

enum ProductStatus { ACTIVE, DELETED, ALL }
```


---

## 5. Integration & Architecture

### 5.1 Layer Flow (theo kiến trúc MVVM-lite)

```
┌─────────┐  ┌──────────────────────┐  ┌──────────────────────┐  ┌────────────────────┐  ┌─────────────┐
│  View   │─▶│ ProductController    │─▶│ ProductListViewModel  │─▶│ ProductService     │─▶│ ProductDao  │
│ (FXML)  │  │ (bind + event)       │  │ ProductFormViewModel  │  │ (business logic)   │  │ (SQL thuần) │
└─────────┘  └──────────────────────┘  │ StockAdjViewModel    │  └─────────┬──────────┘  └─────────────┘
                                        └──────────────────────┘            │
                                                           ┌────────────────┼─────────────────────┐
                                                           │                │                     │
                                               ┌───────────────────┐ ┌─────────────────┐ ┌────────────────────┐
                                               │ AdjustmentService │ │ ImportService   │ │ ExportService      │
                                               │ (kiểm kho TX)     │ │ (Excel/CSV read)│ │ (Excel/CSV write)  │
                                               └───────────────────┘ └─────────────────┘ └────────────────────┘
```

### 5.2 Các class liên quan

| Layer      | Class                           | Trách nhiệm                                                                   |
| ---------- | ------------------------------- | ----------------------------------------------------------------------------- |
| View       | `product-list.fxml`             | Danh sách SP: toolbar + TableView                                             |
| View       | `product-form-dialog.fxml`      | Dialog Thêm/Sửa SP (dùng chung)                                              |
| View       | `product-detail.fxml`           | Chi tiết SP + lịch sử giá bán                                                |
| View       | `import-preview.fxml`           | Preview validate kết quả import (tái sử dụng từ module KH)                   |
| View       | `stock-adjustment.fxml`         | Phiếu điều chỉnh kho: bảng nhập liệu + toolbar                               |
| View       | `stock-adjustment-history.fxml` | Danh sách phiếu kiểm kho đã lưu                                              |
| Controller | `ProductListController`         | Bind danh sách, search, filter, điều hướng đến dialog/chi tiết/kiểm kho      |
| Controller | `ProductFormController`         | Bind form Thêm/Sửa, validate inline, gọi ProductService                      |
| Controller | `ProductDetailController`       | Hiển thị chi tiết SP + load lịch sử giá bán                                  |
| Controller | `StockAdjustmentController`     | Bind bảng kiểm kho, tính variance realtime, gọi AdjustmentService            |
| ViewModel  | `ProductListViewModel`          | `FilteredList` + search/filter logic                                         |
| ViewModel  | `ProductFormViewModel`          | State form + validation + `isValid` computed                                  |
| ViewModel  | `StockAdjustmentViewModel`      | `ObservableList<AdjustmentRow>` + `canSave` computed                         |
| Service    | `ProductService`                | `add()`, `update()`, `softDelete()`, `restore()`, `checkDeletable()`         |
| Service    | `AdjustmentService`             | `createAdjustment()` — toàn bộ transaction kiểm kho (6 bước)                 |
| Service    | `ImportService`                 | Đọc file Excel/CSV, validate từng dòng, trả về `ImportResult`                |
| Service    | `ExportService`                 | Xuất `List<Product>` ra file Excel/CSV                                        |
| DAO        | `ProductDao`                    | `insert()`, `update()`, `findAll()`, `findByFilter()`, `setDeleted()`        |
| DAO        | `PriceHistoryDao`               | `insert()`, `findByProduct()`                                                 |
| DAO        | `StockAdjustmentDao`            | `insertAdjustment()`, `insertItem()`, `findAll()` (lịch sử phiếu)            |

### 5.3 Threading Model

```
[JavaFX App Thread]
    │
    ├── Search / Filter: FilteredList.setPredicate() → UI thread (in-memory, nhanh)
    │
    ├── Load danh sách SP: ProductService.findAll() → background Task
    │     └── Platform.runLater() → cập nhật ObservableList
    │
    ├── Thêm / Sửa / Xóa / Khôi phục: ProductService → background Task
    │     └── Platform.runLater() → refresh danh sách + đóng dialog
    │
    ├── Load phiếu kiểm kho: StockAdjustmentViewModel.load() → background Task
    │     └── Platform.runLater() → hiện bảng điều chỉnh
    │
    ├── Lưu phiếu kiểm kho: AdjustmentService.createAdjustment() → background Task
    │     └── Platform.runLater() → thông báo thành công + đóng màn hình
    │
    ├── Import: ImportService.validate() + batch insert → background Task
    │     ├── Báo tiến độ (ProgressIndicator) khi đọc file lớn
    │     └── Platform.runLater() → hiện màn hình preview validate
    │
    └── Export: ExportService.export() → background Task
          └── Platform.runLater() → hiện dialog chọn nơi lưu file
```

### 5.4 Module phụ thuộc

- **Module này phụ thuộc vào:**
  - **Danh mục** (`units`, `categories`) — phải khởi tạo seed dữ liệu trước; ComboBox trong form SP đọc từ đây.

- **Các module khác đọc dữ liệu từ module này:**
  - **Bán hàng** đọc `products` (`deleted_at IS NULL`) để hiển thị SP trong ComboBox POS.
  - **Mua hàng** đọc `products` để chọn SP khi tạo phiếu nhập; sau khi nhập → cập nhật `products.stock_qty` trong cùng transaction.
  - **Dashboard** đọc `products.stock_qty` để cảnh báo hàng sắp hết kho.

- **Thư viện:**
  - Import/Export Excel: **Apache POI** (`poi-ooxml`).
  - Import/Export CSV: **OpenCSV** hoặc xử lý thủ công.


---

## 6. Edge Cases & Risks

### 6.1 Edge Cases

| #   | Tình huống                                                               | Xử lý                                                                                                         |
| --- | ------------------------------------------------------------------------ | ------------------------------------------------------------------------------------------------------------- |
| E1  | Xóa mềm SP còn tồn kho (`stock_qty > 0`)                                | Chặn, hiện dialog: "Không thể ẩn — hàng hóa còn [X] đơn vị trong kho"                                       |
| E2  | Xóa mềm SP đã xuất hiện trong hóa đơn cũ                                | Cho phép — `invoice_items` giữ `product_id`; query JOIN vẫn trả về tên SP (kể cả SP đã ẩn)                   |
| E3  | SP bị ẩn xuất hiện trong ComboBox bán hàng (POS)                         | Không hiển thị SP có `deleted_at IS NOT NULL` — query WHERE `deleted_at IS NULL`                              |
| E4  | Sửa giá bán về đúng giá cũ (không thay đổi)                             | Không INSERT vào `price_history` — chỉ ghi khi `new_price ≠ old_price`                                       |
| E5  | Kiểm kho: người dùng nhập Tồn thực tế < 0                               | Chặn ngay tại input: "Tồn thực tế không được âm"                                                             |
| E6  | Kiểm kho: toàn bộ Variance đều = 0 (không có gì thay đổi)               | Thông báo: "Không có hàng hóa nào thay đổi", nút [Lưu phiếu] disabled                                       |
| E7  | Kiểm kho lệch dương: người dùng nhập Giá vốn = 0                        | Cho phép (hàng được tặng/biếu, không có chi phí)                                                             |
| E8  | Kiểm kho khi đang có giao dịch bán hàng song song                        | MVP có 1 user — không có concurrency thực sự. `current_qty` là snapshot lúc mở phiếu; lưu phiếu ghi đè `stock_qty = actual_qty`. Chấp nhận last-write-wins. |
| E9  | Đơn vị bị xóa sau khi đã gắn vào SP                                     | `unit_id ON DELETE RESTRICT` — DB chặn xóa Đơn vị nếu còn SP đang dùng; báo lỗi lên Service                 |
| E10 | Danh mục bị xóa sau khi đã gắn vào SP                                   | `category_id ON DELETE SET NULL` — SP mất danh mục nhưng vẫn hoạt động bình thường                           |
| E11 | Import: Đơn vị trong file không tồn tại trong hệ thống                   | Báo lỗi: "Dòng X: Đơn vị '[tên]' không tồn tại — vui lòng tạo trong module Danh mục trước"                  |
| E12 | Import: Mã hàng bỏ trống trong file                                      | Không báo lỗi mã — hệ thống tự sinh mã mới khi import                                                        |
| E13 | Import: Mã hàng trong file trùng với SP đã có trong DB                   | Đánh dấu lỗi dòng đó, bỏ qua; các dòng hợp lệ vẫn import bình thường                                        |
| E14 | Import: File rỗng (0 dòng dữ liệu)                                       | Hiện thông báo: "File không có dữ liệu để import"                                                            |
| E15 | Export khi danh sách đang rỗng (filter không có kết quả)                 | Thông báo: "Không có dữ liệu để xuất"                                                                        |
| E16 | Tên SP chỉ toàn khoảng trắng                                             | Trim trước khi validate — coi là rỗng, báo lỗi "Tên hàng hóa không được để trống"                           |
| E17 | Giá bán = 0                                                              | Cho phép (hàng mẫu, hàng tặng kèm)                                                                           |
| E18 | Mã hàng bị gap sau rollback (HH000003 không tồn tại)                     | Chấp nhận gap — mã chỉ cần unique, không cần liên tục                                                        |

### 6.2 Risks & Mitigations

| Rủi ro                                                              | Mức độ     | Giảm thiểu                                                                                    |
| ------------------------------------------------------------------- | ---------- | --------------------------------------------------------------------------------------------- |
| `stock_qty` bị desync với tổng `inventory_batches.qty`              | Trung bình | Cả hai luôn cập nhật trong **1 TRANSACTION**; không có đường code nào update độc lập         |
| Nhân viên điều chỉnh kho để xóa dấu vết gian lận                   | Cao        | MVP: 1 user duy nhất là chủ cửa hàng. Giai đoạn 2: thêm phân quyền + approval workflow      |
| Import file lớn (nghìn dòng) gây đơ UI                              | Trung bình | Chạy trên background Task, hiện `ProgressIndicator`                                           |
| File import CSV có encoding khác UTF-8                              | Thấp       | Detect encoding hoặc ghi rõ trong UI: "Lưu file CSV dạng UTF-8"                              |
| Xóa Đơn vị đang được dùng bởi SP                                    | Thấp       | `ON DELETE RESTRICT` trên `unit_id` — DB chặn, Service bắt exception và báo lỗi cụ thể       |
| Mất lịch sử giá khi xóa SP                                          | Thấp       | `price_history.product_id ON DELETE RESTRICT` — không cho hard-delete SP; soft-delete không ảnh hưởng |


---

## 7. Decision Log

| #  | Quyết định                                                                           | Lý do                                                                                            |
| -- | ------------------------------------------------------------------------------------ | ------------------------------------------------------------------------------------------------ |
| D1 | Soft delete dùng `deleted_at = NOW()` thay vì `status TEXT`                          | Lưu chính xác thời điểm xóa; dễ query "SP ẩn trong 7 ngày qua"; phổ biến ở các hệ thống lớn    |
| D2 | Không hiển thị Giá vốn trên màn hình SP                                              | Với FIFO batch, mỗi lô có giá vốn khác nhau — một con số "giá vốn SP" sẽ gây hiểu nhầm nghiêm trọng |
| D3 | Giá bán readonly trong form, sửa riêng qua nút — không; thực ra sửa giá bán nằm trong form Sửa, nhưng ghi `price_history` tự động | Tách biệt rõ hành động "sửa thông tin" vs "thay đổi giá"; audit trail tự động, không phụ thuộc người dùng nhớ ghi |
| D4 | Kiểm kho 1 bước (không có nháp/duyệt)                                                | MVP chỉ có 1 user là chủ cửa hàng — không cần approval workflow; tiết kiệm scope đáng kể        |
| D5 | Bảng riêng `stock_adjustments` + `stock_adjustment_items`                             | Nhóm nhiều dòng điều chỉnh thành 1 phiếu có mã `DC000001`; dễ xem lịch sử; không làm rối `stock_movements` |
| D6 | Kiểm kho lệch dương: tạo `inventory_batch` mới với `source='ADJUSTMENT'`              | Duy trì tính toàn vẹn FIFO — hàng dôi dư có giá vốn riêng, không làm sai giá vốn các lô cũ    |
| D7 | Chặn xóa mềm nếu `stock_qty > 0`                                                     | Tránh "ghost inventory" — hàng đang có tồn mà bị ẩn sẽ gây sai lệch báo cáo                    |
| D8 | Lịch sử giá bán chỉ ghi khi `new_price ≠ old_price`                                  | Tránh ghi bản ghi thừa; `old_price = new_price` không có giá trị audit                          |
| D9 | Import trùng mã → báo lỗi, không ghi đè                                              | An toàn — tránh ghi đè ngoài ý muốn; người dùng phải xử lý thủ công khi cần update             |
| D10| Mã hàng format `HH000001` (6 chữ số)                                                  | Nhất quán với pattern hệ thống (`HD`, `PN`, `DC`...); 6 chữ số đủ cho 999.999 sản phẩm          |
