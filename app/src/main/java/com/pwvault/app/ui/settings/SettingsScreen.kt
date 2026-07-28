package com.pwvault.app.ui.settings

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dialpad
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.IosShare
import androidx.compose.material.icons.filled.Password
import androidx.compose.material.icons.filled.Sell
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.pwvault.app.BuildConfig
import com.pwvault.app.R
import com.pwvault.app.security.AppLanguage
import com.pwvault.app.security.ThemeMode
import com.pwvault.app.ui.theme.PwVaultChrome
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
    onDisablePin: () -> Unit,
    onDisableBiometric: () -> Unit,
    onManageTags: () -> Unit,
    onExport: () -> Unit,
    onImport: () -> Unit,
    hasAutoBackupFolder: Boolean,
    onPickAutoBackupFolder: () -> Unit,
    onDisableAutoBackup: () -> Unit,
    viewModel: SettingsViewModel,
    onBack: () -> Unit,
) {
    val state = viewModel.state.collectAsState().value

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
        ) {
            SettingsBanner()

            SettingsSectionTitle(R.string.settings_unlock_method_section)
            UnlockMethodSection(
                unlockedState,
                canSetupBiometric,
                onSetupPin,
                onSetupBiometric,
                onDisablePin,
                onDisableBiometric,
            )

            SettingsSectionTitle(R.string.settings_security_section)
            Text(
                text = stringResource(R.string.settings_security_section_description),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 8.dp),
            )
            AutoLockSection(
                options = viewModel.autoLockOptions,
                selected = state.autoLockTimeout,
                onSelect = viewModel::setAutoLockTimeout,
            )

            SettingsSectionTitle(R.string.settings_appearance_section)
            ThemeSection(selected = state.themeMode, onSelect = viewModel::setThemeMode)

            SettingsSectionTitle(R.string.settings_language_section)
            LanguageSection(selected = state.language, onSelect = viewModel::setLanguage)

            SettingsSectionTitle(R.string.settings_data_section)
            DataSection(
                onManageTags,
                onExport,
                onImport,
                hasAutoBackupFolder,
                onPickAutoBackupFolder,
                onDisableAutoBackup,
            )

            TextButton(onClick = onBack, modifier = Modifier.fillMaxWidth().padding(top = 24.dp)) {
                Text(stringResource(R.string.vault_back_button))
            }
        }
    }
}

@Composable
private fun SettingsBanner() {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.linearGradient(listOf(Color(0xFF232838), Color(0xFF171A22))),
                    shape = RoundedCornerShape(20.dp),
                ).padding(vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer) {
            Icon(
                painter = painterResource(R.drawable.ic_launcher_monochrome),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.padding(14.dp).size(26.dp),
            )
        }
        Text(
            text = stringResource(R.string.app_name),
            style = MaterialTheme.typography.headlineSmall,
            color = PwVaultChrome.Content,
            modifier = Modifier.padding(top = 10.dp),
        )
        Text(
            text = stringResource(R.string.settings_tagline),
            style = MaterialTheme.typography.bodySmall,
            color = PwVaultChrome.ContentMuted,
            modifier = Modifier.padding(top = 2.dp),
        )
        Text(
            text = "v${BuildConfig.VERSION_NAME}",
            style = MaterialTheme.typography.labelSmall,
            color = PwVaultChrome.ContentFaint,
            modifier = Modifier.padding(top = 6.dp),
        )
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

/** Shared "icon in a tonal circle + label + trailing status/action" row used by both Unlock method and Data. */
@Composable
private fun SettingsIconRow(
    icon: ImageVector,
    label: String,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    trailing: @Composable () -> Unit = {},
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier =
            modifier
                .fillMaxWidth()
                .let { if (onClick != null) it.clickable(onClick = onClick) else it }
                .padding(vertical = 10.dp),
    ) {
        Surface(shape = CircleShape, color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.14f)) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.padding(8.dp).size(18.dp),
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(start = 12.dp).weight(1f),
        )
        trailing()
    }
}

@Composable
private fun UnlockMethodSection(
    unlockedState: UnlockUiState.Unlocked,
    canSetupBiometric: Boolean,
    onSetupPin: () -> Unit,
    onSetupBiometric: () -> Unit,
    onDisablePin: () -> Unit,
    onDisableBiometric: () -> Unit,
) {
    // Master password is always on, so it never counts toward the "at least one" guard below —
    // the guard is only between PIN and Biometric, the two methods that can actually be toggled.
    val pinIsOnlyMethod = unlockedState.hasPin && !unlockedState.hasBiometric
    val biometricIsOnlyMethod = unlockedState.hasBiometric && !unlockedState.hasPin

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
    ) {
        Column(modifier = Modifier.padding(horizontal = 12.dp)) {
            SettingsIconRow(
                icon = Icons.Filled.Password,
                label = stringResource(R.string.settings_app_password_label),
            ) {
                Text(
                    text = stringResource(R.string.settings_always_on),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            SettingsIconRow(icon = Icons.Filled.Dialpad, label = stringResource(R.string.settings_pin_label)) {
                Switch(
                    checked = unlockedState.hasPin,
                    enabled = !pinIsOnlyMethod,
                    onCheckedChange = { checked -> if (checked) onSetupPin() else onDisablePin() },
                )
            }
            if (pinIsOnlyMethod) {
                Text(
                    text = stringResource(R.string.settings_unlock_method_required_hint),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
            }
            if (unlockedState.hasBiometric || canSetupBiometric) {
                SettingsIconRow(
                    icon = Icons.Filled.Fingerprint,
                    label = stringResource(R.string.settings_biometric_label),
                ) {
                    Switch(
                        checked = unlockedState.hasBiometric,
                        enabled = !biometricIsOnlyMethod && !unlockedState.biometricSetupBusy,
                        onCheckedChange = { checked -> if (checked) onSetupBiometric() else onDisableBiometric() },
                    )
                }
                if (biometricIsOnlyMethod) {
                    Text(
                        text = stringResource(R.string.settings_unlock_method_required_hint),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(bottom = 8.dp),
                    )
                }
                if (unlockedState.biometricSetupError != null) {
                    Text(
                        text = unlockedState.biometricSetupError.message(),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(bottom = 8.dp),
                    )
                }
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
    Row(modifier = Modifier.horizontalScroll(rememberScrollState())) {
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
private fun LanguageSection(
    selected: AppLanguage,
    onSelect: (AppLanguage) -> Unit,
) {
    Row {
        FilterChip(
            selected = selected == AppLanguage.SYSTEM,
            onClick = { onSelect(AppLanguage.SYSTEM) },
            label = { Text(stringResource(R.string.language_system)) },
            modifier = Modifier.padding(end = 8.dp),
        )
        FilterChip(
            selected = selected == AppLanguage.EN,
            onClick = { onSelect(AppLanguage.EN) },
            label = { Text(stringResource(R.string.language_en)) },
            modifier = Modifier.padding(end = 8.dp),
        )
        FilterChip(
            selected = selected == AppLanguage.VI,
            onClick = { onSelect(AppLanguage.VI) },
            label = { Text(stringResource(R.string.language_vi)) },
        )
    }
}

@Composable
private fun DataSection(
    onManageTags: () -> Unit,
    onExport: () -> Unit,
    onImport: () -> Unit,
    hasAutoBackupFolder: Boolean,
    onPickAutoBackupFolder: () -> Unit,
    onDisableAutoBackup: () -> Unit,
) {
    Column {
        SettingsIconRow(
            icon = Icons.Filled.Sell,
            label = stringResource(R.string.manage_tags_cd),
            onClick = onManageTags,
        )
        SettingsIconRow(
            icon = Icons.Filled.IosShare,
            label = stringResource(R.string.export_cd),
            onClick = onExport,
        )
        SettingsIconRow(
            icon = Icons.Filled.FileDownload,
            label = stringResource(R.string.import_cd),
            onClick = onImport,
        )
        SettingsIconRow(
            icon = Icons.Filled.FolderOpen,
            label =
                stringResource(
                    if (hasAutoBackupFolder) R.string.auto_backup_on_cd else R.string.auto_backup_off_cd,
                ),
        ) {
            Switch(
                checked = hasAutoBackupFolder,
                onCheckedChange = { checked -> if (checked) onPickAutoBackupFolder() else onDisableAutoBackup() },
            )
        }
    }
}
