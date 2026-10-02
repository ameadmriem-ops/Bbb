package com.example.data.repository

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.core.content.FileProvider
import com.example.data.local.GeneratedImageDao
import com.example.data.local.GeneratedImageEntity
import com.example.data.remote.GeminiClient
import com.example.model.GeneratedImageItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import kotlin.random.Random

class ImageRepository(
    private val context: Context,
    private val imageDao: GeneratedImageDao,
    private val usageQuotaManager: UsageQuotaManager,
    private val adminRepository: AdminRepository
) {

    fun getAllImages(): Flow<List<GeneratedImageItem>> {
        return imageDao.getAllImages().map { list ->
            list.map { it.toDomain() }
        }
    }

    suspend fun generateImage(
        prompt: String,
        aspectRatio: String = "1:1"
    ): Result<GeneratedImageItem> = withContext(Dispatchers.IO) {
        val freeLimit = adminRepository.stats.value.freeDailyImageLimit
        if (!usageQuotaManager.canGenerateImage(freeLimit)) {
            val limitMsg = "وصلت للحد الأقصى لتوليد الصور في الباقة المجانية ($freeLimit صور/يوم). يرجى الترقية إلى Premium للحصول على توليد صور غير محدود بجودة فائقة!"
            return@withContext Result.failure(Exception(limitMsg))
        }

        if (prompt.isBlank()) {
            return@withContext Result.failure(Exception("يرجى إدخال وصف للصورة"))
        }

        val (width, height) = when (aspectRatio) {
            "16:9" -> Pair(1280, 720)
            "9:16" -> Pair(720, 1280)
            "4:3" -> Pair(1024, 768)
            else -> Pair(1024, 1024)
        }

        val encodedPrompt = URLEncoder.encode(prompt.trim(), StandardCharsets.UTF_8.toString())
        val seed = Random.nextInt(1, 999999)
        // High quality fast Flux/SD model endpoint
        val imageUrl = "https://image.pollinations.ai/prompt/$encodedPrompt?width=$width&height=$height&model=flux&seed=$seed&nologo=true"

        val startTime = System.currentTimeMillis()
        try {
            // Pre-download to verify and cache locally
            val request = Request.Builder().url(imageUrl).build()
            val response = GeminiClient.rawOkHttpClient.newCall(request).execute()
            val latency = System.currentTimeMillis() - startTime

            if (response.isSuccessful && response.body != null) {
                val bytes = response.body!!.bytes()
                val localFile = saveImageFile(bytes, "img_${System.currentTimeMillis()}.png")

                val domainItem = GeneratedImageItem(
                    prompt = prompt,
                    imageUrl = imageUrl,
                    localUri = localFile.absolutePath,
                    aspectRatio = aspectRatio
                )

                val id = imageDao.insertImage(GeneratedImageEntity.fromDomain(domainItem))
                usageQuotaManager.recordImageGenerated()
                adminRepository.updateMetrics(imagesDelta = 1)
                adminRepository.logApiCall("pollinations-ai/flux", 200, latency, false)

                Result.success(domainItem.copy(id = id))
            } else {
                adminRepository.logApiCall("pollinations-ai/flux", response.code, latency, true)
                Result.failure(Exception("تعذر إنشاء الصورة من الخادم (كود ${response.code})"))
            }
        } catch (e: Exception) {
            val latency = System.currentTimeMillis() - startTime
            adminRepository.logApiCall("pollinations-ai/flux", 500, latency, true)
            Result.failure(Exception("فشل توليد الصورة: ${e.localizedMessage ?: "تحقق من اتصال الإنترنت"}"))
        }
    }

    suspend fun deleteImage(id: Long) = withContext(Dispatchers.IO) {
        imageDao.deleteImageById(id)
    }

    suspend fun clearAll() = withContext(Dispatchers.IO) {
        imageDao.clearAllImages()
    }

    fun shareImage(item: GeneratedImageItem) {
        try {
            val file = if (item.localUri != null) File(item.localUri) else null
            if (file != null && file.exists()) {
                val uri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.provider",
                    file
                )
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "image/png"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    putExtra(Intent.EXTRA_TEXT, "تم إنشاء هذه الصورة بواسطة AI Smart: ${item.prompt}")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(Intent.createChooser(shareIntent, "مشاركة الصورة").apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                })
            }
        } catch (_: Exception) {}
    }

    private fun saveImageFile(bytes: ByteArray, filename: String): File {
        val dir = File(context.filesDir, "generated_images")
        if (!dir.exists()) dir.mkdirs()
        val file = File(dir, filename)
        FileOutputStream(file).use { it.write(bytes) }
        return file
    }
}
