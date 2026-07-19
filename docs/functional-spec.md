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
- Khóa mã hóa sinh từ Master Password (KDF: PBKDF2/Argon2).
- Không lưu Master Password dưới mọi hình thức.
- File lưu trong vùng dữ liệu riêng của app (private storage).

## 4. Đăng nhập / Mở khóa app
- Thiết lập Master Password khi cài đặt lần đầu (bắt buộc).
- Mở khóa bằng Master Password (luôn là phương án gốc).
- Mở khóa nhanh tùy chọn: vân tay/khuôn mặt **hoặc** mã PIN số.
- PIN số là fallback bắt buộc phải có cho mọi thiết bị (kể cả không có cảm biến sinh trắc học).
- Sinh trắc học/PIN chỉ mở khóa UI, khóa mã hóa thật vẫn dựa trên Master Password (lưu trong Android Keystore, không lưu plaintext).
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
- Mặc định export ra **file mã hóa riêng của app** (định dạng `.pwvbackup`, AES-256, khóa từ Master Password) — dùng để backup hoặc chuyển máy.
- Export CSV/Excel plaintext là tùy chọn **phụ**, không phải mặc định:
  - Yêu cầu xác nhận cảnh báo 2 bước trước khi xuất.
  - File CSV/Excel plaintext bắt buộc được nén kèm mật khẩu (zip + password) trước khi ghi ra bộ nhớ ngoài.
  - File tạm lưu ở thư mục riêng, tự động xóa sau 5 phút ✅ hoặc khi thoát app.
- Yêu cầu xác thực lại (Master Password) trước mọi thao tác export.
- Backup định kỳ (tự động nhắc, không tự động gửi đi đâu):
  - App nhắc người dùng backup nếu quá 30 ngày ✅ chưa export bản mới.
  - Người dùng chọn nơi lưu qua Storage Access Framework (SD card, USB OTG, bộ nhớ ngoài) — không mặc định lưu cloud.
  - Giữ tối đa K bản backup gần nhất (rotate), tránh ghi đè mất bản cũ. ❓ (chưa có giá trị K, cần bạn chốt)
- Import lại từ `.pwvbackup` để khôi phục/chuyển máy: chỉ cần Master Password, không cần map cột như CSV.

## 8. Bảo mật bổ sung
- Chặn chụp màn hình (FLAG_SECURE) trong toàn bộ app.
- Không cho backup app data qua Google Auto Backup (`android:allowBackup=false`).
- Cảnh báo mật khẩu yếu/trùng lặp.
- Kiểm tra mật khẩu có nằm trong danh sách rò rỉ (tùy chọn, dùng database offline, không gọi API ngoài).

## 9. Rủi ro đã có phương án, còn lại cần chấp nhận
- Mất Master Password = mất toàn bộ dữ liệu (không thể khôi phục) — không có backdoor, đây là đánh đổi bắt buộc của mô hình zero-knowledge.
- Nếu người dùng tắt hẳn nhắc backup và không tự export thủ công thì vẫn có thể mất dữ liệu khi mất máy.
- File CSV/Excel plaintext (nếu người dùng chủ động chọn) vẫn là điểm yếu nếu không tự xóa kịp — đã giảm rủi ro bằng nén có mật khẩu + tự xóa, nhưng không loại bỏ hoàn toàn.
