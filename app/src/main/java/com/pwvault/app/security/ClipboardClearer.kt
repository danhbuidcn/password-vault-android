package com.pwvault.app.security

import android.content.ClipData
import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import android.os.PersistableBundle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val CLIP_LABEL = "password"

/**
 * Copies a password to the clipboard and overwrites it after [SecurityPolicy.CLIPBOARD_CLEAR_DELAY] —
 * `functional-spec.md §5`. Marks the clip as sensitive (API 33+) so the system clipboard preview
 * doesn't render the plaintext password, matching the app-wide FLAG_SECURE data-leak stance.
 *
 * Schedules the clear on its own process-lifetime scope rather than the caller's `viewModelScope` —
 * that scope is cancelled as soon as the Activity/ViewModel is destroyed (e.g. the user backs out of
 * the app right after copying), which would otherwise silently skip the clear and leave the password
 * on the clipboard indefinitely.
 */
class ClipboardClearer(
    private val context: Context,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    fun copyPassword(password: String) {
        val manager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        manager.setPrimaryClip(sensitiveClip(password))
        scope.launch {
            delay(SecurityPolicy.CLIPBOARD_CLEAR_DELAY)
            manager.setPrimaryClip(sensitiveClip(""))
        }
    }

    private fun sensitiveClip(text: String): ClipData {
        val clip = ClipData.newPlainText(CLIP_LABEL, text)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            clip.description.extras =
                PersistableBundle().apply {
                    putBoolean(ClipDescription.EXTRA_IS_SENSITIVE, true)
                }
        }
        return clip
    }
}
