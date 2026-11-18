package com.focusfit.app.domain

import com.focusfit.app.domain.model.UserProfile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime

class RemindersTest {

    private val ref = UserProfile.REFERENCE
    private val monday = LocalDate.of(2026, 11, 16)
    private val settings = NotificationSettings()
    private fun plan(date: LocalDate, s: NotificationSettings = settings, nowTime: LocalTime = LocalTime.MIDNIGHT) =
        ReminderPlanner.remindersFor(ref, date, s, date.atTime(nowTime))

    @Test fun `monday has krav maga reminders 2h and 30min before`() {
        val training = plan(monday).filter { it.category == ReminderCategory.TRAINING }
        assertEquals(listOf(LocalTime.of(17, 0), LocalTime.of(18, 30)), training.map { it.time.toLocalTime() })
        assertTrue(training.all { it.taskId == "session_krav maga" })
    }

    @Test fun `thursday core reminder opens the routine`() {
        val core = plan(monday.plusDays(3)).single { it.category == ReminderCategory.TRAINING }
        assertEquals(WeekPlanner.CORE_A, core.routineId)
        assertEquals(LocalTime.of(18, 0), core.time.toLocalTime())
    }

    @Test fun `water every 150 min from wake plus 2h until 20h`() {
        val water = plan(monday).filter { it.category == ReminderCategory.WATER }.map { it.time.toLocalTime() }
        assertEquals(listOf("10:00", "12:30", "15:00", "17:30", "20:00").map(LocalTime::parse), water)
    }

    @Test fun `ids are unique`() {
        val all = plan(monday.plusDays(6))
        assertEquals(all.size, all.map { it.id }.toSet().size)
    }

    @Test fun `weekly review on sunday, weigh-in on monday morning`() {
        assertTrue(plan(monday.plusDays(6)).any { it.category == ReminderCategory.WEEKLY && it.title.contains("review") })
        val weighIn = plan(monday).single { it.category == ReminderCategory.WEEKLY }
        assertEquals(LocalTime.of(8, 15), weighIn.time.toLocalTime())
        assertFalse(plan(monday.plusDays(2)).any { it.category == ReminderCategory.WEEKLY })
    }

    @Test fun `past reminders and disabled categories are dropped`() {
        val afternoon = plan(monday, nowTime = LocalTime.of(16, 0))
        assertTrue(afternoon.all { it.time.toLocalTime() > LocalTime.of(16, 0) })
        val noWater = plan(monday, settings.copy(enabled = ReminderCategory.entries.toSet() - ReminderCategory.WATER))
        assertTrue(noWater.none { it.category == ReminderCategory.WATER })
    }

    @Test fun `nudge checks run hourly from wake plus 1h until 21h`() {
        val times = ReminderPlanner.nudgeTimes(monday, settings, monday.atStartOfDay()).map { it.toLocalTime() }
        assertEquals(LocalTime.of(9, 15), times.first())
        assertEquals(LocalTime.of(20, 15), times.last())
        assertEquals(12, times.size)
        val off = settings.copy(enabled = settings.enabled - ReminderCategory.NUDGES)
        assertTrue(ReminderPlanner.nudgeTimes(monday, off, monday.atStartOfDay()).isEmpty())
        assertEquals(4, ReminderPlanner.nudgeTimes(monday, settings, monday.atTime(17, 0)).size)
    }

    @Test fun `quiet hours wrap past midnight`() {
        assertTrue(settings.isQuiet(LocalTime.of(23, 30)))
        assertTrue(settings.isQuiet(LocalTime.of(6, 0)))
        assertFalse(settings.isQuiet(LocalTime.of(7, 30)))
        assertFalse(settings.isQuiet(LocalTime.of(22, 59)))
        val early = settings.copy(quietStart = LocalTime.of(21, 0))
        assertTrue(plan(monday, early).none { it.time.toLocalTime() >= LocalTime.of(21, 0) })
    }
}
