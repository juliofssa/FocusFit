package com.focusfit.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class ContentTest {

    private val workouts = LibraryParser.workouts(File("src/main/assets/workouts.json").readText())
    private val meals = LibraryParser.meals(File("src/main/assets/meals.json").readText())

    @Test fun `bundled content pack is valid`() = assertEquals(emptyList<String>(), ContentValidator.validate(workouts, meals))

    @Test fun `published content pack is valid and its manifest matches the files`() {
        val dir = File("../content-pack")
        fun lf(name: String) = File(dir, name).readText().replace("\r\n", "\n")
        val manifest = ContentManifest.parse(File(dir, "manifest.json").readText())
        assertTrue(manifest.version >= 1)
        for (name in listOf(ContentManifest.WORKOUTS, ContentManifest.MEALS)) {
            assertEquals(name, manifest.files[name], com.focusfit.app.data.LibraryRepository.sha256(lf(name)))
        }
        val errors = ContentValidator.validate(LibraryParser.workouts(lf(ContentManifest.WORKOUTS)), LibraryParser.meals(lf(ContentManifest.MEALS)))
        assertEquals(emptyList<String>(), errors)
    }

    @Test fun `every bundled exercise has a demo video`() =
        assertTrue(workouts.exercises.filter { it.youtubeId == null }.map { it.id }.toString(), workouts.exercises.all { it.youtubeId != null })

    @Test fun `broken packs are rejected with clear messages`() {
        val broken = workouts.copy(
            exercises = workouts.exercises.map { if (it.id == "plank") it.copy(youtubeId = "nope", harderId = "ghost") else it },
            routines = workouts.routines.filter { it.id != WeekPlanner.CORE_B },
        )
        val errors = ContentValidator.validate(broken, meals.copy(meals = meals.meals.filter { it.type != "SNACK" }))
        assertTrue(errors.any { "youtubeId 'nope'" in it })
        assertTrue(errors.any { "harderId 'ghost'" in it })
        assertTrue(errors.any { "Missing routine 'core_b'" in it })
        assertTrue(errors.any { "No meal ideas of type SNACK" in it })
    }

    @Test fun `manifest parses and ignores unknown fields`() {
        val m = ContentManifest.parse("""{"version":7,"files":{"workouts.json":"ab","meals.json":"cd"},"extra":1}""")
        assertEquals(7, m.version)
        assertEquals(ContentManifest.SUPPORTED_FORMAT, m.format)
        assertEquals("ab", m.files[ContentManifest.WORKOUTS])
    }

    @Test fun `youtube links are parsed into ids`() {
        assertEquals("GbSC02oU3To", YouTube.idFrom("https://www.youtube.com/watch?v=GbSC02oU3To&t=12s"))
        assertEquals("GbSC02oU3To", YouTube.idFrom("https://youtu.be/GbSC02oU3To?si=abc"))
        assertEquals("eyeuugrpLYA", YouTube.idFrom("https://youtube.com/shorts/eyeuugrpLYA"))
        assertEquals("GbSC02oU3To", YouTube.idFrom("GbSC02oU3To"))
        assertEquals(null, YouTube.idFrom("https://example.com/video"))
    }
}
