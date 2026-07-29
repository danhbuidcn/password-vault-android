package com.pwvault.app.export

import com.pwvault.app.domain.VaultItem

private val CSV_HEADER = listOf("Name", "Username", "Password", "URL", "Note", "Tags", "CustomFields")
private const val CSV_SPECIAL_CHARS = ",\"\n\r"

/** Serializes Vault Items to a plaintext CSV — RFC4180-style quoting, one row per item. */
class CsvExporter {
    fun toCsv(items: List<VaultItem>): String {
        val rows =
            items.map { item ->
                listOf(
                    item.name,
                    item.username,
                    item.password,
                    item.url,
                    item.note,
                    item.tag?.name.orEmpty(),
                    item.customFields.joinToString(";") { "${it.label}:${it.value}" },
                )
            }
        return (listOf(CSV_HEADER) + rows).joinToString("\r\n") { row -> row.joinToString(",") { it.toCsvField() } }
    }

    private fun String.toCsvField(): String =
        if (any { it in CSV_SPECIAL_CHARS }) {
            "\"${replace("\"", "\"\"")}\""
        } else {
            this
        }
}
