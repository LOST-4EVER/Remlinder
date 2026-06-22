package com.checkin.app.data.repository

import com.checkin.app.data.local.SettingsDataStore
import com.checkin.app.data.local.dao.CheckInSettingsDao
import com.checkin.app.data.local.entity.CheckInSettingsEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit

class CheckInRepository(
    private val dao: CheckInSettingsDao,
    private val dataStore: SettingsDataStore
) {

    val settingsFlow: Flow<CheckInSettingsEntity?> = dao.getSettings()

    suspend fun updateSettings(settings: CheckInSettingsEntity) {
        dao.insertOrUpdate(settings)
    }

    suspend fun checkIn() {
        val now = System.currentTimeMillis()
        val settings = dao.getSettingsSync() ?: return
        val updatedSettings = settings.copy(
            lastCheckInTime = now,
            nextDeadlineTime = now + TimeUnit.HOURS.toMillis(settings.intervalHours.toLong()),
            isEnabled = true
        )
        dao.insertOrUpdate(updatedSettings)
    }

    suspend fun getSettings(): CheckInSettingsEntity? {
        return dao.getSettingsSync()
    }

    suspend fun isDeadlineExpired(): Boolean {
        val settings = dao.getSettingsSync() ?: return false
        return System.currentTimeMillis() > settings.nextDeadlineTime
    }

    fun getLatitude(): Flow<Double> = dataStore.lastKnownLatitude

    fun getLongitude(): Flow<Double> = dataStore.lastKnownLongitude

    suspend fun saveLocation(lat: Double, lng: Double) {
        dataStore.setLatitude(lat)
        dataStore.setLongitude(lng)
    }

    suspend fun isFirstLaunch(): Boolean {
        return dataStore.isFirstLaunch.first()
    }

    suspend fun setFirstLaunchComplete() {
        dataStore.setFirstLaunch(false)
    }
}
