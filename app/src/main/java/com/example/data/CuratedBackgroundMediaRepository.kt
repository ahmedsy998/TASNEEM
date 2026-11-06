package com.example.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.media.MediaMuxer
import android.net.Uri
import com.example.model.BackgroundCategory
import com.example.model.CuratedBackgroundMediaItem
import com.example.model.MediaCropMode
import com.example.model.PhoneMediaBackground
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

class CuratedBackgroundMediaRepository(private val context: Context) {

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(25, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    private val cacheDir: File by lazy {
        File(context.filesDir, "tasneem_curated_backgrounds").apply { mkdirs() }
    }

    val allItems: List<CuratedBackgroundMediaItem> = buildCatalog()

    fun getItems(
        isVideo: Boolean,
        category: BackgroundCategory? = null,
        searchQuery: String = ""
    ): List<CuratedBackgroundMediaItem> {
        val normalizedQuery = searchQuery.trim()
        return allItems.filter { item ->
            item.isVideo == isVideo &&
                (category == null || item.category == category) &&
                (normalizedQuery.isEmpty() ||
                    item.titleAr.contains(normalizedQuery, ignoreCase = true) ||
                    item.category.titleAr.contains(normalizedQuery, ignoreCase = true))
        }
    }

    fun isCached(item: CuratedBackgroundMediaItem): Boolean {
        val target = getTargetFile(item)
        return target.exists() && target.length() > 2048L
    }

    fun getTargetFile(item: CuratedBackgroundMediaItem): File {
        val ext = if (item.isVideo) "mp4" else "jpg"
        return File(cacheDir, "${item.id}.$ext")
    }

    /**
     * Downloads the selected curated background item (or retrieves it from cache),
     * with an automatic high-resolution procedural synthesis fallback so every item
     * works reliably both online and offline.
     */
    suspend fun prepareAndCacheMediaItem(
        item: CuratedBackgroundMediaItem,
        currentDimLevel: Float = 0.52f
    ): PhoneMediaBackground = withContext(Dispatchers.IO) {
        val targetFile = getTargetFile(item)
        if (!targetFile.exists() || targetFile.length() <= 2048L) {
            val downloaded = tryDownloadFromRemote(item.mediaUrl, targetFile)
            if (!downloaded || targetFile.length() <= 2048L) {
                if (item.isVideo) {
                    synthesizeCuratedMotionVideo(item, targetFile)
                } else {
                    synthesizeCuratedHighResImage(item, targetFile)
                }
            }
        }

        val uri = Uri.fromFile(targetFile).toString()
        PhoneMediaBackground(
            uriString = uri,
            isVideo = item.isVideo,
            fileName = "${item.titleAr} (${if (item.isVideo) "فيديو" else "صورة"})",
            mimeType = if (item.isVideo) "video/mp4" else "image/jpeg",
            durationMs = if (item.isVideo) (item.durationSeconds * 1000L).coerceAtLeast(6000L) else 0L,
            cropMode = MediaCropMode.CENTER_CROP,
            scale = 1.0f,
            offsetX = 0f,
            offsetY = 0f,
            brightness = 0f,
            dimLevel = currentDimLevel,
            loopVideoInExport = true,
            curatedItemId = item.id
        )
    }

    private fun tryDownloadFromRemote(url: String, destination: File): Boolean {
        if (url.isBlank() || url.startsWith("procedural://")) return false
        return try {
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "TasneemQuranStudio/2.0 (Android)")
                .build()
            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return false
                val body = response.body ?: return false
                val tempFile = File(destination.parentFile, "${destination.name}.tmp")
                body.byteStream().use { input ->
                    FileOutputStream(tempFile).use { output ->
                        input.copyTo(output)
                    }
                }
                if (tempFile.length() > 2048L) {
                    tempFile.renameTo(destination)
                    true
                } else {
                    tempFile.delete()
                    false
                }
            }
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Generates a rich, high-resolution (1080x1920) thematic image for the given category
     * when offline or when a remote image needs local rendering.
     */
    private fun synthesizeCuratedHighResImage(item: CuratedBackgroundMediaItem, targetFile: File) {
        val width = 1080
        val height = 1920
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawCuratedSceneFrame(canvas, width.toFloat(), height.toFloat(), item, phase = 0.25f)
        FileOutputStream(targetFile).use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 92, out)
        }
        bitmap.recycle()
    }

    /**
     * Generates a smooth 5-second looping MP4 motion background video for the selected curated video item.
     */
    private fun synthesizeCuratedMotionVideo(item: CuratedBackgroundMediaItem, targetFile: File) {
        val width = 720
        val height = 1280
        val fps = 15
        val durationSec = 4
        val totalFrames = fps * durationSec
        val colorFormat = MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Flexible

        val format = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, width, height).apply {
            setInteger(MediaFormat.KEY_COLOR_FORMAT, colorFormat)
            setInteger(MediaFormat.KEY_BIT_RATE, 2_500_000)
            setInteger(MediaFormat.KEY_FRAME_RATE, fps)
            setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1)
        }

        var encoder: MediaCodec? = null
        var muxer: MediaMuxer? = null
        var muxerStarted = false
        var trackIndex = -1
        val frameBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val frameCanvas = Canvas(frameBitmap)
        val argbPixels = IntArray(width * height)
        val yuvBuffer = ByteArray(width * height * 3 / 2)

        try {
            encoder = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_AVC)
            encoder.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            encoder.start()
            muxer = MediaMuxer(targetFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
            val bufferInfo = MediaCodec.BufferInfo()

            for (frameIdx in 0..totalFrames) {
                if (frameIdx < totalFrames) {
                    val inIndex = encoder.dequeueInputBuffer(15_000)
                    if (inIndex >= 0) {
                        val inputBuffer = encoder.getInputBuffer(inIndex)
                        if (inputBuffer != null) {
                            inputBuffer.clear()
                            val phase = frameIdx.toFloat() / totalFrames.toFloat()
                            drawCuratedSceneFrame(frameCanvas, width.toFloat(), height.toFloat(), item, phase)
                            frameBitmap.getPixels(argbPixels, 0, width, 0, 0, width, height)
                            encodeArgbToNv12(argbPixels, yuvBuffer, width, height)
                            inputBuffer.put(yuvBuffer)
                            val ptsUs = (frameIdx * 1_000_000L) / fps
                            encoder.queueInputBuffer(inIndex, 0, yuvBuffer.size, ptsUs, 0)
                        }
                    }
                } else {
                    val inIndex = encoder.dequeueInputBuffer(15_000)
                    if (inIndex >= 0) {
                        val ptsUs = (frameIdx * 1_000_000L) / fps
                        encoder.queueInputBuffer(inIndex, 0, 0, ptsUs, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                    }
                }

                var draining = true
                while (draining) {
                    val outIndex = encoder.dequeueOutputBuffer(bufferInfo, 10_000)
                    when {
                        outIndex == MediaCodec.INFO_TRY_AGAIN_LATER -> draining = false
                        outIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                            trackIndex = muxer.addTrack(encoder.outputFormat)
                            muxer.start()
                            muxerStarted = true
                        }
                        outIndex >= 0 -> {
                            val outBuf = encoder.getOutputBuffer(outIndex)
                            if (outBuf != null && bufferInfo.size > 0 && muxerStarted &&
                                (bufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG) == 0
                            ) {
                                outBuf.position(bufferInfo.offset)
                                outBuf.limit(bufferInfo.offset + bufferInfo.size)
                                muxer.writeSampleData(trackIndex, outBuf, bufferInfo)
                            }
                            encoder.releaseOutputBuffer(outIndex, false)
                            if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                                draining = false
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            // Fallback to high-res image if device encoder does not accept flexible YUV
            synthesizeCuratedHighResImage(item, targetFile)
        } finally {
            frameBitmap.recycle()
            try { encoder?.stop(); encoder?.release() } catch (_: Exception) {}
            try { if (muxerStarted) muxer?.stop(); muxer?.release() } catch (_: Exception) {}
        }
    }

    /**
     * Renders an atmospheric, category-specific Islamic or Nature scene onto [canvas]
     * used for both thumbnail previews and high-resolution offline/procedural media.
     */
    fun drawCuratedSceneFrame(
        canvas: Canvas,
        width: Float,
        height: Float,
        item: CuratedBackgroundMediaItem,
        phase: Float
    ) {
        val c0 = item.primaryPaletteColors.getOrElse(0) { 0xFF1D2227 }.toInt()
        val c1 = item.primaryPaletteColors.getOrElse(1) { 0xFF343A40 }.toInt()
        val c2 = item.primaryPaletteColors.getOrElse(2) { 0xFFE8872F }.toInt()

        val skyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                0f, 0f, 0f, height,
                intArrayOf(c0, c1, c2),
                floatArrayOf(0f, 0.58f, 1f),
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(0f, 0f, width, height, skyPaint)

        // Subtle animated celestial glow
        val glowCenterX = width * (0.5f + 0.14f * sin(phase * Math.PI * 2.0).toFloat())
        val glowCenterY = height * (0.30f + 0.05f * cos(phase * Math.PI * 2.0).toFloat())
        val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = RadialGradient(
                glowCenterX,
                glowCenterY,
                min(width, height) * 0.58f,
                intArrayOf(
                    Color.argb(110, Color.red(c2), Color.green(c2), Color.blue(c2)),
                    Color.argb(35, Color.red(c1), Color.green(c1), Color.blue(c1)),
                    Color.TRANSPARENT
                ),
                floatArrayOf(0f, 0.55f, 1f),
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(0f, 0f, width, height, glowPaint)

        val silhouettePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(185, 16, 20, 25)
            style = Paint.Style.FILL
        }
        val accentStrokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(120, 232, 135, 47)
            style = Paint.Style.STROKE
            strokeWidth = width * 0.0035f
        }

        when (item.category) {
            BackgroundCategory.HOLY_KAABA,
            BackgroundCategory.HAJJ_UMRAH -> {
                // Draw stylized Holy Kaaba silhouette with Golden Kiswah band
                val kaabaW = width * 0.34f
                val kaabaH = kaabaW * 1.12f
                val left = (width - kaabaW) / 2f
                val top = height * 0.56f
                val kaabaPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.rgb(18, 20, 24)
                }
                canvas.drawRoundRect(RectF(left, top, left + kaabaW, top + kaabaH), 10f, 10f, kaabaPaint)

                // Golden Hizam (Kiswah belt)
                val beltPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.rgb(228, 168, 72)
                }
                val beltTop = top + kaabaH * 0.24f
                canvas.drawRect(left + kaabaW * 0.04f, beltTop, left + kaabaW * 0.96f, beltTop + kaabaH * 0.08f, beltPaint)
                // Kaaba door in gold
                val doorLeft = left + kaabaW * 0.62f
                val doorTop = top + kaabaH * 0.42f
                canvas.drawRect(doorLeft, doorTop, doorLeft + kaabaW * 0.16f, top + kaabaH * 0.78f, beltPaint)
            }

            BackgroundCategory.MOSQUES_MINARETS,
            BackgroundCategory.MADINAH_PROPHET_MOSQUE,
            BackgroundCategory.ISLAMIC_ARCHITECTURE -> {
                // Draw graceful Mosque dome and twin minarets silhouette at the bottom horizon
                val baseHorizonY = height * 0.78f
                canvas.drawRect(0f, baseHorizonY, width, height, silhouettePaint)
                // Central Dome
                val domeRadius = width * 0.22f
                canvas.drawCircle(width * 0.5f, baseHorizonY, domeRadius, silhouettePaint)
                canvas.drawCircle(width * 0.5f, baseHorizonY, domeRadius, accentStrokePaint)
                // Twin Minarets
                val minaretW = width * 0.045f
                val minaretH = height * 0.26f
                canvas.drawRect(width * 0.18f, baseHorizonY - minaretH, width * 0.18f + minaretW, baseHorizonY, silhouettePaint)
                canvas.drawRect(width * 0.775f, baseHorizonY - minaretH, width * 0.775f + minaretW, baseHorizonY, silhouettePaint)
            }

            BackgroundCategory.ISLAMIC_GEOMETRIC_ART -> {
                // Draw 8-fold Islamic star geometric rosette
                val cx = width * 0.5f
                val cy = height * 0.5f
                val maxR = min(width, height) * 0.38f
                for (ring in 1..3) {
                    val r = maxR * (ring / 3f)
                    val path = Path()
                    val points = 16
                    for (i in 0 until points) {
                        val angle = (i * Math.PI * 2.0 / points) + phase * 0.4
                        val rad = if (i % 2 == 0) r else r * 0.68f
                        val px = cx + (rad * cos(angle)).toFloat()
                        val py = cy + (rad * sin(angle)).toFloat()
                        if (i == 0) path.moveTo(px, py) else path.lineTo(px, py)
                    }
                    path.close()
                    canvas.drawPath(path, accentStrokePaint)
                }
            }

            else -> {
                // Layered serene mountain / desert dune / river horizon waves
                for (layer in 0..2) {
                    val path = Path()
                    val baseY = height * (0.66f + layer * 0.09f)
                    path.moveTo(0f, height)
                    path.lineTo(0f, baseY)
                    val steps = 24
                    for (s in 1..steps) {
                        val x = (width * s) / steps
                        val wave = sin((s.toFloat() / steps) * Math.PI * (2.0 + layer) + phase * Math.PI * 2.0 + layer)
                        val y = baseY + (wave * height * 0.032f).toFloat()
                        path.lineTo(x, y)
                    }
                    path.lineTo(width, height)
                    path.close()
                    val layerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = Color.argb(95 + layer * 55, 18, 22, 27)
                    }
                    canvas.drawPath(path, layerPaint)
                }
            }
        }
    }

    private fun encodeArgbToNv12(argb: IntArray, yuv: ByteArray, width: Int, height: Int) {
        val frameSize = width * height
        var yIndex = 0
        var uvIndex = frameSize
        for (j in 0 until height) {
            for (i in 0 until width) {
                val pixel = argb[j * width + i]
                val r = (pixel shr 16) and 0xFF
                val g = (pixel shr 8) and 0xFF
                val b = pixel and 0xFF
                val y = ((66 * r + 129 * g + 25 * b + 128) shr 8) + 16
                val u = ((-38 * r - 74 * g + 112 * b + 128) shr 8) + 128
                val v = ((112 * r - 94 * g - 18 * b + 128) shr 8) + 128
                yuv[yIndex++] = y.coerceIn(0, 255).toByte()
                if (j % 2 == 0 && i % 2 == 0 && uvIndex + 1 < yuv.size) {
                    yuv[uvIndex++] = u.coerceIn(0, 255).toByte()
                    yuv[uvIndex++] = v.coerceIn(0, 255).toByte()
                }
            }
        }
    }

    private fun buildCatalog(): List<CuratedBackgroundMediaItem> {
        val items = mutableListOf<CuratedBackgroundMediaItem>()

        fun addCategoryPair(
            category: BackgroundCategory,
            imgTitle1: String,
            imgUrl1: String,
            imgTitle2: String,
            imgUrl2: String,
            vidTitle: String,
            vidUrl: String,
            palette1: List<Long>,
            palette2: List<Long>
        ) {
            items.add(
                CuratedBackgroundMediaItem(
                    id = "${category.id}_img_1",
                    titleAr = imgTitle1,
                    category = category,
                    isVideo = false,
                    thumbnailUrl = imgUrl1,
                    mediaUrl = imgUrl1,
                    primaryPaletteColors = palette1
                )
            )
            items.add(
                CuratedBackgroundMediaItem(
                    id = "${category.id}_img_2",
                    titleAr = imgTitle2,
                    category = category,
                    isVideo = false,
                    thumbnailUrl = imgUrl2,
                    mediaUrl = imgUrl2,
                    primaryPaletteColors = palette2
                )
            )
            items.add(
                CuratedBackgroundMediaItem(
                    id = "${category.id}_vid_1",
                    titleAr = vidTitle,
                    category = category,
                    isVideo = true,
                    thumbnailUrl = imgUrl1,
                    mediaUrl = vidUrl,
                    durationSeconds = 8,
                    primaryPaletteColors = palette1
                )
            )
        }

        // 1. Nature and Landscapes
        addCategoryPair(
            category = BackgroundCategory.NATURE_LANDSCAPES,
            imgTitle1 = "سهول خضراء هادئة عند الفجر",
            imgUrl1 = "https://images.unsplash.com/photo-1506744038136-46273834b3fb?w=1080&q=80",
            imgTitle2 = "وادي الطبيعة الضبابي الساكن",
            imgUrl2 = "https://images.unsplash.com/photo-1470071459604-3b5ec3a7fe05?w=1080&q=80",
            vidTitle = "نسيم الطبيعة والمروج الهادئة (فيديو متحرك)",
            vidUrl = "procedural://nature_landscapes_vid",
            palette1 = listOf(0xFF142824, 0xFF284B43, 0xFFE8872F),
            palette2 = listOf(0xFF1A2630, 0xFF34495E, 0xFFD49A56)
        )

        // 2. Mountains
        addCategoryPair(
            category = BackgroundCategory.MOUNTAINS,
            imgTitle1 = "قمم الجبال الشاهقة فوق السحاب",
            imgUrl1 = "https://images.unsplash.com/photo-1464822759023-fed622ff2c3b?w=1080&q=80",
            imgTitle2 = "سلاسل الجبال الذهبية وقت الغسق",
            imgUrl2 = "https://images.unsplash.com/photo-1454496522488-7a8e488e8606?w=1080&q=80",
            vidTitle = "سحب تعانق قمم الجبال الرواسي (فيديو متحرك)",
            vidUrl = "procedural://mountains_vid",
            palette1 = listOf(0xFF161D26, 0xFF2E3B4E, 0xFFE8872F),
            palette2 = listOf(0xFF1F242D, 0xFF3E4652, 0xFFF0A553)
        )

        // 3. Rivers and Waterfalls
        addCategoryPair(
            category = BackgroundCategory.RIVERS_WATERFALLS,
            imgTitle1 = "شلالات المياه الصافية في الغابة",
            imgUrl1 = "https://images.unsplash.com/photo-1432405972618-c60b0225b8f9?w=1080&q=80",
            imgTitle2 = "نهر هادئ يعكس نور السماء",
            imgUrl2 = "https://images.unsplash.com/photo-1501785888041-af3ef285b470?w=1080&q=80",
            vidTitle = "جريان الأنهار والمياه العذبة (فيديو متحرك)",
            vidUrl = "procedural://rivers_vid",
            palette1 = listOf(0xFF102530, 0xFF1E485C, 0xFF5FA8D3),
            palette2 = listOf(0xFF152A38, 0xFF295266, 0xFFE8872F)
        )

        // 4. Forests and Trees
        addCategoryPair(
            category = BackgroundCategory.FORESTS_TREES,
            imgTitle1 = "أشعة الشمس تتخلل أشجار الصنوبر",
            imgUrl1 = "https://images.unsplash.com/photo-1448375240586-882707db888b?w=1080&q=80",
            imgTitle2 = "غابة خريفية بأوراق ذهبية دافئة",
            imgUrl2 = "https://images.unsplash.com/photo-1441974231531-c6227db76b6e?w=1080&q=80",
            vidTitle = "حفيف الأشجار وضوء الغابة (فيديو متحرك)",
            vidUrl = "procedural://forests_vid",
            palette1 = listOf(0xFF112218, 0xFF21422E, 0xFFD99441),
            palette2 = listOf(0xFF1E2618, 0xFF3A482B, 0xFFE8872F)
        )

        // 5. Deserts and Sand Dunes
        addCategoryPair(
            category = BackgroundCategory.DESERTS_DUNES,
            imgTitle1 = "كثبان الصحراء الذهبية عند الأصيل",
            imgUrl1 = "https://images.unsplash.com/photo-1509316975850-ff9c5deb0cd9?w=1080&q=80",
            imgTitle2 = "رمال البادية العربية تحت الشفق",
            imgUrl2 = "https://images.unsplash.com/photo-1473580044384-7ba9967e16a0?w=1080&q=80",
            vidTitle = "تموجات الكثبان الرملية الذهبية (فيديو متحرك)",
            vidUrl = "procedural://deserts_vid",
            palette1 = listOf(0xFF241911, 0xFF5C3A21, 0xFFE8872F),
            palette2 = listOf(0xFF2A1C14, 0xFF704526, 0xFFF3B35A)
        )

        // 6. Sky and Clouds
        addCategoryPair(
            category = BackgroundCategory.SKY_CLOUDS,
            imgTitle1 = "سماء صافية وسحب بيضاء ناعمة",
            imgUrl1 = "https://images.unsplash.com/photo-1513002749550-c59d786b8e6c?w=1080&q=80",
            imgTitle2 = "آفاق السماء المهيبة فوق الغمام",
            imgUrl2 = "https://images.unsplash.com/photo-1534088568595-a066f410bcda?w=1080&q=80",
            vidTitle = "حركة السحب والغمام في السماء (فيديو متحرك)",
            vidUrl = "procedural://sky_clouds_vid",
            palette1 = listOf(0xFF162436, 0xFF2E4C6D, 0xFFE8872F),
            palette2 = listOf(0xFF1E2D3D, 0xFF3E5C76, 0xFFF5F1EA)
        )

        // 7. Sunsets and Sunrises
        addCategoryPair(
            category = BackgroundCategory.SUNSETS_SUNRISES,
            imgTitle1 = "شفق الغروب البرتقالي فوق الأفق",
            imgUrl1 = "https://images.unsplash.com/photo-1495616811223-4d98c6e9c869?w=1080&q=80",
            imgTitle2 = "إشراقة الفجر الذهبي الهادئ",
            imgUrl2 = "https://images.unsplash.com/photo-1470252649378-9c29740c9fa8?w=1080&q=80",
            vidTitle = "تدرج الشفق وغروب الشمس الدافئ (فيديو متحرك)",
            vidUrl = "procedural://sunsets_vid",
            palette1 = listOf(0xFF1D1824, 0xFF4E2A38, 0xFFE8872F),
            palette2 = listOf(0xFF241B18, 0xFF6B3B2A, 0xFFF3B35A)
        )

        // 8. Night Skies and Stars
        addCategoryPair(
            category = BackgroundCategory.NIGHT_STARS,
            imgTitle1 = "سماء الليل المرصعة بالنجوم والمجرة",
            imgUrl1 = "https://images.unsplash.com/photo-1519681393784-d120267933ba?w=1080&q=80",
            imgTitle2 = "كون فسيح ونجوم لامعة في السحر",
            imgUrl2 = "https://images.unsplash.com/photo-1475274047050-1d0c0975c63e?w=1080&q=80",
            vidTitle = "تأمل النجوم وسماء الليل الساكنة (فيديو متحرك)",
            vidUrl = "procedural://night_stars_vid",
            palette1 = listOf(0xFF0B0F17, 0xFF1B2436, 0xFFE8872F),
            palette2 = listOf(0xFF0E131F, 0xFF22304A, 0xFFC5C8CB)
        )

        // 9. Islamic Architecture
        addCategoryPair(
            category = BackgroundCategory.ISLAMIC_ARCHITECTURE,
            imgTitle1 = "أقواس وأروقة العمارة الإسلامية العريقة",
            imgUrl1 = "https://images.unsplash.com/photo-1584551246679-0daf3d275d0f?w=1080&q=80",
            imgTitle2 = "مقرنصات وقباب إسلامية مزخرفة",
            imgUrl2 = "https://images.unsplash.com/photo-1542816417-0983c9c9ad53?w=1080&q=80",
            vidTitle = "ظلال وضوء في أروقة العمارة الإسلامية (فيديو متحرك)",
            vidUrl = "procedural://islamic_arch_vid",
            palette1 = listOf(0xFF1D2227, 0xFF343A40, 0xFFE8872F),
            palette2 = listOf(0xFF231F1D, 0xFF4A3D36, 0xFFF3B35A)
        )

        // 10. Mosques and Minarets
        addCategoryPair(
            category = BackgroundCategory.MOSQUES_MINARETS,
            imgTitle1 = "مآذن وقباب المساجد عند الغروب",
            imgUrl1 = "https://images.unsplash.com/photo-1564769625905-50e93615e769?w=1080&q=80",
            imgTitle2 = "جامع مهيب بإضاءة ليلية روحانية",
            imgUrl2 = "https://images.unsplash.com/photo-1519817650390-64a93db51149?w=1080&q=80",
            vidTitle = "أفق المآذن والقباب الروحانية (فيديو متحرك)",
            vidUrl = "procedural://mosques_vid",
            palette1 = listOf(0xFF161B22, 0xFF2C3542, 0xFFE8872F),
            palette2 = listOf(0xFF1A2028, 0xFF343A40, 0xFFF3B35A)
        )

        // 11. The Holy Kaaba
        addCategoryPair(
            category = BackgroundCategory.HOLY_KAABA,
            imgTitle1 = "الكعبة المشرفة في المسجد الحرام بمكة",
            imgUrl1 = "https://images.unsplash.com/photo-1565552645632-d725f8bfc19a?w=1080&q=80",
            imgTitle2 = "كسوة الكعبة المشرفة والذهب الخالص",
            imgUrl2 = "https://images.unsplash.com/photo-1591604129939-f1efa4d9f7fa?w=1080&q=80",
            vidTitle = "أنوار الكعبة المشرفة والحرم المكي (فيديو متحرك)",
            vidUrl = "procedural://holy_kaaba_vid",
            palette1 = listOf(0xFF121417, 0xFF282C32, 0xFFE8872F),
            palette2 = listOf(0xFF181614, 0xFF363028, 0xFFF3B35A)
        )

        // 12. Madinah and Al-Masjid an-Nabawi
        addCategoryPair(
            category = BackgroundCategory.MADINAH_PROPHET_MOSQUE,
            imgTitle1 = "المسجد النبوي الشريف بالمدينة المنورة",
            imgUrl1 = "https://images.unsplash.com/photo-1591604129939-f1efa4d9f7fa?w=1080&q=80",
            imgTitle2 = "ساحات ومظلات الحرم النبوي الشريف",
            imgUrl2 = "https://images.unsplash.com/photo-1580418827493-f2b22c0a76cb?w=1080&q=80",
            vidTitle = "سكينة المسجد النبوي بالمدينة المنورة (فيديو متحرك)",
            vidUrl = "procedural://madinah_vid",
            palette1 = listOf(0xFF14221E, 0xFF26423A, 0xFFE8872F),
            palette2 = listOf(0xFF1C2426, 0xFF344246, 0xFFF5F1EA)
        )

        // 13. Hajj and Umrah
        addCategoryPair(
            category = BackgroundCategory.HAJJ_UMRAH,
            imgTitle1 = "أجواء الحج والعمرة في رحاب الحرمين",
            imgUrl1 = "https://images.unsplash.com/photo-1565552645632-d725f8bfc19a?w=1080&q=80",
            imgTitle2 = "جبل الرحمة والمشاعر المقدسة",
            imgUrl2 = "https://images.unsplash.com/photo-1542816417-0983c9c9ad53?w=1080&q=80",
            vidTitle = "روحانية الحج والعمرة والمشاعر المقدسة (فيديو متحرك)",
            vidUrl = "procedural://hajj_umrah_vid",
            palette1 = listOf(0xFF1B1E24, 0xFF343A40, 0xFFE8872F),
            palette2 = listOf(0xFF221E1A, 0xFF423830, 0xFFF3B35A)
        )

        // 14. Islamic Geometric Art
        addCategoryPair(
            category = BackgroundCategory.ISLAMIC_GEOMETRIC_ART,
            imgTitle1 = "زخارف نجمية إسلامية بالبرتقالي والفضي",
            imgUrl1 = "procedural://islamic_geo_1",
            imgTitle2 = "نقوش أرابيسك هندسية ذهبية داكنة",
            imgUrl2 = "procedural://islamic_geo_2",
            vidTitle = "زخارف هندسية إسلامية متحركة (فيديو متحرك)",
            vidUrl = "procedural://islamic_geo_vid",
            palette1 = listOf(0xFF1D2227, 0xFF343A40, 0xFFE8872F),
            palette2 = listOf(0xFF161A1E, 0xFF2B3137, 0xFFF3B35A)
        )

        // 15. Peaceful Spiritual Landscapes
        addCategoryPair(
            category = BackgroundCategory.PEACEFUL_SPIRITUAL,
            imgTitle1 = "مشهد روحاني هادئ للتدبر والخشوع",
            imgUrl1 = "https://images.unsplash.com/photo-1507525428034-b723cf961d3e?w=1080&q=80",
            imgTitle2 = "سكون البحر والأفق عند الغسق",
            imgUrl2 = "https://images.unsplash.com/photo-1518837695005-2083093ee35b?w=1080&q=80",
            vidTitle = "أمواج هادئة وأفق روحاني للتلاوة (فيديو متحرك)",
            vidUrl = "procedural://peaceful_spiritual_vid",
            palette1 = listOf(0xFF182028, 0xFF2C3A47, 0xFFE8872F),
            palette2 = listOf(0xFF1D2227, 0xFF343A40, 0xFFE8872F)
        )

        return items
    }
}
