package com.example.ui.chat

import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import java.io.File

// Scribble line structure representing a single brush swipe
data class ScribbleLine(
    val points: List<Offset>,
    val color: Color,
    val strokeWidth: Float
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ChildrenColoringBookView(
    uiState: ChatUiState,
    paddingValues: PaddingValues,
    onThemeChange: (String) -> Unit,
    onChildNameChange: (String) -> Unit,
    onGenerateClick: (String, String) -> Unit,
    onGeneratePdfClick: () -> Unit
) {
    val context = LocalContext.current
    var inputTheme by remember { mutableStateOf(uiState.colorBookTheme) }
    var inputName by remember { mutableStateOf(uiState.colorBookChildName) }
    
    // Track selected active coloring page on screen (0 to 4)
    var activePageIndex by remember { mutableStateOf(0) }
    
    // Store scribbles separately for each of the 5 pages
    val pageScribbles = remember { mutableStateMapOf<Int, List<ScribbleLine>>() }
    
    // Brush controls
    var selectedColor by remember { mutableStateOf(Color(0xFFFF5252)) } // default: bright red crayon
    var brushWidth by remember { mutableStateOf(8f) }
    
    val presetSuggestions = listOf(
        Pair("🦖 Space Dinosaurs", "space dinosaurs"),
        Pair("🏰 Princess Castle", "fairytale princess castle"),
        Pair("🐬 Underwater sea", "underwater ocean dolphin adventure"),
        Pair("⚡ Friendly Robots", "friendly workspace robots"),
        Pair("🦁 Jungle Safari", "happy jungle safari cubs"),
        Pair("🚜 Busy Farm", "cozy farm cow chicken piggy sheep"),
        Pair("🏎️ Speed Racing", "speedy racing cars and mega monster trucks"),
        Pair("🦸 Superheroes", "cute mini superheroes with flying capes"),
        Pair("🐶 Playful Pets", "playful puppies and fluffy kittens playing"),
        Pair("🍭 Candyland", "sweet chocolate river candyland lollipops"),
        Pair("🐉 Magical Dragons", "magical flying friendly dragons"),
        Pair("🏴‍☠️ Pirate Adventure", "sailor pirates treasure hunt adventure"),
        Pair("🚀 Space Explorers", "cosmic space stars adventure and rocket ship"),
        Pair("🧸 Toy Kingdom", "toy soldiers teddy bear wonderland"),
        Pair("🍦 Ice Cream Land", "ice cream mountain sweet sprinkles candy sundaes"),
        Pair("🎪 Circus Magic", "circus fun juggling bears happy balloons"),
        Pair("🦉 Enchanted Forest", "mystical talking trees glowing owl forest fairy"),
        Pair("🦖 Dino Dino Cave", "retro cute baby dinosaurs running volcanoes"),
        Pair("🛸 Alien Saucer Space", "friendly glowing green aliens flying saucer UFO"),
        Pair("❄️ Winter Wonderland", "snowy landscapes sledding penguin snowman holiday")
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
    ) {
        // --- HEADER ---
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
                    text = "🎨 Magic Coloring Generator",
                    color = Color.White,
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold)
                )
                Text(
                    text = "Create customized print-ready books for your kids with rhyming story poems!",
                    color = Color.White.copy(alpha = 0.85f),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        Column(modifier = Modifier.padding(16.dp)) {
            // --- TOP CONFIGURATION CARD ---
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Customize Your Book Theme & Child Participant",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    
                    // Child Name input
                    OutlinedTextField(
                        value = inputName,
                        onValueChange = {
                            inputName = it
                            onChildNameChange(it)
                        },
                        label = { Text("✏️ Child's Name") },
                        placeholder = { Text("e.g. Alex, Liam, Sophia") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp)
                            .testTag("coloring_child_name_input"),
                        shape = RoundedCornerShape(12.dp)
                    )

                    // Theme input
                    OutlinedTextField(
                        value = inputTheme,
                        onValueChange = {
                            inputTheme = it
                            onThemeChange(it)
                        },
                        label = { Text("🚀 Coloring Book Theme") },
                        placeholder = { Text("e.g. astronaut cats, pizza volcanoes") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp)
                            .testTag("coloring_theme_input"),
                        shape = RoundedCornerShape(12.dp)
                    )

                    // Presets
                    Text(
                        text = "Spark a theme idea:",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                    
                    FlowRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        presetSuggestions.forEach { (label, value) ->
                            SuggestionChip(
                                onClick = {
                                    inputTheme = value
                                    onThemeChange(value)
                                },
                                label = { Text(label) },
                                shape = RoundedCornerShape(16.dp)
                            )
                        }
                    }

                    // Generate Button
                    Button(
                        onClick = {
                            if (inputTheme.isBlank()) {
                                Toast.makeText(context, "Please write a theme first!", Toast.LENGTH_SHORT).show()
                            } else {
                                onGenerateClick(inputTheme, inputName)
                                activePageIndex = 0
                            }
                        },
                        enabled = !uiState.colorBookIsLoading,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("submit_coloring_book_button"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        if (uiState.colorBookIsLoading) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("Formulating Pages...", fontWeight = FontWeight.SemiBold)
                        } else {
                            Icon(Icons.Default.Star, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Generate 5 Magic Pages!", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // --- OFFLINE MODE INDICATOR ---
            if (uiState.geminiApiKey.isBlank() || uiState.geminiApiKey.contains("PLACEHOLDER")) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("💡", fontSize = 18.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "API Key not configured. Using high-precision local vector illustrations. Configure GEMINI_API_KEY for custom AI Imagen lines!",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // --- ERROR DISPLAY ---
            if (uiState.colorBookError != null) {
                Text(
                    text = "⚠️ ${uiState.colorBookError}",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
            }

            // --- GENERATED BOOK VIEW ---
            if (uiState.colorBookPages.isNotEmpty()) {
                val pages = uiState.colorBookPages
                if (activePageIndex >= pages.size) activePageIndex = 0
                val currentPage = pages[activePageIndex]

                Column {
                    // Page Selector
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { if (activePageIndex > 0) activePageIndex-- },
                            enabled = activePageIndex > 0,
                            modifier = Modifier
                                .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                                .size(40.dp)
                        ) {
                            Icon(Icons.Default.KeyboardArrowLeft, contentDescription = "Previous Page")
                        }

                        Text(
                            text = "Page ${currentPage.pageNumber} of 5",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary
                        )

                        IconButton(
                            onClick = { if (activePageIndex < 4) activePageIndex++ },
                            enabled = activePageIndex < 4,
                            modifier = Modifier
                                .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                                .size(40.dp)
                        ) {
                            Icon(Icons.Default.KeyboardArrowRight, contentDescription = "Next Page")
                        }
                    }

                    // --- MAIN INTERACTIVE CANVAS ---
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1f),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(2.dp, MaterialTheme.colorScheme.primary),
                        colors = CardDefaults.cardColors(containerColor = Color.White)
                    ) {
                        Box(modifier = Modifier.fillMaxSize()) {
                            // 1. Core Line Art Layer
                            if (!currentPage.imagePath.isNullOrEmpty()) {
                                AsyncImage(
                                    model = currentPage.imagePath,
                                    contentDescription = currentPage.title,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Fit
                                )
                            } else {
                                // Draw high-fidelity local clean vector lines
                                DynamicVectorLineArt(
                                    presetType = currentPage.presetType,
                                    pageNumber = currentPage.pageNumber,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }

                            // 2. Kid Interactive Scribble Layer
                            val listLines = pageScribbles[currentPage.pageNumber] ?: emptyList()
                            var currentPoints = remember { mutableStateListOf<Offset>() }

                            Canvas(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .pointerInput(currentPage.pageNumber) {
                                        detectDragGestures(
                                            onDragStart = { offset ->
                                                currentPoints.clear()
                                                currentPoints.add(offset)
                                            },
                                            onDrag = { change, dragAmount ->
                                                change.consume()
                                                val newOffset = change.position
                                                currentPoints.add(newOffset)
                                                // Live drawing updating
                                                val activeLines = pageScribbles[currentPage.pageNumber] ?: emptyList()
                                                val updatedLines = activeLines.toMutableList()
                                                if (updatedLines.isNotEmpty() && updatedLines.last().color == selectedColor && updatedLines.last().strokeWidth == brushWidth) {
                                                    val lastLine = updatedLines.removeAt(updatedLines.size - 1)
                                                    updatedLines.add(lastLine.copy(points = lastLine.points + newOffset))
                                                } else {
                                                    updatedLines.add(ScribbleLine(currentPoints.toList(), selectedColor, brushWidth))
                                                }
                                                pageScribbles[currentPage.pageNumber] = updatedLines
                                            },
                                            onDragEnd = {
                                                if (currentPoints.isNotEmpty()) {
                                                    val activeLines = pageScribbles[currentPage.pageNumber] ?: emptyList()
                                                    pageScribbles[currentPage.pageNumber] = activeLines + ScribbleLine(
                                                        points = currentPoints.toList(),
                                                        color = selectedColor,
                                                        strokeWidth = brushWidth
                                                    )
                                                }
                                                currentPoints.clear()
                                            }
                                        )
                                    }
                            ) {
                                listLines.forEach { line ->
                                    val drawPath = Path().apply {
                                        if (line.points.isNotEmpty()) {
                                            moveTo(line.points.first().x, line.points.first().y)
                                            for (i in 1 until line.points.size) {
                                                lineTo(line.points[i].x, line.points[i].y)
                                            }
                                        }
                                    }
                                    drawPath(
                                        path = drawPath,
                                        color = line.color,
                                        style = Stroke(
                                            width = line.strokeWidth,
                                            cap = StrokeCap.Round,
                                            join = StrokeJoin.Round
                                        )
                                    )
                                }
                            }
                        }
                    }

                    // --- BRUSH & CRAYON CONTROLS ---
                    Spacer(modifier = Modifier.height(12.dp))
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            // Crayon Palette
                            Text(
                                text = "🖍️ Select Crayon:",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(bottom = 6.dp)
                            )
                            
                            val crayons = listOf(
                                Color(0xFFFF5252) to "Red",
                                Color(0xFFFF9800) to "Orange",
                                Color(0xFFFFEB3B) to "Yellow",
                                Color(0xFF4CAF50) to "Green",
                                Color(0xFF2196F3) to "Blue",
                                Color(0xFF9C27B0) to "Purple",
                                Color(0xFFE91E63) to "Pink",
                                Color(0xFF1E1E1E) to "Charcoal",
                                Color(0xFFFFFFFF) to "Eraser"
                            )

                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                items(crayons) { (color, name) ->
                                    Box(
                                        modifier = Modifier
                                            .size(46.dp)
                                            .clip(CircleShape)
                                            .background(color)
                                            .border(
                                                width = if (selectedColor == color) 3.dp else 1.dp,
                                                color = if (selectedColor == color) MaterialTheme.colorScheme.primary else Color.LightGray,
                                                shape = CircleShape
                                            )
                                            .clickable { selectedColor = color }
                                            .testTag("crayon_$name"),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (name == "Eraser") {
                                            Icon(Icons.Default.Clear, contentDescription = "Eraser", modifier = Modifier.size(16.dp), tint = Color.Gray)
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))
                            // Slider & Clear Buttons
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                    Icon(Icons.Default.Create, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Slider(
                                        value = brushWidth,
                                        onValueChange = { brushWidth = it },
                                        valueRange = 3f..36f,
                                        modifier = Modifier.fillMaxWidth(0.7f)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("${brushWidth.toInt()}px", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                                
                                OutlinedButton(
                                    onClick = { pageScribbles[currentPage.pageNumber] = emptyList() },
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                                ) {
                                    Icon(Icons.Default.Refresh, contentDescription = "Clear", modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Clear Page", fontSize = 11.sp)
                                }
                            }
                        }
                    }

                    // --- STORY RHYME POEM ---
                    Spacer(modifier = Modifier.height(16.dp))
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.3f))
                    ) {
                        Column(
                            modifier = Modifier.padding(18.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = currentPage.title.uppercase(),
                                fontWeight = FontWeight.ExtraBold,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.tertiary,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = currentPage.storyPoem,
                                style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 22.sp),
                                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                        }
                    }

                    // --- EXPORT PDF PANEL ---
                    Spacer(modifier = Modifier.height(24.dp))
                    Divider()
                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "🖨️ Done coloring? Compile into a custom physical book!",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    Button(
                        onClick = { onGeneratePdfClick() },
                        enabled = !uiState.colorBookGeneratingPdf,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                            .testTag("download_pdf_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        if (uiState.colorBookGeneratingPdf) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("Assembling Premium PDF Layout...", fontWeight = FontWeight.SemiBold)
                        } else {
                            Icon(Icons.Default.Share, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Compile & Export PDF Book", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    // Download Success Card
                    if (!uiState.colorBookPdfPath.isNullOrEmpty()) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9))
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("🎉", fontSize = 24.sp)
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = "Coloring Book Saved Successfully!",
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF2E7D32),
                                        fontSize = 14.sp
                                    )
                                    Text(
                                        text = "Saved to: ${uiState.colorBookPdfPath}",
                                        fontSize = 11.sp,
                                        color = Color(0xFF1B5E20)
                                    )
                                    Text(
                                        text = "Open inside downloads folder on your device to print instantly!",
                                        fontSize = 11.sp,
                                        color = Color(0xFF1B5E20).copy(alpha = 0.8f)
                                    )
                                }
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(50.dp))
                }
            } else {
                // Initial splash layout
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "📚 No Coloring Book Generated Yet",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Enter a fun theme and child's name above, then tap compile!",
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DynamicVectorLineArt(
    presetType: String,
    pageNumber: Int,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.padding(16.dp)) {
        val cx = size.width / 2f
        val cy = size.height / 2f
        
        // Setup paint style
        val stroke = Stroke(
            width = 4f,
            cap = StrokeCap.Round,
            join = StrokeJoin.Round
        )

        // Backdrop stars/bubbles
        if (presetType == "underwater") {
            drawCircle(Color.LightGray, radius = 6f, center = Offset(cx - 100f, cy - 80f), style = stroke)
            drawCircle(Color.LightGray, radius = 4f, center = Offset(cx - 90f, cy - 110f), style = stroke)
            drawCircle(Color.LightGray, radius = 9f, center = Offset(cx + 120f, cy - 50f), style = stroke)
        } else {
            drawStarOnConsole(this, cx - 110f, cy - 120f, 12f)
            drawStarOnConsole(this, cx + 110f, cy - 100f, 8f)
            drawStarOnConsole(this, cx + 120f, cy + 80f, 9f)
        }

        when (presetType) {
            "space_dinosaur" -> {
                // Planet in orbit
                drawCircle(Color.Black, radius = 35f, center = Offset(cx - 80f, cy + 100f), style = stroke)
                
                // Cute Dinosaur Head
                drawCircle(Color.Black, radius = 55f, center = Offset(cx, cy), style = stroke)
                
                // Sphere Space Helmet
                drawCircle(Color.Black, radius = 75f, center = Offset(cx, cy), style = Stroke(width = 2f))
                
                // Dinosaur body
                drawCircle(Color.Black, radius = 75f, center = Offset(cx - 10f, cy + 85f), style = stroke)
                
                // Face details
                drawCircle(Color.Black, radius = 5f, center = Offset(cx - 15f, cy - 10f))
                drawCircle(Color.Black, radius = 5f, center = Offset(cx + 15f, cy - 10f))
                
                // Smiley
                val smilePath = Path().apply {
                    arcTo(
                        rect = androidx.compose.ui.geometry.Rect(cx - 20f, cy - 5f, cx + 20f, cy + 20f),
                        startAngleDegrees = 0f,
                        sweepAngleDegrees = 180f,
                        forceMoveTo = false
                    )
                }
                drawPath(smilePath, Color.Black, style = stroke)
                
                // Tiny Hands
                drawCircle(Color.Black, radius = 12f, center = Offset(cx - 40f, cy + 60f), style = stroke)
                drawCircle(Color.Black, radius = 12f, center = Offset(cx + 40f, cy + 60f), style = stroke)
            }
            "princess_castle" -> {
                // Castle Wall base
                val path = Path().apply {
                    moveTo(cx - 40f, cy + 120f)
                    lineTo(cx - 40f, cy - 20f)
                    lineTo(cx + 40f, cy - 20f)
                    lineTo(cx + 40f, cy + 120f)
                    close()
                }
                drawPath(path, Color.Black, style = stroke)
                
                // Castle Roof
                val roofPath = Path().apply {
                    moveTo(cx - 50f, cy - 20f)
                    lineTo(cx, cy - 80f)
                    lineTo(cx + 50f, cy - 20f)
                    close()
                }
                drawPath(roofPath, Color.Black, style = stroke)
                
                // Princess Head
                drawCircle(Color.Black, radius = 25f, center = Offset(cx, cy + 50f), style = stroke)
                
                // Tiara
                val tiaraPath = Path().apply {
                    arcTo(
                        rect = androidx.compose.ui.geometry.Rect(cx - 15f, cy + 30f, cx + 15f, cy + 45f),
                        startAngleDegrees = 0f, sweepAngleDegrees = -180f, forceMoveTo = false
                    )
                }
                drawPath(tiaraPath, Color.Black, style = stroke)
                
                // Dress
                val dressPath = Path().apply {
                    moveTo(cx - 15f, cy + 75f)
                    lineTo(cx - 45f, cy + 120f)
                    lineTo(cx + 45f, cy + 120f)
                    lineTo(cx + 15f, cy + 75f)
                    close()
                }
                drawPath(dressPath, Color.Black, style = stroke)
            }
            "underwater" -> {
                // Dolphin/Submarine body ellipse
                val ovalPath = Path().apply {
                    addOval(androidx.compose.ui.geometry.Rect(cx - 100f, cy - 50f, cx + 100f, cy + 50f))
                }
                drawPath(ovalPath, Color.Black, style = stroke)
                
                // Submarine windows
                drawCircle(Color.Black, radius = 14f, center = Offset(cx - 30f, cy), style = stroke)
                drawCircle(Color.Black, radius = 14f, center = Offset(cx + 30f, cy), style = stroke)
                
                // Periscope
                val periscope = Path().apply {
                    moveTo(cx, cy - 50f)
                    lineTo(cx, cy - 90f)
                    lineTo(cx + 30f, cy - 90f)
                }
                drawPath(periscope, Color.Black, style = stroke)
                
                // Seaweed
                val seaweed = Path().apply {
                    moveTo(cx - 120f, cy + 140f)
                    quadraticTo(cx - 140f, cy + 80f, cx - 110f, cy + 40f)
                    quadraticTo(cx - 100f, cy + 80f, cx - 100f, cy + 140f)
                }
                drawPath(seaweed, Color.Black, style = stroke)
            }
            "robot" -> {
                // Robot Head
                val rhead = Path().apply {
                    moveTo(cx - 50f, cy + 10f)
                    lineTo(cx - 50f, cy - 70f)
                    lineTo(cx + 50f, cy - 70f)
                    lineTo(cx + 50f, cy + 10f)
                    close()
                }
                drawPath(rhead, Color.Black, style = stroke)
                
                // Antenna
                drawLine(Color.Black, Offset(cx, cy - 70f), Offset(cx, cy - 100f), strokeWidth = 4f)
                drawCircle(Color.Black, radius = 8f, center = Offset(cx, cy - 105f), style = stroke)
                
                // Eyes
                val eyeL = Path().apply { addRect(androidx.compose.ui.geometry.Rect(cx - 32f, cy - 50f, cx - 12f, cy - 30f)) }
                val eyeR = Path().apply { addRect(androidx.compose.ui.geometry.Rect(cx + 12f, cy - 50f, cx + 32f, cy - 30f)) }
                drawPath(eyeL, Color.Black, style = stroke)
                drawPath(eyeR, Color.Black, style = stroke)
                
                // Smile Line
                drawLine(Color.Black, Offset(cx - 20f, cy - 10f), Offset(cx + 20f, cy - 10f), strokeWidth = 4f)
                
                // Body Block
                val rbody = Path().apply { addRect(androidx.compose.ui.geometry.Rect(cx - 60f, cy + 20f, cx + 60f, cy + 110f)) }
                drawPath(rbody, Color.Black, style = stroke)
                
                // Chest Meter
                drawCircle(Color.Black, radius = 20f, center = Offset(cx, cy + 65f), style = stroke)
            }
            "jungle" -> {
                // Lion outer mane circle lines
                drawCircle(Color.Black, radius = 70f, center = Offset(cx, cy), style = stroke)
                
                // Inner head circle
                drawCircle(Color.Black, radius = 45f, center = Offset(cx, cy), style = stroke)
                
                // Eyes & ears
                drawCircle(Color.Black, radius = 5f, center = Offset(cx - 15f, cy - 10f))
                drawCircle(Color.Black, radius = 5f, center = Offset(cx + 15f, cy - 10f))
                
                // Cute triangle nose
                val nose = Path().apply {
                    moveTo(cx - 8f, cy + 5f)
                    lineTo(cx + 8f, cy + 5f)
                    lineTo(cx, cy + 15f)
                    close()
                }
                drawPath(nose, Color.Black, style = stroke)
                
                // Elephant/Lion ears
                drawCircle(Color.Black, radius = 14f, center = Offset(cx - 30f, cy - 35f), style = stroke)
                drawCircle(Color.Black, radius = 14f, center = Offset(cx + 30f, cy - 35f), style = stroke)
            }
            "farm" -> {
                // Barn Base
                val rbase = Path().apply {
                    addRect(androidx.compose.ui.geometry.Rect(cx - 50f, cy + 10f, cx + 50f, cy + 110f))
                }
                drawPath(rbase, Color.Black, style = stroke)
                
                // Roof
                val rroof = Path().apply {
                    moveTo(cx - 65f, cy + 10f)
                    lineTo(cx, cy - 40f)
                    lineTo(cx + 65f, cy + 10f)
                    close()
                }
                drawPath(rroof, Color.Black, style = stroke)

                // Door
                val rdoor = Path().apply {
                    addRect(androidx.compose.ui.geometry.Rect(cx - 20f, cy + 50f, cx + 20f, cy + 110f))
                }
                drawPath(rdoor, Color.Black, style = stroke)

                // Sun
                drawCircle(Color.Black, radius = 25f, center = Offset(cx - 100f, cy - 85f), style = stroke)
                
                // Cloud
                drawCircle(Color.Black, radius = 15f, center = Offset(cx + 100f, cy - 70f), style = stroke)
                drawCircle(Color.Black, radius = 20f, center = Offset(cx + 120f, cy - 70f), style = stroke)
                drawCircle(Color.Black, radius = 15f, center = Offset(cx + 140f, cy - 70f), style = stroke)
            }
            "vehicles" -> {
                // Car Body
                val rbody = Path().apply {
                    addRect(androidx.compose.ui.geometry.Rect(cx - 85f, cy, cx + 85f, cy + 50f))
                }
                drawPath(rbody, Color.Black, style = stroke)

                // Cabin
                val rcabin = Path().apply {
                    moveTo(cx - 45f, cy)
                    lineTo(cx - 20f, cy - 30f)
                    lineTo(cx + 30f, cy - 30f)
                    lineTo(cx + 55f, cy)
                    close()
                }
                drawPath(rcabin, Color.Black, style = stroke)

                // Wheels
                drawCircle(Color.Black, radius = 20f, center = Offset(cx - 45f, cy + 50f), style = stroke)
                drawCircle(Color.Black, radius = 20f, center = Offset(cx + 45f, cy + 50f), style = stroke)
                
                drawCircle(Color.Black, radius = 6f, center = Offset(cx - 45f, cy + 50f), style = Stroke(width = 2f))
                drawCircle(Color.Black, radius = 6f, center = Offset(cx + 45f, cy + 50f), style = Stroke(width = 2f))
            }
            "superheroes" -> {
                // Head
                drawCircle(Color.Black, radius = 35f, center = Offset(cx, cy - 20f), style = stroke)

                // Mask lines
                drawLine(Color.Black, Offset(cx - 35f, cy - 25f), Offset(cx + 35f, cy - 25f), strokeWidth = 4f)
                drawLine(Color.Black, Offset(cx - 35f, cy - 15f), Offset(cx + 35f, cy - 15f), strokeWidth = 4f)
                drawCircle(Color.Black, radius = 5f, center = Offset(cx - 15f, cy - 20f))
                drawCircle(Color.Black, radius = 5f, center = Offset(cx + 15f, cy - 20f))

                // Superhero body/shield
                val rshield = Path().apply {
                    moveTo(cx - 25f, cy + 15f)
                    lineTo(cx - 45f, cy + 90f)
                    lineTo(cx, cy + 120f)
                    lineTo(cx + 45f, cy + 90f)
                    lineTo(cx + 25f, cy + 15f)
                    close()
                }
                drawPath(rshield, Color.Black, style = stroke)

                // Shield star
                drawStarOnConsole(this, cx, cy + 60f, 15f)
            }
            "pets" -> {
                // Dog head
                drawCircle(Color.Black, radius = 45f, center = Offset(cx, cy), style = stroke)

                // Left floppy ear
                val leftEar = Path().apply {
                    moveTo(cx - 35f, cy - 20f)
                    quadraticTo(cx - 65f, cy - 10f, cx - 55f, cy + 30f)
                    quadraticTo(cx - 40f, cy + 40f, cx - 25f, cy + 10f)
                    close()
                }
                drawPath(leftEar, Color.Black, style = stroke)

                // Right floppy ear
                val rightEar = Path().apply {
                    moveTo(cx + 35f, cy - 20f)
                    quadraticTo(cx + 65f, cy - 10f, cx + 55f, cy + 30f)
                    quadraticTo(cx + 40f, cy + 40f, cx + 25f, cy + 10f)
                    close()
                }
                drawPath(rightEar, Color.Black, style = stroke)

                // Eyes
                drawCircle(Color.Black, radius = 6f, center = Offset(cx - 15f, cy - 5f))
                drawCircle(Color.Black, radius = 6f, center = Offset(cx + 15f, cy - 5f))

                // Nose
                drawCircle(Color.Black, radius = 7f, center = Offset(cx, cy + 12f))

                // Smiley mouth
                val mouthL = Path().apply {
                    arcTo(
                        rect = androidx.compose.ui.geometry.Rect(cx - 12f, cy, cx, cy + 20f),
                        startAngleDegrees = 0f, sweepAngleDegrees = 180f, forceMoveTo = false
                    )
                }
                val mouthR = Path().apply {
                    arcTo(
                        rect = androidx.compose.ui.geometry.Rect(cx, cy, cx + 12f, cy + 20f),
                        startAngleDegrees = 0f, sweepAngleDegrees = 180f, forceMoveTo = false
                    )
                }
                drawPath(mouthL, Color.Black, style = stroke)
                drawPath(mouthR, Color.Black, style = stroke)
            }
            "candyland" -> {
                // Large swirly lollipop
                drawCircle(Color.Black, radius = 40f, center = Offset(cx - 40f, cy - 20f), style = stroke)
                drawCircle(Color.Black, radius = 25f, center = Offset(cx - 40f, cy - 20f), style = Stroke(width = 2f))
                drawCircle(Color.Black, radius = 12f, center = Offset(cx - 40f, cy - 20f), style = Stroke(width = 1.5f))

                // Stick
                drawLine(Color.Black, Offset(cx - 40f, cy + 20f), Offset(cx - 40f, cy + 110f), strokeWidth = 4f)

                // Wrapped candy center
                drawCircle(Color.Black, radius = 25f, center = Offset(cx + 50f, cy + 20f), style = stroke)

                // Ties
                val leftTie = Path().apply {
                    moveTo(cx + 25f, cy + 20f)
                    lineTo(cx + 5f, cy + 5f)
                    lineTo(cx + 5f, cy + 35f)
                    close()
                }
                drawPath(leftTie, Color.Black, style = stroke)

                val rightTie = Path().apply {
                    moveTo(cx + 75f, cy + 20f)
                    lineTo(cx + 95f, cy + 5f)
                    lineTo(cx + 95f, cy + 35f)
                    close()
                }
                drawPath(rightTie, Color.Black, style = stroke)
            }
            "dragons" -> {
                // Dragon head
                drawCircle(Color.Black, radius = 40f, center = Offset(cx, cy - 10f), style = stroke)
                
                // Snout nostrils
                drawCircle(Color.Black, radius = 8f, center = Offset(cx - 10f, cy + 10f), style = stroke)
                drawCircle(Color.Black, radius = 8f, center = Offset(cx + 10f, cy + 10f), style = stroke)
                
                // Left Wing
                val leftWing = Path().apply {
                    moveTo(cx - 30f, cy)
                    lineTo(cx - 75f, cy - 40f)
                    lineTo(cx - 45f, cy + 20f)
                    close()
                }
                drawPath(leftWing, Color.Black, style = stroke)
                
                // Right Wing
                val rightWing = Path().apply {
                    moveTo(cx + 30f, cy)
                    lineTo(cx + 75f, cy - 40f)
                    lineTo(cx + 45f, cy + 20f)
                    close()
                }
                drawPath(rightWing, Color.Black, style = stroke)
                
                // Friendly eyes
                drawCircle(Color.Black, radius = 4f, center = Offset(cx - 12f, cy - 15f))
                drawCircle(Color.Black, radius = 4f, center = Offset(cx + 12f, cy - 15f))
            }
            "pirates" -> {
                // Skull hat triangle
                val hat = Path().apply {
                    moveTo(cx - 60f, cy - 20f)
                    lineTo(cx, cy - 65f)
                    lineTo(cx + 60f, cy - 20f)
                    close()
                }
                drawPath(hat, Color.Black, style = stroke)
                
                // Head circle
                drawCircle(Color.Black, radius = 30f, center = Offset(cx, cy + 15f), style = stroke)
                
                // Eye patch
                drawCircle(Color.Black, radius = 8f, center = Offset(cx - 10f, cy + 12f))
                drawLine(Color.Black, Offset(cx - 30f, cy + 2f), Offset(cx, cy + 20f), strokeWidth = 3f)
                
                // Normal eye
                drawCircle(Color.Black, radius = 4f, center = Offset(cx + 12f, cy + 12f))
            }
            "astronauts" -> {
                // Helmet
                drawCircle(Color.Black, radius = 60f, center = Offset(cx, cy), style = stroke)
                // Visor glass
                drawCircle(Color.Black, radius = 45f, center = Offset(cx, cy), style = Stroke(width = 2f))
                // Eyes & Smile
                drawCircle(Color.Black, radius = 4f, center = Offset(cx - 12f, cy - 8f))
                drawCircle(Color.Black, radius = 4f, center = Offset(cx + 12f, cy - 8f))
                drawLine(Color.Black, Offset(cx - 10f, cy + 10f), Offset(cx + 10f, cy + 10f), strokeWidth = 3f)
                // Antenna
                drawLine(Color.Black, Offset(cx, cy - 60f), Offset(cx, cy - 80f), strokeWidth = 3f)
                drawStarOnConsole(this, cx, cy - 85f, 8f)
            }
            "toys" -> {
                // Bear Head
                drawCircle(Color.Black, radius = 45f, center = Offset(cx, cy), style = stroke)
                // Ears
                drawCircle(Color.Black, radius = 18f, center = Offset(cx - 38f, cy - 38f), style = stroke)
                drawCircle(Color.Black, radius = 18f, center = Offset(cx + 38f, cy - 38f), style = stroke)
                // Muzzle
                drawCircle(Color.Black, radius = 14f, center = Offset(cx, cy + 12f), style = stroke)
                // Small Nose
                val nose = Path().apply {
                    moveTo(cx, cy + 8f)
                    lineTo(cx - 5f, cy + 3f)
                    lineTo(cx + 5f, cy + 3f)
                    close()
                }
                drawPath(nose, Color.Black)
            }
            "ice_cream" -> {
                // Cone triangle
                val cone = Path().apply {
                    moveTo(cx - 35f, cy + 20f)
                    lineTo(cx + 35f, cy + 20f)
                    lineTo(cx, cy + 110f)
                    close()
                }
                drawPath(cone, Color.Black, style = stroke)
                // Scoops
                drawCircle(Color.Black, radius = 35f, center = Offset(cx, cy), style = stroke)
                drawCircle(Color.Black, radius = 30f, center = Offset(cx, cy - 40f), style = stroke)
                drawCircle(Color.Black, radius = 25f, center = Offset(cx, cy - 75f), style = stroke)
                // Cherry
                drawCircle(Color.Black, radius = 8f, center = Offset(cx, cy - 95f))
                drawLine(Color.Black, Offset(cx, cy - 103f), Offset(cx + 15f, cy - 110f), strokeWidth = 2f)
            }
            "circus" -> {
                // Tent Base
                val rbase = Path().apply {
                    addRect(androidx.compose.ui.geometry.Rect(cx - 55f, cy + 20f, cx + 55f, cy + 110f))
                }
                drawPath(rbase, Color.Black, style = stroke)
                // Roof
                val rroof = Path().apply {
                    moveTo(cx - 65f, cy + 20f)
                    lineTo(cx, cy - 50f)
                    lineTo(cx + 65f, cy + 20f)
                    close()
                }
                drawPath(rroof, Color.Black, style = stroke)
                // Flag
                drawLine(Color.Black, Offset(cx, cy - 50f), Offset(cx, cy - 80f), strokeWidth = 3f)
                val flag = Path().apply {
                    moveTo(cx, cy - 80f)
                    lineTo(cx + 25f, cy - 70f)
                    lineTo(cx, cy - 60f)
                    close()
                }
                drawPath(flag, Color.Black)
            }
            "enchanted" -> {
                // Mushroom Stem
                val stem = Path().apply {
                    addRect(androidx.compose.ui.geometry.Rect(cx - 20f, cy + 30f, cx + 20f, cy + 115f))
                }
                drawPath(stem, Color.Black, style = stroke)
                // Cap
                val cap = Path().apply {
                    moveTo(cx - 75f, cy + 30f)
                    quadraticTo(cx, cy - 55f, cx + 75f, cy + 30f)
                    close()
                }
                drawPath(cap, Color.Black, style = stroke)
                // Cap spots
                drawCircle(Color.Black, radius = 10f, center = Offset(cx - 30f, cy - 5f), style = Stroke(width = 2f))
                drawCircle(Color.Black, radius = 8f, center = Offset(cx + 35f, cy + 5f), style = Stroke(width = 2f))
            }
            "dino_cave" -> {
                // Volcano
                val volcano = Path().apply {
                    moveTo(cx - 90f, cy + 110f)
                    lineTo(cx - 30f, cy + 10f)
                    lineTo(cx + 30f, cy + 10f)
                    lineTo(cx + 90f, cy + 110f)
                    close()
                }
                drawPath(volcano, Color.Black, style = stroke)
                // Smoke clouds
                drawCircle(Color.Black, radius = 18f, center = Offset(cx - 15f, cy - 12f), style = stroke)
                drawCircle(Color.Black, radius = 18f, center = Offset(cx + 15f, cy - 12f), style = stroke)
                // Dino baby head peeking
                drawCircle(Color.Black, radius = 16f, center = Offset(cx - 100f, cy + 80f), style = stroke)
            }
            "aliens" -> {
                // Saucer disk
                val disk = Path().apply {
                    addOval(androidx.compose.ui.geometry.Rect(cx - 95f, cy + 10f, cx + 95f, cy + 55f))
                }
                drawPath(disk, Color.Black, style = stroke)
                // Glass dome on top
                val dome = Path().apply {
                    arcTo(
                        rect = androidx.compose.ui.geometry.Rect(cx - 45f, cy - 35f, cx + 45f, cy + 15f),
                        startAngleDegrees = 180f, sweepAngleDegrees = 180f, forceMoveTo = true
                    )
                }
                drawPath(dome, Color.Black, style = stroke)
                // Happy alien head
                drawCircle(Color.Black, radius = 20f, center = Offset(cx, cy - 10f), style = stroke)
                drawLine(Color.Black, Offset(cx, cy - 30f), Offset(cx, cy - 50f), strokeWidth = 3f)
                drawCircle(Color.Black, radius = 5f, center = Offset(cx, cy - 52f))
            }
            "winter" -> {
                // Snowman balls
                drawCircle(Color.Black, radius = 45f, center = Offset(cx, cy + 65f), style = stroke)
                drawCircle(Color.Black, radius = 32f, center = Offset(cx, cy + 5f), style = stroke)
                drawCircle(Color.Black, radius = 22f, center = Offset(cx, cy - 40f), style = stroke)
                // Hat
                val hatBase = Path().apply {
                    addRect(androidx.compose.ui.geometry.Rect(cx - 15f, cy - 85f, cx + 15f, cy - 62f))
                }
                drawPath(hatBase, Color.Black, style = stroke)
                drawLine(Color.Black, Offset(cx - 28f, cy - 62f), Offset(cx + 28f, cy - 62f), strokeWidth = 4f)
            }
            else -> { // jungle / other
                // Lion outer mane circle lines
                drawCircle(Color.Black, radius = 70f, center = Offset(cx, cy), style = stroke)
                
                // Inner head circle
                drawCircle(Color.Black, radius = 45f, center = Offset(cx, cy), style = stroke)
                
                // Eyes & ears
                drawCircle(Color.Black, radius = 5f, center = Offset(cx - 15f, cy - 10f))
                drawCircle(Color.Black, radius = 5f, center = Offset(cx + 15f, cy - 10f))
                
                // Cute triangle nose
                val nose = Path().apply {
                    moveTo(cx - 8f, cy + 5f)
                    lineTo(cx + 8f, cy + 5f)
                    lineTo(cx, cy + 15f)
                    close()
                }
                drawPath(nose, Color.Black, style = stroke)
                
                // Elephant/Lion ears
                drawCircle(Color.Black, radius = 14f, center = Offset(cx - 30f, cy - 35f), style = stroke)
                drawCircle(Color.Black, radius = 14f, center = Offset(cx + 30f, cy - 35f), style = stroke)
            }
        }
    }
}

// Help draw starry decorations inside canvas
private fun drawStarOnConsole(scope: DrawScope, x: Float, y: Float, sz: Float) {
    val path = Path().apply {
        moveTo(x, y - sz)
        lineTo(x + sz * 0.3f, y - sz * 0.3f)
        lineTo(x + sz, y)
        lineTo(x + sz * 0.3f, y + sz * 0.3f)
        lineTo(x, y + sz)
        lineTo(x - sz * 0.3f, y + sz * 0.3f)
        lineTo(x - sz, y)
        lineTo(x - sz * 0.3f, y - sz * 0.3f)
        close()
    }
    scope.drawPath(path, Color.Black, style = Stroke(width = 3f))
}
