package com.pwvault.app.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.pwvault.app.domain.VaultItem
import com.pwvault.app.domain.VaultItemType

@Entity(
    tableName = "vault_items",
    foreignKeys = [
        ForeignKey(
            entity = TagEntity::class,
            parentColumns = ["id"],
            childColumns = ["tagId"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
    indices = [Index("tagId")],
)
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
    val tagId: Long? = null,
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
        tagId = tag?.id,
    )
