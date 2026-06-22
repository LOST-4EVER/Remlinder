package com.checkin.app.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.checkin.app.data.local.entity.CheckInSettingsEntity
import com.checkin.app.data.model.TriggerAction
import com.checkin.app.data.repository.CheckInRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val contactName: String = "",
    val contactPhone: String = "",
    val intervalHours: Int = 24,
    val reminderIntervalMinutes: Int = 30,
    val triggerAction: TriggerAction = TriggerAction.SMS,
    val smsMessage: String = "Emergency: I have not checked in. Please assist.",
    val isSaving: Boolean = false
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val repository: CheckInRepository
) : ViewModel() {

    private val _state = MutableStateFlow(SettingsUiState())
    val state: StateFlow<SettingsUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            repository.settingsFlow.collect { settings ->
                settings?.let { s ->
                    _state.update {
                        it.copy(
                            contactName = s.contactName,
                            contactPhone = s.contactPhone,
                            intervalHours = s.intervalHours,
                            reminderIntervalMinutes = s.reminderIntervalMinutes,
                            triggerAction = try {
                                TriggerAction.valueOf(s.triggerAction)
                            } catch (_: IllegalArgumentException) {
                                TriggerAction.SMS
                            },
                            smsMessage = s.smsMessage
                        )
                    }
                }
            }
        }
    }

    fun updateContactName(name: String) {
        _state.update { it.copy(contactName = name) }
    }

    fun updateContactPhone(phone: String) {
        _state.update { it.copy(contactPhone = phone) }
    }

    fun updateIntervalHours(hours: Int) {
        _state.update { it.copy(intervalHours = hours) }
    }

    fun updateReminderInterval(minutes: Int) {
        _state.update { it.copy(reminderIntervalMinutes = minutes) }
    }

    fun updateTriggerAction(action: TriggerAction) {
        _state.update { it.copy(triggerAction = action) }
    }

    fun updateSmsMessage(message: String) {
        _state.update { it.copy(smsMessage = message) }
    }

    fun saveSettings() {
        viewModelScope.launch {
            _state.update { it.copy(isSaving = true) }
            val current = _state.value
            val settings = repository.getSettings() ?: CheckInSettingsEntity()
            repository.updateSettings(
                settings.copy(
                    contactName = current.contactName,
                    contactPhone = current.contactPhone,
                    intervalHours = current.intervalHours,
                    reminderIntervalMinutes = current.reminderIntervalMinutes,
                    triggerAction = current.triggerAction.name,
                    smsMessage = current.smsMessage
                )
            )
            _state.update { it.copy(isSaving = false) }
        }
    }
}
