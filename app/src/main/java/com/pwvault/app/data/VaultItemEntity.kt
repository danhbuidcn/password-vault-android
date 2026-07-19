package com.pwvault.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.pwvault.app.domain.VaultItem
import com.pwvault.app.domain.VaultItemType

@Entity(tableName = "vault_items")
data class VaultItemEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val type: String = VaultItemType.LOGIN.name,
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
        type = VaultItemType.valueOf(type),
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
        type = type.name,
        name = name,
        username = username,
        password = password,
        url = url,
        note = note,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )
