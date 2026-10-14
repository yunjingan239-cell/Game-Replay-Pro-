package com.example.util

import android.content.ContentValues
import android.content.Context
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

/**
 * Utility for exporting recorded game highlight videos directly to the system gallery / photo album.
 */
object MediaExportHelper {

    private const val TAG = "MediaExportHelper"

    /**
     * Saves an MP4 video file to the system gallery under "Movies/GameReplay".
     * Works on Android 10+ using Scoped Storage (no write permission required)
     * and triggers system media scanner so it immediately appears in the Photos/Gallery app.
     */
    fun saveVideoToGallery(
        context: Context,
        videoFile: File,
        title: String? = null
    ): Result<Uri> {
        if (!videoFile.exists() || videoFile.length() == 0L) {
            return Result.failure(IllegalStateException("视频文件不存在或为空"))
        }

        val clipTitle = title ?: videoFile.nameWithoutExtension
        val fileName = if (clipTitle.endsWith(".mp4", ignoreCase = true)) clipTitle else "$clipTitle.mp4"

        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val contentValues = ContentValues().apply {
                    put(MediaStore.Video.Media.DISPLAY_NAME, fileName)
                    put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
                    put(MediaStore.Video.Media.TITLE, clipTitle)
                    put(MediaStore.Video.Media.RELATIVE_PATH, "Movies/GameReplay")
                    put(MediaStore.Video.Media.DATE_ADDED, System.currentTimeMillis() / 1000)
                    put(MediaStore.Video.Media.DATE_MODIFIED, System.currentTimeMillis() / 1000)
                    put(MediaStore.Video.Media.IS_PENDING, 1)
                }

                val resolver = context.contentResolver
                val uri = resolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, contentValues)
                    ?: return Result.failure(IllegalStateException("无法创建媒体相册条目"))

                resolver.openOutputStream(uri)?.use { outputStream ->
                    FileInputStream(videoFile).use { inputStream ->
                        inputStream.copyTo(outputStream)
                    }
                }

                contentValues.clear()
                contentValues.put(MediaStore.Video.Media.IS_PENDING, 0)
                resolver.update(uri, contentValues, null, null)

                Log.i(TAG, "Video successfully saved to system gallery via MediaStore: $uri")
                Result.success(uri)
            } else {
                // Legacy devices (API < 29)
                val moviesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES)
                val targetDir = File(moviesDir, "GameReplay").apply { mkdirs() }
                val targetFile = File(targetDir, fileName)

                FileInputStream(videoFile).use { input ->
                    FileOutputStream(targetFile).use { output ->
                        input.copyTo(output)
                    }
                }

                var resultUri: Uri? = null
                MediaScannerConnection.scanFile(
                    context,
                    arrayOf(targetFile.absolutePath),
                    arrayOf("video/mp4")
                ) { _, uri ->
                    resultUri = uri
                }

                Result.success(resultUri ?: Uri.fromFile(targetFile))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save video to gallery", e)
            Result.failure(e)
        }
    }
}
