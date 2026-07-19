package com.pwvault.app

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import com.pwvault.app.ui.theme.PwVaultTheme
import com.pwvault.app.ui.unlock.BiometricUnlockScreen
import com.pwvault.app.ui.unlock.PinUnlockScreen
import com.pwvault.app.ui.unlock.SetupScreen
import com.pwvault.app.ui.unlock.UnlockScreen
import com.pwvault.app.ui.unlock.UnlockUiState
import com.pwvault.app.ui.unlock.UnlockViewModel
import com.pwvault.app.ui.vault.VaultScreen
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

private enum class BiometricOperation { UNLOCK, SETUP }

@AndroidEntryPoint
class MainActivity : FragmentActivity() {
    private val unlockViewModel: UnlockViewModel by viewModels()
    private lateinit var biometricPrompt: BiometricPrompt
    private lateinit var unlockPromptInfo: BiometricPrompt.PromptInfo
    private lateinit var setupPromptInfo: BiometricPrompt.PromptInfo
    private var pendingBiometricOperation = BiometricOperation.UNLOCK

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!BuildConfig.DEBUG) {
            window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)
        }

        unlockPromptInfo = createBiometricPromptInfo(getString(R.string.use_master_password_instead))
        setupPromptInfo = createBiometricPromptInfo(getString(R.string.pin_setup_cancel))
        biometricPrompt = BiometricPrompt(this, ContextCompat.getMainExecutor(this), biometricAuthenticationCallback())
        val canSetupBiometric =
            BiometricManager.from(this).canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG) ==
                BiometricManager.BIOMETRIC_SUCCESS

        setContent {
            PwVaultTheme {
                PwVaultApp(
                    unlockViewModel = unlockViewModel,
                    canSetupBiometric = canSetupBiometric,
                    onAuthenticateBiometricUnlock = ::triggerBiometricUnlock,
                    onSetupBiometric = ::triggerBiometricSetup,
                )
            }
        }
    }

    private fun createBiometricPromptInfo(negativeButtonText: String): BiometricPrompt.PromptInfo =
        BiometricPrompt.PromptInfo
            .Builder()
            .setTitle(getString(R.string.biometric_prompt_title))
            .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG)
            .setNegativeButtonText(negativeButtonText)
            .build()

    private fun triggerBiometricUnlock() {
        pendingBiometricOperation = BiometricOperation.UNLOCK
        lifecycleScope.launch {
            val cipher = unlockViewModel.prepareBiometricUnlockCipher()
            if (cipher != null) {
                biometricPrompt.authenticate(unlockPromptInfo, BiometricPrompt.CryptoObject(cipher))
            } else {
                unlockViewModel.onBiometricUnlockError()
            }
        }
    }

    private fun triggerBiometricSetup() {
        pendingBiometricOperation = BiometricOperation.SETUP
        lifecycleScope.launch {
            val cipher = unlockViewModel.prepareBiometricSetupCipher()
            if (cipher != null) {
                biometricPrompt.authenticate(setupPromptInfo, BiometricPrompt.CryptoObject(cipher))
            } else {
                unlockViewModel.onBiometricSetupError()
            }
        }
    }

    private fun biometricAuthenticationCallback() =
        object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                val cipher = result.cryptoObject?.cipher ?: return
                when (pendingBiometricOperation) {
                    BiometricOperation.UNLOCK -> unlockViewModel.completeBiometricUnlock(cipher)
                    BiometricOperation.SETUP -> unlockViewModel.completeBiometricSetup(cipher)
                }
            }

            override fun onAuthenticationError(
                errorCode: Int,
                errString: CharSequence,
            ) {
                val cancelled =
                    errorCode == BiometricPrompt.ERROR_USER_CANCELED ||
                        errorCode == BiometricPrompt.ERROR_NEGATIVE_BUTTON
                when (pendingBiometricOperation) {
                    BiometricOperation.UNLOCK ->
                        if (cancelled) {
                            unlockViewModel.onBiometricUnlockCancelled()
                        } else {
                            unlockViewModel.onBiometricUnlockError()
                        }
                    BiometricOperation.SETUP ->
                        if (cancelled) {
                            unlockViewModel.onBiometricSetupCancelled()
                        } else {
                            unlockViewModel.onBiometricSetupError()
                        }
                }
            }
        }

    override fun onStop() {
        super.onStop()
        unlockViewModel.onAppBackgrounded()
    }

    override fun onResume() {
        super.onResume()
        unlockViewModel.onAppForegrounded()
    }
}

@Composable
private fun PwVaultApp(
    unlockViewModel: UnlockViewModel,
    canSetupBiometric: Boolean,
    onAuthenticateBiometricUnlock: () -> Unit,
    onSetupBiometric: () -> Unit,
) {
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
                lockedUntilMillis = state.lockedUntilMillis,
                onUnlock = unlockViewModel::unlock,
            )
        is UnlockUiState.PinEntry ->
            PinUnlockScreen(
                state = state,
                onUnlock = unlockViewModel::unlockWithPin,
                onUseMasterPassword = unlockViewModel::switchToMasterPassword,
                onUseBiometric = if (state.hasBiometric) unlockViewModel::switchToBiometric else null,
            )
        is UnlockUiState.BiometricEntry ->
            BiometricUnlockScreen(
                error = state.error,
                busy = state.busy,
                onAuthenticate = onAuthenticateBiometricUnlock,
                onUseMasterPassword = unlockViewModel::switchToMasterPassword,
                onUsePin = if (state.hasPin) unlockViewModel::switchToPin else null,
            )
        is UnlockUiState.Unlocked ->
            VaultScreen(
                state = state,
                canSetupBiometric = canSetupBiometric,
                onSetupPin = unlockViewModel::setupPin,
                onSetupBiometric = onSetupBiometric,
            )
    }
}
