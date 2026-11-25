package com.focusfit.app.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

private fun c(v: Long) = Color(v)

private fun Scheme.toLight(): ColorScheme = lightColorScheme(
    primary = c(primary), onPrimary = c(onPrimary), primaryContainer = c(primaryContainer), onPrimaryContainer = c(onPrimaryContainer),
    secondary = c(secondary), onSecondary = c(onSecondary), secondaryContainer = c(secondaryContainer), onSecondaryContainer = c(onSecondaryContainer),
    tertiary = c(tertiary), onTertiary = c(onTertiary), tertiaryContainer = c(tertiaryContainer), onTertiaryContainer = c(onTertiaryContainer),
    error = c(error), onError = c(onError), errorContainer = c(errorContainer), onErrorContainer = c(onErrorContainer),
    background = c(background), onBackground = c(onBackground),
    surface = c(surface), onSurface = c(onSurface), surfaceVariant = c(surfaceVariant), onSurfaceVariant = c(onSurfaceVariant),
    surfaceContainerLowest = c(surfaceContainerLowest), surfaceContainerLow = c(surfaceContainerLow), surfaceContainer = c(surfaceContainer),
    surfaceContainerHigh = c(surfaceContainerHigh), surfaceContainerHighest = c(surfaceContainerHighest),
    surfaceBright = c(surfaceContainerLowest), surfaceDim = c(surfaceContainerHighest),
    outline = c(outline), outlineVariant = c(outlineVariant),
    inverseSurface = c(inverseSurface), inverseOnSurface = c(inverseOnSurface), inversePrimary = c(inversePrimary),
)

private fun Scheme.toDark(): ColorScheme = darkColorScheme(
    primary = c(primary), onPrimary = c(onPrimary), primaryContainer = c(primaryContainer), onPrimaryContainer = c(onPrimaryContainer),
    secondary = c(secondary), onSecondary = c(onSecondary), secondaryContainer = c(secondaryContainer), onSecondaryContainer = c(onSecondaryContainer),
    tertiary = c(tertiary), onTertiary = c(onTertiary), tertiaryContainer = c(tertiaryContainer), onTertiaryContainer = c(onTertiaryContainer),
    error = c(error), onError = c(onError), errorContainer = c(errorContainer), onErrorContainer = c(onErrorContainer),
    background = c(background), onBackground = c(onBackground),
    surface = c(surface), onSurface = c(onSurface), surfaceVariant = c(surfaceVariant), onSurfaceVariant = c(onSurfaceVariant),
    surfaceContainerLowest = c(surfaceContainerLowest), surfaceContainerLow = c(surfaceContainerLow), surfaceContainer = c(surfaceContainer),
    surfaceContainerHigh = c(surfaceContainerHigh), surfaceContainerHighest = c(surfaceContainerHighest),
    surfaceBright = c(surfaceContainerHighest), surfaceDim = c(surfaceContainerLowest),
    outline = c(outline), outlineVariant = c(outlineVariant),
    inverseSurface = c(inverseSurface), inverseOnSurface = c(inverseOnSurface), inversePrimary = c(inversePrimary),
)

/** Slightly larger small text than Material defaults: hints and labels are read on the go. */
private val AppTypography = Typography().run {
    copy(
        bodySmall = bodySmall.copy(fontSize = 13.sp, lineHeight = 18.sp),
        labelSmall = labelSmall.copy(fontSize = 12.sp, lineHeight = 16.sp, fontWeight = FontWeight.Medium),
        labelMedium = labelMedium.copy(fontSize = 13.sp, lineHeight = 18.sp),
        bodyMedium = bodyMedium.copy(fontSize = 15.sp, lineHeight = 21.sp),
    )
}

/**
 * FocusFit's own high-contrast colours by default. [useWallpaperColors] switches to Material You
 * (Android 12+), which can produce low-contrast combinations on some wallpapers, so it's opt-in.
 */
@Composable
fun FocusFitTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    useWallpaperColors: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colors = when {
        useWallpaperColors && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val ctx = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(ctx) else dynamicLightColorScheme(ctx)
        }
        darkTheme -> Palette.dark.toDark()
        else -> Palette.light.toLight()
    }
    MaterialTheme(colorScheme = colors, typography = AppTypography, content = content)
}
