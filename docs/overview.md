# Overview

## Summary

- pwvault-android là ứng dụng Android quản lý mật khẩu cá nhân, hoạt động hoàn toàn offline.
- Mật khẩu được lưu trong một file mã hóa (AES-256) ngay trên máy, chỉ app này đọc được.
- Không có server hay đồng bộ cloud tự động; backup/chuyển máy thực hiện bằng cách xuất một file mã hóa riêng của app.
- Dự án đang trong giai đoạn implement theo [roadmap.md](plans/roadmap.md): scaffold + Master Password setup/unlock (Feature 0, 1) đã xong, các tính năng còn lại (PIN, sinh trắc học, auto-lock/lockout, CRUD Vault Item, import/export, auto-backup...) đang chờ triển khai.

---

## Purpose

- Cho người dùng một nơi lưu mật khẩu an toàn, không phụ thuộc dịch vụ cloud bên thứ ba, giảm rủi ro rò rỉ dữ liệu qua internet.
- Cho phép tùy biến sâu theo nhu cầu từng người dùng: trường dữ liệu, bảo mật/mở khóa, sinh mật khẩu, giao diện & tổ chức dữ liệu.

---

## Target Users

- Cá nhân tự quản lý mật khẩu của mình, ưu tiên riêng tư/offline hơn tiện lợi đồng bộ nhiều thiết bị tự động.

---

## Main Features

- Mở khóa app bằng Master Password, PIN số, hoặc sinh trắc học (vân tay/khuôn mặt).
- Thêm/sửa/xóa/tìm kiếm/phân loại (tag) Vault Item.
- Hiển thị thời gian cập nhật gần nhất (last update) trên mỗi Vault Item.
- Một ứng dụng/dịch vụ có thể có nhiều Vault Item (nhiều tài khoản), không giới hạn số lượng.
- Ngoài Vault Item loại Login (username/password/URL), hỗ trợ loại Note — ghi chú bảo mật không gắn với tài khoản (VD: mã khóa két, ghi chú riêng tư).
- Phân loại Vault Item bằng tag: app có sẵn tag gợi ý (Personal, Bank, Social Media...), người dùng tự tạo/sửa/xóa tag riêng, một Vault Item có thể gắn nhiều tag.
- Trường dữ liệu tùy biến: ngoài các trường mặc định (username, password, URL, ghi chú), người dùng tự thêm trường tùy ý cho từng mục (VD: security question, mã PIN thẻ, backup code...).
- Sinh mật khẩu ngẫu nhiên, tùy chỉnh độ dài/bộ ký tự/loại trừ ký tự dễ nhầm, lưu lại thành nhiều mẫu (template) để tái dùng.
- Tùy chỉnh bảo mật & mở khóa: thời gian tự khóa, bật/tắt từng phương thức mở khóa, độ mạnh tham số mã hóa — trong giới hạn an toàn tối thiểu do app quy định.
- Tùy chỉnh giao diện & tổ chức dữ liệu: theme, tag/nhóm/icon tự đặt, cách sắp xếp/hiển thị danh sách.
- Import mật khẩu từ file CSV/Excel.
- Export: mỗi lần xuất, người dùng chọn 1 trong 2 hình thức — mã hóa (`.pwvbackup`, chỉ app đọc được, dùng cho backup/chuyển máy) hoặc không mã hóa (CSV/Excel, mở/xem bình thường bằng ứng dụng khác).
- Auto-backup tự động chạy nền mỗi khi thêm/sửa/xóa Vault Item (ghi `.pwvbackup` vào thư mục người dùng chọn qua SAF, không cần thao tác thủ công), giữ tối đa 5 bản gần nhất (rotate); ngoài ra vẫn nhắc export thủ công định kỳ.
- Cảnh báo mật khẩu yếu/trùng lặp, chặn chụp màn hình.

---

## Business Domain

- Quản lý mật khẩu cá nhân (personal password manager), mô hình lưu trữ offline / zero-knowledge.
- Khái niệm cốt lõi: Vault (kho dữ liệu), Master Password, Vault Item (2 loại: Login — tài khoản có username/password, Note — ghi chú bảo mật không có username/password), Tag.

---

## Core Business Rules

- Master Password không bao giờ được lưu dưới bất kỳ hình thức nào; khóa mã hóa sinh từ Master Password qua KDF.
- Mất Master Password = mất toàn bộ dữ liệu, không có cơ chế khôi phục (zero-knowledge, không có backdoor).
- PIN/sinh trắc học chỉ mở khóa giao diện, không thay thế khóa mã hóa thực sự (vẫn dựa trên Master Password, lưu qua Android Keystore).
- Người dùng tự chọn kiểu export mỗi lần: mã hóa (`.pwvbackup`, chỉ app đọc được) hoặc không mã hóa (CSV/Excel, mở xem bình thường).
- Export không mã hóa hiển thị cảnh báo trước khi xuất (dữ liệu ở dạng đọc được).
- Mọi thao tác export **thủ công** đều yêu cầu xác thực lại bằng Master Password; Auto-backup chạy nền là ngoại lệ — dùng Vault Key đã có sẵn trong session, không hỏi lại.
- Giới hạn số lần nhập sai Master Password/PIN, tăng dần thời gian khóa khi nhập sai.
- Mọi tùy chỉnh bảo mật (thời gian tự khóa, tham số mã hóa...) đều có giá trị mặc định an toàn; không cho đặt dưới ngưỡng tối thiểu app quy định.
- Trường tùy biến do người dùng tự thêm không có kiểu cố định (free-form), nhưng được mã hóa cùng cấp với các trường mặc định.
- Vault Item loại Note không có username/password, chỉ có tiêu đề + nội dung + tag/custom field như các loại khác.
- Tag là danh sách mở: app có sẵn một số tag gợi ý ban đầu, người dùng toàn quyền thêm/sửa/xóa; không giới hạn số tag trên 1 Vault Item.

---

## Project Scope

### In Scope

- Lưu trữ, quản lý mật khẩu offline trên thiết bị Android.
- Import/export CSV, Excel.
- Backup/khôi phục qua file mã hóa riêng của app.
- Mở khóa bằng Master Password, PIN số, sinh trắc học.

### Out of Scope

- Đồng bộ tự động qua internet/cloud.
- Chia sẻ mật khẩu giữa nhiều người dùng.
- Nền tảng khác ngoài Android (iOS, desktop) — chỉ làm app Android.

---

## Main Workflow

- Cài đặt lần đầu → thiết lập Master Password → (tùy chọn) bật PIN/sinh trắc học.
- Mở app → mở khóa (Master Password / PIN / sinh trắc học) → xem/thêm/sửa/xóa Vault Item (Login hoặc Note).
- Auto-backup tự động ghi `.pwvbackup` vào thư mục đã chọn mỗi khi có thay đổi Vault Item (không cần thao tác); ngoài ra có thể backup định kỳ/theo nhắc nhở → xuất file `.pwvbackup` ra bộ nhớ ngoài (SD/USB).
- Đổi máy → cài app trên máy mới → import file `.pwvbackup` bằng Master Password để khôi phục.
- Cần chuyển/xem dữ liệu dạng bảng → import/export CSV/Excel (export loại này luôn ở dạng đọc được, có cảnh báo).

---

## Key Entities

- Vault Item: loại (Login hoặc Note), tên, ghi chú, danh sách Tag, danh sách Custom Field, thời gian tạo/cập nhật gần nhất. Login có thêm username/password/URL; Note có thêm nội dung.
- Tag: nhãn phân loại Vault Item, có tag gợi ý sẵn (Personal, Bank, Social Media...) và tag tự tạo.
- Custom Field: trường tùy biến gắn với 1 Vault Item (key, value, loại hiển thị).
- Password Generator Template: bộ quy tắc sinh mật khẩu người dùng lưu lại để tái dùng.
- Vault: file dữ liệu mã hóa chính (AES-256).
- Backup file (`.pwvbackup`): file mã hóa dùng cho backup và chuyển máy.
- Master Password.

---

## Project Constraints

- Không gửi dữ liệu qua mạng dưới bất kỳ hình thức nào (nguyên tắc offline tuyệt đối).
- Không dùng Google Auto Backup cho dữ liệu app (`android:allowBackup=false`).

---

## Related Documents

- [architecture.md](architecture.md) — Kotlin + Jetpack Compose + Room/SQLCipher, minSdk 26 (chốt 2026-07-19).
- [flow.md](flow.md) — cơ chế hoạt động: vòng đời app, luồng mở khóa/khóa, luồng dữ liệu.
- [glossary.md](glossary.md)
- [manifest.md](manifest.md) — stack + load map cho `/code-plan`, `/code-guard`.
- [functional-spec.md](functional-spec.md) — tài liệu nguồn.
