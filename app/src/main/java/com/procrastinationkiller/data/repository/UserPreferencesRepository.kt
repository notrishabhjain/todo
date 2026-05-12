package com.procrastinationkiller.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.procrastinationkiller.domain.engine.ReminderFrequency
import com.procrastinationkiller.domain.model.ReminderMode
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_preferences")

@Singleton
class UserPreferencesRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private object PreferencesKeys {
        val REMINDER_MODE = stringPreferencesKey("reminder_mode")
        val REMINDER_FREQUENCY = stringPreferencesKey("reminder_frequency")
        val MONITORED_APPS = stringSetPreferencesKey("monitored_apps")
        val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
        val THEME_DARK_MODE = booleanPreferencesKey("theme_dark_mode")
    }

    val reminderMode: Flow<ReminderMode> = context.dataStore.data.map { preferences ->
        val modeStr = preferences[PreferencesKeys.REMINDER_MODE] ?: ReminderMode.NORMAL.name
        try {
            ReminderMode.valueOf(modeStr)
        } catch (_: IllegalArgumentException) {
            ReminderMode.NORMAL
        }
    }

    val reminderFrequency: Flow<ReminderFrequency> = context.dataStore.data.map { preferences ->
        val freqStr = preferences[PreferencesKeys.REMINDER_FREQUENCY] ?: ReminderFrequency.EVERY_30_MIN.name
        try {
            ReminderFrequency.valueOf(freqStr)
        } catch (_: IllegalArgumentException) {
            ReminderFrequency.EVERY_30_MIN
        }
    }

    val monitoredApps: Flow<Set<String>> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.MONITORED_APPS] ?: setOf(
            "com.whatsapp",
            "org.telegram.messenger",
            "com.slack",
            "com.google.android.gm"
        )
    }

    val onboardingCompleted: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.ONBOARDING_COMPLETED] ?: false
    }

    val isDarkMode: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.THEME_DARK_MODE] ?: true
    }

    suspend fun setReminderMode(mode: ReminderMode) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.REMINDER_MODE] = mode.name
        }
    }

    suspend fun setReminderFrequency(frequency: ReminderFrequency) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.REMINDER_FREQUENCY] = frequency.name
        }
    }

    suspend fun setMonitoredApps(apps: Set<String>) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.MONITORED_APPS] = apps
        }
    }

    suspend fun setOnboardingCompleted(completed: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.ONBOARDING_COMPLETED] = completed
        }
    }

    suspend fun setDarkMode(isDark: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.THEME_DARK_MODE] = isDark
        }
    }
}
