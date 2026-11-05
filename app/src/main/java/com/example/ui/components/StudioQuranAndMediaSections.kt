package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.QuranDataRepository
import com.example.model.AudioDownloadState
import com.example.model.MediaCropMode
import com.example.model.PhoneMediaBackground
import com.example.model.QuranVerse
import com.example.model.ReciterInfo
import com.example.model.SurahInfo
import com.example.ui.theme.AmiriQuranFontFamily
import com.example.ui.theme.LocalTasneemColors
import com.example.ui.theme.TasneemDeepCharcoal
import com.example.ui.theme.TasneemOrange
import com.example.ui.theme.TasneemSoftIvory
import kotlin.math.roundToInt

/**
 * Section 1: Quran Surah, Verse Range, Reciter, and Exact Selected-Verse Audio Download Card.
 */
@Composable
fun QuranAndReciterSectionCard(
    selectedSurah: SurahInfo,
    startVerse: Int,
    endVerse: Int,
    selectedVerses: List<QuranVerse>,
    activeVerseNumber: Int,
    selectedReciter: ReciterInfo,
    audioDownloadState: AudioDownloadState,
    onOpenSurahModal: () -> Unit,
    onUpdateVerseRange: (Int, Int) -> Unit,
    onOpenReciterModal: () -> Unit,
    onDownloadAudio: () -> Unit,
    onTapVerseToSeek: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalTasneemColors.current
    val totalSelected = (endVerse - startVerse + 1).coerceAtLeast(1)
    var showVersesList by remember { mutableStateOf(false) }

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = colors.cardBackground,
        border = androidx.compose.foundation.BorderStroke(1.dp, colors.borderSubtle),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            SectionHeaderBadge(
                stepNumber = "١",
                title = "الآيات والتلاوة الصوتية",
                subtitle = "اختيار السورة ونطاق الآيات والقارئ وتحميل تلاوة الآيات المختارة فقط"
            )

            // Surah Selector Button
            Text(
                text = "السورة القرآنية",
                style = MaterialTheme.typography.labelLarge,
                color = colors.textSecondary
            )

            Surface(
                shape = RoundedCornerShape(14.dp),
                color = colors.elevatedSurface,
                border = androidx.compose.foundation.BorderStroke(1.dp, TasneemOrange.copy(alpha = 0.5f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .clickable(onClick = onOpenSurahModal)
                    .testTag("open_surah_selector_button")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.MenuBook,
                            contentDescription = null,
                            tint = TasneemOrange,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "${QuranDataRepository.toArabicDigits(selectedSurah.number)} - سورة ${selectedSurah.nameAr}",
                            style = MaterialTheme.typography.titleMedium,
                            color = colors.textPrimary
                        )
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "${QuranDataRepository.toArabicDigits(selectedSurah.verseCount)} آية",
                            style = MaterialTheme.typography.labelMedium,
                            color = TasneemOrange
                        )
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = "تغيير السورة",
                            tint = colors.textSecondary
                        )
                    }
                }
            }

            // Starting Verse and Ending Verse Selectors
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                VerseStepperControl(
                    label = "من الآية",
                    currentVerse = startVerse,
                    minVerse = 1,
                    maxVerse = selectedSurah.verseCount,
                    onVerseChange = { newStart ->
                        val adjustedEnd = maxOf(newStart, endVerse)
                        onUpdateVerseRange(newStart, adjustedEnd)
                    },
                    testTagPrefix = "start_verse",
                    modifier = Modifier.weight(1f)
                )

                VerseStepperControl(
                    label = "إلى الآية",
                    currentVerse = endVerse,
                    minVerse = startVerse,
                    maxVerse = selectedSurah.verseCount,
                    onVerseChange = { newEnd ->
                        onUpdateVerseRange(startVerse, newEnd)
                    },
                    testTagPrefix = "end_verse",
                    modifier = Modifier.weight(1f)
                )
            }

            // Selected Verse Count Summary Bar
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = TasneemOrange.copy(alpha = 0.12f),
                border = androidx.compose.foundation.BorderStroke(1.dp, TasneemOrange.copy(alpha = 0.35f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "إجمالي الآيات المختارة: ${QuranDataRepository.toArabicDigits(totalSelected)} آيات",
                        style = MaterialTheme.typography.labelLarge,
                        color = TasneemOrange
                    )
                    Text(
                        text = "النطاق: ${QuranDataRepository.toArabicDigits(startVerse)} ← ${QuranDataRepository.toArabicDigits(endVerse)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.textSecondary
                    )
                }
            }

            // Expandable Preview of Selected Uthmani Verses
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = colors.elevatedSurface,
                border = androidx.compose.foundation.BorderStroke(1.dp, colors.borderSubtle),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { showVersesList = !showVersesList }
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "عرض النصوص العثمانية للآيات المختارة (${QuranDataRepository.toArabicDigits(selectedVerses.size)})",
                            style = MaterialTheme.typography.titleSmall,
                            color = colors.textPrimary
                        )
                        Icon(
                            imageVector = if (showVersesList) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = null,
                            tint = TasneemOrange
                        )
                    }

                    AnimatedVisibility(visible = showVersesList) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 10.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            selectedVerses.forEachIndexed { idx, verse ->
                                val isActive = verse.verseNumber == activeVerseNumber
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (isActive) TasneemOrange.copy(alpha = 0.16f) else colors.cardBackground,
                                    border = androidx.compose.foundation.BorderStroke(
                                        1.dp,
                                        if (isActive) TasneemOrange else colors.borderSubtle
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .clickable { onTapVerseToSeek(idx) }
                                ) {
                                    Text(
                                        text = "${verse.uthmaniText} ${QuranDataRepository.formatAyahSymbol(verse.verseNumber)}",
                                        fontFamily = AmiriQuranFontFamily,
                                        fontSize = 18.sp,
                                        lineHeight = 32.sp,
                                        color = if (isActive) TasneemOrange else colors.textPrimary,
                                        textAlign = TextAlign.Right,
                                        modifier = Modifier.padding(10.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Reciter Selector Button (20 Famous Reciters)
            Text(
                text = "القارئ (٢٠ قارئًا معتمدًا)",
                style = MaterialTheme.typography.labelLarge,
                color = colors.textSecondary
            )

            Surface(
                shape = RoundedCornerShape(14.dp),
                color = colors.elevatedSurface,
                border = androidx.compose.foundation.BorderStroke(1.dp, TasneemOrange.copy(alpha = 0.5f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .clickable(onClick = onOpenReciterModal)
                    .testTag("open_reciter_selector_button")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.RecordVoiceOver,
                            contentDescription = null,
                            tint = TasneemOrange,
                            modifier = Modifier.size(20.dp)
                        )
                        Column {
                            Text(
                                text = selectedReciter.nameAr,
                                style = MaterialTheme.typography.titleMedium,
                                color = colors.textPrimary
                            )
                            Text(
                                text = selectedReciter.styleAr,
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.textSecondary
                            )
                        }
                    }
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = "تغيير القارئ",
                        tint = colors.textSecondary
                    )
                }
            }

            // Exact Selected-Verse Audio Download Status Card (Section 07)
            SelectedVerseAudioDownloadStatusCard(
                totalSelectedVerses = totalSelected,
                audioDownloadState = audioDownloadState,
                onDownloadAudio = onDownloadAudio
            )
        }
    }
}

@Composable
private fun VerseStepperControl(
    label: String,
    currentVerse: Int,
    minVerse: Int,
    maxVerse: Int,
    onVerseChange: (Int) -> Unit,
    testTagPrefix: String,
    modifier: Modifier = Modifier
) {
    val colors = LocalTasneemColors.current
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = colors.textSecondary
        )
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = colors.elevatedSurface,
            border = androidx.compose.foundation.BorderStroke(1.dp, colors.borderSubtle),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 6.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(
                    onClick = { if (currentVerse > minVerse) onVerseChange(currentVerse - 1) },
                    enabled = currentVerse > minVerse,
                    modifier = Modifier
                        .size(36.dp)
                        .testTag("${testTagPrefix}_decrement")
                ) {
                    Icon(
                        imageVector = Icons.Default.Remove,
                        contentDescription = "إنقاص الآية",
                        tint = if (currentVerse > minVerse) TasneemOrange else colors.textSecondary.copy(alpha = 0.35f)
                    )
                }

                Text(
                    text = QuranDataRepository.toArabicDigits(currentVerse),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary
                )

                IconButton(
                    onClick = { if (currentVerse < maxVerse) onVerseChange(currentVerse + 1) },
                    enabled = currentVerse < maxVerse,
                    modifier = Modifier
                        .size(36.dp)
                        .testTag("${testTagPrefix}_increment")
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "زيادة الآية",
                        tint = if (currentVerse < maxVerse) TasneemOrange else colors.textSecondary.copy(alpha = 0.35f)
                    )
                }
            }
        }
    }
}

@Composable
private fun SelectedVerseAudioDownloadStatusCard(
    totalSelectedVerses: Int,
    audioDownloadState: AudioDownloadState,
    onDownloadAudio: () -> Unit
) {
    val colors = LocalTasneemColors.current
    var showDownloadedFiles by remember { mutableStateOf(false) }

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = colors.elevatedSurface,
        border = androidx.compose.foundation.BorderStroke(1.dp, colors.borderSubtle),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            when (audioDownloadState) {
                is AudioDownloadState.Idle -> {
                    Button(
                        onClick = onDownloadAudio,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = TasneemOrange,
                            contentColor = TasneemDeepCharcoal
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("download_selected_verses_audio_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.CloudDownload,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "تحميل صوت الآيات المختارة فقط (${QuranDataRepository.toArabicDigits(totalSelectedVerses)} آيات)",
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }

                is AudioDownloadState.Downloading -> {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        CircularProgressIndicator(
                            color = TasneemOrange,
                            strokeWidth = 2.5.dp,
                            modifier = Modifier.size(22.dp)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "جاري تحميل ملفات الآيات المختارة فقط...",
                                style = MaterialTheme.typography.titleSmall,
                                color = colors.textPrimary
                            )
                            Text(
                                text = "تم تحميل ${QuranDataRepository.toArabicDigits(audioDownloadState.downloadedCount)} من أصل ${QuranDataRepository.toArabicDigits(audioDownloadState.selectedVerseCount)} ملفات صوتية",
                                style = MaterialTheme.typography.bodySmall,
                                color = TasneemOrange
                            )
                        }
                    }
                    LinearProgressIndicator(
                        progress = { audioDownloadState.progressFraction },
                        color = TasneemOrange,
                        trackColor = colors.cardBackground,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                    )
                }

                is AudioDownloadState.Ready -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = TasneemOrange,
                                modifier = Modifier.size(22.dp)
                            )
                            Column {
                                Text(
                                    text = "التلاوة جاهزة للمعاينة والتصدير (${QuranDataRepository.toArabicDigits(audioDownloadState.segments.size)} ملفات آيات فقط)",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = colors.textPrimary
                                )
                                Text(
                                    text = "المدة الكاملة: ${formatMillis(audioDownloadState.totalDurationMs)} · " +
                                        if (audioDownloadState.hasWordLevelSync) "مزامنة الآيات والكلمات"
                                        else "مزامنة دقيقة على مستوى الآيات",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TasneemOrange
                                )
                            }
                        }

                        IconButton(
                            onClick = onDownloadAudio,
                            modifier = Modifier.testTag("reload_selected_audio_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "إعادة التحقق من الصوت",
                                tint = colors.textSecondary
                            )
                        }
                    }

                    // Expandable verified downloaded files list
                    Text(
                        text = if (showDownloadedFiles) "إخفاء قائمة الملفات الصوتية المحملة ▲"
                        else "عرض الملفات الصوتية المحملة للآيات المختارة (${QuranDataRepository.toArabicDigits(audioDownloadState.segments.size)}) ▼",
                        style = MaterialTheme.typography.labelMedium,
                        color = colors.textSecondary,
                        modifier = Modifier.clickable { showDownloadedFiles = !showDownloadedFiles }
                    )

                    AnimatedVisibility(visible = showDownloadedFiles) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.padding(top = 4.dp)
                        ) {
                            audioDownloadState.segments.forEach { seg ->
                                Text(
                                    text = "• الآية ${QuranDataRepository.toArabicDigits(seg.verseNumber)}: ${seg.localFilePath.substringAfterLast('/')} (${String.format("%.1f", seg.durationMs / 1000f)} ثانية)",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = colors.textSecondary
                                )
                            }
                        }
                    }
                }

                is AudioDownloadState.Error -> {
                    Text(
                        text = audioDownloadState.messageAr,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFFF8A65)
                    )
                    OutlinedButton(
                        onClick = onDownloadAudio,
                        border = androidx.compose.foundation.BorderStroke(1.dp, TasneemOrange),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = null,
                            tint = TasneemOrange,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "إعادة محاولة تحميل الآيات المختارة",
                            color = TasneemOrange,
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
            }
        }
    }
}

/**
 * Section 2: Phone Media Import Card (Strictly No Preloaded Background Library - Section 11).
 * Features two prominent actions:
 * - "اختيار صورة من الهاتف" (Choose Image from Phone)
 * - "اختيار فيديو من الهاتف" (Choose Video from Phone)
 * Plus cropping, scaling, positioning, brightness, dimming, and video looping controls.
 */
@Composable
fun PhoneMediaImportSectionCard(
    phoneMedia: PhoneMediaBackground,
    mediaImportMessageAr: String?,
    onChooseImageFromPhone: () -> Unit,
    onChooseVideoFromPhone: () -> Unit,
    onUpdateCropMode: (MediaCropMode) -> Unit,
    onUpdateScale: (Float) -> Unit,
    onUpdateOffset: (Float, Float) -> Unit,
    onUpdateBrightness: (Float) -> Unit,
    onUpdateDimLevel: (Float) -> Unit,
    onUpdateLoopVideo: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalTasneemColors.current

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = colors.cardBackground,
        border = androidx.compose.foundation.BorderStroke(1.dp, colors.borderSubtle),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            SectionHeaderBadge(
                stepNumber = "٢",
                title = "خلفية المقطع من الهاتف",
                subtitle = "اختر صورة أو فيديو مباشرة من هاتفك (تتم المعالجة محليًا بالكامل على جهازك)"
            )

            // Two prominent media selection actions mandated by Section 11
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = onChooseImageFromPhone,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (!phoneMedia.isVideo && !phoneMedia.uriString.isNullOrBlank())
                            TasneemOrange else colors.elevatedSurface,
                        contentColor = if (!phoneMedia.isVideo && !phoneMedia.uriString.isNullOrBlank())
                            TasneemDeepCharcoal else colors.textPrimary
                    ),
                    shape = RoundedCornerShape(14.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, TasneemOrange),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("choose_image_from_phone_button")
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(vertical = 6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Image,
                            contentDescription = null,
                            tint = if (!phoneMedia.isVideo && !phoneMedia.uriString.isNullOrBlank())
                                TasneemDeepCharcoal else TasneemOrange,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "اختيار صورة من الهاتف",
                            style = MaterialTheme.typography.labelLarge,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                Button(
                    onClick = onChooseVideoFromPhone,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (phoneMedia.isVideo && !phoneMedia.uriString.isNullOrBlank())
                            TasneemOrange else colors.elevatedSurface,
                        contentColor = if (phoneMedia.isVideo && !phoneMedia.uriString.isNullOrBlank())
                            TasneemDeepCharcoal else colors.textPrimary
                    ),
                    shape = RoundedCornerShape(14.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, TasneemOrange),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("choose_video_from_phone_button")
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(vertical = 6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.VideoLibrary,
                            contentDescription = null,
                            tint = if (phoneMedia.isVideo && !phoneMedia.uriString.isNullOrBlank())
                                TasneemDeepCharcoal else TasneemOrange,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "اختيار فيديو من الهاتف",
                            style = MaterialTheme.typography.labelLarge,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            if (!mediaImportMessageAr.isNullOrBlank()) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = TasneemOrange.copy(alpha = 0.14f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = mediaImportMessageAr,
                        style = MaterialTheme.typography.bodySmall,
                        color = TasneemSoftIvory,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                    )
                }
            }

            // Cropping & Framing Mode
            Text(
                text = "وضع الاقتصاص والتأطير",
                style = MaterialTheme.typography.labelLarge,
                color = colors.textSecondary
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MediaCropMode.entries.forEach { mode ->
                    val isSelected = phoneMedia.cropMode == mode
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (isSelected) TasneemOrange else colors.elevatedSurface,
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isSelected) TasneemOrange else colors.borderSubtle
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { onUpdateCropMode(mode) }
                    ) {
                        Text(
                            text = mode.labelAr,
                            style = MaterialTheme.typography.labelMedium,
                            color = if (isSelected) TasneemDeepCharcoal else colors.textPrimary,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(vertical = 10.dp, horizontal = 4.dp)
                        )
                    }
                }
            }

            // Background Dimming Slider (تعتيم الخلفية)
            LabeledSliderControl(
                label = "تعتيم الخلفية لوضوح الآيات",
                valueText = "${(phoneMedia.dimLevel * 100).roundToInt()}%",
                value = phoneMedia.dimLevel,
                valueRange = 0f..0.9f,
                onValueChange = onUpdateDimLevel
            )

            // Brightness Adjustment Slider (إضاءة الخلفية)
            LabeledSliderControl(
                label = "درجة سطوع وإضاءة الخلفية",
                valueText = "${(phoneMedia.brightness * 100).roundToInt()}%",
                value = phoneMedia.brightness,
                valueRange = -0.5f..0.5f,
                onValueChange = onUpdateBrightness
            )

            // Scale / Zoom Slider (تكبير وتصغير الخلفية)
            LabeledSliderControl(
                label = "تكبير وتصغير الخلفية",
                valueText = "${(phoneMedia.scale * 100).roundToInt()}%",
                value = phoneMedia.scale,
                valueRange = 0.5f..2.5f,
                onValueChange = onUpdateScale
            )

            // Horizontal & Vertical Offset Sliders
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                LabeledSliderControl(
                    label = "تحريك أفقي",
                    valueText = "${(phoneMedia.offsetX * 100).roundToInt()}%",
                    value = phoneMedia.offsetX,
                    valueRange = -1f..1f,
                    onValueChange = { onUpdateOffset(it, phoneMedia.offsetY) },
                    modifier = Modifier.weight(1f)
                )
                LabeledSliderControl(
                    label = "تحريك عمودي",
                    valueText = "${(phoneMedia.offsetY * 100).roundToInt()}%",
                    value = phoneMedia.offsetY,
                    valueRange = -1f..1f,
                    onValueChange = { onUpdateOffset(phoneMedia.offsetX, it) },
                    modifier = Modifier.weight(1f)
                )
            }

            // Video Looping Toggle when Video is selected
            if (phoneMedia.isVideo) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "تكرار فيديو الخلفية تلقائيًا",
                            style = MaterialTheme.typography.titleSmall,
                            color = colors.textPrimary
                        )
                        Text(
                            text = "إعادة تكرار فيديو الخلفية حتى تنتهي التلاوة المختارة بالكامل أثناء التصدير",
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.textSecondary
                        )
                    }
                    Switch(
                        checked = phoneMedia.loopVideoInExport,
                        onCheckedChange = onUpdateLoopVideo,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = TasneemDeepCharcoal,
                            checkedTrackColor = TasneemOrange
                        )
                    )
                }
            }
        }
    }
}

@Composable
fun SectionHeaderBadge(
    stepNumber: String,
    title: String,
    subtitle: String
) {
    val colors = LocalTasneemColors.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(TasneemOrange.copy(alpha = 0.18f))
                .border(1.5.dp, TasneemOrange, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = stepNumber,
                style = MaterialTheme.typography.labelLarge,
                color = TasneemOrange
            )
        }
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = colors.textPrimary
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = colors.textSecondary
            )
        }
    }
}

@Composable
fun LabeledSliderControl(
    label: String,
    valueText: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalTasneemColors.current
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = colors.textPrimary
            )
            Text(
                text = valueText,
                style = MaterialTheme.typography.labelMedium,
                color = TasneemOrange
            )
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            colors = SliderDefaults.colors(
                thumbColor = TasneemOrange,
                activeTrackColor = TasneemOrange,
                inactiveTrackColor = colors.elevatedSurface
            ),
            modifier = Modifier.height(28.dp)
        )
    }
}
