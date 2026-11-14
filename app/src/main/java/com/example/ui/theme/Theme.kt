package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection

data class TasneemExtendedColors(
    val colorTheme: AppColorTheme = AppColorTheme.TASNEEM_ORANGE_SILVER,
    val backgroundStyle: InterfaceBackgroundStyle,
    val isDark: Boolean,
    val screenBackground: Color,
    val cardBackground: Color,
    val elevatedSurface: Color,
    val primaryAccent: Color,
    val secondaryAccent: Color = TasneemOrangeLight,
    val onPrimaryAccent: Color = TasneemDeepCharcoal,
    val textPrimary: Color,
    val textSecondary: Color,
    val borderSubtle: Color
)

val LocalTasneemColors = staticCompositionLocalOf {
    TasneemExtendedColors(
        colorTheme = AppColorTheme.TASNEEM_ORANGE_SILVER,
        backgroundStyle = InterfaceBackgroundStyle.DEEP_CHARCOAL,
        isDark = true,
        screenBackground = TasneemDeepCharcoal,
        cardBackground = TasneemDarkSilver,
        elevatedSurface = Color(0xFF282E34),
        primaryAccent = TasneemBrandOrange,
        secondaryAccent = TasneemOrangeLight,
        onPrimaryAccent = TasneemDeepCharcoal,
        textPrimary = TasneemWhite,
        textSecondary = TasneemSilverGray,
        borderSubtle = Color(0x33C5C8CB)
    )
}

@Composable
fun TasneemTheme(
    themeMode: AppThemeMode = AppThemeMode.DARK,
    backgroundStyle: InterfaceBackgroundStyle = InterfaceBackgroundStyle.DEEP_CHARCOAL,
    colorTheme: AppColorTheme = AppColorTheme.TASNEEM_ORANGE_SILVER,
    content: @Composable () -> Unit
) {
    val systemDark = isSystemInDarkTheme()
    val useDark = when (themeMode) {
        AppThemeMode.LIGHT -> false
        AppThemeMode.DARK -> true
        AppThemeMode.SYSTEM -> systemDark
    }

    // Keep reactive TasneemOrange in sync with the active colorTheme
    // (When TASNEEM_ORANGE_SILVER is active, TasneemOrange == TasneemBrandOrange #E8872F)
    TasneemOrange = colorTheme.primaryAccent
    SideEffect {
        TasneemOrange = colorTheme.primaryAccent
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

    val isDefaultTheme = colorTheme == AppColorTheme.TASNEEM_ORANGE_SILVER

    val resolvedScreenBg = if (isDefaultTheme) {
        effectiveStyle.primaryBackground
    } else if (useDark) {
        colorTheme.darkBackground
    } else {
        colorTheme.lightBackground
    }

    val resolvedElevatedSurface = if (isDefaultTheme) {
        effectiveStyle.secondarySurface
    } else if (useDark) {
        colorTheme.darkSecondarySurface
    } else {
        colorTheme.lightSecondarySurface
    }

    val resolvedCardSurface = if (isDefaultTheme) {
        effectiveStyle.cardSurface
    } else if (useDark) {
        colorTheme.darkCardSurface
    } else {
        colorTheme.lightCardSurface
    }

    val resolvedTextPrimary = if (isDefaultTheme) {
        effectiveStyle.textPrimary
    } else if (useDark) {
        colorTheme.darkTextPrimary
    } else {
        colorTheme.lightTextPrimary
    }

    val resolvedTextSecondary = if (isDefaultTheme) {
        effectiveStyle.textSecondary
    } else if (useDark) {
        colorTheme.darkTextSecondary
    } else {
        colorTheme.lightTextSecondary
    }

    val colorScheme = if (useDark) {
        darkColorScheme(
            primary = colorTheme.primaryAccent,
            onPrimary = colorTheme.onPrimaryDark,
            primaryContainer = resolvedElevatedSurface,
            onPrimaryContainer = resolvedTextPrimary,
            secondary = colorTheme.secondaryAccent,
            onSecondary = colorTheme.onPrimaryDark,
            secondaryContainer = resolvedCardSurface,
            onSecondaryContainer = resolvedTextPrimary,
            tertiary = colorTheme.primaryAccent,
            background = resolvedScreenBg,
            onBackground = resolvedTextPrimary,
            surface = resolvedElevatedSurface,
            onSurface = resolvedTextPrimary,
            surfaceVariant = resolvedCardSurface,
            onSurfaceVariant = resolvedTextSecondary,
            outline = resolvedTextSecondary.copy(alpha = 0.35f)
        )
    } else {
        lightColorScheme(
            primary = colorTheme.primaryAccent,
            onPrimary = TasneemWhite,
            primaryContainer = resolvedElevatedSurface,
            onPrimaryContainer = resolvedTextPrimary,
            secondary = colorTheme.secondaryAccent,
            onSecondary = resolvedTextPrimary,
            secondaryContainer = resolvedCardSurface,
            onSecondaryContainer = resolvedTextPrimary,
            tertiary = colorTheme.primaryAccent,
            background = resolvedScreenBg,
            onBackground = resolvedTextPrimary,
            surface = resolvedElevatedSurface,
            onSurface = resolvedTextPrimary,
            surfaceVariant = resolvedCardSurface,
            onSurfaceVariant = resolvedTextSecondary,
            outline = resolvedTextSecondary.copy(alpha = 0.25f)
        )
    }

    val extendedColors = TasneemExtendedColors(
        colorTheme = colorTheme,
        backgroundStyle = backgroundStyle,
        isDark = useDark,
        screenBackground = resolvedScreenBg,
        cardBackground = resolvedCardSurface,
        elevatedSurface = resolvedElevatedSurface,
        primaryAccent = colorTheme.primaryAccent,
        secondaryAccent = colorTheme.secondaryAccent,
        onPrimaryAccent = colorTheme.onPrimaryDark,
        textPrimary = resolvedTextPrimary,
        textSecondary = resolvedTextSecondary,
        borderSubtle = if (useDark) resolvedTextSecondary.copy(alpha = 0.22f) else resolvedTextSecondary.copy(alpha = 0.18f)
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
