package com.pwvault.app.ui.export

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.pwvault.app.R
import com.pwvault.app.ui.unlock.MIN_PASSWORD_LENGTH
import com.pwvault.app.ui.unlock.PasswordField

@Composable
fun ExportScreen(
    state: ExportUiState,
    onChooseTarget: (ExportTarget) -> Unit,
    onSubmitMasterPassword: (CharArray) -> Unit,
    onToggleAck1: () -> Unit,
    onToggleAck2: () -> Unit,
    onContinueFromWarning: () -> Unit,
    onSubmitZipPassword: (password: CharArray, confirm: CharArray) -> Unit,
    onPickDestination: (ExportTarget, suggestedFileName: String) -> Unit,
    onClose: () -> Unit,
) {
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxSize().padding(24.dp),
        ) {
            when (state) {
                is ExportUiState.Closed -> Unit
                is ExportUiState.Choice ->
                    ExportChoice(onChooseTarget = onChooseTarget, onCancel = onClose)
                is ExportUiState.Reauth ->
                    ExportReauth(state = state, onSubmit = onSubmitMasterPassword, onCancel = onClose)
                is ExportUiState.CsvWarning ->
                    ExportCsvWarning(
                        state = state,
                        onToggleAck1 = onToggleAck1,
                        onToggleAck2 = onToggleAck2,
                        onContinue = onContinueFromWarning,
                        onCancel = onClose,
                    )
                is ExportUiState.CsvPassword ->
                    ExportCsvPassword(state = state, onSubmit = onSubmitZipPassword, onCancel = onClose)
                is ExportUiState.PickDestination -> {
                    LaunchedEffect(state) { onPickDestination(state.target, state.suggestedFileName) }
                    Text(stringResource(R.string.export_preparing))
                    TextButton(onClick = onClose) { Text(stringResource(R.string.vault_cancel_button)) }
                }
                is ExportUiState.Writing -> {
                    CircularProgressIndicator()
                    Text(stringResource(R.string.export_writing), modifier = Modifier.padding(top = 16.dp))
                }
                is ExportUiState.Done -> {
                    Text(stringResource(R.string.export_done))
                    TextButton(onClick = onClose) { Text(stringResource(R.string.vault_back_button)) }
                }
                is ExportUiState.Failed -> {
                    Text(stringResource(R.string.export_failed), color = MaterialTheme.colorScheme.error)
                    TextButton(onClick = onClose) { Text(stringResource(R.string.vault_back_button)) }
                }
            }
        }
    }
}

@Composable
private fun ExportChoice(
    onChooseTarget: (ExportTarget) -> Unit,
    onCancel: () -> Unit,
) {
    Text(stringResource(R.string.export_title), style = MaterialTheme.typography.headlineSmall)
    Button(
        onClick = { onChooseTarget(ExportTarget.BACKUP) },
        modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
    ) {
        Text(stringResource(R.string.export_backup_option))
    }
    Text(
        text = stringResource(R.string.export_backup_option_hint),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Button(
        onClick = { onChooseTarget(ExportTarget.CSV) },
        modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
    ) {
        Text(stringResource(R.string.export_csv_option))
    }
    Text(
        text = stringResource(R.string.export_csv_option_hint),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    TextButton(onClick = onCancel, modifier = Modifier.fillMaxWidth().padding(top = 16.dp)) {
        Text(stringResource(R.string.vault_cancel_button))
    }
}

@Composable
private fun ExportReauth(
    state: ExportUiState.Reauth,
    onSubmit: (CharArray) -> Unit,
    onCancel: () -> Unit,
) {
    var password by remember { mutableStateOf("") }
    Text(stringResource(R.string.export_reauth_title), style = MaterialTheme.typography.headlineSmall)
    PasswordField(
        value = password,
        onValueChange = { password = it },
        label = stringResource(R.string.password_label),
        modifier = Modifier.padding(top = 16.dp),
    )
    if (state.error == ExportError.WRONG_MASTER_PASSWORD) {
        Text(
            text = stringResource(R.string.error_wrong_password),
            color = MaterialTheme.colorScheme.error,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
    Button(
        onClick = { onSubmit(password.toCharArray()) },
        enabled = !state.busy && password.isNotEmpty(),
        modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
    ) {
        Text(stringResource(R.string.export_confirm_button))
    }
    TextButton(onClick = onCancel, modifier = Modifier.fillMaxWidth()) {
        Text(stringResource(R.string.vault_cancel_button))
    }
}

@Composable
private fun ExportCsvWarning(
    state: ExportUiState.CsvWarning,
    onToggleAck1: () -> Unit,
    onToggleAck2: () -> Unit,
    onContinue: () -> Unit,
    onCancel: () -> Unit,
) {
    Text(stringResource(R.string.export_csv_warning_title), style = MaterialTheme.typography.headlineSmall)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
    ) {
        Checkbox(checked = state.ack1, onCheckedChange = { onToggleAck1() })
        Text(stringResource(R.string.export_csv_warning_ack1))
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
    ) {
        Checkbox(checked = state.ack2, onCheckedChange = { onToggleAck2() })
        Text(stringResource(R.string.export_csv_warning_ack2))
    }
    Button(
        onClick = onContinue,
        enabled = state.ack1 && state.ack2,
        modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
    ) {
        Text(stringResource(R.string.export_confirm_button))
    }
    TextButton(onClick = onCancel, modifier = Modifier.fillMaxWidth()) {
        Text(stringResource(R.string.vault_cancel_button))
    }
}

@Composable
private fun ExportCsvPassword(
    state: ExportUiState.CsvPassword,
    onSubmit: (password: CharArray, confirm: CharArray) -> Unit,
    onCancel: () -> Unit,
) {
    var password by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    Text(stringResource(R.string.export_zip_password_title), style = MaterialTheme.typography.headlineSmall)
    PasswordField(
        value = password,
        onValueChange = { password = it },
        label = stringResource(R.string.export_zip_password_label),
        modifier = Modifier.padding(top = 16.dp),
    )
    PasswordField(
        value = confirm,
        onValueChange = { confirm = it },
        label = stringResource(R.string.confirm_password_label),
        modifier = Modifier.padding(top = 8.dp),
    )
    if (state.error == ExportError.ZIP_PASSWORD_TOO_SHORT) {
        Text(
            text = stringResource(R.string.error_password_too_short, MIN_PASSWORD_LENGTH),
            color = MaterialTheme.colorScheme.error,
            modifier = Modifier.padding(top = 4.dp),
        )
    } else if (state.error == ExportError.ZIP_PASSWORD_MISMATCH) {
        Text(
            text = stringResource(R.string.error_password_mismatch),
            color = MaterialTheme.colorScheme.error,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
    Button(
        onClick = { onSubmit(password.toCharArray(), confirm.toCharArray()) },
        enabled = password.isNotEmpty() && confirm.isNotEmpty(),
        modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
    ) {
        Text(stringResource(R.string.export_confirm_button))
    }
    TextButton(onClick = onCancel, modifier = Modifier.fillMaxWidth()) {
        Text(stringResource(R.string.vault_cancel_button))
    }
}
