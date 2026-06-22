package com.checkin.app.di

import android.content.Context
import androidx.room.Room
import com.checkin.app.data.local.CheckInDatabase
import com.checkin.app.data.local.SettingsDataStore
import com.checkin.app.data.local.dao.CheckInSettingsDao
import com.checkin.app.data.repository.CheckInRepository
import com.checkin.app.domain.SchedulingManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): CheckInDatabase {
        return Room.databaseBuilder(
            context.applicationContext,
            CheckInDatabase::class.java,
            "check_in_database"
        ).build()
    }

    @Provides
    @Singleton
    fun provideSettingsDao(database: CheckInDatabase): CheckInSettingsDao {
        return database.checkInSettingsDao()
    }

    @Provides
    @Singleton
    fun provideSettingsDataStore(@ApplicationContext context: Context): SettingsDataStore {
        return SettingsDataStore(context)
    }

    @Provides
    @Singleton
    fun provideRepository(
        dao: CheckInSettingsDao,
        dataStore: SettingsDataStore
    ): CheckInRepository {
        return CheckInRepository(dao, dataStore)
    }

    @Provides
    @Singleton
    fun provideSchedulingManager(): SchedulingManager {
        return SchedulingManager()
    }
}
