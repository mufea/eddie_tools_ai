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
    val customSystemInstruction: String = "You are a helpful and expert AI Assistant. Please use Indonesian if requested, or Markdown formatting for easy reading.",
    val customApiKey: String = "",
    val isDarkTheme: Boolean = false,
    val currentUserProfile: UserProfile? = null
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

    // Keep track of registered users in memory for high-fidelity fully-functional Sign In/Up
    private val registeredUsers = mutableMapOf<String, Pair<String, String>>(
        "mufeas@gmail.com" to Pair("password123", "Mufeas")
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

                    queryFlow.collect { sessionEntities ->
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
                        chatDao.getMessagesForSession(sessionId).collect { messageEntities ->
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
            state.copy(currentUserProfile = null)
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
}
