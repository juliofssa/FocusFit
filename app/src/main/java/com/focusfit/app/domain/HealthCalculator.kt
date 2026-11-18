package com.focusfit.app.domain

import com.focusfit.app.domain.model.Goal
import com.focusfit.app.domain.model.Sex
import com.focusfit.app.domain.model.TargetOverrides
import com.focusfit.app.domain.model.UserProfile
import kotlin.math.max
import kotlin.math.roundToInt

/** Daily targets derived from the profile. Values are estimates (see docs/HEALTH_ENGINE.md). */
data class Targets(
    val bmi: Double,
    val healthyWeightMinKg: Double,
    val healthyWeightMaxKg: Double,
    val bmr: Int,
    val tdee: Int,
    val kcalAverage: Int,
    val kcalTrainingDay: Int,
    val kcalRestDay: Int,
    val proteinG: Int,
    val fatG: Int,
    val carbsG: Int,
    val waterMl: Int,
    val waterMlTrainingDay: Int,
    val stepGoal: Int,
    val readingMin: Int,
)

object HealthCalculator {

    const val MIN_STEP_GOAL = 6_000
    const val MAX_STEP_GOAL = 10_000
    const val START_READING_MIN = 15
    private const val TRAINING_DAY_BONUS = 150
    private const val REST_DAY_REDUCTION = 100

    fun bmi(weightKg: Double, heightCm: Double): Double {
        val m = heightCm / 100.0
        return weightKg / (m * m)
    }

    /** Mifflin–St Jeor. */
    fun bmr(sex: Sex, weightKg: Double, heightCm: Double, age: Int): Double {
        val base = 10 * weightKg + 6.25 * heightCm - 5 * age
        return if (sex == Sex.MALE) base + 5 else base - 161
    }

    fun activityFactor(sessionsPerWeek: Int, avgDailySteps: Int): Double = when {
        sessionsPerWeek >= 6 -> 1.55
        sessionsPerWeek >= 3 && avgDailySteps >= 7_000 -> 1.465
        sessionsPerWeek <= 1 && avgDailySteps < 5_000 -> 1.2
        else -> 1.375
    }

    /** Starting step goal: current average + 1k, within [MIN_STEP_GOAL, MAX_STEP_GOAL]. */
    fun initialStepGoal(avgDailySteps: Int): Int =
        (roundToThousand(avgDailySteps) + 1_000).coerceIn(MIN_STEP_GOAL, MAX_STEP_GOAL)

    /** Weekly progression: +1k when the goal was hit on at least 5 of 7 days. */
    fun nextStepGoal(currentGoal: Int, daysGoalHit: Int): Int =
        if (daysGoalHit >= 5) (currentGoal + 1_000).coerceAtMost(MAX_STEP_GOAL) else currentGoal

    /** The calculated suggestions, ignoring the user's overrides (shown next to them in Targets). */
    fun suggestedTargets(p: UserProfile): Targets = targets(p.copy(targetOverrides = TargetOverrides()))

    /** Targets the app uses: calculated, with any user overrides applied (fat/carbs follow the kcal & protein). */
    fun targets(p: UserProfile): Targets {
        val o = p.targetOverrides
        val bmi = bmi(p.weightKg, p.heightCm)
        val h2 = (p.heightCm / 100.0).let { it * it }
        val bmr = bmr(p.sex, p.weightKg, p.heightCm, p.age)
        val tdee = bmr * activityFactor(p.sessionsPerWeek, p.avgDailySteps)

        val kcal = o.kcal?.toDouble() ?: when (p.goal) {
            Goal.LOSE -> max(tdee - p.pace.kcalAdjust, bmr * 1.1)
            Goal.MAINTAIN -> tdee
            Goal.GAIN -> tdee + p.pace.kcalAdjust / 2.0
        }

        // Use goal weight for protein when obese, so targets stay realistic.
        val proteinBaseKg = if (bmi > 30) p.targetWeightKg else p.weightKg
        val protein = o.proteinG?.toDouble() ?: (1.8 * proteinBaseKg)
        val fat = max(0.8 * p.weightKg, 0.2 * kcal / 9)
        val carbs = max(0.0, (kcal - protein * 4 - fat * 9) / 4)
        val water = o.waterMl?.toDouble() ?: (35 * p.weightKg)

        return Targets(
            bmi = (bmi * 10).roundToInt() / 10.0,
            healthyWeightMinKg = (18.5 * h2 * 10).roundToInt() / 10.0,
            healthyWeightMaxKg = (24.9 * h2 * 10).roundToInt() / 10.0,
            bmr = bmr.roundToInt(),
            tdee = tdee.roundToInt(),
            kcalAverage = roundTo(kcal, 10),
            kcalTrainingDay = roundTo(kcal + TRAINING_DAY_BONUS, 10),
            kcalRestDay = roundTo(kcal - REST_DAY_REDUCTION, 10),
            proteinG = protein.roundToInt(),
            fatG = fat.roundToInt(),
            carbsG = carbs.roundToInt(),
            waterMl = o.waterMl ?: roundTo(water, 250),
            waterMlTrainingDay = o.waterMl?.plus(500) ?: roundTo(water + 500, 250),
            stepGoal = o.steps ?: initialStepGoal(p.avgDailySteps),
            readingMin = o.readingMin ?: START_READING_MIN,
        )
    }

    /** Allowed ranges for user overrides (Targets screen). */
    val KCAL_RANGE = 1_200..5_000
    val PROTEIN_RANGE = 40..300
    val WATER_RANGE = 1_000..6_000
    val STEPS_RANGE = 2_000..30_000
    val READING_RANGE = 5..180

    private fun roundTo(value: Double, step: Int): Int = ((value / step).roundToInt()) * step

    private fun roundToThousand(steps: Int): Int = roundTo(steps.toDouble(), 1_000)
}
