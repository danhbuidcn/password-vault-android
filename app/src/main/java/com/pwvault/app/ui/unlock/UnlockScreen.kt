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
 * exists as an immutable String here before being copied into a CharArray for [onUnlock].
 */
@Composable
fun UnlockScreen(
    error: UnlockError?,
    busy: Boolean,
    lockedUntilMillis: Long? = null,
    onUnlock: (password: CharArray) -> Unit,
) {
    var password by remember { mutableStateOf("") }
    // Driven by user actions (submit shows, edit hides) rather than keyed on `error`'s value — two
    // consecutive failures can carry the exact same UnlockError, which a value-equality key can't
    // distinguish from "unchanged", so it would fail to re-show the second time.
    var showError by remember { mutableStateOf(error != null) }
    val lockoutSecondsRemaining = rememberLockoutSecondsRemaining(lockedUntilMillis)

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
            text = stringResource(R.string.unlock_title),
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(top = 16.dp, bottom = 24.dp),
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
        UnlockStatusMessage(
            error = if (showError) error else null,
            lockoutSecondsRemaining = lockoutSecondsRemaining,
        )
        Button(
            onClick = {
                showError = true
                onUnlock(password.toCharArray())
            },
            enabled = !busy && lockoutSecondsRemaining <= 0,
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
        ) {
            Text(stringResource(if (busy) R.string.unlock_button_busy else R.string.unlock_button))
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun UnlockScreenPreview() {
    PwVaultTheme {
        UnlockScreen(error = null, busy = false, onUnlock = {})
    }
}
