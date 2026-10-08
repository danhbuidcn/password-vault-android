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

- Mở khóa app bằng PIN số (mật khẩu duy nhất), hoặc sinh trắc học (vân tay/khuôn mặt) — Feature 19 bỏ Master Password.
- Thêm/sửa/xóa/tìm kiếm/phân loại (tag) Vault Item.
- Hiển thị thời gian cập nhật gần nhất (last update) trên mỗi Vault Item.
- Một ứng dụng/dịch vụ có thể có nhiều Vault Item (nhiều tài khoản), không giới hạn số lượng.
- Vault Item loại Login (username/password/URL); loại Note (ghi chú bảo mật không gắn tài khoản) vẫn xem/sửa được nếu đã có từ trước, nhưng màn Thêm mục chỉ tạo mới được Login.
- Phân loại Vault Item bằng tag: app có sẵn tag gợi ý (Personal, Bank, Social Media...), người dùng tự tạo/sửa/xóa tag riêng, một Vault Item có thể gắn nhiều tag.
- Trường dữ liệu tùy biến (Custom Field): mục đã có từ trước vẫn hiển thị nguyên vẹn, nhưng màn Thêm/sửa không còn cho thêm trường mới.
- Sinh mật khẩu ngẫu nhiên, tùy chỉnh độ dài (tối đa 15 ký tự)/bộ ký tự.
- Tùy chỉnh bảo mật & mở khóa: thời gian tự khóa, bật/tắt từng phương thức mở khóa, độ mạnh tham số mã hóa — trong giới hạn an toàn tối thiểu do app quy định.
- Tùy chỉnh giao diện & tổ chức dữ liệu: theme, tag/nhóm/icon tự đặt, cách sắp xếp/hiển thị danh sách.
- Import mật khẩu từ file CSV/Excel.
- Export: file CSV thường (không mã hóa), import lại được vào app trên máy khác; nhắc export định kỳ 30 ngày. Không còn auto-backup và `.pwvbackup` (Feature 19).
- Cảnh báo mật khẩu yếu/trùng lặp, chặn chụp màn hình.

---

## Business Domain

- Quản lý mật khẩu cá nhân (personal password manager), mô hình lưu trữ offline / zero-knowledge.
- Khái niệm cốt lõi: Vault (kho dữ liệu), Master Password, Vault Item (2 loại: Login — tài khoản có username/password, Note — ghi chú bảo mật không có username/password), Tag.

---

## Core Business Rules

- PIN là mật khẩu duy nhất; Vault key ngẫu nhiên, bọc bằng Android Keystore, mở bằng PIN hoặc sinh trắc học (Feature 19).
- Mất máy / reset máy / xóa app = mất dữ liệu, chỉ còn bản CSV đã export.
- Export là CSV không mã hóa, có cảnh báo; yêu cầu nhập lại PIN trước khi export.
- Giới hạn số lần nhập sai PIN, tăng dần thời gian khóa khi nhập sai.
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
- Mở khóa bằng PIN số, sinh trắc học (Feature 19 bỏ Master Password).

### Out of Scope

- Đồng bộ tự động qua internet/cloud.
- Chia sẻ mật khẩu giữa nhiều người dùng.
- Nền tảng khác ngoài Android (iOS, desktop) — chỉ làm app Android.

---

## Main Workflow

- Cài đặt lần đầu → đặt PIN → (tùy chọn) bật sinh trắc học trong Settings.
- Mở app → mở khóa (PIN / sinh trắc học) → xem/thêm/sửa/xóa Vault Item (Login hoặc Note).
- Backup định kỳ/theo nhắc nhở → export CSV ra bộ nhớ ngoài.
- Đổi máy → cài app trên máy mới → đặt PIN → import file CSV.
- Cần chuyển/xem dữ liệu dạng bảng → import/export CSV/Excel (export loại này luôn ở dạng đọc được, có cảnh báo).

---

## Key Entities

- Vault Item: loại (Login hoặc Note), tên, ghi chú, danh sách Tag, danh sách Custom Field, thời gian tạo/cập nhật gần nhất. Login có thêm username/password/URL; Note có thêm nội dung.
- Tag: nhãn phân loại Vault Item, có tag gợi ý sẵn (Personal, Bank, Social Media...) và tag tự tạo.
- Custom Field: trường tùy biến gắn với 1 Vault Item (key, value, loại hiển thị).
- Vault: file dữ liệu mã hóa chính (AES-256).
- ~~Backup file (`.pwvbackup`)~~: đã bỏ ở Feature 19, thay bằng export CSV.
- ~~Master Password~~: đã bỏ ở Feature 19, PIN là mật khẩu duy nhất.

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
