package com.pwvault.app.ui.unlock

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pwvault.app.data.VaultFileManager
import com.pwvault.app.security.KeyDerivation
import com.pwvault.app.security.VaultMetadataStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Arrays
import javax.inject.Inject

const val MIN_PASSWORD_LENGTH = 8
private const val WIPE_CHAR = ' '

enum class UnlockError {
    PASSWORD_MISMATCH,
    PASSWORD_TOO_SHORT,
    CREATE_FAILED,
    WRONG_PASSWORD,
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

    data object Unlocked : UnlockUiState
}

@HiltViewModel
class UnlockViewModel
    @Inject
    constructor(
        private val keyDerivation: KeyDerivation,
        private val metadataStore: VaultMetadataStore,
        private val vaultFileManager: VaultFileManager,
    ) : ViewModel() {
        private val _state =
            MutableStateFlow<UnlockUiState>(
                if (vaultFileManager.hasVaultFile()) UnlockUiState.Locked() else UnlockUiState.Setup(),
            )
        val state: StateFlow<UnlockUiState> = _state.asStateFlow()

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
                Arrays.fill(key, 0)
                _state.value =
                    if (success) {
                        UnlockUiState.Unlocked
                    } else {
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
                Arrays.fill(key, 0)
                _state.value =
                    if (success) {
                        UnlockUiState.Unlocked
                    } else {
                        UnlockUiState.Locked(error = UnlockError.WRONG_PASSWORD)
                    }
            }
        }
    }
