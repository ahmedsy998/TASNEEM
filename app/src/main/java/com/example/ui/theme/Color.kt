package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// ==================================================
// APPROVED TASNEEM BRAND COLOR PALETTE (DO NOT CHANGE)
// ==================================================
val TasneemOrange = Color(0xFFE8872F)
val TasneemOrangeLight = Color(0xFFF3A257)
val TasneemOrangeDark = Color(0xFFC66B19)

val TasneemDarkSilver = Color(0xFF343A40)
val TasneemDeepCharcoal = Color(0xFF1D2227)
val TasneemSoftIvory = Color(0xFFF5F1EA)
val TasneemSilverGray = Color(0xFFC5C8CB)
val TasneemWhite = Color(0xFFFFFFFF)

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
