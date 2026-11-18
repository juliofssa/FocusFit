package com.focusfit.app.domain

import com.focusfit.app.domain.ProfileValidator.Field
import com.focusfit.app.domain.model.Goal
import com.focusfit.app.domain.model.ScheduledActivity
import com.focusfit.app.domain.model.UserProfile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek

class ProfileValidatorTest {

    private val ref = UserProfile.REFERENCE

    @Test fun `reference profile is valid`() = assertTrue(ProfileValidator.validateAll(ref).isEmpty())

    @Test fun `basics out of range`() {
        val errors = ProfileValidator.validateBasics(ref.copy(age = 9, heightCm = 300.0, weightKg = 20.0))
        assertEquals(setOf(Field.AGE, Field.HEIGHT, Field.WEIGHT), errors.keys)
    }

    @Test fun `lose goal needs lower target`() =
        assertTrue(Field.TARGET_WEIGHT in ProfileValidator.validateGoal(ref.copy(targetWeightKg = 90.0)))

    @Test fun `gain goal needs higher target`() =
        assertTrue(Field.TARGET_WEIGHT in ProfileValidator.validateGoal(ref.copy(goal = Goal.GAIN, targetWeightKg = 80.0)))

    @Test fun `target below healthy bmi is rejected`() =
        assertTrue(Field.TARGET_WEIGHT in ProfileValidator.validateGoal(ref.copy(targetWeightKg = 50.0)))

    @Test fun `maintain ignores direction`() =
        assertTrue(ProfileValidator.validateGoal(ref.copy(goal = Goal.MAINTAIN, targetWeightKg = 84.0)).isEmpty())

    @Test fun `activities need name and days`() {
        val noDays = ScheduledActivity("Yoga", "🧘", emptySet(), null, 60)
        val noName = ScheduledActivity(" ", "🏅", setOf(DayOfWeek.MONDAY), null, 60)
        assertTrue(Field.ACTIVITIES in ProfileValidator.validateActivities(listOf(noDays)))
        assertTrue(Field.ACTIVITIES in ProfileValidator.validateActivities(listOf(noName)))
        assertTrue(ProfileValidator.validateActivities(emptyList()).isEmpty())
    }

    @Test fun `sport emoji guesses`() {
        assertEquals("🥋", sportEmoji("Krav Maga"))
        assertEquals("🏋️", sportEmoji("CrossFit"))
        assertEquals("🎾", sportEmoji("Padel"))
        assertEquals("🏅", sportEmoji("Climbing"))
    }
}
