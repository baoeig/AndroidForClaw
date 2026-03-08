package com.xiaomo.feishu.messaging

import android.util.Log
import com.google.gson.JsonObject
import com.xiaomo.feishu.FeishuClient
import com.xiaomo.feishu.FeishuConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File

/**
 * 飞书媒体处理
 * 对齐 OpenClaw src/media.ts
 */
class FeishuMedia(
    private val config: FeishuConfig,
    private val client: FeishuClient
) {
    companion object {
        private const val TAG = "FeishuMedia"
    }

    private val httpClient = OkHttpClient()

    /**
     * 上传图片
     */
    suspend fun uploadImage(imageFile: File): Result<MediaUploadResult> = withContext(Dispatchers.IO) {
        try {
            // 检查文件大小
            val fileSizeMb = imageFile.length() / (1024.0 * 1024.0)
            if (fileSizeMb > config.mediaMaxMb) {
                return@withContext Result.failure(Exception("Image too large: ${fileSizeMb}MB > ${config.mediaMaxMb}MB"))
            }

            uploadMediaFile(imageFile, "image")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to upload image", e)
            Result.failure(e)
        }
    }

    /**
     * 上传文件
     */
    suspend fun uploadFile(file: File): Result<MediaUploadResult> = withContext(Dispatchers.IO) {
        try {
            // 检查文件大小
            val fileSizeMb = file.length() / (1024.0 * 1024.0)
            if (fileSizeMb > config.mediaMaxMb) {
                return@withContext Result.failure(Exception("File too large: ${fileSizeMb}MB > ${config.mediaMaxMb}MB"))
            }

            uploadMediaFile(file, "file")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to upload file", e)
            Result.failure(e)
        }
    }

    /**
     * 下载图片
     */
    suspend fun downloadImage(imageKey: String, outputFile: File): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            downloadMediaFile(imageKey, "image", outputFile)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to download image", e)
            Result.failure(e)
        }
    }

    /**
     * 下载文件
     */
    suspend fun downloadFile(fileKey: String, outputFile: File): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            downloadMediaFile(fileKey, "file", outputFile)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to download file", e)
            Result.failure(e)
        }
    }

    /**
     * 发送图片消息
     */
    suspend fun sendImage(
        receiveId: String,
        imageKey: String,
        receiveIdType: String = "open_id"
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val content = """{"image_key":"$imageKey"}"""
            val body = mapOf(
                "receive_id" to receiveId,
                "msg_type" to "image",
                "content" to content
            )

            val result = client.post(
                "/open-apis/im/v1/messages?receive_id_type=$receiveIdType",
                body
            )

            if (result.isFailure) {
                return@withContext Result.failure(result.exceptionOrNull()!!)
            }

            val data = result.getOrNull()?.getAsJsonObject("data")
            val messageId = data?.get("message_id")?.asString
                ?: return@withContext Result.failure(Exception("Missing message_id"))

            Log.d(TAG, "Image message sent: $messageId")
            Result.success(messageId)

        } catch (e: Exception) {
            Log.e(TAG, "Failed to send image message", e)
            Result.failure(e)
        }
    }

    /**
     * 发送文件消息
     */
    suspend fun sendFile(
        receiveId: String,
        fileKey: String,
        receiveIdType: String = "open_id"
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val content = """{"file_key":"$fileKey"}"""
            val body = mapOf(
                "receive_id" to receiveId,
                "msg_type" to "file",
                "content" to content
            )

            val result = client.post(
                "/open-apis/im/v1/messages?receive_id_type=$receiveIdType",
                body
            )

            if (result.isFailure) {
                return@withContext Result.failure(result.exceptionOrNull()!!)
            }

            val data = result.getOrNull()?.getAsJsonObject("data")
            val messageId = data?.get("message_id")?.asString
                ?: return@withContext Result.failure(Exception("Missing message_id"))

            Log.d(TAG, "File message sent: $messageId")
            Result.success(messageId)

        } catch (e: Exception) {
            Log.e(TAG, "Failed to send file message", e)
            Result.failure(e)
        }
    }

    // ===== 内部方法 =====

    /**
     * 上传媒体文件
     */
    private suspend fun uploadMediaFile(file: File, type: String): Result<MediaUploadResult> {
        val tokenResult = client.getTenantAccessToken()
        if (tokenResult.isFailure) {
            return Result.failure(tokenResult.exceptionOrNull()!!)
        }
        val token = tokenResult.getOrNull()!!

        val requestBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("image_type", type)
            .addFormDataPart(
                "image",
                file.name,
                file.asRequestBody("application/octet-stream".toMediaType())
            )
            .build()

        val request = Request.Builder()
            .url("${config.getApiBaseUrl()}/open-apis/im/v1/images")
            .addHeader("Authorization", "Bearer $token")
            .post(requestBody)
            .build()

        val response = httpClient.newCall(request).execute()
        val responseBody = response.body?.string() ?: ""

        if (!response.isSuccessful) {
            return Result.failure(Exception("HTTP ${response.code}"))
        }

        val json = com.google.gson.Gson().fromJson(responseBody, JsonObject::class.java)
        val code = json.get("code")?.asInt ?: -1

        if (code != 0) {
            val msg = json.get("msg")?.asString ?: "Unknown error"
            return Result.failure(Exception(msg))
        }

        val data = json.getAsJsonObject("data")
        val imageKey = data?.get("image_key")?.asString
            ?: return Result.failure(Exception("Missing image_key"))

        Log.d(TAG, "Media uploaded: $imageKey")
        return Result.success(MediaUploadResult(imageKey))
    }

    /**
     * 下载媒体文件
     */
    private suspend fun downloadMediaFile(key: String, type: String, outputFile: File): Result<Unit> {
        val tokenResult = client.getTenantAccessToken()
        if (tokenResult.isFailure) {
            return Result.failure(tokenResult.exceptionOrNull()!!)
        }
        val token = tokenResult.getOrNull()!!

        val request = Request.Builder()
            .url("${config.getApiBaseUrl()}/open-apis/im/v1/images/$key")
            .addHeader("Authorization", "Bearer $token")
            .get()
            .build()

        val response = httpClient.newCall(request).execute()

        if (!response.isSuccessful) {
            return Result.failure(Exception("HTTP ${response.code}"))
        }

        val inputStream = response.body?.byteStream()
            ?: return Result.failure(Exception("Empty response body"))

        outputFile.outputStream().use { output ->
            inputStream.copyTo(output)
        }

        Log.d(TAG, "Media downloaded: ${outputFile.absolutePath}")
        return Result.success(Unit)
    }
}

/**
 * 媒体上传结果
 */
data class MediaUploadResult(
    val key: String
)
