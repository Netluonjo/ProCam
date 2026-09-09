package com.sondeptrai.procam.core.camera

import android.content.ContentValues
import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CaptureResult
import android.hardware.camera2.DngCreator
import android.media.Image
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class RawCaptureManager(private val context: Context) {
    companion object {
        private const val TAG = "RawCaptureManager"
    }

    suspend fun saveDngImage(
        rawImage: Image,
        characteristics: CameraCharacteristics,
        captureResult: CaptureResult,
        orientation: Int
    ): Uri? = withContext(Dispatchers.IO) {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss_SSS", Locale.US).format(Date())
        val fileName = "PROCAM_RAW_$timeStamp.dng"

        try {
            val dngCreator = DngCreator(characteristics, captureResult).apply {
                setOrientation(orientation)
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val values = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, "image/x-adobe-dng")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/ProCam")
                }
                val uri = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                if (uri != null) {
                    context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                        dngCreator.writeImage(outputStream, rawImage)
                    }
                    Log.d(TAG, "RAW DNG saved successfully via MediaStore: $uri")
                    dngCreator.close()
                    return@withContext uri
                }
            } else {
                val directory = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "ProCam")
                if (!directory.exists()) directory.mkdirs()
                val file = File(directory, fileName)
                FileOutputStream(file).use { outputStream ->
                    dngCreator.writeImage(outputStream, rawImage)
                }
                Log.d(TAG, "RAW DNG saved to file: ${file.absolutePath}")
                dngCreator.close()
                return@withContext Uri.fromFile(file)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save RAW DNG image", e)
        } finally {
            rawImage.close()
        }
        return@withContext null
    }

    suspend fun saveJpegImage(
        jpegBytes: ByteArray
    ): Uri? = withContext(Dispatchers.IO) {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss_SSS", Locale.US).format(Date())
        val fileName = "PROCAM_$timeStamp.jpg"

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val values = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/ProCam")
                }
                val uri = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                if (uri != null) {
                    context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                        outputStream.write(jpegBytes)
                    }
                    Log.d(TAG, "JPEG saved successfully: $uri")
                    return@withContext uri
                }
            } else {
                val directory = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "ProCam")
                if (!directory.exists()) directory.mkdirs()
                val file = File(directory, fileName)
                FileOutputStream(file).use { outputStream ->
                    outputStream.write(jpegBytes)
                }
                Log.d(TAG, "JPEG saved to: ${file.absolutePath}")
                return@withContext Uri.fromFile(file)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save JPEG image", e)
        }
        return@withContext null
    }
}
