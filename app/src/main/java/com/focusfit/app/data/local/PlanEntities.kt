package com.focusfit.app.data.local

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "workout_log")
@kotlinx.serialization.Serializable
data class WorkoutLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: String,
    val routineId: String,
    val level: Int,
    val durationSec: Int,
    val rpe: Int,
    val createdAt: Long,
)

/** One "free meal" (80/20 rule) used on [date]. */
@Entity(tableName = "free_meal")
@kotlinx.serialization.Serializable
data class FreeMealEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: String,
)

/** A meal idea the user doesn't want suggested. */
@Entity(tableName = "hidden_meal")
@kotlinx.serialization.Serializable
data class HiddenMealEntity(@PrimaryKey val mealId: String)

@Dao
interface PlanDao {
    @Query("SELECT * FROM workout_log ORDER BY date, createdAt")
    fun observeWorkoutLogs(): Flow<List<WorkoutLogEntity>>

    @Insert
    suspend fun insertWorkoutLog(log: WorkoutLogEntity)

    @Query("SELECT * FROM free_meal WHERE date >= :from ORDER BY date")
    fun observeFreeMealsSince(from: String): Flow<List<FreeMealEntity>>

    @Insert
    suspend fun insertFreeMeal(meal: FreeMealEntity)

    @Query("DELETE FROM free_meal WHERE id = (SELECT MAX(id) FROM free_meal)")
    suspend fun deleteLastFreeMeal()

    @Query("SELECT mealId FROM hidden_meal")
    fun observeHiddenMeals(): Flow<List<String>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun hideMeal(meal: HiddenMealEntity)

    @Query("DELETE FROM hidden_meal")
    suspend fun unhideAllMeals()
}
