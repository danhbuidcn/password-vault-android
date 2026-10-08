package com.pwvault.app.ui.export

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pwvault.app.data.VaultItemRepository
import com.pwvault.app.export.CsvExporter
import com.pwvault.app.security.BackupPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

enum class ExportError {
    WRONG_PIN,
    WRITE_FAILED,
}

sealed interface ExportUiState {
    data object Closed : ExportUiState

    data class Reauth(
        val error: ExportError? = null,
        val busy: Boolean = false,
    ) : ExportUiState

    data class PickDestination(
        val suggestedFileName: String,
    ) : ExportUiState

    data object Writing : ExportUiState

    data object Done : ExportUiState

    data class Failed(
        val error: ExportError,
    ) : ExportUiState
}

/**
 * Export = PIN re-auth → pick destination → write a plain (unencrypted) CSV that can be imported
 * back on another device — see docs/plans/feature-19-simplify-unlock-csv-plan.md.
 */
@HiltViewModel
class ExportViewModel
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
        private val vaultItemRepository: VaultItemRepository,
        private val csvExporter: CsvExporter,
        private val backupPreferences: BackupPreferences,
    ) : ViewModel() {
        private val _state = MutableStateFlow<ExportUiState>(ExportUiState.Closed)
        val state: StateFlow<ExportUiState> = _state.asStateFlow()

        fun open() {
            _state.value = ExportUiState.Reauth()
        }

        fun close() {
            _state.value = ExportUiState.Closed
        }

        fun submitPin(
            pin: CharArray,
            verify: suspend (CharArray) -> Boolean,
        ) {
            val current = _state.value as? ExportUiState.Reauth ?: return
            _state.value = current.copy(busy = true, error = null)
            viewModelScope.launch {
                _state.value =
                    if (verify(pin)) {
                        ExportUiState.PickDestination(suggestedFileName())
                    } else {
                        current.copy(busy = false, error = ExportError.WRONG_PIN)
                    }
            }
        }

        fun onDestinationPicked(uri: Uri?) {
            if (_state.value !is ExportUiState.PickDestination) return
            if (uri == null) {
                close()
                return
            }
            _state.value = ExportUiState.Writing
            viewModelScope.launch {
                val success = writeCsv(uri)
                if (success) backupPreferences.recordManualExportNow()
                _state.value = if (success) ExportUiState.Done else ExportUiState.Failed(ExportError.WRITE_FAILED)
            }
        }

        private suspend fun writeCsv(uri: Uri): Boolean =
            withContext(Dispatchers.IO) {
                runCatching {
                    val csv = csvExporter.toCsv(vaultItemRepository.observeItems().first())
                    context.contentResolver.openOutputStream(uri)?.use { out -> out.write(csv.toByteArray()) }
                        ?: error("Couldn't open destination")
                }.isSuccess
            }

        private fun suggestedFileName(): String =
            "pwvault-export-${SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US).format(Date())}.csv"
    }
