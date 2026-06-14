package com.remlinder.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class MediaType { TEXT, AUDIO, IMAGE, TASK }

@Entity(tableName = "reminders")
data class ReminderEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val description: String? = null,
    val mediaType: MediaType = MediaType.TEXT,
    val mediaUri: String? = null,
    val triggerAtMillis: Long,
    val durationSeconds: Int = 0,
    val isSnoozed: Boolean = false,
    val snoozeCount: Int = 0,
    val maxSnoozes: Int = 3,
    val isCompleted: Boolean = false,
    val nextTriggerAtMillis: Long? = null,
    val createdAt: Long = System.currentTimeMillis()
)
