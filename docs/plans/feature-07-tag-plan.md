# Plan — Feature 7: Quản lý Tag (CRUD + gắn nhiều tag cho 1 item)

## Goal

- Cho phép tạo/sửa/xóa Tag và gắn nhiều Tag cho 1 Vault Item — `functional-spec.md §5` ("Phân nhóm/thẻ (tag) để sắp xếp"), `glossary.md` (Tag: danh sách mở, nhiều Tag/1 item, có sẵn tag gợi ý).

---

## Scope

### In Scope

- Bảng `tags` (Room) + bảng nối nhiều-nhiều `vault_item_tag_cross_ref` (Vault Item ↔ Tag).
- Màn "Quản lý Tag" riêng (thêm/sửa tên/xóa), mở từ danh sách Vault Item.
- Chọn nhiều Tag cho 1 Vault Item ngay trong form thêm/sửa (Feature 5) bằng `FilterChip`.
- Hiện Tag đã gắn ở màn chi tiết Vault Item.
- Seed sẵn vài Tag gợi ý ("Personal", "Bank", "Social Media" — đúng 3 tên nêu trong `glossary.md`) khi Vault mới được tạo lần đầu.
- Version Room tăng 1 → 2, dùng `fallbackToDestructiveMigration()` — app chưa release (xem Questions).

### Out of Scope

- Lọc/tìm theo Tag ở danh sách (không nằm trong tên Feature 7 "CRUD + gắn tag"; có thể làm sau nếu cần).
- Custom Field (Feature 8), loại Note (Feature 9).

**Đã đổi ý giữa chừng, ghi lại cho rõ:** hiện Tag dạng chip trên từng dòng danh sách + màu tự động riêng cho mỗi Tag (`TagColorDot`/`TagChip`) ban đầu được thêm trực tiếp vào Feature 7 (yêu cầu bổ sung 2026-07-19), đã code xong và verify sống đầy đủ (xem Status) TRƯỚC KHI có quyết định sau đó gộp các góp ý UI/UX phát sinh giữa chừng vào roadmap #16. Vì đã hoàn thành + verify, giữ nguyên trong Feature 7 thay vì revert — #16 vẫn còn ý nghĩa cho phần redesign toàn diện hơn (theo ManageEngine PasswordManager Pro), không phải cho 2 chi tiết nhỏ này.

---

## Files

**Mới:**
- `domain/Tag.kt` — model nghiệp vụ (id, name).
- `data/TagEntity.kt` — `@Entity(tableName = "tags")`, `name` unique (`Index(unique = true)`).
- `data/VaultItemTagCrossRef.kt` — bảng nối, `primaryKeys = ["vaultItemId", "tagId"]`, `ForeignKey` CASCADE cả 2 chiều.
- `data/VaultItemWithTags.kt` — `@Embedded` VaultItemEntity + `@Relation` (qua `Junction(VaultItemTagCrossRef::class)`) danh sách TagEntity.
- `data/TagDao.kt` — `observeAll()`, `insert`, `update`, `delete`.
- `data/TagRepository.kt` — bọc `TagDao`, map entity ↔ domain `Tag`.
- `ui/vault/TagManagerScreen.kt` — màn quản lý Tag (thêm/sửa/xóa).
- `ui/vault/TagViewModel.kt` — state + logic cho `TagManagerScreen` (ViewModel riêng, không nhét vào `VaultViewModel` để tránh phình 1 class quá nhiều trách nhiệm — xem Questions).
- `docs/plans/feature-07-tag-plan.md` — chính plan này.

**Sửa:**
- `data/VaultDatabase.kt` — thêm `TagEntity`/`VaultItemTagCrossRef` vào `entities`, `version = 2`, thêm `tagDao()`.
- `data/VaultFileManager.kt` — `Room.databaseBuilder(...)` thêm `.fallbackToDestructiveMigration()` (bump version cần có, app chưa release); thêm `.addCallback(onCreate = seed 3 tag gợi ý bằng raw SQL)` — chỉ chạy đúng 1 lần khi schema mới được tạo (Room tự đảm bảo, không cần tự kiểm tra).
- `data/VaultItemDao.kt` — bỏ `observeAll()`/`getById()` (không còn dùng, thay bằng bản có Tag), thêm `@Transaction observeAllWithTags(): Flow<List<VaultItemWithTags>>`, `@Transaction getByIdWithTags(id): VaultItemWithTags?`, `@Transaction setTagsForItem(itemId, tagIds: List<Long>)` (xóa hết cross-ref cũ của item rồi insert lại theo danh sách mới — đơn giản hơn diff thêm/bớt).
- `data/VaultItemRepository.kt` — `observeItems()`/`getItem()` dùng bản WithTags, map sang domain `VaultItem` (thêm field `tags`); thêm `setItemTags(itemId, tagIds)`.
- `domain/VaultItem.kt` — thêm `tags: List<Tag> = emptyList()`.
- `di/DataModule.kt` — thêm provider `TagRepository`.
- `ui/vault/VaultViewModel.kt` — inject thêm `TagRepository` (chỉ đọc, để hiện chip chọn Tag trong form); `ItemForm` state thêm `availableTags: List<Tag>`, `selectedTagIds: Set<Long>`; thêm `toggleTagSelected(tagId)`; `openAddForm`/`openEditForm` nạp `availableTags` hiện có + `selectedTagIds` (rỗng khi thêm mới, theo `item.tags` khi sửa); `save()` gọi thêm `repository.setItemTags(...)` sau khi thêm/sửa item xong.
- `ui/vault/VaultItemFormScreen.kt` — thêm hàng `FilterChip` cho từng Tag khả dụng, bật/tắt theo `selectedTagIds`.
- `ui/vault/VaultItemDetailScreen.kt` — hiện danh sách Tag đã gắn (nếu có) dưới URL/Note.
- `ui/vault/VaultScreen.kt` — thêm nút mở `TagManagerScreen` (icon cạnh ô tìm kiếm), dùng `remember { mutableStateOf(false) }` cục bộ giống `showPinDialog` đang có (không nhét vào `VaultUiState` vì đây thuần là điều hướng UI, không phải dữ liệu nghiệp vụ); nhận thêm `tagViewModel: TagViewModel`.
- `MainActivity.kt` — thêm `tagViewModel: TagViewModel by viewModels()`, truyền xuống `VaultScreen`.
- `values/strings.xml` + `values-vi/strings.xml` — string mới cho quản lý Tag + chọn Tag trong form.
- `docs/plans/roadmap.md` — cập nhật trạng thái Feature 7 sau khi xong.

---

## Implementation Steps

1. `domain/Tag.kt`, `data/TagEntity.kt`, `data/VaultItemTagCrossRef.kt`, `data/VaultItemWithTags.kt`, `data/TagDao.kt`.
2. `VaultDatabase.kt`: thêm entity mới, `version = 2`.
3. `VaultFileManager.kt`: `.fallbackToDestructiveMigration()` + `.addCallback` seed 3 tag gợi ý.
4. `VaultItemDao.kt`: bỏ `observeAll`/`getById` cũ, thêm 3 hàm mới (WithTags + setTagsForItem).
5. `data/TagRepository.kt`, sửa `VaultItemRepository.kt`, `domain/VaultItem.kt`.
6. `di/DataModule.kt`: thêm provider `TagRepository`.
7. `ui/vault/TagViewModel.kt` (theo đúng pattern `startObservingItems()` của `VaultViewModel` — không collect Flow trong `init` để tránh crash trước khi unlock, bài học từ Feature 5), `ui/vault/TagManagerScreen.kt`.
8. `VaultViewModel.kt`: inject `TagRepository`, mở rộng `ItemForm` state, `toggleTagSelected`, nạp Tag trong `openAddForm`/`openEditForm`, cập nhật `save()`.
9. `VaultItemFormScreen.kt`, `VaultItemDetailScreen.kt`: UI hiện/chọn Tag.
10. `VaultScreen.kt`, `MainActivity.kt`: wiring `TagViewModel` + nút mở `TagManagerScreen`.
11. i18n string mới (en + vi).
12. Build + verify: `ktlintCheck detekt lint testDebugUnitTest assembleDebug`.
13. Verify sống: mở Quản lý Tag → thấy đúng 3 tag gợi ý có sẵn (vault mới) → thêm 1 tag mới → sửa tên → xóa 1 tag → quay lại thêm/sửa 1 Vault Item → chọn nhiều Tag qua `FilterChip` → lưu → mở lại chi tiết item → thấy đúng các Tag đã chọn → xóa 1 Tag đang được gắn ở Quản lý Tag → mở lại item đó, xác nhận Tag đã xóa không còn hiện (cascade đúng).
14. `/code-guard` + `/code-review`, sửa hết Critical/Major trước khi commit.
15. Cập nhật `roadmap.md` trạng thái Feature 7 = Done.
16. 1 commit.

---

## Risks

- Tăng version Room (1 → 2) giữa lúc đang phát triển — dùng `fallbackToDestructiveMigration()` nghĩa là dữ liệu test cũ trên máy/emulator sẽ mất khi mở lại bản build mới. Chấp nhận được vì app chưa release, chưa có người dùng thật (xem Questions).
- `@Relation`/`Junction` là pattern Room mới trong project — cần verify sống kỹ để chắc join đúng (không trả về Tag trùng/thiếu).
- `setTagsForItem` xóa hết rồi insert lại — đơn giản, đúng, nhưng không tối ưu cho item có rất nhiều Tag; chấp nhận được vì số Tag/item thực tế nhỏ.

---

## Questions

*(đã đề xuất phương án — chỉ hỏi lại nếu muốn đổi)*

- **Tăng version Room có cần viết migration thật không, hay dùng `fallbackToDestructiveMigration()`?** 👉 Dùng `fallbackToDestructiveMigration()` — app chưa release (`gh release create` còn đang chờ tới khi roadmap xong theo ghi chú trước), viết migration thật cho schema còn đang thay đổi liên tục qua từng feature (7/8/9 đều sẽ đổi schema) là công sức bỏ ra cho rủi ro chưa tồn tại.
- **Quản lý Tag có nên nhét chung vào `VaultViewModel` không?** 👉 Không — tách riêng `TagViewModel` để tránh 1 ViewModel gánh quá nhiều trách nhiệm (CRUD item + tìm kiếm + CRUD Tag), theo đúng tinh thần giới hạn `TooManyFunctions` đã cấu hình trong `detekt.yml`. `VaultViewModel` chỉ đọc Tag (để hiện chip chọn trong form), không sở hữu logic CRUD Tag.
- **Điều hướng sang màn Quản lý Tag có cần thêm vào `VaultUiState` không?** 👉 Không — dùng `remember { mutableStateOf(false) }` cục bộ trong `VaultScreen` giống `showPinDialog` đã có sẵn, vì đây thuần là điều hướng UI, không phải dữ liệu nghiệp vụ cần sống sót qua rotation theo ViewModel (Compose `rememberSaveable` không cần thiết vì đây không phải dữ liệu nhập của user).

---

## Status

✅ Hoàn tất. Build xanh (`ktlintCheck detekt lint testDebugUnitTest assembleDebug`).

Verify sống trên emulator (`pwvault-test`): 3 tag gợi ý ("Bank", "Personal", "Social Media") xuất hiện đúng trên Vault mới tạo (raw-SQL seed qua `RoomDatabase.Callback.onCreate` — xác nhận CHỈ chạy khi tạo file DB thật sự mới, không chạy lại khi `fallbackToDestructiveMigration()` tái tạo bảng từ 1 file DB cũ hơn version — không phải vấn đề thật vì user thật luôn tạo Vault mới hoàn toàn, chỉ ảnh hưởng dữ liệu test cũ trên máy dev). Tag CRUD: thêm tag mới, từ chối đúng khi trùng tên (không phân biệt hoa/thường), sửa tên, xóa tag đều hoạt động đúng. Gắn nhiều Tag cho 1 Vault Item qua `FilterChip` trong form: xác nhận chọn được nhiều Tag cùng lúc, lưu đúng, hiện đúng ở cả màn chi tiết lẫn từng dòng trong danh sách (kèm chấm màu `TagColorDot` theo `tag.id`). Xóa 1 Tag đang gắn ở Quản lý Tag → item vẫn còn nguyên, chỉ mất đúng Tag đã xóa (cascade đúng qua `ForeignKey.CASCADE`), không ảnh hưởng Tag khác.

Phát hiện trong lúc test sống (không phải bug code — quirk automation): `VaultItemFormScreen` tự động focus vào ô Tên mỗi khi mở form (auto-focus từ Feature 5), khiến bàn phím ảo tự bật lên che khuất vùng Tags/Save/Cancel — tap vào các nút/chip trong vùng bị che có thể trúng nhầm bàn phím hoặc chèn ký tự lạ vào ô Tên. Phải tắt bàn phím (BACK) trước khi thao tác vùng bên dưới. Không phải lỗi sản phẩm (người dùng thật gõ xong sẽ tự ẩn bàn phím trước khi cuộn xuống), chỉ là điều cần nhớ khi test bằng `uiautomator`/`adb input`.

`/code-review` (trước commit) — 2 sửa Minor áp dụng ngay vì gần như miễn phí (cùng lúc bump schema version cho feature này):
1. Unique index trên `tags.name` vốn phân biệt hoa/thường ở tầng DB (khác với check trong bộ nhớ ở `TagViewModel` — không phân biệt hoa/thường) — để hở 1 khe hẹp lý thuyết (race lúc mới mở màn, Flow chưa kịp emit) cho phép lọt trùng tên khác hoa/thường. Thêm `@ColumnInfo(collate = ColumnInfo.NOCASE)` cho cột `name` để tầng DB tự chặn đúng như tầng ViewModel.
2. Thông báo lỗi "đã có tag trùng tên" bị lưu lại và hiện sai ngữ cảnh khi rời màn Quản lý Tag rồi quay lại (phát hiện trực tiếp lúc test sống) — `startObservingTags()` nay tự xóa `newTagError`/`renameError` mỗi lần màn được mở lại.

Verify lại sau 2 sửa trên (build lại xanh + reinstall + test sống lại từ đầu): seed 3 tag đúng, từ chối trùng tên "bank" so với "Bank" vẫn đúng (không đổi hành vi), và xác nhận thông báo lỗi không còn tồn tại sai ngữ cảnh khi quay lại màn Quản lý Tag.
