package com.remlinder.app

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import androidx.work.Configuration
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.remlinder.app.service.TimerOverlayService
import com.remlinder.app.worker.DailyCacheWorker
import java.util.concurrent.TimeUnit

class RemlinderApp : Application(), Configuration.Provider {

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
        scheduleDailyCache()
    }

    private fun createNotificationChannels() {
        val alarmChannel = NotificationChannel(
            REMINDER_CHANNEL_ID,
            "Reminders",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Reminder alarm notifications"
            enableVibration(true)
            enableLights(true)
            setShowBadge(true)
        }

        val overlayChannel = NotificationChannel(
            TimerOverlayService.OVERLAY_CHANNEL_ID,
            "Timer Overlay",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Shows floating timer over other apps"
            setShowBadge(false)
        }

        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.createNotificationChannel(alarmChannel)
        notificationManager.createNotificationChannel(overlayChannel)
    }

    private fun scheduleDailyCache() {
        val dailyWork = PeriodicWorkRequestBuilder<DailyCacheWorker>(
            24, TimeUnit.HOURS
        ).build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "daily_cache",
            ExistingPeriodicWorkPolicy.KEEP,
            dailyWork
        )
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setMinimumLoggingLevel(android.util.Log.INFO)
            .build()

    companion object {
        const val REMINDER_CHANNEL_ID = "remlinder_reminders"
    }
}
