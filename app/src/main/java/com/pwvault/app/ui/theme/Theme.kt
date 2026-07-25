package com.pwvault.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors =
    lightColorScheme(
        primary = Color(0xFFC6903E),
        onPrimary = Color(0xFF1C2230),
        primaryContainer = Color(0xFFC6903E),
        onPrimaryContainer = Color(0xFF1C2230),
        secondary = Color(0xFF2F8F7E),
        onSecondary = Color(0xFF1C2230),
        secondaryContainer = Color(0xFFC6903E),
        onSecondaryContainer = Color(0xFF1C2230),
        tertiary = Color(0xFF2F8F7E),
        onTertiary = Color(0xFF1C2230),
        background = Color(0xFFF3F4F7),
        onBackground = Color(0xFF1C2230),
        surface = Color(0xFFFFFFFF),
        onSurface = Color(0xFF1C2230),
        surfaceVariant = Color(0xFFECEEF2),
        onSurfaceVariant = Color(0xFF5B6472),
        error = Color(0xFFC1443A),
        onError = Color(0xFFFFFFFF),
        outline = Color(0xFFE1E4EA),
    )

private val DarkColors =
    darkColorScheme(
        primary = Color(0xFFD9A54B),
        onPrimary = Color(0xFF1C2230),
        primaryContainer = Color(0xFFD9A54B),
        onPrimaryContainer = Color(0xFF1C2230),
        secondary = Color(0xFF4CA89B),
        onSecondary = Color(0xFF1C2230),
        secondaryContainer = Color(0xFFD9A54B),
        onSecondaryContainer = Color(0xFF1C2230),
        tertiary = Color(0xFF4CA89B),
        onTertiary = Color(0xFF1C2230),
        background = Color(0xFF14171F),
        onBackground = Color(0xFFECEEF2),
        surface = Color(0xFF1D212C),
        onSurface = Color(0xFFECEEF2),
        surfaceVariant = Color(0xFF262B38),
        onSurfaceVariant = Color(0xFF9AA3B2),
        error = Color(0xFFE2685D),
        onError = Color(0xFFFFFFFF),
        outline = Color(0xFF2B303D),
    )

@Composable
fun PwVaultTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColors else LightColors
    MaterialTheme(colorScheme = colorScheme, content = content)
}
