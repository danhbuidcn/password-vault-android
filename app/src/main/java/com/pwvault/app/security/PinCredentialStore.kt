package com.pwvault.app.security

import android.content.Context
import android.util.Base64

private const val PREFS_NAME = "pin_credentials"
private const val KEY_PIN_SALT = "pin_salt"
private const val KEY_PIN_HASH = "pin_hash"
private const val KEY_ENCRYPTED_VAULT_KEY = "encrypted_vault_key"
private const val KEY_IV = "iv"

data class PinCredentials(
    val pinSalt: ByteArray,
    val pinHash: ByteArray,
    val encryptedVaultKey: ByteArray,
    val iv: ByteArray,
)

/** Persists the PIN verifier (Argon2id hash, not the PIN itself) and the Keystore-wrapped Vault key. */
class PinCredentialStore(
    context: Context,
) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun hasPin(): Boolean = prefs.contains(KEY_PIN_HASH)

    fun save(
        pinSalt: ByteArray,
        pinHash: ByteArray,
        encryptedVaultKey: ByteArray,
        iv: ByteArray,
    ) {
        prefs
            .edit()
            .putString(KEY_PIN_SALT, Base64.encodeToString(pinSalt, Base64.NO_WRAP))
            .putString(KEY_PIN_HASH, Base64.encodeToString(pinHash, Base64.NO_WRAP))
            .putString(KEY_ENCRYPTED_VAULT_KEY, Base64.encodeToString(encryptedVaultKey, Base64.NO_WRAP))
            .putString(KEY_IV, Base64.encodeToString(iv, Base64.NO_WRAP))
            .apply()
    }

    fun clear() {
        prefs.edit().clear().apply()
    }

    fun load(): PinCredentials? {
        val salt = prefs.getString(KEY_PIN_SALT, null) ?: return null
        val hash = prefs.getString(KEY_PIN_HASH, null) ?: return null
        val encryptedVaultKey = prefs.getString(KEY_ENCRYPTED_VAULT_KEY, null) ?: return null
        val iv = prefs.getString(KEY_IV, null) ?: return null
        return PinCredentials(
            pinSalt = Base64.decode(salt, Base64.NO_WRAP),
            pinHash = Base64.decode(hash, Base64.NO_WRAP),
            encryptedVaultKey = Base64.decode(encryptedVaultKey, Base64.NO_WRAP),
            iv = Base64.decode(iv, Base64.NO_WRAP),
        )
    }
}
