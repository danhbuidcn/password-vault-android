# Plan — Feature 3: Sinh trắc học unlock (BiometricPrompt)

## Goal

- Cho phép mở khóa nhanh bằng vân tay/khuôn mặt, tùy chọn (`functional-spec.md §4`, `architecture.md`), không thay thế PIN — PIN vẫn là fallback bắt buộc mọi thiết bị.
- Giữ đúng mô hình: sinh trắc học chỉ mở khóa UI, khóa mã hóa DB thật vẫn dẫn xuất từ Master Password; Keystore gate quyền truy cập bản Vault Key đã bọc (wrap), giống PIN nhưng gate bằng phần cứng sinh trắc học thay vì hash so sánh.

---

## Scope

### In Scope

- Thiết lập sinh trắc học (từ `VaultScreen`, tạm thời — giống cách PIN đang làm) khi Vault đã unlock, chỉ hiện nút nếu thiết bị có cảm biến đã enroll (`BiometricManager.canAuthenticate(BIOMETRIC_STRONG) == BIOMETRIC_SUCCESS`).
- Khóa AES-256-GCM riêng trong Android Keystore, **`setUserAuthenticationRequired(true)`** + yêu cầu xác thực cho **mỗi lần dùng** (không có validity duration) → bọc Vault Key. Không cần verifier riêng (không giống PIN) vì phần cứng (TEE/fingerprint HAL) tự gate qua `BiometricPrompt.CryptoObject`.
- Màn hình `BiometricEntry` thay cho `PinEntry`/`Locked` khi đã bật sinh trắc học — tự động bật `BiometricPrompt` khi vào màn hình, có nút "Thử lại" và fallback "Dùng PIN"/"Dùng mật khẩu".
- Setup cũng yêu cầu xác thực sinh trắc học ngay lúc bật (thao tác ENCRYPT_MODE cũng bị khóa bởi cùng key) — xác nhận đúng vân tay/khuôn mặt trước khi bật tính năng.
- Nếu enroll thêm vân tay/khuôn mặt mới → khóa Keystore tự invalidate (`setInvalidatedByBiometricEnrollment(true)`, mặc định) → cần thiết lập lại sinh trắc học, PIN/Master Password không bị ảnh hưởng.

### Out of Scope

- Giới hạn số lần sinh trắc học sai (hệ điều hành tự xử lý ở tầng OS, không cần lockout riêng của app — khác PIN).
- Đổi/tắt sinh trắc học sau khi bật (chỉ có bật lần đầu ở bản này, giống PIN Feature 2).
- Vị trí UI chính thức (Settings) — tạm đặt nút ở `VaultScreen`, Feature 15 dời.
- Weak biometric (`BIOMETRIC_WEAK`, ví dụ face unlock 2D không an toàn) — chỉ dùng `BIOMETRIC_STRONG`.

---

## Files

**Mới:**
- `security/BiometricKeystoreKeyProvider.kt` — tạo/lấy khóa AES-256-GCM riêng (alias khác PIN), `setUserAuthenticationRequired(true)`, yêu cầu xác thực mỗi lần dùng (API ≥30: `setUserAuthenticationParameters(0, AUTH_BIOMETRIC_STRONG)`; API 26-29: `setUserAuthenticationValidityDurationSeconds(-1)`).
- `security/BiometricCredentialStore.kt` — lưu `encryptedVaultKey` + `iv` (Base64) trong SharedPreferences riêng (`biometric_credentials`). Không lưu hash/verifier — Keystore tự gate.
- `security/BiometricUnlockManager.kt` — `hasBiometric(): Boolean`, `prepareSetupCipher(): Cipher` (ENCRYPT_MODE), `completeSetup(cipher, vaultKey)`, `prepareUnlockCipher(): Cipher?` (DECRYPT_MODE, null nếu chưa bật), `completeUnlock(cipher): ByteArray`. Cipher được `init()` trước rồi trả cho lớp UI bọc vào `BiometricPrompt.CryptoObject` — quyền dùng cipher chỉ được HAL sinh trắc học cấp sau khi `BiometricPrompt` xác thực thành công.
- `ui/unlock/BiometricUnlockScreen.kt` — icon vân tay, tự trigger prompt qua `LaunchedEffect`, nút "Thử lại", nút fallback.

**Sửa:**
- `MainActivity.kt` — đổi `ComponentActivity` → `androidx.fragment.app.FragmentActivity` (bắt buộc để tạo `BiometricPrompt`; là superset của `ComponentActivity`, `setContent` Compose vẫn hoạt động bình thường). Thêm nhánh điều hướng `BiometricEntry`. Sở hữu instance `BiometricPrompt` (cần `FragmentActivity` + executor) và hàm `authenticateBiometric(cipher, onSuccess, onError)` gọi từ Compose layer.
- `ui/unlock/UnlockViewModel.kt` — thêm `UnlockUiState.BiometricEntry`, các hàm suspend `prepareBiometricUnlockCipher(): Cipher?`, `completeBiometricUnlock(cipher)`, `prepareBiometricSetupCipher(): Cipher`, `completeBiometricSetup(cipher)`, cập nhật `initialState()` (ưu tiên: có biometric → `BiometricEntry`; có PIN → `PinEntry`; else → `Locked`), thêm `switchToPin()` (nếu có PIN) bên cạnh `switchToMasterPassword()` đã có.
- `ui/unlock/PinUnlockScreen.kt` — thêm tham số `onUseBiometric: (() -> Unit)?` tùy chọn (nút "Dùng sinh trắc học" nếu có bật, để quay lại từ PIN sang biometric).
- `ui/vault/VaultScreen.kt` — thêm nút tạm "Thiết lập sinh trắc học" (chỉ hiện nếu `canAuthenticate == BIOMETRIC_SUCCESS` và chưa bật).
- `di/SecurityModule.kt` — cung cấp `BiometricKeystoreKeyProvider`, `BiometricCredentialStore`, `BiometricUnlockManager`.
- `values/strings.xml` + `values-vi/strings.xml` — string mới cho toàn bộ UI biometric.

---

## Implementation Steps

1. `BiometricKeystoreKeyProvider`: alias riêng (`pwvault_biometric_wrap_key`), branch theo `Build.VERSION.SDK_INT` cho auth-per-use, `setInvalidatedByBiometricEnrollment(true)`.
2. `BiometricCredentialStore`: SharedPreferences riêng, lưu `encryptedVaultKey` + `iv` (Base64), `hasBiometric(): Boolean`.
3. `BiometricUnlockManager`: các hàm `prepareSetupCipher`/`completeSetup`/`prepareUnlockCipher`/`completeUnlock`, dùng `Dispatchers.Default` cho thao tác Keystore/Cipher giống `PinManager`.
4. `MainActivity`: đổi sang `FragmentActivity`, thêm helper tạo `BiometricPrompt` + `PromptInfo` (`setAllowedAuthenticators(BIOMETRIC_STRONG)`, không cho `DEVICE_CREDENTIAL` — có PIN riêng của app rồi, không cần PIN/pattern hệ thống), nhánh `BiometricEntry` trong `when(state)`.
5. `UnlockViewModel`: thêm state + hàm điều phối (chuẩn bị cipher → trả cho UI → UI gọi `BiometricPrompt` → callback trả cipher đã được cấp quyền → `completeUnlock`/`completeSetup` chạy `vaultFileManager.openVault(key)` hoặc lưu credential).
6. `BiometricUnlockScreen`: `LaunchedEffect(Unit)` tự gọi prompt lần đầu vào màn hình; nút "Thử lại" gọi lại; nút fallback sang PIN/Master Password.
7. `VaultScreen`: nút thiết lập sinh trắc học → gọi `prepareBiometricSetupCipher` → `MainActivity` trigger `BiometricPrompt` → `completeBiometricSetup`.
8. i18n string mới (en + vi) — chỉ dùng từ ngữ dễ hiểu ("Mở khóa bằng vân tay/khuôn mặt"), không dùng thuật ngữ kỹ thuật.
9. Build + verify: `ktlintCheck detekt lint assembleDebug`.
10. Verify sống: enroll vân tay ảo trên Android Emulator (`adb -e emu finger touch 1`, xem `docs/dev-setup.md`) hoặc thiết bị thật nếu đã kết nối lại — bật sinh trắc học → khóa lại → mở bằng vân tay đúng → hủy prompt (test nút "Thử lại") → chuyển sang PIN vẫn mở được → chuyển sang Master Password vẫn mở được.
11. `/code-guard` + `/code-review`, sửa hết Critical/Major trước khi commit ([[feedback_pwvault_review_before_next_feature]]).
12. 1 commit.

---

## Status

✅ Hoàn tất. Build xanh (`ktlintCheck detekt lint assembleDebug`). Verify sống đầy đủ trên Android Emulator (`pwvault-test`, vân tay ảo qua `adb -e emu finger touch`, xem `docs/dev-setup.md`):
- Thiết lập sinh trắc học từ `VaultScreen` → `BiometricPrompt` hệ thống hiện đúng tiêu đề "Unlock PWVault" → xác thực vân tay đúng → `hasBiometric` chuyển true, nút biến mất.
- Force-stop + mở lại app → `BiometricEntry` là màn hình đầu tiên (ưu tiên cao nhất), tự động bật `BiometricPrompt`.
- Vân tay đúng → mở khóa thành công. Vân tay không đúng/chưa enroll → prompt hệ thống giữ nguyên cho thử lại, không crash.
- Hủy qua nút phụ hệ thống ("Use password instead") → quay về `BiometricUnlockScreen` (không có lỗi hiển thị), có nút "Thử lại", "Dùng mã PIN thay thế", "Dùng mật khẩu thay thế".
- Chuyển qua lại PIN ↔ Biometric hoạt động đúng hai chiều (mỗi màn đều có nút chuyển sang màn còn lại khi có sẵn).
- Dùng mật khẩu (Master Password) thay thế từ màn Biometric → mở khóa đúng.
- **Kiểm tra riêng cho rủi ro "khóa Keystore bị invalidate khi enroll vân tay mới"**: enroll thêm 1 vân tay mới trên emulator → mở lại app → không crash (đã bắt `KeyPermanentlyInvalidatedException`), tự xóa credential cũ, hiện lỗi "Fingerprint/face unlock failed" nhẹ nhàng, fallback PIN vẫn hoạt động, nút "Thiết lập sinh trắc học" xuất hiện lại đúng, thiết lập lại từ đầu thành công với khóa mới.

`/code-review` (trước commit) phát hiện 1 bug Critical: `KeyPermanentlyInvalidatedException` không được bắt ở `prepareUnlockCipher`/`prepareSetupCipher`, sẽ làm crash app khi người dùng enroll vân tay mới — đã sửa (bắt exception, tự xóa khóa Keystore + credential, fallback về PIN/mật khẩu), verify sống xác nhận không còn crash. Ngoài ra sửa 1 vấn đề Minor: nút phụ của `BiometricPrompt` dùng chung text "Use password instead" cho cả luồng thiết lập lẫn mở khóa — tách thành 2 `PromptInfo` riêng, luồng thiết lập dùng "Cancel".

Chưa verify trên thiết bị vật lý (vẫn đang ngắt kết nối) — theo `architecture.md` quyết định ban đầu là ưu tiên thiết bị thật cho tính năng này, nhưng vân tay ảo trên emulator đã kiểm chứng đúng luồng Keystore/`CryptoObject`/`BiometricPrompt` thật (không phải giả lập ở tầng ứng dụng), nên đủ tin cậy để commit; sẽ test thêm bằng vân tay/khuôn mặt thật khi thiết bị kết nối lại.

## Risks

- `BiometricPrompt` cần `FragmentActivity` — đổi base class của `MainActivity` là thay đổi rủi ro thấp (superset của `ComponentActivity`) nhưng cần build+chạy lại toàn bộ luồng Setup/Locked/PinEntry để đảm bảo không có regression ngoài ý muốn.
- Trên emulator, `finger touch` là cảm biến giả lập — xác thực được cơ chế Keystore/CryptoObject thật, nhưng chưa test được vân tay/khuôn mặt thật; nếu thiết bị vật lý chưa kết nối lại kịp, ghi rõ trong `## Status` phần nào đã verify sống, phần nào còn thiếu (giống Feature 2).
- Khóa Keystore biometric bị invalidate khi enroll vân tay mới — hành vi đúng theo thiết kế, cần đảm bảo app xử lý gracefully (fallback về PIN/Master Password, không crash) khi `Cipher.init()`/`doFinal()` ném `KeyPermanentlyInvalidatedException`.
- `BIOMETRIC_STRONG` không có trên mọi thiết bị/emulator image — nút thiết lập phải ẩn đúng khi `canAuthenticate() != BIOMETRIC_SUCCESS`, tránh crash khi gọi `BiometricPrompt` trên thiết bị không hỗ trợ.

---

## Questions

*(đã đề xuất phương án — chỉ hỏi lại nếu muốn đổi)*

- **Có cho phép fallback `DEVICE_CREDENTIAL` (PIN/pattern hệ thống Android) trong `BiometricPrompt` không?** 👉 Không — app đã có PIN riêng của mình làm fallback, cho phép `DEVICE_CREDENTIAL` sẽ tạo 2 lớp PIN gây rối; chỉ dùng `BIOMETRIC_STRONG`.
- **Bắt buộc phải có PIN trước khi bật sinh trắc học không?** 👉 Không bắt buộc thứ tự — sinh trắc học độc lập với PIN (đơn giản hơn, đúng tinh thần "tùy chọn"); PIN vẫn luôn là fallback bắt buộc tồn tại độc lập theo thiết kế chung của app.
- **Test sinh trắc học khi thiết bị thật đang ngắt kết nối:** 👉 Dùng vân tay ảo trên Android Emulator (`adb -e emu finger touch`) để verify sống luồng Keystore/CryptoObject thật; nếu thiết bị thật kết nối lại trước khi commit, test thêm bằng vân tay thật cho chắc, không bắt buộc chặn commit nếu emulator đã pass đủ (giống mức độ rigor đã áp dụng ở Feature 2).
