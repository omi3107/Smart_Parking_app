package com.example.parkkar.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_preferences")

class UserPreferencesRepository private constructor(context: Context) {

    private val appContext = context.applicationContext

    private object PreferencesKeys {
        val USER_NAME = stringPreferencesKey("user_name")
        val USER_EMAIL = stringPreferencesKey("user_email")
        val USER_PHONE = stringPreferencesKey("user_phone")
        val IS_DARK_THEME = booleanPreferencesKey("is_dark_theme")
        val NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
        val IS_GOOGLE_SIGN_IN = booleanPreferencesKey("is_google_sign_in")
    }

    val isDarkTheme: Flow<Boolean> = appContext.dataStore.data
        .map {
            it[PreferencesKeys.IS_DARK_THEME] ?: false
        }

    suspend fun setDarkTheme(isDarkTheme: Boolean) {
        appContext.dataStore.edit {
            it[PreferencesKeys.IS_DARK_THEME] = isDarkTheme
        }
    }

    val notificationsEnabled: Flow<Boolean> = appContext.dataStore.data
        .map {
            it[PreferencesKeys.NOTIFICATIONS_ENABLED] ?: true
        }

    suspend fun setNotificationsEnabled(enabled: Boolean) {
        appContext.dataStore.edit {
            it[PreferencesKeys.NOTIFICATIONS_ENABLED] = enabled
        }
    }

    val userName: Flow<String> = appContext.dataStore.data
        .map {
            it[PreferencesKeys.USER_NAME] ?: ""
        }

    suspend fun setUserName(name: String) {
        appContext.dataStore.edit {
            it[PreferencesKeys.USER_NAME] = name
        }
    }

    val userEmail: Flow<String> = appContext.dataStore.data
        .map {
            it[PreferencesKeys.USER_EMAIL] ?: ""
        }

    suspend fun setUserEmail(email: String) {
        appContext.dataStore.edit {
            it[PreferencesKeys.USER_EMAIL] = email
        }
    }

    val userPhone: Flow<String> = appContext.dataStore.data
        .map {
            it[PreferencesKeys.USER_PHONE] ?: ""
        }

    suspend fun setUserPhone(phone: String) {
        appContext.dataStore.edit {
            it[PreferencesKeys.USER_PHONE] = phone
        }
    }

    val isGoogleSignIn: Flow<Boolean> = appContext.dataStore.data
        .map {
            it[PreferencesKeys.IS_GOOGLE_SIGN_IN] ?: false
        }

    suspend fun setGoogleSignIn(isGoogleSignIn: Boolean) {
        appContext.dataStore.edit {
            it[PreferencesKeys.IS_GOOGLE_SIGN_IN] = isGoogleSignIn
        }
    }

    companion object {
        @Volatile
        private var INSTANCE: UserPreferencesRepository? = null

        fun getInstance(context: Context): UserPreferencesRepository {
            return INSTANCE ?: synchronized(this) {
                val instance = UserPreferencesRepository(context)
                INSTANCE = instance
                instance
            }
        }
    }
}