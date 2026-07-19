package com.pwvault.app.security

import android.content.Context
import android.util.Base64

private const val PREFS_NAME = "biometric_credentials"
private const val KEY_ENCRYPTED_VAULT_KEY = "encrypted_vault_key"
private const val KEY_IV = "iv"

data class BiometricCredentials(
    val encryptedVaultKey: ByteArray,
    val iv: ByteArray,
)

/**
 * Persists the Keystore-wrapped Vault key for biometric unlock. No verifier is stored — the
 * hardware-backed Keystore key itself gates decryption (see [BiometricKeystoreKeyProvider]).
 */
class BiometricCredentialStore(
    context: Context,
) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun hasBiometric(): Boolean = prefs.contains(KEY_ENCRYPTED_VAULT_KEY)

    fun save(
        encryptedVaultKey: ByteArray,
        iv: ByteArray,
    ) {
        prefs
            .edit()
            .putString(KEY_ENCRYPTED_VAULT_KEY, Base64.encodeToString(encryptedVaultKey, Base64.NO_WRAP))
            .putString(KEY_IV, Base64.encodeToString(iv, Base64.NO_WRAP))
            .apply()
    }

    fun load(): BiometricCredentials? {
        val encryptedVaultKey = prefs.getString(KEY_ENCRYPTED_VAULT_KEY, null) ?: return null
        val iv = prefs.getString(KEY_IV, null) ?: return null
        return BiometricCredentials(
            encryptedVaultKey = Base64.decode(encryptedVaultKey, Base64.NO_WRAP),
            iv = Base64.decode(iv, Base64.NO_WRAP),
        )
    }

    fun clear() {
        prefs.edit().clear().apply()
    }
}
