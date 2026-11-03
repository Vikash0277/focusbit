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
        UnlockIntervention::class,
        Note::class
    ],
    version = 11,
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
    abstract fun noteDao(): NoteDao

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

        private val MIGRATION_8_9 = object : androidx.room.migration.Migration(8, 9) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS notes (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        title TEXT NOT NULL,
                        content TEXT NOT NULL,
                        emoji TEXT NOT NULL,
                        category TEXT NOT NULL,
                        colorTag TEXT NOT NULL,
                        date TEXT NOT NULL,
                        createdAt INTEGER NOT NULL
                    )
                """.trimIndent())
            }
        }

        private val MIGRATION_9_10 = object : androidx.room.migration.Migration(9, 10) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE todos ADD COLUMN dueDate TEXT NOT NULL DEFAULT ''")
            }
        }

        private val MIGRATION_10_11 = object : androidx.room.migration.Migration(10, 11) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE notes ADD COLUMN audioPath TEXT DEFAULT NULL")
            }
        }

        fun getDatabase(context: Context): FocusFlowDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    FocusFlowDatabase::class.java,
                    "focusflow_database"
                )
                    .addMigrations(MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9, MIGRATION_9_10, MIGRATION_10_11)
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
