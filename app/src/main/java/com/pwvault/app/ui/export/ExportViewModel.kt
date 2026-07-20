package com.pwvault.app.ui.export

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pwvault.app.data.VaultFileManager
import com.pwvault.app.data.VaultItemRepository
import com.pwvault.app.export.CsvExporter
import com.pwvault.app.export.ExportTempFileCleaner
import com.pwvault.app.export.PasswordZipWriter
import com.pwvault.app.security.BackupPreferences
import com.pwvault.app.ui.unlock.MIN_PASSWORD_LENGTH
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Arrays
import java.util.Date
import java.util.Locale
import javax.inject.Inject

private const val WIPE_CHAR = ' '
private const val BACKUP_ENTRY_NAME = "vault-export.csv"

enum class ExportTarget { BACKUP, CSV }

enum class ExportError {
    WRONG_MASTER_PASSWORD,
    ZIP_PASSWORD_MISMATCH,
    ZIP_PASSWORD_TOO_SHORT,
    WRITE_FAILED,
}

sealed interface ExportUiState {
    data object Closed : ExportUiState

    data object Choice : ExportUiState

    data class Reauth(
        val target: ExportTarget,
        val error: ExportError? = null,
        val busy: Boolean = false,
    ) : ExportUiState

    data class CsvWarning(
        val ack1: Boolean = false,
        val ack2: Boolean = false,
    ) : ExportUiState

    data class CsvPassword(
        val error: ExportError? = null,
    ) : ExportUiState

    data class PickDestination(
        val target: ExportTarget,
        val suggestedFileName: String,
    ) : ExportUiState

    data class Writing(
        val target: ExportTarget,
    ) : ExportUiState

    data object Done : ExportUiState

    data class Failed(
        val error: ExportError,
    ) : ExportUiState
}

@HiltViewModel
class ExportViewModel
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
        private val vaultFileManager: VaultFileManager,
        private val vaultItemRepository: VaultItemRepository,
        private val csvExporter: CsvExporter,
        private val passwordZipWriter: PasswordZipWriter,
        private val exportTempFileCleaner: ExportTempFileCleaner,
        private val backupPreferences: BackupPreferences,
    ) : ViewModel() {
        private val _state = MutableStateFlow<ExportUiState>(ExportUiState.Closed)
        val state: StateFlow<ExportUiState> = _state.asStateFlow()

        private var pendingZipPassword: CharArray? = null

        fun open() {
            _state.value = ExportUiState.Choice
        }

        fun close() {
            pendingZipPassword?.let { Arrays.fill(it, WIPE_CHAR) }
            pendingZipPassword = null
            _state.value = ExportUiState.Closed
        }

        fun chooseTarget(target: ExportTarget) {
            _state.value = ExportUiState.Reauth(target)
        }

        fun submitMasterPassword(
            password: CharArray,
            verify: suspend (CharArray) -> Boolean,
        ) {
            val current = _state.value as? ExportUiState.Reauth ?: return
            _state.value = current.copy(busy = true, error = null)
            viewModelScope.launch {
                if (verify(password)) {
                    _state.value =
                        if (current.target == ExportTarget.BACKUP) {
                            ExportUiState.PickDestination(ExportTarget.BACKUP, suggestedFileName(".pwvbackup"))
                        } else {
                            ExportUiState.CsvWarning()
                        }
                } else {
                    _state.value = current.copy(busy = false, error = ExportError.WRONG_MASTER_PASSWORD)
                }
            }
        }

        fun toggleAck1() {
            val current = _state.value as? ExportUiState.CsvWarning ?: return
            _state.value = current.copy(ack1 = !current.ack1)
        }

        fun toggleAck2() {
            val current = _state.value as? ExportUiState.CsvWarning ?: return
            _state.value = current.copy(ack2 = !current.ack2)
        }

        fun continueFromWarning() {
            val current = _state.value as? ExportUiState.CsvWarning ?: return
            if (!current.ack1 || !current.ack2) return
            _state.value = ExportUiState.CsvPassword()
        }

        fun submitZipPassword(
            password: CharArray,
            confirm: CharArray,
        ) {
            if (!password.contentEquals(confirm)) {
                Arrays.fill(password, WIPE_CHAR)
                Arrays.fill(confirm, WIPE_CHAR)
                _state.value = ExportUiState.CsvPassword(error = ExportError.ZIP_PASSWORD_MISMATCH)
                return
            }
            Arrays.fill(confirm, WIPE_CHAR)
            if (password.size < MIN_PASSWORD_LENGTH) {
                Arrays.fill(password, WIPE_CHAR)
                _state.value = ExportUiState.CsvPassword(error = ExportError.ZIP_PASSWORD_TOO_SHORT)
                return
            }
            pendingZipPassword = password
            _state.value = ExportUiState.PickDestination(ExportTarget.CSV, suggestedFileName(".zip"))
        }

        fun onDestinationPicked(
            target: ExportTarget,
            uri: Uri?,
        ) {
            if (uri == null) {
                close()
                return
            }
            _state.value = ExportUiState.Writing(target)
            viewModelScope.launch {
                val success =
                    if (target == ExportTarget.BACKUP) writeBackup(uri) else writeCsv(uri)
                if (success) backupPreferences.recordManualExportNow()
                _state.value = if (success) ExportUiState.Done else ExportUiState.Failed(ExportError.WRITE_FAILED)
            }
        }

        private suspend fun writeBackup(uri: Uri): Boolean =
            withContext(Dispatchers.IO) {
                runCatching {
                    context.contentResolver.openOutputStream(uri)?.use { out -> vaultFileManager.copyVaultFileTo(out) }
                        ?: error("Couldn't open destination")
                }.isSuccess
            }

        private suspend fun writeCsv(uri: Uri): Boolean {
            val password = pendingZipPassword
            if (password == null) return false
            val exportDir = File(context.cacheDir, "export").apply { mkdirs() }
            val timestamp = fileTimestamp()
            val csvFile = File(exportDir, "vault-export-$timestamp.csv")
            val zipFile = File(exportDir, "vault-export-$timestamp.zip")
            return withContext(Dispatchers.IO) {
                try {
                    val items = vaultItemRepository.observeItems().first()
                    csvFile.writeText(csvExporter.toCsv(items))
                    passwordZipWriter.writeSingleEntryZip(csvFile, BACKUP_ENTRY_NAME, password, zipFile)
                    context.contentResolver.openOutputStream(uri)?.use { out ->
                        zipFile.inputStream().use { it.copyTo(out) }
                    } ?: error("Couldn't open destination")
                    true
                } catch (_: java.io.IOException) {
                    false
                } catch (_: net.lingala.zip4j.exception.ZipException) {
                    false
                } finally {
                    exportTempFileCleaner.scheduleDelete(csvFile)
                    exportTempFileCleaner.scheduleDelete(zipFile)
                    pendingZipPassword = null
                }
            }
        }

        private fun suggestedFileName(extension: String): String = "pwvault-export-${fileTimestamp()}$extension"

        private fun fileTimestamp(): String = SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US).format(Date())
    }
