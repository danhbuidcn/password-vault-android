package com.pwvault.app.importer

import com.pwvault.app.domain.CustomField
import com.pwvault.app.domain.VaultItemType

/** A source row after column mapping, ready to become a Vault Item. */
data class ImportedRow(
    val name: String,
    val username: String,
    val password: String,
    val url: String,
    val note: String,
    val type: VaultItemType = VaultItemType.LOGIN,
    val tagNames: List<String> = emptyList(),
    val customFields: List<CustomField> = emptyList(),
)
