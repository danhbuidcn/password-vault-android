# pwvault-android

Ứng dụng Android quản lý mật khẩu cá nhân, chạy hoàn toàn offline. Chi tiết nghiệp vụ/kiến trúc xem [docs/overview.md](docs/overview.md) và [docs/architecture.md](docs/architecture.md).

## Screenshots

_(Sẽ bổ sung ảnh chụp màn hình sau khi hoàn thành app.)_

## Cài đặt (Download)

App chưa phát hành bản release — sẽ đăng file `.apk` kèm ghi chú version/ngày build tại [GitHub Releases](https://github.com/danhbuidcn/password-vault-android/releases) sau khi hoàn thành roadmap. Quy trình publish: [docs/dev-setup.md#publish-github-release](docs/dev-setup.md#publish-github-release).

## Quick start

Yêu cầu: JDK 21, Android SDK, thiết bị Android thật (minSdk 26+) qua USB. Setup môi trường lần đầu + hướng dẫn chạy trên emulator (khi không có thiết bị thật) xem [docs/dev-setup.md](docs/dev-setup.md).

```bash
./gradlew installDebug
adb shell am start -n com.pwvault.app/.MainActivity
```

## Verify trước khi commit

```bash
./gradlew ktlintCheck detekt lint testDebugUnitTest assembleDebug
```

Rule/code style áp dụng cho project này nằm ở kho trung tâm — xem [docs/manifest.md](docs/manifest.md) (load map) và `AI/kit/context/lang/kotlin.md` + `AI/kit/context/security.md`.

## Build file cài đặt (APK)

App chỉ dùng cho 1 người (side-load, không đăng Play Store). Chưa có `signingConfig` cho release nên dùng bản debug-signed làm file cài — chi tiết lệnh xem [docs/dev-setup.md](docs/dev-setup.md#build-file-cài-đặt-apk-cho-người-dùng-cuối).

## Tài liệu dự án

- [docs/overview.md](docs/overview.md) — tổng quan nghiệp vụ
- [docs/functional-spec.md](docs/functional-spec.md) — spec nguồn
- [docs/architecture.md](docs/architecture.md) — kiến trúc & quyết định kỹ thuật (bao gồm [cấu trúc project](docs/architecture.md#project-structure))
- [docs/flow.md](docs/flow.md) — cơ chế hoạt động: vòng đời app, luồng mở khóa/khóa, luồng dữ liệu (CRUD, backup, import)
- [docs/glossary.md](docs/glossary.md) — thuật ngữ
- [docs/manifest.md](docs/manifest.md) — stack + rule load map cho `/code-plan`, `/code-guard`
- [docs/dev-setup.md](docs/dev-setup.md) — setup môi trường dev, chạy trên thiết bị thật/emulator, build APK
- [docs/plans/](docs/plans/) — implementation plan theo từng task
