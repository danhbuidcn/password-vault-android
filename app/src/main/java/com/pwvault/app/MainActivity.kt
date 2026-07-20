package com.pwvault.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import com.pwvault.app.security.BackupPreferences
import com.pwvault.app.security.ThemeMode
import com.pwvault.app.ui.export.ExportTarget
import com.pwvault.app.ui.export.ExportViewModel
import com.pwvault.app.ui.settings.SettingsViewModel
import com.pwvault.app.ui.theme.PwVaultTheme
import com.pwvault.app.ui.unlock.BiometricUnlockScreen
import com.pwvault.app.ui.unlock.PinUnlockScreen
import com.pwvault.app.ui.unlock.SetupScreen
import com.pwvault.app.ui.unlock.UnlockScreen
import com.pwvault.app.ui.unlock.UnlockUiState
import com.pwvault.app.ui.unlock.UnlockViewModel
import com.pwvault.app.ui.vault.PasswordTemplateViewModel
import com.pwvault.app.ui.vault.TagViewModel
import com.pwvault.app.ui.vault.VaultScreen
import com.pwvault.app.ui.vault.VaultViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val EXPORT_DESTINATION_MIME_TYPE = "application/octet-stream"
private const val STATE_PENDING_EXPORT_TARGET = "pendingExportTarget"

private enum class BiometricOperation { UNLOCK, SETUP }

@AndroidEntryPoint
class MainActivity : FragmentActivity() {
    @Inject
    lateinit var backupPreferences: BackupPreferences

    private val unlockViewModel: UnlockViewModel by viewModels()
    private val vaultViewModel: VaultViewModel by viewModels()
    private val tagViewModel: TagViewModel by viewModels()
    private val passwordTemplateViewModel: PasswordTemplateViewModel by viewModels()
    private val exportViewModel: ExportViewModel by viewModels()
    private val settingsViewModel: SettingsViewModel by viewModels()
    private lateinit var biometricPrompt: BiometricPrompt
    private lateinit var unlockPromptInfo: BiometricPrompt.PromptInfo
    private lateinit var setupPromptInfo: BiometricPrompt.PromptInfo
    private lateinit var exportDestinationLauncher: ActivityResultLauncher<String>
    private lateinit var autoBackupFolderLauncher: ActivityResultLauncher<Uri?>
    private lateinit var notificationPermissionLauncher: ActivityResultLauncher<String>
    private var pendingBiometricOperation = BiometricOperation.UNLOCK
    private var pendingExportTarget: ExportTarget? = null
    private var hasAutoBackupFolder by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (BuildConfig.ENABLE_SCREENSHOT_BLOCK) {
            window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)
        }
        // A screen rotation while the system "save as" picker is open recreates this Activity —
        // without restoring this, the picker's result would come back to a fresh instance with
        // pendingExportTarget == null and onExportDestinationPicked would silently no-op, stranding
        // the export flow (the ExportViewModel state survives via ViewModelStore, but nothing would
        // ever call onDestinationPicked to move it out of PickDestination).
        pendingExportTarget =
            savedInstanceState?.getString(STATE_PENDING_EXPORT_TARGET)?.let { ExportTarget.valueOf(it) }

        unlockPromptInfo = createBiometricPromptInfo(getString(R.string.use_master_password_instead))
        setupPromptInfo = createBiometricPromptInfo(getString(R.string.pin_setup_cancel))
        biometricPrompt = BiometricPrompt(this, ContextCompat.getMainExecutor(this), biometricAuthenticationCallback())
        exportDestinationLauncher =
            registerForActivityResult(ActivityResultContracts.CreateDocument(EXPORT_DESTINATION_MIME_TYPE)) { uri ->
                onExportDestinationPicked(uri)
            }
        autoBackupFolderLauncher =
            registerForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
                onAutoBackupFolderPicked(uri)
            }
        notificationPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) {}
        requestNotificationPermissionIfNeeded()
        hasAutoBackupFolder = backupPreferences.getAutoBackupFolderUri() != null
        val canSetupBiometric =
            BiometricManager.from(this).canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG) ==
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
                    ThemeMode.SYSTEM -> isSystemInDarkTheme()
                }
            PwVaultTheme(darkTheme = darkTheme) {
                PwVaultApp(
                    unlockViewModel = unlockViewModel,
                    vaultViewModel = vaultViewModel,
                    tagViewModel = tagViewModel,
                    passwordTemplateViewModel = passwordTemplateViewModel,
                    exportViewModel = exportViewModel,
                    settingsViewModel = settingsViewModel,
                    canSetupBiometric = canSetupBiometric,
                    onAuthenticateBiometricUnlock = ::triggerBiometricUnlock,
                    onSetupBiometric = ::triggerBiometricSetup,
                    onPickExportDestination = ::triggerExportDestinationPicker,
                    hasAutoBackupFolder = hasAutoBackupFolder,
                    onPickAutoBackupFolder = { autoBackupFolderLauncher.launch(null) },
                )
            }
        }
    }

    private fun triggerExportDestinationPicker(
        target: ExportTarget,
        suggestedFileName: String,
    ) {
        pendingExportTarget = target
        exportDestinationLauncher.launch(suggestedFileName)
    }

    private fun onExportDestinationPicked(uri: Uri?) {
        val target = pendingExportTarget ?: return
        pendingExportTarget = null
        exportViewModel.onDestinationPicked(target, uri)
    }

    private fun onAutoBackupFolderPicked(uri: Uri?) {
        if (uri == null) return
        contentResolver.takePersistableUriPermission(
            uri,
            Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
        )
        backupPreferences.setAutoBackupFolderUri(uri)
        hasAutoBackupFolder = true
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

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        pendingExportTarget?.let { outState.putString(STATE_PENDING_EXPORT_TARGET, it.name) }
    }
}

@Composable
private fun PwVaultApp(
    unlockViewModel: UnlockViewModel,
    vaultViewModel: VaultViewModel,
    tagViewModel: TagViewModel,
    passwordTemplateViewModel: PasswordTemplateViewModel,
    exportViewModel: ExportViewModel,
    settingsViewModel: SettingsViewModel,
    canSetupBiometric: Boolean,
    onAuthenticateBiometricUnlock: () -> Unit,
    onSetupBiometric: () -> Unit,
    onPickExportDestination: (ExportTarget, String) -> Unit,
    hasAutoBackupFolder: Boolean,
    onPickAutoBackupFolder: () -> Unit,
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
                viewModel = vaultViewModel,
                tagViewModel = tagViewModel,
                passwordTemplateViewModel = passwordTemplateViewModel,
                exportViewModel = exportViewModel,
                settingsViewModel = settingsViewModel,
                onVerifyMasterPassword = unlockViewModel::verifyMasterPassword,
                onPickExportDestination = onPickExportDestination,
                hasAutoBackupFolder = hasAutoBackupFolder,
                onPickAutoBackupFolder = onPickAutoBackupFolder,
            )
    }
}
