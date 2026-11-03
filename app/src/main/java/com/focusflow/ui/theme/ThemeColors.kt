package com.focusflow.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

data class AppColorScheme(
    val background: Color,
    val surface: Color,
    val secondarySurface: Color,
    val primary: Color,
    val primaryDark: Color,
    val softAccent: Color,
    val primaryText: Color,
    val secondaryText: Color,
    val border: Color,
    val isDark: Boolean,
    // Accent Gradients
    val primaryGradientStart: Color = primary,
    val primaryGradientEnd: Color = primaryDark,
    val secondaryGradientStart: Color = Color(0xFFFFB84D),
    val secondaryGradientEnd: Color = Color(0xFFF59E0B)
)

// ==========================================
// THEME 1: LAVENDER DREAM (DEFAULT)
// ==========================================
val LavenderDreamColors = AppColorScheme(
    background = Color(0xFFF7F6FA),
    surface = Color(0xFFFFFFFF),
    secondarySurface = Color(0xFFF0EDF7),
    primary = Color(0xFF8977E8),
    primaryDark = Color(0xFF705CCB),
    softAccent = Color(0xFFEDE8FF),
    primaryText = Color(0xFF252334),
    secondaryText = Color(0xFF777184),
    border = Color(0xFFEAE7F0),
    isDark = false,
    primaryGradientStart = Color(0xFF9E8CFB),
    primaryGradientEnd = Color(0xFF705CCB),
    secondaryGradientStart = Color(0xFFFFCC66),
    secondaryGradientEnd = Color(0xFFF59E0B)
)

// ==========================================
// THEME 2: MIDNIGHT
// ==========================================
val MidnightColors = AppColorScheme(
    background = Color(0xFF151720),
    surface = Color(0xFF222634),
    secondarySurface = Color(0xFF292E3D),
    primary = Color(0xFF9B8AFB),
    primaryDark = Color(0xFF806DE0),
    softAccent = Color(0xFF343047),
    primaryText = Color(0xFFF7F7FC),
    secondaryText = Color(0xFFA6A8B8),
    border = Color(0xFF34394A),
    isDark = true,
    primaryGradientStart = Color(0xFFA697FC),
    primaryGradientEnd = Color(0xFF705CCB),
    secondaryGradientStart = Color(0xFFFBBF24),
    secondaryGradientEnd = Color(0xFFD97706)
)

// ==========================================
// THEME 3: OCEAN BREEZE
// ==========================================
val OceanBreezeColors = AppColorScheme(
    background = Color(0xFFF1F8FA),
    surface = Color(0xFFFFFFFF),
    secondarySurface = Color(0xFFE7F2F4),
    primary = Color(0xFF379BA5),
    primaryDark = Color(0xFF286D76),
    softAccent = Color(0xFFDCEEF0),
    primaryText = Color(0xFF18343D),
    secondaryText = Color(0xFF66818A),
    border = Color(0xFFDCE8EA),
    isDark = false,
    primaryGradientStart = Color(0xFF4DB6AC),
    primaryGradientEnd = Color(0xFF286D76),
    secondaryGradientStart = Color(0xFF38BDF8),
    secondaryGradientEnd = Color(0xFF0284C7)
)

// ==========================================
// THEME 4: FOREST CALM
// ==========================================
val ForestCalmColors = AppColorScheme(
    background = Color(0xFFF3F6F0),
    surface = Color(0xFFFFFFFF),
    secondarySurface = Color(0xFFEAF0E6),
    primary = Color(0xFF729A75),
    primaryDark = Color(0xFF557B59),
    softAccent = Color(0xFFE1EBDD),
    primaryText = Color(0xFF26372C),
    secondaryText = Color(0xFF738475),
    border = Color(0xFFE0E8DC),
    isDark = false,
    primaryGradientStart = Color(0xFF86B089),
    primaryGradientEnd = Color(0xFF557B59),
    secondaryGradientStart = Color(0xFFA3E635),
    secondaryGradientEnd = Color(0xFF65A30D)
)

// Material 3 ColorScheme Adapter
fun AppColorScheme.toMaterialColorScheme(): ColorScheme {
    return if (isDark) {
        darkColorScheme(
            primary = primary,
            onPrimary = Color(0xFF100826),
            primaryContainer = softAccent,
            onPrimaryContainer = Color(0xFFF7F7FC),
            inversePrimary = primaryDark,
            secondary = Color(0xFFFBBF24),
            onSecondary = Color(0xFF3D2600),
            secondaryContainer = Color(0xFF382A1B),
            onSecondaryContainer = Color(0xFFFFE4A8),
            tertiary = Color(0xFFF472B6),
            onTertiary = Color(0xFF4A0E2F),
            tertiaryContainer = Color(0xFF581C3F),
            onTertiaryContainer = Color(0xFFFFD9EC),
            background = background,
            onBackground = primaryText,
            surface = surface,
            onSurface = primaryText,
            surfaceVariant = secondarySurface,
            onSurfaceVariant = secondaryText,
            surfaceContainer = secondarySurface,
            surfaceContainerLow = background,
            surfaceContainerLowest = background,
            surfaceContainerHigh = surface,
            surfaceContainerHighest = secondarySurface,
            outline = border,
            outlineVariant = border.copy(alpha = 0.6f)
        )
    } else {
        lightColorScheme(
            primary = primary,
            onPrimary = Color.White,
            primaryContainer = softAccent,
            onPrimaryContainer = primaryDark,
            inversePrimary = primary,
            secondary = Color(0xFFF59E0B),
            onSecondary = Color.White,
            secondaryContainer = Color(0xFFFEF3C7),
            onSecondaryContainer = Color(0xFF78350F),
            tertiary = Color(0xFFEC4899),
            onTertiary = Color.White,
            tertiaryContainer = Color(0xFFFCE7F3),
            onTertiaryContainer = Color(0xFF831843),
            background = background,
            onBackground = primaryText,
            surface = surface,
            onSurface = primaryText,
            surfaceVariant = secondarySurface,
            onSurfaceVariant = secondaryText,
            surfaceContainer = secondarySurface,
            surfaceContainerLow = background,
            surfaceContainerLowest = Color.White,
            surfaceContainerHigh = surface,
            surfaceContainerHighest = secondarySurface,
            outline = border,
            outlineVariant = border.copy(alpha = 0.7f)
        )
    }
}

// Composition Local for Custom Colors
val LocalAppColors = staticCompositionLocalOf { LavenderDreamColors }

val AppColors: AppColorScheme
    @Composable
    @ReadOnlyComposable
    get() = LocalAppColors.current

