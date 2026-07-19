package com.pwvault.app.security

import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

/**
 * Single source of truth for security-relevant thresholds — never hardcode these as
 * `private const val` in individual files. See docs/plans/feature-04-autolock-lockout-plan.md.
 */
object SecurityPolicy {
    /** `null` means "Never" — the Vault only locks when the process dies. */
    val AUTO_LOCK_TIMEOUT_OPTIONS: List<Duration?> =
        listOf(30.seconds, 1.minutes, 5.minutes, 15.minutes, null)
    val DEFAULT_AUTO_LOCK_TIMEOUT: Duration = 1.minutes

    const val MAX_ATTEMPTS_BEFORE_LOCKOUT = 5
    val INITIAL_LOCKOUT_DURATION: Duration = 30.seconds
    val MAX_LOCKOUT_DURATION: Duration = 30.minutes
}
