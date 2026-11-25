package com.focusfit.app.ui.theme

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Text must stay readable: every text/background pair used in the app meets WCAG AA (≥ 4.5:1). */
class PaletteTest {

    private val aa = 4.5

    private fun pairs(s: Scheme) = mapOf(
        "onSurface / surface" to (s.onSurface to s.surface),
        "onBackground / background" to (s.onBackground to s.background),
        "onSurfaceVariant (hints) / surface" to (s.onSurfaceVariant to s.surface),
        // Cards use surfaceContainerHighest; hints and coloured labels sit on them.
        "onSurface / card" to (s.onSurface to s.surfaceContainerHighest),
        "onSurfaceVariant (hints) / card" to (s.onSurfaceVariant to s.surfaceContainerHighest),
        "primary text / card" to (s.primary to s.surfaceContainerHighest),
        "tertiary text / card" to (s.tertiary to s.surfaceContainerHighest),
        "error text / card" to (s.error to s.surfaceContainerHighest),
        "primary text / surface" to (s.primary to s.surface),
        "onPrimary / primary (buttons)" to (s.onPrimary to s.primary),
        "onPrimaryContainer / primaryContainer (done cards)" to (s.onPrimaryContainer to s.primaryContainer),
        "onSurfaceVariant / primaryContainer (hints on done cards)" to (s.onSurfaceVariant to s.primaryContainer),
        "onSecondaryContainer / secondaryContainer (banner, rest)" to (s.onSecondaryContainer to s.secondaryContainer),
        "onTertiaryContainer / tertiaryContainer" to (s.onTertiaryContainer to s.tertiaryContainer),
        "onSecondary / secondary" to (s.onSecondary to s.secondary),
        "onTertiary / tertiary" to (s.onTertiary to s.tertiary),
        "onErrorContainer / errorContainer" to (s.onErrorContainer to s.errorContainer),
        "onSurface / surfaceContainer (nav bar)" to (s.onSurface to s.surfaceContainer),
        "onSurfaceVariant / surfaceContainer (nav bar labels)" to (s.onSurfaceVariant to s.surfaceContainer),
        "inverseOnSurface / inverseSurface (snackbars)" to (s.inverseOnSurface to s.inverseSurface),
    )

    private fun assertReadable(name: String, s: Scheme) {
        val failing = pairs(s).mapValues { (_, p) -> Palette.contrast(p.first, p.second) }.filterValues { it < aa }
        assertTrue("$name scheme has low-contrast pairs: $failing", failing.isEmpty())
    }

    @Test fun `light scheme is readable`() = assertReadable("Light", Palette.light)

    @Test fun `dark scheme is readable`() = assertReadable("Dark", Palette.dark)

    @Test fun `contrast formula matches WCAG reference values`() {
        assertEquals(21.0, Palette.contrast(0xFF000000, 0xFFFFFFFF), 0.01)
        assertEquals(1.0, Palette.contrast(0xFF777777, 0xFF777777), 0.001)
        assertEquals(4.48, Palette.contrast(0xFF777777, 0xFFFFFFFF), 0.01)
    }
}
