package com.pwvault.app.data

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        VaultItemEntity::class,
        TagEntity::class,
        CustomFieldEntity::class,
    ],
    version = 7,
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

/**
 * Vault items now carry a single tag instead of many. Every real vault seen so far only ever
 * assigns one tag per item, so this keeps the first (arbitrary if duplicates exist) cross-ref row
 * per item and drops the rest, then removes the now-unused many-to-many table.
 */
val MIGRATION_6_7 =
    object : Migration(6, 7) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE vault_items ADD COLUMN tagId INTEGER REFERENCES tags(id) ON DELETE SET NULL")
            db.execSQL(
                """
                UPDATE vault_items SET tagId = (
                    SELECT tagId FROM vault_item_tag_cross_ref WHERE vaultItemId = vault_items.id LIMIT 1
                )
                """.trimIndent(),
            )
            db.execSQL("CREATE INDEX IF NOT EXISTS index_vault_items_tagId ON vault_items(tagId)")
            db.execSQL("DROP TABLE IF EXISTS vault_item_tag_cross_ref")
        }
    }
