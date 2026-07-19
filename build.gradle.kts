// org.jetbrains.kotlin.android is intentionally NOT applied: AGP 9+ has built-in Kotlin support
// and rejects that plugin (https://developer.android.com/build/releases/agp-9-0-0-release-notes).
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.hilt) apply false
    alias(libs.plugins.ktlint) apply false
    alias(libs.plugins.detekt) apply false
}
