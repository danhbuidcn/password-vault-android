package com.pwvault.app.ui.settings

import androidx.lifecycle.ViewModel
import com.pwvault.app.security.AutoLockPreferences
import com.pwvault.app.security.SecurityPolicy
import com.pwvault.app.security.ThemeMode
import com.pwvault.app.security.ThemePreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import kotlin.time.Duration

data class SettingsUiState(
    val autoLockTimeout: Duration?,
    val themeMode: ThemeMode,
)

@HiltViewModel
class SettingsViewModel
    @Inject
    constructor(
        private val autoLockPreferences: AutoLockPreferences,
        private val themePreferences: ThemePreferences,
    ) : ViewModel() {
        private val _state =
            MutableStateFlow(
                SettingsUiState(
                    autoLockTimeout = autoLockPreferences.getTimeout(),
                    themeMode = themePreferences.getMode(),
                ),
            )
        val state: StateFlow<SettingsUiState> = _state.asStateFlow()

        val autoLockOptions: List<Duration?> = SecurityPolicy.AUTO_LOCK_TIMEOUT_OPTIONS

        fun setAutoLockTimeout(timeout: Duration?) {
            autoLockPreferences.setTimeout(timeout)
            _state.value = _state.value.copy(autoLockTimeout = timeout)
        }

        fun setThemeMode(mode: ThemeMode) {
            themePreferences.setMode(mode)
            _state.value = _state.value.copy(themeMode = mode)
        }
    }
