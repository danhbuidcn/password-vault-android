# Plan — Feature 8: Custom Field

## Goal

- Cho phép người dùng thêm nhiều trường tùy biến (label/value tự do) vào 1 Vault Item, ngoài các trường mặc định — `functional-spec.md §5`, `glossary.md` (Custom Field: free-form, mã hóa cùng cấp trường mặc định).

---

## Scope

### In Scope

- Bảng `custom_fields` (Room), quan hệ 1-nhiều với `vault_items` (`ForeignKey.CASCADE`) — khác Tag (nhiều-nhiều), vì Custom Field không dùng chung giữa các item.
- Thêm/sửa/xóa Custom Field ngay trong form thêm/sửa Vault Item (danh sách label/value động, nút thêm/xóa dòng).
- Hiện Custom Field ở màn chi tiết, ẩn/hiện từng value riêng (giống password — spec nêu ví dụ "mã PIN thẻ", "backup code", đều nhạy cảm).
- Version Room tăng 2 → 3, tiếp tục `fallbackToDestructiveMigration()` (đã chốt từ Feature 7, lý do không đổi — xem `feature-07-tag-plan.md`).
- Đổi tên `VaultItemWithTags` → `VaultItemWithDetails` (nay gộp cả Tag lẫn Custom Field, tên cũ không còn đúng).

### Out of Scope

- Kiểu dữ liệu cố định cho Custom Field (free-form đúng theo spec, không có type picker).
- Custom Field dùng chung/tái sử dụng giữa các item (không nằm trong glossary, mỗi field thuộc đúng 1 item).

---

## Files

**Mới:**
- `domain/CustomField.kt` — `data class CustomField(val id: Long = 0, val label: String, val value: String)`.
- `data/CustomFieldEntity.kt` — `@Entity(tableName = "custom_fields")`, `vaultItemId` FK CASCADE, `Index("vaultItemId")`.
- `docs/plans/feature-08-custom-field-plan.md` — chính plan này.

**Sửa:**
- `data/VaultDatabase.kt` — thêm `CustomFieldEntity`, `version = 3`.
- `data/VaultItemWithTags.kt` → đổi tên file/class thành `VaultItemWithDetails`, thêm `@Relation(parentColumn = "id", entityColumn = "vaultItemId") val customFields: List<CustomFieldEntity>`.
- `data/VaultItemDao.kt` — đổi tên `observeAllWithTags`/`getByIdWithTags` → `observeAllWithDetails`/`getByIdWithDetails`; thêm `clearCustomFields(itemId)`, `insertCustomFields(fields: List<CustomFieldEntity>)`, `@Transaction setCustomFieldsForItem(itemId, fields)` (cùng pattern xóa-hết-rồi-thêm-lại như Tag).
- `data/VaultItemRepository.kt` — dùng bản `WithDetails`; thêm `setCustomFields(itemId, fields: List<CustomField>)`.
- `domain/VaultItem.kt` — thêm `customFields: List<CustomField> = emptyList()`.
- `ui/vault/VaultViewModel.kt` — `save()` nhận thêm tham số `customFields: List<Pair<String, String>>`, lọc bỏ dòng rỗng cả label lẫn value trước khi gọi `repository.setCustomFields(...)`.
- `ui/vault/VaultItemFormScreen.kt` — danh sách Custom Field động (label + value + nút xóa dòng), nút "Thêm trường".
- `ui/vault/VaultItemDetailScreen.kt` — hiện danh sách Custom Field (label + value ẩn/hiện từng dòng, dùng icon giống password).
- `values/strings.xml` + `values-vi/strings.xml` — string mới.
- `docs/plans/roadmap.md` — cập nhật trạng thái Feature 8.

---

## Implementation Steps

1. `domain/CustomField.kt`, `data/CustomFieldEntity.kt`.
2. `VaultDatabase.kt`: thêm entity, `version = 3`.
3. Đổi tên `VaultItemWithTags` → `VaultItemWithDetails`, thêm quan hệ Custom Field.
4. `VaultItemDao.kt`: đổi tên 2 hàm WithTags → WithDetails, thêm 3 hàm Custom Field.
5. `VaultItemRepository.kt`, `domain/VaultItem.kt`.
6. `VaultViewModel.save()`: thêm tham số `customFields`, lọc rỗng, gọi `setCustomFields`.
7. `VaultItemFormScreen.kt`: UI thêm/sửa/xóa dòng Custom Field.
8. `VaultItemDetailScreen.kt`: UI hiện Custom Field, ẩn/hiện từng value.
9. i18n string mới (en + vi).
10. Build + verify: `ktlintCheck detekt lint testDebugUnitTest assembleDebug`.
11. Verify sống: thêm Vault Item với 2 Custom Field → lưu → mở chi tiết thấy đúng, value ẩn mặc định, bấm hiện đúng → sửa item, xóa 1 field, thêm 1 field mới → lưu → xác nhận đúng danh sách sau sửa → xóa Vault Item → xác nhận Custom Field con cũng mất theo (cascade).
12. `/code-guard` + `/code-review`, sửa Critical/Major.
13. Cập nhật `roadmap.md` Feature 8 = Done.
14. 1 commit.

---

## Risks

- Tăng version Room lần nữa (2 → 3) — tiếp tục chấp nhận mất dữ liệu test cũ, đã có tiền lệ Feature 7.
- Danh sách Custom Field động trong form dùng key theo index (không phải id ổn định) — chấp nhận được vì thao tác chỉ thêm/xóa tuần tự, không kéo-thả sắp xếp lại.

## Questions

- **Value của Custom Field có nên ẩn mặc định như password không?** 👉 Có — ví dụ trong spec ("mã PIN thẻ", "backup code") đều là dữ liệu nhạy cảm, ẩn mặc định + nút hiện/ẩn từng dòng nhất quán với password.
