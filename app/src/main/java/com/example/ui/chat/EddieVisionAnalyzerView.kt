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
import androidx.compose.material.icons.filled.MailOutline
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
                val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
                val bitmap = BitmapFactory.decodeStream(inputStream)
                onImageSelected(uri, bitmap)
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
            onImageSelected(null, bitmap)
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
                            text = "Ditenagai asisten multimodal Gemini. Unggah rancangan UI, foto objek, atau terjemahkan teks.",
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
                            text = "📸 Belum ada foto atau gambar terpilih.",
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
                                Text("Ambil Foto", fontSize = 12.sp)
                            }
                            
                            Button(
                                onClick = { galleryLauncher.launch("image/*") },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                            ) {
                                Text("Pilih Galeri", fontSize = 12.sp)
                            }
                        }
                    }
                }
            }

            // Choice of Task Mode
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Layanan Analisis AI Multimodal:",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(
                        "describe" to "🔍 Deskripsi",
                        "translate" to "🗣️ Terjemahkan",
                        "code" to "💻 UI ke Kode"
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
                label = { Text("Instruksi / Detail Tambahan AI (Opsional)") },
                placeholder = { Text("Cth: 'Ubah kode Compose ini menjadi tema gelap' atau 'Sebutkan benda warna merah saja'") },
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
                    Text("Sedang Menganalisis Gambar...")
                } else {
                    Text("✨ Analisis Gambar Sekarang", fontWeight = FontWeight.Bold)
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
                                text = "Hasil Analisis Vision AI",
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
                                Text("Membuka mata digital, Eddie melakukan sinkronisasi visual dengan model multimodal...", style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center)
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
