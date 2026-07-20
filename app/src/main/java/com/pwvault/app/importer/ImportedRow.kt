package com.pwvault.app.importer

/** A source row after column mapping, ready to become a Login-type Vault Item. */
data class ImportedRow(
    val name: String,
    val username: String,
    val password: String,
    val url: String,
    val note: String,
)
