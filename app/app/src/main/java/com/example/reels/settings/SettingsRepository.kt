package com.example.reels.settings

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private const val DATASTORE_NAME = "app_settings"

val Context.dataStore by preferencesDataStore(name = DATASTORE_NAME)

private object Keys {
    val FOLDER_NAME = stringPreferencesKey("folder_name")
    val CHILD_MODE = booleanPreferencesKey("child_mode")
    val WHITELIST_ENABLED = booleanPreferencesKey("whitelist_enabled")
    val MAX_SESSION_TIME = intPreferencesKey("max_session_time")
    val SESSION_TIME_ENABLED = booleanPreferencesKey("session_time_enabled")
    val SETTINGS_PROTECTED = booleanPreferencesKey("settings_protected")
}

data class AppSettings(
    val folderName: String = "ShortsVideos",
    val childMode: Boolean = true,
    val whitelistEnabled: Boolean = true,
    val maxSessionTime: Int = 30, // Default 30 minutes
    val sessionTimeEnabled: Boolean = false,
    val settingsProtected: Boolean = false
)

class SettingsRepository(private val context: Context) {

    val settingsFlow: Flow<AppSettings> = context.dataStore.data.map { prefs: Preferences ->
        AppSettings(
            folderName = prefs[Keys.FOLDER_NAME] ?: "ShortsVideos",
            childMode = prefs[Keys.CHILD_MODE] ?: true,
            whitelistEnabled = prefs[Keys.WHITELIST_ENABLED] ?: true,
            maxSessionTime = prefs[Keys.MAX_SESSION_TIME] ?: 30,
            sessionTimeEnabled = prefs[Keys.SESSION_TIME_ENABLED] ?: false,
            settingsProtected = prefs[Keys.SETTINGS_PROTECTED] ?: false
        )
    }

    suspend fun updateFolderName(name: String) {
        context.dataStore.edit { it[Keys.FOLDER_NAME] = name }
    }

    suspend fun setChildMode(enabled: Boolean) {
        context.dataStore.edit { it[Keys.CHILD_MODE] = enabled }
    }

    suspend fun setWhitelistEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.WHITELIST_ENABLED] = enabled }
    }
    
    suspend fun setMaxSessionTime(minutes: Int) {
        context.dataStore.edit { it[Keys.MAX_SESSION_TIME] = minutes }
    }
    
    suspend fun setSessionTimeEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.SESSION_TIME_ENABLED] = enabled }
    }
    
    suspend fun setSettingsProtected(protected: Boolean) {
        context.dataStore.edit { it[Keys.SETTINGS_PROTECTED] = protected }
    }
}
