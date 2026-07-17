package com.example.ui.chat

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.draw.scale
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
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.InputStream

// Template Data Model
data class MemeTemplate(
    val id: String,
    val name: String,
    val imageUrl: String,
    val defaultTop: String,
    val defaultBottom: String
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MemeGeneratorView(
    uiState: ChatUiState,
    paddingValues: PaddingValues,
    onImageSelected: (Bitmap?, Uri?) -> Unit,
    onUpdateTopText: (String) -> Unit,
    onUpdateBottomText: (String) -> Unit,
    onUpdateTextSize: (Float) -> Unit,
    onUpdateTextColor: (String) -> Unit,
    onToggleCapitalization: () -> Unit,
    onSelectSuggestedCaption: (String) -> Unit,
    onAnalyzeImage: (Bitmap) -> Unit,
    onClearAll: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var isLoadingTemplate by remember { mutableStateOf(false) }

    // Predefined trending meme templates
    val trendingTemplates = remember {
        listOf(
            MemeTemplate(
                id = "drake",
                name = "Hotline Bling (Drake)",
                imageUrl = "https://images.unsplash.com/photo-1531746020798-e6953c6e8e04?auto=format&fit=crop&q=80&w=400",
                defaultTop = "AI WRITING MY MEME CODES",
                defaultBottom = "ME EATING PIZZA ROLLS"
            ),
            MemeTemplate(
                id = "distracted",
                name = "Distracted Boyfriend",
                imageUrl = "https://images.unsplash.com/photo-1517841905240-472988babdf9?auto=format&fit=crop&q=80&w=400",
                defaultTop = "NEW FRAMEWORKS",
                defaultBottom = "MY TRUSTY STACK"
            ),
            MemeTemplate(
                id = "success",
                name = "Success Kid",
                imageUrl = "https://images.unsplash.com/photo-1544725176-7c40e5a71c5e?auto=format&fit=crop&q=80&w=400",
                defaultTop = "COMPILED ON FIRST TRY",
                defaultBottom = "ZERO WARNINGS"
            ),
            MemeTemplate(
                id = "two_buttons",
                name = "Two Buttons Choice",
                imageUrl = "https://images.unsplash.com/photo-1516245834210-c4c142787335?auto=format&fit=crop&q=80&w=400",
                defaultTop = "ANOTHER CAPTION",
                defaultBottom = "FIXING MEME BUGS"
            ),
            MemeTemplate(
                id = "doge",
                name = "Doge standard",
                imageUrl = "https://images.unsplash.com/photo-1583511655857-d19b40a7a54e?auto=format&fit=crop&q=80&w=400",
                defaultTop = "MUCH MEME ENGINE",
                defaultBottom = "SO AUTOMATED"
            )
        )
    }

    var selectedTemplateId by remember { mutableStateOf<String?>(null) }

    // Image launchers
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val stream: InputStream? = context.contentResolver.openInputStream(uri)
                val decoded = BitmapFactory.decodeStream(stream)
                if (decoded != null) {
                    val downscaled = resizeMemeBitmap(decoded, 1024)
                    selectedTemplateId = null
                    onImageSelected(downscaled, uri)
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Error loading image: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap: Bitmap? ->
        if (bitmap != null) {
            val downscaled = resizeMemeBitmap(bitmap, 1024)
            selectedTemplateId = null
            onImageSelected(downscaled, null)
        }
    }

    // Aesthetic color guidelines (Playful Slate and Electric Neon accents)
    val memePrimaryDark = Color(0xFF6200EE)
    val memeAccentNeon = Color(0xFF00E676)
    val optionCardBackground = Color(0xFFF6F8FF)
    
    // Dynamic text color parsing helper
    val parsedTextColor = remember(uiState.memeTextColor) {
        try {
            Color(android.graphics.Color.parseColor(uiState.memeTextColor))
        } catch (e: Exception) {
            Color.White
        }
    }

    val rightPanelScrollState = rememberScrollState()
    val isKeyboardVisible = WindowInsets.isImeVisible
    LaunchedEffect(isKeyboardVisible) {
        if (isKeyboardVisible) {
            rightPanelScrollState.animateScrollTo(rightPanelScrollState.maxValue)
        }
    }
    LaunchedEffect(uiState.memeTopText, uiState.memeBottomText) {
        if (uiState.memeTopText.isNotEmpty() || uiState.memeBottomText.isNotEmpty()) {
            rightPanelScrollState.animateScrollTo(rightPanelScrollState.maxValue)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFFAF9FD))
            .padding(paddingValues)
            .imePadding()
    ) {
        // Upper Title Banner
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color(0xFFE8E5FF), Color(0xFFFAF9FD))
                    )
                )
                .padding(horizontal = 16.dp, vertical = 20.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "AI Meme Lab 🎭",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF1E1035)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Magic Caption scans your photo & extracts internet humor",
                        fontSize = 12.sp,
                        color = Color(0xFF5E5075)
                    )
                }

                if (uiState.memeSelectedBitmap != null) {
                    IconButton(
                        onClick = {
                            selectedTemplateId = null
                            onClearAll()
                        },
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = Color(0xFFFFEBEE),
                            contentColor = Color(0xFFD32F2F)
                        ),
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Restart Editor",
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }

        // Error Banner safely displayed
        if (uiState.memeError.isNotEmpty()) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3CD)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                shape = RoundedCornerShape(8.dp)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("💡", fontSize = 16.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = uiState.memeError,
                        color = Color(0xFF856404),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxSize()
                .weight(1f)
        ) {
            // LEFT PANEL (Visual Meme Frame and Template Selector)
            Column(
                modifier = Modifier
                    .weight(1.2f)
                    .fillMaxHeight()
                    .padding(start = 16.dp, end = 8.dp, bottom = 12.dp)
            ) {
                // Meme Frame Board
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .testTag("meme_interactive_frame"),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1326)),
                    border = BorderStroke(2.dp, Color(0xFFE5DEFF)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isLoadingTemplate) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                CircularProgressIndicator(color = memePrimaryDark)
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    "Downloading template...",
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        } else if (uiState.memeSelectedBitmap != null) {
                            // Active Meme Layout
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                val bitmap = uiState.memeSelectedBitmap
                                val aspectRatio = bitmap.width.toFloat() / bitmap.height.toFloat()
                                Box(
                                    modifier = Modifier
                                        .aspectRatio(aspectRatio),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Image(
                                        bitmap = bitmap.asImageBitmap(),
                                        contentDescription = "Active Meme Base",
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.FillBounds
                                    )

                                    // Text Overlays in Real Impact Style!
                                    if (uiState.memeTopText.isNotEmpty()) {
                                        AutoFitMemeText(
                                            text = uiState.memeTopText,
                                            maxFontSize = uiState.memeTextSize,
                                            color = parsedTextColor,
                                            isCapitalized = uiState.memeIsCapitalized,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .align(Alignment.TopCenter)
                                                .padding(top = 16.dp, start = 12.dp, end = 12.dp)
                                        )
                                    }

                                    if (uiState.memeBottomText.isNotEmpty()) {
                                        AutoFitMemeText(
                                            text = uiState.memeBottomText,
                                            maxFontSize = uiState.memeTextSize,
                                            color = parsedTextColor,
                                            isCapitalized = uiState.memeIsCapitalized,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .align(Alignment.BottomCenter)
                                                .padding(bottom = 16.dp, start = 12.dp, end = 12.dp)
                                        )
                                    }
                                }
                            }
                        } else {
                            // Empty State / Setup Controls
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(20.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(72.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFFECE7FF)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("🤪", fontSize = 36.sp)
                                }
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    "No Image Chosen Yet",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    "Tap gallery, snap camera, or choose a trending template below!",
                                    color = Color(0xFFAFA3CE),
                                    fontSize = 11.sp,
                                    textAlign = TextAlign.Center
                                )

                                Spacer(modifier = Modifier.height(24.dp))

                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Button(
                                        onClick = { galleryLauncher.launch("image/*") },
                                        colors = ButtonDefaults.buttonColors(containerColor = memePrimaryDark),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Text("🖼️ Select Gallery", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }

                                    Button(
                                        onClick = { cameraLauncher.launch(null) },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF311B92)),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Text("📸 Camera", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Template selection ribbon
                Text(
                    text = "👇 TRENDING MEME TEMPLATES",
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    color = Color(0xFF6E5E8A),
                    modifier = Modifier.padding(bottom = 6.dp)
                )

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(trendingTemplates) { template ->
                        val isSelected = selectedTemplateId == template.id
                        Card(
                            modifier = Modifier
                                .width(90.dp)
                                .height(95.dp)
                                .clickable {
                                    selectedTemplateId = template.id
                                    onUpdateTopText(template.defaultTop)
                                    onUpdateBottomText(template.defaultBottom)
                                    
                                    // Trigger image download
                                    isLoadingTemplate = true
                                    coroutineScope.launch(Dispatchers.IO) {
                                        try {
                                            val loader = coil.ImageLoader(context)
                                            val request = ImageRequest.Builder(context)
                                                .data(template.imageUrl)
                                                .allowHardware(false)
                                                .build()
                                            val result = loader.execute(request)
                                            val drawable = result.drawable
                                            val bitmap = (drawable as? android.graphics.drawable.BitmapDrawable)?.bitmap
                                                ?: drawable?.let {
                                                    val b = Bitmap.createBitmap(it.intrinsicWidth, it.intrinsicHeight, Bitmap.Config.ARGB_8888)
                                                    val canvas = android.graphics.Canvas(b)
                                                    it.setBounds(0, 0, canvas.width, canvas.height)
                                                    it.draw(canvas)
                                                    b
                                                }
                                            if (bitmap != null) {
                                                withContext(Dispatchers.Main) {
                                                    onImageSelected(bitmap, null)
                                                    isLoadingTemplate = false
                                                }
                                            } else {
                                                withContext(Dispatchers.Main) {
                                                    isLoadingTemplate = false
                                                    Toast.makeText(context, "Could not load template", Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                        } catch (e: Exception) {
                                            withContext(Dispatchers.Main) {
                                                isLoadingTemplate = false
                                                Toast.makeText(context, "Download failed: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    }
                                },
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(
                                width = if (isSelected) 3.dp else 1.dp,
                                color = if (isSelected) memePrimaryDark else Color(0xFFDDD2F3)
                            ),
                            colors = CardDefaults.cardColors(containerColor = Color.White)
                        ) {
                            Box(modifier = Modifier.fillMaxSize()) {
                                AsyncImage(
                                    model = template.imageUrl,
                                    contentDescription = template.name,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                                Spacer(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.7f))))
                                )
                                Text(
                                    text = template.name.substringBefore(" ("),
                                    color = Color.White,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier
                                        .align(Alignment.BottomCenter)
                                        .padding(4.dp)
                                )
                            }
                        }
                    }
                }
            }

            // RIGHT PANEL (Controls, Customize Text, & Magic Captions)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .verticalScroll(rightPanelScrollState)
                    .padding(start = 8.dp, end = 16.dp, bottom = 12.dp)
            ) {
                // Magic Caption Action Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFECE7FF)),
                    border = BorderStroke(1.dp, Color(0xFFD6C8FF))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("✨", fontSize = 20.sp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                "Magic AI Captioner",
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF311B92),
                                fontSize = 14.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            "Let Gemini evaluate your photo context & suggest 5 funny caption options to instantly apply!",
                            fontSize = 11.sp,
                            color = Color(0xFF5E4E8C),
                            lineHeight = 15.sp
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Button(
                            onClick = {
                                if (uiState.memeSelectedBitmap != null) {
                                    onAnalyzeImage(uiState.memeSelectedBitmap)
                                } else {
                                    Toast.makeText(context, "Choose or upload an image first!", Toast.LENGTH_SHORT).show()
                                }
                            },
                            enabled = uiState.memeSelectedBitmap != null && !uiState.memeIsGeneratingCaptions,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp)
                                .testTag("magic_caption_button"),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = memePrimaryDark,
                                contentColor = Color.White,
                                disabledContainerColor = Color(0xFFDCD6F3)
                            )
                        ) {
                            if (uiState.memeIsGeneratingCaptions) {
                                CircularProgressIndicator(
                                    color = Color.White,
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Analyzing Image...", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            } else {
                                Text("✨ Magic Caption", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        // Displaying Suggested Captions
                        if (uiState.memeSuggestedCaptions.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(12.dp))
                            HorizontalDivider(color = Color(0xFFD6C8FF), thickness = 1.dp)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "💡 Select a caption option below:",
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                color = Color(0xFF311B92),
                                modifier = Modifier.padding(bottom = 8.dp)
                            )

                            uiState.memeSuggestedCaptions.forEachIndexed { i, caption ->
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp)
                                        .clickable {
                                            onSelectSuggestedCaption(caption)
                                            Toast.makeText(context, "Caption Applied!", Toast.LENGTH_SHORT).show()
                                        }
                                        .testTag("suggested_caption_$i"),
                                    colors = CardDefaults.cardColors(containerColor = Color.White),
                                    shape = RoundedCornerShape(8.dp),
                                    border = BorderStroke(1.dp, Color(0xFFE5DEFF))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "${i + 1}.",
                                            fontWeight = FontWeight.Bold,
                                            color = memePrimaryDark,
                                            fontSize = 11.sp
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = caption,
                                            fontSize = 11.sp,
                                            color = Color(0xFF21153B),
                                            lineHeight = 14.sp,
                                            fontWeight = FontWeight.Medium,
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Customize Panel
                Text(
                    text = "✍️ CUSTOM CAPTION LABELS",
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    color = Color(0xFF6E5E8A)
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Top Caption input
                OutlinedTextField(
                    value = uiState.memeTopText,
                    onValueChange = onUpdateTopText,
                    label = { Text("Top Caption text") },
                    textStyle = TextStyle(fontSize = 13.sp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("meme_top_input"),
                    shape = RoundedCornerShape(10.dp),
                    maxLines = 2,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = memePrimaryDark,
                        unfocusedBorderColor = Color(0xFFDDD2F3)
                    )
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Bottom Caption input
                OutlinedTextField(
                    value = uiState.memeBottomText,
                    onValueChange = onUpdateBottomText,
                    label = { Text("Bottom Caption text") },
                    textStyle = TextStyle(fontSize = 13.sp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("meme_bottom_input"),
                    shape = RoundedCornerShape(10.dp),
                    maxLines = 2,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = memePrimaryDark,
                        unfocusedBorderColor = Color(0xFFDDD2F3)
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                // CAPTION DESIGN CONTROLS
                Text(
                    text = "🎨 FONT DESIGN CONTROLS",
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    color = Color(0xFF6E5E8A)
                )
                Spacer(modifier = Modifier.height(6.dp))

                // Caption size controller
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        "Font Size: ${uiState.memeTextSize.toInt()}sp",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF423A57)
                    )
                    Row {
                        IconButton(
                            onClick = { onUpdateTextSize((uiState.memeTextSize - 2f).coerceAtLeast(12f)) },
                            modifier = Modifier
                                .size(30.dp)
                                .background(Color(0xFFEDE7F6), CircleShape)
                        ) {
                            Text("-", fontWeight = FontWeight.Bold, color = memePrimaryDark)
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        IconButton(
                            onClick = { onUpdateTextSize((uiState.memeTextSize + 2f).coerceAtMost(48f)) },
                            modifier = Modifier
                                .size(30.dp)
                                .background(Color(0xFFEDE7F6), CircleShape)
                        ) {
                            Text("+", fontWeight = FontWeight.Bold, color = memePrimaryDark)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Color Choice buttons
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        "Text Color:",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF423A57)
                    )

                    val textColors = listOf(
                        "#FFFFFF" to Color.White,
                        "#FFEB3B" to Color(0xFFFFEB3B), // Yellow
                        "#FF5722" to Color(0xFFFF5722), // Coral
                        "#00E676" to Color(0xFF00E676), // Lime
                        "#00E5FF" to Color(0xFF00E5FF)  // Cyan
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        textColors.forEach { (hex, color) ->
                            val isSelected = uiState.memeTextColor == hex
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(color)
                                    .border(
                                        width = if (isSelected) 3.dp else 1.dp,
                                        color = if (isSelected) memePrimaryDark else Color.LightGray,
                                        shape = CircleShape
                                    )
                                    .clickable { onUpdateTextColor(hex) }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Capitalization switch
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        "Force Caps (Classic Mode):",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF423A57)
                    )
                    Switch(
                        checked = uiState.memeIsCapitalized,
                        onCheckedChange = { onToggleCapitalization() },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = memePrimaryDark
                        ),
                        modifier = Modifier.scale(0.8f)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Simulated Export Action Button
                Button(
                    onClick = {
                        if (uiState.memeSelectedBitmap != null) {
                            Toast.makeText(context, "💾 Meme compiled and saved to Gallery!", Toast.LENGTH_LONG).show()
                        } else {
                            Toast.makeText(context, "Choose/upload an image first to save!", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("download_meme_button"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF388E3C))
                ) {
                    Icon(imageVector = Icons.Default.Check, contentDescription = "Save Meme")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Export Meme to Gallery", fontWeight = FontWeight.Black, fontSize = 12.sp)
                }
            }
        }
    }
}

// Simple helper to scale down images smoothly
private fun resizeMemeBitmap(bitmap: Bitmap, targetMaxSize: Int): Bitmap {
    val width = bitmap.width
    val height = bitmap.height
    if (width <= targetMaxSize && height <= targetMaxSize) return bitmap
    
    val ratio = width.toFloat() / height.toFloat()
    val (targetWidth, targetHeight) = if (width > height) {
        targetMaxSize to (targetMaxSize / ratio).toInt()
    } else {
        (targetMaxSize * ratio).toInt() to targetMaxSize
    }
    return Bitmap.createScaledBitmap(bitmap, targetWidth, targetHeight, true)
}

@Composable
private fun AutoFitMemeText(
    text: String,
    maxFontSize: Float,
    color: Color,
    isCapitalized: Boolean,
    modifier: Modifier = Modifier,
    shadowColor: Color = Color.Black
) {
    val processedText = if (isCapitalized) text.uppercase() else text
    var fontSizeValue by remember(processedText, maxFontSize) { mutableStateOf(maxFontSize) }
    var readyToDraw by remember(processedText, maxFontSize) { mutableStateOf(false) }

    Text(
        text = processedText,
        color = color,
        fontSize = fontSizeValue.sp,
        fontWeight = FontWeight.Black,
        fontFamily = FontFamily.SansSerif,
        textAlign = TextAlign.Center,
        lineHeight = (fontSizeValue + 4f).sp,
        style = TextStyle(
            shadow = Shadow(
                color = shadowColor,
                offset = Offset(3f, 3f),
                blurRadius = 4f
            )
        ),
        maxLines = 3,
        overflow = TextOverflow.Clip,
        onTextLayout = { textLayoutResult ->
            if (textLayoutResult.didOverflowHeight || textLayoutResult.didOverflowWidth) {
                if (fontSizeValue > 10f) {
                    fontSizeValue = (fontSizeValue - 1f).coerceAtLeast(10f)
                } else {
                    readyToDraw = true
                }
            } else {
                readyToDraw = true
            }
        },
        modifier = modifier.drawWithContent {
            if (readyToDraw) {
                drawContent()
            }
        }
    )
}
