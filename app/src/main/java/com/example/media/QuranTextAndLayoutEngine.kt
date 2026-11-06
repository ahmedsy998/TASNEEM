package com.example.media

import android.text.SpannableString
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import com.example.model.HeaderElementConfig
import com.example.model.QuranTextStyleConfig
import com.example.model.QuranVerse
import com.example.model.RecitationHeaderConfig
import com.example.model.TransitionEffectType
import com.example.model.VerseAudioSegment
import com.example.model.VerseTransitionConfig
import com.example.model.VideoAspectRatio
import com.example.model.WatermarkConfig
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

data class PhraseSlice(
    val phraseIndex: Int,
    val totalPhrases: Int,
    val text: String,
    val words: List<String>,
    val startWordIndex1Based: Int,
    val endWordIndex1Based: Int,
    val startMs: Long,
    val endMs: Long
)

data class ActivePhraseRenderState(
    val currentSlice: PhraseSlice,
    val previousSlice: PhraseSlice?,
    val localPhraseTimeMs: Long,
    val phraseDurationMs: Long,
    val activeWordInPhrase: Int, // 0-indexed within currentSlice.words, or -1
    val transitionState: TransitionFrameState
)

data class TransitionFrameState(
    val mainAlpha: Float = 1f,
    val offsetXNorm: Float = 0f,
    val offsetYNorm: Float = 0f,
    val scale: Float = 1f,
    val blurRadiusPx: Float = 0f,
    val visibleWordCount: Int = Int.MAX_VALUE,
    val previousAlpha: Float = 0f,
    val previousOffsetXNorm: Float = 0f,
    val previousScale: Float = 1f,
    val sparkleDissolvePhase: Float = 0f
)

data class AutoLayoutResult(
    val textStyle: QuranTextStyleConfig,
    val headerConfig: RecitationHeaderConfig,
    val watermarkConfig: WatermarkConfig
)

object QuranTextAndLayoutEngine {

    /**
     * Checks whether a character is an Arabic/Quranic combining diacritic (Tashkeel) or waqf mark.
     * Covers:
     * - \u0610..\u061A : Arabic honorifics & small high marks
     * - \u064B..\u065F : Standard Tashkeel (Fathatan, Dammatan, Kasratan, Fatha, Damma, Kasra, Shadda, Sukun, Maddah, Hamza)
     * - \u0670         : Superscript Alef (Dagger Alef)
     * - \u06D6..\u06ED : Quranic annotation signs, Sajdah, Rub el Hizb, small letters, pause marks
     * - \u08D3..\u08FF : Extended Arabic-A Quranic marks
     */
    fun isQuranicDiacritic(ch: Char): Boolean {
        val code = ch.code
        return (code in 0x0610..0x061A) ||
            (code in 0x064B..0x065F) ||
            (code == 0x0670) ||
            (code in 0x06D6..0x06ED) ||
            (code in 0x08D3..0x08FF)
    }

    /**
     * Checks if a token ends with or is a Quranic pause mark (Waqf sign).
     */
    private fun hasWaqfMark(word: String): Boolean {
        for (ch in word) {
            if (ch.code in 0x06D6..0x06DC) return true
        }
        return false
    }

    /**
     * Builds an Android SpannableString that colors Quranic diacritics (Tashkeel) independently
     * from base Arabic letters using ForegroundColorSpan.
     * Android's HarfBuzz text shaper keeps cursive Arabic joining and ligatures intact across
     * ForegroundColorSpan boundaries when font metrics and typeface remain identical.
     */
    fun buildQuranSpannable(
        text: String,
        mainColorInt: Int,
        enableIndependentTashkeel: Boolean,
        tashkeelColorInt: Int,
        activeWordCharRange: IntRange? = null,
        activeWordHighlightColorInt: Int? = null
    ): SpannableString {
        val spannable = SpannableString(text)
        if (text.isEmpty()) return spannable

        // Base color span across entire text
        spannable.setSpan(
            ForegroundColorSpan(mainColorInt),
            0,
            text.length,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )

        // Optional active word base highlight
        if (activeWordCharRange != null && activeWordHighlightColorInt != null) {
            val start = activeWordCharRange.first.coerceIn(0, text.length)
            val end = (activeWordCharRange.last + 1).coerceIn(start, text.length)
            if (end > start) {
                spannable.setSpan(
                    ForegroundColorSpan(activeWordHighlightColorInt),
                    start,
                    end,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            }
        }

        // Independent Tashkeel color spans on each contiguous diacritic run
        if (enableIndependentTashkeel && tashkeelColorInt != mainColorInt) {
            var i = 0
            while (i < text.length) {
                if (isQuranicDiacritic(text[i])) {
                    val runStart = i
                    while (i < text.length && isQuranicDiacritic(text[i])) {
                        i++
                    }
                    spannable.setSpan(
                        ForegroundColorSpan(tashkeelColorInt),
                        runStart,
                        i,
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                } else {
                    i++
                }
            }
        }

        return spannable
    }

    /**
     * Builds a Jetpack Compose AnnotatedString for UI previews of independent Tashkeel coloring.
     */
    fun buildQuranAnnotatedString(
        text: String,
        mainColor: Color,
        enableIndependentTashkeel: Boolean,
        tashkeelColor: Color
    ): AnnotatedString {
        return buildAnnotatedString {
            append(text)
            if (text.isNotEmpty()) {
                addStyle(SpanStyle(color = mainColor), 0, text.length)
                if (enableIndependentTashkeel && tashkeelColor != mainColor) {
                    var i = 0
                    while (i < text.length) {
                        if (isQuranicDiacritic(text[i])) {
                            val start = i
                            while (i < text.length && isQuranicDiacritic(text[i])) {
                                i++
                            }
                            addStyle(SpanStyle(color = tashkeelColor), start, i)
                        } else {
                            i++
                        }
                    }
                }
            }
        }
    }

    /**
     * Splits a Quranic verse into sequential single-line readable phrases (Requirements 05 & 09)
     * so that:
     * 1. Long verses (e.g., Ayat al-Kursi 2:255 or Al-Baqarah 2:282) are NEVER truncated.
     * 2. Only the currently recited phrase is shown on ONE single line.
     * 3. Each phrase is synchronized with the reciter's word timings (or proportional character timing).
     */
    fun segmentVerseIntoSingleLinePhrases(
        verse: QuranVerse,
        segment: VerseAudioSegment?,
        maxWordsPerPhrase: Int = 4,
        maxBaseCharsPerPhrase: Int = 28
    ): List<PhraseSlice> {
        val rawTokens = verse.uthmaniText
            .trim()
            .split(Regex("\\s+"))
            .filter { it.isNotBlank() }

        if (rawTokens.isEmpty()) {
            val ayahEnd = "﴿${toArabicNumerals(verse.verseNumber)}﴾"
            return listOf(
                PhraseSlice(
                    phraseIndex = 0,
                    totalPhrases = 1,
                    text = ayahEnd,
                    words = listOf(ayahEnd),
                    startWordIndex1Based = 1,
                    endWordIndex1Based = 1,
                    startMs = 0L,
                    endMs = max(1000L, segment?.durationMs ?: 4000L)
                )
            )
        }

        // Group words into concise single-line phrases (typically 3..4 words or up to pause mark)
        data class RawPhraseGroup(
            val words: MutableList<String>,
            val startWord1Based: Int,
            var endWord1Based: Int
        )

        val groups = mutableListOf<RawPhraseGroup>()
        var currentWords = mutableListOf<String>()
        var currentBaseCharCount = 0
        var groupStartWordIdx = 1

        rawTokens.forEachIndexed { idx, token ->
            val word1Based = idx + 1
            val tokenBaseChars = token.count { !isQuranicDiacritic(it) }.coerceAtLeast(1)

            if (currentWords.isEmpty()) {
                groupStartWordIdx = word1Based
            }

            currentWords.add(token)
            currentBaseCharCount += tokenBaseChars

            val reachedWordLimit = currentWords.size >= maxWordsPerPhrase
            val reachedCharLimit = currentBaseCharCount >= maxBaseCharsPerPhrase && currentWords.size >= 2
            val hitPauseMark = hasWaqfMark(token) && currentWords.size >= 2
            val isLastWord = idx == rawTokens.lastIndex

            if (!isLastWord && (reachedWordLimit || reachedCharLimit || hitPauseMark)) {
                groups.add(
                    RawPhraseGroup(
                        words = currentWords.toMutableList(),
                        startWord1Based = groupStartWordIdx,
                        endWord1Based = word1Based
                    )
                )
                currentWords = mutableListOf()
                currentBaseCharCount = 0
            }
        }

        if (currentWords.isNotEmpty()) {
            // If trailing remainder is just 1 short word and we already have a previous group, merge it
            if (currentWords.size == 1 && groups.isNotEmpty() && groups.last().words.size < maxWordsPerPhrase + 1) {
                val lastGroup = groups.last()
                lastGroup.words.addAll(currentWords)
                lastGroup.endWord1Based = rawTokens.size
            } else {
                groups.add(
                    RawPhraseGroup(
                        words = currentWords.toMutableList(),
                        startWord1Based = groupStartWordIdx,
                        endWord1Based = rawTokens.size
                    )
                )
            }
        }

        // Append ornate verse number symbol to the final phrase
        val ayahSymbol = "﴿${toArabicNumerals(verse.verseNumber)}﴾"
        if (groups.isNotEmpty()) {
            groups.last().words.add(ayahSymbol)
        }

        val totalDurationMs = max(1200L, segment?.durationMs ?: (groups.size * 2800L))
        val wordTimings = segment?.wordTimings.orEmpty().sortedBy { it.wordIndex }

        val totalGroups = groups.size
        val slices = mutableListOf<PhraseSlice>()

        // Check if wordTimings cover the verse reliably
        val hasValidWordTimings = wordTimings.size >= 2 &&
            wordTimings.last().endMs > wordTimings.first().startMs

        if (hasValidWordTimings) {
            var previousEndMs = 0L
            groups.forEachIndexed { index, group ->
                val matchingTimings = wordTimings.filter {
                    it.wordIndex in group.startWord1Based..group.endWord1Based
                }
                val startMs = if (index == 0) {
                    0L
                } else {
                    matchingTimings.firstOrNull()?.startMs?.coerceAtLeast(previousEndMs) ?: previousEndMs
                }
                val rawEndMs = if (index == groups.lastIndex) {
                    totalDurationMs
                } else {
                    matchingTimings.lastOrNull()?.endMs ?: (startMs + (totalDurationMs - startMs) / (totalGroups - index))
                }
                val endMs = rawEndMs.coerceIn(startMs + 250L, totalDurationMs)
                previousEndMs = endMs

                slices.add(
                    PhraseSlice(
                        phraseIndex = index,
                        totalPhrases = totalGroups,
                        text = group.words.joinToString(" "),
                        words = group.words,
                        startWordIndex1Based = group.startWord1Based,
                        endWordIndex1Based = group.endWord1Based,
                        startMs = startMs,
                        endMs = if (index == groups.lastIndex) totalDurationMs else endMs
                    )
                )
            }
        } else {
            // Proportional timing by character weight (base letters + elongation marks)
            val weights = groups.map { group ->
                val textJoined = group.words.joinToString(" ")
                val baseWeight = textJoined.count { !isQuranicDiacritic(it) }.coerceAtLeast(3)
                val maddWeight = textJoined.count { it == '\u0653' || it == '\u0670' || it == 'ا' || it == 'و' || it == 'ي' }
                (baseWeight + maddWeight * 0.6f).coerceAtLeast(4f)
            }
            val totalWeight = weights.sum().coerceAtLeast(1f)
            var accumulatedMs = 0L

            groups.forEachIndexed { index, group ->
                val startMs = accumulatedMs
                val sliceDuration = if (index == groups.lastIndex) {
                    totalDurationMs - startMs
                } else {
                    ((weights[index] / totalWeight) * totalDurationMs).toLong().coerceAtLeast(400L)
                }
                val endMs = if (index == groups.lastIndex) {
                    totalDurationMs
                } else {
                    (startMs + sliceDuration).coerceAtMost(totalDurationMs - 200L)
                }
                accumulatedMs = endMs

                slices.add(
                    PhraseSlice(
                        phraseIndex = index,
                        totalPhrases = totalGroups,
                        text = group.words.joinToString(" "),
                        words = group.words,
                        startWordIndex1Based = group.startWord1Based,
                        endWordIndex1Based = group.endWord1Based,
                        startMs = startMs,
                        endMs = endMs
                    )
                )
            }
        }

        return slices
    }

    /**
     * Resolves the currently active phrase and its transition state at [localVerseTimeMs]
     * within the given [verse] and [segment].
     */
    fun resolveActivePhraseAtTime(
        verse: QuranVerse,
        segment: VerseAudioSegment?,
        localVerseTimeMs: Long,
        singleLineMode: Boolean,
        transitionConfig: VerseTransitionConfig
    ): ActivePhraseRenderState {
        val totalVerseDurationMs = max(1200L, segment?.durationMs ?: 5000L)
        val clampedVerseTimeMs = localVerseTimeMs.coerceIn(0L, totalVerseDurationMs)

        val slices = if (singleLineMode) {
            segmentVerseIntoSingleLinePhrases(verse, segment)
        } else {
            // Even when singleLineMode is disabled, split ultra-long verses (> 18 words) into readable pages so they never truncate
            segmentVerseIntoSingleLinePhrases(
                verse = verse,
                segment = segment,
                maxWordsPerPhrase = 14,
                maxBaseCharsPerPhrase = 95
            )
        }

        val currentIdx = slices.indexOfLast { clampedVerseTimeMs >= it.startMs }
            .coerceIn(0, slices.lastIndex)
        val currentSlice = slices[currentIdx]
        val previousSlice = if (currentIdx > 0) slices[currentIdx - 1] else null

        val phraseDurationMs = max(350L, currentSlice.endMs - currentSlice.startMs)
        val localPhraseTimeMs = (clampedVerseTimeMs - currentSlice.startMs).coerceIn(0L, phraseDurationMs)

        // Determine active word index inside currentSlice
        val wordCount = currentSlice.words.size.coerceAtLeast(1)
        val progressInPhrase = (localPhraseTimeMs.toFloat() / phraseDurationMs.toFloat()).coerceIn(0f, 0.999f)
        val activeWordInPhrase = (progressInPhrase * wordCount).toInt().coerceIn(0, wordCount - 1)

        val transitionState = computeTransitionFrameState(
            transitionConfig = transitionConfig,
            localPhraseTimeMs = localPhraseTimeMs,
            phraseDurationMs = phraseDurationMs,
            totalWordsInPhrase = wordCount,
            hasPreviousSlice = previousSlice != null
        )

        return ActivePhraseRenderState(
            currentSlice = currentSlice,
            previousSlice = previousSlice,
            localPhraseTimeMs = localPhraseTimeMs,
            phraseDurationMs = phraseDurationMs,
            activeWordInPhrase = activeWordInPhrase,
            transitionState = transitionState
        )
    }

    /**
     * Computes smooth frame-by-frame animation parameters for all 7 transition effects (Requirement 06).
     */
    fun computeTransitionFrameState(
        transitionConfig: VerseTransitionConfig,
        localPhraseTimeMs: Long,
        phraseDurationMs: Long,
        totalWordsInPhrase: Int,
        hasPreviousSlice: Boolean
    ): TransitionFrameState {
        val enterWindowMs = min(transitionConfig.durationMs.coerceIn(80L, 1200L), phraseDurationMs / 2)
        val exitWindowMs = min((transitionConfig.durationMs * 0.65f).toLong().coerceAtLeast(60L), phraseDurationMs / 3)

        val enterLinear = if (enterWindowMs > 0) {
            (localPhraseTimeMs.toFloat() / enterWindowMs.toFloat()).coerceIn(0f, 1f)
        } else 1f

        val timeRemainingMs = (phraseDurationMs - localPhraseTimeMs).coerceAtLeast(0L)
        val exitLinear = if (exitWindowMs > 0 && timeRemainingMs < exitWindowMs) {
            (timeRemainingMs.toFloat() / exitWindowMs.toFloat()).coerceIn(0f, 1f)
        } else 1f

        val enterEased = easeOutCubic(enterLinear)
        val exitEased = easeInOutSine(exitLinear)

        return when (transitionConfig.effectType) {
            TransitionEffectType.FADE_IN_OUT -> {
                TransitionFrameState(
                    mainAlpha = min(enterEased, exitEased),
                    scale = 1f
                )
            }

            TransitionEffectType.SMOOTH_SLIDE -> {
                // Slides in smoothly from right (+0.06f) to center (0f), and exits slightly left (-0.04f)
                val enterSlideX = (1f - enterEased) * 0.065f
                val exitSlideX = -(1f - exitEased) * 0.045f
                TransitionFrameState(
                    mainAlpha = min(enterEased, exitEased),
                    offsetXNorm = enterSlideX + exitSlideX,
                    previousAlpha = if (hasPreviousSlice && enterLinear < 1f) (1f - enterEased) * 0.6f else 0f,
                    previousOffsetXNorm = -enterEased * 0.065f
                )
            }

            TransitionEffectType.GENTLE_ZOOM -> {
                val enterScale = 0.88f + (0.12f * enterEased)
                val exitScale = 1.0f + (1f - exitEased) * 0.04f
                TransitionFrameState(
                    mainAlpha = min(enterEased, exitEased),
                    scale = if (enterLinear < 1f) enterScale else exitScale,
                    previousAlpha = if (hasPreviousSlice && enterLinear < 1f) (1f - enterEased) * 0.45f else 0f,
                    previousScale = 1f + enterEased * 0.06f
                )
            }

            TransitionEffectType.DISSOLVE -> {
                val combinedAlpha = min(enterEased, exitEased)
                TransitionFrameState(
                    mainAlpha = combinedAlpha,
                    scale = 0.96f + 0.04f * combinedAlpha,
                    sparkleDissolvePhase = if (enterLinear < 1f) (1f - enterEased) else (1f - exitEased),
                    previousAlpha = if (hasPreviousSlice && enterLinear < 1f) (1f - enterEased) * 0.7f else 0f
                )
            }

            TransitionEffectType.SOFT_BLUR -> {
                val enterBlur = (1f - enterEased) * 14f
                val exitBlur = (1f - exitEased) * 10f
                TransitionFrameState(
                    mainAlpha = min(enterEased, exitEased).coerceAtLeast(0.08f),
                    blurRadiusPx = max(enterBlur, exitBlur),
                    scale = 0.97f + 0.03f * enterEased
                )
            }

            TransitionEffectType.TYPE_ON_REVEAL -> {
                // Progressive word-by-word reveal across the first 70% of the phrase duration
                val revealWindowMs = max(enterWindowMs * 2, (phraseDurationMs * 0.68f).toLong())
                val revealFraction = (localPhraseTimeMs.toFloat() / revealWindowMs.toFloat()).coerceIn(0f, 1f)
                val visibleWords = max(1, kotlin.math.ceil(revealFraction * totalWordsInPhrase).toInt())
                TransitionFrameState(
                    mainAlpha = exitEased,
                    visibleWordCount = visibleWords,
                    scale = 1f
                )
            }

            TransitionEffectType.ELEGANT_CROSSFADE -> {
                TransitionFrameState(
                    mainAlpha = min(enterEased, exitEased),
                    offsetYNorm = (1f - enterEased) * 0.018f - (1f - exitEased) * 0.012f,
                    scale = 0.96f + 0.04f * enterEased,
                    previousAlpha = if (hasPreviousSlice && enterLinear < 1f) (1f - enterEased) * 0.75f else 0f,
                    previousScale = 1f - enterEased * 0.03f
                )
            }
        }
    }

    /**
     * Intelligent Automatic Verse Positioning & Safe-Area Layout Calculator (Requirement 04).
     * Automatically calculates non-overlapping positions and safe font sizes for:
     * - 9:16 Portrait (TikTok, Instagram Reels, YouTube Shorts safe margins)
     * - 16:9 Landscape (YouTube widescreen safe margins)
     * - 1:1 Square (Instagram / Social feed post)
     */
    fun computeAutoLayout(
        aspectRatio: VideoAspectRatio,
        currentTextStyle: QuranTextStyleConfig,
        currentHeader: RecitationHeaderConfig,
        currentWatermark: WatermarkConfig
    ): AutoLayoutResult {
        return when (aspectRatio) {
            VideoAspectRatio.PORTRAIT_9_16 -> {
                // Safe zone away from top status bar (y >= 0.18) and bottom Reels/TikTok captions (y <= 0.82)
                AutoLayoutResult(
                    textStyle = currentTextStyle.copy(
                        normalizedX = 0.50f,
                        normalizedY = 0.48f,
                        fontSizeSp = 28f,
                        isAutoPositioned = true
                    ),
                    headerConfig = currentHeader.copy(
                        surahName = currentHeader.surahName.copy(
                            normalizedX = 0.50f,
                            normalizedY = 0.22f,
                            fontSizeSp = 22f,
                            marginDp = 18f
                        ),
                        verseNumber = currentHeader.verseNumber.copy(
                            normalizedX = 0.50f,
                            normalizedY = 0.28f,
                            fontSizeSp = 15f,
                            marginDp = 16f
                        ),
                        reciterName = currentHeader.reciterName.copy(
                            normalizedX = 0.50f,
                            normalizedY = 0.66f,
                            fontSizeSp = 16f,
                            marginDp = 18f
                        )
                    ),
                    watermarkConfig = currentWatermark.copy(
                        normalizedX = 0.50f,
                        normalizedY = 0.79f,
                        fontSizeSp = 15f,
                        marginDp = 18f
                    )
                )
            }

            VideoAspectRatio.LANDSCAPE_16_9 -> {
                // Widescreen layout: spacious horizontal room, balanced vertical distribution
                AutoLayoutResult(
                    textStyle = currentTextStyle.copy(
                        normalizedX = 0.50f,
                        normalizedY = 0.49f,
                        fontSizeSp = 30f,
                        isAutoPositioned = true
                    ),
                    headerConfig = currentHeader.copy(
                        surahName = currentHeader.surahName.copy(
                            normalizedX = 0.50f,
                            normalizedY = 0.17f,
                            fontSizeSp = 23f,
                            marginDp = 24f
                        ),
                        verseNumber = currentHeader.verseNumber.copy(
                            normalizedX = 0.50f,
                            normalizedY = 0.25f,
                            fontSizeSp = 15f,
                            marginDp = 20f
                        ),
                        reciterName = currentHeader.reciterName.copy(
                            normalizedX = 0.50f,
                            normalizedY = 0.72f,
                            fontSizeSp = 16f,
                            marginDp = 24f
                        )
                    ),
                    watermarkConfig = currentWatermark.copy(
                        normalizedX = 0.50f,
                        normalizedY = 0.85f,
                        fontSizeSp = 14f,
                        marginDp = 20f
                    )
                )
            }

            VideoAspectRatio.SQUARE_1_1 -> {
                // Square 1:1 balanced post layout
                AutoLayoutResult(
                    textStyle = currentTextStyle.copy(
                        normalizedX = 0.50f,
                        normalizedY = 0.48f,
                        fontSizeSp = 27f,
                        isAutoPositioned = true
                    ),
                    headerConfig = currentHeader.copy(
                        surahName = currentHeader.surahName.copy(
                            normalizedX = 0.50f,
                            normalizedY = 0.18f,
                            fontSizeSp = 21f,
                            marginDp = 18f
                        ),
                        verseNumber = currentHeader.verseNumber.copy(
                            normalizedX = 0.50f,
                            normalizedY = 0.25f,
                            fontSizeSp = 14f,
                            marginDp = 16f
                        ),
                        reciterName = currentHeader.reciterName.copy(
                            normalizedX = 0.50f,
                            normalizedY = 0.69f,
                            fontSizeSp = 15f,
                            marginDp = 18f
                        )
                    ),
                    watermarkConfig = currentWatermark.copy(
                        normalizedX = 0.50f,
                        normalizedY = 0.82f,
                        fontSizeSp = 14f,
                        marginDp = 16f
                    )
                )
            }
        }
    }

    /**
     * Converts Arabic-Indic (٠-٩) or Extended Arabic-Indic (۰-۹) digits to standard Western digits (0-9)
     * and strips any non-digit characters so numeric input fields accept both keyboards seamlessly.
     */
    fun normalizeNumericInput(raw: String): String {
        val sb = StringBuilder(raw.length)
        for (ch in raw) {
            when (ch) {
                in '0'..'9' -> sb.append(ch)
                in '٠'..'٩' -> sb.append(('0'.code + (ch.code - '٠'.code)).toChar())
                in '۰'..'۹' -> sb.append(('0'.code + (ch.code - '۰'.code)).toChar())
            }
        }
        return sb.toString()
    }

    fun toArabicNumerals(number: Int): String {
        val arabicDigits = charArrayOf('٠', '١', '٢', '٣', '٤', '٥', '٦', '٧', '٨', '٩')
        return number.toString().map { ch ->
            if (ch in '0'..'9') arabicDigits[ch - '0'] else ch
        }.joinToString("")
    }

    private fun easeOutCubic(t: Float): Float {
        val inv = 1f - t.coerceIn(0f, 1f)
        return 1f - inv * inv * inv
    }

    private fun easeInOutSine(t: Float): Float {
        val clamped = t.coerceIn(0f, 1f)
        return (-(cos(Math.PI * clamped) - 1.0) / 2.0).toFloat()
    }
}
