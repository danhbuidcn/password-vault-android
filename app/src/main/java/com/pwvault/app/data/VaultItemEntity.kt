package com.pwvault.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.pwvault.app.domain.VaultItem

@Entity(tableName = "vault_items")
data class VaultItemEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val username: String,
    val password: String,
    val url: String,
    val note: String,
    val createdAt: Long,
    val updatedAt: Long,
)

fun VaultItemEntity.toDomain(): VaultItem =
    VaultItem(
        id = id,
        name = name,
        username = username,
        password = password,
        url = url,
        note = note,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )

fun VaultItem.toEntity(): VaultItemEntity =
    VaultItemEntity(
        id = id,
        name = name,
        username = username,
        password = password,
        url = url,
        note = note,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )
