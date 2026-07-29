package com.pwvault.app.security

import android.app.LocaleManager
import android.content.Context
import android.os.Build
import android.os.LocaleList
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import java.util.Locale

enum class AppLanguage { EN, VI }

/**
 * Per-app language. On API 33+, calls the platform `LocaleManager` directly — verified more
 * reliable than `AppCompatDelegate.setApplicationLocales()` alone, which in this app (no
 * `AppCompatActivity`) was observed to leave the platform-level per-app locale unset. Below API
 * 33, falls back to `AppCompatDelegate`'s own storage (the platform API doesn't exist there).
 *
 * Only English/Vietnamese are offered — no "System" option. Before the user ever picks one
 * explicitly, the current display language (already resolved from the device locale by Android
 * resource loading) is used as the default.
 */
class LanguagePreferences(
    private val context: Context,
) {
    fun getLanguage(): AppLanguage {
        val languageTag = currentLocales().firstOrNull()?.language ?: Locale.getDefault().language
        return if (languageTag == "vi") AppLanguage.VI else AppLanguage.EN
    }

    fun setLanguage(language: AppLanguage) {
        val tag =
            when (language) {
                AppLanguage.EN -> "en"
                AppLanguage.VI -> "vi"
            }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            setPlatformLocales(tag)
        } else {
            AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(tag))
        }
    }

    private fun currentLocales(): List<Locale> =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            platformLocales()
        } else {
            val compat = AppCompatDelegate.getApplicationLocales()
            (0 until compat.size()).mapNotNull { compat[it] }
        }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private fun setPlatformLocales(languageTag: String) {
        context.getSystemService(LocaleManager::class.java).applicationLocales =
            LocaleList.forLanguageTags(languageTag)
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private fun platformLocales(): List<Locale> {
        val list = context.getSystemService(LocaleManager::class.java).applicationLocales
        return (0 until list.size()).mapNotNull { list[it] }
    }
}
