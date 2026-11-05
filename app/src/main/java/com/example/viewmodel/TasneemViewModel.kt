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
import com.example.data.QuranDataRepository
import com.example.data.ReciterRepository
import com.example.data.ThemeManager
import com.example.media.Mp4VideoRenderEngine
import com.example.media.PhoneMediaImportManager
import com.example.media.PlaybackSyncState
import com.example.media.QuranSynchronizationEngine
import com.example.media.VideoFrameCanvasRenderer
import com.example.model.ArabicQuranFont
import com.example.model.AudioDownloadState
import com.example.model.MediaCropMode
import com.example.model.PhoneMediaBackground
import com.example.model.QuranTextAlignment
import com.example.model.QuranTextStyleConfig
import com.example.model.QuranVerse
import com.example.model.ReciterInfo
import com.example.model.SurahInfo
import com.example.model.TextColorOption
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
    private val audioDownloadManager = AudioDownloadManager(appContext, reciterRepository)
    private val syncEngine = QuranSynchronizationEngine(appContext, viewModelScope)
    private val mediaImportManager = PhoneMediaImportManager(appContext)
    private val frameRenderer = VideoFrameCanvasRenderer(appContext)
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
    // Quran Surah & Verse Range Selection
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
            if (state.selectedVerses.isEmpty()) {
                val res = quranRepository.getSelectedVerses(
                    state.selectedSurah.number,
                    state.startVerse,
                    state.endVerse
                )
                res.onSuccess { verses ->
                    _uiState.update { it.copy(selectedVerses = verses) }
                }
            }

            audioDownloadManager.downloadSelectedVerseAudio(
                reciter = state.selectedReciter,
                surahNumber = state.selectedSurah.number,
                startVerse = state.startVerse,
                endVerse = state.endVerse
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
    // Phone Media Import (Strictly From Phone, No Preloaded Gallery)
    // ==================================================
    fun onPhoneMediaPicked(uri: Uri?, isVideo: Boolean) {
        if (uri == null) return
        val currentMedia = _uiState.value.phoneMedia
        val importRes = mediaImportManager.importMediaUri(uri, isVideo, currentMedia)
        importRes.fold(
            onSuccess = { imported ->
                _uiState.update {
                    it.copy(
                        phoneMedia = imported,
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

    fun updateMediaCropMode(mode: MediaCropMode) {
        _uiState.update { it.copy(phoneMedia = it.phoneMedia.copy(cropMode = mode)) }
    }

    fun updateMediaScale(scale: Float) {
        _uiState.update {
            it.copy(phoneMedia = it.phoneMedia.copy(scale = scale.coerceIn(0.5f, 3.0f)))
        }
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

    fun updateMediaBrightness(brightness: Float) {
        _uiState.update {
            it.copy(phoneMedia = it.phoneMedia.copy(brightness = brightness.coerceIn(-0.5f, 0.5f)))
        }
    }

    fun updateMediaDimLevel(dimLevel: Float) {
        _uiState.update {
            it.copy(phoneMedia = it.phoneMedia.copy(dimLevel = dimLevel.coerceIn(0f, 0.9f)))
        }
    }

    fun updateVideoLoopInExport(loop: Boolean) {
        _uiState.update {
            it.copy(phoneMedia = it.phoneMedia.copy(loopVideoInExport = loop))
        }
    }

    // ==================================================
    // Arabic Font & Quranic Text Appearance Customization
    // ==================================================
    fun updateArabicFont(font: ArabicQuranFont) {
        _uiState.update { it.copy(textStyle = it.textStyle.copy(font = font)) }
    }

    fun updateFontSizeSp(sizeSp: Float) {
        _uiState.update {
            it.copy(textStyle = it.textStyle.copy(fontSizeSp = sizeSp.coerceIn(16f, 44f)))
        }
    }

    fun updateLineSpacing(multiplier: Float) {
        _uiState.update {
            it.copy(textStyle = it.textStyle.copy(lineSpacingMultiplier = multiplier.coerceIn(1.15f, 2.2f)))
        }
    }

    fun updateTextColor(colorOption: TextColorOption) {
        _uiState.update { it.copy(textStyle = it.textStyle.copy(colorOption = colorOption)) }
    }

    fun updateTextAlignment(alignment: QuranTextAlignment) {
        _uiState.update { it.copy(textStyle = it.textStyle.copy(alignment = alignment)) }
    }

    fun updateTextBold(bold: Boolean) {
        _uiState.update { it.copy(textStyle = it.textStyle.copy(isBold = bold)) }
    }

    fun updateTextPosition(normX: Float, normY: Float) {
        _uiState.update {
            it.copy(
                textStyle = it.textStyle.copy(
                    normalizedX = normX.coerceIn(0.15f, 0.85f),
                    normalizedY = normY.coerceIn(0.20f, 0.80f)
                )
            )
        }
    }

    fun toggleShowSurahHeader(show: Boolean) {
        _uiState.update { it.copy(textStyle = it.textStyle.copy(showSurahHeader = show)) }
    }

    fun toggleShowReciterName(show: Boolean) {
        _uiState.update { it.copy(textStyle = it.textStyle.copy(showReciterName = show)) }
    }

    fun toggleShowTafsir(show: Boolean) {
        _uiState.update { it.copy(textStyle = it.textStyle.copy(showTafsir = show)) }
    }

    fun toggleShowTranslation(show: Boolean) {
        _uiState.update { it.copy(textStyle = it.textStyle.copy(showTranslation = show)) }
    }

    // ==================================================
    // Tasneem Watermark Customization ("تسنيم")
    // ==================================================
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

    fun updateWatermarkFontSize(sizeSp: Float) {
        _uiState.update {
            it.copy(watermark = it.watermark.copy(fontSizeSp = sizeSp.coerceIn(11f, 26f)))
        }
    }

    fun updateWatermarkScale(scale: Float) {
        _uiState.update {
            it.copy(watermark = it.watermark.copy(scale = scale.coerceIn(0.6f, 1.8f)))
        }
    }

    fun updateWatermarkOpacity(opacity: Float) {
        _uiState.update {
            it.copy(watermark = it.watermark.copy(opacity = opacity.coerceIn(0.2f, 1.0f)))
        }
    }

    fun updateWatermarkMargin(marginDp: Float) {
        _uiState.update {
            it.copy(watermark = it.watermark.copy(marginDp = marginDp.coerceIn(8f, 48f)))
        }
    }

    fun toggleWatermarkShadow(enabled: Boolean) {
        _uiState.update { it.copy(watermark = it.watermark.copy(subtleShadow = enabled)) }
    }

    fun toggleWatermarkGlow(enabled: Boolean) {
        _uiState.update { it.copy(watermark = it.watermark.copy(elegantGlow = enabled)) }
    }

    // ==================================================
    // Aspect Ratio, Resolution & Complete MP4 Video Export
    // ==================================================
    fun updateAspectRatio(ratio: VideoAspectRatio) {
        _uiState.update { it.copy(aspectRatio = ratio, exportState = VideoExportState.Idle) }
    }

    fun updateVideoResolution(resolution: VideoResolution) {
        _uiState.update { it.copy(resolution = resolution, exportState = VideoExportState.Idle) }
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

        if (state.phoneMedia.uriString.isNullOrBlank()) {
            _uiState.update {
                it.copy(exportState = VideoExportState.Failed("يرجى اختيار صورة أو فيديو من الهاتف كخلفية للفيديو قبل التصدير."))
            }
            return
        }

        syncEngine.pause()
        exportJob?.cancel()
        exportJob = viewModelScope.launch {
            mp4RenderEngine.renderCompleteMp4Video(
                surahNameAr = state.selectedSurah.nameAr,
                verses = state.selectedVerses,
                audioSegments = dlState.segments,
                reciterNameAr = state.selectedReciter.nameAr,
                mediaBackground = state.phoneMedia,
                textStyleConfig = state.textStyle,
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
            ).show()
        }
    }

    override fun onCleared() {
        super.onCleared()
        syncEngine.release()
    }
}
