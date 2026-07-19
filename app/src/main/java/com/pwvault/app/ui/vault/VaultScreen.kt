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
import com.pwvault.app.ui.unlock.UnlockError

/**
 * Placeholder — real Vault Item list/CRUD lands in Feature 5 (docs/plans/roadmap.md).
 * The "set up PIN" entry point below is temporary; Feature 15 moves it into Settings.
 */
@Composable
fun VaultScreen(
    hasPin: Boolean,
    pinSetupError: UnlockError?,
    pinSetupBusy: Boolean,
    onSetupPin: (pin: CharArray, confirm: CharArray) -> Unit,
) {
    var showPinDialog by remember { mutableStateOf(false) }

    // Close the dialog only once setup actually succeeds, so validation errors stay visible
    // instead of the dialog closing itself away on every confirm tap.
    LaunchedEffect(hasPin) {
        if (hasPin) showPinDialog = false
    }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(text = stringResource(R.string.vault_placeholder))
            if (!hasPin) {
                Button(onClick = { showPinDialog = true }) {
                    Text(stringResource(R.string.setup_pin_button))
                }
            }
        }
    }

    if (showPinDialog) {
        PinSetupDialog(
            error = pinSetupError,
            busy = pinSetupBusy,
            onConfirm = { pin, confirm ->
                onSetupPin(pin, confirm)
                showPinDialog = false
            },
            onDismiss = { showPinDialog = false },
        )
    }
}
