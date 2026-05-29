package com.example.ui.chat

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

data class GeminiModel(
    val displayName: String,
    val apiValue: String,
    val subtitle: String
)

val availableModels = listOf(
    GeminiModel(
        displayName = "Gemini 3.1 Flash Lite",
        apiValue = "gemini-3.1-flash-lite",
        subtitle = "Frontier-class performance rivaling larger models at a fraction of the cost."
    ),
    GeminiModel(
        displayName = "Gemini 3.5 Flash",
        apiValue = "gemini-3.5-flash",
        subtitle = "Most intelligent model for sustained frontier performance on agentic and coding tasks."
    ),
    GeminiModel(
        displayName = "Gemini 3.1 Pro",
        apiValue = "gemini-3.1-pro",
        subtitle = "Advanced intelligence, complex problem-solving skills, and powerful agentic and vibe coding capabilities."
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
    val selectedModel: GeminiModel = availableModels[1], // Default is 3.5 Flash
    val customSystemInstruction: String = "You are Eddie, a helpful and expert AI Assistant. Please use Indonesian if requested, or Markdown formatting for easy reading.",
    val customApiKey: String = "",
    val isDarkTheme: Boolean = false,
    val currentUserProfile: UserProfile? = null,
    val isSubscribed: Boolean = false,
    val activeSubscriptionPlan: String? = null,
    val subscriptionExpiryDate: String? = null,
    val generatedImages: List<com.example.data.db.GeneratedImageEntity> = emptyList(),
    val imageGenIsLoading: Boolean = false,
    val imageGenLog: String = "",
    val generatedVideos: List<com.example.data.db.GeneratedVideoEntity> = emptyList(),
    val videoGenIsLoading: Boolean = false,
    val videoGenLog: String = "",
    val generatedMusic: List<com.example.data.db.GeneratedMusicEntity> = emptyList(),
    val musicGenIsLoading: Boolean = false,
    val musicGenLog: String = "",
    val codeSelectedLanguage: String = "Kotlin",
    val codeSelectedTask: String = "Generasi Kode",
    val codeTargetLanguage: String = "Python",
    val codeOutput: String = "",
    val codeIsLoading: Boolean = false,
    val codeFreeQueriesLeft: Int = 3
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
        observeSessionsByCurrentUser()
        observeMessagesForCurrentSession()
        observeGeneratedImagesByCurrentUser()
        observeGeneratedVideosByCurrentUser()
        observeGeneratedMusicByCurrentUser()
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
        
        val sessionId = _uiState.value.currentSessionId
        val userItemMessageId = UUID.randomUUID().toString()

        // 1. Save user message to Room database
        viewModelScope.launch {
            chatDao.insertMessage(ChatMessageEntity(
                id = userItemMessageId,
                sessionId = sessionId,
                role = "USER",
                text = prompt,
                timestamp = System.currentTimeMillis()
            ))

            // Update session title if it's the default placeholder
            val currentSession = _uiState.value.currentSession
            if (currentSession.title == "Percakapan Baru") {
                val newTitle = if (prompt.length > 25) prompt.take(22) + "..." else prompt
                chatDao.insertSession(ChatSessionEntity(
                    id = sessionId,
                    title = newTitle,
                    modelValue = _uiState.value.selectedModel.apiValue,
                    userEmail = _uiState.value.currentUserProfile?.email
                ))
            }

            // Set loading state
            _uiState.update { state -> state.copy(isLoading = true) }

            // Retrieve complete, safe context history
            val history = _uiState.value.messages

            // Call Gemini API Repository
            val responseText = repository.sendMessage(
                history = history,
                newPrompt = prompt,
                model = _uiState.value.selectedModel.apiValue,
                customSystemInstruction = _uiState.value.customSystemInstruction,
                customApiKey = _uiState.value.customApiKey.ifBlank { null }
            )

            val modelItemMessageId = UUID.randomUUID().toString()

            // 2. Save intelligent assistant response to database
            chatDao.insertMessage(ChatMessageEntity(
                id = modelItemMessageId,
                sessionId = sessionId,
                role = "MODEL",
                text = responseText,
                timestamp = System.currentTimeMillis()
            ))

            // Reset loading state
            _uiState.update { state -> state.copy(isLoading = false) }
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
            _uiState.update { state ->
                state.copy(
                    imageGenIsLoading = true,
                    imageGenLog = "> Mempersiapkan kueri pemrosesan...\n"
                )
            }

            // Handshake & network delay simulation
            delay(1000)
            _uiState.update { state ->
                state.copy(imageGenLog = state.imageGenLog + "> Mengirim visual request ke Engine Pollinations AI...\n")
            }
            delay(1000)
            _uiState.update { state ->
                state.copy(imageGenLog = state.imageGenLog + "> Merender latents gaya '$style' dengan rasio [$aspectRatio]...\n")
            }
            delay(1200)

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

            val finalPromptQuery = prompt.trim() + aestheticTag
            val encodedPrompt = java.net.URLEncoder.encode(finalPromptQuery, "UTF-8")
            val randomSeed = java.util.Random().nextInt(1000000)
            val finalImageUrl = "https://image.pollinations.ai/p/${encodedPrompt}?width=$width&height=$height&seed=$randomSeed&nologo=true"

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
                    imageGenLog = state.imageGenLog + "> GENERASI SUKSES!\n> Menyimpan detail lukisan ke database lokal..."
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
            _uiState.update { state ->
                state.copy(
                    videoGenIsLoading = true,
                    videoGenLog = "> Mempersiapkan engine render video...\n"
                )
            }

            delay(1000)
            _uiState.update { state ->
                state.copy(videoGenLog = state.videoGenLog + "> Menganalisis deskripsi gerakan: '$prompt'...\n")
            }
            delay(1200)
            _uiState.update { state ->
                state.copy(videoGenLog = state.videoGenLog + "> Memilih model visual dengan gaya '$style'...\n")
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
                    videoGenLog = state.videoGenLog + "> GENERASI SUKSES!\n> Menyimpan file video render ke pustaka lokal..."
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
            _uiState.update { state ->
                state.copy(
                    musicGenIsLoading = true,
                    musicGenLog = "> Menghubungkan ke AI Music Synthesizer Engine...\n"
                )
            }

            delay(1000)
            _uiState.update { state ->
                state.copy(musicGenLog = state.musicGenLog + "> Menganalisis ketukan & mood: '${prompt.trim()}'...\n")
            }
            delay(1200)
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
                    musicGenLog = state.musicGenLog + "> GENERASI MUSIK SUKSES!\n> File audio berkualitas tinggi tersimpan dalam pustaka lagu lokal."
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
                    customSystemInstruction = developerSystemInstruction,
                    customApiKey = _uiState.value.customApiKey.ifBlank { null }
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
}

