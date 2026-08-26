# Spec: Danh mục (Nhà cung cấp)

> Module: phase-2-expansion/danh-muc · Phiên bản: 1.0
> Tham chiếu: [database-schema.md](../../database-schema.md)

---

## 1. Executive Summary

Tính năng quản lý **Nhà cung cấp** (Supplier) cho phép chủ cửa hàng ghi nhận thông tin đối tác cung cấp hàng hóa. Nhà cung cấp là danh mục cốt lõi phục vụ cho chức năng **Nhập hàng (Purchase)**. 
Người dùng quản lý tập trung Nhà cung cấp tại màn hình **Cài đặt** (sidebar → Thiết lập → tab Nhà cung cấp).

Thao tác hỗ trợ: Thêm / Sửa / Xóa mềm (`is_active = 0`). Mã nhà cung cấp (`code`) và số điện thoại (`phone`) cần phải duy nhất.

---

## 2. Requirements & Use Cases

### 2.1 Functional Requirements

| ID    | Yêu cầu                                                                          | Mức độ |
| ----- | -------------------------------------------------------------------------------- | ------ |
| FR-01 | Hiển thị danh sách nhà cung cấp đang hoạt động (`isActive=true`) với thanh tìm kiếm | Bắt buộc |
| FR-02 | Thêm nhà cung cấp mới. Nếu để trống mã (`code`), hệ thống tự động sinh (VD: NCC001) | Bắt buộc |
| FR-03 | Sửa thông tin nhà cung cấp (tên, sđt, địa chỉ, ghi chú)                            | Bắt buộc |
| FR-04 | Xóa mềm nhà cung cấp (chuyển `isActive` = 0)                                     | Bắt buộc |
| FR-05 | Validation: Tên, SĐT không được để trống.                                        | Bắt buộc |
| FR-06 | Validation: Mã NCC và Số điện thoại phải là duy nhất.                              | Bắt buộc |

### 2.2 Use Cases

**Thêm nhà cung cấp:**
1. Người dùng bấm [+ Thêm nhà cung cấp].
2. Mở dialog nhập liệu.
3. Người dùng nhập tên, số điện thoại (bắt buộc) và các thông tin khác → bấm [Lưu].
4. Hệ thống kiểm tra validation và insert vào DB. Cập nhật lại UI.

**Sửa thông tin:**
1. Người dùng bấm [✏️ Sửa] trên dòng tương ứng.
2. Form hiện ra với thông tin cũ. Người dùng sửa lại và bấm [Lưu].
3. Hệ thống validate và update vào DB. Cập nhật UI.

**Xóa:**
1. Bấm nút [🗑️ Xóa].
2. Hệ thống hiển thị popup xác nhận. Bấm "Xóa".
3. Update trạng thái `isActive = false`. Dòng bị ẩn khỏi danh sách.

---

## 3. UI/UX & Navigation

### 3.1 Luồng điều hướng
```
[Sidebar] → [⚙️ Thiết lập]
    │
    ├── [Tab: Đơn vị tính]
    ├── [Tab: Nhóm hàng]
    └── [Tab: Nhà cung cấp]
```

### 3.2 Wireframe Tab Nhà cung cấp
```
┌──────────────────────────────────────────────────────────┐
│ 🔍 [ Tìm theo mã, tên, SĐT...  ]      [+ Thêm nhà cung cấp]│
│                                                            │
│ ┌──────────────────────────────────────────────────────┐ │
│ │ Mã NCC   Tên NCC      Điện thoại    Địa chỉ  Thao tác │ │
│ │ NCC001   Công ty A    090xxxxxxx    Hà Nội    ✏️ 🗑️   │ │
│ │ NCC002   Đại lý B     091xxxxxxx    HCM       ✏️ 🗑️   │ │
│ └──────────────────────────────────────────────────────┘ │
│                                         Tổng: 2 NCC      │
└──────────────────────────────────────────────────────────┘
```

---

## 4. Data Models & State

### 4.1 Bảng Database liên quan (`suppliers`)
Bảng `suppliers` đã được tạo trong script migration `V2__MuaHang_Schema.sql`.

```sql
CREATE TABLE IF NOT EXISTS suppliers (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    code TEXT NOT NULL UNIQUE,          -- Mã NCC (VD: NCC001)
    name TEXT NOT NULL,
    phone TEXT NOT NULL UNIQUE,
    address TEXT,
    note TEXT,
    is_active INTEGER DEFAULT 1,        -- 1: Hoạt động, 0: Đã xóa
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

### 4.2 POJO Model
Nằm tại `com.shop.model.Supplier` với các thuộc tính ánh xạ trực tiếp từ DB.

---

## 5. Integration & Architecture

- **DAO Layer:** `SupplierDao.java` thực hiện SQL CRUD (câu lệnh SQL trực tiếp qua JDBC).
- **Service Layer:** `SupplierService.java` xử lý logic sinh mã, kiểm tra trùng lặp `code`, `phone`.
- **View/Controller:** `supplier-tab.fxml` & `SupplierTabController` chịu trách nhiệm render grid, search filter và CRUD UI. Tương tác với `supplier-form-dialog.fxml` (do `SupplierFormDialogController` điều khiển) để nhập liệu.
