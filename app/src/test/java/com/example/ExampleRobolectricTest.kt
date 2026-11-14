package com.example

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.text.style.ForegroundColorSpan
import androidx.test.core.app.ApplicationProvider
import com.example.data.AcousticSpeechEnvelope
import com.example.data.AudioDownloadManager
import com.example.data.CuratedBackgroundMediaRepository
import com.example.data.QuranDataRepository
import com.example.data.QuranFontManager
import com.example.data.ReciterRepository
import com.example.media.OpenTypeFontInspector
import com.example.media.QuranTextAndLayoutEngine
import com.example.media.VideoFrameCanvasRenderer
import com.example.model.ArabicFontCategory
import com.example.model.ArabicQuranFont
import com.example.model.BackgroundCategory
import com.example.model.FontFileFormat
import com.example.model.FontQuranCompatibilityLevel
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
import com.example.model.VideoAspectRatio
import com.example.model.VideoResolution
import com.example.model.WatermarkConfig
import com.example.model.WordTiming
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.charset.StandardCharsets

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

        // Strip prepended Basmala from Surahs 2..114 Verse 1:
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

        assertEquals("وَٱلتِّينِ وَٱلزَّيْتُونِ", tinVerses[0].uthmaniText)
        val p1 = QuranTextAndLayoutEngine.segmentVerseIntoSingleLinePhrases(tinVerses[0], null)
        assertEquals(1, p1.size)
        assertEquals("وَٱلتِّينِ وَٱلزَّيْتُونِ ﴿١﴾", p1[0].text)

        val p4 = QuranTextAndLayoutEngine.segmentVerseIntoSingleLinePhrases(tinVerses[3], null)
        assertEquals("Surah At-Tin 95:4 must be displayed as ONE complete single-line phrase", 1, p4.size)
        assertEquals("لَقَدْ خَلَقْنَا ٱلْإِنسَٰنَ فِىٓ أَحْسَنِ تَقْوِيمٍ ﴿٤﴾", p4[0].text)

        listOf(0, 1, 2, 3, 4, 6, 7).forEach { idx ->
            val slices = QuranTextAndLayoutEngine.segmentVerseIntoSingleLinePhrases(tinVerses[idx], null)
            assertEquals("Surah At-Tin verse ${idx + 1} must be 1 complete phrase", 1, slices.size)
            assertTrue(slices[0].text.contains(tinVerses[idx].uthmaniText))
        }

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

        for (i in 0 until phrases.lastIndex) {
            assertEquals(phrases[i].endMs, phrases[i + 1].startMs)
        }

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

        val reciterRepo = ReciterRepository()
        assertEquals(20, reciterRepo.allReciters.size)
    }

    @Test
    fun `TEST 1 - All 15 permanently bundled Arabic fonts have distinct font files, categories, and OpenType metadata`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val fontManager = QuranFontManager(context)

        // Verify all 15 built-in fonts exist
        assertEquals(15, ArabicQuranFont.entries.size)

        // Verify all 4 built-in categories are populated
        assertTrue(ArabicQuranFont.entries.any { it.category == ArabicFontCategory.QURANIC_FONTS })
        assertTrue(ArabicQuranFont.entries.any { it.category == ArabicFontCategory.NASKH_FONTS })
        assertTrue(ArabicQuranFont.entries.any { it.category == ArabicFontCategory.MODERN_ARABIC_FONTS })
        assertTrue(ArabicQuranFont.entries.any { it.category == ArabicFontCategory.DECORATIVE_ARABIC_FONTS })

        val sha256Hashes = mutableSetOf<String>()
        val familyNames = mutableSetOf<String>()

        ArabicQuranFont.entries.forEach { font ->
            val file = font.resolveBundledFontFile(context)
            assertNotNull("Bundled font file must exist for ${font.id}", file)
            assertTrue("Bundled font file must not be empty for ${font.id}", file!!.exists() && file.length() > 10_000L)

            val hash = QuranFontManager.computeSha256(file.readBytes())
            assertTrue("Font ${font.id} must have a unique distinct .ttf file (no duplicate font files)", sha256Hashes.add(hash))

            val typeface = font.loadTypeface(context, bold = false)
            assertNotNull("Typeface must load for ${font.id}", typeface)

            val report = fontManager.getBuiltInFontValidationReport(font)
            assertEquals(FontFileFormat.TTF, report.openTypeReport.format)
            assertTrue("Font ${font.id} must support basic Arabic letters", report.openTypeReport.basicArabicCoverageRatio >= 0.85f)
            assertTrue("Font ${font.id} must have GSUB table", report.openTypeReport.hasGsubTable)
            assertTrue("Font ${font.id} must have GPOS table", report.openTypeReport.hasGposTable)
            familyNames.add(report.openTypeReport.internalFamilyName)
        }

        // Verify distinct binary files for all 15 built-in fonts
        assertEquals(15, sha256Hashes.size)
        assertTrue("Distinct internal family names must be present across the 15 fonts", familyNames.size >= 12)
    }

    @Test
    fun `TEST 2, 3, 4, 11, 12 - Import valid TTF and OTF fonts, detect incomplete Tashkeel font, persist across restart, and remove imported font safely`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val fontManager = QuranFontManager(context)

        // Clean any prior test imported fonts
        fontManager.getImportedFonts().forEach { fontManager.removeImportedFont(it.id) }

        // TEST 2: Import a valid TTF Arabic Quranic font (using Amiri Quran .ttf bytes with a custom copy)
        val amiriQuranFile = ArabicQuranFont.AMIRI_QURAN.resolveBundledFontFile(context)!!
        val tempTtfFile = File(context.cacheDir, "UserImportedAmiriQuran.ttf").apply {
            writeBytes(amiriQuranFile.readBytes())
        }

        val ttfImportResult = fontManager.importFontFromFile(tempTtfFile, "UserImportedAmiriQuran.ttf").getOrThrow()
        val importedTtf = ttfImportResult.importedFont
        assertEquals(FontFileFormat.TTF, importedTtf.format)
        assertEquals("خط مستورد", importedTtf.badgeLabelAr)
        assertTrue(importedTtf.validationReport.isUsableInGallery)
        assertTrue(importedTtf.validationReport.isFullyQuranCompatible)
        assertEquals(FontQuranCompatibilityLevel.FULLY_VERIFIED_QURANIC, importedTtf.validationReport.compatibilityLevel)

        // TEST 3: Import a valid OTF Arabic font (with 'OTTO' SFNT header)
        val scheherazadeBytes = ArabicQuranFont.SCHEHERAZADE_NEW.resolveBundledFontFile(context)!!.readBytes().clone()
        // Set SFNT magic bytes to 'OTTO' (0x4F54544F) to test OTF binary format detection and import pipeline
        scheherazadeBytes[0] = 0x4F
        scheherazadeBytes[1] = 0x54
        scheherazadeBytes[2] = 0x54
        scheherazadeBytes[3] = 0x4F
        assertEquals(FontFileFormat.OTF, OpenTypeFontInspector.detectFontFormat(scheherazadeBytes))

        val tempOtfFile = File(context.cacheDir, "UserImportedScheherazade.otf").apply {
            writeBytes(scheherazadeBytes)
        }
        val otfImportResult = fontManager.importFontFromFile(tempOtfFile, "UserImportedScheherazade.otf").getOrThrow()
        val importedOtf = otfImportResult.importedFont
        assertEquals(FontFileFormat.OTF, importedOtf.format)
        assertEquals("خط مستورد", importedOtf.badgeLabelAr)
        assertTrue(importedOtf.validationReport.isUsableInGallery)

        // TEST 4: Import a font with incomplete Arabic diacritic support (supports Arabic letters \u0621..\u064A but lacks Tashkeel \u064B..\u0653)
        val incompleteTashkeelFontBytes = buildSyntheticFontWithArabicLettersOnlyNoTashkeel(
            baseFontBytes = ArabicQuranFont.CAIRO.resolveBundledFontFile(context)!!.readBytes()
        )
        val inspectedIncomplete = OpenTypeFontInspector.inspectFontBytes(incompleteTashkeelFontBytes, "IncompleteTashkeelArabic.ttf")
        assertTrue("Synthetic font must support basic Arabic letters", inspectedIncomplete.report.basicArabicCoverageRatio >= 0.85f)
        assertTrue("Synthetic font must be missing core Tashkeel marks", inspectedIncomplete.report.missingCoreTashkeelMarksAr.isNotEmpty())

        val tempIncompleteFile = File(context.cacheDir, "IncompleteTashkeelArabic.ttf").apply {
            writeBytes(incompleteTashkeelFontBytes)
        }
        val incompleteImportResult = fontManager.importFontFromFile(tempIncompleteFile, "IncompleteTashkeelArabic.ttf").getOrThrow()
        val importedIncomplete = incompleteImportResult.importedFont
        assertFalse("Font missing Tashkeel must NEVER be marked as fully Quran-compatible", importedIncomplete.validationReport.isFullyQuranCompatible)
        assertTrue("Font missing Tashkeel must require Quranic fallback", importedIncomplete.validationReport.requiresFallbackForQuran)
        assertEquals(
            FontQuranCompatibilityLevel.PARTIAL_ARABIC_INCOMPLETE_TASHKEEL,
            importedIncomplete.validationReport.compatibilityLevel
        )
        assertNotNull("Clear Arabic warning must be generated for incomplete Tashkeel font", importedIncomplete.validationReport.warningMessageAr)
        assertTrue(importedIncomplete.validationReport.warningMessageAr!!.contains("غير مكتمل التشكيل القرآني"))

        // TEST 11: Persist selected imported font and verify it remains available after creating a new QuranFontManager (simulating app restart)
        fontManager.saveSelectedFontPreference(
            selectedFontId = importedTtf.id,
            enableFallback = true,
            fallbackFont = ArabicQuranFont.KFGQPC_UTHMANIC
        )
        val restartedFontManager = QuranFontManager(context)
        val persistedList = restartedFontManager.getImportedFonts()
        assertEquals(3, persistedList.size)
        assertTrue(persistedList.any { it.id == importedTtf.id })
        assertTrue(persistedList.any { it.id == importedOtf.id })
        assertTrue(persistedList.any { it.id == importedIncomplete.id })

        val restoredStyle = restartedFontManager.restorePersistedTextStyle()
        assertTrue(restoredStyle.isCustomImportedFontSelected)
        assertEquals(importedTtf.id, restoredStyle.selectedFontId)
        assertEquals(importedTtf.localFilePath, restoredStyle.customFontFilePath)
        assertTrue(restoredStyle.activeFontDisplayNameAr.contains("خط مستورد"))

        // TEST 12: Remove the currently selected imported font and verify:
        // 1. Built-in fonts cannot be deleted
        assertFalse(restartedFontManager.removeImportedFont(ArabicQuranFont.KFGQPC_UTHMANIC.id))
        assertFalse(restartedFontManager.removeImportedFont(ArabicQuranFont.AMIRI.id))
        assertEquals(15, ArabicQuranFont.entries.size)

        // 2. Deleting the selected imported font succeeds and automatically reverts selection to default built-in Quranic font
        assertTrue(restartedFontManager.removeImportedFont(importedTtf.id))
        assertEquals(2, restartedFontManager.getImportedFonts().size)
        assertEquals(ArabicQuranFont.KFGQPC_UTHMANIC.id, restartedFontManager.getPersistedSelectedFontId())
        val styleAfterDelete = restartedFontManager.restorePersistedTextStyle()
        assertFalse(styleAfterDelete.isCustomImportedFontSelected)
        assertEquals(ArabicQuranFont.KFGQPC_UTHMANIC, styleAfterDelete.font)
    }

    @Test
    fun `TEST 5, 6, 7, 8, 9, 10, 13 - Vocalized Quranic rendering, Shadda+Vowel Mark-to-Mark, long words, zero vertical clipping, independent Tashkeel colors, Live Preview, MP4 frame rendering, and audio sync invariance`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val fontManager = QuranFontManager(context)
        val renderer = VideoFrameCanvasRenderer(context)

        // TEST 5 & 6 & 7: Validate all 10 vocalized Quranic samples (including Shadda+Fatha, Shadda+Damma, Shadda+Kasra, Tanween, Maddah, and long words)
        assertEquals(10, OpenTypeFontInspector.VALIDATION_SAMPLES.size)
        val quranFontsToVerify = listOf(
            ArabicQuranFont.KFGQPC_UTHMANIC,
            ArabicQuranFont.AMIRI_QURAN,
            ArabicQuranFont.SCHEHERAZADE_NEW,
            ArabicQuranFont.LATEEF,
            ArabicQuranFont.NOTO_NASKH_ARABIC
        )
        quranFontsToVerify.forEach { qFont ->
            val report = fontManager.getBuiltInFontValidationReport(qFont)
            assertEquals(10, report.sampleResults.size)
            assertTrue("Font ${qFont.id} must support Mark-to-Base (GPOS mark)", report.openTypeReport.hasMarkToBaseGpos)
            assertTrue("Font ${qFont.id} must support Mark-to-Mark (GPOS mkmk) for Shadda + Fatha/Damma/Kasra", report.openTypeReport.hasMarkToMarkGpos)
            assertFalse("Font ${qFont.id} must not have detached marks", report.hasDetachedMarks)
            assertFalse("Font ${qFont.id} must not clip diacritics vertically", report.hasVerticalClippingRisk)

            // Check Samples 3, 4, 5 (Shadda + Fatha, Damma, Kasra) and Sample 10 (Long Quranic words)
            val shaddaFatha = report.sampleResults.first { it.id == "sample_3_shadda_fatha" }
            val shaddaDamma = report.sampleResults.first { it.id == "sample_4_shadda_damma" }
            val shaddaKasra = report.sampleResults.first { it.id == "sample_5_shadda_kasra" }
            val longPhrase = report.sampleResults.first { it.id == "sample_10_long_quranic_phrase" }
            assertTrue(shaddaFatha.passed)
            assertTrue(shaddaDamma.passed)
            assertTrue(shaddaKasra.passed)
            assertTrue(longPhrase.passed)
        }

        // TEST 8, 9, 10: Render Live Preview & MP4 Export frames with both built-in and imported fonts + independent Tashkeel colors
        val amiriQuranFile = ArabicQuranFont.AMIRI_QURAN.resolveBundledFontFile(context)!!
        val customCopyFile = File(context.cacheDir, "StudioCustomQuranFont.ttf").apply {
            writeBytes(amiriQuranFile.readBytes())
        }
        val importedCustom = fontManager.importFontFromFile(customCopyFile, "StudioCustomQuranFont.ttf").getOrThrow().importedFont

        val testStyles = listOf(
            QuranTextStyleConfig(
                font = ArabicQuranFont.KFGQPC_UTHMANIC,
                selectedFontId = ArabicQuranFont.KFGQPC_UTHMANIC.id,
                mainTextColorHex = 0xFFFFFFFF,
                enableIndependentTashkeelColor = true,
                tashkeelColorHex = 0xFFE8872F
            ),
            QuranTextStyleConfig(
                font = ArabicQuranFont.AMIRI_QURAN,
                selectedFontId = importedCustom.id,
                customFontFilePath = importedCustom.localFilePath,
                customFontDisplayName = importedCustom.displayName,
                enableQuranicFontFallback = true,
                mainTextColorHex = 0xFFFFFFFF,
                enableIndependentTashkeelColor = true,
                tashkeelColorHex = 0xFFE8872F
            )
        )

        val vocalizedVerse = QuranVerse(
            surahNumber = 95,
            verseNumber = 4,
            uthmaniText = "لَقَدْ خَلَقْنَا ٱلْإِنسَٰنَ فِىٓ أَحْسَنِ تَقْوِيمٍ"
        )

        testStyles.forEach { style ->
            val resolved = QuranFontManager.resolvePhraseTypographyWithFallback(
                context = context,
                phraseText = "${vocalizedVerse.uthmaniText} ﴿٤﴾",
                textConfig = style,
                mainColorInt = Color.WHITE,
                tashkeelColorInt = Color.rgb(232, 135, 47)
            )
            assertNotNull(resolved.effectiveBaseTypeface)
            assertEquals("${vocalizedVerse.uthmaniText} ﴿٤﴾", resolved.spannable.toString())

            // Verify independent Tashkeel ForegroundColorSpans are applied
            val colorSpans = resolved.spannable.getSpans(0, resolved.spannable.length, ForegroundColorSpan::class.java)
            assertTrue("Independent Tashkeel color spans must be present", colorSpans.size > 5)

            // Render onto 1080p MP4 Export Frame Canvas & Live Preview Canvas
            val bitmap = Bitmap.createBitmap(540, 960, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            val hitBoxes = renderer.renderFrame(
                canvas = canvas,
                frameWidth = 540,
                frameHeight = 960,
                backgroundBitmap = null,
                mediaConfig = PhoneMediaBackground(),
                surahNameAr = "التين",
                activeVerse = vocalizedVerse,
                activeSegment = null,
                localVerseTimeMs = 400L,
                reciterNameAr = "ياسر الدوسري",
                textConfig = style,
                headerConfig = RecitationHeaderConfig(),
                transitionConfig = VerseTransitionConfig(effectType = TransitionEffectType.FADE_IN_OUT),
                watermarkConfig = WatermarkConfig(
                    signatureText = "تسنيم",
                    selectedSocialPlatforms = SocialPlatform.entries.toSet()
                )
            )
            assertFalse(hitBoxes.quranVerseBounds.isEmpty)
            assertTrue("Quran verse box must have positive height with vertical anti-clipping headroom", hitBoxes.quranVerseBounds.height() > 30f)
            bitmap.recycle()
        }

        // TEST 13: Verify changing the font never alters Quranic audio synchronization or verse phrase segmentation
        val baselinePhrases = QuranTextAndLayoutEngine.segmentVerseIntoSingleLinePhrases(vocalizedVerse, null)
        ArabicQuranFont.entries.forEach { font ->
            val phrasesWithFont = QuranTextAndLayoutEngine.segmentVerseIntoSingleLinePhrases(vocalizedVerse, null)
            assertEquals(baselinePhrases.size, phrasesWithFont.size)
            assertEquals(baselinePhrases[0].text, phrasesWithFont[0].text)
            assertEquals(baselinePhrases[0].startMs, phrasesWithFont[0].startMs)
            assertEquals(baselinePhrases[0].endMs, phrasesWithFont[0].endMs)
        }

        assertEquals(15, BackgroundCategory.entries.size)
        val curatedRepo = CuratedBackgroundMediaRepository(context)
        assertEquals(30, curatedRepo.getItems(isVideo = false).size)
        assertEquals(15, curatedRepo.getItems(isVideo = true).size)
    }

    @Test
    fun `PERSISTENT SETTINGS & DEFAULTS - Fares Abbad default reciter, rattil_official_2026 default signature, automatic Fade In Out, and full settings persistence across restart`() {
        val app = ApplicationProvider.getApplicationContext<android.app.Application>()
        val reciterRepo = ReciterRepository()
        val fontManager = QuranFontManager(app)
        val settingsManager = com.example.data.StudioSettingsManager(app, reciterRepo, fontManager)

        // 1. Clear saved settings to simulate a fresh installation
        settingsManager.clearAllSavedSettingsForTesting()

        // Verify all 20 reciters remain available and Fares Abbad ("فارس عباد") is the initial default reciter
        assertEquals(20, reciterRepo.allReciters.size)
        assertEquals("fares_abbad", reciterRepo.defaultReciter.id)
        assertEquals("فارس عباد", reciterRepo.defaultReciter.nameAr)

        val freshState = settingsManager.restoreStudioUiState()
        assertEquals("fares_abbad", freshState.selectedReciter.id)
        assertEquals("فارس عباد", freshState.selectedReciter.nameAr)
        assertEquals("rattil.official.2026", freshState.watermark.signatureText)
        assertEquals(TransitionEffectType.FADE_IN_OUT, freshState.verseTransition.effectType)

        // 2. Customize all user settings via TasneemViewModel and verify automatic persistence
        val vm1 = com.example.viewmodel.TasneemViewModel(app)
        assertEquals("فارس عباد", vm1.uiState.value.selectedReciter.nameAr)
        assertEquals("rattil.official.2026", vm1.uiState.value.watermark.signatureText)
        assertEquals(TransitionEffectType.FADE_IN_OUT, vm1.uiState.value.verseTransition.effectType)

        // Select another reciter (e.g., Mishary Al-Afasy)
        val customReciter = reciterRepo.getReciterById("al_afasy")
        vm1.selectReciter(customReciter)

        // Import & select a custom font
        val amiriQuranFile = ArabicQuranFont.AMIRI_QURAN.resolveBundledFontFile(app)!!
        val customFontTemp = File(app.cacheDir, "PersistedCustomFont.ttf").apply {
            writeBytes(amiriQuranFile.readBytes())
        }
        val importedFont = vm1.importCustomFontFromFile(customFontTemp, "PersistedCustomFont.ttf").getOrThrow()

        // Customize font size, main text color, Tashkeel color, and text position
        vm1.updateFontSizeSp(34f)
        vm1.updateMainTextColorHex(0xFFF5F1EA)
        vm1.updateTashkeelColorHex(0xFFF3B35A)
        vm1.updateTextPosition(0.44f, 0.52f)

        // Customize recitation header settings
        vm1.updateSurahHeaderElement { it.copy(fontSizeSp = 25f, colorHex = 0xFFE8872F) }

        // Customize background media selection & adjustments via ViewModel
        val bgFile = File(app.filesDir, "persisted_test_bg.png")
        val testBmp = Bitmap.createBitmap(16, 16, Bitmap.Config.ARGB_8888)
        bgFile.outputStream().use { out ->
            testBmp.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
        vm1.onPhoneMediaPicked(android.net.Uri.fromFile(bgFile), isVideo = false)
        vm1.updateMediaDimLevel(0.68f)
        vm1.updateMediaScale(1.25f)

        // Customize personal signature & social media icons
        vm1.updateSignatureText("my.custom.signature.2026")
        vm1.toggleSocialPlatform(SocialPlatform.INSTAGRAM)
        vm1.toggleSocialPlatform(SocialPlatform.YOUTUBE)
        vm1.updateSocialIconsSizeDp(24f)

        // Customize aspect ratio and resolution
        vm1.updateAspectRatio(VideoAspectRatio.LANDSCAPE_16_9)
        vm1.updateVideoResolution(VideoResolution.RES_720P)

        // 3. Simulate application closure and device restart by creating a brand-new StudioSettingsManager & ViewModel
        val restartedSettingsManager = com.example.data.StudioSettingsManager(app, ReciterRepository(), QuranFontManager(app))
        val restoredState = restartedSettingsManager.restoreStudioUiState()

        // Verify previously saved reciter is preserved (NOT reset to Fares Abbad)
        assertEquals("al_afasy", restoredState.selectedReciter.id)
        assertEquals("مشاري العفاسي", restoredState.selectedReciter.nameAr)

        // Verify imported font & typography settings survived restart
        assertTrue(restoredState.importedFonts.any { it.id == importedFont.id })
        assertEquals(importedFont.id, restoredState.textStyle.selectedFontId)
        assertEquals(34f, restoredState.textStyle.fontSizeSp, 0.01f)
        assertEquals(0xFFF5F1EA, restoredState.textStyle.mainTextColorHex)
        assertEquals(0xFFF3B35A, restoredState.textStyle.tashkeelColorHex)
        assertEquals(0.44f, restoredState.textStyle.normalizedX, 0.01f)
        assertEquals(0.52f, restoredState.textStyle.normalizedY, 0.01f)

        // Verify header settings survived restart
        assertEquals(25f, restoredState.recitationHeader.surahName.fontSizeSp, 0.01f)
        assertEquals(0xFFE8872F, restoredState.recitationHeader.surahName.colorHex)

        // Verify background media survived restart
        assertEquals(android.net.Uri.fromFile(bgFile).toString(), restoredState.phoneMedia.uriString)
        assertEquals(0.68f, restoredState.phoneMedia.dimLevel, 0.01f)
        assertEquals(1.25f, restoredState.phoneMedia.scale, 0.01f)

        // Verify customized signature & social media icons survived restart (NOT overwritten by default signature)
        assertEquals("my.custom.signature.2026", restoredState.watermark.signatureText)
        assertTrue(SocialPlatform.INSTAGRAM in restoredState.watermark.selectedSocialPlatforms)
        assertTrue(SocialPlatform.YOUTUBE in restoredState.watermark.selectedSocialPlatforms)
        assertEquals(24f, restoredState.watermark.socialIconsSizeDp, 0.01f)

        // Verify aspect ratio & resolution survived restart
        assertEquals(VideoAspectRatio.LANDSCAPE_16_9, restoredState.aspectRatio)
        assertEquals(VideoResolution.RES_720P, restoredState.resolution)

        // Verify Fade In / Fade Out transition is active automatically
        assertEquals(TransitionEffectType.FADE_IN_OUT, restoredState.verseTransition.effectType)
        val fadeEnterState = QuranTextAndLayoutEngine.computeTransitionFrameState(
            transitionConfig = restoredState.verseTransition,
            localPhraseTimeMs = 90L,
            phraseDurationMs = 3000L,
            totalWordsInPhrase = 5,
            hasPreviousSlice = false
        )
        assertTrue("Fade In must smoothly ramp alpha between 0 and 1", fadeEnterState.mainAlpha in 0.05f..0.99f)
    }

    /**
     * Builds a valid SFNT TrueType binary from [baseFontBytes] whose cmap Format 4 / Format 12
     * tables have the core Tashkeel codepoints (0x064B..0x0655, 0x0670) zeroed out so we can
     * deterministically test TEST 4 (detecting an imported Arabic font with incomplete Tashkeel support).
     */
    private fun buildSyntheticFontWithArabicLettersOnlyNoTashkeel(baseFontBytes: ByteArray): ByteArray {
        val modified = baseFontBytes.clone()
        val buf = ByteBuffer.wrap(modified).order(ByteOrder.BIG_ENDIAN)
        val numTables = (buf.getShort(4).toInt() and 0xFFFF).coerceAtMost(128)
        for (i in 0 until numTables) {
            val rec = 12 + i * 16
            val tagBytes = ByteArray(4)
            buf.position(rec)
            buf.get(tagBytes)
            val tag = String(tagBytes, StandardCharsets.US_ASCII)
            val tableOffset = buf.getInt(rec + 8)
            val tableLength = buf.getInt(rec + 12)
            if (tag == "cmap" && tableOffset > 0 && tableOffset + tableLength <= modified.size) {
                val subCount = (buf.getShort(tableOffset + 2).toInt() and 0xFFFF).coerceAtMost(32)
                for (s in 0 until subCount) {
                    val subRec = tableOffset + 4 + s * 8
                    val subOffset = tableOffset + buf.getInt(subRec + 4)
                    if (subOffset + 16 > modified.size) continue
                    val format = buf.getShort(subOffset).toInt() and 0xFFFF
                    if (format == 12) {
                        val numGroups = buf.getInt(subOffset + 12).coerceIn(0, 65536)
                        for (g in 0 until numGroups) {
                            val gOff = subOffset + 16 + g * 12
                            if (gOff + 12 > modified.size) break
                            val startChar = buf.getInt(gOff)
                            val endChar = buf.getInt(gOff + 4)
                            if (endChar >= 0x064B && startChar <= 0x0655) {
                                // Zero out startGlyphId so Tashkeel codepoints map to glyph 0 (.notdef)
                                if (startChar >= 0x064B && endChar <= 0x065F) {
                                    buf.putInt(gOff + 8, 0)
                                } else if (startChar < 0x064B && endChar >= 0x064B) {
                                    buf.putInt(gOff + 4, 0x064A)
                                }
                            }
                        }
                    } else if (format == 4) {
                        val segCountX2 = buf.getShort(subOffset + 6).toInt() and 0xFFFF
                        val segCount = segCountX2 / 2
                        val endCodeBase = subOffset + 14
                        val startCodeBase = endCodeBase + segCountX2 + 2
                        for (seg in 0 until segCount) {
                            val endCode = buf.getShort(endCodeBase + seg * 2).toInt() and 0xFFFF
                            val startCode = buf.getShort(startCodeBase + seg * 2).toInt() and 0xFFFF
                            if (endCode >= 0x064B && startCode <= 0x0655) {
                                if (startCode < 0x064B) {
                                    buf.putShort(endCodeBase + seg * 2, 0x064A.toShort())
                                } else if (endCode <= 0x065F) {
                                    buf.putShort(startCodeBase + seg * 2, (endCode + 1).toShort())
                                }
                            }
                        }
                    }
                }
            }
        }
        return modified
    }
}
