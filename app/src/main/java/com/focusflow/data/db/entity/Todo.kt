package com.focusflow.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "todos")
data class Todo(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val description: String = "",
    val isCompleted: Boolean = false,
    val priority: String = "Medium", // "High", "Medium", "Low"
    val dueTime: String = "", // e.g. "14:00" or "02:00 PM"
    val category: String = "General", // "Work", "Study", "Personal", "Health"
    val createdAt: Long = System.currentTimeMillis(),
    val completedAt: Long? = null
)
