package com.example.data

import com.example.data.BackendProxyClient.withProxyAuth
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

    suspend fun testProxyConnectivity(baseUrl: String = BackendProxyClient.DEFAULT_PROXY_ROOT): Pair<Boolean, String> {
        return withContext(Dispatchers.IO) {
            try {
                val rootUrl = BackendProxyClient.normalizeProxyRoot(baseUrl)
                val firebaseToken = BackendProxyClient.getFirebaseIdToken()

                // 1. First test root health check
                try {
                    val rootRequest = Request.Builder()
                        .url(rootUrl)
                        .withProxyAuth("Bearer", firebaseToken)
                        .get()
                        .build()
                    client.newCall(rootRequest).execute().use { rootResp ->
                        if (rootResp.isSuccessful) {
                            val body = rootResp.body?.string() ?: ""
                            return@withContext Pair(true, "Proxy server is ONLINE! (HTTP ${rootResp.code}) $body")
                        }
                    }
                } catch (_: Exception) {}

                // 2. Test chat endpoint
                val targetUrl = BackendProxyClient.resolveEndpoint(baseUrl, "/api/chat")

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
                    .withProxyAuth("Bearer", firebaseToken)
                    .post(jsonBody.toString().toRequestBody("application/json".toMediaTypeOrNull()))
                    .build()

                client.newCall(request).execute().use { response ->
                    val body = response.body?.string() ?: ""
                    if (response.isSuccessful && !body.contains("API key not valid", ignoreCase = true)) {
                        Pair(true, "Proxy server is ONLINE and functional! (HTTP ${response.code})")
                    } else if (body.contains("API key not valid", ignoreCase = true) || body.contains("API_KEY_INVALID", ignoreCase = true)) {
                        Pair(true, "Proxy Reachable (HTTP ${response.code}), but upstream API key on proxy server needs attention.")
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
        baseUrl: String = BackendProxyClient.DEFAULT_PROXY_ROOT,
        params: List<String>
    ): Result<Map<String, Map<String, String>>> {
        return withContext(Dispatchers.IO) {
            try {
                val rootUrl = BackendProxyClient.normalizeProxyRoot(baseUrl)
                val firebaseToken = BackendProxyClient.getFirebaseIdToken()
                val resultMap = mutableMapOf<String, Map<String, String>>()

                for (param in params) {
                    val encodedModel = java.net.URLEncoder.encode(param, "UTF-8")
                    val targetUrl = "$rootUrl/api/keys?model=$encodedModel"
                    val request = Request.Builder()
                        .url(targetUrl)
                        .header("User-Agent", "ChatAssistant-App/1.0")
                        .withProxyAuth("Bearer", firebaseToken)
                        .get()
                        .build()

                    client.newCall(request).execute().use { response ->
                        if (response.isSuccessful) {
                            val bodyStr = response.body?.string() ?: ""
                            val json = JSONObject(bodyStr)
                            val targetModel = json.optString("targetModel", "").trim()
                            val modelId = json.optString("modelId", "").trim()

                            resultMap[param] = mapOf(
                                "targetModel" to targetModel,
                                "modelId" to modelId
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

    data class TopUpPackage(
        val id: String,
        val name: String,
        val tokens: Int,
        val price: String,
        val description: String,
        val badge: String? = null
    )

    private fun parseTopUpPackageObject(obj: JSONObject, index: Int): TopUpPackage? {
        val id = obj.optString("id", "pack_${index + 1}").trim().ifBlank { "pack_${index + 1}" }
        val directName = obj.optString("name", obj.optString("title", "")).trim()
        val directTokens = obj.optInt("tokens", obj.optInt("amount", 0))
        val directPrice = obj.optString("price", obj.optString("priceLabel", "")).trim()
        val directDesc = obj.optString("description", obj.optString("desc", "")).trim()
        val directBadge = obj.optString("badge", "").trim().takeIf {
            it.isNotBlank() && !it.equals("null", ignoreCase = true)
        }
        if (directName.isNotBlank() && directTokens > 0 && directPrice.isNotBlank()) {
            return TopUpPackage(
                id = id,
                name = directName,
                tokens = directTokens,
                price = directPrice,
                description = directDesc.ifBlank { "+$directTokens tokens untuk chat & studio AI" },
                badge = directBadge
            )
        }

        // Also support targetModel / modelId pipe format: "Starter Pack|250|$0.99|Deskripsi|POPULAR"
        val packed = obj.optString("targetModel", obj.optString("modelId", "")).trim()
        if (packed.contains("|")) {
            val parts = packed.split("|").map { it.trim() }
            if (parts.size >= 3) {
                val pName = parts[0]
                val pTokens = parts[1].filter { it.isDigit() }.toIntOrNull() ?: 0
                val pPrice = parts[2]
                val pDesc = parts.getOrNull(3).orEmpty().ifBlank { "+$pTokens tokens untuk chat & studio AI" }
                val pBadge = parts.getOrNull(4)?.takeIf { it.isNotBlank() && !it.equals("null", ignoreCase = true) }
                if (pName.isNotBlank() && pTokens > 0 && pPrice.isNotBlank()) {
                    return TopUpPackage(
                        id = id,
                        name = pName,
                        tokens = pTokens,
                        price = pPrice,
                        description = pDesc,
                        badge = pBadge
                    )
                }
            }
        }
        return null
    }

    suspend fun fetchTopUpPackagesFromProxy(
        baseUrl: String = BackendProxyClient.DEFAULT_PROXY_ROOT
    ): Result<List<TopUpPackage>> {
        return withContext(Dispatchers.IO) {
            try {
                val rootUrl = BackendProxyClient.normalizeProxyRoot(baseUrl)
                val firebaseToken = BackendProxyClient.getFirebaseIdToken()
                val candidateUrls = listOf(
                    "$rootUrl/api/topup-packages",
                    "$rootUrl/api/keys?model=TOPUP_PACKAGES"
                )

                for (targetUrl in candidateUrls) {
                    try {
                        val request = Request.Builder()
                            .url(targetUrl)
                            .header("User-Agent", "ChatAssistant-App/1.0")
                            .withProxyAuth("Bearer", firebaseToken)
                            .get()
                            .build()

                        client.newCall(request).execute().use { response ->
                            if (!response.isSuccessful) return@use
                            val bodyStr = response.body?.string().orEmpty().trim()
                            if (bodyStr.isBlank()) return@use

                            val packagesArray = when {
                                bodyStr.startsWith("[") -> JSONArray(bodyStr)
                                bodyStr.startsWith("{") -> {
                                    val json = JSONObject(bodyStr)
                                    json.optJSONArray("packages")
                                        ?: json.optJSONArray("data")
                                        ?: json.optJSONArray("items")
                                        ?: json.optJSONArray("topupPackages")
                                }
                                else -> null
                            }

                            if (packagesArray != null && packagesArray.length() > 0) {
                                val parsed = mutableListOf<TopUpPackage>()
                                for (i in 0 until packagesArray.length()) {
                                    val obj = packagesArray.optJSONObject(i) ?: continue
                                    parseTopUpPackageObject(obj, i)?.let { parsed.add(it) }
                                }
                                if (parsed.isNotEmpty()) {
                                    return@withContext Result.success(parsed.take(5))
                                }
                            }
                        }
                    } catch (_: Exception) {
                        // Try next candidate URL
                    }
                }

                // Fallback: query TOPUP_PACK_1..5 individually via /api/keys?model=
                val queriedPackages = mutableListOf<TopUpPackage>()
                for (i in 0 until 5) {
                    val param = "TOPUP_PACK_${i + 1}"
                    val targetUrl = "$rootUrl/api/keys?model=$param"
                    try {
                        val request = Request.Builder()
                            .url(targetUrl)
                            .header("User-Agent", "ChatAssistant-App/1.0")
                            .withProxyAuth("Bearer", firebaseToken)
                            .get()
                            .build()

                        client.newCall(request).execute().use { response ->
                            if (!response.isSuccessful) return@use
                            val bodyStr = response.body?.string().orEmpty().trim()
                            if (!bodyStr.startsWith("{")) return@use
                            val json = JSONObject(bodyStr)
                            parseTopUpPackageObject(json, i)?.let { pkg ->
                                queriedPackages.add(pkg)
                            }
                        }
                    } catch (_: Exception) {
                        // Continue querying remaining packs
                    }
                }

                if (queriedPackages.isNotEmpty()) {
                    Result.success(queriedPackages.take(5))
                } else {
                    Result.failure(Exception("Paket top up dari proxy belum tersedia."))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    private fun extractElevenLabsKeyFromJson(bodyStr: String): String? {
        if (bodyStr.isBlank()) return null
        val trimmed = bodyStr.trim()
        if (!trimmed.startsWith("{")) return null
        return runCatching {
            val json = JSONObject(trimmed)
            val candidateKeys = listOf(
                "ELEVENLABS_API_KEY",
                "elevenlabs_api_key",
                "elevenLabsApiKey",
                "apiKey",
                "api_key",
                "key",
                "value",
                "secret",
                "token"
            )
            for (field in candidateKeys) {
                val value = json.optString(field, "").trim()
                if (value.isNotBlank() && value != "proxy_active" && !value.equals("null", ignoreCase = true)) {
                    return@runCatching value
                }
            }
            for (nestedName in listOf("keys", "data", "result", "config")) {
                val nested = json.optJSONObject(nestedName) ?: continue
                for (field in candidateKeys) {
                    val value = nested.optString(field, "").trim()
                    if (value.isNotBlank() && value != "proxy_active" && !value.equals("null", ignoreCase = true)) {
                        return@runCatching value
                    }
                }
            }
            null
        }.getOrNull()
    }

    suspend fun fetchElevenLabsApiKeyFromProxy(
        baseUrl: String = BackendProxyClient.DEFAULT_PROXY_ROOT
    ): String? {
        return withContext(Dispatchers.IO) {
            val rootUrl = BackendProxyClient.normalizeProxyRoot(baseUrl)
            val firebaseToken = BackendProxyClient.getFirebaseIdToken()
            val candidateUrls = listOf(
                "$rootUrl/api/keys?model=ELEVENLABS_API_KEY",
                "$rootUrl/api/keys?key=ELEVENLABS_API_KEY",
                "$rootUrl/api/keys"
            )

            for (targetUrl in candidateUrls) {
                try {
                    val request = Request.Builder()
                        .url(targetUrl)
                        .header("User-Agent", "ChatAssistant-App/1.0")
                        .withProxyAuth("Bearer", firebaseToken)
                        .get()
                        .build()

                    client.newCall(request).execute().use { response ->
                        if (response.isSuccessful) {
                            val bodyStr = response.body?.string().orEmpty()
                            val extracted = extractElevenLabsKeyFromJson(bodyStr)
                            if (!extracted.isNullOrBlank()) {
                                return@withContext extracted
                            }
                        }
                    }
                } catch (_: Exception) {
                    // Try next candidate endpoint
                }
            }
            null
        }
    }

    suspend fun fetchApiKeysFromProxy(baseUrl: String = BackendProxyClient.DEFAULT_PROXY_ROOT): Result<Map<String, String>> {
        val (connected, statusMessage) = testProxyConnectivity(baseUrl)
        return if (connected) {
            Result.success(emptyMap())
        } else {
            Result.failure(Exception(statusMessage))
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

            val resolvedModel = model.trim()

            // Sanitize conversation history to prevent models from seeing and mimicking 
            // system-level Token Usage or Pemakaian Token status notifications previously written to DB
            val sanitizedHistory = history.map { msg ->
                val idx = msg.text.indexOf("\n\n---\n*🪙")
                val cleanText = if (idx != -1) msg.text.substring(0, idx) else msg.text
                msg.copy(text = cleanText)
            }

            val normalizedProvider = provider.lowercase().trim()
            val customUrl = when (normalizedProvider) {
                "openai" -> providerUrls["OpenAI"] ?: providerUrls["openai"]
                "deepseek" -> providerUrls["DeepSeek"] ?: providerUrls["Deepseek"] ?: providerUrls["deepseek"]
                "claude", "anthropic" -> providerUrls["Claude"] ?: providerUrls["Anthropic"] ?: providerUrls["claude"] ?: providerUrls["anthropic"]
                "xai" -> providerUrls["XAI"] ?: providerUrls["Xai"] ?: providerUrls["xai"]
                else -> providerUrls[provider] ?: providerUrls[normalizedProvider]
            }?.trim()?.ifBlank { null }

            val targetModel = when (normalizedProvider) {
                "openai" -> when (resolvedModel.lowercase()) {
                    "openai", "" -> "gpt-4o-mini"
                    "gpt" -> "gpt-4o"
                    else -> resolvedModel
                }
                "deepseek" -> if (resolvedModel.isNotBlank() && !resolvedModel.equals("deepseek", ignoreCase = true)) resolvedModel else "deepseek-chat"
                "claude", "anthropic" -> if (resolvedModel.isNotBlank() && !resolvedModel.equals("claude", ignoreCase = true) && !resolvedModel.equals("anthropic", ignoreCase = true)) resolvedModel else "claude-3-7-sonnet-20250219"
                "xai" -> if (resolvedModel.isNotBlank() && !resolvedModel.equals("openai", ignoreCase = true) && !resolvedModel.equals("xai", ignoreCase = true)) resolvedModel else "grok-beta"
                else -> if (resolvedModel.isNotBlank() && !resolvedModel.contains("gemini")) resolvedModel else "gpt-4o-mini"
            }

            callOpenAiCompatible(
                history = sanitizedHistory,
                newPrompt = newPrompt,
                model = targetModel,
                systemText = systemText,
                baseUrl = BackendProxyClient.normalizeProxyRoot(customUrl),
                provider = normalizedProvider.ifBlank { "openai" }
            )
        }
    }

    private suspend fun callOpenAiCompatible(
        history: List<ChatMessage>,
        newPrompt: String,
        model: String,
        systemText: String,
        baseUrl: String,
        provider: String = "openai"
    ): String {
        return try {
            val firebaseToken = BackendProxyClient.getFirebaseIdToken()
            val jsonBody = JSONObject().apply {
                put("provider", provider)
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

            val clean = baseUrl.trim().trimEnd('/')
            val targetUrl = when {
                clean.endsWith("/v1/chat/completions", ignoreCase = true) -> clean
                clean.endsWith("/v1", ignoreCase = true) -> "$clean/chat/completions"
                else -> BackendProxyClient.resolveEndpoint(clean, "/api/chat")
            }

            val requestBody = jsonBody.toString().toRequestBody("application/json".toMediaTypeOrNull())
            val request = Request.Builder()
                .url(targetUrl)
                .withProxyAuth("Bearer", firebaseToken)
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
                    val directText = listOf("text", "reply", "response", "answer", "content", "output")
                        .firstNotNullOfOrNull { key ->
                            responseJson.optString(key, "").trim().takeIf { it.isNotBlank() }
                        }
                    directText ?: "Unexpected response: ${resStr.take(200)}"
                }
            }
        } catch (e: Exception) {
            "Failed to make API request: ${e.localizedMessage}"
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
            val normalizedProvider = provider.lowercase().trim()
            val customUrl = providerUrls[provider]?.trim()?.ifBlank { null }
                ?: providerUrls["OpenAI"]?.trim()?.ifBlank { null }
                ?: BackendProxyClient.DEFAULT_PROXY_ROOT
            val targetModel = when (model) {
                "gpt5.5", "gpt-5.5" -> "gpt-5.5"
                "openai", "" -> "gpt-5-mini"
                else -> model
            }
            callOpenAiCompatible(
                history = emptyList(),
                newPrompt = prompt,
                model = targetModel,
                systemText = systemText,
                baseUrl = BackendProxyClient.normalizeProxyRoot(customUrl),
                provider = normalizedProvider.ifBlank { "openai" }
            )
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

            val customUrl = (providerUrls[provider] ?: providerUrls["OpenAI"])?.trim().orEmpty().trimEnd('/')
            val resolvedUrl = when {
                customUrl.endsWith("/v1/chat/completions", ignoreCase = true) -> customUrl
                customUrl.endsWith("/v1", ignoreCase = true) -> "$customUrl/chat/completions"
                else -> BackendProxyClient.resolveEndpoint(customUrl.ifBlank { null }, "/api/chat")
            }
            val firebaseToken = BackendProxyClient.getFirebaseIdToken()
            val resolvedModel = if (model.isBlank()) "gpt-4o-mini" else model

            try {
                val jsonBody = JSONObject().apply {
                    put("provider", provider.lowercase().trim().ifBlank { "openai" })
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
                    .url(resolvedUrl)
                    .withProxyAuth("Bearer", firebaseToken)
                    .header("Content-Type", "application/json")
                    .post(requestBody)
                    .build()

                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        return@withContext "HTTP Error ${response.code}: ${response.body?.string() ?: "Failed to connect to proxy server."}"
                    }
                    val resStr = response.body?.string() ?: return@withContext "Empty response from proxy server."
                    val responseJson = JSONObject(resStr)
                    if (responseJson.has("choices")) {
                        val choices = responseJson.getJSONArray("choices")
                        if (choices.length() > 0) {
                            choices.getJSONObject(0).getJSONObject("message").getString("content")
                        } else {
                            "Empty choices returned from proxy server."
                        }
                    } else if (responseJson.has("candidates")) {
                        val parts = responseJson.optJSONArray("candidates")
                            ?.optJSONObject(0)
                            ?.optJSONObject("content")
                            ?.optJSONArray("parts")
                        if (parts != null && parts.length() > 0) {
                            parts.getJSONObject(0).optString("text", "No response text generated.")
                        } else {
                            "Empty response from proxy server."
                        }
                    } else if (responseJson.has("error")) {
                        val err = responseJson.optJSONObject("error")
                        val errMsg = err?.optString("message") ?: responseJson.optString("error")
                        "API Error: $errMsg"
                    } else {
                        listOf("text", "reply", "response", "answer", "content", "output")
                            .firstNotNullOfOrNull { key ->
                                responseJson.optString(key, "").trim().takeIf { it.isNotBlank() }
                            } ?: "Unexpected response: ${resStr.take(200)}"
                    }
                }
            } catch (e: Exception) {
                "Error Vision Proxy API: ${e.localizedMessage}"
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
            val firebaseToken = BackendProxyClient.getFirebaseIdToken()
            val resolvedUrl = BackendProxyClient.resolveEndpoint(customBaseUrl, "/v1/images/generations")
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
                put("provider", "openai")
                put("model", actualModelName)
                put("prompt", prompt)
                put("n", 1)
                put("size", resolvedSize)
            }

            val requestBody = jsonBody.toString().toRequestBody("application/json".toMediaTypeOrNull())

            val request = Request.Builder()
                .url(resolvedUrl)
                .withProxyAuth("Bearer", firebaseToken)
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
            val firebaseToken = BackendProxyClient.getFirebaseIdToken()
            val resolvedUrl = BackendProxyClient.resolveEndpoint(customBaseUrl, "/v1/videos")

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

            val postBody = JSONObject().apply {
                put("provider", "openai")
                put("model", actualModelName)
                put("prompt", finalPrompt)
                put("seconds", secondsParam)
                put("size", resolvedSize)
                if (!webhookUrl.isNullOrBlank()) {
                    put("webhook", webhookUrl.trim())
                    put("webhook_url", webhookUrl.trim())
                }
            }.toString()
            
            onLogMessage("> Starting generation via secure backend proxy...\n> Sending POST request to: $resolvedUrl\n")

            val requestBody = postBody.toRequestBody("application/json".toMediaTypeOrNull())
            val request = Request.Builder()
                .url(resolvedUrl)
                .withProxyAuth("Bearer", firebaseToken)
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
            val pollUrl = "$resolvedUrl/$videoId"
            
            while (!completed && elapsedSeconds < maxWaitSeconds) {
                delay(10000L) // Poll every 10 seconds as requested
                elapsedSeconds += 10
                
                onLogMessage("> Polling status: GET /v1/videos/$videoId (elapsed: ${elapsedSeconds}s)...\n")
                
                val pollRequest = Request.Builder()
                    .url(pollUrl)
                    .withProxyAuth("Bearer", firebaseToken)
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

            val contentUrl = "$resolvedUrl/$videoId/content"
            onLogMessage("> Video completed! Calling content endpoint: GET /v1/videos/$videoId/content\n")
            onLogMessage("> Downloading MP4...\n")
            
            val contentRequest = Request.Builder()
                .url(contentUrl)
                .withProxyAuth("Bearer", firebaseToken)
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
        endpointUrl: String = "fal-ai/flux/schnell",
        aspectRatio: String = "1:1",
        customApiKey: String? = null,
        customBaseUrl: String? = null
    ): String {
        return withContext(Dispatchers.IO) {
            val firebaseToken = BackendProxyClient.getFirebaseIdToken()
            val resolvedProxyUrl = BackendProxyClient.resolveEndpoint(customBaseUrl, "/api/image")

            val modelPath = endpointUrl.trim()
                .removePrefix("https://queue.fal.run/")
                .removePrefix("https://fal.run/")
                .substringBefore("?")
                .trim('/')
                .ifBlank { "fal-ai/flux/schnell" }

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
                put("provider", "fal")
                put("model", modelPath)
                put("prompt", prompt)
                put("image_size", resolvedImageSize)
                put("sync_mode", true)
            }

            val requestBody = jsonBody.toString().toRequestBody("application/json".toMediaTypeOrNull())

            val request = Request.Builder()
                .url(resolvedProxyUrl)
                .withProxyAuth("Key", firebaseToken)
                .header("Content-Type", "application/json")
                .post(requestBody)
                .build()

            client.newCall(request).execute().use { response ->
                val bodyStr = response.body?.string() ?: ""
                if (!response.isSuccessful) {
                    throw IllegalStateException("Fal.ai Proxy Error ${response.code}: $bodyStr")
                }
                val responseJson = JSONObject(bodyStr)

                // If the response yielded immediate image data, return it
                val imagesArray = responseJson.optJSONArray("images")
                if (imagesArray != null && imagesArray.length() > 0) {
                    return@withContext imagesArray.getJSONObject(0).getString("url")
                }
                val directUrl = responseJson.optString("url").ifBlank { responseJson.optString("image_url") }
                if (directUrl.isNotBlank()) {
                    return@withContext directUrl
                }

                // If immediate results are not present but queue details are provided, poll via backend proxy
                val statusUrl = responseJson.optString("status_url")
                val responseUrl = responseJson.optString("response_url")

                if (statusUrl.isNotBlank() && responseUrl.isNotBlank()) {
                    var completed = false
                    var attempts = 0
                    val maxAttempts = 60 // Wait up to 60 seconds

                    while (!completed && attempts < maxAttempts) {
                        delay(1000)
                        attempts++

                        val pollStatusUrl = "$resolvedProxyUrl?statusUrl=" + java.net.URLEncoder.encode(statusUrl, "UTF-8")
                        val statusRequest = Request.Builder()
                            .url(pollStatusUrl)
                            .withProxyAuth("Key", firebaseToken)
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
                        val pollResultUrl = "$resolvedProxyUrl?statusUrl=" + java.net.URLEncoder.encode(responseUrl, "UTF-8")
                        val resultRequest = Request.Builder()
                            .url(pollResultUrl)
                            .withProxyAuth("Key", firebaseToken)
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
        customApiKey: String? = null,
        customBaseUrl: String? = null
    ): String {
        return withContext(Dispatchers.IO) {
            val firebaseToken = BackendProxyClient.getFirebaseIdToken()
            val resolvedProxyUrl = BackendProxyClient.resolveEndpoint(customBaseUrl, "/api/image")

            // Resolve endpoint based on model selection
            val modelLower = model.lowercase()
            val endpointPath = when {
                modelLower.startsWith("sd3") || modelLower.contains("stable") -> "sd3"
                modelLower == "ultra" -> "ultra"
                else -> "core"
            }

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

            val stabilityModel = if (endpointPath == "sd3") {
                when {
                    modelLower.contains("sd3-turbo") -> "sd3-turbo"
                    modelLower.contains("sd3-medium") -> "sd3-medium"
                    modelLower.contains("sd3-large-turbo") -> "sd3-large-turbo"
                    modelLower.contains("sd3-large") -> "sd3-large"
                    modelLower.contains("sd3.5-large-turbo") -> "sd3.5-large-turbo"
                    modelLower.contains("sd3.5-large") -> "sd3.5-large"
                    modelLower.contains("sd3.5-medium") -> "sd3.5-medium"
                    else -> "sd3-large"
                }
            } else {
                endpointPath
            }

            val jsonBody = JSONObject().apply {
                put("provider", "stability")
                put("endpoint", endpointPath)
                put("model", stabilityModel)
                put("prompt", prompt)
                put("output_format", "png")
                put("aspect_ratio", resolvedAspectRatio)
                if (endpointPath == "sd3") {
                    put("mode", "text-to-image")
                }
            }

            val requestBody = jsonBody.toString().toRequestBody("application/json".toMediaTypeOrNull())

            val request = Request.Builder()
                .url(resolvedProxyUrl)
                .withProxyAuth("Bearer", firebaseToken)
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .post(requestBody)
                .build()

            client.newCall(request).execute().use { response ->
                val contentType = response.header("Content-Type").orEmpty().lowercase()
                if (!response.isSuccessful) {
                    val bodyStr = response.body?.string() ?: ""
                    throw IllegalStateException("Stability AI Proxy Error ${response.code}: $bodyStr")
                }

                if (contentType.startsWith("image/")) {
                    val bytes = response.body?.bytes()
                        ?: throw IllegalStateException("Stability AI returned an empty image body.")
                    val cacheDir = com.example.AppContextProvider.context.cacheDir
                    val file = java.io.File(cacheDir, "stability_img_${System.currentTimeMillis()}_${java.util.UUID.randomUUID()}.png")
                    java.io.FileOutputStream(file).use { out ->
                        out.write(bytes)
                    }
                    return@withContext "file://${file.absolutePath}"
                }

                val bodyStr = response.body?.string() ?: ""
                val responseJson = JSONObject(bodyStr)
                val base64ImageBytes = responseJson.optString("image", "")
                    .ifBlank { responseJson.optString("b64_json", "") }
                if (base64ImageBytes.isNotBlank()) {
                    val cleanB64 = base64ImageBytes.substringAfter("base64,", base64ImageBytes)
                    val bytes = android.util.Base64.decode(cleanB64, android.util.Base64.DEFAULT)
                    val cacheDir = com.example.AppContextProvider.context.cacheDir
                    val file = java.io.File(cacheDir, "stability_img_${System.currentTimeMillis()}_${java.util.UUID.randomUUID()}.png")
                    java.io.FileOutputStream(file).use { out ->
                        out.write(bytes)
                    }
                    return@withContext "file://${file.absolutePath}"
                }

                val directUrl = responseJson.optString("url")
                    .ifBlank { responseJson.optString("image_url") }
                    .ifBlank { responseJson.optJSONArray("images")?.optJSONObject(0)?.optString("url").orEmpty() }
                    .ifBlank { responseJson.optJSONArray("data")?.optJSONObject(0)?.optString("url").orEmpty() }
                if (directUrl.isNotBlank()) {
                    return@withContext directUrl
                }

                throw IllegalStateException("No image returned by Stability AI. Response: $bodyStr")
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
            val firebaseToken = BackendProxyClient.getFirebaseIdToken()
            val resolvedProxyUrl = BackendProxyClient.resolveEndpoint(customBaseUrl, "/api/video")

            val actualModel = if (model.lowercase().trim().startsWith("fal-ai/")) {
                model.trim()
            } else {
                when (model.lowercase().trim()) {
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
                    customApiKey = null,
                    seed = seed,
                    autoFix = autoFix,
                    enhancePrompt = enhancePrompt,
                    negativePrompt = negativePrompt,
                    safetyTolerance = safetyTolerance,
                    proxyUrl = resolvedProxyUrl,
                    onLog = onLogMessage
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
                    customApiKey = null,
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
            val resolvedFalModelId = when {
                actualModel.lowercase().trim().contains("happy-horse") ->
                    if (isImageToVideo) "alibaba/happy-horse/image-to-video" else "alibaba/happy-horse/text-to-video"
                actualModel.lowercase().trim().contains("wan/v2.7") ->
                    if (isImageToVideo) "fal-ai/wan/v2.7/image-to-video" else "fal-ai/wan/v2.7/text-to-video"
                actualModel.lowercase().trim().contains("wan/v2.6") ->
                    if (isImageToVideo) "fal-ai/wan/v2.6/image-to-video" else "fal-ai/wan/v2.6/text-to-video"
                actualModel.lowercase().trim().contains("wan/v2.5") || actualModel.lowercase().trim().contains("wan-25-preview") ->
                    if (isImageToVideo) "fal-ai/wan-25-preview/image-to-video" else "fal-ai/wan-25-preview/text-to-video"
                actualModel.lowercase().trim().contains("sora-2-pro") ->
                    if (isImageToVideo) "fal-ai/sora-2/image-to-video/pro" else "fal-ai/sora-2/text-to-video/pro"
                actualModel.lowercase().trim().contains("sora-2") ->
                    if (isImageToVideo) "fal-ai/sora-2/image-to-video" else "fal-ai/sora-2/text-to-video"
                actualModel.contains("pixverse") -> actualModel
                else -> actualModel
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

            jsonBody.put("provider", "fal")
            jsonBody.put("model", resolvedFalModelId)

            onLogMessage("> Connecting to secure video proxy ($resolvedProxyUrl) for model $resolvedFalModelId...\n")
            val requestBody = jsonBody.toString().toRequestBody("application/json".toMediaTypeOrNull())
            
            val request = Request.Builder()
                .url(resolvedProxyUrl)
                .withProxyAuth("Key", firebaseToken)
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
                
                if (statusUrl.isNotBlank() && responseUrl.isNotBlank()) {
                    onLogMessage("> Submission successful! Request ID: ${responseJson.optString("request_id")}\n")
                    onLogMessage("> Active queue position: ${responseJson.optInt("queue_position", 0)}\n")
                    onLogMessage("> Starting proxy-routed polling...\n")
                    
                    var completed = false
                    var attempts = 0
                    val maxAttempts = 300 // Wait up to 5 minutes
                    
                    while (!completed && attempts < maxAttempts) {
                        delay(2000) // Poll every 2 seconds to not spam
                        attempts++
                        
                        val pollStatusUrl = "$resolvedProxyUrl?statusUrl=" + java.net.URLEncoder.encode(statusUrl, "UTF-8")

                        val statusRequest = Request.Builder()
                            .url(pollStatusUrl)
                            .withProxyAuth("Key", firebaseToken)
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
                        val pollResultUrl = "$resolvedProxyUrl?statusUrl=" + java.net.URLEncoder.encode(responseUrl, "UTF-8")

                        val resultRequest = Request.Builder()
                            .url(pollResultUrl)
                            .withProxyAuth("Key", firebaseToken)
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
                                if (url.isNotBlank()) {
                                    return@withContext url
                                }
                            }
                            
                            // Let's check "video_url" or "url" keys directly
                            val directUrl = resultJson.optString("video_url").ifBlank { resultJson.optString("url") }
                            if (directUrl.isNotBlank()) {
                                return@withContext directUrl
                            }
                            
                            throw IllegalStateException("No video URL found in completed Fal.ai response: $resultBody")
                        }
                    } else {
                        throw IllegalStateException("Fal.ai video generation timed out after 5 minutes.")
                    }
                } else {
                    // Try direct video output if sync mode was used
                    val videoObj = responseJson.optJSONObject("video")
                    if (videoObj != null) {
                        val url = videoObj.optString("url")
                        if (url.isNotBlank()) {
                            return@withContext url
                        }
                    }
                    val directUrl = responseJson.optString("video_url").ifBlank { responseJson.optString("url") }
                    if (directUrl.isNotBlank()) {
                        return@withContext directUrl
                    }
                    throw IllegalStateException("Failed to parse async status/response URLs from Fal.ai: $bodyStr")
                }
            }
        }
    }

    suspend fun generateElevenLabsMusic(
        prompt: String,
        genre: String,
        duration: String,
        tempo: String,
        model: String = "music_v2_5",
        customApiKey: String? = null,
        customBaseUrl: String? = null,
        onLogMessage: (String) -> Unit = {}
    ): String {
        return withContext(Dispatchers.IO) {
            val rootProxyUrl = BackendProxyClient.normalizeProxyRoot(customBaseUrl)
            val firebaseToken = BackendProxyClient.getFirebaseIdToken()
            val resolvedProxyUrl = BackendProxyClient.resolveEndpoint(rootProxyUrl, "/api/music")

            val resolvedModelId = when (model.trim()) {
                "Eleven Music v1", "music_v1" -> "music_v1"
                "Eleven Music v2", "music_v2" -> "music_v2"
                "Eleven Music v2.5", "music_v2_5" -> "music_v2_5"
                else -> "music_v2_5"
            }

            val resolvedApiKey = customApiKey?.trim()?.takeIf { it.isNotBlank() && it != "proxy_active" }

            val parsedSeconds = duration.lowercase().replace("s", "").trim().toIntOrNull() ?: 15
            val musicLengthMs = (parsedSeconds * 1000).coerceIn(3000, 600000)

            val fullPrompt = buildString {
                append(prompt.trim())
                if (genre.isNotBlank()) append(". Genre: $genre")
                if (tempo.isNotBlank()) append(". Tempo: $tempo")
            }.take(4100)

            val useDirectElevenLabs = !resolvedApiKey.isNullOrBlank()
            val targetUrl = if (useDirectElevenLabs) {
                "https://api.elevenlabs.io/v1/music?output_format=auto"
            } else {
                resolvedProxyUrl
            }

            onLogMessage(
                if (useDirectElevenLabs) {
                    "> ELEVENLABS_API_KEY resolved.\n" +
                        "> Endpoint: POST https://api.elevenlabs.io/v1/music?output_format=auto\n"
                } else {
                    "> Proxy key kept server-side; routing via $resolvedProxyUrl (provider: elevenlabs)\n"
                }
            )
            onLogMessage("> Model ID: $resolvedModelId | Length: ${musicLengthMs}ms\n")

            val jsonBody = JSONObject().apply {
                if (!useDirectElevenLabs) {
                    put("provider", "elevenlabs")
                }
                put("model_id", resolvedModelId)
                put("prompt", fullPrompt)
                put("music_length_ms", musicLengthMs)
            }

            val requestBody = jsonBody.toString().toRequestBody("application/json".toMediaTypeOrNull())
            val requestBuilder = Request.Builder()
                .url(targetUrl)
                .header("Content-Type", "application/json")
                .header("Accept", "audio/mpeg, application/json")
                .post(requestBody)

            if (useDirectElevenLabs) {
                requestBuilder.header("xi-api-key", resolvedApiKey!!)
            } else {
                requestBuilder.withProxyAuth("Bearer", firebaseToken)
            }
            val request = requestBuilder.build()

            val musicClient = client.newBuilder()
                .readTimeout(180, TimeUnit.SECONDS)
                .writeTimeout(60, TimeUnit.SECONDS)
                .build()

            onLogMessage("> Sending music composition request ($resolvedModelId)...\n")

            fun extractElevenLabsError(rawBody: String, fallback: String): String {
                return try {
                    val errJson = JSONObject(rawBody)
                    val detail = errJson.opt("detail")
                    when (detail) {
                        is JSONObject -> {
                            val status = detail.optString("status", "")
                            val msg = detail.optString("message", "")
                            listOf(status, msg).filter { it.isNotBlank() }.joinToString(": ")
                                .ifBlank { rawBody }
                        }
                        is org.json.JSONArray -> detail.toString()
                        is String -> detail
                        else -> errJson.optString("message", errJson.optString("error", rawBody))
                    }.ifBlank { fallback }
                } catch (_: Exception) {
                    rawBody.ifBlank { fallback }
                }
            }

            musicClient.newCall(request).execute().use { response ->
                val contentType = response.header("Content-Type").orEmpty().lowercase()
                if (!response.isSuccessful) {
                    val errorBody = response.body?.string().orEmpty()
                    val parsedMsg = extractElevenLabsError(errorBody, response.message)
                    val hint = when (response.code) {
                        404 -> " (Endpoint POST /api/music belum aktif di proxy atau URL tidak ditemukan)"
                        422 -> " (Parameter ditolak ElevenLabs — pastikan proxy hanya meneruskan model_id, prompt, music_length_ms & menghapus field 'provider')"
                        401, 403 -> " (API key ElevenLabs tidak valid atau akun ElevenLabs belum berlangganan Music API)"
                        else -> ""
                    }
                    throw IllegalStateException("ElevenLabs Error HTTP ${response.code}: $parsedMsg$hint")
                }

                val body = response.body ?: throw IllegalStateException("Backend proxy returned an empty response body.")
                val rawBytes = body.bytes()

                if (contentType.contains("application/json") || (rawBytes.isNotEmpty() && rawBytes[0] == '{'.code.toByte())) {
                    val jsonStr = runCatching { String(rawBytes) }.getOrDefault("")
                    val json = JSONObject(jsonStr)
                    val remoteUrl = json.optString("url")
                        .ifBlank { json.optString("audioUrl") }
                        .ifBlank { json.optString("audio_url") }
                    if (remoteUrl.isNotBlank()) {
                        onLogMessage("> Received music URL from proxy.\n")
                        return@withContext remoteUrl
                    }

                    val audioBase64 = json.optString("audio_base64")
                        .ifBlank { json.optString("audio") }
                        .ifBlank { json.optString("b64_json") }
                    if (audioBase64.isNotBlank()) {
                        val cleanB64 = audioBase64.substringAfter("base64,", audioBase64)
                        val decodedBytes = android.util.Base64.decode(cleanB64, android.util.Base64.DEFAULT)
                        val musicDir = java.io.File(com.example.AppContextProvider.context.filesDir, "generated_music")
                        if (!musicDir.exists()) musicDir.mkdirs()
                        val outFile = java.io.File(
                            musicDir,
                            "elevenlabs_${System.currentTimeMillis()}_${java.util.UUID.randomUUID()}.mp3"
                        )
                        java.io.FileOutputStream(outFile).use { out ->
                            out.write(decodedBytes)
                        }
                        onLogMessage("> Saved ${decodedBytes.size} bytes of decoded audio/mpeg.\n")
                        return@withContext "file://${outFile.absolutePath}"
                    }

                    if (json.has("detail") || json.has("error") || json.optBoolean("success", true).not()) {
                        val errMsg = extractElevenLabsError(jsonStr, "Unknown music proxy error")
                        throw IllegalStateException("ElevenLabs Proxy Error: $errMsg")
                    }

                    throw IllegalStateException("Unexpected JSON response from music proxy: ${jsonStr.take(300)}")
                }

                if (rawBytes.size < 128) {
                    val previewText = runCatching { String(rawBytes) }.getOrDefault("")
                    throw IllegalStateException("Invalid audio payload from ElevenLabs proxy (${rawBytes.size} bytes): $previewText")
                }

                val musicDir = java.io.File(com.example.AppContextProvider.context.filesDir, "generated_music")
                if (!musicDir.exists()) {
                    musicDir.mkdirs()
                }
                val outFile = java.io.File(
                    musicDir,
                    "elevenlabs_${System.currentTimeMillis()}_${java.util.UUID.randomUUID()}.mp3"
                )
                java.io.FileOutputStream(outFile).use { out ->
                    out.write(rawBytes)
                }
                onLogMessage("> Received ${rawBytes.size} bytes of audio/mpeg via backend proxy.\n")
                "file://${outFile.absolutePath}"
            }
        }
    }
}
