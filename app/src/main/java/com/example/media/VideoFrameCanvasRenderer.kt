package com.example.media

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BlurMaskFilter
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.LinearGradient
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import android.text.Layout
import android.text.StaticLayout
import android.text.TextDirectionHeuristics
import android.text.TextPaint
import com.example.data.QuranDataRepository
import com.example.model.ArabicQuranFont
import com.example.model.HeaderElementConfig
import com.example.model.MediaCropMode
import com.example.model.PhoneMediaBackground
import com.example.model.QuranTextAlignment
import com.example.model.QuranTextStyleConfig
import com.example.model.QuranVerse
import com.example.model.RecitationHeaderConfig
import com.example.model.SocialPlatform
import com.example.model.VerseAudioSegment
import com.example.model.VerseTransitionConfig
import com.example.model.WatermarkConfig
import kotlin.math.max
import kotlin.math.min

enum class DraggableCanvasElement {
    QURAN_VERSE,
    SURAH_NAME,
    VERSE_NUMBER,
    RECITER_NAME,
    SIGNATURE_WATERMARK
}

data class FrameElementHitBoxes(
    val quranVerseBounds: RectF = RectF(),
    val surahNameBounds: RectF = RectF(),
    val verseNumberBounds: RectF = RectF(),
    val reciterNameBounds: RectF = RectF(),
    val signatureBounds: RectF = RectF()
)

class VideoFrameCanvasRenderer(private val context: Context) {

    private val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    private val dimPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val badgePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val badgeBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 2f
        color = Color.argb(140, 232, 135, 47) // #E8872F
    }
    private val socialIconFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.rgb(232, 135, 47) // Approved Tasneem Orange #E8872F
    }
    private val socialIconStrokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
        color = Color.rgb(232, 135, 47) // Approved Tasneem Orange #E8872F
    }

    fun renderFrame(
        canvas: Canvas,
        frameWidth: Int,
        frameHeight: Int,
        backgroundBitmap: Bitmap?,
        mediaConfig: PhoneMediaBackground,
        surahNameAr: String,
        activeVerse: QuranVerse,
        activeSegment: VerseAudioSegment? = null,
        localVerseTimeMs: Long = 0L,
        reciterNameAr: String,
        textConfig: QuranTextStyleConfig,
        headerConfig: RecitationHeaderConfig = RecitationHeaderConfig(),
        transitionConfig: VerseTransitionConfig = VerseTransitionConfig(),
        watermarkConfig: WatermarkConfig,
        drawBackground: Boolean = true
    ): FrameElementHitBoxes {
        val w = frameWidth.toFloat().coerceAtLeast(1f)
        val h = frameHeight.toFloat().coerceAtLeast(1f)
        val scaleFactor = min(w, h) / 1080f

        if (drawBackground) {
            // 1. Base background fill (#1D2227 Deep Charcoal)
            canvas.drawColor(Color.rgb(29, 34, 39))

            // 2. Draw user-selected or curated background image / video frame
            if (backgroundBitmap != null && !backgroundBitmap.isRecycled) {
                val brightnessShift = (mediaConfig.brightness * 255f).coerceIn(-160f, 160f)
                val cm = ColorMatrix(
                    floatArrayOf(
                        1f, 0f, 0f, 0f, brightnessShift,
                        0f, 1f, 0f, 0f, brightnessShift,
                        0f, 0f, 1f, 0f, brightnessShift,
                        0f, 0f, 0f, 1f, 0f
                    )
                )
                bgPaint.colorFilter = ColorMatrixColorFilter(cm)

                val srcW = backgroundBitmap.width.toFloat().coerceAtLeast(1f)
                val srcH = backgroundBitmap.height.toFloat().coerceAtLeast(1f)
                val baseScale = when (mediaConfig.cropMode) {
                    MediaCropMode.CENTER_CROP, MediaCropMode.CUSTOM_PAN_ZOOM -> max(w / srcW, h / srcH)
                    MediaCropMode.FIT_CENTER -> min(w / srcW, h / srcH)
                }
                val finalScale = baseScale * mediaConfig.scale.coerceIn(0.4f, 4.0f)

                val dx = (w - srcW * finalScale) / 2f + (mediaConfig.offsetX * w * 0.35f)
                val dy = (h - srcH * finalScale) / 2f + (mediaConfig.offsetY * h * 0.35f)

                val matrix = Matrix().apply {
                    postScale(finalScale, finalScale)
                    postTranslate(dx, dy)
                }
                canvas.drawBitmap(backgroundBitmap, matrix, bgPaint)
            } else {
                // Subtle dark silver & charcoal gradient when no image is selected
                val defaultGrad = LinearGradient(
                    0f, 0f, 0f, h,
                    intArrayOf(
                        Color.rgb(25, 30, 36),
                        Color.rgb(42, 48, 54),
                        Color.rgb(24, 28, 33)
                    ),
                    floatArrayOf(0f, 0.5f, 1f),
                    Shader.TileMode.CLAMP
                )
                dimPaint.shader = defaultGrad
                canvas.drawRect(0f, 0f, w, h, dimPaint)
                dimPaint.shader = null
            }
        }

        // 3. Draw background dimming & subtle vignette for Quranic text legibility
        val dimAlpha = (mediaConfig.dimLevel.coerceIn(0f, 0.92f) * 255).toInt()
        dimPaint.shader = null
        dimPaint.color = Color.argb(dimAlpha, 18, 22, 26)
        canvas.drawRect(0f, 0f, w, h, dimPaint)

        val vignetteGradient = LinearGradient(
            0f, 0f, 0f, h,
            intArrayOf(
                Color.argb(115, 14, 17, 21),
                Color.argb(20, 14, 17, 21),
                Color.argb(130, 14, 17, 21)
            ),
            floatArrayOf(0f, 0.5f, 1f),
            Shader.TileMode.CLAMP
        )
        dimPaint.shader = vignetteGradient
        canvas.drawRect(0f, 0f, w, h, dimPaint)
        dimPaint.shader = null

        // 4. Resolve Active Quranic Phrase & Transition State (Requirements 05, 06, 09)
        val phraseRenderState = QuranTextAndLayoutEngine.resolveActivePhraseAtTime(
            verse = activeVerse,
            segment = activeSegment,
            localVerseTimeMs = localVerseTimeMs,
            singleLineMode = textConfig.singleLineDisplay,
            transitionConfig = transitionConfig
        )

        val quranVerseBounds = drawActiveQuranPhrase(
            canvas = canvas,
            frameWidth = w,
            frameHeight = h,
            scaleFactor = scaleFactor,
            activeVerse = activeVerse,
            phraseRenderState = phraseRenderState,
            textConfig = textConfig
        )

        // 5. Draw Independent Recitation Info Elements (Requirement 08):
        // A. Surah Name ("اسم السورة")
        val surahBounds = if (headerConfig.surahName.visible && surahNameAr.isNotBlank()) {
            drawIndependentHeaderElement(
                canvas = canvas,
                frameWidth = w,
                frameHeight = h,
                scaleFactor = scaleFactor,
                text = "سُورَةُ $surahNameAr",
                config = headerConfig.surahName,
                avoidRect = quranVerseBounds,
                preferAbove = true
            )
        } else RectF()

        // B. Verse Number ("رقم الآية")
        val verseNumBounds = if (headerConfig.verseNumber.visible) {
            drawIndependentHeaderElement(
                canvas = canvas,
                frameWidth = w,
                frameHeight = h,
                scaleFactor = scaleFactor,
                text = "الآية ${QuranDataRepository.toArabicDigits(activeVerse.verseNumber)}",
                config = headerConfig.verseNumber,
                avoidRect = quranVerseBounds,
                preferAbove = true
            )
        } else RectF()

        // C. Reciter Name ("اسم القارئ")
        val reciterBounds = if (headerConfig.reciterName.visible && reciterNameAr.isNotBlank()) {
            drawIndependentHeaderElement(
                canvas = canvas,
                frameWidth = w,
                frameHeight = h,
                scaleFactor = scaleFactor,
                text = "القارئ: $reciterNameAr",
                config = headerConfig.reciterName,
                avoidRect = quranVerseBounds,
                preferAbove = false
            )
        } else RectF()

        // 6. Draw Personal Signature & Social Media Vector Icons (Requirement 10)
        val signatureBounds = if (watermarkConfig.enabled) {
            drawSignatureAndSocialIcons(
                canvas = canvas,
                frameWidth = w,
                frameHeight = h,
                scaleFactor = scaleFactor,
                watermarkConfig = watermarkConfig,
                quranTextBounds = quranVerseBounds
            )
        } else RectF()

        return FrameElementHitBoxes(
            quranVerseBounds = quranVerseBounds,
            surahNameBounds = surahBounds,
            verseNumberBounds = verseNumBounds,
            reciterNameBounds = reciterBounds,
            signatureBounds = signatureBounds
        )
    }

    private fun drawActiveQuranPhrase(
        canvas: Canvas,
        frameWidth: Float,
        frameHeight: Float,
        scaleFactor: Float,
        activeVerse: QuranVerse,
        phraseRenderState: ActivePhraseRenderState,
        textConfig: QuranTextStyleConfig
    ): RectF {
        val quranTypeface = textConfig.font.loadTypeface(context, textConfig.isBold)
        val uiTypeface = ArabicQuranFont.CAIRO.loadTypeface(context, bold = true)

        val transition = phraseRenderState.transitionState
        val alphaMultiplier = transition.mainAlpha.coerceIn(0f, 1f)

        val rawMainColor = (textConfig.mainTextColorHex or 0xFF000000L).toInt()
        val rawTashkeelColor = (textConfig.tashkeelColorHex or 0xFF000000L).toInt()

        val mainColorWithAlpha = applyAlpha(rawMainColor, alphaMultiplier)
        val tashkeelColorWithAlpha = applyAlpha(rawTashkeelColor, alphaMultiplier)

        val maxTextWidthPx = (frameWidth * 0.90f).toInt().coerceAtLeast(64)
        var targetTextSizePx = (textConfig.fontSizeSp * 2.65f * scaleFactor)
            .coerceIn(10f * scaleFactor, 155f * scaleFactor)

        // Prepare display text (respecting Type-on Reveal visibleWordCount if active)
        val words = phraseRenderState.currentSlice.words
        val visibleWords = if (transition.visibleWordCount < words.size) {
            words.take(transition.visibleWordCount.coerceAtLeast(1))
        } else {
            words
        }
        val phraseText = visibleWords.joinToString(" ")
        val fullMeasureText = phraseRenderState.currentSlice.text

        val verseTextPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = quranTypeface
            textSize = targetTextSizePx
            color = mainColorWithAlpha
            isFakeBoldText = textConfig.isBold
            val shadowAlpha = (200 * alphaMultiplier).toInt().coerceIn(0, 255)
            setShadowLayer(10f * scaleFactor, 0f, 4f * scaleFactor, Color.argb(shadowAlpha, 0, 0, 0))
            if (transition.blurRadiusPx > 0.6f) {
                maskFilter = BlurMaskFilter(transition.blurRadiusPx * scaleFactor.coerceAtLeast(0.5f), BlurMaskFilter.Blur.NORMAL)
            }
        }

        // Section 07: In singleLineDisplay mode, measure the full rendered Uthmani phrase + verse symbol
        // in the exact scaled coordinate space so Live Preview and 1080p/720p export fit 100% identically
        // without wrapping or clipping the final word or verse number symbol.
        var measuredFullWidthPx = if (fullMeasureText.isNotEmpty()) {
            max(
                verseTextPaint.measureText(fullMeasureText),
                Layout.getDesiredWidth(fullMeasureText, verseTextPaint)
            )
        } else 0f

        if (textConfig.singleLineDisplay && fullMeasureText.isNotEmpty()) {
            val minAllowedPx = (12f * 2.2f * scaleFactor).coerceAtLeast(6f)
            val safeTargetWidthPx = maxTextWidthPx * 0.94f
            while (measuredFullWidthPx > safeTargetWidthPx && targetTextSizePx > minAllowedPx) {
                targetTextSizePx *= 0.94f
                verseTextPaint.textSize = targetTextSizePx
                measuredFullWidthPx = max(
                    verseTextPaint.measureText(fullMeasureText),
                    Layout.getDesiredWidth(fullMeasureText, verseTextPaint)
                )
            }
        }

        // Build SpannableString with independent Tashkeel color (Requirement 03)
        val spannablePhrase = QuranTextAndLayoutEngine.buildQuranSpannable(
            text = phraseText,
            mainColorInt = mainColorWithAlpha,
            enableIndependentTashkeel = textConfig.enableIndependentTashkeelColor,
            tashkeelColorInt = tashkeelColorWithAlpha
        )

        val layoutAlignment = when (textConfig.alignment) {
            QuranTextAlignment.CENTER -> Layout.Alignment.ALIGN_CENTER
            QuranTextAlignment.RIGHT -> Layout.Alignment.ALIGN_NORMAL
            QuranTextAlignment.LEFT -> Layout.Alignment.ALIGN_OPPOSITE
        }

        // Guarantee StaticLayout container width has headroom above desired width in single-line mode
        // so HarfBuzz RTL ligatures and ornate verse numbers never wrap onto a truncated 2nd line
        val desiredSpannableWidth = Layout.getDesiredWidth(spannablePhrase, verseTextPaint)
        val verseLayoutWidthPx = if (textConfig.singleLineDisplay) {
            max(maxTextWidthPx, kotlin.math.ceil(max(measuredFullWidthPx, desiredSpannableWidth) + 28f * scaleFactor).toInt())
        } else {
            maxTextWidthPx
        }

        val verseLayout = StaticLayout.Builder
            .obtain(spannablePhrase, 0, spannablePhrase.length, verseTextPaint, verseLayoutWidthPx)
            .setAlignment(layoutAlignment)
            .setTextDirection(TextDirectionHeuristics.RTL)
            .setLineSpacing(0f, textConfig.lineSpacingMultiplier.coerceIn(1.1f, 2.4f))
            .setMaxLines(if (textConfig.singleLineDisplay) 1 else 6)
            .setIncludePad(true)
            .build()

        // Optional previous phrase crossfade/slide ghost during transition
        val prevSlice = phraseRenderState.previousSlice
        val prevLayout = if (prevSlice != null && transition.previousAlpha > 0.03f) {
            val prevAlpha = transition.previousAlpha.coerceIn(0f, 1f)
            val prevPaint = TextPaint(verseTextPaint).apply {
                color = applyAlpha(rawMainColor, prevAlpha)
                setShadowLayer(6f * scaleFactor, 0f, 2f * scaleFactor, Color.argb((140 * prevAlpha).toInt(), 0, 0, 0))
            }
            if (textConfig.singleLineDisplay && prevSlice.text.isNotEmpty()) {
                var prevW = max(prevPaint.measureText(prevSlice.text), Layout.getDesiredWidth(prevSlice.text, prevPaint))
                val minPrevPx = (12f * 2.2f * scaleFactor).coerceAtLeast(6f)
                while (prevW > maxTextWidthPx * 0.94f && prevPaint.textSize > minPrevPx) {
                    prevPaint.textSize *= 0.94f
                    prevW = max(prevPaint.measureText(prevSlice.text), Layout.getDesiredWidth(prevSlice.text, prevPaint))
                }
            }
            val prevSpannable = QuranTextAndLayoutEngine.buildQuranSpannable(
                text = prevSlice.text,
                mainColorInt = applyAlpha(rawMainColor, prevAlpha),
                enableIndependentTashkeel = textConfig.enableIndependentTashkeelColor,
                tashkeelColorInt = applyAlpha(rawTashkeelColor, prevAlpha)
            )
            val prevDesiredW = Layout.getDesiredWidth(prevSpannable, prevPaint)
            val prevLayoutWidthPx = if (textConfig.singleLineDisplay) {
                max(maxTextWidthPx, kotlin.math.ceil(prevDesiredW + 28f * scaleFactor).toInt())
            } else {
                maxTextWidthPx
            }
            StaticLayout.Builder
                .obtain(prevSpannable, 0, prevSpannable.length, prevPaint, prevLayoutWidthPx)
                .setAlignment(layoutAlignment)
                .setTextDirection(TextDirectionHeuristics.RTL)
                .setMaxLines(if (textConfig.singleLineDisplay) 1 else 4)
                .setIncludePad(true)
                .build()
        } else null

        // Optional Tafsir / Translation layouts below the active line
        val tafsirLayout = if (textConfig.showTafsir && activeVerse.tafsirMuyassar.isNotBlank()) {
            val tafsirPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                typeface = uiTypeface
                textSize = (targetTextSizePx * 0.54f).coerceAtLeast(12f * scaleFactor)
                color = applyAlpha(Color.rgb(245, 241, 234), alphaMultiplier)
                setShadowLayer(6f * scaleFactor, 0f, 2f * scaleFactor, Color.argb(180, 0, 0, 0))
            }
            StaticLayout.Builder
                .obtain(activeVerse.tafsirMuyassar, 0, activeVerse.tafsirMuyassar.length, tafsirPaint, maxTextWidthPx)
                .setAlignment(Layout.Alignment.ALIGN_CENTER)
                .setTextDirection(TextDirectionHeuristics.RTL)
                .setLineSpacing(0f, 1.25f)
                .setMaxLines(2)
                .build()
        } else null

        val translationLayout = if (textConfig.showTranslation && activeVerse.translationSaheeh.isNotBlank()) {
            val transPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                typeface = uiTypeface
                textSize = (targetTextSizePx * textConfig.translationFontSizeScale.coerceIn(0.45f, 1.0f))
                    .coerceAtLeast(11f * scaleFactor)
                color = applyAlpha(Color.rgb(197, 200, 203), alphaMultiplier)
                setShadowLayer(6f * scaleFactor, 0f, 2f * scaleFactor, Color.argb(180, 0, 0, 0))
            }
            StaticLayout.Builder
                .obtain(activeVerse.translationSaheeh, 0, activeVerse.translationSaheeh.length, transPaint, maxTextWidthPx)
                .setAlignment(Layout.Alignment.ALIGN_CENTER)
                .setLineSpacing(0f, 1.2f)
                .setMaxLines(2)
                .build()
        } else null

        val spacingPx = 18f * scaleFactor
        var totalBlockHeight = verseLayout.height.toFloat()
        if (tafsirLayout != null) totalBlockHeight += spacingPx + tafsirLayout.height
        if (translationLayout != null) totalBlockHeight += spacingPx + translationLayout.height

        val baseCenterX = (textConfig.normalizedX.coerceIn(0.12f, 0.88f)) * frameWidth
        val baseCenterY = (textConfig.normalizedY.coerceIn(0.14f, 0.86f)) * frameHeight

        val animatedCenterX = baseCenterX + (transition.offsetXNorm * frameWidth)
        val animatedCenterY = baseCenterY + (transition.offsetYNorm * frameHeight)
        val topY = (animatedCenterY - totalBlockHeight / 2f)
            .coerceIn(frameHeight * 0.06f, (frameHeight * 0.92f - totalBlockHeight).coerceAtLeast(frameHeight * 0.06f))

        // Draw outgoing phrase during crossfade/slide/zoom if active
        if (prevLayout != null) {
            canvas.save()
            val prevX = baseCenterX + (transition.previousOffsetXNorm * frameWidth)
            val prevY = baseCenterY - prevLayout.height / 2f
            canvas.translate(prevX, prevY + prevLayout.height / 2f)
            canvas.scale(transition.previousScale, transition.previousScale)
            canvas.translate(-prevLayout.width / 2f, -prevLayout.height / 2f)
            prevLayout.draw(canvas)
            canvas.restore()
        }

        // Draw current active Quranic phrase with scale & transition
        var cursorY = topY
        canvas.save()
        canvas.translate(animatedCenterX, cursorY + verseLayout.height / 2f)
        canvas.scale(transition.scale, transition.scale)
        canvas.translate(-verseLayout.width / 2f, -verseLayout.height / 2f)
        verseLayout.draw(canvas)
        canvas.restore()
        cursorY += verseLayout.height

        if (tafsirLayout != null) {
            cursorY += spacingPx
            canvas.save()
            canvas.translate(baseCenterX - maxTextWidthPx / 2f, cursorY)
            tafsirLayout.draw(canvas)
            canvas.restore()
            cursorY += tafsirLayout.height
        }

        if (translationLayout != null) {
            cursorY += spacingPx
            canvas.save()
            canvas.translate(baseCenterX - maxTextWidthPx / 2f, cursorY)
            translationLayout.draw(canvas)
            canvas.restore()
            cursorY += translationLayout.height
        }

        val hitWidth = min(maxTextWidthPx.toFloat(), measuredFullWidthPx + 64f * scaleFactor)
            .coerceAtLeast(140f * scaleFactor)

        return RectF(
            baseCenterX - hitWidth / 2f,
            topY - 14f * scaleFactor,
            baseCenterX + hitWidth / 2f,
            cursorY + 14f * scaleFactor
        )
    }

    /**
     * Draws one of the 3 independent Recitation Header elements (Surah Name, Verse Number, Reciter Name)
     * with its own font, size, color, opacity, alignment, margins, and automatic collision avoidance
     * against the Quranic text block (Requirement 08).
     */
    private fun drawIndependentHeaderElement(
        canvas: Canvas,
        frameWidth: Float,
        frameHeight: Float,
        scaleFactor: Float,
        text: String,
        config: HeaderElementConfig,
        avoidRect: RectF,
        preferAbove: Boolean
    ): RectF {
        val typeface = config.font.loadTypeface(context, bold = true)
        val alphaInt = (config.opacity.coerceIn(0.15f, 1.0f) * 255).toInt()
        val baseColorInt = (config.colorHex or 0xFF000000L).toInt()
        val colorWithAlpha = Color.argb(
            alphaInt,
            Color.red(baseColorInt),
            Color.green(baseColorInt),
            Color.blue(baseColorInt)
        )

        val textSizePx = (config.fontSizeSp * 2.4f * scaleFactor).coerceIn(16f, 96f)
        val marginPx = (config.marginDp * 2.2f * scaleFactor).coerceAtLeast(16f * scaleFactor)

        val paint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            this.typeface = typeface
            this.textSize = textSizePx
            this.color = colorWithAlpha
            this.textAlign = when (config.alignment) {
                QuranTextAlignment.CENTER -> Paint.Align.CENTER
                QuranTextAlignment.RIGHT -> Paint.Align.RIGHT
                QuranTextAlignment.LEFT -> Paint.Align.LEFT
            }
            setShadowLayer(8f * scaleFactor, 0f, 3f * scaleFactor, Color.argb((alphaInt * 0.85f).toInt(), 0, 0, 0))
        }

        val textWidth = paint.measureText(text).coerceAtLeast(60f * scaleFactor)
        val textHeight = (paint.fontMetrics.descent - paint.fontMetrics.ascent).coerceAtLeast(textSizePx)

        var centerX = (config.normalizedX.coerceIn(0.08f, 0.92f) * frameWidth)
            .coerceIn(marginPx + textWidth / 2f, (frameWidth - marginPx - textWidth / 2f).coerceAtLeast(marginPx + textWidth / 2f))
        var centerY = (config.normalizedY.coerceIn(0.06f, 0.94f) * frameHeight)
            .coerceIn(marginPx + textHeight / 2f, (frameHeight - marginPx - textHeight / 2f).coerceAtLeast(marginPx + textHeight / 2f))

        var candidateRect = RectF(
            centerX - textWidth / 2f - 12f * scaleFactor,
            centerY - textHeight / 2f - 6f * scaleFactor,
            centerX + textWidth / 2f + 12f * scaleFactor,
            centerY + textHeight / 2f + 6f * scaleFactor
        )

        // Prevent overlapping the main Quranic verse
        if (RectF.intersects(candidateRect, avoidRect)) {
            centerY = if (preferAbove) {
                (avoidRect.top - textHeight * 0.85f).coerceAtLeast(marginPx + textHeight / 2f)
            } else {
                (avoidRect.bottom + textHeight * 0.85f).coerceAtMost(frameHeight - marginPx - textHeight / 2f)
            }
            candidateRect = RectF(
                centerX - textWidth / 2f - 12f * scaleFactor,
                centerY - textHeight / 2f - 6f * scaleFactor,
                centerX + textWidth / 2f + 12f * scaleFactor,
                centerY + textHeight / 2f + 6f * scaleFactor
            )
        }

        val drawX = when (config.alignment) {
            QuranTextAlignment.CENTER -> centerX
            QuranTextAlignment.RIGHT -> candidateRect.right - 12f * scaleFactor
            QuranTextAlignment.LEFT -> candidateRect.left + 12f * scaleFactor
        }
        val baselineY = centerY - (paint.fontMetrics.ascent + paint.fontMetrics.descent) / 2f
        canvas.drawText(text, drawX, baselineY, paint)

        return candidateRect
    }

    /**
     * Draws the Personal Signature (defaulting to "تسنيم") and optional Social Media Icons
     * (Facebook, Instagram, TikTok, YouTube) centered directly below the signature in
     * approved Tasneem Orange (#E8872F) without platform names or usernames (Requirement 10).
     */
    private fun drawSignatureAndSocialIcons(
        canvas: Canvas,
        frameWidth: Float,
        frameHeight: Float,
        scaleFactor: Float,
        watermarkConfig: WatermarkConfig,
        quranTextBounds: RectF
    ): RectF {
        val marginPx = (watermarkConfig.marginDp * 2.2f * scaleFactor).coerceAtLeast(20f * scaleFactor)
        val wmTextSize = (watermarkConfig.fontSizeSp * 2.4f * watermarkConfig.scale * scaleFactor)
            .coerceIn(16f, 84f)
        val alphaInt = (watermarkConfig.opacity.coerceIn(0.15f, 1.0f) * 255).toInt()

        val sigTypeface = watermarkConfig.font.loadTypeface(context, bold = true)
        val baseSigColor = (watermarkConfig.colorHex or 0xFF000000L).toInt()

        val wmPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = sigTypeface
            textSize = wmTextSize
            color = Color.argb(
                alphaInt,
                Color.red(baseSigColor),
                Color.green(baseSigColor),
                Color.blue(baseSigColor)
            )
            textAlign = Paint.Align.CENTER
            if (watermarkConfig.elegantGlow) {
                setShadowLayer(12f * scaleFactor, 0f, 0f, Color.argb((alphaInt * 0.75f).toInt(), 232, 135, 47))
            } else if (watermarkConfig.subtleShadow) {
                setShadowLayer(8f * scaleFactor, 0f, 3f * scaleFactor, Color.argb((alphaInt * 0.9f).toInt(), 0, 0, 0))
            }
        }

        val accentDotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(alphaInt, 232, 135, 47) // Tasneem Orange #E8872F
        }

        val label = watermarkConfig.signatureText.ifBlank { "تسنيم" }
        val textWidth = wmPaint.measureText(label)
        val pillWidth = textWidth + 56f * scaleFactor
        val pillHeight = wmTextSize * 1.62f

        val platformsOrdered = SocialPlatform.entries.filter { it in watermarkConfig.selectedSocialPlatforms }
        val iconSizePx = (watermarkConfig.socialIconsSizeDp * 2.2f * watermarkConfig.scale * scaleFactor)
            .coerceIn(20f * scaleFactor, 72f * scaleFactor)
        val iconSpacingPx = iconSizePx * 0.52f
        val socialRowHeight = if (platformsOrdered.isNotEmpty()) iconSizePx + 18f * scaleFactor else 0f
        val totalGroupHeight = pillHeight + socialRowHeight

        val socialRowWidth = if (platformsOrdered.isNotEmpty()) {
            platformsOrdered.size * iconSizePx + (platformsOrdered.size - 1) * iconSpacingPx
        } else 0f
        val totalGroupWidth = max(pillWidth, socialRowWidth + 24f * scaleFactor)

        var targetX = (watermarkConfig.normalizedX * frameWidth)
            .coerceIn(marginPx + totalGroupWidth / 2f, (frameWidth - marginPx - totalGroupWidth / 2f).coerceAtLeast(marginPx + totalGroupWidth / 2f))
        var targetY = (watermarkConfig.normalizedY * frameHeight)
            .coerceIn(marginPx + pillHeight / 2f, (frameHeight - marginPx - totalGroupHeight + pillHeight / 2f).coerceAtLeast(marginPx + pillHeight / 2f))

        val candidateGroupRect = RectF(
            targetX - totalGroupWidth / 2f,
            targetY - pillHeight / 2f,
            targetX + totalGroupWidth / 2f,
            targetY + pillHeight / 2f + socialRowHeight
        )

        // Never allow signature/social icons to obscure Quranic text
        if (RectF.intersects(candidateGroupRect, quranTextBounds)) {
            val bottomSafeY = frameHeight - marginPx - totalGroupHeight + pillHeight / 2f
            targetY = if (bottomSafeY - pillHeight / 2f > quranTextBounds.bottom) {
                bottomSafeY
            } else {
                (marginPx + pillHeight / 2f).coerceAtLeast(pillHeight / 2f)
            }
        }

        val pillRect = RectF(
            targetX - pillWidth / 2f,
            targetY - pillHeight / 2f,
            targetX + pillWidth / 2f,
            targetY + pillHeight / 2f
        )

        badgePaint.color = Color.argb((alphaInt * 0.42f).toInt(), 29, 34, 39)
        canvas.drawRoundRect(pillRect, pillHeight / 2f, pillHeight / 2f, badgePaint)

        badgeBorderPaint.alpha = (alphaInt * 0.55f).toInt()
        canvas.drawRoundRect(pillRect, pillHeight / 2f, pillHeight / 2f, badgeBorderPaint)

        val diamondRadius = 4.5f * scaleFactor * watermarkConfig.scale
        canvas.drawCircle(pillRect.right - 20f * scaleFactor, targetY, diamondRadius, accentDotPaint)

        val fontMetrics = wmPaint.fontMetrics
        val baselineY = targetY - (fontMetrics.ascent + fontMetrics.descent) / 2f
        canvas.drawText(label, targetX - 4f * scaleFactor, baselineY, wmPaint)

        // Draw selected Social Media Vector Icons centered directly below the signature in #E8872F
        if (platformsOrdered.isNotEmpty()) {
            val iconsCenterY = pillRect.bottom + 12f * scaleFactor + iconSizePx / 2f +
                (watermarkConfig.socialIconsOffsetY * 40f * scaleFactor)
            val startIconX = targetX - socialRowWidth / 2f + iconSizePx / 2f

            socialIconFillPaint.color = Color.argb(alphaInt, 232, 135, 47) // #E8872F
            socialIconStrokePaint.color = Color.argb(alphaInt, 232, 135, 47) // #E8872F
            socialIconStrokePaint.strokeWidth = (iconSizePx * 0.095f).coerceAtLeast(2f)

            platformsOrdered.forEachIndexed { index, platform ->
                val cx = startIconX + index * (iconSizePx + iconSpacingPx)
                drawSocialPlatformVectorIcon(
                    canvas = canvas,
                    platform = platform,
                    cx = cx,
                    cy = iconsCenterY,
                    size = iconSizePx,
                    alphaInt = alphaInt,
                    scaleFactor = scaleFactor
                )
            }
        }

        return RectF(
            targetX - totalGroupWidth / 2f,
            pillRect.top,
            targetX + totalGroupWidth / 2f,
            pillRect.bottom + socialRowHeight
        )
    }

    /**
     * Draws clean, recognizable vector icons for Facebook, Instagram, TikTok, and YouTube
     * in the approved Tasneem Orange (#E8872F) without platform names or usernames.
     */
    private fun drawSocialPlatformVectorIcon(
        canvas: Canvas,
        platform: SocialPlatform,
        cx: Float,
        cy: Float,
        size: Float,
        alphaInt: Int,
        scaleFactor: Float
    ) {
        val r = size / 2f
        // Subtle dark circular backdrop so orange vector icon pops against any video background
        badgePaint.color = Color.argb((alphaInt * 0.48f).toInt(), 24, 28, 33)
        canvas.drawCircle(cx, cy, r * 1.12f, badgePaint)

        when (platform) {
            SocialPlatform.FACEBOOK -> {
                // Rounded square outline + stylized 'f'
                val box = RectF(cx - r * 0.78f, cy - r * 0.78f, cx + r * 0.78f, cy + r * 0.78f)
                canvas.drawRoundRect(box, r * 0.36f, r * 0.36f, socialIconStrokePaint)
                val fPath = Path().apply {
                    moveTo(cx + r * 0.22f, cy - r * 0.46f)
                    lineTo(cx + r * 0.05f, cy - r * 0.46f)
                    cubicTo(cx - r * 0.12f, cy - r * 0.46f, cx - r * 0.14f, cy - r * 0.30f, cx - r * 0.14f, cy - r * 0.12f)
                    lineTo(cx - r * 0.14f, cy + r * 0.52f)
                    moveTo(cx - r * 0.32f, cy - r * 0.04f)
                    lineTo(cx + r * 0.20f, cy - r * 0.04f)
                }
                canvas.drawPath(fPath, socialIconStrokePaint)
            }

            SocialPlatform.INSTAGRAM -> {
                // Camera rounded square, inner lens circle, and top-right flash dot
                val box = RectF(cx - r * 0.78f, cy - r * 0.78f, cx + r * 0.78f, cy + r * 0.78f)
                canvas.drawRoundRect(box, r * 0.42f, r * 0.42f, socialIconStrokePaint)
                canvas.drawCircle(cx, cy, r * 0.36f, socialIconStrokePaint)
                canvas.drawCircle(cx + r * 0.42f, cy - r * 0.42f, r * 0.095f, socialIconFillPaint)
            }

            SocialPlatform.TIKTOK -> {
                // Recognizable TikTok eighth-note vector icon
                val notePath = Path().apply {
                    moveTo(cx + r * 0.12f, cy - r * 0.56f)
                    lineTo(cx + r * 0.12f, cy + r * 0.22f)
                    // Top right wave flag
                    moveTo(cx + r * 0.12f, cy - r * 0.56f)
                    cubicTo(
                        cx + r * 0.18f, cy - r * 0.30f,
                        cx + r * 0.38f, cy - r * 0.18f,
                        cx + r * 0.54f, cy - r * 0.16f
                    )
                }
                canvas.drawPath(notePath, socialIconStrokePaint)
                // Note loop at bottom-left
                val loopRect = RectF(cx - r * 0.46f, cy - r * 0.04f, cx + r * 0.12f, cy + r * 0.54f)
                canvas.drawArc(loopRect, 35f, 295f, false, socialIconStrokePaint)
            }

            SocialPlatform.YOUTUBE -> {
                // Rounded play screen + central triangle
                val screen = RectF(cx - r * 0.85f, cy - r * 0.60f, cx + r * 0.85f, cy + r * 0.60f)
                canvas.drawRoundRect(screen, r * 0.32f, r * 0.32f, socialIconStrokePaint)
                val playTriangle = Path().apply {
                    moveTo(cx - r * 0.20f, cy - r * 0.28f)
                    lineTo(cx + r * 0.32f, cy)
                    lineTo(cx - r * 0.20f, cy + r * 0.28f)
                    close()
                }
                canvas.drawPath(playTriangle, socialIconFillPaint)
            }
        }
    }

    private fun applyAlpha(colorInt: Int, alphaMultiplier: Float): Int {
        val baseAlpha = Color.alpha(colorInt)
        val combinedAlpha = (baseAlpha * alphaMultiplier.coerceIn(0f, 1f)).toInt().coerceIn(0, 255)
        return Color.argb(
            combinedAlpha,
            Color.red(colorInt),
            Color.green(colorInt),
            Color.blue(colorInt)
        )
    }
}
