# Roadmap — Toàn bộ tính năng pwvault-android

> Danh sách tính năng chắt từ [functional-spec.md](../functional-spec.md) + [overview.md](../overview.md), chia theo thứ tự phụ thuộc (làm trước → làm sau). Mỗi mục có 1 plan riêng ở `docs/plans/<tên>-plan.md` khi tới lượt triển khai, theo đúng quy trình `/code-plan` → code → `/code-guard` → `/code-review` → verify → 1 commit.

## Trạng thái

| # | Tính năng | Trạng thái | Plan |
|---|---|---|---|
| 0 | Project scaffold (Gradle/Compose/Room+SQLCipher/Hilt/ktlint/detekt) | ✅ Done | [project-scaffold-plan.md](project-scaffold-plan.md) |
| 1 | Master Password first-run setup + tạo Vault (SQLCipher) + unlock cơ bản | ✅ Done | [feature-01-master-password-unlock-plan.md](feature-01-master-password-unlock-plan.md) |
| 2 | PIN unlock (fallback bắt buộc mọi thiết bị) | ✅ Done | [feature-02-pin-unlock-plan.md](feature-02-pin-unlock-plan.md) |
| 3 | Sinh trắc học unlock (BiometricPrompt, tùy chọn) | ⏳ Pending | — |
| 4 | Tự động khóa theo thời gian + giới hạn số lần nhập sai (lockout tăng dần) | ⏳ Pending | — |
| 5 | Vault Item CRUD — loại Login (thêm/sửa/xóa/danh sách) | ⏳ Pending | — |
| 6 | Tìm kiếm Vault Item | ⏳ Pending | — |
| 7 | Quản lý Tag (CRUD + gắn nhiều tag cho 1 item) | ⏳ Pending | — |
| 8 | Custom Field (trường tùy biến free-form theo từng item) | ⏳ Pending | — |
| 9 | Vault Item loại Note | ⏳ Pending | — |
| 10 | Sinh mật khẩu ngẫu nhiên + lưu template tái dùng | ⏳ Pending | — |
| 11 | Import CSV/Excel (map cột, cảnh báo trùng lặp) | ⏳ Pending | — |
| 12 | Export — mã hóa `.pwvbackup` + plaintext CSV/Excel (cảnh báo 2 bước, nén có mật khẩu) | ⏳ Pending | — |
| 13 | Auto-backup nền (ghi mỗi khi đổi Vault Item, atomic, rotate 5 bản) + nhắc export thủ công định kỳ (WorkManager) | ⏳ Pending | — |
| 14 | Cảnh báo mật khẩu yếu/trùng lặp | ⏳ Pending | — |
| 15 | Màn hình Settings (tham số bảo mật, theme, phương thức unlock) | ⏳ Pending | — |

## Ghi chú phụ thuộc

- 1 → 2, 3, 4: cần Vault/unlock cơ bản trước khi thêm phương thức mở khóa phụ và rate-limit.
- 1 → 5: cần Vault (SQLCipher DB) mở được trước khi có chỗ lưu Vault Item. Feature 5 mới thêm Room thật lên trên (Feature 1 chỉ dùng SQLCipher raw API để chứng minh luồng mã hóa/mở khóa hoạt động).
- 5 → 6, 7, 8, 9: cần entity Vault Item tồn tại trước khi search/tag/custom field/note gắn vào.
- 5 → 10, 11, 12: cần entity + repository Vault Item trước khi generator lưu được, import/export đọc/ghi được.
- 12 → 13: rotate backup cần cơ chế export `.pwvbackup` đã có trước.
- Toàn bộ: 14 cần dữ liệu Vault Item thật (từ 5) để so sánh trùng lặp/độ yếu.

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
