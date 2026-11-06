package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.UnfoldLess
import androidx.compose.material.icons.filled.UnfoldMore
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.media.PlaybackSyncState
import com.example.ui.components.ArabicFontsCollapsibleGroup
import com.example.ui.components.BackgroundMediaLibraryCollapsibleGroup
import com.example.ui.components.CuratedBackgroundLibraryDialog
import com.example.ui.components.MadinahMushafSettingsCollapsibleGroup
import com.example.ui.components.PersistentLivePreviewPanel
import com.example.ui.components.PersonalSignatureCollapsibleGroup
import com.example.ui.components.QuranTextStylingCollapsibleGroup
import com.example.ui.components.RecitationInfoHeaderCollapsibleGroup
import com.example.ui.components.ReciterAndRecitationCollapsibleGroup
import com.example.ui.components.SocialMediaIconsCollapsibleGroup
import com.example.ui.components.SurahAndVerseSelectionCollapsibleGroup
import com.example.ui.components.TextAndTashkeelColorsCollapsibleGroup
import com.example.ui.components.TransitionEffectsCollapsibleGroup
import com.example.ui.components.VersePositionAndSizeCollapsibleGroup
import com.example.ui.components.VideoQualityAndExportCollapsibleGroup
import com.example.ui.theme.LocalTasneemColors
import com.example.ui.theme.TasneemOrange
import com.example.viewmodel.StudioUiState
import com.example.viewmodel.TasneemScreen
import com.example.viewmodel.TasneemViewModel

/**
 * Tasneem Quranic Video Creation Studio Screen (Requirements 01..14).
 * Enforces a FIXED Persistent Live Preview ("البث الحي") at the top of the screen while
 * all collapsible editing groups scroll smoothly underneath it.
 */
@Composable
fun TasneemStudioScreen(
    uiState: StudioUiState,
    playbackSyncState: PlaybackSyncState,
    viewModel: TasneemViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val colors = LocalTasneemColors.current
    val isFullscreen = uiState.currentScreen == TasneemScreen.FULLSCREEN_PREVIEW

    BackHandler {
        if (isFullscreen) {
            viewModel.closeFullscreenPreview()
        } else {
            viewModel.navigateToHome()
        }
    }

    // Native Android Photo Picker Launchers for Phone Image & Phone Video
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

    // Collapsible Studio Groups (including dedicated Madinah Mushaf group 0)
    val expandedGroups = remember {
        mutableStateMapOf(
            0 to false, // إعدادات المصحف (مصحف المدينة النبوية — رواية حفص عن عاصم)
            1 to true,  // القارئ والتلاوة
            2 to true,  // اختيار السورة والآيات
            3 to false, // إعدادات معلومات التلاوة
            4 to false, // الخطوط العربية
            5 to false, // النص القرآني
            6 to false, // ألوان النص والتشكيل
            7 to false, // موضع وحجم الآيات
            8 to false, // تأثيرات الانتقال
            9 to false, // الخلفيات والصور والفيديوهات
            10 to false,// التوقيع الشخصي والعلامة المائية
            11 to false,// أيقونات التواصل الاجتماعي
            12 to true  // إعدادات الفيديو والجودة والتصدير
        )
    }

    fun isGroupExpanded(index: Int): Boolean = expandedGroups[index] == true
    fun toggleGroup(index: Int) {
        expandedGroups[index] = !(expandedGroups[index] ?: false)
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
            recitationHeader = uiState.recitationHeader,
            verseTransition = uiState.verseTransition,
            watermark = uiState.watermark,
            aspectRatio = uiState.aspectRatio,
            isFullscreen = isFullscreen,
            onTogglePlayPause = viewModel::togglePlayPause,
            onSeekToMs = viewModel::seekToOverallPosition,
            onUpdateWatermarkPosition = viewModel::updateWatermarkPosition,
            onUpdateTextPosition = viewModel::updateTextPosition,
            onUpdateSurahHeaderPosition = { x, y ->
                viewModel.updateSurahHeaderElement { it.copy(normalizedX = x, normalizedY = y) }
            },
            onUpdateVerseNumberPosition = { x, y ->
                viewModel.updateVerseNumberHeaderElement { it.copy(normalizedX = x, normalizedY = y) }
            },
            onUpdateReciterHeaderPosition = { x, y ->
                viewModel.updateReciterHeaderElement { it.copy(normalizedX = x, normalizedY = y) }
            },
            onToggleFullscreen = {
                if (isFullscreen) viewModel.closeFullscreenPreview()
                else viewModel.openFullscreenPreview()
            }
        )

        // 2. SCROLLABLE COLLAPSIBLE STUDIO GROUPS BELOW THE FIXED LIVE PREVIEW
        if (!isFullscreen) {
            val scrollState = rememberScrollState()
            val totalSelectedVerses = (uiState.endVerse - uiState.startVerse + 1).coerceAtLeast(1)

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(scrollState)
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Quick Expand All / Collapse All Header Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "أقسام إعدادات الاستوديو والمصحف",
                        style = MaterialTheme.typography.labelLarge,
                        color = colors.textSecondary
                    )
                    val allExpanded = (0..12).all { expandedGroups[it] == true }
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = colors.elevatedSurface,
                        border = BorderStroke(1.dp, colors.borderSubtle),
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .clickable {
                                val target = !allExpanded
                                for (i in 0..12) expandedGroups[i] = target
                            }
                            .testTag("toggle_all_collapsible_groups_button")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = if (allExpanded) Icons.Default.UnfoldLess else Icons.Default.UnfoldMore,
                                contentDescription = null,
                                tint = TasneemOrange,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = if (allExpanded) "طي جميع الأقسام" else "فتح جميع الأقسام",
                                style = MaterialTheme.typography.labelSmall,
                                color = TasneemOrange
                            )
                        }
                    }
                }

                // Group 1: القارئ والتلاوة (Reciter and Recitation)
                ReciterAndRecitationCollapsibleGroup(
                    expanded = isGroupExpanded(1),
                    onToggleExpanded = { toggleGroup(1) },
                    selectedReciter = uiState.selectedReciter,
                    totalSelectedVerses = totalSelectedVerses,
                    audioDownloadState = uiState.audioDownloadState,
                    onOpenReciterModal = { viewModel.setReciterSelectorVisible(true) },
                    onDownloadAudio = viewModel::downloadSelectedVersesAudio,
                    onResetReciter = viewModel::resetReciterToDefault
                )

                // Group 2: اختيار السورة والآيات (Surah and Verse Selection)
                SurahAndVerseSelectionCollapsibleGroup(
                    expanded = isGroupExpanded(2),
                    onToggleExpanded = { toggleGroup(2) },
                    selectedSurah = uiState.selectedSurah,
                    startVerse = uiState.startVerse,
                    endVerse = uiState.endVerse,
                    onOpenSurahModal = { viewModel.setSurahSelectorVisible(true) },
                    onUpdateVerseRange = viewModel::updateVerseRange,
                    onResetVerseRange = viewModel::resetVerseRange
                )

                // Dedicated Madinah Mushaf Section ("إعدادات المصحف" — Section 04)
                MadinahMushafSettingsCollapsibleGroup(
                    expanded = isGroupExpanded(0),
                    onToggleExpanded = { toggleGroup(0) },
                    mushafConfig = uiState.mushafConfig,
                    onSelectBasmalaRuleMode = viewModel::updateMushafBasmalaRuleMode,
                    onToggleWaqfSymbols = viewModel::toggleMushafWaqfSymbols,
                    onResetMushafConfig = viewModel::resetMushafConfig
                )

                // Group 3: إعدادات معلومات التلاوة (Recitation Information Header)
                RecitationInfoHeaderCollapsibleGroup(
                    expanded = isGroupExpanded(3),
                    onToggleExpanded = { toggleGroup(3) },
                    headerConfig = uiState.recitationHeader,
                    onUpdateSurahHeader = viewModel::updateSurahHeaderElement,
                    onResetSurahHeader = viewModel::resetSurahHeaderElement,
                    onUpdateVerseNumberHeader = viewModel::updateVerseNumberHeaderElement,
                    onResetVerseNumberHeader = viewModel::resetVerseNumberHeaderElement,
                    onUpdateReciterHeader = viewModel::updateReciterHeaderElement,
                    onResetReciterHeader = viewModel::resetReciterHeaderElement
                )

                // Group 4: الخطوط العربية (Arabic Fonts)
                ArabicFontsCollapsibleGroup(
                    expanded = isGroupExpanded(4),
                    onToggleExpanded = { toggleGroup(4) },
                    textStyle = uiState.textStyle,
                    onSelectFont = viewModel::updateArabicFont,
                    onResetFont = viewModel::resetArabicFont
                )

                // Group 5: النص القرآني (Quranic Text Styling)
                QuranTextStylingCollapsibleGroup(
                    expanded = isGroupExpanded(5),
                    onToggleExpanded = { toggleGroup(5) },
                    textStyle = uiState.textStyle,
                    onToggleSingleLineDisplay = viewModel::toggleSingleLineDisplay,
                    onUpdateFontSize = viewModel::updateFontSizeSp,
                    onResetFontSize = viewModel::resetFontSizeSp,
                    onUpdateLineSpacing = viewModel::updateLineSpacing,
                    onResetLineSpacing = viewModel::resetLineSpacing,
                    onSelectAlignment = viewModel::updateTextAlignment,
                    onResetAlignment = viewModel::resetTextAlignment,
                    onToggleBold = viewModel::updateTextBold,
                    onToggleTafsir = viewModel::toggleShowTafsir,
                    onToggleTranslation = viewModel::toggleShowTranslation
                )

                // Group 6: ألوان النص والتشكيل (Text and Tashkeel Colors)
                TextAndTashkeelColorsCollapsibleGroup(
                    expanded = isGroupExpanded(6),
                    onToggleExpanded = { toggleGroup(6) },
                    textStyle = uiState.textStyle,
                    onUpdateMainTextColorHex = viewModel::updateMainTextColorHex,
                    onResetMainTextColor = viewModel::resetMainTextColor,
                    onToggleIndependentTashkeel = viewModel::toggleIndependentTashkeelColor,
                    onUpdateTashkeelColorHex = viewModel::updateTashkeelColorHex,
                    onResetTashkeelColor = viewModel::resetTashkeelColor
                )

                // Group 7: موضع وحجم الآيات (Verse Position and Size)
                VersePositionAndSizeCollapsibleGroup(
                    expanded = isGroupExpanded(7),
                    onToggleExpanded = { toggleGroup(7) },
                    aspectRatio = uiState.aspectRatio,
                    textStyle = uiState.textStyle,
                    onUpdateTextPosition = viewModel::updateTextPosition,
                    onRestoreAutoPositioning = viewModel::restoreAutomaticPositioning
                )

                // Group 8: تأثيرات الانتقال (Transition Effects)
                TransitionEffectsCollapsibleGroup(
                    expanded = isGroupExpanded(8),
                    onToggleExpanded = { toggleGroup(8) },
                    transitionConfig = uiState.verseTransition,
                    onSelectEffect = viewModel::updateTransitionEffect,
                    onSelectSpeedPreset = viewModel::updateTransitionSpeedPreset,
                    onUpdateDurationMs = viewModel::updateTransitionDurationMs,
                    onResetTransitions = viewModel::resetTransitionSettings
                )

                // Group 9: الخلفيات والصور والفيديوهات (Background Media Library)
                BackgroundMediaLibraryCollapsibleGroup(
                    expanded = isGroupExpanded(9),
                    onToggleExpanded = { toggleGroup(9) },
                    phoneMedia = uiState.phoneMedia,
                    mediaImportMessageAr = uiState.mediaImportMessageAr,
                    onOpenCuratedLibrary = { viewModel.setCuratedLibraryModalVisible(true) },
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
                    onResetScale = viewModel::resetMediaScale,
                    onUpdateOffset = viewModel::updateMediaOffset,
                    onResetOffset = viewModel::resetMediaOffset,
                    onUpdateBrightness = viewModel::updateMediaBrightness,
                    onResetBrightness = viewModel::resetMediaBrightness,
                    onUpdateDimLevel = viewModel::updateMediaDimLevel,
                    onResetDimLevel = viewModel::resetMediaDimLevel,
                    onUpdateLoopVideo = viewModel::updateVideoLoopInExport
                )

                // Group 10: التوقيع الشخصي والعلامة المائية (Personal Signature and Watermark)
                PersonalSignatureCollapsibleGroup(
                    expanded = isGroupExpanded(10),
                    onToggleExpanded = { toggleGroup(10) },
                    watermark = uiState.watermark,
                    onToggleEnabled = viewModel::toggleWatermarkEnabled,
                    onUpdateSignatureText = viewModel::updateSignatureText,
                    onResetSignatureText = viewModel::resetSignatureText,
                    onSelectSignatureFont = viewModel::updateSignatureFont,
                    onResetSignatureFont = viewModel::resetSignatureFont,
                    onSelectSignatureColorHex = viewModel::updateSignatureColorHex,
                    onResetSignatureColor = viewModel::resetSignatureColor,
                    onApplyPreset = viewModel::applyWatermarkPreset,
                    onResetPosition = viewModel::resetWatermarkPosition,
                    onUpdateFontSize = viewModel::updateWatermarkFontSize,
                    onResetFontSize = viewModel::resetWatermarkFontSize,
                    onUpdateScale = viewModel::updateWatermarkScale,
                    onResetScale = viewModel::resetWatermarkScale,
                    onUpdateOpacity = viewModel::updateWatermarkOpacity,
                    onResetOpacity = viewModel::resetWatermarkOpacity,
                    onUpdateMargin = viewModel::updateWatermarkMargin,
                    onResetMargin = viewModel::resetWatermarkMargin,
                    onToggleShadow = viewModel::toggleWatermarkShadow,
                    onToggleGlow = viewModel::toggleWatermarkGlow
                )

                // Group 11: أيقونات التواصل الاجتماعي (Social Media Icons)
                SocialMediaIconsCollapsibleGroup(
                    expanded = isGroupExpanded(11),
                    onToggleExpanded = { toggleGroup(11) },
                    watermark = uiState.watermark,
                    onTogglePlatform = viewModel::toggleSocialPlatform,
                    onUpdateIconsSizeDp = viewModel::updateSocialIconsSizeDp,
                    onResetIconsSizeDp = viewModel::resetSocialIconsSizeDp,
                    onUpdateIconsOffsetY = viewModel::updateSocialIconsOffsetY,
                    onResetSocialIcons = viewModel::resetSocialIconsSettings
                )

                // Group 12: إعدادات الفيديو والجودة والتصدير (Video Quality and Export Settings)
                VideoQualityAndExportCollapsibleGroup(
                    expanded = isGroupExpanded(12),
                    onToggleExpanded = { toggleGroup(12) },
                    selectedSurah = uiState.selectedSurah,
                    startVerse = uiState.startVerse,
                    endVerse = uiState.endVerse,
                    reciterNameAr = uiState.selectedReciter.nameAr,
                    hasPhoneMedia = !uiState.phoneMedia.uriString.isNullOrBlank(),
                    audioDownloadState = uiState.audioDownloadState,
                    resolution = uiState.resolution,
                    aspectRatio = uiState.aspectRatio,
                    exportState = uiState.exportState,
                    onSelectAspectRatio = viewModel::updateAspectRatio,
                    onResetAspectRatio = viewModel::resetAspectRatio,
                    onSelectResolution = viewModel::updateVideoResolution,
                    onResetResolution = viewModel::resetVideoResolution,
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

    // Curated Nature & Islamic Background Media Library Dialog (Requirement 14)
    if (uiState.showCuratedLibraryModal) {
        CuratedBackgroundLibraryDialog(
            repository = viewModel.curatedMediaRepository,
            selectedItemId = uiState.phoneMedia.curatedItemId,
            isDownloading = uiState.isDownloadingCuratedMedia,
            onSelectItem = viewModel::selectCuratedBackgroundItem,
            onDismiss = { viewModel.setCuratedLibraryModalVisible(false) }
        )
    }
}
