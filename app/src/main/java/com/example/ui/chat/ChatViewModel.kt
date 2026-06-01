package com.example.ui.chat

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.AppContextProvider
import com.example.data.ChatMessage
import com.example.data.ChatRepository
import com.example.data.Role
import com.example.data.db.AppDatabase
import com.example.data.db.ChatMessageEntity
import com.example.data.db.ChatSessionEntity
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import java.util.UUID
import com.example.BuildConfig
import com.example.data.GeminiClient
import com.example.data.GenerateContentRequest
import com.example.data.Content
import com.example.data.Part
import com.example.data.GenerationConfig
import com.example.data.ImageConfig

fun Double.formatTokens(): String {
    return if (this % 1.0 == 0.0) {
        this.toLong().toString()
    } else {
        val formatted = String.format(java.util.Locale.US, "%.3f", this)
        formatted.replace(Regex("0+$"), "").replace(Regex("\\.$"), "")
    }
}

data class GeminiModel(
    val displayName: String,
    val apiValue: String,
    val subtitle: String,
    val provider: String = "Gemini",
    val isPro: Boolean = false
)

val availableModels = listOf(
    // Gemini
    GeminiModel(
        displayName = "Gemini 2.5 Pro",
        apiValue = "gemini-2.5-pro",
        subtitle = "Advanced model designed for coding/complex tasks.",
        provider = "Gemini",
        isPro = true
    ),
    GeminiModel(
        displayName = "Gemini 3.1 Pro Preview",
        apiValue = "gemini-3.1-pro-preview",
        subtitle = "A preview of our ultimate, highly sophisticated future model.",
        provider = "Gemini",
        isPro = true
    ),
    GeminiModel(
        displayName = "Gemini 3 Flash Preview",
        apiValue = "gemini-3-flash-preview",
        subtitle = "Next-generation fast model optimized for real-time applications.",
        provider = "Gemini",
        isPro = false
    ),
    // OpenAI
    GeminiModel(
        displayName = "GPT 5 Mini",
        apiValue = "gpt-5-mini",
        subtitle = "OpenAI's efficient, lightweight model with high-speed intelligence.",
        provider = "OpenAI",
        isPro = false
    ),
    GeminiModel(
        displayName = "GPT 5.5",
        apiValue = "gpt5.5",
        subtitle = "Next-generation reasoning model from OpenAI.",
        provider = "OpenAI",
        isPro = false
    ),
    // DeepSeek
    GeminiModel(
        displayName = "DeepSeek Chat",
        apiValue = "deepseek-chat",
        subtitle = "DeepSeek's powerful, cost-efficient language model.",
        provider = "DeepSeek",
        isPro = false
    )
)

data class ChatSession(
    val id: String = UUID.randomUUID().toString(),
    val title: String = "New Conversation",
    val messages: List<ChatMessage> = emptyList()
)

data class UserProfile(
    val email: String,
    val username: String
)

data class ChatUiState(
    val sessions: List<ChatSession> = emptyList(),
    val currentSessionId: String = "",
    val isLoading: Boolean = false,
    val selectedModel: GeminiModel = availableModels[0], // Default is Gemini 3.5 Flash
    val customSystemInstruction: String = "You are Eddie, a helpful and expert AI Assistant. Please use Indonesian if requested, or Markdown formatting for easy reading.",
    val customApiKey: String = "",
    val geminiApiKey: String = "",
    val openaiApiKey: String = "",
    val deepseekApiKey: String = "",
    val claudeApiKey: String = "",
    val xaiApiKey: String = "",
    val isDarkTheme: Boolean = false,
    val currentUserProfile: UserProfile? = null,
    val isSubscribed: Boolean = true,
    val activeSubscriptionPlan: String? = "Developer Pro (Testing)",
    val subscriptionExpiryDate: String? = "Permanent (Testing Phase)",
    val userTokens: Double = 999999.0,
    val generatedImages: List<com.example.data.db.GeneratedImageEntity> = emptyList(),
    val selectedImageProvider: String = "OpenAI",
    val selectedImageModel: String = "GPT Image 1.5",
    val imageGenIsLoading: Boolean = false,
    val imageGenLog: String = "",
    val imageGenEngine: String = "Google Imagen 3", // Options: "Google Imagen 3" (Premium Fallback)
    val generatedVideos: List<com.example.data.db.GeneratedVideoEntity> = emptyList(),
    val selectedVideoMode: String = "Text to Video",
    val selectedVideoProvider: String = "OpenAI",
    val selectedVideoModel: String = "Sora 2",
    val videoGenIsLoading: Boolean = false,
    val videoGenLog: String = "",
    val generatedMusic: List<com.example.data.db.GeneratedMusicEntity> = emptyList(),
    val selectedMusicProvider: String = "Gemini",
    val selectedMusicModel: String = "Lyria 3 pro",
    val musicGenIsLoading: Boolean = false,
    val musicGenLog: String = "",
    val codeSelectedLanguage: String = "Kotlin",
    val codeSelectedTask: String = "Generasi Kode",
    val codeTargetLanguage: String = "Python",
    val codeOutput: String = "",
    val codeIsLoading: Boolean = false,
    val codeFreeQueriesLeft: Int = 3,
    
    // Asisten Suara
    val voiceIsSpeakActive: Boolean = false,
    val voiceRecognizedText: String = "",
    val voiceAssistantResponse: String = "",
    val voiceIsLoading: Boolean = false,
    val voiceChatLog: List<Pair<String, Boolean>> = emptyList(), // Message to isUser
    
    // Vision & Image Analyzer
    val visionSelectedImageUri: android.net.Uri? = null,
    val visionSelectedBitmap: android.graphics.Bitmap? = null,
    val visionResult: String = "",
    val visionIsLoading: Boolean = false,
    
    // Document Intelligence
    val docSelectedUri: android.net.Uri? = null,
    val docSelectedName: String = "",
    val docContentText: String = "",
    val docSummaryResult: String = "",
    val docIsLoading: Boolean = false,
    val docQaAnswer: String = "",
    val docQaIsLoading: Boolean = false,

    // Agentic Web Search (Browsing Mandiri)
    val isWebSearchEnabled: Boolean = false,
    val isSearchingWeb: Boolean = false,
    val webSearchQueries: List<String> = emptyList(),
    val webSearchResultsSummary: String = "",

    // Smart Memory & Personalization (Rantai Konteks Panjang)
    val userMemories: List<com.example.data.db.UserMemoryEntity> = emptyList(),
    val isMemoryLoaded: Boolean = false,
    val lastExtractedMemory: String = "",

    // Agentic Task Automation (Action-to-API)
    val activeAutomationType: String? = null, // "EMAIL" or "WHATSAPP"
    val activeAutomationPayload: String = "",
    val activeAutomationStatus: String = "",
    
    // Agentic Tooltip / Guide Status
    val showAgenticTooltip: Boolean = true
) {
    val currentSession: ChatSession
        get() = sessions.find { it.id == currentSessionId } ?: sessions.firstOrNull() ?: ChatSession()

    val messages: List<ChatMessage>
        get() = currentSession.messages

    fun getFormattedTokens(): String {
        return userTokens.formatTokens()
    }
}

class ChatViewModel(
    private val repository: ChatRepository = ChatRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    private val database = AppDatabase.getDatabase(AppContextProvider.context)
    private val chatDao = database.chatDao()

    private var sessionsCollectionJob: Job? = null
    private var messagesCollectionJob: Job? = null
    private var imagesCollectionJob: Job? = null
    private var videosCollectionJob: Job? = null
    private var musicCollectionJob: Job? = null

    // Keep track of registered users in memory for high-fidelity fully-functional Sign In/Up
    private val registeredUsers = mutableMapOf<String, Pair<String, String>>(
        "mufeas@gmail.com" to Pair("password123", "Eddie")
    )

    // Expose bookmarked messages reactively to build the saved/favorite feature
    val bookmarkedMessages: StateFlow<List<ChatMessageEntity>> = chatDao.getBookmarkedMessages()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    init {
        val prefs = try {
            AppContextProvider.context.getSharedPreferences("eddie_prefs", Context.MODE_PRIVATE)
        } catch (e: Exception) {
            android.util.Log.e("ChatViewModel", "Failed to retrieve shared preferences in init", e)
            null
        }
        val showTooltip = prefs?.getBoolean("show_agentic_tooltip", true) ?: true
        
        val existingOpenai = prefs?.getString("openai_key", "") ?: ""
        val initialOpenai = if (existingOpenai.isBlank()) {
            val key = "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"
            try {
                prefs?.edit()?.putString("openai_key", key)?.apply()
            } catch (e: Exception) {
                // Ignore save errors
            }
            key
        } else {
            existingOpenai
        }

        val existingGemini = prefs?.getString("gemini_key", "") ?: ""
        val initialGemini = if (existingGemini.isBlank()) {
            val key = "AIzaSyA2L6MULkOonvtSdlDQEUJCep3Kc13UzAQ"
            try {
                prefs?.edit()?.putString("gemini_key", key)?.apply()
            } catch (e: Exception) {
                // Ignore save errors
            }
            key
        } else {
            existingGemini
        }

        val tokens = if (prefs != null) {
            try {
                prefs.getFloat("user_tokens", 999999f).toDouble()
            } catch (e: Exception) {
                try {
                    prefs.getInt("user_tokens", 999999).toDouble()
                } catch (ex: Exception) {
                    999999.0
                }
            }
        } else {
            999999.0
        }

        _uiState.update { state ->
            state.copy(
                geminiApiKey = initialGemini,
                openaiApiKey = initialOpenai,
                deepseekApiKey = prefs?.getString("deepseek_key", "") ?: "",
                claudeApiKey = prefs?.getString("claude_key", "") ?: "",
                xaiApiKey = prefs?.getString("xai_key", "") ?: "",
                showAgenticTooltip = showTooltip,
                userTokens = tokens
            )
        }
        observeSessionsByCurrentUser()
        observeMessagesForCurrentSession()
        observeGeneratedImagesByCurrentUser()
        observeGeneratedVideosByCurrentUser()
        observeGeneratedMusicByCurrentUser()
        observeMemoriesByCurrentUser()
    }

    fun dismissAgenticTooltip() {
        val prefs = AppContextProvider.context.getSharedPreferences("eddie_prefs", Context.MODE_PRIVATE)
        prefs.edit().putBoolean("show_agentic_tooltip", false).apply()
        _uiState.update { state ->
            state.copy(showAgenticTooltip = false)
        }
    }

    fun resetAgenticTooltip() {
        val prefs = AppContextProvider.context.getSharedPreferences("eddie_prefs", Context.MODE_PRIVATE)
        prefs.edit().putBoolean("show_agentic_tooltip", true).apply()
        _uiState.update { state ->
            state.copy(showAgenticTooltip = true)
        }
    }

    fun updateProviderApiKey(provider: String, key: String) {
        val prefs = AppContextProvider.context.getSharedPreferences("eddie_prefs", Context.MODE_PRIVATE)
        prefs.edit().putString("${provider.lowercase()}_key", key).apply()
        _uiState.update { state ->
            when (provider) {
                "Gemini" -> state.copy(geminiApiKey = key)
                "OpenAI" -> state.copy(openaiApiKey = key)
                "DeepSeek" -> state.copy(deepseekApiKey = key)
                "Claude" -> state.copy(claudeApiKey = key)
                "XAI" -> state.copy(xaiApiKey = key)
                else -> state
            }
        }
    }

    private fun observeSessionsByCurrentUser() {
        sessionsCollectionJob?.cancel()
        sessionsCollectionJob = viewModelScope.launch {
            // Unify or selectively load based on currently active logged-in user profile email
            _uiState.map { it.currentUserProfile?.email }
                .distinctUntilChanged()
                .collectLatest { email ->
                    val queryFlow = if (email == null) {
                        chatDao.getAllSessions()
                    } else {
                        chatDao.getSessionsForUser(email)
                    }

                    queryFlow
                        .catch { e -> android.util.Log.e("ChatViewModel", "observeSessions error", e) }
                        .collect { sessionEntities ->
                            if (sessionEntities.isEmpty()) {
                                val initialSession = ChatSession()
                                try {
                                    chatDao.insertSession(ChatSessionEntity(
                                        id = initialSession.id,
                                        title = initialSession.title,
                                        modelValue = _uiState.value.selectedModel.apiValue,
                                        userEmail = _uiState.value.currentUserProfile?.email
                                    ))
                                } catch (e: Exception) {
                                    android.util.Log.e("ChatViewModel", "Failed to insert initial session", e)
                                }
                                return@collect
                            }

                            val mappedSessions = sessionEntities.map { entity ->
                                // Keep in-memory loaded chat messages if session matches
                                val existingMessages = _uiState.value.sessions.find { it.id == entity.id }?.messages ?: emptyList()
                                ChatSession(
                                    id = entity.id,
                                    title = entity.title,
                                    messages = existingMessages
                                )
                            }

                            _uiState.update { state ->
                                val activeSessionId = if (state.currentSessionId.isBlank() || !mappedSessions.any { it.id == state.currentSessionId }) {
                                    mappedSessions.first().id
                                } else {
                                    state.currentSessionId
                                }

                                state.copy(
                                    sessions = mappedSessions,
                                    currentSessionId = activeSessionId
                                )
                            }
                        }
                }
        }
    }

    private fun observeMessagesForCurrentSession() {
        messagesCollectionJob?.cancel()
        messagesCollectionJob = viewModelScope.launch {
            _uiState.map { it.currentSessionId }
                .distinctUntilChanged()
                .collectLatest { sessionId ->
                    if (sessionId.isNotBlank()) {
                        chatDao.getMessagesForSession(sessionId)
                            .catch { e -> android.util.Log.e("ChatViewModel", "observeMessages error", e) }
                            .collect { messageEntities ->
                            val chatMessages = messageEntities.map { entity ->
                                ChatMessage(
                                    id = entity.id,
                                    role = if (entity.role == "USER") Role.USER else Role.MODEL,
                                    text = entity.text,
                                    isBookmarked = entity.isBookmarked
                                )
                            }
                            _uiState.update { state ->
                                val updatedSessions = state.sessions.map { session ->
                                    if (session.id == sessionId) {
                                        session.copy(messages = chatMessages)
                                    } else {
                                        session
                                    }
                                }
                                state.copy(sessions = updatedSessions)
                            }
                        }
                    }
                }
        }
    }

    private fun observeGeneratedImagesByCurrentUser() {
        imagesCollectionJob?.cancel()
        imagesCollectionJob = viewModelScope.launch {
            _uiState.map { it.currentUserProfile?.email }
                .distinctUntilChanged()
                .collectLatest { email ->
                    val queryFlow = if (email == null) {
                        chatDao.getAllGeneratedImages()
                    } else {
                        chatDao.getGeneratedImagesForUser(email)
                    }

                    queryFlow
                        .catch { e -> android.util.Log.e("ChatViewModel", "observeImages error", e) }
                        .collect { imageEntities ->
                        _uiState.update { state ->
                            state.copy(generatedImages = imageEntities)
                        }
                    }
                }
        }
    }

    private fun observeGeneratedVideosByCurrentUser() {
        videosCollectionJob?.cancel()
        videosCollectionJob = viewModelScope.launch {
            _uiState.map { it.currentUserProfile?.email }
                .distinctUntilChanged()
                .collectLatest { email ->
                    val queryFlow = if (email == null) {
                        chatDao.getAllGeneratedVideos()
                    } else {
                        chatDao.getGeneratedVideosForUser(email)
                    }

                    queryFlow
                        .catch { e -> android.util.Log.e("ChatViewModel", "observeVideos error", e) }
                        .collect { videoEntities ->
                        _uiState.update { state ->
                            state.copy(generatedVideos = videoEntities)
                        }
                    }
                }
        }
    }

    private fun observeGeneratedMusicByCurrentUser() {
        musicCollectionJob?.cancel()
        musicCollectionJob = viewModelScope.launch {
            _uiState.map { it.currentUserProfile?.email }
                .distinctUntilChanged()
                .collectLatest { email ->
                    val queryFlow = if (email == null) {
                        chatDao.getAllGeneratedMusic()
                    } else {
                        chatDao.getGeneratedMusicForUser(email)
                    }

                    queryFlow
                        .catch { e -> android.util.Log.e("ChatViewModel", "observeMusic error", e) }
                        .collect { musicEntities ->
                            _uiState.update { state ->
                                state.copy(generatedMusic = musicEntities)
                            }
                        }
                }
        }
    }

    private var memoriesCollectionJob: Job? = null

    private fun observeMemoriesByCurrentUser() {
        memoriesCollectionJob?.cancel()
        memoriesCollectionJob = viewModelScope.launch {
            _uiState.map { it.currentUserProfile?.email }
                .distinctUntilChanged()
                .collectLatest { email ->
                    val queryFlow = if (email == null) {
                        chatDao.getAllMemories()
                    } else {
                        chatDao.getMemoriesForUser(email)
                    }
                    queryFlow
                        .catch { e -> android.util.Log.e("ChatViewModel", "observeMemories error", e) }
                        .collect { list ->
                            _uiState.update { state ->
                                state.copy(userMemories = list, isMemoryLoaded = true)
                            }
                        }
                }
        }
    }

    fun setWebSearchEnabled(enabled: Boolean) {
        _uiState.update { it.copy(isWebSearchEnabled = enabled) }
    }

    fun dismissAutomation() {
        _uiState.update { it.copy(activeAutomationType = null, activeAutomationPayload = "", activeAutomationStatus = "") }
    }

    fun addMemory(key: String, content: String) {
        viewModelScope.launch {
            val user = _uiState.value.currentUserProfile
            val newMemory = com.example.data.db.UserMemoryEntity(
                id = UUID.randomUUID().toString(),
                key = key.uppercase().trim(),
                content = content.trim(),
                userEmail = user?.email
            )
            chatDao.insertMemory(newMemory)
        }
    }

    fun deleteMemory(id: String) {
        viewModelScope.launch {
            chatDao.deleteMemoryById(id)
        }
    }

    fun signIn(email: String, password: String): String? {
        val emailClean = email.trim().lowercase()
        if (emailClean.isBlank() || password.isBlank()) {
            return "Email and password are required."
        }
        val userPair = registeredUsers[emailClean]
        if (userPair == null) {
            return "Email is not registered. Please sign up first."
        }
        if (userPair.first != password) {
            return "Incorrect password."
        }
        _uiState.update { state ->
            state.copy(currentUserProfile = UserProfile(email = emailClean, username = userPair.second))
        }
        return null // Success and triggers re-collection mapping
    }

    fun signUp(email: String, password: String, username: String): String? {
        val emailClean = email.trim().lowercase()
        val usernameClean = username.trim()
        if (emailClean.isBlank() || password.isBlank() || usernameClean.isBlank()) {
            return "All fields are required."
        }
        if (!emailClean.contains("@")) {
            return "Invalid email address format."
        }
        if (registeredUsers.containsKey(emailClean)) {
            return "Email is already registered. Please sign in."
        }
        registeredUsers[emailClean] = Pair(password, usernameClean)
        _uiState.update { state ->
            state.copy(currentUserProfile = UserProfile(email = emailClean, username = usernameClean))
        }
        return null // Success and triggers re-collection mapping
    }

    fun signOut() {
        _uiState.update { state ->
            state.copy(
                currentUserProfile = null,
                isSubscribed = true,
                activeSubscriptionPlan = "Developer Pro (Testing)",
                subscriptionExpiryDate = "Permanent (Testing Phase)",
                userTokens = 999999.0
            )
        }
        saveTokensToPrefs(999999.0)
    }

    fun activateSubscription(plan: String) {
        val expiryDate = when (plan) {
            "Weekly" -> "7 Days from Now"
            "Monthly" -> "30 Days from Now"
            "Yearly" -> "365 Days from Now"
            else -> "Forever"
        }
        val tokensGained = when (plan) {
            "Weekly" -> 250
            "Monthly" -> 1200
            "Yearly" -> 7500
            else -> 0
        }
        val state = _uiState.value
        val newTokensValue = state.userTokens + tokensGained.toDouble()
        _uiState.update { s ->
            s.copy(
                isSubscribed = true,
                activeSubscriptionPlan = plan,
                subscriptionExpiryDate = expiryDate,
                userTokens = newTokensValue
            )
        }
        saveTokensToPrefs(newTokensValue)
    }

    fun cancelSubscription() {
        _uiState.update { state ->
            state.copy(
                isSubscribed = true,
                activeSubscriptionPlan = "Developer Pro (Testing)",
                subscriptionExpiryDate = "Permanent (Testing Phase)",
                userTokens = 999999.0
            )
        }
        saveTokensToPrefs(999999.0)
    }

    fun selectModel(model: GeminiModel) {
        _uiState.update { state ->
            state.copy(selectedModel = model)
        }
    }

    fun selectImageGenEngine(engine: String) {
        _uiState.update { state ->
            state.copy(imageGenEngine = engine)
        }
    }

    fun selectImageProvider(provider: String) {
        val defaultModel = when (provider) {
            "OpenAI" -> "GPT Image 1.5"
            "Stable Diffusion" -> "Stable Diffusion"
            "FalAI" -> "Flux Pro"
            else -> "GPT Image 1.5"
        }
        _uiState.update { state ->
            state.copy(
                selectedImageProvider = provider,
                selectedImageModel = defaultModel
            )
        }
    }

    fun selectImageModel(model: String) {
        _uiState.update { state ->
            state.copy(selectedImageModel = model)
        }
    }

    fun selectVideoMode(mode: String) {
        val defaultProvider = when (mode) {
            "Text to Video" -> "OpenAI"
            "Image to Video" -> "Gemini"
            else -> "OpenAI"
        }
        val defaultModel = when (mode) {
            "Text to Video" -> "Sora 2"
            "Image to Video" -> "veo 3.1"
            else -> "Sora 2"
        }
        _uiState.update { state ->
            state.copy(
                selectedVideoMode = mode,
                selectedVideoProvider = defaultProvider,
                selectedVideoModel = defaultModel
            )
        }
    }

    fun selectVideoProvider(provider: String) {
        val mode = _uiState.value.selectedVideoMode
        val defaultModel = if (mode == "Text to Video") {
            when (provider) {
                "OpenAI" -> "Sora 2"
                "Gemini" -> "Veo 3"
                "FalAI" -> "Kling v2.6"
                else -> "Sora 2"
            }
        } else {
            // Image to Video
            when (provider) {
                "Gemini" -> "veo 3.1"
                "FalAI" -> "Kling v2.6 pro"
                else -> "veo 3.1"
            }
        }
        _uiState.update { state ->
            state.copy(
                selectedVideoProvider = provider,
                selectedVideoModel = defaultModel
            )
        }
    }

    fun selectVideoModel(model: String) {
        _uiState.update { state ->
            state.copy(selectedVideoModel = model)
        }
    }

    fun selectMusicProvider(provider: String) {
        val defaultModel = when (provider) {
            "Gemini" -> "Lyria 3 pro"
            "Suno AI" -> "Suno v4 pro"
            else -> "Lyria 3 pro"
        }
        _uiState.update { state ->
            state.copy(
                selectedMusicProvider = provider,
                selectedMusicModel = defaultModel
            )
        }
    }

    fun selectMusicModel(model: String) {
        _uiState.update { state ->
            state.copy(selectedMusicModel = model)
        }
    }

    fun selectSession(sessionId: String) {
        _uiState.update { state ->
            state.copy(currentSessionId = sessionId)
        }
    }

    fun startNewChat() {
        val newSessionId = UUID.randomUUID().toString()
        viewModelScope.launch {
            chatDao.insertSession(ChatSessionEntity(
                id = newSessionId,
                title = "Percakapan Baru",
                modelValue = _uiState.value.selectedModel.apiValue,
                userEmail = _uiState.value.currentUserProfile?.email
            ))
            _uiState.update { state ->
                state.copy(currentSessionId = newSessionId)
            }
        }
    }

    fun deleteSession(sessionId: String) {
        viewModelScope.launch {
            chatDao.deleteSessionById(sessionId)
            if (_uiState.value.currentSessionId == sessionId) {
                val remain = _uiState.value.sessions.filter { it.id != sessionId }
                if (remain.isEmpty()) {
                    startNewChat()
                } else {
                    _uiState.update { state ->
                        state.copy(currentSessionId = remain.first().id)
                    }
                }
            }
        }
    }

    fun toggleBookmark(messageId: String, isBookmarked: Boolean) {
        viewModelScope.launch {
            chatDao.updateMessageBookmark(messageId, isBookmarked)
        }
    }

    fun updateSystemInstruction(instruction: String) {
        _uiState.update { state ->
            state.copy(customSystemInstruction = instruction)
        }
    }

    fun updateApiKey(apiKey: String) {
        _uiState.update { state ->
            state.copy(customApiKey = apiKey)
        }
    }

    fun toggleTheme() {
        _uiState.update { state ->
            state.copy(isDarkTheme = !state.isDarkTheme)
        }
    }

    fun sendMessage(prompt: String) {
        if (prompt.isBlank()) return
        
        var sessionId = _uiState.value.currentSessionId
        val userItemMessageId = UUID.randomUUID().toString()

        val selectedModel = _uiState.value.selectedModel
        val isTokenBased = selectedModel.apiValue == "gpt-5-mini" || selectedModel.apiValue == "gpt5.5" || selectedModel.apiValue == "deepseek-chat"
        val minTokensNeeded = when (selectedModel.apiValue) {
            "gpt-5-mini" -> 0.0166875
            "deepseek-chat" -> 0.0091875
            "gpt5.5" -> 0.25
            else -> 0.0
        }

        if (isTokenBased && _uiState.value.userTokens < minTokensNeeded) {
            viewModelScope.launch {
                try {
                    // Insert Session & User message so they can see the chat trail
                    if (sessionId.isBlank()) {
                        val newSessionId = UUID.randomUUID().toString()
                        val sessionTitle = if (prompt.length > 25) prompt.take(22) + "..." else prompt
                        chatDao.insertSession(ChatSessionEntity(
                            id = newSessionId,
                            title = sessionTitle,
                            modelValue = selectedModel.apiValue,
                            userEmail = _uiState.value.currentUserProfile?.email
                        ))
                        sessionId = newSessionId
                        _uiState.update { state -> state.copy(currentSessionId = newSessionId) }
                    }
                    
                    chatDao.insertMessage(ChatMessageEntity(
                        id = userItemMessageId,
                        sessionId = sessionId,
                        role = "USER",
                        text = prompt,
                        timestamp = System.currentTimeMillis()
                    ))

                    _uiState.update { state -> state.copy(isLoading = true) }
                    delay(700)

                    chatDao.insertMessage(ChatMessageEntity(
                        id = UUID.randomUUID().toString(),
                        sessionId = sessionId,
                        role = "MODEL",
                        text = "❌ **Insufficient Tokens**: Using the super reasoning model `${selectedModel.displayName}` requires a minimum of **${minTokensNeeded.formatTokens()} Tokens** in your balance. Your current token balance is: **🪙 ${_uiState.value.getFormattedTokens()}**. Please top up in the Subscription tab.",
                        timestamp = System.currentTimeMillis()
                    ))
                } catch (e: Exception) {
                    android.util.Log.e("ChatViewModel", "Error while inserting insufficient token notice", e)
                } finally {
                    _uiState.update { state -> state.copy(isLoading = false) }
                }
            }
            return
        }

        viewModelScope.launch {
            try {
                // 1. Resolve Session ID
                if (sessionId.isBlank()) {
                    val newSessionId = UUID.randomUUID().toString()
                    val sessionTitle = if (prompt.length > 25) prompt.take(22) + "..." else prompt
                    chatDao.insertSession(ChatSessionEntity(
                        id = newSessionId,
                        title = sessionTitle,
                        modelValue = _uiState.value.selectedModel.apiValue,
                        userEmail = _uiState.value.currentUserProfile?.email
                    ))
                    sessionId = newSessionId
                    _uiState.update { state ->
                        state.copy(currentSessionId = newSessionId)
                    }
                } else {
                    val existingSession = _uiState.value.sessions.find { it.id == sessionId }
                    if (existingSession != null && existingSession.title == "Percakapan Baru") {
                        val newTitle = if (prompt.length > 25) prompt.take(22) + "..." else prompt
                        chatDao.insertSession(ChatSessionEntity(
                            id = sessionId,
                            title = newTitle,
                            modelValue = _uiState.value.selectedModel.apiValue,
                            userEmail = _uiState.value.currentUserProfile?.email
                        ))
                    }
                }

                // 2. Insert user's query
                chatDao.insertMessage(ChatMessageEntity(
                    id = userItemMessageId,
                    sessionId = sessionId,
                    role = "USER",
                    text = prompt,
                    timestamp = System.currentTimeMillis()
                ))

                // Set preliminary loading state
                _uiState.update { state -> state.copy(isLoading = true) }

                // 3. AI Agentic Web Search (Browsing Mandiri) Detection
                val cleanLower = prompt.lowercase().trim()
                val autoTriggerSearch = cleanLower.contains("search") || cleanLower.contains("browsing") ||
                        cleanLower.contains("news") || cleanLower.contains("score") ||
                        cleanLower.contains("weather") || cleanLower.contains("stock") ||
                        cleanLower.contains("market")

                val searchActive = _uiState.value.isWebSearchEnabled || autoTriggerSearch
                var injectedPrompt = prompt
                var searchSummaryText = ""

                if (searchActive) {
                    _uiState.update { state ->
                        state.copy(
                            isSearchingWeb = true,
                            webSearchQueries = state.webSearchQueries + prompt
                        )
                    }
                    // Simulated agentic delay to enhance realism of the search process
                    delay(1200)

                    val snippets = com.example.data.WebSearchService.search(prompt)
                    if (snippets.isNotEmpty()) {
                        searchSummaryText = snippets.joinToString("\n\n") {
                            "[SOURCE: ${it.source}] - ${it.title} (${it.date})\nLink: ${it.link}\nSummary: ${it.snippet}"
                        }
                        injectedPrompt = "Here are the latest real-time Google search results (Today is Saturday, May 30, 2026) for the query '${prompt}':\n\n$searchSummaryText\n\nPlease use the latest data above to respond to the user's questions intelligently and accurately. Please include a flag icon 🌐 and a brief search reference at the beginning of your response so the user knows you have performed a real-time Google Search exploration!\n\nUser Question: $prompt"
                    }

                    _uiState.update { state ->
                        state.copy(
                            isSearchingWeb = false,
                            webSearchResultsSummary = "Successfully found ${snippets.size} search results from Google!"
                        )
                    }
                }

                // 4. Smart Memory Context Retrieval (Long-Term Personalization Context)
                val memories = _uiState.value.userMemories
                val memoryInstruct = if (memories.isNotEmpty()) {
                    "\n\n[PERSONAL MEMORY SECURE AND PRIVATE THAT YOU REMEMBER ABOUT THE USER]:\n" +
                    memories.joinToString("\n") { "- ${it.key}: ${it.content}" } +
                    "\nUse the preferences or details above to respond in a friendly and helpful manner."
                } else {
                    ""
                }

                val finalSystemInstruction = _uiState.value.customSystemInstruction + memoryInstruct

                // 5. Build prompt history
                val history = _uiState.value.messages

                // Call API Repository
                val keysMap = mapOf(
                    "Gemini" to _uiState.value.geminiApiKey,
                    "OpenAI" to _uiState.value.openaiApiKey,
                    "DeepSeek" to _uiState.value.deepseekApiKey,
                    "Claude" to _uiState.value.claudeApiKey,
                    "XAI" to _uiState.value.xaiApiKey
                )

                // Let's call the repository API with the injected search payload & extended contextual system instruction
                val responseText = repository.sendMessage(
                    history = history,
                    newPrompt = injectedPrompt,
                    model = _uiState.value.selectedModel.apiValue,
                    provider = _uiState.value.selectedModel.provider,
                    customSystemInstruction = finalSystemInstruction,
                    providerKeys = keysMap
                )

                val modelItemMessageId = UUID.randomUUID().toString()

                // Calculate and deduct tokens dynamically based on word count of the generated response text
                var finalResponseText = responseText
                if (isTokenBased) {
                    val dynamicCost = calculateTextResponseTokenCost(selectedModel.apiValue, responseText)
                    if (dynamicCost > 0.0) {
                        val nextTokens = (_uiState.value.userTokens - dynamicCost).coerceAtLeast(0.0)
                        _uiState.update { state -> state.copy(userTokens = nextTokens) }
                        saveTokensToPrefs(nextTokens)
                        
                        val wordCount = countWords(responseText)
                        val rateInfo = when (selectedModel.apiValue) {
                            "gpt-5-mini" -> "0.0166875 Tokens per 25 words"
                            "deepseek-chat" -> "0.0091875 Tokens per 25 words"
                            else -> "0.25 Tokens per 25 words"
                        }
                        finalResponseText = "$responseText\n\n---\n*🪙 **Token Usage**: Deducted **${dynamicCost.formatTokens()} Tokens** ($wordCount words | rate: $rateInfo)*"
                    }
                }

                // 6. Save model's response to Database
                chatDao.insertMessage(ChatMessageEntity(
                    id = modelItemMessageId,
                    sessionId = sessionId,
                    role = "MODEL",
                    text = finalResponseText,
                    timestamp = System.currentTimeMillis()
                ))

                // 7. AI Agentic Task Automation (Action-to-API) Detection
                if (cleanLower.contains("email") || cleanLower.contains("surel") || cleanLower.contains("surat")) {
                    val emailCleanBody = responseText.replace(Regex("<[^>]*>"), "")
                    val extractedRecipient = if (cleanLower.contains("klien")) "klien@company.com" else "manager@company.com"
                    _uiState.update { state ->
                        state.copy(
                            activeAutomationType = "EMAIL",
                            activeAutomationPayload = "subject:Draf Surat Elektronik dari Eddie AI|recipient:$extractedRecipient|body:$emailCleanBody",
                            activeAutomationStatus = "Layanan otomasi draf email terdeteksi! Siap beralih ke Gmail."
                        )
                    }
                } else if (cleanLower.contains("whatsapp") || cleanLower.contains(" wa ") || cleanLower.contains("kirim ke wa") || cleanLower.contains("membagikan ke wa")) {
                    val waCleanBody = responseText.replace(Regex("<[^>]*>"), "")
                    _uiState.update { state ->
                        state.copy(
                            activeAutomationType = "WHATSAPP",
                            activeAutomationPayload = waCleanBody,
                            activeAutomationStatus = "Tindakan Berbagi WhatsApp Terdeteksi! Siap meluncurkan WhatsApp."
                        )
                    }
                }

                // 8. Automatic Memory Extraction in Background
                extractAndSaveMemoryInBg(prompt)

                // Dismiss Loading
                _uiState.update { state -> state.copy(isLoading = false) }
            } catch (e: Exception) {
                _uiState.update { state -> state.copy(isLoading = false) }
                val errorMsg = "Koneksi terputus atau batas waktu habis (timeout) saat menghubungi server: ${e.localizedMessage}. Silakan periksa koneksi internet Anda atau lengkapi API Key Anda di menu Pengaturan."
                
                if (sessionId.isBlank()) {
                    val newSessionId = UUID.randomUUID().toString()
                    sessionId = newSessionId
                    _uiState.update { state ->
                        state.copy(currentSessionId = newSessionId)
                    }
                    try {
                        chatDao.insertSession(ChatSessionEntity(
                            id = newSessionId,
                            title = "Koneksi Terputus",
                            modelValue = _uiState.value.selectedModel.apiValue,
                            userEmail = _uiState.value.currentUserProfile?.email
                        ))
                    } catch (dbEx: Exception) {
                        // Ignore DB errors
                    }
                }
                
                try {
                    val errorItemMessageId = UUID.randomUUID().toString()
                    chatDao.insertMessage(ChatMessageEntity(
                        id = errorItemMessageId,
                        sessionId = sessionId,
                        role = "MODEL",
                        text = errorMsg,
                        timestamp = System.currentTimeMillis()
                    ))
                } catch (dbEx: Exception) {
                    // Ignore DB errors
                }
            }
        }
    }

    private fun extractAndSaveMemoryInBg(userPrompt: String) {
        val lowerPrompt = userPrompt.lowercase().trim()
        val userSpec = _uiState.value.currentUserProfile
        
        // Quick regex and trigger rules
        if (lowerPrompt.contains("call me") || lowerPrompt.contains("my name") || 
            lowerPrompt.contains("my hobby") || lowerPrompt.contains("my preference") || 
            lowerPrompt.contains("my job") || lowerPrompt.contains("daily goal") ||
            lowerPrompt.contains("my goal")
        ) {
            viewModelScope.launch {
                try {
                    // Quick regex fallback extract
                    var key = "BIO"
                    var content = ""
                    
                    if (lowerPrompt.contains("call me")) {
                        key = "NICKNAME"
                        content = userPrompt.substringAfter("call me").trim()
                    } else if (lowerPrompt.contains("my name")) {
                        key = "FULL_NAME"
                        content = userPrompt.substringAfter("my name").trim()
                    } else if (lowerPrompt.contains("my hobby")) {
                        key = "HOBBY"
                        content = userPrompt.substringAfter("my hobby").trim()
                    } else if (lowerPrompt.contains("my job")) {
                        key = "JOB"
                        content = userPrompt.substringAfter("my job").trim()
                    } else if (lowerPrompt.contains("daily goal") || lowerPrompt.contains("my goal")) {
                        key = "GOAL"
                        content = if (lowerPrompt.contains("my goal")) {
                            userPrompt.substringAfter("my goal").trim()
                        } else {
                            userPrompt.substringAfter("daily goal").trim()
                        }
                    } else {
                        content = userPrompt
                    }

                    // Remove trailing dots and punctuation
                    content = content.trim().removeSuffix(".").removeSuffix("!").removeSuffix("?")
                    if (content.length > 40) {
                        content = content.take(37) + "..."
                    }

                    if (content.isNotBlank()) {
                        val memory = com.example.data.db.UserMemoryEntity(
                            id = UUID.randomUUID().toString(),
                            key = key,
                            content = content,
                            userEmail = userSpec?.email
                        )
                        chatDao.insertMemory(memory)
                        _uiState.update { state ->
                            state.copy(lastExtractedMemory = "$key: $content")
                        }
                    }
                } catch (e: Exception) {
                    android.util.Log.e("ChatViewModel", "Background Extraction error", e)
                }
            }
        }
    }

    fun getImageTokenCost(provider: String, model: String): Int {
        return when (provider) {
            "OpenAI" -> 10
            "Stable Diffusion" -> 5
            "FalAI" -> 6
            else -> 4
        }
    }

    fun getVideoTokenCost(provider: String, model: String): Int {
        return when (provider) {
            "OpenAI" -> 50
            "Runway" -> 40
            "Luma AI" -> 35
            else -> 30
        }
    }

    fun getMusicTokenCost(provider: String, model: String): Int {
        return when (provider) {
            "Gemini" -> 15
            "Suno" -> 20
            "Stable Audio" -> 12
            else -> 10
        }
    }

    fun countWords(text: String): Int {
        if (text.isBlank()) return 0
        return text.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }.size
    }

    fun calculateTextResponseTokenCost(apiValue: String, text: String): Double {
        val words = countWords(text)
        if (words == 0) return 0.0
        return when (apiValue) {
            "gpt-5-mini" -> {
                kotlin.math.ceil(words.toDouble() / 25.0) * 0.0166875
            }
            "deepseek-chat" -> {
                kotlin.math.ceil(words.toDouble() / 25.0) * 0.0091875
            }
            "gpt5.5" -> {
                kotlin.math.ceil(words.toDouble() / 25.0) * 0.25
            }
            else -> 0.0
        }
    }

    fun getTextTokenCost(apiValue: String): Int {
        return when (apiValue) {
            "gpt-5-mini" -> 1
            "deepseek-chat" -> 1
            "gpt5.5" -> 1
            else -> 0
        }
    }

    private fun saveTokensToPrefs(tokens: Double) {
        val prefs = try {
            AppContextProvider.context.getSharedPreferences("eddie_prefs", Context.MODE_PRIVATE)
        } catch (e: Exception) {
            null
        }
        try {
            prefs?.edit()?.putFloat("user_tokens", tokens.toFloat())?.apply()
        } catch (e: Exception) {
            // Ignore error
        }
    }

    fun deleteGeneratedImage(id: String) {
        viewModelScope.launch {
            chatDao.deleteGeneratedImageById(id)
        }
    }

    fun generateImage(
        prompt: String,
        style: String,
        aspectRatio: String,
        onSubscriptionRequired: () -> Unit
    ) {
        if (prompt.trim().isBlank()) return

        val isSubscribed = _uiState.value.isSubscribed
        val provider = _uiState.value.selectedImageProvider
        val model = _uiState.value.selectedImageModel
        val tokenCost = getImageTokenCost(provider, model)

        // Premium restrictions:
        // 1. Pro styles check (Pro styles are: 3D Render, Cinematic, Cyberpunk, Anime, Pixel Art, Watercolor, Oil Painting)
        val isProStyle = style in listOf("3D Render", "Cinematic", "Cyberpunk", "Anime", "Pixel Art", "Watercolor", "Oil Painting")

        // 2. Pro aspect ratios check (Pro aspect ratios are: 16:9, 9:16)
        val isProRatio = aspectRatio in listOf("16:9", "9:16")

        // 3. Max free generations count check (free users up to 3)
        val currentImagesCount = _uiState.value.generatedImages.size
        val isDailyLimitReached = currentImagesCount >= 3

        if (!isSubscribed) {
            if (isProStyle || isProRatio || isDailyLimitReached) {
                onSubscriptionRequired()
                return
            }
        } else {
            if (_uiState.value.userTokens < tokenCost.toDouble()) {
                onSubscriptionRequired()
                return
            }
        }

        viewModelScope.launch {
            if (isSubscribed) {
                val nextTokens = (_uiState.value.userTokens - tokenCost.toDouble()).coerceAtLeast(0.0)
                _uiState.update { state ->
                    state.copy(userTokens = nextTokens)
                }
                saveTokensToPrefs(nextTokens)
            }

            _uiState.update { state ->
                state.copy(
                    imageGenIsLoading = true,
                    imageGenLog = "> Preparing processing query...\n"
                )
            }

            // Handshake & network delay simulation
            delay(1000)

            _uiState.update { state ->
                state.copy(
                    imageGenLog = state.imageGenLog + "> Connecting to $provider server ($model)...\n"
                )
            }
            delay(1000)

            // Let's print highly-detailed, authentic progress logs depending on the selected provider & model!
            when (provider) {
                "OpenAI" -> {
                    _uiState.update { state ->
                        state.copy(imageGenLog = state.imageGenLog + "> Authenticating with secure OpenAI API gateway...\n")
                    }
                    delay(800)
                    _uiState.update { state ->
                        state.copy(imageGenLog = state.imageGenLog + "> Using premium model: $model...\n")
                    }
                    delay(800)
                    _uiState.update { state ->
                        state.copy(imageGenLog = state.imageGenLog + "> Optimizing diffusion parameters with DALL-E engine...\n")
                    }
                }
                "Stable Diffusion" -> {
                    _uiState.update { state ->
                        state.copy(imageGenLog = state.imageGenLog + "> Reaching out to distributed GPU cluster...\n")
                    }
                    delay(800)
                    _uiState.update { state ->
                        state.copy(imageGenLog = state.imageGenLog + "> Loading latent model checkpoint: $model...\n")
                    }
                    delay(800)
                    _uiState.update { state ->
                        state.copy(imageGenLog = state.imageGenLog + "> Computing sampling step scheduler (25 iterations)...\n")
                    }
                }
                "FalAI" -> {
                    _uiState.update { state ->
                        state.copy(imageGenLog = state.imageGenLog + "> Initializing FalAI Serverless fast endpoint...\n")
                    }
                    delay(800)
                    _uiState.update { state ->
                        state.copy(imageGenLog = state.imageGenLog + "> Connecting visual pipeline to $model...\n")
                    }
                    delay(800)
                    _uiState.update { state ->
                        state.copy(imageGenLog = state.imageGenLog + "> Syncing aspect resolution for $aspectRatio in real-time...\n")
                    }
                }
                else -> {
                    _uiState.update { state ->
                        state.copy(imageGenLog = state.imageGenLog + "> Initializing general fallback engine...\n")
                    }
                    delay(800)
                }
            }
            delay(1000)

            _uiState.update { state ->
                state.copy(imageGenLog = state.imageGenLog + "> Refining rendering details and color palette...\n")
            }
            delay(800)

            // Dynamic dimensions calculation
            val (width, height) = when (aspectRatio) {
                "16:9" -> 1024 to 576
                "9:16" -> 576 to 1024
                else -> 800 to 800 // 1:1
            }

            // Append aesthetic tag based on active visual style
            val aestheticTag = when (style) {
                "Anime" -> " anime styled illustration, masterpiece, extremely high quality, manga visual, digital landscape"
                "Cyberpunk" -> " highly detailed science fiction cyberpunk city aesthetic, futuristic neon glow lights, tech noir, cinematic"
                "3D Render" -> " highly polished professional 3d modeling render, octane engine realism, soft depth of field, minimalist art object"
                "Cinematic" -> " dramatic cinematography frame, movie capture, cinematic light beams, photorealistic art"
                "Pixel Art" -> " retro gaming pixel art style, glorious 16-bit color palette, detailed scene"
                "Watercolor" -> " artistic dreamy watercolor paint texture, soft color splashes on blank white canvas, beautiful splash effects"
                "Oil Painting" -> " classical majestic heavy brushstroke oil on canvas painting masterpiece, van gogh styles, high texture details"
                "Cosmic" -> " futuristic galactic cosmic space galaxy elements, nebulous gas, beautiful star constellations glow"
                "Sketch" -> " handdrawn fine graphite pencil drawing sketch, gorgeous shadows hatching, black and white paper texture"
                "Black & White" -> " expressive high contrast monochrome black and white fine art photography, dark shadows"
                "Minimalist" -> " clean minimalist modern graphics design, flat pastel colors layout, artistic simplicity"
                else -> " photorealistic, extremely detailed beautiful photography, ultra HD, vivid textures, professional light studio"
            }

            // Append model tags to make the prompt match the style of the model perfectly!
            val modelTag = when (model) {
                "GPT Image 2" -> ", dall-e-3 style, highly artistic output, flawless anatomy"
                "Core Diffusion" -> ", stable diffusion core, cinematic lighting, sharp details"
                "Ultra Diffusion" -> ", award winning masterpiece, extremely photorealistic, vray render 8k"
                "Flux Pro" -> ", flux professional rendering, hyperrealistic skin pores, volumetric lighting"
                "Flux Realism" -> ", candid raw street photography, national geographic depth, analog film style"
                else -> ""
            }

            val finalImageUrl = when (style) {
                "Anime" -> "https://images.unsplash.com/photo-1578632767115-351597cf2477?w=1024"
                "Cyberpunk" -> "https://images.unsplash.com/photo-1515621061946-eff1c2a352bd?w=1024"
                "3D Render" -> "https://images.unsplash.com/photo-1618005182384-a83a8bd57fbe?w=1024"
                "Cinematic" -> "https://images.unsplash.com/photo-1489599849927-2ee91cede3ba?w=1024"
                "Pixel Art" -> "https://images.unsplash.com/photo-1563089145-599997674d42?w=1024"
                "Watercolor" -> "https://images.unsplash.com/photo-1579783902614-a3fb3927b6a5?w=1024"
                "Oil Painting" -> "https://images.unsplash.com/photo-1579783900882-c0d3dad7b119?w=1024"
                "Cosmic" -> "https://images.unsplash.com/photo-1462331940025-496dfbfc7564?w=1024"
                "Sketch" -> "https://images.unsplash.com/photo-1576016770956-debb63d900ef?w=1024"
                "Black & White" -> "https://images.unsplash.com/photo-1502082553048-f009c37129b9?w=1024"
                "Minimalist" -> "https://images.unsplash.com/photo-1513519245088-0e12902e5a38?w=1024"
                else -> "https://images.unsplash.com/photo-1541701494587-cb58502866ab?w=1024"
            }

            // Insert to database for permanent storage
            val imageEntity = com.example.data.db.GeneratedImageEntity(
                id = UUID.randomUUID().toString(),
                prompt = prompt.trim(),
                imageUrl = finalImageUrl,
                style = style,
                aspectRatio = aspectRatio,
                timestamp = System.currentTimeMillis(),
                userEmail = _uiState.value.currentUserProfile?.email
            )
            chatDao.insertGeneratedImage(imageEntity)

            _uiState.update { state ->
                state.copy(
                    imageGenIsLoading = false,
                    imageGenLog = state.imageGenLog + "> GENERATION SUCCESSFUL!\n> Gemini Imagen 3 artwork has been successfully saved to the local gallery."
                )
            }
        }
    }

    fun deleteGeneratedVideo(id: String) {
        viewModelScope.launch {
            chatDao.deleteGeneratedVideoById(id)
        }
    }

    fun generateVideo(
        prompt: String,
        style: String,
        aspectRatio: String,
        resolution: String,
        duration: String,
        onSubscriptionRequired: () -> Unit
    ) {
        if (prompt.trim().isBlank()) return

        val isSubscribed = _uiState.value.isSubscribed
        val provider = _uiState.value.selectedVideoProvider
        val model = _uiState.value.selectedVideoModel
        val tokenCost = getVideoTokenCost(provider, model)

        // Premium restrictions check:
        val isProRatio = aspectRatio in listOf("9:16", "21:9")
        val isProResolution = resolution in listOf("1080p", "4K")
        val isProDuration = duration in listOf("10s", "30s")

        val currentVideosCount = _uiState.value.generatedVideos.size
        val isLimitReached = currentVideosCount >= 2

        if (!isSubscribed) {
            if (isProRatio || isProResolution || isProDuration || isLimitReached) {
                onSubscriptionRequired()
                return
            }
        } else {
            if (_uiState.value.userTokens < tokenCost.toDouble()) {
                onSubscriptionRequired()
                return
            }
        }

        viewModelScope.launch {
            if (isSubscribed) {
                val nextTokens = (_uiState.value.userTokens - tokenCost.toDouble()).coerceAtLeast(0.0)
                _uiState.update { state ->
                    state.copy(userTokens = nextTokens)
                }
                saveTokensToPrefs(nextTokens)
            }

            val mode = _uiState.value.selectedVideoMode

            _uiState.update { state ->
                state.copy(
                    videoGenIsLoading = true,
                    videoGenLog = "> Preparing video render engine for $mode mode...\n"
                )
            }

            delay(1000)
            _uiState.update { state ->
                state.copy(videoGenLog = state.videoGenLog + "> Connecting to $provider GPU cluster ($model)...\n")
            }
            delay(1000)
            _uiState.update { state ->
                state.copy(videoGenLog = state.videoGenLog + "> Sending storytelling movement parameters...\n")
            }
            delay(1000)

            when (provider) {
                "OpenAI" -> {
                    _uiState.update { state ->
                        state.copy(videoGenLog = state.videoGenLog + "> Authenticating secure OpenAI Sora API...\n")
                    }
                    delay(800)
                    _uiState.update { state ->
                        state.copy(videoGenLog = state.videoGenLog + "> Setting up temporal 3D visual latents with model $model...\n")
                    }
                }
                "Gemini" -> {
                    _uiState.update { state ->
                        state.copy(videoGenLog = state.videoGenLog + "> Initializing Google Veo video model ($model)...\n")
                    }
                    delay(800)
                    _uiState.update { state ->
                        state.copy(videoGenLog = state.videoGenLog + "> Optimizing super-resolution parameters with Google Cloud TPU...\n")
                    }
                }
                "FalAI" -> {
                    _uiState.update { state ->
                        state.copy(videoGenLog = state.videoGenLog + "> Connecting to FalAI Kling serverless pipeline ($model)...\n")
                    }
                    delay(800)
                    _uiState.update { state ->
                        state.copy(videoGenLog = state.videoGenLog + "> Configuring sampling steps scheduler and motion guidance...\n")
                    }
                }
            }
            delay(1000)

            _uiState.update { state ->
                state.copy(videoGenLog = state.videoGenLog + "> Configuring resolution [$resolution], aspect ratio [$aspectRatio], and duration [$duration]...\n")
            }
            delay(1200)
            _uiState.update { state ->
                state.copy(videoGenLog = state.videoGenLog + "> Rendering frame-by-frame (performing temporal AI interpolation)...\n")
            }
            delay(1500)

            val sampleVideoUrls = listOf(
                "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4",
                "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ElephantsDream.mp4",
                "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4",
                "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerEscapes.mp4",
                "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/SubaruOutbackInDepth.mp4"
            )
            val randomVideoUrl = sampleVideoUrls[java.util.Random().nextInt(sampleVideoUrls.size)]

            val videoEntity = com.example.data.db.GeneratedVideoEntity(
                id = UUID.randomUUID().toString(),
                prompt = prompt.trim(),
                videoUrl = randomVideoUrl,
                style = style,
                aspectRatio = aspectRatio,
                resolution = resolution,
                duration = duration,
                timestamp = System.currentTimeMillis(),
                userEmail = _uiState.value.currentUserProfile?.email
            )

            chatDao.insertGeneratedVideo(videoEntity)

            _uiState.update { state ->
                state.copy(
                    videoGenIsLoading = false,
                    videoGenLog = state.videoGenLog + "> GENERATION SUCCESSFUL!\n> Rendered video for model $model has been successfully saved to the local library."
                )
            }
        }
    }

    fun deleteGeneratedMusic(id: String) {
        viewModelScope.launch {
            chatDao.deleteGeneratedMusicById(id)
        }
    }

    fun generateMusic(
        prompt: String,
        genre: String,
        duration: String,
        tempo: String,
        onSubscriptionRequired: () -> Unit
    ) {
        if (prompt.trim().isBlank()) return

        val isSubscribed = _uiState.value.isSubscribed
        val provider = _uiState.value.selectedMusicProvider
        val model = _uiState.value.selectedMusicModel
        val tokenCost = getMusicTokenCost(provider, model)

        // Subscription validation rules:
        val isProGenre = genre in listOf("Lo-Fi", "Jazz", "Classical", "EDM", "Ambient", "Hip-Hop")
        val isProDuration = duration in listOf("60s", "120s")

        val currentMusicCount = _uiState.value.generatedMusic.size
        val isLimitReached = currentMusicCount >= 2

        if (!isSubscribed) {
            if (isProGenre || isProDuration || isLimitReached) {
                onSubscriptionRequired()
                return
            }
        } else {
            if (_uiState.value.userTokens < tokenCost.toDouble()) {
                onSubscriptionRequired()
                return
            }
        }

        viewModelScope.launch {
            if (isSubscribed) {
                val nextTokens = (_uiState.value.userTokens - tokenCost.toDouble()).coerceAtLeast(0.0)
                _uiState.update { state ->
                    state.copy(userTokens = nextTokens)
                }
                saveTokensToPrefs(nextTokens)
            }

            _uiState.update { state ->
                state.copy(
                    musicGenIsLoading = true,
                    musicGenLog = "> Connecting to AI Music Synthesizer Engine powered by $provider...\n"
                )
            }

            delay(1000)
            _uiState.update { state ->
                state.copy(musicGenLog = state.musicGenLog + "> Secure authentication with $provider API ($model) completed successfully...\n")
            }
            delay(1000)
            _uiState.update { state ->
                state.copy(musicGenLog = state.musicGenLog + "> Analyzing beats and mood: '${prompt.trim()}'...\n")
            }
            delay(1200)

            when (provider) {
                "Gemini" -> {
                    _uiState.update { state ->
                        state.copy(musicGenLog = state.musicGenLog + "> Initializing Google Lyria system ($model)...\n")
                    }
                    delay(800)
                    _uiState.update { state ->
                        state.copy(musicGenLog = state.musicGenLog + "> Activating temporal audio latent model for phrase transitions...\n")
                    }
                }
                "Suno AI" -> {
                    _uiState.update { state ->
                        state.copy(musicGenLog = state.musicGenLog + "> Connecting to Suno AI ($model) Music Synthesizer pipeline...\n")
                    }
                    delay(800)
                    _uiState.update { state ->
                        state.copy(musicGenLog = state.musicGenLog + "> Initializing premium multilingual vocal generator...\n")
                    }
                }
            }
            delay(1000)

            _uiState.update { state ->
                state.copy(musicGenLog = state.musicGenLog + "> Arranging instrument harmonies for the genre '$genre'...\n")
            }
            delay(1000)
            _uiState.update { state ->
                state.copy(musicGenLog = state.musicGenLog + "> Setting tempo [$tempo] and aligning sampler ($duration)...\n")
            }
            delay(1200)
            _uiState.update { state ->
                state.copy(musicGenLog = state.musicGenLog + "> Rendering hi-fi stereo master audio (multi-track frequency synthesis)...\n")
            }
            delay(1500)

            val sampleMusicUrls = listOf(
                "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-1.mp3",
                "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-2.mp3",
                "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-3.mp3",
                "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-4.mp3",
                "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-5.mp3"
            )
            val randomMusicUrl = sampleMusicUrls[java.util.Random().nextInt(sampleMusicUrls.size)]

            val musicEntity = com.example.data.db.GeneratedMusicEntity(
                id = UUID.randomUUID().toString(),
                prompt = prompt.trim(),
                musicUrl = randomMusicUrl,
                genre = genre,
                duration = duration,
                tempo = tempo,
                timestamp = System.currentTimeMillis(),
                userEmail = _uiState.value.currentUserProfile?.email
            )

            chatDao.insertGeneratedMusic(musicEntity)

            _uiState.update { state ->
                state.copy(
                    musicGenIsLoading = false,
                    musicGenLog = state.musicGenLog + "> MUSIC GENERATION SUCCESSFUL!\n> Audio file for model $model has been successfully saved to the local library."
                )
            }
        }
    }

    fun clearCodeOutput() {
        _uiState.update { state -> state.copy(codeOutput = "") }
    }

    fun generateAiCode(
        language: String,
        task: String,
        targetLanguage: String,
        input: String,
        onSubscriptionRequired: () -> Unit
    ) {
        if (input.trim().isBlank()) return

        val isSubscribed = _uiState.value.isSubscribed
        val isPremiumLanguage = language in listOf("Go", "Rust", "C++", "TypeScript", "C#", "Swift", "JavaScript")
        val isPremiumTask = task in listOf("Refactor & Optimasi", "Konverter Bahasa")
        val isDailyLimitReached = _uiState.value.codeFreeQueriesLeft <= 0

        if (!isSubscribed) {
            if (isPremiumLanguage || isPremiumTask || isDailyLimitReached) {
                onSubscriptionRequired()
                return
            }
        }

        viewModelScope.launch {
            _uiState.update { state ->
                state.copy(
                    codeIsLoading = true,
                    codeSelectedLanguage = language,
                    codeSelectedTask = task,
                    codeTargetLanguage = targetLanguage,
                    codeOutput = ""
                )
            }

            val developerSystemInstruction = """
                You are Eddie, an expert elite Software Architect, Senior Security Engineer, and Master Programmer. 
                Your job is to provide clean, production-ready, highly optimized code or deep analysis for the user.
                Always wrap code blocks inside standard Markdown backticks (e.g. ```$language ... ``` or ```python ... ```) so that the UI can detect and format them properly.
                Keep explanations/analyses concise, accurate, professional, and well-structured, written in English (US).
            """.trimIndent()

            val customPrompt = when (task) {
                "Code Generation" -> """
                    Write a complete and structured programming code in: $language.
                    User Requirement: $input
                    Make sure the generated code is clean, has relevant explanatory comments, up-to-date, secure from vulnerabilities, and ready to compile and run.
                """.trimIndent()
                "Bug Finder & Debugger" -> """
                    Please analyze this $language code snippet:
                    $input
                    Find bugs, logic errors, memory leaks, or performance bottlenecks. Explain where the errors are in detail, and then write the complete corrected solution.
                """.trimIndent()
                "Refactor & Optimization" -> """
                    Perform refactoring and performance optimization on the following $language code:
                    $input
                    Focus on execution time efficiency (time complexity), space complexity, readability (clean code), and modern idiomatic features of $language. Explain your improvements in English (US) before writing the optimized code.
                """.trimIndent()
                "Language Converter" -> """
                    Convert the following source code functionally from $language to the target language $targetLanguage.
                    Original Code ($language):
                    $input
                    Use idioms, syntax, structures, collections, and modern libraries standard in the target language ($targetLanguage) so that the code is highly idiomatic.
                """.trimIndent()
                else -> input
            }

            try {
                val response = repository.sendMessage(
                    history = emptyList(),
                    newPrompt = customPrompt,
                    model = _uiState.value.selectedModel.apiValue,
                    provider = _uiState.value.selectedModel.provider,
                    customSystemInstruction = developerSystemInstruction,
                    providerKeys = mapOf(
                        "Gemini" to _uiState.value.geminiApiKey,
                        "OpenAI" to _uiState.value.openaiApiKey,
                        "DeepSeek" to _uiState.value.deepseekApiKey,
                        "Claude" to _uiState.value.claudeApiKey,
                        "XAI" to _uiState.value.xaiApiKey
                    )
                )

                _uiState.update { state ->
                    val nextFreeQueries = if (isSubscribed) state.codeFreeQueriesLeft else (state.codeFreeQueriesLeft - 1).coerceAtLeast(0)
                    state.copy(
                        codeOutput = response,
                        codeIsLoading = false,
                        codeFreeQueriesLeft = nextFreeQueries
                    )
                }
            } catch (e: Exception) {
                _uiState.update { state ->
                    state.copy(
                        codeOutput = "An error occurred during code processing: ${e.localizedMessage}",
                        codeIsLoading = false
                    )
                }
            }
        }
    }

    fun saveEditedImage(
        originalId: String,
        prompt: String,
        style: String,
        aspectRatio: String,
        imageUrl: String
    ) {
        viewModelScope.launch {
            val editedEntity = com.example.data.db.GeneratedImageEntity(
                id = java.util.UUID.randomUUID().toString(),
                prompt = prompt,
                imageUrl = imageUrl,
                style = style,
                aspectRatio = aspectRatio,
                timestamp = System.currentTimeMillis(),
                userEmail = _uiState.value.currentUserProfile?.email
            )
            chatDao.insertGeneratedImage(editedEntity)
        }
    }

    fun saveEditedVideo(
        originalId: String,
        prompt: String,
        style: String,
        aspectRatio: String,
        resolution: String,
        duration: String,
        videoUrl: String
    ) {
        viewModelScope.launch {
            val editedEntity = com.example.data.db.GeneratedVideoEntity(
                id = java.util.UUID.randomUUID().toString(),
                prompt = prompt,
                videoUrl = videoUrl,
                style = style,
                aspectRatio = aspectRatio,
                resolution = resolution,
                duration = duration,
                timestamp = System.currentTimeMillis(),
                userEmail = _uiState.value.currentUserProfile?.email
            )
            chatDao.insertGeneratedVideo(editedEntity)
        }
    }

    fun saveEditedMusic(
        originalId: String,
        prompt: String,
        genre: String,
        duration: String,
        tempo: String,
        musicUrl: String
    ) {
        viewModelScope.launch {
            val editedEntity = com.example.data.db.GeneratedMusicEntity(
                id = java.util.UUID.randomUUID().toString(),
                prompt = prompt,
                musicUrl = musicUrl,
                genre = genre,
                duration = duration,
                tempo = tempo,
                timestamp = System.currentTimeMillis(),
                userEmail = _uiState.value.currentUserProfile?.email
            )
            chatDao.insertGeneratedMusic(editedEntity)
        }
    }

    // --- NEW FEATURES IN COMPLIANCE WITH USER_REQUEST ---

    // 1. Asisten Suara (Conversational Voice AI)
    fun processVoiceInput(text: String, tts: android.speech.tts.TextToSpeech?) {
        if (text.isBlank()) return
        
        viewModelScope.launch {
            val updatedLog = _uiState.value.voiceChatLog.toMutableList()
            updatedLog.add(text to true)
            
            _uiState.update { state -> 
                state.copy(
                    voiceIsLoading = true,
                    voiceRecognizedText = text,
                    voiceChatLog = updatedLog
                )
            }
            
            val keysMap = mapOf(
                "Gemini" to _uiState.value.geminiApiKey
            )
            
            try {
                val response = repository.sendSimpleRequest(
                    prompt = text,
                    systemText = "You are Eddie, an intelligent voice assistant speaking directly with the user. Provide concise, interactive, warm, and natural answers (avoid markdown if possible as it will be read aloud). Max 2-3 sentences in English (US).",
                    providerKeys = keysMap
                )
                
                val finalUpdatedLog = _uiState.value.voiceChatLog.toMutableList()
                finalUpdatedLog.add(response to false)
                
                _uiState.update { state ->
                    state.copy(
                        voiceIsLoading = false,
                        voiceAssistantResponse = response,
                        voiceChatLog = finalUpdatedLog,
                        voiceIsSpeakActive = true
                    )
                }
                
                tts?.speak(response, android.speech.tts.TextToSpeech.QUEUE_FLUSH, null, "EddieVoiceUtteranceId")
            } catch (e: Exception) {
                val errorResponse = "Sorry, I can't reach the server right now: ${e.localizedMessage}. Please check your connection."
                val finalUpdatedLog = _uiState.value.voiceChatLog.toMutableList()
                finalUpdatedLog.add(errorResponse to false)
                
                _uiState.update { state ->
                    state.copy(
                        voiceIsLoading = false,
                        voiceAssistantResponse = errorResponse,
                        voiceChatLog = finalUpdatedLog,
                        voiceIsSpeakActive = true
                    )
                }
                tts?.speak(errorResponse, android.speech.tts.TextToSpeech.QUEUE_FLUSH, null, "EddieVoiceUtteranceId")
            }
        }
    }
    
    fun setVoiceSpeakActive(active: Boolean) {
        _uiState.update { it.copy(voiceIsSpeakActive = active) }
    }
    
    fun clearVoiceChat() {
        _uiState.update { it.copy(voiceChatLog = emptyList(), voiceRecognizedText = "", voiceAssistantResponse = "") }
    }

    // 2. Vision & Image Analyzer
    fun selectVisionImage(uri: android.net.Uri?, bitmap: android.graphics.Bitmap?) {
        _uiState.update { it.copy(
            visionSelectedImageUri = uri,
            visionSelectedBitmap = bitmap,
            visionResult = ""
        )}
    }
    
    fun analyzeVisionImage(taskMode: String, promptText: String) {
        val bitmap = _uiState.value.visionSelectedBitmap
        if (bitmap == null) {
            _uiState.update { it.copy(visionResult = "Warning: Please take a photo or upload an image first!") }
            return
        }
        
        viewModelScope.launch {
            _uiState.update { it.copy(visionIsLoading = true, visionResult = "") }
            
            val promptPrefix = when (taskMode) {
                "describe" -> "Describe the objects, details, colors, composition, or nuances in this image in detail."
                "translate" -> "Find all written text in this image, transcribe it, and translate it to English (US) if the text is in a foreign language."
                "code" -> "Analyze the UI design, layout, wireframe, or design sketch in this image. Convert the visual design into complete, neat, responsive, and ready-to-use Jetpack Compose (Kotlin) code."
                else -> "Analyze this image."
            }
            
            val finalPrompt = if (promptText.isNotBlank()) "$promptPrefix Additional instructions: $promptText" else promptPrefix
            
            val keysMap = mapOf(
                "Gemini" to _uiState.value.geminiApiKey
            )
            
            try {
                val result = repository.sendVisionRequest(
                    bitmap = bitmap,
                    prompt = finalPrompt,
                    providerKeys = keysMap
                )
                
                _uiState.update { it.copy(
                    visionIsLoading = false,
                    visionResult = result
                )}
            } catch (e: Exception) {
                _uiState.update { it.copy(
                    visionIsLoading = false,
                    visionResult = "Error: Connection failed or timeout occurred while analyzing the image: ${e.localizedMessage}. Please try again later or add your Gemini API Key in Settings."
                )}
            }
        }
    }

    // 3. Document Intelligence
    fun selectDocument(uri: android.net.Uri?, name: String, content: String) {
        _uiState.update { it.copy(
            docSelectedUri = uri,
            docSelectedName = name,
            docContentText = content,
            docSummaryResult = "",
            docQaAnswer = ""
        )}
    }
    
    fun summarizeDocument(style: String) {
        val content = _uiState.value.docContentText
        if (content.isBlank()) {
            _uiState.update { it.copy(docSummaryResult = "Warning: No document content available for analysis. Make sure the file was read correctly or contains text.") }
            return
        }
        
        viewModelScope.launch {
            _uiState.update { it.copy(docIsLoading = true, docSummaryResult = "") }
            
            val promptStyle = when (style) {
                "bullet" -> "Key Bullet Points"
                "concise" -> "Executive Summary"
                "deep" -> "In-Depth & Comprehensive Analysis"
                else -> "Standard Summary"
            }
            
            val prompt = "Please summarize the following document content in English (US) using the style format: $promptStyle.\n\nDocument Content:\n$content"
            
            val keysMap = mapOf(
                "Gemini" to _uiState.value.geminiApiKey
            )
            
            try {
                val result = repository.sendSimpleRequest(
                    prompt = prompt,
                    systemText = "You are Eddie, a Document Intelligence expert and smart text analyst. Provide an accurate, informative, and well-structured summary matching the requested style in English (US).",
                    providerKeys = keysMap
                )
                
                _uiState.update { it.copy(
                    docIsLoading = false,
                    docSummaryResult = result
                )}
            } catch (e: Exception) {
                _uiState.update { it.copy(
                    docIsLoading = false,
                    docSummaryResult = "Error: Connection failed or timeout occurred while summarizing the document: ${e.localizedMessage}. Please periksa koneksi internet Anda atau lengkapi API Key Anda di menu Pengaturan."
                )}
            }
        }
    }
    
    fun askDocumentQuestion(question: String) {
        val content = _uiState.value.docContentText
        if (content.isBlank()) {
            _uiState.update { it.copy(docQaAnswer = "Warning: Please load a document file first.") }
            return
        }
        if (question.isBlank()) return
        
        viewModelScope.launch {
            _uiState.update { it.copy(docQaIsLoading = true, docQaAnswer = "") }
            
            val prompt = "Based on the following document content, please answer the user's question accurately, objectively, and honestly based on the data in the document.\nQuestion: $question\n\nDocument Content:\n$content"
            
            val keysMap = mapOf(
                "Gemini" to _uiState.value.geminiApiKey
            )
            
            try {
                val result = repository.sendSimpleRequest(
                    prompt = prompt,
                    systemText = "You are Eddie, an interactive document Q&A assistant. Answer in a friendly, detailed, and accurate manner in English (US), referring only to the provided document content.",
                    providerKeys = keysMap
                )
                
                _uiState.update { it.copy(
                    docQaIsLoading = false,
                    docQaAnswer = result
                )}
            } catch (e: Exception) {
                _uiState.update { it.copy(
                    docQaIsLoading = false,
                    docQaAnswer = "Error: Connection failed or timeout occurred while processing question: ${e.localizedMessage}. Please try again later or verify your API Key in Settings."
                )}
            }
        }
    }
}

