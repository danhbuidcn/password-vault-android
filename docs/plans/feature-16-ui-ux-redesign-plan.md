# Plan — Feature 16: UI/UX redesign (visual + tương tác)

## Goal

- Cải thiện cả **visual** (bố cục, style) lẫn **UX/tương tác** (hành vi, mức độ dễ dùng) của app — không giới hạn ở "chỉ đổi giao diện, giữ nguyên logic" như phạm vi ban đầu.
- Gồm 2 nguồn: (a) tham khảo UI ManageEngine PasswordManager Pro (ảnh người dùng cung cấp 2026-07-19) cho phần visual của Unlock/Vault List/Item Detail; (b) các góp ý UX phát sinh trực tiếp từ người dùng trong quá trình dùng app, gộp chung vào đây thay vì làm rải rác từng cái.
- Theo quyết định người dùng: triển khai **sau cùng**, khi tất cả tính năng 1-15 đã xong — xem `roadmap.md`. **Quy ước:** mọi góp ý UI/UX phát sinh giữa lúc làm các tính năng khác sẽ được ghi thêm vào đây (chỉ cập nhật doc, không code ngay), trừ khi người dùng nói rõ muốn làm ngay.

---

## A. Visual — tham khảo ảnh (Unlock + Vault List + Item Detail)

Pattern rút ra từ từng ảnh tham khảo (ManageEngine PasswordManager Pro):

- **"Favorites" (list):** icon tròn đầu mỗi row, title + dòng mô tả phụ bên dưới, có thanh tiêu đề (top bar) riêng thay vì trôi tự do.
- **"Account Details" (x2):** mỗi field có label nhỏ (xám, cỡ chữ nhỏ) phía trên value — không dồn các giá trị vào các dòng `Text` liền nhau không phân biệt.
- **"Personal passphrase" (unlock):** icon khóa đặt trong khối tròn nền màu nhạt ở giữa màn hình, tiêu đề, 1 field, dòng giải thích phụ bên dưới field.
- **Ảnh Settings + Login server config:** không dùng — Settings là roadmap #15 riêng (chưa tới lượt); màn login server config không áp dụng (pwvault không có server, hoàn toàn offline).

### Scope

**1. Unlock family** (`SetupScreen`/`UnlockScreen`/`PinUnlockScreen`/`BiometricUnlockScreen`):

- Icon khóa/vân tay bọc trong khối tròn nền màu (`Surface` + `CircleShape` + `colorScheme.primaryContainer`) thay vì `Icon` trần như hiện tại — tách 1 composable dùng chung (`LockIconBadge`) vì lặp lại y hệt ở cả 4 màn.
- Thêm dòng subtitle giải thích ngắn dưới tiêu đề cho `UnlockScreen`/`PinUnlockScreen`/`BiometricUnlockScreen` (`SetupScreen` đã có `setup_subtitle` sẵn, dùng làm mẫu) — văn phong "plain language" đã dùng từ Feature 1 (tránh thuật ngữ kỹ thuật).
- Không đổi hành vi lỗi/focus (giữ field khi lỗi, auto-focus...) — đó là scope riêng của mục 5b (đang pending), không gộp vào đây.

**2. Vault List** (`VaultItemListScreen` trong `VaultScreen.kt`):

- Leading avatar tròn = chữ cái đầu `item.name`, màu nền suy ra tất định từ hash tên (không thêm icon-per-type, vì Vault Item hiện chỉ có 1 loại "Login" — loại Note là Feature 9, chưa tới).
- Bọc phần trên (nút setup PIN/sinh trắc học nếu thiếu, ô tìm kiếm) dưới 1 `TopAppBar` (tiêu đề app) thay vì `Column` trần không có thanh tiêu đề — khớp bố cục có header của ảnh Favorites.
- Giữ nguyên ô tìm kiếm luôn hiện (đã có từ Feature 6) — không đổi thành icon-mở-rộng như ảnh, vì tăng phức tạp tương tác mà không có lợi ích rõ.

**3. Vault Item Detail** (`VaultItemDetailScreen.kt`):

- Đổi từ các `Text` rời rạc sang pattern "label nhỏ phía trên value" cho từng field — tách 1 composable dùng chung `DetailField(label, value)` (đang lặp lại gần y hệt 4-5 lần trong file hiện tại → gộp hợp lý, không phải abstraction sớm vì pattern đã lặp sẵn).
- Thêm field mới hiển thị `item.updatedAt` (đã có sẵn trong `VaultItem`, domain model có nhưng chưa hiển thị ở đâu) dạng label "Last updated", format theo locale — tương ứng "Last Accessed Time" trong ảnh mẫu, dùng đúng dữ liệu đã có, không thêm cột DB mới.
- Giữ nguyên nút eye-toggle + copy cho password — chỉ đổi layout bọc ngoài.

### Out of Scope

- Đổi màu thương hiệu (header xanh như ảnh) — `Theme.kt` vẫn dùng `lightColorScheme()`/`darkColorScheme()` mặc định Material3, không hardcode màu. Theme picker là đúng phạm vi Settings (#15).
- Số đếm bên phải mỗi row trong ảnh Favorites (vd "31", "2") — không có dữ liệu tương ứng trong model phẳng hiện tại (pwvault không có khái niệm "resource chứa nhiều account con"), không fabricate số liệu giả.
- Hiển thị Tag ở Detail — `VaultItem.tags` tồn tại trong model nhưng luôn rỗng tới khi Feature 7 (Tag management) xong; nên làm cùng/sau Feature 7, không phải ở đây dù cùng đụng Detail screen.
- Đổi tương tác search thành icon-mở-rộng trong TopAppBar (xem mục List ở trên).
- Hành vi lỗi/focus của form Unlock — vẫn là mục 5b riêng, độc lập, không gộp.
- Settings screen (theme picker, offline toggle, clear data...) trong ảnh — roadmap #15 riêng, chưa tới lượt.

---

## B. UX — góp ý trực tiếp từ người dùng

### B1. Password generator: rút ngắn + đơn giản hóa (2026-07-19)

**Vấn đề:** mật khẩu random hiện tại (`PasswordGenerator.kt`) dài 20 ký tự, random thuần từ tập 74 ký tự (`ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%^&*-_=+`) — khó nhớ, khó gõ tay khi cần nhập thủ công (vd đăng nhập trên thiết bị/trình duyệt khác không paste được).

**Đề xuất:**
- Rút `SecurityPolicy.GENERATED_PASSWORD_LENGTH` từ `20` xuống `8`.
- Đảm bảo tuân thủ rule cơ bản thay vì random thuần: bắt buộc ≥1 chữ hoa, ≥1 chữ thường, ≥1 số, ≥1 ký tự đặc biệt trong 8 ký tự (random thuần hiện tại không đảm bảo — có xác suất nhỏ ra toàn chữ thường hoặc thiếu hẳn 1 loại). Sinh 4 ký tự bắt buộc (mỗi loại 1) + 4 ký tự còn lại random từ toàn bộ charset, rồi trộn ngẫu nhiên vị trí (Fisher–Yates bằng `SecureRandom` đang dùng, không dùng `List.shuffled()` mặc định vì nó nhận `kotlin.random.Random` chứ không nhận `SecureRandom`) — tránh việc 4 ký tự bắt buộc luôn nằm ở đầu (dễ đoán).
- Giữ nguyên `SecureRandom`, giữ nguyên bộ ký tự đặc biệt hiện tại (đã là các ký tự phổ biến, dễ gõ, không đổi thêm gì ở đây).

**Out of scope (chưa yêu cầu):** loại bỏ ký tự dễ nhầm (`0`/`O`, `1`/`l`/`I`), cho phép tùy chỉnh độ dài/rule trong lúc tạo (vd slider), lưu template tái dùng — đó là roadmap #10 riêng ("Sinh mật khẩu ngẫu nhiên + lưu template tái dùng").

### Files (khi triển khai B1)

- `security/SecurityPolicy.kt` — `GENERATED_PASSWORD_LENGTH = 20` → `8`.
- `security/PasswordGenerator.kt` — tách `CHARSET` thành 4 nhóm (upper/lower/digit/special), đảm bảo ≥1 ký tự mỗi nhóm, trộn vị trí bằng Fisher–Yates thủ công với `SecureRandom`.
- Test unit (nếu có `PasswordGeneratorTest`, chưa kiểm tra lúc viết doc này — kiểm tra lại khi tới lượt triển khai): cập nhật assertion độ dài nếu có hardcode `20`.

### B2. Tag: màu tự động riêng biệt + hiện ngay ở danh sách (2026-07-19)

**Bối cảnh:** Feature 7 (Tag) tại thời điểm ghi chú này đã có sẵn CRUD Tag + gắn nhiều Tag/1 Vault Item (many-to-many qua `vault_item_tag_cross_ref`, đã đúng yêu cầu) + hiện Tag ở màn chi tiết. Plan gốc của Feature 7 (`feature-07-tag-plan.md`) cố tình để "hiện Tag ở danh sách" ngoài phạm vi. Theo góp ý mới của người dùng, 2 việc sau gộp vào đây thay vì làm trong Feature 7:

- **Mỗi Tag tự động có 1 màu riêng biệt** — suy ra tất định từ `tag.id` qua 1 palette cố định (`id % palette.size`), không lưu cột màu trong DB, không có UI chọn màu thủ công. Hiện dưới dạng chấm tròn nhỏ (`leadingIcon`) trong mọi chip Tag thay vì tô cả nền chip — tránh phải tự tính màu chữ tương phản trên nền màu bất kỳ.
- **Hiện Tag ngay trên từng dòng ở danh sách Vault Item** (`VaultItemListScreen`) — dùng cùng chip Tag (dùng chung với màn chi tiết) thay vì chỉ hiện ở Detail như phạm vi gốc của Feature 7.

**Files (khi triển khai B2):**
- `ui/theme/TagColor.kt` (mới) — palette `Color` cố định + hàm `tagColor(tagId: Long): Color`.
- `ui/vault/TagChip.kt` (mới) — `TagColorDot(tagId)` (chấm tròn nhỏ) + `TagChip(tag)` (`AssistChip` dùng chung), dùng lại ở cả `VaultItemDetailScreen` và `VaultItemListScreen`; `TagColorDot` cũng gắn vào `FilterChip` trong `VaultItemFormScreen` và dòng Tag trong `TagManagerScreen` để nhất quán màu ở mọi nơi Tag xuất hiện.
- `ui/vault/VaultItemDetailScreen.kt` — thay khối `AssistChip` hiện có bằng `TagChip` dùng chung.
- `ui/vault/VaultItemFormScreen.kt` — `FilterChip` hiện có thêm `leadingIcon = { TagColorDot(tag.id) }`.
- `ui/vault/VaultScreen.kt` — mỗi dòng trong `LazyColumn` của `VaultItemListScreen` thêm hàng `TagChip` nếu `item.tags` không rỗng.
- `ui/vault/TagManagerScreen.kt` — mỗi dòng Tag thêm `TagColorDot` trước tên.

---

## Files (tổng hợp, cả A + B)

**Mới:**
- `ui/unlock/LockIconBadge.kt` — composable icon tròn dùng chung cho 4 màn Unlock.

**Sửa:**
- `ui/unlock/{SetupScreen,UnlockScreen,PinUnlockScreen,BiometricUnlockScreen}.kt` — dùng `LockIconBadge`; thêm subtitle cho 3 màn chưa có.
- `ui/vault/VaultScreen.kt` — `VaultItemListScreen` thêm `TopAppBar`, avatar chữ cái đầu cho mỗi row.
- `ui/vault/VaultItemDetailScreen.kt` — thêm `DetailField` composable dùng chung, thêm field "Last updated" từ `item.updatedAt`.
- `security/SecurityPolicy.kt` — `GENERATED_PASSWORD_LENGTH` 20 → 8.
- `security/PasswordGenerator.kt` — đảm bảo rule cơ bản (xem B1).
- `ui/vault/TagManagerScreen.kt` — thêm `TagColorDot` trước tên mỗi Tag (xem B2).
- `ui/vault/VaultItemFormScreen.kt` — `FilterChip` Tag thêm `TagColorDot` (xem B2).
- `values/strings.xml` + `values-vi/strings.xml` — subtitle mới cho Unlock/PinUnlock/Biometric, label "Last updated", tiêu đề TopAppBar của list.
- `docs/plans/roadmap.md` — cập nhật trạng thái Feature 16 sau khi xong.

**Mới (bổ sung B2):**
- `ui/theme/TagColor.kt`, `ui/vault/TagChip.kt` — xem B2.

---

## Implementation Steps

1. `LockIconBadge.kt`: composable nhận `icon: ImageVector`, render `Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer)` bọc `Icon` bên trong.
2. Áp `LockIconBadge` vào 4 màn Unlock family, thay `Icon` trần hiện tại.
3. Thêm string subtitle mới (en + vi) cho Unlock/PinUnlock/Biometric, hiển thị dưới title theo đúng vị trí `SetupScreen` đang làm.
4. `VaultScreen.kt`: `Scaffold` hiện có (đang dùng cho FAB) thêm `topBar = { TopAppBar(title = ...) }`; thêm avatar chữ cái đầu (`Surface` tròn + `Text` initial) trước `Column` tên/username mỗi row trong `LazyColumn`.
5. `VaultItemDetailScreen.kt`: viết `DetailField(label: String, value: String)` private composable; thay các `Text` rời hiện tại (name/username/password-row/url/note) bằng `DetailField`; thêm field mới "Last updated" từ `item.updatedAt`.
6. `SecurityPolicy.kt`: đổi `GENERATED_PASSWORD_LENGTH` = 8.
7. `PasswordGenerator.kt`: tách charset 4 nhóm, sinh đảm bảo rule cơ bản, trộn vị trí bằng Fisher–Yates thủ công (xem B1).
8. `TagColor.kt`, `TagChip.kt` (mới); áp `TagChip`/`TagColorDot` vào `VaultItemDetailScreen.kt`, `VaultItemListScreen` (trong `VaultScreen.kt`), `VaultItemFormScreen.kt`, `TagManagerScreen.kt` (xem B2).
9. Build + verify: `./gradlew ktlintCheck detekt lint testDebugUnitTest assembleDebug`.
10. Verify sống trên thiết bị/emulator: so từng màn hình với ảnh tham khảo; bấm "Generate password" nhiều lần xác nhận luôn đủ ≥1 chữ hoa/thường/số/ký tự đặc biệt và đúng 8 ký tự; xác nhận mỗi Tag có màu riêng nhất quán ở mọi nơi (list/detail/form/Tag Manager) và Tag hiện đúng ở từng dòng danh sách; xác nhận không phá luồng nghiệp vụ hiện có (unlock bằng password/PIN/sinh trắc học, xem/sửa/xóa Vault Item, tìm kiếm, setup PIN/sinh trắc học từ màn List).
11. `/code-guard` + `/code-review`, sửa hết Critical/Major trước khi commit.
12. Cập nhật `roadmap.md` trạng thái Feature 16 = Done.
13. 1 commit (hoặc tách nhiều commit theo A/B1/B2 nếu review thấy diff quá lớn gộp chung — quyết định lúc triển khai).

---

## Risks

- Phần A (visual): thay đổi thuần UI, không đụng schema DB hay luồng mã hóa/mở khóa — rủi ro thấp. `TopAppBar` cần bọc đúng trong `Scaffold` đã có sẵn (đang dùng cho FAB) — kiểm tra không phá `floatingActionButton`. Áp dụng cùng lúc cho cả 4 màn Unlock — lỗi layout ảnh hưởng trực tiếp luồng mở khóa chính, verify sống kỹ trước khi commit.
- Phần B1 (password generator): giảm độ dài 20 → 8 làm giảm entropy (từ ~124 bit xuống ~49.7 bit trên bộ 74 ký tự) — vẫn đủ mạnh cho mật khẩu từng tài khoản riêng lẻ (không phải Master Password của chính pwvault — cái đó không đổi), đây là đánh đổi bảo mật lấy khả năng gõ tay theo đúng yêu cầu người dùng, không phải sơ suất.

---

## Questions

*(đã đề xuất phương án — chỉ hỏi lại nếu muốn đổi)*

- **Avatar theo chữ cái đầu tên hay theo loại item?** 👉 Chữ cái đầu — Vault Item hiện chỉ có 1 loại ("Login"), phân loại theo type chưa có ý nghĩa tới khi Feature 9 (Note) xong.
- **"Rule cơ bản" cho password generator nghĩa là gì?** 👉 ≥1 chữ hoa + ≥1 chữ thường + ≥1 số + ≥1 ký tự đặc biệt (rule phức tạp mật khẩu kiểu phổ biến/OWASP-style cơ bản) — không đổi bộ ký tự đặc biệt hiện có, không loại ký tự dễ nhầm (chưa yêu cầu).

---

## Status

⏳ Chưa triển khai — theo quyết định người dùng (2026-07-19), làm sau khi tất cả tính năng 1-15 trong roadmap hoàn tất. Doc này ghi lại scope (visual + UX) trước, để không mất ngữ cảnh khi tới lượt triển khai. Mọi góp ý UI/UX mới trong lúc làm các feature khác sẽ được thêm tiếp vào mục B của doc này.
