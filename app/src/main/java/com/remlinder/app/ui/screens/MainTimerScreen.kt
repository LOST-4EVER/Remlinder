package com.remlinder.app.ui.screens

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RemoveCircle
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material.icons.filled.TextSnippet
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.remlinder.app.data.local.MediaType
import com.remlinder.app.ui.components.ReminderCard
import com.remlinder.app.ui.components.TimerWheel
import com.remlinder.app.ui.components.WigglyWormAnimation
import com.remlinder.app.ui.theme.TimerAccent
import com.remlinder.app.viewmodel.MainViewModel

private val DURATION_PRESETS = listOf(
    1 to "1m", 3 to "3m", 5 to "5m", 10 to "10m",
    15 to "15m", 30 to "30m", 60 to "1h", 120 to "2h"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainTimerScreen(viewModel: MainViewModel) {
    val reminders by viewModel.activeReminders.collectAsState(initial = emptyList())
    val isTimerRunning by viewModel.isTimerRunning.collectAsState()
    val remainingSeconds by viewModel.remainingSeconds.collectAsState()
    val totalSeconds by viewModel.totalSeconds.collectAsState()
    val timerComplete by viewModel.timerComplete.collectAsState()
    val activeCount by viewModel.activeCount.collectAsState(initial = 0)
    val showOverlay by viewModel.showOverlay.collectAsState()
    val context = LocalContext.current

    var showCreateSheet by remember { mutableStateOf(false) }

    val progress = if (totalSeconds > 0) 1f - (remainingSeconds.toFloat() / totalSeconds) else 0f
    val wheelProgress by animateFloatAsState(targetValue = progress, animationSpec = tween(400), label = "progress")

    LaunchedEffect(Unit) {
        viewModel.events.collect { }
    }

    Scaffold(
        topBar = {
            LargeTopAppBar(
                title = {
                    Column {
                        Text("Remlinder", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                        Text(
                            if (activeCount > 0) "$activeCount active" else "No active reminders",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                colors = TopAppBarDefaults.largeTopAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showCreateSheet = true },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("New Reminder") },
                containerColor = TimerAccent
            )
        }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            Column(Modifier.fillMaxSize()) {

                // ── Timer Section ──────────────────────────────────
                Box(
                    Modifier.fillMaxWidth().weight(0.5f),
                    contentAlignment = Alignment.Center
                ) {
                    TimerWheel(
                        progress = wheelProgress,
                        onProgressChange = { viewModel.updateProgress(it) },
                        enabled = !isTimerRunning,
                        size = 260.dp
                    )

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            viewModel.formatTime(remainingSeconds),
                            style = MaterialTheme.typography.displayLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Spacer(Modifier.height(4.dp))

                        Text(
                            if (isTimerRunning) "Running..." else if (totalSeconds > 0) "Ready" else "Set time",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // ── Duration Presets ───────────────────────────────
                if (!isTimerRunning) {
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(DURATION_PRESETS) { (minutes, label) ->
                            val selected = totalSeconds == minutes * 60
                            Surface(
                                onClick = { viewModel.setDurationMinutes(minutes) },
                                shape = RoundedCornerShape(20.dp),
                                color = if (selected) MaterialTheme.colorScheme.primaryContainer
                                        else MaterialTheme.colorScheme.surfaceContainerHighest,
                                border = if (selected) BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(0.4f)) else null
                            ) {
                                Text(
                                    label,
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (selected) MaterialTheme.colorScheme.primary
                                            else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                // ── Fine-tune Controls ─────────────────────────────
                if (!isTimerRunning && totalSeconds > 0) {
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        FilledTonalIconButton(onClick = { viewModel.adjustDuration(-300) }) {
                            Icon(Icons.Default.RemoveCircle, "−5m", Modifier.size(20.dp))
                        }
                        Spacer(Modifier.width(8.dp))
                        FilledTonalIconButton(onClick = { viewModel.adjustDuration(-60) }) {
                            Icon(Icons.Default.RemoveCircle, "−1m", Modifier.size(18.dp))
                        }

                        Spacer(Modifier.width(24.dp))

                        // ── Play / Pause / Reset ───────────────────
                        if (!isTimerRunning && remainingSeconds > 0) {
                            Button(
                                onClick = {
                                    if (viewModel.hasOverlayPermission()) {
                                        viewModel.startTimer()
                                    } else {
                                        val intent = Intent(
                                            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                            Uri.parse("package:${context.packageName}")
                                        )
                                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                        context.startActivity(intent)
                                    }
                                },
                                shape = CircleShape,
                                modifier = Modifier.size(56.dp),
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Icon(Icons.Default.PlayArrow, "Start", Modifier.size(28.dp))
                            }
                        }

                        if (isTimerRunning) {
                            Button(
                                onClick = { viewModel.pauseTimer() },
                                shape = CircleShape,
                                modifier = Modifier.size(56.dp),
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Icon(Icons.Default.Pause, "Pause", Modifier.size(28.dp))
                            }
                        }

                        Spacer(Modifier.width(8.dp))

                        FilledTonalIconButton(onClick = { viewModel.adjustDuration(60) }) {
                            Icon(Icons.Default.AddCircle, "+1m", Modifier.size(18.dp))
                        }
                        Spacer(Modifier.width(8.dp))
                        FilledTonalIconButton(onClick = { viewModel.adjustDuration(300) }) {
                            Icon(Icons.Default.AddCircle, "+5m", Modifier.size(20.dp))
                        }
                    }
                }

                // ── Running Controls ───────────────────────────────
                if (isTimerRunning) {
                    Spacer(Modifier.height(8.dp))
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        FilledTonalButton(onClick = { viewModel.pauseTimer() }) {
                            Icon(Icons.Default.Pause, null, Modifier.size(20.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Pause")
                        }
                        Spacer(Modifier.width(12.dp))
                        OutlinedButton(onClick = { viewModel.resetTimer() }) {
                            Icon(Icons.Default.RestartAlt, null, Modifier.size(20.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Reset")
                        }
                    }
                }

                if (!isTimerRunning && remainingSeconds > 0) {
                    Spacer(Modifier.height(4.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                        OutlinedButton(onClick = { viewModel.resetTimer() }) {
                            Icon(Icons.Default.RestartAlt, null, Modifier.size(20.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Reset")
                        }
                    }
                }

                // ── Reminder List ──────────────────────────────────
                AnimatedVisibility(
                    reminders.isNotEmpty(), enter = fadeIn() + slideInVertically(), exit = fadeOut()
                ) {
                    LazyColumn(
                        Modifier.fillMaxSize().padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(bottom = 96.dp, top = 8.dp)
                    ) {
                        items(reminders, key = { it.id }) { reminder ->
                            ReminderCard(
                                reminder = reminder,
                                onDelete = { viewModel.deleteReminder(reminder.id) },
                                onComplete = { viewModel.markCompleted(reminder.id) },
                                onSnooze = {
                                    viewModel.snoozeReminder(reminder.id, System.currentTimeMillis() + 5 * 60 * 1000)
                                }
                            )
                        }
                    }
                }

                // ── Empty State ────────────────────────────────────
                AnimatedVisibility(
                    reminders.isEmpty() && !timerComplete, enter = fadeIn(), exit = fadeOut()
                ) {
                    Box(Modifier.fillMaxSize().weight(1f), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            WigglyWormAnimation(Modifier.size(80.dp).padding(16.dp), speedMultiplier = 0.8f, wormCount = 1)
                            Spacer(Modifier.height(16.dp))
                            Text("No reminders yet", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("Tap + to create one", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                        }
                    }
                }
            }

            // ── Timer Complete Overlay ─────────────────────────────
            if (timerComplete) {
                Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)) {
                    Column(
                        Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        WigglyWormAnimation(Modifier.size(200.dp).padding(32.dp), speedMultiplier = 1.5f)
                        Spacer(Modifier.height(24.dp))
                        Text("Time's Up!", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold, color = TimerAccent)
                        Spacer(Modifier.height(8.dp))
                        Text(viewModel.formatTime(totalSeconds), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(24.dp))
                        Button(onClick = { viewModel.resetTimer() }, shape = RoundedCornerShape(16.dp)) {
                            Text("Dismiss")
                        }
                    }
                }
            }
        }
    }

    // ── Create Reminder Sheet ──────────────────────────────────
    if (showCreateSheet) {
        CreateReminderSheet(
            onDismiss = { showCreateSheet = false },
            onCreate = { title, description, mediaType, uri, duration ->
                viewModel.createReminder(title, description, mediaType, uri, duration)
                showCreateSheet = false
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CreateReminderSheet(
    onDismiss: () -> Unit,
    onCreate: (String, String?, MediaType, String?, Int) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val context = LocalContext.current

    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var durationMinutes by remember { mutableIntStateOf(5) }
    var selectedMediaType by remember { mutableStateOf(MediaType.TEXT) }
    var mediaUri by remember { mutableStateOf<String?>(null) }

    val audioPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri -> mediaUri = uri?.toString() }
    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri -> mediaUri = uri?.toString() }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState, shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 32.dp)) {

            Text("New Reminder", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(20.dp))

            OutlinedTextField(
                value = title, onValueChange = { title = it },
                label = { Text("Title") }, singleLine = true,
                modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)
            )
            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = description, onValueChange = { description = it },
                label = { Text("Description (optional)") },
                modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp),
                minLines = 2, maxLines = 4
            )
            Spacer(Modifier.height(16.dp))

            Text("Media Type", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(8.dp))

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MediaType.values().forEach { type ->
                    val icon = when (type) { MediaType.TEXT -> Icons.Default.TextSnippet; MediaType.AUDIO -> Icons.Default.AudioFile; MediaType.IMAGE -> Icons.Default.Image; MediaType.TASK -> Icons.Default.TaskAlt }
                    val name = when (type) { MediaType.TEXT -> "Text"; MediaType.AUDIO -> "Audio"; MediaType.IMAGE -> "Image"; MediaType.TASK -> "Task" }

                    Surface(
                        onClick = {
                            selectedMediaType = type
                            if (type == MediaType.AUDIO) audioPicker.launch("audio/*")
                            else if (type == MediaType.IMAGE) imagePicker.launch("image/*")
                        },
                        shape = RoundedCornerShape(12.dp),
                        color = if (selectedMediaType == type) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.weight(1f),
                        border = if (selectedMediaType == type) BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(0.3f)) else null
                    ) {
                        Column(Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(icon, null, tint = if (selectedMediaType == type) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(24.dp))
                            Text(name, style = MaterialTheme.typography.labelSmall, color = if (selectedMediaType == type) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }

            if (mediaUri != null) {
                Spacer(Modifier.height(8.dp))
                Text("Media attached", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
            }
            Spacer(Modifier.height(16.dp))

            Text("Duration", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(8.dp))

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(1, 5, 10, 15, 30, 60).forEach { min ->
                    OutlinedButton(
                        onClick = { durationMinutes = min },
                        shape = RoundedCornerShape(12.dp),
                        colors = if (durationMinutes == min) ButtonDefaults.outlinedButtonColors(containerColor = MaterialTheme.colorScheme.primary.copy(0.1f))
                                else ButtonDefaults.outlinedButtonColors()
                    ) {
                        Text(if (min < 60) "${min}m" else "${min / 60}h", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
            Spacer(Modifier.height(24.dp))

            Button(
                onClick = { if (title.isNotBlank()) onCreate(title, description.ifBlank { null }, selectedMediaType, mediaUri, durationMinutes * 60) },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(16.dp),
                enabled = title.isNotBlank()
            ) {
                Text("Create Reminder", fontWeight = FontWeight.SemiBold)
            }
        }
    }
}
