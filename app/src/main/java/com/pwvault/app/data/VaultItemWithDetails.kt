package com.pwvault.app.data

import androidx.room.Embedded
import androidx.room.Relation
import com.pwvault.app.domain.VaultItem

data class VaultItemWithDetails(
    @Embedded
    val item: VaultItemEntity,
    @Relation(
        parentColumn = "tagId",
        entityColumn = "id",
    )
    val tag: TagEntity?,
    @Relation(
        parentColumn = "id",
        entityColumn = "vaultItemId",
    )
    val customFields: List<CustomFieldEntity>,
)

fun VaultItemWithDetails.toDomain(): VaultItem =
    item.toDomain().copy(
        tag = tag?.toDomain(),
        customFields = customFields.map { it.toDomain() },
    )
