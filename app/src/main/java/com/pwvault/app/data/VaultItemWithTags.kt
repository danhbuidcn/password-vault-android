package com.pwvault.app.data

import androidx.room.Embedded
import androidx.room.Junction
import androidx.room.Relation
import com.pwvault.app.domain.VaultItem

data class VaultItemWithTags(
    @Embedded
    val item: VaultItemEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "id",
        associateBy = Junction(VaultItemTagCrossRef::class, parentColumn = "vaultItemId", entityColumn = "tagId"),
    )
    val tags: List<TagEntity>,
)

fun VaultItemWithTags.toDomain(): VaultItem = item.toDomain().copy(tags = tags.map { it.toDomain() })
