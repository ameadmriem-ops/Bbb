package com.example.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Base64
import com.example.model.AttachmentType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.InputStream

object FileAnalyzerHelper {

    suspend fun getFileName(context: Context, uri: Uri): String = withContext(Dispatchers.IO) {
        var name = "file"
        try {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (nameIndex != -1 && cursor.moveToFirst()) {
                    name = cursor.getString(nameIndex)
                }
            }
        } catch (_: Exception) {}
        name
    }

    fun detectAttachmentType(fileName: String, mimeType: String?): AttachmentType {
        val lower = fileName.lowercase()
        val mime = mimeType?.lowercase() ?: ""
        return when {
            mime.startsWith("image/") || lower.endsWith(".png") || lower.endsWith(".jpg") || lower.endsWith(".jpeg") || lower.endsWith(".webp") ->
                AttachmentType.IMAGE
            mime.contains("pdf") || lower.endsWith(".pdf") ->
                AttachmentType.PDF
            lower.endsWith(".kt") || lower.endsWith(".java") || lower.endsWith(".py") || lower.endsWith(".js") || lower.endsWith(".ts") ||
                    lower.endsWith(".html") || lower.endsWith(".css") || lower.endsWith(".json") || lower.endsWith(".xml") || lower.endsWith(".sql") ->
                AttachmentType.CODE_FILE
            else ->
                AttachmentType.TEXT_FILE
        }
    }

    suspend fun readTextContent(context: Context, uri: Uri): String? = withContext(Dispatchers.IO) {
        try {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                stream.bufferedReader(Charsets.UTF_8).use { reader ->
                    // Limit text reading to 100k characters for safe token limit
                    val buffer = CharArray(100000)
                    val read = reader.read(buffer)
                    if (read > 0) String(buffer, 0, read) else null
                }
            }
        } catch (_: Exception) {
            null
        }
    }

    suspend fun convertImageToBase64(context: Context, uri: Uri): Pair<String, String>? = withContext(Dispatchers.IO) {
        try {
            val mimeType = context.contentResolver.getType(uri) ?: "image/jpeg"
            val input: InputStream? = context.contentResolver.openInputStream(uri)
            val bitmap = BitmapFactory.decodeStream(input) ?: return@withContext null
            
            // Downscale to max 1280px for performance & token efficiency
            val maxDim = 1280
            val scaledBitmap = if (bitmap.width > maxDim || bitmap.height > maxDim) {
                val ratio = bitmap.width.toFloat() / bitmap.height.toFloat()
                val targetW = if (ratio > 1) maxDim else (maxDim * ratio).toInt()
                val targetH = if (ratio > 1) (maxDim / ratio).toInt() else maxDim
                Bitmap.createScaledBitmap(bitmap, targetW, targetH, true)
            } else {
                bitmap
            }

            val outputStream = ByteArrayOutputStream()
            scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 85, outputStream)
            val base64 = Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP)
            Pair(base64, "image/jpeg")
        } catch (_: Exception) {
            null
        }
    }
}
