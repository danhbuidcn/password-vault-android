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
    version = 8,
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

/**
 * Vault items now carry up to [com.pwvault.app.domain.MAX_TAGS_PER_VAULT_ITEM] tags instead of one —
 * restores the many-to-many cross-ref table dropped by MIGRATION_6_7, backfilling it from the single
 * `tagId` column before dropping that column. SQLite (even the SQLCipher-bundled version) has no
 * reliable cross-version DROP COLUMN, so `vault_items` is rebuilt via the standard SQLite
 * create-copy-drop-rename sequence rather than an in-place ALTER TABLE.
 */
val MIGRATION_7_8 =
    object : Migration(7, 8) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS vault_item_tag_cross_ref (" +
                    "vaultItemId INTEGER NOT NULL, tagId INTEGER NOT NULL, " +
                    "PRIMARY KEY(vaultItemId, tagId), " +
                    "FOREIGN KEY(vaultItemId) REFERENCES vault_items(id) ON DELETE CASCADE, " +
                    "FOREIGN KEY(tagId) REFERENCES tags(id) ON DELETE CASCADE)",
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS index_vault_item_tag_cross_ref_tagId ON vault_item_tag_cross_ref(tagId)",
            )
            db.execSQL(
                """
                INSERT INTO vault_item_tag_cross_ref (vaultItemId, tagId)
                SELECT id, tagId FROM vault_items WHERE tagId IS NOT NULL
                """.trimIndent(),
            )

            db.execSQL(
                "CREATE TABLE vault_items_new (" +
                    "id INTEGER NOT NULL, type TEXT NOT NULL, name TEXT NOT NULL, " +
                    "username TEXT NOT NULL, password TEXT NOT NULL, url TEXT NOT NULL, " +
                    "note TEXT NOT NULL, createdAt INTEGER NOT NULL, updatedAt INTEGER NOT NULL, " +
                    "PRIMARY KEY(id))",
            )
            db.execSQL(
                """
                INSERT INTO vault_items_new (id, type, name, username, password, url, note, createdAt, updatedAt)
                SELECT id, type, name, username, password, url, note, createdAt, updatedAt FROM vault_items
                """.trimIndent(),
            )
            db.execSQL("DROP TABLE vault_items")
            db.execSQL("ALTER TABLE vault_items_new RENAME TO vault_items")
        }
    }
