package com.focusfit.app.domain

import com.focusfit.app.domain.model.UserProfile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate

class DailyTasksTest {

    private val monday = LocalDate.of(2026, 11, 16)
    private val sunday = LocalDate.of(2026, 11, 22)
    private val ref = UserProfile.REFERENCE

    private fun ids(date: LocalDate, custom: List<CustomTask> = emptyList()) =
        DailyTaskGenerator.tasksFor(ref, date, custom).map { it.id }

    @Test fun `monday has krav maga plus daily habits`() {
        assertEquals(
            listOf("session_krav maga", "steps", "reading", "protein", "water", "sleep"),
            ids(monday),
        )
    }

    @Test fun `thursday has walk and core A`() {
        val thursday = monday.plusDays(3)
        assertTrue("session_core_a" in ids(thursday))
        assertTrue("session_walk" in ids(thursday))
    }

    @Test fun `rest day has no session task`() = assertTrue(ids(sunday).none { it.startsWith("session_") })

    @Test fun `water target is higher on training days`() {
        fun waterTarget(d: LocalDate) = (DailyTaskGenerator.tasksFor(ref, d, emptyList())
            .first { it.id == "water" }.kind as TaskKind.Counter).target
        assertEquals(3_500, waterTarget(monday))
        assertEquals(3_000, waterTarget(sunday))
    }

    @Test fun `custom tasks follow their schedule`() {
        val daily = CustomTask(1, "Stretch")
        val weekdays = CustomTask(2, "Pack lunch", days = setOf(DayOfWeek.WEDNESDAY))
        val once = CustomTask(3, "Buy shoes", oneOffDate = monday)
        val custom = listOf(daily, weekdays, once)
        assertEquals(listOf("custom_1", "custom_3"), ids(monday, custom).filter { it.startsWith("custom") })
        assertEquals(listOf("custom_1", "custom_2"), ids(monday.plusDays(2), custom).filter { it.startsWith("custom") })
    }

    @Test fun `done rules for check and counter`() {
        val check = TaskDef("a", "", "", null)
        val water = TaskDef("w", "", "", null, TaskKind.Counter(3_000, 250, "ml"))
        assertFalse(check.isDone(null))
        assertTrue(check.isDone(TaskProgress(checked = true)))
        assertFalse(water.isDone(TaskProgress(value = 2_750)))
        assertTrue(water.isDone(TaskProgress(value = 3_000)))
    }

    @Test fun `streak counts back from today or yesterday`() {
        val today = LocalDate.of(2026, 11, 18)
        val ratios = mapOf(
            today.minusDays(1) to 1.0,
            today.minusDays(2) to 0.85,
            today.minusDays(3) to 0.5,
            today.minusDays(4) to 1.0,
        )
        assertEquals(2, Streaks.current(ratios, today))                    // today not good yet
        assertEquals(3, Streaks.current(ratios + (today to 0.8), today))   // today counts once good
        assertEquals(0, Streaks.current(emptyMap(), today))
    }

    @Test fun `week score averages monday to today`() {
        val wednesday = LocalDate.of(2026, 11, 18)
        val ratios = mapOf(monday to 1.0, monday.plusDays(1) to 0.5) // Wednesday missing = 0
        assertEquals(0.5, Streaks.weekScore(ratios, wednesday), 1e-9)
        assertEquals(1.0, Streaks.weekScore(ratios, monday), 1e-9)
    }
}
