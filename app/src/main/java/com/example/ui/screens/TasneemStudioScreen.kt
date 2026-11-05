package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.media.PlaybackSyncState
import com.example.ui.components.Mp4ExportAndValidationSectionCard
import com.example.ui.components.PersistentLivePreviewPanel
import com.example.ui.components.PhoneMediaImportSectionCard
import com.example.ui.components.QuranAndReciterSectionCard
import com.example.ui.components.TypographyAndWatermarkSectionCard
import com.example.viewmodel.StudioUiState
import com.example.viewmodel.TasneemScreen
import com.example.viewmodel.TasneemViewModel

/**
 * Tasneem Quranic Video Creation Studio Screen (Sections 05..16).
 * Enforces a FIXED Persistent Live Preview ("البث الحي") at the top of the screen while
 * all editing controls scroll smoothly underneath it.
 */
@Composable
fun TasneemStudioScreen(
    uiState: StudioUiState,
    playbackSyncState: PlaybackSyncState,
    viewModel: TasneemViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isFullscreen = uiState.currentScreen == TasneemScreen.FULLSCREEN_PREVIEW

    BackHandler {
        if (isFullscreen) {
            viewModel.closeFullscreenPreview()
        } else {
            viewModel.navigateToHome()
        }
    }

    // Native Android Photo Picker Launchers for Phone Image & Phone Video (Section 11)
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            viewModel.onPhoneMediaPicked(uri, isVideo = false)
        }
    }

    val videoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            viewModel.onPhoneMediaPicked(uri, isVideo = true)
        }
    }

    Column(
        modifier = modifier.fillMaxSize()
    ) {
        // 1. PERSISTENT LIVE PREVIEW ("البث الحي") - FIXED AT TOP, NEVER SCROLLS OUT OF VIEW
        PersistentLivePreviewPanel(
            surahNameAr = uiState.selectedSurah.nameAr,
            selectedVerses = uiState.selectedVerses,
            reciterNameAr = uiState.selectedReciter.nameAr,
            playbackSyncState = playbackSyncState,
            audioDownloadState = uiState.audioDownloadState,
            phoneMedia = uiState.phoneMedia,
            textStyle = uiState.textStyle,
            watermark = uiState.watermark,
            aspectRatio = uiState.aspectRatio,
            isFullscreen = isFullscreen,
            onTogglePlayPause = viewModel::togglePlayPause,
            onSeekToMs = viewModel::seekToOverallPosition,
            onUpdateWatermarkPosition = viewModel::updateWatermarkPosition,
            onUpdateTextPosition = viewModel::updateTextPosition,
            onToggleFullscreen = {
                if (isFullscreen) viewModel.closeFullscreenPreview()
                else viewModel.openFullscreenPreview()
            }
        )

        // 2. SCROLLABLE STUDIO SETTINGS CONTAINER BELOW THE FIXED LIVE PREVIEW
        if (!isFullscreen) {
            val scrollState = rememberScrollState()
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(scrollState)
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Step 1: Quran Surah, Verse Range, Reciter & Selected-Verse Audio Download
                QuranAndReciterSectionCard(
                    selectedSurah = uiState.selectedSurah,
                    startVerse = uiState.startVerse,
                    endVerse = uiState.endVerse,
                    selectedVerses = uiState.selectedVerses,
                    activeVerseNumber = playbackSyncState.activeVerseNumber,
                    selectedReciter = uiState.selectedReciter,
                    audioDownloadState = uiState.audioDownloadState,
                    onOpenSurahModal = { viewModel.setSurahSelectorVisible(true) },
                    onUpdateVerseRange = viewModel::updateVerseRange,
                    onOpenReciterModal = { viewModel.setReciterSelectorVisible(true) },
                    onDownloadAudio = viewModel::downloadSelectedVersesAudio,
                    onTapVerseToSeek = viewModel::seekToVerseIndex
                )

                // Step 2: Phone Media Import (Choose Image from Phone / Choose Video from Phone)
                PhoneMediaImportSectionCard(
                    phoneMedia = uiState.phoneMedia,
                    mediaImportMessageAr = uiState.mediaImportMessageAr,
                    onChooseImageFromPhone = {
                        imagePickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    onChooseVideoFromPhone = {
                        videoPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)
                        )
                    },
                    onUpdateCropMode = viewModel::updateMediaCropMode,
                    onUpdateScale = viewModel::updateMediaScale,
                    onUpdateOffset = viewModel::updateMediaOffset,
                    onUpdateBrightness = viewModel::updateMediaBrightness,
                    onUpdateDimLevel = viewModel::updateMediaDimLevel,
                    onUpdateLoopVideo = viewModel::updateVideoLoopInExport
                )

                // Step 3: Arabic Quran Font Library, Text Styling & Tasneem Watermark
                TypographyAndWatermarkSectionCard(
                    aspectRatio = uiState.aspectRatio,
                    textStyle = uiState.textStyle,
                    watermark = uiState.watermark,
                    onSelectAspectRatio = viewModel::updateAspectRatio,
                    onSelectFont = viewModel::updateArabicFont,
                    onUpdateFontSize = viewModel::updateFontSizeSp,
                    onUpdateLineSpacing = viewModel::updateLineSpacing,
                    onSelectTextColor = viewModel::updateTextColor,
                    onSelectAlignment = viewModel::updateTextAlignment,
                    onToggleBold = viewModel::updateTextBold,
                    onToggleSurahHeader = viewModel::toggleShowSurahHeader,
                    onToggleReciterName = viewModel::toggleShowReciterName,
                    onToggleTafsir = viewModel::toggleShowTafsir,
                    onToggleTranslation = viewModel::toggleShowTranslation,
                    onApplyWatermarkPreset = viewModel::applyWatermarkPreset,
                    onUpdateWatermarkSize = viewModel::updateWatermarkFontSize,
                    onUpdateWatermarkScale = viewModel::updateWatermarkScale,
                    onUpdateWatermarkOpacity = viewModel::updateWatermarkOpacity,
                    onUpdateWatermarkMargin = viewModel::updateWatermarkMargin,
                    onToggleWatermarkShadow = viewModel::toggleWatermarkShadow,
                    onToggleWatermarkGlow = viewModel::toggleWatermarkGlow
                )

                // Step 4: Complete MP4 Video Export & Validation
                Mp4ExportAndValidationSectionCard(
                    selectedSurah = uiState.selectedSurah,
                    startVerse = uiState.startVerse,
                    endVerse = uiState.endVerse,
                    reciterNameAr = uiState.selectedReciter.nameAr,
                    hasPhoneMedia = !uiState.phoneMedia.uriString.isNullOrBlank(),
                    audioDownloadState = uiState.audioDownloadState,
                    resolution = uiState.resolution,
                    aspectRatio = uiState.aspectRatio,
                    exportState = uiState.exportState,
                    onSelectResolution = viewModel::updateVideoResolution,
                    onStartMp4Export = viewModel::startMp4VideoExport,
                    onCancelExport = viewModel::cancelMp4Export,
                    onSaveVideoToGallery = { viewModel.saveVideoToPhoneGallery(context) },
                    onOpenVideo = { viewModel.openExportedVideo(context) },
                    onShareVideo = { viewModel.shareExportedVideo(context) }
                )

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}
