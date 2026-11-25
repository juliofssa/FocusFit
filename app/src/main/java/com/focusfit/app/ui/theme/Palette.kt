package com.focusfit.app.ui.theme

import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

/**
 * FocusFit colours as plain ARGB values (no Compose types) so their contrast can be unit-tested.
 * Every text/background pair the app uses meets WCAG AA (≥ 4.5:1), see PaletteTest.
 */
data class Scheme(
    val primary: Long, val onPrimary: Long, val primaryContainer: Long, val onPrimaryContainer: Long,
    val secondary: Long, val onSecondary: Long, val secondaryContainer: Long, val onSecondaryContainer: Long,
    val tertiary: Long, val onTertiary: Long, val tertiaryContainer: Long, val onTertiaryContainer: Long,
    val error: Long, val onError: Long, val errorContainer: Long, val onErrorContainer: Long,
    val background: Long, val onBackground: Long,
    val surface: Long, val onSurface: Long, val surfaceVariant: Long, val onSurfaceVariant: Long,
    val surfaceContainerLowest: Long, val surfaceContainerLow: Long, val surfaceContainer: Long,
    val surfaceContainerHigh: Long, val surfaceContainerHighest: Long,
    val outline: Long, val outlineVariant: Long,
    val inverseSurface: Long, val inverseOnSurface: Long, val inversePrimary: Long,
)

object Palette {

    val light = Scheme(
        primary = 0xFF1E6B4A, onPrimary = 0xFFFFFFFF, primaryContainer = 0xFFB4EFCB, onPrimaryContainer = 0xFF002113,
        secondary = 0xFF8A4E00, onSecondary = 0xFFFFFFFF, secondaryContainer = 0xFFFFDDB8, onSecondaryContainer = 0xFF2C1600,
        tertiary = 0xFF2B5B92, onTertiary = 0xFFFFFFFF, tertiaryContainer = 0xFFD4E3FF, onTertiaryContainer = 0xFF001C3A,
        error = 0xFFBA1A1A, onError = 0xFFFFFFFF, errorContainer = 0xFFFFDAD6, onErrorContainer = 0xFF410002,
        background = 0xFFF6F9F6, onBackground = 0xFF171D1A,
        surface = 0xFFF6F9F6, onSurface = 0xFF171D1A, surfaceVariant = 0xFFDBE5DD, onSurfaceVariant = 0xFF3C4740,
        surfaceContainerLowest = 0xFFFFFFFF, surfaceContainerLow = 0xFFF0F4F0, surfaceContainer = 0xFFEAEFEB,
        surfaceContainerHigh = 0xFFE4E9E5, surfaceContainerHighest = 0xFFDEE4DF,
        outline = 0xFF6C7770, outlineVariant = 0xFFBFC9C1,
        inverseSurface = 0xFF2C322E, inverseOnSurface = 0xFFEDF2ED, inversePrimary = 0xFF8FD8B0,
    )

    val dark = Scheme(
        primary = 0xFF8FD8B0, onPrimary = 0xFF003823, primaryContainer = 0xFF005234, onPrimaryContainer = 0xFFB4EFCB,
        secondary = 0xFFFFB868, onSecondary = 0xFF492900, secondaryContainer = 0xFF693C00, onSecondaryContainer = 0xFFFFDDB8,
        tertiary = 0xFFA6C8FF, onTertiary = 0xFF00315E, tertiaryContainer = 0xFF0F4478, onTertiaryContainer = 0xFFD4E3FF,
        error = 0xFFFFB4AB, onError = 0xFF690005, errorContainer = 0xFF93000A, onErrorContainer = 0xFFFFDAD6,
        background = 0xFF0F1512, onBackground = 0xFFE2E6E1,
        surface = 0xFF0F1512, onSurface = 0xFFE2E6E1, surfaceVariant = 0xFF3C4740, onSurfaceVariant = 0xFFC3CCC5,
        surfaceContainerLowest = 0xFF0A0F0C, surfaceContainerLow = 0xFF171D1A, surfaceContainer = 0xFF1B221E,
        surfaceContainerHigh = 0xFF262C29, surfaceContainerHighest = 0xFF303733,
        outline = 0xFF8D9790, outlineVariant = 0xFF3C4740,
        inverseSurface = 0xFFE2E6E1, inverseOnSurface = 0xFF2C322E, inversePrimary = 0xFF1E6B4A,
    )

    /** WCAG 2.x contrast ratio between two opaque ARGB colours (1.0 … 21.0). */
    fun contrast(a: Long, b: Long): Double {
        val la = luminance(a)
        val lb = luminance(b)
        return (max(la, lb) + 0.05) / (min(la, lb) + 0.05)
    }

    private fun luminance(argb: Long): Double {
        fun channel(shift: Int): Double {
            val c = ((argb shr shift) and 0xFF) / 255.0
            return if (c <= 0.03928) c / 12.92 else ((c + 0.055) / 1.055).pow(2.4)
        }
        return 0.2126 * channel(16) + 0.7152 * channel(8) + 0.0722 * channel(0)
    }
}
