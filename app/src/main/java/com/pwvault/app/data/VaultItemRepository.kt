package com.pwvault.app.data

import com.pwvault.app.domain.CustomField
import com.pwvault.app.domain.VaultItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class VaultItemRepository(
    private val vaultFileManager: VaultFileManager,
    private val autoBackupWriter: AutoBackupWriter,
) {
    private val dao get() = vaultFileManager.database().vaultItemDao()

    fun observeItems(): Flow<List<VaultItem>> = dao.observeAllWithDetails().map { list -> list.map { it.toDomain() } }

    suspend fun getItem(id: Long): VaultItem? = dao.getByIdWithDetails(id)?.toDomain()

    suspend fun addItem(item: VaultItem): Long = dao.insert(item.toEntity()).also { autoBackupWriter.scheduleBackup() }

    suspend fun updateItem(item: VaultItem) {
        dao.update(item.toEntity())
        autoBackupWriter.scheduleBackup()
    }

    suspend fun deleteItem(item: VaultItem) {
        dao.delete(item.toEntity())
        autoBackupWriter.scheduleBackup()
    }

    suspend fun setItemTags(
        itemId: Long,
        tagIds: Set<Long>,
    ) = dao.setTagsForItem(itemId, tagIds.toList())

    suspend fun setCustomFields(
        itemId: Long,
        customFields: List<CustomField>,
    ) = dao.setCustomFieldsForItem(
        itemId,
        customFields.map { CustomFieldEntity(vaultItemId = itemId, label = it.label, value = it.value) },
    )
}
