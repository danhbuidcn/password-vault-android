package com.pwvault.app.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.pwvault.app.domain.CustomField

@Entity(
    tableName = "custom_fields",
    foreignKeys = [
        ForeignKey(
            entity = VaultItemEntity::class,
            parentColumns = ["id"],
            childColumns = ["vaultItemId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("vaultItemId")],
)
data class CustomFieldEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val vaultItemId: Long,
    val label: String,
    val value: String,
)

fun CustomFieldEntity.toDomain(): CustomField = CustomField(id = id, label = label, value = value)
