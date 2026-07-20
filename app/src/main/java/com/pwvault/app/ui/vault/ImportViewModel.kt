package com.pwvault.app.ui.vault

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pwvault.app.data.VaultItemRepository
import com.pwvault.app.domain.VaultItem
import com.pwvault.app.importer.CsvImportParser
import com.pwvault.app.importer.ImportDuplicateDetector
import com.pwvault.app.importer.ImportedRow
import com.pwvault.app.importer.XlsxImportParser
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

enum class ImportFormError { NAME_COLUMN_REQUIRED }

enum class ImportError { READ_FAILED }

sealed interface ImportUiState {
    data object Closed : ImportUiState

    data object Reading : ImportUiState

    data class Mapping(
        val rows: List<List<String>>,
        val hasHeaderRow: Boolean = true,
        val nameColumn: Int? = null,
        val usernameColumn: Int? = null,
        val passwordColumn: Int? = null,
        val urlColumn: Int? = null,
        val noteColumn: Int? = null,
        val error: ImportFormError? = null,
    ) : ImportUiState {
        val columnCount: Int get() = rows.maxOfOrNull { it.size } ?: 0
        val dataRows: List<List<String>> get() = if (hasHeaderRow) rows.drop(1) else rows

        /** Header text for [index] when available — `null` means the caller should show a fallback "Column N" label. */
        fun columnLabel(index: Int): String? =
            if (hasHeaderRow) rows.firstOrNull()?.getOrNull(index)?.takeIf { it.isNotBlank() } else null
    }

    data class Preview(
        val importedRows: List<ImportedRow>,
        val duplicateIndexes: Set<Int>,
        val skippedCount: Int,
        val busy: Boolean = false,
    ) : ImportUiState

    data class Done(
        val importedCount: Int,
        val duplicateCount: Int,
        val skippedCount: Int,
    ) : ImportUiState

    data class Failed(
        val error: ImportError,
    ) : ImportUiState
}

@HiltViewModel
class ImportViewModel
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
        private val csvImportParser: CsvImportParser,
        private val xlsxImportParser: XlsxImportParser,
        private val duplicateDetector: ImportDuplicateDetector,
        private val vaultItemRepository: VaultItemRepository,
    ) : ViewModel() {
        private val _state = MutableStateFlow<ImportUiState>(ImportUiState.Closed)
        val state: StateFlow<ImportUiState> = _state.asStateFlow()

        fun close() {
            _state.value = ImportUiState.Closed
        }

        fun onFilePicked(uri: Uri?) {
            if (uri == null) return
            _state.value = ImportUiState.Reading
            viewModelScope.launch {
                val rows = runCatching { readRows(uri) }.getOrNull()
                _state.value =
                    if (rows.isNullOrEmpty()) {
                        ImportUiState.Failed(ImportError.READ_FAILED)
                    } else {
                        ImportUiState.Mapping(rows = rows)
                    }
            }
        }

        fun toggleHeaderRow() {
            val current = _state.value as? ImportUiState.Mapping ?: return
            _state.value = current.copy(hasHeaderRow = !current.hasHeaderRow)
        }

        fun updateNameColumn(column: Int?) = updateMapping { it.copy(nameColumn = column, error = null) }

        fun updateUsernameColumn(column: Int?) = updateMapping { it.copy(usernameColumn = column) }

        fun updatePasswordColumn(column: Int?) = updateMapping { it.copy(passwordColumn = column) }

        fun updateUrlColumn(column: Int?) = updateMapping { it.copy(urlColumn = column) }

        fun updateNoteColumn(column: Int?) = updateMapping { it.copy(noteColumn = column) }

        fun confirmMapping() {
            val mapping = _state.value as? ImportUiState.Mapping ?: return
            if (mapping.nameColumn == null) {
                _state.value = mapping.copy(error = ImportFormError.NAME_COLUMN_REQUIRED)
                return
            }
            val candidates = mapping.dataRows.map { row -> row.toImportedRow(mapping) }
            val (valid, skipped) = candidates.partition { it.name.isNotBlank() }
            viewModelScope.launch {
                val existing = vaultItemRepository.observeItems().first()
                val duplicateIndexes = duplicateDetector.findDuplicateRowIndexes(valid, existing)
                _state.value = ImportUiState.Preview(valid, duplicateIndexes, skipped.size)
            }
        }

        fun confirmImport() {
            val preview = _state.value as? ImportUiState.Preview ?: return
            _state.value = preview.copy(busy = true)
            viewModelScope.launch {
                val now = System.currentTimeMillis()
                preview.importedRows.forEach { row ->
                    vaultItemRepository.addItem(
                        VaultItem(
                            id = 0,
                            name = row.name,
                            username = row.username,
                            password = row.password,
                            url = row.url,
                            note = row.note,
                            createdAt = now,
                            updatedAt = now,
                        ),
                    )
                }
                _state.value =
                    ImportUiState.Done(
                        importedCount = preview.importedRows.size,
                        duplicateCount = preview.duplicateIndexes.size,
                        skippedCount = preview.skippedCount,
                    )
            }
        }

        private fun updateMapping(transform: (ImportUiState.Mapping) -> ImportUiState.Mapping) {
            val current = _state.value as? ImportUiState.Mapping ?: return
            _state.value = transform(current)
        }

        private fun List<String>.toImportedRow(mapping: ImportUiState.Mapping): ImportedRow =
            ImportedRow(
                name = getOrNull(requireNotNull(mapping.nameColumn)).orEmpty(),
                username = mapping.usernameColumn?.let { getOrNull(it) }.orEmpty(),
                password = mapping.passwordColumn?.let { getOrNull(it) }.orEmpty(),
                url = mapping.urlColumn?.let { getOrNull(it) }.orEmpty(),
                note = mapping.noteColumn?.let { getOrNull(it) }.orEmpty(),
            )

        private suspend fun readRows(uri: Uri): List<List<String>> =
            withContext(Dispatchers.IO) {
                val isXlsx = queryDisplayName(uri)?.endsWith(".xlsx", ignoreCase = true) == true
                context.contentResolver.openInputStream(uri)?.use { input ->
                    if (isXlsx) {
                        xlsxImportParser.parse(input)
                    } else {
                        csvImportParser.parse(input.readBytes().toString(Charsets.UTF_8))
                    }
                } ?: error("Couldn't open source file")
            }

        private fun queryDisplayName(uri: Uri): String? {
            context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (index >= 0 && cursor.moveToFirst()) return cursor.getString(index)
            }
            return null
        }
    }
