# Plan — Khởi tạo Android project scaffold (pwvault-android)

## Goal

- Dựng khung project Android (Gradle + Kotlin + Compose) đúng theo `architecture.md` đã chốt, build & chạy được trên thiết bị thật qua `adb`/`scrcpy` — **chưa** implement tính năng nghiệp vụ.
- Dọn môi trường dev trên máy Ubuntu này (hiện chưa có JDK, Android SDK, `adb`, `scrcpy`, và repo chưa init git).

---

## Scope

### In Scope

- Cài môi trường: JDK, Android SDK cmdline-tools, `adb`, `scrcpy` (qua `apt`/`sdkmanager`).
- `git init` cho repo `pwvault-android` (hiện chưa có git).
- Gradle project skeleton: `settings.gradle.kts`, root `build.gradle.kts`, `gradle/libs.versions.toml`, `app/build.gradle.kts`.
- Package skeleton theo đúng `architecture.md`: `data/`, `domain/`, `ui/`, `security/`, `di/`.
- 1 màn hình Compose placeholder ("Unlock" trống) + `MainActivity` — chỉ để verify build/run thật, không có logic.
- Tooling: `ktlint`, `detekt`, `.editorconfig` (theo `lang/kotlin.md`).
- Hilt DI cơ bản (module rỗng) — xem quyết định bên dưới.
- Cập nhật `docs/manifest.md` — thay lệnh Verify "dự kiến" bằng lệnh đã chạy thật.

### Out of Scope

- Mọi logic nghiệp vụ: Master Password/KDF, Keystore, Room schema thật, Vault Item CRUD, import/export, backup, password generator — mỗi cái sẽ có `/code-plan` riêng sau khi scaffold chạy được.
- Cấu hình ký release APK thật (chỉ scaffold build debug trước).
- CI/CD.
- Thư viện đọc/ghi Excel cụ thể — chọn khi implement tính năng import/export.

---

## Files

**Môi trường (không phải file trong repo):** JDK, Android SDK cmdline-tools, `adb`, `scrcpy` — cài qua `apt`/`sdkmanager`.

**Repo (mới, tạo trong `pwvault-android/`):**
- `.gitignore`
- `settings.gradle.kts`
- `build.gradle.kts`
- `gradle/libs.versions.toml`
- `gradle.properties`
- `app/build.gradle.kts`
- `app/src/main/AndroidManifest.xml`
- `app/src/main/java/com/pwvault/app/PwVaultApp.kt` (Application class, `@HiltAndroidApp`)
- `app/src/main/java/com/pwvault/app/MainActivity.kt`
- `app/src/main/java/com/pwvault/app/ui/theme/Theme.kt` (Material 3 skeleton)
- `app/src/main/java/com/pwvault/app/ui/unlock/UnlockScreen.kt` (placeholder, chưa có logic)
- `app/src/main/java/com/pwvault/app/di/` (package rỗng, sẵn chỗ cho Hilt module)
- `.editorconfig`
- `config/detekt/detekt.yml`

**Cập nhật:**
- `docs/manifest.md` — mục Verify.

---

## Implementation Steps

1. Cài JDK (bản AGP hiện tại yêu cầu) + `adb` + `scrcpy` qua `apt`; xác nhận `adb devices` thấy máy bạn khi cắm USB.
2. Cài Android SDK cmdline-tools (không cài Android Studio GUI — xem Decisions) qua `sdkmanager`, set `ANDROID_HOME`, chấp nhận license.
3. `git init` trong `pwvault-android/`, thêm `.gitignore` chuẩn Android/Gradle.
4. Tạo `settings.gradle.kts` + root `build.gradle.kts` + `gradle/libs.versions.toml` — pin phiên bản AGP/Kotlin/Compose BOM/Room/SQLCipher/Hilt tại thời điểm chạy thật (kiểm tra trang release chính thức lúc thực thi, không hard-code số phỏng đoán vào plan).
5. Tạo module `app/` với `build.gradle.kts`: `applicationId = "com.pwvault.app"`, minSdk 26, plugin Compose + Room (ksp) + SQLCipher + Hilt + ktlint + detekt.
6. Tạo package skeleton rỗng: `data/`, `domain/`, `ui/`, `security/`, `di/` dưới `com.pwvault.app`.
7. Viết `MainActivity` + 1 Compose screen placeholder ("Unlock — coming soon") + Material 3 theme tối thiểu + `PwVaultApp` (`@HiltAndroidApp`).
8. Set `android:allowBackup="false"` trong `AndroidManifest.xml` (constraint đã chốt trong `overview.md`).
9. `./gradlew ktlintCheck detekt lint assembleDebug` — sửa tới khi pass.
10. `./gradlew installDebug` lên thiết bị thật qua `adb`, mở `scrcpy` xác nhận app chạy, hiện đúng màn hình placeholder.
11. Cập nhật `docs/manifest.md` mục Verify bằng lệnh đã chạy thật (bỏ ⚠️ "dự kiến").

---

## Risks

- Máy Ubuntu hiện **chưa có JDK/Android SDK/adb/scrcpy** — bước 1–2 tốn thời gian tải (vài trăm MB–GB), có thể phát sinh lỗi cài đặt tùy Ubuntu 26.04.
- Phiên bản AGP/Kotlin/Compose BOM đổi nhanh — số cụ thể chốt tại lúc chạy thật (bước 4), không cam kết số trong plan này để tránh sai lệch.
- SQLCipher + Room tích hợp qua `SupportFactory` có thể phát sinh vấn đề tương thích phiên bản — nếu gặp, sẽ báo lại trước khi đổi hướng.

---

## Questions

*(Đã tự đề xuất phương án dựa trên thông tin hiện có — chỉ hỏi lại nếu bạn muốn đổi.)*

- **Package name/applicationId:** đề xuất `com.pwvault.app` — app cá nhân, không có domain/công ty, chỉ side-load (không lên Play Store) nên không quan trọng chuyện trùng applicationId. 👉 Dùng giá trị này trừ khi bạn muốn tên khác.
- **DI framework:** dùng **Hilt** ngay từ scaffold — khớp với đề xuất đã có sẵn trong `architecture.md` và tinh thần "theo phương án khuyến nghị" bạn đã chọn ở các câu hỏi trước. 👉 Đã đưa vào Files/Steps ở trên.
- **Cài Android SDK:** dùng **cmdline-tools thuần**, không cài Android Studio GUI — khớp với workflow CLI + `scrcpy` bạn đã chọn thay vì emulator. 👉 Đã đưa vào Steps ở trên.
