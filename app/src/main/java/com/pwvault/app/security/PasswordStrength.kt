package com.pwvault.app.security

/** Flags a Vault Item password as weak by length + character-class diversity — `functional-spec.md §8`. */
object PasswordStrength {
    fun isWeak(password: String): Boolean {
        if (password.length < SecurityPolicy.WEAK_PASSWORD_MIN_LENGTH) return true
        val classCount =
            listOf(
                password.any { it.isUpperCase() },
                password.any { it.isLowerCase() },
                password.any { it.isDigit() },
                password.any { !it.isLetterOrDigit() },
            ).count { it }
        return classCount < SecurityPolicy.WEAK_PASSWORD_MIN_CHAR_CLASSES
    }
}
