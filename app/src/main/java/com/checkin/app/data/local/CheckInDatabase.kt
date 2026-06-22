package com.checkin.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.checkin.app.data.local.dao.CheckInSettingsDao
import com.checkin.app.data.local.entity.CheckInSettingsEntity

@Database(
    entities = [CheckInSettingsEntity::class],
    version = 1,
    exportSchema = false
)
abstract class CheckInDatabase : RoomDatabase() {

    abstract fun checkInSettingsDao(): CheckInSettingsDao

    companion object {
        @Volatile
        private var INSTANCE: CheckInDatabase? = null

        fun getInstance(context: Context): CheckInDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    CheckInDatabase::class.java,
                    "check_in_database"
                ).build().also { INSTANCE = it }
            }
        }
    }
}
