package com.pwvault.app.importer

/** Parses RFC4180-style CSV text into rows of raw cell strings — mirrors `export/CsvExporter`'s escaping. */
class CsvImportParser {
    fun parse(text: String): List<List<String>> {
        // Windows/Excel-authored CSV commonly starts with a UTF-8 BOM — strip it so it doesn't
        // silently attach itself to the first header/field.
        val body = text.removePrefix("\uFEFF")
        val state = ParserState()
        var i = 0
        while (i < body.length) {
            i = state.consume(body, i)
        }
        state.finish()
        return state.rows
    }

    private class ParserState {
        val rows = mutableListOf<List<String>>()
        private var row = mutableListOf<String>()
        private val currentField = StringBuilder()
        private var inQuotes = false

        fun consume(
            text: String,
            index: Int,
        ): Int {
            val c = text[index]
            return when {
                inQuotes && c == '"' && text.getOrNull(index + 1) == '"' -> {
                    currentField.append('"')
                    index + 2
                }
                c == '"' -> {
                    inQuotes = !inQuotes
                    index + 1
                }
                !inQuotes && c == ',' -> {
                    endField()
                    index + 1
                }
                !inQuotes && c == '\n' -> {
                    endRow()
                    index + 1
                }
                !inQuotes && c == '\r' -> {
                    endRow()
                    if (text.getOrNull(index + 1) == '\n') index + 2 else index + 1
                }
                else -> {
                    currentField.append(c)
                    index + 1
                }
            }
        }

        fun finish() {
            if (currentField.isNotEmpty() || row.isNotEmpty()) {
                endField()
                rows.add(row)
            }
        }

        private fun endField() {
            row.add(currentField.toString())
            currentField.clear()
        }

        private fun endRow() {
            endField()
            rows.add(row)
            row = mutableListOf()
        }
    }
}
