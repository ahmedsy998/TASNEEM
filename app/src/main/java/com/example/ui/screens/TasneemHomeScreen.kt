package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.FontDownload
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.MovieCreation
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.PlayCircleFilled
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.QuranDataRepository
import com.example.ui.components.TasneemGeometricLogo
import com.example.ui.theme.AmiriFontFamily
import com.example.ui.theme.LocalTasneemColors
import com.example.ui.theme.TasneemDeepCharcoal
import com.example.ui.theme.TasneemOrange
import com.example.ui.theme.TasneemSoftIvory
import com.example.viewmodel.StudioUiState

/**
 * Tasneem Main Screen (Section 04).
 * Clean, distraction-free Islamic visual identity featuring the primary action:
 * "أنشئ فيديو قرآني" (Create a Quranic Video).
 * Free of any login, Qibla, Prayer Times, Offline Mode, Drafts, or bottom navigation clutter.
 */
@Composable
fun TasneemHomeScreen(
    uiState: StudioUiState,
    onStartCreateVideo: () -> Unit,
    onQuickOpenSurahSelector: () -> Unit,
    onQuickOpenReciterSelector: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalTasneemColors.current
    val scrollState = rememberScrollState()
    val totalVersesSelected = (uiState.endVerse - uiState.startVerse + 1).coerceAtLeast(1)

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(horizontal = 20.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // 1. Hero Card: Tasneem Brand & Primary Action ("أنشئ فيديو قرآني")
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = colors.cardBackground,
            tonalElevation = 6.dp,
            shadowElevation = 8.dp,
            border = androidx.compose.foundation.BorderStroke(
                width = 1.5.dp,
                color = TasneemOrange.copy(alpha = 0.55f)
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                TasneemOrange.copy(alpha = 0.14f),
                                colors.cardBackground
                            )
                        )
                    )
                    .padding(24.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    TasneemGeometricLogo(sizeDp = 72)

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "﴿ وَمِزَاجُهُۥ مِن تَسْنِيمٍ ﴾",
                        fontFamily = AmiriFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 24.sp,
                        color = TasneemOrange,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "استوديو تسنيم لصناعة الفيديو القرآني",
                        style = MaterialTheme.typography.headlineMedium,
                        color = colors.textPrimary,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "اختر السورة ونطاق الآيات بدقة، حمّل تلاوة القارئ للآيات المختارة فقط، أضف خلفيتك من الهاتف، وصدّر الفيديو كاملاً بجودة MP4 عالية.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.textSecondary,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(22.dp))

                    // Primary CTA: Create a Quranic Video ("أنشئ فيديو قرآني")
                    Button(
                        onClick = onStartCreateVideo,
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = TasneemOrange,
                            contentColor = TasneemDeepCharcoal
                        ),
                        contentPadding = PaddingValues(horizontal = 28.dp, vertical = 16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("create_quran_video_primary_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.MovieCreation,
                            contentDescription = null,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "أنشئ فيديو قرآني",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // 2. Current Project Summary Card (Quick Overview & Instant Studio Jump)
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = colors.cardBackground,
            border = androidx.compose.foundation.BorderStroke(1.dp, colors.borderSubtle),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "إعدادات المقطع الحالي",
                        style = MaterialTheme.typography.titleMedium,
                        color = TasneemOrange
                    )
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = TasneemOrange.copy(alpha = 0.16f),
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .clickable(onClick = onStartCreateVideo)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayCircleFilled,
                                contentDescription = null,
                                tint = TasneemOrange,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "فتح المحرر والبث الحي",
                                style = MaterialTheme.typography.labelMedium,
                                color = TasneemOrange
                            )
                        }
                    }
                }

                // Quick Interactive Summary Tiles
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    SummaryInfoTile(
                        icon = Icons.Default.MenuBook,
                        label = "السورة والآيات",
                        value = "سورة ${uiState.selectedSurah.nameAr} (${QuranDataRepository.toArabicDigits(uiState.startVerse)} - ${QuranDataRepository.toArabicDigits(uiState.endVerse)})",
                        subValue = "إجمالي ${QuranDataRepository.toArabicDigits(totalVersesSelected)} آيات",
                        onClick = {
                            onStartCreateVideo()
                            onQuickOpenSurahSelector()
                        },
                        modifier = Modifier.weight(1f)
                    )

                    SummaryInfoTile(
                        icon = Icons.Default.RecordVoiceOver,
                        label = "القارئ المختار",
                        value = uiState.selectedReciter.nameAr,
                        subValue = "تلاوة الآيات المختارة فقط",
                        onClick = {
                            onStartCreateVideo()
                            onQuickOpenReciterSelector()
                        },
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    SummaryInfoTile(
                        icon = Icons.Default.FontDownload,
                        label = "الخط القرآني",
                        value = uiState.textStyle.font.displayNameAr.substringBefore(" ("),
                        subValue = "توقيع العلامة: ${uiState.watermark.signatureText}",
                        onClick = onStartCreateVideo,
                        modifier = Modifier.weight(1f)
                    )

                    SummaryInfoTile(
                        icon = Icons.Default.HighQuality,
                        label = "صيغة ودقة التصدير",
                        value = "MP4 · ${uiState.resolution.id}",
                        subValue = uiState.aspectRatio.labelAr,
                        onClick = onStartCreateVideo,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // 3. Studio Capabilities & Workflow Highlights
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = colors.cardBackground,
            border = androidx.compose.foundation.BorderStroke(1.dp, colors.borderSubtle),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "خطوات العمل في استوديو تسنيم",
                    style = MaterialTheme.typography.titleMedium,
                    color = colors.textPrimary
                )

                WorkflowStepRow(
                    stepNumber = "١",
                    icon = Icons.Default.MenuBook,
                    title = "اختيار السورة ونطاق الآيات الدقيق",
                    subtitle = "الوصول الكامل لجميع سور القرآن الكريم الـ ١١٤ بالرسم العثماني الموثق."
                )
                WorkflowStepRow(
                    stepNumber = "٢",
                    icon = Icons.Default.AudioFile,
                    title = "تحميل صوت الآيات المختارة فقط",
                    subtitle = "نخبة ٢٠ قارئًا معتمدًا مع تحميل ملفات الآيات المحددة فقط ومزامنة النص مع التلاوة."
                )
                WorkflowStepRow(
                    stepNumber = "٣",
                    icon = Icons.Default.PhotoLibrary,
                    title = "استيراد خلفية الصورة أو الفيديو من الهاتف",
                    subtitle = "اختر صورتك أو مقطعك مباشرة من الهاتف مع تحكم كامل بالاقتصاص والإضاءة والتكرار."
                )
                WorkflowStepRow(
                    stepNumber = "٤",
                    icon = Icons.Default.HighQuality,
                    title = "البث الحي وتصدير الفيديو الكامل بصيغة MP4",
                    subtitle = "معاينة فورية ثابتة أثناء التعديل وتصدير الفيديو كاملاً بدقة 1080p أو 720p."
                )
            }
        }
    }
}

@Composable
private fun SummaryInfoTile(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    subValue: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalTasneemColors.current
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = colors.elevatedSurface,
        border = androidx.compose.foundation.BorderStroke(1.dp, colors.borderSubtle),
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = TasneemOrange,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    color = colors.textSecondary
                )
            }
            Text(
                text = value,
                style = MaterialTheme.typography.titleSmall,
                color = colors.textPrimary,
                maxLines = 1
            )
            Text(
                text = subValue,
                style = MaterialTheme.typography.bodySmall,
                color = TasneemOrange,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun WorkflowStepRow(
    stepNumber: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String
) {
    val colors = LocalTasneemColors.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(TasneemOrange.copy(alpha = 0.16f))
                .border(1.dp, TasneemOrange, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = stepNumber,
                style = MaterialTheme.typography.labelLarge,
                color = TasneemOrange
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = TasneemSoftIvory,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    color = colors.textPrimary
                )
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = colors.textSecondary
            )
        }
    }
}
