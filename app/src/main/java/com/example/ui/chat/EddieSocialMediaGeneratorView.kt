package com.example.ui.chat

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.widget.Toast
import coil.compose.AsyncImage

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun EddieSocialMediaGeneratorView(
    uiState: ChatUiState,
    paddingValues: PaddingValues,
    onGenerate: (String, String) -> Unit,
    onClear: () -> Unit
) {
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current

    var ideaInput by remember { mutableStateOf("") }
    var selectedTone by remember { mutableStateOf("Professional") }
    val toneOptions = listOf("Professional", "Witty", "Urgent")
    val toneIcons = listOf("💼", "🎭", "⚡")

    // Active sub-tab to preview platform drafts
    var selectedPlatformTab by remember { mutableStateOf("LinkedIn") }

    fun shareContent(platformName: String, text: String) {
        if (text.isBlank()) return
        try {
            val sendIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(android.content.Intent.EXTRA_SUBJECT, "$platformName Draft")
                putExtra(android.content.Intent.EXTRA_TEXT, text)
            }
            val shareIntent = android.content.Intent.createChooser(sendIntent, "Share Draft for $platformName")
            context.startActivity(shareIntent)
        } catch (e: Exception) {
            Toast.makeText(context, "Could not launch sharing.", Toast.LENGTH_SHORT).show()
        }
    }

    fun copyToClipboard(text: String, label: String) {
        if (text.isBlank()) return
        clipboardManager.setText(AnnotatedString(text))
        Toast.makeText(context, "$label draft copied to clipboard!", Toast.LENGTH_SHORT).show()
    }

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
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Feature Header Banner (Non-Sticky Header)
            Card(
                modifier = Modifier
                    .fillMaxWidth(),
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
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("🌐", style = TextStyle(fontSize = 24.sp))
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            text = "Multi-Platform social Engine",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "Simultaneously draft high-engagement, customized content for LinkedIn, Twitter, and Instagram with unique native ratio images.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            
            // Output Panel (displays when content exists or is loading)
            if (uiState.socialIsLoading || uiState.socialLinkedInPost.isNotBlank()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("🪄 ", style = TextStyle(fontSize = 18.sp))
                                Text(
                                    text = "SIMULTANEOUS DRAFTS",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 1.sp
                                    ),
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            if (uiState.socialIsLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            } else {
                                IconButton(
                                    onClick = onClear,
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Clear,
                                        contentDescription = "Clear drafts",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        if (uiState.socialIsLoading) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 32.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                LinearProgressIndicator(
                                    modifier = Modifier
                                        .fillMaxWidth(0.7f)
                                        .clip(RoundedCornerShape(4.dp))
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "Analyzing concept context...",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "Generating custom ratio artwork & copywriting frameworks...",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        } else {
                            // Render platform selection sub-tabs
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                listOf("LinkedIn", "X / Twitter", "Instagram").forEach { platform ->
                                    val isCur = when (platform) {
                                        "LinkedIn" -> selectedPlatformTab == "LinkedIn"
                                        "X / Twitter" -> selectedPlatformTab == "Twitter"
                                        else -> selectedPlatformTab == "Instagram"
                                    }
                                    val platKey = if (platform == "X / Twitter") "Twitter" else platform
                                    
                                    Card(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clickable { selectedPlatformTab = platKey },
                                        shape = RoundedCornerShape(8.dp),
                                        colors = CardDefaults.cardColors(
                                            containerColor = if (isCur) MaterialTheme.colorScheme.secondaryContainer 
                                                             else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                        ),
                                        border = if (isCur) BorderStroke(1.2.dp, MaterialTheme.colorScheme.primary) else null
                                    ) {
                                        Text(
                                            text = platform,
                                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                            textAlign = TextAlign.Center,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 8.dp)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // Display selected platform draft details
                            when (selectedPlatformTab) {
                                "LinkedIn" -> {
                                    PlatformDraftPreview(
                                        platformName = "LinkedIn 💼",
                                        aspectRatioText = "Optimal Ratio: Horizontal Desktop (1.91:1) • Suggested 1200x630 pixel canvas",
                                        imagePromptText = uiState.socialLinkedInImagePrompt,
                                        imageUrl = uiState.socialLinkedInImageUrl,
                                        draftText = uiState.socialLinkedInPost,
                                        onCopy = { copyToClipboard(uiState.socialLinkedInPost, "LinkedIn") },
                                        onShare = { shareContent("LinkedIn", uiState.socialLinkedInPost) }
                                    )
                                }
                                "Twitter" -> {
                                    PlatformDraftPreview(
                                        platformName = "X / Twitter 🐦",
                                        aspectRatioText = "Optimal Ratio: Widescreen Feed (16:9) • Suggested 1200x675 pixel card",
                                        imagePromptText = uiState.socialTwitterImagePrompt,
                                        imageUrl = uiState.socialTwitterImageUrl,
                                        draftText = uiState.socialTwitterPost,
                                        onCopy = { copyToClipboard(uiState.socialTwitterPost, "X/Twitter") },
                                        onShare = { shareContent("X/Twitter", uiState.socialTwitterPost) }
                                    )
                                }
                                "Instagram" -> {
                                    PlatformDraftPreview(
                                        platformName = "Instagram 📸",
                                        aspectRatioText = "Optimal Ratio: Tall Portrait Grid (4:5) • Suggested 1080x1350 pixel canvas",
                                        imagePromptText = uiState.socialInstagramImagePrompt,
                                        imageUrl = uiState.socialInstagramImageUrl,
                                        draftText = uiState.socialInstagramPost,
                                        onCopy = { copyToClipboard(uiState.socialInstagramPost, "Instagram") },
                                        onShare = { shareContent("Instagram", uiState.socialInstagramPost) }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Input Configuration Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "1. DEFINE YOUR CAMPAIGN IDEA OR CONCEPT",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    OutlinedTextField(
                        value = ideaInput,
                        onValueChange = { ideaInput = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("e.g. launching a premium organic tea brand with eco-friendly biodegradable mesh bags...") },
                        maxLines = 5,
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "2. DESIRED COPYWRITING TONE",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        toneOptions.forEachIndexed { i, tone ->
                            val isSel = tone == selectedTone
                            Card(
                                modifier = Modifier.clickable { selectedTone = tone },
                                shape = RoundedCornerShape(20.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isSel) MaterialTheme.colorScheme.primaryContainer 
                                                     else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                ),
                                border = if (isSel) BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else null
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(toneIcons[i] + "  ", style = TextStyle(fontSize = 14.sp))
                                    Text(
                                        text = tone,
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isSel) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                        )
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Button(
                        onClick = {
                            if (ideaInput.trim().isNotBlank()) {
                                onGenerate(ideaInput, selectedTone)
                            } else {
                                Toast.makeText(context, "Please enter your idea first.", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        enabled = !uiState.socialIsLoading
                    ) {
                        Text(
                            text = "✨ Simultaneously Generate Posts & Artworks",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PlatformDraftPreview(
    platformName: String,
    aspectRatioText: String,
    imagePromptText: String,
    imageUrl: String,
    draftText: String,
    onCopy: () -> Unit,
    onShare: () -> Unit
) {
    Column {
        // Platform Label
        Text(
            text = platformName,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(6.dp))

        // Ratio Badge Guide
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
                .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = "ratio info",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = aspectRatioText,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Multi-Aspect Ratio Tailored Image Card
        if (imageUrl.isNotBlank()) {
            val aspectModifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
            
            val finalModifier = when {
                platformName.contains("LinkedIn") -> aspectModifier.aspectRatio(1.91f)
                platformName.contains("Twitter") || platformName.contains("X ") -> aspectModifier.aspectRatio(1.777f) // 16:9 is 1.777f
                else -> aspectModifier.aspectRatio(0.8f) // 4:5 is 0.8f for Instagram Portrait
            }

            Box(modifier = finalModifier) {
                AsyncImage(
                    model = imageUrl,
                    contentDescription = "Artwork generated for $platformName",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                // Creative prompt overlay
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.75f))))
                        .padding(10.dp)
                ) {
                    Text(
                        text = "AI Visual Concept: $imagePromptText",
                        color = Color.White,
                        style = MaterialTheme.typography.labelSmall,
                        maxLines = 2
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Text Post Body Editor/Viewer Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
            shape = RoundedCornerShape(8.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = draftText,
                    style = MaterialTheme.typography.bodyMedium,
                    lineHeight = 20.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Copy / Share action segment
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedButton(
                onClick = onCopy,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(vertical = 12.dp)
            ) {
                Text("📋 Copy Draft", fontWeight = FontWeight.Bold)
            }
            Button(
                onClick = onShare,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(vertical = 12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Share,
                    contentDescription = "Share text",
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Share Post", fontWeight = FontWeight.Bold)
            }
        }
    }
}
