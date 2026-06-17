package com.remlinder.app.service

import android.app.Notification
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.TextView
import androidx.core.app.NotificationCompat
import com.remlinder.app.R

class TimerOverlayService : Service() {

    private lateinit var windowManager: WindowManager
    private var overlayView: View? = null
    private var timeText: TextView? = null
    private var params: WindowManager.LayoutParams? = null

    private var initialX = 0
    private var initialY = 0
    private var initialTouchX = 0f
    private var initialTouchY = 0f

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_SHOW -> {
                val seconds = intent.getIntExtra(EXTRA_SECONDS, 0)
                showOverlay()
                updateDisplay(seconds)
                startForeground(NOTIFICATION_ID, buildNotification(seconds))
            }
            ACTION_UPDATE -> {
                val seconds = intent.getIntExtra(EXTRA_SECONDS, 0)
                updateDisplay(seconds)
                val nm = getSystemService(NotificationManager::class.java)
                nm.notify(NOTIFICATION_ID, buildNotification(seconds))
            }
            ACTION_HIDE -> {
                cleanup()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }
        return START_NOT_STICKY
    }

    private fun showOverlay() {
        if (overlayView != null) return

        val inflater = getSystemService(LAYOUT_INFLATER_SERVICE) as LayoutInflater
        overlayView = inflater.inflate(R.layout.overlay_timer, null)

        timeText = overlayView?.findViewById(R.id.overlay_time_text)

        overlayView?.setOnClickListener {
            val intent = packageManager.getLaunchIntentForPackage(packageName)
            intent?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            startActivity(intent)
        }

        overlayView?.findViewById<View>(R.id.overlay_close_btn)?.setOnClickListener {
            sendBroadcast(Intent(OVERLAY_CLOSED_ACTION))
            cleanup()
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }

        val layoutFlag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        params = WindowManager.LayoutParams(
            dpToPx(180), dpToPx(72), layoutFlag,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = dpToPx(16)
            y = dpToPx(100)
        }

        overlayView?.setOnTouchListener { _, event ->
            params?.let { p ->
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        initialX = p.x; initialY = p.y
                        initialTouchX = event.rawX; initialTouchY = event.rawY
                        false
                    }
                    MotionEvent.ACTION_MOVE -> {
                        p.x = initialX + (event.rawX - initialTouchX).toInt()
                        p.y = initialY + (event.rawY - initialTouchY).toInt()
                        windowManager.updateViewLayout(overlayView, p)
                        true
                    }
                }
            }
            false
        }

        try { windowManager.addView(overlayView, params) } catch (_: Exception) { }
    }

    private fun updateDisplay(seconds: Int) {
        timeText?.text = formatTime(seconds)
    }

    private fun buildNotification(seconds: Int): Notification {
        return NotificationCompat.Builder(this, OVERLAY_CHANNEL_ID)
            .setContentTitle("Timer Running")
            .setContentText(formatTime(seconds))
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setOngoing(true)
            .setSilent(true)
            .build()
    }

    private fun formatTime(seconds: Int): String {
        val h = seconds / 3600
        val m = (seconds % 3600) / 60
        val s = seconds % 60
        return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%02d:%02d".format(m, s)
    }

    private fun dpToPx(dp: Int) = (dp * resources.displayMetrics.density).toInt()

    private fun cleanup() {
        overlayView?.let { view ->
            try { windowManager.removeView(view) } catch (_: Exception) { }
        }
        overlayView = null; timeText = null; params = null
    }

    override fun onDestroy() { cleanup(); super.onDestroy() }
    override fun onBind(intent: Intent?) = null

    companion object {
        const val OVERLAY_CHANNEL_ID = "timer_overlay"
        const val NOTIFICATION_ID = 9001

        const val ACTION_SHOW = "com.remlinder.app.SHOW_OVERLAY"
        const val ACTION_UPDATE = "com.remlinder.app.UPDATE_OVERLAY"
        const val ACTION_HIDE = "com.remlinder.app.HIDE_OVERLAY"
        const val OVERLAY_CLOSED_ACTION = "com.remlinder.app.OVERLAY_CLOSED"
        const val EXTRA_SECONDS = "extra_seconds"

        fun show(context: Context, seconds: Int) {
            Intent(context, TimerOverlayService::class.java).apply {
                action = ACTION_SHOW
                putExtra(EXTRA_SECONDS, seconds)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) context.startForegroundService(this)
                else context.startService(this)
            }
        }

        fun update(context: Context, seconds: Int) {
            Intent(context, TimerOverlayService::class.java).apply {
                action = ACTION_UPDATE
                putExtra(EXTRA_SECONDS, seconds)
                context.startService(this)
            }
        }

        fun hide(context: Context) {
            Intent(context, TimerOverlayService::class.java).apply {
                action = ACTION_HIDE
                context.startService(this)
            }
        }
    }
}
