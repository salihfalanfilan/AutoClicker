package com.kinetic.autoclicker.presentation.theme

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance

// Default Stitch Kinetic Dark Constants
val KineticBackground = Color(0xFF11131A)
val KineticSurface = Color(0xFF11131A)
val KineticSurfaceDim = Color(0xFF11131A)
val KineticSurfaceBright = Color(0xFF373941)
val KineticSurfaceContainerLowest = Color(0xFF0C0E15)
val KineticSurfaceContainerLow = Color(0xFF191B22)
val KineticSurfaceContainer = Color(0xFF1D1F27)
val KineticSurfaceContainerHigh = Color(0xFF282A31)
val KineticSurfaceContainerHighest = Color(0xFF33343C)

val KineticPrimary = Color(0xFF299EEC)
val KineticOnPrimary = Color(0xFFFFFFFF)
val KineticPrimaryContainer = Color(0xFF299EEC)
val KineticOnPrimaryContainer = Color(0xFFFFFFFF)
val KineticInversePrimary = Color(0xFF1B6A9E)

val KineticSecondary = Color(0xFF9449DB)
val KineticOnSecondary = Color(0xFFFFFFFF)
val KineticSecondaryContainer = Color(0xFF9449DB)
val KineticOnSecondaryContainer = Color(0xFFFFFFFF)
val KineticAccentBlue = Color(0xFF299EEC)

val KineticTertiary = Color(0xFFFFB5AB)
val KineticOnTertiary = Color(0xFF60130D)
val KineticTertiaryContainer = Color(0xFFFF8B7C)
val KineticOnTertiaryContainer = Color(0xFF76231B)

val KineticError = Color(0xFFFFB4AB)
val KineticOnError = Color(0xFF690005)
val KineticErrorContainer = Color(0xFF93000A)
val KineticOnErrorContainer = Color(0xFFFFDAD6)
val KineticStopRed = Color(0xFFEF4444)

val KineticOnSurface = Color(0xFFE2E2EC)
val KineticOnSurfaceVariant = Color(0xFFBCCBB9)
val KineticOutline = Color(0xFF869585)
val KineticOutlineVariant = Color(0xFF3D4A3D)

/**
 * Safely converts a 24-bit or 32-bit hex Long (e.g. 0xFF4BE277L) into an sRGB Compose [Color].
 * Never pass ULong directly to Color(), as Color(ULong) expects a 64-bit packed Color value
 * with ColorSpace bits in the lowest 6 bits.
 */
fun hexToComposeColor(hex: Long): Color {
    val argb = (hex and 0xFFFFFFL) or 0xFF000000L
    return Color(argb)
}

/**
 * Dynamic Theme & Custom Color Palette supporting:
 * - Light / White Mode
 * - Dark Mode (Stitch obsidian slate)
 * - AMOLED Pure Black Mode (#000000)
 * - User-customized Primary & Secondary Accent Colors
 */
data class KineticPalette(
    val isLight: Boolean,
    val isAmoled: Boolean,
    val background: Color,
    val surface: Color,
    val surfaceContainerLowest: Color,
    val surfaceContainerLow: Color,
    val surfaceContainer: Color,
    val surfaceContainerHigh: Color,
    val surfaceContainerHighest: Color,
    val primary: Color,
    val onPrimary: Color,
    val primaryContainer: Color,
    val onPrimaryContainer: Color,
    val secondary: Color,
    val onSecondary: Color,
    val secondaryContainer: Color,
    val onSecondaryContainer: Color,
    val tertiary: Color,
    val tertiaryContainer: Color,
    val onTertiaryContainer: Color,
    val error: Color,
    val errorContainer: Color,
    val onErrorContainer: Color,
    val onSurface: Color,
    val onSurfaceVariant: Color,
    val outline: Color,
    val outlineVariant: Color
)

fun buildKineticPalette(
    isLight: Boolean,
    isAmoled: Boolean,
    primaryHex: Long,
    secondaryHex: Long
): KineticPalette {
    val customPrimary = hexToComposeColor(primaryHex)
    val customSecondary = hexToComposeColor(secondaryHex)

    val onPrimaryColor = if (customPrimary.luminance() > 0.42f) Color(0xFF1B3B24) else Color(0xFFF8FAFC)
    val onSecondaryColor = if (customSecondary.luminance() > 0.42f) Color(0xFF061B3A) else Color(0xFFF8FAFC)

    return when {
        isLight -> KineticPalette(
            isLight = true,
            isAmoled = false,
            background = Color(0xFFF4F6FB),
            surface = Color(0xFFF4F6FB),
            surfaceContainerLowest = Color(0xFFFFFFFF),
            surfaceContainerLow = Color(0xFFEEF2F8),
            surfaceContainer = Color(0xFFFFFFFF),
            surfaceContainerHigh = Color(0xFFE4E9F2),
            surfaceContainerHighest = Color(0xFFD8E0EC),
            primary = customPrimary,
            onPrimary = onPrimaryColor,
            primaryContainer = customPrimary,
            onPrimaryContainer = onPrimaryColor,
            secondary = customSecondary,
            onSecondary = onSecondaryColor,
            secondaryContainer = customSecondary,
            onSecondaryContainer = onSecondaryColor,
            tertiary = Color(0xFFD97706),
            tertiaryContainer = Color(0xFFFDE68A),
            onTertiaryContainer = Color(0xFF78350F),
            error = Color(0xFFDC2626),
            errorContainer = Color(0xFFFEE2E2),
            onErrorContainer = Color(0xFF991B1B),
            onSurface = Color(0xFF0F172A),
            onSurfaceVariant = Color(0xFF475569),
            outline = Color(0xFF64748B),
            outlineVariant = Color(0xFFCBD5E1)
        )
        isAmoled -> KineticPalette(
            isLight = false,
            isAmoled = true,
            background = Color(0xFF000000),
            surface = Color(0xFF000000),
            surfaceContainerLowest = Color(0xFF000000),
            surfaceContainerLow = Color(0xFF07090E),
            surfaceContainer = Color(0xFF0B0E14),
            surfaceContainerHigh = Color(0xFF141821),
            surfaceContainerHighest = Color(0xFF1E2330),
            primary = customPrimary,
            onPrimary = onPrimaryColor,
            primaryContainer = customPrimary,
            onPrimaryContainer = onPrimaryColor,
            secondary = customSecondary,
            onSecondary = onSecondaryColor,
            secondaryContainer = customSecondary,
            onSecondaryContainer = onSecondaryColor,
            tertiary = KineticTertiary,
            tertiaryContainer = KineticTertiaryContainer,
            onTertiaryContainer = KineticOnTertiaryContainer,
            error = KineticError,
            errorContainer = KineticErrorContainer,
            onErrorContainer = KineticOnErrorContainer,
            onSurface = Color(0xFFF8FAFC),
            onSurfaceVariant = Color(0xFF94A3B8),
            outline = Color(0xFF64748B),
            outlineVariant = Color(0xFF1E293B)
        )
        else -> KineticPalette(
            isLight = false,
            isAmoled = false,
            background = KineticBackground,
            surface = KineticSurface,
            surfaceContainerLowest = KineticSurfaceContainerLowest,
            surfaceContainerLow = KineticSurfaceContainerLow,
            surfaceContainer = KineticSurfaceContainer,
            surfaceContainerHigh = KineticSurfaceContainerHigh,
            surfaceContainerHighest = KineticSurfaceContainerHighest,
            primary = customPrimary,
            onPrimary = onPrimaryColor,
            primaryContainer = customPrimary,
            onPrimaryContainer = onPrimaryColor,
            secondary = customSecondary,
            onSecondary = onSecondaryColor,
            secondaryContainer = customSecondary,
            onSecondaryContainer = onSecondaryColor,
            tertiary = KineticTertiary,
            tertiaryContainer = KineticTertiaryContainer,
            onTertiaryContainer = KineticOnTertiaryContainer,
            error = KineticError,
            errorContainer = KineticErrorContainer,
            onErrorContainer = KineticOnErrorContainer,
            onSurface = KineticOnSurface,
            onSurfaceVariant = KineticOnSurfaceVariant,
            outline = KineticOutline,
            outlineVariant = KineticOutlineVariant
        )
    }
}

val LocalKineticPalette = staticCompositionLocalOf {
    buildKineticPalette(
        isLight = false,
        isAmoled = false,
        primaryHex = 0xFF299EECL,
        secondaryHex = 0xFF9449DBL
    )
}
