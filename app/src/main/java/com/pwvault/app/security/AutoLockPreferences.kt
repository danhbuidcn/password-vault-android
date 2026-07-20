package com.pwvault.app.security

import android.content.Context
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

private const val PREFS_NAME = "auto_lock_preferences"
private const val KEY_TIMEOUT_MILLIS = "timeout_millis"

/** `-1` sentinel means "Never" (see [SecurityPolicy.AUTO_LOCK_TIMEOUT_OPTIONS]). */
private const val NEVER_SENTINEL = -1L

/** Persists the user's chosen auto-lock timeout — read/written from `SettingsScreen`. */
class AutoLockPreferences(
    context: Context,
) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /** `null` means "Never" — auto-lock is disabled. */
    fun getTimeout(): Duration? {
        val millis = prefs.getLong(KEY_TIMEOUT_MILLIS, SecurityPolicy.DEFAULT_AUTO_LOCK_TIMEOUT.inWholeMilliseconds)
        return if (millis == NEVER_SENTINEL) null else millis.milliseconds
    }

    /** Pass `null` for "Never". */
    fun setTimeout(timeout: Duration?) {
        val millis = timeout?.inWholeMilliseconds ?: NEVER_SENTINEL
        prefs.edit().putLong(KEY_TIMEOUT_MILLIS, millis).apply()
    }
}
