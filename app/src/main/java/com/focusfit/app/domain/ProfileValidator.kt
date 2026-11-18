package com.focusfit.app.domain

import com.focusfit.app.domain.model.Goal
import com.focusfit.app.domain.model.ScheduledActivity
import com.focusfit.app.domain.model.UserProfile

/** Field-level checks used by onboarding. Returns a message per invalid field (empty = valid). */
object ProfileValidator {

    val AGE_RANGE = 14..90
    val HEIGHT_RANGE = 120.0..230.0
    val WEIGHT_RANGE = 35.0..300.0
    const val MAX_SESSIONS_PER_WEEK = 14

    enum class Field { AGE, HEIGHT, WEIGHT, TARGET_WEIGHT, ACTIVITIES }

    fun validateBasics(p: UserProfile): Map<Field, String> = buildMap {
        if (p.age !in AGE_RANGE) put(Field.AGE, "Age must be between ${AGE_RANGE.first} and ${AGE_RANGE.last}")
        if (p.heightCm !in HEIGHT_RANGE) put(Field.HEIGHT, "Height must be between 120 and 230 cm")
        if (p.weightKg !in WEIGHT_RANGE) put(Field.WEIGHT, "Weight must be between 35 and 300 kg")
    }

    fun validateGoal(p: UserProfile): Map<Field, String> = buildMap {
        val t = p.targetWeightKg
        when {
            t !in WEIGHT_RANGE -> put(Field.TARGET_WEIGHT, "Target must be between 35 and 300 kg")
            p.goal == Goal.LOSE && t >= p.weightKg -> put(Field.TARGET_WEIGHT, "To lose weight, the target must be below ${p.weightKg} kg")
            p.goal == Goal.GAIN && t <= p.weightKg -> put(Field.TARGET_WEIGHT, "To gain weight, the target must be above ${p.weightKg} kg")
            HealthCalculator.bmi(t, p.heightCm) < 18.5 -> put(Field.TARGET_WEIGHT, "That target is below a healthy weight for your height")
        }
    }

    fun validateActivities(activities: List<ScheduledActivity>): Map<Field, String> = buildMap {
        when {
            activities.any { it.name.isBlank() } -> put(Field.ACTIVITIES, "Give every sport a name")
            activities.any { it.days.isEmpty() } -> put(Field.ACTIVITIES, "Pick at least one day for every sport")
            activities.any { it.durationMin !in 10..240 } -> put(Field.ACTIVITIES, "Duration must be between 10 and 240 min")
            activities.sumOf { it.days.size } > MAX_SESSIONS_PER_WEEK -> put(Field.ACTIVITIES, "That's more than $MAX_SESSIONS_PER_WEEK sessions a week")
        }
    }

    fun validateAll(p: UserProfile): Map<Field, String> =
        validateBasics(p) + validateGoal(p) + validateActivities(p.activities)
}

/** Picks an emoji for a sport name so the user only has to type the name. */
fun sportEmoji(name: String): String {
    val n = name.lowercase()
    return when {
        listOf("krav", "box", "kick", "muay", "mma", "jiu", "judo", "karate", "taekwondo").any { it in n } -> "🥋"
        listOf("crossfit", "gym", "weights", "lifting", "strength").any { it in n } -> "🏋️"
        listOf("run", "jog").any { it in n } -> "🏃"
        listOf("swim").any { it in n } -> "🏊"
        listOf("bike", "cycl", "spin").any { it in n } -> "🚴"
        listOf("yoga", "pilates").any { it in n } -> "🧘"
        listOf("football", "soccer", "futsal").any { it in n } -> "⚽"
        listOf("padel", "tennis").any { it in n } -> "🎾"
        listOf("basket").any { it in n } -> "🏀"
        else -> "🏅"
    }
}
