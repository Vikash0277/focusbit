package com.focusflow.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "coin_transactions")
data class CoinTransaction(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val amount: Long, // Positive for earned, negative for spent
    val type: String, // "EARNED", "SPENT", "BONUS", "REFUND"
    val source: String, // e.g. "Completed Todo: Master Kotlin", "Focus Session 25m"
    val createdAt: Long = System.currentTimeMillis(),
    val idempotencyKey: String? = null // Prevents duplicate rewards for same event
) {
    val timestamp: Long get() = createdAt
    val description: String get() = source
}
