package com.remlinder.app.ui.screens

import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Snooze
import androidx.compose.material.icons.filled.TextSnippet
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.remlinder.app.data.local.AppDatabase
import com.remlinder.app.data.local.MediaType
import com.remlinder.app.data.local.ReminderEntity
import com.remlinder.app.data.repository.ReminderRepository
import com.remlinder.app.ui.components.WigglyWormAnimation
import com.remlinder.app.ui.theme.AlarmBg
import com.remlinder.app.ui.theme.AlarmRed
import com.remlinder.app.ui.theme.TimerAccent

class AlarmFullScreenActivity : ComponentActivity() {

    private var mediaPlayer: MediaPlayer? = null
    private var vibrator: Vibrator? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.apply {
            addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED)
            addFlags(WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON)
            addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            addFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD)
        }

        val reminderId = intent.getLongExtra("reminder_id", -1L)

        setContent {
            MaterialTheme {
                AlarmContent(
                    reminderId = reminderId,
                    onDismiss = {
                        cleanup()
                        finish()
                    },
                    onSnooze = { minutes ->
                        snoozeReminder(reminderId, minutes)
                        cleanup()
                        finish()
                    }
                )
            }
        }
    }

    private fun snoozeReminder(reminderId: Long, minutes: Int) {
        val nextTrigger = System.currentTimeMillis() + minutes * 60 * 1000L
        com.remlinder.app.alarm.AlarmReceiver.scheduleAlarm(this, reminderId, nextTrigger)
    }

    private fun cleanup() {
        mediaPlayer?.apply {
            if (isPlaying) stop()
            release()
        }
        mediaPlayer = null
        vibrator?.cancel()
    }

    override fun onDestroy() {
        super.onDestroy()
        cleanup()
    }
}

@Composable
private fun AlarmContent(
    reminderId: Long,
    onDismiss: () -> Unit,
    onSnooze: (Int) -> Unit
) {
    val context = LocalContext.current
    var reminder by remember { mutableStateOf<ReminderEntity?>(null) }

    LaunchedEffect(reminderId) {
        if (reminderId > 0) {
            val db = AppDatabase.getInstance(context)
            val repo = ReminderRepository(db.reminderDao())
            reminder = repo.getById(reminderId)
        }
    }

    DisposableEffect(Unit) {
        var player: MediaPlayer? = null
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vm = context.getSystemService(android.content.Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vm.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(android.content.Context.VIBRATOR_SERVICE) as Vibrator
        }

        val vibeEffect = VibrationEffect.createWaveform(
            longArrayOf(0, 500, 500, 500, 500, 500),
            intArrayOf(0, 255, 0, 255, 0, 255),
            -1
        )
        vibrator.vibrate(vibeEffect)

        reminder?.mediaUri?.let { uri ->
            if (uri.isNotBlank()) {
                try {
                    player = MediaPlayer().apply {
                        setDataSource(context, Uri.parse(uri))
                        setVolume(1.0f, 1.0f)
                        isLooping = true
                        prepare()
                        start()
                    }
                } catch (_: Exception) { }
            }
        }

        onDispose {
            player?.apply {
                if (isPlaying) stop()
                release()
            }
            vibrator.cancel()
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = AlarmBg
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            WigglyWormAnimation(
                modifier = Modifier
                    .size(180.dp)
                    .padding(24.dp),
                speedMultiplier = 2f,
                wormColorStart = AlarmRed,
                wormColorEnd = TimerAccent
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "REMINDER",
                style = MaterialTheme.typography.titleLarge,
                color = AlarmRed.copy(alpha = 0.8f),
                fontWeight = FontWeight.Bold,
                letterSpacing = 4.sp
            )

            Spacer(modifier = Modifier.height(24.dp))

            reminder?.let { r ->
                val icon = when (r.mediaType) {
                    MediaType.TEXT -> Icons.Default.TextSnippet
                    MediaType.AUDIO -> Icons.Default.AudioFile
                    MediaType.IMAGE -> Icons.Default.Image
                    MediaType.TASK -> Icons.Default.TextSnippet
                }

                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.6f),
                    modifier = Modifier.size(32.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = r.title,
                    style = MaterialTheme.typography.headlineMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )

                if (!r.description.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = r.description,
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.White.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center
                    )
                }

                if (r.mediaType == MediaType.IMAGE && !r.mediaUri.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(r.mediaUri)
                            .crossfade(true)
                            .build(),
                        contentDescription = "Reminder image",
                        modifier = Modifier
                            .size(200.dp)
                            .clip(RoundedCornerShape(16.dp)),
                        contentScale = ContentScale.Crop
                    )
                }
            }

            Spacer(modifier = Modifier.height(48.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Button(
                    onClick = { onSnooze(5) },
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White.copy(alpha = 0.1f)
                    )
                ) {
                    Icon(
                        Icons.Default.Snooze,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Snooze 5m")
                }

                Button(
                    onClick = onDismiss,
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = TimerAccent
                    )
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Dismiss")
                }
            }
        }
    }
}
