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
    val isBookmarked: Boolean = false
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
        providerKeys: Map<String, String> = emptyMap()
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
                "claude-sonnet-4-6" -> "openai"
                "claude-haiku-4-5-20251001" -> "openai"
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
                        callGemini(sanitizedHistory, newPrompt, if (resolvedModel == "openai") "gemini-3-flash-preview" else resolvedModel, systemText, fallbackKey)
                    }
                }
                "OpenAI" -> {
                    val fallbackKey = getFallbackKey(activeKey, BuildConfig.OPENAI_API_KEY, "PLACEHOLDER_OPENAI_API_KEY")
                    if (fallbackKey == null) {
                        "API Key OpenAI is missing. Please set your OpenAI API Key in Settings to start chatting."
                    } else {
                        callOpenAiCompatible(
                            history = sanitizedHistory,
                            newPrompt = newPrompt,
                            model = if (resolvedModel == "openai") "gpt-5-mini" else resolvedModel,
                            systemText = systemText,
                            apiKey = fallbackKey,
                            baseUrl = "https://api.openai.com/v1/chat/completions"
                        )
                    }
                }
                "DeepSeek" -> {
                    val fallbackKey = getFallbackKey(activeKey, BuildConfig.DEEPSEEK_API_KEY, "PLACEHOLDER_DEEPSEEK_API_KEY")
                    if (fallbackKey == null) {
                        "API Key DeepSeek is missing. Please set your DeepSeek API Key in Settings to start chatting."
                    } else {
                        callOpenAiCompatible(
                            history = sanitizedHistory,
                            newPrompt = newPrompt,
                            model = "deepseek-chat",
                            systemText = systemText,
                            apiKey = fallbackKey,
                            baseUrl = "https://api.deepseek.com/v1/chat/completions"
                        )
                    }
                }
                "Claude" -> {
                    val fallbackKey = getFallbackKey(activeKey, BuildConfig.ANTHROPIC_API_KEY, "PLACEHOLDER_ANTHROPIC_API_KEY")
                    if (fallbackKey == null) {
                        "API Key Claude is missing. Please set your Anthropic Claude API Key in Settings to start chatting."
                    } else {
                        callClaude(sanitizedHistory, newPrompt, resolvedModel, systemText, fallbackKey)
                    }
                }
                "XAI" -> {
                    val fallbackKey = getFallbackKey(activeKey, "", "PLACEHOLDER_XAI_API_KEY")
                    if (fallbackKey == null) {
                        "API Key xAI is missing. Please set your xAI API Key in Settings to start chatting."
                    } else {
                        callOpenAiCompatible(
                            history = sanitizedHistory,
                            newPrompt = newPrompt,
                            model = "grok-beta",
                            systemText = systemText,
                            apiKey = fallbackKey,
                            baseUrl = "https://api.x.ai/v1/chat/completions"
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
        apiKey: String
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
            val response = GeminiClient.service.generateContent(model, apiKey, request)
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
        apiKey: String
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
                .url("https://api.anthropic.com/v1/messages")
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
        providerKeys: Map<String, String> = emptyMap()
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
                val response = GeminiClient.service.generateContent("gemini-3-flash-preview", keyToUse, request)
                response.candidates.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: "No response from Gemini."
            } catch (e: Exception) {
                "Error: ${e.localizedMessage}"
            }
        }
    }

    suspend fun sendVisionRequest(
        bitmap: android.graphics.Bitmap,
        prompt: String,
        providerKeys: Map<String, String> = emptyMap()
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
                val response = GeminiClient.service.generateContent("gemini-3-flash-preview", keyToUse, request)
                response.candidates.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: "No response from Gemini."
            } catch (e: Exception) {
                "Error: ${e.localizedMessage}"
            }
        }
    }
}
