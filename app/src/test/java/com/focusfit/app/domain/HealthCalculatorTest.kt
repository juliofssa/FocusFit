package com.focusfit.app.domain

import com.focusfit.app.domain.model.Sex
import com.focusfit.app.domain.model.UserProfile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HealthCalculatorTest {

    private val t = HealthCalculator.targets(UserProfile.REFERENCE)

    @Test fun `bmi of reference profile`() = assertEquals(27.7, t.bmi, 0.01)

    @Test fun `mifflin st jeor bmr male`() =
        assertEquals(1767.5, HealthCalculator.bmr(Sex.MALE, 84.0, 174.0, 33), 0.01)

    @Test fun `mifflin st jeor bmr female`() =
        assertEquals(1601.5, HealthCalculator.bmr(Sex.FEMALE, 84.0, 174.0, 33), 0.01)

    @Test fun `healthy weight range for 174 cm`() {
        assertEquals(56.0, t.healthyWeightMinKg, 0.1)
        assertEquals(75.4, t.healthyWeightMaxKg, 0.1)
    }

    @Test fun `calorie target is a moderate deficit`() {
        assertTrue("kcal ${t.kcalAverage}", t.kcalAverage in 1_950..2_150)
        assertTrue(t.kcalAverage < t.tdee)
        assertTrue(t.kcalAverage >= t.bmr * 1.1 - 10)
        assertTrue(t.kcalTrainingDay > t.kcalAverage && t.kcalRestDay < t.kcalAverage)
    }

    @Test fun `macros of reference profile`() {
        assertEquals(151, t.proteinG)
        assertEquals(67, t.fatG)
        val kcalFromMacros = t.proteinG * 4 + t.fatG * 9 + t.carbsG * 4
        assertEquals(t.kcalAverage.toDouble(), kcalFromMacros.toDouble(), 20.0)
    }

    @Test fun `water target`() {
        assertEquals(3_000, t.waterMl)
        assertEquals(3_500, t.waterMlTrainingDay)
    }

    @Test fun `step goal starts at least 6k and progresses`() {
        assertEquals(6_000, t.stepGoal)
        assertEquals(9_000, HealthCalculator.initialStepGoal(8_100))
        assertEquals(10_000, HealthCalculator.initialStepGoal(14_000))
        assertEquals(7_000, HealthCalculator.nextStepGoal(6_000, daysGoalHit = 5))
        assertEquals(6_000, HealthCalculator.nextStepGoal(6_000, daysGoalHit = 4))
        assertEquals(10_000, HealthCalculator.nextStepGoal(10_000, daysGoalHit = 7))
    }

    @Test fun `activity factor table`() {
        assertEquals(1.2, HealthCalculator.activityFactor(0, 3_000), 0.0)
        assertEquals(1.375, HealthCalculator.activityFactor(3, 5_000), 0.0)
        assertEquals(1.465, HealthCalculator.activityFactor(3, 8_000), 0.0)
        assertEquals(1.55, HealthCalculator.activityFactor(6, 3_000), 0.0)
    }
}
