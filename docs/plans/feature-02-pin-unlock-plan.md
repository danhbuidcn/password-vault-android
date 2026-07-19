# Plan — Feature 2: PIN unlock

## Goal

- Cho phép người dùng bật PIN số làm cách mở khóa nhanh, thay cho việc luôn phải gõ Master Password đầy đủ.
- Master Password vẫn luôn là phương án gốc, không bị thay thế — đúng `functional-spec.md §4` và `architecture.md#authentication`.

---

## Scope

### In Scope

- Thiết lập PIN (từ màn Vault, tạm thời — Settings thật là Feature 15) khi Vault đã unlock.
- Lưu trữ: khóa AES-256-GCM trong Android Keystore (non-exportable) bọc (wrap) Vault Key; PIN verifier (Argon2id hash + salt) để kiểm tra nhanh trước khi unwrap.
- Màn hình nhập PIN thay cho Unlock (Master Password) khi PIN đã bật, có nút "Dùng Master Password thay thế".
- Validate PIN tối thiểu 4 số, chỉ số (0-9).

### Out of Scope

- Sinh trắc học (Feature 3).
- Giới hạn số lần nhập sai / lockout (Feature 4) — PIN tạm thời chưa có rate-limit riêng, dùng chung cơ chế sẽ xây ở Feature 4.
- Đổi/tắt PIN sau khi đã bật (chỉ có bật lần đầu ở bản này).
- Vị trí UI chính thức (Settings) — tạm đặt nút ở `VaultScreen`, Feature 15 sẽ dời.

---

## Files

**Mới:**
- `security/PinKeystoreKeyProvider.kt` — tạo/lấy khóa AES-256-GCM trong `AndroidKeyStore` (alias riêng, non-exportable).
- `security/PinCredentialStore.kt` — lưu `pinSalt`, `pinHash` (Argon2id), `encryptedVaultKey` (Base64 ciphertext+IV) trong SharedPreferences riêng.
- `security/PinManager.kt` — `setupPin(pin, vaultKey)`, `verifyPin(pin): ByteArray?` (trả Vault Key nếu đúng, null nếu sai), `hasPin(): Boolean`.
- `ui/unlock/PinUnlockScreen.kt` — nhập PIN, nút chuyển sang Master Password.
- `ui/unlock/PinSetupDialog.kt` hoặc screen tương tự — nhập PIN mới + xác nhận.

**Sửa:**
- `ui/unlock/UnlockViewModel.kt` — thêm state cho PIN entry, hàm `setupPin`, `unlockWithPin`, `switchToMasterPassword`.
- `ui/vault/VaultScreen.kt` — thêm nút tạm "Thiết lập PIN" (chỉ hiện nếu chưa có PIN).
- `MainActivity.kt` — thêm nhánh điều hướng cho state PIN.
- `di/SecurityModule.kt` — cung cấp `PinKeystoreKeyProvider`, `PinCredentialStore`, `PinManager`.
- `values/strings.xml` + `values-vi/strings.xml` — string mới cho toàn bộ UI PIN.

---

## Implementation Steps

1. `PinKeystoreKeyProvider`: `KeyGenerator` với `AndroidKeyStore` provider, `KeyProperties.KEY_ALGORITHM_AES`, `BLOCK_MODE_GCM`, `ENCRYPTION_PADDING_NONE`, `setUserAuthenticationRequired(false)` (PIN tự app xác thực, không dùng cơ chế xác thực hệ thống — đúng tinh thần "PIN chỉ mở khóa UI").
2. `PinCredentialStore`: SharedPreferences riêng (`pin_credentials`), lưu `pinSalt` (16 byte random), `pinHash` (Argon2id(pin, pinSalt), cùng tham số với `KeyDerivation`), `encryptedVaultKey` (Base64) + `iv` (Base64).
3. `PinManager.setupPin(pin, vaultKey)`: derive `pinHash` lưu lại; mã hóa `vaultKey` bằng khóa Keystore (AES-GCM) → lưu `encryptedVaultKey` + `iv`.
4. `PinManager.verifyPin(pin)`: derive hash từ `pin` nhập vào + salt đã lưu, so với `pinHash` đã lưu (constant-time compare) — sai thì trả `null` ngay, không thử giải mã. Đúng thì giải mã `encryptedVaultKey` bằng khóa Keystore → trả Vault Key.
5. `UnlockViewModel`: thêm `UnlockUiState.PinEntry`, hàm `unlockWithPin` gọi `vaultFileManager.openVault(key)` giống hệt luồng Master Password (dùng lại logic mở Vault, không lặp code).
6. `VaultScreen`: nút "Thiết lập PIN" → dialog nhập PIN + xác nhận → gọi `setupPin`.
7. `MainActivity`: nếu `hasPin()` và Vault đang Locked → hiện `PinUnlockScreen` thay vì `UnlockScreen`; có nút chuyển sang `UnlockScreen`.
8. i18n string mới (en + vi).
9. Build + verify: `ktlintCheck detekt lint assembleDebug`.
10. Cài lên thiết bị thật, test tay: bật PIN → khóa lại → mở bằng PIN đúng → mở bằng PIN sai (báo lỗi) → chuyển sang Master Password vẫn mở được.
11. `/code-guard` + `/code-review`.
12. 1 commit.

---

## Status

✅ Hoàn tất. Build xanh (`ktlintCheck detekt lint assembleDebug`). Verify sống đầy đủ trên Android Emulator (`pwvault-test` AVD, do thiết bị thật bị ngắt kết nối USB giữa chừng): Setup → tạo Vault → Unlocked → Set up PIN → Save (hasPin chuyển true, dialog tự đóng) → force-stop + relaunch → PinUnlockScreen hiện đúng → PIN sai báo lỗi "Wrong PIN", màn hình giữ nguyên → PIN đúng unlock thành công → "Use password instead" chuyển đúng sang Master Password và unlock lại được bằng password.

`/code-review` (2 vòng):
- Vòng 1 phát hiện 1 bug Major (dialog PIN setup tự đóng che mất lỗi validate) — đã sửa bằng `LaunchedEffect(hasPin)`, chỉ đóng dialog khi setup thành công.
- Vòng 2 (trước commit) phát hiện 1 bug Major (`PinManager.verifyPin` không wipe `pin` CharArray ở nhánh early-return khi `credentialStore.load()` trả `null`, vi phạm contract wipe-mọi-nhánh của dự án) — đã sửa, build lại xanh.

## Risks

- PIN có entropy thấp (4-6 số) — bản thân PIN hash không chống brute-force cục bộ; giảm thiểu bằng lockout dùng chung sẽ xây ở Feature 4 (chưa có ở bản này, ghi rõ trong `functional-spec.md` là chấp nhận được vì PIN "chỉ mở khóa UI").
- Khóa Keystore không đồng bộ giữa các thiết bị / mất khi gỡ cài đặt — chấp nhận được (app offline single-device).
- `verifyPin` phải constant-time compare hash để tránh timing attack cơ bản — dùng `MessageDigest.isEqual` thay vì `==`/`equals` cho `ByteArray`.

---

## Questions

*(đã đề xuất phương án — chỉ hỏi lại nếu muốn đổi)*

- **Độ dài PIN tối thiểu:** đề xuất 4 số (chuẩn phổ biến, giống PIN SIM/ATM). 👉 Dùng 4, không giới hạn tối đa cụ thể (người dùng có thể chọn dài hơn nếu muốn).
- **Vị trí nút "Thiết lập PIN" tạm thời:** đề xuất đặt trong `VaultScreen` (chưa có Settings) vì đơn giản nhất, dời sang Feature 15. 👉 Dùng vị trí này.
