package com.pwvault.app.security

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.pwvault.app.R
import kotlin.time.Duration.Companion.days

const val EXPORT_REMINDER_CHANNEL_ID = "export_reminder"
private const val NOTIFICATION_ID = 1

/**
 * Runs daily (see `PwVaultApp`) and reminds the user to export manually if
 * [SecurityPolicy.EXPORT_REMINDER_INTERVAL_DAYS] have passed with no manual export —
 * `functional-spec.md §7.2`. Constructed by WorkManager's default factory, not Hilt — its only
 * dependency ([BackupPreferences]) is a plain SharedPreferences wrapper, so DI wiring adds nothing.
 */
class ExportReminderWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val backupPreferences = BackupPreferences(applicationContext)
        val lastExportAt = backupPreferences.getLastManualExportAtMillis()
        val intervalMillis = SecurityPolicy.EXPORT_REMINDER_INTERVAL_DAYS.days.inWholeMilliseconds
        val overdue = lastExportAt == null || System.currentTimeMillis() - lastExportAt >= intervalMillis
        if (overdue) showReminder()
        return Result.success()
    }

    private fun showReminder() {
        if (ActivityCompat.checkSelfPermission(applicationContext, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        val notification =
            NotificationCompat
                .Builder(applicationContext, EXPORT_REMINDER_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_monochrome)
                .setContentTitle(applicationContext.getString(R.string.export_reminder_notification_title))
                .setContentText(applicationContext.getString(R.string.export_reminder_notification_text))
                .setAutoCancel(true)
                .build()
        NotificationManagerCompat.from(applicationContext).notify(NOTIFICATION_ID, notification)
    }
}
