package com.focusflow.data.db.dao

import androidx.room.*
import com.focusflow.data.db.entity.MoodEntry
import kotlinx.coroutines.flow.Flow

@Dao
interface MoodDao {
    @Query("SELECT * FROM mood_entries WHERE date = :date LIMIT 1")
    suspend fun getMoodForDate(date: String): MoodEntry?

    @Query("SELECT * FROM mood_entries ORDER BY date DESC LIMIT 7")
    fun getWeeklyMoods(): Flow<List<MoodEntry>>

    @Query("SELECT * FROM mood_entries ORDER BY date DESC")
    fun getAllMoods(): Flow<List<MoodEntry>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMood(entry: MoodEntry): Long

    @Update
    suspend fun updateMood(entry: MoodEntry)
}
