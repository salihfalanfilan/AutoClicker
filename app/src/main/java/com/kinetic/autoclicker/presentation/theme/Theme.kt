package com.kinetic.autoclicker.presentation.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import com.kinetic.autoclicker.domain.model.EngineSettings
import com.kinetic.autoclicker.domain.model.ThemeMode

@Composable
fun KineticAutoClickerTheme(
    settings: EngineSettings = EngineSettings(),
    content: @Composable () -> Unit
) {
    val systemDark = isSystemInDarkTheme()

    val isLight = when (settings.themeMode) {
        ThemeMode.SYSTEM -> !systemDark
        ThemeMode.LIGHT -> true
        ThemeMode.DARK, ThemeMode.AMOLED -> false
    }
    val isAmoled = settings.themeMode == ThemeMode.AMOLED

    val palette = remember(
        isLight,
        isAmoled,
        settings.primaryAccentHex,
        settings.secondaryAccentHex
    ) {
        buildKineticPalette(
            isLight = isLight,
            isAmoled = isAmoled,
            primaryHex = settings.primaryAccentHex,
            secondaryHex = settings.secondaryAccentHex
        )
    }

    val isTurkish = remember(settings.appLanguage) {
        settings.appLanguage.resolveIsTurkish()
    }
    val appStrings = if (isTurkish) TurkishAppStrings else EnglishAppStrings

    val colorScheme = if (isLight) {
        lightColorScheme(
            primary = palette.primary,
            onPrimary = palette.onPrimary,
            primaryContainer = palette.primaryContainer,
            onPrimaryContainer = palette.onPrimaryContainer,
            secondary = palette.secondary,
            onSecondary = palette.onSecondary,
            secondaryContainer = palette.secondaryContainer,
            onSecondaryContainer = palette.onSecondaryContainer,
            tertiary = palette.tertiary,
            tertiaryContainer = palette.tertiaryContainer,
            onTertiaryContainer = palette.onTertiaryContainer,
            error = palette.error,
            errorContainer = palette.errorContainer,
            onErrorContainer = palette.onErrorContainer,
            background = palette.background,
            onBackground = palette.onSurface,
            surface = palette.surface,
            onSurface = palette.onSurface,
            surfaceVariant = palette.surfaceContainerHighest,
            onSurfaceVariant = palette.onSurfaceVariant,
            outline = palette.outline,
            outlineVariant = palette.outlineVariant,
            surfaceContainer = palette.surfaceContainer,
            surfaceContainerHigh = palette.surfaceContainerHigh,
            surfaceContainerHighest = palette.surfaceContainerHighest,
            surfaceContainerLow = palette.surfaceContainerLow,
            surfaceContainerLowest = palette.surfaceContainerLowest
        )
    } else {
        darkColorScheme(
            primary = palette.primary,
            onPrimary = palette.onPrimary,
            primaryContainer = palette.primaryContainer,
            onPrimaryContainer = palette.onPrimaryContainer,
            secondary = palette.secondary,
            onSecondary = palette.onSecondary,
            secondaryContainer = palette.secondaryContainer,
            onSecondaryContainer = palette.onSecondaryContainer,
            tertiary = palette.tertiary,
            tertiaryContainer = palette.tertiaryContainer,
            onTertiaryContainer = palette.onTertiaryContainer,
            error = palette.error,
            errorContainer = palette.errorContainer,
            onErrorContainer = palette.onErrorContainer,
            background = palette.background,
            onBackground = palette.onSurface,
            surface = palette.surface,
            onSurface = palette.onSurface,
            surfaceVariant = palette.surfaceContainerHighest,
            onSurfaceVariant = palette.onSurfaceVariant,
            outline = palette.outline,
            outlineVariant = palette.outlineVariant,
            surfaceContainer = palette.surfaceContainer,
            surfaceContainerHigh = palette.surfaceContainerHigh,
            surfaceContainerHighest = palette.surfaceContainerHighest,
            surfaceContainerLow = palette.surfaceContainerLow,
            surfaceContainerLowest = palette.surfaceContainerLowest
        )
    }

    CompositionLocalProvider(
        LocalKineticPalette provides palette,
        LocalAppStrings provides appStrings
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = KineticTypography,
            shapes = KineticShapes,
            content = content
        )
    }
}
