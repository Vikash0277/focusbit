package com.focusflow.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.RocketLaunch
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

    // Stacked Screens
    data object Appearance : Screen("appearance", "Appearance & Themes", Icons.Filled.Palette)
    data object Notifications : Screen("notifications", "Notifications & Reminders", Icons.Filled.Notifications)
    data object SetGoal : Screen("set_goal", "Set / Edit Goal", Icons.Filled.RocketLaunch)
    data object AddTodo : Screen("add_todo", "New Task", Icons.Filled.Add)
    data object TodoDetail : Screen("todo_detail/{todoId}", "Task Details", Icons.Filled.Checklist) {
        fun createRoute(todoId: Long) = "todo_detail/$todoId"
    }
    data object AddNote : Screen("add_note", "New Note", Icons.Filled.EditNote)
    data object NoteDetail : Screen("note_detail/{noteId}", "Note Details", Icons.AutoMirrored.Filled.MenuBook) {
        fun createRoute(noteId: Long) = "note_detail/$noteId"
    }

    companion object {
        val bottomNavItems = listOf(Today, Shield, Scroll, Habits, Timer)
    }
}
