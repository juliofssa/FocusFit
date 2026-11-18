package com.focusfit.app.domain

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * manifest.json of an online content pack. The app downloads the pack only when [version] is newer
 * than what it has, and only uses files whose SHA-256 matches [files].
 */
@Serializable
data class ContentManifest(
    val version: Int,
    /** Layout of the JSON files; the app ignores packs with a format it doesn't know. */
    val format: Int = 1,
    /** File name → lowercase hex SHA-256. Must contain workouts.json and meals.json. */
    val files: Map<String, String>,
    val publishedAt: String? = null,
    val notes: String? = null,
) {
    companion object {
        const val SUPPORTED_FORMAT = 1
        const val WORKOUTS = "workouts.json"
        const val MEALS = "meals.json"
        private val json = Json { ignoreUnknownKeys = true }
        fun parse(text: String): ContentManifest = json.decodeFromString(text)
    }
}

/** Checks a content pack before the app uses it (also run by the content repo's GitHub check). */
object ContentValidator {

    private val YOUTUBE_ID = Regex("^[A-Za-z0-9_-]{11}$")
    val MEAL_TYPES = setOf("BREAKFAST", "LUNCH", "DINNER", "PRE_TRAINING", "SNACK")

    /** Routines the app's week planner and screens refer to by id. */
    val REQUIRED_ROUTINES = setOf(WeekPlanner.CORE_A, WeekPlanner.CORE_B, WeekPlanner.MOBILITY, "quick_core")

    fun validate(workouts: WorkoutLibrary, meals: MealLibrary): List<String> = buildList {
        val ids = workouts.exercises.map { it.id }
        ids.groupBy { it }.filterValues { it.size > 1 }.keys.forEach { add("Duplicate exercise id '$it'") }
        val idSet = ids.toSet()
        workouts.exercises.forEach { e ->
            if (e.reps == null && e.seconds == null) add("Exercise '${e.id}' needs reps or seconds")
            e.easierId?.let { if (it !in idSet) add("Exercise '${e.id}': easierId '$it' doesn't exist") }
            e.harderId?.let { if (it !in idSet) add("Exercise '${e.id}': harderId '$it' doesn't exist") }
            e.youtubeId?.let { if (!YOUTUBE_ID.matches(it)) add("Exercise '${e.id}': youtubeId '$it' isn't an 11-character YouTube id") }
            if (e.videoStartSec < 0) add("Exercise '${e.id}': videoStartSec can't be negative")
        }
        REQUIRED_ROUTINES.filter { r -> workouts.routines.none { it.id == r } }.forEach { add("Missing routine '$it'") }
        workouts.routines.forEach { r ->
            if (r.steps.isEmpty()) add("Routine '${r.id}' has no steps")
            if (r.format !in setOf("SETS", "CIRCUIT")) add("Routine '${r.id}': unknown format '${r.format}'")
            r.steps.forEach { s -> if (s.exerciseId !in idSet) add("Routine '${r.id}' uses unknown exercise '${s.exerciseId}'") }
        }
        meals.meals.map { it.id }.groupBy { it }.filterValues { it.size > 1 }.keys.forEach { add("Duplicate meal id '$it'") }
        meals.meals.forEach { m ->
            if (m.type !in MEAL_TYPES) add("Meal '${m.id}': unknown type '${m.type}'")
            if (m.proteinG < 0) add("Meal '${m.id}': protein can't be negative")
        }
        MEAL_TYPES.filter { t -> meals.meals.none { it.type == t } }.forEach { add("No meal ideas of type $it") }
    }
}
