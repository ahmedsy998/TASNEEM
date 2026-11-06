package com.example.ui.components

import android.graphics.RectF
import android.net.Uri
import android.view.TextureView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RadioButtonChecked
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import coil.compose.AsyncImage
import com.example.data.QuranDataRepository
import com.example.media.DraggableCanvasElement
import com.example.media.FrameElementHitBoxes
import com.example.media.PlaybackSyncState
import com.example.media.VideoFrameCanvasRenderer
import com.example.model.AudioDownloadState
import com.example.model.MediaCropMode
import com.example.model.PhoneMediaBackground
import com.example.model.QuranTextStyleConfig
import com.example.model.QuranVerse
import com.example.model.RecitationHeaderConfig
import com.example.model.VerseTransitionConfig
import com.example.model.VideoAspectRatio
import com.example.model.WatermarkConfig
import com.example.ui.theme.LocalTasneemColors
import com.example.ui.theme.TasneemDeepCharcoal
import com.example.ui.theme.TasneemOrange
import java.util.Locale
import kotlin.math.roundToInt

/**
 * Persistent Live Preview Panel ("البث الحي") - Requirements 04, 06, 07, 08, 09, 10, 12.
 * Remains fixed and visible at the top of the Video Studio while the user scrolls through settings below it.
 * Uses the exact same VideoFrameCanvasRenderer as Mp4VideoRenderEngine so the Live Preview and exported MP4
 * match 100% in aspect ratio, fonts, Tashkeel colors, single-line phrases, transitions, headers, and signature.
 */
@Composable
fun PersistentLivePreviewPanel(
    surahNameAr: String,
    selectedVerses: List<QuranVerse>,
    reciterNameAr: String,
    playbackSyncState: PlaybackSyncState,
    audioDownloadState: AudioDownloadState,
    phoneMedia: PhoneMediaBackground,
    textStyle: QuranTextStyleConfig,
    recitationHeader: RecitationHeaderConfig = RecitationHeaderConfig(),
    verseTransition: VerseTransitionConfig = VerseTransitionConfig(),
    watermark: WatermarkConfig,
    aspectRatio: VideoAspectRatio,
    isFullscreen: Boolean,
    onTogglePlayPause: () -> Unit,
    onSeekToMs: (Long) -> Unit,
    onUpdateWatermarkPosition: (Float, Float) -> Unit,
    onUpdateTextPosition: (Float, Float) -> Unit,
    onUpdateSurahHeaderPosition: (Float, Float) -> Unit = { _, _ -> },
    onUpdateVerseNumberPosition: (Float, Float) -> Unit = { _, _ -> },
    onUpdateReciterHeaderPosition: (Float, Float) -> Unit = { _, _ -> },
    onToggleFullscreen: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalTasneemColors.current
    val activeVerse = selectedVerses.find { it.verseNumber == playbackSyncState.activeVerseNumber }
        ?: selectedVerses.firstOrNull()
        ?: QuranVerse(112, 1, "قُلْ هُوَ ٱللَّهُ أَحَدٌ")

    val activeSegment = remember(audioDownloadState, activeVerse.verseNumber) {
        (audioDownloadState as? AudioDownloadState.Ready)?.segments?.find {
            it.verseNumber == activeVerse.verseNumber
        }
    }

    val localVerseTimeMs = remember(playbackSyncState.overallPositionMs, playbackSyncState.currentVersePositionMs, activeSegment) {
        if (activeSegment != null) {
            (playbackSyncState.overallPositionMs - activeSegment.startOffsetMs).coerceAtLeast(0L)
        } else {
            playbackSyncState.currentVersePositionMs.coerceAtLeast(0L)
        }
    }

    Surface(
        color = colors.elevatedSurface,
        tonalElevation = 6.dp,
        shadowElevation = 8.dp,
        shape = if (isFullscreen) RoundedCornerShape(0.dp) else RoundedCornerShape(bottomStart = 20.dp, bottomEnd = 20.dp),
        modifier = modifier
            .fillMaxWidth()
            .testTag("persistent_live_preview_panel")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            // Top Header Row: "البث الحي" badge + Active Surah/Verse summary + Fullscreen toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        color = TasneemOrange.copy(alpha = 0.18f),
                        shape = RoundedCornerShape(50),
                        border = androidx.compose.foundation.BorderStroke(1.dp, TasneemOrange)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.RadioButtonChecked,
                                contentDescription = null,
                                tint = TasneemOrange,
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = "البث الحي",
                                style = MaterialTheme.typography.labelLarge,
                                color = TasneemOrange
                            )
                        }
                    }

                    Text(
                        text = "سورة $surahNameAr · الآية ${QuranDataRepository.toArabicDigits(activeVerse.verseNumber)} (${aspectRatio.id})",
                        style = MaterialTheme.typography.labelLarge,
                        color = colors.textPrimary
                    )
                }

                // Fullscreen / Return to Editor Action
                Surface(
                    shape = RoundedCornerShape(50),
                    color = colors.cardBackground,
                    border = androidx.compose.foundation.BorderStroke(1.dp, colors.borderSubtle),
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .clickable(onClick = onToggleFullscreen)
                        .testTag("toggle_fullscreen_preview_button")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = if (isFullscreen) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                            contentDescription = if (isFullscreen) "العودة إلى المحرر" else "معاينة بملء الشاشة",
                            tint = TasneemOrange,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = if (isFullscreen) "العودة إلى المحرر" else "ملء الشاشة",
                            style = MaterialTheme.typography.labelMedium,
                            color = colors.textPrimary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Video Canvas Container with true aspect ratio (9:16 portrait, 16:9 landscape, 1:1 square)
            val previewBoxHeight = when {
                isFullscreen -> 470.dp
                aspectRatio == VideoAspectRatio.LANDSCAPE_16_9 -> 195.dp
                aspectRatio == VideoAspectRatio.SQUARE_1_1 -> 215.dp
                else -> 235.dp
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(previewBoxHeight),
                contentAlignment = Alignment.Center
            ) {
                LivePreviewVideoFrame(
                    surahNameAr = surahNameAr,
                    activeVerse = activeVerse,
                    activeSegment = activeSegment,
                    localVerseTimeMs = localVerseTimeMs,
                    reciterNameAr = reciterNameAr,
                    isPlaying = playbackSyncState.isPlaying,
                    phoneMedia = phoneMedia,
                    textStyle = textStyle,
                    recitationHeader = recitationHeader,
                    verseTransition = verseTransition,
                    watermark = watermark,
                    aspectRatio = aspectRatio,
                    onUpdateWatermarkPosition = onUpdateWatermarkPosition,
                    onUpdateTextPosition = onUpdateTextPosition,
                    onUpdateSurahHeaderPosition = onUpdateSurahHeaderPosition,
                    onUpdateVerseNumberPosition = onUpdateVerseNumberPosition,
                    onUpdateReciterHeaderPosition = onUpdateReciterHeaderPosition
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Audio Download Status or Playback & Seek Controls
            when (audioDownloadState) {
                is AudioDownloadState.Downloading -> {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "جاري تحميل تلاوة الآية ${QuranDataRepository.toArabicDigits(audioDownloadState.currentVerseNumber)} (${audioDownloadState.downloadedCount}/${audioDownloadState.selectedVerseCount})...",
                                style = MaterialTheme.typography.bodySmall,
                                color = TasneemOrange
                            )
                            Text(
                                text = "${(audioDownloadState.progressFraction * 100).roundToInt()}%",
                                style = MaterialTheme.typography.labelMedium,
                                color = colors.textPrimary
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
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
                }

                is AudioDownloadState.Error -> {
                    Text(
                        text = audioDownloadState.messageAr,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFFF8A65)
                    )
                }

                else -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        IconButton(
                            onClick = onTogglePlayPause,
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(TasneemOrange)
                                .testTag("live_preview_play_pause_button")
                        ) {
                            Icon(
                                imageVector = if (playbackSyncState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = if (playbackSyncState.isPlaying) "إيقاف مؤقت" else "تشغيل المعاينة",
                                tint = TasneemDeepCharcoal,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        val totalMs = playbackSyncState.totalDurationMs.coerceAtLeast(1L)
                        val currentMs = playbackSyncState.overallPositionMs.coerceIn(0L, totalMs)

                        Slider(
                            value = currentMs.toFloat(),
                            onValueChange = { onSeekToMs(it.toLong()) },
                            valueRange = 0f..totalMs.toFloat(),
                            colors = SliderDefaults.colors(
                                thumbColor = TasneemOrange,
                                activeTrackColor = TasneemOrange,
                                inactiveTrackColor = colors.cardBackground
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .height(26.dp)
                        )

                        Text(
                            text = "${formatMillis(currentMs)} / ${formatMillis(playbackSyncState.totalDurationMs)}",
                            style = MaterialTheme.typography.labelMedium,
                            color = colors.textSecondary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LivePreviewVideoFrame(
    surahNameAr: String,
    activeVerse: QuranVerse,
    activeSegment: com.example.model.VerseAudioSegment?,
    localVerseTimeMs: Long,
    reciterNameAr: String,
    isPlaying: Boolean,
    phoneMedia: PhoneMediaBackground,
    textStyle: QuranTextStyleConfig,
    recitationHeader: RecitationHeaderConfig,
    verseTransition: VerseTransitionConfig,
    watermark: WatermarkConfig,
    aspectRatio: VideoAspectRatio,
    onUpdateWatermarkPosition: (Float, Float) -> Unit,
    onUpdateTextPosition: (Float, Float) -> Unit,
    onUpdateSurahHeaderPosition: (Float, Float) -> Unit,
    onUpdateVerseNumberPosition: (Float, Float) -> Unit,
    onUpdateReciterHeaderPosition: (Float, Float) -> Unit
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val frameRenderer = remember(context) { VideoFrameCanvasRenderer(context) }

    val currentWatermark by rememberUpdatedState(watermark)
    val currentTextStyle by rememberUpdatedState(textStyle)
    val currentHeader by rememberUpdatedState(recitationHeader)

    var lastHitBoxes by remember { mutableStateOf(FrameElementHitBoxes()) }
    var activeDragElement by remember { mutableStateOf<DraggableCanvasElement?>(null) }

    val aspectModifier = if (aspectRatio == VideoAspectRatio.LANDSCAPE_16_9) {
        Modifier
            .fillMaxWidth(0.95f)
            .aspectRatio(aspectRatio.ratioValue, matchHeightConstraintsFirst = false)
    } else {
        Modifier
            .fillMaxHeight()
            .aspectRatio(aspectRatio.ratioValue, matchHeightConstraintsFirst = true)
    }

    BoxWithConstraints(
        modifier = aspectModifier
            .clip(RoundedCornerShape(16.dp))
            .background(TasneemDeepCharcoal)
            .border(1.5.dp, TasneemOrange.copy(alpha = 0.55f), RoundedCornerShape(16.dp)),
        contentAlignment = Alignment.Center
    ) {
        val frameWidthPx = with(density) { maxWidth.toPx().coerceAtLeast(1f) }
        val frameHeightPx = with(density) { maxHeight.toPx().coerceAtLeast(1f) }

        // 1. Background Media Layer (Phone / Curated Image or Video)
        if (!phoneMedia.uriString.isNullOrBlank()) {
            val contentScale = when (phoneMedia.cropMode) {
                MediaCropMode.CENTER_CROP, MediaCropMode.CUSTOM_PAN_ZOOM -> ContentScale.Crop
                MediaCropMode.FIT_CENTER -> ContentScale.Fit
            }

            if (phoneMedia.isVideo) {
                PhoneBackgroundVideoSurface(
                    videoUriString = phoneMedia.uriString,
                    isPlaying = isPlaying,
                    scale = phoneMedia.scale,
                    offsetX = phoneMedia.offsetX * frameWidthPx * 0.35f,
                    offsetY = phoneMedia.offsetY * frameHeightPx * 0.35f
                )
            } else {
                val brightnessShift = (phoneMedia.brightness * 255f).coerceIn(-160f, 160f)
                val colorMatrix = remember(brightnessShift) {
                    ColorMatrix(
                        floatArrayOf(
                            1f, 0f, 0f, 0f, brightnessShift,
                            0f, 1f, 0f, 0f, brightnessShift,
                            0f, 0f, 1f, 0f, brightnessShift,
                            0f, 0f, 0f, 1f, 0f
                        )
                    )
                }

                AsyncImage(
                    model = Uri.parse(phoneMedia.uriString),
                    contentDescription = "خلفية الفيديو المختارة",
                    contentScale = contentScale,
                    colorFilter = ColorFilter.colorMatrix(colorMatrix),
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            scaleX = phoneMedia.scale
                            scaleY = phoneMedia.scale
                            translationX = phoneMedia.offsetX * frameWidthPx * 0.35f
                            translationY = phoneMedia.offsetY * frameHeightPx * 0.35f
                        }
                )
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFF1D2227),
                                Color(0xFF282F36),
                                Color(0xFF1D2227)
                            )
                        )
                    )
            )
        }

        // 2. Unified Canvas Overlay (100% shared with Mp4VideoRenderEngine)
        // Renders dimming, single-line Quranic phrase, independent Tashkeel colors,
        // 7 transition animations, 3 independent header elements, and Personal Signature + Social Icons.
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(frameWidthPx, frameHeightPx) {
                    detectDragGestures(
                        onDragStart = { startOffset ->
                            val x = startOffset.x
                            val y = startOffset.y
                            val pad = 32f
                            activeDragElement = when {
                                expandRect(lastHitBoxes.signatureBounds, pad).contains(x, y) ->
                                    DraggableCanvasElement.SIGNATURE_WATERMARK
                                expandRect(lastHitBoxes.surahNameBounds, pad).contains(x, y) ->
                                    DraggableCanvasElement.SURAH_NAME
                                expandRect(lastHitBoxes.verseNumberBounds, pad).contains(x, y) ->
                                    DraggableCanvasElement.VERSE_NUMBER
                                expandRect(lastHitBoxes.reciterNameBounds, pad).contains(x, y) ->
                                    DraggableCanvasElement.RECITER_NAME
                                else -> DraggableCanvasElement.QURAN_VERSE
                            }
                        },
                        onDragEnd = { activeDragElement = null },
                        onDragCancel = { activeDragElement = null },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            val dxNorm = dragAmount.x / frameWidthPx
                            val dyNorm = dragAmount.y / frameHeightPx
                            when (activeDragElement ?: DraggableCanvasElement.QURAN_VERSE) {
                                DraggableCanvasElement.SIGNATURE_WATERMARK -> {
                                    onUpdateWatermarkPosition(
                                        (currentWatermark.normalizedX + dxNorm).coerceIn(0.08f, 0.92f),
                                        (currentWatermark.normalizedY + dyNorm).coerceIn(0.06f, 0.94f)
                                    )
                                }
                                DraggableCanvasElement.SURAH_NAME -> {
                                    onUpdateSurahHeaderPosition(
                                        (currentHeader.surahName.normalizedX + dxNorm).coerceIn(0.08f, 0.92f),
                                        (currentHeader.surahName.normalizedY + dyNorm).coerceIn(0.06f, 0.92f)
                                    )
                                }
                                DraggableCanvasElement.VERSE_NUMBER -> {
                                    onUpdateVerseNumberPosition(
                                        (currentHeader.verseNumber.normalizedX + dxNorm).coerceIn(0.08f, 0.92f),
                                        (currentHeader.verseNumber.normalizedY + dyNorm).coerceIn(0.06f, 0.92f)
                                    )
                                }
                                DraggableCanvasElement.RECITER_NAME -> {
                                    onUpdateReciterHeaderPosition(
                                        (currentHeader.reciterName.normalizedX + dxNorm).coerceIn(0.08f, 0.92f),
                                        (currentHeader.reciterName.normalizedY + dyNorm).coerceIn(0.06f, 0.92f)
                                    )
                                }
                                DraggableCanvasElement.QURAN_VERSE -> {
                                    onUpdateTextPosition(
                                        (currentTextStyle.normalizedX + dxNorm).coerceIn(0.12f, 0.88f),
                                        (currentTextStyle.normalizedY + dyNorm).coerceIn(0.14f, 0.86f)
                                    )
                                }
                            }
                        }
                    )
                }
        ) {
            drawIntoCanvas { composeCanvas ->
                lastHitBoxes = frameRenderer.renderFrame(
                    canvas = composeCanvas.nativeCanvas,
                    frameWidth = size.width.toInt().coerceAtLeast(1),
                    frameHeight = size.height.toInt().coerceAtLeast(1),
                    backgroundBitmap = null,
                    mediaConfig = phoneMedia,
                    surahNameAr = surahNameAr,
                    activeVerse = activeVerse,
                    activeSegment = activeSegment,
                    localVerseTimeMs = localVerseTimeMs,
                    reciterNameAr = reciterNameAr,
                    textConfig = textStyle,
                    headerConfig = recitationHeader,
                    transitionConfig = verseTransition,
                    watermarkConfig = watermark,
                    drawBackground = false
                )
            }
        }

        // Semantic test tag target for automated Robolectric tests verifying the draggable watermark
        if (watermark.enabled) {
            val wmOffsetX = ((watermark.normalizedX - 0.5f) * frameWidthPx).roundToInt()
            val wmOffsetY = ((watermark.normalizedY - 0.5f) * frameHeightPx).roundToInt()
            Box(
                modifier = Modifier
                    .offset { IntOffset(wmOffsetX, wmOffsetY) }
                    .size(48.dp, 24.dp)
                    .testTag("draggable_tasneem_watermark")
            )
        }
    }
}

private fun expandRect(rect: RectF, pad: Float): RectF {
    if (rect.isEmpty) return RectF()
    return RectF(rect.left - pad, rect.top - pad, rect.right + pad, rect.bottom + pad)
}

@Composable
private fun PhoneBackgroundVideoSurface(
    videoUriString: String,
    isPlaying: Boolean,
    scale: Float,
    offsetX: Float,
    offsetY: Float
) {
    val context = LocalContext.current
    val bgPlayer = remember(videoUriString) {
        ExoPlayer.Builder(context).build().apply {
            volume = 0f // Muted background video; Quranic recitation is the audio track
            repeatMode = Player.REPEAT_MODE_ALL
            setMediaItem(MediaItem.fromUri(Uri.parse(videoUriString)))
            prepare()
            playWhenReady = true
        }
    }

    LaunchedEffect(isPlaying) {
        bgPlayer.playWhenReady = true
    }

    DisposableEffect(bgPlayer) {
        onDispose {
            bgPlayer.release()
        }
    }

    AndroidView(
        factory = { ctx ->
            TextureView(ctx).also { textureView ->
                bgPlayer.setVideoTextureView(textureView)
            }
        },
        update = { textureView ->
            bgPlayer.setVideoTextureView(textureView)
        },
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                translationX = offsetX
                translationY = offsetY
            }
    )
}

fun formatMillis(ms: Long): String {
    val totalSeconds = (ms / 1000L).coerceAtLeast(0L)
    val minutes = totalSeconds / 60L
    val seconds = totalSeconds % 60L
    return String.format(Locale.US, "%02d:%02d", minutes, seconds)
}
