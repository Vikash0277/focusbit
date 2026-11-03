package com.focusflow.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "mood_entries")
data class MoodEntry(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val date: String, // yyyy-MM-dd format
    val mood: Int = 3, // 1-5 scale
    val energyLevel: Int = 3, // 1-5 scale
    val note: String = "",
    val timestampMs: Long = System.currentTimeMillis()
)
