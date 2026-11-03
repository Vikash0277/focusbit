package com.focusflow.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "goals")
data class Goal(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val tag: String = "",
    // Goal type: "HOURS" (track cumulative hours) or "DAYS" (track completed days)
    val goalType: String = "HOURS",
    val targetHours: Float = 0f,
    val loggedHours: Float = 0f,
    // Days-based goal fields
    val targetDays: Int = 0,
    val loggedDays: Int = 0,
    val dailyTarget: Float = 2f,
    val icon: String = "rocket_launch",
    val status: String = "on_track", // on_track, at_risk, completed
    val reminderEnabled: Boolean = true,
    val reminderTime: String = "09:00"
)
