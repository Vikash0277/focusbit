package com.focusflow.ui.theme

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import kotlinx.coroutines.flow.StateFlow

class ThemeViewModel : ViewModel() {

    val currentTheme: StateFlow<AppTheme> = ThemeManager.currentTheme

    fun selectTheme(theme: AppTheme) {
        ThemeManager.setTheme(theme)
    }

    class Factory : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return ThemeViewModel() as T
        }
    }
}

