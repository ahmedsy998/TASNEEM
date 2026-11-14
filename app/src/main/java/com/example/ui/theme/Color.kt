package com.example.ui.theme

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color

// ==================================================
// APPROVED TASNEEM DEFAULT BRAND COLOR PALETTE (DO NOT CHANGE)
// ==================================================
val TasneemBrandOrange = Color(0xFFE8872F)
val TasneemOrangeLight = Color(0xFFF3A257)
val TasneemOrangeDark = Color(0xFFC66B19)

val TasneemDarkSilver = Color(0xFF343A40)
val TasneemDeepCharcoal = Color(0xFF1D2227)
val TasneemSoftIvory = Color(0xFFF5F1EA)
val TasneemSilverGray = Color(0xFFC5C8CB)
val TasneemWhite = Color(0xFFFFFFFF)

/**
 * Active primary theme accent color used across Tasneem UI components.
 * Defaults to [TasneemBrandOrange] (#E8872F) so the default Orange & Dark Silver theme
 * remains 100% unchanged, and updates reactively when the user selects one of the
 * optional coordinated color themes in the Appearance ("المظهر") sheet.
 */
var TasneemOrange: Color by mutableStateOf(TasneemBrandOrange)

/**
 * Harmonious Application Color Themes (Section 2 - Optional Coordinated Color Themes).
 * The default theme is [TASNEEM_ORANGE_SILVER] which preserves the exact original
 * Orange (#E8872F) and Dark Silver (#343A40 / #1D2227) palette.
 */
enum class AppColorTheme(
    val id: String,
    val titleAr: String,
    val subtitleAr: String,
    val primaryAccent: Color,
    val secondaryAccent: Color,
    val onPrimaryDark: Color,
    val darkBackground: Color,
    val darkSecondarySurface: Color,
    val darkCardSurface: Color,
    val darkTextPrimary: Color,
    val darkTextSecondary: Color,
    val lightBackground: Color,
    val lightSecondarySurface: Color,
    val lightCardSurface: Color,
    val lightTextPrimary: Color,
    val lightTextSecondary: Color
) {
    TASNEEM_ORANGE_SILVER(
        id = "tasneem_orange_silver",
        titleAr = "برتقالي تسنيم والفضي الداكن (الافتراضي)",
        subtitleAr = "الهوية الأصلية المعتمدة باللون البرتقالي الدافئ والفضي الفحمي",
        primaryAccent = TasneemBrandOrange,
        secondaryAccent = TasneemOrangeLight,
        onPrimaryDark = TasneemDeepCharcoal,
        darkBackground = TasneemDeepCharcoal,
        darkSecondarySurface = TasneemDarkSilver,
        darkCardSurface = Color(0xFF262C32),
        darkTextPrimary = TasneemWhite,
        darkTextSecondary = TasneemSilverGray,
        lightBackground = TasneemSoftIvory,
        lightSecondarySurface = TasneemWhite,
        lightCardSurface = TasneemWhite,
        lightTextPrimary = TasneemDeepCharcoal,
        lightTextSecondary = TasneemDarkSilver
    ),
    TURQUOISE(
        id = "turquoise_theme",
        titleAr = "ثيم التركواز الهادئ",
        subtitleAr = "تركواز أنيق متناسق مع الفحمي الداكن وخلفيات محايدة مريحة للعين",
        primaryAccent = Color(0xFF26B6C6),
        secondaryAccent = Color(0xFF64D6E3),
        onPrimaryDark = Color(0xFF101B1E),
        darkBackground = Color(0xFF172226),
        darkSecondarySurface = Color(0xFF223137),
        darkCardSurface = Color(0xFF293B42),
        darkTextPrimary = Color(0xFFF2F8F9),
        darkTextSecondary = Color(0xFFB8CCD0),
        lightBackground = Color(0xFFEEF6F7),
        lightSecondarySurface = Color(0xFFFFFFFF),
        lightCardSurface = Color(0xFFF7FBFC),
        lightTextPrimary = Color(0xFF172226),
        lightTextSecondary = Color(0xFF2F444B)
    ),
    WARM_YELLOW(
        id = "warm_yellow_theme",
        titleAr = "ثيم الأصفر الذهبي الدافئ",
        subtitleAr = "أصفر دافئ متوازن غير ساطع مع الفحمي والفضي والعاجي الناعم",
        primaryAccent = Color(0xFFD9A738),
        secondaryAccent = Color(0xFFE8C266),
        onPrimaryDark = Color(0xFF1B1D20),
        darkBackground = Color(0xFF1E2124),
        darkSecondarySurface = Color(0xFF2B3035),
        darkCardSurface = Color(0xFF343A40),
        darkTextPrimary = Color(0xFFF6F2E9),
        darkTextSecondary = Color(0xFFC9C4B8),
        lightBackground = Color(0xFFF7F3E8),
        lightSecondarySurface = Color(0xFFFFFFFF),
        lightCardSurface = Color(0xFFFCF9F2),
        lightTextPrimary = Color(0xFF1E2124),
        lightTextSecondary = Color(0xFF3A3F45)
    ),
    DEEP_TEAL(
        id = "deep_teal_theme",
        titleAr = "ثيم الأخضر المزرق العميق (Teal)",
        subtitleAr = "درجات التييل العميقة الهادئة مع أسطح الفحمي والفضي المتناغمة",
        primaryAccent = Color(0xFF1E9E8E),
        secondaryAccent = Color(0xFF52C4B5),
        onPrimaryDark = Color(0xFF101C1A),
        darkBackground = Color(0xFF162221),
        darkSecondarySurface = Color(0xFF213230),
        darkCardSurface = Color(0xFF293D3B),
        darkTextPrimary = Color(0xFFF0F7F6),
        darkTextSecondary = Color(0xFFB6CBC8),
        lightBackground = Color(0xFFEDF5F4),
        lightSecondarySurface = Color(0xFFFFFFFF),
        lightCardSurface = Color(0xFFF6FAFA),
        lightTextPrimary = Color(0xFF162221),
        lightTextSecondary = Color(0xFF2D4340)
    ),
    WARM_GOLD(
        id = "warm_gold_theme",
        titleAr = "ثيم الذهبي الفاخر الدافئ",
        subtitleAr = "ذهبي هادئ أنيق مع أسطح فحمية داكنة وتباين مريح للنص العربي",
        primaryAccent = Color(0xFFC89B54),
        secondaryAccent = Color(0xFFDFB97A),
        onPrimaryDark = Color(0xFF191B1E),
        darkBackground = Color(0xFF1C1E21),
        darkSecondarySurface = Color(0xFF282C30),
        darkCardSurface = Color(0xFF31363C),
        darkTextPrimary = Color(0xFFF5F1EA),
        darkTextSecondary = Color(0xFFC6C1B8),
        lightBackground = Color(0xFFF5F1E9),
        lightSecondarySurface = Color(0xFFFFFFFF),
        lightCardSurface = Color(0xFFFAF7F2),
        lightTextPrimary = Color(0xFF1C1E21),
        lightTextSecondary = Color(0xFF383C42)
    ),
    OCEAN_BLUE(
        id = "ocean_blue_theme",
        titleAr = "ثيم المحيط الأزرق والتركواز",
        subtitleAr = "تناغم أزرق المحيط والتركواز مع درجات محايدة مريحة للعين",
        primaryAccent = Color(0xFF3898D4),
        secondaryAccent = Color(0xFF4CC2C8),
        onPrimaryDark = Color(0xFF101921),
        darkBackground = Color(0xFF162029),
        darkSecondarySurface = Color(0xFF21303D),
        darkCardSurface = Color(0xFF293B4B),
        darkTextPrimary = Color(0xFFF1F6FA),
        darkTextSecondary = Color(0xFFB8C8D4),
        lightBackground = Color(0xFFEDF4F8),
        lightSecondarySurface = Color(0xFFFFFFFF),
        lightCardSurface = Color(0xFFF6FAFC),
        lightTextPrimary = Color(0xFF162029),
        lightTextSecondary = Color(0xFF2E4252)
    );

    companion object {
        fun fromId(id: String?): AppColorTheme =
            entries.find { it.id == id } ?: TASNEEM_ORANGE_SILVER
    }
}

// Interface Background Styles (Section 14 - Application UI Themes, NOT video backgrounds)
enum class InterfaceBackgroundStyle(
    val id: String,
    val titleAr: String,
    val subtitleAr: String,
    val isDarkSurface: Boolean,
    val primaryBackground: Color,
    val secondarySurface: Color,
    val cardSurface: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val hasGeometricPattern: Boolean = false
) {
    WARM_IVORY(
        id = "warm_ivory",
        titleAr = "عاجي دافئ",
        subtitleAr = "خلفية نهارية هادئة مريحة للعين",
        isDarkSurface = false,
        primaryBackground = TasneemSoftIvory,
        secondarySurface = TasneemWhite,
        cardSurface = TasneemWhite,
        textPrimary = TasneemDeepCharcoal,
        textSecondary = TasneemDarkSilver
    ),
    PREMIUM_SILVER(
        id = "premium_silver",
        titleAr = "فضي داكن فاخر",
        subtitleAr = "الهوية الرسمية الفاخرة لاستوديو تسنيم",
        isDarkSurface = true,
        primaryBackground = TasneemDarkSilver,
        secondarySurface = TasneemDeepCharcoal,
        cardSurface = Color(0xFF282E34),
        textPrimary = TasneemWhite,
        textSecondary = TasneemSilverGray
    ),
    DEEP_CHARCOAL(
        id = "deep_charcoal",
        titleAr = "فحمي عميق",
        subtitleAr = "تباين داكن عميق يبرز اللون البرتقالي الفاخر",
        isDarkSurface = true,
        primaryBackground = TasneemDeepCharcoal,
        secondarySurface = TasneemDarkSilver,
        cardSurface = Color(0xFF262C32),
        textPrimary = TasneemWhite,
        textSecondary = TasneemSilverGray
    ),
    MIDNIGHT_SKY(
        id = "midnight_sky",
        titleAr = "سماء الليل",
        subtitleAr = "طابع ليلي هادئ للتأمل والتدبر",
        isDarkSurface = true,
        primaryBackground = Color(0xFF171C23),
        secondarySurface = Color(0xFF212832),
        cardSurface = Color(0xFF232A34),
        textPrimary = TasneemSoftIvory,
        textSecondary = TasneemSilverGray
    ),
    SANDSTONE(
        id = "sandstone",
        titleAr = "رملي صحراوي",
        subtitleAr = "درجات العاج والرمال العربية الدافئة",
        isDarkSurface = false,
        primaryBackground = Color(0xFFEDE6DA),
        secondarySurface = TasneemSoftIvory,
        cardSurface = TasneemWhite,
        textPrimary = TasneemDeepCharcoal,
        textSecondary = TasneemDarkSilver
    ),
    DEEP_FOREST(
        id = "deep_forest",
        titleAr = "واحة السكينة",
        subtitleAr = "درجة داكنة مستوحاة من الظلال الهادئة",
        isDarkSurface = true,
        primaryBackground = Color(0xFF1A2221),
        secondarySurface = Color(0xFF242E2D),
        cardSurface = Color(0xFF283331),
        textPrimary = TasneemSoftIvory,
        textSecondary = TasneemSilverGray
    ),
    ISLAMIC_PATTERNS(
        id = "islamic_patterns",
        titleAr = "زخارف إسلامية أنيقة",
        subtitleAr = "نقوش هندسية إسلامية خفيفة بهوية تسنيم",
        isDarkSurface = true,
        primaryBackground = TasneemDeepCharcoal,
        secondarySurface = TasneemDarkSilver,
        cardSurface = Color(0xFF272D33),
        textPrimary = TasneemWhite,
        textSecondary = TasneemSilverGray,
        hasGeometricPattern = true
    );

    companion object {
        fun fromId(id: String): InterfaceBackgroundStyle =
            entries.find { it.id == id } ?: DEEP_CHARCOAL
    }
}

enum class AppThemeMode(val id: String, val titleAr: String) {
    SYSTEM("system", "تلقائي (حسب النظام)"),
    LIGHT("light", "الوضع النهاري"),
    DARK("dark", "الوضع الليلي");

    companion object {
        fun fromId(id: String): AppThemeMode =
            entries.find { it.id == id } ?: DARK
    }
}
