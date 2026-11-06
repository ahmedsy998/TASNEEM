package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MovieCreation
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Palette
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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.QuranDataRepository
import com.example.media.QuranTextAndLayoutEngine
import com.example.model.ArabicQuranFont
import com.example.model.AudioDownloadState
import com.example.model.QuranTextAlignment
import com.example.model.QuranTextStyleConfig
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
import com.example.ui.theme.LocalTasneemColors
import com.example.ui.theme.TasneemDeepCharcoal
import com.example.ui.theme.TasneemOrange
import com.example.ui.theme.TasneemSoftIvory
import kotlin.math.roundToInt

/**
 * Group 4: Arabic Fonts Library ("الخطوط العربية") - Requirement 12.
 * All 8 Arabic fonts load real full Arabic .ttf typefaces and visibly change the Quranic text.
 */
@Composable
fun ArabicFontsCollapsibleGroup(
    expanded: Boolean,
    onToggleExpanded: () -> Unit,
    textStyle: QuranTextStyleConfig,
    onSelectFont: (ArabicQuranFont) -> Unit,
    onResetFont: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalTasneemColors.current
    val mainComposeColor = Color(textStyle.mainTextColorHex or 0xFF000000L)
    val tashkeelComposeColor = Color(textStyle.tashkeelColorHex or 0xFF000000L)

    CollapsibleStudioGroupCard(
        groupNumberAr = "٤",
        titleAr = "الخطوط العربية",
        subtitleAr = textStyle.font.displayNameAr,
        expanded = expanded,
        onToggleExpanded = onToggleExpanded,
        onResetGroup = onResetFont,
        testTag = "collapsible_group_arabic_fonts",
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "اختر الخط العربي المصحفي (٨ خطوط عربية كاملة الحروف والتشكيل)",
                style = MaterialTheme.typography.labelLarge,
                color = TasneemOrange,
                modifier = Modifier.weight(1f)
            )
            SettingResetIconButton(
                contentDescription = "إعادة ضبط الخط إلى الافتراضي",
                onReset = onResetFont,
                testTag = "reset_font_button"
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            ArabicQuranFont.entries.forEach { font ->
                val isSelected = font == textStyle.font
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = colors.elevatedSurface,
                    border = BorderStroke(
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
                        verticalArrangement = Arrangement.spacedBy(6.dp)
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

                        // Real Arabic Quranic Preview rendered with this exact font & current Tashkeel color
                        val previewSample = remember(
                            textStyle.mainTextColorHex,
                            textStyle.enableIndependentTashkeelColor,
                            textStyle.tashkeelColorHex
                        ) {
                            QuranTextAndLayoutEngine.buildQuranAnnotatedString(
                                text = "﴿ وَمِزَاجُهُۥ مِن تَسْنِيمٍ ۝٢٧ عَيْنًا يَشْرَبُ بِهَا ٱلْمُقَرَّبُونَ ﴾",
                                mainColor = mainComposeColor,
                                enableIndependentTashkeel = textStyle.enableIndependentTashkeelColor,
                                tashkeelColor = tashkeelComposeColor
                            )
                        }

                        Text(
                            text = previewSample,
                            fontFamily = font.composeFontFamily,
                            fontSize = 21.sp,
                            lineHeight = 34.sp,
                            modifier = Modifier.fillMaxWidth()
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
    }
}

/**
 * Group 5: Quranic Text Styling ("النص القرآني") - Requirement 09 (Single-Line Display) & Typography.
 */
@Composable
fun QuranTextStylingCollapsibleGroup(
    expanded: Boolean,
    onToggleExpanded: () -> Unit,
    textStyle: QuranTextStyleConfig,
    onToggleSingleLineDisplay: (Boolean) -> Unit,
    onUpdateFontSize: (Float) -> Unit,
    onResetFontSize: () -> Unit,
    onUpdateLineSpacing: (Float) -> Unit,
    onResetLineSpacing: () -> Unit,
    onSelectAlignment: (QuranTextAlignment) -> Unit,
    onResetAlignment: () -> Unit,
    onToggleBold: (Boolean) -> Unit,
    onToggleTafsir: (Boolean) -> Unit,
    onToggleTranslation: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalTasneemColors.current

    CollapsibleStudioGroupCard(
        groupNumberAr = "٥",
        titleAr = "النص القرآني",
        subtitleAr = if (textStyle.singleLineDisplay) "عرض سطر واحد متزامن مع التلاوة · حجم ${textStyle.fontSizeSp.roundToInt()}"
        else "عرض متعدد الأسطر · حجم ${textStyle.fontSizeSp.roundToInt()}",
        expanded = expanded,
        onToggleExpanded = onToggleExpanded,
        onResetGroup = {
            onToggleSingleLineDisplay(true)
            onResetFontSize()
            onResetLineSpacing()
            onResetAlignment()
        },
        testTag = "collapsible_group_quran_text",
        modifier = modifier
    ) {
        // Requirement 09: Display Only the Currently Recited Text in ONE LINE
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = TasneemOrange.copy(alpha = 0.14f),
            border = BorderStroke(1.dp, TasneemOrange.copy(alpha = 0.5f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "عرض المقطع المتلو حاليًا في سطر واحد فقط",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = TasneemOrange
                    )
                    Text(
                        text = "إظهار العبارة المنطوقة فقط في سطر واحد أنيق يتقدم تلقائيًا مع صوت القارئ دون تكديس الأسطر",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.textPrimary
                    )
                }
                Switch(
                    checked = textStyle.singleLineDisplay,
                    onCheckedChange = onToggleSingleLineDisplay,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = TasneemDeepCharcoal,
                        checkedTrackColor = TasneemOrange
                    ),
                    modifier = Modifier.testTag("single_line_display_switch")
                )
            }
        }

        // Font Size & Line Spacing Sliders with individual reset buttons
        LabeledSliderControl(
            label = "حجم الخط القرآني",
            valueText = "${textStyle.fontSizeSp.roundToInt()} نقطة",
            value = textStyle.fontSizeSp,
            valueRange = 16f..46f,
            onValueChange = onUpdateFontSize,
            onReset = onResetFontSize
        )

        LabeledSliderControl(
            label = "تباعد الأسطر",
            valueText = "${(textStyle.lineSpacingMultiplier * 100).roundToInt()}%",
            value = textStyle.lineSpacingMultiplier,
            valueRange = 1.15f..2.2f,
            onValueChange = onUpdateLineSpacing,
            onReset = onResetLineSpacing
        )

        // Text Alignment Selector + Reset
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "محاذاة النص القرآني",
                style = MaterialTheme.typography.labelLarge,
                color = colors.textSecondary
            )
            SettingResetIconButton(
                contentDescription = "إعادة ضبط محاذاة النص",
                onReset = onResetAlignment
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            QuranTextAlignment.entries.forEach { alignOpt ->
                val isSelected = alignOpt == textStyle.alignment
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (isSelected) TasneemOrange else colors.elevatedSurface,
                    border = BorderStroke(1.dp, if (isSelected) TasneemOrange else colors.borderSubtle),
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

        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            CompactCheckboxRow(
                checked = textStyle.isBold,
                onCheckedChange = onToggleBold,
                title = "تفعيل الوزن العريض للخط (Bold)"
            )
            CompactCheckboxRow(
                checked = textStyle.showTafsir,
                onCheckedChange = onToggleTafsir,
                title = "إظهار التفسير الميسر أسفل الآية"
            )
            CompactCheckboxRow(
                checked = textStyle.showTranslation,
                onCheckedChange = onToggleTranslation,
                title = "إظهار ترجمة معاني الآيات في الفيديو"
            )
        }
    }
}

/**
 * Group 6: Independent Quranic Text & Diacritics (Tashkeel) Colors ("ألوان النص والتشكيل") - Requirement 03.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TextAndTashkeelColorsCollapsibleGroup(
    expanded: Boolean,
    onToggleExpanded: () -> Unit,
    textStyle: QuranTextStyleConfig,
    onUpdateMainTextColorHex: (Long) -> Unit,
    onResetMainTextColor: () -> Unit,
    onToggleIndependentTashkeel: (Boolean) -> Unit,
    onUpdateTashkeelColorHex: (Long) -> Unit,
    onResetTashkeelColor: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalTasneemColors.current
    var showCustomMainPicker by remember { mutableStateOf(false) }
    var showCustomTashkeelPicker by remember { mutableStateOf(false) }

    val mainComposeColor = Color(textStyle.mainTextColorHex or 0xFF000000L)
    val tashkeelComposeColor = Color(textStyle.tashkeelColorHex or 0xFF000000L)

    CollapsibleStudioGroupCard(
        groupNumberAr = "٦",
        titleAr = "ألوان النص والتشكيل",
        subtitleAr = if (textStyle.enableIndependentTashkeelColor) "تلوين مستقل للحركات والتشكيل مفعّل"
        else "لون موحد للنص والتشكيل",
        expanded = expanded,
        onToggleExpanded = onToggleExpanded,
        onResetGroup = {
            onResetMainTextColor()
            onResetTashkeelColor()
        },
        testTag = "collapsible_group_colors_tashkeel",
        modifier = modifier
    ) {
        // Live Uthmani Tashkeel Preview Box
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = TasneemDeepCharcoal,
            border = BorderStroke(1.dp, TasneemOrange.copy(alpha = 0.45f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            val annotatedSample = remember(
                textStyle.mainTextColorHex,
                textStyle.enableIndependentTashkeelColor,
                textStyle.tashkeelColorHex
            ) {
                QuranTextAndLayoutEngine.buildQuranAnnotatedString(
                    text = "بِسْمِ ٱللَّهِ ٱلرَّحْمَٰنِ ٱلرَّحِيمِ ﴿١﴾",
                    mainColor = mainComposeColor,
                    enableIndependentTashkeel = textStyle.enableIndependentTashkeelColor,
                    tashkeelColor = tashkeelComposeColor
                )
            }
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp)
            ) {
                Text(
                    text = annotatedSample,
                    fontFamily = textStyle.font.composeFontFamily,
                    fontSize = 24.sp,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "معاينة حية للون الحروف القرآنية ولون حركات التشكيل والضبط",
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.textSecondary
                )
            }
        }

        // 1. Main Quranic Text Color ("لون النص القرآني")
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "لون النص القرآني الأساسي",
                style = MaterialTheme.typography.titleSmall,
                color = colors.textPrimary
            )
            SettingResetIconButton(
                contentDescription = "إعادة ضبط لون النص القرآني",
                onReset = onResetMainTextColor,
                testTag = "reset_main_text_color_button"
            )
        }

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            TextColorOption.entries.forEach { colorOpt ->
                val isSelected = textStyle.mainTextColorHex == colorOpt.colorHex
                ColorSwatchChip(
                    label = colorOpt.labelAr,
                    color = colorOpt.composeColor,
                    isSelected = isSelected,
                    onClick = { onUpdateMainTextColorHex(colorOpt.colorHex) }
                )
            }
            Surface(
                shape = RoundedCornerShape(50),
                color = colors.elevatedSurface,
                border = BorderStroke(1.dp, TasneemOrange),
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .clickable { showCustomMainPicker = !showCustomMainPicker }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Palette,
                        contentDescription = null,
                        tint = TasneemOrange,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "لون مخصص",
                        style = MaterialTheme.typography.labelMedium,
                        color = TasneemOrange
                    )
                }
            }
        }

        if (showCustomMainPicker) {
            CustomRgbColorPickerCard(
                title = "تخصيص لون النص القرآني",
                currentColorHex = textStyle.mainTextColorHex,
                onColorSelected = onUpdateMainTextColorHex
            )
        }

        // 2. Independent Tashkeel Color Toggle & Picker ("تلوين التشكيل بشكل مستقل" / "لون التشكيل")
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
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "تلوين التشكيل بشكل مستقل",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = colors.textPrimary
                        )
                        Text(
                            text = "تخصيص لون الفتحة والضمة والكسرة والسكون والشدة والمدود وعلامات الوقف",
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.textSecondary
                        )
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SettingResetIconButton(
                            contentDescription = "إعادة ضبط لون التشكيل",
                            onReset = onResetTashkeelColor,
                            testTag = "reset_tashkeel_color_button"
                        )
                        Switch(
                            checked = textStyle.enableIndependentTashkeelColor,
                            onCheckedChange = onToggleIndependentTashkeel,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = TasneemDeepCharcoal,
                                checkedTrackColor = TasneemOrange
                            ),
                            modifier = Modifier.testTag("independent_tashkeel_switch")
                        )
                    }
                }

                if (textStyle.enableIndependentTashkeelColor) {
                    Text(
                        text = "لون التشكيل والحركات القرآنية",
                        style = MaterialTheme.typography.labelLarge,
                        color = TasneemOrange
                    )

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        TextColorOption.entries.forEach { colorOpt ->
                            val isSelected = textStyle.tashkeelColorHex == colorOpt.colorHex
                            ColorSwatchChip(
                                label = colorOpt.labelAr,
                                color = colorOpt.composeColor,
                                isSelected = isSelected,
                                onClick = { onUpdateTashkeelColorHex(colorOpt.colorHex) }
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = colors.cardBackground,
                            border = BorderStroke(1.dp, TasneemOrange),
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .clickable { showCustomTashkeelPicker = !showCustomTashkeelPicker }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Palette,
                                    contentDescription = null,
                                    tint = TasneemOrange,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "لون تشكيل مخصص",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = TasneemOrange
                                )
                            }
                        }
                    }

                    if (showCustomTashkeelPicker) {
                        CustomRgbColorPickerCard(
                            title = "تخصيص لون التشكيل القرآني",
                            currentColorHex = textStyle.tashkeelColorHex,
                            onColorSelected = onUpdateTashkeelColorHex
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ColorSwatchChip(
    label: String,
    color: Color,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val colors = LocalTasneemColors.current
    Surface(
        shape = RoundedCornerShape(50),
        color = colors.elevatedSurface,
        border = BorderStroke(
            width = if (isSelected) 2.dp else 1.dp,
            color = if (isSelected) TasneemOrange else colors.borderSubtle
        ),
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .clickable(onClick = onClick)
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
                    .background(color)
                    .border(1.dp, Color.Gray, CircleShape)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = colors.textPrimary
            )
        }
    }
}

@Composable
private fun CustomRgbColorPickerCard(
    title: String,
    currentColorHex: Long,
    onColorSelected: (Long) -> Unit
) {
    val colors = LocalTasneemColors.current
    val initialR = ((currentColorHex shr 16) and 0xFF).toFloat()
    val initialG = ((currentColorHex shr 8) and 0xFF).toFloat()
    val initialB = (currentColorHex and 0xFF).toFloat()

    var red by remember(currentColorHex) { mutableFloatStateOf(initialR) }
    var green by remember(currentColorHex) { mutableFloatStateOf(initialG) }
    var blue by remember(currentColorHex) { mutableFloatStateOf(initialB) }

    fun emitUpdatedColor(r: Float, g: Float, b: Float) {
        val hex = (0xFF000000L) or
            ((r.roundToInt().coerceIn(0, 255).toLong()) shl 16) or
            ((g.roundToInt().coerceIn(0, 255).toLong()) shl 8) or
            (b.roundToInt().coerceIn(0, 255).toLong())
        onColorSelected(hex)
    }

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = colors.cardBackground,
        border = BorderStroke(1.dp, TasneemOrange.copy(alpha = 0.5f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelLarge,
                    color = TasneemOrange
                )
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(Color(currentColorHex or 0xFF000000L))
                        .border(1.dp, Color.White, CircleShape)
                )
            }
            LabeledSliderControl(
                label = "الأحمر (R)",
                valueText = "${red.roundToInt()}",
                value = red,
                valueRange = 0f..255f,
                onValueChange = {
                    red = it
                    emitUpdatedColor(red, green, blue)
                }
            )
            LabeledSliderControl(
                label = "الأخضر (G)",
                valueText = "${green.roundToInt()}",
                value = green,
                valueRange = 0f..255f,
                onValueChange = {
                    green = it
                    emitUpdatedColor(red, green, blue)
                }
            )
            LabeledSliderControl(
                label = "الأزرق (B)",
                valueText = "${blue.roundToInt()}",
                value = blue,
                valueRange = 0f..255f,
                onValueChange = {
                    blue = it
                    emitUpdatedColor(red, green, blue)
                }
            )
        }
    }
}

/**
 * Group 7: Intelligent Automatic Verse Positioning & Scaling ("موضع وحجم الآيات") - Requirement 04.
 */
@Composable
fun VersePositionAndSizeCollapsibleGroup(
    expanded: Boolean,
    onToggleExpanded: () -> Unit,
    aspectRatio: VideoAspectRatio,
    textStyle: QuranTextStyleConfig,
    onUpdateTextPosition: (Float, Float) -> Unit,
    onRestoreAutoPositioning: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalTasneemColors.current

    CollapsibleStudioGroupCard(
        groupNumberAr = "٧",
        titleAr = "موضع وحجم الآيات",
        subtitleAr = if (textStyle.isAutoPositioned) "التموضع الذكي التلقائي مفعّل لمقاس ${aspectRatio.id}"
        else "موضع يدوي مخصص (${(textStyle.normalizedX * 100).roundToInt()}% , ${(textStyle.normalizedY * 100).roundToInt()}%)",
        expanded = expanded,
        onToggleExpanded = onToggleExpanded,
        onResetGroup = onRestoreAutoPositioning,
        testTag = "collapsible_group_verse_position",
        modifier = modifier
    ) {
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = colors.elevatedSurface,
            border = BorderStroke(1.dp, TasneemOrange.copy(alpha = 0.45f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoFixHigh,
                        contentDescription = null,
                        tint = TasneemOrange,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "محرك التموضع الذكي الآمن (${aspectRatio.labelAr})",
                        style = MaterialTheme.typography.titleSmall,
                        color = TasneemOrange
                    )
                }
                Text(
                    text = "يحسب التطبيق تلقائيًا أفضل موضع، وعرض، وحجم خط، وهوامش آمنة للآيات ومعلومات التلاوة لتجنب أزرار تيك توك وريلز وشورتس ويوتيوب.",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textSecondary
                )

                Button(
                    onClick = onRestoreAutoPositioning,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = TasneemOrange,
                        contentColor = TasneemDeepCharcoal
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("restore_auto_positioning_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoFixHigh,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "استعادة الضبط التلقائي الذكي للموضع والحجم",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            LabeledSliderControl(
                label = "الموضع الأفقي للآية (X)",
                valueText = "${(textStyle.normalizedX * 100).roundToInt()}%",
                value = textStyle.normalizedX,
                valueRange = 0.12f..0.88f,
                onValueChange = { x -> onUpdateTextPosition(x, textStyle.normalizedY) },
                onReset = { onUpdateTextPosition(0.50f, textStyle.normalizedY) },
                modifier = Modifier.weight(1f)
            )
            LabeledSliderControl(
                label = "الموضع العمودي للآية (Y)",
                valueText = "${(textStyle.normalizedY * 100).roundToInt()}%",
                value = textStyle.normalizedY,
                valueRange = 0.14f..0.86f,
                onValueChange = { y -> onUpdateTextPosition(textStyle.normalizedX, y) },
                onReset = { onUpdateTextPosition(textStyle.normalizedX, 0.48f) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

/**
 * Group 8: Transition Effects Between Verses & Phrases ("تأثيرات الانتقال") - Requirement 06.
 */
@Composable
fun TransitionEffectsCollapsibleGroup(
    expanded: Boolean,
    onToggleExpanded: () -> Unit,
    transitionConfig: VerseTransitionConfig,
    onSelectEffect: (TransitionEffectType) -> Unit,
    onSelectSpeedPreset: (TransitionSpeedPreset) -> Unit,
    onUpdateDurationMs: (Long) -> Unit,
    onResetTransitions: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalTasneemColors.current

    CollapsibleStudioGroupCard(
        groupNumberAr = "٨",
        titleAr = "تأثيرات الانتقال",
        subtitleAr = "${transitionConfig.effectType.titleAr} · ${transitionConfig.durationMs} مللي ثانية",
        expanded = expanded,
        onToggleExpanded = onToggleExpanded,
        onResetGroup = onResetTransitions,
        testTag = "collapsible_group_transitions",
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "نوع تأثير الانتقال بين الآيات والمقاطع (٧ تأثيرات)",
                style = MaterialTheme.typography.labelLarge,
                color = colors.textSecondary
            )
            SettingResetIconButton(
                contentDescription = "إعادة ضبط تأثيرات الانتقال",
                onReset = onResetTransitions,
                testTag = "reset_transitions_button"
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            TransitionEffectType.entries.forEach { effect ->
                val isSelected = transitionConfig.effectType == effect
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = colors.elevatedSurface,
                    border = BorderStroke(
                        width = if (isSelected) 2.dp else 1.dp,
                        color = if (isSelected) TasneemOrange else colors.borderSubtle
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onSelectEffect(effect) }
                        .testTag("transition_effect_${effect.id}")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = effect.titleAr,
                                style = MaterialTheme.typography.titleSmall,
                                color = if (isSelected) TasneemOrange else colors.textPrimary
                            )
                            Text(
                                text = effect.subtitleAr,
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.textSecondary
                            )
                        }
                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "مختار",
                                tint = TasneemOrange,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }

        // Transition Speed Presets
        Text(
            text = "سرعة الانتقال",
            style = MaterialTheme.typography.labelLarge,
            color = colors.textSecondary
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            TransitionSpeedPreset.entries.forEach { preset ->
                val isSelected = transitionConfig.speedPreset == preset &&
                    transitionConfig.durationMs == preset.durationMs
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (isSelected) TasneemOrange else colors.elevatedSurface,
                    border = BorderStroke(1.dp, if (isSelected) TasneemOrange else colors.borderSubtle),
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { onSelectSpeedPreset(preset) }
                ) {
                    Text(
                        text = "${preset.labelAr} (${preset.durationMs}ms)",
                        style = MaterialTheme.typography.labelMedium,
                        color = if (isSelected) TasneemDeepCharcoal else colors.textPrimary,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                    )
                }
            }
        }

        // Exact Transition Duration Slider
        LabeledSliderControl(
            label = "مدة تأثير الانتقال (مللي ثانية)",
            valueText = "${transitionConfig.durationMs} ms",
            value = transitionConfig.durationMs.toFloat(),
            valueRange = 80f..1000f,
            onValueChange = { onUpdateDurationMs(it.toLong()) },
            onReset = { onUpdateDurationMs(TransitionSpeedPreset.NORMAL.durationMs) }
        )
    }
}

/**
 * Group 10: Personal Signature and Watermark ("التوقيع الشخصي والعلامة المائية") - Requirement 10.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PersonalSignatureCollapsibleGroup(
    expanded: Boolean,
    onToggleExpanded: () -> Unit,
    watermark: WatermarkConfig,
    onToggleEnabled: (Boolean) -> Unit,
    onUpdateSignatureText: (String) -> Unit,
    onResetSignatureText: () -> Unit,
    onSelectSignatureFont: (ArabicQuranFont) -> Unit,
    onResetSignatureFont: () -> Unit,
    onSelectSignatureColorHex: (Long) -> Unit,
    onResetSignatureColor: () -> Unit,
    onApplyPreset: (WatermarkPresetPosition) -> Unit,
    onResetPosition: () -> Unit,
    onUpdateFontSize: (Float) -> Unit,
    onResetFontSize: () -> Unit,
    onUpdateScale: (Float) -> Unit,
    onResetScale: () -> Unit,
    onUpdateOpacity: (Float) -> Unit,
    onResetOpacity: () -> Unit,
    onUpdateMargin: (Float) -> Unit,
    onResetMargin: () -> Unit,
    onToggleShadow: (Boolean) -> Unit,
    onToggleGlow: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalTasneemColors.current

    CollapsibleStudioGroupCard(
        groupNumberAr = "١٠",
        titleAr = "التوقيع الشخصي والعلامة المائية",
        subtitleAr = if (watermark.enabled) "التوقيع الحالي: «${watermark.signatureText.ifBlank { "تسنيم" }}»"
        else "التوقيع الشخصي مخفي",
        expanded = expanded,
        onToggleExpanded = onToggleExpanded,
        onResetGroup = {
            onToggleEnabled(true)
            onResetSignatureText()
            onResetSignatureFont()
            onResetSignatureColor()
            onResetPosition()
            onResetFontSize()
            onResetScale()
            onResetOpacity()
        },
        testTag = "collapsible_group_personal_signature",
        modifier = modifier
    ) {
        // Show / Hide Toggle
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "إظهار التوقيع الشخصي في الفيديو",
                style = MaterialTheme.typography.titleSmall,
                color = colors.textPrimary
            )
            Switch(
                checked = watermark.enabled,
                onCheckedChange = onToggleEnabled,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = TasneemDeepCharcoal,
                    checkedTrackColor = TasneemOrange
                ),
                modifier = Modifier.testTag("signature_enabled_switch")
            )
        }

        if (watermark.enabled) {
            // Custom Signature Text Input (default "تسنيم")
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "نص التوقيع الشخصي (الافتراضي: تسنيم)",
                    style = MaterialTheme.typography.labelLarge,
                    color = colors.textSecondary
                )
                SettingResetIconButton(
                    contentDescription = "إعادة ضبط نص التوقيع إلى تسنيم",
                    onReset = onResetSignatureText,
                    testTag = "reset_signature_text_button"
                )
            }

            OutlinedTextField(
                value = watermark.signatureText,
                onValueChange = onUpdateSignatureText,
                placeholder = { Text("تسنيم") },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = TasneemOrange,
                    unfocusedBorderColor = colors.borderSubtle,
                    focusedContainerColor = colors.elevatedSurface,
                    unfocusedContainerColor = colors.elevatedSurface,
                    focusedTextColor = colors.textPrimary,
                    unfocusedTextColor = colors.textPrimary
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("personal_signature_text_input")
            )

            // Quick Suggestion Chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("تسنيم", "صدقة جارية", "قناة القرآن الكريم", "تلاوات خاشعة").forEach { suggestion ->
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = colors.elevatedSurface,
                        border = BorderStroke(1.dp, colors.borderSubtle),
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .clickable { onUpdateSignatureText(suggestion) }
                    ) {
                        Text(
                            text = suggestion,
                            style = MaterialTheme.typography.labelSmall,
                            color = TasneemOrange,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            // Signature Font Selector
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "خط التوقيع الشخصي",
                    style = MaterialTheme.typography.labelLarge,
                    color = colors.textSecondary
                )
                SettingResetIconButton(
                    contentDescription = "إعادة ضبط خط التوقيع",
                    onReset = onResetSignatureFont
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ArabicQuranFont.entries.forEach { font ->
                    val isSelected = watermark.font == font
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (isSelected) TasneemOrange else colors.elevatedSurface,
                        border = BorderStroke(1.dp, if (isSelected) TasneemOrange else colors.borderSubtle),
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { onSelectSignatureFont(font) }
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

            // Signature Text Color
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "لون نص التوقيع الشخصي",
                    style = MaterialTheme.typography.labelLarge,
                    color = colors.textSecondary
                )
                SettingResetIconButton(
                    contentDescription = "إعادة ضبط لون التوقيع",
                    onReset = onResetSignatureColor
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TextColorOption.entries.forEach { colorOpt ->
                    ColorSwatchChip(
                        label = colorOpt.labelAr,
                        color = colorOpt.composeColor,
                        isSelected = watermark.colorHex == colorOpt.colorHex,
                        onClick = { onSelectSignatureColorHex(colorOpt.colorHex) }
                    )
                }
            }

            // Preset Positions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "موضع التوقيع (يمكنك أيضًا سحبه باللمس في البث الحي)",
                    style = MaterialTheme.typography.labelLarge,
                    color = colors.textSecondary
                )
                SettingResetIconButton(
                    contentDescription = "إعادة ضبط موضع التوقيع",
                    onReset = onResetPosition
                )
            }

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                WatermarkPresetPosition.entries.forEach { preset ->
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = colors.elevatedSurface,
                        border = BorderStroke(1.dp, colors.borderSubtle),
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onApplyPreset(preset) }
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
                label = "حجم خط التوقيع",
                valueText = "${watermark.fontSizeSp.roundToInt()} نقطة",
                value = watermark.fontSizeSp,
                valueRange = 11f..28f,
                onValueChange = onUpdateFontSize,
                onReset = onResetFontSize
            )

            LabeledSliderControl(
                label = "مقياس تكبير التوقيع",
                valueText = "${(watermark.scale * 100).roundToInt()}%",
                value = watermark.scale,
                valueRange = 0.6f..1.8f,
                onValueChange = onUpdateScale,
                onReset = onResetScale
            )

            LabeledSliderControl(
                label = "شفافية التوقيع",
                valueText = "${(watermark.opacity * 100).roundToInt()}%",
                value = watermark.opacity,
                valueRange = 0.2f..1.0f,
                onValueChange = onUpdateOpacity,
                onReset = onResetOpacity
            )

            LabeledSliderControl(
                label = "الهوامش الآمنة من الحواف",
                valueText = "${watermark.marginDp.roundToInt()} dp",
                value = watermark.marginDp,
                valueRange = 8f..48f,
                onValueChange = onUpdateMargin,
                onReset = onResetMargin
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                CompactCheckboxRow(
                    checked = watermark.subtleShadow,
                    onCheckedChange = onToggleShadow,
                    title = "ظل ناعم",
                    modifier = Modifier.weight(1f)
                )
                CompactCheckboxRow(
                    checked = watermark.elegantGlow,
                    onCheckedChange = onToggleGlow,
                    title = "توهج أنيق",
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

/**
 * Group 11: Social Media Icons ("أيقونات التواصل الاجتماعي") - Requirement 10.
 * Displays real recognizable vector icons in Tasneem Orange (#E8872F) centered below the signature
 * without platform names or usernames in the video.
 */
@Composable
fun SocialMediaIconsCollapsibleGroup(
    expanded: Boolean,
    onToggleExpanded: () -> Unit,
    watermark: WatermarkConfig,
    onTogglePlatform: (SocialPlatform) -> Unit,
    onUpdateIconsSizeDp: (Float) -> Unit,
    onResetIconsSizeDp: () -> Unit,
    onUpdateIconsOffsetY: (Float) -> Unit,
    onResetSocialIcons: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalTasneemColors.current

    CollapsibleStudioGroupCard(
        groupNumberAr = "١١",
        titleAr = "أيقونات التواصل الاجتماعي",
        subtitleAr = if (watermark.selectedSocialPlatforms.isEmpty()) "اختياري: إظهار أيقونات المنصات أسفل التوقيع"
        else "مفعّل: ${watermark.selectedSocialPlatforms.size} أيقونات باللون البرتقالي أسفل التوقيع",
        expanded = expanded,
        onToggleExpanded = onToggleExpanded,
        onResetGroup = onResetSocialIcons,
        testTag = "collapsible_group_social_icons",
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "اختر المنصات لإظهار أيقوناتها البرتقالية (#E8872F) مباشرة أسفل التوقيع (أيقونات فقط بدون نصوص):",
                style = MaterialTheme.typography.bodySmall,
                color = colors.textSecondary,
                modifier = Modifier.weight(1f)
            )
            SettingResetIconButton(
                contentDescription = "إعادة ضبط أيقونات التواصل الاجتماعي",
                onReset = onResetSocialIcons,
                testTag = "reset_social_icons_button"
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SocialPlatform.entries.forEach { platform ->
                val isSelected = platform in watermark.selectedSocialPlatforms
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = if (isSelected) TasneemOrange.copy(alpha = 0.18f) else colors.elevatedSurface,
                    border = BorderStroke(
                        width = if (isSelected) 2.dp else 1.dp,
                        color = if (isSelected) TasneemOrange else colors.borderSubtle
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(14.dp))
                        .clickable { onTogglePlatform(platform) }
                        .testTag("social_platform_${platform.id}")
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.padding(vertical = 12.dp, horizontal = 4.dp)
                    ) {
                        SocialPlatformVectorPreviewIcon(
                            platform = platform,
                            modifier = Modifier.size(28.dp)
                        )
                        Text(
                            text = platform.labelAr,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isSelected) TasneemOrange else colors.textPrimary,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }

        if (watermark.selectedSocialPlatforms.isNotEmpty()) {
            LabeledSliderControl(
                label = "حجم أيقونات التواصل الاجتماعي",
                valueText = "${watermark.socialIconsSizeDp.roundToInt()} dp",
                value = watermark.socialIconsSizeDp,
                valueRange = 12f..34f,
                onValueChange = onUpdateIconsSizeDp,
                onReset = onResetIconsSizeDp
            )

            LabeledSliderControl(
                label = "المسافة العمودية أسفل التوقيع",
                valueText = "${(watermark.socialIconsOffsetY * 100).roundToInt()}%",
                value = watermark.socialIconsOffsetY,
                valueRange = -0.4f..0.4f,
                onValueChange = onUpdateIconsOffsetY,
                onReset = { onUpdateIconsOffsetY(0f) }
            )
        }
    }
}

@Composable
private fun SocialPlatformVectorPreviewIcon(
    platform: SocialPlatform,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val cx = w / 2f
        val cy = h / 2f
        val r = minOf(w, h) / 2f
        val stroke = Stroke(width = r * 0.18f, cap = StrokeCap.Round, join = StrokeJoin.Round)

        when (platform) {
            SocialPlatform.FACEBOOK -> {
                drawRoundRect(
                    color = TasneemOrange,
                    topLeft = Offset(cx - r * 0.78f, cy - r * 0.78f),
                    size = Size(r * 1.56f, r * 1.56f),
                    cornerRadius = CornerRadius(r * 0.36f, r * 0.36f),
                    style = stroke
                )
                val fPath = Path().apply {
                    moveTo(cx + r * 0.22f, cy - r * 0.45f)
                    lineTo(cx + r * 0.04f, cy - r * 0.45f)
                    cubicTo(cx - r * 0.12f, cy - r * 0.45f, cx - r * 0.14f, cy - r * 0.28f, cx - r * 0.14f, cy - r * 0.10f)
                    lineTo(cx - r * 0.14f, cy + r * 0.52f)
                    moveTo(cx - r * 0.32f, cy - r * 0.04f)
                    lineTo(cx + r * 0.20f, cy - r * 0.04f)
                }
                drawPath(fPath, color = TasneemOrange, style = stroke)
            }

            SocialPlatform.INSTAGRAM -> {
                drawRoundRect(
                    color = TasneemOrange,
                    topLeft = Offset(cx - r * 0.78f, cy - r * 0.78f),
                    size = Size(r * 1.56f, r * 1.56f),
                    cornerRadius = CornerRadius(r * 0.42f, r * 0.42f),
                    style = stroke
                )
                drawCircle(
                    color = TasneemOrange,
                    radius = r * 0.36f,
                    center = Offset(cx, cy),
                    style = stroke
                )
                drawCircle(
                    color = TasneemOrange,
                    radius = r * 0.10f,
                    center = Offset(cx + r * 0.42f, cy - r * 0.42f)
                )
            }

            SocialPlatform.TIKTOK -> {
                val notePath = Path().apply {
                    moveTo(cx + r * 0.12f, cy - r * 0.56f)
                    lineTo(cx + r * 0.12f, cy + r * 0.22f)
                    moveTo(cx + r * 0.12f, cy - r * 0.56f)
                    cubicTo(
                        cx + r * 0.18f, cy - r * 0.30f,
                        cx + r * 0.38f, cy - r * 0.18f,
                        cx + r * 0.54f, cy - r * 0.16f
                    )
                }
                drawPath(notePath, color = TasneemOrange, style = stroke)
                drawArc(
                    color = TasneemOrange,
                    startAngle = 35f,
                    sweepAngle = 295f,
                    useCenter = false,
                    topLeft = Offset(cx - r * 0.46f, cy - r * 0.04f),
                    size = Size(r * 0.58f, r * 0.58f),
                    style = stroke
                )
            }

            SocialPlatform.YOUTUBE -> {
                drawRoundRect(
                    color = TasneemOrange,
                    topLeft = Offset(cx - r * 0.85f, cy - r * 0.60f),
                    size = Size(r * 1.70f, r * 1.20f),
                    cornerRadius = CornerRadius(r * 0.32f, r * 0.32f),
                    style = stroke
                )
                val playPath = Path().apply {
                    moveTo(cx - r * 0.20f, cy - r * 0.28f)
                    lineTo(cx + r * 0.32f, cy)
                    lineTo(cx - r * 0.20f, cy + r * 0.28f)
                    close()
                }
                drawPath(playPath, color = TasneemOrange)
            }
        }
    }
}

/**
 * Group 12: Video Quality and Complete MP4 Export ("إعدادات الفيديو والجودة والتصدير").
 */
@Composable
fun VideoQualityAndExportCollapsibleGroup(
    expanded: Boolean,
    onToggleExpanded: () -> Unit,
    selectedSurah: SurahInfo,
    startVerse: Int,
    endVerse: Int,
    reciterNameAr: String,
    hasPhoneMedia: Boolean,
    audioDownloadState: AudioDownloadState,
    resolution: VideoResolution,
    aspectRatio: VideoAspectRatio,
    exportState: VideoExportState,
    onSelectAspectRatio: (VideoAspectRatio) -> Unit,
    onResetAspectRatio: () -> Unit,
    onSelectResolution: (VideoResolution) -> Unit,
    onResetResolution: () -> Unit,
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

    CollapsibleStudioGroupCard(
        groupNumberAr = "١٢",
        titleAr = "إعدادات الفيديو والجودة والتصدير",
        subtitleAr = "صيغة MP4 فقط · ${resolution.id} · مقاس ${aspectRatio.id}",
        expanded = expanded,
        onToggleExpanded = onToggleExpanded,
        onResetGroup = {
            onResetAspectRatio()
            onResetResolution()
        },
        testTag = "collapsible_group_export",
        modifier = modifier
    ) {
        // Video Aspect Ratio Selection (9:16, 16:9, 1:1)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "مقاس أبعاد الفيديو",
                style = MaterialTheme.typography.labelLarge,
                color = colors.textSecondary
            )
            SettingResetIconButton(
                contentDescription = "إعادة ضبط مقاس الفيديو",
                onReset = onResetAspectRatio
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            VideoAspectRatio.entries.forEach { ratio ->
                val isSelected = ratio == aspectRatio
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (isSelected) TasneemOrange else colors.elevatedSurface,
                    border = BorderStroke(
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

        // Video Resolution Selection (1080p vs 720p)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "جودة ودقة التصدير (MP4)",
                style = MaterialTheme.typography.labelLarge,
                color = colors.textSecondary
            )
            SettingResetIconButton(
                contentDescription = "إعادة ضبط دقة التصدير",
                onReset = onResetResolution
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            VideoResolution.entries.forEach { res ->
                val isSelected = res == resolution
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = colors.elevatedSurface,
                    border = BorderStroke(
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

        // Export Summary Metadata Table
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
                    "الخلفية المختارة:",
                    if (hasPhoneMedia) "جاهزة (من المكتبة أو الهاتف)" else "سيتم استخدام الخلفية الافتراضية"
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
                    border = BorderStroke(1.dp, TasneemOrange),
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
                            border = BorderStroke(1.dp, colors.borderSubtle),
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
                    border = BorderStroke(1.5.dp, TasneemOrange),
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
                    border = BorderStroke(1.dp, Color(0xFFFF8A65)),
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
