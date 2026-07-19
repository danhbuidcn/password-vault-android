package com.pwvault.app.ui.unlock

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.pwvault.app.R

@Composable
fun UnlockError.message(): String =
    when (this) {
        UnlockError.PASSWORD_MISMATCH -> stringResource(R.string.error_password_mismatch)
        UnlockError.PASSWORD_TOO_SHORT -> stringResource(R.string.error_password_too_short, MIN_PASSWORD_LENGTH)
        UnlockError.CREATE_FAILED -> stringResource(R.string.error_create_failed)
        UnlockError.WRONG_PASSWORD -> stringResource(R.string.error_wrong_password)
    }
