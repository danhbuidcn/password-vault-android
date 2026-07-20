package com.pwvault.app.importer

import org.dhatim.fastexcel.reader.ReadableWorkbook
import java.io.InputStream

/** Reads the first sheet of an `.xlsx` file into rows of raw cell strings via `fastexcel-reader`. */
class XlsxImportParser {
    fun parse(input: InputStream): List<List<String>> =
        ReadableWorkbook(input).use { workbook ->
            workbook.firstSheet.read().map { row ->
                (0 until row.cellCount).map { row.getCellText(it) }
            }
        }
}
