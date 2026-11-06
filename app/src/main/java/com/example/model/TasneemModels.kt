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
import com.example.ui.theme.KfgqpcUthmanicFontFamily
import com.example.ui.theme.LateefFontFamily
import com.example.ui.theme.NotoNaskhArabicFontFamily
import com.example.ui.theme.NotoSansArabicFontFamily
import com.example.ui.theme.ScheherazadeNewFontFamily
import java.util.concurrent.ConcurrentHashMap

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
    val surahNumber: Int = 1,
    val verseNumber: Int = 1,
    val wordIndex: Int, // 1-indexed word position in verse
    val wordText: String = "",
    val startTimeMs: Long,
    val endTimeMs: Long
) {
    constructor(wordIndex: Int, startMs: Long, endMs: Long) : this(
        surahNumber = 1,
        verseNumber = 1,
        wordIndex = wordIndex,
        wordText = "",
        startTimeMs = startMs,
        endTimeMs = endMs
    )

    val startMs: Long get() = startTimeMs
    val endMs: Long get() = endTimeMs
}

data class VerseAudioSegment(
    val surahNumber: Int,
    val verseNumber: Int,
    val localFilePath: String,
    val sourceUrl: String,
    val durationMs: Long,
    val startOffsetMs: Long,
    val endOffsetMs: Long,
    val wordTimings: List<WordTiming> = emptyList(),
    val speechStartMs: Long = 0L,
    val speechEndMs: Long = durationMs,
    val containsLeadingBasmalaAudio: Boolean = false,
    val acousticPauseWindowsMs: List<Long> = emptyList(),
    val isVerified: Boolean = true
)

enum class MushafBasmalaRuleMode(
    val id: String,
    val titleAr: String,
    val descriptionAr: String
) {
    MADINAH_MUSHAF_AUDIO_LOCKED(
        id = "madinah_audio_locked",
        titleAr = "مطابقة التلاوة الصوتية وأحكام مصحف المدينة",
        descriptionAr = "الفاتحة (١:١ آية مستقلة) · التوبة (بلا بسملة) · النمل (٢٧:٣٠ جزء من الآية) · سائر السور (لا تُضاف للآية ١ إلا إذا نطقها القارئ في الملف الصوتي)"
    ),
    STRICT_VERSE_TEXT_ONLY(
        id = "strict_verse_text_only",
        titleAr = "النص القرآني الصافي للآية فقط",
        descriptionAr = "عرض نص الآية المحقق فقط دون إضافة البسملة الافتتاحية في غير الفاتحة (١:١) والنمل (٢٧:٣٠)"
    )
}

/**
 * Madinah Mushaf Configuration (Section 04 — "إعدادات المصحف").
 * Default Edition: مصحف المدينة النبوية — مجمع الملك فهد لطباعة المصحف الشريف
 * Default Narration: رواية حفص عن عاصم
 */
data class MushafEditionConfig(
    val editionNameAr: String = "مصحف المدينة النبوية — مجمع الملك فهد لطباعة المصحف الشريف",
    val narrationNameAr: String = "رواية حفص عن عاصم",
    val scriptNameAr: String = "الرسم العثماني الموافق لمصحف المدينة النبوية",
    val numberingSystemAr: String = "العد الكوفي المعتمد في مصحف المدينة النبوية (٦٢٣٦ آية)",
    val basmalaRuleMode: MushafBasmalaRuleMode = MushafBasmalaRuleMode.MADINAH_MUSHAF_AUDIO_LOCKED,
    val preserveUthmanicOrthography: Boolean = true,
    val showQuranicWaqfSymbols: Boolean = true
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
        displayNameAr = "خط مصحف المدينة العثماني (KFGQPC Uthmanic)",
        descriptionAr = "خط مجمع الملك فهد لطباعة المصحف الشريف بالرسم العثماني الأصيل",
        fontResId = R.font.kfgqpc_uthmanic,
        composeFontFamily = KfgqpcUthmanicFontFamily,
        licensingNoteAr = "خط حفص العثماني القياسي للمصحف الشريف"
    ),
    AMIRI(
        id = "amiri",
        displayNameAr = "خط أميري الكلاسيكي (Amiri)",
        descriptionAr = "خط نسخ أدبي عريق مستوحى من مطبعة بولاق بالقاهرة",
        fontResId = R.font.amiri,
        composeFontFamily = AmiriFontFamily
    ),
    SCHEHERAZADE_NEW(
        id = "scheherazade_new",
        displayNameAr = "خط شهرزاد المصحفي (Scheherazade New)",
        descriptionAr = "خط نسخ تقليدي واسع الحروف واضح الحركات والمدود",
        fontResId = R.font.scheherazade_new,
        composeFontFamily = ScheherazadeNewFontFamily
    ),
    NOTO_NASKH_ARABIC(
        id = "noto_naskh_arabic",
        displayNameAr = "نوتو نسخ عربي (Noto Naskh Arabic)",
        descriptionAr = "خط نسخ متوازن فائق الوضوح للشاشات الذكية",
        fontResId = R.font.noto_naskh_arabic,
        composeFontFamily = NotoNaskhArabicFontFamily
    ),
    LATEEF(
        id = "lateef",
        displayNameAr = "خط لطيف الروحاني (Lateef)",
        descriptionAr = "خط عربي انسيابي طويل القوائم يناسب المقاطع الهادئة",
        fontResId = R.font.lateef,
        composeFontFamily = LateefFontFamily
    ),
    NOTO_SANS_ARABIC(
        id = "noto_sans_arabic",
        displayNameAr = "نوتو سانس عربي (Noto Sans Arabic)",
        descriptionAr = "خط عصري مجرد واضح القراءة في المقاطع السريعة",
        fontResId = R.font.noto_sans_arabic,
        composeFontFamily = NotoSansArabicFontFamily
    ),
    CAIRO(
        id = "cairo",
        displayNameAr = "خط القاهرة الهندسي (Cairo)",
        descriptionAr = "خط عربي حديث ذو طابع هندسي متماسك",
        fontResId = R.font.cairo,
        composeFontFamily = CairoFontFamily
    ),
    IBM_PLEX_SANS_ARABIC(
        id = "ibm_plex_sans_arabic",
        displayNameAr = "آي بي إم بليكس عربي (IBM Plex Sans Arabic)",
        descriptionAr = "خط تقني احترافي متناسق الحروف والمسافات",
        fontResId = R.font.ibm_plex_sans_arabic,
        composeFontFamily = IbmPlexSansArabicFontFamily
    );

    fun loadTypeface(context: Context, bold: Boolean = false): Typeface {
        val cacheKey = "${id}_$bold"
        typefaceCache[cacheKey]?.let { return it }
        val base = try {
            ResourcesCompat.getFont(context.applicationContext, fontResId) ?: Typeface.DEFAULT
        } catch (e: Exception) {
            Typeface.DEFAULT
        }
        val resolved = if (bold) Typeface.create(base, Typeface.BOLD) else base
        typefaceCache[cacheKey] = resolved
        return resolved
    }

    companion object {
        private val typefaceCache = ConcurrentHashMap<String, Typeface>()
    }
}

enum class QuranTextAlignment(
    val id: String,
    val labelAr: String,
    val composeAlign: TextAlign
) {
    CENTER("center", "توسيط", TextAlign.Center),
    RIGHT("right", "يمين", TextAlign.Right),
    LEFT("left", "يسار", TextAlign.Left)
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
    WARM_GOLD("warm_gold", "ذهبي دافئ", 0xFFF3B35A, Color(0xFFF3B35A)),
    SILVER_GRAY("silver_gray", "فضي هادئ", 0xFFC5C8CB, Color(0xFFC5C8CB)),
    SKY_ICE("sky_ice", "سماوي فاتح", 0xFFD9ECF2, Color(0xFFD9ECF2)),
    DEEP_CHARCOAL("deep_charcoal", "فحمي داكن", 0xFF1D2227, Color(0xFF1D2227))
}

data class QuranTextStyleConfig(
    val font: ArabicQuranFont = ArabicQuranFont.KFGQPC_UTHMANIC,
    val fontSizeSp: Float = 28f,
    val lineSpacingMultiplier: Float = 1.45f,
    val colorOption: TextColorOption = TextColorOption.WHITE,
    val mainTextColorHex: Long = 0xFFFFFFFF,
    val enableIndependentTashkeelColor: Boolean = true,
    val tashkeelColorHex: Long = 0xFFE8872F,
    val alignment: QuranTextAlignment = QuranTextAlignment.CENTER,
    val isBold: Boolean = false,
    val normalizedX: Float = 0.50f,
    val normalizedY: Float = 0.48f,
    val isAutoPositioned: Boolean = true,
    val singleLineDisplay: Boolean = true,
    val showSurahHeader: Boolean = true,
    val showReciterName: Boolean = true,
    val showTafsir: Boolean = false,
    val showTranslation: Boolean = false,
    val translationFontSizeScale: Float = 0.65f,
    val smoothTransitions: Boolean = true
)

/**
 * Independent Header Element Configuration (Requirement 08 - إعدادات معلومات التلاوة)
 */
data class HeaderElementConfig(
    val visible: Boolean = true,
    val normalizedX: Float = 0.50f,
    val normalizedY: Float = 0.20f,
    val fontSizeSp: Float = 18f,
    val font: ArabicQuranFont = ArabicQuranFont.AMIRI,
    val colorHex: Long = 0xFFF5F1EA,
    val opacity: Float = 0.92f,
    val alignment: QuranTextAlignment = QuranTextAlignment.CENTER,
    val marginDp: Float = 16f
)

data class RecitationHeaderConfig(
    val surahName: HeaderElementConfig = HeaderElementConfig(
        visible = true,
        normalizedX = 0.50f,
        normalizedY = 0.21f,
        fontSizeSp = 22f,
        font = ArabicQuranFont.KFGQPC_UTHMANIC,
        colorHex = 0xFFF5F1EA,
        opacity = 0.95f,
        alignment = QuranTextAlignment.CENTER,
        marginDp = 16f
    ),
    val verseNumber: HeaderElementConfig = HeaderElementConfig(
        visible = true,
        normalizedX = 0.50f,
        normalizedY = 0.27f,
        fontSizeSp = 15f,
        font = ArabicQuranFont.CAIRO,
        colorHex = 0xFFE8872F,
        opacity = 0.92f,
        alignment = QuranTextAlignment.CENTER,
        marginDp = 14f
    ),
    val reciterName: HeaderElementConfig = HeaderElementConfig(
        visible = true,
        normalizedX = 0.50f,
        normalizedY = 0.68f,
        fontSizeSp = 16f,
        font = ArabicQuranFont.CAIRO,
        colorHex = 0xFFF5F1EA,
        opacity = 0.88f,
        alignment = QuranTextAlignment.CENTER,
        marginDp = 16f
    )
)

/**
 * Transition Effects Between Verses & Phrases (Requirement 06)
 */
enum class TransitionEffectType(
    val id: String,
    val titleAr: String,
    val subtitleAr: String
) {
    FADE_IN_OUT("fade_in_out", "تلاشي ناعم (Fade In / Out)", "ظهور واختفاء تدريجي مريح للعين"),
    SMOOTH_SLIDE("smooth_slide", "انزلاق انسيابي (Smooth Slide)", "حركة أفقية/عمودية ناعمة مع التلاوة"),
    GENTLE_ZOOM("gentle_zoom", "تقريب هادئ (Gentle Zoom)", "بروز لطيف للمقطع المقروء"),
    DISSOLVE("dissolve", "ذوبان بصري (Dissolve)", "انتقال ضوئي متدرج بين المقاطع"),
    SOFT_BLUR("soft_blur", "تمويه ناعم (Soft Blur)", "تلاشي ضبابي أنيق عند الانتقال"),
    TYPE_ON_REVEAL("type_on", "ظهور تدريجي للكلمات (Type-on Reveal)", "إظهار الكلمات بالتزامن مع النطق"),
    ELEGANT_CROSSFADE("crossfade", "تداخل متقاطع أنيق (Crossfade)", "تبديل انسيابي متناغم بين الآيات")
}

enum class TransitionSpeedPreset(
    val id: String,
    val labelAr: String,
    val durationMs: Long
) {
    VERY_SLOW("very_slow", "بطيء جدًا", 750L),
    SLOW("slow", "بطيء", 550L),
    NORMAL("normal", "عادي", 360L),
    FAST("fast", "سريع", 220L),
    VERY_FAST("very_fast", "سريع جدًا", 130L)
}

data class VerseTransitionConfig(
    val effectType: TransitionEffectType = TransitionEffectType.ELEGANT_CROSSFADE,
    val speedPreset: TransitionSpeedPreset = TransitionSpeedPreset.NORMAL,
    val durationMs: Long = 360L
)

/**
 * Social Media Platforms for Personal Signature (Requirement 10)
 */
enum class SocialPlatform(
    val id: String,
    val labelAr: String
) {
    FACEBOOK("facebook", "فيسبوك"),
    INSTAGRAM("instagram", "إنستغرام"),
    TIKTOK("tiktok", "تيك توك"),
    YOUTUBE("youtube", "يوتيوب")
}

enum class WatermarkPresetPosition(
    val id: String,
    val labelAr: String,
    val normX: Float,
    val normY: Float
) {
    BOTTOM_CENTER("bottom_center", "أسفل الوسط", 0.50f, 0.80f),
    BOTTOM_RIGHT("bottom_right", "أسفل اليمين", 0.78f, 0.80f),
    BOTTOM_LEFT("bottom_left", "أسفل اليسار", 0.22f, 0.80f),
    TOP_RIGHT("top_right", "أعلى اليمين", 0.78f, 0.14f),
    TOP_LEFT("top_left", "أعلى اليسار", 0.22f, 0.14f)
}

data class WatermarkConfig(
    val enabled: Boolean = true,
    val signatureText: String = "تسنيم",
    val font: ArabicQuranFont = ArabicQuranFont.CAIRO,
    val colorHex: Long = 0xFFF5F1EA,
    val alignment: QuranTextAlignment = QuranTextAlignment.CENTER,
    val normalizedX: Float = 0.50f,
    val normalizedY: Float = 0.80f,
    val fontSizeSp: Float = 15f,
    val scale: Float = 1.0f,
    val opacity: Float = 0.82f,
    val marginDp: Float = 16f,
    val subtleShadow: Boolean = true,
    val elegantGlow: Boolean = true,
    val selectedSocialPlatforms: Set<SocialPlatform> = emptySet(),
    val socialIconsSizeDp: Float = 18f,
    val socialIconsOffsetY: Float = 0f // Relative vertical adjustment under signature
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
    val offsetX: Float = 0f,
    val offsetY: Float = 0f,
    val brightness: Float = 0f,
    val dimLevel: Float = 0.52f,
    val loopVideoInExport: Boolean = true,
    val curatedItemId: String? = null
)

/**
 * Curated Nature & Islamic Background Media Library Categories (Requirement 14)
 */
enum class BackgroundCategory(
    val id: String,
    val titleAr: String
) {
    NATURE_LANDSCAPES("nature_landscapes", "الطبيعة والمناظر الطبيعية"),
    MOUNTAINS("mountains", "الجبال والقمم"),
    RIVERS_WATERFALLS("rivers_waterfalls", "الأنهار والشلالات"),
    FORESTS_TREES("forests_trees", "الغابات والأشجار"),
    DESERTS_DUNES("deserts_dunes", "الصحاري والكثبان الرملية"),
    SKY_CLOUDS("sky_clouds", "السماء والسحب"),
    SUNSETS_SUNRISES("sunsets_sunrises", "الغروب والشروق"),
    NIGHT_STARS("night_stars", "سماء الليل والنجوم"),
    ISLAMIC_ARCHITECTURE("islamic_architecture", "العمارة الإسلامية"),
    MOSQUES_MINARETS("mosques_minarets", "المساجد والمآذن"),
    HOLY_KAABA("holy_kaaba", "الكعبة المشرفة"),
    MADINAH_PROPHET_MOSQUE("madinah_mosque", "المدينة المنورة والمسجد النبوي"),
    HAJJ_UMRAH("hajj_umrah", "الحج والعمرة"),
    ISLAMIC_GEOMETRIC_ART("islamic_geometric", "الزخارف الهندسية الإسلامية"),
    PEACEFUL_SPIRITUAL("peaceful_spiritual", "المشاهد الروحانية الهادئة")
}

data class CuratedBackgroundMediaItem(
    val id: String,
    val titleAr: String,
    val category: BackgroundCategory,
    val isVideo: Boolean,
    val thumbnailUrl: String,
    val mediaUrl: String,
    val durationSeconds: Int = 0,
    val licenseInfoAr: String = "ملكية عامة / ترخيص مشاع مفتوح (CC0 / Public Domain)",
    val primaryPaletteColors: List<Long> = listOf(0xFF1D2227, 0xFF343A40, 0xFFE8872F)
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
    PORTRAIT_9_16("9:16", "طولي 9:16 (ريلز / شورتس / تيك توك)", 9f / 16f),
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
