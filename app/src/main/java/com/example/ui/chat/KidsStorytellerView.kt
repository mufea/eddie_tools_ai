package com.example.ui.chat

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun KidsStorytellerView(
    uiState: ChatUiState,
    paddingValues: PaddingValues,
    onThemeChange: (String) -> Unit,
    onChildNameChange: (String) -> Unit,
    onStyleChange: (String) -> Unit,
    onPageIndexChange: (Int) -> Unit,
    onGenerateClick: (String, String, String) -> Unit,
    onPlayTtsClick: (String) -> Unit,
    onStopTtsClick: () -> Unit,
    onSelectHistoryStory: (KidStory) -> Unit,
    onResetClick: () -> Unit
) {
    val context = LocalContext.current
    var inputTheme by remember { mutableStateOf(uiState.storySelectedTheme) }
    var inputName by remember { mutableStateOf(uiState.storyChildName) }
    var selectedStyle by remember { mutableStateOf(uiState.storySelectedStyle) }

    val presetThemes = listOf(
        Pair("🚀 Space Galaxy Expedition", "Space Galaxy Expedition"),
        Pair("🦄 Rainbow Pegasus Palace", "Rainbow Pegasus Palace"),
        Pair("🦕 Friendly Baby Dinosaurs", "Friendly Baby Dinosaurs"),
        Pair("🧸 Living Toy Kingdom", "Living Toy Kingdom"),
        Pair("🦁 Whimsical Safari Picnic", "Whimsical Safari Picnic"),
        Pair("🧙 Magic Wizard School", "Magic Wizard School"),
        Pair("🍬 Floating Candy Island", "Floating Candy Island"),
        Pair("🐬 Sunken Pirate Treasure", "Sunken Pirate Treasure")
    )

    val styles = listOf(
        Pair("🎨 Watercolor", "Watercolor Illustration"),
        Pair("🎮 Pixel Art", "Cute Pixel Art"),
        Pair("🧸 Claymation", "Playful 3D Claymation"),
        Pair("✏️ story Sketch", "Whimsical Storybook Sketch")
    )

    // Animated background infinite color pulse for children's enchantment
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alphaPulse"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.background,
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f * pulseAlpha)
                    )
                )
            )
            .verticalScroll(rememberScrollState())
    ) {
        // --- HEADER HERO ---
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary,
                            MaterialTheme.colorScheme.tertiary
                        )
                    )
                )
                .padding(24.dp)
        ) {
            Column {
                Text(
                    text = "📖 Kids AI Storyteller",
                    color = Color.White,
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold)
                )
                Text(
                    text = "Imagine any magical theme, custom-style illustrations, and hear stories read aloud!",
                    color = Color.White.copy(alpha = 0.9f),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        if (uiState.storyActiveBook == null) {
            // ==================== CONFIGURATION SCREEN ====================
            Column(modifier = Modifier.padding(16.dp)) {
                
                // Greeting message
                Card(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("🤖", fontSize = 36.sp, modifier = Modifier.padding(end = 12.dp))
                        Column {
                            Text(
                                "Create a Personalized Adventure!",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                "Enter the child's name, choose reference topics, and watch Gemini draw custom story pages!",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Field 1: Child's name
                Text(
                    text = "👶 Step 1: Who is the Hero?",
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 6.dp),
                    color = MaterialTheme.colorScheme.primary
                )
                OutlinedTextField(
                    value = inputName,
                    onValueChange = { inputName = it },
                    placeholder = { Text("e.g. Leo, Alice, Ryan") },
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp).testTag("child_name_input"),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = "Baby Name") }
                )

                // Field 2: Custom theme / plot
                Text(
                    text = "🌌 Step 2: What is the magical Theme?",
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 6.dp),
                    color = MaterialTheme.colorScheme.primary
                )
                OutlinedTextField(
                    value = inputTheme,
                    onValueChange = { inputTheme = it },
                    placeholder = { Text("Compose anything! e.g., A golden puppy exploring Saturn...") },
                    modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp).testTag("story_theme_input"),
                    shape = RoundedCornerShape(12.dp),
                    leadingIcon = { Icon(Icons.Default.Star, contentDescription = "Theme Icon") }
                )

                // Quick preset tags
                Text(
                    text = "Tap to choose a Quick Preset Topic:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 6.dp)
                )
                FlowRow(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    presetThemes.forEach { (emojiTitle, actionTheme) ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                .border(
                                    width = 1.dp,
                                    color = if (inputTheme == actionTheme) MaterialTheme.colorScheme.primary else Color.Transparent,
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .clickable {
                                    inputTheme = actionTheme
                                }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(emojiTitle, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }

                // Field 3: Style option choice
                Text(
                    text = "🎨 Step 3: Choose the Illustration Style:",
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp),
                    color = MaterialTheme.colorScheme.primary
                )
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(bottom = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    styles.forEach { (label, sysStyle) ->
                        val isSelected = selectedStyle == sysStyle
                        ElevatedFilterChip(
                            selected = isSelected,
                            onClick = { selectedStyle = sysStyle },
                            label = { Text(label, fontSize = 12.sp, fontWeight = FontWeight.Bold) },
                            colors = FilterChipDefaults.elevatedFilterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                            )
                        )
                    }
                }

                if (uiState.storyIsLoading != null) {
                    // Loading bar + custom step text
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f))
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(28.dp))
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = uiState.storyIsLoading,
                                textAlign = TextAlign.Center,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                } else {
                    // Start generating button
                    Button(
                        onClick = {
                            onGenerateClick(inputTheme, inputName, selectedStyle)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .testTag("generate_story_button"),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Text("✨ Let's Go! Weave My Story ✨", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                }

                // Saved Storybook shelf library
                if (uiState.storyBookList.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        text = "📚 Library: Saved Adventures",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(bottom = 10.dp)
                    )
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        uiState.storyBookList.forEach { story ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onSelectHistoryStory(story) },
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("📖", fontSize = 24.sp, modifier = Modifier.padding(end = 10.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(story.title, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        Text(
                                            text = "Hero: ${story.childName} • Style: ${story.illustrationStyle}",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Icon(
                                        imageVector = Icons.Default.ArrowForward,
                                        contentDescription = "Open Book"
                                    )
                                }
                            }
                        }
                    }
                }
            }
        } else {
            // ==================== STORYBOOK PLAYER ====================
            val book = uiState.storyActiveBook
            val currentPageIndex = uiState.storyCurrentPageIndex
            val pages = book.pages

            if (pages.isNotEmpty() && currentPageIndex < pages.size) {
                val page = pages[currentPageIndex]

                Column(modifier = Modifier.padding(16.dp)) {
                    // Header controls
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = onResetClick) {
                            Icon(Icons.Default.Home, contentDescription = "Back to Library")
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = book.title,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "Page ${page.pageNumber} of ${pages.size}",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Box(modifier = Modifier.size(48.dp)) // spacer balance
                    }

                    // Page Illustration Card
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1.2f)
                            .padding(bottom = 16.dp),
                        shape = RoundedCornerShape(20.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Box(modifier = Modifier.fillMaxSize()) {
                            if (page.imagePath != null) {
                                AsyncImage(
                                    model = page.imagePath,
                                    contentDescription = "Page ${page.pageNumber} illustration",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                // Shimmer/Progress illustration painter state
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                        .padding(16.dp),
                                    verticalArrangement = Arrangement.Center,
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    val infiniteStarTransition = rememberInfiniteTransition(label = "star_turning")
                                    val rotationDegrees by infiniteStarTransition.animateFloat(
                                        initialValue = 0f,
                                        targetValue = 360f,
                                        animationSpec = infiniteRepeatable(
                                            animation = tween(4000, easing = LinearEasing),
                                            repeatMode = RepeatMode.Restart
                                        ),
                                        label = "rotateDegrees"
                                    )
                                    Text(
                                        text = "🎨✨🎨",
                                        fontSize = 32.sp,
                                        modifier = Modifier.animateContentSize()
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(
                                        text = "Painting Dynamic Page Illustration...",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.primary,
                                        textAlign = TextAlign.Center
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Drawing: ${page.title}",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.padding(horizontal = 8.dp)
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth(0.6f))
                                }
                            }
                        }
                    }

                    // Read Aloud Controls
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f))
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = if (uiState.storyIsPlayingAudio) "🔊" else "🔇",
                                fontSize = 24.sp,
                                modifier = Modifier.padding(end = 10.dp)
                            )
                            Column {
                                Text(
                                    text = if (uiState.storyIsPlayingAudio) "Playing Story Voice" else "Voice Speaker Offline",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Speak aloud page text",
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        if (uiState.storyIsPlayingAudio) {
                            Button(
                                onClick = onStopTtsClick,
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                            ) {
                                Text("⏹️ Stop", fontSize = 12.sp)
                            }
                        } else {
                            Button(
                                onClick = { onPlayTtsClick(page.text) },
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                            ) {
                                Text("🔊 Read Page", fontSize = 12.sp)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Page story Content Storybook Text
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFDF5)), // beautiful ivory/cream paper color
                        border = BorderStroke(1.dp, Color(0xFFEFE8D0))
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Text(
                                text = page.title,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = Color(0xFF4E3629), // deep chocolate cozy brown text
                                modifier = Modifier.padding(bottom = 10.dp)
                            )
                            Text(
                                text = page.text,
                                fontSize = 15.sp,
                                lineHeight = 24.sp,
                                color = Color(0xFF5D4037)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Next / Back Pagination navigation bar controls
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        FilledTonalButton(
                            onClick = { onPageIndexChange(currentPageIndex - 1) },
                            enabled = currentPageIndex > 0,
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back Page")
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Prev")
                        }

                        // Little pagination circles
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            pages.forEachIndexed { idx, _ ->
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (idx == currentPageIndex) MaterialTheme.colorScheme.primary
                                            else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
                                        )
                                )
                            }
                        }

                        Button(
                            onClick = {
                                if (currentPageIndex < pages.size - 1) {
                                    onPageIndexChange(currentPageIndex + 1)
                                }
                            },
                            enabled = currentPageIndex < pages.size - 1,
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Text("Next")
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(Icons.Default.ArrowForward, contentDescription = "Next Page")
                        }
                    }
                }
            }
        }
    }
}
