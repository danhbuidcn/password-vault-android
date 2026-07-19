package com.pwvault.app.domain

data class PasswordTemplate(
    val id: Long = 0,
    val name: String,
    val length: Int,
    val useUpper: Boolean,
    val useLower: Boolean,
    val useDigits: Boolean,
    val useSpecial: Boolean,
)
