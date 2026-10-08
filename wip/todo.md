# Todo

## 👉 Việc còn lại ở nhà (laptop) — release v0.6.1 lên điện thoại

Laptop là máy giữ khoá ký cũ (SHA-256 `facd3183…`). Chỉ APK ký bằng khoá này mới cài đè được lên app 0.4.4 trên điện thoại (giữ nguyên dữ liệu).

1. Lần đầu (nếu laptop chưa có): `sudo apt install -y gh && gh auth login`.
2. Chạy 1 lệnh:

   ```bash
   cd password-vault-android
   git checkout main
   git pull origin main     # để có scripts/release.sh
   scripts/release.sh
   ```

   Script tự: pull code → kiểm tra khoá ký (sai khoá thì dừng, báo lỗi) → build APK → tạo tag `v0.6.1` → tạo GitHub Release kèm APK. Hướng dẫn đầy đủ: `docs/dev-setup.md` mục "Publish GitHub Release".
3. Trên điện thoại: mở trang Releases (https://github.com/danhbuidcn/password-vault-android/releases) → tải APK `v0.6.1` → cài đè.
4. Mở app lần đầu:
   - Bản cũ đã có PIN → mở bằng PIN như cũ.
   - Chưa có PIN → nhập Master Password cũ 1 lần cuối → đặt PIN.
   - Vân tay/khuôn mặt: bật lại trong Settings.
5. Nên làm: copy `~/.android/debug.keystore` của laptop ra chỗ an toàn (và sang máy công ty) — mất file này là không cài đè được nữa.
6. Ghi chú: tag `v0.6.0` trên GitHub trỏ bản chưa có A/B bên dưới, không có Release — bỏ qua, dùng `v0.6.1`.

## 2026-10-08 — Export bằng vân tay/khuôn mặt + Quên PIN + script release — ✅ xong

### A. Export: xác thực bằng vân tay/khuôn mặt

- [x] Màn Export: nếu đã bật vân tay/khuôn mặt → tự hiện prompt sinh trắc học ngay khi mở. Thành công → chọn nơi lưu file.
- [x] Vẫn giữ ô nhập PIN làm dự phòng. Thêm nút "Dùng vân tay/khuôn mặt" để gọi lại prompt.
- [x] Code: `ExportViewModel.onBiometricVerified()`; `MainActivity` thêm prompt EXPORT; `ExportScreen` nhận cờ `hasBiometric`.

### B. Quên PIN → xác thực bằng khoá màn hình điện thoại (thay cho gửi mail)

- [x] Màn nhập PIN: nút "Quên PIN?" (chỉ hiện khi điện thoại có khoá màn hình).
- [x] Bấm → prompt hệ thống (mã/hình vẽ/vân tay của điện thoại). Thành công → mở Vault → bắt buộc đặt PIN mới.
- [x] Offline hoàn toàn, không thêm quyền Internet.

### D. Script release 1 lệnh cho laptop

- [x] `scripts/release.sh` (pull → kiểm tra khoá ký → build → tag → GitHub Release).
- [x] Cập nhật `docs/dev-setup.md` mục release trỏ tới script.

### C. Verify + ghi lại

- [x] Emulator: export bằng vân tay (prompt tự hiện → chọn nơi lưu); huỷ prompt → export bằng PIN; Quên PIN → khoá màn hình điện thoại → đặt PIN mới 9999 → PIN cũ báo "Wrong PIN", PIN mới mở được; máy không có khoá màn hình → nút "Quên PIN?" ẩn.
- [x] Build: ktlint + assembleDebug xanh; detekt/lint còn 2 lỗi cũ có sẵn trên `main` (`VaultItemFormScreen`, `SettingsScreen.LanguageSection`), ngoài phạm vi.
- [x] Script: kiểm tra cú pháp; chạy thử trên máy công ty → dừng đúng ở bước kiểm tra khoá ký (máy này không có khoá cũ).
- [x] Cập nhật tài liệu (functional-spec, flow, dev-setup).
- [x] Bump version 0.6.1 (versionCode 11) — tag `v0.6.0` đã trỏ commit cũ, chưa có A/B.
- [x] Commit + push.
