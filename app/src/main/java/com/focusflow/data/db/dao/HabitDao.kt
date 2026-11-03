package com.focusflow.data.db.dao

import androidx.room.*
import com.focusflow.data.db.entity.Habit
import com.focusflow.data.db.entity.HabitCompletion
import kotlinx.coroutines.flow.Flow

@Dao
interface HabitDao {
    @Query("SELECT * FROM habits WHERE isActive = 1 ORDER BY time ASC")
    fun getActiveHabits(): Flow<List<Habit>>

    @Query("SELECT * FROM habits ORDER BY id DESC")
    fun getAllHabits(): Flow<List<Habit>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHabit(habit: Habit): Long

    @Update
    suspend fun updateHabit(habit: Habit)

    @Delete
    suspend fun deleteHabit(habit: Habit)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCompletion(completion: HabitCompletion)

    @Query("SELECT * FROM habit_completions WHERE habitId = :habitId AND date = :date LIMIT 1")
    suspend fun getCompletion(habitId: Long, date: String): HabitCompletion?

    @Query("DELETE FROM habit_completions WHERE habitId = :habitId AND date = :date")
    suspend fun deleteCompletion(habitId: Long, date: String)

    @Query("SELECT * FROM habit_completions WHERE habitId = :habitId ORDER BY date DESC")
    fun getCompletionsForHabit(habitId: Long): Flow<List<HabitCompletion>>

    @Query("SELECT * FROM habit_completions WHERE date = :date")
    fun getCompletionsForDate(date: String): Flow<List<HabitCompletion>>

    @Query("SELECT COUNT(*) FROM habit_completions WHERE habitId = :habitId AND date LIKE :monthPrefix || '%'")
    suspend fun getMonthlyCompletionCount(habitId: Long, monthPrefix: String): Int

    @Query("SELECT * FROM habit_completions WHERE date >= :startDate ORDER BY date ASC")
    fun getCompletionsSince(startDate: String): Flow<List<HabitCompletion>>

    @Query("SELECT * FROM habit_completions ORDER BY date ASC")
    fun getAllCompletions(): Flow<List<HabitCompletion>>
}
