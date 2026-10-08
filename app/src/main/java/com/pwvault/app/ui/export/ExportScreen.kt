package com.pwvault.app.ui.export

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.pwvault.app.R

@Composable
fun ExportScreen(
    state: ExportUiState,
    onSubmitPin: (CharArray) -> Unit,
    onPickDestination: (suggestedFileName: String) -> Unit,
    onClose: () -> Unit,
) {
    // Disabled during Writing — that state has no on-screen Cancel either, since `close()` doesn't
    // cancel the in-flight write coroutine (it just sets state to Closed, which the write's own
    // Done/Failed completion would silently overwrite, popping the screen back up after the user
    // thought they'd backed out).
    BackHandler(enabled = state !is ExportUiState.Writing, onBack = onClose)
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxSize().padding(24.dp),
        ) {
            when (state) {
                is ExportUiState.Closed -> Unit
                is ExportUiState.Reauth ->
                    ExportReauth(state = state, onSubmit = onSubmitPin, onCancel = onClose)
                is ExportUiState.PickDestination -> {
                    LaunchedEffect(state) { onPickDestination(state.suggestedFileName) }
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
private fun ExportReauth(
    state: ExportUiState.Reauth,
    onSubmit: (CharArray) -> Unit,
    onCancel: () -> Unit,
) {
    var pin by remember { mutableStateOf("") }
    Text(stringResource(R.string.export_title), style = MaterialTheme.typography.headlineSmall)
    Text(
        text = stringResource(R.string.export_csv_warning),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 8.dp),
    )
    OutlinedTextField(
        value = pin,
        onValueChange = { pin = it },
        label = { Text(stringResource(R.string.pin_label)) },
        singleLine = true,
        visualTransformation = PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
        modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
    )
    if (state.error == ExportError.WRONG_PIN) {
        Text(
            text = stringResource(R.string.error_wrong_pin),
            color = MaterialTheme.colorScheme.error,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
    Button(
        onClick = { onSubmit(pin.toCharArray()) },
        enabled = !state.busy && pin.isNotEmpty(),
        modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
    ) {
        Text(stringResource(R.string.export_confirm_button))
    }
    TextButton(onClick = onCancel, modifier = Modifier.fillMaxWidth()) {
        Text(stringResource(R.string.vault_cancel_button))
    }
}
