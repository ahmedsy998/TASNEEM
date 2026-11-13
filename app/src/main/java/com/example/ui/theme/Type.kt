package com.example.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.R

// 1. KFGQPC Uthmanic Script
val KfgqpcUthmanicFontFamily = FontFamily(
    Font(R.font.kfgqpc_uthmanic, FontWeight.Normal),
    Font(R.font.kfgqpc_uthmanic, FontWeight.Bold)
)

// 2. Amiri Quran
val AmiriQuranFontFamily = FontFamily(
    Font(R.font.amiri_quran, FontWeight.Normal),
    Font(R.font.amiri_quran, FontWeight.Bold)
)

// 3. Amiri
val AmiriFontFamily = FontFamily(
    Font(R.font.amiri, FontWeight.Normal),
    Font(R.font.amiri, FontWeight.Bold)
)

// 4. Scheherazade New
val ScheherazadeNewFontFamily = FontFamily(
    Font(R.font.scheherazade_new, FontWeight.Normal),
    Font(R.font.scheherazade_new, FontWeight.Bold)
)

// 5. Lateef
val LateefFontFamily = FontFamily(
    Font(R.font.lateef, FontWeight.Normal),
    Font(R.font.lateef, FontWeight.Bold)
)

// 6. Noto Naskh Arabic
val NotoNaskhArabicFontFamily = FontFamily(
    Font(R.font.noto_naskh_arabic, FontWeight.Normal),
    Font(R.font.noto_naskh_arabic, FontWeight.Bold)
)

// 7. Noto Naskh Arabic UI
val NotoNaskhArabicUiFontFamily = FontFamily(
    Font(R.font.noto_naskh_arabic_ui, FontWeight.Normal),
    Font(R.font.noto_naskh_arabic_ui, FontWeight.Bold)
)

// 8. Markazi Text
val MarkaziTextFontFamily = FontFamily(
    Font(R.font.markazi_text, FontWeight.Normal),
    Font(R.font.markazi_text, FontWeight.Bold)
)

// 9. Noto Sans Arabic
val NotoSansArabicFontFamily = FontFamily(
    Font(R.font.noto_sans_arabic, FontWeight.Normal),
    Font(R.font.noto_sans_arabic, FontWeight.Bold)
)

// 10. Noto Sans Arabic UI
val NotoSansArabicUiFontFamily = FontFamily(
    Font(R.font.noto_sans_arabic_ui, FontWeight.Normal),
    Font(R.font.noto_sans_arabic_ui, FontWeight.Bold)
)

// 11. Cairo
val CairoFontFamily = FontFamily(
    Font(R.font.cairo, FontWeight.Normal),
    Font(R.font.cairo, FontWeight.Medium),
    Font(R.font.cairo, FontWeight.Bold)
)

// 12. IBM Plex Sans Arabic
val IbmPlexSansArabicFontFamily = FontFamily(
    Font(R.font.ibm_plex_sans_arabic, FontWeight.Normal),
    Font(R.font.ibm_plex_sans_arabic, FontWeight.Bold)
)

// 13. Mada
val MadaFontFamily = FontFamily(
    Font(R.font.mada, FontWeight.Normal),
    Font(R.font.mada, FontWeight.Bold)
)

// 14. Reem Kufi
val ReemKufiFontFamily = FontFamily(
    Font(R.font.reem_kufi, FontWeight.Normal),
    Font(R.font.reem_kufi, FontWeight.Bold)
)

// 15. Aref Ruqaa
val ArefRuqaaFontFamily = FontFamily(
    Font(R.font.aref_ruqaa, FontWeight.Normal),
    Font(R.font.aref_ruqaa, FontWeight.Bold)
)

val Typography = Typography(
    displayLarge = TextStyle(
        fontFamily = AmiriFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 36.sp,
        lineHeight = 48.sp
    ),
    headlineLarge = TextStyle(
        fontFamily = CairoFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 26.sp,
        lineHeight = 36.sp
    ),
    headlineMedium = TextStyle(
        fontFamily = CairoFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp,
        lineHeight = 30.sp
    ),
    titleLarge = TextStyle(
        fontFamily = CairoFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 19.sp,
        lineHeight = 28.sp
    ),
    titleMedium = TextStyle(
        fontFamily = CairoFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 24.sp
    ),
    titleSmall = TextStyle(
        fontFamily = CairoFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = IbmPlexSansArabicFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 26.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = IbmPlexSansArabicFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 22.sp
    ),
    bodySmall = TextStyle(
        fontFamily = IbmPlexSansArabicFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 18.sp
    ),
    labelLarge = TextStyle(
        fontFamily = CairoFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 14.sp,
        lineHeight = 20.sp
    ),
    labelMedium = TextStyle(
        fontFamily = CairoFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp
    )
)
