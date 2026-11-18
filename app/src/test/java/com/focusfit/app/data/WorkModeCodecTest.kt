package com.focusfit.app.data

import com.focusfit.app.domain.model.WorkMode
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.DayOfWeek.MONDAY
import java.time.DayOfWeek.SUNDAY
import java.time.DayOfWeek.WEDNESDAY

class WorkModeCodecTest {

    @Test fun `round trip drops OFF days`() {
        val modes = mapOf(WEDNESDAY to WorkMode.OFFICE, MONDAY to WorkMode.REMOTE, SUNDAY to WorkMode.OFF)
        val encoded = ProfileRepository.encodeWorkModes(modes)
        assertEquals("MONDAY=REMOTE;WEDNESDAY=OFFICE", encoded)
        assertEquals(modes - SUNDAY, ProfileRepository.decodeWorkModes(encoded))
    }

    @Test fun `empty string decodes to empty map`() =
        assertEquals(emptyMap<Any, Any>(), ProfileRepository.decodeWorkModes(""))
}
