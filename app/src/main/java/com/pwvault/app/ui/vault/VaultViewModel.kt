package com.pwvault.app.ui.vault

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pwvault.app.data.TagRepository
import com.pwvault.app.data.VaultItemRepository
import com.pwvault.app.domain.CustomField
import com.pwvault.app.domain.MAX_TAGS_PER_VAULT_ITEM
import com.pwvault.app.domain.Tag
import com.pwvault.app.domain.VaultItem
import com.pwvault.app.domain.VaultItemType
import com.pwvault.app.security.ClipboardClearer
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class VaultFormError { NAME_REQUIRED }

data class VaultItemFormInput(
    val type: VaultItemType,
    val name: String,
    val username: String,
    val password: String,
    val url: String,
    val note: String,
    val customFields: List<Pair<String, String>>,
)

data class VaultItemWarning(
    val duplicate: Boolean = false,
) {
    val hasWarning: Boolean get() = duplicate
}

sealed interface VaultUiState {
    data class ItemList(
        val items: List<VaultItem> = emptyList(),
        val searchQuery: String = "",
        val availableTags: List<Tag> = emptyList(),
        val selectedTagFilterIds: Set<Long> = emptySet(),
    ) : VaultUiState

    data class ItemDetail(
        val item: VaultItem,
        val passwordVisible: Boolean = false,
        val copyEventId: Int = 0,
        val warning: VaultItemWarning = VaultItemWarning(),
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
        private var latestWarnings: Map<Long, VaultItemWarning> = emptyMap()
        private var availableTags: List<Tag> = emptyList()
        private var searchQuery: String = ""
        private var tagFilterIds: Set<Long> = emptySet()
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
                        latestWarnings = computeWarnings(items)
                        if (_state.value is VaultUiState.ItemList) {
                            _state.value = currentListState()
                        }
                    }
                }
            tagsObserveJob?.cancel()
            tagsObserveJob =
                viewModelScope.launch {
                    tagRepository.observeTags().collect { tags ->
                        availableTags = tags
                        if (_state.value is VaultUiState.ItemList) {
                            _state.value = currentListState()
                        }
                    }
                }
        }

        fun updateSearchQuery(query: String) {
            searchQuery = query
            _state.value = currentListState()
        }

        fun toggleTagFilter(tagId: Long) {
            tagFilterIds = if (tagId in tagFilterIds) tagFilterIds - tagId else tagFilterIds + tagId
            _state.value = currentListState()
        }

        fun clearTagFilter() {
            tagFilterIds = emptySet()
            _state.value = currentListState()
        }

        private fun currentListState(): VaultUiState.ItemList {
            val searchFiltered =
                if (searchQuery.isBlank()) {
                    latestItems
                } else {
                    latestItems.filter {
                        it.name.contains(searchQuery, ignoreCase = true) ||
                            it.username.contains(searchQuery, ignoreCase = true)
                    }
                }
            val tagFiltered =
                if (tagFilterIds.isEmpty()) {
                    searchFiltered
                } else {
                    searchFiltered.filter { item -> item.tags.any { it.id in tagFilterIds } }
                }
            return VaultUiState.ItemList(
                items = tagFiltered,
                searchQuery = searchQuery,
                availableTags = availableTags,
                selectedTagFilterIds = tagFilterIds,
            )
        }

        private fun computeWarnings(items: List<VaultItem>): Map<Long, VaultItemWarning> {
            val passwordCounts =
                items
                    .map { it.password }
                    .filter { it.isNotEmpty() }
                    .groupingBy { it }
                    .eachCount()
            return items.associate { item ->
                val hasPassword = item.password.isNotEmpty()
                item.id to VaultItemWarning(duplicate = hasPassword && (passwordCounts[item.password] ?: 0) > 1)
            }
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

        /** No-op once [MAX_TAGS_PER_VAULT_ITEM] tags are already selected — deselecting is always allowed. */
        fun toggleTagSelected(tagId: Long) {
            val form = _state.value as? VaultUiState.ItemForm ?: return
            val selected =
                when {
                    tagId in form.selectedTagIds -> form.selectedTagIds - tagId
                    form.selectedTagIds.size >= MAX_TAGS_PER_VAULT_ITEM -> return
                    else -> form.selectedTagIds + tagId
                }
            _state.value = form.copy(selectedTagIds = selected)
        }

        fun openDetail(item: VaultItem) {
            _state.value = VaultUiState.ItemDetail(item, warning = latestWarnings[item.id] ?: VaultItemWarning())
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

        fun save(input: VaultItemFormInput) {
            val form = _state.value as? VaultUiState.ItemForm ?: return
            if (input.name.isBlank()) {
                _state.value = form.copy(error = VaultFormError.NAME_REQUIRED)
                return
            }
            _state.value = form.copy(busy = true, error = null)
            viewModelScope.launch {
                val now = System.currentTimeMillis()
                val existing = form.initial
                val selectedTags = form.availableTags.filter { it.id in form.selectedTagIds }
                val itemId =
                    if (existing != null) {
                        // Type is fixed once an item is created — always keep existing.type here,
                        // never trust the passed-in type, even though the picker is hidden while editing.
                        repository.updateItem(
                            existing.copy(
                                name = input.name,
                                username = input.username,
                                password = input.password,
                                url = input.url,
                                note = input.note,
                                tags = selectedTags,
                                updatedAt = now,
                            ),
                        )
                        existing.id
                    } else {
                        // A Note item never carries login fields, even if the user typed into them
                        // before switching the type picker to Note.
                        val isNote = input.type == VaultItemType.NOTE
                        repository.addItem(
                            VaultItem(
                                id = 0,
                                type = input.type,
                                name = input.name,
                                username = if (isNote) "" else input.username,
                                password = if (isNote) "" else input.password,
                                url = if (isNote) "" else input.url,
                                note = input.note,
                                tags = selectedTags,
                                createdAt = now,
                                updatedAt = now,
                            ),
                        )
                    }
                repository.setItemTags(itemId, form.selectedTagIds)
                repository.setCustomFields(
                    itemId,
                    input.customFields
                        .filter { it.first.isNotBlank() || it.second.isNotBlank() }
                        .map { CustomField(label = it.first, value = it.second) },
                )
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
