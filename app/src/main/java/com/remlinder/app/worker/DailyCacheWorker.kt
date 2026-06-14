package com.remlinder.app.worker

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.remlinder.app.alarm.AlarmReceiver
import com.remlinder.app.data.local.AppDatabase
import com.remlinder.app.data.repository.ReminderRepository
import java.util.concurrent.TimeUnit

class DailyCacheWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        return try {
            val db = AppDatabase.getInstance(applicationContext)
            val repo = ReminderRepository(db.reminderDao())

            repo.cleanOldCompleted()

            val now = System.currentTimeMillis()
            val threeDaysFromNow = now + 3 * 24 * 60 * 60 * 1000L

            val upcomingReminders = repo.getRemindersBetween(now, threeDaysFromNow)

            for (reminder in upcomingReminders) {
                if (!reminder.isCompleted && !reminder.isSnoozed) {
                    AlarmReceiver.scheduleAlarm(
                        applicationContext,
                        reminder.id,
                        reminder.triggerAtMillis
                    )
                }
            }

            scheduleNextDailyRun()

            Log.d(TAG, "Cached ${upcomingReminders.size} reminders for next 3 days")
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Daily cache failed", e)
            Result.retry()
        }
    }

    private fun scheduleNextDailyRun() {
        val dailyWork = PeriodicWorkRequestBuilder<DailyCacheWorker>(
            24, TimeUnit.HOURS
        )
            .build()

        WorkManager.getInstance(applicationContext)
            .enqueueUniquePeriodicWork(
                "daily_cache",
                ExistingPeriodicWorkPolicy.KEEP,
                dailyWork
            )
    }

    companion object {
        private const val TAG = "DailyCacheWorker"
    }
}
