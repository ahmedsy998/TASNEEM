package com.example.model

import android.content.Context
import android.graphics.Typeface
import androidx.annotation.FontRes
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.core.content.res.ResourcesCompat
import com.example.R
import com.example.ui.theme.AmiriFontFamily
import com.example.ui.theme.AmiriQuranFontFamily
import com.example.ui.theme.CairoFontFamily
import com.example.ui.theme.IbmPlexSansArabicFontFamily
import com.example.ui.theme.LateefFontFamily
import com.example.ui.theme.NotoNaskhArabicFontFamily
import com.example.ui.theme.NotoSansArabicFontFamily
import com.example.ui.theme.ScheherazadeNewFontFamily

data class SurahInfo(
    val number: Int,
    val nameAr: String,
    val verseCount: Int,
    val revelationTypeAr: String
)

data class QuranVerse(
    val surahNumber: Int,
    val verseNumber: Int,
    val uthmaniText: String,
    val tafsirMuyassar: String = "",
    val translationSaheeh: String = ""
)

data class WordTiming(
    val wordIndex: Int, // 1-indexed word position in verse
    val startMs: Long,
    val endMs: Long
)

data class VerseAudioSegment(
    val surahNumber: Int,
    val verseNumber: Int,
    val localFilePath: String,
    val sourceUrl: String,
    val durationMs: Long,
    val startOffsetMs: Long,
    val endOffsetMs: Long,
    val wordTimings: List<WordTiming> = emptyList(),
    val isVerified: Boolean = true
)

data class ReciterInfo(
    val index: Int, // 1..20
    val id: String,
    val nameAr: String,
    val styleAr: String,
    val everyAyahSubfolder: String,
    val quranComRecitationId: Int?,
    val alQuranCloudEdition: String?,
    val bitrateKbps: Int
)

enum class ArabicQuranFont(
    val id: String,
    val displayNameAr: String,
    val descriptionAr: String,
    @FontRes val fontResId: Int,
    val composeFontFamily: FontFamily,
    val licensingNoteAr: String? = null
) {
    KFGQPC_UTHMANIC(
        id = "kfgqpc_uthmanic",
        displayNameAr = "الرسم العثماني المصحفي (مصحف المدينة / أميري قرآن)",
        descriptionAr = "خط مصحفي مضبوط التشكيل وعلامات الوقف للآيات القرآنية",
        fontResId = R.font.amiri_quran,
        composeFontFamily = AmiriQuranFontFamily,
        licensingNoteAr = "يدعم الرسم العثماني وتشكيل المصحف الشريف وفق الضوابط القياسية"
    ),
    AMIRI(
        id = "amiri",
        displayNameAr = "خط أميري (Amiri)",
        descriptionAr = "خط نسخ كلاسيكي عريق مستوحى من مطبعة بولاق",
        fontResId = R.font.amiri,
        composeFontFamily = AmiriFontFamily
    ),
    SCHEHERAZADE_NEW(
        id = "scheherazade_new",
        displayNameAr = "خط شهرزاد الجديد (Scheherazade New)",
        descriptionAr = "خط نسخ تقليدي واضح التشكيل والحركات القرآنية",
        fontResId = R.font.scheherazade_new,
        composeFontFamily = ScheherazadeNewFontFamily
    ),
    NOTO_NASKH_ARABIC(
        id = "noto_naskh_arabic",
        displayNameAr = "نوتو نسخ عربي (Noto Naskh Arabic)",
        descriptionAr = "خط نسخ متوازن فائق الوضوح للشاشات العالية الدقة",
        fontResId = R.font.noto_naskh_arabic,
        composeFontFamily = NotoNaskhArabicFontFamily
    ),
    LATEEF(
        id = "lateef",
        displayNameAr = "خط لطيف (Lateef)",
        descriptionAr = "خط عربي انسيابي ناعم يناسب المقاطع الروحانية",
        fontResId = R.font.lateef,
        composeFontFamily = LateefFontFamily
    ),
    NOTO_SANS_ARABIC(
        id = "noto_sans_arabic",
        displayNameAr = "نوتو سانس عربي (Noto Sans Arabic)",
        descriptionAr = "خط عصري واضح القراءة في مختلف الأحجام",
        fontResId = R.font.noto_sans_arabic,
        composeFontFamily = NotoSansArabicFontFamily
    ),
    CAIRO(
        id = "cairo",
        displayNameAr = "خط القاهرة (Cairo)",
        descriptionAr = "خط عربي حديث يجمع بين الأصالة والوضوح البصري",
        fontResId = R.font.cairo,
        composeFontFamily = CairoFontFamily
    ),
    IBM_PLEX_SANS_ARABIC(
        id = "ibm_plex_sans_arabic",
        displayNameAr = "آي بي إم بليكس عربي (IBM Plex Sans Arabic)",
        descriptionAr = "خط هندسي احترافي متناسق الحروف والمسافات",
        fontResId = R.font.ibm_plex_sans_arabic,
        composeFontFamily = IbmPlexSansArabicFontFamily
    );

    fun loadTypeface(context: Context, bold: Boolean = false): Typeface {
        val base = try {
            ResourcesCompat.getFont(context, fontResId) ?: Typeface.DEFAULT
        } catch (e: Exception) {
            Typeface.DEFAULT
        }
        return if (bold) Typeface.create(base, Typeface.BOLD) else base
    }
}

enum class QuranTextAlignment(
    val id: String,
    val labelAr: String,
    val composeAlign: TextAlign
) {
    CENTER("center", "توسيط", TextAlign.Center),
    RIGHT("right", "محاذاة لليمين", TextAlign.Right),
    JUSTIFY("justify", "ضبط الكلمات", TextAlign.Justify)
}

enum class TextColorOption(
    val id: String,
    val labelAr: String,
    val colorHex: Long,
    val composeColor: Color
) {
    WHITE("white", "أبيض ناصع", 0xFFFFFFFF, Color(0xFFFFFFFF)),
    SOFT_IVORY("soft_ivory", "عاجي ناعم", 0xFFF5F1EA, Color(0xFFF5F1EA)),
    TASNEEM_ORANGE("tasneem_orange", "برتقالي تسنيم", 0xFFE8872F, Color(0xFFE8872F)),
    SILVER_GRAY("silver_gray", "فضي هادئ", 0xFFC5C8CB, Color(0xFFC5C8CB)),
    DEEP_CHARCOAL("deep_charcoal", "فحمي داكن", 0xFF1D2227, Color(0xFF1D2227))
}

data class QuranTextStyleConfig(
    val font: ArabicQuranFont = ArabicQuranFont.KFGQPC_UTHMANIC,
    val fontSizeSp: Float = 26f,
    val lineSpacingMultiplier: Float = 1.55f,
    val colorOption: TextColorOption = TextColorOption.WHITE,
    val alignment: QuranTextAlignment = QuranTextAlignment.CENTER,
    val isBold: Boolean = false,
    val normalizedX: Float = 0.5f, // 0..1 center X in frame
    val normalizedY: Float = 0.5f, // 0..1 center Y in frame
    val showSurahHeader: Boolean = true,
    val showReciterName: Boolean = true,
    val showTafsir: Boolean = false,
    val showTranslation: Boolean = false,
    val translationFontSizeScale: Float = 0.72f,
    val smoothTransitions: Boolean = true
)

enum class WatermarkPresetPosition(
    val id: String,
    val labelAr: String,
    val normX: Float,
    val normY: Float
) {
    BOTTOM_CENTER("bottom_center", "أسفل الوسط", 0.5f, 0.91f),
    BOTTOM_RIGHT("bottom_right", "أسفل اليمين", 0.82f, 0.91f),
    BOTTOM_LEFT("bottom_left", "أسفل اليسار", 0.18f, 0.91f),
    TOP_RIGHT("top_right", "أعلى اليمين", 0.82f, 0.09f),
    TOP_LEFT("top_left", "أعلى اليسار", 0.18f, 0.09f)
}

data class WatermarkConfig(
    val signatureText: String = "تسنيم",
    val normalizedX: Float = 0.5f,
    val normalizedY: Float = 0.91f,
    val fontSizeSp: Float = 15f,
    val scale: Float = 1.0f,
    val opacity: Float = 0.78f,
    val marginDp: Float = 16f,
    val subtleShadow: Boolean = true,
    val elegantGlow: Boolean = true
)

enum class MediaCropMode(val id: String, val labelAr: String) {
    CENTER_CROP("center_crop", "ملء الإطار (اقتصاص متوازن)"),
    FIT_CENTER("fit_center", "احتواء كامل داخل الإطار"),
    CUSTOM_PAN_ZOOM("custom", "تحكم حر بالموضع والتكبير")
}

data class PhoneMediaBackground(
    val uriString: String? = null,
    val isVideo: Boolean = false,
    val fileName: String = "",
    val mimeType: String = "",
    val durationMs: Long = 0L,
    val cropMode: MediaCropMode = MediaCropMode.CENTER_CROP,
    val scale: Float = 1.0f,
    val offsetX: Float = 0f, // -1f .. 1f
    val offsetY: Float = 0f, // -1f .. 1f
    val brightness: Float = 0f, // -0.6f .. +0.6f
    val dimLevel: Float = 0.52f, // 0f .. 0.9f background darkening for readability
    val loopVideoInExport: Boolean = true
)

enum class VideoResolution(
    val id: String,
    val labelAr: String,
    val subtitleAr: String,
    val shortSidePx: Int,
    val longSidePx: Int,
    val bitrateBps: Int
) {
    RES_1080P(
        id = "1080p",
        labelAr = "1080p · وضوح أعلى (Full HD)",
        subtitleAr = "دقة فائقة مناسبة للنشر الاحترافي",
        shortSidePx = 1080,
        longSidePx = 1920,
        bitrateBps = 8_000_000
    ),
    RES_720P(
        id = "720p",
        labelAr = "720p · حجم أخف (HD)",
        subtitleAr = "توازن ممتاز بين الجودة وسرعة التصدير",
        shortSidePx = 720,
        longSidePx = 1280,
        bitrateBps = 4_500_000
    )
}

enum class VideoAspectRatio(
    val id: String,
    val labelAr: String,
    val ratioValue: Float // width / height
) {
    PORTRAIT_9_16("9:16", "طولي 9:16 (ريلز / شورتس)", 9f / 16f),
    LANDSCAPE_16_9("16:9", "عرضي 16:9 (يوتيوب / شاشات)", 16f / 9f),
    SQUARE_1_1("1:1", "مربع 1:1 (منشورات)", 1f / 1f);

    fun getDimensions(resolution: VideoResolution): Pair<Int, Int> {
        return when (this) {
            PORTRAIT_9_16 -> Pair(resolution.shortSidePx, resolution.longSidePx)
            LANDSCAPE_16_9 -> Pair(resolution.longSidePx, resolution.shortSidePx)
            SQUARE_1_1 -> Pair(resolution.shortSidePx, resolution.shortSidePx)
        }
    }
}

sealed class AudioDownloadState {
    data object Idle : AudioDownloadState()
    data class Downloading(
        val selectedVerseCount: Int,
        val downloadedCount: Int,
        val currentVerseNumber: Int,
        val progressFraction: Float,
        val downloadedFiles: List<String>
    ) : AudioDownloadState()

    data class Ready(
        val surahNumber: Int,
        val startVerse: Int,
        val endVerse: Int,
        val reciterId: String,
        val segments: List<VerseAudioSegment>,
        val totalDurationMs: Long,
        val hasWordLevelSync: Boolean
    ) : AudioDownloadState()

    data class Error(
        val messageAr: String,
        val failedVerseNumber: Int? = null
    ) : AudioDownloadState()
}

data class ExportValidationReport(
    val isSuccess: Boolean,
    val mp4FileExists: Boolean,
    val mp4ContainerValid: Boolean,
    val hasVideoStream: Boolean,
    val hasAudioStream: Boolean,
    val completeAudioPresent: Boolean,
    val expectedDurationMs: Long,
    val actualDurationMs: Long,
    val fileSizeBytes: Long,
    val localFilePath: String,
    val mediaStoreUriString: String?,
    val messageAr: String
)

sealed class VideoExportState {
    data object Idle : VideoExportState()
    data class Rendering(
        val progressFraction: Float,
        val currentPhaseAr: String,
        val renderedDurationMs: Long,
        val totalDurationMs: Long
    ) : VideoExportState()

    data class Completed(
        val validationReport: ExportValidationReport
    ) : VideoExportState()

    data class Failed(
        val errorMessageAr: String
    ) : VideoExportState()
}
