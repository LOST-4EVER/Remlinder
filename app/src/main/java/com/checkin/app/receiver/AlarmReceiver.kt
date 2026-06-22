package com.checkin.app.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.PowerManager
import com.checkin.app.CheckInApp
import com.checkin.app.service.EmergencyActionService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

class AlarmReceiver : BroadcastReceiver() {

    private val scope = CoroutineScope(Dispatchers.IO + Job())

    override fun onReceive(context: Context, intent: Intent) {
        val pendingResult = goAsync()
        try {
            val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
            val wakeLock = powerManager.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK,
                "CheckInApp:AlarmWakeLock"
            )
            wakeLock.acquire(TimeUnit.MINUTES.toMillis(2))

            scope.launch {
                try {
                    val app = context.applicationContext as CheckInApp
                    val repository = app.repository
                    val schedulingManager = app.schedulingManager
                    val settings = repository.getSettings() ?: return@launch

                    val now = System.currentTimeMillis()
                    if (now > settings.nextDeadlineTime) {
                        val serviceIntent = Intent(context, EmergencyActionService::class.java)
                        context.startForegroundService(serviceIntent)
                        repository.updateSettings(settings.copy(isEnabled = false))
                        schedulingManager.cancelAlarm(context)
                    } else if (settings.isEnabled) {
                        val nextDeadline = now + TimeUnit.HOURS.toMillis(settings.intervalHours.toLong())
                        schedulingManager.scheduleAlarm(context, nextDeadline)
                    }
                } finally {
                    if (wakeLock.isHeld) {
                        wakeLock.release()
                    }
                    pendingResult.finish()
                }
            }
        } catch (e: Exception) {
            pendingResult.finish()
        }
    }
}
