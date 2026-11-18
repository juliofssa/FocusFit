package com.focusfit.app.domain

import com.focusfit.app.domain.SuggestionRule.BACK_TO_BACK
import com.focusfit.app.domain.SuggestionRule.EVENING_STEPS
import com.focusfit.app.domain.SuggestionRule.MISSED_SESSION
import com.focusfit.app.domain.SuggestionRule.OFFICE_DAY
import com.focusfit.app.domain.SuggestionRule.POST_TRAINING
import com.focusfit.app.domain.SuggestionRule.PRE_TRAINING
import com.focusfit.app.domain.SuggestionRule.PROTEIN_HABIT
import com.focusfit.app.domain.SuggestionRule.READING_EVENING
import com.focusfit.app.domain.SuggestionRule.TOO_HARD
import com.focusfit.app.domain.SuggestionRule.WATER_BEHIND
import com.focusfit.app.domain.model.UserProfile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime

class SuggestionsTest {

    private val monday = LocalDate.of(2026, 11, 16)    // Krav Maga 19:00
    private val wednesday = monday.plusDays(2)          // office, after Mon+Tue training
    private val thursday = monday.plusDays(3)           // Core A

    private fun input(
        date: LocalDate,
        time: String,
        today: Map<String, TaskProgress> = emptyMap(),
        yesterday: Map<String, TaskProgress> = emptyMap(),
        proteinMissed: Int = 0,
        rpe: List<Int> = emptyList(),
    ) = SuggestionInput(date.atTime(LocalTime.parse(time)), UserProfile.REFERENCE, 6_000, today, yesterday, proteinMissed, rpe)

    private fun rules(i: SuggestionInput) = SuggestionEngine.evaluate(i).map { it.rule }

    private val checked = TaskProgress(checked = true)

    @Test fun `pre and post training around krav maga`() {
        assertTrue(PRE_TRAINING in rules(input(monday, "16:30")))
        assertFalse(PRE_TRAINING in rules(input(monday, "18:30")))
        assertTrue(POST_TRAINING in rules(input(monday, "20:45")))
        assertFalse(POST_TRAINING in rules(input(monday, "23:45")))
    }

    @Test fun `evening steps suggests a walk length`() {
        val s = SuggestionEngine.evaluate(input(thursday, "19:00", today = mapOf("steps" to TaskProgress(value = 2_000))))
            .first { it.rule == EVENING_STEPS }
        assertTrue(s.text, s.text.startsWith("A 40-min walk"))
        assertFalse(EVENING_STEPS in rules(input(thursday, "19:00", today = mapOf("steps" to TaskProgress(value = 4_000)))))
        assertFalse(EVENING_STEPS in rules(input(thursday, "12:00", today = mapOf("steps" to TaskProgress(value = 1_000)))))
    }

    @Test fun `wednesday after two training days is a recovery office day`() {
        val r = rules(input(wednesday, "09:00", yesterday = mapOf("session_crossfit" to checked)))
        assertTrue(BACK_TO_BACK in r)
        assertTrue(OFFICE_DAY in r)
        assertFalse(MISSED_SESSION in r)
    }

    @Test fun `missed crossfit yesterday offers quick core`() {
        val s = SuggestionEngine.evaluate(input(wednesday, "10:00")).first { it.rule == MISSED_SESSION }
        assertEquals(SuggestionAction.StartRoutine("quick_core"), s.action)
        assertTrue(s.title.contains("CrossFit"))
    }

    @Test fun `no missed-session nudge on a training day`() =
        assertFalse(MISSED_SESSION in rules(input(monday.plusDays(1), "10:00")))

    @Test fun `water, protein, hard workouts, reading`() {
        assertTrue(WATER_BEHIND in rules(input(thursday, "16:00", today = mapOf("water" to TaskProgress(value = 1_000)))))
        assertFalse(WATER_BEHIND in rules(input(thursday, "16:00", today = mapOf("water" to TaskProgress(value = 2_000)))))
        assertTrue(PROTEIN_HABIT in rules(input(thursday, "12:00", proteinMissed = 3)))
        assertFalse(PROTEIN_HABIT in rules(input(thursday, "12:00", proteinMissed = 3, today = mapOf("protein" to checked))))
        assertTrue(TOO_HARD in rules(input(thursday, "12:00", rpe = listOf(6, 9, 10))))
        assertFalse(TOO_HARD in rules(input(thursday, "12:00", rpe = listOf(9, 7))))
        assertTrue(READING_EVENING in rules(input(thursday, "22:00")))
        assertFalse(READING_EVENING in rules(input(thursday, "22:00", today = mapOf("reading" to checked))))
    }

    @Test fun `weight plateau after 3 flat weeks`() {
        val weights = (0..3).map { WeightEntry(thursday.minusWeeks(3L - it), 84.0 - it * 0.1) }
        val r = rules(input(thursday, "12:00").copy(weights = weights))
        assertTrue(SuggestionRule.WEIGHT_PLATEAU in r)
    }

    @Test fun `results are ordered by priority`() {
        val r = rules(input(wednesday, "09:00"))
        assertEquals(r.sortedBy { it.ordinal }, r)
        assertEquals(MISSED_SESSION, r.first())
    }
}
