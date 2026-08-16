package com.pwvault.app.data

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.pwvault.app.security.VaultMetadataStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory
import java.io.DataInputStream
import java.io.File
import java.io.InputStream
import java.io.OutputStream

private const val VAULT_FILE_NAME = "vault.db"
private const val RESTORE_TEMP_FILE_NAME = "vault.db.restore-tmp"
private val BACKUP_MAGIC = byteArrayOf('P'.code.toByte(), 'W'.code.toByte(), 'V'.code.toByte(), 'B'.code.toByte())
private const val BACKUP_FORMAT_VERSION = 1

/** Suggested tags seeded once when a vault is first created — see `glossary.md` (Tag). */
private val DEFAULT_TAG_NAMES = listOf("Personal", "Bank", "Social Media")

/**
 * Opens the encrypted Vault file through Room's SupportOpenHelperFactory (SQLCipher-backed), using the
 * Vault key derived from the Master Password. Keeps the opened [VaultDatabase] alive for the
 * duration of the `Unlocked` session — callers must [close] it as soon as the app locks again,
 * since the connection holds the decrypted key in memory.
 */
class VaultFileManager(
    private val context: Context,
    private val metadataStore: VaultMetadataStore,
) {
    private val vaultFile: File = File(context.filesDir, VAULT_FILE_NAME)
    private val restoreTempFile: File = File(context.filesDir, RESTORE_TEMP_FILE_NAME)

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

    /**
     * Streams a consistent snapshot of the encrypted Vault file to [output] (e.g. a SAF `Uri` the
     * user picked for a `.pwvbackup` export), prefixed with a small header (magic + format version +
     * KDF salt — see [stageRestoreCandidate]). The salt lives outside `vault.db` (in
     * [VaultMetadataStore]'s SharedPreferences), so without it a restore on another install could
     * never re-derive the same Vault key from the correct Master Password. Checkpoints WAL first so
     * any data only sitting in the `-wal` sidecar file is merged into `vault.db` before the copy —
     * otherwise a recently saved item could be silently missing from the exported backup.
     */
    suspend fun copyVaultFileTo(output: OutputStream) =
        withContext(Dispatchers.IO) {
            database().query("PRAGMA wal_checkpoint(FULL)", null).use { it.moveToFirst() }
            val salt = metadataStore.getOrCreateSalt()
            output.write(BACKUP_MAGIC)
            output.write(BACKUP_FORMAT_VERSION)
            output.write(salt.size)
            output.write(salt)
            vaultFile.inputStream().use { input -> input.copyTo(output) }
        }

    /**
     * Parses a picked `.pwvbackup`'s header and stages its payload at [restoreTempFile], without
     * touching the real `vault.db`. Returns the embedded KDF salt, or `null` if [input] isn't a
     * recognized backup (wrong/missing magic, unsupported format version, truncated header) — e.g. a
     * backup exported before this header existed, or an unrelated file.
     */
    suspend fun stageRestoreCandidate(input: InputStream): ByteArray? =
        withContext(Dispatchers.IO) {
            runCatching {
                val dataInput = DataInputStream(input)
                val magic = ByteArray(BACKUP_MAGIC.size)
                dataInput.readFully(magic)
                if (!magic.contentEquals(BACKUP_MAGIC)) return@runCatching null
                if (dataInput.readUnsignedByte() != BACKUP_FORMAT_VERSION) return@runCatching null
                val saltLength = dataInput.readUnsignedByte()
                if (saltLength <= 0) return@runCatching null
                val salt = ByteArray(saltLength)
                dataInput.readFully(salt)
                restoreTempFile.outputStream().use { out -> dataInput.copyTo(out) }
                salt
            }.getOrElse {
                restoreTempFile.delete()
                null
            }
        }

    /**
     * Validates the file staged by [stageRestoreCandidate] opens with [key] (i.e. the entered Master
     * Password matched the backup's salt). On success, atomically replaces `vault.db` with the staged
     * file and opens it for real. On failure (wrong password), the staged file is deliberately left in
     * place — the user must be able to retry with a different password against the same picked file,
     * without re-picking it — and any existing `vault.db` is left untouched either way. Call
     * [discardRestoreCandidate] once the restore flow is abandoned (cancelled, or a new file picked).
     */
    suspend fun tryActivateRestoreCandidate(key: ByteArray): Boolean =
        withContext(Dispatchers.IO) {
            if (!restoreTempFile.exists()) return@withContext false
            sidecarFilesOf(restoreTempFile).forEach { it.delete() }
            val candidateValid = openAndValidate(key, restoreTempFile, keepOpen = false)
            sidecarFilesOf(restoreTempFile).forEach { it.delete() }
            if (!candidateValid) return@withContext false
            sidecarFilesOf(vaultFile).forEach { it.delete() }
            vaultFile.delete()
            if (!restoreTempFile.renameTo(vaultFile)) {
                restoreTempFile.delete()
                return@withContext false
            }
            openAndValidate(key, vaultFile)
        }

    /** Discards the file staged by [stageRestoreCandidate] — call when the restore flow is abandoned. */
    suspend fun discardRestoreCandidate() =
        withContext(Dispatchers.IO) {
            sidecarFilesOf(restoreTempFile).forEach { it.delete() }
            restoreTempFile.delete()
        }

    private fun sidecarFilesOf(file: File): List<File> =
        listOf(File(file.parentFile, "${file.name}-wal"), File(file.parentFile, "${file.name}-shm"))

    private suspend fun openAndValidate(
        key: ByteArray,
        path: File,
        keepOpen: Boolean = true,
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
                    if (keepOpen) database = db else db.close()
                    true
                },
                onFailure = {
                    db.close()
                    false
                },
            )
        }
}
