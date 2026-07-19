package com.pwvault.app.ui.vault

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.pwvault.app.R
import com.pwvault.app.security.PasswordGenerator
import com.pwvault.app.ui.unlock.PasswordField

@Composable
fun VaultItemFormScreen(
    state: VaultUiState.ItemForm,
    onSave: (name: String, username: String, password: String, url: String, note: String) -> Unit,
    onCancel: () -> Unit,
) {
    var name by remember { mutableStateOf(state.initial?.name.orEmpty()) }
    var username by remember { mutableStateOf(state.initial?.username.orEmpty()) }
    var password by remember { mutableStateOf(state.initial?.password.orEmpty()) }
    var url by remember { mutableStateOf(state.initial?.url.orEmpty()) }
    var note by remember { mutableStateOf(state.initial?.note.orEmpty()) }

    // Only NAME_REQUIRED can be shown here, and it's derivable straight from the current text —
    // so the message disappears the moment the user types a name, no ViewModel round-trip needed.
    val showNameError = state.error == VaultFormError.NAME_REQUIRED && name.isBlank()

    val nameFocusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { nameFocusRequester.requestFocus() }

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
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text(stringResource(R.string.item_name_label)) },
            singleLine = true,
            isError = showNameError,
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp).focusRequester(nameFocusRequester),
        )
        if (showNameError) {
            Text(
                text = state.error.message(),
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
        OutlinedTextField(
            value = username,
            onValueChange = { username = it },
            label = { Text(stringResource(R.string.item_username_label)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        )
        PasswordField(
            value = password,
            onValueChange = { password = it },
            label = stringResource(R.string.password_label),
            modifier = Modifier.padding(top = 8.dp),
        )
        TextButton(onClick = { password = PasswordGenerator.generate() }) {
            Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.padding(end = 4.dp))
            Text(stringResource(R.string.generate_password_button))
        }
        OutlinedTextField(
            value = url,
            onValueChange = { url = it },
            label = { Text(stringResource(R.string.item_url_label)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        )
        OutlinedTextField(
            value = note,
            onValueChange = { note = it },
            label = { Text(stringResource(R.string.item_note_label)) },
            minLines = 4,
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        )
        Button(
            onClick = { onSave(name, username, password, url, note) },
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
