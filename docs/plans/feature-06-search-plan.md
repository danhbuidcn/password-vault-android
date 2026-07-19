# Plan — Feature 6: Tìm kiếm Vault Item

## Goal

- Cho phép lọc danh sách Vault Item theo tên hoặc username, ngay trên màn hình danh sách hiện có — `functional-spec.md §5`, dòng "Tìm kiếm theo tên/username".

---

## Scope

### In Scope

- Ô tìm kiếm ở đầu danh sách (`VaultItemListScreen`), lọc theo `name`/`username`, không phân biệt hoa/thường.
- Lọc trong bộ nhớ trên `latestItems` đã có sẵn trong `VaultViewModel` (không thêm query Room mới) — dữ liệu 1 người dùng, số lượng Vault Item nhỏ, lọc Kotlin `filter` là đủ, không cần lọc ở tầng SQL.
- Giữ nguyên chuỗi tìm kiếm khi quay lại danh sách từ màn chi tiết/form (trải nghiệm liền mạch, không mất trạng thái tìm kiếm).
- Phân biệt 2 trạng thái rỗng: chưa có Vault Item nào (string cũ `vault_empty_state`) và có Vault Item nhưng không khớp tìm kiếm (string mới).

### Out of Scope

- Tìm theo Tag/Custom Field/Note (chưa tồn tại, Feature 7/8/9).
- Lọc ở tầng SQL (`LIKE` query trong DAO) — không cần thiết ở quy mô dữ liệu hiện tại, có thể đổi sau nếu dữ liệu lớn hơn nhiều (không có tín hiệu cho thấy cần bây giờ).
- Debounce — lọc thuần Kotlin trên list đã có sẵn trong bộ nhớ, không có I/O, không cần debounce.

---

## Files

**Sửa:**
- `ui/vault/VaultViewModel.kt` — thêm field `searchQuery: String`, hàm `updateSearchQuery(query: String)`; `VaultUiState.ItemList` thêm field `searchQuery: String = ""`; hàm dùng chung `currentListState()` (lọc `latestItems` theo `searchQuery` hiện tại) dùng ở cả Flow collector (`startObservingItems`) lẫn `backToList()` để giữ nguyên chuỗi tìm kiếm khi quay lại danh sách.
- `ui/vault/VaultScreen.kt` — `VaultItemListScreen` thêm `OutlinedTextField` tìm kiếm ở đầu danh sách (icon kính lúp, nút xóa nhanh khi có nội dung); phân biệt "chưa có mục nào" (`vault_empty_state`) và "không có kết quả khớp" (string mới) dựa vào `searchQuery.isBlank()`.
- `values/strings.xml` + `values-vi/strings.xml` — thêm `search_placeholder` (hint ô tìm kiếm), `vault_no_results_state` (không khớp tìm kiếm), `search_clear_cd` (content description nút xóa nhanh).
- `docs/plans/roadmap.md` — cập nhật trạng thái Feature 6 sau khi xong.

---

## Implementation Steps

1. `VaultViewModel.kt`: thêm `private var searchQuery: String = ""`; thêm `private fun currentListState(): VaultUiState.ItemList` trả về `ItemList` với `items` đã lọc (`name`/`username` chứa `searchQuery`, không phân biệt hoa/thường; rỗng thì trả nguyên `latestItems`) và `searchQuery` hiện tại; dùng hàm này thay cho `VaultUiState.ItemList(items)`/`VaultUiState.ItemList(latestItems)` ở cả Flow collector lẫn `backToList()`; thêm `fun updateSearchQuery(query: String)` gọi `searchQuery = query` rồi `_state.value = currentListState()` (chỉ khi đang ở `ItemList`, nhưng hàm này chỉ được gọi từ màn danh sách nên luôn đúng state).
2. `VaultScreen.kt`: `VaultItemListScreen` nhận thêm `searchQuery: String`, `onSearchQueryChange: (String) -> Unit`; thêm `OutlinedTextField` ngay trên `LazyColumn`/empty-state (dưới 2 nút PIN/sinh trắc học); empty-state hiện `vault_no_results_state` nếu `searchQuery.isNotBlank()`, ngược lại `vault_empty_state`.
3. i18n string mới (en + vi).
4. Build + verify: `ktlintCheck detekt lint testDebugUnitTest assembleDebug`.
5. Verify sống: có sẵn vài Vault Item khác tên/username → gõ vào ô tìm kiếm → danh sách lọc đúng theo tên lẫn username, không phân biệt hoa/thường → xóa hết chữ → danh sách đầy đủ trở lại → gõ chuỗi không khớp → hiện đúng "không có kết quả" (khác với "chưa có mục nào") → mở 1 item đang lọc → quay lại → vẫn còn giữ đúng chuỗi tìm kiếm và danh sách đã lọc.
6. `/code-guard` + `/code-review`, sửa hết Critical/Major trước khi commit.
7. Cập nhật `roadmap.md` trạng thái Feature 6 = Done.
8. 1 commit.

---

## Risks

- Không có rủi ro đáng kể — thay đổi thuần UI + lọc trong bộ nhớ, không đụng tới schema DB hay luồng mã hóa/mở khóa.

---

## Questions

*(đã đề xuất phương án — chỉ hỏi lại nếu muốn đổi)*

- **Lọc theo SQL hay trong bộ nhớ?** 👉 Trong bộ nhớ (Kotlin `filter` trên `latestItems` đã có sẵn) — app single-user, số lượng Vault Item nhỏ, thêm query Room mới lúc này là abstraction chưa cần thiết.

---

## Status

✅ Hoàn tất. Build xanh (`ktlintCheck detekt lint testDebugUnitTest assembleDebug`). Verify sống trên emulator: gõ tìm kiếm lọc đúng theo tên ("proton" → "ProtonMail") lẫn username ("myuser" → item có username "myuserdhg"), không phân biệt hoa/thường; nút xóa nhanh trả lại danh sách đầy đủ; gõ chuỗi không khớp hiện đúng "No items match your search" (khác với "No items yet"); mở 1 item đang lọc rồi bấm Back → danh sách vẫn giữ đúng chuỗi tìm kiếm đã gõ trước đó.

`/code-review`: không có lỗi Critical/Major. 1 ghi chú Minor — `searchQuery` sống sót qua chu kỳ auto-lock → mở khóa lại (không tự reset), thay vì reset về danh sách đầy đủ. Đây là lựa chọn UX, không phải lỗi (spec không yêu cầu rõ hành vi nào), giữ nguyên như hiện tại.
