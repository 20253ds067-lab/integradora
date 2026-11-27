package com.example.integradora.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LuxuryObsidianColorScheme = darkColorScheme(
    primary = CasinoPrimary,
    onPrimary = CasinoPrimaryDark,
    primaryContainer = CasinoSurfaceElevated,
    onPrimaryContainer = TextPrimary,

    secondary = CasinoGold,
    onSecondary = CasinoPrimaryDark,
    secondaryContainer = CasinoGoldSubtle,
    onSecondaryContainer = CasinoGoldLight,

    background = CasinoBg,
    onBackground = TextPrimary,

    surface = CasinoSurface,
    onSurface = TextPrimary,
    surfaceVariant = CasinoSurfaceElevated,
    onSurfaceVariant = TextSecondary,

    outline = CasinoBorder,
    outlineVariant = CasinoBorderSubtle,

    error = CasinoRed,
    onError = TextPrimary,
    errorContainer = CasinoRedBg,
    onErrorContainer = CasinoRed
)

@Composable
fun IntegradoraTheme(
    content: @Composable () -> Unit
) {
    val colorScheme = LuxuryObsidianColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            window?.let {
                it.statusBarColor = CasinoBg.toArgb()
                it.navigationBarColor = CasinoBg.toArgb()
                WindowCompat.getInsetsController(it, view).isAppearanceLightStatusBars = false
                WindowCompat.getInsetsController(it, view).isAppearanceLightNavigationBars = false
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
