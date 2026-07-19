# pwvault-android

Ứng dụng Android quản lý mật khẩu cá nhân, chạy hoàn toàn offline. Chi tiết nghiệp vụ/kiến trúc xem [docs/overview.md](docs/overview.md) và [docs/architecture.md](docs/architecture.md).

## Yêu cầu môi trường

- JDK 21
- Android SDK (platform 37.1, build-tools 37.0.0, platform-tools)
- Thiết bị Android thật (minSdk 26+) nối qua USB — dự án này không dùng emulator
- [`scrcpy`](https://github.com/Genymobile/scrcpy) để xem màn hình thiết bị trên máy Ubuntu khi dev (`sudo apt install scrcpy`)

Set biến môi trường (đổi đường dẫn nếu bạn cài JDK/SDK ở chỗ khác):

```bash
export JAVA_HOME="$HOME/.local/opt/jdk-21.0.11+10"
export ANDROID_HOME="$HOME/Android/Sdk"
export PATH="$JAVA_HOME/bin:$ANDROID_HOME/platform-tools:$ANDROID_HOME/cmdline-tools/latest/bin:$PATH"
```

(Có thể thêm 3 dòng trên vào `~/.bashrc` để không phải export lại mỗi terminal mới.)

## Build & chạy trên thiết bị thật

1. **Bật Developer options + USB debugging trên điện thoại:**
   Settings → About phone → bấm liên tiếp 7 lần vào **Build number** để mở khóa Developer options → vào Settings → System → Developer options → bật **USB debugging**.

   Trên Samsung (One UI), **Build number** nằm trong mục con: Settings → About phone → **Software information** → Build number.

2. **Cắm điện thoại vào máy qua cáp USB.**

3. **Kiểm tra máy tính nhận thiết bị:**

   ```bash
   adb devices
   ```

   - Thấy thiết bị ở trạng thái `device` → OK, qua bước tiếp theo.
   - Thấy `unauthorized` → mở khóa màn hình điện thoại, xác nhận popup **"Allow USB debugging"** rồi chạy lại lệnh trên.
   - Không thấy thiết bị nào → thử đổi cáp/cổng USB, hoặc đổi chế độ USB trên điện thoại sang **File Transfer/MTP** (kéo thanh thông báo USB xuống để đổi).

4. **Build, cài và chạy app:**

   ```bash
   ./gradlew installDebug
   adb shell am start -n com.pwvault.app/.MainActivity
   ```

   Lần build đầu có thể mất vài phút để tải dependency. Từ lần sau, nếu chỉ sửa code, chạy lại `./gradlew installDebug` là đủ.

5. **Xem màn hình điện thoại ngay trên máy tính (tùy chọn, cần cài `scrcpy` ở bước trên):**

   ```bash
   scrcpy
   ```

   Đóng cửa sổ `scrcpy` không ảnh hưởng tới app đang chạy trên điện thoại.

## Verify trước khi commit

```bash
./gradlew ktlintCheck detekt lint testDebugUnitTest assembleDebug
```

Rule/code style áp dụng cho project này nằm ở kho trung tâm — xem [docs/manifest.md](docs/manifest.md) (load map) và `AI/kit/context/lang/kotlin.md` + `AI/kit/context/security.md`.

## Build file cài đặt (APK) cho người dùng cuối

App này chỉ dùng cho 1 người (side-load, không đăng Play Store).

⚠️ `assembleRelease` chưa có `signingConfig` (ngoài scope của plan scaffold ban đầu) — APK release hiện ra dạng unsigned, chưa cài trực tiếp được. Tạm dùng bản **debug-signed** làm file cài:

```bash
./gradlew assembleDebug
mkdir -p dist
cp app/build/outputs/apk/debug/app-debug.apk "dist/pwvault-android-$(grep -oP '(?<=versionName = ")[^\"]+' app/build.gradle.kts)-$(date +%Y%m%d)-debug.apk"
```

Cài trực tiếp qua USB:

```bash
adb install -r dist/pwvault-android-<version>-<yyyyMMdd>-debug.apk
```

Hoặc copy file `.apk` vào điện thoại (qua cáp/`adb push`) rồi mở bằng File Manager để cài (cần bật "Cài ứng dụng từ nguồn không xác định" cho app quản lý file dùng để mở APK).

Ký release thật (keystore riêng, không debug-signed) là việc của một plan sau, chưa làm ở bước scaffold này.

## Cấu trúc project

Xem [docs/architecture.md](docs/architecture.md#project-structure).

## Tài liệu dự án

- [docs/overview.md](docs/overview.md) — tổng quan nghiệp vụ
- [docs/functional-spec.md](docs/functional-spec.md) — spec nguồn
- [docs/architecture.md](docs/architecture.md) — kiến trúc & quyết định kỹ thuật
- [docs/flow.md](docs/flow.md) — cơ chế hoạt động: vòng đời app, luồng mở khóa/khóa, luồng dữ liệu (CRUD, backup, import)
- [docs/glossary.md](docs/glossary.md) — thuật ngữ
- [docs/manifest.md](docs/manifest.md) — stack + rule load map cho `/code-plan`, `/code-guard`
- [docs/plans/](docs/plans/) — implementation plan theo từng task
