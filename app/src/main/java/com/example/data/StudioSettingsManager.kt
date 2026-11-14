package com.example.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.model.ArabicQuranFont
import com.example.model.DEFAULT_PERSONAL_SIGNATURE
import com.example.model.HeaderElementConfig
import com.example.model.MediaCropMode
import com.example.model.MushafBasmalaRuleMode
import com.example.model.MushafEditionConfig
import com.example.model.PhoneMediaBackground
import com.example.model.QuranTextAlignment
import com.example.model.QuranTextStyleConfig
import com.example.model.RecitationHeaderConfig
import com.example.model.SocialPlatform
import com.example.model.SurahInfo
import com.example.model.TextColorOption
import com.example.model.TransitionEffectType
import com.example.model.TransitionSpeedPreset
import com.example.model.VerseTransitionConfig
import com.example.model.VideoAspectRatio
import com.example.model.VideoResolution
import com.example.model.WatermarkConfig
import com.example.viewmodel.StudioUiState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

private val Context.studioSettingsDataStore by preferencesDataStore(name = "tasneem_studio_settings_datastore")

/**
 * Persistent Studio Settings Manager (Requirement 1, 2, 3, 4).
 *
 * Permanently saves and restores all user-customized studio settings across application
 * closure, ViewModel recreation, and device restarts using Jetpack DataStore + atomic
 * local configuration storage:
 * - Selected Quran reciter (defaults to Fares Abbad "فارس عباد" ONLY when no previous preference exists)
 * - Selected Surah, start verse, end verse, and Madinah Mushaf settings
 * - Selected Quranic font (built-in or imported font), fallback settings, font size, line spacing, alignment, bold, single-line mode, tafsir, translation
 * - Main text color and independent Tashkeel color
 * - Quranic text position and auto-positioning state
 * - Independent recitation header settings (Surah name, Verse number, Reciter name)
 * - Background image or video selection, crop mode, scale, offset, brightness, dim level, loop setting
 * - Personal signature (defaults to "rattil.official.2026" ONLY when no previous user signature exists) and watermark settings
 * - Social media icon selections, size, and vertical offset
 * - Video aspect ratio and resolution
 * - Automatic internal Fade In / Fade Out transition
 */
class StudioSettingsManager(
    private val context: Context,
    private val reciterRepository: ReciterRepository = ReciterRepository(),
    private val quranFontManager: QuranFontManager = QuranFontManager(context)
) {

    private val appContext = context.applicationContext
    private val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val settingsFile = File(appContext.filesDir, SETTINGS_FILE_NAME)
    private val ioScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    companion object {
        private const val PREFS_NAME = "tasneem_persistent_studio_settings_v1"
        private const val KEY_SETTINGS_JSON = "studio_settings_json"
        private const val SETTINGS_FILE_NAME = "tasneem_studio_settings_v1.json"
        private val DATASTORE_JSON_KEY = stringPreferencesKey("studio_settings_json")
    }

    val settingsJsonFlow: Flow<String?> = appContext.studioSettingsDataStore.data.map { ds ->
        ds[DATASTORE_JSON_KEY]
    }

    /**
     * Saves all customizable settings from [state] immediately and atomically.
     */
    fun saveStudioSettings(state: StudioUiState) {
        val jsonString = serializeStateToJson(state).toString()

        // 1. Synchronous SharedPreferences commit for immediate consistency across restarts
        prefs.edit().putString(KEY_SETTINGS_JSON, jsonString).commit()

        // 2. Synchronous atomic file backup in internal app storage
        try {
            settingsFile.writeText(jsonString, Charsets.UTF_8)
        } catch (_: Exception) {
        }

        // 3. Sync font preference with QuranFontManager
        quranFontManager.saveSelectedFontPreference(
            selectedFontId = state.textStyle.selectedFontId,
            enableFallback = state.textStyle.enableQuranicFontFallback,
            fallbackFont = state.textStyle.fallbackFont
        )

        // 4. Persist to Jetpack DataStore<Preferences>
        ioScope.launch {
            try {
                appContext.studioSettingsDataStore.edit { ds ->
                    ds[DATASTORE_JSON_KEY] = jsonString
                }
            } catch (_: Exception) {
            }
        }
    }

    /**
     * Restores the complete [StudioUiState] on startup or recreation.
     * Applies initial defaults (Fares Abbad reciter, "rattil.official.2026" signature, Fade In / Out)
     * ONLY when no previous user-saved preference exists.
     */
    fun restoreStudioUiState(): StudioUiState {
        val importedFonts = quranFontManager.getImportedFonts()
        val baseFontRestoredStyle = quranFontManager.restorePersistedTextStyle(QuranTextStyleConfig())
        val defaultState = StudioUiState(
            selectedReciter = reciterRepository.defaultReciter, // فارس عباد
            textStyle = baseFontRestoredStyle,
            importedFonts = importedFonts,
            verseTransition = VerseTransitionConfig(
                effectType = TransitionEffectType.FADE_IN_OUT,
                speedPreset = TransitionSpeedPreset.NORMAL,
                durationMs = 360L
            ),
            watermark = WatermarkConfig(
                signatureText = DEFAULT_PERSONAL_SIGNATURE // rattil.official.2026
            )
        )

        val rawJson = prefs.getString(KEY_SETTINGS_JSON, null)
            ?.takeIf { it.isNotBlank() }
            ?: try {
                if (settingsFile.exists() && settingsFile.length() > 2L) {
                    settingsFile.readText(Charsets.UTF_8)
                } else null
            } catch (_: Exception) {
                null
            }

        if (rawJson.isNullOrBlank()) {
            val activeValidation = quranFontManager.getValidationReportForFont(defaultState.textStyle)
            return defaultState.copy(activeFontValidationReport = activeValidation)
        }

        return try {
            val root = JSONObject(rawJson)
            deserializeStateFromJson(root, defaultState, importedFonts)
        } catch (_: Exception) {
            val activeValidation = quranFontManager.getValidationReportForFont(defaultState.textStyle)
            defaultState.copy(activeFontValidationReport = activeValidation)
        }
    }

    /**
     * Clears persisted studio settings (used only in isolated tests when verifying fresh-install defaults).
     */
    fun clearAllSavedSettingsForTesting() {
        prefs.edit().clear().commit()
        try {
            if (settingsFile.exists()) settingsFile.delete()
        } catch (_: Exception) {
        }
    }

    private fun serializeStateToJson(state: StudioUiState): JSONObject {
        val root = JSONObject()

        // 1. Reciter
        root.put("selectedReciterId", state.selectedReciter.id)

        // 2. Surah & Verse Range
        root.put(
            "surah",
            JSONObject().apply {
                put("number", state.selectedSurah.number)
                put("nameAr", state.selectedSurah.nameAr)
                put("verseCount", state.selectedSurah.verseCount)
                put("revelationTypeAr", state.selectedSurah.revelationTypeAr)
                put("startVerse", state.startVerse)
                put("endVerse", state.endVerse)
            }
        )

        // 3. Mushaf Config
        root.put(
            "mushafConfig",
            JSONObject().apply {
                put("basmalaRuleModeId", state.mushafConfig.basmalaRuleMode.id)
                put("preserveUthmanicOrthography", state.mushafConfig.preserveUthmanicOrthography)
                put("showQuranicWaqfSymbols", state.mushafConfig.showQuranicWaqfSymbols)
            }
        )

        // 4. Quran Text Style & Font (including imported fonts & Tashkeel color)
        val ts = state.textStyle
        root.put(
            "textStyle",
            JSONObject().apply {
                put("fontId", ts.font.id)
                put("selectedFontId", ts.selectedFontId)
                put("customFontFilePath", ts.customFontFilePath ?: "")
                put("customFontDisplayName", ts.customFontDisplayName ?: "")
                put("enableQuranicFontFallback", ts.enableQuranicFontFallback)
                put("fallbackFontId", ts.fallbackFont.id)
                put("fontSizeSp", ts.fontSizeSp.toDouble())
                put("lineSpacingMultiplier", ts.lineSpacingMultiplier.toDouble())
                put("colorOptionId", ts.colorOption.id)
                put("mainTextColorHex", ts.mainTextColorHex)
                put("enableIndependentTashkeelColor", ts.enableIndependentTashkeelColor)
                put("tashkeelColorHex", ts.tashkeelColorHex)
                put("alignmentId", ts.alignment.id)
                put("isBold", ts.isBold)
                put("normalizedX", ts.normalizedX.toDouble())
                put("normalizedY", ts.normalizedY.toDouble())
                put("isAutoPositioned", ts.isAutoPositioned)
                put("singleLineDisplay", ts.singleLineDisplay)
                put("showSurahHeader", ts.showSurahHeader)
                put("showReciterName", ts.showReciterName)
                put("showTafsir", ts.showTafsir)
                put("showTranslation", ts.showTranslation)
                put("translationFontSizeScale", ts.translationFontSizeScale.toDouble())
            }
        )

        // 5. Recitation Info Header
        root.put(
            "recitationHeader",
            JSONObject().apply {
                put("surahName", serializeHeaderElement(state.recitationHeader.surahName))
                put("verseNumber", serializeHeaderElement(state.recitationHeader.verseNumber))
                put("reciterName", serializeHeaderElement(state.recitationHeader.reciterName))
            }
        )

        // 6. Background Media (Image / Video from phone or curated library)
        val pm = state.phoneMedia
        root.put(
            "phoneMedia",
            JSONObject().apply {
                put("uriString", pm.uriString ?: "")
                put("isVideo", pm.isVideo)
                put("fileName", pm.fileName)
                put("mimeType", pm.mimeType)
                put("durationMs", pm.durationMs)
                put("cropModeId", pm.cropMode.id)
                put("scale", pm.scale.toDouble())
                put("offsetX", pm.offsetX.toDouble())
                put("offsetY", pm.offsetY.toDouble())
                put("brightness", pm.brightness.toDouble())
                put("dimLevel", pm.dimLevel.toDouble())
                put("loopVideoInExport", pm.loopVideoInExport)
                put("curatedItemId", pm.curatedItemId ?: "")
            }
        )

        // 7. Personal Signature, Watermark & Social Media Icons
        val wm = state.watermark
        root.put(
            "watermark",
            JSONObject().apply {
                put("enabled", wm.enabled)
                put("signatureText", wm.signatureText)
                put("fontId", wm.font.id)
                put("colorHex", wm.colorHex)
                put("alignmentId", wm.alignment.id)
                put("normalizedX", wm.normalizedX.toDouble())
                put("normalizedY", wm.normalizedY.toDouble())
                put("fontSizeSp", wm.fontSizeSp.toDouble())
                put("scale", wm.scale.toDouble())
                put("opacity", wm.opacity.toDouble())
                put("marginDp", wm.marginDp.toDouble())
                put("subtleShadow", wm.subtleShadow)
                put("elegantGlow", wm.elegantGlow)
                put(
                    "selectedSocialPlatforms",
                    JSONArray().apply {
                        wm.selectedSocialPlatforms.forEach { put(it.id) }
                    }
                )
                put("socialIconsSizeDp", wm.socialIconsSizeDp.toDouble())
                put("socialIconsOffsetY", wm.socialIconsOffsetY.toDouble())
            }
        )

        // 8. Video Aspect Ratio & Resolution
        root.put("aspectRatioId", state.aspectRatio.id)
        root.put("resolutionId", state.resolution.id)

        return root
    }

    private fun deserializeStateFromJson(
        root: JSONObject,
        defaultState: StudioUiState,
        importedFonts: List<com.example.model.CustomImportedFont>
    ): StudioUiState {
        // 1. Reciter (default to Fares Abbad only if not previously saved)
        val savedReciterId = root.optString("selectedReciterId", "").takeIf { it.isNotBlank() }
        val resolvedReciter = if (savedReciterId != null) {
            reciterRepository.getReciterById(savedReciterId)
        } else {
            reciterRepository.defaultReciter
        }

        // 2. Surah & Verse Range
        val surahObj = root.optJSONObject("surah")
        val selectedSurah = if (surahObj != null) {
            SurahInfo(
                number = surahObj.optInt("number", defaultState.selectedSurah.number).coerceIn(1, 114),
                nameAr = surahObj.optString("nameAr", defaultState.selectedSurah.nameAr),
                verseCount = surahObj.optInt("verseCount", defaultState.selectedSurah.verseCount).coerceAtLeast(1),
                revelationTypeAr = surahObj.optString("revelationTypeAr", defaultState.selectedSurah.revelationTypeAr)
            )
        } else {
            defaultState.selectedSurah
        }
        val startVerse = (surahObj?.optInt("startVerse", defaultState.startVerse) ?: defaultState.startVerse)
            .coerceIn(1, selectedSurah.verseCount)
        val endVerse = (surahObj?.optInt("endVerse", defaultState.endVerse) ?: defaultState.endVerse)
            .coerceIn(startVerse, selectedSurah.verseCount)

        // 3. Mushaf Config
        val mushafObj = root.optJSONObject("mushafConfig")
        val mushafConfig = if (mushafObj != null) {
            val modeId = mushafObj.optString("basmalaRuleModeId", defaultState.mushafConfig.basmalaRuleMode.id)
            val mode = MushafBasmalaRuleMode.entries.find { it.id == modeId } ?: defaultState.mushafConfig.basmalaRuleMode
            defaultState.mushafConfig.copy(
                basmalaRuleMode = mode,
                preserveUthmanicOrthography = mushafObj.optBoolean(
                    "preserveUthmanicOrthography",
                    defaultState.mushafConfig.preserveUthmanicOrthography
                ),
                showQuranicWaqfSymbols = mushafObj.optBoolean(
                    "showQuranicWaqfSymbols",
                    defaultState.mushafConfig.showQuranicWaqfSymbols
                )
            )
        } else {
            defaultState.mushafConfig
        }

        // 4. Text Style & Font
        val tsObj = root.optJSONObject("textStyle")
        val rawTextStyle = if (tsObj != null) {
            val baseFont = ArabicQuranFont.fromIdOrDefault(tsObj.optString("fontId", defaultState.textStyle.font.id))
            val savedSelectedFontId = tsObj.optString("selectedFontId", baseFont.id)
            val fallbackFont = ArabicQuranFont.fromIdOrDefault(tsObj.optString("fallbackFontId", ArabicQuranFont.KFGQPC_UTHMANIC.id))
            val enableFallback = tsObj.optBoolean("enableQuranicFontFallback", true)

            // Resolve imported font if selectedFontId refers to an imported font that still exists on disk
            val matchedImported = if (savedSelectedFontId.startsWith("imported_")) {
                importedFonts.find { it.id == savedSelectedFontId && File(it.localFilePath).exists() }
            } else null

            val resolvedFontId: String
            val resolvedCustomPath: String?
            val resolvedCustomName: String?
            val resolvedBuiltInFont: ArabicQuranFont

            if (matchedImported != null) {
                resolvedBuiltInFont = baseFont
                resolvedFontId = matchedImported.id
                resolvedCustomPath = matchedImported.localFilePath
                resolvedCustomName = matchedImported.displayName
            } else if (savedSelectedFontId.startsWith("imported_")) {
                // Imported font was deleted; fall back to default built-in Quranic font
                resolvedBuiltInFont = ArabicQuranFont.KFGQPC_UTHMANIC
                resolvedFontId = ArabicQuranFont.KFGQPC_UTHMANIC.id
                resolvedCustomPath = null
                resolvedCustomName = null
            } else {
                resolvedBuiltInFont = ArabicQuranFont.fromIdOrDefault(savedSelectedFontId)
                resolvedFontId = resolvedBuiltInFont.id
                resolvedCustomPath = null
                resolvedCustomName = null
            }

            val colorOptId = tsObj.optString("colorOptionId", defaultState.textStyle.colorOption.id)
            val colorOption = TextColorOption.entries.find { it.id == colorOptId } ?: defaultState.textStyle.colorOption
            val alignId = tsObj.optString("alignmentId", defaultState.textStyle.alignment.id)
            val alignment = QuranTextAlignment.entries.find { it.id == alignId } ?: defaultState.textStyle.alignment

            QuranTextStyleConfig(
                font = resolvedBuiltInFont,
                selectedFontId = resolvedFontId,
                customFontFilePath = resolvedCustomPath,
                customFontDisplayName = resolvedCustomName,
                enableQuranicFontFallback = enableFallback,
                fallbackFont = fallbackFont,
                fontSizeSp = tsObj.optDouble("fontSizeSp", defaultState.textStyle.fontSizeSp.toDouble()).toFloat().coerceIn(16f, 46f),
                lineSpacingMultiplier = tsObj.optDouble("lineSpacingMultiplier", defaultState.textStyle.lineSpacingMultiplier.toDouble()).toFloat().coerceIn(1.15f, 2.2f),
                colorOption = colorOption,
                mainTextColorHex = tsObj.optLong("mainTextColorHex", defaultState.textStyle.mainTextColorHex),
                enableIndependentTashkeelColor = tsObj.optBoolean("enableIndependentTashkeelColor", defaultState.textStyle.enableIndependentTashkeelColor),
                tashkeelColorHex = tsObj.optLong("tashkeelColorHex", defaultState.textStyle.tashkeelColorHex),
                alignment = alignment,
                isBold = tsObj.optBoolean("isBold", defaultState.textStyle.isBold),
                normalizedX = tsObj.optDouble("normalizedX", defaultState.textStyle.normalizedX.toDouble()).toFloat().coerceIn(0.12f, 0.88f),
                normalizedY = tsObj.optDouble("normalizedY", defaultState.textStyle.normalizedY.toDouble()).toFloat().coerceIn(0.14f, 0.86f),
                isAutoPositioned = tsObj.optBoolean("isAutoPositioned", defaultState.textStyle.isAutoPositioned),
                singleLineDisplay = tsObj.optBoolean("singleLineDisplay", defaultState.textStyle.singleLineDisplay),
                showSurahHeader = tsObj.optBoolean("showSurahHeader", defaultState.textStyle.showSurahHeader),
                showReciterName = tsObj.optBoolean("showReciterName", defaultState.textStyle.showReciterName),
                showTafsir = tsObj.optBoolean("showTafsir", defaultState.textStyle.showTafsir),
                showTranslation = tsObj.optBoolean("showTranslation", defaultState.textStyle.showTranslation),
                translationFontSizeScale = tsObj.optDouble("translationFontSizeScale", defaultState.textStyle.translationFontSizeScale.toDouble()).toFloat(),
                smoothTransitions = true
            )
        } else {
            defaultState.textStyle
        }

        // 5. Recitation Info Header
        val rhObj = root.optJSONObject("recitationHeader")
        val recitationHeader = if (rhObj != null) {
            RecitationHeaderConfig(
                surahName = deserializeHeaderElement(rhObj.optJSONObject("surahName"), defaultState.recitationHeader.surahName),
                verseNumber = deserializeHeaderElement(rhObj.optJSONObject("verseNumber"), defaultState.recitationHeader.verseNumber),
                reciterName = deserializeHeaderElement(rhObj.optJSONObject("reciterName"), defaultState.recitationHeader.reciterName)
            )
        } else {
            defaultState.recitationHeader
        }

        // 6. Background Media (preserve if file/URI is still accessible)
        val pmObj = root.optJSONObject("phoneMedia")
        val phoneMedia = if (pmObj != null) {
            val savedUri = pmObj.optString("uriString", "").takeIf { it.isNotBlank() }
            val validUri = savedUri?.takeIf { isSavedMediaUriAccessible(it) }
            val cropId = pmObj.optString("cropModeId", defaultState.phoneMedia.cropMode.id)
            val cropMode = MediaCropMode.entries.find { it.id == cropId } ?: defaultState.phoneMedia.cropMode
            PhoneMediaBackground(
                uriString = validUri,
                isVideo = pmObj.optBoolean("isVideo", false),
                fileName = pmObj.optString("fileName", ""),
                mimeType = pmObj.optString("mimeType", ""),
                durationMs = pmObj.optLong("durationMs", 0L),
                cropMode = cropMode,
                scale = pmObj.optDouble("scale", 1.0).toFloat().coerceIn(0.5f, 3.0f),
                offsetX = pmObj.optDouble("offsetX", 0.0).toFloat().coerceIn(-1f, 1f),
                offsetY = pmObj.optDouble("offsetY", 0.0).toFloat().coerceIn(-1f, 1f),
                brightness = pmObj.optDouble("brightness", 0.0).toFloat().coerceIn(-0.5f, 0.5f),
                dimLevel = pmObj.optDouble("dimLevel", 0.52).toFloat().coerceIn(0f, 0.9f),
                loopVideoInExport = pmObj.optBoolean("loopVideoInExport", true),
                curatedItemId = pmObj.optString("curatedItemId", "").takeIf { it.isNotBlank() }
            )
        } else {
            defaultState.phoneMedia
        }

        // 7. Personal Signature & Watermark (default to "rattil.official.2026" only when no user-defined signature exists)
        val wmObj = root.optJSONObject("watermark")
        val watermark = if (wmObj != null) {
            val sigText = if (wmObj.has("signatureText")) {
                wmObj.optString("signatureText", DEFAULT_PERSONAL_SIGNATURE)
            } else {
                DEFAULT_PERSONAL_SIGNATURE
            }
            val wmFont = ArabicQuranFont.fromIdOrDefault(wmObj.optString("fontId", defaultState.watermark.font.id))
            val wmAlignId = wmObj.optString("alignmentId", defaultState.watermark.alignment.id)
            val wmAlign = QuranTextAlignment.entries.find { it.id == wmAlignId } ?: defaultState.watermark.alignment

            val platformsSet = LinkedHashSet<SocialPlatform>()
            val platformsArr = wmObj.optJSONArray("selectedSocialPlatforms")
            if (platformsArr != null) {
                for (i in 0 until platformsArr.length()) {
                    val pId = platformsArr.optString(i, "")
                    SocialPlatform.entries.find { it.id == pId }?.let { platformsSet.add(it) }
                }
            }

            WatermarkConfig(
                enabled = wmObj.optBoolean("enabled", defaultState.watermark.enabled),
                signatureText = sigText,
                font = wmFont,
                colorHex = wmObj.optLong("colorHex", defaultState.watermark.colorHex),
                alignment = wmAlign,
                normalizedX = wmObj.optDouble("normalizedX", defaultState.watermark.normalizedX.toDouble()).toFloat().coerceIn(0.08f, 0.92f),
                normalizedY = wmObj.optDouble("normalizedY", defaultState.watermark.normalizedY.toDouble()).toFloat().coerceIn(0.06f, 0.94f),
                fontSizeSp = wmObj.optDouble("fontSizeSp", defaultState.watermark.fontSizeSp.toDouble()).toFloat().coerceIn(11f, 28f),
                scale = wmObj.optDouble("scale", defaultState.watermark.scale.toDouble()).toFloat().coerceIn(0.6f, 1.8f),
                opacity = wmObj.optDouble("opacity", defaultState.watermark.opacity.toDouble()).toFloat().coerceIn(0.2f, 1.0f),
                marginDp = wmObj.optDouble("marginDp", defaultState.watermark.marginDp.toDouble()).toFloat().coerceIn(8f, 48f),
                subtleShadow = wmObj.optBoolean("subtleShadow", defaultState.watermark.subtleShadow),
                elegantGlow = wmObj.optBoolean("elegantGlow", defaultState.watermark.elegantGlow),
                selectedSocialPlatforms = platformsSet,
                socialIconsSizeDp = wmObj.optDouble("socialIconsSizeDp", defaultState.watermark.socialIconsSizeDp.toDouble()).toFloat().coerceIn(12f, 34f),
                socialIconsOffsetY = wmObj.optDouble("socialIconsOffsetY", defaultState.watermark.socialIconsOffsetY.toDouble()).toFloat().coerceIn(-0.5f, 0.5f)
            )
        } else {
            defaultState.watermark
        }

        // 8. Aspect Ratio & Resolution
        val ratioId = root.optString("aspectRatioId", defaultState.aspectRatio.id)
        val aspectRatio = VideoAspectRatio.entries.find { it.id == ratioId } ?: defaultState.aspectRatio
        val resId = root.optString("resolutionId", defaultState.resolution.id)
        val resolution = VideoResolution.entries.find { it.id == resId } ?: defaultState.resolution

        // 9. Transitions: Always enforce automatic internal Fade In / Fade Out (Requirement 2)
        val automaticFadeTransition = VerseTransitionConfig(
            effectType = TransitionEffectType.FADE_IN_OUT,
            speedPreset = TransitionSpeedPreset.NORMAL,
            durationMs = 360L
        )

        val activeValidation = quranFontManager.getValidationReportForFont(rawTextStyle)

        return defaultState.copy(
            mushafConfig = mushafConfig,
            selectedSurah = selectedSurah,
            startVerse = startVerse,
            endVerse = endVerse,
            selectedReciter = resolvedReciter,
            phoneMedia = phoneMedia,
            textStyle = rawTextStyle,
            importedFonts = importedFonts,
            activeFontValidationReport = activeValidation,
            recitationHeader = recitationHeader,
            verseTransition = automaticFadeTransition,
            watermark = watermark,
            aspectRatio = aspectRatio,
            resolution = resolution
        )
    }

    private fun isSavedMediaUriAccessible(uriString: String): Boolean {
        return try {
            val uri = android.net.Uri.parse(uriString)
            if (uri.scheme == "file") {
                val path = uri.path
                !path.isNullOrBlank() && File(path).exists()
            } else {
                appContext.contentResolver.openInputStream(uri)?.use { true } ?: false
            }
        } catch (_: Exception) {
            false
        }
    }

    private fun serializeHeaderElement(el: HeaderElementConfig): JSONObject {
        return JSONObject().apply {
            put("visible", el.visible)
            put("normalizedX", el.normalizedX.toDouble())
            put("normalizedY", el.normalizedY.toDouble())
            put("fontSizeSp", el.fontSizeSp.toDouble())
            put("fontId", el.font.id)
            put("colorHex", el.colorHex)
            put("opacity", el.opacity.toDouble())
            put("alignmentId", el.alignment.id)
            put("marginDp", el.marginDp.toDouble())
        }
    }

    private fun deserializeHeaderElement(obj: JSONObject?, defaultEl: HeaderElementConfig): HeaderElementConfig {
        if (obj == null) return defaultEl
        val font = ArabicQuranFont.fromIdOrDefault(obj.optString("fontId", defaultEl.font.id))
        val alignId = obj.optString("alignmentId", defaultEl.alignment.id)
        val alignment = QuranTextAlignment.entries.find { it.id == alignId } ?: defaultEl.alignment
        return HeaderElementConfig(
            visible = obj.optBoolean("visible", defaultEl.visible),
            normalizedX = obj.optDouble("normalizedX", defaultEl.normalizedX.toDouble()).toFloat().coerceIn(0.05f, 0.95f),
            normalizedY = obj.optDouble("normalizedY", defaultEl.normalizedY.toDouble()).toFloat().coerceIn(0.05f, 0.95f),
            fontSizeSp = obj.optDouble("fontSizeSp", defaultEl.fontSizeSp.toDouble()).toFloat().coerceIn(10f, 38f),
            font = font,
            colorHex = obj.optLong("colorHex", defaultEl.colorHex),
            opacity = obj.optDouble("opacity", defaultEl.opacity.toDouble()).toFloat().coerceIn(0.1f, 1.0f),
            alignment = alignment,
            marginDp = obj.optDouble("marginDp", defaultEl.marginDp.toDouble()).toFloat().coerceIn(4f, 48f)
        )
    }
}
