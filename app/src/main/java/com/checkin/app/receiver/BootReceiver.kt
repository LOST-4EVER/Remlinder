package com.checkin.app.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.checkin.app.CheckInApp
import com.checkin.app.service.EmergencyActionService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {

    private val scope = CoroutineScope(Dispatchers.IO + Job())

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED
            && intent.action != "android.intent.action.QUICKBOOT_POWERON"
            && intent.action != "com.htc.intent.action.QUICKBOOT_POWERON") return

        val pendingResult = goAsync()

        scope.launch {
            try {
                val app = context.applicationContext as CheckInApp
                val repository = app.repository
                val schedulingManager = app.schedulingManager

                val now = System.currentTimeMillis()
                val settings = repository.getSettings()

                if (settings != null && settings.isEnabled) {
                    if (now > settings.nextDeadlineTime) {
                        val serviceIntent = Intent(context, EmergencyActionService::class.java)
                        context.startForegroundService(serviceIntent)
                    } else {
                        schedulingManager.scheduleAlarm(context, settings.nextDeadlineTime)
                    }
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
