package com.focusflow.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "blocked_apps")
data class BlockedApp(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val appName: String,
    val packageName: String = "",
    val category: String = "",
    val iconName: String = "block",
    val isBlocked: Boolean = true,
    val startTime: String = "09:00",
    val endTime: String = "22:00",
    val isStrictLock: Boolean = true,
    val lockDurationMinutes: Int = 15,
    val lockedUntilMillis: Long = 0L
)
