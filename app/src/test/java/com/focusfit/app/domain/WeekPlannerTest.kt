package com.focusfit.app.domain

import com.focusfit.app.domain.model.ScheduledActivity
import com.focusfit.app.domain.model.UserProfile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.DayOfWeek.FRIDAY
import java.time.DayOfWeek.MONDAY
import java.time.DayOfWeek.SATURDAY
import java.time.DayOfWeek.SUNDAY
import java.time.DayOfWeek.THURSDAY
import java.time.DayOfWeek.TUESDAY
import java.time.DayOfWeek.WEDNESDAY

class WeekPlannerTest {

    private val week = WeekPlanner.plan(UserProfile.REFERENCE).associateBy { it.day }

    private fun types(day: DayOfWeek) = week.getValue(day).sessions.map { it.type }
    private fun routines(day: DayOfWeek) = week.getValue(day).sessions.mapNotNull { it.routineId }

    @Test fun `user sports are kept on their own days`() {
        assertEquals("Krav Maga", week.getValue(MONDAY).sessions.single().title)
        assertEquals("CrossFit", week.getValue(TUESDAY).sessions.single().title)
        assertEquals("Krav Maga", week.getValue(FRIDAY).sessions.single().title)
    }

    @Test fun `office wednesday after back-to-back training gets lunch walk and mobility`() {
        assertEquals(listOf(SessionType.WALK, SessionType.MOBILITY), types(WEDNESDAY))
        assertTrue(week.getValue(WEDNESDAY).sessions.first().title.contains("Lunch walk"))
    }

    @Test fun `core A on thursday and core B on saturday`() {
        assertEquals(listOf(WeekPlanner.CORE_A), routines(THURSDAY))
        assertEquals(listOf(WeekPlanner.CORE_B), routines(SATURDAY))
        assertEquals(60, week.getValue(SATURDAY).sessions.first { it.type == SessionType.WALK }.durationMin)
    }

    @Test fun `sunday is rest`() = assertEquals(listOf(SessionType.REST), types(SUNDAY))

    @Test fun `core sessions are never on consecutive days`() {
        val profile = UserProfile.REFERENCE.copy(
            activities = listOf(ScheduledActivity("Run", "🏃", setOf(SATURDAY), null, 45)),
        )
        val coreDays = WeekPlanner.plan(profile)
            .filter { d -> d.sessions.any { it.type == SessionType.CORE } }
            .map { it.day }
        assertEquals(2, coreDays.size)
        assertTrue(coreDays[1].value - coreDays[0].value > 1)
    }
}
