# Tài liệu chức năng — pwvault-android

App Android quản lý mật khẩu offline, lưu file mã hóa cục bộ, không có server/cloud bắt buộc.

## 1. Tổng quan
- App Android lưu mật khẩu hoàn toàn trên máy (offline).
- Không có server, không đồng bộ qua internet tự động.
- Dữ liệu lưu dưới dạng file mã hóa, chỉ app đọc được.

## 2. Mục tiêu
- Bảo mật tối đa, không rò rỉ dữ liệu qua mạng.
- Người dùng tự kiểm soát hoàn toàn dữ liệu của mình.
- Có phương án backup an toàn, không phụ thuộc cloud.

## 3. Kiến trúc lưu trữ
- 1 file dữ liệu chính, mã hóa (AES-256).
- Khóa mã hóa ngẫu nhiên (32 byte), bọc bằng khóa Android Keystore, chỉ mở được sau khi xác thực PIN hoặc sinh trắc học — xem [Feature 19](plans/feature-19-simplify-unlock-csv-plan.md).
- File lưu trong vùng dữ liệu riêng của app (private storage).

## 4. Đăng nhập / Mở khóa app
- Thiết lập mã PIN số khi cài đặt lần đầu (bắt buộc, ≥4 số). PIN là mật khẩu **duy nhất** người dùng cần nhớ — không còn Master Password ([Feature 19](plans/feature-19-simplify-unlock-csv-plan.md)).
- Mở khóa bằng PIN, hoặc vân tay/khuôn mặt nếu đã bật trong Settings (tùy chọn, `BIOMETRIC_WEAK` để khuôn mặt chạy được trên đa số máy).
- PIN luôn bật, chỉ đổi được, không tắt được.
- Vault cũ (tạo bằng Master Password, chưa có PIN): mở bằng Master Password lần cuối, sau đó bắt buộc đặt PIN.
- Tự động khóa sau 1 phút không thao tác (mặc định) ✅, cho phép cấu hình 30 giây/1 phút/5 phút/15 phút/Không bao giờ.
- Giới hạn 5 lần nhập sai ✅ (chống brute-force), sau đó khóa tạm thời tăng dần: 30 giây, nhân đôi mỗi lần sai tiếp theo, tối đa 30 phút ✅.

## 5. Quản lý mật khẩu
- Thêm / sửa / xóa mục mật khẩu.
- Mỗi mục gồm: tên, username, password, URL, ghi chú.
- Tìm kiếm theo tên/username.
- Phân nhóm/thẻ (tag) để sắp xếp.
- Sinh mật khẩu ngẫu nhiên (độ dài, ký tự tùy chỉnh).
- Copy nhanh password vào clipboard, tự xóa clipboard sau 30 giây ✅.
- Ẩn/hiện password khi xem.

## 6. Import
- Import từ file CSV.
- Import từ file Excel (.xlsx).
- Cho phép map cột (tên, user, pass, url...) khi import.
- Cảnh báo trùng lặp khi import.
- Sau khi import xong, nhắc người dùng xóa file nguồn nếu là plaintext.

## 7. Export & Backup (gộp, dùng chung cho cả backup và chuyển máy)

> **Feature 19:** Auto-backup và file `.pwvbackup` đã bỏ. Backup/chuyển máy chỉ còn bằng CSV — xem 7.2. Mục 7.1 giữ lại để tham khảo lịch sử.

### 7.1 Auto-backup (tự động, chạy nền) — ❌ đã bỏ (Feature 19)
- Mỗi khi thêm/sửa/xóa Vault Item thành công, tự động ghi 1 bản `.pwvbackup` (mã hóa AES-256, khóa từ Master Password — dùng lại Vault Key đang có sẵn trong session `Unlocked`, **không** cần xác thực lại).
- Ghi atomic: ghi ra file tạm trước, rename đè lên bản cũ sau khi ghi xong — tránh hỏng file backup nếu app crash/mất điện giữa chừng.
- Lưu vào 1 thư mục do người dùng chọn **1 lần duy nhất** qua Storage Access Framework (ví dụ thư mục được Google Drive/FolderSync tự đồng bộ) — app xin quyền ghi lâu dài (persistable URI permission), không hardcode đường dẫn.
- Giữ tối đa 5 bản gần nhất (rotate) ✅, không ghi đè hoàn toàn 1 file duy nhất — tránh mất cả máy lẫn backup nếu bản mới nhất bị hỏng.
- Người dùng tự đưa thư mục này lên Google Drive/nơi lưu ngoài; app không tự động upload cloud.
- Khôi phục: cài app mới → import 1 trong các bản `.pwvbackup` → nhập đúng Master Password cũ → xem được dữ liệu. Không có cách khôi phục nếu quên Master Password (giữ nguyên mô hình zero-knowledge, xem mục 9).

### 7.2 Export thủ công (theo yêu cầu người dùng)
- Export ra file **CSV thường, không mã hóa** (cột: Name, Username, Password, URL, Note, Tags, CustomFields, Type) — dùng cho backup và chuyển máy.
- Yêu cầu nhập lại PIN trước khi export; màn export có 1 dòng cảnh báo file không mã hóa.
- App vẫn nhắc người dùng export nếu quá 30 ngày ✅ chưa export bản mới.
- Chuyển máy: cài app → đặt PIN → Import file CSV. Header của app được tự map cột, khôi phục cả loại item, Tag, Custom Field.

## 8. Bảo mật bổ sung
- Chặn chụp màn hình (FLAG_SECURE) trong toàn bộ app.
- Không cho backup app data qua Google Auto Backup (`android:allowBackup=false`).
- Cảnh báo mật khẩu yếu/trùng lặp.
- Kiểm tra mật khẩu có nằm trong danh sách rò rỉ (tùy chọn, dùng database offline, không gọi API ngoài).

## 9. Rủi ro đã có phương án, còn lại cần chấp nhận
- Không có Master Password: mất máy / reset máy / xóa app = mất dữ liệu, chỉ còn bản CSV đã export.
- File CSV export không mã hóa — ai có file là đọc được toàn bộ mật khẩu; người dùng tự cất giữ.
- Nếu người dùng tắt hẳn nhắc backup và không tự export thủ công thì vẫn có thể mất dữ liệu khi mất máy.
