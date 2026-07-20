# Plan — Feature 9: Vault Item loại Note

## Goal

- Cho Vault Item có thêm loại **Note** (ghi chú bảo mật không gắn tài khoản: tên + nội dung + tag), song song với loại **Login** hiện có — `functional-spec.md §2, §5`, `glossary.md`.
- Người dùng chọn loại khi **thêm mới**; loại không đổi được sau khi đã tạo (xem Questions).

---

## Scope

### In Scope

- Cột `type` (LOGIN/NOTE) trên `VaultItemEntity`, Room bump version 2 → 3 (destructive fallback đã có sẵn ở `VaultFileManager`, đúng pattern Feature 7 đã dùng — app chưa release nên đổi schema phá hủy vẫn chấp nhận được).
- Form thêm/sửa: bộ chọn loại (Login/Note) chỉ hiện khi **thêm mới**; chọn Note thì ẩn username/password/nút sinh mật khẩu/URL, đổi nhãn ô "Ghi chú" → "Nội dung".
- Chi tiết + danh sách: ẩn username/password/URL khi item là Note; icon nhỏ phân biệt Login/Note ở đầu dòng danh sách và tiêu đề chi tiết.
- Tái dùng field `note` sẵn có làm "nội dung" cho Note (không thêm cột mới) — đúng dữ liệu đang có, chỉ khác nhãn hiển thị theo loại.

### Out of Scope

- Đổi loại item sau khi đã tạo (Login ↔ Note) — không có yêu cầu, tránh phát sinh xử lý dữ liệu thừa (xóa username/password khi chuyển Login → Note).
- Custom Field (Feature 8, agent khác đang làm song song).
- Mọi tinh chỉnh UI/UX thêm ngoài icon phân biệt tối thiểu (màu sắc, badge, layout khác biệt sâu hơn) — theo quy ước dự án, gom vào `feature-16-ui-ux-redesign-plan.md`.

---

## Files

**Sửa:**
- `domain/VaultItem.kt` — thêm `enum class VaultItemType { LOGIN, NOTE }`, thêm field `type: VaultItemType = VaultItemType.LOGIN` vào `VaultItem`.
- `data/VaultItemEntity.kt` — thêm cột `type: String = VaultItemType.LOGIN.name`; cập nhật `toDomain()`/`toEntity()` map qua `VaultItemType.valueOf(type)` / `type.name`.
- `data/VaultDatabase.kt` — `version = 2` → `version = 3`.
- `ui/vault/VaultViewModel.kt` — `save()` thêm tham số `type: VaultItemType`; khi tạo mới dùng `type` truyền vào (Note thì ép `username`/`password`/`url` rỗng dù form có dữ liệu cũ sót lại); khi sửa giữ nguyên `existing.type`, bỏ qua `type` truyền vào (loại không đổi được sau khi tạo — chặn cả ở tầng ViewModel, không chỉ ẩn UI).
- `ui/vault/VaultItemFormScreen.kt` — thêm local state `type` (khởi tạo từ `state.initial?.type ?: VaultItemType.LOGIN`); 2 `FilterChip` chọn Login/Note, chỉ hiện khi `state.editingId == null`; ẩn username/`PasswordField`/nút sinh mật khẩu/URL khi `type == NOTE`; đổi nhãn ô note thành `item_content_label` khi `type == NOTE`.
- `ui/vault/VaultItemDetailScreen.kt` — bọc khối username/password trong `if (state.item.type == VaultItemType.LOGIN)`; URL cũng chỉ hiện khi Login; icon nhỏ trước tên theo `state.item.type`.
- `ui/vault/VaultScreen.kt` — dòng danh sách thêm icon nhỏ theo `item.type` trước tên.
- `values/strings.xml` + `values-vi/strings.xml` — thêm `item_type_login`, `item_type_note`, `item_content_label`, content-description cho icon loại item.
- `docs/plans/roadmap.md` — cập nhật trạng thái Feature 9.

**Không đụng tới** (thuộc Feature 8, agent khác đang làm ở working directory gốc): `CustomFieldEntity.kt`, `domain/CustomField.kt` — không tồn tại trong worktree này, không thêm.

---

## Implementation Steps

1. `domain/VaultItem.kt`: thêm `VaultItemType` enum + field `type`.
2. `data/VaultItemEntity.kt`: thêm cột `type`, cập nhật map 2 chiều.
3. `data/VaultDatabase.kt`: bump version → 3.
4. `ui/vault/VaultViewModel.kt`: `save()` nhận thêm `type`; áp logic ép rỗng field Login khi tạo mới Note; giữ `existing.type` khi sửa.
5. `ui/vault/VaultItemFormScreen.kt`: bộ chọn loại (2 `FilterChip`, ẩn khi editing), ẩn field theo loại, đổi nhãn ô nội dung.
6. `ui/vault/VaultItemDetailScreen.kt`: ẩn khối Login-only theo `state.item.type`, icon theo loại.
7. `ui/vault/VaultScreen.kt`: icon theo loại ở dòng danh sách.
8. i18n string mới (en + vi).
9. Build + verify: `ktlintCheck detekt lint testDebugUnitTest assembleDebug`.
10. Verify sống: thêm item chọn Note → chỉ thấy tên+nội dung+tag, không thấy username/password/URL → lưu → hiện đúng trong list (icon Note) → mở chi tiết đúng → sửa (không thấy lại bộ chọn loại) → xóa có xác nhận; thêm item Login như cũ vẫn hoạt động đúng (không regressions); auto-lock → mở lại, dữ liệu cả 2 loại còn nguyên.
11. `/code-guard` + `/code-review`, sửa hết Critical/Major trước khi commit.
12. Cập nhật `roadmap.md` Feature 9 = Done.
13. 1 commit.

---

## Risks

- Room version bump 2 → 3 dùng `fallbackToDestructiveMigration` (đã có sẵn) — xóa sạch dữ liệu test hiện tại trên thiết bị/emulator khi cài đè bản mới, giống hệt Feature 7 đã gặp. Cần `pm clear` hoặc chấp nhận mất dữ liệu test cũ trước khi verify sống — không ảnh hưởng người dùng thật (chưa release).
- Nếu quên ép rỗng `username`/`password`/`url` khi tạo mới Note (bug tương tự có thể xảy ra nếu user gõ vào các field đó rồi mới chuyển sang Note trước khi field bị ẩn) → dữ liệu rác lưu vào DB dù UI không cho thấy. Xử lý ở bước 4 (ép rỗng tại ViewModel, không tin tưởng riêng UI ẩn field).

---

## Questions

*(đã đề xuất phương án — chỉ hỏi lại nếu muốn đổi)*

- **Loại item có đổi được sau khi tạo không (Login ↔ Note)?** 👉 Không — spec không yêu cầu, và đổi loại kéo theo phải xử lý dữ liệu mồ côi (Note chuyển sang Login thì username/password trống, Login chuyển sang Note thì mất username/password đã lưu mà không cảnh báo rõ ràng). Giữ đơn giản: chọn loại lúc tạo, cố định.
- **Nội dung Note có bắt buộc nhập không?** 👉 Không — giữ đúng nguyên tắc đã chốt ở Feature 5 (chỉ `name` bắt buộc), tránh validation khác nhau giữa 2 loại làm phức tạp form.
- **Có cần icon/badge phân biệt Login/Note ở danh sách không?** 👉 Có, tối thiểu 1 icon nhỏ — đây là nhu cầu chức năng cốt lõi để phân biệt 2 loại item (không có thì user phải mở từng item mới biết), không phải trang trí thêm nên không đẩy sang Feature 16. Mọi tinh chỉnh sâu hơn (màu, badge, layout) mới đẩy sang Feature 16.

---

## Status

✅ Hoàn tất. Build xanh (`ktlintCheck detekt lint testDebugUnitTest assembleDebug`). `/code-guard` + `/code-review` không có Critical/Major; 1 Minor (icon loại item ở danh sách thiếu `contentDescription`) đã sửa luôn.
