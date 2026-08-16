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
        UnlockError.PIN_MISMATCH -> stringResource(R.string.error_pin_mismatch)
        UnlockError.PIN_TOO_SHORT -> stringResource(R.string.error_pin_too_short, MIN_PIN_LENGTH)
        UnlockError.PIN_NOT_NUMERIC -> stringResource(R.string.error_pin_not_numeric)
        UnlockError.WRONG_PIN -> stringResource(R.string.error_wrong_pin)
        UnlockError.BIOMETRIC_FAILED -> stringResource(R.string.error_biometric_failed)
        UnlockError.BIOMETRIC_SETUP_FAILED -> stringResource(R.string.error_biometric_setup_failed)
        UnlockError.RESTORE_INVALID_FILE -> stringResource(R.string.error_restore_invalid_file)
        UnlockError.RESTORE_WRONG_PASSWORD -> stringResource(R.string.error_restore_wrong_password)
    }
