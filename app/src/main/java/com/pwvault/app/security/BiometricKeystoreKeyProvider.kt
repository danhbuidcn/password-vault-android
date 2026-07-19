package com.pwvault.app.security

import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyStore
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey

private const val ANDROID_KEYSTORE = "AndroidKeyStore"
private const val KEY_ALIAS = "pwvault_biometric_wrap_key"

/**
 * Provides the hardware-backed AES-GCM key used to wrap the Vault key for biometric unlock.
 * Unlike [PinKeystoreKeyProvider]'s key, this one requires a fresh biometric authentication for
 * every use (no validity duration) — Keystore itself gates access, not app-level comparison. See
 * docs/plans/feature-03-biometric-unlock-plan.md.
 */
class BiometricKeystoreKeyProvider {
    fun getOrCreateKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        (keyStore.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }

        val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        val builder =
            KeyGenParameterSpec
                .Builder(KEY_ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setUserAuthenticationRequired(true)
                .setInvalidatedByBiometricEnrollment(true)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            builder.setUserAuthenticationParameters(0, KeyProperties.AUTH_BIOMETRIC_STRONG)
        } else {
            @Suppress("DEPRECATION")
            builder.setUserAuthenticationValidityDurationSeconds(-1)
        }
        keyGenerator.init(builder.build())
        return keyGenerator.generateKey()
    }

    /** Removes the Keystore entry, e.g. after it was permanently invalidated by re-enrollment. */
    fun deleteKey() {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        keyStore.deleteEntry(KEY_ALIAS)
    }
}
