package com.pwvault.app.security

import java.security.SecureRandom

/** Suggests a strong random password for the Vault Item form's "Generate" action. */
object PasswordGenerator {
    private const val UPPER = "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
    private const val LOWER = "abcdefghijklmnopqrstuvwxyz"
    private const val DIGITS = "0123456789"
    private const val SPECIAL = "!@#$%^&*-_=+"
    private val random = SecureRandom()

    /**
     * Generates a password of [length] characters from the selected character classes, guaranteeing
     * at least one character from each selected class (a flat random draw from the combined charset
     * doesn't guarantee that — see feature-16-ui-ux-redesign-plan.md §B1 for the same reasoning applied
     * to the fixed-length default generator).
     *
     * @throws IllegalArgumentException if no class is selected, or [length] is smaller than the number
     * of selected classes (can't place >=1 char per class otherwise).
     */
    fun generate(
        length: Int = SecurityPolicy.GENERATED_PASSWORD_LENGTH,
        useUpper: Boolean = true,
        useLower: Boolean = true,
        useDigits: Boolean = true,
        useSpecial: Boolean = true,
    ): String {
        val groups =
            buildList {
                if (useUpper) add(UPPER)
                if (useLower) add(LOWER)
                if (useDigits) add(DIGITS)
                if (useSpecial) add(SPECIAL)
            }
        require(groups.isNotEmpty()) { "At least one character class must be selected" }
        require(length >= groups.size) { "length must be at least ${groups.size} for the selected character classes" }

        val combined = groups.joinToString(separator = "")
        val chars = groups.map { it[random.nextInt(it.length)] }.toMutableList()
        repeat(length - groups.size) { chars.add(combined[random.nextInt(combined.length)]) }

        // Fisher-Yates with SecureRandom so the guaranteed-class characters aren't always at the front
        // (predictable) — List.shuffled() only accepts kotlin.random.Random, not java.security.SecureRandom.
        for (i in chars.indices.reversed()) {
            val j = random.nextInt(i + 1)
            val tmp = chars[i]
            chars[i] = chars[j]
            chars[j] = tmp
        }
        return chars.joinToString(separator = "")
    }
}
