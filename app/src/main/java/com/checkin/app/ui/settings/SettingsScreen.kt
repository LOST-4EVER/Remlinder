package com.checkin.app.ui.settings

import android.content.Intent
import android.provider.ContactsContract
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.checkin.app.data.model.TriggerAction
import com.checkin.app.ui.theme.BorderColor
import com.checkin.app.ui.theme.CardBackground
import com.checkin.app.ui.theme.DarkBackground
import com.checkin.app.ui.theme.DarkSurface
import com.checkin.app.ui.theme.DarkSurfaceVariant
import com.checkin.app.ui.theme.PrimaryGreen
import com.checkin.app.ui.theme.TextPrimary
import com.checkin.app.ui.theme.TextSecondary
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    navController: NavController,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    val contactPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val data = result.data ?: return@rememberLauncherForActivityResult
        val contactUri = data.data ?: return@rememberLauncherForActivityResult
        val idProjection = arrayOf(
            ContactsContract.Contacts._ID,
            ContactsContract.Contacts.DISPLAY_NAME
        )
        context.contentResolver.query(contactUri, idProjection, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val contactId = cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts._ID))
                val name = cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME))
                viewModel.updateContactName(name.orEmpty())
                val phoneCursor = context.contentResolver.query(
                    ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                    null,
                    "${ContactsContract.CommonDataKinds.Phone.CONTACT_ID} = ?",
                    arrayOf(contactId),
                    null
                )
                phoneCursor?.use { pc ->
                    if (pc.moveToFirst()) {
                        val phone = pc.getString(
                            pc.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                        )
                        viewModel.updateContactPhone(phone.orEmpty())
                    }
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("Settings", color = TextPrimary)
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = TextPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DarkBackground
                )
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        containerColor = DarkBackground
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(Modifier.height(8.dp))

            EmergencyContactSection(
                contactName = state.contactName,
                contactPhone = state.contactPhone,
                onNameChange = viewModel::updateContactName,
                onPhoneChange = viewModel::updateContactPhone,
                onPickContact = {
                    val intent = Intent(Intent.ACTION_PICK, ContactsContract.Contacts.CONTENT_URI)
                    contactPickerLauncher.launch(intent)
                }
            )

            CheckInIntervalSection(
                intervalHours = state.intervalHours,
                onIntervalChange = viewModel::updateIntervalHours
            )

            ReminderIntervalSection(
                reminderMinutes = state.reminderIntervalMinutes,
                onReminderChange = viewModel::updateReminderInterval
            )

            EmergencyActionSection(
                triggerAction = state.triggerAction,
                smsMessage = state.smsMessage,
                onTriggerActionChange = viewModel::updateTriggerAction,
                onSmsMessageChange = viewModel::updateSmsMessage
            )

            Button(
                onClick = {
                    viewModel.saveSettings()
                    scope.launch {
                        snackbarHostState.showSnackbar("Settings saved!")
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                enabled = !state.isSaving,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = PrimaryGreen
                )
            ) {
                Text(
                    text = if (state.isSaving) "Saving..." else "Save Settings",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun EmergencyContactSection(
    contactName: String,
    contactPhone: String,
    onNameChange: (String) -> Unit,
    onPhoneChange: (String) -> Unit,
    onPickContact: () -> Unit
) {
    SectionCard(title = "Emergency Contact") {
        OutlinedTextField(
            value = contactName,
            onValueChange = onNameChange,
            label = { Text("Name") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            colors = outlinedFieldColors()
        )

        Spacer(Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = contactPhone,
                onValueChange = onPhoneChange,
                label = { Text("Phone Number") },
                modifier = Modifier.weight(1f),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                colors = outlinedFieldColors()
            )
            OutlinedButton(
                onClick = onPickContact,
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = PrimaryGreen
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = "Pick Contact",
                    modifier = Modifier.padding(end = 4.dp)
                )
                Text("Pick")
            }
        }
    }
}

@Composable
private fun CheckInIntervalSection(
    intervalHours: Int,
    onIntervalChange: (Int) -> Unit
) {
    SectionCard(title = "Check-In Interval") {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf(6, 12, 24, 48).forEach { hours ->
                IntervalChip(
                    label = "${hours}h",
                    selected = intervalHours == hours,
                    onClick = { onIntervalChange(hours) },
                    modifier = Modifier.weight(1f)
                )
            }
            IntervalChip(
                label = "Custom",
                selected = !listOf(6, 12, 24, 48).contains(intervalHours),
                onClick = { onIntervalChange(72) },
                modifier = Modifier.weight(1f)
            )
        }

        if (!listOf(6, 12, 24, 48).contains(intervalHours)) {
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = intervalHours.toString(),
                onValueChange = { value ->
                    value.toIntOrNull()?.let { onIntervalChange(it.coerceIn(1, 720)) }
                },
                label = { Text("Custom hours") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                colors = outlinedFieldColors()
            )
        }
    }
}

@Composable
private fun ReminderIntervalSection(
    reminderMinutes: Int,
    onReminderChange: (Int) -> Unit
) {
    SectionCard(title = "Reminder Interval") {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf(5, 10, 15, 30, 60).forEach { minutes ->
                IntervalChip(
                    label = when (minutes) {
                        60 -> "1h"
                        else -> "${minutes}m"
                    },
                    selected = reminderMinutes == minutes,
                    onClick = { onReminderChange(minutes) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun EmergencyActionSection(
    triggerAction: TriggerAction,
    smsMessage: String,
    onTriggerActionChange: (TriggerAction) -> Unit,
    onSmsMessageChange: (String) -> Unit
) {
    SectionCard(title = "Emergency Action") {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
                .clickable { onTriggerActionChange(TriggerAction.SMS) }
        ) {
            RadioButton(
                selected = triggerAction == TriggerAction.SMS,
                onClick = { onTriggerActionChange(TriggerAction.SMS) },
                colors = RadioButtonDefaults.colors(
                    selectedColor = PrimaryGreen,
                    unselectedColor = TextSecondary
                )
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = "Send SMS",
                style = MaterialTheme.typography.bodyLarge,
                color = TextPrimary
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
                .clickable { onTriggerActionChange(TriggerAction.PHONE_CALL) }
        ) {
            RadioButton(
                selected = triggerAction == TriggerAction.PHONE_CALL,
                onClick = { onTriggerActionChange(TriggerAction.PHONE_CALL) },
                colors = RadioButtonDefaults.colors(
                    selectedColor = PrimaryGreen,
                    unselectedColor = TextSecondary
                )
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = "Initiate Phone Call",
                style = MaterialTheme.typography.bodyLarge,
                color = TextPrimary
            )
        }

        if (triggerAction == TriggerAction.SMS) {
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = smsMessage,
                onValueChange = onSmsMessageChange,
                label = { Text("SMS Message") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
                minLines = 3,
                colors = outlinedFieldColors()
            )
        }
    }
}

@Composable
private fun SectionCard(
    title: String,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(12.dp))
            content()
        }
    }
}

@Composable
private fun IntervalChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = if (selected) PrimaryGreen else TextSecondary
            )
        },
        modifier = modifier,
        colors = FilterChipDefaults.filterChipColors(
            containerColor = DarkSurfaceVariant,
            selectedContainerColor = PrimaryGreen.copy(alpha = 0.15f)
        ),
        border = FilterChipDefaults.filterChipBorder(
            borderColor = BorderColor,
            selectedBorderColor = PrimaryGreen,
            enabled = true,
            selected = selected
        )
    )
}

@Composable
private fun outlinedFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = TextPrimary,
    unfocusedTextColor = TextPrimary,
    cursorColor = PrimaryGreen,
    focusedBorderColor = PrimaryGreen,
    unfocusedBorderColor = BorderColor,
    focusedLabelColor = PrimaryGreen,
    unfocusedLabelColor = TextSecondary,
    focusedContainerColor = DarkSurface,
    unfocusedContainerColor = DarkSurface
)

