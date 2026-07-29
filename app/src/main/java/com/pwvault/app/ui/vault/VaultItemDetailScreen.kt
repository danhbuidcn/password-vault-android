package com.pwvault.app.ui.vault

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import com.pwvault.app.ui.theme.PwVaultChrome
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val detailUpdatedFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())

@OptIn(ExperimentalMaterial3Api::class)
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

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(state.item.name, style = MaterialTheme.typography.titleMedium) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.vault_back_button),
                            tint = PwVaultChrome.Accent,
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onEdit) {
                        Icon(
                            imageVector = Icons.Filled.Edit,
                            contentDescription = stringResource(R.string.vault_edit_button),
                            tint = PwVaultChrome.Accent,
                        )
                    }
                    IconButton(onClick = onDelete) {
                        Icon(
                            imageVector = Icons.Filled.Delete,
                            contentDescription = stringResource(R.string.vault_delete_button),
                            tint = PwVaultChrome.Accent,
                        )
                    }
                },
                colors =
                    TopAppBarDefaults.topAppBarColors(
                        containerColor = PwVaultChrome.Background,
                        titleContentColor = PwVaultChrome.Content,
                    ),
            )
        },
    ) { padding ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.14f),
                ) {
                    Icon(
                        imageVector = typeIcon,
                        contentDescription = stringResource(typeLabelRes),
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.padding(10.dp).size(20.dp),
                    )
                }
                Text(
                    text = state.item.name,
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(start = 12.dp),
                )
            }

            if (state.item.type == VaultItemType.LOGIN) {
                LoginDetailFields(state, onTogglePasswordVisible, onCopyPassword)
            }

            if (state.item.note.isNotEmpty()) {
                DetailField(
                    label = stringResource(if (isNote) R.string.item_content_label else R.string.item_note_label),
                    value = state.item.note,
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

            DetailField(
                label = stringResource(R.string.vault_last_updated_field_label),
                value = detailUpdatedFormat.format(Date(state.item.updatedAt)),
            )

            SnackbarHost(hostState = snackbarHostState)
        }
    }
}

@Composable
private fun LoginDetailFields(
    state: VaultUiState.ItemDetail,
    onTogglePasswordVisible: () -> Unit,
    onCopyPassword: () -> Unit,
) {
    if (state.item.username.isNotEmpty()) {
        DetailField(label = stringResource(R.string.item_username_label), value = state.item.username)
    }

    Column(modifier = Modifier.padding(top = 16.dp)) {
        Text(
            text = stringResource(R.string.password_label),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = if (state.passwordVisible) state.item.password else "•".repeat(state.item.password.length),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(top = 2.dp),
            )
            IconButton(onClick = onTogglePasswordVisible) {
                Icon(
                    imageVector = if (state.passwordVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
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
    }

    PasswordWarnings(state.warning)

    if (state.item.url.isNotEmpty()) {
        DetailField(label = stringResource(R.string.item_url_label), value = state.item.url)
    }
}

@Composable
private fun DetailField(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.padding(top = 16.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(text = value, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(top = 2.dp))
    }
}

@Composable
private fun PasswordWarnings(warning: VaultItemWarning) {
    if (!warning.hasWarning) return
    val message = stringResource(R.string.warning_duplicate_password)
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(top = 12.dp)
                .background(MaterialTheme.colorScheme.error.copy(alpha = 0.12f), RoundedCornerShape(12.dp))
                .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Filled.WarningAmber,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.error,
            modifier = Modifier.size(18.dp),
        )
        Text(
            text = message,
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(start = 10.dp),
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
