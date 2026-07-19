package com.pwvault.app.ui.unlock

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay

private const val TICK_MILLIS = 1000L

/** Ticks every second while [lockedUntilMillis] is in the future; `0` once expired or `null`. */
@Composable
fun rememberLockoutSecondsRemaining(lockedUntilMillis: Long?): Int {
    var secondsRemaining by remember(lockedUntilMillis) {
        mutableIntStateOf(secondsUntil(lockedUntilMillis))
    }
    LaunchedEffect(lockedUntilMillis) {
        while (secondsUntil(lockedUntilMillis) > 0) {
            delay(TICK_MILLIS)
            secondsRemaining = secondsUntil(lockedUntilMillis)
        }
    }
    return secondsRemaining
}

private fun secondsUntil(lockedUntilMillis: Long?): Int {
    if (lockedUntilMillis == null) return 0
    val remainingMillis = lockedUntilMillis - System.currentTimeMillis()
    return (remainingMillis / TICK_MILLIS).toInt().coerceAtLeast(0)
}
