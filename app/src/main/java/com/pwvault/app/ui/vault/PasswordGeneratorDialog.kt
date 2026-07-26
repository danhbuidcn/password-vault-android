package com.pwvault.app.ui.vault

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.pwvault.app.R
import com.pwvault.app.security.PasswordGenerator
import com.pwvault.app.security.SecurityPolicy
import kotlin.math.roundToInt

private const val MIN_LENGTH = 4
private const val MAX_LENGTH = 15

@Composable
fun PasswordGeneratorDialog(
    onUsePassword: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var length by remember { mutableStateOf(SecurityPolicy.GENERATED_PASSWORD_LENGTH.coerceIn(MIN_LENGTH, MAX_LENGTH)) }
    var useUpper by remember { mutableStateOf(true) }
    var useLower by remember { mutableStateOf(true) }
    var useDigits by remember { mutableStateOf(true) }
    var useSpecial by remember { mutableStateOf(true) }
    var preview by remember { mutableStateOf("") }

    val selectedCount = listOf(useUpper, useLower, useDigits, useSpecial).count { it }
    val isValid = selectedCount > 0 && length >= selectedCount

    // Re-derives validity from the live state instead of closing over the `isValid` above: callers
    // (checkbox toggles) mutate state and call this in the same click handler, before Compose
    // recomposes — a stale `isValid` here would let a 0-classes-selected call through to
    // PasswordGenerator.generate(), which throws IllegalArgumentException and crashes the dialog.
    fun regenerate() {
        val currentSelectedCount = listOf(useUpper, useLower, useDigits, useSpecial).count { it }
        if (currentSelectedCount > 0 && length >= currentSelectedCount) {
            preview = PasswordGenerator.generate(length, useUpper, useLower, useDigits, useSpecial)
        }
    }
    LaunchedEffect(Unit) { regenerate() }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.password_generator_title)) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Text(
                    text = preview,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
                Text(
                    text = stringResource(R.string.password_generator_length_label, length),
                    style = MaterialTheme.typography.labelLarge,
                )
                Slider(
                    value = length.toFloat(),
                    onValueChange = { length = it.roundToInt() },
                    onValueChangeFinished = { regenerate() },
                    valueRange = MIN_LENGTH.toFloat()..MAX_LENGTH.toFloat(),
                    steps = MAX_LENGTH - MIN_LENGTH - 1,
                )
                CharClassCheckbox(useUpper, stringResource(R.string.password_generator_use_upper)) {
                    useUpper = it
                    regenerate()
                }
                CharClassCheckbox(useLower, stringResource(R.string.password_generator_use_lower)) {
                    useLower = it
                    regenerate()
                }
                CharClassCheckbox(useDigits, stringResource(R.string.password_generator_use_digits)) {
                    useDigits = it
                    regenerate()
                }
                CharClassCheckbox(useSpecial, stringResource(R.string.password_generator_use_special)) {
                    useSpecial = it
                    regenerate()
                }
                if (!isValid) {
                    Text(
                        text = stringResource(R.string.error_password_generator_no_class_selected),
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
                TextButton(onClick = { regenerate() }, enabled = isValid) {
                    Text(stringResource(R.string.password_generator_regenerate_button))
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onUsePassword(preview) }, enabled = isValid && preview.isNotEmpty()) {
                Text(stringResource(R.string.password_generator_use_button))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.vault_cancel_button))
            }
        },
    )
}

@Composable
private fun CharClassCheckbox(
    checked: Boolean,
    label: String,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Checkbox(checked = checked, onCheckedChange = onCheckedChange)
        Text(text = label)
    }
}
