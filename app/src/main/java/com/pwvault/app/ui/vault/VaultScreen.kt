package com.pwvault.app.ui.vault

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Label
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import com.pwvault.app.domain.VaultItem
import com.pwvault.app.ui.unlock.PinSetupDialog
import com.pwvault.app.ui.unlock.UnlockUiState
import com.pwvault.app.ui.unlock.message

@Composable
fun VaultScreen(
    state: UnlockUiState.Unlocked,
    canSetupBiometric: Boolean,
    onSetupPin: (pin: CharArray, confirm: CharArray) -> Unit,
    onSetupBiometric: () -> Unit,
    viewModel: VaultViewModel,
    tagViewModel: TagViewModel,
) {
    var showPinDialog by remember { mutableStateOf(false) }
    var showTagManager by remember { mutableStateOf(false) }

    // Close the dialog only once setup actually succeeds, so validation errors stay visible
    // instead of the dialog closing itself away on every confirm tap.
    LaunchedEffect(state.hasPin) {
        if (state.hasPin) showPinDialog = false
    }

    // Runs once per unlock session — VaultScreen only enters composition while Unlocked, and the
    // vault database is only open then (see VaultViewModel.startObservingItems doc).
    LaunchedEffect(Unit) {
        viewModel.startObservingItems()
    }

    if (showTagManager) {
        TagManagerScreen(viewModel = tagViewModel, onBack = { showTagManager = false })
        return
    }

    when (val vaultState = viewModel.state.collectAsState().value) {
        is VaultUiState.ItemList ->
            VaultItemListScreen(
                vaultItems = vaultState.items,
                searchQuery = vaultState.searchQuery,
                unlockedState = state,
                canSetupBiometric = canSetupBiometric,
                onSetupPin = { showPinDialog = true },
                onSetupBiometric = onSetupBiometric,
                onAddItem = viewModel::openAddForm,
                onOpenItem = viewModel::openDetail,
                onSearchQueryChange = viewModel::updateSearchQuery,
                onManageTags = { showTagManager = true },
            )
        is VaultUiState.ItemDetail ->
            VaultItemDetailScreen(
                state = vaultState,
                onBack = viewModel::backToList,
                onEdit = { viewModel.openEditForm(vaultState.item) },
                onDelete = { viewModel.openDeleteConfirm(vaultState.item) },
                onTogglePasswordVisible = viewModel::togglePasswordVisible,
                onCopyPassword = viewModel::copyPassword,
            )
        is VaultUiState.ItemForm ->
            VaultItemFormScreen(
                state = vaultState,
                onSave = viewModel::save,
                onCancel = viewModel::backToList,
                onToggleTag = viewModel::toggleTagSelected,
            )
        is VaultUiState.DeleteConfirm ->
            AlertDialog(
                onDismissRequest = viewModel::cancelDeleteConfirm,
                title = { Text(stringResource(R.string.vault_delete_confirm_title)) },
                text = { Text(stringResource(R.string.vault_delete_confirm_message, vaultState.item.name)) },
                confirmButton = {
                    TextButton(onClick = viewModel::confirmDelete) {
                        Text(stringResource(R.string.vault_delete_button))
                    }
                },
                dismissButton = {
                    TextButton(onClick = viewModel::cancelDeleteConfirm) {
                        Text(stringResource(R.string.vault_cancel_button))
                    }
                },
            )
    }

    if (showPinDialog) {
        PinSetupDialog(
            error = state.pinSetupError,
            busy = state.pinSetupBusy,
            onConfirm = { pin, confirm -> onSetupPin(pin, confirm) },
            onDismiss = { showPinDialog = false },
        )
    }
}

@Composable
private fun VaultItemListScreen(
    vaultItems: List<VaultItem>,
    searchQuery: String,
    unlockedState: UnlockUiState.Unlocked,
    canSetupBiometric: Boolean,
    onSetupPin: () -> Unit,
    onSetupBiometric: () -> Unit,
    onAddItem: () -> Unit,
    onOpenItem: (VaultItem) -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onManageTags: () -> Unit,
) {
    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = onAddItem) {
                Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.vault_add_item_cd))
            }
        },
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (!unlockedState.hasPin) {
                Button(onClick = onSetupPin, modifier = Modifier.padding(16.dp)) {
                    Text(stringResource(R.string.setup_pin_button))
                }
            }
            if (!unlockedState.hasBiometric && canSetupBiometric) {
                Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                    Button(onClick = onSetupBiometric, enabled = !unlockedState.biometricSetupBusy) {
                        Text(stringResource(R.string.setup_biometric_button))
                    }
                    if (unlockedState.biometricSetupError != null) {
                        Text(
                            text = unlockedState.biometricSetupError.message(),
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }
            }

            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                label = { Text(stringResource(R.string.search_placeholder)) },
                singleLine = true,
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { onSearchQueryChange("") }) {
                            Icon(Icons.Filled.Clear, contentDescription = stringResource(R.string.search_clear_cd))
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            )

            TextButton(onClick = onManageTags, modifier = Modifier.padding(horizontal = 8.dp)) {
                Icon(
                    Icons.AutoMirrored.Filled.Label,
                    contentDescription = null,
                    modifier = Modifier.padding(end = 4.dp),
                )
                Text(stringResource(R.string.manage_tags_cd))
            }

            if (vaultItems.isEmpty()) {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Text(
                            text =
                                stringResource(
                                    if (searchQuery.isNotBlank()) {
                                        R.string.vault_no_results_state
                                    } else {
                                        R.string.vault_empty_state
                                    },
                                ),
                        )
                    }
                }
            } else {
                LazyColumn(contentPadding = PaddingValues(16.dp)) {
                    items(vaultItems, key = { it.id }) { item ->
                        Column(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .clickable { onOpenItem(item) }
                                    .padding(vertical = 12.dp),
                        ) {
                            Text(text = item.name, style = MaterialTheme.typography.bodyLarge)
                            if (item.username.isNotEmpty()) {
                                Text(
                                    text = item.username,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            if (item.tags.isNotEmpty()) {
                                Row(
                                    modifier =
                                        Modifier
                                            .horizontalScroll(rememberScrollState())
                                            .padding(top = 4.dp),
                                ) {
                                    item.tags.forEach { tag ->
                                        TagChip(tag = tag, modifier = Modifier.padding(end = 8.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
