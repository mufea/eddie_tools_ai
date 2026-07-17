package com.example.ui.chat

import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.db.InteriorDesignEntity
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

// Structure for beautiful preset demo rooms
data class PresetDemoRoom(
    val id: String,
    val title: String,
    val description: String,
    val originalUrl: String,
    val defaultStyle: String,
    val styleReimaginedUrls: Map<String, String>
)

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun EddieInteriorDesignerView(
    viewModel: ChatViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current

    // Local states
    var customProjectTitle by remember { mutableStateOf("") }
    var chatInputText by remember { mutableStateOf("") }
    var showSaveDialog by remember { mutableStateOf(false) }
    var showHistoryDialog by remember { mutableStateOf(false) }

    val listState = rememberLazyListState()
    val chatState = rememberScrollState()
    val isKeyboardVisible = WindowInsets.isImeVisible

    // Scroll main list and inner chat to bottom when keyboard appears
    LaunchedEffect(isKeyboardVisible) {
        if (isKeyboardVisible) {
            listState.animateScrollToItem(6) // index of SECTION 7 (the chat interface)
            if (uiState.interiorChatMessages.isNotEmpty()) {
                chatState.animateScrollTo(chatState.maxValue)
            }
        }
    }

    // Auto-scroll chat to bottom when a new message is received/sent
    LaunchedEffect(uiState.interiorChatMessages.size) {
        if (uiState.interiorChatMessages.isNotEmpty()) {
            chatState.animateScrollTo(chatState.maxValue)
        }
    }

    // Dotted border stroke for drag/upload card
    val strokePattern = remember {
        val stroke = android.graphics.DashPathEffect(floatArrayOf(15f, 10f), 0f)
        stroke
    }

    // High-fidelity presets for immediate premium playability
    val demoRooms = remember {
        listOf(
            PresetDemoRoom(
                id = "demo_living",
                title = "Cozy Living Room Preset",
                description = "A standard neutral empty layout with simple furnishings",
                originalUrl = "https://images.unsplash.com/photo-1513694203232-719a280e022f?auto=format&fit=crop&q=80&w=1024",
                defaultStyle = "Scandinavian",
                styleReimaginedUrls = mapOf(
                    "Scandinavian" to "https://images.unsplash.com/photo-1598928506311-c55ded91a20c?auto=format&fit=crop&q=80&w=1024",
                    "Mid-Century Modern" to "https://images.unsplash.com/photo-1586023492125-27b2c045efd7?auto=format&fit=crop&q=80&w=1024",
                    "Industrial Loft" to "https://images.unsplash.com/photo-1505691938895-1758d7feb511?auto=format&fit=crop&q=80&w=1024",
                    "Bohemian Chic" to "https://images.unsplash.com/photo-1505693416388-ac5ce068fe85?auto=format&fit=crop&q=80&w=1024",
                    "Japandi" to "https://images.unsplash.com/photo-1616486338812-3dadae4b4ace?auto=format&fit=crop&q=80&w=1024",
                    "Minimalist Zen" to "https://images.unsplash.com/photo-1560185007-c5ca9d2c014d?auto=format&fit=crop&q=80&w=1024",
                    "Art Deco" to "https://images.unsplash.com/photo-1540518614846-7eded433c457?auto=format&fit=crop&q=80&w=1024",
                    "Rustic Farmhouse" to "https://images.unsplash.com/photo-1507652313519-d4e9174996dd?auto=format&fit=crop&q=80&w=1024"
                )
            ),
            PresetDemoRoom(
                id = "demo_bedroom",
                title = "Plain Bedroom Preset",
                description = "An empty plain bedroom awaiting artistic style layout",
                originalUrl = "https://images.unsplash.com/photo-1505693395321-883724634266?auto=format&fit=crop&q=80&w=1024",
                defaultStyle = "Minimalist Zen",
                styleReimaginedUrls = mapOf(
                    "Scandinavian" to "https://images.unsplash.com/photo-1598928506311-c55ded91a20c?auto=format&fit=crop&q=80&w=1024",
                    "Mid-Century Modern" to "https://images.unsplash.com/photo-1586023492125-27b2c045efd7?auto=format&fit=crop&q=80&w=1024",
                    "Industrial Loft" to "https://images.unsplash.com/photo-1505691938895-1758d7feb511?auto=format&fit=crop&q=80&w=1024",
                    "Bohemian Chic" to "https://images.unsplash.com/photo-1505693416388-ac5ce068fe85?auto=format&fit=crop&q=80&w=1024",
                    "Japandi" to "https://images.unsplash.com/photo-1616486338812-3dadae4b4ace?auto=format&fit=crop&q=80&w=1024",
                    "Minimalist Zen" to "https://images.unsplash.com/photo-1560185007-c5ca9d2c014d?auto=format&fit=crop&q=80&w=1024",
                    "Art Deco" to "https://images.unsplash.com/photo-1540518614846-7eded433c457?auto=format&fit=crop&q=80&w=1024",
                    "Rustic Farmhouse" to "https://images.unsplash.com/photo-1507652313519-d4e9174996dd?auto=format&fit=crop&q=80&w=1024"
                )
            )
        )
    }

    var selectedPresetRoomId by remember { mutableStateOf("demo_living") }
    val activePresetRoom = demoRooms.find { it.id == selectedPresetRoomId } ?: demoRooms[0]

    // Active visual assets
    val activeOriginalImage: Any = if (uiState.interiorOriginalBitmap != null) {
        uiState.interiorOriginalBitmap!!
    } else {
        activePresetRoom.originalUrl
    }

    val activeReimaginedImage: Any? = if (uiState.interiorReimaginedImageUrls.isNotEmpty()) {
        uiState.interiorReimaginedImageUrls[uiState.interiorSelectedStyle] ?: activePresetRoom.styleReimaginedUrls[uiState.interiorSelectedStyle]
    } else {
        activePresetRoom.styleReimaginedUrls[uiState.interiorSelectedStyle]
    }

    // Initialize baseline items on start
    LaunchedEffect(uiState.interiorSelectedStyle, selectedPresetRoomId) {
        if (uiState.interiorShoppableItems.isEmpty()) {
            viewModel.selectInteriorStyle(uiState.interiorSelectedStyle)
        }
    }

    // Media Picker launcher
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    val source = ImageDecoder.createSource(context.contentResolver, uri)
                    val bitmap = ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
                        decoder.isMutableRequired = true
                    }
                    viewModel.setInteriorOriginalImage(bitmap, uri)
                } else {
                    @Suppress("DEPRECATION")
                    val bitmap = MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
                    viewModel.setInteriorOriginalImage(bitmap, uri)
                }
                selectedPresetRoomId = "" // Deselect preset
                Toast.makeText(context, "Room photo uploaded successfully!", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(context, "Error loading image: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            }
        }
    }

    LazyColumn(
        state = listState,
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("interior_designer_scrollable")
            .imePadding(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // --- SECTION 1: HEADER & HERO ---
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("interior_header_card"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "AI Interior Designer 🛋️",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Upload your room photo and instantly reimagined custom aesthetics with Gemini design intelligence.",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )
                    }

                    IconButton(
                        onClick = { showHistoryDialog = true },
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.1f), CircleShape)
                            .testTag("interior_history_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Menu,
                            contentDescription = "Saved Designs History",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }
        }

        // --- SECTION 2: THE INTERACTIVE COMPARE SLIDER ---
        item {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Interactive Visualization",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Badge(containerColor = MaterialTheme.colorScheme.secondary) {
                            Text("Drag to Compare", color = Color.White, fontSize = 10.sp, modifier = Modifier.padding(2.dp))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                BoxWithConstraints(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(280.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.DarkGray)
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp))
                        .testTag("compare_slider_container")
                ) {
                    val maxW = maxWidth
                    val widthPx = with(LocalDensity.current) { maxW.toPx() }
                    var sliderPosition by remember { mutableStateOf(widthPx / 2f) }

                    // Safety boundaries
                    sliderPosition = sliderPosition.coerceIn(0f, widthPx)

                    // Layer 1 (Bottom): Reimagined Image
                    if (activeReimaginedImage != null) {
                        Box(modifier = Modifier.fillMaxSize()) {
                            if (activeReimaginedImage is String) {
                                AsyncImage(
                                    model = activeReimaginedImage,
                                    contentDescription = "AI Reimagined Space Style",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            } else if (activeReimaginedImage is Bitmap) {
                                Image(
                                    bitmap = activeReimaginedImage.asImageBitmap(),
                                    contentDescription = "AI Reimagined Space Style",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            }

                            // Dark overlay style indicator (bottom right)
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .padding(8.dp)
                                    .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "Reimagined: ${uiState.interiorSelectedStyle}",
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    } else {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                        }
                    }

                    // Layer 2 (Top Layer, Cropped): Original Image
                    // We crop it using a sub-box with exact horizontal size matching sliderPosition
                    val density = LocalDensity.current
                    val widthDp = with(density) { sliderPosition.toDp() }

                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .width(widthDp)
                            .clipToBounds()
                    ) {
                        Box(modifier = Modifier.width(maxW).fillMaxHeight()) {
                            if (activeOriginalImage is String) {
                                AsyncImage(
                                    model = activeOriginalImage,
                                    contentDescription = "Original Space Photo",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            } else if (activeOriginalImage is Bitmap) {
                                Image(
                                    bitmap = activeOriginalImage.asImageBitmap(),
                                    contentDescription = "Original Space Photo",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            }

                            // Original overlay indicator (bottom left)
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomStart)
                                    .padding(8.dp)
                                    .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "Original",
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    // Divider bar with drag handle
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .width(4.dp)
                            .offset { IntOffset(sliderPosition.roundToInt(), 0) }
                            .background(MaterialTheme.colorScheme.primary)
                    )

                    // Drag Handler Button overlay
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .offset {
                                IntOffset(
                                    (sliderPosition - 18f).roundToInt(),
                                    (140 - 20) // Centered vertically in height of 280
                                )
                            }
                            .pointerInput(Unit) {
                                detectDragGestures { change, dragAmount ->
                                    change.consume()
                                    sliderPosition += dragAmount.x
                                }
                            }
                            .background(MaterialTheme.colorScheme.primary, CircleShape)
                            .border(2.dp, Color.White, CircleShape)
                            .testTag("compare_slider_handle"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowForward,
                            contentDescription = "Drag to compare slider handle",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }

        // --- SECTION 3: ROOM PRESETS & CUSTOM UPLOAD BAR ---
        item {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Select Room Setup",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(8.dp))

                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Upload Custom Button Card
                    item {
                        Card(
                            modifier = Modifier
                                .width(120.dp)
                                .height(95.dp)
                                .clickable { imagePickerLauncher.launch("image/*") }
                                .testTag("upload_custom_photo_btn"),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "Upload Photo icon",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Upload Room",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }

                    // Presets List
                    items(demoRooms) { room ->
                        val isSelected = selectedPresetRoomId == room.id
                        Card(
                            modifier = Modifier
                                .width(160.dp)
                                .height(95.dp)
                                .clickable {
                                    selectedPresetRoomId = room.id
                                    viewModel.setInteriorOriginalImage(null, null) // Clear custom
                                    viewModel.selectInteriorStyle(room.defaultStyle)
                                }
                                .testTag("preset_card_${room.id}"),
                            shape = RoundedCornerShape(12.dp),
                            border = if (isSelected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                AsyncImage(
                                    model = room.originalUrl,
                                    contentDescription = room.title,
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(RoundedCornerShape(8.dp)),
                                    contentScale = ContentScale.Crop
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = room.title,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "Preset Space",
                                        fontSize = 9.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // --- SECTION 4: REIMAGINED STYLES CAROUSEL ---
        item {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Reimagined Style Carousel",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(8.dp))

                val stylesList = remember {
                    listOf(
                        "Scandinavian" to "🪵 Scandinavian",
                        "Mid-Century Modern" to "🛋️ Mid-Century",
                        "Industrial Loft" to "🧱 Industrial",
                        "Bohemian Chic" to "🌸 Boho Chic",
                        "Japandi" to "🍵 Japandi",
                        "Minimalist Zen" to "🎋 Minimal Zen",
                        "Art Deco" to "💎 Art Deco",
                        "Rustic Farmhouse" to "🏡 Farmhouse"
                    )
                }

                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(stylesList) { (styleId, styleLabel) ->
                        val isSelected = uiState.interiorSelectedStyle == styleId
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                viewModel.selectInteriorStyle(styleId)
                            },
                            label = { Text(styleLabel, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                            modifier = Modifier.testTag("style_chip_$styleId")
                        )
                    }
                }
            }
        }

        // --- SECTION 5: GENERATE MAKEOVER ACTION BAR ---
        item {
            Column(modifier = Modifier.fillMaxWidth()) {
                if (uiState.interiorIsAnalyzing) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(36.dp))
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Gemini is analyzing architectural parameters & generating spatial makeovers...",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Generate Button
                        Button(
                            onClick = {
                                // If they uploaded a custom image, analyze with Gemini!
                                if (uiState.interiorOriginalBitmap != null) {
                                    viewModel.analyzeInteriorRoom(uiState.interiorOriginalBitmap!!)
                                } else {
                                    // Preset mode, just re-trigger preset loading/simulating analysis
                                    coroutineScope.launch {
                                        viewModel.selectInteriorStyle(uiState.interiorSelectedStyle)
                                        Toast.makeText(context, "Preset visual makeover computed!", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("generate_makeover_btn"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Star, contentDescription = "Sparkle")
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (uiState.interiorOriginalBitmap != null) "AI Generate Makeover ✨" else "Compute Makeover Layout",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }

                        // Save current project button
                        FilledTonalButton(
                            onClick = { showSaveDialog = true },
                            modifier = Modifier
                                .height(48.dp)
                                .testTag("save_makeover_project_btn"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Check, contentDescription = "Save project")
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Save", fontWeight = FontWeight.Bold)
                        }
                    }
                }

                if (uiState.interiorError.isNotBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = uiState.interiorError,
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                }
            }
        }

        // --- SECTION 6: SHOPPABLE CURATED GOODS GRIDS ---
        item {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Shoppable Curated Goods 🛒",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Selected furniture item links seen in the reimagined ${uiState.interiorSelectedStyle} layout.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                )

                Spacer(modifier = Modifier.height(10.dp))

                if (uiState.interiorShoppableItems.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp)
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No items loaded. Select a style above.", fontSize = 12.sp, color = Color.Gray)
                    }
                } else {
                    // Let's list the products cleanly in a horizontal scrollable list or custom grid
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(uiState.interiorShoppableItems) { item ->
                            Card(
                                modifier = Modifier
                                    .width(200.dp)
                                    .testTag("shoppable_item_card_${item.id}"),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                            ) {
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    // Thumbnail
                                    AsyncImage(
                                        model = item.imageUrl,
                                        contentDescription = item.name,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(110.dp)
                                            .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)),
                                        contentScale = ContentScale.Crop
                                    )

                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(8.dp)
                                    ) {
                                        Text(
                                            text = item.name,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )

                                        Spacer(modifier = Modifier.height(4.dp))

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = item.price,
                                                fontWeight = FontWeight.ExtraBold,
                                                fontSize = 13.sp,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                            Badge(containerColor = MaterialTheme.colorScheme.surfaceVariant) {
                                                Text(item.merchant, fontSize = 9.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(2.dp))
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(6.dp))

                                        // Rating stars
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Star,
                                                contentDescription = "Rating",
                                                tint = Color(0xFFFFB300),
                                                modifier = Modifier.size(12.dp)
                                            )
                                            Text(
                                                text = item.rating.toString(),
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Medium
                                            )
                                        }

                                        Spacer(modifier = Modifier.height(8.dp))

                                        // Click action
                                        Button(
                                            onClick = {
                                                val shopIntent = android.content.Intent(
                                                    android.content.Intent.ACTION_VIEW,
                                                    Uri.parse("${item.shopUrl}${Uri.encode(item.name + " " + item.merchant)}")
                                                )
                                                try {
                                                    context.startActivity(shopIntent)
                                                } catch (e: Exception) {
                                                    Toast.makeText(context, "Searching deal for ${item.name}...", Toast.LENGTH_SHORT).show()
                                                }
                                            },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(32.dp)
                                                .testTag("view_deal_btn_${item.id}"),
                                            contentPadding = PaddingValues(0.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                                        ) {
                                            Text("View Deal 🛒", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // --- SECTION 7: CONTEXT-AWARE CHAT INTERFACE ---
        item {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Refine Design with Eddie 💬",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(4.dp))

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(260.dp)
                        .testTag("interior_chat_container"),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        // Chats Scroll Area
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .verticalScroll(chatState)
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (uiState.interiorChatMessages.isEmpty()) {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "No messages. Type below or ask a prompt suggestion!",
                                        fontSize = 11.sp,
                                        color = Color.Gray,
                                        modifier = Modifier.padding(16.dp)
                                    )
                                }
                            } else {
                                uiState.interiorChatMessages.forEach { msg ->
                                    val isUser = msg.sender == "user"
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .clip(
                                                    RoundedCornerShape(
                                                        topStart = 12.dp,
                                                        topEnd = 12.dp,
                                                        bottomStart = if (isUser) 12.dp else 2.dp,
                                                        bottomEnd = if (isUser) 2.dp else 12.dp
                                                    )
                                                )
                                                .background(
                                                    if (isUser) MaterialTheme.colorScheme.secondaryContainer
                                                    else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                                                )
                                                .padding(horizontal = 12.dp, vertical = 8.dp)
                                                .widthIn(max = 260.dp)
                                        ) {
                                            Column {
                                                Text(
                                                    text = if (isUser) "You" else "Eddie (AI Consultant)",
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (isUser) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onPrimaryContainer
                                                )
                                                Spacer(modifier = Modifier.height(2.dp))
                                                Text(
                                                    text = msg.text,
                                                    fontSize = 12.sp,
                                                    color = if (isUser) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurface
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Suggested Quick Chips Row
                        val suggestionPrompts = listOf(
                            "Keep layout but make the rug blue 🔵",
                            "Suggest gold lighting 💡",
                            "Where to buy? 🛍️"
                        )

                        LazyRow(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f))
                                .padding(vertical = 4.dp, horizontal = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            items(suggestionPrompts) { text ->
                                SuggestionChip(
                                    onClick = {
                                        viewModel.sendInteriorChatMessage(text)
                                    },
                                    label = { Text(text, fontSize = 10.sp) },
                                    modifier = Modifier.testTag("prompt_chip_${text.hashCode()}")
                                )
                            }
                        }

                        // Chat Entry Box
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            TextField(
                                value = chatInputText,
                                onValueChange = { chatInputText = it },
                                placeholder = { Text("Ask Eddie to refine details...", fontSize = 12.sp) },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("chat_input_field"),
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                                keyboardActions = KeyboardActions(
                                    onSend = {
                                        if (chatInputText.isNotBlank()) {
                                            viewModel.sendInteriorChatMessage(chatInputText)
                                            chatInputText = ""
                                            focusManager.clearFocus()
                                        }
                                    }
                                ),
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = Color.Transparent,
                                    unfocusedContainerColor = Color.Transparent,
                                    disabledContainerColor = Color.Transparent
                                ),
                                singleLine = true
                            )

                            IconButton(
                                onClick = {
                                    if (chatInputText.isNotBlank()) {
                                        viewModel.sendInteriorChatMessage(chatInputText)
                                        chatInputText = ""
                                        focusManager.clearFocus()
                                    }
                                },
                                modifier = Modifier
                                    .background(MaterialTheme.colorScheme.primary, CircleShape)
                                    .testTag("chat_send_button")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Send,
                                    contentDescription = "Send Chat Message",
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // --- DIALOG: SAVE PROJECT ---
    if (showSaveDialog) {
        AlertDialog(
            onDismissRequest = { showSaveDialog = false },
            title = { Text("Save Makeover Project 💾") },
            text = {
                Column {
                    Text("Provide a beautiful descriptive title for this interior design consultant project:", fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = customProjectTitle,
                        onValueChange = { customProjectTitle = it },
                        placeholder = { Text("My Scandinavian Living Room", fontSize = 12.sp) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("dialog_project_title_input"),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.saveCurrentInteriorDesign(customProjectTitle)
                        customProjectTitle = ""
                        showSaveDialog = false
                        Toast.makeText(context, "Makeover Project saved securely to Room database!", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.testTag("dialog_save_confirm")
                ) {
                    Text("Save Project")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSaveDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // --- DIALOG: SAVED PROJECTS HISTORY (ROOM LOCAL DB INTEGRATION) ---
    if (showHistoryDialog) {
        AlertDialog(
            onDismissRequest = { showHistoryDialog = false },
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Saved Makeovers 📁")
                    IconButton(onClick = { viewModel.resetActiveInteriorDesign() }) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = "New Makeover", tint = MaterialTheme.colorScheme.primary)
                    }
                }
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    if (uiState.interiorDesigns.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(150.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("No saved projects found. Complete and save a makeover first!", fontSize = 12.sp, color = Color.Gray)
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 350.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(uiState.interiorDesigns) { design ->
                                val isSelected = uiState.interiorSelectedDesignId == design.id
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            viewModel.loadInteriorDesign(design)
                                            showHistoryDialog = false
                                            Toast.makeText(context, "Loaded Project: ${design.title}", Toast.LENGTH_SHORT).show()
                                        }
                                        .testTag("history_item_${design.id}"),
                                    border = if (isSelected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                    )
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
                                                text = design.title,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = "Style: ${design.selectedStyle}",
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }

                                        Row {
                                            IconButton(
                                                onClick = {
                                                    viewModel.deleteInteriorDesign(design.id)
                                                    Toast.makeText(context, "Project deleted.", Toast.LENGTH_SHORT).show()
                                                },
                                                modifier = Modifier.testTag("delete_history_btn_${design.id}")
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Delete,
                                                    contentDescription = "Delete",
                                                    tint = MaterialTheme.colorScheme.error,
                                                    modifier = Modifier.size(18.dp)
                                                )
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
                Button(onClick = { showHistoryDialog = false }) {
                    Text("Close")
                }
            }
        )
    }
}
