package com.focusfit.app.domain

import com.focusfit.app.domain.model.DietStyle
import com.focusfit.app.domain.model.FoodTag
import com.focusfit.app.domain.model.TargetOverrides
import com.focusfit.app.domain.model.UserProfile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.time.LocalDate
import java.time.LocalTime

class CustomizationTest {

    private val ref = UserProfile.REFERENCE
    private val monday = LocalDate.of(2026, 11, 16)
    private val wednesday = monday.plusDays(2)

    // ---- Target overrides ----

    @Test fun `overrides replace calculated targets and macros follow`() {
        val p = ref.copy(targetOverrides = TargetOverrides(kcal = 2_400, proteinG = 170, waterMl = 2_800, steps = 12_000, readingMin = 30))
        val t = HealthCalculator.targets(p)
        assertEquals(2_400, t.kcalAverage)
        assertEquals(2_550, t.kcalTrainingDay)
        assertEquals(170, t.proteinG)
        assertEquals(2_800, t.waterMl)
        assertEquals(3_300, t.waterMlTrainingDay)
        assertEquals(12_000, t.stepGoal)
        assertEquals(30, t.readingMin)
        assertEquals(2_400.0, (t.proteinG * 4 + t.fatG * 9 + t.carbsG * 4).toDouble(), 20.0)
        // Suggested values ignore overrides
        assertEquals(HealthCalculator.targets(ref), HealthCalculator.suggestedTargets(p))
    }

    @Test fun `fixed step goal wins over weekly progression`() {
        val p = ref.copy(targetOverrides = TargetOverrides(steps = 9_000))
        val steps = DailyTaskGenerator.tasksFor(p, monday, emptyList(), stepGoal = 7_000).first { it.id == "steps" }
        assertEquals(TaskKind.Steps(9_000), steps.kind)
    }

    // ---- Diet ----

    @Test fun `diet styles and exclusions filter meals`() {
        val meals = LibraryParser.meals(File("src/main/assets/meals.json").readText()).meals
        fun ok(p: UserProfile) = meals.filter { MealFilter.allowed(it, p.excludedFoods) }
        val vegan = ok(ref.copy(dietStyle = DietStyle.VEGAN))
        assertTrue(vegan.isNotEmpty())
        assertTrue(vegan.none { m -> MealFilter.tagsOf(m).any { it in DietStyle.VEGAN.excludes } })
        // Every meal type still has at least one vegan idea.
        assertEquals(meals.map { it.type }.toSet(), vegan.map { it.type }.toSet())
        val noDairy = ok(ref.copy(foodExcludes = setOf(FoodTag.DAIRY)))
        assertTrue(noDairy.none { FoodTag.DAIRY in MealFilter.tagsOf(it) })
        assertEquals(meals.size, ok(ref).size)
    }

    // ---- One-off week changes ----

    private val kravMonday = PlanChange(date = monday, kind = PlanChangeKind.SKIP, key = "krav maga", moveGroup = "m1")
    private val kravOnWednesday = PlanChange(
        date = wednesday, kind = PlanChangeKind.ADD, key = "krav maga", moveGroup = "m1",
        session = PlannedSession(SessionType.SPORT, "Krav Maga", "🥋", 90, LocalTime.of(19, 0)),
    )

    @Test fun `moving a session only changes those two dates`() {
        val p = ref.copy(planChanges = listOf(kravMonday, kravOnWednesday))
        assertTrue(WeekPlanner.dayPlan(p, monday).sessions.isEmpty())
        val wed = WeekPlanner.dayPlan(p, wednesday)
        assertTrue(wed.isTrainingDay)
        assertTrue(wed.sessions.any { it.title == "Krav Maga" })
        // Next week is back to normal.
        assertTrue(WeekPlanner.dayPlan(p, monday.plusWeeks(1)).sessions.any { it.title == "Krav Maga" })
        // Today's to-dos and reminders follow the change.
        assertTrue(DailyTaskGenerator.tasksFor(p, wednesday, emptyList()).any { it.id == "session_krav maga" })
        assertTrue(ReminderPlanner.remindersFor(p, monday, NotificationSettings(), monday.atStartOfDay())
            .none { it.category == ReminderCategory.TRAINING })
    }

    @Test fun `adding to the rest day replaces the rest placeholder`() {
        val sunday = monday.plusDays(6)
        val add = PlanChange(date = sunday, kind = PlanChangeKind.ADD, key = WeekPlanner.MOBILITY,
            session = PlannedSession(SessionType.MOBILITY, "Mobility", "🧘", 10, routineId = WeekPlanner.MOBILITY))
        val day = WeekPlanner.dayPlan(ref.copy(planChanges = listOf(add)), sunday)
        assertEquals(listOf(SessionType.MOBILITY), day.sessions.map { it.type })
    }

    @Test fun `week of applies changes per date`() {
        val week = WeekPlanner.weekOf(ref.copy(planChanges = listOf(kravMonday)), monday)
        assertEquals(7, week.size)
        assertEquals(monday, week.first().date)
        assertTrue(week.first().sessions.isEmpty())
    }

    // ---- Reminder times & onboarding ----

    @Test fun `custom reminder times are used`() {
        val s = NotificationSettings(homeWorkoutTime = LocalTime.of(7, 45), readingTime = LocalTime.of(21, 0), lastWaterTime = LocalTime.of(15, 0))
        val thursday = monday.plusDays(3)
        val r = ReminderPlanner.remindersFor(ref, thursday, s, thursday.atStartOfDay())
        assertEquals(LocalTime.of(7, 45), r.first { it.category == ReminderCategory.TRAINING }.time.toLocalTime())
        assertEquals(LocalTime.of(21, 0), r.first { it.category == ReminderCategory.READING }.time.toLocalTime())
        assertEquals(LocalTime.of(15, 0), r.filter { it.category == ReminderCategory.WATER }.maxOf { it.time }.toLocalTime())
    }

    @Test fun `new users start blank and must fill in their details`() {
        assertTrue(UserProfile.NEW_USER.activities.isEmpty())
        assertFalse(ProfileValidator.validateBasics(UserProfile.NEW_USER).isEmpty())
    }
}
