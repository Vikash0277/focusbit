package com.focusflow.data.db.dao

import androidx.room.*
import com.focusflow.data.db.entity.CoinTransaction
import kotlinx.coroutines.flow.Flow

@Dao
interface CoinDao {
    @Query("SELECT * FROM coin_transactions ORDER BY createdAt DESC")
    fun getAllTransactions(): Flow<List<CoinTransaction>>

    @Query("SELECT COALESCE(SUM(amount), 0) FROM coin_transactions")
    fun getTotalBalance(): Flow<Long>

    @Query("SELECT COALESCE(SUM(amount), 0) FROM coin_transactions")
    suspend fun getTotalBalanceDirect(): Long

    @Query("SELECT COALESCE(SUM(amount), 0) FROM coin_transactions WHERE amount > 0 AND createdAt >= :startOfDay")
    fun getTodayEarnings(startOfDay: Long): Flow<Long>

    @Query("SELECT COALESCE(SUM(amount), 0) FROM coin_transactions WHERE amount < 0 AND createdAt >= :startOfDay")
    fun getTodaySpent(startOfDay: Long): Flow<Long>

    @Query("SELECT COUNT(*) FROM coin_transactions WHERE idempotencyKey = :key")
    suspend fun hasTransactionWithKey(key: String): Int

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertTransaction(tx: CoinTransaction): Long
}
