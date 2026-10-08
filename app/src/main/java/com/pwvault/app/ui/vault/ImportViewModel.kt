package com.pwvault.app.ui.vault

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pwvault.app.data.TagRepository
import com.pwvault.app.data.VaultItemRepository
import com.pwvault.app.domain.CustomField
import com.pwvault.app.domain.MAX_TAGS_PER_VAULT_ITEM
import com.pwvault.app.domain.VaultItem
import com.pwvault.app.domain.VaultItemType
import com.pwvault.app.export.CSV_COLUMN_CUSTOM_FIELDS
import com.pwvault.app.export.CSV_COLUMN_NAME
import com.pwvault.app.export.CSV_COLUMN_NOTE
import com.pwvault.app.export.CSV_COLUMN_PASSWORD
import com.pwvault.app.export.CSV_COLUMN_TAGS
import com.pwvault.app.export.CSV_COLUMN_TYPE
import com.pwvault.app.export.CSV_COLUMN_URL
import com.pwvault.app.export.CSV_COLUMN_USERNAME
import com.pwvault.app.export.CSV_CUSTOM_FIELD_LABEL_SEPARATOR
import com.pwvault.app.export.CSV_CUSTOM_FIELD_SEPARATOR
import com.pwvault.app.export.CSV_TAG_SEPARATOR
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
        // Only auto-detected from the header (no picker) — present when re-importing the app's own export.
        val typeColumn: Int? = null,
        val tagsColumn: Int? = null,
        val customFieldsColumn: Int? = null,
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
        private val tagRepository: TagRepository,
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
                        autoMapped(rows)
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
                val tagIdsByName =
                    tagRepository
                        .observeTags()
                        .first()
                        .associate { it.name.lowercase() to it.id }
                        .toMutableMap()
                preview.importedRows.forEach { row ->
                    val itemId =
                        vaultItemRepository.addItem(
                            VaultItem(
                                id = 0,
                                type = row.type,
                                name = row.name,
                                username = row.username,
                                password = row.password,
                                url = row.url,
                                note = row.note,
                                createdAt = now,
                                updatedAt = now,
                            ),
                        )
                    if (row.tagNames.isNotEmpty()) {
                        val tagIds =
                            row.tagNames.map { name ->
                                tagIdsByName.getOrPut(name.lowercase()) { tagRepository.addTag(name) }
                            }
                        vaultItemRepository.setItemTags(itemId, tagIds.toSet())
                    }
                    if (row.customFields.isNotEmpty()) vaultItemRepository.setCustomFields(itemId, row.customFields)
                }
                _state.value =
                    ImportUiState.Done(
                        importedCount = preview.importedRows.size,
                        duplicateCount = preview.duplicateIndexes.size,
                        skippedCount = preview.skippedCount,
                    )
            }
        }

        /** Pre-selects columns whose header matches a known name (the app's own export, or common aliases). */
        private fun autoMapped(rows: List<List<String>>): ImportUiState.Mapping {
            val header = rows.first().map { it.trim().lowercase() }

            fun find(vararg names: String): Int? = header.indexOfFirst { it in names }.takeIf { it >= 0 }
            return ImportUiState.Mapping(
                rows = rows,
                nameColumn = find(CSV_COLUMN_NAME.lowercase(), "title"),
                usernameColumn = find(CSV_COLUMN_USERNAME.lowercase(), "login", "user", "email"),
                passwordColumn = find(CSV_COLUMN_PASSWORD.lowercase()),
                urlColumn = find(CSV_COLUMN_URL.lowercase(), "uri", "website"),
                noteColumn = find(CSV_COLUMN_NOTE.lowercase(), "notes"),
                typeColumn = find(CSV_COLUMN_TYPE.lowercase()),
                tagsColumn = find(CSV_COLUMN_TAGS.lowercase()),
                customFieldsColumn = find(CSV_COLUMN_CUSTOM_FIELDS.lowercase()),
            )
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
                type =
                    mapping.typeColumn
                        ?.let { getOrNull(it) }
                        ?.let { cell ->
                            VaultItemType.entries.firstOrNull { it.name.equals(cell.trim(), ignoreCase = true) }
                        }
                        ?: VaultItemType.LOGIN,
                tagNames =
                    mapping.tagsColumn
                        ?.let { getOrNull(it) }
                        ?.split(CSV_TAG_SEPARATOR)
                        ?.map { it.trim() }
                        ?.filter { it.isNotEmpty() }
                        ?.distinctBy { it.lowercase() }
                        ?.take(MAX_TAGS_PER_VAULT_ITEM)
                        .orEmpty(),
                customFields =
                    mapping.customFieldsColumn
                        ?.let { getOrNull(it) }
                        ?.split(CSV_CUSTOM_FIELD_SEPARATOR)
                        ?.filter { it.isNotBlank() }
                        ?.map { line ->
                            CustomField(
                                label = line.substringBefore(CSV_CUSTOM_FIELD_LABEL_SEPARATOR).trim(),
                                value = line.substringAfter(CSV_CUSTOM_FIELD_LABEL_SEPARATOR, "").trim(),
                            )
                        }.orEmpty(),
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
