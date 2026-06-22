package com.checkin.app.domain

import android.content.Context
import android.content.Intent
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.checkin.app.CheckInApp
import com.checkin.app.service.EmergencyActionService
import java.util.concurrent.TimeUnit

class CheckInHealthWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val app = applicationContext as CheckInApp
        val repository = app.repository
        val schedulingManager = app.schedulingManager
        val settings = repository.getSettings() ?: return Result.success()

        val now = System.currentTimeMillis()

        if (!settings.isEnabled) {
            schedulingManager.cancelAlarm(applicationContext)
            schedulingManager.cancelHealthCheckWork(applicationContext)
            return Result.success()
        }

        val deadlinePassed = now > settings.nextDeadlineTime
        val withinWindow = settings.nextDeadlineTime - now <= TimeUnit.MINUTES.toMillis(30)
        val noAlarmScheduled = !schedulingManager.isAlarmScheduled(applicationContext)

        if (deadlinePassed) {
            val serviceIntent = Intent(applicationContext, EmergencyActionService::class.java)
            applicationContext.startForegroundService(serviceIntent)
            return Result.success()
        }

        if (withinWindow && noAlarmScheduled) {
            schedulingManager.scheduleAlarm(applicationContext, settings.nextDeadlineTime)
        }

        return Result.success()
    }
}
