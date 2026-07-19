package com.pwvault.app.security

import android.security.keystore.KeyPermanentlyInvalidatedException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec

private const val GCM_TAG_LENGTH_BITS = 128
private const val AES_GCM_TRANSFORMATION = "AES/GCM/NoPadding"

/**
 * Orchestrates biometric setup/unlock. The crypto operation is split in two: a [Cipher] is
 * prepared here, then authorized by [androidx.biometric.BiometricPrompt] in the UI layer (which
 * needs the hosting Activity), then handed back here to finish the encrypt/decrypt — see
 * docs/plans/feature-03-biometric-unlock-plan.md.
 */
class BiometricUnlockManager(
    private val keystoreKeyProvider: BiometricKeystoreKeyProvider,
    private val credentialStore: BiometricCredentialStore,
) {
    fun hasBiometric(): Boolean = credentialStore.hasBiometric()

    /** Returns `null` if the Keystore key was invalidated (e.g. a new fingerprint was enrolled). */
    suspend fun prepareSetupCipher(): Cipher? =
        withContext(Dispatchers.Default) {
            runOrInvalidate {
                Cipher.getInstance(AES_GCM_TRANSFORMATION).apply {
                    init(Cipher.ENCRYPT_MODE, keystoreKeyProvider.getOrCreateKey())
                }
            }
        }

    suspend fun completeSetup(
        cipher: Cipher,
        vaultKey: ByteArray,
    ) {
        withContext(Dispatchers.Default) {
            val encryptedVaultKey = cipher.doFinal(vaultKey)
            credentialStore.save(encryptedVaultKey, cipher.iv)
        }
    }

    /** Returns `null` if biometric unlock isn't set up, or if the Keystore key was invalidated. */
    suspend fun prepareUnlockCipher(): Cipher? =
        withContext(Dispatchers.Default) {
            val credentials = credentialStore.load() ?: return@withContext null
            runOrInvalidate {
                Cipher.getInstance(AES_GCM_TRANSFORMATION).apply {
                    val spec = GCMParameterSpec(GCM_TAG_LENGTH_BITS, credentials.iv)
                    init(Cipher.DECRYPT_MODE, keystoreKeyProvider.getOrCreateKey(), spec)
                }
            }
        }

    suspend fun completeUnlock(cipher: Cipher): ByteArray? =
        withContext(Dispatchers.Default) {
            val credentials = credentialStore.load() ?: return@withContext null
            runCatching { cipher.doFinal(credentials.encryptedVaultKey) }.getOrNull()
        }

    /**
     * Re-enrolling a fingerprint/face permanently invalidates the Keystore key (by design). When
     * that happens the wrapped Vault key is unrecoverable — delete both so `hasBiometric()` goes
     * back to `false` and the user can set biometric unlock up again from scratch.
     */
    private inline fun runOrInvalidate(block: () -> Cipher): Cipher? =
        try {
            block()
        } catch (expected: KeyPermanentlyInvalidatedException) {
            keystoreKeyProvider.deleteKey()
            credentialStore.clear()
            null
        }
}
