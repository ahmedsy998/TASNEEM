package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MovieCreation
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.SaveAlt
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.example.model.ArabicQuranFont
import com.example.model.AudioDownloadState
import com.example.model.QuranTextAlignment
import com.example.model.QuranTextStyleConfig
import com.example.model.SurahInfo
import com.example.model.TextColorOption
import com.example.model.VideoAspectRatio
import com.example.model.VideoExportState
import com.example.model.VideoResolution
import com.example.model.WatermarkConfig
import com.example.model.WatermarkPresetPosition
import com.example.ui.theme.LocalTasneemColors
import com.example.ui.theme.TasneemDeepCharcoal
import com.example.ui.theme.TasneemOrange
import com.example.ui.theme.TasneemSoftIvory
import kotlin.math.roundToInt

/**
 * Section 3: Arabic Quran Font Library, Text Appearance & Tasneem Watermark Card (Sections 10 & 13).
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TypographyAndWatermarkSectionCard(
    aspectRatio: VideoAspectRatio,
    textStyle: QuranTextStyleConfig,
    watermark: WatermarkConfig,
    onSelectAspectRatio: (VideoAspectRatio) -> Unit,
    onSelectFont: (ArabicQuranFont) -> Unit,
    onUpdateFontSize: (Float) -> Unit,
    onUpdateLineSpacing: (Float) -> Unit,
    onSelectTextColor: (TextColorOption) -> Unit,
    onSelectAlignment: (QuranTextAlignment) -> Unit,
    onToggleBold: (Boolean) -> Unit,
    onToggleSurahHeader: (Boolean) -> Unit,
    onToggleReciterName: (Boolean) -> Unit,
    onToggleTafsir: (Boolean) -> Unit,
    onToggleTranslation: (Boolean) -> Unit,
    onApplyWatermarkPreset: (WatermarkPresetPosition) -> Unit,
    onUpdateWatermarkSize: (Float) -> Unit,
    onUpdateWatermarkScale: (Float) -> Unit,
    onUpdateWatermarkOpacity: (Float) -> Unit,
    onUpdateWatermarkMargin: (Float) -> Unit,
    onToggleWatermarkShadow: (Boolean) -> Unit,
    onToggleWatermarkGlow: (Boolean) -> Unit,
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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            SectionHeaderBadge(
                stepNumber = "٣",
                title = "الخطوط القرآنية والتصميم وتوقيع تسنيم",
                subtitle = "اختر المقاس، الخط العربي المصحفي، مظهر الآيات، وخصائص علامة «تسنيم»"
            )

            // Video Aspect Ratio Selection (9:16, 16:9, 1:1)
            Text(
                text = "مقاس الفيديو",
                style = MaterialTheme.typography.labelLarge,
                color = colors.textSecondary
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                VideoAspectRatio.entries.forEach { ratio ->
                    val isSelected = ratio == aspectRatio
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) TasneemOrange else colors.elevatedSurface,
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isSelected) TasneemOrange else colors.borderSubtle
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onSelectAspectRatio(ratio) }
                            .testTag("aspect_ratio_${ratio.id}")
                    ) {
                        Text(
                            text = ratio.labelAr,
                            style = MaterialTheme.typography.labelMedium,
                            color = if (isSelected) TasneemDeepCharcoal else colors.textPrimary,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(vertical = 10.dp, horizontal = 4.dp)
                        )
                    }
                }
            }

            // Arabic Quran Font Library (8 Fonts with Real Quranic Arabic Preview)
            Text(
                text = "مكتبة الخطوط القرآنية العربية (معاينة حية للرسم والتشكيل)",
                style = MaterialTheme.typography.labelLarge,
                color = TasneemOrange
            )

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ArabicQuranFont.entries.forEach { font ->
                    val isSelected = font == textStyle.font
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = colors.elevatedSurface,
                        border = androidx.compose.foundation.BorderStroke(
                            width = if (isSelected) 2.dp else 1.dp,
                            color = if (isSelected) TasneemOrange else colors.borderSubtle
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .clickable { onSelectFont(font) }
                            .testTag("font_option_${font.id}")
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = font.displayNameAr,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = if (isSelected) TasneemOrange else colors.textPrimary
                                )
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = "مختار",
                                        tint = TasneemOrange,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                            // Real Arabic Quranic Preview for each font
                            Text(
                                text = "﴿ وَمِزَاجُهُۥ مِن تَسْنِيمٍ ۝٢٧ عَيْنًا يَشْرَبُ بِهَا ٱلْمُقَرَّبُونَ ﴾",
                                fontFamily = font.composeFontFamily,
                                fontSize = 19.sp,
                                lineHeight = 30.sp,
                                color = colors.textPrimary
                            )
                            Text(
                                text = font.descriptionAr,
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.textSecondary
                            )
                        }
                    }
                }
            }

            // Font Size & Line Spacing Sliders
            LabeledSliderControl(
                label = "حجم الخط القرآني",
                valueText = "${textStyle.fontSizeSp.roundToInt()} نقطة",
                value = textStyle.fontSizeSp,
                valueRange = 16f..44f,
                onValueChange = onUpdateFontSize
            )

            LabeledSliderControl(
                label = "تباعد الأسطر",
                valueText = "${(textStyle.lineSpacingMultiplier * 100).roundToInt()}%",
                value = textStyle.lineSpacingMultiplier,
                valueRange = 1.15f..2.2f,
                onValueChange = onUpdateLineSpacing
            )

            // Font Color Selector
            Text(
                text = "لون النص القرآني",
                style = MaterialTheme.typography.labelLarge,
                color = colors.textSecondary
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                TextColorOption.entries.forEach { colorOpt ->
                    val isSelected = colorOpt == textStyle.colorOption
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = colors.elevatedSurface,
                        border = androidx.compose.foundation.BorderStroke(
                            width = if (isSelected) 2.dp else 1.dp,
                            color = if (isSelected) TasneemOrange else colors.borderSubtle
                        ),
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .clickable { onSelectTextColor(colorOpt) }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(16.dp)
                                    .clip(CircleShape)
                                    .background(colorOpt.composeColor)
                                    .border(1.dp, Color.Gray, CircleShape)
                            )
                            Text(
                                text = colorOpt.labelAr,
                                style = MaterialTheme.typography.labelMedium,
                                color = colors.textPrimary
                            )
                        }
                    }
                }
            }

            // Text Alignment & Weight
            Text(
                text = "محاذاة النص وسماكة الخط",
                style = MaterialTheme.typography.labelLarge,
                color = colors.textSecondary
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                QuranTextAlignment.entries.forEach { alignOpt ->
                    val isSelected = alignOpt == textStyle.alignment
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
                            .clickable { onSelectAlignment(alignOpt) }
                    ) {
                        Text(
                            text = alignOpt.labelAr,
                            style = MaterialTheme.typography.labelMedium,
                            color = if (isSelected) TasneemDeepCharcoal else colors.textPrimary,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(vertical = 10.dp)
                        )
                    }
                }
            }

            // Checkboxes for Bold, Surah Header, Reciter Name, Tafsir, Translation
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                CompactCheckboxRow(
                    checked = textStyle.isBold,
                    onCheckedChange = onToggleBold,
                    title = "تفعيل الوزن العريض للخط (Bold)"
                )
                CompactCheckboxRow(
                    checked = textStyle.showSurahHeader,
                    onCheckedChange = onToggleSurahHeader,
                    title = "إظهار اسم السورة ورقم الآية أعلى المقطع"
                )
                CompactCheckboxRow(
                    checked = textStyle.showReciterName,
                    onCheckedChange = onToggleReciterName,
                    title = "إظهار اسم القارئ في الفيديو"
                )
                CompactCheckboxRow(
                    checked = textStyle.showTafsir,
                    onCheckedChange = onToggleTafsir,
                    title = "إظهار التفسير الميسر مع كل آية"
                )
                CompactCheckboxRow(
                    checked = textStyle.showTranslation,
                    onCheckedChange = onToggleTranslation,
                    title = "إظهار ترجمة معاني الآيات في الفيديو"
                )
            }

            // ==================================================
            // Tasneem Watermark ("تسنيم") Customization (Section 13)
            // ==================================================
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = colors.elevatedSurface,
                border = androidx.compose.foundation.BorderStroke(1.dp, TasneemOrange.copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "توقيع وعلامة «تسنيم» المائية",
                            style = MaterialTheme.typography.titleMedium,
                            color = TasneemOrange
                        )
                        Text(
                            text = "تسنيم",
                            style = MaterialTheme.typography.titleMedium,
                            color = TasneemSoftIvory
                        )
                    }
                    Text(
                        text = "يمكنك سحب علامة «تسنيم» مباشرة باللمس داخل شاشة البث الحي أو ضبطها بدقة من هنا (لا تحجب الآيات أبدًا).",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.textSecondary
                    )

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        WatermarkPresetPosition.entries.forEach { preset ->
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = colors.cardBackground,
                                border = androidx.compose.foundation.BorderStroke(1.dp, colors.borderSubtle),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { onApplyWatermarkPreset(preset) }
                            ) {
                                Text(
                                    text = preset.labelAr,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = colors.textPrimary,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }

                    LabeledSliderControl(
                        label = "حجم خط توقيع تسنيم",
                        valueText = "${watermark.fontSizeSp.roundToInt()} نقطة",
                        value = watermark.fontSizeSp,
                        valueRange = 11f..26f,
                        onValueChange = onUpdateWatermarkSize
                    )

                    LabeledSliderControl(
                        label = "مقياس تكبير العلامة المائية",
                        valueText = "${(watermark.scale * 100).roundToInt()}%",
                        value = watermark.scale,
                        valueRange = 0.6f..1.8f,
                        onValueChange = onUpdateWatermarkScale
                    )

                    LabeledSliderControl(
                        label = "شفافية العلامة المائية",
                        valueText = "${(watermark.opacity * 100).roundToInt()}%",
                        value = watermark.opacity,
                        valueRange = 0.2f..1.0f,
                        onValueChange = onUpdateWatermarkOpacity
                    )

                    LabeledSliderControl(
                        label = "الهوامش الآمنة من الحواف",
                        valueText = "${watermark.marginDp.roundToInt()} dp",
                        value = watermark.marginDp,
                        valueRange = 8f..48f,
                        onValueChange = onUpdateWatermarkMargin
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        CompactCheckboxRow(
                            checked = watermark.subtleShadow,
                            onCheckedChange = onToggleWatermarkShadow,
                            title = "ظل ناعم",
                            modifier = Modifier.weight(1f)
                        )
                        CompactCheckboxRow(
                            checked = watermark.elegantGlow,
                            onCheckedChange = onToggleWatermarkGlow,
                            title = "توهج أنيق",
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CompactCheckboxRow(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    title: String,
    modifier: Modifier = Modifier
) {
    val colors = LocalTasneemColors.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 2.dp)
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = CheckboxDefaults.colors(
                checkedColor = TasneemOrange,
                checkmarkColor = TasneemDeepCharcoal,
                uncheckedColor = colors.textSecondary
            )
        )
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium,
            color = colors.textPrimary
        )
    }
}

/**
 * Section 4: Complete MP4 Video Export & Validation Card (Sections 15 & 16).
 * Strictly MP4 only. Renders complete recitation duration and validates container, streams, and duration.
 */
@Composable
fun Mp4ExportAndValidationSectionCard(
    selectedSurah: SurahInfo,
    startVerse: Int,
    endVerse: Int,
    reciterNameAr: String,
    hasPhoneMedia: Boolean,
    audioDownloadState: AudioDownloadState,
    resolution: VideoResolution,
    aspectRatio: VideoAspectRatio,
    exportState: VideoExportState,
    onSelectResolution: (VideoResolution) -> Unit,
    onStartMp4Export: () -> Unit,
    onCancelExport: () -> Unit,
    onSaveVideoToGallery: () -> Unit,
    onOpenVideo: () -> Unit,
    onShareVideo: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalTasneemColors.current
    val totalVerses = (endVerse - startVerse + 1).coerceAtLeast(1)
    val actualDurationMs = (audioDownloadState as? AudioDownloadState.Ready)?.totalDurationMs ?: 0L

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = colors.cardBackground,
        border = androidx.compose.foundation.BorderStroke(1.5.dp, TasneemOrange.copy(alpha = 0.55f)),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            SectionHeaderBadge(
                stepNumber = "٤",
                title = "توليد وحفظ الفيديو الكامل (MP4 فقط)",
                subtitle = "إنتاج المقطع القرآني كاملاً بكامل مدة التلاوة المختارة مع فحص سلامة المسارات"
            )

            // Video Resolution Selection (1080p vs 720p)
            Text(
                text = "جودة ودقة التصدير (MP4)",
                style = MaterialTheme.typography.labelLarge,
                color = colors.textSecondary
            )
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                VideoResolution.entries.forEach { res ->
                    val isSelected = res == resolution
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = colors.elevatedSurface,
                        border = androidx.compose.foundation.BorderStroke(
                            width = if (isSelected) 2.dp else 1.dp,
                            color = if (isSelected) TasneemOrange else colors.borderSubtle
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .clickable { onSelectResolution(res) }
                            .testTag("resolution_option_${res.id}")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = res.labelAr,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = if (isSelected) TasneemOrange else colors.textPrimary
                                )
                                Text(
                                    text = res.subtitleAr,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = colors.textSecondary
                                )
                            }
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = "مختارة",
                                    tint = TasneemOrange
                                )
                            }
                        }
                    }
                }
            }

            // Export Summary Metadata Table (Section 16 requirement)
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
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "بيانات التصدير والتحقق قبل الرندرة",
                        style = MaterialTheme.typography.titleSmall,
                        color = TasneemOrange
                    )
                    ExportMetadataRow("صيغة الملف النهائي:", "MP4 فقط (H.264 + AAC)")
                    ExportMetadataRow("الدقة والمقاس:", "${resolution.id} · ${aspectRatio.labelAr}")
                    ExportMetadataRow("السورة المختارة:", "سورة ${selectedSurah.nameAr}")
                    ExportMetadataRow(
                        "نطاق الآيات:",
                        "من الآية ${QuranDataRepository.toArabicDigits(startVerse)} إلى ${QuranDataRepository.toArabicDigits(endVerse)} (${QuranDataRepository.toArabicDigits(totalVerses)} آيات)"
                    )
                    ExportMetadataRow("القارئ:", reciterNameAr)
                    ExportMetadataRow(
                        "مدة الصوت الفعلية الكاملة:",
                        if (actualDurationMs > 0L) "${formatMillis(actualDurationMs)} (كامل التلاوة دون اقتصاص)"
                        else "بانتظار تحميل صوت الآيات"
                    )
                    ExportMetadataRow(
                        "خلفية الهاتف:",
                        if (hasPhoneMedia) "جاهزة ومحددة من الهاتف" else "لم يتم اختيار صورة أو فيديو بعد"
                    )
                }
            }

            // Export Action & Real-time Progress / Validation Report
            when (exportState) {
                is VideoExportState.Idle -> {
                    Button(
                        onClick = onStartMp4Export,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = TasneemOrange,
                            contentColor = TasneemDeepCharcoal
                        ),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                            .testTag("export_complete_mp4_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.MovieCreation,
                            contentDescription = null,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "توليد وحفظ الفيديو الكامل بصيغة MP4",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                is VideoExportState.Rendering -> {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = colors.elevatedSurface,
                        border = androidx.compose.foundation.BorderStroke(1.dp, TasneemOrange),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "جاري تصدير الفيديو الكامل (MP4)...",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = TasneemOrange
                                )
                                Text(
                                    text = "${(exportState.progressFraction * 100).roundToInt()}%",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = colors.textPrimary
                                )
                            }
                            Text(
                                text = exportState.currentPhaseAr,
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.textSecondary
                            )
                            Text(
                                text = "المدة المعالجة: ${formatMillis(exportState.renderedDurationMs)} / ${formatMillis(exportState.totalDurationMs)}",
                                style = MaterialTheme.typography.labelMedium,
                                color = TasneemSoftIvory
                            )
                            LinearProgressIndicator(
                                progress = { exportState.progressFraction },
                                color = TasneemOrange,
                                trackColor = colors.cardBackground,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(4.dp))
                            )
                            OutlinedButton(
                                onClick = onCancelExport,
                                border = androidx.compose.foundation.BorderStroke(1.dp, colors.borderSubtle),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = null,
                                    tint = colors.textSecondary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("إلغاء التصدير", color = colors.textSecondary)
                            }
                        }
                    }
                }

                is VideoExportState.Completed -> {
                    val report = exportState.validationReport
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = TasneemOrange.copy(alpha = 0.14f),
                        border = androidx.compose.foundation.BorderStroke(1.5.dp, TasneemOrange),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Verified,
                                    contentDescription = null,
                                    tint = TasneemOrange,
                                    modifier = Modifier.size(24.dp)
                                )
                                Text(
                                    text = "اكتمل تصدير وفحص ملف الفيديو MP4 بنجاح",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = TasneemOrange
                                )
                            }

                            Text(
                                text = report.messageAr,
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.textPrimary
                            )

                            ExportMetadataRow(
                                "حالة حاوية MP4 والمسارين:",
                                "سليم (فيديو H.264 + صوت AAC كامل)"
                            )
                            ExportMetadataRow(
                                "مدة الفيديو المنتجة:",
                                "${formatMillis(report.actualDurationMs)} (متطابق مع التلاوة)"
                            )
                            ExportMetadataRow(
                                "حجم ملف MP4:",
                                String.format("%.2f ميجابايت", report.fileSizeBytes / (1024f * 1024f))
                            )

                            // Save, Open, Share Actions (Section 16)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = onSaveVideoToGallery,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = TasneemOrange,
                                        contentColor = TasneemDeepCharcoal
                                    ),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("save_exported_mp4_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.SaveAlt,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("حفظ الفيديو", style = MaterialTheme.typography.labelLarge)
                                }

                                Button(
                                    onClick = onOpenVideo,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = colors.cardBackground,
                                        contentColor = colors.textPrimary
                                    ),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("open_exported_mp4_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.OpenInNew,
                                        contentDescription = null,
                                        tint = TasneemOrange,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("فتح الفيديو", style = MaterialTheme.typography.labelLarge)
                                }

                                Button(
                                    onClick = onShareVideo,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = colors.cardBackground,
                                        contentColor = colors.textPrimary
                                    ),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("share_exported_mp4_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Share,
                                        contentDescription = null,
                                        tint = TasneemOrange,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("مشاركة", style = MaterialTheme.typography.labelLarge)
                                }
                            }
                        }
                    }
                }

                is VideoExportState.Failed -> {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = Color(0x33FF5252),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFF8A65)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = exportState.errorMessageAr,
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color(0xFFFFAB91)
                            )
                            Button(
                                onClick = onStartMp4Export,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = TasneemOrange,
                                    contentColor = TasneemDeepCharcoal
                                ),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("إعادة محاولة التصدير (MP4)", style = MaterialTheme.typography.labelLarge)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ExportMetadataRow(label: String, value: String) {
    val colors = LocalTasneemColors.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = colors.textSecondary
        )
        Text(
            text = value,
            style = MaterialTheme.typography.labelMedium,
            color = colors.textPrimary
        )
    }
}
