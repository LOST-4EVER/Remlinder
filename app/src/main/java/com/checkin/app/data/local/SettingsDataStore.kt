package com.checkin.app.data.local

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "check_in_preferences")

class SettingsDataStore(private val context: Context) {

    private object Keys {
        val IS_FIRST_LAUNCH = booleanPreferencesKey("is_first_launch")
        val LAST_KNOWN_LATITUDE = doublePreferencesKey("last_known_latitude")
        val LAST_KNOWN_LONGITUDE = doublePreferencesKey("last_known_longitude")
    }

    val isFirstLaunch: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[Keys.IS_FIRST_LAUNCH] ?: true
    }

    val lastKnownLatitude: Flow<Double> = context.dataStore.data.map { preferences ->
        preferences[Keys.LAST_KNOWN_LATITUDE] ?: 0.0
    }

    val lastKnownLongitude: Flow<Double> = context.dataStore.data.map { preferences ->
        preferences[Keys.LAST_KNOWN_LONGITUDE] ?: 0.0
    }

    suspend fun setFirstLaunch(isFirst: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[Keys.IS_FIRST_LAUNCH] = isFirst
        }
    }

    suspend fun setLatitude(lat: Double) {
        context.dataStore.edit { preferences ->
            preferences[Keys.LAST_KNOWN_LATITUDE] = lat
        }
    }

    suspend fun setLongitude(lng: Double) {
        context.dataStore.edit { preferences ->
            preferences[Keys.LAST_KNOWN_LONGITUDE] = lng
        }
    }
}
