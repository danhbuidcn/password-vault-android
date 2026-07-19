package com.pwvault.app.ui.vault

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.pwvault.app.R

@Composable
fun VaultFormError.message(): String =
    when (this) {
        VaultFormError.NAME_REQUIRED -> stringResource(R.string.error_item_name_required)
    }
