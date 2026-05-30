package com.example.ui.chat

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EddieConversationalVoiceView(
    uiState: ChatUiState,
    paddingValues: PaddingValues,
    onMicClick: () -> Unit,
    onClearChat: () -> Unit,
    onStopTts: () -> Unit
) {
    val context = LocalContext.current
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    // Animation for voice wave
    val infiniteTransition = rememberInfiniteTransition(label = "VoicePulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = if (uiState.voiceIsSpeakActive) 1.25f else if (uiState.voiceIsLoading) 1.15f else 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = if (uiState.voiceIsSpeakActive) 600 else 1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "PulseScale"
    )

    val waveAlpha by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = if (uiState.voiceIsSpeakActive) 0.6f else if (uiState.voiceIsLoading) 0.4f else 0.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = if (uiState.voiceIsSpeakActive) 600 else 1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "WaveAlpha"
    )

    // Auto scroll bottom
    LaunchedEffect(uiState.voiceChatLog.size) {
        if (uiState.voiceChatLog.isNotEmpty()) {
            listState.animateScrollToItem(uiState.voiceChatLog.size - 1)
        }
    }

    Scaffold(
        modifier = Modifier.padding(paddingValues),
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header Info Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.1f)),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "Info",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Column {
                        Text(
                            text = "Eddie Voice Assistant (Voice AI)",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Natural low-latency Voice-to-Voice communication. Press the microphone button and start speaking.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Main Waveform and Voice Indicator Visualizer
            Box(
                modifier = Modifier
                    .weight(1.2f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                // Background pulsed wave circle
                Box(
                    modifier = Modifier
                        .size(160.dp)
                        .scale(pulseScale)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.primary.copy(alpha = waveAlpha),
                                    Color.Transparent
                                )
                            )
                        )
                )

                // Mid circle wave
                Box(
                    modifier = Modifier
                        .size(110.dp)
                        .scale(pulseScale * 0.95f)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.secondary.copy(alpha = 0.25f),
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                )
                            )
                        )
                )

                // Foreground active core button
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.verticalGradient(
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
                        text = if (uiState.voiceIsSpeakActive) "🔊" else if (uiState.voiceIsLoading) "⏳" else "🎙️",
                        style = TextStyle(fontSize = 32.sp)
                    )
                }
            }

            // Real-Time Transcript Display
            Column(
                modifier = Modifier
                    .weight(2f)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f))
                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f), RoundedCornerShape(16.dp))
                    .padding(8.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Voice Dialogue Transcript",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        if (uiState.voiceIsSpeakActive) {
                            TextButton(onClick = onStopTts) {
                                Text("Stop Voice", fontSize = 12.sp, color = MaterialTheme.colorScheme.error)
                            }
                        }
                        if (uiState.voiceChatLog.isNotEmpty()) {
                            IconButton(
                                onClick = onClearChat,
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Clean Chat",
                                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }

                Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))

                if (uiState.voiceChatLog.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No voice dialogue yet. Please press the mic button above and start speaking.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(vertical = 8.dp)
                    ) {
                        items(uiState.voiceChatLog) { (message, isUser) ->
                            val alignment = if (isUser) Alignment.End else Alignment.Start
                            val containerColor = if (isUser) {
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
                                Box(
                                    modifier = Modifier
                                        .clip(
                                            RoundedCornerShape(
                                                topStart = 16.dp,
                                                topEnd = 16.dp,
                                                bottomStart = if (isUser) 16.dp else 4.dp,
                                                bottomEnd = if (isUser) 4.dp else 16.dp
                                            )
                                        )
                                        .background(containerColor)
                                        .padding(horizontal = 12.dp, vertical = 8.dp)
                                        .widthIn(max = 280.dp)
                                ) {
                                    Text(
                                        text = message,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = textColor
                                    )
                                }
                            }
                        }
                        
                        if (uiState.voiceIsLoading) {
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
                                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                                            .padding(horizontal = 12.dp, vertical = 6.dp)
                                    ) {
                                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                        Text("Eddie is analyzing & speaking...", style = MaterialTheme.typography.bodySmall)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Action Trigger Button
            Button(
                onClick = onMicClick,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                ),
                shape = RoundedCornerShape(30.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("🎙️", fontSize = 20.sp)
                    Text("Connect & Start Speaking", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
