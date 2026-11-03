package com.focusflow.ui.theme

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

object ThemeManager {

    private lateinit var preferences: ThemePreferences
    private val scope = CoroutineScope(Dispatchers.Main.immediate)

    private val _currentTheme = MutableStateFlow(AppTheme.LAVENDER_DREAM)
    val currentTheme: StateFlow<AppTheme> = _currentTheme.asStateFlow()

    fun init(context: Context) {
        preferences = ThemePreferences(context.applicationContext)
        _currentTheme.value = preferences.getInitialTheme()

        scope.launch {
            preferences.themeFlow.collect { theme ->
                _currentTheme.value = theme
            }
        }
    }

    fun setTheme(theme: AppTheme) {
        _currentTheme.value = theme
        scope.launch(Dispatchers.IO) {
            preferences.setAppTheme(theme)
        }
    }

    fun getColorSchemeForTheme(theme: AppTheme, isSystemInDarkTheme: Boolean): AppColorScheme {
        return when (theme) {
            AppTheme.LAVENDER_DREAM -> LavenderDreamColors
            AppTheme.MIDNIGHT -> MidnightColors
            AppTheme.OCEAN_BREEZE -> OceanBreezeColors
            AppTheme.FOREST_CALM -> ForestCalmColors
            AppTheme.SYSTEM_DEFAULT -> if (isSystemInDarkTheme) MidnightColors else LavenderDreamColors
        }
    }
}

