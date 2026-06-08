package com.example.data

import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

enum class Role {
    USER, MODEL
}

data class ChatMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val role: Role,
    val text: String,
    val isBookmarked: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)

class ChatRepository {
    private val defaultApiKey = BuildConfig.GEMINI_API_KEY
    private val defaultSystemInstruction = "You are Eddie, a helpful and expert AI Assistant. Please use Indonesian if requested, or Markdown formatting for easy reading."
    
    // Lazy HTTP client to preserve resources
    private val client by lazy {
        OkHttpClient.Builder()
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build()
    }

    private fun getFallbackKey(activeKey: String, configKey: String, placeholder: String): String? {
        val trimmedActive = activeKey.trim()
        if (trimmedActive.isNotBlank()) return trimmedActive
        val trimmedConfig = configKey.trim()
        if (trimmedConfig.isNotBlank() && trimmedConfig != placeholder) {
            return trimmedConfig
        }
        return null
    }

    suspend fun sendMessage(
        history: List<ChatMessage>,
        newPrompt: String,
        model: String,
        provider: String,
        customSystemInstruction: String? = null,
        providerKeys: Map<String, String> = emptyMap(),
        providerUrls: Map<String, String> = emptyMap()
    ): String {
        return withContext(Dispatchers.IO) {
            val systemText = customSystemInstruction?.ifBlank { null } ?: defaultSystemInstruction
            val activeKey = providerKeys[provider]?.trim() ?: ""

            val resolvedModel = when (model) {
                "gpt-5-mini" -> "openai"
                "gpt5.5" -> "openai"
                "gemini-2.5-pro" -> "gemini-2.5-pro"
                "gemini-3.1-pro-preview" -> "gemini-3.1-pro-preview"
                "gemini-3-flash-preview" -> "gemini-3-flash-preview"
                "grok-4-0709" -> "openai"
                "deepseek-chat" -> "deepseek"
                else -> model
            }

            // Sanitize conversation history to prevent models from seeing and mimicking 
            // system-level Token Usage or Pemakaian Token status notifications previously written to DB
            val sanitizedHistory = history.map { msg ->
                val idx = msg.text.indexOf("\n\n---\n*🪙")
                val cleanText = if (idx != -1) msg.text.substring(0, idx) else msg.text
                msg.copy(text = cleanText)
            }

            when (provider) {
                "Gemini" -> {
                    val fallbackKey = getFallbackKey(activeKey, BuildConfig.GEMINI_API_KEY, "PLACEHOLDER_GEMINI_API_KEY")
                    if (fallbackKey == null) {
                        "API Key Gemini is missing. Please set your Gemini API Key in Settings to start chatting."
                    } else {
                        val customUrl = providerUrls["Gemini"]?.trim()
                        callGemini(sanitizedHistory, newPrompt, if (resolvedModel == "openai") "gemini-3-flash-preview" else resolvedModel, systemText, fallbackKey, customUrl)
                    }
                }
                "OpenAI" -> {
                    val fallbackKey = getFallbackKey(activeKey, BuildConfig.OPENAI_API_KEY, "PLACEHOLDER_OPENAI_API_KEY")
                    if (fallbackKey == null) {
                        "API Key OpenAI is missing. Please set your OpenAI API Key in Settings to start chatting."
                    } else {
                        val customUrl = providerUrls["OpenAI"]?.trim()?.ifBlank { null }
                            ?: "https://api.openai.com/v1/chat/completions"
                        callOpenAiCompatible(
                            history = sanitizedHistory,
                            newPrompt = newPrompt,
                            model = if (resolvedModel == "openai") "gpt-5-mini" else resolvedModel,
                            systemText = systemText,
                            apiKey = fallbackKey,
                            baseUrl = customUrl
                        )
                    }
                }
                "DeepSeek" -> {
                    val fallbackKey = getFallbackKey(activeKey, BuildConfig.DEEPSEEK_API_KEY, "PLACEHOLDER_DEEPSEEK_API_KEY")
                    if (fallbackKey == null) {
                        "API Key DeepSeek is missing. Please set your DeepSeek API Key in Settings to start chatting."
                    } else {
                        val customUrl = providerUrls["DeepSeek"]?.trim()?.ifBlank { null }
                            ?: "https://api.deepseek.com/v1/chat/completions"
                        callOpenAiCompatible(
                            history = sanitizedHistory,
                            newPrompt = newPrompt,
                            model = "deepseek-chat",
                            systemText = systemText,
                            apiKey = fallbackKey,
                            baseUrl = customUrl
                        )
                    }
                }
                "Claude" -> {
                    val fallbackKey = getFallbackKey(activeKey, BuildConfig.ANTHROPIC_API_KEY, "PLACEHOLDER_ANTHROPIC_API_KEY")
                    if (fallbackKey == null || fallbackKey.contains("PLACEHOLDER") || fallbackKey.isBlank()) {
                        "API Key Anthropic Claude is missing. Please set your Claude API Key in Settings to start chatting."
                    } else {
                        val customUrl = providerUrls["Claude"]?.trim()?.ifBlank { null }
                            ?: "https://api.anthropic.com/v1/messages"
                        callClaude(sanitizedHistory, newPrompt, resolvedModel, systemText, fallbackKey, customUrl)
                    }
                }
                "XAI" -> {
                    val fallbackKey = getFallbackKey(activeKey, BuildConfig.XAI_API_KEY, "PLACEHOLDER_XAI_API_KEY")
                    if (fallbackKey == null || fallbackKey.contains("PLACEHOLDER") || fallbackKey.isBlank()) {
                        "API Key xAI is missing. Please set your xAI API Key in Settings to start chatting."
                    } else {
                        val customUrl = providerUrls["XAI"]?.trim()?.ifBlank { null }
                            ?: "https://api.x.ai/v1/chat/completions"
                        callOpenAiCompatible(
                            history = sanitizedHistory,
                            newPrompt = newPrompt,
                            model = if (resolvedModel == "openai" || resolvedModel == "grok-4-0709" || resolvedModel == "grok-beta") "grok-beta" else resolvedModel,
                            systemText = systemText,
                            apiKey = fallbackKey,
                            baseUrl = customUrl
                        )
                    }
                }
                else -> {
                    "Provider '$provider' is not supported. Please choose a supported provider like Gemini or OpenAI."
                }
            }
        }
    }

    private suspend fun callGemini(
        history: List<ChatMessage>,
        newPrompt: String,
        model: String,
        systemText: String,
        apiKey: String,
        customBaseUrl: String? = null
    ): String {
        val activeSystemInstruction = Content(
            parts = listOf(Part(systemText))
        )

        val apiContents = history.map {
            Content(
                parts = listOf(Part(text = it.text)),
                role = if (it.role == Role.USER) "user" else "model"
            )
        }.toMutableList()
        
        // Add the new prompt
        apiContents.add(Content(parts = listOf(Part(text = newPrompt)), role = "user"))

        val request = GenerateContentRequest(
            contents = apiContents,
            systemInstruction = activeSystemInstruction
        )

        return try {
            val baseUrl = customBaseUrl?.trim()?.ifBlank { null } ?: "https://generativelanguage.googleapis.com/"
            val resolvedUrl = "${baseUrl}v1beta/models/${model}:generateContent"
            val response = GeminiClient.service.generateContent(resolvedUrl, apiKey, request)
            response.candidates.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: "Tidak ada respons dari Gemini."
        } catch (e: Exception) {
            "Error: ${e.localizedMessage}"
        }
    }

    private fun callOpenAiCompatible(
        history: List<ChatMessage>,
        newPrompt: String,
        model: String,
        systemText: String,
        apiKey: String,
        baseUrl: String
    ): String {
        return try {
            val jsonBody = JSONObject().apply {
                put("model", model)
                
                val messagesArray = JSONArray().apply {
                    put(JSONObject().apply {
                        put("role", "system")
                        put("content", systemText)
                    })
                    
                    history.forEach { msg ->
                        put(JSONObject().apply {
                            put("role", if (msg.role == Role.USER) "user" else "assistant")
                            put("content", msg.text)
                        })
                    }
                    
                    put(JSONObject().apply {
                        put("role", "user")
                        put("content", newPrompt)
                    })
                }
                
                put("messages", messagesArray)
            }

            val requestBody = jsonBody.toString().toRequestBody("application/json".toMediaTypeOrNull())
            val request = Request.Builder()
                .url(baseUrl)
                .header("Authorization", "Bearer $apiKey")
                .header("Content-Type", "application/json")
                .post(requestBody)
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return "HTTP Error ${response.code}: ${response.body?.string() ?: "Failed to connect to the API server."}"
                }
                val resStr = response.body?.string() ?: return "Empty response from API server."
                val responseJson = JSONObject(resStr)
                responseJson.getJSONArray("choices")
                    .getJSONObject(0)
                    .getJSONObject("message")
                    .getString("content")
            }
        } catch (e: Exception) {
            "Failed to make API request: ${e.localizedMessage}"
        }
    }

    private fun callClaude(
        history: List<ChatMessage>,
        newPrompt: String,
        model: String,
        systemText: String,
        apiKey: String,
        baseUrl: String
    ): String {
        return try {
            val jsonBody = JSONObject().apply {
                put("model", model)
                put("system", systemText)
                put("max_tokens", 4096)
                
                val messagesArray = JSONArray().apply {
                    history.forEach { msg ->
                        put(JSONObject().apply {
                            put("role", if (msg.role == Role.USER) "user" else "assistant")
                            put("content", msg.text)
                        })
                    }
                    
                    put(JSONObject().apply {
                        put("role", "user")
                        put("content", newPrompt)
                    })
                }
                
                put("messages", messagesArray)
            }

            val requestBody = jsonBody.toString().toRequestBody("application/json".toMediaTypeOrNull())
            val request = Request.Builder()
                .url(baseUrl)
                .header("x-api-key", apiKey)
                .header("anthropic-version", "2023-06-01")
                .header("Content-Type", "application/json")
                .post(requestBody)
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return "HTTP Claude Error ${response.code}: ${response.body?.string() ?: "Failed to connect with Anthropic."}"
                }
                val resStr = response.body?.string() ?: return "Empty Claude response."
                val responseJson = JSONObject(resStr)
                responseJson.getJSONArray("content")
                    .getJSONObject(0)
                    .getString("text")
            }
        } catch (e: Exception) {
            "Failed to make Claude API request: ${e.localizedMessage}"
        }
    }

    suspend fun sendSimpleRequest(
        prompt: String,
        systemText: String,
        providerKeys: Map<String, String> = emptyMap(),
        providerUrls: Map<String, String> = emptyMap()
    ): String {
        return withContext(Dispatchers.IO) {
            val activeKey = providerKeys["Gemini"]?.trim() ?: ""
            val keyToUse = if (activeKey.isNotBlank()) activeKey else defaultApiKey
            val activeSystemInstruction = Content(
                parts = listOf(Part(systemText))
            )
            val request = GenerateContentRequest(
                contents = listOf(Content(parts = listOf(Part(text = prompt)), role = "user")),
                systemInstruction = activeSystemInstruction
            )
            try {
                val baseUrl = providerUrls["Gemini"]?.trim()?.ifBlank { null } ?: "https://generativelanguage.googleapis.com/"
                val resolvedUrl = "${baseUrl}v1beta/models/gemini-3-flash-preview:generateContent"
                val response = GeminiClient.service.generateContent(resolvedUrl, keyToUse, request)
                response.candidates.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: "No response from Gemini."
            } catch (e: Exception) {
                "Error: ${e.localizedMessage}"
            }
        }
    }

    suspend fun sendVisionRequest(
        bitmap: android.graphics.Bitmap,
        prompt: String,
        providerKeys: Map<String, String> = emptyMap(),
        providerUrls: Map<String, String> = emptyMap()
    ): String {
        return withContext(Dispatchers.IO) {
            val activeKey = providerKeys["Gemini"]?.trim() ?: ""
            val keyToUse = if (activeKey.isNotBlank()) activeKey else defaultApiKey
            
            // Convert Bitmap to Base64
            val outputStream = java.io.ByteArrayOutputStream()
            bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 80, outputStream)
            val base64Data = android.util.Base64.encodeToString(outputStream.toByteArray(), android.util.Base64.NO_WRAP)
            
            val request = GenerateContentRequest(
                contents = listOf(Content(
                    parts = listOf(
                        Part(text = prompt),
                        Part(inlineData = InlineData(mimeType = "image/jpeg", data = base64Data))
                    ),
                    role = "user"
                ))
            )
            try {
                val baseUrl = providerUrls["Gemini"]?.trim()?.ifBlank { null } ?: "https://generativelanguage.googleapis.com/"
                val resolvedUrl = "${baseUrl}v1beta/models/gemini-3-flash-preview:generateContent"
                val response = GeminiClient.service.generateContent(resolvedUrl, keyToUse, request)
                response.candidates.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: "No response from Gemini."
            } catch (e: Exception) {
                "Error: ${e.localizedMessage}"
            }
        }
    }

    suspend fun generateOpenAiImage(
        prompt: String,
        model: String,
        size: String = "1024x1024",
        aspectRatio: String = "1:1",
        customApiKey: String? = null,
        customBaseUrl: String? = null
    ): String {
        return withContext(Dispatchers.IO) {
            val keyToUse = if (!customApiKey.isNullOrBlank()) customApiKey.trim() else BuildConfig.OPENAI_API_KEY
            if (keyToUse.isBlank() || keyToUse.contains("PLACEHOLDER")) {
                throw IllegalStateException("OpenAI API key is not configured. Please add your OpenAI API key in the API Key Settings.")
            }

            val baseUrl = customBaseUrl?.trim()?.ifBlank { null } ?: "https://api.openai.com/v1/"
            val actualModelName = model.trim()

            val resolvedSize = if (actualModelName.lowercase().contains("gpt-image-2")) {
                when (aspectRatio.trim()) {
                    "16:9", "21:9", "3:2", "1536:1024", "1792:1024" -> "1792x1024"
                    "9:16", "9:21", "2:3", "4:5", "1024:1536", "1024:1792" -> "1024x1792"
                    else -> "1024x1024"
                }
            } else {
                "1024x1024"
            }

            val jsonBody = JSONObject().apply {
                put("model", actualModelName)
                put("prompt", prompt)
                put("n", 1)
                put("size", resolvedSize)
            }

            val requestBody = jsonBody.toString().toRequestBody("application/json".toMediaTypeOrNull())
            
            val resolvedUrl = if (baseUrl.endsWith("images/generations")) {
                baseUrl
            } else if (baseUrl.endsWith("/")) {
                "${baseUrl}images/generations"
            } else {
                "${baseUrl}/images/generations"
            }

            val request = Request.Builder()
                .url(resolvedUrl)
                .header("Authorization", "Bearer $keyToUse")
                .header("Content-Type", "application/json")
                .post(requestBody)
                .build()

            client.newCall(request).execute().use { response ->
                val bodyStr = response.body?.string() ?: ""
                if (!response.isSuccessful) {
                    throw IllegalStateException("OpenAI Error ${response.code}: $bodyStr")
                }
                val responseJson = JSONObject(bodyStr)
                
                // 1. Try OpenAI standard "data" array
                val dataArray = responseJson.optJSONArray("data")
                if (dataArray != null && dataArray.length() > 0) {
                    val firstElement = dataArray.opt(0)
                    if (firstElement is String && firstElement.isNotBlank()) {
                        return@withContext firstElement.trim()
                    } else if (firstElement is JSONObject) {
                        val urlKeys = listOf("url", "image_url", "imageUrl", "b64_json", "image", "uri")
                        for (key in urlKeys) {
                            val value = firstElement.optString(key)
                            if (!value.isNullOrBlank()) {
                                if (key == "b64_json" && !value.startsWith("data:")) {
                                    return@withContext "data:image/png;base64,${value.trim()}"
                                } else {
                                    return@withContext value.trim()
                                }
                            }
                        }
                    }
                }
                
                // 1.1 Try "data" as a single JSON object (common in some custom gateways)
                val dataObj = responseJson.optJSONObject("data")
                if (dataObj != null) {
                    val urlKeys = listOf("url", "image_url", "imageUrl", "b64_json", "image", "uri")
                    for (key in urlKeys) {
                        val value = dataObj.optString(key)
                        if (!value.isNullOrBlank()) {
                            if (key == "b64_json" && !value.startsWith("data:")) {
                                return@withContext "data:image/png;base64,${value.trim()}"
                            } else {
                                return@withContext value.trim()
                            }
                        }
                    }
                }
                
                // 2. Try root level keys (direct url/image representation)
                val rootKeys = listOf("url", "image_url", "imageUrl", "image", "uri")
                for (key in rootKeys) {
                    val value = responseJson.optString(key)
                    if (!value.isNullOrBlank()) {
                        return@withContext value.trim()
                    }
                }
                
                // 3. Try "images" array (popular on many proxy setups)
                val imagesArray = responseJson.optJSONArray("images")
                if (imagesArray != null && imagesArray.length() > 0) {
                    val firstElement = imagesArray.opt(0)
                    if (firstElement is String && firstElement.isNotBlank()) {
                        return@withContext firstElement.trim()
                    } else if (firstElement is JSONObject) {
                        val innerKeys = listOf("url", "image_url", "imageUrl", "b64_json", "image", "uri")
                        for (key in innerKeys) {
                            val value = firstElement.optString(key)
                            if (!value.isNullOrBlank()) {
                                if (key == "b64_json" && !value.startsWith("data:")) {
                                    return@withContext "data:image/png;base64,${value.trim()}"
                                } else {
                                    return@withContext value.trim()
                                }
                            }
                        }
                    }
                }

                // 3.1 Try "images" as a single JSON object
                val imagesObj = responseJson.optJSONObject("images")
                if (imagesObj != null) {
                    val urlKeys = listOf("url", "image_url", "imageUrl", "b64_json", "image", "uri")
                    for (key in urlKeys) {
                        val value = imagesObj.optString(key)
                        if (!value.isNullOrBlank()) {
                            if (key == "b64_json" && !value.startsWith("data:")) {
                                return@withContext "data:image/png;base64,${value.trim()}"
                            } else {
                                return@withContext value.trim()
                            }
                        }
                    }
                }

                // 3.2 Try "output" field structure (used by Replicate/RunPod translation proxies)
                val outputValue = responseJson.opt("output")
                if (outputValue != null) {
                    if (outputValue is String && outputValue.isNotBlank()) {
                        return@withContext outputValue.trim()
                    } else if (outputValue is JSONArray && outputValue.length() > 0) {
                        val firstElem = outputValue.opt(0)
                        if (firstElem is String && firstElem.isNotBlank()) {
                            return@withContext firstElem.trim()
                        } else if (firstElem is JSONObject) {
                            val innerKeys = listOf("url", "image_url", "imageUrl", "b64_json", "image", "uri")
                            for (key in innerKeys) {
                                val value = firstElem.optString(key)
                                if (!value.isNullOrBlank()) {
                                    if (key == "b64_json" && !value.startsWith("data:")) {
                                        return@withContext "data:image/png;base64,${value.trim()}"
                                    } else {
                                        return@withContext value.trim()
                                    }
                                }
                            }
                        }
                    } else if (outputValue is JSONObject) {
                        val innerKeys = listOf("url", "image_url", "imageUrl", "b64_json", "image", "uri")
                        for (key in innerKeys) {
                            val value = outputValue.optString(key)
                            if (!value.isNullOrBlank()) {
                                if (key == "b64_json" && !value.startsWith("data:")) {
                                    return@withContext "data:image/png;base64,${value.trim()}"
                                } else {
                                    return@withContext value.trim()
                                }
                            }
                        }
                    }
                }
                
                // 4. Failing all, build highly detailed exception
                val availableKeys = mutableListOf<String>()
                val keysIterator = responseJson.keys()
                while (keysIterator.hasNext()) {
                    availableKeys.add(keysIterator.next())
                }
                throw IllegalStateException("Could not find image URL/b64 in response. Available root keys: $availableKeys. Response: $bodyStr")
            }
        }
    }

    suspend fun generateFalImage(
        prompt: String,
        endpointUrl: String = "https://queue.fal.run/fal-ai/flux/schnell",
        aspectRatio: String = "1:1",
        customApiKey: String? = null
    ): String {
        return withContext(Dispatchers.IO) {
            val keyToUse = if (!customApiKey.isNullOrBlank()) customApiKey.trim() else BuildConfig.FAL_API_KEY
            if (keyToUse.isBlank() || keyToUse.contains("PLACEHOLDER")) {
                throw IllegalStateException("Fal.ai API key is not configured. Please add your FAL_API_KEY in the Secrets panel.")
            }

            val resolvedImageSize = when (aspectRatio.trim()) {
                "16:9" -> "16:9"
                "9:16" -> "9:16"
                "3:2", "1536:1024" -> "3:2"
                "2:3", "1024:1536" -> "2:3"
                "4:5" -> "4:5"
                "21:9" -> "21:9"
                "9:21" -> "9:21"
                else -> "square_hd"
            }

            val jsonBody = JSONObject().apply {
                put("prompt", prompt)
                put("image_size", resolvedImageSize)
                put("sync_mode", true)
            }

            val requestBody = jsonBody.toString().toRequestBody("application/json".toMediaTypeOrNull())
            val resolvedUrl = if (endpointUrl.contains("?")) endpointUrl else "$endpointUrl?sync_mode=true"
            
            val request = Request.Builder()
                .url(resolvedUrl)
                .header("Authorization", "Key $keyToUse")
                .header("Content-Type", "application/json")
                .post(requestBody)
                .build()

            client.newCall(request).execute().use { response ->
                val bodyStr = response.body?.string() ?: ""
                if (!response.isSuccessful) {
                    throw IllegalStateException("Fal.ai Error ${response.code}: $bodyStr")
                }
                val responseJson = JSONObject(bodyStr)
                val imagesArray = responseJson.optJSONArray("images")
                if (imagesArray != null && imagesArray.length() > 0) {
                    imagesArray.getJSONObject(0).getString("url")
                } else {
                    throw IllegalStateException("No image generated by Fal.ai. Response: $bodyStr")
                }
            }
        }
    }

    suspend fun generateStabilityImage(
        prompt: String,
        model: String = "core",
        aspectRatio: String = "1024:1024",
        customApiKey: String? = null
    ): String {
        return withContext(Dispatchers.IO) {
            val keyToUse = if (!customApiKey.isNullOrBlank()) customApiKey.trim() else BuildConfig.STABILITY_API_KEY
            if (keyToUse.isBlank() || keyToUse.contains("PLACEHOLDER")) {
                throw IllegalStateException("Stability AI API key is not configured. Please add your STABILITY_API_KEY in the Secrets panel.")
            }

            // Resolve endpoint based on model selection
            val endpointPath = when (model.lowercase()) {
                "sd3" -> "sd3"
                "ultra" -> "ultra"
                else -> "core"
            }
            val resolvedUrl = "https://api.stability.ai/v2beta/stable-image/generate/$endpointPath"

            // Map aspect ratio string to Stability AI expected values ("1:1", "16:9", "21:9", "3:2", "2:3", "4:5", "5:4", "9:16", "9:21")
            val resolvedAspectRatio = when (aspectRatio.trim()) {
                "1536:1024" -> "3:2"
                "1024:1536" -> "2:3"
                "16:9" -> "16:9"
                "9:16" -> "9:16"
                "21:9" -> "21:9"
                "9:21" -> "9:21"
                "3:2" -> "3:2"
                "2:3" -> "2:3"
                "4:5" -> "4:5"
                "5:4" -> "5:4"
                "1:1" -> "1:1"
                else -> {
                    if (aspectRatio.contains(":")) {
                        val parts = aspectRatio.split(":")
                        if (parts.size == 2) {
                            val w = parts[0].toIntOrNull() ?: 1024
                            val h = parts[1].toIntOrNull() ?: 1024
                            if (w > h) "3:2" else if (h > w) "2:3" else "1:1"
                        } else {
                            "1:1"
                        }
                    } else {
                        "1:1"
                    }
                }
            }

            val builder = okhttp3.MultipartBody.Builder()
                .setType(okhttp3.MultipartBody.FORM)
                .addFormDataPart("prompt", prompt)
                .addFormDataPart("output_format", "png")
                .addFormDataPart("aspect_ratio", resolvedAspectRatio)

            // For SD3, check if we need to add other parameters
            if (endpointPath == "sd3") {
                builder.addFormDataPart("mode", "text-to-image")
                builder.addFormDataPart("model", "sd3-large")
            }

            val requestBody = builder.build()

            val request = Request.Builder()
                .url(resolvedUrl)
                .header("Authorization", "Bearer $keyToUse")
                .header("Accept", "application/json")
                .post(requestBody)
                .build()

            client.newCall(request).execute().use { response ->
                val bodyStr = response.body?.string() ?: ""
                if (!response.isSuccessful) {
                    throw IllegalStateException("Stability AI Error ${response.code}: $bodyStr")
                }
                val responseJson = JSONObject(bodyStr)
                val base64ImageBytes = responseJson.optString("image", "")
                if (base64ImageBytes.isNotBlank()) {
                    val bytes = android.util.Base64.decode(base64ImageBytes, android.util.Base64.DEFAULT)
                    val cacheDir = com.example.AppContextProvider.context.cacheDir
                    val file = java.io.File(cacheDir, "stability_img_${System.currentTimeMillis()}_${java.util.UUID.randomUUID()}.png")
                    java.io.FileOutputStream(file).use { out ->
                        out.write(bytes)
                    }
                    "file://${file.absolutePath}"
                } else {
                    throw IllegalStateException("No image returned by Stability AI. Response: $bodyStr")
                }
            }
        }
    }

    suspend fun generateGeminiImage(
        prompt: String,
        model: String = "gemini-2.5-flash",
        aspectRatio: String = "1:1",
        imageSize: String = "1K",
        customApiKey: String? = null
    ): String {
        return withContext(Dispatchers.IO) {
            val keyToUse = if (!customApiKey.isNullOrBlank()) customApiKey.trim() else BuildConfig.GEMINI_API_KEY
            if (keyToUse.isBlank() || keyToUse.contains("PLACEHOLDER")) {
                throw IllegalStateException("Gemini API key is not configured. Please add your GEMINI_API_KEY in the Secrets panel.")
            }

            val request = GenerateContentRequest(
                contents = listOf(Content(
                    parts = listOf(Part(text = prompt))
                )),
                generationConfig = GenerationConfig(
                    imageConfig = ImageConfig(
                        aspectRatio = when (aspectRatio.trim()) {
                            "1024:1024", "1:1" -> "1:1"
                            "1536:1024", "3:2", "4:3" -> "4:3"
                            "1024:1536", "2:3", "3:4" -> "3:4"
                            "16:9" -> "16:9"
                            "9:16" -> "9:16"
                            else -> "1:1"
                        },
                        imageSize = imageSize
                    ),
                    responseModalities = listOf("TEXT", "IMAGE")
                )
            )

            val baseUrl = "https://generativelanguage.googleapis.com/"
            val resolvedUrl = "${baseUrl}v1beta/models/$model:generateContent"
            
            try {
                val response = GeminiClient.service.generateContent(resolvedUrl, keyToUse, request)
                val partWithImage = response.candidates.firstOrNull()?.content?.parts?.firstOrNull { it.inlineData != null }
                val base64Data = partWithImage?.inlineData?.data
                
                if (!base64Data.isNullOrBlank()) {
                    val bytes = android.util.Base64.decode(base64Data, android.util.Base64.DEFAULT)
                    val cacheDir = com.example.AppContextProvider.context.cacheDir
                    val file = java.io.File(cacheDir, "gemini_img_${System.currentTimeMillis()}_${java.util.UUID.randomUUID()}.png")
                    java.io.FileOutputStream(file).use { out ->
                        out.write(bytes)
                    }
                    "file://${file.absolutePath}"
                } else {
                    val responseText = response.candidates.firstOrNull()?.content?.parts?.firstOrNull { it.text != null }?.text
                    throw IllegalStateException(responseText ?: "No image returned by Gemini Imagen.")
                }
            } catch (e: Exception) {
                throw IllegalStateException(e.localizedMessage ?: "Unknown error calling Gemini Imagen.")
            }
        }
    }
}
