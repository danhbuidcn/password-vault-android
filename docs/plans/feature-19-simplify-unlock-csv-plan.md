# Plan — Feature 19: Đơn giản hoá mở khoá (PIN + sinh trắc học) và backup chỉ bằng CSV

> Trạng thái: ✅ **Hoàn tất** (duyệt 2026-10-08)

## Input

Yêu cầu user (2026-10-08):

- Bỏ Master Password. Chỉ 3 cách mở khoá: PIN số, vân tay, khuôn mặt. Không cần nhớ mật khẩu nào khác.
- Tắt auto-backup.
- Export/Import chỉ dùng file CSV, không mã hoá. File export import lại được vào app (máy khác).

User xác nhận thêm (2026-10-08): "vào app mặc định đặt 1 mật khẩu, user chỉ cần nhớ 1 mật khẩu" → mật khẩu đó chính là PIN số đặt lúc setup.

Đã chốt: hỗ trợ cả khuôn mặt loại "yếu" (`BIOMETRIC_WEAK`) → sinh trắc học chỉ là cổng xác nhận, không gắn với khoá Keystore.

## Output

- Setup lần đầu: tạo PIN (≥4 số, nhập 2 lần). Không còn màn Master Password.
- Mở khoá: PIN, hoặc vân tay/khuôn mặt nếu đã bật trong Settings.
- Settings: không còn mục Auto-backup. PIN luôn bật; sinh trắc học bật/tắt tuỳ chọn.
- Export: nhập PIN → chọn nơi lưu → ghi `.csv` thường.
- Import: chọn `.csv` export từ app → tự map cột → import.

## Context

- Hiện tại: Vault key = Argon2id(Master Password). PIN và biometric chỉ bọc lại Vault key đó qua Keystore (`PinManager`, `BiometricUnlockManager`).
- `PinKeystoreKeyProvider` dùng key Keystore **không** yêu cầu xác thực → sau khi biometric (weak) thành công, app tự giải được Vault key từ bản bọc của PIN. Không cần lưu thêm bản bọc riêng cho biometric.
- Vault key mới: 32 byte ngẫu nhiên, sinh lúc setup.
- User cũ (đã có Vault):
  - Đã có PIN → mở bằng PIN như cũ, không mất dữ liệu (PIN bọc đúng Vault key hiện tại).
  - Chưa có PIN → hiện màn Master Password **một lần**, mở xong bắt buộc tạo PIN. Sau đó không còn màn Master Password.
  - Đang bật biometric kiểu cũ (STRONG) → phải bật lại biometric 1 lần trong Settings.
- **Rủi ro cần biết**: không còn Master Password nên nếu mất máy/reset máy/xoá app, dữ liệu chỉ còn trong file CSV đã export. File CSV không mã hoá — ai có file là đọc được hết.

## Plan

### A. Mở khoá chỉ bằng PIN + sinh trắc học

- [x] `UnlockViewModel`: `createVault(pin, confirm)` — validate PIN (khớp, ≥4, chỉ số) → sinh Vault key ngẫu nhiên → `createVault` → `pinManager.setupPin`.
- [x] `SetupScreen`: đổi 2 ô mật khẩu thành 2 ô PIN (bàn phím số). Bỏ nút "Restore from backup".
- [x] `PinManager`: thêm `unwrapVaultKey()` — giải Vault key không cần PIN, chỉ gọi sau khi biometric thành công.
- [x] Biometric: `BiometricPrompt` dùng `BIOMETRIC_WEAK`, không `CryptoObject`. Cờ bật/tắt lưu trong SharedPreferences. Xoá `BiometricCredentialStore`, `BiometricKeystoreKeyProvider`; đơn giản hoá `BiometricUnlockManager`.
- [x] Settings: PIN không tắt được. Bỏ ràng buộc "ít nhất 1 trong 2".
- [x] Màn PIN/biometric: bỏ nút "Dùng Master Password".
- [x] Legacy: Vault có sẵn mà chưa có PIN → giữ màn Master Password (`UnlockScreen`) chỉ cho trường hợp này → mở xong ép tạo PIN.
- [x] Bỏ `verifyMasterPassword`; thay bằng `verifyPin` cho bước xác nhận khi export.

### B. Tắt auto-backup

- [x] Xoá `AutoBackupWriter` + DI + lời gọi trong `VaultItemRepository`.
- [x] Bỏ toggle Auto-backup trong Settings và folder picker trong `MainActivity`.
- [x] `BackupPreferences`: bỏ phần folder URI. Giữ nhắc export định kỳ 30 ngày (`ExportReminderWorker`) — giờ nhắc export CSV.

### C. Export CSV thường

- [x] `ExportViewModel`/`ExportScreen`: bỏ bước chọn `.pwvbackup`/CSV, bỏ cảnh báo 2 checkbox, bỏ mật khẩu zip. Luồng: nhập PIN (có 1 dòng cảnh báo file không mã hoá) → chọn nơi lưu → ghi `pwvault-<ngày>.csv`.
- [x] Xoá `PasswordZipWriter`, `ExportTempFileCleaner` (+ dependency zip nếu không còn dùng).
- [x] `CsvExporter`: thêm cột `Type` (LOGIN/NOTE) để import lại đúng loại.

### D. Import lại CSV của app

- [x] `ImportViewModel`: nếu header khớp format export của app → tự map cột (vẫn cho sửa map).
- [x] Import thêm `Type`, `Tags` (tạo tag nếu chưa có), `CustomFields` khi có các cột này. File CSV từ nguồn khác vẫn map tay như hiện tại.

### E. Dọn restore `.pwvbackup`

- [x] Xoá `RestorePasswordScreen`, state `RestorePassword`, `onRestoreFilePicked`/`restoreVault`/`cancelRestore`.
- [x] `VaultFileManager`: xoá `copyVaultFileTo`, `stageRestoreCandidate`, `tryActivateRestoreCandidate`, `discardRestoreCandidate`.
- [x] Xoá string resource không còn dùng (`values/` + `values-vi/`).

### F. Tài liệu + verify

- [x] Cập nhật `roadmap.md` (thêm dòng 19), `flow.md`, `functional-spec.md`, `architecture.md` cho phần unlock/backup.
- [x] `./gradlew ktlintCheck detekt lint assembleDebug` xanh.
- [x] Chạy thử trên emulator: setup PIN → thêm item có tag/custom field → export CSV → cài sạch → setup PIN → import CSV → dữ liệu đủ.
- [x] 1 commit cho feature (không bump version trừ khi user yêu cầu).

## Ghi chú khi làm

- Form thêm/sửa item hiện không cho sửa Custom Field và không có chọn loại Note trên UI (có sẵn từ trước). Import vẫn khôi phục đủ 2 thông tin này từ CSV.
- Custom Field trong CSV đổi từ phân cách `;` sang xuống dòng (giá trị có `;` không còn bị cắt).
- Bỏ luôn dependency `zip4j`, `androidx.documentfile` (không còn dùng).
- Settings: PIN không tắt được, có nút "Change" để đổi PIN.

## Verify

- `ktlintCheck` xanh, `assembleDebug` xanh.
- `detekt` và `lint` còn 1 lỗi mỗi loại, đều có sẵn trên `main` trước khi sửa, nằm ngoài phạm vi: `VaultItemFormScreen` (CyclomaticComplexMethod), `SettingsScreen.LanguageSection` (ContextCastToActivity).
- Emulator Pixel_5_API_34:
  - Cài mới: đặt PIN (lỗi PIN không khớp hiện đúng, ô nhập giữ nguyên) → thêm item có tag → đổi PIN → export (PIN cũ bị từ chối, PIN mới được nhận) → file CSV thường.
  - Máy mới (xoá data): đặt PIN → import CSV có thêm item Note, custom field nhiều dòng, dấu `"` → cột tự map → export lại ra đúng dữ liệu, tag mới "Wifi" được tạo.
  - Nâng cấp từ bản HEAD cũ (vault Master Password, chưa có PIN): hiện "Enter your old password" → mở xong bị ép đặt PIN (không đóng được dialog) → mở lại app chỉ hỏi PIN, dữ liệu cũ còn.
  - Vân tay (emulator): bật trong Settings → mở app hiện prompt vân tay → chạm là vào được; "Use PIN instead" chuyển về màn PIN.
  - Chưa test được khuôn mặt (emulator không có), dùng chung đường `BIOMETRIC_WEAK` với vân tay.
