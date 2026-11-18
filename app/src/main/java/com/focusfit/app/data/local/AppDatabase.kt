package com.focusfit.app.data.local

import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        ProfileEntity::class,
        ActivityEntity::class,
        TaskLogEntity::class,
        DaySummaryEntity::class,
        CustomTaskEntity::class,
        WorkoutLogEntity::class,
        FreeMealEntity::class,
        HiddenMealEntity::class,
        BookEntity::class,
        ReadingSessionEntity::class,
        SuggestionLogEntity::class,
        WeightEntity::class,
        PlanChangeEntity::class,
    ],
    version = 8,
    exportSchema = true,
    autoMigrations = [
        AutoMigration(from = 1, to = 2),
        AutoMigration(from = 2, to = 3),
        AutoMigration(from = 3, to = 4),
        AutoMigration(from = 4, to = 5),
        AutoMigration(from = 5, to = 6),
        AutoMigration(from = 6, to = 7),
        AutoMigration(from = 7, to = 8),
    ],
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun profileDao(): ProfileDao
    abstract fun dailyDao(): DailyDao
    abstract fun planDao(): PlanDao
    abstract fun readingDao(): ReadingDao
    abstract fun suggestionDao(): SuggestionDao
    abstract fun weightDao(): WeightDao
    abstract fun backupDao(): BackupDao
}
