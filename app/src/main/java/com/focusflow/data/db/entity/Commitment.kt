package com.focusflow.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "commitments")
data class Commitment(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val goalId: Long = 0,
    val title: String,
    val startTime: String = "",
    val endTime: String = "",
    val duration: String = "",
    val label: String = "",
    val date: String = "", // yyyy-MM-dd
    val status: String = "upcoming" // completed, upcoming, active
)
