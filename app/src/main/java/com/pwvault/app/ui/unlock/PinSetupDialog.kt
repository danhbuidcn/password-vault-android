package com.pwvault.app.ui.unlock

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.pwvault.app.R

@Composable
fun PinSetupDialog(
    error: UnlockError?,
    busy: Boolean,
    onConfirm: (pin: CharArray, confirm: CharArray) -> Unit,
    onDismiss: () -> Unit,
) {
    var pin by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    // Driven by user actions (submit shows, edit hides) rather than keyed on `error`'s value.
    // setupPin()'s validation failures (e.g. PIN_MISMATCH) set the error directly with no
    // intermediate null reset, unlike unlock()/createVault() — two consecutive identical failures
    // wouldn't change a value-equality key, so it would fail to re-show the second time.
    var showError by remember { mutableStateOf(error != null) }

    val pinFocusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { pinFocusRequester.requestFocus() }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.pin_setup_title)) },
        text = {
            Column {
                Text(
                    text = stringResource(R.string.pin_setup_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                )
                OutlinedTextField(
                    value = pin,
                    onValueChange = {
                        pin = it
                        showError = false
                    },
                    label = { Text(stringResource(R.string.pin_label)) },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp).focusRequester(pinFocusRequester),
                )
                OutlinedTextField(
                    value = confirm,
                    onValueChange = {
                        confirm = it
                        showError = false
                    },
                    label = { Text(stringResource(R.string.confirm_pin_label)) },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                )
                if (showError && error != null) {
                    Text(
                        text = error.message(),
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    showError = true
                    onConfirm(pin.toCharArray(), confirm.toCharArray())
                },
                enabled = !busy,
            ) {
                Text(stringResource(R.string.pin_setup_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.pin_setup_cancel))
            }
        },
    )
}
