# Plan — Feature 16: UI/UX redesign (visual + tương tác)

## Cập nhật 2026-07-24 — thay thế phần "A. Visual" bên dưới

Bản gốc (2026-07-19, giữ nguyên bên dưới để tham khảo lịch sử quyết định) tham khảo ảnh ManageEngine PasswordManager Pro và chủ động loại bỏ việc đổi màu thương hiệu ("Theme.kt vẫn dùng `lightColorScheme()`/`darkColorScheme()` mặc định Material3, không hardcode màu").

2026-07-24: người dùng gửi bộ ảnh tham khảo khác (app "Password Manager" — thẻ danh sách, chip lọc theo danh mục, FAB tròn, banner Settings) kèm yêu cầu icon đẹp hơn. Đã duyệt bảng màu + icon + mockup 4 màn hình (List/Detail/Form/Settings) — xem `### A0.` bên dưới. **Bản này thay thế "A. Visual" gốc**: có đổi màu thương hiệu, không giữ Material3 mặc định nữa.

Trạng thái các phần còn lại của plan gốc (tất cả ✅ Done 2026-07-25, xem `## Status`):
- **B2 đã xong** — `TagColor.kt` + `TagChip.kt` dùng ở `VaultScreen.kt`/`VaultItemFormScreen.kt`/`TagManagerScreen.kt`, palette đã đổi sang 8 tông mới.
- **B1 đã xong** — `SecurityPolicy.GENERATED_PASSWORD_LENGTH` 20 → 8.
- **A (Unlock family — LockIconBadge + subtitle) đã xong** — khối tròn nền icon dùng token `brass`/`ink` qua `Theme.kt`, không phải `primaryContainer` mặc định.

### A0. Visual — bảng màu, icon, 4 màn hình (2026-07-24, thay thế "A. Visual" gốc)

**Bảng màu** (thêm vào `colors.xml` + dựng `ColorScheme` tuỳ biến trong `Theme.kt`, không dùng `lightColorScheme()`/`darkColorScheme()` trần nữa):

| Token | Light | Dark | Vai trò |
|---|---|---|---|
| Ink Chrome | `#1C2230` | `#1C2230` (cố định, không đổi theo theme) | Top app bar / banner Settings |
| Brass (accent chính) | `#C6903E` | `#D9A54B` | FAB, nút Save, chip đang chọn, icon nhấn trên chrome |
| Verdigris (phụ/success) | `#2F8F7E` | `#4CA89B` | Trạng thái tích cực (mật khẩu mạnh, đã copy) — không dùng làm accent chính |
| Danger | `#C1443A` | `#E2685D` | Cảnh báo mật khẩu yếu/trùng |
| Paper (bg) | `#F3F4F7` | `#14171F` | Nền màn hình |
| Surface / Surface-2 | `#FFFFFF` / `#ECEEF2` | `#1D212C` / `#262B38` | Thẻ / nền field |

Tag palette (`TagColor.kt`) tinh chỉnh lại 8 tông (Coral `#E0725A`, Marigold `#D2A23F`, Moss `#6B9080`, Teal `#4C93A6`, Indigo `#5B6FD9`, Plum `#8B5FA8`, Rose `#C1638A`, Slate `#7B8AA8`) — thay 8 tông Material 300 gốc, giữ nguyên cơ chế `tagId % palette.size`.

**Icon** — Concept A đã duyệt: nền gradient graphite (`#2A3040` → `#171A22`), khoá hình mề đay gradient đồng thau (`#F0C878` → `#B8792A`), lỗ khoá "khoét" màu nền tạo chiều sâu. Vẽ lại `ic_launcher_foreground.xml` + `ic_launcher_monochrome.xml`, đổi `ic_launcher_background`.

**4 màn hình** (mockup đã duyệt tại artifact phiên 2026-07-24):
- **List** (`VaultItemListScreen` trong `VaultScreen.kt`): top bar chrome cố định (title + gear icon), search pill bo tròn thay `OutlinedTextField` viền, hàng chip lọc theo Tag, đổi `Row` thẻ hiện tại sang `Card` (avatar tròn màu verdigris-tint, tên, username, password row ẩn dạng pill, tag chip, timestamp), FAB bo góc vuông (không phải tròn) dùng `brass`.
- **Detail** (`VaultItemDetailScreen.kt`): giữ đề xuất `DetailField` (label nhỏ phía trên value) từ plan gốc, áp token màu mới; banner cảnh báo mật khẩu yếu dùng token `danger`.
- **Form** (`VaultItemFormScreen.kt`): field có leading icon (globe/person/lock), chip chọn nhiều Tag (đã đúng model, chỉ style lại), nút Save full-width `brass`.
- **Settings** (`SettingsScreen.kt`): thêm banner thương hiệu ở đầu (gradient ink + mark khoá đồng thau + tên app + tagline), giữ nguyên cấu trúc section/row hiện có, chỉ style lại.

Không đổi kiến trúc điều hướng (Settings vẫn full-screen `TextButton` back, không thêm drawer trượt).

### Phase 2 (làm sau khi Phase 1 lên máy thật) — Responsive cho tablet

Theo yêu cầu người dùng (2026-07-24): mở rộng bố cục cho tablet **sau khi** Phase 1 (bảng màu + icon + 4 màn hình trên) đã triển khai và duyệt trên điện thoại. Chưa thiết kế chi tiết ở bước này — chỉ ghi nhận thứ tự. Khi tới lượt, cần ít nhất: layout 2-pane (List + Detail cạnh nhau khi width class Expanded, dùng `WindowSizeClass`), kiểm tra lại spacing/max-width của Form và Settings trên màn rộng, và chip/card không kéo giãn full-width một cách xấu xí trên tablet.

## Code-plan 2026-07-24 — Phase 1 (chi tiết triển khai)

*File location: giữ trong `docs/plans/` của repo này, không chuyển sang `kit/context/` — vì
15 feature trước đều lưu ở đây, `roadmap.md` và code đang trỏ tới đường dẫn này. `common.md` §9
(bản mới nhất) cũng yêu cầu lưu trong repo dự án, dưới `docs/` — khớp với cách làm hiện tại.*

### Goal

- Áp bảng màu + icon mới đã duyệt vào code thật, style lại 4 màn hình (List/Detail/Form/Settings).
- Thêm khối icon tròn cho 4 màn Unlock, rút độ dài mật khẩu sinh tự động 20 → 8 ký tự.

### Scope

#### In Scope

- `colors.xml` + `Theme.kt`: bảng màu ink/brass/verdigris/danger tuỳ biến, thay cho Material3 mặc định.
- `TagColor.kt`: đổi 8 màu tag sang bảng mới.
- Vẽ lại icon app (`ic_launcher_foreground.xml`, `ic_launcher_monochrome.xml`, màu nền).
- `LockIconBadge` mới, áp cho 4 màn Unlock (Setup/Unlock/PinUnlock/Biometric) + thêm subtitle.
- Style lại List, Detail, Form, Settings theo mockup đã duyệt (chỉ đổi giao diện, không đổi logic).
- `SecurityPolicy.GENERATED_PASSWORD_LENGTH`: 20 → 8.
- Thêm bộ lọc theo Tag ở màn List (xem mục Questions — đây là hành vi mới, không chỉ giao diện).

#### Out of Scope

- Responsive cho tablet (Phase 2, làm sau khi Phase 1 lên máy thật và được duyệt).
- Đổi kiến trúc điều hướng (Settings vẫn full-screen, không thêm drawer trượt).
- Đổi model dữ liệu Tag/VaultItem, đổi schema DB.
- Loại ký tự dễ nhầm hoặc cho tuỳ chỉnh độ dài mật khẩu sinh tự động (đã out of scope từ bản gốc).

### Flow

1. App khởi động: `MainActivity` đọc `ThemeMode` đã lưu → `PwVaultTheme` dựng `ColorScheme` mới.
2. User chưa unlock: mở Setup/Unlock/PinUnlock/Biometric → thấy `LockIconBadge` + subtitle mới.
3. User vào màn List: `VaultViewModel` phát danh sách + tag → UI hiện top bar, ô tìm kiếm, chip lọc
   tag, `Card` cho mỗi item, FAB — chọn chip tag sẽ lọc lại danh sách hiển thị.
4. User mở 1 item: `VaultItemDetailScreen` hiện từng trường qua `DetailField` (label + value).
5. User thêm/sửa item: `VaultItemFormScreen` hiện field có icon, chọn Tag bằng chip (logic cũ).
6. User mở Settings: banner thương hiệu mới ở đầu, các mục bên dưới giữ nguyên logic đọc/ghi.
7. User bấm "Generate password": `PasswordGenerator` sinh đúng 8 ký tự, vẫn đủ 4 loại ký tự.

### Files

- `app/src/main/res/values/colors.xml`
- `app/src/main/java/com/pwvault/app/ui/theme/Theme.kt`
- `app/src/main/java/com/pwvault/app/ui/theme/TagColor.kt`
- `app/src/main/res/drawable/ic_launcher_foreground.xml`
- `app/src/main/res/drawable/ic_launcher_monochrome.xml`
- `app/src/main/java/com/pwvault/app/ui/unlock/LockIconBadge.kt` (mới)
- `app/src/main/java/com/pwvault/app/ui/unlock/SetupScreen.kt`
- `app/src/main/java/com/pwvault/app/ui/unlock/UnlockScreen.kt`
- `app/src/main/java/com/pwvault/app/ui/unlock/PinUnlockScreen.kt`
- `app/src/main/java/com/pwvault/app/ui/unlock/BiometricUnlockScreen.kt`
- `app/src/main/java/com/pwvault/app/ui/vault/VaultViewModel.kt` (thêm state lọc theo Tag ở List)
- `app/src/main/java/com/pwvault/app/ui/vault/VaultScreen.kt`
- `app/src/main/java/com/pwvault/app/ui/vault/VaultItemDetailScreen.kt`
- `app/src/main/java/com/pwvault/app/ui/vault/VaultItemFormScreen.kt`
- `app/src/main/java/com/pwvault/app/ui/settings/SettingsScreen.kt`
- `app/src/main/java/com/pwvault/app/security/SecurityPolicy.kt`
- `app/src/main/res/values/strings.xml` + `app/src/main/res/values-vi/strings.xml`
- `docs/plans/roadmap.md` (cập nhật trạng thái sau khi xong)

### Implementation Steps

1. `colors.xml`: thêm token màu ink/brass/verdigris/danger/paper/surface (light + dark).
2. `Theme.kt`: dựng `ColorScheme` từ token mới thay vì `lightColorScheme()`/`darkColorScheme()` trần.
3. `TagColor.kt`: đổi 8 giá trị màu, giữ nguyên hàm `tagColor()`.
4. Vẽ lại icon app theo Concept A đã duyệt (khoá gradient đồng thau trên nền graphite).
5. Viết `LockIconBadge.kt`, áp vào 4 màn Unlock, thêm subtitle mới cho Unlock/PinUnlock/Biometric.
6. `VaultViewModel.kt`: thêm `availableTags`/`selectedTagIds` vào `ItemList`, thêm hàm lọc theo tag,
   cập nhật `currentListState()` lọc theo tag đã chọn (giữ nguyên lọc theo search hiện có).
7. `VaultScreen.kt`: top bar cố định, ô tìm kiếm dạng pill, hàng chip lọc Tag, đổi `Row` sang `Card`.
8. `VaultItemDetailScreen.kt`: viết `DetailField`, thêm field "Last updated" từ `item.updatedAt`.
9. `VaultItemFormScreen.kt`: thêm leading icon cho field, style nút Save (chỉ đổi giao diện).
10. `SettingsScreen.kt`: thêm banner thương hiệu ở đầu, giữ nguyên toàn bộ logic bên dưới.
11. `SecurityPolicy.kt`: đổi `GENERATED_PASSWORD_LENGTH` = 8.
12. Thêm string mới (en + vi) cho subtitle Unlock, label "Last updated", nhãn chip lọc.
13. Build + verify: `ktlintCheck detekt lint testDebugUnitTest assembleDebug`.
14. Verify sống trên máy thật: so từng màn với mockup, thử unlock 3 cách, CRUD, tìm kiếm, lọc theo
    tag, generate password (đúng 8 ký tự, đủ 4 loại), đổi theme Light/Dark/System.
15. `/code-guard` + `/code-review`, sửa hết lỗi mức Critical/Major trước khi commit.
16. Cập nhật `roadmap.md` Feature 16 = Done, cập nhật mục Status trong file này.
17. 1 commit.

### Risks

- Đổi màu toàn app là thay đổi lớn, dễ va vào rất nhiều màn hình — cần xem kỹ từng màn trên máy
  thật trước khi commit, không chỉ tin vào build xanh.
- Thêm bộ lọc theo Tag ở màn List là hành vi mới (không chỉ đổi giao diện) — nếu lọc sai sẽ làm
  người dùng tưởng mất dữ liệu (item vẫn còn, chỉ đang bị ẩn do lọc nhầm) — cần test kỹ.
- Vẽ lại icon app có thể bị hệ thống Android cache icon cũ — cần gỡ cài lại app khi test trên máy
  thật để chắc chắn thấy icon mới, không nhầm là icon chưa đổi được.
- Đổi 4 màn Unlock cùng lúc — đây là màn đầu tiên người dùng thấy mỗi lần mở app, lỗi ở đây chặn
  luôn cả luồng dùng app, nên test luồng mở khóa kỹ trước khi commit.

### Questions

*(đã đề xuất phương án — chỉ hỏi lại nếu muốn đổi)*

- **Mockup có chip lọc theo Tag ở màn List, nhưng code hiện tại chưa có logic lọc này (chỉ có ở màn
  Thêm/sửa) — có làm luôn phần lọc thật, hay chỉ hiện chip cho đẹp mà chưa lọc?** 👉 Đề xuất: làm
  luôn phần lọc thật (mục 6 ở Implementation Steps) — vì hiện chip mà bấm không có tác dụng gây
  khó hiểu cho người dùng hơn là không hiện chip.

---

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

### Out of Scope (bản gốc 2026-07-19 — đã bị thay thế, xem `### A0.` ở trên)

- ~~Đổi màu thương hiệu (header xanh như ảnh) — `Theme.kt` vẫn dùng `lightColorScheme()`/`darkColorScheme()` mặc định Material3, không hardcode màu.~~ **Đã đổi quyết định 2026-07-24** — nay có bảng màu thương hiệu riêng, xem `### A0.`. Theme picker (light/dark/system) ở Settings (#15) vẫn giữ nguyên, chỉ áp thêm token màu mới lên cả 2 chế độ.
- Số đếm bên phải mỗi row trong ảnh Favorites (vd "31", "2") — không có dữ liệu tương ứng trong model phẳng hiện tại (pwvault không có khái niệm "resource chứa nhiều account con"), không fabricate số liệu giả.
- Hiển thị Tag ở Detail — `VaultItem.tags` tồn tại trong model nhưng luôn rỗng tới khi Feature 7 (Tag management) xong; nên làm cùng/sau Feature 7, không phải ở đây dù cùng đụng Detail screen.
- Đổi tương tác search thành icon-mở-rộng trong TopAppBar (xem mục List ở trên).
- Hành vi lỗi/focus của form Unlock — vẫn là mục 5b riêng, độc lập, không gộp.
- ~~Settings screen (theme picker, offline toggle, clear data...) trong ảnh — roadmap #15 riêng, chưa tới lượt.~~ **Đã đổi 2026-07-24** — #15 nay đã Done (Material3 mặc định, chưa có style riêng), Settings được style lại trong Feature 16 theo `### A0.` (banner thương hiệu + token màu mới), không đổi cấu trúc section/logic.

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

- **Avatar theo chữ cái đầu tên hay theo loại item?** 👉 **Đổi 2026-07-24**: theo loại item (icon Person/Description hiện có trong `VaultItemListScreen`, bọc trong khối tròn tint màu verdigris) — Feature 9 (Note) đã xong nên phân biệt Login/Note theo icon đã có ý nghĩa, không cần đổi sang chữ cái đầu nữa.
- **"Rule cơ bản" cho password generator nghĩa là gì?** 👉 ≥1 chữ hoa + ≥1 chữ thường + ≥1 số + ≥1 ký tự đặc biệt (rule phức tạp mật khẩu kiểu phổ biến/OWASP-style cơ bản) — không đổi bộ ký tự đặc biệt hiện có, không loại ký tự dễ nhầm (chưa yêu cầu).

---

## Status

✅ Phase 1 xong (2026-07-25) — bảng màu ink/brass/verdigris/danger, icon app mới (Concept A), style lại List/Detail/Form/Settings, `LockIconBadge` + subtitle cho 4 màn Unlock, lọc theo Tag thật (không chỉ hiện chip), `GENERATED_PASSWORD_LENGTH` 20→8. B2 giữ nguyên (đã xong trước đó). `/code-guard` + `/code-review` sạch (1 vi phạm màu hardcode đã sửa, xem `docs/code-review/feature-16-ui-ux-redesign.md`). Build xanh (ktlint/detekt/lint/assembleDebug); repo không có unit test nào để chạy.

2 lỗi phát hiện trong lúc hoàn thiện, đã sửa trước commit:
- **Critical:** `VaultFileManager` còn `fallbackToDestructiveMigration(dropAllTables = true)` — sẽ xoá sạch vault thật trên máy đã cài `v0.1.0` ở lần bump version kế tiếp. Bỏ destructive fallback; thiếu migration giờ sẽ crash rõ ràng thay vì âm thầm mất dữ liệu.
- **Major:** nhánh làm dở đã xoá toàn bộ tính năng "lưu template mật khẩu tái dùng" (Feature 10) mà không ghi vào plan nào. Khôi phục lại đầy đủ (DAO/Entity/Repository/ViewModel/dialog UI/strings), DB giữ nguyên version 5.

Phase 2 (tablet responsive) làm sau, sau khi Phase 1 lên máy thật và được duyệt.
