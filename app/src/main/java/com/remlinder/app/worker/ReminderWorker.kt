package com.remlinder.app.worker

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.remlinder.app.alarm.AlarmReceiver
import com.remlinder.app.data.local.AppDatabase
import com.remlinder.app.data.repository.ReminderRepository
import java.util.concurrent.TimeUnit

class ReminderWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        return try {
            val db = AppDatabase.getInstance(applicationContext)
            val repo = ReminderRepository(db.reminderDao())

            val now = System.currentTimeMillis()
            val dueReminders = repo.getDueReminders(now)

            for (reminder in dueReminders) {
                AlarmReceiver.scheduleAlarm(
                    applicationContext,
                    reminder.id,
                    reminder.triggerAtMillis
                )
            }

            Log.d(TAG, "Processed ${dueReminders.size} due reminders")
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Reminder worker failed", e)
            Result.retry()
        }
    }

    companion object {
        private const val TAG = "ReminderWorker"

        fun scheduleCheck(context: Context, delayMinutes: Long = 15) {
            val checkWork = OneTimeWorkRequestBuilder<ReminderWorker>()
                .setInitialDelay(delayMinutes, TimeUnit.MINUTES)
                .build()

            WorkManager.getInstance(context)
                .enqueue(checkWork)
        }
    }
}
