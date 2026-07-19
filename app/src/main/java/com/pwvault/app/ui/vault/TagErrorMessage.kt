package com.pwvault.app.ui.vault

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.pwvault.app.R

@Composable
fun TagError.message(): String =
    when (this) {
        TagError.NAME_REQUIRED -> stringResource(R.string.error_tag_name_required)
        TagError.NAME_DUPLICATE -> stringResource(R.string.error_tag_name_duplicate)
    }
