# Index: Ma trận Test Case — Phase 1 MVP

> Tạo bởi: Senior QA Automation Engineer  
> Dựa trên Spec: [docs/specs/phase-1-mvp/](../specs/phase-1-mvp/)  
> Ngày tạo: 2026-08-13  
> Kỹ thuật áp dụng: Equivalence Partitioning, Boundary Value Analysis

---

## Tổng quan

| Module | File | Số Test Case | P0 | P1 | P2 |
|--------|------|-------------|-----|-----|-----|
| TC-01 — Đăng nhập | [TC-01-dang-nhap.md](TC-01-dang-nhap.md) | 27 | 12 | 10 | 5 |
| TC-02 — Danh mục | [TC-02-danh-muc.md](TC-02-danh-muc.md) | 26 | 14 | 9 | 3 |
| TC-03 — Dashboard | [TC-03-dashboard.md](TC-03-dashboard.md) | 27 | 13 | 11 | 3 |
| TC-04 — Khách hàng | [TC-04-khach-hang.md](TC-04-khach-hang.md) | 45 | 22 | 15 | 8 |
| TC-05 — Bán hàng (POS) | [TC-05-ban-hang.md](TC-05-ban-hang.md) | 41 | 25 | 12 | 4 |
| TC-06 — Mua hàng | [TC-06-mua-hang.md](TC-06-mua-hang.md) | 39 | 22 | 13 | 4 |
| TC-07 — Quản lý Hàng hóa | [TC-07-quan-ly-hang-hoa.md](TC-07-quan-ly-hang-hoa.md) | 42 | 23 | 14 | 5 |
| TC-08 — Hệ thống | [TC-08-he-thong.md](TC-08-he-thong.md) | 42 | 22 | 16 | 4 |
| **TỔNG** | | **289** | **153** | **100** | **36** |

---

## Phân loại theo Type

| Type | Số Test Case | Mô tả |
|------|-------------|-------|
| **Functional** | ~145 | Kiểm tra luồng chính và nghiệp vụ bình thường |
| **Negative** | ~65 | Kiểm tra xử lý lỗi, dữ liệu không hợp lệ |
| **Edge Case** | ~53 | Kiểm tra tình huống biên, đặc biệt |
| **Boundary** | ~20 | Kiểm tra giá trị biên (Boundary Value Analysis) |
| **Security** | ~6 | Kiểm tra bảo mật cơ bản (không lộ data, không dùng double cho tiền) |

---

## Phân loại theo Priority

| Priority | Số TC | Tiêu chí |
|----------|-------|---------|
| **P0** | 153 | Business-critical — Block nếu fail. Phải pass trước khi release. |
| **P1** | 100 | Quan trọng — Ảnh hưởng UX đáng kể. Cần pass trước release. |
| **P2** | 36 | Nice-to-have — Edge case ít gặp. Có thể dời sau nếu cần. |

---

## Kỹ thuật phân tích được áp dụng

### Equivalence Partitioning (Phân vùng tương đương)
- **SĐT:** Valid (10 chữ số) / Invalid (< 10) / Invalid (> 10) / Non-numeric
- **Email:** Valid format / Invalid format / Empty (optional)
- **Số tiền:** > 0 / = 0 / < 0
- **Tên:** Non-empty / Empty / Whitespace-only
- **Giảm giá %:** 0-100% (valid) / < 0 / > 100

### Boundary Value Analysis (Phân tích giá trị biên)
- **SĐT:** 9 chữ số (invalid), 10 chữ số (valid), 11 chữ số (invalid)
- **Mật khẩu:** 3 ký tự (invalid), 4 ký tự (valid boundary)
- **Giảm giá:** 0% (min valid), 100% (max valid)
- **Giá bán:** -1 (invalid), 0 (valid min), > 0 (valid)
- **Dashboard load:** ≤ 2 giây (target), > 2 giây (fail)
- **Logo size:** ≤ 200KB (ok), > 200KB (cảnh báo)
- **Log lines:** ≤ 500 dòng (max hiển thị)
- **Stock movements:** ≤ 1.000 dòng (max hiển thị)

---

## Nhóm Test Cases theo Cross-cutting Concerns

### Threading & Performance
Tất cả thao tác DB nặng PHẢI chạy trên background thread:

| Test ID | Mô tả |
|---------|-------|
| TC-01-012 | BCrypt verify không đơ UI |
| TC-01-023 | BCrypt hash không đơ UI |
| TC-03-025 | Dashboard load không đơ UI |
| TC-04-045 | Tìm kiếm KH < 300ms |
| TC-05-040 | Thanh toán không đơ UI |
| TC-06-039 | Tạo phiếu nhập không đơ UI |
| TC-07-041 | Tìm kiếm SP < 300ms |
| TC-08-027 | Đọc log không đơ UI |

### Transaction Atomicity
PHẢI rollback khi lỗi DB:

| Test ID | Mô tả |
|---------|-------|
| TC-05-026 | Bán hàng — rollback khi lỗi DB |
| TC-06-018 | Tạo phiếu nhập — rollback khi lỗi DB |
| TC-06-036 | Trả nợ NCC — rollback khi lỗi DB |
| TC-08-040 | stock_movements không ghi khi rollback |

### Data Integrity
- `stock_qty` luôn = SUM(`inventory_batches.qty_remaining`): TC-06-037
- Mọi thay đổi kho phải ghi `stock_movements`: TC-07-042, TC-08-038, TC-08-039
- Soft-delete giữ nguyên data lịch sử: TC-04-026, TC-05-033

---

## Ghi chú

> **Automation Priority:** P0 cases nên được tự động hóa trước, đặc biệt nhóm Transaction Atomicity và Threading.
> 
> **Manual Testing:** P2 cases (edge cases hiếm gặp, UI visual) phù hợp kiểm tra thủ công.
> 
> **Regression Suite:** Toàn bộ P0 + P1 cases nên là regression suite chạy trước mỗi lần release.
