package com.pwvault.app.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.pwvault.app.domain.PasswordTemplate

@Entity(tableName = "password_templates", indices = [Index(value = ["name"], unique = true)])
data class PasswordTemplateEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    // NOCASE so the unique index itself rejects case-variant duplicates, same reasoning as TagEntity.name.
    @ColumnInfo(collate = ColumnInfo.NOCASE)
    val name: String,
    val length: Int,
    val useUpper: Boolean,
    val useLower: Boolean,
    val useDigits: Boolean,
    val useSpecial: Boolean,
)

fun PasswordTemplateEntity.toDomain(): PasswordTemplate =
    PasswordTemplate(
        id = id,
        name = name,
        length = length,
        useUpper = useUpper,
        useLower = useLower,
        useDigits = useDigits,
        useSpecial = useSpecial,
    )
