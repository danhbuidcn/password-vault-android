package com.pwvault.app.security

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat

enum class AppLanguage { SYSTEM, EN, VI }

/**
 * Wraps the AndroidX per-app language API. `AppCompatDelegate` persists the choice and applies it
 * to every Activity itself (including recreating the current one) — no local storage needed here.
 */
class LanguagePreferences {
    fun getLanguage(): AppLanguage =
        when (AppCompatDelegate.getApplicationLocales().toLanguageTags()) {
            "en" -> AppLanguage.EN
            "vi" -> AppLanguage.VI
            else -> AppLanguage.SYSTEM
        }

    fun setLanguage(language: AppLanguage) {
        val locales =
            when (language) {
                AppLanguage.SYSTEM -> LocaleListCompat.getEmptyLocaleList()
                AppLanguage.EN -> LocaleListCompat.forLanguageTags("en")
                AppLanguage.VI -> LocaleListCompat.forLanguageTags("vi")
            }
        AppCompatDelegate.setApplicationLocales(locales)
    }
}
