package com.focusflow.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color

// ==========================================
// M3 EXPRESSIVE – DARK PALETTE
// Warm, inviting tones with deeper saturation
// ==========================================
val DarkPrimary = Color(0xFFB49AFF)         // Brighter, warmer violet
val DarkOnPrimary = Color(0xFF2D0F6A)
val DarkPrimaryContainer = Color(0xFF4A21A0)
val DarkOnPrimaryContainer = Color(0xFFEADDFF)
val DarkInversePrimary = Color(0xFF7C3AED)

val DarkSecondary = Color(0xFFFDD068)       // Richer golden yellow
val DarkOnSecondary = Color(0xFF3D2600)
val DarkSecondaryContainer = Color(0xFF5A3900)
val DarkOnSecondaryContainer = Color(0xFFFFE4A8)

val DarkTertiary = Color(0xFFF9A8D4)        // Warmer pink/rose
val DarkOnTertiary = Color(0xFF4A0E2F)
val DarkTertiaryContainer = Color(0xFF6E1A47)
val DarkOnTertiaryContainer = Color(0xFFFFD9EC)

val DarkError = Color(0xFFFFB4AB)
val DarkOnError = Color(0xFF690005)
val DarkErrorContainer = Color(0xFF93000A)
val DarkOnErrorContainer = Color(0xFFFFDAD6)

val DarkSurface = Color(0xFF110F1A)         // Slightly warmer dark
val DarkOnSurface = Color(0xFFF0ECF8)
val DarkSurfaceVariant = Color(0xFF2C2640)
val DarkOnSurfaceVariant = Color(0xFFCCC4DC)
val DarkSurfaceDim = Color(0xFF110F1A)
val DarkSurfaceBright = Color(0xFF3A3350)
val DarkSurfaceContainer = Color(0xFF1C1828)
val DarkSurfaceContainerLow = Color(0xFF171322)
val DarkSurfaceContainerLowest = Color(0xFF0C0A14)
val DarkSurfaceContainerHigh = Color(0xFF262038)
val DarkSurfaceContainerHighest = Color(0xFF312B46)

val DarkOutline = Color(0xFF7E748F)
val DarkOutlineVariant = Color(0xFF423D55)
val DarkInverseSurface = Color(0xFFE8E0F4)
val DarkInverseOnSurface = Color(0xFF1E1A2B)

// ==========================================
// M3 EXPRESSIVE – LIGHT PALETTE
// Bright, high-contrast expressive pastels
// ==========================================
val LightPrimary = Color(0xFF6D28D9)        // Vivid violet primary
val LightOnPrimary = Color(0xFFFFFFFF)
val LightPrimaryContainer = Color(0xFFF3EAFF)
val LightOnPrimaryContainer = Color(0xFF3B0F7E)
val LightInversePrimary = Color(0xFFB49AFF)

val LightSecondary = Color(0xFFE8960A)      // Deep warm amber
val LightOnSecondary = Color(0xFFFFFFFF)
val LightSecondaryContainer = Color(0xFFFFF5E0)
val LightOnSecondaryContainer = Color(0xFF5A3900)

val LightTertiary = Color(0xFFD6366E)       // Vivid rose-pink
val LightOnTertiary = Color(0xFFFFFFFF)
val LightTertiaryContainer = Color(0xFFFFE0EE)
val LightOnTertiaryContainer = Color(0xFF6E1A47)

val LightError = Color(0xFFBA1A1A)
val LightOnError = Color(0xFFFFFFFF)
val LightErrorContainer = Color(0xFFFFDAD6)
val LightOnErrorContainer = Color(0xFF410002)

val LightSurface = Color(0xFFFCF8FF)        // Warmest white surface
val LightOnSurface = Color(0xFF1B1728)
val LightSurfaceVariant = Color(0xFFF0E8FC)
val LightOnSurfaceVariant = Color(0xFF534B65)
val LightSurfaceDim = Color(0xFFE0D6EF)
val LightSurfaceBright = Color(0xFFFEFBFF)
val LightSurfaceContainer = Color(0xFFF5EFFE)
val LightSurfaceContainerLow = Color(0xFFF9F4FF)
val LightSurfaceContainerLowest = Color(0xFFFFFFFF)
val LightSurfaceContainerHigh = Color(0xFFECE4F6)
val LightSurfaceContainerHighest = Color(0xFFE4DCEE)

val LightOutline = Color(0xFF847A95)
val LightOutlineVariant = Color(0xFFD5CBE6)
val LightInverseSurface = Color(0xFF2E2940)
val LightInverseOnSurface = Color(0xFFF5F0FF)

// ==========================================
// M3 Expressive Accent & Gradient Tokens
// ==========================================
val VioletCardGradientStart = Color(0xFFA67CFC)
val VioletCardGradientEnd = Color(0xFF6D28D9)
val AmberCardGradientStart = Color(0xFFFFCC66)
val AmberCardGradientEnd = Color(0xFFE8960A)
val RoseCardGradientStart = Color(0xFFF9A8D4)
val RoseCardGradientEnd = Color(0xFFD6366E)
val MintGreen = Color(0xFF10B981)
val SkyBlue = Color(0xFF0EA5E9)
val Coral = Color(0xFFF97316)

// ==========================================
// Composable Theme Properties (Dynamic for Light & Dark)
// ==========================================
val Primary: Color @Composable @ReadOnlyComposable get() = MaterialTheme.colorScheme.primary
val OnPrimary: Color @Composable @ReadOnlyComposable get() = MaterialTheme.colorScheme.onPrimary
val PrimaryContainer: Color @Composable @ReadOnlyComposable get() = MaterialTheme.colorScheme.primaryContainer
val OnPrimaryContainer: Color @Composable @ReadOnlyComposable get() = MaterialTheme.colorScheme.onPrimaryContainer
val PrimaryFixedDim: Color @Composable @ReadOnlyComposable get() = MaterialTheme.colorScheme.primary
val InversePrimary: Color @Composable @ReadOnlyComposable get() = MaterialTheme.colorScheme.inversePrimary

val Secondary: Color @Composable @ReadOnlyComposable get() = MaterialTheme.colorScheme.secondary
val OnSecondary: Color @Composable @ReadOnlyComposable get() = MaterialTheme.colorScheme.onSecondary
val SecondaryContainer: Color @Composable @ReadOnlyComposable get() = MaterialTheme.colorScheme.secondaryContainer
val OnSecondaryContainer: Color @Composable @ReadOnlyComposable get() = MaterialTheme.colorScheme.onSecondaryContainer
val SecondaryFixed: Color @Composable @ReadOnlyComposable get() = MaterialTheme.colorScheme.secondary

val Tertiary: Color @Composable @ReadOnlyComposable get() = MaterialTheme.colorScheme.tertiary
val OnTertiary: Color @Composable @ReadOnlyComposable get() = MaterialTheme.colorScheme.onTertiary
val TertiaryContainer: Color @Composable @ReadOnlyComposable get() = MaterialTheme.colorScheme.tertiaryContainer
val OnTertiaryContainer: Color @Composable @ReadOnlyComposable get() = MaterialTheme.colorScheme.onTertiaryContainer
val TertiaryFixed: Color @Composable @ReadOnlyComposable get() = MaterialTheme.colorScheme.tertiary

val Error: Color @Composable @ReadOnlyComposable get() = MaterialTheme.colorScheme.error
val OnError: Color @Composable @ReadOnlyComposable get() = MaterialTheme.colorScheme.onError
val ErrorContainer: Color @Composable @ReadOnlyComposable get() = MaterialTheme.colorScheme.errorContainer
val OnErrorContainer: Color @Composable @ReadOnlyComposable get() = MaterialTheme.colorScheme.onErrorContainer

val Surface: Color @Composable @ReadOnlyComposable get() = MaterialTheme.colorScheme.surface
val OnSurface: Color @Composable @ReadOnlyComposable get() = MaterialTheme.colorScheme.onSurface
val SurfaceVariant: Color @Composable @ReadOnlyComposable get() = MaterialTheme.colorScheme.surfaceVariant
val OnSurfaceVariant: Color @Composable @ReadOnlyComposable get() = MaterialTheme.colorScheme.onSurfaceVariant
val SurfaceDim: Color @Composable @ReadOnlyComposable get() = MaterialTheme.colorScheme.surfaceDim
val SurfaceBright: Color @Composable @ReadOnlyComposable get() = MaterialTheme.colorScheme.surfaceBright
val SurfaceContainer: Color @Composable @ReadOnlyComposable get() = MaterialTheme.colorScheme.surfaceContainer
val SurfaceContainerLow: Color @Composable @ReadOnlyComposable get() = MaterialTheme.colorScheme.surfaceContainerLow
val SurfaceContainerLowest: Color @Composable @ReadOnlyComposable get() = MaterialTheme.colorScheme.surfaceContainerLowest
val SurfaceContainerHigh: Color @Composable @ReadOnlyComposable get() = MaterialTheme.colorScheme.surfaceContainerHigh
val SurfaceContainerHighest: Color @Composable @ReadOnlyComposable get() = MaterialTheme.colorScheme.surfaceContainerHighest

val Outline: Color @Composable @ReadOnlyComposable get() = MaterialTheme.colorScheme.outline
val OutlineVariant: Color @Composable @ReadOnlyComposable get() = MaterialTheme.colorScheme.outlineVariant
val InverseSurface: Color @Composable @ReadOnlyComposable get() = MaterialTheme.colorScheme.inverseSurface
val InverseOnSurface: Color @Composable @ReadOnlyComposable get() = MaterialTheme.colorScheme.inverseOnSurface
