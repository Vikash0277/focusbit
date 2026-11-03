package com.focusflow.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.focusflow.data.db.entity.UnlockIntervention
import kotlinx.coroutines.flow.Flow

data class ReasonStat(
    val reason: String,
    val count: Int
)

@Dao
interface InterventionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertIntervention(intervention: UnlockIntervention): Long

    @Query("SELECT * FROM unlock_interventions ORDER BY timestamp DESC")
    fun getAllInterventions(): Flow<List<UnlockIntervention>>

    @Query("SELECT * FROM unlock_interventions WHERE timestamp >= :startOfDay ORDER BY timestamp DESC")
    fun getTodayInterventions(startOfDay: Long): Flow<List<UnlockIntervention>>

    @Query("SELECT COUNT(*) FROM unlock_interventions WHERE timestamp >= :startOfDay AND outcome = 'UNLOCKED'")
    suspend fun getTodayUnlockCountDirect(startOfDay: Long): Int

    @Query("SELECT COUNT(*) FROM unlock_interventions WHERE timestamp >= :startOfDay AND outcome = 'UNLOCKED'")
    fun getTodayUnlockCount(startOfDay: Long): Flow<Int>

    @Query("SELECT COUNT(*) FROM unlock_interventions WHERE outcome = 'RESISTED'")
    fun getResistedCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM unlock_interventions WHERE outcome = 'UNLOCKED'")
    fun getUnlockedCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM unlock_interventions")
    fun getTotalInterventionsCount(): Flow<Int>

    @Query("SELECT reason, COUNT(*) as count FROM unlock_interventions GROUP BY reason ORDER BY count DESC")
    fun getReasonStats(): Flow<List<ReasonStat>>
}
