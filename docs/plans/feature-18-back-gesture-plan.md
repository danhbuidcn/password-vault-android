# Plan — Feature 18: Hỗ trợ nút back hệ thống / vuốt cạnh trên các màn hình con

## Goal

- UX polish, không thuộc `functional-spec.md` — phát sinh từ phản hồi trực tiếp của người dùng: nút "Back" vẽ ở cuối màn Settings bất tiện, muốn vuốt từ cạnh trái (cử chỉ back của Android) cũng đóng được màn hình, giống mọi app khác. Cùng dạng với Feature 5b (UX polish không nằm trong chain phụ thuộc chính).

## Bối cảnh

Toàn bộ màn hình con trong app (Settings, chi tiết mục, quản lý tag, form thêm/sửa, Export, Import, Restore) là các nhánh `when` render có điều kiện trong `VaultScreen.kt`/`MainActivity.kt` — không dùng Navigation Compose. Rà `grep -rn "BackHandler"` toàn bộ `app/src/main/java` không có kết quả nào — không màn nào bắt sự kiện back hệ thống, nên cả nút back 3 phím lẫn cử chỉ vuốt cạnh (predictive back) đều không đóng được các màn này, chỉ nút on-screen mới có tác dụng.

## Scope

### In Scope

Thêm `androidx.activity.compose.BackHandler(onBack = <callback back/cancel/close có sẵn>)` vào đầu thân composable của 7 màn hình toàn màn hình đang có sẵn callback đóng màn:

- `SettingsScreen.kt` (`onBack`), `TagManagerScreen.kt` (`onBack`), `VaultItemDetailScreen.kt` (`onBack`), `VaultItemFormScreen.kt` (`onCancel`), `ExportScreen.kt` (`onClose`), `ImportScreen.kt` (`onClose`), `RestorePasswordScreen.kt` (`onCancel`, thuộc Feature 17).

Mỗi màn tự sở hữu hành vi back của nó (đặt `BackHandler` ngay trong file màn hình, không tập trung ở `VaultScreen.kt`) — nhất quán với cách các file này đã tự chứa state/behaviour riêng.

Đã xác nhận `ExportScreen`/`ImportScreen` không cần back-từng-bước: mọi state con bên trong đều dùng chung 1 callback đóng toàn bộ (`onCancel = onClose` mọi nơi), nên back = đóng hẳn, giống hệt nút on-screen hiện tại.

### Out of Scope

- `PinSetupDialog.kt` và hộp xác nhận xoá (`DeleteConfirm`, inline trong `VaultScreen.kt`) — cả hai dùng Material3 `AlertDialog`, tự động gọi `onDismissRequest` khi back/vuốt cạnh (hành vi mặc định của `Dialog` trên Android), không cần sửa.
- `VaultScreen.kt` (màn gốc sau khi unlock) — không có khái niệm "back" ở đây, back ở màn gốc vẫn nên thoát/minimize app như bình thường.
- Màn trước-khi-unlock khác (`UnlockScreen`, `PinUnlockScreen`, `BiometricUnlockScreen`, `SetupScreen`) — không có "màn trước" để quay lại, back thoát app ở các màn khoá là hành vi đúng/mong đợi (bảo mật).

---

## Files

**Sửa** (thêm import `androidx.activity.compose.BackHandler` + 1 dòng gọi):
- `app/src/main/java/com/pwvault/app/ui/settings/SettingsScreen.kt`
- `app/src/main/java/com/pwvault/app/ui/vault/TagManagerScreen.kt`
- `app/src/main/java/com/pwvault/app/ui/vault/VaultItemDetailScreen.kt`
- `app/src/main/java/com/pwvault/app/ui/vault/VaultItemFormScreen.kt`
- `app/src/main/java/com/pwvault/app/ui/export/ExportScreen.kt`
- `app/src/main/java/com/pwvault/app/ui/vault/ImportScreen.kt`
- `app/src/main/java/com/pwvault/app/ui/unlock/RestorePasswordScreen.kt`

Không thêm dependency mới — `androidx-activity-compose` đã có sẵn trong `libs.versions.toml`.

---

## Verify

- Build xanh: `ktlintCheck detekt lint assembleDebug`.
- Verify sống trên emulator: mở Settings → nhấn back hệ thống → quay đúng về danh sách (trước đây sẽ thoát app) → xác nhận qua `dumpsys window` (activity vẫn là `MainActivity`, không bị finish). Lặp lại với form "Add item" → back → quay đúng về danh sách. Dùng nút back hệ thống (`KEYCODE_BACK`) để verify vì đây là đúng code path (`OnBackPressedDispatcher`) mà cả nút 3 phím lẫn cử chỉ vuốt cạnh đều đi qua — không cần giả lập cử chỉ vuốt tay thật.
- Các màn còn lại (TagManager, VaultItemDetail, Export, Import, Restore) dùng chung 1 pattern đã verify — không lặp lại verify sống riêng từng cái do cùng cơ chế.

## Status

✅ Hoàn tất (code + build xanh + verify sống 2/7 màn hình đại diện — Settings và Add-item form).
