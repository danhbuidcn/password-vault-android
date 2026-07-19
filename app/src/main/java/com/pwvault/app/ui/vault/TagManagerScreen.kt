package com.pwvault.app.ui.vault

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.pwvault.app.R
import com.pwvault.app.domain.Tag

@Composable
fun TagManagerScreen(
    viewModel: TagViewModel,
    onBack: () -> Unit,
) {
    LaunchedEffect(Unit) { viewModel.startObservingTags() }

    val state by viewModel.state.collectAsState()
    var newTagName by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
        Text(text = stringResource(R.string.tag_manager_title), style = MaterialTheme.typography.headlineSmall)

        Row(modifier = Modifier.fillMaxWidth().padding(top = 16.dp)) {
            OutlinedTextField(
                value = newTagName,
                onValueChange = { newTagName = it },
                label = { Text(stringResource(R.string.add_tag_placeholder)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        Button(
            onClick = {
                viewModel.addTag(newTagName)
                newTagName = ""
            },
            modifier = Modifier.padding(top = 8.dp),
        ) {
            Text(stringResource(R.string.add_tag_button))
        }
        val newTagError = state.newTagError
        if (newTagError != null) {
            Text(
                text = newTagError.message(),
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(top = 4.dp),
            )
        }

        LazyColumn(modifier = Modifier.padding(top = 16.dp)) {
            items(state.tags, key = { it.id }) { tag ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        TagColorDot(tag.id, modifier = Modifier.padding(end = 8.dp))
                        Text(text = tag.name, style = MaterialTheme.typography.bodyLarge)
                    }
                    Row {
                        IconButton(onClick = { viewModel.startRename(tag) }) {
                            Icon(Icons.Filled.Edit, contentDescription = stringResource(R.string.rename_tag_cd))
                        }
                        IconButton(onClick = { viewModel.startDelete(tag) }) {
                            Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.delete_tag_cd))
                        }
                    }
                }
            }
        }

        TextButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.vault_back_button))
        }
    }

    val renamingTag = state.renamingTag
    if (renamingTag != null) {
        RenameTagDialog(
            tag = renamingTag,
            error = state.renameError,
            onConfirm = viewModel::confirmRename,
            onDismiss = viewModel::cancelRename,
        )
    }

    val deletingTag = state.deletingTag
    if (deletingTag != null) {
        AlertDialog(
            onDismissRequest = viewModel::cancelDelete,
            title = { Text(stringResource(R.string.delete_tag_confirm_title)) },
            text = { Text(stringResource(R.string.delete_tag_confirm_message, deletingTag.name)) },
            confirmButton = {
                TextButton(onClick = viewModel::confirmDelete) {
                    Text(stringResource(R.string.vault_delete_button))
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::cancelDelete) {
                    Text(stringResource(R.string.vault_cancel_button))
                }
            },
        )
    }
}

@Composable
private fun RenameTagDialog(
    tag: Tag,
    error: TagError?,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var name by remember { mutableStateOf(tag.name) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.rename_tag_title)) },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                if (error != null) {
                    Text(
                        text = error.message(),
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(name) }) {
                Text(stringResource(R.string.vault_save_button))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.vault_cancel_button))
            }
        },
    )
}
