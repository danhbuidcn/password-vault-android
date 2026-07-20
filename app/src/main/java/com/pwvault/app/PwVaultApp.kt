package com.pwvault.app

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.pwvault.app.security.EXPORT_REMINDER_CHANNEL_ID
import com.pwvault.app.security.ExportReminderWorker
import dagger.hilt.android.HiltAndroidApp
import java.util.concurrent.TimeUnit

private const val EXPORT_REMINDER_WORK_NAME = "export_reminder"

/** How often the reminder check itself runs — distinct from the overdue threshold it checks against. */
private const val EXPORT_REMINDER_WORKER_CHECK_INTERVAL_DAYS = 1L

@HiltAndroidApp
class PwVaultApp : Application() {
    override fun onCreate() {
        super.onCreate()
        createExportReminderNotificationChannel()
        scheduleExportReminderWork()
    }

    private fun createExportReminderNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel =
            NotificationChannel(
                EXPORT_REMINDER_CHANNEL_ID,
                getString(R.string.export_reminder_channel_name),
                NotificationManager.IMPORTANCE_DEFAULT,
            )
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    private fun scheduleExportReminderWork() {
        val request =
            PeriodicWorkRequestBuilder<ExportReminderWorker>(
                EXPORT_REMINDER_WORKER_CHECK_INTERVAL_DAYS,
                TimeUnit.DAYS,
            ).build()
        WorkManager
            .getInstance(this)
            .enqueueUniquePeriodicWork(EXPORT_REMINDER_WORK_NAME, ExistingPeriodicWorkPolicy.KEEP, request)
    }
}
