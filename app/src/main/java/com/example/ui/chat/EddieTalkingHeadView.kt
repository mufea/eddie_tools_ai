package com.example.ui.chat

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import android.widget.Toast

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EddieTalkingHeadView(
    uiState: ChatUiState,
    paddingValues: PaddingValues,
    onSelectImage: (String) -> Unit,
    onAnimate: (String) -> Unit,
    onStop: () -> Unit
) {
    var scriptInput by remember { mutableStateOf("") }
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    // Default high-fidelity virtual avatars if user's generated image list is empty or for quick selection
    val presetAvatars = listOf(
        "Emma (Companion)" to "https://images.unsplash.com/photo-1494790108377-be9c29b29330?auto=format&fit=crop&q=80&w=400",
        "Kazu (Anime)" to "https://images.unsplash.com/photo-1534528741775-53994a69daeb?auto=format&fit=crop&q=80&w=400",
        "Dr. Adrian" to "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?auto=format&fit=crop&q=80&w=400",
        "Robo (Cyber)" to "https://images.unsplash.com/photo-1531746020798-e6953c6e8e04?auto=format&fit=crop&q=80&w=400"
    )

    // Current selected avatar URL (defaults to the first preset or user selected)
    val activeAvatarUrl = uiState.talkingHeadImageUrl.ifBlank {
        if (uiState.generatedImages.isNotEmpty()) {
            uiState.generatedImages.first().imageUrl ?: presetAvatars[0].second
        } else {
            presetAvatars[0].second
        }
    }

    LaunchedEffect(Unit) {
        if (uiState.talkingHeadImageUrl.isBlank()) {
            onSelectImage(activeAvatarUrl)
        }
    }

    // Dynamic animators for talking mouth & facial ripples!
    val infiniteTransition = rememberInfiniteTransition(label = "LipSync")
    val mouthScale by infiniteTransition.animateFloat(
        initialValue = 0.9f,
        targetValue = 1.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(140, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "MouthScale"
    )

    val eyesOffset by infiniteTransition.animateFloat(
        initialValue = -1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "EyesLook"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .background(Color.Transparent)
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Face,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            text = "AI Avatar / Talking Head Animator",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "Transform static portrait screens into interactive speaking video simulations perfectly synced to smart neural speech synthesis (TTS).",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // 1. SELECT AVATAR SECTION
            Text(
                text = "AVATAR SELECTOR",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Horizontal picker of presets or generated images
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // If they have generated images in Image Studio, display them first!
                if (uiState.generatedImages.isNotEmpty()) {
                    uiState.generatedImages.forEachIndexed { index, genImage ->
                        genImage.imageUrl?.let { url ->
                            Box(
                                modifier = Modifier
                                    .size(72.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .border(
                                        width = 3.dp,
                                        color = if (activeAvatarUrl == url) MaterialTheme.colorScheme.primary else Color.Transparent,
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .clickable { onSelectImage(url) }
                            ) {
                                AsyncImage(
                                    model = url,
                                    contentDescription = "Studio Image $index",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.BottomEnd)
                                        .background(MaterialTheme.colorScheme.secondary, RoundedCornerShape(topStart = 4.dp))
                                        .padding(horizontal = 4.dp, vertical = 1.dp)
                                ) {
                                    Text("Studio", style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp, color = Color.White))
                                }
                            }
                        }
                    }
                }

                // Preset Avatars
                presetAvatars.forEach { preset ->
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .border(
                                width = 3.dp,
                                color = if (activeAvatarUrl == preset.second) MaterialTheme.colorScheme.primary else Color.Transparent,
                                shape = RoundedCornerShape(8.dp)
                            )
                            .clickable { onSelectImage(preset.second) }
                    ) {
                        AsyncImage(
                            model = preset.second,
                            contentDescription = preset.first,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .fillMaxWidth()
                                .background(Color.Black.copy(alpha = 0.6f))
                                .padding(vertical = 1.dp)
                        ) {
                            Text(
                                text = preset.first,
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp, color = Color.White),
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }

            // 2. ANIMATOR PREVIEW & SPEECH RIPPLES PANEL
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    // Base face image with subtle float translation
                    AsyncImage(
                        model = activeAvatarUrl,
                        contentDescription = "Active Avatar Portrait",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .offset(y = if (uiState.talkingHeadIsPlaying) eyesOffset.dp else 0.dp)
                    )

                    // Overlay Speech ripple / Voice indicators & Lips Animation
                    if (uiState.talkingHeadIsPlaying) {
                        // Dynamic mouth open overlay representation at standard average lips coordinates (62% down core layout)
                        Box(
                            modifier = Modifier
                                .align(Alignment.Center)
                                .offset(y = 48.dp)
                                .size(
                                    width = (24.dp * mouthScale),
                                    height = (12.dp * mouthScale)
                                )
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.85f))
                        )

                        // Outer breathing aura
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .border(
                                    width = 4.dp,
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                                    shape = RoundedCornerShape(4.dp)
                                )
                        )

                        // Real-time speaking status badge
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(12.dp)
                                .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(12.dp))
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Notifications, contentDescription = null, tint = Color.White, modifier = Modifier.size(12.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Speaking...", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = Color.White))
                            }
                        }
                    } else if (uiState.talkingHeadIsLoading) {
                        // Processing/Synthesis Blur/Overlay
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.6f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                                Text(
                                    "Synchronizing lip sync & vocal data streams...",
                                    style = MaterialTheme.typography.bodySmall.copy(color = Color.White, fontWeight = FontWeight.Bold),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    } else {
                        // Static passive status badge
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(12.dp)
                                .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text("Ready (Idle)", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = Color.White))
                        }
                    }
                }
            }

            // 3. VOICE SCRIPT INPUT PANEL
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "AVATAR SPEECH TEXT / SCRIPT",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedTextField(
                        value = scriptInput,
                        onValueChange = { scriptInput = it },
                        placeholder = { Text("Type what the avatar should speak...") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 4,
                        shape = RoundedCornerShape(8.dp)
                    )

                    // Quick Script Templates
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val scriptTemplates = listOf(
                            "Hello friend! Good morning, let's continue building the future of Android AI together.",
                            "Thank you for your fantastic dedication today. Is there any new research I can gather for you?",
                            "App successfully compiled with zero errors and clean unit tests!"
                        )
                        scriptTemplates.forEachIndexed { i, template ->
                            InputChip(
                                selected = false,
                                onClick = { scriptInput = template },
                                label = { Text("Template ${i + 1}", maxLines = 1) }
                            )
                        }
                    }

                    if (uiState.talkingHeadIsPlaying) {
                        Button(
                            onClick = onStop,
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Clear, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Stop Speaking Simulation")
                        }
                    } else {
                        Button(
                            onClick = {
                                if (scriptInput.isNotBlank()) {
                                    onAnimate(scriptInput)
                                } else {
                                    Toast.makeText(context, "Please enter a speech script first.", Toast.LENGTH_SHORT).show()
                                }
                            },
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Launch Face Animation & TTS")
                        }
                    }
                }
            }
        }
    }
}
