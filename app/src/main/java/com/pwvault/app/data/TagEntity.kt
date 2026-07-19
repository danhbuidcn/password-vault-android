package com.pwvault.app.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.pwvault.app.domain.Tag

@Entity(tableName = "tags", indices = [Index(value = ["name"], unique = true)])
data class TagEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    // NOCASE so the unique index itself rejects case-variant duplicates (e.g. "Bank" vs "bank"),
    // not just the in-memory check in TagViewModel — that check alone has a narrow startup race.
    @ColumnInfo(collate = ColumnInfo.NOCASE)
    val name: String,
)

fun TagEntity.toDomain(): Tag = Tag(id = id, name = name)
