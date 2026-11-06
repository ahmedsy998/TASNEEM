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
import com.example.data.ReciterRepository
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
import com.example.model.HeaderElementConfig
import com.example.model.MediaCropMode
import com.example.model.MushafBasmalaRuleMode
import com.example.model.MushafEditionConfig
import com.example.model.PhoneMediaBackground
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
    val recitationHeader: RecitationHeaderConfig = RecitationHeaderConfig(),
    val verseTransition: VerseTransitionConfig = VerseTransitionConfig(),
    val watermark: WatermarkConfig = WatermarkConfig(),
    val aspectRatio: VideoAspectRatio = VideoAspectRatio.PORTRAIT_9_16,
    val resolution: VideoResolution = VideoResolution.RES_1080P,
    val exportState: VideoExportState = VideoExportState.Idle
)

class TasneemViewModel(application: Application) : AndroidViewModel(application) {

    private val appContext = application.applicationContext
    private val themeManager = ThemeManager(appContext)
    val quranRepository = QuranDataRepository(appContext)
    val reciterRepository = ReciterRepository()
    val curatedMediaRepository = CuratedBackgroundMediaRepository(appContext)
    private val audioDownloadManager = AudioDownloadManager(appContext, reciterRepository)
    private val syncEngine = QuranSynchronizationEngine(appContext, viewModelScope)
    val mediaImportManager = PhoneMediaImportManager(appContext)
    val frameRenderer = VideoFrameCanvasRenderer(appContext)
    private val mp4RenderEngine = Mp4VideoRenderEngine(appContext, mediaImportManager, frameRenderer)

    val themeMode: StateFlow<AppThemeMode> = themeManager.themeModeFlow.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        AppThemeMode.DARK
    )

    val backgroundStyle: StateFlow<InterfaceBackgroundStyle> = themeManager.backgroundStyleFlow.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        InterfaceBackgroundStyle.DEEP_CHARCOAL
    )

    val playbackSyncState: StateFlow<PlaybackSyncState> = syncEngine.syncState
    val previewReciterId: StateFlow<String?> = syncEngine.previewReciterId

    private val defaultReciter = reciterRepository.allReciters[10] // ياسر الدوسري (index 11)
    private val _uiState = MutableStateFlow(
        StudioUiState(selectedReciter = defaultReciter)
    )
    val uiState: StateFlow<StudioUiState> = _uiState.asStateFlow()

    private var downloadJob: Job? = null
    private var exportJob: Job? = null

    init {
        loadVersesForCurrentSelection(autoDownloadAudio = true)
        initializeDefaultCuratedBackgroundIfEmpty()
    }

    private fun initializeDefaultCuratedBackgroundIfEmpty() {
        viewModelScope.launch {
            if (_uiState.value.phoneMedia.uriString.isNullOrBlank()) {
                val defaultItem = curatedMediaRepository.allItems.firstOrNull() ?: return@launch
                val prepared = curatedMediaRepository.prepareAndCacheMediaItem(defaultItem)
                if (_uiState.value.phoneMedia.uriString.isNullOrBlank()) {
                    _uiState.update { it.copy(phoneMedia = prepared) }
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

    // ==================================================
    // Quran Surah & Verse Range Selection (Requirement 01)
    // ==================================================
    fun selectSurah(surah: SurahInfo) {
        syncEngine.resetPlayback()
        val defaultEnd = minOf(surah.verseCount, 10)
        _uiState.update {
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
        _uiState.update {
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
        _uiState.update {
            it.copy(mushafConfig = it.mushafConfig.copy(basmalaRuleMode = mode))
        }
    }

    fun toggleMushafWaqfSymbols(enabled: Boolean) {
        _uiState.update {
            it.copy(mushafConfig = it.mushafConfig.copy(showQuranicWaqfSymbols = enabled))
        }
    }

    fun resetMushafConfig() {
        _uiState.update {
            it.copy(mushafConfig = MushafEditionConfig())
        }
    }

    // ==================================================
    // Reciter Selection & Exact Selected-Verse Audio Download
    // ==================================================
    fun selectReciter(reciter: ReciterInfo) {
        syncEngine.stopPreview()
        syncEngine.resetPlayback()
        _uiState.update {
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
                _uiState.update {
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
            _uiState.update {
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
        _uiState.update { it.copy(phoneMedia = it.phoneMedia.copy(cropMode = mode)) }
    }

    fun updateMediaScale(scale: Float) {
        _uiState.update {
            it.copy(phoneMedia = it.phoneMedia.copy(scale = scale.coerceIn(0.5f, 3.0f)))
        }
    }

    fun resetMediaScale() {
        updateMediaScale(1.0f)
    }

    fun updateMediaOffset(offsetX: Float, offsetY: Float) {
        _uiState.update {
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
        _uiState.update {
            it.copy(phoneMedia = it.phoneMedia.copy(brightness = brightness.coerceIn(-0.5f, 0.5f)))
        }
    }

    fun resetMediaBrightness() {
        updateMediaBrightness(0f)
    }

    fun updateMediaDimLevel(dimLevel: Float) {
        _uiState.update {
            it.copy(phoneMedia = it.phoneMedia.copy(dimLevel = dimLevel.coerceIn(0f, 0.9f)))
        }
    }

    fun resetMediaDimLevel() {
        updateMediaDimLevel(0.52f)
    }

    fun updateVideoLoopInExport(loop: Boolean) {
        _uiState.update {
            it.copy(phoneMedia = it.phoneMedia.copy(loopVideoInExport = loop))
        }
    }

    // ==================================================
    // Arabic Font, Quranic Text & Independent Tashkeel Colors (Requirements 03, 04, 09, 12)
    // ==================================================
    fun updateArabicFont(font: ArabicQuranFont) {
        _uiState.update { it.copy(textStyle = it.textStyle.copy(font = font)) }
    }

    fun resetArabicFont() {
        updateArabicFont(ArabicQuranFont.KFGQPC_UTHMANIC)
    }

    fun updateFontSizeSp(sizeSp: Float) {
        _uiState.update {
            it.copy(textStyle = it.textStyle.copy(fontSizeSp = sizeSp.coerceIn(16f, 46f)))
        }
    }

    fun resetFontSizeSp() {
        updateFontSizeSp(28f)
    }

    fun updateLineSpacing(multiplier: Float) {
        _uiState.update {
            it.copy(textStyle = it.textStyle.copy(lineSpacingMultiplier = multiplier.coerceIn(1.15f, 2.2f)))
        }
    }

    fun resetLineSpacing() {
        updateLineSpacing(1.45f)
    }

    fun updateTextColor(colorOption: TextColorOption) {
        _uiState.update {
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
        _uiState.update {
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
        _uiState.update {
            it.copy(textStyle = it.textStyle.copy(enableIndependentTashkeelColor = enabled))
        }
    }

    fun updateTashkeelColorHex(colorHex: Long) {
        _uiState.update {
            it.copy(
                textStyle = it.textStyle.copy(
                    enableIndependentTashkeelColor = true,
                    tashkeelColorHex = colorHex
                )
            )
        }
    }

    fun resetTashkeelColor() {
        _uiState.update {
            it.copy(
                textStyle = it.textStyle.copy(
                    enableIndependentTashkeelColor = true,
                    tashkeelColorHex = 0xFFE8872F
                )
            )
        }
    }

    fun updateTextAlignment(alignment: QuranTextAlignment) {
        _uiState.update { it.copy(textStyle = it.textStyle.copy(alignment = alignment)) }
    }

    fun resetTextAlignment() {
        updateTextAlignment(QuranTextAlignment.CENTER)
    }

    fun updateTextBold(bold: Boolean) {
        _uiState.update { it.copy(textStyle = it.textStyle.copy(isBold = bold)) }
    }

    fun toggleSingleLineDisplay(singleLine: Boolean) {
        _uiState.update { it.copy(textStyle = it.textStyle.copy(singleLineDisplay = singleLine)) }
    }

    fun updateTextPosition(normX: Float, normY: Float) {
        _uiState.update {
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
        _uiState.update {
            it.copy(
                textStyle = autoResult.textStyle,
                recitationHeader = autoResult.headerConfig,
                watermark = autoResult.watermarkConfig
            )
        }
    }

    fun toggleShowTafsir(show: Boolean) {
        _uiState.update { it.copy(textStyle = it.textStyle.copy(showTafsir = show)) }
    }

    fun toggleShowTranslation(show: Boolean) {
        _uiState.update { it.copy(textStyle = it.textStyle.copy(showTranslation = show)) }
    }

    // ==================================================
    // Independent Recitation Header Settings (Requirement 08 - إعدادات معلومات التلاوة)
    // ==================================================
    fun updateSurahHeaderElement(transform: (HeaderElementConfig) -> HeaderElementConfig) {
        _uiState.update {
            val updated = transform(it.recitationHeader.surahName)
            it.copy(
                recitationHeader = it.recitationHeader.copy(surahName = updated),
                textStyle = it.textStyle.copy(showSurahHeader = updated.visible)
            )
        }
    }

    fun resetSurahHeaderElement() {
        val default = RecitationHeaderConfig().surahName
        _uiState.update {
            it.copy(
                recitationHeader = it.recitationHeader.copy(surahName = default),
                textStyle = it.textStyle.copy(showSurahHeader = default.visible)
            )
        }
    }

    fun updateVerseNumberHeaderElement(transform: (HeaderElementConfig) -> HeaderElementConfig) {
        _uiState.update {
            val updated = transform(it.recitationHeader.verseNumber)
            it.copy(recitationHeader = it.recitationHeader.copy(verseNumber = updated))
        }
    }

    fun resetVerseNumberHeaderElement() {
        val default = RecitationHeaderConfig().verseNumber
        _uiState.update {
            it.copy(recitationHeader = it.recitationHeader.copy(verseNumber = default))
        }
    }

    fun updateReciterHeaderElement(transform: (HeaderElementConfig) -> HeaderElementConfig) {
        _uiState.update {
            val updated = transform(it.recitationHeader.reciterName)
            it.copy(
                recitationHeader = it.recitationHeader.copy(reciterName = updated),
                textStyle = it.textStyle.copy(showReciterName = updated.visible)
            )
        }
    }

    fun resetReciterHeaderElement() {
        val default = RecitationHeaderConfig().reciterName
        _uiState.update {
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
    // Transition Effects Between Verses & Phrases (Requirement 06)
    // ==================================================
    fun updateTransitionEffect(effect: TransitionEffectType) {
        _uiState.update {
            it.copy(verseTransition = it.verseTransition.copy(effectType = effect))
        }
    }

    fun updateTransitionSpeedPreset(preset: TransitionSpeedPreset) {
        _uiState.update {
            it.copy(
                verseTransition = it.verseTransition.copy(
                    speedPreset = preset,
                    durationMs = preset.durationMs
                )
            )
        }
    }

    fun updateTransitionDurationMs(durationMs: Long) {
        _uiState.update {
            it.copy(
                verseTransition = it.verseTransition.copy(
                    durationMs = durationMs.coerceIn(80L, 1000L)
                )
            )
        }
    }

    fun resetTransitionSettings() {
        _uiState.update { it.copy(verseTransition = VerseTransitionConfig()) }
    }

    // ==================================================
    // Personal Signature ("التوقيع الشخصي") & Social Media Icons (Requirement 10)
    // ==================================================
    fun toggleWatermarkEnabled(enabled: Boolean) {
        _uiState.update { it.copy(watermark = it.watermark.copy(enabled = enabled)) }
    }

    fun updateSignatureText(text: String) {
        _uiState.update { it.copy(watermark = it.watermark.copy(signatureText = text.take(40))) }
    }

    fun resetSignatureText() {
        updateSignatureText("تسنيم")
    }

    fun updateSignatureFont(font: ArabicQuranFont) {
        _uiState.update { it.copy(watermark = it.watermark.copy(font = font)) }
    }

    fun resetSignatureFont() {
        updateSignatureFont(ArabicQuranFont.CAIRO)
    }

    fun updateSignatureColorHex(colorHex: Long) {
        _uiState.update { it.copy(watermark = it.watermark.copy(colorHex = colorHex)) }
    }

    fun resetSignatureColor() {
        updateSignatureColorHex(0xFFF5F1EA)
    }

    fun applyWatermarkPreset(preset: WatermarkPresetPosition) {
        _uiState.update {
            it.copy(
                watermark = it.watermark.copy(
                    normalizedX = preset.normX,
                    normalizedY = preset.normY
                )
            )
        }
    }

    fun updateWatermarkPosition(normX: Float, normY: Float) {
        _uiState.update {
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
        _uiState.update {
            it.copy(watermark = it.watermark.copy(fontSizeSp = sizeSp.coerceIn(11f, 28f)))
        }
    }

    fun resetWatermarkFontSize() {
        updateWatermarkFontSize(15f)
    }

    fun updateWatermarkScale(scale: Float) {
        _uiState.update {
            it.copy(watermark = it.watermark.copy(scale = scale.coerceIn(0.6f, 1.8f)))
        }
    }

    fun resetWatermarkScale() {
        updateWatermarkScale(1.0f)
    }

    fun updateWatermarkOpacity(opacity: Float) {
        _uiState.update {
            it.copy(watermark = it.watermark.copy(opacity = opacity.coerceIn(0.2f, 1.0f)))
        }
    }

    fun resetWatermarkOpacity() {
        updateWatermarkOpacity(0.82f)
    }

    fun updateWatermarkMargin(marginDp: Float) {
        _uiState.update {
            it.copy(watermark = it.watermark.copy(marginDp = marginDp.coerceIn(8f, 48f)))
        }
    }

    fun resetWatermarkMargin() {
        updateWatermarkMargin(16f)
    }

    fun toggleWatermarkShadow(enabled: Boolean) {
        _uiState.update { it.copy(watermark = it.watermark.copy(subtleShadow = enabled)) }
    }

    fun toggleWatermarkGlow(enabled: Boolean) {
        _uiState.update { it.copy(watermark = it.watermark.copy(elegantGlow = enabled)) }
    }

    fun toggleSocialPlatform(platform: SocialPlatform) {
        _uiState.update {
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
        _uiState.update {
            it.copy(watermark = it.watermark.copy(socialIconsSizeDp = sizeDp.coerceIn(12f, 34f)))
        }
    }

    fun resetSocialIconsSizeDp() {
        updateSocialIconsSizeDp(18f)
    }

    fun updateSocialIconsOffsetY(offsetY: Float) {
        _uiState.update {
            it.copy(watermark = it.watermark.copy(socialIconsOffsetY = offsetY.coerceIn(-0.5f, 0.5f)))
        }
    }

    fun resetSocialIconsSettings() {
        _uiState.update {
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
        _uiState.update { current ->
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
        _uiState.update { it.copy(resolution = resolution, exportState = VideoExportState.Idle) }
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
                _uiState.update { it.copy(phoneMedia = prepared) }
                prepared
            } else {
                state.phoneMedia
            }

            mp4RenderEngine.renderCompleteMp4Video(
                surahNameAr = state.selectedSurah.nameAr,
                verses = state.selectedVerses,
                audioSegments = dlState.segments,
                reciterNameAr = state.selectedReciter.nameAr,
                mediaBackground = activeMedia,
                textStyleConfig = state.textStyle,
                headerConfig = state.recitationHeader,
                transitionConfig = state.verseTransition,
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
