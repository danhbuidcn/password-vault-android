package com.pwvault.app.security

import android.content.Context
import android.util.Base64
import java.security.SecureRandom

private const val PREFS_NAME = "vault_metadata"
private const val KEY_SALT = "salt"
private const val SALT_LENGTH_BYTES = 16

/**
 * Persists the (non-secret) KDF salt for the Vault. The salt alone cannot derive the Vault key
 * without the Master Password, so plain SharedPreferences is an acceptable store for it.
 */
class VaultMetadataStore(
    context: Context,
) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getOrCreateSalt(): ByteArray {
        val existing = prefs.getString(KEY_SALT, null)
        if (existing != null) {
            return Base64.decode(existing, Base64.NO_WRAP)
        }
        val salt = ByteArray(SALT_LENGTH_BYTES)
        SecureRandom().nextBytes(salt)
        prefs.edit().putString(KEY_SALT, Base64.encodeToString(salt, Base64.NO_WRAP)).apply()
        return salt
    }

    /** Overwrites the stored salt — used after restoring a `.pwvbackup` so later unlocks derive against it. */
    fun setSalt(salt: ByteArray) {
        prefs.edit().putString(KEY_SALT, Base64.encodeToString(salt, Base64.NO_WRAP)).apply()
    }
}
