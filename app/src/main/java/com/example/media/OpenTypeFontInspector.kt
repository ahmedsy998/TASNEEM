package com.example.media

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.text.Layout
import android.text.StaticLayout
import android.text.TextDirectionHeuristics
import android.text.TextPaint
import com.example.model.FontFileFormat
import com.example.model.FontQuranCompatibilityLevel
import com.example.model.OpenTypeCapabilitiesReport
import com.example.model.QuranFontValidationReport
import com.example.model.QuranFontValidationSampleResult
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.charset.StandardCharsets
import kotlin.math.abs

/**
 * Binary OpenType / TrueType (.TTF / .OTF) Font Inspector & Intelligent Quranic Validation Engine (Sections 3, 4, 5).
 *
 * Inspects actual SFNT tables (cmap, head, hhea, OS/2, name, GSUB, GPOS, GDEF) and performs
 * shaping, glyph coverage, mark-to-base, mark-to-mark (Shadda + Fatha/Damma/Kasra), and vertical
 * clipping verification across 10 authentic vocalized Quranic test samples.
 */
object OpenTypeFontInspector {

    data class ValidationSampleSpec(
        val id: String,
        val titleAr: String,
        val categoryAr: String,
        val sampleText: String,
        val requiredCodepoints: List<Int>,
        val requiresMarkToMark: Boolean = false,
        val requiresQuranicSymbols: Boolean = false
    )

    val VALIDATION_SAMPLES: List<ValidationSampleSpec> = listOf(
        ValidationSampleSpec(
            id = "sample_1_isolated_diacritics",
            titleAr = "١. الحروف المنفردة مع الحركات الأساسية",
            categoryAr = "الحركات المفردة (فتحة · ضمة · كسرة · سكون)",
            sampleText = "أَ إِ أُ بَ بِ بُ تْ ثَ جِ حُ دْ رَ زِ سُ شْ",
            requiredCodepoints = listOf(0x0628, 0x064E, 0x064F, 0x0650, 0x0652)
        ),
        ValidationSampleSpec(
            id = "sample_2_connected_words",
            titleAr = "٢. الكلمات العربية المتصلة والوصل العثماني",
            categoryAr = "اتصال الحروف (GSUB init/medi/fina)",
            sampleText = "بِسْمِ ٱللَّهِ ٱلرَّحْمَٰنِ ٱلرَّحِيمِ",
            requiredCodepoints = listOf(0x0628, 0x0633, 0x0645, 0x064E, 0x0650, 0x0651, 0x0652, 0x0670, 0x0671),
            requiresMarkToMark = true
        ),
        ValidationSampleSpec(
            id = "sample_3_shadda_fatha",
            titleAr = "٣. الشدة مع الفتحة (تراكب الحركات فوق الحرف)",
            categoryAr = "الشدة المفتوحة (GPOS Mark-to-Mark)",
            sampleText = "إِيَّاكَ نَعْبُدُ وَإِيَّاكَ نَسْتَعِينُ · رَبَّنَا تَقَبَّلْ مِنَّا",
            requiredCodepoints = listOf(0x0651, 0x064E),
            requiresMarkToMark = true
        ),
        ValidationSampleSpec(
            id = "sample_4_shadda_damma",
            titleAr = "٤. الشدة مع الضمة",
            categoryAr = "الشدة المضمومة (GPOS Mark-to-Mark)",
            sampleText = "يُحِبُّهُمْ وَيُحِبُّونَهُۥ · أَشَدُّ حُبًّا لِّلَّهِ",
            requiredCodepoints = listOf(0x0651, 0x064F),
            requiresMarkToMark = true
        ),
        ValidationSampleSpec(
            id = "sample_5_shadda_kasra",
            titleAr = "٥. الشدة مع الكسرة",
            categoryAr = "الشدة المكسورة (تموضع الكسرة مع الشدة)",
            sampleText = "فَسَبِّحْ بِحَمْدِ رَبِّكَ · وَعَلِّمْنَا مِن لَّدُنكَ عِلْمًا",
            requiredCodepoints = listOf(0x0651, 0x0650),
            requiresMarkToMark = true
        ),
        ValidationSampleSpec(
            id = "sample_6_tanween",
            titleAr = "٦. التنوين (فتح · ضم · كسر) مع التشديد",
            categoryAr = "أحكام التنوين (ً ٌ ٍ)",
            sampleText = "كِتَٰبٌ مُّبِينٌ · عَيْنًا يَشْرَبُ بِهَا · مَّآءٍ مَّعِينٍ",
            requiredCodepoints = listOf(0x064B, 0x064C, 0x064D, 0x0651),
            requiresMarkToMark = true
        ),
        ValidationSampleSpec(
            id = "sample_7_sukun",
            titleAr = "٧. السكون والقلقلة والهمس",
            categoryAr = "السكون (ْ / ۡ)",
            sampleText = "قُلْ هُوَ ٱللَّهُ أَحَدٌ · لَمْ يَلِدْ وَلَمْ يُولَدْ",
            requiredCodepoints = listOf(0x0652, 0x064E, 0x064F, 0x064C)
        ),
        ValidationSampleSpec(
            id = "sample_8_maddah_dagger_alef",
            titleAr = "٨. علامات المدّ والألف الخنجرية",
            categoryAr = "المدود والألف الصغيرة (ٓ · ٰ)",
            sampleText = "وَلَا ٱلضَّآلِّينَ · مَآ أَنزَلْنَا عَلَيْكَ ٱلْقُرْءَانَ لِتَشْقَىٰ",
            requiredCodepoints = listOf(0x0653, 0x0670, 0x0651),
            requiresMarkToMark = true
        ),
        ValidationSampleSpec(
            id = "sample_9_quranic_symbols",
            titleAr = "٩. رموز الوقف والضبط المصحفي وأرقام الآيات",
            categoryAr = "رموز مصحف المدينة (ۖ ۗ ۚ ۛ ﴿١﴾)",
            sampleText = "ذَٰلِكَ ٱلْكِتَٰبُ لَا رَيْبَ ۛ فِيهِ ۛ هُدًى لِّلْمُتَّقِينَ ﴿٢﴾",
            requiredCodepoints = listOf(0x0670, 0x0671, 0x06DB),
            requiresMarkToMark = true,
            requiresQuranicSymbols = true
        ),
        ValidationSampleSpec(
            id = "sample_10_long_quranic_phrase",
            titleAr = "١٠. العبارات القرآنية الطويلة كثيفة التشكيل",
            categoryAr = "اختبار الاتزان البصري وعدم اقتصاص الحركات العلوية والسفلية",
            sampleText = "فَسَيَكْفِيكَهُمُ ٱللَّهُ ۚ وَهُوَ ٱلسَّمِيعُ ٱلْعَلِيمُ ﴿١٣٧﴾",
            requiredCodepoints = listOf(0x064E, 0x064F, 0x0650, 0x0651, 0x0652, 0x0671, 0x06DA),
            requiresMarkToMark = true,
            requiresQuranicSymbols = true
        )
    )

    private val BASIC_ARABIC_LETTERS: List<Int> = listOf(
        0x0621, 0x0622, 0x0623, 0x0624, 0x0625, 0x0626, 0x0627, 0x0628, 0x0629, 0x062A,
        0x062B, 0x062C, 0x062D, 0x062E, 0x062F, 0x0630, 0x0631, 0x0632, 0x0633, 0x0634,
        0x0635, 0x0636, 0x0637, 0x0638, 0x0639, 0x063A, 0x0641, 0x0642, 0x0643, 0x0644,
        0x0645, 0x0646, 0x0647, 0x0648, 0x0649, 0x064A
    )

    private val CORE_TASHKEEL_MARKS: Map<Int, String> = linkedMapOf(
        0x064E to "الفتحة (َ)",
        0x064F to "الضمة (ُ)",
        0x0650 to "الكسرة (ِ)",
        0x064B to "تنوين الفتح (ً)",
        0x064C to "تنوين الضم (ٌ)",
        0x064D to "تنوين الكسر (ٍ)",
        0x0652 to "السكون (ْ)",
        0x0651 to "الشدة (ّ)",
        0x0653 to "المدّة (ٓ)",
        0x0654 to "الهمزة العلوية (ٔ)",
        0x0670 to "الألف الخنجرية (ٰ)"
    )

    private val QURANIC_UTHMANI_SYMBOLS: Map<Int, String> = linkedMapOf(
        0x0671 to "ألف الوصل العثمانية (ٱ)",
        0x06D6 to "علامة الوقف (ۖ)",
        0x06D7 to "علامة الوصل أولى (ۗ)",
        0x06D8 to "علامة الوقف اللازم ()",
        0x06DA to "علامة الوقف الجائز (ۚ)",
        0x06DB to "علامة تعانق الوقف (ۛ)",
        0x06DC to "السين العلوية (ۜ)",
        0x06DD to "رمز نهاية الآية (۝)",
        0x06E1 to "السكون المصحفي (ۡ)",
        0x06E5 to "الواو الصغيرة (ۥ)",
        0x06E6 to "الياء الصغيرة (ۦ)"
    )

    private data class TableEntry(
        val tag: String,
        val offset: Int,
        val length: Int
    )

    data class ParsedFontBinary(
        val report: OpenTypeCapabilitiesReport,
        val supportedCodepoints: Set<Int>
    )

    /**
     * Quick check of the first 4 bytes to verify whether a file is a genuine TTF or OTF font.
     */
    fun detectFontFormat(bytes: ByteArray): FontFileFormat {
        if (bytes.size < 12) return FontFileFormat.UNKNOWN
        val b0 = bytes[0].toInt() and 0xFF
        val b1 = bytes[1].toInt() and 0xFF
        val b2 = bytes[2].toInt() and 0xFF
        val b3 = bytes[3].toInt() and 0xFF
        val magic = (b0 shl 24) or (b1 shl 16) or (b2 shl 8) or b3
        return when (magic) {
            0x00010000, 0x74727565 -> FontFileFormat.TTF // 0x00010000 or 'true'
            0x4F54544F -> FontFileFormat.OTF             // 'OTTO'
            else -> FontFileFormat.UNKNOWN
        }
    }

    /**
     * Parses the binary SFNT tables of a .ttf or .otf file and extracts full OpenType capabilities
     * and supported Unicode codepoints.
     */
    fun inspectFontBytes(bytes: ByteArray, fallbackFileName: String = "Font"): ParsedFontBinary {
        val format = detectFontFormat(bytes)
        if (format == FontFileFormat.UNKNOWN || bytes.size < 32) {
            return ParsedFontBinary(
                report = OpenTypeCapabilitiesReport(
                    format = FontFileFormat.UNKNOWN,
                    sfntVersionTag = "INVALID",
                    internalFamilyName = fallbackFileName,
                    fullFontName = fallbackFileName,
                    postScriptName = fallbackFileName,
                    unitsPerEm = 1000,
                    ascender = 800,
                    descender = -200,
                    yMin = -200,
                    yMax = 800,
                    tableTags = emptySet(),
                    hasCmapTable = false,
                    hasGsubTable = false,
                    hasGposTable = false,
                    hasGdefTable = false,
                    hasArabicScriptInGsub = false,
                    hasArabicScriptInGpos = false,
                    hasArabicJoiningFeatures = false,
                    hasArabicLigatureFeatures = false,
                    hasMarkToBaseGpos = false,
                    hasMarkToMarkGpos = false,
                    basicArabicLetterCount = 0,
                    totalBasicArabicLetters = BASIC_ARABIC_LETTERS.size,
                    coreTashkeelCount = 0,
                    totalCoreTashkeelMarks = CORE_TASHKEEL_MARKS.size,
                    quranicSymbolCount = 0,
                    totalQuranicSymbols = QURANIC_UTHMANI_SYMBOLS.size,
                    supportedCodepointsCount = 0,
                    missingCoreTashkeelMarksAr = CORE_TASHKEEL_MARKS.values.toList(),
                    missingQuranicSymbolsAr = QURANIC_UTHMANI_SYMBOLS.values.toList()
                ),
                supportedCodepoints = emptySet()
            )
        }

        val buf = ByteBuffer.wrap(bytes).order(ByteOrder.BIG_ENDIAN)
        val sfntMagic = buf.getInt(0)
        val sfntVersionTag = if (sfntMagic == 0x4F54544F) "OTTO" else "0x00010000"
        val numTables = (buf.getShort(4).toInt() and 0xFFFF).coerceAtMost(128)

        val tables = LinkedHashMap<String, TableEntry>()
        for (i in 0 until numTables) {
            val recordOffset = 12 + i * 16
            if (recordOffset + 16 > bytes.size) break
            val tagBytes = ByteArray(4)
            buf.position(recordOffset)
            buf.get(tagBytes)
            val tag = String(tagBytes, StandardCharsets.US_ASCII)
            val tableOffset = buf.getInt(recordOffset + 8)
            val tableLength = buf.getInt(recordOffset + 12)
            if (tableOffset > 0 && tableLength > 0 && tableOffset.toLong() + tableLength.toLong() <= bytes.size.toLong()) {
                tables[tag] = TableEntry(tag, tableOffset, tableLength)
            }
        }

        // 1. Parse 'head' table
        var unitsPerEm = 1000
        var yMin = -300
        var yMax = 1000
        tables["head"]?.let { head ->
            if (head.length >= 44) {
                unitsPerEm = (buf.getShort(head.offset + 18).toInt() and 0xFFFF).coerceAtLeast(64)
                yMin = buf.getShort(head.offset + 38).toInt()
                yMax = buf.getShort(head.offset + 42).toInt()
            }
        }

        // 2. Parse 'hhea' table
        var ascender = 850
        var descender = -250
        tables["hhea"]?.let { hhea ->
            if (hhea.length >= 10) {
                ascender = buf.getShort(hhea.offset + 4).toInt()
                descender = buf.getShort(hhea.offset + 6).toInt()
            }
        }

        // 3. Parse 'name' table
        val cleanFallback = fallbackFileName.substringBeforeLast('.')
        var familyName = cleanFallback
        var fullName = cleanFallback
        var postScriptName = cleanFallback
        tables["name"]?.let { nameTbl ->
            val extracted = parseNameTable(buf, nameTbl.offset, nameTbl.length)
            familyName = extracted[16]?.takeIf { it.isNotBlank() }
                ?: extracted[1]?.takeIf { it.isNotBlank() }
                ?: cleanFallback
            fullName = extracted[4]?.takeIf { it.isNotBlank() } ?: familyName
            postScriptName = extracted[6]?.takeIf { it.isNotBlank() } ?: familyName
        }

        // 4. Parse 'cmap' table for exact Unicode codepoint coverage
        val supportedCodepoints = tables["cmap"]?.let { cmapTbl ->
            parseCmapTable(buf, cmapTbl.offset, cmapTbl.length)
        } ?: emptySet()

        // 5. Parse 'GSUB' table for Arabic script & joining/ligature features
        var hasArabicGsub = false
        var hasJoiningGsub = false
        var hasLigatureGsub = false
        tables["GSUB"]?.let { gsub ->
            val info = parseLayoutTableScriptsAndFeatures(buf, gsub.offset, gsub.length, isGpos = false)
            hasArabicGsub = info.hasArabicScript
            hasJoiningGsub = info.featureTags.any { it in setOf("init", "medi", "fina", "isol") }
            hasLigatureGsub = info.featureTags.any { it in setOf("rlig", "liga", "calt", "ccmp") }
        }

        // 6. Parse 'GPOS' table for Arabic script, Mark-to-Base ('mark') & Mark-to-Mark ('mkmk')
        var hasArabicGpos = false
        var hasMarkToBase = false
        var hasMarkToMark = false
        tables["GPOS"]?.let { gpos ->
            val info = parseLayoutTableScriptsAndFeatures(buf, gpos.offset, gpos.length, isGpos = true)
            hasArabicGpos = info.hasArabicScript
            hasMarkToBase = ("mark" in info.featureTags) || (4 in info.lookupTypes) || (5 in info.lookupTypes)
            hasMarkToMark = ("mkmk" in info.featureTags) || (6 in info.lookupTypes)
        }

        val basicSupported = BASIC_ARABIC_LETTERS.count { it in supportedCodepoints }
        val missingTashkeel = CORE_TASHKEEL_MARKS.filterKeys { it !in supportedCodepoints }.values.toList()
        val coreTashkeelSupported = CORE_TASHKEEL_MARKS.size - missingTashkeel.size

        val missingQuranicSymbols = QURANIC_UTHMANI_SYMBOLS.filterKeys { it !in supportedCodepoints }.values.toList()
        val quranicSymbolsSupported = QURANIC_UTHMANI_SYMBOLS.size - missingQuranicSymbols.size

        val report = OpenTypeCapabilitiesReport(
            format = format,
            sfntVersionTag = sfntVersionTag,
            internalFamilyName = familyName,
            fullFontName = fullName,
            postScriptName = postScriptName,
            unitsPerEm = unitsPerEm,
            ascender = ascender,
            descender = descender,
            yMin = yMin,
            yMax = yMax,
            tableTags = tables.keys,
            hasCmapTable = "cmap" in tables,
            hasGsubTable = "GSUB" in tables,
            hasGposTable = "GPOS" in tables,
            hasGdefTable = "GDEF" in tables,
            hasArabicScriptInGsub = hasArabicGsub,
            hasArabicScriptInGpos = hasArabicGpos,
            hasArabicJoiningFeatures = hasJoiningGsub,
            hasArabicLigatureFeatures = hasLigatureGsub,
            hasMarkToBaseGpos = hasMarkToBase,
            hasMarkToMarkGpos = hasMarkToMark,
            basicArabicLetterCount = basicSupported,
            totalBasicArabicLetters = BASIC_ARABIC_LETTERS.size,
            coreTashkeelCount = coreTashkeelSupported,
            totalCoreTashkeelMarks = CORE_TASHKEEL_MARKS.size,
            quranicSymbolCount = quranicSymbolsSupported,
            totalQuranicSymbols = QURANIC_UTHMANI_SYMBOLS.size,
            supportedCodepointsCount = supportedCodepoints.size,
            missingCoreTashkeelMarksAr = missingTashkeel,
            missingQuranicSymbolsAr = missingQuranicSymbols
        )

        return ParsedFontBinary(
            report = report,
            supportedCodepoints = supportedCodepoints
        )
    }

    /**
     * Performs full OpenType + Android HarfBuzz shaping and rendering validation on a font file (Sections 4 & 5).
     */
    fun validateFontFile(
        fontFile: File,
        fontId: String,
        displayNameOverride: String? = null,
        loadedTypeface: Typeface? = null
    ): QuranFontValidationReport {
        val bytes = try {
            if (fontFile.exists() && fontFile.length() in 64L..(25L * 1024L * 1024L)) {
                fontFile.readBytes()
            } else {
                ByteArray(0)
            }
        } catch (_: Exception) {
            ByteArray(0)
        }

        val parsed = inspectFontBytes(bytes, fontFile.nameWithoutExtension)
        val ot = parsed.report
        val codepoints = parsed.supportedCodepoints
        val resolvedName = displayNameOverride?.takeIf { it.isNotBlank() } ?: ot.internalFamilyName

        if (ot.format == FontFileFormat.UNKNOWN || ot.basicArabicCoverageRatio < 0.70f) {
            return QuranFontValidationReport(
                fontId = fontId,
                fontDisplayName = resolvedName,
                compatibilityLevel = FontQuranCompatibilityLevel.INCOMPATIBLE_NO_ARABIC,
                isUsableInGallery = false,
                isFullyQuranCompatible = false,
                requiresFallbackForQuran = true,
                canGuaranteeMixedMarkAttachment = false,
                hasMissingCharacters = true,
                hasDetachedMarks = true,
                hasIncorrectMarkPositioning = true,
                hasOverlappingDiacritics = true,
                hasBrokenJoiningOrLigatures = true,
                hasVerticalClippingRisk = false,
                openTypeReport = ot,
                sampleResults = emptyList(),
                summaryMessageAr = "الملف المختار لا يدعم الحروف العربية الأساسية أو ليس ملف خط صالحًا (.TTF / .OTF).",
                warningMessageAr = "تم رفض الخط لأنه يفتقر إلى الحروف العربية الأساسية ولا يمكن استخدامه لعرض القرآن الكريم."
            )
        }

        val typeface = loadedTypeface ?: try {
            Typeface.createFromFile(fontFile)
        } catch (_: Exception) {
            null
        }

        // Check shaping & advance width of base letters vs base letters + diacritics
        val paint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            if (typeface != null) this.typeface = typeface
            textSize = 48f
        }

        // 1. Detached mark check:
        // In a valid Arabic font with GDEF/GPOS Mark attachment, "بَ" has virtually the same advance width as "ب",
        // whereas if diacritics are spacing glyphs without mark attachment, "بَ" and "بَّ" become much wider.
        // Note: Under JVM Robolectric ShadowPaint, measureText() returns string length ("بَ" == "اب" == 2.0f),
        // so only apply runtime advance-width heuristics when native HarfBuzz shaping is active ("بَ" != "اب").
        val baseBaWidth = paint.measureText("ب").coerceAtLeast(1f)
        val vocalizedBaWidth = paint.measureText("بَ")
        val twoBaseLettersWidth = paint.measureText("اب")
        val shaddaBaWidth = paint.measureText("بَّ")
        val isNativeHarfBuzzMeasurement = abs(vocalizedBaWidth - twoBaseLettersWidth) > 0.05f
        val measuredDetached = isNativeHarfBuzzMeasurement &&
            ((vocalizedBaWidth > baseBaWidth * 1.42f) || (shaddaBaWidth > baseBaWidth * 1.65f))
        val hasDetachedMarks = !ot.hasMarkToBaseGpos || !ot.hasGdefTable || measuredDetached

        // 2. Arabic joining check:
        // Connected "بسم" vs spaced "ب س م" + GSUB joining features
        val hasJoining = ot.hasArabicJoiningFeatures && ot.hasArabicScriptInGsub
        val hasBrokenJoining = !hasJoining

        // 3. Mark-to-Mark (Shadda + Fatha/Damma/Kasra) overlap check:
        val hasOverlappingDiacritics = !ot.hasMarkToMarkGpos

        // 4. Vertical clipping check across StaticLayout(includePad = true):
        val verticalClippingRisk = checkVerticalClippingOnTallSample(paint, ot)

        // 5. Evaluate all 10 Quranic validation samples
        val sampleResults = VALIDATION_SAMPLES.map { spec ->
            val missingInSample = spec.requiredCodepoints.filter { cp -> cp !in codepoints }
            val hasAllSampleGlyphs = missingInSample.isEmpty()
            val marksAttachedForSample = hasAllSampleGlyphs &&
                ot.hasMarkToBaseGpos &&
                (!spec.requiresMarkToMark || ot.hasMarkToMarkGpos) &&
                !hasDetachedMarks

            val samplePassed = hasAllSampleGlyphs && marksAttachedForSample && !hasBrokenJoining
            val note = when {
                !hasAllSampleGlyphs -> {
                    val missingLetters = missingInSample.map { cp ->
                        CORE_TASHKEEL_MARKS[cp] ?: QURANIC_UTHMANI_SYMBOLS[cp] ?: "U+${cp.toString(16).uppercase()}"
                    }
                    "ينقص الخط بعض الرموز أو الحركات: ${missingLetters.joinToString("، ")} (سيتم تعويضها بالخط الاحتياطي)"
                }
                !marksAttachedForSample -> {
                    "الخط يفتقر إلى جداول التموضع الدقيق (GPOS Mark/Mkmk) لتراكب الشدة مع الحركات"
                }
                else -> {
                    "تم التحقق من سلامة الحروف واتصالها وتموضع التشكيل بنجاح"
                }
            }

            QuranFontValidationSampleResult(
                id = spec.id,
                titleAr = spec.titleAr,
                categoryAr = spec.categoryAr,
                sampleText = spec.sampleText,
                passed = samplePassed,
                hasAllGlyphs = hasAllSampleGlyphs,
                marksProperlyAttached = marksAttachedForSample,
                noVerticalClipping = !verticalClippingRisk,
                diagnosticNoteAr = note
            )
        }

        val hasAllCoreTashkeel = ot.missingCoreTashkeelMarksAr.isEmpty()
        val hasAllPrimaryVowelsAndShadda = listOf(0x064E, 0x064F, 0x0650, 0x0651, 0x0652, 0x064B, 0x064C, 0x064D)
            .all { it in codepoints }
        val hasAllQuranicSymbols = ot.missingQuranicSymbolsAr.isEmpty()
        val hasProperShapingAndPositioning = ot.hasArabicJoiningFeatures && ot.hasMarkToBaseGpos && ot.hasMarkToMarkGpos && !hasDetachedMarks

        val compatibilityLevel = when {
            hasAllCoreTashkeel && hasAllQuranicSymbols && hasProperShapingAndPositioning -> {
                FontQuranCompatibilityLevel.FULLY_VERIFIED_QURANIC
            }
            hasAllPrimaryVowelsAndShadda && ot.hasArabicJoiningFeatures && ot.hasMarkToBaseGpos && !hasDetachedMarks -> {
                FontQuranCompatibilityLevel.REQUIRES_VISUAL_VERIFICATION_OR_FALLBACK
            }
            else -> {
                FontQuranCompatibilityLevel.PARTIAL_ARABIC_INCOMPLETE_TASHKEEL
            }
        }

        val warningMessageAr: String? = when (compatibilityLevel) {
            FontQuranCompatibilityLevel.FULLY_VERIFIED_QURANIC -> null
            FontQuranCompatibilityLevel.REQUIRES_VISUAL_VERIFICATION_OR_FALLBACK -> {
                val missingList = (ot.missingCoreTashkeelMarksAr + ot.missingQuranicSymbolsAr).take(5)
                if (missingList.isNotEmpty()) {
                    "تنبيه: هذا الخط يدعم الحروف والتشكيل الأساسي، لكنه يفتقر إلى بعض رموز الضبط العثماني (${missingList.joinToString("، ")}). تم تفعيل «استخدام خط احتياطي للتشكيل القرآني» تلقائيًا لضمان دقة الآيات."
                } else {
                    "تنبيه: هذا الخط يدعم التشكيل الأساسي ويتطلب التحقق البصري في المعاينة؛ تم تفعيل الخط الاحتياطي للرموز المصحفية تلقائيًا."
                }
            }
            FontQuranCompatibilityLevel.PARTIAL_ARABIC_INCOMPLETE_TASHKEEL -> {
                val reasons = mutableListOf<String>()
                if (ot.missingCoreTashkeelMarksAr.isNotEmpty()) {
                    reasons.add("نقص الحركات (${ot.missingCoreTashkeelMarksAr.take(4).joinToString("، ")})")
                }
                if (!ot.hasMarkToBaseGpos || hasDetachedMarks) {
                    reasons.add("عدم دعم ربط التشكيل بالحروف (GPOS Mark-to-Base)")
                }
                if (!ot.hasMarkToMarkGpos) {
                    reasons.add("عدم دعم تراكب الشدة مع الحركات (GPOS Mark-to-Mark)")
                }
                "تحذير هام: هذا الخط غير مكتمل التشكيل القرآني بسبب ${reasons.joinToString(" و")}. حفاظًا على قدسية النص القرآني ومنع انفصال أو اختفاء الحركات، يستخدم التطبيق الخط القرآني الاحتياطي للنص المشكول بينما يبقى الخط متاحًا للنصوص العادية."
            }
            FontQuranCompatibilityLevel.INCOMPATIBLE_NO_ARABIC -> {
                "هذا الخط لا يدعم الحروف العربية."
            }
        }

        val summaryMessageAr = when (compatibilityLevel) {
            FontQuranCompatibilityLevel.FULLY_VERIFIED_QURANIC ->
                "اجتاز الخط جميع اختبارات الحروف العربية والتشكيل القرآني والشدة والمدود وعلامات الوقف (١٠/١٠)."
            FontQuranCompatibilityLevel.REQUIRES_VISUAL_VERIFICATION_OR_FALLBACK ->
                "يدعم الخط الحروف العربية والتشكيل الأساسي مع تعويض الرموز المصحفية العثمانية عبر الخط الاحتياطي."
            FontQuranCompatibilityLevel.PARTIAL_ARABIC_INCOMPLETE_TASHKEEL ->
                "يدعم الخط الحروف العربية العادية فقط ويفتقر إلى متطلبات التشكيل القرآني الكامل؛ تم تفعيل الحماية بالخط الاحتياطي."
            FontQuranCompatibilityLevel.INCOMPATIBLE_NO_ARABIC ->
                "الخط غير صالح للكتابة العربية."
        }

        return QuranFontValidationReport(
            fontId = fontId,
            fontDisplayName = resolvedName,
            compatibilityLevel = compatibilityLevel,
            isUsableInGallery = true,
            isFullyQuranCompatible = compatibilityLevel == FontQuranCompatibilityLevel.FULLY_VERIFIED_QURANIC,
            requiresFallbackForQuran = compatibilityLevel != FontQuranCompatibilityLevel.FULLY_VERIFIED_QURANIC,
            canGuaranteeMixedMarkAttachment = hasAllPrimaryVowelsAndShadda && ot.hasMarkToBaseGpos && ot.hasMarkToMarkGpos && !hasDetachedMarks,
            hasMissingCharacters = !hasAllCoreTashkeel || !hasAllQuranicSymbols,
            hasDetachedMarks = hasDetachedMarks,
            hasIncorrectMarkPositioning = !ot.hasMarkToBaseGpos,
            hasOverlappingDiacritics = hasOverlappingDiacritics,
            hasBrokenJoiningOrLigatures = hasBrokenJoining,
            hasVerticalClippingRisk = verticalClippingRisk,
            openTypeReport = ot,
            sampleResults = sampleResults,
            summaryMessageAr = summaryMessageAr,
            warningMessageAr = warningMessageAr
        )
    }

    private fun checkVerticalClippingOnTallSample(paint: TextPaint, ot: OpenTypeCapabilitiesReport): Boolean {
        return try {
            val sample = "ٱلرَّحْمَٰنِ ٱلضَّآلِّينَ ﴿١﴾"
            val layout = StaticLayout.Builder
                .obtain(sample, 0, sample.length, paint, 800)
                .setAlignment(Layout.Alignment.ALIGN_CENTER)
                .setTextDirection(TextDirectionHeuristics.RTL)
                .setIncludePad(true)
                .build()
            val height = layout.height
            if (height <= 0) return false
            val bmp = Bitmap.createBitmap(800, height.coerceAtLeast(32), Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bmp)
            layout.draw(canvas)
            // Verify top row (y=0) and bottom row (y=height-1) are not cut through active glyph ink
            var topRowInk = 0
            var bottomRowInk = 0
            for (x in 0 until bmp.width step 4) {
                if (Color.alpha(bmp.getPixel(x, 0)) > 64) topRowInk++
                if (Color.alpha(bmp.getPixel(x, bmp.height - 1)) > 64) bottomRowInk++
            }
            bmp.recycle()
            (topRowInk > 6 || bottomRowInk > 6) && (abs(ot.yMax) > ot.unitsPerEm * 2)
        } catch (_: Exception) {
            false
        }
    }

    private fun parseNameTable(buf: ByteBuffer, tableOffset: Int, tableLength: Int): Map<Int, String> {
        val result = HashMap<Int, String>()
        if (tableLength < 6) return result
        return try {
            val count = (buf.getShort(tableOffset + 2).toInt() and 0xFFFF).coerceAtMost(256)
            val stringOffset = buf.getShort(tableOffset + 4).toInt() and 0xFFFF
            val storageStart = tableOffset + stringOffset

            for (i in 0 until count) {
                val recOffset = tableOffset + 6 + i * 12
                if (recOffset + 12 > tableOffset + tableLength) break
                val platformId = buf.getShort(recOffset).toInt() and 0xFFFF
                val encodingId = buf.getShort(recOffset + 2).toInt() and 0xFFFF
                val nameId = buf.getShort(recOffset + 6).toInt() and 0xFFFF
                val length = buf.getShort(recOffset + 8).toInt() and 0xFFFF
                val offset = buf.getShort(recOffset + 10).toInt() and 0xFFFF

                if (nameId !in setOf(1, 4, 6, 16)) continue
                val strPos = storageStart + offset
                if (length <= 0 || strPos < 0 || strPos + length > buf.capacity()) continue

                val strBytes = ByteArray(length)
                buf.position(strPos)
                buf.get(strBytes)

                val decoded = when {
                    platformId == 3 || platformId == 0 -> {
                        String(strBytes, StandardCharsets.UTF_16BE).trim { it <= ' ' || it == '\u0000' }
                    }
                    platformId == 1 && encodingId == 0 -> {
                        String(strBytes, StandardCharsets.ISO_8859_1).trim { it <= ' ' || it == '\u0000' }
                    }
                    else -> {
                        String(strBytes, StandardCharsets.UTF_8).trim { it <= ' ' || it == '\u0000' }
                    }
                }

                if (decoded.isNotBlank() && (platformId == 3 || !result.containsKey(nameId))) {
                    result[nameId] = decoded
                }
            }
            result
        } catch (_: Exception) {
            result
        }
    }

    private fun parseCmapTable(buf: ByteBuffer, tableOffset: Int, tableLength: Int): Set<Int> {
        val codepoints = HashSet<Int>(1024)
        if (tableLength < 4) return codepoints
        return try {
            val numTables = (buf.getShort(tableOffset + 2).toInt() and 0xFFFF).coerceAtMost(64)
            val format12Offsets = LinkedHashSet<Int>()
            val format4Offsets = LinkedHashSet<Int>()

            for (i in 0 until numTables) {
                val rec = tableOffset + 4 + i * 8
                if (rec + 8 > tableOffset + tableLength) break
                val platformId = buf.getShort(rec).toInt() and 0xFFFF
                val subOffset = buf.getInt(rec + 4)
                val absOffset = tableOffset + subOffset
                if (absOffset < 0 || absOffset + 4 > buf.capacity()) continue
                val format = buf.getShort(absOffset).toInt() and 0xFFFF
                if (format == 12 && (platformId == 3 || platformId == 0)) {
                    format12Offsets.add(absOffset)
                } else if (format == 4 && (platformId == 3 || platformId == 0)) {
                    format4Offsets.add(absOffset)
                } else if (format == 4 && format4Offsets.isEmpty()) {
                    format4Offsets.add(absOffset)
                }
            }

            for (off in format12Offsets) {
                parseCmapFormat12(buf, off, codepoints)
            }
            for (off in format4Offsets) {
                parseCmapFormat4(buf, off, codepoints)
            }
            codepoints
        } catch (_: Exception) {
            codepoints
        }
    }

    private fun parseCmapFormat12(buf: ByteBuffer, subOffset: Int, out: MutableSet<Int>) {
        if (subOffset + 16 > buf.capacity()) return
        val numGroups = buf.getInt(subOffset + 12).coerceIn(0, 65536)
        for (i in 0 until numGroups) {
            val gOff = subOffset + 16 + i * 12
            if (gOff + 12 > buf.capacity()) break
            val startChar = buf.getInt(gOff)
            val endChar = buf.getInt(gOff + 4)
            val startGlyphId = buf.getInt(gOff + 8)
            // Focus on BMP + Arabic blocks (0x0020..0xFEFF) to keep set compact and fast
            if (endChar < 0x0020 || startChar > 0xFEFF) continue
            val clampedStart = startChar.coerceAtLeast(0x0020)
            val clampedEnd = endChar.coerceAtMost(0xFEFF)
            if (clampedEnd >= clampedStart && (clampedEnd - clampedStart) <= 8192) {
                for (cp in clampedStart..clampedEnd) {
                    val glyphId = startGlyphId + (cp - startChar)
                    if (glyphId != 0) out.add(cp)
                }
            }
        }
    }

    private fun parseCmapFormat4(buf: ByteBuffer, subOffset: Int, out: MutableSet<Int>) {
        if (subOffset + 14 > buf.capacity()) return
        val segCountX2 = (buf.getShort(subOffset + 6).toInt() and 0xFFFF)
        val segCount = (segCountX2 / 2).coerceIn(0, 4096)
        val endCodeBase = subOffset + 14
        val startCodeBase = endCodeBase + segCountX2 + 2
        val idDeltaBase = startCodeBase + segCountX2
        val idRangeOffsetBase = idDeltaBase + segCountX2
        if (idRangeOffsetBase + segCountX2 > buf.capacity()) return

        for (i in 0 until segCount) {
            val endCode = buf.getShort(endCodeBase + i * 2).toInt() and 0xFFFF
            val startCode = buf.getShort(startCodeBase + i * 2).toInt() and 0xFFFF
            if (startCode == 0xFFFF && endCode == 0xFFFF) break
            val idDelta = buf.getShort(idDeltaBase + i * 2).toInt()
            val idRangePos = idRangeOffsetBase + i * 2
            val idRangeOffset = buf.getShort(idRangePos).toInt() and 0xFFFF

            val clampedStart = startCode.coerceAtLeast(0x0020)
            val clampedEnd = endCode.coerceAtMost(0xFEFF)
            if (clampedEnd < clampedStart || (clampedEnd - clampedStart) > 8192) continue

            for (cp in clampedStart..clampedEnd) {
                val glyphId = if (idRangeOffset == 0) {
                    (cp + idDelta) and 0xFFFF
                } else {
                    val glyphIndexOffset = idRangePos + idRangeOffset + (cp - startCode) * 2
                    if (glyphIndexOffset >= 0 && glyphIndexOffset + 2 <= buf.capacity()) {
                        val rawGlyph = buf.getShort(glyphIndexOffset).toInt() and 0xFFFF
                        if (rawGlyph != 0) (rawGlyph + idDelta) and 0xFFFF else 0
                    } else 0
                }
                if (glyphId != 0) {
                    out.add(cp)
                }
            }
        }
    }

    private data class LayoutTableInfo(
        val hasArabicScript: Boolean,
        val featureTags: Set<String>,
        val lookupTypes: Set<Int>
    )

    private fun parseLayoutTableScriptsAndFeatures(
        buf: ByteBuffer,
        tableOffset: Int,
        tableLength: Int,
        isGpos: Boolean
    ): LayoutTableInfo {
        if (tableLength < 10) return LayoutTableInfo(false, emptySet(), emptySet())
        var hasArabic = false
        val features = HashSet<String>()
        val lookupTypes = HashSet<Int>()

        try {
            val scriptListRel = buf.getShort(tableOffset + 4).toInt() and 0xFFFF
            val featureListRel = buf.getShort(tableOffset + 6).toInt() and 0xFFFF
            val lookupListRel = buf.getShort(tableOffset + 8).toInt() and 0xFFFF

            // 1. ScriptList
            if (scriptListRel > 0 && scriptListRel + 2 <= tableLength) {
                val scriptListAbs = tableOffset + scriptListRel
                val scriptCount = (buf.getShort(scriptListAbs).toInt() and 0xFFFF).coerceAtMost(128)
                for (i in 0 until scriptCount) {
                    val rec = scriptListAbs + 2 + i * 6
                    if (rec + 6 > tableOffset + tableLength) break
                    val tagBytes = ByteArray(4)
                    buf.position(rec)
                    buf.get(tagBytes)
                    val tag = String(tagBytes, StandardCharsets.US_ASCII)
                    if (tag == "arab" || tag == "DFLT") {
                        if (tag == "arab") hasArabic = true
                    }
                }
            }

            // 2. FeatureList
            if (featureListRel > 0 && featureListRel + 2 <= tableLength) {
                val featListAbs = tableOffset + featureListRel
                val featCount = (buf.getShort(featListAbs).toInt() and 0xFFFF).coerceAtMost(512)
                for (i in 0 until featCount) {
                    val rec = featListAbs + 2 + i * 6
                    if (rec + 6 > tableOffset + tableLength) break
                    val tagBytes = ByteArray(4)
                    buf.position(rec)
                    buf.get(tagBytes)
                    val tag = String(tagBytes, StandardCharsets.US_ASCII)
                    features.add(tag)
                }
            }

            // 3. LookupList
            if (lookupListRel > 0 && lookupListRel + 2 <= tableLength) {
                val lookupListAbs = tableOffset + lookupListRel
                val lookupCount = (buf.getShort(lookupListAbs).toInt() and 0xFFFF).coerceAtMost(512)
                for (i in 0 until lookupCount) {
                    val offPos = lookupListAbs + 2 + i * 2
                    if (offPos + 2 > tableOffset + tableLength) break
                    val lookupRel = buf.getShort(offPos).toInt() and 0xFFFF
                    val lookupAbs = lookupListAbs + lookupRel
                    if (lookupAbs + 6 > tableOffset + tableLength) continue
                    val lookupType = buf.getShort(lookupAbs).toInt() and 0xFFFF
                    val subTableCount = (buf.getShort(lookupAbs + 4).toInt() and 0xFFFF).coerceAtMost(64)
                    if (isGpos && lookupType == 9 && subTableCount > 0 && lookupAbs + 8 <= tableOffset + tableLength) {
                        // Extension Positioning (Lookup Type 9 in GPOS wraps the actual lookup type at subtable + 2)
                        val firstSubRel = buf.getShort(lookupAbs + 6).toInt() and 0xFFFF
                        val extSubAbs = lookupAbs + firstSubRel
                        if (extSubAbs + 4 <= tableOffset + tableLength) {
                            val extLookupType = buf.getShort(extSubAbs + 2).toInt() and 0xFFFF
                            lookupTypes.add(extLookupType)
                        }
                    } else {
                        lookupTypes.add(lookupType)
                    }
                }
            }
        } catch (_: Exception) {
        }

        return LayoutTableInfo(
            hasArabicScript = hasArabic,
            featureTags = features,
            lookupTypes = lookupTypes
        )
    }
}
