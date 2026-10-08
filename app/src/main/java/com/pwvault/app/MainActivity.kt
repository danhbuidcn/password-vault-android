package com.pwvault.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.pwvault.app.security.ThemeMode
import com.pwvault.app.ui.export.ExportViewModel
import com.pwvault.app.ui.settings.SettingsViewModel
import com.pwvault.app.ui.theme.PwVaultTheme
import com.pwvault.app.ui.unlock.BiometricUnlockScreen
import com.pwvault.app.ui.unlock.PinUnlockScreen
import com.pwvault.app.ui.unlock.SetupScreen
import com.pwvault.app.ui.unlock.UnlockScreen
import com.pwvault.app.ui.unlock.UnlockUiState
import com.pwvault.app.ui.unlock.UnlockViewModel
import com.pwvault.app.ui.vault.ImportViewModel
import com.pwvault.app.ui.vault.TagViewModel
import com.pwvault.app.ui.vault.VaultScreen
import com.pwvault.app.ui.vault.VaultViewModel
import dagger.hilt.android.AndroidEntryPoint

private const val EXPORT_DESTINATION_MIME_TYPE = "text/csv"
private val IMPORT_SOURCE_MIME_TYPES =
    arrayOf(
        "text/csv",
        "text/comma-separated-values",
        "text/plain",
        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
    )

private enum class BiometricOperation { UNLOCK, SETUP }

@AndroidEntryPoint
class MainActivity : FragmentActivity() {
    private val unlockViewModel: UnlockViewModel by viewModels()
    private val vaultViewModel: VaultViewModel by viewModels()
    private val tagViewModel: TagViewModel by viewModels()
    private val exportViewModel: ExportViewModel by viewModels()
    private val importViewModel: ImportViewModel by viewModels()
    private val settingsViewModel: SettingsViewModel by viewModels()
    private lateinit var biometricPrompt: BiometricPrompt
    private lateinit var unlockPromptInfo: BiometricPrompt.PromptInfo
    private lateinit var setupPromptInfo: BiometricPrompt.PromptInfo
    private lateinit var exportDestinationLauncher: ActivityResultLauncher<String>
    private lateinit var importSourceLauncher: ActivityResultLauncher<Array<String>>
    private lateinit var notificationPermissionLauncher: ActivityResultLauncher<String>
    private var pendingBiometricOperation = BiometricOperation.UNLOCK

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (BuildConfig.ENABLE_SCREENSHOT_BLOCK) {
            window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)
        }
        unlockPromptInfo = createBiometricPromptInfo(getString(R.string.use_pin_instead))
        setupPromptInfo = createBiometricPromptInfo(getString(R.string.pin_setup_cancel))
        biometricPrompt = BiometricPrompt(this, ContextCompat.getMainExecutor(this), biometricAuthenticationCallback())
        registerActivityResultLaunchers()
        requestNotificationPermissionIfNeeded()
        val canSetupBiometric =
            BiometricManager.from(this).canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK) ==
                BiometricManager.BIOMETRIC_SUCCESS

        setContent {
            val themeMode =
                settingsViewModel.state
                    .collectAsState()
                    .value.themeMode
            val darkTheme =
                when (themeMode) {
                    ThemeMode.LIGHT -> false
                    ThemeMode.DARK -> true
                }
            PwVaultTheme(darkTheme = darkTheme) {
                PwVaultApp(
                    unlockViewModel = unlockViewModel,
                    vaultViewModel = vaultViewModel,
                    tagViewModel = tagViewModel,
                    exportViewModel = exportViewModel,
                    importViewModel = importViewModel,
                    settingsViewModel = settingsViewModel,
                    canSetupBiometric = canSetupBiometric,
                    onAuthenticateBiometricUnlock = ::triggerBiometricUnlock,
                    onSetupBiometric = ::triggerBiometricSetup,
                    onPickExportDestination = { exportDestinationLauncher.launch(it) },
                    onPickImportSource = { importSourceLauncher.launch(IMPORT_SOURCE_MIME_TYPES) },
                )
            }
        }
    }

    private fun registerActivityResultLaunchers() {
        exportDestinationLauncher =
            registerForActivityResult(ActivityResultContracts.CreateDocument(EXPORT_DESTINATION_MIME_TYPE)) { uri ->
                exportViewModel.onDestinationPicked(uri)
            }
        importSourceLauncher =
            registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
                importViewModel.onFilePicked(uri)
            }
        notificationPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) {}
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }

    /**
     * `BIOMETRIC_WEAK` so face unlock works on most devices — biometric is a gate only, no
     * `CryptoObject` (see docs/plans/feature-19-simplify-unlock-csv-plan.md).
     */
    private fun createBiometricPromptInfo(negativeButtonText: String): BiometricPrompt.PromptInfo =
        BiometricPrompt.PromptInfo
            .Builder()
            .setTitle(getString(R.string.biometric_prompt_title))
            .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_WEAK)
            .setNegativeButtonText(negativeButtonText)
            .build()

    private fun triggerBiometricUnlock() {
        pendingBiometricOperation = BiometricOperation.UNLOCK
        biometricPrompt.authenticate(unlockPromptInfo)
    }

    private fun triggerBiometricSetup() {
        pendingBiometricOperation = BiometricOperation.SETUP
        biometricPrompt.authenticate(setupPromptInfo)
    }

    private fun biometricAuthenticationCallback() =
        object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                when (pendingBiometricOperation) {
                    BiometricOperation.UNLOCK -> unlockViewModel.completeBiometricUnlock()
                    BiometricOperation.SETUP -> unlockViewModel.completeBiometricSetup()
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
                        if (!cancelled) unlockViewModel.onBiometricSetupError()
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
    vaultViewModel: VaultViewModel,
    tagViewModel: TagViewModel,
    exportViewModel: ExportViewModel,
    importViewModel: ImportViewModel,
    settingsViewModel: SettingsViewModel,
    canSetupBiometric: Boolean,
    onAuthenticateBiometricUnlock: () -> Unit,
    onSetupBiometric: () -> Unit,
    onPickExportDestination: (String) -> Unit,
    onPickImportSource: () -> Unit,
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
                onUseBiometric = if (state.hasBiometric) unlockViewModel::switchToBiometric else null,
            )
        is UnlockUiState.BiometricEntry ->
            BiometricUnlockScreen(
                error = state.error,
                busy = state.busy,
                onAuthenticate = onAuthenticateBiometricUnlock,
                onUsePin = unlockViewModel::switchToPin,
            )
        is UnlockUiState.Unlocked ->
            VaultScreen(
                state = state,
                canSetupBiometric = canSetupBiometric,
                onSetupPin = unlockViewModel::setupPin,
                onSetupBiometric = onSetupBiometric,
                onDisableBiometric = unlockViewModel::disableBiometric,
                viewModel = vaultViewModel,
                tagViewModel = tagViewModel,
                exportViewModel = exportViewModel,
                importViewModel = importViewModel,
                settingsViewModel = settingsViewModel,
                onVerifyPin = unlockViewModel::verifyPin,
                onPickExportDestination = onPickExportDestination,
                onPickImportSource = onPickImportSource,
            )
    }
}
