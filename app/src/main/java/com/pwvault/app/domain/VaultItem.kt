package com.pwvault.app.domain

data class VaultItem(
    val id: Long,
    val name: String,
    val username: String,
    val password: String,
    val url: String,
    val note: String,
    val createdAt: Long,
    val updatedAt: Long,
)
