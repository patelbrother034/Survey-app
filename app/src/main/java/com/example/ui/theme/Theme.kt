package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF7DD3FC),
    onPrimary = Color(0xFF00304F),
    primaryContainer = Color(0xFF004A77),
    onPrimaryContainer = Color(0xFFE0F2FE),
    secondary = Color(0xFF38BDF8),
    onSecondary = Color(0xFF0F172A),
    background = CleanDarkBg,
    surface = CleanDarkSurface,
    onBackground = CleanDarkOnSurface,
    onSurface = CleanDarkOnSurface,
    surfaceVariant = Color(0xFF1E293B),
    onSurfaceVariant = Color(0xFF94A3B8),
    outline = CleanDarkOutline
)

private val LightColorScheme = lightColorScheme(
    primary = CleanPrimary,
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = CleanPrimaryContainer,
    onPrimaryContainer = CleanOnPrimaryContainer,
    secondary = CleanSecondary,
    onSecondary = Color(0xFFFFFFFF),
    background = CleanLightBg,
    surface = CleanLightSurface,
    onBackground = CleanLightOnSurface,
    onSurface = CleanLightOnSurface,
    surfaceVariant = CleanLightSurface,
    onSurfaceVariant = Color(0xFF475569),
    outline = CleanBorder
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Disable to force our custom premium brand aesthetic
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
