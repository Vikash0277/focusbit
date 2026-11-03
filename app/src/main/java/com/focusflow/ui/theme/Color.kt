package com.focusflow.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color

// ==========================================
// Dark Palette Tokens
// ==========================================
val DarkPrimary = Color(0xFFC0C1FF)
val DarkOnPrimary = Color(0xFF1000A9)
val DarkPrimaryContainer = Color(0xFF8083FF)
val DarkOnPrimaryContainer = Color(0xFF0D0096)
val DarkInversePrimary = Color(0xFF494BD6)

val DarkSecondary = Color(0xFF7BD0FF)
val DarkOnSecondary = Color(0xFF00354A)
val DarkSecondaryContainer = Color(0xFF00A6E0)
val DarkOnSecondaryContainer = Color(0xFF00374D)

val DarkTertiary = Color(0xFF4EDEA3)
val DarkOnTertiary = Color(0xFF003824)
val DarkTertiaryContainer = Color(0xFF00885D)
val DarkOnTertiaryContainer = Color(0xFF000703)

val DarkError = Color(0xFFFFB4AB)
val DarkOnError = Color(0xFF690005)
val DarkErrorContainer = Color(0xFF93000A)
val DarkOnErrorContainer = Color(0xFFFFDAD6)

val DarkSurface = Color(0xFF0B1326)
val DarkOnSurface = Color(0xFFDAE2FD)
val DarkSurfaceVariant = Color(0xFF2D3449)
val DarkOnSurfaceVariant = Color(0xFFC7C4D7)
val DarkSurfaceDim = Color(0xFF0B1326)
val DarkSurfaceBright = Color(0xFF31394D)
val DarkSurfaceContainer = Color(0xFF171F33)
val DarkSurfaceContainerLow = Color(0xFF131B2E)
val DarkSurfaceContainerLowest = Color(0xFF060E20)
val DarkSurfaceContainerHigh = Color(0xFF222A3D)
val DarkSurfaceContainerHighest = Color(0xFF2D3449)

val DarkOutline = Color(0xFF908FA0)
val DarkOutlineVariant = Color(0xFF464554)
val DarkInverseSurface = Color(0xFFDAE2FD)
val DarkInverseOnSurface = Color(0xFF283044)

// ==========================================
// Light Palette Tokens
// ==========================================
val LightPrimary = Color(0xFF434EE8)
val LightOnPrimary = Color(0xFFFFFFFF)
val LightPrimaryContainer = Color(0xFFE0E2FF)
val LightOnPrimaryContainer = Color(0xFF0B0080)
val LightInversePrimary = Color(0xFFC0C1FF)

val LightSecondary = Color(0xFF006688)
val LightOnSecondary = Color(0xFFFFFFFF)
val LightSecondaryContainer = Color(0xFFC4E7FF)
val LightOnSecondaryContainer = Color(0xFF001E2B)

val LightTertiary = Color(0xFF006C48)
val LightOnTertiary = Color(0xFFFFFFFF)
val LightTertiaryContainer = Color(0xFF8EF8C3)
val LightOnTertiaryContainer = Color(0xFF002113)

val LightError = Color(0xFFBA1A1A)
val LightOnError = Color(0xFFFFFFFF)
val LightErrorContainer = Color(0xFFFFDAD6)
val LightOnErrorContainer = Color(0xFF410002)

val LightSurface = Color(0xFFF7F9FD)
val LightOnSurface = Color(0xFF181C22)
val LightSurfaceVariant = Color(0xFFE1E2EC)
val LightOnSurfaceVariant = Color(0xFF44464F)
val LightSurfaceDim = Color(0xFFD8DAE0)
val LightSurfaceBright = Color(0xFFF7F9FD)
val LightSurfaceContainer = Color(0xFFECEEF4)
val LightSurfaceContainerLow = Color(0xFFF2F3F9)
val LightSurfaceContainerLowest = Color(0xFFFFFFFF)
val LightSurfaceContainerHigh = Color(0xFFE6E8EE)
val LightSurfaceContainerHighest = Color(0xFFE0E2E8)

val LightOutline = Color(0xFF74777F)
val LightOutlineVariant = Color(0xFFC4C6D0)
val LightInverseSurface = Color(0xFF2D3137)
val LightInverseOnSurface = Color(0xFFEFF0F7)

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
