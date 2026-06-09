package com.posbah.app.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColors = darkColorScheme(
    primary = Saffron500,
    onPrimary = Charcoal900,
    primaryContainer = Saffron600,
    onPrimaryContainer = Paper100,
    secondary = Paper200,
    onSecondary = Charcoal900,
    secondaryContainer = Charcoal700,
    onSecondaryContainer = Paper200,
    tertiary = SaffronGlow,
    onTertiary = Charcoal900,
    background = Charcoal800,
    onBackground = Paper100,
    surface = Charcoal700,
    onSurface = Paper100,
    surfaceVariant = Charcoal600,
    onSurfaceVariant = Muted400,
    outline = Charcoal500,
    outlineVariant = Charcoal500,
    error = Danger,
    onError = Paper100,
    inverseSurface = Paper100,
    inverseOnSurface = Charcoal900,
    inversePrimary = Saffron600,
    surfaceTint = Saffron500,
    scrim = Color(0x99000000)
)

private val LightColors = lightColorScheme(
    primary = Saffron600,
    onPrimary = Paper100,
    primaryContainer = Saffron400,
    onPrimaryContainer = Charcoal900,
    secondary = Charcoal800,
    onSecondary = Paper100,
    secondaryContainer = Paper200,
    onSecondaryContainer = Charcoal900,
    tertiary = Saffron500,
    onTertiary = Charcoal900,
    background = Paper100,
    onBackground = Ink900,
    surface = Color.White,
    onSurface = Ink900,
    surfaceVariant = Paper200,
    onSurfaceVariant = Ink700,
    outline = Paper300,
    outlineVariant = Paper200,
    error = Danger,
    onError = Paper100,
    inverseSurface = Charcoal800,
    inverseOnSurface = Paper100,
    inversePrimary = Saffron400,
    surfaceTint = Saffron500
)

@Composable
fun POSBahTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colors = if (darkTheme) DarkColors else LightColors

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            window.statusBarColor = colors.background.toArgb()
            window.navigationBarColor = colors.background.toArgb()
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !darkTheme
                isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    MaterialTheme(
        colorScheme = colors,
        typography = POSBahTypography,
        content = content
    )
}
