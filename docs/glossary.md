# Glossary

## Summary

- Tài liệu giải thích các thuật ngữ nghiệp vụ và kỹ thuật riêng của pwvault-android — ứng dụng Android quản lý mật khẩu offline.
- Dùng khi đọc overview.md, functional-spec.md hoặc bất kỳ tài liệu/feature nào nhắc tới các khái niệm dưới đây mà chưa rõ nghĩa.

---

## Business Terms

### Vault

- File dữ liệu mã hóa chính (AES-256) chứa toàn bộ Vault Item của người dùng.
- Chỉ app pwvault-android đọc được; không có server hay bản sao ở đâu khác trừ khi người dùng tự export.

### Master Password

- Mật khẩu gốc do người dùng thiết lập khi cài app lần đầu; là phương án mở khóa gốc, luôn tồn tại kể cả khi đã bật PIN/sinh trắc học.
- Không bao giờ được lưu dưới bất kỳ hình thức nào (kể cả mã hóa); khóa mã hóa Vault được sinh từ Master Password qua KDF (PBKDF2/Argon2).
- Mất Master Password = mất toàn bộ dữ liệu, không có cơ chế khôi phục (mô hình zero-knowledge, không có backdoor).

### Vault Item

- Đơn vị dữ liệu cơ bản người dùng lưu trong Vault. Có 2 loại: **Login** (tài khoản có username/password/URL) và **Note** (ghi chú bảo mật không gắn tài khoản).
- Mỗi Vault Item có: tên, ghi chú, danh sách Tag, danh sách Custom Field, thời gian tạo/cập nhật gần nhất.

### Tag

- Nhãn dùng để phân loại/sắp xếp Vault Item; một Vault Item có thể gắn nhiều Tag.
- App có sẵn tag gợi ý (Personal, Bank, Social Media...); người dùng toàn quyền thêm/sửa/xóa tag riêng — danh sách tag là danh sách mở, không giới hạn số lượng trên 1 Vault Item.

### Custom Field

- Trường dữ liệu tùy biến người dùng tự thêm vào một Vault Item, ngoài các trường mặc định (username, password, URL, ghi chú).
- Free-form (không có kiểu cố định, ví dụ: security question, mã PIN thẻ, backup code), nhưng được mã hóa cùng cấp với các trường mặc định.

### Password Generator Template

- Bộ quy tắc sinh mật khẩu ngẫu nhiên (độ dài, bộ ký tự, loại trừ ký tự dễ nhầm) mà người dùng lưu lại để tái dùng nhiều lần.

### Backup file (`.pwvbackup`)

- File mã hóa (AES-256, khóa từ Master Password) dùng để backup hoặc chuyển máy; chỉ app pwvault-android đọc được.
- Khôi phục từ file này chỉ cần Master Password, không cần map cột như import CSV/Excel.

---

## Abbreviations

### KDF

- Key Derivation Function — hàm sinh khóa mã hóa từ Master Password (PBKDF2 hoặc Argon2).

### SAF

- Storage Access Framework — API Android dùng để người dùng tự chọn nơi lưu file backup (SD card, USB OTG, bộ nhớ ngoài) mà không mặc định ghi lên cloud.

---

## Technical Terms

### Zero-knowledge (mô hình lưu trữ)

- Mô hình trong đó chỉ người dùng nắm được thông tin để giải mã dữ liệu của chính họ (Master Password); nhà phát triển/ứng dụng không có cách nào truy cập hoặc khôi phục thay.

### Android Keystore

- Kho lưu khóa bảo mật của hệ điều hành Android; app dùng để lưu khóa mã hóa thật (dẫn xuất từ Master Password), không lưu plaintext. PIN/sinh trắc học chỉ mở khóa giao diện, không thay thế khóa mã hóa thật.

### FLAG_SECURE

- Cờ Android dùng để chặn chụp/quay màn hình ứng dụng, áp dụng cho toàn bộ app pwvault-android nhằm chống lộ dữ liệu qua screenshot.

### Backup rotation

- Cơ chế giữ tối đa K bản backup gần nhất, tự động loại bỏ bản cũ hơn để tránh ghi đè mất bản backup còn giá trị.

---

## Related Documents

- [overview.md](overview.md)
- [functional-spec.md](functional-spec.md)
- architecture.md — chưa có, chờ chốt tech stack.
