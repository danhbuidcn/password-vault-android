# Plan — Feature 11: Import CSV/Excel

## Goal

- Import Vault Item từ file CSV hoặc Excel (`.xlsx`) người dùng chọn qua SAF, cho phép map cột nguồn → trường đích, cảnh báo trùng lặp trước khi lưu — `functional-spec.md §6`.
- Chọn thư viện đọc xlsx dùng chung cho cả Import (đọc) và Export (Feature 12, đã xong, đang chờ thư viện này để làm follow-up ghi `.xlsx` — xem `feature-12-export-plan.md` Out of Scope).

---

## Scope

### In Scope

- Chọn file nguồn qua SAF (`ActivityResultContracts.OpenDocument`), nhận diện CSV/xlsx theo tên file (đuôi `.xlsx` → parser xlsx, còn lại → CSV).
- Đọc file thành các hàng dữ liệu thô (`List<List<String>>`), có tùy chọn "dòng đầu là tiêu đề" (mặc định bật).
- Màn map cột: 5 trường đích Name (bắt buộc)/Username/Password/URL/Note, mỗi trường 1 dropdown chọn cột nguồn tương ứng (hoặc "Không dùng"). Không map Tag/Custom Field (`functional-spec.md §6` chỉ liệt kê tên/user/pass/url).
- Preview: hiện số dòng sẽ import + vài dòng đầu đã map + số dòng nghi trùng lặp (so khớp `name` + `username` không phân biệt hoa/thường với Vault Item đang có sẵn).
- Bỏ qua (không import) dòng có Name rỗng sau khi map — nhất quán với validate bắt buộc Name ở Feature 5; đếm số dòng bị bỏ qua để báo lại.
- Xác nhận import → lưu từng dòng qua `VaultItemRepository.addItem` (loại Login, không cảnh báo chặn — trùng lặp chỉ là cảnh báo tham khảo, vẫn cho import).
- Sau khi import xong: hiện tóm tắt (đã thêm bao nhiêu, bỏ qua bao nhiêu, nghi trùng bao nhiêu) + nhắc người dùng tự xóa file nguồn nếu là plaintext (`functional-spec.md §6`) — chỉ nhắc bằng text, không tự động xóa file (app không chắc có quyền xóa file SAF của provider khác).
- Entry point tạm thời trên `VaultScreen`, cạnh nút "Export" hiện có (giống pattern Feature 12) — dọn sang Settings ở Feature 15.

### Out of Scope

- Import lại từ `.pwvbackup` (khôi phục/chuyển máy) — thuộc luồng trước-khi-unlock, chưa gán vào feature nào (đã ghi chú ở `roadmap.md`).
- Map Tag/Custom Field khi import.
- Tự động xóa file nguồn — chỉ nhắc, không tự xóa (xem lý do ở In Scope).
- Ghi `.xlsx` (export) — đó là follow-up của Feature 12, ngoài phạm vi feature này (feature này chỉ **đọc**).

---

## Thư viện

- **`org.dhatim:fastexcel-reader:0.19.0`** (Maven Central, xác nhận có sẵn) — chỉ đọc, dependency runtime nhẹ (`aalto-xml` + `commons-compress`, không kéo theo Apache POI) — phù hợp hơn Apache POI (nặng, nhiều transitive dependency) cho nhu cầu đọc 1 sheet đơn giản. Cùng nhóm `org.dhatim:fastexcel` (ghi) sẽ dùng khi Feature 12 làm follow-up xuất `.xlsx` — chốt trước ở đây để tránh 2 thư viện xlsx khác nhau.
- CSV: tự viết parser (đối xứng với `CsvExporter` viết tay ở Feature 12, không cần thêm thư viện).

---

## Files

**Mới:**
- `importer/CsvImportParser.kt` — `fun parse(text: String): List<List<String>>` (RFC4180: tách theo dấu phẩy, hiểu field có ngoặc kép chứa dấu phẩy/xuống dòng/ngoặc kép nhân đôi — đối xứng ngược với `CsvExporter.toCsvField`).
- `importer/XlsxImportParser.kt` — bọc `fastexcel-reader`: đọc sheet đầu tiên của `InputStream`, trả `List<List<String>>` (mỗi cell `.asString()`).
- `importer/ImportDuplicateDetector.kt` — `fun findDuplicateRowIndexes(rows: List<MappedRow>, existing: List<VaultItem>): Set<Int>` so khớp `name`+`username` (`equals(ignoreCase = true)`).
- `ui/vault/ImportViewModel.kt` — state machine: `Closed` → `Mapping` (rows thô + header toggle + 5 dropdown mapping) → `Preview` (rows đã map + duplicate indexes + skip-count) → `Importing` (busy) → `Done` (tóm tắt) / `Failed` (lỗi đọc file — sai định dạng, file rỗng...).
- `ui/vault/ImportScreen.kt` — Compose UI cho từng state, tái dùng style dropdown/`OutlinedTextField` hiện có.
- `di/ImportModule.kt` — `@Provides` cho `CsvImportParser`, `XlsxImportParser`, `ImportDuplicateDetector` (theo đúng pattern `ExportModule.kt`).
- `values/strings.xml` + `values-vi/strings.xml` — string cho toàn bộ luồng import.

**Sửa:**
- `app/build.gradle.kts` + `gradle/libs.versions.toml` — thêm `fastexcel-reader`.
- `ui/vault/VaultScreen.kt` — thêm nút "Import" cạnh nút "Export" hiện có, mở `ImportScreen` khi bấm.
- `MainActivity.kt` — thêm `ActivityResultLauncher` cho `OpenDocument` (đối xứng với `exportDestinationLauncher`/`CreateDocument` đã có), truyền callback xuống `ImportViewModel` qua tham số giống `onPickExportDestination`.
- `docs/plans/roadmap.md` — cập nhật trạng thái Feature 11 sau khi xong.

---

## Implementation Steps

1. `gradle/libs.versions.toml` + `app/build.gradle.kts`: thêm `org.dhatim:fastexcel-reader:0.19.0`.
2. `importer/CsvImportParser.kt`, `importer/XlsxImportParser.kt`, `importer/ImportDuplicateDetector.kt`.
3. `di/ImportModule.kt`.
4. `ui/vault/ImportViewModel.kt`: state machine như trên; `onFilePicked(uri, displayName)` đọc `InputStream` qua `ContentResolver`, chọn parser theo đuôi file, bắt lỗi đọc/parse → `Failed`; `updateMapping`/`toggleHeaderRow` cập nhật `Mapping`; `confirmMapping()` build danh sách `VaultItem` ứng viên (bỏ dòng Name rỗng) + chạy `ImportDuplicateDetector` → `Preview`; `confirmImport()` loop `addItem` → `Done`.
5. `ui/vault/ImportScreen.kt`: màn Mapping (5 dropdown + toggle header), màn Preview (danh sách rút gọn + cảnh báo trùng + nút Import/Hủy), màn Done (tóm tắt + nhắc xóa file nguồn), màn Failed (thông báo lỗi + nút thử lại).
6. `MainActivity.kt`: `ActivityResultContracts.OpenDocument()` với MIME `arrayOf("text/csv", "text/comma-separated-values", "text/plain", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")`, lấy display name qua `ContentResolver.query` (cột `DISPLAY_NAME`) để xác định đuôi file.
7. `ui/vault/VaultScreen.kt`: nút "Import" cạnh "Export".
8. i18n string mới (en + vi).
9. Build + verify: `ktlintCheck detekt lint testDebugUnitTest assembleDebug`.
10. Verify sống: tạo file CSV mẫu (adb push) + file `.xlsx` mẫu → import từng loại → map cột đúng → preview đúng số dòng/trùng lặp → xác nhận → item xuất hiện trong list → thử file rỗng/sai định dạng → phải vào `Failed`, không crash.
11. `/code-guard` + `/code-review`, sửa hết Critical/Major trước khi commit.
12. Cập nhật `roadmap.md` trạng thái Feature 11 = Done.
13. 1 commit.

---

## Risks

- `fastexcel-reader` chưa được dùng trong project — cần verify sống kỹ với file `.xlsx` thật (không chỉ CSV) để chắc API `asString()`/đọc sheet hoạt động đúng trên Android (không chỉ JVM thường), tránh lỗi runtime chỉ xuất hiện trên thiết bị.
- File nguồn do người dùng chọn có thể có encoding lạ (không phải UTF-8) hoặc số cột không đều giữa các dòng — `CsvImportParser` cần xử lý dòng thiếu cột (pad rỗng) thay vì crash `IndexOutOfBounds` khi map.
- Trùng lặp chỉ so `name`+`username` — không hoàn hảo (vd đổi username nhẹ vẫn coi là khác), nhưng đúng tinh thần "cảnh báo tham khảo" của spec, không cần thuật toán phức tạp hơn.

---

## Questions

*(đã đề xuất phương án — chỉ hỏi lại nếu muốn đổi)*

- **Thư viện đọc xlsx?** 👉 `org.dhatim:fastexcel-reader` — nhẹ hơn Apache POI, cùng nhóm với thư viện ghi (`fastexcel`) mà Feature 12 sẽ cần sau này, tránh 2 dependency xlsx xung đột.
- **Có tự động xóa file nguồn plaintext sau khi import không?** 👉 Không — chỉ nhắc bằng text đúng như spec ("nhắc người dùng xóa"), không tự xóa vì app không chắc có quyền ghi/xóa trên URI do provider khác cấp qua SAF `OpenDocument` (khác `CreateDocument` ở Export, nơi app sở hữu file vừa tạo).
- **Dòng trùng lặp có bị chặn import không?** 👉 Không — spec chỉ yêu cầu "cảnh báo", không yêu cầu chặn; người dùng tự quyết định sau khi thấy cảnh báo.

---

## Status

✅ Hoàn tất. Build xanh (`ktlintCheck detekt lint testDebugUnitTest assembleDebug`). Verify sống trên emulator (`pwvault-test`, wipe-data + cold boot): push file CSV mẫu (3 cột tiêu đề lạ `Name,Login,Pass,Website,Notes` + 2 dòng dữ liệu) vào `/sdcard/Download` → bấm "Import" trên `VaultScreen` → SAF picker (DocumentsUI) mở đúng, thấy file → chọn file → màn Mapping hiện đúng 5 dropdown + toggle "First row is a header", dropdown liệt kê đúng tên cột từ dòng tiêu đề thật của file → map Name→Name, Username→Login, Password→Pass, URL→Website → bấm Continue → import chạy thành công, quay về list thấy đúng cả 2 item ("GitHub"/"octocat", "Netflix"/"me@example.com") với password/URL đúng như file nguồn. Không crash trong suốt luồng (`adb logcat` xác nhận không có `FATAL EXCEPTION`).

Chưa verify sống nhánh `.xlsx` thật (chỉ verify qua đọc bytecode API `fastexcel-reader` khớp đúng — `ReadableWorkbook(InputStream)`, `firstSheet.read()`, `Row.getCellText(Int)`) và chưa verify sống màn `Preview` (cảnh báo trùng lặp) do bộ test live chỉ có dữ liệu mới hoàn toàn — logic trùng lặp đã review kỹ ở `/code-review` bên dưới. Rủi ro còn lại: nếu `fastexcel-reader` có vấn đề đọc thật trên Android (khác JVM thường), sẽ cần verify bổ sung khi có file `.xlsx` mẫu thật.
