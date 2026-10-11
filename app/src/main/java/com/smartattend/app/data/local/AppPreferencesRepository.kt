package com.smartattend.app.data.local

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.smartattend.app.core.model.StudentSettings
import com.smartattend.app.core.model.ThemePreference
import com.smartattend.app.domain.auth.AuthenticationSessionStore
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.smartAttendDataStore by preferencesDataStore(name = "smartattend_preferences")

class AppPreferencesRepository(private val context: Context) : AuthenticationSessionStore {
    private object Keys {
        val installationId = stringPreferencesKey("installation_id")
        val notifications = booleanPreferencesKey("notifications_enabled")
        val reminderMinutes = intPreferencesKey("reminder_lead_minutes")
        val theme = stringPreferencesKey("theme_preference")
        val seedVersion = intPreferencesKey("demo_seed_version")
        val demoMode = booleanPreferencesKey("authentication_demo_mode")
    }

    val settings: Flow<StudentSettings> = context.smartAttendDataStore.data.map { values ->
        StudentSettings(
            notificationsEnabled = values[Keys.notifications] ?: true,
            reminderLeadMinutes = values[Keys.reminderMinutes] ?: 30,
            theme = values[Keys.theme]?.let { runCatching { ThemePreference.valueOf(it) }.getOrNull() }
                ?: ThemePreference.SYSTEM,
        )
    }

    val seedVersion: Flow<Int> = context.smartAttendDataStore.data.map { it[Keys.seedVersion] ?: 0 }

    suspend fun installationId(): String {
        context.smartAttendDataStore.data.first()[Keys.installationId]?.let { return it }
        val generated = "SA-${UUID.randomUUID()}"
        var resolved = generated
        context.smartAttendDataStore.edit { values ->
            resolved = values[Keys.installationId] ?: generated
            values[Keys.installationId] = resolved
        }
        return resolved
    }

    suspend fun setNotificationsEnabled(enabled: Boolean) {
        context.smartAttendDataStore.edit { it[Keys.notifications] = enabled }
    }

    suspend fun setReminderLeadMinutes(minutes: Int) {
        require(minutes in 0..1440)
        context.smartAttendDataStore.edit { it[Keys.reminderMinutes] = minutes }
    }

    suspend fun setTheme(theme: ThemePreference) {
        context.smartAttendDataStore.edit { it[Keys.theme] = theme.name }
    }

    suspend fun setSeedVersion(version: Int) {
        context.smartAttendDataStore.edit { it[Keys.seedVersion] = version }
    }

    override suspend fun isDemoModeEnabled(): Boolean =
        context.smartAttendDataStore.data.first()[Keys.demoMode] ?: false

    override suspend fun setDemoModeEnabled(enabled: Boolean) {
        context.smartAttendDataStore.edit { values ->
            if (enabled) {
                values[Keys.demoMode] = true
            } else {
                values.remove(Keys.demoMode)
            }
        }
    }
}
