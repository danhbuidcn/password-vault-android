package com.pwvault.app.security

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyStore
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey

private const val ANDROID_KEYSTORE = "AndroidKeyStore"
private const val KEY_ALIAS = "pwvault_pin_wrap_key"

/**
 * Provides the hardware-backed (non-exportable) AES-GCM key used to wrap the Vault key for PIN
 * unlock. The PIN itself never derives this key — see docs/plans/feature-02-pin-unlock-plan.md.
 */
class PinKeystoreKeyProvider {
    fun getOrCreateKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        (keyStore.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }

        val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        val spec =
            KeyGenParameterSpec
                .Builder(KEY_ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setUserAuthenticationRequired(false)
                .build()
        keyGenerator.init(spec)
        return keyGenerator.generateKey()
    }
}
