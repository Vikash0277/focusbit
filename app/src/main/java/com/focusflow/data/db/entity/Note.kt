package com.focusflow.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "notes")
data class Note(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val content: String = "",
    val emoji: String = "📝",
    val category: String = "Productive", // "High Priority", "Productive", "Ideas", "Personal", "Work"
    val colorTag: String = "#8B5CF6", // Hex string
    val date: String = "", // e.g. "03 Nov 2026"
    val audioPath: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

