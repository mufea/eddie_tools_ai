package com.example.ui.chat

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import java.io.InputStream

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CulinaryAssistantView(
    uiState: ChatUiState,
    paddingValues: PaddingValues,
    onFilterChange: (String) -> Unit,
    onSelectRecipe: (CulinaryRecipe?) -> Unit,
    onStepIndexChange: (Int) -> Unit,
    onAddShoppingListItem: (String) -> Unit,
    onRemoveShoppingListItem: (String) -> Unit,
    onClearShoppingList: () -> Unit,
    onImageSelected: (Bitmap?, Uri?) -> Unit,
    onAnalyzeImage: (Bitmap) -> Unit,
    onSpeakStep: (String) -> Unit,
    onStopSpeak: () -> Unit
) {
    val context = LocalContext.current
    var activeTabSubScreen by remember { mutableStateOf("fridge") } // "fridge" or "shopping"

    // Image Pickers
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val decoded: Bitmap? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    val source = ImageDecoder.createSource(context.contentResolver, uri)
                    ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
                        decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                    }
                } else {
                    val options = BitmapFactory.Options().apply {
                        inPreferredConfig = Bitmap.Config.ARGB_8888
                    }
                    context.contentResolver.openInputStream(uri)?.use { stream ->
                        BitmapFactory.decodeStream(stream, null, options)
                    }
                }
                if (decoded != null) {
                    val downscaled = resizeCulinaryBitmap(decoded, 1080)
                    onImageSelected(downscaled, uri)
                    onAnalyzeImage(downscaled)
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
            val downscaled = resizeCulinaryBitmap(bitmap, 1080)
            onImageSelected(downscaled, null)
            onAnalyzeImage(downscaled)
        }
    }

    // Modern organic colors: Earthy Green and warm orange highlights
    val culinaryPrimaryDark = Color(0xFF1E4631)
    val culinaryAccentOrange = Color(0xFFE07A5F)
    val cardBackgroundIvory = Color(0xFFFBFBF9)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.background,
                        Color(0xFFF4F6F4)
                    )
                )
            )
    ) {
        // --- CUSTOM HERO HEADER BANNER ---
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(culinaryPrimaryDark, Color(0xFF2C6B4E))
                    )
                )
                .padding(vertical = 20.dp, horizontal = 24.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "🍳 Smart Fridge & Culinary Assistant",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Snap your fridge contents & cook hands-free",
                        color = Color.White.copy(alpha = 0.85f),
                        fontSize = 12.sp
                    )
                }
                
                // Toggle sub-screen tabs (Fridge/Recipes VS Shopping List)
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White.copy(alpha = 0.15f))
                        .padding(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (activeTabSubScreen == "fridge") Color.White else Color.Transparent)
                            .clickable { activeTabSubScreen = "fridge" }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "Fridge",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (activeTabSubScreen == "fridge") culinaryPrimaryDark else Color.White
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (activeTabSubScreen == "shopping") Color.White else Color.Transparent)
                            .clickable { activeTabSubScreen = "shopping" }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        BadgedBox(
                            badge = {
                                if (uiState.culinaryShoppingList.isNotEmpty()) {
                                    Badge(containerColor = culinaryAccentOrange) {
                                        Text(uiState.culinaryShoppingList.size.toString())
                                    }
                                }
                            }
                        ) {
                            Text(
                                text = "Shopping List",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (activeTabSubScreen == "shopping") culinaryPrimaryDark else Color.White
                            )
                        }
                    }
                }
            }
        }

        if (uiState.culinarySelectedRecipe != null) {
            // ==================== STEP-BY-STEP HANDS-FREE PLAYER ====================
            val recipe = uiState.culinarySelectedRecipe
            val activeStepIndex = uiState.culinaryCurrentStepIndex
            val stepText = recipe.steps.getOrNull(activeStepIndex) ?: "Ready to start cooking!"

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header of step mode
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { onSelectRecipe(null) },
                        modifier = Modifier.background(MaterialTheme.colorScheme.surface, CircleShape)
                    ) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back to Recipes")
                    }

                    Text(
                        text = "STEP-BY-STEP MODE",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = culinaryAccentOrange,
                        letterSpacing = 1.5.sp
                    )

                    Box(modifier = Modifier.size(48.dp))
                }

                Text(
                    text = recipe.title,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = culinaryPrimaryDark,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // Large Progress Indicator
                Text(
                    text = "Step ${activeStepIndex + 1} of ${recipe.steps.size}",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(0.9f).padding(bottom = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    recipe.steps.forEachIndexed { i, _ ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(
                                    if (i <= activeStepIndex) culinaryPrimaryDark 
                                    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f)
                                )
                        )
                    }
                }

                // SUPER LARGE ACCESSIBLE TEXT CARD
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 20.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = cardBackgroundIvory),
                    elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = "Step Icon",
                            tint = culinaryPrimaryDark,
                            modifier = Modifier.size(36.dp).padding(bottom = 12.dp)
                        )

                        Text(
                            text = stepText,
                            fontSize = 24.sp, // EXTRA LARGE
                            lineHeight = 36.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF3E2723), // Warm rich dark contrast
                            textAlign = TextAlign.Center,
                            modifier = Modifier.testTag("step_large_text")
                        )
                    }
                }

                // Voice / Read Aloud controls
                Card(
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .padding(bottom = 24.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (uiState.culinaryIsTtsPlaying) culinaryAccentOrange.copy(alpha = 0.15f)
                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = if (uiState.culinaryIsTtsPlaying) "🗣️" else "🤫",
                                fontSize = 24.sp,
                                modifier = Modifier.padding(end = 10.dp)
                            )
                            Column {
                                Text(
                                    text = if (uiState.culinaryIsTtsPlaying) "Reading Step Aloud..." else "Voice Guide Paused",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = culinaryPrimaryDark
                                )
                                Text(
                                    text = "Hands-free sound directions",
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        if (uiState.culinaryIsTtsPlaying) {
                            IconButton(
                                onClick = onStopSpeak,
                                modifier = Modifier.background(Color.White, CircleShape)
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "Stop Speech", tint = Color.Red)
                            }
                        } else {
                            IconButton(
                                onClick = { onSpeakStep(stepText) },
                                modifier = Modifier.background(culinaryPrimaryDark, CircleShape)
                            ) {
                                Icon(Icons.Filled.PlayArrow, contentDescription = "Read Aloud", tint = Color.White)
                            }
                        }
                    }
                }

                // Bottom Pagination controllers
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilledTonalButton(
                        onClick = { onStepIndexChange(activeStepIndex - 1) },
                        enabled = activeStepIndex > 0,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.height(48.dp)
                    ) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back Page")
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("PREV", fontWeight = FontWeight.Bold)
                    }

                    FilledTonalButton(
                        onClick = { onSelectRecipe(null) },
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        modifier = Modifier.height(48.dp)
                    ) {
                        Text("QUIT", fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = {
                            if (activeStepIndex < recipe.steps.size - 1) {
                                onStepIndexChange(activeStepIndex + 1)
                            } else {
                                Toast.makeText(context, "🎉 You compiled this recipe successfully! Bon Appetit!", Toast.LENGTH_LONG).show()
                                onSelectRecipe(null)
                            }
                        },
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = culinaryPrimaryDark),
                        modifier = Modifier.height(48.dp)
                    ) {
                        Text(
                            text = if (activeStepIndex < recipe.steps.size - 1) "NEXT" else "DONE 🎉", 
                            fontWeight = FontWeight.Bold
                        )
                        if (activeStepIndex < recipe.steps.size - 1) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(Icons.Default.ArrowForward, contentDescription = "Next Step")
                        }
                    }
                }

                // Missing ingredients reminder right in Step cook view!
                if (recipe.missingIngredients.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(30.dp))
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF5F3))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                "Missing something?",
                                fontWeight = FontWeight.Bold,
                                color = culinaryAccentOrange,
                                fontSize = 13.sp
                            )
                            Text(
                                "You can click on any missing ingredient below to throw it in your checkable Shopping List:",
                                fontSize = 11.sp,
                                modifier = Modifier.padding(bottom = 10.dp)
                            )
                            
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                recipe.missingIngredients.forEach { item ->
                                    val isAdded = uiState.culinaryShoppingList.contains(item)
                                    AssistChip(
                                        onClick = {
                                            if (isAdded) {
                                                onRemoveShoppingListItem(item)
                                            } else {
                                                onAddShoppingListItem(item)
                                                Toast.makeText(context, "Added $item to Shopping List!", Toast.LENGTH_SHORT).show()
                                            }
                                        },
                                        label = { Text(item, fontSize = 11.sp) },
                                        leadingIcon = {
                                            Icon(
                                                imageVector = if (isAdded) Icons.Default.Check else Icons.Default.Add,
                                                contentDescription = "Status",
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        } else if (activeTabSubScreen == "shopping") {
            // ==================== CHECKABLE SHOPPING LIST SCREEN ====================
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "🛒 Shopping Pantry",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium,
                            color = culinaryPrimaryDark
                        )
                        Text(
                            text = "One-click elements saved from recipes",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    if (uiState.culinaryShoppingList.isNotEmpty()) {
                        TextButton(
                            onClick = onClearShoppingList,
                            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = "Clear All")
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Clear Clean", fontWeight = FontWeight.Bold)
                        }
                    }
                }

                if (uiState.culinaryShoppingList.isEmpty()) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 50.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("🥑", fontSize = 64.sp)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Your Shopping List is Empty!",
                            fontWeight = FontWeight.Bold,
                            color = culinaryPrimaryDark
                        )
                        Text(
                            text = "Snap a photo of your fridge, pick a recipe, and tap on missing ingredients to pile them here.",
                            style = MaterialTheme.typography.bodySmall,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 24.dp, vertical = 6.dp)
                        )
                    }
                } else {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, Color(0xFFE2EFE8))
                    ) {
                        LazyColumn(
                            modifier = Modifier.fillMaxWidth().padding(6.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            items(uiState.culinaryShoppingList) { item ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color(0xFFF9FBF9))
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ShoppingCart,
                                        contentDescription = "Cart",
                                        tint = culinaryPrimaryDark,
                                        modifier = Modifier.padding(end = 12.dp).size(20.dp)
                                    )
                                    
                                    Text(
                                        text = item,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color(0xFF333333),
                                        modifier = Modifier.weight(1f),
                                        fontSize = 14.sp
                                    )

                                    IconButton(onClick = { onRemoveShoppingListItem(item) }) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Remove Item",
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } else {
            // ==================== MAIN FRIDGE SCANNER SCREEN ====================
            Row(modifier = Modifier.fillMaxSize()) {
                
                // Adaptive Filter Sidebar check: If wide layout, display full left side panel!
                // Standard: For responsive design, we place a beautiful sliding drawer structure or a side panel!
                // Let's create a beautiful dynamic left sidebar or horizontal row depending on context size.
                // We will build a Side-Panel directly in the flow!
                
                Column(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(100.dp)
                        .background(Color(0xFFECEFEA))
                        .border(width = 1.dp, color = Color(0xFFDFE4DD))
                        .padding(vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "DIET",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = culinaryPrimaryDark,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    val dietaryOptions = listOf(
                        Pair("🥗 All", "All"),
                        Pair("🌿 Veg", "Vegetarian"),
                        Pair("🥩 Keto", "Keto"),
                        Pair("🌾 Gluten", "Gluten-Free"),
                        Pair("🌱 Vegan", "Vegan")
                    )

                    dietaryOptions.forEach { (label, actionFilter) ->
                        val isSelected = uiState.culinaryDietaryFilter == actionFilter
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 6.dp, vertical = 4.dp)
                                .clickable { onFilterChange(actionFilter) },
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) culinaryPrimaryDark else Color.Transparent
                            )
                        ) {
                            Text(
                                text = label,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) Color.White else Color(0xFF555555),
                                textAlign = TextAlign.Center,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 10.dp, horizontal = 2.dp)
                            )
                        }
                    }
                }

                // Right Column container for scanned photo and list cards
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp)
                ) {
                    // Photo Upload Card
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        border = BorderStroke(1.dp, Color(0xFFE2EFE8))
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            if (uiState.culinarySelectedBitmap != null) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(150.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                ) {
                                    Image(
                                        bitmap = uiState.culinarySelectedBitmap.asImageBitmap(),
                                        contentDescription = "Fridge Photo",
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(Color.Black.copy(alpha = 0.3f))
                                    )
                                    Text(
                                        "Fridge Scanned ✅",
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        modifier = Modifier
                                            .align(Alignment.BottomEnd)
                                            .padding(8.dp)
                                    )
                                }
                            } else {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(110.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color(0xFFF2F5F2))
                                        .clickable { cameraLauncher.launch(null) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("📸", fontSize = 32.sp)
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            "Snap Fridge Photo",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            color = culinaryPrimaryDark
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = { galleryLauncher.launch("image/*") },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text("🖼️", fontSize = 16.sp)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Gallery", fontSize = 11.sp)
                                }

                                Button(
                                    onClick = { cameraLauncher.launch(null) },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = culinaryAccentOrange)
                                ) {
                                    Text("📸", fontSize = 16.sp)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Snap", fontSize = 11.sp)
                                }
                            }
                        }
                    }

                    // Scan Progress Indicator or Results
                    if (uiState.culinaryIsScanning) {
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                            colors = CardDefaults.cardColors(containerColor = culinaryPrimaryDark.copy(alpha = 0.08f))
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                val pulseAnim = rememberInfiniteTransition(label = "scan")
                                val progressVal by pulseAnim.animateFloat(
                                    initialValue = 0.2f,
                                    targetValue = 1f,
                                    animationSpec = infiniteRepeatable(
                                        animation = tween(1200, easing = EaseInOutQuad),
                                        repeatMode = RepeatMode.Reverse
                                    ),
                                    label = "progress"
                                )
                                LinearProgressIndicator(
                                    progress = { progressVal },
                                    color = culinaryAccentOrange,
                                    modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp))
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    "AI is scanning photo for visible ingredients...",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = culinaryPrimaryDark,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    } else if (uiState.culinaryDetectedIngredients.isNotEmpty()) {
                        // Display detected elements
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFF1F6F2)),
                            border = BorderStroke(1.dp, Color(0xFFE0EFE5))
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    "🥕 Identified Ingredients:",
                                    fontWeight = FontWeight.Bold,
                                    color = culinaryPrimaryDark,
                                    fontSize = 12.sp,
                                    modifier = Modifier.padding(bottom = 6.dp)
                                )
                                
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    uiState.culinaryDetectedIngredients.forEach { item ->
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(Color.White)
                                                .border(1.dp, Color(0xFFDFEBE3), RoundedCornerShape(8.dp))
                                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                        ) {
                                            Text(item, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = culinaryPrimaryDark)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    if (uiState.culinaryError.isNotBlank()) {
                        Text(
                            text = uiState.culinaryError,
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                        )
                    }

                    // --- SCROLLABLE LIST OF RECIPE CARDS ---
                    Text(
                        text = "🍽️ Suggested Recipes",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = culinaryPrimaryDark,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    val filteredList = uiState.culinaryRecipes.filter { recipe ->
                        uiState.culinaryDietaryFilter == "All" || recipe.dietaryTags.contains(uiState.culinaryDietaryFilter)
                    }

                    if (filteredList.isEmpty()) {
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.dp, Color(0xFFECECEC))
                        ) {
                            Column(
                                modifier = Modifier.padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("🍳", fontSize = 42.sp)
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = if (uiState.culinaryRecipes.isEmpty()) "No active recipes found." else "No recipes match this filter.",
                                    fontWeight = FontWeight.Bold,
                                    color = culinaryPrimaryDark,
                                    textAlign = TextAlign.Center
                                )
                                Text(
                                    text = if (uiState.culinaryRecipes.isEmpty()) "Take or load a photo of your fridge to automatically weave delicious recipes!" else "Try selecting another lifestyle diet in the sidebar.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }
                        }
                    } else {
                        filteredList.forEach { recipe ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp)
                                    .clickable { onSelectRecipe(recipe) },
                                shape = RoundedCornerShape(16.dp),
                                border = BorderStroke(1.dp, Color(0xFFE4EDE7)),
                                colors = CardDefaults.cardColors(containerColor = Color.White)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.Top
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = recipe.title,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 15.sp,
                                                color = culinaryPrimaryDark,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = recipe.sDescription,
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 2,
                                                overflow = TextOverflow.Ellipsis,
                                                modifier = Modifier.padding(vertical = 4.dp)
                                            )
                                        }

                                        // Difficulty badge
                                        val badgeColor = when (recipe.difficulty) {
                                            "Easy" -> Color(0xFF4CAF50)
                                            "Medium" -> Color(0xFFFF9800)
                                            else -> Color(0xFFF44336)
                                        }
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(badgeColor.copy(alpha = 0.15f))
                                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                        ) {
                                            Text(
                                                text = recipe.difficulty.uppercase(),
                                                color = badgeColor,
                                                fontWeight = FontWeight.Black,
                                                fontSize = 9.sp
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))

                                    // Tags: Calories, Prep Time, Tags
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text("⏱️", fontSize = 14.sp)
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(recipe.prepTime, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                        }

                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text("🔥", fontSize = 14.sp)
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("${recipe.calories} kcal", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                        }
                                        
                                        if (recipe.dietaryTags.isNotEmpty()) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text("🌱", fontSize = 14.sp)
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(recipe.dietaryTags.joinToString(", "), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF4CAF50))
                                            }
                                        }
                                    }

                                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = Color(0xFFF4F4F4))

                                    // Missing Ingredients Addition Notification Block
                                    if (recipe.missingIngredients.isNotEmpty()) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = "⚠️ Missing: ${recipe.missingIngredients.joinToString(", ")}",
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color(0xFFC62828),
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Text(
                                                    "Quick-add to Shopping checklist",
                                                    fontSize = 8.sp,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }

                                            TextButton(
                                                onClick = {
                                                    recipe.missingIngredients.forEach { item ->
                                                        onAddShoppingListItem(item)
                                                    }
                                                    Toast.makeText(context, "Added all missing items to Shopping Pantry!", Toast.LENGTH_SHORT).show()
                                                },
                                                colors = ButtonDefaults.textButtonColors(contentColor = culinaryAccentOrange),
                                                contentPadding = PaddingValues(horizontal = 8.dp)
                                            ) {
                                                Text("➕🛒", fontSize = 14.sp)
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("Add All", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    } else {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(Icons.Default.CheckCircle, contentDescription = "All items list", tint = Color(0xFF2E7D32), modifier = Modifier.size(14.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("You have all ingredients! Let's start!", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32))
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
}

private fun resizeCulinaryBitmap(bitmap: Bitmap, targetWidth: Int): Bitmap {
    val srcWidth = bitmap.width
    val srcHeight = bitmap.height
    if (srcWidth <= targetWidth) return bitmap
    val ratio = srcWidth.toFloat() / srcHeight.toFloat()
    val targetHeight = (targetWidth / ratio).toInt()
    return Bitmap.createScaledBitmap(bitmap, targetWidth, targetHeight, true)
}
