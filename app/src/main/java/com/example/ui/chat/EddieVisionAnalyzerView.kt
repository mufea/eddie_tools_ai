package com.example.ui.chat

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.launch
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.io.InputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EddieVisionAnalyzerView(
    uiState: ChatUiState,
    paddingValues: PaddingValues,
    onImageSelected: (Uri?, Bitmap?) -> Unit,
    onAnalyzeClick: (String, String) -> Unit,
    onCopyText: (String) -> Unit,
    onShareText: (String) -> Unit
) {
    val context = LocalContext.current
    var selectedTaskMode by remember { mutableStateOf("describe") } // "describe", "translate", "code"
    var customInstructionPrompt by remember { mutableStateOf("") }

    // Launcher for Gallery selection
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val bitmap = decodeSampledBitmapFromUri(context, uri, 1024, 1024)
                if (bitmap != null) {
                    onImageSelected(uri, bitmap)
                }
            } catch (e: Exception) {
                // handle error
            }
        }
    }

    // Launcher for Camera capture
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap: Bitmap? ->
        if (bitmap != null) {
            try {
                val downscaled = resizeBitmapToMax(bitmap, 1024)
                onImageSelected(null, downscaled)
            } catch (e: Exception) {
                onImageSelected(null, bitmap)
            }
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
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header Card
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
                        contentDescription = "Vision",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Column {
                        Text(
                            text = "AI Vision & Image Analyzer",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Powered by Gemini multimodal assistant. Upload UI designs, object photos, or translate text.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Image Container / Selector block
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
            ) {
                if (uiState.visionSelectedBitmap != null) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        Image(
                            bitmap = uiState.visionSelectedBitmap.asImageBitmap(),
                            contentDescription = "Selected image",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Fit
                        )
                        
                        // Close or clear button
                        Surface(
                            color = MaterialTheme.colorScheme.errorContainer,
                            shape = CircleShape,
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(12.dp)
                                .size(36.dp)
                                .clickable { onImageSelected(null, null) }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text("❌", fontSize = 14.sp)
                            }
                        }
                    }
                } else {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "📸 No photo or image has been selected yet.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Button(
                                onClick = { cameraLauncher.launch() },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                            ) {
                                Text("Take Photo", fontSize = 12.sp)
                            }
                            
                            Button(
                                onClick = { galleryLauncher.launch("image/*") },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                            ) {
                                Text("Select Gallery", fontSize = 12.sp)
                            }
                        }
                    }
                }
            }

            // Choice of Task Mode
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "AI Multimodal Analysis Services:",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(
                        "describe" to "🔍 Description",
                        "translate" to "🗣️ Translate",
                        "code" to "💻 UI to Code"
                    ).forEach { (mode, label) ->
                        val isSelected = selectedTaskMode == mode
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedTaskMode = mode },
                            label = { Text(label) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        )
                    }
                }
            }

            // Prompt Modifier TextInput
            OutlinedTextField(
                value = customInstructionPrompt,
                onValueChange = { customInstructionPrompt = it },
                label = { Text("AI Instructions / Additional Details (Optional)") },
                placeholder = { Text("e.g. 'Turn this Compose code into dark theme' or 'List red colored objects only'") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            // Analyze action Button
            Button(
                onClick = { onAnalyzeClick(selectedTaskMode, customInstructionPrompt) },
                enabled = uiState.visionSelectedBitmap != null && !uiState.visionIsLoading,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                if (uiState.visionIsLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp)
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("Analyzing Image...")
                } else {
                    Text("✨ Analyze Image Now", fontWeight = FontWeight.Bold)
                }
            }

            // Analysis Result terminal
            if (uiState.visionResult.isNotBlank() || uiState.visionIsLoading) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Vision AI Analysis Result",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary
                            )
                            
                            if (uiState.visionResult.isNotBlank() && !uiState.visionIsLoading) {
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    IconButton(
                                        onClick = { onCopyText(uiState.visionResult) },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Text("📋", fontSize = 14.sp)
                                    }
                                    IconButton(
                                        onClick = { onShareText(uiState.visionResult) },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(imageVector = Icons.Default.Share, contentDescription = "Share", modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }

                        Divider(modifier = Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))

                        if (uiState.visionIsLoading) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                CircularProgressIndicator()
                                Text("Opening digital eyes, Eddie is performing visual synchronization with multimodal model...", style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center)
                            }
                        } else {
                            SelectionContainer {
                                Text(
                                    text = uiState.visionResult,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontFamily = if (selectedTaskMode == "code") FontFamily.Monospace else FontFamily.Default,
                                        lineHeight = 22.sp
                                    ),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// Support functions to safely decode and downsample bitmaps avoiding OutOfMemoryError crashes
fun resizeBitmapToMax(bitmap: Bitmap, maxDimension: Int): Bitmap {
    val width = bitmap.width
    val height = bitmap.height
    if (width <= maxDimension && height <= maxDimension) return bitmap
    val ratio = width.toFloat() / height.toFloat()
    val newWidth: Int
    val newHeight: Int
    if (width > height) {
        newWidth = maxDimension
        newHeight = (maxDimension / ratio).toInt()
    } else {
        newHeight = maxDimension
        newWidth = (maxDimension * ratio).toInt()
    }
    return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
}

fun decodeSampledBitmapFromUri(context: android.content.Context, uri: Uri, reqWidth: Int, reqHeight: Int): Bitmap? {
    try {
        val options = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }
        context.contentResolver.openInputStream(uri)?.use { inputStream ->
            BitmapFactory.decodeStream(inputStream, null, options)
        }

        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight)
        options.inJustDecodeBounds = false

        return context.contentResolver.openInputStream(uri)?.use { inputStream ->
            BitmapFactory.decodeStream(inputStream, null, options)
        }
    } catch (e: Exception) {
        return null
    }
}

fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
    val height = options.outHeight
    val width = options.outWidth
    var inSampleSize = 1

    if (height > reqHeight || width > reqWidth) {
        val halfHeight = height / 2
        val halfWidth = width / 2

        while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
            inSampleSize *= 2
        }
    }
    return inSampleSize
}
