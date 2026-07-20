# Plan — Feature 15: Màn hình Settings

## Goal

- 1 màn hình Settings gộp: tham số bảo mật (auto-lock), theme, phương thức unlock (PIN/sinh trắc học) — đúng tên roadmap.
- Dọn các entry point "tạm thời" đã cố ý để trên `VaultScreen` chờ Settings — đúng như plan gốc của Feature 5 ("nút PIN/sinh trắc học... Feature 15 mới dọn sang Settings"), Feature 12 ("Entry point tạm thời... giống nút PIN/sinh trắc học/Manage tags hiện có — dọn sang Settings ở Feature 15"), Feature 13 (tương tự cho Auto-backup folder) — cả 5 nút (PIN, sinh trắc học, Manage tags, Export, Auto-backup folder) đều đã được ghi nhận là tạm thời, chuyển hết vào Settings, không phải suy đoán riêng của feature này.

---

## Scope

### In Scope

- `SettingsScreen` mới, full-screen overlay giống `TagManagerScreen`/`ExportScreen` hiện có (không phải Navigation-Compose, đúng kiến trúc đang dùng).
- Mục "Phương thức mở khóa": nút Thiết lập PIN, nút Thiết lập sinh trắc học (chuyển nguyên logic từ `VaultScreen`, không đổi hành vi).
- Mục "Bảo mật": chọn thời gian auto-lock (5 lựa chọn có sẵn ở `SecurityPolicy.AUTO_LOCK_TIMEOUT_OPTIONS`) — UI mới, `AutoLockPreferences` đã có sẵn từ Feature 4, chỉ chưa có màn hình đọc/ghi.
- Mục "Giao diện": theme Light/Dark/System — `ui/theme/PwVaultTheme.kt` đã có sẵn tham số `darkTheme`, chỉ chưa nối với lựa chọn người dùng. `ThemePreferences` mới.
- Mục "Dữ liệu": nút Manage tags, Export, Auto-backup folder (chuyển nguyên, không đổi hành vi — chỉ đổi chỗ hiển thị).
- `VaultScreen`/`VaultItemListScreen` bỏ 5 nút/CTA cũ, thay bằng 1 icon Settings duy nhất.

### Out of Scope

- Tùy biến màu thương hiệu/branding sâu hơn Light/Dark/System — đây là phần visual sâu hơn thuộc Feature 16 (đã ghi rõ trong `feature-16-ui-ux-redesign-plan.md`: "brand color/header (that's Settings, #15)" chỉ nói Settings sở hữu khái niệm này, không có nghĩa Settings phải làm color picker đầy đủ ngay — Light/Dark/System là đủ cho "theme" ở mức tối thiểu hợp lý).
- Đổi ngưỡng bảo mật khác (lockout, độ dài PIN tối thiểu...) — không phải "tham số bảo mật" nào cũng cần lộ ra UI, chỉ auto-lock đã có sẵn cơ chế đọc/ghi từ Feature 4 và được đặt tên "Feature 15 sẽ đọc/ghi" rõ ràng.

---

## Files

**Mới:**
- `security/ThemePreferences.kt` — `enum class ThemeMode { LIGHT, DARK, SYSTEM }`, get/set qua SharedPreferences, cùng pattern `AutoLockPreferences`.
- `ui/settings/SettingsViewModel.kt` — đọc/ghi `AutoLockPreferences` + `ThemePreferences`, expose `StateFlow` đơn giản (không cần sealed state phức tạp như Vault, chỉ 2 giá trị đang chọn).
- `ui/settings/SettingsScreen.kt` — UI 4 mục nêu trên, nhận callback PIN/biometric/ManageTags/Export/AutoBackupFolder từ `VaultScreen` (tái dùng nguyên, không tạo luồng mới).

**Sửa:**
- `di/SecurityModule.kt` — thêm provider `ThemePreferences`.
- `MainActivity.kt` — thêm `settingsViewModel: SettingsViewModel by viewModels()`; đọc `themeMode` để tính `darkTheme` truyền vào `PwVaultTheme(...)` ở `setContent`.
- `ui/vault/VaultScreen.kt` — bỏ `VaultActionButtons` (Manage tags/Export/Auto-backup) và 2 nút PIN/sinh trắc học trong `VaultItemListScreen`; thêm 1 icon Settings mở `SettingsScreen` (state `showSettings`, cùng pattern `showTagManager`).
- `security/AutoLockPreferences.kt` — bỏ dòng comment "Feature 15 will read/write this" (đã đúng lúc, không còn "sẽ" nữa).
- `values/strings.xml` + `values-vi/strings.xml` — string mới cho Settings.
- `docs/plans/roadmap.md` — cập nhật trạng thái Feature 15.

---

## Implementation Steps

1. `security/ThemePreferences.kt`.
2. `di/SecurityModule.kt`: provider `ThemePreferences`.
3. `ui/settings/SettingsViewModel.kt`.
4. `ui/settings/SettingsScreen.kt`: 4 mục, tái dùng callback có sẵn.
5. `ui/vault/VaultScreen.kt`: bỏ nút cũ, thêm icon Settings + `showSettings` state.
6. `MainActivity.kt`: `settingsViewModel`, nối `themeMode` vào `PwVaultTheme`.
7. `security/AutoLockPreferences.kt`: dọn comment.
8. i18n string mới (en + vi).
9. Build + verify: `ktlintCheck detekt lint testDebugUnitTest assembleDebug`.
10. Verify sống: mở Settings từ icon mới → Thiết lập PIN/sinh trắc học hoạt động y hệt trước (chuyển chỗ không đổi hành vi) → đổi auto-lock timeout, chờ đúng thời gian mới xác nhận tự khóa đúng → đổi theme Dark, thoát vào lại app xác nhận vẫn Dark (persist qua `ThemePreferences`) → Manage tags/Export/Auto-backup folder vẫn hoạt động đúng từ vị trí mới.
11. `/code-guard` + `/code-review`, sửa hết Critical/Major trước khi commit.
12. Cập nhật `roadmap.md` Feature 15 = Done.
13. 1 commit.

---

## Risks

- Chuyển PIN/biometric setup ra khỏi `VaultScreen` mất luôn CTA nổi bật ngay trên list khi user chưa thiết lập — đánh đổi chấp nhận được để dọn giao diện, đúng tinh thần "consolidation" của Feature 15; user vẫn thấy được qua Settings, không mất tính năng.
- `MainActivity` đọc `themeMode` 1 lần lúc `onCreate` giống pattern `hasAutoBackupFolder` (Feature 13) — đổi theme trong Settings cần cập nhật lại biến này để `PwVaultTheme` recompose đúng (không tự động nếu chỉ đọc 1 lần).

---

## Questions

*(đã đề xuất phương án — chỉ hỏi lại nếu muốn đổi)*

- **Theme "System" có phải mặc định không?** 👉 Có — giữ đúng hành vi hiện tại (`isSystemInDarkTheme()`) làm mặc định, không đổi trải nghiệm hiện có cho user chưa vào Settings.

---

## Status

✅ Hoàn tất. Build xanh (`ktlintCheck detekt lint testDebugUnitTest assembleDebug`). `/code-guard` + `/code-review` không có Critical/Major.

Ghi chú nhỏ (không chặn): đóng Manage Tags/Export từ trong Settings quay về thẳng màn danh sách chứ không quay lại Settings — quyết định UX chấp nhận được vì Settings là màn mới hoàn toàn, "quay đúng nơi vừa mở" chưa từng là hành vi cần giữ nguyên.
