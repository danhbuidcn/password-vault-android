# Plan — Feature 12: Export (mã hóa `.pwvbackup` + plaintext CSV)

## Goal

- Export toàn bộ Vault ra file mã hóa `.pwvbackup` (mặc định, dùng backup/chuyển máy) hoặc CSV plaintext (tùy chọn phụ, nén kèm mật khẩu) — `functional-spec.md §7`.
- Bắt buộc xác thực lại Master Password trước mọi thao tác export; CSV bắt buộc cảnh báo 2 bước + nén zip có mật khẩu + tự xóa file tạm.

---

## Scope

### In Scope

- Xác thực lại Master Password trước export (`UnlockViewModel.verifyMasterPassword`), dùng chung cho cả 2 loại export.
- Export `.pwvbackup`: chọn nơi lưu qua SAF, checkpoint WAL rồi copy nguyên file `vault.db` (đã mã hóa AES-256 sẵn bởi SQLCipher, khóa từ Master Password) — không cần định dạng/mã hóa riêng, tái dùng đúng cơ chế mã hóa hiện có.
- Export CSV plaintext: cảnh báo 2 bước (2 checkbox xác nhận) → nhập mật khẩu nén (riêng, không phải Master Password) → sinh CSV vào file tạm private → nén zip có mật khẩu (thư viện mới `zip4j`) → ghi ra SAF → tự xóa file CSV tạm sau 5 phút (chết theo tiến trình app nếu thoát sớm hơn, giống pattern `ClipboardClearer`).
- Entry point tạm thời trên `VaultScreen` (giống nút PIN/sinh trắc học/Manage tags hiện có) — dọn sang Settings ở Feature 15.

### Out of Scope

- Export Excel (`.xlsx`) — dự án chưa có thư viện đọc/ghi xlsx nào; Feature 11 (Import CSV/Excel, agent khác đang làm song song) sẽ cần thư viện **đọc** xlsx — nên chọn chung 1 thư viện đọc+ghi để tránh 2 dependency xlsx khác nhau/xung đột khi merge. CSV đã đáp ứng "plaintext, mở được bằng Excel/Sheets". Làm follow-up sau khi biết Feature 11 chọn thư viện gì.
- Import/khôi phục lại từ `.pwvbackup` — cần màn hình mới ở luồng trước-khi-unlock (Setup/first-run), khác hẳn luồng CRUD của feature này; chưa roadmap item nào sở hữu rõ ràng việc này. Ghi chú lại ở `roadmap.md`, không tự mở rộng phạm vi.
- Auto-backup nền + nhắc định kỳ 30 ngày + rotate 5 bản — đúng phạm vi Feature 13 (phụ thuộc: "12 → 13: rotate backup cần cơ chế export `.pwvbackup` đã có trước", cơ chế đó chính là bước copy file ở feature này).

---

## Files

**Mới:**
- `export/CsvExporter.kt` — `fun toCsv(items: List<VaultItem>): String` (header + escape RFC4180: quote field chứa `,`/`"`/xuống dòng, double-up `"`). Cột: Name, Username, Password, URL, Note, Tags (join `;`), CustomFields (join `label:value;`).
- `export/PasswordZipWriter.kt` — bọc `zip4j`: `writeSingleEntryZip(sourceFile: File, entryName: String, password: CharArray, destination: OutputStream)`.
- `ui/export/ExportViewModel.kt` — state machine: `Closed` → `Choice` (chọn Backup/CSV) → `Reauth` (nhập lại Master Password) → (`Backup`: pick SAF → ghi) hoặc (`CsvWarning` 2 checkbox → `CsvPassword` nhập mật khẩu nén → pick SAF → ghi).
- `ui/export/ExportScreen.kt` — Compose UI cho từng state ở trên, tái dùng `PasswordField` cho ô Master Password và mật khẩu nén.
- `values/strings.xml` + `values-vi/strings.xml` — string mới cho toàn bộ luồng export.

**Sửa:**
- `data/VaultFileManager.kt` — thêm `suspend fun copyVaultFileTo(output: OutputStream)`: `PRAGMA wal_checkpoint(FULL)` qua `database()` rồi stream-copy `vaultFile` (không lộ `File`/path ra ngoài class, giữ đúng style encapsulation hiện có).
- `ui/unlock/UnlockViewModel.kt` — thêm `suspend fun verifyMasterPassword(password: CharArray): Boolean`: derive lại key từ password nhập + salt hiện có, so khớp byte với `vaultKey` đang giữ trong session (không mở lại DB, không lộ key ra ngoài — chỉ trả `Boolean`).
- `security/SecurityPolicy.kt` — thêm `EXPORT_TEMP_FILE_CLEANUP_DELAY = 5.minutes`.
- `ui/vault/VaultScreen.kt` — thêm nút "Export" cạnh "Manage tags" (tạm thời, dọn sang Settings ở Feature 15), mở `ExportScreen` khi bấm.
- `MainActivity.kt` — thêm `exportViewModel: ExportViewModel by viewModels()`, truyền xuống `VaultScreen`/`ExportScreen` cùng `unlockViewModel::verifyMasterPassword` (đúng pattern truyền function reference đang dùng cho `onSetupPin`).
- `gradle/libs.versions.toml` + `app/build.gradle.kts` — thêm dependency `zip4j` (2.11.5, pure Java, không cần thư viện Android riêng).
- `docs/plans/roadmap.md` — cập nhật trạng thái Feature 12; ghi chú "restore từ `.pwvbackup`" và "export Excel" còn thiếu chủ, chưa gán feature nào.

---

## Implementation Steps

1. `gradle/libs.versions.toml` + `app/build.gradle.kts`: thêm `zip4j`.
2. `security/SecurityPolicy.kt`: thêm `EXPORT_TEMP_FILE_CLEANUP_DELAY`.
3. `data/VaultFileManager.kt`: `copyVaultFileTo(output)`.
4. `ui/unlock/UnlockViewModel.kt`: `verifyMasterPassword(password)`.
5. `export/CsvExporter.kt`, `export/PasswordZipWriter.kt`.
6. `ui/export/ExportViewModel.kt`: state machine + gọi các hàm trên; tự xóa file CSV tạm bằng scope riêng sống theo Hilt singleton/ViewModel process lifetime (không dùng `viewModelScope`, cùng lý do đã áp dụng cho `ClipboardClearer` ở Feature 5).
7. `ui/export/ExportScreen.kt`: UI từng bước, dùng `ActivityResultContracts.CreateDocument` (gọi qua callback truyền từ `MainActivity`, theo đúng cách `BiometricPrompt` đang được kích hoạt từ Activity xuống ViewModel).
8. `MainActivity.kt`: đăng ký `CreateDocument` launcher, `exportViewModel`, truyền props xuống.
9. `ui/vault/VaultScreen.kt`: nút Export + `when` sang `ExportScreen`.
10. i18n string mới (en + vi).
11. Build + verify: `ktlintCheck detekt lint testDebugUnitTest assembleDebug`.
12. Verify sống: mở Vault có sẵn item → Export → nhập sai Master Password bị chặn → nhập đúng → chọn Backup → chọn nơi lưu → mở lại file bằng chính app (đổi tên file thành `vault.db`, thay file private, mở app) xác nhận dữ liệu khớp; chọn CSV → tick đủ 2 cảnh báo → nhập mật khẩu nén → chọn nơi lưu → giải nén bằng mật khẩu đã nhập, mở CSV xác nhận đúng dữ liệu (kể cả item có custom field/tag) → đợi > 5 phút xác nhận file CSV tạm trong `cacheDir` đã bị xóa.
13. `/code-guard` + `/code-review`, sửa hết Critical/Major trước khi commit.
14. Cập nhật `roadmap.md` Feature 12 = Done + ghi chú 2 mục còn thiếu chủ.
15. 1 commit.

---

## Risks

- Room/SQLCipher có thể đang ở WAL mode (Room mặc định bật khi driver hỗ trợ) — copy thẳng `vault.db` mà không checkpoint trước có thể thiếu dữ liệu vừa ghi (còn nằm ở file `-wal`). Đã xử lý ở bước 3 (`PRAGMA wal_checkpoint(FULL)` trước khi copy).
- Thêm dependency `zip4j` cùng lúc nhiều feature khác cũng đang sửa `build.gradle.kts`/`libs.versions.toml` (Feature 8/10/11 đang chạy song song ở worktree khác) → nguy cơ conflict khi merge vào `main`. Chấp nhận được — đây là chi phí tất yếu của việc làm song song trên file dùng chung, không có cách né hoàn toàn; xử lý ở bước merge, không phải bước implement.
- Mật khẩu nén CSV nhập sai/quên → mất luôn nội dung export đó (giống triết lý zero-knowledge toàn app) — chấp nhận được, đúng tinh thần bảo mật đã chốt từ đầu dự án, không phải bug.

---

## Questions

*(đã đề xuất phương án — chỉ hỏi lại nếu muốn đổi)*

- **Mật khẩu nén CSV có dùng lại Master Password không, hay nhập riêng?** 👉 Nhập riêng — CSV plaintext có thể được mở trên máy khác không cài app này (dùng tool zip thông thường), không nên tái dùng Master Password cho việc đó (giảm rủi ro nếu file zip bị lộ, không đồng thời lộ luôn Master Password thật).
- **"2 bước cảnh báo" hiện dưới dạng gì?** 👉 1 màn hình với 2 checkbox bắt buộc tick trước khi nút "Tiếp tục" bật — đơn giản hơn 2 dialog nối tiếp, vẫn đúng tinh thần "phải xác nhận 2 lần" của spec.
- **Restore từ `.pwvbackup` và export Excel để feature nào làm?** 👉 Chưa gán — ghi chú rõ trong `roadmap.md` để không rơi vào khoảng trống, quyết định sau khi Feature 11/15 rõ hình hài hơn.

---

## Status

✅ Hoàn tất. Build xanh (`ktlintCheck detekt lint testDebugUnitTest assembleDebug`). `/code-guard` không có violation. `/code-review` tìm 3 Major, đã sửa hết trước commit:
1. `writeBackup`/`writeCsv` trong `ExportViewModel` chạy I/O đĩa (ghi file, nén AES zip4j, copy stream) trực tiếp trong `viewModelScope.launch` không có `withContext(Dispatchers.IO)` → chạy trên main thread, rủi ro ANR. Đã bọc cả 2 hàm trong `withContext(Dispatchers.IO)`.
2. `MainActivity.pendingExportTarget` là field thường, mất khi Activity bị recreate (xoay màn hình lúc SAF picker đang mở) → `onExportDestinationPicked` silently no-op, kẹt vĩnh viễn ở state `PickDestination`. Đã lưu/khôi phục qua `onSaveInstanceState`/`savedInstanceState`.
3. State `PickDestination` trong `ExportScreen` không có nút Cancel → nếu callback không bao giờ được gọi (kể cả sau khi sửa #2, các trường hợp khác như app bị kill giữa lúc picker mở), user kẹt không thoát được. Đã thêm nút Cancel gọi `onClose`.
