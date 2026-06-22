package com.checkin.app.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "check_in_settings")
data class CheckInSettingsEntity(
    @PrimaryKey
    val id: Int = 1,
    @ColumnInfo(name = "contact_name")
    val contactName: String = "",
    @ColumnInfo(name = "contact_phone")
    val contactPhone: String = "",
    @ColumnInfo(name = "interval_hours")
    val intervalHours: Int = 24,
    @ColumnInfo(name = "reminder_interval_minutes")
    val reminderIntervalMinutes: Int = 30,
    @ColumnInfo(name = "trigger_action")
    val triggerAction: String = "SMS",
    @ColumnInfo(name = "sms_message")
    val smsMessage: String = "Emergency: I have not checked in. Please assist.",
    @ColumnInfo(name = "is_enabled")
    val isEnabled: Boolean = false,
    @ColumnInfo(name = "last_check_in_time")
    val lastCheckInTime: Long = 0L,
    @ColumnInfo(name = "next_deadline_time")
    val nextDeadlineTime: Long = 0L
)
