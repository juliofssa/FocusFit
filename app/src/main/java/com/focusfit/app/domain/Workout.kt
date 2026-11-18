package com.focusfit.app.domain

import java.time.LocalDate

/** One screen of the guided session: an exercise to do, or a rest. */
data class SessionStep(
    val isRest: Boolean,
    val exercise: Exercise?,
    /** Countdown length; null for rep-based work (user taps Done). */
    val seconds: Int?,
    val reps: Int?,
    val sideLabel: String?,
    val setLabel: String,
    /** Index of the routine step this belongs to (used for swapping exercises). */
    val routineStepIndex: Int,
) {
    val title: String get() = if (isRest) "Rest" else exercise!!.name + (sideLabel?.let { " · $it" } ?: "")
    val amount: String
        get() = when {
            seconds != null -> "$seconds s"
            reps != null -> "$reps reps" + if (exercise?.perSide == true) " per side" else ""
            else -> ""
        }
}

/** A completed session. */
data class WorkoutLog(val date: LocalDate, val routineId: String, val level: Int, val durationSec: Int, val rpe: Int)

data class LevelStatus(
    val level: Int,
    /** Easy sessions (RPE ≤ 7) done at the current level. */
    val easySessions: Int,
    /** True when the last 2 sessions at this level were very hard (RPE ≥ 9). */
    val tooHard: Boolean,
) {
    val isMax: Boolean get() = level >= Progression.MAX_LEVEL
}

/**
 * 3-level progression (see docs/WORKOUT_LIBRARY.md §3):
 *  L1 as written · L2 +10 s on holds / +2 reps · L3 harder variants (or +20 s / +4 reps) and +1 circuit round.
 * A level unlocks after [SESSIONS_TO_LEVEL_UP] sessions with RPE ≤ [EASY_RPE].
 */
object Progression {
    const val MAX_LEVEL = 3
    const val SESSIONS_TO_LEVEL_UP = 4
    const val EASY_RPE = 7
    const val HARD_RPE = 9

    fun status(logs: List<WorkoutLog>): LevelStatus {
        var level = 1
        var easy = 0
        val atLevel = mutableListOf<WorkoutLog>()
        logs.sortedBy { it.date }.forEach { log ->
            if (log.level != level) return@forEach // logs from a level we've moved past
            atLevel += log
            if (log.rpe <= EASY_RPE) easy++
            if (easy >= SESSIONS_TO_LEVEL_UP && level < MAX_LEVEL) {
                level++
                easy = 0
                atLevel.clear()
            }
        }
        val lastTwo = atLevel.takeLast(2)
        return LevelStatus(level, easy, lastTwo.size == 2 && lastTwo.all { it.rpe >= HARD_RPE })
    }

    /** The exercise to use at [level] (harder variant at L3 when one exists). */
    fun exerciseFor(base: Exercise, level: Int, library: WorkoutLibrary): Exercise =
        if (level >= 3) library.exerciseOrNull(base.harderId) ?: base else base

    /** Adds time/reps for the level, unless L3 already switched to a harder variant. */
    fun bump(value: Int?, isSeconds: Boolean, level: Int, switchedVariant: Boolean): Int? {
        if (value == null || switchedVariant) return value
        val extra = when (level) {
            2 -> if (isSeconds) 10 else 2
            3 -> if (isSeconds) 20 else 4
            else -> 0
        }
        return value + extra
    }

    fun rounds(routine: Routine, level: Int): Int =
        routine.rounds + if (routine.isCircuit && routine.rounds > 1 && level >= 3) 1 else 0
}

object SessionBuilder {

    /**
     * Expands a routine into steps. [swaps] maps a routine step index to a replacement exercise id
     * (easier/harder variant chosen by the user during the session).
     */
    fun build(
        routine: Routine,
        library: WorkoutLibrary,
        level: Int = 1,
        swaps: Map<Int, String> = emptyMap(),
    ): List<SessionStep> {
        val steps = mutableListOf<SessionStep>()
        fun rest(seconds: Int, index: Int) {
            if (seconds > 0) steps += SessionStep(true, null, seconds, null, null, "", index)
        }
        fun work(index: Int, setLabel: String) {
            val rs = routine.steps[index]
            val base = library.exercise(rs.exerciseId)
            val swapped = swaps[index]?.let(library::exercise)
            val ex = swapped ?: Progression.exerciseFor(base, level, library)
            val switched = ex.id != base.id
            val secs = Progression.bump(if (switched) ex.seconds else rs.secondsOverride ?: ex.seconds, true, level, switched)
            val reps = Progression.bump(if (switched) ex.reps else rs.repsOverride ?: ex.reps, false, level, switched)
            if (secs != null && ex.perSide) {
                steps += SessionStep(false, ex, secs, null, "left", setLabel, index)
                steps += SessionStep(false, ex, secs, null, "right", setLabel, index)
            } else {
                steps += SessionStep(false, ex, secs, if (secs == null) reps else null, null, setLabel, index)
            }
        }

        if (routine.isCircuit) {
            val rounds = Progression.rounds(routine, level)
            for (r in 1..rounds) {
                routine.steps.indices.forEach { i ->
                    work(i, "Round $r of $rounds")
                    if (i < routine.steps.lastIndex) rest(routine.restBetweenExercisesSec, i)
                }
                if (r < rounds) rest(routine.restBetweenRoundsSec, routine.steps.lastIndex)
            }
        } else {
            routine.steps.forEachIndexed { i, rs ->
                for (set in 1..rs.sets) {
                    work(i, "Set $set of ${rs.sets}")
                    if (set < rs.sets) rest(routine.restBetweenSetsSec, i)
                }
                if (i < routine.steps.lastIndex) rest(routine.restBetweenExercisesSec, i)
            }
        }
        return steps
    }

    /** Rough duration, counting ~3 s per rep. */
    fun estimatedMinutes(steps: List<SessionStep>): Int =
        (steps.sumOf { it.seconds ?: ((it.reps ?: 0) * 3 * if (it.exercise?.perSide == true) 2 else 1) } / 60.0)
            .let { kotlin.math.ceil(it).toInt() }
}
