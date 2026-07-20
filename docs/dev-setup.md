# Hướng dẫn setup môi trường dev

## Yêu cầu

- JDK 21
- Android SDK (platform 37.1, build-tools 37.0.0, platform-tools)
- Thiết bị Android thật (minSdk 26+) nối qua USB — dự án này không dùng emulator làm target chính
- [`scrcpy`](https://github.com/Genymobile/scrcpy) để xem màn hình thiết bị trên máy Ubuntu khi dev (`sudo apt install scrcpy`)

Set biến môi trường (đổi đường dẫn nếu bạn cài JDK/SDK ở chỗ khác):

```bash
export JAVA_HOME="$HOME/.local/opt/jdk-21.0.11+10"
export ANDROID_HOME="$HOME/Android/Sdk"
export PATH="$JAVA_HOME/bin:$ANDROID_HOME/platform-tools:$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/emulator:$PATH"
```

(Có thể thêm 3 dòng trên vào `~/.bashrc` để không phải export lại mỗi terminal mới.)

## Chạy trên thiết bị thật

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

## Chạy khi không có thiết bị thật (dùng Android Emulator)

1. **Cài Android Emulator qua `sdkmanager`** (dùng `ANDROID_HOME` đã export ở trên):

   ```bash
   sdkmanager --install "platform-tools" "emulator" "system-images;android-34;google_apis;x86_64"
   ```

2. **Kiểm tra máy có hỗ trợ ảo hóa phần cứng (KVM) — bắt buộc để emulator chạy đủ nhanh:**

   ```bash
   sudo apt install cpu-checker
   kvm-ok
   ```

   Nếu báo lỗi không dùng được KVM, cần bật ảo hóa (VT-x/AMD-V) trong BIOS, hoặc thêm user vào group `kvm`:

   ```bash
   sudo usermod -aG kvm "$USER"
   ```

   (Cần đăng xuất/đăng nhập lại để group có hiệu lực.)

3. **Tạo AVD (Android Virtual Device):**

   ```bash
   avdmanager create avd -n pwvault-test -k "system-images;android-34;google_apis;x86_64" -d pixel_6
   ```

   Nếu thấy in ra 2 dòng `Error: Could not load devices from .../devices.xml` — đây là lỗi vô hại đã biết của `avdmanager` (nó dò tìm `devices.xml` sai chỗ rồi tự fallback về danh sách device tích hợp sẵn). AVD vẫn được tạo thành công (kiểm tra bằng `ls ~/.android/avd/`), không cần xử lý gì thêm.

4. **Khởi động emulator:**

   ```bash
   emulator -avd pwvault-test -no-snapshot-save -gpu host
   ```

   Đợi tới khi màn hình home của Android hiện lên (lần đầu có thể mất 1-2 phút để boot). `-no-snapshot-save` tránh lưu lại state hỏng nếu emulator bị treo/force-quit (xem lỗi "Emulator Is Not Responding" bên dưới); `-gpu host` giảm tải render bằng GPU máy host thay vì phần mềm.

   **Nếu gặp "Emulator Is Not Responding":** `adb devices` vẫn thường thấy máy ảo ở trạng thái `device` — máy ảo Android bên trong không hề đơ, chỉ cửa sổ render bị treo (thường do máy host thiếu RAM/đang swap khi chạy đồng thời Gradle build + emulator + scrcpy + trình duyệt). Kiểm tra bằng `free -h` — nếu swap đang dùng nhiều, đóng bớt ứng dụng hoặc đợi build xong rồi mới thao tác trên emulator. Có thể bấm "Wait" thay vì "Force Quit" vì adb vẫn thường phản hồi được.

5. **Kiểm tra `adb` đã nhận emulator:**

   ```bash
   adb devices
   ```

   Sẽ thấy một dòng dạng `emulator-5554   device`.

6. **Build, cài và chạy app — giống hệt lệnh dùng cho thiết bị thật:**

   ```bash
   ./gradlew installDebug
   adb shell am start -n com.pwvault.app/.MainActivity
   ```

   Nếu vừa cắm thiết bị thật vừa chạy emulator cùng lúc, `adb` sẽ báo lỗi "more than one device/emulator" — dùng `adb -s emulator-5554 install ...` hoặc `adb -s <device-id> install ...` để chỉ định rõ target (lấy id từ `adb devices`).

## Build file cài đặt (APK) cho người dùng cuối

App này chỉ dùng cho 1 người (side-load, không đăng Play Store).

⚠️ `assembleRelease` chưa có `signingConfig` (ngoài scope của plan scaffold ban đầu) — APK release hiện ra dạng unsigned, chưa cài trực tiếp được. Tạm dùng bản **debug-signed** làm file cài; khác biệt duy nhất còn lại so với bản release thật là **chữ ký** (ký release thật là việc của một plan sau). Chặn chụp màn hình (`FLAG_SECURE`) mặc định **bật** trên mọi build kể cả debug (`BuildConfig.ENABLE_SCREENSHOT_BLOCK`), không còn tắt riêng theo `BuildConfig.DEBUG` như trước.

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

**Chụp ảnh màn hình app để làm docs/README:** vì `FLAG_SECURE` mặc định bật ngay cả trên build debug, `adb shell screencap`/`scrcpy` sẽ chỉ chụp được màn hình đen trừ khi tắt tạm bằng cờ Gradle:

```bash
./gradlew installDebug -PenableScreenshotBlock=false
```

Cài lại bằng `./gradlew installDebug` (không có cờ) để bật lại chặn chụp màn hình trước khi quay lại dùng/test bình thường.

## Publish GitHub Release

Khi app đã hoàn thành (hết roadmap hoặc tới mốc muốn phát hành), đăng file APK lên GitHub Releases để người dùng tải về cài trực tiếp — không commit file `.apk` vào git (đã bị chặn qua `.gitignore`).

1. **Cài & đăng nhập GitHub CLI (một lần duy nhất trên máy dev):**

   ```bash
   sudo apt install -y gh
   gh auth login
   ```

2. **Build APK cài đặt** theo hướng dẫn ở mục [Build file cài đặt (APK)](#build-file-cài-đặt-apk-cho-người-dùng-cuối) phía trên, ra file dạng `dist/pwvault-android-<version>-<yyyyMMdd>-debug.apk`.

3. **Đảm bảo mọi commit liên quan đã push lên `origin/main`** (`git push`) — tag release sẽ trỏ vào commit hiện tại.

4. **Tạo tag + release, đính kèm file APK:**

   ```bash
   VERSION=$(grep -oP '(?<=versionName = ")[^"]+' app/build.gradle.kts)
   DATE=$(date +%Y-%m-%d)
   APK="dist/pwvault-android-${VERSION}-$(date +%Y%m%d)-debug.apk"

   gh release create "v${VERSION}" "$APK" \
     --title "pwvault-android v${VERSION}" \
     --notes "Version: ${VERSION}
   Ngày build: ${DATE}
   Yêu cầu: Android 8.0 (API 26) trở lên
   Cài đặt: tải file .apk bên dưới → mở bằng File Manager trên điện thoại → bật \"Cài ứng dụng từ nguồn không xác định\" nếu được hỏi → Install."
   ```

   Đổi `--notes` cho phù hợp mỗi lần release (đổi log, breaking change nếu có). Dùng `gh release create "v${VERSION}" "$APK" --prerelease ...` nếu là bản chưa hoàn thiện (WIP/alpha).

5. **Kiểm tra lại:** mở `https://github.com/danhbuidcn/password-vault-android/releases`, tải file `.apk` vừa đăng, cài thử trên điện thoại thật để chắc chắn "download xong chạy được luôn".

Từ lần release sau, chỉ cần lặp lại bước 2–4 với version mới (nhớ bump `versionName` trong `app/build.gradle.kts` trước khi build).
