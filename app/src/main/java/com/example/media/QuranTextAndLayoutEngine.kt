package com.example.media

import android.text.SpannableString
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import com.example.data.QuranDataRepository
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

    private fun isStandaloneWaqfToken(token: String): Boolean {
        return token.isNotEmpty() && token.all { it.code in 0x06D6..0x06ED }
    }

    /**
     * Arabic prepositions, particles, demonstratives, and relative pronouns that must NEVER
     * be left dangling at the end of a phrase slice (e.g. never split right after "فِىٓ" or "مِن" or "لَا").
     */
    private val NON_BREAKABLE_ENDING_SKELETONS = setOf(
        "في", "من", "الي", "علي", "عن", "مع", "حتي", "مذ", "منذ",
        "ان", "لا", "ما", "لم", "لن", "قد", "لقد", "سوف", "هل", "بل", "ام", "او", "ثم",
        "الذي", "التي", "الذين", "اللائي", "اللاتي",
        "هذا", "هذه", "ذلك", "تلك", " اولئك",
        "غير", "دون", "بين", "عند", "قبل", "بعد", "فوق", "تحت", "حول", "كل", "بعض", "اي",
        "يا", "ايها", "ايتها", "رب", "بسم", "باسم"
    )

    private fun isNonBreakableEndingWord(token: String): Boolean {
        val skeleton = QuranDataRepository.toConsonantalSkeleton(token)
        return skeleton in NON_BREAKABLE_ENDING_SKELETONS
    }

    private fun isNaturalClauseStarterWord(token: String): Boolean {
        val skeleton = QuranDataRepository.toConsonantalSkeleton(token)
        return skeleton.startsWith("فلهم") ||
            skeleton.startsWith("فله") ||
            skeleton.startsWith("ولا") ||
            skeleton.startsWith("وما") ||
            skeleton.startsWith("والذين") ||
            skeleton.startsWith("فمن") ||
            skeleton.startsWith("الا") ||
            skeleton.startsWith("ثم") ||
            skeleton.startsWith("حتي") ||
            skeleton.startsWith("كذلك") ||
            skeleton.startsWith("اولئك") ||
            skeleton.startsWith("وهو") ||
            skeleton.startsWith("انما")
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
     * Segments a Quranic verse into complete, natural single-line phrases (Sections 01, 02, 03, 06, 07):
     * 1. Enforces Madinah Mushaf (Hafs 'an Asim) Basmala accuracy: strips any unrecited leading Basmala
     *    from Verse 1 of Surahs 2..8 and 10..114, preserves Surah Al-Fatihah 1:1, never adds Basmala to
     *    Surah Al-Tawbah 9, and preserves the internal Basmala in Surah An-Naml 27:30.
     * 2. If the full verse fits on one single readable line (<= 8 words and <= 48 base characters, e.g.
     *    Surah At-Tin 95:1..5 & 95:7..8, Al-Ikhlas 112:1..4, Al-Fatihah 1:1..6, An-Naml 27:30),
     *    displays the COMPLETE verse on one line without splitting it!
     * 3. If a verse is longer (e.g. 95:6, 1:7, or Ayat al-Kursi 2:255), splits ONLY at natural Quranic
     *    Waqf pause marks (\u06D6..\u06DC) or complete grammatical clauses, NEVER breaking after a
     *    preposition/particle (such as "فِىٓ" or "مِن" or "لَا").
     * 4. Synchronizes phrase start/end boundaries strictly to the reciter's word-level timestamps
     *    (or acoustic speech-envelope forced alignment), advancing to the next phrase ONLY when the
     *    reciter begins uttering the first word of the next phrase.
     */
    fun segmentVerseIntoSingleLinePhrases(
        verse: QuranVerse,
        segment: VerseAudioSegment?,
        maxWordsPerPhrase: Int = 8,
        maxBaseCharsPerPhrase: Int = 48
    ): List<PhraseSlice> {
        val sanitizedText = QuranDataRepository.sanitizeVerseTextForMadinahMushaf(
            surahNumber = verse.surahNumber,
            verseNumber = verse.verseNumber,
            rawUthmaniText = verse.uthmaniText
        )

        // Merge standalone Waqf tokens (like "ۚ" or "ۖ") onto the preceding word so a phrase never starts with a bare pause sign
        val rawSplit = sanitizedText
            .trim()
            .split(Regex("\\s+"))
            .filter { it.isNotBlank() }

        val mergedTokens = mutableListOf<String>()
        for (token in rawSplit) {
            if (isStandaloneWaqfToken(token) && mergedTokens.isNotEmpty()) {
                val lastIdx = mergedTokens.lastIndex
                mergedTokens[lastIdx] = "${mergedTokens[lastIdx]} $token"
            } else {
                mergedTokens.add(token)
            }
        }

        if (mergedTokens.isEmpty()) {
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

        data class RawPhraseGroup(
            val words: MutableList<String>,
            var startWord1Based: Int,
            var endWord1Based: Int
        )

        val totalBaseCharsInVerse = mergedTokens.sumOf { tok ->
            tok.count { !isQuranicDiacritic(it) && !it.isWhitespace() }.coerceAtLeast(1)
        }
        val hasMidVerseWaqf = mergedTokens.dropLast(1).any { hasWaqfMark(it) }

        val groups = mutableListOf<RawPhraseGroup>()

        // Rule A: If the entire verse fits on one single line and has no mid-verse Waqf pause split requirement,
        // keep the COMPLETE verse as a single undivided line!
        if (mergedTokens.size <= maxWordsPerPhrase && totalBaseCharsInVerse <= maxBaseCharsPerPhrase && !hasMidVerseWaqf) {
            groups.add(
                RawPhraseGroup(
                    words = mergedTokens.toMutableList(),
                    startWord1Based = 1,
                    endWord1Based = mergedTokens.size
                )
            )
        } else {
            // Rule B: First split into natural clauses at Quranic Waqf marks (\u06D6..\u06DC)
            val waqfClauses = mutableListOf<RawPhraseGroup>()
            var clauseWords = mutableListOf<String>()
            var clauseStartIdx = 1

            mergedTokens.forEachIndexed { idx, token ->
                val word1Based = idx + 1
                if (clauseWords.isEmpty()) {
                    clauseStartIdx = word1Based
                }
                clauseWords.add(token)
                val isLast = idx == mergedTokens.lastIndex
                if (!isLast && hasWaqfMark(token) && clauseWords.size >= 2) {
                    waqfClauses.add(
                        RawPhraseGroup(
                            words = clauseWords.toMutableList(),
                            startWord1Based = clauseStartIdx,
                            endWord1Based = word1Based
                        )
                    )
                    clauseWords = mutableListOf()
                }
            }
            if (clauseWords.isNotEmpty()) {
                if (clauseWords.size == 1 && waqfClauses.isNotEmpty()) {
                    val prev = waqfClauses.last()
                    prev.words.addAll(clauseWords)
                    prev.endWord1Based = mergedTokens.size
                } else {
                    waqfClauses.add(
                        RawPhraseGroup(
                            words = clauseWords.toMutableList(),
                            startWord1Based = clauseStartIdx,
                            endWord1Based = mergedTokens.size
                        )
                    )
                }
            }

            // For each Waqf clause, if it fits on one line, keep it whole; otherwise split into balanced complete sub-clauses
            for (clause in waqfClauses) {
                val clauseBaseChars = clause.words.sumOf { tok ->
                    tok.count { !isQuranicDiacritic(it) && !it.isWhitespace() }.coerceAtLeast(1)
                }
                if (clause.words.size <= maxWordsPerPhrase && clauseBaseChars <= maxBaseCharsPerPhrase) {
                    groups.add(clause)
                } else {
                    // Split long clause at natural connector words or balanced midpoint without ever breaking after a preposition
                    var subWords = mutableListOf<String>()
                    var subBaseChars = 0
                    var subStartWord1Based = clause.startWord1Based

                    clause.words.forEachIndexed { localIdx, word ->
                        val globalWord1Based = clause.startWord1Based + localIdx
                        val wordChars = word.count { !isQuranicDiacritic(it) && !it.isWhitespace() }.coerceAtLeast(1)
                        val remainingWordsInClause = clause.words.size - localIdx

                        // Check if we should break BEFORE this word because it is a natural clause starter (like "فَلَهُمْ" or "وَلَا")
                        val canBreakBeforeCurrent = subWords.size >= 4 &&
                            remainingWordsInClause >= 3 &&
                            isNaturalClauseStarterWord(word) &&
                            !isNonBreakableEndingWord(subWords.last())

                        if (canBreakBeforeCurrent) {
                            groups.add(
                                RawPhraseGroup(
                                    words = subWords.toMutableList(),
                                    startWord1Based = subStartWord1Based,
                                    endWord1Based = globalWord1Based - 1
                                )
                            )
                            subWords = mutableListOf()
                            subBaseChars = 0
                            subStartWord1Based = globalWord1Based
                        }

                        if (subWords.isEmpty()) {
                            subStartWord1Based = globalWord1Based
                        }
                        subWords.add(word)
                        subBaseChars += wordChars

                        val remainingAfterCurrent = clause.words.size - (localIdx + 1)
                        val reachedLimit = subWords.size >= maxWordsPerPhrase || subBaseChars >= maxBaseCharsPerPhrase

                        // Never break if current word is a preposition/particle or if only 1..2 words remain in the clause
                        if (reachedLimit && remainingAfterCurrent >= 3 && !isNonBreakableEndingWord(word)) {
                            groups.add(
                                RawPhraseGroup(
                                    words = subWords.toMutableList(),
                                    startWord1Based = subStartWord1Based,
                                    endWord1Based = globalWord1Based
                                )
                            )
                            subWords = mutableListOf()
                            subBaseChars = 0
                        }
                    }

                    if (subWords.isNotEmpty()) {
                        if (subWords.size <= 2 && groups.isNotEmpty()) {
                            val prev = groups.last()
                            prev.words.addAll(subWords)
                            prev.endWord1Based = clause.endWord1Based
                        } else {
                            groups.add(
                                RawPhraseGroup(
                                    words = subWords.toMutableList(),
                                    startWord1Based = subStartWord1Based,
                                    endWord1Based = clause.endWord1Based
                                )
                            )
                        }
                    }
                }
            }
        }

        // Append ornate verse number symbol to the final phrase
        val ayahSymbol = "﴿${toArabicNumerals(verse.verseNumber)}﴾"
        if (groups.isNotEmpty()) {
            groups.last().words.add(ayahSymbol)
        }

        val totalDurationMs = max(1200L, segment?.durationMs ?: (groups.size * 3200L))
        val speechStartMs = (segment?.speechStartMs ?: 0L).coerceIn(0L, max(0L, totalDurationMs - 300L))
        val speechEndMs = (segment?.speechEndMs ?: totalDurationMs).coerceIn(speechStartMs + 250L, totalDurationMs)

        // Ensure we have calibrated word timings for every word in mergedTokens
        val rawTimings = segment?.wordTimings.orEmpty().sortedBy { it.wordIndex }
        val effectiveWordTimings = if (rawTimings.size == mergedTokens.size && rawTimings.last().endTimeMs > rawTimings.first().startTimeMs) {
            rawTimings
        } else if (rawTimings.size >= 2 && rawTimings.last().endTimeMs > rawTimings.first().startTimeMs) {
            // Re-calibrate existing wordTimings onto mergedTokens count
            com.example.data.AudioDownloadManager.buildCalibratedWordTimings(
                surahNumber = verse.surahNumber,
                verseNumber = verse.verseNumber,
                verseWords = mergedTokens,
                totalDurationMs = totalDurationMs,
                envelope = com.example.data.AcousticSpeechEnvelope(
                    speechStartMs = speechStartMs,
                    speechEndMs = speechEndMs,
                    pauseMidpointsMs = segment?.acousticPauseWindowsMs.orEmpty(),
                    containsLeadingBasmalaAudio = segment?.containsLeadingBasmalaAudio == true
                ),
                rawApiTimings = rawTimings
            )
        } else {
            // Generate phonetic + acoustic pause forced-aligned word timings across [speechStartMs..speechEndMs]
            com.example.data.AudioDownloadManager.buildCalibratedWordTimings(
                surahNumber = verse.surahNumber,
                verseNumber = verse.verseNumber,
                verseWords = mergedTokens,
                totalDurationMs = totalDurationMs,
                envelope = com.example.data.AcousticSpeechEnvelope(
                    speechStartMs = speechStartMs,
                    speechEndMs = speechEndMs,
                    pauseMidpointsMs = segment?.acousticPauseWindowsMs.orEmpty(),
                    containsLeadingBasmalaAudio = segment?.containsLeadingBasmalaAudio == true
                ),
                rawApiTimings = emptyList()
            )
        }

        val totalGroups = groups.size
        val slices = mutableListOf<PhraseSlice>()

        // Compute each phrase's exact startMs from the first word of that phrase,
        // and keep the current phrase visible until the reciter starts the first word of the NEXT phrase!
        val phraseOnsetTimesMs = LongArray(totalGroups)
        for (index in 0 until totalGroups) {
            if (index == 0) {
                phraseOnsetTimesMs[index] = 0L
            } else {
                val firstWordIdx = groups[index].startWord1Based
                val matchingFirstWord = effectiveWordTimings.find { it.wordIndex == firstWordIdx }
                    ?: effectiveWordTimings.getOrNull((firstWordIdx - 1).coerceIn(0, effectiveWordTimings.lastIndex))
                val prevOnset = phraseOnsetTimesMs[index - 1]
                phraseOnsetTimesMs[index] = (matchingFirstWord?.startTimeMs ?: (prevOnset + 400L))
                    .coerceIn(prevOnset + 250L, totalDurationMs - (totalGroups - index) * 200L)
            }
        }

        for (index in 0 until totalGroups) {
            val group = groups[index]
            val startMs = phraseOnsetTimesMs[index]
            val endMs = if (index == totalGroups - 1) {
                totalDurationMs
            } else {
                phraseOnsetTimesMs[index + 1]
            }

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

        return slices
    }

    /**
     * Resolves the currently active phrase and its transition state at [localVerseTimeMs]
     * within the given [verse] and [segment], driven strictly by the audio playback clock.
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
            // Even when singleLineMode is disabled, split ultra-long verses (> 16 words) into readable pages so they never truncate
            segmentVerseIntoSingleLinePhrases(
                verse = verse,
                segment = segment,
                maxWordsPerPhrase = 16,
                maxBaseCharsPerPhrase = 105
            )
        }

        val currentIdx = slices.indexOfLast { clampedVerseTimeMs >= it.startMs }
            .coerceIn(0, slices.lastIndex)
        val currentSlice = slices[currentIdx]
        val previousSlice = if (currentIdx > 0) slices[currentIdx - 1] else null

        val phraseDurationMs = max(350L, currentSlice.endMs - currentSlice.startMs)
        val localPhraseTimeMs = (clampedVerseTimeMs - currentSlice.startMs).coerceIn(0L, phraseDurationMs)

        // Determine active word index inside currentSlice using word-level timings when available
        val wordCount = currentSlice.words.size.coerceAtLeast(1)
        val wordTimings = segment?.wordTimings.orEmpty()
        val activeWordInPhrase = if (wordTimings.isNotEmpty()) {
            val activeGlobalWord = wordTimings.lastOrNull { clampedVerseTimeMs >= it.startTimeMs }?.wordIndex
                ?: currentSlice.startWordIndex1Based
            (activeGlobalWord - currentSlice.startWordIndex1Based).coerceIn(0, wordCount - 1)
        } else {
            val progressInPhrase = (localPhraseTimeMs.toFloat() / phraseDurationMs.toFloat()).coerceIn(0f, 0.999f)
            (progressInPhrase * wordCount).toInt().coerceIn(0, wordCount - 1)
        }

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
