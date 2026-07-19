package com.pwvault.app.ui.vault

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pwvault.app.data.TagRepository
import com.pwvault.app.domain.Tag
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class TagError { NAME_REQUIRED, NAME_DUPLICATE }

data class TagManagerUiState(
    val tags: List<Tag> = emptyList(),
    val newTagError: TagError? = null,
    val renamingTag: Tag? = null,
    val renameError: TagError? = null,
    val deletingTag: Tag? = null,
)

@HiltViewModel
class TagViewModel
    @Inject
    constructor(
        private val repository: TagRepository,
    ) : ViewModel() {
        private val _state = MutableStateFlow(TagManagerUiState())
        val state: StateFlow<TagManagerUiState> = _state.asStateFlow()

        private var observeJob: Job? = null

        /**
         * Starts (re)observing tags — call once per unlock session, same reasoning as
         * [VaultViewModel.startObservingItems]. Also clears any error left over from a previous
         * visit to this screen, so it doesn't show up out of context on the next one.
         */
        fun startObservingTags() {
            _state.value = _state.value.copy(newTagError = null, renameError = null)
            observeJob?.cancel()
            observeJob =
                viewModelScope.launch {
                    repository.observeTags().collect { tags ->
                        _state.value = _state.value.copy(tags = tags)
                    }
                }
        }

        fun addTag(name: String) {
            val error = validateName(name, excluding = null)
            if (error != null) {
                _state.value = _state.value.copy(newTagError = error)
                return
            }
            _state.value = _state.value.copy(newTagError = null)
            viewModelScope.launch {
                runCatching { repository.addTag(name) }
                    .onFailure { _state.value = _state.value.copy(newTagError = TagError.NAME_DUPLICATE) }
            }
        }

        fun startRename(tag: Tag) {
            _state.value = _state.value.copy(renamingTag = tag, renameError = null)
        }

        fun cancelRename() {
            _state.value = _state.value.copy(renamingTag = null, renameError = null)
        }

        fun confirmRename(newName: String) {
            val tag = _state.value.renamingTag ?: return
            val error = validateName(newName, excluding = tag.id)
            if (error != null) {
                _state.value = _state.value.copy(renameError = error)
                return
            }
            viewModelScope.launch {
                runCatching { repository.renameTag(tag, newName) }
                    .fold(
                        onSuccess = { _state.value = _state.value.copy(renamingTag = null, renameError = null) },
                        onFailure = { _state.value = _state.value.copy(renameError = TagError.NAME_DUPLICATE) },
                    )
            }
        }

        fun startDelete(tag: Tag) {
            _state.value = _state.value.copy(deletingTag = tag)
        }

        fun cancelDelete() {
            _state.value = _state.value.copy(deletingTag = null)
        }

        fun confirmDelete() {
            val tag = _state.value.deletingTag ?: return
            viewModelScope.launch {
                repository.deleteTag(tag)
                _state.value = _state.value.copy(deletingTag = null)
            }
        }

        private fun validateName(
            name: String,
            excluding: Long?,
        ): TagError? =
            when {
                name.isBlank() -> TagError.NAME_REQUIRED
                _state.value.tags.any { it.id != excluding && it.name.equals(name, ignoreCase = true) } ->
                    TagError.NAME_DUPLICATE
                else -> null
            }
    }
