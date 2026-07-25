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

    /** How long a copied password stays on the clipboard before it's overwritten. */
    val CLIPBOARD_CLEAR_DELAY: Duration = 30.seconds

    /** Length of a suggested password from [com.pwvault.app.security.PasswordGenerator]. */
    const val GENERATED_PASSWORD_LENGTH = 8

    /** How long a plaintext CSV export temp file survives in [android.content.Context.getCacheDir] before deletion. */
    val EXPORT_TEMP_FILE_CLEANUP_DELAY: Duration = 5.minutes

    /** Below this length, a Vault Item password is flagged weak — see [com.pwvault.app.security.PasswordStrength]. */
    const val WEAK_PASSWORD_MIN_LENGTH = 8

    /** Below this many distinct character classes (upper/lower/digit/special), a password is flagged weak. */
    const val WEAK_PASSWORD_MIN_CHAR_CLASSES = 3

    /** Max `pwvault-auto-*.pwvbackup` files kept in the user-picked auto-backup folder. */
    const val AUTO_BACKUP_MAX_FILES = 5

    /** Remind the user to export manually if this many days pass with no manual export — see `ExportReminderWorker`. */
    const val EXPORT_REMINDER_INTERVAL_DAYS = 30
}
