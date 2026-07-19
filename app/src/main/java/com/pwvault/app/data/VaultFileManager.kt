package com.pwvault.app.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.zetetic.database.sqlcipher.SQLiteDatabase
import java.io.File

private const val VAULT_FILE_NAME = "vault.db"

/**
 * Creates/opens the encrypted Vault file directly via SQLCipher (no Room yet — there is no entity
 * schema until Feature 5). Feature 5 re-opens this same file through Room's SupportFactory using
 * the same derived key.
 */
class VaultFileManager(
    context: Context,
) {
    private val vaultFile: File = File(context.filesDir, VAULT_FILE_NAME)

    init {
        System.loadLibrary("sqlcipher")
    }

    fun hasVaultFile(): Boolean = vaultFile.exists()

    suspend fun createVault(key: ByteArray): Boolean = openAndValidate(key)

    suspend fun openVault(key: ByteArray): Boolean = openAndValidate(key)

    private suspend fun openAndValidate(key: ByteArray): Boolean =
        withContext(Dispatchers.IO) {
            runCatching {
                val db = SQLiteDatabase.openOrCreateDatabase(vaultFile, key, null, null, null)
                try {
                    db.rawQuery("SELECT count(*) FROM sqlite_master", null).use { it.moveToFirst() }
                } finally {
                    db.close()
                }
            }.isSuccess
        }
}
