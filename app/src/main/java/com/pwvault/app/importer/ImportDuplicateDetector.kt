package com.pwvault.app.importer

import com.pwvault.app.domain.VaultItem

/** Flags import rows that look like they already exist in the Vault — `functional-spec.md §6`. */
class ImportDuplicateDetector {
    fun findDuplicateRowIndexes(
        rows: List<ImportedRow>,
        existing: List<VaultItem>,
    ): Set<Int> {
        val existingKeys = existing.map { it.name.lowercase() to it.username.lowercase() }.toSet()
        return rows
            .withIndex()
            .filter { (_, row) -> (row.name.lowercase() to row.username.lowercase()) in existingKeys }
            .map { (index, _) -> index }
            .toSet()
    }
}
