package com.example.ui.chat

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun EddieLanguageLearningView(
    uiState: ChatUiState,
    paddingValues: PaddingValues,
    onLangChange: (String) -> Unit,
    onTopicChange: (String) -> Unit,
    onLevelChange: (String) -> Unit,
    onMicClick: () -> Unit,
    onClearChat: () -> Unit,
    onStopTts: () -> Unit,
    onSendMessage: (String) -> Unit,
    onPlayTts: (String) -> Unit
) {
    val context = LocalContext.current
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    var textInput by remember { mutableStateOf("") }
    val isKeyboardVisible = WindowInsets.isImeVisible
    
    // Auto scroll bottom when chat updates or loading state toggles
    LaunchedEffect(uiState.languageChatLog.size, uiState.languageIsLoading) {
        if (uiState.languageChatLog.isNotEmpty()) {
            val targetIndex = if (uiState.languageIsLoading) uiState.languageChatLog.size else uiState.languageChatLog.size - 1
            if (targetIndex >= 0) {
                listState.animateScrollToItem(targetIndex)
            }
        }
    }

    LaunchedEffect(isKeyboardVisible) {
        if (isKeyboardVisible && uiState.languageChatLog.isNotEmpty()) {
            val targetIndex = if (uiState.languageIsLoading) uiState.languageChatLog.size else uiState.languageChatLog.size - 1
            if (targetIndex >= 0) {
                listState.animateScrollToItem(targetIndex)
            }
        }
    }

    LaunchedEffect(textInput) {
        if (textInput.isNotEmpty() && uiState.languageChatLog.isNotEmpty()) {
            val targetIndex = if (uiState.languageIsLoading) uiState.languageChatLog.size else uiState.languageChatLog.size - 1
            if (targetIndex >= 0) {
                listState.animateScrollToItem(targetIndex)
            }
        }
    }

    val languages = listOf(
        "Spanish" to "🇪🇸",
        "French" to "🇫🇷",
        "Japanese" to "🇯🇵",
        "German" to "🇩🇪",
        "Korean" to "🇰🇷",
        "Chinese Mandarin" to "🇨🇳",
        "English" to "🇬🇧"
    )

    val topics = listOf(
        "Casual Greetings" to "💬",
        "At the Cafe" to "☕",
        "Hotel Check-In" to "🏨",
        "Asking for Directions" to "🗺️",
        "Job Interview" to "💼",
        "Travel Flight" to "✈️"
    )

    val levels = listOf(
        "Beginner",
        "Intermediate",
        "Advanced"
    )

    Scaffold(
        modifier = Modifier.padding(paddingValues),
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .imePadding()
                .padding(innerPadding)
                .padding(12.dp)
        ) {
            if (!isKeyboardVisible) {
                // Header Hero Banner Card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .background(MaterialTheme.colorScheme.primary, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("🗣️", fontSize = 22.sp)
                            }
                            Column {
                                Text(
                                    text = "Conversational Partner Eddie",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Text(
                                    text = "Improve speaking, pronunciation & grammar in real time!",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                                )
                            }
                        }
                    }
                }

                // Learning Setup Accordion Panel inside a Scrollable Row
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                        .padding(10.dp)
                ) {
                    // Topic Selector
                    Text(
                        text = "1. Select Language Partner Profile:",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    // Target Language Horizontal Pills
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        languages.forEach { (lang, flag) ->
                            val isSelected = uiState.languageSelectedLocale == lang
                            FilterChip(
                                selected = isSelected,
                                onClick = { onLangChange(lang) },
                                label = { Text("$flag $lang") },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Scenario Selector
                    Text(
                        text = "2. Active Conversation Scenario:",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        topics.forEach { (topic, emoji) ->
                            val isSelected = uiState.languageConversationTopic == topic
                            FilterChip(
                                selected = isSelected,
                                onClick = { onTopicChange(topic) },
                                label = { Text("$emoji $topic") },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.secondary,
                                    selectedLabelColor = MaterialTheme.colorScheme.onSecondary
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Skill Proficiency Level
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "3. Level:",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            levels.forEach { level ->
                                val isSelected = uiState.languagePracticeLevel == level
                                val chipBg = if (isSelected) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.surfaceVariant
                                val chipText = if (isSelected) MaterialTheme.colorScheme.onTertiary else MaterialTheme.colorScheme.onSurfaceVariant
                                
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(chipBg)
                                        .clickable { onLevelChange(level) }
                                        .padding(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = level,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = chipText
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Dynamics Suggested vocabulary hints card
            if (uiState.languageSuggestedVocabulary.isNotEmpty()) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f))
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "💡 Useful Practice Phrases (Tap to Use):",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            uiState.languageSuggestedVocabulary.forEach { (foreign, english) ->
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                                        .clickable {
                                            textInput = foreign
                                        }
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Column {
                                        Text(
                                            text = foreign,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = english,
                                            fontSize = 9.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Central Dialogue / Chat Transcript Log
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f))
                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f), RoundedCornerShape(16.dp))
            ) {
                if (uiState.languageChatLog.isEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "👋 Start your Conversation!",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Send a text greeting or hold the mic to reply to Eddie. Eddie will speak in ${uiState.languageSelectedLocale}, assist you with pronunciation spelling, and gently correct your errors!",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(uiState.languageChatLog) { chatMessage ->
                            val isUser = chatMessage.isUser
                            val alignment = if (isUser) Alignment.End else Alignment.Start
                            val bubbleColor = if (isUser) {
                                MaterialTheme.colorScheme.primaryContainer
                            } else {
                                MaterialTheme.colorScheme.secondaryContainer
                            }
                            val textColor = if (isUser) {
                                MaterialTheme.colorScheme.onPrimaryContainer
                            } else {
                                MaterialTheme.colorScheme.onSecondaryContainer
                            }

                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = alignment
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(0.9f),
                                    horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
                                    verticalAlignment = Alignment.Top
                                ) {
                                    if (!isUser) {
                                        Text(
                                            text = "👨‍🏫",
                                            modifier = Modifier.padding(end = 6.dp, top = 4.dp),
                                            fontSize = 18.sp
                                        )
                                    }

                                    Column(
                                        modifier = Modifier
                                            .clip(
                                                RoundedCornerShape(
                                                    topStart = 16.dp,
                                                    topEnd = 16.dp,
                                                    bottomStart = if (isUser) 16.dp else 4.dp,
                                                    bottomEnd = if (isUser) 4.dp else 16.dp
                                                )
                                            )
                                            .background(bubbleColor)
                                            .padding(12.dp)
                                    ) {
                                        // Main Script / Message Text
                                        Text(
                                            text = chatMessage.message,
                                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                            color = textColor
                                        )

                                        // Phonetic guides / Pinyin etc. (Only for foreign assistant responses)
                                        if (!isUser && chatMessage.phoneticSpelling != null) {
                                            Divider(
                                                color = textColor.copy(alpha = 0.15f),
                                                modifier = Modifier.padding(vertical = 4.dp)
                                            )
                                            Text(
                                                text = "🗣️ Pronounce: ${chatMessage.phoneticSpelling}",
                                                style = MaterialTheme.typography.bodySmall.copy(
                                                    fontStyle = FontStyle.Italic,
                                                    fontFamily = FontFamily.Monospace
                                                ),
                                                color = textColor.copy(alpha = 0.85f),
                                                fontSize = 11.sp
                                            )
                                        }

                                        // Expandable Translation display
                                        if (!isUser && chatMessage.translationInEnglish != null) {
                                            var revealTranslation by remember { mutableStateOf(false) }
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(Color.White.copy(alpha = 0.25f))
                                                    .clickable { revealTranslation = !revealTranslation }
                                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                                            ) {
                                                Text(
                                                    text = if (revealTranslation) "Hide English Translation 👁️" else "Show English Translation 👁️",
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = textColor
                                                )
                                            }
                                            AnimatedVisibility(visible = revealTranslation) {
                                                Text(
                                                    text = chatMessage.translationInEnglish,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = textColor.copy(alpha = 0.9f),
                                                    modifier = Modifier.padding(top = 4.dp)
                                                )
                                            }
                                        }

                                        // Pronunciation tip card
                                        if (!isUser && chatMessage.pronunciationTips != null) {
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = "💡 Accent Tip: ${chatMessage.pronunciationTips}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = textColor.copy(alpha = 0.75f),
                                                fontSize = 10.sp
                                            )
                                        }

                                        // Speak/Audio Re-play Trigger
                                        if (!isUser) {
                                            Spacer(modifier = Modifier.height(6.dp))
                                            IconButton(
                                                onClick = { onPlayTts(chatMessage.message) },
                                                modifier = Modifier
                                                    .size(24.dp)
                                                    .background(Color.White.copy(alpha = 0.3f), CircleShape)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.PlayArrow,
                                                    contentDescription = "Speak Aloud",
                                                    modifier = Modifier.size(14.dp),
                                                    tint = textColor
                                                )
                                            }
                                        }

                                        // Grammar / Word suggestions and helpful corrections (only for user inputs)
                                        if (isUser && chatMessage.corrections != null) {
                                            Spacer(modifier = Modifier.height(6.dp))
                                            Card(
                                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.35f)),
                                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.25f))
                                            ) {
                                                Column(modifier = Modifier.padding(6.dp)) {
                                                    Text(
                                                        text = "⭐ Eddie's Grammar Feedback:",
                                                        fontSize = 9.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = MaterialTheme.colorScheme.onErrorContainer
                                                    )
                                                    Text(
                                                        text = chatMessage.corrections,
                                                        fontSize = 11.sp,
                                                        color = MaterialTheme.colorScheme.onSurface
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    if (isUser) {
                                        Text(
                                            text = "👤",
                                            modifier = Modifier.padding(start = 6.dp, top = 4.dp),
                                            fontSize = 18.sp
                                        )
                                    }
                                }
                            }
                        }
                        
                        // Speak/Loading response wave indicator
                        if (uiState.languageIsLoading) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(8.dp),
                                    contentAlignment = Alignment.CenterStart
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                            .padding(horizontal = 12.dp, vertical = 6.dp)
                                    ) {
                                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                        Text("Eddie is preparing response & speech...", style = MaterialTheme.typography.bodySmall)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Keyboard/Speak Action control panel inputs
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Clear state button
                IconButton(
                    onClick = onClearChat,
                    modifier = Modifier.size(46.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Reset Chat",
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f)
                    )
                }

                // Text Input area
                TextField(
                    value = textInput,
                    onValueChange = { textInput = it },
                    placeholder = { Text("Write in target language...", fontSize = 13.sp) },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                        disabledContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    ),
                    shape = RoundedCornerShape(24.dp),
                    trailingIcon = {
                        if (textInput.isNotEmpty()) {
                            IconButton(onClick = { textInput = "" }) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = "Clear input",
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    },
                    textStyle = TextStyle(fontSize = 13.sp),
                    singleLine = true
                )

                // Trigger Send OR mic record button
                if (textInput.isBlank()) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(
                                        MaterialTheme.colorScheme.primary,
                                        MaterialTheme.colorScheme.tertiary
                                    )
                                )
                            )
                            .clickable { onMicClick() },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (uiState.languageIsSpeakActive) "🔊" else "🎙️",
                            fontSize = 20.sp
                        )
                    }
                } else {
                    IconButton(
                        onClick = {
                            onSendMessage(textInput)
                            textInput = ""
                        },
                        modifier = Modifier
                            .size(48.dp)
                            .background(MaterialTheme.colorScheme.primary, CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Send,
                            contentDescription = "Send",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}
