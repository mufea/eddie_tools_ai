package com.example.ui.chat

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.widget.Toast

data class ParsedSlide(
    val title: String,
    val layout: String,
    val bullets: List<String>,
    val note: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EddieSlideOutlineView(
    uiState: ChatUiState,
    paddingValues: PaddingValues,
    onGenerate: (String) -> Unit,
    onClear: () -> Unit
) {
    var topicInput by remember { mutableStateOf("") }
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current
    var currentSlideIndex by remember { mutableStateOf(0) }

    // Dynamic parser to turn Gemini's structured output into visual slides!
    val parsedSlides = remember(uiState.slidesOutput) {
        val slides = mutableListOf<ParsedSlide>()
        if (uiState.slidesOutput.isBlank()) return@remember slides

        val blocks = uiState.slidesOutput.split(Regex("(?=Slide\\s?\\d+:)"))
        for (block in blocks) {
            if (block.trim().isBlank()) continue
            
            val lines = block.lines().map { it.trim() }.filter { it.isNotEmpty() }
            if (lines.isEmpty()) continue

            // 1. Extract Slide Title
            val titleLine = lines.firstOrNull { it.startsWith("Slide") } ?: "Slide Draft"
            val cleanTitle = titleLine.substringAfter(":").trim()

            // 2. Extract Visual Layout description
            val layoutLine = lines.firstOrNull { it.startsWith("- Visual Layout:") || it.startsWith("Visual Layout:") }
            val cleanLayout = layoutLine?.substringAfter(":")?.trim() ?: "Modern Minimalist Card Grid"

            // 3. Extract bullets
            val bullets = lines.filter { it.startsWith("*") || it.startsWith("-") && !it.contains("Visual Layout:") && !it.contains("Slide Note:") }
                .map { it.trim().removePrefix("*").removePrefix("-").trim() }

            // 4. Extract Slide notes
            val noteLine = lines.firstOrNull { it.startsWith("- Slide Note:") || it.startsWith("Slide Note:") }
            val cleanNote = noteLine?.substringAfter(":")?.trim() ?: "Expand on the key bullet points clearly."

            slides.add(ParsedSlide(cleanTitle, cleanLayout, bullets, cleanNote))
        }
        slides
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
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            text = "AI Slide & Presenter Outline",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "Design visual layouts, bullet points, and presenter notes for a beautifully structured presentation.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            if (uiState.slidesOutput.isBlank() && !uiState.slidesIsLoading) {
                // Form input
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            text = "CREATE NEW PRESENTATION OUTLINE",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        OutlinedTextField(
                            value = topicInput,
                            onValueChange = { topicInput = it },
                            label = { Text("Presentation Topic / Theme") },
                            placeholder = { Text("e.g., Digital Marketing Strategy for SMBs or Intro to Machine Learning") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            maxLines = 2,
                            colors = OutlinedTextFieldDefaults.colors(
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline
                            )
                        )

                        Button(
                            onClick = {
                                if (topicInput.isNotBlank()) {
                                    onGenerate(topicInput)
                                    currentSlideIndex = 0
                                } else {
                                    Toast.makeText(context, "Please enter a presentation topic.", Toast.LENGTH_SHORT).show()
                                }
                            },
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Star, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Design Presentation Outline (Slides)")
                        }
                    }
                }
            }

            // Loading Panel
            if (uiState.slidesIsLoading) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp).fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                        Text(
                            text = "Organizing visual layouts, arranging bullet points, and synthesizing your slide deck...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            // Render Visual Cards & Carousel if Output parsed
            if (parsedSlides.isNotEmpty()) {
                val slide = parsedSlides.getOrNull(currentSlideIndex)
                if (slide != null) {
                    // Slide Container simulating physical Presentation slide ratio (16:9 look)
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1.6f),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
                    ) {
                        // Styled background simulation mimicking slide templates
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.linearGradient(
                                        colors = listOf(
                                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f),
                                            MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.05f)
                                        )
                                    )
                                )
                                .padding(16.dp)
                        ) {
                            Column(modifier = Modifier.fillMaxSize()) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.Info,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = slide.layout,
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 10.sp),
                                            color = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.weight(1f, fill = false),
                                            maxLines = 1
                                        )
                                    }
                                    Text(
                                        text = "SLIDE ${currentSlideIndex + 1}/${parsedSlides.size}",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Spacer(modifier = Modifier.height(12.dp))

                                Text(
                                    text = slide.title,
                                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(10.dp))

                                Column(
                                    modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    slide.bullets.forEach { bullet ->
                                        Row(verticalAlignment = Alignment.Top) {
                                            Text("• ", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary))
                                            Text(
                                                text = bullet,
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Carousel controls
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { if (currentSlideIndex > 0) currentSlideIndex-- },
                            enabled = currentSlideIndex > 0
                        ) {
                            Icon(Icons.AutoMirrored.Default.ArrowBack, contentDescription = "Previous Slide")
                        }

                        Text(
                            text = "Slide ${currentSlideIndex + 1} of ${parsedSlides.size}",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                        )

                        IconButton(
                            onClick = { if (currentSlideIndex < parsedSlides.size - 1) currentSlideIndex++ },
                            enabled = currentSlideIndex < parsedSlides.size - 1
                        ) {
                            Icon(Icons.AutoMirrored.Default.ArrowForward, contentDescription = "Next Slide")
                        }
                    }

                    // Presenter guide Note (bottom details)
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "PRESENTER SPEECH / NOTES",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = slide.note,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    // Export / copy / reset row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                clipboardManager.setText(AnnotatedString(uiState.slidesOutput))
                                Toast.makeText(context, "Entire presentation outline copied to clipboard!", Toast.LENGTH_SHORT).show()
                            },
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Copy Outline")
                        }

                        OutlinedButton(
                            onClick = onClear,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Clear, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Start Fresh")
                        }
                    }
                }
            }
        }
    }
}
