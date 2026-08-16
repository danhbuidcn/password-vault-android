package com.pwvault.app.ui.unlock

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.pwvault.app.R
import com.pwvault.app.ui.theme.PwVaultTheme

/**
 * Shown after the user picks a `.pwvbackup` file on [SetupScreen]'s "Restore from backup" — asks for
 * the Master Password that was used on the device the backup came from. Compose's TextField state is
 * String-based (no CharArray input API) — the password briefly exists as an immutable String here
 * before being copied into a CharArray for [onRestore].
 */
@Composable
fun RestorePasswordScreen(
    error: UnlockError?,
    busy: Boolean,
    onRestore: (password: CharArray) -> Unit,
    onCancel: () -> Unit,
) {
    // Disabled while busy — mirrors the on-screen Cancel button below: a restore in flight replaces
    // the real vault file, so it must not be interruptible mid-operation.
    BackHandler(enabled = !busy, onBack = onCancel)
    var password by remember { mutableStateOf("") }
    var showError by remember { mutableStateOf(error != null) }

    val passwordFocusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { passwordFocusRequester.requestFocus() }

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
                text = stringResource(R.string.restore_title),
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(top = 16.dp),
            )
            Text(
                text = stringResource(R.string.restore_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp, bottom = 24.dp),
            )
            PasswordField(
                value = password,
                onValueChange = {
                    password = it
                    showError = false
                },
                label = stringResource(R.string.password_label),
                modifier = Modifier.focusRequester(passwordFocusRequester),
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
                    onRestore(password.toCharArray())
                },
                enabled = !busy,
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
            ) {
                Text(stringResource(if (busy) R.string.restore_button_busy else R.string.restore_button))
            }
            TextButton(onClick = onCancel, enabled = !busy) {
                Text(stringResource(R.string.restore_cancel))
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun RestorePasswordScreenPreview() {
    PwVaultTheme {
        RestorePasswordScreen(error = null, busy = false, onRestore = {}, onCancel = {})
    }
}
