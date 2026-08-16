package com.pwvault.app.ui.unlock

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pwvault.app.data.VaultFileManager
import com.pwvault.app.security.AutoLockPreferences
import com.pwvault.app.security.BackupPreferences
import com.pwvault.app.security.BiometricUnlockManager
import com.pwvault.app.security.KeyDerivation
import com.pwvault.app.security.LockoutPolicy
import com.pwvault.app.security.PinManager
import com.pwvault.app.security.VaultMetadataStore
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Arrays
import javax.crypto.Cipher
import javax.inject.Inject

const val MIN_PASSWORD_LENGTH = 8
const val MIN_PIN_LENGTH = 4
private const val WIPE_CHAR = ' '

enum class UnlockError {
    PASSWORD_MISMATCH,
    PASSWORD_TOO_SHORT,
    CREATE_FAILED,
    WRONG_PASSWORD,
    PIN_MISMATCH,
    PIN_TOO_SHORT,
    PIN_NOT_NUMERIC,
    WRONG_PIN,
    BIOMETRIC_FAILED,
    BIOMETRIC_SETUP_FAILED,
    RESTORE_INVALID_FILE,
    RESTORE_WRONG_PASSWORD,
}

sealed interface UnlockUiState {
    data object Loading : UnlockUiState

    data class Setup(
        val error: UnlockError? = null,
        val busy: Boolean = false,
    ) : UnlockUiState

    data class RestorePassword(
        val error: UnlockError? = null,
        val busy: Boolean = false,
    ) : UnlockUiState

    data class Locked(
        val error: UnlockError? = null,
        val busy: Boolean = false,
        val lockedUntilMillis: Long? = null,
    ) : UnlockUiState

    data class PinEntry(
        val hasBiometric: Boolean,
        val error: UnlockError? = null,
        val busy: Boolean = false,
        val lockedUntilMillis: Long? = null,
    ) : UnlockUiState

    data class BiometricEntry(
        val hasPin: Boolean,
        val error: UnlockError? = null,
        val busy: Boolean = false,
    ) : UnlockUiState

    data class Unlocked(
        val hasPin: Boolean,
        val hasBiometric: Boolean,
        val pinSetupError: UnlockError? = null,
        val pinSetupBusy: Boolean = false,
        val biometricSetupError: UnlockError? = null,
        val biometricSetupBusy: Boolean = false,
    ) : UnlockUiState
}

@HiltViewModel
class UnlockViewModel
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
        private val keyDerivation: KeyDerivation,
        private val metadataStore: VaultMetadataStore,
        private val vaultFileManager: VaultFileManager,
        private val pinManager: PinManager,
        private val biometricUnlockManager: BiometricUnlockManager,
        private val lockoutPolicy: LockoutPolicy,
        private val autoLockPreferences: AutoLockPreferences,
        private val backupPreferences: BackupPreferences,
    ) : ViewModel() {
        private val _state = MutableStateFlow<UnlockUiState>(initialState())
        val state: StateFlow<UnlockUiState> = _state.asStateFlow()

        /**
         * Vault key held only while unlocked, for actions (e.g. PIN/biometric setup) that need to wrap it.
         * Never persisted.
         */
        private var vaultKey: ByteArray? = null

        /** Salt extracted from the `.pwvbackup` staged by [onRestoreFilePicked], consumed by [restoreVault]. */
        private var pendingRestoreSalt: ByteArray? = null

        /** Set on [onAppBackgrounded], consumed on [onAppForegrounded]. Not persisted — see plan's Risks. */
        private var backgroundedAtMillis: Long? = null

        private fun initialState(): UnlockUiState {
            if (!vaultFileManager.hasVaultFile()) return UnlockUiState.Setup()
            return lockedState()
        }

        private fun lockedState(): UnlockUiState {
            val lockedUntilMillis = lockoutPolicy.currentLockoutUntilMillis()
            return when {
                biometricUnlockManager.hasBiometric() -> UnlockUiState.BiometricEntry(hasPin = pinManager.hasPin())
                pinManager.hasPin() ->
                    UnlockUiState.PinEntry(hasBiometric = false, lockedUntilMillis = lockedUntilMillis)
                else -> UnlockUiState.Locked(lockedUntilMillis = lockedUntilMillis)
            }
        }

        fun createVault(
            password: CharArray,
            confirm: CharArray,
        ) {
            if (!password.contentEquals(confirm)) {
                Arrays.fill(password, WIPE_CHAR)
                Arrays.fill(confirm, WIPE_CHAR)
                _state.value = UnlockUiState.Setup(error = UnlockError.PASSWORD_MISMATCH)
                return
            }
            Arrays.fill(confirm, WIPE_CHAR)
            if (password.size < MIN_PASSWORD_LENGTH) {
                Arrays.fill(password, WIPE_CHAR)
                _state.value = UnlockUiState.Setup(error = UnlockError.PASSWORD_TOO_SHORT)
                return
            }
            _state.value = UnlockUiState.Setup(busy = true)
            viewModelScope.launch {
                val salt = metadataStore.getOrCreateSalt()
                val key = keyDerivation.derive(password, salt)
                val success = vaultFileManager.createVault(key)
                _state.value =
                    if (success) {
                        vaultKey = key
                        backupPreferences.seedReminderClockAtVaultCreation()
                        UnlockUiState.Unlocked(hasPin = false, hasBiometric = false)
                    } else {
                        Arrays.fill(key, 0)
                        UnlockUiState.Setup(error = UnlockError.CREATE_FAILED)
                    }
            }
        }

        /** Called with the SAF `Uri` the user picked via "Restore from backup" on [UnlockUiState.Setup]. */
        fun onRestoreFilePicked(uri: Uri?) {
            if (uri == null) return
            _state.value = UnlockUiState.Setup(busy = true)
            viewModelScope.launch {
                val salt =
                    context.contentResolver.openInputStream(uri)?.use { input ->
                        vaultFileManager.stageRestoreCandidate(input)
                    }
                _state.value =
                    if (salt == null) {
                        UnlockUiState.Setup(error = UnlockError.RESTORE_INVALID_FILE)
                    } else {
                        pendingRestoreSalt = salt
                        UnlockUiState.RestorePassword()
                    }
            }
        }

        fun restoreVault(password: CharArray) {
            val salt = pendingRestoreSalt
            if (salt == null) {
                Arrays.fill(password, WIPE_CHAR)
                _state.value = UnlockUiState.Setup()
                return
            }
            _state.value = UnlockUiState.RestorePassword(busy = true)
            viewModelScope.launch {
                val key = keyDerivation.derive(password, salt)
                val success = vaultFileManager.tryActivateRestoreCandidate(key)
                _state.value =
                    if (success) {
                        pendingRestoreSalt = null
                        metadataStore.setSalt(salt)
                        vaultKey = key
                        backupPreferences.seedReminderClockAtVaultCreation()
                        UnlockUiState.Unlocked(hasPin = false, hasBiometric = false)
                    } else {
                        Arrays.fill(key, 0)
                        UnlockUiState.RestorePassword(error = UnlockError.RESTORE_WRONG_PASSWORD)
                    }
            }
        }

        fun cancelRestore() {
            pendingRestoreSalt = null
            _state.value = UnlockUiState.Setup()
            viewModelScope.launch { vaultFileManager.discardRestoreCandidate() }
        }

        fun unlock(password: CharArray) {
            val lockedUntilMillis = lockoutPolicy.currentLockoutUntilMillis()
            if (lockedUntilMillis != null) {
                Arrays.fill(password, WIPE_CHAR)
                _state.value = UnlockUiState.Locked(lockedUntilMillis = lockedUntilMillis)
                return
            }
            _state.value = UnlockUiState.Locked(busy = true)
            viewModelScope.launch {
                val salt = metadataStore.getOrCreateSalt()
                val key = keyDerivation.derive(password, salt)
                val success = vaultFileManager.openVault(key)
                _state.value =
                    if (success) {
                        lockoutPolicy.recordSuccess()
                        vaultKey = key
                        UnlockUiState.Unlocked(
                            hasPin = pinManager.hasPin(),
                            hasBiometric = biometricUnlockManager.hasBiometric(),
                        )
                    } else {
                        Arrays.fill(key, 0)
                        lockoutPolicy.recordFailure()
                        UnlockUiState.Locked(
                            error = UnlockError.WRONG_PASSWORD,
                            lockedUntilMillis = lockoutPolicy.currentLockoutUntilMillis(),
                        )
                    }
            }
        }

        fun unlockWithPin(pin: CharArray) {
            val hasBiometric = biometricUnlockManager.hasBiometric()
            val lockedUntilMillis = lockoutPolicy.currentLockoutUntilMillis()
            if (lockedUntilMillis != null) {
                Arrays.fill(pin, WIPE_CHAR)
                _state.value =
                    UnlockUiState.PinEntry(hasBiometric = hasBiometric, lockedUntilMillis = lockedUntilMillis)
                return
            }
            _state.value = UnlockUiState.PinEntry(hasBiometric = hasBiometric, busy = true)
            viewModelScope.launch {
                val key = pinManager.verifyPin(pin)
                _state.value =
                    if (key != null && vaultFileManager.openVault(key)) {
                        lockoutPolicy.recordSuccess()
                        vaultKey = key
                        UnlockUiState.Unlocked(hasPin = true, hasBiometric = hasBiometric)
                    } else {
                        key?.let { Arrays.fill(it, 0) }
                        lockoutPolicy.recordFailure()
                        UnlockUiState.PinEntry(
                            hasBiometric = hasBiometric,
                            error = UnlockError.WRONG_PIN,
                            lockedUntilMillis = lockoutPolicy.currentLockoutUntilMillis(),
                        )
                    }
            }
        }

        /**
         * Re-verifies [password] against the currently unlocked session's key, without reopening the
         * Vault database — used to re-authenticate before a sensitive action (e.g. export) while
         * already `Unlocked`. Always wipes [password].
         */
        suspend fun verifyMasterPassword(password: CharArray): Boolean {
            val currentKey = vaultKey
            if (currentKey == null) {
                Arrays.fill(password, WIPE_CHAR)
                return false
            }
            val salt = metadataStore.getOrCreateSalt()
            val candidateKey = keyDerivation.derive(password, salt)
            val matches = candidateKey.contentEquals(currentKey)
            Arrays.fill(candidateKey, 0)
            return matches
        }

        fun onAppBackgrounded() {
            backgroundedAtMillis = System.currentTimeMillis()
        }

        fun onAppForegrounded() {
            val backgroundedAt = backgroundedAtMillis ?: return
            backgroundedAtMillis = null
            val timeout = autoLockPreferences.getTimeout() ?: return
            val elapsed = System.currentTimeMillis() - backgroundedAt
            if (elapsed < timeout.inWholeMilliseconds) return
            if (_state.value !is UnlockUiState.Unlocked) return
            vaultKey?.let { Arrays.fill(it, 0) }
            vaultKey = null
            vaultFileManager.close()
            _state.value = lockedState()
        }

        fun switchToMasterPassword() {
            _state.value = UnlockUiState.Locked(lockedUntilMillis = lockoutPolicy.currentLockoutUntilMillis())
        }

        fun switchToPin() {
            _state.value =
                UnlockUiState.PinEntry(
                    hasBiometric = biometricUnlockManager.hasBiometric(),
                    lockedUntilMillis = lockoutPolicy.currentLockoutUntilMillis(),
                )
        }

        fun switchToBiometric() {
            _state.value = UnlockUiState.BiometricEntry(hasPin = pinManager.hasPin())
        }

        fun setupPin(
            pin: CharArray,
            confirm: CharArray,
        ) {
            val key = vaultKey ?: return
            val hasBiometric = biometricUnlockManager.hasBiometric()
            if (!pin.contentEquals(confirm)) {
                Arrays.fill(pin, WIPE_CHAR)
                Arrays.fill(confirm, WIPE_CHAR)
                _state.value =
                    UnlockUiState.Unlocked(
                        hasPin = false,
                        hasBiometric = hasBiometric,
                        pinSetupError = UnlockError.PIN_MISMATCH,
                    )
                return
            }
            Arrays.fill(confirm, WIPE_CHAR)
            if (pin.size < MIN_PIN_LENGTH) {
                Arrays.fill(pin, WIPE_CHAR)
                _state.value =
                    UnlockUiState.Unlocked(
                        hasPin = false,
                        hasBiometric = hasBiometric,
                        pinSetupError = UnlockError.PIN_TOO_SHORT,
                    )
                return
            }
            if (pin.any { it !in '0'..'9' }) {
                Arrays.fill(pin, WIPE_CHAR)
                _state.value =
                    UnlockUiState.Unlocked(
                        hasPin = false,
                        hasBiometric = hasBiometric,
                        pinSetupError = UnlockError.PIN_NOT_NUMERIC,
                    )
                return
            }
            _state.value = UnlockUiState.Unlocked(hasPin = false, hasBiometric = hasBiometric, pinSetupBusy = true)
            viewModelScope.launch {
                pinManager.setupPin(pin, key)
                _state.value = UnlockUiState.Unlocked(hasPin = true, hasBiometric = hasBiometric)
            }
        }

        /** No-op unless the other method (Biometric) is on — at least one of {PIN, Biometric} must stay enabled. */
        fun disablePin() {
            val current = _state.value as? UnlockUiState.Unlocked ?: return
            if (!current.hasBiometric) return
            pinManager.clearPin()
            _state.value = current.copy(hasPin = false)
        }

        /** No-op unless the other method (PIN) is on — at least one of {PIN, Biometric} must stay enabled. */
        fun disableBiometric() {
            val current = _state.value as? UnlockUiState.Unlocked ?: return
            if (!current.hasPin) return
            biometricUnlockManager.disable()
            _state.value = current.copy(hasBiometric = false)
        }

        /** Prepares the unlock [Cipher]; the caller (Activity) must authorize it via `BiometricPrompt` before use. */
        suspend fun prepareBiometricUnlockCipher(): Cipher? {
            _state.value = UnlockUiState.BiometricEntry(hasPin = pinManager.hasPin(), busy = true)
            return biometricUnlockManager.prepareUnlockCipher()
        }

        /** [cipher] must already be authorized by `BiometricPrompt` (i.e. the one returned in its success callback). */
        fun completeBiometricUnlock(cipher: Cipher) {
            viewModelScope.launch {
                val key = biometricUnlockManager.completeUnlock(cipher)
                _state.value =
                    if (key != null && vaultFileManager.openVault(key)) {
                        vaultKey = key
                        UnlockUiState.Unlocked(hasPin = pinManager.hasPin(), hasBiometric = true)
                    } else {
                        key?.let { Arrays.fill(it, 0) }
                        UnlockUiState.BiometricEntry(hasPin = pinManager.hasPin(), error = UnlockError.BIOMETRIC_FAILED)
                    }
            }
        }

        fun onBiometricUnlockError() {
            _state.value =
                UnlockUiState.BiometricEntry(hasPin = pinManager.hasPin(), error = UnlockError.BIOMETRIC_FAILED)
        }

        fun onBiometricUnlockCancelled() {
            _state.value = UnlockUiState.BiometricEntry(hasPin = pinManager.hasPin())
        }

        /** Prepares the setup [Cipher]; the caller (Activity) must authorize it via `BiometricPrompt` before use. */
        suspend fun prepareBiometricSetupCipher(): Cipher? {
            if (vaultKey == null) return null
            _state.value =
                UnlockUiState.Unlocked(hasPin = pinManager.hasPin(), hasBiometric = false, biometricSetupBusy = true)
            return biometricUnlockManager.prepareSetupCipher()
        }

        /** [cipher] must already be authorized by `BiometricPrompt` (i.e. the one returned in its success callback). */
        fun completeBiometricSetup(cipher: Cipher) {
            val key = vaultKey ?: return
            viewModelScope.launch {
                biometricUnlockManager.completeSetup(cipher, key)
                _state.value = UnlockUiState.Unlocked(hasPin = pinManager.hasPin(), hasBiometric = true)
            }
        }

        fun onBiometricSetupError() {
            _state.value =
                UnlockUiState.Unlocked(
                    hasPin = pinManager.hasPin(),
                    hasBiometric = false,
                    biometricSetupError = UnlockError.BIOMETRIC_SETUP_FAILED,
                )
        }

        fun onBiometricSetupCancelled() {
            _state.value = UnlockUiState.Unlocked(hasPin = pinManager.hasPin(), hasBiometric = false)
        }
    }
