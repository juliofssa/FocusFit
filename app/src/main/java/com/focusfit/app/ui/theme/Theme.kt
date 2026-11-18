package com.focusfit.app.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val Green = Color(0xFF2F7D5B)
private val GreenLight = Color(0xFF5CC493)
private val Orange = Color(0xFFD9822B)
private val Blue = Color(0xFF3A6FB0)

private val LightColors = lightColorScheme(
    primary = Green,
    secondary = Orange,
    tertiary = Blue,
    primaryContainer = Color(0xFFD8EFE3),
    onPrimaryContainer = Color(0xFF0B3624),
)

private val DarkColors = darkColorScheme(
    primary = GreenLight,
    secondary = Color(0xFFF0A35A),
    tertiary = Color(0xFF7AA8E6),
    primaryContainer = Color(0xFF1F3A2D),
    onPrimaryContainer = Color(0xFFCDEEDC),
)

@Composable
fun FocusFitTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit,
) {
    val colors = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val ctx = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(ctx) else dynamicLightColorScheme(ctx)
        }
        darkTheme -> DarkColors
        else -> LightColors
    }
    MaterialTheme(colorScheme = colors, content = content)
}
