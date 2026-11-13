package com.example.ui.components

import android.graphics.Paint
import android.text.Layout
import android.text.StaticLayout
import android.text.TextDirectionHeuristics
import android.text.TextPaint
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.QuranFontManager
import com.example.model.FontQuranCompatibilityLevel
import com.example.model.QuranFontValidationReport
import com.example.model.QuranTextStyleConfig
import com.example.ui.theme.LocalTasneemColors
import com.example.ui.theme.TasneemDeepCharcoal
import com.example.ui.theme.TasneemOrange
import kotlin.math.max

/**
 * Renders vocalized Quranic Arabic text using Android's native HarfBuzz StaticLayout engine,
 * applying the exact Typeface (built-in or custom imported .TTF/.OTF), intelligent Quranic
 * fallback spans, and independent Tashkeel colors with generous vertical anti-clipping headroom.
 */
@Composable
fun NativeQuranFontPreviewBox(
    sampleText: String,
    textStyle: QuranTextStyleConfig,
    fontSizeSp: Float = 21f,
    minHeightDp: Dp = 54.dp,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val density = LocalDensity.current

    val mainColorInt = (textStyle.mainTextColorHex or 0xFF000000L).toInt()
    val tashkeelColorInt = (textStyle.tashkeelColorHex or 0xFF000000L).toInt()

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(minHeightDp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val canvasWidth = size.width.toInt().coerceAtLeast(64)
            val canvasHeight = size.height
            val textSizePx = with(density) { fontSizeSp.dp.toPx() }

            val resolved = QuranFontManager.resolvePhraseTypographyWithFallback(
                context = context,
                phraseText = sampleText,
                textConfig = textStyle,
                mainColorInt = mainColorInt,
                tashkeelColorInt = tashkeelColorInt
            )

            val paint = TextPaint(Paint.ANTI_ALIAS_FLAG or Paint.SUBPIXEL_TEXT_FLAG).apply {
                typeface = resolved.effectiveBaseTypeface
                textSize = textSizePx
                color = mainColorInt
                isFakeBoldText = textStyle.isBold
            }

            // Auto-scale down slightly if sampleText is wider than preview card so it never clips horizontally
            var desiredW = max(paint.measureText(sampleText), Layout.getDesiredWidth(resolved.spannable, paint))
            val safeWidth = canvasWidth * 0.94f
            val minPx = textSizePx * 0.55f
            while (desiredW > safeWidth && paint.textSize > minPx) {
                paint.textSize *= 0.93f
                desiredW = max(paint.measureText(sampleText), Layout.getDesiredWidth(resolved.spannable, paint))
            }

            val builder = StaticLayout.Builder
                .obtain(resolved.spannable, 0, resolved.spannable.length, paint, canvasWidth)
                .setAlignment(Layout.Alignment.ALIGN_CENTER)
                .setTextDirection(TextDirectionHeuristics.RTL)
                .setMaxLines(2)
                .setIncludePad(true)
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                builder.setUseLineSpacingFromFallbacks(true)
            }
            val layout = builder.build()

            drawIntoCanvas { composeCanvas ->
                val nativeCanvas = composeCanvas.nativeCanvas
                nativeCanvas.save()
                val dy = ((canvasHeight - layout.height) / 2f).coerceAtLeast(2f)
                nativeCanvas.translate(0f, dy)
                layout.draw(nativeCanvas)
                nativeCanvas.restore()
            }
        }
    }
}

/**
 * Intelligent Quranic Font Validation Dialog (Sections 4, 5, 6).
 * Displays OpenType table inspection results (cmap, GSUB, GPOS Mark-to-Base & Mark-to-Mark)
 * and renders all 10 vocalized Quranic validation samples using the selected font.
 */
@Composable
fun QuranFontValidationDialog(
    report: QuranFontValidationReport,
    textStyle: QuranTextStyleConfig,
    onToggleFallback: (Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    val colors = LocalTasneemColors.current
    val ot = report.openTypeReport

    val badgeColor = when (report.compatibilityLevel) {
        FontQuranCompatibilityLevel.FULLY_VERIFIED_QURANIC -> Color(0xFF4CAF50)
        FontQuranCompatibilityLevel.REQUIRES_VISUAL_VERIFICATION_OR_FALLBACK -> TasneemOrange
        FontQuranCompatibilityLevel.PARTIAL_ARABIC_INCOMPLETE_TASHKEEL -> Color(0xFFFF8A65)
        FontQuranCompatibilityLevel.INCOMPATIBLE_NO_ARABIC -> Color(0xFFE53935)
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(22.dp),
            color = colors.cardBackground,
            border = BorderStroke(1.5.dp, TasneemOrange.copy(alpha = 0.7f)),
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .height(680.dp)
                .testTag("quran_font_validation_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Header
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
                            imageVector = Icons.Default.Verified,
                            contentDescription = null,
                            tint = TasneemOrange,
                            modifier = Modifier.size(24.dp)
                        )
                        Column {
                            Text(
                                text = "مختبر فحص التشكيل القرآني للخط",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = TasneemOrange
                            )
                            Text(
                                text = report.fontDisplayName,
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.textSecondary
                            )
                        }
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("close_font_validation_dialog_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "إغلاق",
                            tint = colors.textPrimary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // 1. Overall Compatibility Banner
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = badgeColor.copy(alpha = 0.14f),
                        border = BorderStroke(1.dp, badgeColor),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = if (report.isFullyQuranCompatible) Icons.Default.CheckCircle else Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = badgeColor,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = report.compatibilityLevel.badgeTitleAr,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = badgeColor
                                )
                            }
                            Text(
                                text = report.summaryMessageAr,
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.textPrimary
                            )
                            if (!report.warningMessageAr.isNullOrBlank()) {
                                Text(
                                    text = report.warningMessageAr,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFFFFCC80)
                                )
                            }
                        }
                    }

                    // 2. Fallback Switch Inside Validation Lab
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = colors.elevatedSurface,
                        border = BorderStroke(1.dp, colors.borderSubtle),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "استخدام خط احتياطي للتشكيل القرآني",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = TasneemOrange
                                )
                                Text(
                                    text = "تعويض الرموز أو الحركات غير المدعومة تلقائيًا لمنع انفصال التشكيل أو ظهور مربعات فارغة",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = colors.textSecondary
                                )
                            }
                            Switch(
                                checked = textStyle.enableQuranicFontFallback,
                                onCheckedChange = onToggleFallback,
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = TasneemDeepCharcoal,
                                    checkedTrackColor = TasneemOrange
                                )
                            )
                        }
                    }

                    // 3. OpenType Binary Table Inspection Summary (Section 4)
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = colors.elevatedSurface,
                        border = BorderStroke(1.dp, colors.borderSubtle),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "فحص جداول OpenType والقدرات الطباعية للخط:",
                                style = MaterialTheme.typography.titleSmall,
                                color = TasneemOrange
                            )
                            OpenTypeMetricRow("صيغة الملف واسم العائلة:", "${ot.format.labelAr} · ${ot.internalFamilyName}")
                            OpenTypeMetricRow(
                                "تغطية الحروف العربية الأساسية:",
                                "${ot.basicArabicLetterCount} / ${ot.totalBasicArabicLetters} (${(ot.basicArabicCoverageRatio * 100).toInt()}%)"
                            )
                            OpenTypeMetricRow(
                                "تغطية حركات التشكيل الأساسية:",
                                "${ot.coreTashkeelCount} / ${ot.totalCoreTashkeelMarks} (${(ot.coreTashkeelCoverageRatio * 100).toInt()}%)"
                            )
                            OpenTypeMetricRow(
                                "تغطية رموز الوقف والضبط العثماني:",
                                "${ot.quranicSymbolCount} / ${ot.totalQuranicSymbols} (${(ot.quranicSymbolCoverageRatio * 100).toInt()}%)"
                            )
                            OpenTypeMetricRow(
                                "جدول اتصال الحروف والتركيبات (GSUB):",
                                if (ot.hasArabicJoiningFeatures) "مدعوم (init / medi / fina / liga)" else "غير مكتمل"
                            )
                            OpenTypeMetricRow(
                                "ربط الحركة بالحرف (GPOS Mark-to-Base):",
                                if (ot.hasMarkToBaseGpos && !report.hasDetachedMarks) "مدعوم وسليم" else "غير مدعوم (يعتمد على الخط الاحتياطي)"
                            )
                            OpenTypeMetricRow(
                                "تراكب الشدة مع الحركات (GPOS Mark-to-Mark):",
                                if (ot.hasMarkToMarkGpos) "مدعوم وسليم" else "غير مدعوم (يعتمد على الخط الاحتياطي)"
                            )
                        }
                    }

                    // 4. 10 Authentic Vocalized Quranic Test Samples (Section 5)
                    Text(
                        text = "معاينة وفحص ١٠ نماذج قرآنية مشكولة بالكامل:",
                        style = MaterialTheme.typography.titleSmall,
                        color = TasneemOrange
                    )

                    report.sampleResults.forEach { sample ->
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = TasneemDeepCharcoal,
                            border = BorderStroke(
                                width = 1.dp,
                                color = if (sample.passed) TasneemOrange.copy(alpha = 0.45f) else Color(0xFFFF8A65)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
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
                                        text = sample.titleAr,
                                        style = MaterialTheme.typography.labelLarge,
                                        color = TasneemOrange
                                    )
                                    Icon(
                                        imageVector = if (sample.passed) Icons.Default.CheckCircle else Icons.Default.Info,
                                        contentDescription = null,
                                        tint = if (sample.passed) Color(0xFF4CAF50) else Color(0xFFFFB74D),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }

                                NativeQuranFontPreviewBox(
                                    sampleText = sample.sampleText,
                                    textStyle = textStyle,
                                    fontSizeSp = 21f,
                                    minHeightDp = 56.dp
                                )

                                Text(
                                    text = sample.diagnosticNoteAr,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = colors.textSecondary
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = TasneemOrange,
                        contentColor = TasneemDeepCharcoal
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "إغلاق مختبر فحص الخط",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun OpenTypeMetricRow(label: String, value: String) {
    val colors = LocalTasneemColors.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = colors.textSecondary,
            modifier = Modifier.weight(0.58f)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.labelMedium,
            color = colors.textPrimary,
            modifier = Modifier.weight(0.42f)
        )
    }
}
