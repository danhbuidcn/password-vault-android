package com.pwvault.app.data

import com.pwvault.app.domain.VaultItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class VaultItemRepository(
    private val vaultFileManager: VaultFileManager,
) {
    private val dao get() = vaultFileManager.database().vaultItemDao()

    fun observeItems(): Flow<List<VaultItem>> = dao.observeAll().map { list -> list.map { it.toDomain() } }

    suspend fun getItem(id: Long): VaultItem? = dao.getById(id)?.toDomain()

    suspend fun addItem(item: VaultItem): Long = dao.insert(item.toEntity())

    suspend fun updateItem(item: VaultItem) = dao.update(item.toEntity())

    suspend fun deleteItem(item: VaultItem) = dao.delete(item.toEntity())
}
