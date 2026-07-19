# Architecture

> ⚠️ Dự án chưa có code (giai đoạn viết tài liệu). Tài liệu này ghi lại **hướng kiến trúc đã chốt** với người
> phụ trách dự án (qua bộ câu hỏi tech stack ngày 2026-07-19) để bắt đầu code — không phải kiến trúc đọc ngược
> từ code có sẵn. Cập nhật lại phần "Architecture Decisions" nếu có thay đổi trong quá trình implement.

## Summary

- App Android single-module, kiến trúc MVVM + Repository, UI bằng Jetpack Compose.
- Toàn bộ dữ liệu Vault lưu trong 1 database SQLite mã hóa (SQLCipher) truy cập qua Room; không có backend, không gọi mạng.
- Khóa mã hóa DB dẫn xuất từ Master Password (KDF), khóa thật lưu qua Android Keystore — không lưu Master Password dưới mọi hình thức.

---

## Purpose

- Cho người dùng một nơi lưu mật khẩu an toàn, hoàn toàn offline, tùy biến sâu (custom field, tag, password generator). Xem chi tiết ở [overview.md](overview.md).

---

## Tech Stack

- **Ngôn ngữ:** Kotlin.
- **UI:** Jetpack Compose (Material 3).
- **Local database:** SQLCipher for Android + Room (`SupportFactory` mở DB bằng khóa dẫn xuất từ Master Password).
- **Bảo mật khóa:** Android Keystore (AndroidKeyStore provider), `BiometricPrompt` cho vân tay/khuôn mặt.
- **minSdk:** 26 (Android 8.0). `targetSdk`/`compileSdk`: bản mới nhất tại thời điểm implement (⚠️ chưa chốt số cụ thể).
- **Async:** Kotlin Coroutines + Flow (Room hỗ trợ Flow native, phù hợp danh sách Vault Item cập nhật realtime).
- **DI:** 👉 đề xuất Hilt (chuẩn cặp đôi với Compose/Room hiện tại) — chưa xác nhận với người phụ trách, có thể đổi.
- **Background job:** WorkManager (nhắc backup định kỳ).
- **Import/Export:** Storage Access Framework (SAF) cho chọn nơi lưu; thư viện đọc/ghi CSV + Excel (`.xlsx`) — 👉 chọn thư viện cụ thể (vd Apache POI cho xlsx) khi implement.
- **Backend / Cloud:** Không có — ứng dụng offline tuyệt đối (xem Project Constraints ở [overview.md](overview.md)).

---

## Project Structure

> Đề xuất cấu trúc theo package-by-layer, chuẩn MVVM cho app Compose 1 module. Điều chỉnh khi implement nếu cần package-by-feature.

- `data/` — Room entities/DAO, SQLCipher setup, repository implementation, import/export (CSV/Excel/`.pwvbackup`).
- `domain/` — model nghiệp vụ (Vault Item, Tag, Custom Field, Password Generator Template), use case nếu cần tách khỏi ViewModel.
- `ui/` — màn hình Compose theo feature (unlock, vault list, item detail, generator, settings/backup), theo Material 3 theming.
- `security/` — KDF, Keystore wrapper, Biometric, lockout/rate-limit logic, FLAG_SECURE setup.
- `di/` — Hilt module (nếu chọn Hilt).

---

## Architecture Layers

- **UI (Compose)** — hiển thị, nhận input, gọi ViewModel; không chứa logic mã hóa/business rule.
- **ViewModel** — state cho từng màn hình, gọi Repository, không biết chi tiết Room/SQLCipher.
- **Repository** — điểm truy cập dữ liệu duy nhất cho ViewModel; ẩn chi tiết Room DAO + import/export + backup.
- **Data (Room/SQLCipher)** — DAO, entity, mở/đóng DB bằng khóa dẫn xuất từ Master Password.
- **Security** — KDF (Master Password → khóa DB), Android Keystore, BiometricPrompt, lockout đếm số lần sai.

---

## Data Storage

| Storage | Purpose |
|---|---|
| SQLCipher DB (qua Room) | Vault Item (Login/Note), Tag, Custom Field, Password Generator Template — dữ liệu chính. |
| Android Keystore | Khóa mã hóa thật (dẫn xuất từ Master Password), không lưu plaintext. |
| App private storage | File DB, file backup tạm trước khi ghi ra ngoài qua SAF. |
| External storage (qua SAF) | File `.pwvbackup` và export CSV/Excel do người dùng chọn nơi lưu (SD card, USB OTG...). |

- Không dùng Google Auto Backup (`android:allowBackup=false`) — xem [overview.md](overview.md#project-constraints).

---

## Authentication

- Master Password: bắt buộc, thiết lập lần đầu, luôn là phương án mở khóa gốc.
- PIN số: fallback bắt buộc có trên mọi thiết bị.
- Sinh trắc học (vân tay/khuôn mặt): tùy chọn, qua `BiometricPrompt`.
- PIN/sinh trắc học chỉ mở khóa UI; khóa mã hóa DB thật luôn dẫn xuất từ Master Password, lưu qua Android Keystore.
- Giới hạn số lần nhập sai, tăng dần thời gian khóa khi sai liên tục (chống brute-force).
- Tự động khóa sau X phút không thao tác (X tùy chỉnh, có giá trị mặc định an toàn, có ngưỡng tối thiểu app quy định).

---

## Authorization

- Không áp dụng — app single-user, không có vai trò/phân quyền nhiều người dùng (ngoài scope, xem [overview.md](overview.md#out-of-scope)).

---

## External Services

- Không có — nguyên tắc offline tuyệt đối, không gửi dữ liệu qua mạng dưới bất kỳ hình thức nào.

---

## Deployment

- **Local development:** Android Studio, chạy/xem app trên **thiết bị Android vật lý qua USB** — `adb` (install/debug/logcat) + `scrcpy` (mirror màn hình lên Ubuntu). Chốt 2026-07-19: dùng thiết bị thật thay vì emulator vì cần test sinh trắc học/Keystore thật.
- **Production:** Phân phối bằng **APK side-load** (build release APK đã ký, cài trực tiếp qua `adb install` hoặc copy file vào máy). Không đăng Google Play Store.

---

## Architecture Decisions

- ✅ (2026-07-19) Kotlin + Jetpack Compose + SQLCipher/Room + minSdk 26 — chốt qua trao đổi trực tiếp với người phụ trách dự án.
- ✅ (2026-07-19) Xem/test app trên thiết bị Android vật lý qua USB (`adb` + `scrcpy`), không dùng emulator.
- ✅ (2026-07-19) Phân phối bằng APK side-load (single-user, không lên Play Store).
- 👉 DI framework (Hilt) và thư viện đọc/ghi Excel — đề xuất, chưa chốt chính thức, xác nhận khi bắt đầu implement.
