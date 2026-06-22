package com.checkin.app.service

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.IBinder
import android.telephony.SmsManager
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.checkin.app.CheckInApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class EmergencyActionService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    companion object {
        private const val NOTIFICATION_ID = 2001
        private const val CHANNEL_ID = "emergency_action"
        private const val CHANNEL_NAME = "Emergency Actions"
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIFICATION_ID, buildNotification())

        serviceScope.launch {
            try {
                val app = applicationContext as CheckInApp
                val repository = app.repository
                val settings = repository.getSettings() ?: return@launch

                when (settings.triggerAction) {
                    "SMS" -> performSmsAction(repository, settings.smsMessage, settings.contactPhone)
                    "PHONE_CALL" -> performCallAction(settings.contactPhone)
                    else -> performSmsAction(repository, settings.smsMessage, settings.contactPhone)
                }
            } finally {
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }

        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            CHANNEL_NAME,
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Notifications for emergency actions"
        }
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }

    private fun buildNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Emergency Action")
            .setContentText("Executing emergency contact action...")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setOngoing(true)
            .build()
    }

    private suspend fun performSmsAction(
        repository: com.checkin.app.data.repository.CheckInRepository,
        message: String,
        phone: String
    ) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS)
            != PackageManager.PERMISSION_GRANTED
        ) return

        val lat = repository.getLatitude().first()
        val lng = repository.getLongitude().first()

        val fullMessage = if (lat != 0.0 && lng != 0.0) {
            "$message\nMy location: https://maps.google.com/?q=$lat,$lng"
        } else {
            message
        }

        try {
            val smsManager = SmsManager.getDefault()
            smsManager.sendTextMessage(phone, null, fullMessage, null, null)
        } catch (_: Exception) {
        }
    }

    private suspend fun performCallAction(phone: String) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE)
            != PackageManager.PERMISSION_GRANTED
        ) return

        try {
            val callIntent = Intent(Intent.ACTION_CALL).apply {
                data = Uri.parse("tel:$phone")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            startActivity(callIntent)
        } catch (_: Exception) {
        }
    }
}
