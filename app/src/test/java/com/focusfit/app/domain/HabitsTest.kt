package com.focusfit.app.domain

import com.focusfit.app.domain.model.UserProfile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime

class HabitsTest {

    private val wednesday = LocalDate.of(2026, 11, 18)

    @Test fun `step goal rises after 5 good days, stays otherwise, caps at 10k`() {
        assertEquals(7_000, StepGoals.evaluate(6_000, listOf(6_500, 7_000, 6_000, 8_000, 6_100, 2_000, 0)))
        assertEquals(6_000, StepGoals.evaluate(6_000, listOf(6_500, 7_000, 6_000, 8_000, 3_000, 2_000)))
        assertEquals(10_000, StepGoals.evaluate(10_000, List(7) { 12_000 }))
        assertEquals(6_000, StepGoals.evaluate(6_000, emptyList()))
    }

    @Test fun `steps task uses goal override and is done by count or manual tick`() {
        val steps = DailyTaskGenerator.tasksFor(UserProfile.REFERENCE, wednesday, emptyList(), stepGoal = 8_000)
            .first { it.id == "steps" }
        assertEquals(TaskKind.Steps(8_000), steps.kind)
        assertFalse(steps.isDone(TaskProgress(value = 7_999)))
        assertTrue(steps.isDone(TaskProgress(value = 8_000)))
        assertTrue(steps.isDone(TaskProgress(checked = true)))
    }

    @Test fun `sleep task follows bedtime setting`() {
        val sleep = DailyTaskGenerator.tasksFor(UserProfile.REFERENCE, wednesday, emptyList(), bedTime = LocalTime.of(23, 0))
            .first { it.id == "sleep" }
        assertEquals("In bed by 23:00", sleep.title)
    }

    @Test fun `reading stats`() {
        val sessions = listOf(
            ReadingSession(wednesday, 1, 20, 12),
            ReadingSession(wednesday.minusDays(1), 1, 15, 10),
            ReadingSession(wednesday.minusDays(2), 1, 10, 6),     // Monday
            ReadingSession(wednesday.minusDays(4), 1, 30, 20),    // Saturday last week (streak broken on Sunday)
            ReadingSession(wednesday.minusMonths(1), 1, 30, 25),  // previous month
        )
        val books = listOf(
            Book(1, "A", 300, 120),
            Book(2, "B", 200, 200, finishedOn = LocalDate.of(2026, 3, 1)),
            Book(3, "C", 150, 150, finishedOn = LocalDate.of(2025, 12, 30)),
        )
        val s = ReadingStats.of(sessions, books, wednesday)
        assertEquals(20, s.todayMinutes)
        assertEquals(45, s.weekMinutes)
        assertEquals(48, s.monthPages)
        assertEquals(1, s.booksThisYear)
        assertEquals(3, s.streak)
        assertEquals(0.4f, books[0].progress, 0.001f)
    }

    @Test fun `reading streak can end yesterday`() {
        val s = ReadingStats.of(listOf(ReadingSession(wednesday.minusDays(1), null, 10, 5)), emptyList(), wednesday)
        assertEquals(1, s.streak)
        assertEquals(0, s.todayMinutes)
    }
}
