package com.focusfit.app.domain.model

import com.focusfit.app.domain.PlanChange
import java.time.DayOfWeek
import java.time.DayOfWeek.FRIDAY
import java.time.DayOfWeek.MONDAY
import java.time.DayOfWeek.SUNDAY
import java.time.DayOfWeek.THURSDAY
import java.time.DayOfWeek.TUESDAY
import java.time.DayOfWeek.WEDNESDAY
import java.time.LocalTime

enum class Sex { MALE, FEMALE }

enum class Goal { LOSE, MAINTAIN, GAIN }

/** Weight-change pace. [kcalAdjust] is the daily deficit (LOSE) or surplus (GAIN). */
enum class Pace(val kgPerWeek: Double, val kcalAdjust: Int) {
    RELAXED(0.25, 275),
    STANDARD(0.5, 450),
}

enum class WorkMode { OFFICE, REMOTE, OFF }

/** Ingredients a meal can contain; used for diet styles and personal exclusions (allergies, dislikes). */
enum class FoodTag(val label: String) {
    MEAT("Meat"), PORK("Pork"), FISH("Fish & seafood"), EGGS("Eggs"), DAIRY("Dairy / lactose"),
    GLUTEN("Gluten"), NUTS("Nuts"), SOY("Soy"),
}

enum class DietStyle(val label: String, val excludes: Set<FoodTag>) {
    ANY("Everything", emptySet()),
    PESCATARIAN("Pescatarian", setOf(FoodTag.MEAT, FoodTag.PORK)),
    VEGETARIAN("Vegetarian", setOf(FoodTag.MEAT, FoodTag.PORK, FoodTag.FISH)),
    VEGAN("Vegan", setOf(FoodTag.MEAT, FoodTag.PORK, FoodTag.FISH, FoodTag.EGGS, FoodTag.DAIRY)),
}

/** User-set targets that replace the calculated ones (null = use the suggestion). */
data class TargetOverrides(
    val kcal: Int? = null,
    val proteinG: Int? = null,
    val waterMl: Int? = null,
    /** Fixed step goal; disables the weekly +1,000 progression. */
    val steps: Int? = null,
    val readingMin: Int? = null,
) {
    val isEmpty: Boolean get() = listOf(kcal, proteinG, waterMl, steps, readingMin).all { it == null }
}

/** A sport the user does on fixed weekdays (fully user-defined). */
data class ScheduledActivity(
    val name: String,
    val emoji: String,
    val days: Set<DayOfWeek>,
    val time: LocalTime?,
    val durationMin: Int,
)

data class UserProfile(
    val sex: Sex,
    val age: Int,
    val heightCm: Double,
    val weightKg: Double,
    val goal: Goal,
    val targetWeightKg: Double,
    val pace: Pace,
    /** Current average daily steps (until Health Connect provides real data). */
    val avgDailySteps: Int,
    val activities: List<ScheduledActivity>,
    val workModes: Map<DayOfWeek, WorkMode>,
    val restDay: DayOfWeek = SUNDAY,
    val targetOverrides: TargetOverrides = TargetOverrides(),
    val dietStyle: DietStyle = DietStyle.ANY,
    /** Extra ingredients to avoid (allergies, dislikes) on top of [dietStyle]. */
    val foodExcludes: Set<FoodTag> = emptySet(),
    val freeMealsPerWeek: Int = 3,
    /** One-off changes to specific dates (skip / move / add a session); the weekly template stays as is. */
    val planChanges: List<PlanChange> = emptyList(),
) {
    val sessionsPerWeek: Int get() = activities.sumOf { it.days.size }

    val trainingDays: Set<DayOfWeek> get() = activities.flatMap { it.days }.toSet()

    /** Everything this person doesn't eat: diet style + personal exclusions. */
    val excludedFoods: Set<FoodTag> get() = dietStyle.excludes + foodExcludes

    fun workModeOn(day: DayOfWeek): WorkMode = workModes[day] ?: WorkMode.OFF

    companion object {
        /** Example profile (the app author's) — used by tests and docs. New users start from [NEW_USER]. */
        val REFERENCE = UserProfile(
            sex = Sex.MALE,
            age = 33,
            heightCm = 174.0,
            weightKg = 84.0,
            goal = Goal.LOSE,
            targetWeightKg = 78.0,
            pace = Pace.STANDARD,
            avgDailySteps = 5_000,
            activities = listOf(
                ScheduledActivity("Krav Maga", "🥋", setOf(MONDAY, FRIDAY), LocalTime.of(19, 0), 90),
                ScheduledActivity("CrossFit", "🏋️", setOf(TUESDAY), LocalTime.of(18, 30), 60),
            ),
            workModes = mapOf(
                MONDAY to WorkMode.REMOTE,
                TUESDAY to WorkMode.REMOTE,
                WEDNESDAY to WorkMode.OFFICE,
                THURSDAY to WorkMode.REMOTE,
                FRIDAY to WorkMode.REMOTE,
            ),
        )

        /** Neutral starting point for onboarding: body fields are blank in the form, no sports, office Mon–Fri. */
        val NEW_USER = UserProfile(
            sex = Sex.MALE,
            age = 0,
            heightCm = 0.0,
            weightKg = 0.0,
            goal = Goal.MAINTAIN,
            targetWeightKg = 0.0,
            pace = Pace.STANDARD,
            avgDailySteps = 5_000,
            activities = emptyList(),
            workModes = listOf(MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY).associateWith { WorkMode.OFFICE },
        )
    }
}
