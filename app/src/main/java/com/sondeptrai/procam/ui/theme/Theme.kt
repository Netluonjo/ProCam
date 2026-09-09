package com.sondeptrai.procam.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = ProCamAmber,
    secondary = TextSecondary,
    tertiary = PeakingGreen,
    background = DslrBlack,
    surface = DslrDarkGrey,
    onPrimary = DslrBlack,
    onSecondary = TextPrimary,
    onTertiary = DslrBlack,
    onBackground = TextPrimary,
    onSurface = TextPrimary
)

@Composable
fun ProCamTheme(
    content: @Composable () -> Unit
) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = DslrBlack.toArgb()
            window.navigationBarColor = DslrBlack.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = false
        }
    }

    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content
    )
}
