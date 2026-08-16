package com.pwvault.app.ui.vault

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.pwvault.app.R

@Composable
fun ImportScreen(
    state: ImportUiState,
    onToggleHeaderRow: () -> Unit,
    onNameColumnChange: (Int?) -> Unit,
    onUsernameColumnChange: (Int?) -> Unit,
    onPasswordColumnChange: (Int?) -> Unit,
    onUrlColumnChange: (Int?) -> Unit,
    onNoteColumnChange: (Int?) -> Unit,
    onContinueMapping: () -> Unit,
    onConfirmImport: () -> Unit,
    onRetry: () -> Unit,
    onClose: () -> Unit,
) {
    // Disabled during Reading — that state has no on-screen Close either, since `close()` doesn't
    // cancel the in-flight read coroutine (it just sets state to Closed, which the read's own
    // Mapping/Failed completion would silently overwrite, popping the screen back up after the user
    // thought they'd backed out).
    BackHandler(enabled = state !is ImportUiState.Reading, onBack = onClose)
    Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
        Text(text = stringResource(R.string.import_title), style = MaterialTheme.typography.headlineSmall)

        when (state) {
            is ImportUiState.Closed -> Unit
            is ImportUiState.Reading ->
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    CircularProgressIndicator()
                    Text(text = stringResource(R.string.import_reading), modifier = Modifier.padding(top = 16.dp))
                }
            is ImportUiState.Mapping ->
                MappingContent(
                    state = state,
                    onToggleHeaderRow = onToggleHeaderRow,
                    onNameColumnChange = onNameColumnChange,
                    onUsernameColumnChange = onUsernameColumnChange,
                    onPasswordColumnChange = onPasswordColumnChange,
                    onUrlColumnChange = onUrlColumnChange,
                    onNoteColumnChange = onNoteColumnChange,
                    onContinueMapping = onContinueMapping,
                    onClose = onClose,
                )
            is ImportUiState.Preview ->
                PreviewContent(state = state, onConfirmImport = onConfirmImport, onClose = onClose)
            is ImportUiState.Done ->
                DoneContent(state = state, onClose = onClose)
            is ImportUiState.Failed ->
                Column(modifier = Modifier.padding(top = 24.dp)) {
                    Text(text = stringResource(R.string.import_failed), color = MaterialTheme.colorScheme.error)
                    Button(onClick = onRetry, modifier = Modifier.padding(top = 16.dp)) {
                        Text(stringResource(R.string.import_retry_button))
                    }
                    TextButton(onClick = onClose) { Text(stringResource(R.string.vault_cancel_button)) }
                }
        }
    }
}

@Composable
private fun MappingContent(
    state: ImportUiState.Mapping,
    onToggleHeaderRow: () -> Unit,
    onNameColumnChange: (Int?) -> Unit,
    onUsernameColumnChange: (Int?) -> Unit,
    onPasswordColumnChange: (Int?) -> Unit,
    onUrlColumnChange: (Int?) -> Unit,
    onNoteColumnChange: (Int?) -> Unit,
    onContinueMapping: () -> Unit,
    onClose: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(top = 16.dp),
        ) {
            Checkbox(checked = state.hasHeaderRow, onCheckedChange = { onToggleHeaderRow() })
            Text(stringResource(R.string.import_header_row_toggle))
        }

        ColumnPicker(
            label = stringResource(R.string.import_map_name_label),
            state = state,
            selected = state.nameColumn,
            onSelect = onNameColumnChange,
            required = true,
        )
        if (state.error == ImportFormError.NAME_COLUMN_REQUIRED) {
            Text(
                text = stringResource(R.string.error_import_name_column_required),
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
        ColumnPicker(
            label = stringResource(R.string.import_map_username_label),
            state = state,
            selected = state.usernameColumn,
            onSelect = onUsernameColumnChange,
        )
        ColumnPicker(
            label = stringResource(R.string.import_map_password_label),
            state = state,
            selected = state.passwordColumn,
            onSelect = onPasswordColumnChange,
        )
        ColumnPicker(
            label = stringResource(R.string.import_map_url_label),
            state = state,
            selected = state.urlColumn,
            onSelect = onUrlColumnChange,
        )
        ColumnPicker(
            label = stringResource(R.string.import_map_note_label),
            state = state,
            selected = state.noteColumn,
            onSelect = onNoteColumnChange,
        )

        Button(onClick = onContinueMapping, modifier = Modifier.fillMaxWidth().padding(top = 16.dp)) {
            Text(stringResource(R.string.import_continue_button))
        }
        TextButton(onClick = onClose, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.vault_cancel_button))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ColumnPicker(
    label: String,
    state: ImportUiState.Mapping,
    selected: Int?,
    onSelect: (Int?) -> Unit,
    required: Boolean = false,
) {
    var expanded by remember { mutableStateOf(false) }
    val noneLabel = stringResource(R.string.import_column_none)
    val selectedLabel =
        selected?.let { state.columnLabel(it) ?: stringResource(R.string.import_column_label, it + 1) }
            ?: if (required) "" else noneLabel

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = Modifier.padding(top = 8.dp),
    ) {
        OutlinedTextField(
            value = selectedLabel,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.fillMaxWidth().menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            if (!required) {
                DropdownMenuItem(text = { Text(noneLabel) }, onClick = {
                    onSelect(null)
                    expanded = false
                })
            }
            for (index in 0 until state.columnCount) {
                val columnLabel = state.columnLabel(index) ?: stringResource(R.string.import_column_label, index + 1)
                DropdownMenuItem(text = { Text(columnLabel) }, onClick = {
                    onSelect(index)
                    expanded = false
                })
            }
        }
    }
}

@Composable
private fun PreviewContent(
    state: ImportUiState.Preview,
    onConfirmImport: () -> Unit,
    onClose: () -> Unit,
) {
    Column(modifier = Modifier.padding(top = 16.dp)) {
        Text(text = stringResource(R.string.import_preview_title), style = MaterialTheme.typography.titleMedium)
        Text(
            text = stringResource(R.string.import_preview_summary, state.importedRows.size),
            modifier = Modifier.padding(top = 8.dp),
        )
        if (state.duplicateIndexes.isNotEmpty()) {
            Text(text = stringResource(R.string.import_preview_duplicates, state.duplicateIndexes.size))
        }
        if (state.skippedCount > 0) {
            Text(text = stringResource(R.string.import_preview_skipped, state.skippedCount))
        }
        Button(
            onClick = onConfirmImport,
            enabled = !state.busy,
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
        ) {
            Text(stringResource(R.string.import_confirm_button))
        }
        TextButton(onClick = onClose, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.vault_cancel_button))
        }
    }
}

@Composable
private fun DoneContent(
    state: ImportUiState.Done,
    onClose: () -> Unit,
) {
    Column(modifier = Modifier.padding(top = 16.dp)) {
        Text(text = stringResource(R.string.import_done_title), style = MaterialTheme.typography.titleMedium)
        Text(
            text = stringResource(R.string.import_done_summary, state.importedCount),
            modifier = Modifier.padding(top = 8.dp),
        )
        Text(text = stringResource(R.string.import_done_delete_reminder), modifier = Modifier.padding(top = 8.dp))
        Button(onClick = onClose, modifier = Modifier.fillMaxWidth().padding(top = 16.dp)) {
            Text(stringResource(R.string.import_close_button))
        }
    }
}
