# Plan — Feature 1: Master Password setup + Vault creation + unlock cơ bản

## Goal

- Lần đầu cài app: người dùng thiết lập Master Password → tạo file Vault mã hóa (SQLCipher).
- Lần sau: nhập đúng Master Password → mở được Vault → vào màn hình placeholder "đã unlock".
- Không lưu Master Password dưới bất kỳ hình thức nào (đúng constraint zero-knowledge).

---

## Scope

### In Scope

- Phát hiện first-run (chưa có file Vault) vs đã có Vault.
- Màn hình Setup: nhập + xác nhận Master Password, validate tối thiểu, tạo Vault.
- Màn hình Unlock: nhập Master Password, mở Vault, báo lỗi khi sai.
- KDF Argon2id: derive khóa mã hóa từ Master Password + salt ngẫu nhiên.
- Lưu salt (không bí mật) trong app private storage — dùng để derive lại khóa mỗi lần unlock.
- Tạo/mở file Vault mã hóa bằng SQLCipher raw API tại `context.filesDir` (chưa dùng Room — xem Risks).
- Màn hình placeholder sau khi unlock thành công (Vault Item list thật là Feature 5).
- Tuân `security.md`: Master Password giữ ở `CharArray`, wipe sau khi dùng, không log.

### Out of Scope

- PIN / sinh trắc học unlock (Feature 2, 3).
- Auto-lock timer, giới hạn số lần nhập sai/lockout (Feature 4).
- Vault Item CRUD thật, Room schema (Feature 5).
- Đổi Master Password.
- Bất kỳ cơ chế khôi phục Master Password (không tồn tại theo thiết kế zero-knowledge).

---

## Files

**Mới:**
- `security/KeyDerivation.kt` — wrapper Argon2id (argon2kt), input `CharArray` + salt → key bytes, wipe input sau khi dùng.
- `security/VaultMetadataStore.kt` — đọc/ghi salt (SharedPreferences, `MODE_PRIVATE`) + đường dẫn file Vault; `hasVault(): Boolean`.
- `data/VaultFileManager.kt` — tạo/mở file SQLCipher tại `context.filesDir/vault.db` bằng key derive được; trả kết quả thành công/thất bại (sai key → exception bắt được, không crash).
- `di/SecurityModule.kt` — Hilt module cung cấp `KeyDerivation`, `VaultMetadataStore`, `VaultFileManager` (singleton, cần `@ApplicationContext`).
- `ui/unlock/UnlockViewModel.kt` — `@HiltViewModel`; `StateFlow<UnlockUiState>` (`Setup`, `Locked`, `Unlocked`, lỗi kèm message); hàm `createVault(password, confirm)`, `unlock(password)`.
- `ui/unlock/SetupScreen.kt` — form Master Password + xác nhận, validate, gọi `createVault`.
- `ui/vault/VaultScreen.kt` — placeholder "Vault đã mở khóa (danh sách item — sắp có)".

**Sửa:**
- `ui/unlock/UnlockScreen.kt` — thay placeholder text bằng form nhập Master Password thật + hiển thị lỗi.
- `MainActivity.kt` — quan sát `UnlockUiState` từ `UnlockViewModel`, render `SetupScreen` / `UnlockScreen` / `VaultScreen` tương ứng.
- `app/build.gradle.kts`, `gradle/libs.versions.toml` — thêm dependency `argon2kt` (1.6.0, verified Maven Central 2026-07-19).

---

## Implementation Steps

1. Thêm `argon2kt:1.6.0` vào `libs.versions.toml` + `app/build.gradle.kts`.
2. `KeyDerivation.kt`: hàm `derive(password: CharArray, salt: ByteArray): ByteArray` dùng Argon2id, tham số `m=65536 KiB (64MB), t=3, p=1` (theo khuyến nghị OWASP cho tình huống cần bảo mật cao hơn mức server tối thiểu, chấp nhận được trên thiết bị di động tầm trung); wipe `password` array trong `finally`.
3. `VaultMetadataStore.kt`: `getOrCreateSalt(): ByteArray` (sinh `SecureRandom` 16 byte nếu chưa có, lưu Base64 vào SharedPreferences), `hasVault(): Boolean` (check file `vault.db` tồn tại).
4. `VaultFileManager.kt`: `createVault(key: ByteArray)` (tạo file mới + set key), `openVault(key: ByteArray): Boolean` (mở file hiện có, trả `false` nếu sai key thay vì crash app).
5. `SecurityModule.kt`: `@Provides @Singleton` cho 3 class trên.
6. `UnlockViewModel.kt`: init đọc `hasVault()` → set state `Setup` hoặc `Locked`. `createVault(password, confirm)`: validate khớp + độ dài tối thiểu 8 ký tự → derive key → `createVault` → state `Unlocked`. `unlock(password)`: derive key từ salt đã lưu → `openVault` → `Unlocked` hoặc lỗi "Sai Master Password".
7. `SetupScreen.kt` + sửa `UnlockScreen.kt`: `OutlinedTextField` ẩn ký tự (password), nút submit, hiển thị lỗi validate/unlock.
8. `VaultScreen.kt`: placeholder tĩnh.
9. `MainActivity.kt`: `collectAsState()` từ ViewModel, `when` render đúng màn hình.
10. Build + verify: `./gradlew ktlintCheck detekt lint assembleDebug`.
11. Cài lên thiết bị thật (`installDebug`), test tay: setup Master Password mới → thấy Vault placeholder → kill app → mở lại → nhập đúng password → vào lại → nhập sai → thấy lỗi.
12. `/code-guard` + `/code-review`.
13. 1 commit cho toàn bộ Feature 1.

---

## Status

✅ Hoàn tất và verify thật trên thiết bị (Samsung SM-A556E): Setup → tạo Vault → khóa lại (force-stop) → mở lại đúng vào Locked → nhập đúng Master Password → Unlocked. Không crash.

Đã sửa trong lúc implement (không trong plan ban đầu, phát sinh từ bug thật + feedback bạn):
- Bug thật: thiếu `System.loadLibrary("sqlcipher")` khiến tạo Vault luôn fail — đã sửa.
- Đưa Argon2id + mở SQLCipher ra khỏi main thread (`Dispatchers.Default`/`Dispatchers.IO`).
- i18n đầy đủ (English mặc định + `values-vi`), copy tiếng Việt dùng "mật khẩu" thay vì thuật ngữ "Master Password" cho dễ hiểu.
- Show/hide password toggle (`material-icons-extended`).
- `/code-guard` + `/code-review`: Passed, không finding cần sửa.

`/code-guard` + `/code-review`: Passed.

---

## Risks

- Feature 1 dùng SQLCipher **raw API** (chưa qua Room) vì chưa có entity thật — Feature 5 sẽ bọc lại bằng Room (`SupportFactory`) trên cùng file `vault.db` với cùng key derive được; cần đảm bảo tương thích khi ghép, không tạo file Vault mới.
- Argon2id `m=64MB` có thể mất 0.5–2s trên thiết bị tầm trung — chấp nhận được cho 1 lần unlock, nhưng cần đo thời gian thật trên máy bạn (Samsung A55) ở bước 11, chỉnh tham số nếu quá chậm/quá nhanh.
- Thư viện `argon2kt` dùng native lib (JNI) — rủi ro thiếu `.so` cho ABI lạ; thiết bị test (arm64-v8a) thuộc nhóm phổ biến nên rủi ro thấp.

---

## Questions

*(đã đề xuất phương án dựa trên OWASP Password Storage Cheat Sheet 2026 + best practice mobile — chỉ hỏi lại nếu bạn muốn đổi)*

- **Độ dài tối thiểu Master Password:** đề xuất 8 ký tự, không ép kiểu ký tự (chữ hoa/số/ký tự đặc biệt) ở bản đầu — strength meter chi tiết hơn để dành cho tinh chỉnh sau. 👉 Dùng 8.
- **Tham số Argon2id:** đề xuất `m=64MB, t=3, p=1` (cao hơn mức tối thiểu OWASP 19MB/t=2, phù hợp vì đây là unlock 1 lần trên thiết bị cá nhân, không phải server chịu tải nhiều request). 👉 Dùng giá trị này, đo lại thời gian thật ở bước test tay.
- **Vị trí file Vault:** đề xuất `context.filesDir/vault.db` (private storage mặc định của app, đúng constraint). 👉 Dùng giá trị này.
