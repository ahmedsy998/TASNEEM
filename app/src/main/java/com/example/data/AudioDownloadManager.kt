package com.example.data

import android.content.Context
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMetadataRetriever
import android.util.Log
import com.example.model.AudioDownloadState
import com.example.model.QuranVerse
import com.example.model.ReciterInfo
import com.example.model.VerseAudioSegment
import com.example.model.WordTiming
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

data class AcousticSpeechEnvelope(
    val speechStartMs: Long,
    val speechEndMs: Long,
    val pauseMidpointsMs: List<Long>,
    val containsLeadingBasmalaAudio: Boolean
)

class AudioDownloadManager(
    private val context: Context,
    private val reciterRepository: ReciterRepository
) {

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    private val audioCacheRoot: File by lazy {
        File(context.filesDir, "tasneem_verse_audio").apply { mkdirs() }
    }

    /**
     * Downloads ONLY the individual verse audio files in the range [startVerse..endVerse]
     * for the specified [surahNumber] and [reciter], analyzes each file's acoustic speech
     * boundaries, and produces calibrated word-level timing metadata.
     * Never downloads unselected verses or full-surah recordings.
     */
    fun downloadSelectedVerseAudio(
        reciter: ReciterInfo,
        surahNumber: Int,
        startVerse: Int,
        endVerse: Int,
        verses: List<QuranVerse> = emptyList()
    ): Flow<AudioDownloadState> = flow {
        val totalSelected = (endVerse - startVerse + 1).coerceAtLeast(1)

        if (!reciterRepository.hasVerifiedPerVerseProvider(reciter)) {
            emit(
                AudioDownloadState.Error(
                    messageAr = "القارئ «${reciter.nameAr}» لا يتوفر له حاليًا مصدر تلاوة مقسم حسب الآيات المفردة في المزود الافتراضي. تطبيق تسنيم لا يقوم بتحميل السورة كاملة لتجنب تحميل آيات غير مختارة. يرجى اختيار قارئ آخر أو ربط مزود آيات مفردة معتمد."
                )
            )
            return@flow
        }

        val reciterDir = File(audioCacheRoot, reciter.id).apply { mkdirs() }
        val downloadedFilesNames = mutableListOf<String>()
        val segments = mutableListOf<VerseAudioSegment>()
        var cumulativeOffsetMs = 0L
        var anyWordLevelSync = false

        emit(
            AudioDownloadState.Downloading(
                selectedVerseCount = totalSelected,
                downloadedCount = 0,
                currentVerseNumber = startVerse,
                progressFraction = 0f,
                downloadedFiles = emptyList()
            )
        )

        for ((idx, verseNum) in (startVerse..endVerse).withIndex()) {
            val sss = String.format(Locale.US, "%03d", surahNumber)
            val aaa = String.format(Locale.US, "%03d", verseNum)
            val fileName = "${reciter.id}_${sss}_${aaa}.mp3"
            val targetFile = File(reciterDir, fileName)

            val primaryUrl = reciterRepository.resolveVerseAudioUrl(reciter, surahNumber, verseNum)
            var resolvedUrl = primaryUrl ?: ""

            // Check if valid file is already cached on device
            var isFileValid = validateAudioFile(targetFile)
            if (!isFileValid) {
                var downloaded = false
                if (!primaryUrl.isNullOrBlank()) {
                    downloaded = downloadFileFromUrl(primaryUrl, targetFile)
                }

                // Fallback to AlQuran.cloud verified single-ayah audio endpoint if primary failed
                if (!downloaded && reciter.alQuranCloudEdition != null) {
                    val fallbackAudioUrl = resolveAlQuranCloudAyahAudioUrl(
                        surahNumber = surahNumber,
                        verseNumber = verseNum,
                        edition = reciter.alQuranCloudEdition
                    )
                    if (!fallbackAudioUrl.isNullOrBlank()) {
                        resolvedUrl = fallbackAudioUrl
                        downloaded = downloadFileFromUrl(fallbackAudioUrl, targetFile)
                    }
                }

                isFileValid = downloaded && validateAudioFile(targetFile)
            }

            if (!isFileValid) {
                if (targetFile.exists()) targetFile.delete()
                emit(
                    AudioDownloadState.Error(
                        messageAr = "تعذر تحميل أو التحقق من الملف الصوتي للآية رقم ($verseNum) للقارئ «${reciter.nameAr}». يرجى التحقق من اتصال الإنترنت.",
                        failedVerseNumber = verseNum
                    )
                )
                return@flow
            }

            val durationMs = extractAudioDurationMs(targetFile).coerceAtLeast(1500L)
            val verseObj = verses.find { it.verseNumber == verseNum }
            val sanitizedUthmaniText = QuranDataRepository.sanitizeVerseTextForMadinahMushaf(
                surahNumber = surahNumber,
                verseNumber = verseNum,
                rawUthmaniText = verseObj?.uthmaniText ?: ""
            )
            val verseWords = sanitizedUthmaniText
                .trim()
                .split(Regex("\\s+"))
                .filter { it.isNotBlank() }

            val rawApiTimings = if (reciter.quranComRecitationId != null) {
                fetchVerifiedWordTimings(
                    recitationId = reciter.quranComRecitationId,
                    surahNumber = surahNumber,
                    verseNumber = verseNum
                )
            } else {
                emptyList()
            }

            val envelope = analyzeMp3SpeechEnvelope(
                mp3File = targetFile,
                surahNumber = surahNumber,
                verseNumber = verseNum,
                verseWords = verseWords,
                totalDurationMs = durationMs
            )

            val calibratedWordTimings = buildCalibratedWordTimings(
                surahNumber = surahNumber,
                verseNumber = verseNum,
                verseWords = verseWords,
                totalDurationMs = durationMs,
                envelope = envelope,
                rawApiTimings = rawApiTimings
            )

            if (calibratedWordTimings.isNotEmpty()) {
                anyWordLevelSync = true
            }

            val startOffset = cumulativeOffsetMs
            val endOffset = startOffset + durationMs
            cumulativeOffsetMs = endOffset

            Log.d(
                "TasneemSync",
                "Prepared segment Surah=$surahNumber Verse=$verseNum " +
                    "duration=${durationMs}ms speech=[${envelope.speechStartMs}..${envelope.speechEndMs}]ms " +
                    "offset=[$startOffset..$endOffset]ms words=${calibratedWordTimings.size} " +
                    "leadingBasmalaAudio=${envelope.containsLeadingBasmalaAudio}"
            )

            segments.add(
                VerseAudioSegment(
                    surahNumber = surahNumber,
                    verseNumber = verseNum,
                    localFilePath = targetFile.absolutePath,
                    sourceUrl = resolvedUrl,
                    durationMs = durationMs,
                    startOffsetMs = startOffset,
                    endOffsetMs = endOffset,
                    wordTimings = calibratedWordTimings,
                    speechStartMs = envelope.speechStartMs,
                    speechEndMs = envelope.speechEndMs,
                    containsLeadingBasmalaAudio = envelope.containsLeadingBasmalaAudio,
                    acousticPauseWindowsMs = envelope.pauseMidpointsMs,
                    isVerified = true
                )
            )

            downloadedFilesNames.add(fileName)
            val completedCount = idx + 1
            emit(
                AudioDownloadState.Downloading(
                    selectedVerseCount = totalSelected,
                    downloadedCount = completedCount,
                    currentVerseNumber = verseNum,
                    progressFraction = completedCount.toFloat() / totalSelected.toFloat(),
                    downloadedFiles = downloadedFilesNames.toList()
                )
            )
        }

        emit(
            AudioDownloadState.Ready(
                surahNumber = surahNumber,
                startVerse = startVerse,
                endVerse = endVerse,
                reciterId = reciter.id,
                segments = segments,
                totalDurationMs = cumulativeOffsetMs,
                hasWordLevelSync = anyWordLevelSync
            )
        )
    }.flowOn(Dispatchers.IO)

    /**
     * Decodes the MP3 file into 20ms RMS acoustic energy windows to detect:
     * 1. Exact speech onset [speechStartMs] (ignoring leading MP3 silence)
     * 2. Exact speech end [speechEndMs] (ignoring trailing MP3 silence/reverb tail)
     * 3. Natural intra-verse breath pauses [pauseMidpointsMs]
     * 4. Whether a Verse 1 audio file for Surahs 2..8, 10..114 actually contains a recited Basmala
     */
    fun analyzeMp3SpeechEnvelope(
        mp3File: File,
        surahNumber: Int,
        verseNumber: Int,
        verseWords: List<String>,
        totalDurationMs: Long
    ): AcousticSpeechEnvelope {
        val defaultStart = min(180L, totalDurationMs / 14)
        val defaultEnd = max(defaultStart + 400L, totalDurationMs - min(220L, totalDurationMs / 11))

        if (!mp3File.exists() || mp3File.length() < 512L) {
            return AcousticSpeechEnvelope(
                speechStartMs = defaultStart,
                speechEndMs = defaultEnd,
                pauseMidpointsMs = emptyList(),
                containsLeadingBasmalaAudio = false
            )
        }

        val windowMs = 20L
        val rmsWindows = mutableListOf<Float>()
        val extractor = MediaExtractor()
        var decoder: MediaCodec? = null

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
            if (audioTrackIdx >= 0 && inputFormat != null) {
                extractor.selectTrack(audioTrackIdx)
                val mime = inputFormat.getString(MediaFormat.KEY_MIME) ?: MediaFormat.MIMETYPE_AUDIO_MPEG
                val sampleRate = if (inputFormat.containsKey(MediaFormat.KEY_SAMPLE_RATE)) {
                    inputFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE)
                } else 44100
                val channels = if (inputFormat.containsKey(MediaFormat.KEY_CHANNEL_COUNT)) {
                    inputFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
                } else 1
                val samplesPerWindow = ((sampleRate * windowMs) / 1000L).toInt().coerceAtLeast(1) * channels.coerceAtLeast(1)

                decoder = MediaCodec.createDecoderByType(mime)
                decoder.configure(inputFormat, null, null, 0)
                decoder.start()

                val bufferInfo = MediaCodec.BufferInfo()
                var inputDone = false
                var outputDone = false
                var currentSumSq = 0.0
                var currentSampleCount = 0

                while (!outputDone) {
                    if (!inputDone) {
                        val inIdx = decoder.dequeueInputBuffer(5_000L)
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
                                    decoder.queueInputBuffer(inIdx, 0, sampleSize, extractor.sampleTime, 0)
                                    extractor.advance()
                                }
                            }
                        }
                    }

                    val outIdx = decoder.dequeueOutputBuffer(bufferInfo, 5_000L)
                    if (outIdx >= 0) {
                        val outBuf = decoder.getOutputBuffer(outIdx)
                        if (outBuf != null && bufferInfo.size >= 2) {
                            outBuf.position(bufferInfo.offset)
                            outBuf.limit(bufferInfo.offset + bufferInfo.size)
                            val shortBuf = outBuf.slice().order(ByteOrder.LITTLE_ENDIAN).asShortBuffer()
                            while (shortBuf.hasRemaining()) {
                                val sample = shortBuf.get().toDouble() / 32768.0
                                currentSumSq += sample * sample
                                currentSampleCount++
                                if (currentSampleCount >= samplesPerWindow) {
                                    rmsWindows.add(sqrt(currentSumSq / currentSampleCount).toFloat())
                                    currentSumSq = 0.0
                                    currentSampleCount = 0
                                }
                            }
                        }
                        decoder.releaseOutputBuffer(outIdx, false)
                        if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                            outputDone = true
                        }
                    } else if (outIdx == MediaCodec.INFO_TRY_AGAIN_LATER && inputDone) {
                        break
                    }
                }
                if (currentSampleCount > 0) {
                    rmsWindows.add(sqrt(currentSumSq / currentSampleCount).toFloat())
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

        if (rmsWindows.size < 8) {
            return AcousticSpeechEnvelope(
                speechStartMs = defaultStart,
                speechEndMs = defaultEnd,
                pauseMidpointsMs = emptyList(),
                containsLeadingBasmalaAudio = false
            )
        }

        val peakRms = (rmsWindows.maxOrNull() ?: 0.1f).coerceAtLeast(0.01f)
        val speechThreshold = max(0.008f, peakRms * 0.075f)
        val firstActiveWindow = rmsWindows.indexOfFirst { it >= speechThreshold }.takeIf { it >= 0 } ?: 0
        val lastActiveWindow = rmsWindows.indexOfLast { it >= speechThreshold }.takeIf { it >= firstActiveWindow } ?: rmsWindows.lastIndex

        val detectedStartMs = (firstActiveWindow * windowMs).coerceIn(0L, totalDurationMs / 3)
        val detectedEndMs = ((lastActiveWindow + 1) * windowMs).coerceIn(
            detectedStartMs + 350L,
            totalDurationMs
        )

        // Detect intra-verse breath pauses (>= 160ms of low energy inside active speech)
        val pauseThreshold = max(0.006f, peakRms * 0.055f)
        val pauseMidpoints = mutableListOf<Long>()
        var silenceRunStart = -1
        for (wIdx in (firstActiveWindow + 6)..(lastActiveWindow - 6)) {
            if (rmsWindows[wIdx] < pauseThreshold) {
                if (silenceRunStart < 0) silenceRunStart = wIdx
            } else {
                if (silenceRunStart >= 0) {
                    val runLen = wIdx - silenceRunStart
                    if (runLen >= 8) { // >= 160ms natural waqf/breath pause
                        val midWindow = (silenceRunStart + wIdx) / 2
                        pauseMidpoints.add(midWindow * windowMs)
                    }
                    silenceRunStart = -1
                }
            }
        }

        // Determine if a Verse 1 audio file of Surahs 2..8, 10..114 actually includes a recited Basmala
        // EveryAyah split-verse MP3s start directly with Verse 1 (Basmala is 001001.mp3).
        // Only mark containsLeadingBasmalaAudio = true if the speech duration exceeds the verse's
        // expected phonetic duration by >= 3,400ms AND has a pause between 2,400ms and 4,600ms.
        val speechSpanMs = detectedEndMs - detectedStartMs
        val expectedVerseOnlyMaxMs = max(2600L, verseWords.size * 1450L)
        val hasBasmalaBoundaryPause = pauseMidpoints.any { it - detectedStartMs in 2400L..4600L }
        val containsLeadingBasmala = verseNumber == 1 &&
            surahNumber != 1 &&
            surahNumber != 9 &&
            verseWords.isNotEmpty() &&
            speechSpanMs >= (expectedVerseOnlyMaxMs + 3400L) &&
            hasBasmalaBoundaryPause

        return AcousticSpeechEnvelope(
            speechStartMs = detectedStartMs,
            speechEndMs = detectedEndMs,
            pauseMidpointsMs = pauseMidpoints,
            containsLeadingBasmalaAudio = containsLeadingBasmala
        )
    }

    companion object {
        /**
         * Computes the relative recitation duration weight of a Quranic Uthmani word based on:
         * - Consonantal & vowel letters
         * - Maddah sign (\u0653) — 4 to 5 harakat elongation (Madd Muttasil / Munfasil / Lazim)
         * - Superscript Alef (\u0670) & long vowels (ا, و, ي, ى) — 2 harakat natural elongation
         * - Shadda (\u0651) & Ghunnah (نّ, مّ) — 2 harakat nasalization/gemination
         * - Waqf pause sign (\u06D6..\u06DC) — natural breath/pause cadence at end of clause
         */
        fun computeQuranWordPhoneticWeight(word: String): Float {
            var baseLetters = 0
            var longVowels = 0
            var maddSigns = 0
            var shaddaSigns = 0
            var ghunnahBonus = 0f
            var waqfBonus = 0f

            for (i in word.indices) {
                val ch = word[i]
                val code = ch.code
                when {
                    code == 0x0653 -> maddSigns++ // Maddah ~ 4 harakat
                    code == 0x0670 -> longVowels++ // Dagger Alef ~ 2 harakat
                    code == 0x0651 -> {
                        shaddaSigns++
                        if (i > 0 && (word[i - 1] == 'ن' || word[i - 1] == 'م')) {
                            ghunnahBonus += 1.4f // Ghunnah on Mushaddad Noon/Meem
                        }
                    }
                    code in 0x06D6..0x06DC -> waqfBonus += 1.2f
                    ch == 'ا' || ch == 'و' || ch == 'ي' || ch == 'ى' -> {
                        baseLetters++
                        longVowels++
                    }
                    !((code in 0x0610..0x061A) || (code in 0x064B..0x065F) || (code in 0x06D6..0x06ED) || (code in 0x08D3..0x08FF)) -> {
                        baseLetters++
                    }
                }
            }

            return (baseLetters.coerceAtLeast(1) * 1.0f) +
                (longVowels * 0.75f) +
                (maddSigns * 2.4f) +
                (shaddaSigns * 0.55f) +
                ghunnahBonus +
                waqfBonus
        }

        /**
         * Builds calibrated, strictly monotonic [WordTiming] entries for every word in [verseWords]
         * locked to the actual audio speech window `[envelope.speechStartMs, envelope.speechEndMs]`.
         */
        fun buildCalibratedWordTimings(
            surahNumber: Int,
            verseNumber: Int,
            verseWords: List<String>,
            totalDurationMs: Long,
            envelope: AcousticSpeechEnvelope,
            rawApiTimings: List<WordTiming>
        ): List<WordTiming> {
            if (verseWords.isEmpty()) return emptyList()

            val speechStart = envelope.speechStartMs.coerceIn(0L, max(0L, totalDurationMs - 300L))
            val speechEnd = envelope.speechEndMs.coerceIn(speechStart + 250L, totalDurationMs)
            val speechDuration = (speechEnd - speechStart).coerceAtLeast(250L)

            // Check if rawApiTimings can be mapped onto verseWords
            val sortedApi = rawApiTimings
                .filter { it.endTimeMs > it.startTimeMs && it.startTimeMs >= 0L }
                .sortedBy { it.wordIndex }

            // If API included 4 leading Basmala word timings on Verse 1 (Surahs 2..8, 10..114)
            // when the audio does NOT contain Basmala, drop the first 4 API segments and re-index
            val effectiveApi = if (
                verseNumber == 1 &&
                surahNumber != 1 &&
                surahNumber != 9 &&
                !envelope.containsLeadingBasmalaAudio &&
                sortedApi.size == verseWords.size + 4
            ) {
                sortedApi.drop(4).mapIndexed { idx, wt ->
                    wt.copy(wordIndex = idx + 1)
                }
            } else {
                sortedApi
            }

            // Use calibrated API timings if they match the word count closely
            if (effectiveApi.size == verseWords.size && effectiveApi.isNotEmpty()) {
                val apiMinStart = effectiveApi.first().startTimeMs
                val apiMaxEnd = effectiveApi.last().endTimeMs
                val apiSpan = (apiMaxEnd - apiMinStart).coerceAtLeast(1L)

                var prevEnd = speechStart
                return verseWords.mapIndexed { idx, wordText ->
                    val raw = effectiveApi[idx]
                    val normStart = (raw.startTimeMs - apiMinStart).toDouble() / apiSpan.toDouble()
                    val normEnd = (raw.endTimeMs - apiMinStart).toDouble() / apiSpan.toDouble()
                    val mappedStart = if (idx == 0) {
                        speechStart
                    } else {
                        (speechStart + (normStart * speechDuration).toLong()).coerceAtLeast(prevEnd)
                    }
                    val mappedEnd = if (idx == verseWords.lastIndex) {
                        speechEnd
                    } else {
                        (speechStart + (normEnd * speechDuration).toLong())
                            .coerceIn(mappedStart + 60L, speechEnd - 40L)
                    }
                    prevEnd = mappedEnd
                    WordTiming(
                        surahNumber = surahNumber,
                        verseNumber = verseNumber,
                        wordIndex = idx + 1,
                        wordText = wordText,
                        startTimeMs = mappedStart,
                        endTimeMs = mappedEnd
                    )
                }
            }

            // Phonetic + Acoustic Pause Forced Alignment across [speechStart..speechEnd]
            val weights = verseWords.map { computeQuranWordPhoneticWeight(it) }
            val totalWeight = weights.sum().coerceAtLeast(1f)
            var cursorMs = speechStart
            val result = mutableListOf<WordTiming>()

            verseWords.forEachIndexed { idx, wordText ->
                val startMs = if (idx == 0) speechStart else cursorMs
                val rawEndMs = if (idx == verseWords.lastIndex) {
                    speechEnd
                } else {
                    val proportionalDur = ((weights[idx] / totalWeight) * speechDuration).toLong().coerceAtLeast(80L)
                    val candidateEnd = (startMs + proportionalDur).coerceAtMost(speechEnd - 60L)
                    // If this word has a Waqf mark or a nearby detected acoustic breath pause within 220ms, snap to the pause!
                    val nearbyPause = envelope.pauseMidpointsMs.minByOrNull { kotlin.math.abs(it - candidateEnd) }
                    if (nearbyPause != null && kotlin.math.abs(nearbyPause - candidateEnd) <= 220L && nearbyPause > startMs + 80L) {
                        nearbyPause
                    } else {
                        candidateEnd
                    }
                }
                val endMs = if (idx == verseWords.lastIndex) speechEnd else rawEndMs.coerceAtLeast(startMs + 60L)
                cursorMs = endMs

                result.add(
                    WordTiming(
                        surahNumber = surahNumber,
                        verseNumber = verseNumber,
                        wordIndex = idx + 1,
                        wordText = wordText,
                        startTimeMs = startMs,
                        endTimeMs = endMs
                    )
                )
            }

            return result
        }
    }

    private fun downloadFileFromUrl(url: String, destination: File): Boolean {
        return try {
            val tempFile = File(destination.parentFile, "${destination.name}.tmp")
            val request = Request.Builder().url(url).get().build()
            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return false
                val body = response.body ?: return false
                body.byteStream().use { input ->
                    FileOutputStream(tempFile).use { output ->
                        val buffer = ByteArray(8192)
                        var read: Int
                        while (input.read(buffer).also { read = it } != -1) {
                            output.write(buffer, 0, read)
                        }
                        output.flush()
                    }
                }
            }
            if (tempFile.exists() && tempFile.length() > 512L) {
                if (destination.exists()) destination.delete()
                tempFile.renameTo(destination)
                true
            } else {
                tempFile.delete()
                false
            }
        } catch (e: Exception) {
            false
        }
    }

    private fun resolveAlQuranCloudAyahAudioUrl(
        surahNumber: Int,
        verseNumber: Int,
        edition: String
    ): String? {
        return try {
            val apiUrl = "https://api.alquran.cloud/v1/ayah/$surahNumber:$verseNumber/$edition"
            val req = Request.Builder().url(apiUrl).get().build()
            httpClient.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return null
                val body = resp.body?.string() ?: return null
                val json = JSONObject(body)
                val data = json.optJSONObject("data") ?: return null
                val audioUrl = data.optString("audio", "")
                audioUrl.takeIf { it.startsWith("https://") }
            }
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Fetches verified word-level timestamp segments from Quran.com v4 API when supported by the reciter.
     */
    private fun fetchVerifiedWordTimings(
        recitationId: Int,
        surahNumber: Int,
        verseNumber: Int
    ): List<WordTiming> {
        return try {
            val url = "https://api.quran.com/api/v4/recitations/$recitationId/by_ayah/$surahNumber:$verseNumber"
            val req = Request.Builder().url(url).get().build()
            httpClient.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return emptyList()
                val body = resp.body?.string() ?: return emptyList()
                val json = JSONObject(body)
                val audioFiles = json.optJSONArray("audio_files") ?: return emptyList()
                if (audioFiles.length() == 0) return emptyList()
                val first = audioFiles.getJSONObject(0)
                val segmentsArr = first.optJSONArray("segments") ?: return emptyList()
                val timings = mutableListOf<WordTiming>()
                for (i in 0 until segmentsArr.length()) {
                    val seg = segmentsArr.optJSONArray(i) ?: continue
                    if (seg.length() >= 3) {
                        val wordIdx = seg.optInt(0, -1)
                        val startMs = seg.optLong(1, -1L)
                        val endMs = seg.optLong(2, -1L)
                        if (wordIdx >= 1 && startMs >= 0L && endMs > startMs) {
                            timings.add(
                                WordTiming(
                                    surahNumber = surahNumber,
                                    verseNumber = verseNumber,
                                    wordIndex = wordIdx,
                                    wordText = "",
                                    startTimeMs = startMs,
                                    endTimeMs = endMs
                                )
                            )
                        }
                    }
                }
                timings
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun validateAudioFile(file: File): Boolean {
        if (!file.exists() || file.length() < 512L) return false
        val duration = extractAudioDurationMs(file)
        return duration > 200L
    }

    fun extractAudioDurationMs(file: File): Long {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(file.absolutePath)
            val durStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            durStr?.toLongOrNull() ?: 0L
        } catch (_: Exception) {
            0L
        } finally {
            try {
                retriever.release()
            } catch (_: Exception) {
            }
        }
    }
}
