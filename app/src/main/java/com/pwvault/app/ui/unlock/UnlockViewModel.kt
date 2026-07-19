package com.pwvault.app.ui.unlock

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pwvault.app.data.VaultFileManager
import com.pwvault.app.security.KeyDerivation
import com.pwvault.app.security.PinManager
import com.pwvault.app.security.VaultMetadataStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Arrays
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
}

sealed interface UnlockUiState {
    data object Loading : UnlockUiState

    data class Setup(
        val error: UnlockError? = null,
        val busy: Boolean = false,
    ) : UnlockUiState

    data class Locked(
        val error: UnlockError? = null,
        val busy: Boolean = false,
    ) : UnlockUiState

    data class PinEntry(
        val error: UnlockError? = null,
        val busy: Boolean = false,
    ) : UnlockUiState

    data class Unlocked(
        val hasPin: Boolean,
        val pinSetupError: UnlockError? = null,
        val pinSetupBusy: Boolean = false,
    ) : UnlockUiState
}

@HiltViewModel
class UnlockViewModel
    @Inject
    constructor(
        private val keyDerivation: KeyDerivation,
        private val metadataStore: VaultMetadataStore,
        private val vaultFileManager: VaultFileManager,
        private val pinManager: PinManager,
    ) : ViewModel() {
        private val _state = MutableStateFlow<UnlockUiState>(initialState())
        val state: StateFlow<UnlockUiState> = _state.asStateFlow()

        /** Vault key held only while unlocked, for actions (e.g. PIN setup) that need to wrap it. Never persisted. */
        private var vaultKey: ByteArray? = null

        private fun initialState(): UnlockUiState =
            when {
                !vaultFileManager.hasVaultFile() -> UnlockUiState.Setup()
                pinManager.hasPin() -> UnlockUiState.PinEntry()
                else -> UnlockUiState.Locked()
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
                        UnlockUiState.Unlocked(hasPin = false)
                    } else {
                        Arrays.fill(key, 0)
                        UnlockUiState.Setup(error = UnlockError.CREATE_FAILED)
                    }
            }
        }

        fun unlock(password: CharArray) {
            _state.value = UnlockUiState.Locked(busy = true)
            viewModelScope.launch {
                val salt = metadataStore.getOrCreateSalt()
                val key = keyDerivation.derive(password, salt)
                val success = vaultFileManager.openVault(key)
                _state.value =
                    if (success) {
                        vaultKey = key
                        UnlockUiState.Unlocked(hasPin = pinManager.hasPin())
                    } else {
                        Arrays.fill(key, 0)
                        UnlockUiState.Locked(error = UnlockError.WRONG_PASSWORD)
                    }
            }
        }

        fun unlockWithPin(pin: CharArray) {
            _state.value = UnlockUiState.PinEntry(busy = true)
            viewModelScope.launch {
                val key = pinManager.verifyPin(pin)
                _state.value =
                    if (key != null && vaultFileManager.openVault(key)) {
                        vaultKey = key
                        UnlockUiState.Unlocked(hasPin = true)
                    } else {
                        key?.let { Arrays.fill(it, 0) }
                        UnlockUiState.PinEntry(error = UnlockError.WRONG_PIN)
                    }
            }
        }

        fun switchToMasterPassword() {
            _state.value = UnlockUiState.Locked()
        }

        fun setupPin(
            pin: CharArray,
            confirm: CharArray,
        ) {
            val key = vaultKey ?: return
            if (!pin.contentEquals(confirm)) {
                Arrays.fill(pin, WIPE_CHAR)
                Arrays.fill(confirm, WIPE_CHAR)
                _state.value = UnlockUiState.Unlocked(hasPin = false, pinSetupError = UnlockError.PIN_MISMATCH)
                return
            }
            Arrays.fill(confirm, WIPE_CHAR)
            if (pin.size < MIN_PIN_LENGTH) {
                Arrays.fill(pin, WIPE_CHAR)
                _state.value = UnlockUiState.Unlocked(hasPin = false, pinSetupError = UnlockError.PIN_TOO_SHORT)
                return
            }
            if (pin.any { it !in '0'..'9' }) {
                Arrays.fill(pin, WIPE_CHAR)
                _state.value = UnlockUiState.Unlocked(hasPin = false, pinSetupError = UnlockError.PIN_NOT_NUMERIC)
                return
            }
            _state.value = UnlockUiState.Unlocked(hasPin = false, pinSetupBusy = true)
            viewModelScope.launch {
                pinManager.setupPin(pin, key)
                _state.value = UnlockUiState.Unlocked(hasPin = true)
            }
        }
    }
