package com.pwvault.app.ui.unlock

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.pwvault.app.ui.theme.PwVaultTheme

/**
 * Placeholder — unlock flow (Master Password / PIN / biometric) is implemented in a later plan.
 */
@Composable
fun UnlockScreen() {
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(text = "PWVault — Unlock (coming soon)")
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun UnlockScreenPreview() {
    PwVaultTheme {
        UnlockScreen()
    }
}
