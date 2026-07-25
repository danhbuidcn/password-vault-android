package com.pwvault.app.ui.unlock

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.rememberVectorPainter
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

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            LockIconBadge(
                painter = rememberVectorPainter(Icons.Filled.Fingerprint),
                contentDescription = null,
            )
            Text(
                text = stringResource(R.string.biometric_unlock_title),
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(top = 16.dp),
            )
            Text(
                text = stringResource(R.string.biometric_unlock_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp, bottom = 24.dp),
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
}
