package com.pwvault.app.ui.vault

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pwvault.app.data.VaultItemRepository
import com.pwvault.app.domain.VaultItem
import com.pwvault.app.security.ClipboardClearer
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class VaultFormError { NAME_REQUIRED }

sealed interface VaultUiState {
    data class ItemList(
        val items: List<VaultItem> = emptyList(),
    ) : VaultUiState

    data class ItemDetail(
        val item: VaultItem,
        val passwordVisible: Boolean = false,
        val copyEventId: Int = 0,
    ) : VaultUiState

    data class ItemForm(
        val editingId: Long? = null,
        val initial: VaultItem? = null,
        val error: VaultFormError? = null,
        val busy: Boolean = false,
    ) : VaultUiState

    data class DeleteConfirm(
        val item: VaultItem,
    ) : VaultUiState
}

@HiltViewModel
class VaultViewModel
    @Inject
    constructor(
        private val repository: VaultItemRepository,
        private val clipboardClearer: ClipboardClearer,
    ) : ViewModel() {
        private val _state = MutableStateFlow<VaultUiState>(VaultUiState.ItemList())
        val state: StateFlow<VaultUiState> = _state.asStateFlow()

        private var latestItems: List<VaultItem> = emptyList()
        private var observeJob: Job? = null

        /**
         * Starts (re)observing the vault — call once per unlock session (e.g. from a
         * `LaunchedEffect(Unit)` when the vault screen enters composition). The vault database is
         * only open while `Unlocked`, and a fresh [VaultDatabase] instance is opened on every
         * unlock, so the previous collection (if any) must be cancelled rather than reused.
         */
        fun startObservingItems() {
            observeJob?.cancel()
            observeJob =
                viewModelScope.launch {
                    repository.observeItems().collect { items ->
                        latestItems = items
                        if (_state.value is VaultUiState.ItemList) {
                            _state.value = VaultUiState.ItemList(items)
                        }
                    }
                }
        }

        fun openAddForm() {
            _state.value = VaultUiState.ItemForm()
        }

        fun openEditForm(item: VaultItem) {
            _state.value = VaultUiState.ItemForm(editingId = item.id, initial = item)
        }

        fun openDetail(item: VaultItem) {
            _state.value = VaultUiState.ItemDetail(item)
        }

        fun openDeleteConfirm(item: VaultItem) {
            _state.value = VaultUiState.DeleteConfirm(item)
        }

        fun cancelDeleteConfirm() {
            val current = _state.value as? VaultUiState.DeleteConfirm ?: return
            _state.value = VaultUiState.ItemDetail(current.item)
        }

        fun backToList() {
            _state.value = VaultUiState.ItemList(latestItems)
        }

        fun togglePasswordVisible() {
            val current = _state.value as? VaultUiState.ItemDetail ?: return
            _state.value = current.copy(passwordVisible = !current.passwordVisible)
        }

        fun copyPassword() {
            val current = _state.value as? VaultUiState.ItemDetail ?: return
            clipboardClearer.copyPassword(current.item.password)
            _state.value = current.copy(copyEventId = current.copyEventId + 1)
        }

        fun save(
            name: String,
            username: String,
            password: String,
            url: String,
            note: String,
        ) {
            val form = _state.value as? VaultUiState.ItemForm ?: return
            if (name.isBlank()) {
                _state.value = form.copy(error = VaultFormError.NAME_REQUIRED)
                return
            }
            _state.value = form.copy(busy = true, error = null)
            viewModelScope.launch {
                val now = System.currentTimeMillis()
                val existing = form.initial
                if (existing != null) {
                    repository.updateItem(
                        existing.copy(
                            name = name,
                            username = username,
                            password = password,
                            url = url,
                            note = note,
                            updatedAt = now,
                        ),
                    )
                } else {
                    repository.addItem(
                        VaultItem(
                            id = 0,
                            name = name,
                            username = username,
                            password = password,
                            url = url,
                            note = note,
                            createdAt = now,
                            updatedAt = now,
                        ),
                    )
                }
                backToList()
            }
        }

        fun confirmDelete() {
            val current = _state.value as? VaultUiState.DeleteConfirm ?: return
            viewModelScope.launch {
                repository.deleteItem(current.item)
                backToList()
            }
        }
    }
