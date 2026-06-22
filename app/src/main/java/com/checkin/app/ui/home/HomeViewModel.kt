package com.checkin.app.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.checkin.app.data.repository.CheckInRepository
import com.checkin.app.domain.SchedulingManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import kotlin.math.max

data class HomeUiState(
    val timeRemainingMillis: Long = 0L,
    val isEnabled: Boolean = false,
    val contactName: String = "",
    val intervalHours: Int = 24,
    val isDeadlineExpired: Boolean = false,
    val formattedTimeRemaining: String = "00:00:00"
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: CheckInRepository,
    private val schedulingManager: SchedulingManager
) : ViewModel() {

    private val _state = MutableStateFlow(HomeUiState())
    val state: StateFlow<HomeUiState> = _state.asStateFlow()

    private var tickJob: Job? = null

    init {
        viewModelScope.launch {
            repository.settingsFlow.collect { settings ->
                settings?.let { s ->
                    val now = System.currentTimeMillis()
                    val expired = s.isEnabled && now > s.nextDeadlineTime
                    val remaining = if (expired || !s.isEnabled) {
                        0L
                    } else {
                        max(0L, s.nextDeadlineTime - now)
                    }
                    _state.update {
                        it.copy(
                            isEnabled = s.isEnabled,
                            contactName = s.contactName,
                            intervalHours = s.intervalHours,
                            isDeadlineExpired = expired,
                            timeRemainingMillis = remaining,
                            formattedTimeRemaining = if (s.isEnabled) formatTime(remaining) else "00:00:00"
                        )
                    }
                }
            }
        }

        tickJob = viewModelScope.launch {
            while (isActive) {
                delay(1000L)
                _state.update { current ->
                    if (current.isEnabled && !current.isDeadlineExpired) {
                        val newRemaining = max(0L, current.timeRemainingMillis - 1000L)
                        current.copy(
                            timeRemainingMillis = newRemaining,
                            isDeadlineExpired = newRemaining == 0L,
                            formattedTimeRemaining = formatTime(newRemaining)
                        )
                    } else {
                        current
                    }
                }
            }
        }
    }

    fun checkIn() {
        viewModelScope.launch {
            repository.checkIn()
            val settings = repository.getSettings() ?: return@launch
            schedulingManager.scheduleAlarm(
                CheckInAppProvider.getContext(),
                settings.nextDeadlineTime
            )
        }
    }

    fun startTracking() {
        viewModelScope.launch {
            val settings = repository.getSettings() ?: return@launch
            val now = System.currentTimeMillis()
            val deadline = if (settings.nextDeadlineTime <= now) {
                now + TimeUnit.HOURS.toMillis(settings.intervalHours.toLong())
            } else {
                settings.nextDeadlineTime
            }
            repository.updateSettings(settings.copy(isEnabled = true, nextDeadlineTime = deadline))
            schedulingManager.scheduleAlarm(
                CheckInAppProvider.getContext(),
                deadline
            )
        }
    }

    fun stopTracking() {
        viewModelScope.launch {
            val settings = repository.getSettings() ?: return@launch
            repository.updateSettings(settings.copy(isEnabled = false))
            schedulingManager.cancelAlarm(CheckInAppProvider.getContext())
            _state.update {
                it.copy(
                    isEnabled = false,
                    timeRemainingMillis = 0L,
                    formattedTimeRemaining = "00:00:00",
                    isDeadlineExpired = false
                )
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        tickJob?.cancel()
    }

    private fun formatTime(millis: Long): String {
        val totalSeconds = millis / 1000
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60
        return String.format("%02d:%02d:%02d", hours, minutes, seconds)
    }
}

internal object CheckInAppProvider {
    fun getContext(): android.content.Context {
        return com.checkin.app.CheckInApp.instance
    }
}
