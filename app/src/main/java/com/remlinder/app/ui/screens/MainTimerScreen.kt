package com.remlinder.app.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.provider.AlarmClock
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material.icons.filled.TextSnippet
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.remlinder.app.data.local.MediaType
import com.remlinder.app.ui.components.ReminderCard
import com.remlinder.app.ui.components.TimerWheel
import com.remlinder.app.ui.components.WigglyWormAnimation
import com.remlinder.app.ui.theme.TimerAccent
import com.remlinder.app.viewmodel.MainViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainTimerScreen(
    viewModel: MainViewModel
) {
    val reminders by viewModel.activeReminders.collectAsState(initial = emptyList())
    val isTimerRunning by viewModel.isTimerRunning.collectAsState()
    val remainingSeconds by viewModel.remainingSeconds.collectAsState()
    val totalSeconds by viewModel.totalSeconds.collectAsState()
    val timerComplete by viewModel.timerComplete.collectAsState()
    val activeCount by viewModel.activeCount.collectAsState(initial = 0)

    var showCreateSheet by remember { mutableStateOf(false) }
    var selectedMediaType by remember { mutableStateOf(MediaType.TEXT) }

    val context = LocalContext.current

    LaunchedEffect(timerComplete) {
        if (timerComplete) {
            delay(2000)
            viewModel.resetTimer()
        }
    }

    Scaffold(
        topBar = {
            LargeTopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Reminders",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold
                        )
                        if (activeCount > 0) {
                            Text(
                                text = "$activeCount active",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.largeTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(360.dp),
                contentAlignment = Alignment.Center
            ) {
                TimerWheel(
                    progress = if (totalSeconds > 0) 1f - (remainingSeconds.toFloat() / totalSeconds) else 0f,
                    onProgressChange = {},
                    enabled = false,
                    size = 260.dp
                )

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    val displayTime = formatTime(remainingSeconds)
                    Text(
                        text = displayTime,
                        style = MaterialTheme.typography.displayLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        if (!isTimerRunning && remainingSeconds > 0) {
                            Button(
                                onClick = { viewModel.startTimer() },
                                shape = CircleShape,
                                modifier = Modifier.size(56.dp),
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)
                            ) {
                                Icon(
                                    Icons.Default.PlayArrow,
                                    contentDescription = "Start",
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        } else if (isTimerRunning) {
                            Button(
                                onClick = { viewModel.pauseTimer() },
                                shape = CircleShape,
                                modifier = Modifier.size(56.dp),
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)
                            ) {
                                Icon(
                                    Icons.Default.Pause,
                                    contentDescription = "Pause",
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }

                        if (remainingSeconds > 0) {
                            OutlinedButton(
                                onClick = { viewModel.resetTimer() },
                                shape = CircleShape,
                                modifier = Modifier.size(56.dp),
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)
                            ) {
                                Icon(
                                    Icons.Default.RestartAlt,
                                    contentDescription = "Reset",
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }
                    }
                }

                if (timerComplete) {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            WigglyWormAnimation(
                                modifier = Modifier
                                    .size(200.dp)
                                    .padding(32.dp),
                                speedMultiplier = 1.5f
                            )

                            Spacer(modifier = Modifier.height(24.dp))

                            Text(
                                text = "Time's Up!",
                                style = MaterialTheme.typography.headlineLarge,
                                fontWeight = FontWeight.Bold,
                                color = TimerAccent
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            Button(
                                onClick = { viewModel.resetTimer() },
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Text("Dismiss")
                            }
                        }
                    }
                }
            }

            AnimatedVisibility(
                visible = true,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 96.dp)
                ) {
                    if (reminders.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 48.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    WigglyWormAnimation(
                                        modifier = Modifier
                                            .size(80.dp)
                                            .padding(16.dp),
                                        speedMultiplier = 0.8f,
                                        wormCount = 1
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text(
                                        text = "No reminders yet",
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                                    )
                                    Text(
                                        text = "Tap + to create one",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                                    )
                                }
                            }
                        }
                    } else {
                        items(reminders, key = { it.id }) { reminder ->
                            ReminderCard(
                                reminder = reminder,
                                onDelete = { viewModel.deleteReminder(reminder.id) },
                                onSnooze = {
                                    viewModel.snoozeReminder(
                                        reminder.id,
                                        System.currentTimeMillis() + 5 * 60 * 1000
                                    )
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showCreateSheet) {
        CreateReminderSheet(
            selectedMediaType = selectedMediaType,
            onMediaTypeChange = { selectedMediaType = it },
            onDismiss = { showCreateSheet = false },
            onCreate = { title, description, uri, duration ->
                viewModel.createReminder(
                    title = title,
                    description = description,
                    mediaType = selectedMediaType,
                    mediaUri = uri,
                    durationSeconds = duration
                )
                showCreateSheet = false
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CreateReminderSheet(
    selectedMediaType: MediaType,
    onMediaTypeChange: (MediaType) -> Unit,
    onDismiss: () -> Unit,
    onCreate: (title: String, description: String?, uri: String?, durationSeconds: Int) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()

    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var durationMinutes by remember { mutableIntStateOf(5) }
    var mediaUri by remember { mutableStateOf<String?>(null) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
        ) {
            Text(
                text = "New Reminder",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(20.dp))

            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Title") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = description ?: "",
                onValueChange = { description = it },
                label = { Text("Description (optional)") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                minLines = 2,
                maxLines = 4
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Media Type",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MediaType.values().forEach { type ->
                    val icon = when (type) {
                        MediaType.TEXT -> Icons.Default.TextSnippet
                        MediaType.AUDIO -> Icons.Default.AudioFile
                        MediaType.IMAGE -> Icons.Default.Image
                        MediaType.TASK -> Icons.Default.TaskAlt
                    }
                    val name = when (type) {
                        MediaType.TEXT -> "Text"
                        MediaType.AUDIO -> "Audio"
                        MediaType.IMAGE -> "Image"
                        MediaType.TASK -> "Task"
                    }

                    Surface(
                        onClick = { onMediaTypeChange(type) },
                        shape = RoundedCornerShape(12.dp),
                        color = if (selectedMediaType == type)
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                        else
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                tint = if (selectedMediaType == type)
                                    MaterialTheme.colorScheme.primary
                                else
                                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                modifier = Modifier.size(24.dp)
                            )
                            Text(
                                text = name,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (selectedMediaType == type)
                                    MaterialTheme.colorScheme.primary
                                else
                                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Duration",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(1, 5, 10, 15, 30, 60).forEach { min ->
                    OutlinedButton(
                        onClick = { durationMinutes = min },
                        shape = RoundedCornerShape(12.dp),
                        colors = if (durationMinutes == min)
                            ButtonDefaults.outlinedButtonColors(
                                containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                            )
                        else
                            ButtonDefaults.outlinedButtonColors()
                    ) {
                        Text(
                            text = if (min < 60) "${min}m" else "${min / 60}h",
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    if (title.isNotBlank()) {
                        onCreate(
                            title,
                            description.ifBlank { null },
                            mediaUri,
                            durationMinutes * 60
                        )
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(16.dp),
                enabled = title.isNotBlank()
            ) {
                Text("Create Reminder")
            }
        }
    }
}

private fun formatTime(seconds: Int): String {
    val h = seconds / 3600
    val m = (seconds % 3600) / 60
    val s = seconds % 60
    return if (h > 0) {
        String.format("%d:%02d:%02d", h, m, s)
    } else {
        String.format("%02d:%02d", m, s)
    }
}
