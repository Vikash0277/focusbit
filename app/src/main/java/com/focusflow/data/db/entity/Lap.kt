package com.focusflow.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "laps")
data class Lap(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val sessionId: Long,
    val lapNumber: Int,
    val label: String = "",
    val splitTimeMs: Long = 0,
    val overallTimeMs: Long = 0,
    val note: String = ""
)
