package com.focusflow.data.db.dao

import androidx.room.*
import com.focusflow.data.db.entity.Commitment
import kotlinx.coroutines.flow.Flow

@Dao
interface CommitmentDao {
    @Query("SELECT * FROM commitments WHERE date = :date ORDER BY startTime ASC")
    fun getCommitmentsForDate(date: String): Flow<List<Commitment>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCommitment(commitment: Commitment): Long

    @Update
    suspend fun updateCommitment(commitment: Commitment)

    @Delete
    suspend fun deleteCommitment(commitment: Commitment)
}
