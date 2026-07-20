package com.pwvault.app.ui.settings

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Label
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.IosShare
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.pwvault.app.R
import com.pwvault.app.security.ThemeMode
import com.pwvault.app.ui.unlock.UnlockUiState
import com.pwvault.app.ui.unlock.message
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

@Composable
fun SettingsScreen(
    unlockedState: UnlockUiState.Unlocked,
    canSetupBiometric: Boolean,
    onSetupPin: () -> Unit,
    onSetupBiometric: () -> Unit,
    onManageTags: () -> Unit,
    onExport: () -> Unit,
    hasAutoBackupFolder: Boolean,
    onPickAutoBackupFolder: () -> Unit,
    viewModel: SettingsViewModel,
    onBack: () -> Unit,
) {
    val state = viewModel.state.collectAsState().value

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
    ) {
        Text(stringResource(R.string.settings_title), style = MaterialTheme.typography.headlineSmall)

        SettingsSectionTitle(R.string.settings_unlock_method_section)
        UnlockMethodSection(unlockedState, canSetupBiometric, onSetupPin, onSetupBiometric)

        SettingsSectionTitle(R.string.settings_security_section)
        AutoLockSection(
            options = viewModel.autoLockOptions,
            selected = state.autoLockTimeout,
            onSelect = viewModel::setAutoLockTimeout,
        )

        SettingsSectionTitle(R.string.settings_appearance_section)
        ThemeSection(selected = state.themeMode, onSelect = viewModel::setThemeMode)

        SettingsSectionTitle(R.string.settings_data_section)
        DataSection(onManageTags, onExport, hasAutoBackupFolder, onPickAutoBackupFolder)

        TextButton(onClick = onBack, modifier = Modifier.fillMaxWidth().padding(top = 24.dp)) {
            Text(stringResource(R.string.vault_back_button))
        }
    }
}

@Composable
private fun SettingsSectionTitle(
    @StringRes textRes: Int,
) {
    Text(
        text = stringResource(textRes),
        style = MaterialTheme.typography.labelLarge,
        modifier = Modifier.padding(top = 24.dp, bottom = 8.dp),
    )
}

@Composable
private fun UnlockMethodSection(
    unlockedState: UnlockUiState.Unlocked,
    canSetupBiometric: Boolean,
    onSetupPin: () -> Unit,
    onSetupBiometric: () -> Unit,
) {
    if (unlockedState.hasPin) {
        Text(stringResource(R.string.pin_enabled_label))
    } else {
        Button(onClick = onSetupPin) { Text(stringResource(R.string.setup_pin_button)) }
    }
    if (unlockedState.hasBiometric) {
        Text(stringResource(R.string.biometric_enabled_label), modifier = Modifier.padding(top = 8.dp))
    } else if (canSetupBiometric) {
        Column(modifier = Modifier.padding(top = 8.dp)) {
            Button(onClick = onSetupBiometric, enabled = !unlockedState.biometricSetupBusy) {
                Text(stringResource(R.string.setup_biometric_button))
            }
            if (unlockedState.biometricSetupError != null) {
                Text(text = unlockedState.biometricSetupError.message(), color = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
private fun AutoLockSection(
    options: List<Duration?>,
    selected: Duration?,
    onSelect: (Duration?) -> Unit,
) {
    Row {
        options.forEach { option ->
            FilterChip(
                selected = option == selected,
                onClick = { onSelect(option) },
                label = { Text(autoLockLabel(option)) },
                modifier = Modifier.padding(end = 8.dp),
            )
        }
    }
}

@Composable
private fun autoLockLabel(duration: Duration?): String =
    when (duration) {
        30.seconds -> stringResource(R.string.auto_lock_30s)
        1.minutes -> stringResource(R.string.auto_lock_1m)
        5.minutes -> stringResource(R.string.auto_lock_5m)
        15.minutes -> stringResource(R.string.auto_lock_15m)
        null -> stringResource(R.string.auto_lock_never)
        else -> duration.toString()
    }

@Composable
private fun ThemeSection(
    selected: ThemeMode,
    onSelect: (ThemeMode) -> Unit,
) {
    Row {
        FilterChip(
            selected = selected == ThemeMode.LIGHT,
            onClick = { onSelect(ThemeMode.LIGHT) },
            label = { Text(stringResource(R.string.theme_light)) },
            modifier = Modifier.padding(end = 8.dp),
        )
        FilterChip(
            selected = selected == ThemeMode.DARK,
            onClick = { onSelect(ThemeMode.DARK) },
            label = { Text(stringResource(R.string.theme_dark)) },
            modifier = Modifier.padding(end = 8.dp),
        )
        FilterChip(
            selected = selected == ThemeMode.SYSTEM,
            onClick = { onSelect(ThemeMode.SYSTEM) },
            label = { Text(stringResource(R.string.theme_system)) },
        )
    }
}

@Composable
private fun DataSection(
    onManageTags: () -> Unit,
    onExport: () -> Unit,
    hasAutoBackupFolder: Boolean,
    onPickAutoBackupFolder: () -> Unit,
) {
    TextButton(onClick = onManageTags) {
        Icon(Icons.AutoMirrored.Filled.Label, contentDescription = null, modifier = Modifier.padding(end = 4.dp))
        Text(stringResource(R.string.manage_tags_cd))
    }
    TextButton(onClick = onExport) {
        Icon(Icons.Filled.IosShare, contentDescription = null, modifier = Modifier.padding(end = 4.dp))
        Text(stringResource(R.string.export_cd))
    }
    TextButton(onClick = onPickAutoBackupFolder) {
        Icon(Icons.Filled.FolderOpen, contentDescription = null, modifier = Modifier.padding(end = 4.dp))
        Text(stringResource(if (hasAutoBackupFolder) R.string.auto_backup_on_cd else R.string.auto_backup_off_cd))
    }
}
