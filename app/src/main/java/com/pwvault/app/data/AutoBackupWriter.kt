package com.pwvault.app.data

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.pwvault.app.security.BackupPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

private const val MIME_TYPE = "application/octet-stream"
private const val TEMP_NAME = "pwvault-auto.tmp"
private const val FINAL_NAME = "pwvault-auto-backup.pwvbackup"

/**
 * Writes an auto-backup copy of the Vault after every item change — `functional-spec.md §7.1`.
 * No re-authentication needed: [VaultFileManager.copyVaultFileTo] just copies the already-encrypted
 * `vault.db` bytes, it never touches the Vault key.
 *
 * Overwrites a single fixed-name file each time rather than keeping a rotating history — the caller
 * edits items often enough that a per-edit file history is mostly noise in the picked folder.
 *
 * Runs on its own process-lifetime scope rather than the caller's `viewModelScope` — same reasoning
 * as [com.pwvault.app.security.ClipboardClearer]: a save/delete's coroutine can return to the UI
 * before this finishes, and the ViewModel could be cleared in between.
 */
class AutoBackupWriter(
    private val context: Context,
    private val vaultFileManager: VaultFileManager,
    private val backupPreferences: BackupPreferences,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // Rapid successive item changes (e.g. two quick edits) can each call scheduleBackup() before the
    // previous write finishes. Without serializing, both coroutines would share the same TEMP_NAME
    // file and could delete/overwrite each other's in-flight write, or race in rotate()'s list+delete.
    private val writeMutex = Mutex()

    fun scheduleBackup() {
        val folderUri = backupPreferences.getAutoBackupFolderUri() ?: return
        scope.launch {
            runCatching { writeMutex.withLock { writeBackup(folderUri) } }
        }
    }

    private suspend fun writeBackup(folderUri: Uri) {
        val folder = DocumentFile.fromTreeUri(context, folderUri) ?: return
        folder.findFile(TEMP_NAME)?.delete()
        val temp = folder.createFile(MIME_TYPE, TEMP_NAME) ?: return

        val written =
            context.contentResolver.openOutputStream(temp.uri)?.use { out ->
                vaultFileManager.copyVaultFileTo(out)
                true
            } ?: false
        if (!written) {
            temp.delete()
            return
        }
        folder.findFile(FINAL_NAME)?.delete()
        if (!temp.renameTo(FINAL_NAME)) temp.delete()
    }
}
