package com.pwvault.app.data

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        VaultItemEntity::class,
        TagEntity::class,
        VaultItemTagCrossRef::class,
        CustomFieldEntity::class,
        PasswordTemplateEntity::class,
    ],
    version = 5,
)
abstract class VaultDatabase : RoomDatabase() {
    abstract fun vaultItemDao(): VaultItemDao

    abstract fun tagDao(): TagDao

    abstract fun passwordTemplateDao(): PasswordTemplateDao
}
