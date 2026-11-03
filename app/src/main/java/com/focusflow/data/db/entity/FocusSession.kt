package com.focusflow.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "focus_sessions")
data class FocusSession(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val goalId: Long = 0,
    val goalTitle: String = "",
    val startTimeMs: Long = 0,
    val endTimeMs: Long = 0,
    val durationMs: Long = 0,
    val targetDurationMs: Long = 0,
    val status: String = "running" // running, paused, completed
)
