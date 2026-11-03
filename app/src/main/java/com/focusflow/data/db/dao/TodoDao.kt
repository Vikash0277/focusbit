package com.focusflow.data.db.dao

import androidx.room.*
import com.focusflow.data.db.entity.Todo
import kotlinx.coroutines.flow.Flow

@Dao
interface TodoDao {
    @Query("SELECT * FROM todos ORDER BY isCompleted ASC, createdAt DESC")
    fun getAllTodos(): Flow<List<Todo>>

    @Query("SELECT * FROM todos WHERE isCompleted = 0 ORDER BY createdAt DESC")
    fun getPendingTodos(): Flow<List<Todo>>

    @Query("SELECT COUNT(*) FROM todos WHERE isCompleted = 1")
    fun getCompletedTodosCount(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTodo(todo: Todo): Long

    @Update
    suspend fun updateTodo(todo: Todo)

    @Delete
    suspend fun deleteTodo(todo: Todo)

    @Query("UPDATE todos SET isCompleted = :completed, completedAt = :completedAt WHERE id = :id")
    suspend fun toggleTodo(id: Long, completed: Boolean, completedAt: Long?)

    @Query("DELETE FROM todos WHERE id = :id")
    suspend fun deleteById(id: Long)
}
