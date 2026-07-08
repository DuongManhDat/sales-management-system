# Spec: Đăng nhập (Authentication)

> Module: phase-1-mvp/dang-nhap · Phiên bản: 1.0 · Ngày: 2026-07-08
> Tham chiếu: [MVP-plan.md](../../MVP-plan.md) · [architecture.md](../../architecture.md) · [database-schema.md](../../database-schema.md)

---

## 1. Executive Summary

Module **Đăng nhập** là cổng bảo vệ duy nhất của ứng dụng — đảm bảo chỉ chủ cửa hàng mới truy cập được dữ liệu nhạy cảm (doanh thu, khách hàng, tồn kho). Chức năng bao gồm:

- **Đăng nhập:** Nhập mật khẩu mỗi lần mở app → xác thực bằng BCrypt → vào màn hình chính.
- **Thiết lập mật khẩu lần đầu:** Khi app mới cài (DB trống) → yêu cầu tạo mật khẩu (nhập + xác nhận) → lưu hash vào DB → quay về form đăng nhập.

**Ràng buộc đã chốt:**
- Một người dùng duy nhất (chủ cửa hàng), không phân quyền role.
- Mật khẩu hash bằng **BCrypt**, không lưu thô.
- **Không auto-login** — mỗi lần mở app đều phải nhập mật khẩu.
- **Không giới hạn** số lần nhập sai.
- **Quên mật khẩu** = xóa file DB và bắt đầu lại (không có cơ chế khôi phục).
- Đổi mật khẩu thuộc module **Cài đặt** (spec riêng).

---

## 2. Requirements & Use Cases

### 2.1 Functional Requirements

| ID    | Yêu cầu                                                                 | Độ ưu tiên |
| ----- | ------------------------------------------------------------------------ | ---------- |
| FR-01 | Hiển thị form đăng nhập khi mở app (nếu đã có mật khẩu trong DB)        | Bắt buộc   |
| FR-02 | Hiển thị form thiết lập mật khẩu khi mở app lần đầu (`app_user` trống)  | Bắt buộc   |
| FR-03 | Xác thực mật khẩu bằng BCrypt hash                                      | Bắt buộc   |
| FR-04 | Hiện popup "Sai mật khẩu" khi nhập sai                                  | Bắt buộc   |
| FR-05 | Nhấn Enter trong ô mật khẩu tương đương bấm nút "Đăng nhập"            | Bắt buộc   |
| FR-06 | Nút hiện/ẩn mật khẩu (toggle visibility) trên cả 2 form                | Bắt buộc   |
| FR-07 | Hiển thị phiên bản app trên màn hình đăng nhập                          | Bắt buộc   |
| FR-08 | Validation thiết lập: mật khẩu khác rỗng + 2 ô phải khớp nhau          | Bắt buộc   |
| FR-09 | Sau thiết lập thành công → popup thành công → quay về form đăng nhập    | Bắt buộc   |
| FR-10 | Đăng nhập thành công → chuyển sang Main Window                           | Bắt buộc   |

### 2.2 Non-functional Requirements

| ID     | Yêu cầu                                                                          |
| ------ | --------------------------------------------------------------------------------- |
| NFR-01 | Kiểm tra `app_user` khi khởi động phải nhanh (< 200ms)                           |
| NFR-02 | Hash/verify BCrypt chạy trên background thread (không đơ UI)                      |
| NFR-03 | Không lưu mật khẩu thô ở bất kỳ đâu — chỉ lưu hash trong DB                     |

### 2.3 Use Cases

#### UC-01 — Đăng nhập

- **Actor:** Chủ cửa hàng
- **Tiền điều kiện:** App đã cài. Đã thiết lập mật khẩu (bảng `app_user` có ít nhất 1 bản ghi).

**Luồng chính:**

```
1. Người dùng mở app.
2. Hệ thống kiểm tra bảng `app_user`:
   → Có bản ghi → hiển thị form "Đăng nhập".
3. Người dùng nhập mật khẩu vào ô PasswordField.
4. Người dùng bấm [Đăng nhập] (hoặc nhấn Enter).
5. Hệ thống verify mật khẩu với BCrypt hash (trên background thread).
6. Khớp → chuyển sang Main Window (màn hình chính với sidebar).
```

**Luồng ngoại lệ:**

| Bước | Điều kiện              | Xử lý                                                   |
| ---- | ---------------------- | -------------------------------------------------------- |
| 4    | Ô mật khẩu trống      | Disable nút "Đăng nhập" (hoặc không phản hồi)           |
| 5    | Mật khẩu sai          | Hiện popup: "Sai mật khẩu. Vui lòng thử lại." → xóa ô mật khẩu, focus lại ô |

#### UC-01b — Thiết lập mật khẩu lần đầu

- **Actor:** Chủ cửa hàng
- **Tiền điều kiện:** App mới cài hoặc DB mới tạo. Bảng `app_user` trống (chưa có bản ghi nào).

**Luồng chính:**

```
1. Người dùng mở app.
2. Hệ thống kiểm tra bảng `app_user`:
   → Trống → hiển thị form "Thiết lập mật khẩu".
3. Người dùng nhập mật khẩu vào ô "Mật khẩu".
4. Người dùng nhập lại mật khẩu vào ô "Xác nhận mật khẩu".
5. Người dùng bấm [Xác nhận] (hoặc nhấn Enter).
6. Hệ thống validation:
   → Cả 2 ô khác rỗng VÀ giá trị khớp nhau → tiếp tục.
7. Hệ thống hash mật khẩu bằng BCrypt (trên background thread).
8. INSERT bản ghi vào `app_user` (username='owner', password_hash, created_at).
9. Hiện popup: "Thiết lập mật khẩu thành công!"
10. Người dùng bấm [OK] → chuyển về form "Đăng nhập" (UC-01).
```

**Luồng ngoại lệ:**

| Bước | Điều kiện                          | Xử lý                                                        |
| ---- | ---------------------------------- | ------------------------------------------------------------- |
| 5    | Ô "Mật khẩu" trống                | Hiện popup: "Vui lòng nhập mật khẩu."                        |
| 5    | Ô "Xác nhận mật khẩu" trống       | Hiện popup: "Vui lòng xác nhận mật khẩu."                    |
| 6    | 2 ô không khớp nhau               | Hiện popup: "Mật khẩu xác nhận không khớp." → xóa ô xác nhận, focus lại |
| 8    | Lỗi DB khi insert                 | Hiện popup lỗi hệ thống, giữ nguyên form để thử lại          |

---

## 3. UI/UX & Navigation

### 3.1 Luồng điều hướng khi mở app

```
[App khởi động]
    │
    ├── Kiểm tra bảng `app_user`
    │
    ├── Trống (lần đầu)
    │     └── [Form Thiết lập mật khẩu]
    │           │ (thiết lập thành công)
    │           └── popup "Thành công" → [Form Đăng nhập]
    │
    └── Có bản ghi
          └── [Form Đăng nhập]
                │ (đăng nhập thành công)
                └── [Main Window + Sidebar]
```

### 3.2 Wireframe — Form Đăng nhập

```
┌──────────────────────────────────────────────┐
│                                                │
│              🏪  QUẢN LÝ BÁN HÀNG              │
│                                                │
│        Mật khẩu:  [ •••••••••••• ] [👁️]       │
│                                                │
│                 [  Đăng nhập  ]                │
│                                                │
│                                    v1.0.0      │
└──────────────────────────────────────────────┘
```

> **Kích thước cửa sổ:** Nhỏ gọn (~400×300px), căn giữa màn hình. Không resize.
> Không phải full-size như Main Window.

### 3.3 Wireframe — Form Thiết lập mật khẩu (lần đầu)

```
┌──────────────────────────────────────────────┐
│                                                │
│              🏪  QUẢN LÝ BÁN HÀNG              │
│          Thiết lập mật khẩu lần đầu            │
│                                                │
│      Mật khẩu:          [ •••••••••• ] [👁️]   │
│      Xác nhận mật khẩu: [ •••••••••• ] [👁️]   │
│                                                │
│                 [  Xác nhận  ]                 │
│                                                │
│                                    v1.0.0      │
└──────────────────────────────────────────────┘
```

### 3.4 Chi tiết các thành phần UI

| Thành phần              | Loại control                  | Hành vi                                                                |
| ----------------------- | ----------------------------- | ---------------------------------------------------------------------- |
| Tiêu đề "QUẢN LÝ BÁN HÀNG" | `Label` (bold, cỡ lớn)    | Hiển thị tên app, căn giữa                                            |
| Ô mật khẩu              | `PasswordField`              | Ẩn ký tự bằng dấu chấm. Nhấn Enter = bấm nút chính.                  |
| Nút [👁️] toggle         | `ToggleButton` hoặc `Button` | Bấm → chuyển `PasswordField` ↔ `TextField` (hiện/ẩn mật khẩu)        |
| Ô xác nhận mật khẩu     | `PasswordField`              | Chỉ hiện trên form thiết lập. Cũng có nút [👁️] riêng.                |
| Nút [Đăng nhập]         | `Button`                      | Disabled khi ô mật khẩu trống. Là nút mặc định (Enter kích hoạt).    |
| Nút [Xác nhận]          | `Button`                      | Disabled khi bất kỳ ô nào trống. Là nút mặc định trên form thiết lập. |
| Label phiên bản          | `Label` (nhỏ, mờ)            | Hiển thị `v1.0.0` ở góc dưới phải. Đọc từ `pom.xml` hoặc constant.   |

### 3.5 Hành vi Toggle hiện/ẩn mật khẩu

```
Trạng thái MẶC ĐỊNH:
  [PasswordField: ••••••••] [👁️]     ← mật khẩu bị ẩn

Bấm [👁️]:
  [TextField: abc12345  ] [🔒]     ← mật khẩu hiện rõ

Bấm [🔒]:
  [PasswordField: ••••••••] [👁️]     ← quay về ẩn
```

> **Kỹ thuật:** Dùng `StackPane` chứa cả `PasswordField` lẫn `TextField`, toggle visibility. Giá trị 2 field được bind 2 chiều (`bidirectionalBind`).

---

## 4. Data Models & State

### 4.1 Bảng Database liên quan

```sql
-- Người dùng (1 bản ghi duy nhất)
CREATE TABLE IF NOT EXISTS app_user (
    id            INTEGER PRIMARY KEY,
    username      TEXT NOT NULL DEFAULT 'owner',
    password_hash TEXT NOT NULL,              -- BCrypt hash
    created_at    TEXT NOT NULL               -- ISO 8601
) STRICT;
```

> **Lưu ý:** Bảng này chỉ có **1 bản ghi duy nhất** (chủ cửa hàng). Không hỗ trợ multi-user.

### 4.2 State Changes

**Khi thiết lập mật khẩu lần đầu (UC-01b):**

```
1. INSERT → app_user (username='owner', password_hash=BCrypt.hash(input), created_at=now())
```

**Khi đăng nhập (UC-01):**

```
1. SELECT password_hash FROM app_user WHERE id = 1
2. BCrypt.verify(input, password_hash)
   → true  → cho phép vào Main Window
   → false → hiện popup "Sai mật khẩu"
```

> Đăng nhập **chỉ đọc** — không thay đổi dữ liệu nào trong DB.

---

## 5. Integration & Architecture

### 5.1 Layer Flow (theo kiến trúc MVVM-lite)

```
┌──────────────┐    ┌──────────────────┐    ┌──────────────┐    ┌──────────────┐
│  View (FXML) │───▶│ LoginController  │───▶│  AuthService  │───▶│  AppUserDao   │
│              │    │ (bind + event)   │    │ (BCrypt logic)│    │ (SQL thuần)   │
└──────────────┘    └──────────────────┘    └──────────────┘    └──────────────┘
```

> **Đăng nhập là màn hình đơn giản** → Controller kiêm luôn ViewModel (theo quy tắc "lite" trong [architecture.md](../../architecture.md)). Không cần tách `LoginViewModel` riêng.

### 5.2 Các class liên quan

| Layer      | Class              | Trách nhiệm                                                          |
| ---------- | ------------------ | --------------------------------------------------------------------- |
| View       | `login-view.fxml`  | Layout form đăng nhập (ô mật khẩu, nút toggle, nút đăng nhập, label version) |
| View       | `setup-view.fxml`  | Layout form thiết lập mật khẩu (2 ô + nút xác nhận)                  |
| Controller | `LoginController`  | Bind FXML controls, xử lý sự kiện [Đăng nhập], gọi `AuthService`    |
| Controller | `SetupController`  | Bind FXML controls, validation 2 ô, gọi `AuthService`               |
| Service    | `AuthService`      | `login(password)` — verify BCrypt; `setup(password)` — hash + insert |
| DAO        | `AppUserDao`       | `findFirst()` — kiểm tra có user chưa; `insert(user)` — tạo user    |
| Infra      | `App.java`         | Entry point — kiểm tra `app_user` → quyết định hiện form nào        |

### 5.3 Threading Model

```
[JavaFX App Thread]
    │
    ├── Khởi động: AppUserDao.findFirst() → nhanh (< 200ms), chạy sync được
    │
    ├── Đăng nhập: AuthService.login() → background Task
    │     ├── BCrypt.checkpw() (CPU-intensive ~100ms)
    │     └── Platform.runLater() → chuyển Main Window hoặc hiện popup lỗi
    │
    └── Thiết lập MK: AuthService.setup() → background Task
          ├── BCrypt.hashpw() + AppUserDao.insert()
          └── Platform.runLater() → hiện popup thành công → chuyển form đăng nhập
```

### 5.4 Module phụ thuộc

- **Đầu vào:** Không phụ thuộc module nghiệp vụ nào.
- **Đầu ra cho module khác:** Đăng nhập thành công là **tiền điều kiện** để truy cập mọi module (Bán hàng, Hàng hóa, Khách hàng, Dashboard...).
- **Liên quan:** Module **Cài đặt** sẽ gọi `AuthService.changePassword()` (spec riêng).

---

## 6. Edge Cases & Risks

### 6.1 Edge Cases

| #   | Tình huống                                              | Xử lý                                                                |
| --- | ------------------------------------------------------- | --------------------------------------------------------------------- |
| E1  | App mở lần đầu, bảng `app_user` trống                  | Hiện form thiết lập mật khẩu (UC-01b)                                |
| E2  | Nhập mật khẩu toàn khoảng trắng                        | Coi là hợp lệ (yêu cầu chỉ là "khác rỗng"). Trim hay không? → **Không trim** — mật khẩu giữ nguyên. |
| E3  | Mật khẩu rất dài (vd: 1000 ký tự)                      | BCrypt tự giới hạn 72 bytes. Không cần validate độ dài tối đa.       |
| E4  | DB bị xóa/corrupt giữa chừng                           | App khởi động → `SchemaInitializer` tạo lại schema → coi như lần đầu (E1) |
| E5  | Bấm nút [Đăng nhập] nhiều lần liên tiếp (double-click) | Disable nút sau lần bấm đầu, enable lại khi xử lý xong              |
| E6  | Quên mật khẩu                                           | Không có cơ chế khôi phục. Phải xóa file DB và bắt đầu lại.         |
| E7  | Bảng `app_user` có nhiều hơn 1 bản ghi (dữ liệu lạ)   | Luôn lấy bản ghi đầu tiên (`LIMIT 1`). Không tạo thêm.              |

### 6.2 Risks & Mitigations

| Rủi ro                                          | Mức độ     | Giảm thiểu                                                          |
| ----------------------------------------------- | ---------- | ------------------------------------------------------------------- |
| Quên mật khẩu → mất toàn bộ dữ liệu            | Trung bình | Ghi chú rõ trong app/hướng dẫn. GĐ2 có thể thêm cơ chế khôi phục. |
| BCrypt chạy trên UI thread → đơ giao diện       | Cao        | Bắt buộc chạy trong `javafx.concurrent.Task` (background thread)    |
| File DB không mã hóa → ai copy được file thì đọc được | Thấp  | Chấp nhận ở MVP. GĐ2 có thể thêm mã hóa DB (SQLCipher).           |
| Brute-force mật khẩu (không giới hạn lần sai)   | Thấp       | App offline, chỉ chạy local → rủi ro thấp. Chấp nhận ở MVP.        |

---

## 7. Decision Log (Quyết định đã chốt trong spec này)

| #  | Quyết định                                              | Lý do                                                         |
| -- | ------------------------------------------------------- | ------------------------------------------------------------- |
| D1 | Một người dùng duy nhất, không phân quyền               | Nghiệp vụ 1 chủ cửa hàng, đơn giản hóa MVP                  |
| D2 | Mật khẩu hash bằng BCrypt                               | Chuẩn an toàn, có salt tích hợp, chống rainbow table          |
| D3 | Không auto-login, mỗi lần mở app đều nhập MK            | Bảo vệ dữ liệu nhạy cảm khi máy dùng chung                  |
| D4 | Không giới hạn số lần nhập sai                          | App offline, rủi ro brute-force thấp; đơn giản hóa MVP       |
| D5 | Quên MK = xóa DB, không có cơ chế khôi phục             | Offline không có email/OTP; tránh phức tạp hóa MVP            |
| D6 | Validation MK: chỉ cần khác rỗng                        | Đơn giản, phù hợp 1 người dùng tự quản lý                    |
| D7 | Thiết lập xong → quay về form đăng nhập (không tự vào)  | Thống nhất luồng: luôn qua form đăng nhập để vào app          |
| D8 | Controller kiêm ViewModel (không tách)                  | Màn hình đơn giản, theo quy tắc "lite" trong architecture.md  |
| D9 | Đổi mật khẩu thuộc spec Cài đặt (không nằm ở đây)      | Tách concern: đăng nhập vs. quản lý tài khoản                |
