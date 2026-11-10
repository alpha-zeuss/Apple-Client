package com.music.appleclient.core.designsystem.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = AppleMusicColors.Accent,
    onPrimary = AppleMusicColors.TextPrimary,
    background = AppleMusicColors.BackgroundDark,
    surface = AppleMusicColors.SurfaceDark,
    onBackground = AppleMusicColors.TextPrimary,
    onSurface = AppleMusicColors.TextPrimary,
    surfaceVariant = AppleMusicColors.SurfaceElevated,
    onSurfaceVariant = AppleMusicColors.TextSecondary
)

@Composable
fun AppleMusicTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = DarkColorScheme // Defaulting to Apple Music's signature OLED Dark Mode
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = AppleMusicColors.BackgroundDark.toArgb()
            window.navigationBarColor = AppleMusicColors.BackgroundDark.toArgb()
            val windowInsetsController = WindowCompat.getInsetsController(window, view)
            windowInsetsController.isAppearanceLightStatusBars = false
            windowInsetsController.isAppearanceLightNavigationBars = false
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppleMusicTypography,
        content = content
    )
}

