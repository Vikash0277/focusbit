package com.focusflow.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(
    val route: String,
    val title: String,
    val icon: ImageVector
) {
    data object Today : Screen("today", "Today", Icons.Filled.CalendarToday)
    data object Shield : Screen("shield", "Shield", Icons.Filled.Shield)
    data object Scroll : Screen("scroll", "Scrolls", Icons.Filled.SwapVert)
    data object Habits : Screen("habits", "Habits", Icons.Outlined.AutoAwesome)
    data object Timer : Screen("timer", "Focus", Icons.Filled.Timer)
    data object AppUsage : Screen("app_usage", "Phone Usage", Icons.Filled.CalendarToday)

    companion object {
        val bottomNavItems = listOf(Today, Shield, Scroll, Habits, Timer)
    }
}
