# Plan — Feature 4: Tự động khóa + giới hạn số lần nhập sai

## Goal

- Tự động khóa lại Vault sau một khoảng thời gian không dùng app, chống lộ dữ liệu khi rời máy (`functional-spec.md §4`, dòng 27).
- Giới hạn số lần nhập sai Master Password/PIN, khóa tạm thời tăng dần để chống brute-force (`functional-spec.md §4`, dòng 28; `architecture.md` dòng 77).
- Đây là feature đầu tiên có threshold bảo mật dạng số — theo [[feedback_pwvault_security_constants]], toàn bộ các số này phải nằm trong **1 file** duy nhất, không rải rác `private const val` từng file.

---

## Scope

### In Scope

- `security/SecurityPolicy.kt` — file tập trung toàn bộ threshold: thời gian auto-lock mặc định + các lựa chọn cấu hình sẵn, số lần sai tối đa trước khi khóa, thời gian khóa ban đầu, thời gian khóa tối đa.
- Auto-lock: khóa lại (xóa `vaultKey` khỏi bộ nhớ, quay về màn hình mở khóa phù hợp) nếu thời gian app ở nền (background) vượt quá ngưỡng cấu hình. Mặc định 1 phút, theo đúng mô hình các app quản lý mật khẩu phổ biến (Bitwarden, 1Password...): đếm giờ từ lúc app xuống nền, không phải đếm giờ "không chạm màn hình" khi đang mở app.
- Lockout: đếm chung 1 bộ đếm số lần sai cho cả Master Password và PIN (sai ở màn nào cũng cộng dồn — nếu tách riêng, kẻ tấn công bị khóa PIN vẫn đoán tiếp được Master Password, vô nghĩa). Sinh trắc học KHÔNG tính vào bộ đếm này (hệ điều hành đã tự giới hạn số lần thử sinh trắc học ở tầng HAL, không cần app tự làm thêm — xem Questions).
- Bộ đếm sai + thời điểm hết khóa phải **lưu bền** (SharedPreferences) — khác với auto-lock (không cần lưu bền, vì tiến trình bị hệ điều hành kill giữa chừng thì `vaultKey` cũng mất theo, coi như đã khóa). Lockout phải lưu bền để tránh bypass bằng cách force-stop rồi mở lại app.
- Màn `Locked`/`PinEntry` hiện đúng trạng thái khóa tạm thời (đếm ngược số giây còn lại), vô hiệu hóa nút Mở khóa trong lúc khóa.
- Unlock thành công (bất kỳ phương thức nào) → reset bộ đếm sai về 0.

### Out of Scope

- Màn hình Settings để người dùng đổi thời gian auto-lock (Feature 15) — bản này chỉ dùng giá trị mặc định (1 phút) đã lưu sẵn trong SharedPreferences theo đúng key mà Settings sau này sẽ đọc/ghi, không tự dựng UI tạm thời (auto-lock không có hành động "bật/tắt" như PIN/biometric nên không cần nút tạm ở `VaultScreen`).
- Giới hạn riêng cho sinh trắc học (đã có ở tầng hệ điều hành, xem Questions).
- Khóa vĩnh viễn ("permanently locked" đòi xóa Vault) — spec chỉ yêu cầu khóa tạm thời tăng dần, tối đa 30 phút rồi lặp lại chu kỳ (không tăng thêm nữa sau khi chạm trần).

---

## Files

**Mới:**
- `security/SecurityPolicy.kt` — hằng số tập trung: `DEFAULT_AUTO_LOCK_TIMEOUT`, `AUTO_LOCK_TIMEOUT_OPTIONS` (30s/1min/5min/15min/Never), `MAX_ATTEMPTS_BEFORE_LOCKOUT = 5`, `INITIAL_LOCKOUT_DURATION = 30s`, `MAX_LOCKOUT_DURATION = 30min`.
- `security/AutoLockPreferences.kt` — SharedPreferences lưu ngưỡng auto-lock đang chọn (đọc `DEFAULT_AUTO_LOCK_TIMEOUT` nếu chưa có), key dùng chung cho Feature 15 sau này.
- `security/LockoutStore.kt` — SharedPreferences lưu `wrongAttemptCount: Int`, `lockedUntilMillis: Long`.
- `security/LockoutPolicy.kt` — logic thuần: `recordFailure()` (tăng đếm, tính thời gian khóa mới nếu ≥ ngưỡng), `recordSuccess()` (reset), `currentLockout(): Long?` (thời điểm hết khóa nếu đang khóa, null nếu không).

**Sửa:**
- `ui/unlock/UnlockViewModel.kt` — thêm `lockedUntilMillis: Long?` vào `Locked`/`PinEntry` state; `unlock()`/`unlockWithPin()` kiểm tra lockout trước khi derive/verify (tránh tốn CPU Argon2id vô ích); gọi `LockoutPolicy.recordFailure()`/`recordSuccess()` đúng chỗ; thêm `onAppBackgrounded()`/`onAppForegrounded()` cho auto-lock.
- `MainActivity.kt` — override `onStop()`/`onResume()` gọi 2 hàm trên.
- `ui/unlock/UnlockScreen.kt`, `ui/unlock/PinUnlockScreen.kt` — hiện đếm ngược khi `lockedUntilMillis != null`, vô hiệu hóa nút Mở khóa.
- `di/SecurityModule.kt` — cung cấp `AutoLockPreferences`, `LockoutStore`, `LockoutPolicy`.
- `values/strings.xml` + `values-vi/strings.xml` — string đếm ngược ("Thử lại sau %1$d giây").

---

## Implementation Steps

1. `SecurityPolicy.kt`: định nghĩa toàn bộ hằng số (dùng `kotlin.time.Duration` cho dễ đọc: `30.seconds`, `1.minutes`...).
2. `AutoLockPreferences`: `getTimeout(): Duration`, `setTimeout(Duration)` (chưa có UI gọi `setTimeout` ở bản này, nhưng để sẵn method cho Feature 15).
3. `LockoutStore`: SharedPreferences thô lưu 2 giá trị (Int + Long).
4. `LockoutPolicy`: dùng `SecurityPolicy` + `LockoutStore`, thuần logic không phụ thuộc Android Context ngoài store đã inject.
5. `UnlockViewModel`: thêm field `lockedUntilMillis` vào state cần thiết; sửa `unlock`/`unlockWithPin` gọi `lockoutPolicy.currentLockout()` trước — nếu đang khóa, set state với `lockedUntilMillis` và return ngay, không derive; sau khi verify xong (đúng/sai) gọi `recordSuccess()`/`recordFailure()` tương ứng. Thêm `onAppBackgrounded()` (lưu timestamp), `onAppForegrounded()` (so sánh với `autoLockPreferences.getTimeout()`, nếu vượt và đang `Unlocked` → xóa `vaultKey`, chuyển về state khóa phù hợp giống `initialState()`).
6. `MainActivity`: `override fun onStop()`/`onResume()` gọi 2 hàm ViewModel tương ứng.
7. UI: `UnlockScreen`/`PinUnlockScreen` nhận thêm `lockedUntilMillis: Long?`, dùng `LaunchedEffect` tick mỗi giây tính số giây còn lại, hiện text + vô hiệu hóa nút khi > 0.
8. i18n string mới (en + vi).
9. Build + verify: `ktlintCheck detekt lint assembleDebug`.
10. Verify sống trên emulator: sai Master Password 5 lần liên tiếp → khóa 30s, nút Mở khóa vô hiệu hóa, đếm ngược đúng; hết 30s → thử lại được; sai tiếp lần 6 → khóa 60s (kiểm tra tăng gấp đôi); unlock đúng → bộ đếm reset về 0 (verify bằng cách sai 4 lần rồi đúng, không bị khóa). Auto-lock: unlock xong, home app (background) > 1 phút (hoặc set thời gian ngắn hơn tạm thời để test nhanh, xem Questions), mở lại → phải quay về màn khóa.
11. `/code-guard` + `/code-review`, sửa hết Critical/Major trước khi commit.
12. 1 commit.

---

## Status

✅ Hoàn tất. Build xanh (`ktlintCheck detekt lint assembleDebug`). Verify sống đầy đủ trên Android Emulator (`pwvault-test`, thiết bị thật vẫn chưa kết nối lại):
- Sai PIN 4 lần liên tiếp → chỉ hiện "Wrong PIN", không khóa. Lần sai thứ 5 → khóa 30s đúng, nút Mở khóa vô hiệu hóa (`enabled=false`), đếm ngược hiện đúng.
- Sai tiếp sau khi hết khóa → khóa lần 2 tăng lên 60s (xác nhận tăng gấp đôi đúng công thức).
- Bộ đếm dùng chung PIN + Master Password: chuyển màn hình trong lúc đang khóa vẫn thấy đúng đếm ngược (đã sửa 1 lỗi tìm thấy khi test — xem bên dưới).
- Trạng thái khóa (`attemptCount`, `lockedUntilMillis`) sống sót qua reinstall APK + khởi động lại app (SharedPreferences bền vững) — đúng yêu cầu chống bypass bằng force-stop.
- Mở khóa đúng sau khi hết khóa → reset bộ đếm về 0 (xác nhận: 1 lần sai tiếp theo chỉ hiện lỗi thường, không khóa lại ngay).
- Auto-lock: nền app 8s (< ngưỡng) → vẫn `Unlocked` khi mở lại. Nền app 65s (> ngưỡng mặc định 1 phút) → tự khóa lại đúng, quay về `PinEntry`; mở khóa lại bình thường sau đó.
- Rủi ro đã nêu trong plan — `BiometricPrompt` có gây `onStop()` giả khiến auto-lock nhầm không — đã verify sống bằng cách hạ tạm `DEFAULT_AUTO_LOCK_TIMEOUT` xuống 5s, giữ `BiometricPrompt` mở 8s rồi mới xác thực vân tay: thiết lập sinh trắc học vẫn thành công, không bị auto-lock giữa chừng → xác nhận `BiometricPrompt` không trigger `onStop()` của `MainActivity`. Đã trả lại đúng giá trị 1 phút trước khi build/commit cuối.

`/code-review` (trước commit): tự phát hiện 1 lỗi Major trong lúc test sống — `switchToMasterPassword()`/`switchToPin()` không truyền `lockedUntilMillis` hiện tại vào state mới, khiến màn hình vừa chuyển sang không hiện đếm ngược (dù `unlock()`/`unlockWithPin()` vẫn chặn đúng ở lượt bấm tiếp theo) — đã sửa bằng cách đọc `lockoutPolicy.currentLockoutUntilMillis()` ở cả 2 hàm, build lại xanh, verify lại xác nhận đếm ngược hiện đúng ngay khi chuyển màn.

## Risks

- Test auto-lock thật cần chờ đủ 1 phút (giá trị mặc định) mỗi lần — tốn thời gian test sống. Giảm thiểu: có thể tạm thời set `DEFAULT_AUTO_LOCK_TIMEOUT` xuống vài giây chỉ trong lúc test cục bộ rồi trả lại đúng giá trị spec trước khi commit (không được commit giá trị test).
- `onStop()` cũng được gọi khi xoay màn hình hoặc app bị che bởi dialog hệ thống (vd. `BiometricPrompt` cũng chạy trong 1 Activity/Fragment riêng có thể trigger `onStop` của `MainActivity`) — cần kiểm tra kỹ để không vô tình auto-lock ngay giữa lúc đang xác thực sinh trắc học. Sẽ verify sống thao tác mở khóa bằng sinh trắc học sau khi có Feature 4 để đảm bảo không bị khóa nhầm.
- Lockout dùng `SharedPreferences` (không mã hóa) để lưu bộ đếm — chấp nhận được vì đây không phải dữ liệu bí mật (chỉ là số lần sai + timestamp), không ảnh hưởng zero-knowledge model.

---

## Questions

*(đã đề xuất phương án — chỉ hỏi lại nếu muốn đổi)*

- **Sinh trắc học có tính vào bộ đếm sai chung không?** 👉 Không — hệ điều hành (BiometricPrompt/HAL) đã tự giới hạn số lần thử sinh trắc học, tính thêm vào bộ đếm app dễ gây khóa nhầm người dùng hợp lệ chỉ vì vân tay ướt/bẩn vài lần.
- **`onStop()` do xoay màn hình/dialog hệ thống có làm auto-lock nhầm không?** 👉 Không — `onStop()` của `ComponentActivity`/`FragmentActivity` chỉ gọi khi Activity thật sự không còn hiển thị (bị che hoàn toàn hoặc đưa xuống nền), xoay màn hình gọi `onPause`→`onStop`→(destroy+recreate)→`onStart`→`onResume` rất nhanh (thường < 1s) nên không đủ để vượt ngưỡng auto-lock tối thiểu (30s). `BiometricPrompt` chạy trong cùng Activity (không phải Activity riêng) nên không trigger `onStop` của `MainActivity`. Sẽ verify sống để chắc chắn.
- **Test nhanh không cần chờ đủ 1 phút:** 👉 Sửa tạm `DEFAULT_AUTO_LOCK_TIMEOUT` xuống 5-10 giây chỉ trong lúc test cục bộ trên emulator, trả lại giá trị đúng spec (1 phút) trước khi build cuối + commit.
