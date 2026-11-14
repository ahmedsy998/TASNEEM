package com.example.media

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.OpenableColumns
import com.example.model.MediaCropMode
import com.example.model.PhoneMediaBackground

class PhoneMediaImportManager(private val context: Context) {

    fun importMediaUri(
        uri: Uri,
        expectedVideo: Boolean,
        previousSettings: PhoneMediaBackground
    ): Result<PhoneMediaBackground> {
        return try {
            val contentResolver = context.contentResolver

            // Attempt persistent read permission when supported by provider
            try {
                contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (_: SecurityException) {
                // Photo Picker URIs grant session/temporary read access automatically
            }

            val mimeType = contentResolver.getType(uri)?.lowercase() ?: ""
            val fileName = resolveDisplayName(uri)
            val isVideo = expectedVideo || mimeType.startsWith("video/")

            // Persist a local copy in private app storage so Photo Picker / temporary URIs survive app & device restart
            val persistedUri = persistMediaToInternalStorage(uri, fileName, isVideo) ?: uri

            if (isVideo) {
                val retriever = MediaMetadataRetriever()
                val durationMs = try {
                    if (persistedUri.scheme == "file" && !persistedUri.path.isNullOrBlank()) {
                        retriever.setDataSource(java.io.File(persistedUri.path!!).absolutePath)
                    } else {
                        retriever.setDataSource(context, persistedUri)
                    }
                    val hasVideo = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_HAS_VIDEO)
                    val retrDuration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
                    if (hasVideo != null && retrDuration > 0L) {
                        retrDuration
                    } else {
                        val fallbackDur = inspectMp4FileDurationMs(persistedUri)
                        if (hasVideo == null && fallbackDur <= 0L) {
                            return Result.failure(Exception("الملف المختار لا يحتوي على مسار فيديو صالح أو أن الصيغة غير مدعومة."))
                        }
                        fallbackDur.coerceAtLeast(1000L)
                    }
                } catch (_: Exception) {
                    val fallbackDur = inspectMp4FileDurationMs(persistedUri)
                    if (fallbackDur <= 0L) {
                        return Result.failure(Exception("الملف المختار لا يحتوي على مسار فيديو صالح أو أن الصيغة غير مدعومة."))
                    }
                    fallbackDur
                } finally {
                    try {
                        retriever.release()
                    } catch (_: Exception) {
                    }
                }

                Result.success(
                    previousSettings.copy(
                        uriString = persistedUri.toString(),
                        isVideo = true,
                        fileName = fileName.ifBlank { "فيديو من الهاتف.mp4" },
                        mimeType = mimeType.ifBlank { "video/mp4" },
                        durationMs = durationMs,
                        cropMode = MediaCropMode.CENTER_CROP,
                        scale = 1.0f,
                        offsetX = 0f,
                        offsetY = 0f,
                        curatedItemId = null
                    )
                )
            } else {
                // Validate image decoding
                val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                contentResolver.openInputStream(persistedUri)?.use { stream ->
                    BitmapFactory.decodeStream(stream, null, options)
                }
                if (options.outWidth <= 0 || options.outHeight <= 0) {
                    return Result.failure(Exception("تعذر قراءة الصورة المختارة. يرجى اختيار صورة بصيغة JPG أو PNG أو WEBP."))
                }

                Result.success(
                    previousSettings.copy(
                        uriString = persistedUri.toString(),
                        isVideo = false,
                        fileName = fileName.ifBlank { "صورة من الهاتف" },
                        mimeType = mimeType.ifBlank { "image/jpeg" },
                        durationMs = 0L,
                        cropMode = MediaCropMode.CENTER_CROP,
                        scale = 1.0f,
                        offsetX = 0f,
                        offsetY = 0f,
                        curatedItemId = null
                    )
                )
            }
        } catch (e: Exception) {
            Result.failure(Exception("حدث خطأ أثناء استيراد الوسائط من الهاتف: ${e.localizedMessage ?: "ملف غير مدعوم"}"))
        }
    }

    private fun persistMediaToInternalStorage(uri: Uri, originalName: String, isVideo: Boolean): Uri? {
        return try {
            if (uri.scheme == "file") return uri
            val dir = java.io.File(context.filesDir, "persisted_backgrounds").apply { mkdirs() }
            val ext = originalName.substringAfterLast('.', "").lowercase().takeIf { it.length in 2..4 }
                ?: if (isVideo) "mp4" else "jpg"
            val targetFile = java.io.File(dir, if (isVideo) "active_background_video.$ext" else "active_background_image.$ext")
            context.contentResolver.openInputStream(uri)?.use { input ->
                targetFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            if (targetFile.exists() && targetFile.length() > 0L) {
                Uri.fromFile(targetFile)
            } else null
        } catch (_: Exception) {
            null
        }
    }

    fun decodeSampledBitmap(uri: Uri, reqWidth: Int, reqHeight: Int): Bitmap? {
        return try {
            val resolver = context.contentResolver
            val boundsOptions = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            resolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, boundsOptions)
            }
            var inSampleSize = 1
            if (boundsOptions.outHeight > reqHeight || boundsOptions.outWidth > reqWidth) {
                val halfHeight = boundsOptions.outHeight / 2
                val halfWidth = boundsOptions.outWidth / 2
                while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                    inSampleSize *= 2
                }
            }
            val decodeOptions = BitmapFactory.Options().apply {
                this.inSampleSize = inSampleSize
                this.inPreferredConfig = Bitmap.Config.ARGB_8888
            }
            resolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, decodeOptions)
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun inspectMp4FileDurationMs(uri: Uri): Long {
        return try {
            val bytes = if (uri.scheme == "file" && !uri.path.isNullOrBlank()) {
                java.io.File(uri.path!!).readBytes()
            } else {
                context.contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: return 0L
            }
            if (bytes.size < 32) return 0L
            // Reject static JPEG images immediately
            if (bytes[0] == 0xFF.toByte() && bytes[1] == 0xD8.toByte()) return 0L
            val rootType = String(bytes, 4, 4, Charsets.US_ASCII)
            if (rootType != "ftyp" && rootType != "moov" && rootType != "mdat") return 0L
            val ascii = String(bytes, Charsets.ISO_8859_1)
            val mvhdIdx = ascii.indexOf("mvhd")
            if (mvhdIdx >= 0 && mvhdIdx + 24 <= bytes.size) {
                val buf = java.nio.ByteBuffer.wrap(bytes, mvhdIdx + 4, 20).order(java.nio.ByteOrder.BIG_ENDIAN)
                val version = buf.get().toInt() and 0xFF
                buf.position(buf.position() + 3)
                if (version == 0) {
                    buf.int
                    buf.int
                    val timescale = buf.int.toLong().coerceAtLeast(1L)
                    val durUnits = buf.int.toLong() and 0xFFFFFFFFL
                    val durMs = (durUnits * 1000L) / timescale
                    if (durMs > 0L) return durMs
                }
            }
            6000L
        } catch (_: Exception) {
            0L
        }
    }

    private fun resolveDisplayName(uri: Uri): String {
        return try {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIdx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (nameIdx >= 0 && cursor.moveToFirst()) {
                    cursor.getString(nameIdx) ?: ""
                } else {
                    ""
                }
            } ?: ""
        } catch (_: Exception) {
            ""
        }
    }
}
