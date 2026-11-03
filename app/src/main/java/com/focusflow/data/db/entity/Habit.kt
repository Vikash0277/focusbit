package com.focusflow.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "habits")
data class Habit(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val time: String = "",
    val durationMinutes: Int = 0,
    val category: String = "General",
    val iconName: String = "check_circle",
    val isActive: Boolean = true
)
