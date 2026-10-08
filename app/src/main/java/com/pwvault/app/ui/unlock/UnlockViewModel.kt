package com.pwvault.app.ui.unlock

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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.security.SecureRandom
import java.util.Arrays
import javax.inject.Inject

const val MIN_PIN_LENGTH = 4
private const val VAULT_KEY_LENGTH_BYTES = 32
private const val WIPE_CHAR = ' '

enum class UnlockError {
    CREATE_FAILED,
    WRONG_PASSWORD,
    PIN_MISMATCH,
    PIN_TOO_SHORT,
    PIN_NOT_NUMERIC,
    WRONG_PIN,
    BIOMETRIC_FAILED,
    BIOMETRIC_SETUP_FAILED,
}

sealed interface UnlockUiState {
    data object Loading : UnlockUiState

    data class Setup(
        val error: UnlockError? = null,
        val busy: Boolean = false,
    ) : UnlockUiState

    /** Legacy only — a pre-Feature-19 vault that never got a PIN. See [UnlockViewModel.unlock]. */
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
        val error: UnlockError? = null,
        val busy: Boolean = false,
    ) : UnlockUiState

    data class Unlocked(
        val hasPin: Boolean,
        val hasBiometric: Boolean,
        val pinSetupError: UnlockError? = null,
        val pinSetupBusy: Boolean = false,
        val biometricSetupError: UnlockError? = null,
    ) : UnlockUiState
}

/**
 * The PIN is the only secret the user remembers: the Vault key is random, wrapped by a Keystore key
 * and gated by the PIN. Biometric (fingerprint/face) is an optional shortcut gate in front of the
 * same wrapped key — see docs/plans/feature-19-simplify-unlock-csv-plan.md.
 */
@HiltViewModel
class UnlockViewModel
    @Inject
    constructor(
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
         * Vault key held only while unlocked, for actions (e.g. PIN setup) that need to wrap it.
         * Never persisted.
         */
        private var vaultKey: ByteArray? = null

        /** Set on [onAppBackgrounded], consumed on [onAppForegrounded]. Not persisted — see plan's Risks. */
        private var backgroundedAtMillis: Long? = null

        private fun initialState(): UnlockUiState {
            if (!vaultFileManager.hasVaultFile()) return UnlockUiState.Setup()
            return lockedState()
        }

        private fun lockedState(): UnlockUiState {
            val lockedUntilMillis = lockoutPolicy.currentLockoutUntilMillis()
            return when {
                !pinManager.hasPin() -> UnlockUiState.Locked(lockedUntilMillis = lockedUntilMillis)
                biometricUnlockManager.hasBiometric() -> UnlockUiState.BiometricEntry()
                else -> UnlockUiState.PinEntry(hasBiometric = false, lockedUntilMillis = lockedUntilMillis)
            }
        }

        /** First run: the PIN set here is the only secret the user has to remember. */
        fun createVault(
            pin: CharArray,
            confirm: CharArray,
        ) {
            val error = validateNewPin(pin, confirm)
            if (error != null) {
                _state.value = UnlockUiState.Setup(error = error)
                return
            }
            _state.value = UnlockUiState.Setup(busy = true)
            viewModelScope.launch {
                val key = ByteArray(VAULT_KEY_LENGTH_BYTES).also { SecureRandom().nextBytes(it) }
                // PIN first: a vault file without a wrapped key would be unopenable, while a PIN
                // without a vault is simply rolled back below.
                pinManager.setupPin(pin, key)
                val success = vaultFileManager.createVault(key)
                _state.value =
                    if (success) {
                        vaultKey = key
                        backupPreferences.seedReminderClockAtVaultCreation()
                        UnlockUiState.Unlocked(hasPin = true, hasBiometric = false)
                    } else {
                        pinManager.clearPin()
                        Arrays.fill(key, 0)
                        UnlockUiState.Setup(error = UnlockError.CREATE_FAILED)
                    }
            }
        }

        /**
         * Validates a new PIN + its confirmation. Returns the error to show, or `null` if valid.
         * Always wipes [confirm]; wipes [pin] only on failure (on success the caller consumes it).
         */
        private fun validateNewPin(
            pin: CharArray,
            confirm: CharArray,
        ): UnlockError? {
            val error =
                when {
                    !pin.contentEquals(confirm) -> UnlockError.PIN_MISMATCH
                    pin.size < MIN_PIN_LENGTH -> UnlockError.PIN_TOO_SHORT
                    pin.any { it !in '0'..'9' } -> UnlockError.PIN_NOT_NUMERIC
                    else -> null
                }
            Arrays.fill(confirm, WIPE_CHAR)
            if (error != null) Arrays.fill(pin, WIPE_CHAR)
            return error
        }

        /**
         * Legacy only: a vault created before Feature 19 that never got a PIN opens with its old
         * Master Password one last time; `VaultScreen` then forces PIN setup (hasPin = false).
         */
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
                        UnlockUiState.Unlocked(hasPin = false, hasBiometric = false)
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
         * Re-verifies [pin] while already `Unlocked`, without reopening the Vault database — used to
         * re-authenticate before a sensitive action (export). Always wipes [pin].
         */
        suspend fun verifyPin(pin: CharArray): Boolean {
            val key = pinManager.verifyPin(pin) ?: return false
            Arrays.fill(key, 0)
            return true
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

        fun switchToPin() {
            _state.value =
                UnlockUiState.PinEntry(
                    hasBiometric = biometricUnlockManager.hasBiometric(),
                    lockedUntilMillis = lockoutPolicy.currentLockoutUntilMillis(),
                )
        }

        fun switchToBiometric() {
            _state.value = UnlockUiState.BiometricEntry()
        }

        /** Sets the PIN (forced after a legacy master-password unlock) or changes the current one. */
        fun setupPin(
            pin: CharArray,
            confirm: CharArray,
        ) {
            val key = vaultKey ?: return
            val current = _state.value as? UnlockUiState.Unlocked ?: return
            val error = validateNewPin(pin, confirm)
            if (error != null) {
                _state.value = current.copy(pinSetupError = error, pinSetupBusy = false)
                return
            }
            _state.value = current.copy(pinSetupError = null, pinSetupBusy = true)
            viewModelScope.launch {
                pinManager.setupPin(pin, key)
                _state.value = current.copy(hasPin = true, pinSetupError = null, pinSetupBusy = false)
            }
        }

        fun disableBiometric() {
            val current = _state.value as? UnlockUiState.Unlocked ?: return
            biometricUnlockManager.disable()
            _state.value = current.copy(hasBiometric = false)
        }

        /** Called once `BiometricPrompt` succeeded for unlock. */
        fun completeBiometricUnlock() {
            _state.value = UnlockUiState.BiometricEntry(busy = true)
            viewModelScope.launch {
                val key = pinManager.unwrapVaultKey()
                _state.value =
                    if (key != null && vaultFileManager.openVault(key)) {
                        vaultKey = key
                        UnlockUiState.Unlocked(hasPin = true, hasBiometric = true)
                    } else {
                        key?.let { Arrays.fill(it, 0) }
                        UnlockUiState.BiometricEntry(error = UnlockError.BIOMETRIC_FAILED)
                    }
            }
        }

        fun onBiometricUnlockError() {
            _state.value = UnlockUiState.BiometricEntry(error = UnlockError.BIOMETRIC_FAILED)
        }

        fun onBiometricUnlockCancelled() {
            _state.value = UnlockUiState.BiometricEntry()
        }

        /**
         * "Forgot PIN": called once the phone's own screen lock (PIN/pattern/password or biometric)
         * verified the user via `BiometricPrompt` + `DEVICE_CREDENTIAL`. The Vault key is unwrapped
         * without the old PIN; `hasPin = false` makes `VaultScreen` force setting a new PIN.
         * Fully offline — replaces the "reset by email" idea (see docs/functional-spec.md §4).
         */
        fun completeForgotPin() {
            val hasBiometric = biometricUnlockManager.hasBiometric()
            _state.value = UnlockUiState.PinEntry(hasBiometric = hasBiometric, busy = true)
            viewModelScope.launch {
                val key = pinManager.unwrapVaultKey()
                _state.value =
                    if (key != null && vaultFileManager.openVault(key)) {
                        lockoutPolicy.recordSuccess()
                        vaultKey = key
                        UnlockUiState.Unlocked(hasPin = false, hasBiometric = hasBiometric)
                    } else {
                        key?.let { Arrays.fill(it, 0) }
                        UnlockUiState.PinEntry(
                            hasBiometric = hasBiometric,
                            lockedUntilMillis = lockoutPolicy.currentLockoutUntilMillis(),
                        )
                    }
            }
        }

        /** Called once `BiometricPrompt` succeeded for setup — proves the user can actually use it. */
        fun completeBiometricSetup() {
            val current = _state.value as? UnlockUiState.Unlocked ?: return
            biometricUnlockManager.enable()
            _state.value = current.copy(hasBiometric = true, biometricSetupError = null)
        }

        fun onBiometricSetupError() {
            val current = _state.value as? UnlockUiState.Unlocked ?: return
            _state.value = current.copy(hasBiometric = false, biometricSetupError = UnlockError.BIOMETRIC_SETUP_FAILED)
        }
    }
