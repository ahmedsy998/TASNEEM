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
            if (isVideo) {
                val retriever = MediaMetadataRetriever()
                val durationMs = try {
                    retriever.setDataSource(context, uri)
                    val hasVideo = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_HAS_VIDEO)
                    if (hasVideo == null) {
                        return Result.failure(Exception("الملف المختار لا يحتوي على مسار فيديو صالح أو أن الصيغة غير مدعومة."))
                    }
                    retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
                } finally {
                    try {
                        retriever.release()
                    } catch (_: Exception) {
                    }
                }

                Result.success(
                    previousSettings.copy(
                        uriString = uri.toString(),
                        isVideo = true,
                        fileName = fileName.ifBlank { "فيديو من الهاتف.mp4" },
                        mimeType = mimeType.ifBlank { "video/mp4" },
                        durationMs = durationMs,
                        cropMode = MediaCropMode.CENTER_CROP,
                        scale = 1.0f,
                        offsetX = 0f,
                        offsetY = 0f
                    )
                )
            } else {
                // Validate image decoding
                val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                contentResolver.openInputStream(uri)?.use { stream ->
                    BitmapFactory.decodeStream(stream, null, options)
                }
                if (options.outWidth <= 0 || options.outHeight <= 0) {
                    return Result.failure(Exception("تعذر قراءة الصورة المختارة. يرجى اختيار صورة بصيغة JPG أو PNG أو WEBP."))
                }

                Result.success(
                    previousSettings.copy(
                        uriString = uri.toString(),
                        isVideo = false,
                        fileName = fileName.ifBlank { "صورة من الهاتف" },
                        mimeType = mimeType.ifBlank { "image/jpeg" },
                        durationMs = 0L,
                        cropMode = MediaCropMode.CENTER_CROP,
                        scale = 1.0f,
                        offsetX = 0f,
                        offsetY = 0f
                    )
                )
            }
        } catch (e: Exception) {
            Result.failure(Exception("حدث خطأ أثناء استيراد الوسائط من الهاتف: ${e.localizedMessage ?: "ملف غير مدعوم"}"))
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
