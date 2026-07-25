package com.pwvault.app.ui.vault

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.pwvault.app.R
import com.pwvault.app.domain.VaultItemType
import com.pwvault.app.ui.unlock.PasswordField

private class CustomFieldRow(
    label: String = "",
    value: String = "",
) {
    var label by mutableStateOf(label)
    var value by mutableStateOf(value)
}

@Composable
fun VaultItemFormScreen(
    state: VaultUiState.ItemForm,
    passwordTemplateViewModel: PasswordTemplateViewModel,
    onSave: (VaultItemFormInput) -> Unit,
    onCancel: () -> Unit,
    onToggleTag: (Long) -> Unit,
) {
    var type by remember { mutableStateOf(state.initial?.type ?: VaultItemType.LOGIN) }
    var name by remember { mutableStateOf(state.initial?.name.orEmpty()) }
    var username by remember { mutableStateOf(state.initial?.username.orEmpty()) }
    var password by remember { mutableStateOf(state.initial?.password.orEmpty()) }
    var url by remember { mutableStateOf(state.initial?.url.orEmpty()) }
    var note by remember { mutableStateOf(state.initial?.note.orEmpty()) }
    val customFieldRows =
        remember {
            mutableStateListOf<CustomFieldRow>().apply {
                state.initial?.customFields?.forEach { add(CustomFieldRow(it.label, it.value)) }
            }
        }
    var showGeneratorDialog by remember { mutableStateOf(false) }

    // Only NAME_REQUIRED can be shown here, and it's derivable straight from the current text —
    // so the message disappears the moment the user types a name, no ViewModel round-trip needed.
    val showNameError = state.error == VaultFormError.NAME_REQUIRED && name.isBlank()

    val nameFocusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { nameFocusRequester.requestFocus() }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
        ) {
            Text(
                text =
                    stringResource(
                        if (state.editingId != null) R.string.vault_edit_item_title else R.string.vault_add_item_title,
                    ),
                style = MaterialTheme.typography.headlineSmall,
            )
            if (state.editingId == null) {
                Row(modifier = Modifier.padding(top = 12.dp)) {
                    FilterChip(
                        selected = type == VaultItemType.LOGIN,
                        onClick = { type = VaultItemType.LOGIN },
                        label = { Text(stringResource(R.string.item_type_login)) },
                        modifier = Modifier.padding(end = 8.dp),
                    )
                    FilterChip(
                        selected = type == VaultItemType.NOTE,
                        onClick = { type = VaultItemType.NOTE },
                        label = { Text(stringResource(R.string.item_type_note)) },
                    )
                }
            }
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(stringResource(R.string.item_name_label)) },
                singleLine = true,
                isError = showNameError,
                leadingIcon = { Icon(Icons.Filled.Language, contentDescription = null) },
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp).focusRequester(nameFocusRequester),
            )
            if (showNameError) {
                Text(
                    text = state.error.message(),
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
            if (type == VaultItemType.LOGIN) {
                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text(stringResource(R.string.item_username_label)) },
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Filled.Person, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                )
                PasswordField(
                    value = password,
                    onValueChange = { password = it },
                    label = stringResource(R.string.password_label),
                    leadingIcon = { Icon(Icons.Filled.Lock, contentDescription = null) },
                    modifier = Modifier.padding(top = 8.dp),
                )
                TextButton(onClick = { showGeneratorDialog = true }) {
                    Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.padding(end = 4.dp))
                    Text(stringResource(R.string.generate_password_button))
                }
                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    label = { Text(stringResource(R.string.item_url_label)) },
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Filled.Language, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                )
            }
            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                label = {
                    Text(
                        stringResource(
                            if (type == VaultItemType.NOTE) R.string.item_content_label else R.string.item_note_label,
                        ),
                    )
                },
                minLines = 4,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            )
            if (state.availableTags.isNotEmpty()) {
                Text(
                    text = stringResource(R.string.tags_label),
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(top = 16.dp),
                )
                Row(modifier = Modifier.horizontalScroll(rememberScrollState()).padding(top = 8.dp)) {
                    state.availableTags.forEach { tag ->
                        FilterChip(
                            selected = tag.id in state.selectedTagIds,
                            onClick = { onToggleTag(tag.id) },
                            label = { Text(tag.name) },
                            leadingIcon = { TagColorDot(tag.id) },
                            modifier = Modifier.padding(end = 8.dp),
                        )
                    }
                }
            }
            Text(
                text = stringResource(R.string.custom_fields_label),
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.padding(top = 16.dp),
            )
            customFieldRows.forEachIndexed { index, row ->
                Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                    OutlinedTextField(
                        value = row.label,
                        onValueChange = { row.label = it },
                        label = { Text(stringResource(R.string.custom_field_label_hint)) },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    OutlinedTextField(
                        value = row.value,
                        onValueChange = { row.value = it },
                        label = { Text(stringResource(R.string.custom_field_value_hint)) },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                    IconButton(onClick = { customFieldRows.removeAt(index) }) {
                        Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.remove_custom_field_cd))
                    }
                }
            }
            TextButton(onClick = { customFieldRows.add(CustomFieldRow()) }) {
                Text(stringResource(R.string.add_custom_field_button))
            }
            Button(
                onClick = {
                    onSave(
                        VaultItemFormInput(
                            type = type,
                            name = name,
                            username = username,
                            password = password,
                            url = url,
                            note = note,
                            customFields = customFieldRows.map { it.label to it.value },
                        ),
                    )
                },
                enabled = !state.busy,
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
            ) {
                Text(stringResource(R.string.vault_save_button))
            }
            TextButton(onClick = onCancel, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.vault_cancel_button))
            }
        }
    }

    if (showGeneratorDialog) {
        PasswordGeneratorDialog(
            viewModel = passwordTemplateViewModel,
            onUsePassword = {
                password = it
                showGeneratorDialog = false
            },
            onDismiss = { showGeneratorDialog = false },
        )
    }
}
