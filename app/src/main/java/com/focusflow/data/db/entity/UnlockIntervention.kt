package com.focusflow.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "unlock_interventions")
data class UnlockIntervention(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val packageName: String,
    val appName: String,
    val reason: String, // "Just checking", "Taking a break", "Important message", "Work-related", "Other"
    val customNote: String = "",
    val outcome: String, // "UNLOCKED" or "RESISTED"
    val costCoins: Int = 0,
    val durationMinutes: Int = 0,
    val timestamp: Long = System.currentTimeMillis()
)
