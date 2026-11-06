package com.example

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.test.core.app.ApplicationProvider
import com.example.data.AcousticSpeechEnvelope
import com.example.data.AudioDownloadManager
import com.example.data.CuratedBackgroundMediaRepository
import com.example.data.QuranDataRepository
import com.example.data.ReciterRepository
import com.example.media.QuranTextAndLayoutEngine
import com.example.media.VideoFrameCanvasRenderer
import com.example.model.ArabicQuranFont
import com.example.model.BackgroundCategory
import com.example.model.MushafBasmalaRuleMode
import com.example.model.MushafEditionConfig
import com.example.model.PhoneMediaBackground
import com.example.model.QuranTextStyleConfig
import com.example.model.QuranVerse
import com.example.model.RecitationHeaderConfig
import com.example.model.SocialPlatform
import com.example.model.TransitionEffectType
import com.example.model.VerseAudioSegment
import com.example.model.VerseTransitionConfig
import com.example.model.WatermarkConfig
import com.example.model.WordTiming
import kotlinx.coroutines.runBlocking
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
    fun `read Tasneem app_name and verify 114 surahs and Madinah Mushaf defaults`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("تسنيم", appName)

        val quranRepo = QuranDataRepository(context)
        assertEquals(114, quranRepo.allSurahs.size)
        assertEquals("الفاتحة", quranRepo.getSurahByNumber(1).nameAr)
        assertEquals(286, quranRepo.getSurahByNumber(2).verseCount)
        assertEquals("التين", quranRepo.getSurahByNumber(95).nameAr)
        assertEquals(8, quranRepo.getSurahByNumber(95).verseCount)
        assertEquals("الإخلاص", quranRepo.getSurahByNumber(112).nameAr)
        assertEquals(4, quranRepo.getSurahByNumber(112).verseCount)

        // Section 04: Verify Dedicated Madinah Mushaf default configuration
        val mushafConfig = MushafEditionConfig()
        assertEquals(
            "مصحف المدينة النبوية — مجمع الملك فهد لطباعة المصحف الشريف",
            mushafConfig.editionNameAr
        )
        assertEquals("رواية حفص عن عاصم", mushafConfig.narrationNameAr)
        assertEquals(MushafBasmalaRuleMode.MADINAH_MUSHAF_AUDIO_LOCKED, mushafConfig.basmalaRuleMode)
    }

    @Test
    fun `TEST A - Surah Al-Fatihah 1 to 7 preserves Basmala as Verse 1 and displays complete verses`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val quranRepo = QuranDataRepository(context)
        val verses = quranRepo.getSelectedVerses(1, 1, 7).getOrThrow()
        assertEquals(7, verses.size)

        // Verse 1 of Al-Fatihah MUST be the Basmala in Madinah Mushaf (Hafs 'an Asim)
        assertEquals("بِسْمِ ٱللَّهِ ٱلرَّحْمَٰنِ ٱلرَّحِيمِ", verses[0].uthmaniText)
        val v1Phrases = QuranTextAndLayoutEngine.segmentVerseIntoSingleLinePhrases(verses[0], null)
        assertEquals(1, v1Phrases.size)
        assertTrue(v1Phrases[0].text.contains("بِسْمِ ٱللَّهِ ٱلرَّحْمَٰنِ ٱلرَّحِيمِ"))
        assertTrue(v1Phrases[0].text.endsWith("﴿١﴾"))

        // Verses 2..6 each fit completely on a single line without premature splitting
        for (vIdx in 1..5) {
            val phrases = QuranTextAndLayoutEngine.segmentVerseIntoSingleLinePhrases(verses[vIdx], null)
            assertEquals("Verse ${vIdx + 1} of Al-Fatihah must remain 1 complete single-line phrase", 1, phrases.size)
        }
    }

    @Test
    fun `TEST B and C and D - Basmala rules for Al-Baqarah 2_1, Al-Tawbah 9_1, and An-Naml 27_30`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val quranRepo = QuranDataRepository(context)

        // TEST B: Surah Al-Baqarah (2:1..5) - Basmala must NOT appear inside Verse 1 ("الٓمٓ")
        val baqarahVerses = quranRepo.getSelectedVerses(2, 1, 5).getOrThrow()
        assertEquals(5, baqarahVerses.size)
        assertEquals("الٓمٓ", baqarahVerses[0].uthmaniText)
        assertFalse(baqarahVerses[0].uthmaniText.contains("بِسْمِ"))

        // Even if an external API returns Verse 1 with a prepended Uthmani Basmala (with varying diacritic order),
        // sanitizeVerseTextForMadinahMushaf MUST strip it from Surahs 2..114 Verse 1:
        val rawApiTin1WithBasmala = "بِسْمِ ٱللَّهِ ٱلرَّحْمَٰنِ ٱلرَّحِيمِ وَٱلتِّينِ وَٱلزَّيْتُونِ"
        val sanitizedTin1 = QuranDataRepository.sanitizeVerseTextForMadinahMushaf(95, 1, rawApiTin1WithBasmala)
        assertEquals("وَٱلتِّينِ وَٱلزَّيْتُونِ", sanitizedTin1)

        // TEST C: Surah Al-Tawbah (9:1) - Basmala must NEVER appear
        val tawbahVerses = quranRepo.getSelectedVerses(9, 1, 2).getOrThrow()
        assertFalse(tawbahVerses[0].uthmaniText.contains("بِسْمِ"))
        assertTrue(tawbahVerses[0].uthmaniText.startsWith("بَرَآءَةٌ"))
        val sanitizedTawbah = QuranDataRepository.sanitizeVerseTextForMadinahMushaf(
            9,
            1,
            "بِسْمِ ٱللَّهِ ٱلرَّحْمَٰنِ ٱلرَّحِيمِ بَرَآءَةٌ مِّنَ ٱللَّهِ"
        )
        assertEquals("بَرَآءَةٌ مِّنَ ٱللَّهِ", sanitizedTawbah)

        // TEST D: Surah An-Naml Verse 30 (27:30) - Internal Basmala MUST be preserved intact
        val naml30 = quranRepo.getSelectedVerses(27, 30, 30).getOrThrow().first()
        assertEquals(
            "إِنَّهُۥ مِن سُلَيْمَٰنَ وَإِنَّهُۥ بِسْمِ ٱللَّهِ ٱلرَّحْمَٰنِ ٱلرَّحِيمِ",
            naml30.uthmaniText
        )
        val namlPhrases = QuranTextAndLayoutEngine.segmentVerseIntoSingleLinePhrases(naml30, null)
        assertEquals(1, namlPhrases.size)
        assertEquals(
            "إِنَّهُۥ مِن سُلَيْمَٰنَ وَإِنَّهُۥ بِسْمِ ٱللَّهِ ٱلرَّحْمَٰنِ ٱلرَّحِيمِ ﴿٣٠﴾",
            namlPhrases[0].text
        )
    }

    @Test
    fun `TEST E and I - Surah At-Tin 95_1 to 8 complete single-line display without Basmala or premature splitting`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val quranRepo = QuranDataRepository(context)
        val tinVerses = quranRepo.getSelectedVerses(95, 1, 8).getOrThrow()
        assertEquals(8, tinVerses.size)

        // 95:1 must be "وَٱلتِّينِ وَٱلزَّيْتُونِ" without Basmala
        assertEquals("وَٱلتِّينِ وَٱلزَّيْتُونِ", tinVerses[0].uthmaniText)
        val p1 = QuranTextAndLayoutEngine.segmentVerseIntoSingleLinePhrases(tinVerses[0], null)
        assertEquals(1, p1.size)
        assertEquals("وَٱلتِّينِ وَٱلزَّيْتُونِ ﴿١﴾", p1[0].text)

        // 95:4 ("لَقَدْ خَلَقْنَا ٱلْإِنسَٰنَ فِىٓ أَحْسَنِ تَقْوِيمٍ") MUST be displayed as 1 complete phrase,
        // NEVER split after "فِىٓ" as happened in the bug report video!
        val p4 = QuranTextAndLayoutEngine.segmentVerseIntoSingleLinePhrases(tinVerses[3], null)
        assertEquals("Surah At-Tin 95:4 must be displayed as ONE complete single-line phrase", 1, p4.size)
        assertEquals("لَقَدْ خَلَقْنَا ٱلْإِنسَٰنَ فِىٓ أَحْسَنِ تَقْوِيمٍ ﴿٤﴾", p4[0].text)

        // Verses 1, 2, 3, 4, 5, 7, 8 of Surah At-Tin all fit on 1 complete line each
        listOf(0, 1, 2, 3, 4, 6, 7).forEach { idx ->
            val slices = QuranTextAndLayoutEngine.segmentVerseIntoSingleLinePhrases(tinVerses[idx], null)
            assertEquals("Surah At-Tin verse ${idx + 1} must be 1 complete phrase", 1, slices.size)
            assertTrue(slices[0].text.contains(tinVerses[idx].uthmaniText))
        }

        // 95:6 ("إِلَّا ٱلَّذِينَ ءَامَنُوا۟ وَعَمِلُوا۟ ٱلصَّٰلِحَٰتِ فَلَهُمْ أَجْرٌ غَيْرُ مَمْنُونٍ")
        // splits cleanly at the natural clause starter "فَلَهُمْ" and ends with "مَمْنُونٍ ﴿٦﴾"
        val p6 = QuranTextAndLayoutEngine.segmentVerseIntoSingleLinePhrases(tinVerses[5], null)
        assertEquals(2, p6.size)
        assertEquals("إِلَّا ٱلَّذِينَ ءَامَنُوا۟ وَعَمِلُوا۟ ٱلصَّٰلِحَٰتِ", p6[0].text)
        assertEquals("فَلَهُمْ أَجْرٌ غَيْرُ مَمْنُونٍ ﴿٦﴾", p6[1].text)
    }

    @Test
    fun `TEST F and G and H and J - Long verse natural Waqf segmentation, word-timing calibration, and seek accuracy`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val quranRepo = QuranDataRepository(context)
        val ayatAlKursi = runBlocking { quranRepo.getSelectedVerses(2, 255, 255).getOrThrow().first() }

        val words = ayatAlKursi.uthmaniText.split(Regex("\\s+")).filter { it.isNotBlank() }
        val envelope = AcousticSpeechEnvelope(
            speechStartMs = 240L,
            speechEndMs = 47_600L,
            pauseMidpointsMs = listOf(5200L, 10400L, 16800L, 23500L, 31000L, 37200L, 42800L),
            containsLeadingBasmalaAudio = false
        )
        val calibratedTimings = AudioDownloadManager.buildCalibratedWordTimings(
            surahNumber = 2,
            verseNumber = 255,
            verseWords = words,
            totalDurationMs = 48_000L,
            envelope = envelope,
            rawApiTimings = emptyList()
        )
        assertEquals(words.size, calibratedTimings.size)
        assertEquals(240L, calibratedTimings.first().startTimeMs)
        assertEquals(47_600L, calibratedTimings.last().endTimeMs)

        val segment = VerseAudioSegment(
            surahNumber = 2,
            verseNumber = 255,
            localFilePath = "/tmp/002255.mp3",
            sourceUrl = "https://everyayah.com/data/Yasser_Ad-Dussary_128kbps/002255.mp3",
            durationMs = 48_000L,
            startOffsetMs = 0L,
            endOffsetMs = 48_000L,
            wordTimings = calibratedTimings,
            speechStartMs = envelope.speechStartMs,
            speechEndMs = envelope.speechEndMs
        )

        val phrases = QuranTextAndLayoutEngine.segmentVerseIntoSingleLinePhrases(ayatAlKursi, segment)
        assertTrue("Ayat al-Kursi must segment into natural Waqf clauses", phrases.size >= 7)
        assertEquals(0L, phrases.first().startMs)
        assertEquals(48_000L, phrases.last().endMs)
        assertTrue("Final phrase must include verse end symbol", phrases.last().text.contains("﴿٢٥٥﴾"))

        // Verify contiguous non-overlapping phrase timeline (phrase[i].endMs == phrase[i+1].startMs)
        for (i in 0 until phrases.lastIndex) {
            assertEquals(phrases[i].endMs, phrases[i + 1].startMs)
        }

        // Verify seeking to any phrase's midpoint resolves that exact phrase and word index
        phrases.forEach { slice ->
            val midMs = (slice.startMs + slice.endMs) / 2L
            val stateAtMid = QuranTextAndLayoutEngine.resolveActivePhraseAtTime(
                verse = ayatAlKursi,
                segment = segment,
                localVerseTimeMs = midMs,
                singleLineMode = true,
                transitionConfig = VerseTransitionConfig()
            )
            assertEquals(slice.phraseIndex, stateAtMid.currentSlice.phraseIndex)
            assertEquals(slice.text, stateAtMid.currentSlice.text)
        }

        // Verify API timing calibration strips 4 leading Basmala timings when audio does not contain Basmala
        val tin1Words = listOf("وَٱلتِّينِ", "وَٱلزَّيْتُونِ")
        val apiWithBasmalaTimings = listOf(
            WordTiming(95, 1, 1, "بِسْمِ", 0L, 600L),
            WordTiming(95, 1, 2, "ٱللَّهِ", 600L, 1300L),
            WordTiming(95, 1, 3, "ٱلرَّحْمَٰنِ", 1300L, 2200L),
            WordTiming(95, 1, 4, "ٱلرَّحِيمِ", 2200L, 3100L),
            WordTiming(95, 1, 5, "وَٱلتِّينِ", 3200L, 4500L),
            WordTiming(95, 1, 6, "وَٱلزَّيْتُونِ", 4500L, 6200L)
        )
        val calibratedTin1 = AudioDownloadManager.buildCalibratedWordTimings(
            surahNumber = 95,
            verseNumber = 1,
            verseWords = tin1Words,
            totalDurationMs = 3000L,
            envelope = AcousticSpeechEnvelope(150L, 2800L, emptyList(), containsLeadingBasmalaAudio = false),
            rawApiTimings = apiWithBasmalaTimings
        )
        assertEquals(2, calibratedTin1.size)
        assertEquals("وَٱلتِّينِ", calibratedTin1[0].wordText)
        assertEquals("وَٱلزَّيْتُونِ", calibratedTin1[1].wordText)
        assertEquals(150L, calibratedTin1[0].startTimeMs)
        assertEquals(2800L, calibratedTin1[1].endTimeMs)

        // Verify ReciterRepository has all 20 reciters
        val reciterRepo = ReciterRepository()
        assertEquals(20, reciterRepo.allReciters.size)
    }

    @Test
    fun `verify Arabic-Indic and Western numeric input normalization and independent Tashkeel spannable`() {
        assertEquals("255", QuranTextAndLayoutEngine.normalizeNumericInput("٢٥٥"))
        assertEquals("114", QuranTextAndLayoutEngine.normalizeNumericInput("114"))
        assertEquals("7", QuranTextAndLayoutEngine.normalizeNumericInput("٧"))

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
                surahNameAr = "التين",
                activeVerse = QuranVerse(95, 4, "لَقَدْ خَلَقْنَا ٱلْإِنسَٰنَ فِىٓ أَحْسَنِ تَقْوِيمٍ"),
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
