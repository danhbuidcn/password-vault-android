package com.pwvault.app.security

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Arrays
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec

private const val PIN_SALT_LENGTH_BYTES = 16
private const val GCM_TAG_LENGTH_BITS = 128
private const val AES_GCM_TRANSFORMATION = "AES/GCM/NoPadding"
private const val WIPE_CHAR = ' '

/**
 * Orchestrates PIN setup/verification. The PIN never derives the Vault key directly — it only
 * gates a Keystore-wrapped copy of it (see docs/plans/feature-02-pin-unlock-plan.md).
 */
class PinManager(
    private val keyDerivation: KeyDerivation,
    private val keystoreKeyProvider: PinKeystoreKeyProvider,
    private val credentialStore: PinCredentialStore,
) {
    fun hasPin(): Boolean = credentialStore.hasPin()

    /** Turns PIN unlock off. The Vault key stays reachable via master password / biometric. */
    fun clearPin() = credentialStore.clear()

    suspend fun setupPin(
        pin: CharArray,
        vaultKey: ByteArray,
    ) {
        val pinSalt = ByteArray(PIN_SALT_LENGTH_BYTES).also { SecureRandom().nextBytes(it) }
        val pinHash = keyDerivation.derive(pin, pinSalt)

        val (encryptedVaultKey, iv) =
            withContext(Dispatchers.Default) {
                val cipher = Cipher.getInstance(AES_GCM_TRANSFORMATION)
                cipher.init(Cipher.ENCRYPT_MODE, keystoreKeyProvider.getOrCreateKey())
                val ciphertext = cipher.doFinal(vaultKey)
                ciphertext to cipher.iv
            }

        credentialStore.save(pinSalt, pinHash, encryptedVaultKey, iv)
    }

    /** Returns the unwrapped Vault key if [pin] is correct, `null` otherwise. */
    suspend fun verifyPin(pin: CharArray): ByteArray? {
        val credentials =
            credentialStore.load() ?: run {
                Arrays.fill(pin, WIPE_CHAR)
                return null
            }
        val candidateHash = keyDerivation.derive(pin, credentials.pinSalt)

        if (!MessageDigest.isEqual(candidateHash, credentials.pinHash)) {
            return null
        }

        return withContext(Dispatchers.Default) {
            runCatching {
                val cipher = Cipher.getInstance(AES_GCM_TRANSFORMATION)
                val spec = GCMParameterSpec(GCM_TAG_LENGTH_BITS, credentials.iv)
                cipher.init(Cipher.DECRYPT_MODE, keystoreKeyProvider.getOrCreateKey(), spec)
                cipher.doFinal(credentials.encryptedVaultKey)
            }.getOrNull()
        }
    }
}
