package com.pwvault.app.ui.unlock

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.pwvault.app.R

/** Lockout takes priority over a stale [error] — it's the more current, more actionable message. */
@Composable
fun UnlockStatusMessage(
    error: UnlockError?,
    lockoutSecondsRemaining: Int = 0,
) {
    val text =
        when {
            lockoutSecondsRemaining > 0 -> stringResource(R.string.lockout_message, lockoutSecondsRemaining)
            error != null -> error.message()
            else -> null
        }
    if (text != null) {
        Text(text = text, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 8.dp))
    }
}
