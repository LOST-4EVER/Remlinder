package com.remlinder.app.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.remlinder.app.alarm.AlarmReceiver
import com.remlinder.app.data.local.AppDatabase
import com.remlinder.app.data.local.MediaType
import com.remlinder.app.data.repository.ReminderRepository
import com.remlinder.app.worker.DailyCacheWorker
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getInstance(application)
    private val repository = ReminderRepository(db.reminderDao())

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

    private var timerJob: kotlinx.coroutines.Job? = null

    init {
        loadReminders()
    }

    private fun loadReminders() {
        viewModelScope.launch {
            repository.activeReminders.stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )
        }
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

            AlarmReceiver.scheduleAlarm(
                getApplication(),
                id,
                triggerAt
            )

            if (durationSeconds > 0) {
                startCountdown(durationSeconds)
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
        _isTimerRunning.value = true
        _timerComplete.value = false

        timerJob = viewModelScope.launch {
            while (isActive && _remainingSeconds.value > 0) {
                delay(1000L)
                _remainingSeconds.value -= 1
            }

            if (_remainingSeconds.value <= 0 && _totalSeconds.value > 0) {
                _timerComplete.value = true
                _isTimerRunning.value = false
            }
        }
    }

    fun pauseTimer() {
        _isTimerRunning.value = false
        timerJob?.cancel()
    }

    fun resetTimer() {
        timerJob?.cancel()
        _isTimerRunning.value = false
        _remainingSeconds.value = _totalSeconds.value
        _timerComplete.value = false
    }

    fun setTimerDuration(seconds: Int) {
        _totalSeconds.value = seconds
        _remainingSeconds.value = seconds
        _isTimerRunning.value = false
        _timerComplete.value = false
    }

    private fun startCountdown(seconds: Int) {
        setTimerDuration(seconds)
        startTimer()
    }
}
