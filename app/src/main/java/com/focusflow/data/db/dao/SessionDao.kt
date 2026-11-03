package com.focusflow.data.db.dao

import androidx.room.*
import com.focusflow.data.db.entity.FocusSession
import com.focusflow.data.db.entity.Lap
import kotlinx.coroutines.flow.Flow

@Dao
interface SessionDao {
    @Query("SELECT * FROM focus_sessions ORDER BY id DESC")
    fun getAllSessions(): Flow<List<FocusSession>>

    @Query("SELECT * FROM focus_sessions WHERE id = :id")
    suspend fun getSessionById(id: Long): FocusSession?

    @Query("SELECT * FROM focus_sessions WHERE status = 'running' LIMIT 1")
    suspend fun getRunningSession(): FocusSession?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: FocusSession): Long

    @Update
    suspend fun updateSession(session: FocusSession)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLap(lap: Lap): Long

    @Query("SELECT * FROM laps WHERE sessionId = :sessionId ORDER BY lapNumber DESC")
    fun getLapsForSession(sessionId: Long): Flow<List<Lap>>
}
