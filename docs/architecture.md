# Kiến trúc — Hệ thống Quản lý Bán hàng (JavaFX Desktop)

> Tài liệu kiến trúc · Phiên bản 1.0 · 2026-06-27
> Bổ sung cho [MVP-plan.md](./MVP-plan.md). Khung tham chiếu: *Requirements drive architecture · Trade-offs inform decisions · ADRs capture rationale · Start simple*.

---

## 1. Phân loại hệ thống (Context)


| Tiêu chí                | Giá trị                                                   | Hệ quả kiến trúc                                |
| ----------------------- | --------------------------------------------------------- | ----------------------------------------------- |
| Loại ứng dụng           | Desktop 1 người dùng, offline                             | Không cần tầng mạng, không API, không đồng bộ   |
| Quy mô dữ liệu          | Vài nghìn SP, vài chục nghìn đơn/năm                      | DB nhúng (SQLite) là đủ; không cần pool lớn     |
| Đồng thời (concurrency) | 1 người dùng, nhưng có **UI thread vs background thread** | Cần tách I/O khỏi JavaFX Application Thread     |
| Triển khai              | Đóng gói `.exe` chạy local                                | DB & log ghi ở `%APPDATA%`, không ở thư mục cài |
| Đội ngũ                 | Nhỏ (1 dev)                                               | Ưu tiên đơn giản, ít "ma thuật", dễ bảo trì     |


**Kết luận:** Đây là bài toán **CRUD nghiệp vụ + một luồng giao dịch lõi (bán hàng)**. Độ phức tạp nằm ở *tách lớp sạch* và *binding UI*, không phải ở phân tán/quy mô. → Kiến trúc nên **đơn giản, phân lớp rõ**.

---



## 2. ✅ Xác nhận quyết định: MVVM-lite

**MVVM-lite được XÁC NHẬN là phù hợp** cho app này. Lý do cốt lõi: JavaFX có **property binding** (`Property`, `ObservableList`) — đây chính là cơ chế MVVM được thiết kế để tận dụng. Dùng MVVM "thuận theo công cụ" thay vì chống lại nó.

### "lite" nghĩa là gì? (quy tắc rõ ràng để khỏi over-engineer)

- **Màn hình đơn giản (CRUD: Đơn vị, Khách hàng, Hàng hóa):** cho phép **gộp ViewModel vào Controller** — Controller giữ `ObservableList` và gọi thẳng Service. Không bắt buộc tách lớp ViewModel.
- **Màn hình phức tạp (POS Bán hàng, Dashboard):** **TÁCH ViewModel riêng** — vì có nhiều state (giỏ hàng, tạm tính, giảm giá, tiền thối) và logic tính toán cần test độc lập.

> Nguyên tắc: *Bắt đầu đơn giản, chỉ tách ViewModel khi màn hình đủ phức tạp để được lợi.*

---



## 3. Các tầng (Layers)

```
┌──────────────────────────────────────────────────────────────┐
│  VIEW           FXML + CSS  (khai báo giao diện, không logic)   │
├──────────────────────────────────────────────────────────────┤
│  VIEW-CONTROLLER  @FXML Controller                              │
│   • bind control ↔ ViewModel property                          │
│   • nhận sự kiện UI, gọi command của ViewModel                 │
│   • KHÔNG chứa logic nghiệp vụ, KHÔNG gọi DAO trực tiếp        │
├──────────────────────────────────────────────────────────────┤
│  VIEWMODEL       (tách riêng cho màn hình phức tạp)            │
│   • state UI: Property / ObservableList                         │
│   • command: createInvoice(), addToCart()...                  │
│   • gọi Service; KHÔNG tham chiếu Node JavaFX → test được      │
├──────────────────────────────────────────────────────────────┤
│  SERVICE         logic nghiệp vụ + RANH GIỚI GIAO DỊCH (tx)    │
│   • quy tắc: cảnh báo bán âm kho, sinh stock_movements        │
│   • orchestrate nhiều DAO trong 1 transaction                 │
│   • bắt exception → log.error(); ném DomainException           │
├──────────────────────────────────────────────────────────────┤
│  DAO / REPOSITORY   JDBC thuần                                  │
│   • SQL + PreparedStatement; map ResultSet ↔ model            │
│   • KHÔNG chứa logic nghiệp vụ, KHÔNG mở/đóng transaction      │
├──────────────────────────────────────────────────────────────┤
│  MODEL (domain)  POJO: Product, Invoice, Customer...           │
├──────────────────────────────────────────────────────────────┤
│  INFRA  DBConnection · SchemaInitializer · LogContext · AppPaths│
└──────────────────────────────────────────────────────────────┘
```

**Luật phụ thuộc (bắt buộc):** chỉ đi xuống — `View → Controller → ViewModel → Service → DAO → Infra`. Tầng dưới **không** biết tầng trên. Model là POJO thuần, mọi tầng dùng chung.

### Trách nhiệm từng tầng (tóm tắt)


| Tầng        | Được làm                                | Không được làm                           |
| ----------- | --------------------------------------- | ---------------------------------------- |
| View (FXML) | bố cục, style                           | logic                                    |
| Controller  | bind, điều hướng, hiển thị alert        | SQL, tính toán nghiệp vụ                 |
| ViewModel   | state UI, gọi Service                   | tham chiếu `Node`/`Stage` (để test được) |
| Service     | quy tắc nghiệp vụ, **transaction**, log | thao tác UI                              |
| DAO         | SQL CRUD                                | quyết định nghiệp vụ, quản lý tx         |


---



## 4. Luồng dữ liệu — Use case lõi UC-04 (Bán hàng)

```
[POS View] --click "Thanh toán"--> [PosController.onPay()]
      │
      ▼
[PosViewModel.checkout()]            (đọc giỏ hàng từ ObservableList)
      │  gọi
      ▼
[SalesService.createInvoice(cart, customer, discount)]
      │   ┌─ BEGIN TRANSACTION ──────────────────────────────┐
      │   │ 1. InvoiceDao.insert(invoice)                     │
      │   │ 2. InvoiceItemDao.insertAll(items)                │
      │   │ 3. ProductDao.decreaseStock(...)                  │
      │   │    → nếu tồn < 0: log WARN "bán âm kho" (vẫn tiếp) │
      │   │ 4. StockMovementDao.insert(type=BAN, stock_after) │
      │   └─ COMMIT (lỗi bất kỳ → ROLLBACK) ─────────────────┘
      │  trả Invoice
      ▼
[PosViewModel] cập nhật state → [Controller] hiển thị + gọi PdfExporter
```

**Điểm mấu chốt:** toàn bộ bước 1–4 nằm trong **một transaction ở Service**. Nếu trừ kho lỗi giữa chừng → rollback, không để hóa đơn "nửa vời". DAO không tự commit.

### Threading (rất quan trọng cho JavaFX)

```
JavaFX App Thread ──(submit)──> Task<Invoice> { SalesService.createInvoice() }
        ▲                              │ chạy trên background thread (DB I/O)
        └──── Platform.runLater() ◄────┘ (cập nhật UI khi xong)
```

→ DB/PDF luôn chạy trong `Task`/`Service`; **không bao giờ** gọi DB trực tiếp trên App Thread (gây "đơ").

---



## 5. Cross-cutting concerns


| Concern                | Vị trí xử lý                                               | Ghi chú                                                  |
| ---------------------- | ---------------------------------------------------------- | -------------------------------------------------------- |
| **Transaction**        | Service (1 tx / 1 nghiệp vụ)                               | `DBConnection` hỗ trợ begin/commit/rollback              |
| **Logging**            | Service (nghiệp vụ) + global handler (lỗi)                 | `LogContext` gắn `operationId` qua MDC; JSON ra file     |
| **Log tồn kho**        | Service ghi `stock_movements` trong cùng tx bán/nhập       | đảm bảo nhất quán với thay đổi tồn                       |
| **Validation**         | Controller (định dạng input) + Service (quy tắc nghiệp vụ) | 2 lớp: UI chặn sai định dạng, Service chặn sai nghiệp vụ |
| **Tiền tệ**            | `util/Money` — lưu **long (đồng)**, format vi-VN           | tránh `double`                                           |
| **Lỗi không bắt được** | `Thread.setDefaultUncaughtExceptionHandler`                | log FATAL (marker) + dialog xin lỗi                      |


---



## 6. Architecture Decision Records (ADR)



### ADR-001 — Chọn MVVM-lite thay vì MVC/MVP

- **Bối cảnh:** UI JavaFX có property binding; app nhiều màn hình CRUD + 1 màn hình POS phức tạp.
- **Quyết định:** Dùng **MVVM-lite** — tách ViewModel cho màn hình phức tạp, gộp vào Controller cho màn hình đơn giản.
- **Phương án khác:** MVC thuần (Controller dễ phình to, khó test logic UI); MVP (nhiều boilerplate interface, không tận dụng binding).
- **Trade-off:** Được — testable, tận dụng binding, tách bạch; Mất — thêm 1 lớp khái niệm. Giảm thiểu bằng quy tắc "lite".
- **Trạng thái:** ✅ Đã chấp nhận.



### ADR-002 — Service làm ranh giới giao dịch (transaction boundary)

- **Quyết định:** Mỗi nghiệp vụ = 1 transaction mở/đóng tại **Service**; DAO không tự commit.
- **Lý do:** Bán hàng/nhập hàng đụng nhiều bảng (invoice + items + stock + stock_movements) → cần atomic.
- **Trade-off:** DAO phải nhận `Connection` dùng chung trong 1 tx (thay vì tự lấy) → thêm chút truyền tham số. Chấp nhận để đảm bảo nhất quán.
- **Trạng thái:** ✅ Đã chấp nhận.



### ADR-003 — Một SQLite Connection dùng chung (không connection pool)

- **Quyết định:** Giữ **1** `Connection` dùng lại toàn app (truy cập có đồng bộ hóa).
- **Lý do:** 1 người dùng, SQLite mặc định 1 writer; pool là thừa (YAGNI).
- **Trade-off:** Phải đảm bảo thao tác DB chạy tuần tự (đã đúng vì 1 background executor). Nếu sau này cần đọc song song → cân nhắc pool nhỏ.
- **Trạng thái:** ✅ Đã chấp nhận.



### ADR-004 — Quy tắc "lite": khi nào tách ViewModel

- **Quyết định:** Tách ViewModel khi màn hình có **state phức tạp/tính toán** (POS, Dashboard). CRUD đơn giản để Controller kiêm luôn.
- **Lý do:** Tránh boilerplate cho CRUD; tập trung công sức test vào nơi có logic.
- **Trạng thái:** ✅ Đã chấp nhận.



### ADR-005 — DB & log ghi ở `%APPDATA%/ShopManager`

- **Quyết định:** `shop.db` và `logs/` đặt ở `%APPDATA%`, không trong thư mục cài `.exe`.
- **Lý do:** Thư mục `Program Files` thường bị chặn quyền ghi.
- **Trạng thái:** ✅ Đã chấp nhận.

---



## 7. Ánh xạ tầng ↔ package

```
com.shop
 ├─ view/          (Controller + tham chiếu FXML)   ← View-Controller
 ├─ viewmodel/     (PosViewModel, DashboardViewModel) ← chỉ màn hình phức tạp
 ├─ service/       (SalesService, ProductService, AuthService...)
 ├─ dao/           (InvoiceDao, ProductDao, StockMovementDao...)
 ├─ model/         (Invoice, Product, Customer, Unit...)
 ├─ infra/db/      (DBConnection, SchemaInitializer)
 ├─ infra/log/     (LogContext, Mask)
 ├─ config/        (AppPaths)
 └─ util/          (Money, DateFmt, PdfExporter)
resources/  fxml/ · css/ · db/schema.sql · reports/ · logback.xml
```

---



## 8. Validation Checklist

- [x] Requirements rõ ràng (theo MVP-plan đã chốt)
- [x] Ràng buộc đã nhận diện (offline, 1 user, %APPDATA%, JavaFX threading)
- [x] Mỗi quyết định lớn có trade-off (ADR-001..005)
- [x] Đã cân nhắc phương án đơn giản hơn (MVC; gộp ViewModel)
- [x] ADR đã ghi lại
- [x] Pattern khớp năng lực đội (1 dev, ưu tiên đơn giản)

---



## 9. Rủi ro kiến trúc & cách giảm


| Rủi ro                               | Giảm thiểu                                             |
| ------------------------------------ | ------------------------------------------------------ |
| Controller phình to (God controller) | Quy tắc "lite" + tách ViewModel cho POS/Dashboard      |
| Rò rỉ logic nghiệp vụ xuống DAO      | Code review theo luật phụ thuộc; DAO chỉ SQL           |
| Gọi DB trên UI thread gây đơ         | Mọi nghiệp vụ qua `Task`/background executor           |
| Transaction nửa vời khi bán          | tx ở Service + rollback; test luồng UC-04              |
| Tồn kho âm lan rộng                  | log WARN + `stock_movements` truy vết + kiểm kho (GĐ2) |


