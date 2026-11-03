package com.focusflow.ui.theme

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.unit.dp

// M3 Expressive: Much larger, rounder corner radii across all shape categories
val ExpressiveShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(32.dp),
)

@Composable
fun FocusFlowTheme(
    appTheme: AppTheme? = null,
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val activeThemeFromManager by ThemeManager.currentTheme.collectAsState()
    val activeTheme = appTheme ?: activeThemeFromManager

    val targetColors = ThemeManager.getColorSchemeForTheme(activeTheme, isSystemInDarkTheme = darkTheme)

    // Smooth color transitions between themes
    val animatedBackground by animateColorAsState(targetColors.background, tween(300), label = "bg")
    val animatedSurface by animateColorAsState(targetColors.surface, tween(300), label = "surface")
    val animatedSecondarySurface by animateColorAsState(targetColors.secondarySurface, tween(300), label = "secSurface")
    val animatedPrimary by animateColorAsState(targetColors.primary, tween(300), label = "primary")
    val animatedPrimaryDark by animateColorAsState(targetColors.primaryDark, tween(300), label = "primaryDark")
    val animatedSoftAccent by animateColorAsState(targetColors.softAccent, tween(300), label = "softAccent")
    val animatedPrimaryText by animateColorAsState(targetColors.primaryText, tween(300), label = "primaryText")
    val animatedSecondaryText by animateColorAsState(targetColors.secondaryText, tween(300), label = "secText")
    val animatedBorder by animateColorAsState(targetColors.border, tween(300), label = "border")

    val animatedColorScheme = AppColorScheme(
        background = animatedBackground,
        surface = animatedSurface,
        secondarySurface = animatedSecondarySurface,
        primary = animatedPrimary,
        primaryDark = animatedPrimaryDark,
        softAccent = animatedSoftAccent,
        primaryText = animatedPrimaryText,
        secondaryText = animatedSecondaryText,
        border = animatedBorder,
        isDark = targetColors.isDark,
        primaryGradientStart = targetColors.primaryGradientStart,
        primaryGradientEnd = targetColors.primaryGradientEnd,
        secondaryGradientStart = targetColors.secondaryGradientStart,
        secondaryGradientEnd = targetColors.secondaryGradientEnd
    )

    val materialColorScheme = animatedColorScheme.toMaterialColorScheme()

    CompositionLocalProvider(
        LocalAppColors provides animatedColorScheme
    ) {
        MaterialTheme(
            colorScheme = materialColorScheme,
            typography = FocusFlowTypography,
            shapes = ExpressiveShapes,
            content = content
        )
    }
}
