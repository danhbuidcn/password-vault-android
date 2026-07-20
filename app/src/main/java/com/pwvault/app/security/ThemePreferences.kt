package com.pwvault.app.security

import android.content.Context

private const val PREFS_NAME = "theme_preferences"
private const val KEY_MODE = "mode"

enum class ThemeMode { LIGHT, DARK, SYSTEM }

/** Persists the user's chosen theme — `SYSTEM` (follow device setting) is the default. */
class ThemePreferences(
    context: Context,
) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getMode(): ThemeMode = prefs.getString(KEY_MODE, null)?.let { ThemeMode.valueOf(it) } ?: ThemeMode.SYSTEM

    fun setMode(mode: ThemeMode) {
        prefs.edit().putString(KEY_MODE, mode.name).apply()
    }
}
