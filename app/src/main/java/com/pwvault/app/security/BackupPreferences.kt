package com.pwvault.app.security

import android.content.Context
import android.net.Uri

private const val PREFS_NAME = "backup_preferences"
private const val KEY_AUTO_BACKUP_FOLDER_URI = "auto_backup_folder_uri"
private const val KEY_LAST_MANUAL_EXPORT_AT_MILLIS = "last_manual_export_at_millis"

/**
 * Persists auto-backup/export bookkeeping — the user-picked SAF folder for auto-backup (Feature 13)
 * and the timestamp of the last successful manual export (Feature 12), read by [ExportReminderWorker].
 * Constructed directly (not via Hilt) where needed outside the DI graph, same as [AutoLockPreferences].
 */
class BackupPreferences(
    context: Context,
) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getAutoBackupFolderUri(): Uri? = prefs.getString(KEY_AUTO_BACKUP_FOLDER_URI, null)?.let(Uri::parse)

    fun setAutoBackupFolderUri(uri: Uri) {
        prefs.edit().putString(KEY_AUTO_BACKUP_FOLDER_URI, uri.toString()).apply()
    }

    /** Turns auto-backup off. Existing backup files already written to the folder are left in place. */
    fun clearAutoBackupFolderUri() {
        prefs.edit().remove(KEY_AUTO_BACKUP_FOLDER_URI).apply()
    }

    /**
     * `null` only before [seedReminderClockAtVaultCreation] has ever run — in practice that means
     * right at Setup, so [ExportReminderWorker] should never actually see `null` once a vault exists.
     */
    fun getLastManualExportAtMillis(): Long? {
        val value = prefs.getLong(KEY_LAST_MANUAL_EXPORT_AT_MILLIS, -1L)
        return if (value == -1L) null else value
    }

    fun recordManualExportNow() = resetReminderClock()

    /**
     * Called once from `UnlockViewModel.createVault()` so the 30-day reminder clock starts at vault
     * creation — without this, [getLastManualExportAtMillis] would stay `null` until the user's first
     * manual export, and [ExportReminderWorker] would nag on its very first run the next day instead
     * of waiting the intended 30 days.
     */
    fun seedReminderClockAtVaultCreation() = resetReminderClock()

    private fun resetReminderClock() {
        prefs.edit().putLong(KEY_LAST_MANUAL_EXPORT_AT_MILLIS, System.currentTimeMillis()).apply()
    }
}
