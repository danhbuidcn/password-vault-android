package com.pwvault.app.security

import android.content.Context
import android.content.res.Configuration

private const val PREFS_NAME = "theme_preferences"
private const val KEY_MODE = "mode"

enum class ThemeMode { LIGHT, DARK }

/**
 * Persists the user's chosen theme. Only Light/Dark are offered — no "System" option. Before the
 * user ever picks one explicitly, the device's current dark/light setting is used as the default.
 */
class ThemePreferences(
    private val context: Context,
) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getMode(): ThemeMode {
        val explicit = prefs.getString(KEY_MODE, null)?.let { ThemeMode.valueOf(it) }
        if (explicit != null) return explicit
        val nightMode = context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
        return if (nightMode == Configuration.UI_MODE_NIGHT_YES) ThemeMode.DARK else ThemeMode.LIGHT
    }

    fun setMode(mode: ThemeMode) {
        prefs.edit().putString(KEY_MODE, mode.name).apply()
    }
}
