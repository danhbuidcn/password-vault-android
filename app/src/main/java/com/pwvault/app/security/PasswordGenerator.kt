package com.pwvault.app.security

import java.security.SecureRandom

/** Suggests a strong random password for the Vault Item form's "Generate" action. */
object PasswordGenerator {
    private const val CHARSET = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%^&*-_=+"
    private val random = SecureRandom()

    fun generate(): String =
        buildString {
            repeat(SecurityPolicy.GENERATED_PASSWORD_LENGTH) {
                append(CHARSET[random.nextInt(CHARSET.length)])
            }
        }
}
