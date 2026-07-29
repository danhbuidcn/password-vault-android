package com.pwvault.app.domain

/** A Vault Item can carry at most this many tags — see `docs/design/tag-layout-mockup.html` (Option D). */
const val MAX_TAGS_PER_VAULT_ITEM = 3

data class Tag(
    val id: Long,
    val name: String,
)
