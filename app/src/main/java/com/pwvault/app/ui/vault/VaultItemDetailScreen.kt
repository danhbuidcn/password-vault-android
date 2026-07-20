package com.pwvault.app.ui.vault

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.pwvault.app.R
import com.pwvault.app.domain.CustomField
import com.pwvault.app.domain.VaultItemType

@Composable
fun VaultItemDetailScreen(
    state: VaultUiState.ItemDetail,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onTogglePasswordVisible: () -> Unit,
    onCopyPassword: () -> Unit,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val copiedMessage = stringResource(R.string.vault_password_copied_message)

    LaunchedEffect(state.copyEventId) {
        if (state.copyEventId > 0) snackbarHostState.showSnackbar(copiedMessage)
    }

    val isNote = state.item.type == VaultItemType.NOTE
    val typeIcon = if (isNote) Icons.Filled.Description else Icons.Filled.Person
    val typeLabelRes = if (isNote) R.string.item_type_note else R.string.item_type_login

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = typeIcon,
                contentDescription = stringResource(typeLabelRes),
                modifier = Modifier.padding(end = 8.dp),
            )
            Text(text = state.item.name, style = MaterialTheme.typography.headlineSmall)
        }

        if (state.item.type == VaultItemType.LOGIN) {
            if (state.item.username.isNotEmpty()) {
                Text(
                    text = state.item.username,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(top = 16.dp),
                )
            }

            Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                Text(
                    text =
                        if (state.passwordVisible) {
                            state.item.password
                        } else {
                            "•".repeat(state.item.password.length)
                        },
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(top = 12.dp),
                )
                IconButton(onClick = onTogglePasswordVisible) {
                    Icon(
                        imageVector =
                            if (state.passwordVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                        contentDescription =
                            stringResource(
                                if (state.passwordVisible) R.string.password_hide_cd else R.string.password_show_cd,
                            ),
                    )
                }
                IconButton(onClick = onCopyPassword) {
                    Icon(Icons.Filled.ContentCopy, contentDescription = stringResource(R.string.copy_password_cd))
                }
            }

            PasswordWarnings(state.warning)

            if (state.item.url.isNotEmpty()) {
                Text(
                    text = state.item.url,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        }

        if (state.item.note.isNotEmpty()) {
            Text(
                text = state.item.note,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 16.dp),
            )
        }

        if (state.item.customFields.isNotEmpty()) {
            CustomFieldsSection(customFields = state.item.customFields)
        }

        if (state.item.tags.isNotEmpty()) {
            Row(modifier = Modifier.horizontalScroll(rememberScrollState()).padding(top = 16.dp)) {
                state.item.tags.forEach { tag ->
                    TagChip(tag = tag, modifier = Modifier.padding(end = 8.dp))
                }
            }
        }

        Button(onClick = onEdit, modifier = Modifier.fillMaxWidth().padding(top = 24.dp)) {
            Text(stringResource(R.string.vault_edit_button))
        }
        OutlinedButton(onClick = onDelete, modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
            Text(stringResource(R.string.vault_delete_button))
        }
        TextButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.vault_back_button))
        }

        SnackbarHost(hostState = snackbarHostState)
    }
}

@Composable
private fun PasswordWarnings(warning: VaultItemWarning) {
    if (warning.weak) {
        Text(
            text = stringResource(R.string.warning_weak_password),
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodySmall,
        )
    }
    if (warning.duplicate) {
        Text(
            text = stringResource(R.string.warning_duplicate_password),
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

@Composable
private fun CustomFieldsSection(customFields: List<CustomField>) {
    val visibleFieldIds = remember { mutableStateMapOf<Long, Boolean>() }
    Column(modifier = Modifier.padding(top = 16.dp)) {
        customFields.forEach { field ->
            val visible = visibleFieldIds[field.id] == true
            Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = field.label, style = MaterialTheme.typography.labelLarge)
                    Text(
                        text = if (visible) field.value else "•".repeat(field.value.length),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
                IconButton(onClick = { visibleFieldIds[field.id] = !visible }) {
                    Icon(
                        imageVector = if (visible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                        contentDescription =
                            stringResource(
                                if (visible) {
                                    R.string.custom_field_hide_value_cd
                                } else {
                                    R.string.custom_field_show_value_cd
                                },
                            ),
                    )
                }
            }
        }
    }
}
