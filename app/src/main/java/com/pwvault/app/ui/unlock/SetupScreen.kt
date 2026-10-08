package com.pwvault.app.ui.unlock

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.pwvault.app.R
import com.pwvault.app.ui.theme.PwVaultTheme

/**
 * First-run setup: the user picks the PIN — the only secret they need to remember. Compose's
 * TextField state is String-based (no CharArray input API) — the PIN briefly exists as an
 * immutable String here before being copied into CharArrays for [onCreateVault].
 */
@Composable
fun SetupScreen(
    error: UnlockError?,
    busy: Boolean,
    onCreateVault: (pin: CharArray, confirm: CharArray) -> Unit,
) {
    var pin by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    // Driven by user actions (submit shows, edit hides) rather than keyed on `error`'s value — two
    // consecutive failures can carry the exact same UnlockError (e.g. PIN_MISMATCH twice), which
    // would be indistinguishable to a value-equality key and fail to re-show the second time.
    var showError by remember { mutableStateOf(error != null) }

    val pinFocusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { pinFocusRequester.requestFocus() }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            LockIconBadge(
                painter = painterResource(R.drawable.ic_launcher_monochrome),
                contentDescription = null,
            )
            Text(
                text = stringResource(R.string.setup_title),
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(top = 16.dp),
            )
            Text(
                text = stringResource(R.string.setup_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp, bottom = 24.dp),
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
                modifier = Modifier.fillMaxWidth().focusRequester(pinFocusRequester),
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
            Button(
                onClick = {
                    showError = true
                    onCreateVault(pin.toCharArray(), confirm.toCharArray())
                },
                enabled = !busy,
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
            ) {
                Text(stringResource(if (busy) R.string.setup_button_busy else R.string.setup_button))
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun SetupScreenPreview() {
    PwVaultTheme {
        SetupScreen(error = null, busy = false, onCreateVault = { _, _ -> })
    }
}
