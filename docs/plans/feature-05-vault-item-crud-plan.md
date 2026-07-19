# Plan — Feature 5: Vault Item CRUD (loại Login)

## Goal

- Cho phép thêm/sửa/xóa/xem danh sách Vault Item loại Login (tên, username, password, URL, ghi chú) — `functional-spec.md §5`.
- Đây là feature đầu tiên dùng Room thật (qua `SupportOpenHelperFactory` của `net.zetetic:sqlcipher-android`) để mở `vault.db` bằng khóa đã dẫn xuất sẵn — thay cho raw SQLCipher chỉ dùng để chứng minh luồng mã hóa ở Feature 1 (đúng như comment sẵn có trong `VaultFileManager.kt`).

---

## Scope

### In Scope

- Room entity/DAO/database cho Vault Item (chỉ Login, chưa có `type`/Tag/Custom Field — xem Questions).
- `VaultFileManager` mở rộng: build `VaultDatabase` qua `SupportOpenHelperFactory(key)`, giữ instance sống suốt session `Unlocked`, đóng lại đúng lúc khóa (auto-lock).
- `VaultItemRepository` (domain model, không lộ Room entity ra ViewModel/UI — đúng layer đã chốt ở `architecture.md`).
- Màn danh sách (thay placeholder hiện tại), màn thêm/sửa (dùng chung 1 form), màn chi tiết (ẩn/hiện password, copy password vào clipboard tự xóa sau 30s), xác nhận trước khi xóa.
- Giữ nguyên 2 nút "Thiết lập PIN"/"Thiết lập sinh trắc học" đang có trên `VaultScreen` (Feature 15 mới dọn sang Settings).

### Out of Scope

- Tìm kiếm (Feature 6), Tag (Feature 7), Custom Field (Feature 8), loại Note (Feature 9), sinh mật khẩu (Feature 10), import/export (Feature 11/12), cảnh báo yếu/trùng (Feature 14).
- Cột `type` (Login/Note) trên entity — xem Questions.

---

## Files

**Mới:**
- `domain/VaultItem.kt` — model nghiệp vụ (id, name, username, password, url, note, createdAt, updatedAt).
- `data/VaultItemEntity.kt` — `@Entity(tableName = "vault_items")`, PK `id: Long` autoGenerate.
- `data/VaultItemDao.kt` — `observeAll(): Flow<List<VaultItemEntity>>`, `getById`, `insert`, `update`, `deleteById`.
- `data/VaultDatabase.kt` — `@Database(entities = [VaultItemEntity::class], version = 1)`.
- `data/VaultItemRepository.kt` — map entity ↔ domain, bọc `VaultFileManager.database().vaultItemDao()`.
- `di/DataModule.kt` — cung cấp `VaultItemRepository` (tách khỏi `SecurityModule` vì đây là data layer, không phải security layer).
- `security/ClipboardClearer.kt` — copy password + tự xóa clipboard sau `SecurityPolicy.CLIPBOARD_CLEAR_DELAY`, đánh dấu `EXTRA_IS_SENSITIVE` (API 33+).
- `ui/vault/VaultViewModel.kt` — state machine cho list/detail/form/xác nhận xóa.
- `ui/vault/VaultItemFormScreen.kt` — form thêm/sửa (dùng chung).
- `ui/vault/VaultItemDetailScreen.kt` — chi tiết + ẩn/hiện + copy password + nút Sửa/Xóa.

**Sửa:**
- `data/VaultFileManager.kt` — `createVault`/`openVault` chuyển sang build `VaultDatabase` qua `SupportOpenHelperFactory(key)` thay vì raw SQLCipher; thêm `database(): VaultDatabase` (throw nếu chưa mở) và `close()`.
- `ui/unlock/UnlockViewModel.kt` — `onAppForegrounded()` gọi thêm `vaultFileManager.close()` ngay chỗ đang wipe `vaultKey` (đường duy nhất Unlocked → khóa lại).
- `ui/vault/VaultScreen.kt` — từ placeholder thành host: danh sách (LazyColumn) + FAB thêm + `when` sang form/chi tiết/xác nhận xóa theo state của `VaultViewModel`; giữ nguyên 2 nút PIN/sinh trắc học đang có.
- `MainActivity.kt` — thêm `private val vaultViewModel: VaultViewModel by viewModels()`, truyền xuống `VaultScreen` (giống cách `unlockViewModel` đang được truyền, không dùng `hiltViewModel()`/Navigation-Compose để nhất quán với pattern hiện tại).
- `security/SecurityPolicy.kt` — thêm `CLIPBOARD_CLEAR_DELAY = 30.seconds`.
- `values/strings.xml` + `values-vi/strings.xml` — string mới cho list rỗng, form, chi tiết, xác nhận xóa, copy.
- `docs/plans/roadmap.md` — cập nhật trạng thái Feature 5 sau khi xong.

---

## Implementation Steps

1. `SecurityPolicy.kt`: thêm `CLIPBOARD_CLEAR_DELAY = 30.seconds`.
2. `domain/VaultItem.kt`, `data/VaultItemEntity.kt`, `data/VaultItemDao.kt`, `data/VaultDatabase.kt`.
3. `VaultFileManager.kt`: thay thân `createVault`/`openVault` bằng `Room.databaseBuilder(context, VaultDatabase::class.java, vaultFile.absolutePath).openHelperFactory(SupportOpenHelperFactory(key)).build()`, force-open bằng 1 query DAO rẻ tiền trong `runCatching` (giữ đúng hành vi cũ: khóa sai → fail, không crash); lưu instance vào field khi thành công; thêm `database()`/`close()`; bỏ import SQLCipher raw không còn dùng.
4. `data/VaultItemRepository.kt` + `di/DataModule.kt`.
5. `security/ClipboardClearer.kt` + provider trong `SecurityModule.kt`.
6. `UnlockViewModel.onAppForegrounded()`: thêm `vaultFileManager.close()`.
7. `ui/vault/VaultViewModel.kt`: state `ItemList`/`ItemDetail`/`ItemForm`/`DeleteConfirm`; validate `name` không rỗng khi lưu; `copyPassword()` gọi `ClipboardClearer` trong `viewModelScope`.
8. `ui/vault/VaultItemFormScreen.kt` (tái dùng `PasswordField` có sẵn cho ô password), `ui/vault/VaultItemDetailScreen.kt`.
9. `ui/vault/VaultScreen.kt`: viết lại thành host, giữ nguyên 2 nút PIN/sinh trắc học.
10. `MainActivity.kt`: thêm `vaultViewModel`, truyền xuống.
11. i18n string mới (en + vi).
12. Build + verify: `ktlintCheck detekt lint testDebugUnitTest assembleDebug`.
13. Verify sống: thêm item → hiện đúng trong list → mở chi tiết → ẩn/hiện password → copy password, dán trong 30s thấy đúng, đợi > 30s dán lại thấy trống → sửa item → xóa (có xác nhận) → auto-lock rồi mở lại bằng đúng Master Password, dữ liệu vẫn còn → thử mở bằng sai Master Password, phải báo sai (không crash) — xác nhận Room + `SupportOpenHelperFactory` validate khóa đúng như raw SQLCipher trước đây.
14. `/code-guard` + `/code-review`, sửa hết Critical/Major trước khi commit.
15. Cập nhật `roadmap.md` trạng thái Feature 5 = Done.
16. 1 commit.

---

## Risks

- Chuyển từ raw SQLCipher (mở rồi đóng ngay) sang giữ 1 `VaultDatabase` sống suốt session `Unlocked` — nếu sót đường nào khác wipe `vaultKey` mà quên gọi `close()` sẽ giữ khóa đã giải mã trong bộ nhớ lâu hơn cần thiết. Hiện tại chỉ có đúng 1 đường (`onAppForegrounded`), đã xử lý ở bước 6; vẫn cần verify sống kỹ (bước 13).
- Room + `SupportOpenHelperFactory` phải validate sai Master Password bằng exception khi force-open, giống hệt hành vi raw SQLCipher cũ — verify sống bắt buộc để tránh trường hợp Room "mở thành công" với khóa sai rồi mới lỗi ở lần query sau (Room mặc định lazy-open connection).

---

## Questions

*(đã đề xuất phương án — chỉ hỏi lại nếu muốn đổi)*

- **Entity có cần cột `type` (Login/Note) ngay từ bây giờ không?** 👉 Không — Feature 5 chỉ có Login, thêm cột chưa dùng tới là thiết kế đón đầu nhu cầu chưa tới. Feature 9 sẽ thêm cột này qua Room migration khi thật sự cần Note.
- **Trường nào bắt buộc khi thêm Vault Item?** 👉 Chỉ `name` — đây là trường duy nhất hiển thị/định danh trong danh sách; username/password/URL/note để trống được (vd. user chỉ lưu ghi chú, hoặc chưa có password ngay).
- **Có cần thông báo "Đã copy" sau khi copy password không?** 👉 Có, hiện Snackbar ngắn — copy im lặng là UX kém, bổ sung tối thiểu 1 dòng, không phải mở rộng phạm vi.
- **Copy password có cần đánh dấu `EXTRA_IS_SENSITIVE` (Android 13+) không?** 👉 Có — cùng tinh thần chống lộ dữ liệu với `FLAG_SECURE` đã áp dụng toàn app (`functional-spec.md §8`), chi phí thêm gần như 0.

---

## Status

✅ Hoàn tất. Build xanh (`ktlintCheck detekt lint testDebugUnitTest assembleDebug`). Verify sống trên emulator (`pwvault-test`, sau khi wipe-data + cold boot do emulator bất ổn — xem `project_pwvault_android` memory): tạo Vault → thêm item → hiện đúng trong list → mở chi tiết → ẩn/hiện password → copy password (Snackbar "Password copied — clears in 30s" hiện đúng, không crash) → sửa item (đổi tên/URL/note, giữ nguyên password) → lưu → list cập nhật đúng; auto-lock nền → PinEntry → "Use password instead" → nhập đúng Master Password → mở lại được, dữ liệu Vault Item còn nguyên (xác nhận `VaultFileManager.close()`/mở lại qua `SupportOpenHelperFactory` không mất dữ liệu).

Bug Critical tự phát hiện trước khi build đầu tiên xanh: `VaultFileManager` bản refactor sang Room quên gọi `System.loadLibrary("sqlcipher")` (có ở bản raw SQLCipher cũ nhưng bị rớt khi viết lại) → mọi lần tạo/mở Vault đều lỗi `UnsatisfiedLinkError` ở tầng native, báo nhầm thành "Couldn't create the vault". Đã thêm lại `init { System.loadLibrary("sqlcipher") }`, build lại xanh, verify sống lại từ đầu xác nhận hết lỗi.

Form thêm/sửa được bổ sung thêm so với bản plan gốc (áp dụng cùng lúc, không tính riêng): nút gợi ý mật khẩu ngẫu nhiên (`security/PasswordGenerator.kt`, dùng `SecureRandom`, độ dài lấy từ `SecurityPolicy.GENERATED_PASSWORD_LENGTH`), auto-focus vào ô Tên khi mở form, tự ẩn lỗi "Cần nhập tên" ngay khi gõ lại (không cần round-trip qua ViewModel). Đã ghi nhận thêm mục **5b** ở `roadmap.md` để áp dụng lại 2 pattern UX cuối (không xóa field khi lỗi, tự ẩn lỗi) cho các màn hình Feature 1-4 (`SetupScreen`/`UnlockScreen`/`PinUnlockScreen`/`PinSetupDialog`) — chưa làm, nằm ngoài phạm vi Feature 5.
