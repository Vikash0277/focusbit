package com.focusflow.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.focusflow.data.db.dao.*
import com.focusflow.data.db.entity.*

@Database(
    entities = [
        Goal::class,
        Habit::class,
        HabitCompletion::class,
        FocusSession::class,
        Lap::class,
        MoodEntry::class,
        BlockedApp::class,
        BlockSchedule::class,
        Commitment::class,
        Todo::class,
        CoinTransaction::class,
        UnlockIntervention::class
    ],
    version = 8,
    exportSchema = false
)
abstract class FocusFlowDatabase : RoomDatabase() {

    abstract fun goalDao(): GoalDao
    abstract fun habitDao(): HabitDao
    abstract fun sessionDao(): SessionDao
    abstract fun moodDao(): MoodDao
    abstract fun shieldDao(): ShieldDao
    abstract fun commitmentDao(): CommitmentDao
    abstract fun todoDao(): TodoDao
    abstract fun coinDao(): CoinDao
    abstract fun interventionDao(): InterventionDao

    companion object {
        @Volatile
        private var INSTANCE: FocusFlowDatabase? = null

        private val MIGRATION_6_7 = object : androidx.room.migration.Migration(6, 7) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE goals ADD COLUMN reminderEnabled INTEGER NOT NULL DEFAULT 1")
                db.execSQL("ALTER TABLE goals ADD COLUMN reminderTime TEXT NOT NULL DEFAULT '09:00'")
            }
        }

        private val MIGRATION_7_8 = object : androidx.room.migration.Migration(7, 8) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE goals ADD COLUMN goalType TEXT NOT NULL DEFAULT 'HOURS'")
                db.execSQL("ALTER TABLE goals ADD COLUMN targetDays INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE goals ADD COLUMN loggedDays INTEGER NOT NULL DEFAULT 0")
            }
        }

        fun getDatabase(context: Context): FocusFlowDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    FocusFlowDatabase::class.java,
                    "focusflow_database"
                )
                    .addMigrations(MIGRATION_6_7, MIGRATION_7_8)
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
