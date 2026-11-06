package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.data.QuranDataRepository
import com.example.media.QuranTextAndLayoutEngine
import com.example.model.ArabicQuranFont
import com.example.model.AudioDownloadState
import com.example.model.HeaderElementConfig
import com.example.model.MediaCropMode
import com.example.model.MushafBasmalaRuleMode
import com.example.model.MushafEditionConfig
import com.example.model.PhoneMediaBackground
import com.example.model.QuranTextAlignment
import com.example.model.RecitationHeaderConfig
import com.example.model.ReciterInfo
import com.example.model.SurahInfo
import com.example.model.TextColorOption
import com.example.ui.theme.LocalTasneemColors
import com.example.ui.theme.TasneemDeepCharcoal
import com.example.ui.theme.TasneemOrange
import com.example.ui.theme.TasneemSoftIvory
import kotlin.math.roundToInt

/**
 * Reusable Collapsible Settings Group Card (Requirement 13).
 * In Arabic RTL layout, the first item in a Row appears on the RIGHT (step badge + title),
 * and the trailing expand/collapse arrow icon appears on the LEFT.
 */
@Composable
fun CollapsibleStudioGroupCard(
    groupNumberAr: String,
    titleAr: String,
    subtitleAr: String,
    expanded: Boolean,
    onToggleExpanded: () -> Unit,
    onResetGroup: (() -> Unit)? = null,
    testTag: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val colors = LocalTasneemColors.current
    val arrowRotation by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        label = "arrow_rotation"
    )

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = colors.cardBackground,
        border = BorderStroke(
            width = if (expanded) 1.5.dp else 1.dp,
            color = if (expanded) TasneemOrange.copy(alpha = 0.55f) else colors.borderSubtle
        ),
        modifier = modifier
            .fillMaxWidth()
            .testTag(testTag)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Collapsible Group Header (Title on Right in RTL, Arrow on Left)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onToggleExpanded)
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f)
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
                            text = groupNumberAr,
                            style = MaterialTheme.typography.labelLarge,
                            color = TasneemOrange
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = titleAr,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = colors.textPrimary
                        )
                        Text(
                            text = subtitleAr,
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.textSecondary
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (onResetGroup != null) {
                        SettingResetIconButton(
                            contentDescription = "إعادة ضبط $titleAr",
                            onReset = onResetGroup
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(colors.elevatedSurface),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = if (expanded) "طي القسم" else "توسيع القسم",
                            tint = TasneemOrange,
                            modifier = Modifier
                                .size(22.dp)
                                .rotate(arrowRotation)
                        )
                    }
                }
            }

            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, bottom = 18.dp, top = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    content()
                }
            }
        }
    }
}

/**
 * Individual Setting Reset Button shaped like a curved return arrow (Requirement 11).
 * Restores only that specific setting to its default value with visual feedback.
 */
@Composable
fun SettingResetIconButton(
    contentDescription: String,
    onReset: () -> Unit,
    modifier: Modifier = Modifier,
    testTag: String? = null
) {
    val colors = LocalTasneemColors.current
    var pressCount by remember { mutableIntStateOf(0) }
    val rotation by animateFloatAsState(
        targetValue = pressCount * -360f,
        label = "reset_spin"
    )

    Surface(
        shape = CircleShape,
        color = colors.elevatedSurface,
        border = BorderStroke(1.dp, TasneemOrange.copy(alpha = 0.45f)),
        modifier = modifier
            .size(30.dp)
            .clip(CircleShape)
            .clickable {
                pressCount++
                onReset()
            }
            .then(if (testTag != null) Modifier.testTag(testTag) else Modifier)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = Icons.Default.Replay,
                contentDescription = contentDescription,
                tint = TasneemOrange,
                modifier = Modifier
                    .size(16.dp)
                    .rotate(rotation)
            )
        }
    }
}

/**
 * Group 1: Reciter and Recitation ("القارئ والتلاوة").
 */
@Composable
fun ReciterAndRecitationCollapsibleGroup(
    expanded: Boolean,
    onToggleExpanded: () -> Unit,
    selectedReciter: ReciterInfo,
    totalSelectedVerses: Int,
    audioDownloadState: AudioDownloadState,
    onOpenReciterModal: () -> Unit,
    onDownloadAudio: () -> Unit,
    onResetReciter: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalTasneemColors.current
    CollapsibleStudioGroupCard(
        groupNumberAr = "١",
        titleAr = "القارئ والتلاوة",
        subtitleAr = "${selectedReciter.nameAr} · تحميل صوت الآيات المختارة فقط",
        expanded = expanded,
        onToggleExpanded = onToggleExpanded,
        onResetGroup = onResetReciter,
        testTag = "collapsible_group_reciter",
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "القارئ المعتمد (٢٠ قارئًا شهيرًا)",
                style = MaterialTheme.typography.labelLarge,
                color = colors.textSecondary
            )
            SettingResetIconButton(
                contentDescription = "إعادة ضبط القارئ الافتراضي",
                onReset = onResetReciter,
                testTag = "reset_reciter_button"
            )
        }

        Surface(
            shape = RoundedCornerShape(14.dp),
            color = colors.elevatedSurface,
            border = BorderStroke(1.dp, TasneemOrange.copy(alpha = 0.5f)),
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

        SelectedVerseAudioDownloadStatusCard(
            totalSelectedVerses = totalSelectedVerses,
            audioDownloadState = audioDownloadState,
            onDownloadAudio = onDownloadAudio
        )
    }
}

/**
 * Group 2: Surah and Verse Selection ("اختيار السورة والآيات") - Requirements 01 & 02.
 * Includes direct numeric keyboard input for "من الآية" and "إلى الآية" (supporting both Arabic-Indic
 * and Western numerals) and removes the "عرض النصوص العثمانية للآيات المختارة" list box.
 */
@Composable
fun SurahAndVerseSelectionCollapsibleGroup(
    expanded: Boolean,
    onToggleExpanded: () -> Unit,
    selectedSurah: SurahInfo,
    startVerse: Int,
    endVerse: Int,
    onOpenSurahModal: () -> Unit,
    onUpdateVerseRange: (Int, Int) -> Unit,
    onResetVerseRange: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalTasneemColors.current
    val totalSelected = (endVerse - startVerse + 1).coerceAtLeast(1)
    var validationWarningAr by remember(selectedSurah.number, startVerse, endVerse) {
        mutableStateOf<String?>(null)
    }

    CollapsibleStudioGroupCard(
        groupNumberAr = "٢",
        titleAr = "اختيار السورة والآيات",
        subtitleAr = "سورة ${selectedSurah.nameAr} · الآيات ${QuranDataRepository.toArabicDigits(startVerse)} إلى ${QuranDataRepository.toArabicDigits(endVerse)}",
        expanded = expanded,
        onToggleExpanded = onToggleExpanded,
        onResetGroup = onResetVerseRange,
        testTag = "collapsible_group_surah_verses",
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "السورة القرآنية (١١٤ سورة كاملة)",
                style = MaterialTheme.typography.labelLarge,
                color = colors.textSecondary
            )
            SettingResetIconButton(
                contentDescription = "إعادة ضبط نطاق الآيات",
                onReset = {
                    validationWarningAr = null
                    onResetVerseRange()
                },
                testTag = "reset_verse_range_button"
            )
        }

        Surface(
            shape = RoundedCornerShape(14.dp),
            color = colors.elevatedSurface,
            border = BorderStroke(1.dp, TasneemOrange.copy(alpha = 0.5f)),
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

        // Requirement 01: Fully interactive and editable numeric input fields for "من الآية" and "إلى الآية"
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            EditableVerseNumberControl(
                label = "من الآية",
                currentVerse = startVerse,
                minVerse = 1,
                maxVerse = selectedSurah.verseCount,
                onVerseCommitted = { typedStart ->
                    val clampedStart = typedStart.coerceIn(1, selectedSurah.verseCount)
                    val adjustedEnd = if (clampedStart > endVerse) {
                        validationWarningAr = "تم ضبط «إلى الآية» تلقائيًا لتتوافق مع بداية النطاق (${QuranDataRepository.toArabicDigits(clampedStart)})."
                        clampedStart
                    } else {
                        validationWarningAr = null
                        endVerse
                    }
                    onUpdateVerseRange(clampedStart, adjustedEnd)
                },
                onValidationWarning = { warning -> validationWarningAr = warning },
                testTagPrefix = "start_verse",
                modifier = Modifier.weight(1f)
            )

            EditableVerseNumberControl(
                label = "إلى الآية",
                currentVerse = endVerse,
                minVerse = 1,
                maxVerse = selectedSurah.verseCount,
                onVerseCommitted = { typedEnd ->
                    val clampedEnd = typedEnd.coerceIn(1, selectedSurah.verseCount)
                    if (clampedEnd < startVerse) {
                        validationWarningAr = "تم ضبط «من الآية» تلقائيًا بحيث لا تتجاوز نهاية النطاق (${QuranDataRepository.toArabicDigits(clampedEnd)})."
                        onUpdateVerseRange(clampedEnd, clampedEnd)
                    } else {
                        validationWarningAr = null
                        onUpdateVerseRange(startVerse, clampedEnd)
                    }
                },
                onValidationWarning = { warning -> validationWarningAr = warning },
                testTagPrefix = "end_verse",
                modifier = Modifier.weight(1f)
            )
        }

        if (!validationWarningAr.isNullOrBlank()) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = TasneemOrange.copy(alpha = 0.16f),
                border = BorderStroke(1.dp, TasneemOrange),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = validationWarningAr!!,
                    style = MaterialTheme.typography.bodySmall,
                    color = TasneemSoftIvory,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                )
            }
        }

        // Selected Verse Count Summary Bar (Requirement 02: Uthmanic list display box removed)
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = TasneemOrange.copy(alpha = 0.12f),
            border = BorderStroke(1.dp, TasneemOrange.copy(alpha = 0.35f)),
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
    }
}

/**
 * Dedicated Madinah Mushaf Section ("إعدادات المصحف") — Section 04.
 * Default Edition: مصحف المدينة النبوية — مجمع الملك فهد لطباعة المصحف الشريف
 * Default Narration: رواية حفص عن عاصم
 */
@Composable
fun MadinahMushafSettingsCollapsibleGroup(
    expanded: Boolean,
    onToggleExpanded: () -> Unit,
    mushafConfig: MushafEditionConfig,
    onSelectBasmalaRuleMode: (MushafBasmalaRuleMode) -> Unit,
    onToggleWaqfSymbols: (Boolean) -> Unit,
    onResetMushafConfig: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalTasneemColors.current

    CollapsibleStudioGroupCard(
        groupNumberAr = "م",
        titleAr = "إعدادات المصحف",
        subtitleAr = "${mushafConfig.editionNameAr} · ${mushafConfig.narrationNameAr}",
        expanded = expanded,
        onToggleExpanded = onToggleExpanded,
        onResetGroup = onResetMushafConfig,
        testTag = "collapsible_group_madinah_mushaf",
        modifier = modifier
    ) {
        // 1. Official Edition & Narration Card
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = colors.elevatedSurface,
            border = BorderStroke(1.dp, TasneemOrange.copy(alpha = 0.55f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.MenuBook,
                            contentDescription = null,
                            tint = TasneemOrange,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "الطبعة والرواية المعتمدة",
                            style = MaterialTheme.typography.labelLarge,
                            color = TasneemOrange
                        )
                    }
                    SettingResetIconButton(
                        contentDescription = "إعادة ضبط إعدادات المصحف",
                        onReset = onResetMushafConfig,
                        testTag = "reset_mushaf_config_button"
                    )
                }

                Text(
                    text = mushafConfig.editionNameAr,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary
                )

                Text(
                    text = "الرواية: ${mushafConfig.narrationNameAr}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TasneemSoftIvory
                )

                Text(
                    text = "• الرسم القرآني: ${mushafConfig.scriptNameAr}\n" +
                        "• نظام العد والترقيم: ${mushafConfig.numberingSystemAr}\n" +
                        "• هيكل السور: ١١٤ سورة كاملة بعدد آياتها المحقق في مصحف المدينة\n" +
                        "• الضبط العثماني: حفظ الألف الخنجرية، همزة الوصل (ٱ)، علامات المد (ٓ)، السكون، الشدة، التنوين، وعلامات الوقف",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textSecondary
                )
            }
        }

        // 2. Basmala Handling Rules According to Madinah Mushaf & Audio Segments
        Text(
            text = "حكم البسملة ومطابقة التلاوة الصوتية",
            style = MaterialTheme.typography.labelLarge,
            color = colors.textSecondary
        )

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            MushafBasmalaRuleMode.entries.forEach { mode ->
                val isSelected = mushafConfig.basmalaRuleMode == mode
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (isSelected) TasneemOrange.copy(alpha = 0.16f) else colors.elevatedSurface,
                    border = BorderStroke(
                        width = if (isSelected) 1.5.dp else 1.dp,
                        color = if (isSelected) TasneemOrange else colors.borderSubtle
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onSelectBasmalaRuleMode(mode) }
                        .testTag("mushaf_basmala_mode_${mode.id}")
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = mode.titleAr,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) TasneemOrange else colors.textPrimary
                        )
                        Text(
                            text = mode.descriptionAr,
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.textSecondary
                        )
                    }
                }
            }
        }

        // 3. Quranic Waqf Symbols & Uthmanic Annotations Toggle
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "إظهار رموز الوقف والضبط المصحفي (ۖ ۗ ۚ)",
                    style = MaterialTheme.typography.titleSmall,
                    color = colors.textPrimary
                )
                Text(
                    text = "اعتماد مواضع الوقف الطبيعي في مصحف المدينة النبوية لتقسيم الآيات الطويلة دون بتر المعنى",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textSecondary
                )
            }
            Switch(
                checked = mushafConfig.showQuranicWaqfSymbols,
                onCheckedChange = onToggleWaqfSymbols,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = TasneemDeepCharcoal,
                    checkedTrackColor = TasneemOrange
                ),
                modifier = Modifier.testTag("mushaf_waqf_symbols_switch")
            )
        }
    }
}

/**
 * Editable Numeric Verse Field + Stepper Buttons (Requirement 01).
 * Opens the Android numeric keyboard on tap, accepts both Arabic-Indic (٠-٩) and Western (0-9) numerals,
 * allows selecting, deleting, and replacing digits, and validates against [minVerse..maxVerse].
 */
@Composable
private fun EditableVerseNumberControl(
    label: String,
    currentVerse: Int,
    minVerse: Int,
    maxVerse: Int,
    onVerseCommitted: (Int) -> Unit,
    onValidationWarning: (String?) -> Unit,
    testTagPrefix: String,
    modifier: Modifier = Modifier
) {
    val colors = LocalTasneemColors.current
    val focusManager = LocalFocusManager.current
    val focusRequester = remember { FocusRequester() }
    var isFocused by remember { mutableStateOf(false) }

    var textFieldValue by remember(currentVerse) {
        val initialStr = currentVerse.toString()
        mutableStateOf(
            TextFieldValue(
                text = initialStr,
                selection = TextRange(0, initialStr.length)
            )
        )
    }

    LaunchedEffect(currentVerse, isFocused) {
        if (!isFocused) {
            val synced = currentVerse.toString()
            if (textFieldValue.text != synced) {
                textFieldValue = TextFieldValue(synced, TextRange(synced.length))
            }
        }
    }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = "$label (١ - ${QuranDataRepository.toArabicDigits(maxVerse)})",
            style = MaterialTheme.typography.labelLarge,
            color = colors.textSecondary
        )

        Surface(
            shape = RoundedCornerShape(14.dp),
            color = colors.elevatedSurface,
            border = BorderStroke(
                width = if (isFocused) 1.5.dp else 1.dp,
                color = if (isFocused) TasneemOrange else colors.borderSubtle
            ),
            modifier = Modifier
                .fillMaxWidth()
                .clickable { focusRequester.requestFocus() }
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(
                    onClick = {
                        if (currentVerse > minVerse) {
                            onValidationWarning(null)
                            onVerseCommitted(currentVerse - 1)
                        }
                    },
                    enabled = currentVerse > minVerse,
                    modifier = Modifier
                        .size(34.dp)
                        .testTag("${testTagPrefix}_decrement")
                ) {
                    Icon(
                        imageVector = Icons.Default.Remove,
                        contentDescription = "إنقاص الآية",
                        tint = if (currentVerse > minVerse) TasneemOrange else colors.textSecondary.copy(alpha = 0.35f)
                    )
                }

                OutlinedTextField(
                    value = textFieldValue,
                    onValueChange = { newVal ->
                        // Accept both Arabic-Indic (٠-٩) and Western (0-9) digits
                        val normalizedDigits = QuranTextAndLayoutEngine.normalizeNumericInput(newVal.text).take(3)
                        textFieldValue = newVal.copy(
                            text = normalizedDigits,
                            selection = TextRange(normalizedDigits.length)
                        )
                        if (normalizedDigits.isNotEmpty()) {
                            val parsed = normalizedDigits.toIntOrNull()
                            if (parsed != null) {
                                when {
                                    parsed < 1 -> {
                                        onValidationWarning("أقل رقم للآية هو ١.")
                                    }
                                    parsed > maxVerse -> {
                                        onValidationWarning("سورة تحتوي على ${QuranDataRepository.toArabicDigits(maxVerse)} آية كحد أقصى.")
                                        onVerseCommitted(maxVerse)
                                    }
                                    else -> {
                                        onValidationWarning(null)
                                        onVerseCommitted(parsed)
                                    }
                                }
                            }
                        }
                    },
                    singleLine = true,
                    textStyle = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary,
                        textAlign = TextAlign.Center
                    ),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            val parsed = QuranTextAndLayoutEngine.normalizeNumericInput(textFieldValue.text)
                                .toIntOrNull() ?: currentVerse
                            val clamped = parsed.coerceIn(minVerse, maxVerse)
                            textFieldValue = TextFieldValue(clamped.toString(), TextRange(clamped.toString().length))
                            onVerseCommitted(clamped)
                            focusManager.clearFocus()
                        }
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color.Transparent,
                        unfocusedBorderColor = Color.Transparent,
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .focusRequester(focusRequester)
                        .onFocusChanged { focusState ->
                            val nowFocused = focusState.isFocused
                            if (nowFocused && !isFocused) {
                                // Select all digits on focus so user can immediately type to replace or edit
                                val txt = textFieldValue.text
                                textFieldValue = textFieldValue.copy(selection = TextRange(0, txt.length))
                            } else if (!nowFocused && isFocused) {
                                // Commit clamped value when losing focus
                                val parsed = QuranTextAndLayoutEngine.normalizeNumericInput(textFieldValue.text)
                                    .toIntOrNull() ?: currentVerse
                                val clamped = parsed.coerceIn(minVerse, maxVerse)
                                textFieldValue = TextFieldValue(clamped.toString(), TextRange(clamped.toString().length))
                                onVerseCommitted(clamped)
                            }
                            isFocused = nowFocused
                        }
                        .testTag("${testTagPrefix}_input")
                )

                IconButton(
                    onClick = {
                        if (currentVerse < maxVerse) {
                            onValidationWarning(null)
                            onVerseCommitted(currentVerse + 1)
                        }
                    },
                    enabled = currentVerse < maxVerse,
                    modifier = Modifier
                        .size(34.dp)
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

/**
 * Group 3: Independent Recitation Information Header Panel ("إعدادات معلومات التلاوة") - Requirement 08.
 * Independently controls:
 * A. Reciter Name ("اسم القارئ")
 * B. Surah Name ("اسم السورة")
 * C. Verse Number ("رقم الآية")
 */
@Composable
fun RecitationInfoHeaderCollapsibleGroup(
    expanded: Boolean,
    onToggleExpanded: () -> Unit,
    headerConfig: RecitationHeaderConfig,
    onUpdateSurahHeader: ((HeaderElementConfig) -> HeaderElementConfig) -> Unit,
    onResetSurahHeader: () -> Unit,
    onUpdateVerseNumberHeader: ((HeaderElementConfig) -> HeaderElementConfig) -> Unit,
    onResetVerseNumberHeader: () -> Unit,
    onUpdateReciterHeader: ((HeaderElementConfig) -> HeaderElementConfig) -> Unit,
    onResetReciterHeader: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalTasneemColors.current
    var selectedTab by remember { mutableIntStateOf(0) } // 0: القارئ, 1: السورة, 2: رقم الآية

    CollapsibleStudioGroupCard(
        groupNumberAr = "٣",
        titleAr = "إعدادات معلومات التلاوة",
        subtitleAr = "تحكم مستقل في اسم القارئ، واسم السورة، ورقم الآية (الموضع، الخط، الحجم، اللون، والشفافية)",
        expanded = expanded,
        onToggleExpanded = onToggleExpanded,
        onResetGroup = {
            onResetReciterHeader()
            onResetSurahHeader()
            onResetVerseNumberHeader()
        },
        testTag = "collapsible_group_recitation_info",
        modifier = modifier
    ) {
        // Sub-tabs for the 3 independent elements
        val tabs = listOf(
            "اسم القارئ" to headerConfig.reciterName.visible,
            "اسم السورة" to headerConfig.surahName.visible,
            "رقم الآية" to headerConfig.verseNumber.visible
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            tabs.forEachIndexed { idx, (title, isVisible) ->
                val isSelected = selectedTab == idx
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (isSelected) TasneemOrange else colors.elevatedSurface,
                    border = BorderStroke(1.dp, if (isSelected) TasneemOrange else colors.borderSubtle),
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { selectedTab = idx }
                        .testTag("header_tab_$idx")
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp)
                    ) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.labelLarge,
                            color = if (isSelected) TasneemDeepCharcoal else colors.textPrimary,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = if (isVisible) "ظاهر" else "مخفي",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isSelected) TasneemDeepCharcoal.copy(alpha = 0.8f) else TasneemOrange
                        )
                    }
                }
            }
        }

        when (selectedTab) {
            0 -> IndependentHeaderElementEditor(
                elementTitleAr = "اسم القارئ",
                config = headerConfig.reciterName,
                onUpdate = onUpdateReciterHeader,
                onReset = onResetReciterHeader,
                testTagPrefix = "reciter_header"
            )
            1 -> IndependentHeaderElementEditor(
                elementTitleAr = "اسم السورة",
                config = headerConfig.surahName,
                onUpdate = onUpdateSurahHeader,
                onReset = onResetSurahHeader,
                testTagPrefix = "surah_header"
            )
            2 -> IndependentHeaderElementEditor(
                elementTitleAr = "رقم الآية",
                config = headerConfig.verseNumber,
                onUpdate = onUpdateVerseNumberHeader,
                onReset = onResetVerseNumberHeader,
                testTagPrefix = "verse_num_header"
            )
        }
    }
}

@Composable
private fun IndependentHeaderElementEditor(
    elementTitleAr: String,
    config: HeaderElementConfig,
    onUpdate: ((HeaderElementConfig) -> HeaderElementConfig) -> Unit,
    onReset: () -> Unit,
    testTagPrefix: String
) {
    val colors = LocalTasneemColors.current
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = colors.elevatedSurface,
        border = BorderStroke(1.dp, colors.borderSubtle),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Show / Hide Toggle + Individual Reset Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "إظهار $elementTitleAr في الفيديو",
                        style = MaterialTheme.typography.titleSmall,
                        color = colors.textPrimary
                    )
                    SettingResetIconButton(
                        contentDescription = "إعادة ضبط $elementTitleAr",
                        onReset = onReset,
                        testTag = "${testTagPrefix}_reset"
                    )
                }
                Switch(
                    checked = config.visible,
                    onCheckedChange = { vis -> onUpdate { it.copy(visible = vis) } },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = TasneemDeepCharcoal,
                        checkedTrackColor = TasneemOrange
                    ),
                    modifier = Modifier.testTag("${testTagPrefix}_visibility_switch")
                )
            }

            if (config.visible) {
                // Font Family Horizontal Picker
                Text(
                    text = "نوع الخط لـ $elementTitleAr",
                    style = MaterialTheme.typography.labelLarge,
                    color = colors.textSecondary
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ArabicQuranFont.entries.forEach { font ->
                        val isSelected = config.font == font
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) TasneemOrange else colors.cardBackground,
                            border = BorderStroke(1.dp, if (isSelected) TasneemOrange else colors.borderSubtle),
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { onUpdate { it.copy(font = font) } }
                        ) {
                            Text(
                                text = font.displayNameAr.substringBefore(" ("),
                                fontFamily = font.composeFontFamily,
                                style = MaterialTheme.typography.labelMedium,
                                color = if (isSelected) TasneemDeepCharcoal else colors.textPrimary,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                            )
                        }
                    }
                }

                // Font Size Slider
                LabeledSliderControl(
                    label = "حجم خط $elementTitleAr",
                    valueText = "${config.fontSizeSp.roundToInt()} نقطة",
                    value = config.fontSizeSp,
                    valueRange = 11f..34f,
                    onValueChange = { sz -> onUpdate { it.copy(fontSizeSp = sz) } },
                    onReset = { onUpdate { it.copy(fontSizeSp = 18f) } }
                )

                // Color Options Row
                Text(
                    text = "لون $elementTitleAr",
                    style = MaterialTheme.typography.labelLarge,
                    color = colors.textSecondary
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TextColorOption.entries.forEach { colorOpt ->
                        val isSelected = config.colorHex == colorOpt.colorHex
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = colors.cardBackground,
                            border = BorderStroke(
                                width = if (isSelected) 2.dp else 1.dp,
                                color = if (isSelected) TasneemOrange else colors.borderSubtle
                            ),
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .clickable { onUpdate { it.copy(colorHex = colorOpt.colorHex) } }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(14.dp)
                                        .clip(CircleShape)
                                        .background(colorOpt.composeColor)
                                        .border(1.dp, Color.Gray, CircleShape)
                                )
                                Text(
                                    text = colorOpt.labelAr,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = colors.textPrimary
                                )
                            }
                        }
                    }
                }

                // Opacity Slider
                LabeledSliderControl(
                    label = "شفافية $elementTitleAr",
                    valueText = "${(config.opacity * 100).roundToInt()}%",
                    value = config.opacity,
                    valueRange = 0.2f..1.0f,
                    onValueChange = { op -> onUpdate { it.copy(opacity = op) } },
                    onReset = { onUpdate { it.copy(opacity = 0.92f) } }
                )

                // Alignment Selector
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    QuranTextAlignment.entries.forEach { alignOpt ->
                        val isSelected = config.alignment == alignOpt
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSelected) TasneemOrange else colors.cardBackground,
                            border = BorderStroke(1.dp, if (isSelected) TasneemOrange else colors.borderSubtle),
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { onUpdate { it.copy(alignment = alignOpt) } }
                        ) {
                            Text(
                                text = alignOpt.labelAr,
                                style = MaterialTheme.typography.labelMedium,
                                color = if (isSelected) TasneemDeepCharcoal else colors.textPrimary,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }
                    }
                }

                // Horizontal & Vertical Position Sliders + Margin
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    LabeledSliderControl(
                        label = "الموضع الأفقي (X)",
                        valueText = "${(config.normalizedX * 100).roundToInt()}%",
                        value = config.normalizedX,
                        valueRange = 0.08f..0.92f,
                        onValueChange = { x -> onUpdate { it.copy(normalizedX = x) } },
                        onReset = { onUpdate { it.copy(normalizedX = 0.50f) } },
                        modifier = Modifier.weight(1f)
                    )
                    LabeledSliderControl(
                        label = "الموضع العمودي (Y)",
                        valueText = "${(config.normalizedY * 100).roundToInt()}%",
                        value = config.normalizedY,
                        valueRange = 0.06f..0.92f,
                        onValueChange = { y -> onUpdate { it.copy(normalizedY = y) } },
                        onReset = onReset,
                        modifier = Modifier.weight(1f)
                    )
                }

                LabeledSliderControl(
                    label = "الهامش الآمن من الحواف",
                    valueText = "${config.marginDp.roundToInt()} dp",
                    value = config.marginDp,
                    valueRange = 8f..48f,
                    onValueChange = { m -> onUpdate { it.copy(marginDp = m) } },
                    onReset = { onUpdate { it.copy(marginDp = 16f) } }
                )
            }
        }
    }
}

/**
 * Group 9: Background Media Library & Phone Media Import ("الخلفيات والصور والفيديوهات") - Requirement 14.
 */
@Composable
fun BackgroundMediaLibraryCollapsibleGroup(
    expanded: Boolean,
    onToggleExpanded: () -> Unit,
    phoneMedia: PhoneMediaBackground,
    mediaImportMessageAr: String?,
    onOpenCuratedLibrary: () -> Unit,
    onChooseImageFromPhone: () -> Unit,
    onChooseVideoFromPhone: () -> Unit,
    onUpdateCropMode: (MediaCropMode) -> Unit,
    onUpdateScale: (Float) -> Unit,
    onResetScale: () -> Unit,
    onUpdateOffset: (Float, Float) -> Unit,
    onResetOffset: () -> Unit,
    onUpdateBrightness: (Float) -> Unit,
    onResetBrightness: () -> Unit,
    onUpdateDimLevel: (Float) -> Unit,
    onResetDimLevel: () -> Unit,
    onUpdateLoopVideo: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalTasneemColors.current

    CollapsibleStudioGroupCard(
        groupNumberAr = "٩",
        titleAr = "الخلفيات والصور والفيديوهات",
        subtitleAr = if (phoneMedia.fileName.isNotBlank()) phoneMedia.fileName
        else "مكتبة تسنيم (١٥ تصنيفًا طبيعيًا وإسلاميًا) أو الاستيراد من الهاتف",
        expanded = expanded,
        onToggleExpanded = onToggleExpanded,
        onResetGroup = {
            onResetDimLevel()
            onResetBrightness()
            onResetScale()
            onResetOffset()
        },
        testTag = "collapsible_group_backgrounds",
        modifier = modifier
    ) {
        // Prominent Curated Nature & Islamic Background Media Library Button (Requirement 14)
        Button(
            onClick = onOpenCuratedLibrary,
            colors = ButtonDefaults.buttonColors(
                containerColor = TasneemOrange,
                contentColor = TasneemDeepCharcoal
            ),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("open_curated_background_library_button")
        ) {
            Icon(
                imageVector = Icons.Default.AutoAwesome,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "تصفح مكتبة الخلفيات الطبيعية والإسلامية (صور وفيديوهات)",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold
            )
        }

        // Existing Phone Media Import Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick = onChooseImageFromPhone,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (!phoneMedia.isVideo && !phoneMedia.uriString.isNullOrBlank() && phoneMedia.curatedItemId == null)
                        TasneemOrange else colors.elevatedSurface,
                    contentColor = if (!phoneMedia.isVideo && !phoneMedia.uriString.isNullOrBlank() && phoneMedia.curatedItemId == null)
                        TasneemDeepCharcoal else colors.textPrimary
                ),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, TasneemOrange),
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
                        tint = if (!phoneMedia.isVideo && !phoneMedia.uriString.isNullOrBlank() && phoneMedia.curatedItemId == null)
                            TasneemDeepCharcoal else TasneemOrange,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
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
                    containerColor = if (phoneMedia.isVideo && !phoneMedia.uriString.isNullOrBlank() && phoneMedia.curatedItemId == null)
                        TasneemOrange else colors.elevatedSurface,
                    contentColor = if (phoneMedia.isVideo && !phoneMedia.uriString.isNullOrBlank() && phoneMedia.curatedItemId == null)
                        TasneemDeepCharcoal else colors.textPrimary
                ),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, TasneemOrange),
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
                        tint = if (phoneMedia.isVideo && !phoneMedia.uriString.isNullOrBlank() && phoneMedia.curatedItemId == null)
                            TasneemDeepCharcoal else TasneemOrange,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
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
                    border = BorderStroke(
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

        // Background Dimming Slider with individual reset button
        LabeledSliderControl(
            label = "تعتيم الخلفية لوضوح الآيات",
            valueText = "${(phoneMedia.dimLevel * 100).roundToInt()}%",
            value = phoneMedia.dimLevel,
            valueRange = 0f..0.9f,
            onValueChange = onUpdateDimLevel,
            onReset = onResetDimLevel
        )

        // Brightness Adjustment Slider with individual reset button
        LabeledSliderControl(
            label = "درجة سطوع وإضاءة الخلفية",
            valueText = "${(phoneMedia.brightness * 100).roundToInt()}%",
            value = phoneMedia.brightness,
            valueRange = -0.5f..0.5f,
            onValueChange = onUpdateBrightness,
            onReset = onResetBrightness
        )

        // Scale / Zoom Slider with individual reset button
        LabeledSliderControl(
            label = "تكبير وتصغير الخلفية",
            valueText = "${(phoneMedia.scale * 100).roundToInt()}%",
            value = phoneMedia.scale,
            valueRange = 0.5f..2.5f,
            onValueChange = onUpdateScale,
            onReset = onResetScale
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
                onReset = { onUpdateOffset(0f, phoneMedia.offsetY) },
                modifier = Modifier.weight(1f)
            )
            LabeledSliderControl(
                label = "تحريك عمودي",
                valueText = "${(phoneMedia.offsetY * 100).roundToInt()}%",
                value = phoneMedia.offsetY,
                valueRange = -1f..1f,
                onValueChange = { onUpdateOffset(phoneMedia.offsetX, it) },
                onReset = { onUpdateOffset(phoneMedia.offsetX, 0f) },
                modifier = Modifier.weight(1f)
            )
        }

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

@Composable
fun SelectedVerseAudioDownloadStatusCard(
    totalSelectedVerses: Int,
    audioDownloadState: AudioDownloadState,
    onDownloadAudio: () -> Unit
) {
    val colors = LocalTasneemColors.current
    var showDownloadedFiles by remember { mutableStateOf(false) }

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = colors.elevatedSurface,
        border = BorderStroke(1.dp, colors.borderSubtle),
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
                        border = BorderStroke(1.dp, TasneemOrange),
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
    onReset: (() -> Unit)? = null,
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
                color = colors.textPrimary,
                modifier = Modifier.weight(1f)
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = valueText,
                    style = MaterialTheme.typography.labelMedium,
                    color = TasneemOrange
                )
                if (onReset != null) {
                    SettingResetIconButton(
                        contentDescription = "إعادة ضبط $label",
                        onReset = onReset
                    )
                }
            }
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
