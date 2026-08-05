# Spec: Danh mục (Đơn vị tính & Nhóm hàng)

> Module: phase-1-mvp/danh-muc · Phiên bản: 1.0 · Ngày: 2026-07-09
> Tham chiếu: [MVP-plan.md](../../MVP-plan.md) · [architecture.md](../../architecture.md) · [database-schema.md](../../database-schema.md)

---

## 1. Executive Summary

Module **Danh mục** là nền tảng dữ liệu dùng chung cho toàn bộ hệ thống. Trước khi tạo bất kỳ sản phẩm nào, chủ cửa hàng cần thiết lập ít nhất một đơn vị tính. Module gồm hai danh mục con:

- **Đơn vị tính** (`units`): Bắt buộc — mỗi sản phẩm phải chọn 1 đơn vị (cái, kg, lít, thùng...). Được dùng trong Hàng hóa, Nhập hàng, Bán hàng và Hóa đơn PDF.
- **Nhóm/loại hàng** (`categories`): Tùy chọn — giúp phân loại sản phẩm theo nhóm (Nước uống, Thực phẩm...). Không bắt buộc khi tạo sản phẩm.

Cả hai danh mục được quản lý tập trung tại màn hình **Cài đặt** (sidebar → Cài đặt → tab Đơn vị / tab Nhóm hàng). Người dùng tự tạo danh mục theo thực tế cửa hàng — không có dữ liệu seed mặc định.

Thao tác hỗ trợ: thêm / sửa / xóa mềm (`status = INACTIVE`). Không xóa cứng khi danh mục đang được sản phẩm sử dụng.

---

## 2. Requirements & Use Cases

### 2.1 Functional Requirements

| ID    | Yêu cầu                                                                          | Áp dụng cho       | Độ ưu tiên |
| ----- | -------------------------------------------------------------------------------- | ------------------ | ---------- |
| FR-01 | Hiển thị danh sách đơn vị tính với thanh tìm kiếm theo tên                       | Đơn vị tính        | Bắt buộc   |
| FR-02 | Thêm đơn vị tính mới (tên, status mặc định ACTIVE)                               | Đơn vị tính        | Bắt buộc   |
| FR-03 | Sửa tên đơn vị tính                                                               | Đơn vị tính        | Bắt buộc   |
| FR-04 | Xóa mềm đơn vị tính (chuyển status = INACTIVE)                                   | Đơn vị tính        | Bắt buộc   |
| FR-05 | Không cho xóa đơn vị đang được ít nhất 1 sản phẩm sử dụng (FK RESTRICT)         | Đơn vị tính        | Bắt buộc   |
| FR-06 | Hiển thị danh sách nhóm hàng với thanh tìm kiếm theo tên                         | Nhóm hàng          | Bắt buộc   |
| FR-07 | Thêm nhóm hàng mới (tên, status mặc định ACTIVE)                                 | Nhóm hàng          | Bắt buộc   |
| FR-08 | Sửa tên nhóm hàng                                                                 | Nhóm hàng          | Bắt buộc   |
| FR-09 | Xóa mềm nhóm hàng (chuyển status = INACTIVE)                                     | Nhóm hàng          | Bắt buộc   |
| FR-10 | Không cho xóa nhóm hàng đang được ít nhất 1 sản phẩm sử dụng                    | Nhóm hàng          | Bắt buộc   |
| FR-11 | Tên đơn vị / nhóm hàng phải là duy nhất (UNIQUE), không phân biệt hoa thường     | Cả hai             | Bắt buộc   |
| FR-12 | Validation: tên không được để trống                                               | Cả hai             | Bắt buộc   |

### 2.2 Non-functional Requirements

| ID     | Yêu cầu                                                                              |
| ------ | ------------------------------------------------------------------------------------ |
| NFR-01 | Thao tác CRUD phản hồi nhanh (< 300ms) — dataset nhỏ (vài chục mục)                 |
| NFR-02 | Tìm kiếm real-time theo từng ký tự gõ (không cần nút Search)                        |
| NFR-03 | Danh sách chỉ hiển thị các mục có `status = ACTIVE` theo mặc định                   |

### 2.3 Use Cases

#### UC-07a — Quản lý Đơn vị tính

- **Actor:** Chủ cửa hàng
- **Tiền điều kiện:** Đã đăng nhập. Đang ở màn hình Cài đặt → tab Đơn vị.

**Luồng chính — Thêm đơn vị:**

```
1. Người dùng bấm [+ Thêm đơn vị].
2. Hệ thống hiển thị dialog: ô nhập tên đơn vị.
3. Người dùng nhập tên (vd: "thùng") → bấm [Lưu].
4. Hệ thống validation:
   → Tên khác rỗng VÀ chưa tồn tại (UNIQUE) → tiếp tục.
5. INSERT vào bảng `units` (name, status='ACTIVE').
6. Danh sách tự cập nhật hiển thị đơn vị mới.
```

**Luồng chính — Sửa đơn vị:**

```
1. Người dùng bấm [✏️ Sửa] trên dòng đơn vị cần sửa.
2. Hệ thống mở dialog, điền sẵn tên hiện tại vào ô nhập.
3. Người dùng chỉnh tên → bấm [Lưu].
4. Hệ thống validation (tên khác rỗng, không trùng với đơn vị khác).
5. UPDATE units SET name = ? WHERE id = ?
6. Danh sách cập nhật ngay.
```

**Luồng chính — Xóa đơn vị:**

```
1. Người dùng bấm [🗑️ Xóa] trên dòng đơn vị cần xóa.
2. Hệ thống kiểm tra: có sản phẩm nào đang dùng đơn vị này không?
   → Có → hiện cảnh báo: "Không thể xóa. Đơn vị đang được [N] sản phẩm sử dụng."
   → Không → hiện popup xác nhận: "Xóa đơn vị '[tên]'?"
3. Người dùng xác nhận [Xóa].
4. UPDATE units SET status = 'INACTIVE' WHERE id = ?
5. Dòng đó biến mất khỏi danh sách (vì chỉ hiển thị ACTIVE).
```

**Luồng ngoại lệ:**

| Bước | Điều kiện                  | Xử lý                                                              |
| ---- | -------------------------- | ------------------------------------------------------------------ |
| 4    | Tên để trống               | Disable nút [Lưu] (hoặc inline error: "Tên không được để trống")  |
| 4    | Tên đã tồn tại (UNIQUE)    | Inline error: "Tên đơn vị đã tồn tại, vui lòng chọn tên khác."   |

#### UC-07b — Quản lý Nhóm hàng

Tương tự UC-07a, thay `units` bằng `categories` và thay "Đơn vị" bằng "Nhóm hàng". Các luồng và validation hoàn toàn đồng nhất.

---

## 3. UI/UX & Navigation

### 3.1 Luồng điều hướng

```
[Sidebar] → [⚙️ Cài đặt]
    │
    ├── [Tab: Đơn vị tính]   ← mặc định khi vào Cài đặt
    └── [Tab: Nhóm hàng]
```

> Màn hình Cài đặt là container chứa nhiều tab. Danh mục nằm ở 2 tab đầu tiên.
> Các tab khác (ví dụ: Thông tin cửa hàng, Đổi mật khẩu) thuộc spec riêng.

### 3.2 Wireframe — Tab Đơn vị tính

```
┌──────────────────────────────────────────────────────────┐
│  Cài đặt                                                  │
├─────────────┬────────────────────────────────────────────┤
│ Đơn vị tính │ Nhóm hàng │ Thông tin cửa hàng │ ...       │
├─────────────┴────────────────────────────────────────────┤
│                                                            │
│  🔍 [ tìm đơn vị...              ]      [+ Thêm đơn vị]  │
│                                                            │
│  ┌──────────────────────────────────────────────────────┐ │
│  │  Tên đơn vị              Trạng thái    Thao tác      │ │
│  │  cái                     ACTIVE        ✏️  🗑️        │ │
│  │  hộp                     ACTIVE        ✏️  🗑️        │ │
│  │  kg                      ACTIVE        ✏️  🗑️        │ │
│  │  lít                     ACTIVE        ✏️  🗑️        │ │
│  │  ...                                                  │ │
│  └──────────────────────────────────────────────────────┘ │
│                                          Tổng: 6 đơn vị   │
└──────────────────────────────────────────────────────────┘
```

### 3.3 Wireframe — Dialog Thêm / Sửa

```
┌──────────────── Thêm đơn vị tính ─────────────────┐
│                                                     │
│  Tên đơn vị: [ thùng                             ] │
│              (vd: cái, hộp, kg, lít, thùng...)     │
│                                                     │
│  ⚠️ Tên đơn vị đã tồn tại.    ← inline error       │
│                                                     │
│                      [ Hủy ]   [ Lưu ]             │
└─────────────────────────────────────────────────────┘
```

> Dialog sửa có tiêu đề "Sửa đơn vị tính" và điền sẵn tên hiện tại.
> Dialog Nhóm hàng tương tự, chỉ khác tiêu đề và placeholder.

### 3.4 Chi tiết thành phần UI

| Thành phần             | Loại control      | Hành vi                                                             |
| ---------------------- | ----------------- | ------------------------------------------------------------------- |
| Tab control            | `TabPane`         | 2 tab: Đơn vị tính / Nhóm hàng. Tab mặc định: Đơn vị tính.        |
| Thanh tìm kiếm         | `TextField`       | Lọc real-time danh sách khi gõ (filter trên `FilteredList`).       |
| Nút [+ Thêm]           | `Button`          | Mở dialog thêm mới.                                                 |
| Bảng danh sách         | `TableView`       | Cột: Tên — Trạng thái — Thao tác. Hiển thị ACTIVE.                 |
| Nút [✏️ Sửa]           | `Button` (cell)   | Mở dialog sửa, điền sẵn dữ liệu.                                   |
| Nút [🗑️ Xóa]          | `Button` (cell)   | Kiểm tra FK → popup xác nhận hoặc cảnh báo không cho xóa.          |
| Dialog thêm/sửa        | `Dialog<String>`  | Ô nhập tên + inline error. Nút [Lưu] disabled khi ô trống.         |
| Label tổng             | `Label`           | "Tổng: N đơn vị / nhóm hàng" hiển thị ở dưới bảng.                |

### 3.5 Thông báo lỗi & xác nhận

| Tình huống                               | Loại thông báo     | Nội dung                                                       |
| ---------------------------------------- | ------------------ | -------------------------------------------------------------- |
| Tên để trống khi lưu                    | Inline error       | "Tên không được để trống."                                     |
| Tên đã tồn tại (UNIQUE constraint)      | Inline error       | "Tên đã tồn tại, vui lòng chọn tên khác."                     |
| Xóa đơn vị đang dùng bởi sản phẩm      | Alert WARNING      | "Không thể xóa. Đơn vị đang được [N] sản phẩm sử dụng."      |
| Xóa đơn vị/nhóm chưa dùng             | Popup xác nhận     | "Xóa đơn vị '[tên]'? Thao tác này không thể hoàn tác."        |
| Thêm/sửa thành công                     | (không cần popup)  | Danh sách tự cập nhật — đủ phản hồi.                           |

---

## 4. Data Models & State

### 4.1 Bảng Database liên quan

```sql
-- Đơn vị tính
CREATE TABLE IF NOT EXISTS units (
    id      INTEGER PRIMARY KEY,
    name    TEXT NOT NULL UNIQUE,
    status  TEXT NOT NULL DEFAULT 'ACTIVE'   -- ACTIVE | INACTIVE
) STRICT;

-- Nhóm/loại hàng
CREATE TABLE IF NOT EXISTS categories (
    id      INTEGER PRIMARY KEY,
    name    TEXT NOT NULL UNIQUE,
    status  TEXT NOT NULL DEFAULT 'ACTIVE'   -- ACTIVE | INACTIVE
) STRICT;
```

> **Lưu ý:** Schema gốc trong [database-schema.md](../../database-schema.md) chưa có cột `status` cho `categories`. Cần bổ sung cột này khi tạo schema.

### 4.2 State Changes

**Thêm mới:**
```sql
INSERT INTO units(name, status) VALUES (?, 'ACTIVE');
INSERT INTO categories(name, status) VALUES (?, 'ACTIVE');
```

**Sửa tên:**
```sql
UPDATE units SET name = ? WHERE id = ?;
UPDATE categories SET name = ? WHERE id = ?;
```

**Xóa mềm:**
```sql
UPDATE units SET status = 'INACTIVE' WHERE id = ?;
UPDATE categories SET status = 'INACTIVE' WHERE id = ?;
```

**Kiểm tra trước khi xóa (đơn vị tính):**
```sql
SELECT COUNT(*) FROM products WHERE unit_id = ? AND status = 'ACTIVE';
-- > 0 → chặn xóa, hiện cảnh báo
```

**Kiểm tra trước khi xóa (nhóm hàng):**
```sql
SELECT COUNT(*) FROM products WHERE category_id = ? AND status = 'ACTIVE';
-- > 0 → chặn xóa, hiện cảnh báo
```

**Truy vấn danh sách (chỉ hiển thị ACTIVE):**
```sql
SELECT id, name, status FROM units WHERE status = 'ACTIVE' ORDER BY name;
SELECT id, name, status FROM categories WHERE status = 'ACTIVE' ORDER BY name;
```

### 4.3 Model (POJO)

```java
// com.shop.model.Unit.java
public class Unit {
    private int id;
    private String name;
    private String status; // "ACTIVE" | "INACTIVE"
    // getters, setters
}

// com.shop.model.Category.java
public class Category {
    private int id;
    private String name;
    private String status;
    // getters, setters
}
```

---

## 5. Integration & Architecture

### 5.1 Layer Flow (theo kiến trúc MVVM-lite)

```
┌──────────────┐    ┌──────────────────────┐    ┌────────────────────┐    ┌──────────────────┐
│  View (FXML) │───▶│SettingsController    │───▶│  CategoryService   │───▶│  UnitDao         │
│              │    │(bind TabPane + event)│    │  UnitService       │    │  CategoryDao     │
└──────────────┘    └──────────────────────┘    └────────────────────┘    └──────────────────┘
```

> **Màn hình CRUD đơn giản** → Controller kiêm luôn ViewModel (theo quy tắc "lite" trong [architecture.md](../../architecture.md)). Không cần tách ViewModel riêng.

### 5.2 Các class liên quan

| Layer      | Class / File                | Trách nhiệm                                                              |
| ---------- | --------------------------- | ------------------------------------------------------------------------ |
| View       | `settings-view.fxml`        | Layout TabPane chứa tab Đơn vị + tab Nhóm hàng                          |
| View       | `unit-tab.fxml`             | TableView đơn vị + thanh tìm kiếm + nút Thêm                            |
| View       | `category-tab.fxml`         | TableView nhóm hàng + thanh tìm kiếm + nút Thêm                         |
| Controller | `SettingsController`        | Điều hướng giữa các tab                                                  |
| Controller | `UnitTabController`         | Bind TableView, xử lý sự kiện Thêm/Sửa/Xóa đơn vị, gọi `UnitService`  |
| Controller | `CategoryTabController`     | Bind TableView, xử lý sự kiện Thêm/Sửa/Xóa nhóm, gọi `CategoryService` |
| Service    | `UnitService`               | `findAllActive()`, `add(name)`, `update(id, name)`, `softDelete(id)`     |
| Service    | `CategoryService`           | `findAllActive()`, `add(name)`, `update(id, name)`, `softDelete(id)`     |
| DAO        | `UnitDao`                   | SQL CRUD cho bảng `units`                                                |
| DAO        | `CategoryDao`               | SQL CRUD cho bảng `categories`                                           |
| Model      | `Unit`, `Category`          | POJO map với bảng DB                                                     |

### 5.3 Tìm kiếm Real-time với FilteredList

```java
// Trong UnitTabController
ObservableList<Unit> allUnits = FXCollections.observableArrayList();
FilteredList<Unit> filteredUnits = new FilteredList<>(allUnits, u -> true);

searchField.textProperty().addListener((obs, old, text) -> {
    filteredUnits.setPredicate(u ->
        text.isBlank() || u.getName().toLowerCase().contains(text.toLowerCase())
    );
});

tableView.setItems(filteredUnits);
```

> Tìm kiếm chỉ filter trên danh sách đã load trong bộ nhớ — không gọi DB lại mỗi lần gõ. Đủ hiệu quả với dataset nhỏ (vài chục mục).

### 5.4 Module phụ thuộc

- **Đầu vào:** Không phụ thuộc module nghiệp vụ nào.
- **Đầu ra (modules khác phụ thuộc vào Danh mục):**
  - **Hàng hóa:** Dropdown "Đơn vị" và "Nhóm hàng" khi thêm/sửa sản phẩm đọc từ `units` và `categories`.
  - **Bán hàng / Nhập hàng:** Hiển thị đơn vị tính của sản phẩm trên hóa đơn và PDF.
  - **Hóa đơn PDF:** Cột ĐVT in đơn vị tính của từng dòng sản phẩm.

---

## 6. Edge Cases & Risks

### 6.1 Edge Cases

| #  | Tình huống                                                    | Xử lý                                                                         |
| -- | ------------------------------------------------------------- | ------------------------------------------------------------------------------ |
| E1 | Xóa đơn vị đang được sản phẩm sử dụng                        | Chặn xóa, hiện cảnh báo. Không dùng FK RESTRICT ở DB (vì soft-delete, không cascade); kiểm tra ở tầng Service. |
| E2 | Xóa nhóm hàng đang được sản phẩm sử dụng                     | Tương tự E1. Nếu xóa nhóm hàng, `products.category_id` → SET NULL (schema đã định nghĩa). |
| E3 | Đặt tên trùng chính xác (case-sensitive)                      | SQLite UNIQUE mặc định case-insensitive với ASCII. Validate UNIQUE ở tầng Service trước khi INSERT để cho thông báo thân thiện thay vì lỗi exception. |
| E4 | Đặt tên chỉ toàn khoảng trắng                                 | Trim tên trước khi lưu. Sau trim rỗng → báo lỗi "Tên không được để trống".    |
| E5 | Tên quá dài (vd: 200 ký tự)                                   | Giới hạn `maxLength` trên `TextField` (vd: 100 ký tự). Đủ dùng cho tên đơn vị thực tế. |
| E6 | Người dùng bấm [Lưu] nhiều lần liên tiếp (double-click)       | Disable nút [Lưu] sau lần click đầu, enable lại sau khi xử lý xong.           |
| E7 | Tên đơn vị "INACTIVE" tồn tại, người dùng muốn tạo lại       | Cho phép tái kích hoạt bằng cách UPDATE status = 'ACTIVE' thay vì INSERT mới. MVP: đơn giản hóa — chỉ thêm mới. Nếu trùng tên (kể cả INACTIVE), báo lỗi "Tên đã tồn tại". |

### 6.2 Risks & Mitigations

| Rủi ro                                               | Mức độ | Giảm thiểu                                                                 |
| ---------------------------------------------------- | ------ | --------------------------------------------------------------------------- |
| Xóa mềm nhóm hàng → sản phẩm mất nhóm (SET NULL)   | Thấp   | Sản phẩm vẫn hoạt động bình thường, chỉ không có nhóm. Chấp nhận ở MVP.   |
| Người dùng quên tạo đơn vị → không tạo được SP      | Thấp   | Màn hình Hàng hóa (thêm SP) hiển thị hướng dẫn nếu dropdown đơn vị trống: "Vui lòng thêm đơn vị tính trong Cài đặt." |
| Trùng tên do khoảng trắng ẩn (vd: "kg " vs "kg")   | Thấp   | Trim toàn bộ input trước khi validate và lưu (E4).                         |

---

## 7. Decision Log (Quyết định đã chốt trong spec này)

| #  | Quyết định                                                       | Lý do                                                                    |
| -- | ---------------------------------------------------------------- | ------------------------------------------------------------------------ |
| D1 | Cấu trúc phẳng (không phân cấp cha/con) cho cả 2 danh mục      | Đủ dùng cho cửa hàng nhỏ; phân cấp thêm độ phức tạp không cần thiết    |
| D2 | Xóa mềm bằng `status = INACTIVE` thay vì DELETE cứng            | Giữ tính toàn vẹn lịch sử — sản phẩm đã tạo vẫn tham chiếu đơn vị cũ  |
| D3 | Không có seed data mặc định                                      | Cửa hàng mỗi nơi dùng đơn vị/nhóm khác nhau; không áp đặt             |
| D4 | Tìm kiếm filter trên memory (FilteredList), không query DB       | Dataset nhỏ, không cần round-trip DB; UX tốt hơn (real-time)           |
| D5 | `categories` bổ sung cột `status` (chưa có trong schema gốc)    | Cần soft-delete nhất quán giữa 2 danh mục; update schema.sql            |
| D6 | Kiểm tra UNIQUE ở tầng Service (không chỉ dùng DB exception)    | Cho thông báo lỗi thân thiện thay vì `SQLiteException` thô              |
| D7 | Controller kiêm ViewModel (không tách)                           | Màn hình CRUD đơn giản, theo quy tắc "lite" trong architecture.md       |
| D8 | Tên đơn vị INACTIVE trùng → báo lỗi (không tái kích hoạt)      | Đơn giản hóa MVP; người dùng có thể tạo tên khác nếu cần               |
