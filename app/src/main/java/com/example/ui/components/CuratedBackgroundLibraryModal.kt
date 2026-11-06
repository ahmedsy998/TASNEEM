package com.example.ui.components

import android.graphics.Canvas
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.PlayCircleFilled
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.data.CuratedBackgroundMediaRepository
import com.example.model.BackgroundCategory
import com.example.model.CuratedBackgroundMediaItem
import com.example.ui.theme.LocalTasneemColors
import com.example.ui.theme.TasneemDeepCharcoal
import com.example.ui.theme.TasneemOrange
import com.example.ui.theme.TasneemSoftIvory

/**
 * Curated Nature & Islamic Background Media Library Dialog (Requirement 14).
 * Includes:
 * - Separate tabs for Background Images ("صور خلفية") and Background Videos ("فيديوهات خلفية")
 * - All 15 Nature & Islamic categories
 * - Search bar
 * - Thumbnail preview grid with procedural/remote rendering
 * - Full-screen preview before selecting
 * - On-demand download and local caching
 */
@Composable
fun CuratedBackgroundLibraryDialog(
    repository: CuratedBackgroundMediaRepository,
    selectedItemId: String?,
    isDownloading: Boolean,
    onSelectItem: (CuratedBackgroundMediaItem) -> Unit,
    onDismiss: () -> Unit
) {
    val colors = LocalTasneemColors.current
    var showVideosTab by remember { mutableStateOf(false) }
    var selectedCategory by remember { mutableStateOf<BackgroundCategory?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var fullScreenPreviewItem by remember { mutableStateOf<CuratedBackgroundMediaItem?>(null) }

    val filteredItems = remember(showVideosTab, selectedCategory, searchQuery) {
        repository.getItems(
            isVideo = showVideosTab,
            category = selectedCategory,
            searchQuery = searchQuery
        )
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            color = colors.screenBackground,
            shape = RoundedCornerShape(24.dp),
            border = BorderStroke(1.dp, TasneemOrange.copy(alpha = 0.45f)),
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.90f)
                .testTag("curated_background_library_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "مكتبة الخلفيات الطبيعية والإسلامية",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = TasneemOrange
                        )
                        Text(
                            text = "١٥ تصنيفًا روحانيًا وطبيعيًا (صور عالية الدقة وفيديوهات متحركة)",
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.textSecondary
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "إغلاق المكتبة",
                            tint = colors.textPrimary
                        )
                    }
                }

                // Separate Tabs for Images vs Videos
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (!showVideosTab) TasneemOrange else colors.elevatedSurface,
                        border = BorderStroke(1.dp, if (!showVideosTab) TasneemOrange else colors.borderSubtle),
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { showVideosTab = false }
                            .testTag("library_tab_images")
                    ) {
                        Row(
                            modifier = Modifier.padding(vertical = 10.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Image,
                                contentDescription = null,
                                tint = if (!showVideosTab) TasneemDeepCharcoal else TasneemOrange,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "صور خلفية (٣٠)",
                                style = MaterialTheme.typography.labelLarge,
                                color = if (!showVideosTab) TasneemDeepCharcoal else colors.textPrimary
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (showVideosTab) TasneemOrange else colors.elevatedSurface,
                        border = BorderStroke(1.dp, if (showVideosTab) TasneemOrange else colors.borderSubtle),
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { showVideosTab = true }
                            .testTag("library_tab_videos")
                    ) {
                        Row(
                            modifier = Modifier.padding(vertical = 10.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.VideoLibrary,
                                contentDescription = null,
                                tint = if (showVideosTab) TasneemDeepCharcoal else TasneemOrange,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "فيديوهات خلفية (١٥)",
                                style = MaterialTheme.typography.labelLarge,
                                color = if (showVideosTab) TasneemDeepCharcoal else colors.textPrimary
                            )
                        }
                    }
                }

                // Search Bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = {
                        Text(
                            text = "ابحث في الخلفيات (مثال: الكعبة، المسجد النبوي، الجبال، النجوم...)",
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.textSecondary
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            tint = TasneemOrange
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "مسح البحث",
                                    tint = colors.textSecondary
                                )
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
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
                        .testTag("curated_library_search_input")
                )

                // Horizontal Scrollable 15 Category Filter Chips
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CategoryChip(
                        label = "الكل (١٥ تصنيفًا)",
                        isSelected = selectedCategory == null,
                        onClick = { selectedCategory = null }
                    )
                    BackgroundCategory.entries.forEach { category ->
                        CategoryChip(
                            label = category.titleAr,
                            isSelected = selectedCategory == category,
                            onClick = {
                                selectedCategory = if (selectedCategory == category) null else category
                            }
                        )
                    }
                }

                if (isDownloading) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = TasneemOrange.copy(alpha = 0.14f),
                        border = BorderStroke(1.dp, TasneemOrange),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            CircularProgressIndicator(
                                color = TasneemOrange,
                                strokeWidth = 2.5.dp,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "جاري تجهيز وحفظ الخلفية المختارة بجودة عالية في ذاكرة الجهاز...",
                                style = MaterialTheme.typography.bodySmall,
                                color = TasneemSoftIvory
                            )
                        }
                    }
                }

                // Thumbnail Preview Grid
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    contentPadding = PaddingValues(vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(filteredItems, key = { it.id }) { item ->
                        val isSelected = item.id == selectedItemId
                        val isCached = remember(item.id) { repository.isCached(item) }
                        CuratedMediaGridCard(
                            item = item,
                            repository = repository,
                            isSelected = isSelected,
                            isCached = isCached,
                            onPreviewClick = { fullScreenPreviewItem = item },
                            onSelectClick = { onSelectItem(item) }
                        )
                    }
                }
            }
        }
    }

    // Full-Screen Preview Modal Before Selecting
    fullScreenPreviewItem?.let { previewItem ->
        Dialog(
            onDismissRequest = { fullScreenPreviewItem = null },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(
                color = TasneemDeepCharcoal,
                shape = RoundedCornerShape(22.dp),
                border = BorderStroke(1.5.dp, TasneemOrange),
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .fillMaxHeight(0.84f)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = previewItem.titleAr,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = TasneemSoftIvory
                            )
                            Text(
                                text = "${previewItem.category.titleAr} · ${previewItem.licenseInfoAr}",
                                style = MaterialTheme.typography.bodySmall,
                                color = TasneemOrange
                            )
                        }
                        IconButton(onClick = { fullScreenPreviewItem = null }) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "إغلاق المعاينة",
                                tint = TasneemSoftIvory
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .clip(RoundedCornerShape(16.dp))
                            .border(1.dp, TasneemOrange.copy(alpha = 0.5f), RoundedCornerShape(16.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        CuratedItemArtworkSurface(
                            item = previewItem,
                            repository = repository,
                            modifier = Modifier.fillMaxSize()
                        )
                        // Sample Quranic overlay preview
                        Text(
                            text = "﴿ وَمِزَاجُهُۥ مِن تَسْنِيمٍ ﴾",
                            style = MaterialTheme.typography.headlineSmall,
                            color = Color.White,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .background(Color.Black.copy(alpha = 0.42f), RoundedCornerShape(12.dp))
                                .padding(horizontal = 16.dp, vertical = 10.dp)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = { fullScreenPreviewItem = null },
                            border = BorderStroke(1.dp, colors.borderSubtle),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("رجوع للمكتبة", color = TasneemSoftIvory)
                        }
                        Button(
                            onClick = {
                                val chosen = previewItem
                                fullScreenPreviewItem = null
                                onSelectItem(chosen)
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = TasneemOrange,
                                contentColor = TasneemDeepCharcoal
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1.4f)
                        ) {
                            Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("اعتماد هذه الخلفية", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CategoryChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val colors = LocalTasneemColors.current
    Surface(
        shape = RoundedCornerShape(50),
        color = if (isSelected) TasneemOrange else colors.elevatedSurface,
        border = BorderStroke(1.dp, if (isSelected) TasneemOrange else colors.borderSubtle),
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .clickable(onClick = onClick)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = if (isSelected) TasneemDeepCharcoal else colors.textPrimary,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
        )
    }
}

@Composable
private fun CuratedMediaGridCard(
    item: CuratedBackgroundMediaItem,
    repository: CuratedBackgroundMediaRepository,
    isSelected: Boolean,
    isCached: Boolean,
    onPreviewClick: () -> Unit,
    onSelectClick: () -> Unit
) {
    val colors = LocalTasneemColors.current
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = colors.cardBackground,
        border = BorderStroke(
            width = if (isSelected) 2.dp else 1.dp,
            color = if (isSelected) TasneemOrange else colors.borderSubtle
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onSelectClick)
            .testTag("curated_item_${item.id}")
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(9f / 12f)
            ) {
                CuratedItemArtworkSurface(
                    item = item,
                    repository = repository,
                    modifier = Modifier.fillMaxSize()
                )

                // Top badges: Video / Image + Cache status
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = TasneemDeepCharcoal.copy(alpha = 0.78f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = if (item.isVideo) Icons.Default.PlayCircleFilled else Icons.Default.Image,
                                contentDescription = null,
                                tint = TasneemOrange,
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = if (item.isVideo) "فيديو ${item.durationSeconds}ث" else "صورة HD",
                                style = MaterialTheme.typography.labelSmall,
                                color = TasneemSoftIvory
                            )
                        }
                    }

                    IconButton(
                        onClick = onPreviewClick,
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(TasneemDeepCharcoal.copy(alpha = 0.78f))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Visibility,
                            contentDescription = "معاينة بملء الشاشة",
                            tint = TasneemOrange,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = item.titleAr,
                    style = MaterialTheme.typography.labelLarge,
                    color = colors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = item.category.titleAr,
                        style = MaterialTheme.typography.bodySmall,
                        color = TasneemOrange,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Icon(
                        imageVector = if (isSelected) Icons.Default.CheckCircle
                        else if (isCached) Icons.Default.CloudDone
                        else Icons.Default.CloudDownload,
                        contentDescription = null,
                        tint = if (isSelected || isCached) TasneemOrange else colors.textSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun CuratedItemArtworkSurface(
    item: CuratedBackgroundMediaItem,
    repository: CuratedBackgroundMediaRepository,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier) {
        // Procedural artistic canvas backdrop guarantees immediate rich visual rendering even offline
        androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
            drawIntoCanvas { composeCanvas ->
                repository.drawCuratedSceneFrame(
                    canvas = composeCanvas.nativeCanvas,
                    width = size.width.coerceAtLeast(1f),
                    height = size.height.coerceAtLeast(1f),
                    item = item,
                    phase = 0.25f
                )
            }
        }
        // Overlay remote high-res thumbnail when online
        if (!item.thumbnailUrl.startsWith("procedural://")) {
            AsyncImage(
                model = item.thumbnailUrl,
                contentDescription = item.titleAr,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}
