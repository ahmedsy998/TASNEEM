package com.example

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.test.core.app.ApplicationProvider
import com.example.data.CuratedBackgroundMediaRepository
import com.example.data.QuranDataRepository
import com.example.media.QuranTextAndLayoutEngine
import com.example.media.VideoFrameCanvasRenderer
import com.example.model.ArabicQuranFont
import com.example.model.BackgroundCategory
import com.example.model.PhoneMediaBackground
import com.example.model.QuranTextStyleConfig
import com.example.model.QuranVerse
import com.example.model.RecitationHeaderConfig
import com.example.model.SocialPlatform
import com.example.model.TransitionEffectType
import com.example.model.VerseAudioSegment
import com.example.model.VerseTransitionConfig
import com.example.model.VideoAspectRatio
import com.example.model.WatermarkConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read Tasneem app_name and verify 114 surahs`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("تسنيم", appName)

        val quranRepo = QuranDataRepository(context)
        assertEquals(114, quranRepo.allSurahs.size)
        assertEquals("الفاتحة", quranRepo.getSurahByNumber(1).nameAr)
        assertEquals(286, quranRepo.getSurahByNumber(2).verseCount)
        assertEquals("الإخلاص", quranRepo.getSurahByNumber(112).nameAr)
        assertEquals(4, quranRepo.getSurahByNumber(112).verseCount)

        val (validStart, validEnd) = quranRepo.validateVerseRange(2, 1, 20)
        assertEquals(1, validStart)
        assertEquals(20, validEnd)
    }

    @Test
    fun `verify Arabic-Indic and Western numeric input normalization`() {
        assertEquals("255", QuranTextAndLayoutEngine.normalizeNumericInput("٢٥٥"))
        assertEquals("114", QuranTextAndLayoutEngine.normalizeNumericInput("114"))
        assertEquals("7", QuranTextAndLayoutEngine.normalizeNumericInput("٧"))
    }

    @Test
    fun `verify independent Tashkeel diacritics detection and spannable styling`() {
        val bismillah = "بِسْمِ ٱللَّهِ ٱلرَّحْمَٰنِ ٱلرَّحِيمِ"
        assertTrue(bismillah.any { QuranTextAndLayoutEngine.isQuranicDiacritic(it) })
        val spannable = QuranTextAndLayoutEngine.buildQuranSpannable(
            text = bismillah,
            mainColorInt = 0xFFFFFFFF.toInt(),
            enableIndependentTashkeel = true,
            tashkeelColorInt = 0xFFE8872F.toInt()
        )
        assertEquals(bismillah, spannable.toString())
    }

    @Test
    fun `verify long verse segmentation into single-line phrases without truncation`() {
        val ayatAlKursi = QuranVerse(
            surahNumber = 2,
            verseNumber = 255,
            uthmaniText = "ٱللَّهُ لَآ إِلَٰهَ إِلَّا هُوَ ٱلْحَىُّ ٱلْقَيُّومُ ۚ لَا تَأْخُذُهُۥ سِنَةٌ وَلَا نَوْمٌ ۚ لَّهُۥ مَا فِى ٱلسَّمَٰوَٰتِ وَمَا فِى ٱلْأَرْضِ ۗ مَن ذَا ٱلَّذِى يَشْفَعُ عِندَهُۥٓ إِلَّا بِإِذْنِهِۦ ۚ يَعْلَمُ مَا بَيْنَ أَيْدِيهِمْ وَمَا خَلْفَهُمْ ۖ وَلَا يُحِيطُونَ بِشَىْءٍ مِّنْ عِلْمِهِۦٓ إِلَّا بِمَا شَآءَ ۚ وَسِعَ كُرْسِيُّهُ ٱلسَّمَٰوَٰتِ وَٱلْأَرْضَ ۖ وَلَا يَـُٔودُهُۥ حِفْظُهُمَا ۚ وَهُوَ ٱلْعَلِىُّ ٱلْعَظِيمُ"
        )
        val segment = VerseAudioSegment(
            surahNumber = 2,
            verseNumber = 255,
            localFilePath = "/tmp/002255.mp3",
            sourceUrl = "https://everyayah.com/data/Yasser_Ad-Dussary_128kbps/002255.mp3",
            durationMs = 48_000L,
            startOffsetMs = 0L,
            endOffsetMs = 48_000L
        )

        val phrases = QuranTextAndLayoutEngine.segmentVerseIntoSingleLinePhrases(ayatAlKursi, segment)
        assertTrue("Long verse must be segmented into multiple single-line phrases", phrases.size >= 8)
        assertEquals(0L, phrases.first().startMs)
        assertEquals(48_000L, phrases.last().endMs)
        assertTrue("Final phrase must include verse end symbol", phrases.last().text.contains("﴿٢٥٥﴾"))
    }

    @Test
    fun `verify all 8 Arabic fonts and 15 curated background categories and canvas frame rendering`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        assertEquals(8, ArabicQuranFont.entries.size)
        ArabicQuranFont.entries.forEach { font ->
            assertNotNull(font.loadTypeface(context))
        }

        assertEquals(15, BackgroundCategory.entries.size)
        val curatedRepo = CuratedBackgroundMediaRepository(context)
        assertEquals(30, curatedRepo.getItems(isVideo = false).size)
        assertEquals(15, curatedRepo.getItems(isVideo = true).size)

        val renderer = VideoFrameCanvasRenderer(context)
        val bitmap = Bitmap.createBitmap(360, 640, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        TransitionEffectType.entries.forEach { effect ->
            val hitBoxes = renderer.renderFrame(
                canvas = canvas,
                frameWidth = 360,
                frameHeight = 640,
                backgroundBitmap = null,
                mediaConfig = PhoneMediaBackground(),
                surahNameAr = "المطففين",
                activeVerse = QuranVerse(83, 27, "وَمِزَاجُهُۥ مِن تَسْنِيمٍ"),
                activeSegment = null,
                localVerseTimeMs = 150L,
                reciterNameAr = "ياسر الدوسري",
                textConfig = QuranTextStyleConfig(),
                headerConfig = RecitationHeaderConfig(),
                transitionConfig = VerseTransitionConfig(effectType = effect),
                watermarkConfig = WatermarkConfig(
                    signatureText = "تسنيم",
                    selectedSocialPlatforms = SocialPlatform.entries.toSet()
                )
            )
            assertFalse(hitBoxes.quranVerseBounds.isEmpty)
            assertFalse(hitBoxes.signatureBounds.isEmpty)
        }
        bitmap.recycle()
    }
}
