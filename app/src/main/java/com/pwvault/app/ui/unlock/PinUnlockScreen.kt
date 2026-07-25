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
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.unit.dp
import com.pwvault.app.R

@Composable
fun PinUnlockScreen(
    state: UnlockUiState.PinEntry,
    onUnlock: (pin: CharArray) -> Unit,
    onUseMasterPassword: () -> Unit,
    onUseBiometric: (() -> Unit)? = null,
) {
    var pin by remember { mutableStateOf("") }
    // Driven by user actions (submit shows, edit hides) rather than keyed on `error`'s value — two
    // consecutive failures can carry the exact same UnlockError, which a value-equality key can't
    // distinguish from "unchanged", so it would fail to re-show the second time.
    var showError by remember { mutableStateOf(state.error != null) }
    val lockoutSecondsRemaining = rememberLockoutSecondsRemaining(state.lockedUntilMillis)

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
                text = stringResource(R.string.pin_unlock_title),
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(top = 16.dp),
            )
            Text(
                text = stringResource(R.string.pin_unlock_subtitle),
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
            UnlockStatusMessage(
                error = if (showError) state.error else null,
                lockoutSecondsRemaining = lockoutSecondsRemaining,
            )
            Button(
                onClick = {
                    showError = true
                    onUnlock(pin.toCharArray())
                },
                enabled = !state.busy && lockoutSecondsRemaining <= 0,
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
            ) {
                Text(stringResource(R.string.pin_unlock_button))
            }
            TextButton(onClick = onUseMasterPassword, modifier = Modifier.padding(top = 8.dp)) {
                Text(stringResource(R.string.use_master_password_instead))
            }
            if (onUseBiometric != null) {
                TextButton(onClick = onUseBiometric) {
                    Text(stringResource(R.string.use_biometric_instead))
                }
            }
        }
    }
}
