package com.pwvault.app.export

import com.pwvault.app.security.SecurityPolicy
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File

/**
 * Deletes a plaintext export temp file after [SecurityPolicy.EXPORT_TEMP_FILE_CLEANUP_DELAY] —
 * `functional-spec.md §7`. Scheduled on its own process-lifetime scope rather than the caller's
 * `viewModelScope`, same reasoning as [com.pwvault.app.security.ClipboardClearer]: that scope is
 * cancelled as soon as the ViewModel is destroyed, which would otherwise skip the delete. If the
 * process itself dies first, the file dies with it — also satisfying "or on app exit".
 */
class ExportTempFileCleaner {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun scheduleDelete(file: File) {
        scope.launch {
            delay(SecurityPolicy.EXPORT_TEMP_FILE_CLEANUP_DELAY)
            file.delete()
        }
    }
}
