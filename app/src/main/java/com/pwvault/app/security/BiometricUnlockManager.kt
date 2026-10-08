package com.pwvault.app.security

import android.content.Context

private const val PREFS_NAME = "biometric_unlock"
private const val KEY_ENABLED = "enabled"

/** Pre-Feature-19 store of a biometric-bound wrapped Vault key — no longer used, wiped on first run. */
private const val LEGACY_PREFS_NAME = "biometric_credentials"

/**
 * Biometric unlock is a gate only (`BIOMETRIC_WEAK`, so face unlock works on most devices): after
 * `BiometricPrompt` succeeds, the Vault key is unwrapped from the PIN's Keystore-wrapped copy via
 * [PinManager.unwrapVaultKey]. This only stores whether the user turned it on — see
 * docs/plans/feature-19-simplify-unlock-csv-plan.md.
 */
class BiometricUnlockManager(
    context: Context,
) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    init {
        context.deleteSharedPreferences(LEGACY_PREFS_NAME)
    }

    fun hasBiometric(): Boolean = prefs.getBoolean(KEY_ENABLED, false)

    fun enable() {
        prefs.edit().putBoolean(KEY_ENABLED, true).apply()
    }

    fun disable() {
        prefs.edit().putBoolean(KEY_ENABLED, false).apply()
    }
}
