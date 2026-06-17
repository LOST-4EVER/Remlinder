package com.remlinder.app.viewmodel

import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.provider.Settings
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.remlinder.app.alarm.AlarmReceiver
import com.remlinder.app.data.local.AppDatabase
import com.remlinder.app.data.local.MediaType
import com.remlinder.app.data.repository.ReminderRepository
import com.remlinder.app.service.TimerOverlayService
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

sealed interface TimerEvent {
    data object OverlayClosed : TimerEvent
}

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getInstance(application)
    private val repository = ReminderRepository(db.reminderDao())

    private val _events = MutableSharedFlow<TimerEvent>(extraBufferCapacity = 2)
    val events = _events.asSharedFlow()

    val activeReminders = repository.activeReminders
    val activeCount = repository.activeCount

    private val _remainingSeconds = MutableStateFlow(0)
    val remainingSeconds: StateFlow<Int> = _remainingSeconds.asStateFlow()

    private val _totalSeconds = MutableStateFlow(0)
    val totalSeconds: StateFlow<Int> = _totalSeconds.asStateFlow()

    private val _isTimerRunning = MutableStateFlow(false)
    val isTimerRunning: StateFlow<Boolean> = _isTimerRunning.asStateFlow()

    private val _timerComplete = MutableStateFlow(false)
    val timerComplete: StateFlow<Boolean> = _timerComplete.asStateFlow()

    private val _showOverlay = MutableStateFlow(false)
    val showOverlay: StateFlow<Boolean> = _showOverlay.asStateFlow()

    private var timerJob: Job? = null
    private var overlayReceiver: BroadcastReceiver? = null

    init {
        registerOverlayReceiver()
    }

    private fun registerOverlayReceiver() {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                if (intent.action == TimerOverlayService.OVERLAY_CLOSED_ACTION) {
                    timerJob?.cancel()
                    _isTimerRunning.value = false
                    _remainingSeconds.value = _totalSeconds.value
                    _showOverlay.value = false
                    _events.tryEmit(TimerEvent.OverlayClosed)
                }
            }
        }
        overlayReceiver = receiver
        val filter = IntentFilter(TimerOverlayService.OVERLAY_CLOSED_ACTION)
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Context.RECEIVER_NOT_EXPORTED
        } else {
            0
        }
        getApplication<Application>().registerReceiver(receiver, filter, flags)
    }

    fun createReminder(
        title: String,
        description: String?,
        mediaType: MediaType,
        mediaUri: String?,
        durationSeconds: Int
    ) {
        viewModelScope.launch {
            val triggerAt = System.currentTimeMillis() + (durationSeconds * 1000L)
            val id = repository.insert(
                title = title,
                description = description,
                mediaType = mediaType,
                mediaUri = mediaUri,
                triggerAtMillis = triggerAt,
                durationSeconds = durationSeconds
            )
            AlarmReceiver.scheduleAlarm(getApplication(), id, triggerAt)
            if (durationSeconds > 0) {
                setTimerDuration(durationSeconds)
                startTimer()
            }
        }
    }

    fun deleteReminder(id: Long) {
        viewModelScope.launch {
            AlarmReceiver.cancelAlarm(getApplication(), id)
            repository.delete(id)
        }
    }

    fun snoozeReminder(id: Long, nextTriggerAtMillis: Long) {
        viewModelScope.launch {
            repository.snooze(id, nextTriggerAtMillis)
            AlarmReceiver.scheduleAlarm(getApplication(), id, nextTriggerAtMillis)
        }
    }

    fun markCompleted(id: Long) {
        viewModelScope.launch {
            AlarmReceiver.cancelAlarm(getApplication(), id)
            repository.markCompleted(id)
        }
    }

    fun startTimer() {
        if (_remainingSeconds.value <= 0) return
        _isTimerRunning.value = true
        _timerComplete.value = false
        _showOverlay.value = true

        val ctx = getApplication<Application>()
        TimerOverlayService.show(ctx, _remainingSeconds.value)

        timerJob = viewModelScope.launch {
            while (isActive && _remainingSeconds.value > 0) {
                delay(1000L)
                _remainingSeconds.value -= 1
                TimerOverlayService.update(ctx, _remainingSeconds.value)
            }
            if (_remainingSeconds.value <= 0 && _totalSeconds.value > 0) {
                _timerComplete.value = true
                _isTimerRunning.value = false
                _showOverlay.value = false
                TimerOverlayService.hide(ctx)
            }
        }
    }

    fun pauseTimer() {
        _isTimerRunning.value = false
        timerJob?.cancel()
    }

    fun resumeTimer() {
        if (_remainingSeconds.value > 0 && !_isTimerRunning.value) {
            _isTimerRunning.value = true
            val ctx = getApplication<Application>()
            timerJob = viewModelScope.launch {
                while (isActive && _remainingSeconds.value > 0) {
                    delay(1000L)
                    _remainingSeconds.value -= 1
                    TimerOverlayService.update(ctx, _remainingSeconds.value)
                }
                if (_remainingSeconds.value <= 0 && _totalSeconds.value > 0) {
                    _timerComplete.value = true
                    _isTimerRunning.value = false
                    _showOverlay.value = false
                    TimerOverlayService.hide(ctx)
                }
            }
        }
    }

    fun resetTimer() {
        timerJob?.cancel()
        _isTimerRunning.value = false
        _remainingSeconds.value = _totalSeconds.value
        _timerComplete.value = false
        _showOverlay.value = false
        TimerOverlayService.hide(getApplication())
    }

    fun setTimerDuration(seconds: Int) {
        if (!_isTimerRunning.value) {
            _totalSeconds.value = seconds
            _remainingSeconds.value = seconds
            _timerComplete.value = false
        }
    }

    fun setDurationMinutes(minutes: Int) = setTimerDuration(minutes * 60)

    fun adjustDuration(deltaSeconds: Int) {
        if (!_isTimerRunning.value) {
            val new = (_totalSeconds.value + deltaSeconds).coerceIn(60, 7200)
            _totalSeconds.value = new
            _remainingSeconds.value = new
            _timerComplete.value = false
        }
    }

    fun updateProgress(progress: Float) {
        if (!_isTimerRunning.value && progress > 0f) {
            val seconds = (progress * 7200).toInt().coerceIn(60, 7200)
            _totalSeconds.value = seconds
            _remainingSeconds.value = seconds
            _timerComplete.value = false
        }
    }

    fun hasOverlayPermission() = Settings.canDrawOverlays(getApplication())

    fun formatTime(seconds: Int): String {
        val h = seconds / 3600
        val m = (seconds % 3600) / 60
        val s = seconds % 60
        return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%02d:%02d".format(m, s)
    }

    override fun onCleared() {
        super.onCleared()
        overlayReceiver?.let {
            try { getApplication<Application>().unregisterReceiver(it) } catch (_: Exception) { }
        }
        TimerOverlayService.hide(getApplication())
    }
}
