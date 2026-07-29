package com.pwvault.app.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface VaultItemDao {
    @Transaction
    @Query("SELECT * FROM vault_items ORDER BY name COLLATE NOCASE ASC")
    fun observeAllWithDetails(): Flow<List<VaultItemWithDetails>>

    @Transaction
    @Query("SELECT * FROM vault_items WHERE id = :id")
    suspend fun getByIdWithDetails(id: Long): VaultItemWithDetails?

    /** Cheap one-shot query used to force Room to open the connection immediately (see [VaultFileManager]). */
    @Query("SELECT count(*) FROM vault_items")
    suspend fun count(): Int

    @Insert
    suspend fun insert(item: VaultItemEntity): Long

    @Update
    suspend fun update(item: VaultItemEntity)

    @Delete
    suspend fun delete(item: VaultItemEntity)

    @Query("DELETE FROM custom_fields WHERE vaultItemId = :itemId")
    suspend fun clearCustomFields(itemId: Long)

    @Insert
    suspend fun insertCustomFields(fields: List<CustomFieldEntity>)

    @Transaction
    suspend fun setCustomFieldsForItem(
        itemId: Long,
        fields: List<CustomFieldEntity>,
    ) {
        clearCustomFields(itemId)
        insertCustomFields(fields)
    }
}
