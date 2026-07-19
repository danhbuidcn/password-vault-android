package com.pwvault.app.ui.vault

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.res.stringResource
import com.pwvault.app.R
import com.pwvault.app.ui.unlock.PinSetupDialog
import com.pwvault.app.ui.unlock.UnlockUiState
import com.pwvault.app.ui.unlock.message

/**
 * Placeholder — real Vault Item list/CRUD lands in Feature 5 (docs/plans/roadmap.md).
 * The "set up PIN"/"set up biometric" entry points below are temporary; Feature 15 moves them into Settings.
 */
@Composable
fun VaultScreen(
    state: UnlockUiState.Unlocked,
    canSetupBiometric: Boolean,
    onSetupPin: (pin: CharArray, confirm: CharArray) -> Unit,
    onSetupBiometric: () -> Unit,
) {
    var showPinDialog by remember { mutableStateOf(false) }

    // Close the dialog only once setup actually succeeds, so validation errors stay visible
    // instead of the dialog closing itself away on every confirm tap.
    LaunchedEffect(state.hasPin) {
        if (state.hasPin) showPinDialog = false
    }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(text = stringResource(R.string.vault_placeholder))
            if (!state.hasPin) {
                Button(onClick = { showPinDialog = true }) {
                    Text(stringResource(R.string.setup_pin_button))
                }
            }
            if (!state.hasBiometric && canSetupBiometric) {
                Button(onClick = onSetupBiometric, enabled = !state.biometricSetupBusy) {
                    Text(stringResource(R.string.setup_biometric_button))
                }
                if (state.biometricSetupError != null) {
                    Text(
                        text = state.biometricSetupError.message(),
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
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
