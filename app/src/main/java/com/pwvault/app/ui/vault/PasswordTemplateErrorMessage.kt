package com.pwvault.app.ui.vault

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.pwvault.app.R

@Composable
fun PasswordTemplateError.message(): String =
    when (this) {
        PasswordTemplateError.NAME_REQUIRED -> stringResource(R.string.error_password_template_name_required)
        PasswordTemplateError.NAME_DUPLICATE -> stringResource(R.string.error_password_template_name_duplicate)
    }
