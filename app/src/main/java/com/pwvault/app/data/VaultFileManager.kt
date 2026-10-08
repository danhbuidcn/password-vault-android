package com.pwvault.app.data

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory
import java.io.File

private const val VAULT_FILE_NAME = "vault.db"

/** Suggested tags seeded once when a vault is first created — see `glossary.md` (Tag). */
private val DEFAULT_TAG_NAMES = listOf("Personal", "Bank", "Social Media")

/**
 * Opens the encrypted Vault file through Room's SupportOpenHelperFactory (SQLCipher-backed), using the
 * Vault key (random, gated by the PIN — see `UnlockViewModel`). Keeps the opened [VaultDatabase] alive for the
 * duration of the `Unlocked` session — callers must [close] it as soon as the app locks again,
 * since the connection holds the decrypted key in memory.
 */
class VaultFileManager(
    private val context: Context,
) {
    private val vaultFile: File = File(context.filesDir, VAULT_FILE_NAME)

    @Volatile
    private var database: VaultDatabase? = null

    init {
        System.loadLibrary("sqlcipher")
    }

    fun hasVaultFile(): Boolean = vaultFile.exists()

    suspend fun createVault(key: ByteArray): Boolean = openAndValidate(key, vaultFile)

    suspend fun openVault(key: ByteArray): Boolean = openAndValidate(key, vaultFile)

    /** Only valid while a vault is open (i.e. between a successful [createVault]/[openVault] and [close]). */
    fun database(): VaultDatabase = database ?: error("Vault database is not open")

    fun close() {
        database?.close()
        database = null
    }

    private suspend fun openAndValidate(
        key: ByteArray,
        path: File,
    ): Boolean =
        withContext(Dispatchers.IO) {
            val db =
                Room
                    .databaseBuilder(context, VaultDatabase::class.java, path.absolutePath)
                    .openHelperFactory(SupportOpenHelperFactory(key))
                    // No destructive fallback: v0.1.0 (DB version 5) is installed on a real device with
                    // real Vault data (see CHANGELOG/roadmap). A destructive fallback here would silently
                    // wipe that vault the next time the schema version changes. Every version bump from 5
                    // onward must ship an explicit Room Migration that preserves existing rows — a missing
                    // Migration should surface as a loud crash (fixable), never a silent data loss.
                    .addMigrations(MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8)
                    .addCallback(
                        object : RoomDatabase.Callback() {
                            override fun onCreate(db: SupportSQLiteDatabase) {
                                super.onCreate(db)
                                DEFAULT_TAG_NAMES.forEach { name ->
                                    db.execSQL("INSERT INTO tags (name) VALUES (?)", arrayOf(name))
                                }
                            }
                        },
                    ).build()
            runCatching {
                // Room opens the connection lazily — force it now so a wrong key fails here.
                db.vaultItemDao().count()
            }.fold(
                onSuccess = {
                    database = db
                    true
                },
                onFailure = {
                    db.close()
                    false
                },
            )
        }
}
