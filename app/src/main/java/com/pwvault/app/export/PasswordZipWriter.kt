package com.pwvault.app.export

import net.lingala.zip4j.ZipFile
import net.lingala.zip4j.model.ZipParameters
import net.lingala.zip4j.model.enums.AesKeyStrength
import net.lingala.zip4j.model.enums.EncryptionMethod
import java.io.File
import java.util.Arrays

/** Wraps `zip4j` to write a single-entry, AES-256 password-protected zip — see `functional-spec.md §7`. */
class PasswordZipWriter {
    fun writeSingleEntryZip(
        sourceFile: File,
        entryName: String,
        password: CharArray,
        destinationZip: File,
    ) {
        val parameters =
            ZipParameters().apply {
                isEncryptFiles = true
                encryptionMethod = EncryptionMethod.AES
                aesKeyStrength = AesKeyStrength.KEY_STRENGTH_256
                fileNameInZip = entryName
            }
        try {
            ZipFile(destinationZip, password).use { zip -> zip.addFile(sourceFile, parameters) }
        } finally {
            Arrays.fill(password, ' ')
        }
    }
}
