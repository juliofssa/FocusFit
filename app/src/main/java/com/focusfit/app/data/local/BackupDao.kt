package com.focusfit.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

/** Bulk read/write of every table, used only by backup & restore. */
@Dao
interface BackupDao {
    @Query("SELECT * FROM profile") suspend fun profiles(): List<ProfileEntity>
    @Query("SELECT * FROM activity") suspend fun activities(): List<ActivityEntity>
    @Query("SELECT * FROM task_log") suspend fun taskLogs(): List<TaskLogEntity>
    @Query("SELECT * FROM day_summary") suspend fun daySummaries(): List<DaySummaryEntity>
    @Query("SELECT * FROM custom_task") suspend fun customTasks(): List<CustomTaskEntity>
    @Query("SELECT * FROM workout_log") suspend fun workoutLogs(): List<WorkoutLogEntity>
    @Query("SELECT * FROM free_meal") suspend fun freeMeals(): List<FreeMealEntity>
    @Query("SELECT * FROM hidden_meal") suspend fun hiddenMeals(): List<HiddenMealEntity>
    @Query("SELECT * FROM book") suspend fun books(): List<BookEntity>
    @Query("SELECT * FROM reading_session") suspend fun readingSessions(): List<ReadingSessionEntity>
    @Query("SELECT * FROM suggestion_log") suspend fun suggestionLogs(): List<SuggestionLogEntity>
    @Query("SELECT * FROM weight") suspend fun weights(): List<WeightEntity>
    @Query("SELECT * FROM plan_change") suspend fun planChanges(): List<PlanChangeEntity>

    @Query("DELETE FROM profile") suspend fun clearProfiles()
    @Query("DELETE FROM activity") suspend fun clearActivities()
    @Query("DELETE FROM task_log") suspend fun clearTaskLogs()
    @Query("DELETE FROM day_summary") suspend fun clearDaySummaries()
    @Query("DELETE FROM custom_task") suspend fun clearCustomTasks()
    @Query("DELETE FROM workout_log") suspend fun clearWorkoutLogs()
    @Query("DELETE FROM free_meal") suspend fun clearFreeMeals()
    @Query("DELETE FROM hidden_meal") suspend fun clearHiddenMeals()
    @Query("DELETE FROM book") suspend fun clearBooks()
    @Query("DELETE FROM reading_session") suspend fun clearReadingSessions()
    @Query("DELETE FROM suggestion_log") suspend fun clearSuggestionLogs()
    @Query("DELETE FROM weight") suspend fun clearWeights()
    @Query("DELETE FROM plan_change") suspend fun clearPlanChanges()

    @Insert suspend fun insertProfiles(rows: List<ProfileEntity>)
    @Insert suspend fun insertActivities(rows: List<ActivityEntity>)
    @Insert suspend fun insertTaskLogs(rows: List<TaskLogEntity>)
    @Insert suspend fun insertDaySummaries(rows: List<DaySummaryEntity>)
    @Insert suspend fun insertCustomTasks(rows: List<CustomTaskEntity>)
    @Insert suspend fun insertWorkoutLogs(rows: List<WorkoutLogEntity>)
    @Insert suspend fun insertFreeMeals(rows: List<FreeMealEntity>)
    @Insert suspend fun insertHiddenMeals(rows: List<HiddenMealEntity>)
    @Insert suspend fun insertBooks(rows: List<BookEntity>)
    @Insert suspend fun insertReadingSessions(rows: List<ReadingSessionEntity>)
    @Insert suspend fun insertSuggestionLogs(rows: List<SuggestionLogEntity>)
    @Insert suspend fun insertWeights(rows: List<WeightEntity>)
    @Insert suspend fun insertPlanChanges(rows: List<PlanChangeEntity>)
}
