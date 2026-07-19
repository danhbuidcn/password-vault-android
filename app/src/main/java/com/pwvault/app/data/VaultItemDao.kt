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
    fun observeAllWithTags(): Flow<List<VaultItemWithTags>>

    @Transaction
    @Query("SELECT * FROM vault_items WHERE id = :id")
    suspend fun getByIdWithTags(id: Long): VaultItemWithTags?

    /** Cheap one-shot query used to force Room to open the connection immediately (see [VaultFileManager]). */
    @Query("SELECT count(*) FROM vault_items")
    suspend fun count(): Int

    @Insert
    suspend fun insert(item: VaultItemEntity): Long

    @Update
    suspend fun update(item: VaultItemEntity)

    @Delete
    suspend fun delete(item: VaultItemEntity)

    @Query("DELETE FROM vault_item_tag_cross_ref WHERE vaultItemId = :itemId")
    suspend fun clearTags(itemId: Long)

    @Insert
    suspend fun insertTagCrossRefs(refs: List<VaultItemTagCrossRef>)

    @Transaction
    suspend fun setTagsForItem(
        itemId: Long,
        tagIds: List<Long>,
    ) {
        clearTags(itemId)
        insertTagCrossRefs(tagIds.map { VaultItemTagCrossRef(vaultItemId = itemId, tagId = it) })
    }
}
