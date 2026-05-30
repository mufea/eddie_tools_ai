package com.example.ui.chat

import android.app.Activity
import android.content.Intent
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.horizontalScroll
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    viewModel: ChatViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var inputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val context = LocalContext.current
    var activeTab by remember { mutableStateOf("chat") } // "chat" or "image_gen"

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
                        text = "Eddie Chat",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = (-0.5).sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Local database & customized assistant",
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
                        label = { Text("Eddie Chat Assistant", fontWeight = FontWeight.SemiBold) },
                        selected = activeTab == "chat",
                        onClick = {
                            activeTab = "chat"
                            scope.launch { drawerState.close() }
                        }
                    )

                    NavigationDrawerItem(
                        icon = { Text("🎨", style = TextStyle(fontSize = 16.sp)) },
                        label = { Text("Eddie Image Studio", fontWeight = FontWeight.SemiBold) },
                        selected = activeTab == "image_gen",
                        onClick = {
                            activeTab = "image_gen"
                            scope.launch { drawerState.close() }
                        }
                    )

                    NavigationDrawerItem(
                        icon = { Text("🎬", style = TextStyle(fontSize = 16.sp)) },
                        label = { Text("Eddie Video Studio", fontWeight = FontWeight.SemiBold) },
                        selected = activeTab == "video_gen",
                        onClick = {
                            activeTab = "video_gen"
                            scope.launch { drawerState.close() }
                        }
                    )

                    NavigationDrawerItem(
                        icon = { Text("💻", style = TextStyle(fontSize = 16.sp)) },
                        label = { Text("Eddie AI Code Studio", fontWeight = FontWeight.SemiBold) },
                        selected = activeTab == "ai_code",
                        onClick = {
                            activeTab = "ai_code"
                            scope.launch { drawerState.close() }
                        }
                    )

                    NavigationDrawerItem(
                        icon = { Text("🎵", style = TextStyle(fontSize = 16.sp)) },
                        label = { Text("Eddie AI Music Studio", fontWeight = FontWeight.SemiBold) },
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
                        label = { Text("Eddie Voice Assistant", fontWeight = FontWeight.SemiBold) },
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
                        icon = { Text("📄", style = TextStyle(fontSize = 16.sp)) },
                        label = { Text("Document & PDF Summary", fontWeight = FontWeight.SemiBold) },
                        selected = activeTab == "doc_intelligence",
                        onClick = {
                            activeTab = "doc_intelligence"
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
                        label = { Text("Eddie Smart Memory") },
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
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
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
                                            text = "${uiState.selectedModel.provider} • ${uiState.selectedModel.displayName}",
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
                                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold
                                                                ),
                                                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                                            )
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
                                                                    color = Color.White,
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
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.background,
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
                                    TypingIndicator()
                                }
                            }
                        }
                    }

                    if (uiState.isSearchingWeb) {
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
                        onWebSearchToggle = { viewModel.setWebSearchEnabled(it) }
                    )
                }
            } else if (activeTab == "image_gen") {
                EddieImageGeneratorView(
                    uiState = uiState,
                    paddingValues = paddingValues,
                    onGenerate = { prompt, style, ratio ->
                        viewModel.generateImage(prompt, style, ratio, onSubscriptionRequired = {
                            showSubscriptionDialog = true
                            Toast.makeText(context, "Free limit reached (max 3) or Eddie Premium is required for this style and ratio!", Toast.LENGTH_LONG).show()
                        })
                    },
                    onDeleteImage = { id ->
                        viewModel.deleteGeneratedImage(id)
                        Toast.makeText(context, "Image successfully removed from local collection.", Toast.LENGTH_SHORT).show()
                    },
                    onSaveEditedImage = { originalId, prompt, style, ratio, editedUrl ->
                        viewModel.saveEditedImage(originalId, prompt, style, ratio, editedUrl)
                    },
                    onProviderChange = { provider ->
                        viewModel.selectImageProvider(provider)
                    },
                    onModelChange = { model ->
                        viewModel.selectImageModel(model)
                    }
                )
            } else if (activeTab == "video_gen") {
                EddieVideoGeneratorView(
                    uiState = uiState,
                    paddingValues = paddingValues,
                    onGenerateVideo = { prompt, style, ratio, res, dur ->
                        viewModel.generateVideo(prompt, style, ratio, res, dur, onSubscriptionRequired = {
                            showSubscriptionDialog = true
                            Toast.makeText(context, "Free limit reached (max 2) or Eddie Premium is required for this duration, resolution, or aspect ratio!", Toast.LENGTH_LONG).show()
                        })
                    },
                    onDeleteVideo = { id ->
                        viewModel.deleteGeneratedVideo(id)
                        Toast.makeText(context, "Video successfully removed from local collection.", Toast.LENGTH_SHORT).show()
                    },
                    onSaveEditedVideo = { originalId, prompt, style, ratio, res, dur, editedUrl ->
                        viewModel.saveEditedVideo(originalId, prompt, style, ratio, res, dur, editedUrl)
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
        var tempDeepseekKey by remember { mutableStateOf(uiState.deepseekApiKey) }
        var tempClaudeKey by remember { mutableStateOf(uiState.claudeApiKey) }
        var tempXaiKey by remember { mutableStateOf(uiState.xaiApiKey) }

        AlertDialog(
            onDismissRequest = { showApiKeyDialog = false },
            title = {
                Text(
                    text = "🔑 Multi-Provider API Key Manager",
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
                        text = "Enter your custom API Keys below. If left blank (for OpenAI & DeepSeek), the application will automatically route connections to our free server tiers.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // Gemini Key
                    Column {
                        Text(
                            text = "Google Gemini API Key",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedTextField(
                            value = tempGeminiKey,
                            onValueChange = { tempGeminiKey = it },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("Default Active (AI Studio Key)") },
                            singleLine = true,
                            textStyle = MaterialTheme.typography.bodySmall
                        )
                    }

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
                    }

                    // Claude Key
                    Column {
                        Text(
                            text = "Anthropic Claude API Key (Claude Sonet & Haiku)",
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
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.updateProviderApiKey("Gemini", tempGeminiKey)
                        viewModel.updateProviderApiKey("OpenAI", tempOpenaiKey)
                        viewModel.updateProviderApiKey("DeepSeek", tempDeepseekKey)
                        viewModel.updateProviderApiKey("Claude", tempClaudeKey)
                        viewModel.updateProviderApiKey("XAI", tempXaiKey)
                        showApiKeyDialog = false
                        Toast.makeText(context, "All API Keys saved successfully!", Toast.LENGTH_SHORT).show()
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

    val backgroundColor = if (isUser) {
        if (isDark) DarkUserBubbleBg else LightUserBubbleBg
    } else {
        MaterialTheme.colorScheme.surface
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

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start = if (isUser) 48.dp else 4.dp,
                end = if (isUser) 4.dp else 48.dp
            ),
        horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
    ) {
        Surface(
            color = backgroundColor,
            contentColor = contentColor,
            shape = shape,
            border = if (isUser) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
            tonalElevation = 0.dp,
            shadowElevation = 0.dp
        ) {
            MarkdownText(
                text = message.text,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                color = contentColor
            )
        }

        // Action Toolbar (Copy, TTS, Bookmark option for assistant responses)
        if (!isUser) {
            Row(
                modifier = Modifier.padding(top = 4.dp, start = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = onSpeakClick,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
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
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                    modifier = Modifier.height(28.dp)
                ) {
                    Text(
                        text = "📋 COPY",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                    )
                }

                TextButton(
                    onClick = onShareClick,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                    modifier = Modifier.height(28.dp)
                ) {
                    Text(
                        text = "📤 SHARE",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                    )
                }

                TextButton(
                    onClick = onBookmarkClick,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
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
fun TypingIndicator() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 4.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary))
                Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary))
                Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary))
            }
            Text(
                text = "Eddie sedang mengetik...",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
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
    onWebSearchToggle: (Boolean) -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.background,
        tonalElevation = 0.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .navigationBarsPadding(),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(16.dp)
                    )
                    .border(
                        width = 1.dp,
                        color = if (isWebSearchEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                        shape = RoundedCornerShape(16.dp)
                    )
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Speech Input Mic button
                IconButton(
                    onClick = onMicClick,
                    modifier = Modifier.size(40.dp)
                ) {
                    Text("🎙️", style = TextStyle(fontSize = 18.sp))
                }

                // Web Search (Browsing Mandiri) button
                IconButton(
                    onClick = { onWebSearchToggle(!isWebSearchEnabled) },
                    modifier = Modifier
                        .size(40.dp)
                        .background(
                            color = if (isWebSearchEnabled) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                            shape = CircleShape
                        )
                ) {
                    Text(
                        text = "🌐",
                        style = TextStyle(
                            fontSize = 18.sp,
                            color = if (isWebSearchEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }

                TextField(
                    value = inputText,
                    onValueChange = onTextChange,
                    modifier = Modifier.weight(1f),
                    placeholder = {
                        Text(
                            text = if (isWebSearchEnabled) "Ask Google in real-time..." else "Type a message...",
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
                    containerColor = if (isLoading || inputText.isBlank()) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f) else MaterialTheme.colorScheme.primary,
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
            
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isWebSearchEnabled) "🌐 Real-Time Web Search Active (Google Search)" else "⚡ Eddie AI Smart Assistant v3.0",
                    color = if (isWebSearchEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.labelSmall
                )
                Text(
                    text = "Eddie can make mistakes",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.labelSmall
                )
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

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun EddieImageGeneratorView(
    uiState: ChatUiState,
    paddingValues: PaddingValues,
    onGenerate: (String, String, String) -> Unit,
    onDeleteImage: (String) -> Unit,
    onSaveEditedImage: (String, String, String, String, String) -> Unit,
    onProviderChange: (String) -> Unit,
    onModelChange: (String) -> Unit
) {
    val context = LocalContext.current
    var prompt by remember { mutableStateOf("") }
    var selectedStyle by remember { mutableStateOf("Realistic") }
    var selectedAspectRatio by remember { mutableStateOf("1:1") }

    // Zoom popup dialog state controllers
    var selectedImageForZoom by remember { mutableStateOf<com.example.data.db.GeneratedImageEntity?>(null) }
    var editingImageItem by remember { mutableStateOf<com.example.data.db.GeneratedImageEntity?>(null) }

    val styles = listOf(
        "Realistic" to false,
        "Sketch" to false,
        "Black & White" to false,
        "Minimalist" to false,
        "3D Render" to true,
        "Cinematic" to true,
        "Cyberpunk" to true,
        "Anime" to true,
        "Pixel Art" to true,
        "Watercolor" to true,
        "Oil Painting" to true,
        "Cosmic" to true
    )

    val aspectRatios = listOf(
        "1:1" to "Square" to false,
        "16:9" to "Landscape" to true,
        "9:16" to "Portrait" to true
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Hero Header
        Column {
            Text(
                text = "Eddie AI Art Studio ✨",
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Transform descriptive words into stunning digital masterpieces in real-time.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )
                if (uiState.isSubscribed) {
                    Surface(
                        color = Color(0xFFFFB100).copy(alpha = 0.15f),
                        border = BorderStroke(1.dp, Color(0xFFFFB100)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.padding(start = 8.dp)
                    ) {
                        Text(
                            text = "🪙 ${uiState.userTokens} Tokens",
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color(0xFFFFB100)
                        )
                    }
                }
            }
        }

        // Input prompt card
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
                    placeholder = { Text("Example: A cyberpunk city under neon rain, high detail...") },
                    modifier = Modifier.fillMaxWidth().height(100.dp),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Sample Prompts pill row
                Text(
                    text = "Quick Inspiration:",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 6.dp)
                )

                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val quickPrompts = listOf(
                        "An astronaut riding a white horse on Mars, dramatic oil painting",
                        "A floating fantasy village surrounded by soft clouds, watercolor style",
                        "A cute cat wearing a wizard hat reading an old magic spell book, 3D render",
                        "A mystical forest at night filled with glowing golden fireflies"
                    )
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

        // Provider & Model Selection Card
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
                        imageVector = Icons.Default.Info, // A descriptive, present icon
                        contentDescription = "Settings Icon",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "Select Provider & Image Model",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Provider Selection label
                Text(
                    text = "Provider AI:",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Horizontal row of provider chips
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val providers = listOf("OpenAI", "Stable Diffusion", "FalAI")
                    providers.forEach { provider ->
                        val isSelected = uiState.selectedImageProvider == provider
                        FilterChip(
                            selected = isSelected,
                            onClick = { onProviderChange(provider) },
                            label = { Text(provider) },
                            leadingIcon = {
                                val icon = when (provider) {
                                    "OpenAI" -> Icons.Default.CheckCircle
                                    "Stable Diffusion" -> Icons.Default.Face
                                    else -> Icons.Default.Person
                                }
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

                // Model Selection label
                Text(
                    text = "Model:",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Horizontal row of model chips based on the selected provider
                val models = when (uiState.selectedImageProvider) {
                    "OpenAI" -> listOf("GPT Image 1.5", "GPT Image 2")
                    "Stable Diffusion" -> listOf("Stable Diffusion", "Core Diffusion", "Ultra Diffusion")
                    "FalAI" -> listOf("Flux Pro", "Flux Realism")
                    else -> emptyList()
                }

                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    models.forEach { model ->
                        val isSelected = uiState.selectedImageModel == model
                        val isPro = model in listOf("GPT Image 2", "Ultra Diffusion", "Flux Realism")
                        val badgeColor = if (uiState.isSubscribed) MaterialTheme.colorScheme.primary else Color(0xFFFFB100)

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
                                                color = Color.White,
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

        // Styles Section
        Column {
            Text(
                text = "Select Artistic Style",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                styles.forEach { (styleName, isPro) ->
                    val isSelected = selectedStyle == styleName
                    val badgeColor = if (uiState.isSubscribed) MaterialTheme.colorScheme.primary else Color(0xFFFFB100)

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
                                        color = Color.White,
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

        // Aspect Ratio Section
        Column {
            Text(
                text = "Define Image Aspect Ratio",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                aspectRatios.forEach { ratioData ->
                    val ratioPair = ratioData.first
                    val isPro = ratioData.second
                    val ratio = ratioPair.first
                    val label = ratioPair.second
                    val isSelected = selectedAspectRatio == ratio
                    val badgeColor = if (uiState.isSubscribed) MaterialTheme.colorScheme.primary else Color(0xFFFFB100)

                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { selectedAspectRatio = ratio },
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                        border = BorderStroke(
                            width = if (isSelected) 2.dp else 1.dp,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
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
                                            color = Color.White,
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

        // Action Button and Status logs
        if (uiState.imageGenIsLoading) {
            Surface(
                color = Color(0xFF1E1E1E),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Eddie Art Studio - Processing Image...",
                        color = Color.Green,
                        style = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = uiState.imageGenLog,
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
                onClick = { onGenerate(prompt, selectedStyle, selectedAspectRatio) },
                enabled = prompt.isNotBlank(),
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = "Start Painting Image ✨",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), modifier = Modifier.padding(vertical = 8.dp))

        // History Gallery Section
        Text(
            text = "Your Masterpiece Gallery Collection",
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
                        text = "Describe your dream visuals above to start your first gallery.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 24.dp)
                    )
                }
            }
        } else {
            val gridHeight = if (userImages.size <= 2) 230.dp else if (userImages.size <= 4) 460.dp else 680.dp
            Box(modifier = Modifier.fillMaxWidth().height(gridHeight)) {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(userImages) { imageItem ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
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
                                }
                                Column(modifier = Modifier.padding(8.dp)) {
                                    Text(
                                        text = imageItem.prompt,
                                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Ratio: " + imageItem.aspectRatio,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
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
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = {
                            editingImageItem = imageItem
                            selectedImageForZoom = null
                        }
                    ) {
                        Text("Edit Image 🎨")
                    }
                    Button(
                        onClick = {
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_SUBJECT, "Eddie AI Masterpiece")
                                putExtra(Intent.EXTRA_TEXT, "Look at this image I created in Eddie Art Studio:\n\nPrompt: ${imageItem.prompt}\nLink: ${imageItem.imageUrl}")
                            }
                            context.startActivity(Intent.createChooser(shareIntent, "Share Image"))
                        }
                    ) {
                        Text("Share")
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        onDeleteImage(imageItem.id)
                        selectedImageForZoom = null
                    }
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            }
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
fun VideoPlayerDialog(
    videoItem: com.example.data.db.GeneratedVideoEntity,
    onDismissRequest: () -> Unit,
    onDeleteVideo: (String) -> Unit,
    onEditVideo: (() -> Unit)? = null
) {
    val context = LocalContext.current
    var isPlaying by remember { mutableStateOf(true) }
    var currentProgress by remember { mutableStateOf(0f) }

    // Parse duration seconds from the duration string to run simulation properly
    val totalSeconds = when {
        videoItem.duration.contains("3") -> 3
        videoItem.duration.contains("5") -> 5
        videoItem.duration.contains("10") -> 10
        videoItem.duration.contains("30") -> 30
        else -> 5
    }

    LaunchedEffect(isPlaying) {
        if (isPlaying) {
            while (currentProgress < 1f) {
                delay(100)
                currentProgress += 0.1f / totalSeconds
                if (currentProgress >= 1f) {
                    currentProgress = 0f // Loop playback
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
                // Video Screen simulation container
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(240.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.Black),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                brush = androidx.compose.ui.graphics.Brush.radialGradient(
                                    colors = listOf(
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                                        Color.Black
                                    )
                                )
                            )
                    )

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.8f),
                            modifier = Modifier.size(56.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                if (isPlaying) {
                                    Text("🎬", fontSize = 24.sp)
                                } else {
                                    Text("⏸️", fontSize = 24.sp)
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = if (isPlaying) "Playing Video..." else "Stopped",
                            color = Color.Green,
                            style = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (videoItem.prompt.length > 45) videoItem.prompt.take(45) + "..." else videoItem.prompt,
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.6f),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.BottomCenter)
                            .background(Color.Black.copy(alpha = 0.6f))
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
                                    fontSize = 12.sp
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
                }

                // Metadata Section
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "Video Description:",
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
            }
        },
        confirmButton = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (onEditVideo != null) {
                    Button(
                        onClick = {
                            onEditVideo()
                            onDismissRequest()
                        }
                    ) {
                        Text("Edit Video 🎬")
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
                    }
                ) {
                    Text("Share")
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = {
                    onDeleteVideo(videoItem.id)
                    onDismissRequest()
                }
            ) {
                Text("Delete", color = MaterialTheme.colorScheme.error)
            }
        }
    )
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun EddieVideoGeneratorView(
    uiState: ChatUiState,
    paddingValues: PaddingValues,
    onGenerateVideo: (String, String, String, String, String) -> Unit,
    onDeleteVideo: (String) -> Unit,
    onSaveEditedVideo: (String, String, String, String, String, String, String) -> Unit,
    onModeChange: (String) -> Unit,
    onProviderChange: (String) -> Unit,
    onModelChange: (String) -> Unit
) {
    val context = LocalContext.current
    var prompt by remember { mutableStateOf("") }
    var selectedStyle by remember { mutableStateOf("Realistic Drone") }
    var selectedAspectRatio by remember { mutableStateOf("16:9") }
    var selectedResolution by remember { mutableStateOf("720p") }
    var selectedDuration by remember { mutableStateOf("5 seconds") }
    var selectedImageId by remember { mutableStateOf("") }

    var selectedVideoForPlay by remember { mutableStateOf<com.example.data.db.GeneratedVideoEntity?>(null) }
    var editingVideoItem by remember { mutableStateOf<com.example.data.db.GeneratedVideoEntity?>(null) }

    val styles = listOf(
        "Realistic Drone" to false,
        "Cinematic 3D" to false,
        "Cyberpunk Neon" to true,
        "Anime Epic" to true
    )

    val aspectRatios = listOf(
        "16:9" to false to "Landscape",
        "1:1" to false to "Square",
        "9:16" to true to "Shorts/TikTok",
        "21:9" to true to "Cinematic"
    )

    val resolutions = listOf(
        "480p" to false to "Standard SD",
        "720p" to false to "High HD",
        "1080p" to true to "Full HD Pro",
        "4K" to true to "Ultra HD Pro"
    )

    val durations = listOf(
        "3 seconds" to false to "Short Loop",
        "5 seconds" to false to "Medium Loop",
        "10 seconds" to true to "Extended HD",
        "30 seconds" to true to "Max Cinematic"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Column {
            Text(
                text = "Eddie AI Video Studio 🎬",
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Transform your descriptive words into high-resolution cinematic video clips powered by state-of-the-art AI servers.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )
                if (uiState.isSubscribed) {
                    Surface(
                        color = Color(0xFFFFB100).copy(alpha = 0.15f),
                        border = BorderStroke(1.dp, Color(0xFFFFB100)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.padding(start = 8.dp)
                    ) {
                        Text(
                            text = "🪙 ${uiState.userTokens} Tokens",
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color(0xFFFFB100)
                        )
                    }
                }
            }
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
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
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
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val providers = when (uiState.selectedVideoMode) {
                        "Text to Video" -> listOf("OpenAI", "Gemini", "FalAI")
                        else -> listOf("Gemini", "FalAI") // Image to Video
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
                    text = "AI Video Model:",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val models = when (uiState.selectedVideoMode) {
                        "Text to Video" -> {
                            when (uiState.selectedVideoProvider) {
                                "OpenAI" -> listOf("Sora 2", "Sora 2 pro")
                                "Gemini" -> listOf("Veo 3", "Veo 3.1")
                                "FalAI" -> listOf("Kling v2.6", "Kling v3")
                                else -> emptyList()
                            }
                        }
                        else -> { // Image to Video
                            when (uiState.selectedVideoProvider) {
                                "Gemini" -> listOf("veo 3.1", "Fast veo 3.1")
                                "FalAI" -> listOf("Kling v2.6 pro", "Kling v2.6 standard", "Kling v3 pro", "Kling v3 standard")
                                else -> emptyList()
                            }
                        }
                    }

                    models.forEach { model ->
                        val isSelected = uiState.selectedVideoModel == model
                        val isPro = model in listOf("Sora 2 pro", "Veo 3.1", "Kling v3", "Kling v2.6 pro", "Kling v3 pro")
                        val badgeColor = if (uiState.isSubscribed) MaterialTheme.colorScheme.primary else Color(0xFFFFB100)

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
                                                color = Color.White,
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

        // Image Selection for Image to Video mode
        if (uiState.selectedVideoMode == "Image to Video") {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
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

                    Spacer(modifier = Modifier.height(12.dp))

                    if (uiState.generatedImages.isEmpty()) {
                        Text(
                            text = "Your image library is empty. Please generate an image in the 'Eddie Image Studio' tab first to use it as a reference here.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        Row(
                            modifier = Modifier.horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            uiState.generatedImages.forEach { img ->
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
                    }
                }
            }
        }

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

                OutlinedTextField(
                    value = prompt,
                    onValueChange = { prompt = it },
                    placeholder = { Text("Example: Drone camera slowly gliding over tropical beach waves during a golden sunset at dusk...") },
                    modifier = Modifier.fillMaxWidth().height(100.dp),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Quick Inspiration:",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 6.dp)
                )

                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val quickPrompts = listOf(
                        "Cinematic camera slowly gliding across an ancient classical cathedral under gentle rainfall",
                        "Drone capturing majestic snow-capped mountain peaks at golden dawn",
                        "Anime character walking under falling cherry blossom trees with nostalgic backlighting",
                        "Legendary car drifting through neon-lit night cyberpunk city streets with thick smoke"
                    )
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

        Column {
            Text(
                text = "Select Cinematic Style",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                styles.forEach { (styleName, isPro) ->
                    val isSelected = selectedStyle == styleName
                    val badgeColor = if (uiState.isSubscribed) MaterialTheme.colorScheme.primary else Color(0xFFFFB100)

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
                                        color = Color.White,
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

        Column {
            Text(
                text = "Video Aspect Ratio",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                aspectRatios.forEach { item ->
                    val ratioPair = item.first
                    val ratio = ratioPair.first
                    val isPro = ratioPair.second
                    val label = item.second
                    val isSelected = selectedAspectRatio == ratio
                    val badgeColor = if (uiState.isSubscribed) MaterialTheme.colorScheme.primary else Color(0xFFFFB100)

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
                                            color = Color.White,
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

        Column {
            Text(
                text = "Video Resolution",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                resolutions.forEach { item ->
                    val pair = item.first
                    val res = pair.first
                    val isPro = pair.second
                    val label = item.second
                    val isSelected = selectedResolution == res
                    val badgeColor = if (uiState.isSubscribed) MaterialTheme.colorScheme.primary else Color(0xFFFFB100)

                    Surface(
                        modifier = Modifier
                            .width(135.dp)
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
                                            color = Color.White,
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

        Column {
            Text(
                text = "Video Duration",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                durations.forEach { item ->
                    val pair = item.first
                    val dur = pair.first
                    val isPro = pair.second
                    val label = item.second
                    val isSelected = selectedDuration == dur
                    val badgeColor = if (uiState.isSubscribed) MaterialTheme.colorScheme.primary else Color(0xFFFFB100)

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
                                            color = Color.White,
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

        if (uiState.videoGenIsLoading) {
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
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = uiState.videoGenLog,
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
                onClick = { onGenerateVideo(prompt, selectedStyle, selectedAspectRatio, selectedResolution, selectedDuration) },
                enabled = prompt.isNotBlank(),
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = "Start AI Video Render 🚀",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
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
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                uiState.generatedVideos.forEach { video ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
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
                            }

                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = video.prompt,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
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

    selectedVideoForPlay?.let { video ->
        VideoPlayerDialog(
            videoItem = video,
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

    // Audio player state
    var mediaPlayer by remember { mutableStateOf<android.media.MediaPlayer?>(null) }
    var playingMusicId by remember { mutableStateOf<String?>(null) }
    var isAudioPlaying by remember { mutableStateOf(false) }
    var playPercent by remember { mutableStateOf(0f) }

    var editingMusicItem by remember { mutableStateOf<com.example.data.db.GeneratedMusicEntity?>(null) }

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
                val newMp = android.media.MediaPlayer().apply {
                    setDataSource(url)
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
        "Lo-Fi" to true to "Relaxing & Retro Beat",
        "Jazz" to true to "Mellow & Swing",
        "EDM" to true to "Synthesizer & Nightclub",
        "Classical" to true to "Symphony & Orchestra",
        "Ambient" to true to "Deep Atmospheric & Meditation",
        "Hip-Hop" to true to "Urban Beat & Bass"
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
        "Fast" to "Fast & Energetic"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Column {
            Text(
                text = "Eddie AI Music Studio 🎵",
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Transform your imaginative descriptions into hybrid hi-fi instrumental harmony powered by state-of-the-art multi-genre synthetic audio.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )
                if (uiState.isSubscribed) {
                    Surface(
                        color = Color(0xFFFFB100).copy(alpha = 0.15f),
                        border = BorderStroke(1.dp, Color(0xFFFFB100)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.padding(start = 8.dp)
                    ) {
                        Text(
                            text = "🪙 ${uiState.userTokens} Tokens",
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color(0xFFFFB100)
                        )
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
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
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
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val models = when (uiState.selectedMusicProvider) {
                        "Gemini" -> listOf("Lyria 3 pro", "Lyria 3 clip")
                        else -> listOf("Suno v3.5", "Suno v4 pro") // Suno AI
                    }

                    models.forEach { model ->
                        val isSelected = uiState.selectedMusicModel == model
                        val isPro = model in listOf("Lyria 3 pro", "Suno v4 pro")
                        val badgeColor = if (uiState.isSubscribed) MaterialTheme.colorScheme.primary else Color(0xFFFFB100)

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
                                                color = Color.White,
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
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Quick Inspiration:",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 6.dp)
                )

                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val quickPrompts = listOf(
                        "Chill song with soft acoustic guitar strumming by the campfire",
                        "Peaceful classical piano melody with a gentle oceanic breeze",
                        "Thumping heavy bass blended with an energetic retro neon synth",
                        "Lo-fi hip-hop beat with vinyl crackle and cozy window rain sounds"
                    )
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

        // 1. Genre selection
        Column {
            Text(
                text = "Select Popular Genre",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                genres.forEach { item ->
                    val pair = item.first
                    val genreName = pair.first
                    val isPro = pair.second
                    val subtitle = item.second
                    val isSelected = selectedGenre == genreName
                    val badgeColor = if (uiState.isSubscribed) MaterialTheme.colorScheme.primary else Color(0xFFFFB100)

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
                                            color = Color.White,
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

            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                durations.forEach { item ->
                    val pair = item.first
                    val durLabel = pair.first
                    val isPro = pair.second
                    val desc = item.second
                    val isSelected = selectedDuration == durLabel
                    val badgeColor = if (uiState.isSubscribed) MaterialTheme.colorScheme.primary else Color(0xFFFFB100)

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
                                            color = Color.White,
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

            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
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

    editingMusicItem?.let { item ->
        EddieMusicEditorDialog(
            musicItem = item,
            onDismissRequest = { editingMusicItem = null },
            onSaveEditedMusic = onSaveEditedMusic
        )
    }
}

@Composable
fun EddieImageEditorDialog(
    imageItem: com.example.data.db.GeneratedImageEntity,
    onDismissRequest: () -> Unit,
    onSaveEditedImage: (String, String, String, String, String) -> Unit
) {
    val context = LocalContext.current
    var brightness by remember { mutableStateOf(0f) } // range -0.5 to 0.5
    var selectedTone by remember { mutableStateOf("Original") }
    var customText by remember { mutableStateOf("") }
    var textPosition by remember { mutableStateOf("Bottom") }
    var selectedTextColor by remember { mutableStateOf("White") }

    // Rendering / saving simulation state
    var isSaving by remember { mutableStateOf(false) }
    var savingLog by remember { mutableStateOf("") }
    var savingProgress by remember { mutableStateOf(0f) }

    val scope = rememberCoroutineScope()

    val tones = listOf("Original", "Cyberpunk", "Vintage Lofi", "Emerald Green", "Cinema Warm", "B&W", "Cosmic Violet")
    val positions = listOf("Top", "Center", "Bottom")
    val textColors = listOf("White", "Black", "Gold", "Neon Green", "Hot Pink")

    AlertDialog(
        onDismissRequest = { if (!isSaving) onDismissRequest() },
        title = {
            Text(
                text = "Eddie AI Image Editing Studio 🎨",
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
                // Interactive Preview Box
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.DarkGray),
                    contentAlignment = Alignment.Center
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        AsyncImage(
                            model = imageItem.imageUrl,
                            contentDescription = "Preview",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Fit,
                            colorFilter = if (selectedTone == "B&W") androidx.compose.ui.graphics.ColorFilter.colorMatrix(androidx.compose.ui.graphics.ColorMatrix().apply { setToSaturation(0f) }) else null
                        )
                        // Visual feedback overlay for Brightness
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
                        // Visual feedback overlay for Tone Filters
                        if (selectedTone == "Cyberpunk") {
                            Box(modifier = Modifier.fillMaxSize().background(Color.Magenta.copy(alpha = 0.22f)))
                        } else if (selectedTone == "Vintage Lofi") {
                            Box(modifier = Modifier.fillMaxSize().background(Color(0xFFE5A93C).copy(alpha = 0.25f)))
                        } else if (selectedTone == "Emerald Green") {
                            Box(modifier = Modifier.fillMaxSize().background(Color.Green.copy(alpha = 0.18f)))
                        } else if (selectedTone == "Cinema Warm") {
                            Box(modifier = Modifier.fillMaxSize().background(Color(0xFFFF8A00).copy(alpha = 0.18f)))
                        } else if (selectedTone == "Cosmic Violet") {
                            Box(modifier = Modifier.fillMaxSize().background(Color(0xFF8B00FF).copy(alpha = 0.22f)))
                        }

                        // Floating custom text overlay
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
                                        else -> Color(0xFFFF1493)
                                    },
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier
                                        .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }

                // Brightness adjustment
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Brightness",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "${(brightness * 100).toInt()}%",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Slider(
                        value = brightness,
                        onValueChange = { brightness = it },
                        valueRange = -0.5f..0.5f,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Tones/Filters list
                Column {
                    Text(
                        text = "Tone Filters",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        tones.forEach { tone ->
                            val isSelected = selectedTone == tone
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                                border = BorderStroke(1.dp, if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
                                modifier = Modifier.clickable { selectedTone = tone }
                            ) {
                                Text(
                                    text = tone,
                                    style = MaterialTheme.typography.labelMedium,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }

                // Overlays/Watermark Text Option
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Insert Text / Watermark",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                    )
                    OutlinedTextField(
                        value = customText,
                        onValueChange = { customText = it },
                        placeholder = { Text("Enter watermark...") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 1,
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp)
                    )

                    if (customText.isNotBlank()) {
                        // Text Position and Color Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Text Position",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    modifier = Modifier.padding(bottom = 4.dp)
                                )
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    positions.forEach { pos ->
                                        val isSel = textPosition == pos
                                        Box(
                                            modifier = Modifier
                                                .background(
                                                    if (isSel) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                                    RoundedCornerShape(4.dp)
                                                )
                                                .clickable { textPosition = pos }
                                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                        ) {
                                            Text(
                                                text = pos,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = if (isSel) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                    }
                                }
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Text Color",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    modifier = Modifier.padding(bottom = 4.dp)
                                )
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
                                                    RoundedCornerShape(4.dp)
                                                )
                                                .clickable { selectedTextColor = colorStr }
                                                .padding(horizontal = 6.dp, vertical = 4.dp)
                                        ) {
                                            Text(
                                                text = colorStr,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = if (isSel) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                    }
                                }
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
                            savingLog = "> Reading image hifi pixel matrix...\n"
                            savingProgress = 0.1f
                            delay(800)
                            savingLog += "> Calibrating brightness: ${(brightness * 100).toInt()}%\n"
                            savingProgress = 0.35f
                            delay(850)
                            savingLog += "> Applying tone filter: $selectedTone\n"
                            savingProgress = 0.6f
                            delay(900)
                            if (customText.isNotBlank()) {
                                savingLog += "> Inserting watermark: '$customText' at position $textPosition ($selectedTextColor)\n"
                                savingProgress = 0.85f
                                delay(850)
                            }
                            savingLog += "> Hybrid rendering complete! Saving edited copy..."
                            savingProgress = 1.0f
                            delay(600)

                            val styleSuffix = if (selectedTone != "Original") " [$selectedTone]" else ""
                            val watermarkSuffix = if (customText.isNotBlank()) " | Watermarked '$customText'" else ""
                            val newPrompt = "${imageItem.prompt} (Edited - Brightness ${(brightness * 100).toInt()}%$styleSuffix$watermarkSuffix)"

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
fun EddieVideoEditorDialog(
    videoItem: com.example.data.db.GeneratedVideoEntity,
    onDismissRequest: () -> Unit,
    onSaveEditedVideo: (String, String, String, String, String, String, String) -> Unit
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
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
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
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
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
                                videoItem.videoUrl
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
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
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
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
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

