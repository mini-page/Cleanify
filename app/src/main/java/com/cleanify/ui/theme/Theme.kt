/*
 * Cleanify
 * Copyright (c) 2025 LoopOtto
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package com.cleanify.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat

enum class AppTheme {
    SYSTEM,
    LIGHT,
    DARK,
    DARKER,
    AMOLED
}

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF0066CC),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD1E4FF),
    onPrimaryContainer = Color(0xFF001D36),
    secondary = Color(0xFF535F70),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFD7E3F7),
    onSecondaryContainer = Color(0xFF101C2B),
    tertiary = Color(0xFF6B5778),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFF2DAFF),
    onTertiaryContainer = Color(0xFF251431),
    error = Color(0xFFBA1A1A),
    onError = Color.White,
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    background = Color(0xFFFDFCFF),
    onBackground = Color(0xFF1A1C1E),
    surface = Color(0xFFFDFCFF),
    onSurface = Color(0xFF1A1C1E),
    surfaceVariant = Color(0xFFDFE2EB),
    onSurfaceVariant = Color(0xFF43474E),
    outline = Color(0xFF73777F)
)

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF9FCAFF),
    onPrimary = Color(0xFF003258),
    primaryContainer = Color(0xFF004881),
    onPrimaryContainer = Color(0xFFD1E4FF),
    secondary = Color(0xFFBBC7DB),
    onSecondary = Color(0xFF253140),
    secondaryContainer = Color(0xFF3B4858),
    onSecondaryContainer = Color(0xFFD7E3F7),
    tertiary = Color(0xFFD7BDE4),
    onTertiary = Color(0xFF3B2948),
    tertiaryContainer = Color(0xFF523F5F),
    onTertiaryContainer = Color(0xFFF2DAFF),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    background = Color(0xFF1A1C1E),
    onBackground = Color(0xFFE2E2E6),
    surface = Color(0xFF1A1C1E),
    onSurface = Color(0xFFE2E2E6),
    surfaceVariant = Color(0xFF43474E),
    onSurfaceVariant = Color(0xFFC3C7CF),
    outline = Color(0xFF8D9199)
)

private val DarkerColorScheme = darkColorScheme(
    primary = Color(0xFF9FCAFF),
    onPrimary = Color(0xFF003258),
    primaryContainer = Color(0xFF004881),
    onPrimaryContainer = Color(0xFFD1E4FF),
    secondary = Color(0xFFBBC7DB),
    onSecondary = Color(0xFF253140),
    secondaryContainer = Color(0xFF2D3948),
    onSecondaryContainer = Color(0xFFD7E3F7),
    tertiary = Color(0xFFD7BDE4),
    onTertiary = Color(0xFF3B2948),
    tertiaryContainer = Color(0xFF523F5F),
    onTertiaryContainer = Color(0xFFF2DAFF),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    background = Color(0xFF0A0B10),
    onBackground = Color(0xFFE2E2E6),
    surface = Color(0xFF0A0B10),
    onSurface = Color(0xFFE2E2E6),
    surfaceVariant = Color(0xFF191A1D),
    onSurfaceVariant = Color(0xFFC3C7CF),
    outline = Color(0xFF8D9199)
)

private val AmoledColorScheme = darkColorScheme(
    primary = Color(0xFF9FCAFF),
    onPrimary = Color(0xFF003258),
    primaryContainer = Color(0xFF004881),
    onPrimaryContainer = Color(0xFFD1E4FF),
    secondary = Color(0xFFBBC7DB),
    onSecondary = Color(0xFF253140),
    tertiary = Color(0xFFD7BDE4),
    onTertiary = Color(0xFF3B2948),
    tertiaryContainer = Color(0xFF523F5F),
    onTertiaryContainer = Color(0xFFF2DAFF),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    background = Color.Black,
    onBackground = Color(0xFFE2E2E6),
    surface = Color.Black,
    onSurface = Color(0xFFE2E2E6),
    surfaceVariant = Color(0xFF1A1A1A),
    onSurfaceVariant = Color(0xFFC3C7CF),
    outline = Color(0xFF8D9199),
    secondaryContainer = Color(0xFF1A1A1A),

    // Explicitly set surface containers to pure black for the AMOLED theme.
    // This ensures popups like DropdownMenu have a black background.
    surfaceContainerLowest = Color.Black,
    surfaceContainerLow = Color.Black,
    surfaceContainer = Color.Black,
    surfaceContainerHigh = Color.Black,
    surfaceContainerHighest = Color.Black
)

val LocalAppTheme = staticCompositionLocalOf { AppTheme.SYSTEM }

val ExpressiveShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(28.dp) // For dialogs
)

@Composable
fun CleanifyTheme(
    theme: AppTheme = AppTheme.SYSTEM,
    useDynamicColors: Boolean = true,
    accentColorKey: String = "DEFAULT_BLUE",
    content: @Composable () -> Unit
) {
    val darkTheme = theme.isDark
    val supportsDynamic = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    val context = LocalContext.current

    val colorScheme = when {
        // Handle special themes (AMOLED, DARKER) first.
        // If dynamic colors are on, a hybrid theme gets created.
        // combined with dynamic accent colors.
        (theme == AppTheme.AMOLED || theme == AppTheme.DARKER) -> {
            val baseColorScheme = if (theme == AppTheme.AMOLED) AmoledColorScheme else DarkerColorScheme
            if (useDynamicColors && supportsDynamic) {
                val dynamic = dynamicDarkColorScheme(context)
                baseColorScheme.copy(
                    primary = dynamic.primary,
                    onPrimary = dynamic.onPrimary,
                    primaryContainer = dynamic.primaryContainer,
                    onPrimaryContainer = dynamic.onPrimaryContainer,
                    secondary = dynamic.secondary,
                    onSecondary = dynamic.onSecondary,
                    secondaryContainer = dynamic.secondaryContainer,
                    onSecondaryContainer = dynamic.onSecondaryContainer,
                    tertiary = dynamic.tertiary,
                    onTertiary = dynamic.onTertiary,
                    tertiaryContainer = dynamic.tertiaryContainer,
                    onTertiaryContainer = dynamic.onTertiaryContainer
                )
            } else {
                val accentColor = predefinedAccentColors.find { it.key == accentColorKey }
                    ?: predefinedAccentColors.first()
                baseColorScheme.copy(primary = accentColor.darkColor)
            }
        }

        // For standard themes, fully replace with dynamic colors if enabled.
        useDynamicColors && supportsDynamic -> {
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        // For standard themes when dynamic colors are off, use predefined accents.
        else -> {
            val baseColorScheme = when (theme) {
                AppTheme.LIGHT -> LightColorScheme
                AppTheme.DARK -> DarkColorScheme
                // For SYSTEM theme when dynamic colors are off
                else -> if (darkTheme) DarkColorScheme else LightColorScheme
            }
            val accentColor = predefinedAccentColors.find { it.key == accentColorKey }
                ?: predefinedAccentColors.first()
            baseColorScheme.copy(
                primary = if (darkTheme) accentColor.darkColor else accentColor.lightColor
            )
        }
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            WindowCompat.setDecorFitsSystemWindows(window, false)
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars =
                !darkTheme
        }
    }

    CompositionLocalProvider(LocalAppTheme provides theme) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = AppTypography,
            shapes = ExpressiveShapes,
            content = content
        )
    }
}

val AppTheme.isDark: Boolean
    @Composable
    @ReadOnlyComposable
    get() = when (this) {
        AppTheme.SYSTEM -> isSystemInDarkTheme()
        AppTheme.LIGHT -> false
        AppTheme.DARK, AppTheme.DARKER, AppTheme.AMOLED -> true
    }