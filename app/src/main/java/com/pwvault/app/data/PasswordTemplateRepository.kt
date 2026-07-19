package com.pwvault.app.data

import com.pwvault.app.domain.PasswordTemplate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class PasswordTemplateRepository(
    private val vaultFileManager: VaultFileManager,
) {
    private val dao get() = vaultFileManager.database().passwordTemplateDao()

    fun observeTemplates(): Flow<List<PasswordTemplate>> = dao.observeAll().map { list -> list.map { it.toDomain() } }

    suspend fun addTemplate(template: PasswordTemplate): Long =
        dao.insert(
            PasswordTemplateEntity(
                name = template.name,
                length = template.length,
                useUpper = template.useUpper,
                useLower = template.useLower,
                useDigits = template.useDigits,
                useSpecial = template.useSpecial,
            ),
        )

    suspend fun deleteTemplate(template: PasswordTemplate) =
        dao.delete(
            PasswordTemplateEntity(
                id = template.id,
                name = template.name,
                length = template.length,
                useUpper = template.useUpper,
                useLower = template.useLower,
                useDigits = template.useDigits,
                useSpecial = template.useSpecial,
            ),
        )
}
