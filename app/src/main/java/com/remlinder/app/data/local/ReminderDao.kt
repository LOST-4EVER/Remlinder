package com.remlinder.app.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ReminderDao {

    @Query("SELECT * FROM reminders ORDER BY triggerAtMillis ASC")
    fun getAllReminders(): Flow<List<ReminderEntity>>

    @Query("SELECT * FROM reminders WHERE isCompleted = 0 ORDER BY triggerAtMillis ASC")
    fun getActiveReminders(): Flow<List<ReminderEntity>>

    @Query("SELECT * FROM reminders WHERE id = :id")
    suspend fun getReminderById(id: Long): ReminderEntity?

    @Query("SELECT * FROM reminders WHERE isCompleted = 0 AND triggerAtMillis BETWEEN :start AND :end ORDER BY triggerAtMillis ASC")
    suspend fun getRemindersBetween(start: Long, end: Long): List<ReminderEntity>

    @Query("SELECT * FROM reminders WHERE isCompleted = 0 AND isSnoozed = 0 AND triggerAtMillis <= :now ORDER BY triggerAtMillis ASC")
    suspend fun getDueReminders(now: Long): List<ReminderEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(reminder: ReminderEntity): Long

    @Update
    suspend fun update(reminder: ReminderEntity)

    @Delete
    suspend fun delete(reminder: ReminderEntity)

    @Query("DELETE FROM reminders WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("UPDATE reminders SET isCompleted = 1 WHERE id = :id")
    suspend fun markCompleted(id: Long)

    @Query("UPDATE reminders SET isSnoozed = :isSnoozed, snoozeCount = snoozeCount + 1, nextTriggerAtMillis = :nextTrigger WHERE id = :id")
    suspend fun snooze(id: Long, isSnoozed: Boolean, nextTrigger: Long)

    @Query("UPDATE reminders SET isSnoozed = 0, nextTriggerAtMillis = NULL WHERE id = :id")
    suspend fun resetSnooze(id: Long)

    @Query("DELETE FROM reminders WHERE isCompleted = 1 AND triggerAtMillis < :before")
    suspend fun cleanOldCompleted(before: Long)

    @Query("SELECT COUNT(*) FROM reminders WHERE isCompleted = 0")
    fun countActive(): Flow<Int>
}
