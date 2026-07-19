package com.pwvault.app.data

import androidx.room.Embedded
import androidx.room.Junction
import androidx.room.Relation
import com.pwvault.app.domain.VaultItem

data class VaultItemWithDetails(
    @Embedded
    val item: VaultItemEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "id",
        associateBy = Junction(VaultItemTagCrossRef::class, parentColumn = "vaultItemId", entityColumn = "tagId"),
    )
    val tags: List<TagEntity>,
    @Relation(
        parentColumn = "id",
        entityColumn = "vaultItemId",
    )
    val customFields: List<CustomFieldEntity>,
)

fun VaultItemWithDetails.toDomain(): VaultItem =
    item.toDomain().copy(
        tags = tags.map { it.toDomain() },
        customFields = customFields.map { it.toDomain() },
    )
