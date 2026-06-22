package com.checkin.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.checkin.app.data.local.entity.CheckInSettingsEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CheckInSettingsDao {

    @Query("SELECT * FROM check_in_settings WHERE id = 1")
    fun getSettings(): Flow<CheckInSettingsEntity?>

    @Query("SELECT * FROM check_in_settings WHERE id = 1")
    suspend fun getSettingsSync(): CheckInSettingsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(settings: CheckInSettingsEntity)

    @Query("UPDATE check_in_settings SET next_deadline_time = :nextDeadline WHERE id = 1")
    suspend fun updateDeadline(nextDeadline: Long)

    @Query("UPDATE check_in_settings SET last_check_in_time = :time WHERE id = 1")
    suspend fun updateLastCheckIn(time: Long)

    @Query("UPDATE check_in_settings SET is_enabled = :isEnabled WHERE id = 1")
    suspend fun toggleEnabled(isEnabled: Boolean)
}
