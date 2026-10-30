package com.learntogether.app.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat

/** Colors that don't fit Material's scheme: the navy bars and the high-contrast flag. */
data class AppExtras(
    val topBar: Color,
    val onTopBar: Color,
    val indicator: Color,
    val onIndicator: Color,
    val highContrast: Boolean
)

val LocalAppExtras = staticCompositionLocalOf {
    AppExtras(Color(0xFF0B3A82), Color.White, Color.White, Color(0xFF0B3A82), false)
}

private val LightScheme = lightColorScheme(
    primary = Color(0xFF1565D8),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDCE8FF),
    onPrimaryContainer = Color(0xFF0B2A5E),
    secondary = Color(0xFF0B3A82),
    onSecondary = Color.White,
    background = Color(0xFFF4F8FF),
    onBackground = Color(0xFF10213F),
    surface = Color.White,
    onSurface = Color(0xFF10213F),
    surfaceVariant = Color(0xFFE6EEFB),
    onSurfaceVariant = Color(0xFF3B4B66),
    outline = Color(0xFF6F84A8)
)

private val HighContrastScheme = darkColorScheme(
    primary = Color(0xFFFFD54F),
    onPrimary = Color.Black,
    primaryContainer = Color(0xFF262626),
    onPrimaryContainer = Color.White,
    secondary = Color(0xFFFFD54F),
    onSecondary = Color.Black,
    background = Color.Black,
    onBackground = Color.White,
    surface = Color(0xFF121212),
    onSurface = Color.White,
    surfaceVariant = Color(0xFF262626),
    onSurfaceVariant = Color(0xFFE0E0E0),
    outline = Color(0xFFBDBDBD)
)

private val LightExtras = AppExtras(
    topBar = Color(0xFF0B3A82),
    onTopBar = Color.White,
    indicator = Color.White,
    onIndicator = Color(0xFF0B3A82),
    highContrast = false
)

private val HighContrastExtras = AppExtras(
    topBar = Color.Black,
    onTopBar = Color.White,
    indicator = Color(0xFFFFD54F),
    onIndicator = Color.Black,
    highContrast = true
)

val AppTypography = Typography(
    headlineLarge = TextStyle(fontWeight = FontWeight.Bold, fontSize = 34.sp, lineHeight = 40.sp),
    headlineMedium = TextStyle(fontWeight = FontWeight.Bold, fontSize = 28.sp, lineHeight = 34.sp),
    titleLarge = TextStyle(fontWeight = FontWeight.Bold, fontSize = 22.sp, lineHeight = 28.sp),
    titleMedium = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 18.sp, lineHeight = 24.sp),
    bodyLarge = TextStyle(fontSize = 18.sp, lineHeight = 26.sp),
    bodyMedium = TextStyle(fontSize = 16.sp, lineHeight = 22.sp),
    labelLarge = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 16.sp),
    labelMedium = TextStyle(fontWeight = FontWeight.Medium, fontSize = 14.sp)
)

@Composable
fun LearnTogetherTheme(
    highContrast: Boolean,
    textScale: Float,
    content: @Composable () -> Unit
) {
    val scheme = if (highContrast) HighContrastScheme else LightScheme
    val extras = if (highContrast) HighContrastExtras else LightExtras

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = extras.topBar.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        }
    }

    // Scaling fontScale on top of the system setting lets the in-app "Text Size" control stack
    // with the user's Android font size.
    val density = LocalDensity.current
    CompositionLocalProvider(
        LocalAppExtras provides extras,
        LocalDensity provides Density(density.density, density.fontScale * textScale)
    ) {
        MaterialTheme(colorScheme = scheme, typography = AppTypography, content = content)
    }
}
