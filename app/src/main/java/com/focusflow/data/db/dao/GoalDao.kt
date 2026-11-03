package com.focusflow.data.db.dao

import androidx.room.*
import com.focusflow.data.db.entity.Goal
import kotlinx.coroutines.flow.Flow

@Dao
interface GoalDao {
    @Query("SELECT * FROM goals ORDER BY id DESC")
    fun getAllGoals(): Flow<List<Goal>>

    @Query("SELECT * FROM goals WHERE id = :id")
    suspend fun getGoalById(id: Long): Goal?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGoal(goal: Goal): Long

    @Update
    suspend fun updateGoal(goal: Goal)

    @Delete
    suspend fun deleteGoal(goal: Goal)

    @Query("UPDATE goals SET loggedHours = loggedHours + :hours WHERE id = :goalId")
    suspend fun logHours(goalId: Long, hours: Float)

    @Query("UPDATE goals SET loggedDays = loggedDays + 1 WHERE id = :goalId")
    suspend fun logDay(goalId: Long)

    @Query("UPDATE goals SET reminderEnabled = :reminderEnabled, reminderTime = :reminderTime WHERE id = :goalId")
    suspend fun updateGoalReminder(goalId: Long, reminderEnabled: Boolean, reminderTime: String)
}
