package com.example.data

import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

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
    private val defaultSystemInstruction = "You are a helpful and expert AI Assistant. Please use Indonesian if requested, or Markdown formatting for easy reading."

    suspend fun sendMessage(
        history: List<ChatMessage>,
        newPrompt: String,
        model: String,
        customSystemInstruction: String? = null,
        customApiKey: String? = null
    ): String {
        return withContext(Dispatchers.IO) {
            val activeApiKey = customApiKey?.ifBlank { null } ?: defaultApiKey
            val activeSystemInstruction = Content(
                parts = listOf(Part(customSystemInstruction?.ifBlank { null } ?: defaultSystemInstruction))
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

            try {
                val response = GeminiClient.service.generateContent(model, activeApiKey, request)
                response.candidates.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: "Tidak ada respons."
            } catch (e: Exception) {
                "Error: ${e.localizedMessage}"
            }
        }
    }
}
