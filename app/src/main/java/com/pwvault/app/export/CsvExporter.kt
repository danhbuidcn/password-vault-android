package com.pwvault.app.export

import com.pwvault.app.domain.VaultItem

// Column names + list encodings of the app's own CSV format — shared with `importer` so an export
// imports back with every field (see docs/plans/feature-19-simplify-unlock-csv-plan.md).
const val CSV_COLUMN_NAME = "Name"
const val CSV_COLUMN_USERNAME = "Username"
const val CSV_COLUMN_PASSWORD = "Password"
const val CSV_COLUMN_URL = "URL"
const val CSV_COLUMN_NOTE = "Note"
const val CSV_COLUMN_TAGS = "Tags"
const val CSV_COLUMN_CUSTOM_FIELDS = "CustomFields"
const val CSV_COLUMN_TYPE = "Type"

/** Separates tag names inside the Tags cell. */
const val CSV_TAG_SEPARATOR = ";"

/** One custom field per line inside the CustomFields cell, as `label:value` (split at the first `:`). */
const val CSV_CUSTOM_FIELD_SEPARATOR = "\n"
const val CSV_CUSTOM_FIELD_LABEL_SEPARATOR = ":"

private val CSV_HEADER =
    listOf(
        CSV_COLUMN_NAME,
        CSV_COLUMN_USERNAME,
        CSV_COLUMN_PASSWORD,
        CSV_COLUMN_URL,
        CSV_COLUMN_NOTE,
        CSV_COLUMN_TAGS,
        CSV_COLUMN_CUSTOM_FIELDS,
        CSV_COLUMN_TYPE,
    )
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
                    item.tags.joinToString(CSV_TAG_SEPARATOR) { it.name },
                    item.customFields.joinToString(CSV_CUSTOM_FIELD_SEPARATOR) {
                        "${it.label}$CSV_CUSTOM_FIELD_LABEL_SEPARATOR${it.value}"
                    },
                    item.type.name,
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
