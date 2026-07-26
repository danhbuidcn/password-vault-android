package com.pwvault.app.data

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        VaultItemEntity::class,
        TagEntity::class,
        VaultItemTagCrossRef::class,
        CustomFieldEntity::class,
    ],
    version = 6,
)
abstract class VaultDatabase : RoomDatabase() {
    abstract fun vaultItemDao(): VaultItemDao

    abstract fun tagDao(): TagDao
}

/** Drops the reusable-password-template feature's table — removed feature, no replacement column/table. */
val MIGRATION_5_6 =
    object : Migration(5, 6) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("DROP TABLE IF EXISTS password_templates")
        }
    }
