# Manifest

> Đọc bởi `/code-plan` và `/code-guard`: khai báo stack của repo này + map domain → lớp context generic (kho trung tâm
> `~/Documents/projects/AI/kit/context/`) cần nạp khi làm task đúng domain đó. Xem quy ước ở
> [ai-working-blueprint.md](/home/thuongbv/Documents/projects/AI/ai-working-blueprint.md).

## Stack

- **Vai trò:** Mobile developer (Android), làm việc một mình / không backend riêng — app offline hoàn toàn.
- **Ngôn ngữ:** Kotlin.
- **UI:** Jetpack Compose.
- **Local DB:** Room + SQLCipher (SQL, mã hóa).
- **Platform:** Android, minSdk 26.
- Chi tiết đầy đủ: [architecture.md](architecture.md).

---

## Load map (domain → lớp generic cần nạp)

| Domain / Task | Điều kiện | Nạp |
|---|---|---|
| Mọi thay đổi code | luôn | `common.md` |
| Data layer (Room entity/DAO, SQLCipher, migration) | sửa `data/` | + `sql.md` |
| UI Compose (screen, component, theme) | sửa `ui/` | + `frontend.md` ⚠️ file này viết theo tinh thần web (mockup matching: font/spacing/text/hover) — phần hover không áp dụng cho mobile, phần font/spacing/text/xuống dòng vẫn dùng được khi so khớp mockup Compose. |
| Code Kotlin nói chung (idiom, coroutine, Compose, null-safety) | mọi file `.kt`/`.kts` | + `lang/kotlin.md` |
| Code đụng Master Password / khóa mã hóa / nội dung Vault / backup file | xử lý dữ liệu nhạy cảm | + `security.md` |

---

## Verify

✅ Đã chạy thật (2026-07-19), project Gradle scaffold `assembleDebug` build xanh:

- Full verify: `./gradlew ktlintCheck detekt lint testDebugUnitTest assembleDebug`
- Cài lên thiết bị thật (cắm USB, xem qua `scrcpy`): `./gradlew installDebug`
- Build release APK: `./gradlew assembleRelease` — ⚠️ chưa có `signingConfig` (ngoài scope plan scaffold), ra APK unsigned, chưa cài trực tiếp được. Dùng bản debug-signed (`assembleDebug`) làm file cài tạm cho tới khi có plan ký release riêng.

Môi trường build đã cài trên máy dev (JDK 21 + Android SDK cmdline-tools, không qua apt/sudo) — chi tiết ở [README.md](../README.md).

---

## Related Documents

- [overview.md](overview.md)
- [architecture.md](architecture.md)
- [glossary.md](glossary.md)
