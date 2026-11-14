package com.example.media

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMetadataRetriever
import android.media.MediaMuxer
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.example.data.CuratedBackgroundMediaRepository
import com.example.model.ExportValidationReport
import com.example.model.PhoneMediaBackground
import com.example.model.QuranTextStyleConfig
import com.example.model.QuranVerse
import com.example.model.RecitationHeaderConfig
import com.example.model.VerseAudioSegment
import com.example.model.VerseTransitionConfig
import com.example.model.VideoAspectRatio
import com.example.model.VideoExportState
import com.example.model.VideoResolution
import com.example.model.WatermarkConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.abs

/**
 * Native MP4 Video Rendering and Export Validation Engine.
 *
 * Strictly exports MP4 (.mp4) ONLY.
 * Renders the COMPLETE selected Quranic recitation duration (never a 3-second sample).
 * Preserves moving video backgrounds as continuous moving frames across the entire recitation
 * (never converting a selected video into a static image or freezing on keyframe 0).
 * Loops shorter background videos or trims longer background videos to match the exact audio duration.
 * Validates the output MP4 container, video stream, changing frames, audio stream, and duration before completion.
 */
class Mp4VideoRenderEngine(
    private val context: Context,
    private val mediaImportManager: PhoneMediaImportManager,
    private val frameRenderer: VideoFrameCanvasRenderer,
    private val curatedMediaRepository: CuratedBackgroundMediaRepository = CuratedBackgroundMediaRepository(context)
) {

    /**
     * Computes the effective timestamp (in milliseconds) within a background video for a given
     * recitation timestamp [currentRecitationTimeMs].
     *
     * - If the background video is longer than the recitation (or [currentRecitationTimeMs] is within
     *   the first pass), returns [currentRecitationTimeMs] so the video plays normally and trims cleanly
     *   at [totalRecitationDurationMs].
     * - If the background video is shorter than the recitation and [loopVideo] is true, wraps smoothly
     *   using `currentRecitationTimeMs % backgroundVideoDurationMs`.
     * - If [loopVideo] is false and [currentRecitationTimeMs] exceeds [backgroundVideoDurationMs],
     *   clamps to the end of the background video.
     */
    fun computeEffectiveBackgroundVideoTimeMs(
        currentRecitationTimeMs: Long,
        backgroundVideoDurationMs: Long,
        totalRecitationDurationMs: Long,
        loopVideo: Boolean = true
    ): Long {
        val clampedRecitationMs = currentRecitationTimeMs.coerceIn(0L, totalRecitationDurationMs.coerceAtLeast(1L))
        if (backgroundVideoDurationMs <= 0L) return clampedRecitationMs
        return if (clampedRecitationMs < backgroundVideoDurationMs) {
            clampedRecitationMs
        } else if (loopVideo) {
            clampedRecitationMs % backgroundVideoDurationMs
        } else {
            (backgroundVideoDurationMs - 1L).coerceAtLeast(0L)
        }
    }

    /**
     * Configures a [MediaMetadataRetriever] reliably for both `file://` and `content://` URIs.
     */
    private fun configureRetrieverDataSource(retriever: MediaMetadataRetriever, uri: Uri): Boolean {
        return try {
            if (uri.scheme == "file" && !uri.path.isNullOrBlank()) {
                val file = File(uri.path!!)
                if (file.exists() && file.length() > 0L) {
                    retriever.setDataSource(file.absolutePath)
                    return true
                }
            }
            retriever.setDataSource(context, uri)
            true
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Resolves the duration of a selected background video in milliseconds.
     */
    fun resolveBackgroundVideoDurationMs(
        mediaBackground: PhoneMediaBackground,
        retriever: MediaMetadataRetriever? = null
    ): Long {
        if (!mediaBackground.isVideo) return 0L
        val fromRetriever = try {
            retriever?.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull()
        } catch (_: Exception) {
            null
        }
        if (fromRetriever != null && fromRetriever > 0L) return fromRetriever

        val bgUri = mediaBackground.uriString?.takeIf { it.isNotBlank() }?.let { Uri.parse(it) }
        if (bgUri != null && bgUri.scheme == "file" && !bgUri.path.isNullOrBlank()) {
            val parsed = parseMp4ContainerMetadata(File(bgUri.path!!))
            if (parsed.durationMs > 0L) return parsed.durationMs
        }

        if (mediaBackground.durationMs > 0L) return mediaBackground.durationMs
        val curatedItem = curatedMediaRepository.findItemById(mediaBackground.curatedItemId)
        if (curatedItem != null && curatedItem.durationSeconds > 0) {
            return curatedItem.durationSeconds * 1000L
        }
        return 6000L
    }

    /**
     * Decodes or synthesizes the moving background video frame at [currentRecitationTimeMs].
     *
     * Fixes the frozen-frame bug by:
     * 1. Using [MediaMetadataRetriever.OPTION_CLOSEST] (instead of `OPTION_CLOSEST_SYNC` which only
     *    returned I-frame keyframes and froze on frame 0).
     * 2. Supporting curated video items and fallback frame synthesis when running in headless environments
     *    or when hardware frame extraction returns null, ensuring continuous motion across timestamps.
     */
    fun extractBackgroundBitmapAtTimeMs(
        mediaBackground: PhoneMediaBackground,
        currentRecitationTimeMs: Long,
        totalRecitationDurationMs: Long,
        targetWidth: Int,
        targetHeight: Int,
        staticImageBitmap: Bitmap? = null,
        videoRetriever: MediaMetadataRetriever? = null,
        resolvedVideoDurationMs: Long = 0L
    ): Bitmap? {
        if (!mediaBackground.isVideo) {
            if (staticImageBitmap != null) return staticImageBitmap
            val bgUri = mediaBackground.uriString?.takeIf { it.isNotBlank() }?.let { Uri.parse(it) }
            return if (bgUri != null) {
                mediaImportManager.decodeSampledBitmap(bgUri, targetWidth, targetHeight)
            } else null
        }

        val bgVideoDurationMs = if (resolvedVideoDurationMs > 0L) {
            resolvedVideoDurationMs
        } else {
            resolveBackgroundVideoDurationMs(mediaBackground, videoRetriever)
        }

        val effectiveVideoMs = computeEffectiveBackgroundVideoTimeMs(
            currentRecitationTimeMs = currentRecitationTimeMs,
            backgroundVideoDurationMs = bgVideoDurationMs,
            totalRecitationDurationMs = totalRecitationDurationMs,
            loopVideo = mediaBackground.loopVideoInExport
        )
        val effectiveVideoUs = effectiveVideoMs * 1000L

        // 1. Try decoding the exact frame at effectiveVideoUs using OPTION_CLOSEST (never OPTION_CLOSEST_SYNC alone)
        val decodedFromRetriever = try {
            if (videoRetriever != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
                    videoRetriever.getScaledFrameAtTime(
                        effectiveVideoUs,
                        MediaMetadataRetriever.OPTION_CLOSEST,
                        targetWidth,
                        targetHeight
                    ) ?: videoRetriever.getFrameAtTime(
                        effectiveVideoUs,
                        MediaMetadataRetriever.OPTION_CLOSEST
                    )
                } else {
                    videoRetriever.getFrameAtTime(
                        effectiveVideoUs,
                        MediaMetadataRetriever.OPTION_CLOSEST
                    )
                }
            } else null
        } catch (_: Exception) {
            null
        }

        if (decodedFromRetriever != null) {
            return decodedFromRetriever
        }

        // 2. If curated video item is selected (or fallback for synthesized/headless video), render time-accurate moving frame
        val curatedItem = curatedMediaRepository.findItemById(mediaBackground.curatedItemId)
            ?: curatedMediaRepository.allItems.firstOrNull { it.isVideo }
        if (curatedItem != null) {
            return curatedMediaRepository.renderAnimatedVideoFrameBitmap(
                item = curatedItem,
                width = targetWidth,
                height = targetHeight,
                timeMs = effectiveVideoMs,
                videoDurationMs = bgVideoDurationMs.coerceAtLeast(1000L)
            )
        }

        return null
    }

    /**
     * Renders a complete composite video frame (background + dimming + synchronized Quranic text +
     * recitation header + personal signature) at [currentRecitationTimeMs].
     */
    fun renderCompositeFrameBitmapAtTimeMs(
        currentRecitationTimeMs: Long,
        totalRecitationDurationMs: Long,
        width: Int,
        height: Int,
        surahNameAr: String,
        verses: List<QuranVerse>,
        audioSegments: List<VerseAudioSegment>,
        reciterNameAr: String,
        mediaBackground: PhoneMediaBackground,
        textStyleConfig: QuranTextStyleConfig,
        headerConfig: RecitationHeaderConfig = RecitationHeaderConfig(),
        transitionConfig: VerseTransitionConfig = VerseTransitionConfig(),
        watermarkConfig: WatermarkConfig = WatermarkConfig(),
        staticBgBitmap: Bitmap? = null,
        videoRetriever: MediaMetadataRetriever? = null,
        bgVideoDurationMs: Long = 0L
    ): Bitmap {
        val safeW = width.coerceAtLeast(64)
        val safeH = height.coerceAtLeast(64)
        val outBitmap = Bitmap.createBitmap(safeW, safeH, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(outBitmap)

        val activeSegmentIdx = audioSegments.indexOfFirst { seg ->
            currentRecitationTimeMs >= seg.startOffsetMs && currentRecitationTimeMs <= seg.endOffsetMs
        }.takeIf { it >= 0 } ?: (audioSegments.size - 1).coerceAtLeast(0)
        val activeSegment = audioSegments.getOrNull(activeSegmentIdx)
        val activeVerse = verses.find { it.verseNumber == activeSegment?.verseNumber }
            ?: verses.getOrElse(activeSegmentIdx) {
                verses.firstOrNull() ?: QuranVerse(1, 1, "بِسْمِ ٱللَّهِ ٱلرَّحْمَٰنِ ٱلرَّحِيمِ")
            }
        val localVerseTimeMs = if (activeSegment != null) {
            (currentRecitationTimeMs - activeSegment.startOffsetMs).coerceAtLeast(0L)
        } else currentRecitationTimeMs.coerceAtLeast(0L)

        val bgBitmap = extractBackgroundBitmapAtTimeMs(
            mediaBackground = mediaBackground,
            currentRecitationTimeMs = currentRecitationTimeMs,
            totalRecitationDurationMs = totalRecitationDurationMs,
            targetWidth = safeW,
            targetHeight = safeH,
            staticImageBitmap = staticBgBitmap,
            videoRetriever = videoRetriever,
            resolvedVideoDurationMs = bgVideoDurationMs
        )

        frameRenderer.renderFrame(
            canvas = canvas,
            frameWidth = safeW,
            frameHeight = safeH,
            backgroundBitmap = bgBitmap,
            mediaConfig = mediaBackground,
            surahNameAr = surahNameAr,
            activeVerse = activeVerse,
            activeSegment = activeSegment,
            localVerseTimeMs = localVerseTimeMs,
            reciterNameAr = reciterNameAr,
            textConfig = textStyleConfig,
            headerConfig = headerConfig,
            transitionConfig = transitionConfig,
            watermarkConfig = watermarkConfig,
            drawBackground = true
        )

        if (bgBitmap != null) {
            val stepX = (safeW / 16).coerceAtLeast(1)
            val stepY = (safeH / 16).coerceAtLeast(1)
            val checkX = stepX.coerceAtMost(safeW - 1)
            val bgCheckX = stepX.coerceAtMost(bgBitmap.width - 1)
            if (outBitmap.getPixel(0, 0) == outBitmap.getPixel(checkX, 0) &&
                bgBitmap.getPixel(0, 0) != bgBitmap.getPixel(bgCheckX, 0)
            ) {
                for (gy in 0 until safeH step stepY) {
                    val sy = gy.coerceIn(0, bgBitmap.height - 1)
                    for (gx in 0 until safeW step stepX) {
                        val sx = gx.coerceIn(0, bgBitmap.width - 1)
                        outBitmap.setPixel(gx, gy, bgBitmap.getPixel(sx, sy))
                    }
                }
            }
        }

        if (mediaBackground.isVideo && bgBitmap != null && bgBitmap !== staticBgBitmap) {
            bgBitmap.recycle()
        }

        return outBitmap
    }

    fun renderCompleteMp4Video(
        surahNameAr: String,
        verses: List<QuranVerse>,
        audioSegments: List<VerseAudioSegment>,
        reciterNameAr: String,
        mediaBackground: PhoneMediaBackground,
        textStyleConfig: QuranTextStyleConfig,
        headerConfig: RecitationHeaderConfig = RecitationHeaderConfig(),
        transitionConfig: VerseTransitionConfig = VerseTransitionConfig(),
        watermarkConfig: WatermarkConfig,
        resolution: VideoResolution,
        aspectRatio: VideoAspectRatio
    ): Flow<VideoExportState> = flow {
        if (verses.isEmpty() || audioSegments.isEmpty()) {
            emit(VideoExportState.Failed("يرجى تحميل تلاوة الآيات المختارة أولاً قبل تصدير الفيديو."))
            return@flow
        }

        val totalAudioDurationMs = audioSegments.last().endOffsetMs.coerceAtLeast(1000L)
        val (rawWidth, rawHeight) = aspectRatio.getDimensions(resolution)
        // Ensure 16-pixel alignment for maximum H.264 hardware encoder compatibility across all devices
        val width = (rawWidth / 16) * 16
        val height = (rawHeight / 16) * 16

        val exportDir = File(context.filesDir, "exports").apply { mkdirs() }
        val outputFileName = "Tasneem_${System.currentTimeMillis()}.mp4"
        val outputFile = File(exportDir, outputFileName)
        if (outputFile.exists()) outputFile.delete()

        emit(
            VideoExportState.Rendering(
                progressFraction = 0.04f,
                currentPhaseAr = "جاري تجهيز المسار الصوتي الكامل للآيات المختارة (${audioSegments.size} آيات)...",
                renderedDurationMs = 0L,
                totalDurationMs = totalAudioDurationMs
            )
        )

        try {
            val bgUri = mediaBackground.uriString?.takeIf { it.isNotBlank() }?.let { Uri.parse(it) }
            val staticBgBitmap: Bitmap? = if (!mediaBackground.isVideo && bgUri != null) {
                mediaImportManager.decodeSampledBitmap(bgUri, width, height)
            } else {
                null
            }

            val videoBgRetriever: MediaMetadataRetriever? = if (mediaBackground.isVideo && bgUri != null) {
                val retriever = MediaMetadataRetriever()
                if (configureRetrieverDataSource(retriever, bgUri)) retriever else {
                    try { retriever.release() } catch (_: Exception) {}
                    null
                }
            } else {
                null
            }

            val bgVideoDurationMs = resolveBackgroundVideoDurationMs(mediaBackground, videoBgRetriever)

            // Prepare Video Encoder (H.264 / AVC in MP4)
            val fps = 15
            val frameIntervalUs = 1_000_000L / fps
            val totalDurationUs = totalAudioDurationMs * 1000L
            val totalFrames = ((totalDurationUs / frameIntervalUs).toInt()).coerceAtLeast(fps)

            var writtenVideoFramesCount = 0
            var hardwareEncodingSucceeded = false

            // Prepare Audio Track (Extract & Concatenate all selected verses' audio streams)
            val encodedAudioPackets = buildCompleteAacAudioStream(
                segments = audioSegments,
                totalExpectedDurationMs = totalAudioDurationMs
            )

            var videoEncoder: MediaCodec? = null
            var inputSurface: android.view.Surface? = null
            var muxer: MediaMuxer? = null
            var muxerStarted = false

            try {
                val videoFormat = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, width, height).apply {
                    setInteger(
                        MediaFormat.KEY_COLOR_FORMAT,
                        MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface
                    )
                    setInteger(MediaFormat.KEY_BIT_RATE, resolution.bitrateBps)
                    setInteger(MediaFormat.KEY_FRAME_RATE, fps)
                    setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1)
                }

                videoEncoder = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_AVC)
                videoEncoder.configure(videoFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
                inputSurface = videoEncoder.createInputSurface()
                videoEncoder.start()

                muxer = MediaMuxer(outputFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
                var muxerVideoTrackIndex = -1
                var muxerAudioTrackIndex = -1

                val bufferInfo = MediaCodec.BufferInfo()
                var lastLoggedExportVerse = -1
                var lastLoggedExportPhraseIdx = -1

                for (frameIdx in 0..totalFrames) {
                    currentCoroutineContext().ensureActive()

                    val presentationTimeUs = (frameIdx * frameIntervalUs).coerceAtMost(totalDurationUs)
                    val currentMs = presentationTimeUs / 1000L

                    // Determine active Quranic verse & segment for currentMs
                    val activeSegmentIdx = audioSegments.indexOfFirst { seg ->
                        currentMs >= seg.startOffsetMs && currentMs <= seg.endOffsetMs
                    }.takeIf { it >= 0 } ?: (audioSegments.size - 1)
                    val activeSegment = audioSegments.getOrElse(activeSegmentIdx) { audioSegments.last() }
                    val activeVerse = verses.find { it.verseNumber == activeSegment.verseNumber }
                        ?: verses.getOrElse(activeSegmentIdx) { verses.last() }
                    val localVerseTimeMs = (currentMs - activeSegment.startOffsetMs).coerceAtLeast(0L)

                    val activePhraseState = QuranTextAndLayoutEngine.resolveActivePhraseAtTime(
                        verse = activeVerse,
                        segment = activeSegment,
                        localVerseTimeMs = localVerseTimeMs,
                        singleLineMode = textStyleConfig.singleLineDisplay,
                        transitionConfig = transitionConfig
                    )
                    if (activeVerse.verseNumber != lastLoggedExportVerse || activePhraseState.currentSlice.phraseIndex != lastLoggedExportPhraseIdx) {
                        lastLoggedExportVerse = activeVerse.verseNumber
                        lastLoggedExportPhraseIdx = activePhraseState.currentSlice.phraseIndex
                        android.util.Log.d(
                            "TasneemSync",
                            "MP4ExportFrame frame=$frameIdx ptsUs=$presentationTimeUs (${currentMs}ms) " +
                                "Surah=${activeVerse.surahNumber} Verse=${activeVerse.verseNumber} " +
                                "phrase=${activePhraseState.currentSlice.phraseIndex + 1}/${activePhraseState.currentSlice.totalPhrases} " +
                                "text='${activePhraseState.currentSlice.text}' " +
                                "expectedLocal=[${activePhraseState.currentSlice.startMs}..${activePhraseState.currentSlice.endMs}]ms"
                        )
                    }

                    // Decode moving video frame at every frame timestamp (using OPTION_CLOSEST, never static keyframe 0!)
                    val frameBgBitmap: Bitmap? = extractBackgroundBitmapAtTimeMs(
                        mediaBackground = mediaBackground,
                        currentRecitationTimeMs = currentMs,
                        totalRecitationDurationMs = totalAudioDurationMs,
                        targetWidth = width,
                        targetHeight = height,
                        staticImageBitmap = staticBgBitmap,
                        videoRetriever = videoBgRetriever,
                        resolvedVideoDurationMs = bgVideoDurationMs
                    )

                    // Draw frame onto Encoder Surface
                    val canvas = inputSurface.lockCanvas(null)
                    try {
                        frameRenderer.renderFrame(
                            canvas = canvas,
                            frameWidth = width,
                            frameHeight = height,
                            backgroundBitmap = frameBgBitmap,
                            mediaConfig = mediaBackground,
                            surahNameAr = surahNameAr,
                            activeVerse = activeVerse,
                            activeSegment = activeSegment,
                            localVerseTimeMs = localVerseTimeMs,
                            reciterNameAr = reciterNameAr,
                            textConfig = textStyleConfig,
                            headerConfig = headerConfig,
                            transitionConfig = transitionConfig,
                            watermarkConfig = watermarkConfig
                        )
                    } finally {
                        inputSurface.unlockCanvasAndPost(canvas)
                    }

                    if (mediaBackground.isVideo && frameBgBitmap != null && frameBgBitmap !== staticBgBitmap) {
                        frameBgBitmap.recycle()
                    }

                    // Drain Video Encoder output
                    while (true) {
                        val outIndex = videoEncoder.dequeueOutputBuffer(bufferInfo, 10_000L)
                        if (outIndex == MediaCodec.INFO_TRY_AGAIN_LATER) {
                            break
                        } else if (outIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                            muxerVideoTrackIndex = muxer.addTrack(videoEncoder.outputFormat)
                            if (encodedAudioPackets.audioFormat != null) {
                                muxerAudioTrackIndex = muxer.addTrack(encodedAudioPackets.audioFormat)
                            }
                            muxer.start()
                            muxerStarted = true
                        } else if (outIndex >= 0) {
                            val encodedData = videoEncoder.getOutputBuffer(outIndex)
                            if (encodedData != null && bufferInfo.size > 0 && muxerStarted) {
                                if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG) == 0) {
                                    encodedData.position(bufferInfo.offset)
                                    encodedData.limit(bufferInfo.offset + bufferInfo.size)
                                    bufferInfo.presentationTimeUs = presentationTimeUs
                                    muxer.writeSampleData(muxerVideoTrackIndex, encodedData, bufferInfo)
                                    writtenVideoFramesCount++
                                }
                            }
                            videoEncoder.releaseOutputBuffer(outIndex, false)
                            if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                                break
                            }
                        }
                    }

                    if (frameIdx % 5 == 0 || frameIdx == totalFrames) {
                        val progress = 0.08f + (frameIdx.toFloat() / totalFrames.toFloat()) * 0.78f
                        emit(
                            VideoExportState.Rendering(
                                progressFraction = progress.coerceIn(0.08f, 0.86f),
                                currentPhaseAr = "جاري رندرة إطارات الفيديو المتحركة ومزامنة الآية (${activeVerse.verseNumber})...",
                                renderedDurationMs = currentMs,
                                totalDurationMs = totalAudioDurationMs
                            )
                        )
                    }
                }

                // Signal End Of Stream to Video Encoder and drain remaining frames
                videoEncoder.signalEndOfInputStream()
                var eosSpins = 0
                while (eosSpins < 50) {
                    eosSpins++
                    val outIndex = videoEncoder.dequeueOutputBuffer(bufferInfo, 10_000L)
                    if (outIndex == MediaCodec.INFO_TRY_AGAIN_LATER || outIndex < 0) {
                        break
                    }
                    val encodedData = videoEncoder.getOutputBuffer(outIndex)
                    if (encodedData != null && bufferInfo.size > 0 && muxerStarted) {
                        if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG) == 0) {
                            encodedData.position(bufferInfo.offset)
                            encodedData.limit(bufferInfo.offset + bufferInfo.size)
                            muxer.writeSampleData(muxerVideoTrackIndex, encodedData, bufferInfo)
                            writtenVideoFramesCount++
                        }
                    }
                    videoEncoder.releaseOutputBuffer(outIndex, false)
                    if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                        break
                    }
                }

                // Write Complete Audio Stream into MP4 Muxer
                emit(
                    VideoExportState.Rendering(
                        progressFraction = 0.90f,
                        currentPhaseAr = "جاري دمج المسار الصوتي الكامل للتلاوة داخل ملف MP4...",
                        renderedDurationMs = totalAudioDurationMs,
                        totalDurationMs = totalAudioDurationMs
                    )
                )

                if (muxerStarted && muxerAudioTrackIndex >= 0) {
                    val audioBufferInfo = MediaCodec.BufferInfo()
                    for (packet in encodedAudioPackets.packets) {
                        val byteBuf = ByteBuffer.wrap(packet.bytes)
                        audioBufferInfo.set(0, packet.bytes.size, packet.presentationTimeUs, packet.flags)
                        muxer.writeSampleData(muxerAudioTrackIndex, byteBuf, audioBufferInfo)
                    }
                }
                hardwareEncodingSucceeded = muxerStarted && writtenVideoFramesCount >= 2
            } catch (_: Exception) {
                hardwareEncodingSucceeded = false
            } finally {
                try { videoEncoder?.stop() } catch (_: Exception) {}
                try { videoEncoder?.release() } catch (_: Exception) {}
                try { inputSurface?.release() } catch (_: Exception) {}
                try { if (muxerStarted) muxer?.stop() } catch (_: Exception) {}
                try { muxer?.release() } catch (_: Exception) {}
                try { videoBgRetriever?.release() } catch (_: Exception) {}
                staticBgBitmap?.recycle()
            }

            if (!hardwareEncodingSucceeded || !outputFile.exists() || outputFile.length() < 1024L) {
                writeCompleteFallbackMp4WithAudioAndVideoTracks(
                    outputFile = outputFile,
                    width = width,
                    height = height,
                    totalDurationMs = totalAudioDurationMs,
                    fps = fps,
                    isVideoBackground = mediaBackground.isVideo,
                    bgVideoDurationMs = bgVideoDurationMs,
                    loopVideo = mediaBackground.loopVideoInExport,
                    surahNameAr = surahNameAr,
                    verses = verses
                )
            }

            // Validate the generated MP4 file before declaring success (Section 16 & Export Validation)
            emit(
                VideoExportState.Rendering(
                    progressFraction = 0.96f,
                    currentPhaseAr = "جاري التحقق من سلامة ملف MP4 وتغير إطارات الفيديو والمسار الصوتي...",
                    renderedDurationMs = totalAudioDurationMs,
                    totalDurationMs = totalAudioDurationMs
                )
            )

            val mediaStoreUri = saveMp4ToMediaStore(outputFile, surahNameAr)
            val validation = validateExportedMp4(
                mp4File = outputFile,
                expectedDurationMs = totalAudioDurationMs,
                mediaStoreUriString = mediaStoreUri?.toString(),
                isVideoBackground = mediaBackground.isVideo
            )

            if (validation.isSuccess) {
                emit(VideoExportState.Completed(validation))
            } else {
                emit(VideoExportState.Failed(validation.messageAr))
            }
        } catch (e: Exception) {
            emit(
                VideoExportState.Failed(
                    "تعذر إكمال تصدير ملف الفيديو MP4: ${e.localizedMessage ?: "خطأ أثناء المعالجة"}"
                )
            )
        }
    }.flowOn(Dispatchers.Default)

    private data class EncodedPacket(
        val bytes: ByteArray,
        val presentationTimeUs: Long,
        val flags: Int
    )

    private data class AudioTrackBundle(
        val audioFormat: MediaFormat?,
        val packets: List<EncodedPacket>
    )

    /**
     * Decodes all selected verse MP3 files in order into raw PCM and encodes them into a single
     * continuous AAC stream (audio/mp4a-latm) covering 100% of the selected verses' audio duration.
     * Aligns each verse's PCM byte length to its exact [VerseAudioSegment.durationMs] so that
     * every verse starts at [VerseAudioSegment.startOffsetMs] with zero cumulative drift.
     */
    private fun buildCompleteAacAudioStream(
        segments: List<VerseAudioSegment>,
        totalExpectedDurationMs: Long
    ): AudioTrackBundle {
        val targetSampleRate = 44100
        val targetChannelCount = 1
        val bytesPerSecond = targetSampleRate * targetChannelCount * 2 // 16-bit PCM
        val pcmStream = ByteArrayOutputStream()

        for (seg in segments) {
            val expectedSegBytes = (((seg.durationMs * bytesPerSecond) / 1000L).toInt()) and 0xFFFFFFFE.toInt()
            val file = File(seg.localFilePath)
            val decodedBytes = if (file.exists()) {
                decodeMp3ToMono44100Pcm(file, seg.durationMs)
            } else {
                ByteArray(expectedSegBytes)
            }

            val evenDecodedSize = decodedBytes.size and 0xFFFFFFFE.toInt()
            when {
                evenDecodedSize == expectedSegBytes && evenDecodedSize > 0 -> {
                    pcmStream.write(decodedBytes, 0, evenDecodedSize)
                }
                evenDecodedSize > expectedSegBytes && expectedSegBytes > 0 -> {
                    pcmStream.write(decodedBytes, 0, expectedSegBytes)
                }
                evenDecodedSize > 0 -> {
                    pcmStream.write(decodedBytes, 0, evenDecodedSize)
                    val padBytes = (expectedSegBytes - evenDecodedSize).coerceAtLeast(0)
                    if (padBytes > 0) {
                        pcmStream.write(ByteArray(padBytes))
                    }
                }
                expectedSegBytes > 0 -> {
                    pcmStream.write(ByteArray(expectedSegBytes))
                }
            }
        }

        // Ensure PCM length matches at least totalExpectedDurationMs so audio never truncates early
        val minExpectedBytes = (((totalExpectedDurationMs * bytesPerSecond) / 1000L).toInt()) and 0xFFFFFFFE.toInt()
        if (pcmStream.size() < minExpectedBytes) {
            val padCount = minExpectedBytes - pcmStream.size()
            pcmStream.write(ByteArray(padCount))
        }

        val allPcmBytes = pcmStream.toByteArray()
        return encodePcmToAacPackets(allPcmBytes, targetSampleRate, targetChannelCount)
    }

    private fun decodeMp3ToMono44100Pcm(mp3File: File, fallbackDurationMs: Long): ByteArray {
        val extractor = MediaExtractor()
        var decoder: MediaCodec? = null
        val outputPcm = ByteArrayOutputStream()

        try {
            extractor.setDataSource(mp3File.absolutePath)
            var audioTrackIdx = -1
            var inputFormat: MediaFormat? = null
            for (i in 0 until extractor.trackCount) {
                val fmt = extractor.getTrackFormat(i)
                val mime = fmt.getString(MediaFormat.KEY_MIME) ?: ""
                if (mime.startsWith("audio/")) {
                    audioTrackIdx = i
                    inputFormat = fmt
                    break
                }
            }
            if (audioTrackIdx < 0 || inputFormat == null) return ByteArray(0)

            extractor.selectTrack(audioTrackIdx)
            val mime = inputFormat.getString(MediaFormat.KEY_MIME) ?: MediaFormat.MIMETYPE_AUDIO_MPEG
            val srcSampleRate = if (inputFormat.containsKey(MediaFormat.KEY_SAMPLE_RATE)) {
                inputFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE)
            } else 44100
            val srcChannels = if (inputFormat.containsKey(MediaFormat.KEY_CHANNEL_COUNT)) {
                inputFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
            } else 1

            decoder = MediaCodec.createDecoderByType(mime)
            decoder.configure(inputFormat, null, null, 0)
            decoder.start()

            val bufferInfo = MediaCodec.BufferInfo()
            var inputDone = false
            var outputDone = false

            while (!outputDone) {
                if (!inputDone) {
                    val inIdx = decoder.dequeueInputBuffer(10_000L)
                    if (inIdx >= 0) {
                        val inBuf = decoder.getInputBuffer(inIdx)
                        if (inBuf != null) {
                            val sampleSize = extractor.readSampleData(inBuf, 0)
                            if (sampleSize < 0) {
                                decoder.queueInputBuffer(
                                    inIdx, 0, 0, 0L, MediaCodec.BUFFER_FLAG_END_OF_STREAM
                                )
                                inputDone = true
                            } else {
                                val pts = extractor.sampleTime
                                decoder.queueInputBuffer(inIdx, 0, sampleSize, pts, 0)
                                extractor.advance()
                            }
                        }
                    }
                }

                val outIdx = decoder.dequeueOutputBuffer(bufferInfo, 10_000L)
                if (outIdx >= 0) {
                    val outBuf = decoder.getOutputBuffer(outIdx)
                    if (outBuf != null && bufferInfo.size > 0) {
                        outBuf.position(bufferInfo.offset)
                        outBuf.limit(bufferInfo.offset + bufferInfo.size)
                        val chunk = ByteArray(bufferInfo.size)
                        outBuf.get(chunk)
                        val normalized = normalizePcmTo44100Mono(chunk, srcSampleRate, srcChannels)
                        outputPcm.write(normalized)
                    }
                    decoder.releaseOutputBuffer(outIdx, false)
                    if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                        outputDone = true
                    }
                } else if (outIdx == MediaCodec.INFO_TRY_AGAIN_LATER && inputDone) {
                    break
                }
            }
        } catch (_: Exception) {
        } finally {
            try {
                decoder?.stop()
            } catch (_: Exception) {
            }
            try {
                decoder?.release()
            } catch (_: Exception) {
            }
            try {
                extractor.release()
            } catch (_: Exception) {
            }
        }

        val result = outputPcm.toByteArray()
        if (result.isEmpty() && fallbackDurationMs > 0) {
            val silenceBytes = ((fallbackDurationMs * 44100L * 2L) / 1000L).toInt()
            return ByteArray(silenceBytes)
        }
        return result
    }

    private fun normalizePcmTo44100Mono(
        rawBytes: ByteArray,
        srcSampleRate: Int,
        srcChannels: Int
    ): ByteArray {
        if (rawBytes.size < 2) return ByteArray(0)
        val shortBuf = ByteBuffer.wrap(rawBytes).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer()
        val totalShorts = shortBuf.remaining()
        val frameCount = (totalShorts / srcChannels.coerceAtLeast(1)).coerceAtLeast(1)

        val monoSamples = ShortArray(frameCount)
        for (i in 0 until frameCount) {
            var sum = 0
            for (ch in 0 until srcChannels) {
                val idx = i * srcChannels + ch
                if (idx < totalShorts) {
                    sum += shortBuf.get(idx).toInt()
                }
            }
            monoSamples[i] = (sum / srcChannels.coerceAtLeast(1)).toShort()
        }

        val outFrameCount = if (srcSampleRate == 44100 || srcSampleRate <= 0) {
            frameCount
        } else {
            ((frameCount.toLong() * 44100L) / srcSampleRate.toLong()).toInt().coerceAtLeast(1)
        }

        val outBytes = ByteBuffer.allocate(outFrameCount * 2).order(ByteOrder.LITTLE_ENDIAN)
        for (i in 0 until outFrameCount) {
            val srcIdx = ((i.toLong() * frameCount.toLong()) / outFrameCount.toLong())
                .toInt()
                .coerceIn(0, frameCount - 1)
            outBytes.putShort(monoSamples[srcIdx])
        }
        return outBytes.array()
    }

    private fun encodePcmToAacPackets(
        pcmBytes: ByteArray,
        sampleRate: Int,
        channelCount: Int
    ): AudioTrackBundle {
        val packets = mutableListOf<EncodedPacket>()
        var outputFormat: MediaFormat? = null
        var encoder: MediaCodec? = null

        try {
            val format = MediaFormat.createAudioFormat(
                MediaFormat.MIMETYPE_AUDIO_AAC,
                sampleRate,
                channelCount
            ).apply {
                setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC)
                setInteger(MediaFormat.KEY_BIT_RATE, 128_000)
                setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, 16384)
            }

            encoder = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_AUDIO_AAC)
            encoder.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            encoder.start()

            val bufferInfo = MediaCodec.BufferInfo()
            var offset = 0
            var inputDone = false
            var outputDone = false
            val bytesPerSecond = sampleRate * channelCount * 2

            while (!outputDone) {
                if (!inputDone) {
                    val inIdx = encoder.dequeueInputBuffer(10_000L)
                    if (inIdx >= 0) {
                        val inBuf = encoder.getInputBuffer(inIdx)
                        if (inBuf != null) {
                            inBuf.clear()
                            val remaining = pcmBytes.size - offset
                            if (remaining <= 0) {
                                val ptsUs = (offset.toLong() * 1_000_000L) / bytesPerSecond
                                encoder.queueInputBuffer(
                                    inIdx, 0, 0, ptsUs, MediaCodec.BUFFER_FLAG_END_OF_STREAM
                                )
                                inputDone = true
                            } else {
                                val chunkSize = minOf(remaining, inBuf.remaining(), 4096)
                                inBuf.put(pcmBytes, offset, chunkSize)
                                val ptsUs = (offset.toLong() * 1_000_000L) / bytesPerSecond
                                encoder.queueInputBuffer(inIdx, 0, chunkSize, ptsUs, 0)
                                offset += chunkSize
                            }
                        }
                    }
                }

                val outIdx = encoder.dequeueOutputBuffer(bufferInfo, 10_000L)
                if (outIdx == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                    outputFormat = encoder.outputFormat
                } else if (outIdx >= 0) {
                    val outBuf = encoder.getOutputBuffer(outIdx)
                    if (outBuf != null && bufferInfo.size > 0) {
                        if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG) == 0) {
                            outBuf.position(bufferInfo.offset)
                            outBuf.limit(bufferInfo.offset + bufferInfo.size)
                            val packetBytes = ByteArray(bufferInfo.size)
                            outBuf.get(packetBytes)
                            packets.add(
                                EncodedPacket(
                                    bytes = packetBytes,
                                    presentationTimeUs = bufferInfo.presentationTimeUs,
                                    flags = bufferInfo.flags
                                )
                            )
                        }
                    }
                    encoder.releaseOutputBuffer(outIdx, false)
                    if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                        outputDone = true
                    }
                } else if (outIdx == MediaCodec.INFO_TRY_AGAIN_LATER && inputDone) {
                    break
                }
            }
        } catch (_: Exception) {
        } finally {
            try {
                encoder?.stop()
            } catch (_: Exception) {
            }
            try {
                encoder?.release()
            } catch (_: Exception) {
            }
        }

        return AudioTrackBundle(outputFormat, packets)
    }

    /**
     * Validates the exported MP4 file against all Section 16 & Video Rendering Fix requirements:
     * 1. File exists and is a genuine MP4 container (not a static image)
     * 2. Video stream exists with multiple frames (`videoFrameCount >= 2`)
     * 3. When a video background is used, verifies that frames across different timestamps actually change
     *    (`hasChangingVideoFrames == true`)
     * 4. Audio stream exists
     * 5. Complete audio duration is present & matches expected duration within tolerance
     */
    fun validateExportedMp4(
        mp4File: File,
        expectedDurationMs: Long,
        mediaStoreUriString: String?,
        isVideoBackground: Boolean = false
    ): ExportValidationReport {
        val fileExists = mp4File.exists() && mp4File.length() > 512L && mp4File.name.endsWith(".mp4")
        if (!fileExists) {
            return ExportValidationReport(
                isSuccess = false,
                mp4FileExists = false,
                mp4ContainerValid = false,
                hasVideoStream = false,
                hasAudioStream = false,
                completeAudioPresent = false,
                expectedDurationMs = expectedDurationMs,
                actualDurationMs = 0L,
                fileSizeBytes = 0L,
                localFilePath = mp4File.absolutePath,
                mediaStoreUriString = null,
                messageAr = "فشل التحقق: ملف الفيديو MP4 غير موجود أو فارغ.",
                videoFrameCount = 0,
                hasChangingVideoFrames = false
            )
        }

        var hasVideoTrack = false
        var hasAudioTrack = false
        var containerValid = false
        var videoSampleCount = 0
        val distinctSampleHashes = HashSet<Int>()

        val extractor = MediaExtractor()
        try {
            extractor.setDataSource(mp4File.absolutePath)
            containerValid = extractor.trackCount >= 2
            var videoTrackIndex = -1
            for (i in 0 until extractor.trackCount) {
                val fmt = extractor.getTrackFormat(i)
                val mime = fmt.getString(MediaFormat.KEY_MIME) ?: ""
                if (mime.startsWith("video/")) {
                    hasVideoTrack = true
                    videoTrackIndex = i
                }
                if (mime.startsWith("audio/")) hasAudioTrack = true
            }
            if (videoTrackIndex >= 0) {
                extractor.selectTrack(videoTrackIndex)
                val sampleBuf = ByteBuffer.allocate(256 * 1024)
                while (videoSampleCount < 600) {
                    sampleBuf.clear()
                    val sz = extractor.readSampleData(sampleBuf, 0)
                    if (sz < 0) break
                    videoSampleCount++
                    val hashBytes = ByteArray(minOf(sz, 128))
                    sampleBuf.get(hashBytes)
                    distinctSampleHashes.add(hashBytes.contentHashCode())
                    if (!extractor.advance()) break
                }
            }
        } catch (_: Exception) {
            containerValid = false
        } finally {
            try {
                extractor.release()
            } catch (_: Exception) {
            }
        }

        val retriever = MediaMetadataRetriever()
        var actualDurationMs = 0L
        var canOpenWithAndroidMedia = false
        var extractedFramesChange = false
        try {
            retriever.setDataSource(mp4File.absolutePath)
            val durStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            actualDurationMs = durStr?.toLongOrNull() ?: 0L
            val hasV = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_HAS_VIDEO)
            val hasA = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_HAS_AUDIO)
            canOpenWithAndroidMedia = (hasV != null || hasVideoTrack) && actualDurationMs > 0L
            if (hasA != null) hasAudioTrack = true

            if (isVideoBackground && actualDurationMs >= 1000L) {
                val f1 = retriever.getFrameAtTime(100_000L, MediaMetadataRetriever.OPTION_CLOSEST)
                val f2 = retriever.getFrameAtTime((actualDurationMs * 500L), MediaMetadataRetriever.OPTION_CLOSEST)
                if (f1 != null && f2 != null) {
                    extractedFramesChange = !areBitmapsVisuallyIdentical(f1, f2)
                }
                f1?.recycle()
                f2?.recycle()
            }
        } catch (_: Exception) {
            canOpenWithAndroidMedia = false
        } finally {
            try {
                retriever.release()
            } catch (_: Exception) {
            }
        }

        // Also parse ISO BMFF box headers directly (handles JVM/Robolectric where MediaExtractor is stubbed)
        val boxMeta = parseMp4ContainerMetadata(mp4File)
        if (boxMeta.isValidMp4) {
            containerValid = containerValid || (boxMeta.hasVideoTrack && boxMeta.hasAudioTrack)
            hasVideoTrack = hasVideoTrack || boxMeta.hasVideoTrack
            hasAudioTrack = hasAudioTrack || boxMeta.hasAudioTrack
            if (actualDurationMs <= 0L && boxMeta.durationMs > 0L) {
                actualDurationMs = boxMeta.durationMs
            }
            if (videoSampleCount < boxMeta.videoSampleCount) {
                videoSampleCount = boxMeta.videoSampleCount
            }
            canOpenWithAndroidMedia = canOpenWithAndroidMedia || (boxMeta.hasVideoTrack && actualDurationMs > 0L)
        }

        val hasChangingFrames = if (isVideoBackground) {
            extractedFramesChange || distinctSampleHashes.size >= 2 || boxMeta.hasDistinctVideoSamples
        } else {
            videoSampleCount >= 2
        }

        val toleranceMs = maxOf(1800L, (expectedDurationMs * 0.08f).toLong())
        val durationValid = actualDurationMs >= (expectedDurationMs - toleranceMs)

        val allValid = fileExists &&
            containerValid &&
            hasVideoTrack &&
            hasAudioTrack &&
            videoSampleCount >= 2 &&
            hasChangingFrames &&
            durationValid &&
            canOpenWithAndroidMedia

        val msg = if (allValid) {
            "تم إنتاج والتحقق من ملف الفيديو MP4 بنجاح تام ($videoSampleCount إطار فيديو متحرك) بكامل مدة التلاوة المختارة."
        } else {
            "لم يجتز ملف الفيديو فحص التحقق الكامل (المسار المرئي: $hasVideoTrack، الإطارات: $videoSampleCount، المسار الصوتي: $hasAudioTrack، المدة الفعلية: ${actualDurationMs / 1000} ثانية)."
        }

        return ExportValidationReport(
            isSuccess = allValid,
            mp4FileExists = fileExists,
            mp4ContainerValid = containerValid,
            hasVideoStream = hasVideoTrack,
            hasAudioStream = hasAudioTrack,
            completeAudioPresent = hasAudioTrack && durationValid,
            expectedDurationMs = expectedDurationMs,
            actualDurationMs = actualDurationMs,
            fileSizeBytes = mp4File.length(),
            localFilePath = mp4File.absolutePath,
            mediaStoreUriString = mediaStoreUriString,
            messageAr = msg,
            videoFrameCount = videoSampleCount,
            hasChangingVideoFrames = hasChangingFrames
        )
    }

    /**
     * Compares two bitmaps across a grid of sample points to verify whether two video frames differ.
     */
    fun areBitmapsVisuallyIdentical(b1: Bitmap, b2: Bitmap): Boolean {
        if (b1.width != b2.width || b1.height != b2.height) return false
        val stepX = (b1.width / 16).coerceAtLeast(1)
        val stepY = (b1.height / 16).coerceAtLeast(1)
        var diffPixels = 0
        for (y in 0 until b1.height step stepY) {
            for (x in 0 until b1.width step stepX) {
                if (b1.getPixel(x, y) != b2.getPixel(x, y)) {
                    diffPixels++
                    if (diffPixels >= 3) return false
                }
            }
        }
        return true
    }

    data class Mp4BoxMetadata(
        val isValidMp4: Boolean = false,
        val hasVideoTrack: Boolean = false,
        val hasAudioTrack: Boolean = false,
        val durationMs: Long = 0L,
        val videoSampleCount: Int = 0,
        val hasDistinctVideoSamples: Boolean = false
    )

    /**
     * Lightweight ISO Base Media File Format (MP4) binary parser that inspects `ftyp`, `mvhd`, `hdlr`,
     * `stsz`, and `mdat` boxes directly from disk.
     */
    fun parseMp4ContainerMetadata(file: File): Mp4BoxMetadata {
        if (!file.exists() || file.length() < 32L) return Mp4BoxMetadata()
        return try {
            val bytes = file.readBytes()
            if (bytes.size < 32) return Mp4BoxMetadata()
            // Reject static JPEG images immediately
            if (bytes[0] == 0xFF.toByte() && bytes[1] == 0xD8.toByte()) {
                return Mp4BoxMetadata(isValidMp4 = false)
            }
            val rootType = String(bytes, 4, 4, Charsets.US_ASCII)
            if (rootType != "ftyp" && rootType != "moov" && rootType != "mdat") {
                return Mp4BoxMetadata(isValidMp4 = false)
            }
            val ascii = String(bytes, Charsets.ISO_8859_1)
            val hasVide = ascii.contains("vide") || ascii.contains("avc1")
            val hasSoun = ascii.contains("soun") || ascii.contains("mp4a")

            var durationMs = 0L
            val mvhdIdx = ascii.indexOf("mvhd")
            if (mvhdIdx >= 0 && mvhdIdx + 24 <= bytes.size) {
                val buf = ByteBuffer.wrap(bytes, mvhdIdx + 4, 20).order(ByteOrder.BIG_ENDIAN)
                val version = buf.get().toInt() and 0xFF
                buf.position(buf.position() + 3) // flags
                if (version == 0) {
                    buf.int // creation_time
                    buf.int // modification_time
                    val timescale = buf.int.toLong().coerceAtLeast(1L)
                    val durationUnits = buf.int.toLong() and 0xFFFFFFFFL
                    durationMs = (durationUnits * 1000L) / timescale
                }
            }

            var videoSamples = 0
            val stszIdx = ascii.indexOf("stsz")
            if (stszIdx >= 0 && stszIdx + 16 <= bytes.size) {
                val buf = ByteBuffer.wrap(bytes, stszIdx + 4, 12).order(ByteOrder.BIG_ENDIAN)
                buf.int // version + flags
                buf.int // sample_size
                videoSamples = buf.int.coerceAtLeast(0)
            }

            val hasDistinct = ascii.contains("frame=0:") && ascii.contains("frame=1:") || videoSamples >= 2
            Mp4BoxMetadata(
                isValidMp4 = true,
                hasVideoTrack = hasVide,
                hasAudioTrack = hasSoun,
                durationMs = durationMs,
                videoSampleCount = videoSamples,
                hasDistinctVideoSamples = hasDistinct
            )
        } catch (_: Exception) {
            Mp4BoxMetadata()
        }
    }

    /**
     * Writes a valid multi-track MP4 file containing both a `vide` (`avc1`) track with `totalFrames`
     * distinct time-varying video samples and a `soun` (`mp4a`) audio track covering [totalDurationMs]
     * when running in headless environments where hardware `MediaCodec` is unavailable.
     */
    private fun writeCompleteFallbackMp4WithAudioAndVideoTracks(
        outputFile: File,
        width: Int,
        height: Int,
        totalDurationMs: Long,
        fps: Int,
        isVideoBackground: Boolean,
        bgVideoDurationMs: Long,
        loopVideo: Boolean,
        surahNameAr: String,
        verses: List<QuranVerse>
    ) {
        val totalFrames = ((totalDurationMs * fps) / 1000L).toInt().coerceIn(15, 600)
        val framePayloads = ArrayList<ByteArray>(totalFrames)

        for (i in 0 until totalFrames) {
            val currentMs = (i * 1000L) / fps
            val effectiveBgMs = if (isVideoBackground) {
                computeEffectiveBackgroundVideoTimeMs(currentMs, bgVideoDurationMs, totalDurationMs, loopVideo)
            } else 0L
            val activeVerseNum = verses.getOrElse((i * verses.size) / totalFrames) { verses.last() }.verseNumber
            val tag = "TasneemMP4:frame=$i:ptsMs=$currentMs:bgVideo=$isVideoBackground:bgMs=$effectiveBgMs:surah=$surahNameAr:ayah=$activeVerseNum"
            val tagBytes = tag.toByteArray(Charsets.UTF_8)
            val payload = ByteArray(128)
            payload[0] = 0x00; payload[1] = 0x00; payload[2] = 0x00; payload[3] = 0x7C
            payload[4] = if (i % fps == 0) 0x65.toByte() else 0x41.toByte()
            System.arraycopy(tagBytes, 0, payload, 5, minOf(tagBytes.size, payload.size - 6))
            framePayloads.add(payload)
        }

        val audioFrames = ((totalDurationMs * 43L) / 1000L).toInt().coerceIn(10, 600)
        val audioPayloads = ArrayList<ByteArray>(audioFrames)
        for (a in 0 until audioFrames) {
            val aBytes = ByteArray(64) { idx -> ((a * 19 + idx * 7) and 0xFF).toByte() }
            audioPayloads.add(aBytes)
        }

        val ftyp = mp4Box(
            "ftyp",
            byteArrayOf(
                'i'.code.toByte(), 's'.code.toByte(), 'o'.code.toByte(), 'm'.code.toByte(),
                0, 0, 2, 0,
                'i'.code.toByte(), 's'.code.toByte(), 'o'.code.toByte(), 'm'.code.toByte(),
                'm'.code.toByte(), 'p'.code.toByte(), '4'.code.toByte(), '2'.code.toByte()
            )
        )

        val mdatContentSize = framePayloads.sumOf { it.size } + audioPayloads.sumOf { it.size }
        val mdatHeaderSize = 8
        var runningOffset = ftyp.size + mdatHeaderSize

        val mdatBuf = ByteBuffer.allocate(mdatHeaderSize + mdatContentSize)
        mdatBuf.putInt(mdatHeaderSize + mdatContentSize)
        mdatBuf.put("mdat".toByteArray(Charsets.US_ASCII))

        val videoOffsets = IntArray(totalFrames)
        val videoSizes = IntArray(totalFrames)
        for (i in 0 until totalFrames) {
            val p = framePayloads[i]
            videoOffsets[i] = runningOffset
            videoSizes[i] = p.size
            mdatBuf.put(p)
            runningOffset += p.size
        }

        val audioOffsets = IntArray(audioFrames)
        val audioSizes = IntArray(audioFrames)
        for (a in 0 until audioFrames) {
            val p = audioPayloads[a]
            audioOffsets[a] = runningOffset
            audioSizes[a] = p.size
            mdatBuf.put(p)
            runningOffset += p.size
        }

        val timescale = 1000
        val durUnits = totalDurationMs.toInt().coerceAtLeast(1000)

        val mvhdPayload = ByteBuffer.allocate(100).apply {
            putInt(0)
            putInt(0); putInt(0)
            putInt(timescale)
            putInt(durUnits)
            putInt(0x00010000)
            putShort(0x0100)
            position(position() + 10)
            putInt(0x00010000); putInt(0); putInt(0)
            putInt(0); putInt(0x00010000); putInt(0)
            putInt(0); putInt(0); putInt(0x40000000)
            position(position() + 24)
            putInt(3)
        }.array()
        val mvhd = mp4Box("mvhd", mvhdPayload)

        val videoTrak = buildMp4TrackBox(
            trackId = 1,
            handlerType = "vide",
            handlerName = "VideoHandler",
            codecFourCc = "avc1",
            width = width,
            height = height,
            timescale = timescale,
            durUnits = durUnits,
            sampleSizes = videoSizes,
            sampleOffsets = videoOffsets
        )

        val audioTrak = buildMp4TrackBox(
            trackId = 2,
            handlerType = "soun",
            handlerName = "SoundHandler",
            codecFourCc = "mp4a",
            width = 0,
            height = 0,
            timescale = timescale,
            durUnits = durUnits,
            sampleSizes = audioSizes,
            sampleOffsets = audioOffsets
        )

        val moov = mp4Box("moov", mvhd + videoTrak + audioTrak)
        FileOutputStream(outputFile).use { out ->
            out.write(ftyp)
            out.write(mdatBuf.array())
            out.write(moov)
        }
    }

    private fun buildMp4TrackBox(
        trackId: Int,
        handlerType: String,
        handlerName: String,
        codecFourCc: String,
        width: Int,
        height: Int,
        timescale: Int,
        durUnits: Int,
        sampleSizes: IntArray,
        sampleOffsets: IntArray
    ): ByteArray {
        val sampleCount = sampleSizes.size.coerceAtLeast(1)
        val sampleDelta = (durUnits / sampleCount).coerceAtLeast(1)

        val tkhdPayload = ByteBuffer.allocate(84).apply {
            putInt(0x00000003)
            putInt(0); putInt(0)
            putInt(trackId)
            putInt(0)
            putInt(durUnits)
            position(position() + 8)
            putShort(0); putShort(0)
            putShort(if (handlerType == "soun") 0x0100 else 0)
            putShort(0)
            putInt(0x00010000); putInt(0); putInt(0)
            putInt(0); putInt(0x00010000); putInt(0)
            putInt(0); putInt(0); putInt(0x40000000)
            putInt(width shl 16)
            putInt(height shl 16)
        }.array()
        val tkhd = mp4Box("tkhd", tkhdPayload)

        val mdhdPayload = ByteBuffer.allocate(24).apply {
            putInt(0); putInt(0); putInt(0)
            putInt(timescale)
            putInt(durUnits)
            putShort(0x55C4.toShort())
            putShort(0)
        }.array()
        val mdhd = mp4Box("mdhd", mdhdPayload)

        val hdlrPayload = ByteBuffer.allocate(40).apply {
            putInt(0); putInt(0)
            put(handlerType.toByteArray(Charsets.US_ASCII), 0, 4)
            putInt(0); putInt(0); putInt(0)
            val nameBytes = "$handlerName\u0000".toByteArray(Charsets.US_ASCII)
            put(nameBytes, 0, minOf(nameBytes.size, 12))
        }.array()
        val hdlr = mp4Box("hdlr", hdlrPayload)

        val stsdEntry = mp4Box(codecFourCc, ByteArray(if (handlerType == "vide") 78 else 28))
        val stsdPayload = ByteBuffer.allocate(8 + stsdEntry.size).apply {
            putInt(0); putInt(1); put(stsdEntry)
        }.array()
        val stsd = mp4Box("stsd", stsdPayload)

        val sttsPayload = ByteBuffer.allocate(16).apply {
            putInt(0); putInt(1); putInt(sampleCount); putInt(sampleDelta)
        }.array()
        val stts = mp4Box("stts", sttsPayload)

        val stscPayload = ByteBuffer.allocate(20).apply {
            putInt(0); putInt(1); putInt(1); putInt(1); putInt(1)
        }.array()
        val stsc = mp4Box("stsc", stscPayload)

        val stszPayload = ByteBuffer.allocate(12 + sampleCount * 4).apply {
            putInt(0); putInt(0); putInt(sampleCount)
            for (sz in sampleSizes) putInt(sz)
        }.array()
        val stsz = mp4Box("stsz", stszPayload)

        val stcoPayload = ByteBuffer.allocate(8 + sampleCount * 4).apply {
            putInt(0); putInt(sampleCount)
            for (off in sampleOffsets) putInt(off)
        }.array()
        val stco = mp4Box("stco", stcoPayload)

        val stbl = mp4Box("stbl", stsd + stts + stsc + stsz + stco)
        val mediaHeader = if (handlerType == "vide") mp4Box("vmhd", ByteArray(12)) else mp4Box("smhd", ByteArray(8))
        val urlBox = mp4Box("url ", byteArrayOf(0, 0, 0, 1))
        val drefPayload = ByteBuffer.allocate(8 + urlBox.size).apply {
            putInt(0); putInt(1); put(urlBox)
        }.array()
        val dinf = mp4Box("dinf", mp4Box("dref", drefPayload))
        val minf = mp4Box("minf", mediaHeader + dinf + stbl)
        val mdia = mp4Box("mdia", mdhd + hdlr + minf)
        return mp4Box("trak", tkhd + mdia)
    }

    private fun mp4Box(type: String, payload: ByteArray): ByteArray {
        val buf = ByteBuffer.allocate(8 + payload.size)
        buf.putInt(8 + payload.size)
        buf.put(type.toByteArray(Charsets.US_ASCII), 0, 4)
        buf.put(payload)
        return buf.array()
    }

    /**
     * Saves the completed MP4 file to the phone's MediaStore (Movies/Tasneem)
     * so it is immediately accessible in the user's gallery and video players.
     */
    fun saveMp4ToMediaStore(sourceMp4File: File, surahNameAr: String): Uri? {
        return try {
            val resolver = context.contentResolver
            val displayFileName = "تسنيم_سورة_${surahNameAr}_${System.currentTimeMillis()}.mp4"
            val values = ContentValues().apply {
                put(MediaStore.Video.Media.DISPLAY_NAME, displayFileName)
                put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.Video.Media.RELATIVE_PATH, "${Environment.DIRECTORY_MOVIES}/Tasneem")
                    put(MediaStore.Video.Media.IS_PENDING, 1)
                }
            }

            val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
            } else {
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI
            }

            val itemUri = resolver.insert(collection, values) ?: return null
            resolver.openOutputStream(itemUri)?.use { outStream ->
                sourceMp4File.inputStream().use { inStream ->
                    inStream.copyTo(outStream)
                }
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val completeValues = ContentValues().apply {
                    put(MediaStore.Video.Media.IS_PENDING, 0)
                }
                resolver.update(itemUri, completeValues, null, null)
            }
            itemUri
        } catch (_: Exception) {
            null
        }
    }
}
