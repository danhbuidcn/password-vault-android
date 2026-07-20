# Plan — Feature 14: Cảnh báo mật khẩu yếu/trùng lặp

## Goal

- Cảnh báo trực quan khi password của 1 Vault Item (loại Login) yếu hoặc trùng với item khác — `functional-spec.md §8`.

---

## Scope

### In Scope

- Heuristic "yếu": password rỗng loại trừ (Note không có password); còn lại yếu nếu ngắn hơn ngưỡng hoặc không đủ đa dạng loại ký tự (chữ hoa/thường/số/ký tự đặc biệt) — `security/PasswordStrength.kt`, ngưỡng ở `SecurityPolicy.kt`.
- "Trùng lặp": 2+ item có cùng password không rỗng.
- Hiển thị: icon cảnh báo nhỏ ở dòng danh sách (list row) cho item yếu/trùng; dòng chữ cảnh báo ngắn ở màn Chi tiết, ngay dưới password.
- Tính lại mỗi khi danh sách item thay đổi (tái dùng đúng chỗ `latestItems` đang được cập nhật trong `VaultViewModel`).

### Out of Scope

- Kiểm tra password có trong danh sách rò rỉ (offline breach database) — spec đánh dấu "tùy chọn", cần thêm 1 bộ dữ liệu offline lớn, không nằm trong tên Feature 14 ở `roadmap.md`. Để riêng, chưa gán feature.
- Màn "Security Audit" tổng hợp riêng — cảnh báo tại chỗ (list + detail) đã đáp ứng yêu cầu spec, thêm màn riêng là mở rộng phạm vi chưa được yêu cầu.
- Cho phép người dùng tùy chỉnh ngưỡng "yếu" — chưa có Settings (Feature 15); dùng ngưỡng cố định trong `SecurityPolicy.kt`.

---

## Files

**Mới:**
- `security/PasswordStrength.kt` — `fun isWeak(password: String): Boolean` (rỗng → false/không tính; ngắn hơn `SecurityPolicy.WEAK_PASSWORD_MIN_LENGTH` hoặc dưới `SecurityPolicy.WEAK_PASSWORD_MIN_CHAR_CLASSES` loại ký tự trong 4 loại upper/lower/digit/special → yếu).

**Sửa:**
- `security/SecurityPolicy.kt` — thêm `WEAK_PASSWORD_MIN_LENGTH = 8`, `WEAK_PASSWORD_MIN_CHAR_CLASSES = 3`.
- `ui/vault/VaultViewModel.kt` — thêm `data class VaultItemWarning(val weak: Boolean, val duplicate: Boolean)`; field `latestWarnings: Map<Long, VaultItemWarning>` tính lại cùng lúc với `latestItems` trong `observeItems().collect`; `ItemList` state thêm `warnings: Map<Long, VaultItemWarning>`; `ItemDetail` state thêm `warning: VaultItemWarning`; `currentListState()`/`openDetail()` gắn giá trị tương ứng.
- `ui/vault/VaultScreen.kt` — dòng danh sách: icon `WarningAmber` (tint `colorScheme.error`) nếu `warnings[item.id]?.hasWarning == true`.
- `ui/vault/VaultItemDetailScreen.kt` — dưới khối password (chỉ khi `type == LOGIN`): dòng chữ "Weak password"/"Used by another item" (màu `colorScheme.error`) nếu `state.warning.weak`/`.duplicate`.
- `values/strings.xml` + `values-vi/strings.xml` — string cảnh báo mới.
- `docs/plans/roadmap.md` — cập nhật trạng thái Feature 14.

---

## Implementation Steps

1. `security/SecurityPolicy.kt`: thêm 2 hằng số ngưỡng.
2. `security/PasswordStrength.kt`.
3. `ui/vault/VaultViewModel.kt`: `VaultItemWarning`, tính `latestWarnings` cùng `latestItems`, gắn vào `ItemList`/`ItemDetail`.
4. `ui/vault/VaultScreen.kt`: icon cảnh báo ở list row.
5. `ui/vault/VaultItemDetailScreen.kt`: dòng cảnh báo ở Chi tiết.
6. i18n string mới (en + vi).
7. Build + verify: `ktlintCheck detekt lint testDebugUnitTest assembleDebug`.
8. Verify sống: tạo 2 item Login cùng password ngắn (vd "abc123") → cả 2 hiện icon cảnh báo ở list, mở chi tiết thấy đủ 2 dòng "Weak"+"Used by another item"; đổi 1 item sang password dài đủ mạnh và khác → icon biến mất đúng item đó, item còn lại chỉ còn cảnh báo yếu (không còn trùng); item Note không bao giờ hiện cảnh báo.
9. `/code-guard` + `/code-review`, sửa hết Critical/Major trước khi commit.
10. Cập nhật `roadmap.md` Feature 14 = Done.
11. 1 commit.

---

## Risks

- Heuristic yếu là cố định, không cấu hình được cho tới khi có Settings (Feature 15) — chấp nhận được, đúng phạm vi đã chốt ở Out of Scope.
- Tính `latestWarnings` là O(n) theo số item (dùng `groupingBy` đếm password trùng), không phải O(n²) — không đáng lo với quy mô Vault cá nhân.

---

## Questions

*(đã đề xuất phương án — chỉ hỏi lại nếu muốn đổi)*

- **Ngưỡng "yếu" cụ thể là gì?** 👉 Ngắn hơn 8 ký tự HOẶC thiếu đa dạng ký tự (dưới 3/4 loại upper/lower/digit/special) — cùng độ dài tối thiểu đã dùng cho Master Password (`MIN_PASSWORD_LENGTH`), số 3/4 loại đủ chặt để bắt các password kiểu "password123" (chỉ 2 loại: lower+digit) mà không quá khắt khe với password ngẫu nhiên thật.

---

## Status

✅ Hoàn tất. Build xanh (`ktlintCheck detekt lint testDebugUnitTest assembleDebug`). `/code-guard` + `/code-review` không có Critical/Major. 1 điều chỉnh nhỏ so với plan gốc: tách khối cảnh báo trong `VaultItemDetailScreen` thành composable riêng `PasswordWarnings` để giữ cyclomatic complexity dưới ngưỡng detekt, không cần đổi config.
