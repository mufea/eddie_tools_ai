package com.example.data

import android.net.Uri
import android.util.Base64
import android.util.Log
import com.example.AppContextProvider
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * PRODUCTION-READY ASYNC CLIENT FOR FAL AI VEO 3.1 LITE
 * 
 * Implements the asynchronous queue protocol according to specific guidelines:
 * - Model: fal-ai/veo3.1/lite
 * - Base Submission URL: POST https://queue.fal.run/fal-ai/veo3.1/lite
 * - Status Polling URL: GET https://queue.fal.run/fal-ai/veo3.1/lite/requests/{request_id}/status
 * - Result Retrieval URL: GET https://queue.fal.run/fal-ai/veo3.1/lite/requests/{request_id}
 * - Auth Header: "Authorization: Key ${FAL_KEY}"
 * - 5-Second status polling interval with complete queue status logs
 * - Transient HTTP retry engine (up to 3 attempts per call)
 * - 10-Minute generation timeout protection
 * 
 * ---------------------------------------------------------------------------
 * BACKEND REST API ENDPOINT EXAMPLE (e.g. Ktor Server / Spring Boot / Express)
 * ---------------------------------------------------------------------------
 * Here is an example of how you can expose this on your backend:
 * 
 * ```kotlin
 * // Ktor REST API Endpoint Example
 * route("/api/video/generate") {
 *     post {
 *         val request = call.receive<VideoGenerateRequest>()
 *         val apiKey = System.getenv("FAL_KEY") ?: throw IllegalStateException("FAL_KEY is missing!")
 *         
 *         // 1. Submit text-to-video / image-to-video request
 *         val submitResponse = falVeo3Client.submitToQueue(request, apiKey)
 *         
 *         // 2. Return request_id immediately to the client
 *         call.respond(HttpStatusCode.Accepted, mapOf("request_id" to submitResponse.requestId))
 *         
 *         // 3. Optional: Poll asynchronously or let the mobile app call status directly
 *     }
 * }
 * ```
 */
class FalVeo3Client {

    private val tag = "FalVeo3Client"

    // High performance shared HTTP client configured for resilience
    private val httpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    /**
     * Submit video generation request to the Fal AI queue.
     * Supports both Text-to-Video and Image-to-Video across different models (lite, fast, etc.).
     */
    suspend fun generateVideoAsync(
        prompt: String,
        imageUrl: String?,
        mode: String,
        aspectRatio: String,
        duration: String,
        resolution: String,
        generateAudio: Boolean,
        model: String = "fal-ai/veo3.1/lite",
        customApiKey: String? = null,
        seed: Int? = null,
        autoFix: Boolean = true,
        enhancePrompt: Boolean = true,
        negativePrompt: String? = null,
        safetyTolerance: Int = 4,
        onLog: (String) -> Unit = {}
    ): String = withContext(Dispatchers.IO) {
        // Resolve dynamic key
        val keyToUse = if (!customApiKey.isNullOrBlank()) {
            customApiKey.trim()
        } else {
            BuildConfig.FAL_API_KEY
        }

        if (keyToUse.isBlank() || keyToUse.contains("PLACEHOLDER")) {
            throw IllegalStateException("Fal.ai API key is missing. Please set your FAL_API_KEY in the Secrets panel.")
        }

        val actualModel = if (model.contains("/")) model.trim() else "fal-ai/$model"
        val isImageToVideo = mode == "Image to Video" && !imageUrl.isNullOrBlank()
        val baseEndpoint = "https://queue.fal.run/$actualModel"

        onLog("> Constructing production payload for $actualModel...\n")

        val durationSecs = when {
            duration.contains("4") -> "4s"
            duration.contains("6") -> "6s"
            duration.contains("8") -> "8s"
            else -> "5s"
        }
        val aspectToUse = when (aspectRatio) {
            "16:9", "9:16", "1:1", "21:9" -> aspectRatio
            else -> "16:9"
        }
        val resolutionToUse = when (resolution) {
            "720p", "1080p" -> resolution
            else -> "720p"
        }

        // Build robust JSON matching API schema
        val requestJson = JSONObject().apply {
            put("prompt", prompt)
            put("resolution", resolutionToUse)
            put("duration", durationSecs)
            put("aspect_ratio", aspectToUse)
            put("generate_audio", generateAudio)
            put("auto_fix", autoFix)
            put("enhance_prompt", enhancePrompt)
            put("safety_tolerance", safetyTolerance)
            
            if (seed != null) {
                put("seed", seed)
            }
            if (!negativePrompt.isNullOrBlank()) {
                put("negative_prompt", negativePrompt)
            }

            if (isImageToVideo) {
                onLog("> Processing Image-to-Video mode...\n")
                val resolvedImg = resolveImageToDataUrlOrKeep(imageUrl!!)
                onLog("> Image payload processed successfully!\n")
                put("image_url", resolvedImg)
            }
        }

        onLog("> Submitting job to Fal AI queue: $baseEndpoint\n")
        val requestBody = requestJson.toString().toRequestBody("application/json".toMediaTypeOrNull())

        val submitRequest = Request.Builder()
            .url(baseEndpoint)
            .header("Authorization", "Key $keyToUse")
            .header("Content-Type", "application/json")
            .post(requestBody)
            .build()

        // Submit to queue with retry capability for transient network/HTTP issues
        val submitResponseBody = executeWithRetry(submitRequest, "Submission", onLog)
        val submitJson = JSONObject(submitResponseBody)

        val requestId = submitJson.optString("request_id")
        if (requestId.isNullOrBlank()) {
            throw IllegalStateException("Fal AI queue submission failed: Response missing request_id. Payload: $submitResponseBody")
        }

        onLog("> Submission Successful! Request ID: $requestId\n")
        onLog("> Return request_id immediately to the client context.\n")

        // Use dynamic URLs from Fal AI submit response first to prevent 405 Method Not Allowed issues
        val statusUrlFromResponse = submitJson.optString("status_url")
        val resultUrlFromResponse = submitJson.optString("response_url")

        val statusUrl = if (!statusUrlFromResponse.isNullOrBlank()) {
            statusUrlFromResponse
        } else {
            "https://queue.fal.run/$actualModel/requests/$requestId/status"
        }

        val resultUrl = if (!resultUrlFromResponse.isNullOrBlank()) {
            resultUrlFromResponse
        } else {
            "https://queue.fal.run/$actualModel/requests/$requestId"
        }

        onLog("> Selected Status URL: $statusUrl\n")
        onLog("> Selected Result URL: $resultUrl\n")

        onLog("> Initiating 5-second polling loop with 10-minute timeout safety...\n")

        val pollIntervalMs = 5000L
        val maxDurationMs = 10 * 60 * 1000L // 10 minutes timeout as required
        val startTime = System.currentTimeMillis()
        var completed = false

        while (!completed) {
            if (System.currentTimeMillis() - startTime > maxDurationMs) {
                throw IllegalStateException("Fal AI video generation timed out after 10 minutes.")
            }

            delay(pollIntervalMs)

            onLog("> Checking queue status...\n")
            val statusRequest = Request.Builder()
                .url(statusUrl)
                .header("Authorization", "Key $keyToUse")
                .get()
                .build()

            try {
                // Request status with transient failure retries
                val statusBodyStr = executeWithRetry(statusRequest, "Status Check", onLog)
                val statusJson = JSONObject(statusBodyStr)
                val status = statusJson.optString("status").uppercase()

                onLog("> Queue Status Update: $status\n")

                when (status) {
                    "COMPLETED" -> {
                        completed = true
                    }
                    "FAILED" -> {
                        val errMsg = statusJson.optString("error", "Unknown Fal AI queue error.")
                        throw IllegalStateException("Fal AI Queue Generation Failed: $errMsg")
                    }
                    "IN_QUEUE", "IN_PROGRESS" -> {
                        // Continue polling
                    }
                    else -> {
                        onLog("> Warning: Encountered unexpected status code/message: $status\n")
                    }
                }
            } catch (e: Exception) {
                onLog("> Polling warning: ${e.localizedMessage}. Continuing loop...\n")
            }
        }

        onLog("> Generation Completed! Fetching final video assets...\n")

        val resultRequest = Request.Builder()
            .url(resultUrl)
            .header("Authorization", "Key $keyToUse")
            .get()
            .build()

        val resultBodyStr = executeWithRetry(resultRequest, "Result Retrieval", onLog)
        val resultJson = JSONObject(resultBodyStr)

        val videoObj = resultJson.optJSONObject("video")
        if (videoObj != null) {
            val url = videoObj.optString("url")
            if (!url.isNullOrBlank()) {
                onLog("> Success! Extracted video url: $url\n")
                return@withContext url
            }
        }

        // Direct fallback checks
        val fallbackUrl = resultJson.optString("video_url").ifBlank { resultJson.optString("url") }
        if (!fallbackUrl.isNullOrBlank()) {
            onLog("> Success! Extracted direct video url: $fallbackUrl\n")
            return@withContext fallbackUrl
        }

        throw IllegalStateException("Video completed but response format lacked final video URL details: $resultBodyStr")
    }

    /**
     * Executes an HTTP request with built-in retry mechanics for transient errors (5xx, timeouts, IO exceptions).
     */
    private suspend fun executeWithRetry(
        request: Request,
        actionName: String,
        onLog: (String) -> Unit,
        maxRetries: Int = 3
    ): String = withContext(Dispatchers.IO) {
        var lastException: Exception? = null
        for (attempt in 1..maxRetries) {
            try {
                httpClient.newCall(request).execute().use { response ->
                    val bodyStr = response.body?.string() ?: ""
                    if (response.isSuccessful) {
                        return@withContext bodyStr
                    }

                    // Treat 5xx or specific transient codes as retriable
                    if (response.code >= 500 && attempt < maxRetries) {
                        throw IOException("Transient server error (${response.code}) during $actionName")
                    }

                    // Non-retriable or final failure
                    throw IllegalStateException("API Error Code ${response.code}: $bodyStr")
                }
            } catch (e: IOException) {
                lastException = e
                if (attempt < maxRetries) {
                    onLog("> Connection anomaly during $actionName: ${e.localizedMessage}. Retry $attempt/$maxRetries...\n")
                    delay(2000L * attempt)
                }
            } catch (e: Exception) {
                // If it's a structural or logical exception, throw immediately
                throw e
            }
        }
        throw lastException ?: IllegalStateException("Failed $actionName after $maxRetries retry attempts.")
    }

    /**
     * Helper to resolve local Android uris (content/file) to Data URI / Base64 format if required for API compatibility.
     */
    private fun resolveImageToDataUrlOrKeep(imageUrl: String): String {
        if (imageUrl.startsWith("content://") || imageUrl.startsWith("file://")) {
            try {
                val context = AppContextProvider.context
                val uri = Uri.parse(imageUrl)
                val mimeType = context.contentResolver.getType(uri) ?: "image/jpeg"
                context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    val bytes = inputStream.readBytes()
                    val base64Str = Base64.encodeToString(bytes, Base64.NO_WRAP)
                    return "data:$mimeType;base64,$base64Str"
                }
            } catch (e: Exception) {
                Log.e(tag, "Failed to base64 encode content uri", e)
            }
        }
        return imageUrl
    }
}
