package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection

data class TasneemExtendedColors(
    val backgroundStyle: InterfaceBackgroundStyle,
    val isDark: Boolean,
    val screenBackground: Color,
    val cardBackground: Color,
    val elevatedSurface: Color,
    val primaryAccent: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val borderSubtle: Color
)

val LocalTasneemColors = staticCompositionLocalOf {
    TasneemExtendedColors(
        backgroundStyle = InterfaceBackgroundStyle.DEEP_CHARCOAL,
        isDark = true,
        screenBackground = TasneemDeepCharcoal,
        cardBackground = TasneemDarkSilver,
        elevatedSurface = Color(0xFF282E34),
        primaryAccent = TasneemOrange,
        textPrimary = TasneemWhite,
        textSecondary = TasneemSilverGray,
        borderSubtle = Color(0x33C5C8CB)
    )
}

@Composable
fun TasneemTheme(
    themeMode: AppThemeMode = AppThemeMode.DARK,
    backgroundStyle: InterfaceBackgroundStyle = InterfaceBackgroundStyle.DEEP_CHARCOAL,
    content: @Composable () -> Unit
) {
    val systemDark = isSystemInDarkTheme()
    val useDark = when (themeMode) {
        AppThemeMode.LIGHT -> false
        AppThemeMode.DARK -> true
        AppThemeMode.SYSTEM -> systemDark
    }

    // Harmonize the selected interface style with the active light/dark mode while preserving Tasneem palette
    val effectiveStyle = when {
        !useDark && backgroundStyle.isDarkSurface -> {
            if (backgroundStyle == InterfaceBackgroundStyle.SANDSTONE) InterfaceBackgroundStyle.SANDSTONE
            else InterfaceBackgroundStyle.WARM_IVORY
        }
        useDark && !backgroundStyle.isDarkSurface -> {
            InterfaceBackgroundStyle.DEEP_CHARCOAL
        }
        else -> backgroundStyle
    }

    val colorScheme = if (useDark) {
        darkColorScheme(
            primary = TasneemOrange,
            onPrimary = TasneemDeepCharcoal,
            primaryContainer = TasneemDarkSilver,
            onPrimaryContainer = TasneemSoftIvory,
            secondary = TasneemSilverGray,
            onSecondary = TasneemDeepCharcoal,
            secondaryContainer = TasneemDarkSilver,
            onSecondaryContainer = TasneemWhite,
            tertiary = TasneemOrange,
            background = effectiveStyle.primaryBackground,
            onBackground = effectiveStyle.textPrimary,
            surface = effectiveStyle.secondarySurface,
            onSurface = effectiveStyle.textPrimary,
            surfaceVariant = effectiveStyle.cardSurface,
            onSurfaceVariant = effectiveStyle.textSecondary,
            outline = TasneemSilverGray.copy(alpha = 0.35f)
        )
    } else {
        lightColorScheme(
            primary = TasneemOrange,
            onPrimary = TasneemWhite,
            primaryContainer = TasneemSoftIvory,
            onPrimaryContainer = TasneemDeepCharcoal,
            secondary = TasneemDarkSilver,
            onSecondary = TasneemWhite,
            secondaryContainer = TasneemSilverGray.copy(alpha = 0.35f),
            onSecondaryContainer = TasneemDeepCharcoal,
            tertiary = TasneemOrange,
            background = effectiveStyle.primaryBackground,
            onBackground = effectiveStyle.textPrimary,
            surface = effectiveStyle.secondarySurface,
            onSurface = effectiveStyle.textPrimary,
            surfaceVariant = effectiveStyle.cardSurface,
            onSurfaceVariant = effectiveStyle.textSecondary,
            outline = TasneemDarkSilver.copy(alpha = 0.25f)
        )
    }

    val extendedColors = TasneemExtendedColors(
        backgroundStyle = backgroundStyle,
        isDark = useDark,
        screenBackground = effectiveStyle.primaryBackground,
        cardBackground = effectiveStyle.cardSurface,
        elevatedSurface = effectiveStyle.secondarySurface,
        primaryAccent = TasneemOrange,
        textPrimary = effectiveStyle.textPrimary,
        textSecondary = effectiveStyle.textSecondary,
        borderSubtle = if (useDark) TasneemSilverGray.copy(alpha = 0.2f) else TasneemDarkSilver.copy(alpha = 0.16f)
    )

    // Enforce Right-To-Left (RTL) layout across the entire application
    CompositionLocalProvider(
        LocalLayoutDirection provides LayoutDirection.Rtl,
        LocalTasneemColors provides extendedColors
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}
