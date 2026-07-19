package com.pwvault.app.domain

enum class VaultItemType { LOGIN, NOTE }

data class VaultItem(
    val id: Long,
    val type: VaultItemType = VaultItemType.LOGIN,
    val name: String,
    val username: String,
    val password: String,
    val url: String,
    val note: String,
    val createdAt: Long,
    val updatedAt: Long,
    val tags: List<Tag> = emptyList(),
)
