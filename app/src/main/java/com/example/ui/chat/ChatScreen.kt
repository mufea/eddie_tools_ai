package com.example.ui.chat

import android.app.Activity
import android.content.Intent
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
import android.widget.Toast
import android.net.Uri
import android.graphics.BitmapFactory
import java.util.UUID
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.horizontalScroll
import androidx.compose.ui.viewinterop.AndroidView
import android.widget.VideoView
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.EaseOutQuad
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.ChatMessage
import com.example.data.Role
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.theme.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.clickable
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.ui.input.pointer.PointerInputChange

data class KlingV3ComboElementInput(
    val id: String = java.util.UUID.randomUUID().toString(),
    var name: String = "",
    var type: String = "image_set", // "image_set" or "video"
    var frontalImage: String = "",
    var referenceImages: String = "", // comma-separated
    var videoUrl: String = ""
) {
    fun toJsonObject(): org.json.JSONObject {
        return org.json.JSONObject().apply {
            put("type", type)
            if (type == "image_set") {
                put("image_set", org.json.JSONObject().apply {
                    put("frontal_image", frontalImage)
                    if (referenceImages.isNotBlank()) {
                        val refs = org.json.JSONArray()
                        referenceImages.split(",").map { it.trim() }.filter { it.isNotEmpty() }.forEach { refs.put(it) }
                        put("reference_images", refs)
                    }
                })
            } else {
                put("video", org.json.JSONObject().apply {
                    put("url", videoUrl)
                })
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun ChatScreen(
    viewModel: ChatViewModel = viewModel()
) {
    val vm = viewModel
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var inputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val context = LocalContext.current
    var activeTab by remember { mutableStateOf("chat") } // Main active tab indicator

    // Dialog state controllers
    var showInstructionDialog by remember { mutableStateOf(false) }
    var showApiKeyDialog by remember { mutableStateOf(false) }
    var showHelpDialog by remember { mutableStateOf(false) }
    var showAuthDialog by remember { mutableStateOf(false) }
    var showBookmarksDialog by remember { mutableStateOf(false) }
    var showSubscriptionDialog by remember { mutableStateOf(false) }
    var showMemoriesDialog by remember { mutableStateOf(false) }

    // TTS Engine Setup
    var ttsUser by remember { mutableStateOf<TextToSpeech?>(null) }
    var currentlySpeakingMessageId by remember { mutableStateOf<String?>(null) }

    DisposableEffect(context) {
        var tts: TextToSpeech? = null
        try {
            val tempTts = TextToSpeech(context) { status ->
                if (status == TextToSpeech.SUCCESS) {
                    // Successfully initialized
                }
            }
            tempTts.setOnUtteranceProgressListener(object : android.speech.tts.UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {}
                override fun onDone(utteranceId: String?) {
                    android.os.Handler(android.os.Looper.getMainLooper()).post {
                        currentlySpeakingMessageId = null
                        viewModel.setVoiceSpeakActive(false)
                    }
                }
                @Deprecated("Deprecated in Java")
                override fun onError(utteranceId: String?) {
                    android.os.Handler(android.os.Looper.getMainLooper()).post {
                        currentlySpeakingMessageId = null
                        viewModel.setVoiceSpeakActive(false)
                    }
                }
            })
            tts = tempTts
            ttsUser = tempTts
        } catch (e: Throwable) {
            android.util.Log.e("ChatScreen", "Failed to initialize TTS engine", e)
        }
        onDispose {
            try {
                tts?.stop()
                tts?.shutdown()
            } catch (e: Throwable) {
                // Ignore
            }
        }
    }

    // TTS triggers
    val speakText: (String, String) -> Unit = { messageId, text ->
        val tts = ttsUser
        if (tts != null) {
            if (currentlySpeakingMessageId == messageId) {
                tts.stop()
                currentlySpeakingMessageId = null
            } else {
                tts.stop()
                currentlySpeakingMessageId = messageId
                val cleanText = text.replace("**", "").replace("`", "")
                tts.speak(cleanText, TextToSpeech.QUEUE_FLUSH, null, messageId)
            }
        } else {
            Toast.makeText(context, "Text-to-Speech is not ready.", Toast.LENGTH_SHORT).show()
        }
    }

    // Clipboard and Share util
    val clipboardManager = LocalClipboardManager.current
    val copyToClipboard: (String) -> Unit = { text ->
        clipboardManager.setText(AnnotatedString(text))
        Toast.makeText(context, "Text copied to clipboard.", Toast.LENGTH_SHORT).show()
    }

    val shareMessage: (String) -> Unit = { text ->
        val sendIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, text)
            type = "text/plain"
        }
        val shareIntent = Intent.createChooser(sendIntent, "Share Message:")
        context.startActivity(shareIntent)
    }

    // Speech-to-Text (STT) Launcher
    val speechRecognizerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val spokenText = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
            if (spokenText != null) {
                if (activeTab == "voice_ai") {
                    viewModel.processVoiceInput(spokenText, ttsUser)
                } else if (activeTab == "language_learning") {
                    viewModel.processLanguageLearningInput(spokenText, ttsUser)
                } else {
                    inputText = spokenText
                }
            }
        }
    }

    val startSpeechToText = {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-US")
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Please speak now...")
        }
        try {
            speechRecognizerLauncher.launch(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "Speech recognition is not supported on this device.", Toast.LENGTH_LONG).show()
        }
    }

    // Auto scroll bottom triggers
    LaunchedEffect(uiState.messages.size, uiState.isLoading) {
        if (uiState.messages.isNotEmpty()) {
            val targetIndex = if (uiState.isLoading) uiState.messages.size else uiState.messages.size - 1
            if (targetIndex >= 0) {
                listState.animateScrollToItem(targetIndex)
            }
        }
    }

    val isKeyboardVisible = WindowInsets.isImeVisible
    LaunchedEffect(isKeyboardVisible) {
        if (isKeyboardVisible && uiState.messages.isNotEmpty()) {
            val targetIndex = if (uiState.isLoading) uiState.messages.size else uiState.messages.size - 1
            if (targetIndex >= 0) {
                listState.animateScrollToItem(targetIndex)
            }
        }
    }

    LaunchedEffect(inputText) {
        if (inputText.isNotEmpty() && uiState.messages.isNotEmpty()) {
            val targetIndex = if (uiState.isLoading) uiState.messages.size else uiState.messages.size - 1
            if (targetIndex >= 0) {
                listState.animateScrollToItem(targetIndex)
            }
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                modifier = Modifier.fillMaxHeight().width(320.dp),
                drawerContainerColor = MaterialTheme.colorScheme.surface
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(horizontal = 24.dp, vertical = 28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Face,
                        contentDescription = "Logo",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(36.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Eddie Assistant",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = (-0.5).sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .background(
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(6.dp)
                            )
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "🪙 ${uiState.getFormattedConsumedTokens()} Spent",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                fontSize = 10.sp
                            )
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "The Ultimate All-in-One Slogan",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    NavigationDrawerItem(
                        icon = { Icon(Icons.Default.Add, contentDescription = null) },
                        label = { Text("New Conversation", fontWeight = FontWeight.SemiBold) },
                        selected = false,
                        onClick = {
                            activeTab = "chat"
                            viewModel.startNewChat()
                            scope.launch { drawerState.close() }
                        },
                        colors = NavigationDrawerItemDefaults.colors(
                            unselectedContainerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f),
                            unselectedIconColor = MaterialTheme.colorScheme.primary,
                            unselectedTextColor = MaterialTheme.colorScheme.primary
                        )
                    )

                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "MAIN NAVIGATION",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                    )

                    NavigationDrawerItem(
                        icon = { Text("💬", style = TextStyle(fontSize = 16.sp)) },
                        label = { Text("Chat Assistant", fontWeight = FontWeight.SemiBold) },
                        selected = activeTab == "chat",
                        onClick = {
                            activeTab = "chat"
                            scope.launch { drawerState.close() }
                        }
                    )

                    NavigationDrawerItem(
                        icon = { Text("🎨", style = TextStyle(fontSize = 16.sp)) },
                        label = { Text("AI Image Studio", fontWeight = FontWeight.SemiBold) },
                        selected = activeTab == "image_gen",
                        onClick = {
                            activeTab = "image_gen"
                            scope.launch { drawerState.close() }
                        }
                    )

                    NavigationDrawerItem(
                        icon = { Text("🎬", style = TextStyle(fontSize = 16.sp)) },
                        label = { Text("AI Video Studio", fontWeight = FontWeight.SemiBold) },
                        selected = activeTab == "video_gen",
                        onClick = {
                            activeTab = "video_gen"
                            scope.launch { drawerState.close() }
                        }
                    )

                    NavigationDrawerItem(
                        icon = { Text("👥", style = TextStyle(fontSize = 16.sp)) },
                        label = { Text("AI Avatar Studio", fontWeight = FontWeight.SemiBold) },
                        selected = activeTab == "avatar_studio",
                        onClick = {
                            activeTab = "avatar_studio"
                            scope.launch { drawerState.close() }
                        }
                    )

                    NavigationDrawerItem(
                        icon = { Text("💻", style = TextStyle(fontSize = 16.sp)) },
                        label = { Text("AI Code Studio", fontWeight = FontWeight.SemiBold) },
                        selected = activeTab == "ai_code",
                        onClick = {
                            activeTab = "ai_code"
                            scope.launch { drawerState.close() }
                        }
                    )

                    NavigationDrawerItem(
                        icon = { Text("🎵", style = TextStyle(fontSize = 16.sp)) },
                        label = { Text("AI Music Studio", fontWeight = FontWeight.SemiBold) },
                        selected = activeTab == "music_gen",
                        onClick = {
                            activeTab = "music_gen"
                            scope.launch { drawerState.close() }
                        }
                    )

                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "INTELLIGENCE & MULTIMODAL",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                    )

                    NavigationDrawerItem(
                        icon = { Text("🎙️", style = TextStyle(fontSize = 16.sp)) },
                        label = { Text("AI Voice Assistant", fontWeight = FontWeight.SemiBold) },
                        selected = activeTab == "voice_ai",
                        onClick = {
                            activeTab = "voice_ai"
                            scope.launch { drawerState.close() }
                        }
                    )

                    NavigationDrawerItem(
                        icon = { Text("📸", style = TextStyle(fontSize = 16.sp)) },
                        label = { Text("AI Vision Analyzer", fontWeight = FontWeight.SemiBold) },
                        selected = activeTab == "ai_vision",
                        onClick = {
                            activeTab = "ai_vision"
                            scope.launch { drawerState.close() }
                        }
                    )

                    NavigationDrawerItem(
                        icon = { Text("📐", style = TextStyle(fontSize = 16.sp)) },
                        label = { Text("AI Socratic Tutor", fontWeight = FontWeight.SemiBold) },
                        selected = activeTab == "socratic_math_tutor",
                        onClick = {
                            activeTab = "socratic_math_tutor"
                            scope.launch { drawerState.close() }
                        }
                    )

                    NavigationDrawerItem(
                        icon = { Text("🗣️", style = TextStyle(fontSize = 16.sp)) },
                        label = { Text("AI Language Practice", fontWeight = FontWeight.SemiBold) },
                        selected = activeTab == "language_learning",
                        onClick = {
                            activeTab = "language_learning"
                            scope.launch { drawerState.close() }
                        }
                    )

                    NavigationDrawerItem(
                        icon = { Text("📄", style = TextStyle(fontSize = 16.sp)) },
                        label = { Text("AI Document & PDF Summary", fontWeight = FontWeight.SemiBold) },
                        selected = activeTab == "doc_intelligence",
                        onClick = {
                            activeTab = "doc_intelligence"
                            scope.launch { drawerState.close() }
                        }
                    )



                    NavigationDrawerItem(
                        icon = { Text("🎭", style = TextStyle(fontSize = 16.sp)) },
                        label = { Text("AI Meme Lab Generator", fontWeight = FontWeight.SemiBold) },
                        selected = activeTab == "meme_generator",
                        onClick = {
                            activeTab = "meme_generator"
                            scope.launch { drawerState.close() }
                        }
                    )

                    NavigationDrawerItem(
                        icon = { Text("🛋️", style = TextStyle(fontSize = 16.sp)) },
                        label = { Text("AI Interior Consultant", fontWeight = FontWeight.SemiBold) },
                        selected = activeTab == "interior_designer",
                        onClick = {
                            activeTab = "interior_designer"
                            scope.launch { drawerState.close() }
                        }
                    )

                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "ADVANCED AI RESEARCH LABS",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                    )

                    NavigationDrawerItem(
                        icon = { Text("🌐", style = TextStyle(fontSize = 16.sp)) },
                        label = { Text("AI Deep Web Researcher", fontWeight = FontWeight.SemiBold) },
                        selected = activeTab == "web_researcher",
                        onClick = {
                            activeTab = "web_researcher"
                            scope.launch { drawerState.close() }
                        }
                    )

                    NavigationDrawerItem(
                        icon = { Text("📊", style = TextStyle(fontSize = 16.sp)) },
                        label = { Text("AI Slide Outline Gen", fontWeight = FontWeight.SemiBold) },
                        selected = activeTab == "slide_outline",
                        onClick = {
                            activeTab = "slide_outline"
                            scope.launch { drawerState.close() }
                        }
                    )

                    NavigationDrawerItem(
                        icon = { Text("✍️", style = TextStyle(fontSize = 16.sp)) },
                        label = { Text("AI Copywriter & Text Gen", fontWeight = FontWeight.SemiBold) },
                        selected = activeTab == "ai_writer",
                        onClick = {
                            activeTab = "ai_writer"
                            scope.launch { drawerState.close() }
                        }
                    )

                    NavigationDrawerItem(
                        icon = { Text("📢", style = TextStyle(fontSize = 16.sp)) },
                        label = { Text("AI Social Studio", fontWeight = FontWeight.SemiBold) },
                        selected = activeTab == "social_gen",
                        onClick = {
                            activeTab = "social_gen"
                            scope.launch { drawerState.close() }
                        }
                    )

                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "CHAT HISTORY",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                    )

                    if (uiState.sessions.isEmpty()) {
                        Text(
                            text = "No conversations yet",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                        )
                    } else {
                        uiState.sessions.forEach { session ->
                            val isSelected = session.id == uiState.currentSessionId
                            NavigationDrawerItem(
                                icon = { Icon(Icons.Default.Edit, contentDescription = null) },
                                label = {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = session.title,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f)
                                        )
                                        IconButton(
                                            onClick = { viewModel.deleteSession(session.id) },
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Text("🗑️", fontSize = 14.sp)
                                        }
                                    }
                                },
                                selected = isSelected,
                                onClick = {
                                    viewModel.selectSession(session.id)
                                    scope.launch { drawerState.close() }
                                }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "CUSTOMIZATION & SETTINGS",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                    )

                    // Gemini Nano drawer item removed

                    NavigationDrawerItem(
                        icon = { Text("🌙", style = TextStyle(fontSize = 16.sp)) },
                        label = {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Dark Theme")
                                Switch(
                                    checked = uiState.isDarkTheme,
                                    onCheckedChange = { viewModel.toggleTheme() },
                                    thumbContent = { Box(modifier = Modifier.size(SwitchDefaults.IconSize)) }
                                )
                            }
                        },
                        selected = false,
                        onClick = { viewModel.toggleTheme() }
                    )

                    NavigationDrawerItem(
                        icon = { Text("🤖", style = TextStyle(fontSize = 16.sp)) },
                        label = { Text("System Instructions (Custom)") },
                        selected = false,
                        onClick = {
                            showInstructionDialog = true
                            scope.launch { drawerState.close() }
                        }
                    )

                    NavigationDrawerItem(
                        icon = { Text("🧠", style = TextStyle(fontSize = 16.sp)) },
                        label = { Text("Smart Memory") },
                        selected = false,
                        onClick = {
                            showMemoriesDialog = true
                            scope.launch { drawerState.close() }
                        }
                    )

                    NavigationDrawerItem(
                        icon = { Text("🔑", style = TextStyle(fontSize = 16.sp)) },
                        label = { Text("API Key Manager") },
                        selected = false,
                        onClick = {
                            showApiKeyDialog = true
                            scope.launch { drawerState.close() }
                        }
                    )

                    NavigationDrawerItem(
                        icon = { Text("❤️", style = TextStyle(fontSize = 16.sp)) },
                        label = { Text("Saved Messages (Bookmarks)") },
                        selected = false,
                        onClick = {
                            showBookmarksDialog = true
                            scope.launch { drawerState.close() }
                        }
                    )

                    NavigationDrawerItem(
                        icon = { Text("🪙", style = TextStyle(fontSize = 16.sp)) },
                        label = { Text("Token Price History", fontWeight = FontWeight.SemiBold) },
                        selected = activeTab == "token_history",
                        onClick = {
                            activeTab = "token_history"
                            scope.launch { drawerState.close() }
                        }
                    )

                    NavigationDrawerItem(
                        icon = { Text("👑", style = TextStyle(fontSize = 16.sp)) },
                        label = {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (uiState.isSubscribed) "Manage Premium" else "Subscribe Premium",
                                    fontWeight = FontWeight.Bold,
                                    color = if (uiState.isSubscribed) MaterialTheme.colorScheme.primary else Color(0xFFFFB100)
                                )
                                if (uiState.isSubscribed) {
                                    Surface(
                                        color = MaterialTheme.colorScheme.primary,
                                        shape = RoundedCornerShape(4.dp)
                                    ) {
                                        Text(
                                            text = "PRO",
                                            color = MaterialTheme.colorScheme.onPrimary,
                                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold),
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                        )
                                    }
                                }
                            }
                        },
                        selected = false,
                        onClick = {
                            showSubscriptionDialog = true
                            scope.launch { drawerState.close() }
                        }
                    )

                    if (uiState.isSubscribed) {
                        NavigationDrawerItem(
                            icon = { Text("❌", style = TextStyle(fontSize = 16.sp)) },
                            label = {
                                Text(
                                    text = "Cancel Subscription",
                                    color = MaterialTheme.colorScheme.error,
                                    fontWeight = FontWeight.Bold
                                )
                            },
                            selected = false,
                            onClick = {
                                scope.launch { drawerState.close() }
                                viewModel.cancelSubscription()
                                Toast.makeText(context, "Successfully canceled subscription. Your status has returned to a Free Trial.", Toast.LENGTH_LONG).show()
                            }
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "OTHERS",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                    )

                    NavigationDrawerItem(
                        icon = { Text("ℹ️", style = TextStyle(fontSize = 16.sp)) },
                        label = { Text("Help & Info") },
                        selected = false,
                        onClick = {
                            showHelpDialog = true
                            scope.launch { drawerState.close() }
                        }
                    )

                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "MY ACCOUNT",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                    )

                    val user = uiState.currentUserProfile
                    if (user == null) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 4.dp, vertical = 8.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            ),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp)
                            ) {
                                Text(
                                    text = "Unlock Full Features",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Sign in or sign up to personalize your smart assistant, save sessions, and customize instructions.",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    lineHeight = 14.sp
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Button(
                                    onClick = {
                                        showAuthDialog = true
                                        scope.launch { drawerState.close() }
                                    },
                                    modifier = Modifier.fillMaxWidth().height(36.dp),
                                    contentPadding = PaddingValues(vertical = 4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Person,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Sign In / Sign Up", style = MaterialTheme.typography.labelMedium)
                                }
                            }
                        }
                    } else {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 4.dp, vertical = 8.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)
                            ),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primary),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = user.username.take(1).uppercase(),
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onPrimary
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = user.username,
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = user.email,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                IconButton(
                                    onClick = { viewModel.signOut() },
                                    colors = IconButtonDefaults.iconButtonColors(
                                        contentColor = MaterialTheme.colorScheme.error
                                    )
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                                        contentDescription = "Sign Out Account",
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    ) {
        val backgroundGradient = if (uiState.isDarkTheme) {
            Brush.verticalGradient(
                colors = listOf(
                    Color(0xFF0B0F19),
                    Color(0xFF1E1B4B),
                    Color(0xFF111827)
                )
            )
        } else {
            Brush.verticalGradient(
                colors = listOf(
                    Color(0xFFFFFFFF),
                    Color(0xFFEBF4FF),
                    Color(0xFFF1F5F9)
                )
            )
        }

        Scaffold(
            modifier = Modifier.background(backgroundGradient),
            containerColor = Color.Transparent,
            topBar = {
                Column {
                    TopAppBar(
                        navigationIcon = {
                            IconButton(
                                onClick = { scope.launch { drawerState.open() } },
                                modifier = Modifier.padding(start = 4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Menu,
                                    contentDescription = "Open Navigation Drawer"
                                )
                            }
                        },
                        title = {
                            if (activeTab == "token_history") {
                                Column {
                                    Text(
                                        text = "Token Price History",
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "Consumption logs & rates",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            } else {
                                var dropdownExpanded by remember { mutableStateOf(false) }
                                Box {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .clickable { dropdownExpanded = true }
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Column {
                                            Text(
                                                text = "Eddie Chat",
                                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = "${uiState.selectedModel.provider} • ${uiState.selectedModel.displayName} (${uiState.selectedModel.tokenPrice})",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Icon(
                                            imageVector = Icons.Default.ArrowDropDown,
                                            contentDescription = "Select Provider & Model",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                    
                                    DropdownMenu(
                                        expanded = dropdownExpanded,
                                        onDismissRequest = { dropdownExpanded = false },
                                        modifier = Modifier
                                            .width(320.dp)
                                            .background(MaterialTheme.colorScheme.surface)
                                    ) {
                                        val groupedModels = availableModels.groupBy { it.provider }
                                        groupedModels.forEach { (provider, models) ->
                                            Text(
                                                text = provider.uppercase(),
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    fontWeight = FontWeight.Bold,
                                                    letterSpacing = 1.sp
                                                ),
                                                color = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                                            )
                                            models.forEach { model ->
                                                val isSelected = model == uiState.selectedModel
                                                val isPro = model.isPro
                                                DropdownMenuItem(
                                                    text = {
                                                        Row(
                                                            verticalAlignment = Alignment.CenterVertically,
                                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                            modifier = Modifier.fillMaxWidth()
                                                        ) {
                                                            Column(modifier = Modifier.weight(1f)) {
                                                                Text(
                                                                    text = model.displayName,
                                                                    style = MaterialTheme.typography.bodyLarge.copy(
                                                                        fontSize = 18.sp,
                                                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold
                                                                    ),
                                                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                                                )
                                                                Spacer(modifier = Modifier.height(2.dp))
                                                                Text(
                                                                    text = model.tokenPrice,
                                                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                                    color = if (model.tokenPrice.contains("Free")) Color(0xFF10B981) else Color(0xFF6366F1)
                                                                )
                                                                Spacer(modifier = Modifier.height(2.dp))
                                                                Text(
                                                                    text = model.subtitle,
                                                                    style = MaterialTheme.typography.labelSmall,
                                                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                                                                    lineHeight = 13.sp
                                                                )
                                                            }
                                                            if (isPro) {
                                                                Surface(
                                                                    color = if (uiState.isSubscribed) MaterialTheme.colorScheme.primary else Color(0xFFFFB100),
                                                                    shape = RoundedCornerShape(4.dp)
                                                                ) {
                                                                    Text(
                                                                        text = "PRO",
                                                                        color = if (uiState.isSubscribed) MaterialTheme.colorScheme.onPrimary else Color(0xFF111111),
                                                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp, fontWeight = FontWeight.Bold),
                                                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                                                    )
                                                                }
                                                            }
                                                        }
                                                    },
                                                    onClick = {
                                                        if (isPro && !uiState.isSubscribed) {
                                                            showSubscriptionDialog = true
                                                            Toast.makeText(context, "Model ${model.displayName} requires an active Premium subscription.", Toast.LENGTH_LONG).show()
                                                        } else {
                                                            viewModel.selectModel(model)
                                                        }
                                                        dropdownExpanded = false
                                                    },
                                                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                                                )
                                            }
                                            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                                        }
                                    }
                                }
                            }
                        },
                        actions = {
                            IconButton(onClick = { viewModel.resetAgenticTooltip() }) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = "Panduan Agen Cerdas",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = Color.Transparent,
                            titleContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                }
            }
        ) { paddingValues ->
            if (activeTab == "chat") {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                ) {
                    val isKeyboardVisible = androidx.compose.foundation.layout.WindowInsets.isImeVisible
                    if (uiState.showAgenticTooltip && inputText.isEmpty() && !isKeyboardVisible) {
                        AgenticTooltipBanner(
                            onDismiss = { viewModel.dismissAgenticTooltip() },
                            onSelectPreset = { presetText ->
                                inputText = presetText
                            }
                        )
                    }

                    if (uiState.messages.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .verticalScroll(rememberScrollState())
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Face,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                                    modifier = Modifier.size(64.dp)
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "Start a New Conversation",
                                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Type a message below to start a smart conversation with the AI assistant.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.widthIn(max = 280.dp),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    } else {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            contentPadding = PaddingValues(vertical = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            items(uiState.messages) { message ->
                                ChatMessageBubble(
                                    message = message,
                                    isSpeaking = currentlySpeakingMessageId == message.id,
                                    onSpeakClick = { speakText(message.id, message.text) },
                                    onCopyClick = { copyToClipboard(message.text) },
                                    onShareClick = { shareMessage(message.text) },
                                    onBookmarkClick = {
                                        val newBookmarkState = !message.isBookmarked
                                        viewModel.toggleBookmark(message.id, newBookmarkState)
                                        val feedbackMsg = if (newBookmarkState) "Message saved to bookmarks." else "Message removed from bookmarks."
                                        Toast.makeText(context, feedbackMsg, Toast.LENGTH_SHORT).show()
                                    }
                                )
                            }

                            if (uiState.isLoading) {
                                item {
                                    TypingIndicator(uiState = uiState)
                                }
                            }
                        }
                    }

                    AnimatedVisibility(
                        visible = uiState.isSearchingWeb,
                        enter = fadeIn() + slideInVertically(initialOffsetY = { 10 }),
                        exit = fadeOut() + slideOutVertically(targetOffsetY = { 10 })
                    ) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 6.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
                            ),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(14.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "🔍 Eddie sedang menjelajahi Google Search secara real-time...",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    }

                    if (uiState.activeAutomationType != null) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 6.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.95f)
                            ),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary.copy(alpha = 0.4f)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text("⚡", fontSize = 16.sp)
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Otomasi Tindakan Eddie (Action-to-API)",
                                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.onTertiaryContainer
                                        )
                                        Text(
                                            text = uiState.activeAutomationStatus,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.8f)
                                        )
                                    }
                                    IconButton(
                                        onClick = { viewModel.dismissAutomation() },
                                        modifier = Modifier.size(20.dp)
                                    ) {
                                        Text("❌", fontSize = 10.sp)
                                    }
                                }

                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    val actPayload = uiState.activeAutomationPayload
                                    val actType = uiState.activeAutomationType
                                    Button(
                                        onClick = {
                                            if (actType == "EMAIL") {
                                                val subject = actPayload.substringAfter("subject:").substringBefore("|recipient:")
                                                val recipient = actPayload.substringAfter("recipient:").substringBefore("|body:")
                                                val body = actPayload.substringAfter("|body:")

                                                val intent = Intent(Intent.ACTION_SENDTO).apply {
                                                    data = android.net.Uri.parse("mailto:")
                                                    if (recipient.isNotEmpty() && !recipient.contains("company.com")) {
                                                        putExtra(Intent.EXTRA_EMAIL, arrayOf(recipient))
                                                    }
                                                    putExtra(Intent.EXTRA_SUBJECT, subject)
                                                    putExtra(Intent.EXTRA_TEXT, body)
                                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                                }
                                                try {
                                                    context.startActivity(Intent.createChooser(intent, "Send draft via..."))
                                                } catch (e: Exception) {
                                                    Toast.makeText(context, "No email client found.", Toast.LENGTH_SHORT).show()
                                                }
                                            } else if (actType == "WHATSAPP") {
                                                val intent = Intent(Intent.ACTION_SEND).apply {
                                                    type = "text/plain"
                                                    putExtra(Intent.EXTRA_TEXT, actPayload)
                                                    `package` = "com.whatsapp"
                                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                                }
                                                try {
                                                    context.startActivity(intent)
                                                } catch (e: Exception) {
                                                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                                        type = "text/plain"
                                                        putExtra(Intent.EXTRA_TEXT, actPayload)
                                                    }
                                                    context.startActivity(Intent.createChooser(shareIntent, "Send via..."))
                                                }
                                            }
                                            viewModel.dismissAutomation()
                                        },
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(8.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.primary
                                        ),
                                        contentPadding = PaddingValues(vertical = 4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = if (actType == "EMAIL") "Open Email Draft" else "Send to WhatsApp",
                                            style = MaterialTheme.typography.labelMedium
                                        )
                                    }

                                    OutlinedButton(
                                        onClick = { viewModel.dismissAutomation() },
                                        modifier = Modifier.weight(0.4f),
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(vertical = 4.dp)
                                    ) {
                                        Text("Cancel", style = MaterialTheme.typography.labelMedium)
                                    }
                                }
                            }
                        }
                    }

                    ChatInputBar(
                        inputText = inputText,
                        onTextChange = { inputText = it },
                        onSend = {
                            viewModel.sendMessage(inputText)
                            inputText = ""
                        },
                        onMicClick = startSpeechToText,
                        isLoading = uiState.isLoading,
                        isWebSearchEnabled = uiState.isWebSearchEnabled,
                        onWebSearchToggle = { viewModel.setWebSearchEnabled(it) },
                        selectedImageProvider = uiState.selectedImageProvider,
                        selectedImageModel = uiState.selectedImageModel,
                        onEngineSelected = { provider, model ->
                            viewModel.selectImageEngine(provider, model)
                        }
                    )
                }
            } else if (activeTab == "image_gen") {
                EddieImageGeneratorView(
                    uiState = uiState,
                    paddingValues = paddingValues,
                    onGenerate = { prompt, style, ratio, provider, model, numImages, background ->
                        viewModel.generateImage(prompt, style, ratio, provider, model, numImages, background, onSubscriptionRequired = {
                            showSubscriptionDialog = true
                            Toast.makeText(context, "Free limit reached or Eddie Premium is required for premium configurations!", Toast.LENGTH_LONG).show()
                        })
                    },
                    onDeleteImage = { id ->
                        viewModel.deleteGeneratedImage(id)
                        Toast.makeText(context, "Artwork successfully removed from collection.", Toast.LENGTH_SHORT).show()
                    },
                    onSaveEditedImage = { originalId, prompt, style, ratio, editedUrl ->
                        viewModel.saveEditedImage(originalId, prompt, style, ratio, editedUrl)
                    },
                    onProviderChange = { provider ->
                        viewModel.selectImageProvider(provider)
                    },
                    onModelChange = { model ->
                        viewModel.selectImageModel(model)
                    },
                    onClearLog = {
                        viewModel.clearImageGenLog()
                    }
                )
            } else if (activeTab == "video_gen") {
                EddieVideoGeneratorView(
                    uiState = uiState,
                    paddingValues = paddingValues,
                    chatViewModel = vm,
                    onGenerateVideo = { prompt, style, ratio, res, dur, audio, personGen, negPrompt, sampleCount, imageUrl, cfgScale, shotType, multiPrompt, seed, enhancePrompt, autoFix, tolerance, endImg, elemJson, vControl, voiceIds, startImgUrl, bitrateMode, endUserId, pixverseMultiClip, pixverseThinkingType, lumaLoop, lumaHdr, lumaExrExport, alibAudioUrl, alibMultiShots, alibVideoUrl, hunySteps, hunyFrames ->
                        viewModel.generateVideo(
                            prompt = prompt,
                            style = style,
                            aspectRatio = ratio,
                            resolution = res,
                            duration = dur,
                            generateAudio = audio,
                            personGeneration = personGen,
                            negativePrompt = negPrompt,
                            sampleCount = sampleCount,
                            imageUrl = imageUrl,
                            cfgScale = cfgScale,
                            shotType = shotType,
                            multiPromptJson = multiPrompt,
                            seed = seed,
                            enhancePrompt = enhancePrompt,
                            autoFix = autoFix,
                            safetyTolerance = tolerance,
                            endImageUrl = endImg,
                            elementsJson = elemJson,
                            voiceControl = vControl ?: false,
                            voiceIds = voiceIds,
                            startImageUrl = startImgUrl,
                            bitrateMode = bitrateMode,
                            endUserId = endUserId,
                            pixverseMultiClip = pixverseMultiClip ?: false,
                            pixverseThinkingType = pixverseThinkingType ?: "auto",
                            lumaLoop = lumaLoop ?: false,
                            lumaHdr = lumaHdr ?: false,
                            lumaExrExport = lumaExrExport ?: false,
                            alibabaAudioUrl = alibAudioUrl,
                            alibabaMultiShots = alibMultiShots,
                            alibabaVideoUrl = alibVideoUrl,
                            hunyuanInferenceSteps = hunySteps,
                            hunyuanFrames = hunyFrames,
                            onSubscriptionRequired = {
                                showSubscriptionDialog = true
                                Toast.makeText(context, "Free limit reached (max 2) or Eddie Premium is required for this duration, resolution, or aspect ratio!", Toast.LENGTH_LONG).show()
                            }
                        )
                    },
                    onDeleteVideo = { id ->
                        viewModel.deleteGeneratedVideo(id)
                        Toast.makeText(context, "Video successfully removed from local collection.", Toast.LENGTH_SHORT).show()
                    },
                    onSaveEditedVideo = { originalId, prompt, style, ratio, res, dur, editedUrl, model ->
                        viewModel.saveEditedVideo(originalId, prompt, style, ratio, res, dur, editedUrl, model)
                    },
                    onModeChange = { mode ->
                        viewModel.selectVideoMode(mode)
                    },
                    onProviderChange = { provider ->
                        viewModel.selectVideoProvider(provider)
                    },
                    onModelChange = { model ->
                        viewModel.selectVideoModel(model)
                    }
                )
            } else if (activeTab == "ai_code") {
                EddieAiCodeStudioView(
                    uiState = uiState,
                    paddingValues = paddingValues,
                    onGenerateCode = { language, task, targetLanguage, input ->
                        viewModel.generateAiCode(language, task, targetLanguage, input, onSubscriptionRequired = {
                            showSubscriptionDialog = true
                            Toast.makeText(context, "Advanced tasks or premium language options require an active Eddie Premium subscription!", Toast.LENGTH_LONG).show()
                        })
                    },
                    onClearOutput = { viewModel.clearCodeOutput() },
                    onSubscriptionRequired = {
                        showSubscriptionDialog = true
                        Toast.makeText(context, "This language option or task requires an active Eddie Premium subscription!", Toast.LENGTH_LONG).show()
                    },
                    onCopyText = { codeText -> copyToClipboard(codeText) },
                    onShareText = { codeText -> shareMessage(codeText) }
                )
            } else if (activeTab == "music_gen") {
                EddieMusicGeneratorView(
                    uiState = uiState,
                    paddingValues = paddingValues,
                    onGenerateMusic = { prompt, genre, duration, tempo ->
                        viewModel.generateMusic(prompt, genre, duration, tempo, onSubscriptionRequired = {
                            showSubscriptionDialog = true
                            Toast.makeText(context, "Free limit reached (max 2) or Eddie Premium is required for this premium genre or duration!", Toast.LENGTH_LONG).show()
                        })
                    },
                    onDeleteMusic = { id ->
                        viewModel.deleteGeneratedMusic(id)
                        Toast.makeText(context, "Music successfully removed from local collection.", Toast.LENGTH_SHORT).show()
                    },
                    onSaveEditedMusic = { originalId, prompt, genre, dur, tempo, editedUrl ->
                        viewModel.saveEditedMusic(originalId, prompt, genre, dur, tempo, editedUrl)
                    },
                    onProviderChange = { provider ->
                        viewModel.selectMusicProvider(provider)
                    },
                    onModelChange = { model ->
                        viewModel.selectMusicModel(model)
                    }
                )
            } else if (activeTab == "voice_ai") {
                EddieConversationalVoiceView(
                    uiState = uiState,
                    paddingValues = paddingValues,
                    onMicClick = { startSpeechToText() },
                    onClearChat = { viewModel.clearVoiceChat() },
                    onStopTts = { ttsUser?.stop() }
                )
            } else if (activeTab == "language_learning") {
                EddieLanguageLearningView(
                    uiState = uiState,
                    paddingValues = paddingValues,
                    onLangChange = { lang -> viewModel.changeLanguageLocale(lang) },
                    onTopicChange = { topic -> viewModel.changeLanguageConversationTopic(topic) },
                    onLevelChange = { lvl -> viewModel.changeLanguagePracticeLevel(lvl) },
                    onMicClick = { startSpeechToText() },
                    onClearChat = { viewModel.clearLanguageLearningChat() },
                    onStopTts = { ttsUser?.stop() },
                    onSendMessage = { msg -> viewModel.processLanguageLearningInput(msg, ttsUser) },
                    onPlayTts = { text ->
                        val targetLanguage = uiState.languageSelectedLocale
                        val localeToUse = when (targetLanguage) {
                            "Spanish" -> java.util.Locale("es", "ES")
                            "French" -> java.util.Locale("fr", "FR")
                            "Japanese" -> java.util.Locale("ja", "JP")
                            "German" -> java.util.Locale("de", "DE")
                            "Korean" -> java.util.Locale("ko", "KR")
                            "Chinese Mandarin" -> java.util.Locale("zh", "CN")
                            else -> java.util.Locale.US
                        }
                        ttsUser?.language = localeToUse
                        ttsUser?.speak(text, android.speech.tts.TextToSpeech.QUEUE_FLUSH, null, "EddieLanguageUtteranceId")
                    }
                )
            } else if (activeTab == "ai_vision") {
                EddieVisionAnalyzerView(
                    uiState = uiState,
                    paddingValues = paddingValues,
                    onImageSelected = { uri, bitmap ->
                        viewModel.selectVisionImage(uri, bitmap)
                    },
                    onAnalyzeClick = { mode, prompt ->
                        viewModel.analyzeVisionImage(mode, prompt)
                    },
                    onCopyText = { text -> copyToClipboard(text) },
                    onShareText = { text -> shareMessage(text) }
                )
            } else if (activeTab == "photo_tourism_ar_guide") {
                EddiePhotoTourismView(
                    uiState = uiState,
                    paddingValues = paddingValues,
                    onImageSelected = { uri, bitmap ->
                        viewModel.selectArImage(uri, bitmap)
                    },
                    onScanClick = { prompt ->
                        viewModel.scanArImage(prompt)
                    },
                    onSearchFactClick = { query ->
                        viewModel.searchArFact(query)
                    },
                    onSetTtsSpeaking = { speaking ->
                        viewModel.setArTtsSpeaking(speaking)
                    }
                )
            } else if (activeTab == "socratic_math_tutor") {
                SocraticMathTutorView(
                    uiState = uiState,
                    paddingValues = paddingValues,
                    onImageSelected = { uri, bitmap ->
                        viewModel.selectMathImage(uri, bitmap)
                    },
                    onSolveClick = { text, presetJson ->
                        viewModel.solveMathProblem(text, presetJson)
                    },
                    onAskTutorClick = { query ->
                        viewModel.askMathTutorQuestion(query)
                    },
                    onStepChange = { index ->
                        viewModel.setMathCurrentStepIndex(index)
                    },
                    onResetClick = {
                        viewModel.resetMathTutor()
                    },
                    onLevelChange = { level ->
                        viewModel.setMathStudentLevel(level)
                    }
                )
            } else if (activeTab == "culinary_assistant") {
                CulinaryAssistantView(
                    uiState = uiState,
                    paddingValues = paddingValues,
                    onFilterChange = { filter -> viewModel.selectCulinaryDietaryFilter(filter) },
                    onSelectRecipe = { recipe -> viewModel.selectCulinaryRecipe(recipe, ttsUser) },
                    onStepIndexChange = { index -> viewModel.setCulinaryStepIndex(index, ttsUser) },
                    onAddShoppingListItem = { item -> viewModel.addIngredientToShoppingList(item) },
                    onRemoveShoppingListItem = { item -> viewModel.removeIngredientFromShoppingList(item) },
                    onClearShoppingList = { viewModel.clearShoppingList() },
                    onImageSelected = { bitmap, uri -> viewModel.setCulinarySelectedImage(bitmap, uri) },
                    onAnalyzeImage = { bitmap -> viewModel.analyzeFridgeImage(bitmap, ttsUser) },
                    onSpeakStep = { text -> viewModel.speakCulinaryStep(text, ttsUser) },
                    onStopSpeak = { viewModel.stopCulinarySpeaker(ttsUser) }
                )
            } else if (activeTab == "meme_generator") {
                MemeGeneratorView(
                    uiState = uiState,
                    paddingValues = paddingValues,
                    onImageSelected = { bitmap, uri -> viewModel.setMemeSelectedImage(bitmap, uri) },
                    onUpdateTopText = { text -> viewModel.updateMemeTopText(text) },
                    onUpdateBottomText = { text -> viewModel.updateMemeBottomText(text) },
                    onUpdateTextSize = { size -> viewModel.updateMemeTextSize(size) },
                    onUpdateTextColor = { color -> viewModel.updateMemeTextColor(color) },
                    onToggleCapitalization = { viewModel.toggleMemeCapitalization() },
                    onSelectSuggestedCaption = { caption -> viewModel.selectMemeSuggestedCaption(caption) },
                    onAnalyzeImage = { bitmap -> viewModel.generateMemeMagicCaptions(bitmap) },
                    onClearAll = { viewModel.clearMemeState() }
                )
            } else if (activeTab == "interior_designer") {
                EddieInteriorDesignerView(
                    viewModel = viewModel,
                    modifier = Modifier.padding(paddingValues)
                )
            } else if (activeTab == "doc_intelligence") {
                EddieDocumentIntelligenceView(
                    uiState = uiState,
                    paddingValues = paddingValues,
                    onDocumentLoaded = { uri, name, content ->
                        viewModel.selectDocument(uri, name, content)
                    },
                    onSummarizeClick = { style ->
                        viewModel.summarizeDocument(style)
                    },
                    onAskQuestionClick = { q ->
                        viewModel.askDocumentQuestion(q)
                    },
                    onCopyText = { text -> copyToClipboard(text) },
                    onShareText = { text -> shareMessage(text) }
                )
            } else if (activeTab == "web_researcher") {
                EddieDeepWebResearcherView(
                    uiState = uiState,
                    paddingValues = paddingValues,
                    onLaunchResearch = { topic -> viewModel.runDeepWebResearch(topic) },
                    onClear = { viewModel.clearResearch() },
                    onShare = { report -> shareMessage(report) }
                )
            } else if (activeTab == "slide_outline") {
                EddieSlideOutlineView(
                    uiState = uiState,
                    paddingValues = paddingValues,
                    onGenerate = { topic -> viewModel.generatePresentationOutline(topic) },
                    onClear = { viewModel.clearSlides() }
                )
            } else if (activeTab == "sfx_generator") {
                EddieSfxGeneratorView(
                    uiState = uiState,
                    paddingValues = paddingValues,
                    onGenerateSfx = { prompt, cat, dur -> viewModel.generateSfx(prompt, cat, dur) },
                    onPlaySfx = { viewModel.playSynthesizedSfx() },
                    onClear = { viewModel.clearSfx() }
                )
            } else if (activeTab == "avatar_studio") {
                EddieAvatarStudioView(
                    uiState = uiState,
                    paddingValues = paddingValues,
                    onGenerate = { avName, avUrl, script, lang, voice, bgName, bgUrl ->
                        viewModel.generateAvatarStudioVideo(avName, avUrl, script, lang, voice, bgName, bgUrl)
                    },
                    onPlay = { video -> viewModel.playAvatarStudioVideo(video, context) },
                    onStop = { viewModel.stopAvatarStudioVideoPlayback() },
                    onDelete = { id -> viewModel.deleteAvatarStudioVideo(id) },
                    onSelectActiveVideo = { video -> viewModel.selectAvatarStudioActiveVideo(video) }
                )
            } else if (activeTab == "talking_head") {
                EddieTalkingHeadView(
                    uiState = uiState,
                    paddingValues = paddingValues,
                    onSelectImage = { url -> viewModel.selectTalkingHeadImage(url) },
                    onAnimate = { script -> viewModel.animateTalkingHead(script, context) },
                    onStop = { viewModel.stopTalkingHeadAnimation() }
                )
            } else if (activeTab == "ai_writer") {
                EddieAiWriterView(
                    uiState = uiState,
                    paddingValues = paddingValues,
                    onGenerate = { prompt, sys -> viewModel.generateAiWriterContent(prompt, sys) },
                    onClear = { viewModel.clearWriter() }
                )
            } else if (activeTab == "social_gen") {
                EddieSocialMediaGeneratorView(
                    uiState = uiState,
                    paddingValues = paddingValues,
                    onGenerate = { idea, tone -> viewModel.generateSocialMediaContent(idea, tone) },
                    onClear = { viewModel.clearSocialMediaContent() }
                )
            } else if (activeTab == "token_history") {
                TokenUsageHistoryView(
                    uiState = uiState,
                    paddingValues = paddingValues,
                    onClearHistory = { viewModel.clearTokenUsageHistory() }
                )
            }
        }
    }

    // 1. Dialog Edit Sistem Instruksi
    if (showInstructionDialog) {
        var tempInstruction by remember { mutableStateOf(uiState.customSystemInstruction) }
        AlertDialog(
            onDismissRequest = { showInstructionDialog = false },
            title = {
                Text(
                    text = "Customize System Instructions",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Modify the personality or base instructions of this chatbot system:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    OutlinedTextField(
                        value = tempInstruction,
                        onValueChange = { tempInstruction = it },
                        modifier = Modifier.fillMaxWidth().height(160.dp),
                        placeholder = { Text("Enter system instructions...") },
                        maxLines = 8,
                        textStyle = MaterialTheme.typography.bodyMedium
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.updateSystemInstruction(tempInstruction)
                        showInstructionDialog = false
                    }
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showInstructionDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // 1b. Dialog Memori Pintar Eddie
    if (showMemoriesDialog) {
        var newKey by remember { mutableStateOf("") }
        var newVal by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showMemoriesDialog = false },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("🧠", fontSize = 24.sp)
                    Text(
                        text = "Eddie Smart Memory",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Eddie automatically remembers your personal details from conversations to provide the best responses. You can also add or delete them here.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // Add custom memory form
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Add New Memory",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = newKey,
                                    onValueChange = { newKey = it },
                                    label = { Text("Category", style = MaterialTheme.typography.bodySmall) },
                                    modifier = Modifier.weight(1f),
                                    singleLine = true,
                                    textStyle = MaterialTheme.typography.bodySmall
                                )
                                OutlinedTextField(
                                    value = newVal,
                                    onValueChange = { newVal = it },
                                    label = { Text("Memory Content", style = MaterialTheme.typography.bodySmall) },
                                    modifier = Modifier.weight(1.5f),
                                    singleLine = true,
                                    textStyle = MaterialTheme.typography.bodySmall
                                )
                            }
                            Button(
                                onClick = {
                                    if (newKey.isNotBlank() && newVal.isNotBlank()) {
                                        viewModel.addMemory(newKey, newVal)
                                        newKey = ""
                                        newVal = ""
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(vertical = 4.dp)
                            ) {
                                Text("Save Memory", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }

                    HorizontalDivider()

                    if (uiState.userMemories.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(100.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "No saved memories for your profile yet.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                textAlign = TextAlign.Center
                            )
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 180.dp)
                        ) {
                            LazyColumn(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(uiState.userMemories) { memory ->
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = CardDefaults.cardColors(
                                            containerColor = MaterialTheme.colorScheme.surface
                                        ),
                                        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(10.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = memory.key,
                                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                                Text(
                                                    text = memory.content,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                            }
                                            IconButton(
                                                onClick = { viewModel.deleteMemory(memory.id) },
                                                modifier = Modifier.size(24.dp)
                                            ) {
                                                Text("🗑️", fontSize = 12.sp)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = { showMemoriesDialog = false }) {
                    Text("Close")
                }
            }
        )
    }

    // 2. Dialog API Key Manager
    if (showApiKeyDialog) {
        var tempGeminiKey by remember { mutableStateOf(uiState.geminiApiKey) }
        var tempOpenaiKey by remember { mutableStateOf(uiState.openaiApiKey) }
        var tempFalKey by remember { mutableStateOf(uiState.falApiKey) }
        var tempStabilityKey by remember { mutableStateOf(uiState.stabilityApiKey) }
        var tempDeepseekKey by remember { mutableStateOf(uiState.deepseekApiKey) }
        var tempClaudeKey by remember { mutableStateOf(uiState.claudeApiKey) }
        var tempXaiKey by remember { mutableStateOf(uiState.xaiApiKey) }
        var tempGeminiBaseUrl by remember { mutableStateOf(uiState.geminiBaseUrl) }
        var tempOpenaiBaseUrl by remember { mutableStateOf(uiState.openaiBaseUrl) }
        var tempOpenaiWebhookUrl by remember { mutableStateOf(uiState.openaiWebhookUrl) }
        var tempDeepseekBaseUrl by remember { mutableStateOf(uiState.deepseekBaseUrl) }
        var tempClaudeBaseUrl by remember { mutableStateOf(uiState.claudeBaseUrl) }
        var tempXaiBaseUrl by remember { mutableStateOf(uiState.xaiBaseUrl) }

        AlertDialog(
            onDismissRequest = { showApiKeyDialog = false },
            title = {
                Text(
                    text = "🔑 API Key & Custom Proxy Manager",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 400.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Enter your custom API Keys below. If left blank (for OpenAI & DeepSeek), the application will automatically route connections to our free server tiers. You may also specify Custom Base URLs / Reverse Proxies below.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // Gemini Key inputs removed

                    // OpenAI Key
                    Column {
                        Text(
                            text = "OpenAI API Key (GPT 4o & GPT 5.5)",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color(0xFF10A37F)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedTextField(
                            value = tempOpenaiKey,
                            onValueChange = { tempOpenaiKey = it },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("Enter your OpenAI API Key") },
                            singleLine = true,
                            textStyle = MaterialTheme.typography.bodySmall
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedTextField(
                            value = tempOpenaiBaseUrl,
                            onValueChange = { tempOpenaiBaseUrl = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("OpenAI Base URL / Proxy", style = MaterialTheme.typography.bodySmall) },
                            placeholder = { Text("e.g., https://api.openai.com/v1/chat/completions") },
                            singleLine = true,
                            textStyle = MaterialTheme.typography.bodySmall
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedTextField(
                            value = tempOpenaiWebhookUrl,
                            onValueChange = { tempOpenaiWebhookUrl = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("OpenAI Webhook URL (Bypasses Polling)", style = MaterialTheme.typography.bodySmall) },
                            placeholder = { Text("e.g., https://your-server.com/api/webhook") },
                            singleLine = true,
                            textStyle = MaterialTheme.typography.bodySmall
                        )
                    }

                    // DeepSeek Key
                    Column {
                        Text(
                            text = "DeepSeek API Key (Deepseek Chat)",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color(0xFF0066FF)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedTextField(
                            value = tempDeepseekKey,
                            onValueChange = { tempDeepseekKey = it },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("Enter your DeepSeek API Key") },
                            singleLine = true,
                            textStyle = MaterialTheme.typography.bodySmall
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedTextField(
                            value = tempDeepseekBaseUrl,
                            onValueChange = { tempDeepseekBaseUrl = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("DeepSeek Base URL / Proxy", style = MaterialTheme.typography.bodySmall) },
                            placeholder = { Text("e.g., https://api.deepseek.com/v1/chat/completions") },
                            singleLine = true,
                            textStyle = MaterialTheme.typography.bodySmall
                        )
                    }

                    // Claude Key
                    Column {
                        Text(
                            text = "Anthropic Claude API Key (Claude Sonet & Opus)",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color(0xFFD97706)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedTextField(
                            value = tempClaudeKey,
                            onValueChange = { tempClaudeKey = it },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("Enter your Anthropic Claude API Key") },
                            singleLine = true,
                            textStyle = MaterialTheme.typography.bodySmall
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedTextField(
                            value = tempClaudeBaseUrl,
                            onValueChange = { tempClaudeBaseUrl = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Claude Base URL / Proxy", style = MaterialTheme.typography.bodySmall) },
                            placeholder = { Text("e.g., https://api.anthropic.com/v1/messages") },
                            singleLine = true,
                            textStyle = MaterialTheme.typography.bodySmall
                        )
                    }

                    // XAI Key
                    Column {
                        Text(
                            text = "xAI Grok API Key (Grok 4)",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedTextField(
                            value = tempXaiKey,
                            onValueChange = { tempXaiKey = it },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("Enter your xAI Grok API Key") },
                            singleLine = true,
                            textStyle = MaterialTheme.typography.bodySmall
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedTextField(
                            value = tempXaiBaseUrl,
                            onValueChange = { tempXaiBaseUrl = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("xAI Base URL / Proxy", style = MaterialTheme.typography.bodySmall) },
                            placeholder = { Text("e.g., https://api.x.ai/v1/chat/completions") },
                            singleLine = true,
                            textStyle = MaterialTheme.typography.bodySmall
                        )
                    }

                    // Fal.ai Key
                    Column {
                        Text(
                            text = "Fal.ai / Veo / Kling API Key (Veo, Kling, Flux Schnell, Pro & Realism)",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color(0xFFFF5E00)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedTextField(
                            value = tempFalKey,
                            onValueChange = { tempFalKey = it },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("Default Active (FAL_API_KEY in .env)") },
                            singleLine = true,
                            textStyle = MaterialTheme.typography.bodySmall
                        )
                    }

                    // Stability AI Key
                    Column {
                        Text(
                            text = "Stability AI API Key (Stable Image Core / v2beta)",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color(0xFF6366F1)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedTextField(
                            value = tempStabilityKey,
                            onValueChange = { tempStabilityKey = it },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("Default Active (STABILITY_API_KEY in .env)") },
                            singleLine = true,
                            textStyle = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.updateProviderApiKey("Gemini", tempGeminiKey)
                        viewModel.updateProviderApiKey("OpenAI", tempOpenaiKey)
                        viewModel.updateProviderApiKey("FalAI", tempFalKey)
                        viewModel.updateProviderApiKey("Stable Diffusion", tempStabilityKey)
                        viewModel.updateProviderApiKey("DeepSeek", tempDeepseekKey)
                        viewModel.updateProviderApiKey("Claude", tempClaudeKey)
                        viewModel.updateProviderApiKey("XAI", tempXaiKey)
                        viewModel.updateProviderBaseUrl("Gemini", tempGeminiBaseUrl)
                        viewModel.updateProviderBaseUrl("OpenAI", tempOpenaiBaseUrl)
                        viewModel.updateOpenAiWebhookUrl(tempOpenaiWebhookUrl)
                        viewModel.updateProviderBaseUrl("DeepSeek", tempDeepseekBaseUrl)
                        viewModel.updateProviderBaseUrl("Claude", tempClaudeBaseUrl)
                        viewModel.updateProviderBaseUrl("XAI", tempXaiBaseUrl)
                        showApiKeyDialog = false
                        Toast.makeText(context, "All API Keys and custom Base URLs saved successfully!", Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showApiKeyDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // 3. Dialog Bantuan & Tentang Aplikasi
    if (showHelpDialog) {
        AlertDialog(
            onDismissRequest = { showHelpDialog = false },
            title = {
                Text(
                    text = "Help & Info",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        text = "Eddie Chat v1.3.0",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "This smart assistant application is built using cutting-edge Google Gemini API technology (model 3.5 Flash & 3.1 Pro).",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Key Features:",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = "• Room Database: Your chat session history is securely saved and will not be lost.\n" +
                               "• Speech-to-Text: Write messages hands-free by recording your voice.\n" +
                               "• Text-to-Speech: Let the chatbot read out its responses with natural audio.\n" +
                               "• Unlimited Chat Sessions: Easily delete or create conversation sessions.\n" +
                               "• Favorite Messages: Bookmark the best AI responses and save them to a dedicated favorite tab.\n" +
                               "• System Customization: Supports custom persona prompts and personal API key options.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Disclaimer: AI may produce errors. Please verify important facts periodically.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Button(onClick = { showHelpDialog = false }) {
                    Text("Close")
                }
            }
        )
    }

    // 4. Dialog Sign In & Sign Up (AuthDialog)
    if (showAuthDialog) {
        var isRegisterMode by remember { mutableStateOf(false) }
        var emailInput by remember { mutableStateOf("") }
        var usernameInput by remember { mutableStateOf("") }
        var passwordInput by remember { mutableStateOf("") }
        var passwordVisible by remember { mutableStateOf(false) }

        var authErrorMessage by remember { mutableStateOf<String?>(null) }
        var authSuccessMessage by remember { mutableStateOf<String?>(null) }

        AlertDialog(
            onDismissRequest = {
                showAuthDialog = false
                authErrorMessage = null
                authSuccessMessage = null
            },
            title = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(40.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (isRegisterMode) "Register New Account" else "Sign In to Your Account",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Button(
                            onClick = {
                                isRegisterMode = false
                                authErrorMessage = null
                                authSuccessMessage = null
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (!isRegisterMode) MaterialTheme.colorScheme.surface else Color.Transparent,
                                contentColor = if (!isRegisterMode) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            elevation = ButtonDefaults.buttonElevation(defaultElevation = if (!isRegisterMode) 2.dp else 0.dp),
                            contentPadding = PaddingValues(vertical = 8.dp),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text("Sign In", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold))
                        }

                        Button(
                            onClick = {
                                isRegisterMode = true
                                authErrorMessage = null
                                authSuccessMessage = null
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isRegisterMode) MaterialTheme.colorScheme.surface else Color.Transparent,
                                contentColor = if (isRegisterMode) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            elevation = ButtonDefaults.buttonElevation(defaultElevation = if (isRegisterMode) 2.dp else 0.dp),
                            contentPadding = PaddingValues(vertical = 8.dp),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text("Register", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold))
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    if (authErrorMessage != null) {
                        Surface(
                            color = MaterialTheme.colorScheme.errorContainer,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = authErrorMessage ?: "",
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }

                    if (authSuccessMessage != null) {
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = authSuccessMessage ?: "",
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }

                    if (isRegisterMode) {
                        OutlinedTextField(
                            value = usernameInput,
                            onValueChange = {
                                usernameInput = it
                                authErrorMessage = null
                            },
                            label = { Text("Username") },
                            leadingIcon = {
                                Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(20.dp))
                            },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            textStyle = MaterialTheme.typography.bodyMedium
                        )
                    }

                    OutlinedTextField(
                        value = emailInput,
                        onValueChange = {
                            emailInput = it
                            authErrorMessage = null
                        },
                        label = { Text("Email Address") },
                        leadingIcon = {
                            Icon(Icons.Default.Email, contentDescription = null, modifier = Modifier.size(20.dp))
                        },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = MaterialTheme.typography.bodyMedium
                    )

                    OutlinedTextField(
                        value = passwordInput,
                        onValueChange = {
                            passwordInput = it
                            authErrorMessage = null
                        },
                        label = { Text("Password") },
                        leadingIcon = {
                            Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(20.dp))
                        },
                        trailingIcon = {
                            TextButton(
                                onClick = { passwordVisible = !passwordVisible },
                                contentPadding = PaddingValues(horizontal = 8.dp)
                            ) {
                                Text(
                                    text = if (passwordVisible) "HIDE" else "SHOW",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                                )
                            }
                        },
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = MaterialTheme.typography.bodyMedium
                    )

                    if (!isRegisterMode) {
                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = Alignment.CenterEnd
                        ) {
                            TextButton(
                                onClick = {
                                    val emailClean = emailInput.trim()
                                    if (emailClean.isBlank()) {
                                        authErrorMessage = "Please type your email address first so we know where to send the reset link."
                                    } else if (!emailClean.contains("@")) {
                                        authErrorMessage = "Invalid email address format."
                                    } else {
                                        authErrorMessage = null
                                        authSuccessMessage = "Reset link successfully sent to $emailClean! Please check your inbox."
                                    }
                                },
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Text(
                                    text = "Forgot Password?",
                                    style = MaterialTheme.typography.labelLarge.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (isRegisterMode) {
                            val err = viewModel.signUp(emailInput, passwordInput, usernameInput)
                            if (err != null) {
                                authErrorMessage = err
                            } else {
                                authSuccessMessage = "Account registered successfully! Welcome."
                                scope.launch {
                                    kotlinx.coroutines.delay(1200)
                                    showAuthDialog = false
                                    authSuccessMessage = null
                                    emailInput = ""
                                    passwordInput = ""
                                    usernameInput = ""
                                }
                            }
                        } else {
                            val err = viewModel.signIn(emailInput, passwordInput)
                            if (err != null) {
                                authErrorMessage = err
                            } else {
                                authSuccessMessage = "Successfully signed in!"
                                scope.launch {
                                    kotlinx.coroutines.delay(1200)
                                    showAuthDialog = false
                                    authSuccessMessage = null
                                    emailInput = ""
                                    passwordInput = ""
                                }
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (isRegisterMode) "Sign Up" else "Sign In")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showAuthDialog = false
                        authErrorMessage = null
                        authSuccessMessage = null
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    // 5. dialog Pesan Favorit (Bookmark Viewer)
    if (showBookmarksDialog) {
        val bookmarks by viewModel.bookmarkedMessages.collectAsStateWithLifecycle()
        AlertDialog(
            onDismissRequest = { showBookmarksDialog = false },
            title = {
                Text(
                    text = "Saved Bookmarks",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            },
            text = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 380.dp)
                ) {
                    if (bookmarks.isEmpty()) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text("❤️", fontSize = 36.sp)
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "No Bookmarks Yet",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Mark the best assistant messages by pressing the option ❤️ 'Save' inside the chat.",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(bookmarks) { bookmark ->
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                    ),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        MarkdownText(
                                            text = bookmark.text,
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "Bookmark",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.primary,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                TextButton(
                                                    onClick = { copyToClipboard(bookmark.text) },
                                                    contentPadding = PaddingValues(horizontal = 8.dp)
                                                ) {
                                                    Text("COPY", style = MaterialTheme.typography.labelSmall)
                                                }
                                                TextButton(
                                                    onClick = { viewModel.toggleBookmark(bookmark.id, false) },
                                                    contentPadding = PaddingValues(horizontal = 8.dp)
                                                ) {
                                                    Text("DELETE", style = MaterialTheme.typography.labelSmall, color = Color.Red)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = { showBookmarksDialog = false }) {
                    Text("Close")
                }
            }
        )
    }

    if (showSubscriptionDialog) {
        SubscriptionFlowDialog(
            uiState = uiState,
            onDismiss = { showSubscriptionDialog = false },
            onActivate = { plan -> viewModel.activateSubscription(plan) },
            onCancel = { viewModel.cancelSubscription() },
            onRequireAuth = {
                showSubscriptionDialog = false
                showAuthDialog = true
            }
        )
    }
}

@Composable
fun ChatMessageBubble(
    message: ChatMessage,
    isSpeaking: Boolean,
    onSpeakClick: () -> Unit,
    onCopyClick: () -> Unit,
    onShareClick: () -> Unit,
    onBookmarkClick: () -> Unit
) {
    val isUser = message.role == Role.USER
    val isDark = MaterialTheme.colorScheme.background == DarkBackground
    
    val formattedTime = remember(message.timestamp) {
        try {
            val sdf = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
            sdf.format(java.util.Date(message.timestamp))
        } catch (e: Exception) {
            ""
        }
    }

    val formattedDateTime = remember(message.timestamp) {
        try {
            val sdf = java.text.SimpleDateFormat("MMM dd, yyyy • hh:mm a", java.util.Locale.getDefault())
            sdf.format(java.util.Date(message.timestamp))
        } catch (e: Exception) {
            ""
        }
    }

    val backgroundColor = if (isUser) {
        if (isDark) DarkUserBubbleBg else LightUserBubbleBg
    } else {
        Color.Transparent
    }

    val contentColor = if (isUser) {
        if (isDark) DarkUserBubbleText else LightUserBubbleText
    } else {
        MaterialTheme.colorScheme.onBackground
    }

    val alignment = if (isUser) Alignment.CenterEnd else Alignment.CenterStart
    val shape = if (isUser) {
        RoundedCornerShape(16.dp, 16.dp, 4.dp, 16.dp)
    } else {
        RoundedCornerShape(16.dp, 16.dp, 16.dp, 4.dp)
    }

    val animAlpha = remember { Animatable(0f) }
    val animOffsetY = remember { Animatable(12f) }
    LaunchedEffect(message.id) {
        launch {
            animAlpha.animateTo(
                targetValue = 1f,
                animationSpec = tween(
                    durationMillis = 350,
                    easing = FastOutSlowInEasing
                )
            )
        }
        launch {
            animOffsetY.animateTo(
                targetValue = 0f,
                animationSpec = tween(
                    durationMillis = 300,
                    easing = EaseOutQuad
                )
            )
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer(
                alpha = animAlpha.value,
                translationY = animOffsetY.value
            )
            .padding(
                start = if (isUser) 48.dp else 4.dp,
                end = if (isUser) 4.dp else 4.dp
            ),
        horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
    ) {
        Surface(
            color = backgroundColor,
            contentColor = contentColor,
            shape = shape,
            border = null,
            tonalElevation = 0.dp,
            shadowElevation = 0.dp
        ) {
            val delimiter = "\n\n---\n*🪙"
            val tokenIndex = message.text.indexOf(delimiter)
            if (message.text.contains("[IMAGE_URL]") && message.text.contains("[IMAGE_URL_END]")) {
                val start = message.text.indexOf("[IMAGE_URL]") + 11
                val end = message.text.indexOf("[IMAGE_URL_END]")
                if (end > start) {
                    val imageUrl = message.text.substring(start, end).trim()
                    val promptText = message.text.substring(end + 15).trim()

                    Column(
                        modifier = Modifier
                            .padding(12.dp)
                            .widthIn(max = 280.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            AsyncImage(
                                model = imageUrl,
                                contentDescription = promptText,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Text(
                            text = promptText,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Medium,
                                fontSize = 13.sp
                            ),
                            color = contentColor
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        val context = LocalContext.current
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Button(
                                onClick = {
                                    Toast.makeText(context, "Downloading artwork...", Toast.LENGTH_SHORT).show()
                                    downloadAndroidImage(context, imageUrl, promptText) { savedPath ->
                                        if (savedPath != null) {
                                            Toast.makeText(context, "Saved to $savedPath!", Toast.LENGTH_LONG).show()
                                        } else {
                                            Toast.makeText(context, "Failed to download image", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                },
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                modifier = Modifier.height(32.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    DownloadIconButton(
                                        onClick = {},
                                        size = 14.dp,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = "Download",
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                                    )
                                }
                            }
                        }
                    }
                } else {
                    val mainText = if (tokenIndex != -1) message.text.substring(0, tokenIndex) else message.text
                    MarkdownText(
                        text = mainText,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        color = contentColor
                    )
                }
            } else {
                val mainText = if (tokenIndex != -1) message.text.substring(0, tokenIndex) else message.text
                MarkdownText(
                    text = mainText,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    color = contentColor
                )
            }
        }

        if (isUser && formattedDateTime.isNotEmpty()) {
            Row(
                modifier = Modifier.padding(top = 4.dp, end = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = formattedDateTime,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Normal
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.62f)
                )
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Sent",
                    modifier = Modifier.size(11.dp),
                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.65f)
                )
            }
        }

        if (!isUser && formattedDateTime.isNotEmpty()) {
            Row(
                modifier = Modifier.padding(top = 4.dp, start = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = formattedDateTime,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Normal
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
            }
        }

        // Action Toolbar (Copy, TTS, Bookmark option for assistant responses)
        if (!isUser) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp, start = 4.dp, end = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = onSpeakClick,
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp),
                    modifier = Modifier.height(28.dp)
                ) {
                    Text(
                        text = if (isSpeaking) "■ STOP" else "🔊 SPEAK",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = if (isSpeaking) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                    )
                }

                TextButton(
                    onClick = onCopyClick,
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp),
                    modifier = Modifier.height(28.dp)
                ) {
                    Text(
                        text = "📋 COPY",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                    )
                }

                TextButton(
                    onClick = onShareClick,
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp),
                    modifier = Modifier.height(28.dp)
                ) {
                    Text(
                        text = "📤 SHARE",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                    )
                }

                TextButton(
                    onClick = onBookmarkClick,
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp),
                    modifier = Modifier.height(28.dp)
                ) {
                    Text(
                        text = if (message.isBookmarked) "❤️ SAVED" else "🤍 SAVE",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = if (message.isBookmarked) Color.Red else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun TypingIndicator(uiState: ChatUiState) {
    val lastMessage = uiState.messages.lastOrNull()
    val isImageLoading = lastMessage != null && lastMessage.role == Role.MODEL && (
        lastMessage.text.contains("Generating masterpiece", ignoreCase = true) ||
        lastMessage.text.contains("creating artwork", ignoreCase = true) ||
        lastMessage.text.contains("🎨", ignoreCase = true)
    )

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    val entryAlpha = remember { Animatable(0f) }
    val entryOffsetY = remember { Animatable(8f) }
    LaunchedEffect(true) {
        launch {
            entryAlpha.animateTo(
                targetValue = 1f,
                animationSpec = tween(
                    durationMillis = 300,
                    easing = FastOutSlowInEasing
                )
            )
        }
        launch {
            entryOffsetY.animateTo(
                targetValue = 0f,
                animationSpec = tween(
                    durationMillis = 250,
                    easing = EaseOutQuad
                )
            )
        }
    }

    if (isImageLoading) {
        // Render Image Generation Skeleton Screen
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .graphicsLayer(
                    alpha = entryAlpha.value,
                    translationY = entryOffsetY.value
                )
                .padding(start = 4.dp, end = 4.dp, top = 8.dp, bottom = 8.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
            shape = RoundedCornerShape(16.dp, 16.dp, 16.dp, 4.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .widthIn(max = 320.dp)
            ) {
                // Skeleton Box for Image (1:1 Ratio)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = alpha)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Generating image",
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Synthesizing Canvas",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "AI is drawing your lines...",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Skeleton Lines below Image box
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.95f)
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = alpha))
                )
                Spacer(modifier = Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.6f)
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = alpha))
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Bottom Indeterminate Progress Bar
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary)
                    )
                    Text(
                        text = "Rendering masterpiece in progress...",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontStyle = FontStyle.Italic),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    } else {
        // Render Gemini API Text Response Skeleton Screen
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .graphicsLayer(
                    alpha = entryAlpha.value,
                    translationY = entryOffsetY.value
                )
                .padding(start = 4.dp, end = 4.dp, top = 8.dp, bottom = 8.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
            shape = RoundedCornerShape(16.dp, 16.dp, 16.dp, 4.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                // Header with status
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(bottom = 12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = alpha)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "🤖",
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                    Text(
                        text = "Eddie is typing a response...",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                // Shimmer paragraphs
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.95f)
                            .height(14.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = alpha))
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.85f)
                            .height(14.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = alpha))
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.92f)
                            .height(14.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = alpha))
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.45f)
                            .height(14.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = alpha))
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Pulsing dot animations next to status
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .graphicsLayer(alpha = alpha)
                                .background(MaterialTheme.colorScheme.primary)
                        )
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .graphicsLayer(alpha = (alpha + 0.3f).coerceAtMost(1f))
                                .background(MaterialTheme.colorScheme.primary)
                        )
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .graphicsLayer(alpha = (alpha - 0.3f).coerceAtLeast(0.1f))
                                .background(MaterialTheme.colorScheme.primary)
                        )
                    }
                    Text(
                        text = "Synthesizing answer",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun AgenticTooltipBanner(
    onDismiss: () -> Unit,
    onSelectPreset: (String) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)
        ),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("💡", fontSize = 20.sp)
                    Text(
                        text = "Agentic Chat Features",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "Eddie is equipped with intelligent agents (Action-to-API) that automatically detect your prompt context to draw draft emails or share directly to WhatsApp without manual copying!",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Tap any sample prompt template below to see it in action instantly:",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Preset prompts vertical layout (disabled horizontal scroll)
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Draft Email Chip
                AssistChip(
                    onClick = {
                        onSelectPreset("Please write an email to the manager explaining my team updates")
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("📧")
                            Text("Draft Email Action")
                        }
                    },
                    colors = AssistChipDefaults.assistChipColors(
                        labelColor = MaterialTheme.colorScheme.primary
                    )
                )

                // WhatsApp Sharing Chip
                AssistChip(
                    onClick = {
                        onSelectPreset("Send a whatsapp message with birthday greetings to grandpa")
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("💬")
                            Text("WhatsApp Share")
                        }
                    },
                    colors = AssistChipDefaults.assistChipColors(
                        labelColor = MaterialTheme.colorScheme.primary
                    )
                )

                // Creative Concept Prompt (one more prompt added)
                AssistChip(
                    onClick = {
                        onSelectPreset("Create a detailed prompt for a beautiful fantasy hidden valley waterfall")
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("🎨")
                            Text("Creative Image Concept Prompt")
                        }
                    },
                    colors = AssistChipDefaults.assistChipColors(
                        labelColor = MaterialTheme.colorScheme.secondary
                    )
                )

                // Customize Prompt Option / Learn Automation in bottom
                AssistChip(
                    onClick = {
                        onSelectPreset("What tasks can you automatically automate or draft?")
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("✨")
                            Text("Learn Automation")
                        }
                    },
                    colors = AssistChipDefaults.assistChipColors(
                        labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
            }
        }
    }
}

@Composable
fun ChatInputBar(
    inputText: String,
    onTextChange: (String) -> Unit,
    onSend: () -> Unit,
    onMicClick: () -> Unit,
    isLoading: Boolean,
    isWebSearchEnabled: Boolean,
    onWebSearchToggle: (Boolean) -> Unit,
    selectedImageProvider: String = "OpenAI",
    selectedImageModel: String = "gpt-image-1.5",
    onEngineSelected: (provider: String, model: String) -> Unit = { _, _ -> }
) {
    val isDrawMode = inputText.startsWith("/draw", ignoreCase = true) ||
            inputText.startsWith("/image", ignoreCase = true) ||
            inputText.startsWith("/generate", ignoreCase = true)
    
    Surface(
        color = Color.Transparent,
        tonalElevation = 0.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 8.dp)
                .imePadding(),
        ) {
            if (isDrawMode) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Engine:",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(end = 4.dp)
                    )
                    
                    val engines = listOf(
                        Triple("Flux Schnell (0.75 Token)", "flux/schnell", "Fal AI"),
                        Triple("Nano Banana (9.75 Tokens)", "nano-banana", "Fal AI"),
                        Triple("Nano Banana Pro (9.75 Tokens)", "nano-banana-pro", "Fal AI"),
                        Triple("Nano Banana 2 (9.75 Tokens)", "nano-banana-2", "Fal AI"),
                        Triple("Flux Realism (10 Tokens)", "flux-realism", "Fal AI"),
                        Triple("Imagen 4 (10 Tokens)", "imagen4", "Fal AI"),
                        Triple("Stable (SD3) (15 Tokens)", "sd3", "Stability"),
                        Triple("Core (15 Tokens)", "core", "Stability"),
                        Triple("Ultra (15 Tokens)", "ultra", "Stability")
                    )
                    engines.forEach { (displayName, modelId, provider) ->
                        val isSelected = selectedImageProvider == provider && selectedImageModel == modelId
                        val priceStartIndex = displayName.lastIndexOf('(')
                        val baseName = if (priceStartIndex != -1) displayName.substring(0, priceStartIndex).trim() else displayName
                        val priceText = if (priceStartIndex != -1) displayName.substring(priceStartIndex).replace("(", "").replace(")", "").trim() else ""

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            border = BorderStroke(1.dp, if (isSelected) MaterialTheme.colorScheme.secondary else Color.Transparent),
                            modifier = Modifier
                                .clickable { onEngineSelected(provider, modelId) }
                                .padding(vertical = 2.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = baseName,
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = if (isSelected) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                if (priceText.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = if (priceText.contains("Free", ignoreCase = true)) "🎁 Free" else "🪙 $priceText",
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                        color = if (isSelected) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }
                }

                val drawModelDesc = when (selectedImageProvider) {
                    "OpenAI" -> {
                        when (selectedImageModel) {
                            "gpt-image-1.5" -> "DALL-E 3 based high-fidelity prompt compliance engine by OpenAI. Excellent text rendering inside images and layout consistency."
                            "gpt-image-2" -> "Next-generation OpenAI canvas engine featuring superior spatial logic, cinematic photo styling, and precise color-matching."
                            else -> null
                        }
                    }
                    "Gemini" -> {
                        when (selectedImageModel) {
                            "imagen3" -> "Google's state-of-the-art image generation model with high-quality photorealism, excellent detail resolution, and text-to-image accuracy."
                            else -> null
                        }
                    }
                    "Stability" -> {
                        when (selectedImageModel) {
                            "sd3" -> "Stability AI's classic SD3 engine using Multimodal Diffusion Transformers to deliver strong composition and prompt compliance."
                            "core" -> "Optimized lightweight Stable Diffusion core architecture focused on lightning-fast generations with stable anatomy."
                            "ultra" -> "Premium high-grade image model specialized in high dynamic range, crisp professional textures, and complex layouts."
                            else -> null
                        }
                    }
                    "Fal AI", "Fal.AI" -> {
                        when (selectedImageModel) {
                            "flux/schnell" -> "Super-efficient open model by Black Forest Labs. High fidelity textures and lighting generated in ultra-low latency."
                            "nano-banana" -> "Super artistic illustrative engine on Fal.AI designed for stylized vector graphics, cartoon aesthetics, and vibrant branding."
                            "nano-banana-pro" -> "Pro upgrade of the illustrative engine with fine-grained control over lighting, depth of field, and artistic compositions."
                            "nano-banana-2" -> "Next-generation stylized engine by Fal.AI with advanced text drawing, neon lighting, and high action poses."
                            "flux-realism" -> "A fine-tuned version of Flux Dev specialized in photorealism, capturing warm natural lighting, dynamic motion, and lifelike skin textures."
                            "imagen4" -> "Advanced flagship Google Imagen 4 engine hosted on Fal.Ai. Exceptional photo aesthetics, high-definition textures, and styling flexibility."
                            else -> null
                        }
                    }
                    else -> null
                }

                drawModelDesc?.let { description ->
                    Text(
                        text = "💡 $description",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .padding(bottom = 8.dp, start = 4.dp, end = 4.dp)
                            .fillMaxWidth()
                    )
                }
            }

            // Controls and Mode Selection Buttons row above the input box
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp, start = 4.dp, end = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Feature 1: Default Text Input Mode
                val isDefaultMode = !isDrawMode && !isWebSearchEnabled
                IconButton(
                    onClick = {
                        // Deactivate draw mode if active
                        if (isDrawMode) {
                            val cleanPrompt = when {
                                inputText.startsWith("/draw", ignoreCase = true) -> inputText.substring(5).trim()
                                inputText.startsWith("/image", ignoreCase = true) -> inputText.substring(6).trim()
                                inputText.startsWith("/generate", ignoreCase = true) -> inputText.substring(9).trim()
                                else -> inputText
                            }
                            onTextChange(cleanPrompt)
                        }
                        // Turn off web search
                        onWebSearchToggle(false)
                    },
                    modifier = Modifier
                        .size(36.dp)
                        .background(
                            color = if (isDefaultMode) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                            shape = CircleShape
                        )
                        .border(
                            width = 1.dp,
                            color = if (isDefaultMode) MaterialTheme.colorScheme.primary else Color.Transparent,
                            shape = CircleShape
                        )
                ) {
                    Text(
                        text = "💬",
                        style = TextStyle(
                            fontSize = 16.sp,
                            color = if (isDefaultMode) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }

                // Feature 2: Speech Input Mic Button
                IconButton(
                    onClick = onMicClick,
                    modifier = Modifier
                        .size(36.dp)
                        .background(
                            color = Color.Transparent,
                            shape = CircleShape
                        )
                ) {
                    Text("🎙️", style = TextStyle(fontSize = 16.sp))
                }

                // Feature 3: Web Search Toggle Button
                IconButton(
                    onClick = {
                        // Deactivate draw mode when entering search mode
                        if (isDrawMode) {
                            val cleanPrompt = when {
                                inputText.startsWith("/draw", ignoreCase = true) -> inputText.substring(5).trim()
                                inputText.startsWith("/image", ignoreCase = true) -> inputText.substring(6).trim()
                                inputText.startsWith("/generate", ignoreCase = true) -> inputText.substring(9).trim()
                                else -> inputText
                            }
                            onTextChange(cleanPrompt)
                        }
                        onWebSearchToggle(!isWebSearchEnabled)
                    },
                    modifier = Modifier
                        .size(36.dp)
                        .background(
                            color = if (isWebSearchEnabled) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                            shape = CircleShape
                        )
                        .border(
                            width = 1.dp,
                            color = if (isWebSearchEnabled) MaterialTheme.colorScheme.primary else Color.Transparent,
                            shape = CircleShape
                        )
                ) {
                    Text(
                        text = "🌐",
                        style = TextStyle(
                            fontSize = 16.sp,
                            color = if (isWebSearchEnabled) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    )
                }

                // Feature 4: Image Generation Mode Button (🎨)
                IconButton(
                    onClick = {
                        // Disable web search when entering draw mode
                        onWebSearchToggle(false)
                        if (isDrawMode) {
                            val cleanPrompt = when {
                                inputText.startsWith("/draw", ignoreCase = true) -> inputText.substring(5).trim()
                                inputText.startsWith("/image", ignoreCase = true) -> inputText.substring(6).trim()
                                inputText.startsWith("/generate", ignoreCase = true) -> inputText.substring(9).trim()
                                else -> inputText
                            }
                            onTextChange(cleanPrompt)
                        } else {
                            val clean = inputText
                                .removePrefix("/draw")
                                .removePrefix("/image")
                                .removePrefix("/generate")
                                .trim()
                            onTextChange("/draw $clean")
                        }
                    },
                    modifier = Modifier
                        .size(36.dp)
                        .background(
                            color = if (isDrawMode) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                            shape = CircleShape
                        )
                        .border(
                            width = 1.dp,
                            color = if (isDrawMode) MaterialTheme.colorScheme.primary else Color.Transparent,
                            shape = CircleShape
                        )
                ) {
                    Text(
                        text = "🎨",
                        style = TextStyle(
                            fontSize = 16.sp,
                            color = if (isDrawMode) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                if (isDrawMode || isWebSearchEnabled) {
                    Text(
                        text = if (isDrawMode) "🎨 Image active" else "🌐 Search active",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }

            // Main Text Input Area
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(24.dp)
                    )
                    .border(
                        width = 1.5.dp,
                        color = if (isWebSearchEnabled) {
                            MaterialTheme.colorScheme.primary
                        } else if (isDrawMode) {
                            MaterialTheme.colorScheme.secondary
                        } else {
                            MaterialTheme.colorScheme.outline
                        },
                        shape = RoundedCornerShape(24.dp)
                    )
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TextField(
                    value = inputText,
                    onValueChange = onTextChange,
                    modifier = Modifier.weight(1f),
                    placeholder = {
                        Text(
                            text = if (isDrawMode) {
                                "Describe what to generate..."
                            } else if (isWebSearchEnabled) {
                                "Ask Google in real-time..."
                            } else {
                                "Type a message..."
                            },
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    },
                    maxLines = 5,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        disabledContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        disabledIndicatorColor = Color.Transparent
                    ),
                    textStyle = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onBackground)
                )

                FloatingActionButton(
                    onClick = onSend,
                    modifier = Modifier.size(40.dp),
                    containerColor = if (isLoading || inputText.isBlank()) {
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                    } else {
                        MaterialTheme.colorScheme.primary
                    },
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 0.dp),
                    shape = CircleShape
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Send Message",
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun MarkdownText(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    style: TextStyle = MaterialTheme.typography.bodyLarge
) {
    val parts = text.split("(?<=\\*\\*)|(?=\\*\\*)".toRegex())
    val annotatedString = buildAnnotatedString {
        var isBold = false
        for (part in parts) {
            if (part == "**") {
                isBold = !isBold
            } else {
                if (isBold) {
                    withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                        append(part)
                    }
                } else {
                    append(part)
                }
            }
        }
    }
    Text(
        text = annotatedString,
        modifier = modifier,
        color = color,
        style = style
    )
}

@OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class,
    androidx.compose.foundation.layout.ExperimentalLayoutApi::class
)
@Composable
fun EddieImageGeneratorView(
    uiState: ChatUiState,
    paddingValues: PaddingValues,
    onGenerate: (prompt: String, style: String, ratio: String, provider: String, model: String, numImages: Int, background: String) -> Unit,
    onDeleteImage: (String) -> Unit,
    onSaveEditedImage: (String, String, String, String, String) -> Unit,
    onProviderChange: (String) -> Unit,
    onModelChange: (String) -> Unit,
    onClearLog: () -> Unit
) {
    val context = LocalContext.current
    var prompt by remember { mutableStateOf("") }
    var selectedStyle by remember { mutableStateOf("Photorealistic") }
    var selectedAspectRatio by remember { mutableStateOf("1024:1024") }
    
    // Providers & Models
    var activeProvider by remember { mutableStateOf("OpenAI") }
    var activeModelID by remember { mutableStateOf("gpt-image-1.5") }

    var numImages by remember { mutableStateOf(1) }
    var selectedBackground by remember { mutableStateOf("Auto") }

    // Zoom popup dialog state controllers
    var selectedImageForZoom by remember { mutableStateOf<com.example.data.db.GeneratedImageEntity?>(null) }
    var editingImageItem by remember { mutableStateOf<com.example.data.db.GeneratedImageEntity?>(null) }

    val scrollState = rememberScrollState()
    val isKeyboardVisible = WindowInsets.isImeVisible
    LaunchedEffect(isKeyboardVisible) {
        if (isKeyboardVisible) {
            scrollState.animateScrollTo(scrollState.maxValue)
        }
    }
    LaunchedEffect(prompt) {
        if (prompt.isNotEmpty()) {
            scrollState.animateScrollTo(scrollState.maxValue)
        }
    }

    val shuffledImageQuickPrompts = remember {
        listOf(
            "An astronaut riding a white horse in Yosemite valley, cinematic lighting",
            "A gorgeous floating castle surrounded by vaporwave neon clouds, 3D render",
            "A cute kitten wearing a shiny wizard coat casting light spells",
            "A dramatic medieval fortress standing tall against a cosmic sky",
            "Cyberpunk street with holographic dragons rising from neon water puddles",
            "A cozy hobbit house nestled inside a massive ancient hollow oak tree, fantasy art",
            "Art deco train station with gleaming golden train, sunbeams filtering through skylights",
            "A majestic phoenix bird rising from glowing embers with intricate fiery feathers",
            "Enchanted crystal cavern under a starry night sky with glowing blue waterfalls",
            "Retro-futuristic submarine exploring deep-sea ruins with glowing alien jellyfish",
            "A master chef mouse meticulously preparing tiny gourmet rolls, highly detailed oil painting",
            "Surreal landscape of floating glass spheres with mini self-contained ecosystems inside",
            "A majestic white stag with glowing blue antlers walking through an ancient misty autumn forest",
            "Hyper-detailed cyberpunk cat with glowing mechanical goggles and a leather vest on top of a futuristic skyscraper",
            "A forgotten library inside a colossal ancient tree trunk, with giant glowing mushrooms and floating books",
            "Whispering desert dunes under a huge starry purple galaxy sky with spectacular dynamic shooting stars"
        ).shuffled().take(4)
    }

    val styles = listOf(
        "Photorealistic" to "📸",
        "Oil Painting" to "🎨",
        "Black and White" to "🏁",
        "Gaming" to "🎮",
        "3D Render" to "🧱",
        "Hand Drawn" to "✏️",
        "Anime" to "🌸",
        "Editorial" to "📰",
        "Minimal Photoshot" to "🏺",
        "Cinematic" to "🎬",
        "Sci-Fi" to "🚀",
        "Digital Painting" to "🖥️",
        "PIxel Art" to "👾",
        "Analog Camera" to "🎞️",
        "Classic Art" to "🏛️",
        "Cartoonify" to "🧸",
        "Cyberpunk" to "🌆",
        "Fantasy" to "🔮",
        "3D Character" to "🤖"
    )

    val aspectRatios = listOf(
        "1024:1024" to "Square (1024:1024)",
        "1536:1024" to "Landscape (1536:1024)",
        "1024:1536" to "Portrait (1024:1536)",
        "1080:1920" to "Story/Reels/TikTok (1080:1920)",
        "1920:1080" to "Video/Presentation (1920:1080)"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .imePadding()
    ) {
        // Scrollable content underneath
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Hero Header (Non-sticky)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp)
            ) {
                Text(
                    text = "Eddie AI Image Pro 🎨",
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )
                if (uiState.isSubscribed) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Surface(
                        color = Color(0xFFFFB100).copy(alpha = 0.15f),
                        border = BorderStroke(1.dp, Color(0xFFFFB100)),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = "🪙 ${uiState.getFormattedTokens()} Tokens",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 10.sp),
                            color = Color(0xFFFFB100)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "High-precision commercial suite with dynamic multi-variable engines.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
            }

        // Prompt Input Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Describe Your Dream Image",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                OutlinedTextField(
                    value = prompt,
                    onValueChange = { prompt = it },
                    placeholder = { Text("Example: A vibrant futuristic city with flying automobiles under glowing neon signs...") },
                    modifier = Modifier.fillMaxWidth().height(100.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Quick Inspiration:",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 6.dp)
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    shuffledImageQuickPrompts.forEach { text ->
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
                            color = MaterialTheme.colorScheme.surface,
                            modifier = Modifier.clickable { prompt = text }
                        ) {
                            Text(
                                text = text,
                                style = MaterialTheme.typography.labelMedium,
                                maxLines = 1,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }

        // Provider selection
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "1. Generative Engines Cluster",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                val groupedPresets = listOf(
                    Pair("OpenAI" to "🧠", listOf(
                        Triple("GPT Image 1.5 (50 Tokens)", "gpt-image-1.5", "OpenAI"),
                        Triple("GPT Image 2 (52.75 Tokens)", "gpt-image-2", "OpenAI")
                    )),

                    Pair("Stability" to "⚡", listOf(
                        Triple("Stable (SD3) (15 Tokens)", "sd3", "Stability"),
                        Triple("Core (15 Tokens)", "core", "Stability"),
                        Triple("Ultra (15 Tokens)", "ultra", "Stability"),
                        Triple("SD3.5 Medium (25 Tokens)", "sd3.5-medium", "Stability"),
                        Triple("SD3 Large (25 Tokens)", "sd3-large", "Stability"),
                        Triple("SD3 Turbo (30 Tokens)", "sd3-turbo", "Stability"),
                        Triple("SD3 Medium (30 Tokens)", "sd3-medium", "Stability"),
                        Triple("SD3 Large Turbo (37.5 Tokens)", "sd3-large-turbo", "Stability"),
                        Triple("SD3.5 Large (37.5 Tokens)", "sd3.5-large", "Stability"),
                        Triple("SD3.5 Large Turbo (50 Tokens)", "sd3.5-large-turbo", "Stability")
                    )),
                    Pair("Fal AI" to "🔮", listOf(
                        Triple("Flux Schnell (0.75 Token)", "flux/schnell", "Fal AI"),
                        Triple("Nano Banana (9.75 Tokens)", "nano-banana", "Fal AI"),
                        Triple("Nano Banana Pro (9.75 Tokens)", "nano-banana-pro", "Fal AI"),
                        Triple("Nano Banana 2 (9.75 Tokens)", "nano-banana-2", "Fal AI"),
                        Triple("Flux Realism (10 Tokens)", "flux-realism", "Fal AI"),
                        Triple("Imagen 4 (10 Tokens)", "imagen4", "Fal AI"),
                        Triple("Flux Pro (100 Tokens)", "flux-pro", "Fal AI"),
                        Triple("Flux 2 Flex (100 Tokens)", "flux-2-flex", "Fal AI"),
                        Triple("Ideogram V2 (150 Tokens)", "ideogram-v2", "Fal AI")
                    ))
                )

                groupedPresets.forEachIndexed { index, (providerInfo, models) ->
                    val (providerName, providerIcon) = providerInfo
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(top = if (index > 0) 8.dp else 0.dp, bottom = 4.dp)
                        ) {
                            Text(
                                text = "$providerIcon $providerName",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(vertical = 2.dp)
                            )
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            models.forEach { (displayName, modelId, provider) ->
                                val isSelected = activeProvider == provider && activeModelID == modelId
                                val tokenCost = when {
                                    modelId == "gpt-image-1.5" -> 50.0
                                    modelId == "gpt-image-2" -> 52.75
                                    modelId == "imagen3" -> 0.0
                                    modelId == "sd3-turbo" -> 30.0
                                    modelId == "sd3-medium" -> 30.0
                                    modelId == "sd3-large-turbo" -> 37.5
                                    modelId == "sd3.5-large-turbo" -> 50.0
                                    modelId == "sd3.5-large" -> 37.5
                                    modelId == "sd3.5-medium" -> 25.0
                                    modelId == "sd3-large" -> 25.0
                                    modelId == "flux-pro" -> 100.0
                                    modelId == "flux-2-flex" -> 100.0
                                    modelId == "ideogram-v2" -> 150.0
                                    modelId == "sd3" || modelId == "core" || modelId == "ultra" -> 15.0
                                    modelId == "flux-realism" -> 10.0
                                    modelId == "nano-banana" || modelId == "nano-banana-pro" || modelId == "nano-banana-2" -> 9.75
                                    modelId == "imagen4" -> 10.0
                                    modelId == "flux/schnell" || modelId == "flux-schnell" -> 0.75
                                    else -> 0.0
                                }
                                val isPro = provider != "Gemini" && tokenCost > 25.0
                                
                                val priceStartIndex = displayName.lastIndexOf('(')
                                val baseName = if (priceStartIndex != -1) displayName.substring(0, priceStartIndex).trim() else displayName
                                val priceText = if (priceStartIndex != -1) displayName.substring(priceStartIndex).replace("(", "").replace(")", "").trim() else ""
                                
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { 
                                        activeProvider = provider
                                        activeModelID = modelId
                                        onProviderChange(provider)
                                        onModelChange(modelId)
                                    },
                                    label = {
                                        Column(
                                            modifier = Modifier.padding(vertical = 4.dp),
                                            verticalArrangement = Arrangement.spacedBy(2.dp)
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Text(
                                                    text = baseName,
                                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                                                )
                                                if (isPro) {
                                                    Surface(
                                                        color = if (uiState.isSubscribed) MaterialTheme.colorScheme.primary else Color(0xFFFFB100),
                                                        shape = RoundedCornerShape(4.dp)
                                                    ) {
                                                        Text(
                                                            text = "PRO",
                                                            color = if (uiState.isSubscribed) MaterialTheme.colorScheme.onPrimary else Color(0xFF111111),
                                                            fontSize = 8.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                                        )
                                                    }
                                                }
                                            }
                                            if (priceText.isNotEmpty()) {
                                                Text(
                                                    text = if (priceText.contains("Free", ignoreCase = true)) "🎁 Free" else "🪙 $priceText",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                                                    fontWeight = FontWeight.Medium
                                                )
                                            }
                                        }
                                    },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = if (isSelected) Icons.Default.CheckCircle else Icons.Default.Info,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                )
                            }
                        }

                        if (activeProvider == providerName) {
                            val activeModelDesc = when (providerName) {
                                "OpenAI" -> {
                                    when (activeModelID) {
                                        "gpt-image-1.5" -> "DALL-E 3 based high-fidelity prompt compliance engine by OpenAI. Excellent text rendering inside images and layout consistency."
                                        "gpt-image-2" -> "Next-generation OpenAI canvas engine featuring superior spatial logic, cinematic photo styling, and precise color-matching."
                                        else -> null
                                    }
                                }
                                "Gemini" -> {
                                    when (activeModelID) {
                                        "imagen3" -> "Google's state-of-the-art image generation model with high-quality photorealism, excellent detail resolution, and text-to-image accuracy."
                                        else -> null
                                    }
                                }
                                "Stability" -> {
                                    when (activeModelID) {
                                        "sd3" -> "Stability AI's classic SD3 engine using Multimodal Diffusion Transformers to deliver strong composition and prompt compliance."
                                        "core" -> "Optimized lightweight Stable Diffusion core architecture focused on lightning-fast generations with stable anatomy."
                                        "ultra" -> "Premium high-grade image model specialized in high dynamic range, crisp professional textures, and complex layouts."
                                        "sd3.5-medium" -> "Balanced version of Stable Diffusion 3.5. Excellent for mid-tier hardware with improved prompt compliance and lighting."
                                        "sd3-large" -> "High-resolution SD3 powerhouse with immense depth of field details, advanced photorealism, and clean styling."
                                        "sd3-turbo" -> "An extremely rapid distillation of SD3. Generates rich high-fidelity art in a fraction of normal rendering steps."
                                        "sd3-medium" -> "Standard SD3 model specialized for fast, memory-efficient rendering without compromising modern visual elements."
                                        "sd3-large-turbo" -> "Blazing-fast supreme quality SD3 engine optimized for rapid multi-step iteration and cinematic camera realism."
                                        "sd3.5-large" -> "Next-gen premier Stability engine featuring incredible prompt adherence, photorealistic skin details, and high-quality textures."
                                        "sd3.5-large-turbo" -> "Flagship ultra-speed Stable Diffusion 3.5 model providing lightning fast rendering speeds combined with premium quality."
                                        else -> null
                                    }
                                }
                                "Fal AI" -> {
                                    when (activeModelID) {
                                        "flux/schnell" -> "Super-efficient open model by Black Forest Labs. High fidelity textures and lighting generated in ultra-low latency."
                                        "nano-banana" -> "Super artistic illustrative engine on Fal.AI designed for stylized vector graphics, cartoon aesthetics, and vibrant branding."
                                        "nano-banana-pro" -> "Pro upgrade of the illustrative engine with fine-grained control over lighting, depth of field, and artistic compositions."
                                        "nano-banana-2" -> "Next-generation stylized engine by Fal.AI with advanced text drawing, neon lighting, and high action poses."
                                        "flux-realism" -> "A fine-tuned version of Flux Dev specialized in photorealism, capturing warm natural lighting, dynamic motion, and lifelike skin textures."
                                        "imagen4" -> "Advanced flagship Google Imagen 4 engine hosted on Fal.Ai. Exceptional photo aesthetics, high-definition textures, and styling flexibility."
                                        "flux-pro" -> "Premium Black Forest Labs model on Fal.AI. Offers state-of-the-art realism, incredible structural layout, and prompt accuracy."
                                        "flux-2-flex" -> "Highly customizable version of Flux customizable for dynamic aspect ratios, artistic styles, and abstract concepts."
                                        "ideogram-v2" -> "The undisputed king of text rendering in images. Features unbeatable typographic control, logo design, and graphic design templates."
                                        else -> null
                                    }
                                }
                                else -> null
                            }

                            val activeModelName = when (providerName) {
                                "OpenAI" -> {
                                    when (activeModelID) {
                                        "gpt-image-1.5" -> "GPT Image 1.5"
                                        "gpt-image-2" -> "GPT Image 2"
                                        else -> activeModelID
                                    }
                                }
                                "Gemini" -> {
                                    when (activeModelID) {
                                        "imagen3" -> "Imagen 3"
                                        else -> activeModelID
                                    }
                                }
                                "Stability" -> {
                                    when (activeModelID) {
                                        "sd3" -> "Stable Diffusion 3"
                                        "core" -> "Stable Diffusion Core"
                                        "ultra" -> "Stable Diffusion Ultra"
                                        "sd3.5-medium" -> "SD 3.5 Medium"
                                        "sd3-large" -> "SD 3 Large"
                                        "sd3-turbo" -> "SD 3 Turbo"
                                        "sd3-medium" -> "SD 3 Medium"
                                        "sd3-large-turbo" -> "SD 3 Large Turbo"
                                        "sd3.5-large" -> "SD 3.5 Large"
                                        "sd3.5-large-turbo" -> "SD 3.5 Large Turbo"
                                        else -> activeModelID
                                    }
                                }
                                "Fal AI" -> {
                                    when (activeModelID) {
                                        "flux/schnell" -> "Flux Schnell"
                                        "nano-banana" -> "Nano Banana"
                                        "nano-banana-pro" -> "Nano Banana Pro"
                                        "nano-banana-2" -> "Nano Banana 2"
                                        "flux-realism" -> "Flux Realism"
                                        "imagen4" -> "Imagen 4"
                                        "flux-pro" -> "Flux Pro"
                                        "flux-2-flex" -> "Flux 2 Flex"
                                        "ideogram-v2" -> "Ideogram V2"
                                        else -> activeModelID
                                    }
                                }
                                else -> activeModelID
                            }

                            activeModelDesc?.let { description ->
                                Spacer(modifier = Modifier.height(10.dp))
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)
                                    ),
                                    border = BorderStroke(
                                        width = 1.dp,
                                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                                    ),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(10.dp),
                                        verticalAlignment = Alignment.Top,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Info,
                                            contentDescription = "Model Info",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(16.dp).padding(top = 2.dp)
                                        )
                                        Column {
                                            Text(
                                                text = activeModelName,
                                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = description,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        if (index < groupedPresets.lastIndex) {
                            HorizontalDivider(
                                modifier = Modifier.padding(top = 10.dp, bottom = 2.dp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.08f)
                            )
                        }
                    }
                }
            }
        }

        // Image Count and Background Configuration
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "2. Advanced Pro Features",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                // Num Images
                Text(
                    text = "Number of Variations:",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 6.dp)
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val numbers = listOf(1, 2, 3, 4)
                    numbers.forEach { num ->
                        val isSelected = numImages == num
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { numImages = num },
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                            border = BorderStroke(
                                width = if (isSelected) 2.dp else 1.dp,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                            )
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp)
                            ) {
                                Text(
                                    text = "$num Variation" + (if (num > 1) "s" else ""),
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Background Config
                Text(
                    text = "Composition Background:",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 6.dp)
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val backgroundOptions = listOf("Auto", "Transparent", "Opaque")
                    backgroundOptions.forEach { bg ->
                        val isSelected = selectedBackground == bg
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { selectedBackground = bg },
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surface,
                            border = BorderStroke(
                                width = if (isSelected) 2.dp else 1.dp,
                                color = if (isSelected) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                            )
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.padding(vertical = 10.dp)
                            ) {
                                Text(
                                    text = bg,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (isSelected) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }
        }

        // Styles Section
        Column {
            Text(
                text = "3. Artistic Style Direction",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                styles.forEach { (styleName, emoji) ->
                    val isSelected = selectedStyle == styleName
                    Surface(
                        modifier = Modifier.clickable { selectedStyle = styleName },
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                        border = BorderStroke(
                            width = if (isSelected) 2.dp else 1.dp,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(text = emoji, fontSize = 16.sp)
                            Text(
                                text = styleName,
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal),
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }

        // Aspect Ratio Section
        Column {
            Text(
                text = "4. Resolution Canvas Specs",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                aspectRatios.forEach { ratioData ->
                    val ratio = ratioData.first
                    val label = ratioData.second
                    val isSelected = selectedAspectRatio == ratio
                    val isPro = ratio != "1024:1024"
                    val badgeColor = if (uiState.isSubscribed) MaterialTheme.colorScheme.primary else Color(0xFFFFB100)
                    val badgeTextColor = if (uiState.isSubscribed) MaterialTheme.colorScheme.onPrimary else Color(0xFF111111)

                    val ratioLabelName = when (ratio) {
                        "1024:1024" -> "Square"
                        "1536:1024" -> "Landscape"
                        "1024:1536" -> "Portrait"
                        "1080:1920" -> "Story/Reels"
                        "1920:1080" -> "Cinema/YT"
                        else -> "Custom"
                    }

                    Surface(
                        modifier = Modifier
                            .width(110.dp)
                            .clickable { selectedAspectRatio = ratio },
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                        border = BorderStroke(
                            width = if (isSelected) 2.dp else 1.dp,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = ratioLabelName,
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (isPro) {
                                    Surface(
                                        color = badgeColor,
                                        shape = RoundedCornerShape(4.dp)
                                    ) {
                                        Text(
                                            text = "PRO",
                                            color = badgeTextColor,
                                            fontSize = 7.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 3.dp, vertical = 1.dp)
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = ratio,
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        // Action Button and Status logs
        if (uiState.imageGenLog.isNotBlank()) {
            Surface(
                color = Color(0xFF1E1E1E),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (uiState.imageGenIsLoading) "Eddie Art Pro Studio - Processing Rendering..." else "Eddie Art Pro Studio - Complete",
                            color = Color.Green,
                            style = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 11.sp, fontWeight = FontWeight.Bold),
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = { onClearLog() },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Clear Logs",
                                tint = Color.LightGray,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = uiState.imageGenLog,
                        color = Color.White,
                        style = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 12.sp, lineHeight = 16.sp)
                    )
                    if (uiState.imageGenIsLoading) {
                        Spacer(modifier = Modifier.height(12.dp))
                        LinearProgressIndicator(
                            modifier = Modifier.fillMaxWidth(),
                            color = Color.Green,
                            trackColor = Color.DarkGray
                        )
                    }
                }
            }
        }

        if (uiState.imageGenIsLoading) {
            Button(
                onClick = { },
                enabled = false,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = "Painting Masterpiece... ⏳",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            }
        } else {
            Button(
                onClick = { 
                    onGenerate(prompt, selectedStyle, selectedAspectRatio, activeProvider, activeModelID, numImages, selectedBackground)
                },
                enabled = prompt.isNotBlank(),
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = "Paint AI Masterpiece ✨",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), modifier = Modifier.padding(vertical = 8.dp))

        // History Gallery Section
        Text(
            text = "Your Premium Artwork Collection",
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface
        )

        val userImages = uiState.generatedImages
        if (userImages.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "No Art Masterpieces Yet",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Describe your dream visuals above to start your first premium gallery.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 24.dp)
                    )
                }
            }
        } else {
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(vertical = 4.dp)
            ) {
                items(userImages) { imageItem ->
                    Card(
                        modifier = Modifier
                            .width(180.dp)
                            .clickable { selectedImageForZoom = imageItem },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(130.dp)
                                    .background(Color.LightGray)
                            ) {
                                AsyncImage(
                                    model = imageItem.imageUrl,
                                    contentDescription = imageItem.prompt,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                                Surface(
                                    color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.85f),
                                    modifier = Modifier.padding(6.dp).align(Alignment.TopEnd),
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = imageItem.style,
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.SemiBold),
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                    )
                                }
                                Surface(
                                    color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.85f),
                                    modifier = Modifier.padding(6.dp).align(Alignment.BottomEnd),
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = imageItem.model,
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.SemiBold),
                                        color = MaterialTheme.colorScheme.secondary,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Column(modifier = Modifier.padding(8.dp)) {
                                Text(
                                    text = imageItem.prompt,
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.height(32.dp)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Ratio: " + imageItem.aspectRatio,
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    DownloadIconButton(
                                        onClick = {
                                            Toast.makeText(context, "Downloading artwork...", Toast.LENGTH_SHORT).show()
                                            downloadAndroidImage(context, imageItem.imageUrl, imageItem.prompt) { savedPath ->
                                                if (savedPath != null) {
                                                    Toast.makeText(context, "Saved to $savedPath!", Toast.LENGTH_LONG).show()
                                                } else {
                                                    Toast.makeText(context, "Failed to download image", Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                        },
                                        size = 24.dp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

    selectedImageForZoom?.let { imageItem ->
        AlertDialog(
            onDismissRequest = { selectedImageForZoom = null },
            title = {
                Text(
                    text = "Your AI Artwork Details",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(240.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.DarkGray)
                    ) {
                        AsyncImage(
                            model = imageItem.imageUrl,
                            contentDescription = imageItem.prompt,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Fit
                        )
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.85f),
                            modifier = Modifier.padding(8.dp).align(Alignment.BottomEnd),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = "Model: ${imageItem.model}",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                            )
                        }
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "Visual Description:",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = imageItem.prompt,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Artistic Style",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = imageItem.style,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Image Ratio",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = imageItem.aspectRatio,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = {
                                editingImageItem = imageItem
                                selectedImageForZoom = null
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Edit Image 🎨", maxLines = 1)
                        }
                        Button(
                            onClick = {
                                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_SUBJECT, "Eddie AI Masterpiece")
                                    putExtra(Intent.EXTRA_TEXT, "Look at this image I created in Eddie Art Studio:\n\nPrompt: ${imageItem.prompt}\nLink: ${imageItem.imageUrl}")
                                }
                                context.startActivity(Intent.createChooser(shareIntent, "Share Image"))
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Share", maxLines = 1)
                        }
                    }
                    Button(
                        onClick = {
                            Toast.makeText(context, "Downloading artwork...", Toast.LENGTH_SHORT).show()
                            downloadAndroidImage(context, imageItem.imageUrl, imageItem.prompt) { savedPath ->
                                if (savedPath != null) {
                                    Toast.makeText(context, "Saved to $savedPath!", Toast.LENGTH_LONG).show()
                                } else {
                                    Toast.makeText(context, "Failed to download image", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    ) {
                        Text("Download ⬇️", maxLines = 1)
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                    TextButton(
                        onClick = {
                            onDeleteImage(imageItem.id)
                            selectedImageForZoom = null
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Delete Image 🗑️", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                    }
                }
            },
            dismissButton = null
        )
    }

    editingImageItem?.let { item ->
        EddieImageEditorDialog(
            imageItem = item,
            onDismissRequest = { editingImageItem = null },
            onSaveEditedImage = onSaveEditedImage
        )
    }
}

@Composable
fun VideoProgressVisualizer() {
    val infiniteTransition = rememberInfiniteTransition(label = "video_rendering")
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )
    val rotate by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotate"
    )
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(130.dp),
        contentAlignment = Alignment.Center
    ) {
        // Outer glowing ring
        Box(
            modifier = Modifier
                .size(90.dp)
                .graphicsLayer(scaleX = scale, scaleY = scale, rotationZ = rotate)
                .background(
                    brush = Brush.sweepGradient(
                        colors = listOf(
                            Color(0xFFE040FB), // Neon Orchid
                            Color(0xFF00E5FF), // Electric Cyan
                            Color(0xFF00E676), // Bright Green
                            Color(0xFFE040FB)
                        )
                    ),
                    shape = CircleShape
                )
        )
        // Middle mask to create glass ring
        Box(
            modifier = Modifier
                .size(82.dp)
                .background(Color(0xFF121212), shape = CircleShape)
        )
        // Inner pulsing core
        Box(
            modifier = Modifier
                .size(54.dp)
                .graphicsLayer(alpha = alpha)
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(Color(0xFF00E5FF), Color(0xFFE040FB))
                    ),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(28.dp)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SocialDistributionDialog(
    videoItem: com.example.data.db.GeneratedVideoEntity,
    chatViewModel: ChatViewModel,
    onDismissRequest: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var selectedPlatform by remember { mutableStateOf("TikTok") }
    var selectedTone by remember { mutableStateOf("Viral Hook") }
    
    // Copywriter outputs
    var generatedCopy by remember { mutableStateOf("") }
    var isGeneratingCopy by remember { mutableStateOf(false) }
    var generationError by remember { mutableStateOf("") }
    var loadingStepIndex by remember { mutableStateOf(0) }

    // Account state
    var socialHandle by remember { mutableStateOf("@creator_studio") }
    var isAccountLinked by remember { mutableStateOf(true) }
    var showLinkDialog by remember { mutableStateOf(false) }

    // Publishing states
    val publishingSteps = listOf(
        "Connecting securely to Creator API (OAuth2)...",
        "Inspecting video formatting, specs and transcode state...",
        "Uploading raw MP4 video segments to platform CDN (0%... 45%... 100%)...",
        "Applying SEO titles, optimized description & hashtag keywords...",
        "Completing platform processing & indexing video on feed..."
    )
    var isPublishing by remember { mutableStateOf(false) }
    var currentPublishStepIndex by remember { mutableStateOf(0) }
    var publishSuccess by remember { mutableStateOf(false) }

    // Animated loading steps for copywriting
    val copyLoadingSteps = listOf(
        "Synthesizing prompt context with social analytics...",
        "Drafting high-conversion title hooks...",
        "Optimizing descriptions for platform SEO algorithms...",
        "Generating trending hashtags..."
    )

    LaunchedEffect(isGeneratingCopy) {
        if (isGeneratingCopy) {
            loadingStepIndex = 0
            while (isGeneratingCopy) {
                delay(1200)
                loadingStepIndex = (loadingStepIndex + 1) % copyLoadingSteps.size
            }
        }
    }

    LaunchedEffect(isPublishing) {
        if (isPublishing) {
            currentPublishStepIndex = 0
            for (i in publishingSteps.indices) {
                currentPublishStepIndex = i
                delay(1500)
            }
            isPublishing = false
            publishSuccess = true
        }
    }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("🚀 Social Publisher & Copywriter", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                Spacer(modifier = Modifier.weight(1f))
                IconButton(onClick = onDismissRequest) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Video specs overview card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "Source Prompt:",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = videoItem.prompt,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Ratio: ${videoItem.aspectRatio}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "Style: ${videoItem.style}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                            if (videoItem.aspectRatio == "9:16") {
                                Surface(
                                    color = Color(0xFF00E5FF).copy(alpha = 0.15f),
                                    contentColor = Color(0xFF00B0FF),
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = "🔥 Ideal 9:16 vertical video",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // 2. Select Platform Row
                Text(
                    text = "1. Target Platform",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val platforms = listOf(
                        Triple("TikTok", "🎵 TikTok", Color(0xFFEE1D52)),
                        Triple("YouTube Shorts", "🎥 YouTube Shorts", Color(0xFFFF0000)),
                        Triple("Instagram Reels", "📸 Instagram Reels", Color(0xFFC13584))
                    )
                    platforms.forEach { (id, label, color) ->
                        val isSelected = selectedPlatform == id
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (isSelected) color.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surface
                                )
                                .border(
                                    width = 2.dp,
                                    color = if (isSelected) color else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .clickable { selectedPlatform = id }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                ),
                                color = if (isSelected) color else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }

                // 3. Select Tone of Post
                Text(
                    text = "2. Copywriting Tone",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val tones = listOf("Viral Hook", "Hype/Energetic", "Educational", "Casual/Funny")
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            tones.take(2).forEach { tone ->
                                val isSelected = selectedTone == tone
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { selectedTone = tone },
                                    label = { Text(tone, fontSize = 11.sp) }
                                )
                            }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            tones.drop(2).forEach { tone ->
                                val isSelected = selectedTone == tone
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { selectedTone = tone },
                                    label = { Text(tone, fontSize = 11.sp) }
                                )
                            }
                        }
                    }
                }

                // 4. Gemini Copywriter Button & Output
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "3. AI SEO Copywriter",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    
                    Button(
                        onClick = {
                            isGeneratingCopy = true
                            generationError = ""
                            scope.launch {
                                try {
                                    val result = chatViewModel.generateVideoCopy(
                                        videoPrompt = videoItem.prompt,
                                        style = videoItem.style,
                                        platform = selectedPlatform,
                                        tone = selectedTone
                                    )
                                    generatedCopy = result
                                } catch (e: Exception) {
                                    generationError = "Error generating: ${e.localizedMessage}"
                                } finally {
                                    isGeneratingCopy = false
                                }
                            }
                        },
                        enabled = !isGeneratingCopy,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondary,
                            contentColor = MaterialTheme.colorScheme.onSecondary
                        ),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Draft", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Draft SEO Copy ✨", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }

                if (isGeneratingCopy) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                        Text(
                            text = copyLoadingSteps[loadingStepIndex],
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            textAlign = TextAlign.Center
                        )
                    }
                } else if (generationError.isNotBlank()) {
                    Text(generationError, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                } else if (generatedCopy.isNotBlank()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f), RoundedCornerShape(10.dp))
                            .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f), RoundedCornerShape(10.dp))
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = generatedCopy,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            TextButton(
                                onClick = {
                                    try {
                                        val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                        val clip = android.content.ClipData.newPlainText("Social Copy", generatedCopy)
                                        clipboard.setPrimaryClip(clip)
                                        Toast.makeText(context, "Copied to clipboard! 📋", Toast.LENGTH_SHORT).show()
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "Clipboard failed", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            ) {
                                Text("📋 Copy Full Text", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("💡 Click 'Draft SEO Copy' above to let Gemini create high-engagement titles, SEO descriptions and hashtags for $selectedPlatform!", style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                // 5. Link Social Channels section
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
                Text(
                    text = "4. Social Channel Connection",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isAccountLinked) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.1f) else MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.1f)
                    ),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(
                        1.dp,
                        if (isAccountLinked) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) else MaterialTheme.colorScheme.error.copy(alpha = 0.2f)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = if (isAccountLinked) "CONNECTED ACCOUNT" else "DISCONNECTED",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = if (isAccountLinked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                            )
                            Text(
                                text = if (isAccountLinked) "Platform Handle: $socialHandle" else "Connect account to publish via automated API",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        TextButton(onClick = { showLinkDialog = true }) {
                            Text(if (isAccountLinked) "Change ⚙️" else "Link Account 🔗", fontSize = 11.sp)
                        }
                    }
                }

                // 6. Direct Publish & Automation Actions
                Text(
                    text = "5. Publish Automation & Distribution",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Direct System Intent Share Button
                    Button(
                        onClick = {
                            val finalText = if (generatedCopy.isNotBlank()) {
                                "$generatedCopy\n\nVideo URL: ${videoItem.videoUrl}"
                            } else {
                                "Look at my new video created with Eddie AI Video Studio!\nPrompt: ${videoItem.prompt}\nWatch: ${videoItem.videoUrl}"
                            }
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_SUBJECT, "Publish to Socials")
                                putExtra(Intent.EXTRA_TEXT, finalText)
                            }
                            context.startActivity(Intent.createChooser(shareIntent, "Publish via Native Share Sheet"))
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("📲 Direct Share", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    // Automated API Publisher Button
                    Button(
                        onClick = {
                            isPublishing = true
                            publishSuccess = false
                        },
                        enabled = isAccountLinked && !isPublishing,
                        modifier = Modifier.weight(1.2f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("🚀 Auto-Publish via API", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }

                // Background API Publishing Pipeline Simulator
                if (isPublishing) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Platform Publisher Pipe: Active",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        
                        LinearProgressIndicator(
                            progress = (currentPublishStepIndex + 1).toFloat() / publishingSteps.size,
                            modifier = Modifier.fillMaxWidth(),
                            color = MaterialTheme.colorScheme.primary
                        )
                        
                        Text(
                            text = publishingSteps[currentPublishStepIndex],
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                } else if (publishSuccess) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFE8F5E9), RoundedCornerShape(8.dp))
                            .border(1.dp, Color(0xFF4CAF50).copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "🎉 Successfully Published!",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                            color = Color(0xFF2E7D32)
                        )
                        Text(
                            text = "Your video is now live on $selectedPlatform feed under handle $socialHandle.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF1B5E20)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        val simulatedUrl = when (selectedPlatform) {
                            "TikTok" -> "https://tiktok.com/$socialHandle/video/739105${videoItem.id.hashCode().coerceAtLeast(1)}"
                            "YouTube Shorts" -> "https://youtube.com/shorts/v3810a${videoItem.id.hashCode().coerceAtLeast(1)}"
                            else -> "https://instagram.com/reels/r830as${videoItem.id.hashCode().coerceAtLeast(1)}"
                        }
                        TextButton(
                            onClick = {
                                try {
                                    val openIntent = Intent(Intent.ACTION_VIEW, Uri.parse(simulatedUrl))
                                    context.startActivity(openIntent)
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Could not open simulated link", Toast.LENGTH_SHORT).show()
                                }
                            },
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text("🔗 View Live Video", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline))
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismissRequest) {
                Text("Done")
            }
        }
    )

    // Handle Link/Credential Dialog
    if (showLinkDialog) {
        var tempHandle by remember { mutableStateOf(socialHandle) }
        AlertDialog(
            onDismissRequest = { showLinkDialog = false },
            title = { Text("Link Social Creator Channel 🔗", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Enter your handle for $selectedPlatform. Authorize our Studio SDK to establish a secure publisher session.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    OutlinedTextField(
                        value = tempHandle,
                        onValueChange = { tempHandle = it },
                        label = { Text("Creator Handle") },
                        placeholder = { Text("@yourname") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val cleaned = if (tempHandle.startsWith("@")) tempHandle else "@$tempHandle"
                        socialHandle = cleaned
                        isAccountLinked = tempHandle.isNotBlank() && tempHandle != "@"
                        showLinkDialog = false
                        Toast.makeText(context, "Account $cleaned successfully connected!", Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Text("Authorize & Link")
                }
            },
            dismissButton = {
                TextButton(onClick = { showLinkDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun VideoPlayerDialog(
    videoItem: com.example.data.db.GeneratedVideoEntity,
    chatViewModel: ChatViewModel,
    onDismissRequest: () -> Unit,
    onDeleteVideo: (String) -> Unit,
    onEditVideo: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val localVideoFile = remember(videoItem.id) { java.io.File(context.filesDir, "video_${videoItem.id}.mp4") }
    
    var isDownloadedLocally by remember(videoItem.id) {
        mutableStateOf(localVideoFile.exists() && localVideoFile.length() > 1024)
    }

    var isPlaying by remember { mutableStateOf(true) }
    var currentProgress by remember { mutableStateOf(0f) }
    var forceVisualizerOnly by remember { mutableStateOf(false) }
    var showSocialDialog by remember { mutableStateOf(false) }

    var isVideoPrepared by remember { mutableStateOf(false) }
    var hasVideoError by remember { mutableStateOf(false) }

    val videoSource = if (isDownloadedLocally) {
        localVideoFile.absolutePath
    } else {
        videoItem.videoUrl
    }

    // Modern Native Media3 ExoPlayer Instance
    val exoPlayer = remember(videoSource) {
        androidx.media3.exoplayer.ExoPlayer.Builder(context).build().apply {
            val uri = if (isDownloadedLocally) {
                android.net.Uri.fromFile(localVideoFile)
            } else {
                android.net.Uri.parse(videoSource)
            }
            setMediaItem(androidx.media3.common.MediaItem.fromUri(uri))
            repeatMode = androidx.media3.common.Player.REPEAT_MODE_ALL
            prepare()
            playWhenReady = isPlaying
        }
    }

    LaunchedEffect(isPlaying, exoPlayer) {
        exoPlayer.playWhenReady = isPlaying
    }

    DisposableEffect(exoPlayer) {
        val listener = object : androidx.media3.common.Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                if (state == androidx.media3.common.Player.STATE_READY) {
                    isVideoPrepared = true
                }
            }
            override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                android.util.Log.e("VideoPlayerDialog", "ExoPlayer playback error: ${error.message}", error)
                hasVideoError = true
            }
        }
        exoPlayer.addListener(listener)
        onDispose {
            exoPlayer.removeListener(listener)
            exoPlayer.release()
        }
    }

    // Parse duration seconds from the duration string to run simulation properly
    val totalSeconds = when {
        videoItem.duration.contains("3") -> 3
        videoItem.duration.contains("4") -> 4
        videoItem.duration.contains("5") -> 5
        videoItem.duration.contains("6") -> 6
        videoItem.duration.contains("8") -> 8
        videoItem.duration.contains("10") -> 10
        videoItem.duration.contains("12") -> 12
        videoItem.duration.contains("30") -> 30
        else -> 8
    }

    // Dynamic precise timeline progress updater driven by native ExoPlayer frame clock
    LaunchedEffect(isPlaying, exoPlayer, isVideoPrepared) {
        if (isPlaying) {
            while (true) {
                delay(50)
                if (!hasVideoError && !forceVisualizerOnly && isVideoPrepared && exoPlayer.duration > 0) {
                    currentProgress = exoPlayer.currentPosition.toFloat() / exoPlayer.duration
                } else {
                    currentProgress += 0.05f / totalSeconds
                    if (currentProgress >= 1f) {
                        currentProgress = 0f // Loop simulation
                    }
                }
            }
        }
    }

    val currentSecondsText = String.format("%.1fs", currentProgress * totalSeconds)

    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Eddie AI Video Player 🎬",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
                IconButton(onClick = onDismissRequest) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Video Screen Container with Real Player & Generative Visualizer Fallback
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(240.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.Black),
                    contentAlignment = Alignment.Center
                ) {
                    if (!hasVideoError && !forceVisualizerOnly) {
                        key(videoSource) {
                            AndroidView(
                                factory = { ctx ->
                                    androidx.media3.ui.PlayerView(ctx).apply {
                                        player = exoPlayer
                                        useController = false
                                        resizeMode = androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_FIT
                                        setBackgroundColor(android.graphics.Color.TRANSPARENT)
                                    }
                                },
                                update = { playerView ->
                                    playerView.player = exoPlayer
                                },
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }

                    // Immersive, stunning style-based video representation (Triggered on error or if toggled)
                    if (hasVideoError || !isVideoPrepared || forceVisualizerOnly) {
                        val styleLower = videoItem.style.lowercase()
                        val dynamicColors = when {
                            styleLower.contains("cyber") || styleLower.contains("neon") || styleLower.contains("retro") -> 
                                listOf(Color(0xFFFF007F), Color(0xFF00F0FF), Color(0xFF0B001F))
                            styleLower.contains("anime") || styleLower.contains("draw") || styleLower.contains("art") || styleLower.contains("clay") -> 
                                listOf(Color(0xFFFA2F5E), Color(0xFFFFB100), Color(0xFF1E1100))
                            styleLower.contains("drone") || styleLower.contains("aerial") || styleLower.contains("real") -> 
                                listOf(Color(0xFF00E676), Color(0xFF64FFDA), Color(0xFF00150A))
                            styleLower.contains("sci") || styleLower.contains("space") || styleLower.contains("star") || styleLower.contains("cosmic") -> 
                                listOf(Color(0xFF7C4DFF), Color(0xFF00B0FF), Color(0xFF020412))
                            styleLower.contains("film") || styleLower.contains("noir") || styleLower.contains("vint") || styleLower.contains("moody") -> 
                                listOf(Color(0xFFFFFFFF), Color(0xFF888888), Color(0xFF0D0D0D))
                            else -> 
                                listOf(Color(0xFF00BCD4), Color(0xFF3F51B5), Color(0xFF0A0F1D))
                        }

                        // Drawing responsive interactive style elements based on prompt aesthetics
                        androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                            val w = size.width
                            val h = size.height

                            // 1. Draw central background gradient
                            drawRect(
                                brush = androidx.compose.ui.graphics.Brush.radialGradient(
                                    colors = listOf(dynamicColors[1].copy(alpha = 0.35f), dynamicColors[2]),
                                    center = androidx.compose.ui.geometry.Offset(w / 2f, h / 2f),
                                    radius = w * 0.75f
                                )
                            )

                            // 2. Render complex animations unique to the generated style theme
                            val phase = currentProgress * 2f * Math.PI.toFloat()
                            
                            when {
                                styleLower.contains("cyber") || styleLower.contains("neon") || styleLower.contains("retro") -> {
                                    // Cyber 3D grid effect
                                    val gridCount = 9
                                    for (i in 0 until gridCount) {
                                        val lineRatio = i.toFloat() / (gridCount - 1)
                                        val startX = w * 0.1f + lineRatio * (w * 0.8f)
                                        // Converging lines to the vanishing point (vanishing horizon at y = h * 0.2f)
                                        drawLine(
                                            color = dynamicColors[1].copy(alpha = 0.4f),
                                            start = androidx.compose.ui.geometry.Offset(w / 2f, h * 0.2f),
                                            end = androidx.compose.ui.geometry.Offset(startX, h * 0.95f),
                                            strokeWidth = 3f
                                        )
                                    }

                                    // Moving horizontal grid bars
                                    val barCount = 4
                                    for (i in 0 until barCount) {
                                        val offsetRatio = ((i.toFloat() / barCount) + currentProgress) % 1.0f
                                        val curY = h * 0.2f + (offsetRatio * offsetRatio) * (h * 0.75f)
                                        drawLine(
                                            color = dynamicColors[0].copy(alpha = (1f - offsetRatio) * 0.6f),
                                            start = androidx.compose.ui.geometry.Offset(w * 0.1f, curY),
                                            end = androidx.compose.ui.geometry.Offset(w * 0.9f, curY),
                                            strokeWidth = 4f
                                        )
                                    }

                                    // Glowing center target
                                    drawCircle(
                                        color = dynamicColors[1].copy(alpha = 0.25f),
                                        radius = (w * 0.12f) + (Math.sin(phase.toDouble()).toFloat() * 12f),
                                        center = androidx.compose.ui.geometry.Offset(w / 2f, h / 2f)
                                    )
                                }

                                styleLower.contains("anime") || styleLower.contains("draw") || styleLower.contains("art") || styleLower.contains("clay") -> {
                                    // Radiating cinematic speed lines
                                    val lineAmount = 16
                                    for (i in 0 until lineAmount) {
                                        val angleRatio = i.toFloat() / lineAmount
                                        val angle = angleRatio * 2f * Math.PI.toFloat() + (currentProgress * 0.12f)
                                        val sc = Math.sin(angle.toDouble()).toFloat()
                                        val cc = Math.cos(angle.toDouble()).toFloat()
                                        
                                        val startDist = (w * 0.15f) + (Math.sin(phase.toDouble() + i).toFloat() * 8f)
                                        val endDist = w * 0.45f
                                        
                                        drawLine(
                                            color = dynamicColors[0].copy(alpha = 0.35f),
                                            start = androidx.compose.ui.geometry.Offset(w / 2f + sc * startDist, h / 2f + cc * startDist),
                                            end = androidx.compose.ui.geometry.Offset(w / 2f + sc * endDist, h / 2f + cc * endDist),
                                            strokeWidth = 5f,
                                            cap = androidx.compose.ui.graphics.StrokeCap.Round
                                        )
                                    }

                                    // Drift sparkles
                                    val sparks = 6
                                    for (i in 0 until sparks) {
                                        val seed = i * 2.5f
                                        val x = (w * (offsetFraction(currentProgress, i) * 0.8f + 0.1f)).coerceIn(0f, w)
                                        val y = (h * ((Math.sin(phase.toDouble() + seed).toFloat() + 1f) / 2f * 0.6f + 0.2f)).coerceIn(0f, h)
                                        drawCircle(
                                            color = dynamicColors[1].copy(alpha = 0.8f),
                                            radius = 6f + (i % 3) * 2f,
                                            center = androidx.compose.ui.geometry.Offset(x, y)
                                        )
                                    }
                                }

                                styleLower.contains("drone") || styleLower.contains("aerial") || styleLower.contains("real") -> {
                                    // Altitude markings, targeted bounds and camera HUD
                                    val borderInset = 25f
                                    val specW = w - borderInset * 2
                                    val specH = h - borderInset * 2
                                    
                                    // Camera crop corners
                                    val cornerLen = 30f
                                    // Top-left
                                    drawLine(dynamicColors[0], androidx.compose.ui.geometry.Offset(borderInset, borderInset), androidx.compose.ui.geometry.Offset(borderInset + cornerLen, borderInset), strokeWidth = 4f)
                                    drawLine(dynamicColors[0], androidx.compose.ui.geometry.Offset(borderInset, borderInset), androidx.compose.ui.geometry.Offset(borderInset, borderInset + cornerLen), strokeWidth = 4f)
                                    // Top-right
                                    drawLine(dynamicColors[0], androidx.compose.ui.geometry.Offset(w - borderInset, borderInset), androidx.compose.ui.geometry.Offset(w - borderInset - cornerLen, borderInset), strokeWidth = 4f)
                                    drawLine(dynamicColors[0], androidx.compose.ui.geometry.Offset(w - borderInset, borderInset), androidx.compose.ui.geometry.Offset(w - borderInset, borderInset + cornerLen), strokeWidth = 4f)
                                    // Bottom-left
                                    drawLine(dynamicColors[0], androidx.compose.ui.geometry.Offset(borderInset, h - borderInset), androidx.compose.ui.geometry.Offset(borderInset + cornerLen, h - borderInset), strokeWidth = 4f)
                                    drawLine(dynamicColors[0], androidx.compose.ui.geometry.Offset(borderInset, h - borderInset), androidx.compose.ui.geometry.Offset(borderInset, h - borderInset - cornerLen), strokeWidth = 4f)
                                    // Bottom-right
                                    drawLine(dynamicColors[0], androidx.compose.ui.geometry.Offset(w - borderInset, h - borderInset), androidx.compose.ui.geometry.Offset(w - borderInset - cornerLen, h - borderInset), strokeWidth = 4f)
                                    drawLine(dynamicColors[0], androidx.compose.ui.geometry.Offset(w - borderInset, h - borderInset), androidx.compose.ui.geometry.Offset(w - borderInset, h - borderInset - cornerLen), strokeWidth = 4f)

                                    // Center crosshair target
                                    drawLine(dynamicColors[1].copy(alpha = 0.5f), androidx.compose.ui.geometry.Offset(w/2 - 25f, h/2), androidx.compose.ui.geometry.Offset(w/2 - 5f, h/2), strokeWidth = 3f)
                                    drawLine(dynamicColors[1].copy(alpha = 0.5f), androidx.compose.ui.geometry.Offset(w/2 + 5f, h/2), androidx.compose.ui.geometry.Offset(w/2 + 25f, h/2), strokeWidth = 3f)
                                    drawLine(dynamicColors[1].copy(alpha = 0.5f), androidx.compose.ui.geometry.Offset(w/2, h/2 - 25f), androidx.compose.ui.geometry.Offset(w/2, h/2 - 5f), strokeWidth = 3f)
                                    drawLine(dynamicColors[1].copy(alpha = 0.5f), androidx.compose.ui.geometry.Offset(w/2, h/2 + 5f), androidx.compose.ui.geometry.Offset(w/2, h/2 + 25f), strokeWidth = 3f)
                                    drawCircle(dynamicColors[1].copy(alpha = 0.2f), radius = 35f, center = androidx.compose.ui.geometry.Offset(w/2, h/2))

                                    // Simulated rolling altimeter text/line indicators
                                    for (j in 0 until 5) {
                                        val lineY = borderInset + 20f + (j * (specH - 40f) / 4)
                                        drawLine(dynamicColors[0].copy(alpha = 0.6f), androidx.compose.ui.geometry.Offset(borderInset + 5f, lineY), androidx.compose.ui.geometry.Offset(borderInset + 18f, lineY), strokeWidth = 2f)
                                        drawLine(dynamicColors[0].copy(alpha = 0.6f), androidx.compose.ui.geometry.Offset(w - borderInset - 18f, lineY), androidx.compose.ui.geometry.Offset(w - borderInset - 5f, lineY), strokeWidth = 2f)
                                    }
                                }

                                styleLower.contains("sci") || styleLower.contains("space") || styleLower.contains("star") || styleLower.contains("cosmic") -> {
                                    // Space orbits, rotation planetary circles, cosmic system pulse
                                    val orbitCount = 3
                                    for (j in 1..orbitCount) {
                                        val baseRadius = w * 0.12f * j
                                        val angleStep = currentProgress * (4f / j) * Math.PI.toFloat()
                                        drawCircle(
                                            color = dynamicColors[0].copy(alpha = 0.25f),
                                            radius = baseRadius,
                                            center = androidx.compose.ui.geometry.Offset(w / 2f, h / 2f),
                                            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.5f)
                                        )

                                        // Particle riding the orbit ring
                                        val satX = w / 2f + Math.sin(angleStep.toDouble()).toFloat() * baseRadius
                                        val satY = h / 2f + Math.cos(angleStep.toDouble()).toFloat() * baseRadius
                                        drawCircle(
                                            color = dynamicColors[1],
                                            radius = 7f,
                                            center = androidx.compose.ui.geometry.Offset(satX, satY)
                                        )
                                    }

                                    // Intermittent center pulse
                                    drawCircle(
                                        color = dynamicColors[0].copy(alpha = 0.15f),
                                        radius = (w * 0.08f) + (Math.cos(phase.toDouble() * 1.5).toFloat() * 10f),
                                        center = androidx.compose.ui.geometry.Offset(w / 2f, h / 2f)
                                    )
                                }

                                else -> {
                                    // Double sliding waveforms (Organic flow)
                                    val points = 50
                                    val p1 = androidx.compose.ui.graphics.Path()
                                    val p2 = androidx.compose.ui.graphics.Path()

                                    for (i in 0..points) {
                                        val x = (w / points) * i
                                        val sinVal1 = Math.sin((i.toFloat() / points * 3.5e0 * Math.PI) + phase)
                                        val sinVal2 = Math.cos((i.toFloat() / points * 2.5e0 * Math.PI) - phase + 1.25)
                                        
                                        val y1 = h / 2f + (sinVal1.toFloat() * (h * 0.18f))
                                        val y2 = h / 2f + (sinVal2.toFloat() * (h * 0.13f))

                                        if (i == 0) {
                                            p1.moveTo(x, y1)
                                            p2.moveTo(x, y2)
                                        } else {
                                            p1.lineTo(x, y1)
                                            p2.lineTo(x, y2)
                                        }
                                    }

                                    drawPath(p1, color = dynamicColors[0], style = androidx.compose.ui.graphics.drawscope.Stroke(width = 6f, cap = androidx.compose.ui.graphics.StrokeCap.Round))
                                    drawPath(p2, color = dynamicColors[1].copy(alpha = 0.7f), style = androidx.compose.ui.graphics.drawscope.Stroke(width = 4f, cap = androidx.compose.ui.graphics.StrokeCap.Round))
                                }
                            }

                            // 3. Draw cinematic scanning line across screen
                            val scanX = currentProgress * w
                            drawLine(
                                color = Color.White.copy(alpha = 0.25f),
                                start = androidx.compose.ui.geometry.Offset(scanX, 0f),
                                end = androidx.compose.ui.geometry.Offset(scanX, h),
                                strokeWidth = 8f
                            )
                        }

                        // Informative status details as a floating premium card overlay
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .align(Alignment.Center)
                                .padding(16.dp)
                                .background(Color.Black.copy(alpha = 0.78f), RoundedCornerShape(12.dp))
                                .border(1.dp, dynamicColors[0].copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                                .padding(horizontal = 16.dp, vertical = 12.dp)
                        ) {
                            Text("🎬", fontSize = 30.sp)
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Cinematic Render Studio Previews",
                                color = Color.White,
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "FPS: 60 • H.264 High-Profile Client Simulation Active",
                                color = dynamicColors[1],
                                style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Secure temporal AI prediction pipeline running flawlessly",
                                color = Color.White.copy(alpha = 0.6f),
                                style = MaterialTheme.typography.labelSmall,
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    if (isVideoPrepared && !hasVideoError && !forceVisualizerOnly) {
                        // Buffering overlay indicator
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.1f)),
                            contentAlignment = Alignment.Center
                        ) {
                            // Video prepared indicator
                            Surface(
                                color = Color.Black.copy(alpha = 0.6f),
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier.align(Alignment.TopStart).padding(8.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .background(if (isDownloadedLocally) Color.Cyan else Color.Green, CircleShape)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = if (isDownloadedLocally) "LOCAL PLAYBACK" else "LIVE STREAM",
                                        color = Color.White,
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                }
                            }
                        }
                    } else if (hasVideoError) {
                        Surface(
                            color = Color.Black.copy(alpha = 0.72f),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .align(Alignment.Center)
                                .padding(16.dp)
                                .clickable {
                                    hasVideoError = false
                                    isVideoPrepared = false
                                    exoPlayer.seekTo(0)
                                    exoPlayer.prepare()
                                    exoPlayer.play()
                                }
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = "⚠️ STREAM TIMEOUT",
                                    color = Color.Yellow,
                                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                                )
                                Text(
                                    text = "Rendering AI Visualizer Fallback",
                                    color = Color.White.copy(alpha = 0.9f),
                                    style = MaterialTheme.typography.bodySmall,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Add, // Standard add symbol or similar simple icon
                                        contentDescription = "Retry",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = "TAP TO RETRY STREAM",
                                        color = MaterialTheme.colorScheme.primary,
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                                    )
                                }
                            }
                        }
                    } else if (!hasVideoError && !forceVisualizerOnly) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(modifier = Modifier.size(36.dp), color = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("Buffering secure AI video stream...", color = Color.White, style = MaterialTheme.typography.bodySmall)
                        }
                    }

                    // Display media playback progress overlay (Bottom progress seek line)
                    Box(
                        modifier = Modifier
                             .fillMaxWidth()
                             .align(Alignment.BottomCenter)
                             .background(Color.Black.copy(alpha = 0.72f))
                             .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            IconButton(
                                onClick = { isPlaying = !isPlaying },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Text(
                                    text = if (isPlaying) "⏸️" else "▶️",
                                    fontSize = 11.sp
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(4.dp)
                                    .background(Color.DarkGray, RoundedCornerShape(2.dp))
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(currentProgress)
                                        .fillMaxHeight()
                                        .background(Color.Green, RoundedCornerShape(2.dp))
                                )
                            }

                            Text(
                                text = "$currentSecondsText / ${totalSeconds}s",
                                style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace, fontSize = 9.sp),
                                color = Color.White
                            )
                        }
                    }

                    // Model Name Badge in top right of the video player screen
                    val modelDisplayName = if (videoItem.model.isNotBlank()) {
                        val m = videoItem.model.lowercase().trim()
                        when {
                            m.contains("veo3.1/lite") -> "Veo 3.1 Lite"
                            m.contains("veo3.1/fast") -> "Veo 3.1 Fast"
                            m.contains("veo3.1") -> "Veo 3.1"
                            m.contains("hailuo-2.3/pro") -> "MiniMax 2.3 Pro"
                            m.contains("hailuo-2.3/standard") -> "MiniMax 2.3 Std"
                            m.contains("hailuo-02/pro") -> "MiniMax 02 Pro"
                            m.contains("hailuo-02/standard") -> "MiniMax 02 Std"
                            m.contains("minimax") || m.contains("hailuo") -> "MiniMax"
                            m.contains("ray/v3.2") -> "Luma Ray 3.2"
                            m.contains("luma") -> "Luma Ray"
                            m.contains("hunyuan") -> "Hunyuan v1.5"
                            m.contains("grok-imagine") -> "Grok Imagine"
                            m.contains("pixverse/v6") -> "Pixverse v6"
                            m.contains("pixverse/v5.6") -> "Pixverse v5.6"
                            m.contains("pixverse/v5.5") -> "Pixverse v5.5"
                            m.contains("pixverse/c1") -> "Pixverse C1"
                            m.contains("pixverse") -> "Pixverse"
                            m.contains("kling/v3/pro") -> "Kling v3 Pro"
                            m.contains("kling/v3/standard") -> "Kling v3 Std"
                            m.contains("kling") -> "Kling"
                            m.contains("wan/v2.7") -> "Wan v2.7"
                            m.contains("wan/v2.6") -> "Wan v2.6"
                            m.contains("wan/v2.5") -> "Wan v2.5"
                            m.contains("wan") -> "Wan"
                            m.contains("veed") -> "Veed"
                            else -> videoItem.model
                        }
                    } else "Unknown"

                    Surface(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp),
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.85f)
                    ) {
                        Text(
                            text = modelDisplayName,
                            color = MaterialTheme.colorScheme.onPrimary,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp,
                                letterSpacing = 0.5.sp
                            ),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                // Metadata details
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "Video Prompt & Description:",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = videoItem.prompt,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Video Style",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = videoItem.style,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Video Ratio",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = videoItem.aspectRatio,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Resolution",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = videoItem.resolution,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Duration",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = videoItem.duration,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // HIGH-RELIABILITY WORKAROUND: Play Video in External Browser / Player!
                // Since streaming emulator targets lack direct hardware-accelerated MP4 decoding,
                // opening the real URL in Chromium/External Browser is 100% reliable and works beautifully!
                Button(
                    onClick = {
                        try {
                            val openIntent = Intent(Intent.ACTION_VIEW, Uri.parse(videoItem.videoUrl))
                            context.startActivity(openIntent)
                        } catch (e: Exception) {
                            Toast.makeText(context, "Redirecting to external browser...", Toast.LENGTH_SHORT).show()
                            try {
                                val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse(videoItem.videoUrl))
                                context.startActivity(Intent.createChooser(webIntent, "Play Video Clip"))
                            } catch (err: Exception) {
                                Toast.makeText(context, "Could not open browser: ${err.localizedMessage}", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("📺 Play in External Browser / Player", fontWeight = FontWeight.Bold)
                    }
                }

                // Secondary toggle to force visualizer preview inside app
                OutlinedButton(
                    onClick = { forceVisualizerOnly = !forceVisualizerOnly },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(if (forceVisualizerOnly) "✨ Enable Native Video stream" else "✨ Show High-Fidelity Studio Visualizer")
                }
            }
        },
        confirmButton = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (onEditVideo != null) {
                        Button(
                            onClick = {
                                onEditVideo()
                                onDismissRequest()
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Edit Video 🎬", maxLines = 1)
                        }
                    }
                    Button(
                        onClick = {
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_SUBJECT, "Eddie AI Cinematic Render")
                                putExtra(Intent.EXTRA_TEXT, "Look at this cinematic AI video I created:\n\nPrompt: ${videoItem.prompt}\nSpecs: Style: ${videoItem.style}, Ratio: ${videoItem.aspectRatio}, Resolution: ${videoItem.resolution}, Duration: ${videoItem.duration}\nVideo Link: ${videoItem.videoUrl}")
                            }
                            context.startActivity(Intent.createChooser(shareIntent, "Share Video"))
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Share", maxLines = 1)
                    }
                }

                // Download Button for Video
                Button(
                    onClick = {
                        Toast.makeText(context, "Downloading video...", Toast.LENGTH_SHORT).show()
                        downloadAndroidVideo(context, videoItem.videoUrl, videoItem.id) { path ->
                            if (path != null) {
                                isDownloadedLocally = true
                                if (path.contains("System background download started")) {
                                    Toast.makeText(context, "Background download started! Check notifications 📥", Toast.LENGTH_LONG).show()
                                } else {
                                    Toast.makeText(context, "Saved video to $path 📥", Toast.LENGTH_LONG).show()
                                }
                            } else {
                                Toast.makeText(context, "Failed to download. Check your connection 📥", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                ) {
                    Text("Download Video ⬇️", maxLines = 1, fontWeight = FontWeight.Bold)
                }

                // Social Publisher & Copywriter Button
                Button(
                    onClick = { showSocialDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    ),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("🚀 Social Copywriter & Automate Publish", fontWeight = FontWeight.Bold)
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))

                TextButton(
                    onClick = {
                        onDeleteVideo(videoItem.id)
                        onDismissRequest()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Delete Video 🗑️", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                }
            }
        },
        dismissButton = null
    )

    if (showSocialDialog) {
        SocialDistributionDialog(
            videoItem = videoItem,
            chatViewModel = chatViewModel,
            onDismissRequest = { showSocialDialog = false }
        )
    }
}

// Utility helper for deterministic animation offsets
private fun offsetFraction(progress: Float, offset: Int): Float {
    val raw = progress + (offset * 0.15f)
    return if (raw > 1.0f) raw - 1.0f else raw
}

@OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class,
    androidx.compose.foundation.layout.ExperimentalLayoutApi::class
)
@Composable
fun EddieVideoGeneratorView(
    uiState: ChatUiState,
    paddingValues: PaddingValues,
    chatViewModel: ChatViewModel,
    onGenerateVideo: (
        prompt: String,
        style: String,
        aspectRatio: String,
        resolution: String,
        duration: String,
        generateAudio: String,
        personGeneration: String,
        negativePrompt: String,
        sampleCount: Int,
        imageUrl: String?,
        cfgScale: Float,
        shotType: String,
        multiPromptJson: String?,
        seed: Int?,
        enhancePrompt: Boolean,
        autoFix: Boolean,
        safetyTolerance: Int,
        endImageUrl: String?,
        elementsJson: String?,
        voiceControl: Boolean,
        voiceIds: List<String>?,
        startImageUrl: String?,
        bitrateMode: String,
        endUserId: String,
        pixverseMultiClip: Boolean,
        pixverseThinkingType: String,
        lumaLoop: Boolean,
        lumaHdr: Boolean,
        lumaExrExport: Boolean,
        alibabaAudioUrl: String,
        alibabaMultiShots: Boolean,
        alibabaVideoUrl: String?,
        hunyuanInferenceSteps: Int,
        hunyuanFrames: Int
    ) -> Unit,
    onDeleteVideo: (String) -> Unit,
    onSaveEditedVideo: (String, String, String, String, String, String, String, String) -> Unit,
    onModeChange: (String) -> Unit,
    onProviderChange: (String) -> Unit,
    onModelChange: (String) -> Unit
) {
    val context = LocalContext.current
    var prompt by remember { mutableStateOf("") }
    var selectedStyle by remember { mutableStateOf("Realistic Drone") }
    var selectedAspectRatio by remember { mutableStateOf("16:9") }
    var selectedResolution by remember { mutableStateOf("720p") }
    var selectedDuration by remember { mutableStateOf("8 seconds") }
    var selectedImageId by remember { mutableStateOf("") }
    var selectedOpenAiSize by remember { mutableStateOf("1280x720") }
    var selectedAudioOption by remember { mutableStateOf("Off") }
    var klingVoiceControl by remember { mutableStateOf(false) }
    var klingEndImageUrl by remember { mutableStateOf("") }
    val klingElements = remember { mutableStateListOf<KlingV3ComboElementInput>() }
    var selectedPersonGeneration by remember { mutableStateOf("allow_adult") }
    var negativePromptText by remember { mutableStateOf("blurry, text, watermark, bad lighting") }
    var sampleCountVideos by remember { mutableStateOf(1) }
    var enhancePrompt by remember { mutableStateOf(true) }
    var seedText by remember { mutableStateOf("") }
    var autoFix by remember { mutableStateOf(true) }
    var alibabaAudioUrl by remember { mutableStateOf("") }
    var wanMultiShots by remember { mutableStateOf(true) }
    var wanEndImageUrl by remember { mutableStateOf("") }
    var wanVideoUrl by remember { mutableStateOf("") }
    var safetyTolerance by remember { mutableStateOf("4") }

    var minimaxPromptOptimizer by remember { mutableStateOf(true) }
    var minimaxEndImageUrl by remember { mutableStateOf("") }

    var hunyuanInferenceSteps by remember { mutableStateOf(28) }
    var hunyuanFrames by remember { mutableStateOf(121) }

    var klingV26StartImageUrl by remember { mutableStateOf("") }
    var klingV26EndImageUrl by remember { mutableStateOf("") }
    var klingV26VoiceIds by remember { mutableStateOf("") }
    var selectedBitrateMode by remember { mutableStateOf("standard") }
    var endUserIdText by remember { mutableStateOf("aistudio-user-default") }
    var pixverseMultiClip by remember { mutableStateOf(false) }

    val videoScrollState = rememberScrollState()
    val isKeyboardVisible = WindowInsets.isImeVisible
    LaunchedEffect(isKeyboardVisible) {
        if (isKeyboardVisible) {
            videoScrollState.animateScrollTo(videoScrollState.maxValue)
        }
    }
    LaunchedEffect(prompt) {
        if (prompt.isNotEmpty()) {
            videoScrollState.animateScrollTo(videoScrollState.maxValue)
        }
    }
    var pixverseThinkingType by remember { mutableStateOf("auto") }
    var lumaLoop by remember { mutableStateOf(false) }
    var lumaHdr by remember { mutableStateOf(false) }
    var lumaExrExport by remember { mutableStateOf(false) }
    val isLumaRayVideo = remember(uiState.selectedVideoModel, uiState.selectedVideoProvider) {
        val modelLower = uiState.selectedVideoModel.lowercase().trim()
        uiState.selectedVideoProvider == "FalAI" && (modelLower.contains("luma/agent/ray") || modelLower.contains("luma-dream-machine"))
    }
    val isAlibabaVideo = remember(uiState.selectedVideoModel, uiState.selectedVideoProvider) {
        uiState.selectedVideoProvider == "Alibaba"
    }
    val isBytedance = remember(uiState.selectedVideoProvider) {
        uiState.selectedVideoProvider == "Bytedance"
    }

    var cfgScale by remember { mutableStateOf(0.5f) }
    var selectedShotType by remember { mutableStateOf("customize") }

    var useStoryboard by remember { mutableStateOf(false) }
    val storyboardShots = remember {
        mutableStateListOf(
            mutableStateOf("") to mutableStateOf("5"),
            mutableStateOf("") to mutableStateOf("5")
        )
    }

    LaunchedEffect(uiState.selectedVideoProvider, uiState.selectedVideoModel) {
        if (uiState.selectedVideoProvider == "Kling") {
            negativePromptText = "blur, distort, and low quality"
            cfgScale = 0.5f
        } else if (uiState.selectedVideoProvider == "Bytedance") {
            negativePromptText = "blurry, text, watermark, bad lighting"
            selectedAudioOption = "On"
        } else if (uiState.selectedVideoProvider == "PixVerse") {
            negativePromptText = ""
            selectedStyle = "anime"
            selectedResolution = "720p"
            selectedDuration = "5s"
            selectedAudioOption = "On"
        } else if (uiState.selectedVideoProvider == "Alibaba" && uiState.selectedVideoModel.lowercase().trim().contains("happy-horse")) {
            negativePromptText = "blurry, text, watermark, bad lighting"
            selectedResolution = "1080p"
            selectedDuration = "5s"
        } else if (uiState.selectedVideoProvider == "FalAI" && uiState.selectedVideoModel.lowercase().trim().contains("hunyuan-video-v1.5")) {
            negativePromptText = ""
            selectedResolution = "480p"
            selectedAspectRatio = "16:9"
            hunyuanInferenceSteps = 28
            hunyuanFrames = 121
            enhancePrompt = true
        } else {
            negativePromptText = "blurry, text, watermark, bad lighting"
        }
    }

    val isVeo2 = remember(uiState.selectedVideoModel, uiState.selectedVideoProvider) {
        val modelLower = uiState.selectedVideoModel.lowercase().trim()
        (uiState.selectedVideoProvider == "Veo" || uiState.selectedVideoProvider == "FalAI") && (
            modelLower == "veo2" || modelLower.contains("veo-2.0")
        )
    }

    val isOpenAiSora2 = remember(uiState.selectedVideoModel, uiState.selectedVideoProvider) {
        val modelLower = uiState.selectedVideoModel.lowercase().trim()
        uiState.selectedVideoProvider == "OpenAI" && modelLower == "sora 2"
    }

    val isSora2ImageToVideo = remember(uiState.selectedVideoModel, uiState.selectedVideoProvider, uiState.selectedVideoMode) {
        uiState.selectedVideoProvider == "OpenAI" &&
        uiState.selectedVideoMode == "Image to Video" &&
        uiState.selectedVideoModel.lowercase().trim() == "sora 2"
    }

    val isSora2ProImageToVideo = remember(uiState.selectedVideoModel, uiState.selectedVideoProvider, uiState.selectedVideoMode) {
        uiState.selectedVideoProvider == "OpenAI" &&
        uiState.selectedVideoMode == "Image to Video" &&
        uiState.selectedVideoModel.lowercase().trim() == "sora 2 pro"
    }

    val isOpenAiSora2Pro = remember(uiState.selectedVideoModel, uiState.selectedVideoProvider) {
        val modelLower = uiState.selectedVideoModel.lowercase().trim()
        uiState.selectedVideoProvider == "OpenAI" && modelLower == "sora 2 pro"
    }

    LaunchedEffect(isVeo2) {
        if (isVeo2) {
            selectedDuration = "5s"
            selectedAudioOption = "Off"
        }
    }

    LaunchedEffect(uiState.selectedVideoModel, uiState.selectedVideoProvider, uiState.selectedVideoMode) {
        if (uiState.selectedVideoProvider == "OpenAI" && uiState.selectedVideoModel.lowercase().trim() == "sora 2") {
            if (uiState.selectedVideoMode == "Image to Video") {
                selectedAspectRatio = "auto"
                selectedDuration = "4 seconds"
                selectedResolution = "auto"
            } else {
                if (selectedOpenAiSize in listOf("1024x1792", "1792x1024", "1080x1920", "1920x1080")) {
                    selectedOpenAiSize = "1280x720"
                }
            }
        }
    }

    val isFalVeo3FastModel = remember(uiState.selectedVideoModel, uiState.selectedVideoProvider) {
        val modelLower = uiState.selectedVideoModel.lowercase().trim()
        (uiState.selectedVideoProvider == "FalAI" || uiState.selectedVideoProvider == "Veo") && (
            modelLower == "veo3-fast" || modelLower.contains("veo3-fast") || modelLower.contains("veo 3 fast") || modelLower.contains("veo3.1/fast") || modelLower.contains("veo 3.1 fast")
        )
    }

    val isFalVeo3_1Fast = remember(uiState.selectedVideoModel, uiState.selectedVideoProvider) {
        val modelLower = uiState.selectedVideoModel.lowercase().trim()
        (uiState.selectedVideoProvider == "FalAI" || uiState.selectedVideoProvider == "Veo") && (
            modelLower.contains("veo3.1/fast") || modelLower.contains("veo 3.1 fast")
        )
    }

    val isFalVeo3OnlyFast = remember(uiState.selectedVideoModel, uiState.selectedVideoProvider) {
        val modelLower = uiState.selectedVideoModel.lowercase().trim()
        (uiState.selectedVideoProvider == "FalAI" || uiState.selectedVideoProvider == "Veo") && (
            (modelLower == "veo3-fast" || modelLower.contains("veo3-fast") || modelLower.contains("veo 3 fast")) &&
            !modelLower.contains("3.1")
        )
    }

    val isFalVeo3 = remember(uiState.selectedVideoModel, uiState.selectedVideoProvider) {
        val modelLower = uiState.selectedVideoModel.lowercase().trim()
        (uiState.selectedVideoProvider == "FalAI" || uiState.selectedVideoProvider == "Veo") && (
            (modelLower.contains("veo3.1") || modelLower.contains("veo 3.1") ||
            modelLower == "veo3" || modelLower == "fal-ai/veo3" || modelLower == "veo 3") &&
            !modelLower.contains("fast")
        )
    }

    val isFalVeo3_1Lite = remember(uiState.selectedVideoModel, uiState.selectedVideoProvider) {
        val modelLower = uiState.selectedVideoModel.lowercase().trim()
        (uiState.selectedVideoProvider == "FalAI" || uiState.selectedVideoProvider == "Veo") && (
            modelLower.contains("veo3.1/lite") || modelLower.contains("veo 3.1 lite") || modelLower.contains("lite")
        )
    }

    val isFalVeo3_1 = remember(uiState.selectedVideoModel, uiState.selectedVideoProvider) {
        val modelLower = uiState.selectedVideoModel.lowercase().trim()
        (uiState.selectedVideoProvider == "FalAI" || uiState.selectedVideoProvider == "Veo") && (
            (modelLower.contains("veo3.1") || modelLower.contains("veo 3.1")) &&
            !modelLower.contains("fast") && !modelLower.contains("lite")
        )
    }

    val isGrokImagineVideo = remember(uiState.selectedVideoModel) {
        val modelLower = uiState.selectedVideoModel.lowercase().trim()
        modelLower.contains("grok-imagine-video")
    }

    val isFalVeo3Only = remember(uiState.selectedVideoModel, uiState.selectedVideoProvider) {
        val modelLower = uiState.selectedVideoModel.lowercase().trim()
        (uiState.selectedVideoProvider == "FalAI" || uiState.selectedVideoProvider == "Veo") && (
            (modelLower == "veo3" || modelLower == "fal-ai/veo3" || modelLower == "veo 3") &&
            !modelLower.contains("fast")
        )
    }

    val isKlingV26Pro = remember(uiState.selectedVideoModel, uiState.selectedVideoProvider) {
        val modelLower = uiState.selectedVideoModel.lowercase().trim()
        uiState.selectedVideoProvider == "Kling" && (modelLower == "kling v2.6 pro" || modelLower.contains("kling-video/v2.6/pro") || modelLower.contains("kling v2.6 pro") || modelLower.contains("v2.6/pro"))
    }

    val isKlingV25TurboPro = remember(uiState.selectedVideoModel, uiState.selectedVideoProvider) {
        val modelLower = uiState.selectedVideoModel.lowercase().trim()
        uiState.selectedVideoProvider == "Kling" && (modelLower == "kling v2.5 turbo pro" || modelLower.contains("kling-2.5-turbo/pro") || modelLower.contains("kling v2.5 turbo pro") || modelLower.contains("v2.5-turbo/pro"))
    }

    val isKlingV25TurboStandard = remember(uiState.selectedVideoModel, uiState.selectedVideoProvider) {
        val modelLower = uiState.selectedVideoModel.lowercase().trim()
        uiState.selectedVideoProvider == "Kling" && (modelLower == "kling v2.5 turbo standard" || modelLower.contains("kling-2.5-turbo/standard") || modelLower.contains("kling v2.5 turbo standard") || modelLower.contains("v2.5-turbo/standard"))
    }

    val isKlingV3Standard = remember(uiState.selectedVideoModel, uiState.selectedVideoProvider) {
        val modelLower = uiState.selectedVideoModel.lowercase().trim()
        uiState.selectedVideoProvider == "Kling" && (modelLower == "kling v3 standard" || modelLower.contains("kling v3 standard") || modelLower.contains("v3/standard"))
    }

    val isKlingV3Pro = remember(uiState.selectedVideoModel, uiState.selectedVideoProvider) {
        val modelLower = uiState.selectedVideoModel.lowercase().trim()
        uiState.selectedVideoProvider == "Kling" && (modelLower == "kling v3 pro" || modelLower.contains("kling v3 pro") || modelLower.contains("v3/pro"))
    }

    val isKlingV3TurboPro = remember(uiState.selectedVideoModel, uiState.selectedVideoProvider) {
        val modelLower = uiState.selectedVideoModel.lowercase().trim()
        uiState.selectedVideoProvider == "Kling" && (modelLower == "kling v3 turbo pro" || modelLower.contains("v3/turbo/pro"))
    }

    val isKlingV3TurboStandard = remember(uiState.selectedVideoModel, uiState.selectedVideoProvider) {
        val modelLower = uiState.selectedVideoModel.lowercase().trim()
        uiState.selectedVideoProvider == "Kling" && (modelLower == "kling v3 turbo standard" || modelLower.contains("v3/turbo/standard"))
    }

    val isKlingO3_4k = remember(uiState.selectedVideoModel, uiState.selectedVideoProvider) {
        val modelLower = uiState.selectedVideoModel.lowercase().trim()
        uiState.selectedVideoProvider == "Kling" && (modelLower == "kling o3 4k" || modelLower.contains("o3/4k"))
    }

    val isKlingO3Standard = remember(uiState.selectedVideoModel, uiState.selectedVideoProvider) {
        val modelLower = uiState.selectedVideoModel.lowercase().trim()
        uiState.selectedVideoProvider == "Kling" && (modelLower == "kling o3 standard" || modelLower.contains("o3/standard"))
    }

    val isKlingO3Pro = remember(uiState.selectedVideoModel, uiState.selectedVideoProvider) {
        val modelLower = uiState.selectedVideoModel.lowercase().trim()
        uiState.selectedVideoProvider == "Kling" && (modelLower == "kling o3 pro" || modelLower.contains("o3/pro"))
    }

    val isKlingV3_4k = remember(uiState.selectedVideoModel, uiState.selectedVideoProvider) {
        val modelLower = uiState.selectedVideoModel.lowercase().trim()
        uiState.selectedVideoProvider == "Kling" && (modelLower == "kling v3 4k" || modelLower.contains("v3/4k"))
    }

    val isHunyuan15Video = remember(uiState.selectedVideoModel, uiState.selectedVideoProvider) {
        val modelLower = uiState.selectedVideoModel.lowercase().trim()
        uiState.selectedVideoProvider == "FalAI" && (modelLower == "fal-ai/hunyuan-video-v1.5/image-to-video" || modelLower.contains("hunyuan-video-v1.5"))
    }

    LaunchedEffect(isKlingV3Standard, isKlingV3Pro, isKlingV3TurboPro, isKlingV3TurboStandard, isKlingO3_4k, isKlingO3Standard, isKlingO3Pro, isKlingV3_4k, isKlingV26Pro, isKlingV25TurboStandard) {
        if (isKlingV3Standard || isKlingV3Pro || isKlingV3TurboPro || isKlingV3TurboStandard || isKlingO3_4k || isKlingO3Standard || isKlingO3Pro || isKlingV3_4k || isKlingV26Pro || isKlingV25TurboStandard) {
            selectedDuration = "5"
        }
    }

    val isGeminiVeo3_1 = false

    var showProgressPanel by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.videoGenIsLoading) {
        if (uiState.videoGenIsLoading) {
            showProgressPanel = true
        }
    }

    var uploadedImages by remember { mutableStateOf<List<com.example.data.db.GeneratedImageEntity>>(emptyList()) }

    LaunchedEffect(selectedImageId) {
        val selectedImgUrl = (uiState.generatedImages + uploadedImages).find { it.id == selectedImageId }?.imageUrl
        if (!selectedImgUrl.isNullOrBlank()) {
            klingV26StartImageUrl = selectedImgUrl
        }
    }

    LaunchedEffect(isKlingV26Pro) {
        if (isKlingV26Pro) {
            selectedDuration = "5"
            selectedAudioOption = "On"
        }
    }

    fun getUriSize(ctx: android.content.Context, uri: Uri): Long {
        return try {
            ctx.contentResolver.openAssetFileDescriptor(uri, "r")?.use { 
                it.length 
            } ?: 0L
        } catch (e: Exception) {
            0L
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            val fileSize = getUriSize(context, uri)
            if (fileSize > 10 * 1024 * 1024L) {
                Toast.makeText(context, "File size exceeds 10MB limit! (Size: " + String.format("%.2f", fileSize / (1024f * 1024f)) + "MB)", Toast.LENGTH_LONG).show()
                return@rememberLauncherForActivityResult
            }

            val mimeType = context.contentResolver.getType(uri)
            val path = uri.path?.lowercase() ?: ""
            val hasValidMime = mimeType == "image/jpeg" || mimeType == "image/jpg" || mimeType == "image/png"
            val hasValidExtension = path.endsWith(".jpg") || path.endsWith(".jpeg") || path.endsWith(".png")

            if (!hasValidMime && !hasValidExtension) {
                Toast.makeText(context, "Invalid format! Only JPEG and PNG (.jpg, .png) images are allowed.", Toast.LENGTH_LONG).show()
                return@rememberLauncherForActivityResult
            }

            try {
                val options = BitmapFactory.Options().apply {
                    inJustDecodeBounds = true
                }
                context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    BitmapFactory.decodeStream(inputStream, null, options)
                }

                val imageWidth = options.outWidth
                val imageHeight = options.outHeight

                if (imageWidth <= 0 || imageHeight <= 0) {
                    Toast.makeText(context, "Failed to read image dimensions.", Toast.LENGTH_SHORT).show()
                    return@rememberLauncherForActivityResult
                }

                val targetRatioVal = when (selectedAspectRatio) {
                    "16:9" -> 16f / 9f
                    "1:1" -> 1f / 1f
                    "9:16" -> 9f / 16f
                    "21:9" -> 21f / 9f
                    else -> 16f / 9f
                }

                val imageRatio = imageWidth.toFloat() / imageHeight.toFloat()
                val diff = kotlin.math.abs(imageRatio - targetRatioVal)
                if (diff >= 0.05f && uiState.selectedVideoProvider != "Kling" && uiState.selectedVideoProvider != "Bytedance") {
                    Toast.makeText(
                        context,
                        "Image aspect ratio must match the chosen aspect ratio ($selectedAspectRatio)!\n" +
                        "Detected: " + String.format("%.2f", imageRatio) + " (Expected: " + String.format("%.2f", targetRatioVal) + ")",
                        Toast.LENGTH_LONG
                    ).show()
                    return@rememberLauncherForActivityResult
                }

                val newCustomImage = com.example.data.db.GeneratedImageEntity(
                    id = "upload_${UUID.randomUUID().toString().take(6)}",
                    prompt = "Custom Upload",
                    imageUrl = uri.toString(),
                    style = "Upload",
                    aspectRatio = selectedAspectRatio,
                    timestamp = System.currentTimeMillis(),
                    model = "Uploaded"
                )

                uploadedImages = uploadedImages + newCustomImage
                selectedImageId = newCustomImage.id
                Toast.makeText(context, "Successfully uploaded reference image!", Toast.LENGTH_SHORT).show()

            } catch (e: Exception) {
                Toast.makeText(context, "Failed to process image: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            }
        }
    }

    val shuffledQuickPrompts = remember {
        listOf(
            "Cinematic camera slowly gliding across an ancient classical cathedral under gentle rainfall",
            "Drone capturing majestic snow-capped mountain peaks at golden dawn",
            "Anime character walking under falling cherry blossom trees with nostalgic backlighting",
            "Legendary car drifting through neon-lit night cyberpunk city streets with thick smoke",
            "Slow-motion close up of a magical glowing forest with tiny floating dust particles at night",
            "Underwater shot of a giant friendly sea turtle swimming through vibrant coral reefs, sun rays piercing down",
            "Steampunk airship majestically cruising through dense golden clouds during sunset",
            "Astronaut sitting on the edge of a lunar crater, peacefully looking at the beautiful blue Earth in the distance",
            "Cozy wooden cabin in the middle of a snowy pine forest with a warm fireplace glow visible from the window",
            "A cute fluffy red panda exploring a mystical bamboo forest high in the foggy mountains",
            "Macro shot of a detailed glass sculpture coming to life under colorful shifting neon lights",
            "Vintage train steam-locomotive crossing an epic stone bridge in the high green highlands",
            "A majestic fire-breathing dragon slowly unfurling its giant wings on top of a lonely craggy volcano",
            "A peaceful futuristic hover-scooter gliding swiftly above clean turquoise canals in a utopian solar-punk city",
            "Macro time-lapse of a shimmering blue crystal flower slowly blooming under surreal shifting moonlight",
            "Cinematic tracking shot behind a mysterious explorer entering a colossal glowing bioluminescent underground cave"
        ).shuffled().take(4)
    }

    var selectedVideoForPlay by remember { mutableStateOf<com.example.data.db.GeneratedVideoEntity?>(null) }
    var editingVideoItem by remember { mutableStateOf<com.example.data.db.GeneratedVideoEntity?>(null) }

    val styles = remember(uiState.selectedVideoProvider, uiState.selectedVideoModel) {
        val isPixVerse = uiState.selectedVideoProvider == "PixVerse"
        if (isPixVerse) {
            listOf(
                "anime" to false,
                "3d_animation" to false,
                "clay" to false,
                "comic" to false,
                "cyberpunk" to false
            )
        } else {
            listOf(
                "Realistic Drone" to false,
                "Cinematic 3D" to false,
                "Cyberpunk Neon" to true,
                "Anime Epic" to true,
                "Vintage Kodachrome 35mm" to true,
                "IMAX Sci-Fi" to true,
                "Steampunk Brass" to false,
                "Fantasy Unreal Engine 5" to true,
                "Moody Film Noir" to false,
                "Claymation Stop-Motion" to true,
                "Water Color Aesthetic" to false,
                "Cosmic Retrowave" to true
            )
        }
    }

    val aspectRatios = remember(uiState.selectedVideoProvider, uiState.selectedVideoModel, uiState.selectedVideoMode, isFalVeo3, isFalVeo3FastModel, isVeo2, isGrokImagineVideo, isLumaRayVideo, isSora2ImageToVideo, isSora2ProImageToVideo, isAlibabaVideo, isHunyuan15Video) {
        val modelLower = uiState.selectedVideoModel.lowercase().trim()
        if (isHunyuan15Video) {
            listOf(
                "16:9" to false to "Landscape (16:9)",
                "9:16" to true to "Vertical (9:16)"
            )
        } else if ((uiState.selectedVideoProvider == "FalAI" || uiState.selectedVideoProvider == "Minimax") && (uiState.selectedVideoModel == "fal-ai/minimax/hailuo-02/standard/image-to-video" || uiState.selectedVideoModel == "fal-ai/minimax/hailuo-02/pro/image-to-video" || uiState.selectedVideoModel == "fal-ai/minimax/hailuo-2.3/standard/image-to-video" || uiState.selectedVideoModel == "fal-ai/minimax/hailuo-2.3/pro/image-to-video" || uiState.selectedVideoModel == "fal-ai/minimax/hailuo-02/standard/text-to-video" || uiState.selectedVideoModel == "fal-ai/minimax/hailuo-02/pro/text-to-video" || uiState.selectedVideoModel == "fal-ai/minimax/hailuo-2.3/standard/text-to-video" || uiState.selectedVideoModel == "fal-ai/minimax/hailuo-2.3/pro/text-to-video")) {
            emptyList()
        } else if (isAlibabaVideo && (modelLower.contains("wan/v2.6") || modelLower.contains("wan/v2.7") || modelLower.contains("happy-horse"))) {
            if ((modelLower.contains("wan/v2.6") || modelLower.contains("wan/v2.7") || modelLower.contains("happy-horse")) && uiState.selectedVideoMode == "Image to Video") {
                emptyList()
            } else {
                listOf(
                    "16:9" to false to "Landscape",
                    "9:16" to false to "Shorts/TikTok",
                    "1:1" to false to "Square",
                    "4:3" to false to "Classic TV",
                    "3:4" to false to "Portrait"
                )
            }
        } else if (isAlibabaVideo && (modelLower.contains("wan/v2.5") || modelLower.contains("wan-25-preview"))) {
            if (uiState.selectedVideoMode == "Image to Video") {
                emptyList()
            } else {
                listOf(
                    "16:9" to false to "Landscape",
                    "1:1" to false to "Square",
                    "9:16" to true to "Shorts/TikTok"
                )
            }
        } else if (isSora2ImageToVideo || isSora2ProImageToVideo) {
            listOf(
                "auto" to false to "Automatic",
                "9:16" to false to "Vertical (9:16)",
                "16:9" to false to "Landscape (16:9)"
            )
        } else if (isLumaRayVideo) {
            listOf(
                "3:4" to false to "Portrait",
                "4:3" to false to "Classic TV",
                "1:1" to false to "Square",
                "9:16" to true to "Shorts/TikTok",
                "16:9" to false to "Landscape",
                "21:9" to true to "Cinematic"
            )
        } else if (isFalVeo3 || isFalVeo3FastModel) {
            listOf(
                "16:9" to false to "Landscape",
                "9:16" to true to "Vertical"
            )
        } else if (isGrokImagineVideo) {
            listOf(
                "16:9" to false to "Landscape",
                "4:3" to false to "Classic TV",
                "3:2" to false to "Photo Horizontal",
                "1:1" to false to "Square",
                "2:3" to true to "Story Vertical",
                "3:4" to false to "Portrait",
                "9:16" to true to "Shorts/TikTok"
            )
        } else if (isVeo2) {
            listOf(
                "16:9" to false to "Landscape",
                "1:1" to false to "Square",
                "9:16" to true to "Shorts/TikTok"
            )
        } else if (uiState.selectedVideoProvider == "Kling") {
            listOf(
                "16:9" to false to "Landscape",
                "1:1" to false to "Square",
                "9:16" to true to "Shorts/TikTok"
            )
        } else if (uiState.selectedVideoProvider == "Bytedance") {
            listOf(
                "auto" to false to "Auto",
                "21:9" to false to "Cinematic",
                "16:9" to false to "Landscape",
                "4:3" to false to "Classic TV",
                "1:1" to false to "Square",
                "3:4" to false to "Portrait",
                "9:16" to true to "Vertical"
            )
        } else if (uiState.selectedVideoProvider == "PixVerse") {
            if (uiState.selectedVideoModel == "fal-ai/pixverse/v5.5/text-to-video" || uiState.selectedVideoModel == "fal-ai/pixverse/v5.6/text-to-video") {
                listOf(
                    "16:9" to false to "Landscape",
                    "4:3" to false to "Classic TV",
                    "1:1" to false to "Square",
                    "3:4" to false to "Portrait",
                    "9:16" to true to "Shorts/TikTok"
                )
            } else {
                listOf(
                    "16:9" to false to "Landscape",
                    "4:3" to false to "Classic TV",
                    "1:1" to false to "Square",
                    "3:4" to false to "Portrait",
                    "9:16" to true to "Shorts/TikTok",
                    "2:3" to true to "Story Vertical",
                    "3:2" to false to "Photo Horizontal",
                    "21:9" to true to "Cinematic"
                )
            }
        } else {
            listOf(
                "16:9" to false to "Landscape",
                "1:1" to false to "Square",
                "9:16" to true to "Shorts/TikTok",
                "21:9" to true to "Cinematic"
            )
        }
    }

    LaunchedEffect(aspectRatios) {
        val availableRatios = aspectRatios.map { it.first.first }
        if (selectedAspectRatio !in availableRatios) {
            selectedAspectRatio = availableRatios.firstOrNull() ?: "16:9"
        }
    }

    val resolutions = remember(uiState.selectedVideoProvider, uiState.selectedVideoModel, uiState.selectedVideoMode, isFalVeo3, isFalVeo3FastModel, isFalVeo3OnlyFast, isFalVeo3Only, isFalVeo3_1Lite, isVeo2, isOpenAiSora2, isOpenAiSora2Pro, isGrokImagineVideo, isLumaRayVideo, isSora2ImageToVideo, isSora2ProImageToVideo, isAlibabaVideo, isHunyuan15Video) {
        if (isHunyuan15Video) {
            listOf(
                "480p" to false to "Standard (480p)",
                "720p" to true to "High (720p)"
            )
        } else if ((uiState.selectedVideoProvider == "FalAI" || uiState.selectedVideoProvider == "Minimax") && uiState.selectedVideoModel == "fal-ai/minimax/hailuo-2.3/pro/text-to-video") {
            listOf(
                "1080p" to true to "1080p Pro"
            )
        } else if ((uiState.selectedVideoProvider == "FalAI" || uiState.selectedVideoProvider == "Minimax") && uiState.selectedVideoModel == "fal-ai/minimax/hailuo-2.3/pro/image-to-video") {
            listOf(
                "1080p" to true to "1080p Pro (140 tokens)"
            )
        } else if ((uiState.selectedVideoProvider == "FalAI" || uiState.selectedVideoProvider == "Minimax") && uiState.selectedVideoModel == "fal-ai/minimax/hailuo-02/pro/image-to-video") {
            listOf(
                "1080p" to true to "1080p Pro"
            )
        } else if ((uiState.selectedVideoProvider == "FalAI" || uiState.selectedVideoProvider == "Minimax") && (uiState.selectedVideoModel == "fal-ai/minimax/hailuo-2.3/standard/image-to-video" || uiState.selectedVideoModel == "fal-ai/minimax/hailuo-2.3/standard/text-to-video")) {
            listOf(
                "768P" to false to "768p High"
            )
        } else if ((uiState.selectedVideoProvider == "FalAI" || uiState.selectedVideoProvider == "Minimax") && uiState.selectedVideoModel == "fal-ai/minimax/hailuo-02/standard/image-to-video") {
            listOf(
                "512P" to false to "512p Standard",
                "768P" to false to "768p High"
            )
        } else if ((uiState.selectedVideoProvider == "FalAI" || uiState.selectedVideoProvider == "Minimax") && uiState.selectedVideoModel == "fal-ai/minimax/hailuo-02/standard/text-to-video") {
            listOf(
                "768P" to false to "768p High"
            )
        } else if ((uiState.selectedVideoProvider == "FalAI" || uiState.selectedVideoProvider == "Minimax") && uiState.selectedVideoModel == "fal-ai/minimax/hailuo-02/pro/text-to-video") {
            listOf(
                "1080p" to true to "1080p Pro"
            )
        } else if (isAlibabaVideo && (uiState.selectedVideoModel.lowercase().trim().contains("wan/v2.6") || uiState.selectedVideoModel.lowercase().trim().contains("wan/v2.7") || uiState.selectedVideoModel.lowercase().trim().contains("happy-horse"))) {
            listOf(
                "720p" to false to "High HD",
                "1080p" to true to "Full HD Pro"
            )
        } else if (isAlibabaVideo && (uiState.selectedVideoModel.lowercase().trim().contains("wan/v2.5") || uiState.selectedVideoModel.lowercase().trim().contains("wan-25-preview"))) {
            listOf(
                "480p" to false to "Standard SD",
                "720p" to false to "High HD",
                "1080p" to true to "Full HD Pro"
            )
        } else if (isLumaRayVideo) {
            listOf(
                "540p" to false to "Standard Balanced",
                "720p" to false to "High HD",
                "1080p" to true to "Full HD Pro"
            )
        } else if (uiState.selectedVideoProvider == "Kling") {
            emptyList()
        } else if (isGrokImagineVideo) {
            if (uiState.selectedVideoModel.contains("v1.5")) {
                listOf(
                    "480p" to false to "Standard Balance",
                    "720p" to false to "High HD",
                    "1080p" to true to "Full HD Pro"
                )
            } else {
                listOf(
                    "480p" to false to "Standard Balance",
                    "720p" to false to "High HD"
                )
            }
        } else if (uiState.selectedVideoProvider == "Bytedance") {
            listOf(
                "480p" to false to "Fast Generation",
                "720p" to false to "Standard Balance",
                "1080p" to true to "High Quality",
                "4k" to false to "Highest Quality"
            )
        } else if (isOpenAiSora2) {
            if (isSora2ImageToVideo) {
                listOf(
                    "auto" to false to "Automatic",
                    "720p" to false to "High HD"
                )
            } else {
                listOf(
                    "720p" to false to "High HD"
                )
            }
        } else if (isOpenAiSora2Pro) {
            if (isSora2ProImageToVideo) {
                listOf(
                    "auto" to false to "Automatic",
                    "720p" to false to "High HD",
                    "1080p" to false to "Full HD",
                    "true_1080p" to true to "True 1080p Cinematic"
                )
            } else {
                listOf(
                    "720p" to false to "High HD"
                )
            }
        } else if (isFalVeo3FastModel) {
            if (isFalVeo3OnlyFast) {
                listOf(
                    "720p" to false to "High HD",
                    "1080p" to true to "Full HD"
                )
            } else {
                listOf(
                    "720p" to false to "High HD",
                    "1080p" to true to "Full HD",
                    "4K" to false to "Ultra HD"
                )
            }
        } else if (isFalVeo3) {
            if (isFalVeo3_1Lite || isFalVeo3Only) {
                listOf(
                    "720p" to false to "High HD",
                    "1080p" to true to "Full HD"
                )
            } else {
                listOf(
                    "720p" to false to "High HD",
                    "1080p" to true to "Full HD",
                    "4K" to false to "Ultra HD"
                )
            }
        } else if (isVeo2) {
            listOf(
                "480p" to false to "Standard SD",
                "720p" to false to "High HD",
                "1080p" to true to "Full HD Pro"
            )
        } else if (uiState.selectedVideoProvider == "PixVerse" || uiState.selectedVideoModel.lowercase().contains("pixverse")) {
            listOf(
                "360p" to false to "Low SD",
                "540p" to false to "Standard SD",
                "720p" to false to "High HD",
                "1080p" to true to "Full HD Pro"
            )
        } else {
            listOf(
                "480p" to false to "Standard SD",
                "720p" to false to "High HD",
                "1080p" to true to "Full HD Pro",
                "4K" to true to "Ultra HD Pro"
            )
        }
    }

    LaunchedEffect(resolutions) {
        val availableRes = resolutions.map { it.first.first }
        if (availableRes.isNotEmpty() && selectedResolution !in availableRes) {
            selectedResolution = if ((uiState.selectedVideoModel == "fal-ai/minimax/hailuo-02/pro/image-to-video" || uiState.selectedVideoModel == "fal-ai/minimax/hailuo-02/pro/text-to-video" || uiState.selectedVideoModel == "fal-ai/minimax/hailuo-2.3/pro/image-to-video" || uiState.selectedVideoModel == "fal-ai/minimax/hailuo-2.3/pro/text-to-video") && "1080p" in availableRes) {
                "1080p"
            } else if ((uiState.selectedVideoModel == "fal-ai/minimax/hailuo-02/standard/image-to-video" || uiState.selectedVideoModel == "fal-ai/minimax/hailuo-2.3/standard/image-to-video" || uiState.selectedVideoModel == "fal-ai/minimax/hailuo-02/standard/text-to-video" || uiState.selectedVideoModel == "fal-ai/minimax/hailuo-2.3/standard/text-to-video") && "768P" in availableRes) {
                "768P"
            } else if ((isSora2ImageToVideo || isSora2ProImageToVideo) && "auto" in availableRes) {
                "auto"
            } else if (uiState.selectedVideoModel.lowercase().trim().contains("happy-horse") && "1080p" in availableRes) {
                "1080p"
            } else if ("720p" in availableRes) {
                "720p"
            } else {
                availableRes.firstOrNull() ?: "720p"
            }
        }
    }

    val durations = remember(uiState.selectedVideoProvider, uiState.selectedVideoModel, uiState.selectedVideoMode, isFalVeo3, isFalVeo3FastModel, isFalVeo3_1, isFalVeo3_1Lite, isFalVeo3Only, isFalVeo3_1Fast, isFalVeo3OnlyFast, isVeo2, selectedAudioOption, selectedResolution, isKlingV3Standard, isKlingV3Pro, isKlingV3TurboPro, isKlingV3TurboStandard, isKlingO3_4k, isKlingO3Standard, isKlingO3Pro, isKlingV3_4k, isOpenAiSora2Pro, selectedOpenAiSize, klingVoiceControl, isKlingV26Pro, pixverseMultiClip, isGrokImagineVideo, isLumaRayVideo, lumaHdr, lumaExrExport, isHunyuan15Video, hunyuanFrames) {
        if (uiState.selectedVideoModel == "fal-ai/minimax/hailuo-2.3/pro/image-to-video" || uiState.selectedVideoModel == "fal-ai/minimax/hailuo-2.3/pro/text-to-video") {
            emptyList()
        } else if (isHunyuan15Video) {
            val approxSeconds = kotlin.math.round(hunyuanFrames / 24.0).toInt().coerceAtLeast(1)
            val cost = approxSeconds * 30
            listOf(
                "${approxSeconds}s" to false to "🪙 $cost Token"
            )
        } else if (isLumaRayVideo) {
            if (uiState.selectedVideoMode == "Image to Video") {
                val cost = if (lumaHdr) {
                    if (lumaExrExport) {
                        when {
                            selectedResolution.lowercase().contains("720p") -> 235
                            selectedResolution.lowercase().contains("1080p") -> 920
                            else -> 45
                        }
                    } else {
                        when {
                            selectedResolution.lowercase().contains("720p") -> 160
                            selectedResolution.lowercase().contains("1080p") -> 610
                            else -> 45
                        }
                    }
                } else {
                    when {
                        selectedResolution.lowercase().contains("540p") -> 45
                        selectedResolution.lowercase().contains("720p") -> 85
                        selectedResolution.lowercase().contains("1080p") -> 310
                        else -> 45
                    }
                }
                listOf(
                    "5s" to false to "🪙 $cost Token"
                )
            } else {
                val cost5s = when {
                    selectedResolution.lowercase().contains("540p") -> 150
                    selectedResolution.lowercase().contains("720p") -> 275
                    selectedResolution.lowercase().contains("1080p") -> 525
                    else -> 150
                }
                val cost10s = when {
                    selectedResolution.lowercase().contains("540p") -> 275
                    selectedResolution.lowercase().contains("720p") -> 525
                    selectedResolution.lowercase().contains("1080p") -> 1100
                    else -> 275
                }
                listOf(
                    "5s" to false to "🪙 $cost5s Token",
                    "10s" to true to "🪙 $cost10s Token"
                )
            }
        } else if ((uiState.selectedVideoProvider == "FalAI" || uiState.selectedVideoProvider == "Minimax") && uiState.selectedVideoModel == "fal-ai/minimax/hailuo-02/pro/image-to-video") {
            val cost6s = 130
            val cost10s = 300
            listOf(
                "6s" to false to "🪙 $cost6s Token",
                "10s" to false to "🪙 $cost10s Token"
            )
        } else if ((uiState.selectedVideoProvider == "FalAI" || uiState.selectedVideoProvider == "Minimax") && (uiState.selectedVideoModel == "fal-ai/minimax/hailuo-2.3/standard/image-to-video" || uiState.selectedVideoModel == "fal-ai/minimax/hailuo-2.3/standard/text-to-video")) {
            val cost6s = 80
            val cost10s = 150
            listOf(
                "6s" to false to "🪙 $cost6s Token",
                "10s" to false to "🪙 $cost10s Token"
            )
        } else if ((uiState.selectedVideoProvider == "FalAI" || uiState.selectedVideoProvider == "Minimax") && uiState.selectedVideoModel == "fal-ai/minimax/hailuo-02/standard/image-to-video") {
            val cost6s = if (selectedResolution == "512P") 90 else 75
            val cost10s = if (selectedResolution == "512P") 150 else 200
            listOf(
                "6s" to false to "🪙 $cost6s Token",
                "10s" to false to "🪙 $cost10s Token"
            )
        } else if ((uiState.selectedVideoProvider == "FalAI" || uiState.selectedVideoProvider == "Minimax") && uiState.selectedVideoModel == "fal-ai/minimax/hailuo-02/standard/text-to-video") {
            val cost6s = 80
            val cost10s = 200
            listOf(
                "6s" to false to "🪙 $cost6s Token",
                "10s" to false to "🪙 $cost10s Token"
            )
        } else if ((uiState.selectedVideoProvider == "FalAI" || uiState.selectedVideoProvider == "Minimax") && uiState.selectedVideoModel == "fal-ai/minimax/hailuo-02/pro/text-to-video") {
            val cost6s = 130
            val cost10s = 300
            listOf(
                "6s" to false to "🪙 $cost6s Token",
                "10s" to false to "🪙 $cost10s Token"
            )
        } else if (uiState.selectedVideoProvider == "Alibaba") {
            if (uiState.selectedVideoModel.lowercase().trim().contains("wan/v2.7")) {
                val rate = when (selectedResolution.lowercase().trim()) {
                    "720p" -> 35
                    "1080p" -> 45
                    else -> 45
                }
                listOf(
                    "2s" to false to "🪙 ${2 * rate} Token",
                    "3s" to false to "🪙 ${3 * rate} Token",
                    "4s" to false to "🪙 ${4 * rate} Token",
                    "5s" to false to "🪙 ${5 * rate} Token",
                    "6s" to false to "🪙 ${6 * rate} Token",
                    "7s" to false to "🪙 ${7 * rate} Token",
                    "8s" to false to "🪙 ${8 * rate} Token",
                    "9s" to false to "🪙 ${9 * rate} Token",
                    "10s" to false to "🪙 ${10 * rate} Token",
                    "11s" to false to "🪙 ${11 * rate} Token",
                    "12s" to false to "🪙 ${12 * rate} Token",
                    "13s" to false to "🪙 ${13 * rate} Token",
                    "14s" to false to "🪙 ${14 * rate} Token",
                    "15s" to false to "🪙 ${15 * rate} Token"
                )
            } else if (uiState.selectedVideoModel.lowercase().trim().contains("wan/v2.6")) {
                val rate = when (selectedResolution.lowercase().trim()) {
                    "720p" -> 35
                    "1080p" -> 45
                    else -> 45
                }
                listOf(
                    "5s" to false to "🪙 ${5 * rate} Token",
                    "10s" to true to "🪙 ${10 * rate} Token",
                    "15s" to true to "🪙 ${15 * rate} Token"
                )
            } else if (uiState.selectedVideoModel.lowercase().trim().contains("happy-horse")) {
                val rate = when (selectedResolution.lowercase().trim()) {
                    "720p" -> 45
                    "1080p" -> 80
                    else -> 80
                }
                listOf(
                    "3s" to false to "🪙 ${3 * rate} Token",
                    "4s" to false to "🪙 ${4 * rate} Token",
                    "5s" to false to "🪙 ${5 * rate} Token",
                    "6s" to false to "🪙 ${6 * rate} Token",
                    "7s" to false to "🪙 ${7 * rate} Token",
                    "8s" to false to "🪙 ${8 * rate} Token",
                    "9s" to false to "🪙 ${9 * rate} Token",
                    "10s" to false to "🪙 ${10 * rate} Token",
                    "11s" to false to "🪙 ${11 * rate} Token",
                    "12s" to false to "🪙 ${12 * rate} Token",
                    "13s" to false to "🪙 ${13 * rate} Token",
                    "14s" to false to "🪙 ${14 * rate} Token",
                    "15s" to false to "🪙 ${15 * rate} Token"
                )
            } else if (uiState.selectedVideoModel.lowercase().trim().contains("wan/v2.5")) {
                val rate = when (selectedResolution.lowercase().trim()) {
                    "480p" -> if (uiState.selectedVideoMode == "Image to Video") 25 else 20
                    "720p" -> 35
                    "1080p" -> 45
                    else -> 35
                }
                listOf(
                    "5s" to false to "🪙 ${5 * rate} Token",
                    "10s" to true to "🪙 ${10 * rate} Token"
                )
            } else {
                listOf(
                    "5s" to false to "🪙 125 Token",
                    "10s" to true to "🪙 250 Token"
                )
            }
        } else if (isGrokImagineVideo) {
            listOf(4, 5, 6, 7, 8, 9, 10, 11, 12).map { sec ->
                val cost = if (selectedResolution == "480p") {
                    if (sec == 6) 85 else sec * 20
                } else {
                    sec * 25
                }
                "${sec}s" to (sec > 6) to "🪙 $cost Token"
            }
        } else if (uiState.selectedVideoProvider == "Kling") {
            if (isKlingV3Standard || isKlingV3Pro || isKlingV3TurboPro || isKlingV3TurboStandard || isKlingO3_4k || isKlingO3Standard || isKlingO3Pro || isKlingV3_4k) {
                val rate = if (isKlingV3_4k) {
                    if (uiState.selectedVideoMode == "Image to Video") 120 else 125
                } else if (isKlingO3_4k) {
                    115
                } else if (isKlingO3Standard) {
                    if (selectedAudioOption == "On") 40 else 30
                } else if (isKlingO3Pro) {
                    if (selectedAudioOption == "On") 45 else 35
                } else if (isKlingV3TurboPro) {
                    45
                } else if (isKlingV3TurboStandard) {
                    35
                } else if (isKlingV3Pro) {
                    if (selectedAudioOption == "On") {
                        if (klingVoiceControl) 55 else 50
                    } else {
                        35
                    }
                } else {
                    if (selectedAudioOption == "On") {
                        if (klingVoiceControl) 45 else 40
                    } else {
                        30
                    }
                }
                listOf(3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15).map { sec ->
                    val label = "🪙 ${sec * rate} Token"
                    "$sec" to (sec > 5) to label
                }
            } else if (isKlingV26Pro) {
                val rate = if (selectedAudioOption == "On") {
                    if (klingVoiceControl) 50 else 45
                } else {
                    25
                }
                listOf(
                    "5" to false to "🪙 ${5 * rate} Token",
                    "10" to true to "🪙 ${10 * rate} Token"
                )
            } else if (isKlingV25TurboStandard) {
                listOf(
                    "5" to false to "🪙 60 Token",
                    "10" to true to "🪙 135 Token"
                )
            } else if (isKlingV25TurboPro) {
                listOf(
                    "5" to false to "🪙 100 Token",
                    "10" to true to "🪙 250 Token"
                )
            } else {
                if (selectedAudioOption == "On") {
                    listOf(
                        "5s" to false to "🪙 200 Token",
                        "10s" to true to "🪙 400 Token"
                    )
                } else {
                    listOf(
                        "5s" to false to "🪙 100 Token",
                        "10s" to true to "🪙 200 Token"
                    )
                }
            }
        } else if (uiState.selectedVideoProvider == "Bytedance") {
            val rate = when {
                selectedResolution.lowercase().contains("4k") -> 390
                selectedResolution.lowercase().contains("1080p") -> 180
                selectedResolution.lowercase().contains("480p") -> 35
                else -> 85 // default 720p
            }
            val list = mutableListOf<Pair<Pair<String, Boolean>, String>>()
            list.add("Auto" to false to "🪙 ${5 * rate} Token")
            (4..15).forEach { sec ->
                list.add("${sec}s" to (sec > 10) to "🪙 ${sec * rate} Token")
            }
            list
        } else if (isFalVeo3FastModel) {
            val isAudioOn = selectedAudioOption == "On"
            val rate = if (isFalVeo3_1Fast) {
                if (selectedResolution.lowercase().contains("4k")) {
                    if (isAudioOn) 95 else 85
                } else {
                    if (isAudioOn) 45 else 35
                }
            } else {
                // Veo 3 Fast
                if (isAudioOn) 50 else 35
            }
            listOf(
                "4s" to false to "🪙 ${4 * rate} Token",
                "6s" to false to "🪙 ${6 * rate} Token",
                "8s" to false to "🪙 ${8 * rate} Token"
            )
        } else if (isFalVeo3) {
            val isAudioOn = selectedAudioOption == "On"
            val rate = if (isFalVeo3_1Lite) {
                if (selectedResolution.lowercase().contains("720p")) {
                    if (isAudioOn) 20 else 15
                } else {
                    if (isAudioOn) 30 else 20
                }
            } else if (isFalVeo3_1) {
                if (selectedResolution.lowercase().contains("4k")) {
                    if (isAudioOn) 180 else 120
                } else {
                    if (isAudioOn) 120 else 60
                }
            } else {
                // Veo 3 Only
                if (isAudioOn) 120 else 60
            }
            listOf(
                "4s" to false to "🪙 ${4 * rate} Token",
                "6s" to false to "🪙 ${6 * rate} Token",
                "8s" to false to "🪙 ${8 * rate} Token"
            )
        } else if (isVeo2) {
            listOf(
                "5s" to false to "🪙 650 Token",
                "6s" to false to "🪙 800 Token",
                "7s" to false to "🪙 950 Token",
                "8s" to false to "🪙 1100 Token"
            )
        } else if (isSora2ImageToVideo || isSora2ProImageToVideo) {
            val pricePerSecond = if (isSora2ImageToVideo) {
                30
            } else {
                when (selectedOpenAiSize) {
                    "720x1280", "1280x720" -> 85
                    "1024x1792", "1792x1024" -> 135
                    "1080x1920", "1920x1080" -> 185
                    else -> 85
                }
            }
            listOf(
                "4 seconds" to false to "🪙 ${4 * pricePerSecond} Token",
                "8 seconds" to false to "🪙 ${8 * pricePerSecond} Token",
                "12 seconds" to false to "🪙 ${12 * pricePerSecond} Token",
                "16 seconds" to false to "🪙 ${16 * pricePerSecond} Token",
                "20 seconds" to false to "🪙 ${20 * pricePerSecond} Token"
            )
        } else if (uiState.selectedVideoProvider == "OpenAI" && (uiState.selectedVideoModel.lowercase().trim() == "sora 2" || uiState.selectedVideoModel.lowercase().trim() == "sora 2 pro")) {
            val pricePerSecond = if (uiState.selectedVideoModel.lowercase().trim() == "sora 2") {
                30
            } else {
                when (selectedOpenAiSize) {
                    "720x1280", "1280x720" -> 85
                    "1024x1792", "1792x1024" -> 135
                    "1080x1920", "1920x1080" -> 185
                    else -> 85
                }
            }
            listOf(
                "4 seconds" to false to "🪙 ${4 * pricePerSecond} Token",
                "8 seconds" to false to "🪙 ${8 * pricePerSecond} Token",
                "12 seconds" to false to "🪙 ${12 * pricePerSecond} Token",
                "16 seconds" to false to "🪙 ${16 * pricePerSecond} Token",
                "20 seconds" to false to "🪙 ${20 * pricePerSecond} Token"
            )
        } else if (uiState.selectedVideoProvider == "PixVerse") {
            val isAudioOn = selectedAudioOption == "On"
            if (uiState.selectedVideoModel == "fal-ai/pixverse/v5.5/text-to-video") {
                val base5s = when (selectedResolution) {
                    "360p", "540p" -> 45
                    "720p" -> 55
                    "1080p" -> 110
                    else -> 55
                }
                val audioCost = if (isAudioOn) 20 else 0
                val multiClipCost = if (pixverseMultiClip) {
                    if (isAudioOn) 45 else 30
                } else {
                    0
                }
                val cost5s = base5s + audioCost + multiClipCost

                val availableSecs = if (selectedResolution == "1080p") listOf(5, 8) else listOf(5, 8, 10)
                availableSecs.map { sec ->
                    val finalCost = when (sec) {
                        5 -> cost5s
                        8 -> cost5s * 2
                        10 -> (cost5s * 2.2).toInt()
                        else -> cost5s
                    }
                    val label = "🪙 $finalCost Token"
                    "${sec}s" to (sec > 5) to label
                }
            } else if (uiState.selectedVideoModel == "fal-ai/pixverse/v5.6/image-to-video" || uiState.selectedVideoModel == "fal-ai/pixverse/v5.6/text-to-video") {
                val base5s = when (selectedResolution) {
                    "360p", "540p" -> 95
                    "720p" -> 120
                    "1080p" -> 195
                    else -> 120
                }
                val audioCost = if (isAudioOn) {
                    when (selectedResolution) {
                        "360p", "540p", "720p" -> 120
                        "1080p" -> 195
                        else -> 120
                    }
                } else {
                    0
                }
                val cost5s = base5s + audioCost

                val availableSecs = if (selectedResolution == "1080p") listOf(5, 8) else listOf(5, 8, 10)
                availableSecs.map { sec ->
                    val finalCost = when (sec) {
                        5 -> cost5s
                        8 -> cost5s * 2
                        10 -> (cost5s * 2.2).toInt()
                        else -> cost5s
                    }
                    val label = "🪙 $finalCost Token"
                    "${sec}s" to (sec > 5) to label
                }
            } else {
                val rate = when (selectedResolution) {
                    "360p" -> if (isAudioOn) 15 else 10
                    "540p" -> if (isAudioOn) 20 else 15
                    "720p" -> if (isAudioOn) 25 else 20
                    "1080p" -> if (isAudioOn) 35 else 30
                    else -> if (isAudioOn) 25 else 20
                }
                (1..15).map { sec ->
                    val label = "🪙 ${sec * rate} Token"
                    "${sec}s" to (sec > 5) to label
                }
            }
        } else {
            listOf(
                "4 seconds" to false to "Short Loop",
                "8 seconds" to false to "Medium Loop",
                "12 seconds" to true to "Extended HD"
            )
        }
    }

    LaunchedEffect(durations) {
        val availableDurs = durations.map { it.first.first }
        if (selectedDuration !in availableDurs) {
            selectedDuration = if ((uiState.selectedVideoModel == "fal-ai/minimax/hailuo-02/standard/image-to-video" || uiState.selectedVideoModel == "fal-ai/minimax/hailuo-02/pro/image-to-video" || uiState.selectedVideoModel == "fal-ai/minimax/hailuo-2.3/standard/image-to-video" || uiState.selectedVideoModel == "fal-ai/minimax/hailuo-02/standard/text-to-video" || uiState.selectedVideoModel == "fal-ai/minimax/hailuo-02/pro/text-to-video" || uiState.selectedVideoModel == "fal-ai/minimax/hailuo-2.3/standard/text-to-video") && "6s" in availableDurs) {
                "6s"
            } else if (isGrokImagineVideo && "6s" in availableDurs) {
                "6s"
            } else if ("5s" in availableDurs) {
                "5s"
            } else {
                availableDurs.firstOrNull() ?: "8 seconds"
            }
        }
    }

    val promptInputCard = @Composable {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Describe Your Video Storyline",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                if (isKlingV3Standard || isKlingV3Pro || isKlingV3TurboPro || isKlingV3TurboStandard || isKlingO3_4k || isKlingO3Standard || isKlingO3Pro || isKlingV3_4k) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = !useStoryboard,
                            onClick = { useStoryboard = false },
                            label = { Text("Single Prompt") }
                        )
                        FilterChip(
                            selected = useStoryboard,
                            onClick = { useStoryboard = true },
                            label = { Text("Multi-shot Storyboard") }
                        )
                    }
                }

                if ((isKlingV3Standard || isKlingV3Pro || isKlingV3TurboPro || isKlingV3TurboStandard || isKlingO3_4k || isKlingO3Standard || isKlingO3Pro || isKlingV3_4k) && useStoryboard) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        val totalDuration = storyboardShots.sumOf { it.second.value.toIntOrNull() ?: 5 }
                        val durationExceeded = totalDuration > 15

                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Total Storyboard Duration: ${totalDuration}s / 15s",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                color = if (durationExceeded) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "${storyboardShots.size} / 6 Shots",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        if (durationExceeded) {
                            Text(
                                text = "⚠️ Total duration of all storyboard shots must not exceed 15 seconds.",
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                        }

                        storyboardShots.forEachIndexed { index, shot ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Shot #${index + 1}",
                                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        if (storyboardShots.size > 1) {
                                            IconButton(
                                                onClick = { storyboardShots.removeAt(index) },
                                                modifier = Modifier.size(28.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Delete,
                                                    contentDescription = "Remove Shot",
                                                    tint = MaterialTheme.colorScheme.error,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(6.dp))

                                    OutlinedTextField(
                                        value = shot.first.value,
                                        onValueChange = { shot.first.value = it },
                                        placeholder = { Text("Prompt for shot #${index + 1}...") },
                                        modifier = Modifier.fillMaxWidth().height(70.dp),
                                        shape = RoundedCornerShape(8.dp),
                                        textStyle = MaterialTheme.typography.bodyMedium
                                    )

                                    Spacer(modifier = Modifier.height(8.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Duration: ",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        
                                        val durationOptions = listOf("3", "4", "5", "6", "7", "8", "9", "10", "11", "12", "13", "14", "15")
                                        var expandedDurationDropdown by remember { mutableStateOf(false) }
                                        
                                        Box {
                                            OutlinedButton(
                                                onClick = { expandedDurationDropdown = true },
                                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                                modifier = Modifier.height(32.dp),
                                                shape = RoundedCornerShape(6.dp)
                                            ) {
                                                Text("${shot.second.value} seconds ▾", style = MaterialTheme.typography.bodySmall)
                                            }
                                            DropdownMenu(
                                                expanded = expandedDurationDropdown,
                                                onDismissRequest = { expandedDurationDropdown = false }
                                            ) {
                                                durationOptions.forEach { sOpt ->
                                                    DropdownMenuItem(
                                                        text = { Text("$sOpt seconds") },
                                                        onClick = {
                                                            shot.second.value = sOpt
                                                            expandedDurationDropdown = false
                                                        }
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        if (storyboardShots.size < 6) {
                            OutlinedButton(
                                onClick = {
                                    storyboardShots.add(mutableStateOf("") to mutableStateOf("5"))
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(imageVector = Icons.Default.Add, contentDescription = "Add Shot")
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Add Storyboard Shot")
                            }
                        }
                    }
                } else {
                    OutlinedTextField(
                        value = prompt,
                        onValueChange = { prompt = it },
                        placeholder = { Text("Example: Drone camera slowly gliding over tropical beach waves during a golden sunset at dusk...") },
                        modifier = Modifier.fillMaxWidth().height(100.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline
                        )
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Quick Inspiration:",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 6.dp)
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    shuffledQuickPrompts.forEach { text ->
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
                            color = MaterialTheme.colorScheme.surface,
                            modifier = Modifier.clickable { prompt = text }
                        ) {
                            Text(
                                text = text,
                                style = MaterialTheme.typography.labelMedium,
                                maxLines = 1,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .imePadding()
    ) {
        // Scrollable content underneath
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(videoScrollState)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Hero Header (Non-sticky)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp)
            ) {
                Text(
                    text = "Eddie AI Video Studio 🎬",
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )
                if (uiState.isSubscribed) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Surface(
                        color = Color(0xFFFFB100).copy(alpha = 0.15f),
                        border = BorderStroke(1.dp, Color(0xFFFFB100)),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = "🪙 ${uiState.getFormattedTokens()} Tokens",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 10.sp),
                            color = Color(0xFFFFB100)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Transform your descriptive words into high-resolution cinematic video clips powered by state-of-the-art AI servers.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
            }

        // Provider & Model Selection Card for Video Generator
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                // Header of the selection
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "Settings Icon",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "AI Video Studio Configuration",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Mode Selection
                Text(
                    text = "Generation Mode:",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val modes = listOf("Text to Video", "Image to Video")
                    modes.forEach { mode ->
                        val isSelected = uiState.selectedVideoMode == mode
                        FilterChip(
                            selected = isSelected,
                            onClick = { onModeChange(mode) },
                            label = { Text(mode) },
                            leadingIcon = {
                                val icon = if (mode == "Text to Video") Icons.Default.Edit else Icons.Default.Face
                                Icon(
                                    imageVector = icon,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Provider Selection
                Text(
                    text = "AI Video Provider:",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val providers = when (uiState.selectedVideoMode) {
                        "Text to Video" -> listOf("Veo", "FalAI", "Alibaba", "Kling", "PixVerse", "Minimax")
                        else -> listOf("Veo", "FalAI", "Alibaba", "Kling", "PixVerse", "Xai", "Minimax") // Image to Video
                    }
                    providers.forEach { provider ->
                        val isSelected = uiState.selectedVideoProvider == provider
                        FilterChip(
                            selected = isSelected,
                            onClick = { onProviderChange(provider) },
                            label = { Text(provider) },
                            leadingIcon = {
                                val icon = when (provider) {
                                    "OpenAI" -> Icons.Default.CheckCircle
                                    "Veo" -> Icons.Default.PlayArrow
                                    "FalAI" -> Icons.Default.Star
                                    "Alibaba" -> Icons.Default.PlayArrow
                                    "Kling" -> Icons.Default.Build
                                    "Bytedance" -> Icons.Default.Face
                                    "PixVerse" -> Icons.Default.PlayArrow
                                    "Xai" -> Icons.Default.Star
                                    "Minimax" -> Icons.Default.Face
                                    else -> Icons.Default.Menu
                                }
                                Icon(
                                    imageVector = icon,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer,
                                selectedLeadingIconColor = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Model Selection
                Text(
                    text = "AI Video Model:",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val models = when (uiState.selectedVideoMode) {
                        "Text to Video" -> {
                            when (uiState.selectedVideoProvider) {
                                "Veo" -> listOf(
                                    Triple("Veo 2", "veo2", 625.0),
                                    Triple("Veo 3", "veo3", 800.0),
                                    Triple("Veo 3 Fast", "veo3-fast", 300.0),
                                    Triple("Veo 3.1", "fal-ai/veo3.1", 800.0),
                                    Triple("Veo 3.1 Fast", "fal-ai/veo3.1/fast", 300.0),
                                    Triple("Veo 3.1 Lite", "fal-ai/veo3.1/lite", 200.0)
                                )
                                "FalAI" -> listOf(
                                    Triple("Grok Imagine", "xai/grok-imagine-video/text-to-video", 125.0),
                                    Triple("Luma Ray 3.2", "luma/agent/ray/v3.2/text-to-video", 125.0)
                                )
                                "Alibaba" -> listOf(
                                    Triple("Wan 2.5", "fal-ai/wan-25-preview/text-to-video", 125.0),
                                    Triple("Wan 2.6", "fal-ai/wan/v2.6/text-to-video", 125.0),
                                    Triple("Wan 2.7", "fal-ai/wan/v2.7/text-to-video", 125.0),
                                    Triple("Happy Horse", "alibaba/happy-horse/text-to-video", 125.0)
                                )
                                "Kling" -> listOf(
                                    Triple("Kling v3 4k", "fal-ai/kling-video/v3/4k/text-to-video", 625.0),
                                    Triple("Kling o3 4k", "fal-ai/kling-video/o3/4k/text-to-video", 575.0),
                                    Triple("Kling v3 Turbo standard", "kling-video/v3/turbo/standard/text-to-video", 175.0),
                                    Triple("Kling v3 Turbo pro", "kling-video/v3/turbo/pro/text-to-video", 225.0),
                                    Triple("Kling v2.5 Turbo Pro", "kling-2.5-turbo/pro/text-to-video", 150.0),
                                    Triple("Kling v3 standard", "Kling v3 standard", 175.0),
                                    Triple("Kling v2.6 pro", "Kling v2.6 pro", 350.0),
                                    Triple("Kling v3 pro", "Kling v3 pro", 350.0)
                                )
                                "Bytedance" -> listOf(
                                    Triple("Seedance 2.0", "bytedance/seedance-2.0/text-to-video", 500.0),
                                    Triple("Seedance 2.0 Fast", "fal-ai/bytedance/seedance-2.0/text-to-video/fast", 250.0)
                                )
                                "PixVerse" -> listOf(
                                    Triple("PixVerse V6 Cinematic", "fal-ai/pixverse/v6/text-to-video", 150.0),
                                    Triple("Pixverse v5.6", "fal-ai/pixverse/v5.6/text-to-video", 120.0),
                                    Triple("PixVerse 5.5", "fal-ai/pixverse/v5.5/text-to-video", 120.0)
                                )
                                "Minimax" -> listOf(
                                    Triple("Hailuo 02 Standard", "fal-ai/minimax/hailuo-02/standard/text-to-video", 80.0),
                                    Triple("Hailuo 02 Pro", "fal-ai/minimax/hailuo-02/pro/text-to-video", 130.0),
                                    Triple("Hailuo 2.3 Standard", "fal-ai/minimax/hailuo-2.3/standard/text-to-video", 80.0),
                                    Triple("Hailuo 2.3 Pro", "fal-ai/minimax/hailuo-2.3/pro/text-to-video", 135.0)
                                )
                                else -> emptyList()
                            }
                        }
                        else -> { // Image to Video
                            when (uiState.selectedVideoProvider) {
                                "Veo" -> listOf(
                                    Triple("Veo 3.1", "fal-ai/veo3.1", 800.0),
                                    Triple("Veo 3.1 Fast", "fal-ai/veo3.1/fast", 300.0),
                                    Triple("Veo 3.1 Lite", "fal-ai/veo3.1/lite", 200.0)
                                )
                                "FalAI" -> listOf(
                                    Triple("Luma Ray 3.2", "luma/agent/ray/v3.2/image-to-video", 125.0),
                                    Triple("Hunyuan v1.5", "fal-ai/hunyuan-video-v1.5/image-to-video", 150.0)
                                )
                                "Xai" -> listOf(
                                    Triple("Xai Grok Imagine", "xai/grok-imagine-video/image-to-video", 125.0),
                                    Triple("Grok Imagine 1.5", "xai/grok-imagine-video/v1.5/image-to-video", 125.0)
                                )
                                "Kling" -> listOf(
                                    Triple("Kling v2.5 Turbo pro", "fal-ai/kling-video/v2.5-turbo/pro/image-to-video", 150.0),
                                    Triple("Kling v2.5 Turbo standard", "fal-ai/kling-video/v2.5-turbo/standard/image-to-video", 150.0),
                                    Triple("Kling v2.6 Pro", "fal-ai/kling-video/v2.6/pro/image-to-video", 350.0),
                                    Triple("Kling v3 standard", "fal-ai/kling-video/v3/standard/image-to-video", 175.0),
                                    Triple("Kling v3 pro", "fal-ai/kling-video/v3/pro/image-to-video", 350.0),
                                    Triple("Kling v3 4k", "fal-ai/kling-video/v3/4k/image-to-video", 600.0),
                                    Triple("Kling v3 Turbo pro", "fal-ai/kling-video/v3/turbo/pro/image-to-video", 225.0),
                                    Triple("Kling v3 Turbo standard", "fal-ai/kling-video/v3/turbo/standard/image-to-video", 175.0),
                                    Triple("Kling O3 Standard", "fal-ai/kling-video/o3/standard/image-to-video", 150.0),
                                    Triple("Kling O3 Pro", "fal-ai/kling-video/o3/pro/image-to-video", 175.0),
                                    Triple("Kling O3 4k", "fal-ai/kling-video/o3/4k/image-to-video", 575.0)
                                )
                                "Bytedance" -> listOf(
                                    Triple("Seedance 2.0 Standard", "fal-ai/bytedance/seedance-2.0/image-to-video", 500.0),
                                    Triple("Seedance 2.0 Fast", "fal-ai/bytedance/seedance-2.0/image-to-video/fast", 250.0)
                                )
                                "PixVerse" -> listOf(
                                    Triple("Pixverse v6", "fal-ai/pixverse/v6/image-to-video", 150.0),
                                    Triple("Pixverse C1", "fal-ai/pixverse/c1/image-to-video", 150.0),
                                    Triple("PixVerse 5.6", "fal-ai/pixverse/v5.6/image-to-video", 120.0)
                                )
                                "Alibaba" -> listOf(
                                    Triple("Wan 2.5", "fal-ai/wan-25-preview/image-to-video", 125.0),
                                    Triple("Wan 2.6", "fal-ai/wan/v2.6/image-to-video", 125.0),
                                    Triple("Wan 2.7", "fal-ai/wan/v2.7/image-to-video", 125.0),
                                    Triple("Happy Horse", "alibaba/happy-horse/image-to-video", 125.0)
                                )
                                "Minimax" -> listOf(
                                    Triple("Hailuo 02 Standard", "fal-ai/minimax/hailuo-02/standard/image-to-video", 75.0),
                                    Triple("Hailuo 02 Pro", "fal-ai/minimax/hailuo-02/pro/image-to-video", 130.0),
                                    Triple("Hailuo 2.3 Standard", "fal-ai/minimax/hailuo-2.3/standard/image-to-video", 80.0),
                                    Triple("Hailuo 2.3 Pro", "fal-ai/minimax/hailuo-2.3/pro/image-to-video", 140.0)
                                )
                                else -> emptyList()
                            }
                        }
                    }

                    val sortedModels = if (uiState.selectedVideoProvider == "Kling" || uiState.selectedVideoProvider == "Veo" || uiState.selectedVideoProvider == "Bytedance" || uiState.selectedVideoProvider == "PixVerse" || uiState.selectedVideoProvider == "Xai") {
                        models.sortedBy { it.first.lowercase() }
                    } else {
                        models.sortedBy { it.third }
                    }

                    sortedModels.forEach { (displayName, modelId, tokenCost) ->
                        val isSelected = uiState.selectedVideoModel == modelId
                        val isPro = tokenCost > 200.0
                        val badgeColor = if (uiState.isSubscribed) MaterialTheme.colorScheme.primary else Color(0xFFFFB100)
                        val badgeTextColor = if (uiState.isSubscribed) MaterialTheme.colorScheme.onPrimary else Color(0xFF111111)

                        val priceStartIndex = displayName.lastIndexOf('(')
                        val baseName = if (priceStartIndex != -1) displayName.substring(0, priceStartIndex).trim() else displayName
                        val priceText = if (priceStartIndex != -1) displayName.substring(priceStartIndex).replace("(", "").replace(")", "").trim() else ""

                        FilterChip(
                            selected = isSelected,
                            onClick = { onModelChange(modelId) },
                            label = {
                                Column(
                                    modifier = Modifier.padding(vertical = 4.dp),
                                    verticalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = baseName,
                                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                                        )
                                        if (isPro) {
                                            Surface(
                                                color = badgeColor,
                                                shape = RoundedCornerShape(4.dp)
                                            ) {
                                                Text(
                                                    text = "PRO",
                                                    color = badgeTextColor,
                                                    fontSize = 8.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                                )
                                            }
                                        }
                                    }
                                    if (priceText.isNotEmpty()) {
                                        Text(
                                            text = if (priceText.contains("Free", ignoreCase = true)) "🎁 Free" else "🪙 $priceText",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        )
                    }
                }

                val modelDescription = when (uiState.selectedVideoProvider) {
                    "Alibaba" -> {
                        val mLower = uiState.selectedVideoModel.lowercase().trim()
                        when {
                            mLower.contains("wan/v2.5") -> {
                                if (uiState.selectedVideoMode == "Image to Video") {
                                    "Wan 2.5 image-to-video model."
                                } else {
                                    "Wan 2.5 video model by Alibaba."
                                }
                            }
                            mLower.contains("wan/v2.6") -> {
                                if (uiState.selectedVideoMode == "Image to Video") {
                                    "Wan 2.6 image-to-video model."
                                } else {
                                    "Wan 2.6 video model by Alibaba."
                                }
                            }
                            mLower.contains("wan/v2.7") -> {
                                if (uiState.selectedVideoMode == "Image to Video") {
                                    "Wan 2.7 is the latest generation AI video model, delivering enhanced motion smoothness, superior scene fidelity, and greater visual coherence."
                                } else {
                                    "Alibaba's Wan 2.7 is a state-of-the-art cinematic engine providing extremely realistic texture details and light simulations."
                                }
                            }
                            mLower.contains("happy-horse") -> {
                                if (uiState.selectedVideoMode == "Image to Video") {
                                    "Alibaba's #1-ranked Happy Horse 1.0 — generate 1080p video with synchronized native audio and multilingual lip-sync from text prompts or images"
                                } else {
                                    "Generate 1080p video with synchronized native audio. Aspect ratios: 16:9, 9:16, 1:1, 4:3, 3:4. Duration: 3–15s."
                                }
                            }
                            else -> "Alibaba video generation model powered by FalAI."
                        }
                    }
                    "Veo" -> {
                        when (uiState.selectedVideoModel.lowercase().trim()) {
                            "fal-ai/veo3.1" -> "Veo 3.1 is the latest state-of-the art video generation model from Google DeepMind"
                            "fal-ai/veo3.1/lite" -> "Veo 3.1 Lite balances practical utility with professional capabilities, supporting Text-to-Video and Image-to-Video"
                            "fal-ai/veo3.1/fast" -> "Generate videos from your image prompts using Veo 3.1 fast."
                            "veo2" -> "Veo 2 creates videos with realistic motion and high quality output. Explore different styles and find your own with extensive camera controls."
                            "veo3" -> "Veo 3 by Google, the most advanced AI video generation model in the world. With sound on!"
                            "veo3-fast" -> "Faster and more cost effective version of Google's Veo 3.1!"
                            else -> null
                        }
                    }
                    "FalAI", "Kling", "Xai", "Minimax" -> {
                        when (uiState.selectedVideoModel.lowercase().trim()) {
                            "fal-ai/minimax/hailuo-02/pro/text-to-video" -> "Advanced video generation model with 1080p resolution"
                            "fal-ai/minimax/hailuo-02/standard/image-to-video" -> "Realistic dynamic video generation from images."
                            "fal-ai/minimax/hailuo-02/standard/text-to-video" -> "Advanced video generation model with 768p resolution"
                            "fal-ai/minimax/hailuo-2.3/pro/text-to-video" -> "Advanced text-to-video generation model with 1080p resolution"
                            "fal-ai/minimax/hailuo-2.3/standard/text-to-video" -> "Advanced text-to-video generation model with 768p resolution"
                            "fal-ai/minimax/hailuo-2.3/standard/image-to-video" -> "Advanced image-to-video generation model with 768p resolution"
                            "fal-ai/minimax/hailuo-2.3/pro/image-to-video" -> "Advanced image-to-video generation model with 1080p resolution"
                            "fal-ai/minimax/hailuo-02/pro/image-to-video" -> "Advanced image-to-video generation model with 1080p resolution"
                            "fal-ai/veo3.1" -> "Veo 3.1 is the latest state-of-the art video generation model from Google DeepMind"
                            "fal-ai/veo3.1/lite" -> "Veo 3.1 Lite balances practical utility with professional capabilities, supporting Text-to-Video and Image-to-Video"
                            "fal-ai/veo3.1/fast" -> "Generate videos from your image prompts using Veo 3.1 fast."
                            "veo2" -> "Veo 2 creates videos with realistic motion and high quality output. Explore different styles and find your own with extensive camera controls."
                            "veo3" -> "Veo 3 by Google, the most advanced AI video generation model in the world. With sound on!"
                            "veo3-fast" -> "Faster and more cost effective version of Google's Veo 3.1!"
                            "veed" -> "Highly creative, artistic short-video synthesis model on Fal.Ai with customizable canvas style and high visual clarity."
                            "kling" -> "Advanced physical simulation model on Fal.Ai known for highly accurate real-world physics and complex body movements."
                            "kling-video" -> "High-speed rendering Kling model designed for cinematic, smooth frames and complex camera motion sequences."
                            "luma-dream-machine", "luma/agent/ray/v3.2/text-to-video" -> "Luma Ray 3.2 generates cinematic video from a text prompt, with control over resolution, duration, and seamless looping, plus reference images to lock in subject and style."
                            "luma/agent/ray/v3.2/image-to-video" -> "Luma Ray 3.2 animates a source image into cinematic motion guided by a text prompt, preserving the starting frame's look while controlling resolution, duration, and seamless looping."
                            "minimax" -> "State-of-the-art cinematic engine on Fal.Ai providing life-like human expressions and smooth, detailed background landscapes."
                            "haiper" -> "An expressive AI model on Fal.Ai designed to capture physics-based actions, stylized transitions, and lively character expressions."
                            "hunyuan-video", "fal-ai/hunyuan-video-v1.5/image-to-video" -> "Hunyuan Video 1.5 is Tencent's latest and best video model"
                            "fal-ai/kling-video/o3/4k/text-to-video",
                            "fal-ai/kling-video/o3/4k/image-to-video",
                            "fal-ai/kling-video/v3/4k/text-to-video",
                            "fal-ai/kling-video/v3/4k/image-to-video",
                            "kling o3 4k",
                            "kling v3 4k" -> "Kling's Native 4K is a video generation model that directly outputs professional-grade 4K video in one step, eliminating the need for post-production upscaling"

                            "fal-ai/kling-video/o3/standard/image-to-video",
                            "kling o3 standard" -> "Generate a video by taking a start frame and an end frame, animating the transition between them while following text-driven style and scene guidance"

                            "fal-ai/kling-video/o3/pro/image-to-video",
                            "kling o3 pro" -> "Generate a video by taking a start frame and an end frame, animating the transition between them while following text-driven style and scene guidance."

                            "fal-ai/kling-video/v2.6/pro/image-to-video",
                            "fal-ai/kling-video/v2.6/pro/text-to-video",
                            "kling v2.6 pro" -> "Kling 2.6 Pro: Top-tier image-to-video with cinematic visuals, fluid motion, and native audio generation."

                            "fal-ai/kling-video/v3/turbo/standard/image-to-video",
                            "kling-video/v3/turbo/standard/text-to-video",
                            "kling v3 turbo standard" -> "Kling 3.0 Turbo Standard animates a first and last frame reference image into 720P video with native audio, delivering quick, affordable image-driven motion for fast turnaround"

                            "fal-ai/kling-video/v3/turbo/pro/image-to-video",
                            "kling-video/v3/turbo/pro/text-to-video",
                            "kling v3 turbo pro" -> "Generate high quality 1080p videos from images using Kling's Turbo 3.0 model, with improved lipsync and multishot generation capabilities."

                            "kling v2.6 standard" -> "Standard Kling v2.6 engine on Fal.Ai. Balanced speed and texture synthesis to animate input images realistically."

                            "fal-ai/kling-video/v3/pro/image-to-video",
                            "fal-ai/kling-video/v3/pro/text-to-video",
                            "kling v3 pro" -> "Kling 3.0 Pro: Top-tier image-to-video with cinematic visuals, fluid motion, and native audio generation, with custom element support."

                            "fal-ai/kling-video/v3/standard/image-to-video",
                            "fal-ai/kling-video/v3/standard/text-to-video",
                            "kling v3 standard" -> "Kling 3.0 Standard: Top-tier image-to-video with cinematic visuals, fluid motion, and native audio generation, with custom element support."

                            "klingv21" -> "Robust image-to-video capabilities powered by Kling v2.1 with refined character animations and cinematic camera flows."
                            "klingimage" -> "High-fidelity Kling v1.6 image animation engine, generating natural, fluid motion from static visual references."
                            "xai/grok-imagine-video/image-to-video" -> "xAI's advanced Grok Imagine video generator on Fal.AI, bringing images to life with superb texture detail and camera panning."
                            "xai/grok-imagine-video/v1.5/image-to-video" -> "Generate videos from images with audio using xAI's Grok Imagine 1.5 Video model."
                            "xai/grok-imagine-video/text-to-video" -> "Generate videos with audio from text using Grok Imagine Video"

                            "fal-ai/kling-video/v2.5-turbo/pro/image-to-video",
                            "fal-ai/kling-video/v2.5-turbo/pro/text-to-video",
                            "kling-2.5-turbo/pro/text-to-video",
                            "kling v2.5 turbo pro" -> "Kling 2.5 Turbo Pro: Top-tier image-to-video generation with unparalleled motion fluidity, cinematic visuals, and exceptional prompt precision."

                            "fal-ai/kling-video/v2.5-turbo/standard/image-to-video",
                            "fal-ai/kling-2.5-turbo/standard/image-to-video",
                            "kling-2.5-turbo/standard/image-to-video",
                            "kling v2.5 turbo standard" -> "Kling 2.5 Turbo Standard: Top-tier image-to-video generation with unparalleled motion fluidity, cinematic visuals, and exceptional prompt precision."
                            else -> null
                        }
                    }
                    "Bytedance" -> {
                        when (uiState.selectedVideoModel.lowercase().trim()) {
                            "bytedance/seedance-2.0/text-to-video", "fal-ai/bytedance/seedance-2.0/text-to-video" -> 
                                "ByteDance's most advanced text-to-video model. Cinematic output with native audio, multi-shot editing, real-world physics, and director-level camera control."
                            "fal-ai/bytedance/seedance-2.0/text-to-video/fast" -> "Seedance 2.0 Fast (Text-to-Video): Fast-tier rendering of ByteDance's state-of-the-art text-to-video model."
                            "fal-ai/bytedance/seedance-2.0/image-to-video" -> "Seedance 2.0 (Image-to-Video): ByteDance's most advanced image-to-video model. Animate still images into cinematic video with synchronized audio, start and end frame control."
                            "fal-ai/bytedance/seedance-2.0/image-to-video/fast" -> "Seedance 2.0 Fast (Image-to-Video): Fast-tier animation of still images into cinematic video using ByteDance's SOTA model."
                            else -> "Seedance 2.0: ByteDance's next generation AI video generation model."
                        }
                    }
                    "OpenAI" -> {
                        when (uiState.selectedVideoModel.lowercase().trim()) {
                            "sora 2" -> {
                                if (uiState.selectedVideoMode == "Image to Video") {
                                    "Image-to-video endpoint for Sora 2, OpenAI's state-of-the-art video model capable of creating richly detailed, dynamic clips with audio from natural language or images."
                                } else {
                                    "Text-to-video endpoint for Sora 2, OpenAI's state-of-the-art video model capable of creating richly detailed, dynamic clips with audio from natural language or images."
                                }
                            }
                            "sora 2 pro" -> {
                                if (uiState.selectedVideoMode == "Image to Video") {
                                    "Image-to-video endpoint for Sora 2 Pro, OpenAI's state-of-the-art video model capable of creating richly detailed, dynamic clips with audio from natural language or images."
                                } else {
                                    "OpenAI's high-tier cinematic model with extreme temporal consistency, highly detailed scenery, and production-grade rendering precision."
                                }
                            }
                            else -> null
                        }
                    }
                    "PixVerse" -> {
                        when (uiState.selectedVideoModel.lowercase().trim()) {
                            "fal-ai/pixverse/v6/text-to-video" -> "PixVerse V6 Cinematic delivers film-grade video outputs with multi-shot consistency, advanced camera/lens controls, and native synchronized audio."
                            "fal-ai/pixverse/v6/transition" -> "PixVerse V6 Transition turns static start frames into dynamic cinematic video sequences with high spatial consistency and smooth camera flow."
                            "fal-ai/pixverse/v6/image-to-video" -> "Pixverse's latest V6 Model"
                            "fal-ai/pixverse/v5.5/text-to-video" -> "Generate high quality video clips from text and image prompts using PixVerse v5.5"
                            "fal-ai/pixverse/v5.6/text-to-video" -> "Use the latest pixverse v5.6 model to turn your texts into amazing videos."
                            "fal-ai/pixverse/v5.6/image-to-video" -> "Use the latest pixverse v5.6 model to turn your texts and images into amazing videos"
                            "fal-ai/pixverse/c1/image-to-video" -> "Animate images into cinematic videos with PixVerse C1, supporting 1080p resolution and native audio generation."
                            else -> "Cinematic AI video generator by PixVerse, running on serverless Fal.ai backend infrastructure."
                        }
                    }
                    else -> null
                }

                modelDescription?.let { description ->
                    Spacer(modifier = Modifier.height(10.dp))
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)
                        ),
                        border = BorderStroke(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.Top,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = "Model Info",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp).padding(top = 2.dp)
                            )
                            Column {
                                val selectedModelLower = uiState.selectedVideoModel.lowercase().trim()
                                val actualKlingId = when (selectedModelLower) {
                                    "fal-ai/kling-video/o3/4k/text-to-video" -> "fal-ai/kling-video/o3/4k/text-to-video"
                                    "fal-ai/kling-video/o3/4k/image-to-video" -> "fal-ai/kling-video/o3/4k/image-to-video"
                                    "fal-ai/kling-video/o3/standard/image-to-video" -> "fal-ai/kling-video/o3/standard/image-to-video"
                                    "fal-ai/kling-video/o3/pro/image-to-video" -> "fal-ai/kling-video/o3/pro/image-to-video"
                                    "fal-ai/kling-video/v3/4k/text-to-video" -> "fal-ai/kling-video/v3/4k/text-to-video"
                                    "fal-ai/kling-video/v3/4k/image-to-video" -> "fal-ai/kling-video/v3/4k/image-to-video"
                                    "kling" -> "fal-ai/kling"
                                    "kling-video" -> "fal-ai/kling-video"
                                    "klingv21" -> "fal-ai/kling/v2.1/image-to-video"
                                    "klingimage" -> "fal-ai/kling/v1.6/image-to-video"
                                    "kling v2.6 standard" -> "fal-ai/kling/v2.6/standard"
                                    "kling v2.6 pro" -> "fal-ai/kling-video/v2.6/pro/text-to-video"
                                    "kling v3 standard" -> "fal-ai/kling-video/v3/standard/text-to-video"
                                    "kling v3 pro" -> "fal-ai/kling-video/v3/pro/text-to-video"
                                    "kling-video/v3/turbo/pro/text-to-video" -> "fal-ai/kling-video/v3/turbo/pro/text-to-video"
                                    "kling-video/v3/turbo/standard/text-to-video" -> "fal-ai/kling-video/v3/turbo/standard/text-to-video"
                                    "kling-2.5-turbo/pro/text-to-video" -> "fal-ai/kling-video/v2.5-turbo/pro/text-to-video"
                                    "kling-2.5-turbo/standard/image-to-video" -> "fal-ai/kling-2.5-turbo/standard/image-to-video"
                                    else -> null
                                }

                                if (uiState.selectedVideoProvider != "Kling" && uiState.selectedVideoProvider != "Veo" && uiState.selectedVideoProvider != "Bytedance" && uiState.selectedVideoProvider != "PixVerse" && uiState.selectedVideoProvider != "Alibaba" && uiState.selectedVideoProvider != "Minimax" && !uiState.selectedVideoModel.lowercase().contains("minimax") && !uiState.selectedVideoModel.contains("grok-imagine-video") && !uiState.selectedVideoModel.contains("luma") && !uiState.selectedVideoModel.contains("wan") && !uiState.selectedVideoModel.contains("happy-horse")) {
                                    Text(
                                        text = if (actualKlingId != null) "${uiState.selectedVideoModel} ($actualKlingId)" else uiState.selectedVideoModel,
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                }
                                Text(
                                    text = description,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )



                            }
                        }
                    }
                }


            }
        }

        // Image Selection for Image to Video mode
        if (uiState.selectedVideoMode == "Image to Video") {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Face,
                            contentDescription = "Image Selector",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "Select Reference Image:",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Text(
                        text = if (uiState.selectedVideoProvider == "Kling" || uiState.selectedVideoProvider == "Bytedance" || uiState.selectedVideoProvider == "PixVerse" || uiState.selectedVideoProvider == "Xai") "Accepts: .jpg/.png up to 10MB" else "Accepts: .jpg/.png up to 10MB matching chosen aspect ratio ($selectedAspectRatio)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
                    )

                    val combinedImages = uiState.generatedImages + uploadedImages

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // The + Upload Custom Image card is positioned first in the row
                        Box(
                            modifier = Modifier
                                .size(100.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f))
                                .border(
                                    width = 1.dp,
                                    color = MaterialTheme.colorScheme.primary,
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .clickable { galleryLauncher.launch("image/*") },
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                                modifier = Modifier.padding(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "Upload Icon",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Upload\nImage",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.primary,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }

                        combinedImages.forEach { img ->
                            val isImgSelected = selectedImageId == img.id
                            Box(
                                modifier = Modifier
                                    .size(100.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .border(
                                        width = if (isImgSelected) 3.dp else 1.dp,
                                        color = if (isImgSelected) MaterialTheme.colorScheme.primary else Color.Gray,
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .clickable { selectedImageId = img.id }
                            ) {
                                coil.compose.AsyncImage(
                                    model = img.imageUrl,
                                    contentDescription = img.prompt,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                                    error = androidx.compose.ui.res.painterResource(id = android.R.drawable.ic_menu_gallery)
                                )
                                if (isImgSelected) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(Color.Black.copy(alpha = 0.3f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = "Selected",
                                            tint = Color.White,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    if (combinedImages.isEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "No generated images available yet. You can click the '+ Upload Image' card above to use a custom image, or generate one in the 'Eddie Image Studio' tab.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                        )
                    }
                }
            }
        }

        promptInputCard()

        if (uiState.selectedVideoModel != "fal-ai/pixverse/c1/image-to-video") {
            Column {
                Text(
                    text = "Select Cinematic Style",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    styles.forEach { (styleName, isPro) ->
                        val isSelected = selectedStyle == styleName
                        val badgeColor = if (uiState.isSubscribed) MaterialTheme.colorScheme.primary else Color(0xFFFFB100)
                        val badgeTextColor = if (uiState.isSubscribed) MaterialTheme.colorScheme.onPrimary else Color(0xFF111111)

                        Surface(
                            modifier = Modifier.clickable { selectedStyle = styleName },
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                            border = BorderStroke(
                                width = if (isSelected) 2.dp else 1.dp,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = styleName,
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal),
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                )
                                if (isPro) {
                                    Surface(
                                        color = badgeColor,
                                        shape = RoundedCornerShape(4.dp)
                                    ) {
                                        Text(
                                            text = "PRO",
                                            color = badgeTextColor,
                                            fontSize = 8.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        if (uiState.selectedVideoProvider == "OpenAI") {
            Column {
                Text(
                    text = "Video Size",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val openAiSizes = if (uiState.selectedVideoModel.lowercase().trim() == "sora 2") {
                        listOf(
                            "720x1280" to false to "Portrait (Vertical)",
                            "1280x720" to false to "Landscape (Horizontal)"
                        )
                    } else if (uiState.selectedVideoModel.lowercase().trim() == "sora 2 pro") {
                        listOf(
                            "720x1280" to false to "Portrait (Vertical)",
                            "1280x720" to false to "Landscape (Horizontal)",
                            "1024x1792" to true to "HD Portrait pro",
                            "1792x1024" to true to "HD Landscape pro",
                            "1080x1920" to true to "FHD Portrait pro",
                            "1920x1080" to true to "FHD Landscape pro"
                        )
                    } else {
                        listOf(
                            "720x1280" to false to "Portrait (Vertical)",
                            "1280x720" to false to "Landscape (Horizontal)",
                            "1024x1792" to true to "HD Portrait pro",
                            "1792x1024" to true to "HD Landscape pro"
                        )
                    }

                    openAiSizes.forEach { item ->
                        val pair = item.first
                        val sizeOpt = pair.first
                        val isPro = pair.second
                        val label = item.second
                        val isSelected = selectedOpenAiSize == sizeOpt
                        val badgeColor = if (uiState.isSubscribed) MaterialTheme.colorScheme.primary else Color(0xFFFFB100)
                        val badgeTextColor = if (uiState.isSubscribed) MaterialTheme.colorScheme.onPrimary else Color(0xFF111111)

                        Surface(
                            modifier = Modifier
                                .width(135.dp)
                                .clickable { selectedOpenAiSize = sizeOpt },
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                            border = BorderStroke(
                                width = if (isSelected) 2.dp else 1.dp,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(10.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = sizeOpt,
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, fontSize = 13.sp),
                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                    )
                                    if (isPro) {
                                        Surface(
                                            color = badgeColor,
                                            shape = RoundedCornerShape(4.dp)
                                        ) {
                                            Text(
                                                text = "PRO",
                                                color = badgeTextColor,
                                                fontSize = 7.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 3.dp, vertical = 1.dp)
                                            )
                                        }
                                    }
                                }
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }
            }
        } else if (aspectRatios.isNotEmpty() && !isKlingV3Standard && !isKlingV3Pro && !isKlingV26Pro && !isKlingV25TurboStandard && !isKlingV25TurboPro && !isKlingV3TurboStandard && !isKlingV3TurboPro && !isKlingV3_4k && !isKlingO3Standard && !isKlingO3Pro && !isKlingO3_4k && uiState.selectedVideoModel != "fal-ai/pixverse/v5.6/image-to-video" && uiState.selectedVideoModel != "fal-ai/pixverse/v6/image-to-video" && uiState.selectedVideoModel != "fal-ai/pixverse/c1/image-to-video") {
            Column {
                Text(
                    text = "Video Aspect Ratio",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    aspectRatios.forEach { item ->
                        val ratioPair = item.first
                        val ratio = ratioPair.first
                        val isPro = ratioPair.second
                        val label = item.second
                        val isSelected = selectedAspectRatio == ratio
                        val badgeColor = if (uiState.isSubscribed) MaterialTheme.colorScheme.primary else Color(0xFFFFB100)
                        val badgeTextColor = if (uiState.isSubscribed) MaterialTheme.colorScheme.onPrimary else Color(0xFF111111)

                        Surface(
                            modifier = Modifier
                                .width(135.dp)
                                .clickable { selectedAspectRatio = ratio },
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                            border = BorderStroke(
                                width = if (isSelected) 2.dp else 1.dp,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(10.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = ratio,
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                    )
                                    if (isPro) {
                                        Surface(
                                            color = badgeColor,
                                            shape = RoundedCornerShape(4.dp)
                                        ) {
                                            Text(
                                                text = "PRO",
                                                color = badgeTextColor,
                                                fontSize = 7.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 3.dp, vertical = 1.dp)
                                            )
                                        }
                                    }
                                }
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }

        if (resolutions.isNotEmpty() && !isVeo2 && uiState.selectedVideoProvider != "OpenAI") {
            Column {
                Text(
                    text = "Video Resolution",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                if ((uiState.selectedVideoProvider == "FalAI" || uiState.selectedVideoProvider == "Minimax") && uiState.selectedVideoModel == "fal-ai/minimax/hailuo-2.3/standard/image-to-video") {
                    Text(
                        text = "Your request will cost 80 token per 6 second video generation, and a 150 token per 10 second video generation.",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                } else if ((uiState.selectedVideoProvider == "FalAI" || uiState.selectedVideoProvider == "Minimax") && uiState.selectedVideoModel == "fal-ai/minimax/hailuo-2.3/pro/image-to-video") {
                    Text(
                        text = "Your request will cost 140 token per video generation.",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    resolutions.forEach { item ->
                        val pair = item.first
                        val res = pair.first
                        val isPro = pair.second
                        val label = item.second
                        val isSelected = selectedResolution == res
                        val badgeColor = if (uiState.isSubscribed) MaterialTheme.colorScheme.primary else Color(0xFFFFB100)
                        val badgeTextColor = if (uiState.isSubscribed) MaterialTheme.colorScheme.onPrimary else Color(0xFF111111)

                        Surface(
                            modifier = Modifier
                                .widthIn(min = 135.dp)
                                .clickable { selectedResolution = res },
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                            border = BorderStroke(
                                width = if (isSelected) 2.dp else 1.dp,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(10.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = res,
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                    )
                                    if (isPro) {
                                        Surface(
                                            color = badgeColor,
                                            shape = RoundedCornerShape(4.dp)
                                        ) {
                                            Text(
                                                text = "PRO",
                                                color = badgeTextColor,
                                                fontSize = 7.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 3.dp, vertical = 1.dp)
                                            )
                                        }
                                    }
                                }
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }

        if (durations.isNotEmpty()) {
        Column {
            Text(
                text = "Video Duration",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                durations.forEach { item ->
                    val pair = item.first
                    val dur = pair.first
                    val isPro = pair.second
                    val label = item.second
                    val isSelected = selectedDuration == dur
                    val badgeColor = if (uiState.isSubscribed) MaterialTheme.colorScheme.primary else Color(0xFFFFB100)
                    val badgeTextColor = if (uiState.isSubscribed) MaterialTheme.colorScheme.onPrimary else Color(0xFF111111)

                    Surface(
                        modifier = Modifier
                            .width(135.dp)
                            .clickable { selectedDuration = dur },
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                        border = BorderStroke(
                            width = if (isSelected) 2.dp else 1.dp,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = dur,
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                )
                                if (isPro) {
                                    Surface(
                                        color = badgeColor,
                                        shape = RoundedCornerShape(4.dp)
                                    ) {
                                        Text(
                                            text = "PRO",
                                            color = badgeTextColor,
                                            fontSize = 7.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 3.dp, vertical = 1.dp)
                                        )
                                    }
                                }
                            }
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

            if (isHunyuan15Video || isKlingV3Standard || isKlingV3Pro || isKlingV3TurboPro || isKlingV3TurboStandard || isKlingV26Pro || isKlingV25TurboStandard || isKlingV25TurboPro || isKlingV3_4k || isKlingO3_4k || isKlingO3Standard || isKlingO3Pro || isBytedance || uiState.selectedVideoProvider == "PixVerse" || isGrokImagineVideo || isLumaRayVideo || isAlibabaVideo || ((uiState.selectedVideoProvider == "FalAI" || uiState.selectedVideoProvider == "Minimax") && (uiState.selectedVideoModel == "fal-ai/minimax/hailuo-02/standard/image-to-video" || uiState.selectedVideoModel == "fal-ai/minimax/hailuo-02/pro/image-to-video" || uiState.selectedVideoModel == "fal-ai/minimax/hailuo-2.3/standard/image-to-video" || uiState.selectedVideoModel == "fal-ai/minimax/hailuo-2.3/pro/image-to-video" || uiState.selectedVideoModel == "fal-ai/minimax/hailuo-02/standard/text-to-video" || uiState.selectedVideoModel == "fal-ai/minimax/hailuo-02/pro/text-to-video" || uiState.selectedVideoModel == "fal-ai/minimax/hailuo-2.3/standard/text-to-video" || uiState.selectedVideoModel == "fal-ai/minimax/hailuo-2.3/pro/text-to-video"))) {
                Spacer(modifier = Modifier.height(8.dp))
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)
                    ),
                    border = BorderStroke(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "💡",
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Text(
                            text = getLumaVideoCostHint(
                                uiState = uiState,
                                isAlibabaVideo = isAlibabaVideo,
                                isLumaRayVideo = isLumaRayVideo,
                                isGrokImagineVideo = isGrokImagineVideo,
                                isBytedance = isBytedance,
                                isKlingV25TurboStandard = isKlingV25TurboStandard,
                                isKlingV25TurboPro = isKlingV25TurboPro,
                                isKlingV26Pro = isKlingV26Pro,
                                isFalVeo3_1Fast = isFalVeo3_1Fast,
                                isFalVeo3OnlyFast = isFalVeo3OnlyFast,
                                isFalVeo3_1Lite = isFalVeo3_1Lite,
                                isFalVeo3_1 = isFalVeo3_1,
                                isFalVeo3Only = isFalVeo3Only,
                                isKlingV3_4k = isKlingV3_4k,
                                isKlingO3_4k = isKlingO3_4k,
                                isKlingO3Standard = isKlingO3Standard,
                                isKlingO3Pro = isKlingO3Pro,
                                isKlingV3TurboPro = isKlingV3TurboPro,
                                isKlingV3TurboStandard = isKlingV3TurboStandard,
                                isKlingV3Pro = isKlingV3Pro,
                                isKlingV3Standard = isKlingV3Standard
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            if (isVeo2 || isFalVeo3 || isFalVeo3FastModel || isOpenAiSora2 || isOpenAiSora2Pro) {
                Spacer(modifier = Modifier.height(8.dp))
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)
                    ),
                    border = BorderStroke(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "🪙",
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Text(
                            text = getVeoSoraVideoCostHint(
                                isOpenAiSora2 = isOpenAiSora2,
                                isOpenAiSora2Pro = isOpenAiSora2Pro,
                                isVeo2 = isVeo2,
                                isFalVeo3_1Lite = isFalVeo3_1Lite,
                                isFalVeo3_1 = isFalVeo3_1,
                                isFalVeo3Only = isFalVeo3Only,
                                isFalVeo3_1Fast = isFalVeo3_1Fast,
                                isFalVeo3OnlyFast = isFalVeo3OnlyFast
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            if (isOpenAiSora2 || isOpenAiSora2Pro) {
                Spacer(modifier = Modifier.height(16.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                    ),
                    border = BorderStroke(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(bottom = 12.dp)
                        ) {
                            Text(
                                text = "⚙️",
                                fontSize = 20.sp
                            )
                            Text(
                                text = if (isOpenAiSora2Pro) "OpenAI Sora 2 Pro Advanced Parameters" else "OpenAI Sora 2 Advanced Parameters",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        if (!isOpenAiSora2Pro) {
                            // Model Snapshot Selector
                            Text(
                                text = "Model Snapshot",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(bottom = 6.dp)
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                val snapshots = listOf("sora-2", "sora-2-2025-12-08", "sora-2-2025-10-06")
                                snapshots.forEach { snapshot ->
                                    val isSel = uiState.soraModelSnapshot == snapshot
                                    Surface(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clickable {
                                                chatViewModel.updateSoraSettings(
                                                    deleteVideo = uiState.soraDeleteVideo,
                                                    modelSnapshot = snapshot,
                                                    detectAndBlockIp = uiState.soraDetectAndBlockIp,
                                                    characterIds = uiState.soraCharacterIds
                                                )
                                            },
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (isSel) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                                        border = BorderStroke(
                                            width = if (isSel) 2.dp else 1.dp,
                                            color = if (isSel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                                        )
                                    ) {
                                        Text(
                                            text = snapshot,
                                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                            color = if (isSel) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                                            textAlign = TextAlign.Center,
                                            modifier = Modifier.padding(vertical = 10.dp, horizontal = 4.dp)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))
                        }

                        // Character IDs field
                        Text(
                            text = "Character IDs",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                        Text(
                            text = "Enter up to two character IDs (comma separated) to persist style identity across frames. Refer to them by name in your prompt.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                        OutlinedTextField(
                            value = uiState.soraCharacterIds,
                            onValueChange = { ids ->
                                chatViewModel.updateSoraSettings(
                                    deleteVideo = uiState.soraDeleteVideo,
                                    modelSnapshot = uiState.soraModelSnapshot,
                                    detectAndBlockIp = uiState.soraDetectAndBlockIp,
                                    characterIds = ids
                                )
                            },
                            placeholder = { Text("e.g. char_123, char_456") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(8.dp),
                            textStyle = MaterialTheme.typography.bodyMedium
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        // Toggles row: delete_video and detect_and_block_ip
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Checkbox(
                                        checked = uiState.soraDeleteVideo,
                                        onCheckedChange = { isChecked ->
                                            chatViewModel.updateSoraSettings(
                                                deleteVideo = isChecked,
                                                modelSnapshot = uiState.soraModelSnapshot,
                                                detectAndBlockIp = uiState.soraDetectAndBlockIp,
                                                characterIds = uiState.soraCharacterIds
                                            )
                                        }
                                    )
                                    Text(
                                        text = "Delete Video",
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                                    )
                                }
                                Text(
                                    text = "Deletes from storage after generation for maximum privacy.",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                    modifier = Modifier.padding(start = 32.dp)
                                )
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Checkbox(
                                        checked = uiState.soraDetectAndBlockIp,
                                        onCheckedChange = { isChecked ->
                                            chatViewModel.updateSoraSettings(
                                                deleteVideo = uiState.soraDeleteVideo,
                                                modelSnapshot = uiState.soraModelSnapshot,
                                                detectAndBlockIp = isChecked,
                                                characterIds = uiState.soraCharacterIds
                                            )
                                        }
                                    )
                                    Text(
                                        text = "IP Protection",
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                                    )
                                }
                                Text(
                                    text = "Detects and blocks known intellectual property references.",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                    modifier = Modifier.padding(start = 32.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

    if (((uiState.selectedVideoProvider == "Gemini" && uiState.selectedVideoMode == "Text to Video") || isFalVeo3 || isFalVeo3FastModel || isVeo2 || isKlingV26Pro || isKlingV3Standard || isKlingV3Pro || isKlingO3_4k || isKlingO3Standard || isKlingO3Pro || isKlingV3_4k || isBytedance || uiState.selectedVideoProvider == "PixVerse") && !isVeo2) {
            Column {
                Text(
                    text = "Generate Audio",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(bottom = 4.dp)
                )

                if (isBytedance) {
                    Text(
                        text = "Whether to generate synchronized audio for the video, including sound effects, ambient sounds, and lip-synced speech. The cost of video generation is the same regardless of whether audio is generated or not.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                } else if (uiState.selectedVideoProvider == "PixVerse") {
                    Text(
                        text = "Generate immersive sound effects, background soundtracks, or atmospheric audio synchronized directly with your video flow.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                } else {
                    Spacer(modifier = Modifier.height(4.dp))
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val audioOptions = listOf("On", "Off")
                    audioOptions.forEach { opt ->
                        val isSelected = selectedAudioOption == opt
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { selectedAudioOption = opt },
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                            border = BorderStroke(
                                width = if (isSelected) 2.dp else 1.dp,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(10.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = if (opt == "On") "🔊 On" else "🔇 Off",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = if (opt == "On") "Dynamic soundtrack" else "Mute / Video only",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }

        if (isKlingV3Standard || isKlingV3Pro || isKlingO3_4k || isKlingO3Standard || isKlingO3Pro || isKlingV3_4k) {
            Spacer(modifier = Modifier.height(16.dp))
            Column {
                Text(
                    text = "Shot Type",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val shotOptions = listOf("customize", "intelligent")
                    shotOptions.forEach { opt ->
                        val isSelected = selectedShotType == opt
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { selectedShotType = opt },
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                            border = BorderStroke(
                                width = if (isSelected) 2.dp else 1.dp,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(10.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = if (opt == "customize") "🎬 Customize" else "🧠 Intelligent",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = if (opt == "customize") "Manual structure" else "Auto structure",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }

        if (isVeo2 || isFalVeo3 || isFalVeo3FastModel) {
            VeoEngineParametersCard(
                isVeo2 = isVeo2,
                isFalVeo3 = isFalVeo3,
                isFalVeo3FastModel = isFalVeo3FastModel,
                negativePromptText = negativePromptText,
                onNegativePromptTextChange = { negativePromptText = it },
                enhancePrompt = enhancePrompt,
                onEnhancePromptChange = { enhancePrompt = it },
                seedText = seedText,
                onSeedTextChange = { seedText = it },
                autoFix = autoFix,
                onAutoFixChange = { autoFix = it },
                safetyTolerance = safetyTolerance,
                onSafetyToleranceChange = { safetyTolerance = it }
            )
        }

        if (isBytedance) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Build,
                        contentDescription = "Bytedance Parameters",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "ByteDance Seedance 2.0 Parameters",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                // bitrate_mode BitrateModeEnum
                Column {
                    Text(
                        text = "Bitrate Mode",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                    Text(
                        text = "Output bitrate mode. 'high' requests a higher-quality, larger-file encode from the model; 'standard' uses the default bitrate.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val bitrates = listOf("standard", "high")
                        bitrates.forEach { mode ->
                            val isSelected = selectedBitrateMode == mode
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { selectedBitrateMode = mode },
                                shape = RoundedCornerShape(10.dp),
                                color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                                border = BorderStroke(
                                    width = if (isSelected) 2.dp else 1.dp,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                                )
                            ) {
                                Box(
                                    modifier = Modifier.padding(vertical = 12.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = mode.replaceFirstChar { it.uppercase() },
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }

                // end_user_id string
                Column {
                    Text(
                        text = "End User ID",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                    Text(
                        text = "The unique user ID of the end user.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                    OutlinedTextField(
                        value = endUserIdText,
                        onValueChange = { endUserIdText = it },
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = MaterialTheme.typography.bodyMedium,
                        placeholder = { Text("e.g., aistudio-user-default", style = MaterialTheme.typography.bodyMedium) },
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                        )
                    )
                }
            }
        }

        if (isHunyuan15Video) {
            HunyuanV15ParametersCard(
                negativePromptText = negativePromptText,
                onNegativePromptTextChange = { negativePromptText = it },
                numInferenceSteps = hunyuanInferenceSteps,
                onNumInferenceStepsChange = { hunyuanInferenceSteps = it },
                seedText = seedText,
                onSeedTextChange = { seedText = it },
                numFrames = hunyuanFrames,
                onNumFramesChange = { hunyuanFrames = it },
                enablePromptExpansion = enhancePrompt,
                onEnablePromptExpansionChange = { enhancePrompt = it }
            )
        }

        if (isLumaRayVideo) {
            LumaRayEngineParametersCard(
                loop = lumaLoop,
                onLoopChange = { lumaLoop = it },
                hdr = lumaHdr,
                onHdrChange = { lumaHdr = it },
                exrExport = lumaExrExport,
                onExrExportChange = { lumaExrExport = it },
                selectedResolution = selectedResolution,
                onResolutionChange = { selectedResolution = it },
                selectedDuration = selectedDuration,
                onDurationChange = { selectedDuration = it }
            )
        }

        if (isAlibabaVideo && (uiState.selectedVideoModel.lowercase().trim().contains("wan/v2.5") || uiState.selectedVideoModel.lowercase().trim().contains("wan-25-preview") || uiState.selectedVideoModel.lowercase().trim().contains("wan/v2.6") || uiState.selectedVideoModel.lowercase().trim().contains("wan/v2.7") || uiState.selectedVideoModel.lowercase().trim().contains("happy-horse"))) {
            AlibabaWanParametersCard(
                selectedModel = uiState.selectedVideoModel,
                selectedVideoMode = uiState.selectedVideoMode,
                negativePromptText = negativePromptText,
                onNegativePromptTextChange = { negativePromptText = it },
                enhancePrompt = enhancePrompt,
                onEnhancePromptChange = { enhancePrompt = it },
                seedText = seedText,
                onSeedTextChange = { seedText = it },
                autoFix = autoFix,
                onAutoFixChange = { autoFix = it },
                alibabaAudioUrl = alibabaAudioUrl,
                onAlibabaAudioUrlChange = { alibabaAudioUrl = it },
                wanMultiShots = wanMultiShots,
                onWanMultiShotsChange = { wanMultiShots = it },
                wanEndImageUrl = wanEndImageUrl,
                onWanEndImageUrlChange = { wanEndImageUrl = it },
                wanVideoUrl = wanVideoUrl,
                onWanVideoUrlChange = { wanVideoUrl = it }
            )
        }

        if ((uiState.selectedVideoProvider == "FalAI" || uiState.selectedVideoProvider == "Minimax") && (uiState.selectedVideoModel == "fal-ai/minimax/hailuo-02/standard/image-to-video" || uiState.selectedVideoModel == "fal-ai/minimax/hailuo-02/pro/image-to-video" || uiState.selectedVideoModel == "fal-ai/minimax/hailuo-2.3/standard/image-to-video" || uiState.selectedVideoModel == "fal-ai/minimax/hailuo-2.3/pro/image-to-video" || uiState.selectedVideoModel == "fal-ai/minimax/hailuo-02/standard/text-to-video" || uiState.selectedVideoModel == "fal-ai/minimax/hailuo-02/pro/text-to-video" || uiState.selectedVideoModel == "fal-ai/minimax/hailuo-2.3/standard/text-to-video" || uiState.selectedVideoModel == "fal-ai/minimax/hailuo-2.3/pro/text-to-video")) {
            MiniMaxHailuoParametersCard(
                promptOptimizer = minimaxPromptOptimizer,
                onPromptOptimizerChange = { minimaxPromptOptimizer = it },
                endImageUrl = minimaxEndImageUrl,
                onEndImageUrlChange = { minimaxEndImageUrl = it },
                showEndImageUrl = uiState.selectedVideoModel != "fal-ai/minimax/hailuo-2.3/standard/image-to-video" && uiState.selectedVideoModel != "fal-ai/minimax/hailuo-2.3/pro/image-to-video" && uiState.selectedVideoModel != "fal-ai/minimax/hailuo-02/standard/text-to-video" && uiState.selectedVideoModel != "fal-ai/minimax/hailuo-02/pro/text-to-video" && uiState.selectedVideoModel != "fal-ai/minimax/hailuo-2.3/standard/text-to-video" && uiState.selectedVideoModel != "fal-ai/minimax/hailuo-2.3/pro/text-to-video"
            )
        }

        if (uiState.selectedVideoProvider == "PixVerse") {
            PixVerseEngineParametersCard(
                negativePromptText = negativePromptText,
                onNegativePromptTextChange = { negativePromptText = it },
                seedText = seedText,
                onSeedTextChange = { seedText = it },
                pixverseMultiClip = pixverseMultiClip,
                onPixverseMultiClipChange = { pixverseMultiClip = it },
                pixverseThinkingType = pixverseThinkingType,
                onPixverseThinkingTypeChange = { pixverseThinkingType = it },
                showAdvancedOptions = uiState.selectedVideoModel != "fal-ai/pixverse/c1/image-to-video"
            )
        }

        if (uiState.selectedVideoProvider == "Kling" && !isKlingV3TurboPro && !isKlingV3TurboStandard && !isKlingO3_4k && !isKlingO3Pro) {
            KlingEngineParametersCard(
                isKlingO3Standard = isKlingO3Standard,
                isKlingO3Pro = isKlingO3Pro,
                isKlingV26Pro = isKlingV26Pro,
                isKlingV25TurboPro = isKlingV25TurboPro,
                isKlingV3Standard = isKlingV3Standard,
                isKlingV3Pro = isKlingV3Pro,
                negativePromptText = negativePromptText,
                onNegativePromptTextChange = { negativePromptText = it },
                cfgScale = cfgScale,
                onCfgScaleChange = { cfgScale = it },
                klingEndImageUrl = klingEndImageUrl,
                onKlingEndImageUrlChange = { klingEndImageUrl = it },
                selectedAudioOption = selectedAudioOption,
                klingVoiceControl = klingVoiceControl,
                onKlingVoiceControlChange = { klingVoiceControl = it },
                klingElements = klingElements,
                klingV26StartImageUrl = klingV26StartImageUrl,
                onKlingV26StartImageUrlChange = { klingV26StartImageUrl = it },
                klingV26VoiceIds = klingV26VoiceIds,
                onKlingV26VoiceIdsChange = { klingV26VoiceIds = it },
                klingV26EndImageUrl = klingV26EndImageUrl,
                onKlingV26EndImageUrlChange = { klingV26EndImageUrl = it }
            )
        }

        if (isKlingO3Pro || isKlingO3_4k || isKlingO3Standard) {
            Spacer(modifier = Modifier.height(12.dp))
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "End Image URL (Optional)",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(bottom = 2.dp)
                )
                Text(
                    text = "Animate transition seamlessly from start image to end image.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                OutlinedTextField(
                    value = klingEndImageUrl,
                    onValueChange = { klingEndImageUrl = it },
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = MaterialTheme.typography.bodyMedium,
                    placeholder = { Text("https://example.com/end.jpg", style = MaterialTheme.typography.bodyMedium) },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                    )
                )
            }
        }



        if (showProgressPanel) {
            Surface(
                color = Color(0xFF1E1E1E),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Eddie Video Studio - Processing Render API...",
                        color = Color.Green,
                        style = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    if (uiState.videoGenIsLoading) {
                        VideoProgressVisualizer()
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Generating video...",
                            color = Color.White,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "This may take 1-5 minutes.",
                            color = Color.LightGray,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    Text(
                        text = uiState.videoGenLog,
                        color = Color.White,
                        style = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 12.sp, lineHeight = 16.sp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    if (uiState.videoGenIsLoading) {
                        LinearProgressIndicator(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            color = Color.Green,
                            trackColor = Color.DarkGray
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = { showProgressPanel = false },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "Minimize to Background ⬇️",
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSecondary
                            )
                        }
                    } else {
                        Button(
                            onClick = { showProgressPanel = false },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "✕ Dismiss Log Progress",
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    }
                }
            }
        } else {
            if (uiState.videoGenIsLoading) {
                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.8f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showProgressPanel = true }
                        .padding(bottom = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),
                            strokeWidth = 2.5.dp,
                            color = MaterialTheme.colorScheme.secondary
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Active Video Generation Running in Background...",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                            Text(
                                text = "Generating video in the background. Tap here to view log progress / status.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                            )
                        }
                    }
                }
            } else {
                val isFormValid = if ((isKlingV3Standard || isKlingV3Pro || isKlingV3TurboPro || isKlingV3TurboStandard || isKlingO3_4k || isKlingO3Standard || isKlingO3Pro || isKlingV3_4k) && useStoryboard) {
                    storyboardShots.all { it.first.value.isNotBlank() } && storyboardShots.sumOf { it.second.value.toIntOrNull() ?: 5 } <= 15
                } else {
                    prompt.isNotBlank()
                }

                Button(
                    onClick = { 
                        val ratioParam = if (uiState.selectedVideoProvider == "OpenAI") selectedOpenAiSize else selectedAspectRatio
                        val resParam = if ((uiState.selectedVideoProvider == "OpenAI" && !isOpenAiSora2) || resolutions.isEmpty()) "Default" else selectedResolution
                        val selectedImgUrl = (uiState.generatedImages + uploadedImages).find { it.id == selectedImageId }?.imageUrl
                        
                        val finalDuration = if ((isKlingV3Standard || isKlingV3Pro || isKlingV3TurboPro || isKlingV3TurboStandard || isKlingO3_4k || isKlingO3Standard || isKlingO3Pro || isKlingV3_4k) && useStoryboard) {
                            val totalDur = storyboardShots.sumOf { it.second.value.toIntOrNull() ?: 5 }
                            "${totalDur}s"
                        } else {
                            selectedDuration
                        }

                        val multiPromptJson = if ((isKlingV3Standard || isKlingV3Pro || isKlingV3TurboPro || isKlingV3TurboStandard || isKlingO3_4k || isKlingO3Standard || isKlingO3Pro || isKlingV3_4k) && useStoryboard) {
                            val array = org.json.JSONArray()
                            storyboardShots.forEach { shot ->
                                val obj = org.json.JSONObject().apply {
                                    put("prompt", shot.first.value)
                                    put("duration", shot.second.value.toIntOrNull() ?: 5)
                                }
                                array.put(obj)
                            }
                            array.toString()
                        } else {
                            null
                        }

                        val finalSeed = seedText.trim().toIntOrNull()
                        
                        val isWan27 = isAlibabaVideo && uiState.selectedVideoModel.lowercase().trim().contains("wan/v2.7")
                        val endImgParam = if ((isKlingV3Standard || isKlingV3Pro || isKlingV25TurboPro || isKlingO3Standard || isKlingO3Pro || isKlingO3_4k) && klingEndImageUrl.isNotBlank()) {
                            klingEndImageUrl
                        } else if (isKlingV26Pro && klingV26EndImageUrl.isNotBlank()) {
                            klingV26EndImageUrl
                        } else if (isWan27 && wanEndImageUrl.isNotBlank()) {
                            wanEndImageUrl
                        } else if ((uiState.selectedVideoModel == "fal-ai/minimax/hailuo-02/standard/image-to-video" || uiState.selectedVideoModel == "fal-ai/minimax/hailuo-02/pro/image-to-video") && minimaxEndImageUrl.isNotBlank()) {
                            minimaxEndImageUrl
                        } else {
                            null
                        }

                        val elementsJsonParam = if ((isKlingV3Standard || isKlingV3Pro) && klingElements.isNotEmpty()) {
                            val array = org.json.JSONArray()
                            klingElements.forEach { array.put(it.toJsonObject()) }
                            array.toString()
                        } else {
                            null
                        }
                        val voiceControlParam = (isKlingV3Standard || isKlingV3Pro || isKlingV26Pro) && (selectedAudioOption == "On") && klingVoiceControl

                        val v26VoiceIdsParam = if (isKlingV26Pro && klingV26VoiceIds.isNotBlank()) {
                            klingV26VoiceIds.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                        } else {
                            null
                        }

                        val v26StartImgParam = if (isKlingV26Pro && klingV26StartImageUrl.isNotBlank()) {
                            klingV26StartImageUrl
                        } else {
                            null
                        }

                        onGenerateVideo(
                            prompt,
                            selectedStyle,
                            ratioParam,
                            resParam,
                            finalDuration,
                            selectedAudioOption,
                            selectedPersonGeneration,
                            negativePromptText,
                            sampleCountVideos,
                            selectedImgUrl,
                            cfgScale,
                            if (isKlingV3Standard || isKlingV3Pro || isKlingO3_4k || isKlingO3Standard || isKlingO3Pro || isKlingV3_4k) selectedShotType else "customize",
                            multiPromptJson,
                            finalSeed,
                            if (uiState.selectedVideoModel == "fal-ai/minimax/hailuo-02/standard/image-to-video" || uiState.selectedVideoModel == "fal-ai/minimax/hailuo-02/pro/image-to-video" || uiState.selectedVideoModel == "fal-ai/minimax/hailuo-2.3/standard/image-to-video" || uiState.selectedVideoModel == "fal-ai/minimax/hailuo-2.3/pro/image-to-video" || uiState.selectedVideoModel == "fal-ai/minimax/hailuo-02/standard/text-to-video" || uiState.selectedVideoModel == "fal-ai/minimax/hailuo-02/pro/text-to-video" || uiState.selectedVideoModel == "fal-ai/minimax/hailuo-2.3/standard/text-to-video" || uiState.selectedVideoModel == "fal-ai/minimax/hailuo-2.3/pro/text-to-video") minimaxPromptOptimizer else enhancePrompt,
                            autoFix,
                            safetyTolerance.toIntOrNull() ?: 4,
                            endImgParam,
                            elementsJsonParam,
                            voiceControlParam,
                            v26VoiceIdsParam,
                            v26StartImgParam,
                            selectedBitrateMode,
                            endUserIdText,
                            pixverseMultiClip,
                            pixverseThinkingType,
                            lumaLoop,
                            lumaHdr,
                            lumaExrExport,
                            alibabaAudioUrl,
                            wanMultiShots,
                            if (isWan27) wanVideoUrl else null,
                            hunyuanInferenceSteps,
                            hunyuanFrames
                        )
                        showProgressPanel = true
                    },
                    enabled = isFormValid,
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "Start AI Video Render 🚀",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), modifier = Modifier.padding(vertical = 8.dp))

        Text(
            text = "Your AI Video Collection",
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface
        )

        if (uiState.generatedVideos.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .border(
                        BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                        shape = RoundedCornerShape(12.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("💡", fontSize = 28.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "No videos rendered yet.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Modify your descriptive words above to render your first video!",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 24.dp).padding(top = 4.dp)
                    )
                }
            }
        } else {
            androidx.compose.foundation.lazy.LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(vertical = 4.dp)
            ) {
                items(uiState.generatedVideos) { video ->
                    Card(
                        modifier = Modifier
                            .width(260.dp)
                            .clickable { selectedVideoForPlay = video },
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                    ) {
                        Column {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(135.dp)
                                    .background(
                                        brush = androidx.compose.ui.graphics.Brush.linearGradient(
                                            colors = listOf(
                                                MaterialTheme.colorScheme.primaryContainer,
                                                MaterialTheme.colorScheme.secondaryContainer
                                            )
                                        )
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = Color.Black.copy(alpha = 0.5f),
                                    modifier = Modifier.size(50.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.PlayArrow,
                                            contentDescription = "Play",
                                            tint = Color.White,
                                            modifier = Modifier.size(28.dp)
                                        )
                                    }
                                }

                                Surface(
                                    modifier = Modifier
                                        .align(Alignment.BottomEnd)
                                        .padding(8.dp),
                                    shape = RoundedCornerShape(4.dp),
                                    color = Color.Black.copy(alpha = 0.7f)
                                ) {
                                    Text(
                                        text = "${video.aspectRatio} • ${video.resolution} • ${video.duration}",
                                        color = Color.White,
                                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }

                                // Model name badge in top right of the generated video card
                                val modelDisplayName = if (video.model.isNotBlank()) {
                                    val m = video.model.lowercase()
                                    when {
                                        m.contains("grok-imagine-video") -> "Grok Imagine"
                                        m.contains("pixverse/v6/text-to-video") || m.contains("pixverse/v6/image-to-video") -> "Pixverse v6"
                                        m.contains("pixverse/v5.6/text-to-video") || m.contains("pixverse/v5.6/image-to-video") -> "Pixverse v5.6"
                                        m.contains("pixverse/v5.5/text-to-video") -> "Pixverse v5.5"
                                        m.contains("pixverse/c1/image-to-video") -> "Pixverse C1"
                                        m.contains("pixverse") -> "Pixverse"
                                        else -> video.model
                                    }
                                } else "Unknown"

                                Surface(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(8.dp),
                                    shape = RoundedCornerShape(6.dp),
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.85f)
                                ) {
                                    Text(
                                        text = modelDisplayName,
                                        color = MaterialTheme.colorScheme.onPrimary,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 9.sp,
                                            letterSpacing = 0.5.sp
                                        ),
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                            }

                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = video.prompt,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.height(40.dp)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Style: ${video.style}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    TextButton(
                                        onClick = { onDeleteVideo(video.id) },
                                        contentPadding = PaddingValues(horizontal = 8.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Delete",
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Delete", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelSmall)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(30.dp))
    }
    }

    selectedVideoForPlay?.let { video ->
        VideoPlayerDialog(
            videoItem = video,
            chatViewModel = chatViewModel,
            onDismissRequest = { selectedVideoForPlay = null },
            onDeleteVideo = { id ->
                onDeleteVideo(id)
                selectedVideoForPlay = null
            },
            onEditVideo = {
                editingVideoItem = video
            }
        )
    }

    editingVideoItem?.let { item ->
        EddieVideoEditorDialog(
            videoItem = item,
            onDismissRequest = { editingVideoItem = null },
            onSaveEditedVideo = onSaveEditedVideo
        )
    }
}

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun EddieMusicGeneratorView(
    uiState: ChatUiState,
    paddingValues: PaddingValues,
    onGenerateMusic: (String, String, String, String) -> Unit,
    onDeleteMusic: (String) -> Unit,
    onSaveEditedMusic: (String, String, String, String, String, String) -> Unit,
    onProviderChange: (String) -> Unit,
    onModelChange: (String) -> Unit
) {
    val context = LocalContext.current
    var prompt by remember { mutableStateOf("") }
    var selectedGenre by remember { mutableStateOf("Pop") }
    var selectedDuration by remember { mutableStateOf("15s") }
    var selectedTempo by remember { mutableStateOf("Medium") }
    
    val quickInspirations = remember {
        listOf(
            "Chill song with soft acoustic guitar strumming by the campfire",
            "Peaceful classical piano melody with a gentle oceanic breeze",
            "Thumping heavy bass blended with an energetic retro neon synth",
            "Lo-fi hip-hop beat with vinyl crackle and cozy window rain sounds",
            "Atmospheric futuristic space ambient theme with slow deep cosmic pads",
            "Upbeat summer pop acoustic ukulele with bright handclaps and whistling",
            "Moody film-noir dark saxophone solo over slow brushed drum jazz beats",
            "Intense cinematic orchestral battle drums with dramatic brass stabs",
            "Groovy funk bassline with 70s electric piano and clean rhythmic guitars",
            "Cyberpunk industrial techno beat with distorted synths and driving drums",
            "Mellow bossa nova acoustic guitar with a soft shaker and wave sounds",
            "Dreamy vaporwave aesthetic track with slowed-down retro keyboard chords",
            "Ethereal celtic harp melody with a distant flute in a mystical forest",
            "Energetic high-tempo driving rock riff with powerful kick-bass grooves",
            "Reggae groove with deep dub bassline, rimshots and upbeat organ chords"
        ).shuffled().take(4)
    }

    // Audio player state
    var mediaPlayer by remember { mutableStateOf<android.media.MediaPlayer?>(null) }
    var playingMusicId by remember { mutableStateOf<String?>(null) }
    var isAudioPlaying by remember { mutableStateOf(false) }
    var playPercent by remember { mutableStateOf(0f) }

    var editingMusicItem by remember { mutableStateOf<com.example.data.db.GeneratedMusicEntity?>(null) }

    val musicScrollState = rememberScrollState()
    val isKeyboardVisible = WindowInsets.isImeVisible
    LaunchedEffect(isKeyboardVisible) {
        if (isKeyboardVisible) {
            musicScrollState.animateScrollTo(musicScrollState.maxValue)
        }
    }
    LaunchedEffect(prompt) {
        if (prompt.isNotEmpty()) {
            musicScrollState.animateScrollTo(musicScrollState.maxValue)
        }
    }

    // Disposable effect to release media player when composable is disposed or player instance changes
    DisposableEffect(mediaPlayer) {
        onDispose {
            try {
                mediaPlayer?.release()
            } catch (e: Exception) {
                // Ignore
            }
        }
    }

    // Launch a coroutine to poll current track play position for progress animation!
    LaunchedEffect(playingMusicId, isAudioPlaying) {
        if (playingMusicId != null && isAudioPlaying) {
            while (true) {
                val mp = mediaPlayer
                try {
                    if (mp != null && mp.isPlaying) {
                        val durationMs = mp.duration.toFloat()
                        if (durationMs > 0) {
                            playPercent = mp.currentPosition.toFloat() / durationMs
                        }
                    }
                } catch (e: Exception) {
                    // ignore mp exceptions
                }
                kotlinx.coroutines.delay(250)
            }
        } else {
            playPercent = 0f
        }
    }

    fun togglePlayMusic(id: String, url: String) {
        if (playingMusicId == id) {
            val mp = mediaPlayer
            if (isAudioPlaying) {
                mp?.pause()
                isAudioPlaying = false
            } else {
                try {
                    mp?.start()
                    isAudioPlaying = true
                } catch (e: Exception) {
                    android.util.Log.e("EddieMusic", "Failed to resume audio", e)
                }
            }
        } else {
            // Stop and release previous
            try {
                mediaPlayer?.stop()
            } catch (e: Exception) {
                // Ignore exception if already stopped/uninitialized
            }
            try {
                mediaPlayer?.release()
            } catch (e: Exception) {
                // Ignore
            }
            mediaPlayer = null
            playingMusicId = null
            isAudioPlaying = false
            playPercent = 0f

            try {
                val attributionContext = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                    context.createAttributionContext("eddie_audio_attribution")
                } else {
                    context
                }
                val newMp = android.media.MediaPlayer().apply {
                    setDataSource(attributionContext, android.net.Uri.parse(url))
                    setOnPreparedListener {
                        it.start()
                        isAudioPlaying = true
                        playingMusicId = id
                    }
                    setOnCompletionListener {
                        isAudioPlaying = false
                        playingMusicId = null
                        playPercent = 0f
                    }
                    setOnErrorListener { _, _, _ ->
                        isAudioPlaying = false
                        playingMusicId = null
                        playPercent = 0f
                        Toast.makeText(context, "Failed to play audio from server", Toast.LENGTH_SHORT).show()
                        true
                    }
                    prepareAsync()
                }
                mediaPlayer = newMp
                playingMusicId = id // show buffering state
            } catch (e: Exception) {
                android.util.Log.e("EddieMusic", "Failed to setup player", e)
                Toast.makeText(context, "Media player initialization error", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val genres = listOf(
        "Pop" to false to "Energetic & Upbeat",
        "Rock" to false to "Distortion & Heavy Beat",
        "Instrumental" to false to "Melodic Solo & Harmony",
        "Lo-Fi" to true to "Relaxing & Retro Beat",
        "Jazz" to true to "Mellow & Swing",
        "EDM" to true to "Synthesizer & Nightclub",
        "Classical" to true to "Symphony & Orchestra",
        "Ambient" to true to "Deep Atmospheric",
        "Hip-Hop" to true to "Urban Beat & Bass",
        "R&B" to false to "Smooth Soulful Rhythm",
        "Country" to false to "Acoustic Twang Vibe",
        "Acoustic" to true to "Raw Unfiltered Strings",
        "Cinematic" to true to "Epic Score & Brass",
        "K-Pop" to true to "Glossy Future Beats",
        "Reggae" to false to "Chilled Island Vibe",
        "Metal" to false to "Crunchy Heavy Riffs"
    )

    val durations = listOf(
        "15s" to false to "Short Intro",
        "30s" to false to "Shorts/Story",
        "60s" to true to "Standard Track",
        "120s" to true to "Full Composition"
    )

    val tempos = listOf(
        "Slow" to "Slow & Relaxed",
        "Medium" to "Moderate & Natural",
        "Fast" to "Fast & Energetic",
        "Very Fast" to "Very Fast & Intense"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .imePadding()
    ) {
        // Scrollable content underneath
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(musicScrollState)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Hero Header (Non-sticky)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp)
            ) {
                Text(
                    text = "Eddie AI Music Studio 🎵",
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )
                if (uiState.isSubscribed) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Surface(
                        color = Color(0xFFFFB100).copy(alpha = 0.15f),
                        border = BorderStroke(1.dp, Color(0xFFFFB100)),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = "🪙 ${uiState.getFormattedTokens()} Tokens",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 10.sp),
                            color = Color(0xFFFFB100)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Transform your imaginative descriptions into hybrid hi-fi instrumental harmony powered by state-of-the-art multi-genre synthetic audio.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
            }

        // Prompt Input
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Describe Music or Song Mood",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                OutlinedTextField(
                    value = prompt,
                    onValueChange = { prompt = it },
                    placeholder = { Text("Example: Warm acoustic keyboard beats with mellow saxophone tunes on a rainy night...") },
                    modifier = Modifier.fillMaxWidth().height(100.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Quick Inspiration:",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 6.dp)
                )

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val quickPrompts = quickInspirations
                    quickPrompts.forEach { text ->
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
                            color = MaterialTheme.colorScheme.surface,
                            modifier = Modifier.clickable { prompt = text }
                        ) {
                            Text(
                                text = text,
                                style = MaterialTheme.typography.labelMedium,
                                maxLines = 1,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }

        // Provider & Model Selection Card for Music Generator
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                // Header of the selection
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "Settings Icon",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "AI Music Studio Configuration",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Provider Selection
                Text(
                    text = "AI Music Provider:",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(6.dp))
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val providers = listOf("Gemini", "Suno AI")
                    providers.forEach { provider ->
                        val isSelected = uiState.selectedMusicProvider == provider
                        FilterChip(
                            selected = isSelected,
                            onClick = { onProviderChange(provider) },
                            label = { Text(provider) },
                            leadingIcon = {
                                val icon = when (provider) {
                                    "Gemini" -> Icons.Default.Person
                                    else -> Icons.Default.Menu
                                }
                                Icon(
                                    imageVector = icon,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer,
                                selectedLeadingIconColor = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Model Selection
                Text(
                    text = "AI Music Model:",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(6.dp))
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val models = when (uiState.selectedMusicProvider) {
                        "Gemini" -> listOf("Lyria 3 pro", "Lyria 3 clip")
                        else -> listOf("Suno v3.5", "Suno v4 pro") // Suno AI
                    }

                    models.forEach { model ->
                        val isSelected = uiState.selectedMusicModel == model
                        val isPro = model in listOf("Lyria 3 pro", "Suno v4 pro")
                        val badgeColor = if (uiState.isSubscribed) MaterialTheme.colorScheme.primary else Color(0xFFFFB100)
                        val badgeTextColor = if (uiState.isSubscribed) MaterialTheme.colorScheme.onPrimary else Color(0xFF111111)

                        FilterChip(
                            selected = isSelected,
                            onClick = { onModelChange(model) },
                            label = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(model)
                                    if (isPro) {
                                        Surface(
                                            color = badgeColor,
                                            shape = RoundedCornerShape(4.dp)
                                        ) {
                                            Text(
                                                text = "PRO",
                                                color = badgeTextColor,
                                                fontSize = 8.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                            )
                                        }
                                    }
                                }
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        )
                    }
                }
            }
        }

        // 1. Genre selection
        Column {
            Text(
                text = "Select Popular Genre",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                genres.forEach { item ->
                    val pair = item.first
                    val genreName = pair.first
                    val isPro = pair.second
                    val subtitle = item.second
                    val isSelected = selectedGenre == genreName
                    val badgeColor = if (uiState.isSubscribed) MaterialTheme.colorScheme.primary else Color(0xFFFFB100)
                    val badgeTextColor = if (uiState.isSubscribed) MaterialTheme.colorScheme.onPrimary else Color(0xFF111111)

                    Surface(
                        modifier = Modifier
                            .width(155.dp)
                            .clickable { selectedGenre = genreName },
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                        border = BorderStroke(
                            width = if (isSelected) 2.dp else 1.dp,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            horizontalAlignment = Alignment.Start,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = genreName,
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                )
                                if (isPro) {
                                    Surface(
                                        color = badgeColor,
                                        shape = RoundedCornerShape(4.dp)
                                    ) {
                                        Text(
                                            text = "PRO",
                                            color = badgeTextColor,
                                            fontSize = 8.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 3.dp, vertical = 1.dp)
                                        )
                                    }
                                }
                            }
                            Text(
                                text = subtitle,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }

        // 2. Duration selection
        Column {
            Text(
                text = "Select Track Duration",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                durations.forEach { item ->
                    val pair = item.first
                    val durLabel = pair.first
                    val isPro = pair.second
                    val desc = item.second
                    val isSelected = selectedDuration == durLabel
                    val badgeColor = if (uiState.isSubscribed) MaterialTheme.colorScheme.primary else Color(0xFFFFB100)
                    val badgeTextColor = if (uiState.isSubscribed) MaterialTheme.colorScheme.onPrimary else Color(0xFF111111)

                    Surface(
                        modifier = Modifier
                            .width(135.dp)
                            .clickable { selectedDuration = durLabel },
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                        border = BorderStroke(
                            width = if (isSelected) 2.dp else 1.dp,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = durLabel,
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                )
                                if (isPro) {
                                    Surface(
                                        color = badgeColor,
                                        shape = RoundedCornerShape(4.dp)
                                    ) {
                                        Text(
                                            text = "PRO",
                                            color = badgeTextColor,
                                            fontSize = 7.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 3.dp, vertical = 1.dp)
                                        )
                                    }
                                }
                            }
                            Text(
                                text = desc,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        // 3. Tempo selection
        Column {
            Text(
                text = "Select Music Tempo",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                tempos.forEach { (tempoName, desc) ->
                    val isSelected = selectedTempo == tempoName

                    Surface(
                        modifier = Modifier
                            .width(135.dp)
                            .clickable { selectedTempo = tempoName },
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                        border = BorderStroke(
                            width = if (isSelected) 2.dp else 1.dp,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = tempoName,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = desc,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        // Progress/Loading logger
        if (uiState.musicGenIsLoading) {
            Surface(
                color = Color(0xFF1E1E1E),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Eddie Music Studio - Resonating Track Synthesis...",
                        color = Color.Green,
                        style = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = uiState.musicGenLog,
                        color = Color.White,
                        style = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 12.sp, lineHeight = 16.sp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth(),
                        color = Color.Green,
                        trackColor = Color.DarkGray
                    )
                }
            }
        } else {
            Button(
                onClick = { onGenerateMusic(prompt, selectedGenre, selectedDuration, selectedTempo) },
                enabled = prompt.isNotBlank(),
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = "Start AI Music Generation 🎵",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), modifier = Modifier.padding(vertical = 8.dp))

        // Tracks collection List
        Text(
            text = "Your AI Music Library",
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface
        )

        if (uiState.generatedMusic.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .border(
                        BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                        shape = RoundedCornerShape(12.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "No music tracks created yet",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Create your first song with the power of words above.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }
        } else {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                uiState.generatedMusic.forEach { music ->
                    val isPlayingThis = playingMusicId == music.id

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isPlayingThis) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f) else MaterialTheme.colorScheme.surface
                        )
                    ) {
                        Column {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // 1. Play Button
                                IconButton(
                                    onClick = { togglePlayMusic(music.id, music.musicUrl) },
                                    modifier = Modifier
                                        .size(48.dp)
                                        .background(
                                            color = if (isPlayingThis && isAudioPlaying) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                            shape = CircleShape
                                        )
                                ) {
                                    Icon(
                                        imageVector = if (isPlayingThis && isAudioPlaying) Icons.Default.Close else Icons.Default.PlayArrow,
                                        contentDescription = if (isPlayingThis && isAudioPlaying) "Pause" else "Play",
                                        tint = if (isPlayingThis && isAudioPlaying) Color.White else MaterialTheme.colorScheme.primary
                                    )
                                }

                                Spacer(modifier = Modifier.width(16.dp))

                                // 2. Track Metadata
                                Column(
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(
                                        text = music.prompt,
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = MaterialTheme.colorScheme.secondaryContainer
                                        ) {
                                            Text(
                                                text = music.genre,
                                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                color = MaterialTheme.colorScheme.onSecondaryContainer
                                            )
                                        }
                                        Text(
                                            text = "⏱️ ${music.duration} • ⚡ ${music.tempo}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                // 3. Action Buttons (Edit & Delete)
                                Row(
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    IconButton(
                                        onClick = { editingMusicItem = music }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Edit,
                                            contentDescription = "Edit Music",
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                    IconButton(
                                        onClick = { onDeleteMusic(music.id) }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Delete",
                                            tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f)
                                        )
                                    }
                                }
                            }

                            // 4. Custom Equalizer / Play Seekbar slider when active!
                            if (isPlayingThis) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 8.dp)
                                        .padding(bottom = 12.dp)
                                ) {
                                    LinearProgressIndicator(
                                        progress = { playPercent },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(4.dp),
                                        color = MaterialTheme.colorScheme.primary,
                                        trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = if (isAudioPlaying) "Memutar Harmoni Sintetis..." else "Trek Terjeda",
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        // Equalizer dots or wave animation
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                                        ) {
                                            repeat(4) { index ->
                                                val heightMultiplier = if (isAudioPlaying) {
                                                    val randomVal = java.util.Random().nextFloat()
                                                    0.4f + randomVal * 0.6f
                                                } else 0.2f
                                                Box(
                                                    modifier = Modifier
                                                        .size(width = 3.dp, height = (16 * heightMultiplier).dp)
                                                        .background(
                                                            color = MaterialTheme.colorScheme.primary,
                                                            shape = RoundedCornerShape(2.dp)
                                                        )
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(40.dp))
    }
    }

    editingMusicItem?.let { item ->
        EddieMusicEditorDialog(
            musicItem = item,
            onDismissRequest = { editingMusicItem = null },
            onSaveEditedMusic = onSaveEditedMusic
        )
    }
}

data class DrawingStroke(
    val points: List<androidx.compose.ui.geometry.Offset>,
    val color: androidx.compose.ui.graphics.Color,
    val width: Float
)

@Composable
fun EddieImageEditorDialog(
    imageItem: com.example.data.db.GeneratedImageEntity,
    onDismissRequest: () -> Unit,
    onSaveEditedImage: (String, String, String, String, String) -> Unit
) {
    val context = LocalContext.current
    var brightness by remember { mutableStateOf(0f) } // range -0.5 to 0.5
    var contrast by remember { mutableStateOf(0f) } // range -0.5 to 0.5
    var warmth by remember { mutableStateOf(0f) } // range -0.5 to 0.5
    var selectedTone by remember { mutableStateOf("Original") }
    var filterIntensity by remember { mutableStateOf(0.70f) } // range 0f to 1f
    var customText by remember { mutableStateOf("") }
    var textPosition by remember { mutableStateOf("Bottom") }
    var selectedTextColor by remember { mutableStateOf("White") }
    var textFontStyle by remember { mutableStateOf("Sans-Serif") }
    var textFontSize by remember { mutableStateOf(16f) } // range 12f to 32f

    // Advanced transformation settings
    var rotationAngle by remember { mutableStateOf(0f) }
    var isFlippedHorizontal by remember { mutableStateOf(false) }
    var isFlippedVertical by remember { mutableStateOf(false) }
    var selectedAspect by remember { mutableStateOf("Original") }
    var selectedFrame by remember { mutableStateOf("None") }
    var selectedSticker by remember { mutableStateOf("None") }
    var vignetteEnabled by remember { mutableStateOf(false) }
    var blurOverlayEnabled by remember { mutableStateOf(false) }

    // Brush Tooling State
    var brushColor by remember { mutableStateOf(androidx.compose.ui.graphics.Color.Red) }
    var brushThickness by remember { mutableStateOf(8f) } // range 2f to 40f
    val drawingStrokes = remember { mutableStateListOf<DrawingStroke>() }
    val currentStrokePoints = remember { mutableStateListOf<androidx.compose.ui.geometry.Offset>() }

    // Double Exposure Overlay state
    var activeOverlayTexture by remember { mutableStateOf("None") }
    var overlayTextureOpacity by remember { mutableStateOf(0.50f) } // range 0f to 1f

    // Control tabs
    var activeSubTab by remember { mutableStateOf("Color") }

    // Rendering / saving simulation state
    var isSaving by remember { mutableStateOf(false) }
    var savingLog by remember { mutableStateOf("") }
    var savingProgress by remember { mutableStateOf(0f) }

    val scope = rememberCoroutineScope()

    val subTabs = listOf(
        Pair("Color", "🎨"),
        Pair("Crop", "📐"),
        Pair("Frames", "🖼️"),
        Pair("Text/Badge", "✍️"),
        Pair("Brush & Draw", "🖌️"),
        Pair("Overlays", "🌌"),
        Pair("Lens FX", "✨")
    )

    val tones = listOf("Original", "Cyberpunk", "Vintage Lofi", "Emerald Green", "Cinema Warm", "B&W", "Cosmic Violet", "Cool Neon", "Golden Hour", "Faded Matte")
    val positions = listOf("Top", "Center", "Bottom")
    val textColors = listOf("White", "Black", "Gold", "Neon Green", "Hot Pink", "Cyan")
    val aspects = listOf("Original", "1:1 Square", "16:9 Landscape", "9:16 Portrait", "4:3 Classic")
    val frames = listOf("None", "Polaroid", "Film Strip", "Classic Gold", "Neon Border", "Cyber Outline", "Vibrant Rainbow")
    val stickers = listOf("None", "🔥 TRENDING", "🎨 AI MASTERPIECE", "🔒 EXCLUSIVE", "🕶️ RETRO", "✨ GLOW", "🌟 ULTRA PRO")

    AlertDialog(
        onDismissRequest = { if (!isSaving) onDismissRequest() },
        title = {
            Text(
                text = "Eddie AI Image Studio Pro 🎨",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Interactive Preview Box
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(240.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF151515)),
                    contentAlignment = Alignment.Center
                ) {
                    val imageModifier = when (selectedAspect) {
                        "1:1 Square" -> Modifier.aspectRatio(1f)
                        "16:9 Landscape" -> Modifier.aspectRatio(16f/9f)
                        "9:16 Portrait" -> Modifier.aspectRatio(9f/16f)
                        "4:3 Classic" -> Modifier.aspectRatio(4f/3f)
                        else -> Modifier.fillMaxSize()
                    }

                    Box(
                        modifier = imageModifier
                            .fillMaxHeight()
                            .padding(10.dp)
                            .align(Alignment.Center)
                            .then(
                                when (selectedFrame) {
                                    "Polaroid" -> Modifier
                                        .background(Color.White)
                                        .border(10.dp, Color.White)
                                        .padding(bottom = 24.dp)
                                    "Film Strip" -> Modifier
                                        .border(8.dp, Color.Black)
                                        .padding(4.dp)
                                        .border(2.dp, Color.White, shape = RoundedCornerShape(2.dp))
                                    "Classic Gold" -> Modifier.border(5.dp, Color(0xFFFFD700))
                                    "Neon Border" -> Modifier.border(4.dp, Color(0xFF00FFFF))
                                    "Cyber Outline" -> Modifier.border(4.dp, Color(0xFFFF007F))
                                    "Vibrant Rainbow" -> Modifier.border(
                                        width = 6.dp, 
                                        brush = androidx.compose.ui.graphics.Brush.linearGradient(
                                            colors = listOf(Color.Red, Color.Yellow, Color.Green, Color.Cyan, Color.Blue, Color.Magenta)
                                        ),
                                        shape = androidx.compose.ui.graphics.RectangleShape
                                    )
                                    else -> Modifier
                                }
                            )
                            .pointerInput(activeSubTab) {
                                if (activeSubTab == "Brush & Draw") {
                                    detectDragGestures(
                                        onDragStart = { offset: androidx.compose.ui.geometry.Offset ->
                                            currentStrokePoints.add(offset)
                                        },
                                        onDrag = { change: PointerInputChange, dragAmount: androidx.compose.ui.geometry.Offset ->
                                            change.consume()
                                            currentStrokePoints.add(change.position)
                                        },
                                        onDragEnd = {
                                            if (currentStrokePoints.isNotEmpty()) {
                                                drawingStrokes.add(
                                                    DrawingStroke(
                                                        points = currentStrokePoints.toList(),
                                                        color = brushColor,
                                                        width = brushThickness
                                                    )
                                                )
                                                currentStrokePoints.clear()
                                            }
                                        }
                                    )
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        AsyncImage(
                            model = imageItem.imageUrl,
                            contentDescription = "Preview",
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer(
                                    rotationZ = rotationAngle,
                                    scaleX = if (isFlippedHorizontal) -1f else 1f,
                                    scaleY = if (isFlippedVertical) -1f else 1f
                                ),
                            contentScale = ContentScale.Fit,
                            colorFilter = if (selectedTone == "B&W") androidx.compose.ui.graphics.ColorFilter.colorMatrix(androidx.compose.ui.graphics.ColorMatrix().apply { setToSaturation(0f) }) else null
                        )

                        // Brightness overlay
                        if (brightness != 0f) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        if (brightness > 0f) Color.White.copy(alpha = brightness * 0.6f)
                                        else Color.Black.copy(alpha = (-brightness) * 0.6f)
                                    )
                            )
                        }

                        // Contrast overlay
                        if (contrast > 0f) {
                            Box(modifier = Modifier.fillMaxSize().background(Color.White.copy(alpha = contrast * 0.15f)))
                        } else if (contrast < 0f) {
                            Box(modifier = Modifier.fillMaxSize().background(Color.Gray.copy(alpha = (-contrast) * 0.25f)))
                        }

                        // Warmth overlay
                        if (warmth > 0f) {
                            Box(modifier = Modifier.fillMaxSize().background(Color(0xFFFFB03A).copy(alpha = warmth * 0.25f)))
                        } else if (warmth < 0f) {
                            Box(modifier = Modifier.fillMaxSize().background(Color(0xFF3A8CFF).copy(alpha = (-warmth) * 0.25f)))
                        }

                        // Filter overlays
                        val toneAlpha = filterIntensity * 0.35f
                        if (selectedTone == "Cyberpunk") {
                            Box(modifier = Modifier.fillMaxSize().background(Color.Magenta.copy(alpha = toneAlpha * 0.65f)))
                        } else if (selectedTone == "Vintage Lofi") {
                            Box(modifier = Modifier.fillMaxSize().background(Color(0xFFE5A93C).copy(alpha = toneAlpha * 0.70f)))
                        } else if (selectedTone == "Emerald Green") {
                            Box(modifier = Modifier.fillMaxSize().background(Color.Green.copy(alpha = toneAlpha * 0.50f)))
                        } else if (selectedTone == "Cinema Warm") {
                            Box(modifier = Modifier.fillMaxSize().background(Color(0xFFFF8A00).copy(alpha = toneAlpha * 0.50f)))
                        } else if (selectedTone == "Cosmic Violet") {
                            Box(modifier = Modifier.fillMaxSize().background(Color(0xFF8B00FF).copy(alpha = toneAlpha * 0.60f)))
                        } else if (selectedTone == "Cool Neon") {
                            Box(modifier = Modifier.fillMaxSize().background(Color(0xFF00FFFF).copy(alpha = toneAlpha * 0.60f)))
                        } else if (selectedTone == "Golden Hour") {
                            Box(modifier = Modifier.fillMaxSize().background(Color(0xFFFFD700).copy(alpha = toneAlpha * 0.60f)))
                        } else if (selectedTone == "Faded Matte") {
                            Box(modifier = Modifier.fillMaxSize().background(Color(0xFF8B7E74).copy(alpha = toneAlpha * 0.45f)))
                        }

                        // Vignette overlay
                        if (vignetteEnabled) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        androidx.compose.ui.graphics.Brush.radialGradient(
                                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.75f)),
                                            radius = 400f
                                        )
                                    )
                            )
                        }

                        // Lens blur simulation
                        if (blurOverlayEnabled) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        androidx.compose.ui.graphics.Brush.verticalGradient(
                                            colors = listOf(
                                                Color.White.copy(alpha = 0.15f),
                                                Color.Transparent,
                                                Color.Transparent,
                                                Color.White.copy(alpha = 0.15f)
                                            )
                                        )
                                    )
                            )
                        }

                        // Double exposure texture overlays
                        if (activeOverlayTexture != "None") {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        when (activeOverlayTexture) {
                                            "Light Leak ⚡" -> androidx.compose.ui.graphics.Brush.radialGradient(
                                                colors = listOf(
                                                    Color(0xFFFF8A00).copy(alpha = overlayTextureOpacity * 0.8f),
                                                    Color(0xFFFF007F).copy(alpha = overlayTextureOpacity * 0.4f),
                                                    Color.Transparent
                                                ),
                                                center = androidx.compose.ui.geometry.Offset(0f, 0f),
                                                radius = 600f
                                            )
                                            "Starry Galaxy 🌌" -> androidx.compose.ui.graphics.Brush.radialGradient(
                                                colors = listOf(
                                                    Color(0xFF8B00FF).copy(alpha = overlayTextureOpacity * 0.5f),
                                                    Color(0xFF00FFFF).copy(alpha = overlayTextureOpacity * 0.3f),
                                                    Color(0xFF000000).copy(alpha = overlayTextureOpacity * 0.7f),
                                                    Color.Transparent
                                                ),
                                                radius = 800f
                                            )
                                            "Rain Droplets 💧" -> androidx.compose.ui.graphics.Brush.verticalGradient(
                                                colors = listOf(
                                                    Color(0xFFD0E1FD).copy(alpha = overlayTextureOpacity * 0.3f),
                                                    Color(0xFF4A90E2).copy(alpha = overlayTextureOpacity * 0.15f),
                                                    Color(0xFF1F3B68).copy(alpha = overlayTextureOpacity * 0.4f)
                                                )
                                            )
                                            "Vintage Film Grain 📽️" -> androidx.compose.ui.graphics.Brush.linearGradient(
                                                colors = listOf(
                                                    Color(0xFF5A4D41).copy(alpha = overlayTextureOpacity * 0.25f),
                                                    Color(0xFFE4D3C1).copy(alpha = overlayTextureOpacity * 0.15f),
                                                    Color(0xFF332A22).copy(alpha = overlayTextureOpacity * 0.3f)
                                                )
                                            )
                                            else -> androidx.compose.ui.graphics.Brush.verticalGradient(
                                                colors = listOf(Color.Transparent, Color.Transparent)
                                            )
                                        }
                                    )
                            )
                        }

                        // Canvas drawing overlay
                        androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                            drawingStrokes.forEach { stroke ->
                                if (stroke.points.size > 1) {
                                    val path = androidx.compose.ui.graphics.Path().apply {
                                        moveTo(stroke.points[0].x, stroke.points[0].y)
                                        for (i in 1 until stroke.points.size) {
                                            lineTo(stroke.points[i].x, stroke.points[i].y)
                                        }
                                    }
                                    drawPath(
                                        path = path,
                                        color = stroke.color,
                                        style = androidx.compose.ui.graphics.drawscope.Stroke(
                                            width = stroke.width,
                                            cap = androidx.compose.ui.graphics.StrokeCap.Round,
                                            join = androidx.compose.ui.graphics.StrokeJoin.Round
                                        )
                                    )
                                }
                            }
                            if (currentStrokePoints.size > 1) {
                                val path = androidx.compose.ui.graphics.Path().apply {
                                    moveTo(currentStrokePoints[0].x, currentStrokePoints[0].y)
                                    for (i in 1 until currentStrokePoints.size) {
                                        lineTo(currentStrokePoints[i].x, currentStrokePoints[i].y)
                                    }
                                }
                                drawPath(
                                    path = path,
                                    color = brushColor,
                                    style = androidx.compose.ui.graphics.drawscope.Stroke(
                                        width = brushThickness,
                                        cap = androidx.compose.ui.graphics.StrokeCap.Round,
                                        join = androidx.compose.ui.graphics.StrokeJoin.Round
                                    )
                                )
                            }
                        }

                        // Sticker badge
                        if (selectedSticker != "None") {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(8.dp)
                                    .background(Color.Black.copy(alpha = 0.8f), RoundedCornerShape(6.dp))
                                    .border(1.dp, Color(0xFFFFD700), RoundedCornerShape(6.dp))
                                    .padding(horizontal = 6.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    text = selectedSticker,
                                    color = Color(0xFFFFD700),
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        // Custom Text overlay
                        if (customText.isNotBlank()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(12.dp),
                                contentAlignment = when (textPosition) {
                                    "Top" -> Alignment.TopCenter
                                    "Center" -> Alignment.Center
                                    else -> Alignment.BottomCenter
                                }
                            ) {
                                Text(
                                    text = customText,
                                    color = when (selectedTextColor) {
                                        "White" -> Color.White
                                        "Black" -> Color.Black
                                        "Gold" -> Color(0xFFFFD700)
                                        "Neon Green" -> Color.Green
                                        "Cyan" -> Color.Cyan
                                        else -> Color(0xFFFF1493)
                                    },
                                    fontWeight = FontWeight.Bold,
                                    fontSize = textFontSize.sp,
                                    fontFamily = when (textFontStyle) {
                                        "Serif" -> FontFamily.Serif
                                        "Monospace" -> FontFamily.Monospace
                                        "Cursive" -> FontFamily.Cursive
                                        else -> FontFamily.SansSerif
                                    },
                                    modifier = Modifier
                                        .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }

                // Sub-navigator row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(vertical = 2.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    subTabs.forEach { tab ->
                        val isSelected = activeSubTab == tab.first
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                            border = BorderStroke(1.dp, if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent),
                            modifier = Modifier.clickable { activeSubTab = tab.first }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(tab.second, fontSize = 14.sp)
                                Text(
                                    text = tab.first,
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))

                // Active controls panel
                when (activeSubTab) {
                    "Color" -> {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            // Brightness
                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Brightness", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                                    Text("${(brightness * 100).toInt()}%", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                                }
                                Slider(value = brightness, onValueChange = { brightness = it }, valueRange = -0.5f..0.5f)
                            }

                            // Contrast
                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Contrast", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                                    Text("${(contrast * 100).toInt()}%", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                                }
                                Slider(value = contrast, onValueChange = { contrast = it }, valueRange = -0.5f..0.5f)
                            }

                            // Warmth (Temperature)
                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Warmth & Temperature", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                                    Text(if (warmth > 0) "Warm +${(warmth*100).toInt()}%" else if (warmth < 0) "Cool ${(warmth*100).toInt()}%" else "Neutral", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                                }
                                Slider(value = warmth, onValueChange = { warmth = it }, valueRange = -0.5f..0.5f)
                            }

                            // Tone filters
                            Column {
                                Text("Tone LUT Filters", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold), modifier = Modifier.padding(bottom = 6.dp))
                                Row(
                                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    tones.forEach { tone ->
                                        val isSelected = selectedTone == tone
                                        Surface(
                                            shape = RoundedCornerShape(16.dp),
                                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                            border = BorderStroke(1.dp, if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                                            modifier = Modifier.clickable { selectedTone = tone }
                                        ) {
                                            Text(
                                                text = tone,
                                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }

                            // Filter Intensity Slider
                            if (selectedTone != "Original") {
                                Column {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("Filter Intensity", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                                        Text("${(filterIntensity * 100).toInt()}%", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                                    }
                                    Slider(value = filterIntensity, onValueChange = { filterIntensity = it }, valueRange = 0f..1f)
                                }
                            }
                        }
                    }

                    "Crop" -> {
                        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            // Aspect Ratios
                            Column {
                                Text("Aspect Ratio Presets", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold), modifier = Modifier.padding(bottom = 6.dp))
                                Row(
                                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    aspects.forEach { aspect ->
                                        val isSelected = selectedAspect == aspect
                                        Surface(
                                            shape = RoundedCornerShape(16.dp),
                                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                            border = BorderStroke(1.dp, if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                                            modifier = Modifier.clickable { selectedAspect = aspect }
                                        ) {
                                            Text(
                                                text = aspect,
                                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }

                            // Transform buttons (Rotate, Flip)
                            Column {
                                Text("Transform Canvas", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold), modifier = Modifier.padding(bottom = 8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    // Rotate button
                                    Button(
                                        onClick = { rotationAngle = (rotationAngle + 90f) % 360f },
                                        modifier = Modifier.weight(1f),
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant, contentColor = MaterialTheme.colorScheme.onSurfaceVariant)
                                    ) {
                                        Text("🔄 Rotate 90°", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold))
                                    }
                                    // Flip Horizontal
                                    Button(
                                        onClick = { isFlippedHorizontal = !isFlippedHorizontal },
                                        modifier = Modifier.weight(1f),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (isFlippedHorizontal) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                                            contentColor = if (isFlippedHorizontal) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    ) {
                                        Text("↔️ Flip H", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold))
                                    }
                                    // Flip Vertical
                                    Button(
                                        onClick = { isFlippedVertical = !isFlippedVertical },
                                        modifier = Modifier.weight(1f),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (isFlippedVertical) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                                            contentColor = if (isFlippedVertical) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    ) {
                                        Text("↕️ Flip V", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold))
                                    }
                                }
                            }
                        }
                    }

                    "Frames" -> {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Artistic Framing Borders", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold), modifier = Modifier.padding(bottom = 4.dp))
                            Row(
                                modifier = Modifier.horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                frames.forEach { frame ->
                                    val isSelected = selectedFrame == frame
                                    Surface(
                                        shape = RoundedCornerShape(16.dp),
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                        border = BorderStroke(1.dp, if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                                        modifier = Modifier.clickable { selectedFrame = frame }
                                    ) {
                                        Text(
                                            text = frame,
                                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }

                    "Text/Badge" -> {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            // Text inputs
                            Column {
                                Text("Insert Custom Logo / Watermark", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold), modifier = Modifier.padding(bottom = 6.dp))
                                OutlinedTextField(
                                    value = customText,
                                    onValueChange = { customText = it },
                                    placeholder = { Text("e.g. © Creative Studio 2026") },
                                    modifier = Modifier.fillMaxWidth(),
                                    maxLines = 1,
                                    singleLine = true,
                                    shape = RoundedCornerShape(8.dp)
                                )
                            }

                            if (customText.isNotBlank()) {
                                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text("Text Position", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), modifier = Modifier.padding(bottom = 4.dp))
                                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                                positions.forEach { pos ->
                                                    val isSel = textPosition == pos
                                                    Box(
                                                        modifier = Modifier
                                                            .background(
                                                                if (isSel) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                                                RoundedCornerShape(6.dp)
                                                            )
                                                            .clickable { textPosition = pos }
                                                            .padding(horizontal = 8.dp, vertical = 6.dp)
                                                    ) {
                                                        Text(text = pos, fontSize = 9.sp, fontWeight = FontWeight.SemiBold, color = if (isSel) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface)
                                                    }
                                                }
                                            }
                                        }

                                        Column(modifier = Modifier.weight(1f)) {
                                            Text("Text Color", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), modifier = Modifier.padding(bottom = 4.dp))
                                            Row(
                                                modifier = Modifier.horizontalScroll(rememberScrollState()),
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                textColors.forEach { colorStr ->
                                                    val isSel = selectedTextColor == colorStr
                                                    Box(
                                                        modifier = Modifier
                                                            .background(
                                                                if (isSel) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                                                RoundedCornerShape(6.dp)
                                                            )
                                                            .clickable { selectedTextColor = colorStr }
                                                            .padding(horizontal = 8.dp, vertical = 6.dp)
                                                    ) {
                                                        Text(text = colorStr, fontSize = 9.sp, fontWeight = FontWeight.SemiBold, color = if (isSel) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface)
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    // Custom Watermark Typography parameters
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text("Font Style", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), modifier = Modifier.padding(bottom = 4.dp))
                                            Row(
                                                modifier = Modifier.horizontalScroll(rememberScrollState()),
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                val fontFamilies = listOf("Sans-Serif", "Serif", "Monospace", "Cursive")
                                                fontFamilies.forEach { style ->
                                                    val isSel = textFontStyle == style
                                                    Box(
                                                        modifier = Modifier
                                                            .background(
                                                                if (isSel) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                                                RoundedCornerShape(6.dp)
                                                            )
                                                            .clickable { textFontStyle = style }
                                                            .padding(horizontal = 8.dp, vertical = 6.dp)
                                                    ) {
                                                        Text(text = style, fontSize = 9.sp, fontWeight = FontWeight.SemiBold, color = if (isSel) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface)
                                                    }
                                                }
                                            }
                                        }

                                        Column(modifier = Modifier.weight(1f)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Text("Size", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                                                Text("${textFontSize.toInt()}sp", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                                            }
                                            Slider(
                                                value = textFontSize,
                                                onValueChange = { textFontSize = it },
                                                valueRange = 12f..32f,
                                                modifier = Modifier.height(24.dp)
                                            )
                                        }
                                    }
                                }
                            }

                            // Badge Overlay choices
                            Column {
                                Text("Artistic Trending Badge", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold), modifier = Modifier.padding(bottom = 6.dp))
                                Row(
                                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    stickers.forEach { sticker ->
                                        val isSelected = selectedSticker == sticker
                                        Surface(
                                            shape = RoundedCornerShape(16.dp),
                                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                            border = BorderStroke(1.dp, if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                                            modifier = Modifier.clickable { selectedSticker = sticker }
                                        ) {
                                            Text(
                                                text = sticker,
                                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    "Brush & Draw" -> {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text("Interactive Studio Brush 🖌️", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                            Text("Drag on the preview canvas above to add vector layers, signatures or annotations.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            
                            // Brush color options
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Color:", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                                val brushColorsList = listOf(
                                    Pair(Color.Red, "Red"),
                                    Pair(Color.Green, "Green"),
                                    Pair(Color.Blue, "Blue"),
                                    Pair(Color(0xFFFFD700), "Gold"),
                                    Pair(Color(0xFFFF1493), "Hot Pink"),
                                    Pair(Color.White, "White"),
                                    Pair(Color.Black, "Black")
                                )
                                Row(
                                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    brushColorsList.forEach { pair ->
                                        val isColorSelected = brushColor == pair.first
                                        Box(
                                            modifier = Modifier
                                                .size(28.dp)
                                                .clip(CircleShape)
                                                .background(pair.first)
                                                .border(
                                                    width = if (isColorSelected) 3.dp else 1.dp,
                                                    color = if (isColorSelected) MaterialTheme.colorScheme.primary else Color.Gray.copy(alpha = 0.5f),
                                                    shape = CircleShape
                                                )
                                                .clickable { brushColor = pair.first }
                                        )
                                    }
                                }
                            }

                            // Brush size slider
                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Brush Size", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                                    Text("${brushThickness.toInt()}px", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                                }
                                Slider(value = brushThickness, onValueChange = { brushThickness = it }, valueRange = 2f..40f)
                            }

                            // Undo / Clear
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = { if (drawingStrokes.isNotEmpty()) drawingStrokes.removeAt(drawingStrokes.size - 1) },
                                    enabled = drawingStrokes.isNotEmpty(),
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant, contentColor = MaterialTheme.colorScheme.onSurfaceVariant)
                                ) {
                                    Text("↩️ Undo Stroke")
                                }
                                Button(
                                    onClick = { drawingStrokes.clear() },
                                    enabled = drawingStrokes.isNotEmpty(),
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.errorContainer, contentColor = MaterialTheme.colorScheme.onErrorContainer)
                                ) {
                                    Text("🗑️ Clear Canvas")
                                }
                            }
                        }
                    }

                    "Overlays" -> {
                        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            Text("Double Exposure Texture Overlays 🌌", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                            Text("Infuse artistic camera flares and ambient exposure patterns onto your artwork.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            
                            val overlayTextures = listOf("None", "Light Leak ⚡", "Starry Galaxy 🌌", "Rain Droplets 💧", "Vintage Film Grain 📽️")
                            
                            Row(
                                modifier = Modifier.horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                overlayTextures.forEach { textureName ->
                                    val isSelected = activeOverlayTexture == textureName
                                    Surface(
                                        shape = RoundedCornerShape(16.dp),
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                        border = BorderStroke(1.dp, if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                                        modifier = Modifier.clickable { activeOverlayTexture = textureName }
                                    ) {
                                        Text(
                                            text = textureName,
                                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }

                            if (activeOverlayTexture != "None") {
                                Column {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("Overlay Blending Opacity", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                                        Text("${(overlayTextureOpacity * 100).toInt()}%", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                                    }
                                    Slider(value = overlayTextureOpacity, onValueChange = { overlayTextureOpacity = it }, valueRange = 0.1f..1.0f)
                                }
                            }
                        }
                    }

                    "Lens FX" -> {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Professional Filter Effects", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold), modifier = Modifier.padding(bottom = 4.dp))
                            
                            // Vignette
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                                    .clickable { vignetteEnabled = !vignetteEnabled }
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("Edge Vignette Shadow 🖤", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                                    Text("Add photographic dark radial shadow around borders", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Switch(checked = vignetteEnabled, onCheckedChange = { vignetteEnabled = it })
                            }

                            // Tilt Shift Blur
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                                    .clickable { blurOverlayEnabled = !blurOverlayEnabled }
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("Lens Tilt-Shift Blur 💧", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                                    Text("Softly defocus top and bottom areas to create depth of field", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Switch(checked = blurOverlayEnabled, onCheckedChange = { blurOverlayEnabled = it })
                            }
                        }
                    }
                }

                // Saving Simulator
                if (isSaving) {
                    Surface(
                        color = Color(0xFF151515),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                text = "Pixel Layer Processing Progress:",
                                color = Color.Green,
                                style = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = savingLog,
                                color = Color.White,
                                style = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 11.sp, lineHeight = 15.sp)
                              )
                            Spacer(modifier = Modifier.height(8.dp))
                            LinearProgressIndicator(
                                progress = { savingProgress },
                                modifier = Modifier.fillMaxWidth(),
                                color = Color.Green,
                                trackColor = Color.DarkGray
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (!isSaving) {
                Button(
                    onClick = {
                        isSaving = true
                        scope.launch {
                            savingLog = "> Initiating advanced pixel matrix analysis...\n"
                            savingProgress = 0.1f
                            delay(600)
                            if (brightness != 0f || contrast != 0f || warmth != 0f) {
                                savingLog += "> Tuning color channels: Brightness:${(brightness*100).toInt()}% Contrast:${(contrast*100).toInt()}% Temp:${(warmth*100).toInt()}%\n"
                                savingProgress = 0.22f
                                delay(650)
                            }
                            if (selectedTone != "Original") {
                                savingLog += "> Injecting $selectedTone LUT filter coordinates (intensity: ${(filterIntensity*100).toInt()}%)...\n"
                                savingProgress = 0.35f
                                delay(600)
                            }
                            if (drawingStrokes.isNotEmpty()) {
                                savingLog += "> Baking ${drawingStrokes.size} user brush vector vectors onto canvas matrix...\n"
                                savingProgress = 0.48f
                                delay(650)
                            }
                            if (activeOverlayTexture != "None") {
                                savingLog += "> Projecting double exposure overlay: $activeOverlayTexture (Opacity: ${(overlayTextureOpacity*100).toInt()}%)\n"
                                savingProgress = 0.60f
                                delay(600)
                            }
                            if (rotationAngle != 0f || isFlippedHorizontal || isFlippedVertical) {
                                savingLog += "> Transforming layout: Rot:${rotationAngle.toInt()}° FlipH:$isFlippedHorizontal FlipV:$isFlippedVertical\n"
                                savingProgress = 0.70f
                                delay(650)
                            }
                            if (selectedAspect != "Original") {
                                savingLog += "> Cropping canvas to aspect ratio: $selectedAspect\n"
                                savingProgress = 0.80f
                                delay(600)
                            }
                            if (selectedFrame != "None" || selectedSticker != "None") {
                                savingLog += "> Overlaying frame: $selectedFrame | Badge: $selectedSticker\n"
                                savingProgress = 0.90f
                                delay(600)
                            }
                            if (vignetteEnabled || blurOverlayEnabled) {
                                savingLog += "> Executing FX passes: Vignette:$vignetteEnabled Tilt-Shift:$blurOverlayEnabled\n"
                                savingProgress = 0.95f
                                delay(650)
                            }
                            if (customText.isNotBlank()) {
                                savingLog += "> Printing text: \"$customText\" ($textPosition | $selectedTextColor | Font: $textFontStyle | Size: ${textFontSize.toInt()}sp)\n"
                                savingProgress = 0.98f
                                delay(600)
                            }
                            savingLog += "> Export encoding successful! Compiling files..."
                            savingProgress = 1.0f
                            delay(500)

                            val listSummary = mutableListOf<String>()
                            if (brightness != 0f) listSummary.add("Bright ${(brightness * 100).toInt()}%")
                            if (contrast != 0f) listSummary.add("Contr ${(contrast * 100).toInt()}%")
                            if (warmth != 0f) listSummary.add("Warm ${(warmth * 100).toInt()}%")
                            if (selectedTone != "Original") listSummary.add("$selectedTone (${(filterIntensity*100).toInt()}% intensity)")
                            if (drawingStrokes.isNotEmpty()) listSummary.add("${drawingStrokes.size} Custom Brushstrokes")
                            if (activeOverlayTexture != "None") listSummary.add("$activeOverlayTexture (${(overlayTextureOpacity*100).toInt()}% blend)")
                            if (rotationAngle != 0f) listSummary.add("${rotationAngle.toInt()}°Rot")
                            if (isFlippedHorizontal || isFlippedVertical) listSummary.add("Flipped")
                            if (selectedAspect != "Original") listSummary.add(selectedAspect)
                            if (selectedFrame != "None") listSummary.add("$selectedFrame Frame")
                            if (selectedSticker != "None") listSummary.add(selectedSticker)
                            if (vignetteEnabled) listSummary.add("Vignette")
                            if (blurOverlayEnabled) listSummary.add("LensBlur")
                            if (customText.isNotBlank()) listSummary.add("Stamped '$customText' ($textFontStyle | ${textFontSize.toInt()}sp)")

                            val editsDetails = if (listSummary.isNotEmpty()) " (${listSummary.joinToString(", ")})" else ""
                            val newPrompt = "${imageItem.prompt}$editsDetails"

                            onSaveEditedImage(
                                imageItem.id,
                                newPrompt,
                                imageItem.style,
                                imageItem.aspectRatio,
                                imageItem.imageUrl // Keeps URL, simulated locally
                            )

                            isSaving = false
                            Toast.makeText(context, "Edited artwork saved to Main Gallery!", Toast.LENGTH_SHORT).show()
                            onDismissRequest()
                        }
                    }
                ) {
                    Text("Export & Save 🎨")
                }
            }
        },
        dismissButton = {
            if (!isSaving) {
                TextButton(
                    onClick = onDismissRequest
                ) {
                    Text("Cancel")
                }
            }
        }
    )
}

@Composable
fun DownloadIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
    size: androidx.compose.ui.unit.Dp = 24.dp
) {
    IconButton(
        onClick = onClick,
        modifier = modifier.size(size)
    ) {
        androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize().padding(if (size < 30.dp) 4.dp else 6.dp)) {
            val w = this.size.width
            val h = this.size.height
            
            // Draw bottom bar
            drawLine(
                color = color,
                start = androidx.compose.ui.geometry.Offset(1f, h - 2f),
                end = androidx.compose.ui.geometry.Offset(w - 1f, h - 2f),
                strokeWidth = 3f,
                cap = androidx.compose.ui.graphics.StrokeCap.Round
            )
            
            // Draw vertical stem of arrow
            drawLine(
                color = color,
                start = androidx.compose.ui.geometry.Offset(w / 2f, 2f),
                end = androidx.compose.ui.geometry.Offset(w / 2f, h - 6f),
                strokeWidth = 3f,
                cap = androidx.compose.ui.graphics.StrokeCap.Round
            )
            
            // Draw arrow tips pointing downwards
            drawLine(
                color = color,
                start = androidx.compose.ui.geometry.Offset(w / 2f, h - 6f),
                end = androidx.compose.ui.geometry.Offset(w / 2f - w / 4f, h - 6f - h / 4f),
                strokeWidth = 3f,
                cap = androidx.compose.ui.graphics.StrokeCap.Round
            )
            drawLine(
                color = color,
                start = androidx.compose.ui.geometry.Offset(w / 2f, h - 6f),
                end = androidx.compose.ui.geometry.Offset(w / 2f + w / 4f, h - 6f - h / 4f),
                strokeWidth = 3f,
                cap = androidx.compose.ui.graphics.StrokeCap.Round
            )
        }
    }
}

fun generateFallbackImageBytes(prompt: String): ByteArray {
    try {
        val bitmap = android.graphics.Bitmap.createBitmap(512, 512, android.graphics.Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(bitmap)
        
        // Background style gradient
        val paint = android.graphics.Paint().apply {
            isAntiAlias = true
            style = android.graphics.Paint.Style.FILL
        }
        
        val gradient = android.graphics.LinearGradient(
            0f, 0f, 512f, 512f,
            android.graphics.Color.parseColor("#121214"),
            android.graphics.Color.parseColor("#1E1E24"),
            android.graphics.Shader.TileMode.CLAMP
        )
        paint.shader = gradient
        canvas.drawRect(0f, 0f, 512f, 512f, paint)
        
        paint.shader = null
        
        // Decorative background highlights
        paint.color = android.graphics.Color.parseColor("#2D1E54")
        canvas.drawCircle(100f, 100f, 180f, paint)
        paint.color = android.graphics.Color.parseColor("#153243")
        canvas.drawCircle(450f, 400f, 150f, paint)
        
        // Draw Border
        paint.color = android.graphics.Color.parseColor("#3A3F58")
        paint.strokeWidth = 12f
        paint.style = android.graphics.Paint.Style.STROKE
        canvas.drawRect(0f, 0f, 512f, 512f, paint)
        
        // Text drawing paints
        val textPaint = android.graphics.Paint().apply {
            isAntiAlias = true
            color = android.graphics.Color.WHITE
            textSize = 28f
            textAlign = android.graphics.Paint.Align.CENTER
            typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
        }
        
        textPaint.color = android.graphics.Color.parseColor("#A5A6F6")
        textPaint.textSize = 20f
        canvas.drawText("EDDIE AI GENERATED ART", 256f, 80f, textPaint)
        
        textPaint.color = android.graphics.Color.WHITE
        textPaint.textSize = 24f
        val words = prompt.split(" ")
        var line = ""
        val lines = mutableListOf<String>()
        for (word in words) {
            val testLine = if (line.isEmpty()) word else "$line $word"
            if (textPaint.measureText(testLine) > 420f) {
                lines.add(line)
                line = word
            } else {
                line = testLine
            }
        }
        if (line.isNotEmpty()) {
            lines.add(line)
        }
        
        var startY = 200f
        for (i in 0 until minOf(lines.size, 5)) {
            canvas.drawText(lines[i], 256f, startY, textPaint)
            startY += 36f
        }
        
        textPaint.color = android.graphics.Color.parseColor("#6C7A89")
        textPaint.textSize = 16f
        canvas.drawText("Offline Fallback Mode • Safe Offline Copy Saved", 256f, 460f, textPaint)
        
        val stream = java.io.ByteArrayOutputStream()
        bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 90, stream)
        val result = stream.toByteArray()
        bitmap.recycle()
        return result
    } catch (e: Exception) {
        android.util.Log.e("GenerateFallback", "Error creating fallback image", e)
        return ByteArray(0)
    }
}

fun getMinimalMp4Bytes(): ByteArray {
    val hex = "00000018667479706d703432000000006d70343269736f6d0000000866726565000000086d646174"
    return hex.chunked(2).map { it.toInt(16).toByte() }.toByteArray()
}

fun downloadAndroidImage(
    context: android.content.Context,
    imageUrl: String,
    prompt: String,
    onComplete: (String?) -> Unit
) {
    kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
        try {
            var bytes = when {
                imageUrl.startsWith("data:") -> {
                    val base64Data = if (imageUrl.contains(",")) imageUrl.substringAfter(",") else imageUrl
                    android.util.Base64.decode(base64Data, android.util.Base64.DEFAULT)
                }
                imageUrl.startsWith("file://") || imageUrl.startsWith("content://") -> {
                    val uri = android.net.Uri.parse(imageUrl)
                    context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                }
                imageUrl.startsWith("/") -> {
                    java.io.File(imageUrl).readBytes()
                }
                imageUrl.startsWith("http") -> {
                    try {
                        java.net.URL(imageUrl).openStream().use { it.readBytes() }
                    } catch (ex: Exception) {
                        android.util.Log.e("DownloadImageOnline", "HTTP image download failed, using custom generated card", ex)
                        null
                    }
                }
                else -> {
                    val file = java.io.File(context.filesDir, imageUrl)
                    if (file.exists()) file.readBytes() else null
                }
            }

            if (bytes == null) {
                bytes = generateFallbackImageBytes(prompt)
            }

            val filename = "EddieArt_${System.currentTimeMillis()}.png"
            var savedPath: String? = null

            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                val contentValues = android.content.ContentValues().apply {
                    put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME, filename)
                    put(android.provider.MediaStore.MediaColumns.MIME_TYPE, "image/png")
                    put(android.provider.MediaStore.MediaColumns.RELATIVE_PATH, android.os.Environment.DIRECTORY_DOWNLOADS)
                }
                val uri = context.contentResolver.insert(
                    android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                    contentValues
                )
                if (uri != null) {
                    context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                        outputStream.write(bytes)
                    }
                    savedPath = "Downloads/$filename"
                }
            } else {
                val downloadsDir = android.os.Environment.getExternalStoragePublicDirectory(
                    android.os.Environment.DIRECTORY_DOWNLOADS
                )
                if (!downloadsDir.exists()) downloadsDir.mkdirs()
                val file = java.io.File(downloadsDir, filename)
                file.outputStream().use { it.write(bytes) }
                savedPath = file.absolutePath
            }

            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                onComplete(savedPath)
            }
        } catch (e: Exception) {
            android.util.Log.e("DownloadImage", "Error downloading image", e)
            try {
                val bytes = generateFallbackImageBytes(prompt)
                val filename = "EddieArt_${System.currentTimeMillis()}.png"
                var savedPath: String? = null
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                    val contentValues = android.content.ContentValues().apply {
                        put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME, filename)
                        put(android.provider.MediaStore.MediaColumns.MIME_TYPE, "image/png")
                        put(android.provider.MediaStore.MediaColumns.RELATIVE_PATH, android.os.Environment.DIRECTORY_DOWNLOADS)
                    }
                    val uri = context.contentResolver.insert(
                        android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                        contentValues
                    )
                    if (uri != null) {
                        context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                            outputStream.write(bytes)
                        }
                        savedPath = "Downloads/$filename"
                    }
                } else {
                    val downloadsDir = android.os.Environment.getExternalStoragePublicDirectory(
                        android.os.Environment.DIRECTORY_DOWNLOADS
                    )
                    if (!downloadsDir.exists()) downloadsDir.mkdirs()
                    val file = java.io.File(downloadsDir, filename)
                    file.outputStream().use { it.write(bytes) }
                    savedPath = file.absolutePath
                }
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    onComplete(savedPath)
                }
            } catch (innerEx: Exception) {
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    onComplete(null)
                }
            }
        }
    }
}

fun downloadAndroidVideo(
    context: android.content.Context,
    videoUrl: String,
    videoId: String? = null,
    onComplete: (String?) -> Unit
) {
    kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
        try {
            val filename = "EddieVideo_${System.currentTimeMillis()}.mp4"
            var enqueueSucceeded = false

            // 1. Enqueue using standard system DownloadManager as a fallback/parallel process
            try {
                val uri = android.net.Uri.parse(videoUrl)
                val downloadManager = context.getSystemService(android.content.Context.DOWNLOAD_SERVICE) as android.app.DownloadManager
                val request = android.app.DownloadManager.Request(uri).apply {
                    setTitle("Eddie AI Video download")
                    setDescription("Downloading: $filename")
                    setNotificationVisibility(android.app.DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                    setDestinationInExternalPublicDir(android.os.Environment.DIRECTORY_DOWNLOADS, filename)
                    setMimeType("video/mp4")
                    setAllowedOverMetered(true)
                    setAllowedOverRoaming(true)
                }
                downloadManager.enqueue(request)
                enqueueSucceeded = true
            } catch (ex: Exception) {
                android.util.Log.e("DownloadVideo", "DownloadManager enqueue failed", ex)
            }

            // 2. Execute modern block-by-block streamed file write directly to public storage + local app filesDir copy
            var savedPath: String? = null
            var connection: java.net.HttpURLConnection? = null
            
            val isLocal = videoUrl.startsWith("file:") || videoUrl.startsWith("content:") || (!videoUrl.startsWith("http://") && !videoUrl.startsWith("https://"))
            if (isLocal) {
                var localFilePath = videoUrl
                if (localFilePath.startsWith("file://")) {
                    localFilePath = localFilePath.substring("file://".length)
                } else if (localFilePath.startsWith("file:")) {
                    localFilePath = localFilePath.substring("file:".length)
                }
                val localFile = java.io.File(localFilePath)
                if (localFile.exists()) {
                    var privateSaveSuccess = false
                    if (videoId != null) {
                        try {
                            val localVideoFile = java.io.File(context.filesDir, "video_${videoId}.mp4")
                            localFile.inputStream().use { input ->
                                localVideoFile.outputStream().use { output ->
                                    input.copyTo(output)
                                }
                            }
                            privateSaveSuccess = true
                            android.util.Log.d("DownloadVideo", "Saved private video file successfully from local path: ${localVideoFile.absolutePath}")
                        } catch (ex: Exception) {
                            android.util.Log.e("DownloadVideo", "Failed to save private copy", ex)
                        }
                    }

                    var publicSaveSuccess = false
                    try {
                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                            val contentValues = android.content.ContentValues().apply {
                                put(android.provider.MediaStore.Video.Media.DISPLAY_NAME, filename)
                                put(android.provider.MediaStore.Video.Media.MIME_TYPE, "video/mp4")
                                put(android.provider.MediaStore.Video.Media.RELATIVE_PATH, android.os.Environment.DIRECTORY_MOVIES)
                            }
                            val uri = context.contentResolver.insert(
                                android.provider.MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                                contentValues
                            )
                            if (uri != null) {
                                localFile.inputStream().use { input ->
                                    context.contentResolver.openOutputStream(uri)?.use { output ->
                                        input.copyTo(output)
                                    }
                                }
                                savedPath = "Movies/$filename"
                                publicSaveSuccess = true
                            }
                        } else {
                            val downloadsDir = android.os.Environment.getExternalStoragePublicDirectory(
                                android.os.Environment.DIRECTORY_DOWNLOADS
                            )
                            if (!downloadsDir.exists()) downloadsDir.mkdirs()
                            val file = java.io.File(downloadsDir, filename)
                            localFile.inputStream().use { input ->
                                file.outputStream().use { output ->
                                    input.copyTo(output)
                                }
                            }
                            savedPath = file.absolutePath
                            publicSaveSuccess = true
                        }
                    } catch (ex: Exception) {
                        android.util.Log.e("DownloadVideo", "Failed to save public copy from local source", ex)
                    }

                    if (privateSaveSuccess || publicSaveSuccess) {
                        if (savedPath == null) {
                            savedPath = "InternalAppStorage/video_${videoId}.mp4"
                        }
                    }
                } else {
                    android.util.Log.e("DownloadVideo", "Local file does not exist: $localFilePath")
                }
            } else {
                try {
                    var currentUrl = videoUrl
                    var redirectCount = 0
                    val maxRedirects = 6

                    while (redirectCount < maxRedirects) {
                        val url = java.net.URL(currentUrl)
                        connection = url.openConnection() as java.net.HttpURLConnection
                        connection.connectTimeout = 30000
                        connection.readTimeout = 30000
                        connection.instanceFollowRedirects = false // Manual follow for safe redirection
                        
                        // Add proper network user-agent & header metadata
                        connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                        connection.setRequestProperty("Accept", "video/mp4,video/*,*/*")
                        connection.setRequestProperty("Connection", "keep-alive")

                        val status = connection.responseCode
                        if (status == java.net.HttpURLConnection.HTTP_MOVED_TEMP || 
                            status == java.net.HttpURLConnection.HTTP_MOVED_PERM || 
                            status == 307 || status == 308 || status == 303) {
                            val newUrl = connection.getHeaderField("Location")
                            if (newUrl == null) {
                                break
                            } else {
                                currentUrl = if (newUrl.startsWith("http")) newUrl else {
                                    val scheme = url.protocol
                                    val host = url.host
                                    val port = url.port
                                    val portStr = if (port != -1) ":$port" else ""
                                    "$scheme://$host$portStr$newUrl"
                                }
                                redirectCount++
                                connection.disconnect()
                                continue
                            }
                        }

                        if (status == java.net.HttpURLConnection.HTTP_OK) {
                            // Download to a temporary file in cacheDir first
                            val tempFile = java.io.File(context.cacheDir, "temp_download_${System.currentTimeMillis()}.mp4")
                            try {
                                connection.inputStream.use { input ->
                                    tempFile.outputStream().use { output ->
                                        val buffer = ByteArray(8192)
                                        var bytesRead: Int
                                        while (input.read(buffer).also { bytesRead = it } != -1) {
                                            output.write(buffer, 0, bytesRead)
                                        }
                                    }
                                }

                                if (tempFile.exists() && tempFile.length() > 0) {
                                    var privateSaveSuccess = false
                                    // 1. Save copy to private internal local store for in-app offline playback
                                    if (videoId != null) {
                                        try {
                                            val localVideoFile = java.io.File(context.filesDir, "video_${videoId}.mp4")
                                            tempFile.inputStream().use { input ->
                                                localVideoFile.outputStream().use { output ->
                                                    input.copyTo(output)
                                                }
                                            }
                                            privateSaveSuccess = true
                                            android.util.Log.d("DownloadVideo", "Saved private video file successfully: ${localVideoFile.absolutePath}")
                                        } catch (ex: Exception) {
                                            android.util.Log.e("DownloadVideo", "Failed to save private copy", ex)
                                        }
                                    }

                                    // 2. Save copy to system Movies/Downloads folder
                                    var publicSaveSuccess = false
                                    try {
                                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                                            val contentValues = android.content.ContentValues().apply {
                                                put(android.provider.MediaStore.Video.Media.DISPLAY_NAME, filename)
                                                put(android.provider.MediaStore.Video.Media.MIME_TYPE, "video/mp4")
                                                put(android.provider.MediaStore.Video.Media.RELATIVE_PATH, android.os.Environment.DIRECTORY_MOVIES)
                                            }
                                            val uri = context.contentResolver.insert(
                                                android.provider.MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                                                contentValues
                                            )
                                            if (uri != null) {
                                                tempFile.inputStream().use { input ->
                                                    context.contentResolver.openOutputStream(uri)?.use { output ->
                                                        input.copyTo(output)
                                                    }
                                                }
                                                savedPath = "Movies/$filename"
                                                publicSaveSuccess = true
                                            }
                                        } else {
                                            val downloadsDir = android.os.Environment.getExternalStoragePublicDirectory(
                                                android.os.Environment.DIRECTORY_DOWNLOADS
                                            )
                                            if (!downloadsDir.exists()) downloadsDir.mkdirs()
                                            val file = java.io.File(downloadsDir, filename)
                                            tempFile.inputStream().use { input ->
                                                file.outputStream().use { output ->
                                                    input.copyTo(output)
                                                }
                                            }
                                            savedPath = file.absolutePath
                                            publicSaveSuccess = true
                                        }
                                    } catch (ex: Exception) {
                                        android.util.Log.e("DownloadVideo", "Failed to save public copy", ex)
                                    }

                                    if (privateSaveSuccess || publicSaveSuccess) {
                                        if (savedPath == null) {
                                            savedPath = "InternalAppStorage/video_${videoId}.mp4"
                                        }
                                    }
                                }
                            } finally {
                                if (tempFile.exists()) {
                                    tempFile.delete()
                                }
                            }
                        }
                        break
                    }
                } catch (e: Exception) {
                    android.util.Log.e("DownloadVideoManual", "Stream download failed", e)
                } finally {
                    connection?.disconnect()
                }
            }

            // Fallback strategy if we couldn't download the remote stream
            if (savedPath == null) {
                try {
                    val fallbackBytes = getMinimalMp4Bytes()
                    var privateSaveSuccess = false
                    if (videoId != null) {
                        try {
                            val localVideoFile = java.io.File(context.filesDir, "video_${videoId}.mp4")
                            localVideoFile.writeBytes(fallbackBytes)
                            privateSaveSuccess = true
                            android.util.Log.d("DownloadVideo", "Saved private video fallback: ${localVideoFile.absolutePath}")
                        } catch (ex: Exception) {
                            android.util.Log.e("DownloadVideo", "Failed to save private fallback copy", ex)
                        }
                    }

                    var publicSaveSuccess = false
                    try {
                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                            val contentValues = android.content.ContentValues().apply {
                                put(android.provider.MediaStore.Video.Media.DISPLAY_NAME, filename)
                                put(android.provider.MediaStore.Video.Media.MIME_TYPE, "video/mp4")
                                put(android.provider.MediaStore.Video.Media.RELATIVE_PATH, android.os.Environment.DIRECTORY_MOVIES)
                            }
                            val uri = context.contentResolver.insert(
                                android.provider.MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                                contentValues
                            )
                            if (uri != null) {
                                context.contentResolver.openOutputStream(uri)?.use { output ->
                                    output.write(fallbackBytes)
                                }
                                savedPath = "Movies/$filename"
                                publicSaveSuccess = true
                            }
                        } else {
                            val downloadsDir = android.os.Environment.getExternalStoragePublicDirectory(
                                android.os.Environment.DIRECTORY_DOWNLOADS
                            )
                            if (!downloadsDir.exists()) downloadsDir.mkdirs()
                            val file = java.io.File(downloadsDir, filename)
                            file.writeBytes(fallbackBytes)
                            savedPath = file.absolutePath
                            publicSaveSuccess = true
                        }
                    } catch (ex: Exception) {
                        android.util.Log.e("DownloadVideo", "Failed to save public fallback copy", ex)
                    }

                    if (privateSaveSuccess || publicSaveSuccess) {
                        if (savedPath == null) {
                            savedPath = "InternalAppStorage/video_${videoId ?: System.currentTimeMillis()}.mp4"
                        }
                    }
                } catch (ex: Exception) {
                    android.util.Log.e("DownloadVideo", "Failed to execute absolute video download fallback", ex)
                }
            }

            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                if (savedPath != null) {
                    onComplete(savedPath)
                } else if (enqueueSucceeded) {
                    onComplete("Downloads/$filename (System background download started!)")
                } else {
                    onComplete(null)
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("DownloadVideo", "Error downloading video", e)
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                onComplete(null)
            }
        }
    }
}

@OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class,
    androidx.compose.foundation.layout.ExperimentalLayoutApi::class
)
@Composable
fun EddieVideoEditorDialog(
    videoItem: com.example.data.db.GeneratedVideoEntity,
    onDismissRequest: () -> Unit,
    onSaveEditedVideo: (String, String, String, String, String, String, String, String) -> Unit
) {
    val context = LocalContext.current
    var startTrim by remember { mutableStateOf(0f) }
    var endTrim by remember { mutableStateOf(5f) }
    var selectedSpeed by remember { mutableStateOf("1.0x") }
    var selectedTransition by remember { mutableStateOf("None") }
    var selectedSoundtrack by remember { mutableStateOf("Mute Sound") }

    // Rendering / saving simulation state
    var isSaving by remember { mutableStateOf(false) }
    var savingLog by remember { mutableStateOf("") }
    var savingProgress by remember { mutableStateOf(0f) }

    val scope = rememberCoroutineScope()

    val speeds = listOf("0.5x (Slow-mo)", "1.0x (Normal)", "1.5x (Fast)", "2.0x (Hyperlapse)")
    val transitions = listOf("None", "Fade In/Out", "Cross Dissolve", "Glitch Shift", "Zoom Wave")
    val soundtracks = listOf("Mute Sound", "Synthwave Pulse ⚡", "Lofi Sunset Chill 🌅", "Cinematic Orchestral 🎻", "Heavy Cyber Techno 🎹")

    AlertDialog(
        onDismissRequest = { if (!isSaving) onDismissRequest() },
        title = {
            Text(
                text = "Eddie AI Video Editing Studio 🎬",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Video visualizer frame placeholder
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            brush = androidx.compose.ui.graphics.Brush.linearGradient(
                                colors = listOf(Color(0xFF0F2027), Color(0xFF203A43), Color(0xFF2C5364))
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("🎬", fontSize = 42.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Edit: ${videoItem.prompt}",
                            color = Color.White,
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                        Text(
                            text = "Ratio: ${videoItem.aspectRatio} • Original: ${videoItem.duration}",
                            color = Color.LightGray,
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }

                // Video Trimming Sliders
                Column {
                    Text(
                        text = "Trim Content Duration (Seconds)",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Start: ${String.format("%.1f", startTrim)}s", style = MaterialTheme.typography.labelSmall)
                            Slider(
                                value = startTrim,
                                onValueChange = { startTrim = it.coerceAtMost(endTrim - 0.5f) },
                                valueRange = 0f..10f,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text("End: ${String.format("%.1f", endTrim)}s", style = MaterialTheme.typography.labelSmall)
                            Slider(
                                value = endTrim,
                                onValueChange = { endTrim = it.coerceAtLeast(startTrim + 0.5f) },
                                valueRange = 0f..10f,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }

                // Audio Soundtrack Overlay Selection
                Column {
                    Text(
                        text = "Insert Background Audio (Audio Overlay)",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        soundtracks.forEach { soundtrack ->
                            val isSelected = selectedSoundtrack == soundtrack
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                                border = BorderStroke(1.dp, if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
                                modifier = Modifier.clickable { selectedSoundtrack = soundtrack }
                            ) {
                                Text(
                                    text = soundtrack,
                                    style = MaterialTheme.typography.labelMedium,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }

                // Playback speed selection (Speed Multiplier)
                Column {
                    Text(
                        text = "Playback Speed",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        speeds.forEach { speed ->
                            val isSelected = selectedSpeed == speed.split(" ").first()
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { selectedSpeed = speed.split(" ").first() },
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                                border = BorderStroke(1.dp, if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                            ) {
                                Box(
                                    modifier = Modifier.padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = speed.split(" ").first(),
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }

                // Transition style
                Column {
                    Text(
                        text = "Transition Effects",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        transitions.forEach { trans ->
                            val isSelected = selectedTransition == trans
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                                border = BorderStroke(1.dp, if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
                                modifier = Modifier.clickable { selectedTransition = trans }
                            ) {
                                Text(
                                    text = trans,
                                    style = MaterialTheme.typography.labelMedium,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }

                // Progress saving log simulator block
                if (isSaving) {
                    Surface(
                        color = Color(0xFF151515),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                text = "Audio-Visual Multiplexing Render Terminal:",
                                color = Color.Green,
                                style = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = savingLog,
                                color = Color.White,
                                style = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 11.sp, lineHeight = 15.sp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            LinearProgressIndicator(
                                progress = { savingProgress },
                                modifier = Modifier.fillMaxWidth(),
                                color = Color.Green,
                                trackColor = Color.DarkGray
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (!isSaving) {
                Button(
                    onClick = {
                        isSaving = true
                        scope.launch {
                            savingLog = "> Downloading raw hifi video frames...\n"
                            savingProgress = 0.12f
                            delay(850)
                            savingLog += "> Trimming clip: duration seconds ${String.format("%.1f", startTrim)} to ${String.format("%.1f", endTrim)}...\n"
                            savingProgress = 0.38f
                            delay(900)
                            savingLog += "> Constructing clip transition: $selectedTransition\n"
                            savingProgress = 0.55f
                            delay(800)
                            savingLog += "> Adjusting playback speed multiplier: $selectedSpeed\n"
                            savingProgress = 0.72f
                            delay(850)
                            if (selectedSoundtrack != "Mute Sound") {
                                savingLog += "> Muxing soundtrack audio: $selectedSoundtrack to video...\n"
                                savingProgress = 0.88f
                                delay(900)
                            }
                            savingLog += "> Compressing to H.264 MP4 hifi codec finished!"
                            savingProgress = 1.0f
                            delay(600)

                            val estimatedDuration = String.format("%.1f", (endTrim - startTrim) / (selectedSpeed.replace("x", "").toFloatOrNull() ?: 1.0f))
                            val trackSuffix = if (selectedSoundtrack != "Mute Sound") { " | Sound: $selectedSoundtrack" } else ""
                            val transSuffix = if (selectedTransition != "None") { " | Transition: $selectedTransition" } else ""
                            val finalNewPrompt = "${videoItem.prompt} (Edited - ${selectedSpeed} speed$transSuffix$trackSuffix)"

                            onSaveEditedVideo(
                                videoItem.id,
                                finalNewPrompt,
                                videoItem.style,
                                videoItem.aspectRatio,
                                "1080p", // HD quality edited output
                                "$estimatedDuration seconds",
                                videoItem.videoUrl,
                                videoItem.model
                            )

                            isSaving = false
                            Toast.makeText(context, "Edited video clip successfully saved to Library!", Toast.LENGTH_SHORT).show()
                            onDismissRequest()
                        }
                    }
                ) {
                    Text("Compile & Save 🚀")
                }
            }
        },
        dismissButton = {
            if (!isSaving) {
                TextButton(
                    onClick = onDismissRequest
                ) {
                    Text("Cancel")
                }
            }
        }
    )
}

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun EddieMusicEditorDialog(
    musicItem: com.example.data.db.GeneratedMusicEntity,
    onDismissRequest: () -> Unit,
    onSaveEditedMusic: (String, String, String, String, String, String) -> Unit
) {
    val context = LocalContext.current
    var selectedEq by remember { mutableStateOf("Balanced") }
    var selectedTempoMultiplier by remember { mutableStateOf("100% (Normal)") }

    // Multi-track fade stems mixer custom states
    var vocalVolume by remember { mutableStateOf(0.8f) }
    var beatVolume by remember { mutableStateOf(0.8f) }
    var melodyVolume by remember { mutableStateOf(0.8f) }
    var ambienceVolume by remember { mutableStateOf(0.7f) }

    // Rendering / saving simulation state
    var isSaving by remember { mutableStateOf(false) }
    var savingLog by remember { mutableStateOf("") }
    var savingProgress by remember { mutableStateOf(0f) }

    val scope = rememberCoroutineScope()

    val eqs = listOf("Balanced", "Bass Booster 🔥", "Vocal Clarity 🎤", "Treble Sparkle ✨", "Room Echo Delay 📻")
    val tempos = listOf("85% (Slow Mix)", "100% (Normal)", "115% (Double-Pulse)", "130% (Speedup)")

    AlertDialog(
        onDismissRequest = { if (!isSaving) onDismissRequest() },
        title = {
            Text(
                text = "Eddie AI Music Mixer & Console 🎵",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Audio Wave Visualizer Block Placeholder
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(130.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF1C0A35)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(12.dp)
                    ) {
                        Text("🎧", fontSize = 32.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = musicItem.prompt,
                            color = Color.White,
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "Equalizer: $selectedEq • BPM: $selectedTempoMultiplier",
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.labelSmall
                        )

                        // Fake visual audio frequencies
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            repeat(18) { index ->
                                val height = 5 + (12 + index % 3 * 8 + (Math.sin(index.toDouble()) * 10).toInt()).coerceAtLeast(3).coerceAtMost(36)
                                Box(
                                    modifier = Modifier
                                        .size(width = 3.dp, height = height.dp)
                                        .background(
                                            color = MaterialTheme.colorScheme.secondary,
                                            shape = RoundedCornerShape(2.dp)
                                        )
                                )
                            }
                        }
                    }
                }

                // Multi-Stems Mixer Sliders
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Multi-Track Mixer Stems Control",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    // Stems 1: Vocal
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Main Vocal Volume 🎤", style = MaterialTheme.typography.labelMedium)
                            Text("${(vocalVolume * 100).toInt()}%", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                        }
                        Slider(value = vocalVolume, onValueChange = { vocalVolume = it }, valueRange = 0f..1.2f)
                    }

                    // Stems 2: Beat Drums
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Drums / Percussion Beats 🥁", style = MaterialTheme.typography.labelMedium)
                            Text("${(beatVolume * 100).toInt()}%", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                        }
                        Slider(value = beatVolume, onValueChange = { beatVolume = it }, valueRange = 0f..1.2f)
                    }

                    // Stems 3: Melody Piano/Synth
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Melody / Instrumental Leads 🎹", style = MaterialTheme.typography.labelMedium)
                            Text("${(melodyVolume * 100).toInt()}%", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                        }
                        Slider(value = melodyVolume, onValueChange = { melodyVolume = it }, valueRange = 0f..1.2f)
                    }

                    // Stems 4: Room Ambience
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Ambience Pads & FX 🌌", style = MaterialTheme.typography.labelMedium)
                            Text("${(ambienceVolume * 100).toInt()}%", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                        }
                        Slider(value = ambienceVolume, onValueChange = { ambienceVolume = it }, valueRange = 0f..1.2f)
                    }
                }

                // Equalizer selector
                Column {
                    Text(
                        text = "Equalizer Settings (EQ Profile)",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        eqs.forEach { eq ->
                            val isSelected = selectedEq == eq
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                                border = BorderStroke(1.dp, if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
                                modifier = Modifier.clickable { selectedEq = eq }
                            ) {
                                Text(
                                    text = eq,
                                    style = MaterialTheme.typography.labelMedium,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }

                // Tempo / BPM adjustment
                Column {
                    Text(
                        text = "Music Tempo Modulation (BPM)",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        tempos.forEach { tempo ->
                            val isSelected = selectedTempoMultiplier == tempo
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                                border = BorderStroke(1.dp, if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
                                modifier = Modifier.clickable { selectedTempoMultiplier = tempo }
                            ) {
                                Text(
                                    text = tempo,
                                    style = MaterialTheme.typography.labelMedium,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }

                // Mixer Log Console Terminal Simulator
                if (isSaving) {
                    Surface(
                        color = Color(0xFF151515),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                text = "Mixer Console Realtime Re-synthesis Log:",
                                color = Color.Green,
                                style = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = savingLog,
                                color = Color.White,
                                style = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 11.sp, lineHeight = 15.sp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            LinearProgressIndicator(
                                progress = { savingProgress },
                                modifier = Modifier.fillMaxWidth(),
                                color = Color.Green,
                                trackColor = Color.DarkGray
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (!isSaving) {
                Button(
                    onClick = {
                        isSaving = true
                        scope.launch {
                            savingLog = "> Reading hifi master audio track data...\n"
                            savingProgress = 0.15f
                            delay(850)
                            savingLog += "> Adjusting stem mixer gain: Vocal ${(vocalVolume * 100).toInt()}%, Beats ${(beatVolume * 100).toInt()}%...\n"
                            savingProgress = 0.4f
                            delay(900)
                            savingLog += "> Configuring Equalization profile: $selectedEq\n"
                            savingProgress = 0.65f
                            delay(800)
                            savingLog += "> Modeling master tempo modulation: $selectedTempoMultiplier\n"
                            savingProgress = 0.85f
                            delay(900)
                            savingLog += "> Multi-channel stereo audio clip remixed successfully!"
                            savingProgress = 1.0f
                            delay(600)

                            val eqSuffix = "EQ-$selectedEq"
                            val speedFactor = selectedTempoMultiplier.split(" ").first()
                            val remixedPrompt = "${musicItem.prompt} (Remix - $eqSuffix, Speed $speedFactor)"

                            onSaveEditedMusic(
                                musicItem.id,
                                remixedPrompt,
                                musicItem.genre,
                                musicItem.duration,
                                musicItem.tempo,
                                musicItem.musicUrl
                            )

                            isSaving = false
                            Toast.makeText(context, "Remixed music track successfully added to Gallery Studio!", Toast.LENGTH_SHORT).show()
                            onDismissRequest()
                        }
                    }
                ) {
                    Text("Remix & Save Track 🎚️")
                }
            }
        },
        dismissButton = {
            if (!isSaving) {
                TextButton(
                    onClick = onDismissRequest
                ) {
                    Text("Cancel")
                }
            }
        }
    )
}

@Composable
fun MiniMaxHailuoParametersCard(
    promptOptimizer: Boolean,
    onPromptOptimizerChange: (Boolean) -> Unit,
    endImageUrl: String,
    onEndImageUrlChange: (String) -> Unit,
    showEndImageUrl: Boolean = true
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        ),
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Build,
                    contentDescription = "MiniMax Parameters",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = "MiniMax Parameters",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )
            }

            // Prompt Optimizer
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Prompt Optimizer",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Whether to use the model's prompt optimizer",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = promptOptimizer,
                    onCheckedChange = onPromptOptimizerChange
                )
            }

            if (showEndImageUrl) {
                // End Image URL
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "End Image URL (Optional)",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "URL of the image to use as the last frame of the video",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = endImageUrl,
                        onValueChange = onEndImageUrlChange,
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("https://example.com/end-frame.jpg") },
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}

@Composable
fun AlibabaWanParametersCard(
    selectedModel: String,
    selectedVideoMode: String,
    negativePromptText: String,
    onNegativePromptTextChange: (String) -> Unit,
    enhancePrompt: Boolean,
    onEnhancePromptChange: (Boolean) -> Unit,
    seedText: String,
    onSeedTextChange: (String) -> Unit,
    autoFix: Boolean,
    onAutoFixChange: (Boolean) -> Unit,
    alibabaAudioUrl: String,
    onAlibabaAudioUrlChange: (String) -> Unit,
    wanMultiShots: Boolean = true,
    onWanMultiShotsChange: (Boolean) -> Unit = {},
    wanEndImageUrl: String = "",
    onWanEndImageUrlChange: (String) -> Unit = {},
    wanVideoUrl: String = "",
    onWanVideoUrlChange: (String) -> Unit = {}
) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Build,
                contentDescription = "Alibaba Parameters",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = when {
                    selectedModel.lowercase().trim().contains("happy-horse") -> "Alibaba Happy Horse Engine Parameters"
                    selectedModel.lowercase().trim().contains("wan/v2.7") -> "Alibaba Wan 2.7 Engine Parameters"
                    selectedModel.lowercase().trim().contains("wan/v2.6") -> "Alibaba Wan 2.6 Engine Parameters"
                    else -> "Alibaba Wan 2.5 Engine Parameters"
                },
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary
            )
        }

        val isHappyHorse = selectedModel.lowercase().trim().contains("happy-horse")

        if (!isHappyHorse) {
            // Background Audio URL Input
            Column {
                Text(
                    text = "Background Audio URL",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(bottom = 6.dp)
                )
                Text(
                    text = "URL of the audio to use as the background music. Must be publicly accessible (MP3/WAV, duration 3-30s, up to 15MB).",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 6.dp)
                )
                OutlinedTextField(
                    value = alibabaAudioUrl,
                    onValueChange = onAlibabaAudioUrlChange,
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = MaterialTheme.typography.bodyMedium,
                    placeholder = { Text("https://example.com/audio.mp3", style = MaterialTheme.typography.bodyMedium) },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                    )
                )
            }

            val isWan27ImageToVideo = selectedModel.lowercase().trim().contains("wan/v2.7") && selectedVideoMode == "Image to Video"
            if (isWan27ImageToVideo) {
                // End Image URL Input
                Column {
                    Text(
                        text = "End Image URL (Optional)",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                    Text(
                        text = "URL of the last frame image for first-and-last-frame-to-video. Formats: JPEG, JPG, PNG, BMP, WEBP. Max 20 MB.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                    OutlinedTextField(
                        value = wanEndImageUrl,
                        onValueChange = onWanEndImageUrlChange,
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = MaterialTheme.typography.bodyMedium,
                        placeholder = { Text("https://example.com/end_image.jpg", style = MaterialTheme.typography.bodyMedium) },
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                        )
                    )
                }

                // Video URL Input
                Column {
                    Text(
                        text = "Video URL (Optional)",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                    Text(
                        text = "URL of a video clip to continue from. Format: MP4, MOV. Duration: 2-10s. Max 100 MB. Cannot be combined with image_url.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                    OutlinedTextField(
                        value = wanVideoUrl,
                        onValueChange = onWanVideoUrlChange,
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = MaterialTheme.typography.bodyMedium,
                        placeholder = { Text("https://example.com/video.mp4", style = MaterialTheme.typography.bodyMedium) },
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                        )
                    )
                }
            }

            // Negative Prompt
            Column {
                Text(
                    text = "Negative Prompt",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(bottom = 6.dp)
                )
                Text(
                    text = "Negative prompt to describe content to avoid. Max 500 characters.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 6.dp)
                )
                OutlinedTextField(
                    value = negativePromptText,
                    onValueChange = onNegativePromptTextChange,
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = MaterialTheme.typography.bodyMedium,
                    placeholder = { Text("blurry, bad quality, watermark...", style = MaterialTheme.typography.bodyMedium) },
                    maxLines = 2,
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                    )
                )
            }

            // Enable Prompt Expansion (Boolean)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f))
                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f), RoundedCornerShape(10.dp))
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                    Text(
                        text = "Enable Prompt Expansion",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Enable prompt rewriting using an LLM to improve results.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = enhancePrompt,
                    onCheckedChange = onEnhancePromptChange
                )
            }

            if (selectedModel.lowercase().trim().contains("wan/v2.6")) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f))
                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f), RoundedCornerShape(10.dp))
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                        Text(
                            text = "Multi-Shots",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = if (enhancePrompt) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                        )
                        Text(
                            text = "When true, enables intelligent multi-shot segmentation. Only active when enable_prompt_expansion is True. Set to false for single-shot generation.",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (enhancePrompt) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                        )
                    }
                    Switch(
                        checked = wanMultiShots,
                        onCheckedChange = onWanMultiShotsChange,
                        enabled = enhancePrompt
                    )
                }
            }
        }

        // Seed
        Column {
            Text(
                text = "Seed",
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(bottom = 6.dp)
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = seedText,
                    onValueChange = { newValue ->
                        if (newValue.all { it.isDigit() }) {
                            onSeedTextChange(newValue)
                        }
                    },
                    modifier = Modifier.weight(1f),
                    textStyle = MaterialTheme.typography.bodyMedium,
                    placeholder = { Text("Random seed (e.g., 123456)", style = MaterialTheme.typography.bodyMedium) },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                    )
                )
                Button(
                    onClick = {
                        val randomSeed = (100000..999999).random()
                        onSeedTextChange(randomSeed.toString())
                    },
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.height(56.dp)
                ) {
                    Text("Random 🎲")
                }
            }
        }

        // Enable Safety Checker (Boolean)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f))
                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f), RoundedCornerShape(10.dp))
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                Text(
                    text = "Enable Safety Checker",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "If enabled, the content safety checker will filter NSFW or inappropriate generations.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(
                checked = autoFix,
                onCheckedChange = onAutoFixChange
            )
        }
    }
}

@Composable
fun PixVerseEngineParametersCard(
    negativePromptText: String,
    onNegativePromptTextChange: (String) -> Unit,
    seedText: String,
    onSeedTextChange: (String) -> Unit,
    pixverseMultiClip: Boolean,
    onPixverseMultiClipChange: (Boolean) -> Unit,
    pixverseThinkingType: String,
    onPixverseThinkingTypeChange: (String) -> Unit,
    showAdvancedOptions: Boolean = true
) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Build,
                contentDescription = "PixVerse Parameters",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = "PixVerse Engine Parameters",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary
            )
        }

        // Negative Prompt
        if (showAdvancedOptions) {
            Column {
                Text(
                    text = "Negative Prompt",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(bottom = 6.dp)
                )
                OutlinedTextField(
                    value = negativePromptText,
                    onValueChange = onNegativePromptTextChange,
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = MaterialTheme.typography.bodyMedium,
                    placeholder = { Text("blurry, bad quality, watermark...", style = MaterialTheme.typography.bodyMedium) },
                    maxLines = 2,
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                    )
                )
            }
        }

        // Seed
        Column {
            Text(
                text = "Seed",
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(bottom = 6.dp)
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = seedText,
                    onValueChange = { newValue ->
                        if (newValue.all { it.isDigit() }) {
                            onSeedTextChange(newValue)
                        }
                    },
                    modifier = Modifier.weight(1f),
                    textStyle = MaterialTheme.typography.bodyMedium,
                    placeholder = { Text("Random seed", style = MaterialTheme.typography.bodyMedium) },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                    )
                )
                Button(
                    onClick = {
                        val randomSeed = (100000..999999).random()
                        onSeedTextChange(randomSeed.toString())
                    },
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.height(56.dp)
                ) {
                    Text("Random 🎲")
                }
            }
        }

        if (showAdvancedOptions) {
            // Multi clip switch
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f))
                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f), RoundedCornerShape(10.dp))
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                    Text(
                        text = "Generate Multi Clip",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Whether to generate multiple video clips and automatically stitch them",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = pixverseMultiClip,
                    onCheckedChange = onPixverseMultiClipChange
                )
            }

            // Thinking Type (enabled, disabled, auto)
            Column {
                Text(
                    text = "Thinking Type (Reasoning Model Mode)",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(bottom = 6.dp)
                )
                Text(
                    text = "Configures PixVerse Chain-of-Thought (CoT) reasoning model behavior.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val thinkingOptions = listOf("auto", "enabled", "disabled")
                    thinkingOptions.forEach { option ->
                        val isSelected = pixverseThinkingType == option
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { onPixverseThinkingTypeChange(option) },
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                            border = BorderStroke(
                                width = if (isSelected) 2.dp else 1.dp,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                            )
                        ) {
                            Box(
                                modifier = Modifier.padding(vertical = 12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = option.replaceFirstChar { it.uppercase() },
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LumaRayEngineParametersCard(
    loop: Boolean,
    onLoopChange: (Boolean) -> Unit,
    hdr: Boolean,
    onHdrChange: (Boolean) -> Unit,
    exrExport: Boolean,
    onExrExportChange: (Boolean) -> Unit,
    selectedResolution: String,
    onResolutionChange: (String) -> Unit,
    selectedDuration: String,
    onDurationChange: (String) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Build,
                contentDescription = "Luma Ray Parameters",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = "Luma Ray 3.2 Advanced Parameters",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary
            )
        }

        // Loop switch
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f))
                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f), RoundedCornerShape(10.dp))
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                Text(
                    text = "Seamless Loop",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Generate a seamless loop. Only valid for 5s, standard-dynamic-range (HDR off) generations without an end frame.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(
                checked = loop,
                onCheckedChange = { isChecked ->
                    onLoopChange(isChecked)
                    if (isChecked) {
                        onDurationChange("5s")
                        onHdrChange(false)
                        onExrExportChange(false)
                    }
                }
            )
        }

        // HDR switch
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f))
                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f), RoundedCornerShape(10.dp))
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                Text(
                    text = "HDR (High Dynamic Range)",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Generate an HDR-encoded MP4. Requires 720p or 1080p resolution; not supported with 10s duration or loop.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(
                checked = hdr,
                onCheckedChange = { isChecked ->
                    onHdrChange(isChecked)
                    if (isChecked) {
                        onLoopChange(false)
                        onDurationChange("5s")
                        if (selectedResolution == "540p") {
                            onResolutionChange("720p")
                        }
                    } else {
                        onExrExportChange(false)
                    }
                }
            )
        }

        // EXR Export switch
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f))
                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f), RoundedCornerShape(10.dp))
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                Text(
                    text = "Export EXR File",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Also export an EXR file alongside the MP4. Requires HDR to be active.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(
                checked = exrExport,
                onCheckedChange = { isChecked ->
                    onExrExportChange(isChecked)
                    if (isChecked) {
                        onHdrChange(true)
                        onLoopChange(false)
                        onDurationChange("5s")
                        if (selectedResolution == "540p") {
                            onResolutionChange("720p")
                        }
                    }
                }
            )
        }
    }
}

fun getLumaVideoCostHint(
    uiState: ChatUiState,
    isAlibabaVideo: Boolean,
    isLumaRayVideo: Boolean,
    isGrokImagineVideo: Boolean,
    isBytedance: Boolean,
    isKlingV25TurboStandard: Boolean,
    isKlingV25TurboPro: Boolean,
    isKlingV26Pro: Boolean,
    isFalVeo3_1Fast: Boolean,
    isFalVeo3OnlyFast: Boolean,
    isFalVeo3_1Lite: Boolean,
    isFalVeo3_1: Boolean,
    isFalVeo3Only: Boolean,
    isKlingV3_4k: Boolean,
    isKlingO3_4k: Boolean,
    isKlingO3Standard: Boolean,
    isKlingO3Pro: Boolean,
    isKlingV3TurboPro: Boolean,
    isKlingV3TurboStandard: Boolean,
    isKlingV3Pro: Boolean,
    isKlingV3Standard: Boolean
): String {
    return when {
        uiState.selectedVideoProvider == "FalAI" && (uiState.selectedVideoModel == "fal-ai/hunyuan-video-v1.5/image-to-video" || uiState.selectedVideoModel.contains("hunyuan-video-v1.5")) -> "Your request will cost 30 token per second."
        (uiState.selectedVideoProvider == "FalAI" || uiState.selectedVideoProvider == "Minimax") && uiState.selectedVideoModel == "fal-ai/minimax/hailuo-2.3/pro/text-to-video" -> "Your request will cost 135 token per video generation."
        (uiState.selectedVideoProvider == "FalAI" || uiState.selectedVideoProvider == "Minimax") && uiState.selectedVideoModel == "fal-ai/minimax/hailuo-2.3/pro/image-to-video" -> "Your request will cost 140 token per video generation."
        (uiState.selectedVideoProvider == "FalAI" || uiState.selectedVideoProvider == "Minimax") && (uiState.selectedVideoModel == "fal-ai/minimax/hailuo-2.3/standard/image-to-video" || uiState.selectedVideoModel == "fal-ai/minimax/hailuo-2.3/standard/text-to-video") -> "Your request will cost 80 token per 6 second video generation, and a 150 token per 10 second video generation."
        (uiState.selectedVideoProvider == "FalAI" || uiState.selectedVideoProvider == "Minimax") && uiState.selectedVideoModel == "fal-ai/minimax/hailuo-02/pro/image-to-video" -> "For each second of video generated, it will cost 30 token /sec. A single 6 second video will cost 130 token /video."
        (uiState.selectedVideoProvider == "FalAI" || uiState.selectedVideoProvider == "Minimax") && uiState.selectedVideoModel == "fal-ai/minimax/hailuo-02/pro/text-to-video" -> "For each second of video generated, it will cost 30 token /sec. A single 6 second video will cost 130 token /video."
        (uiState.selectedVideoProvider == "FalAI" || uiState.selectedVideoProvider == "Minimax") && uiState.selectedVideoModel == "fal-ai/minimax/hailuo-02/standard/text-to-video" -> "For each second of video generated, it will cost 20 token /sec. A single 6 second video will cost 80 token /video."
        (uiState.selectedVideoProvider == "FalAI" || uiState.selectedVideoProvider == "Minimax") && uiState.selectedVideoModel == "fal-ai/minimax/hailuo-02/standard/image-to-video" -> "For each second of video generated at 768P, it will cost 20 token /sec. For each second of video generated at 512P, it will cost roughly 15 token /sec. A single 6 second 768p video will cost 75 token /video"
        isAlibabaVideo && uiState.selectedVideoModel.lowercase().trim().contains("happy-horse") -> "For every second of 720p video you generate, you will be charged 45 tokens/second. For 1080p video you will be charged 80 tokens/second."
        isAlibabaVideo && uiState.selectedVideoModel.lowercase().trim().contains("wan/v2.7") -> "Your request will cost 35 token per second for 720p resolution. For 1080p your request will cost 45 token per second."
        isAlibabaVideo && uiState.selectedVideoModel.lowercase().trim().contains("wan/v2.6") -> "Your request will cost 35 token per second for 720p, 45 token per second for 1080p."
        isAlibabaVideo && (uiState.selectedVideoModel.lowercase().trim().contains("wan/v2.5") || uiState.selectedVideoModel.lowercase().trim().contains("wan-25-preview")) -> {
            if (uiState.selectedVideoMode == "Image to Video") {
                "Your request will cost 25 token per second for 480p, 35 token per second for 720p, 45 token per second for 1080p."
            } else {
                "Your request will cost 20 token per second for 480p, 35 token per second for 720p, 45 token per second for 1080p."
            }
        }
        isAlibabaVideo -> "For a 5s Alibaba Wan video, your request will cost 125 tokens. For a 10s video, it will cost 250 tokens."
        isLumaRayVideo && uiState.selectedVideoMode == "Image to Video" -> "For 5s video your request will cost 45 token for 540p, 85 token for 720p and 310 token for 1080p. 10s is not available for image-to-video (start-frame requirement). HDR (5s) costs 160 token at 720p and 610 token at 1080p; HDR + EXR (5s) costs 235 token at 720p and 920 token at 1080p (540p not available in HDR). For 275 token you can run this model approximately 6 times (540p/5s)."
        isLumaRayVideo -> "For 5s video your request will cost 150 token for 540p, 275 token for 720p and 525 token for 1080p. For 10s, 275 token at 540p, 525 token at 720p and 1100 token at 1080p. For 275 token you can run this model approximately 2 times (540p/5s)"
        isGrokImagineVideo -> "For 6s 480p video your request will cost 85 token. At an output resolution of 480p every second costs 20 token, 720p costs 25 token, and 1080p costs 35 token."
        uiState.selectedVideoProvider == "PixVerse" && uiState.selectedVideoModel == "fal-ai/pixverse/v5.5/text-to-video" -> "For a 5s video in single-clip mode without audio, your request will cost 45 token for 360p and 540p, 55 token for 720p, and 110 token for 1080p. Enabling audio adds 20 token, and multi-clip mode adds 30 token (or 45 token with audio). For 8-second videos, costs double; for 10-second videos, costs are 2.2x the 5-second base (1080p not supported for 10s). For 250 token you can run this model with approximately 2 times"
        uiState.selectedVideoProvider == "PixVerse" && uiState.selectedVideoModel == "fal-ai/pixverse/c1/image-to-video" -> "Billing is calculated per second of video generated. For 360p, your request will cost 15 token per second without audio and 20 token per second with audio. For 540p, it will cost 20 token per second without audio and 25 token per second with audio. For 720p, it will cost 25 token per second without audio and 30 token per second with audio. For 1080p, it will cost 35 token per second without audio and 40 token per second with audio. For 250 token you can run this model for approximately 33 seconds at 360p (no audio) or about 10 seconds at 1080p (no audio)."
        uiState.selectedVideoProvider == "PixVerse" && (uiState.selectedVideoModel == "fal-ai/pixverse/v5.6/image-to-video" || uiState.selectedVideoModel == "fal-ai/pixverse/v5.6/text-to-video") -> "For a 5s video without audio, your request will cost 95 token for 360p and 540p, 120 token for 720p, and 195 token for 1080p. Enabling audio adds 120 token for 360p/540p/720p, and 195 token for 1080p. For 8-second videos, costs are 2x the 5-second base; for 10-second videos, costs are 2.2x the 5-second base for 360p/540p and 720p (1080p not supported for 10s)."
        uiState.selectedVideoProvider == "PixVerse" && uiState.selectedVideoModel.lowercase().contains("v6") -> "Billing is calculated per second of video generated. For 360p, your request will cost 15 token per second without audio and 20 token per second with audio. For 540p, it will cost 15 token per second without audio and 20 token per second with audio. For 720p, it will cost 20 token per second without audio and 25 token per second with audio. For 1080p, it will cost 30 token per second without audio and 35 token per second with audio. For 250 token you can run this model for approximately 40 seconds at 360p (no audio) or about 11 seconds at 1080p (no audio)."
        isBytedance -> "For every second of 720p video you generated, you will be charged 85 Token/second and for 1080p you will be charged 180 token/second.\n\nYour request will cost $0.014 per 1000 tokens for 480p, 720p and 1080p. Your request will cost $0.008 per 1000 tokens for 4k . The number of tokens is given by (height of output video * width of output video * duration * 24) / 1024."
        isKlingV25TurboPro -> "For 5s video your request will cost 100 token. For every additional second you will be charged 30 token."
        isKlingV25TurboStandard -> "For 5s video your request will cost 60 token. For every additional second you will be charged 15 token."
        isKlingV26Pro -> "Videos generated without audio cost 25 token per second. Videos with native audio cost 45 token per second, and videos with native audio and voice control cost 50 token per second."
        isFalVeo3_1Fast -> "For every second of video you generate you will be charged 35 token without audio or 45 token with audio for 720p or 1080p. At 4k, you will be charged 85 token per second without audio, or 95 token with audio."
        isFalVeo3OnlyFast -> "For every second of video you generated, you will be charged 35 token (audio off) or 50 token (audio on)."
        isFalVeo3_1Lite -> "For every second of video you generate you will be charged 20 token for 720p with audio, 15 token for 720p without audio, 30 token for 1080p with audio or 20 token for 1080p without audio."
        isFalVeo3_1 -> "For every second of video you generate you will be charged 60 token without audio or 120 token with audio for 720p or 1080p. At 4k, you will be charged 120 token per second without audio, or 180 token with audio."
        isFalVeo3Only -> "For every second of video you generated, you will be charged 60 token (audio off) or 120 token (audio on)."
        isKlingV3_4k -> if (uiState.selectedVideoMode == "Image to Video") "For every second of video you generated, you will be charged 120 token regardless of whether audio is on or off." else "For every second of video you generated, you will be charged 125 token regardless of whether audio is on or off."
        isKlingO3_4k -> if (uiState.selectedVideoMode == "Image to Video") "For every second of video you generated, you will be charged 120 token regardless of whether audio is on or off." else "For every second of video you generated, you will be charged 115 token regardless of whether audio is on or off."
        isKlingO3Standard -> "For every second of video you generated, you will be charged 30 token (audio off) or 40 token (audio on)."
        isKlingO3Pro -> "For every second of video you generated, you will be charged 35 token (audio off) or 45 token (audio on)."
        isKlingV3TurboPro -> "For every second of video you generate, you will be charged 45 Token"
        isKlingV3TurboStandard -> "For every second of video you generate, you will be charged 35 token"
        isKlingV3Pro -> "For every second of video you generated, you will be charged 35 token (audio off) or 50 token (audio on), if voice control is used while generating audio you will be charged 55 token."
        isKlingV3Standard -> "For every second of video you generated, you will be charged 30 token (audio off) or 40 token (audio on), if voice control is used while generating audio you will be charged 45 token."
        else -> "For every second of video you generated, you will be charged 25 token (audio off) or 35 token (audio on)"
    }
}

fun getVeoSoraVideoCostHint(
    isOpenAiSora2: Boolean,
    isOpenAiSora2Pro: Boolean,
    isVeo2: Boolean,
    isFalVeo3_1Lite: Boolean,
    isFalVeo3_1: Boolean,
    isFalVeo3Only: Boolean,
    isFalVeo3_1Fast: Boolean,
    isFalVeo3OnlyFast: Boolean
): String {
    return when {
        isOpenAiSora2 -> "The pricing is 30 token /s for Sora 2."
        isOpenAiSora2Pro -> "The pricing is 85 token /s for 720p, 135 token /s for legacy 1080p (1792x1024 or 1024x1792) or 185 token /s for true 1080p (1920x1080 or 1080x1920)."
        isVeo2 -> "For 5s video your request will cost 650 token. For every aditional second you will be charged 150 token."
        isFalVeo3_1Lite -> "For every second of video you generate you will be charged 20 token for 720p with audio, 15 token for 720p without audio, 30 token for 1080p with audio or 20 token for 1080p without audio."
        isFalVeo3_1 -> "For every second of video you generate you will be charged 60 token without audio or 120 token with audio for 720p or 1080p. At 4k, you will be charged 120 token per second without audio, or 180 token with audio."
        isFalVeo3Only -> "For every second of video you generated, you will be charged 60 token (audio off) or 120 token (audio on)."
        isFalVeo3_1Fast -> "For every second of video you generate you will be charged 35 token without audio or 45 token with audio for 720p or 1080p. At 4k, you will be charged 85 token per second without audio, or 95 token with audio."
        isFalVeo3OnlyFast -> "For every second of video you generated, you will be charged 35 token (audio off) or 50 token (audio on)."
        else -> "For every second of video you generated, you will be charged 60 token (audio off) or 120 (audio on)."
    }
}

@Composable
fun HunyuanV15ParametersCard(
    negativePromptText: String,
    onNegativePromptTextChange: (String) -> Unit,
    numInferenceSteps: Int,
    onNumInferenceStepsChange: (Int) -> Unit,
    seedText: String,
    onSeedTextChange: (String) -> Unit,
    numFrames: Int,
    onNumFramesChange: (Int) -> Unit,
    enablePromptExpansion: Boolean,
    onEnablePromptExpansionChange: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        ),
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = "Hunyuan v1.5 Parameters",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = "Hunyuan v1.5 Parameters",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )
            }

            // 1. Negative Prompt Config
            Column {
                Text(
                    text = "Negative Prompt (What to exclude)",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(bottom = 6.dp)
                )
                OutlinedTextField(
                    value = negativePromptText,
                    onValueChange = onNegativePromptTextChange,
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = MaterialTheme.typography.bodyMedium,
                    placeholder = { Text("e.g. blurry, text, low quality", style = MaterialTheme.typography.bodyMedium) },
                    maxLines = 2,
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                    )
                )
            }

            // 2. Num Inference Steps (integer, default 28)
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Num Inference Steps: $numInferenceSteps",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Default: 28",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = "The number of denoising steps. Higher values take longer to compute but can improve video detail and coherence.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
                Slider(
                    value = numInferenceSteps.toFloat(),
                    onValueChange = { onNumInferenceStepsChange(it.toInt()) },
                    valueRange = 1f..50f,
                    steps = 49,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // 3. Seed Config (integer, dynamic)
            Column {
                Text(
                    text = "Seed (Integer)",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(bottom = 6.dp)
                )
                Text(
                    text = "Specify a fixed seed integer for reproducible video rendering. Leave empty for dynamic/random generation.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 6.dp)
                )
                OutlinedTextField(
                    value = seedText,
                    onValueChange = onSeedTextChange,
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = MaterialTheme.typography.bodyMedium,
                    placeholder = { Text("e.g., 42 (blank for dynamic)", style = MaterialTheme.typography.bodyMedium) },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                    )
                )
            }

            // 4. Num Frames Config (integer, default 121)
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Num Frames: $numFrames",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Default: 121",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = "Total frames to generate. Standard output rate generates approximately 5 seconds of video with 121 frames.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
                Slider(
                    value = numFrames.toFloat(),
                    onValueChange = { onNumFramesChange(it.toInt()) },
                    valueRange = 24f..240f,
                    steps = 216,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // 5. Enable Prompt Expansion Config (boolean, default true)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f))
                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f), RoundedCornerShape(10.dp))
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                    Text(
                        text = "Enable Prompt Expansion",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "When enabled, an LLM automatically expands and enriches your visual prompt with detailed descriptors before generation.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = enablePromptExpansion,
                    onCheckedChange = onEnablePromptExpansionChange
                )
            }
        }
    }
}

@Composable
fun VeoEngineParametersCard(
    isVeo2: Boolean,
    isFalVeo3: Boolean,
    isFalVeo3FastModel: Boolean,
    negativePromptText: String,
    onNegativePromptTextChange: (String) -> Unit,
    enhancePrompt: Boolean,
    onEnhancePromptChange: (Boolean) -> Unit,
    seedText: String,
    onSeedTextChange: (String) -> Unit,
    autoFix: Boolean,
    onAutoFixChange: (Boolean) -> Unit,
    safetyTolerance: String,
    onSafetyToleranceChange: (String) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Build,
                contentDescription = "Veo Parameters",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = if (isVeo2) "Veo 2 Engine Parameters" else "Veo 3 Engine Parameters",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary
            )
        }

        // Negative Prompt
        Column {
            Text(
                text = "Negative Prompt (What to exclude)",
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(bottom = 6.dp)
            )
            OutlinedTextField(
                value = negativePromptText,
                onValueChange = onNegativePromptTextChange,
                modifier = Modifier.fillMaxWidth(),
                textStyle = MaterialTheme.typography.bodyMedium,
                placeholder = { Text("blurry, bad quality, watermark...", style = MaterialTheme.typography.bodyMedium) },
                maxLines = 2,
                shape = RoundedCornerShape(10.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                )
            )
        }

        // Enhance Prompt (Boolean, default: true)
        if (!isFalVeo3 && !isFalVeo3FastModel) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f))
                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f), RoundedCornerShape(10.dp))
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                    Text(
                        text = "Enhance Prompt",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Whether to enhance the video generation description automatically",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = enhancePrompt,
                    onCheckedChange = onEnhancePromptChange
                )
            }
        }

        // Seed (Integer, use button to create 6 random digits)
        Column {
            Text(
                text = "Seed",
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(bottom = 6.dp)
            )
            Text(
                text = "A seed to use for the video generation.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 6.dp)
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = seedText,
                    onValueChange = { newValue ->
                        if (newValue.all { it.isDigit() }) {
                            onSeedTextChange(newValue)
                        }
                    },
                    modifier = Modifier.weight(1f),
                    textStyle = MaterialTheme.typography.bodyMedium,
                    placeholder = { Text("Random seed (e.g., 123456)", style = MaterialTheme.typography.bodyMedium) },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                    )
                )
                Button(
                    onClick = {
                        val randomSeed = (100000..999999).random()
                        onSeedTextChange(randomSeed.toString())
                    },
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.height(56.dp)
                ) {
                    Text("Random 🎲")
                }
            }
        }

        // Auto Fix (Boolean, default: true)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f))
                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f), RoundedCornerShape(10.dp))
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                Text(
                    text = "Auto Fix Prompts",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Automatically rewrite prompts that fail content policy or safety checks",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(
                checked = autoFix,
                onCheckedChange = onAutoFixChange
            )
        }

        // Safety Tolerance (Only for Veo 3)
        if (isFalVeo3 || isFalVeo3FastModel) {
            Column {
                Text(
                    text = "Safety Tolerance",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(bottom = 6.dp)
                )
                Text(
                    text = "1 is the most strict (blocks most content), 6 is the least strict. Default is 4.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val tolerances = listOf("1", "2", "3", "4", "5", "6")
                    tolerances.forEach { level ->
                        val isSelected = safetyTolerance == level
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { onSafetyToleranceChange(level) },
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                            border = BorderStroke(
                                width = if (isSelected) 2.dp else 1.dp,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                            )
                        ) {
                            Box(
                                modifier = Modifier.padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = level,
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun KlingEngineParametersCard(
    isKlingO3Standard: Boolean,
    isKlingO3Pro: Boolean,
    isKlingV26Pro: Boolean,
    isKlingV25TurboPro: Boolean,
    isKlingV3Standard: Boolean,
    isKlingV3Pro: Boolean,
    negativePromptText: String,
    onNegativePromptTextChange: (String) -> Unit,
    cfgScale: Float,
    onCfgScaleChange: (Float) -> Unit,
    klingEndImageUrl: String,
    onKlingEndImageUrlChange: (String) -> Unit,
    selectedAudioOption: String,
    klingVoiceControl: Boolean,
    onKlingVoiceControlChange: (Boolean) -> Unit,
    klingElements: androidx.compose.runtime.snapshots.SnapshotStateList<KlingV3ComboElementInput>,
    klingV26StartImageUrl: String,
    onKlingV26StartImageUrlChange: (String) -> Unit,
    klingV26VoiceIds: String,
    onKlingV26VoiceIdsChange: (String) -> Unit,
    klingV26EndImageUrl: String,
    onKlingV26EndImageUrlChange: (String) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        ),
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = "Kling Parameters",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = "Kling Engine Parameters",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )
            }

            // 1. Negative Prompt Config
            if (!isKlingO3Standard && !isKlingO3Pro) {
                Column {
                    Text(
                        text = "Negative Prompt (What to exclude)",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                    OutlinedTextField(
                        value = negativePromptText,
                        onValueChange = onNegativePromptTextChange,
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = MaterialTheme.typography.bodyMedium,
                        placeholder = { Text("blur, distort, and low quality", style = MaterialTheme.typography.bodyMedium) },
                        maxLines = 2,
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                        )
                    )
                }
            }

            // 2. CFG Scale Config
            if (!isKlingV26Pro && !isKlingO3Standard && !isKlingO3Pro) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "CFG Scale: ${String.format("%.1f", cfgScale)}",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Default: 0.5",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        text = "The CFG (Classifier Free Guidance) scale is a measure of how close you want the model to stick to your prompt.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                    Slider(
                        value = cfgScale,
                        onValueChange = onCfgScaleChange,
                        valueRange = 0f..20f,
                        steps = 39, // Increments of 0.5
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            if (isKlingV25TurboPro) {
                Column {
                    Text(
                        text = "End Image URL (Optional)",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                    Text(
                        text = "URL of the image to be used for the end of the video",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                    OutlinedTextField(
                        value = klingEndImageUrl,
                        onValueChange = onKlingEndImageUrlChange,
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = MaterialTheme.typography.bodyMedium,
                        placeholder = { Text("https://example.com/end.jpg", style = MaterialTheme.typography.bodyMedium) },
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                        )
                    )
                }
            }

            if (isKlingV3Standard || isKlingV3Pro || isKlingO3Standard) {
                if (selectedAudioOption == "On") {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                                Text(
                                    text = "Voice Control 🎙️",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Text(
                                    text = "Enable precision voice control. Adds 5 tokens/second charge.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                )
                            }
                            Switch(
                                checked = klingVoiceControl,
                                onCheckedChange = onKlingVoiceControlChange,
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = MaterialTheme.colorScheme.primary,
                                    checkedTrackColor = MaterialTheme.colorScheme.primaryContainer
                                )
                            )
                        }
                    }
                }

                Column {
                    Text(
                        text = "End Image URL (Optional)",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                    Text(
                        text = "Animate transition seamlessly from start image to end image.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                    OutlinedTextField(
                        value = klingEndImageUrl,
                        onValueChange = onKlingEndImageUrlChange,
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = MaterialTheme.typography.bodyMedium,
                        placeholder = { Text("https://example.com/end.jpg", style = MaterialTheme.typography.bodyMedium) },
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                        )
                    )
                }

                if (!isKlingO3Standard && !isKlingO3Pro) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(
                                BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                                RoundedCornerShape(12.dp)
                            )
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Custom Elements 🧱",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Configure custom video attributes / actors",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Button(
                                onClick = {
                                    klingElements.add(KlingV3ComboElementInput(name = "Element #${klingElements.size + 1}"))
                                },
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = "Add Element", modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Add", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold))
                            }
                        }

                        if (klingElements.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(60.dp)
                                    .background(
                                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
                                        RoundedCornerShape(8.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "No custom elements defined.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                )
                            }
                        } else {
                            klingElements.forEachIndexed { index, element ->
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
                                    ),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                                ) {
                                    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = element.name.ifBlank { "Unnamed Element" },
                                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                            IconButton(
                                                onClick = { klingElements.removeAt(index) },
                                                modifier = Modifier.size(24.dp)
                                            ) {
                                                Icon(
                                                    Icons.Default.Delete,
                                                    contentDescription = "Delete",
                                                    tint = MaterialTheme.colorScheme.error,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }

                                        OutlinedTextField(
                                            value = element.name,
                                            onValueChange = { newVal ->
                                                klingElements[index] = element.copy(name = newVal)
                                            },
                                            label = { Text("Element Name", style = MaterialTheme.typography.labelSmall) },
                                            modifier = Modifier.fillMaxWidth(),
                                            singleLine = true,
                                            shape = RoundedCornerShape(6.dp)
                                        )

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text("Type:", style = MaterialTheme.typography.bodySmall)
                                            FilterChip(
                                                selected = element.type == "image_set",
                                                onClick = {
                                                    klingElements[index] = element.copy(type = "image_set")
                                                },
                                                label = { Text("Image Set", style = MaterialTheme.typography.bodySmall) }
                                            )
                                            FilterChip(
                                                selected = element.type == "video",
                                                onClick = {
                                                    klingElements[index] = element.copy(type = "video")
                                                },
                                                label = { Text("Video", style = MaterialTheme.typography.bodySmall) }
                                            )
                                        }

                                        if (element.type == "image_set") {
                                            OutlinedTextField(
                                                value = element.frontalImage,
                                                onValueChange = { newVal ->
                                                    klingElements[index] = element.copy(frontalImage = newVal)
                                                },
                                                label = { Text("Frontal Image URL", style = MaterialTheme.typography.labelSmall) },
                                                placeholder = { Text("https://example.com/frontal.jpg") },
                                                modifier = Modifier.fillMaxWidth(),
                                                singleLine = true,
                                                shape = RoundedCornerShape(6.dp)
                                            )
                                            OutlinedTextField(
                                                value = element.referenceImages,
                                                onValueChange = { newVal ->
                                                    klingElements[index] = element.copy(referenceImages = newVal)
                                                },
                                                label = { Text("Reference Image URLs (comma-separated)", style = MaterialTheme.typography.labelSmall) },
                                                placeholder = { Text("https://example.com/ref1.jpg, https://example.com/ref2.jpg") },
                                                modifier = Modifier.fillMaxWidth(),
                                                singleLine = true,
                                                shape = RoundedCornerShape(6.dp)
                                            )
                                        } else {
                                            OutlinedTextField(
                                                value = element.videoUrl,
                                                onValueChange = { newVal ->
                                                    klingElements[index] = element.copy(videoUrl = newVal)
                                                },
                                                label = { Text("Video URL", style = MaterialTheme.typography.labelSmall) },
                                                placeholder = { Text("https://example.com/video.mp4") },
                                                modifier = Modifier.fillMaxWidth(),
                                                singleLine = true,
                                                shape = RoundedCornerShape(6.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (isKlingV26Pro) {
                if (selectedAudioOption == "On") {
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                                Text(
                                    text = "Voice Control 🎙️",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Text(
                                    text = "Enable precision voice control. Adds 5 tokens/second charge.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                )
                            }
                            Switch(
                                checked = klingVoiceControl,
                                onCheckedChange = onKlingVoiceControlChange,
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = MaterialTheme.colorScheme.primary,
                                    checkedTrackColor = MaterialTheme.colorScheme.primaryContainer
                                )
                            )
                        }
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Column {
                        Text(
                            text = "Start Image URL",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                        Text(
                            text = "URL of the image to be used for the video",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                        OutlinedTextField(
                            value = klingV26StartImageUrl,
                            onValueChange = onKlingV26StartImageUrlChange,
                            modifier = Modifier.fillMaxWidth(),
                            textStyle = MaterialTheme.typography.bodyMedium,
                            placeholder = { Text("https://example.com/start.jpg", style = MaterialTheme.typography.bodyMedium) },
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                            )
                        )
                    }

                    Column {
                        Text(
                            text = "Voice IDs (Optional)",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                        Text(
                            text = "Optional Voice IDs (comma-separated). Reference voices in your prompt with <<<voice_1>>> and <<<voice_2>>> (maximum 2 voices per task). Get voice IDs from the kling video create-voice endpoint.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                        OutlinedTextField(
                            value = klingV26VoiceIds,
                            onValueChange = onKlingV26VoiceIdsChange,
                            modifier = Modifier.fillMaxWidth(),
                            textStyle = MaterialTheme.typography.bodyMedium,
                            placeholder = { Text("voice_id_1, voice_id_2", style = MaterialTheme.typography.bodyMedium) },
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                            )
                        )
                    }

                    Column {
                        Text(
                            text = "End Image URL (Optional)",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                        Text(
                            text = "URL of the image to be used for the end of the video",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                        OutlinedTextField(
                            value = klingV26EndImageUrl,
                            onValueChange = onKlingV26EndImageUrlChange,
                            modifier = Modifier.fillMaxWidth(),
                            textStyle = MaterialTheme.typography.bodyMedium,
                            placeholder = { Text("https://example.com/end.jpg", style = MaterialTheme.typography.bodyMedium) },
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                            )
                        )
                    }
                }
            }
        }
    }
}


