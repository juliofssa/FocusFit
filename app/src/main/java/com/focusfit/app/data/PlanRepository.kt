package com.focusfit.app.data

import com.focusfit.app.data.local.FreeMealEntity
import com.focusfit.app.data.local.HiddenMealEntity
import com.focusfit.app.data.local.PlanDao
import com.focusfit.app.data.local.WorkoutLogEntity
import com.focusfit.app.domain.WorkoutLog
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

/** Workout history, free meals and hidden meal ideas. */
@Singleton
class PlanRepository @Inject constructor(private val dao: PlanDao) {

    val workoutLogs: Flow<List<WorkoutLog>> = dao.observeWorkoutLogs().map { rows ->
        rows.map { WorkoutLog(LocalDate.parse(it.date), it.routineId, it.level, it.durationSec, it.rpe) }
    }

    suspend fun saveWorkout(log: WorkoutLog) = dao.insertWorkoutLog(
        WorkoutLogEntity(
            date = log.date.toString(),
            routineId = log.routineId,
            level = log.level,
            durationSec = log.durationSec,
            rpe = log.rpe,
            createdAt = System.currentTimeMillis(),
        ),
    )

    fun freeMealsSince(from: LocalDate): Flow<List<LocalDate>> =
        dao.observeFreeMealsSince(from.toString()).map { rows -> rows.map { LocalDate.parse(it.date) } }

    suspend fun useFreeMeal(date: LocalDate) = dao.insertFreeMeal(FreeMealEntity(date = date.toString()))

    suspend fun undoFreeMeal() = dao.deleteLastFreeMeal()

    val hiddenMeals: Flow<Set<String>> = dao.observeHiddenMeals().map { it.toSet() }

    suspend fun hideMeal(id: String) = dao.hideMeal(HiddenMealEntity(id))

    suspend fun unhideAllMeals() = dao.unhideAllMeals()
}
