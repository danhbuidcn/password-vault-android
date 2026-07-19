package com.pwvault.app.security

import android.content.Context

private const val PREFS_NAME = "lockout_state"
private const val KEY_ATTEMPT_COUNT = "attempt_count"
private const val KEY_LOCKED_UNTIL_MILLIS = "locked_until_millis"

/**
 * Persists the wrong-attempt counter and lockout expiry. Must survive process death/app restart
 * — otherwise force-stopping the app would bypass the lockout (see
 * docs/plans/feature-04-autolock-lockout-plan.md).
 */
class LockoutStore(
    context: Context,
) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getAttemptCount(): Int = prefs.getInt(KEY_ATTEMPT_COUNT, 0)

    fun getLockedUntilMillis(): Long = prefs.getLong(KEY_LOCKED_UNTIL_MILLIS, 0L)

    fun save(
        attemptCount: Int,
        lockedUntilMillis: Long,
    ) {
        prefs
            .edit()
            .putInt(KEY_ATTEMPT_COUNT, attemptCount)
            .putLong(KEY_LOCKED_UNTIL_MILLIS, lockedUntilMillis)
            .apply()
    }
}
