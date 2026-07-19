package com.pwvault.app.ui.vault

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pwvault.app.data.PasswordTemplateRepository
import com.pwvault.app.domain.PasswordTemplate
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class PasswordTemplateError { NAME_REQUIRED, NAME_DUPLICATE }

data class PasswordTemplateUiState(
    val templates: List<PasswordTemplate> = emptyList(),
    val newTemplateError: PasswordTemplateError? = null,
)

@HiltViewModel
class PasswordTemplateViewModel
    @Inject
    constructor(
        private val repository: PasswordTemplateRepository,
    ) : ViewModel() {
        private val _state = MutableStateFlow(PasswordTemplateUiState())
        val state: StateFlow<PasswordTemplateUiState> = _state.asStateFlow()

        private var observeJob: Job? = null

        /** Starts (re)observing templates — call once per unlock session, same reasoning as TagViewModel. */
        fun startObservingTemplates() {
            _state.value = _state.value.copy(newTemplateError = null)
            observeJob?.cancel()
            observeJob =
                viewModelScope.launch {
                    repository.observeTemplates().collect { templates ->
                        _state.value = _state.value.copy(templates = templates)
                    }
                }
        }

        fun addTemplate(template: PasswordTemplate) {
            val error =
                when {
                    template.name.isBlank() -> PasswordTemplateError.NAME_REQUIRED
                    _state.value.templates.any { it.name.equals(template.name, ignoreCase = true) } ->
                        PasswordTemplateError.NAME_DUPLICATE
                    else -> null
                }
            if (error != null) {
                _state.value = _state.value.copy(newTemplateError = error)
                return
            }
            _state.value = _state.value.copy(newTemplateError = null)
            viewModelScope.launch {
                runCatching { repository.addTemplate(template) }
                    .onFailure {
                        _state.value = _state.value.copy(newTemplateError = PasswordTemplateError.NAME_DUPLICATE)
                    }
            }
        }

        fun deleteTemplate(template: PasswordTemplate) {
            viewModelScope.launch { repository.deleteTemplate(template) }
        }
    }
