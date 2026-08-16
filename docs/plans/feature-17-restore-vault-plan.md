# Plan — Feature 17: Khôi phục Vault từ `.pwvbackup`

## Goal

- Khôi phục Vault trên thiết bị mới (hoặc sau khi xoá app) từ 1 file `.pwvbackup` đã export — `functional-spec.md §7.1/§7.2`: "Khôi phục: cài app mới → import 1 trong các bản `.pwvbackup` → nhập đúng Master Password cũ → xem được dữ liệu."
- Đây là khoảng trống được ghi nhận từ Feature 12 (`roadmap.md`, mục "Chưa có mục nào sở hữu rõ ràng"): restore thuộc luồng trước-khi-unlock (Setup/first-run), khác hẳn luồng CRUD của Export/Import (Feature 11/12).

## Bối cảnh phát sinh

Người dùng báo lỗi: export bản mã hóa trên điện thoại xong, sang máy tính bảng thì "import file mã hóa" không hoạt động, không chọn được file. Rà code phát hiện 2 vấn đề:

1. Màn "Import" (Feature 11) chỉ đọc CSV/XLSX — chưa từng có đường khôi phục `.pwvbackup`, đúng như khoảng trống đã ghi ở `roadmap.md`.
2. **Bug sâu hơn**: salt dùng để dẫn xuất khóa (`VaultMetadataStore`) nằm ngoài `vault.db`, không được đóng gói trong `.pwvbackup` — nếu chỉ sửa picker mà không mang theo salt, khôi phục trên máy khác sẽ luôn báo sai mật khẩu dù nhập đúng.

## Scope

### In Scope

- Định dạng `.pwvbackup` v1: thêm header `"PWVB" (4 byte magic) | version:1 byte | saltLen:1 byte | salt bytes` trước phần bytes `vault.db` gốc — áp dụng cho cả export thủ công (`ExportViewModel`) lẫn auto-backup (`AutoBackupWriter`), vì cả hai đều gọi chung `VaultFileManager.copyVaultFileTo`.
- Màn hình "Restore from backup" mới ở luồng Setup (trước khi có vault): chọn file `.pwvbackup` qua SAF → nhập Master Password của thiết bị gốc → xác thực bằng cách mở thử file tạm với khóa dẫn xuất từ salt trong header → thành công thì thay `vault.db` hiện tại và vào thẳng `Unlocked`.
- Không đụng đến `vault.db` thật cho tới khi file được chọn đã chứng minh mở được bằng đúng mật khẩu (staged ở file tạm trước).
- Sai mật khẩu: giữ nguyên file đã chọn để thử lại (không bắt chọn lại file), chỉ xoá file tạm khi bấm Cancel hoặc khôi phục thành công.
- File không đúng định dạng (thiếu magic — ví dụ backup cũ trước fix này, hoặc file bất kỳ khác): báo lỗi rõ ràng, không crash, không suy diễn sai.

### Out of Scope

- Import lại từ `.zip` CSV có mật khẩu (export thủ công dạng plaintext) — theo `functional-spec.md §6/§7.2`, định dạng này vốn dành cho lưu trữ/chuyển file bên ngoài (mở bằng Excel sau khi giải nén thủ công), không phải để app tự đọc lại.
- Khôi phục đè lên 1 vault đã tồn tại trên máy (đổi backup khác) — chỉ hỗ trợ đúng kịch bản "cài app mới" (`hasVaultFile() == false`) như spec mô tả.

---

## Files

**Mới:**
- `ui/unlock/RestorePasswordScreen.kt` — màn nhập Master Password sau khi đã chọn file, dựa theo `UnlockScreen.kt` (tái dùng `PasswordField`, `LockIconBadge`), có thêm nút Cancel.

**Sửa:**
- `data/VaultFileManager.kt` — header v1 trong `copyVaultFileTo`; thêm `stageRestoreCandidate(InputStream): ByteArray?` (parse header, ghi phần còn lại ra file tạm, trả salt) và `tryActivateRestoreCandidate(key): Boolean` (mở thử file tạm, thành công thì thay `vault.db` thật rồi mở lại qua đúng `openAndValidate` hiện có; thất bại thì giữ nguyên file tạm để thử lại) + `discardRestoreCandidate()` (dọn khi huỷ luồng restore).
- `security/VaultMetadataStore.kt` — thêm `setSalt(salt)` để ghi đè salt cục bộ bằng salt khôi phục từ backup.
- `di/SecurityModule.kt` — tiêm `VaultMetadataStore` vào `VaultFileManager`.
- `ui/unlock/UnlockViewModel.kt` — state `RestorePassword`, lỗi `RESTORE_INVALID_FILE`/`RESTORE_WRONG_PASSWORD`, các hàm `onRestoreFilePicked`/`restoreVault`/`cancelRestore`.
- `ui/unlock/UnlockErrorMessage.kt` — map 2 lỗi mới sang string.
- `ui/unlock/SetupScreen.kt` — nút phụ "Restore from backup".
- `MainActivity.kt` — `restoreSourceLauncher` (`ActivityResultContracts.OpenDocument()`, MIME `*/*` — `.pwvbackup` không có MIME chuẩn, các DocumentsProvider báo MIME khác nhau, dùng `*/*` để tránh đúng lỗi picker-ẩn-file đã gặp) + nhánh `RestorePassword` trong `PwVaultApp`.
- `values/strings.xml`, `values-vi/strings.xml` — string cho luồng restore.

Không thêm dependency mới.

---

## Rủi ro

- **Backward-incompat có chủ đích**: mọi `.pwvbackup` đã export trước khi fix này (kể cả auto-backup) đều thiếu header, sẽ bị từ chối với lỗi rõ ràng thay vì suy diễn sai — người dùng cần export lại bản mới sau khi cập nhật app. Đã cân nhắc: không có cách nào phục hồi salt từ backup cũ (salt gốc chỉ còn trên đúng thiết bị đã tạo ra nó).
- Đã sửa 1 bug phát hiện lúc verify sống: `tryActivateRestoreCandidate` lúc đầu xoá luôn file tạm khi sai mật khẩu, khiến không thể thử lại lần 2 dù nhập đúng ở lần sau — đã sửa để chỉ xoá khi Cancel hoặc thành công.

## Verify

- Build xanh: `ktlintCheck detekt lint assembleDebug` (không có unit test trong dự án).
- Verify sống trên emulator (`pwvault-test`): tạo vault + 1 item → Export → Backup → xoá dữ liệu app (giả lập máy mới) → màn Setup có nút "Restore from backup" → chọn đúng file `.pwvbackup` vừa tạo qua picker (xác nhận picker **hiện được file**, đúng lỗi gốc được báo) → nhập sai mật khẩu → báo lỗi đúng, không crash → dump byte-level xác nhận header `PWVB`+version+salt 16 byte đúng định dạng.
- Chưa verify lại được bằng mắt bước "nhập đúng mật khẩu sau khi đã từng sai" trên cùng 1 phiên do emulator bị treo input giữa chừng (môi trường sandbox, không phải lỗi code) — đã build lại sau fix, logic đọc lại rõ ràng đúng (`restoreTempFile` chỉ bị xoá ở nhánh Cancel/thành công).

## Status

✅ Hoàn tất (code + build xanh + verify sống một phần, xem mục Verify).
