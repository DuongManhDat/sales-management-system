# Spec: Mua hàng (Nhập hàng)

> Module: phase-2-expansion/mua-hang · Phiên bản: 1.0
> Tham chiếu: [database-schema.md](../../database-schema.md)

---

## 1. Executive Summary

Tính năng **Mua hàng** (Nhập hàng) cho phép chủ cửa hàng ghi nhận các giao dịch mua hàng hóa từ Nhà cung cấp. Chức năng này sẽ tạo phiếu nhập kho, tăng số lượng hàng hóa trong kho (qua các lô hàng - batch) và quản lý công nợ với Nhà cung cấp.
Người dùng thao tác tại màn hình **Mua hàng** (thường nằm ở Giao dịch -> Nhập hàng).

---

## 2. Requirements & Use Cases

### 2.1 Functional Requirements

| ID    | Yêu cầu                                                                          | Mức độ |
| ----- | -------------------------------------------------------------------------------- | ------ |
| FR-01 | Hiển thị danh sách phiếu nhập hàng với các bộ lọc (tìm kiếm mã/NCC, trạng thái)    | Bắt buộc |
| FR-02 | Tạo phiếu nhập hàng mới (chọn NCC, thêm sản phẩm, nhập số lượng, đơn giá)         | Bắt buộc |
| FR-03 | Khi hoàn thành phiếu nhập: tự động cập nhật số lượng tồn kho (tạo lô `inventory_batches`) | Bắt buộc |
| FR-04 | Quản lý số tiền đã trả và công nợ. Trạng thái phiếu: "Đã thanh toán", "Còn nợ" | Bắt buộc |
| FR-05 | Validation: Phải có ít nhất 1 sản phẩm. Số lượng > 0, Đơn giá >= 0. NCC phải được chọn (tùy chọn hoặc bắt buộc). | Bắt buộc |

### 2.2 Use Cases

**Tạo phiếu nhập hàng:**
1. Người dùng bấm [+ Tạo phiếu nhập].
2. Mở dialog form phiếu nhập.
3. Người dùng chọn Nhà cung cấp từ danh sách.
4. Người dùng tìm và thêm sản phẩm vào lưới chi tiết, sửa số lượng và đơn giá nhập.
5. Người dùng nhập số tiền "Đã trả NCC".
6. Hệ thống tự động tính tổng tiền và công nợ = Tổng - Đã trả.
7. Bấm [Lưu phiếu].
8. Hệ thống lưu `purchases`, `inventory_batches`, `purchase_items`, `supplier_payments` (nếu có trả tiền) và cập nhật số lượng `products`.

**Xem danh sách:**
1. Người dùng mở tab Nhập hàng.
2. Lưới hiển thị danh sách các phiếu nhập đã tạo (Sắp xếp mới nhất lên đầu).
3. Có thể tìm kiếm hoặc lọc theo trạng thái "Tất cả / Đã trả / Còn nợ".

---

## 3. UI/UX & Navigation

### 3.1 Luồng điều hướng
```
[Sidebar] → [🛒 Giao dịch] → [Tab: Nhập hàng]
```

### 3.2 Wireframe Tab Nhập Hàng
```
┌──────────────────────────────────────────────────────────┐
│ 🔍 [ Tìm mã phiếu, NCC... ] [ Trạng thái ▼]   [+ Tạo phiếu]│
│                                                            │
│ ┌──────────────────────────────────────────────────────┐ │
│ │ Mã phiếu   Ngày nhập    Nhà cung cấp   Tổng tiền     │ │
│ │ PO001      26/08/2026   Công ty A      1,500,000      │ │
│ │ PO002      25/08/2026   Đại lý B       500,000        │ │
│ └──────────────────────────────────────────────────────┘ │
└──────────────────────────────────────────────────────────┘
```

---

## 4. Data Models & State

### 4.1 Bảng Database liên quan
- `purchases`: Lưu thông tin chung của phiếu nhập.
- `purchase_items`: Lưu chi tiết các sản phẩm trong phiếu nhập.
- `inventory_batches`: Lưu lô hàng sinh ra từ việc nhập (phục vụ tính giá vốn FIFO/LIFO).
- `supplier_payments`: Lưu lịch sử thanh toán nợ cho phiếu nhập này.
- `products`: Cần cập nhật `stock_quantity`.

### 4.2 POJO Model
Nằm tại `com.shop.model.Purchase`, `PurchaseItem`, `InventoryBatch`, `SupplierPayment`.

---

## 5. Integration & Architecture

- **DAO Layer:** `PurchaseDao.java` (lưu transaction, tạo batch, insert items).
- **Service Layer:** `PurchaseService.java` (tính toán công nợ, quản lý transaction rollback/commit).
- **View/Controller:** `purchase-list.fxml` & `PurchaseListController`, `purchase-form.fxml` & `PurchaseFormController`.
