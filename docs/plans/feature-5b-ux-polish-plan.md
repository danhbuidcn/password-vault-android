# Plan — Feature 5b: UX polish (SetupScreen/UnlockScreen/PinUnlockScreen/PinSetupDialog)

## Goal

- Áp dụng lại đúng 3 pattern UX đã có sẵn ở `VaultItemFormScreen` (Feature 5) cho 4 màn còn thiếu: không xóa field khi lỗi, tự ẩn lỗi khi gõ lại, auto-focus.

---

## Scope

### In Scope

- `SetupScreen.kt`, `UnlockScreen.kt`, `PinUnlockScreen.kt`: bỏ `password = ""`/`pin = ""` sau khi gọi `onCreateVault`/`onUnlock` — hiện đang xóa vô điều kiện kể cả khi sai, buộc gõ lại từ đầu. Sau khi thành công màn hình bị unmount ngay (chuyển state Unlocked) nên bỏ dòng xóa không ảnh hưởng luồng đúng.
- Cả 4 file: thêm `FocusRequester` + `LaunchedEffect(Unit) { requestFocus() }` cho field đầu tiên — đúng pattern `VaultItemFormScreen`.
- Cả 4 file: lỗi tự ẩn ngay khi gõ lại (không cần bấm submit lần nữa) — dùng `var showError by remember(error) { mutableStateOf(error != null) }`, đặt `showError = false` trong `onValueChange` của field liên quan, chỉ hiện `Text(error.message())`/truyền vào `UnlockStatusMessage` khi `showError`.

### Out of Scope

- Đổi `UnlockStatusMessage`/`PasswordField` (component dùng chung) — chỉ đổi cách gọi ở 4 màn, giữ nguyên 2 component này (lockout-priority-over-error đã đúng sẵn, không cần sửa).
- `PinSetupDialog` hiện không xóa field sau submit (khác 3 màn kia) — chỉ thêm auto-focus + tự ẩn lỗi, không có gì phải "không xóa" thêm.

---

## Files

**Sửa:**
- `ui/unlock/SetupScreen.kt`
- `ui/unlock/UnlockScreen.kt`
- `ui/unlock/PinUnlockScreen.kt`
- `ui/unlock/PinSetupDialog.kt`

---

## Implementation Steps

1. `SetupScreen.kt`: `FocusRequester` cho ô password đầu; bỏ `password = ""; confirm = ""`; `showError` pattern, gắn vào cả 2 `onValueChange`.
2. `UnlockScreen.kt`: tương tự, bỏ `password = ""`; `showError` truyền vào `UnlockStatusMessage(error = if (showError) error else null, ...)`.
3. `PinUnlockScreen.kt`: tương tự, bỏ `pin = ""`.
4. `PinSetupDialog.kt`: auto-focus ô PIN đầu; `showError` cho 2 field pin/confirm.
5. Build + verify: `ktlintCheck detekt lint testDebugUnitTest assembleDebug`.
6. Verify sống: nhập sai Master Password → field giữ nguyên, lỗi hiện; gõ lại 1 ký tự → lỗi tự ẩn ngay (chưa cần bấm Unlock); mở lại SetupScreen/PinSetupDialog → bàn phím tự bật đúng ô đầu tiên.
7. `/code-guard` + `/code-review`, sửa hết Critical/Major trước khi commit.
8. Cập nhật `roadmap.md` mục 5b = Done.
9. 1 commit.

---

## Risks

- Bỏ xóa password sau lần thử sai để lộ password sai trên màn hình lâu hơn (đến khi user tự xóa/gõ lại) — đánh đổi UX vs lộ tạm thời trong phiên của chính user, đã được roadmap chốt là ưu tiên UX (không phải hổng bảo mật với người ngoài, password đã ẩn qua `PasswordVisualTransformation`).

---

## Status

✅ Hoàn tất. Build xanh (`ktlintCheck detekt lint testDebugUnitTest assembleDebug`). `/code-guard` không có violation. `/code-review` tìm 1 Major, đã sửa trước commit:

`showError` bản đầu dùng `remember(error) { mutableStateOf(error != null) }` — key theo giá trị `error`. `UnlockViewModel.setupPin()` khi validation fail (PIN_MISMATCH...) set lỗi trực tiếp, không reset về `null` qua bước `busy=true` như `unlock()`/`createVault()`/`unlockWithPin()`. Hậu quả: 2 lần thất bại liên tiếp cùng loại lỗi (vd PIN_MISMATCH) → `error` không đổi giá trị giữa 2 lần → `remember(error)` không tính lại → lỗi không hiện lại lần 2 dù ViewModel có lỗi mới thật. Đã đổi cả 4 file sang `showError` điều khiển bởi hành động người dùng (bấm submit → `showError = true`, gõ lại → `showError = false`) thay vì key theo giá trị `error` — tránh phụ thuộc ngầm vào việc ViewModel có reset lỗi về null hay không.
