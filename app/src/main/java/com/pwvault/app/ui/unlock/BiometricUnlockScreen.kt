package com.pwvault.app.ui.unlock

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.pwvault.app.R

@Composable
fun BiometricUnlockScreen(
    error: UnlockError?,
    busy: Boolean,
    onAuthenticate: () -> Unit,
    onUseMasterPassword: () -> Unit,
    onUsePin: (() -> Unit)? = null,
) {
    LaunchedEffect(Unit) {
        onAuthenticate()
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = Icons.Filled.Fingerprint,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(64.dp),
        )
        Text(
            text = stringResource(R.string.biometric_unlock_title),
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(top = 16.dp, bottom = 24.dp),
        )
        if (error != null) {
            Text(
                text = error.message(),
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(bottom = 16.dp),
            )
        }
        Button(
            onClick = onAuthenticate,
            enabled = !busy,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(R.string.biometric_unlock_retry_button))
        }
        if (onUsePin != null) {
            TextButton(onClick = onUsePin, modifier = Modifier.padding(top = 8.dp)) {
                Text(stringResource(R.string.use_pin_instead))
            }
        }
        TextButton(onClick = onUseMasterPassword) {
            Text(stringResource(R.string.use_master_password_instead))
        }
    }
}
