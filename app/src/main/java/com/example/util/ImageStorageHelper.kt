package com.example.util

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream

object ImageStorageHelper {

  fun createTempImageUri(context: Context, folder: String = "camera_temp"): Uri {
    val dir = File(context.cacheDir, folder).apply { if (!exists()) mkdirs() }
    val file = File(dir, "temp_${System.currentTimeMillis()}.jpg")
    return FileProvider.getUriForFile(
      context,
      "${context.packageName}.fileprovider",
      file
    )
  }

  fun saveImageFromUri(context: Context, uri: Uri, folder: String = "attachments"): String? {
    return try {
      val dir = File(context.filesDir, folder).apply { if (!exists()) mkdirs() }
      val file = File(dir, "img_${System.currentTimeMillis()}.jpg")
      context.contentResolver.openInputStream(uri)?.use { input ->
        FileOutputStream(file).use { output ->
          input.copyTo(output)
        }
      }
      file.absolutePath
    } catch (e: Exception) {
      e.printStackTrace()
      null
    }
  }

  fun saveBitmap(context: Context, bitmap: Bitmap, folder: String = "attachments"): String? {
    return try {
      val dir = File(context.filesDir, folder).apply { if (!exists()) mkdirs() }
      val file = File(dir, "img_${System.currentTimeMillis()}.jpg")
      FileOutputStream(file).use { out ->
        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
      }
      file.absolutePath
    } catch (e: Exception) {
      e.printStackTrace()
      null
    }
  }
}
