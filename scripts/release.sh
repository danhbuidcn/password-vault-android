#!/usr/bin/env bash
# One-command release, run on the machine holding the app's signing key (the laptop):
#   pull latest main → check signing key → build APK → tag v<versionName> → GitHub Release with the APK.
# Safe to re-run: an existing tag/release for the same commit is reused (the APK is re-uploaded).
#
# Usage: scripts/release.sh
set -euo pipefail
cd "$(dirname "$0")/.."

# SHA-256 of the dedicated release key (keystore.properties, not the debug key) every published
# APK is signed with from here on. Android refuses to update an installed app from an APK signed
# with a different key, so building with any other key would produce an APK the phone can't
# install over the existing app. The first release built with this key is a one-time exception:
# it replaces the earlier debug-signed releases, so it requires uninstall + reinstall on phones
# that still have an old build (export vault data first).
EXPECTED_CERT_SHA256="d30a8295557d838ca47d013b1d1ebf905d23638363a6a525aa63b5f374fd4aa1"

fail() {
  echo "❌ $*" >&2
  exit 1
}

# --- Preconditions ---------------------------------------------------------------------------
command -v gh >/dev/null || fail "Chưa cài GitHub CLI: sudo apt install -y gh && gh auth login"
gh auth status >/dev/null 2>&1 || fail "Chưa đăng nhập GitHub CLI: gh auth login"
[ "$(git rev-parse --abbrev-ref HEAD)" = "main" ] || fail "Phải đứng ở nhánh main (git checkout main)."
git diff --quiet && git diff --cached --quiet || fail "Có thay đổi chưa commit — commit/stash trước khi release."

# --- 1. Update code --------------------------------------------------------------------------
echo "▶ Cập nhật code mới nhất..."
git pull --ff-only origin main
git fetch --tags origin

# --- 2. Signing key check --------------------------------------------------------------------
echo "▶ Kiểm tra khoá ký..."
[ -f keystore.properties ] || fail "Không có keystore.properties (cp từ máy giữ khoá ký, không commit vào git)."
set -a; . ./keystore.properties; set +a
: "${KEYSTORE_BASE64:?Thiếu KEYSTORE_BASE64 trong keystore.properties}"
: "${KEYSTORE_PASSWORD:?Thiếu KEYSTORE_PASSWORD trong keystore.properties}"
: "${KEY_ALIAS:?Thiếu KEY_ALIAS trong keystore.properties}"

KEYTOOL="$(command -v keytool || true)"
[ -z "$KEYTOOL" ] && [ -n "${JAVA_HOME:-}" ] && KEYTOOL="${JAVA_HOME}/bin/keytool"
[ -x "$KEYTOOL" ] || fail "Không tìm thấy keytool (cài JDK hoặc đặt JAVA_HOME)."

KEYSTORE_TMP=$(mktemp -d); trap 'rm -rf "$KEYSTORE_TMP"' EXIT
export PWVAULT_KEYSTORE_FILE="$KEYSTORE_TMP/release.jks"
echo "$KEYSTORE_BASE64" | base64 -d > "$PWVAULT_KEYSTORE_FILE"
export PWVAULT_KEYSTORE_PASSWORD="$KEYSTORE_PASSWORD"
export PWVAULT_KEY_ALIAS="$KEY_ALIAS"

ACTUAL_CERT_SHA256="$("$KEYTOOL" -list -v -keystore "$PWVAULT_KEYSTORE_FILE" -storepass "$KEYSTORE_PASSWORD" -alias "$KEY_ALIAS" \
  | grep -m1 'SHA256:' | sed 's/.*SHA256: *//; s/://g' | tr 'A-F' 'a-f')"
[ "$ACTUAL_CERT_SHA256" = "$EXPECTED_CERT_SHA256" ] \
  || fail "Khoá ký sai (${ACTUAL_CERT_SHA256:0:8}…, cần ${EXPECTED_CERT_SHA256:0:8}…) — APK sẽ không cài đè được lên điện thoại."

# --- 3. Version + tag sanity -----------------------------------------------------------------
VERSION="$(grep -oP '(?<=versionName = ")[^"]+' app/build.gradle.kts)"
TAG="v${VERSION}"
HEAD_SHA="$(git rev-parse HEAD)"
if git rev-parse -q --verify "refs/tags/${TAG}" >/dev/null; then
  [ "$(git rev-list -n1 "$TAG")" = "$HEAD_SHA" ] \
    || fail "Tag ${TAG} đã có nhưng trỏ commit khác — cần bump versionName trong app/build.gradle.kts."
fi

# --- 4. Build --------------------------------------------------------------------------------
echo "▶ Build APK ${VERSION}..."
./gradlew assembleRelease
mkdir -p dist
APK="dist/pwvault-android-${VERSION}-$(date +%Y%m%d).apk"
cp app/build/outputs/apk/release/app-release.apk "$APK"

# --- 5. Tag ----------------------------------------------------------------------------------
if ! git rev-parse -q --verify "refs/tags/${TAG}" >/dev/null; then
  echo "▶ Tạo tag ${TAG}..."
  git tag -a "$TAG" -m "${TAG}: $(git log -1 --format=%s)"
fi
git push origin "$TAG"

# --- 6. GitHub Release -----------------------------------------------------------------------
NOTES="$(git tag -l --format='%(contents)' "$TAG")

Version: ${VERSION}
Ngày build: $(date +%Y-%m-%d)
Yêu cầu: Android 8.0 (API 26) trở lên
Cài đặt: tải file .apk bên dưới → mở bằng File Manager trên điện thoại → bật \"Cài ứng dụng từ nguồn không xác định\" nếu được hỏi → Install (cài đè, giữ nguyên dữ liệu)."

if gh release view "$TAG" >/dev/null 2>&1; then
  echo "▶ Release ${TAG} đã có — upload đè APK..."
  gh release upload "$TAG" "$APK" --clobber
else
  echo "▶ Tạo GitHub Release ${TAG}..."
  gh release create "$TAG" "$APK" --title "pwvault-android ${TAG}" --notes "$NOTES"
fi

echo "✅ Xong: $(gh release view "$TAG" --json url -q .url)"
echo "   APK: ${APK}"
