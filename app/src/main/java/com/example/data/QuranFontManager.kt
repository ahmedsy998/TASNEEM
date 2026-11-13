package com.example.data

import android.content.Context
import android.database.Cursor
import android.graphics.Paint
import android.graphics.Typeface
import android.net.Uri
import android.provider.OpenableColumns
import android.text.SpannableString
import android.text.Spanned
import android.text.TextPaint
import android.text.style.MetricAffectingSpan
import androidx.compose.ui.text.font.FontFamily
import com.example.media.OpenTypeFontInspector
import com.example.media.QuranTextAndLayoutEngine
import com.example.model.ArabicQuranFont
import com.example.model.CustomImportedFont
import com.example.model.FontFileFormat
import com.example.model.FontQuranCompatibilityLevel
import com.example.model.QuranFontValidationReport
import com.example.model.QuranTextStyleConfig
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.security.MessageDigest
import java.util.concurrent.ConcurrentHashMap

/**
 * Custom MetricAffectingSpan that applies a fallback Typeface to an entire word or standalone symbol
 * without ever splitting a base Arabic letter from its combining Tashkeel marks.
 */
class QuranFallbackTypefaceSpan(
    private val fallbackTypeface: Typeface,
    private val isBold: Boolean = false
) : MetricAffectingSpan() {
    override fun updateDrawState(ds: TextPaint) {
        applyTypeface(ds)
    }

    override fun updateMeasureState(paint: TextPaint) {
        applyTypeface(paint)
    }

    private fun applyTypeface(paint: Paint) {
        paint.typeface = fallbackTypeface
        if (isBold) {
            paint.isFakeBoldText = true
        }
    }
}

data class QuranFontImportResult(
    val importedFont: CustomImportedFont,
    val messageAr: String,
    val warningAr: String?
)

data class ResolvedQuranPhraseTypography(
    val primaryTypeface: Typeface,
    val fallbackTypeface: Typeface,
    val effectiveBaseTypeface: Typeface,
    val spannable: SpannableString,
    val usedFullFallbackFont: Boolean,
    val fallbackWordCount: Int
)

/**
 * Central Arabic & Quranic Font Manager (Sections 2, 3, 4, 5, 6, 7, 8, 9, 10).
 *
 * Manages:
 * - All 15 permanently bundled Arabic/Quranic fonts.
 * - Phone-imported custom fonts (.TTF and .OTF) stored in private app storage (filesDir/imported_fonts).
 * - OpenType binary inspection & 10-sample vocalized Quranic validation caching.
 * - Intelligent Quranic font fallback ("استخدام خط احتياطي للتشكيل القرآني").
 * - Persistent storage of imported fonts and user font preferences across app restarts.
 */
class QuranFontManager(private val context: Context) {

    private val appContext = context.applicationContext
    private val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val importedFontsDir = File(appContext.filesDir, "imported_fonts").apply { mkdirs() }
    private val registryFile = File(importedFontsDir, "imported_fonts_registry.json")

    init {
        loadPersistedImportedFontsIntoCache()
    }

    /**
     * Returns all currently available user-imported fonts, sorted newest first.
     */
    fun getImportedFonts(): List<CustomImportedFont> {
        loadPersistedImportedFontsIntoCache()
        return importedFontsMap.values.sortedByDescending { it.importedAtEpochMs }
    }

    /**
     * Imports a custom .ttf or .otf font from an Android SAF Uri (ACTION_OPEN_DOCUMENT).
     */
    fun importFontFromUri(uri: Uri): Result<QuranFontImportResult> {
        return try {
            val originalName = resolveDisplayNameFromUri(uri) ?: "CustomArabicFont.ttf"
            val bytes = appContext.contentResolver.openInputStream(uri)?.use { stream ->
                val buffer = stream.readBytes()
                if (buffer.size > MAX_FONT_FILE_BYTES) {
                    return Result.failure(
                        IllegalArgumentException("حجم ملف الخط كبير جدًا (الحد الأقصى ٢٥ ميجابايت).")
                    )
                }
                buffer
            } ?: return Result.failure(
                IllegalArgumentException("تعذر قراءة ملف الخط المختار من الهاتف.")
            )

            importFontBytesInternal(bytes, originalName)
        } catch (e: Exception) {
            Result.failure(
                IllegalArgumentException(
                    "تعذر استيراد ملف الخط: ${e.localizedMessage ?: "ملف غير صالح"}"
                )
            )
        }
    }

    /**
     * Imports a custom .ttf or .otf font directly from a File (used by file pickers and automated tests).
     */
    fun importFontFromFile(sourceFile: File, originalFileNameOverride: String? = null): Result<QuranFontImportResult> {
        return try {
            if (!sourceFile.exists() || !sourceFile.isFile) {
                return Result.failure(IllegalArgumentException("ملف الخط غير موجود."))
            }
            if (sourceFile.length() > MAX_FONT_FILE_BYTES || sourceFile.length() < 32L) {
                return Result.failure(IllegalArgumentException("حجم ملف الخط غير صالح."))
            }
            val bytes = sourceFile.readBytes()
            val fileName = originalFileNameOverride ?: sourceFile.name
            importFontBytesInternal(bytes, fileName)
        } catch (e: Exception) {
            Result.failure(
                IllegalArgumentException("تعذر قراءة ملف الخط: ${e.localizedMessage ?: "خطأ غير معروف"}")
            )
        }
    }

    private fun importFontBytesInternal(
        bytes: ByteArray,
        originalFileName: String
    ): Result<QuranFontImportResult> {
        val format = OpenTypeFontInspector.detectFontFormat(bytes)
        if (format == FontFileFormat.UNKNOWN) {
            return Result.failure(
                IllegalArgumentException(
                    "صيغة الملف غير مدعومة. يرجى اختيار ملف خط بصيغة TTF أو OTF صالحة."
                )
            )
        }

        val sha256 = computeSha256(bytes)

        // Check if this exact font binary was already imported
        val existing = importedFontsMap.values.find { it.sha256Hash == sha256 && File(it.localFilePath).exists() }
        if (existing != null) {
            return Result.success(
                QuranFontImportResult(
                    importedFont = existing,
                    messageAr = "الخط «${existing.displayName}» مضاف مسبقًا في قائمة الخطوط المستوردة.",
                    warningAr = existing.validationReport.warningMessageAr
                )
            )
        }

        val fontId = "imported_${System.currentTimeMillis()}_${sha256.take(8)}"
        val targetFile = File(importedFontsDir, "$fontId.${format.extension}")
        targetFile.writeBytes(bytes)

        // Verify Android Typeface loader can actually load this font file
        val loadedTypeface = try {
            Typeface.createFromFile(targetFile)
        } catch (_: Exception) {
            null
        }
        if (loadedTypeface == null || loadedTypeface == Typeface.DEFAULT) {
            targetFile.delete()
            return Result.failure(
                IllegalArgumentException("تعذر تحميل محرك الخط (Typeface) من هذا الملف؛ قد يكون الملف تالفًا.")
            )
        }

        // Run binary OpenType + 10-sample Quranic validation
        val parsed = OpenTypeFontInspector.inspectFontBytes(bytes, originalFileName)
        val validationReport = OpenTypeFontInspector.validateFontFile(
            fontFile = targetFile,
            fontId = fontId,
            displayNameOverride = parsed.report.internalFamilyName.ifBlank {
                originalFileName.substringBeforeLast('.')
            },
            loadedTypeface = loadedTypeface
        )

        if (!validationReport.isUsableInGallery ||
            validationReport.compatibilityLevel == FontQuranCompatibilityLevel.INCOMPATIBLE_NO_ARABIC
        ) {
            targetFile.delete()
            return Result.failure(
                IllegalArgumentException(
                    validationReport.warningMessageAr
                        ?: "هذا الخط لا يدعم الحروف العربية الأساسية ولا يمكن إضافته."
                )
            )
        }

        val importedFont = CustomImportedFont(
            id = fontId,
            displayName = validationReport.fontDisplayName,
            originalFileName = originalFileName,
            localFilePath = targetFile.absolutePath,
            format = format,
            fileSizeBytes = targetFile.length(),
            sha256Hash = sha256,
            importedAtEpochMs = System.currentTimeMillis(),
            validationReport = validationReport,
            badgeLabelAr = "خط مستورد"
        )

        importedFontsMap[fontId] = importedFont
        validationReportCache[fontId] = validationReport
        supportedCodepointsCache[fontId] = parsed.supportedCodepoints
        customTypefaceCache["${fontId}_false"] = loadedTypeface
        customTypefaceCache["${fontId}_true"] = Typeface.create(loadedTypeface, Typeface.BOLD)

        saveImportedFontsRegistryToDisk()

        val successMessage = when (validationReport.compatibilityLevel) {
            FontQuranCompatibilityLevel.FULLY_VERIFIED_QURANIC ->
                "تم استيراد وفحص الخط «${importedFont.displayName}» (${format.extension.uppercase()}) بنجاح — متوافق كليًا مع التشكيل القرآني."
            FontQuranCompatibilityLevel.REQUIRES_VISUAL_VERIFICATION_OR_FALLBACK ->
                "تم استيراد الخط «${importedFont.displayName}» (${format.extension.uppercase()}) مع تفعيل الخط الاحتياطي للرموز المصحفية."
            FontQuranCompatibilityLevel.PARTIAL_ARABIC_INCOMPLETE_TASHKEEL ->
                "تم استيراد الخط العربي «${importedFont.displayName}» مع تنبيه: الخط غير مكتمل التشكيل القرآني وتم تفعيل الخط الاحتياطي للتشكيل."
            FontQuranCompatibilityLevel.INCOMPATIBLE_NO_ARABIC ->
                "غير متوافق."
        }

        return Result.success(
            QuranFontImportResult(
                importedFont = importedFont,
                messageAr = successMessage,
                warningAr = validationReport.warningMessageAr
            )
        )
    }

    /**
     * Removes a user-imported font from storage and memory.
     * Built-in fonts can NEVER be deleted (Section 7).
     * Returns true if deleted, false if fontId is built-in or not found.
     */
    fun removeImportedFont(fontId: String): Boolean {
        // Built-in fonts are protected and cannot be removed
        if (ArabicQuranFont.entries.any { it.id == fontId } || !fontId.startsWith("imported_")) {
            return false
        }
        val existing = importedFontsMap.remove(fontId) ?: return false
        try {
            val file = File(existing.localFilePath)
            if (file.exists()) file.delete()
        } catch (_: Exception) {
        }
        customTypefaceCache.remove("${fontId}_false")
        customTypefaceCache.remove("${fontId}_true")
        composeFontFamilyCache.remove(fontId)
        validationReportCache.remove(fontId)
        supportedCodepointsCache.remove(fontId)

        saveImportedFontsRegistryToDisk()

        // If the deleted font was persisted as the active selection, switch to default built-in Quranic font
        if (getPersistedSelectedFontId() == fontId) {
            saveSelectedFontPreference(
                selectedFontId = ArabicQuranFont.KFGQPC_UTHMANIC.id,
                enableFallback = true,
                fallbackFont = ArabicQuranFont.KFGQPC_UTHMANIC
            )
        }
        return true
    }

    /**
     * Returns the validation report for any built-in or imported font.
     */
    fun getValidationReportForFont(textStyle: QuranTextStyleConfig): QuranFontValidationReport {
        if (textStyle.isCustomImportedFontSelected) {
            val id = textStyle.selectedFontId
            validationReportCache[id]?.let { return it }
            importedFontsMap[id]?.let {
                validationReportCache[id] = it.validationReport
                return it.validationReport
            }
            val customFile = textStyle.customFontFilePath?.let { File(it) }
            if (customFile != null && customFile.exists()) {
                val report = OpenTypeFontInspector.validateFontFile(
                    fontFile = customFile,
                    fontId = id,
                    displayNameOverride = textStyle.customFontDisplayName
                )
                validationReportCache[id] = report
                return report
            }
        }
        return getBuiltInFontValidationReport(textStyle.font)
    }

    fun getBuiltInFontValidationReport(font: ArabicQuranFont): QuranFontValidationReport {
        validationReportCache[font.id]?.let { return it }
        val file = font.resolveBundledFontFile(appContext)
        val report = if (file != null && file.exists()) {
            val parsed = OpenTypeFontInspector.inspectFontBytes(file.readBytes(), font.resourceFileName)
            supportedCodepointsCache[font.id] = parsed.supportedCodepoints
            OpenTypeFontInspector.validateFontFile(
                fontFile = file,
                fontId = font.id,
                displayNameOverride = font.displayNameAr,
                loadedTypeface = font.loadTypeface(appContext, bold = false)
            )
        } else {
            // Fallback report if raw file stream wasn't extracted
            OpenTypeFontInspector.validateFontFile(
                fontFile = File(""),
                fontId = font.id,
                displayNameOverride = font.displayNameAr
            )
        }
        validationReportCache[font.id] = report
        return report
    }

    /**
     * Persistence of selected font & fallback preferences across app restarts (Section 7).
     */
    fun saveSelectedFontPreference(
        selectedFontId: String,
        enableFallback: Boolean,
        fallbackFont: ArabicQuranFont = ArabicQuranFont.KFGQPC_UTHMANIC
    ) {
        prefs.edit()
            .putString(KEY_SELECTED_FONT_ID, selectedFontId)
            .putBoolean(KEY_ENABLE_FALLBACK, enableFallback)
            .putString(KEY_FALLBACK_FONT_ID, fallbackFont.id)
            .apply()
    }

    fun getPersistedSelectedFontId(): String {
        return prefs.getString(KEY_SELECTED_FONT_ID, ArabicQuranFont.KFGQPC_UTHMANIC.id)
            ?: ArabicQuranFont.KFGQPC_UTHMANIC.id
    }

    fun getPersistedFallbackEnabled(): Boolean {
        return prefs.getBoolean(KEY_ENABLE_FALLBACK, true)
    }

    fun restorePersistedTextStyle(baseStyle: QuranTextStyleConfig = QuranTextStyleConfig()): QuranTextStyleConfig {
        loadPersistedImportedFontsIntoCache()
        val savedFontId = getPersistedSelectedFontId()
        val fallbackEnabled = getPersistedFallbackEnabled()
        val fallbackFontId = prefs.getString(KEY_FALLBACK_FONT_ID, ArabicQuranFont.KFGQPC_UTHMANIC.id)
            ?: ArabicQuranFont.KFGQPC_UTHMANIC.id
        val fallbackFont = ArabicQuranFont.fromIdOrDefault(fallbackFontId)

        if (savedFontId.startsWith("imported_")) {
            val imported = importedFontsMap[savedFontId]
            if (imported != null && File(imported.localFilePath).exists()) {
                return baseStyle.copy(
                    font = ArabicQuranFont.KFGQPC_UTHMANIC,
                    selectedFontId = imported.id,
                    customFontFilePath = imported.localFilePath,
                    customFontDisplayName = imported.displayName,
                    enableQuranicFontFallback = fallbackEnabled,
                    fallbackFont = fallbackFont
                )
            }
        }

        val builtIn = ArabicQuranFont.fromIdOrDefault(savedFontId)
        return baseStyle.copy(
            font = builtIn,
            selectedFontId = builtIn.id,
            customFontFilePath = null,
            customFontDisplayName = null,
            enableQuranicFontFallback = fallbackEnabled,
            fallbackFont = fallbackFont
        )
    }

    private fun resolveDisplayNameFromUri(uri: Uri): String? {
        var name: String? = null
        val cursor: Cursor? = try {
            appContext.contentResolver.query(uri, null, null, null, null)
        } catch (_: Exception) {
            null
        }
        cursor?.use { c ->
            if (c.moveToFirst()) {
                val idx = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (idx >= 0) {
                    name = c.getString(idx)
                }
            }
        }
        return name ?: uri.lastPathSegment
    }

    private fun loadPersistedImportedFontsIntoCache() {
        if (!registryFile.exists()) return
        try {
            val rawJson = registryFile.readText()
            if (rawJson.isBlank()) return
            val array = JSONArray(rawJson)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val id = obj.optString("id")
                val path = obj.optString("localFilePath")
                val file = File(path)
                if (id.isNotBlank() && file.exists() && file.length() > 32L) {
                    if (!importedFontsMap.containsKey(id)) {
                        val displayName = obj.optString("displayName", file.nameWithoutExtension)
                        val originalName = obj.optString("originalFileName", file.name)
                        val formatStr = obj.optString("format", "TTF")
                        val format = runCatching { FontFileFormat.valueOf(formatStr) }.getOrDefault(FontFileFormat.TTF)
                        val sha256 = obj.optString("sha256Hash", "")
                        val importedAt = obj.optLong("importedAtEpochMs", file.lastModified())

                        val bytes = file.readBytes()
                        val parsed = OpenTypeFontInspector.inspectFontBytes(bytes, originalName)
                        supportedCodepointsCache[id] = parsed.supportedCodepoints
                        val tf = runCatching { Typeface.createFromFile(file) }.getOrNull()
                        if (tf != null) {
                            customTypefaceCache["${id}_false"] = tf
                            customTypefaceCache["${id}_true"] = Typeface.create(tf, Typeface.BOLD)
                        }
                        val report = OpenTypeFontInspector.validateFontFile(
                            fontFile = file,
                            fontId = id,
                            displayNameOverride = displayName,
                            loadedTypeface = tf
                        )
                        validationReportCache[id] = report
                        importedFontsMap[id] = CustomImportedFont(
                            id = id,
                            displayName = displayName,
                            originalFileName = originalName,
                            localFilePath = file.absolutePath,
                            format = format,
                            fileSizeBytes = file.length(),
                            sha256Hash = sha256,
                            importedAtEpochMs = importedAt,
                            validationReport = report
                        )
                    }
                }
            }
        } catch (_: Exception) {
        }
    }

    private fun saveImportedFontsRegistryToDisk() {
        try {
            val array = JSONArray()
            importedFontsMap.values.sortedByDescending { it.importedAtEpochMs }.forEach { font ->
                val obj = JSONObject().apply {
                    put("id", font.id)
                    put("displayName", font.displayName)
                    put("originalFileName", font.originalFileName)
                    put("localFilePath", font.localFilePath)
                    put("format", font.format.name)
                    put("fileSizeBytes", font.fileSizeBytes)
                    put("sha256Hash", font.sha256Hash)
                    put("importedAtEpochMs", font.importedAtEpochMs)
                }
                array.put(obj)
            }
            registryFile.writeText(array.toString(2))
        } catch (_: Exception) {
        }
    }

    companion object {
        private const val PREFS_NAME = "tasneem_font_prefs"
        private const val KEY_SELECTED_FONT_ID = "selected_font_id"
        private const val KEY_ENABLE_FALLBACK = "enable_quranic_fallback"
        private const val KEY_FALLBACK_FONT_ID = "fallback_font_id"
        private const val MAX_FONT_FILE_BYTES = 25 * 1024 * 1024 // 25 MB

        private val importedFontsMap = ConcurrentHashMap<String, CustomImportedFont>()
        private val customTypefaceCache = ConcurrentHashMap<String, Typeface>()
        private val composeFontFamilyCache = ConcurrentHashMap<String, FontFamily>()
        private val validationReportCache = ConcurrentHashMap<String, QuranFontValidationReport>()
        private val supportedCodepointsCache = ConcurrentHashMap<String, Set<Int>>()

        fun computeSha256(bytes: ByteArray): String {
            val digest = MessageDigest.getInstance("SHA-256").digest(bytes)
            return digest.joinToString("") { "%02x".format(it) }
        }

        /**
         * Resolves the Android Typeface for the currently selected font (built-in or custom imported).
         * Uses thread-safe in-memory caching so Live Preview and MP4 Export never recreate Typefaces per frame.
         */
        fun resolvePrimaryTypeface(context: Context, textConfig: QuranTextStyleConfig): Typeface {
            if (textConfig.isCustomImportedFontSelected) {
                val fontId = textConfig.selectedFontId
                val cacheKey = "${fontId}_${textConfig.isBold}"
                customTypefaceCache[cacheKey]?.let { return it }

                val path = textConfig.customFontFilePath ?: importedFontsMap[fontId]?.localFilePath
                if (!path.isNullOrBlank()) {
                    val file = File(path)
                    if (file.exists() && file.length() > 32L) {
                        return try {
                            val base = customTypefaceCache["${fontId}_false"]
                                ?: Typeface.createFromFile(file).also {
                                    customTypefaceCache["${fontId}_false"] = it
                                }
                            val resolved = if (textConfig.isBold) {
                                Typeface.create(base, Typeface.BOLD)
                            } else {
                                base
                            }
                            customTypefaceCache[cacheKey] = resolved
                            resolved
                        } catch (_: Exception) {
                            textConfig.fallbackFont.loadTypeface(context, textConfig.isBold)
                        }
                    }
                }
            }
            return textConfig.font.loadTypeface(context, textConfig.isBold)
        }

        /**
         * Resolves the Jetpack Compose FontFamily for any built-in or imported font for UI previews.
         */
        fun resolveComposeFontFamily(context: Context, textConfig: QuranTextStyleConfig): FontFamily {
            if (textConfig.isCustomImportedFontSelected) {
                val fontId = textConfig.selectedFontId
                composeFontFamilyCache[fontId]?.let { return it }
                val tf = resolvePrimaryTypeface(context, textConfig.copy(isBold = false))
                val family = FontFamily(tf)
                composeFontFamilyCache[fontId] = family
                return family
            }
            return textConfig.font.composeFontFamily
        }

        fun resolveComposeFontFamilyForImported(importedFont: CustomImportedFont): FontFamily {
            composeFontFamilyCache[importedFont.id]?.let { return it }
            val tf = customTypefaceCache["${importedFont.id}_false"] ?: try {
                Typeface.createFromFile(File(importedFont.localFilePath)).also {
                    customTypefaceCache["${importedFont.id}_false"] = it
                }
            } catch (_: Exception) {
                Typeface.DEFAULT
            }
            val family = FontFamily(tf)
            composeFontFamilyCache[importedFont.id] = family
            return family
        }

        /**
         * Ensures the supported codepoint set and validation report for the given font ID are loaded in cache.
         */
        private fun ensureFontInspectionCached(
            context: Context,
            fontId: String,
            customFilePath: String?,
            builtInFont: ArabicQuranFont
        ): Pair<QuranFontValidationReport?, Set<Int>> {
            val existingReport = validationReportCache[fontId]
            val existingCodepoints = supportedCodepointsCache[fontId]
            if (existingReport != null && existingCodepoints != null) {
                return Pair(existingReport, existingCodepoints)
            }

            val file = if (fontId.startsWith("imported_") && !customFilePath.isNullOrBlank()) {
                File(customFilePath)
            } else {
                builtInFont.resolveBundledFontFile(context)
            }

            if (file != null && file.exists() && file.length() > 32L) {
                return try {
                    val bytes = file.readBytes()
                    val parsed = OpenTypeFontInspector.inspectFontBytes(bytes, file.name)
                    val report = existingReport ?: OpenTypeFontInspector.validateFontFile(
                        fontFile = file,
                        fontId = fontId
                    )
                    validationReportCache[fontId] = report
                    supportedCodepointsCache[fontId] = parsed.supportedCodepoints
                    Pair(report, parsed.supportedCodepoints)
                } catch (_: Exception) {
                    Pair(existingReport, existingCodepoints ?: emptySet())
                }
            }
            return Pair(existingReport, existingCodepoints ?: emptySet())
        }

        /**
         * Intelligent Quranic Font Fallback & Independent Tashkeel Coloring Engine (Sections 4, 6, 8, 9).
         *
         * Builds a SpannableString for [phraseText] that:
         * 1. Never modifies the original Quranic Unicode text.
         * 2. Colors base Arabic letters with [mainColorInt] and combining Tashkeel marks with [tashkeelColorInt]
         *    when [textConfig.enableIndependentTashkeelColor] is enabled.
         * 3. When [textConfig.enableQuranicFontFallback] is enabled:
         *    - If the selected font cannot guarantee proper mark-to-base / mark-to-mark attachment for
         *      vocalized Quranic words (e.g. PARTIAL_ARABIC_INCOMPLETE_TASHKEEL), switches the base
         *      Typeface for the vocalized Quranic phrase to the verified Quranic fallback font
         *      (KFGQPC Uthmanic or Amiri Quran) so diacritics are never detached, overlapping, or missing.
         *    - If the selected font supports Arabic letters + core Tashkeel + GPOS mark attachment,
         *      preserves the user's selected font for all supported words, and applies a word-safe
         *      [QuranFallbackTypefaceSpan] ONLY to standalone symbols or words containing unsupported
         *      Uthmani codepoints (never splitting a combining mark from its base letter!).
         */
        fun resolvePhraseTypographyWithFallback(
            context: Context,
            phraseText: String,
            textConfig: QuranTextStyleConfig,
            mainColorInt: Int,
            tashkeelColorInt: Int
        ): ResolvedQuranPhraseTypography {
            val primaryTypeface = resolvePrimaryTypeface(context, textConfig)
            val fallbackTypeface = textConfig.fallbackFont.loadTypeface(context, textConfig.isBold)

            val spannable = QuranTextAndLayoutEngine.buildQuranSpannable(
                text = phraseText,
                mainColorInt = mainColorInt,
                enableIndependentTashkeel = textConfig.enableIndependentTashkeelColor,
                tashkeelColorInt = tashkeelColorInt
            )

            if (!textConfig.enableQuranicFontFallback || phraseText.isEmpty()) {
                return ResolvedQuranPhraseTypography(
                    primaryTypeface = primaryTypeface,
                    fallbackTypeface = fallbackTypeface,
                    effectiveBaseTypeface = primaryTypeface,
                    spannable = spannable,
                    usedFullFallbackFont = false,
                    fallbackWordCount = 0
                )
            }

            val (report, supportedCodepoints) = ensureFontInspectionCached(
                context = context,
                fontId = textConfig.selectedFontId,
                customFilePath = textConfig.customFontFilePath,
                builtInFont = textConfig.font
            )

            val hasDiacriticsInPhrase = phraseText.any { QuranTextAndLayoutEngine.isQuranicDiacritic(it) }

            // If the selected font cannot guarantee mark attachment on vocalized Quranic text,
            // use the complete compatible Quranic fallback font rather than producing broken Tashkeel (Section 6).
            if (hasDiacriticsInPhrase && report != null && !report.canGuaranteeMixedMarkAttachment) {
                return ResolvedQuranPhraseTypography(
                    primaryTypeface = primaryTypeface,
                    fallbackTypeface = fallbackTypeface,
                    effectiveBaseTypeface = fallbackTypeface,
                    spannable = spannable,
                    usedFullFallbackFont = true,
                    fallbackWordCount = phraseText.split(Regex("\\s+")).size
                )
            }

            // Otherwise, preserve the user's selected font for supported words and apply word-level
            // fallback spans only on tokens that contain codepoints missing from the selected font's cmap.
            var fallbackWordCount = 0
            if (supportedCodepoints.isNotEmpty()) {
                var idx = 0
                while (idx < phraseText.length) {
                    while (idx < phraseText.length && phraseText[idx].isWhitespace()) {
                        idx++
                    }
                    if (idx >= phraseText.length) break
                    val wordStart = idx
                    while (idx < phraseText.length && !phraseText[idx].isWhitespace()) {
                        idx++
                    }
                    val wordEnd = idx

                    var wordNeedsFallback = false
                    var charPos = wordStart
                    while (charPos < wordEnd) {
                        val cp = Character.codePointAt(phraseText, charPos)
                        // Ornate parentheses ﴿ (FD3E) and ﴾ (FD3F) or Uthmani marks not in cmap
                        if (cp !in supportedCodepoints) {
                            wordNeedsFallback = true
                            break
                        }
                        charPos += Character.charCount(cp)
                    }

                    if (wordNeedsFallback && primaryTypeface != fallbackTypeface) {
                        spannable.setSpan(
                            QuranFallbackTypefaceSpan(fallbackTypeface, textConfig.isBold),
                            wordStart,
                            wordEnd,
                            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                        )
                        fallbackWordCount++
                    }
                }
            }

            return ResolvedQuranPhraseTypography(
                primaryTypeface = primaryTypeface,
                fallbackTypeface = fallbackTypeface,
                effectiveBaseTypeface = primaryTypeface,
                spannable = spannable,
                usedFullFallbackFont = false,
                fallbackWordCount = fallbackWordCount
            )
        }
    }
}
