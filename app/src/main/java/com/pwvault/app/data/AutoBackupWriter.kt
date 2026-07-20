package com.pwvault.app.data

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.pwvault.app.security.BackupPreferences
import com.pwvault.app.security.SecurityPolicy
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val MIME_TYPE = "application/octet-stream"
private const val TEMP_NAME = "pwvault-auto.tmp"
private const val NAME_PREFIX = "pwvault-auto-"
private const val NAME_SUFFIX = ".pwvbackup"

/**
 * Writes an auto-backup copy of the Vault after every item change — `functional-spec.md §7.1`.
 * No re-authentication needed: [VaultFileManager.copyVaultFileTo] just copies the already-encrypted
 * `vault.db` bytes, it never touches the Vault key.
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
        val finalName = "$NAME_PREFIX${timestamp()}$NAME_SUFFIX"

        val written =
            context.contentResolver.openOutputStream(temp.uri)?.use { out ->
                vaultFileManager.copyVaultFileTo(out)
                true
            } ?: false
        if (!written || !temp.renameTo(finalName)) {
            temp.delete()
            return
        }
        rotate(folder)
    }

    private fun rotate(folder: DocumentFile) {
        val backups =
            folder
                .listFiles()
                .filter { it.name?.startsWith(NAME_PREFIX) == true && it.name?.endsWith(NAME_SUFFIX) == true }
                .sortedByDescending { it.name }
        backups.drop(SecurityPolicy.AUTO_BACKUP_MAX_FILES).forEach { it.delete() }
    }

    private fun timestamp(): String = SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US).format(Date())
}
