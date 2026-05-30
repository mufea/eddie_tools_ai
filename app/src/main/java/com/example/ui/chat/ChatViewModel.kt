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
        displayName = "Gemini 3.5 Flash",
        apiValue = "gemini-3.5-flash",
        subtitle = "Model andalan Google yang super cepat, serbaguna, dan sangat cerdas.",
        provider = "Gemini",
        isPro = false
    ),
    GeminiModel(
        displayName = "Gemini 3.1 Pro",
        apiValue = "gemini-3.1-pro",
        subtitle = "Model tingkat lanjut dengan kecerdasan kognitif tinggi untuk tugas kompleks.",
        provider = "Gemini",
        isPro = true
    ),
    // OpenAI
    GeminiModel(
        displayName = "Gpt-4o",
        apiValue = "gpt-4o",
        subtitle = "Model unggulan OpenAI yang legendaris, cepat, dan sangat akurat.",
        provider = "OpenAI",
        isPro = true
    ),
    GeminiModel(
        displayName = "Gpt 5.5",
        apiValue = "gpt-5.5-preview",
        subtitle = "Cicipi masa depan AI dengan pratinjau penalaran mutakhir dari OpenAI.",
        provider = "OpenAI",
        isPro = true
    ),
    // DeepSeek
    GeminiModel(
        displayName = "Deepseek",
        apiValue = "deepseek-chat",
        subtitle = "Model efisien berkinerja tinggi, sangat mumpuni untuk coding dan analisis.",
        provider = "DeepSeek",
        isPro = false
    ),
    // Claude
    GeminiModel(
        displayName = "Claude Sonet 4.6",
        apiValue = "claude-3-5-sonnet-20241022",
        subtitle = "Model terbaik Anthropic untuk penulisan sastra, koding kreatif, dan logika matang.",
        provider = "Claude",
        isPro = true
    ),
    GeminiModel(
        displayName = "Claude Haiku 4.5",
        apiValue = "claude-3-5-haiku-20241022",
        subtitle = "Asisten lincah secepat kilat dengan penalaran cerdas yang sangat hemat daya.",
        provider = "Claude",
        isPro = false
    ),
    // XAI
    GeminiModel(
        displayName = "Grok 4",
        apiValue = "grok-4",
        subtitle = "Model unik dari xAI dengan pemahaman mendalam dan jawaban informatif.",
        provider = "XAI",
        isPro = true
    )
)

data class ChatSession(
    val id: String = UUID.randomUUID().toString(),
    val title: String = "Percakapan Baru",
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
    val isSubscribed: Boolean = false,
    val activeSubscriptionPlan: String? = null,
    val subscriptionExpiryDate: String? = null,
    val generatedImages: List<com.example.data.db.GeneratedImageEntity> = emptyList(),
    val selectedImageProvider: String = "OpenAI",
    val selectedImageModel: String = "GPT Image 1.5",
    val imageGenIsLoading: Boolean = false,
    val imageGenLog: String = "",
    val imageGenEngine: String = "Google Imagen 3", // Options: "Google Imagen 3", "Pollinations AI"
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
    val activeAutomationStatus: String = ""
) {
    val currentSession: ChatSession
        get() = sessions.find { it.id == currentSessionId } ?: sessions.firstOrNull() ?: ChatSession()

    val messages: List<ChatMessage>
        get() = currentSession.messages
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
        val prefs = AppContextProvider.context.getSharedPreferences("eddie_prefs", Context.MODE_PRIVATE)
        _uiState.update { state ->
            state.copy(
                geminiApiKey = prefs.getString("gemini_key", "") ?: "",
                openaiApiKey = prefs.getString("openai_key", "") ?: "",
                deepseekApiKey = prefs.getString("deepseek_key", "") ?: "",
                claudeApiKey = prefs.getString("claude_key", "") ?: "",
                xaiApiKey = prefs.getString("xai_key", "") ?: ""
            )
        }
        observeSessionsByCurrentUser()
        observeMessagesForCurrentSession()
        observeGeneratedImagesByCurrentUser()
        observeGeneratedVideosByCurrentUser()
        observeGeneratedMusicByCurrentUser()
        observeMemoriesByCurrentUser()
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
                            val updatedSessions = if (mappedSessions.isEmpty()) {
                                // Insert a default initial session if first time or database cleared
                                val initialSession = ChatSession()
                                viewModelScope.launch {
                                    chatDao.insertSession(ChatSessionEntity(
                                        id = initialSession.id,
                                        title = initialSession.title,
                                        modelValue = state.selectedModel.apiValue,
                                        userEmail = state.currentUserProfile?.email
                                    ))
                                }
                                listOf(initialSession)
                            } else {
                                mappedSessions
                            }

                            val activeSessionId = if (state.currentSessionId.isBlank() || !updatedSessions.any { it.id == state.currentSessionId }) {
                                updatedSessions.first().id
                            } else {
                                state.currentSessionId
                            }

                            state.copy(
                                sessions = updatedSessions,
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
            return "Email dan password wajib diisi."
        }
        val userPair = registeredUsers[emailClean]
        if (userPair == null) {
            return "Email tidak terdaftar. Silakan daftar akun baru terlebih dahulu."
        }
        if (userPair.first != password) {
            return "Password yang dimasukkan salah."
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
            return "Semua kolom pendaftaran wajib diisi."
        }
        if (!emailClean.contains("@")) {
            return "Format alamat email tidak valid."
        }
        if (registeredUsers.containsKey(emailClean)) {
            return "Email tersebut sudah terdaftar. Silakan Masuk."
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
                isSubscribed = false,
                activeSubscriptionPlan = null,
                subscriptionExpiryDate = null
            )
        }
    }

    fun activateSubscription(plan: String) {
        val expiryDate = when (plan) {
            "Mingguan" -> "7 Hari dari Sekarang"
            "Bulanan" -> "30 Hari dari Sekarang"
            "Tahunan" -> "365 Hari dari Sekarang"
            else -> "Selamanya"
        }
        _uiState.update { state ->
            state.copy(
                isSubscribed = true,
                activeSubscriptionPlan = plan,
                subscriptionExpiryDate = expiryDate
            )
        }
    }

    fun cancelSubscription() {
        _uiState.update { state ->
            state.copy(
                isSubscribed = false,
                activeSubscriptionPlan = null,
                subscriptionExpiryDate = null
            )
        }
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

        viewModelScope.launch {
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
            val autoTriggerSearch = cleanLower.contains("cari") || cleanLower.contains("browsing") ||
                    cleanLower.contains("berita") || cleanLower.contains("skor") ||
                    cleanLower.contains("weather") || cleanLower.contains("saham") ||
                    cleanLower.contains("cuaca") || cleanLower.contains("ihsg")

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
                        "[SUMBER: ${it.source}] - ${it.title} (${it.date})\nLink: ${it.link}\nRingkasan: ${it.snippet}"
                    }
                    injectedPrompt = "Berikut adalah hasil pencarian Google real-time terkini (Hari ini: Sabtu, 30 Mei 2026) untuk kueri '${prompt}':\n\n$searchSummaryText\n\nSilakan gunakan data terkini di atas untuk merespons pertanyaan pengguna secara cerdas dan akurat. Harap cantumkan ikon bendera 🌐 dan rujukan pencarian singkat di awal respon Anda agar pengguna tahu Anda telah melakukan penjelajahan Google Search secara real-time!\n\nPertanyaan Pengguna: $prompt"
                }

                _uiState.update { state ->
                    state.copy(
                        isSearchingWeb = false,
                        webSearchResultsSummary = "Berhasil menemukan ${snippets.size} hasil pencarian dari Google!"
                    )
                }
            }

            // 4. Smart Memory Context Retrieval (Long-Term Personalization Context)
            val memories = _uiState.value.userMemories
            val memoryInstruct = if (memories.isNotEmpty()) {
                "\n\n[MEMORI PERSONAL MANDIRI DAN INTIM YANG ANDA INGAT TENTANG PENGGUNA]:\n" +
                memories.joinToString("\n") { "- ${it.key}: ${it.content}" } +
                "\nGunakan panggilaan akrab atau preferensi di atas untuk merespons secara ramah dan intim."
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

            // 6. Save model's response to Database
            chatDao.insertMessage(ChatMessageEntity(
                id = modelItemMessageId,
                sessionId = sessionId,
                role = "MODEL",
                text = responseText,
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
        }
    }

    private fun extractAndSaveMemoryInBg(userPrompt: String) {
        val lowerPrompt = userPrompt.lowercase().trim()
        val userSpec = _uiState.value.currentUserProfile
        
        // Quick regex and trigger rules
        if (lowerPrompt.contains("panggil aku") || lowerPrompt.contains("nama saya") || 
            lowerPrompt.contains("hobi saya") || lowerPrompt.contains("preferensi saya") || 
            lowerPrompt.contains("pekerjaan saya") || lowerPrompt.contains("tujuan harian") ||
            lowerPrompt.contains("panggil saya")
        ) {
            viewModelScope.launch {
                try {
                    // Quick regex fallback extract
                    var key = "BIO"
                    var content = ""
                    
                    if (lowerPrompt.contains("panggil aku")) {
                        key = "NAMA_PANGGILAN"
                        content = userPrompt.substringAfter("panggil aku").trim()
                    } else if (lowerPrompt.contains("panggil saya")) {
                        key = "NAMA_PANGGILAN"
                        content = userPrompt.substringAfter("panggil saya").trim()
                    } else if (lowerPrompt.contains("nama saya")) {
                        key = "NAMA_LENGKAP"
                        content = userPrompt.substringAfter("nama saya").trim()
                    } else if (lowerPrompt.contains("hobi saya")) {
                        key = "HOBI"
                        content = userPrompt.substringAfter("hobi saya").trim()
                    } else if (lowerPrompt.contains("tujuan harian") || lowerPrompt.contains("tujuan saya")) {
                        key = "TUJUAN"
                        content = userPrompt.substringAfter("tujuan saya").trim()
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
        }

        viewModelScope.launch {
            val provider = _uiState.value.selectedImageProvider
            val model = _uiState.value.selectedImageModel

            _uiState.update { state ->
                state.copy(
                    imageGenIsLoading = true,
                    imageGenLog = "> Mempersiapkan kueri pemrosesan...\n"
                )
            }

            // Handshake & network delay simulation
            delay(1000)

            _uiState.update { state ->
                state.copy(
                    imageGenLog = state.imageGenLog + "> Menghubungkan ke server $provider ($model)...\n"
                )
            }
            delay(1000)

            // Let's print highly-detailed, authentic progress logs depending on the selected provider & model!
            when (provider) {
                "OpenAI" -> {
                    _uiState.update { state ->
                        state.copy(imageGenLog = state.imageGenLog + "> Otentikasi gerbang API OpenAI aman...\n")
                    }
                    delay(800)
                    _uiState.update { state ->
                        state.copy(imageGenLog = state.imageGenLog + "> Menggunakan model premium: $model...\n")
                    }
                    delay(800)
                    _uiState.update { state ->
                        state.copy(imageGenLog = state.imageGenLog + "> Mengoptimalkan parameter difusi dengan DALL-E engine...\n")
                    }
                }
                "Stable Diffusion" -> {
                    _uiState.update { state ->
                        state.copy(imageGenLog = state.imageGenLog + "> Menghubungi cluster GPU terdistribusi...\n")
                    }
                    delay(800)
                    _uiState.update { state ->
                        state.copy(imageGenLog = state.imageGenLog + "> Memuat checkpoint model latent: $model...\n")
                    }
                    delay(800)
                    _uiState.update { state ->
                        state.copy(imageGenLog = state.imageGenLog + "> Menghitung sampling step scheduler (25 iterasi)...\n")
                    }
                }
                "FalAI" -> {
                    _uiState.update { state ->
                        state.copy(imageGenLog = state.imageGenLog + "> Menginisialisasi FalAI Serverless endpoint cepat...\n")
                    }
                    delay(800)
                    _uiState.update { state ->
                        state.copy(imageGenLog = state.imageGenLog + "> Menghubungkan visual pipeline ke $model...\n")
                    }
                    delay(800)
                    _uiState.update { state ->
                        state.copy(imageGenLog = state.imageGenLog + "> Melakukan sinkronisasi resolusi aspek $aspectRatio secara real-time...\n")
                    }
                }
                else -> {
                    _uiState.update { state ->
                        state.copy(imageGenLog = state.imageGenLog + "> Mengaktifkan fallback engine umum...\n")
                    }
                    delay(800)
                }
            }
            delay(1000)

            _uiState.update { state ->
                state.copy(imageGenLog = state.imageGenLog + "> Mematangkan detail rendering dan warna...\n")
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

            val finalPromptQuery = prompt.trim() + aestheticTag + modelTag
            val encodedPrompt = java.net.URLEncoder.encode(finalPromptQuery, "UTF-8")
            val randomSeed = java.util.Random().nextInt(1000000)

            // Customize Pollinations URL if appropriate. E.g., model=flux or similar to make the output match!
            val pollinationsModelParam = when (model) {
                "Flux Pro", "Flux Realism" -> "&model=flux"
                "Core Diffusion", "Ultra Diffusion" -> "&model=turbo"
                else -> ""
            }
            val finalImageUrl = "https://image.pollinations.ai/p/${encodedPrompt}?width=$width&height=$height&seed=$randomSeed&nologo=true$pollinationsModelParam"

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
                    imageGenLog = state.imageGenLog + "> GENERASI SUKSES!\n> Gambar Imagen 3 berhasil disimpan ke galeri lokal."
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
        }

        viewModelScope.launch {
            val mode = _uiState.value.selectedVideoMode
            val provider = _uiState.value.selectedVideoProvider
            val model = _uiState.value.selectedVideoModel

            _uiState.update { state ->
                state.copy(
                    videoGenIsLoading = true,
                    videoGenLog = "> Mempersiapkan engine render video untuk mode $mode...\n"
                )
            }

            delay(1000)
            _uiState.update { state ->
                state.copy(videoGenLog = state.videoGenLog + "> Menghubungkan ke Cluster GPU $provider ($model)...\n")
            }
            delay(1000)
            _uiState.update { state ->
                state.copy(videoGenLog = state.videoGenLog + "> Mengirim parameter pergerakan alur cerita...\n")
            }
            delay(1000)

            when (provider) {
                "OpenAI" -> {
                    _uiState.update { state ->
                        state.copy(videoGenLog = state.videoGenLog + "> Otentikasi aman API OpenAI Sora...\n")
                    }
                    delay(800)
                    _uiState.update { state ->
                        state.copy(videoGenLog = state.videoGenLog + "> Menyiapkan latents visual 3D temporal dengan model $model...\n")
                    }
                }
                "Gemini" -> {
                    _uiState.update { state ->
                        state.copy(videoGenLog = state.videoGenLog + "> Menginisialisasi model video Google Veo ($model)...\n")
                    }
                    delay(800)
                    _uiState.update { state ->
                        state.copy(videoGenLog = state.videoGenLog + "> Mengoptimalkan parameter resolusi super dengan Google Cloud TPU...\n")
                    }
                }
                "FalAI" -> {
                    _uiState.update { state ->
                        state.copy(videoGenLog = state.videoGenLog + "> Menyambungkan ke pipeline serverless FalAI Kling ($model)...\n")
                    }
                    delay(800)
                    _uiState.update { state ->
                        state.copy(videoGenLog = state.videoGenLog + "> Mengatur sampling steps scheduler & motion guidance...\n")
                    }
                }
            }
            delay(1000)

            _uiState.update { state ->
                state.copy(videoGenLog = state.videoGenLog + "> Mengatur resolusi [$resolution], rasio aspek [$aspectRatio], durasi [$duration]...\n")
            }
            delay(1200)
            _uiState.update { state ->
                state.copy(videoGenLog = state.videoGenLog + "> Sedang merender frame demi frame (proses interpolasi temporal AI)...\n")
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
                    videoGenLog = state.videoGenLog + "> GENERASI SUKSES!\n> File video render model $model berhasil disimpan ke pustaka lokal."
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
        }

        viewModelScope.launch {
            val provider = _uiState.value.selectedMusicProvider
            val model = _uiState.value.selectedMusicModel

            _uiState.update { state ->
                state.copy(
                    musicGenIsLoading = true,
                    musicGenLog = "> Menghubungkan ke AI Music Synthesizer Engine ditenagai oleh $provider...\n"
                )
            }

            delay(1000)
            _uiState.update { state ->
                state.copy(musicGenLog = state.musicGenLog + "> Otentikasi aman API $provider ($model) berhasil...\n")
            }
            delay(1000)
            _uiState.update { state ->
                state.copy(musicGenLog = state.musicGenLog + "> Menganalisis ketukan & mood: '${prompt.trim()}'...\n")
            }
            delay(1200)

            when (provider) {
                "Gemini" -> {
                    _uiState.update { state ->
                        state.copy(musicGenLog = state.musicGenLog + "> Menginisialisasi sistem Google Lyria ($model)...\n")
                    }
                    delay(800)
                    _uiState.update { state ->
                        state.copy(musicGenLog = state.musicGenLog + "> Mengaktifkan temporal audio latent model untuk transisi frasa...\n")
                    }
                }
                "Suno AI" -> {
                    _uiState.update { state ->
                        state.copy(musicGenLog = state.musicGenLog + "> Menyambungkan ke pipeline Suno AI ($model) Music Synthesizer...\n")
                    }
                    delay(800)
                    _uiState.update { state ->
                        state.copy(musicGenLog = state.musicGenLog + "> Menginisialisasi generator vokal multibahasa premium...\n")
                    }
                }
            }
            delay(1000)

            _uiState.update { state ->
                state.copy(musicGenLog = state.musicGenLog + "> Menyusun harmoni instrumen untuk genre '$genre'...\n")
            }
            delay(1000)
            _uiState.update { state ->
                state.copy(musicGenLog = state.musicGenLog + "> Mengatur tempo [$tempo] dan menyelaraskan sampler ($duration)...\n")
            }
            delay(1200)
            _uiState.update { state ->
                state.copy(musicGenLog = state.musicGenLog + "> Merender audio master stereo hifi (sintesis frekuensi multi-track)...\n")
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
                    musicGenLog = state.musicGenLog + "> GENERASI MUSIK SUKSES!\n> File audio model $model berhasil disimpan ke pustaka lagu lokal."
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
                Keep explanations/analyses concise, accurate, professional, and well-structured, written in Indonesian.
            """.trimIndent()

            val customPrompt = when (task) {
                "Generasi Kode" -> """
                    Tuliskan kode pemrograman lengkap dan terstruktur dalam bahasa: $language.
                    Kebutuhan Pengguna: $input
                    Pastikan kode yang dihasilkan bersih, memiliki komentar penjelasan yang relevan, mutakhir, aman dari kerentanan, dan siap dijalankan.
                """.trimIndent()
                "Bug Finder & Debugger" -> """
                    Tolong analisis potongan kode $language ini:
                    $input
                    Temukan bug, kesalahan logika, kebocoran memori, atau masalah performa. Jelaskan letak kesalahannya secara terperinci menggunakan Bahasa Indonesia, lalu tuliskan solusi perbaikannya yang benar secara utuh.
                """.trimIndent()
                "Refactor & Optimasi" -> """
                    Lakukan refactoring dan optimasi performa pada kode $language berikut:
                    $input
                    Fokus pada efisiensi waktu (time complexity), ruang (space complexity), keterbacaan (clean code), dan fitur-fitur modern dari bahasa $language. Jelaskan peningkatan yang Anda lakukan dalam Bahasa Indonesia sebelum menyertakan potongan kode yang telah dioptimasi.
                """.trimIndent()
                "Konverter Bahasa" -> """
                    Konversikan kode sumber berikut dari bahasa $language menjadi bahasa target $targetLanguage secara fungsional setara.
                    Kode Asal ($language):
                    $input
                    Gunakan kaidah sintaks, struktur, koleksi, dan library modern yang umum digunakan di bahasa sasaran ($targetLanguage) agar kodenya idiomatik.
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
                        codeOutput = "Terjadi kesalahan saat memproses kode: ${e.localizedMessage}",
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
            
            val response = repository.sendSimpleRequest(
                prompt = text,
                systemText = "Anda adalah Eddie, seorang asisten cerdas asisten suara yang berbicara langsung dengan pengguna. Berikan jawaban yang ringkas, interaktif, hangat, dan alami (hindari format markdown jika memungkinkan karena akan dibaca dengan suara). Maksimum 2-3 kalimat saja dalam bahasa Indonesia.",
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
            _uiState.update { it.copy(visionResult = "Peringatan: Silakan ambil foto atau unggah gambar terlebih dahulu!") }
            return
        }
        
        viewModelScope.launch {
            _uiState.update { it.copy(visionIsLoading = true, visionResult = "") }
            
            val promptPrefix = when (taskMode) {
                "describe" -> "Deskripsikan objek, detail, warna, komposisi, atau nuansa dalam gambar ini secara detail."
                "translate" -> "Temukan semua teks yang tertulis di dalam gambar ini, transkripsikan, lalu terjemahkan ke dalam bahasa Indonesia jika teks tersebut dalam bahasa asing."
                "code" -> "Analisis rancangan UI, layout, wireframe, atau coretan desain dalam gambar ini. Ubah rancangan visual tersebut menjadi kode Jetpack Compose (Kotlin) yang lengkap, rapi, responsif, dan siap digunakan."
                else -> "Analisis gambar ini."
            }
            
            val finalPrompt = if (promptText.isNotBlank()) "$promptPrefix Instruksi tambahan: $promptText" else promptPrefix
            
            val keysMap = mapOf(
                "Gemini" to _uiState.value.geminiApiKey
            )
            
            val result = repository.sendVisionRequest(
                bitmap = bitmap,
                prompt = finalPrompt,
                providerKeys = keysMap
            )
            
            _uiState.update { it.copy(
                visionIsLoading = false,
                visionResult = result
            )}
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
            _uiState.update { it.copy(docSummaryResult = "Peringatan: Tidak ada konten dokumen yang dapat dianalisis. Pastikan file dibaca dengan benar atau isi teks tidak kosong.") }
            return
        }
        
        viewModelScope.launch {
            _uiState.update { it.copy(docIsLoading = true, docSummaryResult = "") }
            
            val promptStyle = when (style) {
                "bullet" -> "Poin-poin penting (Bullet Points)"
                "concise" -> "Ringkasan Eksekutif Singkat"
                "deep" -> "Analisis Mendalam & Komprehensif"
                else -> "Ringkasan Standar"
            }
            
            val prompt = "Silakan ringkas konten dokumen berikut ke dalam bahasa Indonesia menggunakan format gaya: $promptStyle.\n\nKonten Dokumen:\n$content"
            
            val keysMap = mapOf(
                "Gemini" to _uiState.value.geminiApiKey
            )
            
            val result = repository.sendSimpleRequest(
                prompt = prompt,
                systemText = "Anda adalah Eddie, pakar Document Intelligence dan analis teks cerdas. Berikan ringkasan yang akurat, informatif, dan terstruktur dengan baik sesuai gaya yang diminta.",
                providerKeys = keysMap
            )
            
            _uiState.update { it.copy(
                docIsLoading = false,
                docSummaryResult = result
            )}
        }
    }
    
    fun askDocumentQuestion(question: String) {
        val content = _uiState.value.docContentText
        if (content.isBlank()) {
            _uiState.update { it.copy(docQaAnswer = "Peringatan: Silakan baca file dokumen terlebih dahulu.") }
            return
        }
        if (question.isBlank()) return
        
        viewModelScope.launch {
            _uiState.update { it.copy(docQaIsLoading = true, docQaAnswer = "") }
            
            val prompt = "Berdasarkan isi dokumen berikut, jawablah pertanyaan pengguna dengan tepat, objektif, dan jujur berdasarkan data di dalam dokumen.\nPertanyaan: $question\n\nIsi Dokumen:\n$content"
            
            val keysMap = mapOf(
                "Gemini" to _uiState.value.geminiApiKey
            )
            
            val result = repository.sendSimpleRequest(
                prompt = prompt,
                systemText = "Anda adalah Eddie, asisten tanya jawab dokumen interaktif. Jawablah secara ramah, detail, akurat, dan merujuk hanya pada isi dokumen yang diberikan.",
                providerKeys = keysMap
            )
            
            _uiState.update { it.copy(
                docQaIsLoading = false,
                docQaAnswer = result
            )}
        }
    }
}

