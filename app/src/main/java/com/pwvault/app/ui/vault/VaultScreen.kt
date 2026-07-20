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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.AlertDialog
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
import com.pwvault.app.domain.VaultItemType
import com.pwvault.app.ui.export.ExportScreen
import com.pwvault.app.ui.export.ExportTarget
import com.pwvault.app.ui.export.ExportUiState
import com.pwvault.app.ui.export.ExportViewModel
import com.pwvault.app.ui.settings.SettingsScreen
import com.pwvault.app.ui.settings.SettingsViewModel
import com.pwvault.app.ui.unlock.PinSetupDialog
import com.pwvault.app.ui.unlock.UnlockUiState

@Composable
fun VaultScreen(
    state: UnlockUiState.Unlocked,
    canSetupBiometric: Boolean,
    onSetupPin: (pin: CharArray, confirm: CharArray) -> Unit,
    onSetupBiometric: () -> Unit,
    viewModel: VaultViewModel,
    tagViewModel: TagViewModel,
    passwordTemplateViewModel: PasswordTemplateViewModel,
    exportViewModel: ExportViewModel,
    settingsViewModel: SettingsViewModel,
    importViewModel: ImportViewModel,
    onVerifyMasterPassword: suspend (CharArray) -> Boolean,
    onPickExportDestination: (ExportTarget, String) -> Unit,
    onPickImportSource: () -> Unit,
    hasAutoBackupFolder: Boolean,
    onPickAutoBackupFolder: () -> Unit,
) {
    var showPinDialog by remember { mutableStateOf(false) }
    var showTagManager by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }

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

    val exportState = exportViewModel.state.collectAsState().value
    val importState = importViewModel.state.collectAsState().value

    // A plain if/else chain (not early `return`s) so the trailing PinSetupDialog check below is
    // always reached regardless of which branch renders — PIN setup can now be triggered from
    // SettingsScreen, not just the main list, so skipping past it would silently hide the dialog.
    when {
        showSettings ->
            SettingsScreen(
                unlockedState = state,
                canSetupBiometric = canSetupBiometric,
                onSetupPin = { showPinDialog = true },
                onSetupBiometric = onSetupBiometric,
                onManageTags = {
                    showSettings = false
                    showTagManager = true
                },
                onExport = {
                    showSettings = false
                    exportViewModel.open()
                },
                onImport = {
                    showSettings = false
                    onPickImportSource()
                },
                hasAutoBackupFolder = hasAutoBackupFolder,
                onPickAutoBackupFolder = onPickAutoBackupFolder,
                viewModel = settingsViewModel,
                onBack = { showSettings = false },
            )
        showTagManager -> TagManagerScreen(viewModel = tagViewModel, onBack = { showTagManager = false })
        exportState != ExportUiState.Closed ->
            ExportScreen(
                state = exportState,
                onChooseTarget = exportViewModel::chooseTarget,
                onSubmitMasterPassword = { password ->
                    exportViewModel.submitMasterPassword(password, onVerifyMasterPassword)
                },
                onToggleAck1 = exportViewModel::toggleAck1,
                onToggleAck2 = exportViewModel::toggleAck2,
                onContinueFromWarning = exportViewModel::continueFromWarning,
                onSubmitZipPassword = exportViewModel::submitZipPassword,
                onPickDestination = onPickExportDestination,
                onClose = exportViewModel::close,
            )
        importState != ImportUiState.Closed ->
            ImportScreen(
                state = importState,
                onToggleHeaderRow = importViewModel::toggleHeaderRow,
                onNameColumnChange = importViewModel::updateNameColumn,
                onUsernameColumnChange = importViewModel::updateUsernameColumn,
                onPasswordColumnChange = importViewModel::updatePasswordColumn,
                onUrlColumnChange = importViewModel::updateUrlColumn,
                onNoteColumnChange = importViewModel::updateNoteColumn,
                onContinueMapping = importViewModel::confirmMapping,
                onConfirmImport = importViewModel::confirmImport,
                onRetry = {
                    importViewModel.close()
                    onPickImportSource()
                },
                onClose = importViewModel::close,
            )
        else ->
            when (val vaultState = viewModel.state.collectAsState().value) {
                is VaultUiState.ItemList ->
                    VaultItemListScreen(
                        vaultItems = vaultState.items,
                        warnings = vaultState.warnings,
                        searchQuery = vaultState.searchQuery,
                        onAddItem = viewModel::openAddForm,
                        onOpenItem = viewModel::openDetail,
                        onSearchQueryChange = viewModel::updateSearchQuery,
                        onOpenSettings = { showSettings = true },
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
                        passwordTemplateViewModel = passwordTemplateViewModel,
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
    warnings: Map<Long, VaultItemWarning>,
    searchQuery: String,
    onAddItem: () -> Unit,
    onOpenItem: (VaultItem) -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onOpenSettings: () -> Unit,
) {
    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = onAddItem) {
                Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.vault_add_item_cd))
            }
        },
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.End,
            ) {
                IconButton(onClick = onOpenSettings) {
                    Icon(Icons.Filled.Settings, contentDescription = stringResource(R.string.settings_cd))
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
                        Row(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .clickable { onOpenItem(item) }
                                    .padding(vertical = 12.dp),
                        ) {
                            val isNote = item.type == VaultItemType.NOTE
                            Icon(
                                imageVector = if (isNote) Icons.Filled.Description else Icons.Filled.Person,
                                contentDescription =
                                    stringResource(if (isNote) R.string.item_type_note else R.string.item_type_login),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 2.dp, end = 12.dp),
                            )
                            Column(modifier = Modifier.weight(1f)) {
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
                            if (warnings[item.id]?.hasWarning == true) {
                                Icon(
                                    imageVector = Icons.Filled.WarningAmber,
                                    contentDescription = stringResource(R.string.password_warning_cd),
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.padding(start = 8.dp, top = 2.dp),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
