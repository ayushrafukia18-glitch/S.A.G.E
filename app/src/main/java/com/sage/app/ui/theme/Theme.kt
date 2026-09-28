package com.sage.app.ui.theme

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

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF81C784),
    onPrimary = Color(0xFF00390E),
    primaryContainer = Color(0xFF1B5E20),
    onPrimaryContainer = Color(0xFFA5D6A7),
    secondary = Color(0xFFA5D6A7),
    onSecondary = Color(0xFF0A3912),
    background = SageDarkSurface,
    surface = SageDarkCard,
    onBackground = Color(0xFFE2E3DF),
    onSurface = Color(0xFFE2E3DF),
    surfaceVariant = Color(0xFF262C26),
    onSurfaceVariant = Color(0xFFC2C9BD)
)

private val LightColorScheme = lightColorScheme(
    primary = SageGreenPrimary,
    onPrimary = Color.White,
    primaryContainer = SageLightGreen,
    onPrimaryContainer = Color(0xFF002204),
    secondary = SageGreenSecondary,
    onSecondary = Color.White,
    background = SageLightSurface,
    surface = SageLightCard,
    onBackground = Color(0xFF1A1C19),
    onSurface = Color(0xFF1A1C19),
    surfaceVariant = Color(0xFFE2E8DE),
    onSurfaceVariant = Color(0xFF424940)
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
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
