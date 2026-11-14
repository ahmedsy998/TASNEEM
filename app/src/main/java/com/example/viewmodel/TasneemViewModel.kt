package com.example.viewmodel

import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AudioDownloadManager
import com.example.data.CuratedBackgroundMediaRepository
import com.example.data.QuranDataRepository
import com.example.data.QuranFontManager
import com.example.data.ReciterRepository
import com.example.data.StudioSettingsManager
import com.example.data.ThemeManager
import com.example.media.Mp4VideoRenderEngine
import com.example.media.PhoneMediaImportManager
import com.example.media.PlaybackSyncState
import com.example.media.QuranSynchronizationEngine
import com.example.media.QuranTextAndLayoutEngine
import com.example.media.VideoFrameCanvasRenderer
import com.example.model.ArabicQuranFont
import com.example.model.AudioDownloadState
import com.example.model.CuratedBackgroundMediaItem
import com.example.model.CustomImportedFont
import com.example.model.DEFAULT_PERSONAL_SIGNATURE
import com.example.model.HeaderElementConfig
import com.example.model.MediaCropMode
import com.example.model.MushafBasmalaRuleMode
import com.example.model.MushafEditionConfig
import com.example.model.PhoneMediaBackground
import com.example.model.QuranFontValidationReport
import com.example.model.QuranTextAlignment
import com.example.model.QuranTextStyleConfig
import com.example.model.QuranVerse
import com.example.model.RecitationHeaderConfig
import com.example.model.ReciterInfo
import com.example.model.SocialPlatform
import com.example.model.SurahInfo
import com.example.model.TextColorOption
import com.example.model.TransitionEffectType
import com.example.model.TransitionSpeedPreset
import com.example.model.VerseTransitionConfig
import com.example.model.VideoAspectRatio
import com.example.model.VideoExportState
import com.example.model.VideoResolution
import com.example.model.WatermarkConfig
import com.example.model.WatermarkPresetPosition
import com.example.ui.theme.AppColorTheme
import com.example.ui.theme.AppThemeMode
import com.example.ui.theme.InterfaceBackgroundStyle
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File

enum class TasneemScreen {
    HOME,
    STUDIO,
    FULLSCREEN_PREVIEW
}

data class StudioUiState(
    val currentScreen: TasneemScreen = TasneemScreen.HOME,
    val showAppearanceModal: Boolean = false,
    val showReciterSelectorModal: Boolean = false,
    val showSurahSelectorModal: Boolean = false,
    val showCuratedLibraryModal: Boolean = false,
    val isDownloadingCuratedMedia: Boolean = false,
    val mushafConfig: MushafEditionConfig = MushafEditionConfig(),
    val selectedSurah: SurahInfo = SurahInfo(112, "الإخلاص", 4, "مكية"),
    val startVerse: Int = 1,
    val endVerse: Int = 4,
    val selectedVerses: List<QuranVerse> = emptyList(),
    val isLoadingVerses: Boolean = false,
    val verseErrorAr: String? = null,
    val selectedReciter: ReciterInfo,
    val audioDownloadState: AudioDownloadState = AudioDownloadState.Idle,
    val phoneMedia: PhoneMediaBackground = PhoneMediaBackground(),
    val mediaImportMessageAr: String? = null,
    val textStyle: QuranTextStyleConfig = QuranTextStyleConfig(),
    val importedFonts: List<CustomImportedFont> = emptyList(),
    val fontImportMessageAr: String? = null,
    val fontImportWarningAr: String? = null,
    val activeFontValidationReport: QuranFontValidationReport? = null,
    val showFontValidationModal: Boolean = false,
    val recitationHeader: RecitationHeaderConfig = RecitationHeaderConfig(),
    val verseTransition: VerseTransitionConfig = VerseTransitionConfig(
        effectType = TransitionEffectType.FADE_IN_OUT,
        speedPreset = TransitionSpeedPreset.NORMAL,
        durationMs = 360L
    ),
    val watermark: WatermarkConfig = WatermarkConfig(
        signatureText = DEFAULT_PERSONAL_SIGNATURE
    ),
    val aspectRatio: VideoAspectRatio = VideoAspectRatio.PORTRAIT_9_16,
    val resolution: VideoResolution = VideoResolution.RES_1080P,
    val exportState: VideoExportState = VideoExportState.Idle
)

class TasneemViewModel(application: Application) : AndroidViewModel(application) {

    private val appContext = application.applicationContext
    private val themeManager = ThemeManager(appContext)
    val quranRepository = QuranDataRepository(appContext)
    val quranFontManager = QuranFontManager(appContext)
    val reciterRepository = ReciterRepository()
    val studioSettingsManager = StudioSettingsManager(appContext, reciterRepository, quranFontManager)
    val curatedMediaRepository = CuratedBackgroundMediaRepository(appContext)
    private val audioDownloadManager = AudioDownloadManager(appContext, reciterRepository)
    private val syncEngine = QuranSynchronizationEngine(appContext, viewModelScope)
    val mediaImportManager = PhoneMediaImportManager(appContext)
    val frameRenderer = VideoFrameCanvasRenderer(appContext)
    val mp4RenderEngine = Mp4VideoRenderEngine(appContext, mediaImportManager, frameRenderer, curatedMediaRepository)

    val themeMode: StateFlow<AppThemeMode> = themeManager.themeModeFlow.stateIn(
        viewModelScope,
        SharingStarted.Eagerly,
        themeManager.getInitialThemeMode()
    )

    val backgroundStyle: StateFlow<InterfaceBackgroundStyle> = themeManager.backgroundStyleFlow.stateIn(
        viewModelScope,
        SharingStarted.Eagerly,
        themeManager.getInitialBackgroundStyle()
    )

    val colorTheme: StateFlow<AppColorTheme> = themeManager.colorThemeFlow.stateIn(
        viewModelScope,
        SharingStarted.Eagerly,
        themeManager.getInitialColorTheme()
    )

    val playbackSyncState: StateFlow<PlaybackSyncState> = syncEngine.syncState
    val previewReciterId: StateFlow<String?> = syncEngine.previewReciterId

    // Initial default reciter is Fares Abbad ("فارس عباد") when no previous preference exists
    private val defaultReciter: ReciterInfo = reciterRepository.defaultReciter
    private val _uiState = MutableStateFlow(studioSettingsManager.restoreStudioUiState())
    val uiState: StateFlow<StudioUiState> = _uiState.asStateFlow()

    private var downloadJob: Job? = null
    private var exportJob: Job? = null

    init {
        loadVersesForCurrentSelection(autoDownloadAudio = true)
        initializeDefaultCuratedBackgroundIfEmpty()
    }

    /**
     * Atomically updates [_uiState] and persists all user-customizable settings via [studioSettingsManager].
     */
    private fun updateAndPersistSettings(transform: (StudioUiState) -> StudioUiState) {
        _uiState.update(transform)
        studioSettingsManager.saveStudioSettings(_uiState.value)
    }

    private fun initializeDefaultCuratedBackgroundIfEmpty() {
        viewModelScope.launch {
            if (_uiState.value.phoneMedia.uriString.isNullOrBlank()) {
                val defaultItem = curatedMediaRepository.allItems.firstOrNull() ?: return@launch
                val currentDim = _uiState.value.phoneMedia.dimLevel
                val prepared = curatedMediaRepository.prepareAndCacheMediaItem(defaultItem, currentDim)
                if (_uiState.value.phoneMedia.uriString.isNullOrBlank()) {
                    updateAndPersistSettings {
                        it.copy(
                            phoneMedia = it.phoneMedia.copy(
                                uriString = prepared.uriString,
                                isVideo = prepared.isVideo,
                                fileName = prepared.fileName,
                                mimeType = prepared.mimeType,
                                curatedItemId = prepared.curatedItemId
                            )
                        )
                    }
                }
            }
        }
    }

    // ==================================================
    // Navigation & Modals
    // ==================================================
    fun navigateToStudio() {
        _uiState.update { it.copy(currentScreen = TasneemScreen.STUDIO) }
    }

    fun navigateToHome() {
        syncEngine.pause()
        _uiState.update { it.copy(currentScreen = TasneemScreen.HOME) }
    }

    fun openFullscreenPreview() {
        _uiState.update { it.copy(currentScreen = TasneemScreen.FULLSCREEN_PREVIEW) }
    }

    fun closeFullscreenPreview() {
        _uiState.update { it.copy(currentScreen = TasneemScreen.STUDIO) }
    }

    fun setAppearanceModalVisible(visible: Boolean) {
        _uiState.update { it.copy(showAppearanceModal = visible) }
    }

    fun setReciterSelectorVisible(visible: Boolean) {
        if (!visible) syncEngine.stopPreview()
        _uiState.update { it.copy(showReciterSelectorModal = visible) }
    }

    fun setSurahSelectorVisible(visible: Boolean) {
        _uiState.update { it.copy(showSurahSelectorModal = visible) }
    }

    fun setCuratedLibraryModalVisible(visible: Boolean) {
        _uiState.update { it.copy(showCuratedLibraryModal = visible) }
    }

    // ==================================================
    // Appearance & Theme Controls
    // ==================================================
    fun updateThemeMode(mode: AppThemeMode) {
        viewModelScope.launch {
            themeManager.setThemeMode(mode)
        }
    }

    fun updateBackgroundStyle(style: InterfaceBackgroundStyle) {
        viewModelScope.launch {
            themeManager.setBackgroundStyle(style)
        }
    }

    fun updateColorTheme(theme: AppColorTheme) {
        themeManager.saveColorThemeSync(theme)
        viewModelScope.launch {
            themeManager.setColorTheme(theme)
        }
    }

    // ==================================================
    // Quran Surah & Verse Range Selection (Requirement 01)
    // ==================================================
    fun selectSurah(surah: SurahInfo) {
        syncEngine.resetPlayback()
        val defaultEnd = minOf(surah.verseCount, 10)
        updateAndPersistSettings {
            it.copy(
                selectedSurah = surah,
                startVerse = 1,
                endVerse = defaultEnd,
                showSurahSelectorModal = false,
                audioDownloadState = AudioDownloadState.Idle,
                exportState = VideoExportState.Idle
            )
        }
        loadVersesForCurrentSelection(autoDownloadAudio = true)
    }

    fun updateVerseRange(newStart: Int, newEnd: Int) {
        val currentSurah = _uiState.value.selectedSurah
        val (validStart, validEnd) = quranRepository.validateVerseRange(
            currentSurah.number,
            newStart,
            newEnd
        )
        if (validStart == _uiState.value.startVerse && validEnd == _uiState.value.endVerse) return

        syncEngine.resetPlayback()
        updateAndPersistSettings {
            it.copy(
                startVerse = validStart,
                endVerse = validEnd,
                audioDownloadState = AudioDownloadState.Idle,
                exportState = VideoExportState.Idle
            )
        }
        loadVersesForCurrentSelection(autoDownloadAudio = false)
    }

    fun resetVerseRange() {
        val surah = _uiState.value.selectedSurah
        updateVerseRange(1, minOf(surah.verseCount, 10))
    }

    fun loadVersesForCurrentSelection(autoDownloadAudio: Boolean = false) {
        val state = _uiState.value
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingVerses = true, verseErrorAr = null) }
            val result = quranRepository.getSelectedVerses(
                surahNumber = state.selectedSurah.number,
                startVerse = state.startVerse,
                endVerse = state.endVerse
            )
            result.fold(
                onSuccess = { verses ->
                    _uiState.update {
                        it.copy(
                            selectedVerses = verses,
                            isLoadingVerses = false,
                            verseErrorAr = null
                        )
                    }
                    if (autoDownloadAudio) {
                        downloadSelectedVersesAudio()
                    }
                },
                onFailure = { err ->
                    _uiState.update {
                        it.copy(
                            isLoadingVerses = false,
                            verseErrorAr = err.localizedMessage ?: "تعذر تحميل الآيات المختارة"
                        )
                    }
                }
            )
        }
    }

    // ==================================================
    // Madinah Mushaf Settings (Section 04 — "إعدادات المصحف")
    // ==================================================
    fun updateMushafBasmalaRuleMode(mode: MushafBasmalaRuleMode) {
        updateAndPersistSettings {
            it.copy(mushafConfig = it.mushafConfig.copy(basmalaRuleMode = mode))
        }
    }

    fun toggleMushafWaqfSymbols(enabled: Boolean) {
        updateAndPersistSettings {
            it.copy(mushafConfig = it.mushafConfig.copy(showQuranicWaqfSymbols = enabled))
        }
    }

    fun resetMushafConfig() {
        updateAndPersistSettings {
            it.copy(mushafConfig = MushafEditionConfig())
        }
    }

    // ==================================================
    // Reciter Selection & Exact Selected-Verse Audio Download
    // ==================================================
    fun selectReciter(reciter: ReciterInfo) {
        syncEngine.stopPreview()
        syncEngine.resetPlayback()
        updateAndPersistSettings {
            it.copy(
                selectedReciter = reciter,
                showReciterSelectorModal = false,
                audioDownloadState = AudioDownloadState.Idle,
                exportState = VideoExportState.Idle
            )
        }
        downloadSelectedVersesAudio()
    }

    fun resetReciterToDefault() {
        selectReciter(defaultReciter)
    }

    fun toggleReciterSamplePreview(reciter: ReciterInfo) {
        val state = _uiState.value
        val sampleUrl = reciterRepository.resolveVerseAudioUrl(
            reciter = reciter,
            surahNumber = state.selectedSurah.number,
            verseNumber = state.startVerse
        )
        if (sampleUrl != null) {
            syncEngine.playReciterPreview(reciter.id, sampleUrl)
        }
    }

    fun downloadSelectedVersesAudio() {
        downloadJob?.cancel()
        val state = _uiState.value
        syncEngine.resetPlayback()

        downloadJob = viewModelScope.launch {
            var currentVerses = state.selectedVerses
            if (currentVerses.isEmpty()) {
                val res = quranRepository.getSelectedVerses(
                    state.selectedSurah.number,
                    state.startVerse,
                    state.endVerse
                )
                res.onSuccess { verses ->
                    currentVerses = verses
                    _uiState.update { it.copy(selectedVerses = verses) }
                }
            }

            audioDownloadManager.downloadSelectedVerseAudio(
                reciter = state.selectedReciter,
                surahNumber = state.selectedSurah.number,
                startVerse = state.startVerse,
                endVerse = state.endVerse,
                verses = currentVerses
            ).collect { dlState ->
                _uiState.update { it.copy(audioDownloadState = dlState) }
                if (dlState is AudioDownloadState.Ready) {
                    syncEngine.loadSegments(dlState.segments)
                }
            }
        }
    }

    // ==================================================
    // Audio Playback & Synchronization Controls
    // ==================================================
    fun togglePlayPause() {
        val dlState = _uiState.value.audioDownloadState
        if (dlState !is AudioDownloadState.Ready) {
            downloadSelectedVersesAudio()
            return
        }
        syncEngine.togglePlayPause()
    }

    fun seekToOverallPosition(positionMs: Long) {
        syncEngine.seekToOverallPosition(positionMs)
    }

    fun seekToVerseIndex(index: Int) {
        syncEngine.seekToSegmentIndex(index)
    }

    // ==================================================
    // Phone Media Import & Curated Background Library (Requirement 14)
    // ==================================================
    fun onPhoneMediaPicked(uri: Uri?, isVideo: Boolean) {
        if (uri == null) return
        val currentMedia = _uiState.value.phoneMedia
        val importRes = mediaImportManager.importMediaUri(uri, isVideo, currentMedia)
        importRes.fold(
            onSuccess = { imported ->
                updateAndPersistSettings {
                    it.copy(
                        phoneMedia = imported.copy(curatedItemId = null),
                        mediaImportMessageAr = if (imported.isVideo) {
                            "تم استيراد الفيديو من الهاتف بنجاح: ${imported.fileName}"
                        } else {
                            "تم استيراد الصورة من الهاتف بنجاح: ${imported.fileName}"
                        },
                        exportState = VideoExportState.Idle
                    )
                }
            },
            onFailure = { err ->
                _uiState.update {
                    it.copy(
                        mediaImportMessageAr = err.localizedMessage ?: "تعذر استيراد الملف من الهاتف"
                    )
                }
            }
        )
    }

    fun selectCuratedBackgroundItem(item: CuratedBackgroundMediaItem) {
        viewModelScope.launch {
            _uiState.update { it.copy(isDownloadingCuratedMedia = true) }
            val currentDim = _uiState.value.phoneMedia.dimLevel
            val prepared = curatedMediaRepository.prepareAndCacheMediaItem(item, currentDim)
            updateAndPersistSettings {
                it.copy(
                    phoneMedia = prepared,
                    isDownloadingCuratedMedia = false,
                    showCuratedLibraryModal = false,
                    mediaImportMessageAr = "تم تطبيق الخلفية المختارة من مكتبة تسنيم: ${item.titleAr}",
                    exportState = VideoExportState.Idle
                )
            }
        }
    }

    fun updateMediaCropMode(mode: MediaCropMode) {
        updateAndPersistSettings { it.copy(phoneMedia = it.phoneMedia.copy(cropMode = mode)) }
    }

    fun updateMediaScale(scale: Float) {
        updateAndPersistSettings {
            it.copy(phoneMedia = it.phoneMedia.copy(scale = scale.coerceIn(0.5f, 3.0f)))
        }
    }

    fun resetMediaScale() {
        updateMediaScale(1.0f)
    }

    fun updateMediaOffset(offsetX: Float, offsetY: Float) {
        updateAndPersistSettings {
            it.copy(
                phoneMedia = it.phoneMedia.copy(
                    offsetX = offsetX.coerceIn(-1f, 1f),
                    offsetY = offsetY.coerceIn(-1f, 1f)
                )
            )
        }
    }

    fun resetMediaOffset() {
        updateMediaOffset(0f, 0f)
    }

    fun updateMediaBrightness(brightness: Float) {
        updateAndPersistSettings {
            it.copy(phoneMedia = it.phoneMedia.copy(brightness = brightness.coerceIn(-0.5f, 0.5f)))
        }
    }

    fun resetMediaBrightness() {
        updateMediaBrightness(0f)
    }

    fun updateMediaDimLevel(dimLevel: Float) {
        updateAndPersistSettings {
            it.copy(phoneMedia = it.phoneMedia.copy(dimLevel = dimLevel.coerceIn(0f, 0.9f)))
        }
    }

    fun resetMediaDimLevel() {
        updateMediaDimLevel(0.52f)
    }

    fun updateVideoLoopInExport(loop: Boolean) {
        updateAndPersistSettings {
            it.copy(phoneMedia = it.phoneMedia.copy(loopVideoInExport = loop))
        }
    }

    // ==================================================
    // Arabic Font, Custom Font Import, Quranic Validation & Fallback (Sections 02..10)
    // ==================================================
    fun updateArabicFont(font: ArabicQuranFont) {
        val report = quranFontManager.getBuiltInFontValidationReport(font)
        val autoEnableFallback = if (report.requiresFallbackForQuran) true else _uiState.value.textStyle.enableQuranicFontFallback
        updateAndPersistSettings {
            it.copy(
                textStyle = it.textStyle.copy(
                    font = font,
                    selectedFontId = font.id,
                    customFontFilePath = null,
                    customFontDisplayName = null,
                    enableQuranicFontFallback = autoEnableFallback
                ),
                activeFontValidationReport = report,
                fontImportWarningAr = report.warningMessageAr
            )
        }
    }

    fun selectImportedFont(importedFont: CustomImportedFont) {
        val autoEnableFallback = if (importedFont.validationReport.requiresFallbackForQuran) {
            true
        } else {
            _uiState.value.textStyle.enableQuranicFontFallback
        }
        updateAndPersistSettings {
            it.copy(
                textStyle = it.textStyle.copy(
                    selectedFontId = importedFont.id,
                    customFontFilePath = importedFont.localFilePath,
                    customFontDisplayName = importedFont.displayName,
                    enableQuranicFontFallback = autoEnableFallback
                ),
                activeFontValidationReport = importedFont.validationReport,
                fontImportWarningAr = importedFont.validationReport.warningMessageAr
            )
        }
    }

    fun onCustomFontPickedFromPhone(uri: Uri?) {
        if (uri == null) return
        val result = quranFontManager.importFontFromUri(uri)
        result.fold(
            onSuccess = { importRes ->
                val updatedList = quranFontManager.getImportedFonts()
                val autoEnableFallback = if (importRes.importedFont.validationReport.requiresFallbackForQuran) {
                    true
                } else {
                    _uiState.value.textStyle.enableQuranicFontFallback
                }
                updateAndPersistSettings {
                    it.copy(
                        importedFonts = updatedList,
                        textStyle = it.textStyle.copy(
                            selectedFontId = importRes.importedFont.id,
                            customFontFilePath = importRes.importedFont.localFilePath,
                            customFontDisplayName = importRes.importedFont.displayName,
                            enableQuranicFontFallback = autoEnableFallback
                        ),
                        activeFontValidationReport = importRes.importedFont.validationReport,
                        fontImportMessageAr = importRes.messageAr,
                        fontImportWarningAr = importRes.warningAr
                    )
                }
            },
            onFailure = { err ->
                _uiState.update {
                    it.copy(
                        fontImportMessageAr = null,
                        fontImportWarningAr = err.localizedMessage ?: "تعذر استيراد ملف الخط من الهاتف."
                    )
                }
            }
        )
    }

    fun importCustomFontFromFile(file: File, originalFileNameOverride: String? = null): Result<CustomImportedFont> {
        val result = quranFontManager.importFontFromFile(file, originalFileNameOverride)
        return result.map { importRes ->
            val updatedList = quranFontManager.getImportedFonts()
            val autoEnableFallback = if (importRes.importedFont.validationReport.requiresFallbackForQuran) {
                true
            } else {
                _uiState.value.textStyle.enableQuranicFontFallback
            }
            updateAndPersistSettings {
                it.copy(
                    importedFonts = updatedList,
                    textStyle = it.textStyle.copy(
                        selectedFontId = importRes.importedFont.id,
                        customFontFilePath = importRes.importedFont.localFilePath,
                        customFontDisplayName = importRes.importedFont.displayName,
                        enableQuranicFontFallback = autoEnableFallback
                    ),
                    activeFontValidationReport = importRes.importedFont.validationReport,
                    fontImportMessageAr = importRes.messageAr,
                    fontImportWarningAr = importRes.warningAr
                )
            }
            importRes.importedFont
        }.onFailure { err ->
            _uiState.update {
                it.copy(
                    fontImportMessageAr = null,
                    fontImportWarningAr = err.localizedMessage ?: "تعذر استيراد ملف الخط."
                )
            }
        }
    }

    fun removeImportedFont(fontId: String) {
        val wasSelected = _uiState.value.textStyle.selectedFontId == fontId
        val deleted = quranFontManager.removeImportedFont(fontId)
        if (!deleted) return
        val updatedList = quranFontManager.getImportedFonts()
        if (wasSelected) {
            val defaultFont = ArabicQuranFont.KFGQPC_UTHMANIC
            val defaultReport = quranFontManager.getBuiltInFontValidationReport(defaultFont)
            updateAndPersistSettings {
                it.copy(
                    importedFonts = updatedList,
                    textStyle = it.textStyle.copy(
                        font = defaultFont,
                        selectedFontId = defaultFont.id,
                        customFontFilePath = null,
                        customFontDisplayName = null
                    ),
                    activeFontValidationReport = defaultReport,
                    fontImportMessageAr = "تم حذف الخط المستورد والتبديل تلقائيًا إلى خط مصحف المدينة الافتراضي.",
                    fontImportWarningAr = null
                )
            }
        } else {
            updateAndPersistSettings {
                it.copy(
                    importedFonts = updatedList,
                    fontImportMessageAr = "تم حذف الخط المستورد بنجاح."
                )
            }
        }
    }

    fun toggleQuranicFontFallback(enabled: Boolean) {
        updateAndPersistSettings {
            it.copy(textStyle = it.textStyle.copy(enableQuranicFontFallback = enabled))
        }
    }

    fun setFontValidationModalVisible(visible: Boolean) {
        val report = if (visible) {
            quranFontManager.getValidationReportForFont(_uiState.value.textStyle)
        } else {
            _uiState.value.activeFontValidationReport
        }
        _uiState.update {
            it.copy(
                showFontValidationModal = visible,
                activeFontValidationReport = report
            )
        }
    }

    fun resetArabicFont() {
        updateArabicFont(ArabicQuranFont.KFGQPC_UTHMANIC)
    }

    fun updateFontSizeSp(sizeSp: Float) {
        updateAndPersistSettings {
            it.copy(textStyle = it.textStyle.copy(fontSizeSp = sizeSp.coerceIn(16f, 46f)))
        }
    }

    fun resetFontSizeSp() {
        updateFontSizeSp(28f)
    }

    fun updateLineSpacing(multiplier: Float) {
        updateAndPersistSettings {
            it.copy(textStyle = it.textStyle.copy(lineSpacingMultiplier = multiplier.coerceIn(1.15f, 2.2f)))
        }
    }

    fun resetLineSpacing() {
        updateLineSpacing(1.45f)
    }

    fun updateTextColor(colorOption: TextColorOption) {
        updateAndPersistSettings {
            it.copy(
                textStyle = it.textStyle.copy(
                    colorOption = colorOption,
                    mainTextColorHex = colorOption.colorHex
                )
            )
        }
    }

    fun updateMainTextColorHex(colorHex: Long) {
        val matchingOption = TextColorOption.entries.find { it.colorHex == colorHex } ?: _uiState.value.textStyle.colorOption
        updateAndPersistSettings {
            it.copy(
                textStyle = it.textStyle.copy(
                    colorOption = matchingOption,
                    mainTextColorHex = colorHex
                )
            )
        }
    }

    fun resetMainTextColor() {
        updateMainTextColorHex(0xFFFFFFFF)
    }

    fun toggleIndependentTashkeelColor(enabled: Boolean) {
        updateAndPersistSettings {
            it.copy(textStyle = it.textStyle.copy(enableIndependentTashkeelColor = enabled))
        }
    }

    fun updateTashkeelColorHex(colorHex: Long) {
        updateAndPersistSettings {
            it.copy(
                textStyle = it.textStyle.copy(
                    enableIndependentTashkeelColor = true,
                    tashkeelColorHex = colorHex
                )
            )
        }
    }

    fun resetTashkeelColor() {
        updateAndPersistSettings {
            it.copy(
                textStyle = it.textStyle.copy(
                    enableIndependentTashkeelColor = true,
                    tashkeelColorHex = 0xFFE8872F
                )
            )
        }
    }

    fun updateTextAlignment(alignment: QuranTextAlignment) {
        updateAndPersistSettings { it.copy(textStyle = it.textStyle.copy(alignment = alignment)) }
    }

    fun resetTextAlignment() {
        updateTextAlignment(QuranTextAlignment.CENTER)
    }

    fun updateTextBold(bold: Boolean) {
        updateAndPersistSettings { it.copy(textStyle = it.textStyle.copy(isBold = bold)) }
    }

    fun toggleSingleLineDisplay(singleLine: Boolean) {
        updateAndPersistSettings { it.copy(textStyle = it.textStyle.copy(singleLineDisplay = singleLine)) }
    }

    fun updateTextPosition(normX: Float, normY: Float) {
        updateAndPersistSettings {
            it.copy(
                textStyle = it.textStyle.copy(
                    normalizedX = normX.coerceIn(0.12f, 0.88f),
                    normalizedY = normY.coerceIn(0.14f, 0.86f),
                    isAutoPositioned = false
                )
            )
        }
    }

    fun restoreAutomaticPositioning() {
        val state = _uiState.value
        val autoResult = QuranTextAndLayoutEngine.computeAutoLayout(
            aspectRatio = state.aspectRatio,
            currentTextStyle = state.textStyle,
            currentHeader = state.recitationHeader,
            currentWatermark = state.watermark
        )
        updateAndPersistSettings {
            it.copy(
                textStyle = autoResult.textStyle,
                recitationHeader = autoResult.headerConfig,
                watermark = autoResult.watermarkConfig
            )
        }
    }

    fun toggleShowTafsir(show: Boolean) {
        updateAndPersistSettings { it.copy(textStyle = it.textStyle.copy(showTafsir = show)) }
    }

    fun toggleShowTranslation(show: Boolean) {
        updateAndPersistSettings { it.copy(textStyle = it.textStyle.copy(showTranslation = show)) }
    }

    // ==================================================
    // Independent Recitation Header Settings (Requirement 08 - إعدادات معلومات التلاوة)
    // ==================================================
    fun updateSurahHeaderElement(transform: (HeaderElementConfig) -> HeaderElementConfig) {
        updateAndPersistSettings {
            val updated = transform(it.recitationHeader.surahName)
            it.copy(
                recitationHeader = it.recitationHeader.copy(surahName = updated),
                textStyle = it.textStyle.copy(showSurahHeader = updated.visible)
            )
        }
    }

    fun resetSurahHeaderElement() {
        val default = RecitationHeaderConfig().surahName
        updateAndPersistSettings {
            it.copy(
                recitationHeader = it.recitationHeader.copy(surahName = default),
                textStyle = it.textStyle.copy(showSurahHeader = default.visible)
            )
        }
    }

    fun updateVerseNumberHeaderElement(transform: (HeaderElementConfig) -> HeaderElementConfig) {
        updateAndPersistSettings {
            val updated = transform(it.recitationHeader.verseNumber)
            it.copy(recitationHeader = it.recitationHeader.copy(verseNumber = updated))
        }
    }

    fun resetVerseNumberHeaderElement() {
        val default = RecitationHeaderConfig().verseNumber
        updateAndPersistSettings {
            it.copy(recitationHeader = it.recitationHeader.copy(verseNumber = default))
        }
    }

    fun updateReciterHeaderElement(transform: (HeaderElementConfig) -> HeaderElementConfig) {
        updateAndPersistSettings {
            val updated = transform(it.recitationHeader.reciterName)
            it.copy(
                recitationHeader = it.recitationHeader.copy(reciterName = updated),
                textStyle = it.textStyle.copy(showReciterName = updated.visible)
            )
        }
    }

    fun resetReciterHeaderElement() {
        val default = RecitationHeaderConfig().reciterName
        updateAndPersistSettings {
            it.copy(
                recitationHeader = it.recitationHeader.copy(reciterName = default),
                textStyle = it.textStyle.copy(showReciterName = default.visible)
            )
        }
    }

    // Legacy convenience delegators
    fun toggleShowSurahHeader(show: Boolean) {
        updateSurahHeaderElement { it.copy(visible = show) }
    }

    fun toggleShowReciterName(show: Boolean) {
        updateReciterHeaderElement { it.copy(visible = show) }
    }

    // ==================================================
    // Personal Signature ("التوقيع الشخصي") & Social Media Icons (Requirement 10)
    // ==================================================
    fun toggleWatermarkEnabled(enabled: Boolean) {
        updateAndPersistSettings { it.copy(watermark = it.watermark.copy(enabled = enabled)) }
    }

    fun updateSignatureText(text: String) {
        updateAndPersistSettings { it.copy(watermark = it.watermark.copy(signatureText = text.take(40))) }
    }

    fun resetSignatureText() {
        updateSignatureText(DEFAULT_PERSONAL_SIGNATURE)
    }

    fun updateSignatureFont(font: ArabicQuranFont) {
        updateAndPersistSettings { it.copy(watermark = it.watermark.copy(font = font)) }
    }

    fun resetSignatureFont() {
        updateSignatureFont(ArabicQuranFont.CAIRO)
    }

    fun updateSignatureColorHex(colorHex: Long) {
        updateAndPersistSettings { it.copy(watermark = it.watermark.copy(colorHex = colorHex)) }
    }

    fun resetSignatureColor() {
        updateSignatureColorHex(0xFFF5F1EA)
    }

    fun applyWatermarkPreset(preset: WatermarkPresetPosition) {
        updateAndPersistSettings {
            it.copy(
                watermark = it.watermark.copy(
                    normalizedX = preset.normX,
                    normalizedY = preset.normY
                )
            )
        }
    }

    fun updateWatermarkPosition(normX: Float, normY: Float) {
        updateAndPersistSettings {
            it.copy(
                watermark = it.watermark.copy(
                    normalizedX = normX.coerceIn(0.08f, 0.92f),
                    normalizedY = normY.coerceIn(0.06f, 0.94f)
                )
            )
        }
    }

    fun resetWatermarkPosition() {
        applyWatermarkPreset(WatermarkPresetPosition.BOTTOM_CENTER)
    }

    fun updateWatermarkFontSize(sizeSp: Float) {
        updateAndPersistSettings {
            it.copy(watermark = it.watermark.copy(fontSizeSp = sizeSp.coerceIn(11f, 28f)))
        }
    }

    fun resetWatermarkFontSize() {
        updateWatermarkFontSize(15f)
    }

    fun updateWatermarkScale(scale: Float) {
        updateAndPersistSettings {
            it.copy(watermark = it.watermark.copy(scale = scale.coerceIn(0.6f, 1.8f)))
        }
    }

    fun resetWatermarkScale() {
        updateWatermarkScale(1.0f)
    }

    fun updateWatermarkOpacity(opacity: Float) {
        updateAndPersistSettings {
            it.copy(watermark = it.watermark.copy(opacity = opacity.coerceIn(0.2f, 1.0f)))
        }
    }

    fun resetWatermarkOpacity() {
        updateWatermarkOpacity(0.82f)
    }

    fun updateWatermarkMargin(marginDp: Float) {
        updateAndPersistSettings {
            it.copy(watermark = it.watermark.copy(marginDp = marginDp.coerceIn(8f, 48f)))
        }
    }

    fun resetWatermarkMargin() {
        updateWatermarkMargin(16f)
    }

    fun toggleWatermarkShadow(enabled: Boolean) {
        updateAndPersistSettings { it.copy(watermark = it.watermark.copy(subtleShadow = enabled)) }
    }

    fun toggleWatermarkGlow(enabled: Boolean) {
        updateAndPersistSettings { it.copy(watermark = it.watermark.copy(elegantGlow = enabled)) }
    }

    fun toggleSocialPlatform(platform: SocialPlatform) {
        updateAndPersistSettings {
            val current = it.watermark.selectedSocialPlatforms.toMutableSet()
            if (platform in current) {
                current.remove(platform)
            } else {
                current.add(platform)
            }
            it.copy(watermark = it.watermark.copy(selectedSocialPlatforms = current))
        }
    }

    fun updateSocialIconsSizeDp(sizeDp: Float) {
        updateAndPersistSettings {
            it.copy(watermark = it.watermark.copy(socialIconsSizeDp = sizeDp.coerceIn(12f, 34f)))
        }
    }

    fun resetSocialIconsSizeDp() {
        updateSocialIconsSizeDp(18f)
    }

    fun updateSocialIconsOffsetY(offsetY: Float) {
        updateAndPersistSettings {
            it.copy(watermark = it.watermark.copy(socialIconsOffsetY = offsetY.coerceIn(-0.5f, 0.5f)))
        }
    }

    fun resetSocialIconsSettings() {
        updateAndPersistSettings {
            it.copy(
                watermark = it.watermark.copy(
                    selectedSocialPlatforms = emptySet(),
                    socialIconsSizeDp = 18f,
                    socialIconsOffsetY = 0f
                )
            )
        }
    }

    // ==================================================
    // Aspect Ratio, Resolution & Complete MP4 Video Export
    // ==================================================
    fun updateAspectRatio(ratio: VideoAspectRatio) {
        updateAndPersistSettings { current ->
            if (current.textStyle.isAutoPositioned) {
                val auto = QuranTextAndLayoutEngine.computeAutoLayout(
                    aspectRatio = ratio,
                    currentTextStyle = current.textStyle,
                    currentHeader = current.recitationHeader,
                    currentWatermark = current.watermark
                )
                current.copy(
                    aspectRatio = ratio,
                    textStyle = auto.textStyle,
                    recitationHeader = auto.headerConfig,
                    watermark = auto.watermarkConfig,
                    exportState = VideoExportState.Idle
                )
            } else {
                current.copy(aspectRatio = ratio, exportState = VideoExportState.Idle)
            }
        }
    }

    fun resetAspectRatio() {
        updateAspectRatio(VideoAspectRatio.PORTRAIT_9_16)
    }

    fun updateVideoResolution(resolution: VideoResolution) {
        updateAndPersistSettings { it.copy(resolution = resolution, exportState = VideoExportState.Idle) }
    }

    fun resetVideoResolution() {
        updateVideoResolution(VideoResolution.RES_1080P)
    }

    fun startMp4VideoExport() {
        val state = _uiState.value
        val dlState = state.audioDownloadState

        if (state.selectedVerses.isEmpty()) {
            _uiState.update {
                it.copy(exportState = VideoExportState.Failed("يرجى التحقق من اختيار السورة ونطاق الآيات أولاً."))
            }
            return
        }

        if (dlState !is AudioDownloadState.Ready || dlState.segments.isEmpty()) {
            _uiState.update {
                it.copy(exportState = VideoExportState.Failed("يرجى تحميل الصوت للآيات المختارة أولاً قبل توليد الفيديو."))
            }
            return
        }

        syncEngine.pause()
        exportJob?.cancel()
        exportJob = viewModelScope.launch {
            // If no media background was chosen yet, prepare the default curated background automatically
            val activeMedia = if (state.phoneMedia.uriString.isNullOrBlank()) {
                val fallbackItem = curatedMediaRepository.allItems.first()
                val prepared = curatedMediaRepository.prepareAndCacheMediaItem(fallbackItem, state.phoneMedia.dimLevel)
                updateAndPersistSettings { it.copy(phoneMedia = prepared) }
                prepared
            } else {
                state.phoneMedia
            }

            // Always render with smooth Fade In / Fade Out transition (Requirement 2)
            val automaticFadeTransition = VerseTransitionConfig(
                effectType = TransitionEffectType.FADE_IN_OUT,
                speedPreset = TransitionSpeedPreset.NORMAL,
                durationMs = 360L
            )

            mp4RenderEngine.renderCompleteMp4Video(
                surahNameAr = state.selectedSurah.nameAr,
                verses = state.selectedVerses,
                audioSegments = dlState.segments,
                reciterNameAr = state.selectedReciter.nameAr,
                mediaBackground = activeMedia,
                textStyleConfig = state.textStyle,
                headerConfig = state.recitationHeader,
                transitionConfig = automaticFadeTransition,
                watermarkConfig = state.watermark,
                resolution = state.resolution,
                aspectRatio = state.aspectRatio
            ).collect { exportProgress ->
                _uiState.update { it.copy(exportState = exportProgress) }
            }
        }
    }

    fun cancelMp4Export() {
        exportJob?.cancel()
        _uiState.update { it.copy(exportState = VideoExportState.Idle) }
    }

    fun saveVideoToPhoneGallery(context: Context) {
        val exp = _uiState.value.exportState
        if (exp !is VideoExportState.Completed) return
        val file = File(exp.validationReport.localFilePath)
        if (!file.exists()) {
            Toast.makeText(context, "ملف الفيديو غير موجود.", Toast.LENGTH_SHORT).show()
            return
        }
        val uri = mp4RenderEngine.saveMp4ToMediaStore(file, _uiState.value.selectedSurah.nameAr)
        if (uri != null) {
            Toast.makeText(
                context,
                "تم حفظ الفيديو بصيغة MP4 في مجلد الأفلام (Movies/Tasneem) بنجاح.",
                Toast.LENGTH_LONG
            ).show()
        } else {
            Toast.makeText(
                context,
                "تم حفظ النسخة المحلية في ذاكرة التطبيق: ${file.name}",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    fun openExportedVideo(context: Context) {
        val exp = _uiState.value.exportState
        if (exp !is VideoExportState.Completed) return
        try {
            val file = File(exp.validationReport.localFilePath)
            val uri = if (!exp.validationReport.mediaStoreUriString.isNullOrBlank()) {
                Uri.parse(exp.validationReport.mediaStoreUriString)
            } else {
                FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    file
                )
            }
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "video/mp4")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(Intent.createChooser(intent, "تشغيل الفيديو القرآني (MP4)"))
        } catch (e: Exception) {
            Toast.makeText(
                context,
                "لا يوجد مشغل فيديو خارجي متاح على هذا الجهاز؛ يمكنك المعاينة داخل التطبيق.",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    fun shareExportedVideo(context: Context) {
        val exp = _uiState.value.exportState
        if (exp !is VideoExportState.Completed) return
        try {
            val file = File(exp.validationReport.localFilePath)
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "video/mp4"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(
                    Intent.EXTRA_TEXT,
                    "فيديو قرآني من إنتاج استوديو تسنيم — سورة ${_uiState.value.selectedSurah.nameAr}"
                )
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(shareIntent, "مشاركة الفيديو القرآني (MP4)"))
        } catch (e: Exception) {
            Toast.makeText(
                context,
                "تعذر فتح نافذة المشاركة: ${e.localizedMessage ?: ""}",
                Toast.LENGTH_SHORT
            )
                .show()
        }
    }

    override fun onCleared() {
        super.onCleared()
        syncEngine.release()
    }
}
