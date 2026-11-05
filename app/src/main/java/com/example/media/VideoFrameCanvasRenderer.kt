package com.example.media

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.LinearGradient
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.text.Layout
import android.text.StaticLayout
import android.text.TextDirectionHeuristics
import android.text.TextPaint
import com.example.data.QuranDataRepository
import com.example.model.MediaCropMode
import com.example.model.PhoneMediaBackground
import com.example.model.QuranTextAlignment
import com.example.model.QuranTextStyleConfig
import com.example.model.QuranVerse
import com.example.model.WatermarkConfig
import kotlin.math.max
import kotlin.math.min

class VideoFrameCanvasRenderer(private val context: Context) {

    private val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    private val dimPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val badgePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val badgeBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 2f
        color = Color.argb(140, 232, 135, 47) // #E8872F
    }

    fun renderFrame(
        canvas: Canvas,
        frameWidth: Int,
        frameHeight: Int,
        backgroundBitmap: Bitmap?,
        mediaConfig: PhoneMediaBackground,
        surahNameAr: String,
        activeVerse: QuranVerse,
        reciterNameAr: String,
        textConfig: QuranTextStyleConfig,
        watermarkConfig: WatermarkConfig
    ) {
        val w = frameWidth.toFloat()
        val h = frameHeight.toFloat()
        val scaleFactor = min(w, h) / 1080f

        // 1. Base background fill (#1D2227 Deep Charcoal)
        canvas.drawColor(Color.rgb(29, 34, 39))

        // 2. Draw user-selected phone background image or video frame with crop, scale, offset & brightness
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
        }

        // 3. Draw background dimming & subtle vignette for Quranic text legibility
        val dimAlpha = (mediaConfig.dimLevel.coerceIn(0f, 0.92f) * 255).toInt()
        dimPaint.shader = null
        dimPaint.color = Color.argb(dimAlpha, 18, 22, 26)
        canvas.drawRect(0f, 0f, w, h, dimPaint)

        // Subtle top/bottom vignette
        val vignetteGradient = LinearGradient(
            0f, 0f, 0f, h,
            intArrayOf(
                Color.argb(110, 15, 18, 22),
                Color.argb(20, 15, 18, 22),
                Color.argb(125, 15, 18, 22)
            ),
            floatArrayOf(0f, 0.5f, 1f),
            Shader.TileMode.CLAMP
        )
        dimPaint.shader = vignetteGradient
        canvas.drawRect(0f, 0f, w, h, dimPaint)
        dimPaint.shader = null

        // 4. Prepare Quranic verse typography
        val quranTypeface = textConfig.font.loadTypeface(context, textConfig.isBold)
        val uiTypeface = com.example.model.ArabicQuranFont.CAIRO.loadTypeface(context, bold = true)

        val textColorInt = (textConfig.colorOption.colorHex or 0xFF000000L).toInt()
        val mainTextSizePx = (textConfig.fontSizeSp * 2.6f * scaleFactor).coerceIn(28f, 140f)

        val verseTextPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = quranTypeface
            textSize = mainTextSizePx
            color = textColorInt
            isFakeBoldText = textConfig.isBold
            setShadowLayer(10f * scaleFactor, 0f, 4f * scaleFactor, Color.argb(200, 0, 0, 0))
        }

        val fullVerseString = "${activeVerse.uthmaniText} ${QuranDataRepository.formatAyahSymbol(activeVerse.verseNumber)}"
        val maxTextWidth = (w * 0.84f).toInt().coerceAtLeast(200)

        val layoutAlignment = when (textConfig.alignment) {
            QuranTextAlignment.CENTER -> Layout.Alignment.ALIGN_CENTER
            QuranTextAlignment.RIGHT -> Layout.Alignment.ALIGN_NORMAL
            QuranTextAlignment.JUSTIFY -> Layout.Alignment.ALIGN_CENTER
        }

        val verseLayout = StaticLayout.Builder
            .obtain(fullVerseString, 0, fullVerseString.length, verseTextPaint, maxTextWidth)
            .setAlignment(layoutAlignment)
            .setTextDirection(TextDirectionHeuristics.RTL)
            .setLineSpacing(0f, textConfig.lineSpacingMultiplier.coerceIn(1.1f, 2.4f))
            .setIncludePad(true)
            .build()

        // Optional Tafsir / Translation layouts
        val tafsirLayout = if (textConfig.showTafsir && activeVerse.tafsirMuyassar.isNotBlank()) {
            val tafsirPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                typeface = uiTypeface
                textSize = mainTextSizePx * 0.56f
                color = Color.rgb(245, 241, 234) // Soft Ivory #F5F1EA
                setShadowLayer(6f * scaleFactor, 0f, 2f * scaleFactor, Color.argb(190, 0, 0, 0))
            }
            StaticLayout.Builder
                .obtain(activeVerse.tafsirMuyassar, 0, activeVerse.tafsirMuyassar.length, tafsirPaint, maxTextWidth)
                .setAlignment(Layout.Alignment.ALIGN_CENTER)
                .setTextDirection(TextDirectionHeuristics.RTL)
                .setLineSpacing(0f, 1.3f)
                .build()
        } else null

        val translationLayout = if (textConfig.showTranslation && activeVerse.translationSaheeh.isNotBlank()) {
            val transPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                typeface = uiTypeface
                textSize = mainTextSizePx * textConfig.translationFontSizeScale.coerceIn(0.45f, 1.0f)
                color = Color.rgb(197, 200, 203) // Silver Gray #C5C8CB
                setShadowLayer(6f * scaleFactor, 0f, 2f * scaleFactor, Color.argb(190, 0, 0, 0))
            }
            StaticLayout.Builder
                .obtain(activeVerse.translationSaheeh, 0, activeVerse.translationSaheeh.length, transPaint, maxTextWidth)
                .setAlignment(Layout.Alignment.ALIGN_CENTER)
                .setLineSpacing(0f, 1.25f)
                .build()
        } else null

        val spacingPx = 24f * scaleFactor
        val headerHeight = if (textConfig.showSurahHeader) 80f * scaleFactor else 0f
        val reciterHeight = if (textConfig.showReciterName) 56f * scaleFactor else 0f

        var totalBlockHeight = headerHeight + verseLayout.height
        if (tafsirLayout != null) totalBlockHeight += spacingPx + tafsirLayout.height
        if (translationLayout != null) totalBlockHeight += spacingPx + translationLayout.height
        if (textConfig.showReciterName) totalBlockHeight += spacingPx + reciterHeight

        val centerX = (textConfig.normalizedX.coerceIn(0.15f, 0.85f)) * w
        val centerY = (textConfig.normalizedY.coerceIn(0.20f, 0.80f)) * h
        var currentTopY = (centerY - totalBlockHeight / 2f).coerceIn(h * 0.06f, h * 0.86f - totalBlockHeight)

        val textBoundsRect = RectF(
            centerX - maxTextWidth / 2f - 20f * scaleFactor,
            currentTopY - 16f * scaleFactor,
            centerX + maxTextWidth / 2f + 20f * scaleFactor,
            currentTopY + totalBlockHeight + 16f * scaleFactor
        )

        // 4a. Draw Surah Header if enabled
        if (textConfig.showSurahHeader) {
            val headerPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                typeface = quranTypeface
                textSize = 36f * scaleFactor
                color = Color.rgb(245, 241, 234) // #F5F1EA
                textAlign = Paint.Align.CENTER
                setShadowLayer(8f * scaleFactor, 0f, 2f * scaleFactor, Color.BLACK)
            }
            val subHeaderPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                typeface = uiTypeface
                textSize = 22f * scaleFactor
                color = Color.rgb(232, 135, 47) // #E8872F
                textAlign = Paint.Align.CENTER
            }
            canvas.drawText("سُورَةُ $surahNameAr", centerX, currentTopY + 34f * scaleFactor, headerPaint)
            canvas.drawText(
                "الآية ${QuranDataRepository.toArabicDigits(activeVerse.verseNumber)}",
                centerX,
                currentTopY + 66f * scaleFactor,
                subHeaderPaint
            )
            currentTopY += headerHeight + 12f * scaleFactor
        }

        // 4b. Draw Quranic Verse StaticLayout
        canvas.save()
        canvas.translate(centerX - maxTextWidth / 2f, currentTopY)
        verseLayout.draw(canvas)
        canvas.restore()
        currentTopY += verseLayout.height

        // 4c. Draw Tafsir if enabled
        if (tafsirLayout != null) {
            currentTopY += spacingPx
            canvas.save()
            canvas.translate(centerX - maxTextWidth / 2f, currentTopY)
            tafsirLayout.draw(canvas)
            canvas.restore()
            currentTopY += tafsirLayout.height
        }

        // 4d. Draw Translation if enabled
        if (translationLayout != null) {
            currentTopY += spacingPx
            canvas.save()
            canvas.translate(centerX - maxTextWidth / 2f, currentTopY)
            translationLayout.draw(canvas)
            canvas.restore()
            currentTopY += translationLayout.height
        }

        // 4e. Draw Reciter Name if enabled
        if (textConfig.showReciterName && reciterNameAr.isNotBlank()) {
            currentTopY += spacingPx
            val reciterPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                typeface = uiTypeface
                textSize = 26f * scaleFactor
                color = Color.rgb(245, 241, 234)
                textAlign = Paint.Align.CENTER
                setShadowLayer(6f * scaleFactor, 0f, 2f * scaleFactor, Color.BLACK)
            }
            canvas.drawText(reciterNameAr, centerX, currentTopY + 30f * scaleFactor, reciterPaint)
        }

        // 5. Draw Tasneem Watermark ("تسنيم") ensuring it NEVER obscures the Quranic text
        drawTasneemWatermark(
            canvas = canvas,
            frameWidth = w,
            frameHeight = h,
            scaleFactor = scaleFactor,
            watermarkConfig = watermarkConfig,
            quranTextBounds = textBoundsRect,
            uiTypeface = uiTypeface
        )
    }

    private fun drawTasneemWatermark(
        canvas: Canvas,
        frameWidth: Float,
        frameHeight: Float,
        scaleFactor: Float,
        watermarkConfig: WatermarkConfig,
        quranTextBounds: RectF,
        uiTypeface: android.graphics.Typeface
    ) {
        val marginPx = (watermarkConfig.marginDp * 2.2f * scaleFactor).coerceAtLeast(24f * scaleFactor)
        val wmTextSize = (watermarkConfig.fontSizeSp * 2.4f * watermarkConfig.scale * scaleFactor)
            .coerceIn(18f, 76f)
        val alphaInt = (watermarkConfig.opacity.coerceIn(0.15f, 1.0f) * 255).toInt()

        val wmPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = uiTypeface
            textSize = wmTextSize
            color = Color.argb(alphaInt, 245, 241, 234) // Soft Ivory #F5F1EA
            textAlign = Paint.Align.CENTER
            if (watermarkConfig.elegantGlow) {
                setShadowLayer(14f * scaleFactor, 0f, 0f, Color.argb((alphaInt * 0.85f).toInt(), 232, 135, 47))
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
        val pillHeight = wmTextSize * 1.65f

        var targetX = (watermarkConfig.normalizedX * frameWidth)
            .coerceIn(marginPx + pillWidth / 2f, frameWidth - marginPx - pillWidth / 2f)
        var targetY = (watermarkConfig.normalizedY * frameHeight)
            .coerceIn(marginPx + pillHeight / 2f, frameHeight - marginPx - pillHeight / 2f)

        val candidateRect = RectF(
            targetX - pillWidth / 2f,
            targetY - pillHeight / 2f,
            targetX + pillWidth / 2f,
            targetY + pillHeight / 2f
        )

        // Never allow watermark to obscure Quranic text: relocate to bottom or top safe margin if overlapping
        if (RectF.intersects(candidateRect, quranTextBounds)) {
            val bottomSafeY = frameHeight - marginPx - pillHeight / 2f
            if (bottomSafeY - pillHeight / 2f > quranTextBounds.bottom) {
                targetY = bottomSafeY
            } else {
                targetY = (marginPx + pillHeight / 2f).coerceAtLeast(pillHeight)
            }
        }

        val finalRect = RectF(
            targetX - pillWidth / 2f,
            targetY - pillHeight / 2f,
            targetX + pillWidth / 2f,
            targetY + pillHeight / 2f
        )

        badgePaint.color = Color.argb((alphaInt * 0.42f).toInt(), 29, 34, 39)
        canvas.drawRoundRect(finalRect, pillHeight / 2f, pillHeight / 2f, badgePaint)

        badgeBorderPaint.alpha = (alphaInt * 0.55f).toInt()
        canvas.drawRoundRect(finalRect, pillHeight / 2f, pillHeight / 2f, badgeBorderPaint)

        // Small geometric diamond ornament next to تسنيم
        val diamondRadius = 5f * scaleFactor * watermarkConfig.scale
        canvas.drawCircle(finalRect.right - 20f * scaleFactor, targetY, diamondRadius, accentDotPaint)

        val fontMetrics = wmPaint.fontMetrics
        val baselineY = targetY - (fontMetrics.ascent + fontMetrics.descent) / 2f
        canvas.drawText(label, targetX - 4f * scaleFactor, baselineY, wmPaint)
    }
}
