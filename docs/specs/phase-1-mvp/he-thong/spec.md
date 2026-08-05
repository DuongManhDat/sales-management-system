# Spec: Hệ thống

> Module: Hệ thống · Phase 1 — MVP · Cập nhật: 2026-07-14

---

## 1. Executive Summary

Module Hệ thống là tập hợp các chức năng **cross-cutting** không thuộc nghiệp vụ bán hàng nhưng là nền tảng vận hành của toàn ứng dụng. Giai đoạn 1 bao gồm 4 nhóm chức năng:

| Nhóm | Chức năng | Loại |
|------|-----------|------|
| **Thiết lập lần đầu** | Wizard nhập thông tin cửa hàng khi lần đầu chạy app | UI |
| **Cài đặt** | Xem/sửa thông tin cửa hàng, đổi mật khẩu | UI |
| **Log lỗi** | Ghi lỗi/cảnh báo ra file (Logback); xem trong app | Infrastructure + UI |
| **Log tồn kho** | Ghi mỗi biến động tồn vào `stock_movements`; xem + lọc | Infrastructure + UI |

Không có màn hình "Hệ thống" riêng trong sidebar — thay vào đó được gộp vào mục **⚙️ Cài đặt** của Main Window.

---

## 2. Requirements & Use Cases

### 2.1 Functional Requirements

| ID | Yêu cầu |
|----|---------|
| FR-SYS-01 | Lần đầu khởi chạy app (chưa có `shop_name`): hiển thị màn hình Setup bắt buộc trước khi vào app |
| FR-SYS-02 | Màn hình Setup thu thập: Tên cửa hàng (bắt buộc), Địa chỉ (bắt buộc), Số điện thoại (bắt buộc), Email (tuỳ chọn), Mã số thuế (tuỳ chọn), Logo (tuỳ chọn, PNG/JPG, Base64 vào `settings`) |
| FR-SYS-03 | Trong trang Cài đặt: cho phép xem và sửa lại toàn bộ thông tin cửa hàng |
| FR-SYS-04 | Tên cửa hàng hiển thị trong: title bar cửa sổ chính + sidebar header |
| FR-SYS-05 | Thông tin cửa hàng được nhúng vào header hóa đơn PDF A4 |
| FR-SYS-06 | Trong trang Cài đặt: có nút **Đổi mật khẩu** mở dialog popup (nhập MK cũ → MK mới → xác nhận MK mới) |
| FR-SYS-07 | Logback tự động ghi log lỗi/cảnh báo ra file `%APPDATA%/ShopManager/logs/app.log` (rolling theo ngày) |
| FR-SYS-08 | Trong trang Cài đặt: có tab/section **Nhật ký lỗi** — danh sách dòng log (timestamp, level, message); chỉ xem, không xóa |
| FR-SYS-09 | Mỗi nghiệp vụ thay đổi tồn kho (nhập hàng, bán hàng) phải ghi 1 dòng vào `stock_movements` trong cùng transaction |
| FR-SYS-10 | Trong trang Cài đặt: có tab/section **Lịch sử tồn kho** — bảng `stock_movements`, lọc được theo ngày và theo loại biến động (NHAP / BAN) |

### 2.2 Non-functional Requirements

| ID | Yêu cầu |
|----|---------|
| NFR-SYS-01 | Log file xoay vòng theo ngày (rolling), giữ tối đa 30 ngày, tránh phình file |
| NFR-SYS-02 | Đọc log file để hiển thị trong app phải chạy trên background thread (không block UI) |
| NFR-SYS-03 | Logo Base64 tối đa **200 KB** sau khi encode (gợi ý resize trước khi lưu) |
| NFR-SYS-04 | Ghi `stock_movements` nằm trong cùng transaction với thao tác tồn kho — đảm bảo nhất quán tuyệt đối |
| NFR-SYS-05 | Đổi mật khẩu phải verify mật khẩu cũ bằng BCrypt trước khi cập nhật |

### 2.3 Use Cases

**UC-SYS-01 — Thiết lập lần đầu (First-run Setup)**

- **Actor:** Chủ cửa hàng
- **Tiền điều kiện:** App vừa cài, `settings.shop_name` chưa có hoặc rỗng.
- **Luồng chính:**
  1. App khởi động → `SchemaInitializer` kiểm tra `settings.shop_name`.
  2. Nếu rỗng → điều hướng tới màn hình **Setup** (thay vì Login).
  3. Người dùng nhập Tên cửa hàng, Địa chỉ, Số điện thoại (bắt buộc); Email, MST, Logo (tuỳ chọn).
  4. Nhấn **Bắt đầu sử dụng** → lưu vào `settings` → điều hướng tới màn hình Login.
- **Luồng ngoại lệ:**
  - Thiếu trường bắt buộc → highlight đỏ, không cho lưu.

**UC-SYS-02 — Sửa thông tin cửa hàng**

- **Actor:** Chủ cửa hàng
- **Luồng chính:** Cài đặt → tab Thông tin cửa hàng → sửa trường → **Lưu**.
- **Hậu điều kiện:** Title bar và sidebar header cập nhật ngay lập tức.

**UC-SYS-03 — Đổi mật khẩu**

- **Luồng chính:** Cài đặt → nút **Đổi mật khẩu** → dialog: nhập MK cũ / MK mới / xác nhận → **Lưu**.
- **Luồng ngoại lệ:**
  - MK cũ sai → báo lỗi `"Mật khẩu hiện tại không đúng"`.
  - MK mới < 4 ký tự → báo lỗi.
  - MK mới ≠ xác nhận → báo lỗi.

**UC-SYS-04 — Xem nhật ký lỗi**

- **Luồng chính:** Cài đặt → tab **Nhật ký lỗi** → app đọc file `app.log` hiện tại → hiển thị danh sách dòng log theo thứ tự mới nhất trước.

**UC-SYS-05 — Xem lịch sử tồn kho**

- **Luồng chính:** Cài đặt → tab **Lịch sử tồn kho** → lọc ngày / loại biến động → danh sách `stock_movements`.

---

## 3. UI/UX & Navigation

### 3.1 Vị trí trong điều hướng

```
[Main Window] — Sidebar
  └── ⚙️ Cài đặt
        ├── Tab: Thông tin cửa hàng   (FR-SYS-02, FR-SYS-03)
        ├── Tab: Nhật ký lỗi          (FR-SYS-08)
        └── Tab: Lịch sử tồn kho     (FR-SYS-10)
              [Nút Đổi mật khẩu nằm trong tab Thông tin cửa hàng]
```

Màn hình **Setup** chỉ xuất hiện lần đầu, trước Login:

```
[App khởi động]
      │
      ├── shop_name rỗng ──► [Màn hình Setup] ──(Lưu)──► [Login]
      │
      └── shop_name đã có ──► [Login] ──(thành công)──► [Main Window]
```

### 3.2 Wireframe — Màn hình Setup (lần đầu)

```
┌─────────────────────────────────────────────────────────┐
│                                                          │
│          🏪  Chào mừng! Thiết lập cửa hàng của bạn      │
│                                                          │
│  Tên cửa hàng *   [ Cửa hàng ABC                    ]  │
│  Địa chỉ *        [ 123 Nguyễn Trãi, Q.1, TP.HCM    ]  │
│  Số điện thoại *  [ 0901 234 567                     ]  │
│  Email            [ shop@email.com          (tuỳ chọn)] │
│  Mã số thuế       [ 0123456789              (tuỳ chọn)] │
│                                                          │
│  Logo cửa hàng    [📁 Chọn ảnh...] (PNG/JPG, ≤ 200KB)  │
│                   [Xem trước: ▢ ]                        │
│                                                          │
│                      [ Bắt đầu sử dụng ]                │
│                                                          │
└─────────────────────────────────────────────────────────┘
```

### 3.3 Wireframe — Trang Cài đặt (Main Window)

```
┌─────────────────────────────────────────────────────────────────┐
│  ⚙️ Cài đặt                                                      │
├──────────────────┬──────────────────────────────────────────────┤
│ [Thông tin CH]   │  Tên cửa hàng *  [ Cửa hàng ABC          ]  │
│ [Nhật ký lỗi ]  │  Địa chỉ *       [ 123 Nguyễn Trãi...    ]  │
│ [Lịch sử tồn]   │  Số điện thoại * [ 0901 234 567           ]  │
│                  │  Email           [ shop@email.com         ]  │
│                  │  Mã số thuế      [ 0123456789             ]  │
│                  │  Logo            [📁 Thay đổi ảnh...      ]  │
│                  │                  [Xem trước: [LOGO] ]        │
│                  │                                              │
│                  │     [ Đổi mật khẩu ]         [ Lưu ]        │
└──────────────────┴──────────────────────────────────────────────┘
```

### 3.4 Wireframe — Dialog Đổi mật khẩu

```
┌────────────── Đổi mật khẩu ──────────────┐
│                                           │
│  Mật khẩu hiện tại  [ ••••••••••      ]  │
│  Mật khẩu mới       [ ••••••••••      ]  │
│  Xác nhận MK mới    [ ••••••••••      ]  │
│                                           │
│  ⚠️ [thông báo lỗi nếu có]               │
│                                           │
│              [ Hủy ]   [ Lưu ]           │
└───────────────────────────────────────────┘
```

### 3.5 Wireframe — Tab Nhật ký lỗi

```
┌─────────────────────────────────────────────────────────────────┐
│  📋 Nhật ký lỗi (chỉ xem)                    [ 🔄 Làm mới ]     │
├──────────────────────┬──────────┬─────────────────────────────┤
│ Thời gian            │ Mức      │ Thông điệp                   │
├──────────────────────┼──────────┼─────────────────────────────┤
│ 2026-07-14 08:23:11  │ ERROR    │ Dashboard load failed: ...   │
│ 2026-07-14 08:21:05  │ WARN     │ Bán âm kho SP003 (tồn: -2)  │
│ 2026-07-14 08:00:00  │ INFO     │ App khởi động thành công     │
└──────────────────────┴──────────┴─────────────────────────────┘
  (Hiển thị mới nhất trước. Tối đa 500 dòng gần nhất.)
```

### 3.6 Wireframe — Tab Lịch sử tồn kho

```
┌─────────────────────────────────────────────────────────────────┐
│  📦 Lịch sử biến động tồn kho                                    │
│  Từ: [13/07/2026] Đến: [14/07/2026]  Loại: [Tất cả ▼]  [Lọc]  │
├───────────────┬──────────┬────────┬───────────┬─────────────┤
│ Thời gian     │ Sản phẩm │ Loại   │ Thay đổi  │ Tồn sau     │
├───────────────┼──────────┼────────┼───────────┼─────────────┤
│ 14/07 08:30   │ Coca 330ml│ BAN   │ -2 lon    │ 238 lon     │
│ 13/07 15:12   │ Gạo ST25  │ NHAP  │ +50 kg    │ 53 kg       │
│ 13/07 09:05   │ Mì Hảo Hảo│ BAN   │ -3 gói    │ 0 gói ⚠️   │
└───────────────┴──────────┴────────┴───────────┴─────────────┘
  Chú thích loại: NHAP = nhập hàng · BAN = bán hàng
  ⚠️ = tồn sau ≤ 0 (âm kho)
```

### 3.7 Chi tiết UX

| Element | Hành vi |
|---------|---------|
| Trường bắt buộc | Đánh dấu `*`; border đỏ + tooltip lỗi khi submit mà để trống |
| Nút Lưu (Cài đặt) | Disabled khi không có thay đổi; Enabled khi dữ liệu đã sửa |
| Logo preview | Hiển thị ảnh ngay sau khi chọn file; nếu > 200KB báo cảnh báo |
| Đổi MK — ô mật khẩu | Có toggle 👁️ hiện/ẩn ký tự |
| Tab Nhật ký lỗi | Mức ERROR bôi đỏ, WARN bôi cam, INFO màu mặc định |
| Tab Lịch sử tồn kho | Dòng có `stock_after ≤ 0` bôi vàng + icon ⚠️ |
| Dropdown "Loại" | Tất cả / NHAP / BAN |

---

## 4. Data Models & State

### 4.1 Bảng `settings` — các key sử dụng

| Key | Bắt buộc | Mô tả | Ví dụ value |
|-----|----------|-------|-------------|
| `shop_name` | ✅ | Tên cửa hàng | `Cửa hàng ABC` |
| `shop_address` | ✅ | Địa chỉ | `123 Nguyễn Trãi, Q.1` |
| `shop_phone` | ✅ | Số điện thoại | `0901234567` |
| `shop_email` | — | Email | `shop@email.com` |
| `shop_tax_code` | — | Mã số thuế | `0123456789` |
| `shop_logo_base64` | — | Logo PNG/JPG encode Base64 | `data:image/png;base64,...` |
| `schema_version` | ✅ | Phiên bản schema DB (dùng migration) | `2` |

> 💡 Bảng `settings` đã có trong `database-schema.md`. Chỉ cần bổ sung seed data cho các key mới.

**Seed bổ sung:**
```sql
INSERT OR IGNORE INTO settings(key, value) VALUES
  ('shop_email', ''),
  ('shop_tax_code', ''),
  ('shop_logo_base64', '');
```

### 4.2 Bảng `stock_movements` (đã có trong schema)

```sql
stock_movements (
  id          INTEGER PRIMARY KEY,
  product_id  INTEGER NOT NULL REFERENCES products(id) ON DELETE RESTRICT,
  type        TEXT    NOT NULL,   -- NHAP | BAN | TRA | KIEMKHO
  qty_change  REAL    NOT NULL,   -- dương = nhập, âm = bán
  stock_after REAL    NOT NULL,   -- tồn SAU thay đổi
  ref_type    TEXT,               -- INVOICE | PURCHASE | ADJUST
  ref_id      INTEGER,            -- id chứng từ liên quan
  note        TEXT,
  created_at  TEXT    NOT NULL    -- ISO-8601 local time
)
```

Bảng này **đã có** trong `database-schema.md §3`. Không cần migration.

### 4.3 Cấu trúc file log (Logback)

```
%APPDATA%/ShopManager/
  logs/
    app.log              ← file hiện tại
    app.2026-07-13.log   ← file của ngày trước (rolling)
    app.2026-07-12.log
    ...                  (giữ 30 ngày, xóa cũ hơn tự động)
```

**Format một dòng log:**
```
2026-07-14 08:23:11.045 [JavaFX App Thread] ERROR c.s.service.DashboardService - Dashboard load failed
java.sql.SQLException: ...
```

### 4.4 State trong `SettingsViewModel`

```java
// Thông tin cửa hàng
StringProperty shopName
StringProperty shopAddress
StringProperty shopPhone
StringProperty shopEmail
StringProperty shopTaxCode
StringProperty shopLogoBase64

// Trạng thái form
BooleanProperty dirty      // true khi có thay đổi chưa lưu
BooleanProperty saving

// Nhật ký lỗi
ObservableList<LogEntry> logEntries    // record(timestamp, level, message)

// Lịch sử tồn kho
ObservableList<StockMovementRow> stockMovements
ObjectProperty<LocalDate> filterFrom
ObjectProperty<LocalDate> filterTo
StringProperty            filterType  // "" | "NHAP" | "BAN"
BooleanProperty           loadingStock
```

**Record classes:**
```java
record LogEntry(String timestamp, String level, String message) {}
record StockMovementRow(
    String createdAt, String productName, String unit,
    String type, double qtyChange, double stockAfter
) {}
```

---

## 5. Integration & Architecture

### 5.1 Luồng dữ liệu

```
[SettingsView.fxml]
       │ bind
       ▼
[SettingsController]
  ├── bind TextFields  ↔ viewModel.shopName / shopAddress / ...
  ├── bind TableView   ↔ viewModel.logEntries
  ├── bind TableView   ↔ viewModel.stockMovements
  ├── onSaveShopInfo() → viewModel.saveShopInfo()
  ├── onChangePassword() → dialog → viewModel.changePassword(old, new)
  └── onFilterStock()  → viewModel.loadStockMovements(from, to, type)
       │
       ▼
[SettingsViewModel]
  ├── saveShopInfo()       → SettingsService.saveShopInfo(dto)
  ├── changePassword()     → AuthService.changePassword(old, new)
  ├── loadLogEntries()     → Task: LogReader.readRecentLines(500)
  └── loadStockMovements() → Task: StockMovementService.query(from, to, type)
       │
       ▼
[SettingsService]  [AuthService]  [StockMovementService]  [LogReader]
  └── SettingsDao    └── AppUserDao   └── StockMovementDao   └── đọc file app.log
       │
       ▼
[DBConnection / SQLite]              [%APPDATA%/ShopManager/logs/]
```

### 5.2 First-run Detection

```java
// Trong App.java sau khi SchemaInitializer chạy xong:
String shopName = settingsService.get("shop_name");
if (shopName == null || shopName.isBlank()) {
    navigateTo("fxml/setup.fxml");   // Màn hình Setup lần đầu
} else {
    navigateTo("fxml/login.fxml");   // Màn hình Login thông thường
}
```

### 5.3 Ghi `stock_movements` — quy tắc bắt buộc

Mọi Service thay đổi tồn kho **phải** gọi `StockMovementDao.insert()` trong **cùng transaction**:

```java
// Ví dụ trong SalesService.createInvoice() (đã có trong architecture.md ADR-002):
// BEGIN TRANSACTION
//   InvoiceDao.insert(invoice)
//   InvoiceItemDao.insertAll(items)
//   foreach item:
//     double stockAfter = ProductDao.decreaseStock(productId, qty)
//     StockMovementDao.insert(type=BAN, -qty, stockAfter, INVOICE, invoiceId)
// COMMIT

// Tương tự PurchaseService.createPurchase():
//   double stockAfter = ProductDao.increaseStock(productId, qty)
//   StockMovementDao.insert(type=NHAP, +qty, stockAfter, PURCHASE, purchaseId)
```

### 5.4 Cập nhật title bar & sidebar sau khi lưu

```java
// SettingsController — sau khi SettingsService.saveShopInfo() thành công:
Platform.runLater(() -> {
    mainController.updateShopName(viewModel.shopName.get());
    // MainController: stage.setTitle(shopName + " — Quản lý Bán hàng")
    //                 sidebarHeader.setText(shopName)
});
```

### 5.5 Classes liên quan

| Class | Package | Vai trò |
|-------|---------|---------|
| `SetupView.fxml` | `resources/fxml/` | Màn hình setup lần đầu |
| `SetupController` | `com.shop.view` | Xử lý form setup lần đầu |
| `SettingsView.fxml` | `resources/fxml/` | Trang Cài đặt (3 tab) |
| `SettingsController` | `com.shop.view` | Bind UI ↔ ViewModel |
| `SettingsViewModel` | `com.shop.viewmodel` | State + commands |
| `SettingsService` | `com.shop.service` | Đọc/ghi bảng `settings` |
| `SettingsDao` | `com.shop.dao` | SQL CRUD trên `settings` |
| `StockMovementDao` | `com.shop.dao` | Insert + query `stock_movements` |
| `StockMovementService` | `com.shop.service` | Query `stock_movements` cho UI |
| `LogReader` | `com.shop.util` | Đọc N dòng cuối file `app.log` |
| `AppPaths` | `com.shop.config` | Cung cấp path `%APPDATA%/ShopManager/` |

> 💡 `SettingsService` phần lớn là read-only. Chỉ `saveShopInfo()` và `changePassword()` cần ghi DB (transaction đơn giản 1 bảng).

---

## 6. Edge Cases & Risks

| Tình huống | Xử lý |
|-----------|-------|
| Logo ảnh > 200 KB | Hiển thị cảnh báo ngay khi chọn file; nếu người dùng vẫn xác nhận → vẫn lưu |
| File `app.log` không tồn tại (chưa có lỗi nào) | Hiển thị "Chưa có nhật ký lỗi nào" — không báo lỗi |
| File `app.log` đang bị Logback ghi (file lock) | `LogReader` dùng `RandomAccessFile` / `FileChannel` để đọc, không conflict với Logback writer |
| `stock_movements` rỗng (chưa có giao dịch nào) | Hiển thị "Chưa có biến động tồn kho nào" |
| Lọc `stock_movements` trả về nhiều bản ghi | Giới hạn 1.000 dòng gần nhất + thông báo "Hiển thị 1.000/X bản ghi gần nhất" |
| Đổi MK sai mật khẩu cũ | Báo lỗi ngay; MVP không khóa tài khoản (1 người dùng duy nhất) |
| `shop_name` để trống khi lưu Cài đặt | Validation bắt buộc: không cho lưu nếu `shop_name` rỗng |
| App tắt đột ngột khi đang ghi `stock_movements` | SQLite transaction tự rollback khi DB mở lại — không mất nhất quán |
| MK mới < 4 ký tự | Báo lỗi validation: "Mật khẩu tối thiểu 4 ký tự" |
