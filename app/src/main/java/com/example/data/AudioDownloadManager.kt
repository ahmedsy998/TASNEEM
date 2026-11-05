package com.example.data

import android.content.Context
import android.media.MediaMetadataRetriever
import com.example.model.AudioDownloadState
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
import java.util.Locale
import java.util.concurrent.TimeUnit

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
     * for the specified [surahNumber] and [reciter].
     * Never downloads unselected verses or full-surah recordings.
     */
    fun downloadSelectedVerseAudio(
        reciter: ReciterInfo,
        surahNumber: Int,
        startVerse: Int,
        endVerse: Int
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
            val wordTimings = if (reciter.quranComRecitationId != null) {
                fetchVerifiedWordTimings(
                    recitationId = reciter.quranComRecitationId,
                    surahNumber = surahNumber,
                    verseNumber = verseNum
                )
            } else {
                emptyList()
            }

            if (wordTimings.isNotEmpty()) {
                anyWordLevelSync = true
            }

            val startOffset = cumulativeOffsetMs
            val endOffset = startOffset + durationMs
            cumulativeOffsetMs = endOffset

            segments.add(
                VerseAudioSegment(
                    surahNumber = surahNumber,
                    verseNumber = verseNum,
                    localFilePath = targetFile.absolutePath,
                    sourceUrl = resolvedUrl,
                    durationMs = durationMs,
                    startOffsetMs = startOffset,
                    endOffsetMs = endOffset,
                    wordTimings = wordTimings,
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
     * Never fabricates or guesses word timings.
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
                            timings.add(WordTiming(wordIndex = wordIdx, startMs = startMs, endMs = endMs))
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
