package com.focusfit.app.domain

import com.focusfit.app.domain.model.Goal
import com.focusfit.app.domain.model.UserProfile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class ProgressTest {

    private val start = LocalDate.of(2026, 10, 26) // a Monday
    private val ref = UserProfile.REFERENCE

    /** Weekly weigh-ins (Mondays) losing [perWeek] kg per week from 84 kg. */
    private fun weekly(weeks: Int, perWeek: Double) =
        (0 until weeks).map { WeightEntry(start.plusWeeks(it.toLong()), 84.0 - it * perWeek) }

    @Test fun `moving average smooths daily noise`() {
        val entries = listOf(84.0, 85.0, 83.0, 84.0).mapIndexed { i, kg -> WeightEntry(start.plusDays(i.toLong()), kg) }
        val avg = WeightTrend.movingAverage(entries)
        assertEquals(84.5, avg[1].second, 1e-9)
        assertEquals(84.0, avg[3].second, 1e-9)
    }

    @Test fun `rate per week and weeks to target`() {
        val e = weekly(4, 0.5)
        val today = start.plusWeeks(3)
        assertEquals(-0.5, WeightTrend.ratePerWeek(e, today)!!, 1e-9)
        assertEquals(5, WeightTrend.weeksToTarget(82.5, 80.0, -0.5))
        assertNull(WeightTrend.weeksToTarget(82.5, 80.0, +0.2)) // moving away
        assertNull(WeightTrend.ratePerWeek(e.take(1), start)) // not enough history
    }

    @Test fun `plateau and too-fast advice`() {
        val today = start.plusWeeks(3)
        assertEquals(WeightAdvice.Kind.PLATEAU, WeightAdvice.check(ref, weekly(4, 0.1), today))
        assertNull(WeightAdvice.check(ref, weekly(4, 0.5), today))
        assertEquals(WeightAdvice.Kind.TOO_FAST, WeightAdvice.check(ref, weekly(4, 1.2), today))
        assertNull(WeightAdvice.check(ref.copy(goal = Goal.MAINTAIN), weekly(4, 0.1), today))
        assertNull(WeightAdvice.check(ref, weekly(2, 0.1), start.plusWeeks(1))) // too little data
    }

    @Test fun `weekly review counts sessions, steps and builds advice`() {
        val days = (0L..6L).map { start.plusDays(it) }
        val review = WeeklyReview.build(
            profile = ref,
            weekStart = start,
            ratios = days.associateWith { 0.9 },
            sessionChecks = mapOf(
                start to setOf("session_krav maga"),
                start.plusDays(1) to setOf("session_crossfit"),
                start.plusDays(4) to setOf("session_krav maga"),
            ),
            steps = days.associateWith { 7_000 },
            readingMinutes = 90,
            weights = listOf(WeightEntry(start.minusDays(1), 84.5), WeightEntry(start.plusDays(6), 84.0)),
            stepGoal = 6_000,
        )
        // Mon KM, Tue CF, Wed walk+mobility, Thu walk+core A, Fri KM, Sat walk+core B → 9 planned
        assertEquals(9, review.sessionsPlanned)
        assertEquals(3, review.sessionsDone)
        assertEquals(7_000, review.avgSteps)
        assertEquals(7_000, review.nextStepGoal)
        assertEquals(0.9, review.completion, 1e-9)
        assertTrue(review.wins.any { it.contains("7,000") })
        assertTrue(review.focus.any { it.startsWith("Sessions: 3 of 9") })
        assertEquals(-0.5, review.weightChange!!, 1e-9)
    }
}
