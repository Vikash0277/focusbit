package com.focusflow.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "block_schedules")
data class BlockSchedule(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val iconName: String = "bedtime",
    val startTime: String = "18:00",
    val endTime: String = "21:00",
    val daysOfWeek: String = "Mon,Tue,Wed,Thu,Fri", // CSV
    val appsCount: Int = 0,
    val isAutomatic: Boolean = true
)
