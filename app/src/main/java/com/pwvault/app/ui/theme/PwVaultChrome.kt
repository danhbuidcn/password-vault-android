package com.pwvault.app.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Brand chrome (top app bar / Settings banner) — deliberately the same color in both light and
 * dark theme, so the app keeps one visual anchor regardless of the user's chosen appearance.
 */
object PwVaultChrome {
    val Background = Color(0xFF1C2230)
    val Content = Color(0xFFF3F4F7)
    val ContentMuted = Color(0xFF9AA3B2)
    val ContentFaint = Color(0xFF5B6472)
    val Accent = Color(0xFFD9A54B)
}
