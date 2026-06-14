package com.remlinder.app.data.repository

import com.remlinder.app.data.local.MediaType
import com.remlinder.app.data.local.ReminderDao
import com.remlinder.app.data.local.ReminderEntity
import kotlinx.coroutines.flow.Flow

class ReminderRepository(private val dao: ReminderDao) {

    val allReminders: Flow<List<ReminderEntity>> = dao.getAllReminders()
    val activeReminders: Flow<List<ReminderEntity>> = dao.getActiveReminders()
    val activeCount: Flow<Int> = dao.countActive()

    suspend fun getById(id: Long): ReminderEntity? = dao.getReminderById(id)

    suspend fun getRemindersBetween(start: Long, end: Long): List<ReminderEntity> =
        dao.getRemindersBetween(start, end)

    suspend fun getDueReminders(now: Long): List<ReminderEntity> =
        dao.getDueReminders(now)

    suspend fun insert(
        title: String,
        description: String?,
        mediaType: MediaType,
        mediaUri: String?,
        triggerAtMillis: Long,
        durationSeconds: Int = 0
    ): Long {
        val reminder = ReminderEntity(
            title = title,
            description = description,
            mediaType = mediaType,
            mediaUri = mediaUri,
            triggerAtMillis = triggerAtMillis,
            durationSeconds = durationSeconds
        )
        return dao.insert(reminder)
    }

    suspend fun update(reminder: ReminderEntity) = dao.update(reminder)

    suspend fun delete(id: Long) = dao.deleteById(id)

    suspend fun markCompleted(id: Long) = dao.markCompleted(id)

    suspend fun snooze(id: Long, nextTrigger: Long) {
        dao.snooze(id, true, nextTrigger)
    }

    suspend fun cleanOldCompleted() {
        val weekAgo = System.currentTimeMillis() - 7 * 24 * 60 * 60 * 1000
        dao.cleanOldCompleted(weekAgo)
    }
}
