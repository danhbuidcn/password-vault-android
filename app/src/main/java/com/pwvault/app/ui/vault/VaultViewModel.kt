package com.pwvault.app.ui.vault

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pwvault.app.data.TagRepository
import com.pwvault.app.data.VaultItemRepository
import com.pwvault.app.domain.Tag
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
        val searchQuery: String = "",
    ) : VaultUiState

    data class ItemDetail(
        val item: VaultItem,
        val passwordVisible: Boolean = false,
        val copyEventId: Int = 0,
    ) : VaultUiState

    data class ItemForm(
        val editingId: Long? = null,
        val initial: VaultItem? = null,
        val availableTags: List<Tag> = emptyList(),
        val selectedTagIds: Set<Long> = emptySet(),
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
        private val tagRepository: TagRepository,
        private val clipboardClearer: ClipboardClearer,
    ) : ViewModel() {
        private val _state = MutableStateFlow<VaultUiState>(VaultUiState.ItemList())
        val state: StateFlow<VaultUiState> = _state.asStateFlow()

        private var latestItems: List<VaultItem> = emptyList()
        private var availableTags: List<Tag> = emptyList()
        private var searchQuery: String = ""
        private var observeJob: Job? = null
        private var tagsObserveJob: Job? = null

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
                            _state.value = currentListState()
                        }
                    }
                }
            tagsObserveJob?.cancel()
            tagsObserveJob =
                viewModelScope.launch {
                    tagRepository.observeTags().collect { tags -> availableTags = tags }
                }
        }

        fun updateSearchQuery(query: String) {
            searchQuery = query
            _state.value = currentListState()
        }

        private fun currentListState(): VaultUiState.ItemList {
            val filtered =
                if (searchQuery.isBlank()) {
                    latestItems
                } else {
                    latestItems.filter {
                        it.name.contains(searchQuery, ignoreCase = true) ||
                            it.username.contains(searchQuery, ignoreCase = true)
                    }
                }
            return VaultUiState.ItemList(items = filtered, searchQuery = searchQuery)
        }

        fun openAddForm() {
            _state.value = VaultUiState.ItemForm(availableTags = availableTags)
        }

        fun openEditForm(item: VaultItem) {
            _state.value =
                VaultUiState.ItemForm(
                    editingId = item.id,
                    initial = item,
                    availableTags = availableTags,
                    selectedTagIds = item.tags.map { it.id }.toSet(),
                )
        }

        fun toggleTagSelected(tagId: Long) {
            val form = _state.value as? VaultUiState.ItemForm ?: return
            val selected =
                if (tagId in form.selectedTagIds) form.selectedTagIds - tagId else form.selectedTagIds + tagId
            _state.value = form.copy(selectedTagIds = selected)
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
            _state.value = currentListState()
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
                val itemId =
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
                        existing.id
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
                repository.setItemTags(itemId, form.selectedTagIds)
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
