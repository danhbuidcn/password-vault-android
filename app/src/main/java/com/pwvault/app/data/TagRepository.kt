package com.pwvault.app.data

import com.pwvault.app.domain.Tag
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class TagRepository(
    private val vaultFileManager: VaultFileManager,
) {
    private val dao get() = vaultFileManager.database().tagDao()

    fun observeTags(): Flow<List<Tag>> = dao.observeAll().map { list -> list.map { it.toDomain() } }

    suspend fun addTag(name: String): Long = dao.insert(TagEntity(name = name))

    suspend fun renameTag(
        tag: Tag,
        newName: String,
    ) = dao.update(TagEntity(id = tag.id, name = newName))

    suspend fun deleteTag(tag: Tag) = dao.delete(TagEntity(id = tag.id, name = tag.name))
}
