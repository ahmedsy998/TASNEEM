package com.example.ui.components

import android.net.Uri
import android.view.TextureView
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
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import coil.compose.AsyncImage
import com.example.data.QuranDataRepository
import com.example.media.PlaybackSyncState
import com.example.model.AudioDownloadState
import com.example.model.MediaCropMode
import com.example.model.PhoneMediaBackground
import com.example.model.QuranTextStyleConfig
import com.example.model.QuranVerse
import com.example.model.VideoAspectRatio
import com.example.model.WatermarkConfig
import com.example.ui.theme.CairoFontFamily
import com.example.ui.theme.LocalTasneemColors
import com.example.ui.theme.TasneemDeepCharcoal
import com.example.ui.theme.TasneemOrange
import com.example.ui.theme.TasneemSilverGray
import com.example.ui.theme.TasneemSoftIvory
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Persistent Live Preview Panel ("البث الحي") - Sections 12 & 13.
 * Remains fixed and visible at the top of the Video Studio while the user scrolls through settings below it.
 * Updates immediately on every setting change and supports direct drag positioning of the Tasneem watermark
 * and Quranic text block.
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
    watermark: WatermarkConfig,
    aspectRatio: VideoAspectRatio,
    isFullscreen: Boolean,
    onTogglePlayPause: () -> Unit,
    onSeekToMs: (Long) -> Unit,
    onUpdateWatermarkPosition: (Float, Float) -> Unit,
    onUpdateTextPosition: (Float, Float) -> Unit,
    onToggleFullscreen: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalTasneemColors.current
    val activeVerse = selectedVerses.find { it.verseNumber == playbackSyncState.activeVerseNumber }
        ?: selectedVerses.firstOrNull()
        ?: QuranVerse(112, 1, "قُلْ هُوَ ٱللَّهُ أَحَدٌ")

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
                        text = "سورة $surahNameAr · الآية ${QuranDataRepository.toArabicDigits(activeVerse.verseNumber)}",
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

            // Video Canvas Container
            val previewBoxHeight = if (isFullscreen) 460.dp else 215.dp
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(previewBoxHeight),
                contentAlignment = Alignment.Center
            ) {
                LivePreviewVideoFrame(
                    surahNameAr = surahNameAr,
                    activeVerse = activeVerse,
                    activeWordIndex = playbackSyncState.activeWordIndex,
                    reciterNameAr = reciterNameAr,
                    isPlaying = playbackSyncState.isPlaying,
                    phoneMedia = phoneMedia,
                    textStyle = textStyle,
                    watermark = watermark,
                    aspectRatio = aspectRatio,
                    isFullscreen = isFullscreen,
                    onUpdateWatermarkPosition = onUpdateWatermarkPosition,
                    onUpdateTextPosition = onUpdateTextPosition
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
    activeWordIndex: Int?,
    reciterNameAr: String,
    isPlaying: Boolean,
    phoneMedia: PhoneMediaBackground,
    textStyle: QuranTextStyleConfig,
    watermark: WatermarkConfig,
    aspectRatio: VideoAspectRatio,
    isFullscreen: Boolean,
    onUpdateWatermarkPosition: (Float, Float) -> Unit,
    onUpdateTextPosition: (Float, Float) -> Unit
) {
    val density = LocalDensity.current
    val currentWatermark by rememberUpdatedState(watermark)
    val currentTextStyle by rememberUpdatedState(textStyle)

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxHeight()
            .aspectRatio(aspectRatio.ratioValue, matchHeightConstraintsFirst = true)
            .clip(RoundedCornerShape(16.dp))
            .background(TasneemDeepCharcoal)
            .border(1.5.dp, TasneemOrange.copy(alpha = 0.5f), RoundedCornerShape(16.dp)),
        contentAlignment = Alignment.Center
    ) {
        val frameWidthPx = with(density) { maxWidth.toPx().coerceAtLeast(1f) }
        val frameHeightPx = with(density) { maxHeight.toPx().coerceAtLeast(1f) }
        val scaleFactor = (minOf(frameWidthPx, frameHeightPx) / 900f).coerceIn(0.32f, 1.15f)

        // 1. Phone Background Image or Video
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
                    contentDescription = "خلفية الفيديو المختارة من الهاتف",
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
            // Subtle Islamic gradient canvas when user hasn't picked a phone image/video yet
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

        // 2. Dimming & Vignette Overlay
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF12161A).copy(alpha = phoneMedia.dimLevel.coerceIn(0f, 0.92f)))
        )

        // 3. Draggable Synchronized Quranic Verse Block
        val textCenterOffsetX = ((textStyle.normalizedX - 0.5f) * frameWidthPx).roundToInt()
        val textCenterOffsetY = ((textStyle.normalizedY - 0.5f) * frameHeightPx).roundToInt()

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .offset { IntOffset(textCenterOffsetX, textCenterOffsetY) }
                .fillMaxWidth(0.88f)
                .pointerInput(frameWidthPx, frameHeightPx) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        val newX = (currentTextStyle.normalizedX + dragAmount.x / frameWidthPx)
                            .coerceIn(0.18f, 0.82f)
                        val newY = (currentTextStyle.normalizedY + dragAmount.y / frameHeightPx)
                            .coerceIn(0.22f, 0.78f)
                        onUpdateTextPosition(newX, newY)
                    }
                }
        ) {
            if (textStyle.showSurahHeader) {
                Text(
                    text = "سُورَةُ $surahNameAr",
                    fontFamily = textStyle.font.composeFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = (18f * scaleFactor + 8f).sp,
                    color = TasneemSoftIvory,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "الآية ${QuranDataRepository.toArabicDigits(activeVerse.verseNumber)}",
                    fontFamily = CairoFontFamily,
                    fontSize = (11f * scaleFactor + 6f).sp,
                    color = TasneemOrange,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(6.dp))
            }

            // Highlight active word if verified word-level timestamps exist
            val words = remember(activeVerse.uthmaniText) {
                activeVerse.uthmaniText.trim().split(Regex("\\s+"))
            }
            val ayahSymbol = QuranDataRepository.formatAyahSymbol(activeVerse.verseNumber)
            val baseColor = textStyle.colorOption.composeColor

            val annotatedVerse = buildAnnotatedString {
                words.forEachIndexed { idx, word ->
                    val wordOneBased = idx + 1
                    val isWordHighlighted = activeWordIndex != null && activeWordIndex == wordOneBased
                    withStyle(
                        SpanStyle(
                            color = if (isWordHighlighted) TasneemOrange else baseColor,
                            fontWeight = if (textStyle.isBold || isWordHighlighted) FontWeight.Bold else FontWeight.Normal
                        )
                    ) {
                        append(word)
                        append(" ")
                    }
                }
                withStyle(
                    SpanStyle(
                        color = TasneemOrange,
                        fontWeight = FontWeight.Bold
                    )
                ) {
                    append(ayahSymbol)
                }
            }

            val effectiveFontSize = (textStyle.fontSizeSp * scaleFactor * 1.45f).coerceIn(13f, 38f)
            Text(
                text = annotatedVerse,
                fontFamily = textStyle.font.composeFontFamily,
                fontSize = effectiveFontSize.sp,
                lineHeight = (effectiveFontSize * textStyle.lineSpacingMultiplier).sp,
                textAlign = textStyle.alignment.composeAlign,
                style = TextStyle(
                    shadow = Shadow(
                        color = Color.Black.copy(alpha = 0.85f),
                        blurRadius = 8f
                    )
                ),
                modifier = Modifier.fillMaxWidth()
            )

            if (textStyle.showTafsir && activeVerse.tafsirMuyassar.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = activeVerse.tafsirMuyassar,
                    fontFamily = CairoFontFamily,
                    fontSize = (effectiveFontSize * 0.58f).coerceAtLeast(10f).sp,
                    lineHeight = (effectiveFontSize * 0.78f).sp,
                    color = TasneemSoftIvory.copy(alpha = 0.92f),
                    textAlign = TextAlign.Center
                )
            }

            if (textStyle.showTranslation && activeVerse.translationSaheeh.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = activeVerse.translationSaheeh,
                    fontFamily = CairoFontFamily,
                    fontSize = (effectiveFontSize * textStyle.translationFontSizeScale * 0.75f).coerceAtLeast(9f).sp,
                    lineHeight = (effectiveFontSize * 0.75f).sp,
                    color = TasneemSilverGray,
                    textAlign = TextAlign.Center
                )
            }

            if (textStyle.showReciterName) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = reciterNameAr,
                    fontFamily = CairoFontFamily,
                    fontWeight = FontWeight.Medium,
                    fontSize = (11f * scaleFactor + 6f).sp,
                    color = TasneemSoftIvory.copy(alpha = 0.9f),
                    textAlign = TextAlign.Center
                )
            }
        }

        // 4. Draggable Tasneem Watermark ("تسنيم") with collision avoidance so it never obscures the Quranic text
        val safeNormY = if (abs(watermark.normalizedY - textStyle.normalizedY) < 0.22f &&
            abs(watermark.normalizedX - textStyle.normalizedX) < 0.45f
        ) {
            if (textStyle.normalizedY <= 0.55f) 0.90f else 0.10f
        } else {
            watermark.normalizedY
        }

        val wmOffsetX = ((watermark.normalizedX - 0.5f) * frameWidthPx).roundToInt()
        val wmOffsetY = ((safeNormY - 0.5f) * frameHeightPx).roundToInt()
        val wmAlpha = watermark.opacity.coerceIn(0.2f, 1.0f)
        val wmFontSize = (watermark.fontSizeSp * watermark.scale * scaleFactor * 1.35f).coerceIn(9f, 24f)

        Box(
            modifier = Modifier
                .offset { IntOffset(wmOffsetX, wmOffsetY) }
                .pointerInput(frameWidthPx, frameHeightPx) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        val newX = (currentWatermark.normalizedX + dragAmount.x / frameWidthPx)
                            .coerceIn(0.10f, 0.90f)
                        val newY = (currentWatermark.normalizedY + dragAmount.y / frameHeightPx)
                            .coerceIn(0.07f, 0.93f)
                        onUpdateWatermarkPosition(newX, newY)
                    }
                }
                .then(
                    if (watermark.elegantGlow) {
                        Modifier.shadow(
                            elevation = 8.dp,
                            shape = RoundedCornerShape(50),
                            ambientColor = TasneemOrange,
                            spotColor = TasneemOrange
                        )
                    } else Modifier
                )
                .clip(RoundedCornerShape(50))
                .background(TasneemDeepCharcoal.copy(alpha = wmAlpha * 0.55f))
                .border(
                    width = 0.8.dp,
                    color = TasneemOrange.copy(alpha = wmAlpha * 0.7f),
                    shape = RoundedCornerShape(50)
                )
                .padding(horizontal = 10.dp, vertical = 3.dp)
                .testTag("draggable_tasneem_watermark")
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size((5f * watermark.scale).coerceIn(4f, 9f).dp)
                        .clip(CircleShape)
                        .background(TasneemOrange.copy(alpha = wmAlpha))
                )
                Text(
                    text = watermark.signatureText.ifBlank { "تسنيم" },
                    fontFamily = CairoFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = wmFontSize.sp,
                    color = TasneemSoftIvory.copy(alpha = wmAlpha),
                    style = TextStyle(
                        shadow = if (watermark.subtleShadow || watermark.elegantGlow) {
                            Shadow(
                                color = if (watermark.elegantGlow) TasneemOrange.copy(alpha = wmAlpha)
                                else Color.Black.copy(alpha = wmAlpha),
                                blurRadius = if (watermark.elegantGlow) 10f else 5f
                            )
                        } else null
                    )
                )
            }
        }
    }
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
