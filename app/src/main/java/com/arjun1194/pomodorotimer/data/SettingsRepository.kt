package com.arjun1194.pomodorotimer.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "pomodoro_settings")

class SettingsRepository(private val context: Context) {
    companion object {
        val FOCUS_DURATION_KEY = intPreferencesKey("focus_duration")
        val BREAK_DURATION_KEY = intPreferencesKey("break_duration")
        // Default durations in minutes
        const val DEFAULT_FOCUS_DURATION = 25
        const val DEFAULT_BREAK_DURATION = 5
    }

    val focusDuration: Flow<Int> = context.dataStore.data
        .map { preferences ->
            preferences[FOCUS_DURATION_KEY] ?: DEFAULT_FOCUS_DURATION
        }

    val breakDuration: Flow<Int> = context.dataStore.data
        .map { preferences ->
            preferences[BREAK_DURATION_KEY] ?: DEFAULT_BREAK_DURATION
        }

    suspend fun saveFocusDuration(duration: Int) {
        context.dataStore.edit { preferences ->
            preferences[FOCUS_DURATION_KEY] = duration
        }
    }

    suspend fun saveBreakDuration(duration: Int) {
        context.dataStore.edit { preferences ->
            preferences[BREAK_DURATION_KEY] = duration
        }
    }
}
