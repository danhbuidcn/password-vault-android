package com.pwvault.app

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.pwvault.app.ui.theme.PwVaultTheme
import com.pwvault.app.ui.unlock.SetupScreen
import com.pwvault.app.ui.unlock.UnlockScreen
import com.pwvault.app.ui.unlock.UnlockUiState
import com.pwvault.app.ui.unlock.UnlockViewModel
import com.pwvault.app.ui.vault.VaultScreen
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val unlockViewModel: UnlockViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!BuildConfig.DEBUG) {
            window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)
        }
        setContent {
            PwVaultTheme {
                when (val state = unlockViewModel.state.collectAsState().value) {
                    is UnlockUiState.Loading -> Unit
                    is UnlockUiState.Setup ->
                        SetupScreen(
                            error = state.error,
                            busy = state.busy,
                            onCreateVault = unlockViewModel::createVault,
                        )
                    is UnlockUiState.Locked ->
                        UnlockScreen(
                            error = state.error,
                            busy = state.busy,
                            onUnlock = unlockViewModel::unlock,
                        )
                    is UnlockUiState.Unlocked -> VaultScreen()
                }
            }
        }
    }
}
