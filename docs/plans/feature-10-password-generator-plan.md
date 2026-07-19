# Feature 10 — Sinh mật khẩu ngẫu nhiên tùy biến + lưu template tái dùng

## Goal

- Cho phép người dùng tùy chỉnh độ dài + nhóm ký tự khi sinh mật khẩu ngẫu nhiên (thay vì cố định 1 độ dài/1 bộ ký tự như hiện tại).
- Cho phép lưu 1 cấu hình sinh mật khẩu thành template có tên, để chọn lại dùng nhanh ở lần sau.

## Scope

### In Scope

- Nâng cấp `PasswordGenerator.generate()` nhận tham số: độ dài, bật/tắt chữ hoa/chữ thường/số/ký tự đặc biệt; đảm bảo có ít nhất 1 ký tự mỗi nhóm được chọn (Fisher–Yates với `SecureRandom`, cùng cách tiếp cận đã mô tả ở `feature-16-ui-ux-redesign-plan.md` §B1 nhưng tổng quát hóa theo tham số thay vì hardcode 8 ký tự/4 nhóm cố định).
- `PasswordGeneratorDialog` mở từ nút "Generate password" trên `VaultItemFormScreen`: slider độ dài, 4 checkbox nhóm ký tự, preview mật khẩu sinh ra + nút sinh lại, danh sách template đã lưu (áp dụng/xóa), ô lưu cấu hình hiện tại thành template mới, nút "Dùng mật khẩu này" để áp vào form.
- Bảng `password_templates` mới (Room): id, name (unique, NOCASE), length, useUpper, useLower, useDigits, useSpecial. Entity/Dao/Repository theo đúng pattern `Tag*`.
- `PasswordTemplateViewModel` riêng (giống `TagViewModel`) sở hữu CRUD template (add/delete — không cần rename, ngoài scope).
- Đăng ký entity/dao mới vào `VaultDatabase` (bump version), provider mới trong `DataModule`.
- Wiring: `MainActivity` khởi tạo `passwordTemplateViewModel by viewModels()`, truyền xuống `VaultScreen` → `VaultItemFormScreen`.
- Strings mới (en + vi) cho dialog.

### Out of Scope

- Đổi `SecurityPolicy.GENERATED_PASSWORD_LENGTH` mặc định 20 → 8 hay ép luật bắt buộc cho generator mặc định không cấu hình — đã chốt là việc riêng của Feature 16 §B1 (doc-only, chưa triển khai).
- Rename template.
- Loại ký tự dễ nhầm (0/O, 1/l/I) — thuộc Feature 16, out of scope ở đây.
- Custom charset (người dùng tự nhập ký tự riêng ngoài 4 nhóm chuẩn) — không có trong roadmap/spec.
- UX polish auto-focus/inline-error (item 5b riêng, không liên quan).

## Files

New:

- `app/src/main/java/com/pwvault/app/domain/PasswordTemplate.kt`
- `app/src/main/java/com/pwvault/app/data/PasswordTemplateEntity.kt`
- `app/src/main/java/com/pwvault/app/data/PasswordTemplateDao.kt`
- `app/src/main/java/com/pwvault/app/data/PasswordTemplateRepository.kt`
- `app/src/main/java/com/pwvault/app/ui/vault/PasswordTemplateViewModel.kt`
- `app/src/main/java/com/pwvault/app/ui/vault/PasswordGeneratorDialog.kt`

Modify:

- `app/src/main/java/com/pwvault/app/security/PasswordGenerator.kt`
- `app/src/main/java/com/pwvault/app/data/VaultDatabase.kt`
- `app/src/main/java/com/pwvault/app/di/DataModule.kt`
- `app/src/main/java/com/pwvault/app/ui/vault/VaultItemFormScreen.kt`
- `app/src/main/java/com/pwvault/app/MainActivity.kt`
- `app/src/main/java/com/pwvault/app/ui/vault/VaultScreen.kt`
- `app/src/main/res/values/strings.xml`, `app/src/main/res/values-vi/strings.xml`
- `docs/plans/roadmap.md` (cập nhật trạng thái Feature 10 — làm sau khi commit)

## Implementation Steps

1. `PasswordGenerator.kt`: tách `CHARSET` thành `UPPER`/`LOWER`/`DIGITS`/`SPECIAL`, thêm `generate(length, useUpper, useLower, useDigits, useSpecial)` với validate (>=1 nhóm được chọn, `length >= số nhóm được chọn`), giữ overload không tham số dùng default (`SecurityPolicy.GENERATED_PASSWORD_LENGTH`, cả 4 nhóm `true`) để không phá lời gọi cũ nếu còn nơi khác dùng.
2. Domain + Room: `PasswordTemplate` (data class), `PasswordTemplateEntity` (unique index NOCASE trên `name`, theo `TagEntity`), `PasswordTemplateDao` (`observeAll`/`insert`/`delete`, theo `TagDao`), `PasswordTemplateRepository` (theo `TagRepository`).
3. `VaultDatabase.kt`: thêm `PasswordTemplateEntity::class` vào `entities`, bump `version`, thêm `abstract fun passwordTemplateDao(): PasswordTemplateDao`.
4. `DataModule.kt`: thêm `providePasswordTemplateRepository`.
5. `PasswordTemplateViewModel` (`@HiltViewModel`): state `{ templates: List<PasswordTemplate>, newTemplateError }`, `startObservingTemplates()`, `addTemplate(name, options)` (validate tên trống/trùng — tái dùng logic kiểu `TagViewModel.validateName`), `deleteTemplate(template)`.
6. `PasswordGeneratorDialog` (Composable, `AlertDialog` theo pattern `PinSetupDialog`): state cục bộ length (Slider, mặc định `SecurityPolicy.GENERATED_PASSWORD_LENGTH`, range 4..64) + 4 checkbox (mặc định `true`), preview password (gọi `PasswordGenerator.generate` khi mở dialog và mỗi khi nhấn "Sinh lại"), lỗi validate hiển thị nếu bỏ hết checkbox hoặc length nhỏ hơn số nhóm đang chọn (disable nút sinh khi invalid), danh sách `templates` (row + nút xóa) — chọn 1 template set lại length/checkbox theo template rồi sinh lại preview, ô nhập tên + nút "Lưu thành template" (gọi `addTemplate` với cấu hình hiện tại), nút chính "Dùng mật khẩu này" trả preview hiện tại ra ngoài qua callback `onUsePassword`.
7. `VaultItemFormScreen.kt`: thêm tham số `passwordTemplateViewModel: PasswordTemplateViewModel`, state `showGeneratorDialog`, đổi `onClick` của nút "Generate password" thành mở dialog thay vì gọi `PasswordGenerator.generate()` trực tiếp, render `PasswordGeneratorDialog` khi `showGeneratorDialog`, `onUsePassword = { password = it; showGeneratorDialog = false }`.
8. `VaultScreen.kt` và `MainActivity.kt`: thêm field/tham số `passwordTemplateViewModel`, truyền xuyên suốt đúng cách `tagViewModel` đang được truyền, gọi `startObservingTemplates()` 1 lần mỗi phiên Unlocked tại `VaultScreen` (giống `viewModel.startObservingItems()`).
9. Strings mới (en + vi): tiêu đề dialog, label slider độ dài, 4 label checkbox nhóm ký tự, nút "Sinh lại", nút "Dùng mật khẩu này", label ô tên template, nút "Lưu thành template", label danh sách template + nút xóa, lỗi tên trống/trùng, lỗi chưa chọn nhóm ký tự nào, lỗi độ dài không hợp lệ.
10. Update `docs/plans/roadmap.md` — đánh dấu Feature 10 done, link plan doc — làm sau khi commit thành công.

## Risks

- Slider độ dài cần chặn range hợp lý (4–64) để tránh mật khẩu quá ngắn (không an toàn) hoặc quá dài (UI tràn) — validate ở cả `PasswordGenerator` (`length >= số nhóm chọn`) và dialog (disable nút khi invalid).

## Questions

- Giới hạn độ dài slider (min/max)? Đề xuất 4–64, mặc định `SecurityPolicy.GENERATED_PASSWORD_LENGTH` (hiện là 20) — không mâu thuẫn với Feature 16 §B1 (B1 chỉ đổi default khi triển khai Feature 16, không đổi ở đây). Tiến hành theo đề xuất này nếu không có phản hồi khác.
