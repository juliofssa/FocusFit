package com.focusfit.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.time.LocalDate

class WorkoutTest {

    // Unit tests run with the module dir (app/) as working directory.
    private val library = LibraryParser.workouts(File("src/main/assets/workouts.json").readText())
    private val meals = LibraryParser.meals(File("src/main/assets/meals.json").readText())

    @Test fun `bundled json is consistent`() {
        val ids = library.exercises.map { it.id }.toSet()
        library.exercises.forEach { e ->
            assertTrue("${e.id} needs reps or seconds", e.reps != null || e.seconds != null)
            e.easierId?.let { assertTrue("${e.id}.easierId", it in ids) }
            e.harderId?.let { assertTrue("${e.id}.harderId", it in ids) }
        }
        library.routines.flatMap { it.steps }.forEach { assertTrue(it.exerciseId, it.exerciseId in ids) }
        listOf(WeekPlanner.CORE_A, WeekPlanner.CORE_B, WeekPlanner.MOBILITY, "quick_core").forEach {
            assertNotNull("routine $it", library.routine(it))
        }
        assertTrue(meals.meals.size >= 10)
    }

    @Test fun `core A level 1 expands sets with rests`() {
        val steps = SessionBuilder.build(library.routine(WeekPlanner.CORE_A)!!, library)
        val work = steps.filter { !it.isRest }
        // dead bug 3 + plank 3 + side plank 2×(L+R) + bird dog 2 + bridge 3
        assertEquals(3 + 3 + 4 + 2 + 3, work.size)
        assertEquals(30, work.first { it.exercise!!.id == "plank" }.seconds)
        assertEquals(8, work.first().reps)
        assertFalse(steps.last().isRest)
        assertTrue(SessionBuilder.estimatedMinutes(steps) in 10..20)
    }

    @Test fun `core B is a 3-round circuit`() {
        val steps = SessionBuilder.build(library.routine(WeekPlanner.CORE_B)!!, library)
        assertEquals(15, steps.count { !it.isRest })
        assertEquals(2, steps.count { it.isRest && it.seconds == 60 })
        assertEquals("Round 3 of 3", steps.last().setLabel)
    }

    @Test fun `level 2 adds time and reps, level 3 uses harder variants and an extra round`() {
        val a2 = SessionBuilder.build(library.routine(WeekPlanner.CORE_A)!!, library, level = 2).filter { !it.isRest }
        assertEquals(40, a2.first { it.exercise!!.id == "plank" }.seconds)
        assertEquals(10, a2.first().reps)

        val a3 = SessionBuilder.build(library.routine(WeekPlanner.CORE_A)!!, library, level = 3).filter { !it.isRest }
        assertTrue(a3.any { it.exercise!!.id == "plank_shoulder_taps" })
        assertTrue(a3.none { it.exercise!!.id == "plank" })

        val b3 = SessionBuilder.build(library.routine(WeekPlanner.CORE_B)!!, library, level = 3)
        assertEquals("Round 4 of 4", b3.last().setLabel)
    }

    @Test fun `swap replaces an exercise in every set`() {
        val steps = SessionBuilder.build(library.routine(WeekPlanner.CORE_A)!!, library, swaps = mapOf(1 to "plank_knees"))
        assertEquals(3, steps.count { it.exercise?.id == "plank_knees" })
        assertEquals(0, steps.count { it.exercise?.id == "plank" })
    }

    @Test fun `progression levels up after 4 easy sessions and flags too hard`() {
        val d = LocalDate.of(2026, 11, 1)
        fun log(i: Long, level: Int, rpe: Int) = WorkoutLog(d.plusDays(i), "core_a", level, 900, rpe)

        assertEquals(LevelStatus(1, 0, false), Progression.status(emptyList()))
        val threeEasy = (0L..2).map { log(it, 1, 6) } + log(3, 1, 8)
        assertEquals(LevelStatus(1, 3, false), Progression.status(threeEasy))
        val fourEasy = threeEasy + log(4, 1, 7)
        assertEquals(LevelStatus(2, 0, false), Progression.status(fourEasy))
        val hard = fourEasy + log(5, 2, 9) + log(6, 2, 10)
        assertTrue(Progression.status(hard).tooHard)
        val maxed = (0L..11).map { log(it, if (it < 4) 1 else if (it < 8) 2 else 3, 5) }
        assertEquals(3, Progression.status(maxed).level)
    }
}
