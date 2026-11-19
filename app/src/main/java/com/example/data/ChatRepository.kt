package com.example.data

import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
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
    private val defaultSystemInstruction = "You are Prof Eddie, a helpful and expert AI Assistant. Please use Indonesian if requested, or Markdown formatting for easy reading."
    
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

    suspend fun testProxyConnectivity(baseUrl: String = "https://geminiaitools.com/"): Pair<Boolean, String> {
        return withContext(Dispatchers.IO) {
            try {
                val clean = baseUrl.trim()
                val rootUrl = if (clean.endsWith("/api/chat")) clean.removeSuffix("/api/chat")
                else if (clean.endsWith("/v1/chat/completions")) clean.removeSuffix("/v1/chat/completions")
                else clean.trimEnd('/')

                // 1. First test root health check
                try {
                    val rootRequest = Request.Builder().url(rootUrl).get().build()
                    client.newCall(rootRequest).execute().use { rootResp ->
                        if (rootResp.isSuccessful) {
                            val body = rootResp.body?.string() ?: ""
                            return@withContext Pair(true, "Proxy server is ONLINE! (HTTP ${rootResp.code}) $body")
                        }
                    }
                } catch (_: Exception) {}

                // 2. Test chat endpoint
                val targetUrl = if (clean.endsWith("/api/chat") || clean.endsWith("/v1/chat/completions")) clean else "$rootUrl/api/chat"

                val jsonBody = JSONObject().apply {
                    put("contents", JSONArray().put(JSONObject().apply {
                        put("role", "user")
                        put("parts", JSONArray().put(JSONObject().put("text", "ping")))
                    }))
                    put("messages", JSONArray().put(JSONObject().apply {
                        put("role", "user")
                        put("content", "ping")
                    }))
                    put("prompt", "ping")
                }
                val request = Request.Builder()
                    .url(targetUrl)
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer proxy_active")
                    .post(jsonBody.toString().toRequestBody("application/json".toMediaTypeOrNull()))
                    .build()

                client.newCall(request).execute().use { response ->
                    val body = response.body?.string() ?: ""
                    if (response.isSuccessful && !body.contains("API key not valid", ignoreCase = true)) {
                        Pair(true, "Proxy server is ONLINE and functional! (HTTP ${response.code})")
                    } else if (body.contains("API key not valid", ignoreCase = true) || body.contains("API_KEY_INVALID", ignoreCase = true)) {
                        Pair(true, "Proxy Reachable (HTTP ${response.code}), but upstream API key on proxy server has expired. Enter your API key in settings to bypass this.")
                    } else {
                        Pair(true, "Proxy Reachable (HTTP ${response.code}): ${body.take(100)}")
                    }
                }
            } catch (e: Exception) {
                Pair(false, "Failed to connect to proxy: ${e.localizedMessage}")
            }
        }
    }

    suspend fun fetchDynamicModelConfigsFromProxy(
        baseUrl: String = "https://geminiaitools.com/",
        params: List<String>
    ): Result<Map<String, Map<String, String>>> {
        return withContext(Dispatchers.IO) {
            try {
                val clean = baseUrl.trim()
                val rootUrl = if (clean.endsWith("/api/chat")) clean.removeSuffix("/api/chat")
                else if (clean.endsWith("/v1/chat/completions")) clean.removeSuffix("/v1/chat/completions")
                else clean.trimEnd('/')

                val resultMap = mutableMapOf<String, Map<String, String>>()

                for (param in params) {
                    val targetUrl = "$rootUrl/api/keys?model=$param"
                    val request = Request.Builder()
                        .url(targetUrl)
                        .header("User-Agent", "ChatAssistant-App/1.0")
                        .get()
                        .build()

                    client.newCall(request).execute().use { response ->
                        if (response.isSuccessful) {
                            val bodyStr = response.body?.string() ?: ""
                            val json = JSONObject(bodyStr)
                            val targetModel = json.optString("targetModel", "").trim()
                            val modelId = json.optString("modelId", "").trim()
                            val apiKey = (json.optString("apiKey", "").takeIf { it.isNotBlank() } ?: json.optString("key", "")).trim()

                            resultMap[param] = mapOf(
                                "targetModel" to targetModel,
                                "modelId" to modelId,
                                "apiKey" to apiKey
                            )
                        }
                    }
                }

                if (resultMap.isEmpty()) {
                    Result.failure(Exception("Gagal mengambil konfigurasi model dari proxy."))
                } else {
                    Result.success(resultMap)
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    suspend fun fetchApiKeysFromProxy(baseUrl: String = "https://geminiaitools.com/"): Result<Map<String, String>> {
        return withContext(Dispatchers.IO) {
            try {
                val clean = baseUrl.trim()
                val rootUrl = if (clean.endsWith("/api/chat")) clean.removeSuffix("/api/chat")
                else if (clean.endsWith("/v1/chat/completions")) clean.removeSuffix("/v1/chat/completions")
                else clean.trimEnd('/')

                val targetUrl = "$rootUrl/api/keys"
                val request = Request.Builder()
                    .url(targetUrl)
                    .header("User-Agent", "ChatAssistant-App/1.0")
                    .get()
                    .build()

                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        return@withContext Result.failure(Exception("HTTP ${response.code}: ${response.message}"))
                    }
                    val bodyStr = response.body?.string() ?: ""
                    val json = JSONObject(bodyStr)
                    val map = mutableMapOf<String, String>()
                    if (json.has("keys")) {
                        val keysObj = json.getJSONObject("keys")
                        val it = keysObj.keys()
                        while (it.hasNext()) {
                            val k = it.next()
                            val v = keysObj.optString(k, "").trim()
                            if (v.isNotBlank()) {
                                map[k.lowercase()] = v
                            }
                        }
                    }
                    if (json.has("apiKey") && !map.containsKey("openai")) {
                        val defaultKey = json.optString("apiKey", "").trim()
                        if (defaultKey.isNotBlank()) {
                            map["openai"] = defaultKey
                        }
                    }
                    if (map.isEmpty()) {
                        Result.failure(Exception("Tidak ada API key ditemukan di response proxy."))
                    } else {
                        Result.success(map)
                    }
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
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
                "gpt-5-mini" -> "gpt-5-mini"
                "gpt5.5", "gpt-5.5" -> "gpt-5.5"
                "deepseek-chat", "deepseek-flash", "deepseek-v4-flash" -> "deepseek-v4-flash"
                "deepseek-v4-pro" -> "deepseek-v4-pro"
                "claude-opus-4-7", "claude-opus-5" -> "claude-opus-5"
                else -> model
            }

            // Sanitize conversation history to prevent models from seeing and mimicking 
            // system-level Token Usage or Pemakaian Token status notifications previously written to DB
            val sanitizedHistory = history.map { msg ->
                val idx = msg.text.indexOf("\n\n---\n*🪙")
                val cleanText = if (idx != -1) msg.text.substring(0, idx) else msg.text
                msg.copy(text = cleanText)
            }

            val defaultProxy = "https://geminiaitools.com/"

            when (provider.lowercase().trim()) {
                "openai" -> {
                    val fallbackKey = getFallbackKey(activeKey, BuildConfig.OPENAI_API_KEY, "PLACEHOLDER_OPENAI_API_KEY")
                    val customUrl = (providerUrls["OpenAI"] ?: providerUrls["openai"])?.trim()?.ifBlank { null }
                    val targetModel = when (resolvedModel) {
                        "openai", "" -> "gpt-5-mini"
                        "gpt5.5", "gpt" -> "gpt-5.5"
                        else -> resolvedModel
                    }
                    if (fallbackKey != null) {
                        val url = if (!customUrl.isNullOrBlank() && !customUrl.contains("eddie-proxy") && !customUrl.contains("geminiaitools")) {
                            customUrl
                        } else {
                            "https://api.openai.com/v1/chat/completions"
                        }
                        callOpenAiCompatible(
                            history = sanitizedHistory,
                            newPrompt = newPrompt,
                            model = targetModel,
                            systemText = systemText,
                            apiKey = fallbackKey,
                            baseUrl = url
                        )
                    } else {
                        // Route through proxy (geminiaitools.com)
                        val proxyUrl = if (!customUrl.isNullOrBlank()) customUrl else defaultProxy
                        callOpenAiCompatible(
                            history = sanitizedHistory,
                            newPrompt = newPrompt,
                            model = targetModel,
                            systemText = systemText,
                            apiKey = "proxy_active",
                            baseUrl = proxyUrl
                        )
                    }
                }
                "deepseek" -> {
                    val fallbackKey = getFallbackKey(activeKey, BuildConfig.DEEPSEEK_API_KEY, "PLACEHOLDER_DEEPSEEK_API_KEY")
                    val customUrl = (providerUrls["DeepSeek"] ?: providerUrls["Deepseek"] ?: providerUrls["deepseek"])?.trim()?.ifBlank { null }
                    val targetModel = if (resolvedModel.isNotBlank() && resolvedModel != "deepseek") resolvedModel else "deepseek-chat"
                    if (fallbackKey != null && !fallbackKey.contains("PLACEHOLDER") && fallbackKey.isNotBlank()) {
                        val url = if (!customUrl.isNullOrBlank() && !customUrl.contains("eddie-proxy") && !customUrl.contains("geminiaitools")) {
                            customUrl
                        } else {
                            "https://api.deepseek.com/v1/chat/completions"
                        }
                        callOpenAiCompatible(
                            history = sanitizedHistory,
                            newPrompt = newPrompt,
                            model = targetModel,
                            systemText = systemText,
                            apiKey = fallbackKey,
                            baseUrl = url
                        )
                    } else {
                        // Route through proxy (geminiaitools.com)
                        val proxyUrl = if (!customUrl.isNullOrBlank()) customUrl else defaultProxy
                        callOpenAiCompatible(
                            history = sanitizedHistory,
                            newPrompt = newPrompt,
                            model = targetModel,
                            systemText = systemText,
                            apiKey = "proxy_active",
                            baseUrl = proxyUrl
                        )
                    }
                }
                "claude", "anthropic" -> {
                    val fallbackKey = getFallbackKey(activeKey, BuildConfig.ANTHROPIC_API_KEY, "PLACEHOLDER_ANTHROPIC_API_KEY")
                    val customUrl = (providerUrls["Claude"] ?: providerUrls["Anthropic"] ?: providerUrls["claude"] ?: providerUrls["anthropic"])?.trim()?.ifBlank { null }
                    val targetModel = if (resolvedModel.isNotBlank() && resolvedModel != "claude" && resolvedModel != "anthropic") resolvedModel else "claude-sonnet-4-6"
                    if (fallbackKey != null && !fallbackKey.contains("PLACEHOLDER") && fallbackKey.isNotBlank()) {
                        val url = if (!customUrl.isNullOrBlank() && !customUrl.contains("eddie-proxy") && !customUrl.contains("geminiaitools")) {
                            customUrl
                        } else {
                            "https://api.anthropic.com/v1/messages"
                        }
                        callClaude(sanitizedHistory, newPrompt, targetModel, systemText, fallbackKey, url)
                    } else {
                        // Route through proxy (geminiaitools.com)
                        val proxyUrl = if (!customUrl.isNullOrBlank()) customUrl else defaultProxy
                        callOpenAiCompatible(
                            history = sanitizedHistory,
                            newPrompt = newPrompt,
                            model = targetModel,
                            systemText = systemText,
                            apiKey = "proxy_active",
                            baseUrl = proxyUrl
                        )
                    }
                }
                "xai" -> {
                    val fallbackKey = getFallbackKey(activeKey, BuildConfig.XAI_API_KEY, "PLACEHOLDER_XAI_API_KEY")
                    val customUrl = (providerUrls["XAI"] ?: providerUrls["Xai"] ?: providerUrls["xai"])?.trim()?.ifBlank { null }
                    val targetModel = if (resolvedModel.isNotBlank() && resolvedModel != "openai" && resolvedModel != "grok-beta") resolvedModel else "grok-4.3"
                    if (fallbackKey != null && !fallbackKey.contains("PLACEHOLDER") && fallbackKey.isNotBlank()) {
                        val url = if (!customUrl.isNullOrBlank() && !customUrl.contains("eddie-proxy") && !customUrl.contains("geminiaitools")) {
                            customUrl
                        } else {
                            "https://api.x.ai/v1/chat/completions"
                        }
                        callOpenAiCompatible(
                            history = sanitizedHistory,
                            newPrompt = newPrompt,
                            model = targetModel,
                            systemText = systemText,
                            apiKey = fallbackKey,
                            baseUrl = url
                        )
                    } else {
                        // Route through proxy (geminiaitools.com)
                        val proxyUrl = if (!customUrl.isNullOrBlank()) customUrl else defaultProxy
                        callOpenAiCompatible(
                            history = sanitizedHistory,
                            newPrompt = newPrompt,
                            model = targetModel,
                            systemText = systemText,
                            apiKey = "proxy_active",
                            baseUrl = proxyUrl
                        )
                    }
                }
                else -> {
                    val proxyUrl = providerUrls[provider]?.trim()?.ifBlank { null } ?: defaultProxy
                    callOpenAiCompatible(
                        history = sanitizedHistory,
                        newPrompt = newPrompt,
                        model = if (resolvedModel.isNotBlank() && !resolvedModel.contains("gemini")) resolvedModel else "gpt-5-mini",
                        systemText = systemText,
                        apiKey = "proxy_active",
                        baseUrl = proxyUrl
                    )
                }
            }
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
                // Reasoning models (such as GPT-5.5, o1, o3-mini) strictly require temperature = 1.
                // Explicitly specifying 1 prevents proxy defaults (such as 0.7) from causing HTTP 400.
                put("temperature", 1)
                
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

            val clean = baseUrl.trim()
            val targetUrl = if (clean.endsWith("/v1/chat/completions") || clean.endsWith("/api/chat")) {
                clean
            } else if (clean.endsWith("/")) {
                "${clean}v1/chat/completions"
            } else {
                "$clean/v1/chat/completions"
            }

            val authKey = if (apiKey.isNotBlank() && apiKey != "proxy_active" && !apiKey.contains("PLACEHOLDER")) apiKey else "proxy_active"
            val requestBody = jsonBody.toString().toRequestBody("application/json".toMediaTypeOrNull())
            val request = Request.Builder()
                .url(targetUrl)
                .header("Authorization", "Bearer $authKey")
                .header("Content-Type", "application/json")
                .post(requestBody)
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return "HTTP Error ${response.code}: ${response.body?.string() ?: "Failed to connect to the API server."}"
                }
                val resStr = response.body?.string() ?: return "Empty response from API server."
                val responseJson = JSONObject(resStr)
                if (responseJson.has("choices")) {
                    val choices = responseJson.getJSONArray("choices")
                    if (choices.length() > 0) {
                        choices.getJSONObject(0).getJSONObject("message").getString("content")
                    } else {
                        "Empty choices returned from API server."
                    }
                } else if (responseJson.has("candidates")) {
                    val candidates = responseJson.optJSONArray("candidates")
                    val content = candidates?.optJSONObject(0)?.optJSONObject("content")
                    val parts = content?.optJSONArray("parts")
                    if (parts != null && parts.length() > 0) {
                        parts.getJSONObject(0).optString("text", "No response text generated.")
                    } else {
                        "Empty response from API server."
                    }
                } else if (responseJson.has("error")) {
                    val err = responseJson.optJSONObject("error")
                    val errMsg = err?.optString("message") ?: responseJson.optString("error")
                    "API Error: $errMsg"
                } else {
                    "Unexpected response: ${resStr.take(200)}"
                }
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

                if (responseJson.has("content")) {
                    val contentArr = responseJson.getJSONArray("content")
                    val textBuilder = StringBuilder()

                    for (i in 0 until contentArr.length()) {
                        val block = contentArr.getJSONObject(i)
                        val type = block.optString("type", "")
                        if (type == "text") {
                            val text = block.optString("text", "")
                            if (text.isNotBlank()) {
                                if (textBuilder.isNotEmpty()) textBuilder.append("\n\n")
                                textBuilder.append(text)
                            }
                        }
                    }

                    if (textBuilder.isNotEmpty()) {
                        textBuilder.toString()
                    } else if (contentArr.length() > 0) {
                        // Fallback: check first block text or toString
                        contentArr.getJSONObject(0).optString("text", "No text content returned.")
                    } else {
                        "Empty response content from Claude."
                    }
                } else if (responseJson.has("error")) {
                    val err = responseJson.optJSONObject("error")
                    val msg = err?.optString("message") ?: responseJson.optString("error")
                    "Anthropic API Error: $msg"
                } else {
                    "Unexpected Claude response: ${resStr.take(200)}"
                }
            }
        } catch (e: Exception) {
            "Failed to make Claude API request: ${e.localizedMessage}"
        }
    }

    suspend fun sendSimpleRequest(
        prompt: String,
        systemText: String,
        provider: String = "OpenAI",
        model: String = "gpt-4o-mini",
        providerKeys: Map<String, String> = emptyMap(),
        providerUrls: Map<String, String> = emptyMap()
    ): String {
        return withContext(Dispatchers.IO) {
            val activeKey = providerKeys[provider]?.trim() ?: ""
            val defaultProxy = "https://geminiaitools.com/"
            when (provider) {
                "OpenAI" -> {
                    val fallbackKey = getFallbackKey(activeKey, BuildConfig.OPENAI_API_KEY, "PLACEHOLDER_OPENAI_API_KEY")
                    val customUrl = providerUrls["OpenAI"]?.trim()?.ifBlank { null }
                    val targetModel = when (model) {
                        "gpt5.5", "gpt-5.5" -> "gpt-5.5"
                        "openai", "" -> "gpt-5-mini"
                        else -> model
                    }
                    if (fallbackKey != null) {
                        val url = if (!customUrl.isNullOrBlank() && !customUrl.contains("eddie-proxy") && !customUrl.contains("geminiaitools")) {
                            customUrl
                        } else {
                            "https://api.openai.com/v1/chat/completions"
                        }
                        callOpenAiCompatible(
                            history = emptyList(),
                            newPrompt = prompt,
                            model = targetModel,
                            systemText = systemText,
                            apiKey = fallbackKey,
                            baseUrl = url
                        )
                    } else {
                        val proxyUrl = if (!customUrl.isNullOrBlank()) customUrl else defaultProxy
                        callOpenAiCompatible(
                            history = emptyList(),
                            newPrompt = prompt,
                            model = targetModel,
                            systemText = systemText,
                            apiKey = "proxy_active",
                            baseUrl = proxyUrl
                        )
                    }
                }
                else -> {
                    val fallbackKey = getFallbackKey(activeKey, BuildConfig.OPENAI_API_KEY, "PLACEHOLDER_OPENAI_API_KEY")
                    val customUrl = providerUrls[provider]?.trim()?.ifBlank { null }
                    if (fallbackKey != null) {
                        val url = if (!customUrl.isNullOrBlank() && !customUrl.contains("eddie-proxy") && !customUrl.contains("geminiaitools")) {
                            customUrl
                        } else {
                            "https://api.openai.com/v1/chat/completions"
                        }
                        callOpenAiCompatible(
                            history = emptyList(),
                            newPrompt = prompt,
                            model = if (model.isBlank()) "gpt-5-mini" else model,
                            systemText = systemText,
                            apiKey = fallbackKey,
                            baseUrl = url
                        )
                    } else {
                        val proxyUrl = if (!customUrl.isNullOrBlank()) customUrl else defaultProxy
                        callOpenAiCompatible(
                            history = emptyList(),
                            newPrompt = prompt,
                            model = if (model.isBlank()) "gpt-5-mini" else model,
                            systemText = systemText,
                            apiKey = "proxy_active",
                            baseUrl = proxyUrl
                        )
                    }
                }
            }
        }
    }

    suspend fun sendVisionRequest(
        bitmap: android.graphics.Bitmap,
        prompt: String,
        provider: String = "OpenAI",
        model: String = "gpt-4o-mini",
        providerKeys: Map<String, String> = emptyMap(),
        providerUrls: Map<String, String> = emptyMap()
    ): String {
        return withContext(Dispatchers.IO) {
            // Convert Bitmap to Base64
            val outputStream = java.io.ByteArrayOutputStream()
            bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 80, outputStream)
            val base64Data = android.util.Base64.encodeToString(outputStream.toByteArray(), android.util.Base64.NO_WRAP)

            val activeKey = providerKeys["OpenAI"]?.trim() ?: ""
            val keyToUse = getFallbackKey(activeKey, BuildConfig.OPENAI_API_KEY, "PLACEHOLDER_OPENAI_API_KEY")
            if (keyToUse == null) {
                return@withContext "API Key OpenAI is missing. Please set your OpenAI API Key in Settings."
            }
            val baseUrl = providerUrls["OpenAI"]?.trim()?.ifBlank { null } ?: "https://api.openai.com/v1/chat/completions"
            val resolvedModel = if (model.isBlank()) "gpt-4o-mini" else model

            try {
                val jsonBody = JSONObject().apply {
                    put("model", resolvedModel)
                    val messagesArray = JSONArray().apply {
                        put(JSONObject().apply {
                            put("role", "system")
                            put("content", "You are an expert AI Vision & Image Analyzer. Analyze images thoroughly and answer accurately.")
                        })
                        put(JSONObject().apply {
                            put("role", "user")
                            val contentArray = JSONArray().apply {
                                put(JSONObject().apply {
                                    put("type", "text")
                                    put("text", prompt)
                                })
                                put(JSONObject().apply {
                                    put("type", "image_url")
                                    put("image_url", JSONObject().apply {
                                        put("url", "data:image/jpeg;base64,$base64Data")
                                    })
                                })
                            }
                            put("content", contentArray)
                        })
                    }
                    put("messages", messagesArray)
                }

                val requestBody = jsonBody.toString().toRequestBody("application/json".toMediaTypeOrNull())
                val request = Request.Builder()
                    .url(baseUrl)
                    .header("Authorization", "Bearer $keyToUse")
                    .header("Content-Type", "application/json")
                    .post(requestBody)
                    .build()

                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        return@withContext "HTTP Error ${response.code}: ${response.body?.string() ?: "Failed to connect to OpenAI API."}"
                    }
                    val resStr = response.body?.string() ?: return@withContext "Empty response from OpenAI."
                    val responseJson = JSONObject(resStr)
                    responseJson.getJSONArray("choices")
                        .getJSONObject(0)
                        .getJSONObject("message")
                        .getString("content")
                }
            } catch (e: Exception) {
                "Error OpenAI Vision API: ${e.localizedMessage}"
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

    suspend fun generateOpenAiVideo(
        prompt: String,
        model: String,
        aspectRatio: String = "16:9",
        resolution: String = "1080p",
        duration: String = "5s",
        style: String = "",
        customApiKey: String? = null,
        customBaseUrl: String? = null,
        webhookUrl: String? = null,
        onLogMessage: (String) -> Unit = {}
    ): String {
        return withContext(Dispatchers.IO) {
            val keyToUse = if (!customApiKey.isNullOrBlank()) customApiKey.trim() else BuildConfig.OPENAI_API_KEY
            if (keyToUse.isBlank() || keyToUse.contains("PLACEHOLDER")) {
                throw IllegalStateException("OpenAI API key is not configured. Please add your OpenAI API key in the API Key Settings.")
            }

            var cleanBaseUrl = (customBaseUrl?.trim()?.ifBlank { null } ?: "https://api.openai.com/v1/").trim()
            
            // Clean common suffix misconfigurations from the base URL input
            val suffixesToRemove = listOf(
                "chat/completions",
                "videos/generations",
                "video/generations",
                "video/generation",
                "video/generate",
                "videos/generate",
                "images/generations",
                "image/generations",
                "videos",
                "video"
            )
            for (suffix in suffixesToRemove) {
                val isShortSuffix = suffix == "videos" || suffix == "video"
                if (cleanBaseUrl.endsWith(suffix, ignoreCase = true)) {
                    if (!isShortSuffix || cleanBaseUrl.endsWith("/$suffix", ignoreCase = true)) {
                        cleanBaseUrl = cleanBaseUrl.substring(0, cleanBaseUrl.length - suffix.length).trim()
                    }
                }
                val suffixWithSlash = "$suffix/"
                if (cleanBaseUrl.endsWith(suffixWithSlash, ignoreCase = true)) {
                    if (!isShortSuffix || cleanBaseUrl.endsWith("/$suffixWithSlash", ignoreCase = true)) {
                        cleanBaseUrl = cleanBaseUrl.substring(0, cleanBaseUrl.length - suffixWithSlash.length).trim()
                    }
                }
            }
            if (!cleanBaseUrl.endsWith("/")) {
                cleanBaseUrl = "$cleanBaseUrl/"
            }

            // Auto-fallback helper for raw openai domain without /v1/
            if (cleanBaseUrl.contains("api.openai.com", ignoreCase = true) && !cleanBaseUrl.contains("/v1/", ignoreCase = true)) {
                cleanBaseUrl = "https://api.openai.com/v1/"
            }

            val actualModelName = when (model.trim().lowercase()) {
                "sora 2", "sora-2" -> "sora-2"
                "sora 2 pro", "sora-2-pro", "sora 2 (25 tokens)", "sora 2 pro (125 tokens)" -> "sora-2-pro"
                else -> model.trim()
            }

            val secondsParam = duration.filter { it.isDigit() }.ifBlank { "8" }

            val styleDescription = when (style.trim().lowercase()) {
                "realistic drone" -> "shot from a commercial drone, high-altitude aerial view, sweeping pan, incredibly realistic landscape textures, 4k resolution, cinematic lighting"
                "cinematic 3d" -> "cinematic 3D scene, highly detailed CGI, volumetric lighting, photorealistic textures, dramatic depth of field, Octane render style"
                "cyberpunk neon" -> "cyberpunk retrofuturistic style, glowing neon lights, rain-slicked city streets, high contrast dark atmosphere, purple and cyan color grading, ultra-detailed textures"
                "anime epic" -> "epic anime cinematic visual style, gorgeous hand-drawn aesthetics, vibrant illustrative colors, dynamic backlighting, highly expressive movement, high-quality anime studio production"
                "vintage kodachrome 35mm" -> "shot on classic 35mm Kodachrome film, rich warm vintage colors, subtle film grain, nostalgic 1970s aesthetic, realistic soft analog lighting"
                "imax sci-fi" -> "ultra-wide IMAX cinematography, grand science fiction scale, majestic futuristic machinery, dramatic cosmic lighting, deep shadows, stellar VFX quality"
                "steampunk brass" -> "steampunk aesthetic, rich brass and copper details, atmospheric steam and smoke, intricate clockwork gears, warm amber light, Victorian era futuristic machinery"
                "fantasy unreal engine 5" -> "epic dark fantasy scene rendered in Unreal Engine 5, hyper-detailed ruins, ambient magical glow, realistic foliage, volumetric fog, lifelike textures, path tracing"
                "moody film noir" -> "film noir style, high-contrast monochrome black and white, dramatic chiascuro lighting, dark urban shadows, moody atmospheric fog, detective cinema aesthetic"
                "claymation stop-motion" -> "claymation, detailed stop-motion puppet style, clay texture, slightly choppy hand-crafted organic motion, playful and charmful miniatures"
                "water color aesthetic" -> "gorgeous watercolor art style, hand-painted textures, soft bleeding colors on textured paper, elegant fluid brush strokes, peaceful and artistic visual feel"
                "cosmic retrowave" -> "outrun retrowave synthwave aesthetic, horizontal laser grid, neon wireframe mountains, glowing wireframe wire, futuristic retro sunset, magenta and purple glowing colors"
                else -> if (style.isNotBlank()) "style of $style" else ""
            }
            val finalPrompt = if (styleDescription.isNotBlank()) {
                "$prompt, $styleDescription"
            } else {
                prompt
            }

            val resolvedSize = when {
                aspectRatio.trim().contains("x") -> aspectRatio.trim()
                aspectRatio.trim() == "16:9" -> "1024x576"
                aspectRatio.trim() == "9:16" -> "576x1024"
                aspectRatio.trim() == "21:9" -> "1024x440"
                aspectRatio.trim() == "1:1" -> "1024x1024"
                else -> "1024x576"
            }

            val resolvedUrl = "${cleanBaseUrl}videos"
            val postBody = JSONObject().apply {
                put("model", actualModelName)
                put("prompt", finalPrompt)
                put("seconds", secondsParam)
                put("size", resolvedSize)
                if (!webhookUrl.isNullOrBlank()) {
                    put("webhook", webhookUrl.trim())
                    put("webhook_url", webhookUrl.trim())
                }
            }.toString()
            
            onLogMessage("> Starting generation...\n> Sending POST request to: $resolvedUrl\n")

            val requestBody = postBody.toRequestBody("application/json".toMediaTypeOrNull())
            val request = Request.Builder()
                .url(resolvedUrl)
                .header("Authorization", "Bearer $keyToUse")
                .header("Content-Type", "application/json")
                .post(requestBody)
                .build()

            var videoId = ""
            var status = ""
            
            client.newCall(request).execute().use { response ->
                val bodyStr = response.body?.string() ?: ""
                if (!response.isSuccessful) {
                    throw IllegalStateException("Start generation failed (HTTP ${response.code}): $bodyStr")
                }
                
                val responseJson = JSONObject(bodyStr)
                videoId = responseJson.optString("id")
                    .ifBlank { responseJson.optString("task_id") }
                    .ifBlank { responseJson.optString("job_id") } ?: ""
                status = responseJson.optString("status")
                    .ifBlank { responseJson.optString("state") } ?: ""
                
                if (videoId.isBlank()) {
                    throw IllegalStateException("API did not return a valid video ID/job ID. Response: $bodyStr")
                }
                
                onLogMessage("> Start generation response received.\n> Saved videoId = \"$videoId\"\n> Initial Status: $status\n")
            }

            if (!webhookUrl.isNullOrBlank()) {
                onLogMessage("> Webhook [$webhookUrl] is registered. Bypassing local polling.\n")
                return@withContext "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4?webhook=registered&job_id=$videoId"
            }

            onLogMessage("> Generating video...\n> This may take 1-5 minutes.\n")

            var completed = false
            var elapsedSeconds = 0
            val maxWaitSeconds = 600 // 10 minutes timeout
            val pollUrl = "${cleanBaseUrl}videos/$videoId"
            
            while (!completed && elapsedSeconds < maxWaitSeconds) {
                delay(10000L) // Poll every 10 seconds as requested
                elapsedSeconds += 10
                
                onLogMessage("> Polling status: GET /v1/videos/$videoId (elapsed: ${elapsedSeconds}s)...\n")
                
                val pollRequest = Request.Builder()
                    .url(pollUrl)
                    .header("Authorization", "Bearer $keyToUse")
                    .header("Content-Type", "application/json")
                    .get()
                    .build()
                
                try {
                    client.newCall(pollRequest).execute().use { pollResponse ->
                        val pollBody = pollResponse.body?.string() ?: ""
                        if (pollResponse.isSuccessful) {
                            val pollJson = JSONObject(pollBody)
                            status = pollJson.optString("status")
                                .ifBlank { pollJson.optString("state") }
                                .lowercase().trim()
                            
                            onLogMessage("> Response: status is \"$status\"\n")
                            
                            if (status == "completed" || status == "succeeded" || status == "success") {
                                completed = true
                            } else if (status == "failed" || status == "error") {
                                throw IllegalStateException("Video generation failed at API side: $pollBody")
                            }
                        } else {
                            onLogMessage("> Poll HTTP failure (${pollResponse.code}), will retry...\n")
                        }
                    }
                } catch (e: Exception) {
                    if (e is IllegalStateException) throw e
                    onLogMessage("> Transient polling error: ${e.message}. Retrying...\n")
                }
            }
            
            if (!completed) {
                throw IllegalStateException("Video generation timed out after 600 seconds.")
            }

            val contentUrl = "${cleanBaseUrl}videos/$videoId/content"
            onLogMessage("> Video completed! Calling content endpoint: GET /v1/videos/$videoId/content\n")
            onLogMessage("> Downloading MP4...\n")
            
            val contentRequest = Request.Builder()
                .url(contentUrl)
                .header("Authorization", "Bearer $keyToUse")
                .get()
                .build()
                
            client.newCall(contentRequest).execute().use { contentResponse ->
                if (!contentResponse.isSuccessful) {
                    throw IllegalStateException("Failed to download video content. HTTP ${contentResponse.code}")
                }
                
                val contentType = contentResponse.header("Content-Type") ?: ""
                val bodyBytes = contentResponse.body?.bytes()
                if (bodyBytes == null || bodyBytes.isEmpty()) {
                    throw IllegalStateException("Content endpoint returned empty response body.")
                }
                
                var finalBytes = bodyBytes
                if (contentType.contains("application/json") || (bodyBytes.size < 2000 && String(bodyBytes).trim().startsWith("{"))) {
                    try {
                        val bodyStr = String(bodyBytes)
                        val contentJson = JSONObject(bodyStr)
                        val actualUrl = contentJson.optString("url")
                            .ifBlank { contentJson.optString("download_url") }
                            .ifBlank { contentJson.optString("video_url") }
                        if (!actualUrl.isNullOrBlank()) {
                            onLogMessage("> Nested download URL detected: $actualUrl\n")
                            val secondaryRequest = Request.Builder().url(actualUrl).get().build()
                            client.newCall(secondaryRequest).execute().use { secRes ->
                                if (secRes.isSuccessful) {
                                    finalBytes = secRes.body?.bytes() ?: bodyBytes
                                }
                            }
                        }
                    } catch (e: Exception) {
                        // ignore and use original bodyBytes
                    }
                }
                
                val cacheDir = com.example.AppContextProvider.context.cacheDir
                val outputFile = java.io.File(cacheDir, "sora_$videoId.mp4")
                java.io.FileOutputStream(outputFile).use { fos ->
                    fos.write(finalBytes)
                }
                
                onLogMessage("> Download completed successfully! Saved as local MP4.\n")
                return@withContext "file://${outputFile.absolutePath}"
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
                
                // If the response yielded immediate image data, return it
                val imagesArray = responseJson.optJSONArray("images")
                if (imagesArray != null && imagesArray.length() > 0) {
                    return@withContext imagesArray.getJSONObject(0).getString("url")
                }

                // If immediate results are not present but queue details are provided, poll the queue status_url
                val statusUrl = responseJson.optString("status_url")
                val responseUrl = responseJson.optString("response_url")
                
                if (!statusUrl.isNullOrBlank() && !responseUrl.isNullOrBlank()) {
                    var completed = false
                    var attempts = 0
                    val maxAttempts = 60 // Wait up to 60 seconds
                    
                    while (!completed && attempts < maxAttempts) {
                        delay(1000)
                        attempts++
                        
                        val statusRequest = Request.Builder()
                            .url(statusUrl)
                            .header("Authorization", "Key $keyToUse")
                            .get()
                            .build()
                        
                        client.newCall(statusRequest).execute().use { statusResponse ->
                            if (statusResponse.isSuccessful) {
                                val statusBody = statusResponse.body?.string() ?: ""
                                val statusJson = JSONObject(statusBody)
                                val status = statusJson.optString("status")
                                if (status == "COMPLETED") {
                                    completed = true
                                } else if (status == "FAILED") {
                                    throw IllegalStateException("Fal.ai Queue Job Failed: $statusBody")
                                }
                            }
                        }
                    }
                    
                    if (completed) {
                        val resultRequest = Request.Builder()
                            .url(responseUrl)
                            .header("Authorization", "Key $keyToUse")
                            .get()
                            .build()
                        
                        client.newCall(resultRequest).execute().use { resultResponse ->
                            val resultBody = resultResponse.body?.string() ?: ""
                            if (!resultResponse.isSuccessful) {
                                throw IllegalStateException("Failed to fetch final Fal.ai results: $resultBody")
                            }
                            val resultJson = JSONObject(resultBody)
                            val finalImages = resultJson.optJSONArray("images")
                            if (finalImages != null && finalImages.length() > 0) {
                                return@withContext finalImages.getJSONObject(0).getString("url")
                            } else {
                                throw IllegalStateException("No images found in completed Fal.ai response: $resultBody")
                            }
                        }
                    } else {
                        throw IllegalStateException("Fal.ai Generation timed out after 60 seconds.")
                    }
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
            val modelLower = model.lowercase()
            val endpointPath = when {
                modelLower.startsWith("sd3") || modelLower.contains("stable") -> "sd3"
                modelLower == "ultra" -> "ultra"
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
                val stabilityModel = when {
                    modelLower.contains("sd3-turbo") -> "sd3-turbo"
                    modelLower.contains("sd3-medium") -> "sd3-medium"
                    modelLower.contains("sd3-large-turbo") -> "sd3-large-turbo"
                    modelLower.contains("sd3-large") -> "sd3-large"
                    modelLower.contains("sd3.5-large-turbo") -> "sd3.5-large-turbo"
                    modelLower.contains("sd3.5-large") -> "sd3.5-large"
                    modelLower.contains("sd3.5-medium") -> "sd3.5-medium"
                    else -> "sd3-large"
                }
                builder.addFormDataPart("model", stabilityModel)
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

    suspend fun downloadFile(urlStr: String, destinationFile: java.io.File): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val request = Request.Builder()
                    .url(urlStr)
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
                    .build()
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        android.util.Log.e("ChatRepository", "downloadFile failed: HTTP ${response.code}")
                        return@withContext false
                    }
                    val body = response.body ?: return@withContext false
                    destinationFile.parentFile?.mkdirs()
                    destinationFile.outputStream().use { output ->
                        body.byteStream().use { input ->
                            input.copyTo(output)
                        }
                    }
                    true
                }
            } catch (e: Exception) {
                android.util.Log.e("ChatRepository", "downloadFile Exception: ${e.message}", e)
                false
            }
        }
    }

    private fun resolveImageToDataUrlOrKeep(imageUrl: String): String {
        if (imageUrl.startsWith("content://") || imageUrl.startsWith("file://")) {
            try {
                val context = com.example.AppContextProvider.context
                val uri = android.net.Uri.parse(imageUrl)
                val mimeType = context.contentResolver.getType(uri) ?: "image/jpeg"
                context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    val bytes = inputStream.readBytes()
                    val base64Str = android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP)
                    return "data:$mimeType;base64,$base64Str"
                }
            } catch (e: Exception) {
                android.util.Log.e("ChatRepository", "resolveImageToDataUrlOrKeep Error", e)
            }
        }
        return imageUrl
    }

    suspend fun generateFalVideo(
        prompt: String,
        imageUrl: String?,
        mode: String,
        aspectRatio: String,
        duration: String,
        model: String,
        resolution: String = "720p",
        generateAudio: Boolean = true,
        customApiKey: String? = null,
        negativePrompt: String = "blur, distort, and low quality",
        cfgScale: Float = 0.5f,
        shotType: String = "customize",
        multiPromptJson: String? = null,
        seed: Int? = null,
        enhancePrompt: Boolean = true,
        autoFix: Boolean = true,
        safetyTolerance: Int = 4,
        endImageUrl: String? = null,
        elementsJson: String? = null,
        voiceIds: List<String>? = null,
        startImageUrl: String? = null,
        bitrateMode: String = "standard",
        endUserId: String = "aistudio-user-default",
        pixverseMultiClip: Boolean = false,
        pixverseThinkingType: String = "auto",
        style: String = "",
        lumaLoop: Boolean = false,
        lumaHdr: Boolean = false,
        lumaExrExport: Boolean = false,
        soraDeleteVideo: Boolean = true,
        soraModelSnapshot: String = "sora-2",
        soraDetectAndBlockIp: Boolean = false,
        soraCharacterIds: List<String>? = null,
        alibabaAudioUrl: String? = null,
        alibabaMultiShots: Boolean = true,
        alibabaVideoUrl: String? = null,
        hunyuanInferenceSteps: Int = 28,
        hunyuanFrames: Int = 121,
        customBaseUrl: String? = null,
        onLogMessage: (String) -> Unit = {}
    ): String {
        return withContext(Dispatchers.IO) {
            val isProxy = !customBaseUrl.isNullOrBlank() && (customBaseUrl.contains("api/video") || customBaseUrl.contains("aitools") || customBaseUrl.contains("vercel") || customBaseUrl.contains("workers.dev"))
            val resolvedProxyUrl = if (isProxy) {
                val p = customBaseUrl!!.trim()
                if (p.endsWith("/api/video")) p
                else if (p.endsWith("/")) "${p}api/video"
                else "$p/api/video"
            } else null

            val keyToUse = if (!customApiKey.isNullOrBlank()) customApiKey.trim() else BuildConfig.FAL_API_KEY
            if (resolvedProxyUrl == null && (keyToUse.isBlank() || keyToUse.contains("PLACEHOLDER"))) {
                throw IllegalStateException("Fal.ai API key is not configured. Please add your FAL_API_KEY in the Secrets panel or configure a Video Proxy in Settings.")
            }
            val effectiveKey = if (resolvedProxyUrl != null && (keyToUse.isBlank() || keyToUse.contains("PLACEHOLDER"))) "proxy_active" else keyToUse

            val actualModel = if (model.lowercase().trim().startsWith("fal-ai/")) {
                model.trim()
            } else {
                when (model.lowercase().trim()) {
                    "veo2" -> "fal-ai/veo2"
                    "veo3" -> "fal-ai/veo3"
                    "veo3-fast" -> "fal-ai/veo3/fast"
                    "veo3.1" -> "fal-ai/veo3.1"
                    "veed" -> "fal-ai/veed"
                    "kling" -> "fal-ai/kling"
                    "kling-video" -> "fal-ai/kling-video"
                    "luma-dream-machine" -> "fal-ai/luma-dream-machine"
                    "luma/agent/ray/v3.2/text-to-video" -> "fal-ai/luma/agent/ray/v3.2/text-to-video"
                    "luma/agent/ray/v3.2/image-to-video" -> "fal-ai/luma/agent/ray/v3.2/image-to-video"
                    "minimax" -> "fal-ai/minimax"
                    "haiper" -> "fal-ai/haiper"
                    "xai/grok-imagine-video/text-to-video" -> "fal-ai/xai/grok-imagine-video/text-to-video"
                    "xai/grok-imagine-video/image-to-video" -> "fal-ai/xai/grok-imagine-video/image-to-video"
                    "klingv21" -> "fal-ai/kling/v2.1/image-to-video"
                    "klingimage" -> "fal-ai/kling/v1.6/image-to-video"
                    "kling v2.6 standard" -> "fal-ai/kling/v2.6/standard"
                    "kling v2.6 pro" -> "fal-ai/kling-video/v2.6/pro/text-to-video"
                    "kling v3 standard" -> "fal-ai/kling-video/v3/standard/text-to-video"
                    "kling v3 pro" -> "fal-ai/kling-video/v3/pro/text-to-video"
                    "fal-ai/kling-video/o3/4k/text-to-video" -> "fal-ai/kling-video/o3/4k/text-to-video"
                    "fal-ai/kling-video/o3/standard/image-to-video" -> "fal-ai/kling-video/o3/standard/image-to-video"
                    "fal-ai/kling-video/o3/pro/image-to-video" -> "fal-ai/kling-video/o3/pro/image-to-video"
                    "fal-ai/kling-video/v3/4k/text-to-video" -> "fal-ai/kling-video/v3/4k/text-to-video"
                    "fal-ai/kling-video/v3/4k/image-to-video" -> "fal-ai/kling-video/v3/4k/image-to-video"
                    "kling-video/v3/turbo/pro/text-to-video" -> "fal-ai/kling-video/v3/turbo/pro/text-to-video"
                    "kling-video/v3/turbo/standard/text-to-video" -> "fal-ai/kling-video/v3/turbo/standard/text-to-video"
                    "kling-2.5-turbo/pro/text-to-video" -> "fal-ai/kling-video/v2.5-turbo/pro/text-to-video"
                    "kling-2.5-turbo/standard/image-to-video" -> "fal-ai/kling-2.5-turbo/standard/image-to-video"
                    "sora 2" -> "fal-ai/sora-2"
                    "sora 2 pro" -> "fal-ai/sora-2-pro"
                    else -> {
                        if (model.contains("/")) "fal-ai/$model" else "fal-ai/veo3.1/lite"
                    }
                }
            }

            if (actualModel.startsWith("fal-ai/veo3") || actualModel.contains("veo3")) {
                val client = FalVeo3Client()
                return@withContext client.generateVideoAsync(
                    prompt = prompt,
                    imageUrl = imageUrl,
                    mode = mode,
                    aspectRatio = aspectRatio,
                    duration = duration,
                    resolution = resolution,
                    generateAudio = generateAudio,
                    model = actualModel,
                    customApiKey = customApiKey,
                    seed = seed,
                    autoFix = autoFix,
                    enhancePrompt = enhancePrompt,
                    negativePrompt = negativePrompt,
                    safetyTolerance = safetyTolerance,
                    proxyUrl = resolvedProxyUrl,
                    onLog = onLogMessage
                )
            }

            if (actualModel == "fal-ai/veo2" || actualModel.contains("veo2")) {
                val client = FalVeo2QueueClient()
                return@withContext client.generateVideoAsync(
                    prompt = prompt,
                    imageUrl = imageUrl,
                    mode = mode,
                    aspectRatio = aspectRatio,
                    duration = duration,
                    customApiKey = customApiKey,
                    seed = seed,
                    enhancePrompt = enhancePrompt,
                    autoFix = autoFix,
                    negativePrompt = negativePrompt,
                    proxyUrl = resolvedProxyUrl,
                    onLogMessage = onLogMessage
                )
            }

            if (actualModel.contains("kling")) {
                val client = FalKlingQueueClient()
                return@withContext client.generateVideoAsync(
                    prompt = prompt,
                    imageUrl = imageUrl,
                    mode = mode,
                    aspectRatio = aspectRatio,
                    duration = duration,
                    modelId = actualModel,
                    customApiKey = customApiKey,
                    negativePrompt = negativePrompt,
                    cfgScale = cfgScale,
                    generateAudio = generateAudio,
                    shotType = shotType,
                    multiPromptJson = multiPromptJson,
                    endImageUrl = endImageUrl,
                    elementsJson = elementsJson,
                    voiceIds = voiceIds,
                    startImageUrl = startImageUrl,
                    proxyUrl = resolvedProxyUrl,
                    onLogMessage = onLogMessage
                )
            }

            val isImageToVideo = mode == "Image to Video" && (!imageUrl.isNullOrBlank() || !alibabaVideoUrl.isNullOrBlank())
            val cleanedModel = if (actualModel.startsWith("fal-ai/")) {
                actualModel.removePrefix("fal-ai/")
            } else {
                actualModel
            }
            var endpointUrl = if (actualModel.startsWith("fal-ai/")) {
                "https://queue.fal.run/$actualModel"
            } else {
                "https://queue.fal.run/$cleanedModel"
            }
            if (actualModel.lowercase().trim().contains("happy-horse")) {
                endpointUrl = if (isImageToVideo) "https://queue.fal.run/alibaba/happy-horse/image-to-video" else "https://queue.fal.run/alibaba/happy-horse/text-to-video"
            } else if (actualModel.lowercase().trim().contains("wan/v2.7")) {
                endpointUrl = if (isImageToVideo) "https://queue.fal.run/fal-ai/wan/v2.7/image-to-video" else "https://queue.fal.run/fal-ai/wan/v2.7/text-to-video"
            } else if (actualModel.lowercase().trim().contains("wan/v2.6")) {
                endpointUrl = if (isImageToVideo) "https://queue.fal.run/fal-ai/wan/v2.6/image-to-video" else "https://queue.fal.run/fal-ai/wan/v2.6/text-to-video"
            } else if (actualModel.lowercase().trim().contains("wan/v2.5") || actualModel.lowercase().trim().contains("wan-25-preview")) {
                endpointUrl = if (isImageToVideo) "https://queue.fal.run/fal-ai/wan-25-preview/image-to-video" else "https://queue.fal.run/fal-ai/wan-25-preview/text-to-video"
            } else if (actualModel.lowercase().trim().contains("sora-2-pro")) {
                endpointUrl = if (isImageToVideo) "https://queue.fal.run/fal-ai/sora-2/image-to-video/pro" else "https://queue.fal.run/fal-ai/sora-2/text-to-video/pro"
            } else if (actualModel.lowercase().trim().contains("sora-2")) {
                endpointUrl = if (isImageToVideo) "https://queue.fal.run/fal-ai/sora-2/image-to-video" else "https://queue.fal.run/fal-ai/sora-2/text-to-video"
            }
            if (actualModel.contains("pixverse")) {
                if (isImageToVideo) {
                    if (actualModel == "fal-ai/pixverse/v6/image-to-video" || cleanedModel == "pixverse/v6/image-to-video") {
                        endpointUrl = "https://queue.fal.run/fal-ai/pixverse/v6/image-to-video"
                    } else if (actualModel == "fal-ai/pixverse/v5.6/image-to-video" || cleanedModel == "pixverse/v5.6/image-to-video") {
                        endpointUrl = "https://queue.fal.run/fal-ai/pixverse/v5.6/image-to-video"
                    } else if (actualModel == "fal-ai/pixverse/c1/image-to-video" || cleanedModel == "pixverse/c1/image-to-video") {
                        endpointUrl = "https://queue.fal.run/fal-ai/pixverse/c1/image-to-video"
                    }
                } else {
                    if (actualModel == "fal-ai/pixverse/v6/text-to-video" || cleanedModel == "pixverse/v6/text-to-video") {
                        endpointUrl = "https://queue.fal.run/fal-ai/pixverse/v6/text-to-video"
                    } else if (actualModel == "fal-ai/pixverse/v5.6/text-to-video" || cleanedModel == "pixverse/v5.6/text-to-video") {
                        endpointUrl = "https://queue.fal.run/fal-ai/pixverse/v5.6/text-to-video"
                    } else if (actualModel == "fal-ai/pixverse/v5.5/text-to-video" || cleanedModel == "pixverse/v5.5/text-to-video") {
                        endpointUrl = "https://queue.fal.run/fal-ai/pixverse/v5.5/text-to-video"
                    }
                }
            }

            onLogMessage("> Preparing FalAI request body...\n")
            
            val isHunyuan15 = actualModel.contains("hunyuan-video-v1.5")
            val isByteDance = actualModel.contains("bytedance")
            val isPixVerse = actualModel.contains("pixverse")
            val isGrokImagine = actualModel.contains("grok-imagine-video")
            val isLumaRay = actualModel.contains("luma/agent/ray") || actualModel.contains("luma-dream-machine")
            val isAlibaba = actualModel.contains("wan") || actualModel.contains("happy-horse")
            val isSora2 = actualModel.lowercase().trim().contains("sora-2")
            val isMinimaxHailuo = actualModel.contains("minimax/hailuo-02") || actualModel.contains("minimax/hailuo-2.3")
            
            val jsonBody = JSONObject().apply {
                if (isHunyuan15) {
                    put("prompt", prompt)
                    put("negative_prompt", negativePrompt)
                    put("num_inference_steps", hunyuanInferenceSteps)
                    if (seed != null) {
                        put("seed", seed)
                    }
                    val aspectToUse = when (aspectRatio) {
                        "16:9", "9:16" -> aspectRatio
                        else -> "16:9"
                    }
                    put("aspect_ratio", aspectToUse)
                    val resToUse = if (resolution.isNotBlank()) resolution else "480p"
                    put("resolution", resToUse)
                    put("num_frames", hunyuanFrames)
                    put("enable_prompt_expansion", enhancePrompt)
                    if (isImageToVideo) {
                        if (!imageUrl.isNullOrBlank()) {
                            onLogMessage("> Resolving reference image to web-compatible format...\n")
                            val resolvedImg = resolveImageToDataUrlOrKeep(imageUrl)
                            onLogMessage("> Image size/format resolved successfully! Binding to payload.\n")
                            put("image_url", resolvedImg)
                        }
                    }
                } else if (isMinimaxHailuo) {
                    put("prompt", prompt)
                    
                    if (!imageUrl.isNullOrBlank()) {
                        onLogMessage("> Resolving reference image to web-compatible format...\n")
                        val resolvedImg = resolveImageToDataUrlOrKeep(imageUrl)
                        onLogMessage("> Image size/format resolved successfully! Binding to payload.\n")
                        put("image_url", resolvedImg)
                    }
                    
                    put("prompt_optimizer", enhancePrompt)
                    
                    if (actualModel == "fal-ai/minimax/hailuo-2.3/standard/image-to-video" || actualModel == "fal-ai/minimax/hailuo-2.3/standard/text-to-video") {
                        val durClean = duration.replace("s", "").replace(" seconds", "").trim()
                        val durVal = durClean.toIntOrNull() ?: 6
                        put("duration", durVal)
                    } else if (actualModel == "fal-ai/minimax/hailuo-02/standard/text-to-video" || actualModel == "fal-ai/minimax/hailuo-02/pro/text-to-video") {
                        val durClean = duration.replace("s", "").replace(" seconds", "").trim()
                        val durVal = durClean.toIntOrNull() ?: 6
                        put("duration", durVal)
                    } else if (!actualModel.contains("/pro/")) {
                        val durClean = duration.replace("s", "").replace(" seconds", "").trim()
                        val durVal = durClean.toIntOrNull() ?: 6
                        put("duration", durVal)
                        
                        val resToUse = if (resolution.uppercase().trim() == "512P") "512P" else "768P"
                        put("resolution", resToUse)
                    }
                    
                    if (!actualModel.contains("hailuo-2.3") && actualModel != "fal-ai/minimax/hailuo-02/standard/text-to-video" && actualModel != "fal-ai/minimax/hailuo-02/pro/text-to-video" && !endImageUrl.isNullOrBlank()) {
                        onLogMessage("> Resolving reference end image to web-compatible format...\n")
                        val resolvedEndImg = resolveImageToDataUrlOrKeep(endImageUrl)
                        put("end_image_url", resolvedEndImg)
                    }
                } else if (isSora2) {
                    put("prompt", prompt)
                    
                    val aspectToUse = when (aspectRatio) {
                        "9:16", "16:9", "auto" -> aspectRatio
                        else -> "auto"
                    }
                    put("aspect_ratio", aspectToUse)
                    
                    val resToUse = when (resolution) {
                        "720p", "auto" -> resolution
                        "1080p", "true_1080p" -> {
                            if (actualModel.lowercase().trim().contains("sora-2-pro")) resolution else "auto"
                        }
                        else -> "auto"
                    }
                    put("resolution", resToUse)
                    
                    val durClean = duration.replace("s", "").replace(" seconds", "").trim()
                    val durToUse = when (durClean) {
                        "4", "8", "12", "16", "20" -> durClean
                        else -> "4"
                    }
                    put("duration", durToUse)
                    
                    put("delete_video", soraDeleteVideo)
                    
                    if (!actualModel.lowercase().trim().contains("sora-2-pro")) {
                        put("model", soraModelSnapshot)
                    }
                    
                    put("detect_and_block_ip", soraDetectAndBlockIp)
                    
                    val charIdsArray = org.json.JSONArray()
                    soraCharacterIds?.forEach { charIdsArray.put(it) }
                    put("character_ids", charIdsArray)
                    
                    if (isImageToVideo) {
                        onLogMessage("> Resolving reference image to web-compatible format...\n")
                        val resolvedImg = resolveImageToDataUrlOrKeep(imageUrl!!)
                        onLogMessage("> Image size/format resolved successfully! Binding to payload.\n")
                        put("image_url", resolvedImg)
                    }
                } else if (isGrokImagine) {
                    put("prompt", prompt)
                    
                    val durClean = duration.replace("s", "").replace(" seconds", "").trim()
                    val durVal = durClean.toIntOrNull() ?: 6
                    put("duration", durVal)
                    
                    val resToUse = when (resolution) {
                        "480p", "720p" -> resolution
                        else -> "720p"
                    }
                    put("resolution", resToUse)
                    
                    val aspectToUse = when (aspectRatio) {
                        "16:9", "4:3", "3:2", "1:1", "2:3", "3:4", "9:16" -> aspectRatio
                        else -> "16:9"
                    }
                    put("aspect_ratio", aspectToUse)
                    
                    if (isImageToVideo) {
                        onLogMessage("> Resolving reference image to web-compatible format...\n")
                        val resolvedImg = resolveImageToDataUrlOrKeep(imageUrl!!)
                        onLogMessage("> Image size/format resolved successfully! Binding to payload.\n")
                        put("image_url", resolvedImg)
                    }
                } else if (isAlibaba) {
                    put("prompt", prompt)
                    val durClean = duration.replace("s", "").replace(" seconds", "").trim()
                    if (actualModel.contains("happy-horse")) {
                        val durVal = durClean.toIntOrNull() ?: 5
                        put("duration", durVal)
                        
                        if (!isImageToVideo) {
                            val aspectToUse = when (aspectRatio) {
                                "16:9", "9:16", "1:1", "4:3", "3:4" -> aspectRatio
                                else -> "16:9"
                            }
                            put("aspect_ratio", aspectToUse)
                        }
                        
                        val resToUse = when (resolution) {
                            "720p", "1080p" -> resolution
                            else -> "1080p"
                        }
                        put("resolution", resToUse)
                        
                        if (seed != null) {
                            put("seed", seed)
                        }
                        put("enable_safety_checker", autoFix)
                    } else if (actualModel.contains("wan/v2.7")) {
                        val durVal = durClean.toIntOrNull() ?: 5
                        put("duration", durVal)
                        
                        val aspectToUse = when (aspectRatio) {
                            "16:9", "9:16", "1:1", "4:3", "3:4" -> aspectRatio
                            else -> "16:9"
                        }
                        put("aspect_ratio", aspectToUse)
                        
                        val resToUse = when (resolution) {
                            "720p", "1080p" -> resolution
                            else -> "1080p"
                        }
                        put("resolution", resToUse)
                        
                        put("negative_prompt", negativePrompt)
                        put("enable_prompt_expansion", enhancePrompt)
                        if (seed != null) {
                            put("seed", seed)
                        }
                        put("enable_safety_checker", autoFix)
                        if (!alibabaAudioUrl.isNullOrBlank()) {
                            put("audio_url", alibabaAudioUrl)
                        }
                    } else if (actualModel.contains("wan/v2.6")) {
                        val durVal = durClean.toIntOrNull() ?: 5
                        put("duration", durVal)
                        
                        if (!isImageToVideo) {
                            val aspectToUse = when (aspectRatio) {
                                "16:9", "9:16", "1:1", "4:3", "3:4" -> aspectRatio
                                else -> "16:9"
                            }
                            put("aspect_ratio", aspectToUse)
                        }
                        
                        val resToUse = when (resolution) {
                            "720p", "1080p" -> resolution
                            else -> "1080p"
                        }
                        put("resolution", resToUse)
                        
                        put("negative_prompt", negativePrompt)
                        put("enable_prompt_expansion", enhancePrompt)
                        put("multi_shots", alibabaMultiShots)
                        if (seed != null) {
                            put("seed", seed)
                        }
                        put("enable_safety_checker", autoFix)
                        if (!alibabaAudioUrl.isNullOrBlank()) {
                            put("audio_url", alibabaAudioUrl)
                        }
                    } else if (actualModel.contains("wan/v2.5") || actualModel.contains("wan-25-preview")) {
                        val durVal = durClean.toIntOrNull() ?: 5
                        put("duration", durVal)
                        
                        val aspectToUse = when (aspectRatio) {
                            "16:9", "9:16", "1:1" -> aspectRatio
                            else -> "16:9"
                        }
                        put("aspect_ratio", aspectToUse)
                        
                        val resToUse = when (resolution) {
                            "480p", "720p", "1080p" -> resolution
                            else -> "720p"
                        }
                        put("resolution", resToUse)
                        
                        put("negative_prompt", negativePrompt)
                        put("enable_prompt_expansion", enhancePrompt)
                        if (seed != null) {
                            put("seed", seed)
                        }
                        put("enable_safety_checker", autoFix)
                        if (!alibabaAudioUrl.isNullOrBlank()) {
                            put("audio_url", alibabaAudioUrl)
                        }
                    } else {
                        val durationStr = if (durClean == "10") "10s" else "5s"
                        put("duration", durationStr)
                        val aspectToUse = when (aspectRatio) {
                            "16:9", "9:16", "1:1", "4:3", "3:4", "21:9" -> aspectRatio
                            else -> "16:9"
                        }
                        put("aspect_ratio", aspectToUse)
                    }
                    
                    if (isImageToVideo) {
                        if (actualModel.contains("wan/v2.7")) {
                            if (!alibabaVideoUrl.isNullOrBlank()) {
                                put("video_url", alibabaVideoUrl)
                            } else if (!imageUrl.isNullOrBlank()) {
                                onLogMessage("> Resolving reference image to web-compatible format...\n")
                                val resolvedImg = resolveImageToDataUrlOrKeep(imageUrl)
                                onLogMessage("> Image size/format resolved successfully! Binding to payload.\n")
                                put("image_url", resolvedImg)
                            }
                            if (!endImageUrl.isNullOrBlank()) {
                                onLogMessage("> Resolving end image to web-compatible format...\n")
                                val resolvedEndImg = resolveImageToDataUrlOrKeep(endImageUrl)
                                onLogMessage("> End image resolved successfully! Binding to payload.\n")
                                put("end_image_url", resolvedEndImg)
                            }
                        } else {
                            onLogMessage("> Resolving reference image to web-compatible format...\n")
                            val resolvedImg = resolveImageToDataUrlOrKeep(imageUrl!!)
                            onLogMessage("> Image size/format resolved successfully! Binding to payload.\n")
                            put("image_url", resolvedImg)
                        }
                    }
                } else if (isLumaRay) {
                    put("prompt", prompt)
                    
                    val aspectToUse = when (aspectRatio) {
                        "3:4", "4:3", "1:1", "9:16", "16:9", "21:9" -> aspectRatio
                        else -> "16:9"
                    }
                    put("aspect_ratio", aspectToUse)
                    
                    val resToUse = when (resolution) {
                        "540p", "720p", "1080p" -> resolution
                        else -> "540p"
                    }
                    put("resolution", resToUse)
                    
                    val durClean = duration.replace("s", "").replace(" seconds", "").trim()
                    val durToUse = if (durClean == "10" || durClean == "10s") "10s" else "5s"
                    put("duration", durToUse)
                    
                    put("loop", lumaLoop)
                    put("hdr", lumaHdr)
                    put("exr_export", lumaExrExport)
                    
                    if (isImageToVideo) {
                        onLogMessage("> Resolving reference image to web-compatible format...\n")
                        val resolvedImg = resolveImageToDataUrlOrKeep(imageUrl!!)
                        onLogMessage("> Image size/format resolved successfully! Binding to payload.\n")
                        put("image_url", resolvedImg)
                        
                        if (!endImageUrl.isNullOrBlank()) {
                            onLogMessage("> Resolving reference end image to web-compatible format...\n")
                            val resolvedEndImg = resolveImageToDataUrlOrKeep(endImageUrl)
                            onLogMessage("> End image resolved successfully! Binding to payload as end_image_url.\n")
                            put("end_image_url", resolvedEndImg)
                        }
                    }
                } else if (isByteDance) {
                    put("prompt", prompt)
                    
                    val resVal = if (resolution.isNotBlank()) resolution else "720p"
                    put("resolution", resVal)
                    
                    val durClean = duration.replace("s", "").replace(" seconds", "").trim()
                    val durVal = if (durClean == "auto" || durClean.toIntOrNull() in 4..15) {
                        durClean
                    } else {
                        "auto"
                    }
                    put("duration", durVal)
                    
                    val aspectVal = if (aspectRatio.isBlank()) "auto" else aspectRatio
                    put("aspect_ratio", aspectVal)
                    
                    put("generate_audio", generateAudio)
                    put("bitrate_mode", bitrateMode)
                    put("end_user_id", endUserId)
                    
                    if (isImageToVideo) {
                        onLogMessage("> Resolving reference image to web-compatible format...\n")
                        val resolvedImg = resolveImageToDataUrlOrKeep(imageUrl!!)
                        onLogMessage("> Image size/format resolved successfully! Binding to payload.\n")
                        put("image_url", resolvedImg)
                    }
                } else if (isPixVerse) {
                    if (actualModel.contains("/v6/") || actualModel == "fal-ai/pixverse/v5.5/text-to-video" || actualModel == "fal-ai/pixverse/v5.6/image-to-video" || actualModel == "fal-ai/pixverse/v5.6/text-to-video" || actualModel == "fal-ai/pixverse/c1/image-to-video") {
                        put("prompt", prompt)
                        if (actualModel != "fal-ai/pixverse/v5.6/image-to-video" && actualModel != "fal-ai/pixverse/v6/image-to-video" && actualModel != "fal-ai/pixverse/c1/image-to-video") {
                            val aspectToUse = when (aspectRatio) {
                                "16:9", "4:3", "1:1", "3:4", "9:16", "2:3", "3:2", "21:9" -> aspectRatio
                                else -> "16:9"
                            }
                            put("aspect_ratio", aspectToUse)
                        }
                        
                        val resToUse = when (resolution) {
                            "360p", "540p", "720p", "1080p" -> resolution
                            else -> "720p"
                        }
                        put("resolution", resToUse)
                        
                        val durClean = duration.replace("s", "").replace(" seconds", "").trim()
                        val durVal = durClean.toIntOrNull() ?: 5
                        put("duration", durVal)
                        
                        if (actualModel != "fal-ai/pixverse/c1/image-to-video") {
                            put("negative_prompt", negativePrompt)
                            
                            val styleClean = style.lowercase().trim().replace(" ", "_")
                            val validStyles = listOf("anime", "3d_animation", "clay", "comic", "cyberpunk")
                            if (styleClean in validStyles) {
                                put("style", styleClean)
                            }
                            
                            put("generate_multi_clip_switch", pixverseMultiClip)
                            put("thinking_type", pixverseThinkingType)
                        }
                        
                        if (seed != null) {
                            put("seed", seed)
                        }
                        
                        put("generate_audio_switch", generateAudio)

                        if (isImageToVideo) {
                            onLogMessage("> Resolving reference image for PixVerse...\n")
                            val resolvedImg = resolveImageToDataUrlOrKeep(imageUrl!!)
                            onLogMessage("> Image size/format resolved successfully! Binding to PixVerse payload.\n")
                            put("image_url", resolvedImg)
                        }
                    } else {
                        put("prompt", prompt)
                        val aspectToUse = when (aspectRatio) {
                            "16:9", "9:16", "1:1", "4:3", "3:4", "21:9" -> aspectRatio
                            else -> "16:9"
                        }
                        put("aspect_ratio", aspectToUse)
                        
                        val durClean = duration.replace("s", "").replace(" seconds", "").trim()
                        val durationStr = if (durClean.contains("8") || durClean.contains("12")) "8s" else "5s"
                        put("duration", durationStr)
                        
                        if (isImageToVideo) {
                            onLogMessage("> Resolving reference image for PixVerse...\n")
                            val resolvedImg = resolveImageToDataUrlOrKeep(imageUrl!!)
                            onLogMessage("> Image size/format resolved successfully! Binding to PixVerse payload.\n")
                            put("image_url", resolvedImg)
                        }
                    }

                } else {
                    val durationSecs = if (duration.contains("8")) "8s" else "5s"
                    val aspectToUse = when (aspectRatio) {
                        "16:9", "9:16", "1:1", "4:3", "3:4" -> aspectRatio
                        else -> "16:9"
                    }
                    put("prompt", prompt)
                    put("duration", durationSecs)
                    put("aspect_ratio", aspectToUse)
                    
                    if (isImageToVideo) {
                        onLogMessage("> Resolving reference image to web-compatible format...\n")
                        val resolvedImg = resolveImageToDataUrlOrKeep(imageUrl!!)
                        onLogMessage("> Image size/format resolved successfully! Binding to payload.\n")
                        put("image_url", resolvedImg)
                    }
                }
            }

            val targetSubmitUrl = resolvedProxyUrl ?: endpointUrl
            if (resolvedProxyUrl != null) {
                jsonBody.put("model", actualModel)
            }

            onLogMessage("> Connecting to video pipeline ($targetSubmitUrl)...\n")
            val requestBody = jsonBody.toString().toRequestBody("application/json".toMediaTypeOrNull())
            
            val request = Request.Builder()
                .url(targetSubmitUrl)
                .header("Authorization", "Key $effectiveKey")
                .header("Content-Type", "application/json")
                .post(requestBody)
                .build()

            client.newCall(request).execute().use { response ->
                val bodyStr = response.body?.string() ?: ""
                if (!response.isSuccessful) {
                    throw IllegalStateException("Video API Error ${response.code}: $bodyStr")
                }
                val responseJson = JSONObject(bodyStr)
                
                val statusUrl = responseJson.optString("status_url")
                val responseUrl = responseJson.optString("response_url")
                
                if (!statusUrl.isNullOrBlank() && !responseUrl.isNullOrBlank()) {
                    onLogMessage("> Submission successful! Request ID: ${responseJson.optString("request_id")}\n")
                    onLogMessage("> Active queue position: ${responseJson.optInt("queue_position", 0)}\n")
                    onLogMessage("> Starting client-side polling...\n")
                    
                    var completed = false
                    var attempts = 0
                    val maxAttempts = 300 // Wait up to 5 minutes
                    
                    while (!completed && attempts < maxAttempts) {
                        delay(2000) // Poll every 2 seconds to not spam
                        attempts++
                        
                        val pollStatusUrl = if (resolvedProxyUrl != null) {
                            "$resolvedProxyUrl?statusUrl=" + java.net.URLEncoder.encode(statusUrl, "UTF-8")
                        } else {
                            statusUrl
                        }

                        val statusRequest = Request.Builder()
                            .url(pollStatusUrl)
                            .header("Authorization", "Key $effectiveKey")
                            .get()
                            .build()
                        
                        client.newCall(statusRequest).execute().use { statusResponse ->
                            if (statusResponse.isSuccessful) {
                                val statusBody = statusResponse.body?.string() ?: ""
                                val statusJson = JSONObject(statusBody)
                                val status = statusJson.optString("status")
                                val queuePosition = statusJson.optInt("queue_position", -1)
                                
                                val posLog = if (queuePosition >= 0) " (Queue position: $queuePosition)" else ""
                                onLogMessage("> Generation Status: $status$posLog\n")
                                
                                if (status == "COMPLETED") {
                                    completed = true
                                } else if (status == "FAILED") {
                                    throw IllegalStateException("Video Queue Job Failed: $statusBody")
                                }
                            } else {
                                onLogMessage("> Warning: status polling returned HTTP ${statusResponse.code}. Retrying...\n")
                            }
                        }
                    }
                    
                    if (completed) {
                        onLogMessage("> Fetching completed video outputs...\n")
                        val pollResultUrl = if (resolvedProxyUrl != null) {
                            "$resolvedProxyUrl?statusUrl=" + java.net.URLEncoder.encode(responseUrl, "UTF-8")
                        } else {
                            responseUrl
                        }

                        val resultRequest = Request.Builder()
                            .url(pollResultUrl)
                            .header("Authorization", "Key $effectiveKey")
                            .get()
                            .build()
                        
                        client.newCall(resultRequest).execute().use { resultResponse ->
                            val resultBody = resultResponse.body?.string() ?: ""
                            if (!resultResponse.isSuccessful) {
                                throw IllegalStateException("Failed to fetch final Fal.ai results: $resultBody")
                            }
                            val resultJson = JSONObject(resultBody)
                            
                            // Let's locate the generated video URL
                            val videoObj = resultJson.optJSONObject("video")
                            if (videoObj != null) {
                                val url = videoObj.optString("url")
                                if (!url.isNullOrBlank()) {
                                    return@withContext url
                                }
                            }
                            
                            // Let's check "video_url" or "url" keys directly
                            val directUrl = resultJson.optString("video_url").ifBlank { resultJson.optString("url") }
                            if (!directUrl.isNullOrBlank()) {
                                return@withContext directUrl
                            }
                            
                            throw IllegalStateException("No video URL found in completed Fal.ai response: $resultBody")
                        }
                    } else {
                        throw IllegalStateException("Fal.ai video generation timed out after 5 minutes.")
                    }
                } else {
                    // Try direct video output if sync mode was used (though we submit to async)
                    val videoObj = responseJson.optJSONObject("video")
                    if (videoObj != null) {
                        val url = videoObj.optString("url")
                        if (!url.isNullOrBlank()) {
                            return@withContext url
                        }
                    }
                    throw IllegalStateException("Failed to parse async status/response URLs from Fal.ai: $bodyStr")
                }
            }
        }
    }
}
