# Roadmap — Toàn bộ tính năng pwvault-android

> Danh sách tính năng chắt từ [functional-spec.md](../functional-spec.md) + [overview.md](../overview.md), chia theo thứ tự phụ thuộc (làm trước → làm sau). Mỗi mục có 1 plan riêng ở `docs/plans/<tên>-plan.md` khi tới lượt triển khai, theo đúng quy trình `/code-plan` → code → `/code-guard` → `/code-review` → verify → 1 commit.

## Trạng thái

| # | Tính năng | Trạng thái | Plan |
|---|---|---|---|
| 0 | Project scaffold (Gradle/Compose/Room+SQLCipher/Hilt/ktlint/detekt) | ✅ Done | [project-scaffold-plan.md](project-scaffold-plan.md) |
| 1 | Master Password first-run setup + tạo Vault (SQLCipher) + unlock cơ bản | ✅ Done | [feature-01-master-password-unlock-plan.md](feature-01-master-password-unlock-plan.md) |
| 2 | PIN unlock (fallback bắt buộc mọi thiết bị) | ✅ Done | [feature-02-pin-unlock-plan.md](feature-02-pin-unlock-plan.md) |
| 3 | Sinh trắc học unlock (BiometricPrompt, tùy chọn) | ✅ Done | [feature-03-biometric-unlock-plan.md](feature-03-biometric-unlock-plan.md) |
| 4 | Tự động khóa theo thời gian + giới hạn số lần nhập sai (lockout tăng dần) | ✅ Done | [feature-04-autolock-lockout-plan.md](feature-04-autolock-lockout-plan.md) |
| 5 | Vault Item CRUD — loại Login (thêm/sửa/xóa/danh sách) | ✅ Done | [feature-05-vault-item-crud-plan.md](feature-05-vault-item-crud-plan.md) |
| 5b | UX polish: không xóa field khi lỗi, tự ẩn lỗi khi gõ lại, auto-focus (SetupScreen/UnlockScreen/PinUnlockScreen/PinSetupDialog) | ✅ Done | [feature-5b-ux-polish-plan.md](feature-5b-ux-polish-plan.md) |
| 6 | Tìm kiếm Vault Item | ✅ Done | [feature-06-search-plan.md](feature-06-search-plan.md) |
| 7 | Quản lý Tag (CRUD + gắn nhiều tag cho 1 item) | ✅ Done | [feature-07-tag-plan.md](feature-07-tag-plan.md) |
| 8 | Custom Field (trường tùy biến free-form theo từng item) | ✅ Done | [feature-08-custom-field-plan.md](feature-08-custom-field-plan.md) |
| 9 | Vault Item loại Note | ✅ Done | [feature-09-note-plan.md](feature-09-note-plan.md) |
| 10 | Sinh mật khẩu ngẫu nhiên + lưu template tái dùng | ✅ Done | [feature-10-password-generator-plan.md](feature-10-password-generator-plan.md) |
| 11 | Import CSV/Excel (map cột, cảnh báo trùng lặp) | ✅ Done | [feature-11-import-plan.md](feature-11-import-plan.md) |
| 12 | Export — mã hóa `.pwvbackup` + plaintext CSV (cảnh báo 2 bước, nén có mật khẩu) | ✅ Done | [feature-12-export-plan.md](feature-12-export-plan.md) |
| 13 | Auto-backup nền (ghi mỗi khi đổi Vault Item, atomic, rotate 5 bản) + nhắc export thủ công định kỳ (WorkManager) | ✅ Done | [feature-13-autobackup-plan.md](feature-13-autobackup-plan.md) |
| 14 | Cảnh báo mật khẩu yếu/trùng lặp | ✅ Done | [feature-14-weak-duplicate-warning-plan.md](feature-14-weak-duplicate-warning-plan.md) |
| 15 | Màn hình Settings (tham số bảo mật, theme, phương thức unlock) | ✅ Done | [feature-15-settings-plan.md](feature-15-settings-plan.md) |
| 16 | UI/UX redesign Phase 1 (bảng màu ink/brass/verdigris, icon app mới, style List/Detail/Form/Settings, lọc theo Tag thật) | ✅ Done | [feature-16-ui-ux-redesign-plan.md](feature-16-ui-ux-redesign-plan.md) |

## Ghi chú phụ thuộc

- 1 → 2, 3, 4: cần Vault/unlock cơ bản trước khi thêm phương thức mở khóa phụ và rate-limit.
- 1 → 5: cần Vault (SQLCipher DB) mở được trước khi có chỗ lưu Vault Item. Feature 5 mới thêm Room thật lên trên (Feature 1 chỉ dùng SQLCipher raw API để chứng minh luồng mã hóa/mở khóa hoạt động).
- 5 → 6, 7, 8, 9: cần entity Vault Item tồn tại trước khi search/tag/custom field/note gắn vào.
- 5 → 10, 11, 12: cần entity + repository Vault Item trước khi generator lưu được, import/export đọc/ghi được.
- 12 → 13: rotate backup cần cơ chế export `.pwvbackup` đã có trước.
- Toàn bộ: 14 cần dữ liệu Vault Item thật (từ 5) để so sánh trùng lặp/độ yếu.
- 5b không nằm trong chain phụ thuộc 6-15 — làm ngay sau khi Feature 5 xong (guard/review/verify/commit) là được, không cần chờ các mục sau. Vault Item form của Feature 5 (note dạng text area, nút gợi ý mật khẩu, auto-focus, tự ẩn lỗi tên) đã làm trực tiếp trong Feature 5, không tính vào 5b.
- 16 không phải phụ thuộc kỹ thuật — là quyết định thứ tự làm việc của người dùng (2026-07-19): chỉ triển khai sau khi tất cả 1-15 đã xong, để tránh redesign UI rồi lại phải sửa lại khi các tính năng sau (Tag, Custom Field, Note, Settings...) thêm field/màn hình mới.
- **Chưa có mục nào sở hữu rõ ràng (phát sinh khi làm Feature 12):** (a) Export ra Excel `.xlsx` — Feature 12 chỉ làm CSV, xlsx cần thư viện đọc+ghi dùng chung với Feature 11 (Import), nên đợi Feature 11 chọn thư viện trước; (b) Restore/khôi phục Vault từ file `.pwvbackup` đã export — cần màn hình mới ở luồng trước-khi-unlock (Setup/first-run), khác luồng CRUD của Feature 12. Cả 2 cần được gán vào 1 feature cụ thể (có thể Feature 15 - Settings, hoặc mục mới) trước khi coi roadmap là đầy đủ.

## Quy ước triển khai từng mục

1. `/code-plan` cho đúng 1 tính năng → ghi `docs/plans/<feature>-plan.md`.
2. Code theo plan đã duyệt.
3. `/code-guard` — kiểm scope, quy ước, không phá hành vi cũ.
4. `/code-review` — tìm issue, sửa nếu có.
5. Verify: `./gradlew ktlintCheck detekt lint testDebugUnitTest assembleDebug` (+ cài thử trên máy thật khi có UI thay đổi).
6. 1 commit cho đúng 1 tính năng (theo yêu cầu — mỗi mục một commit).
7. Cập nhật trạng thái ở bảng trên.

---

*Tài liệu sống — cập nhật trạng thái mỗi khi xong 1 mục.*
