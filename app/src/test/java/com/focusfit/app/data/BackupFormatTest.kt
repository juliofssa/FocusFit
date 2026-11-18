package com.focusfit.app.data

import com.focusfit.app.data.local.ActivityEntity
import com.focusfit.app.data.local.ProfileEntity
import com.focusfit.app.data.local.TaskLogEntity
import com.focusfit.app.data.local.WeightEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class BackupFormatTest {

    private val backup = Backup(
        exportedAt = "2026-11-18",
        settings = BackupSettings(listOf("WATER", "NUDGES"), "08:00", "23:30", "23:00", "07:30", 7_000, "2026-11-16"),
        profiles = listOf(ProfileEntity(sex = "MALE", age = 33, heightCm = 174.0, weightKg = 84.0, goal = "LOSE",
            targetWeightKg = 78.0, pace = "STANDARD", avgDailySteps = 5_000, workModes = "WEDNESDAY=OFFICE")),
        activities = listOf(ActivityEntity(1, "Krav Maga", "🥋", "MONDAY,FRIDAY", 1_140, 90, 0)),
        taskLogs = listOf(TaskLogEntity("2026-11-18", "water", 1_500, false, 0)),
        weights = listOf(WeightEntity("2026-11-16", 84.0, null)),
    )

    @Test fun `round trip keeps everything`() = assertEquals(backup, Backup.decode(backup.encode()))

    @Test fun `unknown fields are ignored, missing tables default to empty`() {
        val text = """{"format":1,"exportedAt":"x","futureField":true,
            "settings":{"enabled":[],"wakeTime":"08:00","bedTime":"23:30","quietStart":"23:00","quietEnd":"07:30"}}"""
        val b = Backup.decode(text)
        assertTrue(b.weights.isEmpty() && b.profiles.isEmpty())
    }

    @Test fun `newer format is rejected`() {
        val newer = backup.copy(format = Backup.FORMAT + 1).encode()
        assertThrows(IllegalArgumentException::class.java) { Backup.decode(newer) }
    }
}
