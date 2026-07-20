package com.pwvault.app.ui.unlock

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.pwvault.app.R
import com.pwvault.app.ui.theme.PwVaultTheme

/**
 * Compose's TextField state is String-based (no CharArray input API) — the password briefly
 * exists as an immutable String here before being copied into CharArrays for [onCreateVault].
 */
@Composable
fun SetupScreen(
    error: UnlockError?,
    busy: Boolean,
    onCreateVault: (password: CharArray, confirm: CharArray) -> Unit,
) {
    var password by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    // Driven by user actions (submit shows, edit hides) rather than keyed on `error`'s value — two
    // consecutive failures can carry the exact same UnlockError (e.g. PASSWORD_MISMATCH twice), which
    // would be indistinguishable to a value-equality key and fail to re-show the second time.
    var showError by remember { mutableStateOf(error != null) }

    val passwordFocusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { passwordFocusRequester.requestFocus() }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = Icons.Filled.Lock,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(48.dp),
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
        PasswordField(
            value = password,
            onValueChange = {
                password = it
                showError = false
            },
            label = stringResource(R.string.password_label),
            modifier = Modifier.focusRequester(passwordFocusRequester),
        )
        PasswordField(
            value = confirm,
            onValueChange = {
                confirm = it
                showError = false
            },
            label = stringResource(R.string.confirm_password_label),
            modifier = Modifier.padding(top = 8.dp),
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
                onCreateVault(password.toCharArray(), confirm.toCharArray())
            },
            enabled = !busy,
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
        ) {
            Text(stringResource(if (busy) R.string.setup_button_busy else R.string.setup_button))
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
