package com.focusflow.ui.theme

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.themeDataStore: DataStore<Preferences> by preferencesDataStore(name = "focusflow_theme_datastore")

class ThemePreferences(private val context: Context) {

    private val themeKey = stringPreferencesKey("selected_app_theme")
    private val legacyPrefs = context.getSharedPreferences("focusflow_theme_prefs", Context.MODE_PRIVATE)

    val themeFlow: Flow<AppTheme> = context.themeDataStore.data.map { preferences ->
        val savedId = preferences[themeKey] ?: legacyPrefs.getString("selected_app_theme_id", AppTheme.LAVENDER_DREAM.id)
        AppTheme.fromId(savedId)
    }

    fun getInitialTheme(): AppTheme {
        val savedId = legacyPrefs.getString("selected_app_theme_id", null)
        return if (savedId != null) {
            AppTheme.fromId(savedId)
        } else {
            // Check legacy is_dark_theme if present
            if (legacyPrefs.contains("is_dark_theme") && legacyPrefs.getBoolean("is_dark_theme", false)) {
                AppTheme.MIDNIGHT
            } else {
                AppTheme.LAVENDER_DREAM
            }
        }
    }

    suspend fun setAppTheme(theme: AppTheme) {
        // Fast synchronous write for zero-flicker cold starts
        legacyPrefs.edit()
            .putString("selected_app_theme_id", theme.id)
            .putBoolean("is_dark_theme", theme == AppTheme.MIDNIGHT)
            .apply()

        // Robust asynchronous DataStore write
        context.themeDataStore.edit { preferences ->
            preferences[themeKey] = theme.id
        }
    }
}

