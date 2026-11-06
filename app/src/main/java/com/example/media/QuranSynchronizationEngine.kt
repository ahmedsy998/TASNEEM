package com.example.media

import android.content.Context
import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.example.model.VerseAudioSegment
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File

data class PlaybackSyncState(
    val isPlaying: Boolean = false,
    val isReady: Boolean = false,
    val activeSegmentIndex: Int = 0,
    val activeVerseNumber: Int = 1,
    val activeWordIndex: Int? = null, // 1-indexed when verified word-level timings exist
    val currentVersePositionMs: Long = 0L,
    val overallPositionMs: Long = 0L,
    val totalDurationMs: Long = 0L,
    val isWordSyncActive: Boolean = false
)

class QuranSynchronizationEngine(
    context: Context,
    private val coroutineScope: CoroutineScope
) {
    private val exoPlayer: ExoPlayer = ExoPlayer.Builder(context.applicationContext).build()
    private val previewPlayer: ExoPlayer = ExoPlayer.Builder(context.applicationContext).build()

    private var loadedSegments: List<VerseAudioSegment> = emptyList()
    private var tickerJob: Job? = null

    private val _syncState = MutableStateFlow(PlaybackSyncState())
    val syncState: StateFlow<PlaybackSyncState> = _syncState.asStateFlow()

    private val _previewReciterId = MutableStateFlow<String?>(null)
    val previewReciterId: StateFlow<String?> = _previewReciterId.asStateFlow()

    init {
        exoPlayer.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                updateSyncState()
                if (isPlaying) {
                    startSyncTicker()
                } else {
                    stopSyncTicker()
                }
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                updateSyncState()
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_ENDED) {
                    exoPlayer.pause()
                    exoPlayer.seekTo(0, 0L)
                }
                updateSyncState()
            }
        })

        previewPlayer.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_ENDED || playbackState == Player.STATE_IDLE) {
                    _previewReciterId.value = null
                }
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                if (!isPlaying && previewPlayer.playbackState == Player.STATE_ENDED) {
                    _previewReciterId.value = null
                }
            }
        })
    }

    /**
     * Loads the verified downloaded verse audio segments in exact Quranic order.
     */
    fun loadSegments(segments: List<VerseAudioSegment>) {
        stopPreview()
        exoPlayer.stop()
        exoPlayer.clearMediaItems()
        loadedSegments = segments

        if (segments.isEmpty()) {
            _syncState.value = PlaybackSyncState()
            return
        }

        val mediaItems = segments.map { seg ->
            MediaItem.fromUri(Uri.fromFile(File(seg.localFilePath)))
        }
        exoPlayer.setMediaItems(mediaItems, /* resetPosition = */ true)
        exoPlayer.prepare()

        val totalDur = segments.lastOrNull()?.endOffsetMs ?: 0L
        val firstSeg = segments.first()
        _syncState.value = PlaybackSyncState(
            isPlaying = false,
            isReady = true,
            activeSegmentIndex = 0,
            activeVerseNumber = firstSeg.verseNumber,
            activeWordIndex = null,
            currentVersePositionMs = 0L,
            overallPositionMs = 0L,
            totalDurationMs = totalDur,
            isWordSyncActive = segments.any { it.wordTimings.isNotEmpty() }
        )
    }

    fun play() {
        if (loadedSegments.isEmpty()) return
        stopPreview()
        if (exoPlayer.playbackState == Player.STATE_ENDED) {
            exoPlayer.seekTo(0, 0L)
        }
        exoPlayer.play()
    }

    fun pause() {
        exoPlayer.pause()
    }

    fun togglePlayPause() {
        if (exoPlayer.isPlaying) {
            pause()
        } else {
            play()
        }
    }

    fun resetPlayback() {
        stopSyncTicker()
        exoPlayer.pause()
        if (loadedSegments.isNotEmpty()) {
            exoPlayer.seekTo(0, 0L)
        }
        updateSyncState()
    }

    fun seekToOverallPosition(targetOverallMs: Long) {
        if (loadedSegments.isEmpty()) return
        val total = loadedSegments.last().endOffsetMs
        val clamped = targetOverallMs.coerceIn(0L, total)

        val targetIndex = loadedSegments.indexOfFirst { seg ->
            clamped >= seg.startOffsetMs && clamped <= seg.endOffsetMs
        }.takeIf { it >= 0 } ?: (loadedSegments.size - 1)

        val segment = loadedSegments[targetIndex]
        val localOffset = (clamped - segment.startOffsetMs).coerceIn(0L, segment.durationMs)
        exoPlayer.seekTo(targetIndex, localOffset)
        updateSyncState()
    }

    fun seekToSegmentIndex(index: Int) {
        if (index !in loadedSegments.indices) return
        exoPlayer.seekTo(index, 0L)
        updateSyncState()
    }

    fun playReciterPreview(reciterId: String, previewAudioUrl: String) {
        if (_previewReciterId.value == reciterId && previewPlayer.isPlaying) {
            stopPreview()
            return
        }
        pause()
        _previewReciterId.value = reciterId
        previewPlayer.stop()
        previewPlayer.clearMediaItems()
        previewPlayer.setMediaItem(MediaItem.fromUri(previewAudioUrl))
        previewPlayer.prepare()
        previewPlayer.play()
    }

    fun stopPreview() {
        previewPlayer.stop()
        previewPlayer.clearMediaItems()
        _previewReciterId.value = null
    }

    private var lastLoggedVerse: Int = -1
    private var lastLoggedWord: Int = -1

    private fun startSyncTicker() {
        tickerJob?.cancel()
        tickerJob = coroutineScope.launch(Dispatchers.Main) {
            while (isActive && exoPlayer.isPlaying) {
                updateSyncState()
                delay(16L) // ~60fps high-precision audio clock synchronization
            }
        }
    }

    private fun stopSyncTicker() {
        tickerJob?.cancel()
        tickerJob = null
    }

    private fun updateSyncState() {
        if (loadedSegments.isEmpty()) return
        val itemIdx = exoPlayer.currentMediaItemIndex.coerceIn(0, loadedSegments.size - 1)
        val seg = loadedSegments[itemIdx]
        val liveDuration = exoPlayer.duration.takeIf { it > 200L } ?: seg.durationMs
        val localPos = exoPlayer.currentPosition.coerceIn(0L, liveDuration)
        val overallPos = (seg.startOffsetMs + localPos.coerceAtMost(seg.durationMs)).coerceAtMost(
            loadedSegments.lastOrNull()?.endOffsetMs ?: 0L
        )

        val matchedWordTiming = if (seg.wordTimings.isNotEmpty()) {
            seg.wordTimings.find { localPos >= it.startTimeMs && localPos <= it.endTimeMs }
                ?: seg.wordTimings.lastOrNull { localPos >= it.startTimeMs }
        } else {
            null
        }
        val activeWord = matchedWordTiming?.wordIndex

        if (seg.verseNumber != lastLoggedVerse || (activeWord != null && activeWord != lastLoggedWord)) {
            lastLoggedVerse = seg.verseNumber
            if (activeWord != null) lastLoggedWord = activeWord
            android.util.Log.d(
                "TasneemSync",
                "AudioClock pos=${overallPos}ms (local=${localPos}ms/${seg.durationMs}ms) " +
                    "Surah=${seg.surahNumber} Verse=${seg.verseNumber} " +
                    "Word=${activeWord ?: -1} ('${matchedWordTiming?.wordText ?: ""}') " +
                    "expected=[${matchedWordTiming?.startTimeMs ?: 0}..${matchedWordTiming?.endTimeMs ?: seg.durationMs}]ms " +
                    "segBounds=[${seg.startOffsetMs}..${seg.endOffsetMs}]ms"
            )
        }

        _syncState.value = PlaybackSyncState(
            isPlaying = exoPlayer.isPlaying,
            isReady = true,
            activeSegmentIndex = itemIdx,
            activeVerseNumber = seg.verseNumber,
            activeWordIndex = activeWord,
            currentVersePositionMs = localPos,
            overallPositionMs = overallPos,
            totalDurationMs = loadedSegments.last().endOffsetMs,
            isWordSyncActive = seg.wordTimings.isNotEmpty()
        )
    }

    fun release() {
        stopSyncTicker()
        exoPlayer.release()
        previewPlayer.release()
    }
}
