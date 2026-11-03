package com.focusflow.data.db.dao

import androidx.room.*
import com.focusflow.data.db.entity.BlockedApp
import com.focusflow.data.db.entity.BlockSchedule
import kotlinx.coroutines.flow.Flow

@Dao
interface ShieldDao {
    @Query("SELECT * FROM blocked_apps ORDER BY appName ASC")
    fun getAllBlockedApps(): Flow<List<BlockedApp>>

    @Query("SELECT * FROM blocked_apps ORDER BY appName ASC")
    fun getAllBlockedAppsSync(): List<BlockedApp>

    @Query("SELECT packageName FROM blocked_apps WHERE isBlocked = 1")
    fun getBlockedPackages(): Flow<List<String>>

    @Query("SELECT packageName FROM blocked_apps WHERE isBlocked = 1")
    fun getBlockedPackagesSync(): List<String>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBlockedApp(app: BlockedApp): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBlockedApps(apps: List<BlockedApp>)

    @Update
    suspend fun updateBlockedApp(app: BlockedApp)

    @Delete
    suspend fun deleteBlockedApp(app: BlockedApp)

    @Query("DELETE FROM blocked_apps WHERE packageName = :packageName")
    suspend fun deleteByPackageName(packageName: String)

    @Query("DELETE FROM blocked_apps")
    suspend fun deleteAllBlockedApps()

    @Query("UPDATE blocked_apps SET isBlocked = :isBlocked WHERE id = :appId")
    suspend fun toggleAppBlocked(appId: Long, isBlocked: Boolean)

    @Query("UPDATE blocked_apps SET isBlocked = :isBlocked WHERE packageName = :packageName")
    suspend fun togglePackageBlocked(packageName: String, isBlocked: Boolean)

    @Query("UPDATE blocked_apps SET lockDurationMinutes = :minutes WHERE id = :appId")
    suspend fun updateAppDuration(appId: Long, minutes: Int)

    @Query("UPDATE blocked_apps SET isBlocked = 1, lockedUntilMillis = :lockedUntilMillis WHERE id = :appId")
    suspend fun lockApp(appId: Long, lockedUntilMillis: Long)

    @Query("UPDATE blocked_apps SET isBlocked = 0, lockedUntilMillis = 0 WHERE id = :appId")
    suspend fun unlockApp(appId: Long)

    @Query("UPDATE blocked_apps SET isBlocked = 1, lockedUntilMillis = :currentTimeMillis + (lockDurationMinutes * 60000)")
    suspend fun lockAllApps(currentTimeMillis: Long)

    @Query("UPDATE blocked_apps SET isBlocked = 0, lockedUntilMillis = 0")
    suspend fun unlockAllApps()

    @Query("UPDATE blocked_apps SET startTime = :startTime, endTime = :endTime WHERE id = :appId")
    suspend fun updateAppTimeWindow(appId: Long, startTime: String, endTime: String)

    @Query("UPDATE blocked_apps SET startTime = :startTime, endTime = :endTime")
    suspend fun updateAllTimeWindows(startTime: String, endTime: String)

    @Query("UPDATE blocked_apps SET isStrictLock = :isStrict WHERE id = :appId")
    suspend fun updateAppStrictLock(appId: Long, isStrict: Boolean)

    @Query("UPDATE blocked_apps SET isBlocked = :isBlocked")
    suspend fun setAllAppsBlocked(isBlocked: Boolean)

    @Query("SELECT * FROM block_schedules ORDER BY startTime ASC")
    fun getAllSchedules(): Flow<List<BlockSchedule>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSchedule(schedule: BlockSchedule): Long

    @Update
    suspend fun updateSchedule(schedule: BlockSchedule)

    @Delete
    suspend fun deleteSchedule(schedule: BlockSchedule)
}
