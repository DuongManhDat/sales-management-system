# Database Schema — Hệ thống Quản lý Bán hàng (SQLite)

> Tài liệu schema · Phiên bản 1.0 · 2026-06-27
> Tinh chỉnh từ [MVP-plan.md §7](./MVP-plan.md) · DB: **SQLite** (đã chốt) · File: `db/schema.sql`

---

## 1. Quyết định thiết kế (Design Decisions)

| # | Quyết định | Lý do |
|---|---|---|
| D1 | **Tiền tệ = INTEGER (đồng)** | VND không có đơn vị nhỏ hơn đồng → số nguyên là **chính xác tuyệt đối**, tránh sai số `double` |
| D2 | **Số lượng = REAL** | Hỗ trợ cân ký (1.5 kg, 0.5 lít) — danh mục đơn vị có kg/lít |
| D3 | **Thời gian = TEXT ISO-8601 giờ địa phương** (`YYYY-MM-DD HH:MM:SS`) | `strftime()` của SQLite chạy trực tiếp để thống kê Dashboard theo **giờ/thứ/ngày**; lưu giờ local để `%H`,`%w` đúng múi giờ |
| D4 | **Bảng STRICT** (SQLite ≥ 3.37) | Ép kiểu cột nghiêm ngặt, bắt lỗi sớm thay vì SQLite "type affinity" lỏng lẻo |
| D5 | **`INTEGER PRIMARY KEY`** (alias `rowid`) | PK tự tăng gọn, nhanh; **không** dùng `AUTOINCREMENT` (thừa overhead) |
| D6 | **Soft-delete bằng `status`** (`ACTIVE`/`INACTIVE`) | Không xóa cứng SP/khách đã gắn hóa đơn (giữ toàn vẹn lịch sử — UC-05/UC-07) |
| D7 | **FK bật + action rõ ràng** | `PRAGMA foreign_keys=ON`; chi tiết (items) `CASCADE`, danh mục `RESTRICT` |
| D8 | **`code` do app sinh** (HD000123, PN000045) | Tách `id` (kỹ thuật) khỏi `code` (hiển thị); `code` `UNIQUE` |

---

## 2. Sơ đồ quan hệ (ERD — text)

```
                 ┌───────────┐        ┌──────────────┐
                 │   units   │        │  categories  │
                 └─────┬─────┘        └──────┬───────┘
                       │ 1                    │ 1
                       │                      │ (nullable)
                       ▼ N                    ▼ N
                 ┌──────────────────────────────────┐
                 │             products              │
                 └───┬───────────────┬───────────┬──┘
            N        │ 1          N  │ 1      N  │ 1
   ┌────────────────┘               │           └──────────────┐
   ▼                                ▼                          ▼
┌────────────────┐         ┌────────────────┐        ┌──────────────────┐
│ purchase_items │         │ invoice_items  │        │ stock_movements  │
└──────┬─────────┘         └──────┬─────────┘        └──────────────────┘
       │ N                        │ N
       ▼ 1                        ▼ 1
┌────────────┐    ┌──────────┐  ┌──────────┐
│ purchases  │    │customers │◄─┤ invoices │ (customer_id nullable = khách lẻ)
└────────────┘    └──────────┘  └──────────┘

  app_user (1 dòng) · settings (key-value) · activity_log [GĐ2]
```

---

## 3. DDL hoàn chỉnh (`db/schema.sql`)

```sql
PRAGMA foreign_keys = ON;

-- ============ DANH MỤC ============
CREATE TABLE IF NOT EXISTS units (
    id      INTEGER PRIMARY KEY,
    name    TEXT NOT NULL UNIQUE,
    status  TEXT NOT NULL DEFAULT 'ACTIVE'   -- ACTIVE | INACTIVE
) STRICT;

CREATE TABLE IF NOT EXISTS categories (
    id      INTEGER PRIMARY KEY,
    name    TEXT NOT NULL UNIQUE,
    status  TEXT NOT NULL DEFAULT 'ACTIVE'   -- ACTIVE | INACTIVE (soft-delete, đồng nhất với units)
) STRICT;

-- ============ HÀNG HÓA ============
CREATE TABLE IF NOT EXISTS products (
    id          INTEGER PRIMARY KEY,
    code        TEXT    NOT NULL UNIQUE,
    name        TEXT    NOT NULL,
    unit_id     INTEGER NOT NULL REFERENCES units(id)      ON DELETE RESTRICT,
    category_id INTEGER          REFERENCES categories(id) ON DELETE SET NULL,
    cost_price  INTEGER NOT NULL DEFAULT 0,    -- đồng (giá vốn)
    sale_price  INTEGER NOT NULL DEFAULT 0,    -- đồng (giá bán)
    stock_qty   REAL    NOT NULL DEFAULT 0,    -- tồn kho (cho phép số lẻ)
    status      TEXT    NOT NULL DEFAULT 'ACTIVE',
    created_at  TEXT    NOT NULL,
    updated_at  TEXT    NOT NULL,
    CHECK (cost_price >= 0 AND sale_price >= 0)
) STRICT;

-- ============ KHÁCH HÀNG ============
CREATE TABLE IF NOT EXISTS customers (
    id          INTEGER PRIMARY KEY,
    name        TEXT    NOT NULL,
    phone       TEXT,
    address     TEXT,
    status      TEXT    NOT NULL DEFAULT 'ACTIVE',
    created_at  TEXT    NOT NULL
) STRICT;

-- ============ MUA HÀNG (Nhập) ============
CREATE TABLE IF NOT EXISTS purchases (
    id            INTEGER PRIMARY KEY,
    code          TEXT    NOT NULL UNIQUE,
    supplier      TEXT,                         -- MVP: text; GĐ2 tách bảng suppliers
    purchase_date TEXT    NOT NULL,
    total         INTEGER NOT NULL DEFAULT 0,   -- đồng
    note          TEXT,
    created_at    TEXT    NOT NULL
) STRICT;

CREATE TABLE IF NOT EXISTS purchase_items (
    id          INTEGER PRIMARY KEY,
    purchase_id INTEGER NOT NULL REFERENCES purchases(id) ON DELETE CASCADE,
    product_id  INTEGER NOT NULL REFERENCES products(id)  ON DELETE RESTRICT,
    qty         REAL    NOT NULL,
    cost_price  INTEGER NOT NULL,               -- đồng (giá nhập tại thời điểm)
    amount      INTEGER NOT NULL,               -- = qty * cost_price (đồng)
    CHECK (qty > 0)
) STRICT;

-- ============ BÁN HÀNG (Hóa đơn) ============
CREATE TABLE IF NOT EXISTS invoices (
    id           INTEGER PRIMARY KEY,
    code         TEXT    NOT NULL UNIQUE,
    customer_id  INTEGER          REFERENCES customers(id) ON DELETE SET NULL, -- NULL = khách lẻ
    invoice_date TEXT    NOT NULL,              -- ISO local, dùng cho Dashboard
    subtotal     INTEGER NOT NULL DEFAULT 0,    -- đồng
    discount     INTEGER NOT NULL DEFAULT 0,    -- đồng
    total        INTEGER NOT NULL DEFAULT 0,    -- đồng
    paid         INTEGER NOT NULL DEFAULT 0,    -- đồng (tiền khách trả)
    status       TEXT    NOT NULL DEFAULT 'COMPLETED', -- COMPLETED | (GĐ2: ORDER, RETURNED)
    created_at   TEXT    NOT NULL,
    CHECK (discount >= 0 AND total >= 0)
) STRICT;

CREATE TABLE IF NOT EXISTS invoice_items (
    id          INTEGER PRIMARY KEY,
    invoice_id  INTEGER NOT NULL REFERENCES invoices(id) ON DELETE CASCADE,
    product_id  INTEGER NOT NULL REFERENCES products(id) ON DELETE RESTRICT,
    qty         REAL    NOT NULL,
    sale_price  INTEGER NOT NULL,               -- đồng (giá bán tại thời điểm)
    cost_price  INTEGER NOT NULL DEFAULT 0,     -- đồng (giá vốn tại thời điểm bán — dùng tính lợi nhuận Dashboard)
    amount      INTEGER NOT NULL,               -- = qty * sale_price (đồng)
    CHECK (qty > 0)
) STRICT;

-- ============ LOG NGHIỆP VỤ ============
CREATE TABLE IF NOT EXISTS stock_movements (
    id          INTEGER PRIMARY KEY,
    product_id  INTEGER NOT NULL REFERENCES products(id) ON DELETE RESTRICT,
    type        TEXT    NOT NULL,               -- NHAP | BAN | TRA | KIEMKHO
    qty_change  REAL    NOT NULL,               -- +nhập / -bán
    stock_after REAL    NOT NULL,               -- tồn SAU thay đổi (truy vết)
    ref_type    TEXT,                           -- INVOICE | PURCHASE | ADJUST
    ref_id      INTEGER,                        -- id chứng từ liên quan
    note        TEXT,
    created_at  TEXT    NOT NULL
) STRICT;

-- ============ HỆ THỐNG ============
CREATE TABLE IF NOT EXISTS app_user (
    id            INTEGER PRIMARY KEY,
    username      TEXT NOT NULL DEFAULT 'owner',
    password_hash TEXT NOT NULL,
    created_at    TEXT NOT NULL
) STRICT;

CREATE TABLE IF NOT EXISTS settings (
    key   TEXT PRIMARY KEY,
    value TEXT
) STRICT;

-- [GĐ2] Nhật ký hoạt động (audit)
CREATE TABLE IF NOT EXISTS activity_log (
    id         INTEGER PRIMARY KEY,
    action     TEXT NOT NULL,                   -- UPDATE_PRICE | DELETE_PRODUCT...
    entity     TEXT,                            -- product | invoice...
    entity_id  INTEGER,
    detail     TEXT,                            -- "5000 -> 6000"
    created_at TEXT NOT NULL
) STRICT;
```

---

## 4. Chiến lược Index (tránh anti-pattern "skip indexing")

```sql
-- Tìm/lọc hàng hóa (màn danh sách + POS)
CREATE INDEX IF NOT EXISTS idx_products_name      ON products(name);
CREATE INDEX IF NOT EXISTS idx_products_category  ON products(category_id);
-- (code, unit_id: code đã UNIQUE; FK unit_id ít lọc nên bỏ qua)

-- Khách hàng (tìm theo SĐT / tên)
CREATE INDEX IF NOT EXISTS idx_customers_phone    ON customers(phone);
CREATE INDEX IF NOT EXISTS idx_customers_name     ON customers(name);

-- Dashboard: thống kê doanh thu theo thời gian
CREATE INDEX IF NOT EXISTS idx_invoices_date      ON invoices(invoice_date);
CREATE INDEX IF NOT EXISTS idx_invoices_customer  ON invoices(customer_id);

-- Chi tiết hóa đơn / top sản phẩm bán chạy
CREATE INDEX IF NOT EXISTS idx_invoice_items_inv  ON invoice_items(invoice_id);
CREATE INDEX IF NOT EXISTS idx_invoice_items_prod ON invoice_items(product_id);

-- Phiếu nhập
CREATE INDEX IF NOT EXISTS idx_purchase_items_pur ON purchase_items(purchase_id);
CREATE INDEX IF NOT EXISTS idx_purchase_items_prod ON purchase_items(product_id);

-- Lịch sử tồn kho theo sản phẩm + thời gian
CREATE INDEX IF NOT EXISTS idx_stockmove_prod_time ON stock_movements(product_id, created_at);
```

**Nguyên tắc index ở đây:** chỉ index cột thực sự dùng để **lọc/sắp xếp/join** trong các use case đã biết (tìm SP, Dashboard theo ngày, top SP, lịch sử tồn). Không index tràn lan để khỏi chậm ghi.

---

## 5. Seed dữ liệu khởi tạo

```sql
INSERT OR IGNORE INTO units(id, name) VALUES
 (1,'cái'),(2,'hộp'),(3,'gói'),(4,'chai'),(5,'lon'),(6,'kg'),(7,'lít'),(8,'thùng');

INSERT OR IGNORE INTO settings(key, value) VALUES
 ('shop_name',''),('shop_address',''),('shop_phone',''),('schema_version','1');
```

---

## 6. Ví dụ truy vấn Dashboard (tận dụng `strftime`)

```sql
-- Doanh thu theo GIỜ trong ngày hôm nay
SELECT strftime('%H', invoice_date) AS gio, SUM(total) AS doanh_thu
FROM invoices
WHERE date(invoice_date) = date('now','localtime') AND status='COMPLETED'
GROUP BY gio ORDER BY gio;

-- Doanh thu theo THỨ trong tuần (%w: 0=CN..6=T7)
SELECT strftime('%w', invoice_date) AS thu, SUM(total) AS doanh_thu
FROM invoices
WHERE invoice_date >= date('now','-7 days') AND status='COMPLETED'
GROUP BY thu;

-- Top sản phẩm bán chạy (trong ngày :date, theo doanh thu)
SELECT p.name, p.unit_id, SUM(ii.qty) AS so_luong, SUM(ii.amount) AS doanh_thu
FROM invoice_items ii
     JOIN invoices i  ON i.id  = ii.invoice_id
     JOIN products p  ON p.id  = ii.product_id
WHERE date(i.invoice_date) = :date AND i.status = 'COMPLETED'
GROUP BY ii.product_id
ORDER BY so_luong DESC
LIMIT 5;

-- Lợi nhuận gộp trong ngày :date
-- (yêu cầu invoice_items.cost_price đã được ghi khi tạo hóa đơn)
SELECT SUM(ii.amount - ii.cost_price * ii.qty) AS loi_nhuan_gop
FROM invoice_items ii
     JOIN invoices i ON i.id = ii.invoice_id
WHERE date(i.invoice_date) = :date AND i.status = 'COMPLETED';

-- Hàng sắp hết tồn kho (ngưỡng :threshold = 5 | 10 | 20)
SELECT p.code, p.name, u.name AS don_vi, p.stock_qty
FROM products p JOIN units u ON u.id = p.unit_id
WHERE p.stock_qty <= :threshold AND p.status = 'ACTIVE'
ORDER BY p.stock_qty ASC;
```

> ⚠️ Tránh `SELECT *` trong code thật — liệt kê rõ cột (anti-pattern của skill).

---

## 7. Decision Checklist (database-design skill)

- [x] Đã hỏi/ xác nhận DB → **SQLite** (đã chốt từ MVP plan)
- [x] DB phù hợp ngữ cảnh (offline, 1 user, nhúng)
- [x] Cân nhắc môi trường triển khai (file ở `%APPDATA%`)
- [x] Lập chiến lược index (theo use case thực tế)
- [x] Định nghĩa loại quan hệ + FK action (CASCADE/RESTRICT/SET NULL)
- [x] Đã hỏi điểm chưa rõ ảnh hưởng kiểu cột (qty REAL)

---

## 8. Lưu ý vận hành

- **Bật FK mỗi kết nối:** SQLite tắt FK mặc định → `PRAGMA foreign_keys=ON` ngay sau khi mở `Connection` (đã có trong `DBConnection`).
- **Atomic theo nghiệp vụ:** ghi `invoices`+`invoice_items`+trừ `products.stock_qty`+`stock_movements` trong **1 transaction** (xem [architecture.md ADR-002](./architecture.md)).
- **Bán âm kho:** schema **không** chặn `stock_qty < 0` (theo quyết định nghiệp vụ); chỉ cảnh báo ở tầng Service.
- **Versioning:** `settings.schema_version` để sau này migrate khi đổi schema (GĐ2).
- **`invoice_items.cost_price`:** phải được lấy từ `products.cost_price` tại **thời điểm tạo hóa đơn** và lưu vào `invoice_items`; không tính ngược từ `products` sau khi giá vốn đã thay đổi.

---

## 9. Schema Migrations

| Version | Ngày | Thay đổi | Lý do |
|---------|------|----------|---------|
| v1.0 | 2026-06-27 | Schema khởi tạo (tất cả bảng) | MVP ban đầu |
| v1.1 | 2026-07-11 | Thêm `invoice_items.cost_price INTEGER NOT NULL DEFAULT 0` | Yêu cầu tính lợi nhuận gộp cho Dashboard (xem [Dashboard spec §4.1](./specs/phase-1-mvp/dashboard/spec.md)) |

### Migration script v1.0 → v1.1

```sql
-- Chạy một lần khi nâng phiên bản schema từ v1.0 lên v1.1
-- Kiểm tra version trước khi chạy: SELECT value FROM settings WHERE key='schema_version';

ALTER TABLE invoice_items ADD COLUMN cost_price INTEGER NOT NULL DEFAULT 0;
-- Ghi chú: dữ liệu hóa đơn cũ sẽ có cost_price = 0
-- → lợi nhuận gộp của các đơn cũ sẽ bằng doanh thu (chấp nhận được ở MVP)

UPDATE settings SET value = '2' WHERE key = 'schema_version';
```

> ⚠️ `SchemaInitializer` phải kiểm tra `schema_version` khi khởi động và tự động chạy migration script nếu phiên bản DB thấp hơn phiên bản app.
