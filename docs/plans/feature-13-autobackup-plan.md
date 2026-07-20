# Plan — Feature 13: Auto-backup nền + nhắc export định kỳ

## Goal

- Tự động ghi 1 bản `.pwvbackup` mỗi khi Vault Item thay đổi (thêm/sửa/xóa), rotate tối đa 5 bản — `functional-spec.md §7.1`.
- Nhắc người dùng export thủ công nếu quá 30 ngày chưa có export mới (dùng WorkManager) — `functional-spec.md §7.2`.

---

## Scope

### In Scope

- Chọn 1 lần thư mục đích qua SAF (`OpenDocumentTree`), xin quyền ghi lâu dài (persistable URI permission), lưu lại.
- Sau mỗi `addItem`/`updateItem`/`deleteItem` thành công: ghi 1 bản `.pwvbackup` mới vào thư mục đó — ghi atomic (file tạm → rename), không cần xác thực lại (tái dùng `VaultFileManager.copyVaultFileTo` sẵn có từ Feature 12, vốn không cần key vì chỉ copy file đã mã hóa sẵn).
- Rotate: giữ tối đa 5 file `pwvault-auto-*.pwvbackup` gần nhất trong thư mục, xóa file cũ hơn.
- Ghi nhận thời điểm export thủ công gần nhất (Feature 12) mỗi khi export thành công (cả 2 loại Backup/CSV đều tính).
- 1 `CoroutineWorker` (WorkManager) chạy định kỳ mỗi ngày, kiểm tra > 30 ngày chưa export thủ công → hiện Notification nhắc.
- Entry point tạm thời trên `VaultScreen` (giống Export/Manage tags) để chọn thư mục auto-backup — dọn sang Settings ở Feature 15.

### Out of Scope

- Bật/tắt auto-backup rõ ràng bằng toggle riêng — MVP dùng "đã chọn thư mục = bật, chưa chọn = tắt", đúng tối thiểu spec yêu cầu, không thêm toggle chưa được hỏi.
- Xử lý/hiện lỗi khi auto-backup ghi thất bại (mất quyền thư mục, hết dung lượng...) — spec đã chấp nhận rủi ro này và dùng chính cơ chế nhắc 30 ngày làm lưới an toàn (`functional-spec.md §7.2, §9`), không cần thêm UI báo lỗi riêng.
- Khôi phục từ `.pwvbackup` — vẫn là khoảng trống chưa gán feature (đã ghi ở `roadmap.md` từ Feature 12).

---

## Files

**Mới:**
- `security/BackupPreferences.kt` — SharedPreferences: `autoBackupFolderUri: Uri?` (get/set), `lastManualExportAtMillis: Long?` (get/set) — cùng pattern `AutoLockPreferences.kt`.
- `data/AutoBackupWriter.kt` — `fun scheduleBackup()`: nếu có `autoBackupFolderUri`, ghi file tạm qua `DocumentFile.createFile()` + `VaultFileManager.copyVaultFileTo()`, `renameTo()` sang tên cuối `pwvault-auto-<timestamp>.pwvbackup`, rồi xóa bớt file cũ nếu >5. Chạy trên scope riêng (`SupervisorJob() + Dispatchers.IO`, không phải `viewModelScope`) — cùng lý do đã áp dụng cho `ClipboardClearer`/`ExportTempFileCleaner`: phải hoàn tất dù ViewModel gọi nó đã bị clear.
- `security/ExportReminderWorker.kt` — `CoroutineWorker` thường (không qua Hilt — tự khởi tạo `BackupPreferences(applicationContext)` trực tiếp, không cần DI cho việc đơn giản này), so `System.currentTimeMillis() - lastManualExportAtMillis` với `SecurityPolicy.EXPORT_REMINDER_INTERVAL_DAYS`, hiện Notification nếu vượt ngưỡng.
- `docs/plans/feature-13-autobackup-plan.md` — chính plan này.

**Sửa:**
- `security/SecurityPolicy.kt` — thêm `AUTO_BACKUP_MAX_FILES = 5`, `EXPORT_REMINDER_INTERVAL_DAYS = 30`.
- `data/VaultItemRepository.kt` — thêm dependency `AutoBackupWriter`, gọi `autoBackupWriter.scheduleBackup()` cuối `addItem`/`updateItem`/`deleteItem`.
- `ui/export/ExportViewModel.kt` — `writeBackup`/`writeCsv` thành công → `backupPreferences.recordManualExportNow()`.
- `PwVaultApp.kt` — tạo Notification channel + `enqueueUniquePeriodicWork` cho `ExportReminderWorker` (1 lần, idempotent qua `ExistingPeriodicWorkPolicy.KEEP`).
- `MainActivity.kt` — thêm `ActivityResultLauncher` cho `OpenDocumentTree` (chọn thư mục auto-backup, `takePersistableUriPermission`) + xin quyền `POST_NOTIFICATIONS` (API 33+, best-effort, từ chối vẫn chạy được, chỉ là không thấy Notification).
- `ui/vault/VaultScreen.kt` — thêm nút "Auto-backup folder" cạnh Export/Manage tags, hiện trạng thái đã chọn hay chưa.
- `di/DataModule.kt` — cập nhật provider `VaultItemRepository` (thêm `AutoBackupWriter`).
- `di/SecurityModule.kt` hoặc `di/ExportModule.kt` — provider cho `BackupPreferences`, `AutoBackupWriter`.
- `app/src/main/AndroidManifest.xml` — thêm `<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />` (API 33+).
- `gradle/libs.versions.toml` + `app/build.gradle.kts` — thêm `androidx.documentfile:documentfile` (thao tác file trong thư mục SAF — `androidx.work` đã có sẵn, chưa dùng lần nào).
- `values/strings.xml` + `values-vi/strings.xml` — string mới.
- `docs/plans/roadmap.md` — cập nhật trạng thái Feature 13.

---

## Implementation Steps

1. `gradle/libs.versions.toml` + `app/build.gradle.kts`: thêm `documentfile`.
2. `security/SecurityPolicy.kt`: 2 hằng số mới.
3. `security/BackupPreferences.kt`.
4. `data/AutoBackupWriter.kt`.
5. `data/VaultItemRepository.kt`: gọi `scheduleBackup()`.
6. `di/DataModule.kt` + provider mới cho `BackupPreferences`/`AutoBackupWriter`.
7. `ui/export/ExportViewModel.kt`: ghi nhận thời điểm export thủ công.
8. `security/ExportReminderWorker.kt` + Notification channel + `AndroidManifest.xml` permission.
9. `PwVaultApp.kt`: enqueue periodic work.
10. `MainActivity.kt`: `OpenDocumentTree` launcher + xin quyền notification.
11. `ui/vault/VaultScreen.kt`: nút chọn thư mục auto-backup.
12. i18n string mới (en + vi).
13. Build + verify: `ktlintCheck detekt lint testDebugUnitTest assembleDebug`.
14. Verify sống: chọn thư mục auto-backup → thêm/sửa/xóa item → xác nhận file `pwvault-auto-*.pwvbackup` mới xuất hiện trong thư mục mỗi lần; lặp >5 lần xác nhận chỉ còn đúng 5 file mới nhất; đổi thời gian hệ thống lùi ngày kiểm tra Worker chạy đúng logic (hoặc trigger thủ công qua `adb shell am broadcast` / WorkManager test API) → thấy Notification khi quá 30 ngày kể từ export thủ công gần nhất.
15. `/code-guard` + `/code-review`, sửa hết Critical/Major trước khi commit.
16. Cập nhật `roadmap.md` Feature 13 = Done.
17. 1 commit.

---

## Risks

- `DocumentFile.renameTo()` không đảm bảo thành công trên mọi SAF provider (tùy nhà cung cấp) — nếu rename fail, coi như backup lần đó thất bại, xóa file tạm, không tính vào rotate, không crash app (đúng tinh thần "silent nhưng an toàn" đã chấp nhận ở Out of Scope).
- Ghi auto-backup chạy sau mỗi lần save có thể làm chậm cảm nhận thao tác nếu chạy đồng bộ — đã tránh bằng scope riêng chạy nền, không block coroutine trả kết quả `addItem`/`updateItem`/`deleteItem` về UI.
- WorkManager `PeriodicWorkRequest` tối thiểu 15 phút nhưng thực tế hệ thống có thể trì hoãn xa hơn 1 ngày đã đặt (Doze/battery optimization) — chấp nhận được, đây vốn là nhắc nhở phòng hờ, không phải cơ chế bảo mật cứng.

---

## Questions

*(đã đề xuất phương án — chỉ hỏi lại nếu muốn đổi)*

- **Notification permission bị từ chối thì sao?** 👉 App vẫn chạy bình thường, chỉ đơn giản không hiện được nhắc nhở — không chặn luồng chính, không hỏi lại liên tục (xin đúng 1 lần theo chuẩn Android).
- **Export CSV có tính là "đã export" để reset đồng hồ 30 ngày như Backup không?** 👉 Có — cả 2 loại export thủ công (Feature 12) đều thể hiện người dùng đã chủ động sao lưu, hợp lý để reset nhắc nhở.

---

## Status

✅ Hoàn tất. Build xanh (`ktlintCheck detekt lint testDebugUnitTest assembleDebug`). `/code-guard` không có violation. `/code-review` tìm 2 Major, đã sửa hết trước commit:
1. `AutoBackupWriter.scheduleBackup()` launch coroutine mới mỗi lần gọi trên 1 scope dùng chung, không serialize — 2 lần save liên tiếp nhanh có thể race trên cùng file tạm (`TEMP_NAME`) và trên `rotate()`. Đã thêm `Mutex` bọc quanh `writeBackup()`.
2. `getLastManualExportAtMillis() == null` (chưa từng export thủ công) khiến `ExportReminderWorker` báo "quá hạn" ngay lần chạy đầu tiên (1 ngày sau khi tạo Vault) thay vì đợi đủ 30 ngày. Đã thêm `BackupPreferences.seedReminderClockAtVaultCreation()`, gọi từ `UnlockViewModel.createVault()` khi tạo Vault thành công — đồng hồ 30 ngày bắt đầu tính từ lúc tạo Vault thay vì để `null` mãi.

Sửa #2 kéo theo đụng thêm `ui/unlock/UnlockViewModel.kt` (thêm dependency `BackupPreferences`) và `config/detekt/detekt.yml` (bump `constructorThreshold` 8→9, cùng lý do đã áp dụng cho `TooManyFunctions` ở Feature 12) — ngoài danh sách file gốc trong plan nhưng là sửa lỗi trực tiếp của chính feature này, không phải mở rộng phạm vi.
