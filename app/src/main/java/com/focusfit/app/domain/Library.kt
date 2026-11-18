package com.focusfit.app.domain

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** Exercise + routine definitions shipped in assets/workouts.json (see docs/WORKOUT_LIBRARY.md). */
@Serializable
data class Exercise(
    val id: String,
    val name: String,
    val category: String,
    val focus: String,
    val reps: Int? = null,
    val seconds: Int? = null,
    val perSide: Boolean = false,
    val cues: List<String> = emptyList(),
    val easierId: String? = null,
    val harderId: String? = null,
    val equipment: String = "none",
)

@Serializable
data class RoutineStep(
    val exerciseId: String,
    val sets: Int = 1,
    val secondsOverride: Int? = null,
    val repsOverride: Int? = null,
)

@Serializable
data class Routine(
    val id: String,
    val name: String,
    /** SETS = all sets of one exercise, then the next. CIRCUIT = one pass through all exercises per round. */
    val format: String,
    val rounds: Int = 1,
    val restBetweenSetsSec: Int = 0,
    val restBetweenExercisesSec: Int = 0,
    val restBetweenRoundsSec: Int = 0,
    val steps: List<RoutineStep>,
) {
    val isCircuit: Boolean get() = format == "CIRCUIT"
}

@Serializable
data class WorkoutLibrary(val exercises: List<Exercise>, val routines: List<Routine>) {
    private val byId = exercises.associateBy { it.id }
    fun exercise(id: String): Exercise = byId[id] ?: error("Unknown exercise $id")
    fun exerciseOrNull(id: String?): Exercise? = id?.let(byId::get)
    fun routine(id: String): Routine? = routines.firstOrNull { it.id == id }
}

@Serializable
data class MealIdea(
    val id: String,
    val name: String,
    val type: String,
    val proteinG: Int,
    val tags: List<String> = emptyList(),
    /** Ingredient groups (FoodTag names: MEAT, PORK, FISH, EGGS, DAIRY, GLUTEN, NUTS, SOY). */
    val contains: List<String> = emptyList(),
)

@Serializable
data class MealLibrary(val meals: List<MealIdea>)

object LibraryParser {
    private val json = Json { ignoreUnknownKeys = true }
    fun workouts(text: String): WorkoutLibrary = json.decodeFromString(text)
    fun meals(text: String): MealLibrary = json.decodeFromString(text)
}
