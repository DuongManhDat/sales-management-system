# Spec: Dashboard

> Module: Dashboard · Phase 1 — MVP · Cập nhật: 2026-07-11

---

## 1. Executive Summary

Dashboard là màn hình tổng quan doanh thu, hiển thị ngay sau khi đăng nhập. Mục tiêu là cung cấp cho chủ cửa hàng một cái nhìn nhanh về tình hình kinh doanh trong ngày (mặc định) hoặc một ngày bất kỳ do người dùng chọn — bao gồm doanh thu, lợi nhuận gộp, số đơn hàng, biểu đồ phân bố theo giờ và theo thứ, top sản phẩm bán chạy, và cảnh báo hàng sắp hết.

Theo kiến trúc MVVM-lite ([ADR-004](../../../architecture.md)), Dashboard là **màn hình phức tạp** → tách `DashboardViewModel` riêng để quản lý state và testable độc lập.

---

## 2. Requirements & Use Cases

### 2.1 Functional Requirements

| ID | Yêu cầu |
|----|---------|
| FR-D01 | Hiển thị dữ liệu mặc định cho ngày hôm nay khi mở màn hình |
| FR-D02 | Cho phép người dùng chọn ngày bất kỳ qua DatePicker |
| FR-D03 | Hiển thị 4 KPI cards: Doanh thu, Lợi nhuận gộp, Số đơn, SP sắp hết |
| FR-D04 | Hiển thị Bar Chart doanh thu theo giờ (0–23h) |
| FR-D05 | Hiển thị Bar Chart doanh thu theo thứ trong tuần (T2–CN), dựa trên 7 ngày gần nhất tính từ ngày được chọn |
| FR-D06 | Hiển thị danh sách Top 5 sản phẩm bán chạy nhất trong ngày được chọn |
| FR-D07 | Hiển thị danh sách sản phẩm sắp hết hàng với ngưỡng do người dùng chọn (≤ 5 / ≤ 10 / ≤ 20) |
| FR-D08 | Có nút **Làm mới** (Refresh) để tải lại dữ liệu thủ công |

### 2.2 Non-functional Requirements

| ID | Yêu cầu |
|----|---------|
| NFR-D01 | Toàn bộ truy vấn DB chạy trên **background thread** (Task/Service của JavaFX), không block UI thread |
| NFR-D02 | Thời gian load dữ liệu ≤ 2 giây với ~10.000 hóa đơn (nhờ index `idx_invoices_date`) |
| NFR-D03 | Hiển thị trạng thái "Đang tải..." (loading indicator) trong khi chờ dữ liệu |
| NFR-D04 | Khi không có dữ liệu trong ngày, hiển thị trạng thái trống thay vì lỗi |

### 2.3 Use Case chính

**UC-06 — Xem Dashboard doanh thu**

- **Actor:** Chủ cửa hàng
- **Tiền điều kiện:** Đã đăng nhập thành công.
- **Luồng chính:**
  1. Người dùng mở Dashboard (hoặc vừa đăng nhập xong → app tự điều hướng tới Dashboard).
  2. Hệ thống tự load dữ liệu của **ngày hôm nay** (`date('now','localtime')`).
  3. Hiển thị 4 KPI cards, 2 biểu đồ, Top 5 SP, danh sách hàng sắp hết.
  4. Người dùng có thể chọn **ngày khác** bằng DatePicker → hệ thống reload dữ liệu cho ngày đó.
  5. Người dùng nhấn **Làm mới** → hệ thống tải lại dữ liệu của ngày đang chọn.

- **Luồng ngoại lệ:**
  - DB không đọc được → hiển thị thông báo lỗi, ghi log; các KPI card hiện `—`.
  - Ngày được chọn chưa có hóa đơn nào → KPI card hiển thị `0`, biểu đồ trống, Top 5 rỗng.

---

## 3. UI/UX & Navigation

### 3.1 Vị trí trong điều hướng

Dashboard là mục đầu tiên trong sidebar của `Main Window`. Sau khi đăng nhập, app điều hướng tự động tới Dashboard.

```
[Login] ─(thành công)─► [Main Window]
                              │
                              ▼
                         [Dashboard]  ← mặc định khi vào app
```

### 3.2 Wireframe (text)

```
┌─────────────────────────────────────────────────────────────────────┐
│  Dashboard                            📅 [27/06/2026 ▼]  [🔄 Làm mới] │
├─────────────────────────────────────────────────────────────────────┤
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐│
│  │ 💰 Doanh thu │  │ 📈 Lợi nhuận │  │ 🧾 Số đơn   │  │ ⚠️ Sắp hết  ││
│  │   12.500.000 │  │    3.200.000 │  │      34      │  │   8 sản phẩm││
│  │          đ   │  │          đ   │  │              │  │  [≤ 10 ▼]   ││
│  └──────────────┘  └──────────────┘  └──────────────┘  └──────────────┘│
├─────────────────────────────────────────────────────────────────────┤
│  Doanh thu theo GIỜ trong ngày          Doanh thu theo THỨ (7 ngày)  │
│  ┌────────────────────────────────┐  ┌────────────────────────────┐  │
│  │  (Bar Chart: 0h – 23h)        │  │ (Bar Chart: T2 – CN)       │  │
│  │      ▂▄▃▂  ▇█▇▅▃▂             │  │  ▅  ▆  ▃  ▇  █  ▇  ▂      │  │
│  └────────────────────────────────┘  └────────────────────────────┘  │
├─────────────────────────────────────────────────────────────────────┤
│  🏆 Top 5 sản phẩm bán chạy hôm nay                                   │
│  1. Coca Cola 330ml ················· 120 lon  · 1.200.000 đ          │
│  2. Mì Hảo Hảo ····················· 95 gói   ·   380.000 đ          │
│  3. Nước suối Lavie 500ml ··········· 60 chai  ·   300.000 đ          │
│  4. Gạo ST25 ························ 40 kg    · 1.400.000 đ          │
│  5. Bánh mì sandwich ················ 35 cái   ·   175.000 đ          │
├─────────────────────────────────────────────────────────────────────┤
│  ⚠️ Hàng sắp hết tồn kho (tồn ≤ 10)     Ngưỡng: [≤ 5 | ≤ 10 | ≤ 20] │
│  Mã     │ Tên              │ ĐVT │ Tồn hiện tại                       │
│  SP003  │ Mì gói Hảo Hảo   │ gói │ 0  ⚠️                             │
│  SP007  │ Gạo ST25         │ kg  │ 3                                  │
│  SP012  │ Sữa tươi Vinamilk│ hộp │ 7                                  │
└─────────────────────────────────────────────────────────────────────┘
```

### 3.3 Chi tiết UX

| Element | Hành vi |
|---------|---------|
| DatePicker | Mặc định = hôm nay; giới hạn max = hôm nay (không cho chọn tương lai) |
| Nút Làm mới | Disabled khi đang loading; hiện spinner trong khi tải |
| Loading state | Hiển thị `ProgressIndicator` overlay lên vùng KPI cards + biểu đồ |
| KPI card "Sắp hết" | Combo box chọn ngưỡng (≤ 5 / ≤ 10 / ≤ 20); thay đổi ngưỡng → reload ngay danh sách sắp hết (không cần nhấn Refresh) |
| Biểu đồ giờ | Trục X: 0–23, trục Y: doanh thu (đ); thanh tooltip hiện giá trị khi hover |
| Biểu đồ thứ | Trục X: T2–CN (7 ngày tính từ ngày chọn về trước), trục Y: doanh thu (đ) |
| Top 5 SP | Bảng tĩnh, không click; hiện cả tên, đơn vị, số lượng, doanh thu |
| Giá trị tiền | Định dạng vi-VN: `12.500.000 đ` (dùng `util/Money`) |

---

## 4. Data Models & State

### 4.1 Schema change — Thêm `cost_price` vào `invoice_items`

> ⚠️ **Thay đổi schema cần thiết để tính Lợi nhuận gộp chính xác.**

Lợi nhuận gộp = Σ(sale_price - cost_price) × qty theo từng dòng hóa đơn. Để tính đúng tại thời điểm bán (giá vốn có thể thay đổi sau), cần lưu `cost_price` tại thời điểm bán vào `invoice_items`.

```sql
-- Thêm cột cost_price vào invoice_items
ALTER TABLE invoice_items ADD COLUMN cost_price INTEGER NOT NULL DEFAULT 0;
-- Ghi chú: DEFAULT 0 cho dữ liệu cũ; dữ liệu mới phải truyền cost_price thực từ products
```

**Tác động:** `SalesService.createInvoice()` phải lấy `cost_price` từ `products` và lưu vào `invoice_items` khi tạo hóa đơn.

### 4.2 Nguồn dữ liệu cho từng KPI

| KPI | Nguồn | SQL gợi ý |
|-----|-------|-----------|
| Tổng doanh thu ngày | `invoices.total` | `SELECT SUM(total) FROM invoices WHERE date(invoice_date) = :date AND status='COMPLETED'` |
| Lợi nhuận gộp | `invoice_items.(sale_price - cost_price) * qty` | `SELECT SUM(ii.amount - ii.cost_price * ii.qty) FROM invoice_items ii JOIN invoices i ON i.id = ii.invoice_id WHERE date(i.invoice_date) = :date AND i.status='COMPLETED'` |
| Số đơn hàng | `COUNT(invoices)` | `SELECT COUNT(*) FROM invoices WHERE date(invoice_date) = :date AND status='COMPLETED'` |
| Doanh thu theo giờ | `strftime('%H', invoice_date)` | xem [database-schema.md §6](../../../database-schema.md) |
| Doanh thu theo thứ | `strftime('%w', invoice_date)` | xem [database-schema.md §6](../../../database-schema.md) — 7 ngày trước ngày chọn |
| Top 5 SP | `invoice_items JOIN products GROUP BY product_id ORDER BY SUM(qty) DESC LIMIT 5` | — |
| Hàng sắp hết | `products WHERE stock_qty <= :threshold AND status='ACTIVE'` | — |

### 4.3 State trong `DashboardViewModel`

```java
// Ngày được chọn (bind tới DatePicker)
ObjectProperty<LocalDate> selectedDate

// KPI
LongProperty totalRevenue          // đồng
LongProperty grossProfit           // đồng
IntegerProperty totalOrders
IntegerProperty lowStockCount

// Biểu đồ
ObservableList<XYChart.Data<String,Number>> revenueByHour    // Bar Chart
ObservableList<XYChart.Data<String,Number>> revenueByDayOfWeek

// Top 5 SP
ObservableList<TopProductRow> topProducts

// Hàng sắp hết
ObservableList<LowStockRow> lowStockProducts
IntegerProperty lowStockThreshold  // 5 | 10 | 20

// Trạng thái loading
BooleanProperty loading
StringProperty errorMessage
```

**Record classes:**
```java
record TopProductRow(String productName, String unit, double qty, long revenue) {}
record LowStockRow(String code, String productName, String unit, double stockQty) {}
```

---

## 5. Integration & Architecture

### 5.1 Luồng dữ liệu

```
[DashboardView.fxml]
       │ bind
       ▼
[DashboardController]
  ├── bind DatePicker ↔ viewModel.selectedDate
  ├── bind KPI Labels ↔ viewModel.totalRevenue / grossProfit / ...
  ├── bind Charts ↔ viewModel.revenueByHour / revenueByDayOfWeek
  ├── bind TableViews ↔ viewModel.topProducts / lowStockProducts
  └── onRefresh() → viewModel.loadData()
       │
       ▼
[DashboardViewModel]
  └── loadData() → submit Task<DashboardData> (background thread)
                          │
                          ▼
                  [DashboardService]
                    ├── getRevenueSummary(date)
                    ├── getRevenueByHour(date)
                    ├── getRevenueByDayOfWeek(date)
                    ├── getTopProducts(date, limit=5)
                    └── getLowStockProducts(threshold)
                          │
                          ▼
                  [DashboardDao]
                    └── SQL queries trên invoices, invoice_items, products
                          │
                          ▼
                  [DBConnection / SQLite]
```

### 5.2 Threading

```java
// Trong DashboardViewModel.loadData():
loading.set(true);
errorMessage.set(null);
Task<DashboardData> task = new Task<>() {
    @Override
    protected DashboardData call() throws Exception {
        return dashboardService.loadAll(selectedDate.get(), lowStockThreshold.get());
    }
};
task.setOnSucceeded(e -> {
    Platform.runLater(() -> {
        applyData(task.getValue());
        loading.set(false);
    });
});
task.setOnFailed(e -> {
    Platform.runLater(() -> {
        errorMessage.set("Không thể tải dữ liệu Dashboard.");
        loading.set(false);
        log.error("Dashboard load failed", task.getException());
    });
});
executor.submit(task);
```

### 5.3 Classes liên quan

| Class | Package | Vai trò |
|-------|---------|---------|
| `DashboardView.fxml` | `resources/fxml/` | Layout FXML |
| `DashboardController` | `com.shop.view` | Bind UI ↔ ViewModel, điều hướng |
| `DashboardViewModel` | `com.shop.viewmodel` | State, command `loadData()` |
| `DashboardService` | `com.shop.service` | Gom nhiều DAO query; read-only, không cần transaction |
| `DashboardDao` | `com.shop.dao` | SQL queries thống kê |
| `DashboardData` | `com.shop.model` | DTO gom toàn bộ kết quả cho 1 lần load |
| `TopProductRow` | `com.shop.model` | Record hiển thị top SP |
| `LowStockRow` | `com.shop.model` | Record hiển thị hàng sắp hết |

> 💡 `DashboardService.loadAll()` không cần transaction vì toàn bộ là read-only query. Tuy nhiên vẫn tái sử dụng cùng `Connection` để đảm bảo snapshot nhất quán.

---

## 6. Edge Cases & Risks

| Tình huống | Xử lý |
|-----------|-------|
| Không có hóa đơn nào trong ngày được chọn | KPI = 0, biểu đồ trống (empty state label), Top 5 rỗng — **không báo lỗi** |
| Ngày trong tương lai | DatePicker giới hạn max = hôm nay; không thể chọn |
| `invoice_items.cost_price = 0` (dữ liệu cũ trước khi migration) | Lợi nhuận gộp của đơn đó tính = doanh thu (cost = 0); chấp nhận ở MVP |
| DB đọc bị lỗi (file bị khóa, corrupted) | Hiện `errorMessage`, ghi log ERROR; các KPI card hiện `—` |
| Load chậm (nhiều dữ liệu) | `loading = true` → disable nút Refresh, hiện spinner; timeout sau 30 giây → báo lỗi |
| Người dùng nhấn Refresh liên tục | Nếu đang loading → nút Refresh bị disabled; tránh race condition |
| `stock_qty <= 0` (tồn âm kho) | Vẫn xuất hiện trong danh sách sắp hết nếu `0 <= threshold`; hiển thị `⚠️` |
| Tất cả sản phẩm đang tồn kho tốt | Danh sách sắp hết rỗng → hiện "Không có sản phẩm nào sắp hết hàng" |
