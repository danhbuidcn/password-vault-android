# Code Plan: ui-security-settings-fixes

Feature: 9 phản hồi UI/UX/bảo mật từ user (list, weak-password warning, Security section,
đổi ngôn ngữ, TagManager/Export theme, auto-backup on/off, unlock method on/off, giảm file backup).

## Goal

- Sửa 9 điểm UI/UX/bảo mật trên pwvault-android theo phản hồi người dùng (2026-07-28).
- Không đổi kiến trúc hiện có, tái dùng pattern sẵn có (ThemePreferences, SettingsIconRow, Surface theo theme).

## Scope

### In Scope

1. VaultItemCard (màn list): tag lên cùng row với Name; Username có row riêng, chiếm full width.
2. Cảnh báo mật khẩu yếu: chỉ hiện ở VaultItemFormScreen (create/edit) dưới ô password; bỏ khỏi
   VaultItemDetailScreen (giữ lại cảnh báo duplicate ở Detail).
3. SettingsScreen: thêm dòng mô tả dưới tiêu đề "Security" giải thích auto-lock dùng để làm gì.
4. Thêm chức năng đổi ngôn ngữ (System/English/Tiếng Việt) trong SettingsScreen.
5. TagManagerScreen: bọc nội dung trong `Surface` cùng `MaterialTheme.colorScheme.background`
   như các màn khác — sửa màu chữ ô input bị xám khó đọc.
6. ExportScreen: bọc nội dung trong `Surface` cùng `MaterialTheme.colorScheme.background`;
   đồng bộ spacing/style nút với các màn khác.
7. Auto-backup: cho phép tắt (hiện tại chỉ có thể bật/đổi folder, không tắt được).
8. Unlock method (PIN, Fingerprint/Face) cho phép bật/tắt riêng, nhưng luôn phải còn ít nhất
   1 phương thức "on" trong {PIN, Biometric}. Trạng thái ban đầu (chưa setup gì) giữ nguyên.
9. Auto-backup: ghi đè 1 file cố định thay vì tạo file mới + rotate mỗi lần sửa item.

### Out of Scope

- Logic cảnh báo duplicate password (giữ nguyên, chỉ bỏ phần "weak" khỏi Detail).
- Thêm ngôn ngữ khác ngoài English/Tiếng Việt (2 locale đã có sẵn trong `values`/`values-vi`).
- Redesign toàn bộ ExportScreen/TagManagerScreen ngoài đồng bộ nền + spacing cơ bản.
- Tính năng restore/phục hồi từ file backup (không có sẵn, không nằm trong phản hồi user).
- App password (master password) — luôn "always on", không nằm trong nhóm bật/tắt được.

## Flow

1. Vault list → `VaultItemCard`: Row(Name + Tags) ở trên, Row(Username, full width) ở dưới.
2. Form (create/edit) → gõ password → tính `PasswordStrength.isWeak()` cục bộ theo từng ký tự →
   hiện cảnh báo dưới `PasswordField`. Detail screen → chỉ còn cảnh báo duplicate (nếu có).
3. Settings → mục "Security" → hiện `Text` mô tả dưới `SettingsSectionTitle`.
4. Settings → chọn ngôn ngữ → `SettingsViewModel.setLanguage()` → `LanguagePreferences.setLanguage()`
   → `AppCompatDelegate.setApplicationLocales()` → hệ thống áp dụng locale, Activity recreate
   (ViewModel — kể cả trạng thái Unlocked — sống sót qua recreate như xoay màn hình).
5. TagManager/Export mở lên → `Surface(color = MaterialTheme.colorScheme.background)` bọc ngoài.
6. Settings → bật switch Auto-backup → mở folder picker (flow cũ, không đổi) → lưu URI.
   Settings → tắt switch → `BackupPreferences` xoá URI đã lưu → `AutoBackupWriter` ngừng ghi.
7. Settings → tắt PIN (hoặc Biometric) → `UnlockViewModel` kiểm tra phương thức còn lại có "on"
   không → nếu không còn phương thức nào "on" thì chặn (switch không cho tắt) → nếu còn, gọi
   `PinManager.clearPin()` / `BiometricUnlockManager.disable()` rồi cập nhật `Unlocked.hasPin/hasBiometric`.
8. Add/update/delete item → `VaultItemRepository` gọi `AutoBackupWriter.scheduleBackup()` →
   ghi đè `pwvault-auto-backup.pwvbackup` cố định (bỏ timestamp + rotate).

## Files

- `app/src/main/java/com/pwvault/app/ui/vault/VaultScreen.kt` — (1) restructure `VaultItemCard`.
- `app/src/main/java/com/pwvault/app/ui/vault/VaultItemFormScreen.kt` — (2) thêm cảnh báo weak password.
- `app/src/main/java/com/pwvault/app/ui/vault/VaultItemDetailScreen.kt` — (2) bỏ nhánh `weak` trong `PasswordWarnings`.
- `app/src/main/java/com/pwvault/app/ui/vault/VaultViewModel.kt` — (2) `VaultItemWarning`/`computeWarnings`: bỏ field `weak`.
- `app/src/main/java/com/pwvault/app/ui/settings/SettingsScreen.kt` — (3) mô tả Security, (4) UI chọn ngôn ngữ, (7) switch auto-backup, (8) switch PIN/Biometric.
- `app/src/main/java/com/pwvault/app/ui/settings/SettingsViewModel.kt` — (4) `setLanguage()`, (7) `disableAutoBackup()`.
- `app/src/main/java/com/pwvault/app/security/LanguagePreferences.kt` — **mới** (4), mirror `ThemePreferences.kt`.
- `app/src/main/java/com/pwvault/app/MainActivity.kt` — (4) áp dụng locale lúc khởi động (nếu cần), (7)/(8) wiring callback mới cho `VaultScreen`/`SettingsScreen`.
- `app/src/main/java/com/pwvault/app/ui/vault/TagManagerScreen.kt` — (5) bọc `Surface` theo theme.
- `app/src/main/java/com/pwvault/app/ui/export/ExportScreen.kt` — (6) bọc `Surface` theo theme.
- `app/src/main/java/com/pwvault/app/security/BackupPreferences.kt` — (7) thêm `clearAutoBackupFolderUri()`.
- `app/src/main/java/com/pwvault/app/security/PinManager.kt` — (8) thêm `clearPin()`.
- `app/src/main/java/com/pwvault/app/security/PinCredentialStore.kt` — (8) thêm `clear()` (giống `BiometricCredentialStore.clear()`).
- `app/src/main/java/com/pwvault/app/security/BiometricUnlockManager.kt` — (8) thêm `disable()` public, tái dùng `credentialStore.clear()` + `keystoreKeyProvider.deleteKey()`.
- `app/src/main/java/com/pwvault/app/ui/unlock/UnlockViewModel.kt` — (8) thêm `disablePin()`/`disableBiometric()` với guard "ít nhất 1 phương thức on".
- `app/src/main/java/com/pwvault/app/data/AutoBackupWriter.kt` — (9) đổi sang ghi đè 1 file cố định, bỏ `rotate()`/timestamp.
- `app/src/main/java/com/pwvault/app/security/SecurityPolicy.kt` — (9) bỏ `AUTO_BACKUP_MAX_FILES` (không còn dùng).
- `app/src/main/res/values/strings.xml`, `values-vi/strings.xml` — string mới cho (3),(4),(7),(8).
- `app/build.gradle.kts`, `gradle/libs.versions.toml` — (4) thêm dependency `androidx.appcompat:appcompat` (⚠️ cần xác nhận, xem Questions).

## Implementation Steps

1. (1) Sửa `VaultItemCard`: đưa Row tag vào chung Row với Name (thay vì Row ngoài cùng); tách
   Username thành Row/Text riêng bên dưới, full width trong `Column(weight(1f))`.
2. (2) Thêm `showWeakPasswordWarning` cục bộ trong `VaultItemFormScreen` (tương tự `showNameError`)
   dùng `PasswordStrength.isWeak(password)`, hiện `Text` dưới `PasswordField`.
3. (2) `VaultItemDetailScreen.PasswordWarnings`: chỉ xét `warning.duplicate`, bỏ nhánh `weak`.
4. (2) `VaultViewModel`: bỏ field `weak` khỏi `VaultItemWarning` và khỏi `computeWarnings()`
   (không còn nơi nào dùng sau bước 3).
5. (3) Thêm string `settings_security_section_description`, hiện `Text` (bodySmall,
   onSurfaceVariant) ngay dưới `SettingsSectionTitle(R.string.settings_security_section)`.
6. (4) Tạo `LanguagePreferences` (mirror `ThemePreferences`, enum `AppLanguage { SYSTEM, EN, VI }`).
   Thêm `SettingsViewModel.setLanguage()` cập nhật state + gọi `AppCompatDelegate.setApplicationLocales()`.
   Thêm `FilterChip` row trong `SettingsScreen` giống `ThemeSection`.
7. (5) `TagManagerScreen`: bọc toàn bộ `Column` hiện có trong
   `Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background)`.
8. (6) `ExportScreen`: bọc `Column` hiện có trong `Surface` cùng cách; chỉnh spacing nút theo
   pattern `VaultItemFormScreen`/`SettingsScreen` (padding, `fillMaxWidth()`).
9. (7) `BackupPreferences`: thêm `clearAutoBackupFolderUri()`. `SettingsScreen.DataSection`:
   đổi `SettingsIconRow` auto-backup thành có `Switch` (checked = hasAutoBackupFolder); bật → gọi
   `onPickAutoBackupFolder` (không đổi); tắt → gọi `onDisableAutoBackup` mới (xoá URI qua ViewModel/MainActivity).
10. (8) `PinCredentialStore`/`BiometricCredentialStore`: thêm `clear()` public (Pin) / dùng
    `clear()` có sẵn (Biometric). `PinManager.clearPin()`, `BiometricUnlockManager.disable()`.
11. (8) `UnlockViewModel`: thêm `disablePin()`/`disableBiometric()`, guard: chỉ cho tắt nếu
    phương thức còn lại (`hasBiometric`/`hasPin`) đang `true`; cập nhật `Unlocked` state sau đó.
12. (8) `SettingsScreen.UnlockMethodSection`: đổi PIN/Biometric row từ "On" tĩnh + nút Setup
    sang `Switch` 2 chiều; disable tương tác switch khi đó là phương thức "on" duy nhất.
13. (9) `AutoBackupWriter`: bỏ `NAME_PREFIX`/timestamp/`rotate()`, ghi trực tiếp vào tên file cố
    định `pwvault-auto-backup.pwvbackup` (ghi qua file tạm rồi rename đè, giữ pattern an toàn hiện có).
14. (9) `SecurityPolicy`: xoá `AUTO_BACKUP_MAX_FILES` sau khi không còn nơi dùng.
15. Build + `./gradlew ktlintCheck test` (theo lang/kotlin.md nếu có) để xác nhận không vỡ build.

## Quyết định đã xác nhận (2026-07-28)

- (4) Đồng ý thêm dependency `androidx.appcompat:appcompat` cho `AppCompatDelegate.setApplicationLocales()`.
- (9) Đồng ý ghi đè 1 file cố định: lần ghi đầu tiên (chưa có file) → tạo mới; các lần "user tự sửa"
  (add/update/delete item) sau đó → ghi đè file đã có. Không giữ lịch sử rotate nữa.
- (7) Tắt auto-backup **không** xoá các file `pwvault-auto-*.pwvbackup`/file backup cũ đã có trong
  folder — chỉ ngừng ghi thêm từ đó về sau.
- (6) Không có yêu cầu nào khác cho ExportScreen ngoài đồng bộ nền + spacing cơ bản.

## Risks

- (4) Đổi locale khiến Activity recreate — ViewModel (kể cả `UnlockViewModel.Unlocked`) sống sót
  qua recreate như config change bình thường (đã tested cho xoay màn hình), nhưng cần verify thực tế
  trên thiết bị vì vault database instance được mở live trong bộ nhớ.
- (9) Ghi đè 1 file duy nhất → mất "lưới an toàn" nếu 1 lần ghi bị hỏng giữa chừng (trước đây còn
  bản cũ để phục hồi). Ghi qua file tạm rồi rename đè (pattern đã có trong `AutoBackupWriter`) giảm
  rủi ro nhưng không loại bỏ hoàn toàn — đã được user chấp nhận đánh đổi này.
- (8) Thêm luồng "tắt PIN/Biometric" là tính năng bảo mật mới — cần review kỹ để không tạo ra
  trạng thái vault không thể unlock được.

## Questions

Không còn — đã xác nhận với user (xem mục "Quyết định đã xác nhận" ở trên).
