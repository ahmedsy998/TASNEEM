package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.QuranDataRepository
import com.example.model.ReciterInfo
import com.example.model.SurahInfo
import com.example.ui.theme.LocalTasneemColors
import com.example.ui.theme.TasneemDeepCharcoal
import com.example.ui.theme.TasneemOrange

/**
 * Complete 114 Surahs Selector BottomSheet (Section 06).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SurahSelectorBottomSheet(
    allSurahs: List<SurahInfo>,
    selectedSurah: SurahInfo,
    onSelectSurah: (SurahInfo) -> Unit,
    onDismiss: () -> Unit
) {
    val colors = LocalTasneemColors.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var searchQuery by remember { mutableStateOf("") }

    val filteredSurahs = remember(searchQuery, allSurahs) {
        if (searchQuery.isBlank()) allSurahs
        else allSurahs.filter {
            it.nameAr.contains(searchQuery.trim()) ||
                it.number.toString() == searchQuery.trim() ||
                QuranDataRepository.toArabicDigits(it.number) == searchQuery.trim()
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = colors.elevatedSurface,
        contentColor = colors.textPrimary
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
                .padding(horizontal = 18.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "اختيار السورة القرآنية",
                        style = MaterialTheme.typography.titleLarge,
                        color = colors.textPrimary
                    )
                    Text(
                        text = "المصحف الشريف كاملاً (١١٤ سورة بالرسم العثماني الموثق)",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.textSecondary
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "إغلاق",
                        tint = colors.textSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = {
                    Text("ابحث باسم السورة أو رقمها...", color = colors.textSecondary)
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = TasneemOrange
                    )
                },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = TasneemOrange,
                    unfocusedBorderColor = colors.borderSubtle,
                    focusedTextColor = colors.textPrimary,
                    unfocusedTextColor = colors.textPrimary
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("surah_search_input")
            )

            Spacer(modifier = Modifier.height(12.dp))

            LazyColumn(
                contentPadding = PaddingValues(bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(filteredSurahs, key = { it.number }) { surah ->
                    val isSelected = surah.number == selectedSurah.number
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = colors.cardBackground,
                        border = androidx.compose.foundation.BorderStroke(
                            width = if (isSelected) 2.dp else 1.dp,
                            color = if (isSelected) TasneemOrange else colors.borderSubtle
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .clickable { onSelectSurah(surah) }
                            .testTag("surah_item_${surah.number}")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (isSelected) TasneemOrange
                                            else TasneemOrange.copy(alpha = 0.14f)
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = QuranDataRepository.toArabicDigits(surah.number),
                                        style = MaterialTheme.typography.labelLarge,
                                        color = if (isSelected) TasneemDeepCharcoal else TasneemOrange
                                    )
                                }
                                Column {
                                    Text(
                                        text = "سورة ${surah.nameAr}",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = colors.textPrimary
                                    )
                                    Text(
                                        text = "${surah.revelationTypeAr} · ${QuranDataRepository.toArabicDigits(surah.verseCount)} آية",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = colors.textSecondary
                                    )
                                }
                            }

                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = "مختارة",
                                    tint = TasneemOrange,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Exactly 20 Famous Quran Reciters Selector BottomSheet (Section 08).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReciterSelectorBottomSheet(
    allReciters: List<ReciterInfo>,
    selectedReciter: ReciterInfo,
    previewingReciterId: String?,
    onSelectReciter: (ReciterInfo) -> Unit,
    onPreviewReciter: (ReciterInfo) -> Unit,
    onDismiss: () -> Unit
) {
    val colors = LocalTasneemColors.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = colors.elevatedSurface,
        contentColor = colors.textPrimary
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
                .padding(horizontal = 18.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "اختيار القارئ (٢٠ قارئًا معتمدًا)",
                        style = MaterialTheme.typography.titleLarge,
                        color = colors.textPrimary
                    )
                    Text(
                        text = "يمكنك الاستماع لمعاينة صوتية وتغيير القارئ مع الاحتفاظ بالآيات المختارة",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.textSecondary
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "إغلاق",
                        tint = colors.textSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            LazyColumn(
                contentPadding = PaddingValues(bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(allReciters, key = { it.id }) { reciter ->
                    val isSelected = reciter.id == selectedReciter.id
                    val isPreviewing = reciter.id == previewingReciterId
                    val hasVerifiedAudio = reciter.everyAyahSubfolder.isNotBlank() || reciter.alQuranCloudEdition != null

                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = colors.cardBackground,
                        border = androidx.compose.foundation.BorderStroke(
                            width = if (isSelected) 2.dp else 1.dp,
                            color = if (isSelected) TasneemOrange else colors.borderSubtle
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .clickable { onSelectReciter(reciter) }
                            .testTag("reciter_item_${reciter.index}")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = if (isSelected) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                                    contentDescription = null,
                                    tint = if (isSelected) TasneemOrange else colors.textSecondary,
                                    modifier = Modifier.size(22.dp)
                                )
                                Column {
                                    Text(
                                        text = "${QuranDataRepository.toArabicDigits(reciter.index)}. ${reciter.nameAr}",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = colors.textPrimary
                                    )
                                    Text(
                                        text = reciter.styleAr,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (hasVerifiedAudio) colors.textSecondary else TasneemOrange.copy(alpha = 0.85f)
                                    )
                                }
                            }

                            if (hasVerifiedAudio) {
                                Surface(
                                    shape = RoundedCornerShape(50),
                                    color = if (isPreviewing) TasneemOrange else TasneemOrange.copy(alpha = 0.14f),
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(50))
                                        .clickable { onPreviewReciter(reciter) }
                                        .testTag("preview_reciter_${reciter.index}")
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (isPreviewing) Icons.Default.Stop else Icons.Default.PlayArrow,
                                            contentDescription = if (isPreviewing) "إيقاف المعاينة" else "معاينة صوتية",
                                            tint = if (isPreviewing) TasneemDeepCharcoal else TasneemOrange,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Text(
                                            text = if (isPreviewing) "إيقاف" else "استماع",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = if (isPreviewing) TasneemDeepCharcoal else TasneemOrange
                                        )
                                    }
                                }
                            } else {
                                Icon(
                                    imageVector = Icons.Default.GraphicEq,
                                    contentDescription = "يتطلب تهيئة مزود خارجي",
                                    tint = colors.textSecondary.copy(alpha = 0.5f),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
